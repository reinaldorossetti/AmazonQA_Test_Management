package com.amazonqa.seed

import com.amazonqa.security.Role
import com.amazonqa.security.ScopeType
import com.amazonqa.store.AuditRecord
import com.amazonqa.store.BuildRecord
import com.amazonqa.store.BuildStatus
import com.amazonqa.store.DefectRecord
import com.amazonqa.store.DefectStatus
import com.amazonqa.store.ExecutionRecord
import com.amazonqa.store.ExecutionStatus
import com.amazonqa.store.PlanRecord
import com.amazonqa.store.PlanStatus
import com.amazonqa.store.ProjectRecord
import com.amazonqa.store.ProjectStatus
import com.amazonqa.store.ReportJobRecord
import com.amazonqa.store.RequirementRecord
import com.amazonqa.store.RoleAssignment
import com.amazonqa.store.StateStore
import com.amazonqa.store.SuiteRecord
import com.amazonqa.store.TestCaseRecord
import com.amazonqa.store.UserRecord
import com.amazonqa.store.UserStatus
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Component
class SeedDataRunner(
    private val stateStore: StateStore,
    private val objectMapper: ObjectMapper,
    private val jdbcTemplateProvider: ObjectProvider<JdbcTemplate>,
    @Value("\${seed.enabled:false}") private val seedEnabled: Boolean,
    @Value("\${seed.reset-on-startup:false}") private val seedResetOnStartup: Boolean,
    @Value("\${seed.strict-validation:true}") private val strictValidation: Boolean,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        if (!seedEnabled) {
            logger.info("SeedDataRunner disabled (seed.enabled=false)")
            return
        }

        val jdbcTemplate = jdbcTemplateProvider.ifAvailable
        val seedData = loadSeedData()

        logger.info("Starting seed process. resetOnStartup={}, strictValidation={}", seedResetOnStartup, strictValidation)

        if (seedResetOnStartup) {
            resetStateStore()
            if (jdbcTemplate != null) {
                resetPostgres(jdbcTemplate)
            } else {
                logger.warn("JdbcTemplate is unavailable. PostgreSQL reset was skipped.")
            }
        }

        seedStateStore(seedData)
        if (jdbcTemplate != null) {
            seedPostgres(jdbcTemplate, seedData)
        } else {
            logger.warn("JdbcTemplate is unavailable. PostgreSQL seeding was skipped.")
        }

        val failures = mutableListOf<String>()
        failures.addAll(validateStateStore(seedData))
        if (jdbcTemplate != null) {
            failures.addAll(validatePostgres(jdbcTemplate, seedData))
        }

        if (failures.isNotEmpty()) {
            val message = "Seed validation failed: ${failures.joinToString(" | ")}"
            if (strictValidation) {
                throw IllegalStateException(message)
            }
            logger.warn(message)
            return
        }

        logger.info(
            "Seed process finished successfully. stateStore(users={}, projects={}, builds={}, plans={}, executions={})",
            stateStore.users.size,
            stateStore.projects.size,
            stateStore.builds.size,
            stateStore.plans.size,
            stateStore.executions.size,
        )
    }

    private fun loadSeedData(): SeedDataBundle =
        SeedDataBundle(
            users = readSeedList("seed/users.json"),
            projects = readSeedList("seed/projects.json"),
            requirements = readSeedList("seed/requirements.json"),
            suites = readSeedList("seed/suites.json"),
            testCases = readSeedList("seed/test_cases.json"),
            builds = readSeedList("seed/builds.json"),
            testPlans = readSeedList("seed/test_plans.json"),
            executions = readSeedList("seed/executions.json"),
            defects = readSeedList("seed/defects.json"),
            reportJobs = readSeedList("seed/report_jobs.json"),
            auditLogs = readSeedList("seed/audit_logs.json"),
        )

    private inline fun <reified T> readSeedList(path: String): List<T> {
        val resource = ClassPathResource(path)
        if (!resource.exists()) {
            logger.warn("Seed resource not found: {}", path)
            return emptyList()
        }
        return resource.inputStream.use { input -> objectMapper.readValue(input) }
    }

    private fun resetStateStore() {
        stateStore.users.clear()
        stateStore.projects.clear()
        stateStore.requirements.clear()
        stateStore.suites.clear()
        stateStore.testCases.clear()
        stateStore.builds.clear()
        stateStore.plans.clear()
        stateStore.executions.clear()
        stateStore.defects.clear()
        stateStore.reportJobs.clear()
        stateStore.audits.clear()
        logger.info("StateStore reset completed.")
    }

    private fun resetPostgres(jdbcTemplate: JdbcTemplate) {
        jdbcTemplate.execute("TRUNCATE TABLE test_cases, executions, builds, test_plans, audit_logs, projects, users CASCADE")
        logger.info("PostgreSQL reset completed (TRUNCATE CASCADE).")
    }

    private fun seedStateStore(seedData: SeedDataBundle) {
        seedData.users.forEach { row ->
            val id = row.id.toUuid()
            stateStore.users[id] =
                UserRecord(
                    id = id,
                    fullName = row.fullName,
                    email = row.email,
                    status = row.status.toUserStatus(),
                    createdAt = row.createdAt.toInstant(),
                    updatedAt = row.updatedAt.toInstant(),
                    lastLoginAt = row.lastLoginAt?.toInstant(),
                    roleAssignments = row.roleAssignments.map { it.toRoleAssignment() }.toMutableSet(),
                    preferences = row.preferences.toMutableMap(),
                )
        }

        seedData.projects.forEach { row ->
            val id = row.id.toUuid()
            stateStore.projects[id] =
                ProjectRecord(
                    id = id,
                    name = row.name,
                    status = row.status.toProjectStatus(),
                    createdAt = row.createdAt.toInstant(),
                    updatedAt = row.updatedAt.toInstant(),
                    deletedAt = row.deletedAt?.toInstant(),
                    deletedBy = row.deletedBy,
                )
        }

        seedData.requirements.forEach { row ->
            val id = row.id.toUuid()
            stateStore.requirements[id] =
                RequirementRecord(
                    id = id,
                    projectId = row.projectId.toUuid(),
                    title = row.title,
                    deletedAt = row.deletedAt?.toInstant(),
                )
        }

        seedData.suites.forEach { row ->
            val id = row.id.toUuid()
            stateStore.suites[id] =
                SuiteRecord(
                    id = id,
                    projectId = row.projectId.toUuid(),
                    name = row.name,
                    deletedAt = row.deletedAt?.toInstant(),
                )
        }

        seedData.testCases.forEach { row ->
            val id = row.id.toUuid()
            stateStore.testCases[id] =
                TestCaseRecord(
                    id = id,
                    projectId = row.projectId.toUuid(),
                    title = row.title,
                    testId = row.testId,
                    priority = row.priority,
                    bugSeverity = row.bugSeverity,
                    tagsKeywords = row.tagsKeywords,
                    requirementLink = row.requirementLink,
                    executionType = row.executionType,
                    testCaseStatus = row.testCaseStatus,
                    platform = row.platform,
                    testEnvironment = row.testEnvironment,
                    preconditions = row.preconditions,
                    actions = row.actions,
                    expectedResult = row.expectedResult,
                    executionStatus = row.executionStatus,
                    notes = row.notes,
                    attachments = row.attachments,
                    version = row.version,
                    deletedAt = row.deletedAt?.toInstant(),
                    executedBefore = row.executedBefore,
                )
        }

        seedData.builds.forEach { row ->
            val id = row.id.toUuid()
            stateStore.builds[id] =
                BuildRecord(
                    id = id,
                    projectId = row.projectId.toUuid(),
                    name = row.name,
                    status = row.status.toBuildStatus(),
                    createdAt = row.createdAt.toInstant(),
                    updatedAt = row.updatedAt.toInstant(),
                )
        }

        seedData.testPlans.forEach { row ->
            val id = row.id.toUuid()
            stateStore.plans[id] =
                PlanRecord(
                    id = id,
                    projectId = row.projectId.toUuid(),
                    name = row.name,
                    status = row.status.toPlanStatus(),
                    createdAt = row.createdAt.toInstant(),
                    updatedAt = row.updatedAt.toInstant(),
                )
        }

        seedData.executions.forEach { row ->
            val id = row.id.toUuid()
            stateStore.executions[id] =
                ExecutionRecord(
                    id = id,
                    projectId = row.projectId.toUuid(),
                    buildId = row.buildId.toUuid(),
                    status = row.status.toExecutionStatus(),
                    actualResult = row.actualResult,
                    evidenceUrl = row.evidenceUrl,
                    createdAt = row.createdAt.toInstant(),
                    updatedAt = row.updatedAt.toInstant(),
                )
        }

        seedData.defects.forEach { row ->
            val id = row.id.toUuid()
            stateStore.defects[id] =
                DefectRecord(
                    id = id,
                    projectId = row.projectId.toUuid(),
                    executionId = row.executionId.toUuid(),
                    title = row.title,
                    status = row.status.toDefectStatus(),
                    deletedAt = row.deletedAt?.toInstant(),
                )
        }

        seedData.reportJobs.forEach { row ->
            val id = row.id.toUuid()
            stateStore.reportJobs[id] =
                ReportJobRecord(
                    id = id,
                    projectId = row.projectId.toUuid(),
                    type = row.type,
                    status = row.status,
                )
        }

        seedData.auditLogs.forEach { row ->
            stateStore.audits.add(
                AuditRecord(
                    id = row.id.toUuid(),
                    eventType = row.eventType,
                    actor = row.actor,
                    entityType = row.entityType,
                    entityId = row.entityId,
                    metadata = row.metadata ?: "",
                    createdAt = row.createdAt.toInstant(),
                ),
            )
        }

        logger.info("StateStore seeded from JSON resources.")
    }

    private fun seedPostgres(
        jdbcTemplate: JdbcTemplate,
        seedData: SeedDataBundle,
    ) {
        seedData.users.forEach { row ->
            jdbcTemplate.update(
                """
                INSERT INTO users (id, full_name, email, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    full_name = EXCLUDED.full_name,
                    email = EXCLUDED.email,
                    status = EXCLUDED.status,
                    created_at = EXCLUDED.created_at,
                    updated_at = EXCLUDED.updated_at
                """.trimIndent(),
                row.id.toUuid(),
                row.fullName,
                row.email,
                row.status,
                row.createdAt.toTimestamp(),
                row.updatedAt.toTimestamp(),
            )
        }

        seedData.projects.forEach { row ->
            jdbcTemplate.update(
                """
                INSERT INTO projects (id, name, status, deleted_at, deleted_by, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    status = EXCLUDED.status,
                    deleted_at = EXCLUDED.deleted_at,
                    deleted_by = EXCLUDED.deleted_by,
                    created_at = EXCLUDED.created_at,
                    updated_at = EXCLUDED.updated_at
                """.trimIndent(),
                row.id.toUuid(),
                row.name,
                row.status,
                row.deletedAt?.toTimestamp(),
                row.deletedBy,
                row.createdAt.toTimestamp(),
                row.updatedAt.toTimestamp(),
            )
        }

        seedData.builds.forEach { row ->
            jdbcTemplate.update(
                """
                INSERT INTO builds (id, project_id, name, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    project_id = EXCLUDED.project_id,
                    name = EXCLUDED.name,
                    status = EXCLUDED.status,
                    created_at = EXCLUDED.created_at,
                    updated_at = EXCLUDED.updated_at
                """.trimIndent(),
                row.id.toUuid(),
                row.projectId.toUuid(),
                row.name,
                row.status,
                row.createdAt.toTimestamp(),
                row.updatedAt.toTimestamp(),
            )
        }

        seedData.testCases.forEach { row ->
            jdbcTemplate.update(
                """
                INSERT INTO test_cases (
                    id,
                    project_id,
                    test_id,
                    title,
                    priority,
                    bug_severity,
                    tags_keywords,
                    requirement_link,
                    execution_type,
                    test_case_status,
                    platform,
                    test_environment,
                    preconditions,
                    actions,
                    expected_result,
                    execution_status,
                    notes,
                    attachments,
                    version,
                    deleted_at,
                    executed_before,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                ON CONFLICT (id) DO UPDATE SET
                    project_id = EXCLUDED.project_id,
                    test_id = EXCLUDED.test_id,
                    title = EXCLUDED.title,
                    priority = EXCLUDED.priority,
                    bug_severity = EXCLUDED.bug_severity,
                    tags_keywords = EXCLUDED.tags_keywords,
                    requirement_link = EXCLUDED.requirement_link,
                    execution_type = EXCLUDED.execution_type,
                    test_case_status = EXCLUDED.test_case_status,
                    platform = EXCLUDED.platform,
                    test_environment = EXCLUDED.test_environment,
                    preconditions = EXCLUDED.preconditions,
                    actions = EXCLUDED.actions,
                    expected_result = EXCLUDED.expected_result,
                    execution_status = EXCLUDED.execution_status,
                    notes = EXCLUDED.notes,
                    attachments = EXCLUDED.attachments,
                    version = EXCLUDED.version,
                    deleted_at = EXCLUDED.deleted_at,
                    executed_before = EXCLUDED.executed_before,
                    updated_at = NOW()
                """.trimIndent(),
                row.id.toUuid(),
                row.projectId.toUuid(),
                row.testId,
                row.title,
                row.priority,
                row.bugSeverity,
                row.tagsKeywords,
                row.requirementLink,
                row.executionType,
                row.testCaseStatus,
                row.platform,
                row.testEnvironment,
                row.preconditions,
                row.actions,
                row.expectedResult,
                row.executionStatus,
                row.notes,
                row.attachments,
                row.version,
                row.deletedAt?.toTimestamp(),
                row.executedBefore,
            )
        }

        seedData.testPlans.forEach { row ->
            jdbcTemplate.update(
                """
                INSERT INTO test_plans (id, project_id, name, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    project_id = EXCLUDED.project_id,
                    name = EXCLUDED.name,
                    status = EXCLUDED.status,
                    created_at = EXCLUDED.created_at,
                    updated_at = EXCLUDED.updated_at
                """.trimIndent(),
                row.id.toUuid(),
                row.projectId.toUuid(),
                row.name,
                row.status,
                row.createdAt.toTimestamp(),
                row.updatedAt.toTimestamp(),
            )
        }

        seedData.executions.forEach { row ->
            jdbcTemplate.update(
                """
                INSERT INTO executions (id, build_id, status, actual_result, evidence_url, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    build_id = EXCLUDED.build_id,
                    status = EXCLUDED.status,
                    actual_result = EXCLUDED.actual_result,
                    evidence_url = EXCLUDED.evidence_url,
                    created_at = EXCLUDED.created_at,
                    updated_at = EXCLUDED.updated_at
                """.trimIndent(),
                row.id.toUuid(),
                row.buildId.toUuid(),
                row.status,
                row.actualResult,
                row.evidenceUrl,
                row.createdAt.toTimestamp(),
                row.updatedAt.toTimestamp(),
            )
        }

        seedData.auditLogs.forEach { row ->
            jdbcTemplate.update(
                """
                INSERT INTO audit_logs (id, event_type, actor, entity_type, entity_id, metadata, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    event_type = EXCLUDED.event_type,
                    actor = EXCLUDED.actor,
                    entity_type = EXCLUDED.entity_type,
                    entity_id = EXCLUDED.entity_id,
                    metadata = EXCLUDED.metadata,
                    created_at = EXCLUDED.created_at
                """.trimIndent(),
                row.id.toUuid(),
                row.eventType,
                row.actor,
                row.entityType,
                row.entityId,
                row.metadata,
                row.createdAt.toTimestamp(),
            )
        }

        logger.info("PostgreSQL seeded from JSON resources.")
    }

    private fun validateStateStore(seedData: SeedDataBundle): List<String> {
        val failures = mutableListOf<String>()

        seedData.users.forEach { if (!stateStore.users.containsKey(it.id.toUuid())) failures.add("StateStore missing user ${it.id}") }
        seedData.projects.forEach { if (!stateStore.projects.containsKey(it.id.toUuid())) failures.add("StateStore missing project ${it.id}") }
        seedData.requirements.forEach { if (!stateStore.requirements.containsKey(it.id.toUuid())) failures.add("StateStore missing requirement ${it.id}") }
        seedData.suites.forEach { if (!stateStore.suites.containsKey(it.id.toUuid())) failures.add("StateStore missing suite ${it.id}") }
        seedData.testCases.forEach { if (!stateStore.testCases.containsKey(it.id.toUuid())) failures.add("StateStore missing test case ${it.id}") }
        seedData.builds.forEach { if (!stateStore.builds.containsKey(it.id.toUuid())) failures.add("StateStore missing build ${it.id}") }
        seedData.testPlans.forEach { if (!stateStore.plans.containsKey(it.id.toUuid())) failures.add("StateStore missing test plan ${it.id}") }
        seedData.executions.forEach { if (!stateStore.executions.containsKey(it.id.toUuid())) failures.add("StateStore missing execution ${it.id}") }
        seedData.defects.forEach { if (!stateStore.defects.containsKey(it.id.toUuid())) failures.add("StateStore missing defect ${it.id}") }
        seedData.reportJobs.forEach { if (!stateStore.reportJobs.containsKey(it.id.toUuid())) failures.add("StateStore missing report job ${it.id}") }

        return failures
    }

    private fun validatePostgres(
        jdbcTemplate: JdbcTemplate,
        seedData: SeedDataBundle,
    ): List<String> {
        val failures = mutableListOf<String>()

        validateTableCount(jdbcTemplate, "users", seedData.users.size, failures)
        validateTableCount(jdbcTemplate, "projects", seedData.projects.size, failures)
        validateTableCount(jdbcTemplate, "test_cases", seedData.testCases.size, failures)
        validateTableCount(jdbcTemplate, "builds", seedData.builds.size, failures)
        validateTableCount(jdbcTemplate, "test_plans", seedData.testPlans.size, failures)
        validateTableCount(jdbcTemplate, "executions", seedData.executions.size, failures)
        validateTableCount(jdbcTemplate, "audit_logs", seedData.auditLogs.size, failures)

        seedData.users.forEach { if (!existsById(jdbcTemplate, "users", it.id.toUuid())) failures.add("PostgreSQL missing user ${it.id}") }
        seedData.projects.forEach { if (!existsById(jdbcTemplate, "projects", it.id.toUuid())) failures.add("PostgreSQL missing project ${it.id}") }
        seedData.testCases.forEach { if (!existsById(jdbcTemplate, "test_cases", it.id.toUuid())) failures.add("PostgreSQL missing test case ${it.id}") }
        seedData.builds.forEach { if (!existsById(jdbcTemplate, "builds", it.id.toUuid())) failures.add("PostgreSQL missing build ${it.id}") }
        seedData.testPlans.forEach { if (!existsById(jdbcTemplate, "test_plans", it.id.toUuid())) failures.add("PostgreSQL missing test plan ${it.id}") }
        seedData.executions.forEach { if (!existsById(jdbcTemplate, "executions", it.id.toUuid())) failures.add("PostgreSQL missing execution ${it.id}") }
        seedData.auditLogs.forEach { if (!existsById(jdbcTemplate, "audit_logs", it.id.toUuid())) failures.add("PostgreSQL missing audit log ${it.id}") }

        return failures
    }

    private fun validateTableCount(
        jdbcTemplate: JdbcTemplate,
        table: String,
        expectedMinimum: Int,
        failures: MutableList<String>,
    ) {
        if (expectedMinimum <= 0) {
            return
        }
        val count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM $table", Long::class.java) ?: 0L
        if (count < expectedMinimum.toLong()) {
            failures.add("PostgreSQL table '$table' has $count rows, expected at least $expectedMinimum")
        }
    }

    private fun existsById(
        jdbcTemplate: JdbcTemplate,
        table: String,
        id: UUID,
    ): Boolean {
        val count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM $table WHERE id = ?", Long::class.java, id) ?: 0L
        return count > 0
    }
}

