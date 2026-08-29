package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Instant
import parker.core.interfaces.EvidenceArtifactId

const val REGION_ACCEPTANCE_AUTHORITY_SCHEMA_V2 = "parker.region-transcription-acceptance-authority.v2"

enum class RegionAcceptancePurposeCode { CONTROLLED_LIVE_FIDELITY_ACCEPTANCE }

data class RegionAcceptancePurpose(val code: RegionAcceptancePurposeCode, val detail: String) {
    init {
        require(detail.matches(Regex("^Controlled live fidelity acceptance of the deployed R6 region-anchored external transcription path using the exact [1-9][0-9]*-page authoritative source and exact deterministically reconstructed [1-9][0-9]*-region request surface\\.$")))
    }
    companion object {
        fun controlled(pageCount: Int, regionCount: Int) = RegionAcceptancePurpose(
            RegionAcceptancePurposeCode.CONTROLLED_LIVE_FIDELITY_ACCEPTANCE,
            "Controlled live fidelity acceptance of the deployed R6 region-anchored external transcription path using the exact $pageCount-page authoritative source and exact deterministically reconstructed $regionCount-region request surface.",
        )
    }
}

data class RegionAcceptanceProviderSurface(
    val provider: String = "OpenAI",
    val endpointFamily: String = "Responses API",
    val endpoint: String = OpenAiRegionTranscriptionAdapter.ENDPOINT.toString(),
    val operation: String = "POST /v1/responses",
    val model: String = OPENAI_REGION_MODEL,
    val reasoning: String = "none",
    val store: Boolean = false,
    val adapterId: String = OPENAI_REGION_ADAPTER_ID,
    val adapterVersion: String = OPENAI_REGION_ADAPTER_VERSION,
    val providerNeutralProfileId: String = parker.core.interfaces.REGION_TRANSCRIPTION_PROFILE_ID,
    val providerSpecificProfileId: String = OPENAI_REGION_PROFILE_ID,
    val processingProfile: String = parker.core.interfaces.REGION_TRANSCRIPTION_PROCESSING_PROFILE,
    val schemaId: String = parker.core.interfaces.REGION_TRANSCRIPTION_SCHEMA_ID,
    val wireVersion: Int = parker.core.interfaces.REGION_TRANSCRIPTION_WIRE_VERSION,
    val schemaSha256: String = REGION_TRANSCRIPTION_SCHEMA_SHA256,
    val providerNeutralInstructionSha256: String = regionSha256(REGION_LITERAL_TRANSCRIPTION_INSTRUCTION.toByteArray()),
    val providerInstructionSha256: String = OPENAI_REGION_INSTRUCTION_SHA256,
) {
    init {
        require(provider == "OpenAI" && endpointFamily == "Responses API" && endpoint == OpenAiRegionTranscriptionAdapter.ENDPOINT.toString())
        require(operation == "POST /v1/responses" && model == OPENAI_REGION_MODEL && reasoning == "none" && !store)
        require(adapterId == OPENAI_REGION_ADAPTER_ID && adapterVersion == OPENAI_REGION_ADAPTER_VERSION)
        require(providerNeutralProfileId == parker.core.interfaces.REGION_TRANSCRIPTION_PROFILE_ID && providerSpecificProfileId == OPENAI_REGION_PROFILE_ID)
        require(processingProfile == parker.core.interfaces.REGION_TRANSCRIPTION_PROCESSING_PROFILE)
        require(schemaId == parker.core.interfaces.REGION_TRANSCRIPTION_SCHEMA_ID && wireVersion == parker.core.interfaces.REGION_TRANSCRIPTION_WIRE_VERSION)
        require(schemaSha256 == REGION_TRANSCRIPTION_SCHEMA_SHA256)
        require(providerNeutralInstructionSha256 == regionSha256(REGION_LITERAL_TRANSCRIPTION_INSTRUCTION.toByteArray()))
        require(providerInstructionSha256 == OPENAI_REGION_INSTRUCTION_SHA256)
    }
}

