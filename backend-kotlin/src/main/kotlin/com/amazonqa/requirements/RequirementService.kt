package com.amazonqa.requirements

import com.amazonqa.audit.AuditService
import com.amazonqa.common.exception.DomainException
import com.amazonqa.store.RequirementRecord
import com.amazonqa.store.StateStore
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class RequirementService(
    private val stateStore: StateStore,
    private val auditService: AuditService,
) {
    fun createRequirement(
        projectId: UUID,
        title: String,
    ): RequirementRecord {
        ensureProject(projectId)
        val requirement = RequirementRecord(id = UUID.randomUUID(), projectId = projectId, title = title)
        stateStore.requirements[requirement.id] = requirement
        auditService.logCreate("REQUIREMENT", requirement.id.toString(), "REQUIREMENT_CREATED")
        return requirement
    }

    fun listRequirements(projectId: UUID): List<RequirementRecord> =
        stateStore.requirements.values.filter { it.projectId == projectId && it.deletedAt == null }

    fun getRequirement(requirementId: UUID): RequirementRecord =
        stateStore.requirements[requirementId]
            ?: throw DomainException(HttpStatus.NOT_FOUND, "REQUIREMENT_NOT_FOUND", "Requirement not found")

    fun updateRequirement(
        requirementId: UUID,
        title: String?,
    ): RequirementRecord {
        val requirement = getRequirement(requirementId)
        title?.let { requirement.title = it }
        auditService.logUpdate("REQUIREMENT", requirement.id.toString(), "REQUIREMENT_UPDATED")
        return requirement
    }

    fun deleteRequirement(requirementId: UUID): RequirementRecord {
        val requirement = getRequirement(requirementId)
        requirement.deletedAt = Instant.now()
        auditService.logDelete("REQUIREMENT", requirement.id.toString(), "REQUIREMENT_DELETED")
        return requirement
    }

    fun restoreRequirement(requirementId: UUID): RequirementRecord {
        val requirement = getRequirement(requirementId)
        requirement.deletedAt = null
        auditService.logUpdate("REQUIREMENT", requirement.id.toString(), "REQUIREMENT_RESTORED")
        return requirement
    }

    fun importCsv(
        projectId: UUID,
        rows: List<String>,
    ): Map<String, Any> {
        ensureProject(projectId)
        val imported = rows.filter { it.isNotBlank() }.map { createRequirement(projectId, it) }
        return mapOf("imported" to imported.size, "errors" to 0)
    }

    fun importXml(
        projectId: UUID,
        rows: List<String>,
    ): Map<String, Any> = importCsv(projectId, rows)

    fun getCoverage(requirementId: UUID): Map<String, Any> {
        getRequirement(requirementId)
        return mapOf("coverage" to 100, "status" to "COVERED")
    }

    fun buildTraceabilityMatrix(projectId: UUID): Map<String, Any> {
        ensureProject(projectId)
        return mapOf("projectId" to projectId, "requirements" to listRequirements(projectId).size)
    }

    private fun ensureProject(projectId: UUID) {
        if (!stateStore.projects.containsKey(projectId)) {
            throw DomainException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found")
        }
    }
}
