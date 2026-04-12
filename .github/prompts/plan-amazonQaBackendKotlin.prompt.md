# Backend Architecture Plan — Amazon QA (Spring Boot + Kotlin)

## 1. Objective

Build the backend for **Amazon QA Test Case Management** using:

- **Kotlin**
- **Spring Boot 3.3+**
- **Java SDK 23** - Targeting Java 21 for runtime compatibility while using Java 23 toolchain for development/build.
- *Data Base PostgreSQL*
- Project folder name: **`backend-kotlin`**
- Virtual Threads nativas (Project Loom)
- Virtual Threads (spring.threads.virtual.enabled=true).Com Virtual Threads, você tem o desempenho e escala do modelo reativo (WebFlux) sem precisarmos abandonar a programação imperativa cláscica, síncrona e fácil de debugar do Spring Web MVC.
- Spring Web MVC - Resiliência e Clientes REST Modernos (Clean Code), Adicionar o uso do Resilience4j (para Circuit Breakers e Retry Policies automáticas de falhas com o Jira) e recomendar fortemente o uso da nova interface RestClient do Spring Boot 3.2+ (mais leve e coesa no Kotlin) ao invés do antigo RestTemplate ou do pesado WebClient.
- MDC (Mapped Diagnostic Context) do SLF4J/Logback, injetando automaticamente o userId e o projectId ativo do token no Log principal de toda request. Isso garante que cada passo da stack de tracing do Spring Security identifique no console do Kibana/Datadog qual usuário cometeu o Access Denied.

The backend must prioritize:

- End-to-end traceability (Requirement → Test Case → Execution → Defect → Metrics)
- Security and RBAC
- Immutable versioning for test cases
- Auditability and compliance
- Maintainability with SOLID and clean architecture patterns

---

## 2. Scope and Boundaries

### In Scope (MVP - Phase 1)

1. Authentication and RBAC
2. **User CRUD with multiple access profiles**
3. Project and membership management
4. Requirements import and traceability matrix
5. Test suite and test case lifecycle (CRUD + versioning)
6. Test plan, build, run, execution
7. Defect integration (at least one bug tracker: Jira or Mantis)
8. Operational metrics and report export (CSV/PDF)
9. Audit log for critical actions

### Out of Scope (Phase 2+)

- AI-assisted writing (`Write with AI`)
- Predictive analytics
- Smart test selection by AI

---

## 3. Technology Stack

- Kotlin 2.x
- Spring Boot 3.3+
- Java 23
- Gradle Kotlin DSL
- PostgreSQL (primary database)
- Redis (cache)
- Flyway (database migrations)
- Spring Security + JWT
- OpenAPI 3 (contract-first API docs)
- JUnit 5 + Testcontainers + MockK
- ktlint + detekt
- Micrometer + OpenTelemetry

---

## 4. High-Level Architecture

Adopt a **Modular Monolith** with clear boundaries per domain module.

Layers:

1. `api` (controllers, DTOs, exception handlers)
2. `application` (use cases, orchestration services)
3. `domain` (entities, value objects, domain rules, repository contracts)
4. `infrastructure` (persistence, external integrations, cache, queue)

Design principles:

- SOLID
- Dependency inversion (interfaces in domain/application, implementations in infrastructure)
- OpenAPI-first contracts
- Event-driven integration points for retryable external calls

---

## 5. Folder Structure (`backend-kotlin`)

- `backend-kotlin/src/main/kotlin/com/amazonqa/api/v1`
- `backend-kotlin/src/main/kotlin/com/amazonqa/application`
- `backend-kotlin/src/main/kotlin/com/amazonqa/domain`
- `backend-kotlin/src/main/kotlin/com/amazonqa/infrastructure`
- `backend-kotlin/src/main/resources/db/migration`
- `backend-kotlin/src/test/kotlin`
- `backend-kotlin/docs`
- `backend-kotlin/docs/api/openapi.yaml`
- `backend-kotlin/README.md`

---

## 6. Core Modules, Classes and Main Functions

