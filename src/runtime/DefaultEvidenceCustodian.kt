package parker.core.runtime

import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import parker.core.interfaces.AcceptedEvidenceArtifact
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.EvidenceAcceptanceResult
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceArtifactStorage
import parker.core.interfaces.EvidenceArtifactStorageException
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceManifestRetrievalResult
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.EvidenceSourceIdentityIndex
import parker.core.interfaces.EvidenceSourceManifest
import parker.core.interfaces.EvidenceSourceManifestStorage
import parker.core.interfaces.EvidenceSourceManifestStorageException
import parker.core.interfaces.EvidenceSourceSubmissionResult
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId
import parker.core.interfaces.SourceIdentityReservation

/**
 * Evidence Custodian, Implementation Plan Phase 3 ("Governed acceptance
 * path"), Unit 2. The sole implementation of [EvidenceCustodian], composed
 * from two already-existing, unmodified dependencies: [storage]
 * (`EvidenceArtifactStorage`, Unit 1, Phase 2) and [permissionEngine] (the
 * existing `PermissionEngine` interface). See [EvidenceCustodian]'s own
 * file (`src/interfaces/EvidenceCustodian.kt`) for the full Unit 2
 * rationale -- this class implements exactly what that file's KDoc
 * describes, and does not restate it here beyond what is specific to this
 * implementation.
 *
 * ## Sequence
 *
 * 1. Build an [ExecutionRequest] naming this Unit's own well-known,
 *    disclosed [EVIDENCE_INTAKE_RESOURCE_ID]/[ACCEPT_ACTION_NAME]
 *    conventions (see [EvidenceCustodian]'s own KDoc for why neither is
 *    registered anywhere by this Unit) and call
 *    [PermissionEngine.evaluate].
 * 2. If the decision is not `APPROVED` or `APPROVED_WITH_CONFIRMATION`,
 *    return [EvidenceAcceptanceResult.Rejected] immediately -- **nothing
 *    is minted and nothing is written**. Acceptance never occurs
 *    implicitly, silently, or as a side effect of a denied proposal.
 * 3. Otherwise, mint a fresh [EvidenceArtifactId] (see "Identifier
 *    minting," below) and call [EvidenceArtifactStorage.write]. A storage
 *    failure (`EvidenceArtifactStorageException`, any subtype) propagates
 *    unchanged -- this class neither catches nor reinterprets it, per this
 *    Unit's own "storage and acceptance remain separate responsibilities"
 *    instruction.
 * 4. Only once [EvidenceArtifactStorage.write] returns successfully is
 *    [AcceptedEvidenceArtifact.acceptedAt] read (`Instant.now()`, once)
 *    and [EvidenceAcceptanceResult.Accepted] returned.
 *
 * ## Identifier minting -- a random value, not this repository's usual
 * incrementing-sequence convention, and why
 *
 * Every other identifier-minting Unit in this repository
 * (`InMemoryMemoryCore`'s `"document-${nextDocumentSequence++}"` and its
 * four siblings) uses an in-memory, monotonically-increasing counter. That
 * convention is safe there because the store the counter protects is
 * *itself* in-memory only -- a process restart resets the counter and the
 * store together, so no collision is possible.
 *
 * That precondition does not hold here. [storage] may be
 * `FileSystemEvidenceArtifactStorage`, which is durable across process
 * restarts, while an in-memory sequence counter in this class would not
 * be. A restarted process reusing a from-scratch counter starting at `1`
 * could collide with an identifier a *previous* process run already wrote
 * to disk -- not a theoretical concern, a direct consequence of pairing
 * ephemeral counter state with durable storage. This class mints
 * identifiers from [UUID.randomUUID] instead: a `java.util.UUID`'s own
 * string form (`toString()`) is already lowercase hexadecimal and
 * hyphens only, satisfying `EvidenceArtifactStorage`'s own canonical-identifier
 * pattern (`^[a-z0-9_-]+$`) and never colliding with any Windows reserved
 * device name (far longer than any reserved form), without requiring any
 * new persisted counter state -- state this Unit is not authorised to
 * introduce (no new storage implementation). `EvidenceArtifactStorage.write`'s
 * own existing `DuplicateIdentifier` guard remains as a genuine,
 * meaningful backstop against the astronomically unlikely case of a UUID
 * collision, rather than as the primary uniqueness mechanism a
 * restart-unsafe counter would have made it.
 */
