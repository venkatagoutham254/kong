package com.aforo.apigee.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "connection_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String org;
    
    @Column(name = "envs_csv")
    private String envsCsv;
    
    @Column(name = "analytics_mode")
    private String analyticsMode;
    
    @Column(name = "service_account_json", columnDefinition = "TEXT")
    private String serviceAccountJson;
    
    @Column(name = "hmac_secret")
    private String hmacSecret;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
