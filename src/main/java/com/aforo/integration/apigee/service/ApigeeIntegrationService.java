package com.aforo.integration.apigee.service;

import com.aforo.integration.apigee.dto.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Apigee integration operations.
 */
public interface ApigeeIntegrationService {
    
    /**
     * Test connection to Apigee Management API.
     */
    Mono<ApigeeConnectResponse> testConnection(ApigeeConnectRequest request);
    
    /**
     * Sync catalog from Apigee (API Products, Proxies, Developers, Apps).
     */
    Mono<ApigeeCatalogSyncResponse> syncCatalog(String syncType);
    
    /**
     * Ingest usage events from Apigee.
     */
    Mono<Integer> ingestEvents(List<ApigeeEvent> events);
    
    /**
     * Enforce pricing plans by mapping to API Products.
     */
    Mono<List<Map<String, Object>>> enforcePlans(ApigeeEnforcePlanRequest request);
    
    /**
     * Suspend a developer app.
     */
    Mono<Void> suspendApp(ApigeeSuspendRequest request);
    
    /**
     * Resume a suspended developer app.
     */
    Mono<Void> resumeApp(String developerId, String appName, String consumerKey);
    
    /**
     * Check health of Apigee integration.
     */
    Mono<Map<String, Object>> checkHealth();
}
