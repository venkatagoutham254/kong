# Apigee Integration - Part 3: Final Components (Controller, Scheduler, Liquibase, Tests, Samples)

**Continuation of Part 2 - Ready for Real-Time Testing**

---

## 7. Remaining Services (Continued)

### 7.7 ApigeeIngestionService.java

```java
package aforo.apigee.service;

import aforo.apigee.dto.request.IngestApigeeEventRequest;
import aforo.apigee.dto.request.IngestApigeeBatchRequest;
import aforo.apigee.dto.response.IngestResponse;

public interface ApigeeIngestionService {
    IngestResponse ingestSingle(Long tenantId, IngestApigeeEventRequest event);
    IngestResponse ingestBatch(Long tenantId, IngestApigeeBatchRequest batch);
}
```

### 7.8 ApigeeIngestionServiceImpl.java

```java
package aforo.apigee.service.impl;

import aforo.apigee.dto.request.IngestApigeeEventRequest;
import aforo.apigee.dto.request.IngestApigeeBatchRequest;
import aforo.apigee.dto.response.IngestResponse;
import aforo.apigee.exception.ApigeeValidationException;
import aforo.apigee.model.*;
import aforo.apigee.model.enums.EntityStatus;
import aforo.apigee.model.enums.ProductKind;
import aforo.apigee.model.enums.QueueStatus;
import aforo.apigee.repository.*;
import aforo.apigee.service.ApigeeIngestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class ApigeeIngestionServiceImpl implements ApigeeIngestionService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApigeeIngestionServiceImpl.class);
    
    @Autowired
    private ApigeeUsageRecordRepository usageRecordRepository;
    
    @Autowired
    private ApigeeCustomerRepository customerRepository;
    
    @Autowired
    private ApigeeProductRepository productRepository;
    
    @Autowired
    private ApigeeIngestQueueRepository queueRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public IngestResponse ingestSingle(Long tenantId, IngestApigeeEventRequest event) {
        validateEvent(event);
        
        Optional<ApigeeUsageRecord> existing = usageRecordRepository
            .findByTenantIdAndCorrelationId(tenantId, event.getCorrelationId());
        
        if (existing.isPresent()) {
            logger.debug("Event already processed, correlation ID: {}", event.getCorrelationId());
            return IngestResponse.builder()
                .status("accepted")
                .message("Event already processed (deduplicated)")
                .accepted(0)
                .deduplicated(1)
                .build();
        }
        
        try {
            String payload = objectMapper.writeValueAsString(event);
            ApigeeIngestQueue queueItem = new ApigeeIngestQueue();
            queueItem.setTenantId(tenantId);
            queueItem.setPayload(payload);
            queueItem.setStatus(QueueStatus.PENDING);
            queueItem.setNextAttemptAt(Instant.now());
            queueRepository.save(queueItem);
            
            return IngestResponse.builder()
                .status("accepted")
                .message("Event queued for processing")
                .accepted(1)
                .deduplicated(0)
                .build();
                
        } catch (Exception e) {
            logger.error("Failed to queue event", e);
            throw new ApigeeValidationException("Failed to queue event: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public IngestResponse ingestBatch(Long tenantId, IngestApigeeBatchRequest batch) {
        int accepted = 0;
        int deduplicated = 0;
        
        for (IngestApigeeEventRequest event : batch.getEvents()) {
            try {
                IngestResponse response = ingestSingle(tenantId, event);
                accepted += response.getAccepted();
                deduplicated += response.getDeduplicated();
            } catch (Exception e) {
                logger.error("Failed to process event in batch", e);
            }
        }
        
        return IngestResponse.builder()
            .status("accepted")
            .message(String.format("Batch processed: %d accepted, %d deduplicated", accepted, deduplicated))
            .accepted(accepted)
            .deduplicated(deduplicated)
            .build();
    }
    
    @Transactional
    public void processQueueItem(ApigeeIngestQueue queueItem) {
        try {
            IngestApigeeEventRequest event = objectMapper.readValue(
                queueItem.getPayload(), 
                IngestApigeeEventRequest.class
            );
            
            Optional<ApigeeUsageRecord> existing = usageRecordRepository
                .findByTenantIdAndCorrelationId(queueItem.getTenantId(), event.getCorrelationId());
            
            if (existing.isPresent()) {
                queueItem.setStatus(QueueStatus.DONE);
                queueRepository.save(queueItem);
                return;
            }
            
            Long customerId = resolveCustomerId(queueItem.getTenantId(), event);
            Long productId = resolveProductId(queueItem.getTenantId(), event);
            
            ApigeeUsageRecord record = new ApigeeUsageRecord();
            record.setTenantId(queueItem.getTenantId());
            record.setCorrelationId(event.getCorrelationId());
            record.setTs(Instant.ofEpochMilli(event.getTimestamp()));
            record.setCustomerId(customerId);
            record.setProductId(productId);
            record.setUnits(1L);
            record.setReqBytes(event.getRequest().getBytes());
            record.setRespBytes(event.getResponse().getBytes());
            record.setLatencyMs(event.getResponse().getLatencyMs());
            record.setHttpStatus(event.getResponse().getStatus());
            
            try {
                record.setRawMinimal(objectMapper.writeValueAsString(event));
            } catch (Exception e) {
                logger.error("Failed to serialize raw minimal", e);
            }
            
            usageRecordRepository.save(record);
            
            queueItem.setStatus(QueueStatus.DONE);
            queueRepository.save(queueItem);
            
            logger.info("Processed usage event: {}", event.getCorrelationId());
            
        } catch (Exception e) {
            logger.error("Failed to process queue item: {}", queueItem.getId(), e);
            queueItem.setAttempts(queueItem.getAttempts() + 1);
            
            if (queueItem.getAttempts() >= 3) {
                queueItem.setStatus(QueueStatus.FAILED);
            } else {
                queueItem.setNextAttemptAt(Instant.now().plusSeconds(60 * queueItem.getAttempts()));
            }
            
            queueRepository.save(queueItem);
        }
    }
    
    private Long resolveCustomerId(Long tenantId, IngestApigeeEventRequest event) {
        Optional<ApigeeCustomer> customer = customerRepository
            .findByTenantIdAndAppIdAndConsumerKey(
                tenantId, 
                event.getApp().getAppId(), 
                event.getKey().getConsumerKey()
            );
        
        if (customer.isPresent()) {
            ApigeeCustomer c = customer.get();
            c.setLastSeenAt(Instant.now());
            customerRepository.save(c);
            return c.getId();
        }
        
        ApigeeCustomer newCustomer = new ApigeeCustomer();
        newCustomer.setTenantId(tenantId);
        newCustomer.setDeveloperEmail(event.getDeveloper().getEmail());
        newCustomer.setDeveloperId(event.getDeveloper().getEmail());
        newCustomer.setAppName(event.getApp().getName());
        newCustomer.setAppId(event.getApp().getAppId());
        newCustomer.setConsumerKey(event.getKey().getConsumerKey());
        newCustomer.setStatus(EntityStatus.ACTIVE);
        newCustomer.setSuspended(false);
        newCustomer.setLastSeenAt(Instant.now());
        
        newCustomer = customerRepository.save(newCustomer);
        logger.info("Created placeholder customer: {}", newCustomer.getId());
        
        return newCustomer.getId();
    }
    
    private Long resolveProductId(Long tenantId, IngestApigeeEventRequest event) {
        Optional<ApigeeProduct> product = productRepository
            .findByTenantIdAndKindAndApigeeProxyName(
                tenantId, 
                ProductKind.PROXY, 
                event.getProxy().getName()
            );
        
        if (product.isPresent()) {
            return product.get().getId();
        }
        
        ApigeeProduct newProduct = new ApigeeProduct();
        newProduct.setTenantId(tenantId);
        newProduct.setKind(ProductKind.PROXY);
        newProduct.setApigeeProxyName(event.getProxy().getName());
        newProduct.setApigeeProxyRevision(event.getProxy().getRevision());
        newProduct.setDisplayName(event.getProxy().getName());
        newProduct.setStatus(EntityStatus.ACTIVE);
        newProduct.setLastSyncedAt(Instant.now());
        
        newProduct = productRepository.save(newProduct);
        logger.info("Created placeholder product: {}", newProduct.getId());
        
        return newProduct.getId();
    }
    
    private void validateEvent(IngestApigeeEventRequest event) {
        if (event.getTimestamp() == null) {
            throw new ApigeeValidationException("Timestamp is required");
        }
        if (event.getCorrelationId() == null || event.getCorrelationId().isBlank()) {
            throw new ApigeeValidationException("Correlation ID is required");
        }
        if (event.getProxy() == null || event.getProxy().getName() == null) {
            throw new ApigeeValidationException("Proxy information is required");
        }
        if (event.getRequest() == null || event.getRequest().getMethod() == null || event.getRequest().getPath() == null) {
            throw new ApigeeValidationException("Request information is required");
        }
        if (event.getResponse() == null || event.getResponse().getStatus() == null) {
            throw new ApigeeValidationException("Response information is required");
        }
    }
}
```

