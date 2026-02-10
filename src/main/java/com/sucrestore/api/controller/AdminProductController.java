package com.sucrestore.api.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sucrestore.api.dto.ProductRequest;
import com.sucrestore.api.dto.ProductResponse;
import com.sucrestore.api.service.FileStorageService;
import com.sucrestore.api.service.ProductService;

/**
 * Contrôleur REST pour l'administration des produits. Supporte l'ajout avec
 * image (Multipart) ou URL.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
public class AdminProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private FileStorageService fileStorageService;

    // Mapper Jackson pour convertir le String JSON en Objet Java manuellement
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * GET /api/admin/products : Liste tous les produits (paginé).
     */
    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<ProductResponse>> getAllProducts(
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    /**
     * GET /api/admin/products/{id} : Détail d'un produit par ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> createProduct(
            @RequestPart("product") String productJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException {

        // 1. Convertir le JSON en DTO
        ProductRequest request = objectMapper.readValue(productJson, ProductRequest.class);

        // 2. Traiter l'image (Upload ou URL)
        String imageUrl = request.getImageUrl();
        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = fileStorageService.storeFile(imageFile);

            // Construire l'URL d'accès public à l'image stockée
            imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(fileName)
                    .toUriString();
        }

        // 3. Appeler le service pour créer le produit
        return ResponseEntity.ok(productService.createProduct(request, imageUrl));
    }

    /**
     * PUT /api/admin/products/{id} : Modifier un produit.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestPart("product") String productJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException {

        ProductRequest request = objectMapper.readValue(productJson, ProductRequest.class);

        String imageUrl = request.getImageUrl();
        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = fileStorageService.storeFile(imageFile);
            imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(fileName)
                    .toUriString();
        }

        return ResponseEntity.ok(productService.updateProduct(id, request, imageUrl));
    }

    /**
     * DELETE /api/admin/products/{id} : Supprimer (Archiver) un produit.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok().build();
    }
}