data class RegionAcceptanceManifestV2(val facts: List<RegionAcceptanceFact>) {
    init {
        require(facts.isNotEmpty() && facts == facts.sortedBy { it.name })
        require(facts.map { it.name }.distinct().size == facts.size)
        val names = facts.mapTo(mutableSetOf()) { it.name }
        REQUIRED.forEach { require(it in names) { "missing v2 region acceptance fact: $it" } }
        require(names.any { it.startsWith("page.") } && names.any { it.startsWith("region.") } && names.any { it.startsWith("order.") })
    }
    fun canonicalBytes(): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { out ->
            write(out, REGION_ACCEPTANCE_AUTHORITY_SCHEMA_V2); out.writeInt(facts.size)
            facts.forEach { write(out, it.name); write(out, it.value) }
        }; bytes.toByteArray()
    }
    fun sha256() = v2Digest(canonicalBytes())
    companion object {
        private val REQUIRED = setOf(
            "source.evidence_artifact_id", "source.sha256", "source.byte_length", "source.media_type",
            "deployment.source_commit", "deployment.build_commit", "deployment.runtime_commit", "deployment.image_id",
            "request.correlation_id", "request.profile_id", "request.processing_profile", "request.schema_id", "request.schema_version", "request.schema_sha256",
            "request.provider_neutral_instruction_sha256", "adapter.provider_instruction_sha256",
            "provider.name", "provider.endpoint_family", "provider.endpoint", "provider.operation", "provider.model", "provider.reasoning", "provider.store",
            "provider.adapter_id", "provider.adapter_version", "provider.profile_id", "provider.wire_version", "provider.image_detail",
            "context.policy", "attempt.maximum_provider_attempts", "authority.purpose_code", "authority.purpose_detail",
        )
        fun canonical(facts: Iterable<RegionAcceptanceFact>) = RegionAcceptanceManifestV2(facts.sortedBy { it.name })
        internal fun decode(bytes: ByteArray): RegionAcceptanceManifestV2 = DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            require(read(input) == REGION_ACCEPTANCE_AUTHORITY_SCHEMA_V2); val count = input.readInt(); require(count in 1..20_000)
            val facts = List(count) { RegionAcceptanceFact(read(input), read(input)) }; require(input.read() == -1); RegionAcceptanceManifestV2(facts)
        }
        internal fun write(out: DataOutputStream, value: String) { val b = value.toByteArray(StandardCharsets.UTF_8); out.writeInt(b.size); out.write(b) }
        internal fun read(input: DataInputStream): String { val size = input.readInt(); require(size in 0..1_000_000); return String(ByteArray(size).also(input::readFully), StandardCharsets.UTF_8) }
    }
}

object RegionAcceptanceManifestV2Factory {
    fun create(reconstruction: RegionAcceptanceReconstruction, purpose: RegionAcceptancePurpose, provider: RegionAcceptanceProviderSurface): RegionAcceptanceManifestV2 {
        val base = reconstruction.manifest.facts.filterNot { it.name == "request.instruction_sha256" }.associateByTo(linkedMapOf()) { it.name }
        fun set(name: String, value: String) { base[name] = RegionAcceptanceFact(name, value) }
        set("request.provider_neutral_instruction_sha256", provider.providerNeutralInstructionSha256)
        set("adapter.provider_instruction_sha256", provider.providerInstructionSha256)
        set("provider.name", provider.provider); set("provider.endpoint_family", provider.endpointFamily); set("provider.endpoint", provider.endpoint)
        set("provider.operation", provider.operation); set("provider.model", provider.model); set("provider.reasoning", provider.reasoning); set("provider.store", provider.store.toString())
        set("provider.adapter_id", provider.adapterId); set("provider.adapter_version", provider.adapterVersion); set("provider.profile_id", provider.providerSpecificProfileId)
        set("provider.wire_version", provider.wireVersion.toString()); set("authority.purpose_code", purpose.code.name); set("authority.purpose_detail", purpose.detail)
        require(base.getValue("request.profile_id").value == provider.providerNeutralProfileId)
        require(base.getValue("request.processing_profile").value == provider.processingProfile)
        require(base.getValue("request.schema_id").value == provider.schemaId && base.getValue("request.schema_version").value == provider.wireVersion.toString())
        require(base.getValue("request.schema_sha256").value == provider.schemaSha256)
        require(reconstruction.binding.identity.instructionSha256 == provider.providerInstructionSha256)
        require(reconstruction.binding.identity.profileId == provider.providerSpecificProfileId)
        return RegionAcceptanceManifestV2.canonical(base.values)
    }
}

