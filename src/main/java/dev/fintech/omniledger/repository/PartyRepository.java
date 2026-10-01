package dev.fintech.omniledger.repository;

import dev.fintech.omniledger.model.Party;
import dev.fintech.omniledger.model.enums.PartyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for base Party entities.
 */
@Repository
public interface PartyRepository extends JpaRepository<Party, UUID> {

    List<Party> findByPartyType(PartyType partyType);
}
