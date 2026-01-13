# Apigee Integration - Part 2: Complete Implementation

**Continuation of APIGEE_INTEGRATION_COMPLETE_IMPLEMENTATION.md**

---

## 6. Apigee Client

### 6.1 ApigeeClient.java (Interface)

```java
package aforo.apigee.client;

import java.util.List;
import java.util.Map;

public interface ApigeeClient {
    
    Map<String, Object> testConnection();
    
    List<Map<String, Object>> listApiProxies();
    
    List<Map<String, Object>> listApiProducts();
    
    List<Map<String, Object>> listDevelopers();
    
    List<Map<String, Object>> listDeveloperApps(String developerEmail);
    
    List<Map<String, Object>> listAppKeys(String appId);
    
    Map<String, Object> getAppDetails(String appId);
    
    void attachApiProductToKey(String appId, String consumerKey, String apiProductName);
    
    void detachApiProductFromKey(String appId, String consumerKey, String apiProductName);
}
```

### 6.2 ApigeeXClient.java

```java
package aforo.apigee.client;

import aforo.apigee.exception.ApigeeConnectionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class ApigeeXClient implements ApigeeClient {
    
    private static final Logger logger = LoggerFactory.getLogger(ApigeeXClient.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    private String baseUrl;
    private String orgName;
    private String environment;
    private String authToken;
    
    public ApigeeXClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    public void configure(String baseUrl, String orgName, String environment, String authToken) {
        this.baseUrl = baseUrl;
        this.orgName = orgName;
        this.environment = environment;
        this.authToken = authToken;
    }
    
    @Override
    public Map<String, Object> testConnection() {
        try {
            String url = String.format("%s/v1/organizations/%s", baseUrl, orgName);
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return Map.of("ok", true, "message", "Connected successfully");
            } else {
                return Map.of("ok", false, "message", "Connection failed");
            }
        } catch (Exception e) {
            logger.error("Failed to test Apigee connection", e);
            throw new ApigeeConnectionException("Connection test failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<Map<String, Object>> listApiProxies() {
        try {
            String url = String.format("%s/v1/organizations/%s/apis", baseUrl, orgName);
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                List<Map<String, Object>> proxies = new ArrayList<>();
                
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        Map<String, Object> proxy = new HashMap<>();
                        proxy.put("name", node.asText());
                        proxies.add(proxy);
                    }
                } else if (root.has("proxies")) {
                    for (JsonNode node : root.get("proxies")) {
                        Map<String, Object> proxy = new HashMap<>();
                        proxy.put("name", node.has("name") ? node.get("name").asText() : node.asText());
                        if (node.has("revision")) {
                            proxy.put("revision", node.get("revision").asText());
                        }
                        proxies.add(proxy);
                    }
                }
                
                return proxies;
            }
            
            logger.warn("Failed to list API proxies, status: {}", response.getStatusCode());
            return List.of();
        } catch (Exception e) {
            logger.error("Failed to list API proxies", e);
            return List.of();
        }
    }
    
    @Override
    public List<Map<String, Object>> listApiProducts() {
        try {
            String url = String.format("%s/v1/organizations/%s/apiproducts", baseUrl, orgName);
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                List<Map<String, Object>> products = new ArrayList<>();
                
                if (root.has("apiProduct")) {
                    for (JsonNode node : root.get("apiProduct")) {
                        products.add(parseApiProduct(node));
                    }
                } else if (root.isArray()) {
                    for (JsonNode node : root) {
                        products.add(parseApiProduct(node));
                    }
                }
                
                return products;
            }
            
            logger.warn("Failed to list API products, status: {}", response.getStatusCode());
            return List.of();
        } catch (Exception e) {
            logger.error("Failed to list API products", e);
            return List.of();
        }
    }
    
    @Override
    public List<Map<String, Object>> listDevelopers() {
        try {
            String url = String.format("%s/v1/organizations/%s/developers", baseUrl, orgName);
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                List<Map<String, Object>> developers = new ArrayList<>();
                
                if (root.has("developer")) {
                    for (JsonNode node : root.get("developer")) {
                        developers.add(parseDeveloper(node));
                    }
                } else if (root.isArray()) {
                    for (JsonNode node : root) {
                        developers.add(parseDeveloper(node));
                    }
                }
                
                return developers;
            }
            
            logger.warn("Failed to list developers, status: {}", response.getStatusCode());
            return List.of();
        } catch (Exception e) {
            logger.error("Failed to list developers", e);
            return List.of();
        }
    }
    
    @Override
    public List<Map<String, Object>> listDeveloperApps(String developerEmail) {
        try {
            String url = String.format("%s/v1/organizations/%s/developers/%s/apps", 
                baseUrl, orgName, developerEmail);
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                List<Map<String, Object>> apps = new ArrayList<>();
                
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        apps.add(parseApp(node));
                    }
                } else if (root.has("app")) {
                    for (JsonNode node : root.get("app")) {
                        apps.add(parseApp(node));
                    }
                }
                
                return apps;
            }
            
            logger.warn("Failed to list developer apps, status: {}", response.getStatusCode());
            return List.of();
        } catch (Exception e) {
            logger.error("Failed to list developer apps for: {}", developerEmail, e);
            return List.of();
        }
    }
    
    @Override
    public List<Map<String, Object>> listAppKeys(String appId) {
        try {
            Map<String, Object> appDetails = getAppDetails(appId);
            List<Map<String, Object>> keys = new ArrayList<>();
            
            if (appDetails.containsKey("credentials")) {
                Object credentials = appDetails.get("credentials");
                if (credentials instanceof List) {
                    for (Object cred : (List<?>) credentials) {
                        if (cred instanceof Map) {
                            keys.add((Map<String, Object>) cred);
                        }
                    }
                }
            }
            
            return keys;
        } catch (Exception e) {
            logger.error("Failed to list app keys for: {}", appId, e);
            return List.of();
        }
    }
    
    @Override
    public Map<String, Object> getAppDetails(String appId) {
        try {
            String url = String.format("%s/v1/organizations/%s/apps/%s", baseUrl, orgName, appId);
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                return objectMapper.convertValue(root, Map.class);
            }
            
            return Map.of();
        } catch (Exception e) {
            logger.error("Failed to get app details for: {}", appId, e);
            return Map.of();
        }
    }
    
    @Override
    public void attachApiProductToKey(String appId, String consumerKey, String apiProductName) {
        try {
            String url = String.format("%s/v1/organizations/%s/developers/%s/apps/%s/keys/%s", 
                baseUrl, orgName, "developer-email", appId, consumerKey);
            
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = Map.of("apiProducts", List.of(apiProductName));
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            logger.info("Attached API product {} to key {}", apiProductName, consumerKey);
        } catch (Exception e) {
            logger.error("Failed to attach API product {} to key {}", apiProductName, consumerKey, e);
            throw new ApigeeConnectionException("Failed to attach API product", e);
        }
    }
    
    @Override
    public void detachApiProductFromKey(String appId, String consumerKey, String apiProductName) {
        try {
            String url = String.format("%s/v1/organizations/%s/developers/%s/apps/%s/keys/%s/apiproducts/%s", 
                baseUrl, orgName, "developer-email", appId, consumerKey, apiProductName);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
            logger.info("Detached API product {} from key {}", apiProductName, consumerKey);
        } catch (Exception e) {
            logger.error("Failed to detach API product {} from key {}", apiProductName, consumerKey, e);
            throw new ApigeeConnectionException("Failed to detach API product", e);
        }
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
    
    private Map<String, Object> parseApiProduct(JsonNode node) {
        Map<String, Object> product = new HashMap<>();
        product.put("name", node.has("name") ? node.get("name").asText() : "");
        product.put("displayName", node.has("displayName") ? node.get("displayName").asText() : "");
        
        if (node.has("attributes")) {
            List<String> attrs = new ArrayList<>();
            for (JsonNode attr : node.get("attributes")) {
                attrs.add(attr.asText());
            }
            product.put("attributes", attrs);
        }
        
        return product;
    }
    
    private Map<String, Object> parseDeveloper(JsonNode node) {
        Map<String, Object> developer = new HashMap<>();
        developer.put("email", node.has("email") ? node.get("email").asText() : "");
        developer.put("developerId", node.has("developerId") ? node.get("developerId").asText() : "");
        developer.put("firstName", node.has("firstName") ? node.get("firstName").asText() : "");
        developer.put("lastName", node.has("lastName") ? node.get("lastName").asText() : "");
        return developer;
    }
    
    private Map<String, Object> parseApp(JsonNode node) {
        Map<String, Object> app = new HashMap<>();
        app.put("appId", node.has("appId") ? node.get("appId").asText() : "");
        app.put("name", node.has("name") ? node.get("name").asText() : "");
        app.put("status", node.has("status") ? node.get("status").asText() : "");
        
        if (node.has("credentials")) {
            List<Map<String, Object>> credentials = new ArrayList<>();
            for (JsonNode cred : node.get("credentials")) {
                Map<String, Object> credMap = new HashMap<>();
                credMap.put("consumerKey", cred.has("consumerKey") ? cred.get("consumerKey").asText() : "");
                credMap.put("consumerSecret", cred.has("consumerSecret") ? cred.get("consumerSecret").asText() : "");
                
                if (cred.has("apiProducts")) {
                    List<String> products = new ArrayList<>();
                    for (JsonNode prod : cred.get("apiProducts")) {
                        products.add(prod.has("apiproduct") ? prod.get("apiproduct").asText() : prod.asText());
                    }
                    credMap.put("apiProducts", products);
                }
                
                credentials.add(credMap);
            }
            app.put("credentials", credentials);
        }
        
        return app;
    }
}
```

