package com.amazonqa.api.v1

import com.amazonqa.common.api.ApiEnvelope
import com.amazonqa.common.api.ResponseFactory
import com.amazonqa.execution.ExecutionService
import com.amazonqa.store.ExecutionStatus
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/executions")
class ExecutionController(
    private val executionService: ExecutionService,
) {
    @PostMapping("/{executionId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER')")
    fun status(
        @PathVariable executionId: UUID,
        @RequestBody request: ExecutionStatusRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> =
        ResponseFactory.ok(
            executionService.setExecutionStatus(executionId, request.status, request.actualResult),
            servletRequest,
        )

    @PostMapping("/{executionId}/attachments")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER')")
    fun attach(
        @PathVariable executionId: UUID,
        @RequestBody request: ExecutionAttachmentRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(executionService.attachEvidence(executionId, request.evidenceUrl), servletRequest)
}

data class ExecutionStatusRequest(
    val status: ExecutionStatus,
    val actualResult: String? = null,
)

data class ExecutionAttachmentRequest(
    val evidenceUrl: String,
)
