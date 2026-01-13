package aforo.kong.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "konnect_service_map")
@Data
public class KonnectServiceMap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "control_plane_id", nullable = false)
    private String controlPlaneId;

    @Column(name = "kong_service_id", nullable = false)
    private String kongServiceId;

    @Column(name = "aforo_product_id")
    private Long aforoProductId;

    @Column(name = "name_snapshot")
    private String nameSnapshot;

    @Column(name = "tags_snapshot", columnDefinition = "TEXT")
    private String tagsSnapshot;

    @Column(name = "host")
    private String host;

    @Column(name = "port")
    private Integer port;

    @Column(name = "path")
    private String path;

    @Column(name = "protocol")
    private String protocol;

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
