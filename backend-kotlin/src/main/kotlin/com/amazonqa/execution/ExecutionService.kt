package com.amazonqa.execution

import com.amazonqa.audit.AuditService
import com.amazonqa.common.exception.DomainException
import com.amazonqa.store.BuildStatus
import com.amazonqa.store.ExecutionRecord
import com.amazonqa.store.ExecutionStatus
import com.amazonqa.store.StateStore
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class ExecutionService(
    private val stateStore: StateStore,
    private val auditService: AuditService,
) {
    fun createRun(
        projectId: UUID,
        buildId: UUID,
    ): ExecutionRecord {
        val build =
            stateStore.builds[buildId]
                ?: throw DomainException(HttpStatus.NOT_FOUND, "BUILD_NOT_FOUND", "Build not found", "buildId")
        if (build.projectId != projectId) {
            throw DomainException(HttpStatus.BAD_REQUEST, "BUILD_PROJECT_MISMATCH", "Build does not belong to project")
        }
        val now = Instant.now()
        val execution =
            ExecutionRecord(
                id = UUID.randomUUID(),
                projectId = projectId,
                buildId = buildId,
                status = ExecutionStatus.NOT_RUN,
                createdAt = now,
                updatedAt = now,
            )
        stateStore.executions[execution.id] = execution
        auditService.logCreate("EXECUTION", execution.id.toString(), "RUN_CREATED")
        return execution
    }

    fun assignExecutionsInBulk(
        projectId: UUID,
        buildId: UUID,
        total: Int,
    ): List<ExecutionRecord> = (1..total).map { createRun(projectId, buildId) }

    fun setExecutionStatus(
        executionId: UUID,
        status: ExecutionStatus,
        actualResult: String?,
    ): ExecutionRecord {
        val execution = getExecution(executionId)
        val build =
            stateStore.builds[execution.buildId]
                ?: throw DomainException(HttpStatus.NOT_FOUND, "BUILD_NOT_FOUND", "Build not found", "buildId")

        if (build.status == BuildStatus.CLOSED) {
            throw DomainException(HttpStatus.CONFLICT, "BUILD_CLOSED_IMMUTABLE", "Closed build is immutable for execution updates")
        }

        if (status == ExecutionStatus.FAILED && (actualResult.isNullOrBlank() || execution.evidenceUrl.isNullOrBlank())) {
            throw DomainException(
                HttpStatus.BAD_REQUEST,
                "FAILED_EXECUTION_REQUIRES_EVIDENCE",
                "Failed execution requires actualResult and evidence",
            )
        }

        execution.status = status
        execution.actualResult = actualResult
        execution.updatedAt = Instant.now()
        auditService.logUpdate("EXECUTION", execution.id.toString(), "EXECUTION_STATUS_UPDATED")
        return execution
    }

    fun attachEvidence(
        executionId: UUID,
        evidenceUrl: String,
    ): ExecutionRecord {
        val execution = getExecution(executionId)
        execution.evidenceUrl = evidenceUrl
        execution.updatedAt = Instant.now()
        auditService.logUpdate("EXECUTION", execution.id.toString(), "EXECUTION_EVIDENCE_ATTACHED")
        return execution
    }

    fun hasActiveRun(projectId: UUID): Boolean =
        stateStore.executions.values.any {
            it.projectId == projectId && it.status == ExecutionStatus.NOT_RUN
        }

    fun getExecution(executionId: UUID): ExecutionRecord =
        stateStore.executions[executionId]
            ?: throw DomainException(HttpStatus.NOT_FOUND, "EXECUTION_NOT_FOUND", "Execution not found", "executionId")
}
