package com.sucrestore.api.webhook;

import com.sucrestore.api.entity.Order;
import com.sucrestore.api.entity.OrderItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO représentant une commande dans le payload webhook.
 *
 * Contient exactement les mêmes champs que ceux affichés sur la page livreur
 * (DeliveryDashboard.jsx), garantissant la cohérence web ↔ mobile.
 */
@Data
@Builder
public class WebhookOrderPayload {

    private Long id;
    private String orderNumber;
    private String confirmationCode;

    // Infos client
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private String customerNotes;
    private BigDecimal customerLatitude;
    private BigDecimal customerLongitude;
    private String manualLocationLink;

    // Livraison
    private String deliveryType;   // STANDARD | EXPRESS | PROGRAMMER
    private String scheduledTime;
    private BigDecimal deliveryCost;
    private BigDecimal distance;

    // Montants
    private BigDecimal subtotal;
    private BigDecimal total;

    // Statut
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Articles (résumé)
    private List<WebhookItemPayload> items;

    // ─────────────────────────────────────────────────────────────────────────
    // DTO imbriqué : article de commande
    // ─────────────────────────────────────────────────────────────────────────

    @Data
    @Builder
    public static class WebhookItemPayload {
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Factory : construit le DTO depuis l'entité Order
    // ─────────────────────────────────────────────────────────────────────────

    public static WebhookOrderPayload from(Order order) {
        List<WebhookItemPayload> items = order.getItems().stream()
                .map(item -> WebhookItemPayload.builder()
                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                        .productName(item.getProduct() != null ? item.getProduct().getName() : "")
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .toList();

        return WebhookOrderPayload.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .confirmationCode(order.getConfirmationCode())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .customerAddress(order.getCustomerAddress())
                .customerNotes(order.getCustomerNotes())
                .customerLatitude(order.getCustomerLatitude())
                .customerLongitude(order.getCustomerLongitude())
                .manualLocationLink(order.getManualLocationLink())
                .deliveryType(order.getDeliveryType())
                .scheduledTime(order.getScheduledTime())
                .deliveryCost(order.getDeliveryCost())
                .distance(order.getDistance())
                .subtotal(order.getSubtotal())
                .total(order.getTotal())
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(items)
                .build();
    }
}
