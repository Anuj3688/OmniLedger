package dev.fintech.omniledger.integration;

import dev.fintech.omniledger.dto.request.CreateIndividualPartyRequest;
import dev.fintech.omniledger.dto.request.TransferRequest;
import dev.fintech.omniledger.dto.response.PartyResponse;
import dev.fintech.omniledger.dto.response.TransferResponse;
import dev.fintech.omniledger.model.Account;
import dev.fintech.omniledger.model.enums.AccountType;
import dev.fintech.omniledger.model.enums.Currency;
import dev.fintech.omniledger.model.enums.ResidentialStatus;
import dev.fintech.omniledger.repository.AccountRepository;
import dev.fintech.omniledger.repository.BusinessProfileRepository;
import dev.fintech.omniledger.repository.GovernmentProfileRepository;
import dev.fintech.omniledger.repository.IndividualProfileRepository;
import dev.fintech.omniledger.repository.JournalEntryRepository;
import dev.fintech.omniledger.repository.PartyRepository;
import dev.fintech.omniledger.repository.PostingLineRepository;
import dev.fintech.omniledger.repository.SystemEventRepository;
import dev.fintech.omniledger.service.PartyService;
import dev.fintech.omniledger.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Real PostgreSQL Concurrency Integration Test.
 * Simulates bidirectional simultaneous transfers between accounts to prove:
 * 1. Deadlock-free execution via deterministic lock ordering.
 * 2. Conservation of money (Total system balance invariance).
 * 3. Accurate ledger journal entries and posting lines.
 */
class TransferConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private PartyService partyService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private IndividualProfileRepository individualProfileRepository;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    @Autowired
    private GovernmentProfileRepository governmentProfileRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private PostingLineRepository postingLineRepository;

    @Autowired
    private SystemEventRepository systemEventRepository;

    private UUID accountAliceId;
    private UUID accountBobId;

    @BeforeEach
    void setUp() {
        postingLineRepository.deleteAll();
        journalEntryRepository.deleteAll();
        systemEventRepository.deleteAll();
        accountRepository.deleteAll();
        individualProfileRepository.deleteAll();
        businessProfileRepository.deleteAll();
        governmentProfileRepository.deleteAll();
        partyRepository.deleteAll();

        // 1. Onboard Alice
        PartyResponse aliceParty = partyService.onboardIndividual(
                new CreateIndividualPartyRequest(
                        "Alice Retail",
                        "alice@example.com",
                        "+919876543210",
                        "ALICE1234F",
                        ResidentialStatus.RESIDENT_INDIAN,
                        "IN",
                        LocalDate.of(1992, 4, 10)
                )
        );

        // 2. Onboard Bob
        PartyResponse bobParty = partyService.onboardIndividual(
                new CreateIndividualPartyRequest(
                        "Bob Retail",
                        "bob@example.com",
                        "+919876543211",
                        "BOBXX1234F",
                        ResidentialStatus.RESIDENT_INDIAN,
                        "IN",
                        LocalDate.of(1990, 8, 20)
                )
        );

        // 3. Open accounts with initial balance of ₹1,000.0000 each
        Account aliceAccount = Account.builder()
                .partyId(aliceParty.partyId())
                .accountType(AccountType.ASSET)
                .currency(Currency.INR)
                .balance(new BigDecimal("1000.0000"))
                .build();

        Account bobAccount = Account.builder()
                .partyId(bobParty.partyId())
                .accountType(AccountType.ASSET)
                .currency(Currency.INR)
                .balance(new BigDecimal("1000.0000"))
                .build();

        accountAliceId = accountRepository.save(aliceAccount).getId();
        accountBobId = accountRepository.save(bobAccount).getId();
    }

    @Test
    @DisplayName("Execute simultaneous bidirectional transfers across 10 threads without deadlocks or money loss")
    void executeConcurrentBidirectionalTransfers_SucceedsWithoutDeadlock() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Callable<TransferResponse>> tasks = new ArrayList<>();

        // 5 transfers Alice -> Bob (₹50 each)
        for (int i = 0; i < 5; i++) {
            final String idempotencyKey = "tx-alice-to-bob-" + i;
            tasks.add(() -> {
                readyLatch.countDown();
                startLatch.await(); // wait for the gun to fire all threads simultaneously
                return transferService.executeTransfer(new TransferRequest(
                        idempotencyKey,
                        accountAliceId,
                        accountBobId,
                        new BigDecimal("50.0000"),
                        Currency.INR,
                        "Alice to Bob transfer"
                ));
            });
        }

        // 5 transfers Bob -> Alice (₹50 each)
        for (int i = 0; i < 5; i++) {
            final String idempotencyKey = "tx-bob-to-alice-" + i;
            tasks.add(() -> {
                readyLatch.countDown();
                startLatch.await(); // wait for the gun to fire all threads simultaneously
                return transferService.executeTransfer(new TransferRequest(
                        idempotencyKey,
                        accountBobId,
                        accountAliceId,
                        new BigDecimal("50.0000"),
                        Currency.INR,
                        "Bob to Alice transfer"
                ));
            });
        }

        List<Future<TransferResponse>> futures = new ArrayList<>();
        for (Callable<TransferResponse> task : tasks) {
            futures.add(executor.submit(task));
        }

        readyLatch.await(); // Wait for all 10 threads to be ready
        startLatch.countDown(); // Fire all 10 threads at the EXACT same millisecond!

        // Wait for all 10 transfers to finish
        int successCount = 0;
        for (Future<TransferResponse> future : futures) {
            TransferResponse response = future.get();
            assertNotNull(response);
            successCount++;
        }

        assertEquals(10, successCount, "All 10 concurrent transfers should succeed without deadlocks");

        // Reload accounts from PostgreSQL
        Account finalAlice = accountRepository.findById(accountAliceId).orElseThrow();
        Account finalBob = accountRepository.findById(accountBobId).orElseThrow();

        // 5 * 50 = 250 sent by Alice, 5 * 50 = 250 received by Alice -> net change = 0
        assertEquals(0, new BigDecimal("1000.0000").compareTo(finalAlice.getBalance()));
        assertEquals(0, new BigDecimal("1000.0000").compareTo(finalBob.getBalance()));

        // Double-entry invariant: Sum of balances must remain exactly ₹2,000.0000
        BigDecimal totalSystemBalance = finalAlice.getBalance().add(finalBob.getBalance());
        assertEquals(0, new BigDecimal("2000.0000").compareTo(totalSystemBalance));

        // Ledger audit invariant: Exactly 10 JournalEntries and 20 PostingLines must exist in PostgreSQL
        assertEquals(10, journalEntryRepository.count());
        assertEquals(20, postingLineRepository.count());

        executor.shutdown();
    }
}
