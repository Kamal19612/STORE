package com.sucrestore.api.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

/**
 * DTO représentant un produit renvoyé par l'API (pour le catalogue).
 */
@Data
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String slug;
    private String shortDescription;
    private String description;
    private String volumeWeight; // Volume/Poids (ex: "50ml", "100g")
    private BigDecimal price;
    private BigDecimal oldPrice;
    private String mainImage;
    private String categoryName;
    private String categorySlug;
    private Long categoryId; // Ajouté pour l'édition Admin
    private Integer stock; // Ajouté pour l'édition Admin
    private String externalId; // ID externe de Google Sheets
    private boolean available; // basé sur stock > 0
}
