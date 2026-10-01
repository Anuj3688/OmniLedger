package dev.fintech.omniledger.service.strategy;

import dev.fintech.omniledger.model.User;
import dev.fintech.omniledger.model.enums.UserModificationType;
import dev.fintech.omniledger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Handles validation and updating of a user's email address with format and uniqueness checks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailUpdateStrategy implements UserUpdateStrategy {

    private final UserRepository userRepository;

    // RFC 5322 compliant email regex
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    @Override
    public UserModificationType supports() {
        return UserModificationType.EMAIL;
    }

    @Override
    public void validate(String newValue) {
        log.debug("Validating email address input");
        if (newValue == null || newValue.isBlank()) {
            log.warn("Email validation failed: value is null or blank");
            throw new IllegalArgumentException("Email address cannot be null or empty");
        }
        String normalizedEmail = newValue.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            log.warn("Email validation failed: malformed email address '{}'", newValue);
            throw new IllegalArgumentException("Invalid email address format: " + newValue);
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Email validation failed: duplicate email collision for '{}'", normalizedEmail);
            throw new IllegalArgumentException("Email address already in use: " + normalizedEmail);
        }
    }

    @Override
    public void apply(User user, String newValue) {
        String normalizedEmail = newValue.trim().toLowerCase();
        log.info("Updating email for userId={} from '{}' to '{}'", user.getId(), user.getEmail(), normalizedEmail);
        user.setEmail(normalizedEmail);
    }
}
