# Product Import Changes - Selective Import with Product Types

## Overview
Updated the Apigee integration to support selective product import with product type assignment. Products are no longer automatically imported when fetched from Apigee.

## Key Changes

### 1. Product Type Enum (NEW)
**File:** `src/main/java/com/aforo/apigee/dto/ProductType.java`

Created enum with 4 product types:
- **API** - For API-based products
- **FlatFile** - For file-based products  
- **SQLResult** - For SQL/database products
- **LLMToken** - For LLM token-based products

### 2. Selective Import Request (NEW)
**File:** `src/main/java/com/aforo/apigee/dto/request/SelectiveProductImportRequest.java`

Request structure for selective product import:
- List of selected products
- Each product includes:
  - Product name
  - Display name
  - Product type (one of the 4 types)
  - Quota
  - Resources

### 3. Updated Product Import Request
**File:** `src/main/java/com/aforo/apigee/dto/request/ProductImportRequest.java`

Added `productType` field to support product type assignment during import.

### 4. New Selective Import Endpoint
**Endpoint:** `POST /api/integrations/apigee/products/import-selected`

- Accepts list of selected products with assigned types
- Only imports the selected products (not all)
- Returns detailed import status for each product

### 5. Updated Product Listing
**Endpoint:** `GET /api/integrations/apigee/products`

- Now only fetches and returns products
- **NO automatic import** - user must select which to import
- Returns list for user selection in UI

## API Usage

### Step 1: Fetch Available Products
```bash
GET /api/integrations/apigee/products
```

Returns list of all available Apigee products for selection.

### Step 2: Import Selected Products
```bash
POST /api/integrations/apigee/products/import-selected
Headers:
  X-Organization-Id: 1
  Content-Type: application/json

Body:
{
  "selectedProducts": [
    {
      "productName": "api-product-1",
      "displayName": "API Product One",
      "productType": "API",
      "quota": "1000",
      "resources": ["/api/v1/*"]
    },
    {
      "productName": "file-product-2",
      "displayName": "File Product Two", 
      "productType": "FlatFile",
      "quota": "5000",
      "resources": ["/files/*"]
    }
  ]
}
```

### Response
```json
{
  "totalSelected": 2,
  "successfullyImported": 2,
  "failed": 0,
  "message": "Import completed: 2 successful, 0 failed out of 2 selected",
  "importedProducts": [
    {
      "productName": "api-product-1",
      "productType": "API",
      "status": "SUCCESS",
      "message": "Product created successfully",
      "productId": 123
    },
    {
      "productName": "file-product-2",
      "productType": "FlatFile",
      "status": "SUCCESS",
      "message": "Product created successfully",
      "productId": 124
    }
  ]
}
```

## UI Flow

1. **Connection Screen** - User enters Apigee credentials and tests connection
2. **Product Selection Screen** - Shows all available products with checkboxes for selection
3. **Type Assignment Screen** - For each selected product, user chooses from 4 product types dropdown
4. **Import Confirmation** - Shows selected products with types and imports them
5. **Products List** - Final list showing imported products in the system

## Important Notes

- Products are **NOT** automatically imported anymore
- User must explicitly select products and assign types
- Only 4 product types available (API, FlatFile, SQLResult, LLMToken)
- Each product must have a type assigned before import
- Import is transactional per product (success/failure tracked individually)
