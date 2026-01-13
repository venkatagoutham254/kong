# Aforo × Apigee Integration — Product Requirements Document (PRD)

**Version**: 1.0  
**Date**: December 4, 2025  
**Author**: Aforo Engineering Team  
**Status**: Draft for Review

---

## 1. Summary

### Goal
Enable Apigee (Edge/X) customers to plug into Aforo to meter API consumption, price it, and view reports/dashboards—while delegating enforcement and product lifecycle (API products/apps/developers) to Apigee. Aforo must auto-refresh its catalog when APIs change in Apigee.

### Outcomes
1. **One-click connection** from Aforo to Apigee (Edge or X)
2. **Auto product catalog sync** (API Products → Products; Apps → Customers; Proxies → Services)
3. **Near real-time usage ingestion** from Apigee to Aforo via Analytics API
4. **Pricing plans, wallets, invoices, and analytics** in Aforo
5. **Enforcement via Apigee** (Quota policies, Spike Arrest, Developer App suspension) orchestrated by Aforo
6. **Bidirectional health and reconciliation**

---

## 2. Problem Statement

Apigee users lack a turnkey way to monetize APIs with granular, auditable usage-based billing and rich financial analytics. They need enforcement to remain at the gateway edge for latency and reliability, while getting advanced billing capabilities.

---

## 3. Objectives & Non-Goals

### 3.1 Objectives (P0)

1. **Connect** Aforo ↔ Apigee (Management API) with secure credentials and organization scoping
2. **Discover & import** API catalog from Apigee; keep it fresh automatically
3. **Ingest** per-request usage from Apigee Analytics API into Aforo with dedupe
4. **Configure** pricing/quota plans in Aforo; map to Apigee quota policies
5. **Enforce** via Apigee (Aforo orchestrates quotas and app suspensions)
6. **Provide** reports/dashboards, exports, alerts, and reconciliation

### 3.2 Non-Goals

- Aforo will **not** replace Apigee's API proxy runtime or security policies
- Aforo will **not** mutate API proxies beyond quota policy updates
- Aforo will **not** store request/response bodies by default
- Aforo will **not** manage Apigee infrastructure (environments, virtual hosts)

---

## 4. Personas

| Persona | Role | Needs |
|---------|------|-------|
| **API Monetization Admin** | Primary | Owns pricing, quotas, compliance; configures billing |
| **Platform Engineer** | Secondary | Admins Apigee, approves integrations, sets credentials |
| **Finance/RevOps Analyst** | Consumer | Consumes dashboards/exports, audits usage |
| **Developer (API Consumer)** | End User | Views app-level usage & cost in developer portal |

---

## 5. Assumptions

1. Customer runs **Apigee Edge** (Cloud/Hybrid/Private Cloud) or **Apigee X**
2. Has **Management API** access with appropriate RBAC permissions
3. Has **Analytics API** access for usage data extraction
4. Aforo has existing pricing/rating/invoicing engines
5. OAuth 2.0 or Basic Auth available for API authentication

---

## 6. High-Level Architecture

### Discovery Path
- Aforo pulls `/organizations/{org}/apiproducts`, `/apps`, `/developers` via Management API
- Periodic sync (configurable interval) or webhook-based updates (if available)

### Ingestion Path
- Aforo queries Apigee Analytics API for usage metrics
- Metrics: request count, response size, latency, error rates
- Correlation by: API Product, App, Developer, Proxy, Timestamp

### Enforcement Path
- Aforo manages quota policies in Apigee API proxies
- Updates quota limits via Management API
- Suspends/revokes developer apps for prepaid zero balance
- Optional custom policy for wallet checks

### Analytics Path
- Aforo aggregates itemized records → pricing → invoices → dashboards
- Reconciliation: Compare Aforo vs Apigee Analytics counters

---

## 7. Functional Requirements

### 7.1 Connection & Settings (P0)

#### Connect Apigee Wizard (Edge or X)

**Inputs:**
- **Environment Type**: Apigee Edge / Apigee X
- **Management API URL**: 
  - Edge: `https://api.enterprise.apigee.com/v1`
  - X: `https://apigee.googleapis.com/v1`
- **Organization Name**: e.g., `acme-corp`
- **Authentication**:
  - Edge: Username/Password or OAuth token
  - X: Google Cloud Service Account JSON key
