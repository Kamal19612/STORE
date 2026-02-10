package com.sucrestore.api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sucrestore.api.entity.SliderImage;
import com.sucrestore.api.repository.SliderImageRepository;

/**
 * Service gérant les images du slider (carrousel).
 */
@Service
public class SliderService {

    @Autowired
    private SliderImageRepository sliderImageRepository;

    /**
     * Récupère toutes les images du slider triées par ordre d'affichage.
     */
    @Transactional(readOnly = true)
    public List<SliderImage> getAllSliderImages() {
        return sliderImageRepository.findAll(Sort.by(Sort.Direction.ASC, "displayOrder"));
    }

    @Transactional(readOnly = true)
    public SliderImage getSliderImageById(Long id) {
        return sliderImageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image Slider introuvable ID: " + id));
    }

    /**
     * Ajoute une nouvelle image au slider.
     */
    @Transactional
    public SliderImage addSliderImage(SliderImage sliderImage) {
        return sliderImageRepository.save(sliderImage);
    }

    /**
     * Supprime une image du slider.
     */
    @Transactional
    public void deleteSliderImage(Long id) {
        sliderImageRepository.deleteById(id);
    }
}
