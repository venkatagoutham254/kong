# 🔄 Customer/Developer Sync Implementation Guide

## Overview
This guide implements customer/developer sync from Apigee to Customer Service, similar to product sync.

---

## Part 1: Customer Service Implementation (Port 8082)

### 📝 **Command Prompt for Customer Service Team:**

```
We need to implement external customer/developer import functionality similar to product import:

1. Add 'source' and 'externalId' fields to Customer entity
2. Create CustomerImportResponse DTO
3. Add POST /api/customers/import endpoint (permitAll - no JWT required)
4. Implement importExternalCustomer method in CustomerService
5. Add repository methods to find by externalId and source
6. Create Liquibase changelog for schema changes

Requirements:
- source: String field (default "MANUAL", can be "APIGEE", "KONG", etc.)
- externalId: String field (nullable, unique per source)
- Import endpoint should create new customer if not exists, update if exists (based on externalId + source)
- Return CustomerImportResponse with status "CREATED" or "UPDATED"
- Multi-tenant: Use organizationId from X-Organization-Id header
- Endpoint must be permitAll() for service-to-service communication
```

---

## Part 2: Customer Service Code Changes

### 1️⃣ **Update Customer Entity**

**File:** `Customer.java`

```java
@Column(name = "source", nullable = false)
@Builder.Default
private String source = "MANUAL";

@Column(name = "external_id", nullable = true)
private String externalId;
```

---

### 2️⃣ **Create CustomerImportResponse DTO**

**File:** `CustomerImportResponse.java`

```java
package [your.package].customer.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerImportResponse {
    private String message;
    private String status; // "CREATED" or "UPDATED"
    private Long customerId;
    private String customerEmail;
    private String source;
    private String externalId;
}
```

---

### 3️⃣ **Update CustomerRepository**

**File:** `CustomerRepository.java`

```java
Optional<Customer> findByExternalIdAndSourceAndOrganizationId(
    String externalId, 
    String source, 
    Long organizationId
);

Optional<Customer> findByEmailAndOrganizationId(
    String email, 
    Long organizationId
);
```

---

### 4️⃣ **Add Import Method to CustomerService**

**File:** `CustomerService.java` (Interface)

```java
CustomerImportResponse importExternalCustomer(CustomerImportRequest request);
```

**File:** `CustomerServiceImpl.java`

```java
@Override
@Transactional
public CustomerImportResponse importExternalCustomer(CustomerImportRequest request) {
    log.info("Importing customer from source [{}] with externalId [{}]", 
        request.getSource(), request.getExternalId());
    
    // Validate required fields
    if (request.getSource() == null || request.getSource().isBlank()) {
        throw new IllegalArgumentException("Source is required for import");
    }
    if (request.getExternalId() == null || request.getExternalId().isBlank()) {
        throw new IllegalArgumentException("ExternalId is required for import");
    }
    
    Long organizationId = TenantContext.getCurrentTenant();
    if (organizationId == null) {
        throw new IllegalStateException("Organization ID not found in context");
    }
    
    // Check if customer exists by externalId + source
    Optional<Customer> existingCustomer = customerRepository
        .findByExternalIdAndSourceAndOrganizationId(
            request.getExternalId(), 
            request.getSource(), 
            organizationId
        );
    
    Customer customer;
    String status;
    
    if (existingCustomer.isPresent()) {
        // Update existing customer
        customer = existingCustomer.get();
        customer.setBusinessEmail(request.getBusinessEmail());
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setCompanyName(request.getCompanyName());
        customer.setPhoneNumber(request.getPhoneNumber());
        status = "UPDATED";
        log.info("Updating existing customer [{}] from source [{}]", 
            customer.getBusinessEmail(), request.getSource());
    } else {
        // Create new customer
        customer = Customer.builder()
            .businessEmail(request.getBusinessEmail())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .companyName(request.getCompanyName())
            .phoneNumber(request.getPhoneNumber())
            .source(request.getSource())
            .externalId(request.getExternalId())
            .organizationId(organizationId)
            .status("ACTIVE")
            .build();
        status = "CREATED";
        log.info("Creating new customer [{}] from source [{}]", 
            request.getBusinessEmail(), request.getSource());
    }
    
    customer = customerRepository.save(customer);
    
    log.info("Imported customer [{}] from source [{}] with status [{}]", 
        customer.getBusinessEmail(), request.getSource(), status);
    
    return CustomerImportResponse.builder()
        .message("Customer imported successfully")
        .status(status)
        .customerId(customer.getId())
        .customerEmail(customer.getBusinessEmail())
        .source(customer.getSource())
        .externalId(customer.getExternalId())
        .build();
}
```

