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
    JwtUtils jwtUtils;

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

        // 3. Générer le token JWT
        String jwt = jwtUtils.generateJwtToken(authentication);

        // 4. Récupérer les détails de l'utilisateur connecté
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 5. Retourner la réponse avec le token
        java.util.List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getUsername(),
                roles));
    }
}
