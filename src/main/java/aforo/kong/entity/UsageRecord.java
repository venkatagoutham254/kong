package aforo.kong.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "usage_record",
       indexes = {
           @Index(name = "idx_usage_timestamp", columnList = "timestamp"),
           @Index(name = "idx_usage_consumer", columnList = "consumer_id"),
           @Index(name = "idx_usage_service", columnList = "service_id"),
           @Index(name = "idx_usage_route", columnList = "route_id"),
           @Index(name = "idx_usage_correlation", columnList = "correlation_id"),
           @Index(name = "idx_usage_org_ts", columnList = "organization_id, timestamp")
       },
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"correlation_id", "organization_id"})
       })
@Data
public class UsageRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usage_id")
    private Long usageId;
    
    @Column(name = "correlation_id", length = 128)
    private String correlationId;  // X-Correlation-ID from Kong
    
    @Column(name = "kong_request_id", length = 128)
    private String kongRequestId;  // Kong's internal request ID
    
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
    
    // Foreign keys to Kong entities
    @Column(name = "consumer_id", length = 128)
    private String consumerId;  // Kong consumer ID
    
    @Column(name = "consumer_username", length = 255)
    private String consumerUsername;
    
    @Column(name = "consumer_custom_id", length = 255)
    private String consumerCustomId;
    
    @Column(name = "service_id", length = 128)
    private String serviceId;  // Kong service ID
    
    @Column(name = "service_name", length = 255)
    private String serviceName;
    
    @Column(name = "route_id", length = 128)
    private String routeId;  // Kong route ID
    
    @Column(name = "route_name", length = 255)
    private String routeName;
    
    // Request details
    @Column(name = "request_method", length = 10)
    private String requestMethod;  // GET, POST, etc.
    
    @Column(name = "request_path", length = 2048)
    private String requestPath;
    
    @Column(name = "request_size")
    private Long requestSize;  // Request body size in bytes
    
    @Column(name = "request_headers", columnDefinition = "TEXT")
    private String requestHeaders;  // Filtered headers as JSON
    
    // Response details
    @Column(name = "response_status")
    private Integer responseStatus;  // HTTP status code
    
    @Column(name = "response_size")
    private Long responseSize;  // Response body size in bytes
    
    @Column(name = "response_latency")
    private Long responseLatency;  // Total latency in milliseconds
    
    @Column(name = "upstream_latency")
    private Long upstreamLatency;  // Upstream service latency
    
    @Column(name = "kong_latency")
    private Long kongLatency;  // Kong processing latency
    
    // Billing metrics
    @Column(name = "metric_type", length = 50)
    private String metricType;  // calls, egress_bytes, ingress_bytes, etc.
    
    @Column(name = "billable_units")
    private Double billableUnits;  // Calculated billable units
    
    @Column(name = "unit_price")
    private Double unitPrice;  // Price per unit at time of usage
    
    @Column(name = "total_cost")
    private Double totalCost;  // Calculated cost
    
    @Column(name = "currency", length = 3)
    private String currency = "USD";
    
    // Processing status
    @Column(name = "processed")
    private Boolean processed = false;
    
    @Column(name = "billed")
    private Boolean billed = false;
    
    @Column(name = "invoice_id")
    private String invoiceId;
    
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
    
    // Additional metadata
    @Column(name = "client_ip", length = 45)
    private String clientIp;
    
    @Column(name = "user_agent", length = 512)
    private String userAgent;
    
    @Column(name = "geo_country", length = 2)
    private String geoCountry;
    
    @Column(name = "geo_city", length = 100)
    private String geoCity;
    
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;  // JSON array of tags
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;  // Additional JSON metadata
}
