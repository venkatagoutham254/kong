package com.aforo.apigee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImportResponse {
    private String message;
    private String status;  // "CREATED" or "UPDATED"
    private Long productId;
    private String productName;
    private String source;
    private String externalId;
}
