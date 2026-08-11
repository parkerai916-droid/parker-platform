package parker.ui

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OfflineOwnerInteractionTest {

    @Test
    fun `scripted scenarios preserve every frozen disposition deterministically`() = runTest {
        val interaction = OfflineOwnerInteraction(
            listOf(
                OfflineOwnerScenario.NotAccepted("rejected"),
                OfflineOwnerScenario.Failed("UNKNOWN", "safe"),
                OfflineOwnerScenario.Planned("Rejected"),
                OfflineOwnerScenario.Delivered("FAILED"),
            ),
            delayFor = {},
        )
        val replies = mutableListOf<String>()
        suspend fun submit(text: String) = interaction.submit(text) { replies.add(it.text) }

        assertEquals("rejected", assertIs<OwnerSubmissionDisposition.NotAccepted>(submit("one")).reason)
        assertEquals("safe", assertIs<OwnerSubmissionDisposition.Failed>(submit("two")).safeMessage)
        assertEquals("Rejected", assertIs<OwnerSubmissionDisposition.Planned>(submit("three")).resultCategory)
        assertEquals("FAILED", assertIs<OwnerSubmissionDisposition.Delivered>(submit("four")).executionStatus)
        assertTrue(replies.isEmpty())
        assertEquals(listOf("one", "two", "three", "four"), interaction.submissions)
    }

    @Test
    fun `fake independently rejects concurrent direct submissions`() = runTest {
        val enteredDelay = CompletableDeferred<Unit>()
        val releaseDelay = CompletableDeferred<Unit>()
        val interaction = OfflineOwnerInteraction(
            listOf(
                OfflineOwnerScenario.Delivered(delayMillis = 1),
                OfflineOwnerScenario.Delivered(),
            ),
            delayFor = {
                enteredDelay.complete(Unit)
                releaseDelay.await()
            },
        )

        val first = async { interaction.submit("first") {} }
        enteredDelay.await()
        val failure = assertFailsWith<IllegalStateException> {
            interaction.submit("second") {}
        }
        assertTrue("Only one" in failure.message.orEmpty())
        releaseDelay.complete(Unit)
        first.await()
        assertEquals(listOf("first"), interaction.submissions)
    }

    @Test
    fun `stopped fake reports unavailable without invoking reply receiver`() = runTest {
        val interaction = OfflineOwnerInteraction(emptyList(), initiallyAvailable = false)
        var replyCalled = false

        val disposition = interaction.submit("hello") { replyCalled = true }

        assertIs<OwnerSubmissionDisposition.Unavailable>(disposition)
        assertEquals(false, replyCalled)
    }

    @Test
    fun `offline source has no runtime model network environment or Compose dependency`() {
        val source = Files.readString(Path.of("src/ui/parker/ui/OfflineOwnerInteraction.kt"))
        val forbidden = listOf(
            "ParkerRuntime",
            "ParkerRuntimeConfig",
            "ReasoningProvider",
            "Ollama",
            "Qwen",
            "HttpClient",
            "java.net",
            "System.getenv",
            "androidx.compose",
            "org.jetbrains.compose",
        )

        forbidden.forEach { token ->
            assertTrue(token !in source, "OfflineOwnerInteraction must not reference $token")
        }
    }
}
