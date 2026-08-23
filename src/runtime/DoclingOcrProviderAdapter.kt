package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import parker.core.interfaces.OcrProviderAdapter
import parker.core.interfaces.OcrRecognitionIdentity
import parker.core.interfaces.OcrRecognitionOutcome
import parker.core.interfaces.OcrRecognitionRequest
import parker.core.interfaces.OcrRecognitionResult
import parker.core.interfaces.OcrRecognitionSegment
import parker.core.interfaces.TranscriptionFidelity

/**
 * OCR Mechanism -- Docling Concrete Adapter Implementation Unit. Governed
 * in full by
 * `docs/architecture/OCR_MECHANISM_DOCLING_CONCRETE_ADAPTER_IMPLEMENTATION_PLAN.md`
 * ("the Adapter Plan", adopted `13d21cf`) Sections 4-26, and by
 * `docs/architecture/OCR_MECHANISM_DOCLING_PROVIDER_AUTHORIZATION_SCOPE_LOCK.md`
 * ("the Docling Authorization", adopted `c5b7aad`) Sections 5-6.
 *
 * ## What this file implements
 *
 * [DoclingOcrProviderAdapter] -- the first, and sole, concrete
 * [OcrProviderAdapter] implementation this repository contains, confined
 * exactly as `TikaEvidenceExtractor` is (Docling Authorization Section 5):
 * this file, and this file alone, may reference Docling-specific process
 * invocation or configuration. Structurally mirrors
 * [QmdRelevanceMechanism]'s own already-accepted shape field-for-field
 * (Adapter Plan Section 2) -- an immutable [DoclingOcrProviderAdapterConfiguration],
 * and [DoclingSubprocessInvoker] as "the entire subprocess-call seam,"
 * swappable between [ProcessBuilderDoclingSubprocessInvoker] (production)
 * and a hand-written, in-memory test fake.
 *
 * ## Process/runtime model (Adapter Plan Section 6)
 *
 * One local, one-shot Python bridge subprocess per [recognise] call --
 * never in-process, never a persistent service. [invocationMutex] (a
 * single-permit [Mutex]) is held around the *entire* invocation -- temp-file
 * write through subprocess completion through cleanup (Adapter Plan Section
 * 15) -- so the 15-minute wall-clock timeout, enforced entirely inside
 * [invoker], effectively begins only once a caller has actually acquired
 * this permit, never merely attempted to.
 *
 * ## No-network enforcement (Adapter Plan Section 8; this task's own Phase 6)
 *
 * This class contains no networking-capable dependency of any kind --
 * [applyDoclingOfflineEnvironment] sets Docling's own offline/local-only
 * resolution flags on the subprocess environment before it ever starts, and
 * a missing pre-provisioned model/cache asset must fail closed (mapped from
 * the bridge script's own distinguished exit code, below) rather than ever
 * reaching a code path capable of a live download. **This adapter does not,
 * and cannot, prove that no network request is ever attempted by Docling's
 * own Python runtime** -- that structural guarantee, if pursued further,
 * belongs to a separate, later provisioning/deployment-tier control
 * (Adapter Plan Section 8), never claimed as already sufficient here.
 *
 * ## Resource enforcement (Adapter Plan Section 13)
 *
 * The 64 MiB source-byte bound is primarily the composition/coordinator
 * tier's own responsibility (not yet built, Unit 12 composition remains
 * separately governed); [recognise] re-checks it defensively, as a second,
 * independent layer, never the sole one. The 10,000x10,000px/100MP image
 * bound is, per the Adapter Plan's own Section 13, primarily bridge-script
 * (Python) tier work for full, two-stage decompression-bomb defence; this
 * adapter additionally performs its own **Stage-0** defensive check for
 * image-prefixed media types, using only [ImageIO]'s own header-level
 * `getWidth`/`getHeight` (never a full raster decode) -- a genuinely
 * earlier, JVM-side layer, strengthening rather than relaxing anything the
 * bridge script itself must still do. PDF page-count and post-decode pixel
 * verification remain bridge-script-tier work this adapter does not, and
 * structurally cannot (no PDF-parsing capability exists in this dependency
 * graph), duplicate. The 20 MiB output bound is enforced here, on the
 * parsed `recognisedText`, before any [OcrRecognitionOutcome.Recognised] is
 * ever constructed. stdout/stderr caps are enforced by [ProcessBuilderDoclingSubprocessInvoker]
 * itself, via [BoundedStreamDrain] -- a genuinely new consideration beyond
 * the QMD precedent (Adapter Plan Section 18).
 *
 * **Disclosed limitation, not a defect:** the Stage-0 preflight above can
 * only inspect a declared image format the JVM's own bundled [ImageIO]
 * readers recognise (PNG/JPEG/GIF/BMP/WBMP, by default). An image-prefixed
 * media type Docling itself could genuinely OCR, but that ships with no
 * corresponding bundled Java reader, is rejected here as
 * [parker.core.interfaces.OcrRecognitionOutcome.UnsupportedOrInaccessibleInput]
 * -- fail-closed, never a crash, but narrower than Docling's own actual
 * format coverage might be. Broadening this would require a new
 * image-decoding dependency this task's own governing instruction
 * forbids adding; left as a named, future consideration, not solved here.
 *
 * ## Bridge response protocol -- illustrative, not frozen
 *
 * The exact JSON shape [parseBridgeResponse] accepts, and the exact
 * non-zero exit-code convention [interpret] switches on, are this file's
 * own illustrative bridge-protocol design -- `tools/docling-ocr-bridge.py`
 * itself is **not** written by this unit (out of scope: see this task's
 * own governing instruction not to add Python code or install anything
 * Docling-related). A future unit writing that real script must honour
 * this protocol, or this file must be revised to match whatever protocol
 * that script actually implements -- whichever comes first, that
 * reconciliation is future, separate work, not performed here.
 */