private fun String.toUuid(): UUID = UUID.fromString(this)

private fun String.toInstant(): Instant = Instant.parse(this)

private fun String.toTimestamp(): Timestamp = Timestamp.from(this.toInstant())

private fun RoleAssignmentSeed.toRoleAssignment(): RoleAssignment =
    RoleAssignment(
        role = Role.valueOf(role),
        scopeType = ScopeType.valueOf(scopeType),
        scopeId = scopeId?.toUuid(),
        assignedAt = assignedAt?.toInstant() ?: Instant.now(),
    )

private fun String.toUserStatus(): UserStatus = UserStatus.valueOf(this)

private fun String.toProjectStatus(): ProjectStatus = ProjectStatus.valueOf(this)

private fun String.toBuildStatus(): BuildStatus = BuildStatus.valueOf(this)

private fun String.toPlanStatus(): PlanStatus = PlanStatus.valueOf(this)

private fun String.toExecutionStatus(): ExecutionStatus = ExecutionStatus.valueOf(this)

private fun String.toDefectStatus(): DefectStatus = DefectStatus.valueOf(this)

data class SeedDataBundle(
    val users: List<UserSeed>,
    val projects: List<ProjectSeed>,
    val requirements: List<RequirementSeed>,
    val suites: List<SuiteSeed>,
    val testCases: List<TestCaseSeed>,
    val builds: List<BuildSeed>,
    val testPlans: List<TestPlanSeed>,
    val executions: List<ExecutionSeed>,
    val defects: List<DefectSeed>,
    val reportJobs: List<ReportJobSeed>,
    val auditLogs: List<AuditLogSeed>,
)

