package com.aforo.apigee.dto.request;

import com.aforo.apigee.dto.ProductType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImportRequest {
    private String productName;
    private String productDescription;
    private String source;
    private String externalId;
    private String internalSkuCode;
    private ProductType productType;  // Added product type field
}
