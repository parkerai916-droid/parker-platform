package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import parker.core.interfaces.AnalysisEvidenceItem
import parker.core.interfaces.CsvStructuralResult
import parker.core.interfaces.DocumentAnalysisOutcome
import parker.core.interfaces.DocxStructuralResult
import parker.core.interfaces.EmlStructuralResult
import parker.core.interfaces.EvidenceGenerationSelection
import parker.core.interfaces.OwnerDocumentAnalysisRequest
import parker.core.interfaces.OwnerDocumentAnalysisResult
import parker.core.interfaces.PdfStructuralResult
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.TierADerivativePayload
import parker.core.interfaces.TierAContentRetrievalOutcome
import parker.core.interfaces.TierBOcrContentRetrievalOutcome

/**
 * Minimum Production Document Pipeline — Local Reasoning Implementation.
 * The shortest safe production path from Parker's already-admitted
 * durable document derivatives to a human-reviewable analysis using
 * Parker's existing LOCAL reasoning infrastructure.
 *
 * Sequences: [PermissionEngine] (the same, already-registered
 * `EvidenceIntelligenceInvocationGate` `(EXECUTE, DOCUMENT)` proposal
 * class [TierBOcrOwnerInvocationCoordinator] already evaluates) ->
 * per-selection derivative resolution (trying [tierBOcrContentRetrievalCoordinator]
 * first, falling back to [tierAContentRetrievalCoordinator] only on
 * `WrongDerivativeKind` -- Tier A's own retrieval performs no kind
 * discrimination and would otherwise silently decode a genuine Tier B
 * OCR generation) -> bounded evidence-package assembly -> [promptBuilder]
 * -> [modelInferenceClient.infer] (the currently configured LOCAL
 * implementation only -- this class never selects, constructs, or is
 * capable of reaching any other provider) -> a provenance-bearing
 * [OwnerDocumentAnalysisResult].
 *
 * Creates no durable side effect of any kind: no Evidence write, no
 * derivative write, no Memory/Knowledge/QMD/RKS write, no new audit
 * store, no analysis-result persistence. [analysisText] is provider-
 * generated material for human review only, never automatically
 * promoted to canonical Parker truth.
 */
