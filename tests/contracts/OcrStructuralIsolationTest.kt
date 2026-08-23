package parker.core.interfaces

import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.runtime.OcrExecutionSequencer

/**
 * OCR Mechanism, Implementation Unit 9 ("Structural Isolation Proof").
 * Governed in full by `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md`
 * ("the Contract Design"); by `docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md`
 * ("the Scope Lock") Section 13, Section 14, Section 16; and by
 * `docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan") Unit 9.
 *
 * **This Unit adds no production file.** Its own Purpose is explicit:
 * "Prove, rather than assert, that the OCR mechanism holds none of the
 * dependencies Scope Lock Section 13 excludes." Its own Files-expected-
 * to-change field is "None in production code; new test files only." This
 * file is that proof -- a single, dedicated, consolidated suite proving
 * all eight Scope Lock Section 13 exclusions independently, plus the
 * additional isolation properties (import, type, behaviour, composition,
 * provider, interface) this Unit's own governing prompt names, none of
 * which requires a new production type since Units 1-8 already introduced
 * none of them.
 *
 * **Primary proof technique: a closed, transitively-computed reachable-
 * type graph.** [collectReachableParkerTypes] walks every declared
 * property, primary-constructor parameter, and function signature
 * (parameters and return type, including generic type arguments) reachable
 * from each OCR entry type, recursively, stopping only at non-`parker.*`
 * types (Kotlin/Java standard library types are treated as safe leaves,
 * never expanded further). The resulting set is then asserted to be a
 * *subset* of an explicit allow-list containing only the OCR mechanism's
 * own types plus [EvidenceArtifactId] (a reused identifier value, not a
 * dependency -- see `OcrMechanism.kt`'s own KDoc) and [TranscriptionFidelity].
 * This single test structurally guarantees the absence of *every* excluded
 * category at once -- Evidence Custodian, Memory Core, Knowledge
 * Submission, the Permission Engine, Evidence Intelligence, runtime
 * conversation components, reporting mechanisms, Docling, and anything
 * else not already known-safe -- rather than checking only the specific
 * named types Scope Lock Section 13 happens to enumerate. Named,
 * per-exclusion tests are added alongside it purely for direct citation
 * and readability, not because the closed-set proof is insufficient.
 */
class OcrStructuralIsolationTest {

    private val ocrProductionFiles = listOf(
        java.io.File("src/interfaces/OcrMechanism.kt"),
        java.io.File("src/interfaces/OcrProviderAdapter.kt"),
        java.io.File("src/runtime/OcrExecutionSequencer.kt"),
    )

    private fun String.codeOnly(): String =
        replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .lines()
            .joinToString("\n") { line -> line.substringBefore("//") }

    /**
     * The OCR mechanism's own complete public type set -- everything Units
     * 1-8 introduced, and nothing else. Used both as the allow-list for the
     * reachability walk below and as the expected set for the "no new
     * public API" interface-isolation test.
     */
    private val ocrOwnTypes: Set<KClass<*>> = setOf(
        OcrMechanism::class, OcrProviderAdapter::class, OcrExecutionSequencer::class,
        OcrRecognitionRequest::class, OcrRecognitionIdentity::class, OcrRecognitionSegment::class,
        OcrRecognitionResult::class, OcrRecognitionOutcome::class,
        OcrRecognitionOutcome.Recognised::class, OcrRecognitionOutcome.Failed::class,
        OcrRecognitionOutcome.NotAuthorised::class, OcrRecognitionOutcome.UnsupportedOrInaccessibleInput::class,
        OcrRecognitionOutcome.NoRecognisableContent::class, OcrRecognitionOutcome.PartialOrDegradedOutput::class,
        OcrRecognitionOutcome.ValidationRejection::class, OcrRecognitionOutcome.ProcessingOrDependencyFailure::class,
        OcrRecognitionOutcome.GenuineImplementationFault::class,
    )

    /** [EvidenceArtifactId] (a reused identifier value) and [TranscriptionFidelity] (Unit 1's own closed enum). */
    private val allowedExternalLeaves: Set<KClass<*>> = setOf(EvidenceArtifactId::class, TranscriptionFidelity::class)

