package aforo.kong.service.impl;

import aforo.kong.dto.kong.HttpLogPayload;
import aforo.kong.entity.KongUsageRecord;
import aforo.kong.entity.KonnectServiceMap;
import aforo.kong.entity.KonnectRouteMap;
import aforo.kong.repository.KongUsageRecordRepository;
import aforo.kong.repository.KonnectServiceMapRepository;
import aforo.kong.repository.KonnectRouteMapRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class KongUsageIngestionService {
    
    private static final Logger logger = LoggerFactory.getLogger(KongUsageIngestionService.class);
    
    @Autowired
    private KongUsageRecordRepository usageRecordRepository;
    
    @Autowired
    private KonnectServiceMapRepository serviceMapRepository;
    
    @Autowired
    private KonnectRouteMapRepository routeMapRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Transactional
    public void ingestHttpLogPayload(Long orgId, Object payload) {
        try {
            // Handle batch or single payload
            if (payload instanceof List) {
                List<Object> batch = (List<Object>) payload;
                for (Object item : batch) {
                    processSinglePayload(orgId, item);
                }
            } else {
                processSinglePayload(orgId, payload);
            }
        } catch (Exception e) {
            logger.error("Failed to ingest usage data for org: {}", orgId, e);
            throw new RuntimeException("Failed to ingest usage data", e);
        }
    }
    
    private void processSinglePayload(Long orgId, Object payload) {
        try {
            // Convert to HttpLogPayload
            HttpLogPayload logPayload;
            if (payload instanceof HttpLogPayload) {
                logPayload = (HttpLogPayload) payload;
            } else {
                String json = objectMapper.writeValueAsString(payload);
                logPayload = objectMapper.readValue(json, HttpLogPayload.class);
            }
            
            // Validate required fields
            String kongRequestId = logPayload.getRequest() != null ? logPayload.getRequest().getId() : null;
            String path = logPayload.getRequest() != null ? logPayload.getRequest().getPath() : null;
            String method = logPayload.getRequest() != null ? logPayload.getRequest().getMethod() : null;
            Integer status = logPayload.getResponse() != null ? logPayload.getResponse().getStatus() : null;

            if (kongRequestId == null || path == null || method == null || status == null) {
                logger.warn("Skipping save — missing essential fields. reqId={}, path={}, method={}, status={}",
                        kongRequestId, path, method, status);
                return;
            }
            
            // Generate correlation ID for deduplication
            String correlationId = generateCorrelationId(orgId, logPayload);
            
            // Check for duplicate
            Optional<KongUsageRecord> existing = usageRecordRepository
                    .findByOrganizationIdAndCorrelationId(orgId, correlationId);
            
            if (existing.isPresent()) {
                logger.debug("Duplicate usage record detected, skipping: {}", correlationId);
                return;
            }
            
            // Create usage record
            KongUsageRecord record = new KongUsageRecord();
            record.setOrganizationId(orgId);
            record.setTimestamp(Instant.ofEpochMilli(logPayload.getStartedAt()));
            record.setKongRequestId(kongRequestId);
            record.setCorrelationId(correlationId);
            
            // Service info
            if (logPayload.getService() != null) {
                record.setKongServiceId(logPayload.getService().getId());
            }
            
            // Route info
            if (logPayload.getRoute() != null) {
                record.setKongRouteId(logPayload.getRoute().getId());
            }
            
            // Consumer info
            if (logPayload.getConsumer() != null) {
                record.setKongConsumerId(logPayload.getConsumer().getId());
            }
            
            // Request info
            if (logPayload.getRequest() != null) {
                record.setHttpMethod(logPayload.getRequest().getMethod());
                record.setPath(logPayload.getRequest().getPath());
                record.setRequestSize(logPayload.getRequest().getSize() != null ? 
                        logPayload.getRequest().getSize().longValue() : 0L);
            }
            
            // Response info
            if (logPayload.getResponse() != null) {
                record.setStatus(logPayload.getResponse().getStatus());
                record.setResponseSize(logPayload.getResponse().getSize() != null ? 
                        logPayload.getResponse().getSize().longValue() : 0L);
            }
            
            // Latency
            if (logPayload.getLatencies() != null && logPayload.getLatencies().getProxy() != null) {
                record.setLatencyMs(logPayload.getLatencies().getProxy());
            }
            
            // Store raw payload for debugging
            record.setRawPayload(objectMapper.writeValueAsString(payload));
            
            // Save record
            usageRecordRepository.save(record);
            
            // Async processing for mapping resolution
            resolveAforoMappingsAsync(record);
            
            logger.debug("Ingested usage record for org: {}, correlation: {}", orgId, correlationId);
            
        } catch (Exception e) {
            logger.error("Failed to process single payload for org: {}", orgId, e);
        }
    }
    
    @Async
    protected void resolveAforoMappingsAsync(KongUsageRecord record) {
        try {
            boolean updated = false;
            
            // Resolve service mapping
            if (record.getKongServiceId() != null) {
                Optional<KonnectServiceMap> serviceMap = serviceMapRepository
                        .findByOrganizationIdAndControlPlaneIdAndKongServiceId(
                                record.getOrganizationId(), 
                                "default", // TODO: Extract control plane ID properly
                                record.getKongServiceId());
                
                if (serviceMap.isPresent() && serviceMap.get().getAforoProductId() != null) {
                    record.setAforoProductId(serviceMap.get().getAforoProductId());
                    updated = true;
                }
            }
            
            // Resolve route mapping
            if (record.getKongRouteId() != null) {
                Optional<KonnectRouteMap> routeMap = routeMapRepository
                        .findByOrganizationIdAndControlPlaneIdAndKongRouteId(
                                record.getOrganizationId(),
                                "default", // TODO: Extract control plane ID properly
                                record.getKongRouteId());
                
                if (routeMap.isPresent() && routeMap.get().getAforoEndpointId() != null) {
                    record.setAforoEndpointId(routeMap.get().getAforoEndpointId());
                    updated = true;
                }
            }
            
            // TODO: Resolve consumer mapping when implemented
            
            if (updated) {
                record.setProcessed(true);
                usageRecordRepository.save(record);
            }
            
        } catch (Exception e) {
            logger.error("Failed to resolve Aforo mappings for record: {}", record.getId(), e);
        }
    }
    
    private String generateCorrelationId(Long orgId, HttpLogPayload payload) {
        try {
            String uniqueString;
            
            // Use request.id if available
            if (payload.getRequest() != null && payload.getRequest().getId() != null && !payload.getRequest().getId().isEmpty()) {
                uniqueString = payload.getRequest().getId();
            } else {
                // Fallback to hash of key fields
                StringBuilder sb = new StringBuilder();
                sb.append(payload.getStartedAt());
                
                if (payload.getConsumer() != null && payload.getConsumer().getId() != null) {
                    sb.append("|").append(payload.getConsumer().getId());
                }
                
                if (payload.getRoute() != null && payload.getRoute().getId() != null) {
                    sb.append("|").append(payload.getRoute().getId());
                }
                
                if (payload.getRequest() != null) {
                    sb.append("|").append(payload.getRequest().getMethod());
                    sb.append("|").append(payload.getRequest().getPath());
                }
                
                uniqueString = sb.toString();
            }
            
            // Generate SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(uniqueString.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (Exception e) {
            logger.error("Failed to generate correlation ID", e);
            return String.valueOf(System.nanoTime());
        }
    }
}
