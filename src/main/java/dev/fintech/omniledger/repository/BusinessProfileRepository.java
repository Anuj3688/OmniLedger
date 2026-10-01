package dev.fintech.omniledger.repository;

import dev.fintech.omniledger.model.BusinessProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for Business (merchant/corporate) profiles.
 */
@Repository
public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, UUID> {

    Optional<BusinessProfile> findByGstin(String gstin);

    boolean existsByGstin(String gstin);

    boolean existsByCinNumber(String cinNumber);

    boolean existsByCorporatePan(String corporatePan);
}