class DoclingOcrProviderAdapter(
    private val configuration: DoclingOcrProviderAdapterConfiguration,
    private val invoker: DoclingSubprocessInvoker,
) : OcrProviderAdapter {

    private val invocationMutex = Mutex()

    override suspend fun recognise(request: OcrRecognitionRequest): OcrRecognitionOutcome {
        // Cheap, purely local rejections -- never touch the subprocess boundary, the
        // temporary-file boundary, or the single concurrency permit, so they are
        // deliberately checked *before* acquiring invocationMutex (Adapter Plan Section 15
        // governs the mutex around the genuine invocation; a rejection that never invokes
        // Docling at all is not part of "the invocation" that guarantee protects).
        if (request.content.size.toLong() > configuration.maxSourceBytes) {
            return OcrRecognitionOutcome.ProcessingOrDependencyFailure(
                "source content of ${request.content.size} bytes exceeds the configured maximum of " +
                    "${configuration.maxSourceBytes} bytes",
            )
        }

        if (request.mediaType.startsWith("image/", ignoreCase = true)) {
            when (val preflight = imageDimensionPreflight(request.content)) {
                is ImagePreflightResult.Rejected ->
                    return OcrRecognitionOutcome.ProcessingOrDependencyFailure(preflight.reason)
                is ImagePreflightResult.Unreadable ->
                    return OcrRecognitionOutcome.UnsupportedOrInaccessibleInput(preflight.reason)
                ImagePreflightResult.Ok -> Unit
            }
        }

        return invocationMutex.withLock {
            invokeBridge(request)
        }
    }

    private suspend fun invokeBridge(request: OcrRecognitionRequest): OcrRecognitionOutcome {
        val sourceFile = withContext(Dispatchers.IO) { createSecureSourceTempFile() }
        try {
            withContext(Dispatchers.IO) { Files.write(sourceFile, request.content) }
            val requestJson = buildRequestJson(request, sourceFile)
            val invocation = withContext(Dispatchers.IO) {
                invoker.invoke(requestJson, configuration.timeoutMillis)
            }
            return interpret(invocation)
        } finally {
            // Best-effort, not fail-fast (Adapter Plan Section 17: "Cleanup failure... caught
            // and logged, never masks or replaces the primary result/exception -- mirrors
            // ParkerRuntime.shutdown's own established discipline"). Deliberately swallowed
            // rather than rethrown: if the try block above already returned a value or is
            // propagating a genuine exception (malformed output, process crash), a cleanup
            // failure here must never replace either -- an unguarded delete would let the JVM's
            // ordinary try/finally semantics silently discard the primary outcome in favour of
            // this purely-housekeeping one.
            withContext(Dispatchers.IO) {
                try {
                    Files.deleteIfExists(sourceFile)
                } catch (_: IOException) {
                    // Deliberately swallowed -- see comment above.
                }
            }
        }
    }

    private fun createSecureSourceTempFile(): Path {
        val dir = configuration.doclingTempDir?.let { Paths.get(it) }
        return try {
            val ownerOnly = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))
            if (dir != null) {
                Files.createTempFile(dir, TEMP_SOURCE_FILE_PREFIX, TEMP_SOURCE_FILE_SUFFIX, ownerOnly)
            } else {
                Files.createTempFile(TEMP_SOURCE_FILE_PREFIX, TEMP_SOURCE_FILE_SUFFIX, ownerOnly)
            }
        } catch (unsupported: UnsupportedOperationException) {
            // POSIX permissions unsupported on this platform -- a hardening layer, not a
            // correctness requirement this class can guarantee cross-platform (Adapter Plan
            // Section 16: "owner-only... where supported").
            if (dir != null) {
                Files.createTempFile(dir, TEMP_SOURCE_FILE_PREFIX, TEMP_SOURCE_FILE_SUFFIX)
            } else {
                Files.createTempFile(TEMP_SOURCE_FILE_PREFIX, TEMP_SOURCE_FILE_SUFFIX)
            }
        }
    }

    private fun buildRequestJson(request: OcrRecognitionRequest, sourceFile: Path): String = buildString {
        append('{')
        append(""""protocolVersion":${jsonString(configuration.bridgeProtocolVersion)},""")
        append(""""sourceFilePath":${jsonString(sourceFile.toAbsolutePath().toString())},""")
        append(""""mediaType":${jsonString(request.mediaType)},""")
        append(""""configurationProfile":${jsonString(configuration.configurationProfile)}""")
        if (configuration.modelCacheDir != null) {
            append(""","modelCacheDir":${jsonString(configuration.modelCacheDir)}""")
        }
        append('}')
    }

    private fun imageDimensionPreflight(content: ByteArray): ImagePreflightResult = try {
        ImageIO.createImageInputStream(ByteArrayInputStream(content)).use { iis ->
            if (iis == null) {
                return ImagePreflightResult.Unreadable(
                    "could not open the supplied image content for declared-dimension header inspection",
                )
            }
            val readers = ImageIO.getImageReaders(iis)
            if (!readers.hasNext()) {
                return ImagePreflightResult.Unreadable(
                    "no image reader is available to inspect the supplied content's declared dimensions",
                )
            }
            val reader = readers.next()
            try {
                // seekForwardOnly=true, ignoreMetadata=true -- header-level width/height only;
                // this never triggers a full raster decode (the genuine decompression-bomb
                // defence: reject before, not after, expensive decode work -- Adapter Plan
                // Section 13, "Stage one, before full decompression").
                reader.setInput(iis, true, true)
                val width = reader.getWidth(0)
                val height = reader.getHeight(0)
                val pixels = width.toLong() * height.toLong()
                when {
                    // A malicious or corrupt header can declare a width/height whose true,
                    // unsigned 32-bit value (e.g. 0xFFFFFFFF) overflows Java's signed `int` into
                    // a negative number when a reader parses it via straightforward bit
                    // reconstruction -- silently passing every bound check below (a negative
                    // value is never ">" a positive maximum, and two negatives multiply back to
                    // a small positive product). Explicitly fail closed on any non-positive
                    // declared dimension rather than trust arithmetic that adversarial input can
                    // defeat (Phase 7's own "negative/impossible values" challenge).
                    width <= 0 || height <= 0 ->
                        ImagePreflightResult.Unreadable(
                            "declared image dimensions ${width}x$height are not positive -- malformed or " +
                                "adversarial header, failing closed",
                        )
                    width > configuration.maxImageWidthPx || height > configuration.maxImageHeightPx ->
                        ImagePreflightResult.Rejected(
                            "declared image dimensions ${width}x$height exceed the configured maximum of " +
                                "${configuration.maxImageWidthPx}x${configuration.maxImageHeightPx}",
                        )
                    pixels > configuration.maxImagePixels ->
                        ImagePreflightResult.Rejected(
                            "declared total pixel count $pixels exceeds the configured maximum of " +
                                "${configuration.maxImagePixels}",
                        )
                    else -> ImagePreflightResult.Ok
                }
            } finally {
                reader.dispose()
            }
        }
    } catch (error: IOException) {
        ImagePreflightResult.Unreadable("failed to inspect declared image dimensions: ${error.message}")
    } catch (error: RuntimeException) {
        // Deliberately broad, not narrowed to IllegalArgumentException alone (Phase 7's own
        // "truncated image headers" / "image types with unusual metadata readers" challenge):
        // a truncated or adversarially malformed image can drive a JDK-bundled ImageReader
        // implementation to throw an undocumented, codec-specific RuntimeException (for example
        // an out-of-bounds or parsing-assertion failure) rather than a clean IOException. This
        // preflight check exists to fail closed against untrusted binary input, never to crash
        // the caller -- any failure here degrades to the same honest, disclosed Unreadable
        // outcome. Error (e.g. OutOfMemoryError) is deliberately not caught here.
        ImagePreflightResult.Unreadable("failed to inspect declared image dimensions: ${error.message}")
    }

    private fun interpret(invocation: DoclingSubprocessInvocationResult): OcrRecognitionOutcome {
        if (invocation.timedOut) {
            return OcrRecognitionOutcome.ProcessingOrDependencyFailure(
                "Docling recognition timed out after ${configuration.timeoutMillis}ms",
            )
        }
        if (invocation.stdoutCapExceeded) {
            return OcrRecognitionOutcome.ProcessingOrDependencyFailure(
                "Docling bridge stdout exceeded the configured cap of ${configuration.maxStdoutBytes} bytes -- " +
                    "never treated as a silently truncated success",
            )
        }
        if (invocation.stderrCapExceeded) {
            return OcrRecognitionOutcome.ProcessingOrDependencyFailure(
                "Docling bridge stderr exceeded the configured cap of ${configuration.maxStderrBytes} bytes",
            )
        }

        when (invocation.exitCode) {
            0 -> Unit
            -1 -> return OcrRecognitionOutcome.ProcessingOrDependencyFailure(
                invocation.stderr.ifBlank { "Docling OCR bridge process failed to start or could not be launched" },
            )
            EXIT_CODE_MISSING_ASSETS -> return OcrRecognitionOutcome.ProcessingOrDependencyFailure(
                "Docling bridge reported missing runtime/model assets: " +
                    invocation.stderr.ifBlank { "(no stderr output)" },
            )
            EXIT_CODE_UNSUPPORTED_INPUT -> return OcrRecognitionOutcome.UnsupportedOrInaccessibleInput(
                invocation.stderr.ifBlank { "Docling could not process the supplied content" },
            )
            EXIT_CODE_RESOURCE_LIMIT_BREACH -> return OcrRecognitionOutcome.ProcessingOrDependencyFailure(
                "Docling bridge reported a resource-limit breach: " +
                    invocation.stderr.ifBlank { "(no stderr output)" },
            )
            else -> error(
                "Docling bridge process exited with code ${invocation.exitCode}: " +
                    invocation.stderr.ifBlank { "(no stderr output)" },
            )
        }

        val parsed = parseBridgeResponse(invocation.stdout)
        return when (parsed) {
            is BridgeResponse.NoRecognisableContent -> OcrRecognitionOutcome.NoRecognisableContent(parsed.reason)
            is BridgeResponse.Recognition -> buildRecognitionOutcome(parsed)
        }
    }

    private fun buildRecognitionOutcome(parsed: BridgeResponse.Recognition): OcrRecognitionOutcome {
        val outputBytes = parsed.recognisedText.toByteArray(Charsets.UTF_8).size.toLong()
        if (outputBytes > configuration.maxOutputTextBytes) {
            return OcrRecognitionOutcome.ProcessingOrDependencyFailure(
                "Docling recognised text of $outputBytes bytes exceeds the configured maximum of " +
                    "${configuration.maxOutputTextBytes} bytes",
            )
        }

        val configurationProfile = if (parsed.modelIdentity != null) {
            "${configuration.configurationProfile};model=${parsed.modelIdentity}"
        } else {
            configuration.configurationProfile
        }

        val result = OcrRecognitionResult(
            recognisedText = parsed.recognisedText,
            fidelity = parsed.fidelity,
            identity = OcrRecognitionIdentity(
                mechanismIdentity = DOCLING_MECHANISM_IDENTITY,
                configurationProfile = configurationProfile,
                mechanismVersion = parsed.mechanismVersion,
            ),
            confidence = parsed.confidence,
            recognisedAt = Instant.now(),
            warnings = parsed.warnings,
            segments = parsed.segments,
        )

        return if (parsed.partial) {
            OcrRecognitionOutcome.PartialOrDegradedOutput(
                result,
                parsed.reason ?: "Docling reported a partial recognition",
            )
        } else {
            OcrRecognitionOutcome.Recognised(result)
        }
    }

    private companion object {
        const val TEMP_SOURCE_FILE_PREFIX = "parker-docling-ocr-source-"
        const val TEMP_SOURCE_FILE_SUFFIX = ".tmp"
        const val DOCLING_MECHANISM_IDENTITY = "docling"
    }
}

