package dev.fintech.omniledger.service.policy;

import dev.fintech.omniledger.model.enums.AccountType;
import dev.fintech.omniledger.model.enums.Currency;
import dev.fintech.omniledger.model.enums.PartyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Enforces account ownership constraints for OmniLedger internal platform operations.
 * Platform accounts hold REVENUE (fee pools), EXPENSE (gateway/processing costs),
 * and EQUITY (capital reserves).
 */
@Slf4j
@Component
public class PlatformAccountPolicyStrategy implements AccountPolicyStrategy {

    private static final Set<AccountType> PERMITTED_TYPES = Set.of(
            AccountType.REVENUE,
            AccountType.EXPENSE,
            AccountType.EQUITY
    );

    @Override
    public PartyType supports() {
        return PartyType.PLATFORM;
    }

    @Override
    public void validateAccountCreation(UUID partyId, AccountType accountType, Currency currency) {
        log.debug("Validating PLATFORM account creation: partyId={}, accountType={}, currency={}", partyId, accountType, currency);

        if (!PERMITTED_TYPES.contains(accountType)) {
            log.warn("PLATFORM policy violation: partyId={} attempted to open forbidden accountType={}", partyId, accountType);
            throw new IllegalStateException("Platform internal parties are only permitted to hold REVENUE, EXPENSE, or EQUITY accounts. Requested: " + accountType);
        }

        log.debug("PLATFORM account creation policy passed for partyId={}", partyId);
    }
}
