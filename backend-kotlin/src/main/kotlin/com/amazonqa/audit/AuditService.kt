package com.amazonqa.audit

import com.amazonqa.security.SecuritySupport
import com.amazonqa.store.AuditRecord
import com.amazonqa.store.StateStore
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class AuditService(
    private val stateStore: StateStore,
) {
    fun logCreate(
        entityType: String,
        entityId: String,
        metadata: String = "",
    ) {
        log("CREATE", entityType, entityId, metadata)
    }

    fun logUpdate(
        entityType: String,
        entityId: String,
        metadata: String = "",
    ) {
        log("UPDATE", entityType, entityId, metadata)
    }

    fun logDelete(
        entityType: String,
        entityId: String,
        metadata: String = "",
    ) {
        log("DELETE", entityType, entityId, metadata)
    }

    fun logDeny(
        entityType: String,
        entityId: String,
        metadata: String = "",
    ) {
        log("DENY", entityType, entityId, metadata)
    }

    fun list(): List<AuditRecord> = stateStore.audits.toList().sortedByDescending { it.createdAt }

    private fun log(
        eventType: String,
        entityType: String,
        entityId: String,
        metadata: String,
    ) {
        stateStore.audits.add(
            AuditRecord(
                id = UUID.randomUUID(),
                eventType = eventType,
                actor = SecuritySupport.currentActor(),
                entityType = entityType,
                entityId = entityId,
                metadata = metadata,
                createdAt = Instant.now(),
            ),
        )
    }
}