## 6.1 Identity & RBAC

Classes:
- `AuthController`
- `AuthService`
- `JwtService`
- `RbacService`
- `UserService`
- `UserManagementController`
- `RoleManagementController`
- `AccessPolicyService`
- `PermissionService`

Main functions:
- `login()`
- `refreshToken()`
- `logout()`
- `getCurrentUser()`
- `authorize(user, action, resource)`
- `createUser()`
- `listUsers()`
- `getUserById()`
- `updateUser()`
- `deactivateUser()`
- `assignRoleToUser()`
- `removeRoleFromUser()`
- `setProjectScopedRole()`
- `listEffectivePermissions()`

### 6.1.1 User CRUD and Access Management Model

#### User entity model
- `User`: `id`, `fullName`, `email`, `status`, `createdAt`, `updatedAt`, `lastLoginAt`
- `UserCredential`: `userId`, `passwordHash`, `passwordUpdatedAt`, `mfaEnabled`
- `UserRoleAssignment`: `userId`, `role`, `scopeType`, `scopeId`, `assignedBy`, `assignedAt`

#### Access profile strategy (multiple profiles per user)
- A single user can have multiple profiles simultaneously, for example:
	- Global: `ADMIN`
	- Project scoped: `LEADER` in project A, `TESTER` in project B
- Effective permissions are computed by **union of active roles**, with deny-by-default.

#### Access management rules
- Default access is always denied when no explicit permission exists.
- Global roles apply to all resources; scoped roles apply only to matching `scopeId`.
- Privilege escalation actions (e.g., assign `ADMIN`) require `ADMIN` caller.
- The system must block removing the last active `ADMIN` user.
- Every role change is auditable with before/after snapshots.

---

## 6.2 Projects

Classes:
- `ProjectController`
- `ProjectService`
- `ProjectRepository`

Main functions:
- `createProject()`
- `listProjects()`
- `getProjectById()`
- `updateProject()`
- `deleteProject()`
- `restoreProject()`
- `addMember()`
- `removeMember()`

### 6.2.1 Project deletion policy
- Deletion is **soft delete by default** (`status=ARCHIVED`, `deletedAt`, `deletedBy`).
- Project deletion is blocked when there are active builds/runs in progress.
- Hard delete is admin-only and asynchronous (retention-safe purge job).
- Deleted projects can be restored within retention window.

---

## 6.3 Requirements & Traceability

Classes:
- `RequirementController`
- `RequirementService`
- `TraceabilityService`
- `RequirementImportService`

Main functions:
- `listRequirements()`
- `getRequirementById()`
- `importCsv()`
- `importXml()`
- `createRequirement()`
- `updateRequirement()`
- `deleteRequirement()`
- `restoreRequirement()`
- `linkTestCaseToRequirement()`
- `getRequirementCoverage()`
- `buildTraceabilityMatrix()`

---

## 6.4 Suites & Test Cases

Classes:
- `TestSuiteController`
- `TestCaseController`
- `TestCaseService`
- `TestCaseVersioningService`

Main functions:
- `createSuite()`
- `getSuiteById()`
- `updateSuite()`
- `deleteSuite()`
- `restoreSuite()`
- `createTestCase()`
- `getTestCaseById()`
- `updateTestCase()`
- `createNewVersion()`
- `archiveTestCase()`
- `deleteTestCase()`
- `restoreTestCase()`
- `bulkEditTestCases()`
- `searchTestCases()`

---

## 6.5 Planning, Builds and Execution

Classes:
- `BuildController`
- `TestPlanController`
- `ExecutionController`
- `ExecutionService`

Main functions:
- `createBuild()`
- `getBuildById()`
- `updateBuild()`
- `deleteDraftBuild()`
- `createTestPlan()`
- `getTestPlanById()`
- `updateTestPlan()`
- `deleteDraftTestPlan()`
- `createRun()`
- `assignExecutionsInBulk()`
- `setExecutionStatus()`
- `attachEvidence()`
- `closeBuild()`

