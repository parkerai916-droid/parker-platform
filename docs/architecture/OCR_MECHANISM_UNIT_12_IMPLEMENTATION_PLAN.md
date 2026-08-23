# OCR Mechanism — Unit 12 Implementation Plan

## Status

**Draft for owner review. Governance/implementation-planning only — no
Kotlin is implemented, proposed as a diff, or changed by this document.
No dependency is added. No `ParkerRuntime` wiring, `Resource`
registration, or `ActionVocabulary` registration is performed. Neither
`src/` nor `tests/` is touched. No fixture is added or modified.
Nothing is staged, committed, or pushed.** Document Ingestion's own
Tier B implementation is not begun by this document.

**Repository baseline confirmed fresh before drafting:** `main` at
`0be393ca6aaf222250ee2bb5476f758b2ddbdf44`, working tree clean.

This document implements exactly, and only, what
`docs/architecture/OCR_MECHANISM_UNIT_12_RUNTIME_INVOCATION_SCOPE_LOCK.md`
("the Unit 12 Scope Lock") authorised: the composition-level wiring
that makes Evidence Intelligence's own, already-accepted `analyse`
operation *capable* of invoking the OCR mechanism, using the existing
`(EXECUTE, DOCUMENT)` permission surface and the existing
`ParkerRuntime.analyseEvidence` entry point, within the resource/
security bounds that document froze. It does not reopen, redesign, or
reinterpret the Unit 12 Scope Lock, the OCR Mechanism Contract Design,
the OCR Mechanism Scope Lock, or any Evidence Intelligence governance —
each is treated as controlling and unmodified throughout.

**Narrow accuracy correction (applied during owner-acceptance review of
`docs/architecture/OCR_MECHANISM_DOCLING_PROVIDER_AUTHORIZATION_SCOPE_LOCK.md`
("the Docling Authorization"), adopted commit `c5b7aad`).** At the time
this plan was originally drafted, no concrete OCR provider was
authorized by any adopted governance, and this Status section said so.
That is no longer accurate as a blanket statement, and this correction
narrows it to what remains true. Six distinct items, not to be
conflated with one another anywhere in this document:

1. **Docling provider selection/demonstration** — already adopted,
   before this plan was ever drafted (`SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`
   §5; `DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md` §2, both
   adopted at `84cc061`).
2. **Docling provider authorization** — now adopted (the Docling
   Authorization, `c5b7aad`), closing the exact gap §4, below, this
   plan's own original drafting correctly identified as unresolved.
3. **Concrete `DoclingOcrProviderAdapter` implementation** — still does
   not exist anywhere in this repository (fresh-confirmed, §2/§4,
   below).
4. **Docling runtime/model/cache provisioning** — still separately
   unauthorized and undesigned (Docling Authorization §6/§9 item B).
5. **This plan's own composition wiring** — still unimplemented; this
   remains a governance/planning document only.
6. **Document Ingestion Tier B routing/invocation** — still a separate,
   unaddressed, later implementation unit.

This document still does not implement, install, provision, or add any
dependency for anything above. It still does not select, evaluate, or
newly authorize any OCR provider — that reopening is explicitly not
performed by this correction (§4, below).

---

## 1. Purpose

Produce the smallest implementation design that satisfies the Unit 12
Scope Lock's own authorisation, so that:

1. `DefaultEvidenceIntelligence`'s own composition becomes capable of
   invoking the OCR mechanism, closing the gap that Scope Lock's own
   §3/§11 disclosed (no `OcrMechanism` dependency exists today).
2. A future Document Ingestion Tier B implementation, and a future
   concrete `DoclingOcrProviderAdapter` implementation (now that
   Docling's own provider authorization is separately adopted — Status,
   above), each have a fixed, already-reviewed composition boundary to
   build against.

