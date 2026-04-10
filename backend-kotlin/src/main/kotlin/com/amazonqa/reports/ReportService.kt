package com.amazonqa.reports

import com.amazonqa.audit.AuditService
import com.amazonqa.store.ExecutionStatus
import com.amazonqa.store.ReportJobRecord
import com.amazonqa.store.StateStore
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ReportService(
    private val stateStore: StateStore,
    private val auditService: AuditService,
) {
    fun getOperationalMetrics(projectId: UUID): Map<String, Any> {
        val executions = stateStore.executions.values.filter { it.projectId == projectId }
        return mapOf(
            "totalExecutions" to executions.size,
            "passed" to executions.count { it.status == ExecutionStatus.PASSED },
            "failed" to executions.count { it.status == ExecutionStatus.FAILED },
            "blocked" to executions.count { it.status == ExecutionStatus.BLOCKED },
            "notRun" to executions.count { it.status == ExecutionStatus.NOT_RUN },
        )
    }

    fun exportCoverageCsv(projectId: UUID): String = "projectId,coverage\n$projectId,100"

    fun exportExecutionPdf(projectId: UUID): String = "PDF_REPORT_FOR_$projectId"

    fun createBuildSnapshot(projectId: UUID): Map<String, Any> = mapOf("projectId" to projectId, "snapshotCreated" to true)

    fun createReportJob(
        projectId: UUID,
        type: String,
    ): ReportJobRecord {
        val job = ReportJobRecord(id = UUID.randomUUID(), projectId = projectId, type = type, status = "QUEUED")
        stateStore.reportJobs[job.id] = job
        auditService.logCreate("REPORT_JOB", job.id.toString(), "REPORT_JOB_CREATED")
        return job
    }

    fun getReportJobStatus(jobId: UUID): ReportJobRecord =
        stateStore.reportJobs[jobId] ?: ReportJobRecord(jobId, UUID.randomUUID(), "UNKNOWN", "NOT_FOUND")

    fun cancelReportJob(jobId: UUID): ReportJobRecord {
        val job = stateStore.reportJobs[jobId] ?: ReportJobRecord(jobId, UUID.randomUUID(), "UNKNOWN", "NOT_FOUND")
        job.status = "CANCELLED"
        stateStore.reportJobs[jobId] = job
        auditService.logDelete("REPORT_JOB", job.id.toString(), "REPORT_JOB_CANCELLED")
        return job
    }
}
