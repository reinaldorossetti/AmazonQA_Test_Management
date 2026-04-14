package com.amazonqa.testcase

import com.amazonqa.audit.AuditService
import com.amazonqa.common.exception.DomainException
import com.amazonqa.store.StateStore
import com.amazonqa.store.TestCaseAttachmentRecord
import com.amazonqa.store.TestCaseRecord
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.Timestamp
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
    val executionStatus: String? = null,
    val notes: String? = null,
    val attachments: String? = null,
)

data class UpdateTestCasePayload(
    val title: String? = null,
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
    val executionStatus: String? = null,
    val notes: String? = null,
    val attachments: String? = null,
)

data class UploadedTestCaseAttachment(
    val id: UUID,
    val fileName: String,
    val contentType: String,
    val fileSize: Long,
    val downloadUrl: String,
)

@Service
class TestCaseService(
    private val stateStore: StateStore,
    private val auditService: AuditService,
    private val jdbcTemplateProvider: ObjectProvider<JdbcTemplate>,
) {
    companion object {
        private const val MAX_ATTACHMENT_SIZE_BYTES = 1_048_576L
        private val ALLOWED_IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "webp")
        private val ALLOWED_SPREADSHEET_EXTENSIONS = setOf("csv", "xlsx", "xls")
        private val PRIORITY_MAP =
            mapOf(
                "critical" to "Critical",
                "high" to "High",
                "medium" to "Medium",
                "low" to "Low",
            )
        private val BUG_SEVERITY_MAP =
            mapOf(
                "blocker" to "Blocker",
                "critical" to "Critical",
                "major" to "Major",
                "minor" to "Minor",
                "trivial" to "Trivial",
            )
        private val EXECUTION_TYPE_MAP =
            mapOf(
                "manual" to "Manual",
                "automated" to "Automated",
            )
        private val TEST_CASE_STATUS_MAP =
            mapOf(
                "draft" to "Draft",
                "ready" to "Ready for Review",
                "ready for review" to "Ready for Review",
                "review in progress" to "Review in Progress",
                "rework" to "Rework",
                "final" to "Final",
                "future" to "Future",
                "obsolete" to "Obsolete",
            )
        private val EXECUTION_STATUS_MAP =
            mapOf(
                "not run" to "Not Run",
                "passed" to "Passed",
                "failed" to "Failed",
                "blocked" to "Blocked",
            )
    }

    fun createTestCase(
        projectId: UUID,
        payload: CreateTestCasePayload,
    ): TestCaseRecord {
        ensureProject(projectId)
        val generatedTestId = payload.testId ?: nextTestId(projectId)
        val normalizedTestCaseStatus = normalizeTestCaseStatus(payload.testCaseStatus)
        val normalizedExecutionStatus = normalizeExecutionStatus(payload.executionStatus)
        val testCase =
            TestCaseRecord(
                id = UUID.randomUUID(),
                projectId = projectId,
                title = payload.title,
                testId = generatedTestId,
                priority = normalizePriority(payload.priority),
                bugSeverity = normalizeBugSeverity(payload.bugSeverity),
                tagsKeywords = payload.tagsKeywords,
                requirementLink = payload.requirementLink,
                executionType = normalizeExecutionType(payload.executionType),
                testCaseStatus = normalizedTestCaseStatus,
                platform = payload.platform,
                testEnvironment = payload.testEnvironment,
                preconditions = payload.preconditions,
                actions = payload.actions,
                expectedResult = payload.expectedResult,
                executionStatus = normalizedExecutionStatus,
                notes = payload.notes,
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
        payload: UpdateTestCasePayload,
    ): TestCaseRecord {
        val testCase = getTestCase(testCaseId)

        val nextTitle = payload.title?.trim()
        if (nextTitle != null && nextTitle.isBlank()) {
            throw DomainException(HttpStatus.BAD_REQUEST, "TEST_CASE_TITLE_REQUIRED", "Title must not be blank", "title")
        }

        if (testCase.executedBefore) {
            return createNewVersion(testCaseId, nextTitle ?: testCase.title)
        }

        nextTitle?.let { testCase.title = it }
        payload.testId?.let {
            val normalized = it.trim()
            if (normalized.isBlank()) {
                throw DomainException(HttpStatus.BAD_REQUEST, "TEST_ID_REQUIRED", "testId must not be blank", "testId")
            }
            testCase.testId = normalized
        }
        payload.priority?.let { testCase.priority = normalizePriority(it) }
        payload.bugSeverity?.let { testCase.bugSeverity = normalizeBugSeverity(it) }
        payload.executionType?.let { testCase.executionType = normalizeExecutionType(it) }
        payload.testCaseStatus?.let { testCase.testCaseStatus = normalizeTestCaseStatus(it) }
        payload.executionStatus?.let { testCase.executionStatus = normalizeExecutionStatus(it) }

        testCase.tagsKeywords = applyNullableTextPatch(testCase.tagsKeywords, payload.tagsKeywords)
        testCase.requirementLink = applyNullableTextPatch(testCase.requirementLink, payload.requirementLink)
        testCase.platform = applyNullableTextPatch(testCase.platform, payload.platform)
        testCase.testEnvironment = applyNullableTextPatch(testCase.testEnvironment, payload.testEnvironment)
        testCase.preconditions = applyNullableTextPatch(testCase.preconditions, payload.preconditions)
        testCase.actions = applyNullableTextPatch(testCase.actions, payload.actions)
        testCase.expectedResult = applyNullableTextPatch(testCase.expectedResult, payload.expectedResult)
        testCase.notes = applyNullableTextPatch(testCase.notes, payload.notes)
        testCase.attachments = applyNullableTextPatch(testCase.attachments, payload.attachments)

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

    fun uploadAttachment(
        projectId: UUID,
        testCaseId: UUID,
        fileName: String?,
        contentType: String?,
        fileData: ByteArray,
    ): UploadedTestCaseAttachment {
        ensureProject(projectId)
        val testCase = getTestCase(testCaseId)

        if (testCase.projectId != projectId) {
            throw DomainException(HttpStatus.BAD_REQUEST, "TEST_CASE_PROJECT_MISMATCH", "Test case does not belong to project")
        }

        val normalizedFileName = fileName?.trim().orEmpty().ifBlank { "attachment.bin" }
        val normalizedContentType = contentType?.trim().orEmpty().ifBlank { "application/octet-stream" }

        validateAttachment(normalizedFileName, normalizedContentType, fileData)

        val attachmentId = UUID.randomUUID()
        val downloadUrl = "/api/v1/projects/$projectId/test-cases/$testCaseId/attachments/$attachmentId/download"

        val attachmentRecord =
            TestCaseAttachmentRecord(
                id = attachmentId,
                projectId = projectId,
                testCaseId = testCaseId,
                fileName = normalizedFileName,
                contentType = normalizedContentType,
                fileSize = fileData.size.toLong(),
                fileData = fileData,
            )

        stateStore.testCaseAttachments[attachmentId] = attachmentRecord
        upsertAttachmentInPostgres(attachmentRecord)

        testCase.attachments = mergeAttachmentLinks(testCase.attachments, downloadUrl)
        upsertTestCaseInPostgres(testCase)

        auditService.logCreate("TEST_CASE_ATTACHMENT", attachmentId.toString(), "TEST_CASE_ATTACHMENT_UPLOADED")

        return UploadedTestCaseAttachment(
            id = attachmentId,
            fileName = normalizedFileName,
            contentType = normalizedContentType,
            fileSize = fileData.size.toLong(),
            downloadUrl = downloadUrl,
        )
    }

    fun downloadAttachment(
        projectId: UUID,
        testCaseId: UUID,
        attachmentId: UUID,
    ): TestCaseAttachmentRecord {
        ensureProject(projectId)
        val testCase = getTestCase(testCaseId)

        if (testCase.projectId != projectId) {
            throw DomainException(HttpStatus.BAD_REQUEST, "TEST_CASE_PROJECT_MISMATCH", "Test case does not belong to project")
        }

        val inMemory = stateStore.testCaseAttachments[attachmentId]
        if (inMemory != null && inMemory.projectId == projectId && inMemory.testCaseId == testCaseId) {
            return inMemory
        }

        val jdbcTemplate = jdbcTemplateProvider.ifAvailable
        if (jdbcTemplate != null) {
            val found =
                jdbcTemplate.query(
                    """
                    SELECT id, project_id, test_case_id, file_name, content_type, file_size, file_data, created_at
                    FROM test_case_attachments
                    WHERE id = ? AND project_id = ? AND test_case_id = ?
                    """.trimIndent(),
                    { rs, _ ->
                        TestCaseAttachmentRecord(
                            id = UUID.fromString(rs.getString("id")),
                            projectId = UUID.fromString(rs.getString("project_id")),
                            testCaseId = UUID.fromString(rs.getString("test_case_id")),
                            fileName = rs.getString("file_name"),
                            contentType = rs.getString("content_type"),
                            fileSize = rs.getLong("file_size"),
                            fileData = rs.getBytes("file_data"),
                            createdAt = (rs.getTimestamp("created_at") ?: Timestamp.from(Instant.now())).toInstant(),
                        )
                    },
                    attachmentId,
                    projectId,
                    testCaseId,
                )

            if (found.isNotEmpty()) {
                val loaded = found.first()
                stateStore.testCaseAttachments[loaded.id] = loaded
                return loaded
            }
        }

        throw DomainException(HttpStatus.NOT_FOUND, "ATTACHMENT_NOT_FOUND", "Attachment not found")
    }

    private fun ensureProject(projectId: UUID) {
        if (!stateStore.projects.containsKey(projectId)) {
            throw DomainException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found")
        }
    }

    private fun nextTestId(projectId: UUID): String {
        val next = stateStore.testCases.values.count { it.projectId == projectId } + 1
        return "AMQA-${next.toString().padStart(3, '0')}"
    }

    private fun validateAttachment(
        fileName: String,
        contentType: String,
        fileData: ByteArray,
    ) {
        if (fileData.isEmpty()) {
            throw DomainException(HttpStatus.BAD_REQUEST, "ATTACHMENT_EMPTY", "File is empty")
        }

        if (fileData.size.toLong() > MAX_ATTACHMENT_SIZE_BYTES) {
            throw DomainException(HttpStatus.BAD_REQUEST, "ATTACHMENT_TOO_LARGE", "File size must be at most 1MB")
        }

        val extension = fileName.substringAfterLast('.', "").lowercase()
        val isImage = contentType.startsWith("image/") || extension in ALLOWED_IMAGE_EXTENSIONS
        val isSpreadsheet = extension in ALLOWED_SPREADSHEET_EXTENSIONS

        if (!isImage && !isSpreadsheet) {
            throw DomainException(
                HttpStatus.BAD_REQUEST,
                "ATTACHMENT_TYPE_NOT_ALLOWED",
                "Only image, Excel (.xls/.xlsx) or CSV files are allowed",
            )
        }
    }

    private fun normalizeTestCaseStatus(testCaseStatus: String?): String {
        val input = testCaseStatus?.trim().orEmpty()
        if (input.isBlank()) {
            return "Draft"
        }

        return TEST_CASE_STATUS_MAP[normalizeLookupKey(input)]
            ?: throw DomainException(
                HttpStatus.BAD_REQUEST,
                "TEST_CASE_STATUS_INVALID",
                "Invalid testCaseStatus. Allowed values: Draft, Ready for Review, Review in Progress, Rework, Final, Future, Obsolete",
                "testCaseStatus",
            )
    }

    private fun normalizePriority(priority: String?): String {
        val input = priority?.trim().orEmpty()
        if (input.isBlank()) {
            return "Medium"
        }

        return PRIORITY_MAP[normalizeLookupKey(input)]
            ?: throw DomainException(
                HttpStatus.BAD_REQUEST,
                "TEST_CASE_PRIORITY_INVALID",
                "Invalid priority. Allowed values: Critical, High, Medium, Low",
                "priority",
            )
    }

    private fun normalizeBugSeverity(bugSeverity: String?): String {
        val input = bugSeverity?.trim().orEmpty()
        if (input.isBlank()) {
            return "Major"
        }

        return BUG_SEVERITY_MAP[normalizeLookupKey(input)]
            ?: throw DomainException(
                HttpStatus.BAD_REQUEST,
                "TEST_CASE_BUG_SEVERITY_INVALID",
                "Invalid bugSeverity. Allowed values: Blocker, Critical, Major, Minor, Trivial",
                "bugSeverity",
            )
    }

    private fun normalizeExecutionType(executionType: String?): String {
        val input = executionType?.trim().orEmpty()
        if (input.isBlank()) {
            return "Manual"
        }

        return EXECUTION_TYPE_MAP[normalizeLookupKey(input)]
            ?: throw DomainException(
                HttpStatus.BAD_REQUEST,
                "TEST_CASE_EXECUTION_TYPE_INVALID",
                "Invalid executionType. Allowed values: Manual, Automated",
                "executionType",
            )
    }

    private fun normalizeExecutionStatus(executionStatus: String?): String {
        val input = executionStatus?.trim().orEmpty()
        if (input.isBlank()) {
            return "Not Run"
        }

        return EXECUTION_STATUS_MAP[normalizeLookupKey(input)]
            ?: throw DomainException(
                HttpStatus.BAD_REQUEST,
                "EXECUTION_STATUS_INVALID",
                "Invalid executionStatus. Allowed values: Not Run, Passed, Failed, Blocked",
                "executionStatus",
            )
    }

    private fun normalizeLookupKey(rawValue: String): String =
        rawValue
            .trim()
            .lowercase()
            .replace(Regex("[_-]+"), " ")
            .replace(Regex("\\s+"), " ")

    private fun mergeAttachmentLinks(
        current: String?,
        newDownloadUrl: String,
    ): String {
        val links =
            current
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.toMutableList()
                ?: mutableListOf()

        links.add(newDownloadUrl)
        return links.distinct().joinToString(",")
    }

    private fun upsertTestCaseInPostgres(testCase: TestCaseRecord) {
        val jdbcTemplate = jdbcTemplateProvider.ifAvailable ?: return

        val normalizedPriority = normalizePriorityForPersist(testCase.priority)
        val normalizedBugSeverity = normalizeBugSeverityForPersist(testCase.bugSeverity)
        val normalizedExecutionType = normalizeExecutionTypeForPersist(testCase.executionType)
        val normalizedTestCaseStatus = normalizeTestCaseStatusForPersist(testCase.testCaseStatus)
        val normalizedExecutionStatus = normalizeExecutionStatusForPersist(testCase.executionStatus)

        testCase.priority = normalizedPriority
        testCase.bugSeverity = normalizedBugSeverity
        testCase.executionType = normalizedExecutionType
        testCase.testCaseStatus = normalizedTestCaseStatus
        testCase.executionStatus = normalizedExecutionStatus

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
            testCase.id,
            testCase.projectId,
            testCase.testId,
            testCase.title,
            normalizedPriority,
            normalizedBugSeverity,
            testCase.tagsKeywords,
            testCase.requirementLink,
            normalizedExecutionType,
            normalizedTestCaseStatus,
            testCase.platform,
            testCase.testEnvironment,
            testCase.preconditions,
            testCase.actions,
            testCase.expectedResult,
            normalizedExecutionStatus,
            testCase.notes,
            testCase.attachments,
            testCase.version,
            testCase.deletedAt,
            testCase.executedBefore,
        )
    }

    private fun normalizePriorityForPersist(priority: String?): String {
        val input = priority?.trim().orEmpty()
        if (input.isBlank()) {
            return "Medium"
        }

        return PRIORITY_MAP[normalizeLookupKey(input)] ?: "Medium"
    }

    private fun normalizeBugSeverityForPersist(bugSeverity: String?): String {
        val input = bugSeverity?.trim().orEmpty()
        if (input.isBlank()) {
            return "Major"
        }

        return BUG_SEVERITY_MAP[normalizeLookupKey(input)] ?: "Major"
    }

    private fun normalizeExecutionTypeForPersist(executionType: String?): String {
        val input = executionType?.trim().orEmpty()
        if (input.isBlank()) {
            return "Manual"
        }

        return EXECUTION_TYPE_MAP[normalizeLookupKey(input)] ?: "Manual"
    }

    private fun normalizeTestCaseStatusForPersist(testCaseStatus: String?): String {
        val input = testCaseStatus?.trim().orEmpty()
        if (input.isBlank()) {
            return "Draft"
        }

        return TEST_CASE_STATUS_MAP[normalizeLookupKey(input)] ?: "Draft"
    }

    private fun normalizeExecutionStatusForPersist(executionStatus: String?): String {
        val input = executionStatus?.trim().orEmpty()
        if (input.isBlank()) {
            return "Not Run"
        }

        return EXECUTION_STATUS_MAP[normalizeLookupKey(input)] ?: "Not Run"
    }

    private fun applyNullableTextPatch(currentValue: String?, incomingValue: String?): String? {
        if (incomingValue == null) {
            return currentValue
        }

        val normalized = incomingValue.trim()
        return normalized.ifBlank { null }
    }

    private fun upsertAttachmentInPostgres(attachment: TestCaseAttachmentRecord) {
        val jdbcTemplate = jdbcTemplateProvider.ifAvailable ?: return

        jdbcTemplate.update(
            """
            INSERT INTO test_case_attachments (
                id,
                project_id,
                test_case_id,
                file_name,
                content_type,
                file_size,
                file_data,
                created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
            ON CONFLICT (id) DO UPDATE SET
                project_id = EXCLUDED.project_id,
                test_case_id = EXCLUDED.test_case_id,
                file_name = EXCLUDED.file_name,
                content_type = EXCLUDED.content_type,
                file_size = EXCLUDED.file_size,
                file_data = EXCLUDED.file_data
            """.trimIndent(),
            attachment.id,
            attachment.projectId,
            attachment.testCaseId,
            attachment.fileName,
            attachment.contentType,
            attachment.fileSize,
            attachment.fileData,
        )
    }
}