---

### 5️⃣ **Create CustomerImportRequest DTO**

**File:** `CustomerImportRequest.java`

```java
package [your.package].customer.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerImportRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String businessEmail;
    
    @NotBlank(message = "First name is required")
    private String firstName;
    
    private String lastName;
    
    private String companyName;
    
    private String phoneNumber;
    
    @NotBlank(message = "Source is required for import")
    private String source;
    
    @NotBlank(message = "External ID is required for import")
    private String externalId;
}
```

---

### 6️⃣ **Add Import Endpoint to CustomerController**

**File:** `CustomerController.java`

```java
@PostMapping("/import")
public ResponseEntity<CustomerImportResponse> importCustomer(
    @Valid @RequestBody CustomerImportRequest request
) {
    log.info("Import customer request received for source: {}", request.getSource());
    CustomerImportResponse response = customerService.importExternalCustomer(request);
    return ResponseEntity.ok(response);
}
```

---

### 7️⃣ **Update SecurityConfig**

**File:** `SecurityConfig.java`

```java
// Add this line in the authorizeHttpRequests section:
.requestMatchers(HttpMethod.POST, "/api/customers/import").permitAll()
```

---

### 8️⃣ **Create Liquibase Changelog**

**File:** `db/changelog/add-customer-source-externalid.yml`

```yaml
databaseChangeLog:
  - changeSet:
      id: add-source-externalid-to-customer
      author: system
      changes:
        - addColumn:
            tableName: customers
            columns:
              - column:
                  name: source
                  type: varchar(50)
                  defaultValue: "MANUAL"
                  constraints:
                    nullable: false
              - column:
                  name: external_id
                  type: varchar(255)
                  constraints:
                    nullable: true
        
        - createIndex:
            indexName: idx_customer_external_id_source_org
            tableName: customers
            columns:
              - column:
                  name: external_id
              - column:
                  name: source
              - column:
                  name: organization_id
            unique: true
```

**Update:** `db/changelog/changelog-master.yml`

```yaml
- include:
    file: db/changelog/add-customer-source-externalid.yml
```

---

## Part 3: Integration Service Implementation (Port 8086)

### 1️⃣ **Create CustomerImportRequest DTO**

**File:** `src/main/java/com/aforo/apigee/dto/request/CustomerImportRequest.java`

```java
package com.aforo.apigee.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerImportRequest {
    private String businessEmail;
    private String firstName;
    private String lastName;
    private String companyName;
    private String phoneNumber;
    private String source;
    private String externalId;
}
```

---

### 2️⃣ **Create CustomerSyncResponse DTO**

**File:** `src/main/java/com/aforo/apigee/dto/response/CustomerSyncResponse.java`

```java
package com.aforo.apigee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSyncResponse {
    private String message;
    private int totalFetched;
    private int totalCreated;
    private int totalUpdated;
    private int totalFailed;
    private List<String> errors;
}
```

---

### 3️⃣ **Create ApigeeCustomer DTO**

**File:** `src/main/java/com/aforo/apigee/dto/ApigeeCustomer.java`

```java
package com.aforo.apigee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApigeeCustomer {
    private String developerId;
    private String email;
    private String firstName;
    private String lastName;
    private String userName;
    private String organizationName;
    private String status;
}
```

---

### 4️⃣ **Update ApigeeGateway Interface**

**File:** `src/main/java/com/aforo/apigee/gateway/ApigeeGateway.java`

