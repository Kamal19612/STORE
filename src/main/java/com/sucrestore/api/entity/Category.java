package com.sucrestore.api.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
 * Entité représentant une catégorie de produits (ex: "Gadgets", "Lingerie").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nom de la catégorie (ex: "Bien-être"). Unique.
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * Slug pour l'URL (ex: "bien-etre"). Unique.
     */
    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    /**
     * Description optionnelle pour le SEO ou l'affichage
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * URL de l'image d'illustration
     */
    private String imageUrl;

    /**
     * Catégorie visible ou non sur le site
     */
    @Builder.Default
    private boolean active = true;

    /* Relations (Optionnel selon besoin)
    @OneToMany(mappedBy = "category")
    private List<Product> products;
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
