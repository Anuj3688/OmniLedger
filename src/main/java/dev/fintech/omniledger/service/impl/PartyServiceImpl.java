package dev.fintech.omniledger.service.impl;

import dev.fintech.omniledger.dto.request.CreateBusinessPartyRequest;
import dev.fintech.omniledger.dto.request.CreateGovernmentPartyRequest;
import dev.fintech.omniledger.dto.request.CreateIndividualPartyRequest;
import dev.fintech.omniledger.dto.response.PartyResponse;
import dev.fintech.omniledger.exception.DuplicatePanException;
import dev.fintech.omniledger.exception.PartyNotFoundException;
import dev.fintech.omniledger.model.BusinessProfile;
import dev.fintech.omniledger.model.GovernmentProfile;
import dev.fintech.omniledger.model.IndividualProfile;
import dev.fintech.omniledger.model.Party;
import dev.fintech.omniledger.model.enums.EventType;
import dev.fintech.omniledger.model.enums.PartyStatus;
import dev.fintech.omniledger.model.enums.PartyType;
import dev.fintech.omniledger.model.enums.ResidentialStatus;
import dev.fintech.omniledger.repository.BusinessProfileRepository;
import dev.fintech.omniledger.repository.GovernmentProfileRepository;
import dev.fintech.omniledger.repository.IndividualProfileRepository;
import dev.fintech.omniledger.repository.PartyRepository;
import dev.fintech.omniledger.service.EventService;
import dev.fintech.omniledger.service.PartyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Production implementation of PartyService orchestrating multi-entity onboarding,
 * FEMA residential compliance, domestic vs. foreign corporate validation, and identity audits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartyServiceImpl implements PartyService {

    private final PartyRepository partyRepository;
    private final IndividualProfileRepository individualProfileRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final GovernmentProfileRepository governmentProfileRepository;
    private final EventService eventService;

    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern GSTIN_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");

    @Override
    @Transactional
    public PartyResponse onboardIndividual(CreateIndividualPartyRequest request) {
        log.info("Received request to onboard INDIVIDUAL party: PAN={}, residentialStatus={}",
                request != null ? request.panNumber() : null, request != null ? request.residentialStatus() : null);

        String normalizedPan = validateIndividualOnboarding(request);

        Party party = partyRepository.save(Party.builder()
                .partyType(PartyType.INDIVIDUAL)
                .status(PartyStatus.ACTIVE)
                .build());

        IndividualProfile profile = IndividualProfile.builder()
                .partyId(party.getId())
                .party(party)
                .fullName(request.fullName().trim())
                .panNumber(normalizedPan)
                .residentialStatus(request.residentialStatus())
                .countryOfResidence(request.countryOfResidence().trim().toUpperCase())
                .email(request.email() != null ? request.email().trim().toLowerCase() : null)
                .phoneNumber(request.phoneNumber() != null ? request.phoneNumber().trim() : null)
                .dateOfBirth(request.dateOfBirth())
                .build();

        individualProfileRepository.save(profile);
        log.info("Successfully onboarded INDIVIDUAL partyId={} with PAN={}, status={}",
                party.getId(), normalizedPan, profile.getResidentialStatus());

        eventService.recordGenericEvent(
                EventType.ACCOUNT_CREATED,
                party.getId(),
                null,
                null,
                null,
                String.format("Individual customer onboarded: %s (PAN: %s, ResidentialStatus: %s, Country: %s)",
                        profile.getFullName(), normalizedPan, profile.getResidentialStatus(), profile.getCountryOfResidence()),
                "SUCCESS",
                null
        );

        return new PartyResponse(
                party.getId(),
                party.getPartyType(),
                party.getStatus(),
                profile.getFullName(),
                profile.getPanNumber(),
                party.getCreatedAt(),
                party.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public PartyResponse onboardBusiness(CreateBusinessPartyRequest request) {
        log.info("Received request to onboard BUSINESS party: name={}, country={}",
                request != null ? request.legalBusinessName() : null,
                request != null ? request.countryOfIncorporation() : null);

        validateBusinessOnboarding(request);

        String country = request.countryOfIncorporation().trim().toUpperCase();
        String normalizedGstin = request.gstin() != null && !request.gstin().isBlank() ? request.gstin().trim().toUpperCase() : null;
        String normalizedCorporatePan = request.corporatePan() != null && !request.corporatePan().isBlank() ? request.corporatePan().trim().toUpperCase() : null;
        String foreignReg = request.foreignRegistrationNumber() != null && !request.foreignRegistrationNumber().isBlank() ? request.foreignRegistrationNumber().trim() : null;

        Party party = partyRepository.save(Party.builder()
                .partyType(PartyType.BUSINESS)
                .status(PartyStatus.ACTIVE)
                .build());

        BusinessProfile profile = BusinessProfile.builder()
                .partyId(party.getId())
                .party(party)
                .legalBusinessName(request.legalBusinessName().trim())
                .tradeName(request.tradeName() != null ? request.tradeName().trim() : null)
                .countryOfIncorporation(country)
                .gstin(normalizedGstin)
                .cinNumber(request.cinNumber() != null ? request.cinNumber().trim().toUpperCase() : null)
                .corporatePan(normalizedCorporatePan)
                .foreignRegistrationNumber(foreignReg)
                .email(request.email() != null ? request.email().trim().toLowerCase() : null)
                .phoneNumber(request.phoneNumber() != null ? request.phoneNumber().trim() : null)
                .build();

        businessProfileRepository.save(profile);
        log.info("Successfully onboarded BUSINESS partyId={} (country={})", party.getId(), country);

        String statutoryId = normalizedGstin != null ? normalizedGstin : (normalizedCorporatePan != null ? normalizedCorporatePan : foreignReg);

        eventService.recordGenericEvent(
                EventType.ACCOUNT_CREATED,
                party.getId(),
                null,
                null,
                null,
                String.format("Business party onboarded: %s (Country: %s, ID: %s)",
                        profile.getLegalBusinessName(), country, statutoryId),
                "SUCCESS",
                null
        );

        return new PartyResponse(
                party.getId(),
                party.getPartyType(),
                party.getStatus(),
                profile.getLegalBusinessName(),
                statutoryId,
                party.getCreatedAt(),
                party.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public PartyResponse onboardGovernment(CreateGovernmentPartyRequest request) {
        log.info("Received request to onboard GOVERNMENT party: department={}", request != null ? request.departmentName() : null);

        validateGovernmentOnboarding(request);

        String normalizedNodalCode = request.nodalAgencyCode().trim().toUpperCase();

        Party party = partyRepository.save(Party.builder()
                .partyType(PartyType.GOVERNMENT)
                .status(PartyStatus.ACTIVE)
                .build());

        GovernmentProfile profile = GovernmentProfile.builder()
                .partyId(party.getId())
                .party(party)
                .departmentName(request.departmentName().trim())
                .jurisdiction(request.jurisdiction().trim().toUpperCase())
                .nodalAgencyCode(normalizedNodalCode)
                .taxCategoryCode(request.taxCategoryCode().trim().toUpperCase())
                .build();

        governmentProfileRepository.save(profile);
        log.info("Successfully onboarded GOVERNMENT partyId={} with nodalCode={}", party.getId(), normalizedNodalCode);

        eventService.recordGenericEvent(
                EventType.ACCOUNT_CREATED,
                party.getId(),
                null,
                null,
                null,
                "Government agency onboarded: " + profile.getDepartmentName() + " (" + normalizedNodalCode + ")",
                "SUCCESS",
                null
        );

        return new PartyResponse(
                party.getId(),
                party.getPartyType(),
                party.getStatus(),
                profile.getDepartmentName(),
                profile.getNodalAgencyCode(),
                party.getCreatedAt(),
                party.getUpdatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PartyResponse getPartyById(UUID partyId) {
        log.debug("Fetching Party details for partyId={}", partyId);
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> {
                    log.warn("Party lookup failed: partyId={} not found", partyId);
                    return new PartyNotFoundException(partyId);
                });

        return mapToPartyResponse(party);
    }

    private PartyResponse mapToPartyResponse(Party party) {
        String displayName = "Unknown Party";
        String statutoryId = "N/A";

        switch (party.getPartyType()) {
            case INDIVIDUAL -> {
                IndividualProfile profile = individualProfileRepository.findById(party.getId()).orElse(null);
                if (profile != null) {
                    displayName = profile.getFullName();
                    statutoryId = profile.getPanNumber();
                }
            }
            case BUSINESS -> {
                BusinessProfile profile = businessProfileRepository.findById(party.getId()).orElse(null);
                if (profile != null) {
                    displayName = profile.getLegalBusinessName();
                    statutoryId = profile.getGstin() != null ? profile.getGstin()
                            : (profile.getCorporatePan() != null ? profile.getCorporatePan() : profile.getForeignRegistrationNumber());
                }
            }
            case GOVERNMENT -> {
                GovernmentProfile profile = governmentProfileRepository.findById(party.getId()).orElse(null);
                if (profile != null) {
                    displayName = profile.getDepartmentName();
                    statutoryId = profile.getNodalAgencyCode();
                }
            }
            case PLATFORM -> {
                displayName = "OmniLedger Platform Core";
                statutoryId = "PLATFORM-SYSTEM";
            }
        }

        return new PartyResponse(
                party.getId(),
                party.getPartyType(),
                party.getStatus(),
                displayName,
                statutoryId,
                party.getCreatedAt(),
                party.getUpdatedAt()
        );
    }

    private String validateIndividualOnboarding(CreateIndividualPartyRequest request) {
        if (request == null) throw new IllegalArgumentException("CreateIndividualPartyRequest cannot be null");
        if (request.fullName() == null || request.fullName().isBlank()) {
            throw new IllegalArgumentException("Individual full name is mandatory");
        }
        if (request.panNumber() == null || request.panNumber().isBlank()) {
            throw new IllegalArgumentException("Individual PAN number is mandatory");
        }

        String normalizedPan = request.panNumber().trim().toUpperCase();
        if (!PAN_PATTERN.matcher(normalizedPan).matches()) {
            throw new IllegalArgumentException("Invalid PAN format: " + normalizedPan + ". Expected format: ABCDE1234F");
        }

        if (individualProfileRepository.existsByPanNumber(normalizedPan)) {
            throw new DuplicatePanException(normalizedPan);
        }

        // FEMA Check: If NRI or Foreign national, country of residence cannot be 'IN'
        if (request.residentialStatus() == ResidentialStatus.NON_RESIDENT_INDIAN && "IN".equalsIgnoreCase(request.countryOfResidence().trim())) {
            throw new IllegalArgumentException("Non-Resident Indian (NRI) country of residence cannot be IN (India)");
        }

        return normalizedPan;
    }

    private void validateBusinessOnboarding(CreateBusinessPartyRequest request) {
        if (request == null) throw new IllegalArgumentException("CreateBusinessPartyRequest cannot be null");
        if (request.legalBusinessName() == null || request.legalBusinessName().isBlank()) {
            throw new IllegalArgumentException("Legal business name is mandatory");
        }

        String country = request.countryOfIncorporation() != null ? request.countryOfIncorporation().trim().toUpperCase() : "IN";

        // Domestic Indian Business Rules
        if ("IN".equals(country)) {
            if (request.gstin() == null || request.gstin().isBlank()) {
                throw new IllegalArgumentException("GSTIN is mandatory for domestic Indian businesses");
            }
            if (request.corporatePan() == null || request.corporatePan().isBlank()) {
                throw new IllegalArgumentException("Corporate PAN is mandatory for domestic Indian businesses");
            }

            String normalizedGstin = request.gstin().trim().toUpperCase();
            if (!GSTIN_PATTERN.matcher(normalizedGstin).matches()) {
                throw new IllegalArgumentException("Invalid Indian GSTIN format: " + normalizedGstin + ". Expected 15-character format (e.g. 27ABCDE1234F1Z5)");
            }
            if (businessProfileRepository.existsByGstin(normalizedGstin)) {
                throw new IllegalArgumentException("Business with GSTIN " + normalizedGstin + " already exists");
            }

            String normalizedCorporatePan = request.corporatePan().trim().toUpperCase();
            if (!PAN_PATTERN.matcher(normalizedCorporatePan).matches()) {
                throw new IllegalArgumentException("Invalid Corporate PAN format: " + normalizedCorporatePan);
            }
            if (businessProfileRepository.existsByCorporatePan(normalizedCorporatePan)) {
                throw new IllegalArgumentException("Business with Corporate PAN " + normalizedCorporatePan + " already exists");
            }
        } else {
            // Foreign Business Rules
            if (request.foreignRegistrationNumber() == null || request.foreignRegistrationNumber().isBlank()) {
                throw new IllegalArgumentException("Foreign registration number / EIN is mandatory for foreign corporations incorporated outside India");
            }
        }
    }

    private void validateGovernmentOnboarding(CreateGovernmentPartyRequest request) {
        if (request == null) throw new IllegalArgumentException("CreateGovernmentPartyRequest cannot be null");
        if (request.departmentName() == null || request.departmentName().isBlank()) {
            throw new IllegalArgumentException("Government department name is mandatory");
        }
        if (request.nodalAgencyCode() == null || request.nodalAgencyCode().isBlank()) {
            throw new IllegalArgumentException("Nodal agency code is mandatory");
        }
        if (request.taxCategoryCode() == null || request.taxCategoryCode().isBlank()) {
            throw new IllegalArgumentException("Tax category code is mandatory");
        }

        String normalizedNodalCode = request.nodalAgencyCode().trim().toUpperCase();
        if (governmentProfileRepository.existsByNodalAgencyCode(normalizedNodalCode)) {
            throw new IllegalArgumentException("Government agency with nodal code " + normalizedNodalCode + " already exists");
        }
    }
}
