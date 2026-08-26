import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.24"
    application
}

group = "parker"
version = "0.8.0-runtime-complete"

// Launcher (production OS process entry point, src/composition/Main.kt):
// the built-in `application` plugin, no new external dependency, gives
// `./gradlew run`/`installDist` -- the latter is what Dockerfile builds
// against. mainClass names the top-level `fun main()` Kotlin compiles to
// `parker.composition.MainKt`.
application {
    mainClass.set("parker.composition.MainKt")
    applicationName = "parker"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // Evidence Processing (Searchable PDF), Implementation Unit 2 ("Apache Tika Adapter").
    // Verified against Maven Central during the Implementation Plan's own planning pass
    // (docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_IMPLEMENTATION_PLAN.md, Section 5) --
    // this repository's first third-party (non-kotlinx) production dependency. The scoped
    // PDF-only parser module, not the full tika-parsers-standard-package bundle, and deliberately
    // excluding tika-parser-ocr-module (never added, anywhere) -- OCR is structurally
    // unreachable, not merely unconfigured (Boundary Clarification Determination 2).
    implementation("org.apache.tika:tika-core:3.3.1")
    implementation("org.apache.tika:tika-parser-pdf-module:3.3.1")
    implementation("org.apache.commons:commons-csv:1.14.1")
    implementation("org.apache.james:apache-mime4j-core:0.8.14")
    implementation("org.apache.james:apache-mime4j-dom:0.8.14")
    implementation("org.apache.poi:poi-ooxml:5.5.1")

    testImplementation(kotlin("test-junit5"))
    testImplementation(kotlin("reflect"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

kotlin {
    jvmToolchain(17)
}

// Phase 1 compiled only src/contracts + src/interfaces, excluding eight
// stub files whose referenced types (EventBus's ParkerEvent/EventType/etc.,
// Agent's AgentHealth, MemoryStore's Memory/MemoryQuery/etc.) didn't exist
// yet. Phase 2 (v0.7 Architecture Completion Phase follow-on) specifies
// and implements EventBus's supporting types, so EventBus.kt is back in
// the build. Sprint 4, Track A, Unit A3 specifies and implements
// MemoryStore's supporting types (docs/architecture/MEMORY_RUNTIME_ARCHITECTURE.md,
// docs/architecture/MEMORY_CONTRACT_DESIGN.md), so MemoryStore.kt is back
// in the build too. Sprint 4, Track B, Unit B3 specifies and implements
// WorldModel's supporting types (docs/architecture/WORLD_MODEL_RUNTIME_ARCHITECTURE.md,
// docs/architecture/WORLD_MODEL_CONTRACT_DESIGN.md), so WorldModel.kt is
// back in the build too. Agent/AuditService/ModelManager/NotificationService/
// Plugin remain excluded -- their supporting types are still unspecified
// (AgentHealth is a known, recorded gap; the rest are entirely out of
// this phase's scope). src/runtime holds Phase 2's concrete
// implementations (ToolRegistry, ActionMapper, EventBus), kept separate
// from src/contracts (data types) and src/interfaces (Volume 3 interface
// stubs) per the existing two-directory convention. See
// docs/architecture/IMPLEMENTATION_GAPS.md.
//
// Sprint 10, Unit 4 (Production Composition Root) adds a fourth source
// directory, src/composition (package parker.composition), deliberately
// kept separate from src/runtime: the composition root constructs and
// wires the runtime graph -- it is not itself a runtime component, holds
// no domain responsibility, and per this Unit's own governing instruction
// must not be mistaken for one of the frozen coordinators src/runtime
// already contains. tests/composition mirrors it, matching the existing
// tests/contracts + tests/runtime convention.
sourceSets {
    main {
        kotlin {
            srcDirs("src/contracts", "src/interfaces", "src/runtime", "src/composition", "src/ui")
            exclude(
                "Agent.kt",
                "AuditService.kt",
                "ModelManager.kt",
                "NotificationService.kt",
                "Plugin.kt",
            )
        }
    }
    test {
        kotlin {
            srcDirs("tests/contracts", "tests/runtime", "tests/composition", "tests/ui")
        }
    }
}

// Reasoning Protocol Live-Model Conformance, Unit 1: an explicit,
// detached evaluation source set. It is deliberately not attached to
// test/check/build/assemble (or any other lifecycle task), so ordinary
// repository verification remains offline and deterministic.
val liveModelEvaluation by sourceSets.creating {
    kotlin.srcDir("tests/integration")
    compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    runtimeClasspath += output + compileClasspath
}

configurations[liveModelEvaluation.implementationConfigurationName].extendsFrom(
    configurations.testImplementation.get(),
)
configurations[liveModelEvaluation.runtimeOnlyConfigurationName].extendsFrom(
    configurations.testRuntimeOnly.get(),
)

tasks.register<Test>("reasoningProtocolLiveModelEvaluation") {
    description = "Runs the explicit opt-in Reasoning Protocol live-model evaluation instrument"
    group = "verification"
    testClassesDirs = liveModelEvaluation.output.classesDirs
    classpath = liveModelEvaluation.runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter(tasks.test)
}

// Unit N — explicitly opt-in, one-request synthetic OpenAI acceptance. This remains in the
// detached liveModelEvaluation source set and is never attached to test/check/build or the Unit M
// offline gate. The test itself requires a second explicit system-property gate and fails before
// transport construction when profile, credential, or result-path prerequisites are absent.
tasks.register<Test>("externalTranscriptionLiveAcceptance") {
    description = "Runs the one-request synthetic external-transcription live acceptance instrument"
    group = "verification"
    testClassesDirs = liveModelEvaluation.output.classesDirs
    classpath = liveModelEvaluation.runtimeClasspath
    useJUnitPlatform()
    filter {
        includeTestsMatching("parker.core.runtime.ExternalTranscriptionLiveAcceptanceTest")
    }
    systemProperty("parker.externalTranscription.live.enabled", "true")
    shouldRunAfter(tasks.test)
}

// Unit O.2 — detached, offline, synthetic-only acceptance-method verification. This task is not
// attached to test/check/build and carries no live opt-in, credential, network transport, or real
// Parker storage configuration. It freezes the Unit O worksheet and decision semantics before
// owner document selection.
tasks.register<Test>("unitOOfflineAcceptanceVerification") {
    description = "Runs the detached synthetic Unit O acceptance-method verification"
    group = "verification"
    testClassesDirs = liveModelEvaluation.output.classesDirs
    classpath = liveModelEvaluation.runtimeClasspath
    useJUnitPlatform()
    filter {
        includeTestsMatching("parker.core.runtime.UnitOOfflineAcceptanceInstrumentTest")
        includeTestsMatching("parker.core.runtime.UnitOMetadataPreflightTest")
    }
    shouldRunAfter(tasks.test)
}

// Unit O.3 — explicitly detached, metadata-only, exact-ID deployed preflight. It constructs no
// provider transport and has no source-byte, OCR, derivative, or analysis dependency.
tasks.register<Test>("unitOMetadataPreflight") {
    description = "Runs the exact-ID Unit O.3 authoritative manifest metadata-only preflight"
    group = "verification"
    testClassesDirs = liveModelEvaluation.output.classesDirs
    classpath = liveModelEvaluation.runtimeClasspath
    useJUnitPlatform()
    filter { includeTestsMatching("parker.core.runtime.UnitOMetadataPreflightAcceptanceTest") }
    systemProperty("parker.unitO.metadataPreflight.enabled", "true")
    shouldRunAfter(tasks.test)
}

tasks.register<Test>("reasoningProtocolBaselineCharacterisation") {
    description = "Runs the explicit opt-in Reasoning Protocol Unit 2 baseline characterisation instrument"
    group = "verification"
    testClassesDirs = liveModelEvaluation.output.classesDirs
    classpath = liveModelEvaluation.runtimeClasspath
    useJUnitPlatform()
    filter {
        includeTestsMatching("parker.integration.ReasoningProtocolBaselineCharacterisationTest")
    }
    systemProperty("parker.reasoning.baseline.enabled", "true")
    shouldRunAfter(tasks.test)
}

tasks.register<Test>("reasoningProtocolUnit2DDiagnostic") {
    description = "Runs the explicit opt-in Reasoning Protocol Unit 2-D diagnostic characterisation instrument"
    group = "verification"
    testClassesDirs = liveModelEvaluation.output.classesDirs
    classpath = liveModelEvaluation.runtimeClasspath
    useJUnitPlatform()
    filter {
        includeTestsMatching("parker.integration.ReasoningProtocolDiagnosticCharacterisationTest")
    }
    systemProperty("parker.reasoning.diagnostic.enabled", "true")
    shouldRunAfter(tasks.test)
}

tasks.register<Test>("unit3cControlledRemedyExperiments") {
    description = "Runs the explicit opt-in Reasoning Protocol Unit 3-C controlled remedy experiments instrument"
    group = "verification"
    testClassesDirs = liveModelEvaluation.output.classesDirs
    classpath = liveModelEvaluation.runtimeClasspath
    // "unit3cLiveTaskIncompatible" tags tests whose assertion is valid only
    // under ordinary/offline verification (e.g. asserting the live campaign
    // environment variable is absent) and would necessarily fail during a
    // genuine live run, where that variable is intentionally present. This
    // task excludes them; the general offline live-model-evaluation task
    // above and every other offline path still runs them.
    useJUnitPlatform {
        excludeTags("unit3cLiveTaskIncompatible")
    }
    filter {
        includeTestsMatching("parker.integration.ReasoningProtocolUnit3CControlledRemedyExperimentsTest")
        includeTestsMatching("parker.integration.ReasoningProtocolUnit3COrchestrationTest")
    }
    systemProperty("parker.reasoning.unit3c.enabled", "true")
    shouldRunAfter(tasks.test)
}

tasks.register<Test>("reasoningProtocolFamilyFDiagnostic") {
    description = "Runs the explicit opt-in Reasoning Protocol Unit 3-BF Family F alternative-model diagnostic instrument"
    group = "verification"
    testClassesDirs = liveModelEvaluation.output.classesDirs
    classpath = liveModelEvaluation.runtimeClasspath
    // "familyFLiveTaskIncompatible" tags tests whose assertion is valid only
    // under ordinary/offline verification (e.g. asserting the live execution-
    // approval environment value is absent) and would necessarily fail under
    // a genuine live run, where that value is intentionally present. This
    // task excludes them; the general offline live-model-evaluation task
    // above and every other offline path still runs them.
    useJUnitPlatform {
        excludeTags("familyFLiveTaskIncompatible")
    }
    filter {
        includeTestsMatching("parker.integration.ReasoningProtocolFamilyFDiagnosticTest")
        includeTestsMatching("parker.integration.ReasoningProtocolFamilyFDiagnosticOrchestrationTest")
    }
    systemProperty("parker.reasoning.familyf.enabled", "true")
    shouldRunAfter(tasks.test)
}

tasks.register<Test>("reasoningProtocolFamilyFBoundingEvidence") {
    description = "Runs the explicit opt-in Reasoning Protocol Unit 3-BF Family F bounding-evidence offline estimator instrument"
    group = "verification"
    testClassesDirs = liveModelEvaluation.output.classesDirs
    classpath = liveModelEvaluation.runtimeClasspath
    useJUnitPlatform()
    filter {
        includeTestsMatching("parker.integration.ReasoningProtocolFamilyFBoundingEvidenceTest")
    }
    systemProperty("parker.reasoning.familyf.boundingEvidence.enabled", "true")
    shouldRunAfter(tasks.test)
}

tasks.register<Test>("qmdRelevanceMechanismLiveAcceptance") {
    description = "Runs the explicit opt-in Programme 3 Unit 9.7.3 live QMD relevance mechanism acceptance instrument"
    group = "verification"
    testClassesDirs = liveModelEvaluation.output.classesDirs
    classpath = liveModelEvaluation.runtimeClasspath
    useJUnitPlatform()
    filter {
        includeTestsMatching("parker.integration.QmdRelevanceMechanismLiveAcceptanceTest")
    }
    systemProperty("parker.relevance.qmd.live.enabled", "true")
    shouldRunAfter(tasks.test)
}

// OCR Mechanism, Docling Concrete Adapter -- this Unit's own opt-in live acceptance
// instrument, mirroring qmdRelevanceMechanismLiveAcceptance's own identical shape.
// DOCLING_TEST_PYTHON (and optionally DOCLING_TEST_BRIDGE_SCRIPT/DOCLING_TEST_MODEL_CACHE_DIR)
// must be set in the environment this Gradle invocation itself runs in -- never
// auto-discovered, guessed, or downloaded by this task.
tasks.register<Test>("doclingOcrProviderAdapterLiveAcceptance") {
    description = "Runs the explicit opt-in OCR Mechanism Docling concrete adapter live acceptance instrument"
    group = "verification"
    testClassesDirs = liveModelEvaluation.output.classesDirs
    classpath = liveModelEvaluation.runtimeClasspath
    useJUnitPlatform()
    filter {
        includeTestsMatching("parker.integration.DoclingOcrProviderAdapterLiveAcceptanceTest")
    }
    systemProperty("parker.ocr.docling.live.enabled", "true")
    shouldRunAfter(tasks.test)
}

// OCR Mechanism, Unit 12 ("Runtime Composition") -- this Unit's own opt-in live composition
// acceptance instrument, mirroring doclingOcrProviderAdapterLiveAcceptance's own identical shape,
// but through the real, unmodified ParkerRuntime.analyseEvidence entry point rather than
// DoclingOcrProviderAdapter directly. Same environment variables, same live-provisioning gate.
tasks.register<Test>("ocrMechanismUnit12CompositionLiveAcceptance") {
    description = "Runs the explicit opt-in OCR Mechanism Unit 12 runtime composition live acceptance instrument"
    group = "verification"
    testClassesDirs = liveModelEvaluation.output.classesDirs
    classpath = liveModelEvaluation.runtimeClasspath
    useJUnitPlatform()
    filter {
        includeTestsMatching("parker.integration.OcrMechanismUnit12CompositionLiveAcceptanceTest")
    }
    systemProperty("parker.ocr.docling.live.enabled", "true")
    shouldRunAfter(tasks.test)
}

// OpenAI External Transcription Implementation Plan, Unit M — the single auditable,
// credential-free and network-independent offline gate. It deliberately uses only the ordinary
// test source set; tests/integration (liveModelEvaluation) is structurally absent. Every included
// provider test injects a fake transport, while the production-composition proofs assert that the
// real adapter remains uncomposed and disabled.
tasks.register<Test>("externalTranscriptionOfflineVerification") {
    description = "Runs the governed external-transcription offline verification matrix (Units A-M)"
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform()
    filter {
        includeTestsMatching("parker.composition.ExternalTranscriptionOfflineVerificationTest")
        includeTestsMatching("parker.composition.OpenAi*")
        includeTestsMatching("parker.composition.OwnerEvidenceHttpServerTest")
        includeTestsMatching("parker.composition.OwnerUiEvidenceRuntimeAdapterTest")
        includeTestsMatching("parker.composition.ParkerRuntimeOcrCompositionTest")
        includeTestsMatching("parker.composition.ParkerRuntimeTierBOcrDurableGenerationTest")
        includeTestsMatching("parker.composition.DerivativeContentPersistenceRestartAcceptanceTest")
        includeTestsMatching("parker.core.interfaces.Ocr*")
        includeTestsMatching("parker.core.runtime.AuthorizationPurpose*")
        includeTestsMatching("parker.core.runtime.ExternalTranscription*")
        includeTestsMatching("parker.core.runtime.OpenAi*")
        includeTestsMatching("parker.core.runtime.OcrProcessingRepresentationFactoryTest")
        includeTestsMatching("parker.core.runtime.OcrStructuredResultValidatorTest")
        includeTestsMatching("parker.core.runtime.DerivativeContentCodecTest")
        includeTestsMatching("parker.core.runtime.TierBOcr*")
        includeTestsMatching("parker.core.runtime.DocumentAnalysis*")
        includeTestsMatching("parker.core.runtime.SavedAnalysisCoordinatorTest")
        includeTestsMatching("parker.core.runtime.DoclingOcrProviderAdapterTest")
    }
    shouldRunAfter(tasks.test)
}

// Document Ingestion Programme, Tier B Owner Routing -- this Unit's own opt-in live acceptance
// instrument, mirroring ocrMechanismUnit12CompositionLiveAcceptance's own identical shape, but
// through the real, complete, explicit owner workflow (importEvidenceFileAsOwner ->
// invokeTierAIngestionAsOwner -> analyseEvidence) rather than submitEvidence + analyseEvidence alone.
tasks.register<Test>("tierBOwnerRoutingLiveAcceptance") {
    description = "Runs the explicit opt-in Document Ingestion Tier B owner routing live acceptance instrument"
    group = "verification"
    testClassesDirs = liveModelEvaluation.output.classesDirs
    classpath = liveModelEvaluation.runtimeClasspath
    useJUnitPlatform()
    filter {
        includeTestsMatching("parker.integration.TierBOwnerRoutingLiveAcceptanceTest")
    }
    systemProperty("parker.ocr.docling.live.enabled", "true")
    shouldRunAfter(tasks.test)
}

// Owner UI Evidence Upload & Processing -- this Unit's own opt-in live acceptance instrument,
// mirroring tierBOwnerRoutingLiveAcceptance's own identical shape, but driving the real
// OwnerEvidenceUiController/OwnerUiEvidenceRuntimeAdapter pair the Compose desktop UI itself uses.
tasks.register<Test>("ownerEvidenceUiEndToEndLiveAcceptance") {
    description = "Runs the explicit opt-in Owner UI Evidence Upload & Processing end-to-end live acceptance instrument"
    group = "verification"
    testClassesDirs = liveModelEvaluation.output.classesDirs
    classpath = liveModelEvaluation.runtimeClasspath
    useJUnitPlatform()
    filter {
        includeTestsMatching("parker.integration.OwnerEvidenceUiEndToEndLiveAcceptanceTest")
    }
    systemProperty("parker.ocr.docling.live.enabled", "true")
    shouldRunAfter(tasks.test)
}

// Owner LAN Evidence Upload -- this Unit's own opt-in live acceptance instrument, mirroring
// ownerEvidenceUiEndToEndLiveAcceptance's own identical shape, but driving the real
// OwnerEvidenceHttpServer over real HTTP (java.net.http.HttpClient) rather than in-process calls,
// standing in for the Windows-laptop-side browser this Unit's own governing task describes.
tasks.register<Test>("ownerEvidenceHttpEndToEndLiveAcceptance") {
    description = "Runs the explicit opt-in Owner LAN Evidence Upload end-to-end live acceptance instrument (real HTTP transport, real Docling)"
    group = "verification"
    testClassesDirs = liveModelEvaluation.output.classesDirs
    classpath = liveModelEvaluation.runtimeClasspath
    useJUnitPlatform()
    filter {
        includeTestsMatching("parker.integration.OwnerEvidenceHttpEndToEndLiveAcceptanceTest")
    }
    systemProperty("parker.ocr.docling.live.enabled", "true")
    shouldRunAfter(tasks.test)
}

// Main-Promotion Gate / QMD Linux Portability Defect correction (Programme 3
// Unit 9.7.6 follow-on, bounded investigation and correction). Root cause:
// `QmdCanonicalMemoryRetrievalExperimentTest.kt` (commit `aadd596`) is a
// disposable, historical Section 13/13.1 mechanism-selection precursor --
// explicitly self-documented "Experimental composition seam only" -- that
// invokes a real QMD subprocess via `qmd-authorized-vector-bridge.mts`
// against a hardcoded, Steve's-machine-specific Windows path
// (`C:\Projects\Parker\qmd\node_modules\tsx\dist\cli.mjs`, and a
// `file:///C:/Projects/Parker/qmd/src/store.ts` import inside that bridge
// script itself). It was never designed for portable execution: even a
// corrected, configurable path would still fail on Linux, because the
// underlying QMD `createStore`/`searchVector` API it exercises requires a
// native `sqlite-vec` binary this repository's own Section 13 evidence
// record (`docs/reviews/PROGRAMME_3_UNIT_9_7_SECTION_13_MECHANISM_SELECTION_SPIKE_EVIDENCE_RECORD.md`,
// Section 5) already discloses has no resolved Linux build in this
// environment's `node_modules` tree. Its positive-ranking evidence
// (Property 1) has since been reproduced in fully portable, in-process form
// by the adopted spike (`tests/contracts/RelevanceMechanismSpikeQmdCandidateTest.kt`,
// over the same captured real embedding vectors, no subprocess); the
// production adapter (`src/runtime/QmdRelevanceMechanism.kt` /
// `tools/qmd-relevance-bridge.mts`) deliberately does not use the
// store/searchVector approach this historical class exercises, precisely to
// avoid that same native-binary gap (see that production bridge script's
// own header comment). The class remains fully intact, at its original
// path, still compiled as part of the ordinary `test` source set (so every
// existing relative-path reference, package, and internal-visibility
// relationship to `QmdRealEmbeddingFixtures` is unaffected) -- it is only
// excluded from ordinary execution below, and remains separately runnable,
// on Steve's own Windows development machine only, via the explicit opt-in
// task immediately below.
tasks.register<Test>("qmdCanonicalMemoryRetrievalExperimentEvidence") {
    description = "Runs the explicit opt-in, historical Programme 3 Unit 9.7 Section 13 mechanism-selection precursor evidence instrument (QmdCanonicalMemoryRetrievalExperimentTest), Windows development machine only -- requires a sibling C:\\Projects\\Parker\\qmd checkout"
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform()
    filter {
        includeTestsMatching("parker.composition.QmdCanonicalMemoryRetrievalExperimentTest")
    }
    shouldRunAfter(tasks.test)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        allWarningsAsErrors.set(false)
    }
}

tasks.test {
    useJUnitPlatform()
    // See the historical-experiment correction comment above
    // `qmdCanonicalMemoryRetrievalExperimentEvidence`: excluded here so
    // ordinary `test` is platform-portable and does not depend on Steve's
    // Windows QMD checkout. Still compiled; still separately executable via
    // that explicit opt-in task.
    exclude("**/QmdCanonicalMemoryRetrievalExperimentTest.class")
}