class DefaultEvidenceCustodian(
    private val storage: EvidenceArtifactStorage,
    private val permissionEngine: PermissionEngine,
    private val manifestStorage: EvidenceSourceManifestStorage = InMemoryEvidenceSourceManifestStorage(),
    private val sourceIdentityIndex: EvidenceSourceIdentityIndex = InMemoryEvidenceSourceIdentityIndex(),
) : EvidenceCustodian {

    /**
     * Document Ingestion, Authoritative Source Manifest Foundation
     * Implementation. Step 3 below establishes the Authoritative
     * Evidence Source Manifest (digest, byte length, and the caller's own
     * declared [CandidateEvidenceArtifact.receivedMediaType]/
     * [CandidateEvidenceArtifact.originalFileName], if any) from the
     * exact accepted candidate bytes, immediately after [storage.write]
     * durably succeeds -- bytes-first, manifest-second, exactly as
     * `docs/architecture/DOCUMENT_INGESTION_AUTHORITATIVE_SOURCE_MANIFEST_RETRIEVAL_SCOPE_LOCK.md`
     * ("the Scope Lock") Section 15 requires. [manifestStorage.write]'s
     * own exception, like [storage.write]'s, is never caught here -- it
     * propagates unchanged, so a manifest-persistence failure is never
     * silently swallowed and [accept] never returns [EvidenceAcceptanceResult.Accepted]
     * while claiming a state it cannot back (mirroring Evidence Custodian
     * Phase 7 Boundary Clarification Section 7's own "the write failure
     * propagates... and the caller-visible success is never returned"
     * precedent for exactly this shape of secondary-durable-write
     * failure after an already-durable primary action). The digest this
     * step persists becomes the sole authoritative *expected* digest for
     * this identity from this point forward -- it is never later replaced
     * by a digest recomputed from a subsequent retrieval (Scope Lock
     * Section 7).
     */
    override suspend fun accept(
        requestingPrincipalId: PrincipalId,
        candidate: CandidateEvidenceArtifact,
    ): EvidenceAcceptanceResult {
        val decision = permissionEngine.evaluate(
            buildExecutionRequest(
                requestingPrincipalId = requestingPrincipalId,
                resourceId = EVIDENCE_INTAKE_RESOURCE_ID,
                actionName = ACCEPT_ACTION_NAME,
                intent = "Accept evidence artifact into Evidence Custodian custody",
                requestIdPrefix = "evidence-accept",
            ),
        )

        if (decision.decision != PermissionDecisionOutcome.APPROVED &&
            decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) {
            return EvidenceAcceptanceResult.Rejected(
                "Permission Engine did not authorise evidence acceptance for principal " +
                    "'${requestingPrincipalId.value}' (decision=${decision.decision})",
            )
        }

        val evidenceArtifactId = EvidenceArtifactId("evidence-${UUID.randomUUID()}")
        return EvidenceAcceptanceResult.Accepted(acceptWithEvidenceArtifactId(evidenceArtifactId, candidate))
    }

    /**
     * Crash-safe idempotency review correction. The minimum private helper factored out of
     * [accept] so [submitSource] can complete canonical registration under an already-reserved
     * [EvidenceArtifactId] (never mints one itself) -- [accept]'s own public behaviour,
     * ordering, and exception propagation are completely unchanged by this extraction; it still
     * mints its own fresh, random identifier and calls this helper with it, in the same order as
     * before. Never called by [submitSource] directly -- [submitSource]'s own completion path
     * ([ensureCanonicalRegistration]) must additionally tolerate a concurrent or resumed
     * completer, which this unconditional, single-attempt helper deliberately does not.
     */
    private suspend fun acceptWithEvidenceArtifactId(
        evidenceArtifactId: EvidenceArtifactId,
        candidate: CandidateEvidenceArtifact,
    ): AcceptedEvidenceArtifact {
        storage.write(evidenceArtifactId, candidate.content)

        manifestStorage.write(
            EvidenceSourceManifest(
                evidenceArtifactId = evidenceArtifactId,
                sha256 = sha256Hex(candidate.content),
                byteLength = candidate.content.size.toLong(),
                receivedMediaType = candidate.receivedMediaType,
                originalFileName = candidate.originalFileName,
            ),
        )

        return AcceptedEvidenceArtifact(evidenceArtifactId, acceptedAt = Instant.now())
    }

    /**
     * Document Ingestion, Authoritative Source Manifest Foundation
     * Implementation. Mirrors [retrieve]'s own gated, observational shape
     * exactly -- a permission check, strictly before any manifest access,
     * followed by an unconditional [manifestStorage.read]. A genuinely
     * absent manifest becomes [EvidenceManifestRetrievalResult.NotFound],
     * never fabricated or inferred (Scope Lock Section 13); a genuine
     * storage fault propagates unchanged, never reinterpreted as
     * `NotFound`.
     */
    override suspend fun retrieveManifest(
        requestingPrincipalId: PrincipalId,
        evidenceArtifactId: EvidenceArtifactId,
    ): EvidenceManifestRetrievalResult {
        val decision = permissionEngine.evaluate(
            buildExecutionRequest(
                requestingPrincipalId = requestingPrincipalId,
                resourceId = EVIDENCE_MANIFEST_RETRIEVAL_RESOURCE_ID,
                actionName = RETRIEVE_MANIFEST_ACTION_NAME,
                intent = "Retrieve evidence source manifest from Evidence Custodian custody",
                requestIdPrefix = "evidence-retrieve-manifest",
            ),
        )

        if (decision.decision != PermissionDecisionOutcome.APPROVED &&
            decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) {
            return EvidenceManifestRetrievalResult.Rejected(
                evidenceArtifactId,
                "Permission Engine did not authorise evidence manifest retrieval for principal " +
                    "'${requestingPrincipalId.value}' (decision=${decision.decision})",
            )
        }

        val manifest = manifestStorage.read(evidenceArtifactId)
            ?: return EvidenceManifestRetrievalResult.NotFound(evidenceArtifactId)

        return EvidenceManifestRetrievalResult.Found(manifest)
    }

    /**
     * Parker Agent Gateway, AG-1F (Section 8, Section 16), corrected by the crash-safe idempotency
     * review. See this method's own interface KDoc for the full contract.
     *
     * ## Corrected algorithm
     *
     * 1. Permission check (unchanged) -- a denied decision returns [EvidenceSourceSubmissionResult.Rejected]
     *    before any hash is computed.
     * 2. Compute the authoritative SHA-256; a supplied [advisorySha256] that disagrees fails closed
     *    as [EvidenceSourceSubmissionResult.HashMismatch] -- no reservation is created, nothing is
     *    written (unchanged).
     * 3. [sourceIdentityIndex.createOrGet] atomically claims the authoritative hash for a freshly
     *    minted, proposed [EvidenceArtifactId] -- or reports the identity a prior caller already
     *    claimed it for. This single atomic step, not any in-process lock, is the cross-instance
     *    serialisation point: exactly one caller across any number of concurrent or restarted
     *    attempts ever wins the reservation for a given hash.
     * 4. [ensureCanonicalRegistration] then makes canonical Evidence Custodian state (source bytes
     *    plus manifest) durably match the reserved identity -- completing it if a prior attempt
     *    reserved the identity but crashed before finishing, tolerating a concurrent completer
     *    racing to do the same thing, and never minting a second identity for the same hash under
     *    any interleaving.
     *
     * No in-process mutex guards this method -- the filesystem-level `createOrGet` reservation
     * (for [FileSystemEvidenceSourceIdentityIndex]) and the underlying storage classes' own
     * atomic, duplicate-refusing writes are what make this method correct across multiple
     * [DefaultEvidenceCustodian] instances sharing the same durable storage, not any lock this
     * class itself holds.
     */
    override suspend fun submitSource(
        requestingPrincipalId: PrincipalId,
        candidate: CandidateEvidenceArtifact,
        advisorySha256: String?,
    ): EvidenceSourceSubmissionResult {
        val decision = permissionEngine.evaluate(
            buildExecutionRequest(
                requestingPrincipalId = requestingPrincipalId,
                resourceId = EVIDENCE_INTAKE_RESOURCE_ID,
                actionName = ACCEPT_ACTION_NAME,
                intent = "Submit candidate evidence source for idempotent Evidence Custodian acceptance",
                requestIdPrefix = "evidence-submit-source",
            ),
        )
        if (decision.decision != PermissionDecisionOutcome.APPROVED &&
            decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) {
            return EvidenceSourceSubmissionResult.Rejected(
                "Permission Engine did not authorise evidence source submission for principal " +
                    "'${requestingPrincipalId.value}' (decision=${decision.decision})",
            )
        }

        val computedSha256 = sha256Hex(candidate.content)
        if (advisorySha256 != null && !advisorySha256.equals(computedSha256, ignoreCase = true)) {
            return EvidenceSourceSubmissionResult.HashMismatch(computedSha256, advisorySha256)
        }

        val proposedEvidenceArtifactId = EvidenceArtifactId("evidence-${UUID.randomUUID()}")
        val reservation = sourceIdentityIndex.createOrGet(computedSha256, proposedEvidenceArtifactId)
        val evidenceArtifactId = when (reservation) {
            is SourceIdentityReservation.Created -> reservation.evidenceArtifactId
            is SourceIdentityReservation.Existing -> reservation.evidenceArtifactId
        }

        return when (val outcome = ensureCanonicalRegistration(evidenceArtifactId, candidate, computedSha256)) {
            is CanonicalRegistrationOutcome.NewlyCompleted ->
                EvidenceSourceSubmissionResult.Registered(evidenceArtifactId, outcome.manifest)
            is CanonicalRegistrationOutcome.AlreadyComplete ->
                EvidenceSourceSubmissionResult.AlreadyRegistered(evidenceArtifactId, outcome.manifest)
            is CanonicalRegistrationOutcome.Conflict ->
                EvidenceSourceSubmissionResult.Conflict(evidenceArtifactId, computedSha256, outcome.reason)
        }
    }

    /** Local outcome of [ensureCanonicalRegistration] -- never exposed outside this class. */
    private sealed class CanonicalRegistrationOutcome {
        data class NewlyCompleted(val manifest: EvidenceSourceManifest) : CanonicalRegistrationOutcome()
        data class AlreadyComplete(val manifest: EvidenceSourceManifest) : CanonicalRegistrationOutcome()
        data class Conflict(val reason: String) : CanonicalRegistrationOutcome()
    }

    /**
     * Crash-safe idempotency review correction. Makes canonical Evidence Custodian state (source
     * bytes, then manifest -- [accept]'s own established ordering, unchanged) durably match
     * [evidenceArtifactId], an identity already reserved by [sourceIdentityIndex.createOrGet] --
     * never mints an identity itself.
     *
     * Three cases, all without ever throwing an exception to [submitSource] or overwriting
     * existing canonical state:
     *
     * - **Already complete**: a manifest already exists under [evidenceArtifactId] and its
     *   recorded sha256 matches [computedSha256] -- returns [CanonicalRegistrationOutcome.AlreadyComplete]
     *   with the pre-existing, unmodified manifest. Nothing is written.
     * - **Newly completed**: no manifest exists yet. Bytes are written, tolerating (not treating
     *   as a fault) a [EvidenceArtifactStorageException.DuplicateIdentifier] from a concurrent or
     *   previously-interrupted completer -- verified to hold the identical content before
     *   proceeding, never overwritten. The manifest write is attempted the same way, tolerating
     *   [EvidenceSourceManifestStorageException.DuplicateIdentifier] from a completer that won
     *   the race for that specific step. Returns [CanonicalRegistrationOutcome.NewlyCompleted] if
     *   this call's own manifest write durably completed registration, or
     *   [CanonicalRegistrationOutcome.AlreadyComplete] if a concurrent completer's write won that
     *   race instead -- either way, callers observe a clean [EvidenceSourceSubmissionResult.Registered]/
     *   [EvidenceSourceSubmissionResult.AlreadyRegistered] pair for the same identity, never an
     *   uncaught exception or a second, orphaned identity.
     * - **Conflict**: existing canonical state under [evidenceArtifactId] (different bytes, or a
     *   manifest recording a different sha256) does not match what this submission would produce
     *   -- an internal consistency fault, never a normal outcome for identical-bytes resubmission.
     *   Fails closed as [CanonicalRegistrationOutcome.Conflict]; nothing already stored is ever
     *   overwritten.
     *
     * ## Where source-identity correctness actually comes from
     *
     * The one-identity-per-hash guarantee this method relies on comes entirely from
     * [sourceIdentityIndex.createOrGet]'s own atomic reservation (`submitSource`'s caller),
     * **not** from any cross-instance atomicity of [storage]/[manifestStorage] themselves. Owner
     * review of the crash-safe idempotency correction confirmed [storage]/[manifestStorage]'s own
     * "duplicate writes are refused" check is a pre-existing, frozen, per-instance-only guarantee
     * (their private `Mutex`, not an OS-level atomic create-exclusive) -- across genuinely
     * different instances, more than one concurrent completer of the *same* reserved identity may
     * each physically complete the write. That is accepted as harmless here (every completer
     * writes byte-identical content, so final canonical state is always correct) and is not a
     * defect this method works around or compensates for -- do not add cross-instance-atomicity
     * workarounds to this method on the strength of that finding alone.
     */
    private suspend fun ensureCanonicalRegistration(
        evidenceArtifactId: EvidenceArtifactId,
        candidate: CandidateEvidenceArtifact,
        computedSha256: String,
    ): CanonicalRegistrationOutcome {
        manifestStorage.read(evidenceArtifactId)?.let { existingManifest ->
            return if (existingManifest.sha256 == computedSha256) {
                CanonicalRegistrationOutcome.AlreadyComplete(existingManifest)
            } else {
                CanonicalRegistrationOutcome.Conflict(
                    "source-identity reservation names '${evidenceArtifactId.value}' for sha256 '$computedSha256' but the " +
                        "existing manifest under that identity is sha256='${existingManifest.sha256}' -- refusing to overwrite",
                )
            }
        }

        try {
            storage.write(evidenceArtifactId, candidate.content)
        } catch (e: EvidenceArtifactStorageException.DuplicateIdentifier) {
            val existingBytes = storage.read(evidenceArtifactId)
                ?: return CanonicalRegistrationOutcome.Conflict(
                    "EvidenceArtifactStorage reports existing content for reserved identity '${evidenceArtifactId.value}' " +
                        "but a read immediately afterward returned null",
                )
            if (!existingBytes.contentEquals(candidate.content)) {
                return CanonicalRegistrationOutcome.Conflict(
                    "EvidenceArtifactStorage already holds different bytes under reserved identity '${evidenceArtifactId.value}' " +
                        "for sha256 '$computedSha256' -- refusing to overwrite",
                )
            }
            // Identical bytes are already durably present -- a concurrent or previously
            // interrupted completer beat us to this step. Fall through to complete the manifest.
        }

        val manifest = EvidenceSourceManifest(
            evidenceArtifactId = evidenceArtifactId,
            sha256 = computedSha256,
            byteLength = candidate.content.size.toLong(),
            receivedMediaType = candidate.receivedMediaType,
            originalFileName = candidate.originalFileName,
        )
        return try {
            manifestStorage.write(manifest)
            CanonicalRegistrationOutcome.NewlyCompleted(manifest)
        } catch (e: EvidenceSourceManifestStorageException.DuplicateIdentifier) {
            val existing = manifestStorage.read(evidenceArtifactId)
                ?: return CanonicalRegistrationOutcome.Conflict(
                    "EvidenceSourceManifestStorage reports an existing manifest for reserved identity '${evidenceArtifactId.value}' " +
                        "but a read immediately afterward returned null",
                )
            if (existing.sha256 == computedSha256) {
                CanonicalRegistrationOutcome.AlreadyComplete(existing)
            } else {
                CanonicalRegistrationOutcome.Conflict(
                    "EvidenceSourceManifestStorage already holds a manifest with sha256='${existing.sha256}' under reserved " +
                        "identity '${evidenceArtifactId.value}', disagreeing with computed sha256 '$computedSha256' -- refusing to overwrite",
                )
            }
        }
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }

    /**
     * Evidence Custodian, Implementation Plan Phase 4 ("Retrieval interface
     * behaviour"), Unit 3. See [EvidenceCustodian]'s own file KDoc for the
     * full Unit 3 rationale -- this method implements exactly what that
     * file's KDoc describes.
     *
     * ## Sequence
     *
     * 1. Build an [ExecutionRequest] naming this Unit's own well-known,
     *    disclosed [EVIDENCE_RETRIEVAL_RESOURCE_ID]/[RETRIEVE_ACTION_NAME]
     *    conventions and call [PermissionEngine.evaluate] -- **before any
     *    storage access of any kind.**
     * 2. If the decision is not `APPROVED` or `APPROVED_WITH_CONFIRMATION`,
     *    return [EvidenceRetrievalResult.Rejected] immediately. `storage.read`
     *    is never called on this path -- a denied request produces no
     *    storage interaction whatsoever.
     * 3. Otherwise, call `EvidenceArtifactStorage.read(evidenceArtifactId)`.
     *    A `null` result becomes [EvidenceRetrievalResult.NotFound]; a
     *    non-null result becomes [EvidenceRetrievalResult.Found], carrying
     *    back exactly the [evidenceArtifactId] the caller supplied and
     *    exactly the bytes storage returned, unmodified. A thrown
     *    `EvidenceArtifactStorageException` (any subtype) propagates
     *    unchanged -- this method neither catches nor reinterprets it as
     *    `NotFound` or any other result, per this Unit's own "do not hide
     *    genuine storage failures" instruction.
     *
     * This method mints no identifier ([evidenceArtifactId] is
     * caller-supplied, already-existing), writes nothing, and reads no
     * field of [AcceptedEvidenceArtifact] or any other acceptance-side
     * state -- it is, in its entirety, one permission check and one
     * storage read.
     */
    override suspend fun retrieve(
        requestingPrincipalId: PrincipalId,
        evidenceArtifactId: EvidenceArtifactId,
    ): EvidenceRetrievalResult {
        val decision = permissionEngine.evaluate(
            buildExecutionRequest(
                requestingPrincipalId = requestingPrincipalId,
                resourceId = EVIDENCE_RETRIEVAL_RESOURCE_ID,
                actionName = RETRIEVE_ACTION_NAME,
                intent = "Retrieve evidence artifact from Evidence Custodian custody",
                requestIdPrefix = "evidence-retrieve",
            ),
        )

        if (decision.decision != PermissionDecisionOutcome.APPROVED &&
            decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) {
            return EvidenceRetrievalResult.Rejected(
                evidenceArtifactId,
                "Permission Engine did not authorise evidence retrieval for principal " +
                    "'${requestingPrincipalId.value}' (decision=${decision.decision})",
            )
        }

        val content = storage.read(evidenceArtifactId)
            ?: return EvidenceRetrievalResult.NotFound(evidenceArtifactId)

        return EvidenceRetrievalResult.Found(evidenceArtifactId, content)
    }

    /**
     * Shared [ExecutionRequest] construction for both [accept] and
     * [retrieve] -- the two operations differ only in which disclosed,
     * unregistered convention constants and intent string they supply,
     * never in the shape of the request itself. See [EvidenceCustodian]'s
     * own file KDoc for what a future runtime-composition Unit (Phase 10)
     * must register for a real [PermissionEngine] to resolve either
     * convention to anything other than a conservative `DENIED`.
     */
    private fun buildExecutionRequest(
        requestingPrincipalId: PrincipalId,
        resourceId: ResourceId,
        actionName: String,
        intent: String,
        requestIdPrefix: String,
    ): ExecutionRequest {
        val now = Instant.now()
        return ExecutionRequest(
            requestId = RequestId("$requestIdPrefix-${UUID.randomUUID()}"),
            principalId = requestingPrincipalId,
            origin = RequestOrigin.REMOTE_INTERFACE,
            intent = intent,
            targetResources = listOf(resourceId),
            proposedActions = listOf(actionName),
            priority = RequestPriority.NORMAL,
            createdAt = now,
            correlationId = "$requestIdPrefix-${UUID.randomUUID()}",
        )
    }

    companion object {
        /**
         * A fixed, well-known [ResourceId] this Unit's own [ExecutionRequest]
         * always names as its target -- not a per-artefact identity (an
         * artefact does not have Resource-registry identity anywhere in
         * this Unit; its only identity is [EvidenceArtifactId]), but a
         * single, stable stand-in for "the evidence intake capability
         * itself," analogous to a fixed endpoint rather than a
         * per-request resource. Not registered anywhere by this Unit --
         * see [EvidenceCustodian]'s own KDoc.
         */
        val EVIDENCE_INTAKE_RESOURCE_ID: ResourceId = ResourceId("evidence-custodian-intake")

        /**
         * A fixed proposed-action name a future `ActionVocabulary`
         * registration is expected to map to
         * `(PermissionAction.WRITE, ResourceType.DOCUMENT)`. Not
         * registered anywhere by this Unit.
         */
        const val ACCEPT_ACTION_NAME: String = "evidence.accept"

        /**
         * Evidence Custodian, Implementation Plan Phase 4, Unit 3: the
         * retrieval counterpart to [EVIDENCE_INTAKE_RESOURCE_ID] -- a
         * distinct literal value, never reused from acceptance's own
         * convention, since acceptance and retrieval remain separately
         * gated proposals (Scope Lock Section 7: "Analytical access ...
         * requires Permission Engine authorisation," stated independently
         * of acceptance's own gating). Not registered anywhere by this
         * Unit -- see [EvidenceCustodian]'s own KDoc. A real
         * `DefaultPermissionPolicy` will deny every retrieval request
         * through this path until a future Phase 10 ("Runtime
         * integration") Unit registers a Resource under this identifier;
         * that registration is not performed here.
         */
        val EVIDENCE_RETRIEVAL_RESOURCE_ID: ResourceId = ResourceId("evidence-custodian-retrieval")

        /**
         * Evidence Custodian, Implementation Plan Phase 4, Unit 3: the
         * retrieval counterpart to [ACCEPT_ACTION_NAME]. A future
         * `ActionVocabulary` registration is expected to map this name to
         * a resolvable `(PermissionAction, ResourceType)` pair -- likely
         * `(PermissionAction.READ, ResourceType.DOCUMENT)`, though this
         * Unit does not decide that mapping, since deciding it is itself
         * Phase 10 runtime-composition work. Not registered anywhere by
         * this Unit.
         */
        const val RETRIEVE_ACTION_NAME: String = "evidence.retrieve"

        /**
         * Document Ingestion, Authoritative Source Manifest Foundation
         * Implementation. The manifest-retrieval counterpart to
         * [EVIDENCE_RETRIEVAL_RESOURCE_ID] -- a distinct literal value,
         * never reused from bytes-retrieval's own convention, so a
         * manifest read remains its own separately auditable,
         * separately permission-traceable act (Scope Lock Section 17).
         */
        val EVIDENCE_MANIFEST_RETRIEVAL_RESOURCE_ID: ResourceId = ResourceId("evidence-custodian-manifest-retrieval")

        /**
         * Document Ingestion, Authoritative Source Manifest Foundation
         * Implementation. Governed by
         * `docs/architecture/DOCUMENT_INGESTION_AUTHORITATIVE_SOURCE_MANIFEST_RETRIEVAL_SCOPE_LOCK.md`
         * Section 17: registered in `ParkerRuntime.kt`'s own
         * `ActionVocabulary` registration to `(PermissionAction.READ,
         * ResourceType.DOCUMENT)` -- the same pair [RETRIEVE_ACTION_NAME]
         * already uses. No new `PermissionAction` or `ResourceType` was
         * required.
         */
        const val RETRIEVE_MANIFEST_ACTION_NAME: String = "evidence.retrieve-manifest"
    }
}
