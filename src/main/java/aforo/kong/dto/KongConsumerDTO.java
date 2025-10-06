package aforo.kong.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KongConsumerDTO {
    private String id;
    private String username;
    
    @JsonProperty("custom_id")
    private String customId;
    
    private List<String> tags;
    
    @JsonProperty("created_at")
    private Instant createdAt;
    
    @JsonProperty("updated_at")
    private Instant updatedAt;
    
    // Additional fields for Aforo integration
    @JsonProperty("consumer_groups")
    private List<String> consumerGroups;
    
    // For internal use
    private Long organizationId;
    private String planId;
    private Double walletBalance;
    private String currency;
    private String status;
    private String enforcementMode;
}
