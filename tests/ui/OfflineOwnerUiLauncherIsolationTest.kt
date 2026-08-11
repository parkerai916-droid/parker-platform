package parker.ui

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

class OfflineOwnerUiLauncherIsolationTest {

    @Test
    fun `offline graphical launcher constructs only controller and offline interaction`() {
        val source = Files.readString(Path.of("ui-desktop/src/main/kotlin/parker/ui/OfflineOwnerUiMain.kt"))

        assertTrue("OfflineOwnerInteraction(" in source)
        assertTrue("OwnerUiController(interaction)" in source)
        assertTrue("ParkerOwnerWindow(controller)" in source)

        listOf(
            "ParkerRuntime",
            "ParkerRuntimeConfig",
            "ReasoningProvider",
            "PermissionEngine",
            "ConversationEngine",
            "MemoryCore",
            "Evidence",
            "Ollama",
            "Qwen",
            "HttpClient",
            "java.net",
            "System.getenv",
        ).forEach { forbidden ->
            assertTrue(forbidden !in source, "offline graphical launcher must not reference $forbidden")
        }
    }

    @Test
    fun `existing application main remains headless and offline UI has a separate task`() {
        val build = Files.readString(Path.of("build.gradle.kts"))
        val desktopBuild = Files.readString(Path.of("ui-desktop/build.gradle.kts"))
        val main = Files.readString(Path.of("src/composition/Main.kt"))

        assertTrue("mainClass.set(\"parker.composition.MainKt\")" in build)
        assertTrue("org.jetbrains.compose" !in build)
        assertTrue("tasks.register<JavaExec>(\"runOfflineOwnerUi\")" in desktopBuild)
        assertTrue("mainClass.set(\"parker.ui.OfflineOwnerUiMainKt\")" in desktopBuild)
        assertTrue("OfflineOwnerUiMain" !in main)
        assertTrue("ParkerOwnerWindow" !in main)
    }
}
