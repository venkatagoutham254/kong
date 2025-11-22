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

@Service
@Slf4j
public class AforoProductService {
    
    @Value("${aforo.product.service.url:http://3.208.93.68:8080}")
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
            // Build request WITHOUT productType (catalog service doesn't accept it)
            ProductImportRequest request = ProductImportRequest.builder()
                .productName(apigeeProduct.getDisplayName() != null 
                    ? apigeeProduct.getDisplayName() 
                    : apigeeProduct.getName())
                .productDescription("Imported from Apigee")
                .source("APIGEE")
                .externalId(apigeeProduct.getName())
                .internalSkuCode("APIGEE-" + apigeeProduct.getName())
                // Do NOT set productType - catalog service doesn't accept it
                .build();
            
            log.info("Import request: productName={}, externalId={}, productType={}", 
                request.getProductName(), request.getExternalId(), request.getProductType());
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Organization-Id", organizationId.toString());
            
            // Get JWT token EXACTLY like Kong does
            String authHeader = null;
            try {
                ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
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
            
            HttpEntity<ProductImportRequest> entity = new HttpEntity<>(request, headers);
            
            // Call Aforo import endpoint
            String url = productServiceUrl + "/api/products/import";
            
            log.info("===== CALLING CATALOG =====");
            log.info("URL: {}", url);
            log.info("Auth header present: {}", headers.containsKey("Authorization"));
            log.info("Body: productName={}, externalId={}", request.getProductName(), request.getExternalId());
            
            ResponseEntity<ProductImportResponse> response = restTemplate.postForEntity(
                url,
                entity,
                ProductImportResponse.class
            );
            
            ProductImportResponse result = response.getBody();
            
            log.info("✅ Successfully pushed product {} with type {} to Aforo. Status: {}, Product ID: {}", 
                     apigeeProduct.getName(), 
                     productType,
                     result != null ? result.getStatus() : "UNKNOWN", 
                     result != null ? result.getProductId() : "N/A");
            
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
