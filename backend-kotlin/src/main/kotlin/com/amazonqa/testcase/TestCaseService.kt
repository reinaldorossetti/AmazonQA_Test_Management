package com.amazonqa.testcase

import com.amazonqa.audit.AuditService
import com.amazonqa.common.exception.DomainException
import com.amazonqa.store.StateStore
import com.amazonqa.store.TestCaseRecord
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class TestCaseService(
    private val stateStore: StateStore,
    private val auditService: AuditService,
) {
    fun createTestCase(
        projectId: UUID,
        title: String,
    ): TestCaseRecord {
        ensureProject(projectId)
        val testCase =
            TestCaseRecord(
                id = UUID.randomUUID(),
                projectId = projectId,
                title = title,
                version = 1,
            )
        stateStore.testCases[testCase.id] = testCase
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
        auditService.logCreate("TEST_CASE", newVersion.id.toString(), "TEST_CASE_VERSION_CREATED")
        return newVersion
    }

    fun archiveTestCase(testCaseId: UUID): TestCaseRecord = deleteTestCase(testCaseId)

    fun deleteTestCase(testCaseId: UUID): TestCaseRecord {
        val testCase = getTestCase(testCaseId)
        testCase.deletedAt = Instant.now()
        auditService.logDelete("TEST_CASE", testCase.id.toString(), "TEST_CASE_DELETED")
        return testCase
    }

    fun restoreTestCase(testCaseId: UUID): TestCaseRecord {
        val testCase = getTestCase(testCaseId)
        testCase.deletedAt = null
        auditService.logUpdate("TEST_CASE", testCase.id.toString(), "TEST_CASE_RESTORED")
        return testCase
    }

    fun bulkEdit(
        projectId: UUID,
        titleSuffix: String,
    ): List<TestCaseRecord> {
        val items = listTestCases(projectId)
        items.forEach { it.title = "${it.title}$titleSuffix" }
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
}
