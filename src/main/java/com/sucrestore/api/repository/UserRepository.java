package com.sucrestore.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sucrestore.api.entity.User;

/**
 * Repository pour l'accès aux données des Utilisateurs (CRUD).
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Trouve un utilisateur par son nom d'utilisateur.
     *
     * @param username Le nom d'utilisateur recherché.
     * @return Un Optional contenant l'utilisateur s'il existe.
     */
    Optional<User> findByUsername(String username);

    /**
     * Vérifie si un nom d'utilisateur existe déjà.
     */
    Boolean existsByUsername(String username);

    /**
     * Vérifie si un email existe déjà.
     */
    Boolean existsByEmail(String email);
}
