# Kong-Aforo Integration Implementation Guide

## 🚀 Quick Start

```bash
# 1. Start PostgreSQL
docker run -d -p 5436:5432 \
  -e POSTGRES_USER=root \
  -e POSTGRES_PASSWORD=P4ssword! \
  -e POSTGRES_DB=kong \
  postgres:15

# 2. Run database migrations
mvn clean compile
mvn liquibase:update

# 3. Load sample data (optional)
mvn liquibase:update -Dliquibase.contexts=dev

# 4. Start the application
mvn spring-boot:run

# 5. Access the API
curl http://localhost:8086/api/kong
```

# Kong Integration Implementation Guide

## Overview
This backend implements the Aforo-Kong integration for API monetization. It tracks API usage, manages pricing plans, and enforces rate limits.

## Architecture

```
Kong Gateway → HTTP Log → Aforo Backend → Database
     ↑                                        ↓
     └──── Rate Limiting Enforcement ←────────┘
```

## Key Components

### 1. Database Schema
- **kong_product**: Your existing products from Kong
- **kong_service**: API services from Kong
- **kong_route**: API routes/endpoints
- **kong_consumer**: API consumers/customers  
- **usage_record**: Every API call for billing
- **pricing_plan**: Pricing tiers and quotas

### 2. API Endpoints

#### Connection Management
- `POST /integrations/kong/connect` - Connect to Kong
- `POST /integrations/kong/catalog/sync` - Sync catalog

#### Usage Ingestion
- `POST /integrations/kong/ingest` - Receive usage events from Kong

#### Event Hooks
- `POST /integrations/kong/events` - Process CRUD events

#### Enforcement
- `POST /integrations/kong/enforce/groups` - Set rate limits
- `POST /integrations/kong/suspend` - Suspend consumer
- `POST /integrations/kong/resume/{id}` - Resume consumer

## Setup Instructions

### 1. Run Database Migrations
```bash
mvn liquibase:update
```

### 2. Configure Kong HTTP Log Plugin
Point the HTTP Log plugin to your Aforo instance:
```json
{
  "name": "http-log",
  "config": {
    "http_endpoint": "http://your-aforo-host/integrations/kong/ingest",
    "method": "POST",
    "timeout": 10000,
    "keepalive": 60000
  }
}
```

### 3. Configure Correlation ID Plugin
```json
{
  "name": "correlation-id",
  "config": {
    "header_name": "X-Correlation-ID",
    "generator": "uuid",
    "echo_downstream": true
  }
}
```

## Business Terms Explained

### Services vs Products
- **Kong Service**: Technical API service (e.g., orders-service)
- **Aforo Product**: Business product you sell (e.g., Orders API)
- One product can have multiple services

### Routes vs Endpoints
- **Kong Route**: URL path pattern (e.g., /v1/orders/*)
- **Aforo Endpoint**: Specific API endpoint for billing

### Consumers vs Customers
- **Kong Consumer**: Technical API user with credentials
- **Aforo Customer**: Business entity being billed

### Pricing Concepts
- **Metrics**: What you measure (calls, bytes, latency)
- **Tiers**: Pricing levels (Bronze/Silver/Gold)
- **Quotas**: API call limits per time window
- **Overage**: Extra charges when quota exceeded
- **Prepaid Wallet**: Credit-based payment model

## TODO - Remaining Implementation

### High Priority
1. Implement actual Kong Admin API calls in `KongIntegrationServiceImpl`:
   - Fetch services: `GET /services`
   - Fetch routes: `GET /routes`
   - Fetch consumers: `GET /consumers`
   - Create consumer groups
   - Configure rate limiting plugins

2. Add authentication for ingestion endpoint:
   - HMAC signature verification
   - mTLS support

3. Implement pricing calculation:
   - Process usage records
   - Calculate costs based on pricing plans
   - Update wallet balances

### Medium Priority
4. Analytics endpoints:
   - Usage dashboards
   - Revenue reports
   - API call statistics

5. Reconciliation:
   - Compare Aforo records with Kong metrics
   - Alert on discrepancies

### Low Priority
6. Advanced features:
   - Auto-topup for prepaid
   - Usage alerts
   - Export to CSV/Parquet

## Example Pricing Plan
```json
{
  "planName": "silver",
  "metrics": [
    {
      "type": "calls",
      "tiers": [
        {"from": 0, "to": 10000, "price": 0},
        {"from": 10001, "to": 100000, "price": 0.001},
        {"from": 100001, "to": null, "price": 0.0005}
      ]
    }
  ],
  "quotaLimits": {
    "day": 10000,
    "month": 250000
  }
}
```

## Testing the Integration

### 1. Connect to Kong
```bash
curl -X POST http://localhost:8080/integrations/kong/connect \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "environment": "self-managed",
    "adminApiUrl": "http://kong-admin:8001",
    "token": "kong-admin-token"
  }'
```

### 2. Test Usage Ingestion
```bash
curl -X POST http://localhost:8080/integrations/kong/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "correlation_id": "test-123",
    "service": {"id": "s1", "name": "test"},
    "consumer": {"id": "c1", "username": "testuser"},
    "request": {"method": "GET", "path": "/test"},
    "response": {"status": 200}
  }'
```

### 3. Check Usage Records
```bash
curl http://localhost:8080/api/kong \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Support
For questions about the integration, refer to the PRD document or contact the platform team.
