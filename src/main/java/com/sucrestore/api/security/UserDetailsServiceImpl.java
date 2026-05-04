package com.sucrestore.api.security;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sucrestore.api.entity.User;
import com.sucrestore.api.repository.UserRepository;
import com.sucrestore.api.tenant.StoreContext;

/**
 * Service implémentant l'interface UserDetailsService de Spring Security. Son
 * rôle est de charger les données de l'utilisateur depuis la base de données
 * pour que Spring Security puisse effectuer la vérification du mot de passe.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private com.sucrestore.api.service.SupabaseAdminService supabaseAdminService;

    @Autowired
    private com.sucrestore.api.repository.DeliveryAgentRepository deliveryAgentRepository;

    @Override
    @Transactional // Transactionnel car on pourrait charger des collections Lazy (ex: roles)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            // Accepte la connexion via username OU email (l'UI affiche souvent un email)
            User user = userRepository.findByUsernameOrEmail(username, username)
                    .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + username));

            // Guard rails: avoid NPEs that would bubble up as 500.
            if (user.getUsername() == null || user.getUsername().isBlank()) {
                log.error("[AUTH] user record is invalid (blank username) input={}", username);
                throw new BadCredentialsException("Bad credentials");
            }
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                log.error("[AUTH] user record is invalid (blank password) username={}", user.getUsername());
                throw new BadCredentialsException("Bad credentials");
            }
            if (user.getRole() == null) {
                log.error("[AUTH] user record is invalid (null role) username={} storeId={}",
                    user.getUsername(), user.getStore() != null ? user.getStore().getId() : null);
                throw new BadCredentialsException("Bad credentials");
            }

            // Retourne un objet User de Spring Security (et non notre Entité User)
            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(),
                    user.isActive(), // Enabled ?
                    true, // Account Non Expired
                    true, // Credentials Non Expired
                    true, // Account Non Locked
                    // Conversion du rôle en Authority Spring Security
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );
        } catch (BadCredentialsException | UsernameNotFoundException e) {
            // Expected auth failures
            throw e;
        } catch (RuntimeException e) {
            // Convert unexpected runtime errors to auth failure (prevents leaking stacktraces / 500s).
            log.error("[AUTH] unexpected error while loading user={}: {}", username, e.getMessage(), e);
            throw new BadCredentialsException("Bad credentials");
        }
    }

    // --- Méthodes Admin ---
    public List<User> getAllUsers() {
        if (isSuperAdminCaller()) {
            return userRepository.findAll();
        }
        Long storeId = StoreContext.getStoreIdOrNull();
        return userRepository.findByStoreId(storeId);
    }

    private boolean isSuperAdminCaller() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    private void assertCanManageTargetRole(User.Role targetRole) {
        // ADMIN est autorisé à créer/mettre à jour des rôles "non sensibles".
        // Les rôles ADMIN/SUPER_ADMIN restent réservés au SUPER_ADMIN.
        if (targetRole == null) {
            throw new RuntimeException("Rôle manquant.");
        }
        if ((targetRole == User.Role.ADMIN || targetRole == User.Role.SUPER_ADMIN) && !isSuperAdminCaller()) {
            throw new RuntimeException("Permission insuffisante pour attribuer ce rôle.");
        }
    }

    public User updateUserRole(Long id, String roleName) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable ID: " + id));

        try {
            User.Role role = User.Role.valueOf(roleName);

            // Vérification Unique Super Admin
            if (role == User.Role.SUPER_ADMIN && user.getRole() != User.Role.SUPER_ADMIN) {
                if (userRepository.findByRole(User.Role.SUPER_ADMIN).isPresent()) {
                    throw new RuntimeException("Un seul Super Admin est autorisé !");
                }
            }

            assertCanManageTargetRole(role);
            user.setRole(role);
            return userRepository.save(user);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Rôle invalide : " + roleName);
        }
    }

    @Transactional
    public User createUser(User user) {
        assertCanManageTargetRole(user.getRole());
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Nom d'utilisateur déjà pris !");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email déjà utilisé !");
        }

        // Vérification Unique Super Admin
        if (user.getRole() == User.Role.SUPER_ADMIN) {
            if (userRepository.findByRole(User.Role.SUPER_ADMIN).isPresent()) {
                throw new RuntimeException("Un seul Super Admin est autorisé !");
            }
        }

        final String rawPassword = user.getPassword();
        user.setPassword(passwordEncoder.encode(rawPassword));

        // Multi-store: attach store scope for non-global roles.
        if (user.getRole() != User.Role.DELIVERY_AGENT && user.getRole() != User.Role.DELIVERY) {
            Long storeId = StoreContext.getStoreIdOrNull();
            user.setStore(com.sucrestore.api.entity.Store.builder().id(storeId).build());
        } else {
            user.setStore(null); // global
        }

        User saved = userRepository.save(user);

        // Provision Supabase Auth + mapping for delivery/admin accounts.
        if (saved.getRole() == User.Role.DELIVERY_AGENT || saved.getRole() == User.Role.DELIVERY) {
            try {
                final String authUserId = supabaseAdminService.createAuthUser(
                    saved.getEmail(),
                    rawPassword, // password in clear from request
                    true
                );
                deliveryAgentRepository.upsertDeliveryAgent(
                    authUserId,
                    saved.getId(),
                    User.Role.DELIVERY_AGENT.name(),
                    saved.isActive()
                );
            } catch (Exception e) {
                throw new RuntimeException(
                    "Création Supabase Auth impossible pour ce livreur. " +
                    "Vérifiez SUPABASE_SERVICE_ROLE_KEY et/ou l'email (déjà utilisé ?). " +
                    "Détail: " + e.getMessage()
                );
            }
        }

        return saved;
    }

    @Transactional
    public User updateUser(Long id, User userRequest) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable ID: " + id));

        // Enforce store isolation for non-super-admin: cannot touch users from other stores.
        if (!isSuperAdminCaller()) {
            Long storeId = StoreContext.getStoreIdOrNull();
            if (user.getStore() == null || user.getStore().getId() == null || !user.getStore().getId().equals(storeId)) {
                throw new RuntimeException("Utilisateur introuvable ID: " + id);
            }
        }

        // Empêche un ADMIN de modifier un compte ADMIN/SUPER_ADMIN.
        if (!isSuperAdminCaller()
                && (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.SUPER_ADMIN)) {
            throw new RuntimeException("Permission insuffisante pour modifier ce compte.");
        }

        user.setUsername(userRequest.getUsername());
        user.setEmail(userRequest.getEmail());

        // Vérification Unique Super Admin
        if (userRequest.getRole() == User.Role.SUPER_ADMIN && user.getRole() != User.Role.SUPER_ADMIN) {
            if (userRepository.findByRole(User.Role.SUPER_ADMIN).isPresent()) {
                throw new RuntimeException("Un seul Super Admin est autorisé !");
            }
        }

        assertCanManageTargetRole(userRequest.getRole());
        user.setRole(userRequest.getRole());
        user.setActive(userRequest.isActive());

        // Store scope update: keep current store for store-scoped roles, clear for global delivery roles.
        if (userRequest.getRole() == User.Role.DELIVERY_AGENT || userRequest.getRole() == User.Role.DELIVERY) {
            user.setStore(null);
        } else if (user.getStore() == null) {
            Long storeId = StoreContext.getStoreIdOrNull();
            user.setStore(com.sucrestore.api.entity.Store.builder().id(storeId).build());
        }

        // Update password only if provided (non-empty)
        if (userRequest.getPassword() != null && !userRequest.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        }

        user.setPhone(userRequest.getPhone());

        // Champs identité CNIB (livreurs)
        user.setFirstName(userRequest.getFirstName());
        user.setLastName(userRequest.getLastName());
        user.setBirthDate(userRequest.getBirthDate());
        user.setBirthPlace(userRequest.getBirthPlace());
        user.setGender(userRequest.getGender());
        user.setProfession(userRequest.getProfession());
        user.setCnibNationalId(userRequest.getCnibNationalId());
        user.setCnibSerial(userRequest.getCnibSerial());
        user.setCnibIssueDate(userRequest.getCnibIssueDate());
        user.setCnibExpiryDate(userRequest.getCnibExpiryDate());
        user.setCnibOcrText(userRequest.getCnibOcrText());

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable ID: " + id));
        if (!isSuperAdminCaller() && (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.SUPER_ADMIN)) {
            throw new RuntimeException("Permission insuffisante pour supprimer ce compte.");
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public Long invalidateUserSession(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + username));

        // Incrémente la version du token (ce qui invalide les anciens tokens)
        Long currentVersion = user.getTokenVersion();
        if (currentVersion == null) {
            currentVersion = 0L;
        }
        user.setTokenVersion(currentVersion + 1);
        userRepository.save(user);

        return user.getTokenVersion();
    }

    public Long getUserTokenVersion(String username) {
        return userRepository.findByUsername(username)
                .map(User::getTokenVersion)
                .orElse(0L);
    }
}
