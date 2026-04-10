package com.amazonqa.common.exception

import org.springframework.http.HttpStatus

class DomainException(
    val status: HttpStatus,
    val code: String,
    override val message: String,
    val field: String? = null,
) : RuntimeException(message)
