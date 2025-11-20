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
            apiDetails.setOrganizationId(organizationId);
            apiDetails.setBaseUrl(request.getAdminApiUrl());
            apiDetails.setAuthToken(request.getToken());
            apiDetails.setName("Kong Connection");
            apiDetails.setDescription("Kong Konnect Connection");
            apiDetails.setEndpoint("/services");
            apiDetails.setEnvironment(request.getEnvironment());
            apiDetails.setWorkspace(request.getWorkspace());
            apiDetails.setConnectionStatus("connected");
            apiDetails.setCreatedAt(Instant.now());
            apiDetails.setUpdatedAt(Instant.now());
            clientApiDetailsRepository.save(apiDetails);
            
            response.setConnectionId(apiDetails.getId().toString());
            response.setStatus("connected");
            response.setMessage("Successfully connected to Kong");
            response.setServicesDiscovered(0);
            response.setRoutesDiscovered(0);
            response.setConsumersDiscovered(0);
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
        logger.info("Syncing services for client details ID: {}", clientDetailsId);
        Long organizationId = TenantContext.require();
        
        CatalogSyncResponseDTO response = new CatalogSyncResponseDTO();
        CatalogSyncResponseDTO.SyncStats stats = new CatalogSyncResponseDTO.SyncStats();
        
        try {
            // Get client API details
            ClientApiDetails apiDetails = clientApiDetailsRepository.findById(clientDetailsId)
                .orElseThrow(() -> new RuntimeException("Client API details not found"));
            
            // Fetch services from Kong
            String url = apiDetails.getBaseUrl() + "/services";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiDetails.getAuthToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> kongResponse = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (kongResponse.getStatusCode().is2xxSuccessful() && kongResponse.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> services = (List<Map<String, Object>>) kongResponse.getBody().get("data");
                
                if (services != null) {
                    stats.setFetched(services.size());
                    int created = 0;
                    int updated = 0;
                    
                    for (Map<String, Object> serviceData : services) {
                        String serviceId = (String) serviceData.get("id");
                        String serviceName = (String) serviceData.get("name");
                        
                        // Check if service already exists
                        Optional<KongService> existingService = serviceRepository.findByIdAndOrganizationId(serviceId, organizationId);
                        
                        KongService service;
                        if (existingService.isPresent()) {
                            service = existingService.get();
                            updated++;
                        } else {
                            service = new KongService();
                            service.setId(serviceId);
                            service.setOrganizationId(organizationId);
                            created++;
                        }
                        
                        service.setName(serviceName);
                        service.setHost((String) serviceData.get("host"));
                        service.setPath((String) serviceData.get("path"));
                        service.setProtocol((String) serviceData.get("protocol"));
                        
                        Object portObj = serviceData.get("port");
                        if (portObj instanceof Number) {
                            service.setPort(((Number) portObj).intValue());
                        }
                        
                        service.setEnabled((Boolean) serviceData.getOrDefault("enabled", true));
                        // service.setLastSyncTime(Instant.now()); // TODO: Add this field to entity
                        
                        serviceRepository.save(service);
                    }
                    
                    stats.setCreated(created);
                    stats.setUpdated(updated);
                    logger.info("Synced {} services: {} created, {} updated", services.size(), created, updated);
                } else {
                    stats.setFetched(0);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to sync services", e);
            stats.setFailed(stats.getFetched());
        }
        
        response.setServices(stats);
        return response;
    }
    
    @Override
    public CatalogSyncResponseDTO syncRoutes(Long clientDetailsId) {
        logger.info("Syncing routes for client details ID: {}", clientDetailsId);
        Long organizationId = TenantContext.require();
        
        CatalogSyncResponseDTO response = new CatalogSyncResponseDTO();
        CatalogSyncResponseDTO.SyncStats stats = new CatalogSyncResponseDTO.SyncStats();
        
        try {
            // Get client API details
            ClientApiDetails apiDetails = clientApiDetailsRepository.findById(clientDetailsId)
                .orElseThrow(() -> new RuntimeException("Client API details not found"));
            
            // Fetch routes from Kong
            String url = apiDetails.getBaseUrl() + "/routes";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiDetails.getAuthToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> kongResponse = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (kongResponse.getStatusCode().is2xxSuccessful() && kongResponse.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> routes = (List<Map<String, Object>>) kongResponse.getBody().get("data");
                
                if (routes != null) {
                    stats.setFetched(routes.size());
                    int created = 0;
                    int updated = 0;
                    
                    for (Map<String, Object> routeData : routes) {
                        String routeId = (String) routeData.get("id");
                        String routeName = (String) routeData.get("name");
                        
                        // Check if route already exists
                        Optional<KongRoute> existingRoute = routeRepository.findByIdAndOrganizationId(routeId, organizationId);
                        
                        KongRoute route;
                        if (existingRoute.isPresent()) {
                            route = existingRoute.get();
                            updated++;
                        } else {
                            route = new KongRoute();
                            route.setId(routeId);
                            route.setOrganizationId(organizationId);
                            created++;
                        }
                        
                        route.setName(routeName);
                        @SuppressWarnings("unchecked")
                        List<String> paths = (List<String>) routeData.get("paths");
                        if (paths != null && !paths.isEmpty()) {
                            route.setPaths(String.join(",", paths));
                        }
                        
                        @SuppressWarnings("unchecked")
                        List<String> methods = (List<String>) routeData.get("methods");
                        if (methods != null && !methods.isEmpty()) {
                            route.setMethods(String.join(",", methods));
                        }
                        
                        // Get service ID from service object
                        @SuppressWarnings("unchecked")
                        Map<String, Object> serviceData = (Map<String, Object>) routeData.get("service");
                        if (serviceData != null) {
                            // route.setServiceId((String) serviceData.get("id")); // TODO: Add this field to entity
                        }
                        
                        route.setStripPath((Boolean) routeData.getOrDefault("strip_path", false));
                        // route.setLastSyncTime(Instant.now()); // TODO: Add this field to entity
                        
                        routeRepository.save(route);
                    }
                    
                    stats.setCreated(created);
                    stats.setUpdated(updated);
                    logger.info("Synced {} routes: {} created, {} updated", routes.size(), created, updated);
                } else {
                    stats.setFetched(0);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to sync routes", e);
            stats.setFailed(stats.getFetched());
        }
        
        response.setRoutes(stats);
        return response;
    }
    
    @Override
    public CatalogSyncResponseDTO syncConsumers(Long clientDetailsId) {
        logger.info("Syncing consumers for client details ID: {}", clientDetailsId);
        Long organizationId = TenantContext.require();
        
        CatalogSyncResponseDTO response = new CatalogSyncResponseDTO();
        CatalogSyncResponseDTO.SyncStats stats = new CatalogSyncResponseDTO.SyncStats();
        
        try {
            // Get client API details
            ClientApiDetails apiDetails = clientApiDetailsRepository.findById(clientDetailsId)
                .orElseThrow(() -> new RuntimeException("Client API details not found"));
            
            // Fetch consumers from Kong
            String url = apiDetails.getBaseUrl() + "/consumers";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiDetails.getAuthToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> kongResponse = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (kongResponse.getStatusCode().is2xxSuccessful() && kongResponse.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> consumers = (List<Map<String, Object>>) kongResponse.getBody().get("data");
                
                if (consumers != null) {
                    stats.setFetched(consumers.size());
                    int created = 0;
                    int updated = 0;
                    
                    for (Map<String, Object> consumerData : consumers) {
                        String consumerId = (String) consumerData.get("id");
                        String username = (String) consumerData.get("username");
                        String customId = (String) consumerData.get("custom_id");
                        
                        // Check if consumer already exists
                        Optional<KongConsumer> existingConsumer = consumerRepository.findByIdAndOrganizationId(consumerId, organizationId);
                        
                        KongConsumer consumer;
                        if (existingConsumer.isPresent()) {
                            consumer = existingConsumer.get();
                            updated++;
                        } else {
                            consumer = new KongConsumer();
                            consumer.setId(consumerId);
                            consumer.setOrganizationId(organizationId);
                            created++;
                        }
                        
                        consumer.setUsername(username);
                        consumer.setCustomId(customId);
                        
                        @SuppressWarnings("unchecked")
                        List<String> tags = (List<String>) consumerData.get("tags");
                        if (tags != null && !tags.isEmpty()) {
                            consumer.setTags(String.join(",", tags));
                        }
                        
                        consumer.setStatus("active");
                        consumer.setWalletBalance(0.0); // Default wallet balance
                        // consumer.setLastSyncTime(Instant.now()); // TODO: Add this field to entity
                        
                        consumerRepository.save(consumer);
                    }
                    
                    stats.setCreated(created);
                    stats.setUpdated(updated);
                    logger.info("Synced {} consumers: {} created, {} updated", consumers.size(), created, updated);
                } else {
                    stats.setFetched(0);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to sync consumers", e);
            stats.setFailed(stats.getFetched());
        }
        
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
    public void ingestUsageEvent(KongEventDTO event, Long organizationId) {
        // Set organization context and delegate to existing method
        TenantContext.set(organizationId);
        try {
            ingestUsageEvent(event);
        } finally {
            TenantContext.clear();
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
    public void ingestUsageEvents(List<KongEventDTO> events, Long organizationId) {
        // Set organization context and delegate to existing method
        TenantContext.set(organizationId);
        try {
            ingestUsageEvents(events);
        } finally {
            TenantContext.clear();
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
    public void suspendConsumer(SuspendRequestDTO request, Long organizationId) {
        // Set organization context and delegate to existing method
        TenantContext.set(organizationId);
        try {
            suspendConsumer(request);
        } finally {
            TenantContext.clear();
        }
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
    public void resumeConsumer(String consumerId, Long organizationId) {
        // Set organization context and delegate to existing method
        TenantContext.set(organizationId);
        try {
            resumeConsumer(consumerId);
        } finally {
            TenantContext.clear();
        }
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
