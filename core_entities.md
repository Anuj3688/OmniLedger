# 🗄️ OmniLedger: Database Entity Specifications

## 🏛️ 1. Party & Identity Subsystem (Enterprise Party Archetype)

### 📌 Why We Adopted the Party Model over a Simple Flat Users Table:
A standard flat `users` table treats individual retail customers, commercial merchants, and sovereign tax authorities as the same concept. In real-world core banking and FinTech:
1. **Tax Split & Settlement:** When a user pays a merchant, GST/tax must be credited to a government tax authority account (`LIABILITY` owed to the state). A government agency is not an individual user.
2. **Database Normalization (Zero Nullable Bloat):** Individuals require personal PAN and DOB, while businesses require GSTIN, CIN, and corporate registration. Shoving these into one table leaves 60% of columns `NULL`. The Party Model provides clean Third Normal Form (3NF) isolation.
3. **Pluggable Account Policies:** Decoupling legal party types allows enforcing strict real-world account allocation limits (e.g. individuals cannot hold `EQUITY` or `REVENUE` accounts; government agencies only hold tax collection buckets).

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
* `pan_number` (`VARCHAR(10)` / Unique Constraint) - Individual tax identifier (PAN).
* `email` (`VARCHAR(120)` / Unique Constraint) - Contact and notification email.
* `phone_number` (`VARCHAR(15)`) - Mobile number for SMS/OTP alerts.
* `date_of_birth` (`DATE`) - Date of birth for age verification and compliance.

### 🏢 1.3 Business Profiles Table (`business_profiles`)
Stores corporate registration and tax credentials for merchant/enterprise entities.
* `party_id` (`UUID` / PK & FK) - References `parties(party_id)`.
* `legal_business_name` (`VARCHAR(150)`) - Officially registered corporate name.
* `trade_name` (`VARCHAR(150)`) - Commercial/operating brand name.
* `gstin` (`VARCHAR(15)` / Unique Constraint) - Goods and Services Tax Identification Number.
* `cin_number` (`VARCHAR(21)` / Unique Constraint) - Corporate Identification Number (MCA).
* `corporate_pan` (`VARCHAR(10)` / Unique Constraint) - Company PAN.
* `email` (`VARCHAR(120)`) - Official accounts/billing email.
* `phone_number` (`VARCHAR(15)`) - Corporate contact desk number.

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
* `party_id` (`UUID` / Foreign Key) - References `parties(party_id)`.
* `account_type` (`VARCHAR(20)`) - Classification (`ASSET`, `LIABILITY`, `EQUITY`, `REVENUE`, `EXPENSE`).
* `balance` (`NUMERIC(19, 4)`) - High-precision monetary balance (mapped to Java `BigDecimal`).
* `currency` (`VARCHAR(10)`) - Currency code (`INR`, `USD`, `EUR`, `GBP`).

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
* `journal_entry_id` (`UUID` / Foreign Key) - References `journal_entries(journal_entry_id)`.
* `account_id` (`UUID` / Foreign Key) - References `accounts(account_id)`.
* `amount` (`NUMERIC(19, 4)`) - High-precision monetary value (mapped to Java `BigDecimal`).
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
