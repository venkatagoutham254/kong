# ✅ Fresh Start Complete - Clean Environment

## 🎯 What Was Done

### **1. Stopped and Removed Docker Containers**
```bash
✅ Stopped postgres container
✅ Removed postgres container
✅ Removed Docker volume: apigee_pgdata_apigee
```

### **2. Cleaned Application**
```bash
✅ Stopped application on port 8086
✅ Ran mvn clean
✅ Removed all build artifacts
```

### **3. Started Fresh PostgreSQL**
```bash
✅ Started new postgres:15 container
✅ Port: 5432
✅ Database: aforo_apigee
✅ User: venkatagowtham
✅ Password: postgres
✅ Container name: postgres
```

### **4. Built and Started Application**
```bash
✅ mvn clean install -DskipTests
✅ mvn spring-boot:run
✅ Application running on port 8086
✅ Fresh database with clean schema
```

---

## 🚀 Current Status

### **Services Running:**

| Service | Port | Status | Database |
|---------|------|--------|----------|
| Apigee Integration Service | 8086 | ✅ Running | Fresh PostgreSQL |
| PostgreSQL | 5432 | ✅ Running | Clean database |

### **Database:**
- **Host:** localhost
- **Port:** 5432
- **Database:** aforo_apigee
- **User:** venkatagowtham
- **Password:** postgres
- **Status:** ✅ Fresh, clean database with no old data

---

## 🧪 Quick Tests

### **Test 1: Health Check**
```bash
curl http://localhost:8086/actuator/health

# Expected: {"status":"UP"}
```

### **Test 2: Swagger UI**
```bash
open http://localhost:8086/swagger-ui.html

# Expected: Swagger UI loads
```

### **Test 3: Database Connection**
```bash
docker exec postgres psql -U venkatagowtham -d aforo_apigee -c "\dt"

# Expected: List of tables (created by Hibernate)
```

### **Test 4: Fetch Products (Requires JWT)**
```bash
# This will fail with 401 because JWT is now required
curl http://localhost:8086/api/integrations/apigee/products

# Expected: 401 Unauthorized
```

### **Test 5: Fetch Products with JWT**
```bash
# Generate JWT token first, then:
JWT_TOKEN="your-token-here"

curl http://localhost:8086/api/integrations/apigee/products \
  -H "Authorization: Bearer $JWT_TOKEN"

# Expected: 200 OK with products
```

---

## 📊 What's Different Now

### **Before:**
- Old database with existing data
- Potentially corrupted state
- Old build artifacts

### **After:**
- ✅ Fresh PostgreSQL database
- ✅ Clean schema (created by Hibernate)
- ✅ No old data
- ✅ Fresh build
- ✅ JWT authentication enabled
- ✅ Multi-tenant support active

---

## 🔐 Security Status

### **JWT Authentication:**
- ✅ **Enabled** for all API endpoints
- ✅ Requires `Authorization: Bearer <token>` header
- ✅ Token must contain `organizationId` claim
- ✅ Multi-tenant isolation active

### **Public Endpoints (No JWT):**
- ✅ `/swagger-ui/**` - API documentation
- ✅ `/api-docs/**` - OpenAPI specs
- ✅ `/actuator/health` - Health check
- ✅ `/api/integrations/apigee/webhooks/**` - HMAC only

### **Protected Endpoints (JWT Required):**
- 🔒 `GET /api/integrations/apigee/products`
- 🔒 `POST /api/integrations/apigee/sync`
- 🔒 `GET /api/integrations/apigee/developers`
- 🔒 `POST /api/integrations/apigee/connections`
- 🔒 All other `/api/integrations/apigee/**` endpoints

---

## 📝 Database Schema

### **Tables Created by Hibernate:**

```sql
-- Connection configurations
connection_config (
  id BIGSERIAL PRIMARY KEY,
  org VARCHAR(255),
  project_id VARCHAR(255),
  sa_json_path TEXT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)

-- Imported products from Apigee
imported_product (
  id BIGSERIAL PRIMARY KEY,
  apigee_name VARCHAR(255),
  display_name VARCHAR(255),
  quota VARCHAR(50),
  resources_json TEXT,
  status VARCHAR(50),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)

-- Developer links
developer_link (
  id BIGSERIAL PRIMARY KEY,
  apigee_developer_id VARCHAR(255),
  email VARCHAR(255),
  aforo_customer_id VARCHAR(255),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)

-- Subscription mappings
subscription_mapping (
  id BIGSERIAL PRIMARY KEY,
  apigee_app_id VARCHAR(255),
  apigee_developer_id VARCHAR(255),
  aforo_subscription_id BIGINT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)
```

