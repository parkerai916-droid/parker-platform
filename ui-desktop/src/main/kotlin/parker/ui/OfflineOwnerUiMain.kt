package parker.ui

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

/** Explicit offline-only graphical launcher. It has no runtime mode. */
fun main() = application {
    val interaction = remember {
        OfflineOwnerInteraction(
            listOf(
                OfflineOwnerScenario.ReplyDelivered(
                    replyText = "Hello. I’m Parker. This local preview is ready for our conversation.",
                    delayMillis = 700,
                ),
                OfflineOwnerScenario.ReplyDelivered(
                    replyText = "I received that through the deterministic offline reply channel.",
                    delayMillis = 900,
                ),
                OfflineOwnerScenario.NotAccepted("This offline demonstration rejected the submission.", delayMillis = 500),
                OfflineOwnerScenario.Planned("Completed", delayMillis = 650),
                OfflineOwnerScenario.Failed("OFFLINE", "The scripted demonstration reached a safe failure.", delayMillis = 500),
            ),
        )
    }
    val controller = remember { OwnerUiController(interaction) }

    Window(
        onCloseRequest = {
            controller.shutdown("Window closed")
            exitApplication()
        },
        title = "Parker",
        state = WindowState(width = 980.dp, height = 720.dp),
    ) {
        ParkerOwnerWindow(controller, OwnerWindowPresentationMode.OFFLINE_PREVIEW)
    }
}
