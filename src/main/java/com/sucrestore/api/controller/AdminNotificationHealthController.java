package com.sucrestore.api.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sucrestore.api.config.AppProperties;
import com.sucrestore.api.entity.User;
import com.sucrestore.api.repository.DeliveryDeviceTokenRepository;
import com.sucrestore.api.service.TelegramService;

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
public class AdminNotificationHealthController {

    private final TelegramService telegramService;
    private final AppProperties appProperties;
    private final DeliveryDeviceTokenRepository deviceTokenRepo;

    public AdminNotificationHealthController(
        TelegramService telegramService,
        AppProperties appProperties,
        DeliveryDeviceTokenRepository deviceTokenRepo
    ) {
        this.telegramService = telegramService;
        this.appProperties = appProperties;
        this.deviceTokenRepo = deviceTokenRepo;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean telegramConfigured = telegramService.isConfigured();
        boolean telegramWorking = telegramService.healthCheckTokenOnly();

        boolean fcmEnabled = appProperties.getDelivery().getFcm().isEnabled();
        // "initialized" est interne au FcmService; ici on expose l'état "configurable" (enabled + path set).
        String fcmPath = appProperties.getDelivery().getFcm().getServiceAccountPath();
        boolean fcmConfigPresent = fcmPath != null && !fcmPath.isBlank();

        int adminTokens = deviceTokenRepo.findByUserRoleAndIsActiveTrue(User.Role.ADMIN).size()
            + deviceTokenRepo.findByUserRoleAndIsActiveTrue(User.Role.SUPER_ADMIN).size()
            + deviceTokenRepo.findByUserRoleAndIsActiveTrue(User.Role.MANAGER).size();
        int deliveryTokens = deviceTokenRepo.findByUserRoleAndIsActiveTrue(User.Role.DELIVERY_AGENT).size();

        return ResponseEntity.ok(Map.of(
            "telegram", Map.of(
                "configured", telegramConfigured,
                "working", telegramWorking
            ),
            "fcm", Map.of(
                "enabled", fcmEnabled,
                "configPresent", fcmConfigPresent
            ),
            "tokens", Map.of(
                "admin", adminTokens,
                "delivery", deliveryTokens
            ),
            "hint", "Pour les push mobile (admin + livreur): activer FCM (app.delivery.fcm.*), enregistrer le token via POST /api/delivery/devices/register. Telegram: app_settings telegram_bot_token + telegram_chat_id."
        ));
    }
}

