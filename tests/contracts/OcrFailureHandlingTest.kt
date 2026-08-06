package parker.core.interfaces

import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.runtime.OcrExecutionSequencer

/**
 * OCR Mechanism, Implementation Unit 7 ("Failure Handling"). Governed in
 * full by `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md` ("the
 * Contract Design") Section 6; by
 * `docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md` ("the Scope Lock")
 * Section 10, Section 11; and by
 * `docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan") Unit 7.
 *
 * **What Unit 7 introduced.** Seven new sibling subclasses of
 * `OcrRecognitionOutcome`, one per Scope Lock Section 10 distinction,
 * added alongside -- never replacing -- Unit 1's own `Recognised` and
 * `Failed`. Five are freely producible now (`UnsupportedOrInaccessibleInput`,
 * `NoRecognisableContent`, `PartialOrDegradedOutput`,
 * `ProcessingOrDependencyFailure`, `GenuineImplementationFault`); two exist
 * only for taxonomy completeness and are never constructed by any code
 * path in Units 1-7 (`NotAuthorised`, `ValidationRejection`).
 *
 * This suite is deliberately independent of `OcrMechanismScopeTest.kt`'s
 * own updated structural tests (exhaustive nine-variant check; no
 * kind/code/category field) -- it exercises Unit 7's own behavioural and
 * responsibility-allocation concerns instead: distinguishability under
 * use, non-fabrication of the two reserved distinctions, partial-content
 * preservation, and continued Unit 3 fault-propagation discipline.
 */
class OcrFailureHandlingTest {

    private val ocrUnitFiles = listOf(
        java.io.File("src/interfaces/OcrMechanism.kt"),
        java.io.File("src/interfaces/OcrProviderAdapter.kt"),
        java.io.File("src/runtime/OcrExecutionSequencer.kt"),
    )

    /**
     * Strips KDoc/block comments and line comments, and additionally
     * strips the two declaration lines for [OcrRecognitionOutcome.NotAuthorised]
     * and [OcrRecognitionOutcome.ValidationRejection] themselves, before a
     * whole-file scan for a *construction* of either type. Without this,
     * a scan would trip on the type's own `data class NotAuthorised(...)`
     * declaration line -- the self-referential trap this repository's own
     * `OcrInputContractTest.kt` first introduced `codeOnly()` to avoid.
     * Only an actual constructor *call* -- `NotAuthorised(...)` used as an
     * expression, never as a declaration -- may fail the check below.
     */
    private fun String.codeOnlyExcludingReservedDeclarations(): String =
        replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .lines()
            .filterNot { line ->
                val trimmed = line.trimStart()
                trimmed.startsWith("data class NotAuthorised(") || trimmed.startsWith("data class ValidationRejection(")
            }
            .joinToString("\n") { line -> line.substringBefore("//") }

    private fun sampleResult(text: String = "recognised text") = OcrRecognitionResult(
        recognisedText = text,
        fidelity = TranscriptionFidelity.VERBATIM,
        identity = OcrRecognitionIdentity(mechanismIdentity = "mechanism-a", configurationProfile = "profile-a"),
        recognisedAt = java.time.Instant.EPOCH,
    )

    // -- Every authorised failure distinction remains distinguishable, none collapsed --

    @Test
    fun `all nine outcome variants are structurally distinct types`() {
        val instances: List<OcrRecognitionOutcome> = listOf(
            OcrRecognitionOutcome.Recognised(sampleResult()),
            OcrRecognitionOutcome.Failed("generic failure"),
            OcrRecognitionOutcome.NotAuthorised("not authorised"),
            OcrRecognitionOutcome.UnsupportedOrInaccessibleInput("unsupported input"),
            OcrRecognitionOutcome.NoRecognisableContent("no recognisable content"),
            OcrRecognitionOutcome.PartialOrDegradedOutput(sampleResult(), "degraded"),
            OcrRecognitionOutcome.ValidationRejection("validation rejection"),
            OcrRecognitionOutcome.ProcessingOrDependencyFailure("processing failure"),
            OcrRecognitionOutcome.GenuineImplementationFault("implementation fault"),
        )

        val distinctClasses = instances.map { it::class }.toSet()
        assertEquals(9, distinctClasses.size, "every one of the nine outcome variants must be its own, structurally distinct type -- none collapsed into another")
    }

