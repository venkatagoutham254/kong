package aforo.kong.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ConnectRequestDTO {
    
    @NotNull(message = "Environment is required")
    private String environment;  // "konnect" or "self-managed"
    
    @NotBlank(message = "Admin API URL is required")
    @JsonProperty("adminApiUrl")
    private String adminApiUrl;
    
    private String workspace;  // Kong workspace or Konnect control plane
    
    private String token;  // Bearer token or Personal Access Token
    
    @JsonProperty("mtlsCertPem")
    private String mtlsCertPem;  // mTLS certificate in PEM format
    
    @JsonProperty("mtlsKeyPem")
    private String mtlsKeyPem;  // mTLS private key in PEM format
    
    private ScopeConfig scope;
    
    @JsonProperty("autoInstall")
    private AutoInstallConfig autoInstall;
    
    @JsonProperty("eventHooks")
    private EventHooksConfig eventHooks;
    
    @Data
    public static class ScopeConfig {
        private List<String> workspaces;
        private List<String> services;  // Service IDs or names to import
    }
    
    @Data
    public static class AutoInstallConfig {
        @JsonProperty("correlationId")
        private boolean correlationId = true;
        
        @JsonProperty("httpLog")
        private boolean httpLog = true;
        
        @JsonProperty("rateLimitingAdvanced")
        private boolean rateLimitingAdvanced = true;
    }
    
    @Data
    public static class EventHooksConfig {
        private boolean crud = true;  // CRUD events for services/routes/consumers
        private boolean exceed = true;  // Rate limit exceeded events
    }
}
