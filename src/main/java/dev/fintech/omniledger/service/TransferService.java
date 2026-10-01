package dev.fintech.omniledger.service;

import dev.fintech.omniledger.dto.TransferRequest;
import dev.fintech.omniledger.dto.TransferResponse;

import java.util.UUID;

/**
 * Service contract for ACID double-entry transfers and transaction audits.
 */
public interface TransferService {

    TransferResponse executeTransfer(TransferRequest request);

    TransferResponse getTransferById(UUID journalEntryId);
}
