package aforo.kong.dto.konnect;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KonnectConnectionRequestDTO {
    private String name;
    private String description;
    
    @JsonProperty("base_url")
    private String baseUrl;
    
    private String endpoint;
    
    @JsonProperty("auth_token")
    private String authToken;
    
    @JsonProperty("control_plane_id")
    private String controlPlaneId;
    
    private String region;
}
