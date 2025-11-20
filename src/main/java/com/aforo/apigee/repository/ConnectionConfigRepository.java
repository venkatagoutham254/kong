package com.aforo.apigee.repository;

import com.aforo.apigee.model.ConnectionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConnectionConfigRepository extends JpaRepository<ConnectionConfig, Long> {
    Optional<ConnectionConfig> findByOrg(String org);
}
