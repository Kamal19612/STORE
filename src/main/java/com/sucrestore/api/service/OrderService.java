package com.sucrestore.api.service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sucrestore.api.config.AppProperties;
import com.sucrestore.api.dto.OrderItemRequest;
import com.sucrestore.api.dto.OrderRequest;
import com.sucrestore.api.dto.OrderResponse;
import com.sucrestore.api.entity.Order;
import com.sucrestore.api.entity.OrderItem;
import com.sucrestore.api.entity.Product;
import com.sucrestore.api.repository.OrderRepository;
import com.sucrestore.api.repository.ProductRepository;

/**
 * Service gérant la logique métier pour les commandes (Checkout).
 */
@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AppProperties appProperties;

    /**
     * Traite une nouvelle commande invité. 1. Vérifie le stock. 2. Crée la
     * commande et les lignes. 3. Décrémente le stock. 4. Génère le lien
     * WhatsApp.
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {

        // 1. Générer un numéro de commande unique (ex: ORD-171569854)
        String orderNumber = "ORD-" + System.currentTimeMillis();

        // 2. Initialiser la commande
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerAddress(request.getCustomerAddress())
                .customerNotes(request.getCustomerNotes())
                .customerLatitude(request.getCustomerLatitude())
                .customerLongitude(request.getCustomerLongitude())
                .status(Order.Status.PENDING)
                .items(new ArrayList<>())
                .subtotal(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;

        // 3. Traiter chaque article
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé ID: " + itemRequest.getProductId()));

            // Vérification stock
            if (product.getStock() < itemRequest.getQuantity()) {
                throw new RuntimeException("Stock insuffisant pour le produit : " + product.getName());
            }

            // Décrémentation stock
            product.setStock(product.getStock() - itemRequest.getQuantity());
            productRepository.save(product);

            // Création ligne de commande
            BigDecimal lineTotal = product.getPrice().multiply(new BigDecimal(itemRequest.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .totalPrice(lineTotal)
                    .build();

            order.getItems().add(orderItem);
            subtotal = subtotal.add(lineTotal);
        }

        // 4. Calculs finaux et sauvegarde
        order.setSubtotal(subtotal);
        // order.setTax(...) // Si besoin
        order.setTotal(subtotal); // Pour l'instant Total = Subtotal

        Order savedOrder = orderRepository.save(order);

        // 5. Générer le lien WhatsApp
        String whatsappLink = generateWhatsAppLink(savedOrder);

        return OrderResponse.builder()
                .orderNumber(savedOrder.getOrderNumber())
                .totalAmount(savedOrder.getTotal())
                .status(savedOrder.getStatus().name())
                .whatsappLink(whatsappLink)
                .build();
    }

    /**
     * Génère un lien WhatsApp pré-rempli avec le résumé de la commande.
     */
    private String generateWhatsAppLink(Order order) {
        StringBuilder message = new StringBuilder();
        message.append("*Nouvelle Commande : ").append(order.getOrderNumber()).append("*\n");
        message.append("Date : ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n\n");

        message.append("*Client :*\n");
        message.append("Nom : ").append(order.getCustomerName()).append("\n");
        message.append("Tél : ").append(order.getCustomerPhone()).append("\n");
        message.append("Adresse : ").append(order.getCustomerAddress()).append("\n\n");

        message.append("*Articles :*\n");
        for (OrderItem item : order.getItems()) {
            message.append("- ").append(item.getProduct().getName())
                    .append(" x").append(item.getQuantity())
                    .append(" (").append(item.getTotalPrice()).append(" FCFA)\n");
        }

        message.append("\n*Total : ").append(order.getTotal()).append(" FCFA*");

        try {
            return "https://wa.me/" + appProperties.getWhatsappNumber() + "?text=" + URLEncoder.encode(message.toString(), StandardCharsets.UTF_8.toString());
        } catch (java.io.UnsupportedEncodingException e) {
            // Ne devrait jamais arriver avec UTF-8 standard, mais on doit le gérer
            return "";
        }
    }

    // --- Méthodes Admin ---
    /**
     * Récupère toutes les commandes (Admin). Ajouter pagination si beaucoup de
     * commandes.
     */
    /**
     * Récupère toutes les commandes (Admin) avec pagination.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Order> getAllOrders(org.springframework.data.domain.Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    /**
     * Récupère une commande par son ID (Admin).
     */
    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande introuvable ID: " + id));
    }

    /**
     * Met à jour le statut d'une commande (Admin).
     */
    @Transactional
    public Order updateOrderStatus(Long id, String statusName) {
        Order order = getOrderById(id);

        try {
            Order.Status stringStatus = Order.Status.valueOf(statusName);
            order.setStatus(stringStatus);
            return orderRepository.save(order);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide : " + statusName);
        }
    }
}
