package dev.fintech.omniledger.repository;

import dev.fintech.omniledger.model.ExternalAccountMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalAccountMappingRepository extends JpaRepository<ExternalAccountMapping, UUID> {

    Optional<ExternalAccountMapping> findByExternalSystemAndExternalAccountId(String externalSystem, String externalAccountId);

    @Query("SELECT m.account.id FROM ExternalAccountMapping m WHERE m.externalSystem = :externalSystem AND m.externalAccountId = :externalAccountId")
    Optional<UUID> findAccountId(String externalSystem, String externalAccountId);

    boolean existsByExternalSystemAndExternalAccountId(String externalSystem, String externalAccountId);
}
