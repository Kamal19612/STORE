package com.sucrestore.api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sucrestore.api.entity.Category;
import com.sucrestore.api.repository.CategoryRepository;

/**
 * Service gérant la logique métier pour les catégories.
 */
@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Récupère toutes les catégories actives pour le menu de navigation.
     *
     * @return Liste des catégories actives.
     */
    @Transactional(readOnly = true)
    public List<Category> getAllActiveCategories() {
        return categoryRepository.findByActiveTrue();
    }

    // --- Méthodes Admin ---
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional
    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(Long id, Category categoryRequest) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable ID: " + id));
        category.setName(categoryRequest.getName());
        category.setSlug(categoryRequest.getSlug());
        category.setDescription(categoryRequest.getDescription());
        category.setActive(categoryRequest.isActive());
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable ID: " + id));
        category.setActive(false); // Soft delete
        categoryRepository.save(category);
    }
}
