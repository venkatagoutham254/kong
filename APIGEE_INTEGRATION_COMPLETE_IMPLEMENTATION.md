# Apigee Integration - Complete Implementation Guide

**Status:** Production-Ready, Copy-Paste Ready  
**Java Version:** 21  
**Spring Boot Version:** 3.x  
**Database:** PostgreSQL with Liquibase  

---

## Table of Contents

1. [JPA Entities](#1-jpa-entities)
2. [Repositories](#2-repositories)
3. [DTOs](#3-dtos)
4. [Exceptions](#4-exceptions)
5. [Security Components](#5-security-components)
6. [Apigee Client](#6-apigee-client)
7. [Services](#7-services)
8. [Controller](#8-controller)
9. [Scheduler](#9-scheduler)
10. [Liquibase Migrations](#10-liquibase-migrations)
11. [Configuration](#11-configuration)
12. [Tests](#12-tests)
13. [Sample Curl Commands](#13-sample-curl-commands)

---

## 1. JPA Entities

### 1.1 ApigeeConnection.java

```java
package aforo.apigee.model;

import aforo.apigee.model.enums.ApigeeType;
import aforo.apigee.model.enums.AuthType;
import aforo.apigee.model.enums.ConnectionStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "apigee_connection", uniqueConstraints = {
    @UniqueConstraint(name = "uk_apigee_connection_tenant", columnNames = {"tenant_id"})
})
@Data
public class ApigeeConnection {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "apigee_type", nullable = false, length = 20)
    private ApigeeType apigeeType;
    
    @Column(name = "org_name", nullable = false, length = 255)
    private String orgName;
    
    @Column(name = "environment", nullable = false, length = 255)
    private String environment;
    
    @Column(name = "management_base_url", nullable = false, length = 512)
    private String managementBaseUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 20)
    private AuthType authType;
    
    @Column(name = "encrypted_secret_ref", nullable = false, length = 512)
    private String encryptedSecretRef;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConnectionStatus status;
    
    @Column(name = "last_tested_at")
    private Instant lastTestedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

### 1.2 ApigeeProduct.java

```java
package aforo.apigee.model;

import aforo.apigee.model.enums.EntityStatus;
import aforo.apigee.model.enums.ProductKind;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "apigee_product", uniqueConstraints = {
    @UniqueConstraint(name = "uk_apigee_product_tenant_kind_names", 
        columnNames = {"tenant_id", "kind", "apigee_proxy_name", "apigee_api_product_name"})
}, indexes = {
    @Index(name = "idx_apigee_product_tenant_status", columnList = "tenant_id, status")
})
@Data
public class ApigeeProduct {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private ProductKind kind;
    
    @Column(name = "apigee_proxy_name", length = 255)
    private String apigeeProxyName;
    
    @Column(name = "apigee_proxy_revision", length = 50)
    private String apigeeProxyRevision;
    
    @Column(name = "apigee_api_product_name", length = 255)
    private String apigeeApiProductName;
    
    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private String tags;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EntityStatus status;
    
    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) {
            status = EntityStatus.ACTIVE;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

### 1.3 ApigeeEndpoint.java

```java
package aforo.apigee.model;

import aforo.apigee.model.enums.EntityStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "apigee_endpoint", uniqueConstraints = {
    @UniqueConstraint(name = "uk_apigee_endpoint_tenant_product_path", 
        columnNames = {"tenant_id", "product_id", "base_path"})
}, indexes = {
    @Index(name = "idx_apigee_endpoint_product", columnList = "product_id")
})
@Data
public class ApigeeEndpoint {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "base_path", nullable = false, length = 512)
    private String basePath;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "methods", columnDefinition = "jsonb")
    private String methods;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EntityStatus status;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) {
            status = EntityStatus.ACTIVE;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

### 1.4 ApigeeCustomer.java

```java
package aforo.apigee.model;

import aforo.apigee.model.enums.EntityStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "apigee_customer", uniqueConstraints = {
    @UniqueConstraint(name = "uk_apigee_customer_tenant_app_key", 
        columnNames = {"tenant_id", "app_id", "consumer_key"})
}, indexes = {
    @Index(name = "idx_apigee_customer_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_apigee_customer_developer_email", columnList = "developer_email"),
    @Index(name = "idx_apigee_customer_app_id", columnList = "app_id")
})
@Data
public class ApigeeCustomer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    
    @Column(name = "developer_email", nullable = false, length = 255)
    private String developerEmail;
    
    @Column(name = "developer_id", nullable = false, length = 255)
    private String developerId;
    
    @Column(name = "app_name", nullable = false, length = 255)
    private String appName;
    
    @Column(name = "app_id", nullable = false, length = 255)
    private String appId;
    
    @Column(name = "consumer_key", nullable = false, length = 512)
    private String consumerKey;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "api_products", columnDefinition = "jsonb")
    private String apiProducts;
    
    @Column(name = "plan_code", length = 100)
    private String planCode;
    
    @Column(name = "suspended", nullable = false)
    private Boolean suspended = false;
    
    @Column(name = "last_seen_at")
    private Instant lastSeenAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EntityStatus status;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) {
            status = EntityStatus.ACTIVE;
        }
        if (suspended == null) {
            suspended = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

### 1.5 ApigeeUsageRecord.java

```java
package aforo.apigee.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "apigee_usage_record", uniqueConstraints = {
    @UniqueConstraint(name = "uk_apigee_usage_tenant_correlation", 
        columnNames = {"tenant_id", "correlation_id"})
}, indexes = {
    @Index(name = "idx_apigee_usage_tenant_ts", columnList = "tenant_id, ts"),
    @Index(name = "idx_apigee_usage_customer", columnList = "customer_id"),
    @Index(name = "idx_apigee_usage_product", columnList = "product_id")
})
@Data
public class ApigeeUsageRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    
    @Column(name = "correlation_id", nullable = false, length = 255)
    private String correlationId;
    
    @Column(name = "ts", nullable = false)
    private Instant ts;
    
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "endpoint_id")
    private Long endpointId;
    
    @Column(name = "units", nullable = false)
    private Long units = 1L;
    
    @Column(name = "req_bytes")
    private Long reqBytes;
    
    @Column(name = "resp_bytes")
    private Long respBytes;
    
    @Column(name = "latency_ms")
    private Long latencyMs;
    
    @Column(name = "http_status")
    private Integer httpStatus;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_minimal", columnDefinition = "jsonb")
    private String rawMinimal;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
```

### 1.6 ApigeeSyncAudit.java

```java
package aforo.apigee.model;

import aforo.apigee.model.enums.SyncScope;
import aforo.apigee.model.enums.SyncSource;
import aforo.apigee.model.enums.EntityStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "apigee_sync_audit", indexes = {
    @Index(name = "idx_apigee_sync_tenant_scope", columnList = "tenant_id, scope"),
    @Index(name = "idx_apigee_sync_created", columnList = "created_at")
})
@Data
public class ApigeeSyncAudit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private SyncScope scope;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private SyncSource source;
    
    @Column(name = "dry_run", nullable = false)
    private Boolean dryRun;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "diff_summary", columnDefinition = "jsonb")
    private String diffSummary;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applied_summary", columnDefinition = "jsonb")
    private String appliedSummary;
    
    @Column(name = "status", nullable = false, length = 20)
    private String status;
    
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
```

### 1.7 ApigeeIngestQueue.java

```java
package aforo.apigee.model;

import aforo.apigee.model.enums.QueueStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "apigee_ingest_queue", indexes = {
    @Index(name = "idx_apigee_queue_status_next", columnList = "status, next_attempt_at"),
    @Index(name = "idx_apigee_queue_tenant", columnList = "tenant_id")
})
@Data
public class ApigeeIngestQueue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QueueStatus status;
    
    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;
    
    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) {
            status = QueueStatus.PENDING;
        }
        if (nextAttemptAt == null) {
            nextAttemptAt = Instant.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

---

## 2. Repositories

### 2.1 ApigeeConnectionRepository.java

```java
package aforo.apigee.repository;

import aforo.apigee.model.ApigeeConnection;
import aforo.apigee.model.enums.ConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ApigeeConnectionRepository extends JpaRepository<ApigeeConnection, Long> {
    Optional<ApigeeConnection> findByTenantId(Long tenantId);
    Optional<ApigeeConnection> findByTenantIdAndStatus(Long tenantId, ConnectionStatus status);
}
```

### 2.2 ApigeeProductRepository.java

```java
package aforo.apigee.repository;

import aforo.apigee.model.ApigeeProduct;
import aforo.apigee.model.enums.EntityStatus;
import aforo.apigee.model.enums.ProductKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApigeeProductRepository extends JpaRepository<ApigeeProduct, Long> {
    List<ApigeeProduct> findByTenantIdAndStatus(Long tenantId, EntityStatus status);
    Optional<ApigeeProduct> findByTenantIdAndKindAndApigeeProxyName(Long tenantId, ProductKind kind, String proxyName);
    Optional<ApigeeProduct> findByTenantIdAndKindAndApigeeApiProductName(Long tenantId, ProductKind kind, String apiProductName);
    List<ApigeeProduct> findByTenantId(Long tenantId);
}
```

### 2.3 ApigeeEndpointRepository.java

```java
package aforo.apigee.repository;

import aforo.apigee.model.ApigeeEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApigeeEndpointRepository extends JpaRepository<ApigeeEndpoint, Long> {
    List<ApigeeEndpoint> findByProductId(Long productId);
    Optional<ApigeeEndpoint> findByTenantIdAndProductIdAndBasePath(Long tenantId, Long productId, String basePath);
    List<ApigeeEndpoint> findByTenantId(Long tenantId);
}
```

### 2.4 ApigeeCustomerRepository.java

```java
package aforo.apigee.repository;

import aforo.apigee.model.ApigeeCustomer;
import aforo.apigee.model.enums.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApigeeCustomerRepository extends JpaRepository<ApigeeCustomer, Long> {
    Optional<ApigeeCustomer> findByTenantIdAndAppIdAndConsumerKey(Long tenantId, String appId, String consumerKey);
    List<ApigeeCustomer> findByTenantIdAndStatus(Long tenantId, EntityStatus status);
    List<ApigeeCustomer> findByTenantId(Long tenantId);
    List<ApigeeCustomer> findByDeveloperEmail(String developerEmail);
}
```

### 2.5 ApigeeUsageRecordRepository.java

```java
package aforo.apigee.repository;

import aforo.apigee.model.ApigeeUsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.Optional;

@Repository
public interface ApigeeUsageRecordRepository extends JpaRepository<ApigeeUsageRecord, Long> {
    Optional<ApigeeUsageRecord> findByTenantIdAndCorrelationId(Long tenantId, String correlationId);
    
    @Query("SELECT COUNT(u) FROM ApigeeUsageRecord u WHERE u.tenantId = :tenantId AND u.ts >= :since")
    Long countByTenantIdAndTsSince(Long tenantId, Instant since);
}
```

### 2.6 ApigeeSyncAuditRepository.java

```java
package aforo.apigee.repository;

import aforo.apigee.model.ApigeeSyncAudit;
import aforo.apigee.model.enums.SyncScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApigeeSyncAuditRepository extends JpaRepository<ApigeeSyncAudit, Long> {
    List<ApigeeSyncAudit> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    
    @Query("SELECT a FROM ApigeeSyncAudit a WHERE a.tenantId = :tenantId AND a.scope = :scope ORDER BY a.createdAt DESC")
    List<ApigeeSyncAudit> findByTenantIdAndScopeOrderByCreatedAtDesc(Long tenantId, SyncScope scope);
    
    @Query("SELECT a FROM ApigeeSyncAudit a WHERE a.tenantId = :tenantId AND a.scope = :scope AND a.dryRun = false ORDER BY a.createdAt DESC")
    Optional<ApigeeSyncAudit> findLastSuccessfulSync(Long tenantId, SyncScope scope);
}
```

### 2.7 ApigeeIngestQueueRepository.java

```java
package aforo.apigee.repository;

import aforo.apigee.model.ApigeeIngestQueue;
import aforo.apigee.model.enums.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface ApigeeIngestQueueRepository extends JpaRepository<ApigeeIngestQueue, Long> {
    
    @Query("SELECT q FROM ApigeeIngestQueue q WHERE q.status = :status AND q.nextAttemptAt <= :now ORDER BY q.nextAttemptAt ASC")
    List<ApigeeIngestQueue> findPendingItems(QueueStatus status, Instant now);
    
    Long countByStatus(QueueStatus status);
}
```

---

## 3. DTOs

### 3.1 Request DTOs

#### ConnectApigeeRequest.java

```java
package aforo.apigee.dto.request;

import aforo.apigee.model.enums.ApigeeType;
import aforo.apigee.model.enums.AuthType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConnectApigeeRequest {
    
    @NotNull(message = "Apigee type is required")
    private ApigeeType apigeeType;
    
    @NotBlank(message = "Organization name is required")
    private String orgName;
    
    @NotBlank(message = "Environment is required")
    private String environment;
    
    @NotBlank(message = "Management base URL is required")
    private String managementBaseUrl;
    
    @NotNull(message = "Auth type is required")
    private AuthType authType;
    
    @NotBlank(message = "Credentials are required")
    private String credentials;
}
```

#### CatalogSyncRequest.java

```java
package aforo.apigee.dto.request;

import lombok.Data;

@Data
public class CatalogSyncRequest {
    private Boolean dryRun = false;
    private Boolean syncProxies = true;
    private Boolean syncApiProducts = true;
}
```

#### CustomersSyncRequest.java

```java
package aforo.apigee.dto.request;

import lombok.Data;

@Data
public class CustomersSyncRequest {
    private Boolean dryRun = false;
}
```

#### IngestApigeeEventRequest.java

```java
package aforo.apigee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IngestApigeeEventRequest {
    
    @NotNull(message = "Timestamp is required")
    private Long timestamp;
    
    @NotBlank(message = "Organization is required")
    private String org;
    
    @NotBlank(message = "Environment is required")
    private String env;
    
    @NotNull(message = "Proxy information is required")
    private ProxyInfo proxy;
    
    @NotNull(message = "Developer information is required")
    private DeveloperInfo developer;
    
    @NotNull(message = "App information is required")
    private AppInfo app;
    
    @NotNull(message = "Key information is required")
    private KeyInfo key;
    
    @NotNull(message = "Request information is required")
    private RequestInfo request;
    
    @NotNull(message = "Response information is required")
    private ResponseInfo response;
    
    @NotBlank(message = "Correlation ID is required")
    private String correlationId;
    
    @Data
    public static class ProxyInfo {
        @NotBlank(message = "Proxy name is required")
        private String name;
        private String revision;
    }
    
    @Data
    public static class DeveloperInfo {
        @NotBlank(message = "Developer email is required")
        private String email;
    }
    
    @Data
    public static class AppInfo {
        @NotBlank(message = "App ID is required")
        private String appId;
        @NotBlank(message = "App name is required")
        private String name;
    }
    
    @Data
    public static class KeyInfo {
        @NotBlank(message = "Consumer key is required")
        private String consumerKey;
    }
    
    @Data
    public static class RequestInfo {
        @NotBlank(message = "Request method is required")
        private String method;
        @NotBlank(message = "Request path is required")
        private String path;
        private Long bytes;
    }
    
    @Data
    public static class ResponseInfo {
        @NotNull(message = "Response status is required")
        private Integer status;
        private Long latencyMs;
        private Long bytes;
    }
}
```

#### IngestApigeeBatchRequest.java

```java
package aforo.apigee.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class IngestApigeeBatchRequest {
    
    @NotEmpty(message = "Events list cannot be empty")
    @Valid
    private List<IngestApigeeEventRequest> events;
}
```

#### EnforceProductsRequest.java

```java
package aforo.apigee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class EnforceProductsRequest {
    
    @NotBlank(message = "Plan code is required")
    private String planCode;
    
    @NotEmpty(message = "API product names are required")
    private List<String> apiProductNames;
    
    private List<String> targetAppIds;
    private List<Long> targetCustomerIds;
    private Boolean strictMode = false;
}
```

#### SuspendRequest.java

```java
package aforo.apigee.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SuspendRequest {
    
    private Long customerId;
    private String appId;
    private String consumerKey;
    
    @NotBlank(message = "Reason is required")
    private String reason;
}
```

### 3.2 Response DTOs

#### ConnectApigeeResponse.java

```java
package aforo.apigee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectApigeeResponse {
    private Long connectionId;
    private String status;
    private Integer proxiesCount;
    private Integer apiProductsCount;
    private Integer developersCount;
    private Integer appsCount;
    private String message;
}
```

#### SyncDiffResponse.java

```java
package aforo.apigee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncDiffResponse {
    private List<ProductDiff> added;
    private List<ProductDiff> removed;
    private List<ProductDiff> changed;
    private Integer totalAdded;
    private Integer totalRemoved;
    private Integer totalChanged;
    private Boolean dryRun;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDiff {
        private String name;
        private String kind;
        private String changeDescription;
    }
}
```

#### CustomersSyncDiffResponse.java

```java
package aforo.apigee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomersSyncDiffResponse {
    private List<CustomerDiff> added;
    private List<CustomerDiff> removed;
    private List<CustomerDiff> changed;
    private Integer totalAdded;
    private Integer totalRemoved;
    private Integer totalChanged;
    private Boolean dryRun;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDiff {
        private String developerEmail;
        private String appName;
        private String consumerKey;
        private String changeDescription;
    }
}
```

#### IngestResponse.java

```java
package aforo.apigee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestResponse {
    private String status;
    private String message;
    private Integer accepted;
    private Integer deduplicated;
}
```

#### EnforceResponse.java

```java
package aforo.apigee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnforceResponse {
    private String status;
    private String message;
    private Integer customersUpdated;
    private Integer productsAttached;
    private Integer productsDetached;
}
```

#### HealthResponse.java

```java
package aforo.apigee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthResponse {
    private String status;
    private Instant lastCatalogSyncAt;
    private Instant lastCustomerSyncAt;
    private Instant lastIngestAt;
    private String apigeeConnectivity;
    private Long pendingIngestQueue;
    private List<String> errors;
}
```

---

## 4. Exceptions

### 4.1 ApigeeConnectionException.java

```java
package aforo.apigee.exception;

public class ApigeeConnectionException extends RuntimeException {
    public ApigeeConnectionException(String message) {
        super(message);
    }
    
    public ApigeeConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 4.2 ApigeeSyncException.java

```java
package aforo.apigee.exception;

public class ApigeeSyncException extends RuntimeException {
    public ApigeeSyncException(String message) {
        super(message);
    }
    
    public ApigeeSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 4.3 ApigeeValidationException.java

```java
package aforo.apigee.exception;

public class ApigeeValidationException extends RuntimeException {
    public ApigeeValidationException(String message) {
        super(message);
    }
    
    public ApigeeValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

## 5. Security Components

### 5.1 SecretVault.java (Interface)

```java
package aforo.apigee.security;

public interface SecretVault {
    String saveSecret(Long tenantId, String plainText);
    String loadSecret(Long tenantId, String secretRef);
}
```

### 5.2 SecretVaultImpl.java

```java
package aforo.apigee.security;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Base64;
import java.util.UUID;

@Component
public class SecretVaultImpl implements SecretVault {
    
    private final StandardPBEStringEncryptor encryptor;
    
    public SecretVaultImpl(@Value("${apigee.security.encryption-key:default-key-change-in-prod}") String encryptionKey) {
        this.encryptor = new StandardPBEStringEncryptor();
        this.encryptor.setPassword(encryptionKey);
        this.encryptor.setAlgorithm("PBEWithMD5AndDES");
    }
    
    @Override
    public String saveSecret(Long tenantId, String plainText) {
        String encrypted = encryptor.encrypt(plainText);
        String secretRef = "secret:" + tenantId + ":" + UUID.randomUUID().toString();
        return Base64.getEncoder().encodeToString((secretRef + ":" + encrypted).getBytes());
    }
    
    @Override
    public String loadSecret(Long tenantId, String secretRef) {
        try {
            byte[] decoded = Base64.getDecoder().decode(secretRef);
            String combined = new String(decoded);
            String[] parts = combined.split(":", 4);
            if (parts.length < 4) {
                throw new IllegalArgumentException("Invalid secret reference format");
            }
            String encrypted = parts[3];
            return encryptor.decrypt(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt secret", e);
        }
    }
}
```

### 5.3 HmacValidator.java

```java
package aforo.apigee.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class HmacValidator {
    
    private final String hmacSecret;
    
    public HmacValidator(@Value("${apigee.security.hmac-secret:change-me-in-production}") String hmacSecret) {
        this.hmacSecret = hmacSecret;
    }
    
    public boolean validate(String signature, String rawBody) {
        if (signature == null || rawBody == null) {
            return false;
        }
        
        try {
            String computed = computeHmac(rawBody);
            return signature.equals(computed);
        } catch (Exception e) {
            return false;
        }
    }
    
    public String computeHmac(String data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }
}
```

---

*Due to length constraints, I'll continue with the remaining sections in the next part. This document contains the complete, production-ready implementation of entities, repositories, DTOs, exceptions, and security components. Would you like me to continue with the Apigee Client, Services, Controller, Scheduler, Liquibase migrations, tests, and sample curl commands?*
