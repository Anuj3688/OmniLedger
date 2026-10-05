package dev.fintech.omniledger.service.impl;

import dev.fintech.omniledger.dto.request.ExternalAccountMappingRequest;
import dev.fintech.omniledger.dto.response.ExternalAccountMappingResponse;
import dev.fintech.omniledger.exception.AccountNotFoundException;
import dev.fintech.omniledger.exception.ExternalAccountMappingNotFoundException;
import dev.fintech.omniledger.model.Account;
import dev.fintech.omniledger.model.ExternalAccountMapping;
import dev.fintech.omniledger.repository.AccountRepository;
import dev.fintech.omniledger.repository.ExternalAccountMappingRepository;
import dev.fintech.omniledger.service.ExternalAccountMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalAccountMappingServiceImpl implements ExternalAccountMappingService {

    private final ExternalAccountMappingRepository mappingRepository;
    private final AccountRepository accountRepository;

    private final Map<String, UUID> localResolutionCache = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public ExternalAccountMappingResponse createMapping(ExternalAccountMappingRequest request) {
        log.info("Creating external account mapping for system={} externalAccountId={} to accountId={}",
                request.getExternalSystem(), request.getExternalAccountId(), request.getAccountId());

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.getAccountId()));

        ExternalAccountMapping mapping = mappingRepository
                .findByExternalSystemAndExternalAccountId(request.getExternalSystem(), request.getExternalAccountId())
                .orElse(null);

        if (mapping != null) {
            mapping.setAccount(account);
        } else {
            mapping = ExternalAccountMapping.builder()
                    .externalSystem(request.getExternalSystem())
                    .externalAccountId(request.getExternalAccountId())
                    .account(account)
                    .build();
        }

        ExternalAccountMapping saved = mappingRepository.save(mapping);
        localResolutionCache.put(buildCacheKey(saved.getExternalSystem(), saved.getExternalAccountId()), account.getId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ExternalAccountMappingResponse getMapping(String externalSystem, String externalAccountId) {
        return mappingRepository.findByExternalSystemAndExternalAccountId(externalSystem, externalAccountId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ExternalAccountMappingNotFoundException(externalSystem, externalAccountId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> resolveAccountId(String externalSystem, String externalAccountId) {
        String cacheKey = buildCacheKey(externalSystem, externalAccountId);
        UUID cachedId = localResolutionCache.get(cacheKey);
        if (cachedId != null) {
            return Optional.of(cachedId);
        }

        Optional<UUID> accountId = mappingRepository.findAccountId(externalSystem, externalAccountId);
        accountId.ifPresent(uuid -> localResolutionCache.put(cacheKey, uuid));
        return accountId;
    }

    private String buildCacheKey(String externalSystem, String externalAccountId) {
        return externalSystem + "::" + externalAccountId;
    }

    private ExternalAccountMappingResponse mapToResponse(ExternalAccountMapping mapping) {
        return ExternalAccountMappingResponse.builder()
                .mappingId(mapping.getMappingId())
                .externalSystem(mapping.getExternalSystem())
                .externalAccountId(mapping.getExternalAccountId())
                .accountId(mapping.getAccount().getId())
                .createdAt(mapping.getCreatedAt())
                .build();
    }
}
