package dev.fintech.omniledger.service.policy;

import dev.fintech.omniledger.model.enums.AccountType;
import dev.fintech.omniledger.model.enums.Currency;
import dev.fintech.omniledger.model.enums.PartyType;

import java.util.UUID;

/**
 * Strategy interface enforcing domain and regulatory account ownership rules per PartyType.
 */
public interface AccountPolicyStrategy {

    PartyType supports();

    /**
     * Validates whether the given Party is legally/operationally permitted to open an account of this type.
     * Throws an IllegalArgumentException or IllegalStateException on rule violation.
     */
    void validateAccountCreation(UUID partyId, AccountType accountType, Currency currency);
}
