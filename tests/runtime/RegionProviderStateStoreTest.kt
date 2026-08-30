package parker.core.runtime

import java.net.http.HttpTimeoutException
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import parker.composition.OpenAiApiCredential
import parker.core.interfaces.*
import kotlin.test.*

class RegionProviderStateStoreTest {
    @TempDir lateinit var root: Path
    private class Fake(private val response: OpenAiResponsesTransportResponse?, private val timeout: Boolean = false) : OpenAiResponsesTransport {
        var calls = 0
        override suspend fun execute(request: OpenAiResponsesTransportRequest): OpenAiResponsesTransportResponse { calls++; if (timeout) throw HttpTimeoutException("synthetic"); return response!! }
    }

    @Test fun `raw response is durable before parse and restart verifies exact bytes bindings and digests`() = runTest {
        val raw = envelope(validWire("A𝄞\n  B", listOf("synthetic warning"))).toByteArray()
        val store = FileSystemRegionProviderStateStore(root)
        val transport = Fake(OpenAiResponsesTransportResponse(200, raw))
        val outcome = adapter(transport, store).transcribeWithRawState(request())
        assertIs<OpenAiRegionAdapterOutcome.Success>(outcome)
        val id = store.enumerate().single()
        val recovered = FileSystemRegionProviderStateStore(root).read(id)
        assertContentEquals(raw, recovered.rawBytes); assertEquals(regionSha256(raw), recovered.rawDigest)
        assertEquals("SUCCESS", recovered.outcomeCode); assertFalse(recovered.downstreamProcessingPending)
        assertNotNull(recovered.structuredDigest); assertEquals("A𝄞\n  B", ((recovered.exactStructuredState!!["blocks"] as List<*>).single() as Map<*, *>)["literal_text"])
        assertTrue(store.responseExistsFor(request())); assertEquals(1, transport.calls)
    }

    @Test fun `provider order uncertainty warnings and provenance remain canonical`() = runTest {
        val req = request(two = true)
        val blocks = listOf(block(TWO, 3, "second", 1, listOf("w2")), block(ONE, 2, "first", 2, listOf("w1")))
        val store = FileSystemRegionProviderStateStore(root)
        assertIs<OpenAiRegionAdapterOutcome.Success>(adapter(Fake(OpenAiResponsesTransportResponse(200, envelope(wire(blocks)).toByteArray())), store).transcribeWithRawState(req))
        val recovered = FileSystemRegionProviderStateStore(root).read(store.enumerate().single())
        val saved = recovered.exactStructuredState!!["blocks"] as List<*>
        assertEquals(listOf(TWO, ONE), saved.map { (it as Map<*, *>)["source_region_id"] })
        assertEquals(listOf("w2"), (saved[0] as Map<*, *>)["warnings"])
        val exact = requireNotNull(recovered.exactStructuredState)
        assertEquals("resp-r65", (exact["provider_provenance"] as Map<*, *>)["provider_response_id"])
    }

    @Test fun `parse validation and refusal failures retain factual state but timeout invents none`() = runTest {
        suspend fun run(raw: ByteArray): RecoveredRegionProviderState {
            val dir = Files.createDirectory(root.resolve("case-${Files.list(root).use { it.count() }}")); val store = FileSystemRegionProviderStateStore(dir)
            adapter(Fake(OpenAiResponsesTransportResponse(200, raw)), store).transcribeWithRawState(request()); return store.read(store.enumerate().single())
        }
        assertEquals("MALFORMED_PROVIDER_RESPONSE", run("not-json".toByteArray()).outcomeCode)
        val invalid = validWire("literal", emptyList()).toMutableMap(); invalid["correlation_id"] = "wrong"
        val validation = run(envelope(invalid).toByteArray()); assertEquals("VALIDATION_CORRELATION_MISMATCH", validation.outcomeCode); assertNotNull(validation.exactStructuredState)
        assertEquals("PROVIDER_REFUSAL", run(refusal().toByteArray()).outcomeCode)
        val timeoutRoot = Files.createDirectory(root.resolve("timeout")); val timeoutStore = FileSystemRegionProviderStateStore(timeoutRoot)
        assertEquals("PROVIDER_TIMEOUT", assertIs<OpenAiRegionAdapterOutcome.Failure>(adapter(Fake(null, true), timeoutStore).transcribeWithRawState(request())).code)
        assertTrue(timeoutStore.enumerate().isEmpty())
    }

    @Test fun `immutable conflict crash remnants and corruption fail closed`() {
        val store = FileSystemRegionProviderStateStore(root); val request = request(); val raw = "synthetic".toByteArray()
        val receipt = store.persistReceived(request, 200, "application/json", raw)
        assertFailsWith<RegionProviderStateException> { store.persistReceived(request, 200, "application/json", "different".toByteArray()) }
        Files.writeString(root.resolve(".tmp").resolve("interrupted.tmp"), "partial")
        assertEquals(listOf(receipt.recordId), FileSystemRegionProviderStateStore(root).enumerate())
        val path = root.resolve("${receipt.recordId}.provider-state"); val bytes = Files.readAllBytes(path); Files.write(path, bytes.copyOf(bytes.size - 3))
        assertFailsWith<RegionProviderStateException> { FileSystemRegionProviderStateStore(root).read(receipt.recordId) }
    }

