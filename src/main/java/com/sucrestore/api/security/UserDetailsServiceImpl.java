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

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

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

    @Transactional
    public User createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Nom d'utilisateur déjà pris !");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email déjà utilisé !");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, User userRequest) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable ID: " + id));

        user.setUsername(userRequest.getUsername());
        user.setEmail(userRequest.getEmail());
        user.setRole(userRequest.getRole());
        user.setActive(userRequest.isActive());

        // Update password only if provided (non-empty)
        if (userRequest.getPassword() != null && !userRequest.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