/**
 * Frozen (per this file's own drafting), disclosed adapter configuration --
 * immutable, mirroring [QmdRelevanceMechanismConfiguration]'s own
 * "no setter, a changed value is a disclosed, governed reconstruction"
 * discipline exactly. Every field name is **illustrative, not frozen**
 * (Adapter Plan Section 4).
 *
 * @param pythonExecutablePath The local Python interpreter to launch --
 *   carries no default, mirroring [QmdRelevanceMechanismConfiguration.nodeExecutablePath]'s
 *   own "a caller must state its own actual local endpoint explicitly"
 *   precedent.
 * @param bridgeScriptPath The local Docling-invoking bridge script to run.
 * @param modelCacheDir The fixed, pre-provisioned, read-only model-cache
 *   directory Docling reads from -- never created, written to, or
 *   populated by this adapter (Adapter Plan Section 9). `null` when not
 *   configured; the bridge script's own pre-flight check then relies on
 *   whatever default location its own deployment convention establishes.
 * @param doclingTempDir An explicit, Parker-controlled temporary-file root
 *   for source-byte temp files. `null` falls back to the JVM default temp
 *   directory (Adapter Plan Section 16).
 * @param timeoutMillis The independent wall-clock ceiling per invocation --
 *   15 minutes by default, the Docling Authorization's own frozen bound
 *   (Section 6 there), never reconciled against any page-count bound.
 * @param maxSourceBytes A defensive, second-layer re-check of the 64 MiB
 *   source-byte bound whose primary enforcement belongs to a future
 *   composition/coordinator tier (Adapter Plan Section 13).
 * @param maxImageWidthPx,[maxImageHeightPx],[maxImagePixels] The frozen
 *   10,000x10,000px/100MP decompression-bomb bound (Docling Authorization
 *   Section 6), enforced here only as a Stage-0, declared-header-only
 *   defensive layer for image-prefixed media types.
 * @param maxOutputTextBytes The frozen 20 MiB recognised-text bound.
 * @param maxStdoutBytes,[maxStderrBytes] Kotlin-side subprocess stream
 *   caps -- a genuinely new consideration beyond the QMD precedent
 *   (Adapter Plan Section 18).
 * @param bridgeProtocolVersion,[configurationProfile] This adapter's own
 *   bridge-protocol and configuration-profile identity, folded into
 *   [parker.core.interfaces.OcrRecognitionIdentity.configurationProfile]
 *   per Adapter Plan Section 12 (`OcrRecognitionIdentity` has no
 *   dedicated adapter-version field of its own).
 */
