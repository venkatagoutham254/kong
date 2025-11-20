package com.aforo.apigee.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "usage_events", indexes = {
    @Index(name = "idx_usage_ts", columnList = "ts"),
    @Index(name = "idx_usage_app", columnList = "developer_app"),
    @Index(name = "idx_usage_product", columnList = "api_product")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Instant ts;
    
    private String apiproxy;
    
    @Column(name = "developer_app", nullable = false)
    private String developerApp;
    
    @Column(name = "api_product", nullable = false)
    private String apiProduct;
    
    @Column(nullable = false)
    private String method;
    
    @Column(nullable = false)
    private String path;
    
    @Column(nullable = false)
    private Integer status;
    
    @Column(name = "latency_ms")
    private Integer latencyMs;
    
    @Column(name = "bytes_out")
    private Integer bytesOut;
    
    @Type(JsonBinaryType.class)
    @Column(name = "raw_json", columnDefinition = "jsonb")
    private Map<String, Object> rawJson;
}
