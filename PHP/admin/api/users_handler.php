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
            if (!hasPermission('create_users')) {
                throw new Exception('Permission refusée');
            }
            
            $username = trim($_POST['username']);
            $email = trim($_POST['email']);
            $password = $_POST['password'];
            $role = $_POST['role'];
            
            // Vérifier si username ou email existe déjà
            $sql = "SELECT COUNT(*) FROM admin_users WHERE username = ? OR email = ?";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$username, $email]);
            if ($stmt->fetchColumn() > 0) {
                throw new Exception('Nom d\'utilisateur ou email déjà utilisé');
            }
            
            // Créer l'utilisateur
            $passwordHash = password_hash($password, PASSWORD_DEFAULT);
            $sql = "INSERT INTO admin_users (username, email, password, role) VALUES (?, ?, ?, ?)";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$username, $email, $passwordHash, $role]);
            
            echo json_encode(['success' => true, 'message' => 'Utilisateur créé']);
            break;
            
        case 'get':
            if (!hasPermission('view_users')) {
                throw new Exception('Permission refusée');
            }
            
            $userId = intval($_POST['user_id']);
            $sql = "SELECT id, username, email, role, active FROM admin_users WHERE id = ?";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$userId]);
            $user = $stmt->fetch();
            
            if ($user) {
                echo json_encode(['success' => true, 'user' => $user]);
            } else {
                throw new Exception('Utilisateur introuvable');
            }
            break;
            
        case 'update':
            if (!hasPermission('edit_users')) {
                throw new Exception('Permission refusée');
            }
            
            $userId = intval($_POST['user_id']);
            $username = trim($_POST['username']);
            $email = trim($_POST['email']);
            $role = $_POST['role'];
            $active = isset($_POST['active']) ? 1 : 0;
            
            // Vérifier si username ou email existe déjà (pour un autre user)
            $sql = "SELECT COUNT(*) FROM admin_users WHERE (username = ? OR email = ?) AND id != ?";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$username, $email, $userId]);
            if ($stmt->fetchColumn() > 0) {
                throw new Exception('Nom d\'utilisateur ou email déjà utilisé');
            }
            
            // Mettre à jour
            if (!empty($_POST['password'])) {
                $passwordHash = password_hash($_POST['password'], PASSWORD_DEFAULT);
                $sql = "UPDATE admin_users SET username = ?, email = ?, password = ?, role = ?, active = ? WHERE id = ?";
                $stmt = $pdo->prepare($sql);
                $stmt->execute([$username, $email, $passwordHash, $role, $active, $userId]);
            } else {
                $sql = "UPDATE admin_users SET username = ?, email = ?, role = ?, active = ? WHERE id = ?";
                $stmt = $pdo->prepare($sql);
                $stmt->execute([$username, $email, $role, $active, $userId]);
            }
            
            echo json_encode(['success' => true, 'message' => 'Utilisateur mis à jour']);
            break;
            
        case 'delete':
            if (!hasPermission('delete_users')) {
                throw new Exception('Permission refusée');
            }
            
            $userId = intval($_POST['user_id']);
            
            // Empêcher la suppression de son propre compte
            if ($userId == $_SESSION['admin_id']) {
                throw new Exception('Vous ne pouvez pas supprimer votre propre compte');
            }
            
            $sql = "DELETE FROM admin_users WHERE id = ?";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$userId]);
            
            echo json_encode(['success' => true, 'message' => 'Utilisateur supprimé']);
            break;
            
        default:
            throw new Exception('Action non reconnue');
    }
    
} catch (Exception $e) {
    echo json_encode(['success' => false, 'message' => $e->getMessage()]);
}
