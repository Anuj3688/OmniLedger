package dev.fintech.omniledger.service.strategy;

import dev.fintech.omniledger.model.User;
import dev.fintech.omniledger.model.enums.UserModificationType;

/**
 * Strategy contract for validating and applying a specific user profile modification.
 * Adheres to the Single Responsibility Principle and Open/Closed Principle.
 */
public interface UserUpdateStrategy {

    /**
     * Identifies which modification type this strategy handles.
     */
    UserModificationType supports();

    /**
     * Validates the proposed new value against domain rules.
     * Throws an appropriate exception if validation fails.
     */
    void validate(String newValue);

    /**
     * Applies the validated value to the User entity.
     */
    void apply(User user, String newValue);

    /**
     * Template method that orchestrates validation followed by application.
     */
    default void execute(User user, String newValue) {
        validate(newValue);
        apply(user, newValue);
    }
}
