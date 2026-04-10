package com.amazonqa.api

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.response.ValidatableResponse
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
    ],
)
abstract class ApiIntegrationTestBase {
    protected val faker: Faker = Faker()

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun configureRestAssured() {
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port
    }

    protected fun givenAdmin() = givenToken("admin-token")

    protected fun givenLeader() = givenToken("leader-token")

    protected fun givenTester() = givenToken("tester-token")

    protected fun givenGuest() = givenToken("guest-token")

    protected fun givenToken(token: String) =
        Given {
            header("Authorization", "Bearer $token")
            header("X-Trace-Id", "it-${System.nanoTime()}")
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        }

    protected fun createProject(name: String = "Project ${faker.book().title()}"): String =
        givenAdmin()
            .body(mapOf("name" to name))
            .`when`()
            .post("/api/v1/projects")
            .then()
            .statusCode(200)
            .extractId()

    protected fun createUser(
        fullName: String = faker.name().fullName(),
        email: String = "${faker.internet().username()}-${System.nanoTime()}@example.com",
    ): String =
        givenAdmin()
            .body(mapOf("fullName" to fullName, "email" to email))
            .`when`()
            .post("/api/v1/admin/users")
            .then()
            .statusCode(200)
            .extractId()

    protected fun createRequirement(
        projectId: String,
        title: String = "REQ-${faker.number().digits(5)}",
    ): String =
        givenAdmin()
            .body(mapOf("title" to title))
            .`when`()
            .post("/api/v1/projects/$projectId/requirements")
            .then()
            .statusCode(200)
            .extractId()

    protected fun createSuite(
        projectId: String,
        name: String = "Suite ${faker.book().genre()}",
    ): String =
        givenAdmin()
            .body(mapOf("name" to name))
            .`when`()
            .post("/api/v1/projects/$projectId/suites")
            .then()
            .statusCode(200)
            .extractId()

    protected fun createTestCase(
        projectId: String,
        title: String = "TC-${faker.number().digits(6)}",
    ): String =
        givenAdmin()
            .body(mapOf("title" to title))
            .`when`()
            .post("/api/v1/projects/$projectId/test-cases")
            .then()
            .statusCode(200)
            .extractId()

    protected fun createBuild(
        projectId: String,
        name: String = "Build ${faker.app().name()}",
    ): String =
        givenAdmin()
            .body(mapOf("name" to name))
            .`when`()
            .post("/api/v1/projects/$projectId/builds")
            .then()
            .statusCode(200)
            .extractId()

    protected fun createPlan(
        projectId: String,
        name: String = "Plan ${faker.company().industry()}",
    ): String =
        givenAdmin()
            .body(mapOf("name" to name))
            .`when`()
            .post("/api/v1/projects/$projectId/test-plans")
            .then()
            .statusCode(200)
            .extractId()

    protected fun createRun(
        projectId: String,
        planId: String,
        buildId: String,
    ): String =
        givenAdmin()
            .body(mapOf("buildId" to buildId))
            .`when`()
            .post("/api/v1/projects/$projectId/test-plans/$planId/runs")
            .then()
            .statusCode(200)
            .extractId()

    protected fun createDefect(
        executionId: String,
        title: String = "Defect ${faker.harryPotter().spell()}",
    ): String =
        givenAdmin()
            .body(mapOf("title" to title))
            .`when`()
            .post("/api/v1/executions/$executionId/defects/jira")
            .then()
            .statusCode(200)
            .extractId()

    protected fun ValidatableResponse.extractId(): String =
        extract().path<String>("data.id")
            ?: throw IllegalStateException("Response did not return data.id")
}
