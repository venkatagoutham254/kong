# Real-Time Kong Testing Guide

## Your Kong Details
- **Control Plane ID**: `ff0fd1c6-c4fa-45df-9783-5003ae35399e`
- **Region**: India (IN)
- **Admin API URL**: `https://in.api.konghq.com/v2/control-planes/ff0fd1c6-c4fa-45df-9783-5003ae35399e`
- **Service ID**: `c7a871df-8bff-4977-ac03-f658b1faba7f`
- **Service Name**: `test-payment-api`
- **Route ID**: `2bce6541-7954-4396-9166-1072c8e4ba2a`
- **Consumer ID**: `455be6b1-0513-4460-bc39-1ec396b6faf8`
- **Consumer Username**: `test-user-mm`

---

## Test 1: Connect to Kong ✅

**Endpoint**: `POST /integrations/kong/connect`

**Purpose**: Establish connection to your real Kong Konnect account

**Swagger Steps**:
1. Open: http://localhost:8086/swagger-ui.html
2. Click "Authorize" → Paste JWT token
3. Find: `POST /integrations/kong/connect`
4. Click "Try it out"
5. Paste this JSON:

```json
{
  "environment": "konnect",
  "adminApiUrl": "https://in.api.konghq.com/v2/control-planes/ff0fd1c6-c4fa-45df-9783-5003ae35399e",
  "workspace": "default",
  "token": "kpat_eCxEVikaJDHKkjzD4fiUcdLGFTS4AebaYUyjg9p168gfGDgjA",
  "scope": {
    "workspaces": ["default"],
    "services": []
  },
  "autoInstall": {
    "correlationId": true,
    "httpLog": true,
    "rateLimitingAdvanced": true
  },
  "eventHooks": {
    "crud": true,
    "exceed": true
  }
}
```

**Expected Response**: HTTP 200
```json
{
  "connectionId": "1",
  "status": "connected",
  "message": "Successfully connected to Kong",
  "webhookUrl": "http://localhost:8080/integrations/kong/events",
  "ingestUrl": "http://localhost:8080/integrations/kong/ingest"
}
```

---

## Test 2: Usage Ingestion (Single Event) ✅

**Endpoint**: `POST /integrations/kong/ingest`

**Purpose**: Send a usage event for your real service

**Swagger Steps**:
1. Find: `POST /integrations/kong/ingest`
2. Click "Try it out"
3. Paste this JSON (using YOUR real IDs):

```json
{
  "kong_request_id": "real-test-001",
  "timestamp": "2025-12-04T07:30:00Z",
  "service": {
    "id": "c7a871df-8bff-4977-ac03-f658b1faba7f",
    "name": "test-payment-api"
  },
  "route": {
    "id": "2bce6541-7954-4396-9166-1072c8e4ba2a",
    "paths": ["/test/payments"]
  },
  "consumer": {
    "id": "455be6b1-0513-4460-bc39-1ec396b6faf8",
    "username": "test-user-mm"
  },
  "request": {
    "method": "POST",
    "path": "/test/payments/charge",
    "size": 512
  },
  "response": {
    "status": 200,
    "size": 1024
  }
}
```

**Expected Response**: HTTP 202 Accepted

---

## Test 3: Usage Ingestion (Batch) ✅

**Endpoint**: `POST /integrations/kong/ingest/batch`

**Purpose**: Send multiple usage events at once

**Swagger Steps**:
1. Find: `POST /integrations/kong/ingest/batch`
2. Click "Try it out"
3. Paste this JSON:

```json
[
  {
    "kong_request_id": "batch-real-001",
    "timestamp": "2025-12-04T07:30:00Z",
    "service": {
      "id": "c7a871df-8bff-4977-ac03-f658b1faba7f",
      "name": "test-payment-api"
    },
    "route": {
      "id": "2bce6541-7954-4396-9166-1072c8e4ba2a",
      "paths": ["/test/payments"]
    },
    "consumer": {
      "id": "455be6b1-0513-4460-bc39-1ec396b6faf8",
      "username": "test-user-mm"
    },
    "request": {
      "method": "GET",
      "path": "/test/payments/status",
      "size": 128
    },
    "response": {
      "status": 200,
      "size": 256
    }
  },
  {
    "kong_request_id": "batch-real-002",
    "timestamp": "2025-12-04T07:30:01Z",
    "service": {
      "id": "c7a871df-8bff-4977-ac03-f658b1faba7f",
      "name": "test-payment-api"
    },
    "route": {
      "id": "2bce6541-7954-4396-9166-1072c8e4ba2a",
      "paths": ["/test/payments"]
    },
    "consumer": {
      "id": "455be6b1-0513-4460-bc39-1ec396b6faf8",
      "username": "test-user-mm"
    },
    "request": {
      "method": "POST",
      "path": "/test/payments/refund",
      "size": 256
    },
    "response": {
      "status": 201,
      "size": 512
    }
  }
]
```

**Expected Response**: HTTP 202 Accepted

---

## Test 4: Event Hooks ✅

**Endpoint**: `POST /integrations/kong/events`

**Purpose**: Simulate Kong sending a webhook when service is created

**Swagger Steps**:
1. Find: `POST /integrations/kong/events`
2. Click "Try it out"
3. Paste this JSON:

