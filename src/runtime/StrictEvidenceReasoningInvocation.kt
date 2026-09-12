package parker.core.runtime

import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.GroundedPropositionClassification
import parker.core.interfaces.GroundedReply
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningContextEntry
import parker.core.interfaces.ReasoningProvider
import parker.core.interfaces.ReasoningProviderRequest
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.ReasoningSubject
import parker.core.interfaces.RelationshipEndpoint

/** Execution-only failures for the strict evidence path. */
enum class StrictEvidenceReasoningFailure {
    AUTHORIZATION_FAILURE,
    INPUT_RESOLUTION_FAILURE,
    INPUT_RESOLUTION_INCONSISTENT,
    MALFORMED_TYPED_CONTEXT,
    PROVIDER_FAILURE,
    NON_STRICT_PROVIDER_OUTPUT,
    MALFORMED_GROUNDED_REPLY,
    INVALID_GROUNDING,
    KNOWN_CONFLICT_PROJECTION_FAILURE,
    KNOWN_CONFLICT_ENFORCEMENT_FAILURE,
}

/** Non-durable result of one strict evidence reasoning invocation. */
sealed interface StrictEvidenceReasoningResult {
    data class Success(
        val groundedReply: GroundedReply,
        val validation: GroundedReplyValidationOutcome.Valid,
        val groundingReport: GroundingReport,
    ) : StrictEvidenceReasoningResult

    data class HumanReviewRequired(
        val groundedReply: GroundedReply,
        val validation: GroundedReplyValidationOutcome.Valid,
        val groundingReport: GroundingReport,
    ) : StrictEvidenceReasoningResult

    data class Failed(
        val failure: StrictEvidenceReasoningFailure,
        val groundingReport: GroundingReport? = null,
    ) : StrictEvidenceReasoningResult
}

/** Fixed provider instruction for the separate strict-evidence path. */
internal object StrictEvidenceInstructions {
    val TEXT = """
STRICT EVIDENCE REASONING.
Use only information contained in the supplied Parker governed context. Do not use general model knowledge to fill evidential gaps. Do not invent facts, evidence, evidence identifiers, dates, names, events, relationships, or provenance. Every material proposition must be exactly one of SUPPORTED_FACT, INFERENCE, NOT_ESTABLISHED, or HUMAN_REVIEW_REQUIRED. SUPPORTED_FACT requires a supplied governed context reference. INFERENCE must identify its supplied evidential basis and must not be stated as direct fact. Use NOT_ESTABLISHED when the supplied context does not establish a proposition; do not guess or fabricate support. Use HUMAN_REVIEW_REQUIRED for conflict, unresolved ambiguity, malformed or insufficient grounding, or any issue that cannot be resolved from the supplied context. Do not silently reconcile conflicting evidence. Absence of evidence is not evidence of the opposite. Never promote INFERENCE, NOT_ESTABLISHED, or HUMAN_REVIEW_REQUIRED into a resolved fact. Return only the exact GROUNDED_REPLY_R0 wire format expected by GroundedReplyParser.
    """.trimIndent()
}

/**
 * Distinct, stateless execution path for strict evidence reasoning.
 * Resolution, context capture, provider invocation, parsing, validation, and
 * reporting are deliberately kept in this order. Ordinary conversational
 * ReasoningProvider calls do not pass through this class.
 */
