package com.aforo.apigee.repository;

import com.aforo.apigee.model.DeveloperLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeveloperLinkRepository extends JpaRepository<DeveloperLink, Long> {
    Optional<DeveloperLink> findByApigeeDeveloperId(String apigeeDeveloperId);
}
