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

}
