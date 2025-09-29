package aforo.kong.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KongProductDTO {
    @JsonProperty("internal_id")
    private Long internalId;
    
    private String id;
    private String name;
    private String description;

    // {} in JSON → map in Java
    private Map<String, Object> labels;

    @JsonProperty("public_labels")
    private Map<String, Object> publicLabels;

    @JsonProperty("portal_ids")
    private List<String> portalIds;

    // [] of objects → safest as list of generic maps
    private List<Map<String, Object>> portals;

    @JsonProperty("version_count")
    private Integer versionCount;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    private Long organizationId;
}