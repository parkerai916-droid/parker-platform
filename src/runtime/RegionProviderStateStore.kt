package parker.core.runtime

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermission
import java.time.Instant
import java.util.Base64
import parker.core.interfaces.RegionTranscriptionRequest

const val REGION_PROVIDER_STATE_STORE_ID = "filesystem-region-provider-state-v1"

data class RegionProviderStateReceipt(val recordId: String, val requestDigest: String, val rawDigest: String)
data class RecoveredRegionProviderState(
    val recordId: String, val requestDigest: String, val rawDigest: String, val rawBytes: ByteArray,
    val recordDigest: String, val outcomeCode: String?, val structuredDigest: String?,
    val assessmentDigest: String?, val exactStructuredState: Map<String, Any?>?, val downstreamProcessingPending: Boolean,
)

class RegionProviderStateException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/** Create-once evidential response storage. The response record is final before parsing; assessment is a separate immutable sidecar. */
class FileSystemRegionProviderStateStore(root: Path) {
    private val root = root.toAbsolutePath().normalize()
    private val temporary = this.root.resolve(".tmp")
    init {
        require(Files.isDirectory(this.root) && Files.isWritable(this.root))
        Files.createDirectories(temporary)
        setPrivate(this.root); setPrivate(temporary)
        fsyncDirectory(this.root)
    }

    fun persistReceived(request: RegionTranscriptionRequest, statusCode: Int, contentType: String?, raw: ByteArray): RegionProviderStateReceipt {
        require(raw.isNotEmpty() && raw.size <= MAX_RAW_BYTES)
        val requestMap = requestBinding(request)
        val requestDigest = regionSha256(canonical(requestMap))
        val rawDigest = regionSha256(raw)
        val recordId = regionSha256("${request.correlationId}|OpenAI|$OPENAI_REGION_ADAPTER_ID|$OPENAI_REGION_ADAPTER_VERSION|$requestDigest".toByteArray())
        val factual = linkedMapOf<String, Any?>(
            "format" to REGION_PROVIDER_STATE_STORE_ID, "record_id" to recordId,
            "request_digest" to requestDigest, "request_binding" to requestMap,
            "provider" to "OpenAI", "adapter_id" to OPENAI_REGION_ADAPTER_ID, "adapter_version" to OPENAI_REGION_ADAPTER_VERSION,
            "parser_id" to OPENAI_REGION_PARSER_ID, "parser_version" to OPENAI_REGION_PARSER_VERSION,
            "requested_model" to OPENAI_REGION_MODEL, "http_status" to statusCode,
            "content_type" to contentType, "received_at" to Instant.now().toString(),
            "raw_length" to raw.size, "raw_sha256" to rawDigest,
            "raw_base64" to Base64.getEncoder().encodeToString(raw),
        )
        val recordDigest = regionSha256(canonical(factual))
        val encoded = canonical(linkedMapOf("record" to factual, "record_sha256" to recordDigest))
        createOnce(recordPath(recordId), encoded, allowExact = true)
        return RegionProviderStateReceipt(recordId, requestDigest, rawDigest)
    }

    fun recordAssessment(receipt: RegionProviderStateReceipt, outcomeCode: String, exactStructured: Map<String, Any?>?) {
        val structuredBytes = exactStructured?.let(::canonical)
        require(structuredBytes == null || structuredBytes.size <= MAX_STRUCTURED_BYTES)
        val assessment = linkedMapOf<String, Any?>(
            "format" to "region-provider-state-assessment-v1", "record_id" to receipt.recordId,
            "request_digest" to receipt.requestDigest, "raw_sha256" to receipt.rawDigest,
            "outcome_code" to outcomeCode, "structured_sha256" to structuredBytes?.let(::regionSha256),
            "exact_structured_state" to exactStructured,
        )
        val digest = regionSha256(canonical(assessment))
        createOnce(assessmentPath(receipt.recordId), canonical(linkedMapOf("assessment" to assessment, "assessment_sha256" to digest)), allowExact = true)
    }

    fun read(recordId: String): RecoveredRegionProviderState {
        require(recordId.matches(Regex("^[0-9a-f]{64}$")))
        val top = decodeVerified(recordPath(recordId), "record", "record_sha256")
        requireString(top, "record_id", recordId)
        val requestBinding = map(top["request_binding"])
        val requestDigest = string(top, "request_digest")
        if (regionSha256(canonical(requestBinding)) != requestDigest) corrupt("request binding digest mismatch")
        val raw = try { Base64.getDecoder().decode(string(top, "raw_base64")) } catch (e: Exception) { corrupt("invalid raw encoding", e) }
        val rawDigest = string(top, "raw_sha256")
        if (raw.size != number(top, "raw_length") || regionSha256(raw) != rawDigest) corrupt("raw response digest mismatch")
        val recordDigest = regionSha256(canonical(top))
        val assessmentFile = assessmentPath(recordId)
        if (!Files.exists(assessmentFile)) return RecoveredRegionProviderState(recordId, requestDigest, rawDigest, raw, recordDigest, null, null, null, null, true)
        val assessment = decodeVerified(assessmentFile, "assessment", "assessment_sha256")
        requireString(assessment, "record_id", recordId); requireString(assessment, "request_digest", requestDigest); requireString(assessment, "raw_sha256", rawDigest)
        @Suppress("UNCHECKED_CAST") val structured = assessment["exact_structured_state"] as? Map<String, Any?>
        val structuredDigest = assessment["structured_sha256"] as? String
        if ((structured == null) != (structuredDigest == null) || (structured != null && regionSha256(canonical(structured)) != structuredDigest)) corrupt("structured state digest mismatch")
        return RecoveredRegionProviderState(recordId, requestDigest, rawDigest, raw, recordDigest,
            string(assessment, "outcome_code"), structuredDigest, regionSha256(canonical(assessment)), structured, false)
    }

