package com.aforo.apigee.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "developer_links")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeveloperLink {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "apigee_developer_id", nullable = false, unique = true)
    private String apigeeDeveloperId;
    
    @Column(nullable = false)
    private String email;
    
    @Column(name = "aforo_customer_id", nullable = false)
    private String aforoCustomerId;
    
    @CreationTimestamp
    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt;
}
