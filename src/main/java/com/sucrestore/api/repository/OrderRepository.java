package com.sucrestore.api.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sucrestore.api.entity.Order;

/**
 * Repository pour l'accès aux données des Commandes.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Trouve une commande par son numéro unique public.
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Trouve les commandes ayant l'un des statuts donnés.
     */
    Page<Order> findByStatusIn(List<Order.Status> statuses, Pageable pageable);

    /**
     * Compte le nombre de commandes par statut
     */
    Long countByStatus(Order.Status status);

    /**
     * Chiffre d'affaires réel : uniquement commandes CONFIRMED, SHIPPED, DELIVERED non supprimées.
     */
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.status IN ('CONFIRMED', 'SHIPPED', 'DELIVERED') AND o.deleted = false")
    BigDecimal sumValidRevenue();

    /**
     * Compte les commandes non supprimées par statut.
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.deleted = false")
    Long countByStatusAndNotDeleted(@org.springframework.data.repository.query.Param("status") Order.Status status);

    /**
     * Trouve les commandes disponibles pour livraison (CONFIRMED et pas de
     * livreur).
     */
    Page<Order> findByStatusAndDeliveryAgentNull(Order.Status status, Pageable pageable);

    /**
     * Trouve les commandes par statut (utilisé par le bot Telegram).
     */
    List<Order> findTop10ByStatusOrderByIdDesc(Order.Status status);

    /**
     * Trouve les commandes modifiées après une certaine date.
     */
    List<Order> findByUpdatedAtAfter(java.time.LocalDateTime lastSync);

    /**
     * Trouve les commandes assignées à un livreur spécifique avec certains
     * statuts.
     */
    Page<Order> findByDeliveryAgentUsernameAndStatusIn(String username, List<Order.Status> statuses, Pageable pageable);

    /**
     * Trouve toutes les commandes non supprimées (soft delete).
     */
    Page<Order> findByDeletedFalse(Pageable pageable);

    /**
     * Statistiques journalières des 7 derniers jours : [date, nbCommandes, revenu].
     * Retourne Object[] : [0]=LocalDate, [1]=Long count, [2]=BigDecimal sum
     */
    @Query(value = """
        SELECT CAST(o.created_at AS DATE), COUNT(o.id), COALESCE(SUM(o.total), 0)
        FROM orders o
        WHERE o.deleted = false AND o.created_at >= CURRENT_DATE - 6
        GROUP BY CAST(o.created_at AS DATE)
        ORDER BY CAST(o.created_at AS DATE)
        """, nativeQuery = true)
    List<Object[]> findDailyStatsLast7Days();

    /**
     * 5 commandes les plus récentes non supprimées.
     */
    List<Order> findTop5ByDeletedFalseOrderByCreatedAtDesc();
}
