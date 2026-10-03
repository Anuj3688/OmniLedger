package dev.fintech.omniledger.integration;

import dev.fintech.omniledger.dto.request.CreateAccountRequest;
import dev.fintech.omniledger.dto.request.CreateGovernmentPartyRequest;
import dev.fintech.omniledger.dto.request.CreateIndividualPartyRequest;
import dev.fintech.omniledger.dto.request.TransferRequest;
import dev.fintech.omniledger.dto.response.AccountResponse;
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
import dev.fintech.omniledger.service.AccountService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShardedAccountConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountService accountService;

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

    private UUID govtTaxMasterAccountId;
    private final List<UUID> taxpayerAccountIds = new ArrayList<>();

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
        taxpayerAccountIds.clear();

        PartyResponse govtParty = partyService.onboardGovernment(
                new CreateGovernmentPartyRequest(
                        "GST Collection Directorate",
                        "IN-CENTRAL",
                        "GST-REV-01",
                        "INDIRECT-TAX"
                )
        );

        AccountResponse govtAccount = accountService.createAccount(
                new CreateAccountRequest(
                        govtParty.partyId(),
                        AccountType.LIABILITY,
                        Currency.INR,
                        BigDecimal.ZERO,
                        true,
                        5
                )
        );
        govtTaxMasterAccountId = govtAccount.accountId();

        for (int i = 0; i < 10; i++) {
            String pan = String.format("ABCDE%04dF", 1000 + i);
            PartyResponse taxpayerParty = partyService.onboardIndividual(
                    new CreateIndividualPartyRequest(
                            "Taxpayer " + i,
                            "taxpayer" + i + "@example.com",
                            "+9198000000" + (10 + i),
                            pan,
                            ResidentialStatus.RESIDENT_INDIAN,
                            "IN",
                            LocalDate.of(1985, 1, 1).plusYears(i)
                    )
            );

            Account taxpayerAcc = accountRepository.save(Account.builder()
                    .partyId(taxpayerParty.partyId())
                    .accountType(AccountType.ASSET)
                    .currency(Currency.INR)
                    .balance(new BigDecimal("1000.0000"))
                    .build());

            taxpayerAccountIds.add(taxpayerAcc.getId());
        }
    }

    @Test
    @DisplayName("10 concurrent taxpayers paying ₹100 tax to single Government sharded account succeeds without hot-row contention")
    void executeConcurrentTaxPaymentsToHotGovtAccount_SucceedsViaShards() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Callable<TransferResponse>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final UUID payerAccountId = taxpayerAccountIds.get(i);
            final String idempotencyKey = "tax-payment-tx-" + i;

            tasks.add(() -> {
                readyLatch.countDown();
                startLatch.await();
                return transferService.executeTransfer(new TransferRequest(
                        idempotencyKey,
                        payerAccountId,
                        govtTaxMasterAccountId,
                        new BigDecimal("100.0000"),
                        Currency.INR,
                        "Monthly GST settlement"
                ));
            });
        }

        List<Future<TransferResponse>> futures = new ArrayList<>();
        for (Callable<TransferResponse> task : tasks) {
            futures.add(executor.submit(task));
        }

        readyLatch.await();
        startLatch.countDown();

        int successfulTransfers = 0;
        for (Future<TransferResponse> future : futures) {
            TransferResponse response = future.get();
            assertNotNull(response);
            successfulTransfers++;
        }

        assertEquals(10, successfulTransfers, "All 10 concurrent tax payments must succeed");

        AccountResponse govtMaster = accountService.getAccountById(govtTaxMasterAccountId);
        System.out.println("=== DEBUG Master Balance: " + govtMaster.balance());
        List<Account> shards = accountRepository.findByParentAccountId(govtTaxMasterAccountId);
        for (Account s : shards) {
            System.out.println("=== DEBUG Shard ID=" + s.getId() + " Balance=" + s.getBalance());
        }

        assertTrue(govtMaster.isSharded(), "Govt account should be flagged as sharded");
        assertEquals(0, new BigDecimal("1000.0000").compareTo(govtMaster.balance()),
                "Aggregate government tax balance must equal exactly ₹1,000.0000. Actual: " + govtMaster.balance());

        assertEquals(5, shards.size(), "Should have exactly 5 child shards");

        BigDecimal sumOfShards = shards.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("1000.0000").compareTo(sumOfShards),
                "Sum of all physical shard balances must equal ₹1,000.0000. Actual: " + sumOfShards);

        assertEquals(10, journalEntryRepository.count(), "10 journal entries should exist");
        assertEquals(20, postingLineRepository.count(), "20 posting lines should exist");

        executor.shutdown();
    }
}
