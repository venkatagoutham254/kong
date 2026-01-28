package com.aforo.apigee.service;

import com.aforo.apigee.dto.ProductType;
import com.aforo.apigee.dto.request.ProductImportRequest;
import com.aforo.apigee.dto.response.ApiProductResponse;
import com.aforo.apigee.dto.response.ProductImportResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AforoProductService {
    
    @Value("${aforo.product.service.url:http://product.dev.aforo.space:8080}")
    private String productServiceUrl;
    
    private final RestTemplate restTemplate;
    
    public AforoProductService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Get JWT token from current request context
     */
    private String getJwtTokenFromRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    return authHeader;
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract JWT token from request: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Push a single product to Aforo ProductRatePlanService
     */
    public ProductImportResponse pushProductToAforo(
        ApiProductResponse apigeeProduct, 
        Long organizationId
    ) {
        log.info("Pushing product {} to Aforo ProductRatePlanService", apigeeProduct.getName());
        
        try {
            // Build request
            ProductImportRequest request = ProductImportRequest.builder()
                .productName(apigeeProduct.getDisplayName() != null 
                    ? apigeeProduct.getDisplayName() 
                    : apigeeProduct.getName())
                .productDescription("Imported from Apigee")
                .source("APIGEE")
                .externalId(apigeeProduct.getName())
                .internalSkuCode("APIGEE-" + apigeeProduct.getName())
                .build();
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Organization-Id", organizationId.toString());
            
            HttpEntity<ProductImportRequest> entity = new HttpEntity<>(request, headers);
            
            // Call Aforo import endpoint
            String url = productServiceUrl + "/api/products/import";
            
            ResponseEntity<ProductImportResponse> response = restTemplate.postForEntity(
                url,
                entity,
                ProductImportResponse.class
            );
            
            ProductImportResponse result = response.getBody();
            
            log.info("✅ Successfully pushed product {} to Aforo. Status: {}, Product ID: {}", 
                     apigeeProduct.getName(), 
                     result != null ? result.getStatus() : "UNKNOWN", 
                     result != null ? result.getProductId() : "N/A");
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ Failed to push product {} to Aforo: {}", 
                     apigeeProduct.getName(), 
                     e.getMessage());
            throw new RuntimeException("Failed to push product to Aforo", e);
        }
    }
    
    /**
     * Push a single product to Aforo ProductRatePlanService with specific product type
     */
    public ProductImportResponse pushProductToAforo(
        ApiProductResponse apigeeProduct, 
        ProductType productType,
        Long organizationId
    ) {
        log.info("Pushing product {} to Aforo ProductRatePlanService with type {}", 
                 apigeeProduct.getName(), productType);
        
        try {
            // Build request using Map EXACTLY like Kong does
            Map<String, Object> importRequest = new HashMap<>();
            importRequest.put("productName", apigeeProduct.getDisplayName() != null 
                ? apigeeProduct.getDisplayName() 
                : apigeeProduct.getName());
            importRequest.put("productDescription", "Imported from Apigee");
            importRequest.put("source", "APIGEE");
            importRequest.put("externalId", apigeeProduct.getName());
            importRequest.put("internalSkuCode", "APIGEE-" + apigeeProduct.getName());
            
            log.info("Import request: productName={}, externalId={}", 
                importRequest.get("productName"), importRequest.get("externalId"));
            
            // Set headers EXACTLY like Kong
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Organization-Id", organizationId.toString());
            
            // Get JWT token EXACTLY like Kong does
            String authHeader = null;
            try {
                org.springframework.web.context.request.ServletRequestAttributes attr = 
                    (org.springframework.web.context.request.ServletRequestAttributes) 
                    org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
                authHeader = attr.getRequest().getHeader("Authorization");
                log.info("Got Authorization header: {}", authHeader != null ? "YES" : "NO");
            } catch (Exception e) {
                log.error("Could not get Authorization header: {}", e.getMessage());
            }
            
            if (authHeader != null) {
                headers.set("Authorization", authHeader);
            } else {
                log.error("No Authorization header found in request!");
            }
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(importRequest, headers);
            
            // Call Aforo import endpoint
            String url = productServiceUrl + "/api/products/import";
            
            log.info("===== CALLING CATALOG =====");
            log.info("URL: {}", url);
            log.info("Auth header present: {}", headers.containsKey("Authorization"));
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                url,
                entity,
                Map.class
            );
            
            Map<String, Object> responseBody = response.getBody();
            
            log.info("✅ Successfully pushed product {} to Aforo. Response: {}", 
                     apigeeProduct.getName(), responseBody);
            
            // Build ProductImportResponse from Map response
            ProductImportResponse result = new ProductImportResponse();
            if (responseBody != null) {
                result.setProductId(responseBody.get("productId") != null ? 
                    ((Number) responseBody.get("productId")).longValue() : null);
                result.setProductName((String) responseBody.get("productName"));
                result.setStatus((String) responseBody.get("status"));
                result.setMessage((String) responseBody.get("message"));
                result.setSource((String) responseBody.get("source"));
                result.setExternalId((String) responseBody.get("externalId"));
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ Failed to push product {} to Aforo: {}", 
                     apigeeProduct.getName(), 
                     e.getMessage());
            log.error("Full error: ", e);
            throw new RuntimeException("Failed to push product to Aforo", e);
        }
    }
}
