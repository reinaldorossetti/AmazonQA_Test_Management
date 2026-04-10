package com.amazonqa.api

import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AdminUserAccessApiIntegrationTest : ApiIntegrationTestBase() {
    @Test
    fun `admin can manage user lifecycle`() {
        val email = "${faker.internet().username()}-${System.nanoTime()}@example.com"

        val userId =
            givenAdmin()
                .body(mapOf("fullName" to faker.name().fullName(), "email" to email))
                .`when`()
                .post("/api/v1/admin/users")
                .then()
                .statusCode(200)
                .body("data.email", equalTo(email))
                .extractId()

        givenAdmin()
            .`when`()
            .get("/api/v1/admin/users/$userId")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(userId))

        val updatedName = "${faker.name().firstName()} QA"
        givenAdmin()
            .body(mapOf("fullName" to updatedName))
            .`when`()
            .patch("/api/v1/admin/users/$userId")
            .then()
            .statusCode(200)
            .body("data.fullName", equalTo(updatedName))

        givenAdmin()
            .body(mapOf("status" to "INACTIVE"))
            .`when`()
            .patch("/api/v1/admin/users/$userId/status")
            .then()
            .statusCode(200)
            .body("data.status", equalTo("INACTIVE"))

        givenAdmin()
            .`when`()
            .get("/api/v1/admin/users")
            .then()
            .statusCode(200)
            .extract()
            .asString()
            .also { payload -> assertTrue(payload.contains(email)) }

        givenAdmin()
            .`when`()
            .delete("/api/v1/admin/users/$userId")
            .then()
            .statusCode(200)
            .body("data.status", equalTo("DELETED"))
    }

    @Test
    fun `admin can list roles and permissions`() {
        val rolesPayload =
            givenAdmin()
                .`when`()
                .get("/api/v1/admin/roles")
                .then()
                .statusCode(200)
                .extract()
                .asString()
        assertTrue(rolesPayload.contains("ADMIN"))

        val permissionsPayload =
            givenAdmin()
                .`when`()
                .get("/api/v1/admin/permissions")
                .then()
                .statusCode(200)
                .extract()
                .asString()
        assertTrue(permissionsPayload.contains("PROJECT_READ"))
    }

    @Test
    fun `admin can manage global and scoped roles`() {
        val userId = createUser()
        val projectId = createProject()

        val initialRoles =
            givenAdmin()
                .`when`()
                .get("/api/v1/admin/users/$userId/roles")
                .then()
                .statusCode(200)
                .extract()
                .asString()
        assertTrue(initialRoles.contains("GUEST"))

        val globalRolesAfterAssign =
            givenAdmin()
                .body(mapOf("role" to "LEADER"))
                .`when`()
                .post("/api/v1/admin/users/$userId/roles")
                .then()
                .statusCode(200)
                .extract()
                .asString()
        assertTrue(globalRolesAfterAssign.contains("LEADER"))

        val scopedRolesAfterAssign =
            givenAdmin()
                .body(mapOf("role" to "TESTER", "scopeId" to projectId))
                .`when`()
                .post("/api/v1/admin/users/$userId/scoped-roles")
                .then()
                .statusCode(200)
                .extract()
                .asString()
        assertTrue(scopedRolesAfterAssign.contains(projectId))

        val effectivePermissions =
            givenAdmin()
                .`when`()
                .get("/api/v1/admin/users/$userId/effective-permissions")
                .then()
                .statusCode(200)
                .extract()
                .asString()
        assertTrue(effectivePermissions.contains("PROJECT_WRITE"))

        givenAdmin()
            .`when`()
            .delete("/api/v1/admin/users/$userId/roles/LEADER")
            .then()
            .statusCode(200)

        givenAdmin()
            .body(mapOf("scopeId" to projectId))
            .`when`()
            .delete("/api/v1/admin/users/$userId/scoped-roles/TESTER")
            .then()
            .statusCode(200)
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
