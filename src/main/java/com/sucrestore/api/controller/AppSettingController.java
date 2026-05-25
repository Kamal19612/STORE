package com.sucrestore.api.controller;

import com.sucrestore.api.entity.AppSetting;
import com.sucrestore.api.service.AppSettingService;
import com.sucrestore.api.service.TelegramService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppSettingController {

    private final AppSettingService appSettingService;
    private final TelegramService telegramService;

    @GetMapping("/public/settings")
    public Map<String, String> getPublicSettings() {
        return appSettingService.getPublicSettings();
    }

    @GetMapping("/admin/settings")
    public List<AppSetting> getAllSettings() {
        return appSettingService.getAllSettings();
    }

    @PutMapping("/admin/settings")
    public void updateSettings(@RequestBody Map<String, String> settings) {
        appSettingService.updateSettings(settings);
        // Webhook Telegram: toujours actif → tentative d'enregistrement après sauvegarde
        // (utile quand le bot-token est stocké en DB et ne peut pas être pris au démarrage).
        telegramService.registerWebhookNow();
    }
}
