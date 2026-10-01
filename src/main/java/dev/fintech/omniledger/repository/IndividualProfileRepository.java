package dev.fintech.omniledger.repository;

import dev.fintech.omniledger.model.IndividualProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for Individual (retail) profiles.
 */
@Repository
public interface IndividualProfileRepository extends JpaRepository<IndividualProfile, UUID> {

    Optional<IndividualProfile> findByPanNumber(String panNumber);

    boolean existsByPanNumber(String panNumber);

    boolean existsByEmail(String email);
}
