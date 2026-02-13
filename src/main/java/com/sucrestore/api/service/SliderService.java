package com.sucrestore.api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.sucrestore.api.entity.Slider;
import com.sucrestore.api.repository.SliderRepository;

@Service
public class SliderService {

    @Autowired
    private SliderRepository sliderRepository;

    @Autowired
    private FileStorageService fileStorageService;

    // Récupérer tous les sliders (pour Admin)
    public List<Slider> getAllSliders() {
        return sliderRepository.findAllByOrderByDisplayOrderAsc();
    }

    // Récupérer les sliders actifs (pour Public)
    public List<Slider> getActiveSliders() {
        return sliderRepository.findAllByActiveTrueOrderByDisplayOrderAsc();
    }

    // Créer un slider (fichier ou URL optionnels)
    public Slider createSlider(String title, String description, MultipartFile imageFile, String imageUrl, Integer order, Boolean active) {
        String finalImageUrl = imageUrl;

        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = fileStorageService.storeFile(imageFile);
            finalImageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(fileName)
                    .toUriString();
        }

        if (finalImageUrl == null || finalImageUrl.trim().isEmpty()) {
            throw new RuntimeException("Une image est obligatoire (Fichier ou URL).");
        }

        Slider slider = Slider.builder()
                .title(title)
                .description(description)
                .imageUrl(finalImageUrl)
                .displayOrder(order != null ? order : 10)
                .active(active != null ? active : true)
                .build();

        return sliderRepository.save(slider);
    }

    // Supprimer un slider
    public void deleteSlider(Long id) {
        sliderRepository.deleteById(id);
    }

    // Toggle Active
    public Slider toggleActive(Long id) {
        Slider slider = sliderRepository.findById(id).orElseThrow(() -> new RuntimeException("Slider introuvable"));
        slider.setActive(!slider.isActive());
        return sliderRepository.save(slider);
    }
}
