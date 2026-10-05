# 🗄️ OmniLedger: Database Entity Specifications

## 🏛️ 1. Party & Identity Subsystem (Enterprise Party Archetype)

### 📌 Why We Adopted the Party Model over a Simple Flat Users Table:
A standard flat `users` table treats individual retail customers, commercial merchants, and sovereign tax authorities as the same concept. In real-world core banking and FinTech:
1. **Tax Split & Settlement:** When a user pays a merchant, GST/tax must be credited to a government tax authority account (`LIABILITY` owed to the state). A government agency is not an individual user.
2. **Database Normalization (Zero Nullable Bloat):** Individuals require personal PAN and DOB, while businesses require GSTIN, CIN, and corporate registration. Shoving these into one table leaves 60% of columns `NULL`. The Party Model provides clean Third Normal Form (3NF) isolation.
3. **Pluggable Account Policies:** Decoupling legal party types allows enforcing strict real-world account allocation limits (e.g. individuals cannot open `EQUITY` or `REVENUE` accounts; government agencies only hold tax collection buckets).
4. **Data Protection & PII Isolation:** Statutory identifiers (PAN) and contact details (email, phone) are stored with AES-256-GCM authenticated encryption at rest and HMAC-SHA256 blind indexing for queryability.

---

### 🏛️ 1.1 Parties Table (`parties`)
Acts as the central polymorphic identity registry for any actor interacting with the ledger.
* `party_id` (`UUID` / Primary Key) - Unique identifier for the legal or operational entity.
* `party_type` (`VARCHAR(20)`) - Classification (`INDIVIDUAL`, `BUSINESS`, `GOVERNMENT`, `PLATFORM`).
* `status` (`VARCHAR(20)`) - Operational status (`ACTIVE`, `SUSPENDED`, `CLOSED`).
* `created_at` (`TIMESTAMP WITH TIME ZONE`) - Profile creation timestamp.
* `updated_at` (`TIMESTAMP WITH TIME ZONE`) - Last modification timestamp.

### 👤 1.2 Individual Profiles Table (`individual_profiles`)
Stores personal KYC and contact details for retail natural persons.
* `party_id` (`UUID` / PK & FK) - References `parties(party_id)` via shared primary key.
* `full_name` (`VARCHAR(100)`) - Legal full name.
* `pan_number` (`VARCHAR(255)`) - AES-256-GCM encrypted individual tax identifier (PAN).
* `pan_hash` (`VARCHAR(64)` / Unique Index) - Deterministic HMAC-SHA256 blind index for unique constraints and lookups.
* `email` (`VARCHAR(255)`) - AES-256-GCM encrypted contact and notification email.
* `email_hash` (`VARCHAR(64)`) - Blind index for email lookups.
* `phone_number` (`VARCHAR(255)`) - AES-256-GCM encrypted mobile number.
* `date_of_birth` (`DATE`) - Date of birth for age verification and compliance.
* `residential_status` (`VARCHAR(30)`) - FEMA status (`RESIDENT_INDIAN`, `NON_RESIDENT_INDIAN`, `FOREIGN_NATIONAL`).
* `country_of_residence` (`VARCHAR(2)`) - ISO-2 country code.

### 🏢 1.3 Business Profiles Table (`business_profiles`)
Stores corporate registration and tax credentials for merchant/enterprise entities.
* `party_id` (`UUID` / PK & FK) - References `parties(party_id)`.
* `legal_business_name` (`VARCHAR(150)`) - Officially registered corporate name.
* `trade_name` (`VARCHAR(150)`) - Commercial/operating brand name.
* `gstin` (`VARCHAR(15)` / Unique Constraint) - Goods and Services Tax Identification Number.
* `cin_number` (`VARCHAR(21)` / Unique Constraint) - Corporate Identification Number (MCA).
* `corporate_pan` (`VARCHAR(255)`) - AES-256-GCM encrypted company PAN.
* `corporate_pan_hash` (`VARCHAR(64)` / Unique Index) - Deterministic HMAC-SHA256 blind index for company PAN.
* `email` (`VARCHAR(255)`) - AES-256-GCM encrypted official accounts/billing email.
* `phone_number` (`VARCHAR(255)`) - AES-256-GCM encrypted corporate contact desk number.
* `country_of_incorporation` (`VARCHAR(2)`) - ISO-2 country of incorporation.
* `foreign_registration_number` (`VARCHAR(64)`) - Cross-border registry ID for foreign entities.

