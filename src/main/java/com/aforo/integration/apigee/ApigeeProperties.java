package com.aforo.integration.apigee;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

/**
 * Configuration properties for Apigee integration.
 * Reads from application.yml under aforo.apigee prefix.
 */
@Data
@Component
@ConfigurationProperties(prefix = "aforo.apigee")
public class ApigeeProperties {
    
    /**
     * Apigee organization name
     */
    private String org;
    
    /**
     * Apigee environment (e.g., test, prod)
     */
    private String env;
    
    /**
     * Base URL for Apigee Management API
     * Default: https://apigee.googleapis.com/v1
     */
    private String baseUrl = "https://apigee.googleapis.com/v1";
    
    /**
     * Bearer token for Apigee Management API authentication
     * TODO: Later migrate to KMS/Secret Manager
     */
    private String token;
    
    /**
     * Connection timeout in seconds
     */
    private int connectTimeoutSeconds = 10;
    
    /**
     * Read timeout in seconds
     */
    private int readTimeoutSeconds = 30;
    
    /**
     * Enable debug logging for Apigee API calls
     */
    private boolean debugLogging = false;
}
