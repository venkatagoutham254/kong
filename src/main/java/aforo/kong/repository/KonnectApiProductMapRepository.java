package aforo.kong.repository;

import aforo.kong.entity.KonnectApiProductMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KonnectApiProductMapRepository extends JpaRepository<KonnectApiProductMap, Long> {
    Optional<KonnectApiProductMap> findByOrgIdAndKonnectApiProductId(Long orgId, String konnectApiProductId);
    List<KonnectApiProductMap> findByOrgIdAndStatus(Long orgId, String status);
    List<KonnectApiProductMap> findByOrgId(Long orgId);
}
