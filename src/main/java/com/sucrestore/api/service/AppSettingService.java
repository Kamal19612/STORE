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

@Service
@RequiredArgsConstructor
public class AppSettingService {

    private final AppSettingRepository appSettingRepository;

    public List<AppSetting> getAllSettings() {
        return appSettingRepository.findAll();
    }

    public Optional<String> getSettingValue(String key) {
        return appSettingRepository.findByKey(key).map(AppSetting::getValue);
    }

    @Transactional
    public void updateSettings(Map<String, String> newSettings) {
        for (Map.Entry<String, String> entry : newSettings.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            AppSetting setting = appSettingRepository.findByKey(key)
                    .orElse(AppSetting.builder().key(key).build());

            setting.setValue(value);
            appSettingRepository.save(setting);
        }
    }

    public Map<String, String> getPublicSettings() {
        Map<String, String> publicSettings = new HashMap<>();
        // Définir les clés publiques autorisées
        String[] publicKeys = {
            "contact_phone", "contact_email", "contact_address",
            "social_facebook", "social_instagram", "footer_copyright",
            "whatsapp_number", "store_name"
        };

        for (String key : publicKeys) {
            getSettingValue(key).ifPresent(value -> publicSettings.put(key, value));
        }
        return publicSettings;
    }
}
