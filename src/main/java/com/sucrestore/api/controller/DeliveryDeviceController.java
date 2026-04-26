package com.sucrestore.api.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sucrestore.api.entity.DeliveryDeviceToken;
import com.sucrestore.api.entity.User;
import com.sucrestore.api.repository.DeliveryDeviceTokenRepository;
import com.sucrestore.api.repository.UserRepository;

@RestController
@RequestMapping("/api/delivery/devices")
@PreAuthorize("hasRole('DELIVERY_AGENT') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
public class DeliveryDeviceController {

    private final DeliveryDeviceTokenRepository tokenRepo;
    private final UserRepository userRepository;

    public DeliveryDeviceController(DeliveryDeviceTokenRepository tokenRepo, UserRepository userRepository) {
        this.tokenRepo = tokenRepo;
        this.userRepository = userRepository;
    }

    /**
     * Enregistre/actualise le token FCM du téléphone du livreur.
     * Body: { "token": "...", "platform": "android|ios" }
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body, Authentication auth) {
        String token = body.get("token");
        String platform = body.getOrDefault("platform", "android");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "token requis"));
        }

        String username = auth.getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

        DeliveryDeviceToken entity = tokenRepo.findByFcmToken(token).orElseGet(() -> DeliveryDeviceToken.builder()
            .fcmToken(token)
            .build());

        entity.setUser(user);
        entity.setPlatform(platform);
        entity.setActive(true);
        entity.setLastSeenAt(LocalDateTime.now());
        tokenRepo.save(entity);

        return ResponseEntity.ok(Map.of("ok", true));
    }

    /**
     * Désactive le token courant (utile si logout / changement téléphone).
     * Body: { "token": "..." }
     */
    @DeleteMapping("/unregister")
    public ResponseEntity<Map<String, Object>> unregister(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "token requis"));
        }
        tokenRepo.findByFcmToken(token).ifPresent(e -> {
            e.setActive(false);
            e.setLastSeenAt(LocalDateTime.now());
            tokenRepo.save(e);
        });
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication auth) {
        return ResponseEntity.ok(Map.of(
            "username", auth.getName(),
            "devices", tokenRepo.findByUserUsernameAndIsActiveTrue(auth.getName()).size()
        ));
    }
}