### 7.9 ApigeeEnforcementService.java

```java
package aforo.apigee.service;

import aforo.apigee.dto.request.EnforceProductsRequest;
import aforo.apigee.dto.request.SuspendRequest;
import aforo.apigee.dto.response.EnforceResponse;

public interface ApigeeEnforcementService {
    EnforceResponse enforceProducts(Long tenantId, EnforceProductsRequest request);
    EnforceResponse suspendCustomer(Long tenantId, SuspendRequest request);
    EnforceResponse resumeCustomer(Long tenantId, Long customerId);
}
```

### 7.10 ApigeeEnforcementServiceImpl.java

```java
package aforo.apigee.service.impl;

import aforo.apigee.client.ApigeeClient;
import aforo.apigee.client.ApigeeClientFactory;
import aforo.apigee.dto.request.EnforceProductsRequest;
import aforo.apigee.dto.request.SuspendRequest;
import aforo.apigee.dto.response.EnforceResponse;
import aforo.apigee.exception.ApigeeConnectionException;
import aforo.apigee.model.ApigeeConnection;
import aforo.apigee.model.ApigeeCustomer;
import aforo.apigee.model.ApigeeSyncAudit;
import aforo.apigee.model.enums.SyncScope;
import aforo.apigee.model.enums.SyncSource;
import aforo.apigee.repository.ApigeeConnectionRepository;
import aforo.apigee.repository.ApigeeCustomerRepository;
import aforo.apigee.repository.ApigeeSyncAuditRepository;
import aforo.apigee.service.ApigeeEnforcementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ApigeeEnforcementServiceImpl implements ApigeeEnforcementService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApigeeEnforcementServiceImpl.class);
    
    @Autowired
    private ApigeeConnectionRepository connectionRepository;
    
    @Autowired
    private ApigeeCustomerRepository customerRepository;
    
    @Autowired
    private ApigeeSyncAuditRepository syncAuditRepository;
    
    @Autowired
    private ApigeeClientFactory clientFactory;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public EnforceResponse enforceProducts(Long tenantId, EnforceProductsRequest request) {
        logger.info("Enforcing products for tenant: {}, plan: {}", tenantId, request.getPlanCode());
        
        try {
            ApigeeConnection connection = connectionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ApigeeConnectionException("Connection not found"));
            
            ApigeeClient client = clientFactory.createClient(connection);
            
            List<ApigeeCustomer> customers;
            if (request.getTargetCustomerIds() != null && !request.getTargetCustomerIds().isEmpty()) {
                customers = new ArrayList<>();
                for (Long customerId : request.getTargetCustomerIds()) {
                    customerRepository.findById(customerId).ifPresent(customers::add);
                }
            } else {
                customers = customerRepository.findByTenantId(tenantId);
            }
            
            int customersUpdated = 0;
            int productsAttached = 0;
            int productsDetached = 0;
            
            for (ApigeeCustomer customer : customers) {
                try {
                    for (String productName : request.getApiProductNames()) {
                        client.attachApiProductToKey(
                            customer.getAppId(), 
                            customer.getConsumerKey(), 
                            productName
                        );
                        productsAttached++;
                    }
                    
                    customer.setPlanCode(request.getPlanCode());
                    try {
                        customer.setApiProducts(objectMapper.writeValueAsString(request.getApiProductNames()));
                    } catch (Exception e) {
                        logger.error("Failed to serialize API products", e);
                    }
                    
                    customerRepository.save(customer);
                    customersUpdated++;
                    
                } catch (Exception e) {
                    logger.error("Failed to enforce products for customer: {}", customer.getId(), e);
                }
            }
            
            ApigeeSyncAudit audit = new ApigeeSyncAudit();
            audit.setTenantId(tenantId);
            audit.setScope(SyncScope.ENFORCEMENT);
            audit.setSource(SyncSource.MANUAL);
            audit.setDryRun(false);
            audit.setStatus("SUCCESS");
            syncAuditRepository.save(audit);
            
            return EnforceResponse.builder()
                .status("success")
                .message("Products enforced successfully")
                .customersUpdated(customersUpdated)
                .productsAttached(productsAttached)
                .productsDetached(productsDetached)
                .build();
                
        } catch (Exception e) {
            logger.error("Failed to enforce products for tenant: {}", tenantId, e);
            throw new ApigeeConnectionException("Enforcement failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public EnforceResponse suspendCustomer(Long tenantId, SuspendRequest request) {
        logger.info("Suspending customer for tenant: {}, reason: {}", tenantId, request.getReason());
        
        try {
            ApigeeCustomer customer;
            
            if (request.getCustomerId() != null) {
                customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ApigeeConnectionException("Customer not found"));
            } else if (request.getAppId() != null && request.getConsumerKey() != null) {
                customer = customerRepository.findByTenantIdAndAppIdAndConsumerKey(
                    tenantId, request.getAppId(), request.getConsumerKey()
                ).orElseThrow(() -> new ApigeeConnectionException("Customer not found"));
            } else {
                throw new ApigeeConnectionException("Customer ID or App ID + Consumer Key required");
            }
            
            customer.setSuspended(true);
            customerRepository.save(customer);
            
            return EnforceResponse.builder()
                .status("success")
                .message("Customer suspended")
                .customersUpdated(1)
                .productsAttached(0)
                .productsDetached(0)
                .build();
                
        } catch (Exception e) {
            logger.error("Failed to suspend customer for tenant: {}", tenantId, e);
            throw new ApigeeConnectionException("Suspension failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public EnforceResponse resumeCustomer(Long tenantId, Long customerId) {
        logger.info("Resuming customer for tenant: {}, customerId: {}", tenantId, customerId);
        
        try {
            ApigeeCustomer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ApigeeConnectionException("Customer not found"));
            
            customer.setSuspended(false);
            customerRepository.save(customer);
            
            return EnforceResponse.builder()
                .status("success")
                .message("Customer resumed")
                .customersUpdated(1)
                .productsAttached(0)
                .productsDetached(0)
                .build();
                
        } catch (Exception e) {
            logger.error("Failed to resume customer for tenant: {}", tenantId, e);
            throw new ApigeeConnectionException("Resume failed: " + e.getMessage(), e);
        }
    }
}
```

