package com.sucrestore.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Classe de configuration typée pour les propriétés de l'application
 * (application.yml). Permet d'éviter les avertissements "Unknown property" et
 * offre l'autocomplétion.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    private Jwt jwt = new Jwt();

    @Data
    public static class Jwt {

        private String secret;
        private long expiration;
    }

    private String whatsappNumber;
    private String storeName;
    private String storePhone;
    private String currency = "FCFA"; // Valeur par défaut

    private final Storage storage = new Storage();

    private final Webhook webhook = new Webhook();

    @Data
    public static class Storage {

        private String location = "uploads";
    }

    /**
     * Valeurs par défaut quand les clés {@code webhook_*} ne sont pas encore
     * renseignées dans {@code app_settings} (ex. premier démarrage ou CI).
     * Peuvent être surchargées par variables d'environnement.
     */
    @Data
    public static class Webhook {

        /** URL complète du POST (ex. http://127.0.0.1:3001/webhook). */
        private String relayUrl = "http://127.0.0.1:3001/webhook";

        /** Secret HMAC partagé avec WebhookRelay. */
        private String secret = "change_me_avec_un_secret_fort";

        /** Si aucune valeur en base, ce booléen s'applique (défaut : actif en dev). */
        private boolean enabled = true;

        /**
         * Identifiant stable de cette boutique dans le JSON webhook (champ {@code source}),
         * pour que l'app mobile rattache l'événement à la bonne entrée {@code external_sources}.
         */
        private String sourceIdentifier = "";
    }
}
