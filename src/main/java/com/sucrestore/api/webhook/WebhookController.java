package com.sucrestore.api.webhook;

import com.sucrestore.api.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Endpoints admin pour la configuration et le test du webhook.
 *
 * Accès restreint : ADMIN et SUPER_ADMIN uniquement.
 *
 * GET  /api/admin/webhook/config          → lire la configuration actuelle
 * PUT  /api/admin/webhook/config          → mettre à jour URL / secret / activer
 * POST /api/admin/webhook/test            → envoyer un payload de test
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/webhook")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class WebhookController {

    private final AppSettingService       appSettingService;
    private final WebhookService          webhookService;
    private final WebhookSettingsResolver webhookSettingsResolver;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/admin/webhook/config
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retourne la configuration webhook actuelle.
     * Le secret est masqué (remplacé par "••••") pour ne pas l'exposer en clair.
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getConfig() {
        // Valeurs effectives (base + repli application.yml / env)
        String url     = webhookSettingsResolver.url();
        String secret  = webhookSettingsResolver.secret();
        String enabled = webhookSettingsResolver.enabled() ? "true" : "false";

        Map<String, String> config = new HashMap<>();
        config.put("webhook_url",     url);
        config.put("webhook_secret",  secret.isBlank() ? "" : "••••••••");
        config.put("webhook_enabled", enabled);

        return ResponseEntity.ok(config);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/admin/webhook/config
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Met à jour la configuration webhook.
     *
     * Corps attendu :
     * {
     *   "webhook_url"     : "https://mon-serveur.com/webhook",
     *   "webhook_secret"  : "mon_secret_ultra_fort",
     *   "webhook_enabled" : "true"
     * }
     *
     * Si webhook_secret est omis ou vide, l'ancien secret est conservé.
     */
    @PutMapping("/config")
    public ResponseEntity<Map<String, String>> updateConfig(@RequestBody Map<String, String> body) {
        Map<String, String> toSave = new HashMap<>();

        if (body.containsKey("webhook_url")) {
            toSave.put("webhook_url", body.get("webhook_url"));
        }
        if (body.containsKey("webhook_enabled")) {
            String enabled = body.get("webhook_enabled");
            // Accepter "true"/"false" sous toutes les formes
            toSave.put("webhook_enabled", Boolean.parseBoolean(enabled) ? "true" : "false");
        }
        // Ne mettre à jour le secret que s'il est explicitement fourni et non vide
        if (body.containsKey("webhook_secret") && !body.get("webhook_secret").isBlank()) {
            toSave.put("webhook_secret", body.get("webhook_secret"));
        }

        appSettingService.updateSettings(toSave);

        log.info("WEBHOOK: configuration mise à jour par un admin — url={}, enabled={}",
                toSave.get("webhook_url"), toSave.get("webhook_enabled"));

        return ResponseEntity.ok(Map.of("message", "Configuration webhook enregistrée."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/admin/webhook/test
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Envoie un payload WEBHOOK_TEST vers l'URL configurée.
     * Permet de vérifier que la connexion et la signature fonctionnent
     * avant d'activer le webhook en production.
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> sendTest() {
        String url    = webhookSettingsResolver.url();
        String secret = webhookSettingsResolver.secret();

        if (url.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "webhook_url non configurée. Renseignez-la d'abord."));
        }

        try {
            webhookService.sendTest(url, secret);
            return ResponseEntity.ok(Map.of(
                    "message", "Payload de test envoyé vers " + url,
                    "event",   WebhookEventType.WEBHOOK_TEST.name()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
