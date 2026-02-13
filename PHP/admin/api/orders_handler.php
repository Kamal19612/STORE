<?php
require_once __DIR__ . '/../../config.php';
require_once __DIR__ . '/../includes/auth.php';
require_once __DIR__ . '/../../db_connection.php';

startAdminSession();

if (!isAdminLoggedIn()) {
    echo json_encode(['success' => false, 'message' => 'Non autorisé']);
    exit;
}

$action = $_POST['action'] ?? $_GET['action'] ?? '';

try {
    $pdo = getDbConnection();
    
    switch ($action) {
        case 'update_status':
            if (!hasPermission('edit_orders')) {
                throw new Exception('Permission refusée');
            }
            
            $orderId = intval($_POST['order_id']);
            $status = $_POST['status'];
            
            // Vérifier que le statut est valide
            if (!array_key_exists($status, ORDER_STATUSES)) {
                throw new Exception('Statut invalide');
            }
            
            // Mettre à jour la commande
            $sql = "UPDATE orders SET status = ? WHERE id = ?";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$status, $orderId]);
            
            // Ajouter dans l'historique
            $sql = "INSERT INTO order_status_history (order_id, status, created_by) VALUES (?, ?, ?)";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$orderId, $status, $_SESSION['admin_id']]);
            
            echo json_encode(['success' => true, 'message' => 'Statut mis à jour']);
            break;
            
        case 'get_order_status':
            $orderId = intval($_POST['order_id']);
            
            $sql = "SELECT status FROM orders WHERE id = ?";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$orderId]);
            $order = $stmt->fetch();
            
            if ($order) {
                echo json_encode(['success' => true, 'status' => $order['status']]);
            } else {
                throw new Exception('Commande introuvable');
            }
            break;
            
        case 'delete':
            if (!hasPermission('delete_orders')) {
                throw new Exception('Permission refusée');
            }
            
            $orderId = intval($_POST['order_id']);
            
            $sql = "DELETE FROM orders WHERE id = ?";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$orderId]);
            
            echo json_encode(['success' => true, 'message' => 'Commande supprimée']);
            break;
            
        case 'export':
            if (!hasPermission('view_orders')) {
                throw new Exception('Permission refusée');
            }
            
            // Export CSV
            header('Content-Type: text/csv; charset=utf-8');
            header('Content-Disposition: attachment; filename=commandes_' . date('Y-m-d') . '.csv');
            
            $output = fopen('php://output', 'w');
            fprintf($output, chr(0xEF).chr(0xBB).chr(0xBF)); // BOM UTF-8
            
            // En-têtes
            fputcsv($output, ['N° Commande', 'Client', 'Téléphone', 'Date', 'Total', 'Statut'], ';');
            
            // Données
            $sql = "SELECT order_id, customer_name, customer_phone, created_at, total, status FROM orders ORDER BY created_at DESC";
            $stmt = $pdo->query($sql);
            
            while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
                fputcsv($output, [
                    $row['order_id'],
                    $row['customer_name'],
                    $row['customer_phone'],
                    date('d/m/Y H:i', strtotime($row['created_at'])),
                    $row['total'],
                    ORDER_STATUSES[$row['status']]['label']
                ], ';');
            }
            
            fclose($output);
            exit;
            
        default:
            throw new Exception('Action non reconnue');
    }
    
} catch (Exception $e) {
    echo json_encode(['success' => false, 'message' => $e->getMessage()]);
}
