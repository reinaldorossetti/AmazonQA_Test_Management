package com.amazonqa.store

import com.amazonqa.security.Role
import com.amazonqa.security.ScopeType
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class UserStatus { ACTIVE, INACTIVE, DELETED }

enum class ProjectStatus { ACTIVE, ARCHIVED }

enum class BuildStatus { DRAFT, OPEN, CLOSED }

enum class PlanStatus { DRAFT, ACTIVE, CLOSED }

enum class ExecutionStatus { PASSED, FAILED, BLOCKED, NOT_RUN }

enum class DefectStatus { OPEN, IN_PROGRESS, CLOSED }

data class RoleAssignment(
    val role: Role,
    val scopeType: ScopeType,
    val scopeId: UUID? = null,
    val assignedAt: Instant = Instant.now(),
)

data class UserRecord(
    val id: UUID,
    var fullName: String,
    var email: String,
    var status: UserStatus,
    val createdAt: Instant,
    var updatedAt: Instant,
    var lastLoginAt: Instant? = null,
    val roleAssignments: MutableSet<RoleAssignment> = mutableSetOf(),
    val preferences: MutableMap<String, String> = mutableMapOf(),
)

data class UserProfileRecord(
    val userId: UUID,
    var personType: String = "PF",
    var firstName: String? = null,
    var lastName: String? = null,
    var phone: String? = null,
    var cpf: String? = null,
    var cnpj: String? = null,
    var companyName: String? = null,
    var addressZip: String? = null,
    var addressStreet: String? = null,
    var addressNumber: String? = null,
    var addressComplement: String? = null,
    var addressNeighborhood: String? = null,
    var addressCity: String? = null,
    var addressState: String? = null,
    var residenceProofFilename: String? = null,
    var passwordHash: String? = null,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
)

data class ProjectRecord(
    val id: UUID,
    var name: String,
    var status: ProjectStatus,
    val createdAt: Instant,
    var updatedAt: Instant,
    var deletedAt: Instant? = null,
    var deletedBy: String? = null,
)

data class RequirementRecord(
    val id: UUID,
    val projectId: UUID,
    var title: String,
    var deletedAt: Instant? = null,
)

data class SuiteRecord(
    val id: UUID,
    val projectId: UUID,
    var name: String,
    var deletedAt: Instant? = null,
)

data class TestCaseRecord(
    val id: UUID,
    val projectId: UUID,
    var title: String,
    var testId: String? = null,
    var priority: String = "Medium",
    var bugSeverity: String = "Major",
    var tagsKeywords: String? = null,
    var requirementLink: String? = null,
    var executionType: String = "Manual",
    var testCaseStatus: String = "Draft",
    var platform: String? = null,
    var testEnvironment: String? = null,
    var preconditions: String? = null,
    var actions: String? = null,
    var expectedResult: String? = null,
    var actualResult: String? = null,
    var executionStatus: String = "Not Run",
    var notes: String? = null,
    var customFields: String? = null,
    var attachments: String? = null,
    var version: Int,
    var deletedAt: Instant? = null,
    var executedBefore: Boolean = false,
)

data class BuildRecord(
    val id: UUID,
    val projectId: UUID,
    var name: String,
    var status: BuildStatus,
    val createdAt: Instant,
    var updatedAt: Instant,
)

data class PlanRecord(
    val id: UUID,
    val projectId: UUID,
    var name: String,
    var status: PlanStatus,
    val createdAt: Instant,
    var updatedAt: Instant,
)

data class ExecutionRecord(
    val id: UUID,
    val projectId: UUID,
    val buildId: UUID,
    var status: ExecutionStatus,
    var actualResult: String? = null,
    var evidenceUrl: String? = null,
    val createdAt: Instant,
    var updatedAt: Instant,
)

data class DefectRecord(
    val id: UUID,
    val projectId: UUID,
    val executionId: UUID,
    var title: String,
    var status: DefectStatus,
    var deletedAt: Instant? = null,
)

data class ReportJobRecord(
    val id: UUID,
    val projectId: UUID,
    val type: String,
    var status: String,
)

data class AuditRecord(
    val id: UUID,
    val eventType: String,
    val actor: String,
    val entityType: String,
    val entityId: String,
    val metadata: String,
    val createdAt: Instant,
)

@Component
class StateStore {
    val users: MutableMap<UUID, UserRecord> = ConcurrentHashMap()
    val userProfiles: MutableMap<UUID, UserProfileRecord> = ConcurrentHashMap()
    val projects: MutableMap<UUID, ProjectRecord> = ConcurrentHashMap()
    val requirements: MutableMap<UUID, RequirementRecord> = ConcurrentHashMap()
    val suites: MutableMap<UUID, SuiteRecord> = ConcurrentHashMap()
    val testCases: MutableMap<UUID, TestCaseRecord> = ConcurrentHashMap()
    val builds: MutableMap<UUID, BuildRecord> = ConcurrentHashMap()
    val plans: MutableMap<UUID, PlanRecord> = ConcurrentHashMap()
    val executions: MutableMap<UUID, ExecutionRecord> = ConcurrentHashMap()
    val defects: MutableMap<UUID, DefectRecord> = ConcurrentHashMap()
    val reportJobs: MutableMap<UUID, ReportJobRecord> = ConcurrentHashMap()
    val audits: MutableList<AuditRecord> = mutableListOf()

    init {
        val now = Instant.now()
        val adminId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val projectId = UUID.fromString("10000000-0000-0000-0000-000000000001")
        users[adminId] =
            UserRecord(
                id = adminId,
                fullName = "System Admin",
                email = "admin@amazonqa.local",
                status = UserStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
                roleAssignments =
                    mutableSetOf(
                        RoleAssignment(role = Role.ADMIN, scopeType = ScopeType.GLOBAL),
                    ),
            )
        projects[projectId] =
            ProjectRecord(
                id = projectId,
                name = "Amazon QA Seed Project",
                status = ProjectStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
            )
    }
}