data class RoleAssignmentSeed(
    val role: String,
    val scopeType: String,
    val scopeId: String? = null,
    val assignedAt: String? = null,
)

data class UserSeed(
    val id: String,
    val fullName: String,
    val email: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val lastLoginAt: String? = null,
    val roleAssignments: List<RoleAssignmentSeed> = emptyList(),
    val preferences: Map<String, String> = emptyMap(),
)

data class ProjectSeed(
    val id: String,
    val name: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val deletedBy: String? = null,
)

data class RequirementSeed(
    val id: String,
    val projectId: String,
    val title: String,
    val deletedAt: String? = null,
)

data class SuiteSeed(
    val id: String,
    val projectId: String,
    val name: String,
    val deletedAt: String? = null,
)

data class TestCaseSeed(
    val id: String,
    val projectId: String,
    val testId: String,
    val title: String,
    val priority: String = "Medium",
    val bugSeverity: String = "Major",
    val tagsKeywords: String? = null,
    val requirementLink: String? = null,
    val executionType: String = "Manual",
    val testCaseStatus: String = "Draft",
    val platform: String? = null,
    val testEnvironment: String? = null,
    val preconditions: String? = null,
    val actions: String? = null,
    val expectedResult: String? = null,
    val executionStatus: String = "Not Run",
    val notes: String? = null,
    val attachments: String? = null,
    val version: Int,
    val deletedAt: String? = null,
    val executedBefore: Boolean = false,
)

data class BuildSeed(
    val id: String,
    val projectId: String,
    val name: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
)

data class TestPlanSeed(
    val id: String,
    val projectId: String,
    val name: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
)

data class ExecutionSeed(
    val id: String,
    val projectId: String,
    val buildId: String,
    val status: String,
    val actualResult: String? = null,
    val evidenceUrl: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

data class DefectSeed(
    val id: String,
    val projectId: String,
    val executionId: String,
    val title: String,
    val status: String,
    val deletedAt: String? = null,
)

data class ReportJobSeed(
    val id: String,
    val projectId: String,
    val type: String,
    val status: String,
)

data class AuditLogSeed(
    val id: String,
    val eventType: String,
    val actor: String,
    val entityType: String,
    val entityId: String,
    val metadata: String? = null,
    val createdAt: String,
)
