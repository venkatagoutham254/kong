package com.aforo.integration.apigee.client;

import com.aforo.integration.apigee.dto.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Interface for Apigee Management API client operations.
 * Provides reactive methods for interacting with Apigee v1 Management API.
 */
public interface ApigeeManagementClient {
    
    // Organization operations
    Mono<ApigeeOrgInfo> getOrganization();
    
    // API Proxy operations
    Flux<String> listApis();
    Mono<ApigeeApiProxy> getApi(String apiName);
    
    // API Product operations
    Flux<String> listApiProducts();
    Mono<ApigeeApiProduct> getApiProduct(String productName);
    
    // Developer operations
    Flux<String> listDevelopers();
    Mono<ApigeeDeveloper> getDeveloper(String developerId);
    
    // Developer App operations
    Flux<ApigeeAppRef> listDeveloperApps(String developerId);
    Mono<ApigeeApp> getDeveloperApp(String developerId, String appName);
    
    // App Key operations - for entitlement/plan mapping
    Mono<Void> addApiProductToAppKey(String developerId, String appName, 
                                     String consumerKey, String apiProductName);
    Mono<Void> removeApiProductFromAppKey(String developerId, String appName, 
                                          String consumerKey, String apiProductName);
    
    // App suspension operations
    Mono<Void> revokeAppKey(String developerId, String appName, String consumerKey);
    Mono<Void> approveAppKey(String developerId, String appName, String consumerKey);
    
    // KVM operations (optional - for per-customer quotas/wallet)
    Mono<Void> upsertKvmEntry(String kvmName, String entryName, String value);
    Mono<String> getKvmEntry(String kvmName, String entryName);
}