data class RegionTranscriptionAcceptanceAuthorityV2(
    val authorityId: String, val programmeUnit: String, val executionId: String,
    val purpose: RegionAcceptancePurpose, val manifest: RegionAcceptanceManifestV2,
    val maximumProviderAttempts: Int, val authorisedBy: String, val authorisedAt: Instant,
) {
    init {
        val id = Regex("^[A-Za-z0-9_.-]{1,120}$"); require(id.matches(authorityId) && id.matches(programmeUnit) && id.matches(executionId))
        require(maximumProviderAttempts == 1 && authorisedBy.isNotBlank() && authorisedBy.length <= 256)
        val facts = manifest.facts.associate { it.name to it.value }
        require(facts["authority.purpose_code"] == purpose.code.name && facts["authority.purpose_detail"] == purpose.detail)
    }
    val manifestSha256 get() = manifest.sha256()
    val recordId get() = v2Digest(canonicalPayload())
    internal fun canonicalPayload(): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { out ->
            listOf(authorityId, programmeUnit, executionId, purpose.code.name, purpose.detail, maximumProviderAttempts.toString(), authorisedBy, authorisedAt.toString())
                .forEach { RegionAcceptanceManifestV2.write(out, it) }
            val manifestBytes = manifest.canonicalBytes(); out.writeInt(manifestBytes.size); out.write(manifestBytes)
        }; bytes.toByteArray()
    }
}

class FileSystemRegionAcceptanceAuthorityStorageV2(storageRoot: Path) {
    private val root = storageRoot.toAbsolutePath().normalize()
    init { require(Files.isDirectory(root) && Files.isReadable(root) && Files.isWritable(root)) }
    fun admit(authority: RegionTranscriptionAcceptanceAuthorityV2) {
        val payload = authority.canonicalPayload(); val envelope = ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { out -> RegionAcceptanceManifestV2.write(out, REGION_ACCEPTANCE_AUTHORITY_SCHEMA_V2); out.writeInt(payload.size); out.write(payload); RegionAcceptanceManifestV2.write(out, v2Digest(payload)); RegionAcceptanceManifestV2.write(out, authority.recordId) }; bytes.toByteArray()
        }
        try {
            FileChannel.open(target(authority.authorityId), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { channel -> val b = ByteBuffer.wrap(envelope); while (b.hasRemaining()) channel.write(b); channel.force(true) }
            if (!System.getProperty("os.name").startsWith("Windows", true)) FileChannel.open(root, StandardOpenOption.READ).use { it.force(true) }
        } catch (_: java.nio.file.FileAlreadyExistsException) { require(load(authority.authorityId) == authority) { "v2 region acceptance authority identity conflict" } }
    }
    fun load(authorityId: String): RegionTranscriptionAcceptanceAuthorityV2? {
        require(ID.matches(authorityId)); val path = target(authorityId); if (!Files.exists(path)) return null
        require(Files.isRegularFile(path) && Files.size(path) in 1..MAX_BYTES)
        return DataInputStream(Files.newInputStream(path)).use { input ->
            require(RegionAcceptanceManifestV2.read(input) == REGION_ACCEPTANCE_AUTHORITY_SCHEMA_V2); val size = input.readInt(); require(size in 1..MAX_BYTES.toInt())
            val payload = ByteArray(size).also(input::readFully); require(RegionAcceptanceManifestV2.read(input) == v2Digest(payload)); val record = RegionAcceptanceManifestV2.read(input); require(input.read() == -1)
            decode(payload).also { require(it.recordId == record) }
        }
    }
    fun exists(authorityId: String) = Files.exists(target(authorityId))
    private fun decode(payload: ByteArray) = DataInputStream(ByteArrayInputStream(payload)).use { input ->
        val authorityId = RegionAcceptanceManifestV2.read(input); val unit = RegionAcceptanceManifestV2.read(input); val execution = RegionAcceptanceManifestV2.read(input)
        val purpose = RegionAcceptancePurpose(RegionAcceptancePurposeCode.valueOf(RegionAcceptanceManifestV2.read(input)), RegionAcceptanceManifestV2.read(input))
        val attempts = RegionAcceptanceManifestV2.read(input).toInt(); val by = RegionAcceptanceManifestV2.read(input); val at = Instant.parse(RegionAcceptanceManifestV2.read(input))
        val size = input.readInt(); require(size in 1..MAX_BYTES.toInt()); val manifest = RegionAcceptanceManifestV2.decode(ByteArray(size).also(input::readFully)); require(input.read() == -1)
        RegionTranscriptionAcceptanceAuthorityV2(authorityId, unit, execution, purpose, manifest, attempts, by, at)
    }
    private fun target(id: String) = root.resolve("$id.region-acceptance-authority-v2").normalize().also { require(it.parent == root) }
    private companion object { val ID = Regex("^[A-Za-z0-9_.-]{1,120}$"); const val MAX_BYTES = 16L * 1024L * 1024L }
}

