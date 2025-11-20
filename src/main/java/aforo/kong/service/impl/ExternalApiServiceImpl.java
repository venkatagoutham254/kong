package aforo.kong.service.impl;

import aforo.kong.dto.ClientApiDetailsDTO;
import aforo.kong.dto.KongProductDTO;
import aforo.kong.dto.KongProductResponse;
import aforo.kong.entity.ClientApiDetails;
import aforo.kong.entity.KongProduct;
import aforo.kong.mapper.KongProductMapper;
import aforo.kong.repository.ClientApiDetailsRepository;
import aforo.kong.repository.KongProductRepository;
import aforo.kong.service.ExternalApiService;
import aforo.kong.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExternalApiServiceImpl implements ExternalApiService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalApiServiceImpl.class);
    
    private final RestTemplate restTemplate;
    private final KongProductRepository productRepository;
    private final KongProductMapper productMapper;
    private final ObjectMapper objectMapper;
    private final ClientApiDetailsRepository clientApiDetailsRepository;
    public ExternalApiServiceImpl(RestTemplate restTemplate,
                                  KongProductRepository productRepository,
                                  KongProductMapper productMapper,ClientApiDetailsRepository clientApiDetailsRepository,
                                  ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.clientApiDetailsRepository = clientApiDetailsRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public KongProductResponse fetchProducts(ClientApiDetailsDTO apiDetails) {
        // Get organization ID from tenant context
        Long organizationId = TenantContext.require();
        logger.info("Fetching products for organization: {}", organizationId);
        
        String url = buildUrl(apiDetails.getBaseUrl(), apiDetails.getEndpoint());

        HttpHeaders headers = new HttpHeaders();
        if (apiDetails.getAuthToken() != null && !apiDetails.getAuthToken().isBlank()) {
            headers.setBearerAuth(apiDetails.getAuthToken());
        }
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            KongProductResponse productResponse =
                    objectMapper.readValue(response.getBody(), KongProductResponse.class);

            if (productResponse != null && productResponse.getData() != null) {
                // Save products to database and collect the saved entities
                List<KongProduct> savedProducts = new ArrayList<>();
                
                productResponse.getData().forEach(dto -> {
                    // Set organization ID in DTO before mapping
                    dto.setOrganizationId(organizationId);
                    
                    // Check if product already exists (upsert logic)
                    Optional<KongProduct> existingProduct = productRepository.findByIdAndOrganizationId(dto.getId(), organizationId);
                    
                    KongProduct product;
                    if (existingProduct.isPresent()) {
                        // Update existing product
                        product = existingProduct.get();
                        product.setName(dto.getName());
                        product.setDescription(dto.getDescription());
                        product.setUpdatedAt(dto.getUpdatedAt());
                        product.setVersionCount(dto.getVersionCount());
                        // Update JSON fields if present
                        try {
                            if (dto.getLabels() != null) {
                                product.setLabels(objectMapper.writeValueAsString(dto.getLabels()));
                            }
                            if (dto.getPublicLabels() != null) {
                                product.setPublicLabelsJson(objectMapper.writeValueAsString(dto.getPublicLabels()));
                            }
                            if (dto.getPortalIds() != null) {
                                product.setPortalIdsJson(objectMapper.writeValueAsString(dto.getPortalIds()));
                            }
                            if (dto.getPortals() != null) {
                                product.setPortalsJson(objectMapper.writeValueAsString(dto.getPortals()));
                            }
                        } catch (Exception ex) {
                            logger.warn("Error serializing JSON fields for product {}: {}", dto.getId(), ex.getMessage());
                        }
                        logger.debug("Updated existing product {} for organization {}", product.getId(), organizationId);
                    } else {
                        // Create new product
                        product = productMapper.toEntity(dto);
                        logger.debug("Created new product {} for organization {}", product.getId(), organizationId);
                    }
                    
                    KongProduct savedProduct = productRepository.save(product);
                    savedProducts.add(savedProduct);
                });
                
                // Convert saved entities back to DTOs (with internal IDs)
                List<KongProductDTO> updatedDtos = savedProducts.stream()
                    .map(productMapper::toDto)
                    .collect(Collectors.toList());
                
                // Update the response with DTOs that have internal IDs
                productResponse.setData(updatedDtos);
                
                // Set organization_id in the main response object
                productResponse.setOrganizationId(organizationId);
            }

            return productResponse;
        } catch (Exception e) {
            logger.error("Error while fetching products for organization {}: {}", organizationId, e.getMessage());
            throw new RuntimeException("Error while fetching products: " + e.getMessage(), e);
        }
    }

