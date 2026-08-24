# Document Ingestion — Tier B Durable OCR Derivative Content Scope Lock

## Status

**Draft for owner review. Not yet accepted, canonical, or
implementation-authorising.** Governance only. No Kotlin is implemented,
proposed as a diff, or changed by this document. No dependency is added.
No interface is implemented. No storage directory is created. No runtime,
test, Docker, or `.env` file is touched. Memory Core, Knowledge, QMD, and
RKS are not modified. OCR is not redesigned.

**This is the second correction pass on this document, following a
second independent review that returned "NOT READY — CORRECTIONS
REQUIRED."** The first pass's central architectural fix — the unified
`DerivativeGenerationId`/`DerivativeGenerationRecord` model, replacing a
prior draft's incorrect parallel-identity design — is **confirmed cured**
and is **not reopened** by this pass. This pass corrects one remaining
blocker (a real Permission Engine authorization gap in the new durable
operation) and ten further major/minor/editorial findings, each verified
against fresh primary-source inspection before being accepted, exactly as
the prior pass was.

This document responds to the governing task **"TIER B DURABLE OCR
DERIVATIVE CONTENT PERSISTENCE AND RETRIEVAL GOVERNANCE SCOPE-LOCK
UNIT."**

**This document reopens, redesigns, or reinterprets none of:** CDR-006,
CDR-007, CDR-008, `OCR_MECHANISM_CONTRACT_DESIGN.md`,
`OCR_MECHANISM_SCOPE_LOCK.md`,
`OCR_MECHANISM_UNIT_12_RUNTIME_INVOCATION_SCOPE_LOCK.md` ("Unit 12"),
`EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`,
`EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`,
`DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md` ("the
Record Scope Lock"), `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`,
`DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md`,
`DOCUMENT_INGESTION_MEMORY_CORE_CROSS_REFERENCE_SCOPE_LOCK.md`,
`DOCUMENT_INGESTION_DERIVATIVE_TO_MEMORY_CORE_REGISTRATION_SCOPE_LOCK.md`,
`DOCUMENT_INGESTION_PROGRAMME_GOVERNANCE_CLOSURE.md`,
`DOCUMENT_INGESTION_PROGRAMME_IMPLEMENTATION_CLOSURE.md`, or the Tier A
Content Scope Lock itself. It does not redesign the Permission Engine
and introduces no new permission vocabulary — §9, below, reuses the
existing, already-registered `EvidenceIntelligenceInvocationGate`
convention exactly.

## 1. Purpose

Determine and freeze the minimum governed architecture required for
Parker to persist and retrieve the derivative content produced by its
**existing** Tier B OCR mechanism, so that:

> Evidence Custodian source → Permission Engine authorisation → explicit,
> structurally owner-authorised Tier B OCR execution →
> `DerivativeGenerationId` → governed `DerivativeGenerationRecord` →
> subordinate durable Derivative Content Entry, keyed by that same
> `DerivativeGenerationId` → restart → retrieval of that same durable OCR
> result by known identity → no OCR rerun required merely to view/retrieve
> it.

This document does **not** redesign OCR, does **not** authorise automatic
OCR, does **not** authorise reasoning-provider submission, and does
**not** promote OCR output into Memory or Knowledge. It resolves the one
open question Unit 12 §13 itself left to future, separate Document
Ingestion governance — whether Document Ingestion may construct its own
`DerivativeGenerationRecord` for a Tier B result — by adopting Parker's
existing, unified derivative-generation architecture, not by inventing a
second one.

## 2. Governing documents inspected fresh, this pass

- `DOCUMENT_INGESTION_DERIVATIVE_CONTENT_PERSISTENCE_RETRIEVAL_SCOPE_LOCK.md`
  — full. Storage authority, cardinality, Versioned Storage
  Representation and digest semantics, content-before-record write
  ordering, retrieval scope, owner authority, retention minimums,
  sensitive-content controls, and its own Tier B deferral.
- `OCR_MECHANISM_UNIT_12_RUNTIME_INVOCATION_SCOPE_LOCK.md` — full, §13
  in particular (both paragraphs — first paragraph describes what Unit
  12 itself authorises for runtime invocation, none; second paragraph
  explicitly reserves a future Document-Ingestion-side
  `DerivativeGenerationRecord` for exactly this document to resolve, and
  this document now does). §5 ("the existing `(EXECUTE, DOCUMENT)`
  invocation-level gate, already registered... already governs every
  `analyseEvidence` call regardless of `analysisKind`" — the load-bearing
  citation for §9's correction, below).
- **`EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` §6 step 0, §7, §11** — fresh-read
  in full this pass (the correction's own primary source for §9, below):
  "whatever composes Evidence Intelligence into the running system
  evaluates a dedicated Permission Engine proposal class... before
  calling the Evidence Intelligence operation" — a requirement on **any**
  composing caller of Evidence-Intelligence-adjacent machinery, not only
  `analyseEvidence` itself.
- **`src/runtime/EvidenceIntelligenceInvocationGate.kt`** — fresh-read in
  full this pass. A Kotlin `object` (structurally incapable of holding a
  `PermissionEngine` reference itself), exposing a fixed
  `EVIDENCE_INTELLIGENCE_INVOCATION_RESOURCE_ID` and
  `ANALYSE_ACTION_NAME`, and one pure function,
  `buildExecutionRequest(requestingPrincipalId): ExecutionRequest` — it
  builds the request; it does **not** call
  `PermissionEngine.evaluate`. That call is explicitly left to "whatever
  Unit 8 introduces" as Evidence Intelligence's composing caller — this
  document's new operation (§9, below) is now a second such composing
  caller, and inherits the identical obligation.
- **`src/composition/ParkerRuntime.kt`, fresh-read this pass** —
  `analyseEvidence` (~line 1891) calls
  `permissionEngine.evaluate(EvidenceIntelligenceInvocationGate.buildExecutionRequest(requestingPrincipalId))`
  before ever calling `evidenceIntelligence.analyse(request)`; on any
  decision other than `APPROVED`/`APPROVED_WITH_CONFIRMATION`, it returns
  `EvidenceIntelligenceInvocationOutcome.NotAuthorised(...)` and performs
  no further work. The real, already-registered policy rule
  (`PermissionPolicyRule(action = EXECUTE, resourceType = DOCUMENT,
  outcome = APPROVED, level = AUTOMATIC)`, confirmed present in the real
  composition graph) currently approves this request unconditionally for
  any principal — but the **evaluation itself is real and mandatory**,
  not merely a policy-rule formality; a future change to that rule (for
  example, narrowing it, or introducing a per-principal condition) is
  fully effective the moment it changes, precisely because the
  evaluation is genuinely performed on every call. **Contrasted, fresh,
  against `invokeTierAIngestionAsOwner`** (~line 1722): that method
  performs **no** `permissionEngine.evaluate` call of any kind — it
  resolves `PrincipalId(config.ownerPrincipalId)` and delegates directly.
  This is not a contradiction: Tier A's own document-ingestion machinery
  carries no applicable Permission Engine gate of its own, while
  Evidence-Intelligence-adjacent machinery (which Tier B's OCR execution
  runs through) does, by the Evidence Intelligence Scope Lock's own
  explicit requirement, cited above. §9, below, corrects the prior pass's
  error of treating these two, structurally different cases as
  interchangeable.
- **`src/runtime/DoclingOcrProviderAdapter.kt`, fresh-read this pass** —
  the load-bearing citation for §10's correction, below.
  `buildRecognitionOutcome` (~line 322): `OcrRecognitionIdentity.mechanismIdentity`
  is always the fixed constant `DOCLING_MECHANISM_IDENTITY = "docling"`.
  `OcrRecognitionIdentity.mechanismVersion` is `parsed.mechanismVersion`
  — the bridge script's own disclosed Docling product version.
  `OcrRecognitionIdentity.configurationProfile` is, when the bridge
  discloses a model identity (`parsed.modelIdentity != null`), the
  adapter's own configured profile string **with a `;model=<modelIdentity>`
  suffix appended** (`"${configuration.configurationProfile};model=${parsed.modelIdentity}"`);
  otherwise, the bare configured profile string, unsuffixed. **No field
  named `modelIdentity` or `modelVersion` exists anywhere on
  `OcrRecognitionIdentity` itself** — a real model identity, when the
  bridge discloses one, is genuinely present as data, but embedded as a
  substring of `configurationProfile`, never its own field; a model
  *version*, distinct from `mechanismVersion` (Docling's own product
  version), is not captured anywhere in the current implementation at
  all, embedded or otherwise.
- **`src/runtime/FileSystemDerivativeGenerationStorage.kt`, fresh-read
  this pass** — the load-bearing citation for §18's new section, below.
  `requireSafe(id)`: `SAFE_IDENTIFIER = Regex("^[a-z0-9_-]+$")`,
  `RESERVED_NAMES = setOf("con", "prn", "aux", "nul")`, plus a numbered-reserved-name
  check (`com1`-`com9`, `lpt1`-`lpt9`); an id failing any of these throws
  `DerivativeGenerationStorageException.UnsafeIdentifier` before any
  filesystem path is ever constructed from it. `FileSystemDerivativeContentStorage.kt`
  applies the identical discipline.
- `DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md` — full.
  §5 (`DerivativeGenerationId`), §6 (immutability), §9 (content
  identity/digest), **§10 (producer identity — plugin/version mandatory,
  adapter identity/version mandatory where applicable, model
  identity/version **conditionally mandatory whenever the transformation
  is Tier B**, re-read with particular care this pass — this is the
  requirement §10/§11, below, resolve honestly rather than fabricate)**,
  §11 (transformation vocabulary — closed, already includes `OCR` and
  `MODEL_INFERENCE`), §12 (three-timestamp discipline), §13
  (completeness/warnings — five-value vocabulary by reference), §14
  (structural provenance), §15 (confidence), §18 (authorization), §19
  (failure atomicity), §20 (frozen field-shape taxonomy, never touched by
  this document).
- `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` — Invariant I-7, I-11, I-15.
- `DOCUMENT_INGESTION_PROGRAMME_IMPLEMENTATION_CLOSURE.md` — the "Future
  Tier B `DerivativeGenerationRecord` mechanics" deferred item this
  document resolves.
- `DOCUMENT_INGESTION_MEMORY_CORE_CROSS_REFERENCE_SCOPE_LOCK.md`,
  `DOCUMENT_INGESTION_DERIVATIVE_TO_MEMORY_CORE_REGISTRATION_SCOPE_LOCK.md`
  §6 (Tier A `Admitted`-only eligibility, unaffected by this document).
- `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` — the four-way
  `EvidenceAnalysisResult` taxonomy; "Evidence Intelligence performs only
  the first [proposal], never acceptance."
- `DOCUMENT_INGESTION_GOVERNANCE_AMENDMENT_MAP.md` Amendment 5 — CDR-007's
  assignment of OCR/transcription/extraction to Evidence Intelligence.
- CDR-006, CDR-007, CDR-008 (`docs/decisions/`) — unreopened.

## 3. Fresh implementation inspection

- `src/interfaces/OcrMechanism.kt` — `OcrMechanism.recognise(request):
  OcrRecognitionOutcome`; `OcrRecognitionResult(recognisedText: String,
  fidelity: TranscriptionFidelity, identity: OcrRecognitionIdentity,
  confidence: Double? = null, recognisedAt: Instant, warnings: List<String>
  = emptyList(), segments: List<OcrRecognitionSegment> = emptyList())`;
  `OcrRecognitionIdentity(mechanismIdentity, configurationProfile,
  mechanismVersion?)` — **exactly these three fields, no more**, verified
  this pass against the actual `data class` declaration; `OcrRecognitionSegment(text,
  fidelity, pageNumber?)`. "No bounding box, coordinate, heading, table,
  or other document-structure concept is represented here, and none may
  be added under this Unit"; `confidence`'s own KDoc: "never durable,
  never written to `CandidateAssertion.confidence` or any other durable
  field."
- **`OcrRecognitionOutcome`, the full nine-way sealed class**:
  `Recognised(result: OcrRecognitionResult)`; `Failed(reason: String)`;
  `NotAuthorised(reason: String)`; `UnsupportedOrInaccessibleInput(reason: String)`;
  `NoRecognisableContent(reason: String)`; `PartialOrDegradedOutput(partialResult:
  OcrRecognitionResult, reason: String)` — a full `OcrRecognitionResult`
  plus a mandatory, non-blank degradation reason; `ValidationRejection(reason: String)`;
  `ProcessingOrDependencyFailure(reason: String)`; `GenuineImplementationFault(reason: String)`.
- `src/runtime/EvidenceIntelligenceOcrCoordinator.kt` — `internal class`,
  exactly two constructor dependencies (`EvidenceCustodian`,
  `OcrMechanism`). Performs the manifest-verified integrity sequence
  before ever calling `ocrMechanism.recognise`. Returns `internal sealed
  class OcrCoordinatorOutcome` (`ManifestNotFound`, `ManifestRejected`,
  `ByteLengthMismatch`, `DigestMismatch`, `NotOcrEligible`,
  `Recognised(outcome: OcrRecognitionOutcome)`). No `try`/`catch` around
  the `ocrMechanism` call.
- `src/interfaces/EvidenceIntelligence.kt`, `src/runtime/DefaultEvidenceIntelligence.kt`,
  `src/runtime/EvidenceIntelligenceAcceptanceCoordinator.kt` — the
  existing, unchanged transient path: `TransientOutput` →
  `NotDispatched`. **Current production behaviour: a successful OCR
  recognition, right now, is never durable in any form, anywhere, once
  the single `analyseEvidence` call that produced it returns.**
- `src/composition/ParkerRuntime.kt`'s `analyseEvidence` and
  `invokeTierAIngestionAsOwner` — both fresh-read and contrasted, §2
  above.
- `src/ui/parker/ui/OwnerEvidenceUpload.kt` — `TierBProcessingOutcome`:
  `Completed(resultCount: Int)`; `NotAuthorised(reason: String)`;
  `Failed(safeMessage: String)`. `resultCount` is only a count.
- `src/composition/OwnerEvidenceHttpServer.kt` — `handleOcr` (~line
  277); browser `ocrRow()` (~line 1049) sets only `row.status`/`row.message`,
  never `row.content`; the "View Extracted Content" action's own
  `derivativeGenerationId` gate is never satisfied by a Tier B row today.
  **This same file's own real, already-fixed `handleRetrieveContent`
  path is the identifier-safety precedent §18, below, reuses**: it
  catches `DerivativeGenerationStorageException.UnsafeIdentifier` and
  `DerivativeContentStorageException.UnsafeIdentifier` specifically and
  returns `400`, rather than letting a hostile path-shaped identifier
  reach any filesystem operation.
- `src/runtime/DerivativeGenerationCoordinator.kt`,
  `src/runtime/FileSystemDerivativeGenerationStorage.kt`,
  `src/runtime/FileSystemDerivativeContentStorage.kt`,
  `src/runtime/DerivativeContentCodec.kt`, `src/runtime/DoclingOcrProviderAdapter.kt`
  — re-inspected fresh, §2 above, for §10 (provenance), §17
  (bounds), §18 (identifier safety), and §19 (write ordering), below.
  Confirmed: `GovernedTierADocumentIngestionRouter` maps every
  `AdmittedAuditFailed` variant to `TierADocumentRoutingResult.ReconciliationRequired(format,
  record, payload, reason, mediaFacts)` — the real, already-accepted
  reconciliation precedent §22, below, reuses. Confirmed: `FileSystemDerivativeContentStorage.prepare()`
  deletes its own `.tmp` file in a `finally` block regardless of
  outcome; neither `prepare()` nor `publishPrepared()` deletes a
  `.prepared`-staged file that was successfully written but never
  promoted — no automatic sweep exists anywhere in this codebase today.

**Disclosed factual finding, re-verified.** No OCR recognised text is
displayed to the owner anywhere today, transient or durable. This
document, once adopted by a future implementation, both adds the durable-retrieval
capability and, as a direct consequence, first-time display of OCR text
to the owner at all.

**Current audit behaviour.** No `DocumentIngestionAudit` record is
written anywhere on the Tier B path today.

## 4. Authority model — frozen

- **Source Evidence** — Evidence Custodian remains the sole authority for
  original admitted source bytes (CDR-006, unreopened).
- **Derivative Generation identity and storage** (`DerivativeGenerationId`,
  `DerivativeGenerationStorage`, `DocumentIngestionAudit`) — Document
  Ingestion's own, already-accepted, unified machinery, extended by this
  document to Tier B (§5-§9, below), resolving Unit 12 §13's own
  explicitly deferred question.
- **Derivative Content** (`DerivativeContentStorage`, already authorised
  for Tier A) — extended to a new Tier B content kind, keyed by the same
  `DerivativeGenerationId`.
- **Tier B OCR mechanism output** (`OcrRecognitionResult`/`OcrRecognitionOutcome`)
  — owned by the OCR mechanism itself until disclosed to its caller
  (Contract Design §2/§8; unreopened).
- **Evidence Intelligence invocation authority** (`EvidenceIntelligenceInvocationGate`,
  evaluated via the real `PermissionEngine`) — the existing, already-registered
  `(EXECUTE, DOCUMENT)` gate, which **this document's new durable
  operation must also evaluate** (§9, below) — it does not create a new
  authority, it reuses the one that already, correctly, governs
  Evidence-Intelligence-adjacent invocation.
- **Transient Evidence Intelligence analysis** (`EvidenceAnalysisResult.TransientOutput`,
  reached via `analyseEvidence`) — entirely unchanged.
- **Tier B Durable Generation** (new, this document) — Document
  Ingestion owns a `DerivativeGenerationRecord` and subordinate
  Derivative Content entry for a specific, already-completed,
  Permission-Engine-authorised and structurally owner-authorised OCR
  execution.
- **Memory Core / Knowledge / QMD / RKS** — no new authority.

## 5. Tier A architecture-reuse result

| Tier A element | Reusable for Tier B? | Why |
| --- | --- | --- |
| `DerivativeGenerationId` as the keying identity | **Yes** | Unit 12 §13's second paragraph explicitly reserves this question for a future Document Ingestion unit; this is that unit |
| `DerivativeGenerationStorage`/`FileSystemDerivativeGenerationStorage` (the actual store) | **Yes, the same instance** | A single, unified generation-identity space is simpler and more auditable than a second parallel store |
| `DocumentIngestionAudit`/`FileSystemDocumentIngestionAudit` (the actual audit port) | **Yes, the same instance** | Same reasoning |
| `prepare → ADMISSION_AUTHORISED → publish → ADMITTED` sequence | **Yes, reused unchanged** | Format-agnostic already (§19, below) |
| `DerivativeContentStorage`/`DerivativeContentEntry` shape | **Yes**, extended with a new content kind | Nothing OCR-specific conflicts with the existing discipline |
| Versioned Storage Representation concept | **Yes**, one new representation kind | Extends the same already-adopted provenance-model language |
| Storage-integrity-only digest semantics | **Yes, unchanged, mandatory** (§16, below) | No OCR-specific reason to weaken it |
| Retrieval coordinator pattern | **Yes**, extended to resolve the generation record first, then content, then verify kind (§24, below) | Satisfies the restart-proof requirement |
| Owner-authority pattern (no caller-supplied principal) | **Yes, unchanged in discipline**, applied to the *new* operation (§9, below) — **but no longer treated as a substitute for Permission Engine evaluation** | Corrected this pass: structural owner-only and Permission-Engine-authorised are two distinct, both-required guarantees, not one |
| HTTP retrieval model | **Yes, the same shape** | Reuses the existing owner HTTP authentication path |
| `DerivativeGenerationRecord` field-shape (identity §10-§11, transformation vocabulary §14, structural provenance §14) | **Yes, the literal type** | Tier B receives a real `DerivativeGenerationRecord` |
| `DerivativeGenerationRecord`'s own confidence field | **No, not populated for Tier B** | §16, below, states this narrowly |

**Conclusion, unchanged from the first correction pass, not reopened.**
Reuse the unified `DerivativeGenerationId`/`DerivativeGenerationStorage`/`DocumentIngestionAudit`/`DerivativeContentStorage`
architecture in full.

## 6. Core authority question — what is a durable Tier B OCR result?

**A durable Tier B OCR result is a `DerivativeGenerationRecord` and its
subordinate Derivative Content entry, both minted through Document
Ingestion's own unified generation machinery, recording what one
specific, Permission-Engine-authorised and structurally owner-authorised
OCR execution truthfully produced — entirely independent of, and never
substituting for, whatever Evidence Intelligence itself does with its
own, unchanged `TransientOutput` wrapping of a separate recognition
reached through `analyseEvidence`.**

It **IS NOT**: original source evidence (CDR-006, unreopened); a
replacement or corrected source (Provenance Model I-7); canonical truth
about the source (§15, below); evidential truth of any kind (CDR-007);
Memory, Knowledge, a QMD/RKS record, or a reasoning conclusion (§31-§33,
below); a human-verified transcription; an `EvidenceAnalysisResult.CandidateArtifactProduced`/`CandidateRecordProduced`
or any Memory Core write; indistinguishable from a Tier A generation
(§28, below).

## 7. Source / derivative relationship — frozen

Every persisted Tier B `DerivativeGenerationRecord` **must** correspond
to an already-custodied `EvidenceArtifactId` whose authoritative source
manifest independently verifies — the exact same manifest-verified
sequence `EvidenceIntelligenceOcrCoordinator` already performs. The
relationship is the **same** chain Tier A already uses:
`EvidenceArtifactId` → `DerivativeGenerationId` →
`DerivativeGenerationRecord` → subordinate Derivative Content entry.

**No orphan durable Tier B payload may be casually authorised.** A
Derivative Content entry whose `derivativeGenerationId` names no
resolvable `DerivativeGenerationRecord` is a genuinely reachable
reconciliation state (§22, below), never a permanently tolerated
"orphan by design."

## 8. Generation identity — frozen

- Each explicit, authorised OCR execution mints a distinct new
  `DerivativeGenerationId` at the point its admission sequence begins
  (§19, below) — the same `idFactory` discipline Tier A already uses.
- Repeated, explicit OCR of the same source creates a new, independent
  `DerivativeGenerationId` every time — directly required by Unit 12
  §16's own reprocessing rule.
- Historical durable OCR generations remain immutable (§23, below).
- No OCR run overwrites a previous generation.

## 9. Structural owner authorisation and Permission Engine gating — corrected

**Corrects this pass's own blocker.** The prior pass required this
document's new durable-generation operation to be structurally
owner-only (no caller-supplied principal), and incorrectly treated that
guarantee as sufficient authority, on its own, to bypass the existing
Permission Engine evaluation `analyseEvidence` itself performs before
touching Evidence-Intelligence-adjacent machinery. **These are two
distinct, both-required guarantees, not substitutes for one another:**

- **Structural owner-only** answers "can any principal other than the
  configured owner ever reach this operation?" — no.
- **Permission Engine authorisation** answers "has the applicable,
  already-governed authorisation decision for invoking
  Evidence-Intelligence-adjacent machinery actually been evaluated, this
  call, for this principal?" — a question the first guarantee does not
  answer, and the Evidence Intelligence Scope Lock (§6 step 0, fresh-cited
  §2 above) already requires be answered by *any* composing caller of
  that machinery, not only `analyseEvidence`.

**This document therefore authorises a distinct capability —
illustratively, `invokeTierBOcrDurableGenerationAsOwner(evidenceArtifactId)`
— with the following, corrected, required sequence:**

1. **Resolve the configured owner identity structurally.** No
   `requestingPrincipalId` parameter of any kind; the operation resolves
   `PrincipalId(config.ownerPrincipalId)` internally, exactly as
   `invokeTierAIngestionAsOwner` already does.
2. **Evaluate the existing, applicable Permission Engine authorisation
   before any OCR work.** The operation must call
   `permissionEngine.evaluate(EvidenceIntelligenceInvocationGate.buildExecutionRequest(ownerPrincipalId))`
   — the **same**, already-registered `(EXECUTE, DOCUMENT)` proposal
   class `analyseEvidence` itself already evaluates, using the
   structurally-resolved owner principal from step 1 as the request's
   own principal. **No new `PermissionAction`, `ResourceType`, `Resource`,
   or `ActionVocabulary` entry is introduced** — this reuses the exact
   existing convention, registered once, already in the real composition
   graph (§2, above).
3. **Fail closed on rejection, with zero durable side effects.** Any
   decision other than `APPROVED`/`APPROVED_WITH_CONFIRMATION` returns a
   truthful non-admission outcome immediately — no OCR execution, no
   `DerivativeGenerationId` minted, no content prepared, no record
   prepared, no audit write, no publication. Mirrors exactly how
   `analyseEvidence` itself already handles a non-approved decision.
4. **Only after step 2 succeeds does OCR execution begin** — the
   operation's own next action is to invoke the *same*, unmodified
   `EvidenceIntelligenceOcrCoordinator`/`OcrMechanism.recognise`
   sequence Tier B already uses today. OCR execution remains the
   upstream, explicit processing action, never moved into or made a side
   effect of the derivative-generation machinery itself — only *after*
   that already-completed OCR execution returns does the admission
   sequence (§19, below) begin, recording what the OCR mechanism already,
   truthfully produced.
5. **No caller-supplied principal may control durable Tier B creation**
   under any circumstance, restated for emphasis.
6. **Bearer-token HTTP authorisation is not weakened.** A future HTTP
   surface reuses the existing `Authorization: Bearer <token>`
   discipline, unmodified.
7. **`analyseEvidence`/Evidence Intelligence itself is not globally
   redesigned**, and gains no new call site. The new operation composes
   `EvidenceIntelligenceOcrCoordinator`/`OcrMechanism` and
   `PermissionEngine`/`EvidenceIntelligenceInvocationGate` directly —
   the same lower-level dependencies `DefaultEvidenceIntelligence` and
   `analyseEvidence` themselves already compose, reused, never
   re-implemented.
8. **An authorisation failure is never caught and converted into
   durable success.**
9. **Owner retrieval (§25, below) is likewise structurally owner-only**,
   on a separate operation from durable-generation creation. Retrieval
   of already-admitted content is a read of Document Ingestion's own
   already-admitted durable state, not a fresh invocation of
   Evidence-Intelligence-adjacent machinery — it does not itself require
   a fresh `EvidenceIntelligenceInvocationGate` evaluation, mirroring
   exactly how Tier A's own `retrieveTierAExtractedContentAsOwner`
   requires no fresh Tier A admission-gate evaluation to read
   already-admitted content.

This section deliberately keeps two authority concepts distinct:
**transient Evidence Intelligence analysis authority** (`analyseEvidence`,
unchanged, caller-discipline owner-only, its own Permission Engine gate)
is not the same thing as, and is never conflated with, **durable Tier B
derivative-generation authority** (the new operation this section
authorises — structurally owner-only *and* independently
Permission-Engine-gated, both required).

## 10. Producer / model / provenance — truth table, corrected

**Corrects this pass's own major finding: no equivalence between
mechanism identity and model identity is asserted anywhere below.**
Every field is classified against fresh reading of the actual
implementation (§2-§3, above):

| Field | Currently available? | Exact factual source | Future generation-record requirement |
| --- | --- | --- | --- |
| `recognisedText` | CURRENTLY AVAILABLE | `OcrRecognitionResult.recognisedText` | N/A — already produced |
| Document fidelity | CURRENTLY AVAILABLE | `OcrRecognitionResult.fidelity` | N/A |
| Segments, segment page number | CURRENTLY AVAILABLE, optional | `OcrRecognitionResult.segments` | N/A |
| `confidence` | CURRENTLY AVAILABLE | `OcrRecognitionResult.confidence` | **Must never be persisted** — §16, below |
| `warnings` | CURRENTLY AVAILABLE | `OcrRecognitionResult.warnings` | N/A |
| Outcome kind, degradation reason | CURRENTLY AVAILABLE | The outer `OcrRecognitionOutcome` sealed variant itself | Must be persisted explicitly, §13 below |
| Mechanism identity | CURRENTLY AVAILABLE | `OcrRecognitionIdentity.mechanismIdentity` — for the real Docling adapter, the fixed constant `"docling"` | Satisfies the Record's own "plugin/product identity" requirement directly — this is a product/framework identity, stated as exactly that, never re-labelled as model identity |
| Mechanism version | CURRENTLY AVAILABLE | `OcrRecognitionIdentity.mechanismVersion` — the Docling bridge's own disclosed product version | Satisfies the Record's own plugin-version requirement directly |
| Configuration profile | CURRENTLY AVAILABLE | `OcrRecognitionIdentity.configurationProfile` | Satisfies the Record's own configuration-identity requirement |
| **Model identity** | **NOT CURRENTLY AVAILABLE as a distinct field** — genuinely present as data in one real, inspected adapter, but never exposed independently | For the real Docling adapter (fresh-read, §2 above): when the bridge script discloses one, it is embedded as a `;model=<modelIdentity>` suffix appended to `configurationProfile`'s own string; absent (no suffix) when the bridge discloses none | **FUTURE REQUIRED FOR DURABLE ADMISSION.** A future implementation extracting a Tier B Record's own `modelIdentity` value must parse this exact, disclosed substring convention — never fabricate a value, never substitute `mechanismIdentity`, and must fail closed (§11, below) when no `;model=` suffix is present |
| **Model version** | **NOT CURRENTLY AVAILABLE at all** | No field, embedded or otherwise, captures a model version distinct from `mechanismVersion` in the current implementation | **FUTURE REQUIRED FOR DURABLE ADMISSION**, currently unsatisfiable — §11, below, states the honest consequence |
| Adapter identity/version | Not present as a field distinct from mechanism identity | Internal Docling adapter composition detail, never disclosed to a caller (OCR Scope Lock §13, unreopened) | Where the Record's own "adapter identity/version where applicable" requirement genuinely applies, a future implementation states its own real source honestly — this document does not invent one |
| Producer identity, as a whole structured record | Assembled from the fields above, not a single pre-built value | N/A — a future implementation's own mapping step | Must be assembled honestly from the fields actually available (§11, below, governs the model-identity/version gap specifically) |
| Transformation history | Not produced by `OcrRecognitionResult` itself | Governed vocabulary already includes `OCR`/`MODEL_INFERENCE` | Must include `OCR` at minimum; `MODEL_INFERENCE` only where the concrete adapter is genuinely model-backed, stated truthfully by the implementation, never defaulted |
| Completeness state | Not produced as a Routing/Completeness-Policy value | The five-value vocabulary already exists | §14, below |
| Page count (request-supplied) | Present only on `OcrRecognitionRequest.pageCount?` — caller-supplied, unauthoritative | An input fact, never an output fact | **Must never be treated as coverage evidence** — §14, below, corrects this explicitly |
| Source media type | Not on the OCR result at all | The source manifest's own declared/detected media type — an already-governed input fact | A future implementation records source media facts from the governed source manifest, never from the OCR result |
| `recognisedAt` | CURRENTLY AVAILABLE | `OcrRecognitionResult.recognisedAt` | Satisfies the Record's own "generation time" requirement directly |

**No inferred equivalence appears anywhere in this table.** Model
identity's real, disclosed source (the `configurationProfile` substring
convention) is stated as exactly what it is — a genuinely derivable fact
from the real, inspected adapter — never conflated with, or substituted
by, mechanism identity. Model *version* has no real source at all today,
and this table does not invent one.

## 11. Mandatory-provenance fail-closed rule — new, normative

**Corrects the prior pass's silence on this consequence.** The Record
Scope Lock §10 makes model identity **and** version conditionally
mandatory whenever the transformation is Tier B. §10, above, establishes,
honestly, that:

- model identity is genuinely derivable today, for the real Docling
  adapter, via the disclosed `configurationProfile` substring convention
  — but only when the bridge itself discloses one for that execution;
- model **version** has no genuine source at all in the current
  implementation.

**The rule this document freezes, without exception:** if a field the
Record Scope Lock requires as mandatory for a valid Tier B
`DerivativeGenerationRecord` cannot be truthfully populated from the
actual OCR result for a given execution, the structurally owner-only
durable-generation operation (§9, above) **must fail closed** — a
distinct, honest `MandatoryProvenanceUnavailable` outcome, before any
`DerivativeGenerationId` is minted, before any content or record is
prepared. **Fabricating a placeholder value (including, explicitly, the
string `"unknown"`) to satisfy this requirement is never authorised, by
this document or by any future implementation relying on it.**

**Disclosed, honest consequence.** Under current implementation facts
(§10, above), model *version* has no source at all — meaning this
fail-closed outcome is, today, reachable on every execution, until
either: (a) a future, separate OCR governance unit extends
`OcrRecognitionIdentity` with a genuine, distinct model-version field
(this document does not perform that extension), or (b) a future,
separate governance decision — explicitly not made by this document,
which declines to infer it — determines that a specific mechanism's own
version genuinely stands in for its model's own version for Tier B
purposes, and says so as its own, freestanding ruling, not as something
this document infers by default. This document's own architecture,
authorisation, ordering, retrieval, and safety requirements (§4-§30)
are fully specified and implementable regardless of when that gap
closes; only the final admission step is gated on it, honestly.

## 12. Permitted Tier B content fields

**Permitted:** `recognisedText`, fidelity, `identity` (mechanism
identity/version, configuration profile — as truthfully available,
§10-§11 above), `recognisedAt`, `warnings`, `segments`, outcome kind,
degradation reason where present, the source `EvidenceArtifactId`, the
`DerivativeGenerationId`, `representationVersion` (§15, below).

**Prohibited — technically available, not authorised for durability:**
`confidence` (§10, above; never persisted, §16 below).

**Does not exist in the governed shape at all:** coordinates, bounding
boxes, or any layout/document-structure concept; raw image bytes,
rendered page images, or any byte-backed field; temporary Docling files,
intermediate OCR engine artifacts, model/cache files, or raw engine
responses.

## 13. Clean vs. degraded outcome — preserved, never collapsed

A durable Tier B generation must record, explicitly, which of exactly
two admissible outcome kinds produced it: **`RECOGNISED`** (the OCR
execution returned `Recognised`) or **`PARTIAL_OR_DEGRADED`** (the
execution returned `PartialOrDegradedOutput`, carrying a mandatory,
persisted degradation reason). A degraded result must never become
indistinguishable from a clean recognised result merely because it was
persisted.

**Non-admissible outcomes produce no durable generation at all:**
`NoRecognisableContent`, `UnsupportedOrInaccessibleInput`,
`ValidationRejection`, `ProcessingOrDependencyFailure`,
`GenuineImplementationFault`, `Failed`, `NotAuthorised` — none of these
may ever produce a `DerivativeGenerationRecord`, a Derivative Content
entry, or any durable state of any kind.

## 14. Completeness and transformation history — corrected

**Corrects this pass's own major finding.** The prior pass's unconditional
`RECOGNISED → AccountedFor` mapping is removed. **`AccountedFor` may be
assigned to a Tier B generation only when governed coverage evidence
genuinely proves all relevant source content/pages were accounted for.**
No field in the current, governed OCR result shape provides that
evidence (§10, above — `pageCount` is a caller-supplied, unauthoritative
request fact, never an output fact, and must never be treated as
coverage proof). Consequently, under current governed facts:

- **`RECOGNISED`** (clean, no degradation reported) → `AccountedForWithQualifications`
  — the mechanism reported no degradation, but no independent evidence
  proves complete source coverage (for example, a silently-skipped page
  the mechanism itself never disclosed skipping).
- **`PARTIAL_OR_DEGRADED`** → `AccountedForWithQualifications`, **never**
  `AccountedFor` under any circumstance — the degradation reason (§13,
  above) is carried as, or alongside, the generation's own warnings.
- **`AccountedFor` becomes reachable only once a future, separately
  governed coverage-evidence capability exists** — for example, a
  genuinely disclosed, independently verifiable "pages processed vs.
  pages present in source" fact. This document does not create that
  capability, and does not itself decide what would satisfy it; it
  freezes only that `AccountedFor` requires real evidence, never a
  mechanism's own silence about degradation.
- `NotAssessable` remains available for a future implementation to use
  truthfully where even the qualified classification above cannot be
  supported — this document does not require it be used in preference to
  `AccountedForWithQualifications` for the two admissible outcome kinds
  above, only that it remain an honest option should a future
  implementation encounter a case this document has not anticipated.

**Transformation history** must include `OCR` at minimum;
`MODEL_INFERENCE` only where the concrete, composed adapter is genuinely
model-backed, stated truthfully, never defaulted either way. **Warnings**
are the OCR result's own `warnings` list, order preserved, plus the
degradation reason when present.

**This section governs only generations that reach admission at all** —
per §11, above, that remains gated on the mandatory-provenance
requirement; this section does not, by itself, make any generation
admissible that §11 would otherwise block.

## 15. Representation versioning

Extends the Tier A Content Scope Lock's concept, unmodified in
substance: no canonical serialization declared; explicit
`representationVersion` required, permanent, immutable; unsupported
versions fail closed; no silent rewriting of historical entries.

## 16. Storage-integrity digest — mandatory, not optional

For every durably admitted Tier B Derivative Content entry, a SHA-256
digest over the Versioned Storage Representation's own serialized bytes
**MUST** be computed and recorded, and **MUST** be verified before any
decoded field is trusted on read — reusing the Tier A content codec's
own existing mechanism unchanged. It proves **exactly one thing**: the
persisted storage bytes have not been altered or corrupted since
written. It proves **none** of: source-byte identity; OCR correctness;
evidential correctness; human verification; semantic equivalence across
representation versions; or the recognition's own `confidence`
(excluded from durability entirely, §10 above).

**On the confidence exclusion's own wording, corrected this pass.** This
Tier B scope elects not to populate the `DerivativeGenerationRecord`'s
optional `confidence` field, narrowly, because the governing OCR
contract for this mechanism (Contract Design §9, Scope Lock §6)
prohibits durable confidence for its own output. This is not asserted as
a general precedence rule between governance documents — only as this
specific document's own considered election, for this specific field,
given that specific, unambiguous prohibition.

## 17. Representation size bounds — mandatory freezing before implementation

The 20 MiB `recognisedText` ceiling (reused from Unit 12 §14, unchanged)
and the 64 MiB total-entry ceiling (reused from the Tier A content
codec's own `MAX_ENTRY_BYTES`, unchanged) remain valid outer limits.

**Corrected this pass: freezing further limits is a mandatory
precondition for implementation authorisation, not an encouragement.** A
future implementation plan relying on this document **must**, before any
code implementing durable Tier B admission is authorised, freeze
explicit, finite, justified numeric maximums for at least:

- segment count;
- individual segment text length;
- warning count;
- individual warning length;
- metadata/configuration/provenance string length (mechanism identity,
  configuration profile, mechanism version);
- any other repeated collection or free-form string appearing in the
  persisted representation.

Each such limit **must**: be finite; fit within the 64 MiB total-entry
ceiling alongside every other bounded field; cause the future
implementation to reject an oversized representation, in full, before
durable publication; never permit silent truncation reported as
complete. A future implementation plan that leaves any of the above
limits unfrozen, "encouraged," or merely aspirational does not satisfy
this document, and durable Tier B admission may not be authorised to
proceed against such a plan.

## 18. Identifier safety — normative, real section

**New this pass, correcting the prior pass's own broken cross-reference
(it cited a "§25 identifier safety" section that did not exist — its
actual §25 was browser semantics).** A future implementation of durable
Tier B generation and content storage/retrieval **must**:

1. use only Parker-governed identifiers — `EvidenceArtifactId` and
   `DerivativeGenerationId`, both opaque, Parker-minted (never
   plugin-supplied, never caller-chosen) — as the sole basis for any
   filesystem-facing operation;
2. accept no arbitrary filesystem path input, from any source, at any
   layer;
3. accept no client-provided server path of any kind through any HTTP
   surface;
4. validate every identifier for safety **before** it is used to resolve
   any filesystem location — reusing the exact, already-implemented
   discipline `FileSystemDerivativeGenerationStorage`/`FileSystemDerivativeContentStorage`
   already apply (fresh-confirmed, §2 above): a safe-character regex
   (`^[a-z0-9_-]+$`), a fixed reserved-name rejection list (`con`, `prn`,
   `aux`, `nul`, and the numbered `com`/`lpt` forms), evaluated before
   any path is constructed;
5. fail closed on a malformed or unsafe identifier — an honest, distinct
   rejection, never a best-effort sanitisation or silent substitution;
6. reject filesystem-reserved names explicitly, wherever the underlying
   storage representation is filesystem-backed;
7. guarantee path containment — every resolved storage location is
   constructed by concatenating a validated identifier with the fixed,
   configured storage root; no identifier, however malformed, may cause
   resolution outside that root;
8. derive every storage or retrieval filename **only** from validated
   Parker IDs — never from a caller-supplied filename, media type, or
   any other untrusted string;
9. at the HTTP layer specifically, never let a raw URL path segment
   become a filesystem path unchecked — reusing the real,
   already-implemented precedent (§2-§3, above): `OwnerEvidenceHttpServer.kt`'s
   own `handleRetrieveContent` path catches
   `DerivativeGenerationStorageException.UnsafeIdentifier`/`DerivativeContentStorageException.UnsafeIdentifier`
   specifically and returns `400` before any unsafe identifier can
   reach a filesystem operation; a future Tier B HTTP surface must apply
   the identical discipline.

This section is binding on any future implementation relying on this
document — it is not a description of what Tier A's implementation
happens to already do, restated for Tier B by analogy alone; it is a
requirement this document imposes directly.

## 19. Write / publication ordering

The ordering, corrected this pass only to insert the new mandatory-provenance
gate (§11, above) as an explicit precondition:

1. The structurally owner-only, Permission-Engine-authorised operation
   (§9, above) resolves owner identity, evaluates Permission Engine
   authorisation, and — only once approved — invokes OCR execution.
   Only `Recognised`/`PartialOrDegradedOutput` proceed past this step
   (§13, above).
2. **Mandatory-provenance check (§11, above).** If a Record-mandatory
   field cannot be truthfully populated for this execution, the
   operation returns `MandatoryProvenanceUnavailable` here and mints
   nothing. Only once this check passes does a `DerivativeGenerationId`
   get minted.
3. A new `DerivativeGenerationId` is minted (§8, above).
4. Derivative Content is **prepared** (`.tmp` discipline, atomic rename
   into `.prepared`).
5. Derivative Content is **published** (atomic rename from `.prepared`
   into its final location).
6. The `DerivativeGenerationRecord` is **prepared** (the identical
   `.tmp`/`.prepared` discipline, a second, independent staged object).
7. `DocumentIngestionAuditStage.ADMISSION_AUTHORISED` is recorded.
8. The `DerivativeGenerationRecord` is **published**.
9. `DocumentIngestionAuditStage.ADMITTED` is recorded.
10. Only once step 9 succeeds is the generation ever reported to the
    owner as durably retrievable.

This is the same content-before-record ordering the Tier A Content Scope
Lock already froze, reused unchanged.

**Named failure combinations, resolved honestly (fresh-reconciled
against the real `PdfDerivativeGenerationCoordinationOutcome` taxonomy,
§2-§3 above):**

1. **Permission Engine rejects (step 1).** No OCR execution occurs; no
   durable side effect of any kind; a truthful `NotAuthorised`-shaped
   outcome, mirroring `analyseEvidence`'s own existing handling.
2. **Mandatory provenance unavailable (step 2).** A truthful
   `MandatoryProvenanceUnavailable` outcome; OCR execution already
   completed (its own transient result is simply not admitted), but no
   `DerivativeGenerationId` is minted and no durable state of any kind
   is created.
3. **OCR succeeds, content prepare (step 4) fails.** No
   `DerivativeGenerationId` is ever disclosed as durable. The temp file
   is cleaned by the existing `.tmp`-discipline `finally` block; no
   `.prepared` artifact exists. Mirrors `PreparationFailed`.
4. **Content prepare succeeds, generation record prepare (step 6)
   fails.** Content is durably published (steps 4-5 completed); the
   record is not. A genuinely reachable orphan-content state, requiring
   reconciliation (§22, below); content remains invisible to retrieval
   (§24, below resolves the record first) but is not deleted.
5. **Content publish succeeds, record publish (step 8) fails.** Mirrors
   `PublicationFailed`: `ADMISSION_AUTHORISED` (step 7) recorded,
   `ADMITTED` (step 9) not. Retrieval fails closed as `UnknownGeneration`,
   even though content already durably exists. Requires reconciliation.
6. **Record publish succeeds, content publish fails.** **Not reachable**,
   by construction — content publish (steps 4-5) is a hard precondition
   completed before record preparation (step 6) is ever attempted.
7. **Audit publication fails.** `ADMISSION_AUTHORISED` write failure
   (step 7) mirrors `AuthorisationAuditFailed` — record remains staged,
   never published. `ADMITTED` write failure (step 9) mirrors
   `AdmittedAuditFailed` — the record **is** genuinely published and
   durably admitted, but its audit trail's own final entry is missing;
   the real Tier A precedent (`ReconciliationRequired`, fresh-confirmed
   §2-§3 above) is reused directly: the owner-facing outcome truthfully
   discloses the record and content as genuinely admitted, flagged for
   reconciliation, never silently hidden or presented as unqualified
   success.
8. **Crash between any publication steps.** Each individual object's own
   prepare/publish step retains the OS-level atomic-rename guarantee — a
   restart cannot observe a half-renamed file for any single object,
   only, at most, the exact partial multi-object states named above.
9. **Restart with `.prepared` state.** An orphaned `.prepared`-staged
   record or content entry remains on disk after restart, never treated
   as valid or retrievable. No automatic sweep exists in the current
   codebase (§20, below, corrects the earlier overclaim on this point).
10. **Record present, content missing.** Only reachable if content is
    independently deleted or corrupted after a legitimate admission.
    Retrieval reports a distinct, honest `ContentMissing` outcome.
11. **Content present, record missing.** The same state as failure
    combination 4, above.

No filesystem multi-object transaction is claimed anywhere above.

## 20. `.prepared` cleanup — corrected factual claim

Fresh inspection of `FileSystemDerivativeGenerationStorage.kt` and
`FileSystemDerivativeContentStorage.kt`, this pass, confirms: `.tmp`
temporary files are deleted in a `finally` block on every `prepare()`
attempt, success or failure. **No automatic cleanup of a successfully
written but never-promoted `.prepared` artifact exists anywhere in the
current codebase.** A future implementation relying on this document
must define its own cleanup/recovery mechanism for orphaned `.prepared`
state (§22, below) — this document does not claim one already exists,
and does not itself invent one.

## 21. Audit semantics

A durable Tier B generation reuses the same `DocumentIngestionAudit`
port and the same two-stage (`ADMISSION_AUTHORISED`/`ADMITTED`)
discipline Tier A already uses (§19, above). A Tier B generation must
never become owner-visible as a clean, unqualified durable success while
its required `ADMITTED` audit entry is absent or failed to write — the
`AdmittedAuditFailed`/`ReconciliationRequired` precedent governs that
case truthfully instead.

## 22. Reconciliation

Because the unified, two-object model (§19, above) governs, Tier A's own
already-accepted reconciliation reality applies identically to Tier B:
content published with no corresponding record, and record published
with an audit gap, are both genuinely reachable partial-durable states.

**Reconciliation is bounded operational state management, never a second
authority:** it never grants a second source of truth independent of the
`DerivativeGenerationRecord`/Content pair themselves; it never becomes
hidden permanent success; it never automatically, silently repairs or
rewrites historical content; it never becomes an excuse to overwrite a
generation. This document does not itself specify the reconciliation
mechanism's own implementation.

## 23. Immutability

Content and the generation record for a given `DerivativeGenerationId`
are each write-once. Once successfully published, neither is ever
silently replaced, appended to, or mutated in place. A repeated,
explicit OCR execution against the same source always mints a new
`DerivativeGenerationId` and therefore an entirely new, independent
record/content pair.

## 24. Retrieval model

**known `EvidenceArtifactId` + known `DerivativeGenerationId`** →

1. resolve the `DerivativeGenerationRecord` — absence yields
   `UnknownGeneration`;
2. verify the record's own source relationship — mismatch yields
   `SourceMismatch`;
3. verify the record identifies a Tier B OCR generation (§28, below)
   before attempting to decode content as the Tier B representation
   shape — otherwise yields `WrongDerivativeKind`;
4. retrieve the subordinate Derivative Content entry — absence yields
   `ContentMissing`;
5. verify the storage-integrity digest (§16, above) before trusting any
   decoded field — failure yields `ContentCorrupt`;
6. verify `representationVersion` is supported (§15, above) — failure
   yields `UnsupportedRepresentationVersion`;
7. return the safe, owner-facing result.

**Prohibited, unchanged:** enumeration, browsing, search, or general
evidence discovery of any kind; arbitrary generation-only lookup without
the paired source-identity check; any client-supplied filesystem path
(§18, above); retrieval ever triggering a fresh OCR execution.

## 25. Owner authority for retrieval

Retrieval is gated by the same structural discipline §9, above,
establishes: no caller-supplied principal parameter of any kind on the
retrieval operation itself. This document does not correct the
disclosed, pre-existing `analyseEvidence`-itself limitation — that
belongs to the transient analysis path, unreopened. No anonymous
retrieval is authorised under any circumstance.

## 26. Explicit OCR invocation preservation

**Nothing in this document changes when or how OCR is invoked upstream.**
Unit 12's own frozen invocation-authority decisions are restated, not
reopened: OCR remains explicit, owner-triggered only.
`RequiresTierB`/`RequiresOcr` remain classification results, never
execution commands. **Not authorised by this document:** Tier A
automatically invoking Tier B; upload automatically invoking OCR;
retrieval automatically invoking OCR; restart automatically invoking
OCR; missing durable content automatically invoking OCR. The new
structurally owner-only, Permission-Engine-authorised durable-generation
operation (§9, above) is itself an explicit, owner-triggered action,
never automatic, never a side effect of anything else.

## 27. Browser semantics — normative requirements

A future owner-facing presentation of durable Tier B content **must**:
insert and render OCR-controlled text as inert text only (a DOM
`textContent`-equivalent assignment, never `innerHTML`); never treat any
portion of the recognised text, warnings, or degradation reason as
executable markup; never allow OCR-controlled text to execute script
under any circumstance; serve the HTTP retrieval response with a safe,
non-executable content type (`application/json`); never interpolate
OCR-controlled text into page markup as a template string, attribute
value, or inline script context.

**Minimum future behavioural flow, not a UI redesign:** Upload → Process
→ `REQUIRES_OCR` (unchanged) → owner explicitly triggers the new,
structurally owner-only durable-generation operation (§9, above) → on
success, the response carries the new `DerivativeGenerationId` → "View
Extracted Content" becomes available, gated on that identity → it
fetches from the durable retrieval endpoint (§24, above), never from a
transient in-memory response → after a restart, it still retrieves
durably; no OCR rerun is ever triggered by viewing.

## 28. Tier A / Tier B kind discrimination

With the unified `DerivativeGenerationId` identity space, retrieval
(§24, above, step 3) **must** verify the resolved
`DerivativeGenerationRecord`'s own kind discriminator
(`derivativeKind`/`transformationHistory` containing `OCR`) before
attempting to decode the subordinate content as either representation
shape. A Tier A generation retrieved through a Tier B-specific retrieval
path yields a distinct, honest `WrongDerivativeKind` outcome, never a
mis-decoded result. A Tier B generation is never returned, or claimed
valid, through any Tier A-only semantic path — the existing
`TierAContentRetrievalCoordinator` is not modified or repurposed.

## 29. Source-deletion correction

Three explicit, distinguished cases: (1) unknown or never-valid source
relationship — corruption/tampering, handled by §16/§24's own failure
outcomes; (2) source relationship that fails unexpectedly while the
source should exist — an operational fault, disclosed honestly, never
reclassified as derivative corruption; (3) source evidence that existed
validly and was later lawfully deleted under CDR-006 authority — neither
corruption nor tampering; this document does not invent new
deletion-cascade authority, and does not require or forbid cascading
deletion. The generation may retain historical lineage/tombstone status,
naming a source truthfully disclosed as "no longer retrievable —
lawfully deleted," never misreported as corrupted.

## 30. Retention / deletion — minimum safe semantics

No historical-generation retention/purge policy is resolved here. No
permanent orphan state is tolerated as a designed outcome — §19/§22,
above, name the genuinely reachable partial-durable states and require
bounded reconciliation. Source deletion does not, by this document,
cascade to durable Tier B content. Tombstone capability preserved, not
decided. Interrupted-deletion recovery not resolved here, because no
deletion mechanism is authorised here.

## 31. Memory / Knowledge — explicit non-effects

Reusing the `DerivativeGenerationRecord` type for Tier B does **not**
make a Tier B generation eligible for the Derivative-to-Memory-Core
Registration path, which remains explicitly Tier A `Admitted`-only. This
document does not, and no future implementation it authorises may,
without its own separate governance: register or copy durable Tier B
content into Memory Core; promote durable Tier B content to Knowledge;
treat OCR completion, or the existence of a durable Tier B generation,
as itself triggering `CandidateArtifactProduced`/`CandidateRecordProduced`/`CandidateKnowledgeProduced`
construction; join the durable Tier B generation to the
Derivative-to-Memory-Core Registration path.

## 32. QMD / RKS — explicit non-effects

QMD/RKS receive no canonical or persistence authority from a durable
Tier B generation. No automatic indexing of any kind is authorised.
CDR-008's own boundary is untouched.

## 33. Reasoning providers — explicit non-effect

No external reasoning-provider submission is authorised by this
document. Durable Tier B retrieval is a prerequisite viewing capability,
not authorisation to send content elsewhere.

## 34. Sensitive-content controls

Filesystem access to the reused generation/content stores restricted
exactly as narrowly as they already are. Full recognised text is never
logged, at any log level, under any circumstance. No secret, token, or
credential value ever appears in a generation record, content entry, or
its surrounding metadata. Exception/error messages arising from durable
Tier B storage, admission, or retrieval never include the content
itself, a server filesystem path, or a stack trace to any owner-facing
surface. Reads and writes are bounded (§17, above). Corruption is
detected via the mandatory storage-integrity digest (§16, above);
digest mismatch fails closed. Unsupported `representationVersion` fails
closed (§15, above). `confidence` is never persisted at all (§10,
above). No claim of encryption-at-rest is made.

## 35. Restart acceptance requirement — mandatory future proof

A future implementation unit relying on this document must demonstrate:
import a deterministic scanned fixture; Tier A returns `REQUIRES_OCR`;
invoke the durable-generation operation (§9, above) exactly once,
confirming both the Permission Engine evaluation and the
mandatory-provenance check (§11, above) are genuinely exercised, not
bypassed; receive the new `DerivativeGenerationId`; confirm the record
and content entry both exist durably, including audit state; retrieve
durable content pre-restart and record deterministic values (including
outcome kind, degradation reason if any, and completeness state, not
merely `recognisedText`); destroy/restart the runtime; do **not** rerun
OCR or invoke the durable-generation operation again; retrieve the same
identity again; require exact governed-field equality with the
pre-restart values.

**Structural, not merely behavioural, non-regeneration guarantee
required.** The retrieval coordinator/path exercised must hold no
`OcrMechanism`, `OcrExecutionSequencer`, `OcrProviderAdapter`, or
`EvidenceIntelligence` dependency of any kind.

## 36. Reprocessing acceptance requirement — mandatory future proof

OCR run A via the durable-generation operation → `DerivativeGenerationId`
A → record A → content A. OCR run B, same source, same operation →
`DerivativeGenerationId` B → record B → content B. A ≠ B. Both
retrievable before and after restart, independently intact, neither
overwrites the other, each retains its own independent provenance,
outcome kind, and completeness state.

## 37. Adversarial review — all 30 vectors, re-run this pass

| # | Attack | Classification | Where addressed |
| --- | --- | --- | --- |
| 1 | OCR content elevated to source evidence | REJECTED | §4, §6 |
| 2 | OCR text represented as human-verified truth | REJECTED | §6 |
| 3 | Source digest confused with derivative digest | REJECTED | §16 |
| 4 | Duplicate generation overwrite | REJECTED | §8, §23 |
| 5 | Permanent orphan OCR payload | CONTROLLED — genuinely reachable, never permanently tolerated, bounded reconciliation required | §19, §22 |
| 6 | Partial record/content publication | CONTROLLED — named failure combinations resolved, honest reconciliation required | §19, §22 |
| 7 | Corrupt persisted bytes | REJECTED | §16, §24 |
| 8 | Unsupported representation version | REJECTED | §15, §24 |
| 9 | Evidence/generation mismatch | REJECTED | §24 |
| 10 | Path traversal | REJECTED | §18, §24 |
| 11 | Reserved filesystem identifiers | REJECTED, normative — a real, dedicated section, correctly cross-referenced | §18 |
| 12 | Oversized OCR result | REJECTED — outer ceilings reused, and further limits mandatorily frozen before implementation, not merely encouraged | §17 |
| 13 | OCR text leakage to logs | REJECTED | §34 |
| 14 | Filesystem/temp-path leakage | REJECTED | §34 |
| 15 | Unauthenticated retrieval | REJECTED | §25 |
| 16 | Retrieval triggering OCR | REJECTED | §24, §26, §35 |
| 17 | Restart triggering OCR | REJECTED | §26 |
| 18 | Memory write | REJECTED | §31 |
| 19 | Knowledge promotion | REJECTED | §31 |
| 20 | QMD/RKS authority expansion | REJECTED | §32 |
| 21 | Reasoning-provider submission | REJECTED | §33 |
| 22 | Source deletion leaving unexplained derivative state | CONTROLLED — three-case classification; full policy HONESTLY DEFERRED | §29, §30 |
| 23 | OCR text becoming executable HTML/script | REJECTED, normative | §27 |
| 24 | OCR engine/model provenance loss | CONTROLLED — honestly classified as a real, currently-unfilled requirement, with a required fail-closed consequence, never fabricated | §10, §11 |
| 25 | Repeat OCR overwriting historical content | REJECTED | §8, §23, §36 |
| 26 | Tier A/Tier B kind confusion | REJECTED | §28 |
| 27 | Tier B retrieved under Tier A-only semantics | REJECTED | §28 |
| 28 | Silent representation-version migration | REJECTED | §15 |
| 29 | Audit failure falsely reported as success | REJECTED | §19, §21 |
| 30 | Hidden permanent reconciliation authority | REJECTED | §22 |
| — | **(This pass's own blocker) Durable OCR operation bypassing Permission Engine authorisation** | REJECTED — evaluation of the existing, applicable `EvidenceIntelligenceInvocationGate` is now mandatory, first, before any OCR work | §9, §19 |

No item above is classified DEFECT or AMBIGUOUS. Items 5, 6, 22, and 24
are CONTROLLED or HONESTLY DEFERRED, each with a stated, bounded reason
that does not block implementation authority for the rest of this
document; none is left open-ended or unaddressed.

## 38. Constitutional self-certification

| Authority | Check | Result |
| --- | --- | --- |
| CDR-006 | Evidence Custodian sole source authority | Unreopened; §4, §6, §29, §30 restate its scope |
| CDR-007 | Evidence Intelligence owns analytical judgement; not a truth authority | Untouched; §6, §31 restate |
| CDR-008 | Memory Core boundary | Untouched; §32 restates |
| Unit 12 §13 | Present-tense: Unit 12 itself creates no Tier B generation record/store; explicitly reserves the question | Honoured in full; this document is the reserved future unit |
| **Evidence Intelligence Scope Lock §6 step 0** | Any composing caller of Evidence-Intelligence-adjacent machinery evaluates the Permission Engine gate first | **Corrected this pass: the new durable-generation operation now genuinely does this** (§9), reusing the existing, already-registered convention, no new vocabulary |
| Unit 12 §7 (invocation semantics) | Explicit, owner-triggered only | Restated, unreopened; §9, §26 |
| Unit 12 §12/§17 (OCR output semantics, downstream effects) | No automatic Memory/Knowledge join | Restated, unreopened; §6, §31 |
| Unit 12 §14 (resource/security bounds) | 20 MiB output-text ceiling | Reused directly; §17 |
| Unit 12 §16 (reprocessing) | Two independent executions, each independently append-only | Extended to durability; §8, §36 |
| Record Scope Lock §5/§6/§9-§15/§18-§19 | Identity, immutability, digest, producer identity (conditionally mandatory model identity/version for Tier B), transformation vocabulary, time, completeness, confidence, authorization, failure atomicity | Reused as the literal type; **model-identity/version requirement now honestly resolved with a fail-closed rule, never fabricated** (§10, §11) |
| Provenance Model I-7 | OCR-origin text cannot be represented as native source text | §6 restates |
| Tier A Content Scope Lock | Storage-authority pattern, digest semantics, retrieval scoping, owner authority, sensitive-content controls | Reused in full; §5, §15, §16, §24, §34 |
| **This document's own identifier-safety claims** | Are the requirements this document imposes normative, and correctly cross-referenced? | **Corrected this pass: §18 is now a real, dedicated, normative section; every adversarial and self-certification reference to it now points to §18 correctly, not to the prior pass's misnumbered §25** |
| Programme Implementation/Governance Closure | Future Tier B `DerivativeGenerationRecord` mechanics named as deferred | This document is the separate, future unit that deferral itself anticipated, and now resolves it |

## Final Recommendation

**READY FOR FINAL CODEX REVIEW.** The blocker (Permission Engine
authorisation bypass) is corrected: the new durable-generation operation
now evaluates the existing, already-registered
`EvidenceIntelligenceInvocationGate` convention, using the
structurally-resolved owner principal, before any OCR work, content
preparation, generation-record preparation, audit, or publication, and
fails closed with zero durable side effects on rejection — no Permission
Engine redesign, no new permission vocabulary. All five major/minor
findings are corrected: provenance is stated as a truth table with an
honest, disclosed, non-fabricated gap (model version) and a required
fail-closed consequence, never an inferred "mechanism is the model"
equivalence and never the placeholder `"unknown"`; completeness is
conditional on genuine coverage evidence, never unconditional on a clean
recognition, and request-supplied page count is explicitly barred from
serving as coverage proof; confidence exclusion is stated as this
document's own narrow election, not a claimed general precedence;
representation-size sub-limits must be frozen before implementation is
authorised, not merely encouraged; a real, dedicated, normative
identifier-safety section now exists, correctly cross-referenced
everywhere it is cited. The unified `DerivativeGenerationId`/`DerivativeGenerationRecord`
architecture, subordinate content storage, clean-vs-degraded
distinction, mandatory digest, audit, reconciliation, source-deletion
distinction, safe rendering, kind discrimination, restart proof, A/B
reprocessing proof, and all Memory/Knowledge/QMD/RKS/reasoning-provider
non-effects are all confirmed intact, none regressed. All 30 adversarial
vectors plus this pass's own added authorisation vector resolve to
REJECTED or CONTROLLED/HONESTLY DEFERRED with a stated, bounded reason;
none remain DEFECT or AMBIGUOUS. No code implementation is authorised
until this document is itself adopted.
