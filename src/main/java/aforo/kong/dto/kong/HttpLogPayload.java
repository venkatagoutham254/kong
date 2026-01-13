package aforo.kong.dto.kong;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HttpLogPayload {
    
    @JsonProperty("started_at")
    private Long startedAt;
    
    private ServiceInfo service;
    private RouteInfo route;
    private ConsumerInfo consumer;
    private RequestInfo request;
    private ResponseInfo response;
    private UpstreamInfo upstream;
    private LatenciesInfo latencies;
    
    @JsonProperty("kong_request_id")
    private String kongRequestId;
    
    @JsonProperty("client_ip")
    private String clientIp;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ServiceInfo {
        private String id;
        private String name;
        private String host;
        private Integer port;
        private String path;
        private String protocol;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RouteInfo {
        private String id;
        private String name;
        private String[] paths;
        private String[] methods;
        private String[] hosts;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConsumerInfo {
        private String id;
        private String username;
        @JsonProperty("custom_id")
        private String customId;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RequestInfo {
        private String method;
        @JsonAlias({"uri", "path"})
        private String path;
        private String url;
        private Integer size;
        private String id;
        private Map<String, String[]> headers;
        @JsonProperty("querystring")
        private Map<String, String[]> queryString;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseInfo {
        private Integer status;
        private Integer size;
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
        private Integer kong;
        private Integer proxy;
        private Integer request;
    }
}
