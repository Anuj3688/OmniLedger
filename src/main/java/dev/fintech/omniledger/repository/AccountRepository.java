package dev.fintech.omniledger.repository;

import dev.fintech.omniledger.model.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for financial accounts.
 * Provides pessimistic row-level locking queries for high-concurrency transfer execution.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * Executes SELECT ... FOR UPDATE to acquire an exclusive row-level lock on an account.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") UUID id);
}
