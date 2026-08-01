package parker.core.runtime

import java.util.concurrent.CopyOnWriteArrayList
import parker.core.interfaces.EvidenceDeletionAudit
import parker.core.interfaces.EvidenceDeletionAuditRecord

/**
 * Test-only fake, mirroring [FakePermissionEngine]'s own
 * lambda-configurable shape. Records every [record] call, in order, so a
 * test can assert on exactly what was persisted, and can be configured
 * to throw -- simulating a genuine audit-persistence fault
 * ([parker.core.interfaces.EvidenceDeletionAuditException.PersistenceFailure])
 * without needing a real, failing filesystem.
 */
class FakeEvidenceDeletionAudit(
    private val recordBehavior: (suspend (EvidenceDeletionAuditRecord) -> Unit)? = null,
) : EvidenceDeletionAudit {

    private val backing = CopyOnWriteArrayList<EvidenceDeletionAuditRecord>()
    val records: List<EvidenceDeletionAuditRecord> get() = backing.toList()

    override suspend fun record(record: EvidenceDeletionAuditRecord) {
        if (recordBehavior != null) {
            recordBehavior.invoke(record)
        } else {
            backing.add(record)
        }
    }
}
