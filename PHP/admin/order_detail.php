<?php
define('ADMIN_PAGE', true);
require_once __DIR__ . '/includes/auth.php';
require_once __DIR__ . '/../db_connection.php';

requireAdminLogin();

$orderId = intval($_GET['id'] ?? 0);
$pageName = 'Détail Commande';

try {
    $pdo = getDbConnection();
    
    // Récupérer la commande
    $sql = "SELECT * FROM orders WHERE id = ?";
    $stmt = $pdo->prepare($sql);
    $stmt->execute([$orderId]);
    $order = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$order) {
        header('Location: orders.php');
        exit;
    }
    
    // Récupérer les articles
    $sql = "SELECT * FROM order_items WHERE order_id = ?";
    $stmt = $pdo->prepare($sql);
    $stmt->execute([$orderId]);
    $items = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Récupérer l'historique des statuts
    $sql = "SELECT h.*, a.username 
            FROM order_status_history h 
            LEFT JOIN admin_users a ON h.created_by = a.id 
            WHERE h.order_id = ? 
            ORDER BY h.created_at DESC";
    $stmt = $pdo->prepare($sql);
    $stmt->execute([$orderId]);
    $history = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
} catch (Exception $e) {
    $error = "Erreur lors du chargement de la commande";
}

include 'includes/header.php';
?>

<div class="mb-3">
    <a href="orders.php" class="btn btn-secondary">
        <i class="fas fa-arrow-left"></i> Retour aux commandes
    </a>
</div>