data class DoclingOcrProviderAdapterConfiguration(
    val pythonExecutablePath: String,
    val bridgeScriptPath: String,
    val modelCacheDir: String? = null,
    val doclingTempDir: String? = null,
    val timeoutMillis: Long = 900_000L,
    val maxSourceBytes: Long = 67_108_864L,
    val maxImageWidthPx: Int = 10_000,
    val maxImageHeightPx: Int = 10_000,
    val maxImagePixels: Long = 100_000_000L,
    val maxOutputTextBytes: Long = 20_971_520L,
    val maxStdoutBytes: Long = 25_165_824L,
    val maxStderrBytes: Long = 1_048_576L,
    val bridgeProtocolVersion: String = "1",
    val configurationProfile: String = "docling-bridge-v1",
) {
    init {
        require(pythonExecutablePath.isNotBlank()) { "DoclingOcrProviderAdapterConfiguration.pythonExecutablePath must not be blank" }
        require(bridgeScriptPath.isNotBlank()) { "DoclingOcrProviderAdapterConfiguration.bridgeScriptPath must not be blank" }
        require(modelCacheDir == null || modelCacheDir.isNotBlank()) {
            "DoclingOcrProviderAdapterConfiguration.modelCacheDir must not be blank when supplied -- use null to leave it unset"
        }
        require(doclingTempDir == null || doclingTempDir.isNotBlank()) {
            "DoclingOcrProviderAdapterConfiguration.doclingTempDir must not be blank when supplied -- use null to leave it unset"
        }
        require(timeoutMillis > 0) { "DoclingOcrProviderAdapterConfiguration.timeoutMillis must be positive" }
        require(maxSourceBytes > 0) { "DoclingOcrProviderAdapterConfiguration.maxSourceBytes must be positive" }
        require(maxImageWidthPx > 0) { "DoclingOcrProviderAdapterConfiguration.maxImageWidthPx must be positive" }
        require(maxImageHeightPx > 0) { "DoclingOcrProviderAdapterConfiguration.maxImageHeightPx must be positive" }
        require(maxImagePixels > 0) { "DoclingOcrProviderAdapterConfiguration.maxImagePixels must be positive" }
        require(maxOutputTextBytes > 0) { "DoclingOcrProviderAdapterConfiguration.maxOutputTextBytes must be positive" }
        require(maxStdoutBytes > 0) { "DoclingOcrProviderAdapterConfiguration.maxStdoutBytes must be positive" }
        require(maxStderrBytes > 0) { "DoclingOcrProviderAdapterConfiguration.maxStderrBytes must be positive" }
        require(bridgeProtocolVersion.isNotBlank()) { "DoclingOcrProviderAdapterConfiguration.bridgeProtocolVersion must not be blank" }
        require(configurationProfile.isNotBlank()) { "DoclingOcrProviderAdapterConfiguration.configurationProfile must not be blank" }
    }
}

