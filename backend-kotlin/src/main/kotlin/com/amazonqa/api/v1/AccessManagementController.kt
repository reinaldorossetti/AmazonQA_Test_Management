package com.amazonqa.api.v1

import com.amazonqa.common.api.ApiEnvelope
import com.amazonqa.common.api.ResponseFactory
import com.amazonqa.identity.AccessService
import com.amazonqa.security.Role
import com.amazonqa.security.ScopeType
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin")
class AccessManagementController(
    private val accessService: AccessService,
) {
    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    fun roles(servletRequest: HttpServletRequest): ApiEnvelope<Any> = ResponseFactory.ok(accessService.listRoles(), servletRequest)

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    fun permissions(servletRequest: HttpServletRequest): ApiEnvelope<Any> =
        ResponseFactory.ok(accessService.listPermissions(), servletRequest)

    @GetMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    fun userRoles(
        @PathVariable userId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(accessService.getUserRoles(userId), servletRequest)

    @PostMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    fun assignRole(
        @PathVariable userId: UUID,
        @RequestBody request: RoleRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> =
        ResponseFactory.ok(
            accessService.assignRole(userId, request.role, ScopeType.GLOBAL, null),
            servletRequest,
        )

    @DeleteMapping("/users/{userId}/roles/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    fun removeRole(
        @PathVariable userId: UUID,
        @PathVariable role: Role,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(accessService.removeRole(userId, role, ScopeType.GLOBAL, null), servletRequest)

    @PostMapping("/users/{userId}/scoped-roles")
    @PreAuthorize("hasRole('ADMIN')")
    fun scopedRole(
        @PathVariable userId: UUID,
        @RequestBody request: ScopedRoleRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> =
        ResponseFactory.ok(
            accessService.assignRole(userId, request.role, ScopeType.PROJECT, request.scopeId),
            servletRequest,
        )

    @DeleteMapping("/users/{userId}/scoped-roles/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    fun removeScopedRole(
        @PathVariable userId: UUID,
        @PathVariable role: Role,
        @RequestBody request: ScopedRoleDeleteRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(accessService.removeRole(userId, role, ScopeType.PROJECT, request.scopeId), servletRequest)

    @GetMapping("/users/{userId}/effective-permissions")
    @PreAuthorize("hasRole('ADMIN')")
    fun effectivePermissions(
        @PathVariable userId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(accessService.effectivePermissions(userId), servletRequest)
}

data class RoleRequest(
    val role: Role,
)

data class ScopedRoleRequest(
    val role: Role,
    val scopeId: UUID,
)

data class ScopedRoleDeleteRequest(
    val scopeId: UUID,
)
