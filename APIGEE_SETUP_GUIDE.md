# 🚀 Apigee Account Setup Guide - Step by Step

## 📋 Complete Guide to Prepare Your Apigee Account for Testing

This guide will walk you through setting up your Apigee account step-by-step before testing the integration APIs.

---

## 🎯 Prerequisites

- Apigee account (Edge or X/Hybrid)
- Admin access to Apigee organization
- Access to Google Cloud Console (for Apigee X) or Apigee Edge UI

---

## 📝 Step-by-Step Setup

### ✅ STEP 1: Login to Apigee

#### For Apigee Edge:
1. Go to: https://apigee.com/edge
2. Login with your credentials
3. Select your organization

#### For Apigee X:
1. Go to: https://console.cloud.google.com/apigee
2. Login with your Google Cloud account
3. Select your project

**✅ Verify**: You can see the Apigee dashboard

---

### ✅ STEP 2: Note Your Organization Details

#### Find Your Organization Name:
1. In Apigee UI, look at the top-left corner
2. Note down the **Organization Name** (e.g., `acme-corp`)

#### Find Your Environment:
1. Go to **Admin** → **Environments**
2. Note down environment names (e.g., `test`, `prod`)
3. **Choose one** for testing (recommend `test` or `dev`)

**📝 Write Down:**
```
Organization: ___________________
Environment: ___________________
```

---

### ✅ STEP 3: Create API Proxies (If Not Already Present)

#### Create a Test API Proxy:

1. **Go to**: Develop → API Proxies
2. **Click**: "+ Proxy" or "Create New"
3. **Select**: "Reverse proxy"
4. **Fill in**:
   - **Proxy Name**: `test-payment-api`
   - **Proxy Base Path**: `/payments`
   - **Target URL**: `https://httpbin.org` (for testing)
5. **Click**: Next → Next → Create

#### Create Another Proxy (Optional):
Repeat above with:
- **Proxy Name**: `test-user-api`
- **Proxy Base Path**: `/users`
- **Target URL**: `https://httpbin.org`

**✅ Verify**: You can see proxies in the list

**📝 Note Down Proxy Names:**
```
Proxy 1: ___________________
Proxy 2: ___________________
```

---

### ✅ STEP 4: Create API Products

API Products group your APIs for monetization.

#### Create First API Product:

1. **Go to**: Publish → API Products
2. **Click**: "+ API Product" or "Create"
3. **Fill in**:
   - **Name**: `BRONZE_PRODUCT`
   - **Display Name**: `Bronze Plan`
   - **Description**: `Basic API access - 1000 calls/day`
   - **Environment**: Select your environment (e.g., `test`)
   - **Access**: Public or Private
   - **Quota**: 
     - Limit: `1000`
     - Interval: `1`
     - Time Unit: `day`
4. **Add API Proxies**:
   - Click "Add a proxy"
   - Select `test-payment-api`
   - Add path: `/` (or leave blank for all paths)
5. **Click**: Save

#### Create More API Products:

Repeat above for:

**Silver Product:**
- Name: `SILVER_PRODUCT`
- Display Name: `Silver Plan`
- Quota: `5000` calls/day
- Add same proxies

**Gold Product:**
- Name: `GOLD_PRODUCT`
- Display Name: `Gold Plan`
- Quota: `10000` calls/day
- Add same proxies

**✅ Verify**: You can see 3 API Products in the list

**📝 Note Down Product Names:**
```
Product 1: ___________________
Product 2: ___________________
Product 3: ___________________
```

---

### ✅ STEP 5: Create Developers

Developers are the API consumers.

#### Create First Developer:

1. **Go to**: Publish → Developers
2. **Click**: "+ Developer" or "Create"
3. **Fill in**:
   - **First Name**: `John`
   - **Last Name**: `Doe`
   - **Email**: `john.doe@example.com` (use your email)
   - **Username**: `john.doe`
4. **Click**: Create

#### Create More Developers (Optional):

Repeat for:
- Email: `jane.smith@example.com`
- Username: `jane.smith`

**✅ Verify**: You can see developers in the list

