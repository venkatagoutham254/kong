package aforo.kong.dto.konnect;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class KonnectRuntimeSyncPreviewDTO {
    private List<ServiceChange> addedServices;
    private List<ServiceChange> removedServices;
    private List<ServiceChange> changedServices;
    
    private List<RouteChange> addedRoutes;
    private List<RouteChange> removedRoutes;
    private List<RouteChange> changedRoutes;
    
    @Data
    @Builder
    public static class ServiceChange {
        private String kongServiceId;
        private String name;
        private String host;
        private String protocol;
        private List<String> tags;
    }
    
    @Data
    @Builder
    public static class RouteChange {
        private String kongRouteId;
        private String kongServiceId;
        private String name;
        private List<String> paths;
        private List<String> methods;
        private List<String> hosts;
    }
}
