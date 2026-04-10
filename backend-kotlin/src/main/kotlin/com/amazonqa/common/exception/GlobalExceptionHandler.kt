package com.amazonqa.common.exception

import com.amazonqa.common.api.ApiEnvelope
import com.amazonqa.common.api.ApiError
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(DomainException::class)
    fun handleDomainException(
        ex: DomainException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiEnvelope<Nothing>> {
        return ResponseEntity
            .status(ex.status)
            .body(
                ApiEnvelope(
                    error = ApiError(code = ex.code, field = ex.field, message = ex.message),
                    traceId = request.requestId(),
                ),
            )
    }

    @ExceptionHandler(AuthorizationDeniedException::class, AccessDeniedException::class)
    fun handleAccessDenied(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiEnvelope<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(
                ApiEnvelope(
                    error = ApiError(code = "FORBIDDEN", message = ex.message ?: "Access denied"),
                    traceId = request.requestId(),
                ),
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiEnvelope<Nothing>> {
        val fieldError = ex.bindingResult.fieldErrors.firstOrNull()
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ApiEnvelope(
                    error =
                        ApiError(
                            code = "VALIDATION_ERROR",
                            field = fieldError?.field,
                            message = fieldError?.defaultMessage ?: "Invalid payload",
                        ),
                    traceId = request.requestId(),
                ),
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnknown(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiEnvelope<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ApiEnvelope(
                    error = ApiError(code = "INTERNAL_ERROR", message = ex.message ?: "Unexpected error"),
                    traceId = request.requestId(),
                ),
            )
    }
}

private fun HttpServletRequest.requestId(): String =
    this.getHeader("X-Trace-Id") ?: this.requestURI + "-" + System.currentTimeMillis()
