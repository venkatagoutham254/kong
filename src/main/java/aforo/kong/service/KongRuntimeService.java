package aforo.kong.service;

import aforo.kong.dto.konnect.KonnectRuntimeSyncPreviewDTO;
import aforo.kong.dto.konnect.KonnectServiceDTO;
import aforo.kong.dto.konnect.KonnectRouteDTO;

import java.util.List;

public interface KongRuntimeService {
    
    // Services
    List<KonnectServiceDTO> fetchServices(Long orgId);
    KonnectRuntimeSyncPreviewDTO previewRuntimeSync(Long orgId);
    void applyRuntimeSync(Long orgId);
    
    // Routes  
    List<KonnectRouteDTO> fetchRoutes(Long orgId);
    
    // Consumers
    List<Object> fetchConsumers(Long orgId);
    List<Object> fetchConsumerGroups(Long orgId);
    void importConsumers(Long orgId, List<String> consumerIds);
    void syncConsumers(Long orgId);
    
    // Usage Ingestion
    void ingestUsageData(Long orgId, Object usagePayload);
    
    // Enforcement
    void enforceRateLimits(Long orgId, String planId, String groupId, Object limits);
    void suspendConsumer(Long orgId, String consumerId);
    void resumeConsumer(Long orgId, String consumerId);
    
    // Health
    Object getIntegrationHealth(Long orgId);
}
