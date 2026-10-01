package dev.fintech.omniledger.controller;

import dev.fintech.omniledger.dto.TransferRequest;
import dev.fintech.omniledger.dto.TransferResponse;
import dev.fintech.omniledger.service.TransferService;
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
 * REST controller for executing ACID-compliant transfers and retrieving transaction audit receipts.
 */
@Tag(name = "Transfers", description = "Endpoints for double-entry financial transfers and ledger audits")
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @Operation(
            summary = "Execute financial transfer",
            description = "Performs an atomic, ACID-compliant double-entry transfer between two accounts with idempotency protection"
    )
    @PostMapping
    public ResponseEntity<TransferResponse> executeTransfer(@RequestBody TransferRequest request) {
        TransferResponse response = transferService.executeTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get transfer audit receipt",
            description = "Retrieves an immutable journal entry and its balanced debit/credit posting lines by UUID"
    )
    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getTransferById(@PathVariable UUID id) {
        TransferResponse response = transferService.getTransferById(id);
        return ResponseEntity.ok(response);
    }
}
