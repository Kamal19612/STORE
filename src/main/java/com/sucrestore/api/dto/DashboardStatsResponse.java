package com.sucrestore.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour les statistiques du dashboard admin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    /**
     * Nombre total de commandes
     */
    private Long totalOrders;

    /**
     * Nombre total de produits
     */
    private Long totalProducts;

    /**
     * Revenu total (somme de toutes les commandes)
     */
    private BigDecimal totalRevenue;

    /**
     * Nombre de commandes en attente
     */
    private Long pendingOrders;

    /**
     * Nombre de commandes confirmées
     */
    private Long confirmedOrders;
}
