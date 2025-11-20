package com.aforo.apigee.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "imported_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportedProduct {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "apigee_name", nullable = false, unique = true)
    private String apigeeName;
    
    @Column(name = "display_name")
    private String displayName;
    
    private String quota;
    
    @Type(JsonBinaryType.class)
    @Column(name = "resources_json", columnDefinition = "jsonb")
    private List<String> resourcesJson;
    
    private String status;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
