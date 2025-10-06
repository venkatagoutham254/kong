package aforo.kong.repository;

import aforo.kong.entity.PricingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingPlanRepository extends JpaRepository<PricingPlan, Long> {
    
    Optional<PricingPlan> findByPlanNameAndOrganizationId(String planName, Long organizationId);
    
    Optional<PricingPlan> findByKongConsumerGroupAndOrganizationId(String consumerGroup, Long organizationId);
    
    List<PricingPlan> findByOrganizationId(Long organizationId);
    
    List<PricingPlan> findByStatusAndOrganizationId(String status, Long organizationId);
    
    boolean existsByPlanNameAndOrganizationId(String planName, Long organizationId);
    
    @Query("SELECT p FROM PricingPlan p WHERE p.organizationId = :organizationId AND p.status = 'active' ORDER BY p.basePrice ASC")
    List<PricingPlan> findActivePlansByOrganizationIdOrderByPrice(@Param("organizationId") Long organizationId);
    
    @Query("SELECT p FROM PricingPlan p WHERE p.prepaidCreditsEnabled = true AND p.organizationId = :organizationId")
    List<PricingPlan> findPrepaidPlans(@Param("organizationId") Long organizationId);
}
