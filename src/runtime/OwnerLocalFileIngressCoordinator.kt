package parker.core.runtime

import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.LinkOption
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.util.UUID
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.EvidenceAcceptanceResult
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.OwnerLocalFileIngressOutcome
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId

/**
 * Document Ingestion, Owner-Authorized Local File Ingress. Implements
 * exactly the chain
 * `docs/architecture/DOCUMENT_INGESTION_OWNER_AUTHORIZED_LOCAL_FILE_INGRESS_SCOPE_LOCK.md`
 * ("the Scope Lock") Section 2/Section 21's "Implementation authorization
 * consequence" fixes, and no more:
 *
 * Permission evaluation (Section 3, before any filesystem access) ->
 * absolute-path validation (Section 4) -> symlink rejection, every path
 * component (Section 5) -> regular-file validation (Section 6/Section 8)
 * -> 64 MiB size bound (Section 7) -> single-open, bounded, byte-exact
 * read (Section 8/Section 9) -> [CandidateEvidenceArtifact] construction
 * (Section 10-12) -> [EvidenceCustodian.accept], exactly once (Section
 * 13). Operation ends there -- no Tier A, OCR, Tier B, Memory Core,
 * Knowledge, or Evidence Intelligence call of any kind (Section 14).
 *
 * **Two dependencies.** Unlike [TierAOwnerInvocationCoordinator] (which
 * needs no [PermissionEngine] of its own because both operations it
 * sequences are already internally gated), this coordinator performs a
 * genuinely new, distinct gated act -- reading a local file the owner
 * designates -- before ever reaching [EvidenceCustodian.accept]'s own,
 * separate, unchanged gate (Scope Lock Section 3's own two-layer model).
 * [permissionEngine] backs that first layer only; [evidenceCustodian]'s
 * own internal `PermissionEngine` reference backs the second,
 * independently, unchanged.
 *
 * **No new `PermissionAction`/`ResourceType`.** [LOCAL_FILE_READ_ACTION_NAME]
 * maps to the identical existing `(PermissionAction.WRITE, ResourceType.DOCUMENT)`
 * pair `"evidence.accept"` itself already uses, evaluated against the
 * identical existing [DefaultEvidenceCustodian.EVIDENCE_INTAKE_RESOURCE_ID]
 * resource (Scope Lock Section 3).
 */
