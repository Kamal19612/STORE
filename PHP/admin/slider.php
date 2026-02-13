<?php
define('ADMIN_PAGE', true);
require_once __DIR__ . '/includes/auth.php';
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../db_connection.php';

requireAdminLogin();

if (!hasPermission('view_slider')) {
    die('Accès non autorisé');
}

$pageName = 'Gestion Slider Pub';

// Récupérer toutes les images
try {
    $pdo = getDbConnection();
    $sql = "SELECT * FROM slider_images ORDER BY order_position ASC, created_at DESC";
    $stmt = $pdo->query($sql);
    $slides = $stmt->fetchAll(PDO::FETCH_ASSOC);
} catch (Exception $e) {
    $error = "Erreur lors du chargement des slides";
}

include 'includes/header.php';
?>

<div class="card">
    <div class="card-header bg-white">
        <div class="d-flex justify-content-between align-items-center">
            <h5 class="mb-0"><i class="fas fa-images"></i> Images du Slider</h5>
            <?php if (hasPermission('edit_slider')): ?>
            <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#addSlideModal">
                <i class="fas fa-plus"></i> Nouvelle Image
            </button>
            <?php endif; ?>
        </div>
    </div>
    <div class="card-body">
        <div class="alert alert-info">
            <i class="fas fa-info-circle"></i> 
            <strong>Dimensions recommandées :</strong> 1920x400px | 
            <strong>Format :</strong> JPG, PNG, WebP | 
            <strong>Poids max :</strong> 500KB
        </div>
        
        <?php if (isset($error)): ?>
        <div class="alert alert-danger"><?= $error ?></div>
        <?php endif; ?>
        
        <?php if (empty($slides)): ?>
        <div class="text-center py-5">
            <i class="fas fa-images text-muted" style="font-size: 64px;"></i>
            <p class="text-muted mt-3">Aucune image dans le slider</p>
        </div>
        <?php else: ?>
        <div class="table-responsive">
            <table class="table table-hover">
                <thead>
                    <tr>
                        <th style="width: 80px;">Ordre</th>
                        <th style="width: 150px;">Aperçu</th>
                        <th>Titre</th>
                        <th>Lien</th>
                        <th style="width: 100px;">Statut</th>
                        <th style="width: 150px;">Actions</th>
                    </tr>
                </thead>
                <tbody id="slidesList">
                    <?php foreach ($slides as $slide): ?>
                    <tr data-slide-id="<?= $slide['id'] ?>">
                        <td>
                            <div class="d-flex flex-column gap-1">
                                <button class="btn btn-sm btn-outline-secondary" onclick="moveSlide(<?= $slide['id'] ?>, 'up')" title="Monter">
                                    <i class="fas fa-arrow-up"></i>
                                </button>
                                <span class="text-center"><strong><?= $slide['order_position'] ?></strong></span>
                                <button class="btn btn-sm btn-outline-secondary" onclick="moveSlide(<?= $slide['id'] ?>, 'down')" title="Descendre">
                                    <i class="fas fa-arrow-down"></i>
                                </button>
                            </div>
                        </td>
                        <td>
                            <img src="<?= htmlspecialchars($slide['image_url']) ?>" 
                                 alt="Slide" 
                                 class="img-thumbnail"
                                 style="max-height: 80px; width: auto;">
                        </td>
                        <td>
                            <?= htmlspecialchars($slide['title'] ?: 'Sans titre') ?>
                        </td>
                        <td>
                            <?php if ($slide['link']): ?>
                            <a href="<?= htmlspecialchars($slide['link']) ?>" target="_blank" class="text-primary">
                                <i class="fas fa-external-link-alt"></i> Voir
                            </a>
                            <?php else: ?>
                            <span class="text-muted">Aucun</span>
                            <?php endif; ?>
                        </td>
                        <td>
                            <div class="form-check form-switch">
                                <input class="form-check-input" type="checkbox" 
                                       <?= $slide['active'] ? 'checked' : '' ?>
                                       onchange="toggleSlide(<?= $slide['id'] ?>, this.checked)">
                            </div>
                        </td>
                        <td>
                            <div class="btn-group btn-group-sm">
                                <?php if (hasPermission('edit_slider')): ?>
                                <button onclick="editSlide(<?= $slide['id'] ?>)" 
                                        class="btn btn-outline-warning" title="Modifier">
                                    <i class="fas fa-edit"></i>
                                </button>
                                <?php endif; ?>
                                
                                <?php if (hasPermission('delete_slider')): ?>
                                <button onclick="deleteSlide(<?= $slide['id'] ?>)" 
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
        <?php endif; ?>
    </div>
</div>

