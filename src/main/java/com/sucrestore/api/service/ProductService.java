package com.sucrestore.api.service;

import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.sucrestore.api.dto.ImportSummary;
import com.sucrestore.api.dto.ProductRequest;
import com.sucrestore.api.dto.ProductResponse;
import com.sucrestore.api.entity.Category;
import com.sucrestore.api.entity.Product;
import com.sucrestore.api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service gérant la logique métier pour les produits.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    /**
     * Récupère une liste paginée de tous les produits actifs. Transforme les
     * entités en DTO pour l'API.
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllActiveProducts(Pageable pageable) {
        return productRepository.findByActiveTrue(pageable)
                .map(this::mapToResponse);
    }

    /**
     * Récupère une liste paginée de produits par catégorie.
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryIdAndActiveTrue(categoryId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Recherche des produits par nom (recherche textuelle simple).
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String query, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCaseAndActiveTrue(query, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Récupère le détail d'un produit via son slug.
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Produit introuvable : " + slug));

        return mapToResponse(product);
    }

    /**
     * Mapper utilitaire : Convertit une Entité Product en DTO ProductResponse.
     */
    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .shortDescription(product.getShortDescription())
                .description(product.getDescription())
                .volumeWeight(product.getVolumeWeight()) // Volume/Poids
                .price(product.getPrice())
                .oldPrice(product.getOldPrice())
                .mainImage(product.getMainImage())
                .categoryName(product.getCategory().getName())
                .categorySlug(product.getCategory().getSlug())
                .categoryId(product.getCategory().getId()) // Mappage ID Catégorie
                .stock(product.getStock()) // Mappage Stock réel
                .externalId(product.getExternalId()) // Mappage External ID
                .available(product.getStock() > 0)
                .active(product.isActive()) // Mappage statut actif réel
                .created(false) // Par défaut false, surchargé lors de l'import
                .build();
    }

    /**
     * Récupère les N produits les plus commandés (pour le carrousel public).
     * Si aucune commande n'existe, retourne les produits actifs les plus récents avec images.
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getTopOrderedProducts(int limit) {
        List<Product> topProducts = productRepository.findTopOrderedProducts(PageRequest.of(0, limit));

        // Fallback : Si aucun produit commandé ou moins que la limite, compléter avec les produits actifs récents
        if (topProducts.size() < limit) {
            Page<Product> recentProducts = productRepository
                .findByActiveTrueAndMainImageIsNotNullOrderByIdDesc(PageRequest.of(0, limit));

            // Fusionner les listes en évitant les doublons
            List<Product> combined = new java.util.ArrayList<>(topProducts);
            for (Product product : recentProducts.getContent()) {
                if (!combined.contains(product) && combined.size() < limit) {
                    combined.add(product);
                }
            }
            topProducts = combined;
        }

        return topProducts.stream()
                .filter(p -> p.getMainImage() != null && !p.getMainImage().isEmpty())
                .map(this::mapToResponse)
                .toList();
    }

    // --- Méthodes Admin ---
    /**
     * Récupère tous les produits (actifs et inactifs) pour l'admin.
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    /**
     * Récupère un produit par son ID pour l'admin.
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit introuvable ID: " + id));
        return mapToResponse(product);
    }

    @Autowired
    private com.sucrestore.api.repository.CategoryRepository categoryRepository;

    /**
     * Crée un nouveau produit.
     */
    @Transactional
    public ProductResponse createProduct(com.sucrestore.api.dto.ProductRequest request, String imageUrl) {
        Product product = Product.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .shortDescription(request.getShortDescription())
                .description(request.getDescription())
                .price(request.getPrice())
                .oldPrice(request.getOldPrice())
                .stock(request.getStock())
                .active(request.isActive())
                .mainImage(imageUrl) // URL ou chemin fichier
                .category(resolveCategory(request))
                .build();

        return mapToResponse(productRepository.save(product));
    }

    /**
     * Met à jour un produit existant.
     */
    @Transactional
    public ProductResponse updateProduct(Long id, com.sucrestore.api.dto.ProductRequest request, String imageUrl) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit introuvable ID: " + id));

        product.setName(request.getName());
        product.setSlug(request.getSlug());
        product.setShortDescription(request.getShortDescription());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setOldPrice(request.getOldPrice());
        product.setStock(request.getStock());
        product.setActive(request.isActive());

        // Mettre à jour l'image seulement si une nouvelle est fournie
        if (imageUrl != null && !imageUrl.isEmpty()) {
            product.setMainImage(imageUrl);
        }

        // Gestion Catégorie (ID ou Nom)
        Category category = resolveCategory(request);
        if (!product.getCategory().getId().equals(category.getId())) {
            product.setCategory(category);
        }

        return mapToResponse(productRepository.save(product));
    }

    /**
     * Archive un produit (Soft Delete).
     */
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit introuvable ID: " + id));

        product.setActive(false); // Soft delete
        productRepository.save(product);
    }

    /**
     * Supprime définitivement tous les produits du catalogue.
     * Supprime d'abord les order_items liés pour respecter les contraintes FK,
     * puis supprime tous les produits.
     * Action réservée au SUPER_ADMIN.
     */
    @Transactional
    public int deleteAllProducts() {
        int count = (int) productRepository.count();
        // Supprimer les order_items référençant des produits (via requête native)
        productRepository.deleteAllOrderItemsReferences();
        productRepository.deleteAll();
        return count;
    }

    /**
     * Helper pour trouver ou créer une catégorie selon la requête.
     */
    private Category resolveCategory(com.sucrestore.api.dto.ProductRequest request) {
        // Priorité 1: Recherche par ID si présent
        if (request.getCategoryId() != null) {
            return categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Catégorie introuvable ID: " + request.getCategoryId()));
        }

        // Priorité 2: Recherche ou Création par Nom
        if (request.getCategoryName() != null && !request.getCategoryName().trim().isEmpty()) {
            String name = request.getCategoryName().trim();
            return categoryRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> createNewCategory(name));
        }

        throw new RuntimeException("Une catégorie (ID ou Nom) est requise.");
    }

    private Category createNewCategory(String name) {
        // Crée un slug basique
        String slug = name.toLowerCase()
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        Category newCategory = Category.builder()
                .name(name)
                .slug(slug)
                .active(true)
                .build();

        return categoryRepository.save(newCategory);
    }

    /**
     * Importe un produit (Création ou Mise à jour basée sur l'ExternalId puis
     * Slug). Méthode transactionnelle isolée pour permettre le traitement par
     * lots "Best Effort".
     */
    @Transactional
    public ProductResponse importProduct(com.sucrestore.api.dto.ProductRequest request, String imageUrl, String externalId) {
        log.info("🚀 [IMPORT] ImportProduit - Nom: {}, ExternalID: {}, Slug: {}, Stock: {}, Active: {}",
            request.getName(), externalId, request.getSlug(), request.getStock(), request.isActive());

        // Validation basique
        if (request.getSlug() == null || request.getSlug().isEmpty()) {
            log.error("❌ [IMPORT] Slug manquant pour le produit: {}", request.getName());
            throw new RuntimeException("Le slug est obligatoire pour l'import");
        }

        // PRIORITÉ 1: Recherche par ExternalId (si fourni et non vide)
        if (externalId != null && !externalId.trim().isEmpty()) {
            log.debug("🔍 [IMPORT] Recherche par ExternalId: {}", externalId);
            Optional<Product> existingByExternalId = productRepository.findByExternalId(externalId);
            if (existingByExternalId.isPresent()) {
                // Mise à jour du produit existant
                log.info("🔄 [IMPORT] Produit existant trouvé par ExternalId: {}", externalId);
                Product existingProduct = existingByExternalId.get();

                existingProduct.setName(request.getName());
                existingProduct.setShortDescription(request.getShortDescription());
                existingProduct.setDescription(request.getDescription());
                existingProduct.setVolumeWeight(request.getVolumeWeight()); // Volume/Poids
                existingProduct.setPrice(request.getPrice());
                existingProduct.setStock(request.getStock());
                existingProduct.setActive(true); // Réactiver si importé

                // NE PAS régénérer le slug - garder l'ancien pour éviter les liens cassés
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    existingProduct.setMainImage(imageUrl);
                }

                // Synchro catégorie
                Category category = resolveCategory(request);
                if (!existingProduct.getCategory().getId().equals(category.getId())) {
                    existingProduct.setCategory(category);
                }

                Product saved = productRepository.save(existingProduct);
                log.info("✅ [IMPORT] Produit mis à jour - ID: {}, Nom: {}, Active: {}", saved.getId(), saved.getName(), saved.isActive());
                ProductResponse response = mapToResponse(saved);
                response.setCreated(false);
                return response;
            } else {
                log.debug("🔍 [IMPORT] Aucun produit trouvé avec ExternalId: {}", externalId);
            }
        }

        // PRIORITÉ 2: Recherche par Slug (pour compatibilité avec anciens produits sans externalId)
        log.debug("🔍 [IMPORT] Recherche par Slug: {}", request.getSlug());
        return productRepository.findBySlug(request.getSlug())
                .map(existingProduct -> {
                    // Mise à jour + assignation de l'externalId si manquant
                    if (externalId != null && !externalId.trim().isEmpty()) {
                        existingProduct.setExternalId(externalId);
                    }

                    existingProduct.setName(request.getName());
                    existingProduct.setShortDescription(request.getShortDescription());
                    existingProduct.setDescription(request.getDescription());
                    existingProduct.setPrice(request.getPrice());
                    existingProduct.setStock(request.getStock());
                    existingProduct.setActive(true);

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        existingProduct.setMainImage(imageUrl);
                    }

                    Category category = resolveCategory(request);
                    if (!existingProduct.getCategory().getId().equals(category.getId())) {
                        existingProduct.setCategory(category);
                    }

                    Product saved = productRepository.save(existingProduct);
                    log.info("✅ [IMPORT] Produit mis à jour via Slug - ID: {}, Nom: {}, Active: {}", saved.getId(), saved.getName(), saved.isActive());
                    ProductResponse response = mapToResponse(saved);
                    response.setCreated(false);
                    return response;
                })
                .orElseGet(() -> {
                    // NOUVEAU produit - création avec externalId
                    log.info("➕ [IMPORT] Aucun produit existant, création d'un nouveau produit");
                    return createProductWithExternalId(request, imageUrl, externalId);
                });
    }

    /**
     * Crée un nouveau produit avec externalId (helper pour l'import).
     */
    public ProductResponse createProductWithExternalId(com.sucrestore.api.dto.ProductRequest request, String imageUrl, String externalId) {
        log.info("➕ [IMPORT] Création nouveau produit - Nom: {}, ExternalID: {}, Active: {}", request.getName(), externalId, request.isActive());

        Product product = new Product();
        product.setExternalId(externalId);
        product.setName(request.getName());
        product.setSlug(request.getSlug());
        product.setShortDescription(request.getShortDescription());
        product.setDescription(request.getDescription());
        product.setVolumeWeight(request.getVolumeWeight()); // Volume/Poids
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setActive(request.isActive()); // Utilise la valeur de la requête au lieu de forcer true
        product.setMainImage(imageUrl);

        // Catégorie
        Category category = resolveCategory(request);
        log.debug("🏷️ [IMPORT] Catégorie résolue: {} (ID: {})", category.getName(), category.getId());
        product.setCategory(category);

        Product saved = productRepository.save(product);
        log.info("✅ [IMPORT] Nouveau produit CRÉÉ - ID: {}, Nom: {}, Active: {}, Stock: {}",
            saved.getId(), saved.getName(), saved.isActive(), saved.getStock());
        ProductResponse response = mapToResponse(saved);
        response.setCreated(true);
        return response;
    }

    /**
     * Traite un fichier CSV uploadé pour importer des produits.
     */
    public ImportSummary processCsvImport(MultipartFile file) {
        ImportSummary summary = new ImportSummary();

        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build()) { // Ignore la ligne d'en-tête

            List<String[]> rows = csvReader.readAll();
            int rowNum = 2; // Commence à 2 (1 = en-tête)

            for (String[] row : rows) {
                summary.incrementTotal();
                try {
                    // Format attendu: 0:Nom, 1:Catégorie, 2:Prix, 3:ImageURL, 4:Description, 5:Stock, 6:ExternalId(Optionnel)
                    if (row.length < 5) {
                        summary.addError(rowNum, "Colonnes manquantes (Minimum requis: Nom, Catégorie, Prix, Image, Description)");
                        rowNum++;
                        continue;
                    }

                    String name = row[0].trim();
                    String categoryName = row[1].trim();
                    String priceStr = row[2].trim();
                    String imageUrl = row[3].trim();
                    String description = row[4].trim();
                    String stockStr = row.length > 5 ? row[5].trim() : "0";
                    String externalId = row.length > 6 ? row[6].trim() : null;

                    if (name.isEmpty() || categoryName.isEmpty() || priceStr.isEmpty()) {
                        summary.addError(rowNum, "Champs obligatoires manquants (Nom, Catégorie, Prix)");
                        rowNum++;
                        continue;
                    }

                    BigDecimal price;
                    try {
                        price = new BigDecimal(priceStr.replace(",", "."));
                    } catch (NumberFormatException e) {
                        summary.addError(rowNum, "Format de prix invalide: " + priceStr);
                        rowNum++;
                        continue;
                    }

                    int stock = 0;
                    try {
                        if (!stockStr.isEmpty()) {
                            stock = (int) Double.parseDouble(stockStr); // Gère les ".0" potentiels
                        }
                    } catch (NumberFormatException e) {
                        // Stock par défaut à 0 si erreur
                    }

                    // Générer un slug basique si aucun externalId n'est fourni
                    String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-");
                    if (externalId != null && !externalId.isEmpty()) {
                        slug = externalId + "-" + slug;
                    }
                    if (slug.length() > 250) {
                        slug = slug.substring(0, 250);
                    }

                    ProductRequest request = new ProductRequest();
                    request.setName(name);
                    request.setSlug(slug);
                    request.setCategoryId(null); // On utilise categoryName
                    request.setCategoryName(categoryName);
                    request.setPrice(price);
                    request.setStock(stock);
                    request.setDescription(description);
                    request.setShortDescription(description.length() > 100 ? description.substring(0, 100) : description);
                    // imageUrl est passé séparément à importProduct()

                    importProduct(request, imageUrl, externalId);
                    summary.incrementSuccess();

                } catch (Exception e) {
                    summary.addError(rowNum, "Erreur inattendue: " + e.getMessage());
                }
                rowNum++;
            }

        } catch (Exception e) {
            log.error("Erreur lors de la lecture du CSV", e);
            throw new RuntimeException("Erreur de parsing CSV: " + e.getMessage());
        }

        return summary;
    }
}
