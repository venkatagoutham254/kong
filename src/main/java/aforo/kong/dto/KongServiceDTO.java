package aforo.kong.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KongServiceDTO {
    private String id;
    private String name;
    private String host;
    private Integer port;
    private String protocol;
    private String path;
    private Integer retries;
    
    @JsonProperty("connect_timeout")
    private Integer connectTimeout;
    
    @JsonProperty("write_timeout")
    private Integer writeTimeout;
    
    @JsonProperty("read_timeout")
    private Integer readTimeout;
    
    private List<String> tags;
    private Boolean enabled;
    
    @JsonProperty("ca_certificates")
    private List<String> caCertificates;
    
    @JsonProperty("client_certificate")
    private Map<String, Object> clientCertificate;
    
    @JsonProperty("tls_verify")
    private Boolean tlsVerify;
    
    @JsonProperty("tls_verify_depth")
    private Integer tlsVerifyDepth;
    
    @JsonProperty("created_at")
    private Instant createdAt;
    
    @JsonProperty("updated_at")
    private Instant updatedAt;
    
    // For internal use
    private Long organizationId;
}
