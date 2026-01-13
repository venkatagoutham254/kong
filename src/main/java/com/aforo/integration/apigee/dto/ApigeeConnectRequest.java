package com.aforo.integration.apigee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for connecting to Apigee.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApigeeConnectRequest {
    @NotBlank(message = "Organization is required")
    private String org;
    
    @NotBlank(message = "Environment is required")
    private String env;
    
    private String baseUrl; // Optional, uses default from properties if not provided
    private String token; // Optional, uses default from properties if not provided
}
