package com.amazonqa.api

import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test

class AdminUserAccessApiIntegrationTest : ApiIntegrationTestBase() {
    @Test
    fun `admin can manage user lifecycle`() {
        val fullName = faker.name().fullName()
        val email = "${faker.internet().username()}-${System.nanoTime()}@example.com"

        val userId =
            givenAdmin()
                .body(mapOf("fullName" to fullName, "email" to email))
                .`when`()
                .post("/api/v1/admin/users")
                .then()
                .statusCode(200)
                .body("data.fullName", equalTo(fullName))
                .body("data.email", equalTo(email))
                .body("data.status", equalTo("ACTIVE"))
                .extractId()

        givenAdmin()
            .`when`()
            .get("/api/v1/admin/users/$userId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(userId))
            .body("data.fullName", equalTo(fullName))
            .body("data.email", equalTo(email))
            .body("data.status", equalTo("ACTIVE"))

        val updatedName = "${faker.name().firstName()} QA"
        givenAdmin()
            .body(mapOf("fullName" to updatedName))
            .`when`()
            .patch("/api/v1/admin/users/$userId")
            .then()
            .statusCode(200)
            .body("data.fullName", equalTo(updatedName))

        givenAdmin()
            .`when`()
            .get("/api/v1/admin/users/$userId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(userId))
            .body("data.fullName", equalTo(updatedName))
            .body("data.email", equalTo(email))

        givenAdmin()
            .body(mapOf("status" to "INACTIVE"))
            .`when`()
            .patch("/api/v1/admin/users/$userId/status")
            .then()
            .statusCode(200)
            .body("data.status", equalTo("INACTIVE"))

        givenAdmin()
            .`when`()
            .get("/api/v1/admin/users/$userId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(userId))
            .body("data.status", equalTo("INACTIVE"))

        givenAdmin()
            .`when`()
            .get("/api/v1/admin/users")
            .then()
            .statusCode(200)
            .body("data.id", hasItem(userId))
            .body("data.email", hasItem(email))
            .body("data.fullName", hasItem(updatedName))
            .body("data.status", hasItem("INACTIVE"))

        givenAdmin()
            .`when`()
            .delete("/api/v1/admin/users/$userId")
            .then()
            .statusCode(200)
            .body("data.status", equalTo("DELETED"))

        givenAdmin()
            .`when`()
            .get("/api/v1/admin/users/$userId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(userId))
            .body("data.fullName", equalTo(updatedName))
            .body("data.email", equalTo(email))
            .body("data.status", equalTo("DELETED"))
    }

    @Test
    fun `admin can list roles and permissions`() {
        givenAdmin()
            .`when`()
            .get("/api/v1/admin/roles")
            .then()
            .statusCode(200)
            .body("data", hasItems("ADMIN", "LEADER", "TESTER", "GUEST"))

        givenAdmin()
            .`when`()
            .get("/api/v1/admin/permissions")
            .then()
            .statusCode(200)
            .body("data", hasItem("PROJECT_READ"))
            .body("data", hasItem("*"))
    }

    @Test
    fun `admin can manage global and scoped roles`() {
        val userId = createUser()
        val projectId = createProject()

        givenAdmin()
            .`when`()
            .get("/api/v1/admin/users/$userId/roles")
            .then()
            .statusCode(200)
            .body("data.role", hasItem("GUEST"))
            .body("data.scopeType", hasItem("GLOBAL"))

        givenAdmin()
            .body(mapOf("role" to "LEADER"))
            .`when`()
            .post("/api/v1/admin/users/$userId/roles")
            .then()
            .statusCode(200)
            .body("data.role", hasItems("GUEST", "LEADER"))

        givenAdmin()
            .body(mapOf("role" to "TESTER", "scopeId" to projectId))
            .`when`()
            .post("/api/v1/admin/users/$userId/scoped-roles")
            .then()
            .statusCode(200)
            .body("data.role", hasItems("GUEST", "LEADER", "TESTER"))
            .body("data.scopeId", hasItem(projectId))

        givenAdmin()
            .`when`()
            .get("/api/v1/admin/users/$userId/effective-permissions")
            .then()
            .statusCode(200)
            .body("data", hasItems("PROJECT_WRITE", "EXECUTION_WRITE", "PROJECT_READ"))

        givenAdmin()
            .`when`()
            .delete("/api/v1/admin/users/$userId/roles/LEADER")
            .then()
            .statusCode(200)

        givenAdmin()
            .`when`()
            .get("/api/v1/admin/users/$userId/roles")
            .then()
            .statusCode(200)
            .body("data.role", not(hasItem("LEADER")))

        givenAdmin()
            .body(mapOf("scopeId" to projectId))
            .`when`()
            .delete("/api/v1/admin/users/$userId/scoped-roles/TESTER")
            .then()
            .statusCode(200)

        givenAdmin()
            .`when`()
            .get("/api/v1/admin/users/$userId/roles")
            .then()
            .statusCode(200)
            .body("data.scopeId", not(hasItem(projectId)))
            .body("data.role", hasItem("GUEST"))
    }

    @Test
    fun `tester cannot access admin users`() {
        givenTester()
            .`when`()
            .get("/api/v1/admin/users")
            .then()
            .statusCode(403)
            .body("error.code", equalTo("FORBIDDEN"))
    }
}
