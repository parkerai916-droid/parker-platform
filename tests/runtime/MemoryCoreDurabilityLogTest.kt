package parker.core.runtime

import java.time.Instant
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.jvmErasure
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import parker.core.interfaces.ContentNature
import parker.core.interfaces.Provenance
import parker.core.interfaces.ProvenanceId

/**
 * Memory Core Durability, Implementation Unit 2 (Durable Storage
 * Interface). Structural and behavioural contract tests for
 * [MemoryCoreDurabilityLog] only -- no production implementation exists
 * yet, per this Unit's own explicit instruction; behavioural tests below
 * exercise [FakeMemoryCoreDurabilityLog], a test-only fake, never a real
 * filesystem. No persistence, encoding, or replay is tested here -- each
 * is a later Unit's own responsibility.
 */
class MemoryCoreDurabilityLogTest {

    private fun entry(sourceIdentifier: String): DurableMemoryCoreEntry.ProvenanceCreated =
        DurableMemoryCoreEntry.ProvenanceCreated(
            provenance = Provenance(
                provenanceId = ProvenanceId("provenance-$sourceIdentifier"),
                sourceIdentifier = sourceIdentifier,
                sourceType = "conversation",
                acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
                ingestionTime = Instant.parse("2026-01-01T00:00:01Z"),
                contentNature = ContentNature.ORIGINAL,
            ),
        )

    // ================= Interface visibility =================

    @Test
    fun `MemoryCoreDurabilityLog is internal, never public API surface`() {
        assertEquals(KVisibility.INTERNAL, MemoryCoreDurabilityLog::class.visibility)
    }

    // ================= Exact operation set =================

    @Test
    fun `MemoryCoreDurabilityLog declares exactly append and readAll -- nothing more`() {
        val declared = MemoryCoreDurabilityLog::class.declaredFunctions

        assertEquals(
            setOf("append", "readAll"),
            declared.map { it.name }.toSet(),
            "MemoryCoreDurabilityLog must declare exactly append and readAll -- no batch write, delete, " +
                "update, replace, truncate, clear, or compact operation may exist at this layer -- " +
                "found: ${declared.map { it.name }}",
        )
        assertEquals(2, declared.size, "no operation name may be declared more than once")
    }

    // ================= append accepts one durable entry at a time =================

    @Test
    fun `append accepts exactly one DurableMemoryCoreEntry parameter -- no batch or vararg form`() {
        val appendFunction = MemoryCoreDurabilityLog::class.declaredFunctions.single { it.name == "append" }

        // parameters[0] is the receiver (the MemoryCoreDurabilityLog instance itself) for a member function
        // reached via declaredFunctions; the real, single value parameter is parameters[1].
        val valueParameters = appendFunction.parameters.drop(1)

        assertEquals(1, valueParameters.size, "append must accept exactly one parameter")
        assertEquals(DurableMemoryCoreEntry::class, valueParameters.single().type.jvmErasure)
    }

    // ================= readAll return shape: ordered, non-nullable, no result wrapper =================

    @Test
    fun `readAll returns a non-nullable List of DurableMemoryCoreEntry -- no nullable or sealed-result wrapper`() {
        val readAllFunction = MemoryCoreDurabilityLog::class.declaredFunctions.single { it.name == "readAll" }

        assertEquals(List::class, readAllFunction.returnType.jvmErasure)
        assertTrue(!readAllFunction.returnType.isMarkedNullable, "readAll's own return type must not be nullable")
    }

    // ================= Sequence semantics: append order is preserved exactly =================

    @Test
    fun `readAll preserves the exact order entries were appended in`() = runTest {
        val log: MemoryCoreDurabilityLog = FakeMemoryCoreDurabilityLog()

        log.append(entry("first"))
        log.append(entry("second"))
        log.append(entry("third"))

        val expected = listOf(entry("first"), entry("second"), entry("third"))
        assertEquals(expected, log.readAll())
    }

    @Test
    fun `readAll against a freshly-created, never-appended-to log returns an empty list -- genuine emptiness, not failure`() = runTest {
        val log: MemoryCoreDurabilityLog = FakeMemoryCoreDurabilityLog()

        assertEquals(emptyList(), log.readAll())
    }

