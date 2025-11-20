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
public class AuthorizeRequest {
    
    @NotBlank(message = "Organization is required")
    private String org;
    
    @NotBlank(message = "Environment is required")
    private String env;
    
    @NotBlank(message = "Developer app is required")
    private String developerApp;
    
    @NotBlank(message = "API product is required")
    private String apiProduct;
    
    @NotBlank(message = "HTTP method is required")
    private String method;
    
    @NotBlank(message = "Path is required")
    private String path;
}
