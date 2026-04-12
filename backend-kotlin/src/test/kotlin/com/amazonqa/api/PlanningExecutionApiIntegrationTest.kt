package com.amazonqa.api

import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class PlanningExecutionApiIntegrationTest : ApiIntegrationTestBase() {
    @Test
    fun `build and test plan CRUD endpoints work`() {
        val projectId = createProject()
        val buildName = "Build ${faker.number().digits(4)}"

        val buildId =
            givenLeader()
                .body(mapOf("name" to buildName))
                .`when`()
                .post("/api/v1/projects/$projectId/builds")
                .then()
                .statusCode(200)
                .body("data.name", equalTo(buildName))
                .body("data.status", equalTo("DRAFT"))
                .extractId()

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/builds/$buildId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(buildId))
            .body("data.name", equalTo(buildName))
            .body("data.status", equalTo("DRAFT"))

        val updatedBuildName = "Build Updated"

        givenLeader()
            .body(mapOf("name" to updatedBuildName, "status" to "OPEN"))
            .`when`()
            .patch("/api/v1/projects/$projectId/builds/$buildId")
            .then()
            .statusCode(200)
            .body("data.name", equalTo(updatedBuildName))
            .body("data.status", equalTo("OPEN"))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/builds/$buildId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(buildId))
            .body("data.name", equalTo(updatedBuildName))
            .body("data.status", equalTo("OPEN"))

        givenLeader()
            .`when`()
            .delete("/api/v1/projects/$projectId/builds/$buildId")
            .then()
            .statusCode(409)
            .body("error.code", equalTo("BUILD_DELETE_DRAFT_ONLY"))

        val draftBuildName = "Build Draft ${faker.number().digits(5)}"
        val draftBuildId =
            givenLeader()
                .body(mapOf("name" to draftBuildName))
                .`when`()
                .post("/api/v1/projects/$projectId/builds")
                .then()
                .statusCode(200)
                .body("data.name", equalTo(draftBuildName))
                .body("data.status", equalTo("DRAFT"))
                .extractId()

        givenLeader()
            .`when`()
            .delete("/api/v1/projects/$projectId/builds/$draftBuildId")
            .then()
            .statusCode(200)
            .body("data.deleted", equalTo(true))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/builds/$draftBuildId")
            .then()
            .statusCode(404)
            .body("error.code", equalTo("BUILD_NOT_FOUND"))

        val planName = "Plan ${faker.company().industry()}"

        val planId =
            givenLeader()
                .body(mapOf("name" to planName))
                .`when`()
                .post("/api/v1/projects/$projectId/test-plans")
                .then()
                .statusCode(200)
                .body("data.name", equalTo(planName))
                .body("data.status", equalTo("DRAFT"))
                .extractId()

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/test-plans/$planId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(planId))
            .body("data.name", equalTo(planName))
            .body("data.status", equalTo("DRAFT"))

        val updatedPlanName = "Plan Updated"

        givenLeader()
            .body(mapOf("name" to updatedPlanName, "status" to "ACTIVE"))
            .`when`()
            .patch("/api/v1/projects/$projectId/test-plans/$planId")
            .then()
            .statusCode(200)
            .body("data.name", equalTo(updatedPlanName))
            .body("data.status", equalTo("ACTIVE"))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/test-plans/$planId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(planId))
            .body("data.name", equalTo(updatedPlanName))
            .body("data.status", equalTo("ACTIVE"))

        givenLeader()
            .`when`()
            .delete("/api/v1/projects/$projectId/test-plans/$planId")
            .then()
            .statusCode(409)
            .body("error.code", equalTo("PLAN_DELETE_DRAFT_ONLY"))

        val draftPlanName = "Plan Draft ${faker.company().buzzword()}"
        val draftPlanId =
            givenLeader()
                .body(mapOf("name" to draftPlanName))
                .`when`()
                .post("/api/v1/projects/$projectId/test-plans")
                .then()
                .statusCode(200)
                .body("data.name", equalTo(draftPlanName))
                .body("data.status", equalTo("DRAFT"))
                .extractId()

        givenLeader()
            .`when`()
            .delete("/api/v1/projects/$projectId/test-plans/$draftPlanId")
            .then()
            .statusCode(200)
            .body("data.deleted", equalTo(true))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/test-plans/$draftPlanId")
            .then()
            .statusCode(404)
            .body("error.code", equalTo("PLAN_NOT_FOUND"))
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

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/metrics")
            .then()
            .statusCode(200)
            .body("data.totalExecutions", equalTo(1))
            .body("data.passed", equalTo(1))
            .body("data.failed", equalTo(0))
            .body("data.notRun", equalTo(0))

        val runWithoutEvidenceId = createRun(projectId, planId, buildId)
        givenTester()
            .body(mapOf("status" to "FAILED", "actualResult" to "stacktrace"))
            .`when`()
            .post("/api/v1/executions/$runWithoutEvidenceId/status")
            .then()
            .statusCode(400)
            .body("error.code", equalTo("FAILED_EXECUTION_REQUIRES_EVIDENCE"))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/metrics")
            .then()
            .statusCode(200)
            .body("data.totalExecutions", equalTo(2))
            .body("data.passed", equalTo(1))
            .body("data.failed", equalTo(0))
            .body("data.notRun", equalTo(1))
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

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/builds/$buildId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(buildId))
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