---

## 🔄 Next Steps

### **1. Generate JWT Token for Testing**

**Option A: Use jwt.io**
1. Go to https://jwt.io
2. Algorithm: HS256
3. Payload:
```json
{
  "sub": "test@example.com",
  "organizationId": 1,
  "exp": 1735689600
}
```
4. Secret: `your-256-bit-secret-key-for-jwt-token-signing-minimum-32-characters-required`
5. Copy token

**Option B: Use Java Code**
```java
String token = Jwts.builder()
    .subject("test@example.com")
    .claim("organizationId", 1)
    .expiration(new Date(System.currentTimeMillis() + 86400000))
    .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
    .compact();
```

### **2. Test All Endpoints**

```bash
# Set your JWT token
export JWT_TOKEN="your-generated-token"

# Test products
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8086/api/integrations/apigee/products

# Test sync
curl -X POST \
  -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8086/api/integrations/apigee/sync

# Test developers
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8086/api/integrations/apigee/developers

# Test connections
curl -X POST \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"org":"aforo-aadhaar-477607","projectId":"aforo-aadhaar-477607"}' \
  http://localhost:8086/api/integrations/apigee/connections
```

### **3. Verify Database**

```bash
# Connect to database
docker exec -it postgres psql -U venkatagowtham -d aforo_apigee

# Check tables
\dt

# Check data
SELECT * FROM connection_config;
SELECT * FROM imported_product;
SELECT * FROM developer_link;
SELECT * FROM subscription_mapping;

# Exit
\q
```

---

## 🐛 Troubleshooting

### **Issue: Application won't start**

**Check logs:**
```bash
# View application logs
tail -f logs/application.log

# Or check terminal output
```

**Common causes:**
- Database not ready
- Port 8086 already in use
- Missing JWT secret

### **Issue: Database connection failed**

**Check PostgreSQL:**
```bash
# Check if container is running
docker ps | grep postgres

# Check PostgreSQL logs
docker logs postgres

# Test connection
docker exec postgres pg_isready -U venkatagowtham
```

### **Issue: 401 Unauthorized on all endpoints**

**Cause:** JWT authentication is now enabled

**Solution:**
1. Generate JWT token (see above)
2. Include in Authorization header
3. Ensure token contains `organizationId` claim

### **Issue: Tables not created**

**Cause:** Hibernate DDL auto-update might have failed

**Solution:**
```bash
# Check application logs for Hibernate errors
grep -i "hibernate" logs/application.log

# Manually create tables if needed
docker exec -it postgres psql -U venkatagowtham -d aforo_apigee -f schema.sql
```

---

## 📞 Quick Reference

### **Docker Commands:**

```bash
# View running containers
docker ps

# View logs
docker logs postgres
docker logs -f postgres  # Follow logs

# Stop container
docker stop postgres

# Remove container
docker rm postgres

# Remove volume
docker volume rm apigee_pgdata_apigee

# Restart container
docker restart postgres
```

### **Application Commands:**

```bash
# Stop application
lsof -ti:8086 | xargs kill -9

# Clean build
mvn clean

# Build without tests
mvn clean install -DskipTests

# Run application
mvn spring-boot:run

# Run in background
nohup mvn spring-boot:run > app.log 2>&1 &
```

### **Database Commands:**

```bash
# Connect to database
docker exec -it postgres psql -U venkatagowtham -d aforo_apigee

# Run SQL file
docker exec -i postgres psql -U venkatagowtham -d aforo_apigee < schema.sql

# Backup database
docker exec postgres pg_dump -U venkatagowtham aforo_apigee > backup.sql

# Restore database
docker exec -i postgres psql -U venkatagowtham -d aforo_apigee < backup.sql
```

---

## ✅ Summary

**Environment Status:**
- ✅ Fresh PostgreSQL database running
- ✅ Clean application build
- ✅ Application running on port 8086
- ✅ JWT authentication enabled
- ✅ Multi-tenant support active
- ✅ All old data removed
- ✅ Ready for testing

**What Changed:**
- 🔄 Database completely reset
- 🔄 All old data removed
- 🔄 Fresh schema created
- 🔄 JWT authentication now required
- 🔄 Multi-tenant isolation active

**Next Actions:**
1. Generate JWT token
2. Test endpoints with JWT
3. Verify database operations
4. Test sync functionality

🎉 **Fresh environment is ready!**
