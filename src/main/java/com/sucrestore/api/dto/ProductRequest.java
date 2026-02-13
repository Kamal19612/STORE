package com.sucrestore.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO pour la création ou modification d'un produit (Admin).
 */
@Data
public class ProductRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String name;

    @NotBlank(message = "Le slug est obligatoire")
    private String slug;

    private String shortDescription;
    private String description;

    @NotNull(message = "Le prix est obligatoire")
    @Min(0)
    private BigDecimal price;

    private BigDecimal oldPrice;

    @NotNull(message = "Le stock est obligatoire")
    @Min(0)
    private Integer stock;

    // Pour l'image, on reçoit soit une URL (String), soit un fichier via Multipart dans le Controller
    // Ce champ sert si l'admin fournit une URL directe
    private String imageUrl;

    // Modifié : categoryId n'est plus @NotNull car on peut fournir un Nom
    private Long categoryId;

    // Nouveau champ pour la création dynamique ou recherche par nom
    private String categoryName;

    private boolean active = true;
}