**📝 Note Down Developer Emails:**
```
Developer 1: ___________________
Developer 2: ___________________
```

---

### ✅ STEP 6: Create Developer Apps

Apps are how developers access your APIs.

#### Create First App:

1. **Go to**: Publish → Apps
2. **Click**: "+ App" or "Create"
3. **Fill in**:
   - **Name**: `mobile-app`
   - **Developer**: Select `john.doe@example.com`
   - **Callback URL**: `https://example.com/callback` (optional)
4. **Add Products**:
   - Click "Add product"
   - Select `BRONZE_PRODUCT`
   - Click "Add"
5. **Click**: Create

#### Get API Key (Consumer Key):

1. After creating the app, you'll see **Credentials** section
2. Click "Show" next to **Key** (Consumer Key)
3. **Copy the Key** - This is your API Key!

**📝 Note Down:**
```
App Name: ___________________
Consumer Key: ___________________
Consumer Secret: ___________________
```

#### Create More Apps (Optional):

Repeat for:
- App Name: `web-app`
- Developer: Same or different
- Product: `SILVER_PRODUCT`

**✅ Verify**: You can see apps in the list with API keys

---

### ✅ STEP 7: Get Management API Token

You need a token to access Apigee Management API.

#### For Apigee Edge:

1. **Go to**: Account → My Profile
2. **Find**: "OAuth Tokens" or "Personal Access Tokens"
3. **Click**: "Generate Token" or "Create Token"
4. **Copy the token** - This is your Bearer token!

#### For Apigee X (Google Cloud):

1. **Go to**: Google Cloud Console
2. **Open**: Cloud Shell (top right)
3. **Run**:
```bash
gcloud auth print-access-token
```
4. **Copy the output** - This is your Bearer token!

**Or create a Service Account:**
1. Go to: IAM & Admin → Service Accounts
2. Create service account with "Apigee Admin" role
3. Create and download JSON key
4. Use this for authentication

**📝 Note Down:**
```
Token Type: (OAuth / Service Account)
Token: ___________________
```

**⚠️ IMPORTANT**: Keep this token secure! Don't share it.

---

### ✅ STEP 8: Test Your API Proxy

Before integrating with Aforo, test that your proxy works.

#### Test with curl:

```bash
# Replace with your values
ORG="your-org"
ENV="test"
API_KEY="your-consumer-key"

# For Apigee Edge:
curl -H "x-api-key: $API_KEY" \
  https://$ORG-$ENV.apigee.net/payments/get

# For Apigee X:
curl -H "x-api-key: $API_KEY" \
  https://your-domain/payments/get
```

**✅ Expected**: You should get a 200 response

---

### ✅ STEP 9: Prepare Configuration for Aforo

Now collect all the information you need for Aforo integration.

**📝 Fill This Template:**

```yaml
# Apigee Configuration for Aforo Integration

Organization: ___________________
Environment: ___________________
Management API URL: 
  # Edge: https://api.enterprise.apigee.com/v1
  # X: https://apigee.googleapis.com/v1
Bearer Token: ___________________

# Test Data
API Proxies:
  - Name: ___________________
    Base Path: ___________________
  - Name: ___________________
    Base Path: ___________________

API Products:
  - Name: ___________________
    Quota: ___ calls/day
  - Name: ___________________
    Quota: ___ calls/day

Developers:
  - Email: ___________________
  - Email: ___________________

Developer Apps:
  - Name: ___________________
    Developer: ___________________
    Consumer Key: ___________________
    Product: ___________________
```

---

## 🧪 Now You're Ready to Test Aforo Integration!

### Next Steps:

1. **Set Environment Variables**:
```bash
export APIGEE_ORG="your-org"
export APIGEE_ENV="test"
export APIGEE_TOKEN="your-bearer-token"
```

2. **Start Aforo Application**:
```bash
# Start database first
docker-compose up -d postgres

# Start application
mvn spring-boot:run
```

3. **Open Swagger**:
```
http://localhost:8086/swagger-ui.html
```

4. **Follow Testing Guide** (see below)

