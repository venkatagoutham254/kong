# Kong Integration Service - API Documentation

## Overview

The Kong Integration Service is a comprehensive Spring Boot microservice that provides seamless integration with Kong Gateway and Kong Konnect. It enables API catalog synchronization, usage event ingestion, rate limit enforcement, consumer management, and analytics for usage-based billing systems.

**Base URL:** `http://localhost:8086`  
**API Version:** v1  
**Content-Type:** `application/json`

## Table of Contents

1. [Authentication](#authentication)
2. [Kong Product Management](#kong-product-management)
3. [Kong Integration](#kong-integration)
4. [Analytics & Reporting](#analytics--reporting)
5. [Client API Details Management](#client-api-details-management)
6. [Data Models](#data-models)
7. [Error Handling](#error-handling)
8. [Integration Patterns](#integration-patterns)

---

## Authentication

The service uses JWT-based authentication with organization-based multi-tenancy:

```
Authorization: Bearer <jwt-token>
X-Organization-Id: <organization-id>
```

---

## Kong Product Management

### Fetch Products from External API
```http
GET /api/kong/fetch
```

**Description:** Fetches Kong products from external API and saves them for the current organization.

**Headers:**
```
Authorization: Bearer <jwt-token>
X-Organization-Id: <organization-id>
```

**Response (200 OK):**
```json
{
  "products": [
    {
      "id": "service-123",
      "name": "Payment API",
      "description": "Payment processing service",
      "host": "payment.api.com",
      "port": 443,
      "protocol": "https"
    }
  ],
  "count": 1,
  "syncedAt": "2024-11-04T16:30:00Z"
}
```

### Fetch Products Using Stored Client Details
```http
GET /api/kong/fetch/from-db/{clientDetailsId}
```

**Description:** Fetches Kong products using stored client API details.

**Path Parameters:**
- `clientDetailsId` (Long): Client API details ID

**Response (200 OK):**
```json
[
  {
    "products": [
      {
        "id": "service-123",
        "name": "Payment API",
        "description": "Payment processing service"
      }
    ],
    "count": 1,
    "syncedAt": "2024-11-04T16:30:00Z"
  }
]
```

### Get All Products
```http
GET /api/kong
```

**Description:** Returns all Kong products for the current organization.

**Response (200 OK):**
```json
[
  {
    "id": "service-123",
    "name": "Payment API",
    "description": "Payment processing service",
    "host": "payment.api.com",
    "port": 443,
    "protocol": "https",
    "organizationId": 123,
    "createdAt": "2024-11-04T16:00:00Z"
  }
]
```

### Get Product by ID
```http
GET /api/kong/{id}
```

**Description:** Returns a specific Kong product by ID.

**Path Parameters:**
- `id` (String): Product ID

**Response (200 OK):**
```json
{
  "id": "service-123",
  "name": "Payment API",
  "description": "Payment processing service",
  "host": "payment.api.com",
  "port": 443,
  "protocol": "https",
  "organizationId": 123,
  "createdAt": "2024-11-04T16:00:00Z"
}
```

### Delete Product
```http
DELETE /api/kong/{id}
```

**Description:** Deletes a Kong product by ID.

**Path Parameters:**
- `id` (String): Product ID

**Response (204 No Content)**

---

## Kong Integration

### Connect to Kong Gateway/Konnect
```http
POST /integrations/kong/connect
```

**Description:** Establishes connection to Kong Gateway or Konnect and performs initial catalog sync.

**Request Body:**
```json
{
  "environment": "konnect",
  "adminApiUrl": "https://api.konghq.com",
  "workspace": "default",
  "token": "kpat_xxxxxxxxxxx",
  "scope": {
    "workspaces": ["default"],
    "services": ["payment-api", "user-api"]
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

**Response (200 OK):**
```json
{
  "status": "connected",
  "message": "Successfully connected to Kong Konnect",
  "connectionId": "conn-123",
  "servicesImported": 5,
  "routesImported": 12,
  "consumersImported": 3,
  "pluginsInstalled": ["correlation-id", "http-log", "rate-limiting-advanced"],
  "hooksConfigured": ["crud", "exceed"]
}
```

### Sync Catalog
```http
POST /integrations/kong/catalog/sync?clientDetailsId={clientDetailsId}
```

**Description:** Triggers on-demand synchronization of Kong services, routes, and consumers.

**Query Parameters:**
- `clientDetailsId` (Long): Client API details ID

**Response (202 Accepted):**
```json
{
  "syncId": "sync-456",
  "status": "initiated",
  "message": "Catalog sync initiated",
  "estimatedDuration": "2-5 minutes"
}
```

### Ingest Usage Events
```http
POST /integrations/kong/ingest
```

**Description:** Receives HTTP Log events from Kong (single or batched).

**Request Body (Single Event):**
```json
{
  "kong_request_id": "req-123",
  "correlation_id": "corr-456",
  "timestamp": "2024-11-04T16:30:00Z",
  "service": {
    "id": "service-123",
    "name": "payment-api",
    "host": "payment.internal",
    "port": 8080,
    "protocol": "http"
  },
  "route": {
    "id": "route-456",
    "name": "payment-route",
    "paths": ["/api/v1/payments"],
    "methods": ["POST", "GET"]
  },
  "consumer": {
    "id": "consumer-789",
    "username": "api-client-1",
    "custom_id": "client-123"
  },
  "request": {
    "method": "POST",
    "path": "/api/v1/payments",
    "size": 1024,
    "headers": {
      "content-type": ["application/json"],
      "authorization": ["Bearer xxx"]
    }
  },
  "response": {
    "status": 200,
    "size": 512
  },
  "latencies": {
    "request": 150,
    "kong": 5,
    "proxy": 145
  }
}
```

**Request Body (Batch Events):**
```json
[
  {
    "kong_request_id": "req-123",
    "timestamp": "2024-11-04T16:30:00Z",
    "service": {...},
    "route": {...},
    "consumer": {...}
  },
  {
    "kong_request_id": "req-124",
    "timestamp": "2024-11-04T16:30:01Z",
    "service": {...},
    "route": {...},
    "consumer": {...}
  }
]
```

**Response (202 Accepted)**

### Process Event Hooks
```http
POST /integrations/kong/events
```

**Description:** Receives Kong Event Hooks for CRUD operations and rate limit exceeded events.

**Request Body:**
```json
{
  "source": "kong",
  "event": "crud",
  "entity": "service",
  "operation": "create",
  "data": {
    "id": "service-new",
    "name": "new-api",
    "host": "new.api.com"
  },
  "timestamp": "2024-11-04T16:30:00Z"
}
```

**Response (202 Accepted)**

### Enforce Rate Limits
```http
POST /integrations/kong/enforce/groups
```

**Description:** Maps pricing plans to consumer groups and pushes rate limit configurations to Kong.

**Request Body:**
```json
{
  "mappings": [
    {
      "planId": "plan-basic",
      "consumerGroupName": "basic-tier",
      "limits": [
        {
          "window": "minute",
          "limit": 100,
          "identifier": "consumer"
        },
        {
          "window": "hour",
          "limit": 1000,
          "identifier": "consumer"
        }
      ]
    },
    {
      "planId": "plan-premium",
      "consumerGroupName": "premium-tier",
      "limits": [
        {
          "window": "minute",
          "limit": 1000,
          "identifier": "consumer"
        }
      ]
    }
  ]
}
```

**Response (200 OK)**

### Suspend Consumer
```http
POST /integrations/kong/suspend
```

**Description:** Suspends a consumer by moving to suspended group or adding request-termination plugin.

**Request Body:**
```json
{
  "consumerId": "consumer-789",
  "mode": "group",
  "reason": "Payment overdue",
  "suspendedGroupName": "suspended"
}
```

**Response (200 OK)**

### Resume Consumer
```http
POST /integrations/kong/resume/{consumerId}
```

**Description:** Resumes a suspended consumer by restoring their original group and removing termination plugins.

**Path Parameters:**
- `consumerId` (String): Consumer ID

**Response (200 OK)**

### Health Check
```http
GET /integrations/kong/health
```

**Description:** Returns connectivity and hook status for Kong integration.

**Response (200 OK):**
```json
{
  "kongReachable": true,
  "status": "healthy",
  "activeHooks": ["crud", "exceed"],
  "lastSync": "2024-11-04T16:25:00Z"
}
```

---

## Analytics & Reporting

### Get Billing Summary
```http
GET /api/kong/analytics/billing/{consumerId}?startTime={startTime}&endTime={endTime}
```

**Description:** Returns billing summary for a consumer in the specified time period.

**Path Parameters:**
- `consumerId` (String): Consumer ID

**Query Parameters:**
- `startTime` (ISO-8601, optional): Start time (default: 30 days ago)
- `endTime` (ISO-8601, optional): End time (default: now)

**Response (200 OK):**
```json
{
  "consumerId": "consumer-789",
  "period": {
    "startTime": "2024-10-05T16:30:00Z",
    "endTime": "2024-11-04T16:30:00Z"
  },
  "totalRequests": 15000,
  "totalCost": 150.00,
  "currency": "USD",
  "breakdown": {
    "payment-api": {
      "requests": 10000,
      "cost": 100.00
    },
    "user-api": {
      "requests": 5000,
      "cost": 50.00
    }
  }
}
```

### Get Usage Statistics
```http
GET /api/kong/analytics/usage/{consumerId}?startTime={startTime}&endTime={endTime}
```

**Description:** Returns detailed usage statistics for a consumer.

**Response (200 OK):**
```json
{
  "consumerId": "consumer-789",
  "period": {
    "startTime": "2024-10-28T16:30:00Z",
    "endTime": "2024-11-04T16:30:00Z"
  },
  "totalRequests": 5000,
  "successfulRequests": 4850,
  "errorRequests": 150,
  "averageLatency": 145,
  "peakRps": 50,
  "dailyBreakdown": [
    {
      "date": "2024-11-04",
      "requests": 1000,
      "errors": 20,
      "avgLatency": 140
    }
  ]
}
```

### Get Top Consumers
```http
GET /api/kong/analytics/top-consumers?limit={limit}&startTime={startTime}&endTime={endTime}
```

**Description:** Returns top consumers by API usage.

**Query Parameters:**
- `limit` (Integer, default: 10): Number of results
- `startTime` (ISO-8601, optional): Start time
- `endTime` (ISO-8601, optional): End time

**Response (200 OK):**
```json
[
  {
    "consumerId": "consumer-789",
    "username": "api-client-1",
    "totalRequests": 15000,
    "totalCost": 150.00,
    "rank": 1
  },
  {
    "consumerId": "consumer-456",
    "username": "api-client-2",
    "totalRequests": 12000,
    "totalCost": 120.00,
    "rank": 2
  }
]
```

### Get Top Services
```http
GET /api/kong/analytics/top-services?limit={limit}&startTime={startTime}&endTime={endTime}
```

**Description:** Returns top services by API usage.

**Response (200 OK):**
```json
[
  {
    "serviceId": "service-123",
    "serviceName": "payment-api",
    "totalRequests": 25000,
    "uniqueConsumers": 50,
    "averageLatency": 145,
    "rank": 1
  }
]
```

### Check Quota Status
```http
GET /api/kong/analytics/quota-check/{consumerId}?threshold={threshold}
```

**Description:** Checks if a consumer is approaching their quota limits.

**Path Parameters:**
- `consumerId` (String): Consumer ID

**Query Parameters:**
- `threshold` (Double, default: 80): Threshold percentage

**Response (200 OK):**
```json
{
  "consumerId": "consumer-789",
  "threshold": 80.0,
  "approachingQuota": true
}
```

### Top Up Wallet
```http
POST /api/kong/analytics/wallet/topup
```

**Description:** Adds credits to a consumer's prepaid wallet.

**Request Body:**
```json
{
  "consumerId": "consumer-789",
  "amount": 100.00
}
```

**Response (200 OK):**
```json
{
  "consumerId": "consumer-789",
  "amount": 100.00,
  "status": "success"
}
```

---

## Client API Details Management

### Create Client API Details
```http
POST /api/client-api-details
```

**Description:** Create new client API details for Kong integration.

**Request Body:**
```json
{
  "name": "Production Kong",
  "description": "Production Kong Gateway",
  "baseUrl": "https://admin.kong.prod.com",
  "endpoint": "/admin-api",
  "authToken": "kong-admin-token-123"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "Production Kong",
  "description": "Production Kong Gateway",
  "baseUrl": "https://admin.kong.prod.com",
  "endpoint": "/admin-api",
  "authToken": "kong-admin-token-123",
  "organizationId": 123,
  "createdAt": "2024-11-04T16:30:00Z"
}
```

### Get All Client API Details
```http
GET /api/client-api-details
```

**Description:** Retrieve all client API details for the organization.

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Production Kong",
    "description": "Production Kong Gateway",
    "baseUrl": "https://admin.kong.prod.com",
    "endpoint": "/admin-api",
    "authToken": "kong-admin-token-123",
    "organizationId": 123,
    "createdAt": "2024-11-04T16:30:00Z"
  }
]
```

### Get Client API Details by ID
```http
GET /api/client-api-details/{id}
```

**Description:** Retrieve specific client API details by ID.

**Path Parameters:**
- `id` (Long): Client API details ID

**Response (200 OK):** Same structure as create response.

### Delete Client API Details
```http
DELETE /api/client-api-details/{id}
```

**Description:** Delete client API details by ID.

**Path Parameters:**
- `id` (Long): Client API details ID

**Response (204 No Content)**

---

## Data Models

### Connect Request Structure
```json
{
  "environment": "konnect",           // "konnect" or "self-managed"
  "adminApiUrl": "https://api.konghq.com",
  "workspace": "default",             // Kong workspace or Konnect control plane
  "token": "kpat_xxxxxxxxxxx",        // Bearer token or Personal Access Token
  "mtlsCertPem": "-----BEGIN CERTIFICATE-----...", // Optional mTLS certificate
  "mtlsKeyPem": "-----BEGIN PRIVATE KEY-----...",  // Optional mTLS private key
  "scope": {
    "workspaces": ["default"],
    "services": ["payment-api", "user-api"]
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

### Kong Event Structure
```json
{
  "kong_request_id": "req-123",
  "correlation_id": "corr-456",
  "timestamp": "2024-11-04T16:30:00Z",
  "service": {
    "id": "service-123",
    "name": "payment-api",
    "host": "payment.internal",
    "port": 8080,
    "protocol": "http"
  },
  "route": {
    "id": "route-456",
    "name": "payment-route",
    "paths": ["/api/v1/payments"],
    "methods": ["POST", "GET"]
  },
  "consumer": {
    "id": "consumer-789",
    "username": "api-client-1",
    "custom_id": "client-123"
  },
  "request": {
    "method": "POST",
    "path": "/api/v1/payments",
    "size": 1024
  },
  "response": {
    "status": 200,
    "size": 512
  },
  "latencies": {
    "request": 150,
    "kong": 5,
    "proxy": 145
  }
}
```

---

## Error Handling

### Standard Error Response Format
```json
{
  "timestamp": "2024-11-04T16:30:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid Kong connection parameters",
  "path": "/integrations/kong/connect"
}
```

### Common HTTP Status Codes
- **200 OK**: Successful operations
- **202 Accepted**: Asynchronous operations initiated
- **204 No Content**: Successful DELETE operations
- **400 Bad Request**: Invalid request data
- **401 Unauthorized**: Missing or invalid authentication
- **404 Not Found**: Resource not found
- **502 Bad Gateway**: Kong connectivity issues
- **500 Internal Server Error**: Unexpected server errors

---

## Integration Patterns

### Kong Gateway Integration Flow
1. **Connect**: Establish connection to Kong Admin API
2. **Catalog Sync**: Import services, routes, and consumers
3. **Plugin Installation**: Auto-install required plugins
4. **Event Hook Setup**: Configure CRUD and rate limit event hooks
5. **Usage Ingestion**: Receive and process HTTP Log events
6. **Rate Limit Enforcement**: Push rate limits to consumer groups

### Multi-Tenant Architecture
- **Organization Isolation**: All data scoped to organization ID
- **JWT Authentication**: Bearer token with organization claims
- **Tenant Context**: Organization context maintained throughout requests

### Asynchronous Processing
- **Catalog Sync**: Background synchronization of Kong entities
- **Usage Processing**: Batch processing of usage events
- **Rate Limit Updates**: Asynchronous rate limit enforcement

---

## Configuration

### Application Properties
```yaml
# Server configuration
server.port: 8086

# Database configuration
spring.datasource.url: jdbc:postgresql://localhost:5436/kong
spring.datasource.username: root
spring.datasource.password: P4ssword!

# Kong integration
kong.client-details-url: http://localhost:8081/mock/api/details
kong.integration.enabled: true
kong.integration.default-admin-url: https://api.konghq.com
kong.integration.timeout: 30000
kong.integration.retry-attempts: 3

# Usage processing
kong.integration.usage-processing.batch-size: 100
kong.integration.usage-processing.processing-interval-ms: 60000

# Rate limiting defaults
kong.integration.rate-limiting.default-limits.minute: 100
kong.integration.rate-limiting.default-limits.hour: 1000
kong.integration.rate-limiting.default-limits.day: 10000
kong.integration.rate-limiting.default-limits.month: 100000
```

---

## Notes

1. **Kong Compatibility**: Supports both Kong Gateway and Kong Konnect
2. **Authentication Methods**: Bearer tokens, Personal Access Tokens, and mTLS
3. **Plugin Management**: Auto-installation of required plugins
4. **Event Processing**: Real-time and batch event processing
5. **Rate Limiting**: Advanced rate limiting with consumer groups
6. **Analytics**: Comprehensive usage analytics and billing summaries
7. **Multi-tenancy**: Organization-based data isolation
8. **Database**: PostgreSQL with Liquibase schema management
9. **Port**: Service runs on port 8086
10. **Swagger UI**: Available at `/swagger-ui.html`

---

*Last Updated: November 4, 2024*  
*Service Version: 1.0.0*  
*Port: 8086*
