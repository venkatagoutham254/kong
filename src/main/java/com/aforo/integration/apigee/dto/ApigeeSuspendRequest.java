package com.aforo.integration.apigee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for suspending an Apigee app.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApigeeSuspendRequest {
    @NotBlank(message = "Developer ID is required")
    private String developerId;
    
    @NotBlank(message = "App name is required")
    private String appName;
    
    @NotBlank(message = "Consumer key is required")
    private String consumerKey;
    
    @NotBlank(message = "Mode is required")
    @Pattern(regexp = "revoke|remove-products", message = "Mode must be 'revoke' or 'remove-products'")
    private String mode;
    
    private String reason;
}
