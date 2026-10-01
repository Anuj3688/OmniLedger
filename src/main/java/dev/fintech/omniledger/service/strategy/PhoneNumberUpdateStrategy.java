package dev.fintech.omniledger.service.strategy;

import dev.fintech.omniledger.model.User;
import dev.fintech.omniledger.model.enums.UserModificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Handles validation and updating of a user's contact phone number.
 */
@Slf4j
@Component
public class PhoneNumberUpdateStrategy implements UserUpdateStrategy {

    // Validates international/Indian phone formats: e.g. +919876543210, 9876543210
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(?:\\+?\\d{1,3}[- ]?)?\\d{10}$");

    @Override
    public UserModificationType supports() {
        return UserModificationType.PHONE_NUMBER;
    }

    @Override
    public void validate(String newValue) {
        log.debug("Validating phone number input");
        if (newValue == null || newValue.isBlank()) {
            log.warn("Phone number validation failed: value is null or blank");
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }
        String cleaned = newValue.trim().replaceAll("[\\s-]", "");
        if (!PHONE_PATTERN.matcher(cleaned).matches()) {
            log.warn("Phone number validation failed: malformed format '{}'", newValue);
            throw new IllegalArgumentException("Invalid phone number format: " + newValue + ". Expected 10-digit number optionally prefixed with country code");
        }
    }

    @Override
    public void apply(User user, String newValue) {
        String cleaned = newValue.trim();
        log.info("Updating phone number for userId={} from '{}' to '{}'", user.getId(), user.getPhoneNumber(), cleaned);
        user.setPhoneNumber(cleaned);
    }
}
