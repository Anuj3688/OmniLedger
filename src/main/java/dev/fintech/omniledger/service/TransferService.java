package dev.fintech.omniledger.service;

import dev.fintech.omniledger.dto.request.TransferRequest;
import dev.fintech.omniledger.dto.response.TransferResponse;

import java.util.UUID;

/**
 * Service contract for high-concurrency double-entry ledger transfers.
 */
public interface TransferService {

    TransferResponse executeTransfer(TransferRequest request);

    TransferResponse getTransferById(UUID id);
}
