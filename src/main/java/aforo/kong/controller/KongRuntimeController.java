package aforo.kong.controller;

import aforo.kong.dto.konnect.KonnectRuntimeSyncPreviewDTO;
import aforo.kong.dto.konnect.KonnectServiceDTO;
import aforo.kong.dto.konnect.KonnectRouteDTO;
import aforo.kong.service.KongRuntimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/integrations")
@Tag(name = "Kong Runtime", description = "Kong Runtime Integration APIs")
public class KongRuntimeController {
    
    @Autowired
    private KongRuntimeService kongRuntimeService;
    
    @Value("${kong.ingest.secret}")
    private String kongIngestSecret;
    
    // Services & Routes
    @GetMapping("/konnect/services")
    @Operation(summary = "Fetch services from Konnect")
    public ResponseEntity<List<KonnectServiceDTO>> getServices(
            @RequestHeader("X-Organization-Id") Long orgId) {
        List<KonnectServiceDTO> services = kongRuntimeService.fetchServices(orgId);
        return ResponseEntity.ok(services);
    }
    
    @GetMapping("/konnect/routes")
    @Operation(summary = "Fetch routes from Konnect")
    public ResponseEntity<List<KonnectRouteDTO>> getRoutes(
            @RequestHeader("X-Organization-Id") Long orgId) {
        List<KonnectRouteDTO> routes = kongRuntimeService.fetchRoutes(orgId);
        return ResponseEntity.ok(routes);
    }
    
    @PostMapping("/konnect/runtime/preview")
    @Operation(summary = "Preview runtime sync changes")
    public ResponseEntity<KonnectRuntimeSyncPreviewDTO> previewRuntimeSync(
            @RequestHeader("X-Organization-Id") Long orgId) {
        KonnectRuntimeSyncPreviewDTO preview = kongRuntimeService.previewRuntimeSync(orgId);
        return ResponseEntity.ok(preview);
    }
    
    @PostMapping("/konnect/runtime/apply")
    @Operation(summary = "Apply runtime sync")
    public ResponseEntity<Map<String, String>> applyRuntimeSync(
            @RequestHeader("X-Organization-Id") Long orgId) {
        kongRuntimeService.applyRuntimeSync(orgId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Runtime sync applied"));
    }
    
    // Consumers
    @GetMapping("/konnect/consumers")
    @Operation(summary = "Fetch consumers from Konnect")
    public ResponseEntity<List<Object>> getConsumers(
            @RequestHeader("X-Organization-Id") Long orgId) {
        List<Object> consumers = kongRuntimeService.fetchConsumers(orgId);
        return ResponseEntity.ok(consumers);
    }
    
    @PostMapping("/konnect/consumers/import")
    @Operation(summary = "Import selected consumers")
    public ResponseEntity<Map<String, String>> importConsumers(
            @RequestHeader("X-Organization-Id") Long orgId,
            @RequestBody Map<String, List<String>> request) {
        kongRuntimeService.importConsumers(orgId, request.get("consumer_ids"));
        return ResponseEntity.ok(Map.of("status", "success"));
    }
    
    @GetMapping("/konnect/consumer-groups")
    @Operation(summary = "Fetch consumer groups from Konnect")
    public ResponseEntity<List<Object>> getConsumerGroups(
            @RequestHeader("X-Organization-Id") Long orgId) {
        List<Object> groups = kongRuntimeService.fetchConsumerGroups(orgId);
        return ResponseEntity.ok(groups);
    }
    
    @PostMapping("/konnect/customers/preview")
    @Operation(summary = "Preview customer sync")
    public ResponseEntity<Object> previewCustomerSync(
            @RequestHeader("X-Organization-Id") Long orgId) {
        // To be implemented
        return ResponseEntity.ok(Map.of("added", List.of(), "removed", List.of(), "changed", List.of()));
    }
    
    @PostMapping("/konnect/customers/apply")
    @Operation(summary = "Apply customer sync")
    public ResponseEntity<Map<String, String>> applyCustomerSync(
            @RequestHeader("X-Organization-Id") Long orgId) {
        kongRuntimeService.syncConsumers(orgId);
        return ResponseEntity.ok(Map.of("status", "success"));
    }
    
    // Usage Ingestion
    @PostMapping("/kong/ingest")
    @Operation(summary = "Ingest usage data from Kong HTTP Log plugin")
    public ResponseEntity<Map<String, String>> ingestUsage(
            @RequestHeader(value = "X-Organization-Id", required = false) Long orgId,
            @RequestHeader(value = "X-Integration-Secret", required = false) String integrationSecret,
            @RequestBody Object payload) {
        
        // Validate integration secret (machine-to-machine auth)
        if (integrationSecret == null || !integrationSecret.equals(kongIngestSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Invalid or missing integration secret"));
        }

        
        // Determine org from payload or header
        if (orgId == null) {
            // Extract from payload if not in header
            orgId = 27L; // Default for now
        }
        
        kongRuntimeService.ingestUsageData(orgId, payload);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("status", "accepted", "message", "Usage data queued for processing"));
    }
    
    // Enforcement
    @PostMapping("/kong/enforce/groups")
    @Operation(summary = "Enforce rate limits on consumer groups")
    public ResponseEntity<Map<String, String>> enforceGroupLimits(
            @RequestHeader("X-Organization-Id") Long orgId,
            @RequestBody Map<String, Object> request) {
        String planId = (String) request.get("plan_id");
        String groupId = (String) request.get("group_id");
        Object limits = request.get("limits");
        
        kongRuntimeService.enforceRateLimits(orgId, planId, groupId, limits);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Rate limits applied"));
    }
    
    @PostMapping("/kong/suspend")
    @Operation(summary = "Suspend a consumer")
    public ResponseEntity<Map<String, String>> suspendConsumer(
            @RequestHeader("X-Organization-Id") Long orgId,
            @RequestBody Map<String, String> request) {
        String consumerId = request.get("consumer_id");
        kongRuntimeService.suspendConsumer(orgId, consumerId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Consumer suspended"));
    }
    
    // Health
    @GetMapping("/kong/health")
    @Operation(summary = "Check Kong integration health")
    public ResponseEntity<Object> getHealth(
            @RequestHeader("X-Organization-Id") Long orgId) {
        Object health = kongRuntimeService.getIntegrationHealth(orgId);
        return ResponseEntity.ok(health);
    }
}
