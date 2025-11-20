package com.aforo.apigee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMappingRequest {
    
    @NotBlank(message = "Developer app is required")
    private String developerApp;
    
    @NotBlank(message = "API product is required")
    private String apiProduct;
    
    @NotNull(message = "Aforo product ID is required")
    private Long aforoProductId;
    
    @NotNull(message = "Rate plan ID is required")
    private Long ratePlanId;
    
    @NotBlank(message = "Billing type is required")
    private String billingType;
}
