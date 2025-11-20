package com.aforo.apigee.repository;

import com.aforo.apigee.model.ImportedProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImportedProductRepository extends JpaRepository<ImportedProduct, Long> {
    Optional<ImportedProduct> findByApigeeName(String apigeeName);
}