    @Test fun `altered raw structured checksum and binding each fail closed`() {
        fun built(): Pair<Path, String> {
            val dir = Files.createDirectory(root.resolve("s${Files.list(root).use { it.count() }}")); val store = FileSystemRegionProviderStateStore(dir)
            val receipt = store.persistReceived(request(), 200, null, "raw".toByteArray()); store.recordAssessment(receipt, "SUCCESS", validWire("x", emptyList())); return dir to receipt.recordId
        }
        listOf("raw_base64", "literal_text", "record_sha256", "source_sha256").forEach { marker ->
            val (dir, id) = built(); val target = if (marker == "literal_text") dir.resolve("$id.assessment") else dir.resolve("$id.provider-state")
            val text = Files.readString(target)
            val changed = when (marker) { "raw_base64" -> text.replace("cmF3", "YmFk"); "literal_text" -> text.replace("\"x\"", "\"y\""); "record_sha256" -> text.replace(Regex("(?<=record_sha256\\\":\\\")[0-9a-f]")) { if (it.value == "0") "1" else "0" }; else -> text.replace("a".repeat(64), "b".repeat(64)) }
            Files.writeString(target, changed); assertFailsWith<RegionProviderStateException>(marker) { FileSystemRegionProviderStateStore(dir).read(id) }
        }
    }

    private fun adapter(t: Fake, s: FileSystemRegionProviderStateStore) = OpenAiRegionTranscriptionAdapter(OpenAiApiCredential.fromEnvironment("R65_SYNTHETIC_KEY")!!, t, providerStateStore = s)
    private fun request(two: Boolean = false): RegionTranscriptionRequest = RegionTranscriptionRequest("r65-correlation", REGION_TRANSCRIPTION_PROFILE_ID, REGION_TRANSCRIPTION_SCHEMA_ID, REGION_TRANSCRIPTION_WIRE_VERSION, REGION_TRANSCRIPTION_SCHEMA_SHA256, REGION_TRANSCRIPTION_PROCESSING_PROFILE, REGION_LITERAL_TRANSCRIPTION_INSTRUCTION, if (two) listOf(target(ONE, 2), target(TWO, 3)) else listOf(target(ONE, 2)))
    private fun target(id: String, page: Int): RegionTranscriptionTarget { val bytes=byteArrayOf(1,2,3); val bounds=PixelCropBounds(1,2,11,12); val pid=PageRepresentationId("b".repeat(64)); val crop=CanonicalPixelDigest("c".repeat(64)); return RegionTranscriptionTarget(EvidenceArtifactId("evidence-r65"), "a".repeat(64), pid, page, PagePixelDimensions(100,100), SourceRegionId(id), bounds, crop, SourceRegionStructuralClass.TEXT_LIKE, "pixel-whitespace-source-regions-v1", 1, RegionTranscriptionImage(pid,bounds,crop,"image/png",RegionTranscriptionImage.sha256(bytes),bytes)) }
    private fun validWire(text: String, warnings: List<String>) = wire(listOf(block(ONE,2,text,1,warnings)))
    private fun wire(blocks: List<Map<String,Any?>>) = linkedMapOf<String,Any?>("correlation_id" to "r65-correlation","transcription_profile_id" to REGION_TRANSCRIPTION_PROFILE_ID,"schema_id" to REGION_TRANSCRIPTION_SCHEMA_ID,"schema_version" to REGION_TRANSCRIPTION_WIRE_VERSION,"provider_provenance" to mapOf("provider" to "OpenAI","requested_model" to OPENAI_REGION_MODEL,"provider_reported_model" to OPENAI_REGION_MODEL,"provider_response_id" to "resp-r65","adapter_id" to OPENAI_REGION_ADAPTER_ID,"adapter_version" to OPENAI_REGION_ADAPTER_VERSION,"parser_id" to OPENAI_REGION_PARSER_ID,"parser_version" to OPENAI_REGION_PARSER_VERSION),"blocks" to blocks)
    private fun block(id:String,page:Int,text:String,ordinal:Int,warnings:List<String>) = mapOf<String,Any?>("source_region_id" to id,"page_number" to page,"literal_text" to text,"status" to "TRANSCRIBED","uncertainties" to emptyList<Any>(),"warnings" to warnings,"provider_returned_ordinal" to ordinal,"visual_observations" to emptyList<Any>())
    private fun envelope(wire: Map<String,Any?>) = RegionJson.encode(mapOf("id" to "resp-r65","model" to OPENAI_REGION_MODEL,"output" to listOf(mapOf("type" to "message","content" to listOf(mapOf("type" to "output_text","text" to RegionJson.encode(wire)))))))
    private fun refusal() = RegionJson.encode(mapOf("id" to "resp-r65","model" to OPENAI_REGION_MODEL,"output" to listOf(mapOf("type" to "message","content" to listOf(mapOf("type" to "refusal","refusal" to "synthetic refusal"))))))
    companion object { val ONE="1f"+"0".repeat(62); val TWO="2f"+"0".repeat(62) }
}
