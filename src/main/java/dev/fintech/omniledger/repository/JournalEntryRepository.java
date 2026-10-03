package dev.fintech.omniledger.repository;

import dev.fintech.omniledger.model.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    Optional<JournalEntry> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    @Query("SELECT DISTINCT j FROM JournalEntry j LEFT JOIN FETCH j.lines l LEFT JOIN FETCH l.account WHERE j.id = :id")
    Optional<JournalEntry> findByIdWithLines(@Param("id") UUID id);

    @Query("SELECT DISTINCT j FROM JournalEntry j LEFT JOIN FETCH j.lines l LEFT JOIN FETCH l.account WHERE j.idempotencyKey = :idempotencyKey")
    Optional<JournalEntry> findByIdempotencyKeyWithLines(@Param("idempotencyKey") String idempotencyKey);
}
