package com.amazonqa.identity

import com.amazonqa.audit.AuditService
import com.amazonqa.common.exception.DomainException
import com.amazonqa.security.Role
import com.amazonqa.store.RoleAssignment
import com.amazonqa.store.StateStore
import com.amazonqa.store.UserRecord
import com.amazonqa.store.UserStatus
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Service
class UserService(
    private val stateStore: StateStore,
    private val auditService: AuditService,
    private val jdbcTemplateProvider: ObjectProvider<JdbcTemplate>,
) {
    fun createUser(
        fullName: String,
        email: String,
        role: Role = Role.GUEST,
    ): UserRecord {
        if (stateStore.users.values.any { it.email.equals(email, ignoreCase = true) }) {
            throw DomainException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Email is already registered", "email")
        }
        val now = Instant.now()
        val user =
            UserRecord(
                id = UUID.randomUUID(),
                fullName = fullName,
                email = email,
                status = UserStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
                roleAssignments =
                    mutableSetOf(
                        RoleAssignment(role = role, scopeType = com.amazonqa.security.ScopeType.GLOBAL),
                    ),
            )
        stateStore.users[user.id] = user
        upsertUserInPostgres(user)
        auditService.logCreate("USER", user.id.toString(), "USER_CREATED")
        return user
    }

    fun listUsers(): List<UserRecord> = stateStore.users.values.sortedBy { it.email }

    fun getUser(userId: UUID): UserRecord =
        stateStore.users[userId]
            ?: throw DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found", "userId")

    fun updateUser(
        userId: UUID,
        fullName: String?,
        email: String?,
    ): UserRecord {
        val user = getUser(userId)
        email?.let { newEmail ->
            val duplicate =
                stateStore.users.values.any {
                    it.id != userId && it.email.equals(newEmail, ignoreCase = true)
                }
            if (duplicate) {
                throw DomainException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Email is already registered", "email")
            }
        }
        fullName?.let { user.fullName = it }
        email?.let { user.email = it }
        user.updatedAt = Instant.now()
        upsertUserInPostgres(user)
        auditService.logUpdate("USER", user.id.toString(), "USER_UPDATED")
        return user
    }

    fun changeStatus(
        userId: UUID,
        status: UserStatus,
    ): UserRecord {
        val user = getUser(userId)
        if (status != UserStatus.ACTIVE && user.isGlobalAdmin() && countActiveGlobalAdmins() <= 1) {
            throw DomainException(HttpStatus.CONFLICT, "LAST_ADMIN_PROTECTION", "Cannot deactivate the last active ADMIN")
        }
        user.status = status
        user.updatedAt = Instant.now()
        upsertUserInPostgres(user)
        auditService.logUpdate("USER", user.id.toString(), "USER_STATUS_UPDATED")
        return user
    }

    fun deleteUser(userId: UUID): UserRecord {
        val user = getUser(userId)
        if (user.isGlobalAdmin() && countActiveGlobalAdmins() <= 1) {
            throw DomainException(HttpStatus.CONFLICT, "LAST_ADMIN_PROTECTION", "Cannot delete the last active ADMIN")
        }
        user.status = UserStatus.DELETED
        user.updatedAt = Instant.now()
        upsertUserInPostgres(user)
        auditService.logDelete("USER", user.id.toString(), "USER_SOFT_DELETED")
        return user
    }

    fun updateMyPreferences(preferences: Map<String, String>): Map<String, String> {
        val actor = com.amazonqa.security.SecuritySupport.currentActor()
        val user =
            stateStore.users.values.firstOrNull { it.email.equals(actor, ignoreCase = true) }
                ?: throw DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Authenticated user not found")
        user.preferences.putAll(preferences)
        user.updatedAt = Instant.now()
        auditService.logUpdate("USER", user.id.toString(), "USER_PREFERENCES_UPDATED")
        return user.preferences.toMap()
    }

    private fun countActiveGlobalAdmins(): Int = stateStore.users.values.count { it.status == UserStatus.ACTIVE && it.isGlobalAdmin() }

    private fun UserRecord.isGlobalAdmin(): Boolean =
        roleAssignments.any { it.role == Role.ADMIN && it.scopeType == com.amazonqa.security.ScopeType.GLOBAL }

    private fun upsertUserInPostgres(user: UserRecord) {
        val jdbcTemplate = jdbcTemplateProvider.ifAvailable ?: return
        jdbcTemplate.update(
            """
            INSERT INTO users (id, full_name, email, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                full_name = EXCLUDED.full_name,
                email = EXCLUDED.email,
                status = EXCLUDED.status,
                updated_at = EXCLUDED.updated_at
            """.trimIndent(),
            user.id,
            user.fullName,
            user.email,
            user.status.name,
            Timestamp.from(user.createdAt),
            Timestamp.from(user.updatedAt),
        )
    }
}
