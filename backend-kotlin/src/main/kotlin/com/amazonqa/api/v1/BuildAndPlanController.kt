package com.amazonqa.api.v1

import com.amazonqa.common.api.ApiEnvelope
import com.amazonqa.common.api.ResponseFactory
import com.amazonqa.execution.ExecutionService
import com.amazonqa.planning.BuildService
import com.amazonqa.planning.TestPlanService
import com.amazonqa.store.BuildStatus
import com.amazonqa.store.PlanStatus
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
@RequestMapping("/api/v1")
class BuildAndPlanController(
    private val buildService: BuildService,
    private val planService: TestPlanService,
    private val executionService: ExecutionService,
) {
    @PostMapping("/projects/{projectId}/builds")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun createBuild(
        @PathVariable projectId: UUID,
        @RequestBody request: CreateBuildRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(buildService.createBuild(projectId, request.name), servletRequest)

    @GetMapping("/projects/{projectId}/builds/{buildId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER','GUEST')")
    fun getBuild(
        @PathVariable buildId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(buildService.getBuild(buildId), servletRequest)

    @PatchMapping("/projects/{projectId}/builds/{buildId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun updateBuild(
        @PathVariable buildId: UUID,
        @RequestBody request: UpdateBuildRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(buildService.updateBuild(buildId, request.name, request.status), servletRequest)

    @DeleteMapping("/projects/{projectId}/builds/{buildId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun deleteBuild(
        @PathVariable buildId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> {
        buildService.deleteDraftBuild(buildId)
        return ResponseFactory.ok(mapOf("deleted" to true), servletRequest)
    }

    @PostMapping("/projects/{projectId}/test-plans")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun createPlan(
        @PathVariable projectId: UUID,
        @RequestBody request: CreatePlanRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(planService.createPlan(projectId, request.name), servletRequest)

    @GetMapping("/projects/{projectId}/test-plans/{planId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER','GUEST')")
    fun getPlan(
        @PathVariable planId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(planService.getPlan(planId), servletRequest)

    @PatchMapping("/projects/{projectId}/test-plans/{planId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun updatePlan(
        @PathVariable planId: UUID,
        @RequestBody request: UpdatePlanRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(planService.updatePlan(planId, request.name, request.status), servletRequest)

    @DeleteMapping("/projects/{projectId}/test-plans/{planId}")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun deletePlan(
        @PathVariable planId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> {
        planService.deleteDraftPlan(planId)
        return ResponseFactory.ok(mapOf("deleted" to true), servletRequest)
    }

    @PostMapping("/projects/{projectId}/test-plans/{planId}/runs")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER','TESTER')")
    fun createRun(
        @PathVariable projectId: UUID,
        @PathVariable planId: UUID,
        @RequestBody request: CreateRunRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> {
        planService.getPlan(planId)
        return ResponseFactory.ok(executionService.createRun(projectId, request.buildId), servletRequest)
    }

    @PatchMapping("/builds/{buildId}/close")
    @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
    fun closeBuild(
        @PathVariable buildId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(buildService.closeBuild(buildId), servletRequest)
}

data class CreateBuildRequest(val name: String)

data class UpdateBuildRequest(
    val name: String? = null,
    val status: BuildStatus? = null,
)

data class CreatePlanRequest(val name: String)

data class UpdatePlanRequest(
    val name: String? = null,
    val status: PlanStatus? = null,
)

data class CreateRunRequest(val buildId: UUID)
