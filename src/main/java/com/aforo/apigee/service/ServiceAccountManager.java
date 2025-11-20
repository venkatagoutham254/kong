package com.aforo.apigee.service;

import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Manages Google Service Account credentials for Apigee
 * Supports both file path (local dev) and JSON content (production/AWS)
 */
@Slf4j
@Service
public class ServiceAccountManager {
    
    private static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";
    
    /**
     * Get access token from service account JSON file path
     */
    public String getAccessTokenFromPath(String saJsonPath) throws IOException {
        log.debug("Getting access token from file path: {}", saJsonPath);
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream(saJsonPath))
                .createScoped(Arrays.asList(CLOUD_PLATFORM_SCOPE));
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }
    
    /**
     * Get access token from service account JSON content (for production/AWS)
     */
    public String getAccessTokenFromJson(String serviceAccountJson) throws IOException {
        log.debug("Getting access token from JSON content");
        byte[] jsonBytes = serviceAccountJson.getBytes(StandardCharsets.UTF_8);
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new ByteArrayInputStream(jsonBytes))
                .createScoped(Arrays.asList(CLOUD_PLATFORM_SCOPE));
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }
    
    /**
     * Get access token - tries JSON content first, falls back to file path
     */
    public String getAccessToken(String serviceAccountJson, String saJsonPath) throws IOException {
        if (serviceAccountJson != null && !serviceAccountJson.isEmpty()) {
            return getAccessTokenFromJson(serviceAccountJson);
        } else if (saJsonPath != null && !saJsonPath.isEmpty()) {
            return getAccessTokenFromPath(saJsonPath);
        } else {
            throw new IllegalArgumentException("Either serviceAccountJson or saJsonPath must be provided");
        }
    }
}
