plugins {
    kotlin("jvm") version "1.9.24"
    id("org.jetbrains.compose") version "1.6.11"
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(project(":"))
    implementation(compose.desktop.currentOs)
}

kotlin {
    jvmToolchain(17)
}

// Explicit, offline-only graphical development path. No task in the root
// runtime project depends on this launcher, and no runtime mode exists here.
tasks.register<JavaExec>("runOfflineOwnerUi") {
    description = "Runs Parker's deterministic offline owner UI"
    group = "application"
    mainClass.set("parker.ui.OfflineOwnerUiMainKt")
    classpath = sourceSets.main.get().runtimeClasspath
}

// Explicit real-runtime graphical path. Unit 5 compiles and structurally
// verifies this task but does not execute it while the live gate is active.
tasks.register<JavaExec>("runOwnerUi") {
    description = "Runs Parker's explicitly configured real-runtime owner UI"
    group = "application"
    mainClass.set("parker.ui.OwnerUiMainKt")
    classpath = sourceSets.main.get().runtimeClasspath
    // Gradle's own JavaExec default working directory is this task's declaring subproject
    // (ui-desktop/), not the repository root -- unlike the root project's own `test`/`run`
    // tasks, and unlike the Docker CLI path's own WORKDIR. ParkerRuntimeConfig's default
    // qmdBridgeScriptPath ("Programme 3, Unit 9.7.5") is explicitly documented as resolved
    // repository-relative, at construction time, from the live process's own working
    // directory -- an assumption every other production entry point already satisfies.
    // Without this, the forked JVM's cwd is ui-desktop/, so that repository-relative default
    // resolves to the non-existent ui-desktop/tools/qmd-relevance-bridge.mts instead of the
    // real tools/qmd-relevance-bridge.mts, and QmdRelevanceMechanism.rank() fails with
    // MODULE_NOT_FOUND the first time Bounded Relevance Computation's fallback is reached.
    workingDir = rootProject.projectDir
    // Belt-and-suspenders regression guard: fail this task immediately, with a clear diagnostic,
    // if the working directory above ever again drifts from the repository root -- rather than
    // launching the UI only to have it fail much later, deep inside a live QMD subprocess call,
    // the first time an owner turn reaches Bounded Relevance Computation's fallback branch.
    doFirst {
        val bridgeScript = workingDir.resolve("tools/qmd-relevance-bridge.mts")
        require(bridgeScript.isFile) {
            "runOwnerUi's own working directory ($workingDir) does not resolve ParkerRuntimeConfig's " +
                "default qmdBridgeScriptPath to a real file (expected: $bridgeScript). This task's " +
                "workingDir must be the repository root, exactly as the CLI/Docker entry point's own " +
                "WORKDIR already is, or PARKER_QMD_BRIDGE_SCRIPT_PATH must be set explicitly."
        }
    }
}
