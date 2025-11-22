package com.aforo.apigee.dto.request;

import com.aforo.apigee.dto.ProductType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request for selective product import from Apigee
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectiveProductImportRequest {
    
    @NotEmpty(message = "At least one product must be selected for import")
    @Valid
    private List<SelectedProduct> selectedProducts;
    
    private String org;  // Optional Apigee org
    
    /**
     * Individual product selection
     * User only needs to provide: productName (like Kong only asks for ID)
     * displayName and productType are auto-assigned
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectedProduct {
        
        @NotNull(message = "Product name is required")
        private String productName;
        
        // displayName is optional - will use productName if not provided
        private String displayName;
        
        // productType is optional - defaults to API
        private ProductType productType;
    }
}