### 🏛️ 1.4 Government Profiles Table (`government_profiles`)
Represents sovereign tax agencies and municipal bodies for automated tax/levy collection.
* `party_id` (`UUID` / PK & FK) - References `parties(party_id)`.
* `department_name` (`VARCHAR(150)`) - Department name (e.g., "Central Board of Indirect Taxes and Customs").
* `jurisdiction` (`VARCHAR(50)`) - Sovereign scope (e.g., `CENTRAL`, `STATE_MAHARASHTRA`).
* `nodal_agency_code` (`VARCHAR(50)` / Unique) - Treasury nodal identifier.
* `tax_category_code` (`VARCHAR(20)`) - Category of tax collected (e.g., `GST`, `TDS`, `STAMP_DUTY`).

---

## 🏦 2. Accounts Table (`accounts`)
Represents individual financial balance buckets owned by any Party (individual, business, government, or platform).
* `account_id` (`UUID` / Primary Key) - Unique identifier for the financial account.
* `party_id` (`UUID` / Foreign Key) - References `parties(party_id)`.\n* `account_type` (`VARCHAR(20)`) - Classification (`ASSET`, `LIABILITY`, `EQUITY`, `REVENUE`, `EXPENSE`).
* `balance` (`NUMERIC(19, 4)`) - High-precision monetary balance (mapped to Java `BigDecimal`).
* `currency` (`VARCHAR(10)`) - Currency code (`INR`, `USD`, `EUR`, `GBP`).
* `is_sharded` (`BOOLEAN`) - Hot-account optimization flag for high-throughput accounts.
* `parent_account_id` (`UUID` / Nullable) - References parent account if this is a sub-shard.

---

## 🧾 3. Journal Entries Table (`journal_entries`)
Acts as the immutable header and receipt for a financial transaction event.
* `journal_entry_id` (`UUID` / Primary Key) - Unique identifier for the transaction event.
* `idempotency_key` (`VARCHAR(128)` / Unique Constraint) - Ensures safe retries by preventing duplicate executions.
* `currency` (`VARCHAR(10)`) - Base transaction currency.
* `description` (`TEXT`) - Human-readable context for the transaction.
* `created_at` (`TIMESTAMP WITH TIME ZONE`) - Transaction execution timestamp (immutable).

---

## 📊 4. Posting Lines Table (`posting_lines`)
Stores the individual debit and credit legs linked to a journal entry, enforcing the double-entry rule ($\sum \text{Debits} = \sum \text{Credits}$).
* `posting_id` (`UUID` / Primary Key) - Unique identifier for the line item.
* `journal_entry_id` (`UUID` / Foreign Key) - References `journal_entries(journal_entry_id)`.\n* `account_id` (`UUID` / Foreign Key) - References `accounts(account_id)`.\n* `amount` (`NUMERIC(19, 4)`) - High-precision monetary value (mapped to Java `BigDecimal`).
* `direction` (`VARCHAR(10)`) - Direction of money flow (`DEBIT` or `CREDIT`).

---

## 📡 5. System Events Table (`system_events`)
Provides an append-only, immutable audit log tracking all operational and lifecycle actions across the ledger.
* `event_id` (`UUID` / Primary Key) - Unique identifier for the audit event.
* `event_type` (`VARCHAR(50)`) - Event category (`ACCOUNT_CREATED`, `TRANSFER_INITIATED`, etc.).
* `user_id` (`UUID` / Nullable) - Correlated user/party ID.
* `account_id` (`UUID` / Nullable) - Correlated account ID.
* `journal_entry_id` (`UUID` / Nullable) - Correlated journal entry ID.
* `idempotency_key` (`VARCHAR(128)` / Nullable) - Client idempotency key for correlation.
* `payload` (`TEXT`) - Event snapshot metadata.
* `status` (`VARCHAR(20)`) - Outcome status (`SUCCESS`, `FAILED`, `PENDING`).
* `error_message` (`TEXT` / Nullable) - Root cause details for failed operations.
* `created_at` (`TIMESTAMP WITH TIME ZONE`) - Exact UTC timestamp when event occurred (indexed).