### 6.3 ApigeeEdgeClient.java

```java
package aforo.apigee.client;

import org.springframework.stereotype.Component;
import java.util.Base64;
import org.springframework.http.HttpHeaders;

@Component
public class ApigeeEdgeClient extends ApigeeXClient {
    
    private String username;
    private String password;
    
    public ApigeeEdgeClient(org.springframework.web.client.RestTemplate restTemplate, 
                           com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }
    
    public void configureBasicAuth(String baseUrl, String orgName, String environment, 
                                   String username, String password) {
        this.username = username;
        this.password = password;
        super.configure(baseUrl, orgName, environment, createBasicAuthToken());
    }
    
    private String createBasicAuthToken() {
        String auth = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }
}
```

### 6.4 ApigeeClientFactory.java

```java
package aforo.apigee.client;

import aforo.apigee.model.ApigeeConnection;
import aforo.apigee.model.enums.ApigeeType;
import aforo.apigee.model.enums.AuthType;
import aforo.apigee.security.SecretVault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ApigeeClientFactory {
    
    @Autowired
    private ApigeeXClient apigeeXClient;
    
    @Autowired
    private ApigeeEdgeClient apigeeEdgeClient;
    
    @Autowired
    private SecretVault secretVault;
    
    public ApigeeClient createClient(ApigeeConnection connection) {
        String credentials = secretVault.loadSecret(connection.getTenantId(), connection.getEncryptedSecretRef());
        
        if (connection.getApigeeType() == ApigeeType.APIGEE_EDGE && 
            connection.getAuthType() == AuthType.BASIC) {
            
            String[] parts = credentials.split(":");
            if (parts.length == 2) {
                apigeeEdgeClient.configureBasicAuth(
                    connection.getManagementBaseUrl(),
                    connection.getOrgName(),
                    connection.getEnvironment(),
                    parts[0],
                    parts[1]
                );
                return apigeeEdgeClient;
            }
        }
        
        apigeeXClient.configure(
            connection.getManagementBaseUrl(),
            connection.getOrgName(),
            connection.getEnvironment(),
            credentials
        );
        
        return apigeeXClient;
    }
}
```

