package aforo.kong.service;

import aforo.kong.dto.*;
import aforo.kong.entity.UsageRecord;

import java.util.List;

public interface KongIntegrationService {
    
    // Connection management
    ConnectResponseDTO connect(ConnectRequestDTO request);
    void testConnection(String adminApiUrl, String token);
    
    // Catalog sync
    CatalogSyncResponseDTO syncCatalog(Long clientDetailsId);
    CatalogSyncResponseDTO syncServices(Long clientDetailsId);
    CatalogSyncResponseDTO syncRoutes(Long clientDetailsId);
    CatalogSyncResponseDTO syncConsumers(Long clientDetailsId);
    
    // Usage ingestion
    void ingestUsageEvent(KongEventDTO event);
    void ingestUsageEvents(List<KongEventDTO> events);
    UsageRecord processUsageEvent(KongEventDTO event);
    
    // Event hooks
    void processEventHook(KongCrudEventDTO event);
    
    // Enforcement
    void enforceRateLimits(EnforceGroupsRequestDTO request);
    void suspendConsumer(SuspendRequestDTO request);
    void resumeConsumer(String consumerId);
    
    // Consumer group management
    void createConsumerGroup(String groupName, Long organizationId);
    void addConsumerToGroup(String consumerId, String groupName);
    void removeConsumerFromGroup(String consumerId, String groupName);
    void updateGroupRateLimits(String groupName, List<EnforceGroupsRequestDTO.RateLimit> limits);
}
