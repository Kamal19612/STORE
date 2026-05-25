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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

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

    private record TelegramConfig(String token, String chatId, Source source) {
        enum Source { DB, ENV }
        boolean isComplete() { return token != null && !token.isBlank() && chatId != null && !chatId.isBlank(); }
    }

    private TelegramConfig resolveConfigPreferDb() {
        if (appSettingService != null) {
            String dbToken = appSettingService.getSettingValue("telegram_bot_token").orElse("").trim();
            String dbChatId = appSettingService.getSettingValue("telegram_chat_id").orElse("").trim();
            if (!dbToken.isBlank() && !dbChatId.isBlank()) {
                return new TelegramConfig(dbToken, dbChatId, TelegramConfig.Source.DB);
            }
        }
        return new TelegramConfig(
            botToken == null ? "" : botToken.trim(),
            defaultChatId == null ? "" : defaultChatId.trim(),
            TelegramConfig.Source.ENV
        );
    }

    private TelegramConfig resolveEnvConfig() {
        return new TelegramConfig(
            botToken == null ? "" : botToken.trim(),
            defaultChatId == null ? "" : defaultChatId.trim(),
            TelegramConfig.Source.ENV
        );
    }

    // Compat interne (autres méthodes du service) : config préférée DB.
    private String getEffectiveBotToken() {
        return resolveConfigPreferDb().token();
    }

    private String getEffectiveChatId() {
        return resolveConfigPreferDb().chatId();
    }

    private boolean isTelegramConfigError(HttpStatusCode status) {
        // 401/403: token invalide / interdit
        // 400: souvent "chat not found" / chat_id invalide / bot pas dans le chat / parse error
        return status != null && (status.value() == 400 || status.value() == 401 || status.value() == 403);
    }

    private void logDbOverrideFallback(String operation, HttpStatusCode status, String responseBody) {
        log.warn("[NOTIF] channel=TELEGRAM order= status=FAIL error=DB override suspected → fallback ENV op={} http={} body={}",
            operation,
            status != null ? status.value() : -1,
            responseBody == null ? "" : responseBody);
    }

    @Value("${app.telegram.webhook-url:}")
    private String webhookUrl;

    @Value("${app.telegram.webhook-secret:}")
    private String webhookSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    public record WebhookRegistrationResult(boolean attempted, boolean success, String message) {}

    public boolean isConfigured() {
        TelegramConfig cfg = resolveConfigPreferDb();
        return cfg.isComplete();
    }

    /**
     * Vérifie uniquement la validité du bot token via getMe.
     * Ne requiert pas chat_id, ce qui évite les faux négatifs (bot pas dans le chat).
     */
    public boolean healthCheckTokenOnly() {
        TelegramConfig cfg = resolveConfigPreferDb();
        if (cfg.token() == null || cfg.token().isBlank()) return false;
        try {
            String url = TELEGRAM_API + cfg.token() + "/getMe";
            String resp = restTemplate.getForObject(url, String.class);
            return resp != null && resp.contains("\"ok\":true");
        } catch (Exception e) {
            return false;
        }
    }

    // --- Enregistrement du webhook au démarrage ---

    @EventListener(ApplicationReadyEvent.class)
    public void registerWebhookOnStartup() {
        WebhookRegistrationResult res = registerWebhookNow();
        if (!res.attempted()) {
            log.info("Telegram webhook non enregistré (bot-token ou webhook-url manquant)");
        } else if (res.success()) {
            log.info("Telegram webhook enregistré : {}", res.message());
        } else {
            log.warn("Échec enregistrement webhook Telegram : {}", res.message());
        }
    }

    /**
     * Enregistre (ou met à jour) le webhook Telegram immédiatement.
     * Utile quand le bot-token est stocké en DB (store-scopé) et donc indisponible au démarrage.
     */
    public WebhookRegistrationResult registerWebhookNow() {
        String effectiveToken = getEffectiveBotToken();
        if (effectiveToken == null || effectiveToken.isBlank() || webhookUrl == null || webhookUrl.isBlank()) {
            return new WebhookRegistrationResult(false, false, "missing_token_or_webhook_url");
        }
        try {
            String url = TELEGRAM_API + effectiveToken + "/setWebhook";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            // IMPORTANT: store-scoped webhook URL so callbacks can resolve tenant reliably.
            // If StoreContext is unavailable, fallback to legacy endpoint.
            String storeCode = com.sucrestore.api.tenant.StoreContext.getStoreCodeOrNull();
            String path = (storeCode != null && !storeCode.isBlank())
                ? ("/api/telegram/" + storeCode + "/webhook")
                : "/api/telegram/webhook";
            body.put("url", webhookUrl + path);
            if (webhookSecret != null && !webhookSecret.isBlank()) {
                body.put("secret_token", webhookSecret);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(url, request, String.class);
            return new WebhookRegistrationResult(true, true, response == null ? "" : response);
        } catch (Exception e) {
            return new WebhookRegistrationResult(true, false, e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    /**
     * Retourne l'état du webhook côté Telegram (getWebhookInfo).
     * Sert au debug quand les boutons ne déclenchent rien.
     */
    public String getWebhookInfoRaw() {
        String effectiveToken = getEffectiveBotToken();
        if (effectiveToken == null || effectiveToken.isBlank()) {
            return "{\"ok\":false,\"error\":\"missing_bot_token\"}";
        }
        try {
            String url = TELEGRAM_API + effectiveToken + "/getWebhookInfo";
            String resp = restTemplate.getForObject(url, String.class);
            return resp == null ? "" : resp;
        } catch (Exception e) {
            return "{\"ok\":false,\"error\":\"" + escapeForJson(e.getMessage()) + "\"}";
        }
    }

    /**
     * Désactive le webhook Telegram (setWebhook url="").
     */
    public WebhookRegistrationResult unregisterWebhookNow() {
        String effectiveToken = getEffectiveBotToken();
        if (effectiveToken == null || effectiveToken.isBlank()) {
            return new WebhookRegistrationResult(false, false, "missing_bot_token");
        }
        try {
            String url = TELEGRAM_API + effectiveToken + "/setWebhook";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("url", "");
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(url, request, String.class);
            return new WebhookRegistrationResult(true, true, response == null ? "" : response);
        } catch (Exception e) {
            return new WebhookRegistrationResult(true, false, e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    /**
     * Envoie un message de test (permet de valider token + chat_id sans attendre une commande).
     */
    public void sendTestMessage(String text) {
        String msg = (text == null || text.isBlank()) ? "✅ Test Telegram OK" : text;
        sendText("🧪 <b>Test</b>\n" + escapeHtml(msg));
    }

    private String escapeForJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // --- Notification nouvelle commande avec boutons ---

    public void sendNewOrderNotification(Order order, String currency) {
        TelegramConfig cfg = resolveConfigPreferDb();
        if (!cfg.isComplete()) {
            log.warn(
                "Telegram non configuré (bot-token ou chat-id vides après résolution env + app_settings) — notification ignorée pour commande {}",
                order.getOrderNumber());
            return;
        }
        String message = buildNewOrderMessage(order, currency);
        sendMessageWithButtons(cfg, message, order.getId(), order.getOrderNumber());
    }

    /**
     * Envoie le message Telegram ; lève une exception si l'envoi échoue définitivement
     * (permet à l'outbox / NotificationRunner de marquer FAIL et relancer).
     */
    private void sendMessageWithButtons(TelegramConfig cfg, String text, Long orderId, String orderNumber) {
        try {
            postSendMessageWithButtons(cfg, text, orderId, orderNumber);
            log.info("[NOTIF] channel=TELEGRAM order={} status=SUCCESS", orderNumber);
        } catch (HttpStatusCodeException e) {
            HttpStatusCode status = e.getStatusCode();
            String responseBody = e.getResponseBodyAsString();
            log.warn("[NOTIF] channel=TELEGRAM order={} status=FAIL error=http_{} body={}",
                orderNumber, status.value(), responseBody);

            if (cfg.source() == TelegramConfig.Source.DB && isTelegramConfigError(status)) {
                TelegramConfig env = resolveEnvConfig();
                if (env.isComplete()
                    && (!env.token().equals(cfg.token()) || !env.chatId().equals(cfg.chatId()))) {
                    logDbOverrideFallback("sendMessage", status, responseBody);
                    try {
                        postSendMessageWithButtons(env, text, orderId, orderNumber);
                        log.info("[NOTIF] channel=TELEGRAM order={} status=SUCCESS (env fallback)", orderNumber);
                        return;
                    } catch (Exception ex) {
                        throw new RuntimeException("Telegram sendMessage failed after ENV fallback: " + ex.getMessage(), ex);
                    }
                }
            }
            throw new RuntimeException("Telegram sendMessage failed: HTTP " + status.value() + " " + responseBody, e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[NOTIF] channel=TELEGRAM order={} status=FAIL error={}", orderNumber, e.toString());
            throw new RuntimeException("Telegram sendMessage failed: " + e.getMessage(), e);
        }
    }

    private void postSendMessageWithButtons(TelegramConfig cfg, String text, Long orderId, String orderNumber) {
        String url = TELEGRAM_API + cfg.token() + "/sendMessage";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", cfg.chatId());
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
    }

    // --- Répondre à un clic de bouton (retire le spinner Telegram) ---

    public void answerCallbackQuery(String callbackQueryId, String text) {
        try {
            String effectiveToken = getEffectiveBotToken();
            String url = TELEGRAM_API + effectiveToken + "/answerCallbackQuery";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("callback_query_id", callbackQueryId);
            body.put("text", text);
            body.put("show_alert", false);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, request, String.class);
        } catch (HttpStatusCodeException e) {
            HttpStatusCode status = e.getStatusCode();
            String responseBody = e.getResponseBodyAsString();
            log.warn("Échec answerCallbackQuery (status {}): {}", status.value(), responseBody);
        } catch (Exception e) {
            log.warn("Échec answerCallbackQuery : {}", e.getMessage());
        }
    }

    // --- Mettre à jour le message après action (remplace les boutons par le résultat) ---

    public void editMessageAfterAction(Long chatIdLong, Integer messageId, String newText) {
        try {
            String effectiveToken = getEffectiveBotToken();
            String url = TELEGRAM_API + effectiveToken + "/editMessageText";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatIdLong);
            body.put("message_id", messageId);
            body.put("text", newText);
            body.put("parse_mode", "HTML");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, request, String.class);
        } catch (HttpStatusCodeException e) {
            HttpStatusCode status = e.getStatusCode();
            String responseBody = e.getResponseBodyAsString();
            log.warn("Échec editMessageText (status {}): {}", status.value(), responseBody);
        } catch (Exception e) {
            log.warn("Échec editMessageText : {}", e.getMessage());
        }
    }

    // --- Envoi d'un message texte simple ---

    public void sendText(String text) {
        String effectiveToken = getEffectiveBotToken();
        if (effectiveToken.isBlank() || getEffectiveChatId().isBlank()) return;
        try {
            String url = TELEGRAM_API + effectiveToken + "/sendMessage";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", getEffectiveChatId());
            body.put("text", text);
            body.put("parse_mode", "HTML");
            restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
        } catch (HttpStatusCodeException e) {
            HttpStatusCode status = e.getStatusCode();
            String responseBody = e.getResponseBodyAsString();
            log.warn("Échec sendText Telegram (status {}): {}", status.value(), responseBody);
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
            String effectiveToken = getEffectiveBotToken();
            String url = TELEGRAM_API + effectiveToken + "/sendMessage";
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
        } catch (HttpStatusCodeException e) {
            HttpStatusCode status = e.getStatusCode();
            String responseBody = e.getResponseBodyAsString();
            log.warn("Échec follow-up WhatsApp Telegram (status {}): {}", status.value(), responseBody);
        } catch (Exception e) {
            log.warn("Échec follow-up WhatsApp Telegram : {}", e.getMessage());
        }
    }

    // --- Liste des commandes en attente ---

    /**
     * Envoie la liste des commandes PENDING avec boutons VALIDER/ANNULER pour chacune.
     */
    public void sendPendingOrdersMenu(List<Order> orders, String currency) {
        String effectiveToken = getEffectiveBotToken();
        if (effectiveToken.isBlank() || getEffectiveChatId().isBlank()) return;
        try {
            if (orders.isEmpty()) {
                sendText("✅ Aucune commande en attente pour le moment.");
                return;
            }

            String url = TELEGRAM_API + effectiveToken + "/sendMessage";
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
        } catch (HttpStatusCodeException e) {
            HttpStatusCode status = e.getStatusCode();
            String responseBody = e.getResponseBodyAsString();
            log.warn("Échec sendPendingOrdersMenu Telegram (status {}): {}", status.value(), responseBody);
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