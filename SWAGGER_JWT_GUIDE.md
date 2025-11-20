# 🔐 Swagger UI with JWT Authentication - Guide

## ✅ JWT Authentication Now Enabled in Swagger UI

I've added OpenAPI configuration to enable JWT authentication in Swagger UI.

---

## 🎯 How to Use JWT in Swagger UI

### **Step 1: Open Swagger UI**

```bash
open http://localhost:8086/swagger-ui.html
```

Or navigate to: http://localhost:8086/swagger-ui.html

---

### **Step 2: Look for the "Authorize" Button**

You should now see:
- **🔓 Authorize** button at the top right of the Swagger UI
- A lock icon 🔒 next to each protected endpoint

---

### **Step 3: Click "Authorize" Button**

1. Click the **Authorize** button (top right)
2. A dialog will appear titled **"Available authorizations"**
3. You'll see **bearerAuth (http, Bearer)**

---

### **Step 4: Enter Your JWT Token**

1. In the **Value** field, paste your JWT token
2. **Important:** Enter ONLY the token, WITHOUT the "Bearer " prefix
3. Click **Authorize**
4. Click **Close**

**Example:**
```
❌ Wrong: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
✅ Correct: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

### **Step 5: Test Protected Endpoints**

Now all protected endpoints will automatically include your JWT token:

1. Expand any endpoint (e.g., `GET /api/integrations/apigee/products`)
2. Click **Try it out**
3. Click **Execute**
4. The request will automatically include: `Authorization: Bearer <your-token>`

---

## 🔑 Generate a Test JWT Token

### **Quick Test Token (Valid for 1 year)**

Use this online tool: https://jwt.io

**Configuration:**
1. **Algorithm:** HS256
2. **Payload:**
```json
{
  "sub": "test@example.com",
  "organizationId": 1,
  "exp": 1735689600
}
```
3. **Secret:** 
```
your-256-bit-secret-key-for-jwt-token-signing-minimum-32-characters-required
```
4. Copy the generated token from the left side

---

## 📸 Visual Guide

### **Before Authorization:**
- Endpoints show 🔓 (unlocked) icon
- Requests will fail with 401 Unauthorized

### **After Authorization:**
- Endpoints show 🔒 (locked) icon
- Requests will succeed with your JWT token

---

## 🧪 Test It Now

### **1. Refresh Swagger UI**
```bash
open http://localhost:8086/swagger-ui.html
```

### **2. You Should See:**
- **Authorize** button at the top right
- Lock icons next to protected endpoints
- Description mentioning JWT authentication

### **3. Generate JWT Token**
Go to https://jwt.io and create a token with:
```json
{
  "sub": "test@example.com",
  "organizationId": 1,
  "exp": 1735689600
}
```

### **4. Authorize in Swagger**
1. Click **Authorize**
2. Paste token (without "Bearer ")
3. Click **Authorize**
4. Click **Close**

### **5. Test Endpoint**
1. Try `GET /api/integrations/apigee/products`
2. Click **Try it out**
3. Click **Execute**
4. Should return 200 OK with products

---

## 🔓 Public Endpoints (No JWT Required)

These endpoints work without JWT:
- `POST /api/integrations/apigee/webhooks/usage` - HMAC only
- All Swagger UI pages
- `/actuator/health`

---

## ❌ Troubleshooting

### **Issue: "Authorize" button not visible**

**Solution:**
1. Clear browser cache
2. Hard refresh: `Cmd + Shift + R` (Mac) or `Ctrl + Shift + R` (Windows)
3. Restart application if needed

### **Issue: Still getting 401 after authorization**

**Possible causes:**
1. Token expired (check `exp` claim)
2. Wrong secret used to generate token
3. Token missing `organizationId` claim

**Solution:**
Generate new token with correct secret and claims

### **Issue: Lock icons not showing**

**Cause:** OpenAPI config not loaded

**Solution:**
1. Restart application
2. Check logs for errors
3. Verify `OpenApiConfig.java` is in classpath

---

## 📝 Sample JWT Tokens for Testing

### **Token 1: Valid for 1 year (Org 1)**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwib3JnYW5pemF0aW9uSWQiOjEsImV4cCI6MTczNTY4OTYwMH0.SIGNATURE_HERE
```

**To generate actual token:**
1. Go to https://jwt.io
2. Use payload above
3. Use your JWT secret
4. Copy the complete token

---

## ✅ Summary

**What Changed:**
- ✅ Added `OpenApiConfig.java`
- ✅ Configured JWT security scheme in OpenAPI
- ✅ **Authorize** button now visible in Swagger UI
- ✅ Lock icons show on protected endpoints
- ✅ Easy JWT token input

**How to Use:**
1. Open Swagger UI
2. Click **Authorize**
3. Paste JWT token
4. Test endpoints

**Next:**
- Generate your JWT token
- Authorize in Swagger UI
- Test all endpoints

🎉 **Swagger UI now supports JWT authentication!**
