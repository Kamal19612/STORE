<?php
require_once __DIR__ . '/../../config.php';
require_once __DIR__ . '/../includes/auth.php';
require_once __DIR__ . '/../../db_connection.php';

startAdminSession();

if (!isAdminLoggedIn()) {
    echo json_encode(['success' => false, 'message' => 'Non autorisé']);
    exit;
}

$action = $_POST['action'] ?? '';

try {
    $pdo = getDbConnection();
    
    switch ($action) {
        case 'create':
            if (!hasPermission('edit_slider')) {
                throw new Exception('Permission refusée');
            }
            
            $imageUrl = trim($_POST['image_url']);
            $title = trim($_POST['title'] ?? '');
            $link = trim($_POST['link'] ?? '');
            $orderPosition = intval($_POST['order_position'] ?? 0);
            $active = isset($_POST['active']) ? 1 : 0;
            
            $sql = "INSERT INTO slider_images (image_url, title, link, order_position, active) 
                    VALUES (?, ?, ?, ?, ?)";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$imageUrl, $title, $link, $orderPosition, $active]);
            
            echo json_encode(['success' => true, 'message' => 'Image ajoutée']);
            break;
            
        case 'get':
            if (!hasPermission('view_slider')) {
                throw new Exception('Permission refusée');
            }
            
            $slideId = intval($_POST['slide_id']);
            $sql = "SELECT * FROM slider_images WHERE id = ?";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$slideId]);
            $slide = $stmt->fetch();
            
            if ($slide) {
                echo json_encode(['success' => true, 'slide' => $slide]);
            } else {
                throw new Exception('Image introuvable');
            }
            break;
            
        case 'update':
            if (!hasPermission('edit_slider')) {
                throw new Exception('Permission refusée');
            }
            
            $slideId = intval($_POST['slide_id']);
            $imageUrl = trim($_POST['image_url']);
            $title = trim($_POST['title'] ?? '');
            $link = trim($_POST['link'] ?? '');
            $orderPosition = intval($_POST['order_position'] ?? 0);
            
            $sql = "UPDATE slider_images 
                    SET image_url = ?, title = ?, link = ?, order_position = ? 
                    WHERE id = ?";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$imageUrl, $title, $link, $orderPosition, $slideId]);
            
            echo json_encode(['success' => true, 'message' => 'Image mise à jour']);
            break;
            
        case 'toggle':
            if (!hasPermission('edit_slider')) {
                throw new Exception('Permission refusée');
            }
            
            $slideId = intval($_POST['slide_id']);
            $active = intval($_POST['active']);
            
            $sql = "UPDATE slider_images SET active = ? WHERE id = ?";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$active, $slideId]);
            
            echo json_encode(['success' => true]);
            break;
            
        case 'move':
            if (!hasPermission('edit_slider')) {
                throw new Exception('Permission refusée');
            }
            
            $slideId = intval($_POST['slide_id']);
            $direction = $_POST['direction']; // 'up' ou 'down'
            
            // Récupérer la position actuelle
            $sql = "SELECT order_position FROM slider_images WHERE id = ?";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$slideId]);
            $currentPosition = $stmt->fetchColumn();
            
            if ($direction === 'up' && $currentPosition > 0) {
                $newPosition = $currentPosition - 1;
                
                // Échanger avec l'élément au-dessus
                $sql = "UPDATE slider_images SET order_position = ? WHERE order_position = ? AND id != ?";
                $stmt = $pdo->prepare($sql);
                $stmt->execute([$currentPosition, $newPosition, $slideId]);
                
                $sql = "UPDATE slider_images SET order_position = ? WHERE id = ?";
                $stmt = $pdo->prepare($sql);
                $stmt->execute([$newPosition, $slideId]);
                
            } elseif ($direction === 'down') {
                $newPosition = $currentPosition + 1;
                
                // Échanger avec l'élément en-dessous
                $sql = "UPDATE slider_images SET order_position = ? WHERE order_position = ? AND id != ?";
                $stmt = $pdo->prepare($sql);
                $stmt->execute([$currentPosition, $newPosition, $slideId]);
                
                $sql = "UPDATE slider_images SET order_position = ? WHERE id = ?";
                $stmt = $pdo->prepare($sql);
                $stmt->execute([$newPosition, $slideId]);
            }
            
            echo json_encode(['success' => true]);
            break;
            
        case 'delete':
            if (!hasPermission('delete_slider')) {
                throw new Exception('Permission refusée');
            }
            
            $slideId = intval($_POST['slide_id']);
            
            $sql = "DELETE FROM slider_images WHERE id = ?";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$slideId]);
            
            echo json_encode(['success' => true, 'message' => 'Image supprimée']);
            break;
            
        default:
            throw new Exception('Action non reconnue');
    }
    
} catch (Exception $e) {
    echo json_encode(['success' => false, 'message' => $e->getMessage()]);
}
