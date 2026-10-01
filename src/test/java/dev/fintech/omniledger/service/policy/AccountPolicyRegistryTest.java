package dev.fintech.omniledger.service.policy;

import dev.fintech.omniledger.model.enums.AccountType;
import dev.fintech.omniledger.model.enums.Currency;
import dev.fintech.omniledger.model.enums.PartyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests validating the polymorphic Account Policy Strategy Engine.
 */
class AccountPolicyRegistryTest {

    private AccountPolicyRegistry registry;

    @BeforeEach
    void setUp() {
        List<AccountPolicyStrategy> strategies = List.of(
                new IndividualAccountPolicyStrategy(),
                new BusinessAccountPolicyStrategy(),
                new GovernmentAccountPolicyStrategy(),
                new PlatformAccountPolicyStrategy()
        );
        registry = new AccountPolicyRegistry(strategies);
    }

    @Test
    @DisplayName("INDIVIDUAL policy permits ASSET and LIABILITY accounts")
    void individualPolicy_PermitsAssetAndLiability() {
        UUID partyId = UUID.randomUUID();

        assertDoesNotThrow(() ->
                registry.validate(PartyType.INDIVIDUAL, partyId, AccountType.ASSET, Currency.INR));

        assertDoesNotThrow(() ->
                registry.validate(PartyType.INDIVIDUAL, partyId, AccountType.LIABILITY, Currency.INR));
    }

    @Test
    @DisplayName("INDIVIDUAL policy strictly prohibits corporate EQUITY, REVENUE, and EXPENSE accounts")
    void individualPolicy_ProhibitsCorporateTypes() {
        UUID partyId = UUID.randomUUID();

        IllegalStateException exEquity = assertThrows(IllegalStateException.class, () ->
                registry.validate(PartyType.INDIVIDUAL, partyId, AccountType.EQUITY, Currency.INR));
        assertTrue(exEquity.getMessage().contains("Retail individual parties are only permitted"));

        IllegalStateException exRevenue = assertThrows(IllegalStateException.class, () ->
                registry.validate(PartyType.INDIVIDUAL, partyId, AccountType.REVENUE, Currency.INR));
        assertTrue(exRevenue.getMessage().contains("Retail individual parties are only permitted"));

        IllegalStateException exExpense = assertThrows(IllegalStateException.class, () ->
                registry.validate(PartyType.INDIVIDUAL, partyId, AccountType.EXPENSE, Currency.INR));
        assertTrue(exExpense.getMessage().contains("Retail individual parties are only permitted"));
    }

    @Test
    @DisplayName("BUSINESS policy permits ASSET, LIABILITY, and EXPENSE accounts")
    void businessPolicy_PermitsCommercialTypes() {
        UUID partyId = UUID.randomUUID();

        assertDoesNotThrow(() ->
                registry.validate(PartyType.BUSINESS, partyId, AccountType.ASSET, Currency.INR));

        assertDoesNotThrow(() ->
                registry.validate(PartyType.BUSINESS, partyId, AccountType.LIABILITY, Currency.INR));

        assertDoesNotThrow(() ->
                registry.validate(PartyType.BUSINESS, partyId, AccountType.EXPENSE, Currency.INR));
    }

    @Test
    @DisplayName("BUSINESS policy prohibits platform REVENUE and EQUITY accounts")
    void businessPolicy_ProhibitsPlatformTypes() {
        UUID partyId = UUID.randomUUID();

        IllegalStateException exRevenue = assertThrows(IllegalStateException.class, () ->
                registry.validate(PartyType.BUSINESS, partyId, AccountType.REVENUE, Currency.INR));
        assertTrue(exRevenue.getMessage().contains("Commercial business parties are only permitted"));

        IllegalStateException exEquity = assertThrows(IllegalStateException.class, () ->
                registry.validate(PartyType.BUSINESS, partyId, AccountType.EQUITY, Currency.INR));
        assertTrue(exEquity.getMessage().contains("Commercial business parties are only permitted"));
    }

    @Test
    @DisplayName("GOVERNMENT policy strictly permits only LIABILITY tax collection buckets")
    void governmentPolicy_PermitsOnlyLiability() {
        UUID partyId = UUID.randomUUID();

        assertDoesNotThrow(() ->
                registry.validate(PartyType.GOVERNMENT, partyId, AccountType.LIABILITY, Currency.INR));

        assertThrows(IllegalStateException.class, () ->
                registry.validate(PartyType.GOVERNMENT, partyId, AccountType.ASSET, Currency.INR));

        assertThrows(IllegalStateException.class, () ->
                registry.validate(PartyType.GOVERNMENT, partyId, AccountType.REVENUE, Currency.INR));

        assertThrows(IllegalStateException.class, () ->
                registry.validate(PartyType.GOVERNMENT, partyId, AccountType.EXPENSE, Currency.INR));

        assertThrows(IllegalStateException.class, () ->
                registry.validate(PartyType.GOVERNMENT, partyId, AccountType.EQUITY, Currency.INR));
    }

    @Test
    @DisplayName("PLATFORM policy permits REVENUE, EXPENSE, and EQUITY accounts")
    void platformPolicy_PermitsInternalPlatformTypes() {
        UUID partyId = UUID.randomUUID();

        assertDoesNotThrow(() ->
                registry.validate(PartyType.PLATFORM, partyId, AccountType.REVENUE, Currency.INR));

        assertDoesNotThrow(() ->
                registry.validate(PartyType.PLATFORM, partyId, AccountType.EXPENSE, Currency.INR));

        assertDoesNotThrow(() ->
                registry.validate(PartyType.PLATFORM, partyId, AccountType.EQUITY, Currency.INR));

        assertThrows(IllegalStateException.class, () ->
                registry.validate(PartyType.PLATFORM, partyId, AccountType.ASSET, Currency.INR));
    }
}
