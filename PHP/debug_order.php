<?php
session_start();
require_once 'config.php';
require_once 'db_connection.php';

header('Content-Type: text/plain');

echo "=== TEST CONNEXION BD ===\n\n";

try {
    $pdo = getDbConnection();
    echo "✓ Connexion BD OK\n\n";
    
    // Vérifier tables
    echo "=== TABLES ===\n";
    $tables = $pdo->query("SHOW TABLES")->fetchAll(PDO::FETCH_COLUMN);
    foreach ($tables as $table) {
        echo "- $table\n";
    }
    
    echo "\n=== TEST INSERT ===\n";
    
    $pdo->beginTransaction();
    
    $sql = "INSERT INTO orders (order_id, order_date, customer_name, customer_phone, customer_notes, subtotal, tax, total) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    
    $stmt = $pdo->prepare($sql);
    $result = $stmt->execute([
        'TEST-' . time(),
        date('Y-m-d H:i:s'),
        'Test Client',
        '70000000',
        'Test',
        1000,
        0,
        1000
    ]);
    
    if ($result) {
        $orderId = $pdo->lastInsertId();
        echo "✓ Insert order OK - ID: $orderId\n";
        
        $sql2 = "INSERT INTO order_items (order_id, item_name, item_description, price, quantity, total) 
                 VALUES (?, ?, ?, ?, ?, ?)";
        $stmt2 = $pdo->prepare($sql2);
        $stmt2->execute([$orderId, 'Produit test', 'Description', 1000, 1, 1000]);
        
        echo "✓ Insert item OK\n";
        $pdo->commit();
        echo "\n✓ TOUT FONCTIONNE\n";
    }
    
} catch (Exception $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    echo "✗ ERREUR: " . $e->getMessage() . "\n";
    echo "Ligne: " . $e->getLine() . "\n";
    echo "Fichier: " . $e->getFile() . "\n";
}
