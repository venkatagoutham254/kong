# Kong Konnect Integration - Ready to Test!

## Your Konnect Setup ✅

**Token**: `kpat_cQzpAsDs6bjGT2fo0awV9yawAaGBr1x65ifX7qyLUUSFelQdA`  
**Region**: IN (India)  
**Base URL**: `https://in.api.konghq.com`  
**Control Plane**: `aforo-dev` (ID: `154d960b-f4c3-408c-b356-95fcbed64c5b`)  
**API Products**: 3 products available  
**JWT Token**: `eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhZm9yby1jdXN0b21lcnNlcnZpY2UiLCJzdWIiOiJubkBhZm9yby5haSIsIm9yZ0lkIjoyNywic3RhdHVzIjoiQUNUSVZFIiwiaWF0IjoxNzY3MTc3OTkyLCJleHAiOjE3Njc3ODI3OTJ9.-Cg5fNlwU8HWGXwlh704eNlJvoa0oZeSVQstG6t4J64`  
**Organization ID**: 27

---

## Step 1: Generate Encryption Key

Run this command:
```bash
openssl rand -base64 32
```

**Example output:**
```
xK8pL2mN9qR3sT5uVwXyZ1aB2cD3eF4gH5iJ6kL7mN8o=
```

**Copy the output** - you'll need it in the next step.

---

## Step 2: Update application.properties

Open: `/Users/venkatagowtham/Desktop/Kong/kong/src/main/resources/application.properties`

Add this line (replace with your generated key):
```properties
encryption.secret.key=xK8pL2mN9qR3sT5uVwXyZ1aB2cD3eF4gH5iJ6kL7mN8o=
```

**Save the file.**

---

## Step 3: Start the Application

```bash
cd /Users/venkatagowtham/Desktop/Kong/kong
./mvnw spring-boot:run
```

**Wait for:**
- `Started KongApplication in X seconds`
- No errors about encryption key
- Scheduler started: `KonnectAutoRefreshScheduler`

**Keep this terminal running!**

---

## Step 4: Run Tests (New Terminal)

Open a **new terminal** and run these tests:

### Test 1: Create Connection ✅

```bash
curl -X POST "http://localhost:8080/api/integrations/konnect/connection" \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: 27" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhZm9yby1jdXN0b21lcnNlcnZpY2UiLCJzdWIiOiJubkBhZm9yby5haSIsIm9yZ0lkIjoyNywic3RhdHVzIjoiQUNUSVZFIiwiaWF0IjoxNzY3MTc3OTkyLCJleHAiOjE3Njc3ODI3OTJ9.-Cg5fNlwU8HWGXwlh704eNlJvoa0oZeSVQstG6t4J64" \
  -d '{
    "name": "Konnect India - aforo-dev",
    "description": "Kong Konnect India region for aforo-dev control plane",
    "baseUrl": "https://in.api.konghq.com",
    "authToken": "kpat_cQzpAsDs6bjGT2fo0awV9yawAaGBr1x65ifX7qyLUUSFelQdA",
    "controlPlaneId": "154d960b-f4c3-408c-b356-95fcbed64c5b",
    "region": "in"
  }' | jq
```

**Expected Response:**
```json
{
  "connectionId": 1,
  "status": "connected",
  "controlPlaneId": "154d960b-f4c3-408c-b356-95fcbed64c5b",
  "message": "Connection successful"
}
```

**✅ Check:** `status` should be `"connected"`

---

### Test 2: Test Connection ✅

```bash
curl -X GET "http://localhost:8080/api/integrations/konnect/connection/test" \
  -H "X-Organization-Id: 27" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhZm9yby1jdXN0b21lcnNlcnZpY2UiLCJzdWIiOiJubkBhZm9yby5haSIsIm9yZ0lkIjoyNywic3RhdHVzIjoiQUNUSVZFIiwiaWF0IjoxNzY3MTc3OTkyLCJleHAiOjE3Njc3ODI3OTJ9.-Cg5fNlwU8HWGXwlh704eNlJvoa0oZeSVQstG6t4J64" | jq
```

**Expected Response:**
```json
{
  "ok": true,
  "message": "Connected successfully",
  "counts": {
    "apiProducts": 3
  }
}
```

**✅ Check:** `ok` is `true` and `apiProducts` is `3`

---

### Test 3: Fetch API Products ✅

```bash
curl -X GET "http://localhost:8080/api/integrations/konnect/api-products" \
  -H "X-Organization-Id: 27" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhZm9yby1jdXN0b21lcnNlcnZpY2UiLCJzdWIiOiJubkBhZm9yby5haSIsIm9yZ0lkIjoyNywic3RhdHVzIjoiQUNUSVZFIiwiaWF0IjoxNzY3MTc3OTkyLCJleHAiOjE3Njc3ODI3OTJ9.-Cg5fNlwU8HWGXwlh704eNlJvoa0oZeSVQstG6t4J64" | jq
```

