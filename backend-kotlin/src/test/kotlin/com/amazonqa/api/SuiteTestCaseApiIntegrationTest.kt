package com.amazonqa.api

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Assertions.assertArrayEquals
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
            .body("data.name", equalTo(suiteName))
            .body("data.deletedAt", nullValue())

        val updatedSuiteName = "$suiteName Updated"

        givenTester()
            .body(mapOf("name" to updatedSuiteName))
            .`when`()
            .patch("/api/v1/projects/$projectId/suites/$suiteId")
            .then()
            .statusCode(200)
            .body("data.name", equalTo(updatedSuiteName))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/suites/$suiteId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(suiteId))
            .body("data.name", equalTo(updatedSuiteName))
            .body("data.deletedAt", nullValue())

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/suites/tree")
            .then()
            .statusCode(200)
            .body("data.id", hasItem(suiteId))
            .body("data.name", hasItem(updatedSuiteName))

        givenLeader()
            .`when`()
            .delete("/api/v1/projects/$projectId/suites/$suiteId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(suiteId))
            .body("data.deletedAt", notNullValue())

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/suites/$suiteId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(suiteId))
            .body("data.name", equalTo(updatedSuiteName))
            .body("data.deletedAt", notNullValue())

        givenLeader()
            .`when`()
            .post("/api/v1/projects/$projectId/suites/$suiteId/restore")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(suiteId))
            .body("data.deletedAt", nullValue())

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/suites/$suiteId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(suiteId))
            .body("data.name", equalTo(updatedSuiteName))
            .body("data.deletedAt", nullValue())
    }

    @Test
    fun `test case CRUD version bulk and search endpoints work`() {
        val projectId = createProject()
        val title = "TC-${faker.number().digits(8)}"
        val testId = "AMQA-${faker.number().digits(3)}"
        val priority = "High"
        val bugSeverity = "Critical"
        val tagsKeywords = "smoke,api"
        val requirementLink = "REQ-${faker.number().digits(6)}"
        val executionType = "Automated"
        val testCaseStatus = "Ready for Review"
        val platform = "Web"
        val testEnvironment = "Staging"
        val preconditions = "User authenticated"
        val actions = "Submit form"
        val expectedResult = "Submission succeeds"
        val executionStatus = "Not Run"
        val notes = "Critical checkout flow"
        val attachments = "https://files.example.com/${faker.internet().uuid()}"

        val testCaseId =
            givenTester()
                .body(
                    mapOf(
                        "title" to title,
                        "testId" to testId,
                        "priority" to priority,
                        "bugSeverity" to bugSeverity,
                        "tagsKeywords" to tagsKeywords,
                        "requirementLink" to requirementLink,
                        "executionType" to executionType,
                        "testCaseStatus" to testCaseStatus,
                        "platform" to platform,
                        "testEnvironment" to testEnvironment,
                        "preconditions" to preconditions,
                        "actions" to actions,
                        "expectedResult" to expectedResult,
                        "executionStatus" to executionStatus,
                        "notes" to notes,
                        "attachments" to attachments,
                    ),
                )
                .`when`()
                .post("/api/v1/projects/$projectId/test-cases")
                .then()
                .statusCode(200)
                .body("data.title", equalTo(title))
                .body("data.testId", equalTo(testId))
                .body("data.priority", equalTo(priority))
                .body("data.bugSeverity", equalTo(bugSeverity))
                .body("data.executionType", equalTo(executionType))
                .body("data.testCaseStatus", equalTo(testCaseStatus))
                .body("data.executionStatus", equalTo(executionStatus))
                .extractId()

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/test-cases")
            .then()
            .statusCode(200)
            .body("data.id", hasItem(testCaseId))
            .body("data.title", hasItem(title))
            .body("data.testId", hasItem(testId))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/test-cases/$testCaseId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(testCaseId))
            .body("data.title", equalTo(title))
            .body("data.testId", equalTo(testId))
            .body("data.priority", equalTo(priority))
            .body("data.bugSeverity", equalTo(bugSeverity))
            .body("data.tagsKeywords", equalTo(tagsKeywords))
            .body("data.requirementLink", equalTo(requirementLink))
            .body("data.executionType", equalTo(executionType))
            .body("data.testCaseStatus", equalTo(testCaseStatus))
            .body("data.platform", equalTo(platform))
            .body("data.testEnvironment", equalTo(testEnvironment))
            .body("data.preconditions", equalTo(preconditions))
            .body("data.actions", equalTo(actions))
            .body("data.expectedResult", equalTo(expectedResult))
            .body("data.executionStatus", equalTo(executionStatus))
            .body("data.notes", equalTo(notes))
            .body("data.attachments", equalTo(attachments))
            .body("data.deletedAt", nullValue())

        val updatedTitle = "$title Updated"
        givenTester()
            .body(mapOf("title" to updatedTitle))
            .`when`()
            .patch("/api/v1/projects/$projectId/test-cases/$testCaseId")
            .then()
            .statusCode(200)
            .body("data.title", equalTo(updatedTitle))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/test-cases/$testCaseId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(testCaseId))
            .body("data.title", equalTo(updatedTitle))
            .body("data.testId", equalTo(testId))
            .body("data.priority", equalTo(priority))
            .body("data.bugSeverity", equalTo(bugSeverity))

        val v2Title = "$updatedTitle v2"
        val v2Id =
            givenTester()
                .body(mapOf("title" to v2Title))
                .`when`()
                .post("/api/v1/projects/$projectId/test-cases/$testCaseId/versions")
                .then()
                .statusCode(200)
                .body("data.title", equalTo(v2Title))
                .body("data.version", greaterThanOrEqualTo(2))
                .extractId()

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/test-cases/$v2Id")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(v2Id))
            .body("data.title", equalTo(v2Title))
            .body("data.testId", equalTo(testId))
            .body("data.version", greaterThanOrEqualTo(2))

        val bulkSuffix = "-bulk"
        givenLeader()
            .body(mapOf("titleSuffix" to bulkSuffix))
            .`when`()
            .patch("/api/v1/projects/$projectId/test-cases/bulk")
            .then()
            .statusCode(200)
            .body("data.size()", greaterThanOrEqualTo(1))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/test-cases/$testCaseId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(testCaseId))
            .body("data.title", equalTo("$updatedTitle$bulkSuffix"))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/test-cases/$v2Id")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(v2Id))
            .body("data.title", equalTo("$v2Title$bulkSuffix"))

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/test-cases/search?query=bulk")
            .then()
            .statusCode(200)
            .body("data.id", hasItems(testCaseId, v2Id))

        givenLeader()
            .`when`()
            .delete("/api/v1/projects/$projectId/test-cases/$testCaseId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(testCaseId))
            .body("data.deletedAt", notNullValue())

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/test-cases/$testCaseId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(testCaseId))
            .body("data.deletedAt", notNullValue())

        givenLeader()
            .`when`()
            .post("/api/v1/projects/$projectId/test-cases/$testCaseId/restore")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(testCaseId))
            .body("data.deletedAt", nullValue())

        givenGuest()
            .`when`()
            .get("/api/v1/projects/$projectId/test-cases/$testCaseId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(testCaseId))
            .body("data.title", equalTo("$updatedTitle$bulkSuffix"))
            .body("data.deletedAt", nullValue())
    }

    @Test
    fun `upload and download test case attachment works`() {
        val projectId = createProject()
        val testCaseId = createTestCase(projectId)
        val fileBytes = "fake-image-content".toByteArray()

        val attachmentId =
            RestAssured
                .given()
                .header("Authorization", "Bearer tester-token")
                .header("X-Trace-Id", "it-${System.nanoTime()}")
                .accept(ContentType.JSON)
                .multiPart("file", "evidence.png", fileBytes, "image/png")
                .`when`()
                .post("/api/v1/projects/$projectId/test-cases/$testCaseId/attachments")
                .then()
                .statusCode(200)
                .body("data.fileName", equalTo("evidence.png"))
                .body("data.contentType", equalTo("image/png"))
                .body("data.fileSize", equalTo(fileBytes.size))
                .extract()
                .path<String>("data.id")

        val downloadedBytes =
            givenGuest()
                .accept("*/*")
                .`when`()
                .get("/api/v1/projects/$projectId/test-cases/$testCaseId/attachments/$attachmentId/download")
                .then()
                .statusCode(200)
                .contentType("image/png")
                .extract()
                .asByteArray()

        assertArrayEquals(fileBytes, downloadedBytes)
    }

    @Test
    fun `attachment upload rejects unsupported type and files larger than 1MB`() {
        val projectId = createProject()
        val testCaseId = createTestCase(projectId)

        RestAssured
            .given()
            .header("Authorization", "Bearer tester-token")
            .header("X-Trace-Id", "it-${System.nanoTime()}")
            .accept(ContentType.JSON)
            .multiPart("file", "spec.pdf", "invalid".toByteArray(), "application/pdf")
            .`when`()
            .post("/api/v1/projects/$projectId/test-cases/$testCaseId/attachments")
            .then()
            .statusCode(400)
            .body("error.code", equalTo("ATTACHMENT_TYPE_NOT_ALLOWED"))

        val oversizedBytes = ByteArray(1_048_577) { 'A'.code.toByte() }
        RestAssured
            .given()
            .header("Authorization", "Bearer tester-token")
            .header("X-Trace-Id", "it-${System.nanoTime()}")
            .accept(ContentType.JSON)
            .multiPart("file", "dataset.csv", oversizedBytes, "text/csv")
            .`when`()
            .post("/api/v1/projects/$projectId/test-cases/$testCaseId/attachments")
            .then()
            .statusCode(400)
            .body("error.code", equalTo("ATTACHMENT_TOO_LARGE"))
    }
}
