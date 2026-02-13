<?php
session_start();

require_once 'config.php';
require_once 'functions.php';
require_once 'db_connection.php';
require_once 'order_helpers.php';

header('Content-Type: application/json');

$logFile = __DIR__ . '/order_error.log';
file_put_contents($logFile, "\n" . date('Y-m-d H:i:s') . " - NOUVELLE COMMANDE\n", FILE_APPEND);

if ($_SERVER['REQUEST_METHOD'] !== 'POST' || !isset($_POST['action']) || $_POST['action'] !== 'checkout') {
    echo json_encode(['success' => false, 'message' => 'Invalid request']);
    exit;
}

if (empty($_SESSION['cart'])) {
    echo json_encode(['success' => false, 'message' => 'Votre panier est vide']);
    exit;
}

$customerName = trim($_POST['customer_name'] ?? '');
$customerPhone = trim($_POST['customer_phone'] ?? '');
$customerAddress = trim($_POST['customer_address'] ?? '');
$customerNotes = trim($_POST['customer_notes'] ?? '');

// Récupérer les coordonnées GPS si disponibles
$customerLatitude = isset($_POST['customer_latitude']) && !empty($_POST['customer_latitude']) 
    ? floatval($_POST['customer_latitude']) 
    : null;
$customerLongitude = isset($_POST['customer_longitude']) && !empty($_POST['customer_longitude']) 
    ? floatval($_POST['customer_longitude']) 
    : null;

if (empty($customerName)) {
    echo json_encode(['success' => false, 'message' => 'Le nom est requis']);
    exit;
}

if (empty($customerPhone)) {
    echo json_encode(['success' => false, 'message' => 'Le téléphone est requis']);
    exit;
}

if (empty($customerAddress)) {
    echo json_encode(['success' => false, 'message' => 'L\'adresse est requise']);
    exit;
}

try {
    $orderId = generateOrderId();
    
    $orderInfo = [
        'order_id' => $orderId,
        'customer' => [
            'name' => $customerName,
            'phone' => $customerPhone,
            'address' => $customerAddress,
            'notes' => $customerNotes
        ]
    ];
    
    $ticket = generateTicket($_SESSION['cart'], $orderInfo);
    
    file_put_contents($logFile, "Ticket généré: " . $orderId . "\n", FILE_APPEND);
    
    // Préparer les variables pour l'insertion
    $orderDate = date('Y-m-d H:i:s');
    $subtotal = $ticket['subtotal'];
    $tax = $ticket['tax'];
    $total = $ticket['total'];
    
    // Sauvegarder en BD
    $pdo = getDbConnection();
    $pdo->beginTransaction();
    
    file_put_contents($logFile, "Transaction démarrée\n", FILE_APPEND);
    
    $sql = "INSERT INTO orders (order_id, order_date, customer_name, customer_phone, customer_notes, customer_latitude, customer_longitude, subtotal, tax, total)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    $stmt = $pdo->prepare($sql);
    $result = $stmt->execute([
        $orderId,
        $orderDate,
        $customerName,
        $customerPhone,
        $customerNotes,
        $customerLatitude,
        $customerLongitude,
        $subtotal,
        $tax,
        $total
    ]);
    
    if (!$result) {
        throw new Exception('Échec insert order');
    }
    
    $dbOrderId = $pdo->lastInsertId();
    file_put_contents($logFile, "Order inseré ID: $dbOrderId\n", FILE_APPEND);
    
    $sql = "INSERT INTO order_items (order_id, item_name, item_description, price, quantity, total) 
            VALUES (?, ?, ?, ?, ?, ?)";
    
    $stmt = $pdo->prepare($sql);
    
    foreach ($ticket['items'] as $item) {
        $stmt->execute([
            $dbOrderId,
            $item['name'],
            $item['description'] ?? '',
            $item['price'],
            $item['quantity'],
            $item['price'] * $item['quantity']
        ]);
    }
    
    file_put_contents($logFile, "Items insérés\n", FILE_APPEND);
    
    $pdo->commit();
    file_put_contents($logFile, "Commit OK\n", FILE_APPEND);
    
    $whatsappLink = createWhatsAppOrderLink($ticket, $customerLatitude, $customerLongitude);
    
    $_SESSION['cart'] = [];
    
    echo json_encode([
        'success' => true,
        'message' => 'Commande enregistrée',
        'order_id' => $orderId,
        'ticket' => $ticket,
        'whatsapp_link' => $whatsappLink
    ]);
    
} catch (Exception $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    
    file_put_contents($logFile, "ERREUR: " . $e->getMessage() . "\n", FILE_APPEND);
    file_put_contents($logFile, "Ligne: " . $e->getLine() . "\n", FILE_APPEND);
    file_put_contents($logFile, "Trace: " . $e->getTraceAsString() . "\n", FILE_APPEND);
    
    echo json_encode([
        'success' => false,
        'message' => 'Erreur: ' . $e->getMessage()
    ]);
}

function createWhatsAppOrderLink($ticket, $latitude = null, $longitude = null) {
    $phone = STORE_INFO['whatsapp'];
    
    $message = "*NOUVELLE COMMANDE " . STORE_INFO['name'] . "*\n\n";
    $message .= "Commande: #" . $ticket['order_id'] . "\n";
    $message .= "Date: " . $ticket['date'] . "\n\n";
    
    $message .= "*CLIENT*\n";
    $message .= "Nom: " . $ticket['customer']['name'] . "\n";
    $message .= "Tel: " . $ticket['customer']['phone'] . "\n";
    
    if (!empty($ticket['customer']['address'])) {
        $message .= "Adresse: " . $ticket['customer']['address'] . "\n";
    }
    
    // Ajouter le lien GPS si disponible
    if ($latitude !== null && $longitude !== null) {
        $message .= "📍 Position GPS: https://www.google.com/maps?q=" . $latitude . "," . $longitude . "\n";
    }
    
    $message .= "\n*ARTICLES*\n";
    foreach ($ticket['items'] as $item) {
        $message .= "- " . $item['quantity'] . "x " . $item['name'];
        $message .= " (" . number_format($item['price'], 0, '.', ' ') . " FCFA)\n";
    }
    
    $message .= "\n*TOTAL: " . number_format($ticket['total'], 0, '.', ' ') . " FCFA*";
    
    if (!empty($ticket['customer']['notes'])) {
        $message .= "\n\nNotes: " . $ticket['customer']['notes'];
    }
    
    return "https://wa.me/" . $phone . "?text=" . urlencode($message);
}