// inside ExternalApiServiceImpl

@Override
public List<KongProductResponse> fetchProductsFromDb(Long clientDetailsId) {
    // Get organization ID from tenant context
    Long organizationId = TenantContext.require();
    logger.info("Fetching products from DB for organization: {} using clientDetailsId: {}", organizationId, clientDetailsId);
    
    ClientApiDetails details = clientApiDetailsRepository.findByIdAndOrganizationId(clientDetailsId, organizationId)
        .orElseThrow(() -> new RuntimeException("ClientApiDetails not found for id: " + clientDetailsId + " and organization: " + organizationId));

    String url = buildUrl(details.getBaseUrl(), details.getEndpoint());
    logger.info("Calling Kong URL: {}", url);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(details.getAuthToken());     // PAT in DB must be raw, no "Bearer "
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

    HttpEntity<String> entity = new HttpEntity<>(headers);

    // ---- FIX: wrap readValue in try/catch ----
    KongProductResponse productResponse;
    try {
        logger.info("Making request to Kong URL: {}", url);
        logger.info("Using auth token: {}...", details.getAuthToken().substring(0, Math.min(20, details.getAuthToken().length())));
        
        ResponseEntity<String> resp =
            restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        String body = resp.getBody();
        logger.info("Konnect raw body:\n{}", body);

// Defensive parse: don't bind the whole thing at once
com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(body);

KongProductResponse tmp = new KongProductResponse();
java.util.List<KongProductDTO> items = new java.util.ArrayList<>();

com.fasterxml.jackson.databind.JsonNode data = root.get("data");
if (data != null && data.isArray()) {
    for (com.fasterxml.jackson.databind.JsonNode n : data) {
        KongProductDTO dto = objectMapper.treeToValue(n, KongProductDTO.class);
        items.add(dto);
    }
}
tmp.setData(items);

// keep meta generic (structure can vary)
com.fasterxml.jackson.databind.JsonNode meta = root.get("meta");
if (meta != null && !meta.isNull()) {
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> metaMap =
            objectMapper.convertValue(meta, java.util.Map.class);
    tmp.setMeta(metaMap);
}

productResponse = tmp;

    } catch (Exception e) {
        logger.error("Failed to parse Konnect response. Error: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to parse Konnect response: " + e.getMessage(), e);
    }
    // -----------------------------------------

    if (productResponse.getData() != null) {
        // Save products to database and collect the saved entities
        List<KongProduct> savedProducts = new ArrayList<>();
        
        for (KongProductDTO dto : productResponse.getData()) {
            // Set organization ID in DTO before mapping
            dto.setOrganizationId(organizationId);
            
            // Check if product already exists (upsert logic)
            Optional<KongProduct> existingProduct = productRepository.findByIdAndOrganizationId(dto.getId(), organizationId);
            
            KongProduct product;
            if (existingProduct.isPresent()) {
                // Update existing product
                product = existingProduct.get();
                product.setName(dto.getName());
                product.setDescription(dto.getDescription());
                product.setUpdatedAt(dto.getUpdatedAt());
                product.setVersionCount(dto.getVersionCount());
                // Update JSON fields if present
                try {
                    if (dto.getLabels() != null) {
                        product.setLabels(objectMapper.writeValueAsString(dto.getLabels()));
                    }
                    if (dto.getPublicLabels() != null) {
                        product.setPublicLabelsJson(objectMapper.writeValueAsString(dto.getPublicLabels()));
                    }
                    if (dto.getPortalIds() != null) {
                        product.setPortalIdsJson(objectMapper.writeValueAsString(dto.getPortalIds()));
                    }
                    if (dto.getPortals() != null) {
                        product.setPortalsJson(objectMapper.writeValueAsString(dto.getPortals()));
                    }
                } catch (Exception ex) {
                    logger.warn("Error serializing JSON fields for product {}: {}", dto.getId(), ex.getMessage());
                }
                logger.debug("Updated existing product {} for organization {}", product.getId(), organizationId);
            } else {
                // Create new product
                product = productMapper.toEntity(dto);
                logger.debug("Created new product {} for organization {}", product.getId(), organizationId);
            }
            
            KongProduct savedProduct = productRepository.save(product);
            savedProducts.add(savedProduct);
        }
        
        // Convert saved entities back to DTOs (with internal IDs)
        List<KongProductDTO> updatedDtos = savedProducts.stream()
            .map(productMapper::toDto)
            .collect(Collectors.toList());
        
        // Update the response with DTOs that have internal IDs
        productResponse.setData(updatedDtos);
        
        // Set organization_id in the main response object
        productResponse.setOrganizationId(organizationId);
    }
    return List.of(productResponse);
}

    @Override
    public List<KongProduct> getAllProducts() {
        Long organizationId = TenantContext.require();
        logger.info("Getting all products for organization: {}", organizationId);
        return productRepository.findByOrganizationId(organizationId);
    }

    @Override
    public KongProduct getProductById(String id) {
        Long organizationId = TenantContext.require();
        logger.info("Getting product {} for organization: {}", id, organizationId);
        return productRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new RuntimeException("KongProduct not found for ID: " + id + " in organization: " + organizationId));
    }

    @Override
    public void deleteProductById(String id) {
        Long organizationId = TenantContext.require();
        logger.info("Deleting product {} for organization: {}", id, organizationId);
        
        KongProduct product = productRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new RuntimeException("KongProduct not found for ID: " + id + " in organization: " + organizationId));
        
        productRepository.delete(product);
        logger.info("Successfully deleted product {} for organization: {}", id, organizationId);
    }

    @Override
    public Map<String, Object> importSelectedProducts(List<String> productIds) {
        Long organizationId = TenantContext.require();
        logger.info("Importing {} selected Kong products for organization: {}", productIds.size(), organizationId);
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> importedProducts = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        for (String productId : productIds) {
            try {
                KongProduct kongProduct = productRepository.findByIdAndOrganizationId(productId, organizationId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
                
                // Prepare product data for product/rateplan service
                Map<String, Object> productData = new HashMap<>();
                productData.put("external_id", kongProduct.getId());
                productData.put("name", kongProduct.getName());
                productData.put("description", kongProduct.getDescription());
                productData.put("source", "kong"); // Set source as kong
                productData.put("product_type", "API"); // Auto-set product type as API
                productData.put("organization_id", organizationId);
                
                // TODO: Call product/rateplan service to save the product
                // For now, we'll just collect the data
                importedProducts.add(productData);
                successCount++;
                
                logger.info("Prepared Kong product {} for import", productId);
            } catch (Exception e) {
                logger.error("Failed to import Kong product {}: {}", productId, e.getMessage());
                failureCount++;
            }
        }
        
        result.put("imported_products", importedProducts);
        result.put("success_count", successCount);
        result.put("failure_count", failureCount);
        result.put("source", "kong");
        
        return result;
    }

    private String buildUrl(String baseUrl, String endpoint) {
        if (baseUrl.endsWith("/") && endpoint.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + endpoint;
        } else if (!baseUrl.endsWith("/") && !endpoint.startsWith("/")) {
            return baseUrl + "/" + endpoint;
        }
        return baseUrl + endpoint;
    }
    
}
