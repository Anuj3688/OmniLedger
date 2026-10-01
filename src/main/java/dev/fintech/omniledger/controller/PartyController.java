package dev.fintech.omniledger.controller;

import dev.fintech.omniledger.dto.request.CreateBusinessPartyRequest;
import dev.fintech.omniledger.dto.request.CreateGovernmentPartyRequest;
import dev.fintech.omniledger.dto.request.CreateIndividualPartyRequest;
import dev.fintech.omniledger.dto.response.PartyResponse;
import dev.fintech.omniledger.service.PartyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for multi-party identity onboarding and compliance retrieval.
 * Supports natural individuals, corporate businesses, and government tax bodies.
 */
@Tag(name = "Parties", description = "Endpoints for multi-entity onboarding (Individuals, Businesses, Government Agencies)")
@RestController
@RequestMapping("/api/v1/parties")
@RequiredArgsConstructor
public class PartyController {

    private final PartyService partyService;

    @Operation(summary = "Onboard individual", description = "Registers a natural retail individual customer with PAN verification")
    @PostMapping("/individuals")
    public ResponseEntity<PartyResponse> onboardIndividual(@RequestBody CreateIndividualPartyRequest request) {
        PartyResponse response = partyService.onboardIndividual(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Onboard business", description = "Registers a commercial merchant or enterprise entity with GSTIN and Corporate PAN")
    @PostMapping("/businesses")
    public ResponseEntity<PartyResponse> onboardBusiness(@RequestBody CreateBusinessPartyRequest request) {
        PartyResponse response = partyService.onboardBusiness(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Onboard government agency", description = "Registers a sovereign tax department or nodal municipal agency")
    @PostMapping("/governments")
    public ResponseEntity<PartyResponse> onboardGovernment(@RequestBody CreateGovernmentPartyRequest request) {
        PartyResponse response = partyService.onboardGovernment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get party by ID", description = "Retrieves unified Party details and statutory identifiers by UUID")
    @GetMapping("/{id}")
    public ResponseEntity<PartyResponse> getPartyById(@PathVariable UUID id) {
        PartyResponse response = partyService.getPartyById(id);
        return ResponseEntity.ok(response);
    }
}