- **Scope**: Select organizations/environments to onboard (default: all)

**Validation:**
- "Test Connection" button
- GET `/organizations/{org}` success within 5s
- Display: API Products count, Apps count, Proxies count

**Secrets Storage:**
- Encrypted at rest (AES-256)
- Rotate tokens/credentials
- Audit access logs

**Webhook Registration (if available):**
- Register webhook endpoints for API product/app changes
- Fallback: Polling every 5 minutes

---

### 7.2 Catalog Sync (P0)

#### Initial Import

**Fetch from Apigee:**
1. **API Products** → Aforo Products
   - `/organizations/{org}/apiproducts`
   - Map: name, displayName, description, quota, scopes, proxies
2. **Developer Apps** → Aforo Customers
   - `/organizations/{org}/developers/{dev}/apps`
   - Map: appId, name, credentials, status, apiProducts
3. **API Proxies** → Aforo Services
   - `/organizations/{org}/apis`
   - Map: name, revision, basepaths, targets

**Tag Convention Support:**
- Read custom attributes: `aforo:metric`, `aforo:plan_id`
- Drive pricing metric mapping (e.g., `calls`, `data_transfer`)

#### Incremental Refresh

**Primary: Webhooks** (if available)
- Process within ≤ 60s
- Events: product created/updated/deleted, app approved/revoked

**Fallback: Polling**
- Cursor on `lastModifiedAt` every 5 min
- Idempotent upserts keyed by Apigee IDs

**Change Preview:**
- Show diff (added/removed/changed products/apps) before applying

---

### 7.3 Usage Ingestion (P0)

#### Transport: Apigee Analytics API → Aforo

**Method:**
- Scheduled job (every 5-15 min) queries Analytics API
- Endpoint: `/organizations/{org}/environments/{env}/stats`
- Dimensions: `apiproduct`, `developer_app`, `proxy`, `response_status_code`
- Metrics: `message_count`, `total_response_time`, `response_size`

**Security:**
- OAuth 2.0 or Service Account authentication
- Optional: Custom analytics dimension for correlation ID

**Schema:**
```json
{
  "timestamp": "2025-12-04T10:00:00Z",
  "apiProduct": "premium-api",
  "developerApp": "mobile-app-v1",
  "developer": "dev@example.com",
  "proxy": "payments-v1",
  "environment": "prod",
  "messageCount": 1500,
  "totalResponseTime": 45000,
  "avgResponseTime": 30,
  "responseSize": 2048000,
  "errorCount": 5,
  "status": "200"
}
```

**Dedupe:**
- By (timestamp window, app, product, proxy) fingerprint
- Handle late-arriving data (up to 24h delay)

**Throughput:**
- ≥ 10k events/min per tenant
- Backpressure friendly with queue

**PII:**
- No request/response bodies
- Header allow-list & redaction for sensitive data

---

### 7.4 Pricing & Plans (P0)

#### Metric Types
- **Calls**: API request count
- **Data Transfer**: Request + response size (bytes)
- **Latency Buckets**: <100ms, 100-500ms, >500ms
- **Custom Dimensions**: By API product, proxy, method, status code

#### Price Models
- **Tiered**: First 1000 calls $0.01, next 9000 $0.005
- **Volume**: All calls at $0.008 if total > 5000
- **Stairstep**: Flat $100 for 0-10k, $200 for 10k-50k
- **Per-unit**: $0.01 per call
- **Minimum fee**: $50/month + usage
- **Overage fee**: $0.02 per call beyond quota
- **Prepaid wallet**: Debit from balance

#### Plan Mapping
- Aforo Plan ↔ Apigee API Product
- Example: Bronze Plan → `bronze-api-product`
- Quota config per plan: 1000 calls/day, 10 GB/month

#### Quota Configuration
- Per plan and/or per app
- Maps to Apigee Quota policy
- Windows: minute, hour, day, week, month

#### Top-ups
- Manual/auto top-up
- Triggers quota update or app reactivation

---

### 7.5 Enforcement (P0)

#### Native Edge Enforcement

**Quota Policy Management:**
- Aforo updates Apigee Quota policies via Management API
- Endpoint: `/organizations/{org}/apis/{proxy}/revisions/{rev}/policies/{policy}`
- Changes propagate ≤ 60s (after proxy deployment)

