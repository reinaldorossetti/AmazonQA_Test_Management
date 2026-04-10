package com.amazonqa.testsuite

import com.amazonqa.audit.AuditService
import com.amazonqa.common.exception.DomainException
import com.amazonqa.store.StateStore
import com.amazonqa.store.SuiteRecord
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class SuiteService(
    private val stateStore: StateStore,
    private val auditService: AuditService,
) {
    fun createSuite(
        projectId: UUID,
        name: String,
    ): SuiteRecord {
        ensureProject(projectId)
        val suite = SuiteRecord(id = UUID.randomUUID(), projectId = projectId, name = name)
        stateStore.suites[suite.id] = suite
        auditService.logCreate("SUITE", suite.id.toString(), "SUITE_CREATED")
        return suite
    }

    fun getSuite(suiteId: UUID): SuiteRecord =
        stateStore.suites[suiteId]
            ?: throw DomainException(HttpStatus.NOT_FOUND, "SUITE_NOT_FOUND", "Suite not found")

    fun updateSuite(
        suiteId: UUID,
        name: String?,
    ): SuiteRecord {
        val suite = getSuite(suiteId)
        name?.let { suite.name = it }
        auditService.logUpdate("SUITE", suite.id.toString(), "SUITE_UPDATED")
        return suite
    }

    fun deleteSuite(suiteId: UUID): SuiteRecord {
        val suite = getSuite(suiteId)
        suite.deletedAt = Instant.now()
        auditService.logDelete("SUITE", suite.id.toString(), "SUITE_DELETED")
        return suite
    }

    fun restoreSuite(suiteId: UUID): SuiteRecord {
        val suite = getSuite(suiteId)
        suite.deletedAt = null
        auditService.logUpdate("SUITE", suite.id.toString(), "SUITE_RESTORED")
        return suite
    }

    fun tree(projectId: UUID): List<SuiteRecord> = stateStore.suites.values.filter { it.projectId == projectId && it.deletedAt == null }

    private fun ensureProject(projectId: UUID) {
        if (!stateStore.projects.containsKey(projectId)) {
            throw DomainException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found")
        }
    }
}
