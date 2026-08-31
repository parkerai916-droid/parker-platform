package parker.core.runtime

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertFails

object V8ReplayResultAttribution {
    fun verify(path:Path,expectedFixture:String,expectedInvocationId:String):Map<String,Any?> {
        require(Files.isRegularFile(path)&&Files.isReadable(path)){"fixture-specific replay result missing"}
        @Suppress("UNCHECKED_CAST") val envelope=RegionJson.parse(path.readText()) as Map<String,Any?>
        require(envelope.keys==setOf("record","record_sha256"))
        @Suppress("UNCHECKED_CAST") val record=envelope["record"] as Map<String,Any?>
        require(envelope["record_sha256"]==sha(RegionJson.encode(record).toByteArray())){"replay result checksum mismatch"}
        require(record["format"]=="oi11r1-v8-isolated-replay-result-v1")
        require(record["fixture"]==expectedFixture){"cross-fixture replay result"}
        require(record["replay_invocation_id"]==expectedInvocationId){"stale replay result"}
        require(record["result"]=="PASS")
        return record
    }
    private fun sha(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt()and 255)}
}

class OrdinaryRequestRegionV8ReplayIsolationTest {
    @Test fun `fixture and invocation attribution fail closed across stale missing and crossed results`() {
        val root=Files.createTempDirectory("oi11r1-replay-isolation-")
        try {
            val paths=(listOf("A","B","C")).associateWith { fixture ->
                val path=root.resolve("$fixture/result.json")
                Files.createDirectories(path.parent)
                val record=linkedMapOf<String,Any?>("format" to "oi11r1-v8-isolated-replay-result-v1","fixture" to fixture,
                    "replay_invocation_id" to "invocation-$fixture","result" to "PASS")
                val bytes=RegionJson.encode(linkedMapOf("record" to record,"record_sha256" to sha(RegionJson.encode(record).toByteArray()))).toByteArray()
                FileChannel.open(path,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE).use { channel ->
                    val buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())channel.write(buffer);channel.force(true)
                };path
            }
            paths.forEach { (fixture,path) -> V8ReplayResultAttribution.verify(path,fixture,"invocation-$fixture") }
            assertFails { V8ReplayResultAttribution.verify(paths.getValue("C"),"A","invocation-A") }
            assertFails { V8ReplayResultAttribution.verify(paths.getValue("A"),"B","invocation-B") }
            assertFails { V8ReplayResultAttribution.verify(paths.getValue("B"),"C","invocation-C") }
            assertFails { V8ReplayResultAttribution.verify(paths.getValue("A"),"A","new-invocation-A") }
            assertFails { V8ReplayResultAttribution.verify(root.resolve("missing/result.json"),"A","invocation-A") }
        } finally { root.toFile().deleteRecursively() }
    }
    private fun sha(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt()and 255)}
}
