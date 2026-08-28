package parker.core.runtime

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Instant
import parker.core.interfaces.*

/** Immutable, owner-issued authority for one bounded pending-lifecycle acceptance execution. */
data class FidelityFirstAcceptanceAuthority(
    val authorityId: String,
    val programmeUnit: String,
    val executionId: String,
    val requestId: String,
    val attemptId: String,
    val evidenceArtifactId: String,
    val sourceSha256: String,
    val sourceByteLength: Long,
    val sourceMediaType: String,
    val productionCommit: String,
    val capabilityId: String,
    val adapterId: String,
    val adapterVersion: String,
    val modelAlias: String,
    val profileId: String,
    val processingProfile: String,
    val reasoning: String,
    val store: Boolean,
    val sourceDetail: String,
    val instructionSha256: String,
    val schemaSha256: String,
    val maximumProviderAttempts: Int,
    val authorisedBy: String,
    val authorisedAt: Instant,
) {
    init {
        val opaque = Regex("^[A-Za-z0-9_.-]{1,120}$")
        require(listOf(authorityId, programmeUnit, executionId, requestId, attemptId).all(opaque::matches))
        require(EvidenceArtifactId(evidenceArtifactId).value == evidenceArtifactId)
        require(listOf(sourceSha256, instructionSha256, schemaSha256).all(SHA256::matches))
        require(COMMIT.matches(productionCommit))
        require(sourceByteLength > 0 && maximumProviderAttempts == 1)
        require(!store) { "fidelity-first acceptance authority requires store=false" }
        require(fields().all { (_, value) -> value.isNotBlank() && value.length <= 4_096 && value.none { it in "\r\n\t" } })
    }

    internal fun fields(): List<Pair<String, String>> = listOf(
        "authorityId" to authorityId, "programmeUnit" to programmeUnit, "executionId" to executionId,
        "requestId" to requestId, "attemptId" to attemptId, "evidenceArtifactId" to evidenceArtifactId,
        "sourceSha256" to sourceSha256, "sourceByteLength" to sourceByteLength.toString(),
        "sourceMediaType" to sourceMediaType, "productionCommit" to productionCommit,
        "capabilityId" to capabilityId, "adapterId" to adapterId, "adapterVersion" to adapterVersion,
        "modelAlias" to modelAlias, "profileId" to profileId, "processingProfile" to processingProfile,
        "reasoning" to reasoning, "store" to store.toString(), "sourceDetail" to sourceDetail,
        "instructionSha256" to instructionSha256, "schemaSha256" to schemaSha256,
        "maximumProviderAttempts" to maximumProviderAttempts.toString(), "authorisedBy" to authorisedBy,
        "authorisedAt" to authorisedAt.toString(),
    )

    fun executionIdentity() = FidelityFirstExecutionIdentity(
        executionId, requestId, attemptId, evidenceArtifactId, sourceSha256, sourceByteLength,
        sourceMediaType, productionCommit, "OpenAI", modelAlias, profileId, instructionSha256,
        schemaSha256, processingProfile, adapterVersion,
    )

    companion object {
        private val SHA256 = Regex("^[0-9a-f]{64}$")
        private val COMMIT = Regex("^[0-9a-f]{40}$")
    }
}

/** Create-once, check-summed authority storage. Existing identities can never be overwritten. */
class FileSystemFidelityFirstAcceptanceAuthorityStorage(storageRoot: Path) {
    private val root = storageRoot.toAbsolutePath().normalize()
    init { require(Files.isDirectory(root) && Files.isReadable(root) && Files.isWritable(root)) }

