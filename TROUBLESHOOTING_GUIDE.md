# 🔧 Customer Sync Troubleshooting Guide

## Issue 1: ✅ FIXED - Wrong Endpoint Path

**Error:**
```
Failed to import customer: 401 : [no body]
```

**Cause:** Integration Service was calling `/api/customers/import` instead of `/v1/api/customers/import`

**Fix:** Updated endpoint path to `/v1/api/customers/import`

**Status:** ✅ FIXED

---

## Issue 2: ⚠️ CURRENT - Foreign Key Constraint Violation

**Error:**
```
ERROR: insert or update on table "aforo_customers" violates foreign key constraint "fk_aforo_customers_organization"
Detail: Key (organization_id)=(3) is not present in table "aforo_organizations"
```

**Cause:** The JWT token contains `orgId: 3`, but organization ID 3 doesn't exist in the Customer Service database.

**Solutions:**

### Option 1: Create Organization ID 3 (Recommended)

Run this SQL in Customer Service database:

```sql
-- Check existing organizations
SELECT id, organization_name FROM aforo_organizations;

-- If organization 3 doesn't exist, create it
INSERT INTO aforo_organizations (id, organization_name, status, created_on)
VALUES (3, 'Test Organization', 'ACTIVE', NOW());
```

### Option 2: Use Existing Organization ID

If you have an existing organization (e.g., ID 1), you can:

1. **Get a new JWT token** with the correct orgId:
   ```bash
   curl -X POST http://localhost:8082/api/login \
     -H "Content-Type: application/json" \
     -d '{
       "businessEmail": "user@example.com",
       "password": "password"
     }'
   ```

2. **Use that token** for the sync request

### Option 3: Update JWT Token Generation

If you control the JWT generation, update it to use an existing organization ID.

---

## Testing After Fix

Once organization ID 3 exists, test again:

```bash
curl -X POST http://localhost:8086/api/integrations/apigee/customers/sync \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected Success Response:**
```json
{
  "message": "Customer sync completed",
  "totalFetched": 1,
  "totalCreated": 1,
  "totalUpdated": 0,
  "totalFailed": 0,
  "errors": []
}
```

---

## Verification Steps

After successful sync:

1. **Check Customer Service database:**
   ```sql
   SELECT id, primary_email, customer_name, source, external_id, organization_id
   FROM aforo_customers
   WHERE source = 'APIGEE';
   ```

2. **Verify customer was imported:**
   - Should see customer with email: `aforo@aforo.ai`
   - Source should be: `APIGEE`
   - External ID should match Apigee developer ID
   - Organization ID should be: `3`

---

## Summary of Fixes Applied

1. ✅ Fixed endpoint path: `/api/customers/import` → `/v1/api/customers/import`
2. ⚠️ **TODO:** Create organization ID 3 in database OR use existing organization

---

## Quick Commands

### Check Organizations
```sql
SELECT * FROM aforo_organizations;
```

### Create Organization 3
```sql
INSERT INTO aforo_organizations (id, organization_name, status, created_on)
VALUES (3, 'Aforo Test Org', 'ACTIVE', NOW());
```

### Check Imported Customers
```sql
SELECT * FROM aforo_customers WHERE source = 'APIGEE';
```

### Delete Test Customer (if needed)
```sql
DELETE FROM aforo_customers WHERE source = 'APIGEE' AND external_id = 'dev-001';
```
