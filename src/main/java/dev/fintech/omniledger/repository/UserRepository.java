package dev.fintech.omniledger.repository;

import dev.fintech.omniledger.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for User identity profiles.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByPanNumber(String panNumber);

    boolean existsByPanNumber(String panNumber);

    boolean existsByEmail(String email);
}
