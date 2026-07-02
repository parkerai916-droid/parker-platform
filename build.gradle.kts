import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.24"
}

group = "parker"
version = "0.6.0-alpha1"

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

// This build deliberately compiles only the Phase 1 (Volume 1 Core
// Contracts) slice of src/interfaces. Eight stub files reference types
// that belong to later phases (EventBus's ParkerEvent/EventType/etc.,
// Agent's AgentHealth, MemoryStore's Memory/MemoryQuery/etc., and so on)
// and are excluded here so the module actually builds. Nothing on disk is
// deleted or rewritten -- this is a build-scope decision, reversible by
// editing this file once those phases are specified. See
// docs/architecture/IMPLEMENTATION_GAPS.md and phase1-assessment.md.
sourceSets {
    main {
        kotlin {
            srcDirs("src/contracts", "src/interfaces")
            exclude(
                "Agent.kt",
                "AuditService.kt",
                "EventBus.kt",
                "MemoryStore.kt",
                "ModelManager.kt",
                "NotificationService.kt",
                "Plugin.kt",
                "WorldModel.kt",
            )
        }
    }
    test {
        kotlin {
            srcDirs("tests/contracts")
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
