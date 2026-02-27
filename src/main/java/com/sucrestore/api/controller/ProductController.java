package com.sucrestore.api.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sucrestore.api.dto.ProductResponse;
import com.sucrestore.api.entity.Category;
import com.sucrestore.api.service.CategoryService;
import com.sucrestore.api.service.ProductService;

/**
 * Contrôleur REST public pour le catalogue (Produits et Catégories). Accessible
 * sans authentification (configuré dans SecurityConfig).
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    /**
     * GET /api/categories : Liste toutes les catégories actives.
     */
    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllActiveCategories());
    }

    /**
     * GET /api/products : Liste paginée des produits. Filtres optionnels :
     * ?category=1 ou ?search=gadget Pagination par défaut : 10 items par page.
     */
    @GetMapping("/products")
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50, sort = "id", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        if (category != null) {
            return ResponseEntity.ok(productService.getProductsByCategory(category, pageable));
        }

        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(productService.searchProducts(search, pageable));
        }

        return ResponseEntity.ok(productService.getAllActiveProducts(pageable));
    }

    /**
     * GET /api/products/{slug} : Détail d'un produit.
     */
    @GetMapping("/products/{slug}")
    public ResponseEntity<ProductResponse> getProductDetail(@PathVariable String slug) {
        return ResponseEntity.ok(productService.getProductBySlug(slug));
    }
}
