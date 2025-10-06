package aforo.kong.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Utility class for interacting with Kong Admin API
 */
@Component
public class KongApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(KongApiClient.class);
    
    private final RestTemplate restTemplate;
    @SuppressWarnings("unused")
    private final ObjectMapper objectMapper; // Reserved for future JSON processing
    
    public KongApiClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Generic GET request to Kong Admin API
     */
    public <T> T get(String baseUrl, String path, String token, Class<T> responseType) {
        String url = buildUrl(baseUrl, path);
        HttpHeaders headers = createHeaders(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<T> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                responseType
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Failed to GET from Kong API: {}", url, e);
            throw new RuntimeException("Kong API GET failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generic POST request to Kong Admin API
     */
    public <T, R> R post(String baseUrl, String path, String token, T body, Class<R> responseType) {
        String url = buildUrl(baseUrl, path);
        HttpHeaders headers = createHeaders(token);
        HttpEntity<T> entity = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<R> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                responseType
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Failed to POST to Kong API: {}", url, e);
            throw new RuntimeException("Kong API POST failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generic PUT request to Kong Admin API
     */
    public <T, R> R put(String baseUrl, String path, String token, T body, Class<R> responseType) {
        String url = buildUrl(baseUrl, path);
        HttpHeaders headers = createHeaders(token);
        HttpEntity<T> entity = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<R> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                responseType
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Failed to PUT to Kong API: {}", url, e);
            throw new RuntimeException("Kong API PUT failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generic PATCH request to Kong Admin API
     */
    public <T, R> R patch(String baseUrl, String path, String token, T body, Class<R> responseType) {
        String url = buildUrl(baseUrl, path);
        HttpHeaders headers = createHeaders(token);
        HttpEntity<T> entity = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<R> response = restTemplate.exchange(
                url,
                HttpMethod.PATCH,
                entity,
                responseType
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Failed to PATCH to Kong API: {}", url, e);
            throw new RuntimeException("Kong API PATCH failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generic DELETE request to Kong Admin API
     */
    public void delete(String baseUrl, String path, String token) {
        String url = buildUrl(baseUrl, path);
        HttpHeaders headers = createHeaders(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        try {
            restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                entity,
                Void.class
            );
        } catch (Exception e) {
            logger.error("Failed to DELETE from Kong API: {}", url, e);
            throw new RuntimeException("Kong API DELETE failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create a consumer group in Kong
     */
    public void createConsumerGroup(String baseUrl, String token, String groupName) {
        Map<String, Object> body = Map.of("name", groupName);
        post(baseUrl, "/consumer-groups", token, body, Map.class);
        logger.info("Created consumer group: {}", groupName);
    }
    
    /**
     * Add consumer to a group
     */
    public void addConsumerToGroup(String baseUrl, String token, String consumerId, String groupName) {
        post(baseUrl, "/consumers/" + consumerId + "/consumer-groups", token, 
             Map.of("group", groupName), Map.class);
        logger.info("Added consumer {} to group {}", consumerId, groupName);
    }
    
    /**
     * Remove consumer from a group
     */
    public void removeConsumerFromGroup(String baseUrl, String token, String consumerId, String groupName) {
        delete(baseUrl, "/consumers/" + consumerId + "/consumer-groups/" + groupName, token);
        logger.info("Removed consumer {} from group {}", consumerId, groupName);
    }
    
    /**
     * Configure rate limiting plugin for a consumer group
     */
    public void configureRateLimitingForGroup(String baseUrl, String token, String groupName, 
                                             Map<String, Long> limits) {
        Map<String, Object> config = Map.of(
            "name", "rate-limiting-advanced",
            "consumer_group", groupName,
            "config", limits
        );
        
        post(baseUrl, "/plugins", token, config, Map.class);
        logger.info("Configured rate limiting for group {}: {}", groupName, limits);
    }
    
    /**
     * Add request termination plugin for a consumer
     */
    public void addRequestTerminationPlugin(String baseUrl, String token, String consumerId) {
        Map<String, Object> config = Map.of(
            "name", "request-termination",
            "consumer", Map.of("id", consumerId),
            "config", Map.of(
                "status_code", 402,
                "message", "Payment required - Wallet balance insufficient"
            )
        );
        
        post(baseUrl, "/plugins", token, config, Map.class);
        logger.info("Added request termination plugin for consumer: {}", consumerId);
    }
    
    /**
     * Configure HTTP Log plugin for a service
     */
    public void configureHttpLogPlugin(String baseUrl, String token, String serviceId, String logEndpoint) {
        Map<String, Object> config = Map.of(
            "name", "http-log",
            "service", Map.of("id", serviceId),
            "config", Map.of(
                "http_endpoint", logEndpoint,
                "method", "POST",
                "timeout", 10000,
                "keepalive", 60000,
                "retry_count", 3
            )
        );
        
        post(baseUrl, "/plugins", token, config, Map.class);
        logger.info("Configured HTTP log plugin for service {}: {}", serviceId, logEndpoint);
    }
    
    /**
     * Configure Correlation ID plugin globally
     */
    public void configureCorrelationIdPlugin(String baseUrl, String token) {
        Map<String, Object> config = Map.of(
            "name", "correlation-id",
            "config", Map.of(
                "header_name", "X-Correlation-ID",
                "generator", "uuid",
                "echo_downstream", true
            )
        );
        
        post(baseUrl, "/plugins", token, config, Map.class);
        logger.info("Configured global correlation ID plugin");
    }
    
    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null && !token.isEmpty()) {
            headers.setBearerAuth(token);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
    
    private String buildUrl(String baseUrl, String path) {
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        } else if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        }
        return baseUrl + path;
    }
}
