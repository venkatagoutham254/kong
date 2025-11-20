package com.aforo.apigee.config;

import com.aforo.apigee.security.HmacFilter;
import com.aforo.apigee.security.JwtTenantFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

// Renamed to avoid conflict with Kong SecurityConfig
// @Configuration  // Disabled - using Kong's SecurityConfig
// @EnableWebSecurity  // Disabled - using Kong's SecurityConfig
@RequiredArgsConstructor
public class ApigeeSecurityConfig {
    
    private final HmacFilter hmacFilter;
    private final JwtTenantFilter jwtTenantFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication required
                .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                
                // Webhook endpoints - HMAC authentication only
                .requestMatchers("/api/integrations/apigee/webhooks/**").permitAll()
                
                // Connection and products endpoints - allow for testing
                .requestMatchers("/api/integrations/apigee/connections").permitAll()
                .requestMatchers("/api/integrations/apigee/products/**").permitAll()
                
                // All other API endpoints - JWT authentication required
                .requestMatchers("/api/integrations/apigee/**").authenticated()
                
                .anyRequest().authenticated()
            )
            // JWT filter first (for JWT auth)
            .addFilterBefore(jwtTenantFilter, UsernamePasswordAuthenticationFilter.class)
            // HMAC filter second (for webhook auth)
            .addFilterAfter(hmacFilter, JwtTenantFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(false);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
