package aforo.kong.dto.konnect;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class KonnectServiceDTO {
    private String id;
    private String name;
    private String host;
    private Integer port;
    private String path;
    private String protocol;
    private List<String> tags;
    
    @JsonProperty("connect_timeout")
    private Integer connectTimeout;
    
    @JsonProperty("read_timeout")
    private Integer readTimeout;
    
    @JsonProperty("write_timeout")
    private Integer writeTimeout;
    
    private Integer retries;
    
    @JsonProperty("created_at")
    private Long createdAt;
    
    @JsonProperty("updated_at")
    private Long updatedAt;
    
    // Additional fields that might come from Konnect
    private Map<String, Object> extras;
}
