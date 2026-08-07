package parker.core.runtime

import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.PrincipalId

/**
 * Memory Core Durability, Implementation Unit 4 (Replay and Startup
 * Recovery). Reconstructs a fresh [InMemoryMemoryCore]'s complete state
 * from a [MemoryCoreDurabilityLog]'s own durably-appended entry
 * sequence, through governed internal pathways -- never by direct field
 * manipulation. Governed by
 * `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md` ("the
 * Contract Design") Section 6 (Recovery) and Section 7 (Corruption and
 * Lifecycle), and
 * `docs/architecture/MEMORY_CORE_DURABILITY_SCOPE_LOCK.md` ("the Scope
 * Lock") Section 8 (Recovery Rules).
 *
 * ## Not yet wired to a `DurableMemoryCore` decorator -- disclosed, not
 * an oversight
 *
 * `docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md`'s
 * own Unit 4 text describes this capability as `DurableMemoryCore.recover()`,
 * a method on a `MemoryCore`/`MemoryRetrieval`-implementing decorator.
 * That decorator does not exist in this repository yet -- this session's
 * own prior Unit ("Unit 3") built the Implementation Plan's own deferred
 * Unit 2 output instead of the Plan's own Unit 3 (the decorator itself),
 * and this session's own current task explicitly excludes building it
 * here too ("no Memory Core decorator," "no runtime composition"). This
 * object is therefore a standalone, decorator-independent internal
 * component: it takes a [MemoryCoreDurabilityLog] and returns a fully
 * reconstructed [InMemoryMemoryCore]. Wiring it into a decorator and into
 * `ParkerRuntime.kt` remains later, separate work.
 *
 * ## Replay order: strict durable order, not a separate reordering pass
 *
 * [recover] replays [MemoryCoreDurabilityLog.readAll]'s own result in
 * exact sequence -- the file's own true total write order, by
 * construction of the durability log's own single-log design. This
 * alone already guarantees Contract Design Section 6's own ordering
 * requirement, that "Provenance records must be available before any
 * Entity, Document, Assertion, or Relationship record that references
 * one": the *original* write path could only ever create a
 * dependent record after its Provenance already existed, so replaying in
 * the same order reproduces that same guarantee without any separate
 * dependency-sorting pass.
 *
 * ## Failure handling: fail-fast, no partial recovered state ever exposed
 *
 * [recover] either returns a fully, successfully reconstructed
 * [InMemoryMemoryCore], or throws a [MemoryCoreRecoveryException]
 * subtype and returns nothing -- there is no code path on which a
 * partially-populated instance is returned to a caller. A durability log
 * that cannot be read at all, an entry that cannot be decoded (including
 * an unsupported schema version -- already rejected by
 * [DurableMemoryCoreEntryCodec.decode] itself, simply not swallowed
 * here), a broken referential-integrity reference, a conflicting
 * duplicate identity, an impossible lifecycle transition, or a
 * prior-status mismatch each fail the entire recovery, uniformly,
 * regardless of where in the log the failure occurs.
 *
 * ## Incomplete-terminal-data: no leniency implemented, by deliberate,
 * disclosed choice -- not an oversight
 *
 * The Implementation Plan's own Unit 4 text speculates that a decode
 * failure on a durability log's own *last* line can safely be treated as
 * a discardable, merely-interrupted write, distinguishable from genuine
 * corruption by file position alone. Examined rigorously for this Unit:
 * that is a heuristic, not a proof -- corruption from a cause unrelated
 * to an in-progress append (bit rot, an unrelated tool truncating the
 * file, a bug in a materially different version of this code) could
 * equally affect only the file's own last line, indistinguishable from a
 * genuine interruption using today's line-oriented format alone, and
 * neither the Contract Design nor the Scope Lock fixes a concrete
 * mechanism for telling the two apart from the artifact itself -- only
 * the conceptual distinction between them (Contract Design Section 7).
 * Per this Unit's own governing task ("If the current line-oriented log
 * cannot safely distinguish interrupted final data from non-terminal
 * corruption, stop and report that limitation rather than inventing a
 * heuristic"), this object does **not** implement that leniency: any
 * decode failure, at any position, fails recovery. This is the strictly
 * safer, fail-closed reading, and is independently already required by
 * both governing documents regardless ("no silent empty-store
 * fallback"). A future governance decision may authorise a specific,
 * concrete mechanism for safely distinguishing the two cases; none exists
 * today, and none is invented here.
 */
