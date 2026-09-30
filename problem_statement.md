# 🏦 OmniLedger: Core Banking & Transactional Engine

## 📋 Overview
Design and implement a transactional financial ledger backend service that handles immutable double-entry bookkeeping, multi-currency accounts, and strict concurrency safety.

## 🎯 What is In Scope
* **Core Ledger Engine:** Implementation of an append-only double-entry bookkeeping model (`JournalEntry` and `PostingLine` items) where $\sum \text{Debits} = \sum \text{Credits}$.
* **Concurrency & Safety:** Pessimistic row-level locking (`SELECT ... FOR UPDATE`) to prevent race conditions during high-frequency balance updates.
* **Idempotency Protection:** Enforcing unique `idempotency_key` constraints at the database and service layer to safely handle duplicate transaction retries.
* **Precision Handling:** Using strict arbitrary-precision numeric types (`BigDecimal` in Java and PostgreSQL `NUMERIC(19, 4)`) to completely eliminate floating-point rounding errors.
* **Automated Testing:** Unit tests for invariants and multi-threaded integration tests using thread pools to prove concurrency safety.

## 🚫 What is Out of Scope
* **User Interface / Frontend:** No web or mobile UI; this is strictly a backend REST API service.
* **External Payment Gateway Integration:** Real-world payment rails are simulated via internal mocks/webhooks rather than live API calls.
* **Distributed Consensus / Two-Phase Commit (2PC):** Cross-service distributed transactions (Sagas) are omitted to keep the focus purely on single-database relational transactional rigor.
* **Advanced Multi-Region Replication:** Complex database sharding, geo-replication, and active-active multi-datacenter setups.

## 🔌 Core APIs
* `POST /api/v1/accounts` 🏢 - Create a new financial account.
* `GET /api/v1/accounts/{id}` 🔍 - Retrieve account details and current balance.
* `POST /api/v1/transfers` 💸 - Execute a double-entry financial transfer (requires `idempotencyKey`).
* `GET /api/v1/transfers/{id}` 📜 - Audit an immutable journal entry and its posting lines.

## 🧪 Testing & Validation Strategy
* **Unit Testing 🔬:** Verify double-entry balance validation rules and constraint handling.
* **Concurrency Testing 🚦:** Use multi-threaded thread pools (`ExecutorService`) to simulate concurrent transfers and verify zero negative balances.
* **Idempotency Testing 🔄:** Ensure duplicate requests with the same key return cached responses without duplicate entries.

## 📏 The Basic Benchmark
1. **Mathematical Invariant:** Zero unbalanced ledger entries can ever be persisted.
2. **Concurrency Resistance:** 100 concurrent threads modifying the same account result in zero balance corruption.
3. **Idempotent Retries:** Duplicate requests with matching idempotency keys process exactly once.

## 🔍 Evaluation Criteria
* **Code Quality:** Clean separation of concerns (Controller $\rightarrow$ Service $\rightarrow$ Repository).
* **Database Rigor:** Proper use of PostgreSQL constraints, foreign keys, and unique indexes.
* **Stress Testing:** Automated proof of race-condition safety via test suites.