---

## 📂 6. File Ingestion Subsystem (Reconciliation & Batch Settlement)

Provides an asynchronous, streaming pipeline for ingesting high-volume external transaction files (settlement feeds, clearinghouse reports, bank statements).

### 📑 6.1 File Ingestion Jobs Table (`file_ingestion_jobs`)
Tracks overall lifecycle, idempotency checksum, row counts, and progress of an uploaded ledger file.
* `job_id` (`UUID` / Primary Key) - Unique identifier for the ingestion job.
* `file_name` (`VARCHAR(255)`) - Original upload file name.
* `file_checksum_sha256` (`VARCHAR(64)` / Unique Constraint) - Cryptographic byte checksum preventing duplicate uploads of the identical file.
* `source_system` (`VARCHAR(64)`) - Partner or network identifier (e.g. `STRIPE`, `HDFC_BANK`, `CLEARINGHOUSE`).
* `file_size_bytes` (`BIGINT`) - Size of the file in bytes.
* `total_records` (`INT`) - Total records/lines found in the file.
* `processed_records` (`INT`) - Successfully parsed and posted records.
* `failed_records` (`INT`) - Records that failed validation and were quarantined.
* `status` (`VARCHAR(32)`) - Job lifecycle status (`PENDING`, `PROCESSING`, `COMPLETED`, `PARTIALLY_FAILED`, `FAILED`).
* `error_message` (`TEXT` / Nullable) - Top-level job failure details if aborted.
* `created_at` (`TIMESTAMP WITH TIME ZONE`) - Job submission timestamp.
* `updated_at` (`TIMESTAMP WITH TIME ZONE`) - Last progress heartbeat timestamp.

### 🔗 6.2 External Account Mappings Table (`external_account_mappings`)
Translates partner/external account identifiers into internal OmniLedger Account UUIDs.
* `mapping_id` (`UUID` / Primary Key) - Unique identifier for the mapping.
* `external_system` (`VARCHAR(64)`) - External partner network (e.g. `STRIPE`).
* `external_account_id` (`VARCHAR(128)`) - Partner account number or IBAN.
* `account_id` (`UUID` / Foreign Key) - References internal `accounts(account_id)`.
* `created_at` (`TIMESTAMP WITH TIME ZONE`) - Mapping creation timestamp.
* *Constraint:* `UNIQUE(external_system, external_account_id)`.

### 🚨 6.3 File Ingestion Rejections Table (`file_ingestion_rejections`)
Quarantines invalid, malformed, or unresolvable lines for finance audit and manual reconciliation without failing the whole batch.
* `rejection_id` (`UUID` / Primary Key) - Unique identifier for the rejected line item.
* `job_id` (`UUID` / Foreign Key) - References `file_ingestion_jobs(job_id)`.
* `row_number` (`INT`) - 1-based row index in the source file.
* `raw_record` (`TEXT`) - Exact raw line content from the file.
* `rejection_code` (`VARCHAR(64)`) - Standardized enum code (`MALFORMED_RECORD`, `UNKNOWN_ACCOUNT`, `UNBALANCED_ENTRY`, `INVALID_AMOUNT`, `INVALID_CURRENCY`, `CURRENCY_MISMATCH`, `DUPLICATE_REFERENCE`, `INSUFFICIENT_BALANCE`).
* `reason` (`TEXT`) - Human-readable explanation of why the line was quarantined.
* `created_at` (`TIMESTAMP WITH TIME ZONE`) - Rejection timestamp.
