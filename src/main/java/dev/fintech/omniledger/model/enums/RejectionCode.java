package dev.fintech.omniledger.model.enums;

/**
 * Standardized error classification codes for quarantined file ingestion records.
 * Prevents arbitrary string values and provides strict categorisation for ops reconciliation.
 */
public enum RejectionCode {
    /**
     * The record line is missing required fields or has an invalid delimiter/structure.
     */
    MALFORMED_RECORD,

    /**
     * The external account ID cannot be resolved to an internal OmniLedger account.
     */
    UNKNOWN_ACCOUNT,

    /**
     * The transaction reference contains debits and credits that do not balance (Debits != Credits).
     */
    UNBALANCED_ENTRY,

    /**
     * The monetary amount is null, zero, negative, or exceeds supported decimal precision.
     */
    INVALID_AMOUNT,

    /**
     * The currency code is unrecognized or not supported by OmniLedger.
     */
    INVALID_CURRENCY,

    /**
     * Posting lines in the same transaction reference have conflicting currencies or conflict with account currency.
     */
    CURRENCY_MISMATCH,

    /**
     * The transaction reference has already been executed/posted in OmniLedger (idempotency conflict).
     */
    DUPLICATE_REFERENCE,

    /**
     * The source debit account does not have sufficient balance to execute the posting.
     */
    INSUFFICIENT_BALANCE
}