    // ================= No filesystem or serialization type leakage =================

    @Test
    fun `neither append nor readAll references a filesystem, stream, channel, serializer, or database type`() {
        val forbiddenQualifiedNamePrefixes = listOf("java.nio.file", "java.io", "java.sql", "javax.sql", "java.nio.channels")
        val forbiddenQualifiedNameSubstrings = listOf("Json", "json", "Sqlite", "sqlite", "Serializer", "Serializable", "Parser", "Codec", "Docker", "Channel", "Stream", "File", "Path")

        val allTypes = MemoryCoreDurabilityLog::class.declaredFunctions.flatMap { function ->
            function.parameters.drop(1).map { it.type.jvmErasure } + listOf(function.returnType.jvmErasure)
        }

        allTypes.forEach { type ->
            val qualifiedName = type.qualifiedName ?: return@forEach
            forbiddenQualifiedNamePrefixes.forEach { prefix ->
                assertTrue(!qualifiedName.startsWith(prefix), "found forbidden filesystem-adjacent type '$qualifiedName' (prefix '$prefix')")
            }
            forbiddenQualifiedNameSubstrings.forEach { substring ->
                assertTrue(
                    !qualifiedName.contains(substring),
                    "found forbidden serialization/storage-adjacent type '$qualifiedName' (substring '$substring')",
                )
            }
        }
    }

    // ================= No prohibited Parker-domain dependency =================

    @Test
    fun `neither append nor readAll references PermissionEngine, Knowledge Memory, Evidence Intelligence, EventBus, or ParkerRuntime`() {
        val forbiddenQualifiedNameSubstrings = listOf(
            "PermissionEngine", "Knowledge", "EvidenceCustodian", "EvidenceIntelligence", "EventBus", "ParkerRuntime",
        )

        val allTypes = MemoryCoreDurabilityLog::class.declaredFunctions.flatMap { function ->
            function.parameters.drop(1).map { it.type.jvmErasure } + listOf(function.returnType.jvmErasure)
        }

        allTypes.forEach { type ->
            val qualifiedName = type.qualifiedName ?: return@forEach
            forbiddenQualifiedNameSubstrings.forEach { substring ->
                assertTrue(
                    !qualifiedName.contains(substring),
                    "found forbidden Parker-domain dependency '$qualifiedName' (substring '$substring')",
                )
            }
        }
    }

    // ================= Failure behaviour: explicit, never silently an empty store =================

    @Test
    fun `a durability fault during append propagates as a thrown exception, and the entry is never silently recorded`() = runTest {
        val log = FakeMemoryCoreDurabilityLog(
            appendBehavior = { throw IllegalStateException("simulated durability fault") },
        )

        assertFailsWith<IllegalStateException> { log.append(entry("faulted")) }
        assertEquals(emptyList(), log.appended)
    }

    @Test
    fun `a durability fault during readAll propagates as a thrown exception, never a silently substituted empty list`() = runTest {
        val log = FakeMemoryCoreDurabilityLog(
            readAllBehavior = { throw IllegalStateException("simulated unavailable or corrupt store") },
        )

        assertFailsWith<IllegalStateException> { log.readAll() }
    }

    @Test
    fun `an unavailable or corrupt store (thrown failure) is distinguishable from a genuinely empty one (empty list)`() = runTest {
        val genuinelyEmpty: MemoryCoreDurabilityLog = FakeMemoryCoreDurabilityLog()
        val unavailable: MemoryCoreDurabilityLog = FakeMemoryCoreDurabilityLog(
            readAllBehavior = { throw IllegalStateException("simulated unavailable store") },
        )

        // Genuine emptiness returns normally with an empty list -- no exception.
        assertEquals(emptyList(), genuinelyEmpty.readAll())

        // Unavailability throws -- it never returns normally with an empty list, which would be
        // indistinguishable from genuine emptiness to a caller.
        assertFailsWith<IllegalStateException> { unavailable.readAll() }
    }
}
