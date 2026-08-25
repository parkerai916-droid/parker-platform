package parker.core.runtime

import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.CRC32
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceIntelligence
import parker.core.interfaces.KnowledgeSubmission
import parker.core.interfaces.MemoryCore
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.OcrProviderAdapter
import parker.core.interfaces.OcrRecognitionOutcome
import parker.core.interfaces.OcrRecognitionRequest
import parker.core.interfaces.PermissionEngine

/**
 * OCR Mechanism -- Docling Concrete Adapter Implementation Unit. Pure
 * protocol/validation/structural tests over `DoclingOcrProviderAdapter.kt`
 * -- every test in this file runs offline, deterministically, with no real
 * subprocess, no Python, no Docling installation, and no model of any
 * kind, via [FakeDoclingSubprocessInvoker], mirroring
 * `QmdRelevanceMechanismTest.kt`'s own established convention exactly. No
 * `liveModelEvaluation`-source-set counterpart exists yet -- provisioning
 * does not exist (this task's own governing instruction: "Do NOT add live
 * Docling tests yet if provisioning does not exist").
 */
class DoclingOcrProviderAdapterTest {

    private fun configuration(
        timeoutMillis: Long = 900_000L,
        maxSourceBytes: Long = 67_108_864L,
        maxImageWidthPx: Int = 10_000,
        maxImageHeightPx: Int = 10_000,
        maxImagePixels: Long = 100_000_000L,
        maxOutputTextBytes: Long = 20_971_520L,
        doclingTempDir: String? = null,
    ): DoclingOcrProviderAdapterConfiguration = DoclingOcrProviderAdapterConfiguration(
        pythonExecutablePath = "python3",
        bridgeScriptPath = "tools/docling-ocr-bridge.py",
        timeoutMillis = timeoutMillis,
        maxSourceBytes = maxSourceBytes,
        maxImageWidthPx = maxImageWidthPx,
        maxImageHeightPx = maxImageHeightPx,
        maxImagePixels = maxImagePixels,
        maxOutputTextBytes = maxOutputTextBytes,
        doclingTempDir = doclingTempDir,
    )

    private class FakeDoclingSubprocessInvoker(
        private val respond: (requestJson: String, timeoutMillis: Long) -> DoclingSubprocessInvocationResult,
    ) : DoclingSubprocessInvoker {
        var lastRequestJson: String? = null
            private set
        var invocationCount: Int = 0
            private set

        override fun invoke(requestJson: String, timeoutMillis: Long): DoclingSubprocessInvocationResult {
            lastRequestJson = requestJson
            invocationCount += 1
            return respond(requestJson, timeoutMillis)
        }
    }

    private fun sampleRequest(
        content: ByteArray = byteArrayOf(1, 2, 3, 4, 5),
        mediaType: String = "application/pdf",
    ) = OcrRecognitionRequest(
        sourceEvidenceId = EvidenceArtifactId("evidence-1"),
        content = content,
        mediaType = mediaType,
    )

    private fun success(recognisedText: String = "hello world", extraFields: String = "") =
        DoclingSubprocessInvocationResult(
            exitCode = 0,
            stdout = """{"status":"recognised","recognisedText":${jsonQuote(recognisedText)},"fidelity":"VERBATIM"$extraFields}""",
            stderr = "",
            timedOut = false,
        )

    private fun jsonQuote(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    /** Extracts the `sourceFilePath` field the adapter itself wrote into the request JSON. */
    private fun extractSourceFilePath(requestJson: String): String {
        val match = Regex("\"sourceFilePath\":\"((?:\\\\.|[^\"\\\\])*)\"").find(requestJson)
            ?: error("no sourceFilePath field found in request JSON: $requestJson")
        return match.groupValues[1].replace("\\\\", "\\")
    }

    // Builds a minimal, syntactically valid PNG containing only a real IHDR chunk (declaring
    // width/height), an empty IDAT chunk, and an IEND chunk -- enough for ImageIO's own
    // header-only getWidth()/getHeight() to answer without ever decoding a real raster, so a
    // test proving "declared dimensions exceeding the bound are rejected before decompression"
    // never actually allocates a giant in-memory bitmap (which would defeat the entire point of
    // what is under test).
    private fun buildMinimalPngBytes(width: Int, height: Int): ByteArray {
        val signature = byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10)
        val ihdrData = ByteArray(13)
        writeIntBE(ihdrData, 0, width)
        writeIntBE(ihdrData, 4, height)
        ihdrData[8] = 8 // bit depth
        ihdrData[9] = 6 // color type: RGBA
        ihdrData[10] = 0 // compression
        ihdrData[11] = 0 // filter
        ihdrData[12] = 0 // interlace
        val ihdr = buildChunk("IHDR", ihdrData)
        val idat = buildChunk("IDAT", ByteArray(0))
        val iend = buildChunk("IEND", ByteArray(0))
        return signature + ihdr + idat + iend
    }

