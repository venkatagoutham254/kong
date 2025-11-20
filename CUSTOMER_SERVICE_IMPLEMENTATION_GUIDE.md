# 🔧 Customer Service - Implementation Guide

## Overview
This guide provides the complete implementation for customer import and listing functionality to support Apigee integration.

---

## ⚠️ CRITICAL CHANGES REQUIRED

### 1. Add `source` Field to Import Endpoint
### 2. Implement GET Customers Endpoint
### 3. Update Database Schema

---

# Part 1: Add `source` Field to Import Endpoint

## 🔴 **CRITICAL: Current Implementation is Missing `source` Field**

Your current import endpoint has:
```json
{
  "businessEmail": "string",
  "firstName": "string",
  "lastName": "string",
  "companyName": "string",
  "phoneNumber": "string",
  "externalId": "string"
}
```

**REQUIRED: Add `source` field:**
```json
{
  "businessEmail": "string",
  "firstName": "string",
  "lastName": "string",
  "companyName": "string",
  "phoneNumber": "string",
  "source": "string",        // ← ADD THIS
  "externalId": "string"
}
```

---

## Step 1: Update CustomerImportRequest DTO

**File:** `CustomerImportRequest.java`

```java
package [your.package].dto.request;

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
    private String source;  // ← ADD THIS FIELD (e.g., "APIGEE", "MANUAL", "KONG")
    
    @NotBlank(message = "External ID is required for import")
    private String externalId;
}
```

---

## Step 2: Update Customer Entity

**File:** `Customer.java`

Add these fields to your Customer entity:

```java
@Entity
@Table(name = "customers")
public class Customer {
    
    // ... existing fields ...
    
    @Column(name = "source", nullable = false)
    @Builder.Default
    private String source = "MANUAL";  // ← ADD THIS
    
    @Column(name = "external_id", nullable = true)
    private String externalId;  // ← ADD THIS
    
    // ... rest of the entity ...
}
```

---

## Step 3: Update CustomerRepository

**File:** `CustomerRepository.java`

Add this method:

```java
package [your.package].repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    // Existing methods...
    
    // ← ADD THIS METHOD
    Optional<Customer> findByExternalIdAndSourceAndOrganizationId(
        String externalId, 
        String source, 
        Long organizationId
    );
    
    // For GET customers endpoint
    List<Customer> findByOrganizationId(Long organizationId);
    
    // Optional: Filter by source
    List<Customer> findByOrganizationIdAndSource(Long organizationId, String source);
}
```

---

## Step 4: Update CustomerService Implementation

**File:** `CustomerServiceImpl.java`

Update the `importExternalCustomer` method:

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
    
    // Get organization ID from tenant context
    Long organizationId = TenantContext.getCurrentTenant();
    if (organizationId == null) {
        throw new IllegalStateException("Organization ID not found in context");
    }
    
    // ✅ CRITICAL: Check if customer exists by externalId + source + organizationId
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
            .source(request.getSource())  // ← SET SOURCE
            .externalId(request.getExternalId())  // ← SET EXTERNAL ID
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

## Step 5: Create CustomerImportResponse DTO

**File:** `CustomerImportResponse.java`

```java
package [your.package].dto.response;

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
    private String status;  // "CREATED" or "UPDATED"
    private Long customerId;
    private String customerEmail;
    private String source;
    private String externalId;
}
```

---

## Step 6: Update SecurityConfig

**File:** `SecurityConfig.java`

Ensure the import endpoint is permitAll (no JWT required for service-to-service calls):

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> {})
        .authorizeHttpRequests(auth -> auth
            // Allow Swagger & health endpoints
            .requestMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/api/health"
            ).permitAll()
            
            // ✅ CRITICAL: Allow import endpoint without JWT (service-to-service)
            .requestMatchers(HttpMethod.POST, "/v1/api/customers/import").permitAll()
            
            // All other customer endpoints require JWT
            .requestMatchers("/v1/api/customers/**").authenticated()
            
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 ->
            oauth2.jwt(jwt -> jwt.decoder(jwtDecoder()))
        );
    
    // Add JWT tenant filter
    http.addFilterAfter(jwtTenantFilter, BearerTokenAuthenticationFilter.class);
    
    return http.build();
}
```

---

## Step 7: Create Liquibase Changelog

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
databaseChangeLog:
  # ... existing changelogs ...
  
  - include:
      file: db/changelog/add-customer-source-externalid.yml
```

---

# Part 2: Implement GET Customers Endpoint

## Step 1: Add Method to CustomerService Interface

**File:** `CustomerService.java`

```java
public interface CustomerService {
    
    // Existing methods...
    
    // ← ADD THESE METHODS
    List<CustomerResponse> getAllCustomers();
    
    List<CustomerResponse> getCustomersBySource(String source);
}
```

---

## Step 2: Implement in CustomerServiceImpl

**File:** `CustomerServiceImpl.java`

```java
@Override
public List<CustomerResponse> getAllCustomers() {
    Long organizationId = TenantContext.getCurrentTenant();
    if (organizationId == null) {
        throw new IllegalStateException("Organization ID not found in context");
    }
    
    log.info("Fetching all customers for organization: {}", organizationId);
    
    List<Customer> customers = customerRepository.findByOrganizationId(organizationId);
    
    return customers.stream()
        .map(this::toCustomerResponse)
        .collect(Collectors.toList());
}

@Override
public List<CustomerResponse> getCustomersBySource(String source) {
    Long organizationId = TenantContext.getCurrentTenant();
    if (organizationId == null) {
        throw new IllegalStateException("Organization ID not found in context");
    }
    
    log.info("Fetching customers for organization: {} with source: {}", organizationId, source);
    
    List<Customer> customers = customerRepository.findByOrganizationIdAndSource(organizationId, source);
    
    return customers.stream()
        .map(this::toCustomerResponse)
        .collect(Collectors.toList());
}

// Helper method to convert entity to response DTO
private CustomerResponse toCustomerResponse(Customer customer) {
    return CustomerResponse.builder()
        .id(customer.getId())
        .businessEmail(customer.getBusinessEmail())
        .firstName(customer.getFirstName())
        .lastName(customer.getLastName())
        .companyName(customer.getCompanyName())
        .phoneNumber(customer.getPhoneNumber())
        .status(customer.getStatus())
        .source(customer.getSource())
        .externalId(customer.getExternalId())
        .createdAt(customer.getCreatedAt())
        .build();
}
```

