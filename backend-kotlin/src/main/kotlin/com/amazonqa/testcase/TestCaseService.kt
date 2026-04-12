package com.amazonqa.testcase

import com.amazonqa.audit.AuditService
import com.amazonqa.common.exception.DomainException
import com.amazonqa.store.StateStore
import com.amazonqa.store.TestCaseRecord
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

data class CreateTestCasePayload(
    val title: String,
    val testId: String? = null,
    val priority: String? = null,
    val bugSeverity: String? = null,
    val tagsKeywords: String? = null,
    val requirementLink: String? = null,
    val executionType: String? = null,
    val testCaseStatus: String? = null,
    val platform: String? = null,
    val testEnvironment: String? = null,
    val preconditions: String? = null,
    val actions: String? = null,
    val expectedResult: String? = null,
    val actualResult: String? = null,
    val executionStatus: String? = null,
    val notes: String? = null,
    val customFields: String? = null,
    val attachments: String? = null,
)

@Service
class TestCaseService(
    private val stateStore: StateStore,
    private val auditService: AuditService,
    private val jdbcTemplateProvider: ObjectProvider<JdbcTemplate>,
) {
    fun createTestCase(
        projectId: UUID,
        payload: CreateTestCasePayload,
    ): TestCaseRecord {
        ensureProject(projectId)
        val generatedTestId = payload.testId ?: nextTestId(projectId)
        val testCase =
            TestCaseRecord(
                id = UUID.randomUUID(),
                projectId = projectId,
                title = payload.title,
                testId = generatedTestId,
                priority = payload.priority ?: "Medium",
                bugSeverity = payload.bugSeverity ?: "Major",
                tagsKeywords = payload.tagsKeywords,
                requirementLink = payload.requirementLink,
                executionType = payload.executionType ?: "Manual",
                testCaseStatus = payload.testCaseStatus ?: "Draft",
                platform = payload.platform,
                testEnvironment = payload.testEnvironment,
                preconditions = payload.preconditions,
                actions = payload.actions,
                expectedResult = payload.expectedResult,
                actualResult = payload.actualResult,
                executionStatus = payload.executionStatus ?: "Not Run",
                notes = payload.notes,
                customFields = payload.customFields,
                attachments = payload.attachments,
                version = 1,
            )
        stateStore.testCases[testCase.id] = testCase
        upsertTestCaseInPostgres(testCase)
        auditService.logCreate("TEST_CASE", testCase.id.toString(), "TEST_CASE_CREATED")
        return testCase
    }

    fun listTestCases(projectId: UUID): List<TestCaseRecord> =
        stateStore.testCases.values.filter { it.projectId == projectId && it.deletedAt == null }

    fun getTestCase(testCaseId: UUID): TestCaseRecord =
        stateStore.testCases[testCaseId]
            ?: throw DomainException(HttpStatus.NOT_FOUND, "TEST_CASE_NOT_FOUND", "Test case not found")

    fun updateTestCase(
        testCaseId: UUID,
        title: String?,
    ): TestCaseRecord {
        val testCase = getTestCase(testCaseId)
        if (testCase.executedBefore) {
            return createNewVersion(testCaseId, title ?: testCase.title)
        }
        title?.let { testCase.title = it }
        upsertTestCaseInPostgres(testCase)
        auditService.logUpdate("TEST_CASE", testCase.id.toString(), "TEST_CASE_UPDATED")
        return testCase
    }

    fun createNewVersion(
        testCaseId: UUID,
        title: String,
    ): TestCaseRecord {
        val old = getTestCase(testCaseId)
        val newVersion =
            old.copy(
                id = UUID.randomUUID(),
                title = title,
                version = old.version + 1,
                deletedAt = null,
                executedBefore = false,
            )
        stateStore.testCases[newVersion.id] = newVersion
        upsertTestCaseInPostgres(newVersion)
        auditService.logCreate("TEST_CASE", newVersion.id.toString(), "TEST_CASE_VERSION_CREATED")
        return newVersion
    }

    fun archiveTestCase(testCaseId: UUID): TestCaseRecord = deleteTestCase(testCaseId)

    fun deleteTestCase(testCaseId: UUID): TestCaseRecord {
        val testCase = getTestCase(testCaseId)
        testCase.deletedAt = Instant.now()
        upsertTestCaseInPostgres(testCase)
        auditService.logDelete("TEST_CASE", testCase.id.toString(), "TEST_CASE_DELETED")
        return testCase
    }

    fun restoreTestCase(testCaseId: UUID): TestCaseRecord {
        val testCase = getTestCase(testCaseId)
        testCase.deletedAt = null
        upsertTestCaseInPostgres(testCase)
        auditService.logUpdate("TEST_CASE", testCase.id.toString(), "TEST_CASE_RESTORED")
        return testCase
    }

    fun bulkEdit(
        projectId: UUID,
        titleSuffix: String,
    ): List<TestCaseRecord> {
        val items = listTestCases(projectId)
        items.forEach {
            it.title = "${it.title}$titleSuffix"
            upsertTestCaseInPostgres(it)
        }
        auditService.logUpdate("TEST_CASE", projectId.toString(), "TEST_CASE_BULK_EDIT")
        return items
    }

    fun search(
        projectId: UUID,
        query: String,
    ): List<TestCaseRecord> = listTestCases(projectId).filter { it.title.contains(query, ignoreCase = true) }

    private fun ensureProject(projectId: UUID) {
        if (!stateStore.projects.containsKey(projectId)) {
            throw DomainException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found")
        }
    }

    private fun nextTestId(projectId: UUID): String {
        val next = stateStore.testCases.values.count { it.projectId == projectId } + 1
        return "AMQA-${next.toString().padStart(3, '0')}"
    }

    private fun upsertTestCaseInPostgres(testCase: TestCaseRecord) {
        val jdbcTemplate = jdbcTemplateProvider.ifAvailable ?: return
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
                actual_result,
                execution_status,
                notes,
                custom_fields,
                attachments,
                version,
                deleted_at,
                executed_before,
                updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, NOW())
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
                actual_result = EXCLUDED.actual_result,
                execution_status = EXCLUDED.execution_status,
                notes = EXCLUDED.notes,
                custom_fields = EXCLUDED.custom_fields,
                attachments = EXCLUDED.attachments,
                version = EXCLUDED.version,
                deleted_at = EXCLUDED.deleted_at,
                executed_before = EXCLUDED.executed_before,
                updated_at = NOW()
            """.trimIndent(),
            testCase.id,
            testCase.projectId,
            testCase.testId,
            testCase.title,
            testCase.priority,
            testCase.bugSeverity,
            testCase.tagsKeywords,
            testCase.requirementLink,
            testCase.executionType,
            testCase.testCaseStatus,
            testCase.platform,
            testCase.testEnvironment,
            testCase.preconditions,
            testCase.actions,
            testCase.expectedResult,
            testCase.actualResult,
            testCase.executionStatus,
            testCase.notes,
            testCase.customFields,
            testCase.attachments,
            testCase.version,
            testCase.deletedAt,
            testCase.executedBefore,
        )
    }
}
