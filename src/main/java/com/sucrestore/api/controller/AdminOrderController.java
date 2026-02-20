package com.sucrestore.api.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sucrestore.api.entity.Order;
import com.sucrestore.api.service.OrderService;

/**
 * Contrôleur REST pour l'administration des commandes. Nécessite le rôle ADMIN
 * ou SUPER_ADMIN.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
public class AdminOrderController {

    @Autowired
    private OrderService orderService;

    /**
     * GET /api/admin/orders : Liste de toutes les commandes.
     */
    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<Order>> getAllOrders(
            @org.springframework.data.web.PageableDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }

    /**
     * GET /api/admin/orders/{id} : Détail d'une commande.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    /**
     * PUT /api/admin/orders/{id}/status : Changer le statut. Body: { "status":
     * "CONFIRMED" }
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    /**
     * GET /api/admin/orders/{id}/history : Récupère l'historique des
     * changements de statut
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<java.util.List<com.sucrestore.api.entity.OrderStatusHistory>> getOrderHistory(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderHistory(id));
    }

    /**
     * GET /api/admin/orders/{id}/whatsapp-notification : Génère le lien
     * WhatsApp pour la notification de statut.
     */
    @GetMapping("/{id}/whatsapp-notification")
    public ResponseEntity<Map<String, String>> getWhatsAppNotificationLink(
            @PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String phoneNumber) {
        String link = orderService.generateWhatsAppNotificationLink(id, phoneNumber);
        return ResponseEntity.ok(Map.of("link", link));
    }

    /**
     * DELETE /api/admin/orders/{id} : Supprimer une commande. Réservé au
     * SUPER_ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok().build();
    }
}
