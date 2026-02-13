<?php
if (!defined('ADMIN_PAGE')) {
    die('Accès direct interdit');
}

$currentAdmin = getCurrentAdmin();
$pageName = $pageName ?? 'Dashboard';
?>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?= $pageName ?> - Admin SUCRE STORE</title>
    
    <!-- Bootstrap 5 -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    
    <!-- Font Awesome -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    
    <!-- DataTables -->
    <link rel="stylesheet" href="https://cdn.datatables.net/1.13.7/css/dataTables.bootstrap5.min.css">
    
    <style>
        :root {
            --primary: #f5ad41;
            --secondary: #242021;
            --sidebar-width: 250px;
        }
        
        body {
            overflow-x: hidden;
        }
        
        /* Sidebar */
        .sidebar {
            position: fixed;
            top: 0;
            left: 0;
            height: 100vh;
            width: var(--sidebar-width);
            background-color: var(--secondary);
            color: white;
            overflow-y: auto;
            transition: transform 0.3s;
            z-index: 1000;
        }
        
        .sidebar-header {
            padding: 20px;
            background-color: rgba(0,0,0,0.2);
            text-align: center;
            border-bottom: 1px solid rgba(255,255,255,0.1);
        }
        
        .sidebar-header h4 {
            margin: 0;
            color: var(--primary);
        }
        
        .sidebar-menu {
            padding: 0;
            margin: 0;
            list-style: none;
        }
        
        .sidebar-menu li a {
            display: block;
            padding: 15px 20px;
            color: white;
            text-decoration: none;
            transition: all 0.3s;
            border-left: 3px solid transparent;
        }
        
        .sidebar-menu li a:hover,
        .sidebar-menu li a.active {
            background-color: rgba(245, 173, 65, 0.1);
            border-left-color: var(--primary);
            color: var(--primary);
        }
        
        .sidebar-menu li a i {
            width: 20px;
            margin-right: 10px;
        }
        
        /* Main content */
        .main-content {
            margin-left: var(--sidebar-width);
            min-height: 100vh;
            background-color: #f8f9fa;
        }
        
        /* Top navbar */
        .top-navbar {
            background-color: white;
            padding: 15px 30px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        
        .content-wrapper {
            padding: 30px;
        }
        
        /* Cards */
        .stat-card {
            border: none;
            border-radius: 10px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            transition: transform 0.3s;
        }
        
        .stat-card:hover {
            transform: translateY(-5px);
        }
        
        .stat-card .card-body {
            padding: 20px;
        }
        
        .stat-icon {
            font-size: 2.5rem;
            opacity: 0.3;
        }
        
        /* Badge status */
        .badge-en_attente { background-color: #ffc107; }
        .badge-en_cours { background-color: #17a2b8; }
        .badge-livree { background-color: #28a745; }
        .badge-annulee { background-color: #dc3545; }
        
        /* Mobile */
        @media (max-width: 768px) {
            .sidebar {
                transform: translateX(-100%);
            }
            
            .sidebar.show {
                transform: translateX(0);
            }
            
            .main-content {
                margin-left: 0;
            }
            
            .mobile-menu-btn {
                display: block !important;
            }
        }
        
        .mobile-menu-btn {
            display: none;
        }
    </style>
</head>
<body>
    <!-- Sidebar -->
    <div class="sidebar" id="sidebar">
        <div class="sidebar-header">
            <h4><i class="fas fa-store"></i> SUCRE STORE</h4>
            <small class="text-muted">Administration</small>
        </div>
        
        <ul class="sidebar-menu">
            <li>
                <a href="index.php" class="<?= basename($_SERVER['PHP_SELF']) == 'index.php' ? 'active' : '' ?>">
                    <i class="fas fa-tachometer-alt"></i> Dashboard
                </a>
            </li>
            
            <?php if (hasPermission('view_orders')): ?>
            <li>
                <a href="orders.php" class="<?= basename($_SERVER['PHP_SELF']) == 'orders.php' ? 'active' : '' ?>">
                    <i class="fas fa-shopping-cart"></i> Commandes
                </a>
            </li>
            <?php endif; ?>
            
            <?php if (hasPermission('view_slider')): ?>
            <li>
                <a href="slider.php" class="<?= basename($_SERVER['PHP_SELF']) == 'slider.php' ? 'active' : '' ?>">
                    <i class="fas fa-images"></i> Slider Pub
                </a>
            </li>
            <?php endif; ?>
            
            <?php if (hasPermission('view_users')): ?>
            <li>
                <a href="users.php" class="<?= basename($_SERVER['PHP_SELF']) == 'users.php' ? 'active' : '' ?>">
                    <i class="fas fa-users"></i> Utilisateurs
                </a>
            </li>
            <?php endif; ?>
            
            <li>
                <a href="profile.php">
                    <i class="fas fa-user-circle"></i> Mon Profil
                </a>
            </li>
            
            <li>
                <a href="logout.php">
                    <i class="fas fa-sign-out-alt"></i> Déconnexion
                </a>
            </li>
        </ul>
    </div>
    
    <!-- Main Content -->
    <div class="main-content">
        <!-- Top Navbar -->
        <div class="top-navbar">
            <div>
                <button class="btn btn-link mobile-menu-btn" onclick="toggleSidebar()">
                    <i class="fas fa-bars"></i>
                </button>
                <h5 class="d-inline-block mb-0"><?= $pageName ?></h5>
            </div>
            
            <div class="dropdown">
                <button class="btn btn-link dropdown-toggle text-decoration-none" type="button" data-bs-toggle="dropdown">
                    <i class="fas fa-user-circle"></i> <?= htmlspecialchars($currentAdmin['username']) ?>
                    <span class="badge bg-secondary"><?= USER_ROLES[$currentAdmin['role']] ?></span>
                </button>
                <ul class="dropdown-menu dropdown-menu-end">
                    <li><a class="dropdown-item" href="profile.php"><i class="fas fa-user"></i> Mon profil</a></li>
                    <li><hr class="dropdown-divider"></li>
                    <li><a class="dropdown-item text-danger" href="logout.php"><i class="fas fa-sign-out-alt"></i> Déconnexion</a></li>
                </ul>
            </div>
        </div>
        
        <!-- Content -->
        <div class="content-wrapper">
