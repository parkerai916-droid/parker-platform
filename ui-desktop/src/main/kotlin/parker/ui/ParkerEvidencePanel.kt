package parker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.FileDialog
import java.awt.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ParkerEvidencePanel(controller: OwnerEvidenceUiController) {
    val state by controller.state.collectAsState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Evidence", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(
                    "Each selected file is imported, custodied, and processed independently.",
                    color = ParkerMuted,
                    fontSize = 12.sp,
                )
            }
            Button(
                onClick = {
                    scope.launch {
                        val selections = pickFiles()
                        if (selections.isNotEmpty()) controller.selectFiles(selections)
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = ParkerAccent),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Select Files…", fontWeight = FontWeight.SemiBold)
            }
        }
        Divider(color = Color.White.copy(alpha = 0.08f))

        if (state.files.isEmpty()) {
            Box(Modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.Center) {
                Text(
                    "No files selected yet.",
                    color = ParkerMuted,
                    modifier = Modifier.widthIn(max = 380.dp),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.files, key = { it.rowId }) { row ->
                    EvidenceFileRowCard(
                        row = row,
                        onProcessTierA = { controller.processTierA(row.rowId) },
                        onProcessTierB = { controller.processTierB(row.rowId) },
                    )
                }
            }
        }
    }
}

/**
 * Native, OS-provided multi-select file dialog -- part of the JDK itself, no
 * new dependency. Runs off the calling coroutine's own dispatcher so the
 * (blocking, native) dialog call never freezes Compose recomposition. Every
 * returned [java.io.File] is a real, already-existing local path the OS
 * dialog itself resolved -- never a string constructed from untrusted input.
 * [java.nio.file.Files.probeContentType] supplies [OwnerEvidenceFileSelection.declaredMediaType]
 * -- an OS-extension-table lookup, the identical epistemic status a
 * browser's own Content-Type header already has (declared, never
 * authoritative); `null` when the OS itself declares no opinion, never a
 * fabricated guess.
 */
private suspend fun pickFiles(): List<OwnerEvidenceFileSelection> = withContext(Dispatchers.IO) {
    val dialog = FileDialog(null as Frame?, "Select evidence files", FileDialog.LOAD)
    dialog.isMultipleMode = true
    dialog.isVisible = true
    dialog.files.map { file ->
        OwnerEvidenceFileSelection(
            absolutePath = file.absolutePath,
            originalFileName = file.name,
            byteLength = file.length(),
            declaredMediaType = runCatching { java.nio.file.Files.probeContentType(file.toPath()) }.getOrNull(),
        )
    }
}

@Composable
private fun EvidenceFileRowCard(
    row: OwnerEvidenceFileRow,
    onProcessTierA: () -> Unit,
    onProcessTierB: () -> Unit,
) {
    Surface(color = ParkerSurfaceRaised, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(row.originalFileName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(
                        buildString {
                            append(formatByteLength(row.byteLength))
                            row.evidenceArtifactId?.let { append(" · ").append(it.value) }
                            row.tierAFormat?.let { append(" · ").append(it) }
                        },
                        color = ParkerMuted,
                        fontSize = 11.sp,
                    )
                }
                StatusChip(row)
            }

            if (isRowBusy(row)) {
                Row(modifier = Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = ParkerAccent)
                }
            } else if (canProcessTierA(row)) {
                OutlinedButton(onClick = onProcessTierA, modifier = Modifier.padding(top = 10.dp)) {
                    Text("Process")
                }
            } else if (canProcessTierB(row)) {
                OutlinedButton(onClick = onProcessTierB, modifier = Modifier.padding(top = 10.dp)) {
                    Text("Run OCR")
                }
            }

            row.message?.let { message ->
                Text(message, color = ParkerMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun StatusChip(row: OwnerEvidenceFileRow) {
    val color = when (row.status) {
        OwnerEvidenceFileStatus.IMPORT_FAILED, OwnerEvidenceFileStatus.FAILED -> Color(0xFFFF8A80)
        OwnerEvidenceFileStatus.TIER_A_COMPLETE, OwnerEvidenceFileStatus.COMPLETE -> ParkerAccent
        OwnerEvidenceFileStatus.REQUIRES_OCR -> Color(0xFFFFD180)
        else -> ParkerMuted
    }
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(color))
        Text(evidenceStatusLabel(row.status), modifier = Modifier.padding(start = 6.dp), color = color, fontSize = 11.sp)
    }
}
