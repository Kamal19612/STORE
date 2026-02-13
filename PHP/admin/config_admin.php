<?php
/**
 * Admin Configuration
 */

// Include main config
require_once __DIR__ . '/../config.php';

// Admin settings
define('ADMIN_PATH', '/admin');
define('ADMIN_SESSION_NAME', 'sucre_admin_session');
define('ADMIN_SESSION_LIFETIME', 7200); // 2 heures

// Permissions par rôle
define('PERMISSIONS', [
    'super_admin' => [
        'view_dashboard',
        'view_orders',
        'edit_orders',
        'delete_orders',
        'view_users',
        'create_users',
        'edit_users',
        'delete_users',
        'view_slider',
        'edit_slider',
        'delete_slider'
    ],
    'admin' => [
        'view_dashboard',
        'view_orders',
        'edit_orders',
        'view_users',
        'view_slider',
        'edit_slider'
    ],
    'gestionnaire' => [
        'view_dashboard',
        'view_orders',
        'edit_orders'
    ]
]);

// Statuts de commande
define('ORDER_STATUSES', [
    'en_attente' => ['label' => 'En attente', 'color' => 'warning', 'icon' => 'clock'],
    'en_cours' => ['label' => 'En cours', 'color' => 'info', 'icon' => 'truck'],
    'livree' => ['label' => 'Livrée', 'color' => 'success', 'icon' => 'check-circle'],
    'annulee' => ['label' => 'Annulée', 'color' => 'danger', 'icon' => 'times-circle']
]);

// Rôles utilisateurs
define('USER_ROLES', [
    'super_admin' => 'Super Administrateur',
    'admin' => 'Administrateur',
    'gestionnaire' => 'Gestionnaire'
]);