---

## 6.6 Defects Integration

Classes:
- `DefectController`
- `DefectService`
- `JiraIntegrationService` (or `MantisIntegrationService`)

Main functions:
- `listDefects()`
- `getDefectById()`
- `createIssueFromExecution()`
- `updateDefect()`
- `closeDefect()`
- `reopenDefect()`
- `deleteDefect()`
- `verifyIntegrationConnection()`
- `retryFailedIssueCreation()`

---

## 6.7 Reports & Metrics

Classes:
- `MetricsController`
- `ReportingService`

Main functions:
- `getOperationalMetrics()`
- `exportCoverageCsv()`
- `exportExecutionPdf()`
- `createBuildSnapshot()`
- `createReportJob()`
- `getReportJobStatus()`
- `cancelReportJob()`

---

## 6.8 Audit & Compliance

Classes:
- `AuditController`
- `AuditService`
- `AuditInterceptor`

Main functions:
- `logCreate()`
- `logUpdate()`
- `logDelete()`
- `logDeny()`
- `queryAuditLogs()`

---

## 7. API Endpoints (v1)

Authentication:
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

Users:
- `GET /api/v1/users/me`
- `PATCH /api/v1/users/me/preferences`

User CRUD:
- `POST /api/v1/admin/users`
- `GET /api/v1/admin/users`
- `GET /api/v1/admin/users/{userId}`
- `PATCH /api/v1/admin/users/{userId}`
- `PATCH /api/v1/admin/users/{userId}/status`
- `DELETE /api/v1/admin/users/{userId}` (soft delete)

Access Management:
- `GET /api/v1/admin/roles`
- `GET /api/v1/admin/permissions`
- `GET /api/v1/admin/users/{userId}/roles`
- `POST /api/v1/admin/users/{userId}/roles`
- `DELETE /api/v1/admin/users/{userId}/roles/{role}`
- `POST /api/v1/admin/users/{userId}/scoped-roles`
- `DELETE /api/v1/admin/users/{userId}/scoped-roles/{role}`
- `GET /api/v1/admin/users/{userId}/effective-permissions`

Projects:
- `POST /api/v1/projects`
- `GET /api/v1/projects`
- `GET /api/v1/projects/{projectId}`
- `PATCH /api/v1/projects/{projectId}`
- `DELETE /api/v1/projects/{projectId}` (soft delete)
- `POST /api/v1/projects/{projectId}/restore`

Requirements:
- `POST /api/v1/projects/{projectId}/requirements`
- `POST /api/v1/projects/{projectId}/requirements/import`
- `GET /api/v1/projects/{projectId}/requirements`
- `GET /api/v1/projects/{projectId}/requirements/{requirementId}`
- `PATCH /api/v1/projects/{projectId}/requirements/{requirementId}`
- `DELETE /api/v1/projects/{projectId}/requirements/{requirementId}` (soft delete)
- `POST /api/v1/projects/{projectId}/requirements/{requirementId}/restore`
- `GET /api/v1/projects/{projectId}/requirements/{requirementId}/coverage`
- `GET /api/v1/projects/{projectId}/traceability-matrix`

Suites/Test Cases:
- `POST /api/v1/projects/{projectId}/suites`
- `GET /api/v1/projects/{projectId}/suites/{suiteId}`
- `PATCH /api/v1/projects/{projectId}/suites/{suiteId}`
- `DELETE /api/v1/projects/{projectId}/suites/{suiteId}` (soft delete)
- `POST /api/v1/projects/{projectId}/suites/{suiteId}/restore`
- `GET /api/v1/projects/{projectId}/suites/tree`
- `POST /api/v1/projects/{projectId}/test-cases`
- `GET /api/v1/projects/{projectId}/test-cases`
- `GET /api/v1/projects/{projectId}/test-cases/{testCaseId}`
- `PATCH /api/v1/projects/{projectId}/test-cases/{testCaseId}`
- `DELETE /api/v1/projects/{projectId}/test-cases/{testCaseId}` (soft delete)
- `POST /api/v1/projects/{projectId}/test-cases/{testCaseId}/restore`
- `POST /api/v1/projects/{projectId}/test-cases/{testCaseId}/versions`
- `PATCH /api/v1/projects/{projectId}/test-cases/bulk`

