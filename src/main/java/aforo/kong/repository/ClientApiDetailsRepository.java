package aforo.kong.repository;

import aforo.kong.entity.ClientApiDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientApiDetailsRepository extends JpaRepository<ClientApiDetails, Long> {
}
