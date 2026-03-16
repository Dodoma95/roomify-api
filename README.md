# 🏠 Roomify API

> A modern **secure backend API** built with **Spring Boot 3 / Java 21** featuring **JWT authentication, REST + GraphQL APIs, database migrations,
CI/CD and code quality monitoring**.

---

# 🚀 Badges

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.2-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-blue)  
![Build](https://github.com/Dodoma95/roomify-api/actions/workflows/ci.yml/badge.svg)
![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Dodoma95_roomify-api&metric=alert_status)
![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Dodoma95_roomify-api&metric=coverage)
![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Dodoma95_roomify-api&metric=security_rating)
![Maintainability](https://sonarcloud.io/api/project_badges/measure?project=Dodoma95_roomify-api&metric=sqale_rating)
![License](https://img.shields.io/badge/license-MIT-blue)

---

# 📖 Overview

**Roomify** is a modern backend API designed with **clean architecture principles**.

It demonstrates how to build a **production-ready backend** using modern Java ecosystem tools:

- secure authentication
- resilient API
- automated CI/CD
- database migrations
- test isolation with containers
- static code analysis

The project exposes both:

- **REST API**
- **GraphQL API**

---

# 🧰 Tech Stack

| Layer             | Technology            |
|-------------------|-----------------------|
| Backend           | Java 21               |
| Framework         | Spring Boot 4.0.2     |
| Security          | Spring Security + JWT |
| Database          | PostgreSQL            |
| Migration         | Flyway                |
| Resilience        | Resilience4j          |
| Testing           | JUnit 5               |
| Integration Tests | Testcontainers        |
| CI/CD             | GitHub Actions        |
| Code Quality      | SonarCloud            |
| API               | REST + GraphQL        |

---

# 📡 API Endpoints

### Swagger UI

[SWAGGER-UI](https://roomify-api-production.up.railway.app/swagger-ui/index.html)

---

# 🧱 Architecture

The project follows a **Clean / Hexagonal inspired architecture**.

```text
src
├── presentation
│   ├── rest
│   └── graphql
│
├── domain
│   ├── model
│   ├── service
│   └── exception
│
├── infrastructure
│   ├── persistence
│   ├── security
│   └── configuration
│
└── db
    └── migration
```

### Goals

✔ isolate business logic  
✔ decouple infrastructure  
✔ improve testability

---

# 🔐 Authentication Flow

The API uses **stateless JWT authentication**.

```text
User
 │
 │ POST /api/v1/auth/register
 ▼
Create account
 │
 │ GET /api/v1/auth/verify
 ▼
Verify account
 │
 │ POST /api/v1/auth/login
 ▼
JWT Token issued
 │
 │ Authorization: Bearer <token>
 ▼
Access secured APIs
```

Security features:

- JWT authentication
- email verification
- rate limiting (Resilience4j)
- stateless API

sequenceDiagram

Client->>API: POST /login
API->>Database: Validate credentials
Database-->>API: User

API-->>Client: JWT Token

Client->>API: Request with Authorization Bearer token
API->>API: Validate token
API-->>Client: Secured resource

---

# 🗄 Database Schema

Simplified schema:

users  
roles  
user_roles  
verification_tokens  
refresh_tokens

Database migrations are handled by **Flyway**.

Location: `src/main/resources/db/migration`

---

# 🧪 Testing Strategy

The project contains two types of tests.

## Unit Tests

Run only the logic layer.

```bash
mvn test
```

## Integration Tests

Run against a **real PostgreSQL container**.

```bash
mvn verify
```

Powered by **Testcontainers**.

Benefits:

✔ real database  
✔ isolated environment  
✔ reproducible CI builds

---

# ⚙️ Running the Project Locally

## 1️⃣ Environment Variables

Before starting the application, configure the following environment variables:

```bash
PGHOST=localhost
PGPORT=5432
PGDATABASE=roomify
PGUSER=roomify
PGPASSWORD=roomify
```

## 2️⃣ Start PostgreSQL 

Example using Docker:

```bash
docker compose up
```

## 3️⃣ Start the Application

```bash
mvn spring-boot:run
```

The API will start on:

```text
http://localhost:8080
```