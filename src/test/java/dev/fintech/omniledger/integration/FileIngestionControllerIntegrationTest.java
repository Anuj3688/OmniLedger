package dev.fintech.omniledger.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.fintech.omniledger.dto.request.CreateBusinessPartyRequest;
import dev.fintech.omniledger.dto.request.ExternalAccountMappingRequest;
import dev.fintech.omniledger.dto.response.PartyResponse;
import dev.fintech.omniledger.model.Account;
import dev.fintech.omniledger.model.enums.AccountType;
import dev.fintech.omniledger.model.enums.Currency;
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
import dev.fintech.omniledger.service.PartyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("File Ingestion and Account Mapping REST API Integration Tests")
class FileIngestionControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PartyService partyService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ExternalAccountMappingRepository mappingRepository;

    @Autowired
    private FileIngestionJobRepository jobRepository;

    @Autowired
    private FileIngestionRejectionRepository rejectionRepository;

    @Autowired
    private PostingLineRepository postingLineRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private SystemEventRepository systemEventRepository;

    @Autowired
    private IndividualProfileRepository individualProfileRepository;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    @Autowired
    private GovernmentProfileRepository governmentProfileRepository;

    @Autowired
    private PartyRepository partyRepository;

    private Account testAccount1;
    private Account testAccount2;

    @BeforeEach
    void setUp() {
        cleanUpDatabase();

        CreateBusinessPartyRequest partyRequest = new CreateBusinessPartyRequest(
                "Apex Ingestion Corp",
                "Apex Ingest",
                "IN",
                "29AABCA1234F1Z8",
                "U72900KA2021PTC123456",
                "AABCA1234F",
                null,
                "finance@apex.com",
                "+918012345678"
        );
        PartyResponse party = partyService.onboardBusiness(partyRequest);

        testAccount1 = accountRepository.save(Account.builder()
                .partyId(party.partyId())
                .accountType(AccountType.ASSET)
                .currency(Currency.INR)
                .balance(new BigDecimal("50000.0000"))
                .isSharded(false)
                .build());

        testAccount2 = accountRepository.save(Account.builder()
                .partyId(party.partyId())
                .accountType(AccountType.LIABILITY)
                .currency(Currency.INR)
                .balance(new BigDecimal("10000.0000"))
                .isSharded(false)
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

    @Test
    @DisplayName("End-to-End: Register mappings, upload CSV with HTTP 202 Accepted, poll job status, check rejections via REST APIs")
    void testEndToEndFileIngestionRestApi() throws Exception {
        // 1. Register External Account Mappings via POST /api/v1/ledger/account-mappings
        ExternalAccountMappingRequest mapReq1 = ExternalAccountMappingRequest.builder()
                .externalSystem("RAZORPAY")
                .externalAccountId("VA_MERCHANT_01")
                .accountId(testAccount1.getId())
                .build();

        mockMvc.perform(post("/api/v1/ledger/account-mappings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mapReq1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalSystem", is("RAZORPAY")))
                .andExpect(jsonPath("$.externalAccountId", is("VA_MERCHANT_01")))
                .andExpect(jsonPath("$.accountId", is(testAccount1.getId().toString())));

        ExternalAccountMappingRequest mapReq2 = ExternalAccountMappingRequest.builder()
                .externalSystem("RAZORPAY")
                .externalAccountId("VA_NODAL_POOL")
                .accountId(testAccount2.getId())
                .build();

        mockMvc.perform(post("/api/v1/ledger/account-mappings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mapReq2)))
                .andExpect(status().isCreated());

        // 2. Fetch mapping via GET /api/v1/ledger/account-mappings/{externalSystem}/{externalAccountId}
        mockMvc.perform(get("/api/v1/ledger/account-mappings/RAZORPAY/VA_MERCHANT_01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalSystem", is("RAZORPAY")))
                .andExpect(jsonPath("$.accountId", is(testAccount1.getId().toString())));

        // 3. Upload CSV via POST /api/v1/ledger/files/ingest -> Expect HTTP 202 Accepted
        String csvContent = """
                reference_id,external_account_id,amount,direction,currency,description
                PAY-001,VA_MERCHANT_01,2500.00,DEBIT,INR,Payout 001
                PAY-001,VA_NODAL_POOL,2500.00,CREDIT,INR,Payout 001
                PAY-002,VA_MERCHANT_01,800.00,DEBIT,INR,Payout 002
                PAY-002,NON_EXISTENT_ACC,800.00,CREDIT,INR,Payout 002
                """;

        MockMultipartFile csvFile = new MockMultipartFile(
                "file",
                "razorpay_settlements_2026.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        String responseBody = mockMvc.perform(multipart("/api/v1/ledger/files/ingest")
                        .file(csvFile)
                        .param("sourceSystem", "RAZORPAY"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("ACCEPTED")))
                .andReturn().getResponse().getContentAsString();

        String jobId = objectMapper.readTree(responseBody).get("jobId").asText();

        // 4. Poll job status via GET /api/v1/ledger/files/jobs/{jobId} until terminal state
        long deadline = System.currentTimeMillis() + 5000;
        String jobStatus = "ACCEPTED";
        while (System.currentTimeMillis() < deadline) {
            String checkResponse = mockMvc.perform(get("/api/v1/ledger/files/jobs/" + jobId))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            jobStatus = objectMapper.readTree(checkResponse).get("status").asText();
            if (!jobStatus.equals("ACCEPTED") && !jobStatus.equals("PROCESSING") && !jobStatus.equals("PENDING")) {
                break;
            }
            Thread.sleep(50);
        }

        mockMvc.perform(get("/api/v1/ledger/files/jobs/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId", is(jobId)))
                .andExpect(jsonPath("$.status", is("PARTIALLY_FAILED")))
                .andExpect(jsonPath("$.totalRecords", is(4)))
                .andExpect(jsonPath("$.processedRecords", is(2)))
                .andExpect(jsonPath("$.failedRecords", is(2)));

        // 5. Query quarantined rejections via GET /api/v1/ledger/files/jobs/{jobId}/rejections
        mockMvc.perform(get("/api/v1/ledger/files/jobs/" + jobId + "/rejections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].rejectionCode", is("UNKNOWN_ACCOUNT")))
                .andExpect(jsonPath("$[1].rejectionCode", is("UNKNOWN_ACCOUNT")));
    }
}
