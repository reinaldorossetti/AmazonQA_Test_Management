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

## Seed Data (JSON + Startup Runner)

The backend now includes a startup seed runner similar to the referenced `SeedDataRunner` pattern.

Seed files are located at:

- `src/main/resources/seed/users.json`
- `src/main/resources/seed/projects.json`
- `src/main/resources/seed/builds.json`
- `src/main/resources/seed/test_plans.json`
- `src/main/resources/seed/executions.json`
- `src/main/resources/seed/audit_logs.json`
- `src/main/resources/seed/requirements.json`
- `src/main/resources/seed/suites.json`
- `src/main/resources/seed/test_cases.json`
- `src/main/resources/seed/defects.json`
- `src/main/resources/seed/report_jobs.json`

Runtime flags (`.env`):

- `SEED_DATA_ENABLED` → enable/disable startup seed execution
- `SEED_RESET_DB` → when `true`, truncates SQL tables and clears `StateStore` before insert
- `SEED_STRICT_VALIDATION` → when `true`, startup fails if post-seed validation fails

Notes:

- SQL seeding targets migration tables (`V1` + `V2`): `users`, `projects`, `builds`, `test_plans`, `executions`, `audit_logs`, `test_cases`.
- In-memory seeding also fills `StateStore` structures used by API modules (`requirements`, `suites`, `test cases`, `defects`, `report jobs`).
- Validation runs after load for both SQL and in-memory datasets.

## User Registration and Profile Endpoints

Implemented endpoints for user onboarding and profile/address lifecycle:

- `POST /api/v1/users/register` (public)
- `POST /api/v1/admin/users/full` (admin)
- `GET /api/v1/admin/users/{userId}/profile` (admin)
- `PATCH /api/v1/admin/users/{userId}/profile` (admin)
- `PATCH /api/v1/admin/users/{userId}/address` (admin)

Payloads use `snake_case` JSON fields (for example: `first_name`, `address_city`, `residence_proof_filename`).

Persistence model:

- Base account data persisted in `users` table.
- Extended profile data persisted in `user_profiles` table (`V3__create_user_profiles.sql`).
- Password is persisted as BCrypt hash in `user_profiles.password_hash`.

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
- Password: `AmazonQA@2026!#`

`psql` example connection:

- `psql -h localhost -p 5432 -U amazonqa -d amazonqa`

## Local PostgreSQL (optional)

This project includes `docker-compose.postgres.yml` with:

- PostgreSQL `17.9`
- automatic database creation using `POSTGRES_DB`
- persistent data on host path `G:/AmazonQA_Test_Management/postgres-data`
- automatic table creation on first startup via:
  - `src/main/resources/db/migration/V1__init.sql`

Start container:

- `docker compose -f docker-compose.postgres.yml up -d`

Stop container:

- `docker compose -f docker-compose.postgres.yml down`

Reset data and force DB/table re-initialization on next up:

- `docker compose -f docker-compose.postgres.yml down`
- remove folder `G:\AmazonQA_Test_Management\postgres-data`

Equivalent compose content:

```yaml
services:
  postgres:
    image: postgres:17.9
    environment:
      POSTGRES_DB: amazonqa
      POSTGRES_USER: amazonqa
      POSTGRES_PASSWORD: "AmazonQA@2026!#"
    ports:
      - "5432:5432"
    volumes:
      - G:/AmazonQA_Test_Management/postgres-data:/var/lib/postgresql/data
      - ./src/main/resources/db/migration/V1__init.sql:/docker-entrypoint-initdb.d/01-V1__init.sql:ro
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
