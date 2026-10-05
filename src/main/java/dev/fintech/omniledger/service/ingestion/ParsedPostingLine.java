package dev.fintech.omniledger.service.ingestion;

import dev.fintech.omniledger.model.enums.Currency;
import dev.fintech.omniledger.model.enums.PostingType;

import java.math.BigDecimal;

public record ParsedPostingLine(
        int rowNumber,
        String rawRecord,
        String referenceId,
        String externalAccountId,
        BigDecimal amount,
        PostingType direction,
        Currency currency,
        String description
) {}
