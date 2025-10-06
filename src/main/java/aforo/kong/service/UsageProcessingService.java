package aforo.kong.service;

import aforo.kong.entity.UsageRecord;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface UsageProcessingService {
    
    /**
     * Process unprocessed usage records and calculate costs
     */
    void processUnprocessedRecords();
    
    /**
     * Calculate cost for a usage record based on consumer's pricing plan
     */
    void calculateCost(UsageRecord record);
    
    /**
     * Generate billing summary for a consumer in a time period
     */
    Map<String, Object> generateBillingSummary(String consumerId, Instant startTime, Instant endTime);
    
    /**
     * Get usage statistics for a consumer
     */
    Map<String, Object> getUsageStatistics(String consumerId, Instant startTime, Instant endTime);
    
    /**
     * Get top consumers by usage
     */
    List<Map<String, Object>> getTopConsumersByUsage(int limit, Instant startTime, Instant endTime);
    
    /**
     * Get top services by usage
     */
    List<Map<String, Object>> getTopServicesByUsage(int limit, Instant startTime, Instant endTime);
    
    /**
     * Check if consumer is approaching quota limits
     */
    boolean isApproachingQuota(String consumerId, double threshold);
    
    /**
     * Deduct from prepaid wallet
     */
    void deductFromWallet(String consumerId, double amount);
    
    /**
     * Top up prepaid wallet
     */
    void topUpWallet(String consumerId, double amount);
}
