package com.sucrestore.api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import com.sucrestore.api.entity.Slider;
import com.sucrestore.api.repository.SliderRepository;

@Slf4j
@Service
@Transactional
public class SliderService {

    @Autowired
    private SliderRepository sliderRepository;

    @Autowired
    private FileStorageService fileStorageService;

    // Récupérer tous les sliders (pour Admin)
    public List<Slider> getAllSliders() {
        System.out.println("!!! DEBUG: SliderService.getAllSliders (ORDER DESC)");
        return sliderRepository.findAll(Sort.by(Direction.DESC, "displayOrder"));
    }

    // Récupérer les sliders actifs (pour Public)
    public List<Slider> getActiveSliders() {
        System.out.println("!!! DEBUG: SliderService.getActiveSliders (ORDER DESC)");
        return sliderRepository.findAllByActiveTrueOrderByDisplayOrderDesc();
    }

    // Créer un slider (fichier ou URL optionnels)
    public Slider createSlider(String title, String description, MultipartFile imageFile, String imageUrl, Integer order, Boolean active) {
        String finalImageUrl = imageUrl;
        
        System.out.println("!!! DEBUG: SliderService.createSlider called");
        System.out.println("!!! imageFile status: " + (imageFile == null ? "NULL" : (imageFile.isEmpty() ? "EMPTY" : "PRESENT")));

        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = fileStorageService.storeFile(imageFile);
            finalImageUrl = "/uploads/" + fileName;
        }

        if (finalImageUrl == null || finalImageUrl.trim().isEmpty()) {
            String debugInfo = String.format("(imageFile is %s, imageUrl is '%s')", 
                (imageFile == null ? "null" : (imageFile.isEmpty() ? "empty" : "present")), 
                (imageUrl == null ? "null" : imageUrl));
            throw new RuntimeException("Une image est obligatoire (Fichier ou URL). " + debugInfo);
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
        if (!sliderRepository.existsById(id)) {
            throw new RuntimeException("Le slider avec l'ID " + id + " n'existe pas.");
        }
        sliderRepository.deleteById(id);
    }

    // Toggle Active
    public Slider toggleActive(Long id) {
        Slider slider = sliderRepository.findById(id).orElseThrow(() -> new RuntimeException("Slider introuvable"));
        slider.setActive(!slider.isActive());
        return sliderRepository.save(slider);
    }
}
