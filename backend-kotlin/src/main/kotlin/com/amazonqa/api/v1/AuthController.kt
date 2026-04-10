package com.amazonqa.api.v1

import com.amazonqa.auth.AuthService
import com.amazonqa.common.api.ApiEnvelope
import com.amazonqa.common.api.ResponseFactory
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Validated
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(authService.login(request.email, request.password), servletRequest)

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody request: RefreshRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(authService.refreshToken(request.refreshToken), servletRequest)

    @PostMapping("/logout")
    fun logout(servletRequest: HttpServletRequest): ApiEnvelope<Any> = ResponseFactory.ok(authService.logout(), servletRequest)
}

data class LoginRequest(
    @field:Email val email: String,
    @field:NotBlank val password: String,
)

data class RefreshRequest(
    @field:NotBlank val refreshToken: String,
)
