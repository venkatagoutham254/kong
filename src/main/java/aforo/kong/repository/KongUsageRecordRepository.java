package aforo.kong.repository;

import aforo.kong.entity.KongUsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface KongUsageRecordRepository extends JpaRepository<KongUsageRecord, Long> {
    
    Optional<KongUsageRecord> findByOrganizationIdAndCorrelationId(
            Long organizationId, String correlationId);
    
    List<KongUsageRecord> findByOrganizationIdAndProcessedFalse(Long organizationId);
    
    List<KongUsageRecord> findByOrganizationIdAndTimestampBetween(
            Long organizationId, Instant start, Instant end);
    
    List<KongUsageRecord> findByProcessedFalseAndCreatedAtBefore(Instant cutoff);
}
