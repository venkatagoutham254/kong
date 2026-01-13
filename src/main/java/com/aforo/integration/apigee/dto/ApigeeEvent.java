package com.aforo.integration.apigee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * DTO representing an Apigee runtime event for usage ingestion.
 * Similar to KongEvent but using Apigee terminology.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApigeeEvent {
    @NotNull
    private String timestamp;
    
    @NotNull
    private String org;
    
    @NotNull
    private String env;
    
    @NotNull
    @JsonProperty("apiProxy")
    private String apiProxy;
    
    @JsonProperty("proxyBasepath")
    private String proxyBasepath;
    
    @JsonProperty("resourcePath")
    private String resourcePath;
    
    private String method;
    private Integer status;
    
    @JsonProperty("latencyMs")
    private Long latencyMs;
    
    @JsonProperty("developerId")
    private String developerId;
    
    @JsonProperty("appName")
    private String appName;
    
    @JsonProperty("apiProduct")
    private String apiProduct;
    
    @JsonProperty("apiKey")
    private String apiKey;
    
    @JsonProperty("correlationId")
    private String correlationId;
    
    // Request/Response metrics
    @JsonProperty("requestSize")
    private Long requestSize;
    
    @JsonProperty("responseSize")
    private Long responseSize;
    
    @JsonProperty("errorCode")
    private String errorCode;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    // Additional metadata
    @JsonProperty("clientIp")
    private String clientIp;
    
    @JsonProperty("userAgent")
    private String userAgent;
    
    @JsonProperty("targetUrl")
    private String targetUrl;
    
    @JsonProperty("targetResponseCode")
    private Integer targetResponseCode;
    
    @JsonProperty("targetLatencyMs")
    private Long targetLatencyMs;
}
