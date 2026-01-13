package com.aforo.integration.apigee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Apigee connection test.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApigeeConnectResponse {
    private String status; // "connected" or "failed"
    private String org;
    private String env;
    private String message;
    private Integer apiProxyCount;
    private Integer apiProductCount;
    private Integer developerCount;
    private Integer appCount;
}
