package com.amazonqa.identity

import com.amazonqa.audit.AuditService
import com.amazonqa.common.exception.DomainException
import com.amazonqa.security.Role
import com.amazonqa.security.ScopeType
import com.amazonqa.store.RoleAssignment
import com.amazonqa.store.StateStore
import com.amazonqa.store.UserStatus
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AccessService(
    private val stateStore: StateStore,
    private val userService: UserService,
    private val auditService: AuditService,
) {
    private val permissionMap: Map<Role, Set<String>> =
        mapOf(
            Role.ADMIN to setOf("*"),
            Role.LEADER to setOf("PROJECT_WRITE", "BUILD_WRITE", "PLAN_WRITE", "DEFECT_WRITE", "REPORT_READ"),
            Role.TESTER to setOf("EXECUTION_WRITE", "DEFECT_WRITE", "TEST_CASE_WRITE", "PROJECT_READ"),
            Role.GUEST to setOf("PROJECT_READ", "REPORT_READ"),
        )

    fun listRoles(): List<String> = Role.entries.map { it.name }

    fun listPermissions(): Set<String> = permissionMap.values.flatten().toSet()

    fun getUserRoles(userId: UUID): Set<RoleAssignment> = userService.getUser(userId).roleAssignments

    fun assignRole(
        userId: UUID,
        role: Role,
        scopeType: ScopeType,
        scopeId: UUID?,
    ): Set<RoleAssignment> {
        val user = userService.getUser(userId)
        user.roleAssignments.add(RoleAssignment(role = role, scopeType = scopeType, scopeId = scopeId))
        auditService.logUpdate("USER", user.id.toString(), "ROLE_ASSIGNED:$role")
        return user.roleAssignments
    }

    fun removeRole(
        userId: UUID,
        role: Role,
        scopeType: ScopeType,
        scopeId: UUID?,
    ): Set<RoleAssignment> {
        val user = userService.getUser(userId)
        val target =
            user.roleAssignments.firstOrNull { it.role == role && it.scopeType == scopeType && it.scopeId == scopeId }
                ?: throw DomainException(HttpStatus.NOT_FOUND, "ROLE_ASSIGNMENT_NOT_FOUND", "Role assignment not found")

        if (
            role == Role.ADMIN &&
            scopeType == ScopeType.GLOBAL &&
            user.status == UserStatus.ACTIVE &&
            countActiveGlobalAdminsIfRemoved(user.id) <= 0
        ) {
            throw DomainException(HttpStatus.CONFLICT, "LAST_ADMIN_PROTECTION", "Cannot remove the last active ADMIN")
        }

        user.roleAssignments.remove(target)
        auditService.logUpdate("USER", user.id.toString(), "ROLE_REMOVED:$role")
        return user.roleAssignments
    }

    fun effectivePermissions(userId: UUID): Set<String> {
        val user = userService.getUser(userId)
        return user.roleAssignments.flatMap { permissionMap[it.role] ?: emptySet() }.toSet()
    }

    private fun countActiveGlobalAdminsIfRemoved(userId: UUID): Int =
        stateStore.users.values.count {
            if (it.id == userId) {
                it.status == UserStatus.ACTIVE &&
                    it.roleAssignments.any {
                            role ->
                        role.role == Role.ADMIN && role.scopeType == ScopeType.GLOBAL
                    } &&
                    it.roleAssignments.size > 1
            } else {
                it.status == UserStatus.ACTIVE &&
                    it.roleAssignments.any {
                            role ->
                        role.role == Role.ADMIN && role.scopeType == ScopeType.GLOBAL
                    }
            }
        }
}
