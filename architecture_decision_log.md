# 📜 OmniLedger: Architecture Decision Log (ADR)

This document records all significant architectural, domain, and design decisions made during the evolution of OmniLedger. Each entry details the **Context**, the **Decision**, the **Alternatives Considered**, and the **Consequences & Benefits**.

---

## 📑 Index of Architecture Decisions

* [ADR-001: Strict Double-Entry Ledger Pattern & Immutability](#adr-001-strict-double-entry-ledger-pattern--immutability)
* [ADR-002: Deterministic Lock Acquisition for Deadlock Prevention](#adr-002-deterministic-lock-acquisition-for-deadlock-prevention)
* [ADR-003: High-Precision Monetary Math with BigDecimal and PostgreSQL NUMERIC(19,4)](#adr-003-high-precision-monetary-math-with-bigdecimal-and-postgresql-numeric194)
* [ADR-004: Type-Safe Currency Enum with Domestic Default (INR)](#adr-004-type-safe-currency-enum-with-domestic-default-inr)
* [ADR-005: Hexagonal System Event & Audit Subsystem with Isolated Transactions](#adr-005-hexagonal-system-event--audit-subsystem-with-isolated-transactions)
* [ADR-006: Strategy-Based Plug-and-Play Profile Modification Engine](#adr-006-strategy-based-plug-and-play-profile-modification-engine)
* [ADR-007: Enterprise Party Model (BIAN Standard) over Flat Users Table](#adr-007-enterprise-party-model-bian-standard-over-flat-users-table)
* [ADR-008: Cross-Border & FEMA Residency Compliance Modeling](#adr-008-cross-border--fema-residency-compliance-modeling)

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
* **Decision:** Introduce a strongly typed `Currency` enum (`INR`, `USD`, `EUR`, `GBP`) located in `dev.fintech.omniledger.model.enums`, defaulting to `Currency.INR`.
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

## ADR-006: Strategy-Based Plug-and-Play Profile Modification Engine
* **Status:** Accepted
* **Context:** User/Party profiles require partial updates (e.g. name, email, phone number). Hardcoding `if-else` or `switch` chains creates brittle code, violates the Single Responsibility Principle, and requires editing service classes every time a new profile field is added.
* **Decision:** Implement the **Strategy Pattern** with an injected `UserUpdateDispatcher`.
  * Each field update lives in a dedicated strategy (`FullNameUpdateStrategy`, `EmailUpdateStrategy`, `PhoneNumberUpdateStrategy`).
  * Each strategy implements both strict domain `validate(newValue)` and `apply(user, newValue)`.
  * The dispatcher collects all strategy beans via Spring DI into an immutable map and dispatches modification items in a loop with **zero `if-else` statements**.
* **Consequences:** Fully satisfies the Open/Closed Principle (OCP). Adding a future field (e.g. `ADDRESS`) only requires creating one new `@Component` strategy class.

---

## ADR-007: Enterprise Party Model (BIAN Standard) over Flat Users Table
* **Status:** Accepted
* **Context:** In real-world banking and FinTech, accounts are owned not only by retail individuals, but also by commercial businesses (merchants) and sovereign government tax authorities (e.g. GST/TDS nodal agencies). Storing all of these in a single flat `users` table results in 60% of database columns being `NULL` (since individuals don't have GSTIN/CIN, and businesses don't have personal DOBs).
* **Decision:** Adopt the international **Banking Industry Architecture Network (BIAN) Party Model**:
  1. Create a central polymorphic `parties` table (`INDIVIDUAL`, `BUSINESS`, `GOVERNMENT`, `PLATFORM`).
  2. Create specialized 1-to-1 profile tables sharing the primary key (`individual_profiles`, `business_profiles`, `government_profiles`).
  3. Refactor `Account` to point to `party_id` instead of `user_id`.
  4. Implement an `AccountPolicyEngine` via the Strategy Pattern to enforce real-world account limits (e.g. individuals cannot open `EQUITY` or `REVENUE` accounts; government agencies only hold tax collection buckets).
* **Consequences:** True enterprise domain modeling, clean 3NF database normalization with zero null-column pollution, natural multi-party transfer support (User $\rightarrow$ Merchant + Platform Fee + Govt GST), and modular account allocation policies.

---

## ADR-008: Cross-Border & FEMA Residency Compliance Modeling
* **Status:** Accepted
* **Context:** Under Reserve Bank of India (RBI) and Foreign Exchange Management Act (FEMA) guidelines, financial institutions cannot treat domestic residents and Non-Resident Indians (NRIs) identically. Foreign corporations operating in India also possess different statutory identifiers than Indian registered entities.
* **Decision:**
  1. For `IndividualProfile`: Introduce `ResidentialStatus` (`RESIDENT_INDIAN`, `NON_RESIDENT_INDIAN`, `FOREIGN_NATIONAL`) and `countryOfResidence` (ISO-2 code). If an individual is an NRI, country of residence cannot be `IN`.
  2. For `BusinessProfile`: Introduce `countryOfIncorporation` (ISO-2 code) and `foreignRegistrationNumber`. For domestic Indian companies (`IN`), Indian `gstin` and `corporatePan` are strictly enforced. For foreign companies (`!IN`), `foreignRegistrationNumber` is mandatory while domestic Indian CIN is not expected.
  3. Government bodies are strictly domestic statutory agencies.
* **Consequences:** Full compliance readiness with RBI/FEMA cross-border regulatory frameworks and realistic international B2B commerce enablement.