<div class="row g-3">
    <!-- Informations commande -->
    <div class="col-md-8">
        <div class="card mb-3">
            <div class="card-header bg-white">
                <h5 class="mb-0">
                    <i class="fas fa-info-circle"></i> Commande #<?= htmlspecialchars($order['order_id']) ?>
                </h5>
            </div>
            <div class="card-body">
                <div class="row mb-3">
                    <div class="col-md-6">
                        <p class="mb-2"><strong>Date:</strong> <?= date('d/m/Y à H:i', strtotime($order['created_at'])) ?></p>
                        <p class="mb-2"><strong>Statut:</strong> 
                            <span class="badge badge-<?= $order['status'] ?>">
                                <i class="fas fa-<?= ORDER_STATUSES[$order['status']]['icon'] ?>"></i>
                                <?= ORDER_STATUSES[$order['status']]['label'] ?>
                            </span>
                        </p>
                    </div>
                    <div class="col-md-6">
                        <p class="mb-2"><strong>Sous-total:</strong> <?= number_format($order['subtotal'], 0) ?> FCFA</p>
                        <p class="mb-2"><strong>Taxe:</strong> <?= number_format($order['tax'], 0) ?> FCFA</p>
                        <p class="mb-2"><strong class="text-primary">Total:</strong> <strong class="text-primary"><?= number_format($order['total'], 0) ?> FCFA</strong></p>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Articles commandés -->
        <div class="card mb-3">
            <div class="card-header bg-white">
                <h5 class="mb-0"><i class="fas fa-shopping-bag"></i> Articles</h5>
            </div>
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Article</th>
                                <th>Prix unitaire</th>
                                <th>Quantité</th>
                                <th>Total</th>
                            </tr>
                        </thead>
                        <tbody>
                            <?php foreach ($items as $item): ?>
                            <tr>
                                <td>
                                    <strong><?= htmlspecialchars($item['item_name']) ?></strong>
                                    <?php if ($item['item_description']): ?>
                                    <br><small class="text-muted"><?= htmlspecialchars($item['item_description']) ?></small>
                                    <?php endif; ?>
                                </td>
                                <td><?= number_format($item['price'], 0) ?> FCFA</td>
                                <td><span class="badge bg-secondary"><?= $item['quantity'] ?></span></td>
                                <td><strong><?= number_format($item['total'], 0) ?> FCFA</strong></td>
                            </tr>
                            <?php endforeach; ?>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
    
    <!-- Informations client -->
    <div class="col-md-4">
        <div class="card mb-3">
            <div class="card-header bg-white">
                <h5 class="mb-0"><i class="fas fa-user"></i> Client</h5>
            </div>
            <div class="card-body">
                <p class="mb-2"><strong>Nom:</strong><br><?= htmlspecialchars($order['customer_name']) ?></p>
                <p class="mb-2"><strong>Téléphone:</strong><br>
                    <a href="tel:<?= htmlspecialchars($order['customer_phone']) ?>">
                        <?= htmlspecialchars($order['customer_phone']) ?>
                    </a>
                </p>
                <?php if ($order['customer_notes']): ?>
                <p class="mb-0"><strong>Notes:</strong><br>
                    <small><?= nl2br(htmlspecialchars($order['customer_notes'])) ?></small>
                </p>
                <?php endif; ?>
                
                <?php if (!empty($order['customer_latitude']) && !empty($order['customer_longitude'])): ?>
                <div class="mb-3">
                    <strong>Position GPS:</strong><br>
                    <p class="text-sm">
                        Latitude: <?= htmlspecialchars($order['customer_latitude']) ?><br>
                        Longitude: <?= htmlspecialchars($order['customer_longitude']) ?>
                    </p>
                    <a href="https://www.google.com/maps?q=<?= $order['customer_latitude'] ?>,<?= $order['customer_longitude'] ?>" 
                       target="_blank" 
                       class="text-blue-600 hover:text-blue-800">
                        <i class="fas fa-map-marker-alt"></i> Voir sur Google Maps
                    </a>
                </div>
                <?php endif; ?>
                
                <hr>
                
                <button onclick="notifyCustomer()" class="btn btn-success w-100 mb-2">
                    <i class="fab fa-whatsapp"></i> Notifier sur WhatsApp
                </button>
            </div>
        </div>
        
        <!-- Changer le statut -->
        <?php if (hasPermission('edit_orders')): ?>
        <div class="card mb-3">
            <div class="card-header bg-white">
                <h5 class="mb-0"><i class="fas fa-edit"></i> Modifier le statut</h5>
            </div>
            <div class="card-body">
                <select class="form-select" id="statusSelect">
                    <?php foreach (ORDER_STATUSES as $key => $status): ?>
                    <option value="<?= $key ?>" <?= $order['status'] == $key ? 'selected' : '' ?>>
                        <?= $status['label'] ?>
                    </option>
                    <?php endforeach; ?>
                </select>
                <button onclick="updateStatus()" class="btn btn-primary w-100 mt-2">
                    Mettre à jour
                </button>
            </div>
        </div>
        <?php endif; ?>
        
        <!-- Historique -->
        <?php if (!empty($history)): ?>
        <div class="card">
            <div class="card-header bg-white">
                <h5 class="mb-0"><i class="fas fa-history"></i> Historique</h5>
            </div>
            <div class="card-body">
                <?php foreach ($history as $h): ?>
                <div class="mb-2">
                    <span class="badge badge-<?= $h['status'] ?>"><?= ORDER_STATUSES[$h['status']]['label'] ?></span>
                    <br>
                    <small class="text-muted">
                        <?= date('d/m/Y H:i', strtotime($h['created_at'])) ?>
                        <?php if ($h['username']): ?>
                        par <?= htmlspecialchars($h['username']) ?>
                        <?php endif; ?>
                    </small>
                </div>
                <?php endforeach; ?>
            </div>
        </div>
        <?php endif; ?>
    </div>
</div>

<script>
function updateStatus() {
    const newStatus = $('#statusSelect').val();
    
    if (!confirm('Confirmer le changement de statut ?')) {
        return;
    }
    
    $.ajax({
        url: 'api/orders_handler.php',
        type: 'POST',
        data: {
            action: 'update_status',
            order_id: <?= $orderId ?>,
            status: newStatus
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

function notifyCustomer() {
    const phone = '<?= $order['customer_phone'] ?>';
    const orderNumber = '<?= $order['order_id'] ?>';
    const status = '<?= $order['status'] ?>';
    const statusLabel = '<?= ORDER_STATUSES[$order['status']]['label'] ?>';
    
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
}
</script>

<?php include 'includes/footer.php'; ?>
