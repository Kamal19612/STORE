package com.sucrestore.api.controller;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.sucrestore.api.entity.PushSubscription;
import com.sucrestore.api.entity.User;
import com.sucrestore.api.repository.PushSubscriptionRepository;
import com.sucrestore.api.repository.UserRepository;
import com.sucrestore.api.service.NotificationService;

/**
 * Endpoint SSE (Server-Sent Events) pour les notifications temps réel,
 * et endpoint Web Push pour l'enregistrement des souscriptions VAPID.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PushSubscriptionRepository pushRepo;

    @Autowired
    private UserRepository userRepository;

    // ─── SSE ─────────────────────────────────────────────────────────────────

    @GetMapping(value = "/stream/admin", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public SseEmitter streamAdmin(Authentication authentication) {
        return subscribe(authentication);
    }

    @GetMapping(value = "/stream/delivery", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public SseEmitter streamDelivery(Authentication authentication) {
        return subscribe(authentication);
    }

    private SseEmitter subscribe(Authentication authentication) {
        String username = authentication.getName();
        Collection<String> roles = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());
        return notificationService.subscribe(username, roles);
    }

    // ─── Web Push ─────────────────────────────────────────────────────────────

    /**
     * Enregistre ou met à jour la souscription Web Push d'un appareil.
     * Body attendu : { endpoint, keys: { p256dh, auth } }
     */
    @PostMapping("/push/subscribe")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> subscribe(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        String endpoint = (String) body.get("endpoint");

        @SuppressWarnings("unchecked")
        Map<String, String> keys = (Map<String, String>) body.get("keys");

        if (endpoint == null || keys == null) {
            return ResponseEntity.badRequest().build();
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        pushRepo.findByEndpoint(endpoint).ifPresentOrElse(
            existing -> {
                existing.setP256dh(keys.get("p256dh"));
                existing.setAuth(keys.get("auth"));
                pushRepo.save(existing);
            },
            () -> pushRepo.save(PushSubscription.builder()
                    .user(user)
                    .endpoint(endpoint)
                    .p256dh(keys.get("p256dh"))
                    .auth(keys.get("auth"))
                    .build())
        );

        return ResponseEntity.ok().build();
    }
}