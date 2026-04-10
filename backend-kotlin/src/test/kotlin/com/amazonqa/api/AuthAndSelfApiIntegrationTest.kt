package com.amazonqa.api

import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test

class AuthAndSelfApiIntegrationTest : ApiIntegrationTestBase() {
    @Test
    fun `login refresh and logout flow works`() {
        val refreshToken =
            givenGuest()
                .body(mapOf("email" to "admin@amazonqa.local", "password" to "strong-pass"))
                .`when`()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .body("data.accessToken", equalTo("admin-token"))
                .body("data.tokenType", equalTo("Bearer"))
                .extract()
                .path<String>("data.refreshToken")

        givenGuest()
            .body(mapOf("refreshToken" to refreshToken))
            .`when`()
            .post("/api/v1/auth/refresh")
            .then()
            .statusCode(200)
            .body("data.accessToken", equalTo("admin-token"))
            .body("data.refreshToken", equalTo(refreshToken))

        givenAdmin()
            .`when`()
            .post("/api/v1/auth/logout")
            .then()
            .statusCode(200)
            .body("data.message", equalTo("Logged out"))
            .body("traceId", notNullValue())
    }

    @Test
    fun `get current user and update preferences`() {
        givenTester()
            .`when`()
            .get("/api/v1/users/me")
            .then()
            .statusCode(200)
            .body("data.username", equalTo("tester@amazonqa.local"))

        givenAdmin()
            .body(mapOf("preferences" to mapOf("theme" to "dark", "locale" to "pt-BR")))
            .`when`()
            .patch("/api/v1/users/me/preferences")
            .then()
            .statusCode(200)
            .body("data.theme", equalTo("dark"))
            .body("data.locale", equalTo("pt-BR"))
    }
}
