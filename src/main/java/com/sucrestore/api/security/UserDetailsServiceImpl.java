package com.sucrestore.api.security;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sucrestore.api.entity.User;
import com.sucrestore.api.repository.UserRepository;

/**
 * Service implémentant l'interface UserDetailsService de Spring Security. Son
 * rôle est de charger les données de l'utilisateur depuis la base de données
 * pour que Spring Security puisse effectuer la vérification du mot de passe.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional // Transactionnel car on pourrait charger des collections Lazy (ex: roles)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Recherche l'utilisateur dans la BDD par son username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec le nom d'utilisateur : " + username));

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
    }

    // --- Méthodes Admin ---
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User updateUserRole(Long id, String roleName) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable ID: " + id));

        try {
            User.Role role = User.Role.valueOf(roleName);
            user.setRole(role);
            return userRepository.save(user);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Rôle invalide : " + roleName);
        }
    }
}
