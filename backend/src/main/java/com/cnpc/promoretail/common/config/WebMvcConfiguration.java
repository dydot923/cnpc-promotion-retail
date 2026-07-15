package com.cnpc.promoretail.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows configured frontend origins to access the backend API directly.
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String[] allowedOrigins;

    /**
     * The startup script may expose Vite on 127.0.0.1 or a LAN address and may
     * select another port when the default one is occupied.
     */
    @Value("${app.cors.allowed-origin-patterns:http://localhost:[*],http://127.0.0.1:[*],http://192.168.*:[*],http://10.*:[*],http://172.*:[*]}")
    private String[] allowedOriginPatterns;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
        registry.addMapping("/actuator/**")
                .allowedOrigins(allowedOrigins)
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET")
                .maxAge(3600);
    }
}
