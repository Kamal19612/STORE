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
}
