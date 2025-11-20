package aforo.kong.controller;

import com.aforo.apigee.dto.request.*;
import com.aforo.apigee.dto.response.*;
import com.aforo.apigee.security.TenantContext;
import com.aforo.apigee.service.AforoProductService;
import com.aforo.apigee.service.AuthorizeService;
import com.aforo.apigee.service.InventoryService;
import com.aforo.apigee.service.MappingService;
import com.aforo.apigee.service.UsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/integrations/apigee")
@RequiredArgsConstructor
@Tag(name = "Apigee Integration", description = "Apigee integration endpoints")
public class ApigeeIntegrationController {
    
    private final InventoryService inventoryService;
    private final MappingService mappingService;
    private final AuthorizeService authorizeService;
    private final UsageService usageService;
    private final AforoProductService aforoProductService;
    
    @PostMapping(value = "/connections", consumes = "multipart/form-data")
    @Operation(summary = "Save and test Apigee connection with file upload")
    public ResponseEntity<ConnectionResponse> saveConnection(
            @RequestParam("org") String org,
            @RequestParam("envs") String envs,
            @RequestParam("analyticsMode") String analyticsMode,
            @RequestParam("serviceAccountFile") org.springframework.web.multipart.MultipartFile serviceAccountFile) {
        log.info("Received connection request for org: {}", org);
        
        try {
            // Read file content
            String serviceAccountJson = new String(serviceAccountFile.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            
            // Create request object
            SaveConnectionRequest request = SaveConnectionRequest.builder()
                    .org(org)
                    .envs(envs)
                    .analyticsMode(analyticsMode)
                    .serviceAccountJson(serviceAccountJson)
                    .build();
            
            ConnectionResponse response = inventoryService.saveAndTestConnection(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error reading service account file", e);
            return ResponseEntity.ok(ConnectionResponse.builder()
                    .connected(false)
                    .message("Error reading service account file: " + e.getMessage())
                    .build());
        }
    }
    
    @GetMapping("/products")
    @Operation(summary = "List Apigee API Products")
    public ResponseEntity<List<ApiProductResponse>> listProducts(@RequestParam(required = false) String org) {
        log.info("Listing API products for org: {}", org);
        List<ApiProductResponse> products = inventoryService.getApiProducts(org);
        return ResponseEntity.ok(products);
    }
    
    @PostMapping("/products/import-selected")
    @Operation(summary = "Import selected products with assigned types", 
               description = "Import only selected Apigee products with their assigned product types. Organization ID is extracted from JWT token.")
    public ResponseEntity<SelectiveImportResponse> importSelectedProducts(
            @Valid @RequestBody SelectiveProductImportRequest request) {
        // Get organization ID from JWT token (set by JwtTenantFilter)
        Long organizationId = TenantContext.getTenantId();
        // For testing without JWT, use a default organization ID
        if (organizationId == null) {
            organizationId = 1L; // Default for testing
            log.warn("No organization ID found in JWT context, using default: {}", organizationId);
        }
        log.info("Starting selective product import for {} products for organization {}", 
                 request.getSelectedProducts().size(), organizationId);
        
        SelectiveImportResponse.SelectiveImportResponseBuilder responseBuilder = SelectiveImportResponse.builder();
        List<SelectiveImportResponse.ImportedProductDetail> importedProducts = new ArrayList<>();
        
        int successCount = 0;
        int failCount = 0;
        
        for (SelectiveProductImportRequest.SelectedProduct selectedProduct : request.getSelectedProducts()) {
            try {
                // Create ApiProductResponse from selected product
                // Note: quota and resources are not needed for import, only name and displayName
                ApiProductResponse apiProduct = ApiProductResponse.builder()
                    .name(selectedProduct.getProductName())
                    .displayName(selectedProduct.getDisplayName())
                    .build();
                
                // Push to Aforo with the selected product type
                ProductImportResponse response = aforoProductService.pushProductToAforo(
                    apiProduct,
                    selectedProduct.getProductType(),
                    organizationId
                );
                
                importedProducts.add(SelectiveImportResponse.ImportedProductDetail.builder()
                    .productName(selectedProduct.getProductName())
                    .productType(selectedProduct.getProductType().toString())
                    .status("SUCCESS")
                    .message(response.getMessage())
                    .productId(response.getProductId())
                    .build());
                
                successCount++;
                
            } catch (Exception e) {
                log.error("Failed to import product {}: {}", selectedProduct.getProductName(), e.getMessage());
                
                importedProducts.add(SelectiveImportResponse.ImportedProductDetail.builder()
                    .productName(selectedProduct.getProductName())
                    .productType(selectedProduct.getProductType().toString())
                    .status("FAILED")
                    .message(e.getMessage())
                    .build());
                
                failCount++;
            }
        }
        
        SelectiveImportResponse response = responseBuilder
            .totalSelected(request.getSelectedProducts().size())
            .successfullyImported(successCount)
            .failed(failCount)
            .message(String.format("Import completed: %d successful, %d failed out of %d selected", 
                                  successCount, failCount, request.getSelectedProducts().size()))
            .importedProducts(importedProducts)
            .build();
        
        log.info("Selective import completed: {} successful, {} failed", successCount, failCount);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/sync")
    @Operation(summary = "Sync products from Apigee to ProductRatePlanService", 
               description = "Fetches products from Apigee and automatically pushes them to ProductRatePlanService. Organization ID is extracted from JWT token.")
    public ResponseEntity<SyncResponse> syncProductsToAforo(
            @RequestParam(required = false) String org) {
        // Get organization ID from JWT token
        Long organizationId = TenantContext.getTenantId();
        log.info("Starting product sync from Apigee to Aforo for organization: {}", organizationId);
        
        try {
            // 1. Fetch products from Apigee
            List<ApiProductResponse> apigeeProducts = inventoryService.getApiProducts(org);
            log.info("Fetched {} products from Apigee", apigeeProducts.size());
            
            int created = 0;
            int updated = 0;
            int failed = 0;
            
            // 2. Push each product to Aforo
            for (ApiProductResponse product : apigeeProducts) {
                try {
                    ProductImportResponse response = aforoProductService.pushProductToAforo(
                        product, 
                        organizationId
                    );
                    
                    if ("CREATED".equals(response.getStatus())) {
                        created++;
                    } else if ("UPDATED".equals(response.getStatus())) {
                        updated++;
                    }
                    
                } catch (Exception e) {
                    log.error("Failed to sync product {}: {}", product.getName(), e.getMessage());
                    failed++;
                }
            }
            
            // 3. Build response
            SyncResponse syncResponse = SyncResponse.builder()
                .productsImported(created)
                .productsUpdated(updated)
                .totalSynced(created + updated)
                .failed(failed)
                .message(String.format("Sync completed: %d created, %d updated, %d failed", 
                                      created, updated, failed))
                .build();
            
            log.info("Sync completed: {} created, {} updated, {} failed", created, updated, failed);
            
            return ResponseEntity.ok(syncResponse);
            
        } catch (Exception e) {
            log.error("Sync failed: {}", e.getMessage());
            
            SyncResponse errorResponse = SyncResponse.builder()
                .productsImported(0)
                .productsUpdated(0)
                .totalSynced(0)
                .failed(0)
                .message("Sync failed: " + e.getMessage())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/developers")
    @Operation(summary = "List Apigee Developers")
    public ResponseEntity<List<DeveloperResponse>> listDevelopers(@RequestParam(required = false) String org) {
        log.info("Listing developers for org: {}", org);
        List<DeveloperResponse> developers = inventoryService.getDevelopers(org);
        return ResponseEntity.ok(developers);
    }
    
    @GetMapping("/developers/{developerId}/apps")
    @Operation(summary = "List Apps for a Developer")
    public ResponseEntity<List<DeveloperAppResponse>> listDeveloperApps(
            @PathVariable String developerId,
            @RequestParam(required = false) String org) {
        log.info("Listing apps for developer: {} in org: {}", developerId, org);
        List<DeveloperAppResponse> apps = inventoryService.getDeveloperApps(developerId, org);
        return ResponseEntity.ok(apps);
    }
    
    @PostMapping("/developers/{developerId}/link")
    @Operation(summary = "Link Apigee Developer to Aforo Customer")
    public ResponseEntity<Void> linkDeveloper(
            @PathVariable String developerId,
            @Valid @RequestBody LinkDeveloperRequest request) {
        log.info("Linking developer {} to customer {}", developerId, request.getAforoCustomerId());
        inventoryService.linkDeveloper(developerId, request);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/mappings/subscriptions")
    @Operation(summary = "Create DRAFT Subscription for (App × Product) and write back attributes")
    public ResponseEntity<MappingResponse> createMapping(@Valid @RequestBody CreateMappingRequest request) {
        log.info("Creating mapping for app: {} and product: {}", 
                 request.getDeveloperApp(), request.getApiProduct());
        MappingResponse response = mappingService.createDraftSubscription(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/authorize")
    @Operation(summary = "Authorization decision for runtime")
    public ResponseEntity<AuthorizeDecision> authorize(@Valid @RequestBody AuthorizeRequest request) {
        log.info("Authorization request for app: {} and product: {}", 
                 request.getDeveloperApp(), request.getApiProduct());
        AuthorizeDecision decision = authorizeService.authorize(request);
        return ResponseEntity.ok(decision);
    }
    
    @PostMapping("/webhooks/usage")
    @Operation(summary = "Ingest usage from Apigee (HMAC verified)")
    public ResponseEntity<Void> ingestUsage(@Valid @RequestBody UsageWebhookEvent event) {
        log.info("Received usage webhook for app: {}", event.getDeveloperApp());
        usageService.ingestUsage(event);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
    
    @PostMapping("/customers/sync")
    @Operation(summary = "Sync customers/developers from Apigee to Customer Service", 
               description = "Fetches developers from Apigee and automatically pushes them to Customer Service")
    public ResponseEntity<CustomerSyncResponse> syncCustomers() {
        log.info("Customer sync request received");
        CustomerSyncResponse response = inventoryService.syncCustomers();
        return ResponseEntity.ok(response);
    }
}
