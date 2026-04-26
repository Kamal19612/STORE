package com.sucrestore.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sucrestore.api.dto.auth.JwtResponse;
import com.sucrestore.api.dto.auth.LoginRequest;
import com.sucrestore.api.security.JwtUtils;

import java.util.Map;
import java.util.Optional;

import jakarta.validation.Valid;

/**
 * Contrôleur REST gérant l'authentification des utilisateurs. Permet aux
 * utilisateurs de se connecter et d'obtenir un token JWT.
 */
@CrossOrigin(origins = "*", maxAge = 3600) // Autorise les requêtes Cross-Origin (CORS) depuis n'importe quelle source
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    com.sucrestore.api.security.UserDetailsServiceImpl userDetailsService;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    com.sucrestore.api.repository.UserRepository userRepository;

    /**
     * Endpoint de connexion (Login).
     *
     * @param loginRequest DTO contenant le username et le password.
     * @return ResponseEntity contenant le JWT et les infos utilisateur si
     * succès, ou une erreur 401.
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        // 1. Authentifier l'utilisateur via le Manager de Spring Security
        // Cela va appeler UserDetailsServiceImpl.loadUserByUsername en interne
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        // 2. Mettre l'authentification dans le contexte de sécurité (SecurityContext)
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Authentifier l'utilisateur
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 4. Invalider les sessions précédentes (Incrémente la version du token en BDD)
        // Cela garantit qu'un seul token est valide à la fois pour un utilisateur
        // 4. Invalider les sessions précédentes (Incrémente la version du token en BDD)
        Long newTokenVersion = userDetailsService.invalidateUserSession(userDetails.getUsername());

        // 5. Générer le nouveau token avec la version
        String jwt = jwtUtils.generateJwtToken(authentication, newTokenVersion);

        // 5. Retourner la réponse avec le token
        java.util.List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(java.util.stream.Collectors.toList());

        String simpleRole = roles.contains("ROLE_DELIVERY_AGENT") ? "livreur" : "admin";

        Optional<com.sucrestore.api.entity.User> uOpt = userRepository.findByUsername(userDetails.getUsername());
        Long livreurId = null;
        String nom = userDetails.getUsername();
        if (uOpt.isPresent()) {
            var u = uOpt.get();
            // Mobile: livreurId = user.id pour l'identité livreur
            if (roles.contains("ROLE_DELIVERY_AGENT")) {
                livreurId = u.getId();
            }
            String first = u.getFirstName() != null ? u.getFirstName().trim() : "";
            String last = u.getLastName() != null ? u.getLastName().trim() : "";
            String full = (first + " " + last).trim();
            if (!full.isBlank()) nom = full;
        }

        return ResponseEntity.ok(new JwtResponse(
                jwt,
                userDetails.getUsername(),
                roles,
                simpleRole,
                livreurId,
                nom
        ));
    }

    /**
     * Endpoint de déconnexion (Logout). Invalide la session côté serveur en
     * incrémentant la version du token.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            userDetailsService.invalidateUserSession(userDetails.getUsername());
        }
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie !"));
    }
}
