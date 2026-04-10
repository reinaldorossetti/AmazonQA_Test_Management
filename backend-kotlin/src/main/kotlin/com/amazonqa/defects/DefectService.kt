package com.amazonqa.defects

import com.amazonqa.audit.AuditService
import com.amazonqa.common.exception.DomainException
import com.amazonqa.store.DefectRecord
import com.amazonqa.store.DefectStatus
import com.amazonqa.store.StateStore
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class DefectService(
    private val stateStore: StateStore,
    private val auditService: AuditService,
) {
    fun list(projectId: UUID): List<DefectRecord> = stateStore.defects.values.filter { it.projectId == projectId && it.deletedAt == null }

    fun get(defectId: UUID): DefectRecord =
        stateStore.defects[defectId]
            ?: throw DomainException(HttpStatus.NOT_FOUND, "DEFECT_NOT_FOUND", "Defect not found")

    fun createIssueFromExecution(
        executionId: UUID,
        title: String,
    ): DefectRecord {
        val execution =
            stateStore.executions[executionId]
                ?: throw DomainException(HttpStatus.NOT_FOUND, "EXECUTION_NOT_FOUND", "Execution not found")
        val defect =
            DefectRecord(
                id = UUID.randomUUID(),
                projectId = execution.projectId,
                executionId = executionId,
                title = title,
                status = DefectStatus.OPEN,
            )
        stateStore.defects[defect.id] = defect
        auditService.logCreate("DEFECT", defect.id.toString(), "DEFECT_CREATED")
        return defect
    }

    fun update(
        defectId: UUID,
        title: String?,
        status: DefectStatus?,
    ): DefectRecord {
        val defect = get(defectId)
        title?.let { defect.title = it }
        status?.let { defect.status = it }
        auditService.logUpdate("DEFECT", defect.id.toString(), "DEFECT_UPDATED")
        return defect
    }

    fun close(defectId: UUID): DefectRecord = update(defectId, null, DefectStatus.CLOSED)

    fun reopen(defectId: UUID): DefectRecord = update(defectId, null, DefectStatus.OPEN)

    fun delete(defectId: UUID): DefectRecord {
        val defect = get(defectId)
        defect.deletedAt = Instant.now()
        auditService.logDelete("DEFECT", defect.id.toString(), "DEFECT_DELETED")
        return defect
    }

    fun verifyIntegrationConnection(): Map<String, Any> = mapOf("provider" to "JIRA", "status" to "OK")

    fun retryFailedIssueCreation(): Map<String, Any> = mapOf("retried" to true)
}