---

## 7. Services

### 7.1 ApigeeConnectionService.java

```java
package aforo.apigee.service;

import aforo.apigee.dto.request.ConnectApigeeRequest;
import aforo.apigee.dto.response.ConnectApigeeResponse;

public interface ApigeeConnectionService {
    ConnectApigeeResponse connect(Long tenantId, ConnectApigeeRequest request);
    void testConnection(Long tenantId);
}
```

### 7.2 ApigeeConnectionServiceImpl.java

```java
package aforo.apigee.service.impl;

import aforo.apigee.client.ApigeeClient;
import aforo.apigee.client.ApigeeClientFactory;
import aforo.apigee.dto.request.ConnectApigeeRequest;
import aforo.apigee.dto.response.ConnectApigeeResponse;
import aforo.apigee.exception.ApigeeConnectionException;
import aforo.apigee.model.ApigeeConnection;
import aforo.apigee.model.enums.ConnectionStatus;
import aforo.apigee.repository.ApigeeConnectionRepository;
import aforo.apigee.security.SecretVault;
import aforo.apigee.service.ApigeeConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class ApigeeConnectionServiceImpl implements ApigeeConnectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApigeeConnectionServiceImpl.class);
    
    @Autowired
    private ApigeeConnectionRepository connectionRepository;
    
    @Autowired
    private SecretVault secretVault;
    
    @Autowired
    private ApigeeClientFactory clientFactory;
    
    @Override
    @Transactional
    public ConnectApigeeResponse connect(Long tenantId, ConnectApigeeRequest request) {
        logger.info("Connecting to Apigee for tenant: {}", tenantId);
        
        try {
            String secretRef = secretVault.saveSecret(tenantId, request.getCredentials());
            
            ApigeeConnection connection = connectionRepository.findByTenantId(tenantId)
                .orElse(new ApigeeConnection());
            
            connection.setTenantId(tenantId);
            connection.setApigeeType(request.getApigeeType());
            connection.setOrgName(request.getOrgName());
            connection.setEnvironment(request.getEnvironment());
            connection.setManagementBaseUrl(request.getManagementBaseUrl());
            connection.setAuthType(request.getAuthType());
            connection.setEncryptedSecretRef(secretRef);
            connection.setStatus(ConnectionStatus.DISCONNECTED);
            
            connection = connectionRepository.save(connection);
            
            ApigeeClient client = clientFactory.createClient(connection);
            Map<String, Object> testResult = client.testConnection();
            
            if (Boolean.TRUE.equals(testResult.get("ok"))) {
                connection.setStatus(ConnectionStatus.CONNECTED);
                connection.setLastTestedAt(Instant.now());
                connectionRepository.save(connection);
                
                int proxiesCount = client.listApiProxies().size();
                int productsCount = client.listApiProducts().size();
                int developersCount = client.listDevelopers().size();
                
                return ConnectApigeeResponse.builder()
                    .connectionId(connection.getId())
                    .status("CONNECTED")
                    .proxiesCount(proxiesCount)
                    .apiProductsCount(productsCount)
                    .developersCount(developersCount)
                    .appsCount(0)
                    .message("Connected successfully")
                    .build();
            } else {
                connection.setStatus(ConnectionStatus.ERROR);
                connectionRepository.save(connection);
                throw new ApigeeConnectionException("Connection test failed");
            }
            
        } catch (Exception e) {
            logger.error("Failed to connect to Apigee for tenant: {}", tenantId, e);
            throw new ApigeeConnectionException("Failed to connect: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void testConnection(Long tenantId) {
        ApigeeConnection connection = connectionRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new ApigeeConnectionException("Connection not found"));
        
        ApigeeClient client = clientFactory.createClient(connection);
        Map<String, Object> result = client.testConnection();
        
        if (!Boolean.TRUE.equals(result.get("ok"))) {
            throw new ApigeeConnectionException("Connection test failed");
        }
        
        connection.setLastTestedAt(Instant.now());
        connection.setStatus(ConnectionStatus.CONNECTED);
        connectionRepository.save(connection);
    }
}
```