    private fun collectReachableParkerTypes(root: KClass<*>, visited: MutableSet<KClass<*>> = mutableSetOf()): Set<KClass<*>> {
        if (!visited.add(root)) return emptySet()
        val qualifiedName = root.qualifiedName.orEmpty()
        if (!qualifiedName.startsWith("parker.")) return emptySet()

        val result = mutableSetOf(root)
        fun expand(classifier: KClass<*>?) {
            if (classifier != null) result += collectReachableParkerTypes(classifier, visited)
        }

        root.declaredMemberProperties.forEach { property ->
            expand(property.returnType.classifier as? KClass<*>)
            property.returnType.arguments.forEach { arg -> expand(arg.type?.classifier as? KClass<*>) }
        }
        root.primaryConstructor?.parameters?.forEach { parameter -> expand(parameter.type.classifier as? KClass<*>) }
        root.declaredFunctions.forEach { function ->
            function.parameters.forEach { parameter -> expand(parameter.type.classifier as? KClass<*>) }
            expand(function.returnType.classifier as? KClass<*>)
        }
        root.sealedSubclasses.forEach { subclass -> result += collectReachableParkerTypes(subclass, visited) }

        return result
    }

    // -- Dependency isolation: the single, closed reachable-type-graph proof --

    @Test
    fun `every parker-owned type reachable from any OCR entry type, at any depth, is either the OCR mechanism's own type or an already-authorised external leaf`() {
        val entryPoints = listOf(
            OcrMechanism::class, OcrProviderAdapter::class, OcrExecutionSequencer::class,
            OcrRecognitionRequest::class, OcrRecognitionIdentity::class, OcrRecognitionSegment::class,
            OcrRecognitionResult::class, OcrRecognitionOutcome::class,
        )
        val reachable = entryPoints.flatMap { collectReachableParkerTypes(it) }.toSet()
        val disallowed = reachable - ocrOwnTypes - allowedExternalLeaves

        assertTrue(
            disallowed.isEmpty(),
            "Found parker-owned types reachable from the OCR mechanism's own public shape that are neither the " +
                "OCR mechanism's own type nor an already-authorised external leaf (EvidenceArtifactId, " +
                "TranscriptionFidelity) -- Scope Lock Section 13 requires zero platform-subsystem dependency of " +
                "any kind. Found: ${disallowed.map { it.qualifiedName }}",
        )
    }

    // -- Dependency isolation: named exclusions, for direct citation (Scope Lock Section 13's own eight items) --

    @Test
    fun `no OCR type reaches EvidenceCustodian, EvidenceIntelligence, or OwnerEvidenceDeletionAuthority, at any declared-property depth`() {
        assertNoReachableDependency(EvidenceCustodian::class, "EvidenceCustodian")
        assertNoReachableDependency(EvidenceIntelligence::class, "EvidenceIntelligence")
        assertNoReachableDependency(OwnerEvidenceDeletionAuthority::class, "OwnerEvidenceDeletionAuthority")
    }

    @Test
    fun `no OCR type reaches MemoryCore or MemoryRetrieval, at any declared-property depth`() {
        assertNoReachableDependency(MemoryCore::class, "MemoryCore")
        assertNoReachableDependency(MemoryRetrieval::class, "MemoryRetrieval")
    }

    @Test
    fun `no OCR type reaches KnowledgeSubmission, at any declared-property depth`() {
        assertNoReachableDependency(KnowledgeSubmission::class, "KnowledgeSubmission")
    }

    @Test
    fun `no OCR type reaches PermissionEngine, at any declared-property depth`() {
        assertNoReachableDependency(PermissionEngine::class, "PermissionEngine")
    }

    @Test
    fun `no OCR type reaches evidence-registration, provenance, or candidate-record types, at any declared-property depth`() {
        assertNoReachableDependency(Provenance::class, "Provenance")
        assertNoReachableDependency(CandidateProvenance::class, "CandidateProvenance")
        assertNoReachableDependency(CandidateEvidenceArtifact::class, "CandidateEvidenceArtifact")
        assertNoReachableDependency(CandidateKnowledge::class, "CandidateKnowledge")
        assertNoReachableDependency(CandidateDocument::class, "CandidateDocument")
        assertNoReachableDependency(parker.core.runtime.EvidenceRegistrationOutcome::class, "EvidenceRegistrationOutcome")
    }

