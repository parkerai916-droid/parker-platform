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
}
