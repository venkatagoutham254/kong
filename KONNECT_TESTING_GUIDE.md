# Kong Konnect Integration - Testing Guide

## Prerequisites: What You Need from Kong Konnect

### 1. Personal Access Token (PAT)
**Where to get it:**
1. Log in to Kong Konnect: https://cloud.konghq.com/
2. Go to **Personal Access Tokens** (in your profile menu)
3. Click **Generate Token**
4. Copy the token (starts with `kpat_`)

**Example format:**
```
kpat_d8AZEzsVCkcgpDQaVW9rDE691CQRKYsrFevY0mK6HQ2jeIgNB
```

⚠️ **IMPORTANT**: Save this token - you won't see it again!

---

### 2. Konnect Region Base URL
Choose based on your region:
- **US**: `https://us.api.konghq.com`
- **EU**: `https://eu.api.konghq.com`
- **AU**: `https://au.api.konghq.com`

---

### 3. Control Plane ID (Optional)
**Where to get it:**
1. In Konnect dashboard, go to **Gateway Manager**
2. Click on your control plane
3. Copy the **Control Plane ID** from the URL or details page

**Example format:**
```
22ef7dda-4ad5-45c4-8079-001ac07ddcad
```

**Note**: If you don't provide this, the system will auto-select the first control plane.

---

### 4. Organization ID
This is your internal Aforo organization ID (from your database).

**Example**: `1` (for testing, use org ID 1)

---

## Before Testing: Configuration Setup

### Step 1: Generate Encryption Key
```bash
openssl rand -base64 32
```

**Example output:**
```
xK8pL2mN9qR3sT5uVwXyZ1aB2cD3eF4gH5iJ6kL7mN8o=
```

### Step 2: Update application.properties
Add this line:
```properties
encryption.secret.key=xK8pL2mN9qR3sT5uVwXyZ1aB2cD3eF4gH5iJ6kL7mN8o=
```

### Step 3: Start the Application
```bash
./mvnw spring-boot:run
```

**Verify startup:**
- Check logs for: `Started KongApplication`
- No encryption errors
- Scheduler started: `KonnectAutoRefreshScheduler`

---

## Test Scenarios

### Test 1: Create/Update Connection ✅

**Endpoint:** `POST /api/integrations/konnect/connection`

**Headers:**
```
Content-Type: application/json
X-Organization-Id: 1
```

**Request Body:**
```json
{
  "name": "Konnect US Production",
  "description": "Kong Konnect US region connection",
  "baseUrl": "https://us.api.konghq.com",
  "authToken": "kpat_YOUR_ACTUAL_TOKEN_HERE",
  "region": "us"
}
```

**Expected Response (Success):**
```json
{
  "connectionId": 1,
  "status": "connected",
  "controlPlaneId": "22ef7dda-4ad5-45c4-8079-001ac07ddcad",
  "message": "Connection successful"
}
```

**Expected Response (Failure):**
```json
{
  "connectionId": 1,
  "status": "failed",
  "controlPlaneId": null,
  "message": "Connection failed"
}
```

**What to verify:**
1. Response status: `200 OK`
2. `status` field is `"connected"`
3. `controlPlaneId` is populated
4. Check database - token should start with `v1:`

**Database Check:**
```sql
SELECT id, name, connection_status, auth_token 
FROM client_api_details 
WHERE organization_id = 1 AND environment = 'konnect';
```

**Expected:**
- `connection_status` = `'connected'`
- `auth_token` starts with `'v1:'`

---

### Test 2: Test Connection ✅

**Endpoint:** `GET /api/integrations/konnect/connection/test`

**Headers:**
```
X-Organization-Id: 1
```

**Expected Response:**
```json
{
  "ok": true,
  "message": "Connected successfully",
  "counts": {
    "apiProducts": 5
  }
}
```

**What to verify:**
1. `ok` is `true`
2. `apiProducts` count matches your Konnect dashboard
3. Response time < 5 seconds

---

### Test 3: Fetch API Products ✅

**Endpoint:** `GET /api/integrations/konnect/api-products`

**Headers:**
```
X-Organization-Id: 1
```

