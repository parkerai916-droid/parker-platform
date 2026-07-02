package parker.core.interfaces

interface AuditService {
    suspend fun record(record: AuditRecord): AuditRecordId
    suspend fun query(query: AuditQuery): List<AuditRecord>
}
