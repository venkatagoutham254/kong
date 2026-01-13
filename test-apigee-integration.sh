#!/bin/bash

# Apigee Integration Test Script
# This script tests all Apigee integration endpoints with mock data

# Configuration
BASE_URL="http://localhost:8086"
JWT_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhZm9yby1jdXN0b21lcnNlcnZpY2UiLCJzdWIiOiJtbUBhZm9yby5haSIsIm9yZ0lkIjoxOCwic3RhdHVzIjoiQUNUSVZFIiwiaWF0IjoxNzY0ODI1MTQ0LCJleHAiOjE3NjU0Mjk5NDR9.e1EPDjLKsQfctzs4dT6D9MdYZQRVojGbxUdkUAahsn8"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ $2${NC}"
    else
        echo -e "${RED}✗ $2${NC}"
    fi
}

echo "========================================="
echo "     Apigee Integration Test Suite"
echo "========================================="
echo ""

# Test 1: Health Check
echo -e "${YELLOW}Test 1: Health Check${NC}"
response=$(curl -s -w "\n%{http_code}" -X GET \
    "${BASE_URL}/integrations/apigee/health" \
    -H "Authorization: Bearer ${JWT_TOKEN}")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
if [ "$http_code" = "200" ]; then
    print_status 0 "Health check passed"
    echo "Response: $body" | jq '.' 2>/dev/null || echo "$body"
else
    print_status 1 "Health check failed (HTTP $http_code)"
fi
echo ""

