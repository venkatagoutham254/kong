package aforo.kong.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "kong_product")
@Data
public class KongProduct {
    @Id
    @Column(nullable = false, unique = true, length = 128)
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
    
}
