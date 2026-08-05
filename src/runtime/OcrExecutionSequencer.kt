package parker.core.runtime

import parker.core.interfaces.OcrMechanism
import parker.core.interfaces.OcrProviderAdapter
import parker.core.interfaces.OcrRecognitionOutcome
import parker.core.interfaces.OcrRecognitionRequest

/**
 * OCR Mechanism, Implementation Unit 3 ("Internal Execution Sequencer").
 * Governed in full by
 * `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md` ("the Contract
 * Design") Section 3; by
 * `docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md` ("the Scope Lock")
 * Section 4, Section 13; and by
 * `docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan") Unit 3.
 *
 * ## What this Unit implements
 *
 * The sole production implementation of [OcrMechanism]: receive an
 * already-constructed [OcrRecognitionRequest], invoke the single,
 * already-supplied [OcrProviderAdapter], and return whatever
 * [OcrRecognitionOutcome] that adapter produces -- "sequence the OCR
 * mechanism's own single operation... as one capability, one act"
 * (Contract Design Section 3; Implementation Plan Unit 3 Purpose). Holds
 * exactly one dependency, supplied once through the constructor -- no
 * further dependency, mirroring the Implementation Plan's own "Unit 1,
 * Unit 2. No further dependency."
 *
 * ## Exactly one adapter, never a selection among several
 *
 * The constructor accepts exactly one [OcrProviderAdapter] -- never a
 * list, a map, or any other collection an implementation could search,
 * rank, or choose among. This file contains no mechanism of any kind for
 * choosing, locating, or admitting a provider: which concrete adapter
 * this sequencer holds is decided once, entirely outside this class, by
 * whatever constructs it -- a decision this Unit does not make and this
 * file does not implement (Implementation Plan Unit 3's own explicit
 * prohibitions).
 *
 * ## No execution beyond calling the supplied adapter once
 *
 * [recognise] performs exactly one call to [adapter]`.recognise` and
 * returns its result unchanged -- no retry, no timeout wrapping, no
 * scheduling, no background or parallel execution, no queueing, and no
 * caching of any kind. No `try`/`catch` of any kind wraps that call: a
 * genuine fault the adapter itself throws propagates unchanged, mirroring
 * exactly `EvidenceRegistrationCoordinator`'s and
 * `EvidenceExtractionCoordinator`'s own identical, already-established
 * discipline ("No `try`/`catch` anywhere in this sequence beyond what
 * already exists in the dependencies it calls -- a genuine fault
 * propagates unchanged," Evidence Processing Boundary Clarification
 * Section 7) and the Evidence Intelligence Contract Design's own
 * identical treatment of a faulting `ReasoningProvider` invocation
 * (Contract Design Section 11 there: "such a fault is... the concrete
 * implementation's concern to signal, by whatever mechanism it chooses,
 * outside the sealed type").
 *
 * ## This is not the deferred OCR orchestration layer
 *
 * This sequencer holds no reference of any kind to `EvidenceCustodian`,
 * `MemoryCore`, Knowledge Memory's submission interface, the Permission
 * Engine, `EvidenceIntelligence`, or any runtime conversation component
 * (Scope Lock Section 13; Implementation Plan Unit 3's own "Files
 * explicitly prohibited"). It is **not** the composition-level
 * coordinator that consumes Evidence Processing's own `RequiresOcr`
 * disclosure -- that coordinator remains Scope Lock Section 18 item 2's
 * own deferred item, not begun here, and never reachable from this file.
 * It performs no self-triggering, no background work, and no independent
 * read path of any kind (Scope Lock Section 4). It constructs no governed
 * record, and decides nothing about whether its own output is worth
 * producing as a candidate (Scope Lock Section 11) -- both remain
 * exclusively Evidence Intelligence's own, later responsibility.
 * No composition-root type of any kind is imported or referenced
 * anywhere in this file (Implementation Plan Unit 12, structurally
 * blocked).
 *
 * @param adapter The single, already-constructed provider adapter this
 *   sequencer delegates every invocation to. Supplied once, by whatever
 *   constructs this class -- never chosen, located, or admitted by this
 *   class itself.
 */
class OcrExecutionSequencer(private val adapter: OcrProviderAdapter) : OcrMechanism {
    override suspend fun recognise(request: OcrRecognitionRequest): OcrRecognitionOutcome =
        adapter.recognise(request)
}
