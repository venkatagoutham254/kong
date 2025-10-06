package aforo.kong.repository;

import aforo.kong.entity.KongRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface KongRouteRepository extends JpaRepository<KongRoute, Long> {
    
    Optional<KongRoute> findByIdAndOrganizationId(String externalId, Long organizationId);
    
    List<KongRoute> findByOrganizationId(Long organizationId);
    
    List<KongRoute> findByKongServiceExternalIdAndOrganizationId(String serviceExternalId, Long organizationId);
    
    boolean existsByIdAndOrganizationId(String externalId, Long organizationId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM KongRoute r WHERE r.id = :externalId AND r.organizationId = :organizationId")
    void deleteByIdAndOrganizationId(@Param("externalId") String externalId, @Param("organizationId") Long organizationId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM KongRoute r WHERE r.kongServiceExternalId = :serviceId AND r.organizationId = :organizationId")
    void deleteByServiceIdAndOrganizationId(@Param("serviceId") String serviceId, @Param("organizationId") Long organizationId);
    
    @Query("SELECT r FROM KongRoute r WHERE r.paths LIKE %:path% AND r.organizationId = :organizationId")
    List<KongRoute> findByPathContainingAndOrganizationId(@Param("path") String path, @Param("organizationId") Long organizationId);
    
    @Query("SELECT r FROM KongRoute r WHERE r.methods LIKE %:method% AND r.organizationId = :organizationId")
    List<KongRoute> findByMethodAndOrganizationId(@Param("method") String method, @Param("organizationId") Long organizationId);
}
