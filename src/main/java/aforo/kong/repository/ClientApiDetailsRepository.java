package aforo.kong.repository;

import aforo.kong.entity.ClientApiDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientApiDetailsRepository extends JpaRepository<ClientApiDetails, Long> {
    Optional<ClientApiDetails> findByIdAndOrganizationId(Long id, Long organizationId);
    Optional<ClientApiDetails> findByOrganizationIdAndEnvironment(Long organizationId, String environment);
    List<ClientApiDetails> findByEnvironmentAndConnectionStatus(String environment, String connectionStatus);
}