### 7.11 ApigeeHealthService.java

```java
package aforo.apigee.service;

import aforo.apigee.dto.response.HealthResponse;

public interface ApigeeHealthService {
    HealthResponse getHealth(Long tenantId);
}
```

### 7.12 ApigeeHealthServiceImpl.java

```java
package aforo.apigee.service.impl;

import aforo.apigee.client.ApigeeClient;
import aforo.apigee.client.ApigeeClientFactory;
import aforo.apigee.dto.response.HealthResponse;
import aforo.apigee.model.ApigeeConnection;
import aforo.apigee.model.ApigeeSyncAudit;
import aforo.apigee.model.enums.QueueStatus;
import aforo.apigee.model.enums.SyncScope;
import aforo.apigee.repository.*;
import aforo.apigee.service.ApigeeHealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ApigeeHealthServiceImpl implements ApigeeHealthService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApigeeHealthServiceImpl.class);
    
    @Autowired
    private ApigeeConnectionRepository connectionRepository;
    
    @Autowired
    private ApigeeSyncAuditRepository syncAuditRepository;
    
    @Autowired
    private ApigeeUsageRecordRepository usageRecordRepository;
    
    @Autowired
    private ApigeeIngestQueueRepository queueRepository;
    
    @Autowired
    private ApigeeClientFactory clientFactory;
    
    @Override
    public HealthResponse getHealth(Long tenantId) {
        logger.info("Checking health for tenant: {}", tenantId);
        
        List<String> errors = new ArrayList<>();
        String apigeeConnectivity = "unknown";
        Instant lastCatalogSync = null;
        Instant lastCustomerSync = null;
        Instant lastIngest = null;
        Long pendingQueue = 0L;
        
        try {
            Optional<ApigeeConnection> connectionOpt = connectionRepository.findByTenantId(tenantId);
            
            if (connectionOpt.isEmpty()) {
                errors.add("No Apigee connection configured");
                apigeeConnectivity = "not_configured";
            } else {
                ApigeeConnection connection = connectionOpt.get();
                
                try {
                    ApigeeClient client = clientFactory.createClient(connection);
                    Map<String, Object> testResult = client.testConnection();
                    
                    if (Boolean.TRUE.equals(testResult.get("ok"))) {
                        apigeeConnectivity = "healthy";
                    } else {
                        apigeeConnectivity = "unhealthy";
                        errors.add("Apigee connection test failed");
                    }
                } catch (Exception e) {
                    apigeeConnectivity = "error";
                    errors.add("Failed to test Apigee connection: " + e.getMessage());
                }
            }
            
            Optional<ApigeeSyncAudit> lastCatalogAudit = syncAuditRepository
                .findLastSuccessfulSync(tenantId, SyncScope.CATALOG);
            if (lastCatalogAudit.isPresent()) {
                lastCatalogSync = lastCatalogAudit.get().getCreatedAt();
            }
            
            Optional<ApigeeSyncAudit> lastCustomerAudit = syncAuditRepository
                .findLastSuccessfulSync(tenantId, SyncScope.CUSTOMERS);
            if (lastCustomerAudit.isPresent()) {
                lastCustomerSync = lastCustomerAudit.get().getCreatedAt();
            }
            
            Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
            Long recentUsageCount = usageRecordRepository.countByTenantIdAndTsSince(tenantId, oneHourAgo);
            if (recentUsageCount > 0) {
                lastIngest = Instant.now();
            }
            
            pendingQueue = queueRepository.countByStatus(QueueStatus.PENDING);
            
            if (pendingQueue > 1000) {
                errors.add("High pending queue count: " + pendingQueue);
            }
            
        } catch (Exception e) {
            logger.error("Health check failed for tenant: {}", tenantId, e);
            errors.add("Health check error: " + e.getMessage());
        }
        
        String overallStatus = errors.isEmpty() ? "healthy" : "degraded";
        
        return HealthResponse.builder()
            .status(overallStatus)
            .lastCatalogSyncAt(lastCatalogSync)
            .lastCustomerSyncAt(lastCustomerSync)
            .lastIngestAt(lastIngest)
            .apigeeConnectivity(apigeeConnectivity)
            .pendingIngestQueue(pendingQueue)
            .errors(errors)
            .build();
    }
}
```