/**
 * The raw outcome of one bridge subprocess invocation, before
 * [DoclingOcrProviderAdapter] interprets it -- deliberately dumb, mirroring
 * [QmdSubprocessInvocationResult] exactly, extended with [stdoutCapExceeded]/
 * [stderrCapExceeded] (Adapter Plan Section 18, a genuinely new
 * consideration beyond the QMD precedent).
 */
data class DoclingSubprocessInvocationResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean,
    val stdoutCapExceeded: Boolean = false,
    val stderrCapExceeded: Boolean = false,
)

/**
 * The entire subprocess-call seam -- mirroring [QmdSubprocessInvoker]'s own
 * "the entire subprocess-call seam" precedent exactly.
 * [DoclingOcrProviderAdapter] never knows or cares what is behind this
 * interface: a real [ProcessBuilderDoclingSubprocessInvoker] in production,
 * or a hand-written, in-memory fake in this Unit's own always-run tests
 * that never spawns a real process and never depends on Docling/Python
 * being installed.
 */
fun interface DoclingSubprocessInvoker {
    fun invoke(requestJson: String, timeoutMillis: Long): DoclingSubprocessInvocationResult
}

/**
 * Concrete, production [DoclingSubprocessInvoker]. Writes [requestJson] to
 * a fresh temporary file (deleted in every path, `finally`), launches
 * [DoclingOcrProviderAdapterConfiguration.pythonExecutablePath] against
 * [DoclingOcrProviderAdapterConfiguration.bridgeScriptPath] with that
 * file's path as the sole argument -- `ProcessBuilder(command: List<String>)`,
 * never a shell string -- and drains stdout/stderr concurrently on daemon
 * threads, each bounded by [BoundedStreamDrain], while waiting. Mirrors
 * [ProcessBuilderQmdSubprocessInvoker] field-for-field (Adapter Plan
 * Section 2), extended only where this Unit's own review required it:
 * [applyDoclingOfflineEnvironment] (Section 8), and the stdout/stderr caps
 * (Section 18).
 *
 * Never throws: process-start failure is reported as a distinguished
 * `DoclingSubprocessInvocationResult(exitCode = -1, ...)`, exactly
 * mirroring [ProcessBuilderQmdSubprocessInvoker]'s own identical
 * discipline, so [DoclingOcrProviderAdapter] alone owns deciding how each
 * fault surfaces.
 *
 * **Disclosed, not fixed, limitation (Adapter Plan Section 14, required to
 * remain explicitly disclosed, never silently omitted):** on timeout,
 * [Process.destroyForcibly] terminates only the direct child process (the
 * Python interpreter) -- it does **not** guarantee termination of any
 * further descendant process that interpreter may itself have spawned
 * (Python's own deep-learning stack has a real, known propensity for
 * worker-process forking). This class does not claim full process-tree
 * termination; a hardening technique achieving it (for example, launching
 * via a process-group wrapper and signalling the group) is recommended by
 * the Adapter Plan for a future, separate provisioning/deployment-tier
 * decision, not implemented here.
 */
class ProcessBuilderDoclingSubprocessInvoker(
    private val configuration: DoclingOcrProviderAdapterConfiguration,
) : DoclingSubprocessInvoker {

    override fun invoke(requestJson: String, timeoutMillis: Long): DoclingSubprocessInvocationResult {
        val requestFile = Files.createTempFile("parker-docling-ocr-request-", ".json")
        try {
            Files.writeString(requestFile, requestJson)

            val command = listOf(
                configuration.pythonExecutablePath,
                configuration.bridgeScriptPath,
                requestFile.toAbsolutePath().toString(),
            )

            val process = try {
                val builder = ProcessBuilder(command)
                applyDoclingOfflineEnvironment(builder.environment())
                builder.start()
            } catch (error: IOException) {
                return DoclingSubprocessInvocationResult(
                    exitCode = -1,
                    stdout = "",
                    stderr = "Docling OCR bridge process failed to start: ${error.message}",
                    timedOut = false,
                )
            }

            val stdoutDrain = BoundedStreamDrain(configuration.maxStdoutBytes)
            val stderrDrain = BoundedStreamDrain(configuration.maxStderrBytes)
            val stdoutThread = Thread { stdoutDrain.drain(process.inputStream) }.apply { isDaemon = true }
            val stderrThread = Thread { stderrDrain.drain(process.errorStream) }.apply { isDaemon = true }
            stdoutThread.start()
            stderrThread.start()

            val finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                stdoutThread.join(1_000)
                stderrThread.join(1_000)
                return DoclingSubprocessInvocationResult(
                    exitCode = -1,
                    stdout = stdoutDrain.text(),
                    stderr = stderrDrain.text(),
                    timedOut = true,
                    stdoutCapExceeded = stdoutDrain.capExceeded,
                    stderrCapExceeded = stderrDrain.capExceeded,
                )
            }

            stdoutThread.join(5_000)
            stderrThread.join(5_000)
            return DoclingSubprocessInvocationResult(
                exitCode = process.exitValue(),
                stdout = stdoutDrain.text(),
                stderr = stderrDrain.text(),
                timedOut = false,
                stdoutCapExceeded = stdoutDrain.capExceeded,
                stderrCapExceeded = stderrDrain.capExceeded,
            )
        } finally {
            // Best-effort, not fail-fast (Adapter Plan Section 17) -- this class's own KDoc
            // promises "never throws"; an unguarded delete here would silently break that
            // promise the one time cleanup itself fails, masking whatever distinguished result
            // was already computed above.
            try {
                Files.deleteIfExists(requestFile)
            } catch (_: IOException) {
                // Deliberately swallowed -- see comment above.
            }
        }
    }
}

