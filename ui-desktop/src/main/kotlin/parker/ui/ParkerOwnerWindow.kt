package parker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ParkerOwnerWindow(
    controller: OwnerUiController,
    presentationMode: OwnerWindowPresentationMode,
) {
    val state by controller.state.collectAsState()
    ParkerOwnerWindowContent(
        state = state,
        presentationMode = presentationMode,
        onSubmit = controller::submit,
    )
}

@Composable
internal fun ParkerOwnerWindowContent(
    state: OwnerUiState,
    presentationMode: OwnerWindowPresentationMode,
    onSubmit: (String) -> OwnerSubmissionAttempt,
) {
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var priorTranscriptCount by remember { mutableStateOf(0) }

    LaunchedEffect(state.transcript.size) {
        if (shouldScrollToTranscriptEnd(priorTranscriptCount, state.transcript.size)) {
            listState.animateScrollToItem(state.transcript.lastIndex)
        }
        priorTranscriptCount = state.transcript.size
    }

    fun submitDraft() {
        if (canSubmitOwnerText(state, draft) && onSubmit(draft) == OwnerSubmissionAttempt.STARTED) {
            draft = ""
        }
    }

    ParkerTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = ParkerBackground) {
            Column(modifier = Modifier.fillMaxSize()) {
                ParkerHeader(state.status, presentationMode)
                Divider(color = Color.White.copy(alpha = 0.08f))

                if (state.transcript.isEmpty()) {
                    EmptyConversation(presentationMode, modifier = Modifier.weight(1f))
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(28.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(state.transcript) { entry -> TranscriptEntry(entry) }
                    }
                }

                AnimatedVisibility(visible = state.status == OwnerUiStatus.PROCESSING) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = ParkerAccent,
                        )
                        Text(
                            "Parker is processing…",
                            modifier = Modifier.padding(start = 10.dp),
                            color = ParkerMuted,
                            fontSize = 13.sp,
                        )
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.08f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(22.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f).height(96.dp).onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown || event.key != Key.Enter) {
                                false
                            } else {
                                when (ownerInputKeyAction(true, event.isShiftPressed, state, draft)) {
                                    OwnerInputKeyAction.SUBMIT -> {
                                        submitDraft()
                                        true
                                    }
                                    OwnerInputKeyAction.INSERT_NEWLINE -> false
                                    OwnerInputKeyAction.NONE -> true
                                }
                            }
                        },
                        enabled = state.status != OwnerUiStatus.STOPPED,
                        label = { Text("Message Parker") },
                        placeholder = { Text("Write a message…") },
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                    )
                    Button(
                        onClick = ::submitDraft,
                        enabled = canSubmitOwnerText(state, draft),
                        modifier = Modifier.height(52.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = ParkerAccent),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Send", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ParkerHeader(status: OwnerUiStatus, presentationMode: OwnerWindowPresentationMode) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(ParkerAccent),
            contentAlignment = Alignment.Center,
        ) {
            Text("P", color = Color(0xFF062118), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text("Parker", style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold)
            Text(ownerConversationHeader(presentationMode), color = ParkerMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
        StatusPill(status)
    }
}

@Composable
private fun StatusPill(status: OwnerUiStatus) {
    val (label, color) = when (status) {
        OwnerUiStatus.READY -> "Ready" to ParkerAccent
        OwnerUiStatus.PROCESSING -> "Processing" to Color(0xFFFFD180)
        OwnerUiStatus.STOPPED -> "Stopped" to ParkerMuted
        OwnerUiStatus.ERROR -> "Error" to MaterialTheme.colors.error
    }
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(ParkerSurfaceRaised)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(color))
        Text(label, modifier = Modifier.padding(start = 7.dp), color = color, fontSize = 12.sp)
    }
}

@Composable
private fun EmptyConversation(
    presentationMode: OwnerWindowPresentationMode,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Start a conversation", fontSize = 22.sp, fontWeight = FontWeight.Medium)
            Text(
                ownerConversationEmptyState(presentationMode),
                modifier = Modifier.padding(top = 8.dp).widthIn(max = 440.dp),
                color = ParkerMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TranscriptEntry(entry: OwnerTranscriptEntry) {
    when (presentationRole(entry)) {
        TranscriptPresentationRole.OWNER -> MessageBubble("You", entry.text, ParkerOwnerBubble, Alignment.End)
        TranscriptPresentationRole.PARKER -> MessageBubble("Parker", entry.text, ParkerReplyBubble, Alignment.Start)
        TranscriptPresentationRole.SYSTEM -> SystemEntry(entry.text)
    }
}

@Composable
private fun MessageBubble(
    author: String,
    text: String,
    color: Color,
    alignment: Alignment.Horizontal,
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Text(author, color = ParkerMuted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
        Surface(color = color, shape = RoundedCornerShape(16.dp), modifier = Modifier.widthIn(max = 620.dp)) {
            Text(text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), lineHeight = 21.sp)
        }
    }
}

@Composable
private fun SystemEntry(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text(
            text,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(ParkerSurfaceRaised)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            color = ParkerMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
    }
}
