package dev.fintech.omniledger.service.policy;

import dev.fintech.omniledger.model.enums.AccountType;
import dev.fintech.omniledger.model.enums.Currency;
import dev.fintech.omniledger.model.enums.PartyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Enforces account ownership constraints for natural persons (retail customers).
 * Retail customers may hold multiple ASSET and LIABILITY accounts (wallets/loans),
 * but are strictly prohibited from holding corporate EQUITY, REVENUE, or EXPENSE accounts.
 */
@Slf4j
@Component
public class IndividualAccountPolicyStrategy implements AccountPolicyStrategy {

    private static final Set<AccountType> PERMITTED_TYPES = Set.of(AccountType.ASSET, AccountType.LIABILITY);

    @Override
    public PartyType supports() {
        return PartyType.INDIVIDUAL;
    }

    @Override
    public void validateAccountCreation(UUID partyId, AccountType accountType, Currency currency) {
        log.debug("Validating INDIVIDUAL account creation: partyId={}, accountType={}, currency={}", partyId, accountType, currency);

        if (!PERMITTED_TYPES.contains(accountType)) {
            log.warn("INDIVIDUAL policy violation: partyId={} attempted to open forbidden accountType={}", partyId, accountType);
            throw new IllegalStateException("Retail individual parties are only permitted to hold ASSET or LIABILITY accounts. Requested: " + accountType);
        }

        log.debug("INDIVIDUAL account creation policy passed for partyId={}", partyId);
    }
}
