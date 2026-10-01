# 📋 OmniLedger: Future Roadmap & TODO Backlog

This document tracks future architectural enhancements, enterprise capabilities, and optimizations planned for subsequent iterations after the core financial ledger engine is completed and fully understood.

---

## 🚀 Backlog Items

### 1. 🔄 Multi-Attribute Profile Modification Engine (Strategy Pattern)
* **Domain Context:** Once parties (Individuals and Businesses) are onboarded, customers need to update contact and operational credentials (e.g., email, mobile number, trade name, billing address).
* **Planned Design:**
  * Implement the Strategy Pattern with zero `if-else` branching using a Spring-managed registry dispatcher (`IndividualProfileUpdateDispatcher`).
  * Dedicated strategy classes per field (`FullNameUpdateStrategy`, `EmailUpdateStrategy`, `PhoneNumberUpdateStrategy`) with isolated validation rules and duplicate checks.
  * Atomic multi-field modifications in a single `PATCH /api/v1/parties/individuals/{partyId}` request.
  * Audit logging of all before/after profile attribute changes via `SystemEvent`.

---

### 2. 🌍 Multi-Currency FX Conversion & Settlement Subsystem
* **Domain Context:** Support cross-currency transfers (e.g., source account in `INR`, destination in `USD`).
* **Planned Design:**
  * Fixed exchange rate provider SPI (`ExchangeRateProvider`).
  * Two-legged transfer with an intermediary platform FX suspense account.

---

### 3. 🛡️ Daily & Transactional Velocity Limits (Risk Engine)
* **Domain Context:** Prevent fraud and comply with regulatory limits (e.g., RBI UPI limit of ₹1,00,000/day for retail individual wallets).
* **Planned Design:**
  * Rule-based velocity checks before locking accounts.
  * Tiered limits based on KYC completeness and PartyType.

---

### 4. 📯 Distributed Event Streaming (Kafka / AWS SQS Adapter)
* **Domain Context:** As transaction volume scales, stream audit events to external data lakes and compliance monitoring systems.
* **Planned Design:**
  * Implement `KafkaEventPublisher` implementing `EventPublisher` SPI.
  * Activate via `@ConditionalOnProperty(name = "omniledger.events.publisher.type", havingValue = "kafka")`.

---

### 5. 📑 Periodic Account Statement & Trial Balance Generation
* **Domain Context:** End-of-day / monthly statement generation for auditing and customer downloads.
* **Planned Design:**
  * Read-only projection queries calculating historical running balances from `PostingLine` entries.
  * Verification that $\sum \text{Assets} + \sum \text{Expenses} = \sum \text{Liabilities} + \sum \text{Equity} + \sum \text{Revenues}$.
