package aforo.kong.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class CatalogSyncResponseDTO {
    
    private SyncStatus status;
    private Instant syncStartTime;
    private Instant syncEndTime;
    private Long durationMs;
    
    private SyncStats services;
    private SyncStats routes;
    private SyncStats consumers;
    
    private List<SyncError> errors;
    
    public enum SyncStatus {
        STARTED,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        PARTIAL
    }
    
    @Data
    public static class SyncStats {
        private Integer fetched;
        private Integer created;
        private Integer updated;
        private Integer deleted;
        private Integer failed;
        private Integer skipped;
    }
    
    @Data
    public static class SyncError {
        private String entityType;
        private String entityId;
        private String error;
        private Instant timestamp;
    }
}
