# Kong Konnect Integration - Implementation Summary

## Overview
This document summarizes the complete implementation of the Kong Konnect integration for Aforo, focusing exclusively on Konnect (not Apigee).

## Implementation Components

### 1. Database Layer

#### Migration Files
- **`013-create-konnect-api-product-map.yaml`**: Creates the mapping table between Konnect API products and Aforo products
  - Tracks import status (ACTIVE/DISABLED)
  - Maintains snapshots of product names and descriptions
  - Ensures uniqueness per organization

#### Entities
- **`KonnectApiProductMap`**: Maps Konnect API products to Aforo products
  - Fields: orgId, konnectApiProductId, aforoProductId, nameSnapshot, descriptionSnapshot, status, lastSeenAt
  - Prevents duplicate imports
  - Enables sync tracking

#### Repositories
- **`KonnectApiProductMapRepository`**: Data access for product mappings
- **`ClientApiDetailsRepository`** (enhanced): Added methods for finding connections by organization and environment

### 2. Security & Encryption

#### Encryption Utility
- **`EncryptionUtil`**: AES encryption for PAT tokens
  - Encrypts tokens before storing in database
  - Decrypts tokens when making API calls
  - Configurable secret key via `encryption.secret.key` property

### 3. DTOs (Data Transfer Objects)

Created in package `aforo.kong.dto.konnect`:

- **`KonnectConnectionRequestDTO`**: Request to create/update connection
- **`KonnectConnectionResponseDTO`**: Connection creation response
- **`KonnectTestResponseDTO`**: Connection test result
- **`KonnectApiProductDTO`**: API product information from Konnect
- **`KonnectImportRequestDTO`**: Import request with selected product IDs
- **`KonnectImportResponseDTO`**: Import operation results
- **`KonnectImportedProductDTO`**: Imported product details
- **`KonnectSyncPreviewDTO`**: Preview of sync changes (added/removed/changed)

### 4. Client Layer

#### KonnectWebClient
- **Purpose**: HTTP client for Konnect API communication
- **Key Methods**:
  - `testConnection()`: Validates connection and credentials
  - `listControlPlanes()`: Fetches available control planes
  - `listApiProducts()`: Retrieves all API products
  - `getApiProductById()`: Fetches specific product details
- **Features**:
  - Bearer token authentication
  - JSON parsing with Jackson
  - Error handling and logging

### 5. Service Layer

#### KonnectService Interface & Implementation
- **`KonnectServiceImpl`**: Core business logic

**Key Methods**:

1. **`createOrUpdateConnection()`**
   - Saves/updates connection details
   - Encrypts PAT token
   - Auto-selects first control plane if not provided
   - Tests connection before saving
   - Sets status to CONNECTED or FAILED

2. **`testConnection()`**
   - Validates existing connection
   - Returns API product count
   - Quick health check (5-second timeout)

3. **`fetchApiProducts()`**
   - Retrieves live API products from Konnect
   - Does not persist to database
   - Returns list for UI display

4. **`importApiProducts()`**
   - Imports selected products into Aforo
   - Creates KongProduct entries
   - Creates mapping entries
   - Handles updates for existing products
   - Returns import statistics (imported/updated/failed)

5. **`listImportedProducts()`**
   - Returns products already imported to Aforo
   - Filters by ACTIVE status
   - Joins with KongProduct table

6. **`previewSync()`**
   - Compares live Konnect data with imported data
   - Identifies: added, removed, changed products
   - Does not modify database

7. **`applySync()`**
   - Applies sync changes
   - Disables removed products
   - Imports/updates added and changed products

8. **`autoRefresh()`**
   - Called by scheduler
   - Auto-syncs all connected organizations
   - Runs every 120 seconds

### 6. Controller Layer

#### KonnectController
- **Base Path**: `/api/integrations/konnect`
- **Organization ID**: Passed via `X-Organization-Id` header

**Endpoints**:

| Method | Path | Description |
|--------|------|-------------|
| POST | `/connection` | Create/update Konnect connection |
| GET | `/connection/test` | Test existing connection |
| GET | `/api-products` | Fetch live API products from Konnect |
| POST | `/api-products/import` | Import selected products |
| GET | `/api-products/imported` | List imported products |
| POST | `/catalog/preview` | Preview sync changes |
| POST | `/catalog/apply` | Apply sync changes |

### 7. Scheduler

#### KonnectAutoRefreshScheduler
- **Frequency**: Every 120 seconds (2 minutes)
- **Function**: Automatically syncs changes for all connected organizations
- **Enabled**: Via `@EnableScheduling` on main application class

## API Flow Examples

### 1. Create Connection
```
POST /api/integrations/konnect/connection
Header: X-Organization-Id: 1
Body: {
  "name": "Konnect US",
  "baseUrl": "https://us.api.konghq.com",
  "authToken": "kpat_xxx",
  "controlPlaneId": "optional",
  "region": "us"
}

Response: {
  "connectionId": 1,
  "status": "connected",
  "controlPlaneId": "22ef7dda-...",
  "message": "Connection successful"
}
```

