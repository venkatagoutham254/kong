package aforo.kong.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class KonnectWebClient {

    private static final Logger logger = LoggerFactory.getLogger(KonnectWebClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public KonnectWebClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> testConnection(String baseUrl, String authToken) {
        try {
            String url = baseUrl + "/v2/control-planes";
            HttpHeaders headers = createHeaders(authToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                int count = root.has("data") ? root.get("data").size() : 0;
                return Map.of(
                    "ok", true,
                    "message", "Connected successfully",
                    "controlPlaneCount", count
                );
            } else {
                logger.warn("Konnect connection test failed with status: {}", response.getStatusCode());
                return Map.of(
                    "ok", false,
                    "message", "Connection failed. Verify token and baseUrl."
                );
            }
        } catch (Exception e) {
            logger.error("Failed to test Konnect connection to: {}", baseUrl, e);
            return Map.of(
                "ok", false,
                "message", "Connection failed. Verify token and baseUrl."
            );
        }
    }

    public List<Map<String, Object>> listControlPlanes(String baseUrl, String authToken) {
        try {
            String url = baseUrl + "/v2/control-planes";
            HttpHeaders headers = createHeaders(authToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                List<Map<String, Object>> controlPlanes = new ArrayList<>();
                
                if (root.has("data")) {
                    for (JsonNode node : root.get("data")) {
                        controlPlanes.add(Map.of(
                            "id", node.get("id").asText(),
                            "name", node.has("name") ? node.get("name").asText() : "Unknown"
                        ));
                    }
                }
                return controlPlanes;
            }
            logger.warn("Failed to list control planes, status: {}", response.getStatusCode());
            return List.of();
        } catch (Exception e) {
            logger.error("Failed to list control planes from: {}", baseUrl, e);
            return List.of();
        }
    }

    public List<Map<String, Object>> listApiProducts(String baseUrl, String controlPlaneId, String authToken) {
        try {
            // Konnect API products are organization-wide, not control-plane specific
            String url = baseUrl + "/v2/api-products";
            
            HttpHeaders headers = createHeaders(authToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                List<Map<String, Object>> products = new ArrayList<>();
                
                if (root.has("data")) {
                    for (JsonNode node : root.get("data")) {
                        products.add(parseApiProduct(node));
                    }
                }
                return products;
            }
            logger.warn("Failed to list API products, status: {}", response.getStatusCode());
            return List.of();
        } catch (Exception e) {
            logger.error("Failed to list API products from: {}", baseUrl, e);
            return List.of();
        }
    }

    public Map<String, Object> getApiProductById(String baseUrl, String productId, String authToken) {
        try {
            String url = baseUrl + "/v2/api-products/" + productId;
            HttpHeaders headers = createHeaders(authToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode node = objectMapper.readTree(response.getBody());
                return parseApiProduct(node);
            }
            logger.warn("Failed to get API product {}, status: {}", productId, response.getStatusCode());
            return Map.of();
        } catch (Exception e) {
            logger.error("Failed to get API product by ID: {}", productId, e);
            return Map.of();
        }
    }

    private Map<String, Object> parseApiProduct(JsonNode node) {
        return Map.of(
            "id", node.get("id").asText(),
            "name", node.has("name") ? node.get("name").asText() : "",
            "description", node.has("description") ? node.get("description").asText() : "",
            "status", node.has("status") ? node.get("status").asText() : "unknown",
            "versionCount", node.has("version_count") ? node.get("version_count").asInt() : 0,
            "updatedAt", node.has("updated_at") ? node.get("updated_at").asText() : ""
        );
    }

    public List<Map<String, Object>> listServices(String baseUrl, String controlPlaneId, String authToken, Integer page, Integer size) {
        try {
            String url = baseUrl + "/v2/control-planes/" + controlPlaneId + "/core-entities/services";
            if (page != null && size != null) {
                url += "?page=" + page + "&size=" + size;
            }
            
            HttpHeaders headers = createHeaders(authToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                List<Map<String, Object>> services = new ArrayList<>();
                
                if (root.has("data")) {
                    for (JsonNode node : root.get("data")) {
                        Map<String, Object> service = new HashMap<>();
                        service.put("id", node.has("id") ? node.get("id").asText() : null);
                        service.put("name", node.has("name") ? node.get("name").asText() : null);
                        service.put("host", node.has("host") ? node.get("host").asText() : null);
                        service.put("port", node.has("port") ? node.get("port").asInt() : null);
                        service.put("path", node.has("path") ? node.get("path").asText() : null);
                        service.put("protocol", node.has("protocol") ? node.get("protocol").asText() : null);
                        
                        if (node.has("tags")) {
                            List<String> tags = new ArrayList<>();
                            for (JsonNode tag : node.get("tags")) {
                                tags.add(tag.asText());
                            }
                            service.put("tags", tags);
                        }
                        
                        service.put("created_at", node.has("created_at") ? node.get("created_at").asLong() : null);
                        service.put("updated_at", node.has("updated_at") ? node.get("updated_at").asLong() : null);
                        services.add(service);
                    }
                }
                return services;
            }
            logger.warn("Failed to list services, status: {}", response.getStatusCode());
            return List.of();
        } catch (Exception e) {
            logger.error("Failed to list services from control plane: {}", controlPlaneId, e);
            return List.of();
        }
    }

    public List<Map<String, Object>> listRoutes(String baseUrl, String controlPlaneId, String authToken, Integer page, Integer size) {
        try {
            String url = baseUrl + "/v2/control-planes/" + controlPlaneId + "/core-entities/routes";
            if (page != null && size != null) {
                url += "?page=" + page + "&size=" + size;
            }
            
            HttpHeaders headers = createHeaders(authToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                List<Map<String, Object>> routes = new ArrayList<>();
                
                if (root.has("data")) {
                    for (JsonNode node : root.get("data")) {
                        Map<String, Object> route = new HashMap<>();
                        route.put("id", node.has("id") ? node.get("id").asText() : null);
                        route.put("name", node.has("name") ? node.get("name").asText() : null);
                        
                        // Service reference
                        if (node.has("service") && node.get("service").has("id")) {
                            route.put("service_id", node.get("service").get("id").asText());
                        }
                        
                        // Arrays
                        route.put("protocols", extractStringArray(node, "protocols"));
                        route.put("methods", extractStringArray(node, "methods"));
                        route.put("hosts", extractStringArray(node, "hosts"));
                        route.put("paths", extractStringArray(node, "paths"));
                        route.put("tags", extractStringArray(node, "tags"));
                        
                        route.put("strip_path", node.has("strip_path") ? node.get("strip_path").asBoolean() : null);
                        route.put("preserve_host", node.has("preserve_host") ? node.get("preserve_host").asBoolean() : null);
                        route.put("created_at", node.has("created_at") ? node.get("created_at").asLong() : null);
                        route.put("updated_at", node.has("updated_at") ? node.get("updated_at").asLong() : null);
                        routes.add(route);
                    }
                }
                return routes;
            }
            logger.warn("Failed to list routes, status: {}", response.getStatusCode());
            return List.of();
        } catch (Exception e) {
            logger.error("Failed to list routes from control plane: {}", controlPlaneId, e);
            return List.of();
        }
    }

    public List<Map<String, Object>> listConsumers(String baseUrl, String controlPlaneId, String authToken, Integer page, Integer size) {
        try {
            String url = baseUrl + "/v2/control-planes/" + controlPlaneId + "/core-entities/consumers";
            if (page != null && size != null) {
                url += "?page=" + page + "&size=" + size;
            }
            
            HttpHeaders headers = createHeaders(authToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                List<Map<String, Object>> consumers = new ArrayList<>();
                
                if (root.has("data")) {
                    for (JsonNode node : root.get("data")) {
                        Map<String, Object> consumer = new HashMap<>();
                        consumer.put("id", node.has("id") ? node.get("id").asText() : null);
                        consumer.put("username", node.has("username") ? node.get("username").asText() : null);
                        consumer.put("custom_id", node.has("custom_id") ? node.get("custom_id").asText() : null);
                        consumer.put("tags", extractStringArray(node, "tags"));
                        consumer.put("created_at", node.has("created_at") ? node.get("created_at").asLong() : null);
                        consumer.put("updated_at", node.has("updated_at") ? node.get("updated_at").asLong() : null);
                        consumers.add(consumer);
                    }
                }
                return consumers;
            }
            logger.warn("Failed to list consumers, status: {}", response.getStatusCode());
            return List.of();
        } catch (Exception e) {
            logger.error("Failed to list consumers from control plane: {}", controlPlaneId, e);
            return List.of();
        }
    }

    public List<Map<String, Object>> listConsumerGroups(String baseUrl, String controlPlaneId, String authToken, Integer page, Integer size) {
        try {
            String url = baseUrl + "/v2/control-planes/" + controlPlaneId + "/core-entities/consumer_groups";
            if (page != null && size != null) {
                url += "?page=" + page + "&size=" + size;
            }
            
            HttpHeaders headers = createHeaders(authToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                List<Map<String, Object>> groups = new ArrayList<>();
                
                if (root.has("data")) {
                    for (JsonNode node : root.get("data")) {
                        Map<String, Object> group = new HashMap<>();
                        group.put("id", node.has("id") ? node.get("id").asText() : null);
                        group.put("name", node.has("name") ? node.get("name").asText() : null);
                        group.put("tags", extractStringArray(node, "tags"));
                        group.put("created_at", node.has("created_at") ? node.get("created_at").asLong() : null);
                        group.put("updated_at", node.has("updated_at") ? node.get("updated_at").asLong() : null);
                        groups.add(group);
                    }
                }
                return groups;
            }
            logger.warn("Failed to list consumer groups, status: {}", response.getStatusCode());
            return List.of();
        } catch (Exception e) {
            logger.error("Failed to list consumer groups from control plane: {}", controlPlaneId, e);
            return List.of();
        }
    }

    private List<String> extractStringArray(JsonNode node, String fieldName) {
        List<String> result = new ArrayList<>();
        if (node.has(fieldName) && node.get(fieldName).isArray()) {
            for (JsonNode item : node.get(fieldName)) {
                result.add(item.asText());
            }
        }
        return result;
    }

    private HttpHeaders createHeaders(String authToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}
