# 📜 OmniLedger: Architecture Decision Log (ADR)

This document records all significant architectural, domain, and design decisions made during the evolution of OmniLedger. Each entry details the **Context**, the **Decision**, the **Alternatives Considered**, and the **Consequences & Benefits**.

---

## 📑 Index of Architecture Decisions

* [ADR-001: Strict Double-Entry Ledger Pattern & Immutability](#adr-001-strict-double-entry-ledger-pattern--immutability)
* [ADR-002: Deterministic Lock Acquisition for Deadlock Prevention](#adr-002-deterministic-lock-acquisition-for-deadlock-prevention)
* [ADR-003: High-Precision Monetary Math with BigDecimal and PostgreSQL NUMERIC(19,4)](#adr-003-high-precision-monetary-math-with-bigdecimal-and-postgresql-numeric194)
* [ADR-004: Type-Safe Currency Enum with Domestic Default (INR)](#adr-004-type-safe-currency-enum-with-domestic-default-inr)
* [ADR-005: Hexagonal System Event & Audit Subsystem with Isolated Transactions](#adr-005-hexagonal-system-event--audit-subsystem-with-isolated-transactions)
* [ADR-006: Enterprise Party Model (BIAN Standard) over Flat Users Table](#adr-006-enterprise-party-model-bian-standard-over-flat-users-table)
* [ADR-007: Cross-Border & FEMA Residency Compliance Modeling](#adr-007-cross-border--fema-residency-compliance-modeling)
* [ADR-008: Transient Database Contention & Throttling Resilience (Exponential Backoff + Jitter)](#adr-008-transient-database-contention--throttling-resilience-exponential-backoff--jitter)
* [ADR-009: PII At-Rest Encryption & HMAC Blind Indexing](#adr-009-pii-at-rest-encryption--hmac-blind-indexing)
* [ADR-010: Streaming File Ingestion Pipeline & Multi-Tier Deduplication](#adr-010-streaming-file-ingestion-pipeline--multi-tier-deduplication)

---

## ADR-001: Strict Double-Entry Ledger Pattern & Immutability
* **Status:** Accepted
* **Context:** Financial transfers must never result in unaccounted "creation" or "destruction" of money. Auditors require an immutable record of every transaction event.
* **Decision:** Implement an append-only double-entry ledger where every transfer produces an immutable `JournalEntry` header and at least two balanced `PostingLine` entries ($\sum \text{Debits} = \sum \text{Credits}$). No `DELETE` or in-place `UPDATE` operations are permitted on journal entries or posting lines; corrections must be executed as compensating reversal entries.
* **Consequences:** Eliminates write contention on historical entries, preserves SOX/GAAP audit compliance, and prevents data tampering.

---

## ADR-002: Deterministic Lock Acquisition for Deadlock Prevention
* **Status:** Accepted
* **Context:** When concurrent threads transfer money simultaneously between the same two accounts in opposite directions (e.g. Thread 1: Account A $\rightarrow$ Account B, Thread 2: Account B $\rightarrow$ Account A), classic circular-wait deadlocks occur in PostgreSQL row locks.
* **Decision:** Sort account `UUID`s before executing `SELECT ... FOR UPDATE` (`sourceAccountId.compareTo(destAccountId)`). All threads acquire locks in the exact same deterministic sequence.
* **Consequences:** Completely eliminates circular database deadlocks without requiring expensive distributed locks or application-level mutexes.

---

## ADR-003: High-Precision Monetary Math with BigDecimal and PostgreSQL NUMERIC(19,4)
* **Status:** Accepted
* **Context:** Floating-point numbers (`float`, `double`) introduce binary precision errors (e.g., `0.1 + 0.2 = 0.30000000000000004`), which are unacceptable in banking.
* **Decision:** Use Java `BigDecimal` and PostgreSQL `NUMERIC(19, 4)` for all balance and transfer amounts.
* **Consequences:** Eliminates rounding errors while providing 4 decimal places of sub-unit precision for micro-fees and currency calculations.

---

## ADR-004: Type-Safe Currency Enum with Domestic Default (INR)
* **Status:** Accepted
* **Context:** Using raw `String` for currency codes leads to silent errors (e.g. typos like `"inr"`, `"Inr"`, or invalid strings). Furthermore, the primary target market for OmniLedger is India.
* **Decision:** Introduce a strongly typed `Currency` enum (`INR`, `USD`, `EUR`, `GBP`) located in `dev.fintech.omniledger.model.enums`, defaulting to `Currency.INR` cinema.
* **Consequences:** Compile-time and schema-level safety; eliminates runtime currency string parsing bugs.

---

## ADR-005: Hexagonal System Event & Audit Subsystem with Isolated Transactions
* **Status:** Accepted
* **Context:** In financial services, we must track all operational events (account created, transfer initiated, transfer failed). If a transfer fails with `InsufficientBalanceException`, the enclosing Spring `@Transactional` rolls back, which would normally delete any failure audit logs recorded in that transaction.
* **Decision:**
  1. Build a decoupled event subsystem using Hexagonal Architecture: an `EventPublisher` output port and a default `DatabaseEventPublisher`.
  2. Use `@ConditionalOnMissingBean` + `@ConditionalOnProperty` to allow future drop-in Kafka or SQS streaming publishers.
  3. Annotate `EventServiceImpl` recording methods with `Propagation.REQUIRES_NEW` so event logs persist independently of parent transaction rollbacks.
* **Consequences:** Permanent non-repudiation audit trail; failure reasons are never lost to transaction rollbacks; zero code changes required to extract the audit system into a shared microservice/library.

---

## ADR-006: Enterprise Party Model (BIAN Standard) over Flat Users Table
* **Status:** Accepted
* **Context:** In real-world banking and FinTech, accounts are owned not only by retail individuals, but also by commercial businesses (merchants) and sovereign government tax authorities (e.g. GST/TDS nodal agencies). Storing all of these in a single flat `users` table results in 60% of database columns being `NULL` (since individuals don't have GSTIN/CIN, and businesses don't have personal DOBs).
* **Decision:** Adopt the international **Banking Industry Architecture Network (BIAN) Party Model**:
  1. Create a central polymorphic `parties` table (`INDIVIDUAL`, `BUSINESS`, `GOVERNMENT`, `PLATFORM`).
  2. Create specialized 1-to-1 profile tables sharing the primary key (`individual_profiles`, `business_profiles`, `government_profiles`).
  3. Refactor `Account` to point to `party_id` instead of `user_id`.
  4. Implement an `AccountPolicyEngine` via the Strategy Pattern to enforce real-world account limits (e.g. individuals cannot open `EQUITY` or `REVENUE` accounts; government agencies only hold tax collection buckets).
* **Consequences:** True enterprise domain modeling, clean 3NF database normalization with zero null-column pollution, natural multi-party transfer support (User $\rightarrow$ Merchant + Platform Fee + Govt GST), and modular account allocation policies.

---

## ADR-007: Cross-Border & FEMA Residency Compliance Modeling
* **Status:** Accepted
* **Context:** Under Reserve Bank of India (RBI) and Foreign Exchange Management Act (FEMA) guidelines, financial institutions cannot treat domestic residents and Non-Resident Indians (NRIs) identically. Foreign corporations operating in India also possess different statutory identifiers than Indian registered entities.
* **Decision:**
  1. For `IndividualProfile`: Introduce `ResidentialStatus` (`RESIDENT_INDIAN`, `NON_RESIDENT_INDIAN`, `FOREIGN_NATIONAL`) and `countryOfResidence` (ISO-2 code). If an individual is an NRI, country of residence cannot be `IN`.
  2. For `BusinessProfile`: Introduce `countryOfIncorporation` (ISO-2 code) and `foreignRegistrationNumber`. For domestic Indian companies (`IN`), Indian `gstin` and `corporatePan` are strictly enforced. For foreign companies (`!IN`), `foreignRegistrationNumber` is mandatory while domestic Indian CIN is not expected.
  3. Government bodies are strictly domestic statutory agencies.
* **Consequences:** Full compliance readiness with RBI/FEMA cross-border regulatory frameworks and realistic international B2B commerce enablement.

---

## ADR-008: Transient Database Contention & Throttling Resilience (Exponential Backoff + Jitter)
* **Status:** Accepted
* **Context:** High-frequency transfers to hot accounts (e.g. high-volume merchant wallets) or managed cloud database rate limits (HTTP 429, connection pool saturation, lock acquisition timeouts) cause transient database errors (`PessimisticLockingFailureException`, `CannotAcquireLockException`, `TransientDataAccessException`). Failing immediately causes unnecessary transaction drops and degrades customer experience.
* **Decision:**
  1. Introduce **Spring Retry** with declarative `@Retryable` wrapping the transfer execution boundary.
  2. Explicitly separate retryable transient infrastructure faults (`TransientDataAccessException`, `ConcurrencyFailureException`, `PessimisticLockingFailureException`) from non-retryable business validation rejections (`InsufficientBalanceException`, `CurrencyMismatchException`, `SameAccountTransferException`, `DuplicateIdempotencyKeyException`).
  3. Use exponential backoff with randomized jitter (initial 100ms, multiplier 2.0, max 1000ms) to prevent thundering herd collisions across concurrent worker threads.
  4. Ensure `@Retryable` wraps outside `@Transactional` so every retry attempt executes within a completely fresh database transaction context (avoiding PostgreSQL aborted transaction errors).
  5. Provide an `@Recover` fallback that gracefully records a `TRANSFER_FAILED` audit log and throws a standard HTTP 503 `ServiceUnavailableException`.
* **Consequences:** Massive boost in transaction throughput and resilience under burst traffic; zero spurious failures on temporary lock contention; strict isolation between infrastructure retries and business logic.

---

## ADR-009: PII At-Rest Encryption & HMAC Blind Indexing
* **Status:** Accepted
* **Context:** Financial customer data (PAN, Email, Phone number) must comply with DPDP Act 2023 (Digital Personal Data Protection Act) and RBI cybersecurity directives. Plaintext storage exposes sensitive identity credentials if database backups or read-replicas are compromised. Furthermore, randomized encryption (AES-GCM) creates non-deterministic ciphertexts, preventing database-level `UNIQUE` constraints and exact-match `WHERE` queries.
* **Decision:**
  1. **AES-256-GCM Envelope Encryption:** Encrypt sensitive fields (`panNumber`, `email`, `phoneNumber`, `corporatePan`) using AES-256-GCM with a secure 12-byte random IV per record and a 128-bit authentication tag.
  2. **HMAC-SHA256 Blind Indexing:** Compute a deterministic HMAC-SHA256 digest with an isolated secret pepper key over normalized PII (`pan_hash`, `corporate_pan_hash`). Place database `UNIQUE` indexes on the blind index columns.
  3. **Zero Plaintext In Logs:** Implement `PiiMasker` to enforce strict masking (`XXXXX1234F`, `r***a@bank.com`) across all log statements and exception messages.
* **Consequences:** Full regulatory compliance and zero plaintext risk at rest, while maintaining $O(1)$ fast indexed searches and database-enforced deduplication without decrypting database rows.

---

## ADR-010: Streaming File Ingestion Pipeline & Multi-Tier Deduplication
* **Status:** Accepted
* **Context:** External clearinghouses, bank feeds, and settlement networks (Stripe, clearing partners) deliver transaction posting files containing thousands to hundreds of thousands of lines. Loading entire files into memory causes `OutOfMemoryError` (OOM), HTTP connection timeouts, and duplicate processing risks if networks retry file transmissions.
* **Decision:**
  1. **Cryptographic Checksum Deduplication (Layer 1):** Compute SHA-256 hash across the incoming byte stream and record in `file_ingestion_jobs` with a database `UNIQUE` constraint. Duplicate file uploads are rejected immediately without parsing.
  2. **Asynchronous Non-Blocking Processing:** Accept files with HTTP `202 Accepted` returning a `jobId`, delegating row processing to worker threads with chunked database transactions (e.g. 500 records per batch).
  3. **External Account Mapping with Cache:** Resolve external system account IDs to OmniLedger account UUIDs via `external_account_mappings` backed by high-throughput lookups.
  4. **Row-Level Idempotency (Layer 2):** Set Journal Entry idempotency key as `sourceSystem + ":" + externalReferenceId` to prevent double-crediting if an altered file contains repeated rows.
  5. **Quarantine / Dead-Letter Isolation:** Malformed, unbalanced, or unresolvable rows are written to `file_ingestion_rejections` without aborting valid rows in the file batch.
* **Consequences:** O(1) constant memory streaming footprint, resilient duplicate upload prevention, fault-tolerant batch settlement, and comprehensive auditability for finance operations.