**Expected Response:**
```json
[
  {
    "konnectApiProductId": "abc123-def456-ghi789",
    "name": "Payment API",
    "description": "Payment processing API",
    "status": "published",
    "versionsCount": 2,
    "updatedAt": "2025-01-02T10:00:00Z"
  },
  {
    "konnectApiProductId": "xyz789-uvw456-rst123",
    "name": "User Management API",
    "description": "User authentication and management",
    "status": "published",
    "versionsCount": 1,
    "updatedAt": "2025-01-01T15:30:00Z"
  }
]
```

**What to verify:**
1. Array of products returned
2. Product names match Konnect dashboard
3. Each product has valid `konnectApiProductId`
4. No products are persisted yet (this is live data)

---

### Test 4: Import API Products ✅

**Endpoint:** `POST /api/integrations/konnect/api-products/import`

**Headers:**
```
Content-Type: application/json
X-Organization-Id: 1
```

**Request Body:**
```json
{
  "selectedApiProductIds": [
    "abc123-def456-ghi789",
    "xyz789-uvw456-rst123"
  ]
}
```

**Expected Response:**
```json
{
  "imported": 2,
  "updated": 0,
  "failed": 0,
  "items": [
    {
      "konnectApiProductId": "abc123-def456-ghi789",
      "aforoProductId": 101,
      "action": "CREATED"
    },
    {
      "konnectApiProductId": "xyz789-uvw456-rst123",
      "aforoProductId": 102,
      "action": "CREATED"
    }
  ]
}
```

**What to verify:**
1. `imported` count matches selected products
2. `failed` is `0`
3. Each item has `aforoProductId` assigned
4. Action is `"CREATED"` for first import

**Database Check:**
```sql
-- Check kong_product table
SELECT internal_id, external_id, name, source 
FROM kong_product 
WHERE organization_id = 1 AND source = 'konnect';

-- Check mapping table
SELECT id, konnect_api_product_id, aforo_product_id, status 
FROM konnect_api_product_map 
WHERE org_id = 1;
```

**Expected:**
- 2 rows in `kong_product` with `source = 'konnect'`
- 2 rows in `konnect_api_product_map` with `status = 'ACTIVE'`

---

### Test 5: List Imported Products ✅

**Endpoint:** `GET /api/integrations/konnect/api-products/imported`

**Headers:**
```
X-Organization-Id: 1
```

**Expected Response:**
```json
[
  {
    "aforoProductId": 101,
    "name": "Payment API",
    "description": "Payment processing API",
    "konnectApiProductId": "abc123-def456-ghi789",
    "lastSeenAt": "2025-01-02T10:30:00Z"
  },
  {
    "aforoProductId": 102,
    "name": "User Management API",
    "description": "User authentication and management",
    "konnectApiProductId": "xyz789-uvw456-rst123",
    "lastSeenAt": "2025-01-02T10:30:00Z"
  }
]
```

**What to verify:**
1. Only imported products shown
2. `aforoProductId` matches database
3. `lastSeenAt` is recent

---

### Test 6: Re-import Same Product (Idempotency) ✅

**Endpoint:** `POST /api/integrations/konnect/api-products/import`

**Request Body:**
```json
{
  "selectedApiProductIds": [
    "abc123-def456-ghi789"
  ]
}
```

**Expected Response:**
```json
{
  "imported": 0,
  "updated": 1,
  "failed": 0,
  "items": [
    {
      "konnectApiProductId": "abc123-def456-ghi789",
      "aforoProductId": 101,
      "action": "UPDATED"
    }
  ]
}
```

**What to verify:**
1. `updated` is `1`, `imported` is `0`
2. Action is `"UPDATED"`
3. Same `aforoProductId` (101)
4. No duplicate rows in database

---

### Test 7: Preview Sync ✅

**Endpoint:** `POST /api/integrations/konnect/catalog/preview`

**Headers:**
```
X-Organization-Id: 1
```

**Expected Response (no changes):**
```json
{
  "added": [],
  "removed": [],
  "changed": []
}
```

**To test with changes:**
1. Add a new API product in Konnect dashboard
2. Call preview again

