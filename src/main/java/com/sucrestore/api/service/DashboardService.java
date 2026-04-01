package com.sucrestore.api.service;

import com.sucrestore.api.dto.DashboardStatsResponse;
import com.sucrestore.api.entity.Order;
import com.sucrestore.api.repository.OrderRepository;
import java.math.BigDecimal;
import com.sucrestore.api.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Service pour les statistiques du dashboard
 */
@Service
public class DashboardService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    /**
     * Calcule toutes les statistiques pour le dashboard admin
     */
    /**
     * Réinitialise les statistiques (Supprime toutes les commandes)
     */
    public void resetStatistics() {
        orderRepository.deleteAll();
    }

    public DashboardStatsResponse getStatistics() {
        // Nombre total de commandes
        Long totalOrders = orderRepository.count();

        // Nombre total de produits
        Long totalProducts = productRepository.count();

        // Revenu total — une seule requête SQL SUM(), aucun chargement d'objets
        BigDecimal totalRevenue = orderRepository.sumTotalRevenue();

        // Nombre de commandes en attente
        Long pendingOrders = orderRepository.countByStatus(Order.Status.PENDING);

        // Nombre de commandes confirmées
        Long confirmedOrders = orderRepository.countByStatus(Order.Status.CONFIRMED);

        return DashboardStatsResponse.builder()
                .totalOrders(totalOrders)
                .totalProducts(totalProducts)
                .totalRevenue(totalRevenue)
                .pendingOrders(pendingOrders)
                .confirmedOrders(confirmedOrders)
                .build();
    }
}
