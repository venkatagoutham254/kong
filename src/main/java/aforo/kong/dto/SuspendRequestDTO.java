package aforo.kong.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SuspendRequestDTO {
    
    @NotBlank(message = "Consumer ID is required")
    @JsonProperty("consumerId")
    private String consumerId;
    
    private String mode = "group";  // "group" or "termination"
    
    private String reason;  // Reason for suspension
    
    @JsonProperty("suspendedGroupName")
    private String suspendedGroupName = "suspended";  // Name of the suspended group
}