**Expected Response (with new product):**
```json
{
  "added": [
    {
      "konnectApiProductId": "new123-product-id",
      "name": "New API",
      "description": "Newly added API",
      "status": "published",
      "versionsCount": 1,
      "updatedAt": "2025-01-02T11:00:00Z"
    }
  ],
  "removed": [],
  "changed": []
}
```

**What to verify:**
1. `added` contains products in Konnect but not imported
2. `removed` contains imported products deleted from Konnect
3. `changed` contains products with name/description updates

---

### Test 8: Apply Sync ✅

**Endpoint:** `POST /api/integrations/konnect/catalog/apply`

**Headers:**
```
X-Organization-Id: 1
```

**Expected Response:**
```json
{
  "imported": 1,
  "updated": 0,
  "failed": 0,
  "items": [
    {
      "konnectApiProductId": "new123-product-id",
      "aforoProductId": 103,
      "action": "CREATED"
    }
  ]
}
```

**What to verify:**
1. New products imported
2. Changed products updated
3. Removed products marked as `DISABLED` in mapping table

**Database Check:**
```sql
SELECT konnect_api_product_id, status 
FROM konnect_api_product_map 
WHERE org_id = 1 AND status = 'DISABLED';
```

---

### Test 9: Concurrent Sync (Locking) ✅

**Test concurrent requests:**

**Terminal 1:**
```bash
curl -X POST http://localhost:8080/api/integrations/konnect/catalog/apply \
  -H "X-Organization-Id: 1"
```

**Terminal 2 (immediately after):**
```bash
curl -X POST http://localhost:8080/api/integrations/konnect/catalog/apply \
  -H "X-Organization-Id: 1"
```

**Expected Behavior:**
1. First request acquires lock and processes
2. Second request waits for lock
3. Both complete successfully (no race condition)
4. Check logs for: `"Acquired sync lock for org: 1"`

---

### Test 10: Auto-Refresh Scheduler ✅

**Wait 2 minutes after starting application**

**Check logs for:**
```
Starting Konnect auto-refresh job
Auto-syncing changes for org: 1
Completed Konnect auto-refresh job
```

**What to verify:**
1. Job runs every 120 seconds
2. Only syncs orgs with changes
3. Skips orgs already being synced
4. No errors in logs

---

## Error Scenarios to Test

### Test E1: Invalid Token ❌

**Request:**
```json
{
  "name": "Test",
  "baseUrl": "https://us.api.konghq.com",
  "authToken": "invalid-token-123"
}
```

**Expected Response:**
```json
{
  "connectionId": 1,
  "status": "failed",
  "controlPlaneId": null,
  "message": "Connection failed"
}
```

**What to verify:**
1. Status is `"failed"`
2. Error message is generic (no token leaked)
3. Logs contain actual error details

---

### Test E2: Invalid Base URL ❌

**Request:**
```json
{
  "name": "Test",
  "baseUrl": "https://invalid-url.com",
  "authToken": "kpat_valid_token"
}
```

**Expected:**
- Connection fails
- Generic error message
- Timeout after 5 seconds (connect timeout)

---

### Test E3: Missing Organization ID ❌

**Request without header:**
```bash
curl -X GET http://localhost:8080/api/integrations/konnect/api-products
```

**Expected:**
- HTTP 400 or 500
- Error about missing organization ID

---

### Test E4: Non-existent Connection ❌

**Request with org that has no connection:**
```
X-Organization-Id: 999
```

**Expected Response:**
```
RuntimeException: Konnect connection not found for organization: 999
```

---

## Complete Test Checklist

### Pre-Deployment Tests
- [ ] Generate encryption key
- [ ] Configure `encryption.secret.key`
- [ ] Application starts without errors
- [ ] Database migrations applied (check `databasechangelog`)

### Connection Tests
- [ ] Create connection with valid token
- [ ] Verify token encrypted with `v1:` prefix in database
- [ ] Test connection endpoint returns success
- [ ] Invalid token returns generic error
- [ ] Control plane auto-selected if not provided

