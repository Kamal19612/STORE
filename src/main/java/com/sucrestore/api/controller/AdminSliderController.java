package com.sucrestore.api.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sucrestore.api.entity.SliderImage;
import com.sucrestore.api.service.FileStorageService;
import com.sucrestore.api.service.SliderService;

/**
 * Contrôleur REST pour l'administration du Slider.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/slider")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
public class AdminSliderController {

    @Autowired
    private SliderService sliderService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * GET /api/admin/slider : Liste des images.
     */
    @GetMapping
    public ResponseEntity<List<SliderImage>> getAllSliderImages() {
        return ResponseEntity.ok(sliderService.getAllSliderImages());
    }

    /**
     * POST /api/admin/slider : Ajouter une image.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SliderImage> addSliderImage(
            @RequestPart(value = "slider", required = false) String sliderJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException {

        SliderImage sliderImage = new SliderImage();
        if (sliderJson != null) {
            sliderImage = objectMapper.readValue(sliderJson, SliderImage.class);
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = fileStorageService.storeFile(imageFile);
            String imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(fileName)
                    .toUriString();
            sliderImage.setImageUrl(imageUrl);
        }

        if (sliderImage.getDisplayOrder() == null) {
            sliderImage.setDisplayOrder(100);
        }

        return ResponseEntity.ok(sliderService.addSliderImage(sliderImage));
    }

    /**
     * PUT /api/admin/slider/{id} : Modifier une image (Ordre, Active,
     * Titre...).
     */
    @org.springframework.web.bind.annotation.PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SliderImage> updateSliderImage(
            @PathVariable Long id,
            @RequestPart(value = "slider") String sliderJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException {

        SliderImage existingImage = sliderService.getSliderImageById(id); // Need to add this method in Service or use Repo directly if Service missing
        SliderImage payload = objectMapper.readValue(sliderJson, SliderImage.class);

        existingImage.setTitle(payload.getTitle());
        existingImage.setActive(payload.isActive());
        existingImage.setDisplayOrder(payload.getDisplayOrder());

        // Update URL only if provided in payload (text) or file
        if (payload.getImageUrl() != null && !payload.getImageUrl().isEmpty()) {
            existingImage.setImageUrl(payload.getImageUrl());
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = fileStorageService.storeFile(imageFile);
            String imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(fileName)
                    .toUriString();
            existingImage.setImageUrl(imageUrl);
        }

        return ResponseEntity.ok(sliderService.addSliderImage(existingImage));
    }

    /**
     * DELETE /api/admin/slider/{id} : Supprimer une image.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSliderImage(@PathVariable Long id) {
        sliderService.deleteSliderImage(id);
        return ResponseEntity.ok().build();
    }
}
