package com.sucrestore.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sucrestore.api.entity.Category;

/**
 * Repository pour l'accès aux données des Catégories.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Trouve une catégorie par son slug.
     */
    Optional<Category> findBySlug(String slug);

    /**
     * Trouve une catégorie par son nom (ignore casse).
     */
    Optional<Category> findByNameIgnoreCase(String name);

    /**
     * Liste toutes les catégories actives.
     */
    List<Category> findByActiveTrue();
}
