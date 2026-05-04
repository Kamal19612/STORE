package com.sucrestore.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Store = tenant (boutique).
 * Identifié par un code stable (ex: "sucre", "spirit") et optionnellement un domaine.
 */
@Entity
@Table(
    name = "stores",
    indexes = {
        @Index(name = "idx_store_code", columnList = "code"),
        @Index(name = "idx_store_domain", columnList = "domain")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Code court stable utilisé par le frontend via header X-Store-Code.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Domaine principal (optionnel) pour résolution automatique par Host header.
     * Ex: "sucre.com", "spirit.com", "sucre.local".
     */
    @Column(unique = true, length = 255)
    private String domain;
}