Planning/Execution:
- `POST /api/v1/projects/{projectId}/builds`
- `GET /api/v1/projects/{projectId}/builds/{buildId}`
- `PATCH /api/v1/projects/{projectId}/builds/{buildId}`
- `DELETE /api/v1/projects/{projectId}/builds/{buildId}` (draft only)
- `POST /api/v1/projects/{projectId}/test-plans`
- `GET /api/v1/projects/{projectId}/test-plans/{planId}`
- `PATCH /api/v1/projects/{projectId}/test-plans/{planId}`
- `DELETE /api/v1/projects/{projectId}/test-plans/{planId}` (draft only)
- `POST /api/v1/projects/{projectId}/test-plans/{planId}/runs`
- `POST /api/v1/executions/{executionId}/status`
- `POST /api/v1/executions/{executionId}/attachments`
- `PATCH /api/v1/builds/{buildId}/close`

Defects:
- `GET /api/v1/projects/{projectId}/defects`
- `GET /api/v1/projects/{projectId}/defects/{defectId}`
- `POST /api/v1/executions/{executionId}/defects/jira`
- `PATCH /api/v1/projects/{projectId}/defects/{defectId}`
- `DELETE /api/v1/projects/{projectId}/defects/{defectId}` (soft delete)
- `POST /api/v1/admin/integrations/jira/config`
- `GET /api/v1/admin/integrations/jira/verify`

Reports:
- `GET /api/v1/projects/{projectId}/metrics`
- `GET /api/v1/projects/{projectId}/reports/coverage?format=csv`
- `GET /api/v1/projects/{projectId}/reports/execution?format=pdf`
- `POST /api/v1/projects/{projectId}/reports/jobs`
- `GET /api/v1/projects/{projectId}/reports/jobs/{jobId}`
- `DELETE /api/v1/projects/{projectId}/reports/jobs/{jobId}`

Audit:
- `GET /api/v1/admin/audit-logs`

---

## 8. Critical Business Rules (from acceptance criteria)

1. Closed build is immutable for execution updates (`409 Conflict`).
2. Failed execution requires `actualResult` + evidence.
3. Guest cannot mutate test plans/builds/test cases (`403 Forbidden`).
4. Editing a test case used in executions must create a new version.
5. Coverage calculation includes only active test cases.
6. External issue creation must be idempotent and retryable.
7. A user may hold multiple roles; effective access equals the union of active grants.
8. Access checks are deny-by-default and scope-aware (`global` or `project`).
9. The platform must prevent deletion/deactivation of the last active `ADMIN`.
10. Role assignment/removal requires authorization and must generate audit events.
11. Project deletion is allowed via soft delete, but blocked if there are active runs/builds.
12. Hard delete is restricted to admin purge workflow and preserves legal/audit retention.
13. Requirement, suite, test case and defect deletions must preserve historical references.
14. Draft-only deletion applies to build/test plan; closed artifacts are immutable.

---

## 9. Quality, Security and Best Practices

- Clean Code with descriptive naming and small cohesive methods
- SOLID across service and use case boundaries
- Input validation with consistent error model (`code`, `field`, `message`, `traceId`)
- JWT + RBAC enforcement on all write endpoints
- Audit for create/update/delete/permission denial
- Retry + backoff for external integrations
- Structured logs + metrics + traces
- Password policy and credential lifecycle controls (rotation, lockout, reset)
- Authorization policy tests for role + scope combinations

---

## 10. Backend Definition of Done (DoD)

