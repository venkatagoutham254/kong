# Apigee Integration Endpoints in Swagger

After the fix, these Apigee endpoints should appear in Swagger UI under the **"Apigee Integration"** tag:

## 🔗 Connection Management
- **POST** `/api/integrations/apigee/connections` - Save and test Apigee connection with file upload

## 📦 Product Management
- **GET** `/api/integrations/apigee/products` - List Apigee API Products
- **POST** `/api/integrations/apigee/products/import-selected` - Import selected products with assigned types
- **POST** `/api/integrations/apigee/sync` - Sync products from Apigee to ProductRatePlanService

## 👥 Developer Management
- **GET** `/api/integrations/apigee/developers` - List Apigee Developers
- **GET** `/api/integrations/apigee/developers/{developerId}/apps` - List Apps for a Developer
- **POST** `/api/integrations/apigee/developers/{developerId}/link` - Link Apigee Developer to Aforo Customer

## 🔐 Authorization & Mapping
- **POST** `/api/integrations/apigee/mappings/subscriptions` - Create DRAFT Subscription for (App × Product)
- **POST** `/api/integrations/apigee/authorize` - Authorization decision for runtime

## 📊 Usage & Webhooks
- **POST** `/api/integrations/apigee/webhooks/usage` - Ingest usage from Apigee (HMAC verified)

## 👤 Customer Sync
- **POST** `/api/integrations/apigee/customers/sync` - Sync customers/developers from Apigee to Customer Service

---

## ✅ Fix Applied

Added the following configuration to `application.yml`:

```yaml
springdoc:
  packages-to-scan: aforo.kong.controller
```

This ensures that Springdoc scans the `aforo.kong.controller` package where `ApigeeIntegrationController` is located.

## 🚀 After Deployment

Once the code is deployed to AWS, refresh the Swagger UI at:
- **AWS Swagger URL**: http://44.203.209.2:8086/swagger-ui/index.html

You should now see the **"Apigee Integration"** section with all the endpoints listed above.
