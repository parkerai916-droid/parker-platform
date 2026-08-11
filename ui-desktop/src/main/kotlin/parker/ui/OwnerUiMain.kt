package parker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch
import parker.composition.OwnerUiRuntimeSession
import parker.composition.OwnerUiRuntimeStartResult
import parker.composition.ParkerRuntimeException
import parker.composition.createOwnerUiRuntimeSession
import parker.composition.shutdownOwnerUiSession

private sealed interface OwnerUiLaunchState {
    data class Starting(val session: OwnerUiRuntimeSession? = null) : OwnerUiLaunchState
    data class Ready(val session: OwnerUiRuntimeSession, val controller: OwnerUiController) : OwnerUiLaunchState
    data class Failed(val session: OwnerUiRuntimeSession?, val safeMessage: String) : OwnerUiLaunchState
}

/** Explicit real-runtime graphical launcher. Never used by the offline task. */
fun main() = application {
    var launchState by remember { mutableStateOf<OwnerUiLaunchState>(OwnerUiLaunchState.Starting()) }
    var closing by remember { mutableStateOf(false) }
    val applicationScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val session = try {
            createOwnerUiRuntimeSession(System.getenv())
        } catch (configurationFailure: ParkerRuntimeException) {
            launchState = OwnerUiLaunchState.Failed(null, "Parker Runtime configuration is invalid")
            return@LaunchedEffect
        }
        launchState = OwnerUiLaunchState.Starting(session)
        when (val result = session.start()) {
            is OwnerUiRuntimeStartResult.Ready ->
                launchState = OwnerUiLaunchState.Ready(session, OwnerUiController(result.interaction))
            is OwnerUiRuntimeStartResult.Failed ->
                launchState = OwnerUiLaunchState.Failed(session, result.safeMessage)
        }
    }

    Window(
        onCloseRequest = {
            if (!closing) {
                closing = true
                val stateAtClose = launchState
                val session = when (stateAtClose) {
                    is OwnerUiLaunchState.Starting -> stateAtClose.session
                    is OwnerUiLaunchState.Ready -> stateAtClose.session
                    is OwnerUiLaunchState.Failed -> stateAtClose.session
                }
                applicationScope.launch {
                    try {
                        if (stateAtClose is OwnerUiLaunchState.Ready) {
                            shutdownOwnerUiSession(stateAtClose.controller, stateAtClose.session)
                        } else {
                            session?.shutdown()
                        }
                    } finally {
                        exitApplication()
                    }
                }
            }
        },
        title = "Parker",
        state = WindowState(width = 980.dp, height = 720.dp),
    ) {
        when (val state = launchState) {
            is OwnerUiLaunchState.Starting -> OwnerUiStarting()
            is OwnerUiLaunchState.Ready -> ParkerOwnerWindow(state.controller)
            is OwnerUiLaunchState.Failed -> OwnerUiStartupFailure(state.safeMessage)
        }
    }
}

@Composable
private fun OwnerUiStarting() {
    ParkerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Text("Starting Parker Runtime...", modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}

@Composable
private fun OwnerUiStartupFailure(safeMessage: String) {
    ParkerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Parker could not start")
                Text(safeMessage, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
