#!/bin/bash

# Apigee Integration Service - API Test Script
# Usage: ./test-api.sh

BASE_URL="http://localhost:8086/api/integrations/apigee"

echo "========================================="
echo "Apigee Integration Service - API Tests"
echo "========================================="
echo ""

# 1. Save & Test Connection
echo "1. Testing connection..."
curl -X POST "$BASE_URL/connections" \
  -H 'Content-Type: application/json' \
  -d '{
    "org": "aadhaar-x",
    "envs": "dev,stage,prod",
    "analyticsMode": "WEBHOOK",
    "hmacSecret": "change-me",
    "saJsonPath": "/secrets/apigee-sa.json"
  }'
echo -e "\n"

# 2. List API Products
echo "2. Listing API Products..."
curl -X GET "$BASE_URL/products"
echo -e "\n"

# 3. List Developers
echo "3. Listing Developers..."
curl -X GET "$BASE_URL/developers"
echo -e "\n"

# 4. List Apps for Developer
echo "4. Listing Apps for Developer icici-001..."
curl -X GET "$BASE_URL/developers/icici-001/apps"
echo -e "\n"

# 5. Link Developer to Aforo Customer
echo "5. Linking Developer to Aforo Customer..."
curl -X POST "$BASE_URL/developers/icici-001/link" \
  -H 'Content-Type: application/json' \
  -d '{"aforoCustomerId": "201"}'
echo -e "\n"

# 6. Create Draft Subscription
echo "6. Creating Draft Subscription..."
curl -X POST "$BASE_URL/mappings/subscriptions" \
  -H 'Content-Type: application/json' \
  -d '{
    "developerApp": "icici-mobile-app",
    "apiProduct": "aadhaar-kyc-product",
    "aforoProductId": 501,
    "ratePlanId": 3007,
    "billingType": "POSTPAID"
  }'
echo -e "\n"

# 7. Authorize Request
echo "7. Testing Authorization..."
curl -X POST "$BASE_URL/authorize" \
  -H 'Content-Type: application/json' \
  -d '{
    "org": "aadhaar-x",
    "env": "prod",
    "developerApp": "icici-mobile-app",
    "apiProduct": "aadhaar-kyc-product",
    "method": "GET",
    "path": "/v1/kyc/check"
  }'
echo -e "\n"

# 8. Ingest Usage (with HMAC)
echo "8. Ingesting Usage Event (with HMAC)..."
BODY='{"ts":"2025-11-05T06:15:30Z","apiproxy":"kyc-api","developerApp":"icici-mobile-app","apiProduct":"aadhaar-kyc-product","method":"GET","path":"/v1/kyc/check","status":200,"latencyMs":120,"bytesOut":2048}'
SIG=$(echo -n "$BODY" | openssl dgst -sha256 -hmac 'change-me' -binary | base64)

curl -X POST "$BASE_URL/webhooks/usage" \
  -H 'Content-Type: application/json' \
  -H "X-Aforo-Signature: $SIG" \
  -d "$BODY"
echo -e "\n"

echo "========================================="
echo "All tests completed!"
echo "========================================="
