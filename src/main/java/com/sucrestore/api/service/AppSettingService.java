package com.sucrestore.api.service;

import com.sucrestore.api.entity.AppSetting;
import com.sucrestore.api.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sucrestore.api.tenant.StoreContext;

@Service
@RequiredArgsConstructor
public class AppSettingService {

    private final AppSettingRepository appSettingRepository;

    public List<AppSetting> getAllSettings() {
        Long storeId = StoreContext.getStoreIdOrNull();
        // For now: return only settings of current store.
        // (Delivery/global admin module can query other stores via a dedicated endpoint later.)
        return appSettingRepository.findAll().stream()
            .filter(s -> s.getStore() != null && storeId != null && storeId.equals(s.getStore().getId()))
            .toList();
    }

    public Optional<String> getSettingValue(String key) {
        Long storeId = StoreContext.getStoreIdOrNull();
        // Prefer store-scoped setting; fallback to legacy global (store NULL) for backward compatibility.
        return appSettingRepository.findByKeyAndStoreId(key, storeId)
            .or(() -> appSettingRepository.findByKeyAndStoreIsNull(key))
            .map(AppSetting::getValue);
    }

    @Transactional
    public void updateSettings(Map<String, String> newSettings) {
        Long storeId = StoreContext.getStoreIdOrNull();
        for (Map.Entry<String, String> entry : newSettings.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            AppSetting setting = appSettingRepository.findByKeyAndStoreId(key, storeId)
                .orElse(AppSetting.builder()
                    .key(key)
                    .store(com.sucrestore.api.entity.Store.builder().id(storeId).build())
                    .build());

            // Normalisation légère pour éviter des valeurs cassées côté liens WhatsApp.
            if ("whatsapp_number".equals(key)) {
                setting.setValue(normalizeWhatsAppNumber(value));
            } else if ("customer_whatsapp_dial_code".equals(key)) {
                setting.setValue(normalizeDialCode(value));
            } else if ("telegram_bot_token".equals(key)) {
                setting.setValue(value == null ? "" : value.trim());
            } else if ("telegram_chat_id".equals(key)) {
                setting.setValue(value == null ? "" : value.trim());
            } else {
                setting.setValue(value);
            }
            appSettingRepository.save(setting);
        }
    }

    /**
     * WhatsApp attend un numéro au format international en chiffres uniquement, sans "+".
     * Exemple BF: 22670123456. Si l'admin saisit un numéro local (8 chiffres), on préfixe 226.
     */
    private String normalizeWhatsAppNumber(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        }
        if (digits.isBlank()) {
            return "";
        }
        if (!digits.startsWith("226") && digits.length() <= 8) {
            return "226" + digits;
        }
        return digits;
    }

    /**
     * Normalise un indicatif au format "+226" (ou vide).
     * Accepte "+226", "226", "00226", "+ 226" et nettoie les caractères.
     */
    private String normalizeDialCode(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return "";
        }
        // garder + et chiffres uniquement
        v = v.replaceAll("[^0-9+]", "");
        if (v.startsWith("00")) {
            v = "+" + v.substring(2);
        }
        if (!v.startsWith("+")) {
            v = "+" + v.replaceAll("[^0-9]", "");
        }
        return v.equals("+") ? "" : v;
    }

    public Map<String, String> getPublicSettings() {
        Map<String, String> publicSettings = new HashMap<>();
        // Définir les clés publiques autorisées
        String[] publicKeys = {
            "contact_phone", "contact_email", "contact_address",
            "social_facebook", "social_instagram", "footer_copyright",
            "whatsapp_number", "store_name", "store_location",
            "customer_whatsapp_dial_code",
            "dist_tier_1_limit", "dist_tier_1_price",
            "dist_tier_2_limit", "dist_tier_2_price",
            "dist_tier_3_price", "min_order_free_delivery",
            "express_surcharge", "scheduled_surcharge"
        };

        for (String key : publicKeys) {
            getSettingValue(key).ifPresent(value -> publicSettings.put(key, value));
        }
        return publicSettings;
    }
}
