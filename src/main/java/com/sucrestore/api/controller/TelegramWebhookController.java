package com.sucrestore.api.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sucrestore.api.config.AppProperties;
import com.sucrestore.api.entity.Order;
import com.sucrestore.api.tenant.StoreContext;
import com.sucrestore.api.tenant.StoreResolverService;
import com.sucrestore.api.repository.OrderRepository;
import com.sucrestore.api.repository.StoreRepository;
import com.sucrestore.api.service.OrderService;
import com.sucrestore.api.service.TelegramService;

@RestController
@RequestMapping("/api/telegram")
public class TelegramWebhookController {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookController.class);

    @Autowired
    private TelegramService telegramService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private StoreResolverService storeResolverService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private StoreRepository storeRepository;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody Map<String, Object> update,
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretToken) {

        return handleWebhookInternal(update, secretToken);
    }

    /**
     * Store-scoped webhook endpoint (recommended): /api/telegram/{storeCode}/webhook
     * This ensures tenant resolution even when domain mapping is shared or ambiguous.
     */
    @PostMapping("/{storeCode}/webhook")
    public ResponseEntity<Void> handleWebhookForStore(
            @PathVariable String storeCode,
            @RequestBody Map<String, Object> update,
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretToken) {
        try {
            var store = storeResolverService.resolveByCodeOrDomainOrDefault(storeCode, null);
            StoreContext.set(store);
            return handleWebhookInternal(update, secretToken);
        } finally {
            StoreContext.clear();
        }
    }

    private ResponseEntity<Void> handleWebhookInternal(
            Map<String, Object> update,
            String secretToken
    ) {
        // Vérification du secret si configuré
        String configuredSecret = telegramService.getWebhookSecret();
        if (!configuredSecret.isBlank() && !configuredSecret.equals(secretToken)) {
            log.warn("Webhook Telegram : secret invalide");
            return ResponseEntity.ok().build();
        }

        // ── 1. Callback bouton inline (VALIDER / ANNULER) ──────────────────────────
        @SuppressWarnings("unchecked")
        Map<String, Object> callbackQuery = (Map<String, Object>) update.get("callback_query");
        if (callbackQuery != null) {
            handleCallbackQueryAsync(callbackQuery);
            return ResponseEntity.ok().build();
        }

        // ── 2. Commandes texte (/commandes, /aide) ─────────────────────────────────
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) update.get("message");
        if (message != null) {
            String text = (String) message.getOrDefault("text", "");
            handleTextCommandAsync(text.trim());
        }

        return ResponseEntity.ok().build();
    }

    @Async("notificationExecutor")
    void handleCallbackQueryAsync(Map<String, Object> callbackQuery) {
        try {
            handleCallbackQuery(callbackQuery);
        } catch (Exception e) {
            log.error("Erreur async callback Telegram: {}", e.toString());
        }
    }

    @Async("notificationExecutor")
    void handleTextCommandAsync(String text) {
        try {
            handleTextCommand(text);
        } catch (Exception e) {
            log.error("Erreur async commande Telegram: {}", e.toString());
        }
    }

    // ── Gestion boutons inline ─────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void handleCallbackQuery(Map<String, Object> callbackQuery) {
        String callbackQueryId = (String) callbackQuery.get("id");
        String data = (String) callbackQuery.get("data");

        Map<String, Object> message = (Map<String, Object>) callbackQuery.get("message");
        Map<String, Object> chat = (Map<String, Object>) message.get("chat");
        Integer messageId = (Integer) message.get("message_id");
        Long chatIdLong = ((Number) chat.get("id")).longValue();

        if (data == null || (!data.startsWith("confirm_") && !data.startsWith("cancel_"))) {
            return;
        }

        boolean isConfirm = data.startsWith("confirm_");
        Long orderId = Long.parseLong(data.split("_")[1]);
        String emoji = isConfirm ? "✅" : "❌";
        String label = isConfirm ? "VALIDÉE" : "REJETÉE";

        // IMPORTANT: Telegram webhook is unauthenticated; tenant may be missing/ambiguous on legacy endpoint.
        // Resolve tenant from order.store to ensure correct scoping (order lookup + telegram settings in DB).
        // We keep store-scoped webhook URL, but this makes legacy endpoint resilient too.
        var previousStore = StoreContext.get();
        try {
            Order orderAny = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable ID: " + orderId));
            if (orderAny.getStore() != null && orderAny.getStore().getId() != null) {
                storeRepository.findById(orderAny.getStore().getId())
                    .ifPresent(StoreContext::set);
            }

            // Gestion commande via règles métier (évite les transitions invalides)
            Order order = isConfirm ? orderService.acceptOrder(orderId) : orderService.rejectOrder(orderId);

            // Répondre au clic (retire le spinner)
            telegramService.answerCallbackQuery(callbackQueryId, emoji + " Commande " + label);

            // Mettre à jour le message en retirant les boutons
            String originalText = (String) message.get("text");
            String updatedText = originalText + "\n\n" + emoji + " <b>Commande " + label + "</b> par l'admin";
            telegramService.editMessageAfterAction(chatIdLong, messageId, updatedText);

            // Envoyer le follow-up avec bouton WhatsApp pour notifier le client
            String whatsappLink = orderService.generateWhatsAppNotificationLink(orderId, null);
            telegramService.sendFollowUpWithWhatsApp(
                chatIdLong,
                order.getOrderNumber(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                whatsappLink,
                isConfirm
            );

            log.info("Commande {} {} via Telegram", orderId, label);
        } catch (Exception e) {
            String msg = (e.getMessage() == null || e.getMessage().isBlank())
                ? (e.getClass().getSimpleName())
                : e.getMessage();
            log.error("Erreur traitement callback Telegram pour commande {} : {}", orderId, msg, e);
            telegramService.answerCallbackQuery(callbackQueryId, "⚠️ Erreur : " + msg);
        } finally {
            // Restore previous tenant context (or clear)
            if (previousStore != null) {
                StoreContext.set(previousStore);
            } else {
                StoreContext.clear();
            }
        }
    }

    // ── Gestion commandes texte ────────────────────────────────────────────────

    private void handleTextCommand(String text) {
        String lower = text.toLowerCase();

        if (lower.startsWith("/commandes") || lower.equals("commandes")) {
            List<Order> pending = orderService.getLatestPendingOrders();
            String currency = appProperties.getCurrency() != null ? appProperties.getCurrency() : "FCFA";
            telegramService.sendPendingOrdersMenu(pending, currency);

        } else if (lower.startsWith("/aide") || lower.equals("aide")) {
            telegramService.sendText(
                "🤖 <b>Commandes disponibles</b>\n\n"
                + "/commandes — Voir les commandes en attente\n"
                + "/aide — Afficher cette aide\n\n"
                + "Les nouvelles commandes arrivent automatiquement avec les boutons "
                + "✅ <b>VALIDER</b> / ❌ <b>ANNULER</b>\n\n"
                + "Après chaque action, un bouton 💬 <b>WhatsApp</b> vous permet de notifier "
                + "le client directement sans vous connecter au panneau admin."
            );
        }
    }
}
