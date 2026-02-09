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
 * @EnableConfigurationProperties(AppProperties.class) : Active la prise en
 * charge de notre classe de configuration typée.
 */
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class SucreStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SucreStoreApplication.class, args);
    }

}
