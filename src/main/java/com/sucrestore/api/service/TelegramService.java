package com.sucrestore.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.sucrestore.api.service.AppSettingService;

import com.sucrestore.api.entity.Order;
import com.sucrestore.api.entity.OrderItem;

@Service
public class TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);
    private static final String TELEGRAM_API = "https://api.telegram.org/bot";

    @Value("${app.telegram.bot-token:}")
    private String botToken;

    @Value("${app.telegram.chat-id:}")
    private String defaultChatId;

    @Autowired
    private AppSettingService appSettingService;

    private String getEffectiveChatId() {
        if (appSettingService != null) {
            String dbChatId = appSettingService.getSettingValue("telegram_chat_id").orElse("");
            if (!dbChatId.isBlank()) {
                return dbChatId;
            }
        }
        return defaultChatId;
    }

    @Value("${app.telegram.webhook-url:}")
    private String webhookUrl;

    @Value("${app.telegram.webhook-secret:}")
    private String webhookSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    // --- Enregistrement du webhook au démarrage ---

    @EventListener(ApplicationReadyEvent.class)
    public void registerWebhookOnStartup() {
        if (botToken.isBlank() || webhookUrl.isBlank()) {
            log.info("Telegram webhook non enregistré (bot-token ou webhook-url manquant)");
            return;
        }
        try {
            String url = TELEGRAM_API + botToken + "/setWebhook";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("url", webhookUrl + "/api/telegram/webhook");
            if (!webhookSecret.isBlank()) {
                body.put("secret_token", webhookSecret);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(url, request, String.class);
            log.info("Telegram webhook enregistré : {}", response);
        } catch (Exception e) {
            log.warn("Échec enregistrement webhook Telegram : {}", e.getMessage());
        }
    }

    // --- Notification nouvelle commande avec boutons ---

    public void sendNewOrderNotification(Order order, String currency) {
        if (botToken.isBlank() || getEffectiveChatId().isBlank()) {
            log.debug("Telegram non configuré — notification ignorée pour commande {}", order.getOrderNumber());
            return;
        }
        String message = buildNewOrderMessage(order, currency);
        sendMessageWithButtons(message, order.getId());
    }

    private void sendMessageWithButtons(String text, Long orderId) {
        try {
            String url = TELEGRAM_API + botToken + "/sendMessage";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", getEffectiveChatId());
            body.put("text", text);
            body.put("parse_mode", "HTML");
            body.put("reply_markup", Map.of(
                "inline_keyboard", List.of(
                    List.of(
                        Map.of("text", "✅ VALIDER", "callback_data", "confirm_" + orderId),
                        Map.of("text", "❌ ANNULER", "callback_data", "cancel_" + orderId)
                    )
                )
            ));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, request, String.class);
            log.debug("Notification Telegram avec boutons envoyée pour commande {}", orderId);
        } catch (Exception e) {
            log.warn("Échec envoi notification Telegram : {}", e.getMessage());
        }
    }

    // --- Répondre à un clic de bouton (retire le spinner Telegram) ---

    public void answerCallbackQuery(String callbackQueryId, String text) {
        try {
            String url = TELEGRAM_API + botToken + "/answerCallbackQuery";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("callback_query_id", callbackQueryId);
            body.put("text", text);
            body.put("show_alert", false);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, request, String.class);
        } catch (Exception e) {
            log.warn("Échec answerCallbackQuery : {}", e.getMessage());
        }
    }

    // --- Mettre à jour le message après action (remplace les boutons par le résultat) ---

    public void editMessageAfterAction(Long chatIdLong, Integer messageId, String newText) {
        try {
            String url = TELEGRAM_API + botToken + "/editMessageText";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatIdLong);
            body.put("message_id", messageId);
            body.put("text", newText);
            body.put("parse_mode", "HTML");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, request, String.class);
        } catch (Exception e) {
            log.warn("Échec editMessageText : {}", e.getMessage());
        }
    }

    // --- Envoi d'un message texte simple ---

    public void sendText(String text) {
        if (botToken.isBlank() || getEffectiveChatId().isBlank()) return;
        try {
            String url = TELEGRAM_API + botToken + "/sendMessage";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", getEffectiveChatId());
            body.put("text", text);
            body.put("parse_mode", "HTML");
            restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
        } catch (Exception e) {
            log.warn("Échec sendText Telegram : {}", e.getMessage());
        }
    }

    // --- Suivi WhatsApp après validation/annulation ---

    /**
     * Envoie un message de suivi dans Telegram après une action sur une commande,
     * avec un bouton inline qui ouvre directement WhatsApp pour notifier le client.
     */
    public void sendFollowUpWithWhatsApp(Long chatId, String orderNumber, String customerName,
                                         String customerPhone, String whatsappLink, boolean isConfirmed) {
        try {
            String url = TELEGRAM_API + botToken + "/sendMessage";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String statusEmoji = isConfirmed ? "✅" : "❌";
            String statusLabel = isConfirmed ? "Confirmée" : "Annulée";

            String text = "📱 <b>Notifier le client</b>\n\n"
                + "Commande : <code>" + escapeHtml(orderNumber) + "</code> — " + escapeHtml(customerName) + "\n"
                + "Tel : " + escapeHtml(customerPhone) + "\n"
                + "Statut : " + statusEmoji + " <b>" + statusLabel + "</b>\n\n"
                + "Appuyez sur le bouton pour envoyer le message au client via WhatsApp :";

            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", text);
            body.put("parse_mode", "HTML");
            body.put("reply_markup", Map.of(
                "inline_keyboard", List.of(
                    List.of(
                        Map.of("text", "💬 Notifier le client", "url", whatsappLink)
                    )
                )
            ));

            restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
            log.debug("Follow-up WhatsApp Telegram envoyé pour commande {}", orderNumber);
        } catch (Exception e) {
            log.warn("Échec follow-up WhatsApp Telegram : {}", e.getMessage());
        }
    }

    // --- Liste des commandes en attente ---

    /**
     * Envoie la liste des commandes PENDING avec boutons VALIDER/ANNULER pour chacune.
     */
    public void sendPendingOrdersMenu(List<Order> orders, String currency) {
        if (botToken.isBlank() || getEffectiveChatId().isBlank()) return;
        try {
            if (orders.isEmpty()) {
                sendText("✅ Aucune commande en attente pour le moment.");
                return;
            }

            String url = TELEGRAM_API + botToken + "/sendMessage";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            StringBuilder sb = new StringBuilder();
            sb.append("📋 <b>Commandes en attente (").append(orders.size()).append(")</b>\n\n");

            List<List<Map<String, String>>> keyboard = new java.util.ArrayList<>();

            for (int i = 0; i < orders.size(); i++) {
                Order o = orders.get(i);
                sb.append(i + 1).append(". <code>").append(escapeHtml(o.getOrderNumber())).append("</code> — ")
                  .append(escapeHtml(o.getCustomerName()))
                  .append(" — ").append(o.getTotal()).append(" ").append(escapeHtml(currency));
                if (o.getDeliveryType() != null) {
                    String t = "EXPRESS".equals(o.getDeliveryType()) ? " ⚡" : " 🕐";
                    sb.append(t);
                }
                sb.append("\n");

                keyboard.add(List.of(
                    Map.of("text", "✅ " + o.getOrderNumber(), "callback_data", "confirm_" + o.getId()),
                    Map.of("text", "❌ " + o.getOrderNumber(), "callback_data", "cancel_" + o.getId())
                ));
            }

            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", getEffectiveChatId());
            body.put("text", sb.toString());
            body.put("parse_mode", "HTML");
            body.put("reply_markup", Map.of("inline_keyboard", keyboard));

            restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
        } catch (Exception e) {
            log.warn("Échec sendPendingOrdersMenu Telegram : {}", e.getMessage());
        }
    }

    // --- Construction du message ---

    private String buildNewOrderMessage(Order order, String currency) {
        StringBuilder sb = new StringBuilder();
        sb.append("🛒 <b>NOUVELLE COMMANDE</b>\n\n");
        sb.append("📋 N° : <code>").append(escapeHtml(order.getOrderNumber())).append("</code>\n");
        sb.append("👤 Client : ").append(escapeHtml(order.getCustomerName())).append("\n");
        sb.append("📞 Tel : ").append(escapeHtml(order.getCustomerPhone())).append("\n");
        sb.append("📍 Adresse : ").append(escapeHtml(order.getCustomerAddress())).append("\n");

        if (order.getDeliveryType() != null) {
            String typeLabel = switch (order.getDeliveryType()) {
                case "EXPRESS" -> "⚡ Express";
                case "PROGRAMMER" -> "🕐 Programmée";
                default -> escapeHtml(order.getDeliveryType());
            };
            sb.append("🚚 Livraison : ").append(typeLabel);
            if ("PROGRAMMER".equals(order.getDeliveryType()) && order.getScheduledTime() != null) {
                sb.append(" à ").append(escapeHtml(order.getScheduledTime()));
            }
            sb.append("\n");
        }

        if (order.getCustomerLatitude() != null && order.getCustomerLongitude() != null) {
            sb.append("🗺 <a href=\"https://www.google.com/maps?q=")
              .append(order.getCustomerLatitude()).append(",").append(order.getCustomerLongitude())
              .append("\">Voir sur Maps</a>\n");
        } else if (order.getManualLocationLink() != null && !order.getManualLocationLink().isBlank()) {
            sb.append("🗺 <a href=\"").append(escapeHtml(order.getManualLocationLink())).append("\">Voir sur Maps</a>\n");
        }

        if (order.getCustomerNotes() != null && !order.getCustomerNotes().isBlank()) {
            sb.append("📝 Notes : ").append(escapeHtml(order.getCustomerNotes())).append("\n");
        }

        sb.append("\n<b>Articles :</b>\n");
        for (OrderItem item : order.getItems()) {
            sb.append("  • ").append(item.getQuantity()).append("x ")
              .append(escapeHtml(item.getProduct().getName()))
              .append(" — ").append(item.getTotalPrice()).append(" ").append(escapeHtml(currency)).append("\n");
        }

        sb.append("\n💰 <b>Total : ").append(order.getTotal()).append(" ").append(escapeHtml(currency)).append("</b>");
        return sb.toString();
    }

    // Échappe les caractères spéciaux HTML pour Telegram parse_mode=HTML
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }
}