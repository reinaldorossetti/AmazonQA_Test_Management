package com.amazonqa.api

import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class PlanningExecutionApiIntegrationTest : ApiIntegrationTestBase() {
    @Test
    fun `build and test plan CRUD endpoints work`() {
        val projectId = createProject()

        val buildId =
            givenLeader()
                .body(mapOf("name" to "Build ${faker.number().digits(4)}"))
                .`when`()
                .post("/api/v1/projects/$projectId/builds")
                .then()
                .statusCode(200)
                .extractId()

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/builds/$buildId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(buildId))

        givenLeader()
            .body(mapOf("name" to "Build Updated", "status" to "OPEN"))
            .`when`()
            .patch("/api/v1/projects/$projectId/builds/$buildId")
            .then()
            .statusCode(200)
            .body("data.status", equalTo("OPEN"))

        givenLeader()
            .`when`()
            .delete("/api/v1/projects/$projectId/builds/$buildId")
            .then()
            .statusCode(409)
            .body("error.code", equalTo("BUILD_DELETE_DRAFT_ONLY"))

        val draftBuildId = createBuild(projectId)
        givenLeader()
            .`when`()
            .delete("/api/v1/projects/$projectId/builds/$draftBuildId")
            .then()
            .statusCode(200)
            .body("data.deleted", equalTo(true))

        val planId =
            givenLeader()
                .body(mapOf("name" to "Plan ${faker.company().industry()}"))
                .`when`()
                .post("/api/v1/projects/$projectId/test-plans")
                .then()
                .statusCode(200)
                .extractId()

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/test-plans/$planId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(planId))

        givenLeader()
            .body(mapOf("name" to "Plan Updated", "status" to "ACTIVE"))
            .`when`()
            .patch("/api/v1/projects/$projectId/test-plans/$planId")
            .then()
            .statusCode(200)
            .body("data.status", equalTo("ACTIVE"))

        givenLeader()
            .`when`()
            .delete("/api/v1/projects/$projectId/test-plans/$planId")
            .then()
            .statusCode(409)
            .body("error.code", equalTo("PLAN_DELETE_DRAFT_ONLY"))

        val draftPlanId = createPlan(projectId)
        givenLeader()
            .`when`()
            .delete("/api/v1/projects/$projectId/test-plans/$draftPlanId")
            .then()
            .statusCode(200)
            .body("data.deleted", equalTo(true))
    }

    @Test
    fun `execution endpoints work including evidence rule`() {
        val projectId = createProject()
        val buildId = createBuild(projectId)
        val planId = createPlan(projectId)
        val runId = createRun(projectId, planId, buildId)

        givenTester()
            .body(mapOf("status" to "PASSED", "actualResult" to "all good"))
            .`when`()
            .post("/api/v1/executions/$runId/status")
            .then()
            .statusCode(200)
            .body("data.status", equalTo("PASSED"))

        givenTester()
            .body(mapOf("evidenceUrl" to "https://evidence.example/${faker.internet().uuid()}"))
            .`when`()
            .post("/api/v1/executions/$runId/attachments")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(runId))

        val runWithoutEvidenceId = createRun(projectId, planId, buildId)
        givenTester()
            .body(mapOf("status" to "FAILED", "actualResult" to "stacktrace"))
            .`when`()
            .post("/api/v1/executions/$runWithoutEvidenceId/status")
            .then()
            .statusCode(400)
            .body("error.code", equalTo("FAILED_EXECUTION_REQUIRES_EVIDENCE"))
    }

    @Test
    fun `closing build blocks future execution updates`() {
        val projectId = createProject()
        val buildId = createBuild(projectId)
        val planId = createPlan(projectId)
        val runId = createRun(projectId, planId, buildId)

        givenLeader()
            .`when`()
            .patch("/api/v1/builds/$buildId/close")
            .then()
            .statusCode(200)
            .body("data.status", equalTo("CLOSED"))

        givenTester()
            .body(mapOf("status" to "PASSED", "actualResult" to "should fail"))
            .`when`()
            .post("/api/v1/executions/$runId/status")
            .then()
            .statusCode(409)
            .body("error.code", equalTo("BUILD_CLOSED_IMMUTABLE"))
    }
}
