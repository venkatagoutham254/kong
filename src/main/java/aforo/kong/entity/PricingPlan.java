package aforo.kong.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "pricing_plan",
       indexes = {
           @Index(name = "idx_pricing_plan_org", columnList = "organization_id"),
           @Index(name = "idx_pricing_plan_name", columnList = "plan_name"),
           @Index(name = "idx_pricing_plan_status", columnList = "status")
       },
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"plan_name", "organization_id"})
       })
@Data
public class PricingPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Long planId;
    
    @Column(name = "plan_name", nullable = false, length = 100)
    private String planName;  // Bronze, Silver, Gold, Custom
    
    @Column(name = "display_name", length = 255)
    private String displayName;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "kong_consumer_group", length = 100)
    private String kongConsumerGroup;  // Mapped Kong consumer group name
    
    // Pricing configuration
    @Column(name = "pricing_model", length = 50)
    private String pricingModel;  // tiered, volume, stairstep, per_unit, prepaid
    
    @Column(name = "currency", length = 3)
    private String currency = "USD";
    
    @Column(name = "base_price")
    private Double basePrice;  // Monthly base fee
    
    @Column(name = "minimum_fee")
    private Double minimumFee;  // Minimum monthly charge
    
    @Column(name = "overage_fee")
    private Double overageFee;  // Fee per unit over quota
    
    // Metrics and pricing tiers (stored as JSON)
    @Column(name = "metrics", columnDefinition = "TEXT")
    private String metrics;  // JSON array of metric configurations
    /* Example metrics JSON:
    [
        {
            "type": "calls",
            "unit": "request",
            "tiers": [
                {"from": 0, "to": 1000, "price": 0},
                {"from": 1001, "to": 10000, "price": 0.001},
                {"from": 10001, "to": null, "price": 0.0005}
            ]
        },
        {
            "type": "egress_bytes",
            "unit": "GB",
            "price": 0.08
        }
    ]
    */
    
    // Rate limit quotas (applied to Kong)
    @Column(name = "quota_limits", columnDefinition = "TEXT")
    private String quotaLimits;  // JSON object with window limits
    /* Example quota limits JSON:
    {
        "second": null,
        "minute": 100,
        "hour": 5000,
        "day": 100000,
        "month": 2000000
    }
    */
    
    // Features and entitlements
    @Column(name = "features", columnDefinition = "TEXT")
    private String features;  // JSON array of included features
    
    @Column(name = "max_consumers")
    private Integer maxConsumers;  // Max consumers allowed on this plan
    
    @Column(name = "max_requests_per_month")
    private Long maxRequestsPerMonth;
    
    // Plan configuration
    @Column(name = "trial_days")
    private Integer trialDays = 0;
    
    @Column(name = "auto_upgrade")
    private Boolean autoUpgrade = false;  // Auto-upgrade to next tier on limit
    
    @Column(name = "prepaid_credits_enabled")
    private Boolean prepaidCreditsEnabled = false;
    
    @Column(name = "auto_topup_enabled")
    private Boolean autoTopupEnabled = false;
    
    @Column(name = "auto_topup_amount")
    private Double autoTopupAmount;
    
    @Column(name = "auto_topup_threshold")
    private Double autoTopupThreshold;  // Trigger topup when balance below this
    
    // Status and metadata
    @Column(name = "status", length = 20)
    private String status = "active";  // active, inactive, deprecated
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;  // Additional JSON metadata
}
