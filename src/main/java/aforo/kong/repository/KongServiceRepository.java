package aforo.kong.repository;

import aforo.kong.entity.KongService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface KongServiceRepository extends JpaRepository<KongService, Long> {
    
    Optional<KongService> findByIdAndOrganizationId(String externalId, Long organizationId);
    
    List<KongService> findByOrganizationId(Long organizationId);
    
    boolean existsByIdAndOrganizationId(String externalId, Long organizationId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM KongService k WHERE k.id = :externalId AND k.organizationId = :organizationId")
    void deleteByIdAndOrganizationId(@Param("externalId") String externalId, @Param("organizationId") Long organizationId);
    
    @Query("SELECT k FROM KongService k LEFT JOIN FETCH k.routes WHERE k.organizationId = :organizationId")
    List<KongService> findAllWithRoutesByOrganizationId(@Param("organizationId") Long organizationId);
    
    @Query("SELECT k FROM KongService k WHERE k.organizationId = :organizationId AND k.enabled = true")
    List<KongService> findEnabledServicesByOrganizationId(@Param("organizationId") Long organizationId);
    
    @Query("SELECT k FROM KongService k WHERE k.tags LIKE %:tag% AND k.organizationId = :organizationId")
    List<KongService> findByTagAndOrganizationId(@Param("tag") String tag, @Param("organizationId") Long organizationId);
}
