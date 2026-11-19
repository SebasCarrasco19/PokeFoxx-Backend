
package com.pokefoxx.backend.config;

import com.pokefoxx.backend.security.CustomUserDetailsService;
import com.pokefoxx.backend.security.JwtAuthenticationFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Configuracion principal de seguridad
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider authenticationProvider,
            JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authenticationProvider(authenticationProvider)

                .authorizeHttpRequests(auth -> auth

                        // Permitir solicitudes OPTIONS de React
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Rutas de autenticacion
                        .requestMatchers("/api/auth/**").permitAll()

                        // Consultar productos publicamente
                        .requestMatchers(HttpMethod.GET, "/api/productos/**").permitAll()

                        // Crear productos: solamente administrador
                        .requestMatchers(HttpMethod.POST, "/api/productos/**")
                        .hasRole("ADMINISTRADOR")

                        // Actualizar productos: solamente administrador
                        .requestMatchers(HttpMethod.PUT, "/api/productos/**")
                        .hasRole("ADMINISTRADOR")

                        // Eliminar productos: solamente administrador
                        .requestMatchers(HttpMethod.DELETE, "/api/productos/**")
                        .hasRole("ADMINISTRADOR")

                        // Consultar categorias publicamente
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/categorias",
                                "/api/categorias/**"
                        ).permitAll()

                        // Crear categorias: solamente administrador
                        .requestMatchers(HttpMethod.POST, "/api/categorias/**")
                        .hasRole("ADMINISTRADOR")

                        // Actualizar categorias: solamente administrador
                        .requestMatchers(HttpMethod.PUT, "/api/categorias/**")
                        .hasRole("ADMINISTRADOR")

                        // Eliminar categorias: solamente administrador
                        .requestMatchers(HttpMethod.DELETE, "/api/categorias/**")
                        .hasRole("ADMINISTRADOR")

                        // Consultar perfil del usuario autenticado
                        .requestMatchers("/api/usuarios/me").authenticated()

                        // Gestion general de usuarios: solamente administrador
                        .requestMatchers("/api/usuarios/**")
                        .hasRole("ADMINISTRADOR")

                        // Cualquier otra ruta requiere autenticacion
                        .anyRequest().authenticated()
                )

                // Incorporar nuestro filtro JWT
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // Configurar autenticacion con usuarios de MySQL
    @Bean
    public AuthenticationProvider authenticationProvider(
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    // Administrador de autenticacion mediante ProviderManager
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationProvider authenticationProvider) {

        return new ProviderManager(authenticationProvider);
    }

    // Permitir conexion con React
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of("http://localhost:5173")
        );

        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );

        configuration.setAllowedHeaders(
                List.of("*")
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
