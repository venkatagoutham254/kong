# Product/RatePlan Service Integration Guide

## Overview
This guide explains how to update your Product/RatePlan service to handle products coming from both Kong and Apigee integrations.

## Required Changes

### 1. Database Schema Updates

Add the following fields to your `products` table if they don't exist:

```sql
ALTER TABLE products ADD COLUMN IF NOT EXISTS external_id VARCHAR(255);
ALTER TABLE products ADD COLUMN IF NOT EXISTS source VARCHAR(50);
ALTER TABLE products ADD COLUMN IF NOT EXISTS product_type VARCHAR(50) DEFAULT 'API';

-- Add indexes for better performance
CREATE INDEX IF NOT EXISTS idx_products_external_id ON products(external_id);
CREATE INDEX IF NOT EXISTS idx_products_source ON products(source);
```

### 2. Product Entity Updates

Update your Product entity to include these fields:

```java
@Entity
@Table(name = "products")
public class Product {
    // ... existing fields ...
    
    @Column(name = "external_id")
    private String externalId;
    
    @Column(name = "source")
    private String source; // "kong" or "apigee"
    
    @Column(name = "product_type")
    private String productType = "API"; // Default to API
    
    // ... getters and setters ...
}
```

### 3. Product Import Endpoint

Create or update your product import endpoint to handle products from both sources:

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @PostMapping("/import")
    public ResponseEntity<ProductImportResponse> importProduct(
            @RequestBody ProductImportRequest request,
            @RequestHeader("X-Organization-Id") Long organizationId) {
        
        // Validate source
        if (!Arrays.asList("kong", "apigee").contains(request.getSource().toLowerCase())) {
            throw new IllegalArgumentException("Invalid source. Must be 'kong' or 'apigee'");
        }
        
        // Check if product already exists by external_id and source
        Optional<Product> existingProduct = productRepository
            .findByExternalIdAndSourceAndOrganizationId(
                request.getExternalId(), 
                request.getSource(), 
                organizationId
            );
        
        Product product;
        if (existingProduct.isPresent()) {
            // Update existing product
            product = existingProduct.get();
            product.setName(request.getProductName());
            product.setDescription(request.getProductDescription());
        } else {
            // Create new product
            product = new Product();
            product.setExternalId(request.getExternalId());
            product.setSource(request.getSource().toLowerCase());
            product.setName(request.getProductName());
            product.setDescription(request.getProductDescription());
            product.setOrganizationId(organizationId);
        }
        
        // Always set product_type as API (auto-set)
        product.setProductType("API");
        
        // Set SKU based on source
        if ("kong".equalsIgnoreCase(request.getSource())) {
            product.setInternalSkuCode("KONG-" + request.getExternalId());
        } else if ("apigee".equalsIgnoreCase(request.getSource())) {
            product.setInternalSkuCode("APIGEE-" + request.getExternalId());
        }
        
        Product savedProduct = productRepository.save(product);
        
        return ResponseEntity.ok(ProductImportResponse.builder()
            .productId(savedProduct.getId())
            .message("Product imported successfully from " + request.getSource())
            .build());
    }
}
```

### 4. Product Import Request DTO

```java
@Data
@Builder
public class ProductImportRequest {
    private String productName;
    private String productDescription;
    private String source; // "kong" or "apigee"
    private String externalId; // ID from the source system
    private String internalSkuCode; // Optional, will be auto-generated if not provided
    // Note: product_type is not needed as it's always "API"
}
```

### 5. Product Import Response DTO

```java
@Data
@Builder
public class ProductImportResponse {
    private Long productId;
    private String message;
    private String source;
    private String externalId;
}
```

### 6. Repository Updates

Add these methods to your ProductRepository:

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Optional<Product> findByExternalIdAndSourceAndOrganizationId(
        String externalId, 
        String source, 
        Long organizationId
    );
    
    List<Product> findBySourceAndOrganizationId(
        String source, 
        Long organizationId
    );
    
    List<Product> findByProductTypeAndOrganizationId(
        String productType, 
        Long organizationId
    );
}
```

### 7. Service Layer Updates

```java
@Service
public class ProductService {
    
    public Product importFromKong(KongProductData data, Long organizationId) {
        ProductImportRequest request = ProductImportRequest.builder()
            .productName(data.getName())
            .productDescription(data.getDescription())
            .source("kong")
            .externalId(data.getId())
            .internalSkuCode("KONG-" + data.getId())
            .build();
        
        return importProduct(request, organizationId);
    }
    
    public Product importFromApigee(ApigeeProductData data, Long organizationId) {
        ProductImportRequest request = ProductImportRequest.builder()
            .productName(data.getDisplayName())
            .productDescription("Imported from Apigee")
            .source("apigee")
            .externalId(data.getName())
            .internalSkuCode("APIGEE-" + data.getName())
            .build();
        
        return importProduct(request, organizationId);
    }
    
    private Product importProduct(ProductImportRequest request, Long organizationId) {
        // Implementation as shown in controller above
        // ...
    }
}
```

## Integration Flow

### From Kong Service:
1. Kong service calls `/api/products/import` with:
   - `source`: "kong"
   - `external_id`: Kong product ID
   - `product_name`: Product name from Kong
   - `product_type`: Automatically set to "API"

### From Apigee Service:
1. Apigee service calls `/api/products/import` with:
   - `source`: "apigee"
   - `external_id`: Apigee product name
   - `product_name`: Display name from Apigee
   - `product_type`: Automatically set to "API"

## Key Points

1. **Product Type**: Always automatically set to "API" - no manual selection needed
2. **Source Tracking**: Products are tracked by their source (kong/apigee)
3. **External ID**: Maintains reference to the original ID in the source system
4. **Duplicate Prevention**: Uses combination of external_id + source + organization_id
5. **SKU Generation**: Automatically generates SKU based on source

## Testing

Test the import endpoint:

```bash
# Import from Kong
curl -X POST http://localhost:8080/api/products/import \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: 1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "productName": "Test Kong Product",
    "productDescription": "Product from Kong",
    "source": "kong",
    "externalId": "kong-123"
  }'

# Import from Apigee
curl -X POST http://localhost:8080/api/products/import \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: 1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "productName": "Test Apigee Product",
    "productDescription": "Product from Apigee",
    "source": "apigee",
    "externalId": "apigee-456"
  }'
```

## Migration Notes

1. Run database migrations to add new columns
2. Update existing products to have default source if needed
3. Set product_type to "API" for all existing products
4. Ensure indexes are created for performance
