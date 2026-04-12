package com.amazonqa

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@SpringBootTest(
    properties = [
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
    ],
)
class EndpointContractCoverageTest {
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private lateinit var handlerMapping: RequestMappingHandlerMapping

    @Test
    fun `all v1 endpoints are mapped`() {
        val mapped =
            handlerMapping.handlerMethods.keys.flatMap { info ->
                val methods = info.methodsCondition.methods.map { it.name }
                val patterns =
                    info.pathPatternsCondition?.patterns?.map { it.patternString }
                        ?: info.patternValues.toList()
                patterns.flatMap { pattern ->
                    if (methods.isEmpty()) {
                        listOf("ANY $pattern")
                    } else {
                        methods.map { method -> "$method $pattern" }
                    }
                }
            }.toSet()

        val expected =
            setOf(
                "POST /api/v1/auth/login",
                "POST /api/v1/auth/refresh",
                "POST /api/v1/auth/logout",
                "POST /api/v1/users/register",
                "GET /api/v1/users/me",
                "PATCH /api/v1/users/me/preferences",
                "POST /api/v1/admin/users",
                "POST /api/v1/admin/users/full",
                "GET /api/v1/admin/users",
                "GET /api/v1/admin/users/{userId}",
                "PATCH /api/v1/admin/users/{userId}",
                "PATCH /api/v1/admin/users/{userId}/status",
                "DELETE /api/v1/admin/users/{userId}",
                "GET /api/v1/admin/users/{userId}/profile",
                "PATCH /api/v1/admin/users/{userId}/profile",
                "PATCH /api/v1/admin/users/{userId}/address",
                "GET /api/v1/admin/roles",
                "GET /api/v1/admin/permissions",
                "GET /api/v1/admin/users/{userId}/roles",
                "POST /api/v1/admin/users/{userId}/roles",
                "DELETE /api/v1/admin/users/{userId}/roles/{role}",
                "POST /api/v1/admin/users/{userId}/scoped-roles",
                "DELETE /api/v1/admin/users/{userId}/scoped-roles/{role}",
                "GET /api/v1/admin/users/{userId}/effective-permissions",
                "POST /api/v1/projects",
                "GET /api/v1/projects",
                "GET /api/v1/projects/{projectId}",
                "PATCH /api/v1/projects/{projectId}",
                "DELETE /api/v1/projects/{projectId}",
                "POST /api/v1/projects/{projectId}/restore",
                "POST /api/v1/projects/{projectId}/requirements",
                "POST /api/v1/projects/{projectId}/requirements/import",
                "GET /api/v1/projects/{projectId}/requirements",
                "GET /api/v1/projects/{projectId}/requirements/{requirementId}",
                "PATCH /api/v1/projects/{projectId}/requirements/{requirementId}",
                "DELETE /api/v1/projects/{projectId}/requirements/{requirementId}",
                "POST /api/v1/projects/{projectId}/requirements/{requirementId}/restore",
                "GET /api/v1/projects/{projectId}/requirements/{requirementId}/coverage",
                "GET /api/v1/projects/{projectId}/traceability-matrix",
                "POST /api/v1/projects/{projectId}/suites",
                "GET /api/v1/projects/{projectId}/suites/{suiteId}",
                "PATCH /api/v1/projects/{projectId}/suites/{suiteId}",
                "DELETE /api/v1/projects/{projectId}/suites/{suiteId}",
                "POST /api/v1/projects/{projectId}/suites/{suiteId}/restore",
                "GET /api/v1/projects/{projectId}/suites/tree",
                "POST /api/v1/projects/{projectId}/test-cases",
                "GET /api/v1/projects/{projectId}/test-cases",
                "GET /api/v1/projects/{projectId}/test-cases/{testCaseId}",
                "PATCH /api/v1/projects/{projectId}/test-cases/{testCaseId}",
                "DELETE /api/v1/projects/{projectId}/test-cases/{testCaseId}",
                "POST /api/v1/projects/{projectId}/test-cases/{testCaseId}/restore",
                "POST /api/v1/projects/{projectId}/test-cases/{testCaseId}/versions",
                "PATCH /api/v1/projects/{projectId}/test-cases/bulk",
                "POST /api/v1/projects/{projectId}/builds",
                "GET /api/v1/projects/{projectId}/builds/{buildId}",
                "PATCH /api/v1/projects/{projectId}/builds/{buildId}",
                "DELETE /api/v1/projects/{projectId}/builds/{buildId}",
                "POST /api/v1/projects/{projectId}/test-plans",
                "GET /api/v1/projects/{projectId}/test-plans/{planId}",
                "PATCH /api/v1/projects/{projectId}/test-plans/{planId}",
                "DELETE /api/v1/projects/{projectId}/test-plans/{planId}",
                "POST /api/v1/projects/{projectId}/test-plans/{planId}/runs",
                "POST /api/v1/executions/{executionId}/status",
                "POST /api/v1/executions/{executionId}/attachments",
                "PATCH /api/v1/builds/{buildId}/close",
                "GET /api/v1/projects/{projectId}/defects",
                "GET /api/v1/projects/{projectId}/defects/{defectId}",
                "POST /api/v1/executions/{executionId}/defects/jira",
                "PATCH /api/v1/projects/{projectId}/defects/{defectId}",
                "DELETE /api/v1/projects/{projectId}/defects/{defectId}",
                "POST /api/v1/admin/integrations/jira/config",
                "GET /api/v1/admin/integrations/jira/verify",
                "GET /api/v1/projects/{projectId}/metrics",
                "GET /api/v1/projects/{projectId}/reports/coverage",
                "GET /api/v1/projects/{projectId}/reports/execution",
                "POST /api/v1/projects/{projectId}/reports/jobs",
                "GET /api/v1/projects/{projectId}/reports/jobs/{jobId}",
                "DELETE /api/v1/projects/{projectId}/reports/jobs/{jobId}",
                "GET /api/v1/admin/audit-logs",
            )

        val missing = expected.filterNot { mapped.contains(it) }
        assertTrue(missing.isEmpty(), "Missing endpoint mappings: $missing")
    }
}
