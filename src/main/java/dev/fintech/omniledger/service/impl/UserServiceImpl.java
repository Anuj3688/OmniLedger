package dev.fintech.omniledger.service.impl;

import dev.fintech.omniledger.dto.CreateUserRequest;
import dev.fintech.omniledger.dto.UpdateUserRequest;
import dev.fintech.omniledger.dto.UserModificationItem;
import dev.fintech.omniledger.dto.UserResponse;
import dev.fintech.omniledger.exception.DuplicatePanException;
import dev.fintech.omniledger.exception.UserNotFoundException;
import dev.fintech.omniledger.model.User;
import dev.fintech.omniledger.model.enums.EventType;
import dev.fintech.omniledger.repository.UserRepository;
import dev.fintech.omniledger.service.EventService;
import dev.fintech.omniledger.service.UserService;
import dev.fintech.omniledger.service.strategy.UserUpdateDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Production implementation of UserService handling onboarding, profile modifications, and customer queries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final EventService eventService;
    private final UserUpdateDispatcher userUpdateDispatcher;

    // Standard Indian PAN format: 5 letters, 4 digits, 1 letter (e.g. ABCDE1234F)
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Received request to onboard new user with PAN={}", request != null ? request.panNumber() : null);

        // Isolated validation step
        String normalizedPan = validateAndNormalizeUserCreation(request);

        User user = User.builder()
                .fullName(request.fullName() != null ? request.fullName().trim() : null)
                .email(request.email() != null ? request.email().trim().toLowerCase() : null)
                .phoneNumber(request.phoneNumber() != null ? request.phoneNumber().trim() : null)
                .panNumber(normalizedPan)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Successfully onboarded user id={} with PAN={}", savedUser.getId(), savedUser.getPanNumber());

        eventService.recordGenericEvent(
                EventType.ACCOUNT_CREATED,
                savedUser.getId(),
                null,
                null,
                null,
                "User onboarded with PAN: " + savedUser.getPanNumber(),
                "SUCCESS",
                null
        );

        return mapToResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        log.debug("Fetching user details for id={}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User lookup failed: user id={} not found", id);
                    return new UserNotFoundException(id);
                });
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        log.info("Received request to update user id={} with {} modifications",
                id, request != null && request.modifications() != null ? request.modifications().size() : 0);

        validateUserUpdateRequest(request);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User update failed: user id={} not found", id);
                    return new UserNotFoundException(id);
                });

        // SOLID: Execute each modification through the Strategy Dispatcher with zero if-else
        for (UserModificationItem item : request.modifications()) {
            userUpdateDispatcher.dispatch(user, item);
        }

        User updatedUser = userRepository.save(user);
        log.info("Successfully updated profile for user id={}", updatedUser.getId());

        eventService.recordGenericEvent(
                EventType.ACCOUNT_CREATED,
                updatedUser.getId(),
                null,
                null,
                null,
                "User profile modified: " + request.modifications().size() + " fields updated",
                "SUCCESS",
                null
        );

        return mapToResponse(updatedUser);
    }

    /**
     * Dedicated validation method isolating all business rules for customer onboarding.
     */
    private String validateAndNormalizeUserCreation(CreateUserRequest request) {
        log.debug("Validating user onboarding request parameters");

        if (request == null) {
            log.error("User onboarding validation failed: request payload is null");
            throw new IllegalArgumentException("CreateUserRequest cannot be null");
        }

        if (request.panNumber() == null || request.panNumber().isBlank()) {
            log.error("User onboarding validation failed: PAN number is missing");
            throw new IllegalArgumentException("PAN number is mandatory");
        }

        String normalizedPan = request.panNumber().trim().toUpperCase();

        if (!PAN_PATTERN.matcher(normalizedPan).matches()) {
            log.warn("User onboarding validation failed: invalid PAN format '{}'", normalizedPan);
            throw new IllegalArgumentException("Invalid PAN number format: " + normalizedPan + ". Expected format: ABCDE1234F");
        }

        if (userRepository.existsByPanNumber(normalizedPan)) {
            log.warn("User onboarding validation failed: duplicate PAN '{}' collision", normalizedPan);
            throw new DuplicatePanException(normalizedPan);
        }

        log.debug("User onboarding request passed all validation checks");
        return normalizedPan;
    }

    /**
     * Dedicated validation method for profile update requests.
     */
    private void validateUserUpdateRequest(UpdateUserRequest request) {
        if (request == null || request.modifications() == null || request.modifications().isEmpty()) {
            log.error("User update validation failed: no modifications provided");
            throw new IllegalArgumentException("At least one modification item must be provided");
        }
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getPanNumber(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