/**
 * Sets Docling's own offline/local-only model-resolution flags on a
 * subprocess environment -- fixed, Parker-controlled constants only, never
 * populated from evidence content or any evidence-derived value (this
 * task's own Phase 5/Phase 6/Phase 14 instructions). Extracted as its own,
 * separately testable, pure function specifically so this behaviour can be
 * verified without spawning any real process (Adapter Plan Section 8:
 * "before importing or invoking any Docling code capable of triggering
 * model resolution").
 *
 * Illustrative variable names, not verified against any real Docling
 * release (Adapter Plan Section 2's own external-knowledge disclosure) --
 * a future implementer confirms the exact names Docling's own current
 * release actually honours.
 */
internal fun applyDoclingOfflineEnvironment(environment: MutableMap<String, String>) {
    environment["HF_HUB_OFFLINE"] = "1"
    environment["TRANSFORMERS_OFFLINE"] = "1"
}

/**
 * Drains an [InputStream] line-by-line into an in-memory buffer capped at
 * [capBytes] -- once the cap is reached, further lines are still read (to
 * avoid the classic `ProcessBuilder` pipe-buffer deadlock) but no longer
 * appended, and [capExceeded] is set so the caller can treat the result as
 * an honest, disclosed failure rather than a silently truncated success
 * (this task's own Phase 7: "No successful truncation may masquerade as
 * complete output"). `internal`, not `private`, so this Unit's own
 * always-run tests can verify the cap behaviour directly, without spawning
 * a real subprocess (Adapter Plan Section 18's own "genuinely new
 * consideration beyond the QMD precedent").
 */
internal class BoundedStreamDrain(private val capBytes: Long) {
    private val buffer = StringBuilder()
    private var bytesRead = 0L
    var capExceeded: Boolean = false
        private set

    fun drain(stream: InputStream) {
        stream.bufferedReader().forEachLine { line ->
            val lineBytes = line.toByteArray(Charsets.UTF_8).size.toLong() + 1L
            if (!capExceeded) {
                if (bytesRead + lineBytes > capBytes) {
                    capExceeded = true
                } else {
                    buffer.append(line).append('\n')
                }
            }
            bytesRead += lineBytes
        }
    }

    fun text(): String = buffer.toString()
}

// ---------------------------------------------------------------------
// Bridge response protocol -- minimal, hand-rolled, deliberately strict
// JSON handling, mirroring QmdRelevanceMechanism.kt's own established
// convention exactly (no external JSON library dependency, Adapter Plan
// Section 25: "no new JVM/Gradle dependency is anticipated").
// ---------------------------------------------------------------------

private const val EXIT_CODE_MISSING_ASSETS = 2
private const val EXIT_CODE_UNSUPPORTED_INPUT = 3
private const val EXIT_CODE_RESOURCE_LIMIT_BREACH = 4

private sealed class ImagePreflightResult {
    object Ok : ImagePreflightResult()
    data class Rejected(val reason: String) : ImagePreflightResult()
    data class Unreadable(val reason: String) : ImagePreflightResult()
}

private sealed class BridgeResponse {
    data class Recognition(
        val partial: Boolean,
        val recognisedText: String,
        val fidelity: TranscriptionFidelity,
        val confidence: Double?,
        val warnings: List<String>,
        val segments: List<OcrRecognitionSegment>,
        val mechanismVersion: String?,
        val modelIdentity: String?,
        val reason: String?,
    ) : BridgeResponse()

    data class NoRecognisableContent(val reason: String) : BridgeResponse()
}

private fun parseBridgeResponse(rawJson: String): BridgeResponse {
    val trimmed = rawJson.trim()
    require(trimmed.startsWith("{") && trimmed.endsWith("}")) {
        "malformed Docling bridge response: not a JSON object: $rawJson"
    }
    val fields = parseTopLevelFields(trimmed.substring(1, trimmed.length - 1))

    val status = fields["status"]?.let { parseJsonStringValue(it, "status") }
        ?: throw IllegalArgumentException("malformed Docling bridge response: missing required field 'status': $rawJson")

    return when (status) {
        "recognised", "partial" -> parseRecognitionResponse(status, fields, rawJson)
        "no_recognisable_content" -> {
            val allowed = setOf("status", "reason")
            val unexpected = fields.keys - allowed
            require(unexpected.isEmpty()) {
                "malformed Docling bridge response: unexpected field(s) $unexpected for status " +
                    "'no_recognisable_content': $rawJson"
            }
            val reason = fields["reason"]?.let { parseJsonStringValue(it, "reason") }
                ?: throw IllegalArgumentException(
                    "malformed Docling bridge response: missing required field 'reason' for status " +
                        "'no_recognisable_content': $rawJson",
                )
            BridgeResponse.NoRecognisableContent(reason)
        }
        else -> throw IllegalArgumentException("malformed Docling bridge response: unknown status '$status': $rawJson")
    }
}

