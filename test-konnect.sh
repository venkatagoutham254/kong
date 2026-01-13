#!/bin/bash

BASE_URL="http://localhost:8086"
ORG_ID="27"
JWT_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhZm9yby1jdXN0b21lcnNlcnZpY2UiLCJzdWIiOiJubkBhZm9yby5haSIsIm9yZ0lkIjoyNywic3RhdHVzIjoiQUNUSVZFIiwiaWF0IjoxNzY3MTc3OTkyLCJleHAiOjE3Njc3ODI3OTJ9.-Cg5fNlwU8HWGXwlh704eNlJvoa0oZeSVQstG6t4J64"
KONNECT_TOKEN="kpat_cQzpAsDs6bjGT2fo0awV9yawAaGBr1x65ifX7qyLUUSFelQdA"
CONTROL_PLANE_ID="154d960b-f4c3-408c-b356-95fcbed64c5b"

echo "========================================="
echo "Kong Konnect Integration Tests"
echo "========================================="
echo ""

echo "Test 1: Create Connection"
echo "-------------------------"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$BASE_URL/api/integrations/konnect/connection" \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: $ORG_ID" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d "{
    \"name\": \"Konnect India - aforo-dev\",
    \"description\": \"Kong Konnect India region\",
    \"baseUrl\": \"https://in.api.konghq.com\",
    \"authToken\": \"$KONNECT_TOKEN\",
    \"controlPlaneId\": \"$CONTROL_PLANE_ID\",
    \"region\": \"in\"
  }")

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$RESPONSE" | sed '/HTTP_CODE/d')

echo "HTTP Status: $HTTP_CODE"
echo "Response: $BODY" | jq . 2>/dev/null || echo "$BODY"
echo ""

if [ "$HTTP_CODE" != "200" ]; then
    echo "❌ Test 1 FAILED"
    echo "Checking application logs..."
    exit 1
else
    echo "✅ Test 1 PASSED"
fi

echo ""
echo "Test 2: Test Connection"
echo "-------------------------"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "$BASE_URL/api/integrations/konnect/connection/test" \
  -H "X-Organization-Id: $ORG_ID" \
  -H "Authorization: Bearer $JWT_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$RESPONSE" | sed '/HTTP_CODE/d')

echo "HTTP Status: $HTTP_CODE"
echo "Response: $BODY" | jq . 2>/dev/null || echo "$BODY"
echo ""

if [ "$HTTP_CODE" != "200" ]; then
    echo "❌ Test 2 FAILED"
else
    echo "✅ Test 2 PASSED"
fi

echo ""
echo "Test 3: Fetch API Products"
echo "-------------------------"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "$BASE_URL/api/integrations/konnect/api-products" \
  -H "X-Organization-Id: $ORG_ID" \
  -H "Authorization: Bearer $JWT_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$RESPONSE" | sed '/HTTP_CODE/d')

echo "HTTP Status: $HTTP_CODE"
echo "Response: $BODY" | jq . 2>/dev/null || echo "$BODY"
echo ""

if [ "$HTTP_CODE" != "200" ]; then
    echo "❌ Test 3 FAILED"
else
    echo "✅ Test 3 PASSED"
fi

echo ""
echo "========================================="
echo "Test Summary"
echo "========================================="
