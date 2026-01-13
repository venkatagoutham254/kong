package aforo.kong.service.impl;

import aforo.kong.client.KonnectWebClient;
import aforo.kong.dto.konnect.*;
import aforo.kong.entity.ClientApiDetails;
import aforo.kong.entity.KonnectApiProductMap;
import aforo.kong.entity.KongProduct;
import aforo.kong.repository.ClientApiDetailsRepository;
import aforo.kong.repository.KonnectApiProductMapRepository;
import aforo.kong.repository.KongProductRepository;
import aforo.kong.service.KonnectService;
import aforo.kong.service.SyncLockService;
import aforo.kong.util.EncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KonnectServiceImpl implements KonnectService {

    private static final Logger logger = LoggerFactory.getLogger(KonnectServiceImpl.class);

    private final ClientApiDetailsRepository connectionRepository;
    private final KonnectApiProductMapRepository mappingRepository;
    private final KongProductRepository productRepository;
    private final KonnectWebClient konnectClient;
    private final EncryptionUtil encryptionUtil;
    private final ObjectMapper objectMapper;
    private final SyncLockService syncLockService;

    public KonnectServiceImpl(
            ClientApiDetailsRepository connectionRepository,
            KonnectApiProductMapRepository mappingRepository,
            KongProductRepository productRepository,
            KonnectWebClient konnectClient,
            EncryptionUtil encryptionUtil,
            ObjectMapper objectMapper,
            SyncLockService syncLockService) {
        this.connectionRepository = connectionRepository;
        this.mappingRepository = mappingRepository;
        this.productRepository = productRepository;
        this.konnectClient = konnectClient;
        this.encryptionUtil = encryptionUtil;
        this.objectMapper = objectMapper;
        this.syncLockService = syncLockService;
    }

    @Override
    @Transactional
    public KonnectConnectionResponseDTO createOrUpdateConnection(Long orgId, KonnectConnectionRequestDTO request) {
        Optional<ClientApiDetails> existingOpt = connectionRepository.findByOrganizationIdAndEnvironment(orgId, "konnect");
        
        ClientApiDetails connection;
        if (existingOpt.isPresent()) {
            connection = existingOpt.get();
        } else {
            connection = new ClientApiDetails();
            connection.setOrganizationId(orgId);
            connection.setEnvironment("konnect");
            connection.setCreatedAt(Instant.now());
        }

        connection.setName(request.getName() != null ? request.getName() : "Kong Konnect Connection");
        connection.setDescription(request.getDescription());
        connection.setUpdatedAt(Instant.now());

        logger.info("Request baseUrl: {}, authToken length: {}", request.getBaseUrl(), 
                    request.getAuthToken() != null ? request.getAuthToken().length() : 0);

        String encryptedToken;
        try {
            encryptedToken = encryptionUtil.encrypt(request.getAuthToken());
            connection.setAuthToken(encryptedToken);
            connection.setBaseUrl(request.getBaseUrl());
            connection.setEndpoint(request.getEndpoint() != null ? request.getEndpoint() : request.getBaseUrl());
            
            logger.info("After setting - connection baseUrl: {}, endpoint: {}", 
                        connection.getBaseUrl(), connection.getEndpoint());
        } catch (Exception e) {
            logger.error("Failed to encrypt auth token for org: {}", orgId, e);
            return KonnectConnectionResponseDTO.builder()
                    .connectionId(null)
                    .status("failed")
                    .controlPlaneId(null)
                    .message("Connection failed: encryption error")
                    .build();
        }

        String controlPlaneId = request.getControlPlaneId();
        String decryptedToken;
        try {
            decryptedToken = encryptionUtil.decrypt(encryptedToken);
        } catch (Exception e) {
            logger.error("Failed to decrypt token for org: {}", orgId, e);
            return KonnectConnectionResponseDTO.builder()
                    .connectionId(null)
                    .status("failed")
                    .controlPlaneId(null)
                    .message("Connection failed: decryption error")
                    .build();
        }

        if (controlPlaneId == null || controlPlaneId.isEmpty()) {
            try {
                List<Map<String, Object>> controlPlanes = konnectClient.listControlPlanes(
                    connection.getBaseUrl(), 
                    decryptedToken
                );
                if (!controlPlanes.isEmpty()) {
                    controlPlaneId = (String) controlPlanes.get(0).get("id");
                }
            } catch (Exception e) {
                logger.error("Failed to list control planes for org: {}", orgId, e);
            }
        }

        try {
            Map<String, String> additionalConfig = new HashMap<>();
            if (controlPlaneId != null) {
                additionalConfig.put("controlPlaneId", controlPlaneId);
            }
            if (request.getRegion() != null) {
                additionalConfig.put("region", request.getRegion());
            }
            connection.setAdditionalConfig(objectMapper.writeValueAsString(additionalConfig));
        } catch (Exception e) {
            logger.error("Failed to serialize additional config", e);
        }

        boolean connectionSuccessful = false;
        try {
            Map<String, Object> testResult = konnectClient.testConnection(connection.getBaseUrl(), decryptedToken);
            
            if (Boolean.TRUE.equals(testResult.get("ok"))) {
                connection.setConnectionStatus("connected");
                connection.setLastSync(Instant.now());
                connectionSuccessful = true;
            } else {
                connection.setConnectionStatus("failed");
            }
        } catch (Exception e) {
            logger.error("Failed to test connection for org: {}", orgId, e);
            connection.setConnectionStatus("failed");
        }

        connection = connectionRepository.save(connection);

        return KonnectConnectionResponseDTO.builder()
                .connectionId(connection.getId())
                .status(connection.getConnectionStatus())
                .controlPlaneId(controlPlaneId)
                .message(connectionSuccessful ? "Connection successful" : "Connection failed")
                .build();
    }

    @Override
    public KonnectTestResponseDTO testConnection(Long orgId) {
        ClientApiDetails connection = getConnectionByOrgId(orgId);
        String decryptedToken = encryptionUtil.decrypt(connection.getAuthToken());
        
        Map<String, Object> result = konnectClient.testConnection(connection.getBaseUrl(), decryptedToken);
        
        List<Map<String, Object>> products = konnectClient.listApiProducts(
            connection.getBaseUrl(),
            extractControlPlaneId(connection),
            decryptedToken
        );

        Map<String, Integer> counts = new HashMap<>();
        counts.put("apiProducts", products.size());

        return KonnectTestResponseDTO.builder()
                .ok((Boolean) result.get("ok"))
                .message((String) result.get("message"))
                .counts(counts)
                .build();
    }

    @Override
    public List<KonnectApiProductDTO> fetchApiProducts(Long orgId) {
        ClientApiDetails connection = getConnectionByOrgId(orgId);
        String decryptedToken = encryptionUtil.decrypt(connection.getAuthToken());
        
        List<Map<String, Object>> products = konnectClient.listApiProducts(
            connection.getBaseUrl(),
            extractControlPlaneId(connection),
            decryptedToken
        );

        return products.stream()
                .map(this::mapToApiProductDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public KonnectImportResponseDTO importApiProducts(Long orgId, KonnectImportRequestDTO request) {
        ClientApiDetails connection = getConnectionByOrgId(orgId);
        String decryptedToken = encryptionUtil.decrypt(connection.getAuthToken());

        int imported = 0;
        int updated = 0;
        int failed = 0;
        List<KonnectImportResponseDTO.ImportedItemDTO> items = new ArrayList<>();

        for (String productId : request.getSelectedApiProductIds()) {
            try {
                Map<String, Object> productData = konnectClient.getApiProductById(
                    connection.getBaseUrl(),
                    productId,
                    decryptedToken
                );

                if (productData.isEmpty()) {
                    failed++;
                    continue;
                }

                Optional<KonnectApiProductMap> existingMapping = 
                    mappingRepository.findByOrgIdAndKonnectApiProductId(orgId, productId);

                String action;
                Long aforoProductId;

                if (existingMapping.isPresent()) {
                    KonnectApiProductMap mapping = existingMapping.get();
                    KongProduct product = productRepository.findById(mapping.getAforoProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found"));
                    
                    product.setName((String) productData.get("name"));
                    product.setDescription((String) productData.get("description"));
                    product.setUpdatedAt(Instant.now());
                    product = productRepository.save(product);

                    mapping.setNameSnapshot((String) productData.get("name"));
                    mapping.setDescriptionSnapshot((String) productData.get("description"));
                    mapping.setLastSeenAt(Instant.now());
                    mapping.setStatus("ACTIVE");
                    mappingRepository.save(mapping);

                    aforoProductId = product.getInternalId();
                    action = "UPDATED";
                    updated++;
                } else {
                    KongProduct product = new KongProduct();
                    product.setId(UUID.randomUUID().toString());
                    product.setName((String) productData.get("name"));
                    product.setDescription((String) productData.get("description"));
                    product.setCreatedAt(Instant.now());
                    product.setUpdatedAt(Instant.now());
                    product.setVersionCount((Integer) productData.getOrDefault("versionCount", 0));
                    product.setOrganizationId(orgId);
                    product.setSource("konnect");
                    product = productRepository.save(product);

                    KonnectApiProductMap mapping = new KonnectApiProductMap();
                    mapping.setOrgId(orgId);
                    mapping.setKonnectApiProductId(productId);
                    mapping.setAforoProductId(product.getInternalId());
                    mapping.setNameSnapshot((String) productData.get("name"));
                    mapping.setDescriptionSnapshot((String) productData.get("description"));
                    mapping.setStatus("ACTIVE");
                    mapping.setLastSeenAt(Instant.now());
                    mappingRepository.save(mapping);

                    aforoProductId = product.getInternalId();
                    action = "CREATED";
                    imported++;
                }

                items.add(KonnectImportResponseDTO.ImportedItemDTO.builder()
                        .konnectApiProductId(productId)
                        .aforoProductId(aforoProductId)
                        .action(action)
                        .build());

            } catch (Exception e) {
                logger.error("Failed to import product: {}", productId, e);
                failed++;
            }
        }

        return KonnectImportResponseDTO.builder()
                .imported(imported)
                .updated(updated)
                .failed(failed)
                .items(items)
                .build();
    }

    @Override
    public List<KonnectImportedProductDTO> listImportedProducts(Long orgId) {
        List<KonnectApiProductMap> mappings = mappingRepository.findByOrgIdAndStatus(orgId, "ACTIVE");
        
        return mappings.stream()
                .map(mapping -> {
                    KongProduct product = productRepository.findById(mapping.getAforoProductId())
                        .orElse(null);
                    
                    if (product == null) {
                        return null;
                    }

                    return KonnectImportedProductDTO.builder()
                            .aforoProductId(product.getInternalId())
                            .name(product.getName())
                            .description(product.getDescription())
                            .konnectApiProductId(mapping.getKonnectApiProductId())
                            .lastSeenAt(mapping.getLastSeenAt())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public KonnectSyncPreviewDTO previewSync(Long orgId) {
        ClientApiDetails connection = getConnectionByOrgId(orgId);
        String decryptedToken = encryptionUtil.decrypt(connection.getAuthToken());
        
        List<Map<String, Object>> liveProducts = konnectClient.listApiProducts(
            connection.getBaseUrl(),
            extractControlPlaneId(connection),
            decryptedToken
        );

        List<KonnectApiProductMap> mappings = mappingRepository.findByOrgId(orgId);
        
        Set<String> liveProductIds = liveProducts.stream()
                .map(p -> (String) p.get("id"))
                .collect(Collectors.toSet());
        
        Set<String> mappedProductIds = mappings.stream()
                .map(KonnectApiProductMap::getKonnectApiProductId)
                .collect(Collectors.toSet());

        List<KonnectApiProductDTO> added = liveProducts.stream()
                .filter(p -> !mappedProductIds.contains(p.get("id")))
                .map(this::mapToApiProductDTO)
                .collect(Collectors.toList());

        List<KonnectApiProductDTO> removed = mappings.stream()
                .filter(m -> !liveProductIds.contains(m.getKonnectApiProductId()))
                .map(m -> KonnectApiProductDTO.builder()
                        .konnectApiProductId(m.getKonnectApiProductId())
                        .name(m.getNameSnapshot())
                        .description(m.getDescriptionSnapshot())
                        .build())
                .collect(Collectors.toList());

        List<KonnectApiProductDTO> changed = new ArrayList<>();
        for (Map<String, Object> liveProduct : liveProducts) {
            String productId = (String) liveProduct.get("id");
            Optional<KonnectApiProductMap> mappingOpt = mappings.stream()
                    .filter(m -> m.getKonnectApiProductId().equals(productId))
                    .findFirst();
            
            if (mappingOpt.isPresent()) {
                KonnectApiProductMap mapping = mappingOpt.get();
                String liveName = (String) liveProduct.get("name");
                String liveDesc = (String) liveProduct.get("description");
                
                if (!liveName.equals(mapping.getNameSnapshot()) || 
                    !Objects.equals(liveDesc, mapping.getDescriptionSnapshot())) {
                    changed.add(mapToApiProductDTO(liveProduct));
                }
            }
        }

        return KonnectSyncPreviewDTO.builder()
                .added(added)
                .removed(removed)
                .changed(changed)
                .build();
    }

    @Override
    public KonnectImportResponseDTO applySync(Long orgId) {
        return syncLockService.executeWithLock(orgId, () -> applySyncInternal(orgId));
    }

    @Transactional
    private KonnectImportResponseDTO applySyncInternal(Long orgId) {
        KonnectSyncPreviewDTO preview = previewSync(orgId);
        
        List<String> toImport = new ArrayList<>();
        toImport.addAll(preview.getAdded().stream()
                .map(KonnectApiProductDTO::getKonnectApiProductId)
                .collect(Collectors.toList()));
        toImport.addAll(preview.getChanged().stream()
                .map(KonnectApiProductDTO::getKonnectApiProductId)
                .collect(Collectors.toList()));

        KonnectImportResponseDTO importResult;
        if (!toImport.isEmpty()) {
            KonnectImportRequestDTO importRequest = new KonnectImportRequestDTO();
            importRequest.setSelectedApiProductIds(toImport);
            importResult = importApiProducts(orgId, importRequest);
        } else {
            importResult = KonnectImportResponseDTO.builder()
                    .imported(0)
                    .updated(0)
                    .failed(0)
                    .items(new ArrayList<>())
                    .build();
        }

        if (importResult.getFailed() == 0) {
            for (KonnectApiProductDTO removed : preview.getRemoved()) {
                Optional<KonnectApiProductMap> mappingOpt = 
                    mappingRepository.findByOrgIdAndKonnectApiProductId(orgId, removed.getKonnectApiProductId());
                mappingOpt.ifPresent(mapping -> {
                    mapping.setStatus("DISABLED");
                    mappingRepository.save(mapping);
                });
            }
        }

        return importResult;
    }

    @Override
    public void autoRefresh() {
        List<ClientApiDetails> connections = connectionRepository.findByEnvironmentAndConnectionStatus("konnect", "connected");
        
        for (ClientApiDetails connection : connections) {
            Long orgId = connection.getOrganizationId();
            
            if (!syncLockService.getLockForOrg(orgId).tryLock()) {
                logger.debug("Skipping auto-refresh for org {} - sync already in progress", orgId);
                continue;
            }
            
            try {
                KonnectSyncPreviewDTO preview = previewSync(orgId);
                
                if (!preview.getAdded().isEmpty() || !preview.getRemoved().isEmpty() || !preview.getChanged().isEmpty()) {
                    logger.info("Auto-syncing changes for org: {}", orgId);
                    applySyncInternal(orgId);
                }
            } catch (Exception e) {
                logger.error("Failed to auto-refresh for org: {}", orgId, e);
            } finally {
                syncLockService.getLockForOrg(orgId).unlock();
            }
        }
    }

    private ClientApiDetails getConnectionByOrgId(Long orgId) {
        return connectionRepository.findByOrganizationIdAndEnvironment(orgId, "konnect")
                .orElseThrow(() -> new RuntimeException("Konnect connection not found for organization: " + orgId));
    }

    private String extractControlPlaneId(ClientApiDetails connection) {
        try {
            if (connection.getAdditionalConfig() != null) {
                @SuppressWarnings("unchecked")
                Map<String, String> config = objectMapper.readValue(
                    connection.getAdditionalConfig(), 
                    Map.class
                );
                return config.get("controlPlaneId");
            }
        } catch (Exception e) {
            logger.error("Failed to extract control plane ID", e);
        }
        return null;
    }

    private KonnectApiProductDTO mapToApiProductDTO(Map<String, Object> productData) {
        return KonnectApiProductDTO.builder()
                .konnectApiProductId((String) productData.get("id"))
                .name((String) productData.get("name"))
                .description((String) productData.get("description"))
                .status((String) productData.get("status"))
                .versionsCount((Integer) productData.getOrDefault("versionCount", 0))
                .updatedAt(productData.get("updatedAt") != null ? 
                    Instant.parse((String) productData.get("updatedAt")) : null)
                .build();
    }
}
