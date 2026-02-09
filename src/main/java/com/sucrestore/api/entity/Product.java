package com.sucrestore.api.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entité représentant un produit vendu sur la boutique.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nom du produit
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Slug unique pour l'URL (ex: "mon-super-produit")
     */
    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    /**
     * Description courte pour les listes
     */
    @Column(length = 500)
    private String shortDescription;

    /**
     * Description détaillée (HTML possible)
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Prix de vente unitaire
     */
    @Column(nullable = false)
    private BigDecimal price;

    /**
     * Prix barré (optionnel, pour les promotions)
     */
    private BigDecimal oldPrice;

    /**
     * Quantité en stock
     */
    @Column(nullable = false)
    private Integer stock;

    /**
     * URL de l'image principale
     */
    private String mainImage;

    /**
     * Produit actif (visible) ou archivé
     */
    @Builder.Default
    private boolean active = true;

    /**
     * Catégorie parente
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
