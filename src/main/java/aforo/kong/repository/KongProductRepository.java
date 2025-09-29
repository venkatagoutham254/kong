package aforo.kong.repository;

import aforo.kong.entity.KongProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KongProductRepository extends JpaRepository<KongProduct, Long> {
    
    // Find all products for a specific organization
    List<KongProduct> findByOrganizationId(Long organizationId);
    
    // Find product by external ID (Kong product ID) and organization
    Optional<KongProduct> findByIdAndOrganizationId(String id, Long organizationId);
    
    // Find product by internal ID and organization
    Optional<KongProduct> findByInternalIdAndOrganizationId(Long internalId, Long organizationId);
    
    // Delete by external ID and organization (for security)
    void deleteByIdAndOrganizationId(String id, Long organizationId);
    
    // Delete by internal ID and organization
    void deleteByInternalIdAndOrganizationId(Long internalId, Long organizationId);
    
    // Check if product exists for organization by external ID
    boolean existsByIdAndOrganizationId(String id, Long organizationId);
    
    // Check if product exists for organization by internal ID
    boolean existsByInternalIdAndOrganizationId(Long internalId, Long organizationId);
}