---

## 8. Controller

### 8.1 ApigeeIntegrationController.java

```java
package aforo.apigee.controller;

import aforo.apigee.dto.request.*;
import aforo.apigee.dto.response.*;
import aforo.apigee.security.HmacValidator;
import aforo.apigee.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/integrations/apigee")
@Tag(name = "Apigee Integration", description = "Apigee Integration APIs")
public class ApigeeIntegrationController {
    
    private static final Logger logger = LoggerFactory.getLogger(ApigeeIntegrationController.class);
    
    @Autowired
    private ApigeeConnectionService connectionService;
    
    @Autowired
    private ApigeeCatalogSyncService catalogSyncService;
    
    @Autowired
    private ApigeeCustomerSyncService customerSyncService;
    
    @Autowired
    private ApigeeIngestionService ingestionService;
    
    @Autowired
    private ApigeeEnforcementService enforcementService;
    
    @Autowired
    private ApigeeHealthService healthService;
    
    @Autowired
    private HmacValidator hmacValidator;
    
    @PostMapping("/connect")
    @Operation(summary = "Connect to Apigee", description = "Establish connection to Apigee and test connectivity")
    public ResponseEntity<ConnectApigeeResponse> connect(
            @RequestHeader(value = "X-Tenant-Id", required = true) Long tenantId,
            @Valid @RequestBody ConnectApigeeRequest request) {
        
        logger.info("Connecting to Apigee for tenant: {}", tenantId);
        ConnectApigeeResponse response = connectionService.connect(tenantId, request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/catalog/sync")
    @Operation(summary = "Sync catalog", description = "Sync API proxies and products from Apigee")
    public ResponseEntity<SyncDiffResponse> syncCatalog(
            @RequestHeader(value = "X-Tenant-Id", required = true) Long tenantId,
            @RequestParam(value = "dryRun", defaultValue = "false") Boolean dryRun,
            @RequestBody(required = false) CatalogSyncRequest request) {
        
        if (request == null) {
            request = new CatalogSyncRequest();
        }
        request.setDryRun(dryRun);
        
        logger.info("Syncing catalog for tenant: {}, dryRun: {}", tenantId, dryRun);
        SyncDiffResponse response = catalogSyncService.syncCatalog(tenantId, request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/customers/sync")
    @Operation(summary = "Sync customers", description = "Sync developers, apps, and keys from Apigee")
    public ResponseEntity<CustomersSyncDiffResponse> syncCustomers(
            @RequestHeader(value = "X-Tenant-Id", required = true) Long tenantId,
            @RequestParam(value = "dryRun", defaultValue = "false") Boolean dryRun,
            @RequestBody(required = false) CustomersSyncRequest request) {
        
        if (request == null) {
            request = new CustomersSyncRequest();
        }
        request.setDryRun(dryRun);
        
        logger.info("Syncing customers for tenant: {}, dryRun: {}", tenantId, dryRun);
        CustomersSyncDiffResponse response = customerSyncService.syncCustomers(tenantId, request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/ingest")
    @Operation(summary = "Ingest usage events", description = "Receive usage events from Apigee (single or batch)")
    public ResponseEntity<?> ingest(
            @RequestHeader(value = "X-Tenant-Id", required = true) Long tenantId,
            @RequestHeader(value = "X-Aforo-Signature", required = false) String signature,
            @RequestBody Object payload) {
        
        if (signature != null) {
            try {
                String rawBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
                if (!hmacValidator.validate(signature, rawBody)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("status", "error", "message", "Invalid signature"));
                }
            } catch (Exception e) {
                logger.error("Failed to validate HMAC signature", e);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Signature validation failed"));
            }
        }
        
        try {
            if (payload instanceof java.util.List) {
                IngestApigeeBatchRequest batch = new com.fasterxml.jackson.databind.ObjectMapper()
                    .convertValue(Map.of("events", payload), IngestApigeeBatchRequest.class);
                IngestResponse response = ingestionService.ingestBatch(tenantId, batch);
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            } else {
                IngestApigeeEventRequest event = new com.fasterxml.jackson.databind.ObjectMapper()
                    .convertValue(payload, IngestApigeeEventRequest.class);
                IngestResponse response = ingestionService.ingestSingle(tenantId, event);
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            }
        } catch (Exception e) {
            logger.error("Failed to ingest events", e);
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", "Invalid payload: " + e.getMessage()));
        }
    }
    
    @PostMapping("/events")
    @Operation(summary = "Receive Apigee events", description = "Webhook endpoint for Apigee audit log events")
    public ResponseEntity<Map<String, String>> receiveEvents(
            @RequestHeader(value = "X-Tenant-Id", required = true) Long tenantId,
            @RequestBody Map<String, Object> payload) {
        
        logger.info("Received Apigee event for tenant: {}", tenantId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(Map.of("status", "accepted", "message", "Event received"));
    }
    
    @PostMapping("/enforce/products")
    @Operation(summary = "Enforce API products", description = "Attach/detach API products to app keys based on plan")
    public ResponseEntity<EnforceResponse> enforceProducts(
            @RequestHeader(value = "X-Tenant-Id", required = true) Long tenantId,
            @Valid @RequestBody EnforceProductsRequest request) {
        
        logger.info("Enforcing products for tenant: {}, plan: {}", tenantId, request.getPlanCode());
        EnforceResponse response = enforcementService.enforceProducts(tenantId, request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/suspend")
    @Operation(summary = "Suspend customer", description = "Suspend a customer's access")
    public ResponseEntity<EnforceResponse> suspendCustomer(
            @RequestHeader(value = "X-Tenant-Id", required = true) Long tenantId,
            @Valid @RequestBody SuspendRequest request) {
        
        logger.info("Suspending customer for tenant: {}", tenantId);
        EnforceResponse response = enforcementService.suspendCustomer(tenantId, request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/resume/{customerId}")
    @Operation(summary = "Resume customer", description = "Resume a suspended customer's access")
    public ResponseEntity<EnforceResponse> resumeCustomer(
            @RequestHeader(value = "X-Tenant-Id", required = true) Long tenantId,
            @PathVariable Long customerId) {
        
        logger.info("Resuming customer for tenant: {}, customerId: {}", tenantId, customerId);
        EnforceResponse response = enforcementService.resumeCustomer(tenantId, customerId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check Apigee integration health")
    public ResponseEntity<HealthResponse> getHealth(
            @RequestHeader(value = "X-Tenant-Id", required = true) Long tenantId) {
        
        HealthResponse response = healthService.getHealth(tenantId);
        return ResponseEntity.ok(response);
    }
}
```

