package dev.fintech.omniledger.repository;

import dev.fintech.omniledger.model.GovernmentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for Government and sovereign agency profiles.
 */
@Repository
public interface GovernmentProfileRepository extends JpaRepository<GovernmentProfile, UUID> {

    Optional<GovernmentProfile> findByNodalAgencyCode(String nodalAgencyCode);

    boolean existsByNodalAgencyCode(String nodalAgencyCode);
}
