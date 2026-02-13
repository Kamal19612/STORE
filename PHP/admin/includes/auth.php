<?php
/**
 * Admin Authentication System
 */

require_once __DIR__ . '/../../config.php';
require_once __DIR__ . '/../config_admin.php';
require_once __DIR__ . '/../../db_connection.php';

/**
 * Démarrer la session admin
 */
function startAdminSession() {
    if (session_status() === PHP_SESSION_NONE) {
        session_name(ADMIN_SESSION_NAME);
        session_start();
    }
}

/**
 * Vérifier si l'utilisateur est connecté
 */
function isAdminLoggedIn() {
    startAdminSession();
    return isset($_SESSION['admin_user']) && isset($_SESSION['admin_id']);
}

/**
 * Vérifier les permissions
 */
function hasPermission($permission) {
    if (!isAdminLoggedIn()) {
        return false;
    }
    
    $role = $_SESSION['admin_role'] ?? 'gestionnaire';
    $permissions = PERMISSIONS[$role] ?? [];
    
    return in_array($permission, $permissions);
}

/**
 * Connexion admin
 */
function loginAdmin($username, $password) {
    try {
        $pdo = getDbConnection();
        
        $sql = "SELECT * FROM admin_users WHERE (username = ? OR email = ?) AND active = 1";
        $stmt = $pdo->prepare($sql);
        $stmt->execute([$username, $username]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if ($user && password_verify($password, $user['password'])) {
            startAdminSession();
            
            $_SESSION['admin_id'] = $user['id'];
            $_SESSION['admin_user'] = $user['username'];
            $_SESSION['admin_email'] = $user['email'];
            $_SESSION['admin_role'] = $user['role'];
            
            // Mettre à jour last_login
            $sql = "UPDATE admin_users SET last_login = NOW() WHERE id = ?";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$user['id']]);
            
            return ['success' => true, 'user' => $user];
        }
        
        return ['success' => false, 'message' => 'Identifiants incorrects'];
        
    } catch (Exception $e) {
        return ['success' => false, 'message' => 'Erreur de connexion'];
    }
}

/**
 * Déconnexion admin
 */
function logoutAdmin() {
    startAdminSession();
    $_SESSION = [];
    session_destroy();
}

/**
 * Rediriger vers login si non connecté
 */
function requireAdminLogin() {
    if (!isAdminLoggedIn()) {
        header('Location: /admin/login.php');
        exit;
    }
}

/**
 * Obtenir l'utilisateur connecté
 */
function getCurrentAdmin() {
    if (!isAdminLoggedIn()) {
        return null;
    }
    
    try {
        $pdo = getDbConnection();
        $sql = "SELECT id, username, email, role FROM admin_users WHERE id = ?";
        $stmt = $pdo->prepare($sql);
        $stmt->execute([$_SESSION['admin_id']]);
        return $stmt->fetch(PDO::FETCH_ASSOC);
    } catch (Exception $e) {
        return null;
    }
}