    fun enumerate(): List<String> = Files.list(root).use { paths -> paths.filter { it.fileName.toString().endsWith(".provider-state") }.map { it.fileName.toString().removeSuffix(".provider-state") }.sorted().toList() }
    fun requestDigestFor(request: RegionTranscriptionRequest): String = regionSha256(canonical(requestBinding(request)))
    fun recordIdFor(request: RegionTranscriptionRequest): String = regionSha256(
        "${request.correlationId}|OpenAI|$OPENAI_REGION_ADAPTER_ID|$OPENAI_REGION_ADAPTER_VERSION|${requestDigestFor(request)}".toByteArray(),
    )
    fun responseExistsFor(request: RegionTranscriptionRequest): Boolean = Files.exists(recordPath(recordIdFor(request)))
    fun readFor(request: RegionTranscriptionRequest): RecoveredRegionProviderState? =
        if (responseExistsFor(request)) read(recordIdFor(request)).also { require(it.requestDigest == requestDigestFor(request)) } else null

    private fun requestBinding(r: RegionTranscriptionRequest): Map<String, Any?> = linkedMapOf(
        "correlation_id" to r.correlationId, "transcription_profile" to r.transcriptionProfileId,
        "schema_id" to r.schemaId, "schema_version" to r.schemaVersion, "schema_sha256" to r.schemaSha256,
        "processing_profile" to r.processingProfile, "instruction_sha256" to regionSha256(r.literalInstruction.toByteArray()),
        "provider" to "OpenAI", "model" to OPENAI_REGION_MODEL, "adapter_id" to OPENAI_REGION_ADAPTER_ID,
        "adapter_version" to OPENAI_REGION_ADAPTER_VERSION, "reasoning" to "none", "store" to false, "image_detail" to OPENAI_REGION_IMAGE_DETAIL,
        "targets" to r.targets.map { t -> linkedMapOf("evidence_artifact_id" to t.sourceEvidenceArtifactId.value, "source_sha256" to t.sourceSha256,
            "page_representation_id" to t.pageRepresentationId.value, "page_number" to t.pageNumber, "source_region_id" to t.sourceRegionId.value,
            "crop_digest" to t.cropDigest.value, "region_image_sha256" to t.regionImage.encodedSha256,
            "page_context_supplied" to (t.pageContextImage != null), "page_context_sha256" to t.pageContextImage?.encodedSha256) },
    )

    private fun createOnce(target: Path, bytes: ByteArray, allowExact: Boolean) {
        if (Files.exists(target)) {
            if (allowExact && Files.readAllBytes(target).contentEquals(bytes)) return
            throw RegionProviderStateException("immutable provider-state conflict")
        }
        val temp = Files.createTempFile(temporary, "provider-state-", ".tmp")
        try {
            setPrivate(temp)
            FileChannel.open(temp, StandardOpenOption.WRITE).use { c -> val b = ByteBuffer.wrap(bytes); while (b.hasRemaining()) c.write(b); c.force(true) }
            Files.createLink(target, temp)
            Files.delete(temp)
            fsyncDirectory(root)
        } catch (e: Exception) { throw RegionProviderStateException("provider-state persistence failed", e) }
        finally { Files.deleteIfExists(temp) }
    }
    private fun decodeVerified(path: Path, field: String, digestField: String): Map<String, Any?> {
        if (!Files.isRegularFile(path) || Files.size(path) > MAX_RECORD_BYTES) corrupt("missing or excessive record")
        val parsed = try { map(RegionJson.parse(Files.readString(path))) } catch (e: Exception) { corrupt("malformed record", e) }
        val value = map(parsed[field]); if (string(parsed, digestField) != regionSha256(canonical(value))) corrupt("record checksum mismatch"); return value
    }
    private fun recordPath(id: String) = root.resolve("$id.provider-state")
    private fun assessmentPath(id: String) = root.resolve("$id.assessment")
    private fun canonical(value: Any?): ByteArray = RegionJson.encode(value).toByteArray(Charsets.UTF_8)
    @Suppress("UNCHECKED_CAST") private fun map(v: Any?): Map<String, Any?> = v as? Map<String, Any?> ?: corrupt("expected object")
    private fun string(v: Map<String, Any?>, k: String) = v[k] as? String ?: corrupt("missing $k")
    private fun number(v: Map<String, Any?>, k: String) = (v[k] as? Number)?.toInt() ?: corrupt("missing $k")
    private fun requireString(v: Map<String, Any?>, k: String, expected: String) { if (string(v, k) != expected) corrupt("$k mismatch") }
    private fun corrupt(message: String, cause: Throwable? = null): Nothing = throw RegionProviderStateException("corrupt provider state: $message", cause)
    private fun fsyncDirectory(directory: Path) { FileChannel.open(directory, StandardOpenOption.READ).use { it.force(true) } }
    private fun setPrivate(path: Path) { runCatching { Files.setPosixFilePermissions(path, setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE).let { if (Files.isDirectory(path)) it else it - PosixFilePermission.OWNER_EXECUTE }) } }
    companion object { const val MAX_RAW_BYTES = 20 * 1024 * 1024; const val MAX_STRUCTURED_BYTES = 16 * 1024 * 1024; const val MAX_RECORD_BYTES = 32L * 1024 * 1024 }
}