internal object MemoryCoreRecovery {

    /**
     * The [PrincipalId] this object passes to every read it performs
     * against the [InMemoryMemoryCore] instance under reconstruction
     * (`getEntity`/`getDocument`/`getAssertion`/`getRelationship`,
     * `transitionStatus`). [InMemoryMemoryCore] never reads this
     * parameter -- it exists purely for auditability, per Errata 004 --
     * so its exact value has no behavioural effect; it is named here for
     * diagnostic clarity, exactly mirroring
     * `ParkerRuntime`'s own established `system.*` Principal-naming
     * convention for internally-originated, non-caller-initiated acts.
     */
    private val recoveryPrincipal = PrincipalId("system.memory-core-recovery")

    /**
     * Reconstructs a fresh [InMemoryMemoryCore] from every entry
     * [durabilityLog] has durably committed, in exact durable order, then
     * restores every per-kind identifier counter (Memory Core Durability,
     * Implementation Unit 5) as the last step before returning.
     *
     * A missing or genuinely empty durability log (zero entries) produces
     * a genuinely empty, but successfully recovered, [InMemoryMemoryCore]
     * -- not a failure. Any other condition under which the recovered
     * state cannot be trusted throws a [MemoryCoreRecoveryException]
     * subtype instead of returning anything.
     *
     * ## Identifier counter restoration happens only after every entry has
     * already been successfully replayed -- never before, never on a
     * partial result
     *
     * [InMemoryMemoryCore.restoreIdentifierCounters] is called exactly
     * once, after the `forEachIndexed` loop below has completed in full
     * without throwing. If any entry fails to replay, this function
     * propagates that failure immediately and never reaches the
     * counter-restoration call at all -- the partially-populated
     * [InMemoryMemoryCore] instance that was being built is simply
     * discarded, unreachable, with whatever counter values it started
     * with (irrelevant, since nothing ever returns it). A failed recovery
     * therefore can never advance a counter that any caller could
     * observe.
     */
    suspend fun recover(durabilityLog: MemoryCoreDurabilityLog): InMemoryMemoryCore {
        val entries = try {
            durabilityLog.readAll()
        } catch (e: Exception) {
            throw MemoryCoreRecoveryException.DurabilityLogUnreadable(e)
        }

        val memoryCore = InMemoryMemoryCore()

        entries.forEachIndexed { index, entry ->
            applyEntry(memoryCore, entry, index)
        }

        try {
            memoryCore.restoreIdentifierCounters()
        } catch (e: Exception) {
            throw MemoryCoreRecoveryException.IdentifierCounterRestorationFailed(e.message ?: "identifier counter restoration failed", e)
        }

        return memoryCore
    }

    private suspend fun applyEntry(memoryCore: InMemoryMemoryCore, entry: DurableMemoryCoreEntry, index: Int) {
        val kind = entry::class.simpleName ?: "unknown"

        try {
            when (entry) {
                is DurableMemoryCoreEntry.ProvenanceCreated -> memoryCore.restoreProvenance(entry.provenance)
                is DurableMemoryCoreEntry.EntityCreated -> memoryCore.restoreEntity(entry.entity)
                is DurableMemoryCoreEntry.DocumentRegistered -> memoryCore.restoreDocument(entry.document)
                is DurableMemoryCoreEntry.AssertionCreated -> memoryCore.restoreAssertion(entry.assertion)
                is DurableMemoryCoreEntry.RelationshipCreated -> memoryCore.restoreRelationship(entry.relationship)
                is DurableMemoryCoreEntry.StatusTransitioned -> applyTransition(memoryCore, entry)
            }
        } catch (e: MemoryCoreRecoveryException) {
            throw e
        } catch (e: IllegalStateException) {
            throw MemoryCoreRecoveryException.ConflictingDuplicateIdentity(index, kind, e.message ?: "conflicting duplicate identity", e)
        } catch (e: Exception) {
            throw MemoryCoreRecoveryException.RestorationFailed(index, kind, e.message ?: "restoration failed", e)
        }
    }

