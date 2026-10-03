package dev.fintech.omniledger.repository;

import dev.fintech.omniledger.model.Account;
import dev.fintech.omniledger.model.enums.AccountType;
import dev.fintech.omniledger.model.enums.Currency;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") UUID id);

    List<Account> findByPartyId(UUID partyId);

    List<Account> findByPartyIdAndAccountType(UUID partyId, AccountType accountType);

    boolean existsByPartyIdAndAccountTypeAndCurrency(UUID partyId, AccountType accountType, Currency currency);
}
