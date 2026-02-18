package com.sucrestore.api.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entité représentant un utilisateur du système (Administrateur, Manager,
 * Vendeur...). Cette classe est mappée à la table "users" de la base de
 * données.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nom d'utilisateur unique pour la connexion
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * Adresse email unique
     */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * Mot de passe crypté (BCrypt)
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(nullable = false)
    private String password;

    /**
     * Numéro de téléphone (Optionnel, utile pour les livreurs)
     */
    @Column(length = 20)
    private String phone;

    /**
     * Rôle de l'utilisateur (définit les permissions)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * Compte actif ou désactivé
     */
    @Builder.Default
    private boolean active = true;

    /**
     * Version du token pour gérer l'invalidation des sessions. Incrémenté à
     * chaque nouvelle connexion.
     */
    @Builder.Default
    @Column(nullable = true)
    private Long tokenVersion = 0L;

    /**
     * Date de la dernière connexion réussie
     */
    private LocalDateTime lastLogin;

    /**
     * Date de création de l'enregistrement (géré automatiquement)
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * Date de la dernière modification (géré automatiquement)
     */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Enumération des rôles disponibles dans l'application.
     */
    public enum Role {
        SUPER_ADMIN, // Accès total
        ADMIN, // Accès à tout sauf suppression des autres admins
        MANAGER, // Gestion des commandes et produits uniquement
        DELIVERY_AGENT, // Livreur : accès aux commandes à livrer
        CUSTOMER      // Client : pour futur compte utilisateur
    }
}
