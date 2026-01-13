package com.aforo.integration.apigee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for enforcing plans in Apigee.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApigeeEnforcePlanRequest {
    @NotEmpty(message = "Mappings are required")
    @Valid
    private List<PlanMapping> mappings;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanMapping {
        private String planId;
        private String developerId;
        private String appName;
        private String consumerKey;
        private String apiProductName;
    }
}
