# backend-kotlin

Backend service for **Amazon QA Test Case Management** using **Kotlin + Spring Boot + PostgreSQL**.

## Stack

- Kotlin 1.9.x
- Spring Boot 3.5.x
- Java 23 (toolchain)
- PostgreSQL + Flyway
- Spring Security (JWT-like bearer token flow for MVP)
- OpenAPI contract in `docs/api/openapi.yaml`
- Testing: JUnit 5, MockK, Mockito Kotlin, Testcontainers
- Quality: ktlint + detekt

## Quick Start

1. Ensure PostgreSQL is running (see compose below).
2. Fill `.env` in project root (`backend-kotlin/.env`).
3. Run tests and quality checks:
   - `./gradlew.bat clean test ktlintCheck detekt`
4. Run app:
   - `./gradlew.bat bootRun`

## API Tests Implemented (Real HTTP, No Mocks)

The project now includes real API integration tests using:

- Rest-Assured (HTTP assertions)
- DataFaker (dynamic test data)
- Spring Boot random port (`@SpringBootTest(webEnvironment = RANDOM_PORT)`)
- Bearer tokens for RBAC scenarios (`admin-token`, `leader-token`, `tester-token`, `guest-token`)

Main test base/helper:

- `src/test/kotlin/com/amazonqa/api/ApiIntegrationTestBase.kt`
  - Common Rest-Assured configuration
  - Generic authenticated request builders
  - Generic helper methods to create project/user/requirement/suite/test case/build/plan/run/defect

Feature-based suites:

- `AuthAndSelfApiIntegrationTest.kt` (auth + self endpoints)
- `AdminUserAccessApiIntegrationTest.kt` (user CRUD + roles/permissions + access rules)
- `ProjectRequirementApiIntegrationTest.kt` (project and requirement CRUD + import + traceability)
- `SuiteTestCaseApiIntegrationTest.kt` (suite/test case CRUD + versioning + bulk + search)
- `PlanningExecutionApiIntegrationTest.kt` (build/plan/run + execution rules)
- `DefectReportingApiIntegrationTest.kt` (defects + Jira config/verify + reports)

Also kept and validated:

- Contract and endpoint mapping coverage
- Security policy checks
- Core business rule tests
- OpenAPI validation

## How to Run Tests in Terminal

From `backend-kotlin` folder:

- Run all tests:
  - `./gradlew.bat test`
- Run lint + static analysis:
  - `./gradlew.bat ktlintCheck detekt`
- Run full quality gate:
  - `./gradlew.bat clean test ktlintCheck detekt`
- Run only API integration tests package:
  - `./gradlew.bat test --tests "com.amazonqa.api.*"`

## How to Start the Application

From `backend-kotlin` folder:

- Start app:
  - `./gradlew.bat bootRun`

Useful URLs after startup:

- API base: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui`
- OpenAPI docs: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`

## How to Connect to PostgreSQL

Application database configuration comes from environment variables in `.env`:

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `POSTGRES_PORT`

Default JDBC used by the app:

- `jdbc:postgresql://localhost:${POSTGRES_PORT:5432}/${POSTGRES_DB:amazonqa}`

If using local compose from this README, use:

- Host: `localhost`
- Port: `5432`
- Database: `amazonqa`
- User: `amazonqa`
- Password: `amazonqa_password`

`psql` example connection:

- `psql -h localhost -p 5432 -U amazonqa -d amazonqa`

## Local PostgreSQL (optional)

You can use this compose:

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: amazonqa
      POSTGRES_USER: amazonqa
      POSTGRES_PASSWORD: amazonqa_password
    ports:
      - "5432:5432"
```

## Auth Tokens for MVP

Use header `Authorization: Bearer <token>`:

- `admin-token`
- `leader-token`
- `tester-token`
- `guest-token`

## DoD Mapping

- All v1 endpoints are implemented in `src/main/kotlin/com/amazonqa/api/v1`.
- Business rules: last admin protection, soft delete flows, draft-only deletion, closed-build immutability.
- OpenAPI source-of-truth at `docs/api/openapi.yaml`.
- Quality gates: `ktlintCheck`, `detekt`, `test`.
