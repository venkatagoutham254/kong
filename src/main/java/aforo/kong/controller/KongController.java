package aforo.kong.controller;

import aforo.kong.dto.ClientApiDetailsDTO;
import aforo.kong.dto.KongProductResponse;
import aforo.kong.entity.KongProduct;
import aforo.kong.service.ClientApiDetailsService;
import aforo.kong.service.ExternalApiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kong-products")
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
    public ResponseEntity<KongProductResponse> fetchProducts() {
        ClientApiDetailsDTO apiDetails = clientApiDetailsService.fetchApiDetails(clientApiUrl);
        KongProductResponse products = externalApiService.fetchProducts(apiDetails);
        return ResponseEntity.ok(products);
    }

    // NEW: fetch using clientDetailsId
    @GetMapping("/fetch/from-db/{clientDetailsId}")
    public ResponseEntity<List<KongProductResponse>> fetchFromDb(
            @PathVariable Long clientDetailsId) {
        List<KongProductResponse> products = externalApiService.fetchProductsFromDb(clientDetailsId);
        return ResponseEntity.ok(products);
    }


    @GetMapping
    public ResponseEntity<List<KongProduct>> getAllProducts() {
        List<KongProduct> products = externalApiService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<KongProduct> getProductById(@PathVariable String id) {
        KongProduct product = externalApiService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        externalApiService.deleteProductById(id);
        return ResponseEntity.noContent().build();
    }
}
