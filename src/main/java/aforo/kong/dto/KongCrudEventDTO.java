package aforo.kong.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for Kong Event Hooks (CRUD and rate limit events)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KongCrudEventDTO {
    
    private String source;  // "crud", "rate-limiting-advanced"
    
    private String event;  // "create", "update", "delete", "exceed"
    
    private String entity;  // "services", "routes", "consumers", "plugins"
    
    private Map<String, Object> before;  // Entity state before change (for updates)
    
    private Map<String, Object> after;  // Entity state after change
    
    private Map<String, Object> data;  // Current entity data
    
    @JsonProperty("workspace_id")
    private String workspaceId;
    
    @JsonProperty("workspace_name")
    private String workspaceName;
    
    private Instant timestamp;
    
    // For rate limit exceeded events
    @JsonProperty("consumer_id")
    private String consumerId;
    
    @JsonProperty("service_id")
    private String serviceId;
    
    @JsonProperty("route_id")
    private String routeId;
    
    private String identifier;  // Rate limit identifier
    
    private String window;  // Rate limit window that was exceeded
    
    private Long limit;  // The limit that was exceeded
}