    fun admit(authority: FidelityFirstAcceptanceAuthority) {
        val target = target(authority.authorityId)
        val bytes = encode(authority).toByteArray(StandardCharsets.UTF_8)
        try {
            FileChannel.open(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use {
                write(it, bytes); it.force(true)
            }
            forceDirectory()
        } catch (e: java.nio.file.FileAlreadyExistsException) {
            require(load(authority.authorityId) == authority) { "acceptance authority identity conflict" }
        }
    }

    fun load(authorityId: String): FidelityFirstAcceptanceAuthority? {
        require(ID.matches(authorityId))
        val target = target(authorityId)
        if (!Files.exists(target)) return null
        require(Files.isRegularFile(target) && Files.size(target) in 1..MAX_BYTES)
        return decode(Files.readString(target))
    }

    private fun target(id: String) = root.resolve("$id.acceptance-authority").normalize().also { require(it.parent == root) }
    private fun encode(authority: FidelityFirstAcceptanceAuthority): String {
        val payload = authority.fields().joinToString("\t") { "${it.first}=${it.second}" }
        return "$payload\tchecksum=${sha256(payload)}\n"
    }
    private fun decode(text: String): FidelityFirstAcceptanceAuthority {
        require(text.endsWith('\n') && text.count { it == '\n' } == 1)
        val parts = text.trimEnd('\n').split('\t'); require(parts.size == 25)
        val payload = parts.dropLast(1).joinToString("\t")
        require(parts.last() == "checksum=${sha256(payload)}") { "acceptance authority checksum mismatch" }
        val pairs = parts.dropLast(1).map { part ->
            val i = part.indexOf('='); require(i > 0); part.substring(0, i) to part.substring(i + 1)
        }
        require(pairs.map { it.first }.distinct().size == pairs.size)
        val f = pairs.toMap()
        return FidelityFirstAcceptanceAuthority(
            f.getValue("authorityId"), f.getValue("programmeUnit"), f.getValue("executionId"),
            f.getValue("requestId"), f.getValue("attemptId"), f.getValue("evidenceArtifactId"),
            f.getValue("sourceSha256"), f.getValue("sourceByteLength").toLong(), f.getValue("sourceMediaType"),
            f.getValue("productionCommit"), f.getValue("capabilityId"), f.getValue("adapterId"),
            f.getValue("adapterVersion"), f.getValue("modelAlias"), f.getValue("profileId"),
            f.getValue("processingProfile"), f.getValue("reasoning"), f.getValue("store").toBooleanStrict(),
            f.getValue("sourceDetail"), f.getValue("instructionSha256"), f.getValue("schemaSha256"),
            f.getValue("maximumProviderAttempts").toInt(), f.getValue("authorisedBy"), Instant.parse(f.getValue("authorisedAt")),
        ).also { require(it.fields().map { field -> field.first }.toSet() == f.keys) }
    }
    private fun forceDirectory() {
        if (!System.getProperty("os.name").startsWith("Windows", true)) FileChannel.open(root, StandardOpenOption.READ).use { it.force(true) }
    }
    private fun write(channel: FileChannel, bytes: ByteArray) { val b = ByteBuffer.wrap(bytes); while (b.hasRemaining()) channel.write(b) }
    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
    private companion object { val ID = Regex("^[A-Za-z0-9_.-]{1,120}$"); const val MAX_BYTES = 64L * 1024L }
}

data class FidelityFirstEffectiveConfiguration(
    val capabilityId: String, val adapterId: String, val adapterVersion: String,
    val modelAlias: String, val profileId: String, val processingProfile: String,
    val reasoning: String, val store: Boolean, val sourceDetail: String,
    val instructionSha256: String, val schemaSha256: String,
)

enum class FidelityFirstAcceptanceLifecycle { DISABLED, ACCEPTANCE_PENDING, ACCEPTED, SUSPENDED, INVALID }

sealed interface FidelityFirstAcceptanceOutcome {
    data class Admitted(val authorityId: String, val attemptId: String, val generationId: DerivativeGenerationId) : FidelityFirstAcceptanceOutcome
    data class Blocked(val reason: String) : FidelityFirstAcceptanceOutcome
    data class Failed(val reason: String, val attemptStarted: Boolean) : FidelityFirstAcceptanceOutcome
}

/** Acceptance-only orchestration; the ordinary owner lane never receives this coordinator or mechanism factory. */
class FidelityFirstAcceptanceCoordinator(
    private val authorities: FileSystemFidelityFirstAcceptanceAuthorityStorage,
    private val ledger: FileSystemFidelityFirstAttemptLedger,
    private val lifecycle: () -> FidelityFirstAcceptanceLifecycle,
    private val effectiveConfiguration: () -> FidelityFirstEffectiveConfiguration?,
    private val deployedCommit: () -> String,
    private val ownerPrincipalId: PrincipalId,
    evidenceCustodian: EvidenceCustodian,
    private val permissionEngine: PermissionEngine,
    private val mechanismFactory: (OpenAiTransportLifecycleObserver) -> ExternalTranscriptionMechanism,
    private val validator: OcrStructuredResultValidator,
    private val durableAdmission: ValidatedExternalTranscriptionAdmission,
) {
    private val sourceResolver = AuthoritativeAcquisitionSourceResolver(evidenceCustodian)
    private val custodian = evidenceCustodian

    suspend fun invoke(authorityId: String): FidelityFirstAcceptanceOutcome {
        val authority = try { authorities.load(authorityId) } catch (_: Exception) { return blocked("AUTHORITY_CORRUPT") }
            ?: return blocked("AUTHORITY_MISSING")
        if (lifecycle() != FidelityFirstAcceptanceLifecycle.ACCEPTANCE_PENDING) return blocked("LIFECYCLE_NOT_ACCEPTANCE_PENDING")
        val configuration = effectiveConfiguration() ?: return blocked("CONFIGURATION_UNAVAILABLE")
        if (!matches(authority, configuration)) return blocked("AUTHORITY_CONFIGURATION_MISMATCH")
        if (deployedCommit() != authority.productionCommit) return blocked("AUTHORITY_COMMIT_MISMATCH")

        val evidenceId = try { EvidenceArtifactId(authority.evidenceArtifactId) } catch (_: Exception) { return blocked("AUTHORITY_SOURCE_ID_INVALID") }
        val trusted = when (val resolved = sourceResolver.resolveSourceThenManifest(ownerPrincipalId, evidenceId)) {
            is AuthoritativeAcquisitionResolution.Verified -> resolved.input
            else -> return blocked("AUTHORITY_SOURCE_UNAVAILABLE")
        }
        if (trusted.sha256 != authority.sourceSha256 || trusted.byteLength != authority.sourceByteLength ||
            trusted.mediaType != authority.sourceMediaType
        ) return blocked("AUTHORITY_SOURCE_MISMATCH")

        val tracker = FidelityFirstAttemptTracker(ledger, authority.executionIdentity())
        try {
            tracker.authorised()
            tracker.preflightPassed()
        } catch (_: Exception) {
            return blocked("ATTEMPT_UNAVAILABLE")
        }

        val coordinator = ExternalTranscriptionOwnerInvocationCoordinator(
            ownerPrincipalId, permissionEngine, custodian, mechanismFactory(tracker), validator, durableAdmission,
            invocationObserver = tracker,
            executionBinding = ExternalTranscriptionExecutionBinding(authority.requestId, authority.attemptId, authority.profileId),
        )
        return try {
            when (val outcome = coordinator.invoke(evidenceId)) {
                is ExternalTranscriptionOwnerInvocationOutcome.Admitted -> {
                    tracker.terminalSuccess(outcome.record.derivativeGenerationId.value)
                    FidelityFirstAcceptanceOutcome.Admitted(authorityId, authority.attemptId, outcome.record.derivativeGenerationId)
                }
                else -> {
                    runCatching { tracker.terminalFailure() }
                    FidelityFirstAcceptanceOutcome.Failed(safeOutcome(outcome), tracker.snapshot().providerAttemptStarted)
                }
            }
        } catch (e: Exception) {
            runCatching { tracker.terminalFailure() }
            FidelityFirstAcceptanceOutcome.Failed("ACCEPTANCE_EXECUTION_FAILED", runCatching { tracker.snapshot().providerAttemptStarted }.getOrDefault(false))
        }
    }

    private fun matches(a: FidelityFirstAcceptanceAuthority, c: FidelityFirstEffectiveConfiguration) =
        a.capabilityId == c.capabilityId && a.adapterId == c.adapterId && a.adapterVersion == c.adapterVersion &&
            a.modelAlias == c.modelAlias && a.profileId == c.profileId && a.processingProfile == c.processingProfile &&
            a.reasoning == c.reasoning && a.store == c.store && a.sourceDetail == c.sourceDetail &&
            a.instructionSha256 == c.instructionSha256 && a.schemaSha256 == c.schemaSha256 && a.maximumProviderAttempts == 1
    private fun blocked(reason: String) = FidelityFirstAcceptanceOutcome.Blocked(reason)
    private fun safeOutcome(outcome: ExternalTranscriptionOwnerInvocationOutcome) = when (outcome) {
        is ExternalTranscriptionOwnerInvocationOutcome.MechanismFailure -> "PROVIDER_OR_TRANSPORT_FAILURE"
        is ExternalTranscriptionOwnerInvocationOutcome.ValidationRejected -> "RESPONSE_ADMISSION_REJECTED"
        is ExternalTranscriptionOwnerInvocationOutcome.AdmissionFailed -> "GENERATION_ADMISSION_FAILED"
        is ExternalTranscriptionOwnerInvocationOutcome.ReconciliationRequired -> "GENERATION_RECONCILIATION_REQUIRED"
        else -> "GOVERNED_EXECUTION_REJECTED"
    }
}