private fun parseRecognitionResponse(status: String, fields: Map<String, String>, rawJson: String): BridgeResponse.Recognition {
    val isPartial = status == "partial"
    val allowedBase = setOf("status", "recognisedText", "fidelity", "confidence", "warnings", "segments", "mechanismVersion", "modelIdentity")
    val allowed = if (isPartial) allowedBase + "reason" else allowedBase
    val required = if (isPartial) {
        setOf("status", "recognisedText", "fidelity", "reason")
    } else {
        setOf("status", "recognisedText", "fidelity")
    }

    val unexpected = fields.keys - allowed
    require(unexpected.isEmpty()) {
        "malformed Docling bridge response: unexpected field(s) $unexpected for status '$status': $rawJson"
    }
    val missing = required - fields.keys
    require(missing.isEmpty()) {
        "malformed Docling bridge response: missing required field(s) $missing for status '$status': $rawJson"
    }

    val recognisedText = parseJsonStringValue(fields.getValue("recognisedText"), "recognisedText")
    val fidelity = parseFidelity(parseJsonStringValue(fields.getValue("fidelity"), "fidelity"))
    val confidence = fields["confidence"]?.let { parseJsonNullableDoubleValue(it, "confidence") }
    val warnings = fields["warnings"]?.let { parseJsonStringArrayValue(it, "warnings") } ?: emptyList()
    val segments = fields["segments"]?.let { parseSegmentsValue(it) } ?: emptyList()
    val mechanismVersion = fields["mechanismVersion"]?.let { parseJsonNullableStringValue(it, "mechanismVersion") }
    val modelIdentity = fields["modelIdentity"]?.let { parseJsonNullableStringValue(it, "modelIdentity") }
    val reason = fields["reason"]?.let { parseJsonStringValue(it, "reason") }

    return BridgeResponse.Recognition(
        partial = isPartial,
        recognisedText = recognisedText,
        fidelity = fidelity,
        confidence = confidence,
        warnings = warnings,
        segments = segments,
        mechanismVersion = mechanismVersion,
        modelIdentity = modelIdentity,
        reason = reason,
    )
}

private fun parseFidelity(raw: String): TranscriptionFidelity = try {
    TranscriptionFidelity.valueOf(raw)
} catch (error: IllegalArgumentException) {
    throw IllegalArgumentException("malformed Docling bridge response: unknown fidelity value '$raw'")
}

private fun parseSegmentsValue(raw: String): List<OcrRecognitionSegment> {
    require(raw.startsWith("[") && raw.endsWith("]")) {
        "malformed Docling bridge response: field 'segments' is not a JSON array: $raw"
    }
    val body = raw.substring(1, raw.length - 1).trim()
    if (body.isEmpty()) return emptyList()

    return splitTopLevelJsonObjects(body).map { entry ->
        val fields = parseTopLevelFields(entry.substring(1, entry.length - 1))
        val allowed = setOf("text", "fidelity", "pageNumber")
        val unexpected = fields.keys - allowed
        require(unexpected.isEmpty()) {
            "malformed Docling bridge response: segment entry declares unexpected field(s) $unexpected: $entry"
        }
        val text = fields["text"]?.let { parseJsonStringValue(it, "segments[].text") }
            ?: throw IllegalArgumentException("malformed Docling bridge response: segment entry missing required field 'text': $entry")
        val fidelityRaw = fields["fidelity"]?.let { parseJsonStringValue(it, "segments[].fidelity") }
            ?: throw IllegalArgumentException("malformed Docling bridge response: segment entry missing required field 'fidelity': $entry")
        val pageNumber = fields["pageNumber"]?.let { rawPageNumber ->
            if (rawPageNumber == "null") {
                null
            } else {
                rawPageNumber.toIntOrNull()
                    ?: throw IllegalArgumentException("malformed Docling bridge response: segment field 'pageNumber' is not an integer: $rawPageNumber")
            }
        }
        OcrRecognitionSegment(text = text, fidelity = parseFidelity(fidelityRaw), pageNumber = pageNumber)
    }
}

/**
 * Splits `"k1":v1,"k2":v2,...` (the body between an object's own outer
 * `{`/`}`) into a key -> raw-value-substring map, respecting nested
 * `{}`/`[]` and string literals -- mirroring
 * [splitTopLevelJsonObjects]'s own depth-tracking discipline, applied to
 * field splitting rather than array-of-object splitting.
 */
private fun parseTopLevelFields(objectBody: String): Map<String, String> {
    val fieldPattern = Regex("^\"([A-Za-z0-9_]+)\"\\s*:\\s*(.*)$", RegexOption.DOT_MATCHES_ALL)
    val fields = mutableMapOf<String, String>()
    splitTopLevelSegments(objectBody).forEach { part ->
        val trimmedPart = part.trim()
        if (trimmedPart.isEmpty()) return@forEach
        val match = fieldPattern.find(trimmedPart)
            ?: throw IllegalArgumentException("malformed Docling bridge response: not a well-formed \"key\":value pair: $trimmedPart")
        val key = match.groupValues[1]
        require(!fields.containsKey(key)) { "malformed Docling bridge response: duplicate field '$key'" }
        fields[key] = match.groupValues[2].trim()
    }
    return fields
}