internal class StrictEvidenceReasoningInvocation(
    private val inputResolver: EvidenceIntelligenceInputResolver,
    private val reasoningProvider: ReasoningProvider,
    private val groundedReplyParser: GroundedReplyParser = GroundedReplyParser(),
) {

    suspend fun invoke(request: EvidenceAnalysisRequest): StrictEvidenceReasoningResult {
        val resolved = try {
            inputResolver.resolve(request)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return StrictEvidenceReasoningResult.Failed(StrictEvidenceReasoningFailure.INPUT_RESOLUTION_FAILURE)
        }
        val evidenceResults = resolved.first
        val memoryCoreResults = resolved.second

        val conflicts = when (val outcome = KnownConflictProjector.project(request, memoryCoreResults, inputResolver.memoryRetrieval)) {
            is KnownConflictProjectionOutcome.Projected -> outcome.conflicts
            KnownConflictProjectionOutcome.FailedClosed ->
                return StrictEvidenceReasoningResult.Failed(StrictEvidenceReasoningFailure.KNOWN_CONFLICT_PROJECTION_FAILURE)
        }

        try {
            EvidenceIntelligenceGroundingValidationGate.validate(
                request,
                evidenceResults,
                memoryCoreResults,
                emptyList(),
            )
        } catch (_: Exception) {
            return StrictEvidenceReasoningResult.Failed(StrictEvidenceReasoningFailure.INPUT_RESOLUTION_INCONSISTENT)
        }

        val context = try {
            typedContext(evidenceResults, memoryCoreResults, conflicts)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return StrictEvidenceReasoningResult.Failed(StrictEvidenceReasoningFailure.MALFORMED_TYPED_CONTEXT)
        }
        // Snapshot the exact governed set before the provider is called. The
        // provider receives a value copy and has no operation to append to it.
        val suppliedSnapshot = context.suppliedGovernedEntries.toList()
        check(suppliedSnapshot == context.suppliedGovernedEntries)

        val providerResponse = try {
            reasoningProvider.reason(
                ReasoningProviderRequest(
                    subject = ReasoningSubject.OfEvidenceAnalysisRequest(request),
                    reasoningContext = context,
                ),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return StrictEvidenceReasoningResult.Failed(StrictEvidenceReasoningFailure.PROVIDER_FAILURE)
        }
        val raw = (providerResponse as? ReasoningProviderResponse.Reply)?.text
            ?: return StrictEvidenceReasoningResult.Failed(StrictEvidenceReasoningFailure.NON_STRICT_PROVIDER_OUTPUT)

        val reply = try {
            groundedReplyParser.parse(raw, context)
        } catch (_: Exception) {
            return StrictEvidenceReasoningResult.Failed(StrictEvidenceReasoningFailure.MALFORMED_GROUNDED_REPLY)
        }
        val validation = GroundedReplyValidator.validate(reply, context, evidenceResults, memoryCoreResults)
        if (validation is GroundedReplyValidationOutcome.Invalid) {
            val report = GroundingReportProjection.project(
                request, reply, context, evidenceResults, memoryCoreResults,
            )
            return StrictEvidenceReasoningResult.Failed(StrictEvidenceReasoningFailure.INVALID_GROUNDING, report)
        }
        val conflictEnforcement = KnownConflictEnforcer.enforce(reply, context, conflicts)
        val conflictReport = GroundingReportProjection.project(
            request, reply, context, evidenceResults, memoryCoreResults, knownConflicts = conflicts,
            conflictEnforcement = conflictEnforcement,
        )
        if (conflictEnforcement is KnownConflictEnforcementOutcome.Invalid) {
            return StrictEvidenceReasoningResult.Failed(StrictEvidenceReasoningFailure.KNOWN_CONFLICT_ENFORCEMENT_FAILURE, conflictReport)
        }
        val semanticSupport = ExactStructuredClaimSupportValidator.validate(
            reply,
            context,
            evidenceResults,
            memoryCoreResults,
            validation,
        )
        val report = GroundingReportProjection.project(
            request,
            reply,
            context,
            evidenceResults,
            memoryCoreResults,
            semanticSupport,
            knownConflicts = conflicts,
            conflictEnforcement = conflictEnforcement,
        )
        val valid = validation as GroundedReplyValidationOutcome.Valid
        if (semanticSupport is ExactStructuredClaimSupportValidationOutcome.Invalid) {
            return StrictEvidenceReasoningResult.Failed(StrictEvidenceReasoningFailure.INVALID_GROUNDING, report)
        }
        return if (semanticSupport is ExactStructuredClaimSupportValidationOutcome.HumanReviewRequired ||
            reply.propositions.any { it.classification == GroundedPropositionClassification.HUMAN_REVIEW_REQUIRED }
        ) {
            StrictEvidenceReasoningResult.HumanReviewRequired(reply, valid, report)
        } else {
            StrictEvidenceReasoningResult.Success(reply, valid, report)
        }
    }

    private fun typedContext(
        evidenceResults: List<EvidenceRetrievalResult>,
        memoryCoreResults: List<Pair<RelationshipEndpoint, MemoryCoreRecord? >>,
        conflicts: List<KnownConflictProjection>,
    ): ReasoningContext {
        val entries = buildList {
            add(ReasoningContextEntry.PlainText(StrictEvidenceInstructions.TEXT))
            evidenceResults.filterIsInstance<EvidenceRetrievalResult.Found>().forEach { found ->
                add(
                    ReasoningContextEntry.GovernedEvidence(
                        text = "Governed evidence supplied: ${found.evidenceArtifactId.value}",
                        evidenceArtifactId = found.evidenceArtifactId,
                        sourceSha256 = sha256(found.content),
                    ),
                )
            }
            memoryCoreResults.filter { it.second != null }.forEach { (endpoint, _) ->
                add(
                    ReasoningContextEntry.GovernedMemoryCore(
                        text = "Governed Memory Core record supplied: ${endpoint.recordKind}/${endpoint.recordId}",
                        reference = endpoint,
                    ),
                )
            }
            addAll(KnownConflictProjector.contextEntries(ReasoningContext.fromTypedEntries(this), conflicts).drop(this.size))
        }
        return ReasoningContext.fromTypedEntries(entries)
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
}