<!-- Modal Ajout Slide -->
<div class="modal fade" id="addSlideModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Nouvelle Image</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <form id="addSlideForm">
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label">URL de l'image *</label>
                        <input type="url" name="image_url" class="form-control" required 
                               placeholder="https://exemple.com/image.jpg">
                        <small class="text-muted">Uploadez d'abord l'image sur votre serveur ou utilisez un hébergeur d'images</small>
                    </div>
                    
                    <div class="mb-3">
                        <label class="form-label">Titre (optionnel)</label>
                        <input type="text" name="title" class="form-control" 
                               placeholder="Ex: Promotion Spéciale">
                    </div>
                    
                    <div class="mb-3">
                        <label class="form-label">Lien de redirection (optionnel)</label>
                        <input type="url" name="link" class="form-control" 
                               placeholder="https://exemple.com/promo">
                        <small class="text-muted">URL vers laquelle rediriger quand on clique sur l'image</small>
                    </div>
                    
                    <div class="mb-3">
                        <label class="form-label">Position</label>
                        <input type="number" name="order_position" class="form-control" value="0" min="0">
                        <small class="text-muted">Plus le nombre est petit, plus l'image apparaît en premier</small>
                    </div>
                    
                    <div class="mb-3">
                        <div class="form-check">
                            <input type="checkbox" name="active" class="form-check-input" value="1" checked id="add_active">
                            <label class="form-check-label" for="add_active">Activer immédiatement</label>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annuler</button>
                    <button type="submit" class="btn btn-primary">Ajouter</button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- Modal Modification Slide -->
<div class="modal fade" id="editSlideModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Modifier Image</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <form id="editSlideForm">
                <input type="hidden" name="slide_id" id="edit_slide_id">
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label">URL de l'image *</label>
                        <input type="url" name="image_url" id="edit_image_url" class="form-control" required>
                    </div>
                    
                    <div class="mb-3">
                        <label class="form-label">Titre (optionnel)</label>
                        <input type="text" name="title" id="edit_title" class="form-control">
                    </div>
                    
                    <div class="mb-3">
                        <label class="form-label">Lien de redirection (optionnel)</label>
                        <input type="url" name="link" id="edit_link" class="form-control">
                    </div>
                    
                    <div class="mb-3">
                        <label class="form-label">Position</label>
                        <input type="number" name="order_position" id="edit_order" class="form-control" min="0">
                    </div>
                    
                    <div class="mb-3">
                        <img id="edit_preview" src="" alt="Aperçu" class="img-fluid" style="max-height: 200px;">
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


<?php include 'includes/footer.php'; ?>

<script>
$(document).ready(function() {
    // Ajouter slide
    $('#addSlideForm').on('submit', function(e) {
        e.preventDefault();
        
        $.ajax({
            url: 'api/slider_handler.php',
            type: 'POST',
            data: $(this).serialize() + '&action=create',
            dataType: 'json',
            success: function(response) {
                if (response.success) {
                    location.reload();
                } else {
                    alert('Erreur: ' + response.message);
                }
            },
            error: function() {
                alert('Erreur de connexion');
            }
        });
    });

    // Modifier slide
    $('#editSlideForm').on('submit', function(e) {
        e.preventDefault();
        
        $.ajax({
            url: 'api/slider_handler.php',
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
});

// Modifier slide
function editSlide(slideId) {
    $.ajax({
        url: 'api/slider_handler.php',
        type: 'POST',
        data: { action: 'get', slide_id: slideId },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                const slide = response.slide;
                $('#edit_slide_id').val(slide.id);
                $('#edit_image_url').val(slide.image_url);
                $('#edit_title').val(slide.title);
                $('#edit_link').val(slide.link);
                $('#edit_order').val(slide.order_position);
                $('#edit_preview').attr('src', slide.image_url);
                $('#editSlideModal').modal('show');
            }
        }
    });
}

// Activer/Désactiver
function toggleSlide(slideId, active) {
    $.ajax({
        url: 'api/slider_handler.php',
        type: 'POST',
        data: { action: 'toggle', slide_id: slideId, active: active ? 1 : 0 },
        dataType: 'json',
        success: function(response) {
            if (!response.success) {
                alert('Erreur: ' + response.message);
                location.reload();
            }
        }
    });
}

// Déplacer slide
function moveSlide(slideId, direction) {
    $.ajax({
        url: 'api/slider_handler.php',
        type: 'POST',
        data: { action: 'move', slide_id: slideId, direction: direction },
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

// Supprimer slide
function deleteSlide(slideId) {
    if (!confirmDelete('Êtes-vous sûr de vouloir supprimer cette image ?')) {
        return;
    }
    
    $.ajax({
        url: 'api/slider_handler.php',
        type: 'POST',
        data: { action: 'delete', slide_id: slideId },
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