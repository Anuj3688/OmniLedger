package dev.fintech.omniledger.service.policy;

import dev.fintech.omniledger.model.enums.AccountType;
import dev.fintech.omniledger.model.enums.Currency;
import dev.fintech.omniledger.model.enums.PartyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Enforces account ownership constraints for commercial merchant and enterprise entities.
 * Businesses may hold ASSET (receivables), LIABILITY (payables), and EXPENSE accounts,
 * but cannot operate general ledger platform REVENUE or EQUITY pools.
 */
@Slf4j
@Component
public class BusinessAccountPolicyStrategy implements AccountPolicyStrategy {

    private static final Set<AccountType> PERMITTED_TYPES = Set.of(
            AccountType.ASSET,
            AccountType.LIABILITY,
            AccountType.EXPENSE
    );

    @Override
    public PartyType supports() {
        return PartyType.BUSINESS;
    }

    @Override
    public void validateAccountCreation(UUID partyId, AccountType accountType, Currency currency) {
        log.debug("Validating BUSINESS account creation: partyId={}, accountType={}, currency={}", partyId, accountType, currency);

        if (!PERMITTED_TYPES.contains(accountType)) {
            log.warn("BUSINESS policy violation: partyId={} attempted to open forbidden accountType={}", partyId, accountType);
            throw new IllegalStateException("Commercial business parties are only permitted to hold ASSET, LIABILITY, or EXPENSE accounts. Requested: " + accountType);
        }

        log.debug("BUSINESS account creation policy passed for partyId={}", partyId);
    }
}
