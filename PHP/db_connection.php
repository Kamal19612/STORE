<?php
/**
 * Database Connection
 * 
 * Handles database connection using PDO
 */

/**
 * Get a PDO database connection
 * 
 * @return PDO The database connection
 */
function getDbConnection() {
    static $pdo;
    
    if (!$pdo) {
        try {
            $dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=" . DB_CHARSET;
            $options = [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES => false,
            ];
            
            $pdo = new PDO($dsn, DB_USER, DB_PASS, $options);
        } catch (PDOException $e) {
            error_log('Database Connection Error: ' . $e->getMessage());
            throw new Exception('Database connection failed: ' . $e->getMessage());
        }
    }
    
    return $pdo;
}

/**
 * Create tables if they don't exist
 */
function setupDatabase() {
    $pdo = getDbConnection();
    
    try {
        // Create orders table
        $pdo->exec("
            CREATE TABLE IF NOT EXISTS `orders` (
                `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                `order_id` VARCHAR(20) NOT NULL,
                `order_date` DATETIME NOT NULL,
                `customer_name` VARCHAR(100) NOT NULL,
                `customer_phone` VARCHAR(20),
                `customer_notes` TEXT,
                `subtotal` DECIMAL(10,2) NOT NULL,
                `tax` DECIMAL(10,2) NOT NULL,
                `total` DECIMAL(10,2) NOT NULL,
                `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                INDEX (`order_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        ");
        
        // Create order items table
        $pdo->exec("
            CREATE TABLE IF NOT EXISTS `order_items` (
                `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                `order_id` INT UNSIGNED NOT NULL,
                `item_name` VARCHAR(100) NOT NULL,
                `item_description` TEXT,
                `price` DECIMAL(10,2) NOT NULL,
                `quantity` INT NOT NULL,
                `total` DECIMAL(10,2) NOT NULL,
                FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        ");
        
        return true;
    } catch (PDOException $e) {
        error_log('Database Setup Error: ' . $e->getMessage());
        return false;
    }
}

// NE PLUS APPELER setupDatabase() automatiquement
// Cela causait des problèmes quand config.php n'était pas encore chargé
// setupDatabase(); // ← LIGNE COMMENTÉE
