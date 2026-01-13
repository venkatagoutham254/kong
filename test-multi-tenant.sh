#!/bin/bash

# JWT Tokens
TOKEN_ORG_9="eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhZm9yby1jdXN0b21lcnNlcnZpY2UiLCJzdWIiOiJnb3d0QGFmb3JvLmFpIiwib3JnSWQiOjksInN0YXR1cyI6IkFDVElWRSIsImlhdCI6MTc2NDE0MDE2MCwiZXhwIjoxNzY0NzQ0OTYwfQ.CDWJeva51XKil4OvSA2dNxj0D_i7yctMEwZWbjnSrOg"
TOKEN_ORG_4="eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhZm9yby1jdXN0b21lcnNlcnZpY2UiLCJzdWIiOiJ0aGlua19tb3VudGFpbl9hZm9yb0BhaS5haSIsIm9yZ0lkIjo0LCJzdGF0dXMiOiJBQ1RJVkUiLCJpYXQiOjE3NjQ3Mzc2MTMsImV4cCI6MTc2NTM0MjQxM30.9xPYzkiu30U9EiLBSHlROVdbQt2P1Ef_bCCgiVySLwU"

BASE_URL="http://localhost:8086"

echo "=========================================="
echo "Multi-Tenant Security Test"
echo "=========================================="
echo ""

# Test 1: Try to access without token (should fail with 401)
echo "Test 1: Access /products without JWT token (should fail)"
echo "---"
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X GET "$BASE_URL/api/integrations/apigee/products"
echo ""
echo ""

# Test 2: Create connection for Organization 9
echo "Test 2: Create Apigee connection for Organization 9"
echo "---"
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X POST "$BASE_URL/api/integrations/apigee/connections" \
  -H "Authorization: Bearer $TOKEN_ORG_9" \
  -F "org=org9-apigee" \
  -F "envs=dev,prod" \
  -F "analyticsMode=STANDARD" \
  -F "serviceAccountFile=@/dev/null"
echo ""
echo ""

# Test 3: Create connection for Organization 4
echo "Test 3: Create Apigee connection for Organization 4"
echo "---"
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X POST "$BASE_URL/api/integrations/apigee/connections" \
  -H "Authorization: Bearer $TOKEN_ORG_4" \
  -F "org=org4-apigee" \
  -F "envs=staging,production" \
  -F "analyticsMode=STANDARD" \
  -F "serviceAccountFile=@/dev/null"
echo ""
echo ""

# Test 4: Try to list products for Org 9 (should work if connection exists)
echo "Test 4: List products for Organization 9"
echo "---"
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X GET "$BASE_URL/api/integrations/apigee/products" \
  -H "Authorization: Bearer $TOKEN_ORG_9"
echo ""
echo ""

# Test 5: Try to list products for Org 4 (should work if connection exists)
echo "Test 5: List products for Organization 4"
echo "---"
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X GET "$BASE_URL/api/integrations/apigee/products" \
  -H "Authorization: Bearer $TOKEN_ORG_4"
echo ""
echo ""

# Test 6: Try to access Org 4's data with Org 9's token (should fail or return empty)
echo "Test 6: Try to access with wrong org parameter (should be ignored)"
echo "---"
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X GET "$BASE_URL/api/integrations/apigee/products?org=org4-apigee" \
  -H "Authorization: Bearer $TOKEN_ORG_9"
echo ""
echo ""

# Test 7: Verify database isolation
echo "Test 7: Check database - connection_configs table"
echo "---"
docker exec postgres psql -U root -d kong -c "SELECT id, org, organization_id FROM connection_configs ORDER BY organization_id;"
echo ""

echo "=========================================="
echo "Test Complete"
echo "=========================================="
