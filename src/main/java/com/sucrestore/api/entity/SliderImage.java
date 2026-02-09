package com.sucrestore.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entité représentant une image du carrousel (Slider) sur la page d'accueil.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "slider_images")
public class SliderImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Titre optionnel pour l'accessibilité ou l'affichage
     */
    private String title;

    /**
     * URL de l'image (stockée localement ou sur un cloud)
     */
    @Column(nullable = false)
    private String imageUrl;

    /**
     * Ordre d'affichage dans le carrousel
     */
    private Integer displayOrder;

    /**
     * Image visible ou non
     */
    @Builder.Default
    private boolean active = true;
}