---

## Step 3: Create CustomerResponse DTO

**File:** `CustomerResponse.java`

```java
package [your.package].dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    private Long id;
    private String businessEmail;
    private String firstName;
    private String lastName;
    private String companyName;
    private String phoneNumber;
    private String status;
    private String source;
    private String externalId;
    private LocalDateTime createdAt;
}
```

---

## Step 4: Add Controller Endpoint

**File:** `CustomerController.java`

```java
@RestController
@RequestMapping("/v1/api/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {
    
    private final CustomerService customerService;
    
    // Existing endpoints...
    
    // ← ADD THIS ENDPOINT
    @GetMapping
    @Operation(summary = "Get all customers for the organization")
    public ResponseEntity<List<CustomerResponse>> getCustomers(
        @RequestParam(required = false) String source
    ) {
        log.info("Get customers request received. Source filter: {}", source);
        
        List<CustomerResponse> customers;
        if (source != null && !source.isBlank()) {
            customers = customerService.getCustomersBySource(source);
        } else {
            customers = customerService.getAllCustomers();
        }
        
        return ResponseEntity.ok(customers);
    }
}
```

---

# 🧪 Testing Guide

## Test 1: Import Customer with Source

```bash
curl -X POST http://localhost:8082/v1/api/customers/import \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: 3" \
  -d '{
    "businessEmail": "developer@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "companyName": "Example Corp",
    "phoneNumber": "+1234567890",
    "source": "APIGEE",
    "externalId": "dev-001"
  }'
```

**Expected Response:**
```json
{
  "message": "Customer imported successfully",
  "status": "CREATED",
  "customerId": 1,
  "customerEmail": "developer@example.com",
  "source": "APIGEE",
  "externalId": "dev-001"
}
```

---

## Test 2: Get All Customers

```bash
curl -X GET http://localhost:8082/v1/api/customers \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "businessEmail": "developer@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "companyName": "Example Corp",
    "phoneNumber": "+1234567890",
    "status": "ACTIVE",
    "source": "APIGEE",
    "externalId": "dev-001",
    "createdAt": "2025-11-09T12:00:00Z"
  }
]
```

---

## Test 3: Get Customers by Source

```bash
curl -X GET "http://localhost:8082/v1/api/customers?source=APIGEE" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Test 4: Full Integration Test (Apigee Sync)

```bash
# Step 1: Sync customers from Apigee Integration Service
curl -X POST http://localhost:8086/api/integrations/apigee/customers/sync \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"

# Step 2: Verify customers were imported
curl -X GET "http://localhost:8082/v1/api/customers?source=APIGEE" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Sync Response:**
```json
{
  "message": "Customer sync completed",
  "totalFetched": 1,
  "totalCreated": 1,
  "totalUpdated": 0,
  "totalFailed": 0,
  "errors": []
}
```

---

# 📋 Implementation Checklist

## Required Changes:

- [ ] **Step 1:** Add `source` field to `CustomerImportRequest` DTO
- [ ] **Step 2:** Add `source` and `externalId` fields to `Customer` entity
- [ ] **Step 3:** Add repository method `findByExternalIdAndSourceAndOrganizationId()`
- [ ] **Step 4:** Add repository method `findByOrganizationId()`
- [ ] **Step 5:** Update `importExternalCustomer()` to use source field
- [ ] **Step 6:** Create `CustomerImportResponse` DTO
- [ ] **Step 7:** Create `CustomerResponse` DTO
- [ ] **Step 8:** Implement `getAllCustomers()` method
- [ ] **Step 9:** Implement `getCustomersBySource()` method
- [ ] **Step 10:** Add GET endpoint in `CustomerController`
- [ ] **Step 11:** Update `SecurityConfig` to allow import endpoint
- [ ] **Step 12:** Create Liquibase changelog for schema changes
- [ ] **Step 13:** Test import endpoint with source field
- [ ] **Step 14:** Test GET customers endpoint
- [ ] **Step 15:** Test full Apigee sync integration

---

# 🎯 Summary

## What This Enables:

1. ✅ **Multi-source customer import** (Apigee, Kong, Manual, etc.)
2. ✅ **Automatic sync** from Apigee Integration Service
3. ✅ **List customers** with optional source filtering
4. ✅ **Prevent duplicates** using externalId + source + organizationId
5. ✅ **Track customer origin** for reporting and analytics

## Key Points:

- **`source` field is CRITICAL** - Without it, you can't track where customers came from
- **Uniqueness is based on 3 fields:** externalId + source + organizationId
- **Import endpoint is permitAll** - Allows service-to-service communication
- **GET endpoint requires JWT** - User authentication for listing customers

---

# 🚀 After Implementation

Once all changes are complete:

1. **Restart Customer Service**
2. **Run database migrations** (Liquibase will auto-apply)
3. **Test import endpoint** with source field
4. **Test GET endpoint** with JWT
5. **Run full Apigee sync** from Integration Service

**Integration Service is ready and waiting!** 🎉
