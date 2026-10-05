# 📋 OmniLedger: Future Roadmap & TODO Backlog

This document tracks future architectural enhancements, enterprise capabilities, and optimizations planned for subsequent iterations after the core financial ledger engine is completed and fully understood.

---

## 🚀 Active & Backlog Items

### 1. 📂 Third-Party Ledger File Ingestion Pipeline
* **Domain Context:** Ingesting external settlement, clearinghouse (NACHA/NEFT/RTGS), or bank statement files with zero-memory OOM streaming and strict deduplication.
* **Pattern 1: Paired Transactions File (Completed ✅):**
  - [x] Schema & Tables (`file_ingestion_jobs`, `external_account_mappings`, `file_ingestion_rejections`).
  - [x] JPA Entities & Spring Data Repositories.
  - [x] Streaming File Ingestion Service with SHA-256 byte checksum deduplication (Layer 1 Deduplication).
  - [x] External Account ID $\rightarrow$ OmniLedger Account resolution with thread-safe `ConcurrentHashMap` caching.
  - [x] Deadlock-free sorted pessimistic account locks (`findByIdWithLock`).
  - [x] Isolated Transaction Posting Processor (`REQUIRES_NEW`) ensuring valid batches commit while invalid batches rollback cleanly.
  - [x] Quarantine / Dead-letter handling for invalid lines (`file_ingestion_rejections`) with strongly-typed `RejectionCode` enum without failing valid rows.
  - [x] REST API Endpoints (`POST /api/v1/ledger/files/ingest`, `GET /api/v1/ledger/files/jobs/{jobId}`, `GET /api/v1/ledger/files/jobs/{jobId}/rejections`, `POST /api/v1/ledger/account-mappings`, `GET /api/v1/ledger/account-mappings/{system}/{accountId}`).
  - [x] End-to-end integration and HTTP API test coverage (`FileIngestionIntegrationTest`, `FileIngestionControllerIntegrationTest`).
* **Pattern 2: Single-Leg Bank Statement File (Queued):**
  - Auto-pairing single-leg bank posting lines against an internal Clearing/Settlement transit suspense account.

---

### 2. 🔄 Multi-Attribute Profile Modification Engine (Strategy Pattern)
* **Domain Context:** Once parties (Individuals and Businesses) are onboarded, customers need to update contact and operational credentials (e.g., email, mobile number, trade name, billing address).
* **Planned Design:**
  * Implement the Strategy Pattern with zero `if-else` branching using a Spring-managed registry dispatcher (`IndividualProfileUpdateDispatcher`).
  * Dedicated strategy classes per field (`FullNameUpdateStrategy`, `EmailUpdateStrategy`, `PhoneNumberUpdateStrategy`) with isolated validation rules and duplicate checks.
  * Atomic multi-field modifications in a single `PATCH /api/v1/parties/individuals/{partyId}` request.
  * Audit logging of all before/after profile attribute changes via `SystemEvent`.

---

### 3. 🌐 Multi-Currency FX Conversion & Settlement Subsystem
* **Domain Context:** Support cross-currency transfers (e.g., source account in `INR`, destination in `USD`).
* **Planned Design:**
  * Fixed exchange rate provider SPI (`ExchangeRateProvider`).
  * Two-legged transfer with an intermediary platform FX suspense account.

---

### 4. 🛡️ Daily & Transactional Velocity Limits (Risk Engine)
* **Domain Context:** Prevent fraud and comply with regulatory limits (e.g., RBI UPI limit of ₹1,00,000/day for retail individual wallets).
* **Planned Design:**
  * Rule-based velocity checks before locking accounts.
  * Tiered limits based on KYC completeness and PartyType.

---

### 5. 📬 Distributed Event Streaming (Kafka / AWS SQS Adapter)
* **Domain Context:** As transaction volume scales, stream audit events to external data lakes and compliance monitoring systems.
* **Planned Design:**
  * Implement `KafkaEventPublisher` implementing `EventPublisher` SPI.
  * Activate via `@ConditionalOnProperty(name = "omniledger.events.publisher.type", havingValue = "kafka")`.

---

### 6. 📑 Periodic Account Statement & Trial Balance Generation
* **Domain Context:** End-of-day / monthly statement generation for auditing and customer downloads.
* **Planned Design:**
  * Snapshot balance at given timestamps using ledger journal entries.
  * Generate balance-sheet proof (Total Debits == Total Credits across the entire system).
