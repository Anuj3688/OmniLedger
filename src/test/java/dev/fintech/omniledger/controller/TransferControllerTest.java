package dev.fintech.omniledger.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.fintech.omniledger.dto.request.TransferRequest;
import dev.fintech.omniledger.dto.response.PostingLineResponse;
import dev.fintech.omniledger.dto.response.TransferResponse;
import dev.fintech.omniledger.exception.DuplicateIdempotencyKeyException;
import dev.fintech.omniledger.exception.GlobalExceptionHandler;
import dev.fintech.omniledger.exception.InsufficientBalanceException;
import dev.fintech.omniledger.model.enums.Currency;
import dev.fintech.omniledger.model.enums.PostingType;
import dev.fintech.omniledger.service.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
@Import(GlobalExceptionHandler.class)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransferService transferService;

    @Test
    @DisplayName("POST /api/v1/transfers should return 201 Created on successful execution")
    void executeTransfer_Success() throws Exception {
        UUID sourceId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();

        TransferRequest request = new TransferRequest(
                "tx-web-001",
                sourceId,
                destId,
                new BigDecimal("250.0000"),
                Currency.INR,
                "Dinner bill split"
        );

        TransferResponse response = new TransferResponse(
                journalId,
                "tx-web-001",
                Currency.INR,
                "Dinner bill split",
                Instant.now(),
                List.of(
                        new PostingLineResponse(UUID.randomUUID(), sourceId, new BigDecimal("250.0000"), PostingType.DEBIT),
                        new PostingLineResponse(UUID.randomUUID(), destId, new BigDecimal("250.0000"), PostingType.CREDIT)
                )
        );

        when(transferService.executeTransfer(any(TransferRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.journalEntryId").value(journalId.toString()))
                .andExpect(jsonPath("$.idempotencyKey").value("tx-web-001"))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.lines.length()").value(2));
    }

    @Test
    @DisplayName("POST /api/v1/transfers should return 400 Bad Request when validation fails")
    void executeTransfer_ValidationError() throws Exception {
        // Missing idempotency key and amount
        String invalidJson = """
                {
                    "sourceAccountId": "11111111-1111-1111-1111-111111111111",
                    "destinationAccountId": "22222222-2222-2222-2222-222222222222"
                }
                """;

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("POST /api/v1/transfers should return 422 Unprocessable Entity when balance is insufficient")
    void executeTransfer_InsufficientBalance() throws Exception {
        UUID sourceId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();

        TransferRequest request = new TransferRequest(
                "tx-web-overdraft",
                sourceId,
                destId,
                new BigDecimal("9999.0000"),
                Currency.INR,
                "Overdraft"
        );

        when(transferService.executeTransfer(any(TransferRequest.class)))
                .thenThrow(new InsufficientBalanceException(sourceId, new BigDecimal("100.0000"), new BigDecimal("9999.0000")));

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_BALANCE"));
    }

    @Test
    @DisplayName("POST /api/v1/transfers should return 409 Conflict when idempotency key conflicts")
    void executeTransfer_DuplicateIdempotencyConflict() throws Exception {
        UUID sourceId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();

        TransferRequest request = new TransferRequest(
                "tx-web-conflict",
                sourceId,
                destId,
                new BigDecimal("500.0000"),
                Currency.INR,
                "Conflict"
        );

        when(transferService.executeTransfer(any(TransferRequest.class)))
                .thenThrow(new DuplicateIdempotencyKeyException("tx-web-conflict"));

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_IDEMPOTENCY_KEY"));
    }

    @Test
    @DisplayName("GET /api/v1/transfers/{id} should return 200 OK with transfer receipt")
    void getTransferById_Success() throws Exception {
        UUID journalId = UUID.randomUUID();
        TransferResponse response = new TransferResponse(
                journalId,
                "tx-get-001",
                Currency.INR,
                "Memo",
                Instant.now(),
                List.of()
        );

        when(transferService.getTransferById(journalId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/transfers/" + journalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.journalEntryId").value(journalId.toString()))
                .andExpect(jsonPath("$.idempotencyKey").value("tx-get-001"));
    }
}
