package dev.fintech.omniledger.service.policy;

import dev.fintech.omniledger.model.enums.AccountType;
import dev.fintech.omniledger.model.enums.Currency;
import dev.fintech.omniledger.model.enums.PartyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry and dispatcher orchestrating account ownership policy validation.
 * Adheres to the Open/Closed Principle with zero if-else branching.
 */
@Slf4j
@Component
public class AccountPolicyRegistry {

    private final Map<PartyType, AccountPolicyStrategy> policyMap;

    public AccountPolicyRegistry(List<AccountPolicyStrategy> strategies) {
        this.policyMap = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(AccountPolicyStrategy::supports, Function.identity()));
        log.info("Initialized AccountPolicyRegistry with {} policies: {}", policyMap.size(), policyMap.keySet());
    }

    /**
     * Dispatches policy validation to the strategy corresponding to the PartyType.
     */
    public void validate(PartyType partyType, UUID partyId, AccountType accountType, Currency currency) {
        if (partyType == null) {
            log.error("Account policy validation failed: partyType is null");
            throw new IllegalArgumentException("PartyType cannot be null");
        }

        AccountPolicyStrategy strategy = policyMap.get(partyType);
        if (strategy == null) {
            log.error("No account policy strategy registered for PartyType: {}", partyType);
            throw new UnsupportedOperationException("No account policy registered for party type: " + partyType);
        }

        log.debug("Enforcing account policy: partyType={}, partyId={}, accountType={}", partyType, partyId, accountType);
        strategy.validateAccountCreation(partyId, accountType, currency);
    }
}
