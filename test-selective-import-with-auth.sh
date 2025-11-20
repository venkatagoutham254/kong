#!/bin/bash

# Test script for selective product import with JWT authentication

JWT_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhZm9yby1jdXN0b21lcnNlcnZpY2UiLCJzdWIiOiJtbHZnQGFmb3JvLmFpIiwic3RhdHVzIjoiQUNUSVZFIiwib3JnSWQiOjEyLCJpYXQiOjE3NjI5MzM3NDYsImV4cCI6MTc2MzUzODU0Nn0.AiWWt6g5-ecB875FthSiL8gp4V749-XBN4StS9d9xNA"
BASE_URL="http://localhost:8086/api/integrations/apigee"

echo "=== Testing Apigee Selective Product Import with JWT Authentication ==="
echo ""

# Step 1: Test connection (this will fail without proper credentials, but shows the endpoint works)
echo "1. Testing Apigee connection..."
curl -X POST "$BASE_URL/connections" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "org": "aforo-aadhaar-477687",
    "envs": "eval",
    "analyticsMode": "STANDARD",
    "hmacSecret": "test-secret",
    "saJsonPath": "/Users/venkatagowtham/Downloads/aforo-aadhaar-477687-9d73f6823717.json"
  }' | jq '.'

echo ""
echo "2. Listing available products from Apigee..."
echo "   (This requires valid Apigee connection)"
curl -X GET "$BASE_URL/products?org=aforo-aadhaar-477687" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.'

echo ""
echo "3. Testing selective import endpoint with sample products..."
echo "   Importing 4 products with all 4 product types (API, FlatFile, SQLResult, LLMToken)"
echo ""

# Step 3: Import selected products with types
curl -X POST "$BASE_URL/products/import-selected" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "X-Organization-Id: 12" \
  -d '{
    "selectedProducts": [
      {
        "productName": "chatbot-cloud-api",
        "displayName": "Chatbot Cloud API",
        "productType": "API",
        "quota": "1000",
        "resources": ["/api/v1/*", "/chatbot/*"]
      },
      {
        "productName": "stripe-payment-data",
        "displayName": "Stripe Payment Data Export",
        "productType": "FlatFile",
        "quota": "5000",
        "resources": ["/files/payments/*"]
      },
      {
        "productName": "customer-analytics-sql",
        "displayName": "Customer Analytics SQL Results",
        "productType": "SQLResult",
        "quota": "10000",
        "resources": ["/sql/analytics/*"]
      },
      {
        "productName": "gpt4-llm-tokens",
        "displayName": "GPT-4 LLM Token Usage",
        "productType": "LLMToken",
        "quota": "2000",
        "resources": ["/llm/gpt4/*"]
      }
    ]
  }' | jq '.'

echo ""
echo "=== Test Complete ==="
echo ""
echo "Summary:"
echo "- Connection endpoint: POST /api/integrations/apigee/connections"
echo "- List products endpoint: GET /api/integrations/apigee/products"
echo "- Selective import endpoint: POST /api/integrations/apigee/products/import-selected"
echo ""
echo "Product Types Available (only 4):"
echo "  1. API - For API-based products"
echo "  2. FlatFile - For file-based products"
echo "  3. SQLResult - For SQL/database products"
echo "  4. LLMToken - For LLM token-based products"
