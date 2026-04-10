package com.amazonqa.planning

import com.amazonqa.audit.AuditService
import com.amazonqa.common.exception.DomainException
import com.amazonqa.store.BuildRecord
import com.amazonqa.store.BuildStatus
import com.amazonqa.store.StateStore
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class BuildService(
    private val stateStore: StateStore,
    private val auditService: AuditService,
) {
    fun createBuild(
        projectId: UUID,
        name: String,
    ): BuildRecord {
        ensureProjectExists(projectId)
        val now = Instant.now()
        val build =
            BuildRecord(
                id = UUID.randomUUID(),
                projectId = projectId,
                name = name,
                status = BuildStatus.DRAFT,
                createdAt = now,
                updatedAt = now,
            )
        stateStore.builds[build.id] = build
        auditService.logCreate("BUILD", build.id.toString(), "BUILD_CREATED")
        return build
    }

    fun getBuild(buildId: UUID): BuildRecord =
        stateStore.builds[buildId]
            ?: throw DomainException(HttpStatus.NOT_FOUND, "BUILD_NOT_FOUND", "Build not found", "buildId")

    fun updateBuild(
        buildId: UUID,
        name: String?,
        status: BuildStatus?,
    ): BuildRecord {
        val build = getBuild(buildId)
        if (build.status == BuildStatus.CLOSED) {
            throw DomainException(HttpStatus.CONFLICT, "BUILD_CLOSED_IMMUTABLE", "Closed build is immutable")
        }
        name?.let { build.name = it }
        status?.let { build.status = it }
        build.updatedAt = Instant.now()
        auditService.logUpdate("BUILD", build.id.toString(), "BUILD_UPDATED")
        return build
    }

    fun deleteDraftBuild(buildId: UUID) {
        val build = getBuild(buildId)
        if (build.status != BuildStatus.DRAFT) {
            throw DomainException(HttpStatus.CONFLICT, "BUILD_DELETE_DRAFT_ONLY", "Only draft builds can be deleted")
        }
        stateStore.builds.remove(build.id)
        auditService.logDelete("BUILD", build.id.toString(), "BUILD_DELETED")
    }

    fun closeBuild(buildId: UUID): BuildRecord {
        val build = getBuild(buildId)
        build.status = BuildStatus.CLOSED
        build.updatedAt = Instant.now()
        auditService.logUpdate("BUILD", build.id.toString(), "BUILD_CLOSED")
        return build
    }

    fun hasActiveBuild(projectId: UUID): Boolean =
        stateStore.builds.values.any { it.projectId == projectId && it.status == BuildStatus.OPEN }

    private fun ensureProjectExists(projectId: UUID) {
        if (!stateStore.projects.containsKey(projectId)) {
            throw DomainException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found", "projectId")
        }
    }
}
