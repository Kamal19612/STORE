package com.sucrestore.api.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.sucrestore.api.entity.Order;
import com.sucrestore.api.entity.OrderItem;

/**
 * Service d'envoi de notifications via Telegram Bot API.
 * Configure app.telegram.bot-token et app.telegram.chat-id dans application.properties.
 */
@Service
public class TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);
    private static final String TELEGRAM_API = "https://api.telegram.org/bot";

    @Value("${app.telegram.bot-token:}")
    private String botToken;

    @Value("${app.telegram.chat-id:}")
    private String chatId;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Envoie une notification Telegram lors d'une nouvelle commande.
     * Ne fait rien si le bot-token ou chat-id n'est pas configuré.
     */
    public void sendNewOrderNotification(Order order, String currency) {
        if (botToken.isBlank() || chatId.isBlank()) {
            log.debug("Telegram non configuré — notification ignorée pour commande {}", order.getOrderNumber());
            return;
        }

        String message = buildNewOrderMessage(order, currency);
        sendMessage(message);
    }

    private String buildNewOrderMessage(Order order, String currency) {
        StringBuilder sb = new StringBuilder();
        sb.append("🛒 *NOUVELLE COMMANDE*\n\n");
        sb.append("📋 N° : `").append(order.getOrderNumber()).append("`\n");
        sb.append("👤 Client : ").append(escapeMarkdown(order.getCustomerName())).append("\n");
        sb.append("📞 Tel : ").append(order.getCustomerPhone()).append("\n");
        sb.append("📍 Adresse : ").append(escapeMarkdown(order.getCustomerAddress())).append("\n");

        if (order.getDeliveryType() != null) {
            String typeLabel = switch (order.getDeliveryType()) {
                case "EXPRESS" -> "⚡ Express";
                case "PROGRAMMER" -> "🕐 Programmée";
                default -> order.getDeliveryType();
            };
            sb.append("🚚 Livraison : ").append(typeLabel);
            if ("PROGRAMMER".equals(order.getDeliveryType()) && order.getScheduledTime() != null) {
                sb.append(" à ").append(order.getScheduledTime());
            }
            sb.append("\n");
        }

        if (order.getCustomerLatitude() != null && order.getCustomerLongitude() != null) {
            sb.append("🗺 Position : https://www.google.com/maps?q=")
              .append(order.getCustomerLatitude()).append(",").append(order.getCustomerLongitude()).append("\n");
        } else if (order.getManualLocationLink() != null && !order.getManualLocationLink().isBlank()) {
            sb.append("🗺 Position : ").append(order.getManualLocationLink()).append("\n");
        }

        if (order.getCustomerNotes() != null && !order.getCustomerNotes().isBlank()) {
            sb.append("📝 Notes : ").append(escapeMarkdown(order.getCustomerNotes())).append("\n");
        }

        sb.append("\n*Articles :*\n");
        for (OrderItem item : order.getItems()) {
            sb.append("  • ").append(item.getQuantity()).append("x ")
              .append(escapeMarkdown(item.getProduct().getName()))
              .append(" — ").append(item.getTotalPrice()).append(" ").append(currency).append("\n");
        }

        sb.append("\n💰 *Total : ").append(order.getTotal()).append(" ").append(currency).append("*");

        return sb.toString();
    }

    private void sendMessage(String text) {
        try {
            String url = TELEGRAM_API + botToken + "/sendMessage";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", text);
            body.put("parse_mode", "Markdown");

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, request, String.class);

            log.debug("Notification Telegram envoyée avec succès");
        } catch (Exception e) {
            log.warn("Échec envoi notification Telegram : {}", e.getMessage());
        }
    }

    /**
     * Échappe les caractères spéciaux Markdown Telegram.
     */
    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replace("_", "\\_").replace("*", "\\*").replace("[", "\\[").replace("`", "\\`");
    }
}