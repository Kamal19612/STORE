<?php
define('ADMIN_PAGE', true);
require_once __DIR__ . '/includes/auth.php';
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../db_connection.php';

requireAdminLogin();

$pageName = 'Mon Profil';
$success = '';
$error = '';

$currentUser = getCurrentAdmin();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    try {
        $pdo = getDbConnection();
        
        $username = trim($_POST['username']);
        $email = trim($_POST['email']);
        
        // Vérifier si username ou email existe déjà (pour un autre user)
        $sql = "SELECT COUNT(*) FROM admin_users WHERE (username = ? OR email = ?) AND id != ?";
        $stmt = $pdo->prepare($sql);
        $stmt->execute([$username, $email, $currentUser['id']]);
        if ($stmt->fetchColumn() > 0) {
            throw new Exception('Nom d\'utilisateur ou email déjà utilisé');
        }
        
        // Mettre à jour le profil
        if (!empty($_POST['new_password'])) {
            if ($_POST['new_password'] !== $_POST['confirm_password']) {
                throw new Exception('Les mots de passe ne correspondent pas');
            }
            
            $passwordHash = password_hash($_POST['new_password'], PASSWORD_DEFAULT);
            $sql = "UPDATE admin_users SET username = ?, email = ?, password = ? WHERE id = ?";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$username, $email, $passwordHash, $currentUser['id']]);
        } else {
            $sql = "UPDATE admin_users SET username = ?, email = ? WHERE id = ?";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$username, $email, $currentUser['id']]);
        }
        
        // Mettre à jour la session
        $_SESSION['admin_user'] = $username;
        $_SESSION['admin_email'] = $email;
        
        $success = 'Profil mis à jour avec succès';
        $currentUser = getCurrentAdmin();
        
    } catch (Exception $e) {
        $error = $e->getMessage();
    }
}

include 'includes/header.php';
?>

<div class="row justify-content-center">
    <div class="col-md-8">
        <?php if ($success): ?>
        <div class="alert alert-success alert-dismissible fade show">
            <i class="fas fa-check-circle"></i> <?= htmlspecialchars($success) ?>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
        <?php endif; ?>
        
        <?php if ($error): ?>
        <div class="alert alert-danger alert-dismissible fade show">
            <i class="fas fa-exclamation-circle"></i> <?= htmlspecialchars($error) ?>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
        <?php endif; ?>
        
        <div class="card">
            <div class="card-header bg-white">
                <h5 class="mb-0"><i class="fas fa-user-circle"></i> Mon Profil</h5>
            </div>
            <div class="card-body">
                <div class="row mb-4">
                    <div class="col-md-3 text-center">
                        <div class="mb-3">
                            <i class="fas fa-user-circle" style="font-size: 100px; color: var(--primary);"></i>
                        </div>
                        <h5><?= htmlspecialchars($currentUser['username']) ?></h5>
                        <p class="text-muted mb-0"><?= USER_ROLES[$currentUser['role']] ?></p>
                    </div>
                    
                    <div class="col-md-9">
                        <form method="POST">
                            <h6 class="mb-3">Informations du compte</h6>
                            
                            <div class="mb-3">
                                <label class="form-label">Nom d'utilisateur</label>
                                <input type="text" name="username" class="form-control" 
                                       value="<?= htmlspecialchars($currentUser['username']) ?>" required>
                            </div>
                            
                            <div class="mb-3">
                                <label class="form-label">Email</label>
                                <input type="email" name="email" class="form-control" 
                                       value="<?= htmlspecialchars($currentUser['email']) ?>" required>
                            </div>
                            
                            <div class="mb-3">
                                <label class="form-label">Rôle</label>
                                <input type="text" class="form-control" 
                                       value="<?= USER_ROLES[$currentUser['role']] ?>" disabled>
                            </div>
                            
                            <hr>
                            
                            <h6 class="mb-3">Changer le mot de passe</h6>
                            <p class="text-muted small">Laissez vide si vous ne souhaitez pas changer votre mot de passe</p>
                            
                            <div class="mb-3">
                                <label class="form-label">Nouveau mot de passe</label>
                                <input type="password" name="new_password" class="form-control" minlength="6">
                                <small class="text-muted">Minimum 6 caractères</small>
                            </div>
                            
                            <div class="mb-3">
                                <label class="form-label">Confirmer le mot de passe</label>
                                <input type="password" name="confirm_password" class="form-control" minlength="6">
                            </div>
                            
                            <hr>
                            
                            <button type="submit" class="btn btn-primary">
                                <i class="fas fa-save"></i> Enregistrer les modifications
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
        
        <div class="card mt-3">
            <div class="card-header bg-white">
                <h5 class="mb-0"><i class="fas fa-info-circle"></i> Informations</h5>
            </div>
            <div class="card-body">
                <div class="row">
                    <div class="col-md-6">
                        <p><strong>Compte créé le:</strong><br>
                        <?= date('d/m/Y à H:i', strtotime($currentUser['created_at'] ?? 'now')) ?></p>
                    </div>
                    <div class="col-md-6">
                        <p><strong>Dernière connexion:</strong><br>
                        <?= isset($currentUser['last_login']) && $currentUser['last_login'] 
                            ? date('d/m/Y à H:i', strtotime($currentUser['last_login'])) 
                            : 'Jamais' ?></p>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<?php include 'includes/footer.php'; ?>
