package com.amazonqa.api.v1

import com.amazonqa.common.api.ApiEnvelope
import com.amazonqa.common.api.ResponseFactory
import com.amazonqa.requirements.RequirementService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/projects/{projectId}")
class RequirementController(
    private val requirementService: RequirementService,
) {
    @PostMapping("/requirements")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER')")
    fun create(
        @PathVariable projectId: UUID,
        @RequestBody request: CreateRequirementRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(requirementService.createRequirement(projectId, request.title), servletRequest)

    @PostMapping("/requirements/import")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun import(
        @PathVariable projectId: UUID,
        @RequestBody request: ImportRequirementsRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> {
        val result =
            if (request.format.equals("xml", ignoreCase = true)) {
                requirementService.importXml(projectId, request.items)
            } else {
                requirementService.importCsv(projectId, request.items)
            }
        return ResponseFactory.ok(result, servletRequest)
    }

    @GetMapping("/requirements")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER','GUEST')")
    fun list(
        @PathVariable projectId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(requirementService.listRequirements(projectId), servletRequest)

    @GetMapping("/requirements/{requirementId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER','GUEST')")
    fun get(
        @PathVariable requirementId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(requirementService.getRequirement(requirementId), servletRequest)

    @PatchMapping("/requirements/{requirementId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER')")
    fun update(
        @PathVariable requirementId: UUID,
        @RequestBody request: UpdateRequirementRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(requirementService.updateRequirement(requirementId, request.title), servletRequest)

    @DeleteMapping("/requirements/{requirementId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun delete(
        @PathVariable requirementId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(requirementService.deleteRequirement(requirementId), servletRequest)

    @PostMapping("/requirements/{requirementId}/restore")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun restore(
        @PathVariable requirementId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(requirementService.restoreRequirement(requirementId), servletRequest)

    @GetMapping("/requirements/{requirementId}/coverage")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER','GUEST')")
    fun coverage(
        @PathVariable requirementId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(requirementService.getCoverage(requirementId), servletRequest)

    @GetMapping("/traceability-matrix")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER','GUEST')")
    fun traceability(
        @PathVariable projectId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(requirementService.buildTraceabilityMatrix(projectId), servletRequest)
}

data class CreateRequirementRequest(
    val title: String,
)

data class UpdateRequirementRequest(
    val title: String? = null,
)

data class ImportRequirementsRequest(
    val format: String = "csv",
    val items: List<String>,
)