    @Test
    fun `success remains distinct from every failure-tier variant`() {
        val recognised: OcrRecognitionOutcome = OcrRecognitionOutcome.Recognised(sampleResult())
        val failureVariants: List<OcrRecognitionOutcome> = listOf(
            OcrRecognitionOutcome.Failed("x"),
            OcrRecognitionOutcome.NotAuthorised("x"),
            OcrRecognitionOutcome.UnsupportedOrInaccessibleInput("x"),
            OcrRecognitionOutcome.NoRecognisableContent("x"),
            OcrRecognitionOutcome.PartialOrDegradedOutput(sampleResult(), "x"),
            OcrRecognitionOutcome.ValidationRejection("x"),
            OcrRecognitionOutcome.ProcessingOrDependencyFailure("x"),
            OcrRecognitionOutcome.GenuineImplementationFault("x"),
        )

        failureVariants.forEach { failure ->
            assertNotEquals(recognised::class, failure::class, "Recognised must remain distinct from ${failure::class.simpleName}")
            assertTrue(recognised !is OcrRecognitionOutcome.Failed, "Recognised must never itself be a Failed subtype")
        }
    }

    @Test
    fun `validation rejection cannot be confused with provider processing failure -- structurally distinct, independently producible`() {
        val validationRejection: KClass<*> = OcrRecognitionOutcome.ValidationRejection("rejected by validation policy")::class
        val processingFailure: KClass<*> = OcrRecognitionOutcome.ProcessingOrDependencyFailure("a required processing step was unavailable")::class

        assertNotEquals(
            validationRejection,
            processingFailure,
            "ValidationRejection (Parker-owned output-quality judgement, Scope Lock Section 11) must remain a " +
                "structurally distinct type from ProcessingOrDependencyFailure (the OCR mechanism's own operational " +
                "concern, Contract Design Section 6) -- never the same representation.",
        )
    }

    // -- Partial/degraded output preserves lawful partial content ------------

    @Test
    fun `PartialOrDegradedOutput preserves the actual partial recognition result unchanged, never discarding it`() {
        val partial = sampleResult(text = "only the top half of the page was legible")
        val outcome = OcrRecognitionOutcome.PartialOrDegradedOutput(partialResult = partial, reason = "bottom half of page illegible")

        assertEquals(partial, outcome.partialResult, "the actual partial recognition must be preserved unchanged, never discarded")
        assertEquals("only the top half of the page was legible", outcome.partialResult.recognisedText)
        assertEquals("bottom half of page illegible", outcome.reason)
    }

    @Test
    fun `PartialOrDegradedOutput is never itself a plain Recognised -- degraded output is never silently promoted to full success`() {
        val outcome: OcrRecognitionOutcome = OcrRecognitionOutcome.PartialOrDegradedOutput(sampleResult(), "degraded")

        assertTrue(outcome !is OcrRecognitionOutcome.Recognised, "a degraded recognition must never be represented as though it were an ordinary, undegraded success")
    }

    // -- Not-authorised: reserved, never fabricated by a provider adapter ----

    @Test
    fun `no code path in Units 1-7 ever constructs NotAuthorised -- it is an orchestration-layer outcome the OCR mechanism never produces`() {
        val constructionPattern = Regex("""\bNotAuthorised\(""")

        ocrUnitFiles.forEach { file ->
            check(file.exists()) { "${file.path} not found from working directory ${java.io.File(".").absolutePath}" }
            assertFalse(
                constructionPattern.containsMatchIn(file.readText().codeOnlyExcludingReservedDeclarations()),
                "${file.path} must never construct OcrRecognitionOutcome.NotAuthorised -- Implementation Plan Unit " +
                    "7's own Constitutional constraints are explicit: it is represented 'for completeness of the " +
                    "taxonomy' only, an orchestration-layer outcome that stops before any of Units 1-7 is ever " +
                    "reached, and this Unit adds no Permission Engine reference for any adapter to fabricate one.",
            )
        }
    }

