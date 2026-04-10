package com.amazonqa.api

import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Test

class SuiteTestCaseApiIntegrationTest : ApiIntegrationTestBase() {
    @Test
    fun `suite CRUD and tree endpoints work`() {
        val projectId = createProject()
        val suiteName = "Suite ${faker.book().title()}"

        val suiteId =
            givenTester()
                .body(mapOf("name" to suiteName))
                .`when`()
                .post("/api/v1/projects/$projectId/suites")
                .then()
                .statusCode(200)
                .body("data.name", equalTo(suiteName))
                .extractId()

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/suites/$suiteId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(suiteId))

        givenTester()
            .body(mapOf("name" to "$suiteName Updated"))
            .`when`()
            .patch("/api/v1/projects/$projectId/suites/$suiteId")
            .then()
            .statusCode(200)
            .body("data.name", equalTo("$suiteName Updated"))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/suites/tree")
            .then()
            .statusCode(200)
            .body("data.id", hasItem(suiteId))

        givenLeader()
            .`when`()
            .delete("/api/v1/projects/$projectId/suites/$suiteId")
            .then()
            .statusCode(200)

        givenLeader()
            .`when`()
            .post("/api/v1/projects/$projectId/suites/$suiteId/restore")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(suiteId))
    }

    @Test
    fun `test case CRUD version bulk and search endpoints work`() {
        val projectId = createProject()
        val title = "TC-${faker.number().digits(8)}"

        val testCaseId =
            givenTester()
                .body(mapOf("title" to title))
                .`when`()
                .post("/api/v1/projects/$projectId/test-cases")
                .then()
                .statusCode(200)
                .body("data.title", equalTo(title))
                .extractId()

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/test-cases")
            .then()
            .statusCode(200)
            .body("data.id", hasItem(testCaseId))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/test-cases/$testCaseId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(testCaseId))

        val updatedTitle = "$title Updated"
        givenTester()
            .body(mapOf("title" to updatedTitle))
            .`when`()
            .patch("/api/v1/projects/$projectId/test-cases/$testCaseId")
            .then()
            .statusCode(200)
            .body("data.title", equalTo(updatedTitle))

        val v2Id =
            givenTester()
                .body(mapOf("title" to "$updatedTitle v2"))
                .`when`()
                .post("/api/v1/projects/$projectId/test-cases/$testCaseId/versions")
                .then()
                .statusCode(200)
                .body("data.version", greaterThanOrEqualTo(2))
                .extractId()

        givenLeader()
            .body(mapOf("titleSuffix" to "-bulk"))
            .`when`()
            .patch("/api/v1/projects/$projectId/test-cases/bulk")
            .then()
            .statusCode(200)
            .body("data.size()", greaterThanOrEqualTo(1))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/test-cases/search?query=bulk")
            .then()
            .statusCode(200)
            .body("data.id", hasItem(v2Id))

        givenLeader()
            .`when`()
            .delete("/api/v1/projects/$projectId/test-cases/$testCaseId")
            .then()
            .statusCode(200)

        givenLeader()
            .`when`()
            .post("/api/v1/projects/$projectId/test-cases/$testCaseId/restore")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(testCaseId))
    }
}
