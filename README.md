# Roomify API

> Backend API for a space-rental platform — REST + GraphQL, stateless JWT auth, hexagonal architecture.

[![Build](https://github.com/Dodoma95/roomify-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Dodoma95/roomify-api/actions/workflows/ci.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=Dodoma95_roomify-api&metric=alert_status)](https://sonarcloud.io/project/overview?id=Dodoma95_roomify-api)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Dodoma95_roomify-api&metric=coverage)](https://sonarcloud.io/project/overview?id=Dodoma95_roomify-api)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Dodoma95_roomify-api&metric=security_rating)](https://sonarcloud.io/project/overview?id=Dodoma95_roomify-api)
[![Maintainability](https://sonarcloud.io/api/project_badges/measure?project=Dodoma95_roomify-api&metric=sqale_rating)](https://sonarcloud.io/project/overview?id=Dodoma95_roomify-api)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.2-green)
![License](https://img.shields.io/badge/license-MIT-blue)

---

## Overview

**Roomify** is a production-ready backend for a space-rental marketplace. Owners list rentable spaces (meeting rooms, coworking desks, studios, event spaces…); users discover and book them.

The API exposes two interfaces in parallel:

| Interface | Base       | Explorer                                                                                                                              |
|-----------|------------|---------------------------------------------------------------------------------------------------------------------------------------|
| REST      | `/api/v1/` | [Swagger UI](https://roomify-api-1ik6.onrender.com/swagger-ui/index.html)                                                     |
| GraphQL   | `/graphql` | [GraphiQL](https://roomify-api-1ik6.onrender.com/graphiql) · [Voyager](https://roomify-api-1ik6.onrender.com/voyager) |

---

## Tech Stack

| Concern      | Choice                                  |
|--------------|-----------------------------------------|
| Language     | Java 21                                 |
| Framework    | Spring Boot 4.0.2                       |
| REST         | Spring MVC                              |
| GraphQL      | Spring GraphQL                          |
| Security     | Spring Security + JWT (jjwt 0.11.5)     |
| Persistence  | Spring Data JPA / Hibernate             |
| Database     | PostgreSQL 16                           |
| Migrations   | Flyway                                  |
| Mapping      | MapStruct 1.6.3                         |
| Resilience   | Resilience4j (`@RateLimiter`, `@Retry`) |
| Email        | Brevo HTTP API via WebClient            |
| API Docs     | springdoc-openapi 3.0.1                 |
| Testing      | JUnit 5 + Testcontainers                |
| Code Quality | SonarCloud + JaCoCo                     |
| CI/CD        | GitHub Actions                          |
| Boilerplate  | Lombok                                  |

---

## Architecture

The project follows **hexagonal (clean) architecture**. The domain layer has zero dependency on Spring, JPA, or any framework.

```
com.roomify
├── presentation/           # Delivery layer
│   ├── endpoint/           # REST controllers
│   ├── resolver/           # GraphQL resolvers
│   └── models/
│       ├── in/             # Request / input models (Bean Validation)
│       └── out/            # Response / output models
│
├── domain/                 # Pure business logic
│   ├── api/                # Interfaces consumed by the presentation layer
│   ├── spi/                # Interfaces implemented by the infrastructure layer
│   ├── service/            # Use-case implementations
│   └── models/             # Domain models & enums
│
├── infrastucture/          # Adapters & framework wiring
│   ├── adapter/            # SPI implementations (JPA, email…)
│   ├── repository/         # Spring Data repositories
│   ├── models/             # JPA entities
│   └── filter/             # JWT filter
│
├── configuration/          # Spring beans (Security, OpenAPI, GraphQL…)
└── shared/                 # Cross-cutting: exceptions, GlobalExceptionHandler, utils
```

### Key patterns

- **SPI inversion** — domain services declare `*Spi` interfaces; infrastructure implements them. The domain never imports JPA.
- **API inversion** — controllers inject `*Api` interfaces; `*Service` classes implement them.
- **Event-driven email** — domain publishes `ApplicationEvent`; infrastructure listens and delegates to Brevo via WebClient, protected by `@Retry`.

---

## Authentication

The API uses **stateless JWT authentication** with email verification.

```
POST /api/v1/auth/register   →  account created (unverified)
GET  /api/v1/auth/verify     →  email link clicked, account activated
POST /api/v1/auth/login      →  returns Bearer JWT
POST /api/v1/auth/resend-verification  →  resend verification email
```

Every protected endpoint requires the header:

```
Authorization: Bearer <token>
```

### Roles

| Role          | Description                               |
|---------------|-------------------------------------------|
| `USER`        | Standard authenticated user               |
| `OWNER`       | Can list and manage their own spaces      |
| `ADMIN`       | Can approve / reject spaces, manage users |
| `SUPER_ADMIN` | Full access                               |

Rate limiting is enforced on write operations via Resilience4j.

---

## REST API

Full interactive documentation: [Swagger UI](https://roomify-api-1ik6.onrender.com/swagger-ui/index.html)

### Auth — public endpoints

| Method | Path                               | Description                 |
|--------|------------------------------------|-----------------------------|
| `POST` | `/api/v1/auth/register`            | Register a new account      |
| `POST` | `/api/v1/auth/login`               | Obtain a JWT token          |
| `GET`  | `/api/v1/auth/verify`              | Verify email via token link |
| `POST` | `/api/v1/auth/resend-verification` | Resend verification email   |

### Places — requires JWT

| Method   | Path                  | Roles                                | Description    |
|----------|-----------------------|--------------------------------------|----------------|
| `POST`   | `/api/v1/places`      | OWNER, ADMIN, SUPER_ADMIN            | Create a space |
| `GET`    | `/api/v1/places`      | All authenticated                    | List spaces    |
| `PATCH`  | `/api/v1/places/{id}` | Owner of space · ADMIN · SUPER_ADMIN | Partial update |
| `DELETE` | `/api/v1/places/{id}` | Owner of space · ADMIN · SUPER_ADMIN | Delete a space |

### Users — requires JWT

| Method | Path            | Roles              | Description    |
|--------|-----------------|--------------------|----------------|
| `GET`  | `/api/v1/users` | ADMIN, SUPER_ADMIN | List all users |

---

## GraphQL API

Interactive schema explorer: [GraphiQL](https://roomify-api-1ik6.onrender.com/graphiql) · schema visualizer: [Voyager](https://roomify-api-1ik6.onrender.com/voyager)

### Query — `places`

Search spaces with optional filtering and pagination. All fields are optional and combined with logical AND.

```graphql
query {
  places(
    filter: {
      types: [MEETING_ROOM, COWORKING_SPACE]
      statuses: [APPROVED]
      nameContains: "Paris"
      capacityMin: 5
      capacityMax: 20
      pricePerHourMin: 10.0
      pricePerHourMax: 80.0
    }
    pagination: { page: 0, pageSize: 10 }
  ) {
    results {
      id
      name
      type
      address
      capacity
      pricePerHour
      status
      owner {
        firstName
        lastName
        email
      }
    }
    pageInfo {
      page
      pageSize
      totalElements
      totalPages
      hasNext
      hasPrevious
    }
  }
}
```

**Authentication required** — Bearer JWT with any role (`USER`, `OWNER`, `ADMIN`, `SUPER_ADMIN`).

### Place types

`MEETING_ROOM` · `COWORKING_SPACE` · `EVENT_SPACE` · `PARTY_ROOM` · `STUDIO`

### Place statuses

| Status     | Meaning                                     |
|------------|---------------------------------------------|
| `PENDING`  | Awaiting admin review (default on creation) |
| `APPROVED` | Visible to all authenticated users          |
| `REJECTED` | Refused by an admin                         |

---

## Database Schema

Migrations are managed by **Flyway** (`src/main/resources/db/migration/`).

```
roomify.users
roomify.roles
roomify.user_roles
roomify.verification_tokens
roomify.places
```

```sql
-- Key constraints on places
UNIQUE (user_id, name, address)          -- no duplicate listing per owner
FOREIGN KEY (user_id) REFERENCES users   -- ownership link
INDEX on (type), (status), (user_id)     -- query performance
```

---

## Running Locally

### Prerequisites

- Java 21
- Maven 3.9+
- Docker (for PostgreSQL and integration tests)

### 1. Start PostgreSQL

```bash
docker compose up -d postgres
```

### 2. Configure environment variables

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/roomify
export SPRING_DATASOURCE_USERNAME=roomify
export SPRING_DATASOURCE_PASSWORD=roomify
export JWT_SECRET=<your-256-bit-secret>
export BREVO_API_KEY=<your-brevo-key>
export API_BASE_URL=http://localhost:8080
```

### 3. Start the application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The API starts on `http://localhost:8080`.

| Tool       | URL                                         |
|------------|---------------------------------------------|
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| GraphiQL   | http://localhost:8080/graphiql              |
| Voyager    | http://localhost:8080/voyager               |

---

## Testing

### Unit tests

```bash
mvn test
```

### Integration tests

Run against a **real PostgreSQL container** via Testcontainers (requires Docker).

```bash
mvn verify
```

All integration test classes extend `AbstractIntegrationTest`, which spins up a throwaway PostgreSQL container and binds the datasource dynamically. Tests are isolated — each class cleans its own data with `@Sql` scripts.

---

## CI/CD & Code Quality

GitHub Actions runs on every push and pull request:

1. Build & unit tests (`mvn test`)
2. Integration tests (`mvn verify`)
3. SonarCloud analysis (coverage, security rating, maintainability)

Quality gates and coverage reports are visible on [SonarCloud](https://sonarcloud.io/project/overview?id=Dodoma95_roomify-api).

---

## License

MIT
