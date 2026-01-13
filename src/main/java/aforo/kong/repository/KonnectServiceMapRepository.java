package aforo.kong.repository;

import aforo.kong.entity.KonnectServiceMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KonnectServiceMapRepository extends JpaRepository<KonnectServiceMap, Long> {
    
    Optional<KonnectServiceMap> findByOrganizationIdAndControlPlaneIdAndKongServiceId(
            Long organizationId, String controlPlaneId, String kongServiceId);
    
    List<KonnectServiceMap> findByOrganizationIdAndControlPlaneId(
            Long organizationId, String controlPlaneId);
    
    List<KonnectServiceMap> findByOrganizationIdAndStatus(
            Long organizationId, String status);
    
    List<KonnectServiceMap> findByOrganizationId(Long organizationId);
}