    private fun writeIntBE(target: ByteArray, offset: Int, value: Int) {
        target[offset] = ((value ushr 24) and 0xFF).toByte()
        target[offset + 1] = ((value ushr 16) and 0xFF).toByte()
        target[offset + 2] = ((value ushr 8) and 0xFF).toByte()
        target[offset + 3] = (value and 0xFF).toByte()
    }

    private fun buildChunk(type: String, data: ByteArray): ByteArray {
        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        val length = ByteArray(4)
        writeIntBE(length, 0, data.size)
        val crc = CRC32()
        crc.update(typeBytes)
        crc.update(data)
        val crcBytes = ByteArray(4)
        writeIntBE(crcBytes, 0, crc.value.toInt())
        return length + typeBytes + data + crcBytes
    }

    // ---- Contract / boundary ----

    @Test
    fun `DoclingOcrProviderAdapter implements OcrProviderAdapter`() {
        assertTrue(OcrProviderAdapter::class.java.isAssignableFrom(DoclingOcrProviderAdapter::class.java))
    }

    @Test
    fun `DoclingOcrProviderAdapter declares only configuration, invoker, and its own mutex -- no canonical Parker dependency`() {
        val properties = DoclingOcrProviderAdapter::class.declaredMemberProperties.map { it.name }.toSet()
        assertEquals(setOf("configuration", "invoker", "invocationMutex"), properties)
    }

    @Test
    fun `DoclingOcrProviderAdapter constructor accepts no PermissionEngine, persistence, or Memory Core type`() {
        val constructor = DoclingOcrProviderAdapter::class.primaryConstructor
        assertTrue(constructor != null)
        val disallowed = setOf(
            "PermissionEngine",
            "EvidenceCustodian",
            "MemoryCore",
            "MemoryRetrieval",
            "KnowledgeSubmission",
            "EvidenceIntelligence",
            "Socket",
            "URL",
            "URLConnection",
            "HttpClient",
        )
        constructor!!.parameters.forEach { parameter ->
            val typeName = parameter.type.classifier?.toString() ?: ""
            disallowed.forEach { name ->
                assertTrue(!typeName.contains(name), "constructor parameter ${parameter.name} must not reference $name")
            }
        }
    }

    // Mirrors OcrExecutionSequencerTest's own reflection-based prohibited-dependency proof,
    // Phase 15 item 19: no Memory/Knowledge/DerivativeGeneration/EvidenceCustodian side effect
    // is structurally possible.
    @Test
    fun `no property or constructor parameter reachable from DoclingOcrProviderAdapter references EvidenceCustodian, MemoryCore, KnowledgeSubmission, PermissionEngine, or EvidenceIntelligence`() {
        val excludedQualifiedNames = setOf(
            EvidenceCustodian::class.qualifiedName,
            MemoryCore::class.qualifiedName,
            MemoryRetrieval::class.qualifiedName,
            KnowledgeSubmission::class.qualifiedName,
            PermissionEngine::class.qualifiedName,
            EvidenceIntelligence::class.qualifiedName,
        )
        val typesToCheck: List<KClass<*>> = listOf(
            DoclingOcrProviderAdapter::class,
            DoclingOcrProviderAdapterConfiguration::class,
            DoclingSubprocessInvoker::class,
        )
        typesToCheck.forEach { type ->
            val propertyTypeNames = type.declaredMemberProperties.mapNotNull { (it.returnType.classifier as? KClass<*>)?.qualifiedName }
            val constructorTypeNames = type.primaryConstructor?.parameters?.mapNotNull {
                (it.type.classifier as? KClass<*>)?.qualifiedName
            } ?: emptyList()
            (propertyTypeNames + constructorTypeNames).forEach { qualifiedName ->
                assertFalse(
                    excludedQualifiedNames.contains(qualifiedName),
                    "${type.simpleName} must not reference '$qualifiedName'",
                )
            }
        }
    }

