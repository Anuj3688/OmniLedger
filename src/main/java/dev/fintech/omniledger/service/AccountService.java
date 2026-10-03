package dev.fintech.omniledger.service;

import dev.fintech.omniledger.dto.request.CreateAccountRequest;
import dev.fintech.omniledger.dto.response.AccountResponse;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse getAccountById(UUID id);

    List<AccountResponse> getAccountsByPartyId(UUID partyId);
}
