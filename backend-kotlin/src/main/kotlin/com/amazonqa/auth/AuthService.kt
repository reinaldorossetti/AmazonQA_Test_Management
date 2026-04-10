package com.amazonqa.auth

import com.amazonqa.common.exception.DomainException
import com.amazonqa.security.SecuritySupport
import com.amazonqa.store.StateStore
import com.amazonqa.store.UserStatus
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val stateStore: StateStore,
) {
    fun login(
        email: String,
        password: String,
    ): AuthTokenResponse {
        if (password.isBlank()) {
            throw DomainException(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", "Password cannot be blank", "password")
        }
        val token =
            when {
                email.contains("admin", ignoreCase = true) -> "admin-token"
                email.contains("leader", ignoreCase = true) -> "leader-token"
                email.contains("tester", ignoreCase = true) -> "tester-token"
                else -> "guest-token"
            }

        stateStore.users.values
            .firstOrNull { it.email.equals(email, ignoreCase = true) && it.status == UserStatus.ACTIVE }
            ?.also { it.lastLoginAt = java.time.Instant.now() }

        return AuthTokenResponse(accessToken = token, refreshToken = "refresh-$token", tokenType = "Bearer")
    }

    fun refreshToken(refreshToken: String): AuthTokenResponse {
        if (!refreshToken.startsWith("refresh-")) {
            throw DomainException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid")
        }
        val baseToken = refreshToken.removePrefix("refresh-")
        return AuthTokenResponse(accessToken = baseToken, refreshToken = refreshToken, tokenType = "Bearer")
    }

    fun logout(): Map<String, String> = mapOf("message" to "Logged out")

    fun getCurrentUser(): Map<String, Any> =
        mapOf(
            "username" to SecuritySupport.currentActor(),
            "roles" to (
                org.springframework.security.core.context.SecurityContextHolder.getContext().authentication?.authorities
                    ?.map { it.authority }
                    ?: emptyList<String>()
            ),
        )
}

data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
)