data class RegionAcceptanceAuthorityCreationRequest(
    val authorityId: String, val programmeUnit: String, val executionId: String, val correlationId: String,
    val evidenceArtifactId: EvidenceArtifactId, val authorisedBy: String, val authorisedAt: Instant,
)
sealed interface RegionAcceptanceAuthorityCreationOutcome {
    data class Created(val authorityId: String, val recordId: String, val manifestSha256: String, val executionIdentity: FidelityFirstExecutionIdentity) : RegionAcceptanceAuthorityCreationOutcome
    data class Blocked(val reason: String) : RegionAcceptanceAuthorityCreationOutcome
}
fun interface RegionAcceptanceCurrentFactsReconstructor { suspend fun reconstruct(evidenceId: EvidenceArtifactId, correlationId: String, executionId: String): RegionAcceptanceReconstruction? }

class RegionTranscriptionAcceptanceAuthorityCreationCoordinator(
    private val authorities: FileSystemRegionAcceptanceAuthorityStorageV2,
    private val reconstructor: RegionAcceptanceCurrentFactsReconstructor,
    private val provider: () -> RegionAcceptanceProviderSurface?,
    private val attemptExists: (FidelityFirstExecutionIdentity) -> Boolean,
    private val providerStateExists: (parker.core.interfaces.RegionTranscriptionRequest) -> Boolean,
) {
    suspend fun create(request: RegionAcceptanceAuthorityCreationRequest): RegionAcceptanceAuthorityCreationOutcome {
        if (authorities.exists(request.authorityId)) return blocked("AUTHORITY_ALREADY_EXISTS")
        val current = try { reconstructor.reconstruct(request.evidenceArtifactId, request.correlationId, request.executionId) } catch (_: Exception) { null }
            ?: return blocked("CURRENT_FACTS_UNAVAILABLE")
        val surface = try { provider() } catch (_: Exception) { null } ?: return blocked("PROVIDER_CONFIGURATION_UNAVAILABLE")
        val pageCount = current.manifest.facts.count { it.name.matches(Regex("^page\\.[0-9]+\\.number$")) }
        val regionCount = current.manifest.facts.count { it.name.matches(Regex("^region\\.[0-9]+\\.id$")) }
        val purpose = try { RegionAcceptancePurpose.controlled(pageCount, regionCount) } catch (_: Exception) { return blocked("PURPOSE_INVALID") }
        val manifest = try { RegionAcceptanceManifestV2Factory.create(current, purpose, surface) } catch (_: Exception) { return blocked("CURRENT_FACTS_INCONSISTENT") }
        if (attemptExists(current.binding.identity)) return blocked("ATTEMPT_ALREADY_EXISTS")
        if (providerStateExists(current.binding.request)) return blocked("PROVIDER_STATE_ALREADY_EXISTS")
        val authority = try { RegionTranscriptionAcceptanceAuthorityV2(request.authorityId, request.programmeUnit, request.executionId, purpose, manifest, 1, request.authorisedBy, request.authorisedAt) }
            catch (_: Exception) { return blocked("AUTHORITY_INVALID") }
        return try {
            authorities.admit(authority)
            RegionAcceptanceAuthorityCreationOutcome.Created(authority.authorityId, authority.recordId, authority.manifestSha256, current.binding.identity)
        } catch (_: Exception) { blocked("AUTHORITY_ADMISSION_FAILED") }
    }
    private fun blocked(reason: String) = RegionAcceptanceAuthorityCreationOutcome.Blocked(reason)
}