    @Test
    fun `no code path in Units 1-7 ever constructs ValidationRejection -- it is Parker-owned validation judgement the OCR mechanism never produces`() {
        val constructionPattern = Regex("""\bValidationRejection\(""")

        ocrUnitFiles.forEach { file ->
            check(file.exists()) { "${file.path} not found from working directory ${java.io.File(".").absolutePath}" }
            assertFalse(
                constructionPattern.containsMatchIn(file.readText().codeOnlyExcludingReservedDeclarations()),
                "${file.path} must never construct OcrRecognitionOutcome.ValidationRejection -- this Unit implements " +
                    "no validation policy, threshold, or mechanism of any kind (Scope Lock Section 11); the type " +
                    "exists only so a future, separately governed Parker-owned validation step has an " +
                    "already-reviewed place to report rejection.",
            )
        }
    }

    @Test
    fun `NotAuthorised and ValidationRejection remain constructible, in principle, by a future caller -- the restriction is on this Unit's own code paths, not on the type system`() {
        // Both must remain real, instantiable types (never sealed off from construction entirely), since a future
        // orchestration layer or Parker-owned validation step must be able to produce them lawfully. What Units 1-7
        // guarantee is that *their own* code never does -- verified above, structurally, not by making construction
        // impossible.
        val notAuthorised = OcrRecognitionOutcome.NotAuthorised("denied by the owner's own Permission Engine policy")
        val validationRejection = OcrRecognitionOutcome.ValidationRejection("rejected by future Parker-owned validation policy")

        assertEquals("denied by the owner's own Permission Engine policy", notAuthorised.reason)
        assertEquals("rejected by future Parker-owned validation policy", validationRejection.reason)
    }

    // -- Genuine faults propagate, never silently wrapped -------------------

    private class ThrowingFakeOcrProviderAdapter(private val fault: Throwable) : OcrProviderAdapter {
        override suspend fun recognise(request: OcrRecognitionRequest): OcrRecognitionOutcome = throw fault
    }

    @Test
    fun `a genuine unexpected fault an adapter throws still propagates unchanged through the sequencer -- never silently wrapped into GenuineImplementationFault or any other outcome`() = runTest {
        val fault = IllegalStateException("unexpected adapter crash")
        val sequencer = OcrExecutionSequencer(ThrowingFakeOcrProviderAdapter(fault))
        val request = OcrRecognitionRequest(
            sourceEvidenceId = EvidenceArtifactId("evidence-1"),
            content = byteArrayOf(1, 2, 3),
            mediaType = "image/png",
        )

        val thrown = assertFailsWith<IllegalStateException> { sequencer.recognise(request) }

        assertTrue(
            thrown === fault,
            "the exact same thrown fault must propagate unchanged -- Unit 7 adds no try/catch to the sequencer and " +
                "must not convert a genuine unexpected fault into any ordinary outcome, including the new " +
                "GenuineImplementationFault variant",
        )
    }

    @Test
    fun `a provider adapter may choose to disclose a caught fault as GenuineImplementationFault, distinct from letting it propagate`() = runTest {
        class DisclosingFakeOcrProviderAdapter : OcrProviderAdapter {
            override suspend fun recognise(request: OcrRecognitionRequest): OcrRecognitionOutcome =
                OcrRecognitionOutcome.GenuineImplementationFault("the recognition engine reported an internal fault")
        }

        val sequencer = OcrExecutionSequencer(DisclosingFakeOcrProviderAdapter())
        val request = OcrRecognitionRequest(
            sourceEvidenceId = EvidenceArtifactId("evidence-1"),
            content = byteArrayOf(1, 2, 3),
            mediaType = "image/png",
        )

        val actual = sequencer.recognise(request)

        assertTrue(actual is OcrRecognitionOutcome.GenuineImplementationFault, "expected GenuineImplementationFault, found ${actual::class.simpleName}")
        assertEquals("the recognition engine reported an internal fault", actual.reason)
    }

