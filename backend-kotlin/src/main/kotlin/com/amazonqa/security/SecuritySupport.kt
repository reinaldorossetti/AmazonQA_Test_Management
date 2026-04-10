package com.amazonqa.security

import org.springframework.security.core.context.SecurityContextHolder

object SecuritySupport {
    fun currentActor(): String = SecurityContextHolder.getContext().authentication?.name ?: "anonymous"
}
