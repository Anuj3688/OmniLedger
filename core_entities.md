# 🗄️ OmniLedger: Database Entity Specifications

## 👥 1. Users Table
Stores core customer identity details.
* `user_id` (`UUID` / Primary Key) - Unique identifier for the user.
* `pan_number` (`VARCHAR`) - Legal tax/identity identifier.
* `created_at` (`TIMESTAMP WITH TIME ZONE`) - Profile creation timestamp.

## 🏦 2. Accounts Table
Represents individual financial buckets (wallets, savings, fee pools) owned by a user.
* `account_id` (`UUID` / Primary Key) - Unique identifier for the financial account.
* `user_id` (`UUID` / Foreign Key) - References `users(user_id)`.
* `balance` (`NUMERIC(19, 4)`) - High-precision monetary balance.
* `account_type` (`VARCHAR`) - Classification (`ASSET`, `LIABILITY`, `EQUITY`, `REVENUE`, `EXPENSE`).
* `currency` (`VARCHAR(3)`) - ISO currency code (e.g., `INR`, `USD`).

## 🧾 3. Journal Entries Table
Acts as the immutable header and receipt for a financial transaction event.
* `journal_entry_id` (`UUID` / Primary Key) - Unique identifier for the transaction event.
* `idempotency_key` (`VARCHAR` / Unique Constraint) - Ensures safe retries by preventing duplicate executions.
* `description` (`TEXT`) - Human-readable context for the transaction.
* `created_at` (`TIMESTAMP WITH TIME ZONE`) - Transaction execution timestamp.

## 📊 4. Posting Lines Table
Stores the individual debit and credit legs linked to a journal entry, enforcing the double-entry rule ($\sum \text{Debits} = \sum \text{Credits}$).
* `posting_id` (`UUID` / Primary Key) - Unique identifier for the line item.
* `journal_entry_id` (`UUID` / Foreign Key) - References `journal_entries(journal_entry_id)`.
* `account_id` (`UUID` / Foreign Key) - References `accounts(account_id)`.
* `amount` (`NUMERIC(19, 4)`) - High-precision monetary value (mapped to Java `BigDecimal`).
* `direction` (`ENUM` / `VARCHAR(10)`) - Direction of money flow (`DEBIT` or `CREDIT`).