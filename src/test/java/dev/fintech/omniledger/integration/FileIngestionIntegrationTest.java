package dev.fintech.omniledger.integration;

import dev.fintech.omniledger.dto.request.CreateBusinessPartyRequest;
import dev.fintech.omniledger.dto.request.ExternalAccountMappingRequest;
import dev.fintech.omniledger.dto.response.FileIngestionJobResponse;
import dev.fintech.omniledger.dto.response.FileIngestionRejectionResponse;
import dev.fintech.omniledger.dto.response.PartyResponse;
import dev.fintech.omniledger.exception.DuplicateFileException;
import dev.fintech.omniledger.model.Account;
import dev.fintech.omniledger.model.enums.AccountType;
import dev.fintech.omniledger.model.enums.Currency;
import dev.fintech.omniledger.model.enums.IngestionJobStatus;
import dev.fintech.omniledger.model.enums.RejectionCode;
import dev.fintech.omniledger.repository.AccountRepository;
import dev.fintech.omniledger.repository.BusinessProfileRepository;
import dev.fintech.omniledger.repository.ExternalAccountMappingRepository;
import dev.fintech.omniledger.repository.FileIngestionJobRepository;
import dev.fintech.omniledger.repository.FileIngestionRejectionRepository;
import dev.fintech.omniledger.repository.GovernmentProfileRepository;
import dev.fintech.omniledger.repository.IndividualProfileRepository;
import dev.fintech.omniledger.repository.JournalEntryRepository;
import dev.fintech.omniledger.repository.PartyRepository;
import dev.fintech.omniledger.repository.PostingLineRepository;
import dev.fintech.omniledger.repository.SystemEventRepository;
import dev.fintech.omniledger.service.ExternalAccountMappingService;
import dev.fintech.omniledger.service.FileIngestionService;
import dev.fintech.omniledger.service.PartyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("File Ingestion Subsystem Integration Tests")
class FileIngestionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FileIngestionService fileIngestionService;

    @Autowired
    private ExternalAccountMappingService mappingService;

    @Autowired
    private PartyService partyService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private PostingLineRepository postingLineRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    @Autowired
    private IndividualProfileRepository individualProfileRepository;

    @Autowired
    private GovernmentProfileRepository governmentProfileRepository;

    @Autowired
    private SystemEventRepository systemEventRepository;

    @Autowired
    private FileIngestionJobRepository jobRepository;

    @Autowired
    private FileIngestionRejectionRepository rejectionRepository;

    @Autowired
    private ExternalAccountMappingRepository mappingRepository;

    private Account sourceAccount;
    private Account destinationAccount;

    @BeforeEach
    void setUp() {
        cleanUpDatabase();

        CreateBusinessPartyRequest partyRequest = new CreateBusinessPartyRequest(
                "Acme Global Settlements Ltd",
                "Acme Settlements",
                "IN",
                "27AABCU9603R1ZM",
                "U72900MH2020PTC123456",
                "AABCU9603R",
                null,
                "billing@acme.com",
                "+919876543210"
        );
        PartyResponse partyResponse = partyService.onboardBusiness(partyRequest);

        sourceAccount = accountRepository.save(Account.builder()
                .partyId(partyResponse.partyId())
                .accountType(AccountType.ASSET)
                .currency(Currency.INR)
                .balance(new BigDecimal("10000.0000"))
                .isSharded(false)
                .build());

        destinationAccount = accountRepository.save(Account.builder()
                .partyId(partyResponse.partyId())
                .accountType(AccountType.LIABILITY)
                .currency(Currency.INR)
                .balance(new BigDecimal("5000.0000"))
                .isSharded(false)
                .build());

        mappingService.createMapping(ExternalAccountMappingRequest.builder()
                .externalSystem("STRIPE")
                .externalAccountId("EXT_SOURCE_100")
                .accountId(sourceAccount.getId())
                .build());

        mappingService.createMapping(ExternalAccountMappingRequest.builder()
                .externalSystem("STRIPE")
                .externalAccountId("EXT_DEST_200")
                .accountId(destinationAccount.getId())
                .build());
    }

    @AfterEach
    void tearDown() {
        cleanUpDatabase();
    }

    private void cleanUpDatabase() {
        rejectionRepository.deleteAll();
        jobRepository.deleteAll();
        mappingRepository.deleteAll();
        postingLineRepository.deleteAll();
        journalEntryRepository.deleteAll();
        systemEventRepository.deleteAll();
        accountRepository.deleteAll();
        individualProfileRepository.deleteAll();
        businessProfileRepository.deleteAll();
        governmentProfileRepository.deleteAll();
        partyRepository.deleteAll();
    }

    private FileIngestionJobResponse waitForJobCompletion(UUID jobId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            FileIngestionJobResponse response = fileIngestionService.getJobStatus(jobId);
            if (response.getStatus() != IngestionJobStatus.ACCEPTED
                    && response.getStatus() != IngestionJobStatus.PROCESSING
                    && response.getStatus() != IngestionJobStatus.PENDING) {
                return response;
            }
            Thread.sleep(50);
        }
        return fileIngestionService.getJobStatus(jobId);
    }

    @Test
    @DisplayName("Should accept file asynchronously with HTTP 202, process batch, update balances, and create journal entries")
    void testSuccessfulPairedFileIngestion() throws InterruptedException {
        String csvContent = """
                reference_id,external_account_id,amount,direction,currency,description
                TXN-BATCH-001,EXT_SOURCE_100,1500.0000,DEBIT,INR,Settlement Payout Leg 1
                TXN-BATCH-001,EXT_DEST_200,1500.0000,CREDIT,INR,Settlement Payout Leg 2
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "stripe_settlement_20261005.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        FileIngestionJobResponse initialResponse = fileIngestionService.ingestPairedTransactionsFile(file, "STRIPE");

        assertThat(initialResponse).isNotNull();
        assertThat(initialResponse.getStatus()).isEqualTo(IngestionJobStatus.ACCEPTED);
        assertThat(initialResponse.getFileChecksumSha256()).isNotBlank();

        FileIngestionJobResponse completedJob = waitForJobCompletion(initialResponse.getJobId());

        assertThat(completedJob.getStatus()).isEqualTo(IngestionJobStatus.COMPLETED);
        assertThat(completedJob.getTotalRecords()).isEqualTo(2);
        assertThat(completedJob.getProcessedRecords()).isEqualTo(2);
        assertThat(completedJob.getFailedRecords()).isEqualTo(0);

        Account updatedSource = accountRepository.findById(sourceAccount.getId()).orElseThrow();
        Account updatedDest = accountRepository.findById(destinationAccount.getId()).orElseThrow();

        assertThat(updatedSource.getBalance()).isEqualByComparingTo(new BigDecimal("8500.0000"));
        assertThat(updatedDest.getBalance()).isEqualByComparingTo(new BigDecimal("6500.0000"));

        boolean journalExists = journalEntryRepository.existsByIdempotencyKey("STRIPE:TXN-BATCH-001");
        assertThat(journalExists).isTrue();
    }

    @Test
    @DisplayName("Should reject exact duplicate file upload synchronously via cryptographic SHA-256 checksum (Layer 1 Deduplication)")
    void testDuplicateFileUploadProtection() throws InterruptedException {
        String csvContent = """
                reference_id,external_account_id,amount,direction,currency,description
                DUP-TXN-001,EXT_SOURCE_100,500.0000,DEBIT,INR,Idempotent test
                DUP-TXN-001,EXT_DEST_200,500.0000,CREDIT,INR,Idempotent test
                """;

        MockMultipartFile file1 = new MockMultipartFile("file", "batch_file.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));
        MockMultipartFile file2 = new MockMultipartFile("file", "batch_file_copy.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        FileIngestionJobResponse initial = fileIngestionService.ingestPairedTransactionsFile(file1, "STRIPE");
        waitForJobCompletion(initial.getJobId());

        assertThatThrownBy(() -> fileIngestionService.ingestPairedTransactionsFile(file2, "STRIPE"))
                .isInstanceOf(DuplicateFileException.class)
                .hasMessageContaining("File has already been processed");
    }

    @Test
    @DisplayName("Should commit valid transaction pair and quarantine invalid pair with RejectionCode.UNKNOWN_ACCOUNT asynchronously")
    void testPartialFailureAndQuarantine() throws InterruptedException {
        String csvContent = """
                reference_id,external_account_id,amount,direction,currency,description
                GOOD-001,EXT_SOURCE_100,1000.0000,DEBIT,INR,Valid transaction leg
                GOOD-001,EXT_DEST_200,1000.0000,CREDIT,INR,Valid transaction leg
                BAD-002,EXT_SOURCE_100,500.0000,DEBIT,INR,Unmapped account leg
                BAD-002,UNKNOWN_UNMAPPED_999,500.0000,CREDIT,INR,Unmapped account leg
                """;

        MockMultipartFile file = new MockMultipartFile("file", "mixed_batch.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        FileIngestionJobResponse initial = fileIngestionService.ingestPairedTransactionsFile(file, "STRIPE");
        FileIngestionJobResponse job = waitForJobCompletion(initial.getJobId());

        assertThat(job.getStatus()).isEqualTo(IngestionJobStatus.PARTIALLY_FAILED);
        assertThat(job.getTotalRecords()).isEqualTo(4);
        assertThat(job.getProcessedRecords()).isEqualTo(2);
        assertThat(job.getFailedRecords()).isEqualTo(2);

        // Verify valid pair was posted to the ledger
        Account updatedSource = accountRepository.findById(sourceAccount.getId()).orElseThrow();
        assertThat(updatedSource.getBalance()).isEqualByComparingTo(new BigDecimal("9000.0000")); // 10000 - 1000 = 9000

        // Verify quarantined lines
        List<FileIngestionRejectionResponse> rejections = fileIngestionService.getJobRejections(job.getJobId());
        assertThat(rejections).hasSize(2);
        assertThat(rejections).allMatch(r -> r.getRejectionCode() == RejectionCode.UNKNOWN_ACCOUNT);
    }

    @Test
    @DisplayName("Should quarantine unbalanced transaction legs with RejectionCode.UNBALANCED_ENTRY asynchronously")
    void testUnbalancedEntryQuarantine() throws InterruptedException {
        String csvContent = """
                reference_id,external_account_id,amount,direction,currency,description
                UNBALANCED-001,EXT_SOURCE_100,1000.0000,DEBIT,INR,Debit 1000
                UNBALANCED-001,EXT_DEST_200,750.0000,CREDIT,INR,Credit only 750
                """;

        MockMultipartFile file = new MockMultipartFile("file", "unbalanced.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        FileIngestionJobResponse initial = fileIngestionService.ingestPairedTransactionsFile(file, "STRIPE");
        FileIngestionJobResponse job = waitForJobCompletion(initial.getJobId());

        assertThat(job.getStatus()).isEqualTo(IngestionJobStatus.FAILED);
        assertThat(job.getProcessedRecords()).isEqualTo(0);
        assertThat(job.getFailedRecords()).isEqualTo(2);

        List<FileIngestionRejectionResponse> rejections = fileIngestionService.getJobRejections(job.getJobId());
        assertThat(rejections).hasSize(2);
        assertThat(rejections).allMatch(r -> r.getRejectionCode() == RejectionCode.UNBALANCED_ENTRY);

        Account untouchedSource = accountRepository.findById(sourceAccount.getId()).orElseThrow();
        assertThat(untouchedSource.getBalance()).isEqualByComparingTo(new BigDecimal("10000.0000"));
    }
}