**Expected Response:**
```json
[
  {
    "konnectApiProductId": "91ed26d3-b9eb-41cb-8883-3ff1e55bfebe",
    "name": "kong testing",
    "description": "testing",
    "status": "...",
    "versionsCount": 0,
    "updatedAt": "..."
  },
  {
    "konnectApiProductId": "59fa07e6-f5a1-4d55-bc37-d4f36502c32e",
    "name": "prd testing 2",
    "description": "prd 2",
    "status": "...",
    "versionsCount": 0,
    "updatedAt": "..."
  },
  {
    "konnectApiProductId": "98cd843a-2baa-46ee-b055-5b3b4f95fc80",
    "name": "prd testing 3",
    "description": "testing 3",
    "status": "...",
    "versionsCount": 0,
    "updatedAt": "..."
  }
]
```

**✅ Check:** Should return 3 products

---

### Test 4: Import API Products ✅

```bash
curl -X POST "http://localhost:8080/api/integrations/konnect/api-products/import" \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: 27" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhZm9yby1jdXN0b21lcnNlcnZpY2UiLCJzdWIiOiJubkBhZm9yby5haSIsIm9yZ0lkIjoyNywic3RhdHVzIjoiQUNUSVZFIiwiaWF0IjoxNzY3MTc3OTkyLCJleHAiOjE3Njc3ODI3OTJ9.-Cg5fNlwU8HWGXwlh704eNlJvoa0oZeSVQstG6t4J64" \
  -d '{
    "selectedApiProductIds": [
      "91ed26d3-b9eb-41cb-8883-3ff1e55bfebe",
      "59fa07e6-f5a1-4d55-bc37-d4f36502c32e"
    ]
  }' | jq
```

**Expected Response:**
```json
{
  "imported": 2,
  "updated": 0,
  "failed": 0,
  "items": [
    {
      "konnectApiProductId": "91ed26d3-b9eb-41cb-8883-3ff1e55bfebe",
      "aforoProductId": 1,
      "action": "CREATED"
    },
    {
      "konnectApiProductId": "59fa07e6-f5a1-4d55-bc37-d4f36502c32e",
      "aforoProductId": 2,
      "action": "CREATED"
    }
  ]
}
```

**✅ Check:** `imported` is `2`, `failed` is `0`, action is `"CREATED"`

---

### Test 5: List Imported Products ✅

```bash
curl -X GET "http://localhost:8080/api/integrations/konnect/api-products/imported" \
  -H "X-Organization-Id: 27" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhZm9yby1jdXN0b21lcnNlcnZpY2UiLCJzdWIiOiJubkBhZm9yby5haSIsIm9yZ0lkIjoyNywic3RhdHVzIjoiQUNUSVZFIiwiaWF0IjoxNzY3MTc3OTkyLCJleHAiOjE3Njc3ODI3OTJ9.-Cg5fNlwU8HWGXwlh704eNlJvoa0oZeSVQstG6t4J64" | jq
```

**Expected Response:**
```json
[
  {
    "aforoProductId": 1,
    "name": "kong testing",
    "description": "testing",
    "konnectApiProductId": "91ed26d3-b9eb-41cb-8883-3ff1e55bfebe",
    "lastSeenAt": "2026-01-02T..."
  },
  {
    "aforoProductId": 2,
    "name": "prd testing 2",
    "description": "prd 2",
    "konnectApiProductId": "59fa07e6-f5a1-4d55-bc37-d4f36502c32e",
    "lastSeenAt": "2026-01-02T..."
  }
]
```

**✅ Check:** Should show 2 imported products

---

### Test 6: Re-import Same Product (Idempotency Test) ✅

```bash
curl -X POST "http://localhost:8080/api/integrations/konnect/api-products/import" \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: 27" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhZm9yby1jdXN0b21lcnNlcnZpY2UiLCJzdWIiOiJubkBhZm9yby5haSIsIm9yZ0lkIjoyNywic3RhdHVzIjoiQUNUSVZFIiwiaWF0IjoxNzY3MTc3OTkyLCJleHAiOjE3Njc3ODI3OTJ9.-Cg5fNlwU8HWGXwlh704eNlJvoa0oZeSVQstG6t4J64" \
  -d '{
    "selectedApiProductIds": [
      "91ed26d3-b9eb-41cb-8883-3ff1e55bfebe"
    ]
  }' | jq
```

