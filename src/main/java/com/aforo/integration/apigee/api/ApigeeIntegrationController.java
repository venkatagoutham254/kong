package com.aforo.integration.apigee.api;

import com.aforo.integration.apigee.dto.*;
import com.aforo.integration.apigee.service.ApigeeIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Apigee integration endpoints.
 * Mirrors the Kong integration functionality for Apigee.
 */
@Slf4j
@RestController
@RequestMapping("/integrations/apigee")
@RequiredArgsConstructor
@Tag(name = "Apigee Integration", description = "Apigee integration management APIs")
@PreAuthorize("isAuthenticated()")
public class ApigeeIntegrationController {
    
    private final ApigeeIntegrationService apigeeIntegrationService;
    
    /**
     * Test connection to Apigee Management API.
     */
    @PostMapping("/connect")
    @Operation(summary = "Connect to Apigee", 
               description = "Test connection and verify Apigee organization access")
    public Mono<ResponseEntity<ApigeeConnectResponse>> connect(
            @Valid @RequestBody ApigeeConnectRequest request) {
        
        log.info("Testing connection to Apigee org: {} env: {}", request.getOrg(), request.getEnv());
        
        return apigeeIntegrationService.testConnection(request)
            .map(response -> {
                if ("connected".equals(response.getStatus())) {
                    return ResponseEntity.ok(response);
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
                }
            })
            .onErrorResume(e -> {
                log.error("Connection test failed", e);
                ApigeeConnectResponse errorResponse = ApigeeConnectResponse.builder()
                    .status("failed")
                    .message(e.getMessage())
                    .build();
                return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse));
            });
    }
    
    /**
     * Sync Apigee catalog (API Products, Proxies, Developers, Apps).
     */
    @PostMapping("/catalog/sync")
    @Operation(summary = "Sync Apigee catalog", 
               description = "Import API Products, Proxies, Developers and Apps from Apigee")
    public Mono<ResponseEntity<ApigeeCatalogSyncResponse>> syncCatalog(
            @RequestParam(required = false) String syncType) {
        
        log.info("Starting catalog sync, type: {}", syncType != null ? syncType : "full");
        
        return apigeeIntegrationService.syncCatalog(syncType)
            .map(response -> ResponseEntity.accepted().body(response))
            .onErrorResume(e -> {
                log.error("Catalog sync failed", e);
                ApigeeCatalogSyncResponse errorResponse = ApigeeCatalogSyncResponse.builder()
                    .status("FAILED")
                    .message(e.getMessage())
                    .build();
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse));
            });
    }
    
    /**
     * Ingest usage events from Apigee runtime.
     */
    @PostMapping("/ingest")
    @Operation(summary = "Ingest usage events", 
               description = "Receive and process usage events from Apigee proxies")
    public Mono<ResponseEntity<Map<String, Object>>> ingestEvents(
            @Valid @RequestBody Object eventData) {
        
        // Handle both single event and array of events
        List<ApigeeEvent> events;
        if (eventData instanceof List) {
            events = (List<ApigeeEvent>) eventData;
            log.info("Ingesting {} Apigee events", events.size());
        } else {
            events = List.of((ApigeeEvent) eventData);
            log.info("Ingesting single Apigee event");
        }
        
        return apigeeIntegrationService.ingestEvents(events)
            .map(result -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "accepted");
                response.put("eventsProcessed", result);
                return ResponseEntity.accepted().body(response);
            })
            .onErrorResume(e -> {
                log.error("Event ingestion failed", e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return Mono.just(ResponseEntity.badRequest().body(errorResponse));
            });
    }
    
    /**
     * Enforce pricing plans by mapping to Apigee API Products.
     */
    @PostMapping("/enforce/plans")
    @Operation(summary = "Enforce pricing plans", 
               description = "Map Aforo plans to Apigee API Products and update app entitlements")
    public Mono<ResponseEntity<Map<String, Object>>> enforcePlans(
            @Valid @RequestBody ApigeeEnforcePlanRequest request) {
        
        log.info("Enforcing {} plan mappings", request.getMappings().size());
        
        return apigeeIntegrationService.enforcePlans(request)
            .map(results -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("results", results);
                return ResponseEntity.ok(response);
            })
            .onErrorResume(e -> {
                log.error("Plan enforcement failed", e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
            });
    }
    
    /**
     * Suspend an Apigee developer app.
     */
    @PostMapping("/suspend")
    @Operation(summary = "Suspend developer app", 
               description = "Suspend or revoke an Apigee developer app for zero wallet balance")
    public Mono<ResponseEntity<Map<String, Object>>> suspendApp(
            @Valid @RequestBody ApigeeSuspendRequest request) {
        
        log.info("Suspending app {} for developer {} mode: {}", 
                request.getAppName(), request.getDeveloperId(), request.getMode());
        
        return apigeeIntegrationService.suspendApp(request)
            .map(v -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "suspended");
                response.put("developerId", request.getDeveloperId());
                response.put("appName", request.getAppName());
                response.put("mode", request.getMode());
                return ResponseEntity.ok(response);
            })
            .onErrorResume(e -> {
                log.error("App suspension failed", e);
                if (e.getMessage().contains("not found")) {
                    return Mono.just(ResponseEntity.notFound().build());
                }
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
            });
    }
    
    /**
     * Resume a suspended Apigee developer app.
     */
    @PostMapping("/resume")
    @Operation(summary = "Resume developer app", 
               description = "Resume a suspended Apigee developer app after wallet top-up")
    public Mono<ResponseEntity<Map<String, Object>>> resumeApp(
            @RequestParam String developerId,
            @RequestParam String appName,
            @RequestParam String consumerKey) {
        
        log.info("Resuming app {} for developer {}", appName, developerId);
        
        return apigeeIntegrationService.resumeApp(developerId, appName, consumerKey)
            .map(v -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "resumed");
                response.put("developerId", developerId);
                response.put("appName", appName);
                return ResponseEntity.ok(response);
            })
            .onErrorResume(e -> {
                log.error("App resume failed", e);
                if (e.getMessage().contains("not found")) {
                    return Mono.just(ResponseEntity.notFound().build());
                }
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
            });
    }
    
    /**
     * Health check for Apigee integration.
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", 
               description = "Check Apigee connectivity and integration status")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        
        log.debug("Performing Apigee health check");
        
        return apigeeIntegrationService.checkHealth()
            .map(health -> ResponseEntity.ok(health))
            .onErrorResume(e -> {
                log.error("Health check failed", e);
                return Mono.just(ResponseEntity.ok(Map.of(
                    "apigeeReachable", false,
                    "error", e.getMessage()
                )));
            });
    }
}
