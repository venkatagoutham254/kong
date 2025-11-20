# Apigee Integration Service for Aforo

A Spring Boot microservice that integrates Apigee API Management with the Aforo billing system.

## Features

- **Apigee Inventory Management**: Import API Products, Developers, and Developer Apps from Apigee
- **Developer Linking**: Link Apigee Developers to Aforo Customers
- **Subscription Mapping**: Create DRAFT subscriptions in Aforo for (App × API Product) combinations
- **Attribute Sync**: Write back Aforo subscription IDs to Apigee App attributes
- **Runtime Authorization**: Provide authorization decisions based on subscription status
- **Usage Ingestion**: Ingest usage events via HMAC-signed webhooks

## Tech Stack

- **Java 21**
- **Spring Boot 3.2.0**
- **PostgreSQL 16**
- **Maven**
- **Docker & Docker Compose**

## Architecture

The service follows a **Ports & Adapters** (Hexagonal) architecture:

```
com.aforo.apigee
 ├─ config/          # Spring configuration
 ├─ controller/      # REST API endpoints
 ├─ dto/             # Request/Response DTOs
 ├─ gateway/         # Apigee client interface (Fake & Real implementations)
 ├─ model/           # JPA entities
 ├─ repository/      # Spring Data JPA repositories
 ├─ security/        # HMAC filter for webhooks
 ├─ service/         # Business logic
 └─ util/            # Utilities
```

## Prerequisites

- Java 21
- Maven 3.8+
- Docker & Docker Compose (for containerized deployment)
- PostgreSQL 16 (if running locally without Docker)

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `aforo_apigee` |
| `DB_USER` | Database user | `postgres` |
| `DB_PASS` | Database password | `postgres` |
| `APIGEE_ORG` | Apigee organization | `aadhaar-x` |
| `APIGEE_BASE` | Apigee base URL | `https://apigee.googleapis.com` |
| `APIGEE_PROJECT_ID` | GCP project ID | `your-gcp-project` |
| `APIGEE_SA_JSON_PATH` | Service account JSON path | `/secrets/apigee-sa.json` |
| `APIGEE_ANALYTICS` | Analytics mode (WEBHOOK/BIGQUERY) | `WEBHOOK` |
| `APIGEE_FAKE` | Use fake Apigee adapter | `true` |
| `AFORO_HMAC_SECRET` | HMAC secret for webhook verification | `change-me` |
| `SUBSCRIPTION_SERVICE_URL` | Subscription service URL | `http://localhost:8082` |
| `PRODUCT_SERVICE_URL` | Product service URL | `http://localhost:8080` |
| `RATEPLAN_SERVICE_URL` | Rate plan service URL | `http://localhost:8080` |
| `CUSTOMER_SERVICE_URL` | Customer service URL | `http://localhost:8081` |

## Build & Run

### Local Development (with Fake Apigee)

```bash
# Build
mvn clean package -DskipTests

# Run
java -jar target/apigee-integration-service-1.0.0.jar
```

The service will start on port **8086**.

### Docker Compose

```bash
# Build and start
docker-compose up -d

# View logs
docker-compose logs -f apigee-integration

# Stop
docker-compose down
```

### Production (with Real Apigee)

1. Create a GCP Service Account with Apigee permissions
2. Download the service account JSON key
3. Place it in `./secrets/apigee-sa.json`
4. Set `APIGEE_FAKE=false` in environment variables
5. Update `APIGEE_ORG`, `APIGEE_PROJECT_ID` accordingly

## API Endpoints

### Base URL
```
http://localhost:8086/api/integrations/apigee
```

### Endpoints

#### 1. Save & Test Connection
```bash
POST /connections
Content-Type: application/json

{
  "org": "aadhaar-x",
  "envs": "dev,stage,prod",
  "analyticsMode": "WEBHOOK",
  "hmacSecret": "secret",
  "saJsonPath": "/secrets/apigee-sa.json"
}
```