**Prepaid Low Balance:**
- When wallet ≤ 0:
  - Option 1: Update quota to 0
  - Option 2: Revoke developer app credentials
  - Option 3: Set app status to "suspended"

**App Suspension:**
- Endpoint: `/organizations/{org}/developers/{dev}/apps/{app}`
- Set `status: "revoked"` or `status: "suspended"`

#### Optional Authorize Callback (P1)

**Custom Policy:**
- Apigee custom policy hits Aforo `/authorize` endpoint
- Request: `{appId, product, operation, estimatedCost}`
- Response: `{allow: true/false, reason, ttl: 60}`
- Cache decision for 10-60s in Apigee cache

---

### 7.6 Analytics & Reporting (P0)

#### Dashboards
- **Usage**: Calls, data transfer, latency over time
- **Revenue**: Total, ARPU, MRR, ARR
- **Top API Products**: By usage, revenue
- **Top Apps**: By usage, revenue
- **Quota Utilization**: % used per plan
- **Errors/Latency**: Overlay with traffic

#### Drill-downs
- By time window (hour/day/week/month)
- By API product, app, developer, environment
- By plan tier

#### Exports
- **Formats**: CSV, Excel, Parquet
- **Scheduled**: Daily/weekly email links
- **On-demand**: Download button

#### Alerts
- **Thresholds**:
  - Usage spike (>200% of baseline)
  - Drift between Aforo and Apigee Analytics (>2%)
  - Low balance (<$10)
  - Approaching quota (80%, 90%, 100%)
  - High error rate (>5%)

#### Attribution
- Surface correlation IDs for dispute resolution
- Link usage to specific API calls (if correlation ID available)

---

### 7.7 Reconciliation (P1)

#### Counter Comparison
- **Aforo itemized records** vs **Apigee Analytics API counters**
- Daily reconciliation job
- Compare: total calls, total data transfer, total errors
- Raise alert if drift > 2%

#### Reconciliation Report
- Show discrepancies by API product, app, time window
- Export for audit

---

### 7.8 Multi-tenant & RBAC (P0)

#### Multi-tenancy
- Aforo tenants may connect multiple Apigee organizations
- Each tenant isolated by `organizationId`
- Separate credentials per Apigee org

#### RBAC
- **Monetization Admin**: Full access to pricing, enforcement
- **Viewer**: Read-only access to dashboards, reports
- **Developer**: View own app usage only

---

### 7.9 Audit & Compliance (P0)

#### Change Log
- Log all plan/quota updates pushed to Apigee
- Log all app suspensions/reactivations
- Immutable audit trail

#### Webhook Log
- Log all webhook events received from Apigee
- Retry failed webhook processing

#### Ingestion Log
- Immutable log of all usage records
- Hash chain for tamper detection

---

## 8. Non-Functional Requirements

### Performance
- **Latency**: Ingestion path non-blocking; authorize callback P95 < 50ms with cache
- **Availability**: 99.9% Aforo ingest API; offline mode allows Apigee to continue proxying
- **Scale**: 100k RPS across tenants; 10B events/month

### Security
- **Authentication**: OAuth 2.0, Service Account, Basic Auth
- **Encryption**: At-rest (AES-256), in-transit (TLS 1.2+)
- **IP Allowlists**: Optional for Management API access
- **Least Privilege**: RBAC tokens with minimal permissions

### Privacy
- **GDPR-ready**: Data retention policies, right to erasure
- **PII Avoidance**: No request/response bodies by default
- **Data Residency**: Support for regional deployments

---

## 9. Data Model (Aforo)

### Entities

