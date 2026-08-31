package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Base64
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import kotlin.test.*

class OrdinaryRequestRegionV8TruthfulContractTest {
    @Test fun `v8 is distinct acceptance-pending and provider observations are absent`() {
        val capability=OrdinaryRequestRegionV8Capability()
        assertEquals(ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID,capability.capabilityId)
        assertEquals(RequestRegionV8CapabilityLifecycle.ACCEPTANCE_PENDING,capability.lifecycle)
        assertNotEquals(OrdinaryRequestRegionV7Capability().digest(),capability.digest())
        assertFalse(REQUEST_REGION_V8_SCHEMA_SOURCE.contains("visual_observations"))
        assertContains(REQUEST_REGION_V8_INSTRUCTION,"Never")
        assertContains(REQUEST_REGION_V8_INSTRUCTION,"character-level visual observations")
        println("OI10R3_V8 capabilityDigest=${capability.digest()} instructionDigest=$REQUEST_REGION_V8_INSTRUCTION_SHA256 schemaDigest=$REQUEST_REGION_V8_SCHEMA_SHA256")
    }

    @Test fun `feasibility projection preserves literals identity order uncertainty and drops anchors`() {
        val v7=linkedMapOf<String,Any?>("correlation_id" to "x","transcription_profile_id" to REQUEST_REGION_V7_PROFILE_ID,
            "schema_id" to REQUEST_REGION_V7_SCHEMA_ID,"schema_version" to 7,"provider_provenance" to linkedMapOf("provider" to "OpenAI",
                "requested_model" to REQUEST_REGION_MODEL,"provider_reported_model" to REQUEST_REGION_MODEL,"provider_response_id" to null,
                "adapter_id" to REQUEST_REGION_ADAPTER_ID,"adapter_version" to REQUEST_REGION_V7_ADAPTER_VERSION,"parser_id" to REQUEST_REGION_PARSER_ID,"parser_version" to REQUEST_REGION_V7_PARSER_VERSION),
            "blocks" to listOf(linkedMapOf("request_region_id" to "a".repeat(64),"page_number" to 1,"literal_text" to "A\nB","status" to "TRANSCRIBED",
                "uncertainties" to emptyList<Any>(),"warnings" to emptyList<Any>(),"provider_returned_ordinal" to 1,
                "visual_observations" to listOf(mapOf("kind" to "LINE_BREAK","start_code_point" to 99,"end_code_point_exclusive" to 99)))))
        val projected=RequestRegionV8FeasibilityProjector.projectHistoricalV7(v7)
        @Suppress("UNCHECKED_CAST") val block=(projected["blocks"] as List<Map<String,Any?>>).single()
        assertEquals("A\nB",block["literal_text"]);assertEquals("a".repeat(64),block["request_region_id"])
        assertFalse(block.containsKey("visual_observations"));assertEquals(REQUEST_REGION_V8_SCHEMA_ID,projected["schema_id"])
    }

    @Test fun `preserved OI10R2 responses replay without converting anchors into evidence`() {
        val paths=listOf("OI10R2_A_RESPONSE_PATH","OI10R2_B_RESPONSE_PATH").mapNotNull{System.getenv(it)?.let(Path::of)}
        assumeTrue(paths.size==2&&paths.all(Files::isRegularFile),"preserved OI10R2 responses not supplied")
        paths.forEach { path ->
            @Suppress("UNCHECKED_CAST") val outer=RegionJson.parse(Files.readString(path)) as Map<String,Any?>
            @Suppress("UNCHECKED_CAST") val record=outer["record"] as Map<String,Any?>;val raw=Base64.getDecoder().decode(record["raw_base64"] as String)
            assertEquals(record["raw_sha256"],sha(raw));@Suppress("UNCHECKED_CAST") val envelope=RegionJson.parse(raw.toString(Charsets.UTF_8)) as Map<String,Any?>
            @Suppress("UNCHECKED_CAST") val output=envelope["output"] as List<Map<String,Any?>>;@Suppress("UNCHECKED_CAST") val content=output.single{it["type"]=="message"}["content"] as List<Map<String,Any?>>
            @Suppress("UNCHECKED_CAST") val v7=RegionJson.parse(content.single{it["type"]=="output_text"}["text"] as String) as Map<String,Any?>;val v8=RequestRegionV8FeasibilityProjector.projectHistoricalV7(v7)
            @Suppress("UNCHECKED_CAST") val oldBlocks=v7["blocks"] as List<Map<String,Any?>>;@Suppress("UNCHECKED_CAST") val newBlocks=v8["blocks"] as List<Map<String,Any?>>
            assertEquals(oldBlocks.map{it["request_region_id"] to it["literal_text"]},newBlocks.map{it["request_region_id"] to it["literal_text"]})
            assertTrue(newBlocks.none{it.containsKey("visual_observations")})
        }
    }
    private fun sha(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt()and 255)}
}
