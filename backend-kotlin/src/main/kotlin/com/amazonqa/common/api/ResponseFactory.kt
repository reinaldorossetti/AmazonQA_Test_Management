package com.amazonqa.common.api

import jakarta.servlet.http.HttpServletRequest

object ResponseFactory {
    fun <T> ok(
        data: T,
        request: HttpServletRequest,
    ): ApiEnvelope<T> = ApiEnvelope(data = data, traceId = request.getHeader("X-Trace-Id") ?: request.requestURI)
}
