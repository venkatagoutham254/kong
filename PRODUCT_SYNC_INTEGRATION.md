# Product Sync to ProductRatePlanService

## Implementation Summary

Successfully enhanced the Apigee Integration Service to automatically push fetched Apigee products to the ProductRatePlanService.

## Changes Made

### 1. WebClientConfig.java
Added a new WebClient bean for ProductRatePlanService:

```java
@Bean
public WebClient productRatePlanClient(WebClient.Builder builder) {
    return builder.baseUrl("http://localhost:8086/api/products").build();
}
```

### 2. InventoryServiceImpl.java

#### Added Dependencies:
- `WebClient productRatePlanClient` - Injected via constructor
- New imports: `MediaType`, `HashMap`, `Map`, `AtomicInteger`

#### Enhanced getApiProducts() Method:
1. **Maintains existing logic** - Still saves products to `importedProductRepository`
2. **Adds async push** - Calls `pushProductToRatePlanService()` for each product
3. **Tracks sync count** - Uses `AtomicInteger` to count successful syncs

#### New Method: pushProductToRatePlanService()
```java
private void pushProductToRatePlanService(ApiProductResponse product, AtomicInteger syncedCount)
```

**Request Body Format:**
```json
{
  "name": "<Apigee displayName or name>",
  "description": "Imported from Apigee",
  "source": "APIGEE",
  "externalId": "<Apigee product name>"
}
```

**Features:**
- ✅ Async execution using `.subscribe()` - doesn't block product fetching
- ✅ Graceful error handling - logs errors but continues processing
- ✅ Success/failure logging for each product
- ✅ Uses `MediaType.APPLICATION_JSON`
- ✅ Posts to `/import` endpoint

## How It Works

### Flow:
1. User calls `GET /api/integrations/apigee/products`
2. Service fetches products from Apigee
3. Each product is:
   - Saved to local database (`imported_product` table)
   - **Asynchronously** pushed to ProductRatePlanService at `http://localhost:8086/api/products/import`
4. Returns product list immediately
5. Background async calls complete and log results

### Example Logs:
```
INFO: Fetching API products from Apigee org: aforo-aadhaar-477607
INFO: Fetched and saved 2 API products. Syncing to ProductRatePlanService...
INFO: Successfully pushed Apigee product pan to ProductRatePlanService. Response: {...}
INFO: Successfully pushed Apigee product ProductAPI-Plan to ProductRatePlanService. Response: {...}
```

## Testing

### 1. Fetch Products (triggers sync):
```bash
curl http://localhost:8086/api/integrations/apigee/products
```

### 2. Verify in Logs:
Look for messages like:
- `"Syncing to ProductRatePlanService..."`
- `"Successfully pushed Apigee product X to ProductRatePlanService"`
- `"Failed to push Apigee product X..."` (if errors occur)

### 3. Check ProductRatePlanService:
```bash
# Verify products were created/updated
curl http://localhost:8086/api/products
```

## Error Handling

**Graceful Degradation:**
- If ProductRatePlanService is down → Logs error, continues processing other products
- If one product fails → Logs error, continues with remaining products
- Original functionality preserved → Products still saved to local DB

**Example Error Log:**
```
ERROR: Failed to push Apigee product pan to ProductRatePlanService: Connection refused
```

## Benefits

✅ **Non-blocking** - Uses async `.subscribe()` so API response is fast
✅ **Resilient** - Errors don't break the entire flow
✅ **Observable** - Clear logging for debugging
✅ **Maintains backwards compatibility** - All existing logic intact
✅ **Automatic sync** - No manual intervention needed

## Multi-Org Support

Works seamlessly with the org parameter:
```bash
# Different org products are also synced
curl "http://localhost:8086/api/integrations/apigee/products?org=another-org"
```

## Configuration

**ProductRatePlanService URL:**
Currently: `http://localhost:8086/api/products`

To change, update in `WebClientConfig.java`:
```java
@Bean
public WebClient productRatePlanClient(WebClient.Builder builder) {
    return builder.baseUrl("http://your-service-url/api/products").build();
}
```

## Monitoring Sync Status

The sync count is tracked but not currently returned in the API response. To add this:

**Future Enhancement:**
Modify return type to include sync status:
```java
public ApiProductSyncResponse getApiProducts(String org) {
    // ... existing code ...
    return new ApiProductSyncResponse(products, "Successfully synced " + syncedCount.get() + " products");
}
```

## Important Notes

⚠️ **Same Port**: ProductRatePlanService and Apigee Integration Service both run on port 8086. Ensure ProductRatePlanService endpoints are available.

⚠️ **Async Nature**: The sync happens in the background. Products are returned immediately, sync completes shortly after.

⚠️ **Idempotency**: The `/import` endpoint should handle duplicate products gracefully (update instead of error).

## Troubleshooting

**Products fetched but not synced?**
- Check if ProductRatePlanService is running
- Verify `/api/products/import` endpoint exists
- Check application logs for error messages

**Slow performance?**
- Async calls shouldn't slow down API
- Check ProductRatePlanService response time
- Review logs for timeout errors

**Products synced multiple times?**
- Each fetch triggers a new sync
- ProductRatePlanService `/import` should be idempotent
- Use `externalId` to prevent duplicates
