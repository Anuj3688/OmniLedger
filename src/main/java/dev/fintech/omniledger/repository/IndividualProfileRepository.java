package dev.fintech.omniledger.repository;

import dev.fintech.omniledger.model.IndividualProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IndividualProfileRepository extends JpaRepository<IndividualProfile, UUID> {

    Optional<IndividualProfile> findByPanHash(String panHash);

    boolean existsByPanHash(String panHash);

    Optional<IndividualProfile> findByEmailHash(String emailHash);

    boolean existsByEmailHash(String emailHash);
}
