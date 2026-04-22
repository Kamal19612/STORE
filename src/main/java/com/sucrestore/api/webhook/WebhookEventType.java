package com.sucrestore.api.webhook;

/**
 * Types d'événements déclenchant un webhook vers l'application mobile.
 *
 * Chaque valeur correspond à un changement d'état majeur d'une commande
 * qui intéresse le livreur (nouvelle course disponible, annulation, etc.).
 */
public enum WebhookEventType {

    /**
     * Une commande vient d'être confirmée par l'admin.
     * → Elle devient disponible pour les livreurs.
     */
    ORDER_CONFIRMED,

    /**
     * Un livreur a accepté la commande (claim).
     * → Elle disparaît de la liste "disponibles".
     */
    ORDER_CLAIMED,

    /**
     * La commande est en route (statut SHIPPED).
     */
    ORDER_IN_DELIVERY,

    /**
     * La livraison a été validée avec le code client.
     */
    ORDER_DELIVERED,

    /**
     * La commande a été annulée (admin ou livreur).
     */
    ORDER_CANCELLED,

    /**
     * La commande a été rejetée par l'admin (refus explicite).
     */
    ORDER_REJECTED,

    /**
     * Payload de test envoyé depuis le panneau admin.
     * Permet de vérifier que la connexion webhook fonctionne.
     */
    WEBHOOK_TEST
}
