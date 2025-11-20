#!/bin/bash

# Test script for selective product import

echo "=== Testing Apigee Selective Product Import ==="
echo ""

# Base URL
BASE_URL="http://localhost:8080/api/integrations/apigee"

# Step 1: List available products from Apigee
echo "1. Fetching available products from Apigee..."
curl -X GET "$BASE_URL/products" \
  -H "Content-Type: application/json" | jq '.'

echo ""
echo "2. Importing selected products with assigned types..."
echo "   - Selecting 2 products with different types"
echo ""

# Step 2: Import selected products with types
curl -X POST "$BASE_URL/products/import-selected" \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: 1" \
  -d '{
    "selectedProducts": [
      {
        "productName": "product1",
        "displayName": "Product One",
        "productType": "API",
        "quota": "1000",
        "resources": ["/api/v1/*"]
      },
      {
        "productName": "product2", 
        "displayName": "Product Two",
        "productType": "FlatFile",
        "quota": "5000",
        "resources": ["/files/*"]
      },
      {
        "productName": "product3",
        "displayName": "Product Three",
        "productType": "SQLResult",
        "quota": "10000",
        "resources": ["/sql/*"]
      },
      {
        "productName": "product4",
        "displayName": "Product Four",
        "productType": "LLMToken",
        "quota": "2000",
        "resources": ["/llm/*"]
      }
    ]
  }' | jq '.'

echo ""
echo "=== Test Complete ==="
