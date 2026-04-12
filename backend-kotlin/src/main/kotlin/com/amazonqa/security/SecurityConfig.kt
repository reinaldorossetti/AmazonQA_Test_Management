package com.amazonqa.security

import com.amazonqa.common.api.ApiEnvelope
import com.amazonqa.common.api.ApiError
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val jwtTokenAuthenticationFilter: JwtTokenAuthenticationFilter,
    private val objectMapper: ObjectMapper,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf -> csrf.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/api/v1/auth/**",
                    "/api/v1/users/register",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/health",
                ).permitAll()
                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling {
                it.authenticationEntryPoint { _, response, _ ->
                    response.status = HttpStatus.UNAUTHORIZED.value()
                    response.contentType = "application/json"
                    response.writer.write(
                        objectMapper.writeValueAsString(
                            ApiEnvelope<Nothing>(
                                error = ApiError(code = "UNAUTHORIZED", message = "Authentication required"),
                                traceId = "security-auth",
                            ),
                        ),
                    )
                }
                it.accessDeniedHandler { _, response, _ ->
                    response.status = HttpStatus.FORBIDDEN.value()
                    response.contentType = "application/json"
                    response.writer.write(
                        objectMapper.writeValueAsString(
                            ApiEnvelope<Nothing>(
                                error = ApiError(code = "FORBIDDEN", message = "Insufficient permission"),
                                traceId = "security-denied",
                            ),
                        ),
                    )
                }
            }
        return http.build()
    }
}
