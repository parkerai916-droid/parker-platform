import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.24"
}

group = "parker"
version = "0.8.0-runtime-complete"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

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
sourceSets {
    main {
        kotlin {
            srcDirs("src/contracts", "src/interfaces", "src/runtime")
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
            srcDirs("tests/contracts", "tests/runtime")
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        allWarningsAsErrors.set(false)
    }
}

tasks.test {
    useJUnitPlatform()
}