### Product Tests
- [ ] Fetch API products returns live data
- [ ] Product count matches Konnect dashboard
- [ ] Import products creates database entries
- [ ] Re-import same product updates (not duplicates)
- [ ] List imported shows only imported products

### Sync Tests
- [ ] Preview sync detects added products
- [ ] Preview sync detects removed products
- [ ] Preview sync detects changed products
- [ ] Apply sync imports new products
- [ ] Apply sync disables removed products (only after successful import)
- [ ] Concurrent syncs don't cause race conditions

### Scheduler Tests
- [ ] Auto-refresh runs every 120 seconds
- [ ] Auto-refresh only syncs orgs with changes
- [ ] Auto-refresh skips orgs being synced manually
- [ ] No errors in scheduler logs

### Security Tests
- [ ] Error messages don't leak sensitive info
- [ ] Tokens encrypted in database
- [ ] Legacy tokens can be decrypted
- [ ] Timeouts prevent hanging requests

---

## Quick Test Script (Copy-Paste Ready)

Save as `test-konnect.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"
ORG_ID="1"
KONNECT_TOKEN="YOUR_KONNECT_PAT_HERE"
KONNECT_BASE_URL="https://us.api.konghq.com"

echo "=== Test 1: Create Connection ==="
curl -X POST "$BASE_URL/api/integrations/konnect/connection" \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: $ORG_ID" \
  -d "{
    \"name\": \"Test Connection\",
    \"baseUrl\": \"$KONNECT_BASE_URL\",
    \"authToken\": \"$KONNECT_TOKEN\",
    \"region\": \"us\"
  }" | jq

echo -e "\n=== Test 2: Test Connection ==="
curl -X GET "$BASE_URL/api/integrations/konnect/connection/test" \
  -H "X-Organization-Id: $ORG_ID" | jq

echo -e "\n=== Test 3: Fetch API Products ==="
curl -X GET "$BASE_URL/api/integrations/konnect/api-products" \
  -H "X-Organization-Id: $ORG_ID" | jq

echo -e "\n=== Test 4: Import Products (update with actual IDs) ==="
# Get product IDs from previous response and update here
curl -X POST "$BASE_URL/api/integrations/konnect/api-products/import" \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: $ORG_ID" \
  -d '{
    "selectedApiProductIds": ["REPLACE_WITH_ACTUAL_PRODUCT_ID"]
  }' | jq

echo -e "\n=== Test 5: List Imported ==="
curl -X GET "$BASE_URL/api/integrations/konnect/api-products/imported" \
  -H "X-Organization-Id: $ORG_ID" | jq

echo -e "\n=== Test 6: Preview Sync ==="
curl -X POST "$BASE_URL/api/integrations/konnect/catalog/preview" \
  -H "X-Organization-Id: $ORG_ID" | jq

echo -e "\n=== Test 7: Apply Sync ==="
curl -X POST "$BASE_URL/api/integrations/konnect/catalog/apply" \
  -H "X-Organization-Id: $ORG_ID" | jq
```

**Usage:**
```bash
chmod +x test-konnect.sh
./test-konnect.sh
```

---

## What to Provide for Testing

Please provide:

1. ✅ **Kong Konnect PAT Token** (starts with `kpat_`)
2. ✅ **Konnect Region** (US/EU/AU)
3. ✅ **Control Plane ID** (optional - will auto-select if not provided)
4. ✅ **At least 2-3 API Products** created in your Konnect dashboard

**Once you provide these, I can:**
- Update the test script with real values
- Run the complete test suite
- Verify all endpoints work correctly
- Check database state after each operation
- Confirm encryption, locking, and sync work as expected

---

## Success Criteria

All tests pass when:
- ✅ Connection created and status = `"connected"`
- ✅ Tokens encrypted with `v1:` prefix
- ✅ API products fetched from Konnect
- ✅ Products imported without duplicates
- ✅ Sync detects and applies changes
- ✅ Concurrent syncs don't cause errors
- ✅ Auto-refresh runs every 2 minutes
- ✅ Error messages are generic (no info leakage)
- ✅ No exceptions in logs

**Ready to test!** Provide the Konnect credentials and I'll help you run through all scenarios.
