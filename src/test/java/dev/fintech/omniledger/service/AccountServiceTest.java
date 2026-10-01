package dev.fintech.omniledger.service;

import dev.fintech.omniledger.dto.AccountResponse;
import dev.fintech.omniledger.dto.CreateAccountRequest;
import dev.fintech.omniledger.exception.PartyNotFoundException;
import dev.fintech.omniledger.model.Account;
import dev.fintech.omniledger.model.Party;
import dev.fintech.omniledger.model.enums.AccountType;
import dev.fintech.omniledger.model.enums.Currency;
import dev.fintech.omniledger.model.enums.PartyType;
import dev.fintech.omniledger.repository.AccountRepository;
import dev.fintech.omniledger.repository.PartyRepository;
import dev.fintech.omniledger.service.impl.AccountServiceImpl;
import dev.fintech.omniledger.service.policy.AccountPolicyRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests validating Account opening, balance constraints, and policy integration.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private AccountPolicyRegistry accountPolicyRegistry;

    @Mock
    private EventService eventService;

    @InjectMocks
    private AccountServiceImpl accountService;

    private UUID mockPartyId;
    private UUID mockAccountId;
    private Party mockParty;

    @BeforeEach
    void setUp() {
        mockPartyId = UUID.randomUUID();
        mockAccountId = UUID.randomUUID();
        mockParty = Party.builder()
                .id(mockPartyId)
                .partyType(PartyType.INDIVIDUAL)
                .build();
    }

    @Test
    @DisplayName("Should successfully create account when party exists and policy passes")
    void createAccount_Success() {
        CreateAccountRequest request = new CreateAccountRequest(
                mockPartyId,
                AccountType.ASSET,
                Currency.INR,
                new BigDecimal("1000.0000")
        );

        when(partyRepository.findById(mockPartyId)).thenReturn(Optional.of(mockParty));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account acc = invocation.getArgument(0);
            acc.setId(mockAccountId);
            return acc;
        });

        AccountResponse response = accountService.createAccount(request);

        assertNotNull(response);
        assertEquals(mockAccountId, response.accountId());
        assertEquals(mockPartyId, response.partyId());
        assertEquals(AccountType.ASSET, response.accountType());
        assertEquals(new BigDecimal("1000.0000"), response.balance());
        assertEquals(Currency.INR, response.currency());

        verify(accountPolicyRegistry).validate(PartyType.INDIVIDUAL, mockPartyId, AccountType.ASSET, Currency.INR);
        verify(eventService).recordAccountCreated(eq(mockAccountId), eq(mockPartyId), any());
    }

    @Test
    @DisplayName("Should reject account creation when initial balance is negative")
    void createAccount_NegativeBalance_ThrowsException() {
        CreateAccountRequest request = new CreateAccountRequest(
                mockPartyId,
                AccountType.ASSET,
                Currency.INR,
                new BigDecimal("-50.0000")
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                accountService.createAccount(request));

        assertTrue(ex.getMessage().contains("Initial balance cannot be negative"));
    }

    @Test
    @DisplayName("Should throw PartyNotFoundException when partyId does not exist")
    void createAccount_PartyNotFound_ThrowsException() {
        CreateAccountRequest request = new CreateAccountRequest(
                mockPartyId,
                AccountType.ASSET,
                Currency.INR,
                BigDecimal.ZERO
        );

        when(partyRepository.findById(mockPartyId)).thenReturn(Optional.empty());

        assertThrows(PartyNotFoundException.class, () ->
                accountService.createAccount(request));
    }

    @Test
    @DisplayName("Should block account creation if AccountPolicyRegistry rejects account type")
    void createAccount_PolicyRejection_ThrowsException() {
        CreateAccountRequest request = new CreateAccountRequest(
                mockPartyId,
                AccountType.EQUITY,
                Currency.INR,
                BigDecimal.ZERO
        );

        when(partyRepository.findById(mockPartyId)).thenReturn(Optional.of(mockParty));
        doThrow(new IllegalStateException("Retail individual parties are only permitted to hold ASSET or LIABILITY accounts"))
                .when(accountPolicyRegistry).validate(PartyType.INDIVIDUAL, mockPartyId, AccountType.EQUITY, Currency.INR);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                accountService.createAccount(request));

        assertTrue(ex.getMessage().contains("Retail individual parties are only permitted"));
    }
}
