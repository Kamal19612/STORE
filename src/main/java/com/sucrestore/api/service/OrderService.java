package com.sucrestore.api.service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sucrestore.api.config.AppProperties;
import com.sucrestore.api.dto.OrderItemRequest;
import com.sucrestore.api.dto.OrderRequest;
import com.sucrestore.api.dto.OrderResponse;
import com.sucrestore.api.entity.Order;
import com.sucrestore.api.entity.OrderItem;
import com.sucrestore.api.entity.OrderStatusHistory;
import com.sucrestore.api.entity.Product;
import com.sucrestore.api.repository.OrderRepository;
import com.sucrestore.api.repository.OrderStatusHistoryRepository;
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
    private OrderStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private AppSettingService appSettingService;

    @Autowired
    private com.sucrestore.api.repository.UserRepository userRepository;

    @Autowired
    @Lazy
    private NotificationService notificationService;

    @Autowired
    @Lazy
    private WebPushService webPushService;

    @Autowired
    @Lazy
    private TelegramService telegramService;

    /**
     * Traite une nouvelle commande invité. 1. Vérifie le stock. 2. Crée la
     * commande et les lignes. 3. Décrémente le stock. 4. Génère le lien
     * WhatsApp.
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {

        // 1. Générer un numéro de commande unique (ex: ORD-171569854)
        String orderNumber = "ORD-" + System.currentTimeMillis();

        // 2. Générer un code de confirmation unique (ex: CONF-1234)
        String confirmationCode = generateConfirmationCode();

        // Extraction des coordonnées si le lien est présent mais pas les coordonnées
        BigDecimal lat = request.getCustomerLatitude();
        BigDecimal lng = request.getCustomerLongitude();
        if ((lat == null || lng == null) && request.getManualLocationLink() != null) {
            BigDecimal[] coords = extractCoordinatesFromLink(request.getManualLocationLink());
            if (coords != null) {
                lat = coords[0];
                lng = coords[1];
            }
        }

        // 3. Initialiser la commande
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .confirmationCode(confirmationCode)
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerAddress(request.getCustomerAddress())
                .customerNotes(request.getCustomerNotes())
                .customerLatitude(lat)
                .customerLongitude(lng)
                .deliveryType(request.getDeliveryType())
                .scheduledTime(request.getScheduledTime())
                .manualLocationLink(request.getManualLocationLink())
                .deliveryCost(request.getDeliveryCost())
                .distance(request.getDistance())
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
        // Utiliser le total envoyé par le front ou recalculer
        if (request.getTotalAmount() != null) {
            order.setTotal(request.getTotalAmount());
        } else {
            BigDecimal delivery = request.getDeliveryCost() != null ? request.getDeliveryCost() : BigDecimal.ZERO;
            order.setTotal(subtotal.add(delivery));
        }

        Order savedOrder = orderRepository.save(order);

        // 5. Générer le lien WhatsApp
        String whatsappLink = generateWhatsAppLink(savedOrder);

        // 6. Notifications : admin (SSE + Telegram) lors d'une nouvelle commande
        String currency = appProperties.getCurrency() != null ? appProperties.getCurrency() : "FCFA";
        try {
            Map<String, Object> notifData = Map.of(
                    "id", savedOrder.getId(),
                    "orderNumber", savedOrder.getOrderNumber(),
                    "customerName", savedOrder.getCustomerName(),
                    "total", savedOrder.getTotal(),
                    "deliveryType", savedOrder.getDeliveryType() != null ? savedOrder.getDeliveryType() : ""
            );
            notificationService.notifyAdmins("new_order", notifData);
            webPushService.notifyAdmins(
                "🛒 Nouvelle commande",
                "#" + savedOrder.getOrderNumber() + " — " + savedOrder.getCustomerName(),
                "order-" + savedOrder.getOrderNumber()
            );
            telegramService.sendNewOrderNotification(savedOrder, currency);
        } catch (Exception e) {
            // Les notifications ne doivent jamais bloquer la commande
        }

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

        // Dynamic Settings with Fallback
        String storeName = appSettingService.getSettingValue("store_name")
                .orElse(appProperties.getStoreName() != null ? appProperties.getStoreName() : "STORE");
        String currency = appProperties.getCurrency() != null ? appProperties.getCurrency() : "FCFA";
        String whatsappNumber = appSettingService.getSettingValue("whatsapp_number")
                .orElse(appProperties.getWhatsappNumber());

        message.append("*NOUVELLE COMMANDE ").append(storeName).append("*").append("\n\n");
        message.append("Commande: #").append(order.getOrderNumber()).append("\n");
        message.append("Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n\n");

        message.append("*CLIENT*").append("\n");
        message.append("Nom: ").append(order.getCustomerName()).append("\n");
        message.append("Tel: ").append(order.getCustomerPhone()).append("\n");
        message.append("Adresse: ").append(order.getCustomerAddress()).append("\n");

        if (order.getCustomerLatitude() != null && order.getCustomerLongitude() != null) {
            message.append("📍 Position: https://www.google.com/maps?q=").append(order.getCustomerLatitude()).append(",").append(order.getCustomerLongitude()).append("\n");
        } else if (order.getManualLocationLink() != null && !order.getManualLocationLink().isEmpty()) {
            message.append("📍 Position: ").append(order.getManualLocationLink()).append("\n");
        }
        
        if (order.getDeliveryType() != null) {
            message.append("🚚 Type: ").append(order.getDeliveryType());
            if ("PROGRAMMER".equals(order.getDeliveryType()) && order.getScheduledTime() != null) {
                message.append(" (").append(order.getScheduledTime()).append(")");
            }
            message.append("\n");
        }
        
        message.append("\n");

        message.append("*ARTICLES*").append("\n");
        for (OrderItem item : order.getItems()) {
            message.append("- ").append(item.getQuantity()).append("x ")
                    .append(item.getProduct().getName())
                    .append(" (").append(item.getTotalPrice()).append(" ").append(currency).append(")\n");
        }

        message.append("\n*TOTAL: ").append(order.getTotal()).append(" ").append(currency).append("*").append("\n\n");

        if (order.getCustomerNotes() != null && !order.getCustomerNotes().isEmpty()) {
            message.append("Notes: ").append(order.getCustomerNotes());
        }

        try {
            return "https://wa.me/" + whatsappNumber + "?text=" + URLEncoder.encode(message.toString(), StandardCharsets.UTF_8.toString());
        } catch (java.io.UnsupportedEncodingException e) {
            return "";
        }
    }

    /**
     * Génère un lien de notification WhatsApp pour le changement de statut.
     */
    @Transactional(readOnly = true)
    public String generateStatusNotificationLink(Long orderId) {
        Order order = getOrderById(orderId);

        // Dynamic Settings with Fallback
        String storeName = appSettingService.getSettingValue("store_name")
                .orElse(appProperties.getStoreName() != null ? appProperties.getStoreName() : "SUCRE STORE");
        String storePhone = appSettingService.getSettingValue("contact_phone")
                .orElse(appProperties.getStorePhone() != null ? appProperties.getStorePhone() : "");

        StringBuilder message = new StringBuilder();
        message.append("Bonjour,\n\n");
        message.append("Votre commande #").append(order.getOrderNumber()).append(" sur ").append(storeName).append(" est actuellement : *").append(getStatusLabel(order.getStatus())).append("*\n\n");

        switch (order.getStatus()) {
            case PENDING:
            case SHIPPED: // Using SHIPPED as "En cours" equivalent for now
                message.append("Votre commande est en cours de préparation et sera bientôt livrée.");
                break;
            case DELIVERED:
                message.append("Votre commande a été livrée avec succès. Merci de votre confiance !");
                break;
            case CANCELLED:
                message.append("Votre commande a été annulée. Pour plus d'informations, contactez-nous.");
                break;
            case CONFIRMED:
            default:
                message.append("Nous vous tiendrons informé de l'évolution de votre commande.");
                break;
        }

        message.append("\n\n").append(storeName).append("\n").append(storePhone);

        // Nettoyage du numéro de téléphone du client (suppression des espaces, etc.)
        String customerPhone = order.getCustomerPhone().replaceAll("\\s+", "").replaceAll("[^0-9]", "");

        try {
            return "https://wa.me/" + customerPhone + "?text=" + URLEncoder.encode(message.toString(), StandardCharsets.UTF_8.toString());
        } catch (java.io.UnsupportedEncodingException e) {
            return "";
        }
    }

    private String getStatusLabel(Order.Status status) {
        return switch (status) {
            case PENDING ->
                "En attente";
            case CONFIRMED ->
                "Confirmée";
            case SHIPPED ->
                "En cours de livraison";
            case DELIVERED ->
                "Livrée";
            case CANCELLED ->
                "Annulée";
            default ->
                status.name();
        };
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
     * Met à jour le statut d'une commande (Admin) et enregistre dans
     * l'historique.
     */
    @Transactional
    public Order updateOrderStatus(Long id, String statusName) {
        Order order = getOrderById(id);

        try {
            Order.Status newStatus = Order.Status.valueOf(statusName);
            order.setStatus(newStatus);
            Order savedOrder = orderRepository.save(order);

            // Créer une entrée dans l'historique (sans admin pour le moment)
            OrderStatusHistory history = OrderStatusHistory.builder()
                    .order(savedOrder)
                    .status(newStatus)
                    .build();
            statusHistoryRepository.save(history);

            // Notification livreurs quand une commande est confirmée (disponible à livrer)
            if (newStatus == Order.Status.CONFIRMED) {
                try {
                    Map<String, Object> notifData = Map.of(
                            "id", savedOrder.getId(),
                            "orderNumber", savedOrder.getOrderNumber(),
                            "customerAddress", savedOrder.getCustomerAddress() != null ? savedOrder.getCustomerAddress() : "",
                            "deliveryType", savedOrder.getDeliveryType() != null ? savedOrder.getDeliveryType() : ""
                    );
                    notificationService.notifyDeliveryAgents("new_delivery", notifData);
                    webPushService.notifyDeliveryAgents(
                        "🚚 Nouvelle livraison disponible",
                        "Commande #" + savedOrder.getOrderNumber(),
                        "delivery-" + savedOrder.getOrderNumber()
                    );
                } catch (Exception e) {
                    // Ne jamais bloquer le changement de statut
                }
            }

            return savedOrder;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide : " + statusName);
        }
    }

    /**
     * Récupère l'historique complet des changements de statut d'une commande
     */
    @Transactional(readOnly = true)
    public java.util.List<OrderStatusHistory> getOrderHistory(Long orderId) {
        // Vérifier que la commande existe
        getOrderById(orderId);
        return statusHistoryRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    /**
     * Génère le lien WhatsApp pour notifier un client selon le statut
     */
    public String generateWhatsAppNotificationLink(Long orderId, String phoneNumber) {
        Order order = getOrderById(orderId);

        // Dynamic Settings with Fallback
        String whatsappNumber = appSettingService.getSettingValue("whatsapp_number")
                .orElse(appProperties.getWhatsappNumber());

        String phoneInput = (phoneNumber != null && !phoneNumber.isBlank()) ? phoneNumber : order.getCustomerPhone();
        String phone = formatPhoneNumberForWhatsApp(phoneInput);

        StringBuilder message = new StringBuilder();
        message.append("Bonjour,\n\n");
        message.append("Votre commande #").append(order.getOrderNumber());
        if (order.getConfirmationCode() != null) {
            message.append(" (Code: ").append(order.getConfirmationCode()).append(")");
        }
        message.append(" sur SUCRE STORE est actuellement : *").append(getStatusLabel(order.getStatus())).append("*\n\n");

        // Message personnalisé selon le statut
        message.append(switch (order.getStatus()) {
            case CONFIRMED ->
                "Votre commande est en cours de préparation et sera bientôt livrée.";
            case SHIPPED ->
                "Votre commande est en cours de livraison.";
            case DELIVERED ->
                "Votre commande a été livrée avec succès. Merci de votre confiance !";
            case CANCELLED ->
                "Votre commande a été annulée. Pour plus d'informations, contactez-nous.";
            default ->
                "Nous vous tiendrons informé de l'évolution de votre commande.";
        });

        message.append("\n\nSUCRE STORE\n").append(whatsappNumber);

        String encodedMessage = URLEncoder.encode(message.toString(), StandardCharsets.UTF_8);
        return "https://wa.me/" + phone + "?text=" + encodedMessage;
    }

    /**
     * Formate un numéro de téléphone pour WhatsApp (ajoute 226 si nécessaire).
     */
    private String formatPhoneNumberForWhatsApp(String phone) {
        if (phone == null) {
            return "";
        }
        // Garder uniquement les chiffres
        String cleaned = phone.replaceAll("[^0-9]", "");

        // Retirer les 00 du début si présents
        if (cleaned.startsWith("00")) {
            cleaned = cleaned.substring(2);
        }

        // Ajouter 226 si le numéro ne commence pas par un indicatif connu (supposition 226 par défaut)
        // On suppose que si le numéro fait 8 chiffres (cas BF), on ajoute 226.
        // Si le numéro commence déjà par 226, on laisse.
        if (!cleaned.startsWith("226")) {
            return "226" + cleaned;
        }

        return cleaned;
    }

    /**
     * Génère un code de confirmation unique au format CONF-XXXX.
     */
    private String generateConfirmationCode() {
        // Format: CONF-1234 (4 chiffres aléatoires entre 1000 et 9999)
        int randomNumber = 1000 + (int) (Math.random() * 9000);
        return "CONF-" + randomNumber;
    }

    /**
     * Tente d'extraire les coordonnées GPS d'un lien Google Maps.
     */
    private BigDecimal[] extractCoordinatesFromLink(String link) {
        try {
            // Regex simple pour trouver lat,lng
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([0-9.-]+),([0-9.-]+)");
            java.util.regex.Matcher matcher = pattern.matcher(link);
            if (matcher.find()) {
                return new BigDecimal[]{
                    new BigDecimal(matcher.group(1)),
                    new BigDecimal(matcher.group(2))
                };
            }
        } catch (Exception e) {
            // Ignorer si format invalide
        }
        return null;
    }

    /**
     * Supprime logiquement une commande (Soft Delete).
     */
    @Transactional
    public void deleteOrder(Long id) {
        Order order = getOrderById(id);
        order.setDeleted(true);
        orderRepository.save(order);
    }

    /**
     * Récupère les commandes modifiées pour la synchronisation.
     */
    @Transactional(readOnly = true)
    public java.util.List<Order> syncOrders(String lastSyncStr) {
        java.time.LocalDateTime lastSync;
        if (lastSyncStr == null || lastSyncStr.isBlank()) {
            lastSync = java.time.LocalDateTime.now().minusDays(30); // Default to last 30 days
        } else {
            lastSync = java.time.LocalDateTime.parse(lastSyncStr);
        }
        return orderRepository.findByUpdatedAtAfter(lastSync);
    }

    // --- Méthodes Livraison (Traceability) ---
    /**
     * Un livreur prend en charge une commande "CONFIRMED".
     */
    @Transactional
    public Order claimOrder(Long orderId, String username) {
        Order order = getOrderById(orderId);

        if (order.getStatus() != Order.Status.CONFIRMED) {
            throw new RuntimeException("La commande n'est pas disponible (Statut: " + order.getStatus() + ")");
        }
        if (order.getDeliveryAgent() != null) {
            throw new RuntimeException("Cette commande est déjà prise en charge.");
        }

        com.sucrestore.api.entity.User agent = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + username));

        order.setDeliveryAgent(agent);
        order.setStatus(Order.Status.SHIPPED); // En cours de livraison

        return orderRepository.save(order);
    }

    /**
     * Un livreur valide la livraison avec le code client.
     */
    @Transactional
    public Order completeDelivery(Long orderId, String username, String code) {
        Order order = getOrderById(orderId);

        if (order.getStatus() != Order.Status.SHIPPED) {
            throw new RuntimeException("Statut incorrect pour validation : " + order.getStatus());
        }
        if (order.getDeliveryAgent() == null || !order.getDeliveryAgent().getUsername().equals(username)) {
            throw new RuntimeException("Vous n'êtes pas assigné à cette commande.");
        }

        // Vérification du code (ignorer la casse et les espaces)
        String inputCode = code.trim();
        if (!inputCode.equalsIgnoreCase(order.getConfirmationCode())) {
            throw new RuntimeException("Code de confirmation incorrect.");
        }

        order.setStatus(Order.Status.DELIVERED);
        return orderRepository.save(order);
    }

    /**
     * Récupère les commandes disponibles pour les livreurs. CONFIRMED + Pas de
     * livreur. NOTE: Pour la confidentialité, on masque les infos clients.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Order> getAvailableDeliveryOrders(org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Order> page = orderRepository.findByStatusAndDeliveryAgentNull(Order.Status.CONFIRMED, pageable);

        // Masquer les infos sensibles
        // On modifie les objets avant de les renvoyer (Attention: s'ils sont gérés par Hibernate, 
        // cela pourrait déclencher un update si on est dans une transaction active @Transactional.
        // Ici readOnly=true, mais par sécurité on détache ou on ne save pas.)
        // Une meilleure approche serait d'utiliser un DTO, mais pour aller vite on va "cleanser" la réponse.
        // Hibernate ne fera pas d'update car on est en read-only (normalement).
        page.getContent().forEach(o -> {
            o.setCustomerPhone("Masqué");
            o.setCustomerAddress("Zone: " + (o.getCustomerAddress().length() > 20 ? o.getCustomerAddress().substring(0, 20) + "..." : o.getCustomerAddress()));
            // On laisse un bout d'adresse pour que le livreur sache si c'est dans sa zone
        });

        return page;
    }

    /**
     * Récupère les commandes assignées au livreur connecté.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Order> getMyDeliveryOrders(String username, org.springframework.data.domain.Pageable pageable) {
        // En cours de livraison (SHIPPED)
        // On pourrait aussi vouloir voir l'historique (DELIVERED), mais la demande se concentre sur le workflow actif.
        // Ajoutons DELIVERED aussi pour l'historique récent ? Le user a dit "le premier a statut la prise en charge pourra ensuite voir les détails"
        // et "confirmer la livraison".
        // On va se concentrer sur SHIPPED pour l'onglet "Mes Courses".
        return orderRepository.findByDeliveryAgentUsernameAndStatusIn(
                username,
                java.util.List.of(Order.Status.SHIPPED),
                pageable
        );
    }
}