    /**
     * Replays one [DurableMemoryCoreEntry.StatusTransitioned] entry.
     *
     * Calling the existing, public [InMemoryMemoryCore.transitionStatus]
     * unconditionally would be incorrect for a repeated durable entry: the
     * closed transition table
     * ([MemoryCoreLifecycleTransitions]) contains no `X -> X` self-loop
     * for any status, so replaying an already-applied transition a second
     * time would throw, not idempotently succeed. This function reads the
     * target record's own current status first and decides among exactly
     * three cases before ever calling [InMemoryMemoryCore.transitionStatus]:
     *
     * - **Current status already equals [entry]'s own `targetStatus`.**
     *   The transition has already been applied by an earlier occurrence
     *   of this same durable fact -- an idempotent duplicate (Contract
     *   Design Section 6). Skipped without calling `transitionStatus`
     *   again.
     * - **Current status equals [entry]'s own `priorStatus`.** The normal
     *   case: the transition is being applied for the first time. The
     *   transition's own validity is checked directly (so an impossible
     *   transition is reported with a clear, dedicated exception rather
     *   than `transitionStatus`'s own generic `IllegalArgumentException`),
     *   then applied via the existing, unmodified public method.
     * - **Neither.** [entry]'s own claimed `priorStatus` does not match
     *   reality at this point in replay -- a genuine prior-status
     *   mismatch, rejected.
     *
     * A target record that does not exist at all in the store yet (its
     * own creation entry never appeared, or appeared later than this
     * transition -- impossible during correctly-ordered replay, but not
     * assumed) is reported as a missing transition target, distinctly
     * from a prior-status mismatch.
     */
    private suspend fun applyTransition(memoryCore: InMemoryMemoryCore, entry: DurableMemoryCoreEntry.StatusTransitioned) {
        val referenceLabel = describeReference(entry.reference)
        val currentStatus = currentStatusOf(memoryCore, entry.reference)
            ?: throw MemoryCoreRecoveryException.MissingTransitionTarget(referenceLabel)

        when (currentStatus) {
            entry.targetStatus -> return
            entry.priorStatus -> {
                if (!MemoryCoreLifecycleTransitions.isValidTransition(entry.priorStatus, entry.targetStatus)) {
                    throw MemoryCoreRecoveryException.ImpossibleTransition(referenceLabel, entry.priorStatus.name, entry.targetStatus.name)
                }
                memoryCore.transitionStatus(recoveryPrincipal, entry.reference, entry.targetStatus)
            }
            else -> throw MemoryCoreRecoveryException.PriorStatusMismatch(referenceLabel, entry.priorStatus.name, currentStatus.name)
        }
    }

    private suspend fun currentStatusOf(memoryCore: InMemoryMemoryCore, reference: MemoryCoreRecordReference): MemoryCoreRecordStatus? =
        when (reference) {
            is MemoryCoreRecordReference.ToEntity -> memoryCore.getEntity(recoveryPrincipal, reference.entityId)?.status
            is MemoryCoreRecordReference.ToDocument -> memoryCore.getDocument(recoveryPrincipal, reference.documentId)?.status
            is MemoryCoreRecordReference.ToAssertion -> memoryCore.getAssertion(recoveryPrincipal, reference.assertionId)?.status
            is MemoryCoreRecordReference.ToRelationship -> memoryCore.getRelationship(recoveryPrincipal, reference.relationshipId)?.status
        }

    private fun describeReference(reference: MemoryCoreRecordReference): String = when (reference) {
        is MemoryCoreRecordReference.ToEntity -> "Entity '${reference.entityId.value}'"
        is MemoryCoreRecordReference.ToDocument -> "Document '${reference.documentId.value}'"
        is MemoryCoreRecordReference.ToAssertion -> "Assertion '${reference.assertionId.value}'"
        is MemoryCoreRecordReference.ToRelationship -> "Relationship '${reference.relationshipId.value}'"
    }
}