```
Product {
  product_id: UUID
  name: String
  apigee_product_name: String
  apigee_org: String
  description: Text
  pricing_bindings: JSON
  quota_config: JSON
  tags: JSON
  created_at: Timestamp
  updated_at: Timestamp
  organization_id: Long
}

Customer {
  customer_id: UUID
  apigee_app_id: String
  apigee_developer_email: String
  app_name: String
  status: Enum(active, suspended, revoked)
  api_products: JSON[]
  credentials: JSON
  wallet_balance: Decimal
  plan_id: UUID
  created_at: Timestamp
  updated_at: Timestamp
  organization_id: Long
}

Service {
  service_id: UUID
  apigee_proxy_name: String
  apigee_org: String
  revision: String
  basepaths: JSON[]
  targets: JSON[]
  created_at: Timestamp
  updated_at: Timestamp
  organization_id: Long
}

UsageRecord {
  usage_id: UUID
  timestamp: Timestamp
  customer_id: UUID
  product_id: UUID
  service_id: UUID
  message_count: Integer
  response_size: Long
  request_size: Long
  avg_latency: Integer
  error_count: Integer
  status_code: String
  environment: String
  correlation_id: String (optional)
  created_at: Timestamp
  organization_id: Long
}

Plan {
  plan_id: UUID
  name: String
  metrics: JSON[]
  tiers: JSON[]
  quota_windows: JSON[]
  created_at: Timestamp
  updated_at: Timestamp
  organization_id: Long
}

Wallet {
  customer_id: UUID
  balance: Decimal
  currency: String(3)
  auto_topup: Boolean
  topup_threshold: Decimal
  topup_amount: Decimal
  updated_at: Timestamp
}

EnforcementBinding {
  binding_id: UUID
  customer_id: UUID
  product_id: UUID
  quota_limits: JSON
  suspended_flag: Boolean
  last_enforced_at: Timestamp
  organization_id: Long
}
```

---

## 10. Aforo Integration APIs

### 10.1 Connection & Configuration

#### `POST /integrations/apigee/connect`
**Purpose**: Save credentials, test connection, initiate catalog sync

**Request:**
```json
{
  "environment": "edge" | "x",
  "managementApiUrl": "https://api.enterprise.apigee.com/v1",
  "organization": "acme-corp",
  "authentication": {
    "type": "basic" | "oauth" | "service_account",
    "username": "admin@acme.com",
    "password": "***",
    "token": "***",
    "serviceAccountJson": "{...}"
  },
  "scope": {
    "environments": ["prod", "test"],
    "apiProducts": []
  },
  "autoSync": {
    "enabled": true,
    "intervalMinutes": 5
  }
}
```

**Response:**
```json
{
  "connectionId": "uuid",
  "status": "connected" | "failed",
  "message": "Successfully connected to Apigee",
  "productsDiscovered": 15,
  "appsDiscovered": 42,
  "proxiesDiscovered": 8
}
```

---

### 10.2 Catalog Sync

#### `POST /integrations/apigee/catalog/sync`
**Purpose**: On-demand catalog sync trigger

**Request:**
```json
{
  "connectionId": "uuid",
  "syncType": "full" | "incremental"
}
```

**Response:**
```json
{
  "status": "COMPLETED" | "IN_PROGRESS" | "FAILED",
  "sync_start_time": "2025-12-04T10:00:00Z",
  "sync_end_time": "2025-12-04T10:00:15Z",
  "duration_ms": 15000,
  "products": {
    "fetched": 15,
    "created": 3,
    "updated": 12,
    "deleted": 0,
    "failed": 0
  },
  "apps": {
    "fetched": 42,
    "created": 5,
    "updated": 37
  },
  "proxies": {
    "fetched": 8,
    "created": 1,
    "updated": 7
  },
  "errors": []
}
```

---

### 10.3 Usage Ingestion

#### `POST /integrations/apigee/ingest`
**Purpose**: Receive usage data from Apigee Analytics API (internal job)

**Request:**
```json
{
  "organization": "acme-corp",
  "environment": "prod",
  "timeRange": {
    "start": "2025-12-04T09:00:00Z",
    "end": "2025-12-04T10:00:00Z"
  },
  "metrics": [
    {
      "apiProduct": "premium-api",
      "developerApp": "mobile-app-v1",
      "developer": "dev@example.com",
      "proxy": "payments-v1",
      "messageCount": 1500,
      "totalResponseTime": 45000,
      "responseSize": 2048000,
      "errorCount": 5
    }
  ]
}
```

**Response:**
```json
{
  "status": "accepted",
  "recordsProcessed": 1,
  "recordsSkipped": 0
}
```

---

### 10.4 Enforcement

#### `POST /integrations/apigee/enforce/quotas`
**Purpose**: Map plans to API products and push quota limits

