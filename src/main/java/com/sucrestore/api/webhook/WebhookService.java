package com.sucrestore.api.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sucrestore.api.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Service responsable de l'envoi des webhooks vers l'application mobile.
 *
 * Configuration : clés {@code app_settings} (prioritaires), sinon
 * {@code app.webhook.*} dans {@code application.yml} / variables d'environnement.
 *
 * Chaque appel est asynchrone (@Async) : il ne bloque jamais la transaction
 * principale en cours (changement de statut, etc.).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookSettingsResolver webhookSettingsResolver;
    private final ObjectMapper            objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // Point d'entrée principal — appelé depuis OrderService
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Envoie un webhook de manière asynchrone.
     *
     * Ne lève jamais d'exception vers l'appelant : les erreurs webhook ne doivent
     * jamais bloquer ni annuler une transaction métier.
     */
    @Async
    public void sendAsync(WebhookEventType event, Order order) {
        try {
            // 1. Vérifier que le webhook est activé
            if (!webhookSettingsResolver.enabled()) {
                log.debug("WEBHOOK: désactivé — événement {} ignoré", event);
                return;
            }

            // 2. Lire l'URL cible
            String url = webhookSettingsResolver.url();
            if (url.isBlank()) {
                log.warn("WEBHOOK: webhook_url non configurée — événement {} ignoré", event);
                return;
            }

            // 3. Construire le payload
            String sourceId = webhookSettingsResolver.sourceIdentifier();
            WebhookPayload payload = WebhookPayload.builder()
                    .event(event)
                    .order(WebhookOrderPayload.from(order))
                    .source(sourceId.isBlank() ? null : sourceId)
                    .build();

            String jsonBody = objectMapper.writeValueAsString(payload);

            // 4. Signer le corps avec HMAC-SHA256
            String secret = webhookSettingsResolver.secret();
            String signature = computeHmacSha256(jsonBody, secret);

            // 5. Préparer les headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-Signature", "sha256=" + signature);
            headers.set("X-Webhook-Event", event.name());
            headers.set("X-Webhook-Version", payload.getVersion());

            // 6. Envoyer le POST HTTP
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            restTemplate.postForEntity(url, request, String.class);

            log.info("WEBHOOK: événement {} envoyé avec succès vers {}", event, url);

        } catch (Exception e) {
            // Ne jamais propager l'exception — le webhook est best-effort
            log.error("WEBHOOK: échec envoi événement {} — {}", event, e.getMessage());
        }
    }

    /**
     * Surcharge pour envoyer un payload de test sans commande réelle.
     * Utilisée par le WebhookController (bouton "Tester").
     */
    @Async
    public void sendTest(String url, String secret) {
        try {
            WebhookPayload payload = WebhookPayload.builder()
                    .event(WebhookEventType.WEBHOOK_TEST)
                    .order(buildTestOrderPayload())
                    .build();

            String jsonBody = objectMapper.writeValueAsString(payload);
            String signature = computeHmacSha256(jsonBody, secret != null ? secret : "");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-Signature", "sha256=" + signature);
            headers.set("X-Webhook-Event", WebhookEventType.WEBHOOK_TEST.name());
            headers.set("X-Webhook-Version", payload.getVersion());

            RestTemplate restTemplate = new RestTemplate();
            restTemplate.postForEntity(url, new HttpEntity<>(jsonBody, headers), String.class);

            log.info("WEBHOOK: payload de test envoyé vers {}", url);

        } catch (Exception e) {
            log.error("WEBHOOK: échec payload test vers {} — {}", url, e.getMessage());
            throw new RuntimeException("Échec du test webhook : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers privés
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Calcule la signature HMAC-SHA256 du corps JSON.
     * Format retourné : chaîne hex (sans préfixe "sha256=").
     */
    private String computeHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hmacBytes);
    }

    /**
     * Construit un ordre fictif pour le payload de test.
     */
    private WebhookOrderPayload buildTestOrderPayload() {
        return WebhookOrderPayload.builder()
                .id(0L)
                .orderNumber("ORD-TEST-0000")
                .customerName("Client Test")
                .customerPhone("0600000000")
                .customerAddress("Adresse de test, Abidjan")
                .deliveryType("STANDARD")
                .total(java.math.BigDecimal.valueOf(5000))
                .deliveryCost(java.math.BigDecimal.valueOf(500))
                .status("CONFIRMED")
                .createdAt(java.time.LocalDateTime.now())
                .items(java.util.List.of())
                .build();
    }
}