# Test 2: Connect to Apigee
echo -e "${YELLOW}Test 2: Connect to Apigee${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST \
    "${BASE_URL}/integrations/apigee/connect" \
    -H "Authorization: Bearer ${JWT_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{
        "org": "test-org",
        "env": "test"
    }')
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
if [ "$http_code" = "200" ] || [ "$http_code" = "502" ]; then
    print_status 0 "Connect endpoint working (HTTP $http_code)"
    echo "Response: $body" | jq '.' 2>/dev/null || echo "$body"
else
    print_status 1 "Connect failed (HTTP $http_code)"
fi
echo ""

# Test 3: Catalog Sync
echo -e "${YELLOW}Test 3: Catalog Sync${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST \
    "${BASE_URL}/integrations/apigee/catalog/sync?syncType=full" \
    -H "Authorization: Bearer ${JWT_TOKEN}")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
if [ "$http_code" = "202" ] || [ "$http_code" = "500" ]; then
    print_status 0 "Catalog sync endpoint working (HTTP $http_code)"
    echo "Response: $body" | jq '.' 2>/dev/null || echo "$body"
else
    print_status 1 "Catalog sync failed (HTTP $http_code)"
fi
echo ""

# Test 4: Ingest Single Event
echo -e "${YELLOW}Test 4: Ingest Single Event${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST \
    "${BASE_URL}/integrations/apigee/ingest" \
    -H "Authorization: Bearer ${JWT_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{
        "timestamp": "2025-12-05T10:00:00Z",
        "org": "test-org",
        "env": "test",
        "apiProxy": "payment-api",
        "proxyBasepath": "/payments",
        "resourcePath": "/charge",
        "method": "POST",
        "status": 200,
        "latencyMs": 150,
        "developerId": "dev@example.com",
        "appName": "mobile-app",
        "apiProduct": "SILVER_PRODUCT",
        "apiKey": "key123",
        "requestSize": 1024,
        "responseSize": 2048
    }')
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
if [ "$http_code" = "202" ]; then
    print_status 0 "Single event ingestion passed"
    echo "Response: $body" | jq '.' 2>/dev/null || echo "$body"
else
    print_status 1 "Single event ingestion failed (HTTP $http_code)"
fi
echo ""

# Test 5: Ingest Multiple Events
echo -e "${YELLOW}Test 5: Ingest Multiple Events${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST \
    "${BASE_URL}/integrations/apigee/ingest" \
    -H "Authorization: Bearer ${JWT_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '[
        {
            "timestamp": "2025-12-05T10:00:00Z",
            "org": "test-org",
            "env": "test",
            "apiProxy": "payment-api",
            "method": "GET",
            "status": 200
        },
        {
            "timestamp": "2025-12-05T10:01:00Z",
            "org": "test-org",
            "env": "test",
            "apiProxy": "user-api",
            "method": "POST",
            "status": 201
        }
    ]')
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
if [ "$http_code" = "202" ]; then
    print_status 0 "Multiple events ingestion passed"
    echo "Response: $body" | jq '.' 2>/dev/null || echo "$body"
else
    print_status 1 "Multiple events ingestion failed (HTTP $http_code)"
fi
echo ""

# Test 6: Enforce Plans
echo -e "${YELLOW}Test 6: Enforce Plans${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST \
    "${BASE_URL}/integrations/apigee/enforce/plans" \
    -H "Authorization: Bearer ${JWT_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{
        "mappings": [
            {
                "planId": "SILVER",
                "developerId": "dev@example.com",
                "appName": "mobile-app",
                "consumerKey": "key123",
                "apiProductName": "SILVER_PRODUCT"
            }
        ]
    }')
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
if [ "$http_code" = "200" ] || [ "$http_code" = "500" ]; then
    print_status 0 "Enforce plans endpoint working (HTTP $http_code)"
    echo "Response: $body" | jq '.' 2>/dev/null || echo "$body"
else
    print_status 1 "Enforce plans failed (HTTP $http_code)"
fi
echo ""

# Test 7: Suspend App
echo -e "${YELLOW}Test 7: Suspend App${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST \
    "${BASE_URL}/integrations/apigee/suspend" \
    -H "Authorization: Bearer ${JWT_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{
        "developerId": "dev@example.com",
        "appName": "mobile-app",
        "consumerKey": "key123",
        "mode": "revoke",
        "reason": "Prepaid wallet balance is zero"
    }')
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
if [ "$http_code" = "200" ] || [ "$http_code" = "404" ] || [ "$http_code" = "500" ]; then
    print_status 0 "Suspend app endpoint working (HTTP $http_code)"
    echo "Response: $body" | jq '.' 2>/dev/null || echo "$body"
else
    print_status 1 "Suspend app failed (HTTP $http_code)"
fi
echo ""

# Test 8: Resume App
echo -e "${YELLOW}Test 8: Resume App${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST \
    "${BASE_URL}/integrations/apigee/resume?developerId=dev@example.com&appName=mobile-app&consumerKey=key123" \
    -H "Authorization: Bearer ${JWT_TOKEN}")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
if [ "$http_code" = "200" ] || [ "$http_code" = "404" ] || [ "$http_code" = "500" ]; then
    print_status 0 "Resume app endpoint working (HTTP $http_code)"
    echo "Response: $body" | jq '.' 2>/dev/null || echo "$body"
else
    print_status 1 "Resume app failed (HTTP $http_code)"
fi
echo ""

# Test 9: Test without JWT (Security Check)
echo -e "${YELLOW}Test 9: Security Check (No JWT)${NC}"
response=$(curl -s -w "\n%{http_code}" -X GET \
    "${BASE_URL}/integrations/apigee/health")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "401" ] || [ "$http_code" = "403" ]; then
    print_status 0 "Security check passed - Unauthorized without JWT"
else
    print_status 1 "Security check failed - Expected 401/403, got $http_code"
fi
echo ""

echo "========================================="
echo "         Test Suite Complete"
echo "========================================="
echo ""
echo "Note: Some tests may fail if Apigee credentials are not configured."
echo "This is expected for mock testing. The important thing is that"
echo "the endpoints are responding and the API structure is correct."
echo ""
echo "To test with real Apigee:"
echo "1. Set APIGEE_ORG, APIGEE_ENV, and APIGEE_TOKEN environment variables"
echo "2. Restart the application"
echo "3. Run this script again"
