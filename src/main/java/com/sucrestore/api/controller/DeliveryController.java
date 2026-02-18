package com.sucrestore.api.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sucrestore.api.entity.Order;

/**
 * Contrôleur REST pour les livreurs. Accès réservé au rôle DELIVERY_AGENT.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/delivery/orders")
@PreAuthorize("hasRole('DELIVERY_AGENT') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class DeliveryController {

    @Autowired
    private com.sucrestore.api.service.OrderService orderService;

    /**
     * GET /api/delivery/orders : Liste des commandes DISPONIBLES (CONFIRMED et
     * sans livreur).
     */
    @GetMapping
    public ResponseEntity<Page<Order>> getOrdersForDelivery(Pageable pageable) {
        return ResponseEntity.ok(orderService.getAvailableDeliveryOrders(pageable));
    }

    /**
     * GET /api/delivery/orders/my-orders : Mes commandes prises en charge.
     */
    @GetMapping("/my-orders")
    public ResponseEntity<Page<Order>> getMyOrders(
            Pageable pageable,
            org.springframework.security.core.Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(orderService.getMyDeliveryOrders(username, pageable));
    }

    /**
     * PUT /api/delivery/orders/{id}/claim : Prendre en charge une commande.
     */
    @PutMapping("/{id}/claim")
    public ResponseEntity<Order> claimOrder(
            @PathVariable Long id,
            org.springframework.security.core.Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(orderService.claimOrder(id, username));
    }

    /**
     * POST /api/delivery/orders/{id}/complete : Valider la livraison avec code.
     * Body: { "code": "CONF-1234" }
     */
    @org.springframework.web.bind.annotation.PostMapping("/{id}/complete")
    public ResponseEntity<Order> completeDelivery(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            org.springframework.security.core.Authentication authentication) {
        String username = authentication.getName();
        String code = payload.get("code");
        return ResponseEntity.ok(orderService.completeDelivery(id, username, code));
    }
}
