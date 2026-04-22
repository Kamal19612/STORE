package com.sucrestore.api.webhook;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Enveloppe JSON envoyée à chaque appel webhook.
 *
 * Structure reçue côté application mobile :
 * {
 *   "event"     : "ORDER_CONFIRMED",
 *   "timestamp" : "2026-04-10T14:30:00",
 *   "version"   : "1.0",
 *   "order"     : { ... mêmes champs que la page livreur ... }
 * }
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebhookPayload {

    /** Type d'événement déclencheur */
    private WebhookEventType event;

    /** Horodatage ISO-8601 de l'émission */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /** Version du format payload (pour évolutions futures sans casser le contrat) */
    @Builder.Default
    private String version = "1.0";

    /** Données complètes de la commande concernée */
    private WebhookOrderPayload order;

    /**
     * Identifiant de la boutique / backend (aligné sur {@code source_identifier}
     * côté app mobile). Permet à Spirit de distinguer plusieurs intégrations.
     */
    private String source;
}