    // ---- 1. Exact bytes written to the temp representation ----

    @Test
    fun `exact source bytes are written to the temp file referenced in the request JSON`() = runTest {
        val originalContent = byteArrayOf(9, 8, 7, 6, 5, 4, 3, 2, 1, 0, -1, -128, 127)
        var writtenBytes: ByteArray? = null
        val invoker = FakeDoclingSubprocessInvoker { requestJson, _ ->
            // Must be read here, inside the invocation itself -- by the time recognise()
            // returns, the adapter's own finally block has already deleted the temp file
            // (Phase 4/16's own unconditional cleanup discipline).
            val path = extractSourceFilePath(requestJson)
            writtenBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path))
            success()
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        adapter.recognise(sampleRequest(content = originalContent))

        assertTrue(originalContent.contentEquals(writtenBytes), "the temp file must contain exactly the supplied source bytes")
    }

    // ---- 2, 3, 4. Temp cleanup on success / failure / timeout ----

    @Test
    fun `temp source file is deleted after a successful invocation`() = runTest {
        lateinit var capturedPath: String
        val invoker = FakeDoclingSubprocessInvoker { requestJson, _ ->
            capturedPath = extractSourceFilePath(requestJson)
            success()
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        adapter.recognise(sampleRequest())

        assertFalse(java.nio.file.Files.exists(java.nio.file.Paths.get(capturedPath)), "the temp file must be deleted after success")
    }

    @Test
    fun `temp source file is deleted after a simulated bridge failure`() = runTest {
        lateinit var capturedPath: String
        val invoker = FakeDoclingSubprocessInvoker { requestJson, _ ->
            capturedPath = extractSourceFilePath(requestJson)
            DoclingSubprocessInvocationResult(exitCode = 1, stdout = "", stderr = "boom", timedOut = false)
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        assertFailsWith<IllegalStateException> { adapter.recognise(sampleRequest()) }

        assertFalse(java.nio.file.Files.exists(java.nio.file.Paths.get(capturedPath)), "the temp file must be deleted even after a thrown failure")
    }

    @Test
    fun `temp source file is deleted after a simulated timeout`() = runTest {
        lateinit var capturedPath: String
        val invoker = FakeDoclingSubprocessInvoker { requestJson, _ ->
            capturedPath = extractSourceFilePath(requestJson)
            DoclingSubprocessInvocationResult(exitCode = -1, stdout = "", stderr = "", timedOut = true)
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val outcome = adapter.recognise(sampleRequest())

        assertTrue(outcome is OcrRecognitionOutcome.ProcessingOrDependencyFailure)
        assertFalse(java.nio.file.Files.exists(java.nio.file.Paths.get(capturedPath)), "the temp file must be deleted after a timeout")
    }

    // ---- 5. Source-size rejection ----

    @Test
    fun `oversized source content is rejected defensively without invoking the subprocess`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ -> success() }
        val adapter = DoclingOcrProviderAdapter(configuration(maxSourceBytes = 10), invoker)

        val outcome = adapter.recognise(sampleRequest(content = ByteArray(11)))

        assertTrue(outcome is OcrRecognitionOutcome.ProcessingOrDependencyFailure)
        assertEquals(0, invoker.invocationCount, "an oversized source must never reach the subprocess boundary")
    }

    // ---- 6. Image dimension rejection ----

    @Test
    fun `declared image dimensions exceeding the configured bound are rejected before the subprocess is invoked`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ -> success() }
        val adapter = DoclingOcrProviderAdapter(configuration(maxImageWidthPx = 5_000, maxImageHeightPx = 5_000), invoker)
        val oversizedPng = buildMinimalPngBytes(width = 20_000, height = 20_000)

        val outcome = adapter.recognise(sampleRequest(content = oversizedPng, mediaType = "image/png"))

        assertTrue(outcome is OcrRecognitionOutcome.ProcessingOrDependencyFailure)
        assertTrue((outcome as OcrRecognitionOutcome.ProcessingOrDependencyFailure).reason.contains("20000x20000"))
        assertEquals(0, invoker.invocationCount, "a rejected image must never reach the subprocess boundary")
    }

    @Test
    fun `declared image dimensions within the configured bound pass the preflight check`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ -> success() }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)
        val smallPng = buildMinimalPngBytes(width = 100, height = 100)

        val outcome = adapter.recognise(sampleRequest(content = smallPng, mediaType = "image/png"))

        assertTrue(outcome is OcrRecognitionOutcome.Recognised)
        assertEquals(1, invoker.invocationCount)
    }

    // ---- 7. Total-pixel rejection (isolated from the width/height bound) ----

    @Test
    fun `declared total pixel count exceeding the configured bound is rejected even when width and height individually satisfy their own bounds`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ -> success() }
        val adapter = DoclingOcrProviderAdapter(
            configuration(maxImageWidthPx = 50_000, maxImageHeightPx = 50_000, maxImagePixels = 100_000_000L),
            invoker,
        )
        // 40,000 x 5,000 = 200,000,000 pixels: exceeds the 100MP bound while each individual
        // dimension (40,000 / 5,000) remains within the 50,000 width/height bound configured above.
        val extremeAspectPng = buildMinimalPngBytes(width = 40_000, height = 5_000)

        val outcome = adapter.recognise(sampleRequest(content = extremeAspectPng, mediaType = "image/png"))

        assertTrue(outcome is OcrRecognitionOutcome.ProcessingOrDependencyFailure)
        assertTrue((outcome as OcrRecognitionOutcome.ProcessingOrDependencyFailure).reason.contains("pixel count"))
        assertEquals(0, invoker.invocationCount)
    }

    @Test
    fun `malformed image metadata fails closed as unsupported input, never silently passed through`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ -> success() }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val outcome = adapter.recognise(sampleRequest(content = byteArrayOf(1, 2, 3), mediaType = "image/png"))

        assertTrue(outcome is OcrRecognitionOutcome.UnsupportedOrInaccessibleInput)
        assertEquals(0, invoker.invocationCount)
    }

    // Adversarial (Phase 7's own "negative/impossible values" challenge): a PNG declaring its own
    // width/height as the 32-bit unsigned value 0xFFFFFFFF. Whether the JDK's own PNG reader
    // reconstructs this as the signed Java int -1, or throws while parsing it, is deliberately
    // not pinned down here -- both are legitimate JDK behaviours across versions. What must hold
    // regardless is the property this test actually proves: such a header can never silently
    // pass the bound check (which it would, if a resulting negative width/height were compared
    // naively: -1 is never ">" a positive maximum) and can never reach the subprocess boundary.
    @Test
    fun `an adversarial 0xFFFFFFFF declared dimension never bypasses the bound check via signed-integer overflow`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ -> success() }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)
        val adversarialPng = buildMinimalPngBytes(width = -1, height = -1) // raw bit pattern 0xFFFFFFFF

        val outcome = adapter.recognise(sampleRequest(content = adversarialPng, mediaType = "image/png"))

        assertTrue(
            outcome is OcrRecognitionOutcome.ProcessingOrDependencyFailure || outcome is OcrRecognitionOutcome.UnsupportedOrInaccessibleInput,
            "an adversarial declared dimension must fail closed as one of the two honest, disclosed outcomes -- got: $outcome",
        )
        assertEquals(0, invoker.invocationCount, "an adversarial declared dimension must never reach the subprocess boundary")
    }

    @Test
    fun `a zero-width declared dimension is rejected, not silently treated as within bounds`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ -> success() }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)
        val zeroWidthPng = buildMinimalPngBytes(width = 0, height = 100)

        val outcome = adapter.recognise(sampleRequest(content = zeroWidthPng, mediaType = "image/png"))

        assertTrue(outcome is OcrRecognitionOutcome.UnsupportedOrInaccessibleInput)
        assertEquals(0, invoker.invocationCount)
    }

    // ---- 8, 9. stdout / stderr cap ----

    @Test
    fun `a stdout cap breach is mapped to an honest failure, never a silently truncated success`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            DoclingSubprocessInvocationResult(exitCode = 0, stdout = "truncated", stderr = "", timedOut = false, stdoutCapExceeded = true)
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val outcome = adapter.recognise(sampleRequest())

        assertTrue(outcome is OcrRecognitionOutcome.ProcessingOrDependencyFailure)
        assertTrue((outcome as OcrRecognitionOutcome.ProcessingOrDependencyFailure).reason.contains("stdout"))
    }

    @Test
    fun `a stderr cap breach is mapped to an honest failure`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            DoclingSubprocessInvocationResult(exitCode = 1, stdout = "", stderr = "truncated", timedOut = false, stderrCapExceeded = true)
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val outcome = adapter.recognise(sampleRequest())

        assertTrue(outcome is OcrRecognitionOutcome.ProcessingOrDependencyFailure)
        assertTrue((outcome as OcrRecognitionOutcome.ProcessingOrDependencyFailure).reason.contains("stderr"))
    }

    @Test
    fun `recognised text exceeding the configured output bound is rejected, never returned as Recognised`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ -> success(recognisedText = "x".repeat(100)) }
        val adapter = DoclingOcrProviderAdapter(configuration(maxOutputTextBytes = 50), invoker)

        val outcome = adapter.recognise(sampleRequest())

        assertTrue(outcome is OcrRecognitionOutcome.ProcessingOrDependencyFailure)
    }

    // ---- 10. Timeout ----

    @Test
    fun `timeout is mapped to ProcessingOrDependencyFailure, never Recognised`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            DoclingSubprocessInvocationResult(exitCode = -1, stdout = "", stderr = "", timedOut = true)
        }
        val adapter = DoclingOcrProviderAdapter(configuration(timeoutMillis = 5_000), invoker)

        val outcome = adapter.recognise(sampleRequest())

        assertTrue(outcome is OcrRecognitionOutcome.ProcessingOrDependencyFailure)
        assertTrue((outcome as OcrRecognitionOutcome.ProcessingOrDependencyFailure).reason.contains("timed out"))
    }

    // ---- 11. Concurrency-one ----

    @Test
    fun `DoclingOcrProviderAdapter declares exactly one Mutex-typed property`() {
        val mutexProperties = DoclingOcrProviderAdapter::class.declaredMemberProperties
            .filter { it.returnType.classifier == kotlinx.coroutines.sync.Mutex::class }
        assertEquals(1, mutexProperties.size, "exactly one single-permit concurrency primitive must guard the entire invocation")
    }

    @Test
    fun `two concurrent invocations never overlap inside the critical section`() {
        val activeCount = AtomicInteger(0)
        val maxObservedConcurrency = AtomicInteger(0)
        val invoker = DoclingSubprocessInvoker { _, _ ->
            val current = activeCount.incrementAndGet()
            maxObservedConcurrency.updateAndGet { existing -> maxOf(existing, current) }
            Thread.sleep(50)
            activeCount.decrementAndGet()
            success()
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        runBlocking {
            val first = async(Dispatchers.IO) { adapter.recognise(sampleRequest()) }
            val second = async(Dispatchers.IO) { adapter.recognise(sampleRequest()) }
            first.await()
            second.await()
        }

        assertEquals(1, maxObservedConcurrency.get(), "the mutex must serialise invocations -- concurrency must never exceed 1")
    }

    // ---- 12. Command is argument-vector based -- see ProcessBuilderDoclingSubprocessInvokerTest ----

    // ---- 13. Offline flags/config passed -- see ProcessBuilderDoclingSubprocessInvokerTest ----

    // ---- 14. Missing runtime/assets failure ----

    @Test
    fun `missing model or cache assets are mapped to a distinct ProcessingOrDependencyFailure`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            DoclingSubprocessInvocationResult(exitCode = 2, stdout = "", stderr = "model cache not found", timedOut = false)
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val outcome = adapter.recognise(sampleRequest())

        assertTrue(outcome is OcrRecognitionOutcome.ProcessingOrDependencyFailure)
        assertTrue((outcome as OcrRecognitionOutcome.ProcessingOrDependencyFailure).reason.contains("model cache not found"))
    }

    @Test
    fun `an unsupported or corrupt source reported by the bridge maps to UnsupportedOrInaccessibleInput`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            DoclingSubprocessInvocationResult(exitCode = 3, stdout = "", stderr = "could not decode source", timedOut = false)
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val outcome = adapter.recognise(sampleRequest())

        assertTrue(outcome is OcrRecognitionOutcome.UnsupportedOrInaccessibleInput)
    }

    @Test
    fun `a bridge-reported resource-limit breach maps to ProcessingOrDependencyFailure`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            DoclingSubprocessInvocationResult(exitCode = 4, stdout = "", stderr = "page count exceeds 200", timedOut = false)
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val outcome = adapter.recognise(sampleRequest())

        assertTrue(outcome is OcrRecognitionOutcome.ProcessingOrDependencyFailure)
        assertTrue((outcome as OcrRecognitionOutcome.ProcessingOrDependencyFailure).reason.contains("page count exceeds 200"))
    }

    @Test
    fun `subprocess start failure is mapped to ProcessingOrDependencyFailure`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            DoclingSubprocessInvocationResult(exitCode = -1, stdout = "", stderr = "Docling OCR bridge process failed to start: No such file or directory", timedOut = false)
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val outcome = adapter.recognise(sampleRequest())

        assertTrue(outcome is OcrRecognitionOutcome.ProcessingOrDependencyFailure)
        assertTrue((outcome as OcrRecognitionOutcome.ProcessingOrDependencyFailure).reason.contains("failed to start"))
    }

    // ---- 15. Malformed output rejected ----

    @Test
    fun `malformed JSON response fails loudly, propagating unchanged`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            DoclingSubprocessInvocationResult(exitCode = 0, stdout = "not json at all", stderr = "", timedOut = false)
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        assertFailsWith<IllegalArgumentException> { adapter.recognise(sampleRequest()) }
    }

    @Test
    fun `missing required field fails loudly`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            DoclingSubprocessInvocationResult(exitCode = 0, stdout = """{"status":"recognised","fidelity":"VERBATIM"}""", stderr = "", timedOut = false)
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val error = assertFailsWith<IllegalArgumentException> { adapter.recognise(sampleRequest()) }
        assertTrue(error.message!!.contains("recognisedText"))
    }

    @Test
    fun `an unexpected extra field fails loudly`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            DoclingSubprocessInvocationResult(
                exitCode = 0,
                stdout = """{"status":"recognised","recognisedText":"hi","fidelity":"VERBATIM","boundingBox":"leaked"}""",
                stderr = "",
                timedOut = false,
            )
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val error = assertFailsWith<IllegalArgumentException> { adapter.recognise(sampleRequest()) }
        assertTrue(error.message!!.contains("unexpected"))
    }

    // Load-bearing (Phase 10's own "Unicode escapes" adversarial item): Python's own `json.dumps`
    // defaults to escaping every non-ASCII character as `\uXXXX`, so any real bridge script
    // emitting non-ASCII recognised text -- accented characters, non-Latin scripts -- would rely
    // on this decoding correctly. A prior drafting of this parser silently dropped the backslash
    // of any `\u`-style escape it did not specifically recognise, corrupting such text rather
    // than decoding or rejecting it; this test proves the corrected behaviour.
    @Test
    fun `Unicode escape sequences in recognised text are decoded correctly, not corrupted`() = runTest {
        // The JSON payload below spells "Café 中文" entirely via \uXXXX escapes -- é (é),
        // 中 (中), 文 (文) -- exactly as Python's json.dumps(ensure_ascii=True) (its own
        // default) would encode any non-ASCII recognised text. Deliberately not embedded as
        // literal UTF-8 source characters: this string is inside a raw, triple-quoted Kotlin
        // literal, so literal characters would bypass the very \u-decoding path under test,
        // making the assertion below pass even with the pre-fix, corrupting parser.
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            DoclingSubprocessInvocationResult(
                exitCode = 0,
                stdout = """{"status":"recognised","recognisedText":"Caf\u00e9 \u4e2d\u6587","fidelity":"VERBATIM"}""",
                stderr = "",
                timedOut = false,
            )
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val outcome = adapter.recognise(sampleRequest())

        assertTrue(outcome is OcrRecognitionOutcome.Recognised)
        assertEquals("Café 中文", (outcome as OcrRecognitionOutcome.Recognised).result.recognisedText)
    }

    @Test
    fun `an unrecognised escape sequence fails loudly rather than silently dropping the backslash`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            DoclingSubprocessInvocationResult(
                exitCode = 0,
                stdout = """{"status":"recognised","recognisedText":"bad \q escape","fidelity":"VERBATIM"}""",
                stderr = "",
                timedOut = false,
            )
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val error = assertFailsWith<IllegalArgumentException> { adapter.recognise(sampleRequest()) }
        assertTrue(error.message!!.contains("unrecognised escape"))
    }

    @Test
    fun `a duplicate top-level field in the bridge response fails loudly`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            DoclingSubprocessInvocationResult(
                exitCode = 0,
                stdout = """{"status":"recognised","status":"partial","recognisedText":"hi","fidelity":"VERBATIM"}""",
                stderr = "",
                timedOut = false,
            )
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val error = assertFailsWith<IllegalArgumentException> { adapter.recognise(sampleRequest()) }
        assertTrue(error.message!!.contains("duplicate"))
    }

    @Test
    fun `unknown status value fails loudly`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            DoclingSubprocessInvocationResult(exitCode = 0, stdout = """{"status":"bogus"}""", stderr = "", timedOut = false)
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val error = assertFailsWith<IllegalArgumentException> { adapter.recognise(sampleRequest()) }
        assertTrue(error.message!!.contains("unknown status"))
    }

    // ---- 16. Provider crash mapped truthfully ----

    @Test
    fun `an unclassified non-zero exit code propagates as an uncaught exception, never a fabricated result`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            DoclingSubprocessInvocationResult(exitCode = 1, stdout = "", stderr = "segmentation fault", timedOut = false)
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val error = assertFailsWith<IllegalStateException> { adapter.recognise(sampleRequest()) }
        assertTrue(error.message!!.contains("segmentation fault"))
    }

    // ---- 17. Provenance fields preserved ----

    @Test
    fun `provenance fields -- mechanismIdentity, mechanismVersion, configurationProfile -- are carried through`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            success(extraFields = ""","mechanismVersion":"2.5.0"""")
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val outcome = adapter.recognise(sampleRequest())

        assertTrue(outcome is OcrRecognitionOutcome.Recognised)
        val identity = (outcome as OcrRecognitionOutcome.Recognised).result.identity
        assertEquals("docling", identity.mechanismIdentity)
        assertEquals("2.5.0", identity.mechanismVersion)
        assertEquals("docling-bridge-v1", identity.configurationProfile)
    }

    @Test
    fun `mechanismVersion remains genuinely absent, never fabricated, when the bridge does not report one`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ -> success() }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val outcome = adapter.recognise(sampleRequest())

        assertTrue(outcome is OcrRecognitionOutcome.Recognised)
        assertNull((outcome as OcrRecognitionOutcome.Recognised).result.identity.mechanismVersion)
    }

    @Test
    fun `modelIdentity and modelVersion map directly onto OcrRecognitionIdentity -- never concatenated into configurationProfile`() = runTest {
        val digest = "6f327246b50388f3c176ae304bd95767ea6dc0c9ae92153ef8cbe210b3c14884"
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            success(
                extraFields = ""","modelIdentity":"rapidocr-onnxruntime:PP-OCRv6_rec_small","modelVersion":"sha256:$digest"""",
            )
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val outcome = adapter.recognise(sampleRequest())

        assertTrue(outcome is OcrRecognitionOutcome.Recognised)
        val identity = (outcome as OcrRecognitionOutcome.Recognised).result.identity
        assertEquals("rapidocr-onnxruntime:PP-OCRv6_rec_small", identity.modelIdentity)
        assertEquals("sha256:$digest", identity.modelVersion)
        assertEquals("docling-bridge-v1", identity.configurationProfile, "configurationProfile must never carry a ';model=' suffix")
    }

    @Test
    fun `modelIdentity and modelVersion are genuinely absent, never fabricated, when the bridge reports neither`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ -> success() }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val outcome = adapter.recognise(sampleRequest())

        assertTrue(outcome is OcrRecognitionOutcome.Recognised)
        val identity = (outcome as OcrRecognitionOutcome.Recognised).result.identity
        assertNull(identity.modelIdentity)
        assertNull(identity.modelVersion)
    }

    @Test
    fun `a bridge response reporting modelIdentity without modelVersion is rejected, never a fabricated one-field pairing`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            success(extraFields = ""","modelIdentity":"rapidocr-onnxruntime:PP-OCRv6_rec_small"""")
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        assertFailsWith<IllegalArgumentException> { adapter.recognise(sampleRequest()) }
    }

    @Test
    fun `a bridge response reporting modelVersion without modelIdentity is rejected, never a fabricated one-field pairing`() = runTest {
        val digest = "6f327246b50388f3c176ae304bd95767ea6dc0c9ae92153ef8cbe210b3c14884"
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            success(extraFields = ""","modelVersion":"sha256:$digest"""")
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        assertFailsWith<IllegalArgumentException> { adapter.recognise(sampleRequest()) }
    }

    @Test
    fun `the malformed pairing diagnostic never leaks recognisedText, raw bridge JSON, or a model path -- even against sensitive OCR-shaped content`() = runTest {
        // Codex correction pass, item 6: the diagnostic must be a bounded,
        // static description of which field was present/absent, never an
        // echo of the raw bridge response -- proven here with deliberately
        // sensitive-looking content (a fake SSN-shaped string and a fake
        // filesystem path) standing in for real OCR-recognised text or a
        // model artifact path.
        val sensitiveText = "SSN: 123-45-6789, patient record at /home/steve/private/patient-notes.pdf"
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            success(
                recognisedText = sensitiveText,
                extraFields = ""","modelIdentity":"rapidocr-onnxruntime:PP-OCRv6_rec_small"""",
            )
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val error = assertFailsWith<IllegalArgumentException> { adapter.recognise(sampleRequest()) }
        val message = error.message.orEmpty()
        assertFalse(message.contains("123-45-6789"), "diagnostic must never contain recognised text content: $message")
        assertFalse(message.contains("/home/steve"), "diagnostic must never contain a filesystem path: $message")
        assertFalse(message.contains(sensitiveText), "diagnostic must never echo the raw recognisedText field: $message")
        assertTrue(message.contains("modelIdentity"), "diagnostic should still name the fields involved: $message")
        assertTrue(message.contains("modelVersion"), "diagnostic should still name the fields involved: $message")
    }

    // ---- 18. Source bytes unchanged ----

    @Test
    fun `source bytes reaching the request are byte-identical after the call completes -- no mutation`() = runTest {
        val originalContent = byteArrayOf(1, 2, 3, 4, 5)
        val expectedCopy = originalContent.copyOf()
        val invoker = FakeDoclingSubprocessInvoker { _, _ -> success() }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        adapter.recognise(sampleRequest(content = originalContent))

        assertTrue(originalContent.contentEquals(expectedCopy), "the adapter must never mutate the supplied source content array")
    }

    // ---- No-network structural proof ----

    @Test
    fun `no networking-capable type appears anywhere in DoclingOcrProviderAdapter's own declared shape`() {
        val disallowedFragments = listOf("Socket", "URLConnection", "HttpClient", "HttpURLConnection", "DatagramSocket")
        val types: List<KClass<*>> = listOf(DoclingOcrProviderAdapter::class, DoclingOcrProviderAdapterConfiguration::class, DoclingSubprocessInvoker::class)
        types.forEach { type ->
            val propertyTypeNames = type.declaredMemberProperties.map { it.returnType.toString() }
            val constructorTypeNames = type.primaryConstructor?.parameters?.map { it.type.toString() } ?: emptyList()
            (propertyTypeNames + constructorTypeNames).forEach { typeName ->
                disallowedFragments.forEach { fragment ->
                    assertFalse(typeName.contains(fragment), "${type.simpleName} must not reference '$fragment'")
                }
            }
        }
    }

    // ---- No-recognisable-content / partial mapping ----

    @Test
    fun `no recognisable content maps to NoRecognisableContent, never a fabricated success`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            DoclingSubprocessInvocationResult(exitCode = 0, stdout = """{"status":"no_recognisable_content","reason":"blank page"}""", stderr = "", timedOut = false)
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val outcome = adapter.recognise(sampleRequest())

        assertTrue(outcome is OcrRecognitionOutcome.NoRecognisableContent)
        assertEquals("blank page", (outcome as OcrRecognitionOutcome.NoRecognisableContent).reason)
    }

    @Test
    fun `a partial recognition preserves the actual partial text, never discarded`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            DoclingSubprocessInvocationResult(
                exitCode = 0,
                stdout = """{"status":"partial","recognisedText":"partial text","fidelity":"NORMALISED","reason":"page 3 unreadable"}""",
                stderr = "",
                timedOut = false,
            )
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        val outcome = adapter.recognise(sampleRequest())

        assertTrue(outcome is OcrRecognitionOutcome.PartialOrDegradedOutput)
        val partial = outcome as OcrRecognitionOutcome.PartialOrDegradedOutput
        assertEquals("partial text", partial.partialResult.recognisedText)
        assertEquals("page 3 unreadable", partial.reason)
    }

    @Test
    fun `a full success is never accompanied by an unexpected reason field`() = runTest {
        val invoker = FakeDoclingSubprocessInvoker { _, _ ->
            DoclingSubprocessInvocationResult(
                exitCode = 0,
                stdout = """{"status":"recognised","recognisedText":"hi","fidelity":"VERBATIM","reason":"should not be here"}""",
                stderr = "",
                timedOut = false,
            )
        }
        val adapter = DoclingOcrProviderAdapter(configuration(), invoker)

        assertFailsWith<IllegalArgumentException> { adapter.recognise(sampleRequest()) }
    }
}
