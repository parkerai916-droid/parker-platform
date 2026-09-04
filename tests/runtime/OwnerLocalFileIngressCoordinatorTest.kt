package parker.core.runtime

import java.io.IOException
import java.nio.channels.ServerSocketChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.net.UnixDomainSocketAddress
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import parker.core.interfaces.DecisionId
import parker.core.interfaces.EvidenceAcceptanceResult
import parker.core.interfaces.EvidenceManifestRetrievalResult
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.OwnerLocalFileIngressOutcome
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ResourceType

/**
 * Document Ingestion, Owner-Authorized Local File Ingress. Behavioural
 * tests for [OwnerLocalFileIngressCoordinator] -- the governed chain
 * permission evaluation -> absolute-path validation -> symlink rejection
 * -> regular-file validation -> size bound -> bounded byte-exact read ->
 * `CandidateEvidenceArtifact` construction -> `EvidenceCustodian.accept`,
 * governed by
 * `docs/architecture/DOCUMENT_INGESTION_OWNER_AUTHORIZED_LOCAL_FILE_INGRESS_SCOPE_LOCK.md`
 * ("the Scope Lock"). Uses only temporary directories/files created for
 * this suite -- never real evidence.
 */
class OwnerLocalFileIngressCoordinatorTest {

    private val ownerPrincipalId = PrincipalId("owner-1")

    private fun approving() = FakePermissionEngine { request -> approvedDecision(request) }
    private fun denying(reason: PermissionDecisionOutcome = PermissionDecisionOutcome.DENIED) =
        FakePermissionEngine { request ->
            PermissionDecision(
                decisionId = DecisionId("decision-${request.requestId.value}"),
                principalId = request.principalId,
                resourceId = request.targetResources.first(),
                action = PermissionAction.WRITE,
                decision = reason,
                level = PermissionLevel.AUTOMATIC,
                timestamp = java.time.Instant.now(),
            )
        }

    private fun approvedDecision(request: ExecutionRequest) = PermissionDecision(
        decisionId = DecisionId("decision-${request.requestId.value}"),
        principalId = request.principalId,
        resourceId = request.targetResources.first(),
        action = PermissionAction.WRITE,
        decision = PermissionDecisionOutcome.APPROVED,
        level = PermissionLevel.AUTOMATIC,
        timestamp = java.time.Instant.now(),
    )

    private fun realCustodian(permissionEngine: FakePermissionEngine = approving()): DefaultEvidenceCustodian {
        val storage = InMemoryEvidenceArtifactStorage()
        val manifestStorage = InMemoryEvidenceSourceManifestStorage()
        return DefaultEvidenceCustodian(storage, permissionEngine, manifestStorage)
    }

    private fun tempFile(content: ByteArray, name: String = "source.bin"): Path {
        val dir = Files.createTempDirectory("local-file-ingress-test")
        val path = dir.resolve(name)
        Files.write(path, content)
        return path
    }

    // --- Authorization (Scope Lock Section 3) ---

    @Test
    fun `denied authorization causes zero filesystem access, before any path validation`() = runTest {
        val permissionEngine = denying()
        val custodian = SpyEvidenceCustodian()
        val coordinator = OwnerLocalFileIngressCoordinator(permissionEngine, custodian)

        // A deliberately nonexistent, garbage absolute path -- if permission were evaluated
        // after path validation, this would surface as PathNotFound/InvalidPath instead.
        val result = coordinator.invoke(ownerPrincipalId, "/definitely/does/not/exist/anywhere.pdf", null)

        val rejected = assertIs<OwnerLocalFileIngressOutcome.AuthorizationRejected>(result)
        assertTrue(rejected.reason.contains("owner-1"))
        assertEquals(0, custodian.acceptCallCount, "Evidence Custodian must never be reached on a denied authorization")
    }