#### 2. List API Products
```bash
GET /products
```

#### 3. List Developers
```bash
GET /developers
```

#### 4. List Developer Apps
```bash
GET /developers/{developerId}/apps
```

#### 5. Link Developer to Aforo Customer
```bash
POST /developers/{developerId}/link
Content-Type: application/json

{
  "aforoCustomerId": "201"
}
```

#### 6. Create Draft Subscription
```bash
POST /mappings/subscriptions
Content-Type: application/json

{
  "developerApp": "icici-mobile-app",
  "apiProduct": "aadhaar-kyc-product",
  "aforoProductId": 501,
  "ratePlanId": 3007,
  "billingType": "POSTPAID"
}
```

#### 7. Authorize Request
```bash
POST /authorize
Content-Type: application/json

{
  "org": "aadhaar-x",
  "env": "prod",
  "developerApp": "icici-mobile-app",
  "apiProduct": "aadhaar-kyc-product",
  "method": "GET",
  "path": "/v1/kyc/check"
}
```

#### 8. Ingest Usage (HMAC-signed)
```bash
POST /webhooks/usage
Content-Type: application/json
X-Aforo-Signature: <HMAC-SHA256-Base64>

{
  "ts": "2025-11-05T06:15:30Z",
  "apiproxy": "kyc-api",
  "developerApp": "icici-mobile-app",
  "apiProduct": "aadhaar-kyc-product",
  "method": "GET",
  "path": "/v1/kyc/check",
  "status": 200,
  "latencyMs": 120,
  "bytesOut": 2048
}
```

## Testing with cURL

### Generate HMAC Signature
```bash
BODY='{"ts":"2025-11-05T06:15:30Z","apiproxy":"kyc-api","developerApp":"icici-mobile-app","apiProduct":"aadhaar-kyc-product","method":"GET","path":"/v1/kyc/check","status":200,"latencyMs":120,"bytesOut":2048}'
SIG=$(echo -n $BODY | openssl dgst -sha256 -hmac 'change-me' -binary | base64)

curl -X POST http://localhost:8086/api/integrations/apigee/webhooks/usage \
  -H 'Content-Type: application/json' \
  -H "X-Aforo-Signature: $SIG" \
  -d "$BODY"
```

## Swagger UI

Access the interactive API documentation at:
```
http://localhost:8086/swagger-ui.html
```

## Database Schema

### Tables

- **connection_configs**: Apigee connection configurations
- **imported_products**: Cached API Products from Apigee
- **developer_links**: Mapping between Apigee Developers and Aforo Customers
- **app_mappings**: Mapping between (App × Product) and Aforo Subscriptions
- **usage_events**: Usage events ingested from Apigee

## Development Modes

### Fake Mode (Development)
- Set `APIGEE_FAKE=true`
- Uses `FakeApigeeGateway` with static demo data
- No real Apigee API calls
- Ideal for local development and testing

### Real Mode (Production)
- Set `APIGEE_FAKE=false`
- Uses `RealApigeeGateway` with Google OAuth
- Requires valid GCP Service Account
- Makes actual Apigee Management API calls

## Security

- **HMAC Verification**: All webhook requests to `/webhooks/usage` must include a valid `X-Aforo-Signature` header
- **Algorithm**: HMAC-SHA256
- **Encoding**: Base64

## Logging

Logs are configured at DEBUG level for:
- `com.aforo.apigee.*`
- `org.hibernate.SQL`

## Troubleshooting

### Connection Issues
- Verify PostgreSQL is running and accessible
- Check database credentials in environment variables
- Ensure port 5432 (or 5433 for Docker) is not blocked

### Apigee Integration Issues
- Verify service account JSON is valid and accessible
- Check GCP project ID and Apigee organization name
- Ensure service account has required Apigee permissions

### HMAC Verification Failures
- Verify HMAC secret matches between sender and receiver
- Ensure request body is not modified in transit
- Check that signature is calculated over raw body bytes

## License

Proprietary - Aforo