sealed interface RegionAcceptanceExecutionOutcomeV2 {
    data class Executed(val authorityId: String, val recordId: String, val outcome: GovernedRegionExecutionOutcome) : RegionAcceptanceExecutionOutcomeV2
    data class Blocked(val reason: String) : RegionAcceptanceExecutionOutcomeV2
}

class RegionAcceptanceExecutionCoordinatorV2(
    private val authorities: FileSystemRegionAcceptanceAuthorityStorageV2,
    private val lifecycle: () -> FidelityFirstAcceptanceLifecycle,
    private val reconstructor: RegionAcceptanceCurrentFactsReconstructor,
    private val provider: () -> RegionAcceptanceProviderSurface?,
    private val execution: GovernedRegionExecutionPort,
) {
    suspend fun invoke(authorityId: String): RegionAcceptanceExecutionOutcomeV2 {
        val authority = try { authorities.load(authorityId) } catch (_: Exception) { return blocked("AUTHORITY_CORRUPT") } ?: return blocked("AUTHORITY_MISSING")
        if (lifecycle() != FidelityFirstAcceptanceLifecycle.ACCEPTANCE_PENDING) return blocked("LIFECYCLE_NOT_ACCEPTANCE_PENDING")
        if (authority.purpose.code != RegionAcceptancePurposeCode.CONTROLLED_LIVE_FIDELITY_ACCEPTANCE) return blocked("PURPOSE_NOT_ACCEPTANCE")
        val facts = authority.manifest.facts.associate { it.name to it.value }
        val evidence = runCatching { EvidenceArtifactId(facts.getValue("source.evidence_artifact_id")) }.getOrNull() ?: return blocked("SOURCE_ID_INVALID")
        val current = try { reconstructor.reconstruct(evidence, facts.getValue("request.correlation_id"), authority.executionId) } catch (_: Exception) { null }
            ?: return blocked("CURRENT_FACTS_UNAVAILABLE")
        val pageCount = current.manifest.facts.count { it.name.matches(Regex("^page\\.[0-9]+\\.number$")) }
        val regionCount = current.manifest.facts.count { it.name.matches(Regex("^region\\.[0-9]+\\.id$")) }
        if (authority.purpose != RegionAcceptancePurpose.controlled(pageCount, regionCount)) return blocked("PURPOSE_FACTS_MISMATCH")
        val surface = try { provider() } catch (_: Exception) { null } ?: return blocked("PROVIDER_CONFIGURATION_UNAVAILABLE")
        val rebuilt = try { RegionAcceptanceManifestV2Factory.create(current, authority.purpose, surface) } catch (_: Exception) { return blocked("CURRENT_FACTS_INCONSISTENT") }
        if (rebuilt != authority.manifest || rebuilt.sha256() != authority.manifestSha256) return blocked("AUTHORITY_FACTS_MISMATCH")
        return RegionAcceptanceExecutionOutcomeV2.Executed(authority.authorityId, authority.recordId, execution.execute(current.binding))
    }
    private fun blocked(reason: String) = RegionAcceptanceExecutionOutcomeV2.Blocked(reason)
}

private fun v2Digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
