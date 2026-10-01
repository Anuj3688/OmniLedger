package dev.fintech.omniledger.service.strategy;

import dev.fintech.omniledger.model.User;
import dev.fintech.omniledger.model.enums.UserModificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles validation and updating of a user's full name.
 */
@Slf4j
@Component
public class FullNameUpdateStrategy implements UserUpdateStrategy {

    @Override
    public UserModificationType supports() {
        return UserModificationType.FULL_NAME;
    }

    @Override
    public void validate(String newValue) {
        log.debug("Validating full name input: length={}", newValue != null ? newValue.length() : 0);
        if (newValue == null || newValue.isBlank()) {
            log.warn("Full name validation failed: value is null or blank");
            throw new IllegalArgumentException("Full name cannot be null or empty");
        }
        String trimmed = newValue.trim();
        if (trimmed.length() < 2 || trimmed.length() > 100) {
            log.warn("Full name validation failed: length={} out of bounds [2, 100]", trimmed.length());
            throw new IllegalArgumentException("Full name must be between 2 and 100 characters");
        }
        if (!trimmed.matches("^[a-zA-Z\\s.'-]+$")) {
            log.warn("Full name validation failed: illegal characters found in '{}'", trimmed);
            throw new IllegalArgumentException("Full name contains invalid characters: " + trimmed);
        }
    }

    @Override
    public void apply(User user, String newValue) {
        log.info("Updating full name for userId={} from '{}' to '{}'", user.getId(), user.getFullName(), newValue.trim());
        user.setFullName(newValue.trim());
    }
}
