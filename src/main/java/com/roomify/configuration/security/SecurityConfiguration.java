package com.roomify.configuration.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.roomify.infrastucture.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private static final String[] WHITELIST = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/verify",
            "/api/v1/auth/resend-verification"
    };

    private final JwtAuthenticationFilter jwtFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfiguration(JwtAuthenticationFilter jwtFilter, CustomUserDetailsService userDetailsService) {
        this.jwtFilter = jwtFilter;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Configure la chaîne de filtres Spring Security pour une API REST stateless.
     *
     * <p>Cette configuration transforme Spring Security en mode "backend API" :
     * aucun mécanisme d'authentification basé sur navigateur ou session serveur
     * n'est utilisé. L'application est destinée à être consommée par un client
     * (frontend SPA, mobile, autre service) qui fournit un token d'authentification
     * à chaque requête (ex: JWT).</p>
     *
     * <h2>Comportement global</h2>
     * <ul>
     *     <li>Désactive la protection CSRF (inutile en stateless).</li>
     *     <li>Désactive totalement les sessions HTTP côté serveur.</li>
     *     <li>Autorise librement les endpoints d'authentification.</li>
     *     <li>Exige une authentification pour toutes les autres requêtes.</li>
     *     <li>Désactive les mécanismes d'authentification navigateur par défaut.</li>
     * </ul>
     *
     * <h2>Détails</h2>
     * <ul>
     *     <li><b>CSRF désactivé</b> :
     *     adapté aux API REST sans cookies de session (ex: JWT dans header Authorization).</li>
     *
     *     <li><b>SessionCreationPolicy.STATELESS</b> :
     *     aucune session n'est créée ni utilisée. Chaque requête doit être autonome.</li>
     *
     *     <li><b>/api/auth/** public</b> :
     *     endpoints de login/refresh/register accessibles sans authentification.</li>
     *
     *     <li><b>Toutes les autres routes sécurisées</b> :
     *     nécessitent un utilisateur authentifié.</li>
     *
     *     <li><b>httpBasic désactivé</b> :
     *     empêche la popup navigateur d'authentification.</li>
     *
     *     <li><b>formLogin désactivé</b> :
     *     supprime la page de login HTML générée par Spring Security.</li>
     * </ul>
     *
     * <h2>Cas d'usage typique</h2>
     * API REST avec authentification par token (JWT, OAuth2 Bearer, API key, etc).
     *
     * @param http configuration HttpSecurity fournie par Spring Security
     * @return SecurityFilterChain configurée pour une API REST stateless
     * @throws Exception en cas d'erreur lors de la construction de la chaîne de filtres
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITELIST).permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

}
