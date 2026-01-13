# Apigee Integration Module - Complete Structure

## Package Structure

```
src/main/java/aforo/apigee/
├── controller/
│   └── ApigeeIntegrationController.java
├── service/
│   ├── ApigeeConnectionService.java
│   ├── ApigeeCatalogSyncService.java
│   ├── ApigeeCustomerSyncService.java
│   ├── ApigeeIngestionService.java
│   ├── ApigeeEnforcementService.java
│   └── ApigeeHealthService.java
├── service/impl/
│   ├── ApigeeConnectionServiceImpl.java
│   ├── ApigeeCatalogSyncServiceImpl.java
│   ├── ApigeeCustomerSyncServiceImpl.java
│   ├── ApigeeIngestionServiceImpl.java
│   ├── ApigeeEnforcementServiceImpl.java
│   └── ApigeeHealthServiceImpl.java
├── client/
│   ├── ApigeeClient.java (interface)
│   ├── ApigeeXClient.java
│   ├── ApigeeEdgeClient.java
│   └── ApigeeClientFactory.java
├── dto/
│   ├── request/
│   │   ├── ConnectApigeeRequest.java
│   │   ├── CatalogSyncRequest.java
│   │   ├── CustomersSyncRequest.java
│   │   ├── IngestApigeeEventRequest.java
│   │   ├── IngestApigeeBatchRequest.java
│   │   ├── EnforceProductsRequest.java
│   │   └── SuspendRequest.java
│   └── response/
│       ├── ConnectApigeeResponse.java
│       ├── SyncDiffResponse.java
│       ├── CustomersSyncDiffResponse.java
│       ├── IngestResponse.java
│       ├── EnforceResponse.java
│       └── HealthResponse.java
├── model/
│   ├── ApigeeConnection.java
│   ├── ApigeeProduct.java
│   ├── ApigeeEndpoint.java
│   ├── ApigeeCustomer.java
│   ├── ApigeeUsageRecord.java
│   ├── ApigeeSyncAudit.java
│   ├── ApigeeIngestQueue.java
│   └── enums/
│       ├── ApigeeType.java
│       ├── AuthType.java
│       ├── ConnectionStatus.java
│       ├── ProductKind.java
│       ├── EntityStatus.java
│       ├── SyncScope.java
│       ├── SyncSource.java
│       └── QueueStatus.java
├── repository/
│   ├── ApigeeConnectionRepository.java
│   ├── ApigeeProductRepository.java
│   ├── ApigeeEndpointRepository.java
│   ├── ApigeeCustomerRepository.java
│   ├── ApigeeUsageRecordRepository.java
│   ├── ApigeeSyncAuditRepository.java
│   └── ApigeeIngestQueueRepository.java
├── mapper/
│   └── ApigeeMapper.java
├── security/
│   ├── SecretVault.java (interface)
│   ├── SecretVaultImpl.java
│   └── HmacValidator.java
├── scheduler/
│   └── ApigeeIngestQueueProcessor.java
└── exception/
    ├── ApigeeConnectionException.java
    ├── ApigeeSyncException.java
    └── ApigeeValidationException.java

src/main/resources/db/changelog/
├── 020-create-apigee-connection.yaml
├── 021-create-apigee-product.yaml
├── 022-create-apigee-endpoint.yaml
├── 023-create-apigee-customer.yaml
├── 024-create-apigee-usage-record.yaml
├── 025-create-apigee-sync-audit.yaml
├── 026-create-apigee-ingest-queue.yaml
└── db.changelog-master.yaml (update)

src/test/java/aforo/apigee/
├── controller/
│   └── ApigeeIntegrationControllerTest.java
├── service/
│   ├── ApigeeCatalogSyncServiceTest.java
│   ├── ApigeeIngestionServiceTest.java
│   └── ApigeeEnforcementServiceTest.java
└── client/
    └── ApigeeXClientTest.java
```

## Files to Generate (Total: 50+ files)

### Entities (7)
### Enums (8)
### Repositories (7)
### DTOs (13)
### Services (12)
### Client (4)
### Security (3)
### Controller (1)
### Mapper (1)
### Scheduler (1)
### Exceptions (3)
### Liquibase (7)
### Tests (4)
### Config (1)
### Documentation (2)
