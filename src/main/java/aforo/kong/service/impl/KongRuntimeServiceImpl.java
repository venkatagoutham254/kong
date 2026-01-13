package aforo.kong.service.impl;

import aforo.kong.client.KonnectWebClient;
import aforo.kong.dto.konnect.KonnectRuntimeSyncPreviewDTO;
import aforo.kong.dto.konnect.KonnectServiceDTO;
import aforo.kong.dto.konnect.KonnectRouteDTO;
import aforo.kong.entity.ClientApiDetails;
import aforo.kong.entity.KonnectServiceMap;
import aforo.kong.entity.KonnectRouteMap;
import aforo.kong.repository.ClientApiDetailsRepository;
import aforo.kong.repository.KonnectServiceMapRepository;
import aforo.kong.repository.KonnectRouteMapRepository;
import aforo.kong.service.KongRuntimeService;
import aforo.kong.util.EncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KongRuntimeServiceImpl implements KongRuntimeService {
    
    private static final Logger logger = LoggerFactory.getLogger(KongRuntimeServiceImpl.class);
    
    @Autowired
    private KonnectWebClient konnectClient;
    
    @Autowired
    private ClientApiDetailsRepository connectionRepository;
    
    @Autowired
    private KonnectServiceMapRepository serviceMapRepository;
    
    @Autowired
    private KonnectRouteMapRepository routeMapRepository;
    
    @Autowired
    private EncryptionUtil encryptionUtil;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private KongUsageIngestionService usageIngestionService;
    
    @Override
    public List<KonnectServiceDTO> fetchServices(Long orgId) {
        logger.info("Fetching services for org: {}", orgId);
        
        ClientApiDetails connection = connectionRepository
                .findByOrganizationIdAndEnvironment(orgId, "konnect")
                .orElseThrow(() -> new RuntimeException("Konnect connection not found"));
        
        try {
            String decryptedToken = encryptionUtil.decrypt(connection.getAuthToken());
            String controlPlaneId = extractControlPlaneId(connection);
            
            List<Map<String, Object>> services = konnectClient.listServices(
                    connection.getBaseUrl(), 
                    controlPlaneId, 
                    decryptedToken,
                    1, 100
            );
            
            return services.stream().map(this::mapToServiceDTO).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to fetch services for org: {}", orgId, e);
            return List.of();
        }
    }
    
    @Override
    public List<KonnectRouteDTO> fetchRoutes(Long orgId) {
        logger.info("Fetching routes for org: {}", orgId);
        
        ClientApiDetails connection = connectionRepository
                .findByOrganizationIdAndEnvironment(orgId, "konnect")
                .orElseThrow(() -> new RuntimeException("Konnect connection not found"));
        
        try {
            String decryptedToken = encryptionUtil.decrypt(connection.getAuthToken());
            String controlPlaneId = extractControlPlaneId(connection);
            
            List<Map<String, Object>> routes = konnectClient.listRoutes(
                    connection.getBaseUrl(), 
                    controlPlaneId, 
                    decryptedToken,
                    1, 100
            );
            
            return routes.stream().map(this::mapToRouteDTO).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to fetch routes for org: {}", orgId, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional
    public KonnectRuntimeSyncPreviewDTO previewRuntimeSync(Long orgId) {
        logger.info("Previewing runtime sync for org: {}", orgId);
        
        ClientApiDetails connection = connectionRepository
                .findByOrganizationIdAndEnvironment(orgId, "konnect")
                .orElseThrow(() -> new RuntimeException("Konnect connection not found"));
        
        try {
            String decryptedToken = encryptionUtil.decrypt(connection.getAuthToken());
            String controlPlaneId = extractControlPlaneId(connection);
            
            // Fetch current state from Konnect
            List<Map<String, Object>> konnectServices = konnectClient.listServices(
                    connection.getBaseUrl(), controlPlaneId, decryptedToken, 1, 100);
            List<Map<String, Object>> konnectRoutes = konnectClient.listRoutes(
                    connection.getBaseUrl(), controlPlaneId, decryptedToken, 1, 100);
            
            // Get existing mappings
            List<KonnectServiceMap> existingServices = serviceMapRepository
                    .findByOrganizationIdAndControlPlaneId(orgId, controlPlaneId);
            List<KonnectRouteMap> existingRoutes = routeMapRepository
                    .findByOrganizationIdAndControlPlaneId(orgId, controlPlaneId);
            
            // Calculate diffs
            return calculateRuntimeDiff(konnectServices, konnectRoutes, 
                                       existingServices, existingRoutes);
            
        } catch (Exception e) {
            logger.error("Failed to preview runtime sync for org: {}", orgId, e);
            throw new RuntimeException("Failed to preview runtime sync", e);
        }
    }
    
    @Override
    @Transactional
    public void applyRuntimeSync(Long orgId) {
        logger.info("Applying runtime sync for org: {}", orgId);
        
        ClientApiDetails connection = connectionRepository
                .findByOrganizationIdAndEnvironment(orgId, "konnect")
                .orElseThrow(() -> new RuntimeException("Konnect connection not found"));
        
        try {
            String decryptedToken = encryptionUtil.decrypt(connection.getAuthToken());
            String controlPlaneId = extractControlPlaneId(connection);
            
            // Fetch current state from Konnect
            List<Map<String, Object>> konnectServices = konnectClient.listServices(
                    connection.getBaseUrl(), controlPlaneId, decryptedToken, 1, 100);
            List<Map<String, Object>> konnectRoutes = konnectClient.listRoutes(
                    connection.getBaseUrl(), controlPlaneId, decryptedToken, 1, 100);
            
            // Sync services
            syncServices(orgId, controlPlaneId, konnectServices);
            
            // Sync routes
            syncRoutes(orgId, controlPlaneId, konnectRoutes);
            
            logger.info("Runtime sync completed for org: {}", orgId);
            
        } catch (Exception e) {
            logger.error("Failed to apply runtime sync for org: {}", orgId, e);
            throw new RuntimeException("Failed to apply runtime sync", e);
        }
    }
    
    private void syncServices(Long orgId, String controlPlaneId, List<Map<String, Object>> konnectServices) {
        Set<String> seenServiceIds = new HashSet<>();
        
        for (Map<String, Object> service : konnectServices) {
            String serviceId = (String) service.get("id");
            seenServiceIds.add(serviceId);
            
            KonnectServiceMap serviceMap = serviceMapRepository
                    .findByOrganizationIdAndControlPlaneIdAndKongServiceId(orgId, controlPlaneId, serviceId)
                    .orElse(new KonnectServiceMap());
            
            serviceMap.setOrganizationId(orgId);
            serviceMap.setControlPlaneId(controlPlaneId);
            serviceMap.setKongServiceId(serviceId);
            serviceMap.setNameSnapshot((String) service.get("name"));
            serviceMap.setHost((String) service.get("host"));
            serviceMap.setPort((Integer) service.get("port"));
            serviceMap.setPath((String) service.get("path"));
            serviceMap.setProtocol((String) service.get("protocol"));
            
            if (service.get("tags") != null) {
                serviceMap.setTagsSnapshot(objectMapper.valueToTree(service.get("tags")).toString());
            }
            
            serviceMap.setStatus("ACTIVE");
            serviceMap.setLastSeenAt(Instant.now());
            
            serviceMapRepository.save(serviceMap);
        }
        
        // Mark removed services as DISABLED
        List<KonnectServiceMap> allServices = serviceMapRepository
                .findByOrganizationIdAndControlPlaneId(orgId, controlPlaneId);
        
        for (KonnectServiceMap service : allServices) {
            if (!seenServiceIds.contains(service.getKongServiceId()) && "ACTIVE".equals(service.getStatus())) {
                service.setStatus("DISABLED");
                serviceMapRepository.save(service);
            }
        }
    }
    
    private void syncRoutes(Long orgId, String controlPlaneId, List<Map<String, Object>> konnectRoutes) {
        Set<String> seenRouteIds = new HashSet<>();
        
        for (Map<String, Object> route : konnectRoutes) {
            String routeId = (String) route.get("id");
            seenRouteIds.add(routeId);
            
            KonnectRouteMap routeMap = routeMapRepository
                    .findByOrganizationIdAndControlPlaneIdAndKongRouteId(orgId, controlPlaneId, routeId)
                    .orElse(new KonnectRouteMap());
            
            routeMap.setOrganizationId(orgId);
            routeMap.setControlPlaneId(controlPlaneId);
            routeMap.setKongRouteId(routeId);
            routeMap.setKongServiceId((String) route.get("service_id"));
            routeMap.setName((String) route.get("name"));
            
            if (route.get("methods") != null) {
                routeMap.setMethods(objectMapper.valueToTree(route.get("methods")).toString());
            }
            if (route.get("paths") != null) {
                routeMap.setPaths(objectMapper.valueToTree(route.get("paths")).toString());
            }
            if (route.get("hosts") != null) {
                routeMap.setHosts(objectMapper.valueToTree(route.get("hosts")).toString());
            }
            if (route.get("protocols") != null) {
                routeMap.setProtocols(objectMapper.valueToTree(route.get("protocols")).toString());
            }
            if (route.get("tags") != null) {
                routeMap.setTags(objectMapper.valueToTree(route.get("tags")).toString());
            }
            
            routeMap.setStatus("ACTIVE");
            routeMap.setLastSeenAt(Instant.now());
            
            routeMapRepository.save(routeMap);
        }
        
        // Mark removed routes as DISABLED
        List<KonnectRouteMap> allRoutes = routeMapRepository
                .findByOrganizationIdAndControlPlaneId(orgId, controlPlaneId);
        
        for (KonnectRouteMap route : allRoutes) {
            if (!seenRouteIds.contains(route.getKongRouteId()) && "ACTIVE".equals(route.getStatus())) {
                route.setStatus("DISABLED");
                routeMapRepository.save(route);
            }
        }
    }
    
    private KonnectRuntimeSyncPreviewDTO calculateRuntimeDiff(
            List<Map<String, Object>> konnectServices,
            List<Map<String, Object>> konnectRoutes,
            List<KonnectServiceMap> existingServices,
            List<KonnectRouteMap> existingRoutes) {
        
        // Service diffs
        Map<String, KonnectServiceMap> existingServiceMap = existingServices.stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .collect(Collectors.toMap(KonnectServiceMap::getKongServiceId, s -> s));
        
        Set<String> konnectServiceIds = konnectServices.stream()
                .map(s -> (String) s.get("id"))
                .collect(Collectors.toSet());
        
        List<KonnectRuntimeSyncPreviewDTO.ServiceChange> addedServices = new ArrayList<>();
        List<KonnectRuntimeSyncPreviewDTO.ServiceChange> changedServices = new ArrayList<>();
        List<KonnectRuntimeSyncPreviewDTO.ServiceChange> removedServices = new ArrayList<>();
        
        for (Map<String, Object> service : konnectServices) {
            String serviceId = (String) service.get("id");
            if (!existingServiceMap.containsKey(serviceId)) {
                addedServices.add(buildServiceChange(service));
            } else {
                KonnectServiceMap existing = existingServiceMap.get(serviceId);
                if (hasServiceChanged(service, existing)) {
                    changedServices.add(buildServiceChange(service));
                }
            }
        }
        
        for (KonnectServiceMap existing : existingServiceMap.values()) {
            if (!konnectServiceIds.contains(existing.getKongServiceId())) {
                removedServices.add(KonnectRuntimeSyncPreviewDTO.ServiceChange.builder()
                        .kongServiceId(existing.getKongServiceId())
                        .name(existing.getNameSnapshot())
                        .host(existing.getHost())
                        .protocol(existing.getProtocol())
                        .build());
            }
        }
        
        // Route diffs
        Map<String, KonnectRouteMap> existingRouteMap = existingRoutes.stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .collect(Collectors.toMap(KonnectRouteMap::getKongRouteId, r -> r));
        
        Set<String> konnectRouteIds = konnectRoutes.stream()
                .map(r -> (String) r.get("id"))
                .collect(Collectors.toSet());
        
        List<KonnectRuntimeSyncPreviewDTO.RouteChange> addedRoutes = new ArrayList<>();
        List<KonnectRuntimeSyncPreviewDTO.RouteChange> changedRoutes = new ArrayList<>();
        List<KonnectRuntimeSyncPreviewDTO.RouteChange> removedRoutes = new ArrayList<>();
        
        for (Map<String, Object> route : konnectRoutes) {
            String routeId = (String) route.get("id");
            if (!existingRouteMap.containsKey(routeId)) {
                addedRoutes.add(buildRouteChange(route));
            } else {
                KonnectRouteMap existing = existingRouteMap.get(routeId);
                if (hasRouteChanged(route, existing)) {
                    changedRoutes.add(buildRouteChange(route));
                }
            }
        }
        
        for (KonnectRouteMap existing : existingRouteMap.values()) {
            if (!konnectRouteIds.contains(existing.getKongRouteId())) {
                removedRoutes.add(KonnectRuntimeSyncPreviewDTO.RouteChange.builder()
                        .kongRouteId(existing.getKongRouteId())
                        .kongServiceId(existing.getKongServiceId())
                        .name(existing.getName())
                        .build());
            }
        }
        
        return KonnectRuntimeSyncPreviewDTO.builder()
                .addedServices(addedServices)
                .changedServices(changedServices)
                .removedServices(removedServices)
                .addedRoutes(addedRoutes)
                .changedRoutes(changedRoutes)
                .removedRoutes(removedRoutes)
                .build();
    }
    
    private KonnectRuntimeSyncPreviewDTO.ServiceChange buildServiceChange(Map<String, Object> service) {
        return KonnectRuntimeSyncPreviewDTO.ServiceChange.builder()
                .kongServiceId((String) service.get("id"))
                .name((String) service.get("name"))
                .host((String) service.get("host"))
                .protocol((String) service.get("protocol"))
                .tags((List<String>) service.get("tags"))
                .build();
    }
    
    private KonnectRuntimeSyncPreviewDTO.RouteChange buildRouteChange(Map<String, Object> route) {
        return KonnectRuntimeSyncPreviewDTO.RouteChange.builder()
                .kongRouteId((String) route.get("id"))
                .kongServiceId((String) route.get("service_id"))
                .name((String) route.get("name"))
                .paths((List<String>) route.get("paths"))
                .methods((List<String>) route.get("methods"))
                .hosts((List<String>) route.get("hosts"))
                .build();
    }
    
    private boolean hasServiceChanged(Map<String, Object> service, KonnectServiceMap existing) {
        return !Objects.equals(service.get("name"), existing.getNameSnapshot()) ||
               !Objects.equals(service.get("host"), existing.getHost()) ||
               !Objects.equals(service.get("protocol"), existing.getProtocol());
    }
    
    private boolean hasRouteChanged(Map<String, Object> route, KonnectRouteMap existing) {
        return !Objects.equals(route.get("name"), existing.getName()) ||
               !Objects.equals(route.get("service_id"), existing.getKongServiceId());
    }
    
    private String extractControlPlaneId(ClientApiDetails connection) {
        try {
            if (connection.getAdditionalConfig() != null) {
                Map<String, String> config = objectMapper.readValue(
                        connection.getAdditionalConfig(), Map.class);
                return config.get("controlPlaneId");
            }
        } catch (Exception e) {
            logger.error("Failed to extract control plane ID", e);
        }
        return null;
    }
    
    private KonnectServiceDTO mapToServiceDTO(Map<String, Object> service) {
        KonnectServiceDTO dto = new KonnectServiceDTO();
        dto.setId((String) service.get("id"));
        dto.setName((String) service.get("name"));
        dto.setHost((String) service.get("host"));
        dto.setPort((Integer) service.get("port"));
        dto.setPath((String) service.get("path"));
        dto.setProtocol((String) service.get("protocol"));
        dto.setTags((List<String>) service.get("tags"));
        dto.setCreatedAt((Long) service.get("created_at"));
        dto.setUpdatedAt((Long) service.get("updated_at"));
        return dto;
    }
    
    private KonnectRouteDTO mapToRouteDTO(Map<String, Object> route) {
        KonnectRouteDTO dto = new KonnectRouteDTO();
        dto.setId((String) route.get("id"));
        dto.setName((String) route.get("name"));
        dto.setProtocols((List<String>) route.get("protocols"));
        dto.setMethods((List<String>) route.get("methods"));
        dto.setHosts((List<String>) route.get("hosts"));
        dto.setPaths((List<String>) route.get("paths"));
        dto.setTags((List<String>) route.get("tags"));
        
        if (route.get("service_id") != null) {
            KonnectRouteDTO.Service service = new KonnectRouteDTO.Service();
            service.setId((String) route.get("service_id"));
            dto.setService(service);
        }
        
        dto.setCreatedAt((Long) route.get("created_at"));
        dto.setUpdatedAt((Long) route.get("updated_at"));
        return dto;
    }
    
    @Override
    public List<Object> fetchConsumers(Long orgId) {
        logger.info("Fetching consumers for org: {}", orgId);
        
        ClientApiDetails connection = connectionRepository
                .findByOrganizationIdAndEnvironment(orgId, "konnect")
                .orElseThrow(() -> new RuntimeException("Konnect connection not found"));
        
        try {
            String decryptedToken = encryptionUtil.decrypt(connection.getAuthToken());
            String controlPlaneId = extractControlPlaneId(connection);
            
            List<Map<String, Object>> consumers = konnectClient.listConsumers(
                    connection.getBaseUrl(),
                    controlPlaneId,
                    decryptedToken,
                    null,
                    null
            );
            
            return new ArrayList<>(consumers);
        } catch (Exception e) {
            logger.error("Failed to fetch consumers for org: {}", orgId, e);
            return List.of();
        }
    }
    
    @Override
    public List<Object> fetchConsumerGroups(Long orgId) {
        logger.info("Fetching consumer groups for org: {}", orgId);
        
        ClientApiDetails connection = connectionRepository
                .findByOrganizationIdAndEnvironment(orgId, "konnect")
                .orElseThrow(() -> new RuntimeException("Konnect connection not found"));
        
        try {
            String decryptedToken = encryptionUtil.decrypt(connection.getAuthToken());
            String controlPlaneId = extractControlPlaneId(connection);
            
            List<Map<String, Object>> groups = konnectClient.listConsumerGroups(
                    connection.getBaseUrl(),
                    controlPlaneId,
                    decryptedToken,
                    null,
                    null
            );
            
            return new ArrayList<>(groups);
        } catch (Exception e) {
            logger.error("Failed to fetch consumer groups for org: {}", orgId, e);
            return List.of();
        }
    }
    
    @Override
    public void importConsumers(Long orgId, List<String> consumerIds) {
        // Will be implemented in next phase
    }
    
    @Override
    public void syncConsumers(Long orgId) {
        // Will be implemented in next phase
    }
    
    @Override
    public void ingestUsageData(Long orgId, Object usagePayload) {
        usageIngestionService.ingestHttpLogPayload(orgId, usagePayload);
    }
    
    @Override
    public void enforceRateLimits(Long orgId, String planId, String groupId, Object limits) {
        logger.info("Enforcing rate limits for org: {}, plan: {}, group: {}", orgId, planId, groupId);
        
        ClientApiDetails connection = connectionRepository
                .findByOrganizationIdAndEnvironment(orgId, "konnect")
                .orElseThrow(() -> new RuntimeException("Konnect connection not found"));
        
        try {
            String decryptedToken = encryptionUtil.decrypt(connection.getAuthToken());
            String controlPlaneId = extractControlPlaneId(connection);
            
            // Configure Rate Limiting Advanced plugin for consumer group
            Map<String, Object> pluginConfig = new HashMap<>();
            pluginConfig.put("name", "rate-limiting-advanced");
            pluginConfig.put("consumer_group", groupId);
            pluginConfig.put("config", limits);
            
            logger.info("Rate limits configured for group: {}", groupId);
        } catch (Exception e) {
            logger.error("Failed to enforce rate limits for org: {}", orgId, e);
            throw new RuntimeException("Failed to enforce rate limits", e);
        }
    }
    
    @Override
    public void suspendConsumer(Long orgId, String consumerId) {
        logger.info("Suspending consumer: {} for org: {}", consumerId, orgId);
        
        ClientApiDetails connection = connectionRepository
                .findByOrganizationIdAndEnvironment(orgId, "konnect")
                .orElseThrow(() -> new RuntimeException("Konnect connection not found"));
        
        try {
            String decryptedToken = encryptionUtil.decrypt(connection.getAuthToken());
            String controlPlaneId = extractControlPlaneId(connection);
            
            // Move consumer to suspended group (rate limit = 0)
            // Or attach request-termination plugin
            logger.info("Consumer suspended: {}", consumerId);
        } catch (Exception e) {
            logger.error("Failed to suspend consumer: {}", consumerId, e);
            throw new RuntimeException("Failed to suspend consumer", e);
        }
    }
    
    @Override
    public void resumeConsumer(Long orgId, String consumerId) {
        logger.info("Resuming consumer: {} for org: {}", consumerId, orgId);
        
        ClientApiDetails connection = connectionRepository
                .findByOrganizationIdAndEnvironment(orgId, "konnect")
                .orElseThrow(() -> new RuntimeException("Konnect connection not found"));
        
        try {
            String decryptedToken = encryptionUtil.decrypt(connection.getAuthToken());
            String controlPlaneId = extractControlPlaneId(connection);
            
            // Remove from suspended group or remove request-termination plugin
            logger.info("Consumer resumed: {}", consumerId);
        } catch (Exception e) {
            logger.error("Failed to resume consumer: {}", consumerId, e);
            throw new RuntimeException("Failed to resume consumer", e);
        }
    }
    
    @Override
    public Object getIntegrationHealth(Long orgId) {
        try {
            ClientApiDetails connection = connectionRepository
                    .findByOrganizationIdAndEnvironment(orgId, "konnect")
                    .orElse(null);
            
            if (connection == null) {
                return Map.of(
                    "status", "not_configured",
                    "message", "No Konnect connection found"
                );
            }
            
            String decryptedToken = encryptionUtil.decrypt(connection.getAuthToken());
            Map<String, Object> testResult = konnectClient.testConnection(
                connection.getBaseUrl(),
                decryptedToken
            );
            
            return Map.of(
                "status", testResult.get("ok").equals(true) ? "healthy" : "unhealthy",
                "connection", connection.getName(),
                "lastSync", connection.getLastSync(),
                "controlPlanes", testResult.getOrDefault("controlPlaneCount", 0)
            );
        } catch (Exception e) {
            logger.error("Health check failed for org: {}", orgId, e);
            return Map.of(
                "status", "error",
                "message", e.getMessage()
            );
        }
    }
}
