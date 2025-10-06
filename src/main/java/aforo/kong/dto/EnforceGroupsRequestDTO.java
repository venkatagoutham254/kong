package aforo.kong.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class EnforceGroupsRequestDTO {
    
    @NotNull(message = "Mappings are required")
    private List<PlanGroupMapping> mappings;
    
    @Data
    public static class PlanGroupMapping {
        @NotNull(message = "Plan ID is required")
        @JsonProperty("planId")
        private String planId;
        
        @JsonProperty("consumerGroupName")
        private String consumerGroupName;
        
        private List<RateLimit> limits;
    }
    
    @Data
    public static class RateLimit {
        @NotNull(message = "Window is required")
        private String window;  // "second", "minute", "hour", "day", "month"
        
        @NotNull(message = "Limit is required")
        private Long limit;  // Number of requests allowed in this window
        
        private String identifier;  // Optional: consumer, credential, ip, service, header, path
    }
}
