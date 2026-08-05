package parker.core.interfaces

/**
 * OCR Mechanism, Implementation Unit 2 ("Provider Adapter Abstraction").
 * Governed in full by
 * `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md` ("the Contract
 * Design") Sections 3, 11, 12-14; by
 * `docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md` ("the Scope Lock")
 * Sections 13, 14, 16; and by
 * `docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan") Unit 2.
 *
 * ## What this Unit implements
 *
 * [OcrProviderAdapter] -- the one boundary a future concrete provider's
 * own types are permitted to exist behind, mirroring exactly how
 * `TikaEvidenceExtractor` is the sole file permitted to import Apache
 * Tika's own types (Evidence Processing Boundary Clarification Section
 * 3; Implementation Plan Unit 2 Purpose). This file defines the
 * abstraction only -- no concrete adapter satisfying it exists anywhere
 * in this repository yet. A concrete adapter (an OCRmyPDF wrapper, a
 * Tesseract wrapper, or any other) is explicitly deferred future work
 * (Scope Lock Section 18, item 6; Implementation Plan Section 16, item
 * 6) -- "Unit 2's own abstraction may proceed without this; a concrete
 * adapter satisfying it may not be written under this plan alone."
 *
 * ## Exactly [OcrMechanism]'s own request and outcome shapes -- no new type
 *
 * Implementation Plan Unit 2 fixes this Unit's own dependencies, inputs,
 * and outputs precisely: "Dependencies. Unit 1's own shapes only. Inputs.
 * Unit 1's own request shape. Outputs. Unit 1's own result/failure
 * shapes." This interface therefore introduces no new public type of its
 * own -- it reuses [OcrRecognitionRequest] and [OcrRecognitionOutcome]
 * exactly as [OcrMechanism] itself already does, never a parallel or
 * provider-adjacent shape.
 *
 * ## No dependency of any kind
 *
 * Mirroring [OcrMechanism]'s own "pure callee, calls nothing" shape
 * exactly (Contract Design Section 12; Scope Lock Section 13, applied
 * here identically): [OcrProviderAdapter] declares no constructor, no
 * property, and no dependency on `EvidenceCustodian`, `MemoryCore`,
 * Knowledge Memory's submission interface, the Permission Engine,
 * `EvidenceIntelligence`'s own result handling, any runtime conversation
 * component, any reporting mechanism, or Docling -- at any depth. It
 * holds no reference back to [OcrMechanism] itself, and no reference to
 * any runtime or composition-root component.
 *
 * ## Provider neutrality is structural, not conventional
 *
 * No concrete OCR engine, library, or service -- OCRmyPDF, Tesseract,
 * EasyOCR, PaddleOCR, or any other -- is named, selected, evaluated,
 * ruled in, or ruled out anywhere in this file (Scope Lock Section 14).
 * This abstraction is, by construction, structurally incapable of
 * selecting or preferring any engine: it declares no default
 * implementation, no factory, no registry, and no configuration -- there
 * is no code path here capable of embedding a preference, because there
 * is no executable code here at all. Provider replacement must never
 * require a constitutional change (Scope Lock Section 14, citing the
 * Constitution's own "Replaceable reasoning providers" principle,
 * "Parker's authority, safety guarantees, and behavioral contracts do
 * not depend on which provider is plugged in") -- any future concrete
 * adapter satisfying this interface may be replaced by another without
 * touching [OcrMechanism], [OcrRecognitionRequest], or
 * [OcrRecognitionOutcome].
 *
 * ## No implementation, no runtime wiring, no orchestration in this Unit
 *
 * This file contains no concrete implementation of [OcrProviderAdapter],
 * no execution sequencing (Implementation Plan Unit 3), no runtime
 * composition of any kind (Unit 12, structurally blocked), no
 * configuration, and no Evidence Intelligence integration. It is not,
 * and must never be confused with, the composition-level coordinator
 * Scope Lock Section 18 defers.
 *
 * ## Why `recognise` is `suspend`
 *
 * For consistency with [OcrMechanism.recognise] itself, which a future
 * Implementation Plan Unit 3 sequencer will invoke in turn -- a shape
 * decision, not a behavioural one, mirroring exactly the reasoning
 * [OcrMechanism]'s own KDoc already gives.
 */
interface OcrProviderAdapter {
    suspend fun recognise(request: OcrRecognitionRequest): OcrRecognitionOutcome
}