---

## 🎯 Testing Guide - Step by Step

### TEST 1: Health Check

**Endpoint**: `GET /integrations/apigee/health`

**In Swagger**:
1. Find "Apigee Integration" section
2. Click on `GET /integrations/apigee/health`
3. Click "Try it out"
4. Click "Execute"

**Expected Response**:
```json
{
  "apigeeReachable": true,
  "org": "your-org",
  "env": "test"
}
```

✅ **If you see this**: Aforo can connect to Apigee!

---

### TEST 2: Connect to Apigee

**Endpoint**: `POST /integrations/apigee/connect`

**In Swagger**:
1. Click "Authorize" button (top right)
2. Paste your JWT token
3. Find `POST /integrations/apigee/connect`
4. Click "Try it out"
5. **Replace the JSON** with:

```json
{
  "org": "YOUR_ORG_NAME",
  "env": "YOUR_ENV_NAME"
}
```

6. Click "Execute"

**Expected Response**:
```json
{
  "status": "connected",
  "org": "your-org",
  "env": "test",
  "apiProxyCount": 2,
  "apiProductCount": 3,
  "developerCount": 1,
  "appCount": 1
}
```

✅ **If you see counts**: Aforo successfully connected and counted your resources!

---

### TEST 3: Sync Catalog

**Endpoint**: `POST /integrations/apigee/catalog/sync`

**In Swagger**:
1. Find `POST /integrations/apigee/catalog/sync`
2. Click "Try it out"
3. Leave syncType as default or enter "full"
4. Click "Execute"

**Expected Response**:
```json
{
  "status": "COMPLETED",
  "productsImported": 3,
  "endpointsImported": 2,
  "customersImported": 1,
  "appsImported": 1,
  "syncStartTime": "2025-12-05T10:00:00Z",
  "syncEndTime": "2025-12-05T10:00:15Z",
  "durationMs": 15000
}
```

✅ **If you see imported counts**: Your Apigee data is now in Aforo!

---

### TEST 4: Ingest Usage Event

**Endpoint**: `POST /integrations/apigee/ingest`

**In Swagger**:
1. Find `POST /integrations/apigee/ingest`
2. Click "Try it out"
3. **Replace the JSON** with your real data:

```json
{
  "timestamp": "2025-12-05T10:00:00Z",
  "org": "YOUR_ORG",
  "env": "YOUR_ENV",
  "apiProxy": "test-payment-api",
  "proxyBasepath": "/payments",
  "resourcePath": "/charge",
  "method": "POST",
  "status": 200,
  "latencyMs": 150,
  "developerId": "john.doe@example.com",
  "appName": "mobile-app",
  "apiProduct": "BRONZE_PRODUCT",
  "apiKey": "YOUR_CONSUMER_KEY",
  "requestSize": 1024,
  "responseSize": 2048
}
```

4. Click "Execute"

**Expected Response**:
```json
{
  "status": "accepted",
  "eventsProcessed": 1
}
```

✅ **If you see "accepted"**: Usage event recorded for billing!

---

### TEST 5: Enforce Plan

**Endpoint**: `POST /integrations/apigee/enforce/plans`

**In Swagger**:
1. Find `POST /integrations/apigee/enforce/plans`
2. Click "Try it out"
3. **Replace the JSON**:

```json
{
  "mappings": [
    {
      "planId": "SILVER",
      "developerId": "john.doe@example.com",
      "appName": "mobile-app",
      "consumerKey": "YOUR_CONSUMER_KEY",
      "apiProductName": "SILVER_PRODUCT"
    }
  ]
}
```

4. Click "Execute"

**Expected Response**:
```json
{
  "status": "success",
  "results": [
    {
      "planId": "SILVER",
      "developerId": "john.doe@example.com",
      "appName": "mobile-app",
      "status": "success"
    }
  ]
}
```

✅ **If you see "success"**: Plan enforced! App now has SILVER_PRODUCT access!

---

### TEST 6: Suspend App

**Endpoint**: `POST /integrations/apigee/suspend`

