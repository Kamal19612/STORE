package com.sucrestore.api.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.sucrestore.api.service.OrderService;

/**
 * Contrôleur REST pour l'administration des commandes. Nécessite le rôle ADMIN
 * ou SUPER_ADMIN.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminOrderController {

    @Autowired
    private OrderService orderService;

    /**
     * GET /api/admin/orders : Liste de toutes les commandes.
     */
    /**
     * GET /api/admin/orders : Liste de toutes les commandes (paginée).
     */
    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<Order>> getAllOrders(org.springframework.data.domain.Pageable pageable) {
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
}
