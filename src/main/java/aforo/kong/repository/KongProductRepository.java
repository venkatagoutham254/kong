package aforo.kong.repository;

import aforo.kong.entity.KongProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KongProductRepository extends JpaRepository<KongProduct, String> {
}
