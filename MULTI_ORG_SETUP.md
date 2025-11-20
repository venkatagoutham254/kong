# Multi-Organization Setup Guide

## Problem
Previously, the Apigee organization was hardcoded in `application.yml`, which doesn't work when different customers have different Apigee organizations.

## Solution
The application now uses a **database-first approach** where the organization configuration is stored in the database and retrieved dynamically.

## How It Works

### 1. Save Organization Configuration
Use the `/connections` endpoint to save a customer's Apigee organization:

```bash
POST /api/integrations/apigee/connections
Content-Type: application/json

{
  "org": "customer-org-name",
  "envs": "prod,test",
  "analyticsMode": "WEBHOOK",
  "hmacSecret": "customer-secret",
  "saJsonPath": "/path/to/customer-sa.json"
}
```

This saves the configuration to the `connection_config` table in PostgreSQL.

### 2. Automatic Org Selection
When you call any API endpoint (products, developers, apps), the system:
1. ✅ Checks the database for saved organization configurations
2. ✅ Uses the most recently saved org
3. ✅ Falls back to the default org from `application.yml` if no saved config exists

### 3. Supported Endpoints
All these endpoints now use the dynamic org from database:
- `GET /api/integrations/apigee/products` 
- `GET /api/integrations/apigee/developers`
- `GET /api/integrations/apigee/developers/{email}/apps`
- `POST /api/integrations/apigee/developers/{id}/link`

## Usage Flow

### For Each New Customer:

1. **Customer provides their Apigee credentials**
   - Organization name
   - Service account JSON file
   - HMAC secret (optional)

2. **Call the connections endpoint**
   ```bash
   curl -X POST 'http://localhost:8086/api/integrations/apigee/connections' \
     -H 'Content-Type: application/json' \
     -d '{
       "org": "new-customer-org",
       "envs": "prod",
       "analyticsMode": "WEBHOOK",
       "hmacSecret": "their-secret",
       "saJsonPath": "/path/to/their-sa.json"
     }'
   ```

3. **All subsequent API calls automatically use this org**
   - No need to pass org as parameter
   - No need to restart the application
   - No need to modify configuration files

## Database Schema

```sql
CREATE TABLE connection_config (
    id BIGSERIAL PRIMARY KEY,
    org VARCHAR(255) NOT NULL,
    envs_csv TEXT,
    analytics_mode VARCHAR(50),
    hmac_secret VARCHAR(255),
    sa_json_path TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

## Future Enhancements

### Multi-Tenant Support (Future)
To support multiple customers simultaneously:

1. Add `tenant_id` or `customer_id` to `connection_config` table
2. Pass tenant identifier in API requests (header or path parameter)
3. Look up the specific org for that tenant

Example:
```java
// Add to ConnectionConfig entity
@Column(name = "tenant_id")
private String tenantId;

// Modify getActiveOrg()
private String getActiveOrg(String tenantId) {
    return connectionConfigRepository
        .findByTenantId(tenantId)
        .map(ConnectionConfig::getOrg)
        .orElse(defaultOrg);
}
```

## Benefits

✅ **No hardcoding** - Each customer's org is stored dynamically
✅ **No restart needed** - Just call `/connections` endpoint
✅ **Database-backed** - Persistent across application restarts  
✅ **Fallback support** - Uses default org if no saved config
✅ **Easy testing** - Switch between orgs by calling `/connections`

## Notes

- Currently uses the **first/most recent** saved org from database
- For multi-tenant scenarios, add tenant identification logic
- Service account JSON files must be accessible at the specified paths
- HMAC secret is only required for webhook endpoints