    @Test
    fun `denied authorization is distinct from any path-validation failure`() = runTest {
        val coordinator = OwnerLocalFileIngressCoordinator(denying(), SpyEvidenceCustodian())

        val result = coordinator.invoke(ownerPrincipalId, "relative/not/absolute.pdf", null)

        // Proves ordering: a relative path would normally yield InvalidPath, but denial fires first.
        assertIs<OwnerLocalFileIngressOutcome.AuthorizationRejected>(result)
    }

    // --- Path validation (Scope Lock Section 4) ---

    @Test
    fun `a relative path is rejected as InvalidPath`() = runTest {
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), SpyEvidenceCustodian())

        val result = coordinator.invoke(ownerPrincipalId, "some/relative/path.pdf", null)

        assertEquals(OwnerLocalFileIngressOutcome.InvalidPath, result)
    }

    @Test
    fun `a syntactically invalid path string is rejected as InvalidPath`() = runTest {
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), SpyEvidenceCustodian())

        // NUL is illegal in a POSIX path and causes Path.of to throw InvalidPathException.
        val result = coordinator.invoke(ownerPrincipalId, "/tmp/bad\u0000name.pdf", null)

        assertEquals(OwnerLocalFileIngressOutcome.InvalidPath, result)
    }

    @Test
    fun `a nonexistent absolute path returns PathNotFound`() = runTest {
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), SpyEvidenceCustodian())
        val dir = Files.createTempDirectory("local-file-ingress-test")

        val result = coordinator.invoke(ownerPrincipalId, dir.resolve("never-created.pdf").toString(), null)

        assertEquals(OwnerLocalFileIngressOutcome.PathNotFound, result)
    }

    @Test
    fun `a directory path returns NotARegularFile, distinctly from PathNotFound`() = runTest {
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), SpyEvidenceCustodian())
        val dir = Files.createTempDirectory("local-file-ingress-test")

        val result = coordinator.invoke(ownerPrincipalId, dir.toString(), null)

        assertEquals(OwnerLocalFileIngressOutcome.NotARegularFile, result)
    }

    @Test
    fun `a Unix domain socket returns NotARegularFile`() = runTest {
        val dir = Files.createTempDirectory("local-file-ingress-test")
        val socketPath = dir.resolve("test.sock")
        val channel = try {
            ServerSocketChannel.open(java.net.StandardProtocolFamily.UNIX).apply { bind(UnixDomainSocketAddress.of(socketPath)) }
        } catch (e: Exception) {
            assumeTrue(false, "this platform does not support UNIX domain sockets -- skipping")
            return@runTest
        }
        try {
            val coordinator = OwnerLocalFileIngressCoordinator(approving(), SpyEvidenceCustodian())

            val result = coordinator.invoke(ownerPrincipalId, socketPath.toString(), null)

            assertEquals(OwnerLocalFileIngressOutcome.NotARegularFile, result)
        } finally {
            channel.close()
        }
    }

    @Test
    fun `a FIFO returns NotARegularFile where mkfifo is available`() = runTest {
        val dir = Files.createTempDirectory("local-file-ingress-test")
        val fifoPath = dir.resolve("test.fifo")
        val exitCode = try {
            ProcessBuilder("mkfifo", fifoPath.toString()).start().waitFor()
        } catch (e: IOException) {
            -1
        }
        assumeTrue(exitCode == 0, "mkfifo is not available on this platform -- skipping")

        val coordinator = OwnerLocalFileIngressCoordinator(approving(), SpyEvidenceCustodian())

        val result = coordinator.invoke(ownerPrincipalId, fifoPath.toString(), null)

        assertEquals(OwnerLocalFileIngressOutcome.NotARegularFile, result)
    }

    // --- Symlink rejection (Scope Lock Section 5) ---

    @Test
    fun `a symlink as the final component is rejected, never followed`() = runTest {
        val dir = Files.createTempDirectory("local-file-ingress-test")
        val target = dir.resolve("real.pdf")
        Files.write(target, "real content".toByteArray())
        val link = dir.resolve("link.pdf")
        assumeSymlinkCreated(link, target)
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), SpyEvidenceCustodian())

        val result = coordinator.invoke(ownerPrincipalId, link.toString(), null)

        assertEquals(OwnerLocalFileIngressOutcome.SymlinkProhibited, result)
    }

    @Test
    fun `a symlinked ancestor directory is rejected, even though the final component is a real file`() = runTest {
        val root = Files.createTempDirectory("local-file-ingress-test")
        val realDir = root.resolve("real-dir")
        Files.createDirectories(realDir)
        val target = realDir.resolve("file.pdf")
        Files.write(target, "content".toByteArray())
        val dirLink = root.resolve("dir-link")
        assumeSymlinkCreated(dirLink, realDir)
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), SpyEvidenceCustodian())

        val result = coordinator.invoke(ownerPrincipalId, dirLink.resolve("file.pdf").toString(), null)

        assertEquals(OwnerLocalFileIngressOutcome.SymlinkProhibited, result)
    }

    @Test
    fun `a broken symlink is rejected, not treated as PathNotFound`() = runTest {
        val dir = Files.createTempDirectory("local-file-ingress-test")
        val brokenLink = dir.resolve("broken-link.pdf")
        assumeSymlinkCreated(brokenLink, dir.resolve("never-existed.pdf"))
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), SpyEvidenceCustodian())

        val result = coordinator.invoke(ownerPrincipalId, brokenLink.toString(), null)

        assertEquals(OwnerLocalFileIngressOutcome.SymlinkProhibited, result)
    }

    @Test
    fun `a symlink to a directory is rejected`() = runTest {
        val dir = Files.createTempDirectory("local-file-ingress-test")
        val realDir = dir.resolve("real-dir")
        Files.createDirectories(realDir)
        val dirLink = dir.resolve("dir-link")
        assumeSymlinkCreated(dirLink, realDir)
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), SpyEvidenceCustodian())

        val result = coordinator.invoke(ownerPrincipalId, dirLink.toString(), null)

        assertEquals(OwnerLocalFileIngressOutcome.SymlinkProhibited, result)
    }

    // --- Size bound (Scope Lock Section 7) ---

    @Test
    fun `a source exactly at the 64 MiB bound is accepted`() = runTest {
        val dir = Files.createTempDirectory("local-file-ingress-test")
        val path = dir.resolve("exactly-64mib.bin")
        java.io.RandomAccessFile(path.toFile(), "rw").use { it.setLength(OwnerLocalFileIngressCoordinator.MAX_SOURCE_BYTES) }
        val custodian = realCustodian()
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), custodian)

        val result = coordinator.invoke(ownerPrincipalId, path.toString(), null)

        assertIs<OwnerLocalFileIngressOutcome.Accepted>(result)
    }

    @Test
    fun `a source one byte over the 64 MiB bound is rejected, distinctly and without truncation`() = runTest {
        val dir = Files.createTempDirectory("local-file-ingress-test")
        val path = dir.resolve("one-over.bin")
        java.io.RandomAccessFile(path.toFile(), "rw").use { it.setLength(OwnerLocalFileIngressCoordinator.MAX_SOURCE_BYTES + 1) }
        val custodian = SpyEvidenceCustodian()
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), custodian)

        val result = coordinator.invoke(ownerPrincipalId, path.toString(), null)

        val tooLarge = assertIs<OwnerLocalFileIngressOutcome.SourceTooLarge>(result)
        assertEquals(OwnerLocalFileIngressCoordinator.MAX_SOURCE_BYTES + 1, tooLarge.observedByteLength)
        assertEquals(0, custodian.acceptCallCount, "an oversized source must never reach Evidence Custodian")
    }

    @Test
    fun `boundedRead fails closed when the actual byte count disagrees with the expected length`() {
        val path = tempFile("small content".toByteArray())
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), SpyEvidenceCustodian())

        assertFailsWith<IOException> {
            coordinator.boundedRead(path, expectedByteLength = 999_999L)
        }
    }

    @Test
    fun `boundedRead succeeds when the actual byte count matches the expected length`() {
        val content = "matching content".toByteArray()
        val path = tempFile(content)
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), SpyEvidenceCustodian())

        val result = coordinator.boundedRead(path, expectedByteLength = content.size.toLong())

        assertContentEquals(content, result)
    }

    // --- Byte fidelity (Scope Lock Section 8) ---

    @Test
    fun `byte-for-byte fidelity is preserved for adversarial byte content`() = runTest {
        val content = byteArrayOf(
            0, 0, 0, // leading zeroes
            0x0D, 0x0A, 0x0D, 0x0A, // CRLF mixture
            0x0A, 0x0D, // LF then CR
            *"héllo wörld 日本語 🎉".toByteArray(Charsets.UTF_8),
            0xFF.toByte(), 0x00, 0x7F, 0x80.toByte(), // arbitrary binary
            0, 0, 0, // trailing zeroes
        )
        val path = tempFile(content)
        val custodian = realCustodian()
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), custodian)

        val outcome = coordinator.invoke(ownerPrincipalId, path.toString(), null)

        val accepted = assertIs<OwnerLocalFileIngressOutcome.Accepted>(outcome)
        val retrieved = custodian.retrieve(ownerPrincipalId, accepted.acceptedEvidenceArtifact.evidenceArtifactId)
        val found = assertIs<parker.core.interfaces.EvidenceRetrievalResult.Found>(retrieved)
        assertContentEquals(content, found.content)
    }

    // --- Filename / media type (Scope Lock Section 10/11) ---

    @Test
    fun `a Unicode filename is preserved literally as the basename`() = runTest {
        val content = "content".toByteArray()
        val path = tempFile(content, name = "日本語ファイル名😀.pdf")
        val custodian = realCustodian()
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), custodian)

        val outcome = coordinator.invoke(ownerPrincipalId, path.toString(), null)

        val accepted = assertIs<OwnerLocalFileIngressOutcome.Accepted>(outcome)
        val manifest = assertIs<EvidenceManifestRetrievalResult.Found>(
            custodian.retrieveManifest(ownerPrincipalId, accepted.acceptedEvidenceArtifact.evidenceArtifactId),
        )
        assertEquals("日本語ファイル名😀.pdf", manifest.manifest.originalFileName)
    }

    @Test
    fun `declared upload basename replaces only staging-name provenance and leaves content identity unchanged`() = runTest {
        val content = "stable bytes".toByteArray()
        val stagingPath = tempFile(content, name = "owner-upload-random.part")
        val custodian = realCustodian()
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), custodian)

        val outcome = coordinator.invoke(
            ownerPrincipalId, stagingPath.toString(), "application/pdf", "Example Human Readable Document.pdf",
        )

        val accepted = assertIs<OwnerLocalFileIngressOutcome.Accepted>(outcome)
        val manifest = assertIs<EvidenceManifestRetrievalResult.Found>(
            custodian.retrieveManifest(ownerPrincipalId, accepted.acceptedEvidenceArtifact.evidenceArtifactId),
        ).manifest
        assertEquals("Example Human Readable Document.pdf", manifest.originalFileName)
        assertEquals(content.size.toLong(), manifest.byteLength)
        assertContentEquals(content, assertIs<parker.core.interfaces.EvidenceRetrievalResult.Found>(
            custodian.retrieve(ownerPrincipalId, accepted.acceptedEvidenceArtifact.evidenceArtifactId),
        ).content)
    }

    @Test
    fun `a hostile-looking basename remains inert metadata, never interpreted`() = runTest {
        val content = "content".toByteArray()
        val hostileName = "'; DROP TABLE evidence;--.pdf"
        val path = tempFile(content, name = hostileName)
        val custodian = realCustodian()
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), custodian)

        val outcome = coordinator.invoke(ownerPrincipalId, path.toString(), null)

        val accepted = assertIs<OwnerLocalFileIngressOutcome.Accepted>(outcome)
        val manifest = assertIs<EvidenceManifestRetrievalResult.Found>(
            custodian.retrieveManifest(ownerPrincipalId, accepted.acceptedEvidenceArtifact.evidenceArtifactId),
        )
        assertEquals(hostileName, manifest.manifest.originalFileName)
    }

    @Test
    fun `an optional owner-declared received media type is preserved through to the manifest`() = runTest {
        val path = tempFile("a,b,c\n1,2,3\n".toByteArray(), name = "ledger.csv")
        val custodian = realCustodian()
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), custodian)

        val outcome = coordinator.invoke(ownerPrincipalId, path.toString(), "text/csv")

        val accepted = assertIs<OwnerLocalFileIngressOutcome.Accepted>(outcome)
        val manifest = assertIs<EvidenceManifestRetrievalResult.Found>(
            custodian.retrieveManifest(ownerPrincipalId, accepted.acceptedEvidenceArtifact.evidenceArtifactId),
        )
        assertEquals("text/csv", manifest.manifest.receivedMediaType)
    }

    @Test
    fun `absent received media type remains absent, never fabricated`() = runTest {
        val path = tempFile("content".toByteArray(), name = "no-declared-type.bin")
        val custodian = realCustodian()
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), custodian)

        val outcome = coordinator.invoke(ownerPrincipalId, path.toString(), null)

        val accepted = assertIs<OwnerLocalFileIngressOutcome.Accepted>(outcome)
        val manifest = assertIs<EvidenceManifestRetrievalResult.Found>(
            custodian.retrieveManifest(ownerPrincipalId, accepted.acceptedEvidenceArtifact.evidenceArtifactId),
        )
        assertNull(manifest.manifest.receivedMediaType)
    }

    @Test
    fun `media type is never inferred from the file extension when not declared`() = runTest {
        // A ".csv"-named file with no owner declaration must still record an absent
        // receivedMediaType, never "text/csv" inferred from the extension.
        val path = tempFile("a,b,c\n".toByteArray(), name = "looks-like-csv.csv")
        val custodian = realCustodian()
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), custodian)

        val outcome = coordinator.invoke(ownerPrincipalId, path.toString(), null)

        val accepted = assertIs<OwnerLocalFileIngressOutcome.Accepted>(outcome)
        val manifest = assertIs<EvidenceManifestRetrievalResult.Found>(
            custodian.retrieveManifest(ownerPrincipalId, accepted.acceptedEvidenceArtifact.evidenceArtifactId),
        )
        assertNull(manifest.manifest.receivedMediaType)
    }

    // --- Evidence Custodian boundary (Scope Lock Section 13) ---

    @Test
    fun `Evidence Custodian rejection is preserved faithfully, never reinterpreted as success`() = runTest {
        val custodian = SpyEvidenceCustodian(
            onAccept = { _, _ -> EvidenceAcceptanceResult.Rejected("Permission Engine did not authorise evidence acceptance") },
        )
        val path = tempFile("content".toByteArray())
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), custodian)

        val outcome = coordinator.invoke(ownerPrincipalId, path.toString(), null)

        val rejected = assertIs<OwnerLocalFileIngressOutcome.EvidenceCustodianRejected>(outcome)
        assertEquals("Permission Engine did not authorise evidence acceptance", rejected.reason)
        assertEquals(1, custodian.acceptCallCount)
    }

    @Test
    fun `a successful acceptance returns the EvidenceArtifactId faithfully and Evidence Custodian is called exactly once`() = runTest {
        val content = "faithful content".toByteArray()
        val path = tempFile(content)
        val custodian = realCustodian()
        val custodianSpy = SpyEvidenceCustodian(onAccept = { p, c -> custodian.accept(p, c) })
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), custodianSpy)

        val outcome = coordinator.invoke(ownerPrincipalId, path.toString(), null)

        val accepted = assertIs<OwnerLocalFileIngressOutcome.Accepted>(outcome)
        assertEquals(1, custodianSpy.acceptCallCount)
        val retrieved = custodian.retrieve(ownerPrincipalId, accepted.acceptedEvidenceArtifact.evidenceArtifactId)
        assertIs<parker.core.interfaces.EvidenceRetrievalResult.Found>(retrieved)
    }

    // --- Structural / authority boundary ---

    @Test
    fun `coordinator declares exactly two constructor dependencies -- PermissionEngine and EvidenceCustodian only`() {
        val constructor = OwnerLocalFileIngressCoordinator::class.constructors.single()
        val parameterTypes = constructor.parameters.map { it.type.classifier }

        assertEquals(
            setOf(parker.core.interfaces.PermissionEngine::class, parker.core.interfaces.EvidenceCustodian::class),
            parameterTypes.toSet(),
            "OwnerLocalFileIngressCoordinator must depend on exactly PermissionEngine and EvidenceCustodian -- " +
                "no EvidenceArtifactStorage, EvidenceSourceManifestStorage, TierADocumentIngestionRouter, " +
                "TierAOwnerInvocationCoordinator, MemoryCore, KnowledgeStore, or EvidenceIntelligence reference of its own",
        )
        assertEquals(2, parameterTypes.size)
    }

    // --- Repeated import (Scope Lock Section 16) ---

    @Test
    fun `importing the same file twice produces two distinct EvidenceArtifactIds, both independently retrievable`() = runTest {
        val content = "same content twice".toByteArray()
        val path = tempFile(content)
        val custodian = realCustodian()
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), custodian)

        val first = assertIs<OwnerLocalFileIngressOutcome.Accepted>(coordinator.invoke(ownerPrincipalId, path.toString(), null))
        val second = assertIs<OwnerLocalFileIngressOutcome.Accepted>(coordinator.invoke(ownerPrincipalId, path.toString(), null))

        assertNotEquals(
            first.acceptedEvidenceArtifact.evidenceArtifactId,
            second.acceptedEvidenceArtifact.evidenceArtifactId,
        )
        assertIs<parker.core.interfaces.EvidenceRetrievalResult.Found>(
            custodian.retrieve(ownerPrincipalId, first.acceptedEvidenceArtifact.evidenceArtifactId),
        )
        assertIs<parker.core.interfaces.EvidenceRetrievalResult.Found>(
            custodian.retrieve(ownerPrincipalId, second.acceptedEvidenceArtifact.evidenceArtifactId),
        )
    }

    // --- Source immutability (Scope Lock Section 17) ---

    @Test
    fun `the source file on disk is never mutated by ingress`() = runTest {
        val content = "immutable source".toByteArray()
        val path = tempFile(content)
        val beforeAttrs = Files.readAttributes(path, BasicFileAttributes::class.java)
        val custodian = realCustodian()
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), custodian)

        coordinator.invoke(ownerPrincipalId, path.toString(), null)

        assertContentEquals(content, Files.readAllBytes(path))
        val afterAttrs = Files.readAttributes(path, BasicFileAttributes::class.java)
        assertEquals(beforeAttrs.size(), afterAttrs.size())
    }

    // --- Path privacy (Scope Lock Section 18) ---

    @Test
    fun `only the basename, never the full path, is persisted in the manifest`() = runTest {
        val content = "content".toByteArray()
        val path = tempFile(content, name = "report.pdf")
        val custodian = realCustodian()
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), custodian)

        val outcome = coordinator.invoke(ownerPrincipalId, path.toString(), null)

        val accepted = assertIs<OwnerLocalFileIngressOutcome.Accepted>(outcome)
        val manifest = assertIs<EvidenceManifestRetrievalResult.Found>(
            custodian.retrieveManifest(ownerPrincipalId, accepted.acceptedEvidenceArtifact.evidenceArtifactId),
        )
        assertEquals("report.pdf", manifest.manifest.originalFileName)
        assertFalse(manifest.manifest.originalFileName!!.contains(path.parent.toString()))
    }

    // --- Non-discovery (Scope Lock Section 6) ---

    @Test
    fun `only the exact designated file is read, never a similarly-named sibling`() = runTest {
        val dir = Files.createTempDirectory("local-file-ingress-test")
        val target = dir.resolve("target.pdf")
        Files.write(target, "target content".toByteArray())
        Files.write(dir.resolve("target.pdf.bak"), "backup content -- must never be read".toByteArray())
        val custodian = realCustodian()
        val coordinator = OwnerLocalFileIngressCoordinator(approving(), custodian)

        val outcome = coordinator.invoke(ownerPrincipalId, target.toString(), null)

        val accepted = assertIs<OwnerLocalFileIngressOutcome.Accepted>(outcome)
        val retrieved = assertIs<parker.core.interfaces.EvidenceRetrievalResult.Found>(
            custodian.retrieve(ownerPrincipalId, accepted.acceptedEvidenceArtifact.evidenceArtifactId),
        )
        assertContentEquals("target content".toByteArray(), retrieved.content)

        // A typo'd path must not be fuzzily resolved to the real sibling.
        val typoResult = coordinator.invoke(ownerPrincipalId, dir.resolve("targe.pdf").toString(), null)
        assertEquals(OwnerLocalFileIngressOutcome.PathNotFound, typoResult)
    }


    private fun assumeSymlinkCreated(link: Path, target: Path) {
        val failure = runCatching { Files.createSymbolicLink(link, target) }.exceptionOrNull()
        assumeTrue(
            failure == null,
            "NOT APPLICABLE ON THIS FILESYSTEM: symbolic-link fixture unavailable (${failure?.javaClass?.simpleName})",
        )
    }

    // --- No new PermissionAction/ResourceType (Scope Lock Section 3/21) ---

    @Test
    fun `no new PermissionAction or ResourceType was introduced`() {
        assertEquals(
            setOf(
                PermissionAction.READ, PermissionAction.WRITE, PermissionAction.DELETE, PermissionAction.EXECUTE,
                PermissionAction.EXPORT, PermissionAction.SHARE, PermissionAction.CONTROL, PermissionAction.NOTIFY,
                PermissionAction.SCHEDULE, PermissionAction.SEND_EXTERNAL,
            ),
            PermissionAction.entries.toSet(),
            "this test's own frozen set must remain exhaustive -- a new PermissionAction here signals a " +
                "constitutional change this Unit's own governance did not authorize",
        )
        assertEquals(
            setOf(
                ResourceType.MEMORY, ResourceType.WORLD_MODEL, ResourceType.DOCUMENT, ResourceType.EMAIL,
                ResourceType.CALENDAR, ResourceType.CONTACT, ResourceType.HOME_ASSISTANT_ENTITY,
                ResourceType.ANDROID_CAPABILITY, ResourceType.TOOL, ResourceType.PLUGIN, ResourceType.AGENT,
                ResourceType.SECRET, ResourceType.CONFIGURATION, ResourceType.AUDIT_LOG,
            ),
            ResourceType.entries.toSet(),
            "this test's own frozen set must remain exhaustive -- a new ResourceType here signals a " +
                "constitutional change this Unit's own governance did not authorize",
        )
    }

    // --- Test helpers ---

    private fun assertFalse(condition: Boolean) = assertTrue(!condition)

    private class SpyEvidenceCustodian(
        private val onAccept: suspend (PrincipalId, parker.core.interfaces.CandidateEvidenceArtifact) -> EvidenceAcceptanceResult = { _, _ ->
            EvidenceAcceptanceResult.Accepted(
                parker.core.interfaces.AcceptedEvidenceArtifact(
                    parker.core.interfaces.EvidenceArtifactId("evidence-${java.util.UUID.randomUUID()}"),
                    java.time.Instant.now(),
                ),
            )
        },
    ) : parker.core.interfaces.EvidenceCustodian {
        var acceptCallCount = 0
            private set

        override suspend fun accept(
            requestingPrincipalId: PrincipalId,
            candidate: parker.core.interfaces.CandidateEvidenceArtifact,
        ): EvidenceAcceptanceResult {
            acceptCallCount++
            return onAccept(requestingPrincipalId, candidate)
        }

        override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: parker.core.interfaces.EvidenceArtifactId) =
            throw AssertionError("retrieve must not be called by OwnerLocalFileIngressCoordinator")

        override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: parker.core.interfaces.EvidenceArtifactId) =
            throw AssertionError("retrieveManifest must not be called by OwnerLocalFileIngressCoordinator")
    }
}