class OwnerLocalFileIngressCoordinator(
    private val permissionEngine: PermissionEngine,
    private val evidenceCustodian: EvidenceCustodian,
) {
    suspend fun invoke(
        ownerPrincipalId: PrincipalId,
        absolutePath: String,
        receivedMediaType: String?,
    ): OwnerLocalFileIngressOutcome {
        val decision = permissionEngine.evaluate(
            ExecutionRequest(
                requestId = RequestId("local-file-ingress-${UUID.randomUUID()}"),
                principalId = ownerPrincipalId,
                origin = RequestOrigin.REMOTE_INTERFACE,
                intent = "Read one owner-designated local file for Evidence Custodian acceptance",
                targetResources = listOf(DefaultEvidenceCustodian.EVIDENCE_INTAKE_RESOURCE_ID),
                proposedActions = listOf(LOCAL_FILE_READ_ACTION_NAME),
                priority = RequestPriority.NORMAL,
                createdAt = Instant.now(),
                correlationId = "local-file-ingress-${UUID.randomUUID()}",
            ),
        )

        if (decision.decision != PermissionDecisionOutcome.APPROVED &&
            decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) {
            return OwnerLocalFileIngressOutcome.AuthorizationRejected(
                "Permission Engine did not authorise local file ingress for principal " +
                    "'${ownerPrincipalId.value}' (decision=${decision.decision})",
            )
        }

        // Absolute-path validation (Scope Lock Section 4). Path.normalize() is purely lexical --
        // it never touches the filesystem and never resolves a symlink -- it exists only to
        // collapse "." / ".." components so the component-by-component symlink walk below and the
        // eventual open both address the exact same, single designated object.
        val path = try {
            Path.of(absolutePath).normalize()
        } catch (e: InvalidPathException) {
            return OwnerLocalFileIngressOutcome.InvalidPath
        }
        if (!path.isAbsolute) {
            return OwnerLocalFileIngressOutcome.InvalidPath
        }

        // Symlink rejection (Scope Lock Section 5): every component of the resolved path, not
        // only the final one. Files.isSymbolicLink(component) inspects exactly that path's own
        // final segment via lstat-equivalent semantics -- it never follows the link under test --
        // so walking every prefix from the leaf to the root, without ever opening or following
        // any of them, correctly detects a symlinked ancestor exactly as it detects a symlinked
        // leaf, including a broken symlink (whose own link type is unaffected by its target's
        // absence).
        var component: Path? = path
        while (component != null) {
            if (Files.isSymbolicLink(component)) {
                return OwnerLocalFileIngressOutcome.SymlinkProhibited
            }
            component = component.parent
        }

        // Single combined attributes read (Scope Lock Section 9's "single-open, best-effort"
        // discipline, applied here to the type/size check): one stat call establishes both
        // regular-file-type and size, immediately followed by the open+read below, minimizing --
        // never claiming to eliminate -- the gap between validating what the path names and
        // reading what it names. NOFOLLOW_LINKS is passed defensively even though the walk above
        // already confirmed no component is a symlink at the moment of that walk.
        val attributes = try {
            Files.readAttributes(path, BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
        } catch (e: NoSuchFileException) {
            return OwnerLocalFileIngressOutcome.PathNotFound
        } catch (e: IOException) {
            return OwnerLocalFileIngressOutcome.PathNotFound
        }

        if (!attributes.isRegularFile) {
            return OwnerLocalFileIngressOutcome.NotARegularFile
        }
        if (attributes.size() > MAX_SOURCE_BYTES) {
            return OwnerLocalFileIngressOutcome.SourceTooLarge(attributes.size())
        }

        val content = try {
            boundedRead(path, attributes.size())
        } catch (e: OversizeDuringReadException) {
            return OwnerLocalFileIngressOutcome.SourceTooLarge(e.observedByteLength)
        } catch (e: InconsistentSourceException) {
            return OwnerLocalFileIngressOutcome.SourceReadFailure(
                "source byte count changed between validation and read completion",
            )
        } catch (e: IOException) {
            return OwnerLocalFileIngressOutcome.SourceReadFailure("source read failed")
        }

        val originalFileName = path.fileName?.toString()
        val candidate = CandidateEvidenceArtifact(
            content = content,
            receivedMediaType = receivedMediaType,
            originalFileName = originalFileName,
        )

        return when (val result = evidenceCustodian.accept(ownerPrincipalId, candidate)) {
            is EvidenceAcceptanceResult.Accepted -> OwnerLocalFileIngressOutcome.Accepted(result.acceptedEvidenceArtifact)
            is EvidenceAcceptanceResult.Rejected -> OwnerLocalFileIngressOutcome.EvidenceCustodianRejected(result.reason)
        }
    }

    /**
     * Single-open, bounded, byte-exact read (Scope Lock Section 8/Section 9). Reads through one
     * opened channel, never more than [MAX_SOURCE_BYTES] + 1 bytes -- enough to distinguish
     * "exactly at the bound" from "grew past it" without ever buffering an unbounded amount.
     * [expectedByteLength] is the size [Files.readAttributes] observed moments earlier; a final
     * byte count that disagrees with it is exactly the "detected... modification... fails closed"
     * case Scope Lock Section 9 requires, never silently tolerated as a successful read of
     * whatever happened to be there.
     *
     * Internal, not private, solely so [OwnerLocalFileIngressCoordinatorTest] can prove the
     * mismatch-detection branch directly and deterministically (by supplying a real file and a
     * deliberately wrong [expectedByteLength]) rather than racing a real concurrent filesystem
     * mutation, which no portable, non-flaky test could otherwise construct. Not part of the
     * public production API -- [invoke] remains this class's only public surface.
     */
    internal fun boundedRead(path: Path, expectedByteLength: Long): ByteArray {
        Files.newInputStream(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS).use { input ->
            val buffer = ByteArray((MAX_SOURCE_BYTES + 1).toInt())
            var totalRead = 0
            while (totalRead < buffer.size) {
                val n = input.read(buffer, totalRead, buffer.size - totalRead)
                if (n < 0) break
                totalRead += n
            }
            if (totalRead > MAX_SOURCE_BYTES) {
                throw OversizeDuringReadException(observedByteLength = totalRead.toLong())
            }
            if (totalRead.toLong() != expectedByteLength) {
                throw InconsistentSourceException()
            }
            return buffer.copyOf(totalRead)
        }
    }

    private class OversizeDuringReadException(val observedByteLength: Long) : IOException()
    private class InconsistentSourceException : IOException()

    companion object {
        /**
         * A new, distinct action-vocabulary verb phrase (Scope Lock Section 3), mapped by
         * runtime composition to the identical existing `(PermissionAction.WRITE,
         * ResourceType.DOCUMENT)` pair `DefaultEvidenceCustodian.ACCEPT_ACTION_NAME`
         * ("evidence.accept") already uses -- not registered anywhere by this class, mirroring
         * `ACCEPT_ACTION_NAME`'s own "not registered anywhere by this Unit" precedent exactly.
         */
        const val LOCAL_FILE_READ_ACTION_NAME: String = "evidence.import-local-file"

        /** 64 MiB (Scope Lock Section 7) -- an ingress-boundary bound, independent of, and never substituting for, any Tier A specialist's own bound. */
        const val MAX_SOURCE_BYTES: Long = 64L * 1024L * 1024L
    }
}
