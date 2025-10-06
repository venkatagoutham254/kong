package aforo.kong.controller;

import aforo.kong.dto.*;
import aforo.kong.service.KongIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/integrations/kong")
@Tag(name = "Kong Integration", description = "Kong Gateway integration API for catalog sync, usage ingestion, and enforcement")
@SecurityRequirement(name = "bearerAuth")
public class KongIntegrationController {
    
    private static final Logger logger = LoggerFactory.getLogger(KongIntegrationController.class);
    
    private final KongIntegrationService integrationService;
    
    public KongIntegrationController(KongIntegrationService integrationService) {
        this.integrationService = integrationService;
    }
    
    /**
     * Connect to Kong Gateway or Konnect
     */
    @PostMapping("/connect")
    @Operation(summary = "Connect to Kong", 
               description = "Establishes connection to Kong Gateway or Konnect and performs initial catalog sync")
    public ResponseEntity<ConnectResponseDTO> connect(@Valid @RequestBody ConnectRequestDTO request) {
        logger.info("Connecting to Kong environment: {}", request.getEnvironment());
        ConnectResponseDTO response = integrationService.connect(request);
        
        if ("failed".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Trigger catalog sync
     */
    @PostMapping("/catalog/sync")
    @Operation(summary = "Sync catalog", 
               description = "Triggers on-demand synchronization of Kong services, routes, and consumers")
    public ResponseEntity<CatalogSyncResponseDTO> syncCatalog(
            @Parameter(description = "Client API details ID") @RequestParam Long clientDetailsId) {
        logger.info("Triggering catalog sync for client details: {}", clientDetailsId);
        CatalogSyncResponseDTO response = integrationService.syncCatalog(clientDetailsId);
        return ResponseEntity.accepted().body(response);
    }
    
    /**
     * Ingest usage events from Kong HTTP Log plugin
     */
    @PostMapping("/ingest")
    @Operation(summary = "Ingest usage events", 
               description = "Receives HTTP Log events from Kong (single or batched)")
    public ResponseEntity<Void> ingestUsageEvents(@RequestBody Object payload) {
        try {
            if (payload instanceof List) {
                // Batch of events
                @SuppressWarnings("unchecked")
                List<KongEventDTO> events = (List<KongEventDTO>) payload;
                logger.info("Received batch of {} usage events", events.size());
                integrationService.ingestUsageEvents(events);
            } else {
                // Single event
                KongEventDTO event = (KongEventDTO) payload;
                logger.debug("Received single usage event");
                integrationService.ingestUsageEvent(event);
            }
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            logger.error("Failed to ingest usage events", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    /**
     * Receive Kong Event Hooks (CRUD and rate limit events)
     */
    @PostMapping("/events")
    @Operation(summary = "Process event hooks", 
               description = "Receives Kong Event Hooks for CRUD operations and rate limit exceeded events")
    public ResponseEntity<Void> processEventHook(@RequestBody KongCrudEventDTO event) {
        logger.info("Received event hook: {} - {} - {}", event.getSource(), event.getEvent(), event.getEntity());
        try {
            integrationService.processEventHook(event);
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            logger.error("Failed to process event hook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    /**
     * Map pricing plans to consumer groups and push rate limits
     */
    @PostMapping("/enforce/groups")
    @Operation(summary = "Enforce rate limits", 
               description = "Maps pricing plans to consumer groups and pushes rate limit configurations to Kong")
    public ResponseEntity<Void> enforceGroups(@Valid @RequestBody EnforceGroupsRequestDTO request) {
        logger.info("Enforcing rate limits for {} plan mappings", request.getMappings().size());
        try {
            integrationService.enforceRateLimits(request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Failed to enforce rate limits", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
    
    /**
     * Suspend a consumer
     */
    @PostMapping("/suspend")
    @Operation(summary = "Suspend consumer", 
               description = "Suspends a consumer by moving to suspended group or adding request-termination plugin")
    public ResponseEntity<Void> suspendConsumer(@Valid @RequestBody SuspendRequestDTO request) {
        logger.info("Suspending consumer: {} with mode: {}", request.getConsumerId(), request.getMode());
        try {
            integrationService.suspendConsumer(request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Failed to suspend consumer", e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Resume a suspended consumer
     */
    @PostMapping("/resume/{consumerId}")
    @Operation(summary = "Resume consumer", 
               description = "Resumes a suspended consumer by restoring their original group and removing termination plugins")
    public ResponseEntity<Void> resumeConsumer(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId) {
        logger.info("Resuming consumer: {}", consumerId);
        try {
            integrationService.resumeConsumer(consumerId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Failed to resume consumer", e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", 
               description = "Returns connectivity and hook status for Kong integration")
    public ResponseEntity<HealthStatus> health() {
        HealthStatus status = new HealthStatus();
        status.setKongReachable(true); // TODO: Implement actual health check
        status.setStatus("healthy");
        return ResponseEntity.ok(status);
    }
    
    @Data
    public static class HealthStatus {
        private boolean kongReachable;
        private String status;
        private List<String> activeHooks;
        private Instant lastSync;
    }
}
