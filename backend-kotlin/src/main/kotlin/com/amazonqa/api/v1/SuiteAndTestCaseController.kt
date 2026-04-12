package com.amazonqa.api.v1

import com.amazonqa.common.api.ApiEnvelope
import com.amazonqa.common.api.ResponseFactory
import com.amazonqa.testcase.CreateTestCasePayload
import com.amazonqa.testcase.TestCaseService
import com.amazonqa.testsuite.SuiteService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/projects/{projectId}")
class SuiteAndTestCaseController(
    private val suiteService: SuiteService,
    private val testCaseService: TestCaseService,
) {
    @PostMapping("/suites")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER')")
    fun createSuite(
        @PathVariable projectId: UUID,
        @RequestBody request: CreateSuiteRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(suiteService.createSuite(projectId, request.name), servletRequest)

    @GetMapping("/suites/{suiteId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER','GUEST')")
    fun getSuite(
        @PathVariable suiteId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(suiteService.getSuite(suiteId), servletRequest)

    @PatchMapping("/suites/{suiteId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER')")
    fun updateSuite(
        @PathVariable suiteId: UUID,
        @RequestBody request: UpdateSuiteRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(suiteService.updateSuite(suiteId, request.name), servletRequest)

    @DeleteMapping("/suites/{suiteId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun deleteSuite(
        @PathVariable suiteId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(suiteService.deleteSuite(suiteId), servletRequest)

    @PostMapping("/suites/{suiteId}/restore")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun restoreSuite(
        @PathVariable suiteId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(suiteService.restoreSuite(suiteId), servletRequest)

    @GetMapping("/suites/tree")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER','GUEST')")
    fun tree(
        @PathVariable projectId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(suiteService.tree(projectId), servletRequest)

    @PostMapping("/test-cases")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER')")
    fun createTestCase(
        @PathVariable projectId: UUID,
        @RequestBody request: CreateTestCaseRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> =
        ResponseFactory.ok(
            testCaseService.createTestCase(
                projectId,
                CreateTestCasePayload(
                    title = request.title,
                    testId = request.testId,
                    priority = request.priority,
                    bugSeverity = request.bugSeverity,
                    tagsKeywords = request.tagsKeywords,
                    requirementLink = request.requirementLink,
                    executionType = request.executionType,
                    testCaseStatus = request.testCaseStatus,
                    platform = request.platform,
                    testEnvironment = request.testEnvironment,
                    preconditions = request.preconditions,
                    actions = request.actions,
                    expectedResult = request.expectedResult,
                    actualResult = request.actualResult,
                    executionStatus = request.executionStatus,
                    notes = request.notes,
                    customFields = request.customFields,
                    attachments = request.attachments,
                ),
            ),
            servletRequest,
        )

    @GetMapping("/test-cases")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER','GUEST')")
    fun listTestCases(
        @PathVariable projectId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(testCaseService.listTestCases(projectId), servletRequest)

    @GetMapping("/test-cases/{testCaseId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER','GUEST')")
    fun getTestCase(
        @PathVariable testCaseId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(testCaseService.getTestCase(testCaseId), servletRequest)

    @PatchMapping("/test-cases/{testCaseId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER')")
    fun updateTestCase(
        @PathVariable testCaseId: UUID,
        @RequestBody request: UpdateTestCaseRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(testCaseService.updateTestCase(testCaseId, request.title), servletRequest)

    @DeleteMapping("/test-cases/{testCaseId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun deleteTestCase(
        @PathVariable testCaseId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(testCaseService.deleteTestCase(testCaseId), servletRequest)

    @PostMapping("/test-cases/{testCaseId}/restore")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun restoreTestCase(
        @PathVariable testCaseId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(testCaseService.restoreTestCase(testCaseId), servletRequest)

    @PostMapping("/test-cases/{testCaseId}/versions")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER')")
    fun versionTestCase(
        @PathVariable testCaseId: UUID,
        @RequestBody request: VersionTestCaseRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(testCaseService.createNewVersion(testCaseId, request.title), servletRequest)

    @PatchMapping("/test-cases/bulk")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun bulkUpdate(
        @PathVariable projectId: UUID,
        @RequestBody request: BulkEditRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(testCaseService.bulkEdit(projectId, request.titleSuffix), servletRequest)

    @GetMapping("/test-cases/search")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER','GUEST')")
    fun search(
        @PathVariable projectId: UUID,
        @RequestParam query: String,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(testCaseService.search(projectId, query), servletRequest)
}

data class CreateSuiteRequest(val name: String)

data class UpdateSuiteRequest(val name: String? = null)

data class CreateTestCaseRequest(
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

data class UpdateTestCaseRequest(val title: String? = null)

data class VersionTestCaseRequest(val title: String)

data class BulkEditRequest(val titleSuffix: String = " - bulk")
