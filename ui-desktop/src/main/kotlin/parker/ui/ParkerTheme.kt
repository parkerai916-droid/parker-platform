package parker.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val ParkerBackground = Color(0xFF0D1412)
internal val ParkerSurface = Color(0xFF17211E)
internal val ParkerSurfaceRaised = Color(0xFF202C28)
internal val ParkerAccent = Color(0xFF69D6A6)
internal val ParkerOwnerBubble = Color(0xFF295548)
internal val ParkerReplyBubble = Color(0xFF23312D)
internal val ParkerMuted = Color(0xFFA7B6B0)

private val ParkerColors = darkColors(
    primary = ParkerAccent,
    primaryVariant = Color(0xFF45B988),
    secondary = Color(0xFF9BE6C5),
    background = ParkerBackground,
    surface = ParkerSurface,
    onPrimary = Color(0xFF062118),
    onSecondary = Color(0xFF08251B),
    onBackground = Color(0xFFF0F7F4),
    onSurface = Color(0xFFF0F7F4),
    error = Color(0xFFFF8A80),
)

@Composable
fun ParkerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = ParkerColors, content = content)
}
