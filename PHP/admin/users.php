<?php
define('ADMIN_PAGE', true);
require_once __DIR__ . '/includes/auth.php';
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../db_connection.php';

requireAdminLogin();

if (!hasPermission('view_users')) {
    die('Accès non autorisé');
}

$pageName = 'Gestion des Utilisateurs';

// Récupérer tous les utilisateurs
try {
    $pdo = getDbConnection();
    $sql = "SELECT * FROM admin_users ORDER BY created_at DESC";
    $stmt = $pdo->query($sql);
    $users = $stmt->fetchAll(PDO::FETCH_ASSOC);
} catch (Exception $e) {
    $error = "Erreur lors du chargement des utilisateurs";
}

include 'includes/header.php';
?>

<div class="card">
    <div class="card-header bg-white">
        <div class="d-flex justify-content-between align-items-center">
            <h5 class="mb-0"><i class="fas fa-users"></i> Liste des Utilisateurs</h5>
            <?php if (hasPermission('create_users')): ?>
            <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#addUserModal">
                <i class="fas fa-plus"></i> Nouvel Utilisateur
            </button>
            <?php endif; ?>
        </div>
    </div>
    <div class="card-body">
        <?php if (isset($error)): ?>
        <div class="alert alert-danger"><?= $error ?></div>
        <?php endif; ?>
        
        <div class="table-responsive">
            <table id="usersTable" class="table table-striped table-hover">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Nom d'utilisateur</th>
                        <th>Email</th>
                        <th>Rôle</th>
                        <th>Statut</th>
                        <th>Dernière connexion</th>
                        <th>Créé le</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <?php foreach ($users as $user): ?>
                    <tr>
                        <td><?= $user['id'] ?></td>
                        <td><strong><?= htmlspecialchars($user['username']) ?></strong></td>
                        <td><?= htmlspecialchars($user['email']) ?></td>
                        <td>
                            <span class="badge bg-primary">
                                <?= USER_ROLES[$user['role']] ?>
                            </span>
                        </td>
                        <td>
                            <?php if ($user['active']): ?>
                            <span class="badge bg-success">Actif</span>
                            <?php else: ?>
                            <span class="badge bg-danger">Inactif</span>
                            <?php endif; ?>
                        </td>
                        <td>
                            <?= $user['last_login'] ? date('d/m/Y H:i', strtotime($user['last_login'])) : 'Jamais' ?>
                        </td>
                        <td><?= date('d/m/Y', strtotime($user['created_at'])) ?></td>
                        <td>
                            <div class="btn-group btn-group-sm">
                                <?php if (hasPermission('edit_users')): ?>
                                <button onclick="editUser(<?= $user['id'] ?>)" 
                                        class="btn btn-outline-warning" title="Modifier">
                                    <i class="fas fa-edit"></i>
                                </button>
                                <?php endif; ?>
                                
                                <?php if (hasPermission('delete_users') && $user['id'] != $_SESSION['admin_id']): ?>
                                <button onclick="deleteUser(<?= $user['id'] ?>)" 
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

<!-- Modal Ajout Utilisateur -->
<div class="modal fade" id="addUserModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Nouvel Utilisateur</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <form id="addUserForm">
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label">Nom d'utilisateur *</label>
                        <input type="text" name="username" class="form-control" required>
                    </div>
                    
                    <div class="mb-3">
                        <label class="form-label">Email *</label>
                        <input type="email" name="email" class="form-control" required>
                    </div>
                    
                    <div class="mb-3">
                        <label class="form-label">Mot de passe *</label>
                        <input type="password" name="password" class="form-control" required minlength="6">
                        <small class="text-muted">Minimum 6 caractères</small>
                    </div>
                    
                    <div class="mb-3">
                        <label class="form-label">Rôle *</label>
                        <select name="role" class="form-select" required>
                            <?php foreach (USER_ROLES as $key => $label): ?>
                            <option value="<?= $key ?>"><?= $label ?></option>
                            <?php endforeach; ?>
                        </select>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annuler</button>
                    <button type="submit" class="btn btn-primary">Créer</button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- Modal Modification Utilisateur -->
<div class="modal fade" id="editUserModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Modifier Utilisateur</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <form id="editUserForm">
                <input type="hidden" name="user_id" id="edit_user_id">
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label">Nom d'utilisateur *</label>
                        <input type="text" name="username" id="edit_username" class="form-control" required>
                    </div>
                    
                    <div class="mb-3">
                        <label class="form-label">Email *</label>
                        <input type="email" name="email" id="edit_email" class="form-control" required>
                    </div>
                    
                    <div class="mb-3">
                        <label class="form-label">Nouveau mot de passe</label>
                        <input type="password" name="password" id="edit_password" class="form-control" minlength="6">
                        <small class="text-muted">Laisser vide pour ne pas changer</small>
                    </div>
                    
                    <div class="mb-3">
                        <label class="form-label">Rôle *</label>
                        <select name="role" id="edit_role" class="form-select" required>
                            <?php foreach (USER_ROLES as $key => $label): ?>
                            <option value="<?= $key ?>"><?= $label ?></option>
                            <?php endforeach; ?>
                        </select>
                    </div>
                    
                    <div class="mb-3">
                        <div class="form-check">
                            <input type="checkbox" name="active" id="edit_active" class="form-check-input" value="1">
                            <label class="form-check-label" for="edit_active">Compte actif</label>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annuler</button>
                    <button type="submit" class="btn btn-primary">Enregistrer</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
$(document).ready(function() {
    $('#usersTable').DataTable({
        language: {
            url: '//cdn.datatables.net/plug-ins/1.13.7/i18n/fr-FR.json'
        },
        order: [[0, 'desc']]
    });
});

// Ajouter utilisateur
$('#addUserForm').on('submit', function(e) {
    e.preventDefault();
    
    $.ajax({
        url: 'api/users_handler.php',
        type: 'POST',
        data: $(this).serialize() + '&action=create',
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                location.reload();
            } else {
                alert('Erreur: ' + response.message);
            }
        }
    });
});

// Modifier utilisateur
function editUser(userId) {
    $.ajax({
        url: 'api/users_handler.php',
        type: 'POST',
        data: { action: 'get', user_id: userId },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                const user = response.user;
                $('#edit_user_id').val(user.id);
                $('#edit_username').val(user.username);
                $('#edit_email').val(user.email);
                $('#edit_role').val(user.role);
                $('#edit_active').prop('checked', user.active == 1);
                $('#editUserModal').modal('show');
            }
        }
    });
}

$('#editUserForm').on('submit', function(e) {
    e.preventDefault();
    
    $.ajax({
        url: 'api/users_handler.php',
        type: 'POST',
        data: $(this).serialize() + '&action=update',
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                location.reload();
            } else {
                alert('Erreur: ' + response.message);
            }
        }
    });
});

// Supprimer utilisateur
function deleteUser(userId) {
    if (!confirmDelete('Êtes-vous sûr de vouloir supprimer cet utilisateur ?')) {
        return;
    }
    
    $.ajax({
        url: 'api/users_handler.php',
        type: 'POST',
        data: { action: 'delete', user_id: userId },
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
</script>

<?php include 'includes/footer.php'; ?>
