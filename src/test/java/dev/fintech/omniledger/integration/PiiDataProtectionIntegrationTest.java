package dev.fintech.omniledger.integration;

import dev.fintech.omniledger.dto.request.CreateIndividualPartyRequest;
import dev.fintech.omniledger.dto.response.PartyResponse;
import dev.fintech.omniledger.exception.DuplicatePanException;
import dev.fintech.omniledger.model.IndividualProfile;
import dev.fintech.omniledger.model.enums.ResidentialStatus;
import dev.fintech.omniledger.repository.AccountRepository;
import dev.fintech.omniledger.repository.BusinessProfileRepository;
import dev.fintech.omniledger.repository.GovernmentProfileRepository;
import dev.fintech.omniledger.repository.IndividualProfileRepository;
import dev.fintech.omniledger.repository.JournalEntryRepository;
import dev.fintech.omniledger.repository.PartyRepository;
import dev.fintech.omniledger.repository.PostingLineRepository;
import dev.fintech.omniledger.repository.SystemEventRepository;
import dev.fintech.omniledger.security.PiiCryptoService;
import dev.fintech.omniledger.service.PartyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PiiDataProtectionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PartyService partyService;

    @Autowired
    private IndividualProfileRepository individualProfileRepository;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    @Autowired
    private GovernmentProfileRepository governmentProfileRepository;

    @Autowired
    private PostingLineRepository postingLineRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private SystemEventRepository systemEventRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PiiCryptoService piiCryptoService;

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
    }

    @Test
    @DisplayName("PII fields stored encrypted in PostgreSQL, indexed via HMAC blind index, and decrypted on read")
    void piiDataIsEncryptedAtRest_AndQueryableViaBlindIndex() {
        String rawPan = "ABCDE9999Z";
        String rawEmail = "security.audit@bank.com";
        String rawPhone = "+919876543210";

        CreateIndividualPartyRequest request = new CreateIndividualPartyRequest(
                "Audit Customer",
                rawEmail,
                rawPhone,
                rawPan,
                ResidentialStatus.RESIDENT_INDIAN,
                "IN",
                LocalDate.of(1989, 4, 12)
        );

        PartyResponse response = partyService.onboardIndividual(request);
        assertNotNull(response.partyId());

        IndividualProfile rawDbEntity = individualProfileRepository.findById(response.partyId()).orElseThrow();

        // 1. Verify at-rest encryption (never stored in plaintext)
        assertNotEquals(rawPan, rawDbEntity.getPanNumber(), "PAN must be encrypted at rest in PostgreSQL");
        assertNotEquals(rawEmail, rawDbEntity.getEmail(), "Email must be encrypted at rest in PostgreSQL");
        assertNotEquals(rawPhone, rawDbEntity.getPhoneNumber(), "Phone must be encrypted at rest in PostgreSQL");
        assertFalse(rawDbEntity.getPanNumber().contains(rawPan));

        // 2. Verify Blind Index (HMAC-SHA256)
        String expectedPanHash = piiCryptoService.computeBlindIndex(rawPan);
        assertEquals(expectedPanHash, rawDbEntity.getPanHash());
        assertEquals(64, rawDbEntity.getPanHash().length());

        // 3. Verify Deterministic Querying via Blind Index
        Optional<IndividualProfile> foundByHash = individualProfileRepository.findByPanHash(expectedPanHash);
        assertTrue(foundByHash.isPresent(), "Entity must be retrievable via HMAC blind index");
        assertEquals(response.partyId(), foundByHash.get().getPartyId());

        // 4. Verify Duplicate PAN Protection
        assertThrows(DuplicatePanException.class, () -> partyService.onboardIndividual(request));

        // 5. Verify Transparent Decryption on API retrieval
        PartyResponse fetched = partyService.getPartyById(response.partyId());
        assertEquals(rawPan, fetched.statutoryIdentifier(), "API read must decrypt PAN seamlessly");
    }
}
