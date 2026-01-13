package aforo.kong.dto.konnect;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class KonnectRouteDTO {
    private String id;
    private String name;
    private List<String> protocols;
    private List<String> methods;
    private List<String> hosts;
    private List<String> paths;
    private List<String> headers;
    private List<String> tags;
    
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
    
    private Service service;
    
    @JsonProperty("created_at")
    private Long createdAt;
    
    @JsonProperty("updated_at")
    private Long updatedAt;
    
    @Data
    public static class Service {
        private String id;
    }
}
