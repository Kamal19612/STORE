package com.sucrestore.api.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.sucrestore.api.entity.User;
import com.sucrestore.api.repository.UserRepository;

/**
 * Composant d'initialisation des données au démarrage de l'application.
 * S'exécute automatiquement après le lancement du contexte Spring.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    /**
     * Méthode exécutée au démarrage. Vérifie si la base de données est vide et
     * crée un utilisateur Admin par défaut si nécessaire.
     */
    @Override
    public void run(String... args) throws Exception {
        // Initialiser un Super Admin si aucun utilisateur n'existe
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@sucrestore.com")
                    .password(passwordEncoder.encode("password123")) // Mot de passe par défaut
                    .role(User.Role.SUPER_ADMIN)
                    .active(true)
                    .build();

            userRepository.save(admin);
            System.out.println("Super Admin User Created: admin / password123");
        }
    }
}