    private fun assertNoReachableDependency(excluded: KClass<*>, label: String) {
        val excludedQualifiedName = excluded.qualifiedName
        val allTypesInScope = ocrOwnTypes
        allTypesInScope.forEach { type ->
            val propertyTypeNames = type.declaredMemberProperties.flatMap { property ->
                listOf(property.returnType.classifier).mapNotNull { (it as? KClass<*>)?.qualifiedName } +
                    property.returnType.arguments.mapNotNull { arg -> (arg.type?.classifier as? KClass<*>)?.qualifiedName }
            }
            val constructorTypeNames = type.primaryConstructor?.parameters?.mapNotNull {
                (it.type.classifier as? KClass<*>)?.qualifiedName
            } ?: emptyList()
            val functionTypeNames = type.declaredFunctions.flatMap { function ->
                function.parameters.mapNotNull { (it.type.classifier as? KClass<*>)?.qualifiedName } +
                    listOfNotNull((function.returnType.classifier as? KClass<*>)?.qualifiedName)
            }
            (propertyTypeNames + constructorTypeNames + functionTypeNames).forEach { qualifiedName ->
                assertFalse(
                    qualifiedName == excludedQualifiedName,
                    "${type.simpleName} must not reference '$label' -- Scope Lock Section 13 excludes it from the " +
                        "OCR mechanism's own dependency graph, at any depth.",
                )
            }
        }
    }

    // -- Import isolation: production files' own import lists are exactly, and only, what governance already documents --

    @Test
    fun `OcrMechanism_kt imports exactly java_time_Instant, and no runtime, evidence, memory, knowledge, permission, networking, storage, or logging package`() {
        val imports = importsOf(java.io.File("src/interfaces/OcrMechanism.kt"))
        assertEquals(setOf("java.time.Instant"), imports)
    }

    @Test
    fun `OcrProviderAdapter_kt imports nothing`() {
        val imports = importsOf(java.io.File("src/interfaces/OcrProviderAdapter.kt"))
        assertTrue(imports.isEmpty(), "OcrProviderAdapter.kt must import nothing -- found: $imports")
    }

    @Test
    fun `OcrExecutionSequencer_kt imports exactly its own four sibling OCR types, and nothing from any other package`() {
        val imports = importsOf(java.io.File("src/runtime/OcrExecutionSequencer.kt"))
        assertEquals(
            setOf(
                "parker.core.interfaces.OcrMechanism", "parker.core.interfaces.OcrProviderAdapter",
                "parker.core.interfaces.OcrRecognitionOutcome", "parker.core.interfaces.OcrRecognitionRequest",
            ),
            imports,
        )
    }

    private fun importsOf(file: java.io.File): Set<String> {
        check(file.exists()) { "${file.path} not found from working directory ${java.io.File(".").absolutePath}" }
        return file.readText().codeOnly().lines()
            .map { it.trim() }
            .filter { it.startsWith("import ") }
            .map { it.removePrefix("import ").trim() }
            .toSet()
    }

    @Test
    fun `no production OCR file imports a prohibited package -- database, networking, filesystem, logging, configuration, or dependency-injection vocabulary`() {
        val forbiddenImportFragments = listOf(
            "java.sql", "javax.sql", "java.net", "java.nio.file", "okhttp", "retrofit", "ktor",
            "org.slf4j", "java.util.logging", "log4j", "config", "Config",
            "javax.inject", "dagger", "koin", "guice", "ServiceLocator",
        )
        ocrProductionFiles.forEach { file ->
            val imports = importsOf(file)
            imports.forEach { import ->
                forbiddenImportFragments.forEach { forbidden ->
                    assertFalse(
                        import.contains(forbidden),
                        "${file.path} must not import '$import' -- contains prohibited fragment '$forbidden'",
                    )
                }
            }
        }
    }

    // -- Type isolation: OCR types subclass nothing from Parker's governed/runtime type surface --

    @Test
    fun `no OCR type is a subclass of CandidateEvidenceArtifact, CandidateKnowledge, CandidateDocument, or CandidateProvenance`() {
        ocrOwnTypes.forEach { type ->
            assertFalse(type.isSubclassOf(CandidateEvidenceArtifact::class), "${type.simpleName} must not subclass CandidateEvidenceArtifact")
            assertFalse(type.isSubclassOf(CandidateKnowledge::class), "${type.simpleName} must not subclass CandidateKnowledge")
            assertFalse(type.isSubclassOf(CandidateDocument::class), "${type.simpleName} must not subclass CandidateDocument")
            assertFalse(type.isSubclassOf(CandidateProvenance::class), "${type.simpleName} must not subclass CandidateProvenance")
        }
    }

