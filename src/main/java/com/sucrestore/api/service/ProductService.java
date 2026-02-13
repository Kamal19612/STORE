package com.sucrestore.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sucrestore.api.dto.ProductResponse;
import com.sucrestore.api.entity.Category;
import com.sucrestore.api.entity.Product;
import com.sucrestore.api.repository.ProductRepository;

/**
 * Service gérant la logique métier pour les produits.
 */
@Service
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
                .price(product.getPrice())
                .oldPrice(product.getOldPrice())
                .mainImage(product.getMainImage())
                .categoryName(product.getCategory().getName())
                .categorySlug(product.getCategory().getSlug())
                .categoryId(product.getCategory().getId()) // Mappage ID Catégorie
                .stock(product.getStock()) // Mappage Stock réel
                .available(product.getStock() > 0)
                .build();
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
     * Importe un produit (Création ou Mise à jour basée sur le Slug). Méthode
     * transactionnelle isolée pour permettre le traitement par lots "Best
     * Effort".
     */
    @Transactional
    public ProductResponse importProduct(com.sucrestore.api.dto.ProductRequest request, String imageUrl) {
        // Validation basique
        if (request.getSlug() == null || request.getSlug().isEmpty()) {
            throw new RuntimeException("Le slug est obligatoire pour l'import");
        }

        // Recherche par Slug
        return productRepository.findBySlug(request.getSlug())
                .map(existingProduct -> {
                    // Update
                    existingProduct.setName(request.getName());
                    existingProduct.setShortDescription(request.getShortDescription());
                    existingProduct.setDescription(request.getDescription());
                    existingProduct.setPrice(request.getPrice());
                    // On ne touche pas oldPrice en import auto sauf si spécifié (ici non géré dans CSV simple)

                    // Gestion intelligente du stock: on remplace ou on ajoute ? Ici on remplace pour la synchro.
                    existingProduct.setStock(request.getStock());
                    existingProduct.setActive(true); // Réactiver si importé

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        existingProduct.setMainImage(imageUrl);
                    }

                    // Synchro catégorie
                    Category category = resolveCategory(request);
                    if (!existingProduct.getCategory().getId().equals(category.getId())) {
                        existingProduct.setCategory(category);
                    }

                    return mapToResponse(productRepository.save(existingProduct));
                })
                .orElseGet(() -> {
                    // Create
                    return createProduct(request, imageUrl);
                });
    }

}