1. `ktlint` and `detekt` passing with no blocking issues
2. OpenAPI updated and validated in CI
3. Unit and integration tests covering critical business paths
4. RBAC matrix tested (positive and negative scenarios)
5. Build closure immutability validated by automated test
6. Requirements import with row-level error reporting validated
7. Jira integration tested (success, retry, idempotency)
8. README + architecture docs updated
9. User CRUD fully validated (create/read/update/deactivate/delete)
10. Multi-profile access rules validated (global + project-scoped roles)
11. Last-admin protection and audit log validation covered by automated tests
12. CRUD validation for Projects, Requirements, Suites, Test Cases, Builds, Plans and Defects
13. Delete/restore workflows validated with authorization + retention constraints

## 11)  Tests - Definition of Done (DoD)
 1. Detekt e Ktlint (com zero code smells tolerados), tipagem forte do Kotlin, uso de MockK + Testcontainers para testes de integração, e documentação via KDoc ou OpenAPI/Swagger nativo (ex: Springdoc OpenAPI).
 3. Unit Tests: 100% coverage for critical paths, including RBAC rules and edge cases.
 4. End to End: e2e tests if applicable (web or mobile applications in TypeScript), E2E tests should not contain simulated data.
 5. Language: 100% English codebase.
 6. Documentation: Updated README or inline JSDoc where applicable.
---

## 11. Recommended Improvements

1. Explicit state machines for `Build`, `Execution`, `TestCase`
2. Optimistic locking for bulk operations
3. Attachment quotas and retention policies
4. Automatic build metrics snapshots
5. Saved filters and cursor pagination for large datasets
6. Asynchronous import/export jobs with progress tracking
7. Attribute-based access control (ABAC) for fine-grained rules in future phases
8. Access review workflow (periodic certification of user roles)
9. Recycle bin module for soft-deleted resources with retention countdown and restore action

---

## 12. User and Core Features BDD (CRUD + Access)

### 12.1 Create user with base profile
- **Given** an authenticated `ADMIN`
- **When** calling `POST /api/v1/admin/users` with valid user data
- **Then** the system creates the user in `ACTIVE` status
- **And** stores an audit entry for `USER_CREATED`

### 12.2 Assign multiple profiles to one user
- **Given** an existing active user
- **When** the admin assigns `LEADER` for project `P1` and `TESTER` for project `P2`
- **Then** both role assignments are persisted with scope metadata
- **And** effective permissions are recalculated

### 12.3 Enforce deny-by-default
- **Given** a user without permission to manage builds
- **When** calling a build mutation endpoint
- **Then** the API returns `403 Forbidden`
- **And** records an audit event with reason `INSUFFICIENT_PERMISSION`

### 12.4 Protect last admin
- **Given** there is only one active `ADMIN`
- **When** attempting to deactivate or remove that user role
- **Then** the API rejects with `409 Conflict`
- **And** returns a domain error code `LAST_ADMIN_PROTECTION`

### 12.5 Soft delete user
- **Given** an active user with historical actions
- **When** an admin deletes the user
- **Then** the user is soft-deleted and authentication is blocked
- **And** historical audit records remain intact

### 12.6 Delete project (soft delete)
- **Given** a project without active builds or runs
- **When** an authorized `LEADER` or `ADMIN` calls `DELETE /api/v1/projects/{projectId}`
- **Then** the project is archived (`deletedAt`, `deletedBy`)
- **And** the API returns success and logs `PROJECT_DELETED`

### 12.7 Block project deletion when active run exists
- **Given** a project with at least one active test run
- **When** deletion is requested
- **Then** the API returns `409 Conflict`
- **And** provides domain code `PROJECT_HAS_ACTIVE_RUNS`

### 12.8 Requirement CRUD with restore
- **Given** an authorized user with requirement permissions
- **When** creating, updating and deleting a requirement
- **Then** all operations succeed with audit entries
- **And** deleted requirements can be restored within retention window

### 12.9 Test case delete keeps history
- **Given** a test case already executed in closed builds
- **When** deleting the test case
- **Then** the test case is soft-deleted only
- **And** execution history remains queryable

### 12.10 Delete draft build only
- **Given** a build in `DRAFT` status
- **When** an authorized user deletes it
- **Then** deletion succeeds
- **But** if status is `OPEN` or `CLOSED`, the API rejects with `409 Conflict`

