package aforo.kong.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "konnect_route_map")
@Data
public class KonnectRouteMap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "control_plane_id", nullable = false)
    private String controlPlaneId;

    @Column(name = "kong_route_id", nullable = false)
    private String kongRouteId;

    @Column(name = "kong_service_id")
    private String kongServiceId;

    @Column(name = "aforo_endpoint_id")
    private Long aforoEndpointId;

    @Column(name = "name")
    private String name;

    @Column(name = "methods", columnDefinition = "TEXT")
    private String methods;

    @Column(name = "paths", columnDefinition = "TEXT")
    private String paths;

    @Column(name = "hosts", columnDefinition = "TEXT")
    private String hosts;

    @Column(name = "protocols", columnDefinition = "TEXT")
    private String protocols;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    @Column(name = "status")
    private String status = "ACTIVE";

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        lastSeenAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        lastSeenAt = Instant.now();
    }
}
