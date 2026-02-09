package com.sucrestore.api.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sucrestore.api.entity.SliderImage;
import com.sucrestore.api.service.SliderService;

/**
 * Contrôleur REST public pour afficher le Slider.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/slider")
public class PublicSliderController {

    @Autowired
    private SliderService sliderService;

    @GetMapping
    public ResponseEntity<List<SliderImage>> getSliderImages() {
        return ResponseEntity.ok(sliderService.getAllSliderImages());
    }
}