**Request:**
```json
{
  "mappings": [
    {
      "planId": "bronze-plan",
      "apiProduct": "bronze-api-product",
      "quotas": [
        {"window": "day", "limit": 1000},
        {"window": "month", "limit": 30000}
      ]
    }
  ]
}
```

**Response:**
```json
{
  "status": "success",
  "updatedProducts": 1,
  "updatedPolicies": 2
}
```

---

#### `POST /integrations/apigee/suspend`
**Purpose**: Suspend developer app (prepaid zero balance)

**Request:**
```json
{
  "appId": "app-uuid",
  "developer": "dev@example.com",
  "mode": "revoke" | "suspend",
  "reason": "Prepaid wallet balance is zero"
}
```

**Response:**
```json
{
  "status": "suspended",
  "appId": "app-uuid",
  "suspendedAt": "2025-12-04T10:00:00Z"
}
```

---

#### `POST /integrations/apigee/resume`
**Purpose**: Resume developer app after top-up

**Request:**
```json
{
  "appId": "app-uuid",
  "developer": "dev@example.com"
}
```

**Response:**
```json
{
  "status": "active",
  "appId": "app-uuid",
  "resumedAt": "2025-12-04T10:05:00Z"
}
```

---

### 10.5 Health & Status

#### `GET /integrations/apigee/health`
**Purpose**: Check connectivity and sync status

**Response:**
```json
{
  "apigeeReachable": true,
  "status": "healthy",
  "lastSync": "2025-12-04T09:55:00Z",
  "nextSync": "2025-12-04T10:00:00Z",
  "activeConnections": 1
}
```

---

### 10.6 Optional Authorize Callback (P1)

#### `POST /integrations/apigee/authorize`
**Purpose**: Real-time allow/deny for custom policy

**Request:**
```json
{
  "appId": "app-uuid",
  "apiProduct": "premium-api",
  "operation": "POST /payments",
  "estimatedCost": 0.05
}
```

**Response:**
```json
{
  "allow": true,
  "reason": "Sufficient wallet balance",
  "ttl_seconds": 60,
  "remainingBalance": 45.50
}
```

---

## 11. UX Requirements & Flows

### 11.1 Settings → Integrations → "Connect Apigee"

#### Screen A: Choose Environment
- **Radio**: Apigee Edge / Apigee X
- **Help link**: "What's the difference?"

#### Screen B: Credentials
**Fields:**
- Management API URL (pre-filled based on environment)
- Organization Name (required)
- Authentication Type: Basic / OAuth / Service Account
- Username/Password or Token or JSON key upload
- IP allowlist hint (read-only)

**Buttons:**
- "Test Connection" (shows Products/Apps/Proxies count)
- "Next"

**Error States:**
- Timeouts, 401/403, invalid org name

#### Screen C: Scope & Auto-Sync
**Multiselect:**
- Environments to onboard (default: All)
- API Products to onboard (default: All)

**Toggles:**
- Enable auto-sync (default: ON)
- Sync interval: 5 / 10 / 15 minutes

**Preview Panel:**
- Impacted products/apps

#### Screen D: Mapping & Attributes
**Table:**
- API Product ↔ Aforo Product preview
- Custom attribute rules: `aforo:metric`, `aforo:plan_id`
- Resolve conflicts (duplicate product codes)

#### Screen E: Review & Connect
**Summary:**
- Credentials (masked)
- Scope
- Sync settings

**Button:** "Connect & Sync"

**Success Toast:**
- "Connected to Apigee! Syncing catalog..."
- Link: "View Imported Products"

---

### 11.2 Products → (Apigee) Catalog

**List View:**
- Products (API Product name, product code, last sync, status)
- Click product → shows Apps using it, metrics, plan binding, recent usage

---

### 11.3 Pricing → Plan Mapping

**Create/Edit Plan:**
- Metrics & tiers
- Map Plan → API Product
- Button: "Apply to Apps" (bulk assign)

---

### 11.4 Customers → Enforcement

**Customer Detail:**
- Current plan (API product)
- Quotas
- Wallet balance
- Last enforcement push time

**Actions:**
- Suspend (prepaid 0)
- Top-up
- Change Plan

---

### 11.5 Analytics → Dashboards

**Widgets:**
- Usage by API product/app/developer
- Revenue
- Quota Utilization
- Errors vs Traffic
- Latency heatmap

