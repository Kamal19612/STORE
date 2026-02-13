<?php
/**
 * Cart Handler - AJAX Operations
 * 
 * Handles all cart-related AJAX requests
 */

session_start();

require_once 'config.php';
require_once 'functions.php';

// Initialize cart if not exists
if (!isset($_SESSION['cart'])) {
    $_SESSION['cart'] = [];
}

// Set header to return JSON
header('Content-Type: application/json');

// Check if request is POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['success' => false, 'message' => 'Invalid request method']);
    exit;
}

$action = $_POST['action'] ?? '';

switch ($action) {
    case 'add':
        handleAddToCart();
        break;
    
    case 'remove':
        handleRemoveFromCart();
        break;
    
    case 'update_quantity':
        handleUpdateQuantity();
        break;
    
    case 'clear':
        handleClearCart();
        break;
    
    case 'get_cart':
        handleGetCart();
        break;
    
    default:
        echo json_encode(['success' => false, 'message' => 'Invalid action']);
        break;
}

/**
 * Add product to cart
 */
function handleAddToCart() {
    $productId = $_POST['product_id'] ?? '';
    $quantity = isset($_POST['quantity']) ? intval($_POST['quantity']) : 1;
    
    if (empty($productId)) {
        echo json_encode(['success' => false, 'message' => 'Product ID required']);
        return;
    }
    
    // Get product details from sheet
    $product = getProductById($productId);
    
    if (!$product) {
        echo json_encode(['success' => false, 'message' => 'Product not found']);
        return;
    }
    
    if (!$product['disponible']) {
        echo json_encode(['success' => false, 'message' => 'Product not available']);
        return;
    }
    
    // Add to cart or update quantity
    if (isset($_SESSION['cart'][$productId])) {
        $_SESSION['cart'][$productId]['quantity'] += $quantity;
    } else {
        $_SESSION['cart'][$productId] = [
            'id' => $product['id'],
            'nom' => $product['nom'],
            'description' => $product['description'],
            'prix' => $product['prix'],
            'image_url' => $product['image_url'],
            'quantity' => $quantity
        ];
    }
    
    $response = [
        'success' => true,
        'message' => 'Product added to cart',
        'cart' => $_SESSION['cart'],
        'summary' => getCartSummary()
    ];
    
    echo json_encode($response);
}

/**
 * Remove product from cart
 */
function handleRemoveFromCart() {
    $productId = $_POST['product_id'] ?? '';
    $quantity = isset($_POST['quantity']) ? intval($_POST['quantity']) : 1;
    
    if (empty($productId)) {
        echo json_encode(['success' => false, 'message' => 'Product ID required']);
        return;
    }
    
    if (!isset($_SESSION['cart'][$productId])) {
        echo json_encode(['success' => false, 'message' => 'Product not in cart']);
        return;
    }
    
    // Decrease quantity or remove completely
    $_SESSION['cart'][$productId]['quantity'] -= $quantity;
    
    if ($_SESSION['cart'][$productId]['quantity'] <= 0) {
        unset($_SESSION['cart'][$productId]);
    }
    
    $response = [
        'success' => true,
        'message' => 'Product removed from cart',
        'cart' => $_SESSION['cart'],
        'summary' => getCartSummary()
    ];
    
    echo json_encode($response);
}

/**
 * Update product quantity in cart
 */
function handleUpdateQuantity() {
    $productId = $_POST['product_id'] ?? '';
    $quantity = isset($_POST['quantity']) ? intval($_POST['quantity']) : 0;
    
    if (empty($productId)) {
        echo json_encode(['success' => false, 'message' => 'Product ID required']);
        return;
    }
    
    if ($quantity <= 0) {
        unset($_SESSION['cart'][$productId]);
    } else {
        if (isset($_SESSION['cart'][$productId])) {
            $_SESSION['cart'][$productId]['quantity'] = $quantity;
        }
    }
    
    $response = [
        'success' => true,
        'message' => 'Cart updated',
        'cart' => $_SESSION['cart'],
        'summary' => getCartSummary()
    ];
    
    echo json_encode($response);
}

/**
 * Clear entire cart
 */
function handleClearCart() {
    $_SESSION['cart'] = [];
    
    $response = [
        'success' => true,
        'message' => 'Cart cleared',
        'cart' => [],
        'summary' => getCartSummary()
    ];
    
    echo json_encode($response);
}

/**
 * Get current cart state
 */
function handleGetCart() {
    $response = [
        'success' => true,
        'cart' => $_SESSION['cart'],
        'summary' => getCartSummary()
    ];
    
    echo json_encode($response);
}

/**
 * Calculate cart summary
 * 
 * @return array Cart summary with totals
 */
function getCartSummary() {
    $items = [];
    $subtotal = 0;
    $count = 0;
    
    foreach ($_SESSION['cart'] as $id => $item) {
        $itemTotal = $item['prix'] * $item['quantity'];
        $subtotal += $itemTotal;
        $count += $item['quantity'];
        
        $items[] = [
            'id' => $id,
            'nom' => $item['nom'],
            'prix' => $item['prix'],
            'quantity' => $item['quantity'],
            'total' => $itemTotal
        ];
    }
    
    $taxRate = STORE_INFO['tax_rate'];
    $tax = $subtotal * ($taxRate / 100);
    $total = $subtotal + $tax;
    
    return [
        'items' => $items,
        'subtotal' => $subtotal,
        'tax_rate' => $taxRate,
        'tax' => $tax,
        'total' => $total,
        'count' => $count
    ];
}
