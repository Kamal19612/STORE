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
        if (userRepository.count() == 0) {
            log.info("Base de données vide. Création du compte Super Admin par défaut...");

            User admin = User.builder()
                    .username("admin")
                    .email("admin@sucrestore.com")
                    .password(passwordEncoder.encode("Pass_word.(1)@!")) // À changer dès la première connexion
                    .role(User.Role.SUPER_ADMIN)
                    .active(true)
                    .build();

            userRepository.save(admin);

            log.info("--------------------------------------------------");
            log.info("IDENTIFIANTS PAR DÉFAUT CRÉÉS :");
            log.info("Username : admin");
            log.info("Password : Pass_word.(1)@!");
            log.info("Rôle     : SUPER_ADMIN");
            log.info("--------------------------------------------------");
        }
    }
}
