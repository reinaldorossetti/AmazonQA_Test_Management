package com.amazonqa.api.v1

import com.amazonqa.common.api.ApiEnvelope
import com.amazonqa.common.api.ResponseFactory
import com.amazonqa.projects.ProjectService
import com.amazonqa.security.SecuritySupport
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.constraints.NotBlank
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/projects")
class ProjectController(
    private val projectService: ProjectService,
) {
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun create(
        @RequestBody request: CreateProjectRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(projectService.createProject(request.name), servletRequest)

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER','GUEST')")
    fun list(
        @RequestParam(required = false, defaultValue = "false") includeArchived: Boolean,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(projectService.listProjects(includeArchived), servletRequest)

    @GetMapping("/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER','GUEST')")
    fun get(
        @PathVariable projectId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(projectService.getProject(projectId), servletRequest)

    @PatchMapping("/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun update(
        @PathVariable projectId: UUID,
        @RequestBody request: UpdateProjectRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(projectService.updateProject(projectId, request.name), servletRequest)

    @DeleteMapping("/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun delete(
        @PathVariable projectId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(projectService.deleteProject(projectId, SecuritySupport.currentActor()), servletRequest)

    @PostMapping("/{projectId}/restore")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun restore(
        @PathVariable projectId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(projectService.restoreProject(projectId), servletRequest)
}

data class CreateProjectRequest(
    @field:NotBlank val name: String,
)

data class UpdateProjectRequest(
    val name: String? = null,
)
