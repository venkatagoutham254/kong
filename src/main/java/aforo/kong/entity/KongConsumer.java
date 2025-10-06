package aforo.kong.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "kong_consumer",
       indexes = {
           @Index(name = "idx_kong_consumer_org", columnList = "organization_id"),
           @Index(name = "idx_kong_consumer_external_id", columnList = "external_id"),
           @Index(name = "idx_kong_consumer_username", columnList = "username"),
           @Index(name = "idx_kong_consumer_custom_id", columnList = "custom_id")
       },
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"external_id", "organization_id"}),
           @UniqueConstraint(columnNames = {"username", "organization_id"}),
           @UniqueConstraint(columnNames = {"custom_id", "organization_id"})
       })
@Data
public class KongConsumer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "internal_id")
    private Long internalId;
    
    @Column(name = "external_id", nullable = false, length = 128)
    private String id;  // Kong consumer ID
    
    @Column(name = "username", length = 255)
    private String username;  // Unique username
    
    @Column(name = "custom_id", length = 255)
    private String customId;  // Custom identifier (e.g., external system ID)
    
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;  // JSON array of tags
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private Instant updatedAt;
    
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
    
    // Consumer group associations (for rate limiting)
    @Column(name = "consumer_groups", columnDefinition = "TEXT")
    private String consumerGroups;  // JSON array of group names
    
    // Additional fields for monetization
    @Column(name = "plan_id")
    private String planId;  // Current pricing plan
    
    @Column(name = "wallet_balance")
    private Double walletBalance;  // Prepaid balance
    
    @Column(name = "currency", length = 3)
    private String currency = "USD";
    
    @Column(name = "status", length = 20)
    private String status = "active";  // active, suspended, terminated
    
    @Column(name = "enforcement_mode", length = 20)
    private String enforcementMode = "group";  // group, termination
    
    // Rate limit settings (cached from Kong)
    @Column(name = "rate_limits", columnDefinition = "TEXT")
    private String rateLimits;  // JSON object with window limits
    
    @Column(name = "last_enforcement_push")
    private Instant lastEnforcementPush;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;  // Additional JSON metadata
}
