<?php
/**
 * Order Helper Functions
 * 
 * Common functions used across the application for order handling
 */

/**
 * Génère un identifiant unique pour une commande
 * 
 * @return string L'identifiant de commande au format ORD-YYYYMMDD-XXXX
 */
function generateOrderId() {
    // Préfixe de la commande
    $prefix = 'ORD';
    
    // Date au format YYYYMMDD
    $dateCode = date('Ymd');
    
    // Numéro aléatoire à 4 chiffres
    $randomNum = mt_rand(1000, 9999);
    
    // Combinaison pour créer l'identifiant unique
    return $prefix . '-' . $dateCode . '-' . $randomNum;
}

/**
 * Génère un ticket/facture à partir du panier et des informations de commande
 * 
 * @param array $cart Les éléments du panier
 * @param array $orderInfo Informations sur la commande (client, etc.)
 * @return array Les détails du ticket formatés
 */
function generateTicket($cart, $orderInfo) {
    // Préparer les éléments du ticket
    $items = [];
    $subtotal = 0;
    
    foreach ($cart as $id => $item) {
        if ($item['quantity'] > 0) {
            $items[] = [
                'id' => $id,
                'name' => $item['nom'],
                'description' => $item['description'] ?? '',
                'price' => $item['prix'],
                'quantity' => $item['quantity']
            ];
            
            $subtotal += $item['prix'] * $item['quantity'];
        }
    }
    
    // Calculer les taxes et le total
    $taxRate = STORE_INFO['tax_rate'];
    $tax = $subtotal * $taxRate;
    $total = $subtotal + $tax;
    
    // Construire le ticket complet
    $ticket = [
        'order_id' => $orderInfo['order_id'],
        'date' => date('Y-m-d H:i:s'),
        'customer' => $orderInfo['customer'],
        'items' => $items,
        'subtotal' => $subtotal,
        'tax_rate' => $taxRate,
        'tax' => $tax,
        'total' => $total
    ];
    
    return $ticket;
}

/**
 * Génère un lien de partage WhatsApp pour une commande
 * 
 * @param array $ticket Données du ticket
 * @return string URL WhatsApp
 */
function createWhatsAppShareLink($ticket) {
    // Construire le message
    $message = "Commande #" . $ticket['order_id'] . "\n\n";
    
    // Ajouter les articles
    foreach ($ticket['items'] as $item) {
        $message .= $item['quantity'] . "x " . $item['name'] . " : " . 
                   number_format($item['price'] * $item['quantity'], 0) . " " . 
                   (isset($ticket['restaurant']['currency']) ? $ticket['restaurant']['currency'] : 'FCFA') . "\n";
    }
    
    // Ajouter le total
    $message .= "\nTotal: " . number_format($ticket['total'], 0) . " " . 
               (isset($ticket['restaurant']['currency']) ? $ticket['restaurant']['currency'] : 'FCFA');
    
    // Créer l'URL
    return "https://wa.me/22660713131?text=" . urlencode($message);
}

/**
 * Format phone number for WhatsApp API
 */
if (!function_exists('formatPhoneForWhatsApp')) {
    /**
     * Format phone number for WhatsApp API
     */
    function formatPhoneForWhatsApp($phone) {
        // Remove any non-numeric characters
        $phone = preg_replace('/[^0-9]/', '', $phone);
        
        // Add country code if not present (assuming France +33 or other local code)
        if (substr($phone, 0, 2) === '06' || substr($phone, 0, 2) === '07') {
            $phone = '33' . substr($phone, 1);
        }
        
        return $phone;
    }
}


/**
 * Format HTML ticket for printing
 */
function formatTicketHtml($ticket) {
    $html = '<div class="ticket" style="font-family: monospace; width: 300px; padding: 10px;">';
    
    // Header
    $html .= '<div style="text-align: center; margin-bottom: 10px;">';
    $html .= '<h2 style="margin: 0;">' . (isset($ticket['restaurant']['name']) ? htmlspecialchars($ticket['restaurant']['name']) : 'Le Restaurant') . '</h2>';
    $html .= '<p>Commande #' . $ticket['order_id'] . '</p>';
    $html .= '<p>' . $ticket['date'] . '</p>';
    $html .= '</div>';
    
    // Customer
    if (!empty($ticket['customer']['name'])) {
        $html .= '<div style="margin-bottom: 10px;">';
        $html .= '<p><strong>Client:</strong> ' . htmlspecialchars($ticket['customer']['name']) . '</p>';
        $html .= '</div>';
    }
    
    // Items
    $html .= '<table style="width: 100%; border-top: 1px dashed #000; border-bottom: 1px dashed #000; padding: 10px 0;">';
    $html .= '<tr><th style="text-align: left;">Qté</th><th style="text-align: left;">Article</th><th style="text-align: right;">Prix</th></tr>';
    
    foreach ($ticket['items'] as $item) {
        $html .= '<tr>';
        $html .= '<td>' . $item['quantity'] . 'x</td>';
        $html .= '<td>' . htmlspecialchars($item['name']) . '</td>';
        $html .= '<td style="text-align: right;">' . number_format($item['price'] * $item['quantity'], 0) . ' ' . 
                (isset($ticket['restaurant']['currency']) ? $ticket['restaurant']['currency'] : 'FCFA') . '</td>';
        $html .= '</tr>';
    }
    
    $html .= '</table>';
    
    // Totals
    $html .= '<div style="margin-top: 10px;">';
    $html .= '<p style="display: flex; justify-content: space-between;"><span>Sous-total:</span> <span>' . 
            number_format($ticket['subtotal'], 0) . ' ' . 
            (isset($ticket['restaurant']['currency']) ? $ticket['restaurant']['currency'] : 'FCFA') . '</span></p>';
            
    $html .= '<p style="display: flex; justify-content: space-between;"><span>TVA (' . $ticket['tax_rate'] . '%):</span> <span>' . 
            number_format($ticket['tax'], 0) . ' ' . 
            (isset($ticket['restaurant']['currency']) ? $ticket['restaurant']['currency'] : 'FCFA') . '</span></p>';
            
    $html .= '<p style="display: flex; justify-content: space-between; font-weight: bold;"><span>Total:</span> <span>' . 
            number_format($ticket['total'], 0) . ' ' . 
            (isset($ticket['restaurant']['currency']) ? $ticket['restaurant']['currency'] : 'FCFA') . '</span></p>';
    $html .= '</div>';
    
    // Footer
    $html .= '<div style="text-align: center; margin-top: 20px;">';
    $html .= '<p>Merci de votre visite!</p>';
    $html .= '</div>';
    
    $html .= '</div>';
    
    return $html;
}