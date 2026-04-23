package com.sucrestore.api.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.sucrestore.api.entity.User;
import com.sucrestore.api.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Initialiseur de données s'exécutant au démarrage de l'application. Crée un
 * utilisateur Super Admin par défaut si la base est vide.
 */
@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        final String username = "admin";
        final String email = "admin@gmail.com";
        final String rawPassword = "Pass_word.(1)@!";

        // Crée (ou remet à niveau) le Super Admin à chaque démarrage.
        // IMPORTANT: ce comportement réinitialise le mot de passe du compte 'admin' à chaque restart.
        User admin = userRepository.findByUsername(username).orElseGet(() -> User.builder().username(username).build());

        admin.setEmail(email);
        admin.setRole(User.Role.SUPER_ADMIN);
        admin.setActive(true);
        admin.setPassword(passwordEncoder.encode(rawPassword));

        userRepository.save(admin);

        log.info("--------------------------------------------------");
        log.info("SUPER ADMIN ASSURÉ AU DÉMARRAGE :");
        log.info("Username : {}", username);
        log.info("Email    : {}", email);
        log.info("Password : {}", rawPassword);
        log.info("Rôle     : SUPER_ADMIN");
        log.info("--------------------------------------------------");
    }
}