    // -- Non-blank reason validation ------------------------------------------

    @Test
    fun `every new Unit 7 variant rejects a blank reason`() {
        assertFailsWith<IllegalArgumentException> { OcrRecognitionOutcome.NotAuthorised("  ") }
        assertFailsWith<IllegalArgumentException> { OcrRecognitionOutcome.UnsupportedOrInaccessibleInput("") }
        assertFailsWith<IllegalArgumentException> { OcrRecognitionOutcome.NoRecognisableContent("  ") }
        assertFailsWith<IllegalArgumentException> { OcrRecognitionOutcome.PartialOrDegradedOutput(sampleResult(), "  ") }
        assertFailsWith<IllegalArgumentException> { OcrRecognitionOutcome.ValidationRejection("") }
        assertFailsWith<IllegalArgumentException> { OcrRecognitionOutcome.ProcessingOrDependencyFailure("  ") }
        assertFailsWith<IllegalArgumentException> { OcrRecognitionOutcome.GenuineImplementationFault("") }
    }

    // -- Immutable payloads ----------------------------------------------------

    @Test
    fun `every Unit 7 variant is a data class declaring only val constructor properties`() {
        val unit7Variants = listOf(
            OcrRecognitionOutcome.NotAuthorised::class,
            OcrRecognitionOutcome.UnsupportedOrInaccessibleInput::class,
            OcrRecognitionOutcome.NoRecognisableContent::class,
            OcrRecognitionOutcome.PartialOrDegradedOutput::class,
            OcrRecognitionOutcome.ValidationRejection::class,
            OcrRecognitionOutcome.ProcessingOrDependencyFailure::class,
            OcrRecognitionOutcome.GenuineImplementationFault::class,
        )

        unit7Variants.forEach { type ->
            assertTrue(type.isData, "${type.simpleName} must be a data class")
            val constructorParameterNames = type.primaryConstructor?.parameters?.map { it.name }.orEmpty().toSet()
            val mutableProperties = type.declaredMemberProperties.filter { it.name in constructorParameterNames }
                .filterIsInstance<kotlin.reflect.KMutableProperty<*>>()
            assertTrue(mutableProperties.isEmpty(), "${type.simpleName} must declare every constructor property as val, never var")
        }
    }

    // -- Provider neutrality ----------------------------------------------------

    @Test
    fun `no Unit 7 type or field name references a concrete OCR provider`() {
        val forbiddenProviderFragments = listOf("tesseract", "ocrmypdf", "paddleocr", "easyocr", "docling")
        val unit7Variants = OcrRecognitionOutcome::class.sealedSubclasses

        unit7Variants.forEach { type ->
            val namesToCheck = listOf(type.simpleName.orEmpty().lowercase()) +
                type.declaredMemberProperties.map { it.name.lowercase() }

            namesToCheck.forEach { name ->
                forbiddenProviderFragments.forEach { forbidden ->
                    assertFalse(name.contains(forbidden), "No name on ${type.simpleName} may reference a concrete provider -- found '$name' containing '$forbidden'")
                }
            }
        }
    }

    // -- No provider-specific error codes or types -------------------------------

