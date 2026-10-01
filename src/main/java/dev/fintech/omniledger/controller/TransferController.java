package dev.fintech.omniledger.controller;

import dev.fintech.omniledger.dto.request.TransferRequest;
import dev.fintech.omniledger.dto.response.TransferResponse;
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
 * REST controller for executing double-entry ledger transfers.
 */
@Tag(name = "Transfers", description = "Endpoints for immutable double-entry ledger money movements")
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @Operation(summary = "Execute transfer", description = "Executes an immutable, balanced double-entry transfer between two accounts")
    @PostMapping
    public ResponseEntity<TransferResponse> executeTransfer(@RequestBody TransferRequest request) {
        TransferResponse response = transferService.executeTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get transfer by ID", description = "Retrieves an immutable journal entry receipt along with its debit and credit posting lines")
    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getTransferById(@PathVariable UUID id) {
        TransferResponse response = transferService.getTransferById(id);
        return ResponseEntity.ok(response);
    }
}
