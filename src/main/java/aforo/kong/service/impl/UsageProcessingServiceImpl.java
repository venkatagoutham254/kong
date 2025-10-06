package aforo.kong.service.impl;

import aforo.kong.entity.KongConsumer;
import aforo.kong.entity.PricingPlan;
import aforo.kong.entity.UsageRecord;
import aforo.kong.repository.KongConsumerRepository;
import aforo.kong.repository.PricingPlanRepository;
import aforo.kong.repository.UsageRecordRepository;
import aforo.kong.service.UsageProcessingService;
import aforo.kong.tenant.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class UsageProcessingServiceImpl implements UsageProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(UsageProcessingServiceImpl.class);
    
    private final UsageRecordRepository usageRecordRepository;
    private final KongConsumerRepository consumerRepository;
    private final PricingPlanRepository pricingPlanRepository;
    private final ObjectMapper objectMapper;
    
    public UsageProcessingServiceImpl(
            UsageRecordRepository usageRecordRepository,
            KongConsumerRepository consumerRepository,
            PricingPlanRepository pricingPlanRepository,
            ObjectMapper objectMapper) {
        this.usageRecordRepository = usageRecordRepository;
        this.consumerRepository = consumerRepository;
        this.pricingPlanRepository = pricingPlanRepository;
        this.objectMapper = objectMapper;
    }
    
    @Override
    @Scheduled(fixedDelay = 60000) // Process every minute
    public void processUnprocessedRecords() {
        Long organizationId = null;
        try {
            organizationId = TenantContext.require();
        } catch (Exception e) {
            // Skip if no tenant context
            return;
        }
        
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(1, ChronoUnit.HOURS);
        
        List<UsageRecord> unprocessedRecords = usageRecordRepository.findUnprocessedRecords(
            organizationId, startTime, endTime);
        
        logger.info("Processing {} unprocessed usage records", unprocessedRecords.size());
        
        for (UsageRecord record : unprocessedRecords) {
            try {
                calculateCost(record);
                record.setProcessed(true);
                usageRecordRepository.save(record);
            } catch (Exception e) {
                logger.error("Failed to process usage record: {}", record.getUsageId(), e);
            }
        }
    }
    
    @Override
    public void calculateCost(UsageRecord record) {
        if (record.getConsumerId() == null) {
            logger.warn("Cannot calculate cost for usage record without consumer ID");
            return;
        }
        
        Long organizationId = record.getOrganizationId();
        
        // Get consumer and their pricing plan
        Optional<KongConsumer> consumerOpt = consumerRepository.findByIdAndOrganizationId(
            record.getConsumerId(), organizationId);
        
        if (consumerOpt.isEmpty()) {
            logger.warn("Consumer not found: {}", record.getConsumerId());
            return;
        }
        
        KongConsumer consumer = consumerOpt.get();
        if (consumer.getPlanId() == null) {
            logger.debug("Consumer {} has no pricing plan, using default", consumer.getId());
            record.setUnitPrice(0.0);
            record.setTotalCost(0.0);
            return;
        }
        
        Optional<PricingPlan> planOpt = pricingPlanRepository.findByPlanNameAndOrganizationId(
            consumer.getPlanId(), organizationId);
        
        if (planOpt.isEmpty()) {
            logger.warn("Pricing plan not found: {}", consumer.getPlanId());
            return;
        }
        
        PricingPlan plan = planOpt.get();
        
        // Calculate cost based on metric type
        double cost = 0.0;
        double unitPrice = 0.0;
        
        String metricType = record.getMetricType();
        if (metricType == null) {
            metricType = "calls";
        }
        
        try {
            // Parse metrics configuration
            if (plan.getMetrics() != null) {
                List<Map<String, Object>> metrics = objectMapper.readValue(
                    plan.getMetrics(), 
                    new TypeReference<List<Map<String, Object>>>() {}
                );
                
                for (Map<String, Object> metric : metrics) {
                    if (metricType.equals(metric.get("type"))) {
                        unitPrice = calculatePriceForMetric(record, metric);
                        break;
                    }
                }
            }
            
            // Calculate total cost
            double units = record.getBillableUnits() != null ? record.getBillableUnits() : 1.0;
            cost = units * unitPrice;
            
            // Apply minimum fee if applicable
            if (plan.getMinimumFee() != null && cost < plan.getMinimumFee()) {
                cost = plan.getMinimumFee();
            }
            
            record.setUnitPrice(unitPrice);
            record.setTotalCost(cost);
            record.setCurrency(plan.getCurrency());
            
            // Deduct from wallet if prepaid
            if (Boolean.TRUE.equals(plan.getPrepaidCreditsEnabled()) && consumer.getWalletBalance() != null) {
                deductFromWallet(consumer.getId(), cost);
            }
            
        } catch (Exception e) {
            logger.error("Failed to calculate cost for record {}", record.getUsageId(), e);
        }
    }
    
    @Override
    public Map<String, Object> generateBillingSummary(String consumerId, Instant startTime, Instant endTime) {
        Long organizationId = TenantContext.require();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("consumerId", consumerId);
        summary.put("startTime", startTime);
        summary.put("endTime", endTime);
        
        // Get total calls
        Long totalCalls = usageRecordRepository.countByConsumerInPeriod(
            consumerId, organizationId, startTime, endTime);
        summary.put("totalCalls", totalCalls);
        
        // Get total cost
        Double totalCost = usageRecordRepository.sumCostByConsumerInPeriod(
            consumerId, organizationId, startTime, endTime);
        summary.put("totalCost", totalCost != null ? totalCost : 0.0);
        
        // Get total bandwidth
        Long totalBandwidth = usageRecordRepository.sumBandwidthByConsumerInPeriod(
            consumerId, organizationId, startTime, endTime);
        summary.put("totalBandwidthBytes", totalBandwidth != null ? totalBandwidth : 0L);
        
        // Get consumer details
        consumerRepository.findByIdAndOrganizationId(consumerId, organizationId)
            .ifPresent(consumer -> {
                summary.put("username", consumer.getUsername());
                summary.put("planId", consumer.getPlanId());
                summary.put("walletBalance", consumer.getWalletBalance());
                summary.put("status", consumer.getStatus());
            });
        
        return summary;
    }
    
    @Override
    public Map<String, Object> getUsageStatistics(String consumerId, Instant startTime, Instant endTime) {
        Long organizationId = TenantContext.require();
        
        List<UsageRecord> records = usageRecordRepository.findByConsumerIdAndOrganizationIdAndTimestampBetween(
            consumerId, organizationId, startTime, endTime);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", records.size());
        
        // Calculate success rate
        long successCount = records.stream()
            .filter(r -> r.getResponseStatus() != null && r.getResponseStatus() < 400)
            .count();
        stats.put("successRate", records.isEmpty() ? 0 : (double) successCount / records.size() * 100);
        
        // Calculate average latency
        double avgLatency = records.stream()
            .filter(r -> r.getResponseLatency() != null)
            .mapToLong(UsageRecord::getResponseLatency)
            .average()
            .orElse(0);
        stats.put("averageLatencyMs", avgLatency);
        
        // Group by service
        Map<String, Long> byService = records.stream()
            .filter(r -> r.getServiceName() != null)
            .collect(Collectors.groupingBy(
                UsageRecord::getServiceName,
                Collectors.counting()
            ));
        stats.put("requestsByService", byService);
        
        // Group by status code
        Map<Integer, Long> byStatus = records.stream()
            .filter(r -> r.getResponseStatus() != null)
            .collect(Collectors.groupingBy(
                UsageRecord::getResponseStatus,
                Collectors.counting()
            ));
        stats.put("requestsByStatus", byStatus);
        
        return stats;
    }
    
    @Override
    public List<Map<String, Object>> getTopConsumersByUsage(int limit, Instant startTime, Instant endTime) {
        Long organizationId = TenantContext.require();
        
        // This would be better as a custom query, but for now we'll do it in memory
        List<UsageRecord> records = usageRecordRepository.findByOrganizationId(
            organizationId, 
            org.springframework.data.domain.PageRequest.of(0, 10000)
        ).getContent();
        
        Map<String, Long> consumerCounts = records.stream()
            .filter(r -> r.getTimestamp().isAfter(startTime) && r.getTimestamp().isBefore(endTime))
            .filter(r -> r.getConsumerUsername() != null)
            .collect(Collectors.groupingBy(
                UsageRecord::getConsumerUsername,
                Collectors.counting()
            ));
        
        return consumerCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> Map.<String, Object>of(
                "consumer", entry.getKey(),
                "requestCount", entry.getValue()
            ))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Map<String, Object>> getTopServicesByUsage(int limit, Instant startTime, Instant endTime) {
        Long organizationId = TenantContext.require();
        
        List<UsageRecord> records = usageRecordRepository.findByOrganizationId(
            organizationId, 
            org.springframework.data.domain.PageRequest.of(0, 10000)
        ).getContent();
        
        Map<String, Long> serviceCounts = records.stream()
            .filter(r -> r.getTimestamp().isAfter(startTime) && r.getTimestamp().isBefore(endTime))
            .filter(r -> r.getServiceName() != null)
            .collect(Collectors.groupingBy(
                UsageRecord::getServiceName,
                Collectors.counting()
            ));
        
        return serviceCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> Map.<String, Object>of(
                "service", entry.getKey(),
                "requestCount", entry.getValue()
            ))
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean isApproachingQuota(String consumerId, double threshold) {
        Long organizationId = TenantContext.require();
        
        Optional<KongConsumer> consumerOpt = consumerRepository.findByIdAndOrganizationId(
            consumerId, organizationId);
        
        if (consumerOpt.isEmpty() || consumerOpt.get().getPlanId() == null) {
            return false;
        }
        
        KongConsumer consumer = consumerOpt.get();
        Optional<PricingPlan> planOpt = pricingPlanRepository.findByPlanNameAndOrganizationId(
            consumer.getPlanId(), organizationId);
        
        if (planOpt.isEmpty()) {
            return false;
        }
        
        PricingPlan plan = planOpt.get();
        
        // Check monthly quota
        if (plan.getMaxRequestsPerMonth() != null) {
            Instant now = Instant.now();
            Instant monthStart = now.minus(30, ChronoUnit.DAYS);
            
            Long currentUsage = usageRecordRepository.countByConsumerInPeriod(
                consumerId, organizationId, monthStart, now);
            
            double usagePercent = (double) currentUsage / plan.getMaxRequestsPerMonth() * 100;
            return usagePercent >= threshold;
        }
        
        return false;
    }
    
    @Override
    @Transactional
    public void deductFromWallet(String consumerId, double amount) {
        Long organizationId = TenantContext.require();
        int updated = consumerRepository.updateWalletBalance(consumerId, -amount, organizationId);
        
        if (updated > 0) {
            logger.debug("Deducted {} from consumer {} wallet", amount, consumerId);
            
            // Check if wallet is now negative and suspend if needed
            consumerRepository.findByIdAndOrganizationId(consumerId, organizationId)
                .ifPresent(consumer -> {
                    if (consumer.getWalletBalance() != null && consumer.getWalletBalance() <= 0) {
                        consumer.setStatus("suspended");
                        consumerRepository.save(consumer);
                        logger.warn("Consumer {} suspended due to insufficient wallet balance", consumerId);
                    }
                });
        }
    }
    
    @Override
    @Transactional
    public void topUpWallet(String consumerId, double amount) {
        Long organizationId = TenantContext.require();
        int updated = consumerRepository.updateWalletBalance(consumerId, amount, organizationId);
        
        if (updated > 0) {
            logger.info("Added {} to consumer {} wallet", amount, consumerId);
            
            // Resume consumer if was suspended
            consumerRepository.findByIdAndOrganizationId(consumerId, organizationId)
                .ifPresent(consumer -> {
                    if ("suspended".equals(consumer.getStatus()) && 
                        consumer.getWalletBalance() != null && 
                        consumer.getWalletBalance() > 0) {
                        consumer.setStatus("active");
                        consumerRepository.save(consumer);
                        logger.info("Consumer {} resumed after wallet top-up", consumerId);
                    }
                });
        }
    }
    
    private double calculatePriceForMetric(UsageRecord record, Map<String, Object> metric) {
        // Check if it's tiered pricing
        if (metric.containsKey("tiers")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tiers = (List<Map<String, Object>>) metric.get("tiers");
            
            // Find the applicable tier based on current usage
            // This is simplified - in production, you'd track cumulative usage
            for (Map<String, Object> tier : tiers) {
                @SuppressWarnings("unused") // TODO: Implement proper tiered pricing logic
                Number from = (Number) tier.get("from");
                @SuppressWarnings("unused") // TODO: Implement proper tiered pricing logic  
                Number to = (Number) tier.get("to");
                Number price = (Number) tier.get("price");
                
                if (price != null) {
                    return price.doubleValue();
                }
            }
        }
        
        // Simple flat pricing
        if (metric.containsKey("price")) {
            Number price = (Number) metric.get("price");
            return price != null ? price.doubleValue() : 0.0;
        }
        
        return 0.0;
    }
}
