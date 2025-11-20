package com.aforo.apigee.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageWebhookEvent {
    
    @NotNull(message = "Timestamp is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant ts;
    
    private String apiproxy;
    
    @NotBlank(message = "Developer app is required")
    private String developerApp;
    
    @NotBlank(message = "API product is required")
    private String apiProduct;
    
    @NotBlank(message = "HTTP method is required")
    private String method;
    
    @NotBlank(message = "Path is required")
    private String path;
    
    @NotNull(message = "Status is required")
    private Integer status;
    
    private Integer latencyMs;
    
    private Integer bytesOut;
    
    private Map<String, String> extras;
}
