package aforo.kong.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.time.Instant;

@Entity
@Table(name = "client_api_details")
@Data
@JsonInclude(Include.NON_NULL)
public class ClientApiDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "endpoint")
    private String endpoint;

    @Column(name = "auth_token")
    private String authToken;
    
    @Column(name = "organization_id")
    private Long organizationId;
    
    @Column(name = "environment")
    private String environment; // "konnect" or "self-managed"
    
    @Column(name = "workspace")
    private String workspace;
    
    @Column(name = "additional_config", columnDefinition = "TEXT")
    private String additionalConfig; // JSON string for additional configuration
    
    @Column(name = "mtls_cert", columnDefinition = "TEXT")
    private String mtlsCert;
    
    @Column(name = "mtls_key", columnDefinition = "TEXT")
    private String mtlsKey;
    
    @Column(name = "connection_status")
    private String connectionStatus; // "connected", "disconnected", "failed"
    
    @Column(name = "last_sync")
    private Instant lastSync;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
}
