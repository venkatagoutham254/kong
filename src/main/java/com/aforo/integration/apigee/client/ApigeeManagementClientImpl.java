package com.aforo.integration.apigee.client;

import com.aforo.integration.apigee.ApigeeProperties;
import com.aforo.integration.apigee.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Implementation of Apigee Management API client using WebClient.
 */
@Slf4j
@Component
public class ApigeeManagementClientImpl implements ApigeeManagementClient {
    
    private final WebClient webClient;
    private final ApigeeProperties properties;
    
    public ApigeeManagementClientImpl(@Qualifier("apigeeWebClient") WebClient webClient,
                                      ApigeeProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }
    
    @Override
    public Mono<ApigeeOrgInfo> getOrganization() {
        String path = String.format("/organizations/%s", properties.getOrg());
        
        return webClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(ApigeeOrgInfo.class)
            .doOnSuccess(org -> log.debug("Retrieved organization: {}", org.getName()))
            .onErrorResume(WebClientResponseException.class, this::handleError);
    }
    
    @Override
    public Flux<String> listApis() {
        String path = String.format("/organizations/%s/apis", properties.getOrg());
        
        return webClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
            .flatMapMany(Flux::fromIterable)
            .doOnComplete(() -> log.debug("Listed all API proxies"))
            .onErrorResume(WebClientResponseException.class, e -> 
                Flux.error(new RuntimeException("Failed to list APIs: " + e.getMessage())));
    }
    
    @Override
    public Mono<ApigeeApiProxy> getApi(String apiName) {
        String path = String.format("/organizations/%s/apis/%s", properties.getOrg(), apiName);
        
        return webClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(ApigeeApiProxy.class)
            .doOnSuccess(api -> log.debug("Retrieved API proxy: {}", api.getName()))
            .onErrorResume(WebClientResponseException.class, this::handleError);
    }
    
    @Override
    public Flux<String> listApiProducts() {
        String path = String.format("/organizations/%s/apiproducts", properties.getOrg());
        
        return webClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
            .flatMapMany(Flux::fromIterable)
            .doOnComplete(() -> log.debug("Listed all API products"))
            .onErrorResume(WebClientResponseException.class, e -> 
                Flux.error(new RuntimeException("Failed to list API products: " + e.getMessage())));
    }
    
    @Override
    public Mono<ApigeeApiProduct> getApiProduct(String productName) {
        String path = String.format("/organizations/%s/apiproducts/%s", 
                                   properties.getOrg(), productName);
        
        return webClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(ApigeeApiProduct.class)
            .doOnSuccess(product -> log.debug("Retrieved API product: {}", product.getName()))
            .onErrorResume(WebClientResponseException.class, this::handleError);
    }
    
    @Override
    public Flux<String> listDevelopers() {
        String path = String.format("/organizations/%s/developers", properties.getOrg());
        
        return webClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
            .flatMapMany(Flux::fromIterable)
            .doOnComplete(() -> log.debug("Listed all developers"))
            .onErrorResume(WebClientResponseException.class, e -> 
                Flux.error(new RuntimeException("Failed to list developers: " + e.getMessage())));
    }
    
    @Override
    public Mono<ApigeeDeveloper> getDeveloper(String developerId) {
        String path = String.format("/organizations/%s/developers/%s", 
                                   properties.getOrg(), developerId);
        
        return webClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(ApigeeDeveloper.class)
            .doOnSuccess(dev -> log.debug("Retrieved developer: {}", dev.getEmail()))
            .onErrorResume(WebClientResponseException.class, this::handleError);
    }
    
    @Override
    public Flux<ApigeeAppRef> listDeveloperApps(String developerId) {
        String path = String.format("/organizations/%s/developers/%s/apps", 
                                   properties.getOrg(), developerId);
        
        return webClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
            .flatMapMany(Flux::fromIterable)
            .map(appName -> ApigeeAppRef.builder()
                .appName(appName)
                .build())
            .doOnComplete(() -> log.debug("Listed apps for developer: {}", developerId))
            .onErrorResume(WebClientResponseException.class, e -> 
                Flux.error(new RuntimeException("Failed to list developer apps: " + e.getMessage())));
    }
    
    @Override
    public Mono<ApigeeApp> getDeveloperApp(String developerId, String appName) {
        String path = String.format("/organizations/%s/developers/%s/apps/%s", 
                                   properties.getOrg(), developerId, appName);
        
        return webClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(ApigeeApp.class)
            .doOnSuccess(app -> log.debug("Retrieved app: {} for developer: {}", 
                                         app.getAppName(), developerId))
            .onErrorResume(WebClientResponseException.class, this::handleError);
    }
    
