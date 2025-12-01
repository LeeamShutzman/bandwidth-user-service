package com.bandwidth.userservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // We apply this mapping to all paths starting with /api/ (e.g., /api/v1/tasks)
        registry.addMapping("/api/**")
                // Allowed origins are set to "*" (all) for easy local development.
                // In a production environment, this should be restricted to your specific frontend URL.
                .allowedOrigins("*")
                // Allowed HTTP methods for API endpoints
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // Allowed request headers
                .allowedHeaders("*");
    }
}
