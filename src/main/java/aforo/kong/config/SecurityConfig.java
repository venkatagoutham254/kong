package aforo.kong.config;

import aforo.kong.security.JwtTenantFilter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${aforo.jwt.secret}")
    private String jwtSecret; // injected from application.yml or env

    // CORS configuration (comma-separated lists)
    @Value("${aforo.cors.allowed-origins:}")
    private String corsAllowedOrigins;

    @Value("${aforo.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String corsAllowedMethods;

    @Value("${aforo.cors.allowed-headers:Authorization,Content-Type,X-Organization-Id}")
    private String corsAllowedHeaders;

    @Value("${aforo.cors.allow-credentials:true}")
    private boolean corsAllowCredentials;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtTenantFilter jwtTenantFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .authorizeHttpRequests(auth -> auth
                // Allow Swagger & health endpoints without auth
                .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api/health").permitAll()
                // Allow Kong webhook endpoints without auth (Kong sends data here)
                .requestMatchers(HttpMethod.POST, "/integrations/kong/ingest").permitAll()
                .requestMatchers(HttpMethod.POST, "/integrations/kong/events").permitAll()
                // Allow preflight requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // All kong APIs need JWT
                .requestMatchers(HttpMethod.POST, "/api/kong/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/kong/**").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/kong/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/kong/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/kong/**").authenticated()
                // everything else
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt -> jwt.decoder(jwtDecoder()))
            );

        // ✅ Ensure the tenant filter runs AFTER Bearer token authentication
        http.addFilterAfter(jwtTenantFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(
                new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256")
        ).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(corsAllowCredentials);

        // Origins: support patterns for AWS/frontends. If not set, default to localhost:3000
        List<String> originPatterns;
        if (corsAllowedOrigins == null || corsAllowedOrigins.isBlank()) {
            originPatterns = List.of("http://localhost:3000");
        } else {
            originPatterns = Arrays.stream(corsAllowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        config.setAllowedOriginPatterns(originPatterns);

        // Methods
        List<String> methods = Arrays.stream(corsAllowedMethods.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        config.setAllowedMethods(methods);

        // Headers
        List<String> headers = Arrays.stream(corsAllowedHeaders.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        config.setAllowedHeaders(headers);

        config.setExposedHeaders(List.of("Location"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