```java
List<ApigeeCustomer> fetchDevelopers(String org, String env);
```

---

### 5️⃣ **Update FakeApigeeGateway**

**File:** `src/main/java/com/aforo/apigee/gateway/FakeApigeeGateway.java`

```java
@Override
public List<ApigeeCustomer> fetchDevelopers(String org, String env) {
    log.info("FakeApigeeGateway: Fetching developers for org: {}, env: {}", org, env);
    
    return List.of(
        ApigeeCustomer.builder()
            .developerId("dev-001")
            .email("developer1@example.com")
            .firstName("John")
            .lastName("Developer")
            .userName("john.dev")
            .organizationName("Example Corp")
            .status("active")
            .build(),
        ApigeeCustomer.builder()
            .developerId("dev-002")
            .email("developer2@example.com")
            .firstName("Jane")
            .lastName("Smith")
            .userName("jane.smith")
            .organizationName("Tech Inc")
            .status("active")
            .build()
    );
}
```

---

### 6️⃣ **Update RealApigeeGateway**

**File:** `src/main/java/com/aforo/apigee/gateway/RealApigeeGateway.java`

```java
@Override
public List<ApigeeCustomer> fetchDevelopers(String org, String env) {
    log.info("RealApigeeGateway: Fetching developers from Apigee for org: {}", org);
    
    try {
        String url = String.format("https://api.enterprise.apigee.com/v1/organizations/%s/developers", org);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<ApigeeCustomer[]> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            ApigeeCustomer[].class
        );
        
        if (response.getBody() != null) {
            return Arrays.asList(response.getBody());
        }
        
        return List.of();
    } catch (Exception e) {
        log.error("Failed to fetch developers from Apigee: {}", e.getMessage());
        throw new RuntimeException("Failed to fetch developers from Apigee", e);
    }
}
```

---

### 7️⃣ **Add Sync Method to InventoryService**

**File:** `src/main/java/com/aforo/apigee/service/InventoryService.java`

```java
CustomerSyncResponse syncCustomers();
```

**File:** `src/main/java/com/aforo/apigee/service/impl/InventoryServiceImpl.java`

```java
@Value("${aforo.services.customer}")
private String customerServiceUrl;

@Override
public CustomerSyncResponse syncCustomers() {
    Long orgId = TenantContext.getTenantId();
    log.info("Starting customer sync for organization: {}", orgId);
    
    // Get connection config
    ConnectionConfig config = connectionConfigRepository.findByOrgId(orgId)
        .orElseThrow(() -> new RuntimeException("Connection not configured for organization: " + orgId));
    
    // Fetch developers from Apigee
    List<ApigeeCustomer> apigeeCustomers = apigeeGateway.fetchDevelopers(
        config.getOrg(), 
        config.getEnvsCsv().split(",")[0] // Use first environment
    );
    
    log.info("Fetched {} developers from Apigee", apigeeCustomers.size());
    
    int created = 0;
    int updated = 0;
    int failed = 0;
    List<String> errors = new ArrayList<>();
    
    // Import each customer to Customer Service
    for (ApigeeCustomer apigeeCustomer : apigeeCustomers) {
        try {
            CustomerImportRequest importRequest = CustomerImportRequest.builder()
                .businessEmail(apigeeCustomer.getEmail())
                .firstName(apigeeCustomer.getFirstName())
                .lastName(apigeeCustomer.getLastName())
                .companyName(apigeeCustomer.getOrganizationName())
                .phoneNumber(null) // Apigee doesn't provide phone
                .source("APIGEE")
                .externalId(apigeeCustomer.getDeveloperId())
                .build();
            
            // Call Customer Service import endpoint
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Organization-Id", String.valueOf(orgId));
            
            HttpEntity<CustomerImportRequest> entity = new HttpEntity<>(importRequest, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                customerServiceUrl + "/api/customers/import",
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String status = (String) response.getBody().get("status");
                if ("CREATED".equals(status)) {
                    created++;
                } else if ("UPDATED".equals(status)) {
                    updated++;
                }
                log.info("Imported customer: {} - {}", apigeeCustomer.getEmail(), status);
            }
            
        } catch (Exception e) {
            failed++;
            String error = String.format("Failed to import customer %s: %s", 
                apigeeCustomer.getEmail(), e.getMessage());
            errors.add(error);
            log.error(error, e);
        }
    }
    
    log.info("Customer sync completed. Created: {}, Updated: {}, Failed: {}", 
        created, updated, failed);
    
    return CustomerSyncResponse.builder()
        .message("Customer sync completed")
        .totalFetched(apigeeCustomers.size())
        .totalCreated(created)
        .totalUpdated(updated)
        .totalFailed(failed)
        .errors(errors)
        .build();
}
```