### 7.3 ApigeeCatalogSyncService.java

```java
package aforo.apigee.service;

import aforo.apigee.dto.request.CatalogSyncRequest;
import aforo.apigee.dto.response.SyncDiffResponse;

public interface ApigeeCatalogSyncService {
    SyncDiffResponse syncCatalog(Long tenantId, CatalogSyncRequest request);
}
```

### 7.4 ApigeeCatalogSyncServiceImpl.java

```java
package aforo.apigee.service.impl;

import aforo.apigee.client.ApigeeClient;
import aforo.apigee.client.ApigeeClientFactory;
import aforo.apigee.dto.request.CatalogSyncRequest;
import aforo.apigee.dto.response.SyncDiffResponse;
import aforo.apigee.exception.ApigeeSyncException;
import aforo.apigee.model.ApigeeConnection;
import aforo.apigee.model.ApigeeProduct;
import aforo.apigee.model.ApigeeSyncAudit;
import aforo.apigee.model.enums.EntityStatus;
import aforo.apigee.model.enums.ProductKind;
import aforo.apigee.model.enums.SyncScope;
import aforo.apigee.model.enums.SyncSource;
import aforo.apigee.repository.ApigeeConnectionRepository;
import aforo.apigee.repository.ApigeeProductRepository;
import aforo.apigee.repository.ApigeeSyncAuditRepository;
import aforo.apigee.service.ApigeeCatalogSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApigeeCatalogSyncServiceImpl implements ApigeeCatalogSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApigeeCatalogSyncServiceImpl.class);
    
    @Autowired
    private ApigeeConnectionRepository connectionRepository;
    
    @Autowired
    private ApigeeProductRepository productRepository;
    
    @Autowired
    private ApigeeSyncAuditRepository syncAuditRepository;
    
    @Autowired
    private ApigeeClientFactory clientFactory;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public SyncDiffResponse syncCatalog(Long tenantId, CatalogSyncRequest request) {
        logger.info("Starting catalog sync for tenant: {}, dryRun: {}", tenantId, request.getDryRun());
        
        try {
            ApigeeConnection connection = connectionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ApigeeSyncException("Connection not found"));
            
            ApigeeClient client = clientFactory.createClient(connection);
            
            List<ApigeeProduct> existingProducts = productRepository.findByTenantId(tenantId);
            Map<String, ApigeeProduct> existingMap = existingProducts.stream()
                .collect(Collectors.toMap(p -> buildProductKey(p), p -> p));
            
            List<SyncDiffResponse.ProductDiff> added = new ArrayList<>();
            List<SyncDiffResponse.ProductDiff> changed = new ArrayList<>();
            List<SyncDiffResponse.ProductDiff> removed = new ArrayList<>();
            
            Set<String> seenKeys = new HashSet<>();
            
            if (Boolean.TRUE.equals(request.getSyncProxies())) {
                List<Map<String, Object>> proxies = client.listApiProxies();
                for (Map<String, Object> proxy : proxies) {
                    String name = (String) proxy.get("name");
                    String key = buildKey(ProductKind.PROXY, name, null);
                    seenKeys.add(key);
                    
                    if (!existingMap.containsKey(key)) {
                        added.add(SyncDiffResponse.ProductDiff.builder()
                            .name(name)
                            .kind("PROXY")
                            .changeDescription("New proxy discovered")
                            .build());
                        
                        if (!request.getDryRun()) {
                            ApigeeProduct product = new ApigeeProduct();
                            product.setTenantId(tenantId);
                            product.setKind(ProductKind.PROXY);
                            product.setApigeeProxyName(name);
                            product.setDisplayName(name);
                            product.setStatus(EntityStatus.ACTIVE);
                            product.setLastSyncedAt(Instant.now());
                            productRepository.save(product);
                        }
                    }
                }
            }
            
            if (Boolean.TRUE.equals(request.getSyncApiProducts())) {
                List<Map<String, Object>> apiProducts = client.listApiProducts();
                for (Map<String, Object> apiProduct : apiProducts) {
                    String name = (String) apiProduct.get("name");
                    String key = buildKey(ProductKind.API_PRODUCT, null, name);
                    seenKeys.add(key);
                    
                    if (!existingMap.containsKey(key)) {
                        added.add(SyncDiffResponse.ProductDiff.builder()
                            .name(name)
                            .kind("API_PRODUCT")
                            .changeDescription("New API product discovered")
                            .build());
                        
                        if (!request.getDryRun()) {
                            ApigeeProduct product = new ApigeeProduct();
                            product.setTenantId(tenantId);
                            product.setKind(ProductKind.API_PRODUCT);
                            product.setApigeeApiProductName(name);
                            product.setDisplayName((String) apiProduct.getOrDefault("displayName", name));
                            product.setStatus(EntityStatus.ACTIVE);
                            product.setLastSyncedAt(Instant.now());
                            productRepository.save(product);
                        }
                    }
                }
            }
            
            for (Map.Entry<String, ApigeeProduct> entry : existingMap.entrySet()) {
                if (!seenKeys.contains(entry.getKey()) && entry.getValue().getStatus() == EntityStatus.ACTIVE) {
                    removed.add(SyncDiffResponse.ProductDiff.builder()
                        .name(entry.getValue().getDisplayName())
                        .kind(entry.getValue().getKind().name())
                        .changeDescription("Product no longer exists in Apigee")
                        .build());
                    
                    if (!request.getDryRun()) {
                        ApigeeProduct product = entry.getValue();
                        product.setStatus(EntityStatus.DISABLED);
                        productRepository.save(product);
                    }
                }
            }
            
            ApigeeSyncAudit audit = new ApigeeSyncAudit();
            audit.setTenantId(tenantId);
            audit.setScope(SyncScope.CATALOG);
            audit.setSource(SyncSource.MANUAL);
            audit.setDryRun(request.getDryRun());
            audit.setStatus("SUCCESS");
            
            try {
                Map<String, Object> diffSummary = Map.of(
                    "added", added.size(),
                    "removed", removed.size(),
                    "changed", changed.size()
                );
                audit.setDiffSummary(objectMapper.writeValueAsString(diffSummary));
                
                if (!request.getDryRun()) {
                    audit.setAppliedSummary(objectMapper.writeValueAsString(diffSummary));
                }
            } catch (Exception e) {
                logger.error("Failed to serialize diff summary", e);
            }
            
            syncAuditRepository.save(audit);
            
            return SyncDiffResponse.builder()
                .added(added)
                .removed(removed)
                .changed(changed)
                .totalAdded(added.size())
                .totalRemoved(removed.size())
                .totalChanged(changed.size())
                .dryRun(request.getDryRun())
                .build();
            
        } catch (Exception e) {
            logger.error("Failed to sync catalog for tenant: {}", tenantId, e);
            throw new ApigeeSyncException("Catalog sync failed: " + e.getMessage(), e);
        }
    }
    
    private String buildProductKey(ApigeeProduct product) {
        return buildKey(product.getKind(), product.getApigeeProxyName(), product.getApigeeApiProductName());
    }
    
    private String buildKey(ProductKind kind, String proxyName, String apiProductName) {
        return kind + ":" + (proxyName != null ? proxyName : apiProductName);
    }
}
```

