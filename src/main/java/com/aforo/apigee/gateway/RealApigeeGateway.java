package com.aforo.apigee.gateway;

import com.aforo.apigee.dto.ApigeeCustomer;
import com.aforo.apigee.dto.response.ApiProductResponse;
import com.aforo.apigee.dto.response.DeveloperAppResponse;
import com.aforo.apigee.dto.response.DeveloperResponse;
import com.aforo.apigee.model.ConnectionConfig;
import com.aforo.apigee.repository.ConnectionConfigRepository;
import com.aforo.apigee.service.ServiceAccountManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
@ConditionalOnProperty(name = "aforo.apigee.fake", havingValue = "false")
public class RealApigeeGateway implements ApigeeGateway {
    
    private final WebClient webClient;
    private final String saJsonPath;
    private final ObjectMapper objectMapper;
    private final ServiceAccountManager serviceAccountManager;
    private final ConnectionConfigRepository connectionConfigRepository;
    
    public RealApigeeGateway(
            @Value("${aforo.apigee.base-url}") String baseUrl,
            @Value("${aforo.apigee.sa-json-path}") String saJsonPath,
            ObjectMapper objectMapper,
            ServiceAccountManager serviceAccountManager,
            ConnectionConfigRepository connectionConfigRepository) {
        this.saJsonPath = saJsonPath;
        this.objectMapper = objectMapper;
        this.serviceAccountManager = serviceAccountManager;
        this.connectionConfigRepository = connectionConfigRepository;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
    
    private String getAccessToken(String org) {
        try {
            // Try to get service account JSON from database first (for AWS/production)
            Optional<ConnectionConfig> configOpt = connectionConfigRepository.findByOrg(org);
            if (configOpt.isPresent() && configOpt.get().getServiceAccountJson() != null) {
                log.debug("Using service account JSON from database for org: {}", org);
                return serviceAccountManager.getAccessTokenFromJson(configOpt.get().getServiceAccountJson());
            }
            
            // Fall back to file path (for local development)
            log.debug("Using service account JSON from file path: {}", saJsonPath);
            return serviceAccountManager.getAccessTokenFromPath(saJsonPath);
        } catch (IOException e) {
            log.error("Failed to get access token for org: {}", org, e);
            throw new RuntimeException("Failed to authenticate with Google: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<ApiProductResponse> listApiProducts(String org) {
        log.info("RealApigeeGateway: Listing API products for org: {}", org);
        String token = getAccessToken(org);
        
        try {
            // First, get list of product names
            String response = webClient.get()
                    .uri("/v1/organizations/{org}/apiproducts", org)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            log.info("API Products list response: {}", response);
            JsonNode root = objectMapper.readTree(response);
            
            // Apigee returns an object with "apiProduct" array
            JsonNode apiProductsNode = root.get("apiProduct");
            if (apiProductsNode != null && apiProductsNode.isArray()) {
                log.info("Found {} products in the list", apiProductsNode.size());
                List<ApiProductResponse> products = new ArrayList<>();
                for (JsonNode productObj : apiProductsNode) {
                    // Each element is an object with "name" field
                    JsonNode productName = productObj.get("name");
                    if (productName == null) continue;
                    try {
                        String productNameStr = productName.asText();
                        log.info("Fetching details for product: {}", productNameStr);
                        
                        // Fetch details for each product
                        String productDetail = webClient.get()
                                .uri("/v1/organizations/{org}/apiproducts/{product}", org, productNameStr)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .retrieve()
                                .bodyToMono(String.class)
                                .block();
                        
                        log.debug("Product detail response for {}: {}", productNameStr, productDetail);
                        JsonNode productNode = objectMapper.readTree(productDetail);
                        ApiProductResponse productResponse = mapToApiProductResponse(productNode);
                        products.add(productResponse);
                        log.info("Successfully mapped product: {}", productResponse.getName());
                    } catch (Exception e) {
                        log.error("Failed to fetch details for product: {}", productName.asText(), e);
                    }
                }
                log.info("Returning {} products", products.size());
                return products;
            }
            
            log.warn("API response is not an array, returning empty list");
            return Collections.emptyList();
                    
        } catch (Exception e) {
            log.error("Failed to list API products", e);
            throw new RuntimeException("Failed to list API products", e);
        }
    }
    
    @Override
    public List<DeveloperResponse> listDevelopers(String org) {
        log.info("RealApigeeGateway: Listing developers for org: {}", org);
        String token = getAccessToken(org);
        
        try {
            String response = webClient.get()
                    .uri("/v1/organizations/{org}/developers", org)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            log.info("Developers list response: {}", response);
            JsonNode root = objectMapper.readTree(response);
            
            // Apigee returns an object with "developer" array
            JsonNode developersNode = root.get("developer");
            if (developersNode != null && developersNode.isArray()) {
                log.info("Found {} developers in the list", developersNode.size());
                List<DeveloperResponse> developers = new ArrayList<>();
                for (JsonNode devObj : developersNode) {
                    try {
                        // Fetch full developer details
                        JsonNode emailNode = devObj.get("email");
                        if (emailNode == null) continue;
                        
                        String email = emailNode.asText();
                        log.info("Fetching details for developer: {}", email);
                        
                        String devDetail = webClient.get()
                                .uri("/v1/organizations/{org}/developers/{email}", org, email)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .retrieve()
                                .bodyToMono(String.class)
                                .block();
                        
                        JsonNode devNode = objectMapper.readTree(devDetail);
                        developers.add(mapToDeveloperResponse(devNode));
                    } catch (Exception e) {
                        log.error("Failed to fetch developer details", e);
                    }
                }
                log.info("Returning {} developers", developers.size());
                return developers;
            }
            
            log.warn("Developer response is not in expected format, returning empty list");
            return Collections.emptyList();
                    
        } catch (Exception e) {
            log.error("Failed to list developers", e);
            throw new RuntimeException("Failed to list developers", e);
        }
    }
    
    @Override
    public List<DeveloperAppResponse> listDeveloperApps(String org, String developerId) {
        log.info("RealApigeeGateway: Listing apps for developer: {} in org: {}", developerId, org);
        String token = getAccessToken(org);
        
        try {
            log.info("Making request to: /v1/organizations/{}/developers/{}/apps", org, developerId);
            String response = webClient.get()
                    .uri("/v1/organizations/{org}/developers/{developerId}/apps", org, developerId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            log.info("Developer apps list response for {}: {}", developerId, response);
            JsonNode root = objectMapper.readTree(response);
            
            // Apigee returns an object with "app" array
            JsonNode appsNode = root.get("app");
            if (appsNode != null && appsNode.isArray()) {
                log.info("Found {} apps for developer {}", appsNode.size(), developerId);
                List<DeveloperAppResponse> apps = new ArrayList<>();
                for (JsonNode appObj : appsNode) {
                    String appName = "unknown";
                    try {
                        // Apigee returns "appId" in the list, not "name"
                        JsonNode appIdNode = appObj.get("appId");
                        if (appIdNode == null) {
                            appIdNode = appObj.get("name");  // fallback to name
                        }
                        if (appIdNode == null) continue;
                        
                        appName = appIdNode.asText();
                        log.info("Fetching details for app: {}", appName);
                        
                        // Fetch full app details
                        String appDetail = webClient.get()
                                .uri("/v1/organizations/{org}/developers/{developerId}/apps/{appName}", 
                                     org, developerId, appName)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .retrieve()
                                .bodyToMono(String.class)
                                .block();
                        
                        log.info("App detail response: {}", appDetail);
                        JsonNode appNode = objectMapper.readTree(appDetail);
                        
                        // Extract products from credentials
                        List<String> products = new ArrayList<>();
                        JsonNode credentialsNode = appNode.get("credentials");
                        if (credentialsNode != null && credentialsNode.isArray()) {
                            for (JsonNode credential : credentialsNode) {
                                String consumerKey = credential.has("consumerKey") ? credential.get("consumerKey").asText() : null;
                                if (consumerKey != null) {
                                    try {
                                        // Fetch credential details to get associated products
                                        String credDetail = webClient.get()
                                                .uri("/v1/organizations/{org}/developers/{developerId}/apps/{appName}/keys/{key}", 
                                                     org, developerId, appName, consumerKey)
                                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                                .retrieve()
                                                .bodyToMono(String.class)
                                                .block();
                                        
                                        JsonNode credNode = objectMapper.readTree(credDetail);
                                        JsonNode apiProducts = credNode.get("apiProducts");
                                        if (apiProducts != null && apiProducts.isArray()) {
                                            for (JsonNode productNode : apiProducts) {
                                                JsonNode apiProductNode = productNode.get("apiproduct");
                                                if (apiProductNode != null) {
                                                    products.add(apiProductNode.asText());
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.warn("Failed to fetch credential details for key {}: {}", consumerKey, e.getMessage());
                                    }
                                }
                            }
                        }
                        
                        DeveloperAppResponse appResponse = mapToDeveloperAppResponse(appNode);
                        
                        // Override products with the ones we fetched from credentials
                        if (!products.isEmpty()) {
                            appResponse.setProducts(products);
                        }
                        
                        // Set developer info from the request parameter since we already know it
                        appResponse.setDeveloperEmail(developerId);
                        
                        apps.add(appResponse);
                        log.info("Mapped app: {} with {} products, developer: {}", 
                                appResponse.getName(), appResponse.getProducts().size(), developerId);
                    } catch (Exception e) {
                        log.error("Failed to fetch app details for {}: {}", appName, e.getMessage(), e);
                    }
                }
                log.info("Returning {} apps", apps.size());
                return apps;
            }
            
            log.warn("Apps response is not in expected format, returning empty list");
            return Collections.emptyList();
                    
        } catch (Exception e) {
            log.error("Failed to list developer apps", e);
            throw new RuntimeException("Failed to list developer apps", e);
        }
    }
    
    @Override
    public void writeAppAttributes(String org, String appId, Map<String, String> attributes) {
        log.info("RealApigeeGateway: Writing attributes to app: {} in org: {}", appId, org);
        String token = getAccessToken(org);
        
        try {
            // Build attributes array for Apigee API
            List<Map<String, String>> attrList = new ArrayList<>();
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                Map<String, String> attr = new HashMap<>();
                attr.put("name", entry.getKey());
                attr.put("value", entry.getValue());
                attrList.add(attr);
            }
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("attribute", attrList);
            
            // Extract developer email from appId if needed (format: developer-email/app-name)
            // For simplicity, we'll use a PUT to update app attributes
            String[] parts = appId.split("/");
            String developerEmail = parts.length > 1 ? parts[0] : "unknown";
            String appName = parts.length > 1 ? parts[1] : appId;
            
            webClient.post()
                    .uri("/v1/organizations/{org}/developers/{developer}/apps/{app}/attributes", 
                         org, developerEmail, appName)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                    
            log.info("Successfully wrote attributes to app: {}", appId);
            
        } catch (Exception e) {
            log.error("Failed to write app attributes", e);
            throw new RuntimeException("Failed to write app attributes", e);
        }
    }
    
    @Override
    public boolean testConnection(String org, String serviceAccountJson) {
        log.info("RealApigeeGateway: Testing connection for org: {}", org);
        try {
            String token = serviceAccountManager.getAccessToken(serviceAccountJson, saJsonPath);
            webClient.get()
                    .uri("/v1/organizations/{org}", org)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return true;
        } catch (Exception e) {
            log.error("Connection test failed", e);
            return false;
        }
    }
    
    private ApiProductResponse mapToApiProductResponse(JsonNode node) {
        log.debug("Mapping product node: {}", node.toString());
        
        List<String> resources = new ArrayList<>();
        JsonNode resourcesNode = node.get("apiResources");
        if (resourcesNode != null && resourcesNode.isArray()) {
            resourcesNode.forEach(r -> resources.add(r.asText()));
        }
        
        // Also check for 'proxies' field which Apigee uses
        JsonNode proxiesNode = node.get("proxies");
        if (proxiesNode != null && proxiesNode.isArray()) {
            proxiesNode.forEach(p -> resources.add(p.asText()));
        }
        
        String name = node.has("name") ? node.get("name").asText() : "unknown";
        String displayName = node.has("displayName") ? node.get("displayName").asText() : name;
        String quota = node.has("quota") ? node.get("quota").asText() : "N/A";
        
        return ApiProductResponse.builder()
                .name(name)
                .displayName(displayName)
                .quota(quota)
                .resources(resources)
                .build();
    }
    
    private DeveloperResponse mapToDeveloperResponse(JsonNode node) {
        Map<String, String> attributes = new HashMap<>();
        JsonNode attrsNode = node.get("attributes");
        if (attrsNode != null && attrsNode.isArray()) {
            attrsNode.forEach(attr -> {
                String name = attr.get("name").asText();
                String value = attr.get("value").asText();
                attributes.put(name, value);
            });
        }
        
        return DeveloperResponse.builder()
                .id(node.get("developerId").asText())
                .email(node.get("email").asText())
                .attributes(attributes)
                .build();
    }
    
    private DeveloperAppResponse mapToDeveloperAppResponse(JsonNode node) {
        log.info("Mapping app node: {}", node.toString());
        
        List<String> products = new ArrayList<>();
        
        // Extract products from credentials
        JsonNode credentialsNode = node.get("credentials");
        if (credentialsNode != null && credentialsNode.isArray() && credentialsNode.size() > 0) {
            for (JsonNode credential : credentialsNode) {
                JsonNode apiProducts = credential.get("apiProducts");
                if (apiProducts != null && apiProducts.isArray()) {
                    for (JsonNode productNode : apiProducts) {
                        JsonNode apiProductNode = productNode.get("apiproduct");
                        if (apiProductNode != null) {
                            products.add(apiProductNode.asText());
                        }
                    }
                }
            }
        }
        
        // Extract attributes
        Map<String, String> attributes = new HashMap<>();
        JsonNode attrsNode = node.get("attributes");
        if (attrsNode != null && attrsNode.isArray()) {
            for (JsonNode attr : attrsNode) {
                if (attr.has("name") && attr.has("value")) {
                    String name = attr.get("name").asText();
                    String value = attr.get("value").asText();
                    attributes.put(name, value);
                }
            }
        }
        
        // Extract developer info
        String developerId = node.has("developerId") ? node.get("developerId").asText() : null;
        String developerEmail = node.has("developerEmail") ? node.get("developerEmail").asText() : null;
        
        // Also check in appFamily field which sometimes contains developer email
        if (developerEmail == null && node.has("appFamily")) {
            developerEmail = node.get("appFamily").asText();
        }
        
        log.info("Mapped app with {} products", products.size());
        
        return DeveloperAppResponse.builder()
                .appId(node.has("appId") ? node.get("appId").asText() : null)
                .name(node.has("name") ? node.get("name").asText() : "unknown")
                .developerId(developerId)
                .developerEmail(developerEmail)
                .products(products)
                .attributes(attributes)
                .build();
    }
    
    @Override
    public List<ApigeeCustomer> fetchDevelopers(String org, String env) {
        log.info("RealApigeeGateway: Fetching developers from Apigee for org: {}", org);
        String token = getAccessToken(org);
        
        try {
            String response = webClient.get()
                    .uri("/v1/organizations/{org}/developers", org)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            log.info("Developers response: {}", response);
            JsonNode root = objectMapper.readTree(response);
            
            JsonNode developersNode = root.get("developer");
            if (developersNode != null && developersNode.isArray()) {
                List<ApigeeCustomer> customers = new ArrayList<>();
                for (JsonNode devObj : developersNode) {
                    try {
                        JsonNode emailNode = devObj.get("email");
                        if (emailNode == null) continue;
                        
                        String email = emailNode.asText();
                        log.info("Fetching details for developer: {}", email);
                        
                        String devDetail = webClient.get()
                                .uri("/v1/organizations/{org}/developers/{email}", org, email)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .retrieve()
                                .bodyToMono(String.class)
                                .block();
                        
                        JsonNode devNode = objectMapper.readTree(devDetail);
                        
                        String developerId = devNode.has("developerId") ? devNode.get("developerId").asText() : email;
                        String firstName = devNode.has("firstName") ? devNode.get("firstName").asText() : "";
                        String lastName = devNode.has("lastName") ? devNode.get("lastName").asText() : "";
                        String userName = devNode.has("userName") ? devNode.get("userName").asText() : email;
                        String status = devNode.has("status") ? devNode.get("status").asText() : "active";
                        
                        // Extract organization name from attributes
                        String organizationName = "";
                        JsonNode attrsNode = devNode.get("attributes");
                        if (attrsNode != null && attrsNode.isArray()) {
                            for (JsonNode attr : attrsNode) {
                                if (attr.has("name") && "company".equals(attr.get("name").asText())) {
                                    organizationName = attr.get("value").asText();
                                    break;
                                }
                            }
                        }
                        
                        customers.add(ApigeeCustomer.builder()
                                .developerId(developerId)
                                .email(email)
                                .firstName(firstName)
                                .lastName(lastName)
                                .userName(userName)
                                .organizationName(organizationName)
                                .status(status)
                                .build());
                        
                    } catch (Exception e) {
                        log.error("Failed to fetch developer details", e);
                    }
                }
                log.info("Returning {} developers", customers.size());
                return customers;
            }
            
            return Collections.emptyList();
            
        } catch (Exception e) {
            log.error("Failed to fetch developers from Apigee: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch developers from Apigee", e);
        }
    }
}
