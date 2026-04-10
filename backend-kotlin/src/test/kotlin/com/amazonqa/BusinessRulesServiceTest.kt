package com.amazonqa

import com.amazonqa.common.exception.DomainException
import com.amazonqa.execution.ExecutionService
import com.amazonqa.identity.UserService
import com.amazonqa.planning.BuildService
import com.amazonqa.projects.ProjectService
import com.amazonqa.store.BuildStatus
import com.amazonqa.store.StateStore
import com.amazonqa.store.UserStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
    ],
)
class BusinessRulesServiceTest {
    @Autowired
    private lateinit var buildService: BuildService

    @Autowired
    private lateinit var projectService: ProjectService

    @Autowired
    private lateinit var executionService: ExecutionService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var stateStore: StateStore

    @Test
    fun `delete build only draft`() {
        val projectId = stateStore.projects.keys.first()
        val build = buildService.createBuild(projectId, "Build to delete")
        buildService.updateBuild(build.id, null, BuildStatus.OPEN)

        val ex =
            assertThrows(DomainException::class.java) {
                buildService.deleteDraftBuild(build.id)
            }

        assertEquals("BUILD_DELETE_DRAFT_ONLY", ex.code)
    }

    @Test
    fun `project deletion blocked when active run exists`() {
        val projectId = stateStore.projects.keys.first()
        val build = buildService.createBuild(projectId, "Build for active run")
        val execution = executionService.createRun(projectId, build.id)
        assertEquals(projectId, execution.projectId)

        val ex =
            assertThrows(DomainException::class.java) {
                projectService.deleteProject(projectId, "admin")
            }

        assertEquals("PROJECT_HAS_ACTIVE_RUNS", ex.code)
    }

    @Test
    fun `last admin cannot be deactivated`() {
        val adminUser = stateStore.users.values.first { user -> user.email == "admin@amazonqa.local" }

        val ex =
            assertThrows(DomainException::class.java) {
                userService.changeStatus(adminUser.id, UserStatus.INACTIVE)
            }

        assertEquals("LAST_ADMIN_PROTECTION", ex.code)
    }
}
