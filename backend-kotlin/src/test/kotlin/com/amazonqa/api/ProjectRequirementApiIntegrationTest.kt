package com.amazonqa.api

import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Test

class ProjectRequirementApiIntegrationTest : ApiIntegrationTestBase() {
    @Test
    fun `project CRUD with restore works`() {
        val projectName = "Project ${faker.company().name()}"

        val projectId =
            givenLeader()
                .body(mapOf("name" to projectName))
                .`when`()
                .post("/api/v1/projects")
                .then()
                .statusCode(200)
                .body("data.name", equalTo(projectName))
                .extractId()

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(projectId))

        givenGuest()
            .`when`()
            .get("/api/v1/projects")
            .then()
            .statusCode(200)
            .body("data.id", hasItem(projectId))

        val updatedName = "$projectName Updated"
        givenLeader()
            .body(mapOf("name" to updatedName))
            .`when`()
            .patch("/api/v1/projects/$projectId")
            .then()
            .statusCode(200)
            .body("data.name", equalTo(updatedName))

        givenLeader()
            .`when`()
            .delete("/api/v1/projects/$projectId")
            .then()
            .statusCode(200)
            .body("data.status", equalTo("ARCHIVED"))

        givenLeader()
            .`when`()
            .post("/api/v1/projects/$projectId/restore")
            .then()
            .statusCode(200)
            .body("data.status", equalTo("ACTIVE"))
    }

    @Test
    fun `requirement CRUD import and reporting endpoints work`() {
        val projectId = createProject()
        val title = "REQ-${faker.number().digits(7)}"

        val requirementId =
            givenTester()
                .body(mapOf("title" to title))
                .`when`()
                .post("/api/v1/projects/$projectId/requirements")
                .then()
                .statusCode(200)
                .body("data.title", equalTo(title))
                .extractId()

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/requirements")
            .then()
            .statusCode(200)
            .body("data.size()", equalTo(1))

        val updatedTitle = "$title-updated"
        givenTester()
            .body(mapOf("title" to updatedTitle))
            .`when`()
            .patch("/api/v1/projects/$projectId/requirements/$requirementId")
            .then()
            .statusCode(200)
            .body("data.title", equalTo(updatedTitle))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/requirements/$requirementId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(requirementId))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/requirements/$requirementId/coverage")
            .then()
            .statusCode(200)
            .body("data.status", equalTo("COVERED"))

        givenLeader()
            .body(mapOf("format" to "csv", "items" to listOf("REQ-A", "REQ-B")))
            .`when`()
            .post("/api/v1/projects/$projectId/requirements/import")
            .then()
            .statusCode(200)
            .body("data.imported", equalTo(2))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/traceability-matrix")
            .then()
            .statusCode(200)
            .body("data.projectId", equalTo(projectId))

        givenLeader()
            .`when`()
            .delete("/api/v1/projects/$projectId/requirements/$requirementId")
            .then()
            .statusCode(200)

        givenLeader()
            .`when`()
            .post("/api/v1/projects/$projectId/requirements/$requirementId/restore")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(requirementId))
    }
}
