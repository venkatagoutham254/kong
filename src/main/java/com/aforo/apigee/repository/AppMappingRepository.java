package com.aforo.apigee.repository;

import com.aforo.apigee.model.AppMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppMappingRepository extends JpaRepository<AppMapping, Long> {
    Optional<AppMapping> findByApigeeAppIdAndApiProduct(String apigeeAppId, String apiProduct);
}
