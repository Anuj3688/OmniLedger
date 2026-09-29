# 🗄️ OmniLedger: Database Entity Specifications

## 👥 1. Users Table
Stores core customer identity details.
* `user_id` (UUID / PK) - Unique identifier for the user.
* `pan_number` (VARCHAR) - Legal tax/identity identifier.
* `created_at` (TIMESTAMP) - Profile creation timestamp.

## 🏦 2. Accounts Table
Represents individual financial buckets (wallets, savings, fee pools) owned by a user.
* `account_id` (UUID / PK) - Unique identifier for the financial account.
* `user_id` (UUID / FK) - References `users(user_id)`.
* `account_type` (VARCHAR) - Classification (`ASSET`, `LIABILITY`, `EQUITY`, `REVENUE`, `EXPENSE`).
* `currency` (VARCHAR) - ISO currency code (e.g., `INR`, `USD`).

## 🧾 3. Journal Entries Table
Acts as the immutable header or receipt for a financial transaction event.
* `journal_entry_id` (UUID / PK) - Unique identifier for the transaction event.
* `idempotency_key` (VARCHAR / UNIQUE) - Ensures safe retries by blocking duplicate requests.
* `description` (TEXT) - Human-readable context for the transaction.
* `created_at` (TIMESTAMP) - Transaction execution timestamp.

## 📊 4. Posting Lines Table
Stores the individual debit and credit legs linked to a journal entry, enforcing the double-entry rule (\(\sum \text{Debits} = \sum \text{Credits}\)).
* `posting_id` (UUID / PK) - Unique identifier for the line item.
* `journal_entry_id` (UUID / FK) - References `journal_entries(journal_entry_id)`.
* `account_id` (UUID / FK) - References `accounts(account_id)`.
* `amount` (NUMERIC) - High-precision monetary value (mapped to Java `BigDecimal`).
* `direction` (ENUM) - Direction of money flow (`DEBIT` or `CREDIT`).