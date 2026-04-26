package com.sucrestore.api.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;

/**
 * Service gérant les connexions SSE (Server-Sent Events) pour les
 * notifications temps réel vers l'admin et les livreurs.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    // username -> SseEmitter (une connexion active par utilisateur)
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // username -> set de rôles Spring Security (ex: "ROLE_ADMIN")
    private final Map<String, Set<String>> userRoles = new ConcurrentHashMap<>();

    /**
     * Abonne un utilisateur authentifié au flux SSE.
     */
    public SseEmitter subscribe(String username, Collection<String> roles) {
        // Fermer l'ancienne connexion si l'utilisateur se reconnecte
        SseEmitter existing = emitters.get(username);
        if (existing != null) {
            existing.complete();
        }

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(username, emitter);
        userRoles.put(username, new HashSet<>(roles));

        Runnable cleanup = () -> {
            emitters.remove(username, emitter);
            userRoles.remove(username);
            log.debug("SSE déconnecté : {}", username);
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> {
            log.debug("SSE erreur pour {} : {}", username, e.getMessage());
            cleanup.run();
        });

        // Envoyer un événement de bienvenue pour confirmer la connexion
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("message", "Flux de notifications actif"), MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            log.warn("Impossible d'envoyer l'événement de connexion à {} : {}", username, e.getMessage());
        }

        log.info("SSE connecté : {} (rôles: {})", username, roles);
        return emitter;
    }

    /**
     * Envoie une notification à tous les utilisateurs ayant le rôle donné.
     */
    public void notifyByRole(String role, String eventType, Object data) {
        userRoles.forEach((username, roles) -> {
            if (roles.contains(role)) {
                SseEmitter emitter = emitters.get(username);
                if (emitter != null) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name(eventType)
                                .data(data, MediaType.APPLICATION_JSON));
                    } catch (Exception e) {
                        log.debug("Impossible d'envoyer la notification à {} : {}", username, e.getMessage());
                        emitters.remove(username);
                        userRoles.remove(username);
                    }
                }
            }
        });
    }

    /**
     * Notifie tous les admins (SUPER_ADMIN + ADMIN) d'un événement.
     */
    public void notifyAdmins(String eventType, Object data) {
        notifyByRole("ROLE_SUPER_ADMIN", eventType, data);
        notifyByRole("ROLE_ADMIN", eventType, data);
        notifyByRole("ROLE_MANAGER", eventType, data);
    }

    /**
     * Notifie tous les livreurs connectés d'un événement.
     */
    public void notifyDeliveryAgents(String eventType, Object data) {
        notifyByRole("ROLE_DELIVERY_AGENT", eventType, data);
    }

    /**
     * Heartbeat toutes les 20 secondes pour garder les connexions SSE actives
     * (prévient la coupure par les proxies/load balancers).
     */
    @Scheduled(fixedRate = 20000)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) return;
        emitters.forEach((username, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data(Map.of("t", System.currentTimeMillis()), MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                emitters.remove(username);
                userRoles.remove(username);
            }
        });
    }

    /**
     * Retourne le nombre de connexions SSE actives (pour le monitoring).
     */
    public int getActiveConnections() {
        return emitters.size();
    }

    /**
     * Ferme toutes les connexions SSE à l'arrêt du serveur.
     * Évite le blocage de l'arrêt gracieux par des emitters longue durée.
     */
    @PreDestroy
    public void closeAll() {
        log.info("Fermeture de {} connexions SSE...", emitters.size());
        emitters.values().forEach(emitter -> {
            try { emitter.complete(); } catch (Exception ignored) {}
        });
        emitters.clear();
        userRoles.clear();
    }
}