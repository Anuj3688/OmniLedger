package dev.fintech.omniledger.service;

import dev.fintech.omniledger.dto.request.CreateBusinessPartyRequest;
import dev.fintech.omniledger.dto.request.CreateGovernmentPartyRequest;
import dev.fintech.omniledger.dto.request.CreateIndividualPartyRequest;
import dev.fintech.omniledger.dto.response.PartyResponse;
import dev.fintech.omniledger.exception.DuplicatePanException;
import dev.fintech.omniledger.model.BusinessProfile;
import dev.fintech.omniledger.model.GovernmentProfile;
import dev.fintech.omniledger.model.IndividualProfile;
import dev.fintech.omniledger.model.Party;
import dev.fintech.omniledger.model.enums.PartyType;
import dev.fintech.omniledger.model.enums.ResidentialStatus;
import dev.fintech.omniledger.repository.BusinessProfileRepository;
import dev.fintech.omniledger.repository.GovernmentProfileRepository;
import dev.fintech.omniledger.repository.IndividualProfileRepository;
import dev.fintech.omniledger.repository.PartyRepository;
import dev.fintech.omniledger.service.impl.PartyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests validating multi-party onboarding, FEMA rules, and statutory constraints.
 */
@ExtendWith(MockitoExtension.class)
class PartyServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private IndividualProfileRepository individualProfileRepository;

    @Mock
    private BusinessProfileRepository businessProfileRepository;

    @Mock
    private GovernmentProfileRepository governmentProfileRepository;

    @Mock
    private EventService eventService;

    @InjectMocks
    private PartyServiceImpl partyService;

    private UUID mockPartyId;

    @BeforeEach
    void setUp() {
        mockPartyId = UUID.randomUUID();
    }

    // =========================================================================
    // INDIVIDUAL ONBOARDING TESTS
    // =========================================================================

    @Test
    @DisplayName("Should successfully onboard Resident Indian with valid PAN")
    void onboardIndividual_ResidentIndian_Success() {
        CreateIndividualPartyRequest request = new CreateIndividualPartyRequest(
                "Rahul Sharma",
                "rahul@example.com",
                "+919876543210",
                "ABCDE1234F",
                ResidentialStatus.RESIDENT_INDIAN,
                "IN",
                LocalDate.of(1990, 5, 15)
        );

        when(individualProfileRepository.existsByPanNumber("ABCDE1234F")).thenReturn(false);
        when(partyRepository.save(any(Party.class))).thenAnswer(invocation -> {
            Party p = invocation.getArgument(0);
            p.setId(mockPartyId);
            return p;
        });

        PartyResponse response = partyService.onboardIndividual(request);

        assertNotNull(response);
        assertEquals(mockPartyId, response.partyId());
        assertEquals(PartyType.INDIVIDUAL, response.partyType());
        assertEquals("Rahul Sharma", response.displayName());
        assertEquals("ABCDE1234F", response.statutoryIdentifier());

        verify(individualProfileRepository).save(any(IndividualProfile.class));
        verify(eventService).recordGenericEvent(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should successfully onboard Non-Resident Indian (NRI) with foreign residence")
    void onboardIndividual_NRI_Success() {
        CreateIndividualPartyRequest request = new CreateIndividualPartyRequest(
                "Priya Patel",
                "priya@example.com",
                "+971501234567",
                "XYZDE5678G",
                ResidentialStatus.NON_RESIDENT_INDIAN,
                "AE", // Dubai / UAE
                LocalDate.of(1988, 8, 20)
        );

        when(individualProfileRepository.existsByPanNumber("XYZDE5678G")).thenReturn(false);
        when(partyRepository.save(any(Party.class))).thenAnswer(invocation -> {
            Party p = invocation.getArgument(0);
            p.setId(mockPartyId);
            return p;
        });

        PartyResponse response = partyService.onboardIndividual(request);

        assertNotNull(response);
        assertEquals(PartyType.INDIVIDUAL, response.partyType());
        assertEquals("Priya Patel", response.displayName());
    }

    @Test
    @DisplayName("FEMA Violation: Should reject Non-Resident Indian (NRI) whose country of residence is IN")
    void onboardIndividual_NRI_FemaViolation_ThrowsException() {
        CreateIndividualPartyRequest request = new CreateIndividualPartyRequest(
                "Rohan Verma",
                "rohan@example.com",
                "+919876543210",
                "ABCDE1234F",
                ResidentialStatus.NON_RESIDENT_INDIAN,
                "IN", // Invalid: NRI cannot reside in India
                LocalDate.of(1992, 1, 10)
        );

        when(individualProfileRepository.existsByPanNumber("ABCDE1234F")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                partyService.onboardIndividual(request));

        assertTrue(ex.getMessage().contains("Non-Resident Indian (NRI) country of residence cannot be IN"));
    }

    @Test
    @DisplayName("Should throw DuplicatePanException when PAN already exists")
    void onboardIndividual_DuplicatePan_ThrowsException() {
        CreateIndividualPartyRequest request = new CreateIndividualPartyRequest(
                "Duplicate User",
                "dup@example.com",
                "+919876543210",
                "ABCDE1234F",
                ResidentialStatus.RESIDENT_INDIAN,
                "IN",
                LocalDate.of(1995, 3, 25)
        );

        when(individualProfileRepository.existsByPanNumber("ABCDE1234F")).thenReturn(true);

        assertThrows(DuplicatePanException.class, () ->
                partyService.onboardIndividual(request));
    }

    @Test
    @DisplayName("Should reject malformed Indian PAN format")
    void onboardIndividual_MalformedPan_ThrowsException() {
        CreateIndividualPartyRequest request = new CreateIndividualPartyRequest(
                "Bad Pan User",
                "bad@example.com",
                "+919876543210",
                "INVALIDPAN", // 10 chars but not matching 5 letters + 4 digits + 1 letter
                ResidentialStatus.RESIDENT_INDIAN,
                "IN",
                LocalDate.of(1995, 3, 25)
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                partyService.onboardIndividual(request));

        assertTrue(ex.getMessage().contains("Invalid PAN format"));
    }

    // =========================================================================
    // BUSINESS ONBOARDING TESTS
    // =========================================================================

    @Test
    @DisplayName("Should successfully onboard domestic Indian business with valid GSTIN and Corporate PAN")
    void onboardBusiness_Domestic_Success() {
        CreateBusinessPartyRequest request = new CreateBusinessPartyRequest(
                "Acme Retail Technologies Pvt Ltd",
                "AcmePay",
                "IN",
                "27ABCDE1234F1Z5",
                "U72900MH2020PTC123456",
                "ABCDE1234F",
                null,
                "billing@acmepay.com",
                "+912212345678"
        );

        when(businessProfileRepository.existsByGstin("27ABCDE1234F1Z5")).thenReturn(false);
        when(businessProfileRepository.existsByCorporatePan("ABCDE1234F")).thenReturn(false);
        when(partyRepository.save(any(Party.class))).thenAnswer(invocation -> {
            Party p = invocation.getArgument(0);
            p.setId(mockPartyId);
            return p;
        });

        PartyResponse response = partyService.onboardBusiness(request);

        assertNotNull(response);
        assertEquals(mockPartyId, response.partyId());
        assertEquals(PartyType.BUSINESS, response.partyType());
        assertEquals("Acme Retail Technologies Pvt Ltd", response.displayName());
        assertEquals("27ABCDE1234F1Z5", response.statutoryIdentifier());

        verify(businessProfileRepository).save(any(BusinessProfile.class));
    }

    @Test
    @DisplayName("Should reject domestic Indian business if GSTIN is missing")
    void onboardBusiness_Domestic_MissingGstin_ThrowsException() {
        CreateBusinessPartyRequest request = new CreateBusinessPartyRequest(
                "Acme Without GSTIN",
                null,
                "IN",
                null, // Missing GSTIN
                null,
                "ABCDE1234F",
                null,
                "billing@acme.com",
                null
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                partyService.onboardBusiness(request));

        assertTrue(ex.getMessage().contains("GSTIN is mandatory for domestic Indian businesses"));
    }

    @Test
    @DisplayName("Should successfully onboard foreign corporation with foreign registration number")
    void onboardBusiness_Foreign_Success() {
        CreateBusinessPartyRequest request = new CreateBusinessPartyRequest(
                "Global Stripe Inc",
                "Stripe USA",
                "US",
                null, // Not required for foreign entity
                null, // No Indian CIN
                null, // No Indian PAN
                "US-EIN-12-3456789",
                "corporate@stripe.com",
                "+14151234567"
        );

        when(partyRepository.save(any(Party.class))).thenAnswer(invocation -> {
            Party p = invocation.getArgument(0);
            p.setId(mockPartyId);
            return p;
        });

        PartyResponse response = partyService.onboardBusiness(request);

        assertNotNull(response);
        assertEquals(PartyType.BUSINESS, response.partyType());
        assertEquals("Global Stripe Inc", response.displayName());
        assertEquals("US-EIN-12-3456789", response.statutoryIdentifier());

        verify(businessProfileRepository).save(any(BusinessProfile.class));
    }

    @Test
    @DisplayName("Should reject foreign corporation if foreign registration number is missing")
    void onboardBusiness_Foreign_MissingRegNumber_ThrowsException() {
        CreateBusinessPartyRequest request = new CreateBusinessPartyRequest(
                "Foreign Company Without Reg",
                null,
                "GB", // United Kingdom
                null,
                null,
                null,
                null, // Missing foreignRegistrationNumber
                "contact@ukcompany.co.uk",
                null
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                partyService.onboardBusiness(request));

        assertTrue(ex.getMessage().contains("Foreign registration number / EIN is mandatory"));
    }

    // =========================================================================
    // GOVERNMENT ONBOARDING TESTS
    // =========================================================================

    @Test
    @DisplayName("Should successfully onboard sovereign tax department")
    void onboardGovernment_Success() {
        CreateGovernmentPartyRequest request = new CreateGovernmentPartyRequest(
                "Central Board of Indirect Taxes and Customs",
                "CENTRAL",
                "CBIC-NODAL-01",
                "GST"
        );

        when(governmentProfileRepository.existsByNodalAgencyCode("CBIC-NODAL-01")).thenReturn(false);
        when(partyRepository.save(any(Party.class))).thenAnswer(invocation -> {
            Party p = invocation.getArgument(0);
            p.setId(mockPartyId);
            return p;
        });

        PartyResponse response = partyService.onboardGovernment(request);

        assertNotNull(response);
        assertEquals(PartyType.GOVERNMENT, response.partyType());
        assertEquals("Central Board of Indirect Taxes and Customs", response.displayName());
        assertEquals("CBIC-NODAL-01", response.statutoryIdentifier());

        verify(governmentProfileRepository).save(any(GovernmentProfile.class));
    }

    @Test
    @DisplayName("Should reject government agency with duplicate nodal agency code")
    void onboardGovernment_DuplicateNodalCode_ThrowsException() {
        CreateGovernmentPartyRequest request = new CreateGovernmentPartyRequest(
                "Duplicate Tax Dept",
                "CENTRAL",
                "CBIC-NODAL-01",
                "GST"
        );

        when(governmentProfileRepository.existsByNodalAgencyCode("CBIC-NODAL-01")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                partyService.onboardGovernment(request));

        assertTrue(ex.getMessage().contains("Government agency with nodal code CBIC-NODAL-01 already exists"));
    }
}