### 2. Fetch API Products
```
GET /api/integrations/konnect/api-products
Header: X-Organization-Id: 1

Response: [
  {
    "konnectApiProductId": "abc123",
    "name": "Payment API",
    "description": "Payment processing",
    "status": "published",
    "versionsCount": 2,
    "updatedAt": "2025-01-02T10:00:00Z"
  }
]
```

### 3. Import Products
```
POST /api/integrations/konnect/api-products/import
Header: X-Organization-Id: 1
Body: {
  "selectedApiProductIds": ["abc123", "def456"]
}

Response: {
  "imported": 2,
  "updated": 0,
  "failed": 0,
  "items": [
    {
      "konnectApiProductId": "abc123",
      "aforoProductId": 101,
      "action": "CREATED"
    }
  ]
}
```

### 4. Preview Sync
```
POST /api/integrations/konnect/catalog/preview
Header: X-Organization-Id: 1

Response: {
  "added": [...],
  "removed": [...],
  "changed": [...]
}
```

## Key Features Implemented

✅ **Connection Management**
- One connection per organization
- Encrypted PAT storage
- Auto control plane selection
- Connection testing

✅ **Product Import**
- Idempotent imports (no duplicates)
- Bulk import support
- Update existing products
- Track import status

✅ **Sync Management**
- Preview before applying
- Detect added/removed/changed products
- Disable removed products (not delete)
- Auto-sync every 2 minutes

✅ **Security**
- PAT encryption at rest
- No PAT logging
- Safe error messages
- Timeout protection

✅ **Data Integrity**
- Unique constraint on org + product ID
- Transactional operations
- Snapshot tracking
- Audit timestamps

## Configuration Required

Add to `application.properties` or `application.yml`:

```properties
# Encryption key for PAT tokens (16 characters minimum)
encryption.secret.key=your-secret-key-here-32-chars
```

## Database Schema

### konnect_api_product_map
```sql
CREATE TABLE konnect_api_product_map (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  konnect_api_product_id VARCHAR(255) NOT NULL,
  aforo_product_id BIGINT,
  name_snapshot VARCHAR(500),
  description_snapshot TEXT,
  status VARCHAR(50) DEFAULT 'ACTIVE' NOT NULL,
  last_seen_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  UNIQUE KEY uk_org_konnect_product (org_id, konnect_api_product_id)
);
```

## Testing Checklist

- [ ] Create connection with valid PAT
- [ ] Test connection endpoint
- [ ] Fetch API products from Konnect
- [ ] Import selected products
- [ ] List imported products
- [ ] Preview sync changes
- [ ] Apply sync
- [ ] Verify auto-refresh runs every 2 minutes
- [ ] Test with invalid PAT (should fail gracefully)
- [ ] Test duplicate import (should update, not create)
- [ ] Verify PAT encryption in database

## Next Steps / Enhancements

1. **Error Handling**: Add custom exceptions and global error handler
2. **Validation**: Add input validation with `@Valid` annotations
3. **Pagination**: Add pagination for large product lists
4. **Filtering**: Add search/filter capabilities
5. **Webhooks**: Listen to Konnect webhooks for real-time sync
6. **Metrics**: Add monitoring and metrics collection
7. **Audit Log**: Track all sync operations
8. **Multi-region**: Support multiple Konnect regions per org

## Files Created/Modified

### Created Files
1. `/src/main/resources/db/changelog/013-create-konnect-api-product-map.yaml`
2. `/src/main/java/aforo/kong/util/EncryptionUtil.java`
3. `/src/main/java/aforo/kong/dto/konnect/*.java` (8 DTOs)
4. `/src/main/java/aforo/kong/entity/KonnectApiProductMap.java`
5. `/src/main/java/aforo/kong/repository/KonnectApiProductMapRepository.java`
6. `/src/main/java/aforo/kong/client/KonnectWebClient.java`
7. `/src/main/java/aforo/kong/service/KonnectService.java`
8. `/src/main/java/aforo/kong/service/impl/KonnectServiceImpl.java`
9. `/src/main/java/aforo/kong/controller/KonnectController.java`
10. `/src/main/java/aforo/kong/scheduler/KonnectAutoRefreshScheduler.java`

### Modified Files
1. `/src/main/resources/db/changelog/db.changelog-master.yaml` - Added migration reference
2. `/src/main/java/aforo/kong/repository/ClientApiDetailsRepository.java` - Added query methods
3. `/src/main/java/aforo/kong/KongApplication.java` - Enabled scheduling

## Architecture Decisions

1. **Reused ClientApiDetails**: Instead of creating a separate KonnectConnection entity, we reused the existing `client_api_details` table with `environment='konnect'` to maintain consistency.

2. **Mapping Table**: Created separate mapping table to track import status and enable idempotent operations.

3. **Soft Delete**: Products removed from Konnect are marked as DISABLED, not deleted, to preserve history.

4. **Encryption**: Used AES encryption for PAT tokens to meet security requirements.

5. **Auto-refresh**: Implemented as scheduled job rather than webhook to maintain simplicity and avoid external dependencies.

## Implementation Complete ✅

All components specified in the implementation brief have been created and integrated. The system is ready for testing and deployment.
