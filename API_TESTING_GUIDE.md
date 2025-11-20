# API Testing Guide - Selective Product Import

## Quick Start

### Prerequisites
- Application running on port 8086
- JWT Token for authentication
- Organization ID: 12

### JWT Token
```
eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhZm9yby1jdXN0b21lcnNlcnZpY2UiLCJzdWIiOiJtbHZnQGFmb3JvLmFpIiwic3RhdHVzIjoiQUNUSVZFIiwib3JnSWQiOjEyLCJpYXQiOjE3NjI5MzM3NDYsImV4cCI6MTc2MzUzODU0Nn0.AiWWt6g5-ecB875FthSiL8gp4V749-XBN4StS9d9xNA
```

---

## API Endpoints

### 1. Test Connection
```bash
curl -X POST http://localhost:8086/api/integrations/apigee/connections \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "org": "aforo-aadhaar-477687",
    "envs": "eval",
    "analyticsMode": "STANDARD",
    "hmacSecret": "test-secret",
    "saJsonPath": "/path/to/service-account.json"
  }'
```

### 2. List Products (No Auto-Import)
```bash
curl -X GET "http://localhost:8086/api/integrations/apigee/products?org=aforo-aadhaar-477687" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response**: Array of available products from Apigee
```json
[
  {
    "name": "product-1",
    "displayName": "Product One",
    "quota": "1000",
    "resources": ["/api/*"]
  }
]
```

### 3. Import Selected Products with Types
```bash
curl -X POST http://localhost:8086/api/integrations/apigee/products/import-selected \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Organization-Id: 12" \
  -d '{
    "selectedProducts": [
      {
        "productName": "api-product",
        "displayName": "API Product",
        "productType": "API",
        "quota": "1000",
        "resources": ["/api/v1/*"]
      },
      {
        "productName": "file-product",
        "displayName": "File Product",
        "productType": "FlatFile",
        "quota": "5000",
        "resources": ["/files/*"]
      }
    ]
  }'
```

**Response**:
```json
{
  "totalSelected": 2,
  "successfullyImported": 2,
  "failed": 0,
  "message": "Import completed: 2 successful, 0 failed out of 2 selected",
  "importedProducts": [
    {
      "productName": "api-product",
      "productType": "API",
      "status": "SUCCESS",
      "message": "Product created successfully",
      "productId": 123
    },
    {
      "productName": "file-product",
      "productType": "FlatFile",
      "status": "SUCCESS",
      "message": "Product created successfully",
      "productId": 124
    }
  ]
}
```

---

## Product Types (Only 4 Available)

| Type | Description | Use Case |
|------|-------------|----------|
| **API** | API-based products | REST APIs, GraphQL endpoints |
| **FlatFile** | File-based products | CSV exports, JSON files, data dumps |
| **SQLResult** | SQL/Database products | Query results, database exports |
| **LLMToken** | LLM token-based products | GPT-4, Claude, LLM API usage |

---

## Testing with Postman/Swagger

### Swagger UI
Access API documentation at:
```
http://localhost:8086/swagger-ui.html
```

### Postman Collection Headers
```
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
X-Organization-Id: 12
```

---

## Example Test Scenarios

### Scenario 1: Import Single Product
```json
{
  "selectedProducts": [
    {
      "productName": "payment-api",
      "displayName": "Payment Processing API",
      "productType": "API",
      "quota": "10000",
      "resources": ["/payments/*", "/transactions/*"]
    }
  ]
}
```

### Scenario 2: Import Multiple Products with Different Types
```json
{
  "selectedProducts": [
    {
      "productName": "user-api",
      "displayName": "User Management API",
      "productType": "API",
      "quota": "5000",
      "resources": ["/users/*"]
    },
    {
      "productName": "analytics-export",
      "displayName": "Analytics Data Export",
      "productType": "FlatFile",
      "quota": "1000",
      "resources": ["/exports/*"]
    },
    {
      "productName": "customer-queries",
      "displayName": "Customer Database Queries",
      "productType": "SQLResult",
      "quota": "2000",
      "resources": ["/queries/*"]
    },
    {
      "productName": "ai-chatbot",
      "displayName": "AI Chatbot Token Usage",
      "productType": "LLMToken",
      "quota": "50000",
      "resources": ["/ai/*"]
    }
  ]
}
```

---

## Error Handling

### 403 Forbidden
**Cause**: Missing or invalid JWT token  
**Solution**: Add valid JWT token in Authorization header

### 400 Bad Request
**Cause**: Invalid product type or missing required fields  
**Solution**: Ensure productType is one of: API, FlatFile, SQLResult, LLMToken

### 500 Internal Server Error
**Cause**: ProductRatePlanService not available  
**Solution**: Ensure ProductRatePlanService is running on port 8081

---

## Validation Rules

1. **Product Type** (Required): Must be one of the 4 types
2. **Product Name** (Required): Unique identifier
3. **Display Name** (Optional): Human-readable name
4. **Quota** (Optional): Usage quota
5. **Resources** (Optional): List of API resources

---

## Quick Test Script

Run the provided test script:
```bash
chmod +x test-selective-import-with-auth.sh
./test-selective-import-with-auth.sh
```

This will test all endpoints and demonstrate the selective import functionality.