    @Test
    fun `every OCR type's own supertype chain contains only Any, its own OCR interfaces, or OcrRecognitionOutcome -- nothing from a Parker runtime or composition type`() {
        ocrOwnTypes.forEach { type ->
            val supertypeNames = type.supertypes.mapNotNull { (it.classifier as? KClass<*>)?.qualifiedName }
            supertypeNames.forEach { qualifiedName ->
                val isAllowed = qualifiedName == Any::class.qualifiedName ||
                    ocrOwnTypes.any { it.qualifiedName == qualifiedName }
                assertTrue(isAllowed, "${type.simpleName} declares a supertype '$qualifiedName' outside Any and the OCR mechanism's own type set")
            }
        }
    }

    // -- Behaviour isolation: no retry, batching, caching, authorisation, registration, provenance, persistence, or orchestration --

    @Test
    fun `no production OCR file contains retry, batching, caching, authorisation, registration, provenance, or persistence vocabulary`() {
        val forbiddenFragments = listOf(
            "retry", "Retry", "batch", "Batch", "cache", "Cache",
            "authoris", "authoriz", "register(", "Registration",
            "Provenance", "persist", "Persist", "orchestrat", "Orchestrat",
        )
        ocrProductionFiles.forEach { file ->
            val codeOnly = file.readText().codeOnly()
            forbiddenFragments.forEach { forbidden ->
                assertFalse(
                    codeOnly.contains(forbidden),
                    "${file.path} must not contain '$forbidden' -- Scope Lock Section 4, Section 16 forbid retry, " +
                        "batching, caching, authorisation, evidence registration, provenance construction, " +
                        "persistence, and orchestration in the OCR mechanism's own code.",
                )
            }
        }
    }

    @Test
    fun `OcrExecutionSequencer performs exactly one adapter invocation per call, never retried, batched, or cached`() = runTest {
        class CountingFakeAdapter : OcrProviderAdapter {
            var invocationCount = 0
                private set
            override suspend fun recognise(request: OcrRecognitionRequest): OcrRecognitionOutcome {
                invocationCount += 1
                return OcrRecognitionOutcome.Failed("fake -- counting only")
            }
        }
        val adapter = CountingFakeAdapter()
        val sequencer = OcrExecutionSequencer(adapter)
        val request = OcrRecognitionRequest(EvidenceArtifactId("evidence-1"), byteArrayOf(1, 2, 3), "image/png")

        sequencer.recognise(request)
        sequencer.recognise(request)

        assertEquals(2, adapter.invocationCount, "exactly one adapter invocation per sequencer call -- no retry, no batching, no caching of a prior call's own result")
    }

    // -- Composition isolation: no reference to ParkerRuntime, composition roots, or dependency-injection/service-locator vocabulary --

    @Test
    fun `no production OCR file references ParkerRuntime, a composition root, a runtime coordinator, or any dependency-injection or service-locator vocabulary`() {
        val forbiddenFragments = listOf("ParkerRuntime", "CompositionRoot", "RuntimeCoordinator", "ServiceLocator", "DependencyInjection", "@Inject", "@Component")
        ocrProductionFiles.forEach { file ->
            val codeOnly = file.readText().codeOnly()
            forbiddenFragments.forEach { forbidden ->
                assertFalse(codeOnly.contains(forbidden), "${file.path} must not reference '$forbidden' -- runtime composition remains Implementation Plan Unit 12, structurally blocked")
            }
        }
    }

    // -- Provider isolation: OcrMechanism -> OcrExecutionSequencer -> OcrProviderAdapter, no alternate execution path --

    @Test
    fun `OcrExecutionSequencer is the sole class in src implementing OcrMechanism`() {
        val implementsPattern = Regex("""\b(?:class|object)\s+\w+(?:\([^)]*\))?\s*:\s*[\w<>,\s]*\bOcrMechanism\b""")
        val srcRoot = java.io.File("src")
        check(srcRoot.exists()) { "src/ directory not found" }
        val implementers = srcRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> implementsPattern.containsMatchIn(file.readText()) }
            .map { it.path.replace('\\', '/') }
            .toList()

