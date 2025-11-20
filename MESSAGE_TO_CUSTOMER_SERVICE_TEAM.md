# Email/Message Template for Customer Service Team

---

**Subject:** URGENT: Customer Import API Changes Required for Apigee Integration

---

Hi Team,

I've reviewed the current customer import endpoint (`POST /v1/api/customers/import`) and identified some critical changes needed to support Apigee integration.

## 🔴 Critical Issue

The current import endpoint is **missing the `source` field**, which is required to:
- Track where customers come from (Apigee, Kong, Manual, etc.)
- Prevent duplicate customers from different sources
- Enable proper multi-source customer management

## 📋 Required Changes

### 1. Add `source` Field to Import Request

**Current request body:**
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

**Required request body:**
```json
{
  "businessEmail": "string",
  "firstName": "string",
  "lastName": "string",
  "companyName": "string",
  "phoneNumber": "string",
  "source": "string",        // ← NEW REQUIRED FIELD
  "externalId": "string"
}
```

### 2. Implement GET Customers Endpoint

We need a new endpoint to list customers:
```
GET /v1/api/customers
GET /v1/api/customers?source=APIGEE
```

### 3. Update Database Schema

Add two columns to `customers` table:
- `source` (VARCHAR(50), default 'MANUAL')
- `external_id` (VARCHAR(255), nullable)

## 📚 Documentation Provided

I've created complete implementation guides in the `/apigee` folder:

1. **CUSTOMER_SERVICE_IMPLEMENTATION_GUIDE.md** - Complete step-by-step guide with all code
2. **CUSTOMER_SERVICE_QUICK_SUMMARY.md** - Quick reference summary

These documents include:
- ✅ All code changes needed
- ✅ Database migration scripts (Liquibase)
- ✅ Complete DTOs and entities
- ✅ Testing examples
- ✅ Implementation checklist

## ⏱️ Estimated Time

Total implementation time: **~1 hour**
- Add source field: 15 min
- Database changes: 10 min
- GET endpoint: 20 min
- Testing: 15 min

## 🎯 Priority

**HIGH** - The Apigee Integration Service is already implemented and ready to sync customers, but it's blocked waiting for these changes.

## 🧪 Testing

Once implemented, we can test the full flow:
1. Sync customers from Apigee → Customer Service
2. List customers via GET endpoint
3. Verify customers are properly imported with source tracking

## 📁 Files Location

All documentation files are in:
```
/Users/venkatagowtham/Desktop/apigee/
- CUSTOMER_SERVICE_IMPLEMENTATION_GUIDE.md
- CUSTOMER_SERVICE_QUICK_SUMMARY.md
```

## ❓ Questions?

Please review the implementation guide and let me know if you have any questions. I'm happy to help with any clarifications.

Thanks!

---

**TL;DR:**
1. Add `source` field to import request (CRITICAL)
2. Implement GET /v1/api/customers endpoint
3. See CUSTOMER_SERVICE_IMPLEMENTATION_GUIDE.md for complete code
4. Estimated time: 1 hour
