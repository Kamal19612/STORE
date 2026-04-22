package com.sucrestore.api.webhook;

import com.sucrestore.api.config.AppProperties;
import com.sucrestore.api.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Résout l'URL, le secret et l'activation du webhook : priorité aux valeurs
 * {@code app_settings}, puis repli sur {@code application.yml} / variables d'environnement.
 */
@Component
@RequiredArgsConstructor
public class WebhookSettingsResolver {

    private static final String KEY_URL              = "webhook_url";
    private static final String KEY_SECRET           = "webhook_secret";
    private static final String KEY_ENABLED          = "webhook_enabled";
    private static final String KEY_SOURCE_IDENTIFIER = "webhook_source_identifier";

    private final AppSettingService appSettingService;
    private final AppProperties     appProperties;

    public String url() {
        return appSettingService.getSettingValue(KEY_URL)
                .filter(s -> !s.isBlank())
                .orElseGet(() -> blankToEmpty(appProperties.getWebhook().getRelayUrl()));
    }

    public String secret() {
        return appSettingService.getSettingValue(KEY_SECRET)
                .filter(s -> !s.isBlank())
                .orElseGet(() -> blankToEmpty(appProperties.getWebhook().getSecret()));
    }

    public boolean enabled() {
        return appSettingService.getSettingValue(KEY_ENABLED)
                .filter(s -> !s.isBlank())
                .map("true"::equalsIgnoreCase)
                .orElse(appProperties.getWebhook().isEnabled());
    }

    /**
     * Valeur du champ JSON {@code source} (ex. {@code sucre_store}, {@code shop_b}).
     * Vide si non configuré — l'app mobile tentera quand même le field_mapping sans source.
     */
    public String sourceIdentifier() {
        return appSettingService.getSettingValue(KEY_SOURCE_IDENTIFIER)
                .filter(s -> !s.isBlank())
                .orElseGet(() -> blankToEmpty(appProperties.getWebhook().getSourceIdentifier()));
    }

    private static String blankToEmpty(String s) {
        return s == null || s.isBlank() ? "" : s;
    }
}
