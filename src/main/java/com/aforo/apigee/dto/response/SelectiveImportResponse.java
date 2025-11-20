package com.aforo.apigee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for selective product import operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectiveImportResponse {
    private int totalSelected;
    private int successfullyImported;
    private int failed;
    private String message;
    private List<ImportedProductDetail> importedProducts;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportedProductDetail {
        private String productName;
        private String productType;
        private String status;  // "SUCCESS" or "FAILED"
        private String message;
        private Long productId;  // ID from ProductRatePlanService if successful
    }
}