**Expected Response:**
```json
{
  "imported": 0,
  "updated": 1,
  "failed": 0,
  "items": [
    {
      "konnectApiProductId": "91ed26d3-b9eb-41cb-8883-3ff1e55bfebe",
      "aforoProductId": 1,
      "action": "UPDATED"
    }
  ]
}
```

**✅ Check:** `updated` is `1`, action is `"UPDATED"`, same `aforoProductId`

---

### Test 7: Preview Sync ✅

```bash
curl -X POST "http://localhost:8080/api/integrations/konnect/catalog/preview" \
  -H "X-Organization-Id: 27" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhZm9yby1jdXN0b21lcnNlcnZpY2UiLCJzdWIiOiJubkBhZm9yby5haSIsIm9yZ0lkIjoyNywic3RhdHVzIjoiQUNUSVZFIiwiaWF0IjoxNzY3MTc3OTkyLCJleHAiOjE3Njc3ODI3OTJ9.-Cg5fNlwU8HWGXwlh704eNlJvoa0oZeSVQstG6t4J64" | jq
```

**Expected Response:**
```json
{
  "added": [
    {
      "konnectApiProductId": "98cd843a-2baa-46ee-b055-5b3b4f95fc80",
      "name": "prd testing 3",
      "description": "testing 3",
      "status": "...",
      "versionsCount": 0,
      "updatedAt": "..."
    }
  ],
  "removed": [],
  "changed": []
}
```

**✅ Check:** `added` should show "prd testing 3" (not imported yet)

---

### Test 8: Apply Sync ✅

```bash
curl -X POST "http://localhost:8080/api/integrations/konnect/catalog/apply" \
  -H "X-Organization-Id: 27" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhZm9yby1jdXN0b21lcnNlcnZpY2UiLCJzdWIiOiJubkBhZm9yby5haSIsIm9yZ0lkIjoyNywic3RhdHVzIjoiQUNUSVZFIiwiaWF0IjoxNzY3MTc3OTkyLCJleHAiOjE3Njc3ODI3OTJ9.-Cg5fNlwU8HWGXwlh704eNlJvoa0oZeSVQstG6t4J64" | jq
```

**Expected Response:**
```json
{
  "imported": 1,
  "updated": 0,
  "failed": 0,
  "items": [
    {
      "konnectApiProductId": "98cd843a-2baa-46ee-b055-5b3b4f95fc80",
      "aforoProductId": 3,
      "action": "CREATED"
    }
  ]
}
```

**✅ Check:** Third product now imported

---

### Test 9: Verify Database (Optional)

```bash
# Connect to your database
psql -U <username> -d <database_name>

# Check encrypted token
SELECT id, name, connection_status, 
       LEFT(auth_token, 10) as token_prefix 
FROM client_api_details 
WHERE organization_id = 27 AND environment = 'konnect';

# Should show: token_prefix starts with 'v1:'

# Check imported products
SELECT internal_id, name, source, organization_id 
FROM kong_product 
WHERE organization_id = 27 AND source = 'konnect';

# Should show: 3 products

# Check mapping table
SELECT id, konnect_api_product_id, aforo_product_id, status 
FROM konnect_api_product_map 
WHERE org_id = 27;

# Should show: 3 mappings with status 'ACTIVE'
```

---

### Test 10: Check Auto-Refresh Scheduler ✅

**Wait 2-3 minutes**, then check application logs:

Look for:
```
Starting Konnect auto-refresh job
Completed Konnect auto-refresh job
```

**✅ Check:** Scheduler runs every 120 seconds without errors

---

## Test Results Checklist

- [ ] Connection created successfully (`status: "connected"`)
- [ ] Test connection returns `ok: true`
- [ ] Fetch products returns 3 products
- [ ] Import 2 products succeeds (`imported: 2`)
- [ ] List imported shows 2 products
- [ ] Re-import updates (not duplicates) (`updated: 1`)
- [ ] Preview sync detects 1 added product
- [ ] Apply sync imports the third product
- [ ] Database shows token starts with `v1:`
- [ ] Auto-refresh scheduler runs every 2 minutes

---

## If Tests Fail

### Connection fails
- Check encryption key is set in application.properties
- Verify token is correct
- Check region is `in` (India)

### Import fails
- Check organization_id is 27
- Verify JWT token is valid
- Check database connection

### Scheduler not running
- Check logs for `@EnableScheduling`
- Verify `KonnectAutoRefreshScheduler` bean created

---

## Success! 🎉

If all tests pass:
- ✅ Encryption working (AES-GCM)
- ✅ Connection management working
- ✅ Import/sync working
- ✅ Idempotency working
- ✅ Locking working
- ✅ Scheduler working

**Ready for deployment!**
