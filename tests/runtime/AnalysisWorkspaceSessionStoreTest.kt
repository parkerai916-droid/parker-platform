package parker.core.runtime

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import parker.core.interfaces.AnalysisType
import parker.core.interfaces.AnalysisWorkspaceSessionId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.StructuredAnalysisResult

class AnalysisWorkspaceSessionStoreTest {
    private val clock = Clock.fixed(Instant.parse("2026-09-12T00:00:00Z"), ZoneOffset.UTC)
    private val evidence = listOf(EvidenceArtifactId("evidence-a"), EvidenceArtifactId("evidence-b"))
    private val result = StructuredAnalysisResult("Answer", emptyList(), emptyList(), emptyList(), emptyList(), "Conclusion")

    @Test
    fun `session is server generated and scope is immutable across turns`() {
        val store = AnalysisWorkspaceSessionStore(clock)
        val session = store.create("case-a", evidence, AnalysisType.ISSUE_ANALYSIS)
        assertTrue(session.sessionId.value.startsWith("analysis-session-"))
        assertEquals(evidence, session.selectedEvidenceArtifactIds)
        val updated = store.record(session.sessionId, "First question", result)
        assertEquals(evidence, updated!!.selectedEvidenceArtifactIds)
        assertNotEquals(session.sessionId, AnalysisWorkspaceSessionId.new())
    }

    @Test
    fun `context retains bounded recent turns and never becomes evidence`() {
        val store = AnalysisWorkspaceSessionStore(clock)
        val session = store.create(null, evidence, AnalysisType.ISSUE_ANALYSIS)
        store.record(session.sessionId, "First question", result)
        val current = store.find(session.sessionId)!!
        val context = store.context(current)!!
        assertTrue(context.contains("Previous user question: First question"))
        assertTrue(context.contains("Previous Parker answer: Answer"))
        assertTrue(!context.contains("EvidenceArtifactId"))
    }

    @Test
    fun `unknown session does not resolve or record`() {
        val store = AnalysisWorkspaceSessionStore(clock)
        val unknown = AnalysisWorkspaceSessionId.new()
        assertNull(store.find(unknown))
        assertNull(store.record(unknown, "question", result))
    }
}
