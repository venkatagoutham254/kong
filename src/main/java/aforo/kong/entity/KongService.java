package aforo.kong.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "kong_service", 
       indexes = {
           @Index(name = "idx_kong_service_org", columnList = "organization_id"),
           @Index(name = "idx_kong_service_external_id", columnList = "external_id")
       },
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"external_id", "organization_id"})
       })
@Data
public class KongService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "internal_id")
    private Long internalId;
    
    @Column(name = "external_id", nullable = false, length = 128)
    private String id;  // Kong service ID
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String host;  // Upstream host
    
    @Column
    private Integer port;  // Upstream port
    
    @Column(length = 20)
    private String protocol; // http, https, grpc, grpcs, tcp, tls, udp, ws, wss
    
    @Column(name = "path")
    private String path;  // Upstream path
    
    @Column(name = "retries")
    private Integer retries;
    
    @Column(name = "connect_timeout")
    private Integer connectTimeout;
    
    @Column(name = "write_timeout")
    private Integer writeTimeout;
    
    @Column(name = "read_timeout")
    private Integer readTimeout;
    
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;  // JSON array of tags
    
    @Column(name = "enabled")
    private Boolean enabled = true;
    
    @Column(name = "ca_certificates", columnDefinition = "TEXT")
    private String caCertificates;  // JSON array
    
    @Column(name = "client_certificate", columnDefinition = "TEXT")
    private String clientCertificate;  // JSON object
    
    @Column(name = "tls_verify")
    private Boolean tlsVerify;
    
    @Column(name = "tls_verify_depth")
    private Integer tlsVerifyDepth;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private Instant updatedAt;
    
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
    
    // Relation to routes
    @OneToMany(mappedBy = "kongService", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<KongRoute> routes;
}
