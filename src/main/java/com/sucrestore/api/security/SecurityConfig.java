package com.sucrestore.api.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Classe de configuration principale de la sécurité Spring Security. Définit
 * les règles d'accès, les filtres et les gestionnaires d'authentification.
 */
@Configuration
@EnableMethodSecurity // Active la sécurité au niveau des méthodes (ex: @PreAuthorize)
public class SecurityConfig {

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Configure le fournisseur d'authentification DAO (Data Access Object). Il
     * fait le lien entre UserDetaisService (les données) et PasswordEncoder
     * (l'encodage).
     */
    @Bean
    @SuppressWarnings("deprecation") // Supprime l'avertissement car DaoAuthenticationProvider est stable même si marqué déprécié dans certaines versions récentes pour inciter à la config lambda
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);

        return authProvider;
    }

    /**
     * Expose le Bean AuthenticationManager pour être utilisé dans les
     * contrôleurs (Login).
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Définit la chaîne de filtres de sécurité (Security Filter Chain). C'est
     * ici qu'on configure les règles HTTP.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable) // Désactive CSRF car nous utilisons des tokens JWT (Stateless)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler)) // Gestionnaire d'erreurs 401
                .cors(cors -> cors.configure(http)) // Active la configuration CORS
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Pas de session serveur (HttpSession)
                .authorizeHttpRequests(auth
                        -> // Liste blanche (URLs publiques)
                        auth.requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/products/**").permitAll()
                        .requestMatchers("/api/categories/**").permitAll()
                        .requestMatchers("/api/sliders/**").permitAll()
                        .requestMatchers("/api/orders/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll() // Accès aux images sans authentification
                        .requestMatchers("/error").permitAll()
                        // Tout le reste nécessite une authentification
                        .anyRequest().authenticated()
                );

        // Ajoute le fournisseur d'authentification configuré
        http.authenticationProvider(authenticationProvider());

        // Ajoute notre filtre JWT *avant* le filtre standard UsernamePassword
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configuration globale CORS pour autoriser le frontend React.
     */
    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOriginPatterns(java.util.List.of("*")); // Autorise tout (Dev)
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
