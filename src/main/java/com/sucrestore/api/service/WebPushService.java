package com.sucrestore.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sucrestore.api.entity.PushSubscription;
import com.sucrestore.api.entity.User;
import com.sucrestore.api.repository.PushSubscriptionRepository;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.Security;
import java.util.List;
import java.util.Map;

/**
 * Envoie des notifications Web Push (VAPID) aux appareils abonnés.
 * Fonctionne en arrière-plan (navigateur fermé / app en background).
 */
@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private static final String VAPID_PUBLIC_KEY  = "BFgwMeEPKmjceqgeKQqezk_yyf_FLa7LTW7eul_0HnSMfPfPFnKwH-fSGjxCUU5cmiCAdIvqlTJkSGUBkhPgFCw";
    private static final String VAPID_PRIVATE_KEY = "4TTR08LVeO1sYKIOoSvI84utCeybjsoENUGMt7HtzDQ";
    private static final String VAPID_SUBJECT     = "mailto:admin@sucrestore.socialracine.com";

    private final PushSubscriptionRepository repo;
    private final ObjectMapper objectMapper;
    private final PushService pushService;

    public WebPushService(PushSubscriptionRepository repo, ObjectMapper objectMapper) throws Exception {
        this.repo = repo;
        this.objectMapper = objectMapper;

        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        this.pushService = new PushService(VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY, VAPID_SUBJECT);
    }

    /**
     * Envoie une notification push à tous les abonnés ayant le rôle donné.
     */
    public void notifyByRole(User.Role role, String title, String body, String tag) {
        List<PushSubscription> subscriptions = repo.findByUserRole(role);
        if (subscriptions.isEmpty()) return;

        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "body",  body,
                "tag",   tag
            ));
        } catch (Exception e) {
            log.error("Erreur sérialisation payload push", e);
            return;
        }

        for (PushSubscription sub : subscriptions) {
            try {
                Notification notification = new Notification(
                    sub.getEndpoint(),
                    sub.getP256dh(),
                    sub.getAuth(),
                    payload
                );
                pushService.send(notification);
            } catch (Exception e) {
                log.warn("Échec push vers {} : {}", sub.getEndpoint(), e.getMessage());
                // Supprime les souscriptions expirées (410 Gone)
                if (e.getMessage() != null && e.getMessage().contains("410")) {
                    repo.delete(sub);
                }
            }
        }
    }

    public void notifyAdmins(String title, String body, String tag) {
        notifyByRole(User.Role.SUPER_ADMIN, title, body, tag);
        notifyByRole(User.Role.ADMIN,       title, body, tag);
    }

    public void notifyDeliveryAgents(String title, String body, String tag) {
        notifyByRole(User.Role.DELIVERY_AGENT, title, body, tag);
    }
}