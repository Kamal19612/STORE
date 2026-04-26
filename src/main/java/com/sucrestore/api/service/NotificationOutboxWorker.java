package com.sucrestore.api.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sucrestore.api.config.AppProperties;
import com.sucrestore.api.entity.NotificationOutbox;
import com.sucrestore.api.entity.Order;
import com.sucrestore.api.repository.NotificationOutboxRepository;
import com.sucrestore.api.repository.OrderRepository;

@Service
public class NotificationOutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxWorker.class);

    private final NotificationOutboxRepository outboxRepo;
    private final NotificationOutboxService outboxService;
    private final OrderRepository orderRepo;
    private final TelegramService telegramService;
    private final FcmService fcmService;
    private final WebPushService webPushService;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public NotificationOutboxWorker(
        NotificationOutboxRepository outboxRepo,
        NotificationOutboxService outboxService,
        OrderRepository orderRepo,
        TelegramService telegramService,
        FcmService fcmService,
        WebPushService webPushService,
        AppProperties appProperties,
        ObjectMapper objectMapper
    ) {
        this.outboxRepo = outboxRepo;
        this.outboxService = outboxService;
        this.orderRepo = orderRepo;
        this.telegramService = telegramService;
        this.fcmService = fcmService;
        this.webPushService = webPushService;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    public void tick() {
        // Batch: 50 items max par tick
        var due = outboxRepo.findDue(LocalDateTime.now(), PageRequest.of(0, 50));
        for (NotificationOutbox row : due) {
            processOne(row);
        }
    }

    @Transactional
    void processOne(NotificationOutbox row) {
        try {
            outboxService.markInProgress(row);

            Order order = orderRepo.findById(row.getOrderId()).orElse(null);
            if (order == null) {
                outboxService.markFailedAndScheduleRetry(row.getId(), "order_not_found", row.getAttempts() + 1);
                return;
            }

            Map<String, Object> payload = Map.of();
            try {
                if (row.getPayloadJson() != null && !row.getPayloadJson().isBlank()) {
                    payload = objectMapper.readValue(row.getPayloadJson(), Map.class);
                }
            } catch (Exception ignored) {}

            dispatch(row, order, payload);
            outboxService.markSent(row.getId());
            log.info("[NOTIF] channel={} order={} status=SUCCESS (outbox)", row.getChannel(), row.getOrderNumber());
        } catch (Exception e) {
            outboxService.markFailedAndScheduleRetry(row.getId(), e.toString(), row.getAttempts() + 1);
        }
    }

    private void dispatch(NotificationOutbox row, Order order, Map<String, Object> payload) {
        String currency = appProperties.getCurrency() != null ? appProperties.getCurrency() : "FCFA";

        switch (row.getChannel()) {
            case TELEGRAM -> {
                if (row.getEventType() == NotificationOutbox.EventType.NEW_ORDER_ADMIN) {
                    telegramService.sendNewOrderNotification(order, currency);
                    return;
                }
                // autres types Telegram possibles plus tard
                throw new IllegalArgumentException("unsupported_telegram_event=" + row.getEventType());
            }
            case FCM -> {
                if (fcmService == null) throw new IllegalStateException("fcm_service_missing");
                switch (row.getEventType()) {
                    case NEW_ORDER_ADMIN -> fcmService.notifyAdminsNewOrder(order);
                    case ORDER_STATUS_ADMIN -> {
                        String actor = payload.getOrDefault("actorUsername", "").toString();
                        fcmService.notifyAdminsOrderStatus(order, actor);
                    }
                    case NEW_DELIVERY -> fcmService.notifyDeliveryAgentsNewDelivery(order);
                    default -> throw new IllegalArgumentException("unsupported_fcm_event=" + row.getEventType());
                }
                return;
            }
            case WEBPUSH -> {
                switch (row.getEventType()) {
                    case NEW_ORDER_ADMIN -> webPushService.notifyAdmins(
                        "🛒 Nouvelle commande",
                        "#" + order.getOrderNumber() + " — " + order.getCustomerName(),
                        "order-" + order.getOrderNumber()
                    );
                    case NEW_DELIVERY -> webPushService.notifyDeliveryAgents(
                        "🚚 Nouvelle livraison disponible",
                        "Commande #" + order.getOrderNumber(),
                        "delivery-" + order.getOrderNumber()
                    );
                    case CUSTOMER_STATUS -> {
                        String title = payload.getOrDefault("title", "").toString();
                        String body = payload.getOrDefault("body", "").toString();
                        if (title.isBlank()) title = "Mise à jour";
                        if (body.isBlank()) body = "Commande #" + order.getOrderNumber();
                        webPushService.notifyCustomer(order.getOrderNumber(), title, body);
                    }
                    default -> throw new IllegalArgumentException("unsupported_webpush_event=" + row.getEventType());
                }
            }
        }
    }
}