    @Override
    public Mono<Void> addApiProductToAppKey(String developerId, String appName, 
                                            String consumerKey, String apiProductName) {
        String path = String.format("/organizations/%s/developers/%s/apps/%s/keys/%s", 
                                   properties.getOrg(), developerId, appName, consumerKey);
        
        Map<String, Object> body = Map.of(
            "apiProducts", List.of(apiProductName),
            "action", "add"
        );
        
        return webClient.post()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .toBodilessEntity()
            .doOnSuccess(response -> 
                log.info("Added API product {} to app key {} for app {}", 
                        apiProductName, consumerKey, appName))
            .onErrorResume(WebClientResponseException.class, this::handleError)
            .then();
    }
    
    @Override
    public Mono<Void> removeApiProductFromAppKey(String developerId, String appName, 
                                                 String consumerKey, String apiProductName) {
        String path = String.format("/organizations/%s/developers/%s/apps/%s/keys/%s/apiproducts/%s", 
                                   properties.getOrg(), developerId, appName, consumerKey, apiProductName);
        
        return webClient.delete()
            .uri(path)
            .retrieve()
            .toBodilessEntity()
            .doOnSuccess(response -> 
                log.info("Removed API product {} from app key {} for app {}", 
                        apiProductName, consumerKey, appName))
            .onErrorResume(WebClientResponseException.class, this::handleError)
            .then();
    }
    
    @Override
    public Mono<Void> revokeAppKey(String developerId, String appName, String consumerKey) {
        String path = String.format("/organizations/%s/developers/%s/apps/%s/keys/%s", 
                                   properties.getOrg(), developerId, appName, consumerKey);
        
        Map<String, Object> body = Map.of("status", "revoked");
        
        return webClient.post()
            .uri(path + "?action=revoke")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .toBodilessEntity()
            .doOnSuccess(response -> 
                log.info("Revoked app key {} for app {}", consumerKey, appName))
            .onErrorResume(WebClientResponseException.class, this::handleError)
            .then();
    }
    
    @Override
    public Mono<Void> approveAppKey(String developerId, String appName, String consumerKey) {
        String path = String.format("/organizations/%s/developers/%s/apps/%s/keys/%s", 
                                   properties.getOrg(), developerId, appName, consumerKey);
        
        Map<String, Object> body = Map.of("status", "approved");
        
        return webClient.post()
            .uri(path + "?action=approve")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .toBodilessEntity()
            .doOnSuccess(response -> 
                log.info("Approved app key {} for app {}", consumerKey, appName))
            .onErrorResume(WebClientResponseException.class, this::handleError)
            .then();
    }
    
    @Override
    public Mono<Void> upsertKvmEntry(String kvmName, String entryName, String value) {
        String path = String.format("/organizations/%s/environments/%s/keyvaluemaps/%s/entries/%s", 
                                   properties.getOrg(), properties.getEnv(), kvmName, entryName);
        
        Map<String, Object> body = Map.of(
            "name", entryName,
            "value", value
        );
        
        return webClient.put()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .toBodilessEntity()
            .doOnSuccess(response -> 
                log.debug("Upserted KVM entry {} in map {}", entryName, kvmName))
            .onErrorResume(WebClientResponseException.class, this::handleError)
            .then();
    }
    
    @Override
    public Mono<String> getKvmEntry(String kvmName, String entryName) {
        String path = String.format("/organizations/%s/environments/%s/keyvaluemaps/%s/entries/%s", 
                                   properties.getOrg(), properties.getEnv(), kvmName, entryName);
        
        return webClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> (String) response.get("value"))
            .doOnSuccess(value -> 
                log.debug("Retrieved KVM entry {} from map {}", entryName, kvmName))
            .onErrorResume(WebClientResponseException.class, this::handleError);
    }
    
    private <T> Mono<T> handleError(WebClientResponseException e) {
        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            log.warn("Resource not found: {}", e.getMessage());
            return Mono.empty();
        } else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            log.error("Authentication failed: {}", e.getMessage());
            return Mono.error(new RuntimeException("Authentication failed. Check your Apigee token."));
        } else {
            log.error("Apigee API error: {} - {}", e.getStatusCode(), e.getMessage());
            return Mono.error(new RuntimeException("Apigee API error: " + e.getMessage()));
        }
    }
}
