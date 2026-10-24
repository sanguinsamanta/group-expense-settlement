# Group Expense Settlement System (Backend)

A clean, production-ready Spring Boot backend for managing group expenses and calculating simplified debt settlements. Designed as a Second-Year Computer Science & Engineering (CSE) mini-project.

---

## 🛠️ Tech Stack

- **Java**: Java 21+ (Compatible with OpenJDK 21 / 25 LTS)
- **Framework**: Spring Boot 3.4
- **Web Layer**: Spring Web (REST API with JSON responses)
- **Persistence**: Spring Data JPA / Hibernate
- **Database**: PostgreSQL
- **Validation**: Bean Validation (Jakarta Validation)
- **Build Tool**: Maven (with included Maven Wrapper `mvnw` / `mvnw.cmd`)
- **Testing**: JUnit 5, Spring Boot Test, H2 (for isolated in-memory test execution)

---

## 🏛️ Architecture & Design Principles

The application adheres to a clean layered architecture that is easy to navigate, maintain, and explain during a college viva:

```
[Browser / Frontend Client]
          │ (JSON over HTTP)
          ▼
   [REST Controller]          <--- Request/Response DTOs & Validation
          │
          ▼
   [Service Layer]            <--- Business Logic, Settlement Algorithm (Greedy), Net Balance Math
          │
          ▼
   [Repository Layer]         <--- Spring Data JPA Interfaces
          │
          ▼
 [PostgreSQL Database]        <--- Normalized Tables (Groups, Members, Expenses, Shares)
```

- **Separation of Concerns**: API Request/Response DTOs are kept strictly separated from JPA Entities.
- **Financial Precision**: All monetary calculations and balances use `java.math.BigDecimal` to prevent floating-point rounding errors.
- **Transaction Safety**: All multi-step persistence operations (e.g., creating expenses and splits) run under `@Transactional`.

---

## 📁 Package Structure

```
src/main/java/com/groupexpense/settlement/
├── GroupExpenseSettlementApplication.java   # Spring Boot Main Entry Point
├── config/                                  # Application & CORS configuration
├── controller/                              # REST API Controllers (HTTP endpoints)
├── dto/                                     # Request & Response Data Transfer Objects
├── entity/                                  # JPA Entities (Database mapping)
├── exception/                               # Custom exceptions & global exception handler
├── repository/                              # Spring Data JPA repositories
└── service/                                 # Business logic & greedy settlement algorithm
```

---

## 📋 Prerequisites

Before running the application locally, ensure you have:

1. **Java Development Kit (JDK)**: JDK 21 or later installed (`java -version`).
2. **PostgreSQL**: PostgreSQL 14+ installed and running locally.
3. **Maven**: Optional (The project includes standard Maven Wrappers `mvnw.cmd` and `mvnw`).

---

## 🚀 Setup & Execution Instructions

### 1. Configure PostgreSQL

1. Start your local PostgreSQL server.
2. Open `psql` or pgAdmin and create the database:
   ```sql
   CREATE DATABASE group_expense_settlement;
   ```

### 2. Configure Environment Variables

Copy `.env.example` to `.env` or set the environment variables in your terminal/IDE:

| Variable | Description | Default Value |
| :--- | :--- | :--- |
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `group_expense_settlement` |
| `DB_USERNAME` | PostgreSQL username | `postgres` |
| `DB_PASSWORD` | PostgreSQL password | `postgres` |
| `PORT` | Application HTTP port | `8080` |

### 3. Build & Test the Application

Using the provided Maven Wrapper (no global Maven installation required):

- **Windows (PowerShell / Command Prompt)**:
  ```powershell
  .\mvnw.cmd clean test
  ```

- **Linux / macOS**:
  ```bash
  ./mvnw clean test
  ```

*(Note: Unit and context tests use an isolated in-memory H2 database in PostgreSQL mode, so tests execute successfully without needing a live PostgreSQL instance running).*

### 4. Run the Application

- **Windows**:
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```

- **Linux / macOS**:
  ```bash
  ./mvnw spring-boot:run
  ```

Once started, the application will listen on `http://localhost:8080`.

---

## 📡 REST API Reference

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/groups` | Create an expense group |
| `GET` | `/api/groups` | List all groups |
| `GET` | `/api/groups/{groupId}` | Get group details and member list |
| `POST` | `/api/groups/{groupId}/members` | Add a member to an existing group |
| `GET` | `/api/groups/{groupId}/members` | List members of a group |
| `POST` | `/api/groups/{groupId}/expenses` | Record an expense (`EQUAL` or `CUSTOM` split) |
| `GET` | `/api/groups/{groupId}/expenses` | Retrieve expense history (sorted by date DESC) |
| `GET` | `/api/groups/{groupId}/balances` | Get net balance for every group member |
| `GET` | `/api/groups/{groupId}/settlements` | Generate suggested transfers using Greedy Algorithm |

---

## 💡 Greedy Settlement Algorithm (Viva Concept)

1. **Net Balance Calculation**:
   $$\text{netBalance} = \sum \text{Total Paid} - \sum \text{Assigned Shares}$$
   - Positive $\to$ Creditor (group owes member)
   - Negative $\to$ Debtor (member owes group)
   - Zero $\to$ Settled
   - **Zero-Sum Property**: $\sum \text{netBalance} \equiv 0.00$

2. **Greedy Matching**:
   - Debtors and creditors are sorted in descending order of amounts.
   - The algorithm repeatedly matches the maximum debtor with the maximum creditor, transferring $\min(\text{debtorAmount}, \text{creditorAmount})$.
   - The balances are decremented and the process continues until all balances are $0.00$.
   - **Theoretical Note**: Greedy matching guarantees that all debts are settled with zero cash drift. While it does not guarantee the globally minimum number of transactions (which is NP-complete, reducible from Subset Sum), it runs in $O(N \log N)$ and provides a highly optimized and practical solution.