### 12.11 Defect CRUD access policy
- **Given** a `TESTER` with project access
- **When** creating or updating a defect in the same project
- **Then** operation is allowed
- **And** deletion requires `LEADER` or `ADMIN`

---

## 13. Delivery Roadmap (30/60/90 days)

- **30 days**: foundation, security, RBAC, **user CRUD + access management**, domain model, test case module
- **60 days**: requirements traceability + planning + execution
- **90 days**: defect integration + reporting + hardening

---

## 14. Development Status Update (as-is codebase)

> Updated based on current implementation in `backend-kotlin` (reference snapshot: 2026-04-12).

### 14.1 Implementation status (done vs pending)

| Module | Status | What is implemented | What is still pending / partial |
|---|---|---|---|
| Identity & RBAC | **Partially done** | Auth endpoints, user self endpoint, admin user CRUD, role assignment/removal, effective permissions, last-admin protection, method-level role guards with `@PreAuthorize`. | Auth is MVP (fixed bearer tokens), no real JWT signing/expiration/rotation pipeline, scoped role assignment exists but authorization is still mainly role-based by token principal. |
| Projects | **Partially done** | Create/list/get/update, soft delete (`ARCHIVED` + `deletedAt/deletedBy`), restore, deletion blocked by active builds/runs. | Hard delete purge workflow not implemented; member management exists at service level but is not exposed in v1 endpoints. |
| Requirements & Traceability | **Partially done** | CRUD + restore, import endpoint, coverage endpoint, traceability matrix endpoint. | Import currently returns simplified result (`errors=0`), no row-level error model; no explicit test-case link entity/rules yet. |
| Suites & Test Cases | **Mostly done (MVP)** | Suite CRUD + restore + tree; test case CRUD + restore + bulk edit + search + version creation. | Versioning rule depends on `executedBefore` flag in memory model; no persistent relational version chain yet. |
| Planning, Builds & Execution | **Mostly done (MVP)** | Build/plan CRUD (with draft-only deletion), run creation, execution status/evidence endpoints, close build, immutable closed build for updates, FAILED requires `actualResult` + evidence. | No explicit state machine abstraction yet (states are enum + guards in services). |
| Defects & Jira integration | **Partially done** | Defect CRUD flows, create defect from execution, Jira config + verify endpoints present. | Jira integration is still stubbed/simplified (no real API calls, idempotency keys, or resilient retry orchestration). |
| Reports & Metrics | **Partially done** | Operational metrics endpoint, coverage/execution report endpoints, report job lifecycle endpoints. | CSV/PDF export content is placeholder; report jobs are in-memory and not asynchronous with durable queue. |
| Audit & Compliance | **Partially done** | Audit records for create/update/delete actions and audit listing endpoint. | No dedicated interceptor yet; deny events are not automatically persisted in all forbidden flows. |
| Quality & Validation | **Done (for current scope)** | Contract mapping test, OpenAPI validation test, security policy tests, business rules tests, API integration suites by feature, Testcontainers smoke test, detekt/ktlint configured. | CI evidence and historical quality trend are outside this document scope. |

### 14.2 Libraries and versions (current) + rationale

Source of truth: `backend-kotlin/build.gradle.kts`.

