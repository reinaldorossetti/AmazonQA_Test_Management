package com.amazonqa.api.v1

import com.amazonqa.audit.AuditService
import com.amazonqa.common.api.ApiEnvelope
import com.amazonqa.common.api.ResponseFactory
import com.amazonqa.defects.DefectService
import com.amazonqa.reports.ReportService
import com.amazonqa.store.DefectStatus
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
@RequestMapping("/api/v1")
class DefectAndReportingController(
    private val defectService: DefectService,
    private val reportService: ReportService,
    private val auditService: AuditService,
) {
    @GetMapping("/projects/{projectId}/defects")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER','GUEST')")
    fun defects(
        @PathVariable projectId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(defectService.list(projectId), servletRequest)

    @GetMapping("/projects/{projectId}/defects/{defectId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER','GUEST')")
    fun defect(
        @PathVariable defectId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(defectService.get(defectId), servletRequest)

    @PostMapping("/executions/{executionId}/defects/jira")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER')")
    fun createDefect(
        @PathVariable executionId: UUID,
        @RequestBody request: CreateDefectRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(defectService.createIssueFromExecution(executionId, request.title), servletRequest)

    @PatchMapping("/projects/{projectId}/defects/{defectId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER')")
    fun updateDefect(
        @PathVariable defectId: UUID,
        @RequestBody request: UpdateDefectRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(defectService.update(defectId, request.title, request.status), servletRequest)

    @DeleteMapping("/projects/{projectId}/defects/{defectId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun deleteDefect(
        @PathVariable defectId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(defectService.delete(defectId), servletRequest)

    @PostMapping("/admin/integrations/jira/config")
    @PreAuthorize("hasRole('ADMIN')")
    fun jiraConfig(
        @RequestBody request: JiraConfigRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(mapOf("configured" to true, "url" to request.baseUrl), servletRequest)

    @GetMapping("/admin/integrations/jira/verify")
    @PreAuthorize("hasRole('ADMIN')")
    fun jiraVerify(servletRequest: HttpServletRequest): ApiEnvelope<Any> =
        ResponseFactory.ok(defectService.verifyIntegrationConnection(), servletRequest)

    @GetMapping("/projects/{projectId}/metrics")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER','GUEST')")
    fun metrics(
        @PathVariable projectId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(reportService.getOperationalMetrics(projectId), servletRequest)

    @GetMapping("/projects/{projectId}/reports/coverage")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER','GUEST')")
    fun coverageReport(
        @PathVariable projectId: UUID,
        @RequestParam format: String,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> =
        ResponseFactory.ok(mapOf("format" to format, "content" to reportService.exportCoverageCsv(projectId)), servletRequest)

    @GetMapping("/projects/{projectId}/reports/execution")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER','GUEST')")
    fun executionReport(
        @PathVariable projectId: UUID,
        @RequestParam format: String,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> =
        ResponseFactory.ok(mapOf("format" to format, "content" to reportService.exportExecutionPdf(projectId)), servletRequest)

    @PostMapping("/projects/{projectId}/reports/jobs")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun createReportJob(
        @PathVariable projectId: UUID,
        @RequestBody request: CreateReportJobRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(reportService.createReportJob(projectId, request.type), servletRequest)

    @GetMapping("/projects/{projectId}/reports/jobs/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER','GUEST')")
    fun reportJob(
        @PathVariable jobId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(reportService.getReportJobStatus(jobId), servletRequest)

    @DeleteMapping("/projects/{projectId}/reports/jobs/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun cancelReportJob(
        @PathVariable jobId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(reportService.cancelReportJob(jobId), servletRequest)

    @GetMapping("/admin/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    fun auditLogs(servletRequest: HttpServletRequest): ApiEnvelope<Any> = ResponseFactory.ok(auditService.list(), servletRequest)
}

data class CreateDefectRequest(val title: String)

data class UpdateDefectRequest(
    val title: String? = null,
    val status: DefectStatus? = null,
)

data class JiraConfigRequest(
    val baseUrl: String,
    val username: String,
    val apiToken: String,
)

data class CreateReportJobRequest(val type: String = "coverage")
