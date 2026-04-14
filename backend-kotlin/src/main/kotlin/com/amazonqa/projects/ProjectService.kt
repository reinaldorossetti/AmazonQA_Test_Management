package com.amazonqa.projects

import com.amazonqa.audit.AuditService
import com.amazonqa.common.exception.DomainException
import com.amazonqa.execution.ExecutionService
import com.amazonqa.planning.BuildService
import com.amazonqa.store.ProjectRecord
import com.amazonqa.store.ProjectStatus
import com.amazonqa.store.StateStore
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Service
class ProjectService(
    private val stateStore: StateStore,
    private val buildService: BuildService,
    private val executionService: ExecutionService,
    private val auditService: AuditService,
    private val jdbcTemplateProvider: ObjectProvider<JdbcTemplate>,
) {
    private val membersByProject: MutableMap<UUID, MutableSet<String>> = mutableMapOf()

    fun createProject(name: String): ProjectRecord {
        val now = Instant.now()
        val project =
            ProjectRecord(
                id = UUID.randomUUID(),
                name = name,
                status = ProjectStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
            )
        stateStore.projects[project.id] = project
        upsertProjectInPostgres(project)
        auditService.logCreate("PROJECT", project.id.toString(), "PROJECT_CREATED")
        return project
    }

    fun listProjects(includeArchived: Boolean): List<ProjectRecord> =
        stateStore.projects.values
            .filter { includeArchived || it.status != ProjectStatus.ARCHIVED }
            .sortedBy { it.name }

    fun getProject(projectId: UUID): ProjectRecord =
        stateStore.projects[projectId]
            ?: throw DomainException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found", "projectId")

    fun updateProject(
        projectId: UUID,
        name: String?,
    ): ProjectRecord {
        val project = getProject(projectId)
        if (project.status == ProjectStatus.ARCHIVED) {
            throw DomainException(HttpStatus.CONFLICT, "PROJECT_ARCHIVED", "Archived project cannot be updated")
        }
        name?.let { project.name = it }
        project.updatedAt = Instant.now()
        upsertProjectInPostgres(project)
        auditService.logUpdate("PROJECT", project.id.toString(), "PROJECT_UPDATED")
        return project
    }

    fun deleteProject(
        projectId: UUID,
        deletedBy: String,
    ): ProjectRecord {
        val project = getProject(projectId)
        if (buildService.hasActiveBuild(projectId) || executionService.hasActiveRun(projectId)) {
            throw DomainException(HttpStatus.CONFLICT, "PROJECT_HAS_ACTIVE_RUNS", "Project has active builds/runs")
        }
        project.status = ProjectStatus.ARCHIVED
        project.deletedAt = Instant.now()
        project.deletedBy = deletedBy
        project.updatedAt = Instant.now()
        upsertProjectInPostgres(project)
        auditService.logDelete("PROJECT", project.id.toString(), "PROJECT_DELETED")
        return project
    }

    fun restoreProject(projectId: UUID): ProjectRecord {
        val project = getProject(projectId)
        project.status = ProjectStatus.ACTIVE
        project.deletedAt = null
        project.deletedBy = null
        project.updatedAt = Instant.now()
        upsertProjectInPostgres(project)
        auditService.logUpdate("PROJECT", project.id.toString(), "PROJECT_RESTORED")
        return project
    }

    fun addMember(
        projectId: UUID,
        userEmail: String,
    ): Set<String> {
        getProject(projectId)
        val members = membersByProject.getOrPut(projectId) { mutableSetOf() }
        members.add(userEmail)
        auditService.logUpdate("PROJECT", projectId.toString(), "MEMBER_ADDED:$userEmail")
        return members
    }

    fun removeMember(
        projectId: UUID,
        userEmail: String,
    ): Set<String> {
        getProject(projectId)
        val members = membersByProject.getOrPut(projectId) { mutableSetOf() }
        members.remove(userEmail)
        auditService.logUpdate("PROJECT", projectId.toString(), "MEMBER_REMOVED:$userEmail")
        return members
    }

    private fun upsertProjectInPostgres(project: ProjectRecord) {
        val jdbcTemplate = jdbcTemplateProvider.ifAvailable ?: return

        jdbcTemplate.update(
            """
            INSERT INTO projects (
                id,
                name,
                status,
                deleted_at,
                deleted_by,
                created_at,
                updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                status = EXCLUDED.status,
                deleted_at = EXCLUDED.deleted_at,
                deleted_by = EXCLUDED.deleted_by,
                updated_at = EXCLUDED.updated_at
            """.trimIndent(),
            project.id,
            project.name,
            project.status.name,
            project.deletedAt?.let { Timestamp.from(it) },
            project.deletedBy,
            Timestamp.from(project.createdAt),
            Timestamp.from(project.updatedAt),
        )
    }
}
