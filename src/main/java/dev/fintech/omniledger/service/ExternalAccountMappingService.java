package dev.fintech.omniledger.service;

import dev.fintech.omniledger.dto.request.ExternalAccountMappingRequest;
import dev.fintech.omniledger.dto.response.ExternalAccountMappingResponse;

import java.util.Optional;
import java.util.UUID;

public interface ExternalAccountMappingService {

    ExternalAccountMappingResponse createMapping(ExternalAccountMappingRequest request);

    ExternalAccountMappingResponse getMapping(String externalSystem, String externalAccountId);

    Optional<UUID> resolveAccountId(String externalSystem, String externalAccountId);
}
