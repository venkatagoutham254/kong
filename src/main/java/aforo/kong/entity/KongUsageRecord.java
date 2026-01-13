package aforo.kong.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "kong_usage_record")
@Data
public class KongUsageRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "ts", nullable = false)
    private Instant timestamp;

    @Column(name = "kong_request_id")
    private String kongRequestId;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Column(name = "kong_service_id")
    private String kongServiceId;

    @Column(name = "kong_route_id")
    private String kongRouteId;

    @Column(name = "kong_consumer_id")
    private String kongConsumerId;

    @Column(name = "http_method")
    private String httpMethod;

    @Column(name = "path", length = 2000)
    private String path;

    @Column(name = "status")
    private Integer status;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "req_size")
    private Long requestSize;

    @Column(name = "resp_size")
    private Long responseSize;

    @Column(name = "aforo_product_id")
    private Long aforoProductId;

    @Column(name = "aforo_endpoint_id")
    private Long aforoEndpointId;

    @Column(name = "aforo_customer_id")
    private Long aforoCustomerId;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "processed")
    private Boolean processed = false;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (processed == null) {
            processed = false;
        }
    }
}
