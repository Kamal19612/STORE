<?php
define('ADMIN_PAGE', true);
require_once __DIR__ . '/includes/auth.php';
require_once __DIR__ . '/../db_connection.php';

requireAdminLogin();

if (!hasPermission('view_orders')) {
    die('Accès non autorisé');
}

$pageName = 'Gestion des Commandes';

// Récupérer toutes les commandes
try {
    $pdo = getDbConnection();
    $sql = "SELECT o.*, 
            (SELECT COUNT(*) FROM order_items WHERE order_id = o.id) as items_count
            FROM orders o 
            ORDER BY o.created_at DESC";
    $stmt = $pdo->query($sql);
    $orders = $stmt->fetchAll(PDO::FETCH_ASSOC);
} catch (Exception $e) {
    $error = "Erreur lors du chargement des commandes";
}

include 'includes/header.php';
?>

<div class="card">
    <div class="card-header bg-white">
        <div class="d-flex justify-content-between align-items-center">
            <h5 class="mb-0"><i class="fas fa-shopping-cart"></i> Liste des Commandes</h5>
            <div>
                <button class="btn btn-sm btn-success" onclick="exportOrders()">
                    <i class="fas fa-file-excel"></i> Exporter
                </button>
            </div>
        </div>
    </div>
    <div class="card-body">
        <?php if (isset($error)): ?>
        <div class="alert alert-danger"><?= $error ?></div>
        <?php endif; ?>
        
        <div class="table-responsive">
            <table id="ordersTable" class="table table-striped table-hover">
                <thead>
                    <tr>
                        <th>N° Commande</th>
                        <th>Client</th>
                        <th>Téléphone</th>
                        <th>Date</th>
                        <th>Articles</th>
                        <th>Total</th>
                        <th>Statut</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <?php foreach ($orders as $order): ?>
                    <tr>
                        <td><strong><?= htmlspecialchars($order['order_id']) ?></strong></td>
                        <td><?= htmlspecialchars($order['customer_name']) ?></td>
                        <td><?= htmlspecialchars($order['customer_phone']) ?></td>
                        <td><?= date('d/m/Y H:i', strtotime($order['created_at'])) ?></td>
                        <td><span class="badge bg-info"><?= $order['items_count'] ?> article(s)</span></td>
                        <td><strong><?= number_format($order['total'], 0) ?> FCFA</strong></td>
                        <td>
                            <select class="form-select form-select-sm badge-<?= $order['status'] ?>" 
                                    onchange="updateOrderStatus(<?= $order['id'] ?>, this.value)"
                                    style="width: auto; display: inline-block;">
                                <?php foreach (ORDER_STATUSES as $key => $status): ?>
                                <option value="<?= $key ?>" <?= $order['status'] == $key ? 'selected' : '' ?>>
                                    <?= $status['label'] ?>
                                </option>
                                <?php endforeach; ?>
                            </select>
                        </td>
                        <td>
                            <div class="btn-group btn-group-sm">
                                <a href="order_detail.php?id=<?= $order['id'] ?>" 
                                   class="btn btn-outline-primary" title="Détails">
                                    <i class="fas fa-eye"></i>
                                </a>
                                <button onclick="notifyCustomer(<?= $order['id'] ?>, '<?= htmlspecialchars($order['customer_phone']) ?>', '<?= htmlspecialchars($order['order_id']) ?>')" 
                                        class="btn btn-outline-success" title="Notifier">
                                    <i class="fab fa-whatsapp"></i>
                                </button>
                                <?php if (hasPermission('delete_orders')): ?>
                                <button onclick="deleteOrder(<?= $order['id'] ?>)" 
                                        class="btn btn-outline-danger" title="Supprimer">
                                    <i class="fas fa-trash"></i>
                                </button>
                                <?php endif; ?>
                            </div>
                        </td>
                    </tr>
                    <?php endforeach; ?>
                </tbody>
            </table>
        </div>
    </div>
</div>

<script>
$(document).ready(function() {
    $('#ordersTable').DataTable({
        language: {
            url: '//cdn.datatables.net/plug-ins/1.13.7/i18n/fr-FR.json'
        },
        order: [[3, 'desc']],
        pageLength: 25
    });
});

function updateOrderStatus(orderId, newStatus) {
    if (!confirm('Confirmer le changement de statut ?')) {
        location.reload();
        return;
    }
    
    $.ajax({
        url: 'api/orders_handler.php',
        type: 'POST',
        data: {
            action: 'update_status',
            order_id: orderId,
            status: newStatus
        },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                location.reload();
            } else {
                alert('Erreur: ' + response.message);
                location.reload();
            }
        },
        error: function() {
            alert('Erreur lors de la mise à jour');
            location.reload();
        }
    });
}

function notifyCustomer(orderId, phone, orderNumber) {
    const statuses = <?= json_encode(ORDER_STATUSES) ?>;
    
    // Récupérer le statut actuel
    $.ajax({
        url: 'api/orders_handler.php',
        type: 'POST',
        data: {
            action: 'get_order_status',
            order_id: orderId
        },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                const status = response.status;
                const statusLabel = statuses[status].label;
                
                let message = `Bonjour,

Votre commande #${orderNumber} sur SUCRE STORE est actuellement : *${statusLabel}*

`;
                
                if (status === 'en_cours') {
                    message += 'Votre commande est en cours de préparation et sera bientôt livrée.';
                } else if (status === 'livree') {
                    message += 'Votre commande a été livrée avec succès. Merci de votre confiance !';
                } else if (status === 'annulee') {
                    message += 'Votre commande a été annulée. Pour plus d\'informations, contactez-nous.';
                } else {
                    message += 'Nous vous tiendrons informé de l\'évolution de votre commande.';
                }
                
                message += `

SUCRE STORE
<?= STORE_INFO['phone'] ?>`;
                
                const whatsappUrl = `https://wa.me/${phone.replace(/\s/g, '')}?text=${encodeURIComponent(message)}`;
                window.open(whatsappUrl, '_blank');
            } else {
                alert('Erreur lors de la récupération du statut');
            }
        }
    });
}

function deleteOrder(orderId) {
    if (!confirmDelete('Êtes-vous sûr de vouloir supprimer cette commande ? Cette action est irréversible.')) {
        return;
    }
    
    $.ajax({
        url: 'api/orders_handler.php',
        type: 'POST',
        data: {
            action: 'delete',
            order_id: orderId
        },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                location.reload();
            } else {
                alert('Erreur: ' + response.message);
            }
        }
    });
}

function exportOrders() {
    window.location.href = 'api/orders_handler.php?action=export';
}
</script>

<?php include 'includes/footer.php'; ?>
