package com.sucrestore.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sucrestore.api.entity.SliderImage;

/**
 * Repository pour l'accès aux images du Slider (Carrousel).
 */
@Repository
public interface SliderImageRepository extends JpaRepository<SliderImage, Long> {

    /**
     * Trouve toutes les images actives, triées par ordre d'affichage.
     */
    List<SliderImage> findByActiveTrueOrderByDisplayOrderAsc();
}
