# Multi-Tenant Security Implementation for Apigee APIs

## Summary of Changes

This document describes the security enhancements made to enforce multi-tenant isolation for all Apigee integration APIs.

## Problem Statement

**Security Vulnerability Identified:**
- Apigee API endpoints were not properly secured with JWT authentication
- The `/products` endpoint accepted an optional `org` parameter that could be manipulated
- Users from Organization A could access Organization B's data by passing a different `org` parameter
- No validation was performed to ensure the `org` parameter matched the authenticated user's organization

**Example of the vulnerability:**
```
GET /api/integrations/apigee/products?org=other-organization
```
This would return products from `other-organization` regardless of which organization the JWT token belonged to.

## Solution Implemented

### 1. Database Schema Changes

**Added `organization_id` to `connection_configs` table:**
- Links each Apigee connection to a specific organization
- Ensures one connection per organization
- Migration file: `022-add-organization-id-to-connection-configs.yaml`

```sql
ALTER TABLE connection_configs 
ADD COLUMN organization_id BIGINT;

CREATE INDEX idx_connection_configs_org ON connection_configs(organization_id);
CREATE UNIQUE CONSTRAINT uk_connection_configs_org_id ON connection_configs(organization_id);
```

### 2. Service Layer Changes

**Updated `InventoryService` interface:**
- Removed `org` parameter from all methods
- Methods now use `organizationId` from `TenantContext` (JWT token)
- Added `organizationId` parameter to `saveAndTestConnection()`

**Updated `InventoryServiceImpl`:**
- All methods now fetch the connection config using `organizationId` from JWT token
- Queries: `connectionConfigRepository.findByOrganizationId(organizationId)`
- Throws exception if no connection is configured for the organization
- Uses the organization-specific Apigee `org` from the connection config

**Updated `MappingServiceImpl`:**
- Now uses organization-specific Apigee org from connection config
- Validates organization access before creating mappings

### 3. Controller Layer Changes

**Updated `ApigeeIntegrationController`:**

All endpoints now have:
1. **JWT Authentication Required:** `@PreAuthorize("isAuthenticated()")`
2. **Security Documentation:** `@SecurityRequirement(name = "bearerAuth")`
3. **Organization Extraction:** `Long organizationId = TenantContext.require();`
4. **Removed `org` parameter:** No longer accepts user-provided org values

**Secured Endpoints:**
- `POST /connections` - Save Apigee connection (requires JWT)
- `GET /products` - List API products (requires JWT, removed org param)
- `POST /products/import-selected` - Import products (requires JWT)
- `POST /sync` - Sync products (requires JWT, removed org param)
- `GET /developers` - List developers (requires JWT, removed org param)
- `GET /developers/{developerId}/apps` - List apps (requires JWT, removed org param)
- `POST /developers/{developerId}/link` - Link developer (requires JWT)
- `POST /mappings/subscriptions` - Create mapping (requires JWT)
- `POST /authorize` - Authorization decision (requires JWT)
- `POST /webhooks/usage` - Ingest usage (requires JWT)
- `POST /customers/sync` - Sync customers (requires JWT)

### 4. Authentication Flow

```
1. User sends request with JWT token in Authorization header
2. JwtTenantFilter extracts organizationId from JWT claims
3. TenantContext stores organizationId in ThreadLocal
4. Controller calls TenantContext.require() to get organizationId
5. Service layer uses organizationId to fetch organization-specific connection config
6. Service layer uses the Apigee org from connection config (not user input)
7. TenantContext is cleared after request completes
```

## Security Benefits

### Before (Vulnerable):
```java
@GetMapping("/products")
public ResponseEntity<List<ApiProductResponse>> listProducts(@RequestParam(required = false) String org) {
    // User can pass any org value - NO VALIDATION!
    List<ApiProductResponse> products = inventoryService.getApiProducts(org);
    return ResponseEntity.ok(products);
}
```

