package dev.fintech.omniledger.dto.response;

import dev.fintech.omniledger.model.enums.AccountType;
import dev.fintech.omniledger.model.enums.Currency;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID accountId,
        UUID partyId,
        AccountType accountType,
        Currency currency,
        BigDecimal balance,
        boolean isSharded,
        UUID parentAccountId
) {
    public AccountResponse(UUID accountId, UUID partyId, AccountType accountType, Currency currency, BigDecimal balance) {
        this(accountId, partyId, accountType, currency, balance, false, null);
    }
}