/**
 * Typed failures for [MemoryCoreRecovery]. Sealed and thrown -- not
 * returned as a sealed result type -- mirroring
 * [MemoryCoreDurabilityLogException]'s own identical, already-established
 * convention. `internal`: never surfaced through `MemoryCore`'s or
 * `MemoryRetrieval`'s own public contracts, and no new public
 * `MemoryCore`/`MemoryRetrieval` API is introduced anywhere by this Unit.
 */
internal sealed class MemoryCoreRecoveryException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {

    /** The durability log itself could not be read at all -- a [MemoryCoreDurabilityLogException] or another I/O-level fault. */
    class DurabilityLogUnreadable(cause: Throwable) :
        MemoryCoreRecoveryException("Memory Core recovery failed: the durability log could not be read", cause)

    /**
     * Restoring a creation entry (Provenance, Entity, Document, Assertion,
     * or Relationship) failed -- a broken provenance reference, a broken
     * relationship endpoint reference, or any other referential-integrity
     * or structural failure. [entryIndex] is the entry's own zero-based
     * position in durable order.
     */
    class RestorationFailed(val entryIndex: Int, val kind: String, val reason: String, cause: Throwable? = null) :
        MemoryCoreRecoveryException("Memory Core recovery failed at entry $entryIndex (kind=$kind): $reason", cause)

    /**
     * A durably repeated identifier was found whose content differs from
     * the record already restored under that identifier -- corruption,
     * not a legitimate at-least-once retry (Contract Design Section 6: "a
     * repeated identifier carrying different content is not a repeated
     * record in this sense; it is corruption").
     */
    class ConflictingDuplicateIdentity(val entryIndex: Int, val kind: String, val reason: String, cause: Throwable) :
        MemoryCoreRecoveryException(
            "Memory Core recovery found a conflicting duplicate identity at entry $entryIndex (kind=$kind): $reason",
            cause,
        )

    /** A [DurableMemoryCoreEntry.StatusTransitioned] entry names a target record that does not exist in the recovered state at this point in replay. */
    class MissingTransitionTarget(val reference: String) :
        MemoryCoreRecoveryException("Memory Core recovery encountered a lifecycle transition targeting $reference, which does not exist at this point in replay")

    /** A [DurableMemoryCoreEntry.StatusTransitioned] entry's own claimed `priorStatus` does not match the target record's actual status at this point in replay. */
    class PriorStatusMismatch(val reference: String, val claimedPriorStatus: String, val actualStatus: String) :
        MemoryCoreRecoveryException(
            "Memory Core recovery encountered a lifecycle transition for $reference claiming prior status " +
                "'$claimedPriorStatus', but its actual status at this point in replay is '$actualStatus'",
        )

    /** A [DurableMemoryCoreEntry.StatusTransitioned] entry names a `priorStatus`/`targetStatus` pair that is not a valid transition. */
    class ImpossibleTransition(val reference: String, val priorStatus: String, val targetStatus: String) :
        MemoryCoreRecoveryException("Memory Core recovery encountered an impossible lifecycle transition for $reference: '$priorStatus' -> '$targetStatus' is not a valid transition")

    /**
     * Memory Core Durability, Implementation Unit 5 (Identifier
     * Restoration). Every entry replayed successfully, but
     * [InMemoryMemoryCore.restoreIdentifierCounters] itself failed --
     * a malformed identifier (wrong prefix, non-numeric or non-positive
     * suffix) among the now-fully-restored records, or a counter that
     * would overflow [Long]'s own upper bound. Recovery as a whole still
     * fails: an [InMemoryMemoryCore] whose own future identifier minting
     * cannot be trusted is not a successfully recovered instance, even
     * though every individual record it holds replayed correctly.
     */
    class IdentifierCounterRestorationFailed(val reason: String, cause: Throwable) :
        MemoryCoreRecoveryException("Memory Core recovery failed while restoring identifier counters: $reason", cause)
}
