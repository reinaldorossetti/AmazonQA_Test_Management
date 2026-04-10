package com.amazonqa.planning

import com.amazonqa.audit.AuditService
import com.amazonqa.common.exception.DomainException
import com.amazonqa.store.PlanRecord
import com.amazonqa.store.PlanStatus
import com.amazonqa.store.StateStore
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class TestPlanService(
    private val stateStore: StateStore,
    private val auditService: AuditService,
) {
    fun createPlan(
        projectId: UUID,
        name: String,
    ): PlanRecord {
        ensureProject(projectId)
        val now = Instant.now()
        val plan =
            PlanRecord(
                id = UUID.randomUUID(),
                projectId = projectId,
                name = name,
                status = PlanStatus.DRAFT,
                createdAt = now,
                updatedAt = now,
            )
        stateStore.plans[plan.id] = plan
        auditService.logCreate("TEST_PLAN", plan.id.toString(), "TEST_PLAN_CREATED")
        return plan
    }

    fun getPlan(planId: UUID): PlanRecord =
        stateStore.plans[planId]
            ?: throw DomainException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "Test plan not found")

    fun updatePlan(
        planId: UUID,
        name: String?,
        status: PlanStatus?,
    ): PlanRecord {
        val plan = getPlan(planId)
        if (plan.status == PlanStatus.CLOSED) {
            throw DomainException(HttpStatus.CONFLICT, "PLAN_CLOSED_IMMUTABLE", "Closed test plan is immutable")
        }
        name?.let { plan.name = it }
        status?.let { plan.status = it }
        plan.updatedAt = Instant.now()
        auditService.logUpdate("TEST_PLAN", plan.id.toString(), "TEST_PLAN_UPDATED")
        return plan
    }

    fun deleteDraftPlan(planId: UUID) {
        val plan = getPlan(planId)
        if (plan.status != PlanStatus.DRAFT) {
            throw DomainException(HttpStatus.CONFLICT, "PLAN_DELETE_DRAFT_ONLY", "Only draft test plans can be deleted")
        }
        stateStore.plans.remove(plan.id)
        auditService.logDelete("TEST_PLAN", plan.id.toString(), "TEST_PLAN_DELETED")
    }

    private fun ensureProject(projectId: UUID) {
        if (!stateStore.projects.containsKey(projectId)) {
            throw DomainException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found")
        }
    }
}
