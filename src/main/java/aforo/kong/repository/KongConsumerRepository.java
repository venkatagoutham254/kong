package aforo.kong.repository;

import aforo.kong.entity.KongConsumer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface KongConsumerRepository extends JpaRepository<KongConsumer, Long> {
    
    Optional<KongConsumer> findByIdAndOrganizationId(String externalId, Long organizationId);
    
    Optional<KongConsumer> findByUsernameAndOrganizationId(String username, Long organizationId);
    
    Optional<KongConsumer> findByCustomIdAndOrganizationId(String customId, Long organizationId);
    
    List<KongConsumer> findByOrganizationId(Long organizationId);
    
    List<KongConsumer> findByPlanIdAndOrganizationId(String planId, Long organizationId);
    
    List<KongConsumer> findByStatusAndOrganizationId(String status, Long organizationId);
    
    boolean existsByIdAndOrganizationId(String externalId, Long organizationId);
    
    boolean existsByUsernameAndOrganizationId(String username, Long organizationId);
    
    boolean existsByCustomIdAndOrganizationId(String customId, Long organizationId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM KongConsumer c WHERE c.id = :externalId AND c.organizationId = :organizationId")
    void deleteByIdAndOrganizationId(@Param("externalId") String externalId, @Param("organizationId") Long organizationId);
    
    @Query("SELECT c FROM KongConsumer c WHERE c.walletBalance < :threshold AND c.organizationId = :organizationId")
    List<KongConsumer> findLowBalanceConsumers(@Param("threshold") Double threshold, @Param("organizationId") Long organizationId);
    
    @Query("SELECT c FROM KongConsumer c WHERE c.consumerGroups LIKE %:group% AND c.organizationId = :organizationId")
    List<KongConsumer> findByConsumerGroup(@Param("group") String group, @Param("organizationId") Long organizationId);
    
    @Modifying
    @Transactional
    @Query("UPDATE KongConsumer c SET c.walletBalance = c.walletBalance + :amount WHERE c.id = :consumerId AND c.organizationId = :organizationId")
    int updateWalletBalance(@Param("consumerId") String consumerId, @Param("amount") Double amount, @Param("organizationId") Long organizationId);
    
    @Modifying
    @Transactional
    @Query("UPDATE KongConsumer c SET c.status = :status WHERE c.id = :consumerId AND c.organizationId = :organizationId")
    int updateStatus(@Param("consumerId") String consumerId, @Param("status") String status, @Param("organizationId") Long organizationId);
}
