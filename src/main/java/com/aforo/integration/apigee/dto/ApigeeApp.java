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
 * DTO representing an Apigee Developer App.
 * Maps to Kong Consumer concept along with Developer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApigeeApp {
    @JsonProperty("appId")
    private String appId;
    
    @JsonProperty("name")
    private String appName;
    
    private String developerId;
    private String status;
    private String callbackUrl;
    private List<String> apiProducts;
    private List<ApigeeAppKey> credentials;
    private Map<String, String> attributes;
    private String createdAt;
    private String lastModifiedAt;
}
