# Fund Transfer Ledger Service 🏦🚀

A high-concurrency, ACID-compliant financial backend service built with **Spring Boot** and **PostgreSQL**, implementing strict **double-entry bookkeeping** and robust **deadlock prevention**.

---

## 🌟 Key Features

*   **Double-Entry Ledger Pattern:** Ensures that every single financial movement is balanced. Every transaction creates an immutable `JournalEntry` header followed by balanced `PostingLine` entries (`DEBIT` and `CREDIT`).
*   **Deadlock Prevention Mechanism:** Prevents circular-wait deadlocks under high-concurrency multi-threaded load by sorting account `UUID`s to establish a strict, deterministic lock acquisition order before executing queries.
*   **Row-Level Locking (`FOR UPDATE`):** Leverages database-level serialization to safely queue and isolate concurrent updates to the exact same account rows without manual thread-sleep logic or CPU wasting.
*   **Strict Financial Math:** Utilizes `BigDecimal` and precise comparison strategies to eliminate floating-point rounding errors typical in financial applications.
*   **ACID Transaction Integrity:** Wrapped with `@Transactional` boundaries to guarantee all-or-nothing atomicity, ensuring automatic rollbacks on exceptions like `InsufficientBalance`.

---

## 🛠️ Technology Stack

*   **Java 17**
*   **Spring Boot 3.2.5**
*   **Spring Data JPA / Hibernate**
*   **PostgreSQL JDBC Driver**
*   **Lombok**
*   **Maven**

---

## ⚙️ How Concurrency & Locking Work

1.  **Application vs. Database Concurrency:** Java application threads execute concurrently across CPU cores. When two threads target the same account row, PostgreSQL serializes access at the database level.
2.  **Driver-Level Blocking:** The JDBC driver safely pauses waiting threads at the execution line until the holding transaction commits and releases its row locks.
3.  **Deterministic Ordering:** By comparing account IDs (`sourceAccountId.compareTo(targetAccountId)`), all threads acquire locks in the same sequence, completely eliminating potential database deadlocks.

---

## 🚀 Getting Started

### Prerequisites
*   Java 17 or higher
*   Maven 3.6+
*   PostgreSQL Database instance

### Configuration (`application.yml` / `application.properties`)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ledger_db
spring.datasource.username=postgres
spring.datasource.password=your_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

### Build & Run
```bash
mvn clean install
mvn spring-boot:run
```

---

## 📂 Project Structure Overview

*   `model/` - Domain entities (`Account`, `JournalEntry`, `PostingLine`, `PostingType`)
*   `repository/` - Spring Data JPA interfaces for database access
*   `service/` - Core fund transfer logic, validation, deadlock sorting, and locking execution
*   `exception/` - Custom error handling (`InsufficientBalanceException`, `AccountNotFoundException`)

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).