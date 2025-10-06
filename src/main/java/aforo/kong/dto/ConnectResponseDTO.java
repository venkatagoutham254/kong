package aforo.kong.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ConnectResponseDTO {
    
    @JsonProperty("connectionId")
    private String connectionId;
    
    private String status;  // "connected", "pending", "failed"
    
    @JsonProperty("servicesDiscovered")
    private Integer servicesDiscovered;
    
    @JsonProperty("routesDiscovered")
    private Integer routesDiscovered;
    
    @JsonProperty("consumersDiscovered")
    private Integer consumersDiscovered;
    
    private String message;  // Error or success message
    
    @JsonProperty("webhookUrl")
    private String webhookUrl;  // Webhook URL for Kong to send events to
    
    @JsonProperty("ingestUrl")
    private String ingestUrl;  // URL for HTTP Log plugin to send events to
}
