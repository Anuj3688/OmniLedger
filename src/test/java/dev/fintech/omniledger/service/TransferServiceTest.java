package dev.fintech.omniledger.service;

import dev.fintech.omniledger.dto.request.TransferRequest;
import dev.fintech.omniledger.dto.response.TransferResponse;
import dev.fintech.omniledger.exception.AccountNotFoundException;
import dev.fintech.omniledger.exception.CurrencyMismatchException;
import dev.fintech.omniledger.exception.DuplicateIdempotencyKeyException;
import dev.fintech.omniledger.exception.InsufficientBalanceException;
import dev.fintech.omniledger.exception.LedgerContentionException;
import dev.fintech.omniledger.exception.SameAccountTransferException;
import dev.fintech.omniledger.model.Account;
import dev.fintech.omniledger.model.JournalEntry;
import dev.fintech.omniledger.model.PostingLine;
import dev.fintech.omniledger.model.enums.AccountType;
import dev.fintech.omniledger.model.enums.Currency;
import dev.fintech.omniledger.model.enums.PostingType;
import dev.fintech.omniledger.repository.AccountRepository;
import dev.fintech.omniledger.repository.JournalEntryRepository;
import dev.fintech.omniledger.service.impl.TransferServiceImpl;
import dev.fintech.omniledger.service.routing.AccountShardRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests validating Double-Entry transfer execution, idempotency, deterministic locking,
 * and contention recovery.
 */
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private EventService eventService;

    @Mock
    private AccountShardRouter accountShardRouter;

    @InjectMocks
    private TransferServiceImpl transferService;

    private UUID sourceId;
    private UUID destId;
    private Account sourceAccount;
    private Account destAccount;

    @BeforeEach
    void setUp() {
        sourceId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        destId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        sourceAccount = Account.builder()
                .id(sourceId)
                .partyId(UUID.randomUUID())
                .accountType(AccountType.ASSET)
                .currency(Currency.INR)
                .balance(new BigDecimal("1000.0000"))
                .build();

        destAccount = Account.builder()
                .id(destId)
                .partyId(UUID.randomUUID())
                .accountType(AccountType.LIABILITY)
                .currency(Currency.INR)
                .balance(new BigDecimal("200.0000"))
                .build();

        lenient().when(accountShardRouter.resolveDestinationAccount(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Should successfully execute transfer with balanced postings and audit event")
    void executeTransfer_Success() {
        TransferRequest request = new TransferRequest(
                "tx-test-001",
                sourceId,
                destId,
                new BigDecimal("300.0000"),
                Currency.INR,
                "Vendor payment"
        );

        when(journalEntryRepository.findByIdempotencyKeyWithLines("tx-test-001")).thenReturn(Optional.empty());
        when(accountRepository.findByIdWithLock(sourceId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdWithLock(destId)).thenReturn(Optional.of(destAccount));

        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry je = invocation.getArgument(0);
            je.setId(UUID.randomUUID());
            je.setCreatedAt(Instant.now());
            return je;
        });

        TransferResponse response = transferService.executeTransfer(request);

        assertNotNull(response);
        assertEquals("tx-test-001", response.idempotencyKey());
        assertEquals(Currency.INR, response.currency());
        assertEquals(2, response.lines().size());

        // Balance assertions: source debited 300 (1000 -> 700), dest credited 300 (200 -> 500)
        assertEquals(new BigDecimal("700.0000"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("500.0000"), destAccount.getBalance());

        verify(journalEntryRepository).save(any(JournalEntry.class));
        verify(eventService).recordTransferCompleted(eq(response.journalEntryId()), eq("tx-test-001"), any());
    }

    @Test
    @DisplayName("Should return existing receipt for idempotent retry without double debiting")
    void executeTransfer_IdempotentRetry_ReturnsExistingReceipt() {
        TransferRequest request = new TransferRequest(
                "tx-test-dup-001",
                sourceId,
                destId,
                new BigDecimal("150.0000"),
                Currency.INR,
                "Duplicate retry"
        );

        JournalEntry existing = JournalEntry.builder()
                .id(UUID.randomUUID())
                .idempotencyKey("tx-test-dup-001")
                .currency(Currency.INR)
                .description("Duplicate retry")
                .createdAt(Instant.now())
                .build();

        PostingLine debit = PostingLine.builder().account(sourceAccount).amount(new BigDecimal("150.0000")).direction(PostingType.DEBIT).build();
        PostingLine credit = PostingLine.builder().account(destAccount).amount(new BigDecimal("150.0000")).direction(PostingType.CREDIT).build();
        existing.addPostingLine(debit);
        existing.addPostingLine(credit);

        when(journalEntryRepository.findByIdempotencyKeyWithLines("tx-test-dup-001")).thenReturn(Optional.of(existing));

        TransferResponse response = transferService.executeTransfer(request);

        assertNotNull(response);
        assertEquals(existing.getId(), response.journalEntryId());
        verify(accountRepository, never()).findByIdWithLock(any());
        verify(journalEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateIdempotencyKeyException when duplicate key has differing amount")
    void executeTransfer_IdempotencyConflict_ThrowsException() {
        TransferRequest request = new TransferRequest(
                "tx-test-conflict-001",
                sourceId,
                destId,
                new BigDecimal("500.0000"),
                Currency.INR,
                "Conflict retry"
        );

        JournalEntry existing = JournalEntry.builder()
                .id(UUID.randomUUID())
                .idempotencyKey("tx-test-conflict-001")
                .currency(Currency.INR)
                .createdAt(Instant.now())
                .build();

        PostingLine debit = PostingLine.builder().account(sourceAccount).amount(new BigDecimal("150.0000")).direction(PostingType.DEBIT).build();
        PostingLine credit = PostingLine.builder().account(destAccount).amount(new BigDecimal("150.0000")).direction(PostingType.CREDIT).build();
        existing.addPostingLine(debit);
        existing.addPostingLine(credit);

        when(journalEntryRepository.findByIdempotencyKeyWithLines("tx-test-conflict-001")).thenReturn(Optional.of(existing));

        assertThrows(DuplicateIdempotencyKeyException.class, () ->
                transferService.executeTransfer(request));
    }

    @Test
    @DisplayName("Should reject transfer when source has insufficient balance")
    void executeTransfer_InsufficientBalance_ThrowsException() {
        TransferRequest request = new TransferRequest(
                "tx-test-insufficient",
                sourceId,
                destId,
                new BigDecimal("5000.0000"),
                Currency.INR,
                "Overdraft attempt"
        );

        when(journalEntryRepository.findByIdempotencyKeyWithLines("tx-test-insufficient")).thenReturn(Optional.empty());
        when(accountRepository.findByIdWithLock(sourceId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdWithLock(destId)).thenReturn(Optional.of(destAccount));

        InsufficientBalanceException ex = assertThrows(InsufficientBalanceException.class, () ->
                transferService.executeTransfer(request));

        assertEquals(sourceId, ex.getAccountId());
        assertEquals(new BigDecimal("1000.0000"), ex.getAvailableBalance());
        verify(eventService).recordTransferFailed(eq("tx-test-insufficient"), any(), any());
    }

    @Test
    @DisplayName("Should reject transfer when accounts are in different currencies")
    void executeTransfer_CurrencyMismatch_ThrowsException() {
        destAccount.setCurrency(Currency.USD);

        TransferRequest request = new TransferRequest(
                "tx-test-fx-mismatch",
                sourceId,
                destId,
                new BigDecimal("100.0000"),
                Currency.INR,
                "Cross currency direct transfer"
        );

        when(journalEntryRepository.findByIdempotencyKeyWithLines("tx-test-fx-mismatch")).thenReturn(Optional.empty());
        when(accountRepository.findByIdWithLock(sourceId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdWithLock(destId)).thenReturn(Optional.of(destAccount));

        assertThrows(CurrencyMismatchException.class, () ->
                transferService.executeTransfer(request));

        verify(eventService).recordTransferFailed(eq("tx-test-fx-mismatch"), any(), any());
    }

    @Test
    @DisplayName("Should reject transfer when source and destination are the same account")
    void executeTransfer_SameAccount_ThrowsException() {
        TransferRequest request = new TransferRequest(
                "tx-test-same-account",
                sourceId,
                sourceId,
                new BigDecimal("100.0000"),
                Currency.INR,
                "Self transfer"
        );

        assertThrows(SameAccountTransferException.class, () ->
                transferService.executeTransfer(request));
    }

    @Test
    @DisplayName("Should deterministically acquire locks in UUID order to prevent deadlocks")
    void executeTransfer_DeterministicLockOrdering() {
        UUID idFirst = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID idSecond = UUID.fromString("22222222-2222-2222-2222-222222222222");
        assertTrue(idFirst.compareTo(idSecond) < 0);

        Account accountFirst = Account.builder().id(idFirst).currency(Currency.INR).balance(new BigDecimal("500")).build();
        Account accountSecond = Account.builder().id(idSecond).currency(Currency.INR).balance(new BigDecimal("500")).build();

        TransferRequest request = new TransferRequest(
                "tx-lock-order-test",
                idSecond,
                idFirst,
                new BigDecimal("100.0000"),
                Currency.INR,
                "Lock order test"
        );

        when(journalEntryRepository.findByIdempotencyKeyWithLines("tx-lock-order-test")).thenReturn(Optional.empty());
        when(accountRepository.findByIdWithLock(idFirst)).thenReturn(Optional.of(accountFirst));
        when(accountRepository.findByIdWithLock(idSecond)).thenReturn(Optional.of(accountSecond));
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(i -> {
            JournalEntry j = i.getArgument(0);
            j.setId(UUID.randomUUID());
            return j;
        });

        transferService.executeTransfer(request);

        InOrder inOrder = Mockito.inOrder(accountRepository);
        inOrder.verify(accountRepository).findByIdWithLock(idFirst);
        inOrder.verify(accountRepository).findByIdWithLock(idSecond);
    }

    @Test
    @DisplayName("Should invoke recover fallback and throw LedgerContentionException when retries are exhausted")
    void recover_ThrowsLedgerContentionException() {
        TransferRequest request = new TransferRequest(
                "tx-contention-exhausted",
                sourceId,
                destId,
                new BigDecimal("100.0000"),
                Currency.INR,
                "Contention test"
        );

        CannotAcquireLockException lockEx = new CannotAcquireLockException("Lock wait timeout exceeded");

        LedgerContentionException ex = assertThrows(LedgerContentionException.class, () ->
                transferService.recover(lockEx, request));

        assertEquals("tx-contention-exhausted", ex.getIdempotencyKey());
        assertTrue(ex.getMessage().contains("database contention/throttling"));
        verify(eventService).recordTransferFailed(eq("tx-contention-exhausted"), contains("Database contention"), eq("TRANSFER_CONTENTION_EXHAUSTED"));
    }
}
