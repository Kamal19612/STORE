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

    @Data
    public static class Storage {

        private String location = "uploads";
    }
}
