package com.aforo.integration.apigee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO representing an Apigee App Key (API Key/Consumer Key).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApigeeAppKey {
    @JsonProperty("consumerKey")
    private String consumerKey;
    
    @JsonProperty("consumerSecret")
    private String consumerSecret;
    
    private String status;
    private List<String> apiProducts;
    private String issuedAt;
    private String expiresAt;
    private List<String> scopes;
}
