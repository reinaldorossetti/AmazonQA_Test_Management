package com.amazonqa.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtTokenAuthenticationFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = extractBearerToken(request)
        if (!token.isNullOrBlank() && SecurityContextHolder.getContext().authentication == null) {
            val principal = TokenPrincipal.fromToken(token)
            if (principal != null) {
                val authorities = principal.roles.map { SimpleGrantedAuthority("ROLE_${it.name}") }
                val authentication = UsernamePasswordAuthenticationToken(principal.username, null, authorities)
                SecurityContextHolder.getContext().authentication = authentication
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun extractBearerToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        if (!header.startsWith("Bearer ")) {
            return null
        }
        return header.removePrefix("Bearer ").trim()
    }
}

data class TokenPrincipal(
    val username: String,
    val roles: Set<Role>,
) {
    companion object {
        fun fromToken(token: String): TokenPrincipal? =
            when (token) {
                "admin-token" -> TokenPrincipal("admin@amazonqa.local", setOf(Role.ADMIN, Role.LEADER, Role.TESTER, Role.GUEST))
                "leader-token" -> TokenPrincipal("leader@amazonqa.local", setOf(Role.LEADER, Role.TESTER, Role.GUEST))
                "tester-token" -> TokenPrincipal("tester@amazonqa.local", setOf(Role.TESTER, Role.GUEST))
                "guest-token" -> TokenPrincipal("guest@amazonqa.local", setOf(Role.GUEST))
                else -> null
            }
    }
}
