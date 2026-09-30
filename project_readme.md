# 🏦 OmniLedger: High-Concurrency ACID Financial Ledger Service 🚀

A high-concurrency, ACID-compliant financial backend service built with **Spring Boot 3.x** and **PostgreSQL**, implementing strict **double-entry bookkeeping**, **deadlock prevention**, and **idempotency protection**.

---

## 🌟 Key Features

* **Double-Entry Ledger Pattern:** Ensures that every single financial movement is balanced. Every transaction creates an immutable `JournalEntry` header followed by balanced `PostingLine` entries (`DEBIT` and `CREDIT`) where $\sum \text{Debits} = \sum \text{Credits}$.
* **Deadlock Prevention Mechanism:** Prevents circular-wait deadlocks under high-concurrency multi-threaded load by sorting account `UUID`s to establish a strict, deterministic lock acquisition order before executing queries.
* **Row-Level Locking (`SELECT FOR UPDATE`):** Leverages database-level pessimistic serialization to safely queue and isolate concurrent updates to the exact same account rows without race conditions or CPU spin-locks.
* **Strict Financial Math:** Utilizes `BigDecimal` with high-precision arithmetic to completely eliminate floating-point rounding errors.
* **ACID Transaction Integrity:** Wrapped with `@Transactional` boundaries to guarantee all-or-nothing atomicity, ensuring automatic rollbacks on business rule violations (e.g., `InsufficientBalanceException`).
* **Idempotency Guarantee:** Enforces unique idempotency keys at both the database constraint layer and service layer to guarantee safe retry handling.
* **Interactive API Documentation:** Integrated OpenAPI 3.0 / Swagger UI for visual endpoint exploration and testing.

---

## 🛠️ Technology Stack

| Component | Technology / Version |
| :--- | :--- |
| **Language** | Java 21 (LTS) |
| **Framework** | Spring Boot 3.3.4 |
| **Persistence** | Spring Data JPA / Hibernate 6 |
| **Database** | PostgreSQL |
| **Documentation** | Springdoc OpenAPI / Swagger UI 2.6.0 |
| **Boilerplate** | Lombok |
| **Build Tool** | Maven 3.9+ |

---

## ⚙️ How Concurrency & Locking Work

1. **Application vs. Database Concurrency:** Java application threads execute concurrently across CPU cores. When multiple threads target the same account row, PostgreSQL serializes access at the database level.
2. **Driver-Level Blocking:** The JDBC driver safely pauses waiting threads at the query execution line until the holding transaction commits and releases its row locks.
3. **Deterministic Ordering:** By comparing account IDs (`sourceAccountId.compareTo(targetAccountId)`), all threads acquire locks in the exact same sequence, completely eliminating potential circular database deadlocks.

---

## 🔌 API Endpoints & Swagger

Interactive Swagger documentation is available once the application is running:
* **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
* **OpenAPI Spec:** `http://localhost:8080/v3/api-docs`

### Core API Routes
* `POST /api/v1/transfers` - Execute an ACID-compliant double-entry transfer (requires `idempotencyKey`)
* `GET /api/v1/transfers/{id}` - Retrieve and audit a journal entry and its posting lines
* `POST /api/v1/accounts` - Create a new financial account
* `GET /api/v1/accounts/{id}` - Retrieve account details and current balance

---

## 🚀 Getting Started

### Prerequisites
* Java 21 LTS installed
* Maven 3.6+
* PostgreSQL database instance running

### Configuration (`application.yml`)
```yaml
spring:
  application:
    name: omni_ledger
  datasource:
    url: jdbc:postgresql://localhost:5432/ledger_db
    username: postgres
    password: your_password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

server:
  port: 8080

springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

### Build & Run
```bash
# Build the application
mvn clean compile

# Run the test suite
mvn test

# Run the service
mvn spring-boot:run
```

---

## 📂 Project Structure Overview

```text
dev.fintech.omniledger
├── controller     # REST API endpoints (TransferController, AccountController)
├── dto            # Request/Response payloads (TransferRequest, TransferResponse)
├── exception      # Domain exceptions and GlobalExceptionHandler
├── model          # Domain entities (Account, JournalEntry, PostingLine, PostingType)
├── repository     # Spring Data JPA repositories with pessimistic locking
└── service        # Core transfer logic, lock sorting, idempotency, validation
```

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).