**In Swagger**:
1. Find `POST /integrations/apigee/suspend`
2. Click "Try it out"
3. **Replace the JSON**:

```json
{
  "developerId": "john.doe@example.com",
  "appName": "mobile-app",
  "consumerKey": "YOUR_CONSUMER_KEY",
  "mode": "revoke",
  "reason": "Demo: Testing suspension"
}
```

4. Click "Execute"

**Expected Response**:
```json
{
  "status": "suspended",
  "developerId": "john.doe@example.com",
  "appName": "mobile-app",
  "mode": "revoke"
}
```

✅ **If you see "suspended"**: App is now suspended! API calls will be blocked!

**⚠️ Verify in Apigee**:
1. Go to Publish → Apps
2. Find "mobile-app"
3. Check credentials - should show "Revoked"

---

### TEST 7: Resume App

**Endpoint**: `POST /integrations/apigee/resume`

**In Swagger**:
1. Find `POST /integrations/apigee/resume`
2. Click "Try it out"
3. **Fill in parameters**:
   - developerId: `john.doe@example.com`
   - appName: `mobile-app`
   - consumerKey: `YOUR_CONSUMER_KEY`
4. Click "Execute"

**Expected Response**:
```json
{
  "status": "resumed",
  "developerId": "john.doe@example.com",
  "appName": "mobile-app"
}
```

✅ **If you see "resumed"**: App is active again! API calls will work!

**⚠️ Verify in Apigee**:
1. Go to Publish → Apps
2. Find "mobile-app"
3. Check credentials - should show "Approved"

---

## 📊 Verification Checklist

After all tests, verify in Apigee:

- [ ] API Proxies are visible in Apigee
- [ ] API Products are configured with quotas
- [ ] Developers are created
- [ ] Apps are created with API keys
- [ ] Suspended app shows "Revoked" status
- [ ] Resumed app shows "Approved" status
- [ ] Plan enforcement added product to app

---

## 🎯 Success Criteria

✅ **All tests pass** if you see:
1. Health check returns `apigeeReachable: true`
2. Connect returns resource counts
3. Catalog sync imports your data
4. Usage ingestion accepts events
5. Plan enforcement succeeds
6. Suspend changes app status to "Revoked"
7. Resume changes app status back to "Approved"

---

## 🐛 Troubleshooting

### Issue: "Authentication failed"
**Solution**: 
- Check your bearer token is correct
- For Apigee X, regenerate token: `gcloud auth print-access-token`
- Token expires after 1 hour for Apigee X

### Issue: "Organization not found"
**Solution**:
- Verify organization name (case-sensitive)
- Check you have access to the organization

### Issue: "Environment not found"
**Solution**:
- Verify environment name
- Check environment exists in Admin → Environments

### Issue: "App not found" when suspending
**Solution**:
- Run catalog sync first
- Verify app name and developer email are correct

### Issue: "Product not found" when enforcing
**Solution**:
- Run catalog sync first
- Verify API product name is correct

---

## 📝 Quick Reference

### Your Configuration:
```bash
# Set these before testing
export APIGEE_ORG="your-org"
export APIGEE_ENV="test"
export APIGEE_TOKEN="your-bearer-token"

# Start application
docker-compose up -d postgres
mvn spring-boot:run

# Open Swagger
open http://localhost:8086/swagger-ui.html
```

### Test Order:
1. Health Check
2. Connect
3. Catalog Sync
4. Ingest Event
5. Enforce Plan
6. Suspend App
7. Resume App

---

## 🎉 You're Done!

Once all tests pass, you have successfully:
- ✅ Set up Apigee account
- ✅ Created test data
- ✅ Connected Aforo to Apigee
- ✅ Synced catalog
- ✅ Ingested usage
- ✅ Enforced plans
- ✅ Suspended and resumed apps

**Your Apigee × Aforo integration is working! 🚀**

---

**Need Help?**
- Check logs: `tail -f /tmp/apigee-app.log`
- Review documentation: `APIGEE_INTEGRATION_GUIDE.md`
- Test with curl: Use `test-apigee-integration.sh`