**Filters:**
- Time range
- API product
- App
- Developer
- Plan

**Export:**
- CSV/Excel
- Schedule email

---

### 11.6 Alerts

**Create Alert:**
- Low balance threshold
- Quota nearing (80/90/100%)
- Drift > 2% vs Apigee Analytics
- High error rate

---

## 12. Acceptance Criteria (Samples)

1. ✅ After successful connection, new/updated API products in Apigee appear in Aforo within ≤ 5 min
2. ✅ Aforo receives usage data from Analytics API and records with correct app/product mapping
3. ✅ Changing a plan in Aforo updates Apigee quota policy in ≤ 60s; 1001st call on 1000/day quota returns 429
4. ✅ When wallet hits zero, app is suspended and calls are blocked until top-up
5. ✅ Dashboards show usage and revenue with drill-downs; exports match itemized records
6. ✅ Reconciliation job flags drift when difference > 2% and emits alert

---

## 13. Rollout Plan

### Phase 1 (Dev)
- Single org; Analytics API polling; manual plan ↔ product mapping
- Basic dashboards

### Phase 2 (Beta)
- Webhook support (if available); automated quota management
- Advanced dashboards & exports

### Phase 3 (GA)
- Multi-org; reconciliation & alerts
- Optional authorize callback
- Apigee X support

---

## 14. Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| **Management API privilege risk** | Least-privilege tokens; audit logs; scoped orgs |
| **Analytics API rate limits** | Batch queries; cache results; exponential backoff |
| **High ingest volume** | Batching, queueing, backpressure; regional endpoints |
| **Clock skew** | Accept time drift ±5 min; normalize timestamps |
| **Privacy** | Metadata-only ingestion; configurable redaction |
| **Apigee X vs Edge differences** | Adapter pattern; environment-specific logic |

---

## 15. Open Questions

1. Which Apigee regions must Aforo support on day-one? (US, EU, APAC?)
2. Do we allow per-proxy custom meters beyond calls/bytes/latency at GA?
3. Minimum Analytics API query interval to avoid rate limits?
4. Support for Apigee Monetization (native) coexistence?

---

## 16. Appendix: Example API Contracts

### Ingest
`POST /integrations/apigee/ingest`
- Body: metrics array
- 202 on enqueue, 400 on schema error, 401/403 on auth

### Enforce
`POST /integrations/apigee/enforce/quotas`
- Body: `{planId, apiProduct, quotas: [{window, limit}]}`

### Suspend
`POST /integrations/apigee/suspend`
- Body: `{appId, developer, mode: 'revoke'|'suspend'}`

### Authorize (opt)
`POST /integrations/apigee/authorize`
- Body: `{appId, apiProduct, operation, estimatedCost}`
- Response: `{allow, reason, ttl_seconds}`

---

## 17. Success Metrics

### Business Metrics
- **Time to First Invoice**: < 1 hour after connection
- **Billing Accuracy**: > 99.9% match with Apigee Analytics
- **Customer Adoption**: > 80% of Apigee customers onboard within 3 months

### Technical Metrics
- **Sync Latency**: < 5 min (P95)
- **Ingestion Throughput**: > 10k events/min
- **API Availability**: > 99.9%
- **Enforcement Propagation**: < 60s

---

## 18. Glossary

| Term | Definition |
|------|------------|
| **API Product** | Apigee's bundle of API proxies with quota/security policies |
| **Developer App** | Application registered by a developer to consume APIs |
| **Quota Policy** | Apigee policy that limits API calls per time window |
| **Analytics API** | Apigee API for querying usage metrics |
| **Management API** | Apigee API for CRUD operations on products/apps/proxies |
| **Prepaid Wallet** | Customer's pre-funded balance for API usage |
| **ARPU** | Average Revenue Per User |
| **MRR** | Monthly Recurring Revenue |

---

**END OF PRD**

---

## Next Steps

1. **Review** this PRD with stakeholders
2. **Prioritize** features (P0, P1, P2)
3. **Create** Jira epics & stories
4. **Design** OpenAPI spec for Aforo APIs
5. **Implement** Phase 1 (Dev)

---

**Document Status**: Ready for Review  
**Approval Required From**: Product Manager, Engineering Lead, Security Team
