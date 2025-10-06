package aforo.kong.controller;

import aforo.kong.service.UsageProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/kong/analytics")
@Tag(name = "Kong Analytics", description = "Analytics and reporting for Kong API usage")
@SecurityRequirement(name = "bearerAuth")
public class KongAnalyticsController {
    
    private final UsageProcessingService usageProcessingService;
    
    public KongAnalyticsController(UsageProcessingService usageProcessingService) {
        this.usageProcessingService = usageProcessingService;
    }
    
    /**
     * Get billing summary for a consumer
     */
    @GetMapping("/billing/{consumerId}")
    @Operation(summary = "Get billing summary", 
               description = "Returns billing summary for a consumer in the specified time period")
    public ResponseEntity<Map<String, Object>> getBillingSummary(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Start time") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @Parameter(description = "End time") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        
        if (startTime == null) {
            startTime = Instant.now().minus(30, ChronoUnit.DAYS);
        }
        if (endTime == null) {
            endTime = Instant.now();
        }
        
        Map<String, Object> summary = usageProcessingService.generateBillingSummary(consumerId, startTime, endTime);
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Get usage statistics for a consumer
     */
    @GetMapping("/usage/{consumerId}")
    @Operation(summary = "Get usage statistics", 
               description = "Returns detailed usage statistics for a consumer")
    public ResponseEntity<Map<String, Object>> getUsageStatistics(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Start time") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @Parameter(description = "End time") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        
        if (startTime == null) {
            startTime = Instant.now().minus(7, ChronoUnit.DAYS);
        }
        if (endTime == null) {
            endTime = Instant.now();
        }
        
        Map<String, Object> stats = usageProcessingService.getUsageStatistics(consumerId, startTime, endTime);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get top consumers by usage
     */
    @GetMapping("/top-consumers")
    @Operation(summary = "Get top consumers", 
               description = "Returns top consumers by API usage")
    public ResponseEntity<List<Map<String, Object>>> getTopConsumers(
            @Parameter(description = "Number of results") @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "Start time") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @Parameter(description = "End time") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        
        if (startTime == null) {
            startTime = Instant.now().minus(30, ChronoUnit.DAYS);
        }
        if (endTime == null) {
            endTime = Instant.now();
        }
        
        List<Map<String, Object>> topConsumers = usageProcessingService.getTopConsumersByUsage(limit, startTime, endTime);
        return ResponseEntity.ok(topConsumers);
    }
    
    /**
     * Get top services by usage
     */
    @GetMapping("/top-services")
    @Operation(summary = "Get top services", 
               description = "Returns top services by API usage")
    public ResponseEntity<List<Map<String, Object>>> getTopServices(
            @Parameter(description = "Number of results") @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "Start time") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @Parameter(description = "End time") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        
        if (startTime == null) {
            startTime = Instant.now().minus(30, ChronoUnit.DAYS);
        }
        if (endTime == null) {
            endTime = Instant.now();
        }
        
        List<Map<String, Object>> topServices = usageProcessingService.getTopServicesByUsage(limit, startTime, endTime);
        return ResponseEntity.ok(topServices);
    }
    
    /**
     * Check if consumer is approaching quota
     */
    @GetMapping("/quota-check/{consumerId}")
    @Operation(summary = "Check quota status", 
               description = "Checks if a consumer is approaching their quota limits")
    public ResponseEntity<Map<String, Object>> checkQuotaStatus(
            @Parameter(description = "Consumer ID") @PathVariable String consumerId,
            @Parameter(description = "Threshold percentage") @RequestParam(defaultValue = "80") double threshold) {
        
        boolean approaching = usageProcessingService.isApproachingQuota(consumerId, threshold);
        
        return ResponseEntity.ok(Map.of(
            "consumerId", consumerId,
            "threshold", threshold,
            "approachingQuota", approaching
        ));
    }
    
    /**
     * Top up consumer wallet
     */
    @PostMapping("/wallet/topup")
    @Operation(summary = "Top up wallet", 
               description = "Adds credits to a consumer's prepaid wallet")
    public ResponseEntity<Map<String, Object>> topUpWallet(
            @RequestBody TopUpRequest request) {
        
        usageProcessingService.topUpWallet(request.getConsumerId(), request.getAmount());
        
        return ResponseEntity.ok(Map.of(
            "consumerId", request.getConsumerId(),
            "amount", request.getAmount(),
            "status", "success"
        ));
    }
    
    @lombok.Data
    public static class TopUpRequest {
        private String consumerId;
        private double amount;
    }
}
