package com.amazonqa.api

import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Test

class DefectReportingApiIntegrationTest : ApiIntegrationTestBase() {
    @Test
    fun `defect CRUD endpoints work`() {
        val projectId = createProject()
        val buildId = createBuild(projectId)
        val planId = createPlan(projectId)
        val runId = createRun(projectId, planId, buildId)

        val defectTitle = "Defect-${faker.number().digits(6)}"
        val defectId =
            givenTester()
                .body(mapOf("title" to defectTitle))
                .`when`()
                .post("/api/v1/executions/$runId/defects/jira")
                .then()
                .statusCode(200)
                .body("data.title", equalTo(defectTitle))
                .extractId()

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/defects")
            .then()
            .statusCode(200)
            .body("data.id", hasItem(defectId))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/defects/$defectId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(defectId))

        givenTester()
            .body(mapOf("title" to "$defectTitle-updated", "status" to "IN_PROGRESS"))
            .`when`()
            .patch("/api/v1/projects/$projectId/defects/$defectId")
            .then()
            .statusCode(200)
            .body("data.status", equalTo("IN_PROGRESS"))

        givenLeader()
            .`when`()
            .delete("/api/v1/projects/$projectId/defects/$defectId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(defectId))
    }

    @Test
    fun `reporting and jira integration endpoints work`() {
        val projectId = createProject()

        givenAdmin()
            .body(
                mapOf(
                    "baseUrl" to "https://jira.example.com",
                    "username" to faker.internet().emailAddress(),
                    "apiToken" to faker.internet().password(12, 16, true, true),
                ),
            ).`when`()
            .post("/api/v1/admin/integrations/jira/config")
            .then()
            .statusCode(200)
            .body("data.configured", equalTo(true))

        givenAdmin()
            .`when`()
            .get("/api/v1/admin/integrations/jira/verify")
            .then()
            .statusCode(200)
            .body("data.status", equalTo("OK"))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/metrics")
            .then()
            .statusCode(200)
            .body("data.totalExecutions", equalTo(0))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/reports/coverage?format=csv")
            .then()
            .statusCode(200)
            .body("data.format", equalTo("csv"))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/reports/execution?format=pdf")
            .then()
            .statusCode(200)
            .body("data.format", equalTo("pdf"))

        val jobId =
            givenLeader()
                .body(mapOf("type" to "coverage"))
                .`when`()
                .post("/api/v1/projects/$projectId/reports/jobs")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("QUEUED"))
                .extractId()

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/reports/jobs/$jobId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(jobId))

        givenLeader()
            .`when`()
            .delete("/api/v1/projects/$projectId/reports/jobs/$jobId")
            .then()
            .statusCode(200)
            .body("data.status", equalTo("CANCELLED"))

        givenAdmin()
            .`when`()
            .get("/api/v1/admin/audit-logs")
            .then()
            .statusCode(200)
    }
}