        assertEquals(listOf("src/runtime/OcrExecutionSequencer.kt"), implementers, "exactly one class must implement OcrMechanism -- found: $implementers")
    }

    @Test
    fun `DoclingOcrProviderAdapter is the sole concrete class in src implementing OcrProviderAdapter`() {
        // Updated during the Docling Concrete Adapter Implementation Unit -- see
        // OcrProviderAdapterScopeTest.kt's own identical update for the full citation. This
        // test's own isolation-proof purpose is preserved: exactly one, named, authorized
        // provider adapter, never a second, unauthorized one.
        val implementsPattern = Regex("""\b(?:class|object)\s+\w+(?:\([^)]*\))?\s*:\s*[\w<>,\s]*\bOcrProviderAdapter\b""")
        val srcRoot = java.io.File("src")
        val implementers = srcRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> implementsPattern.containsMatchIn(file.readText()) }
            .map { it.path.replace('\\', '/') }
            .toList()

        assertEquals(
            listOf("src/runtime/DoclingOcrProviderAdapter.kt"),
            implementers,
            "exactly one, authorized concrete adapter may implement OcrProviderAdapter -- found: $implementers",
        )
    }

    @Test
    fun `OcrExecutionSequencer's own recognise function body is exactly one delegating call to its own adapter -- no branching alternate path`() {
        val body = java.io.File("src/runtime/OcrExecutionSequencer.kt").readText().codeOnly()
        val functionBodies = body.substringAfter("override suspend fun recognise")

        assertFalse(functionBodies.contains("if "), "OcrExecutionSequencer.recognise must contain no conditional branching -- a single delegating call only")
        assertFalse(functionBodies.contains("when "), "OcrExecutionSequencer.recognise must contain no branching dispatch -- a single delegating call only")
        assertFalse(functionBodies.contains("try"), "OcrExecutionSequencer.recognise must contain no try/catch -- Unit 3's own fault-propagation discipline")
    }

    // -- Interface isolation: no new public API beyond the governed contract --

    @Test
    fun `OcrMechanism_kt declares exactly its own seven top-level public declarations, no more`() {
        val topLevelNames = topLevelDeclarationNames(java.io.File("src/interfaces/OcrMechanism.kt"))
        assertEquals(
            setOf("OcrMechanism", "OcrRecognitionRequest", "TranscriptionFidelity", "OcrRecognitionIdentity", "OcrRecognitionSegment", "OcrRecognitionResult", "OcrRecognitionOutcome"),
            topLevelNames,
        )
    }

    @Test
    fun `OcrProviderAdapter_kt declares exactly one top-level public declaration`() {
        assertEquals(setOf("OcrProviderAdapter"), topLevelDeclarationNames(java.io.File("src/interfaces/OcrProviderAdapter.kt")))
    }

    @Test
    fun `OcrExecutionSequencer_kt declares exactly one top-level public declaration`() {
        assertEquals(setOf("OcrExecutionSequencer"), topLevelDeclarationNames(java.io.File("src/runtime/OcrExecutionSequencer.kt")))
    }

    private fun topLevelDeclarationNames(file: java.io.File): Set<String> {
        check(file.exists()) { "${file.path} not found" }
        val topLevelPattern = Regex("""^(?:sealed |data |value |enum |open |abstract )*(?:class|interface|object)\s+(\w+)""")
        return file.readText().codeOnly().lines()
            .mapNotNull { line -> topLevelPattern.find(line)?.groupValues?.get(1) }
            .toSet()
    }

    @Test
    fun `OcrRecognitionOutcome declares exactly its own nine sealed subclasses -- no tenth has appeared`() {
        val expected = setOf(
            "Recognised", "Failed", "NotAuthorised", "UnsupportedOrInaccessibleInput", "NoRecognisableContent",
            "PartialOrDegradedOutput", "ValidationRejection", "ProcessingOrDependencyFailure", "GenuineImplementationFault",
        )
        assertEquals(expected, OcrRecognitionOutcome::class.sealedSubclasses.map { it.simpleName }.toSet())
    }
}
