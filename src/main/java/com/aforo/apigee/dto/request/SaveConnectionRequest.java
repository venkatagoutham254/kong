package com.aforo.apigee.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveConnectionRequest {
    
    @NotBlank(message = "Organization is required")
    private String org;
    
    @NotBlank(message = "Environments CSV is required")
    private String envs;
    
    @NotBlank(message = "Analytics mode is required")
    private String analyticsMode;
    
    @NotBlank(message = "Service account JSON is required")
    private String serviceAccountJson;  // JSON content from uploaded file
}