class DocumentAnalysisCoordinator(
    private val permissionEngine: PermissionEngine,
    private val tierAContentRetrievalCoordinator: TierAContentRetrievalCoordinator,
    private val tierBOcrContentRetrievalCoordinator: TierBOcrContentRetrievalCoordinator,
    private val modelInferenceClient: ModelInferenceClient,
    private val promptBuilder: DocumentAnalysisPromptBuilder,
    private val modelTimeoutMs: Long,
    private val now: () -> Instant = Instant::now,
) {
    suspend fun analyse(ownerPrincipalId: PrincipalId, request: OwnerDocumentAnalysisRequest): DocumentAnalysisOutcome {
        if (request.selections.size > MAX_SELECTIONS) {
            return DocumentAnalysisOutcome.TooManySelections(request.selections.size, MAX_SELECTIONS)
        }
        if (request.instruction.length > MAX_INSTRUCTION_CHARACTERS) {
            return DocumentAnalysisOutcome.InstructionTooLarge(request.instruction.length, MAX_INSTRUCTION_CHARACTERS)
        }

        val decision = permissionEngine.evaluate(EvidenceIntelligenceInvocationGate.buildExecutionRequest(ownerPrincipalId))
        if (decision.decision != PermissionDecisionOutcome.APPROVED &&
            decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) {
            return DocumentAnalysisOutcome.NotAuthorised(
                "Permission Engine did not authorise document analysis invocation for principal " +
                    "'${ownerPrincipalId.value}' (decision=${decision.decision})",
            )
        }

        val items = mutableListOf<AnalysisEvidenceItem>()
        for (selection in request.selections) {
            val outcome = resolveOne(selection)
            if (outcome is Resolved) {
                items += outcome.item
            } else {
                return (outcome as Failed).outcome
            }
        }

        // MAX_TOTAL_CONTENT_CHARACTERS bounds SOURCE extracted-text size only, before JSON
        // encoding. It is not, and is never treated as, a bound on the resulting prompt size --
        // see MAX_PROMPT_CHARACTERS below.
        val totalCharacters = items.sumOf { it.extractedText.length }
        if (totalCharacters > MAX_TOTAL_CONTENT_CHARACTERS) {
            return DocumentAnalysisOutcome.ContentTooLarge(totalCharacters, MAX_TOTAL_CONTENT_CHARACTERS)
        }

        val prompt = promptBuilder.buildPrompt(request.instruction, items)
        // MAX_PROMPT_CHARACTERS is an independent, authoritative ceiling on the actual, fully
        // JSON-encoded prompt -- measured here, after construction, never merely assumed from
        // MAX_TOTAL_CONTENT_CHARACTERS + MAX_INSTRUCTION_CHARACTERS. DefaultDocumentAnalysisPromptBuilder's
        // own JSON escaping (jsonEscape) can expand source characters -- most sharply, a C0 control
        // character becomes a six-character \uXXXX escape -- so an evidence package that passes the
        // *source* content ceiling above can still legitimately fail PromptTooLarge here once
        // encoded. That is intentional fail-closed behaviour, not a bug: neither the evidence
        // content nor the resulting prompt is ever silently truncated to fit. This bound does not
        // guarantee every possible MAX_TOTAL_CONTENT_CHARACTERS-sized evidence package can be
        // analysed -- pathological, escape-heavy content can legitimately be rejected here.
        if (prompt.length > MAX_PROMPT_CHARACTERS) {
            return DocumentAnalysisOutcome.PromptTooLarge(prompt.length, MAX_PROMPT_CHARACTERS)
        }

        val raw = try {
            withTimeout(modelTimeoutMs) { modelInferenceClient.infer(prompt) }
        } catch (e: TimeoutCancellationException) {
            return DocumentAnalysisOutcome.ModelInvocationFailed("Local model inference timed out")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return DocumentAnalysisOutcome.ModelInvocationFailed("Local model inference failed")
        }

        if (raw.length > MAX_RESPONSE_CHARACTERS) {
            return DocumentAnalysisOutcome.ResponseTooLarge(raw.length, MAX_RESPONSE_CHARACTERS)
        }
        if (raw.isBlank()) {
            return DocumentAnalysisOutcome.ModelInvocationFailed("Local model returned an empty response")
        }

        return DocumentAnalysisOutcome.Completed(
            OwnerDocumentAnalysisResult(
                analysisText = raw,
                evidenceItems = items,
                // Parker's current local model-inference seam (ModelInferenceClient/
                // LocalHttpModelInferenceClient) discloses no model identity/version of
                // its own -- represented honestly as absent, never fabricated.
                mechanismIdentity = null,
                mechanismVersion = null,
                analysedAt = now(),
                instruction = request.instruction,
                warnings = items.flatMap { it.warnings },
            ),
        )
    }

    private sealed class ResolveResult
    private data class Resolved(val item: AnalysisEvidenceItem) : ResolveResult()
    private data class Failed(val outcome: DocumentAnalysisOutcome) : ResolveResult()

    private suspend fun resolveOne(selection: EvidenceGenerationSelection): ResolveResult {
        val tierB = tierBOcrContentRetrievalCoordinator.retrieve(selection.evidenceArtifactId, selection.derivativeGenerationId)
        when (tierB) {
            is TierBOcrContentRetrievalOutcome.Retrieved -> {
                val record = tierB.record
                return Resolved(
                    AnalysisEvidenceItem(
                        evidenceArtifactId = selection.evidenceArtifactId,
                        derivativeGenerationId = selection.derivativeGenerationId,
                        derivativeKind = record.derivativeKind,
                        contentIdentity = record.contentIdentity,
                        producerIdentity = record.producerIdentity,
                        extractedText = tierB.extracted.recognisedText,
                        completenessState = record.completenessState,
                        warnings = record.warnings,
                    ),
                )
            }
            is TierBOcrContentRetrievalOutcome.WrongDerivativeKind -> {
                // Fall through to Tier A below -- not an OCR generation.
            }
            is TierBOcrContentRetrievalOutcome.UnknownGeneration ->
                return Failed(DocumentAnalysisOutcome.UnknownGeneration(selection.derivativeGenerationId))
            is TierBOcrContentRetrievalOutcome.SourceMismatch ->
                return Failed(DocumentAnalysisOutcome.SourceMismatch(selection.evidenceArtifactId, selection.derivativeGenerationId))
            is TierBOcrContentRetrievalOutcome.ContentMissing ->
                return Failed(DocumentAnalysisOutcome.ContentMissing(selection.derivativeGenerationId))
            is TierBOcrContentRetrievalOutcome.ContentCorrupt ->
                return Failed(DocumentAnalysisOutcome.ContentCorrupt(selection.derivativeGenerationId, tierB.reason))
            is TierBOcrContentRetrievalOutcome.UnsupportedRepresentationVersion ->
                return Failed(DocumentAnalysisOutcome.UnsupportedRepresentationVersion(selection.derivativeGenerationId, tierB.version))
        }

        val tierA = tierAContentRetrievalCoordinator.retrieve(selection.evidenceArtifactId, selection.derivativeGenerationId)
        return when (tierA) {
            is TierAContentRetrievalOutcome.Retrieved -> {
                val record = tierA.record
                // Defensive, for every governed Tier A kind, not only OCR: Tier A's own retrieval
                // performs no kind discrimination at all (it decodes whatever payload shape the
                // content codec reports, independent of the resolved record's own derivativeKind
                // field), so a stored record whose derivativeKind does not match its own payload's
                // shape -- a genuine internal inconsistency, never reachable through this
                // coordinator's own normal admission paths -- must fail closed here rather than be
                // silently interpreted as an ordinary derivative of whichever kind the payload
                // happens to decode as.
                val payload = tierA.payload
                val expectedKind = when (payload) {
                    is TierADerivativePayload.Pdf -> PDF_DERIVATIVE_KIND
                    is TierADerivativePayload.Csv -> CSV_DERIVATIVE_KIND
                    is TierADerivativePayload.Eml -> EML_DERIVATIVE_KIND
                    is TierADerivativePayload.Docx -> DOCX_DERIVATIVE_KIND
                    is TierADerivativePayload.Ocr ->
                        // Tier A's own retrieval performs no kind discrimination and would
                        // otherwise silently decode a genuine Tier B OCR generation reached this
                        // far only because Tier B's own kind check above already reported
                        // WrongDerivativeKind for it.
                        return Failed(DocumentAnalysisOutcome.UnsupportedDerivativeKind(selection.derivativeGenerationId, record.derivativeKind))
                }
                if (record.derivativeKind != expectedKind) {
                    return Failed(DocumentAnalysisOutcome.UnsupportedDerivativeKind(selection.derivativeGenerationId, record.derivativeKind))
                }
                val extractedText = when (payload) {
                    is TierADerivativePayload.Pdf -> extractPdfText(payload.value)
                    is TierADerivativePayload.Csv -> extractCsvText(payload.value)
                    is TierADerivativePayload.Eml -> extractEmlText(payload.value)
                    is TierADerivativePayload.Docx -> extractDocxText(payload.value)
                    is TierADerivativePayload.Ocr -> error("unreachable -- handled above")
                }
                Resolved(
                    AnalysisEvidenceItem(
                        evidenceArtifactId = selection.evidenceArtifactId,
                        derivativeGenerationId = selection.derivativeGenerationId,
                        derivativeKind = record.derivativeKind,
                        contentIdentity = record.contentIdentity,
                        producerIdentity = record.producerIdentity,
                        extractedText = extractedText,
                        completenessState = record.completenessState,
                        warnings = record.warnings,
                    ),
                )
            }
            is TierAContentRetrievalOutcome.UnknownGeneration ->
                Failed(DocumentAnalysisOutcome.UnknownGeneration(selection.derivativeGenerationId))
            is TierAContentRetrievalOutcome.SourceMismatch ->
                Failed(DocumentAnalysisOutcome.SourceMismatch(selection.evidenceArtifactId, selection.derivativeGenerationId))
            is TierAContentRetrievalOutcome.ContentMissing ->
                Failed(DocumentAnalysisOutcome.ContentMissing(selection.derivativeGenerationId))
            is TierAContentRetrievalOutcome.ContentCorrupt ->
                Failed(DocumentAnalysisOutcome.ContentCorrupt(selection.derivativeGenerationId, tierA.reason))
            is TierAContentRetrievalOutcome.UnsupportedRepresentationVersion ->
                Failed(DocumentAnalysisOutcome.UnsupportedRepresentationVersion(selection.derivativeGenerationId, tierA.version))
        }
    }

    private fun extractPdfText(r: PdfStructuralResult): String = r.documentText

    private fun extractCsvText(r: CsvStructuralResult): String {
        val builder = StringBuilder()
        builder.append(r.headers.joinToString(", "))
        for (row in r.rows) {
            builder.append('\n')
            builder.append(row.joinToString(", "))
        }
        return builder.toString()
    }

    private fun extractEmlText(r: EmlStructuralResult): String {
        val body = r.bodyAlternatives.firstOrNull { it.mediaType.equals("text/plain", ignoreCase = true) }
            ?: r.bodyAlternatives.firstOrNull()
        val builder = StringBuilder()
        r.from?.let { builder.append("From: $it\n") }
        r.to?.let { builder.append("To: $it\n") }
        r.cc?.let { builder.append("Cc: $it\n") }
        r.rawDate?.let { builder.append("Date: $it\n") }
        r.subject?.let { builder.append("Subject: $it\n") }
        if (body != null) {
            builder.append('\n')
            builder.append(body.decodedText)
        }
        return builder.toString()
    }

    private fun extractDocxText(r: DocxStructuralResult): String {
        val builder = StringBuilder()
        r.headers.forEach { hf -> hf.paragraphs.sortedBy { it.order }.forEach { builder.append(it.text).append('\n') } }
        r.paragraphs.sortedBy { it.order }.forEach { builder.append(it.text).append('\n') }
        r.tables.sortedBy { it.order }.forEach { table ->
            table.rows.sortedBy { it.order }.forEach { row ->
                builder.append(row.cells.sortedBy { it.order }.joinToString(" | ") { it.text }).append('\n')
            }
        }
        r.footers.forEach { hf -> hf.paragraphs.sortedBy { it.order }.forEach { builder.append(it.text).append('\n') } }
        return builder.toString()
    }

    companion object {
        /** A modest, frozen, production bound on documents per analysis invocation -- not an enormous theoretical one. Enforced before any derivative retrieval begins. */
        const val MAX_SELECTIONS: Int = 20

        /** A frozen bound on the total extracted-text size of one assembled evidence package. Enforced after retrieval, before model invocation -- per-item size is only knowable once retrieved. No silent truncation: exceeding this fails closed as [DocumentAnalysisOutcome.ContentTooLarge]. */
        const val MAX_TOTAL_CONTENT_CHARACTERS: Int = 200_000

        /** A frozen bound on the local model's own raw response size. Exceeding this fails closed as [DocumentAnalysisOutcome.ResponseTooLarge] -- never truncated and presented as complete. */
        const val MAX_RESPONSE_CHARACTERS: Int = 100_000

        /** A frozen bound on the owner's own analysis instruction. Checked before any retrieval, never truncated. */
        const val MAX_INSTRUCTION_CHARACTERS: Int = 4_000

        /**
         * A frozen, independent, practical production ceiling on the fully assembled, JSON-encoded
         * model prompt -- checked after [DocumentAnalysisPromptBuilder.buildPrompt] actually runs,
         * measured on the real constructed string, never derived from or assumed to equal
         * [MAX_TOTAL_CONTENT_CHARACTERS] + [MAX_INSTRUCTION_CHARACTERS]. [MAX_TOTAL_CONTENT_CHARACTERS]
         * bounds *source* extracted-text size only, before JSON encoding
         * (`DefaultDocumentAnalysisPromptBuilder`'s own `jsonEscape`); escaping -- most sharply, a C0
         * control character becoming a six-character `\uXXXX` sequence -- can expand a
         * source-ceiling-sized package well past this value. That is an accepted, intentional
         * fail-closed outcome ([DocumentAnalysisOutcome.PromptTooLarge]), not a defect: this bound
         * does **not** guarantee every possible [MAX_TOTAL_CONTENT_CHARACTERS]-sized evidence
         * package can be analysed, and this value is deliberately not inflated to try to make that
         * guarantee true for pathological, escape-heavy content -- 224,000 is sized as a sensible
         * practical prompt ceiling for real documents (the actual character-expansion ratio for
         * genuine prose/structured-document text is close to 1:1; heavy control-character content is
         * not realistic evidence and is expected to be rejected here).
         */
        const val MAX_PROMPT_CHARACTERS: Int = 224_000

        // The exact, governed Tier A derivativeKind literals DerivativeGenerationCoordinator's own
        // ingestCsv/ingestEml/ingestDocx/ingestPdf admit with (src/runtime/DerivativeGenerationCoordinator.kt)
        // -- matched against here, never invented or redefined.
        private const val CSV_DERIVATIVE_KIND: String = "CSV structure"
        private const val EML_DERIVATIVE_KIND: String = "EML MIME structure"
        private const val DOCX_DERIVATIVE_KIND: String = "DOCX OOXML structure"
        private const val PDF_DERIVATIVE_KIND: String = "Searchable PDF literal text"
    }
}
