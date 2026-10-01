package dev.fintech.omniledger.service.policy;

import dev.fintech.omniledger.model.enums.AccountType;
import dev.fintech.omniledger.model.enums.Currency;
import dev.fintech.omniledger.model.enums.PartyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Enforces account ownership constraints for sovereign tax agencies and municipal bodies.
 * Government agencies strictly hold LIABILITY accounts (representing tax revenues collected
 * on behalf of the state awaiting disbursement).
 */
@Slf4j
@Component
public class GovernmentAccountPolicyStrategy implements AccountPolicyStrategy {

    @Override
    public PartyType supports() {
        return PartyType.GOVERNMENT;
    }

    @Override
    public void validateAccountCreation(UUID partyId, AccountType accountType, Currency currency) {
        log.debug("Validating GOVERNMENT account creation: partyId={}, accountType={}, currency={}", partyId, accountType, currency);

        if (accountType != AccountType.LIABILITY) {
            log.warn("GOVERNMENT policy violation: partyId={} attempted to open non-LIABILITY accountType={}", partyId, accountType);
            throw new IllegalStateException("Government tax agencies are only permitted to hold LIABILITY accounts (tax collection buckets). Requested: " + accountType);
        }

        log.debug("GOVERNMENT account creation policy passed for partyId={}", partyId);
    }
}