```json
{
  "source": "crud",
  "event": "services:create",
  "entity": "services",
  "data": {
    "id": "c7a871df-8bff-4977-ac03-f658b1faba7f",
    "name": "test-payment-api",
    "protocol": "https",
    "host": "httpbin.org",
    "port": 443
  },
  "timestamp": "2025-12-04T07:30:00Z"
}
```

**Expected Response**: HTTP 202 Accepted

---

## Test 5: Enforce Rate Limits ✅

**Endpoint**: `POST /integrations/kong/enforce/groups`

**Purpose**: Create pricing plans and push rate limits to Kong

**Swagger Steps**:
1. Find: `POST /integrations/kong/enforce/groups`
2. Click "Try it out"
3. Paste this JSON:

```json
{
  "mappings": [
    {
      "planId": "bronze-plan",
      "consumerGroupName": "bronze-tier",
      "limits": [
        {"window": "day", "limit": 1000},
        {"window": "hour", "limit": 100}
      ]
    },
    {
      "planId": "silver-plan",
      "consumerGroupName": "silver-tier",
      "limits": [
        {"window": "day", "limit": 10000},
        {"window": "hour", "limit": 1000}
      ]
    },
    {
      "planId": "gold-plan",
      "consumerGroupName": "gold-tier",
      "limits": [
        {"window": "day", "limit": 100000},
        {"window": "hour", "limit": 10000}
      ]
    }
  ]
}
```

**Expected Response**: HTTP 200 OK

---

## Test 6: Suspend Consumer ✅

**Endpoint**: `POST /integrations/kong/suspend`

**Purpose**: Suspend your test consumer (prepaid zero balance)

**Swagger Steps**:
1. Find: `POST /integrations/kong/suspend`
2. Click "Try it out"
3. Paste this JSON (using YOUR real consumer ID):

```json
{
  "consumerId": "455be6b1-0513-4460-bc39-1ec396b6faf8",
  "mode": "group",
  "reason": "Prepaid wallet balance is zero"
}
```

**Expected Response**: HTTP 202 Accepted or HTTP 404 (if consumer not synced yet)

---

## Test 7: Resume Consumer ✅

**Endpoint**: `POST /integrations/kong/resume/{consumerId}`

**Purpose**: Resume your test consumer after top-up

**Swagger Steps**:
1. Find: `POST /integrations/kong/resume/{consumerId}`
2. Click "Try it out"
3. Enter consumer ID: `455be6b1-0513-4460-bc39-1ec396b6faf8`
4. Click "Execute"

**Expected Response**: HTTP 202 Accepted or HTTP 404 (if consumer not synced yet)

---

## Test 8: Catalog Sync ✅

**Endpoint**: `POST /integrations/kong/catalog/sync`

**Purpose**: Sync your real Kong services, routes, consumers to Aforo

**Swagger Steps**:
1. First, connect to Kong (Test 1)
2. Note the `connectionId` from response
3. Find: `POST /integrations/kong/catalog/sync`
4. Click "Try it out"
5. Enter `clientDetailsId`: (use the connectionId from Test 1)
6. Click "Execute"

**Expected Response**: HTTP 202 Accepted
```json
{
  "status": "COMPLETED",
  "services": {
    "fetched": 1,
    "created": 1
  },
  "routes": {
    "fetched": 1,
    "created": 1
  },
  "consumers": {
    "fetched": 1,
    "created": 1
  }
}
```

---

## Test 9: Health Check ✅

**Endpoint**: `GET /integrations/kong/health`

**Purpose**: Check Kong integration health

**Swagger Steps**:
1. Find: `GET /integrations/kong/health`
2. Click "Try it out"
3. Click "Execute"

**Expected Response**: HTTP 200
```json
{
  "kongReachable": true,
  "status": "healthy"
}
```

---

## Test 10: Security Test ✅

**Purpose**: Verify multi-tenant security

**Swagger Steps**:
1. Click "Authorize" button
2. Clear the JWT token
3. Click "Authorize" (empty)
4. Try any endpoint (e.g., health check)

**Expected Response**: HTTP 401 Unauthorized

---

## Summary Checklist

Test each one in Swagger and check off:

- [ ] Test 1: Connect to Kong
- [ ] Test 2: Usage Ingestion (Single)
- [ ] Test 3: Usage Ingestion (Batch)
- [ ] Test 4: Event Hooks
- [ ] Test 5: Enforce Rate Limits
- [ ] Test 6: Suspend Consumer
- [ ] Test 7: Resume Consumer
- [ ] Test 8: Catalog Sync
- [ ] Test 9: Health Check
- [ ] Test 10: Security Test

---

## Your JWT Token

```
eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhZm9yby1jdXN0b21lcnNlcnZpY2UiLCJzdWIiOiJtbUBhZm9yby5haSIsIm9yZ0lkIjoxOCwic3RhdHVzIjoiQUNUSVZFIiwiaWF0IjoxNzY0ODI1MTQ0LCJleHAiOjE3NjU0Mjk5NDR9.e1EPDjLKsQfctzs4dT6D9MdYZQRVojGbxUdkUAahsn8
```

---

## Swagger UI URL

```
http://localhost:8086/swagger-ui.html
```

---

**All tests use YOUR real Kong data! 🎯**
