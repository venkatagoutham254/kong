package com.aforo.integration.apigee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * DTO representing an Apigee API Product.
 * Maps to Kong Consumer Group + Rate Limiting concept.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApigeeApiProduct {
    private String name;
    private String displayName;
    private String description;
    private List<String> proxies;
    private List<String> apiResources;
    private List<String> environments;
    private List<String> scopes;
    
    // Quota configuration
    @JsonProperty("quota")
    private String quotaLimit;
    
    @JsonProperty("quotaInterval")
    private String quotaInterval;
    
    @JsonProperty("quotaTimeUnit")
    private String quotaTimeUnit;
    
    // Rate limit configuration
    private Integer rateLimit;
    private String rateLimitTimeUnit;
    
    private String approvalType; // auto, manual
    private Map<String, String> attributes;
    private String createdAt;
    private String lastModifiedAt;
}
