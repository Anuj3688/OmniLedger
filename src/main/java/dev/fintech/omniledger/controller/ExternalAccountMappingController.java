package dev.fintech.omniledger.controller;

import dev.fintech.omniledger.dto.request.ExternalAccountMappingRequest;
import dev.fintech.omniledger.dto.response.ExternalAccountMappingResponse;
import dev.fintech.omniledger.service.ExternalAccountMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "External Account Mappings", description = "Endpoints for linking partner external account IDs to internal OmniLedger accounts")
@RestController
@RequestMapping("/api/v1/ledger/account-mappings")
@RequiredArgsConstructor
public class ExternalAccountMappingController {

    private final ExternalAccountMappingService mappingService;

    @Operation(summary = "Register external account mapping", description = "Maps an external system partner account identifier to an OmniLedger Account UUID")
    @PostMapping
    public ResponseEntity<ExternalAccountMappingResponse> createMapping(@Valid @RequestBody ExternalAccountMappingRequest request) {
        ExternalAccountMappingResponse response = mappingService.createMapping(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get external account mapping", description = "Retrieves mapping for a specific external partner and external account identifier")
    @GetMapping("/{externalSystem}/{externalAccountId}")
    public ResponseEntity<ExternalAccountMappingResponse> getMapping(
            @PathVariable("externalSystem") String externalSystem,
            @PathVariable("externalAccountId") String externalAccountId
    ) {
        ExternalAccountMappingResponse response = mappingService.getMapping(externalSystem, externalAccountId);
        return ResponseEntity.ok(response);
    }
}
