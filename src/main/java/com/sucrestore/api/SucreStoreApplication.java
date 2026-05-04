package com.sucrestore.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.sucrestore.api.config.AppProperties;
import com.sucrestore.api.config.GoogleConfig;

/**
 * Point d'entrée de l'application Spring Boot "Sucre Store".
 */
@SpringBootApplication
@EntityScan("com.sucrestore.api.entity")
@EnableJpaRepositories("com.sucrestore.api.repository")
@EnableConfigurationProperties({AppProperties.class, GoogleConfig.class})
@org.springframework.scheduling.annotation.EnableScheduling
public class SucreStoreApplication {

    public static void main(String[] args) {
        // Doit être défini AVANT SpringApplication.run() pour empêcher le
        // RestartClassLoader de DevTools de s'installer (sinon il casse le scan
        // Hibernate des inner-classes générées par Lombok @Builder).
        System.setProperty("spring.devtools.restart.enabled", "false");
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
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(name = "entityManagerFactory")
    public org.springframework.boot.CommandLineRunner initDefaultAdmin(
            com.sucrestore.api.repository.UserRepository userRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {

        return args -> {
            // Vérifier si un admin existe déjà
            if (!userRepository.existsByUsername("admin")) {
                com.sucrestore.api.entity.User admin = com.sucrestore.api.entity.User.builder()
                        .username("admin")
                        .email("admin@sucrestore.com")
                        .password(passwordEncoder.encode("Pass_word.(1)@!"))
                        .role(com.sucrestore.api.entity.User.Role.SUPER_ADMIN)
                        .active(true)
                        .build();

                userRepository.save(admin);
                System.out.println("✅ Administrateur par défaut créé avec succès !");
                System.out.println("   Username: admin");
                System.out.println("   Password: Pass_word.(1)@!");
                System.out.println("   ⚠️  CHANGEZ CE MOT DE PASSE EN PRODUCTION !");
            } else {
                System.out.println("ℹ️  Administrateur 'admin' existe déjà.");
                // Réinitialiser le mot de passe pour être sûr (utile en dev)
                com.sucrestore.api.entity.User admin = userRepository.findByUsername("admin").get();
                admin.setPassword(passwordEncoder.encode("Pass_word.(1)@!"));
                userRepository.save(admin);
                System.out.println("🔄 Mot de passe réinitialisé à : Pass_word.(1)@!");
            }
        };
    }

}