### After (Secure):
```java
@GetMapping("/products")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<List<ApiProductResponse>> listProducts() {
    // organizationId comes from JWT token - CANNOT BE MANIPULATED
    Long organizationId = TenantContext.require();
    List<ApiProductResponse> products = inventoryService.getApiProducts();
    return ResponseEntity.ok(products);
}
```

## Data Isolation

Each organization now has:
1. **Separate Apigee Connection:** Stored in `connection_configs` with unique `organization_id`
2. **Isolated Data Access:** All queries filter by `organization_id` from JWT token
3. **No Cross-Organization Access:** Users cannot access other organizations' data

## Migration Steps

1. **Run Database Migration:**
   - Liquibase will automatically apply `022-add-organization-id-to-connection-configs.yaml`
   - Existing connections will have `organization_id = NULL` initially
   - Admin must update existing connections with proper `organization_id` values

2. **Update Existing Connections:**
   ```sql
   UPDATE connection_configs 
   SET organization_id = <actual_org_id> 
   WHERE org = '<apigee_org_name>';
   ```

3. **Test with JWT Token:**
   - All requests must include valid JWT token with `organizationId` claim
   - Test that users can only access their own organization's data
   - Verify that attempting to access other organizations' data fails

## Breaking Changes

⚠️ **API Changes:**
- All Apigee endpoints now require JWT authentication
- `org` parameter removed from endpoints (ignored if provided)
- Requests without valid JWT token will return 401 Unauthorized
- Requests without `organizationId` in JWT will return 401 Unauthorized

## Testing Checklist

- [ ] Create connection with JWT token for Organization A
- [ ] Verify Organization A can list their products
- [ ] Verify Organization A cannot see Organization B's products
- [ ] Verify requests without JWT token are rejected (401)
- [ ] Verify requests with invalid JWT token are rejected (401)
- [ ] Verify all endpoints require authentication
- [ ] Test product import, sync, developer listing, etc.

## Files Modified

### Entity Classes:
- `ConnectionConfig.java` - Added `organizationId` field

### Repository Classes:
- `ConnectionConfigRepository.java` - Added `findByOrganizationId()` method

### Service Interfaces:
- `InventoryService.java` - Removed `org` parameters, added `organizationId` to saveAndTestConnection

### Service Implementations:
- `InventoryServiceImpl.java` - Uses `TenantContext` and queries by `organizationId`
- `MappingServiceImpl.java` - Uses organization-specific connection config

### Controllers:
- `ApigeeIntegrationController.java` - Added JWT auth to all endpoints, removed `org` parameters

### Database Migrations:
- `022-add-organization-id-to-connection-configs.yaml` - New migration file
- `db.changelog-master.yaml` - Added reference to new migration

## Configuration

No configuration changes required. The existing JWT configuration in `application.yml` is sufficient:

```yaml
aforo:
  jwt:
    issuer: aforo-kong
    secret: ${JWT_SECRET:change-me-please-change-me-32-bytes-min}
    expiryMinutes: 120
```

## JWT Token Requirements

The JWT token must include one of these claims with the organization ID:
- `organizationId`
- `orgId`
- `tenantId`
- `organization_id`
- `org_id`
- `tenant`

Example JWT payload:
```json
{
  "sub": "user@example.com",
  "organizationId": 123,
  "iat": 1234567890,
  "exp": 1234574890
}
```

## Error Handling

**401 Unauthorized:**
- Missing JWT token
- Invalid JWT token
- Missing `organizationId` in JWT claims

**400 Bad Request / 500 Internal Server Error:**
- No Apigee connection configured for organization
- Message: "No Apigee connection configured for organization: {organizationId}"

## Rollback Plan

If issues arise, rollback by:
1. Reverting code changes to previous commit
2. Rolling back database migration:
   ```sql
   DELETE FROM databasechangelog WHERE id = '022-add-organization-id-to-connection-configs';
   ALTER TABLE connection_configs DROP COLUMN organization_id;
   ```

## Future Enhancements

1. Add organization-level rate limiting
2. Add audit logging for cross-organization access attempts
3. Add admin API to manage organization connections
4. Add organization-level feature flags