---

## 9. Scheduler

### 9.1 ApigeeIngestQueueProcessor.java

```java
package aforo.apigee.scheduler;

import aforo.apigee.model.ApigeeIngestQueue;
import aforo.apigee.model.enums.QueueStatus;
import aforo.apigee.repository.ApigeeIngestQueueRepository;
import aforo.apigee.service.impl.ApigeeIngestionServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ApigeeIngestQueueProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(ApigeeIngestQueueProcessor.class);
    
    @Autowired
    private ApigeeIngestQueueRepository queueRepository;
    
    @Autowired
    private ApigeeIngestionServiceImpl ingestionService;
    
    @Scheduled(fixedDelay = 5000)
    public void processQueue() {
        try {
            List<ApigeeIngestQueue> pendingItems = queueRepository
                .findPendingItems(QueueStatus.PENDING, Instant.now());
            
            if (!pendingItems.isEmpty()) {
                logger.info("Processing {} pending queue items", pendingItems.size());
            }
            
            for (ApigeeIngestQueue item : pendingItems) {
                try {
                    item.setStatus(QueueStatus.PROCESSING);
                    queueRepository.save(item);
                    
                    ingestionService.processQueueItem(item);
                    
                } catch (Exception e) {
                    logger.error("Failed to process queue item: {}", item.getId(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Queue processor error", e);
        }
    }
}
```

---

*Continuing with Liquibase migrations, configuration, tests, and sample commands in the next section...*
