package com.amazonqa.common.api

import java.time.Instant

data class ApiEnvelope<T>(
    val data: T? = null,
    val error: ApiError? = null,
    val traceId: String,
    val timestamp: Instant = Instant.now(),
)

data class ApiError(
    val code: String,
    val field: String? = null,
    val message: String,
)