### 7.5 ApigeeCustomerSyncService.java

```java
package aforo.apigee.service;

import aforo.apigee.dto.request.CustomersSyncRequest;
import aforo.apigee.dto.response.CustomersSyncDiffResponse;

public interface ApigeeCustomerSyncService {
    CustomersSyncDiffResponse syncCustomers(Long tenantId, CustomersSyncRequest request);
}
```

### 7.6 ApigeeCustomerSyncServiceImpl.java

```java
package aforo.apigee.service.impl;

import aforo.apigee.client.ApigeeClient;
import aforo.apigee.client.ApigeeClientFactory;
import aforo.apigee.dto.request.CustomersSyncRequest;
import aforo.apigee.dto.response.CustomersSyncDiffResponse;
import aforo.apigee.exception.ApigeeSyncException;
import aforo.apigee.model.ApigeeConnection;
import aforo.apigee.model.ApigeeCustomer;
import aforo.apigee.model.ApigeeSyncAudit;
import aforo.apigee.model.enums.EntityStatus;
import aforo.apigee.model.enums.SyncScope;
import aforo.apigee.model.enums.SyncSource;
import aforo.apigee.repository.ApigeeConnectionRepository;
import aforo.apigee.repository.ApigeeCustomerRepository;
import aforo.apigee.repository.ApigeeSyncAuditRepository;
import aforo.apigee.service.ApigeeCustomerSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApigeeCustomerSyncServiceImpl implements ApigeeCustomerSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApigeeCustomerSyncServiceImpl.class);
    
    @Autowired
    private ApigeeConnectionRepository connectionRepository;
    
    @Autowired
    private ApigeeCustomerRepository customerRepository;
    
    @Autowired
    private ApigeeSyncAuditRepository syncAuditRepository;
    
    @Autowired
    private ApigeeClientFactory clientFactory;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public CustomersSyncDiffResponse syncCustomers(Long tenantId, CustomersSyncRequest request) {
        logger.info("Starting customer sync for tenant: {}, dryRun: {}", tenantId, request.getDryRun());
        
        try {
            ApigeeConnection connection = connectionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ApigeeSyncException("Connection not found"));
            
            ApigeeClient client = clientFactory.createClient(connection);
            
            List<ApigeeCustomer> existingCustomers = customerRepository.findByTenantId(tenantId);
            Map<String, ApigeeCustomer> existingMap = existingCustomers.stream()
                .collect(Collectors.toMap(c -> buildCustomerKey(c), c -> c));
            
            List<CustomersSyncDiffResponse.CustomerDiff> added = new ArrayList<>();
            List<CustomersSyncDiffResponse.CustomerDiff> changed = new ArrayList<>();
            List<CustomersSyncDiffResponse.CustomerDiff> removed = new ArrayList<>();
            
            Set<String> seenKeys = new HashSet<>();
            
            List<Map<String, Object>> developers = client.listDevelopers();
            
            for (Map<String, Object> developer : developers) {
                String developerEmail = (String) developer.get("email");
                String developerId = (String) developer.get("developerId");
                
                List<Map<String, Object>> apps = client.listDeveloperApps(developerEmail);
                
                for (Map<String, Object> app : apps) {
                    String appId = (String) app.get("appId");
                    String appName = (String) app.get("name");
                    
                    Object credentialsObj = app.get("credentials");
                    if (credentialsObj instanceof List) {
                        List<Map<String, Object>> credentials = (List<Map<String, Object>>) credentialsObj;
                        
                        for (Map<String, Object> cred : credentials) {
                            String consumerKey = (String) cred.get("consumerKey");
                            String key = buildKey(appId, consumerKey);
                            seenKeys.add(key);
                            
                            if (!existingMap.containsKey(key)) {
                                added.add(CustomersSyncDiffResponse.CustomerDiff.builder()
                                    .developerEmail(developerEmail)
                                    .appName(appName)
                                    .consumerKey(consumerKey)
                                    .changeDescription("New customer discovered")
                                    .build());
                                
                                if (!request.getDryRun()) {
                                    ApigeeCustomer customer = new ApigeeCustomer();
                                    customer.setTenantId(tenantId);
                                    customer.setDeveloperEmail(developerEmail);
                                    customer.setDeveloperId(developerId);
                                    customer.setAppName(appName);
                                    customer.setAppId(appId);
                                    customer.setConsumerKey(consumerKey);
                                    customer.setStatus(EntityStatus.ACTIVE);
                                    customer.setSuspended(false);
                                    customer.setLastSeenAt(Instant.now());
                                    
                                    Object apiProductsObj = cred.get("apiProducts");
                                    if (apiProductsObj != null) {
                                        try {
                                            customer.setApiProducts(objectMapper.writeValueAsString(apiProductsObj));
                                        } catch (Exception e) {
                                            logger.error("Failed to serialize API products", e);
                                        }
                                    }
                                    
                                    customerRepository.save(customer);
                                }
                            }
                        }
                    }
                }
            }
            
            for (Map.Entry<String, ApigeeCustomer> entry : existingMap.entrySet()) {
                if (!seenKeys.contains(entry.getKey()) && entry.getValue().getStatus() == EntityStatus.ACTIVE) {
                    removed.add(CustomersSyncDiffResponse.CustomerDiff.builder()
                        .developerEmail(entry.getValue().getDeveloperEmail())
                        .appName(entry.getValue().getAppName())
                        .consumerKey(entry.getValue().getConsumerKey())
                        .changeDescription("Customer no longer exists in Apigee")
                        .build());
                    
                    if (!request.getDryRun()) {
                        ApigeeCustomer customer = entry.getValue();
                        customer.setStatus(EntityStatus.DISABLED);
                        customerRepository.save(customer);
                    }
                }
            }
            
            ApigeeSyncAudit audit = new ApigeeSyncAudit();
            audit.setTenantId(tenantId);
            audit.setScope(SyncScope.CUSTOMERS);
            audit.setSource(SyncSource.MANUAL);
            audit.setDryRun(request.getDryRun());
            audit.setStatus("SUCCESS");
            
            try {
                Map<String, Object> diffSummary = Map.of(
                    "added", added.size(),
                    "removed", removed.size(),
                    "changed", changed.size()
                );
                audit.setDiffSummary(objectMapper.writeValueAsString(diffSummary));
                
                if (!request.getDryRun()) {
                    audit.setAppliedSummary(objectMapper.writeValueAsString(diffSummary));
                }
            } catch (Exception e) {
                logger.error("Failed to serialize diff summary", e);
            }
            
            syncAuditRepository.save(audit);
            
            return CustomersSyncDiffResponse.builder()
                .added(added)
                .removed(removed)
                .changed(changed)
                .totalAdded(added.size())
                .totalRemoved(removed.size())
                .totalChanged(changed.size())
                .dryRun(request.getDryRun())
                .build();
            
        } catch (Exception e) {
            logger.error("Failed to sync customers for tenant: {}", tenantId, e);
            throw new ApigeeSyncException("Customer sync failed: " + e.getMessage(), e);
        }
    }
    
    private String buildCustomerKey(ApigeeCustomer customer) {
        return buildKey(customer.getAppId(), customer.getConsumerKey());
    }
    
    private String buildKey(String appId, String consumerKey) {
        return appId + ":" + consumerKey;
    }
}
```

*Continuing in next message with remaining services, controller, scheduler, Liquibase, tests, and sample commands...*
