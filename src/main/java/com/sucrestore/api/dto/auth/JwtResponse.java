package com.sucrestore.api.dto.auth;

import lombok.Data;

/**
 * Objet de Transfert de Données (DTO) pour la réponse d'authentification.
 * Contient le token JWT généré et les informations de l'utilisateur connecté.
 */
@Data
public class JwtResponse {

    /**
     * Le token d'accès JWT
     */
    private String token;

    /**
     * Type de token (toujours "Bearer")
     */
    private String type = "Bearer";

    /**
     * Nom d'utilisateur connecté
     */
    private String username;

    /**
     * Rôles de l'utilisateur (liste de chaînes)
     */
    private java.util.List<String> roles;

    public JwtResponse(String accessToken, String username, java.util.List<String> roles) {
        this.token = accessToken;
        this.username = username;
        this.roles = roles;
    }
}
