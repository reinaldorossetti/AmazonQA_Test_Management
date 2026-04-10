package com.amazonqa.api.v1

import com.amazonqa.auth.AuthService
import com.amazonqa.common.api.ApiEnvelope
import com.amazonqa.common.api.ResponseFactory
import com.amazonqa.identity.UserService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserSelfController(
    private val authService: AuthService,
    private val userService: UserService,
) {
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun me(servletRequest: HttpServletRequest): ApiEnvelope<Any> = ResponseFactory.ok(authService.getCurrentUser(), servletRequest)

    @PatchMapping("/me/preferences")
    @PreAuthorize("isAuthenticated()")
    fun preferences(
        @RequestBody request: PreferencesRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(userService.updateMyPreferences(request.preferences), servletRequest)
}

data class PreferencesRequest(
    val preferences: Map<String, String>,
)
