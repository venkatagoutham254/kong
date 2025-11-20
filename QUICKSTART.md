# Quick Start Guide

## 1. Build the Application

```bash
mvn clean package -DskipTests
```

## 2. Start with Docker Compose (Recommended)

```bash
# Start all services
docker-compose up -d

# Check logs
docker-compose logs -f apigee-integration

# Wait for the service to be ready (check logs for "Started ApigeeIntegrationServiceApplication")
```

## 3. Run Test Script

```bash
# Make the script executable
chmod +x test-api.sh

# Run all tests
./test-api.sh
```

## 4. Access Swagger UI

Open your browser and navigate to:
```
http://localhost:8086/swagger-ui.html
```

## 5. Quick Test Flow

### Step 1: Save Connection
```bash
curl -X POST http://localhost:8086/api/integrations/apigee/connections \
  -H 'Content-Type: application/json' \
  -d '{
    "org": "aadhaar-x",
    "envs": "dev,stage,prod",
    "analyticsMode": "WEBHOOK",
    "hmacSecret": "change-me",
    "saJsonPath": "/secrets/apigee-sa.json"
  }'
```

### Step 2: List API Products
```bash
curl http://localhost:8086/api/integrations/apigee/products
```

### Step 3: List Developers
```bash
curl http://localhost:8086/api/integrations/apigee/developers
```

### Step 4: List Apps for a Developer
```bash
curl http://localhost:8086/api/integrations/apigee/developers/icici-001/apps
```

### Step 5: Link Developer to Aforo Customer
```bash
curl -X POST http://localhost:8086/api/integrations/apigee/developers/icici-001/link \
  -H 'Content-Type: application/json' \
  -d '{"aforoCustomerId": "201"}'
```

### Step 6: Create Draft Subscription
```bash
curl -X POST http://localhost:8086/api/integrations/apigee/mappings/subscriptions \
  -H 'Content-Type: application/json' \
  -d '{
    "developerApp": "icici-mobile-app",
    "apiProduct": "aadhaar-kyc-product",
    "aforoProductId": 501,
    "ratePlanId": 3007,
    "billingType": "POSTPAID"
  }'
```

### Step 7: Test Authorization
```bash
curl -X POST http://localhost:8086/api/integrations/apigee/authorize \
  -H 'Content-Type: application/json' \
  -d '{
    "org": "aadhaar-x",
    "env": "prod",
    "developerApp": "icici-mobile-app",
    "apiProduct": "aadhaar-kyc-product",
    "method": "GET",
    "path": "/v1/kyc/check"
  }'
```

### Step 8: Send Usage Event (with HMAC)
```bash
BODY='{"ts":"2025-11-05T06:15:30Z","apiproxy":"kyc-api","developerApp":"icici-mobile-app","apiProduct":"aadhaar-kyc-product","method":"GET","path":"/v1/kyc/check","status":200,"latencyMs":120,"bytesOut":2048}'
SIG=$(echo -n "$BODY" | openssl dgst -sha256 -hmac 'change-me' -binary | base64)

curl -X POST http://localhost:8086/api/integrations/apigee/webhooks/usage \
  -H 'Content-Type: application/json' \
  -H "X-Aforo-Signature: $SIG" \
  -d "$BODY"
```

## 6. Verify Data in Database

```bash
# Connect to PostgreSQL
docker exec -it apigee-db-1 psql -U postgres -d aforo_apigee

# Check tables
\dt

# View connection configs
SELECT * FROM connection_configs;

# View imported products
SELECT * FROM imported_products;

# View developer links
SELECT * FROM developer_links;

# View app mappings
SELECT * FROM app_mappings;

# View usage events
SELECT * FROM usage_events;

# Exit
\q
```

## 7. Stop Services

```bash
docker-compose down
```

## Troubleshooting

### Port Already in Use
If port 8086 or 5433 is already in use:
```bash
# Check what's using the port
lsof -i :8086
lsof -i :5433

# Kill the process or change the port in docker-compose.yml
```

### Database Connection Issues
```bash
# Check if PostgreSQL is running
docker-compose ps

# View database logs
docker-compose logs db

# Restart database
docker-compose restart db
```

### Application Won't Start
```bash
# View application logs
docker-compose logs apigee-integration

# Rebuild and restart
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

## Next Steps

1. **Production Setup**: Set `APIGEE_FAKE=false` and configure real Apigee credentials
2. **Integration**: Connect to actual subscription-service, product-service, etc.
3. **Monitoring**: Add application monitoring and alerting
4. **Security**: Update HMAC secret and secure database credentials
5. **Scaling**: Configure horizontal scaling for high availability
