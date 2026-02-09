package com.sucrestore.api.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * Trouve tous les produits actifs, avec pagination.
     */
    Page<Product> findByActiveTrue(Pageable pageable);

    /**
     * Trouve les produits d'une catégorie spécifique.
     */
    Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

    /**
     * Recherche de produits par nom (contient, insensible à la casse).
     */
    Page<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);
}
