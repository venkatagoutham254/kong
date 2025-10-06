package aforo.kong.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KongRouteDTO {
    private String id;
    private String name;
    private List<String> protocols;
    private List<String> methods;
    private List<String> hosts;
    private List<String> paths;
    private Map<String, List<String>> headers;
    
    @JsonProperty("https_redirect_status_code")
    private Integer httpsRedirectStatusCode;
    
    @JsonProperty("regex_priority")
    private Integer regexPriority;
    
    @JsonProperty("strip_path")
    private Boolean stripPath;
    
    @JsonProperty("preserve_host")
    private Boolean preserveHost;
    
    @JsonProperty("request_buffering")
    private Boolean requestBuffering;
    
    @JsonProperty("response_buffering")
    private Boolean responseBuffering;
    
    private List<String> snis;
    private List<Map<String, Object>> sources;
    private List<Map<String, Object>> destinations;
    private List<String> tags;
    
    @JsonProperty("service")
    private ServiceReference service;
    
    @JsonProperty("created_at")
    private Instant createdAt;
    
    @JsonProperty("updated_at")
    private Instant updatedAt;
    
    // For internal use
    private Long organizationId;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ServiceReference {
        private String id;
    }
}
