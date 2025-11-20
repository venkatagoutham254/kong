package aforo.kong.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "kong_product")
@Data
public class KongProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "internal_id")
    private Long internalId;
    
    @Column(name = "external_id", nullable = false, unique = true, length = 128)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private int versionCount;

    // store labels map as JSON string
    @Column(name = "labels", columnDefinition = "TEXT")
    private String labels;

    @Column(name = "public_labels_json", columnDefinition = "TEXT")
    private String publicLabelsJson;

    @Column(name = "portal_ids_json", columnDefinition = "TEXT")
    private String portalIdsJson;

    @Column(name = "portals_json", columnDefinition = "TEXT")
    private String portalsJson;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
    
    @Column(name = "source", nullable = false)
    private String source = "kong"; // Default source is kong
}