| Library / Tool | Planned baseline | Current version in project | Why this version / change rationale |
|---|---|---|---|
| Kotlin JVM + Spring + JPA plugins | Kotlin 2.x | `2.0.21` | Locked to stable Kotlin 2.x line for strong typing, modern compiler features, and Spring/Kotlin plugin compatibility. |
| Spring Boot | 3.3+ | `3.5.6` | Kept above baseline to stay on newer Spring Boot patch line (security/bugfix cadence and platform stability). |
| Spring Dependency Management | n/a | `1.1.7` | Standard BOM alignment for consistent transitive dependency resolution in Gradle Kotlin DSL builds. |
| Java toolchain | Java 23 | Toolchain `23` + target/source `21` | Compile/runtime target remains broadly compatible (`21`) while preserving modern JDK toolchain (`23`) for development/build consistency. |
| Springdoc OpenAPI | OpenAPI 3 required | `2.8.3` | Added to expose/validate OpenAPI docs via Spring Web MVC and keep API discoverability in Swagger UI. |
| Flyway | Required | `flyway-core` + `flyway-database-postgresql` (managed by BOM) | Supports migration workflow for PostgreSQL environments. |
| Detekt Gradle plugin | Required | `1.23.8` | Static analysis gate for Kotlin code quality in DoD. |
| Ktlint Gradle plugin | Required | `12.2.0` | Formatting/lint consistency gate in CI/local workflow. |
| Testcontainers | Required | `1.20.2` | Realistic DB smoke/integration checks with PostgreSQL containerized environment. |
| MockK | Required | `1.13.13` | Kotlin-first mocking approach for tests. |
| Mockito Kotlin | Optional support | `5.4.0` | Keeps interoperability with existing mock style and mixed test strategies. |
| Rest-Assured | n/a in plan details | `5.5.0` | Real HTTP-level API assertions for integration suites. |
| Allure plugin + JUnit adapter | n/a in baseline | Plugin `2.12.0`, adapter/report `2.30.0` | Added to produce richer automated test execution reports and support QA evidence. |
| DataFaker | n/a in baseline | `2.4.2` | Generates dynamic test data for more robust integration scenarios. |

### 14.3 Planned technologies not fully implemented yet

1. **Redis cache**: planned in architecture, not yet wired in runtime dependencies/config.
2. **Micrometer + OpenTelemetry tracing**: Actuator is present, but full telemetry/tracing instrumentation is still pending.
3. **Resilience4j + RestClient strategy for Jira**: still pending; current Jira flow is simplified.
4. **MDC contextual logging (`userId`, `projectId`)**: not yet implemented end-to-end in request filter/log pattern.
5. **Persistent modular layers (`application/domain/infrastructure`)**: current implementation is feature-oriented with in-memory `StateStore`; persistence/repository layering is not complete yet.

### 14.4 Current implementation logic (high-level)

1. **Authentication/Authorization flow**
	- Incoming bearer token is resolved in `JwtTokenAuthenticationFilter` and mapped to `TokenPrincipal` roles.
	- Endpoint access is enforced by `@PreAuthorize` role checks.
	- Domain/service-level protections complement endpoint-level checks (for example, last-admin and immutable states).

2. **Critical business-rule guards**
	- **Last admin protection** in `UserService`/`AccessService` blocks delete/deactivate/remove-role operations that would remove the final active global admin.
	- **Build immutability**: once closed, updates/execution mutation are blocked with conflict domain errors.
	- **Execution FAILED policy**: FAILED status requires both `actualResult` and evidence attachment.
	- **Draft-only deletions**: build/test plan deletion only allowed in draft status.

3. **Traceability lifecycle (MVP shape)**
	- Requirement/suite/test-case/defect entities use soft-delete semantics for history safety.
	- Test case versioning creates a new record when prior execution history is present (`executedBefore` guard).
	- Report and metrics APIs expose operational summaries and job orchestration in a simplified in-memory model.

4. **Audit and error model**
	- Services emit create/update/delete audit events to a centralized store.
	- Exceptions are normalized by `GlobalExceptionHandler` using a consistent envelope (`code`, `field`, `message`, `traceId`).

### 14.5 Next recommended execution order (to close MVP gaps)

1. Implement real JWT lifecycle (issuer/signature/expiration/refresh rotation + revocation strategy).
2. Replace in-memory `StateStore` with repository + persistence model (PostgreSQL/JPA/Flyway-backed).
3. Add Resilience4j + RestClient for Jira integration with idempotency keys and retry/backoff policies.
4. Implement MDC request context propagation (`traceId`, `userId`, `projectId`) and structured logs.
5. Add real report generation (CSV/PDF) and asynchronous durable job processing.