---

### 8️⃣ **Add Sync Endpoint to Controller**

**File:** `src/main/java/com/aforo/apigee/controller/ApigeeIntegrationController.java`

```java
@PostMapping("/customers/sync")
public ResponseEntity<CustomerSyncResponse> syncCustomers() {
    log.info("Customer sync request received");
    CustomerSyncResponse response = inventoryService.syncCustomers();
    return ResponseEntity.ok(response);
}
```

---

### 9️⃣ **Update application.yml**

**File:** `src/main/resources/application.yml`

```yaml
aforo:
  services:
    customer: ${CUSTOMER_SERVICE_URL:http://localhost:8082}
```

---

## 🧪 Testing

### Test Customer Import (Customer Service - Port 8082)

```bash
curl -X POST http://localhost:8082/api/customers/import \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: 3" \
  -d '{
    "businessEmail": "test.developer@example.com",
    "firstName": "Test",
    "lastName": "Developer",
    "companyName": "Test Corp",
    "phoneNumber": "+1234567890",
    "source": "APIGEE",
    "externalId": "apigee-dev-123"
  }'
```

**Expected Response:**
```json
{
  "message": "Customer imported successfully",
  "status": "CREATED",
  "customerId": 1,
  "customerEmail": "test.developer@example.com",
  "source": "APIGEE",
  "externalId": "apigee-dev-123"
}
```

---

### Test Customer Sync (Integration Service - Port 8086)

```bash
curl -X POST http://localhost:8086/api/integrations/apigee/customers/sync \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected Response:**
```json
{
  "message": "Customer sync completed",
  "totalFetched": 2,
  "totalCreated": 2,
  "totalUpdated": 0,
  "totalFailed": 0,
  "errors": []
}
```

---

## 📋 Summary

### Customer Service Changes:
1. ✅ Add `source` and `externalId` to Customer entity
2. ✅ Create `CustomerImportRequest` and `CustomerImportResponse` DTOs
3. ✅ Add `POST /api/customers/import` endpoint (permitAll)
4. ✅ Implement `importExternalCustomer` method
5. ✅ Add repository methods
6. ✅ Create Liquibase changelog
7. ✅ Update SecurityConfig

### Integration Service Changes:
1. ✅ Create DTOs: `CustomerImportRequest`, `CustomerSyncResponse`, `ApigeeCustomer`
2. ✅ Add `fetchDevelopers` to ApigeeGateway
3. ✅ Implement in FakeApigeeGateway and RealApigeeGateway
4. ✅ Add `syncCustomers` method to InventoryService
5. ✅ Add `POST /api/integrations/apigee/customers/sync` endpoint
6. ✅ Update application.yml with customer service URL

---

## 🎯 End-to-End Flow

1. **User clicks "Sync Customers"** in frontend
2. **Frontend calls** `POST /api/integrations/apigee/customers/sync` with JWT
3. **Integration Service:**
   - Validates JWT and extracts organizationId
   - Fetches developers from Apigee
   - For each developer, calls Customer Service import endpoint
4. **Customer Service:**
   - Receives import request
   - Checks if customer exists by externalId + source
   - Creates new or updates existing customer
   - Returns status (CREATED/UPDATED)
5. **Integration Service** returns summary to frontend

**All done!** 🎉
