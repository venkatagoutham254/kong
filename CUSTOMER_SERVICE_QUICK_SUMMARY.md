# 🚨 CUSTOMER SERVICE - URGENT CHANGES REQUIRED

## 🔴 CRITICAL ISSUE

Your current import endpoint is **MISSING the `source` field**, which is required for Apigee integration.

---

## ⚡ Quick Summary

### What Needs to Change:

1. **Add `source` field to import request**
2. **Add `source` and `externalId` columns to database**
3. **Implement GET /v1/api/customers endpoint**
4. **Update uniqueness logic**

---

## 📝 Current vs Required

### ❌ Current Import Request:
```json
{
  "businessEmail": "string",
  "firstName": "string",
  "lastName": "string",
  "companyName": "string",
  "phoneNumber": "string",
  "externalId": "string"
}
```

### ✅ Required Import Request:
```json
{
  "businessEmail": "string",
  "firstName": "string",
  "lastName": "string",
  "companyName": "string",
  "phoneNumber": "string",
  "source": "APIGEE",        // ← ADD THIS
  "externalId": "string"
}
```

---

## 🎯 Implementation Tasks

### Task 1: Add `source` Field (CRITICAL)
- Add to `CustomerImportRequest` DTO
- Add to `Customer` entity
- Update import logic to check: `externalId + source + organizationId`

### Task 2: Database Changes
```sql
ALTER TABLE customers ADD COLUMN source VARCHAR(50) DEFAULT 'MANUAL' NOT NULL;
ALTER TABLE customers ADD COLUMN external_id VARCHAR(255);
CREATE UNIQUE INDEX idx_customer_external_id_source_org 
  ON customers(external_id, source, organization_id);
```

### Task 3: Implement GET Endpoint
```java
@GetMapping("/v1/api/customers")
public ResponseEntity<List<CustomerResponse>> getCustomers(
    @RequestParam(required = false) String source
) {
    // Return customers for current organization
    // Optional filter by source
}
```

---

## 📚 Full Documentation

See **CUSTOMER_SERVICE_IMPLEMENTATION_GUIDE.md** for:
- ✅ Complete code examples
- ✅ Step-by-step implementation
- ✅ Testing guide
- ✅ Liquibase migrations
- ✅ All DTOs and entities

---

## 🧪 Test After Implementation

```bash
# Test import with source
curl -X POST http://localhost:8082/v1/api/customers/import \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: 3" \
  -d '{
    "businessEmail": "test@example.com",
    "firstName": "Test",
    "lastName": "User",
    "companyName": "Test Corp",
    "phoneNumber": "+1234567890",
    "source": "APIGEE",
    "externalId": "dev-001"
  }'

# Test GET customers
curl -X GET http://localhost:8082/v1/api/customers \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## ⏱️ Estimated Implementation Time

- **Task 1 (source field):** 15 minutes
- **Task 2 (database):** 10 minutes  
- **Task 3 (GET endpoint):** 20 minutes
- **Testing:** 15 minutes

**Total: ~1 hour**

---

## 🚀 Priority: HIGH

Without the `source` field, Apigee integration **will not work correctly**.

**Integration Service is ready and waiting!**
