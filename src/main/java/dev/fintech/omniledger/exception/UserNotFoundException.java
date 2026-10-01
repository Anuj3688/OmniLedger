package dev.fintech.omniledger.exception;

import java.util.UUID;

/**
 * Thrown when a requested User profile cannot be located by ID.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID userId) {
        super("User not found with id: " + userId);
    }
}
