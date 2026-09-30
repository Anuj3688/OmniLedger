package dev.fintech.omniledger.repository;

import dev.fintech.omniledger.model.SystemEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for persisting and querying immutable audit events.
 */
@Repository
public interface SystemEventRepository extends JpaRepository<SystemEvent, UUID> {

    List<SystemEvent> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant startTime, Instant endTime);

    List<SystemEvent> findAllByOrderByCreatedAtDesc();
}
