package com.sucrestore.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * Trouve les commandes disponibles pour livraison (CONFIRMED et pas de
     * livreur).
     */
    Page<Order> findByStatusAndDeliveryAgentNull(Order.Status status, Pageable pageable);

    /**
     * Trouve les commandes modifiées après une certaine date.
     */
    List<Order> findByUpdatedAtAfter(java.time.LocalDateTime lastSync);

    /**
     * Trouve les commandes assignées à un livreur spécifique avec certains
     * statuts.
     */
    Page<Order> findByDeliveryAgentUsernameAndStatusIn(String username, List<Order.Status> statuses, Pageable pageable);
}
