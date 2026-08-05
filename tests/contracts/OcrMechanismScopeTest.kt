package parker.core.interfaces

import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * OCR Mechanism, Implementation Unit 1 ("Provider-Neutral OCR Capability
 * Contract"). Structural scope-discipline tests only, mirroring
 * `EvidenceExtractorScopeTest.kt`'s own Kotlin-reflection discipline
 * ([KClass.declaredFunctions], never raw `java.lang.reflect`). Covers
 * contract shape, provider neutrality, prohibited dependencies,
 * structural safeguards, and constitutional boundaries only -- no
 * execution-behaviour or orchestration test belongs in this Unit
 * (Implementation Plan Unit 1's own "Do not create tests for execution
 * behaviour or orchestration" scope).
 */
class OcrMechanismScopeTest {

    // -- Contract shape --------------------------------------------------

    @Test
    fun `OcrMechanism declares exactly one operation, public, suspend, abstract, taking an OcrRecognitionRequest`() {
        val declared = OcrMechanism::class.declaredFunctions

        assertEquals(1, declared.size, "OcrMechanism must declare exactly one operation -- found: ${declared.map { it.name }}")
        val recognise = declared.single()
        assertEquals("recognise", recognise.name)
        assertEquals(KVisibility.PUBLIC, recognise.visibility, "recognise must be a public operation")
        assertTrue(recognise.isSuspend, "recognise must be a suspend function")
        assertTrue(recognise.isAbstract, "recognise must be abstract -- no default implementation on the interface itself")

        val valueParameters = recognise.parameters.filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }
        assertEquals(1, valueParameters.size, "recognise must take exactly one value parameter")
        assertEquals(OcrRecognitionRequest::class, valueParameters.single().type.classifier)
    }

    @Test
    fun `OcrMechanism declares no constructor, property, or dependency of its own`() {
        assertEquals(0, OcrMechanism::class.declaredMemberProperties.size)
    }

    @Test
    fun `OcrRecognitionOutcome has exactly two top-level variants -- Recognised and Failed, structurally distinguishable`() {
        val subclasses = OcrRecognitionOutcome::class.sealedSubclasses

        assertEquals(
            setOf("Recognised", "Failed"),
            subclasses.map { it.simpleName }.toSet(),
            "OcrRecognitionOutcome must have exactly Recognised and Failed -- Scope Lock Section 10's own seven " +
                "constitutional distinctions are Implementation Plan Unit 7's own, later, concrete-representation " +
                "responsibility. Neither an enum, a code, a category-name field, nor a per-kind subclass may be " +
                "introduced by this Unit -- found: ${subclasses.map { it.simpleName }}",
        )

        val recognised = OcrRecognitionOutcome.Recognised(
            OcrRecognitionResult(
                recognisedText = "sample",
                fidelity = TranscriptionFidelity.VERBATIM,
                identity = OcrRecognitionIdentity(mechanismIdentity = "id", configurationProfile = "profile"),
                recognisedAt = java.time.Instant.EPOCH,
            ),
        )
        val failed = OcrRecognitionOutcome.Failed(reason = "unreadable")
        val outcomes: List<OcrRecognitionOutcome> = listOf(recognised, failed)
        assertFalse(
            outcomes[0]::class == outcomes[1]::class,
            "Recognised and Failed must be structurally distinct types",
        )
    }

    @Test
    fun `Failed carries exactly one property -- reason -- no failure kind, code, or category of any shape`() {
        val fieldNames = OcrRecognitionOutcome.Failed::class.declaredMemberProperties.map { it.name }.toSet()

        assertEquals(
            setOf("reason"),
            fieldNames,
            "OcrRecognitionOutcome.Failed must carry only 'reason' -- no 'kind', 'code', 'category', or any other " +
                "taxonomy-shaped field. Scope Lock Section 10 leaves the concrete taxonomy, naming, and " +
                "representation of its seven distinctions to Implementation Plan Unit 7, not to this Unit -- " +
                "found: $fieldNames",
        )
    }

    @Test
    fun `none of Scope Lock Section 10's seven distinction labels appears anywhere in the public outcome contract`() {
        val forbiddenLabelFragments = listOf(
            "notauthorised", "unsupported", "inaccessible", "norecognisable", "partial", "degraded",
            "validationrejected", "processingordependency", "implementationfault", "kind", "code", "category",
        )

        val outcomeTypes = listOf(
            OcrRecognitionOutcome::class,
            OcrRecognitionOutcome.Recognised::class,
            OcrRecognitionOutcome.Failed::class,
        )
        val allNames = outcomeTypes.flatMap { type ->
            listOf(type.simpleName?.lowercase().orEmpty()) +
                type.declaredMemberProperties.map { it.name.lowercase() }
        }

        allNames.forEach { name ->
            forbiddenLabelFragments.forEach { forbidden ->
                assertFalse(
                    name.contains(forbidden),
                    "No type or field name in OcrRecognitionOutcome's own public contract may name one of Scope " +
                        "Lock Section 10's seven distinctions, a taxonomy code, or a category -- found '$name' " +
                        "containing '$forbidden'. That representation remains Implementation Plan Unit 7's own, " +
                        "later responsibility.",
                )
            }
        }
    }

    @Test
    fun `Failed discloses a non-blank technical reason, and rejects a blank one`() {
        val failed = OcrRecognitionOutcome.Failed(reason = "the supplied content could not be decoded")

        assertEquals("the supplied content could not be decoded", failed.reason)
        assertFailsWith<IllegalArgumentException> {
            OcrRecognitionOutcome.Failed(reason = "  ")
        }
    }

    @Test
    fun `TranscriptionFidelity has exactly three values, matching Evidence Intelligence Contract Design Section 5`() {
        assertEquals(
            listOf("VERBATIM", "NORMALISED", "INFERRED_RECONSTRUCTION"),
            TranscriptionFidelity.entries.map { it.name },
        )
    }

    @Test
    fun `OcrRecognitionIdentity carries exactly mechanismIdentity, configurationProfile, and mechanismVersion`() {
        val fieldNames = OcrRecognitionIdentity::class.declaredMemberProperties.map { it.name }.toSet()

        assertEquals(setOf("mechanismIdentity", "configurationProfile", "mechanismVersion"), fieldNames)
    }

    // -- Constitutional boundaries: no requesting principal, no caller-declared confidence/evidential-state input --

    @Test
    fun `OcrRecognitionRequest carries no requesting-principal field and no caller-declared confidence or evidential-state field`() {
        val fieldNames = OcrRecognitionRequest::class.declaredMemberProperties.map { it.name.lowercase() }
        val forbiddenFragments = listOf("principal", "confidence", "evidentialstate", "evidential_state")

        fieldNames.forEach { fieldName ->
            forbiddenFragments.forEach { forbidden ->
                assertFalse(
                    fieldName.contains(forbidden),
                    "OcrRecognitionRequest must carry no requesting-principal or caller-declared confidence/evidential-state " +
                        "field (Scope Lock Section 5) -- found field name '$fieldName' containing '$forbidden'.",
                )
            }
        }
    }

    @Test
    fun `OcrRecognitionResult confidence is rejected outside the closed unit interval`() {
        assertFailsWith<IllegalArgumentException> {
            OcrRecognitionResult(
                recognisedText = "sample",
                fidelity = TranscriptionFidelity.VERBATIM,
                identity = OcrRecognitionIdentity(mechanismIdentity = "id", configurationProfile = "profile"),
                confidence = 1.5,
                recognisedAt = java.time.Instant.EPOCH,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            OcrRecognitionResult(
                recognisedText = "sample",
                fidelity = TranscriptionFidelity.VERBATIM,
                identity = OcrRecognitionIdentity(mechanismIdentity = "id", configurationProfile = "profile"),
                confidence = -0.1,
                recognisedAt = java.time.Instant.EPOCH,
            )
        }
    }

    @Test
    fun `blank-string fields are rejected across every shape this Unit defines`() {
        assertFailsWith<IllegalArgumentException> {
            OcrRecognitionRequest(sourceEvidenceId = EvidenceArtifactId("id"), content = ByteArray(1), mediaType = "  ")
        }
        assertFailsWith<IllegalArgumentException> {
            OcrRecognitionIdentity(mechanismIdentity = "  ", configurationProfile = "profile")
        }
        assertFailsWith<IllegalArgumentException> {
            OcrRecognitionIdentity(mechanismIdentity = "id", configurationProfile = "  ")
        }
        assertFailsWith<IllegalArgumentException> {
            OcrRecognitionResult(
                recognisedText = "  ",
                fidelity = TranscriptionFidelity.VERBATIM,
                identity = OcrRecognitionIdentity(mechanismIdentity = "id", configurationProfile = "profile"),
                recognisedAt = java.time.Instant.EPOCH,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            OcrRecognitionOutcome.Failed(reason = "  ")
        }
    }

    // -- OcrRecognitionRequest's own ByteArray-safe equality --

    @Test
    fun `OcrRecognitionRequest compares content structurally, not by reference`() {
        val first = OcrRecognitionRequest(EvidenceArtifactId("id"), byteArrayOf(1, 2, 3), "application/pdf")
        val second = OcrRecognitionRequest(EvidenceArtifactId("id"), byteArrayOf(1, 2, 3), "application/pdf")

        assertEquals(first, second, "OcrRecognitionRequest must compare ByteArray content structurally")
        assertEquals(first.hashCode(), second.hashCode())
    }

    // -- Prohibited dependencies (Scope Lock Section 13) ------------------

    @Test
    fun `no type reachable from OcrMechanism's own public shapes references EvidenceCustodian, MemoryCore, KnowledgeSubmission, PermissionEngine, or EvidenceIntelligence`() {
        val excludedQualifiedNames = setOf(
            EvidenceCustodian::class.qualifiedName,
            MemoryCore::class.qualifiedName,
            MemoryRetrieval::class.qualifiedName,
            KnowledgeSubmission::class.qualifiedName,
            PermissionEngine::class.qualifiedName,
            EvidenceIntelligence::class.qualifiedName,
            OwnerEvidenceDeletionAuthority::class.qualifiedName,
        )

        val typesToCheck: List<KClass<*>> = listOf(
            OcrMechanism::class,
            OcrRecognitionRequest::class,
            OcrRecognitionIdentity::class,
            OcrRecognitionResult::class,
            OcrRecognitionOutcome::class,
        ) + OcrRecognitionOutcome::class.sealedSubclasses

        typesToCheck.forEach { type ->
            val propertyTypeNames = type.declaredMemberProperties.flatMap { property ->
                listOf(property.returnType.classifier?.let { (it as? KClass<*>)?.qualifiedName }) +
                    property.returnType.arguments.mapNotNull { arg -> (arg.type?.classifier as? KClass<*>)?.qualifiedName }
            }
            val functionTypeNames = type.declaredFunctions.flatMap { function ->
                function.parameters.mapNotNull { (it.type.classifier as? KClass<*>)?.qualifiedName } +
                    listOfNotNull((function.returnType.classifier as? KClass<*>)?.qualifiedName)
            }
            val constructorTypeNames = type.primaryConstructor?.parameters?.mapNotNull {
                (it.type.classifier as? KClass<*>)?.qualifiedName
            } ?: emptyList()

            (propertyTypeNames + functionTypeNames + constructorTypeNames)
                .filterNotNull()
                .forEach { qualifiedName ->
                    assertFalse(
                        excludedQualifiedNames.contains(qualifiedName),
                        "${type.simpleName} must not reference '$qualifiedName' -- Scope Lock Section 13 excludes " +
                            "EvidenceCustodian, MemoryCore, KnowledgeSubmission, the Permission Engine, EvidenceIntelligence, " +
                            "and OwnerEvidenceDeletionAuthority from the OCR mechanism's own dependency graph, at any depth.",
                    )
                }
        }
    }

    @Test
    fun `only EvidenceArtifactId, among Evidence Custodian's own types, is referenced -- never CandidateEvidenceArtifact or any custody type`() {
        val custodyOnlyTypeNames = setOf(
            CandidateEvidenceArtifact::class.qualifiedName,
            AcceptedEvidenceArtifact::class.qualifiedName,
            EvidenceAcceptanceResult::class.qualifiedName,
            EvidenceRetrievalResult::class.qualifiedName,
            EvidenceDeletionResult::class.qualifiedName,
        )

        val typesToCheck: List<KClass<*>> = listOf(
            OcrMechanism::class,
            OcrRecognitionRequest::class,
            OcrRecognitionResult::class,
            OcrRecognitionOutcome::class,
        ) + OcrRecognitionOutcome::class.sealedSubclasses

        typesToCheck.forEach { type ->
            val allTypeNames = (
                type.declaredMemberProperties.map { it.returnType.classifier?.let { c -> (c as? KClass<*>)?.qualifiedName } } +
                    (type.primaryConstructor?.parameters?.map { (it.type.classifier as? KClass<*>)?.qualifiedName } ?: emptyList())
                ).filterNotNull()

            allTypeNames.forEach { qualifiedName ->
                assertFalse(
                    custodyOnlyTypeNames.contains(qualifiedName),
                    "${type.simpleName} must not reference '$qualifiedName' -- only EvidenceArtifactId, an identifier " +
                        "value, is reused (Contract Design Section 13); custody-shaped types remain Evidence Custodian's own.",
                )
            }
        }
    }

    // -- Provider neutrality (Scope Lock Section 14) ----------------------

    @Test
    fun `no field name across this Unit's own shapes names a concrete OCR provider`() {
        val forbiddenProviderFragments = listOf("tesseract", "ocrmypdf", "paddleocr", "easyocr", "docling")

        val allFieldNames = listOf(
            OcrRecognitionRequest::class,
            OcrRecognitionIdentity::class,
            OcrRecognitionResult::class,
        ).flatMap { it.declaredMemberProperties.map { property -> property.name.lowercase() } } +
            OcrRecognitionOutcome::class.sealedSubclasses.flatMap { subclass ->
                subclass.declaredMemberProperties.map { it.name.lowercase() }
            }

        allFieldNames.forEach { fieldName ->
            forbiddenProviderFragments.forEach { forbidden ->
                assertFalse(
                    fieldName.contains(forbidden),
                    "No field name may reference a concrete provider -- found '$fieldName' containing '$forbidden' " +
                        "(Scope Lock Section 14: provider neutrality).",
                )
            }
        }
    }

    @Test
    fun `no file in src imports a concrete OCR provider package -- a prose non-example in a KDoc comment is not a leak`() {
        val srcRoot = java.io.File("src")
        check(srcRoot.exists()) { "src/ directory not found from working directory ${java.io.File(".").absolutePath}" }
        val forbiddenProviderNames = listOf("Tesseract", "OCRmyPDF", "PaddleOCR", "EasyOCR")

        val offendingFiles = srcRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file ->
                file.readLines().any { line ->
                    line.trimStart().startsWith("import ") && forbiddenProviderNames.any { line.contains(it) }
                }
            }
            .map { it.path }
            .toList()

        assertTrue(
            offendingFiles.isEmpty(),
            "No file in src/ may import a concrete OCR provider package before a governed adapter (Implementation Plan " +
                "Unit 2) exists to confine it -- found a provider-package import in: $offendingFiles",
        )
    }
}