    @Test
    fun `no Unit 7 variant carries any property beyond a String reason or an OcrRecognitionResult partial payload -- no error code, exit code, or provider-specific type`() {
        val allowedPropertyTypes = setOf(String::class.qualifiedName, OcrRecognitionResult::class.qualifiedName)
        val unit7Variants = listOf(
            OcrRecognitionOutcome.NotAuthorised::class,
            OcrRecognitionOutcome.UnsupportedOrInaccessibleInput::class,
            OcrRecognitionOutcome.NoRecognisableContent::class,
            OcrRecognitionOutcome.PartialOrDegradedOutput::class,
            OcrRecognitionOutcome.ValidationRejection::class,
            OcrRecognitionOutcome.ProcessingOrDependencyFailure::class,
            OcrRecognitionOutcome.GenuineImplementationFault::class,
        )

        unit7Variants.forEach { type ->
            type.declaredMemberProperties.forEach { property ->
                val qualifiedName = (property.returnType.classifier as? KClass<*>)?.qualifiedName
                assertTrue(
                    allowedPropertyTypes.contains(qualifiedName),
                    "${type.simpleName}.${property.name} must be a String or an OcrRecognitionResult -- no provider " +
                        "error code, exit code, stack trace, or provider-specific status type is authorised. Found: $qualifiedName",
                )
            }
        }
    }

    // -- No Permission Engine, Evidence Custodian, Memory, Knowledge, or Evidence Intelligence dependency --

    @Test
    fun `no Unit 7 variant references PermissionEngine, EvidenceCustodian, MemoryCore, KnowledgeSubmission, or EvidenceIntelligence at any depth`() {
        val excludedQualifiedNames = setOf(
            PermissionEngine::class.qualifiedName,
            EvidenceCustodian::class.qualifiedName,
            MemoryCore::class.qualifiedName,
            MemoryRetrieval::class.qualifiedName,
            KnowledgeSubmission::class.qualifiedName,
            EvidenceIntelligence::class.qualifiedName,
            OwnerEvidenceDeletionAuthority::class.qualifiedName,
        )

        val unit7Variants = OcrRecognitionOutcome::class.sealedSubclasses

        unit7Variants.forEach { type ->
            val propertyTypeNames = type.declaredMemberProperties.mapNotNull { (it.returnType.classifier as? KClass<*>)?.qualifiedName }
            val constructorTypeNames = type.primaryConstructor?.parameters?.mapNotNull { (it.type.classifier as? KClass<*>)?.qualifiedName } ?: emptyList()
            val functionTypeNames = type.declaredFunctions.flatMap { function ->
                function.parameters.mapNotNull { (it.type.classifier as? KClass<*>)?.qualifiedName } +
                    listOfNotNull((function.returnType.classifier as? KClass<*>)?.qualifiedName)
            }

            (propertyTypeNames + constructorTypeNames + functionTypeNames).forEach { qualifiedName ->
                assertFalse(
                    excludedQualifiedNames.contains(qualifiedName),
                    "${type.simpleName} must not reference '$qualifiedName' -- Scope Lock Section 13 excludes all of " +
                        "these from the OCR mechanism's own dependency graph, at any depth, including its own " +
                        "failure-representation types.",
                )
            }
        }
    }

    // -- Structural safeguards: cannot represent truth, acceptance, or reporting concepts --

    @Test
    fun `no Unit 7 variant declares a property naming a truth, acceptance, evidential-weight, or reporting concept`() {
        val forbiddenFragments = listOf(
            "truth", "reliab", "evidential", "authoritative", "trustworth", "accept", "reject_", "weight",
            "knowledgeitem", "memoryrecord", "report",
        )
        val unit7Variants = OcrRecognitionOutcome::class.sealedSubclasses

        unit7Variants.forEach { type ->
            type.declaredMemberProperties.map { it.name.lowercase() }.forEach { name ->
                forbiddenFragments.forEach { forbidden ->
                    assertFalse(
                        name.contains(forbidden),
                        "${type.simpleName} must carry no truth, acceptance, evidential-weight, or reporting-shaped " +
                            "property -- found '$name' containing '$forbidden'.",
                    )
                }
            }
        }
    }

    @Test
    fun `Units 1-6 successful-recognition behaviour is unaffected -- Recognised still carries exactly one OcrRecognitionResult`() {
        val fieldNames = OcrRecognitionOutcome.Recognised::class.declaredMemberProperties.map { it.name }.toSet()
        assertEquals(setOf("result"), fieldNames, "Recognised must remain exactly as Unit 1 defined it -- Unit 7 adds no field to it")
    }
}
