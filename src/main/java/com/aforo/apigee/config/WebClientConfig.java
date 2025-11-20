package com.aforo.apigee.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
    
    @Bean
    public WebClient productRatePlanClient(WebClient.Builder builder) {
        return builder.baseUrl("http://localhost:8081/api/products").build();
    }
}
