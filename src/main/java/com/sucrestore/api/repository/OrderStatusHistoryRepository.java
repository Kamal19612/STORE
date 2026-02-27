package com.sucrestore.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sucrestore.api.entity.OrderStatusHistory;

/**
 * Repository pour gérer l'historique des changements de statut des commandes
 */
@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    /**
     * Récupère l'historique complet d'une commande, trié du plus récent au plus
     * ancien
     */
    List<OrderStatusHistory> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
