package aforo.kong.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "kong_route",
       indexes = {
           @Index(name = "idx_kong_route_org", columnList = "organization_id"),
           @Index(name = "idx_kong_route_external_id", columnList = "external_id"),
           @Index(name = "idx_kong_route_service", columnList = "kong_service_id")
       },
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"external_id", "organization_id"})
       })
@Data
public class KongRoute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "internal_id")
    private Long internalId;
    
    @Column(name = "external_id", nullable = false, length = 128)
    private String id;  // Kong route ID
    
    @Column
    private String name;
    
    @Column(name = "protocols", columnDefinition = "TEXT")
    private String protocols;  // JSON array ["http", "https"]
    
    @Column(name = "methods", columnDefinition = "TEXT")
    private String methods;  // JSON array ["GET", "POST"]
    
    @Column(name = "hosts", columnDefinition = "TEXT")
    private String hosts;  // JSON array of hostnames
    
    @Column(name = "paths", columnDefinition = "TEXT")
    private String paths;  // JSON array of path patterns
    
    @Column(name = "headers", columnDefinition = "TEXT")
    private String headers;  // JSON object of header constraints
    
    @Column(name = "https_redirect_status_code")
    private Integer httpsRedirectStatusCode;
    
    @Column(name = "regex_priority")
    private Integer regexPriority;
    
    @Column(name = "strip_path")
    private Boolean stripPath = true;
    
    @Column(name = "preserve_host")
    private Boolean preserveHost = false;
    
    @Column(name = "request_buffering")
    private Boolean requestBuffering = true;
    
    @Column(name = "response_buffering")
    private Boolean responseBuffering = true;
    
    @Column(name = "snis", columnDefinition = "TEXT")
    private String snis;  // JSON array of SNIs
    
    @Column(name = "sources", columnDefinition = "TEXT")
    private String sources;  // JSON array of IP sources
    
    @Column(name = "destinations", columnDefinition = "TEXT")
    private String destinations;  // JSON array of destinations
    
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;  // JSON array of tags
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private Instant updatedAt;
    
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
    
    // Foreign key to kong_service
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kong_service_id", referencedColumnName = "internal_id")
    private KongService kongService;
    
    @Column(name = "kong_service_external_id", length = 128)
    private String kongServiceExternalId;  // Store the Kong service ID for reference
}
