package aforo.kong.service.impl;

import aforo.kong.dto.*;
import aforo.kong.entity.*;
import aforo.kong.repository.*;
import aforo.kong.service.KongIntegrationService;
import aforo.kong.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class KongIntegrationServiceImpl implements KongIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(KongIntegrationServiceImpl.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ClientApiDetailsRepository clientApiDetailsRepository;
    @SuppressWarnings("unused") // Will be used in TODO implementations
    private final KongServiceRepository serviceRepository;
    @SuppressWarnings("unused") // Will be used in TODO implementations
    private final KongRouteRepository routeRepository;
    private final KongConsumerRepository consumerRepository;
    private final UsageRecordRepository usageRecordRepository;
    @SuppressWarnings("unused") // Will be used in TODO implementations
    private final PricingPlanRepository pricingPlanRepository;
    
    @Value("${aforo.base-url:http://localhost:8080}")
    private String aforoBaseUrl;
    
    public KongIntegrationServiceImpl(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            ClientApiDetailsRepository clientApiDetailsRepository,
            KongServiceRepository serviceRepository,
            KongRouteRepository routeRepository,
            KongConsumerRepository consumerRepository,
            UsageRecordRepository usageRecordRepository,
            PricingPlanRepository pricingPlanRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.clientApiDetailsRepository = clientApiDetailsRepository;
        this.serviceRepository = serviceRepository;
        this.routeRepository = routeRepository;
        this.consumerRepository = consumerRepository;
        this.usageRecordRepository = usageRecordRepository;
        this.pricingPlanRepository = pricingPlanRepository;
    }
    
    @Override
    public ConnectResponseDTO connect(ConnectRequestDTO request) {
        Long organizationId = TenantContext.require();
        logger.info("Connecting to Kong for organization: {}", organizationId);
        
        ConnectResponseDTO response = new ConnectResponseDTO();
        
        try {
            // Test connection
            testConnection(request.getAdminApiUrl(), request.getToken());
            
            // Save connection details
            ClientApiDetails apiDetails = new ClientApiDetails();
            // apiDetails.setOrganizationId(organizationId); // Temporarily disabled
            apiDetails.setBaseUrl(request.getAdminApiUrl());
            apiDetails.setAuthToken(request.getToken());
            apiDetails.setName("Kong Connection");
            apiDetails.setDescription("Kong Konnect Connection");
            apiDetails.setEndpoint("/services");
            // apiDetails.setEnvironment(request.getEnvironment()); // Temporarily disabled
            // apiDetails.setAdditionalConfig(objectMapper.writeValueAsString(request)); // Temporarily disabled
            clientApiDetailsRepository.save(apiDetails);
            
            // Perform initial catalog sync
            CatalogSyncResponseDTO syncResult = syncCatalog(apiDetails.getId());
            
            response.setConnectionId(apiDetails.getId().toString());
            response.setStatus("connected");
            response.setServicesDiscovered(syncResult.getServices() != null ? syncResult.getServices().getFetched() : 0);
            response.setRoutesDiscovered(syncResult.getRoutes() != null ? syncResult.getRoutes().getFetched() : 0);
            response.setConsumersDiscovered(syncResult.getConsumers() != null ? syncResult.getConsumers().getFetched() : 0);
            response.setWebhookUrl(aforoBaseUrl + "/integrations/kong/events");
            response.setIngestUrl(aforoBaseUrl + "/integrations/kong/ingest");
            response.setMessage("Successfully connected to Kong and imported catalog");
            
        } catch (Exception e) {
            logger.error("Failed to connect to Kong", e);
            response.setStatus("failed");
            response.setMessage("Connection failed: " + e.getMessage());
        }
        
        return response;
    }
    
    @Override
    public void testConnection(String adminApiUrl, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                adminApiUrl + "/services",
                HttpMethod.GET,
                entity,
                String.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Kong API returned status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to Kong Admin API: " + e.getMessage(), e);
        }
    }
    
    @Override
    public CatalogSyncResponseDTO syncCatalog(Long clientDetailsId) {
        CatalogSyncResponseDTO response = new CatalogSyncResponseDTO();
        response.setSyncStartTime(Instant.now());
        response.setStatus(CatalogSyncResponseDTO.SyncStatus.IN_PROGRESS);
        
        List<CatalogSyncResponseDTO.SyncError> errors = new ArrayList<>();
        
        try {
            // Sync services
            CatalogSyncResponseDTO servicesSync = syncServices(clientDetailsId);
            response.setServices(servicesSync.getServices());
            
            // Sync routes
            CatalogSyncResponseDTO routesSync = syncRoutes(clientDetailsId);
            response.setRoutes(routesSync.getRoutes());
            
            // Sync consumers
            CatalogSyncResponseDTO consumersSync = syncConsumers(clientDetailsId);
            response.setConsumers(consumersSync.getConsumers());
            
            response.setErrors(errors);
            response.setStatus(errors.isEmpty() ? 
                CatalogSyncResponseDTO.SyncStatus.COMPLETED : 
                CatalogSyncResponseDTO.SyncStatus.PARTIAL);
            
        } catch (Exception e) {
            logger.error("Catalog sync failed", e);
            response.setStatus(CatalogSyncResponseDTO.SyncStatus.FAILED);
        }
        
        response.setSyncEndTime(Instant.now());
        response.setDurationMs(response.getSyncEndTime().toEpochMilli() - response.getSyncStartTime().toEpochMilli());
        
        return response;
    }
    
    @Override
    public CatalogSyncResponseDTO syncServices(Long clientDetailsId) {
        // TODO: Implement service sync
        CatalogSyncResponseDTO response = new CatalogSyncResponseDTO();
        CatalogSyncResponseDTO.SyncStats stats = new CatalogSyncResponseDTO.SyncStats();
        stats.setFetched(0);
        stats.setCreated(0);
        stats.setUpdated(0);
        response.setServices(stats);
        return response;
    }
    
    @Override
    public CatalogSyncResponseDTO syncRoutes(Long clientDetailsId) {
        // TODO: Implement route sync
        CatalogSyncResponseDTO response = new CatalogSyncResponseDTO();
        CatalogSyncResponseDTO.SyncStats stats = new CatalogSyncResponseDTO.SyncStats();
        stats.setFetched(0);
        stats.setCreated(0);
        stats.setUpdated(0);
        response.setRoutes(stats);
        return response;
    }
    
    @Override
    public CatalogSyncResponseDTO syncConsumers(Long clientDetailsId) {
        // TODO: Implement consumer sync
        CatalogSyncResponseDTO response = new CatalogSyncResponseDTO();
        CatalogSyncResponseDTO.SyncStats stats = new CatalogSyncResponseDTO.SyncStats();
        stats.setFetched(0);
        stats.setCreated(0);
        stats.setUpdated(0);
        response.setConsumers(stats);
        return response;
    }
    
    @Override
    public void ingestUsageEvent(KongEventDTO event) {
        UsageRecord record = processUsageEvent(event);
        if (record != null) {
            usageRecordRepository.save(record);
            logger.debug("Ingested usage event: {}", record.getCorrelationId());
        }
    }
    
    @Override
    public void ingestUsageEvents(List<KongEventDTO> events) {
        List<UsageRecord> records = events.stream()
            .map(this::processUsageEvent)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        if (!records.isEmpty()) {
            usageRecordRepository.saveAll(records);
            logger.info("Ingested {} usage events", records.size());
        }
    }
    
    @Override
    public UsageRecord processUsageEvent(KongEventDTO event) {
        Long organizationId = TenantContext.require();
        
        // Check for duplicate by correlation ID
        if (event.getCorrelationId() != null) {
            if (usageRecordRepository.existsByCorrelationIdAndOrganizationId(event.getCorrelationId(), organizationId)) {
                logger.debug("Skipping duplicate event with correlation ID: {}", event.getCorrelationId());
                return null;
            }
        }
        
        UsageRecord record = new UsageRecord();
        record.setOrganizationId(organizationId);
        record.setCorrelationId(event.getCorrelationId());
        record.setKongRequestId(event.getKongRequestId());
        record.setTimestamp(event.getTimestamp() != null ? event.getTimestamp() : Instant.now());
        
        // Set service information
        if (event.getService() != null) {
            record.setServiceId(event.getService().getId());
            record.setServiceName(event.getService().getName());
        }
        
        // Set route information
        if (event.getRoute() != null) {
            record.setRouteId(event.getRoute().getId());
            record.setRouteName(event.getRoute().getName());
        }
        
        // Set consumer information
        if (event.getConsumer() != null) {
            record.setConsumerId(event.getConsumer().getId());
            record.setConsumerUsername(event.getConsumer().getUsername());
            record.setConsumerCustomId(event.getConsumer().getCustomId());
        }
        
        // Set request information
        if (event.getRequest() != null) {
            record.setRequestMethod(event.getRequest().getMethod());
            record.setRequestPath(event.getRequest().getPath());
            record.setRequestSize(event.getRequest().getSize());
        }
        
        // Set response information
        if (event.getResponse() != null) {
            record.setResponseStatus(event.getResponse().getStatus());
            record.setResponseSize(event.getResponse().getSize());
        }
        
        // Set latencies
        if (event.getLatencies() != null) {
            record.setResponseLatency(event.getLatencies().getRequest());
            record.setKongLatency(event.getLatencies().getKong());
            record.setUpstreamLatency(event.getLatencies().getProxy());
        }
        
        // Set client information
        record.setClientIp(event.getClientIp());
        
        // Set default billing metrics
        record.setMetricType("calls");
        record.setBillableUnits(1.0);
        record.setProcessed(false);
        record.setBilled(false);
        
        return record;
    }
    
    @Override
    public void processEventHook(KongCrudEventDTO event) {
        logger.info("Processing event hook: {} - {} - {}", event.getSource(), event.getEvent(), event.getEntity());
        // TODO: Implement event hook processing
    }
    
    @Override
    public void enforceRateLimits(EnforceGroupsRequestDTO request) {
        Long organizationId = TenantContext.require();
        logger.info("Enforcing rate limits for organization: {}", organizationId);
        // TODO: Implement rate limit enforcement via Kong Admin API
    }
    
    @Override
    public void suspendConsumer(SuspendRequestDTO request) {
        Long organizationId = TenantContext.require();
        
        KongConsumer consumer = consumerRepository.findByIdAndOrganizationId(request.getConsumerId(), organizationId)
            .orElseThrow(() -> new RuntimeException("Consumer not found"));
        
        consumer.setStatus("suspended");
        consumer.setLastEnforcementPush(Instant.now());
        consumerRepository.save(consumer);
        
        logger.info("Suspended consumer: {} - mode: {}", request.getConsumerId(), request.getMode());
        // TODO: Implement suspension via Kong Admin API
    }
    
    @Override
    public void resumeConsumer(String consumerId) {
        Long organizationId = TenantContext.require();
        
        KongConsumer consumer = consumerRepository.findByIdAndOrganizationId(consumerId, organizationId)
            .orElseThrow(() -> new RuntimeException("Consumer not found"));
        
        consumer.setStatus("active");
        consumer.setLastEnforcementPush(Instant.now());
        consumerRepository.save(consumer);
        
        logger.info("Resumed consumer: {}", consumerId);
        // TODO: Implement resume via Kong Admin API
    }
    
    @Override
    public void createConsumerGroup(String groupName, Long organizationId) {
        logger.info("Creating consumer group: {} for organization: {}", groupName, organizationId);
        // TODO: Implement Kong Admin API call
    }
    
    @Override
    public void addConsumerToGroup(String consumerId, String groupName) {
        logger.info("Adding consumer {} to group {}", consumerId, groupName);
        // TODO: Implement Kong Admin API call
    }
    
    @Override
    public void removeConsumerFromGroup(String consumerId, String groupName) {
        logger.info("Removing consumer {} from group {}", consumerId, groupName);
        // TODO: Implement Kong Admin API call
    }
    
    @Override
    public void updateGroupRateLimits(String groupName, List<EnforceGroupsRequestDTO.RateLimit> limits) {
        logger.info("Updating rate limits for group {}: {}", groupName, limits);
        // TODO: Implement Kong Admin API call
    }
}
