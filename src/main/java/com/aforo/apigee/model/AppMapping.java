package com.aforo.apigee.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "app_mappings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"apigee_app_id", "api_product"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppMapping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "apigee_app_id", nullable = false)
    private String apigeeAppId;
    
    @Column(name = "app_name", nullable = false)
    private String appName;
    
    @Column(name = "apigee_developer_id", nullable = false)
    private String apigeeDeveloperId;
    
    @Column(name = "api_product", nullable = false)
    private String apiProduct;
    
    @Column(name = "aforo_product_id")
    private Long aforoProductId;
    
    @Column(name = "rate_plan_id")
    private Long ratePlanId;
    
    @Column(name = "billing_type")
    private String billingType;
    
    @Column(name = "subscription_id")
    private Long subscriptionId;
    
    @CreationTimestamp
    @Column(name = "mapped_at", nullable = false, updatable = false)
    private Instant mappedAt;
    
    @Column(name = "attributes_pushed")
    private Boolean attributesPushed;
}
