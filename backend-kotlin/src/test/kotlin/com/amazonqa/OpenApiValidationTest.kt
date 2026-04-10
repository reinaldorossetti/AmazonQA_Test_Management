package com.amazonqa

import io.swagger.parser.OpenAPIParser
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class OpenApiValidationTest {
    @Test
    fun `openapi file is valid and contains v1 paths`() {
        val file = File("docs/api/openapi.yaml")
        assertTrue(file.exists(), "OpenAPI file does not exist")

        val result = OpenAPIParser().readLocation(file.absolutePath, null, null)
        assertNotNull(result.openAPI, "OpenAPI parser could not parse the contract")

        val paths = result.openAPI.paths.keys
        assertTrue(paths.contains("/projects/{projectId}/requirements"))
        assertTrue(paths.contains("/admin/audit-logs"))
        assertTrue(paths.contains("/executions/{executionId}/status"))
    }
}
