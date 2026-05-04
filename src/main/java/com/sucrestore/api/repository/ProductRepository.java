package com.sucrestore.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sucrestore.api.entity.Product;

/**
 * Repository pour l'accès aux données des Produits.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Trouve un produit via son slug (URL friendly name).
     */
    Optional<Product> findBySlug(String slug);

    /**
     * Trouve un produit via son ID externe (provenant de Google Sheets).
     */
    Optional<Product> findByExternalId(String externalId);

    /**
     * Trouve tous les produits actifs, avec pagination.
     */
    Page<Product> findByActiveTrue(Pageable pageable);

    /**
     * Trouve tous les produits actifs (sans pagination) - pour synchronisation.
     */
    java.util.List<Product> findByActiveTrue();

    java.util.List<Product> findByActiveTrueAndStoreId(Long storeId);

    /**
     * Trouve les produits d'une catégorie spécifique.
     */
    Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

    /**
     * Recherche de produits par nom (contient, insensible à la casse).
     */
    Page<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);

    Page<Product> findByStoreId(Long storeId, Pageable pageable);

    Page<Product> findByStoreIdAndCategoryId(Long storeId, Long categoryId, Pageable pageable);

    Page<Product> findByStoreIdAndNameContainingIgnoreCase(Long storeId, String name, Pageable pageable);

    // Multi-store scoped
    Optional<Product> findBySlugAndStoreId(String slug, Long storeId);
    Optional<Product> findByExternalIdAndStoreId(String externalId, Long storeId);
    Page<Product> findByActiveTrueAndStoreId(Long storeId, Pageable pageable);
    Page<Product> findByCategoryIdAndActiveTrueAndStoreId(Long categoryId, Long storeId, Pageable pageable);
    Page<Product> findByNameContainingIgnoreCaseAndActiveTrueAndStoreId(String name, Long storeId, Pageable pageable);

    /**
     * Top N produits les plus commandés (par quantité totale dans les commandes).
     * Exclut les produits sans image.
     */
    @Query("SELECT oi.product FROM OrderItem oi " +
           "WHERE oi.product.active = true AND oi.product.mainImage IS NOT NULL AND oi.product.mainImage != '' " +
           "GROUP BY oi.product " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<Product> findTopOrderedProducts(Pageable pageable);

    @Query("SELECT oi.product FROM OrderItem oi " +
           "WHERE oi.product.active = true AND oi.product.mainImage IS NOT NULL AND oi.product.mainImage != '' " +
           "AND oi.product.store.id = :storeId " +
           "GROUP BY oi.product " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<Product> findTopOrderedProductsByStore(@org.springframework.data.repository.query.Param("storeId") Long storeId, Pageable pageable);

    /**
     * Trouve les N produits actifs avec images (pour fallback du carousel).
     */
    Page<Product> findByActiveTrueAndMainImageIsNotNullOrderByIdDesc(Pageable pageable);

    Page<Product> findByActiveTrueAndMainImageIsNotNullAndStoreIdOrderByIdDesc(Long storeId, Pageable pageable);

    /**
     * Supprime tous les order_items référençant des produits (avant suppression totale du catalogue).
     */
    @Modifying
    @Query(value = "DELETE FROM order_items", nativeQuery = true)
    void deleteAllOrderItemsReferences();
}
