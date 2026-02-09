package com.sucrestore.api.repository;

import java.util.Optional;

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
}
