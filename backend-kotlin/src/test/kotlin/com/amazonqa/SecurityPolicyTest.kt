package com.amazonqa

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
    properties = [
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
    ],
)
@AutoConfigureMockMvc
class SecurityPolicyTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `guest cannot mutate build`() {
        val body = """{"name":"Build Guest"}"""
        mockMvc.perform(
            post("/api/v1/projects/10000000-0000-0000-0000-000000000001/builds")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer guest-token")
                .content(body),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `tester can update execution status`() {
        val createBuildBody = """{"name":"Build One"}"""
        val buildResult =
            mockMvc.perform(
                post("/api/v1/projects/10000000-0000-0000-0000-000000000001/builds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer admin-token")
                    .content(createBuildBody),
            ).andExpect(status().isOk).andReturn().response.contentAsString

        val buildId =
            Regex("\\\"id\\\":\\\"([^\\\"]+)\\\"").find(buildResult)?.groupValues?.get(1)
                ?: throw IllegalStateException("Build id not found")

        val planBody = """{"name":"Plan One"}"""
        val planResult =
            mockMvc.perform(
                post("/api/v1/projects/10000000-0000-0000-0000-000000000001/test-plans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer admin-token")
                    .content(planBody),
            ).andExpect(status().isOk).andReturn().response.contentAsString

        val planId =
            Regex("\\\"id\\\":\\\"([^\\\"]+)\\\"").find(planResult)?.groupValues?.get(1)
                ?: throw IllegalStateException("Plan id not found")

        val runBody = """{"buildId":"$buildId"}"""
        val runResult =
            mockMvc.perform(
                post("/api/v1/projects/10000000-0000-0000-0000-000000000001/test-plans/$planId/runs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer admin-token")
                    .content(runBody),
            ).andExpect(status().isOk).andReturn().response.contentAsString

        val executionId =
            Regex("\\\"id\\\":\\\"([^\\\"]+)\\\"").find(runResult)?.groupValues?.get(1)
                ?: throw IllegalStateException("Execution id not found")

        val statusBody = """{"status":"PASSED","actualResult":"done"}"""
        mockMvc.perform(
            post("/api/v1/executions/$executionId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer tester-token")
                .content(statusBody),
        ).andExpect(status().isOk)
    }

    @Test
    fun `closed build blocks execution update`() {
        val buildBody = """{"name":"Build Close"}"""
        val buildResult =
            mockMvc.perform(
                post("/api/v1/projects/10000000-0000-0000-0000-000000000001/builds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer admin-token")
                    .content(buildBody),
            ).andExpect(status().isOk).andReturn().response.contentAsString

        val buildId =
            Regex("\\\"id\\\":\\\"([^\\\"]+)\\\"").find(buildResult)?.groupValues?.get(1)
                ?: throw IllegalStateException("Build id not found")

        mockMvc.perform(
            patch("/api/v1/builds/$buildId/close")
                .header("Authorization", "Bearer admin-token"),
        ).andExpect(status().isOk)

        val planBody = """{"name":"Plan Closed"}"""
        val planResult =
            mockMvc.perform(
                post("/api/v1/projects/10000000-0000-0000-0000-000000000001/test-plans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer admin-token")
                    .content(planBody),
            ).andExpect(status().isOk).andReturn().response.contentAsString

        val planId =
            Regex("\\\"id\\\":\\\"([^\\\"]+)\\\"").find(planResult)?.groupValues?.get(1)
                ?: throw IllegalStateException("Plan id not found")

        val runBody = """{"buildId":"$buildId"}"""
        val runResult =
            mockMvc.perform(
                post("/api/v1/projects/10000000-0000-0000-0000-000000000001/test-plans/$planId/runs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer admin-token")
                    .content(runBody),
            ).andExpect(status().isOk).andReturn().response.contentAsString

        val executionId =
            Regex("\\\"id\\\":\\\"([^\\\"]+)\\\"").find(runResult)?.groupValues?.get(1)
                ?: throw IllegalStateException("Execution id not found")

        val statusBody = """{"status":"PASSED","actualResult":"done"}"""
        mockMvc.perform(
            post("/api/v1/executions/$executionId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer tester-token")
                .content(statusBody),
        ).andExpect(status().isConflict)
    }
}
