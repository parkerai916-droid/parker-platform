package parker.core.interfaces

import java.time.Instant
import java.util.UUID

/** Server-created identity for one Owner Analysis Workspace conversation. */
data class AnalysisWorkspaceSessionId(val value: String) {
    init { require(value.matches(Regex("analysis-session-[0-9a-f-]{36}"))) }
    companion object { fun new() = AnalysisWorkspaceSessionId("analysis-session-${UUID.randomUUID()}") }
}

/** Bounded Owner workspace state; evidence scope is immutable for the session lifetime. */
data class AnalysisWorkspaceSession(
    val sessionId: AnalysisWorkspaceSessionId,
    val caseId: String?,
    val selectedEvidenceArtifactIds: List<EvidenceArtifactId>,
    val analysisType: AnalysisType,
    val createdAt: Instant,
    val updatedAt: Instant,
    val turns: List<AnalysisWorkspaceTurn> = emptyList(),
)

data class AnalysisWorkspaceTurn(
    val question: String,
    val answer: String,
    val conclusion: String,
)
