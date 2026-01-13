package aforo.kong.repository;

import aforo.kong.entity.KonnectRouteMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KonnectRouteMapRepository extends JpaRepository<KonnectRouteMap, Long> {
    
    Optional<KonnectRouteMap> findByOrganizationIdAndControlPlaneIdAndKongRouteId(
            Long organizationId, String controlPlaneId, String kongRouteId);
    
    List<KonnectRouteMap> findByOrganizationIdAndControlPlaneId(
            Long organizationId, String controlPlaneId);
    
    List<KonnectRouteMap> findByOrganizationIdAndKongServiceId(
            Long organizationId, String kongServiceId);
    
    List<KonnectRouteMap> findByOrganizationIdAndStatus(
            Long organizationId, String status);
    
    List<KonnectRouteMap> findByOrganizationId(Long organizationId);
}
