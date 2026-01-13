package com.aforo.integration.apigee.service;

import com.aforo.integration.apigee.ApigeeProperties;
import com.aforo.integration.apigee.client.ApigeeManagementClient;
import com.aforo.integration.apigee.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of Apigee integration service.
 * Orchestrates operations between Apigee and Aforo platform.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApigeeIntegrationServiceImpl implements ApigeeIntegrationService {
    
    private final ApigeeManagementClient apigeeClient;
    private final ApigeeProperties properties;
    
    // TODO: Inject these services once they're available
    // private final ProductService productService;
    // private final CustomerService customerService;
    // private final UsageRecordService usageRecordService;
    
    // Cache for last sync time
    private volatile Instant lastCatalogSyncTime;
    
    @Override
    public Mono<ApigeeConnectResponse> testConnection(ApigeeConnectRequest request) {
        // Override properties if provided in request
        String org = request.getOrg() != null ? request.getOrg() : properties.getOrg();
        String env = request.getEnv() != null ? request.getEnv() : properties.getEnv();
        
        return apigeeClient.getOrganization()
            .flatMap(orgInfo -> {
                // Count resources in parallel
                Mono<Long> apiCount = apigeeClient.listApis().count();
                Mono<Long> productCount = apigeeClient.listApiProducts().count();
                Mono<Long> developerCount = apigeeClient.listDevelopers().count();
                
                return Mono.zip(apiCount, productCount, developerCount)
                    .map(counts -> ApigeeConnectResponse.builder()
                        .status("connected")
                        .org(org)
                        .env(env)
                        .message("Successfully connected to Apigee organization: " + orgInfo.getName())
                        .apiProxyCount(counts.getT1().intValue())
                        .apiProductCount(counts.getT2().intValue())
                        .developerCount(counts.getT3().intValue())
                        .build());
            })
            .onErrorResume(e -> {
                log.error("Failed to connect to Apigee", e);
                return Mono.just(ApigeeConnectResponse.builder()
                    .status("failed")
                    .org(org)
                    .env(env)
                    .message("Connection failed: " + e.getMessage())
                    .build());
            });
    }
    
    @Override
    public Mono<ApigeeCatalogSyncResponse> syncCatalog(String syncType) {
        Instant syncStartTime = Instant.now();
        
        ApigeeCatalogSyncResponse.ApigeeCatalogSyncResponseBuilder responseBuilder = 
            ApigeeCatalogSyncResponse.builder()
                .syncStartTime(syncStartTime.toString())
                .status("IN_PROGRESS");
        
        AtomicInteger productsImported = new AtomicInteger(0);
        AtomicInteger proxiesImported = new AtomicInteger(0);
        AtomicInteger developersImported = new AtomicInteger(0);
        AtomicInteger appsImported = new AtomicInteger(0);
        
        // Sync API Products
        Mono<Void> syncProducts = apigeeClient.listApiProducts()
            .flatMap(productName -> apigeeClient.getApiProduct(productName))
            .doOnNext(product -> {
                // TODO: Map to Aforo Product entity and save
                log.debug("Syncing API Product: {}", product.getName());
                productsImported.incrementAndGet();
            })
            .then();
        
        // Sync API Proxies
        Mono<Void> syncProxies = apigeeClient.listApis()
            .flatMap(apiName -> apigeeClient.getApi(apiName))
            .doOnNext(proxy -> {
                // TODO: Map to Aforo Endpoint entity and save
                log.debug("Syncing API Proxy: {}", proxy.getName());
                proxiesImported.incrementAndGet();
            })
            .then();
        
        // Sync Developers and Apps
        Mono<Void> syncDevelopersAndApps = apigeeClient.listDevelopers()
            .flatMap(developerId -> {
                developersImported.incrementAndGet();
                return apigeeClient.getDeveloper(developerId)
                    .flatMapMany(developer -> 
                        apigeeClient.listDeveloperApps(developer.getDeveloperId())
                            .flatMap(appRef -> 
                                apigeeClient.getDeveloperApp(developer.getDeveloperId(), 
                                                            appRef.getAppName())
                                    .doOnNext(app -> {
                                        // TODO: Map to Aforo Customer entity and save
                                        log.debug("Syncing App: {} for Developer: {}", 
                                                app.getAppName(), developer.getEmail());
                                        appsImported.incrementAndGet();
                                    })
                            )
                    );
            })
            .then();
        
        return Mono.when(syncProducts, syncProxies, syncDevelopersAndApps)
            .then(Mono.fromCallable(() -> {
                Instant syncEndTime = Instant.now();
                lastCatalogSyncTime = syncEndTime;
                
                return responseBuilder
                    .status("COMPLETED")
                    .productsImported(productsImported.get())
                    .endpointsImported(proxiesImported.get())
                    .customersImported(developersImported.get())
                    .appsImported(appsImported.get())
                    .syncEndTime(syncEndTime.toString())
                    .durationMs(syncEndTime.toEpochMilli() - syncStartTime.toEpochMilli())
                    .message(String.format("Synced %d products, %d proxies, %d developers, %d apps",
                            productsImported.get(), proxiesImported.get(), 
                            developersImported.get(), appsImported.get()))
                    .build();
            }))
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorResume(e -> {
                log.error("Catalog sync failed", e);
                return Mono.just(responseBuilder
                    .status("FAILED")
                    .message("Sync failed: " + e.getMessage())
                    .syncEndTime(Instant.now().toString())
                    .build());
            });
    }
    
    @Override
    public Mono<Integer> ingestEvents(List<ApigeeEvent> events) {
        if (events == null || events.isEmpty()) {
            return Mono.just(0);
        }
        
        return Flux.fromIterable(events)
            .parallel()
            .runOn(Schedulers.parallel())
            .flatMap(event -> processEvent(event))
            .sequential()
            .count()
            .map(Long::intValue)
            .doOnSuccess(count -> log.info("Processed {} Apigee events", count))
            .onErrorResume(e -> {
                log.error("Failed to ingest events", e);
                return Mono.error(new RuntimeException("Event ingestion failed: " + e.getMessage()));
            });
    }
    
    private Mono<Void> processEvent(ApigeeEvent event) {
        return Mono.fromRunnable(() -> {
            // TODO: Convert ApigeeEvent to UsageRecord and save
            log.debug("Processing event: {} {} {} at {}", 
                    event.getApiProxy(), event.getMethod(), 
                    event.getResourcePath(), event.getTimestamp());
            
            // Validate event
            if (event.getApiProxy() == null || event.getTimestamp() == null) {
                throw new IllegalArgumentException("Invalid event: missing required fields");
            }
            
            // Map to internal UsageRecord
            // UsageRecord record = UsageRecord.builder()
            //     .timestamp(Instant.parse(event.getTimestamp()))
            //     .customerId(resolveCustomerId(event))
            //     .productId(resolveProductId(event))
            //     .endpointId(resolveEndpointId(event))
            //     .requestCount(1)
            //     .responseSize(event.getResponseSize())
            //     .latencyMs(event.getLatencyMs())
            //     .statusCode(event.getStatus())
            //     .build();
            
            // usageRecordService.save(record);
        });
    }
    
    @Override
    public Mono<List<Map<String, Object>>> enforcePlans(ApigeeEnforcePlanRequest request) {
        return Flux.fromIterable(request.getMappings())
            .flatMap(mapping -> enforcePlanMapping(mapping)
                .map(v -> Map.<String, Object>of(
                    "planId", mapping.getPlanId(),
                    "developerId", mapping.getDeveloperId(),
                    "appName", mapping.getAppName(),
                    "status", "success"
                ))
                .onErrorResume(e -> Mono.just(Map.<String, Object>of(
                    "planId", mapping.getPlanId(),
                    "developerId", mapping.getDeveloperId(),
                    "appName", mapping.getAppName(),
                    "status", "failed",
                    "error", e.getMessage()
                )))
            )
            .collectList()
            .doOnSuccess(results -> 
                log.info("Enforced {} plan mappings", results.size()))
            .onErrorResume(e -> {
                log.error("Plan enforcement failed", e);
                return Mono.error(new RuntimeException("Failed to enforce plans: " + e.getMessage()));
            });
    }
    
    private Mono<Void> enforcePlanMapping(ApigeeEnforcePlanRequest.PlanMapping mapping) {
        // Add API product to app key
        return apigeeClient.addApiProductToAppKey(
            mapping.getDeveloperId(),
            mapping.getAppName(),
            mapping.getConsumerKey(),
            mapping.getApiProductName()
        )
        .doOnSuccess(v -> 
            log.info("Enforced plan {} for app {} by adding product {}", 
                    mapping.getPlanId(), mapping.getAppName(), mapping.getApiProductName()));
    }
    
    @Override
    public Mono<Void> suspendApp(ApigeeSuspendRequest request) {
        if ("revoke".equals(request.getMode())) {
            // Revoke the app key
            return apigeeClient.revokeAppKey(
                request.getDeveloperId(),
                request.getAppName(),
                request.getConsumerKey()
            )
            .doOnSuccess(v -> 
                log.info("Revoked app key {} for app {} (reason: {})", 
                        request.getConsumerKey(), request.getAppName(), request.getReason()));
        } else if ("remove-products".equals(request.getMode())) {
            // Remove all API products from the app key
            return apigeeClient.getDeveloperApp(request.getDeveloperId(), request.getAppName())
                .flatMapMany(app -> {
                    List<String> products = app.getApiProducts();
                    if (products == null || products.isEmpty()) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(products);
                })
                .flatMap(productName -> 
                    apigeeClient.removeApiProductFromAppKey(
                        request.getDeveloperId(),
                        request.getAppName(),
                        request.getConsumerKey(),
                        productName
                    )
                )
                .then()
                .doOnSuccess(v -> 
                    log.info("Removed all products from app {} (reason: {})", 
                            request.getAppName(), request.getReason()));
        } else {
            return Mono.error(new IllegalArgumentException("Invalid suspension mode: " + request.getMode()));
        }
    }
    
    @Override
    public Mono<Void> resumeApp(String developerId, String appName, String consumerKey) {
        // Approve the app key to resume access
        return apigeeClient.approveAppKey(developerId, appName, consumerKey)
            .doOnSuccess(v -> 
                log.info("Resumed app {} by approving key {}", appName, consumerKey))
            .onErrorResume(e -> {
                log.error("Failed to resume app", e);
                return Mono.error(new RuntimeException("Failed to resume app: " + e.getMessage()));
            });
    }
    
    @Override
    public Mono<Map<String, Object>> checkHealth() {
        return apigeeClient.getOrganization()
            .map(org -> {
                Map<String, Object> health = new HashMap<>();
                health.put("apigeeReachable", true);
                health.put("org", org.getName());
                health.put("env", properties.getEnv());
                health.put("lastCatalogSyncTimestamp", 
                          lastCatalogSyncTime != null ? lastCatalogSyncTime.toString() : null);
                return health;
            })
            .onErrorResume(e -> {
                log.warn("Health check failed", e);
                Map<String, Object> health = new HashMap<>();
                health.put("apigeeReachable", false);
                health.put("error", e.getMessage());
                return Mono.just(health);
            });
    }
    
    // Helper methods for resolving IDs (placeholder implementations)
    private String resolveCustomerId(ApigeeEvent event) {
        // TODO: Lookup customer by developerId + appName
        return event.getDeveloperId() + ":" + event.getAppName();
    }
    
    private String resolveProductId(ApigeeEvent event) {
        // TODO: Lookup product by apiProduct name
        return event.getApiProduct();
    }
    
    private String resolveEndpointId(ApigeeEvent event) {
        // TODO: Lookup endpoint by apiProxy + resourcePath
        return event.getApiProxy() + ":" + event.getResourcePath();
    }
}
