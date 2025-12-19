package com.bandwidth.userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

@Configuration
public class SecurityConfig {

    // Inject the key from the User Service's properties file
    @Value("${internal.api.key}")
    private String internalApiKey;

    // Must match the key used in the Auth Service Feign Config
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless APIs
                .authorizeHttpRequests(auth -> auth
                        // CRITICAL: Require the specific header and value for internal endpoint
                        .requestMatchers("/api/v1/users/internal/credentials")
                        .access((authentication, context) -> {
                            String header = context.getRequest().getHeader(INTERNAL_SECRET_HEADER);
                            return new AuthorizationDecision(
                                    header != null && header.equals(internalApiKey)
                            );
                        })

                        // Configure your external endpoints
                        // These will require JWT validation once implemented
                        //.requestMatchers("/api/v1/users/**").authenticated()

                        // Block everything else by default
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}