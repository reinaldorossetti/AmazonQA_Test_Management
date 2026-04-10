package com.amazonqa.api.v1

import com.amazonqa.common.api.ApiEnvelope
import com.amazonqa.common.api.ResponseFactory
import com.amazonqa.identity.UserService
import com.amazonqa.store.UserStatus
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/users")
class AdminUserController(
    private val userService: UserService,
) {
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun create(
        @RequestBody request: CreateUserRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(userService.createUser(request.fullName, request.email), servletRequest)

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun list(servletRequest: HttpServletRequest): ApiEnvelope<Any> = ResponseFactory.ok(userService.listUsers(), servletRequest)

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun get(
        @PathVariable userId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(userService.getUser(userId), servletRequest)

    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun update(
        @PathVariable userId: UUID,
        @RequestBody request: UpdateUserRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(userService.updateUser(userId, request.fullName, request.email), servletRequest)

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    fun status(
        @PathVariable userId: UUID,
        @RequestBody request: ChangeUserStatusRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(userService.changeStatus(userId, request.status), servletRequest)

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun delete(
        @PathVariable userId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(userService.deleteUser(userId), servletRequest)
}

data class CreateUserRequest(
    @field:NotBlank val fullName: String,
    @field:Email val email: String,
)

data class UpdateUserRequest(
    val fullName: String? = null,
    val email: String? = null,
)

data class ChangeUserStatusRequest(
    val status: UserStatus,
)
