package dev.fintech.omniledger.service;

import dev.fintech.omniledger.dto.CreateUserRequest;
import dev.fintech.omniledger.dto.UpdateUserRequest;
import dev.fintech.omniledger.dto.UserResponse;

import java.util.UUID;

/**
 * Service contract for customer identity and onboarding operations.
 */
public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(UUID id);

    UserResponse updateUser(UUID id, UpdateUserRequest request);
}
