package com.sucrestore.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sucrestore.api.dto.ProductResponse;
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
                .mainImage(imageUrl) // URL ou chemin fichier
                .category(categoryRepository.findById(request.getCategoryId())
                        .orElseThrow(() -> new RuntimeException("Catégorie introuvable")))
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

        if (!product.getCategory().getId().equals(request.getCategoryId())) {
            product.setCategory(categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Catégorie introuvable")));
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
}
