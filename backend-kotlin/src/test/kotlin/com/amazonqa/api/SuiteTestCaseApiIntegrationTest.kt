package com.amazonqa.api

import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
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
        val testCaseStatus = "Ready"
        val platform = "Web"
        val testEnvironment = "Staging"
        val preconditions = "User authenticated"
        val actions = "Submit form"
        val expectedResult = "Submission succeeds"
        val actualResult = "N/A"
        val executionStatus = "Not Run"
        val notes = "Critical checkout flow"
        val customFields = "{\"owner\":\"qa-team\",\"module\":\"checkout\"}"
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
                        "actualResult" to actualResult,
                        "executionStatus" to executionStatus,
                        "notes" to notes,
                        "customFields" to customFields,
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
            .body("data.actualResult", equalTo(actualResult))
            .body("data.executionStatus", equalTo(executionStatus))
            .body("data.notes", equalTo(notes))
            .body("data.customFields", equalTo(customFields))
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
}
