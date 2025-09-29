package aforo.kong.controller;

import aforo.kong.dto.ClientApiDetailsDTO;
import aforo.kong.dto.KongProductResponse;
import aforo.kong.entity.KongProduct;
import aforo.kong.service.ClientApiDetailsService;
import aforo.kong.service.ExternalApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kong")
@Tag(name = "Kong Products", description = "Multi-tenant Kong product management API")
@SecurityRequirement(name = "bearerAuth")
public class KongController {

    private final ClientApiDetailsService clientApiDetailsService;
    private final ExternalApiService externalApiService;

    @Value("${kong.client-details-url:http://localhost:8081/mock/api/details}")
    private String clientApiUrl;

    public KongController(ClientApiDetailsService clientApiDetailsService, ExternalApiService externalApiService) {
        this.clientApiDetailsService = clientApiDetailsService;
        this.externalApiService = externalApiService;
    }

    @GetMapping("/fetch")
    @Operation(summary = "Fetch products from external API", 
               description = "Fetches Kong products from external API and saves them for the current organization")
    public ResponseEntity<KongProductResponse> fetchProducts() {
        ClientApiDetailsDTO apiDetails = clientApiDetailsService.fetchApiDetails(clientApiUrl);
        KongProductResponse products = externalApiService.fetchProducts(apiDetails);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/fetch/from-db/{clientDetailsId}")
    @Operation(summary = "Fetch products using stored client details", 
               description = "Fetches Kong products using stored client API details for the current organization")
    public ResponseEntity<List<KongProductResponse>> fetchFromDb(
            @Parameter(description = "Client API details ID") @PathVariable Long clientDetailsId) {
        List<KongProductResponse> products = externalApiService.fetchProductsFromDb(clientDetailsId);
        return ResponseEntity.ok(products);
    }

    @GetMapping
    @Operation(summary = "Get all products", 
               description = "Returns all Kong products for the current organization")
    public ResponseEntity<List<KongProduct>> getAllProducts() {
        List<KongProduct> products = externalApiService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", 
               description = "Returns a specific Kong product by ID for the current organization")
    public ResponseEntity<KongProduct> getProductById(
            @Parameter(description = "Product ID") @PathVariable String id) {
        KongProduct product = externalApiService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", 
               description = "Deletes a Kong product by ID for the current organization")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID") @PathVariable String id) {
        externalApiService.deleteProductById(id);
        return ResponseEntity.noContent().build();
    }
}
