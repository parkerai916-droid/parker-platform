package parker.core.runtime

import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import parker.core.interfaces.AnalysisType
import parker.core.interfaces.AnalysisWorkspaceSession
import parker.core.interfaces.AnalysisWorkspaceSessionId
import parker.core.interfaces.AnalysisWorkspaceTurn
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.StructuredAnalysisResult

/**
 * Owner Analysis Workspace continuity only. This is not evidence, memory, or a case projection.
 * Sessions are process-local: browser refresh survives, Parker restart deliberately does not.
 */
class AnalysisWorkspaceSessionStore(
    private val clock: Clock = Clock.systemUTC(),
    private val maximumSessions: Int = 100,
    private val maximumTurns: Int = 6,
    private val maximumContextCharacters: Int = 24_000,
) {
    private val sessions = ConcurrentHashMap<AnalysisWorkspaceSessionId, AnalysisWorkspaceSession>()

    @Synchronized
    fun create(caseId: String?, evidence: List<EvidenceArtifactId>, analysisType: AnalysisType): AnalysisWorkspaceSession {
        if (sessions.size >= maximumSessions) {
            val oldest = sessions.values.minByOrNull { it.updatedAt }
            if (oldest != null) sessions.remove(oldest.sessionId)
        }
        val now = Instant.now(clock)
        val session = AnalysisWorkspaceSession(AnalysisWorkspaceSessionId.new(), caseId, evidence.toList(), analysisType, now, now)
        sessions[session.sessionId] = session
        return session
    }

    fun find(sessionId: AnalysisWorkspaceSessionId): AnalysisWorkspaceSession? = sessions[sessionId]

    @Synchronized
    fun record(sessionId: AnalysisWorkspaceSessionId, question: String, result: StructuredAnalysisResult): AnalysisWorkspaceSession? {
        val current = sessions[sessionId] ?: return null
        val retained = (current.turns + AnalysisWorkspaceTurn(question, result.answer, result.conclusion)).takeLast(maximumTurns)
        val bounded = trimContext(retained)
        val updated = current.copy(updatedAt = Instant.now(clock), turns = bounded)
        sessions[sessionId] = updated
        return updated
    }

    fun context(session: AnalysisWorkspaceSession): String? = contextForTurns(session.turns)

    private fun contextForTurns(turns: List<AnalysisWorkspaceTurn>): String? = if (turns.isEmpty()) null else
        turns.joinToString("\n\n") { "Previous user question: ${it.question}\nPrevious Parker answer: ${it.answer}\nPrevious Parker conclusion: ${it.conclusion}" }

    private fun trimContext(turns: List<AnalysisWorkspaceTurn>): List<AnalysisWorkspaceTurn> {
        var result = turns
        while (result.size > 1 && contextForTurns(result)!!.length > maximumContextCharacters) result = result.drop(1)
        return result
    }
}
