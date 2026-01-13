package com.aforo.integration.apigee;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient configuration for Apigee Management API.
 * Provides a pre-configured WebClient bean with authentication and timeouts.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ApigeeWebClientConfig {
    
    private final ApigeeProperties apigeeProperties;
    
    @Bean(name = "apigeeWebClient")
    public WebClient apigeeWebClient() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 
                    apigeeProperties.getConnectTimeoutSeconds() * 1000)
            .responseTimeout(Duration.ofSeconds(apigeeProperties.getReadTimeoutSeconds()))
            .doOnConnected(conn ->
                conn.addHandlerLast(new ReadTimeoutHandler(
                    apigeeProperties.getReadTimeoutSeconds(), TimeUnit.SECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(
                        apigeeProperties.getReadTimeoutSeconds(), TimeUnit.SECONDS)));
        
        return WebClient.builder()
            .baseUrl(apigeeProperties.getBaseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apigeeProperties.getToken())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter(logRequest())
            .filter(logResponse())
            .build();
    }
    
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (apigeeProperties.isDebugLogging()) {
                log.debug("Apigee API Request: {} {}", 
                    clientRequest.method(), clientRequest.url());
                clientRequest.headers().forEach((name, values) -> {
                    if (!name.equalsIgnoreCase(HttpHeaders.AUTHORIZATION)) {
                        values.forEach(value -> log.debug("Header: {} = {}", name, value));
                    }
                });
            }
            return Mono.just(clientRequest);
        });
    }
    
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (apigeeProperties.isDebugLogging()) {
                log.debug("Apigee API Response: {}", 
                    clientResponse.statusCode().value());
            }
            return Mono.just(clientResponse);
        });
    }
}
