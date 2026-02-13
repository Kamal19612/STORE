<?php
define('ADMIN_PAGE', true);
require_once __DIR__ . '/includes/auth.php';
require_once __DIR__ . '/../db_connection.php';

requireAdminLogin();

$pageName = 'Dashboard';

// Récupérer les statistiques
try {
    $pdo = getDbConnection();
    
    // Total commandes
    $stmt = $pdo->query("SELECT COUNT(*) as total FROM orders");
    $totalOrders = $stmt->fetch()['total'];
    
    // Commandes aujourd'hui
    $stmt = $pdo->query("SELECT COUNT(*) as total FROM orders WHERE DATE(created_at) = CURDATE()");
    $ordersToday = $stmt->fetch()['total'];
    
    // Chiffre d'affaires total
    $stmt = $pdo->query("SELECT SUM(total) as revenue FROM orders WHERE status != 'annulee'");
    $totalRevenue = $stmt->fetch()['revenue'] ?? 0;
    
    // Chiffre d'affaires ce mois
    $stmt = $pdo->query("SELECT SUM(total) as revenue FROM orders WHERE MONTH(created_at) = MONTH(CURDATE()) AND YEAR(created_at) = YEAR(CURDATE()) AND status != 'annulee'");
    $monthRevenue = $stmt->fetch()['revenue'] ?? 0;
    
    // Répartition par statut
    $stmt = $pdo->query("SELECT status, COUNT(*) as count FROM orders GROUP BY status");
    $statusData = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Dernières commandes
    $stmt = $pdo->query("SELECT * FROM orders ORDER BY created_at DESC LIMIT 10");
    $recentOrders = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Graphique des ventes (7 derniers jours)
    $stmt = $pdo->query("
        SELECT DATE(created_at) as date, COUNT(*) as count, SUM(total) as revenue 
        FROM orders 
        WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
        GROUP BY DATE(created_at)
        ORDER BY date ASC
    ");
    $salesData = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
} catch (Exception $e) {
    $error = "Erreur lors du chargement des statistiques";
}

include 'includes/header.php';
?>

<div class="row g-3 mb-4">
    <!-- Card Total Commandes -->
    <div class="col-md-3">
        <div class="card stat-card border-start border-primary border-4">
            <div class="card-body">
                <div class="d-flex justify-content-between">
                    <div>
                        <h6 class="text-muted mb-1">Total Commandes</h6>
                        <h2 class="mb-0"><?= number_format($totalOrders) ?></h2>
                    </div>
                    <div class="stat-icon text-primary">
                        <i class="fas fa-shopping-cart"></i>
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    <!-- Card Aujourd'hui -->
    <div class="col-md-3">
        <div class="card stat-card border-start border-warning border-4">
            <div class="card-body">
                <div class="d-flex justify-content-between">
                    <div>
                        <h6 class="text-muted mb-1">Aujourd'hui</h6>
                        <h2 class="mb-0"><?= number_format($ordersToday) ?></h2>
                    </div>
                    <div class="stat-icon text-warning">
                        <i class="fas fa-calendar-day"></i>
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    <!-- Card CA Total -->
    <div class="col-md-3">
        <div class="card stat-card border-start border-success border-4">
            <div class="card-body">
                <div class="d-flex justify-content-between">
                    <div>
                        <h6 class="text-muted mb-1">CA Total</h6>
                        <h2 class="mb-0"><?= number_format($totalRevenue, 0) ?></h2>
                        <small class="text-muted">FCFA</small>
                    </div>
                    <div class="stat-icon text-success">
                        <i class="fas fa-money-bill-wave"></i>
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    <!-- Card CA Mois -->
    <div class="col-md-3">
        <div class="card stat-card border-start border-info border-4">
            <div class="card-body">
                <div class="d-flex justify-content-between">
                    <div>
                        <h6 class="text-muted mb-1">Ce Mois</h6>
                        <h2 class="mb-0"><?= number_format($monthRevenue, 0) ?></h2>
                        <small class="text-muted">FCFA</small>
                    </div>
                    <div class="stat-icon text-info">
                        <i class="fas fa-chart-line"></i>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="row g-3 mb-4">
    <!-- Graphique des ventes -->
    <div class="col-md-8">
        <div class="card">
            <div class="card-header bg-white">
                <h5 class="mb-0"><i class="fas fa-chart-area"></i> Ventes des 7 derniers jours</h5>
            </div>
            <div class="card-body">
                <canvas id="salesChart" height="80"></canvas>
            </div>
        </div>
    </div>
    
    <!-- Répartition par statut -->
    <div class="col-md-4">
        <div class="card">
            <div class="card-header bg-white">
                <h5 class="mb-0"><i class="fas fa-chart-pie"></i> Statuts</h5>
            </div>
            <div class="card-body">
                <canvas id="statusChart"></canvas>
            </div>
        </div>
    </div>
</div>

<!-- Dernières commandes -->
<div class="card">
    <div class="card-header bg-white d-flex justify-content-between align-items-center">
        <h5 class="mb-0"><i class="fas fa-list"></i> Dernières Commandes</h5>
        <a href="orders.php" class="btn btn-sm btn-primary">Voir tout</a>
    </div>
    <div class="card-body">
        <div class="table-responsive">
            <table class="table table-hover">
                <thead>
                    <tr>
                        <th>N° Commande</th>
                        <th>Client</th>
                        <th>Téléphone</th>
                        <th>Date</th>
                        <th>Total</th>
                        <th>Statut</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    <?php foreach ($recentOrders as $order): ?>
                    <tr>
                        <td><strong><?= htmlspecialchars($order['order_id']) ?></strong></td>
                        <td><?= htmlspecialchars($order['customer_name']) ?></td>
                        <td><?= htmlspecialchars($order['customer_phone']) ?></td>
                        <td><?= date('d/m/Y H:i', strtotime($order['created_at'])) ?></td>
                        <td><strong><?= number_format($order['total'], 0) ?> FCFA</strong></td>
                        <td>
                            <span class="badge badge-<?= $order['status'] ?>">
                                <?= ORDER_STATUSES[$order['status']]['label'] ?>
                            </span>
                        </td>
                        <td>
                            <a href="order_detail.php?id=<?= $order['id'] ?>" class="btn btn-sm btn-outline-primary">
                                <i class="fas fa-eye"></i>
                            </a>
                        </td>
                    </tr>
                    <?php endforeach; ?>
                </tbody>
            </table>
        </div>
    </div>
</div>

<script>
// Graphique des ventes
const salesCtx = document.getElementById('salesChart').getContext('2d');
const salesChart = new Chart(salesCtx, {
    type: 'line',
    data: {
        labels: <?= json_encode(array_column($salesData, 'date')) ?>,
        datasets: [{
            label: 'Ventes (FCFA)',
            data: <?= json_encode(array_column($salesData, 'revenue')) ?>,
            borderColor: '#f5ad41',
            backgroundColor: 'rgba(245, 173, 65, 0.1)',
            tension: 0.4
        }]
    },
    options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
            legend: {
                display: false
            }
        }
    }
});

// Graphique statuts
const statusCtx = document.getElementById('statusChart').getContext('2d');
const statusChart = new Chart(statusCtx, {
    type: 'doughnut',
    data: {
        labels: <?= json_encode(array_map(function($s) { return ORDER_STATUSES[$s['status']]['label']; }, $statusData)) ?>,
        datasets: [{
            data: <?= json_encode(array_column($statusData, 'count')) ?>,
            backgroundColor: ['#ffc107', '#17a2b8', '#28a745', '#dc3545']
        }]
    }
});
</script>

<?php include 'includes/footer.php'; ?>
