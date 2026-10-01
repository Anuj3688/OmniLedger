package dev.fintech.omniledger.service;

import dev.fintech.omniledger.dto.request.CreateBusinessPartyRequest;
import dev.fintech.omniledger.dto.request.CreateGovernmentPartyRequest;
import dev.fintech.omniledger.dto.request.CreateIndividualPartyRequest;
import dev.fintech.omniledger.dto.response.PartyResponse;

import java.util.UUID;

/**
 * Service contract for onboarding and managing legal entities (Parties).
 */
public interface PartyService {

    PartyResponse onboardIndividual(CreateIndividualPartyRequest request);

    PartyResponse onboardBusiness(CreateBusinessPartyRequest request);

    PartyResponse onboardGovernment(CreateGovernmentPartyRequest request);

    PartyResponse getPartyById(UUID partyId);
}
