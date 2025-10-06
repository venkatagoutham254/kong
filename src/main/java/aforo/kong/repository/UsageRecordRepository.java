package aforo.kong.repository;

import aforo.kong.entity.UsageRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {
    
    Optional<UsageRecord> findByCorrelationIdAndOrganizationId(String correlationId, Long organizationId);
    
    Page<UsageRecord> findByOrganizationId(Long organizationId, Pageable pageable);
    
    List<UsageRecord> findByConsumerIdAndOrganizationIdAndTimestampBetween(
            String consumerId, Long organizationId, Instant startTime, Instant endTime);
    
    List<UsageRecord> findByServiceIdAndOrganizationIdAndTimestampBetween(
            String serviceId, Long organizationId, Instant startTime, Instant endTime);
    
    List<UsageRecord> findByRouteIdAndOrganizationIdAndTimestampBetween(
            String routeId, Long organizationId, Instant startTime, Instant endTime);
    
    @Query("SELECT u FROM UsageRecord u WHERE u.organizationId = :organizationId AND u.timestamp >= :startTime AND u.timestamp < :endTime AND u.processed = false")
    List<UsageRecord> findUnprocessedRecords(@Param("organizationId") Long organizationId, 
                                            @Param("startTime") Instant startTime, 
                                            @Param("endTime") Instant endTime);
    
    @Query("SELECT u FROM UsageRecord u WHERE u.organizationId = :organizationId AND u.billed = false AND u.processed = true")
    List<UsageRecord> findUnbilledRecords(@Param("organizationId") Long organizationId);
    
    // Aggregate queries for analytics
    @Query("SELECT COUNT(u) FROM UsageRecord u WHERE u.consumerId = :consumerId AND u.organizationId = :organizationId AND u.timestamp >= :startTime AND u.timestamp < :endTime")
    Long countByConsumerInPeriod(@Param("consumerId") String consumerId, 
                                 @Param("organizationId") Long organizationId,
                                 @Param("startTime") Instant startTime, 
                                 @Param("endTime") Instant endTime);
    
    @Query("SELECT SUM(u.totalCost) FROM UsageRecord u WHERE u.consumerId = :consumerId AND u.organizationId = :organizationId AND u.timestamp >= :startTime AND u.timestamp < :endTime")
    Double sumCostByConsumerInPeriod(@Param("consumerId") String consumerId, 
                                     @Param("organizationId") Long organizationId,
                                     @Param("startTime") Instant startTime, 
                                     @Param("endTime") Instant endTime);
    
    @Query("SELECT SUM(u.responseSize + u.requestSize) FROM UsageRecord u WHERE u.consumerId = :consumerId AND u.organizationId = :organizationId AND u.timestamp >= :startTime AND u.timestamp < :endTime")
    Long sumBandwidthByConsumerInPeriod(@Param("consumerId") String consumerId, 
                                        @Param("organizationId") Long organizationId,
                                        @Param("startTime") Instant startTime, 
                                        @Param("endTime") Instant endTime);
    
    @Modifying
    @Transactional
    @Query("UPDATE UsageRecord u SET u.processed = true WHERE u.usageId IN :ids")
    int markAsProcessed(@Param("ids") List<Long> usageIds);
    
    @Modifying
    @Transactional
    @Query("UPDATE UsageRecord u SET u.billed = true, u.invoiceId = :invoiceId WHERE u.usageId IN :ids")
    int markAsBilled(@Param("ids") List<Long> usageIds, @Param("invoiceId") String invoiceId);
    
    // Dedupe check
    @Query("SELECT COUNT(u) > 0 FROM UsageRecord u WHERE u.correlationId = :correlationId AND u.organizationId = :organizationId")
    boolean existsByCorrelationIdAndOrganizationId(@Param("correlationId") String correlationId, @Param("organizationId") Long organizationId);
}
