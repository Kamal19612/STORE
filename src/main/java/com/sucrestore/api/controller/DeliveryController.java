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
import com.sucrestore.api.service.DeliveryService;

/**
 * Contrôleur REST pour les livreurs. Accès réservé au rôle DELIVERY_AGENT.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/delivery/orders")
@PreAuthorize("hasRole('DELIVERY_AGENT') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;

    /**
     * GET /api/delivery/orders : Liste des commandes à livrer.
     */
    @GetMapping
    public ResponseEntity<Page<Order>> getOrdersForDelivery(Pageable pageable) {
        return ResponseEntity.ok(deliveryService.getOrdersForDelivery(pageable));
    }

    /**
     * PUT /api/delivery/orders/{id}/status : Mettre à jour le statut (ex:
     * DELIVERED).
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateDeliveryStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        return ResponseEntity.ok(deliveryService.updateDeliveryStatus(id, status));
    }
}
