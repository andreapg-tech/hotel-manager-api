package com.hotel.config;

import com.hotel.filters.JwtAuthenticationFilter;
import com.hotel.filters.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final java.util.List<String> origenesPermitidos;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, RateLimitFilter rateLimitFilter,
                          @Value("${app.cors.allowed-origins}") String origenesPermitidos) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
        // Lista explícita de orígenes de confianza (separados por comas), nunca un comodín
        this.origenesPermitidos = java.util.Arrays.stream(origenesPermitidos.split(","))
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toList();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) // Deshabilitado para APIs REST
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, authEx) -> {
                    res.setStatus(401);
                    res.setContentType("application/json");
                    res.setCharacterEncoding("UTF-8");
                    String json = String.format(
                        "{\"status\": 401, \"error\": \"Acceso denegado. Se requiere un token Bearer en el header Authorization.\", \"mensaje\": \"Acceso denegado. Se requiere un token Bearer en el header Authorization.\", \"ruta\": \"%s\"}",
                        req.getRequestURI()
                    );
                    res.getWriter().print(json);
                })
                .accessDeniedHandler((req, res, accessDeniedEx) -> {
                    res.setStatus(403);
                    res.setContentType("application/json");
                    res.setCharacterEncoding("UTF-8");
                    String json = String.format(
                        "{\"status\": 403, \"error\": \"No tienes permisos suficientes para acceder a este recurso.\", \"mensaje\": \"No tienes permisos suficientes para acceder a este recurso.\", \"ruta\": \"%s\"}",
                        req.getRequestURI()
                    );
                    res.getWriter().print(json);
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Preflight CORS
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Rutas Públicas (Auth y Búsqueda)
                .requestMatchers("/api/login", "/api/registro").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/habitaciones/**").permitAll()
                .requestMatchers("/api/preferencias").permitAll()
                
                // Portal de Empleados (Session-based, permitido aquí para manejo manual en PortalController)
                .requestMatchers("/portal/**").permitAll()
                
                // Rutas de Administración
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // Permitir reenvío interno de errores de Spring Boot (ej. 404)
                .requestMatchers("/error").permitAll()

                // El resto requiere autenticación por seguridad (Secure By Default)
                .anyRequest().authenticated()
            )
            // Añadimos nuestro filtro JWT antes del filtro de autenticación estándar
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
        // Solo los orígenes configurados pueden hacer peticiones con credenciales.
        // Un comodín ("*") junto con allowCredentials permitiría a cualquier web actuar en nombre del usuario.
        config.setAllowedOrigins(origenesPermitidos);
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(java.util.List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(java.util.List.of("Authorization", "Set-Cookie", "Content-Disposition"));

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