private fun splitTopLevelSegments(body: String): List<String> {
    val parts = mutableListOf<String>()
    var depth = 0
    var inString = false
    var start = 0
    var index = 0
    while (index < body.length) {
        val character = body[index]
        if (inString) {
            if (character == '\\') {
                index += 2
                continue
            }
            if (character == '"') inString = false
            index += 1
            continue
        }
        when (character) {
            '"' -> inString = true
            '{', '[' -> depth += 1
            '}', ']' -> depth -= 1
            ',' -> if (depth == 0) {
                parts.add(body.substring(start, index))
                start = index + 1
            }
        }
        index += 1
    }
    val last = body.substring(start)
    if (last.isNotBlank()) parts.add(last)
    return parts
}

private fun splitTopLevelJsonObjects(arrayBody: String): List<String> {
    val objects = mutableListOf<String>()
    var depth = 0
    var start = -1
    var inString = false
    var index = 0
    while (index < arrayBody.length) {
        val character = arrayBody[index]
        if (inString) {
            if (character == '\\') {
                index += 2
                continue
            }
            if (character == '"') inString = false
            index += 1
            continue
        }
        when (character) {
            '"' -> inString = true
            '{' -> {
                if (depth == 0) start = index
                depth += 1
            }
            '}' -> {
                depth -= 1
                if (depth == 0 && start >= 0) {
                    objects.add(arrayBody.substring(start, index + 1))
                    start = -1
                }
            }
        }
        index += 1
    }
    return objects
}

private fun parseJsonStringValue(raw: String, fieldName: String): String {
    require(raw.length >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) {
        "malformed Docling bridge response: field '$fieldName' is not a JSON string: $raw"
    }
    return unescapeJsonString(raw.substring(1, raw.length - 1))
}

private fun parseJsonNullableStringValue(raw: String, fieldName: String): String? =
    if (raw == "null") null else parseJsonStringValue(raw, fieldName)

private fun parseJsonNullableDoubleValue(raw: String, fieldName: String): Double? {
    if (raw == "null") return null
    return raw.toDoubleOrNull()
        ?: throw IllegalArgumentException("malformed Docling bridge response: field '$fieldName' is not a JSON number: $raw")
}

private fun parseJsonStringArrayValue(raw: String, fieldName: String): List<String> {
    require(raw.startsWith("[") && raw.endsWith("]")) {
        "malformed Docling bridge response: field '$fieldName' is not a JSON array: $raw"
    }
    val body = raw.substring(1, raw.length - 1).trim()
    if (body.isEmpty()) return emptyList()
    return splitTopLevelSegments(body).map { parseJsonStringValue(it.trim(), fieldName) }
}

/**
 * Strict, not permissive (Phase 10's own "no malformed field may silently
 * default into success" requirement) -- unlike an earlier drafting of this
 * function, which silently dropped the backslash of any unrecognised
 * escape (corrupting, rather than rejecting, malformed input) and had no
 * `\u` Unicode-escape support at all (genuinely load-bearing: Python's own
 * `json.dumps` defaults to escaping every non-ASCII character as `\uXXXX`,
 * so any real bridge script emitting non-ASCII recognised text -- accented
 * characters, non-Latin scripts -- would have had that text silently
 * corrupted, character-by-character, before this correction). Each
 * `\uXXXX` escape decodes to exactly one UTF-16 `Char`; a genuine
 * surrogate pair (for a character outside the Basic Multilingual Plane)
 * decodes correctly with no special-case handling, because two adjacent
 * UTF-16 surrogate `Char`s appended to a [StringBuilder] already
 * reconstruct the original code point.
 */
private fun unescapeJsonString(value: String): String {
    val builder = StringBuilder(value.length)
    var index = 0
    while (index < value.length) {
        val character = value[index]
        if (character == '\\') {
            require(index + 1 < value.length) {
                "malformed Docling bridge response: string value ends with an unterminated escape sequence"
            }
            when (val next = value[index + 1]) {
                '\\' -> { builder.append('\\'); index += 2 }
                '"' -> { builder.append('"'); index += 2 }
                '/' -> { builder.append('/'); index += 2 }
                'n' -> { builder.append('\n'); index += 2 }
                'r' -> { builder.append('\r'); index += 2 }
                't' -> { builder.append('\t'); index += 2 }
                'b' -> { builder.append('\b'); index += 2 }
                'f' -> { builder.append('\u000C'); index += 2 }
                'u' -> {
                    require(index + 6 <= value.length) {
                        "malformed Docling bridge response: truncated \\u escape sequence"
                    }
                    val hex = value.substring(index + 2, index + 6)
                    val codePoint = hex.toIntOrNull(16)
                        ?: throw IllegalArgumentException(
                            "malformed Docling bridge response: invalid \\u escape sequence: \\u$hex",
                        )
                    builder.append(codePoint.toChar())
                    index += 6
                }
                else -> throw IllegalArgumentException(
                    "malformed Docling bridge response: unrecognised escape sequence '\\$next'",
                )
            }
        } else {
            builder.append(character)
            index += 1
        }
    }
    return builder.toString()
}

private fun jsonString(value: String): String = buildString {
    append('"')
    for (character in value) {
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (character.code < 0x20) {
                append("\\u")
                append(character.code.toString(16).padStart(4, '0'))
            } else {
                append(character)
            }
        }
    }
    append('"')
}
