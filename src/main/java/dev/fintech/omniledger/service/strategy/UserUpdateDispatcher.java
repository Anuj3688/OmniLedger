package dev.fintech.omniledger.service.strategy;

import dev.fintech.omniledger.dto.UserModificationItem;
import dev.fintech.omniledger.model.User;
import dev.fintech.omniledger.model.enums.UserModificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Dispatcher orchestrating profile updates using the Strategy pattern.
 * Eliminates if-else chains and enforces the Open/Closed Principle.
 */
@Slf4j
@Component
public class UserUpdateDispatcher {

    private final Map<UserModificationType, UserUpdateStrategy> strategyMap;

    public UserUpdateDispatcher(List<UserUpdateStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(UserUpdateStrategy::supports, Function.identity()));
        log.info("Initialized UserUpdateDispatcher with {} strategies: {}", strategyMap.size(), strategyMap.keySet());
    }

    /**
     * Finds the matching strategy and executes validation + application.
     */
    public void dispatch(User user, UserModificationItem item) {
        if (item == null || item.type() == null) {
            log.error("Invalid user modification request: item or modification type is null");
            throw new IllegalArgumentException("Modification item and type cannot be null");
        }

        UserUpdateStrategy strategy = strategyMap.get(item.type());
        if (strategy == null) {
            log.error("No registered strategy found for modification type: {}", item.type());
            throw new UnsupportedOperationException("No update strategy found for modification type: " + item.type());
        }

        log.debug("Dispatching update for userId={}, type={}", user.getId(), item.type());
        strategy.execute(user, item.value());
        log.debug("Successfully executed update strategy for userId={}, type={}", user.getId(), item.type());
    }
}
