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
    compileClasspath += sourceSets.main.get().output
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

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        allWarningsAsErrors.set(false)
    }
}

tasks.test {
    useJUnitPlatform()
}
