package com.sucrestore.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.sucrestore.api.config.AppProperties;

/**
 * Point d'entrée de l'application Spring Boot "Sucre Store".
 *
 * Annotations : - @SpringBootApplication : Active la configuration automatique,
 * le scan des composants et la configuration. -
 *
 * @EnableConfigurationProperties(AppProperties.class) : Active la prise en
 * charge de notre classe de configuration typée.
 */
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class SucreStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SucreStoreApplication.class, args);
    }

    /**
     * Initialise un utilisateur administrateur par défaut au démarrage de
     * l'application.
     *
     * Credentials par défaut: - Username: admin - Email: admin@sucrestore.com -
     * Password: admin123 - Role: SUPER_ADMIN
     *
     * IMPORTANT: Changez ce mot de passe en production !
     */
    @org.springframework.context.annotation.Bean
    public org.springframework.boot.CommandLineRunner initDefaultAdmin(
            com.sucrestore.api.repository.UserRepository userRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {

        return args -> {
            // Vérifier si un admin existe déjà
            if (!userRepository.existsByUsername("admin")) {
                com.sucrestore.api.entity.User admin = com.sucrestore.api.entity.User.builder()
                        .username("admin")
                        .email("admin@sucrestore.com")
                        .password(passwordEncoder.encode("admin123"))
                        .role(com.sucrestore.api.entity.User.Role.SUPER_ADMIN)
                        .active(true)
                        .build();

                userRepository.save(admin);
                System.out.println("✅ Administrateur par défaut créé avec succès !");
                System.out.println("   Username: admin");
                System.out.println("   Password: admin123");
                System.out.println("   ⚠️  CHANGEZ CE MOT DE PASSE EN PRODUCTION !");
            } else {
                System.out.println("ℹ️  Administrateur 'admin' existe déjà.");
                // Réinitialiser le mot de passe pour être sûr (utile en dev)
                com.sucrestore.api.entity.User admin = userRepository.findByUsername("admin").get();
                admin.setPassword(passwordEncoder.encode("admin123"));
                userRepository.save(admin);
                System.out.println("🔄 Mot de passe réinitialisé à : admin123");
            }
        };
    }

}
