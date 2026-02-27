package com.sucrestore.api.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.sucrestore.api.dto.SliderRequest;

import com.sucrestore.api.entity.Slider;
import com.sucrestore.api.service.SliderService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class SliderController {

    @Autowired
    private SliderService sliderService;

    /**
     * Public : Récupérer les sliders actifs pour la page d'accueil.
     */
    @GetMapping("/sliders")
    public ResponseEntity<List<Slider>> getActiveSliders() {
        return ResponseEntity.ok(sliderService.getActiveSliders());
    }

    /**
     * Admin : Récupérer tous les sliders.
     */
    @GetMapping("/admin/sliders")
    public ResponseEntity<List<Slider>> getAllSliders() {
        return ResponseEntity.ok(sliderService.getAllSliders());
    }

    /**
     * Admin : Créer un nouveau slider (upload image obligatoire).
     */
    @PostMapping("/admin/sliders")
    public ResponseEntity<Slider> createSlider(@ModelAttribute SliderRequest request) {
        
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("!!! DEBUG: SliderController.createSlider called");
        System.out.println("!!! Title: " + request.getTitle());
        System.out.println("!!! Image is null: " + (request.getImage() == null));
        if (request.getImage() != null) {
            System.out.println("!!! Image Original Name: " + request.getImage().getOriginalFilename());
            System.out.println("!!! Image Content Type: " + request.getImage().getContentType());
            System.out.println("!!! Image Size: " + request.getImage().getSize());
        }
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

        return ResponseEntity.ok(sliderService.createSlider(
                request.getTitle(), 
                request.getDescription(), 
                request.getImage(), 
                request.getImageUrl(), 
                request.getDisplayOrder(), 
                request.getActive()));
    }

    /**
     * Admin : Supprimer un slider.
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @DeleteMapping("/admin/sliders/{id}")
    public ResponseEntity<Void> deleteSlider(@PathVariable Long id) {
        sliderService.deleteSlider(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Admin : Activer/Désactiver un slider.
     */
    @PutMapping("/admin/sliders/{id}/toggle")
    public ResponseEntity<Slider> toggleSlider(@PathVariable Long id) {
        return ResponseEntity.ok(sliderService.toggleActive(id));
    }
}
