package aforo.kong.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for Kong HTTP Log plugin events
 * This represents the data Kong sends for each API request
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KongEventDTO {
    
    @JsonProperty("kong_request_id")
    private String kongRequestId;  // X-Kong-Request-ID header
    
    @JsonProperty("correlation_id")
    private String correlationId;  // X-Correlation-ID if using correlation-id plugin
    
    private Instant timestamp;
    
    private ServiceInfo service;
    private RouteInfo route;
    private ConsumerInfo consumer;
    private RequestInfo request;
    private ResponseInfo response;
    private UpstreamInfo upstream;
    private LatenciesInfo latencies;
    
    @JsonProperty("client_ip")
    private String clientIp;
    
    @JsonProperty("started_at")
    private Long startedAt;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ServiceInfo {
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
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RouteInfo {
        private String id;
        private String name;
        private String[] paths;
        private String[] methods;
        private String[] hosts;
        private String[] protocols;
        @JsonProperty("strip_path")
        private Boolean stripPath;
        @JsonProperty("preserve_host")
        private Boolean preserveHost;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConsumerInfo {
        private String id;
        private String username;
        @JsonProperty("custom_id")
        private String customId;
        private String[] tags;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RequestInfo {
        private String method;
        private String path;
        private String url;
        private Long size;  // Request body size in bytes
        private String uri;
        private Map<String, String[]> headers;
        @JsonProperty("querystring")
        private Map<String, String[]> queryString;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseInfo {
        private Integer status;
        private Long size;  // Response body size in bytes
        private Map<String, String[]> headers;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UpstreamInfo {
        private String ip;
        private Integer port;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LatenciesInfo {
        private Long request;  // Total request latency
        private Long kong;     // Kong processing latency
        private Long proxy;    // Upstream proxy latency
    }
}
