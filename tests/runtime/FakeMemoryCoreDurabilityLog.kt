package parker.core.runtime

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Memory Core Durability, Implementation Unit 2. Test-only fake, mirroring
 * [FakeEvidenceDeletionAudit]'s own lambda-configurable shape. Records
 * every [append] call, in order, so a test can assert on exactly what was
 * durably committed, and can be configured to throw on either operation --
 * simulating a genuine durability fault without needing a real,
 * failing filesystem. This Unit's own governing task explicitly forbids
 * a production implementation; this fake exists solely so this Unit's own
 * tests, and a future Unit's own tests, have something concrete to
 * exercise [MemoryCoreDurabilityLog] against. It is never referenced by
 * `src/composition/` and is never a candidate for runtime composition.
 */
internal class FakeMemoryCoreDurabilityLog(
    private val appendBehavior: (suspend (DurableMemoryCoreEntry) -> Unit)? = null,
    private val readAllBehavior: (suspend () -> List<DurableMemoryCoreEntry>)? = null,
) : MemoryCoreDurabilityLog {

    private val backing = CopyOnWriteArrayList<DurableMemoryCoreEntry>()

    /** Every entry actually appended to [backing] -- distinct from what a configured [appendBehavior] may have done instead. */
    val appended: List<DurableMemoryCoreEntry> get() = backing.toList()

    override suspend fun append(entry: DurableMemoryCoreEntry) {
        if (appendBehavior != null) {
            appendBehavior.invoke(entry)
        } else {
            backing.add(entry)
        }
    }

    override suspend fun readAll(): List<DurableMemoryCoreEntry> {
        return readAllBehavior?.invoke() ?: backing.toList()
    }
}
