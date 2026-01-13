package com.aforo.integration.apigee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Apigee catalog sync operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApigeeCatalogSyncResponse {
    private String status; // "COMPLETED", "IN_PROGRESS", "FAILED"
    private Integer productsImported;
    private Integer endpointsImported;
    private Integer customersImported;
    private Integer appsImported;
    private String syncStartTime;
    private String syncEndTime;
    private Long durationMs;
    private String message;
}