This document does not itself authorise either of those two future
efforts to begin (Unit 12 Scope Lock, Final Recommendation, restated
in this document's own Final Recommendation, below).

---

## 2. Authorities inspected fresh (this document)

**Newly adopted authority (full, fresh-read):**
`docs/architecture/OCR_MECHANISM_UNIT_12_RUNTIME_INVOCATION_SCOPE_LOCK.md`
("the Unit 12 Scope Lock") — confirmed unchanged since its own adoption
commit `0be393c` (`git log`/`md5sum` verified before drafting) — all 23
sections.

**Adopted after this plan's own original drafting, added by this
narrow correction:** `docs/architecture/OCR_MECHANISM_DOCLING_PROVIDER_AUTHORIZATION_SCOPE_LOCK.md`
("the Docling Authorization"), adopted commit `c5b7aad` — confirms
Docling as the already-adopted, demonstrated OCR mechanism (Provenance
Model §5; Routing Policy §2, both `84cc061`) and closes the exact "OCR
Mechanism governance... never extended to cover Docling" gap this
plan's own §4, below, originally, correctly identified — while leaving
the concrete `DoclingOcrProviderAdapter` implementation, Docling's own
runtime/model/cache provisioning, this plan's own composition wiring,
and Document Ingestion's own Tier B routing each separately
unauthorized/unimplemented, exactly as the Docling Authorization's own
§9 (its "Distinguishing A-E" table) fixes.

**OCR Mechanism governance (already inspected in full for the Unit 12
Scope Lock; re-confirmed unchanged here):** `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md`,
`docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md`,
`docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md` (the
existing, accepted plan for Units 1-11, whose own Unit 12 entry and
§16 blocked-work list this document now partially unblocks — not
modified, not reopened; extended by a new, separate document),
`docs/reviews/OCR_MECHANISM_PROGRAMME_COMPLETION_REVIEW.md`.

**Document Ingestion governance (targeted re-confirmation):**
`docs/architecture/DOCUMENT_INGESTION_PROGRAMME_GOVERNANCE_CLOSURE.md`
(Unit 12 row, unchanged), `docs/architecture/DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md`
§2/§8 Owner Decision 6, `docs/architecture/DOCUMENT_INGESTION_CDR007_OCR_EVIDENCE_INTELLIGENCE_CROSS_REFERENCE_SCOPE_LOCK.md`.

**CDR-007, CDR-008, `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`:**
re-confirmed unchanged; no new reading required beyond what the Unit
12 Scope Lock already cites, since this document adds no new
constitutional claim of its own.

**Current production implementation, freshly re-read in full for this
document (not assumed from any prior report):**

- `src/interfaces/OcrMechanism.kt` (full, 533 lines) — `OcrMechanism`
  interface (one method: `suspend fun recognise(request:
  OcrRecognitionRequest): OcrRecognitionOutcome`); `OcrRecognitionRequest`
  (`sourceEvidenceId: EvidenceArtifactId`, `content: ByteArray`,
  `mediaType: String`, `pageCount: Int? = null`); `TranscriptionFidelity`
  (`VERBATIM`/`NORMALISED`/`INFERRED_RECONSTRUCTION`);
  `OcrRecognitionIdentity` (`mechanismIdentity`, `configurationProfile`,
  `mechanismVersion: String? = null`); `OcrRecognitionSegment`;
  `OcrRecognitionResult` (`recognisedText`, `fidelity`, `identity`,
  `confidence: Double? = null`, `recognisedAt: Instant`, `warnings`,
  `segments`); `OcrRecognitionOutcome` sealed class, nine variants:
  `Recognised`, `Failed` (legacy compatibility wrapper),
  `NotAuthorised` (never constructed by Units 1-7 — orchestration-only),
  `UnsupportedOrInaccessibleInput`, `NoRecognisableContent`,
  `PartialOrDegradedOutput` (carries the actual partial result, never
  discarded), `ValidationRejection` (never constructed — Parker-owned
  policy not yet built), `ProcessingOrDependencyFailure`,
  `GenuineImplementationFault`.
- `src/interfaces/OcrProviderAdapter.kt` (full, 86 lines) — one
  interface, identical shape to `OcrMechanism` (`suspend fun
  recognise(request: OcrRecognitionRequest): OcrRecognitionOutcome`),
  reusing the same request/outcome types, zero new types. **Confirmed:
  zero concrete implementations exist anywhere in `src/`** (fresh
  repository-wide search).
- `src/runtime/OcrExecutionSequencer.kt` (full, 87 lines) — the sole
  production `OcrMechanism` implementation:
  `class OcrExecutionSequencer(private val adapter: OcrProviderAdapter)
  : OcrMechanism { override suspend fun recognise(request) =
  adapter.recognise(request) }`. **Confirmed by its own KDoc: no retry,
  no timeout wrapping, no scheduling of any kind is authorised at this
  tier** ("No `try`/`catch` of any kind wraps that call... no retry, no
  timeout wrapping, no scheduling, no background or parallel
  execution") — directly load-bearing for §7.1, below.
- `src/composition/ParkerRuntime.kt` — confirmed, at lines ~1212-1226,
  the exact composition site:
  ```
  val evidenceIntelligenceInputResolver = EvidenceIntelligenceInputResolver(defaultEvidenceCustodian, evidenceIntelligenceMemoryRetrieval)
  val evidenceIntelligenceReasoningCoordinator = EvidenceIntelligenceReasoningCoordinator(reasoningProvider)
  evidenceIntelligence = DefaultEvidenceIntelligence(evidenceIntelligenceInputResolver, evidenceIntelligenceReasoningCoordinator)
  ```
  and the already-registered `EvidenceIntelligenceInvocationGate`/
  `EvidenceIntelligenceAcceptanceCoordinator` resource/verb registrations
  (lines ~1239, ~1244, ~1268-1269, ~1274), unchanged since the Unit 12
  Scope Lock's own review.
- `src/runtime/DefaultEvidenceIntelligence.kt` (`analyse`, in full,
  re-read fresh) — confirmed exactly two constructor dependencies
  (`EvidenceIntelligenceInputResolver`,
  `EvidenceIntelligenceReasoningCoordinator?`), **no `OcrMechanism`
  reference anywhere**. Confirmed `analyse`'s own current body: resolves
  `request` via `inputResolver.resolve(request)`; extracts only
  `evidenceArtifactId`s and `RelationshipEndpoint`s from the resolved
  results for later citation — **the raw retrieved `ByteArray` content
  is read from `EvidenceRetrievalResult.Found` only implicitly, and
  discarded before reasoning is ever invoked; `analyse` today never
  forwards retrieved bytes anywhere.** If nothing resolved, returns
  `emptyList()`; otherwise calls `reasoningCoordinator?.reason(...)`
  (or returns `emptyList()` if the coordinator is `null`) and maps the
  `ReasoningProviderResponse` to zero or more `EvidenceAnalysisResult`
  values via a private `convertReasoningResponse`, explicitly disclosed
  in its own KDoc as "ordinary implementation judgement, not contract
  logic: no governing document defines this mapping."
- `src/runtime/EvidenceIntelligenceReasoningCoordinator.kt` (full, 76
  lines) — the exact template this plan's own new component mirrors
  (§5.C, below): `internal class`, one capability dependency
  (`ReasoningProvider`), one translating method, no output shaping, no
  provider selection, "the absence of any other constructor parameter
  is itself the structural guarantee" this class reaches nothing else.
- `src/runtime/EvidenceIntelligenceInputResolver.kt` (full, re-read
  fresh) — confirmed `internal class`, two dependencies
  (`EvidenceCustodian`, `MemoryRetrieval`); `resolve()` calls
  `evidenceCustodian.retrieve(principal, id)` for every
  `evidenceArtifactIds` entry and returns the raw `List<EvidenceRetrievalResult>`
  unmodified — **no `retrieveManifest` call anywhere in this file, and
  no media-type resolution of any kind.** This is the exact, disclosed
  gap the Unit 12 Scope Lock's own §10 named, re-confirmed unchanged.
- `src/runtime/EvidenceIntelligenceAcceptanceCoordinator.kt` (`dispatch`,
  in full, re-read fresh) — confirmed the real, production mapping:
  `TransientOutput` → `NotDispatched` (never touches any subsystem);
  `CandidateArtifactProduced` → `evidenceCustodian.accept(...)`
  (self-gating); `CandidateRecordProduced` → `dispatchRecord`, gated by
  this coordinator's own `permissionEngine.evaluate(...)` against the
  already-registered `MEMORY_CORE_ACCEPTANCE_RESOURCE_ID`/
  `ACCEPT_MEMORY_CORE_CANDIDATE_ACTION_NAME` pair (confirmed registered
  in `ParkerRuntime.kt` at lines ~1244, ~1274), then
  `MemoryCore.createAssertion`/`createRelationship`;
  `CandidateKnowledgeProduced` → `dispatchKnowledge` (not touched by
  this plan).
- `src/interfaces/EvidenceIntelligence.kt` (`EvidenceAnalysisRequest`,
  `EvidenceAnalysisResult`, re-read fresh) — confirmed
  `EvidenceAnalysisRequest(analysisKind: String, requestingPrincipalId,
  evidenceArtifactIds: List<EvidenceArtifactId> = emptyList(),
  memoryCoreReferences = emptyList(), reasoningContext: ReasoningContext?
  = null)`; `analysisKind` validated only for non-blankness — an open
  `String`, no closed vocabulary anywhere. Confirmed the four-variant
  `EvidenceAnalysisResult` sealed class: `TransientOutput`,
  `CandidateArtifactProduced(candidateEvidenceArtifact:
  CandidateEvidenceArtifact)`, `CandidateRecordProduced(candidateRecord:
  CandidateMemoryCoreRecord)`, `CandidateKnowledgeProduced`.
- `src/composition/ParkerRuntime.kt` (`analyseEvidence`, in full,
  re-read fresh) — confirmed unchanged since Unit 12 Scope Lock review:
  `suspend fun analyseEvidence(requestingPrincipalId: PrincipalId,
  request: EvidenceAnalysisRequest): EvidenceIntelligenceInvocationOutcome`
  — evaluates `EvidenceIntelligenceInvocationGate.buildExecutionRequest(requestingPrincipalId)`,
  returns `NotAuthorised` on denial, otherwise calls
  `evidenceIntelligence.analyse(request)` then
  `evidenceIntelligenceAcceptanceCoordinator.dispatch(requestingPrincipalId, results)`.
  No retry, no exception handling beyond the `RUNNING` lifecycle guard.
- `src/runtime/DefaultPermissionPolicy.kt` and the `ResourceRegistry`/
  `ActionVocabulary` blocks in `ParkerRuntime.kt` — re-confirmed the
  coarse `PermissionPolicyRule(action = EXECUTE, resourceType =
  DOCUMENT, outcome = APPROVED, level = AUTOMATIC)` rule (line ~773-777)
  is the one that actually governs `EvidenceIntelligenceInvocationGate`'s
  `ANALYSE_ACTION_NAME`, and that this rule is principal-blind
  (`DefaultPermissionPolicy`'s own flat rule-matching architecture,
  already disclosed at Unit 12 Scope Lock §6).
- `src/runtime/DefaultEvidenceCustodian.kt`, `src/interfaces/EvidenceCustodian.kt`
  (`retrieve`, `retrieveManifest`, in full) — confirmed
  `EvidenceRetrievalResult` is a three-variant sealed class (`Found(evidenceArtifactId,
  content: ByteArray)`, `NotFound(evidenceArtifactId)`,
  `Rejected(evidenceArtifactId, reason)`); confirmed `Found` carries
  **no media-type field of any kind**. Confirmed
  `retrieveManifest(requestingPrincipalId, evidenceArtifactId):
  EvidenceManifestRetrievalResult`, a three-variant sealed class
  (`Found(manifest: EvidenceSourceManifest)`, `NotFound`, `Rejected`),
  already exists, unmodified, on the same `EvidenceCustodian` interface
  `EvidenceIntelligenceInputResolver` already holds a reference to
  (transitively, via `defaultEvidenceCustodian`).
- `src/interfaces/EvidenceSourceManifest.kt` (full, re-read fresh) —
  confirmed exact shape: `EvidenceSourceManifest(evidenceArtifactId,
  sha256: String, byteLength: Long, receivedMediaType: String? = null,
  originalFileName: String? = null)`. **`receivedMediaType` is
  optional** — genuinely absent for some artefacts, never fabricated.
  This is the sole source of media-type information for any evidence
  artefact `EvidenceIntelligenceInputResolver` resolves — `EvidenceRetrievalResult.Found`
  itself carries none.
- `src/interfaces/TierADocumentIngestionRouter.kt` — re-confirmed
  `TierAMediaFacts(receivedMediaType: String?, mechanicallyDetectedMediaType:
  String?, originalFileName: String?, disagreement: Boolean)`, carried
  by `TierADocumentRoutingResult.RequiresTierB(reason, mediaFacts)` —
  a richer, mechanically-detected media-type signal than the bare
  custody manifest alone supplies, available only to a caller that
  already ran Document Ingestion's own Tier A routing.
- `tests/runtime/OcrExecutionSequencerTest.kt`,
  `tests/runtime/OcrExecutionPipelineTest.kt`,
  `tests/contracts/Ocr*Test.kt` (eight files, names confirmed by fresh
  directory listing) — the established Units 1-11 test-double
  conventions this plan's own test plan (§8, below) follows: hand-written
  fakes for `OcrProviderAdapter`, no real provider anywhere.
- `build.gradle.kts` — fresh, full-file grep for
  `tika|pdf|tesseract|ocr|image`: confirmed exactly two Tika
  dependencies (`tika-core:3.3.1`, `tika-parser-pdf-module:3.3.1`),
  with an explicit, load-bearing comment: *"deliberately excluding
  tika-parser-ocr-module (never added, anywhere) — OCR is structurally
  [excluded]"*. **No OCR-capable library of any kind exists anywhere
  in the current dependency graph.**
- `tests/fixtures/document-ingestion-bakeoff/fixtures/` — fresh
  directory listing confirms `03-scanned.pdf` and `07-text-image.png`
  already exist in the immutable bake-off corpus, already proven (by
  `TierAOwnerInvocationCoordinatorTest`, this session) to produce
  `TierADocumentRoutingResult.RequiresTierB` through the real Tier A
  pipeline. No new fixture is required for this plan's own test design
  (§8, §9, below).

---

## 3. Current OCR implementation state — Units 1-11 versus the gap

**Fully implemented, accepted, unmodified (Units 1-11):** the complete,
provider-neutral `OcrMechanism`/`OcrRecognitionRequest`/`OcrRecognitionOutcome`
contract; `OcrProviderAdapter` as an empty abstraction; `OcrExecutionSequencer`
as the sole `OcrMechanism` implementation, delegating to exactly one
supplied adapter with no retry/timeout/scheduling of its own.

**Missing, confirmed by fresh inspection (this is the exact Unit 12
implementation gap, Phase 3's own ten questions answered in order):**

1. **`OcrMechanism` exists as a production interface** — yes,
   unmodified, `src/interfaces/OcrMechanism.kt`.
2. **No production implementation of `OcrProviderAdapter` exists** —
   confirmed, zero concrete classes anywhere in `src/`.
3. **`DefaultEvidenceIntelligence` has no `OcrMechanism` dependency** —
   confirmed, exactly two constructor parameters, neither is `OcrMechanism`.
4. **OCR dispatch belongs inside `DefaultEvidenceIntelligence.analyse`,
   as a new, third, optional step**, structurally parallel to the
   existing reasoning step, inserted after input resolution and before
   (or alongside) reasoning — never replacing it, and never itself
   constructing the final `EvidenceAnalysisResult` list without going
   through the same disclosure discipline `convertReasoningResponse`
   already establishes (§5.E, below).
5. **`OcrProviderAdapter` exists as an interface only** — confirmed, no
   implementation.
6. **Docling is now the adopted, authorized concrete provider
   candidate — but no concrete adapter implementing it exists yet.**
   Originally, at this plan's own first drafting, no concrete provider
   had been authorized by any adopted governance at all; that changed
   with the Docling Authorization (Status, above; §4, below). Confirmed
   independently at §4, below: provider authorization is resolved;
   implementation is not.
7. **Production composition changes required** — a new, small,
   `internal` coordinator (§6, below) wrapping `OcrMechanism`; a new,
   optional third constructor parameter on `DefaultEvidenceIntelligence`;
   one new construction line in `ParkerRuntime.kt`'s
   `buildAndRegisterRuntimeGraph`; **no new `Resource`, `ActionVocabulary`
   entry, or `PermissionPolicyRule`** (the existing `(EXECUTE, DOCUMENT)`
   gate, already registered, already governs every `analyseEvidence`
   call regardless of `analysisKind`, per Unit 12 Scope Lock §5).
8. **Permission/resource wiring already fully exists and requires no
   change** — re-confirmed at §2, above (`EvidenceIntelligenceInvocationGate`,
   `EVIDENCE_INTELLIGENCE_INVOCATION_RESOURCE_ID`, `ANALYSE_ACTION_NAME`,
   all already registered and exercised by the real `DefaultPermissionPolicy`).
9. **An already-custodied `EvidenceArtifactId` resolves to source bytes
   without weakening custody or integrity** exactly as Tier A already
   does it: `EvidenceCustodian.retrieveManifest` (authoritative SHA-256/
   byte-length) followed by `EvidenceCustodian.retrieve` (bytes),
   cross-verified — never a digest recomputed from the same retrieval
   and compared to itself (§5.G, below). Custody itself is untouched;
   this is read-only, mirroring `TierAOwnerInvocationCoordinator`'s own,
   already-accepted precedent exactly.
10. **`RequiresTierB`/`RequiresOcr` remain eligibility evidence, never
    execution triggers** — observed only as an already-in-hand fact the
    *caller* (the owner, or a future thin request-construction helper)
    may use to decide whether to invoke `analyseEvidence` with an
    OCR-eligible `analysisKind` at all; `analyse`'s own internal logic
    additionally requires the resolved evidence's own manifest-derived
    media type to be image-bearing before ever constructing an
    `OcrRecognitionRequest` (§5.J, below) — two independent conditions,
    neither of which is itself an invocation.

---

## 4. Provider / mechanism determination

**Corrected by this narrow accuracy review — provider *authorization*
is no longer open; provider *implementation* still is.** This plan's
own original drafting is preserved below, unmodified, as an accurate
historical record of what the OCR Mechanism Contract Design and Scope
Lock alone establish (neither has ever selected a provider, and
neither is amended by this correction). What has changed since this
plan's own original drafting is that a *separate*, adopted document —
the Docling Authorization — has since closed the specific gap this
plan's own original text below correctly identified. The corrected
conclusion follows the historical record.

**Provider-neutral by the Contract Design/Scope Lock's own text alone
— confirmed independently, not assumed, and still true of those two
documents themselves:**

- OCR Mechanism Contract Design, Status: *"No concrete OCR engine,
  library, or service... is chosen, named, evaluated, or implied
  anywhere in this document."* §2: *"Select or implement a concrete
  engine"* is a named non-responsibility. §11: *"Concrete engine,
  library, or service identity — no provider is selected."*
- OCR Mechanism Scope Lock §14 (Provider Neutrality): *"No concrete OCR
  provider is selected by this document, or by the Contract Design it
  implements... OCRmyPDF, Tesseract, PaddleOCR, EasyOCR, or any other
  provider is not authorised by name."* §18 item 6 names concrete
  provider identity as still-deferred future work.
- The Unit 12 Scope Lock, freshly re-read for this document, resolves
  the *invocation-authority* blocker only and explicitly does not
  decide "concrete OCR provider identity" (§20, its own Explicit
  Deferrals list) or "process/execution and deployment topology" beyond
  the numeric bounds it froze.
- `build.gradle.kts`, freshly re-inspected: zero OCR-capable
  dependencies exist; the one adjacent capability already present
  (Apache Tika) explicitly, deliberately excludes its own OCR module.
- `OcrProviderAdapter` has zero implementations anywhere in `src/`,
  confirmed by fresh repository-wide search, **still true today** —
  authorization is not implementation.

**Docling provider selection/demonstration — already adopted, before
this plan was ever drafted.** `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`
§5: *"Docling is the demonstrated OCR mechanism"* (Tesseract explicitly
excluded: *"unevaluated because it was not installed; no quality
conclusion is authorised"*). `DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md`
§2's own table: Docling, "currently demonstrated." Both adopted at
`84cc061`. This plan's own original drafting did not name this
evidence — that omission was the accuracy defect this correction fixes
(Status, above).

**Docling provider authorization — now adopted, separately, by the
Docling Authorization (`c5b7aad`).** That document closed exactly the
gap `DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md` §6 had
identified (Docling remaining "Out of Scope... for the OCR Mechanism
programme itself," requiring "a separate future extension of OCR
Mechanism governance to cover Docling at all") — confining a future
`DoclingOcrProviderAdapter` exactly as `TikaEvidenceExtractor` is
already confined, bound by the Unit 12 Scope Lock's own §14
resource/security limits.

**This document does not reopen provider selection.** Tesseract,
PaddleOCR, EasyOCR, and every other provider named in the Contract
Design's/Scope Lock's own illustrative "not authorised by name" lists
above remain exactly as unauthorized, and as unconsidered, as they were
before this correction — Docling is not one alternative among several
still open; it is the sole adopted, demonstrated, and now authorized
candidate, and this correction does not re-evaluate that.

**Corrected conclusion.** Provider *authorization* is resolved
(Docling); provider *implementation* is not. No concrete
`DoclingOcrProviderAdapter` exists (confirmed, above), and this plan
still does not design, write, or authorise the act of implementing it
— that remains genuinely separate, future implementation work,
distinct from the governance decision that now permits it to be
proposed. The Unit 12 Scope Lock's own §18 row remains accurate as
originally quoted, updated only in light of the Docling Authorization's
own resolution of the "further, separate governance decision" it
named: *"A concrete `OcrProviderAdapter` satisfying §14's
resource/security bounds — Required, future, separate implementation
unit"* — the "provider identity itself remains a further, separate
governance decision" clause of that same row is now satisfied by the
Docling Authorization specifically, for Docling specifically, and for
no other provider.

**Consequence for this plan's own scope.** This document designs, and
authorises implementation-planning discussion of, exactly the
composition wiring that makes `OcrMechanism` *reachable* from
`DefaultEvidenceIntelligence` — using a hand-written **test-only fake
adapter** for structural/behavioural verification, mirroring Units 1-11's
own established convention (`OcrExecutionSequencerTest`'s own fake
adapter; Unit 10's own "hand-written fake caller" precedent) — **never
a real, production-capable OCR engine.** Provider authorization no
longer blocks a future `DoclingOcrProviderAdapter` implementation from
being proposed; **implementation itself has not occurred**, and the
running system's own OCR capability remains structurally reachable but
functionally inert until it does: `analyseEvidence` can route an
OCR-eligible request all the way to an `OcrMechanism` call, but no
concrete adapter exists to satisfy it in production.

**The desired production properties named by this task (local
execution; no network access; deterministic/bounded invocation; scanned
PDF and image support; provenance-bearing output; failure over false
success; compatibility with the existing `EvidenceAnalysisResult`/
acceptance boundary) are each addressed as constraints a *future*
concrete adapter must satisfy** (§5.N-Q, §9, below) — this plan freezes
the constraint, not the engine that will someday meet it.

---

## 5. Proposed minimum implementation architecture

Every Kotlin name below is **illustrative, not frozen**, exactly
mirroring this repository's own "exact Kotlin names... remain a future
Implementation Plan's own responsibility" discipline (Contract Design
§11) — except where marked "already frozen," naming an existing,
already-accepted type or member this plan reuses unchanged.

### A. Concrete runtime components required

Exactly one new, small, `internal` class — illustratively
`EvidenceIntelligenceOcrCoordinator` — mirroring
`EvidenceIntelligenceReasoningCoordinator`'s own already-accepted shape
precisely (same file-level pattern, same "internal, not one of the four
public runtime types" discipline). No second new class is required for
the minimum design; a future implementer may split manifest-verification
into its own helper if genuinely warranted, but this plan does not
require it.

### B. Exact dependency direction

`ParkerRuntime` (composition root) → constructs `EvidenceIntelligenceOcrCoordinator(evidenceCustodian
= defaultEvidenceCustodian, ocrMechanism)` → constructs
`DefaultEvidenceIntelligence(inputResolver, reasoningCoordinator,
ocrCoordinator)` (third, nullable parameter). `EvidenceIntelligenceOcrCoordinator`
depends on exactly `EvidenceCustodian` (already frozen, reused, the
same instance already threaded to `EvidenceIntelligenceInputResolver`)
and `OcrMechanism` (already frozen interface) — **never** `MemoryCore`,
**never** `PermissionEngine`, **never** `EvidenceIntelligence` itself,
**never** `TierADocumentIngestionRouter`, **never** a reference back to
`ParkerRuntime`. The absence of any other constructor parameter is
itself the structural guarantee, mirroring every prior coordinator
this session has designed.

### C. `OcrMechanism` role

Unchanged from Units 1-11: a pure callee, `EvidenceIntelligenceOcrCoordinator`'s
own sole dependency, invoked at most once per eligible resolved
evidence artefact per `analyse` call. This plan adds no wrapper around
`OcrMechanism` itself — `OcrExecutionSequencer` remains its own,
unmodified, sole production implementation.

### D. `OcrProviderAdapter` role

Unchanged: the sole boundary a future concrete provider's own types may
exist behind — now, specifically, Docling's own types, per the Docling
Authorization (§4, above), the sole provider that boundary may confine.
**This plan still does not design, name, or write a concrete
implementation** (§4, above) — authorization for a future
`DoclingOcrProviderAdapter` to be proposed is not the same act as this
plan proposing or writing one, and this plan does neither. For
composition to be
verifiable at all, `ParkerRuntime`'s own test-time or a dedicated
integration test constructs `OcrExecutionSequencer` with a
**hand-written, test-only fake `OcrProviderAdapter`** — never wired
into any production build path, exactly mirroring how Unit 10 proved
Evidence-Intelligence-side contract sufficiency without any real
caller existing yet.

### E. `DefaultEvidenceIntelligence` wiring

Gains a third constructor parameter,
`ocrCoordinator: EvidenceIntelligenceOcrCoordinator? = null`
(nullable, mirroring `reasoningCoordinator`'s own already-accepted
optionality exactly — an environment with no OCR composition remains a
structurally valid `DefaultEvidenceIntelligence`, exactly as one with
no reasoning provider already is). `analyse`'s own body gains one new
step, inserted after `inputResolver.resolve(request)` and independent
of whether reasoning is also attempted: for each `EvidenceRetrievalResult.Found`
in the resolved evidence set, if `ocrCoordinator` is non-null and this
plan's own eligibility test (§5.J, below) holds, invoke
`ocrCoordinator.recognise(...)` and fold its outcome into the returned
`List<EvidenceAnalysisResult>` (§5.R, below) — using the same "ordinary
implementation judgement, not contract logic" latitude
`convertReasoningResponse` already exercises, never a new frozen
contract. Where `ocrCoordinator` is `null`, behaviour is unchanged from
today — no regression to any existing `analyse` caller.

### F. `ParkerRuntime` composition impact

One new construction line in `buildAndRegisterRuntimeGraph`, adjacent
to the existing `evidenceIntelligenceInputResolver`/
`evidenceIntelligenceReasoningCoordinator` construction (lines
~1212-1214): constructing the OCR coordinator and passing it as
`DefaultEvidenceIntelligence`'s third argument. **No new `Resource`
registration, no new `ActionVocabulary` entry, no new
`PermissionPolicyRule`** (§3 item 8, above). If no concrete
`OcrMechanism` implementation is authorised at the time this unit is
actually implemented, production composition may legitimately supply
`null` for the coordinator (or omit constructing one at all) — the
wiring becomes reachable in the codebase without becoming functionally
live in production until a concrete adapter exists.

### G. Source retrieval and manifest-integrity sequence

Performed by `EvidenceIntelligenceOcrCoordinator` itself, mirroring
`TierAOwnerInvocationCoordinator`'s own already-accepted four-step
sequence exactly, substituting the artefact's own already-resolved
`EvidenceRetrievalResult.Found` for the first retrieval step (input
resolution has already happened once in `analyse`; this coordinator
does not retrieve bytes a second time):

1. `evidenceCustodian.retrieveManifest(principal, evidenceArtifactId)`.
   `NotFound`/`Rejected` → the artefact is not OCR-eligible for this
   call; no `OcrMechanism` invocation occurs for it (§9, adversarial
   item "source-integrity bypass").
2. On `Found(manifest)`: verify `manifest.byteLength == content.size`
   (the already-resolved `Found.content` from `analyse`'s own earlier
   resolution step) — mismatch is a distinct, honest failure, no OCR
   invocation.
3. Verify `sha256(content) == manifest.sha256` — **the expected digest
   is always `manifest.sha256`, the custody-established value,
   never a digest recomputed from `content` and compared to itself**
   (Unit 12 Scope Lock §10's own "no digest tautology" requirement,
   directly enforced here).
4. Only on full verification: determine `mediaType` from
   `manifest.receivedMediaType` (§5.J, below) and construct the
   `OcrRecognitionRequest`.

### H. Permission evaluation sequence

Unchanged from today's `analyseEvidence`: exactly one evaluation, of
`EvidenceIntelligenceInvocationGate.buildExecutionRequest(requestingPrincipalId)`,
before `evidenceIntelligence.analyse(request)` is ever called. No
second, OCR-specific permission evaluation is introduced anywhere in
this design (Unit 12 Scope Lock §5's own "not `analysisKind`-level"
finding, reused unchanged). `EvidenceIntelligenceOcrCoordinator` itself
holds no `PermissionEngine` reference of any kind — the same structural
guarantee `OcrMechanism`/`OcrProviderAdapter`/`OcrExecutionSequencer`
already hold, extended one tier further.

### I. Owner-principal handling

**No new structural guarantee is introduced, and none is falsely
claimed.** `analyseEvidence` continues to accept a caller-supplied
`requestingPrincipalId`, gated by the existing, principal-blind
`(EXECUTE, DOCUMENT)` rule — exactly the disclosed limitation the Unit
12 Scope Lock's own §6 already documents. §7, below, addresses this
explicitly rather than silently building around it.

### J. `RequiresTierB`/`RequiresOcr` eligibility handling

Two independent conditions, both required, neither alone sufficient,
mirroring the Unit 12 Scope Lock's own §6/§9 framing exactly:

- **Caller-side (outside this design):** the owner (or a future,
  optional, thin request-construction helper, not designed or required
  by this plan) chooses to call `analyseEvidence` with an
  OCR-eligible `analysisKind` — illustratively, `"ocr-transcription"`,
  not frozen — informed by, but never structurally gated on, a prior
  `RequiresTierB`/`RequiresOcr` disclosure.
- **`analyse`-internal (this design):** for a given resolved evidence
  artefact, `EvidenceIntelligenceOcrCoordinator` is only invoked when
  (a) `request.analysisKind` matches the OCR-eligible convention above,
  **and** (b) the artefact's own manifest-derived `receivedMediaType`
  (§5.G step 4) is image-bearing (illustratively, `application/pdf` or
  an `image/*` prefix — not frozen). An artefact whose manifest carries
  no `receivedMediaType` at all is honestly treated as *not*
  OCR-eligible — never fabricated (Unit 12 Scope Lock §7's own
  "extractedFrom... must remain null, truthfully, never fabricated"
  discipline, applied identically here to media type).

Neither condition is itself an invocation — `RequiresTierB`/`RequiresOcr`
never appear anywhere in this design as anything other than
already-in-hand facts a human, or a future thin helper, may consult
before making the one, single, explicit `analyseEvidence` call.

### K. OCR invocation sequence

`EvidenceIntelligenceOcrCoordinator.recognise(evidenceArtifactId,
content, manifest)` (illustrative signature) → performs §5.G's own
four steps → on success, constructs `OcrRecognitionRequest(sourceEvidenceId
= evidenceArtifactId, content, mediaType = manifest.receivedMediaType!!,
pageCount = null)` (illustrative: `pageCount` left `null` for the
minimum design; a future refinement may derive it from Tier A's own
`TierAMediaFacts` where available) → calls `ocrMechanism.recognise(request)`
exactly once, no retry → returns the resulting `OcrRecognitionOutcome`
(or an internal, coordinator-owned pre-execution-failure marker for a
§5.G steps 1-3 rejection) to `DefaultEvidenceIntelligence.analyse`,
which performs the `EvidenceAnalysisResult` mapping (§5.R, below).

### L. Timeout enforcement

**Not at the sequencer tier — structurally forbidden there** (§2,
above: `OcrExecutionSequencer`'s own KDoc explicitly excludes "timeout
wrapping" from its own, already-accepted shape). **Not at the
coordinator tier either** — `EvidenceIntelligenceOcrCoordinator` calls
`ocrMechanism.recognise(...)` exactly once, synchronously, with no
wrapping of its own, mirroring every other coordinator this session has
designed ("no try/catch beyond what already exists in the dependencies
it calls"). **The 15-minute timeout the Unit 12 Scope Lock froze (§14
there) must be enforced inside whatever concrete `OcrProviderAdapter`
implementation a future, separately-governed unit writes** — for
example, a subprocess-backed adapter enforcing it via the subprocess's
own bounded wait, or an in-process adapter enforcing it via
`kotlinx.coroutines.withTimeout`. **On expiry, the adapter must produce
a truthful, disclosed `OcrRecognitionOutcome.ProcessingOrDependencyFailure`**
(the existing, already-accepted "operational concern... disclosed
rather than silently retried or masked" category — the closest
already-existing, non-collapsed variant to a timeout, per the Unit 12
Scope Lock's own §15 mapping: *"OCR mechanism unavailable / OCR
failure / timeout — the same category"*), **never** a silently
truncated `Recognised`. This plan freezes the requirement and the
mapping; it does not, and structurally cannot, implement the mechanism
itself without a concrete adapter to implement it in.

### M. Concurrency enforcement

The Unit 12 Scope Lock's own §14 bound (exactly one concurrent OCR
invocation per Parker instance) is **not enforced by `OcrExecutionSequencer`,
`EvidenceIntelligenceOcrCoordinator`, or `DefaultEvidenceIntelligence`
in this design** — none of the three introduces any concurrency
primitive, mirroring their own "no scheduling... no background or
parallel execution" discipline. A future concrete adapter, or the
composition root, must supply an actual enforcement mechanism (for
example, a single-permit `Mutex`/`Semaphore` guarding adapter
invocation) before production use — named here as required future
work (§9, below), not designed in code by this plan.

### N. Source byte/page/pixel limits

The Unit 12 Scope Lock's own §14 bounds (64 MiB source bytes, 200 PDF
pages, 10,000×10,000px/100MP images) are **pre-execution checks a
future concrete adapter must perform before invoking any actual
recognition work** — on the manifest's own `byteLength` for the byte
bound (available to `EvidenceIntelligenceOcrCoordinator` without
opening the content a second time), and on page/pixel facts the
adapter itself must derive from the content it receives for the
page/pixel bounds (this plan does not require `EvidenceIntelligenceOcrCoordinator`
itself to parse PDF/image structure — that parsing capability does not
exist anywhere in this dependency graph today, and adding it is
provider-adapter-tier work, §4 above). A breach must produce a
distinct, disclosed `OcrRecognitionOutcome.ProcessingOrDependencyFailure`
(mirroring the timeout mapping, §5.L, above), never a silent
truncation.

### O. Output-size enforcement

The 20 MiB recognised-text bound is a future concrete adapter's own
responsibility to enforce before returning `Recognised`/`PartialOrDegradedOutput`
— `OcrRecognitionResult.recognisedText` is an ordinary `String` with no
size ceiling of its own in the already-accepted Unit 1 shape, and this
plan does not propose adding one (doing so would modify already-accepted
OCR Mechanism Unit 1/6 governance, out of scope). Enforcement remains
adapter-tier, exactly as page/pixel limits are (§5.N, above).

### P. No-network enforcement

**Structural, by omission, not a runtime check.** Neither
`OcrExecutionSequencer` nor `EvidenceIntelligenceOcrCoordinator`
introduces any networking dependency of any kind — confirmed by this
plan's own design holding no such reference anywhere. The Unit 12
Scope Lock's own "no network access authorised" decision (§14 there)
binds any future concrete adapter: an adapter implementation that opens
a network socket, DNS resolution, or HTTP client of any kind would
violate already-adopted governance regardless of anything this plan
adds or omits — this plan neither authorises nor forecloses that
violation being technically possible in Kotlin; it forecloses it being
*governed*.

### Q. Model/provider identity and version provenance

`OcrRecognitionIdentity(mechanismIdentity, configurationProfile,
mechanismVersion: String? = null)` — already frozen, unmodified, Unit
1's own shape — is the sole carrier. A future concrete adapter must
populate `mechanismVersion` whenever genuinely known (Unit 12 Scope
Lock §11's own "required whenever the eventual concrete provider is
itself model-backed"); this plan adds no new field and narrows nothing.
Where the eventual `EvidenceAnalysisResult` mapping produces a
`CandidateArtifactProduced`/`CandidateRecordProduced` (§5.R, below),
whatever `CandidateProvenance`/`CandidateAssertion` construction a
future unit builds must carry this identity forward — this plan does
not itself design that construction in code, only requires it.

### R. `EvidenceAnalysisResult` mapping

Performed by `DefaultEvidenceIntelligence.analyse` itself, in a new,
small, private mapping function mirroring `convertReasoningResponse`'s
own already-accepted "ordinary implementation judgement, not contract
logic" latitude exactly:

- `OcrRecognitionOutcome.Recognised`/`PartialOrDegradedOutput` → **for
  the minimum implementation, exactly one `EvidenceAnalysisResult.TransientOutput`**,
  citing the source `EvidenceArtifactId`, carrying the recognised text
  (or partial text) as disposable prose — the single safest default,
  directly foreclosing "OCR result treated as evidential truth" (§9,
  below) by construction: nothing is durably registered merely because
  OCR succeeded. `CandidateArtifactProduced`/`CandidateRecordProduced`
  construction from OCR output remains available, correctly-typed
  machinery (§5.S, §5.T, below) a **future, separate policy decision**
  may choose to exercise from this same mapping point — this plan
  freezes the safe default, not a promotion policy, mirroring the Unit
  12 Scope Lock's own explicit deferral of "output-quality validation
  policy" (§20 there).
- `OcrRecognitionOutcome.NoRecognisableContent`, `UnsupportedOrInaccessibleInput`,
  `ProcessingOrDependencyFailure`, `GenuineImplementationFault`, `Failed`
  → **no `EvidenceAnalysisResult` is produced for this artefact** —
  mirroring exactly how an unresolved (`NotFound`/`Rejected`)
  `EvidenceRetrievalResult` is already, silently, excluded from
  citation today; the artefact simply contributes nothing to this
  `analyse` call's own result list. The coordinator-internal outcome
  (§5.K, above) remains structurally distinct and available for a
  future logging/audit addition — never collapsed at the code level,
  even though the `EvidenceAnalysisResult`-level list does not
  separately surface it (§9, adversarial item "false-success
  outcomes": this is silence, not fabricated success — no result
  claims anything succeeded).
- `OcrRecognitionOutcome.NotAuthorised`, `ValidationRejection` → never
  constructed by any code path this plan designs (unchanged from
  Units 1-11's own "for completeness of the taxonomy only" discipline).

### S. `CandidateArtifactProduced` handling

Not exercised by the minimum mapping's own default rule (§5.R, above).
Remains available, unmodified, self-gating via `EvidenceCustodian.accept`
(§2, above) — a future policy unit may route a `Recognised` OCR result
here instead of `TransientOutput` when it judges the recognised text
worth registering as its own evidence artefact. This plan's own test
plan (§8, below) exercises this mapping directly (constructing the
`EvidenceAnalysisResult.CandidateArtifactProduced` value by hand and
proving it dispatches correctly through the real, unmodified
`EvidenceIntelligenceAcceptanceCoordinator`), independent of whether
`analyse`'s own default policy ever produces one in production.

### T. `CandidateRecordProduced` handling

Symmetric to §5.S: not exercised by the minimum mapping's own default
rule; remains available, unmodified, gated by
`EvidenceIntelligenceAcceptanceCoordinator`'s own existing
`PermissionEngine` reference (§2, above); exercised directly by this
plan's own test plan the same way.

### U. `TransientOutput` handling

The minimum mapping's own default (§5.R, above) — `NotDispatched`,
never touches `EvidenceCustodian`, `MemoryCore`, or Knowledge Memory
(§2, above, re-confirmed from `EvidenceIntelligenceAcceptanceCoordinator.dispatch`'s
own real body).

### V. Failure/result taxonomy

Restated, not invented: every `OcrRecognitionOutcome` variant Units 1-7
already froze (§2, above) remains distinct and unmodified. This plan
adds exactly one new, coordinator-internal (never public, never part
of Evidence Intelligence's own frozen four-type ceiling) distinction:
a pre-execution integrity/eligibility rejection (§5.G steps 1-3, §5.J),
mirroring `TierAOwnerInvocationOutcome`'s own already-accepted shape
(`ManifestRetrievalRejected`/`ManifestNotFound`/`SourceRetrievalRejected`/
`SourceNotFound`/`ByteLengthMismatch`/`DigestMismatch`), never collapsed
into, or confused with, any `OcrRecognitionOutcome` variant (those begin
only once `OcrMechanism.recognise` is actually called).

### W. Reprocessing/history behaviour

Unchanged from the Unit 12 Scope Lock's own §16: every `analyseEvidence`
call is independent; two separate OCR-eligible calls against the same
`EvidenceArtifactId` each independently retrieve, verify, and (if
eligible) invoke OCR again — no caching, no memoisation, no
deduplication anywhere in this design (§9, adversarial item
"reprocessing overwrite/deduplication").

### X. Memory Core boundary

Unchanged: only `CandidateRecordProduced`'s own existing, gated leg
ever reaches `MemoryCore`, and only when a future policy decision
routes an OCR result there (§5.T, above) — never automatically, never
from `TransientOutput`, never from any pre-execution rejection.

### Y. Knowledge boundary

Unchanged: `CandidateKnowledgeProduced` is not touched by this plan at
all; nothing in this design constructs one from an OCR result.

### Z. Evidence Intelligence authority boundary

Unchanged and made executable, exactly as the Unit 12 Scope Lock's own
§17 already states: Evidence Intelligence gains the structural
*capability* to invoke OCR; it gains no new authority over truth,
custody, or evidential classification — every existing constraint
(Contract Design §2, §8) binds this design identically.

### AA. Document Ingestion `DerivativeGenerationRecord` exclusion

Unchanged and reconfirmed: nothing in this design constructs, reads, or
references `DerivativeGenerationRecord`, `DerivativeGenerationStorage`,
or `DocumentIngestionAudit` — `EvidenceIntelligenceOcrCoordinator` holds
no dependency capable of reaching any of the three.

### AB. QMD/RKS boundary

Unchanged: this design adds no retrieval, ranking, or indexing capability
of any kind; nothing here is reachable from, or grants authority to,
QMD/RKS.

### AC. Deletion/retention boundary

Unchanged: no new deletion, cascade, or retention mechanism is proposed
anywhere in this design; whatever rule already governs an accepted
`CandidateEvidenceArtifact` or created `Assertion` governs one
OCR produced identically.

---

## 6. Addressing the two accepted limitations explicitly

### 6.1 Timeout

**Confirmed, independently, in this document's own §2/§5.L: the
200-page bound is not, and must never be read as, evidence that
execution completes within 15 minutes.** The two bounds are independent
(Unit 12 Scope Lock §14, as corrected during its own owner-acceptance
review). This plan's own architecture makes that independence
structural, not merely documented: **no timeout enforcement exists, or
is authorised, anywhere above the concrete adapter tier** —
`OcrExecutionSequencer`'s own already-accepted governance forbids
timeout wrapping at the sequencer tier; this plan's own new coordinator
introduces none either. **The single, correct, governance-consistent
enforcement point is inside whatever concrete `OcrProviderAdapter`
implementation a future, separately-governed unit writes** — required,
not optional, and named as a completion criterion no future adapter may
satisfy Unit 12 without (§9, below). On expiry, that adapter must
produce `OcrRecognitionOutcome.ProcessingOrDependencyFailure` with an
honest, non-blank reason — never a silently truncated `Recognised`,
never a hang, never an uncaught fault masquerading as success.

### 6.2 Owner authority

**Confirmed, independently, re-reading `analyseEvidence`'s own current
body fresh (§2, above): it still accepts a caller-supplied
`requestingPrincipalId`, still gated only by the principal-blind
`(EXECUTE, DOCUMENT)` rule.** This plan does not falsely claim
otherwise anywhere in §5, above.

**Decision: the existing accepted boundary is used as-is, unchanged,
for this Unit 12 implementation — no new structural owner guarantee is
built, and none is silently implied.** Reasoning:

- Building a narrow, structurally owner-only wrapper specifically for
  OCR-eligible `analyseEvidence` calls (for example, a thin,
  illustrative `analyseEvidenceAsOwner` with no `requestingPrincipalId`
  parameter, mirroring `invokeTierAIngestionAsOwner`'s own shape) would
  be a **new structural guarantee Evidence Intelligence's own governance
  has never had, for any `analysisKind`** — not a Unit-12-specific fix,
  but a change to Evidence Intelligence's own already-accepted
  invocation model, generically. That is squarely outside this plan's
  own authority: the Unit 12 Scope Lock's own §6 already named this
  exact possibility ("a future implementation unit introducing the
  optional request-construction helper... may reasonably choose to make
  *that* helper structurally owner-only") as a **future implementation
  choice**, not a Unit 12 requirement, and this document does not
  elevate it to one now.
- **If a genuinely stronger, structural owner-only guarantee is wanted
  for OCR invocation specifically, it requires its own, separate,
  narrow governance decision** — identified here, explicitly, as such,
  rather than silently implemented: a future Scope Lock (or narrow
  amendment) authorising a dedicated, structurally owner-only entry
  point for OCR-eligible analysis, mirroring Document Ingestion's own
  three-entry-point precedent. This plan neither designs nor requires
  that entry point.
- **What this plan does require:** every test exercising owner/
  non-owner behaviour (§8, below) must test the *actual*, current
  guarantee — a caller-supplied principal, approved by the existing
  coarse rule regardless of which principal it names — never a
  fabricated stronger one. Any future implementation claiming
  "owner-only OCR invocation" without either (a) this disclosed
  limitation remaining accurate in its own documentation, or (b) the
  separate governance decision above having been obtained, would itself
  be a defect.

---

## 7. Test and acceptance plan

Every item below reuses the immutable bake-off corpus
(`tests/fixtures/document-ingestion-bakeoff/fixtures/`, `03-scanned.pdf`,
`07-text-image.png`) and the real, production `DefaultPermissionPolicy`/
`ResourceRegistry`/`ActionVocabulary` wherever the item concerns
composition or permission behaviour; every item concerning OCR
*recognition* behaviour itself uses a hand-written, configurable fake
`OcrProviderAdapter`, never a real engine (§4, above) — this split is
itself deliberate, not a shortcut: it is the same split
`TierAOwnerInvocationCoordinatorTest` already uses between real
Evidence Custodian/router composition and real bake-off bytes on one
side, and hand-written fakes for precondition-failure paths on the
other.

1. **Real production composition test** — construct a real `ParkerRuntime`
   (mirroring `ParkerRuntimeOwnerLocalFileIngressIntegrationTest`'s own
   established pattern), with the new OCR coordinator wired to a
   fake adapter, and prove `analyseEvidence` reaches it end to end.
2. **Permission allowed** — a request with an approving `PermissionEngine`
   reaches `evidenceIntelligence.analyse`.
3. **Permission denied** — `NotAuthorised` returned; `evidenceIntelligence.analyse`
   never called (structural spy proof, mirroring every prior
   `NotAuthorised` test this session has written).
4. **Wrong/non-owner principal behaviour as actually supported** — a
   test proving the *actual*, disclosed behaviour: a non-owner
   `PrincipalId`, gated only by the coarse `(EXECUTE, DOCUMENT)` rule,
   is **approved** today (documenting the limitation §6.2 names, not
   silently omitting it) — this test exists specifically so a future
   change to that behaviour is caught, not so the current gap is hidden.
5. **Valid scanned PDF** — real bytes from `03-scanned.pdf`, a fake
   adapter returning `Recognised`; prove the constructed
   `OcrRecognitionRequest` carries the real content/mediaType, and the
   resulting `EvidenceAnalysisResult` list contains the expected
   `TransientOutput`.
6. **Valid supported image** — same, using `07-text-image.png`.
7. **Corrupt PDF** — a manifest/byte-length pair engineered to fail
   verification (§5.G step 2) before any `OcrMechanism` call; fake
   adapter spy proves zero invocations.
8. **Corrupt image** — same shape, image media type.
9. **Oversized source** — a manifest whose `byteLength` exceeds 64 MiB;
   prove rejection before invocation (adapter-tier enforcement is not
   yet built, §5.N — this test proves the *composition* layer at least
   has the manifest fact available to reject on, and documents that the
   actual page/pixel/byte enforcement is adapter-tier future work if not
   yet implemented at the time this unit lands).
10. **Page-limit breach** — fake adapter returns `ProcessingOrDependencyFailure`
    with an honest "page limit exceeded" reason; prove it maps to *no*
    `EvidenceAnalysisResult`, never a fabricated partial success.
11. **Dimension-limit breach** — same shape, image dimensions.
12. **Total-pixel-limit breach** — same shape, total pixels.
13. **Output-size breach** — same shape, output size.
14. **Timeout** — fake adapter returns `ProcessingOrDependencyFailure`
    with an honest "timed out" reason (simulating what a real,
    timeout-enforcing adapter would produce, since no real adapter
    exists to genuinely time out); prove the mapping is honest, never a
    truncated `Recognised`.
15. **OCR provider failure** — fake adapter returns
    `GenuineImplementationFault`; prove no `EvidenceAnalysisResult`
    is fabricated.
16. **Malformed provider output** — fake adapter throws (an
    unexpected, uncaught fault); prove it propagates unchanged out of
    `analyseEvidence`, mirroring `OcrExecutionSequencer`'s own
    already-accepted no-catch discipline, never silently swallowed.
17. **Empty/no-text result** — fake adapter returns
    `NoRecognisableContent`; prove it maps to no `EvidenceAnalysisResult`.
18. **Source changed/integrity mismatch** — a manifest SHA-256 that
    does not match the retrieved content; prove rejection before
    invocation, and prove the expected digest used for comparison is
    the manifest's own value, never a self-recomputed one (mirroring
    `TierAOwnerInvocationCoordinatorTest`'s own "no digest tautology"
    test exactly).
19. **No-network proof** — a structural, reflection-based test proving
    `EvidenceIntelligenceOcrCoordinator` and the new
    `DefaultEvidenceIntelligence` wiring hold no networking-capable
    dependency of any kind (mirroring Unit 9's own closed reachable-type-graph
    proof, extended one tier further).
20. **Concurrency-one proof** — **not fully provable at this tier**
    (§5.M, above: no concurrency enforcement mechanism is designed by
    this plan) — the test plan for this item is limited to a structural
    proof that no concurrency-*expanding* primitive (a thread pool, a
    parallel-dispatch coroutine builder) exists anywhere in the new
    composition; genuine concurrency-limiting behaviour remains future,
    adapter-or-composition-tier work this plan names but does not
    design.
21. **Provenance identity/version proof** — a fake adapter supplying a
    populated `OcrRecognitionIdentity`; prove it is available, unaltered,
    to whatever future `CandidateProvenance` construction eventually
    reads it (mirroring Unit 8's own "hypothetical provenance
    construction" precedent, adapted).
22. **`CandidateArtifactProduced` mapping** — direct, hand-constructed
    test of this mapping path (§5.S, above), proving it dispatches
    through the real, unmodified `EvidenceIntelligenceAcceptanceCoordinator`
    to a real (or fake, for isolation) `EvidenceCustodian.accept`.
23. **`CandidateRecordProduced` mapping** — symmetric, proving dispatch
    to `MemoryCore.createAssertion`/`createRelationship`, gated by the
    coordinator's own real `PermissionEngine` evaluation.
24. **`TransientOutput` mapping** — the minimum default path (§5.R,
    above); prove `NotDispatched`, no subsystem touched.
25. **No automatic Memory registration** — a structural/behavioural
    test proving a `Recognised` OCR result, under the minimum mapping's
    own default rule, never reaches `MemoryCore` at all.
26. **No Knowledge promotion** — symmetric, proving no
    `CandidateKnowledgeProduced` is ever constructed from an OCR result
    by this design.
27. **No `DerivativeGenerationRecord` creation** — a structural test
    proving `EvidenceIntelligenceOcrCoordinator` holds no
    `DerivativeGenerationStorage`/`DocumentIngestionAudit` reference of
    any kind.
28. **Repeated processing/history preservation** — two separate
    `analyseEvidence` calls against the same `EvidenceArtifactId`, each
    independently verified and each independently dispatched; prove
    neither collapses, caches, or suppresses the other.
29. **Genuine end-to-end custodied-source → explicit `analyseEvidence`
    → OCR → `EvidenceAnalysisResult` test** — the capstone integration
    test: real `ParkerRuntime`, real `importEvidenceFileAsOwner` (or
    direct `EvidenceCustodian.accept`) admitting `03-scanned.pdf` into
    real custody, a real manifest, a fake `OcrProviderAdapter` standing
    in for the still-unauthorised concrete engine, one explicit
    `analyseEvidence` call, and assertions on the real, resulting
    `EvidenceIntelligenceInvocationOutcome` — proving the entire chain
    this plan designs is genuinely reachable, not merely unit-tested in
    isolation.

**Full-suite regression requirement:** `./gradlew test --no-daemon
--rerun-tasks` must pass in full (with only the same pre-existing,
environment-conditional skips already documented this session) before
any future implementation of this plan may be declared complete —
named here as a completion criterion for that future unit, not
performed by this document.

---

## 8. Implementation impact map

| Surface | Classification |
| --- | --- |
| New `EvidenceIntelligenceOcrCoordinator` (illustrative name), `internal`, two dependencies (`EvidenceCustodian`, `OcrMechanism`) | **REQUIRED** — future implementation |
| `DefaultEvidenceIntelligence`'s third, nullable constructor parameter and `analyse`'s new OCR-dispatch step | **REQUIRED** — future implementation |
| One new construction line in `ParkerRuntime.kt`'s `buildAndRegisterRuntimeGraph` | **REQUIRED** — future implementation |
| Manifest-verified, non-tautological integrity sequence (§5.G) | **REQUIRED** — future implementation |
| A concrete `DoclingOcrProviderAdapter` implementation | **CONDITIONALLY AUTHORIZED — future implementation.** Provider authorization is resolved (Docling Authorization, `c5b7aad`, §4 above); this plan still does not perform, design, or write that implementation itself, and it remains a separate, future implementation unit, bound by the Docling Authorization's own §5-§6 and this plan's own §5 design |
| Timeout/page/pixel/output-size enforcement inside a concrete adapter | **CONDITIONAL** — required only once, and inside, a future authorised adapter; not designed in code here |
| Concurrency-limiting mechanism | **CONDITIONAL** — required before production use; mechanism and location left to future implementation |
| New `Resource`, `ActionVocabulary` entry, or `PermissionPolicyRule` | **FORBIDDEN** — none required (§3 item 8, above) |
| New `PermissionAction`/`ResourceType` | **FORBIDDEN** — none required, confirmed by Unit 12 Scope Lock §5, unaffected by this plan |
| Structurally owner-only wrapper for OCR invocation specifically | **OPTIONAL, and only under separate future governance** (§6.2, above) — not authorised by this plan |
| Test files for every item in §7, above | **REQUIRED** — future implementation |
| New fixtures | **NOT REQUIRED** — `03-scanned.pdf`/`07-text-image.png` already exist, immutable, already proven `RequiresTierB`-producing |
| Any change to `OCR_MECHANISM_CONTRACT_DESIGN.md`, `OCR_MECHANISM_SCOPE_LOCK.md`, the Unit 12 Scope Lock, `OcrMechanism.kt`, `OcrProviderAdapter.kt`, `OcrExecutionSequencer.kt`, `EvidenceIntelligenceInputResolver.kt`, `EvidenceIntelligenceReasoningCoordinator.kt`, `EvidenceIntelligenceAcceptanceCoordinator.kt`, `EvidenceIntelligence.kt`'s own four public types, `analyseEvidence`'s own signature, `EvidenceIntelligenceInvocationGate.kt`, `TierAOwnerInvocationCoordinator`, any Document Ingestion Derivative-Generation type | **FORBIDDEN** |
| Full-suite regression run | **REQUIRED**, before future implementation may be declared complete |

**Dependency/build impact of this plan itself: none.** No third-party
dependency is proposed, evaluated, or added to implement the
composition wiring this plan designs — a hand-written fake adapter
requires no new library. **Provider selection is no longer open** —
Docling is the adopted, authorized candidate (§4, above) — but **a
future `DoclingOcrProviderAdapter` implementation will still require
its own dependency addition** (a Docling wrapper library, or a
subprocess-invocation utility, depending on how a future implementer
integrates it) — not identified, evaluated, or added by this document.
When that future implementation unit is proposed, it must independently
supply: exact purpose; license; runtime footprint; network behaviour
(must satisfy "none," per Unit 12 Scope Lock §14 and the Docling
Authorization §6); native/system dependencies; security implications;
and why Apache Tika (already present, deliberately OCR-excluded) cannot
satisfy the requirement — none of which this document supplies, because
none of it is decided here. This is a narrower, Docling-specific
version of the same open dependency question this plan's own original
drafting already deferred — not a new deferral, and not now resolved.

---

## 9. Adversarial review

| # | Attack | Result |
| --- | --- | --- |
| 1 | Hidden automatic invocation | Foreclosed — §5.J: two independent, caller-driven conditions; nothing in `analyse`'s own new step self-triggers |
| 2 | Permission bypass | Foreclosed — §5.H: the single existing gate, unchanged, remains the sole check; no second, weaker path introduced |
| 3 | Owner-authority overclaim | Foreclosed by disclosure, not by a stronger guarantee — §6.2 states plainly that no new structural guarantee exists; test item 4 (§7) proves the actual, weaker behaviour rather than hiding it |
| 4 | Source-integrity bypass | Foreclosed — §5.G's four-step sequence, non-tautological, required before any `OcrMechanism` call |
| 5 | Custody bypass | Foreclosed — `EvidenceIntelligenceOcrCoordinator` only ever reads via `EvidenceCustodian.retrieveManifest`/reuses already-resolved `retrieve` content; no write path of any kind |
| 6 | OCR network access | Foreclosed structurally, by omission (§5.P) — no networking dependency anywhere in this design; binding on any future adapter by governance, not by this plan's own code (since no code exists yet) |
| 7 | Unbounded execution | **Not fully foreclosed by this plan alone** — §5.L discloses the enforcement point is a future adapter's own responsibility; this plan freezes the requirement and forbids any false claim of present enforcement |
| 8 | Timeout failure (silent truncation) | Foreclosed by mapping rule — §5.L/§5.R: timeout must map to `ProcessingOrDependencyFailure`, never `Recognised` |
| 9 | Decompression/resource bombs | Partially foreclosed — §5.N names the bounds and the mapping; actual pre-execution enforcement is adapter-tier future work, explicitly named as required (§8) rather than silently assumed done |
| 10 | Page-count evasion | Foreclosed at the mapping level (§5.R); enforcement itself is adapter-tier (§5.N) |
| 11 | Image-dimension evasion | Same as #10 |
| 12 | Output explosion | Same reasoning, §5.O |
| 13 | Concurrency escape | **Not foreclosed by this plan** — §5.M and test item 20 (§7) explicitly disclose this as unenforced future work, not silently claimed solved |
| 14 | Malformed OCR output | Foreclosed — Unit 1's own construction-time validation (`recognisedText` non-blank, `confidence` bounded, page numbers non-decreasing) already rejects a malformed `OcrRecognitionResult` at construction; a genuinely malformed adapter response either fails that validation (constructor throws, propagates unchanged) or is honestly disclosed via one of the seven failure variants |
| 15 | False-success outcomes | Foreclosed — §5.R's own mapping table: every non-`Recognised`/`PartialOrDegradedOutput` outcome produces no result at all, never a fabricated success |
| 16 | Provenance loss | Foreclosed — §5.Q: `OcrRecognitionIdentity` is carried unmodified through to wherever a future `CandidateProvenance` reads it |
| 17 | Provider-version loss | Same, §5.Q |
| 18 | Accidental evidential-truth promotion | Foreclosed — §5.R's own minimum default (`TransientOutput` only) is specifically chosen to prevent this; `CandidateArtifactProduced`/`CandidateRecordProduced` require a future, separate policy decision to ever be produced in production |
| 19 | Accidental Memory write | Foreclosed — §5.X: only the already-gated `CandidateRecordProduced` leg reaches `MemoryCore`, never automatically |
| 20 | Accidental Knowledge promotion | Foreclosed — §5.Y |
| 21 | Accidental `DerivativeGenerationRecord` creation | Foreclosed — §5.AA: no dependency capable of it exists anywhere in this design |
| 22 | Evidence Intelligence authority widening | Foreclosed — §5.Z: capability only, no new authority |
| 23 | Reprocessing overwrite/deduplication | Foreclosed — §5.W: no caching or deduplication mechanism anywhere in this design |
| 24 | History loss | Same, §5.W |
| 25 | Rollback fiction | Foreclosed — no distributed-transaction or rollback mechanism of any kind is proposed anywhere in this design; a downstream acceptance failure never retroactively invalidates the OCR mechanism's own already-honest disclosure |
| 26 | Exception laundering | Foreclosed — §5.K/test item 16: a genuine, unexpected fault propagates unchanged through every tier of this design, mirroring `OcrExecutionSequencer`'s own no-catch discipline exactly |
| 27 | New lifecycle/background behaviour | Foreclosed — no scheduler, queue, or background worker anywhere in this design; `EvidenceIntelligenceOcrCoordinator.recognise` is an ordinary, synchronous-from-the-caller's-perspective `suspend fun` |
| 28 | Architectural duplication | Foreclosed — this design introduces exactly one new class, mirroring an existing, already-accepted pattern (`EvidenceIntelligenceReasoningCoordinator`) rather than inventing a parallel one |
| 29 | Dependency creep | Foreclosed — §8: zero new dependencies required by this plan itself; a future `DoclingOcrProviderAdapter` implementation's own dependency is explicitly deferred, not smuggled in here |

**Two items (7, 13) do not fully resolve to "foreclosed" — both are
explicitly, honestly disclosed as future, adapter-or-composition-tier
work this plan names as required but does not, and structurally
cannot, implement without a concrete adapter to implement it in
(§4).** This is not a defect requiring correction inside this
document's own authority — it is the direct, honest consequence of §4's
own corrected conclusion that provider *implementation* (not
authorization, which is now resolved) remains out of scope, and
resolving either item fully would require exactly the authority this
document declines to invent — writing a concrete adapter. No item
requires changing adopted upstream governance; none triggers a STOP.

---

## 10. Citation and cross-reference audit

Fresh-checked, this document, after drafting: every `§N` self-reference
verified against this file's own heading positions (twelve top-level
numbered sections, 1-12, this one being §10 — corrected from an
original drafting-time defect that wrongly claimed twenty-three,
apparently copied from the Unit 12 Scope Lock's own, unrelated section
count; caught and fixed during this narrow accuracy correction's own
fresh audit); every external citation to the Unit 12 Scope Lock, the
Contract Design, and the Scope Lock verified against the actual heading
text of each (re-confirmed via `grep -n "^## "` against all three,
matching the same heading maps already independently verified during
the Unit 12 Scope Lock's own owner-acceptance review). Every
current-code fact in §2/§3, above, is drawn from a fresh read performed
in this same drafting pass, with file paths and, where stable,
approximate line numbers given — not carried forward from any prior
turn's own report without re-verification.

**This narrow accuracy correction's own citation audit.** Every
Docling-selection citation (`SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` §5,
`DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md` §2) and every
Docling-authorization citation (the Docling Authorization's own
Status, §4, §6, §9) added by this correction was independently
re-verified against the actual adopted text of each source document in
this same review pass, not carried forward from the Docling
Authorization's own prior owner-acceptance report without
re-verification. Every internal `§N`/`§5.X` self-reference this
correction added or edited was checked against this document's own
unchanged heading structure (§5's own lettered subsections, A-AC, were
not touched by this correction and remain at their own already-verified
positions). Every R/C/O/F entry this correction affected (§8's own
impact-map row for the concrete adapter; §12's own first deferral
bullet) was checked against the Docling Authorization's own §9/§11
tables for consistency, confirming this plan's own "CONDITIONALLY
AUTHORIZED" language matches that document's own "Now authorized to be
proposed and built" finding exactly, neither overstating nor
understating it.
Every illustrative Kotlin name (`EvidenceIntelligenceOcrCoordinator`,
`"ocr-transcription"`, `analyseEvidenceAsOwner`) is explicitly marked
non-frozen at first use; every reused name
(`OcrMechanism`/`OcrRecognitionRequest`/`OcrRecognitionOutcome`/
`EvidenceAnalysisRequest`/`EvidenceAnalysisResult`/`analyseEvidence`/
`EvidenceIntelligenceInvocationGate`/`EvidenceCustodian.retrieveManifest`/
`EvidenceSourceManifest`) is an existing, already-accepted production
type or member, confirmed present by direct file inspection in this
same pass, never assumed.

---

## 11. Conflicts or ambiguities

**None found.** Every decision in §5-§6, above, either reuses an
already-accepted mechanism unchanged or names a genuinely new,
narrowly-scoped component mirroring an existing, already-accepted
pattern. §4's own corrected conclusion (provider *authorization*
resolved; provider *implementation* still out of scope) does not
conflict with anything the Unit 12 Scope Lock or the Docling
Authorization decided — the Unit 12 Scope Lock's own §20 deferred
exactly the implementation question this plan still defers, and the
Docling Authorization's own §7/§9 explicitly confirm Unit 12's own
composition wiring is unaffected by, and does not require re-adoption
on account of, Docling's own provider authorization. This plan's own
narrow accuracy correction does not narrow or widen either document's
own deferral, only reconciles this plan's own text with what they now,
together, establish.

---

## 12. Explicit deferrals (restated and extended from the Unit 12 Scope Lock's own §20)

- Concrete `DoclingOcrProviderAdapter` implementation (§4, above) — the
  provider/engine question itself is no longer deferred (Docling is
  authorized); the implementation act is a separate, future
  implementation-planning decision, not this plan's to make or perform.
- Timeout, page/pixel/output-size, and concurrency *enforcement
  mechanisms* (§5.L-N, above) — required, but implementable only
  inside a future, authorised concrete adapter or its own composition.
- A structurally owner-only entry point for OCR invocation specifically
  (§6.2, above) — available as a future, separate, narrow governance
  option, not decided or required here.
- `CandidateArtifactProduced`/`CandidateRecordProduced` promotion
  *policy* — the minimum mapping's own safe default (§5.R) is not a
  policy decision; a future, separate decision may choose to route OCR
  output through either path under stated criteria this plan does not
  supply.
- `pageCount` derivation from Tier A's own `TierAMediaFacts` (§5.K) —
  named as a possible future refinement, not required for the minimum
  design.

---

## Final Recommendation

**READY FOR OWNER REVIEW.**

This plan designs the smallest implementation surface the Unit 12
Scope Lock authorises: one new, narrowly-dependent coordinator; one
new, optional constructor parameter on an already-accepted class; one
new composition-root construction line; zero new permission/resource
surface; zero new dependencies. It freezes a manifest-verified,
non-tautological integrity sequence the current implementation
genuinely lacks, an honest timeout/resource-limit failure mapping, and
a deliberately conservative `EvidenceAnalysisResult` default that
forecloses accidental evidential-truth promotion by construction. It
independently confirms, and does not attempt to work around, both
limitations the Unit 12 Scope Lock's own owner-acceptance review
disclosed: timeout enforcement remains a future adapter's own
responsibility, and owner-authority for `analyseEvidence` remains a
caller-discipline guarantee, not a structural one, unless a further,
separate governance decision changes that.

**Corrected by narrow accuracy review (Status, above; §4, above):**
Docling is the already-adopted, demonstrated OCR mechanism, and is now
separately, further authorized as the concrete provider behind
`OcrProviderAdapter` by the Docling Authorization (`c5b7aad`). This
plan itself still does not select, name, evaluate, implement, or
authorize the *implementation* of a concrete OCR provider or engine —
that remains a distinct, separately-governed implementation act,
confirmed absent from the current dependency graph and from this
plan's own authority, even though the governance question of *which*
provider is now resolved. Adoption of this corrected document does not
itself authorize OCR or Tier B implementation; it authorizes the next,
separately governed steps — actual implementation of the composition
wiring this plan designs, and/or a future `DoclingOcrProviderAdapter`
implementation proposal (now unblocked at the provider-authorization
tier, per §4 above, but not performed here) — each to be independently
proposed and reviewed. Docling's own runtime/model/cache provisioning
and Document Ingestion's own Tier B routing remain, as before, entirely
separate and unaddressed by this plan.
