package com.amazonqa.api

import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.notNullValue
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
                .body("data.status", equalTo("OPEN"))
                .extractId()

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/defects/$defectId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(defectId))
            .body("data.title", equalTo(defectTitle))
            .body("data.status", equalTo("OPEN"))
            .body("data.executionId", equalTo(runId))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/defects")
            .then()
            .statusCode(200)
            .body("data.id", hasItem(defectId))
            .body("data.title", hasItem(defectTitle))
            .body("data.status", hasItem("OPEN"))

        val updatedTitle = "$defectTitle-updated"

        givenTester()
            .body(mapOf("title" to updatedTitle, "status" to "IN_PROGRESS"))
            .`when`()
            .patch("/api/v1/projects/$projectId/defects/$defectId")
            .then()
            .statusCode(200)
            .body("data.title", equalTo(updatedTitle))
            .body("data.status", equalTo("IN_PROGRESS"))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/defects/$defectId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(defectId))
            .body("data.title", equalTo(updatedTitle))
            .body("data.status", equalTo("IN_PROGRESS"))

        givenLeader()
            .`when`()
            .delete("/api/v1/projects/$projectId/defects/$defectId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(defectId))
            .body("data.deletedAt", notNullValue())

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/defects")
            .then()
            .statusCode(200)
            .body("data.id", not(hasItem(defectId)))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/defects/$defectId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(defectId))
            .body("data.title", equalTo(updatedTitle))
            .body("data.status", equalTo("IN_PROGRESS"))
            .body("data.deletedAt", notNullValue())
    }

    @Test
    fun `reporting and jira integration endpoints work`() {
        val projectId = createProject()
        val jiraUrl = "https://jira.example.com"
        val jiraUsername = faker.internet().emailAddress()
        val jiraToken = faker.internet().password(12, 16, true, true)

        givenAdmin()
            .body(
                mapOf(
                    "baseUrl" to jiraUrl,
                    "username" to jiraUsername,
                    "apiToken" to jiraToken,
                ),
            ).`when`()
            .post("/api/v1/admin/integrations/jira/config")
            .then()
            .statusCode(200)
            .body("data.configured", equalTo(true))
            .body("data.url", equalTo(jiraUrl))

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
            .body("data.content", containsString(projectId))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/reports/execution?format=pdf")
            .then()
            .statusCode(200)
            .body("data.format", equalTo("pdf"))
            .body("data.content", containsString("PDF_REPORT_FOR_"))

        val reportType = "execution"

        val jobId =
            givenLeader()
                .body(mapOf("type" to reportType))
                .`when`()
                .post("/api/v1/projects/$projectId/reports/jobs")
                .then()
                .statusCode(200)
                .body("data.type", equalTo(reportType))
                .body("data.projectId", equalTo(projectId))
                .body("data.status", equalTo("QUEUED"))
                .extractId()

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/reports/jobs/$jobId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(jobId))
            .body("data.type", equalTo(reportType))
            .body("data.projectId", equalTo(projectId))
            .body("data.status", equalTo("QUEUED"))

        givenLeader()
            .`when`()
            .delete("/api/v1/projects/$projectId/reports/jobs/$jobId")
            .then()
            .statusCode(200)
            .body("data.status", equalTo("CANCELLED"))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/reports/jobs/$jobId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(jobId))
            .body("data.status", equalTo("CANCELLED"))

        givenAdmin()
            .`when`()
            .get("/api/v1/admin/audit-logs")
            .then()
            .statusCode(200)
            .body("data.metadata", hasItem("REPORT_JOB_CREATED"))
            .body("data.metadata", hasItem("REPORT_JOB_CANCELLED"))
    }
}
