package com.amazonqa

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers(disabledWithoutDocker = true)
class PostgresContainerSmokeTest {
    @Container
    private val postgres = PostgreSQLContainer("postgres:16-alpine")

    @Test
    fun `postgres container starts`() {
        assertTrue(postgres.isRunning)
        assertTrue(postgres.jdbcUrl.contains("postgresql"))
    }
}
