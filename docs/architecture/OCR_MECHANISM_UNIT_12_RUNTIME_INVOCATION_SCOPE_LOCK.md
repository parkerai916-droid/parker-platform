# OCR Mechanism — Unit 12 Runtime Invocation Scope Lock

## Status

**Draft for owner review. Not yet accepted, canonical, or
implementation-authorising.** This document resolves exactly the
governance blocker `docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md`
("the Implementation Plan") §7, §16, and Unit 12 already named, and only
that blocker: `docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md` ("the Scope
Lock") §18 items 1-3 in full, and items 4-5 to the extent required so
that no runtime path could expose rejected OCR output ungated. No
Kotlin is implemented, proposed as a diff, or changed by this document.
No dependency is added. No `ParkerRuntime` wiring, `Resource`
registration, or `ActionVocabulary` registration is performed. Neither
`src/` nor `tests/` is touched. Nothing is staged, committed, or
pushed.

**This document reopens, redesigns, or reinterprets none of:**
`docs/decisions/CDR-006_CONSTITUTIONAL_CLASSIFICATION_OF_ORIGINAL_EVIDENCE_CUSTODY_AND_IMMUTABILITY.md`
("CDR-006"),
`docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md`
("CDR-007"),
`docs/decisions/CDR-008_MEMORY_CORE_DOWNSTREAM_RELEVANCE_BOUNDARY.md`
("CDR-008"), `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md` ("the
Contract Design"), the Scope Lock, `docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md`,
`docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` and its
Amendments, `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`,
`docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md`,
`docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_SCOPE_LOCK.md`,
`docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`,
`docs/architecture/DOCUMENT_INGESTION_CDR007_OCR_EVIDENCE_INTELLIGENCE_CROSS_REFERENCE_SCOPE_LOCK.md`
("Alignment Amendment 5"), `docs/architecture/DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md`,
`docs/architecture/DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md`
("Unit 2"), `docs/architecture/DOCUMENT_INGESTION_DERIVATIVE_TO_MEMORY_CORE_REGISTRATION_SCOPE_LOCK.md`,
`docs/architecture/SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`, or
`docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`. It amends none of them.
It resolves an item every one of them already, explicitly, left open —
converting an already-authorised shape into a fixed invocation-authority
decision, exactly as `DOCUMENT_INGESTION_CDR007_OCR_EVIDENCE_INTELLIGENCE_CROSS_REFERENCE_SCOPE_LOCK.md`
did for Alignment Amendment 5, and exactly as the Scope Lock's own §18
anticipates ("a future OCR Mechanism Scope Lock revision, should one
prove necessary").

## 1. Purpose

Resolve, and resolve only, the four governance items
`docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md` §16 names as
blocking Unit 12 ("Runtime Composition"):

1. Owner control and authorisation for machine-triggered OCR invocation
   (Scope Lock §18 item 1).
2. Design and acceptance of the composition-level coordinator that
   consumes a Tier-B-eligibility disclosure — Evidence Processing's own
   `RequiresOcr`, or Document Ingestion's own `RequiresTierB` — and
   triggers an Evidence Intelligence analysis in response (Scope Lock
   §18 item 2).
3. Whether a dedicated Permission Engine proposal class is required for
   OCR invocation specifically (Scope Lock §18 item 3).
4. Output-quality validation policy/threshold and permission gating for
   rejected output (Scope Lock §18 items 4-5) — resolved here only to
   the extent Implementation Plan §16's own closing sentence requires
   ("should not begin before items 4-5 are at least addressed to the
   extent runtime exposure of rejected output would otherwise be
   ungated"), not as a full quality-threshold policy.

This document additionally fixes the exact reuse relationship between
Document Ingestion's own `TierADocumentRoutingResult.RequiresTierB` and
Evidence Intelligence's own `analyse` operation — the specific
reconciliation `DOCUMENT_INGESTION_CDR007_OCR_EVIDENCE_INTELLIGENCE_CROSS_REFERENCE_SCOPE_LOCK.md`
§5 item 2 and §7 left explicitly to "OCR Mechanism's own still-open
Unit 12 governance" — because the task motivating this document is to
make Document Ingestion's own Tier B implementation separately
authorisable. It does not authorise Tier B implementation itself; that
remains a future, separate implementation-planning decision (§18,
below).

It does not decide, and must not be read as deciding: concrete OCR
provider identity, output-quality *threshold* values, permission
gating for a specific class of rejected output beyond "never ungated,"
Docling or structured-document-conversion capability, or any Memory
Core, Knowledge, QMD/RKS, deletion/retention, or DerivativeReview
authority change.

## 2. Authoritative sources inspected fresh (this document)

**OCR Mechanism governance (full):** the Contract Design; the Scope
Lock (§1-§18, all fourteen frozen boundaries); the Implementation Plan
(§1-§16, all twelve unit definitions, the completion criteria, and the
blocked-work list); `docs/reviews/OCR_MECHANISM_SCOPE_LOCK_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`
and `docs/reviews/OCR_MECHANISM_SCOPE_LOCK_DEFECT_CONFIRMATION_REVIEW.md`
(confirming the Scope Lock's own four corrections were applied and its
status moved from Draft to Accepted, notwithstanding the Scope Lock
file's own un-updated Status header text — disclosed at §22, below);
`docs/reviews/OCR_MECHANISM_PROGRAMME_COMPLETION_REVIEW.md` (confirming
Units 1-11 accepted, Unit 12 blocked, and the exact governance items
blocking it).

**Document Ingestion governance (full or targeted):**
`DOCUMENT_INGESTION_CDR007_OCR_EVIDENCE_INTELLIGENCE_CROSS_REFERENCE_SCOPE_LOCK.md`
(full — its own §4.B/D/E, §5 item 2, §6, §7); `DOCUMENT_INGESTION_PROGRAMME_GOVERNANCE_CLOSURE.md`
(rows naming Unit 12 as Document Ingestion's own pre-existing,
independent, unresolved blocker for Tier B production invocation
specifically); `DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md`
§2, §8 Owner Decision 6; `DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md`
§10 (producer identity, Tier B conditional `modelIdentity`/`modelVersion`
field), §20; `DOCUMENT_INGESTION_DERIVATIVE_TO_MEMORY_CORE_REGISTRATION_SCOPE_LOCK.md`
§6 (eligible derivative state — `Admitted` only), §22 (its own explicit
deferrals, none of which name Tier B); `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`
I-7 ("OCR-origin text is explicitly OCR-derived and cannot be
represented as native source text").

**CDR-007 and CDR-008 (full):** CDR-007's operative classification
(document ingestion, OCR, transcription, and extraction as Evidence
Intelligence's own analytical functions) and its "not a truth
authority" boundary; CDR-008's Memory Core / downstream relevance
boundary (confirmed unaffected — §17, below).

**`docs/reviews/EVIDENCE_PROCESSING_UNIT_5_OCR_PLANNING_REVIEW.md`**
(full) — §3.8 (security boundaries: process/subprocess invocation,
command-injection surface, decompression-bomb and resource-exhaustion
exposure, temporary-file handling, network access), cited directly at
§14, below; §3.9 (performance: "seconds to minutes per page"), cited
directly at §7 and §14, below.

**Current implementation, fresh-inspected:**

- `src/interfaces/PermissionEngine.kt` — `PermissionAction`,
  `ResourceType` enums, unmodified by anything this document decides.
- `src/runtime/DefaultPermissionPolicy.kt`, `src/composition/ParkerRuntime.kt`
  (full `buildAndRegisterRuntimeGraph`, `ResourceRegistry`/`ActionVocabulary`
  registration blocks, and the `analyseEvidence` entry point, lines
  ~1813-1839).
- `src/runtime/EvidenceIntelligenceInvocationGate.kt` (full) — the
  existing, already-registered, already-used invocation-level
  permission convention this document reuses in full (§5, below).
- `src/runtime/DefaultEvidenceIntelligence.kt` (`analyse`, in full) —
  confirmed to hold exactly two constructor dependencies
  (`EvidenceIntelligenceInputResolver`, `EvidenceIntelligenceReasoningCoordinator?`)
  and **no reference of any kind to `OcrMechanism`, `OcrExecutionSequencer`,
  or `OcrProviderAdapter`** — confirmed also by a repository-wide grep
  finding those three types referenced only in their own three files
  (`src/interfaces/OcrMechanism.kt`, `src/interfaces/OcrProviderAdapter.kt`,
  `src/runtime/OcrExecutionSequencer.kt`). This is the exact, disclosed
  gap §11 and §18, below, name.
- `src/runtime/EvidenceIntelligenceInputResolver.kt` (header and
  `resolve`-adjacent text) — confirms input resolution reaches
  `EvidenceCustodian.retrieve` directly, with no authoritative-manifest
  verification step of the kind `TierAOwnerInvocationCoordinator`
  already performs for Tier A (§10, below).
- `src/runtime/EvidenceIntelligenceAcceptanceCoordinator.kt` (full) —
  confirms the real, accepted, four-dependency dispatch shape for every
  `EvidenceAnalysisResult` variant (§12, below).
- `src/interfaces/EvidenceIntelligence.kt` (full) — `EvidenceAnalysisRequest`
  (`analysisKind: String`, `evidenceArtifactIds: List<EvidenceArtifactId>`,
  `memoryCoreReferences`, `reasoningContext`), the four-way
  `EvidenceAnalysisResult` sealed class (`TransientOutput`,
  `CandidateArtifactProduced`, `CandidateRecordProduced`,
  `CandidateKnowledgeProduced`), `CandidateMemoryCoreRecord`.
- `src/interfaces/TierADocumentIngestionRouter.kt` — `TierADocumentRoutingResult.RequiresTierB(reason: String, mediaFacts: TierAMediaFacts)`,
  `TierAMediaFacts(receivedMediaType, mechanicallyDetectedMediaType,
  originalFileName, disagreement)`.
- `src/runtime/TierAOwnerInvocationCoordinator.kt` — confirmed exactly
  two dependencies (`EvidenceCustodian`, `TierADocumentIngestionRouter`),
  no `MemoryCore`, no `EvidenceIntelligence`, no `OcrMechanism`
  reference of any kind — Tier A remains structurally incapable of
  invoking Tier B (§8, below).
- `src/runtime/OwnerLocalFileIngressCoordinator.kt` — `MAX_SOURCE_BYTES
  = 64L * 1024L * 1024L`, the one existing Parker source-byte bound,
  reused as a reference point at §14, below.
- `src/runtime/DefaultEvidenceCustodian.kt` — confirmed no general
  byte-size bound exists at the `accept` boundary itself (only the
  local-file-ingress entry point bounds it, at 64 MiB).

## 3. Current blocker — exact statement

**Not** a `PermissionAction` gap, **not** a `ResourceType` gap (both
already resolved by reuse — §5, below), **not** an OCR-mechanism-
selection gap (already resolved — Units 1-11, restated at §11, below),
**not** an Evidence-Intelligence-mediation-model gap (already
established — §4, below). The unresolved surface is a **bounded
combination of exactly two governance decisions Unit 12 itself
names**, both about runtime *invocation authority* rather than
component *contract*:

1. **Who/what may lawfully trigger a Tier-B-eligible `analyse` call, and
   how.** Implementation Plan §16 item 2's "composition-level
   coordinator that consumes `RequiresOcr`" was never designed, so no
   accepted governance states whether that coordinator is a new Kotlin
   type, an existing entry point reused, or an owner action with no
   coordinator at all.
2. **Whether that invocation needs its own, dedicated permission
   surface**, distinct from the general "invoke Evidence Intelligence"
   gate already accepted and wired (Implementation Plan §16 item 3).

Items 4-5 (output-quality policy; rejected-output gating) are **not**
independently blocking Unit 12's minimum start condition — Implementation
Plan §16's own closing sentence requires only that runtime exposure of
rejected output not be *ungated* when Unit 12 begins, not that a full
threshold policy exist. §12, below, confirms this narrower condition is
already met by already-accepted governance — every dispatch leg a
rejected-quality OCR disclosure could reach is already either
self-gating (`EvidenceCustodian.accept`) or independently
permission-gated (`EvidenceIntelligenceAcceptanceCoordinator`'s own
`PermissionEngine` reference), and a disclosure Evidence Intelligence's
own judgement does not produce as a candidate at all becomes
`TransientOutput`, never durable and never exposed as though accepted —
without inventing a threshold.

**A related, but distinct, gap this document also discloses (§11,
below):** even after items 1-2 are resolved, Evidence Intelligence's
own production implementation (`DefaultEvidenceIntelligence`) holds no
`OcrMechanism` dependency today. Resolving invocation authority does
not, by itself, make Tier B reachable in production; a separate,
future implementation unit must still compose `OcrExecutionSequencer`
into Evidence Intelligence's own dependency graph. Adoption of this
document clears the invocation-authority blocker such a future unit
would otherwise face; it does not itself authorise that unit's
implementation to begin — a separate implementation-planning proposal,
independently reviewed, remains required (§18, below; Final
Recommendation).

## 4. Existing OCR/Tier B authority (restated, not reopened)

- The OCR mechanism (Units 1-11, Accepted) is a provider-neutral,
  dependency-free capability, invoked only through Evidence
  Intelligence's own orchestration (Scope Lock §4; Contract Design
  §12) — never directly, never self-triggered, never from a background
  process (Scope Lock §4).
- Evidence Intelligence's own invocation is already gated, already
  wired, and already production-accepted: `ParkerRuntime.analyseEvidence`
  (§5, below).
- Document Ingestion "routes to, never owns" Tier B (Alignment
  Amendment 5 §4.B, §4.D) — it may disclose that a Tier B
  transformation is warranted or occurred; it may never invoke, gate,
  or authorise recognition itself, and this document creates no path
  by which it could.
- `APPROVED` (`DerivativeReviewRegistry`) grants no OCR or Evidence
  Intelligence authority (Alignment Amendment 5 §4.I, restating Unit 3
  §8) — unaffected, unreopened here.

## 5. Permission/action/resource decision

**No new `PermissionAction`, no new `ResourceType`, and no new,
OCR-specific `ActionVocabulary` verb phrase or `ResourceId` are
authorised or required.**

Evidence Intelligence's own invocation is already gated by exactly one,
already-accepted, already-registered convention:
`EvidenceIntelligenceInvocationGate` (`src/runtime/EvidenceIntelligenceInvocationGate.kt`),
whose `ANALYSE_ACTION_NAME` ("evidence-intelligence.analyse") maps to
the existing `(PermissionAction.EXECUTE, ResourceType.DOCUMENT)` pair,
confirmed registered in both `ResourceRegistry` and `ActionVocabulary`
by `ParkerRuntime.kt` (lines ~1239, ~1268), and confirmed exercised by
the real, production `DefaultPermissionPolicy` through
`ParkerRuntime.analyseEvidence` (lines ~1813-1839).

**Why this already suffices for OCR specifically.** This gate is
**invocation-level, not `analysisKind`-level** — its own KDoc states it
gates "the 'invoke Evidence Intelligence' domain act... once, never
each reference within it," and `EvidenceAnalysisRequest.analysisKind`
is an open `String` the gate never inspects. An OCR-eligible analysis
request (`analysisKind` naming OCR/transcription; `evidenceArtifactIds`
naming the image-bearing artefact) is, to this gate, indistinguishable
in kind from any other `analyse` invocation already gated today. Scope
Lock §18 item 3's own question — "whether a dedicated Permission Engine
proposal class is required for OCR invocation specifically, as
distinct from the already-frozen general 'invoke Evidence Intelligence'
gate" — is answered: **no**, because OCR invocation is not a distinct
invocation path; it is one value of an already-gated operation's own
open input.

**Illustrative, non-frozen `analysisKind` value.** A future
implementation may use a value such as `"ocr-transcription"` to name an
OCR-eligible analysis request — not frozen here, consistent with this
programme's own "exact Kotlin names, method signatures... remain a
future Implementation Plan's own responsibility" discipline (Contract
Design §11, restated by Scope Lock §18 item 8).

**Do not invent a parallel security model.** No OCR-specific
`ResourceId`, verb phrase, or policy rule is authorised. Constructing
one would violate this document's own §1 purpose and the governing
task's own "prefer an existing PermissionAction and ResourceType"
instruction, where — as confirmed here — a governed alternative
already, fully, exists.

## 6. Owner/runtime-invocation decision

**The authorised runtime-component role is `ParkerRuntime.analyseEvidence`
itself — no new coordinator class is authorised or required at the
invocation-authority tier.**

Compared explicitly, per the task's own instruction:

- **A narrowly dedicated "Tier B invocation coordinator," mirroring
  `TierAOwnerInvocationCoordinator`** — **rejected.** Any such
  coordinator would need to call `EvidenceIntelligence.analyse`
  directly or hold a reference to the OCR mechanism directly. The
  former duplicates `ParkerRuntime.analyseEvidence`'s own already-gated
  responsibility for no architectural gain; the latter is expressly
  foreclosed by the Scope Lock's own §4 ("may be invoked only by an
  authorised Evidence Intelligence orchestration path") and §13 (no
  platform-subsystem dependency of the OCR mechanism's own, and no new
  peer holding one on its behalf).
- **The existing OCR coordinator (`OcrExecutionSequencer`)** — not a
  viable invocation-authority host; it is Evidence Intelligence's own
  internal dependency (Scope Lock §3, §13), holds no `PermissionEngine`
  reference of its own by design, and must not acquire one (Contract
  Design §2 "Authorise its own invocation").
- **`ParkerRuntime.analyseEvidence`, reused unchanged** — **adopted.**
  It already performs exactly the sequence Unit 12 needs: permission
  evaluation, then `evidenceIntelligence.analyse(request)`, then
  `evidenceIntelligenceAcceptanceCoordinator.dispatch(...)`. Extending
  Evidence Intelligence's own internal composition to actually reach
  the OCR mechanism when appropriate (§11, below) requires no change to
  this entry point's own signature, gating, or sequencing.

**The "composition-level coordinator that consumes `RequiresOcr`"
(Scope Lock §18 item 2) is resolved as a role, not a class**: the
*owner* is that coordinator — a human decision, exercised by calling an
existing entry point with an existing identity, not a new piece of
running code. Where a future implementation finds a genuine need for a
thin, code-level helper that constructs an `EvidenceAnalysisRequest`
from a `RequiresTierB`/`RequiresOcr` disclosure (for example, to avoid
the owner hand-assembling the request), that helper may be introduced
by a future, separate implementation unit — but it composes the
request only; it holds no `PermissionEngine` reference of its own,
calls `ParkerRuntime.analyseEvidence` (or is itself absorbed into that
method) rather than `EvidenceIntelligence.analyse` directly, and is not
frozen, named, or required by this document.

**Disclosed limitation, not created here, not corrected here.**
Unlike `invokeTierAIngestionAsOwner`/`registerDerivativeInMemoryAsOwner`/`importEvidenceFileAsOwner`
— each structurally forced to `PrincipalId(config.ownerPrincipalId)`,
with no `requestingPrincipalId` parameter of any kind —
`analyseEvidence` accepts a caller-supplied `requestingPrincipalId`,
and the coarse `(PermissionAction.EXECUTE, ResourceType.DOCUMENT)`
policy rule it is gated by does not itself discriminate by principal
(`DefaultPermissionPolicy`'s own flat, principal-blind rule-matching
architecture). "Owner-triggered," as used throughout this document,
therefore means *whoever calls `analyseEvidence` is expected to supply
the owner's own identity* — a caller-discipline guarantee, not the
stronger, structural impossibility-of-substitution guarantee Document
Ingestion's own three entry points provide. This is a pre-existing
property of already-accepted Evidence Intelligence Implementation Plan
Units 6/8, confirmed unaffected by anything this document decides — it
governs every existing `analyseEvidence` call today, for every
`analysisKind`, not only a future Tier-B-eligible one, and correcting
it (for example, by giving `analyseEvidence` the same structurally
owner-only shape) would be a change to already-accepted Evidence
Intelligence governance and code, squarely out of scope for this
governance-only document. A future implementation unit introducing the
optional request-construction helper named above may reasonably choose
to make *that* helper structurally owner-only, closing this gap for
Tier B specifically without touching `analyseEvidence` itself — but
this document does not require that choice, and does not claim the gap
is already closed.

## 7. Invocation semantics decision

**Explicit, owner-triggered only — the same explicit-invocation pattern
Document Ingestion's own owner-facing entry points establish, and the
only pattern the Scope Lock authorises.** As §6, above, discloses,
`analyseEvidence`'s own owner-triggering is a caller-discipline
guarantee, not Document Ingestion's own stronger structural one — that
distinction is not repeated at every mention below, but applies
throughout this section identically.

- **Not automatic after `RequiresTierB`/`RequiresOcr`.** Both are
  classification results, never execution commands (§8, below,
  restated for Tier A; Scope Lock §4's own "may not self-trigger" for
  Evidence Processing's `RequiresOcr` identically). Producing either
  result performs no invocation of any kind, and this document creates
  no path by which it could.
- **Evidence-Intelligence-mediated, not Evidence-Intelligence-initiated.**
  Evidence Intelligence is the *sole lawful path* to OCR (§4, above),
  but it does not itself decide *when* to analyse — `analyseEvidence`
  is called only when a caller invokes it, gated by the existing
  invocation-level permission check (§5, above), whatever principal
  that caller supplies.
- **Already governed, not invented here.** `ParkerRuntime.analyseEvidence`
  is an existing, accepted, explicit entry point; this document adds no
  new trigger mechanism, confirming rather than inventing "preserve the
  existing Parker pattern of a separate explicit owner action."
- **No background or asynchronous invocation is authorised.** Directly
  answering the Planning Review's own §3.9 open question (whether OCR's
  "seconds to minutes per page" performance profile requires a new
  runtime-lifecycle concept): **no.** `analyseEvidence` is an ordinary
  `suspend fun`, synchronous from its caller's own perspective; nothing
  in this document authorises a queue, scheduler, or background worker
  for OCR specifically, consistent with the Scope Lock's own §4 "may
  not start background work."

## 8. Tier A `RequiresTierB` boundary (restated, not reopened)

`TierADocumentRoutingResult.RequiresTierB` is, and remains, exactly
what `DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md` and
Alignment Amendment 5 already fixed: **a classification result, never
an OCR execution command.** Confirmed structurally: `TierAOwnerInvocationCoordinator`
holds exactly two dependencies (`EvidenceCustodian`,
`TierADocumentIngestionRouter`) — no `EvidenceIntelligence`, no
`OcrMechanism`, no `MemoryCore` reference of any kind — so it is not
merely undocumented but structurally impossible for a `RequiresTierB`
result to itself trigger anything. Producing it is Tier A's own,
complete, terminal act for that invocation; nothing this document
authorises changes that.

## 9. Eligible Tier B input

The only lawful input to a Tier-B-eligible `analyse` invocation is:

- **An already-custodied `EvidenceArtifactId`** — the same identity
  Document Ingestion's own `importEvidenceFileAsOwner`/Evidence
  Custodian's `accept` already minted, supplied via
  `EvidenceAnalysisRequest.evidenceArtifactIds`. No arbitrary path,
  URL, upload, Gmail identifier, or caller-supplied bytes are accepted
  by this or any authorised path — `EvidenceAnalysisRequest` carries no
  such field, and this document authorises no new one.
- **A prior `RequiresTierB` (or `RequiresOcr`) result, used only as
  eligibility evidence, never as a structural precondition.** Nothing
  in `analyseEvidence`'s own accepted signature requires one to be
  supplied or checked; an owner may invoke analysis on any evidence
  they judge warrants it. This document does not add a structural
  eligibility gate — doing so would require a code change to an
  already-accepted entry point, which is out of scope for a
  governance-only document, and is not, in any case, necessary to
  unblock Unit 12.
- **No second evidence authority is created.** `EvidenceCustodian`
  remains the sole admission authority (§3, Existing frozen
  boundaries, mirrored from every prior Document Ingestion unit this
  session).

## 10. Source-retrieval/integrity boundary

**Freezes a requirement for future implementation; performs no
retrieval itself.**

Fresh inspection of `EvidenceIntelligenceInputResolver` confirms it
calls `EvidenceCustodian.retrieve` directly, with no separate
authoritative-manifest verification step of the kind
`TierAOwnerInvocationCoordinator` already performs for Tier A (manifest
retrieval → byte retrieval → byte-length verification → SHA-256
verification → exactly one router call, per
`DOCUMENT_INGESTION_AUTHORITATIVE_SOURCE_MANIFEST_RETRIEVAL_SCOPE_LOCK.md`
§21). This is a genuine, disclosed gap in Evidence Intelligence's own
current input resolution, not invented by this document.

**Required for any future Tier B invocation:** before OCR execution,
the same manifest-verified sequence Tier A already performs must be
applied to the image-bearing content — retrieve the authoritative
manifest, retrieve the bytes, verify byte length against the manifest,
verify SHA-256 against the manifest's own persisted digest. **The
expected SHA-256 must be the manifest's own already-persisted value,
never a digest recomputed from the same retrieval and then compared to
itself** — the identical "no digest tautology" discipline
`TierAOwnerInvocationCoordinatorTest` already proves for Tier A,
required here by the same reasoning, not reinvented. Whether this
sequence is added to `EvidenceIntelligenceInputResolver` itself, or
performed by a thin pre-check before an OCR-eligible `EvidenceAnalysisRequest`
is constructed, is left to future implementation planning — this
document freezes the requirement, not the mechanism.

## 11. OCR mechanism authority (restated, and one gap disclosed)

**Restated, not reopened.** The mechanism identity, version identity,
and configuration identity disclosures Units 1-11 already implement
(Implementation Plan §5, Programme Completion Review §5) remain exactly
as accepted: provider-neutral (no concrete engine named anywhere in
`src/`), no network access authorised by anything accepted to date
(Scope Lock §15's own conditional — closed by §14, below), local-only
by omission of any network-capable dependency.

**Model identity/version.** Required whenever the eventual concrete
provider is itself model-backed, exactly as
`DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md` §10
already anticipates for Document Ingestion's own, separate,
not-yet-built future recording capability (§13, below) — this document
adds no new field and narrows nothing.

**Disclosed gap, not resolved here.** `DefaultEvidenceIntelligence`
holds no `OcrMechanism`/`OcrExecutionSequencer` dependency today (§2,
§3, above). Resolving invocation authority (§5-§7, above) does not
itself wire the OCR mechanism into Evidence Intelligence's own
composition. A future, separate implementation unit must: (a) give
`DefaultEvidenceIntelligence` (or an equivalent composition point) an
`OcrMechanism` dependency; (b) decide, as ordinary engineering
judgement within the boundaries this document and the Scope Lock
already fix, when `analyse`'s own logic invokes it (for example, when
resolved evidence carries an image-bearing media type). As above,
adoption of this document clears the invocation-authority blocker;
it does not itself authorise that future unit's implementation to
begin, and does not perform, or choose the shape of, any part of it
(§18, below; Final Recommendation).

## 12. OCR output semantics (restated in full, mapped to the real, accepted `EvidenceAnalysisResult` shape)

Restated from Units 1-11 and mapped, for the first time, onto the real,
already-implemented `EvidenceAnalysisResult`/`EvidenceIntelligenceAcceptanceCoordinator`
shape this document freshly confirmed (§2, above):

- **OCR-observed text, structural observations, page association,
  confidence, warnings, completeness** — exactly the disclosure Units
  1, 6, 7, 8 already implement (recognised text; page-aligned,
  mixed-fidelity segments; a structured, provider-neutral identity
  record; a working, transient confidence signal; an ordered warnings
  list; the nine-way outcome model). None of these becomes evidential
  truth merely through extraction (Contract Design §2, §8) — this
  document changes none of it.
- **Coordinates/bounding boxes** — **not authorised.** No unit of the
  accepted programme discloses spatial coordinates; this document does
  not add one, and no future implementation may fabricate one merely
  because a provider happens to supply it, absent a future governance
  decision to disclose it honestly.
- **Derivative identity — does not exist for Tier B in Document
  Ingestion's own sense.** Tier B output is never wrapped in a
  `DerivativeGenerationRecord`/`DerivativeGenerationId` (§13, below);
  its own identity, where it has one at all, is whatever identity
  Evidence Custodian mints for a newly-accepted `CandidateEvidenceArtifact`
  (below), or none, where the result is `TransientOutput`.
- **Evidence Intelligence candidate material** — the OCR mechanism's
  own disclosure becomes, at Evidence Intelligence's own analytical
  judgement and only then, exactly one of three already-accepted
  `EvidenceAnalysisResult` variants, confirmed by fresh inspection of
  `EvidenceIntelligenceAcceptanceCoordinator.kt`:
  - **`CandidateArtifactProduced`** — the recognised text is itself
    worth registering as a new piece of evidence; dispatched, already
    self-gating, to the existing, unmodified `EvidenceCustodian.accept`
    boundary. This is the concrete realisation of the Contract Design's
    own §3/§5 "carried by the existing, unmodified `CandidateEvidenceArtifact`
    type" language — not a new artefact shape, and not a change to
    `EvidenceCustodian`.
  - **`CandidateRecordProduced`** — the recognised text supports a
    specific analytical claim about existing evidence; dispatched,
    gated by `EvidenceIntelligenceAcceptanceCoordinator`'s own
    `PermissionEngine` reference, to `MemoryCore.createAssertion`/`createRelationship`
    as a `CandidateAssertion`/`CandidateRelationship` — never a fourth,
    OCR-specific record kind.
  - **`TransientOutput`** — the recognition is useful only as working
    analytical material; never accepted, never durable, discarded or
    regenerated freely.
- **Memory Core records / Assertions** — never constructed by the OCR
  mechanism itself (Contract Design §2, §8; Scope Lock §3, §13); only
  ever by Evidence Intelligence's own already-existing, already-gated
  acceptance path, exactly as for any other analysis.
- **Knowledge Items** — never produced directly from OCR text; only
  ever via the same, existing, unmodified `CandidateKnowledgeProduced`/`KnowledgeSubmission.submit`
  path any other Evidence Intelligence analysis already uses, subject
  to that path's own, existing, unmodified gating (§17, below).

**A disclosed citation-vintage observation, not a conflict.** The
Contract Design's own §3/§5 prose ("carried by the existing, unmodified
`CandidateEvidenceArtifact` type... 'Candidate artefact produced'")
predates Evidence Intelligence Unit 1's own later-adopted, corrected
`EvidenceAnalysisResult` naming (`CandidateArtifactProduced`, not
"Candidate artefact produced" verbatim). The underlying mechanism is
identical; only the exact Kotlin name postdates the Contract Design's
own drafting. This document treats Evidence Intelligence's own accepted
implementation as controlling for naming, and the Contract Design as
controlling for the underlying authorisation — no rule conflicts.

## 13. Derivative-generation/admission decision

**Tier B/OCR output does not use Document Ingestion's own
`DerivativeGenerationId`/`DerivativeGenerationStorage`/`DocumentIngestionAudit`
pipeline, or its `prepare → ADMISSION_AUTHORISED → publish → ADMITTED`
sequence, at all.** That pipeline remains exclusively Tier A's own
(Document Ingestion Unit 2's frozen ownership; Document Ingestion Unit
6's own `TierADocumentRoutingResult.Admitted`-only eligibility,
restated by `DOCUMENT_INGESTION_DERIVATIVE_TO_MEMORY_CORE_REGISTRATION_SCOPE_LOCK.md`
§6). Tier B output instead follows the path §12, above, already
confirms: Evidence Intelligence's own existing `EvidenceAnalysisResult`
taxonomy and `EvidenceIntelligenceAcceptanceCoordinator`'s own existing
dispatch. **No new derivative store, identity authority, or parallel
pipeline is created by this document** — every leg already exists,
already implemented, already accepted, already permission-gated where
gating applies.

**Explicitly not resolved here, and not needed to unblock Unit 12:**
whether Document Ingestion may, in some future, separately governed
unit, construct its *own* `DerivativeGenerationRecord` disclosing that
an externally-obtained Tier B/OCR result relates to a source it
ingested (the scenario `DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md`
§10's conditional `modelIdentity`/`modelVersion` field reserves space
for). That is a Document-Ingestion-side recording capability, not an
OCR-Mechanism-side invocation-authority question, and is left, as
Alignment Amendment 5 §7 already leaves it, to future, separate
Document Ingestion governance.

## 14. Resource/security limits — frozen here (load-bearing)

The Planning Review's own §3.8 finding — that native OCR tooling
introduces process/subprocess invocation, command-injection surface,
decompression-bomb and resource-exhaustion exposure, and temporary-file
handling risks "no existing Evidence Processing governance has ever had
to address" — remains genuinely unresolved by anything accepted to
date. Confirmed: `DefaultEvidenceCustodian.accept` enforces no general
byte-size bound of its own; only the local-file-ingress entry point
bounds ingestion, at 64 MiB, and only for that one path. **This
document freezes the following conservative bounds, expressly
implementation-adjustable downward, never upward, without a future
governance decision:**

| Limit | Frozen bound | Basis |
| --- | --- | --- |
| Maximum source bytes for a Tier-B-eligible input | 64 MiB (`OwnerLocalFileIngressCoordinator.MAX_SOURCE_BYTES`, reused, not widened) | No governed Tier B-specific bound exists; reusing the one existing Parker source-byte bound is more conservative than inventing a larger one, and keeps a single, auditable ceiling |
| Maximum PDF page count | 200 pages | Conservative, independent bound on total page/memory exposure — **not** itself a wall-clock guarantee; that is the timeout row's own, independently-enforced job (below). At the Planning Review's own "seconds to minutes per page" finding, a full 200-page recognition can exceed the timeout well before all pages complete, in which case the timeout fires first and the invocation ends in a disclosed `Timeout` failure (§15), never a silently truncated success |
| Maximum image width/height | 10,000 × 10,000 pixels | Conservative bound against decompression-bomb-shaped input (a small file expanding to an enormous in-memory bitmap) |
| Maximum total pixels | 100,000,000 (100 megapixels) | A second, independent bound catching an extreme-aspect-ratio image that individually satisfies the width/height bound above |
| Maximum OCR output size | 20 MiB of recognised text | Bounds a pathological case where a hostile input causes unbounded or repeated recognised text; well in excess of any genuine document's own plausible transcript length |
| Timeout/deadline | 15 minutes per invocation | The actual, governing bound on wall-clock exposure for any invocation, page count notwithstanding — consistent with the Planning Review's own performance finding, generous enough for a legitimate multi-page document at the fast end of that finding, and short enough to guarantee termination; a future implementation must treat this as an independent, unconditional ceiling, not a figure reconciled against the page-count bound above |
| Process/job concurrency | Exactly 1 concurrent OCR invocation per Parker instance | The simplest, safest starting bound — avoids resource-contention and scheduling questions entirely; a future governance decision may raise this if a genuine need is demonstrated |
| Network access | **None authorised.** Closes the Scope Lock §15 conditional ("no network access, unless a future governance stage expressly authorises it") | This document is that future governance stage, and it does not authorise network access — local, in-process or local-subprocess-only execution remains the sole authorised model |
| Path traversal / command injection | Prohibited outright, restated, not newly invented (Scope Lock §15) | Binding on any future concrete adapter; no adapter is authorised by this document regardless |
| Temporary workspace | A controlled, implementation-defined temporary workspace, cleaned up according to rules a future implementation defines | Scope Lock §15's own already-frozen minimum, restated; this document adds no new mechanism |

**These bounds govern the concrete adapter/execution layer a future
implementation unit builds — they do not, and cannot, retroactively
apply to Units 1-11's own already-accepted, provider-neutral, in-memory
contract**, which holds no execution boundary of its own to bound
(Scope Lock §13). A future implementation unit introducing a concrete
`OcrProviderAdapter` must satisfy every bound above; this document
authorises no adapter that does not.

## 15. Failure semantics

Every outcome below must remain distinct and truthful; none may be
collapsed into another, and no false success or rollback fiction is
authorised, mirroring the discipline every Document Ingestion and
Evidence Intelligence unit this repository has already adopted:

- **Authorisation rejection** — the existing `EvidenceIntelligenceInvocationOutcome.NotAuthorised`
  shape, produced by `analyseEvidence` itself, before `analyse` is ever
  called (§5, above; already implemented, unmodified).
- **Source manifest not found / source bytes not found / source
  integrity mismatch** — governed by §10, above, once a future
  implementation adds the required manifest-verified sequence; must
  produce distinct, non-fabricated outcomes, mirroring
  `TierAOwnerInvocationOutcome.ManifestNotFound`/`SourceNotFound`/`DigestMismatch`'s
  own already-accepted shape.
- **Not eligible for Tier B / unsupported media / malformed source** —
  the OCR mechanism's own already-accepted, non-collapsible
  distinctions (Scope Lock §10; Implementation Plan Unit 7's seven
  `OcrRecognitionOutcome` sibling variants), unmodified.
- **Source/resource limit rejection** — a new, honest failure a future
  concrete adapter must produce when any §14 bound is exceeded; never
  silently truncated and presented as complete (a direct application of
  Scope Lock §10's own "operational/mechanical failure" category,
  applied to the concrete bounds this document adds).
- **OCR mechanism unavailable / OCR failure / timeout** — the same
  category, disclosed honestly, never silently retried or masked
  (Scope Lock §10).
- **Malformed/invalid OCR result** — belongs to Evidence Intelligence's
  own analytical judgement (Contract Design §6), producing
  `TransientOutput` or nothing, never a fabricated `CandidateArtifactProduced`/`CandidateRecordProduced`.
- **Preparation failure / authorisation-audit failure / publication
  failure / admitted-audit failure / reconciliation-required
  post-publication state** — **not applicable to Tier B**, because Tier
  B output never enters Document Ingestion's own `prepare → ADMISSION_AUTHORISED
  → publish → ADMITTED` pipeline (§13, above). These categories remain
  exclusively Tier A's own; this document invents no Tier-B analogue of
  them.
- **No false success. No rollback fiction.** A downstream acceptance
  failure (for example, `EvidenceCustodian.accept` rejecting a
  `CandidateArtifactProduced`) never retroactively invalidates the OCR
  mechanism's own already-honest disclosure, and this document creates
  no distributed-transaction or rollback mechanism of any kind — the
  identical discipline `DerivativeMemoryRegistrationCoordinator`'s own
  governance already establishes for a structurally comparable
  two-stage sequence.

## 16. Reprocessing behaviour

**A repeated, explicit Tier B execution against the same source must
not silently overwrite anything, and this document invents no
deduplication.** Because Tier B output never enters Document Ingestion's
own append-only generation history (§13, above), the relevant
append-only guarantee is instead whichever boundary actually receives
the output: `EvidenceCustodian.accept` (for `CandidateArtifactProduced`)
and `MemoryCore.createAssertion`/`createRelationship` (for
`CandidateRecordProduced`) are both already, independently, append-only
by their own existing, unmodified contracts. Two separate OCR
invocations against the same source, each producing its own candidate,
receive two separate, independently retrievable records — neither this
document, nor anything it authorises, collapses them.

## 17. Downstream effects

- **Memory Core.** Effect is exactly, and only, §12/§13's own already-
  described `CandidateArtifactProduced`/`CandidateRecordProduced`
  dispatch — never the Derivative-to-Memory-Core Registration Scope
  Lock's own `DerivativeMemoryRegistrationCoordinator`/`registerDerivativeInMemoryAsOwner`
  path, which remains exclusively for Tier A `Admitted` results (§13,
  above). This document does not silently join Tier B to that newer
  path, and no future implementation may do so without its own,
  separate governance decision — the Derivative-to-Memory-Core
  Registration Scope Lock's own §6 eligible-state freeze is not
  reopened here.
- **`DerivativeReview`.** Unaffected. Tier B output never becomes a
  `DerivativeGenerationRecord` (§13, above), so it is never eligible for
  `DerivativeReviewRegistry` in the first place; `APPROVED` grants no
  OCR or Evidence Intelligence authority regardless (§4, above,
  restating Unit 3 §8 and Alignment Amendment 5 §4.I).
- **Knowledge.** Unaffected beyond what Evidence Intelligence's own
  already-accepted `CandidateKnowledgeProduced`/`KnowledgeSubmission.submit`
  path already permits for any analysis — this document authorises no
  automatic Knowledge promotion merely because OCR succeeded; Evidence
  Intelligence's own analytical judgement decides, exactly as for any
  other input, whether Knowledge submission is warranted at all.
- **Evidence Intelligence.** Made executable for OCR specifically, at
  the invocation-authority tier (§5-§7, above); its own internal
  composition gap (§11, above) remains open, future work.
- **QMD/RKS.** Unaffected. CDR-008's own boundary (Memory Core's
  retrieval interface, not any producing component, is what CDR-008
  governs) is untouched; this document adds no retrieval capability, no
  canonical authority, and no automatic indexing of any kind for OCR
  output specifically, beyond whatever QMD/RKS already, generically,
  computes over content Memory Core's own unmodified interface already
  exposes.
- **Deletion/retention.** No OCR-output deletion cascade, retention
  period, or purge behaviour is invented. Whatever deletion/retention
  rule already governs a `CandidateEvidenceArtifact` once accepted, or
  an `Assertion` once created, governs an OCR-produced one identically
  — no new rule, no new exception. Document Ingestion's own §17
  deferral (Derivative Generation Record Scope Lock) is carried forward
  unchanged and is, in any case, inapplicable here (§13, above).

## 18. Implementation impact map

| Surface | Classification |
| --- | --- |
| `ParkerRuntime.analyseEvidence` reused, unmodified, as the sole owner-facing Tier-B-eligible invocation path | Confirmed — no change authorised or required |
| `EvidenceIntelligenceInvocationGate` reused, unmodified, as the sole permission gate | Confirmed — no new verb phrase, `ResourceId`, or policy rule |
| `DefaultEvidenceIntelligence` gaining an `OcrMechanism` dependency, and invoking it when appropriate | Required, future, separate implementation unit — not performed or authorised to begin here |
| A manifest-verified source-integrity sequence before OCR execution (§10) | Required, future, separate implementation unit |
| A concrete `OcrProviderAdapter` satisfying §14's resource/security bounds | Required, future, separate implementation unit; provider identity itself remains a further, separate governance decision (Scope Lock §18 item 6) |
| An optional, thin request-construction helper from `RequiresTierB`/`RequiresOcr` to `EvidenceAnalysisRequest` | Optional, future, separate implementation unit; not frozen, not required |
| Not required by any reading of this document | Any change to `OCR_MECHANISM_CONTRACT_DESIGN.md`, `OCR_MECHANISM_SCOPE_LOCK.md`, `EvidenceIntelligence.kt`'s own four public types, `EvidenceIntelligenceAcceptanceCoordinator.kt`, `TierAOwnerInvocationCoordinator`, `DerivativeGenerationRecord`/`DerivativeGenerationStorage`, `DerivativeMemoryRegistrationCoordinator`, any new `PermissionAction`/`ResourceType`, any new Memory Core field, any new Knowledge/QMD/RKS authority, any deletion/retention mechanism |

## 19. Required / conditional / optional / forbidden classification

| # | Rule | Class |
| --- | --- | --- |
| 1 | Tier B/OCR invocation occurs only through `ParkerRuntime.analyseEvidence` (or a future, narrow request-construction helper that itself calls it) | **R** |
| 2 | The existing `EvidenceIntelligenceInvocationGate` → `(EXECUTE, DOCUMENT)` gate governs every Tier-B-eligible invocation | **R** |
| 3 | A manifest-verified, non-tautological integrity sequence before OCR execution | **R**, for any future implementation |
| 4 | §14's resource/security bounds, on any future concrete adapter | **R** |
| 5 | An `EvidenceArtifactId` already in custody, as the sole input identity | **R** |
| 6 | A prior `RequiresTierB`/`RequiresOcr` result as eligibility evidence | **O** — never a structural precondition |
| 7 | Model identity/version disclosure, where the concrete provider is model-backed | **C** |
| 8 | A new `PermissionAction`, `ResourceType`, verb phrase, or `ResourceId` for OCR specifically | **F** |
| 9 | A dedicated "Tier B invocation coordinator" holding `EvidenceCustodian`/`OcrMechanism`/`PermissionEngine` references of its own | **F** |
| 10 | Automatic invocation from `RequiresTierB`/`RequiresOcr`, `Admitted`, startup, or any background/scheduled process | **F** |
| 11 | Network access for OCR execution | **F** |
| 12 | Tier B output entering Document Ingestion's own `DerivativeGenerationRecord`/`DerivativeGenerationStorage`/`DocumentIngestionAudit` pipeline | **F** |
| 13 | Tier B output entering the Derivative-to-Memory-Core Registration path | **F** |
| 14 | Coordinates/bounding-box disclosure | **F**, pending future governance |
| 15 | `APPROVED` as an OCR/Evidence Intelligence trigger | **F** |
| 16 | Automatic Knowledge promotion from OCR success alone | **F** |
| 17 | New QMD/RKS canonical or indexing authority | **F** |
| 18 | New deletion/retention mechanism | **F** |
| 19 | Deduplication of repeated Tier B executions | **F** |
| 20 | Rollback of an already-honest OCR disclosure on downstream acceptance failure | **F** |

## 20. Explicit deferrals

- Whether/how `DefaultEvidenceIntelligence` composes `OcrMechanism` and
  when its own `analyse` logic invokes it (§11) — future implementation.
- Concrete `OcrProviderAdapter` identity, library, or service (Scope
  Lock §18 item 6) — future, separate governance.
- Process/execution and deployment topology beyond §14's bounds (Scope
  Lock §18 item 7) — future implementation, within those bounds.
- A full output-quality validation *threshold* policy (Scope Lock §18
  item 4) — not required to unblock Unit 12 (§3, above); remains open.
- Whether Document Ingestion may someday construct its own
  `DerivativeGenerationRecord` disclosing an externally-obtained Tier B
  result (§13) — future, separate Document Ingestion governance.
- Coordinate/bounding-box disclosure (§12) — future governance, if ever
  pursued.
- Raising the concurrency bound above 1 (§14) — future governance, on
  demonstrated need.

## 21. Adversarial challenge table

| # | Challenge | Resolution |
| --- | --- | --- |
| 1 | Automatic Tier B invocation | Foreclosed by §7 — explicit owner-triggered only; `RequiresTierB`/`RequiresOcr` produce no invocation of any kind |
| 2 | Automatic OCR from Tier A | Foreclosed by §8 — `TierAOwnerInvocationCoordinator` structurally cannot reach Evidence Intelligence or the OCR mechanism |
| 3 | Owner authorisation bypass | Partially foreclosed by §5-§6 — no new invocation path is authorised, and every call is gated by the existing, unmodified `(EXECUTE, DOCUMENT)` check; **not** structurally foreclosed against a caller supplying a non-owner `PrincipalId` to `analyseEvidence`, since that gate is principal-blind and this document neither creates nor corrects that pre-existing property (§6's own disclosed limitation) |
| 4 | Caller-supplied identity/media/digest authority | Foreclosed by §9 — only an already-custodied `EvidenceArtifactId` is accepted; no caller-declared confidence or evidential value (Contract Design §4, restated) |
| 5 | Arbitrary path ingress | Foreclosed by §9 — `EvidenceAnalysisRequest` carries no path/URL/upload field, and this document adds none |
| 6 | Duplicate source authority | Foreclosed by §9 — `EvidenceCustodian` remains sole admission authority; no second one is created |
| 7 | OCR result treated as evidential truth | Foreclosed by §12 — recognised text is, at most, a candidate; Evidence Intelligence's own acceptance path is the sole gate to any durable form |
| 8 | Parser payload/OCR text converted to Assertion automatically | Foreclosed by §12 — `CandidateRecordProduced` requires Evidence Intelligence's own analytical judgement, gated by its own existing `PermissionEngine` reference; never automatic |
| 9 | Automatic Memory registration | Foreclosed by §13/§17 — no automatic path exists; every leg requires Evidence Intelligence's own judgement and, for `CandidateRecordProduced`, its own gate |
| 10 | Automatic Knowledge promotion | Foreclosed by §17 — `CandidateKnowledgeProduced` requires the same analytical judgement, unmodified |
| 11 | Evidence Intelligence laundering (Document Ingestion acquiring EI authority by routing to it) | Foreclosed by §4/§8 — Document Ingestion "routes to, never owns" Tier B; no authority transfers in either direction |
| 12 | `APPROVED` authority creep | Foreclosed by §17 — `DerivativeReview` is inapplicable to Tier B output in the first place (§13) |
| 13 | Missing OCR model/mechanism identity | Foreclosed by §11 — model identity/version required whenever the provider is model-backed, restating existing Document Ingestion governance |
| 14 | Fabricated confidence | Foreclosed by §12 — confidence remains transient, working material only, never durable, exactly as Units 1-11 already require |
| 15 | Fabricated coordinates/page association | Foreclosed by §12 — coordinates are explicitly not authorised; page association is limited to what Units 1-11 already, honestly, disclose |
| 16 | Output truncation presented as complete | Foreclosed by §14/§15 — a resource-limit rejection is a distinct, disclosed failure, never silent truncation |
| 17 | Unbounded pixel processing | Foreclosed by §14 — explicit width/height and total-pixel bounds frozen |
| 18 | Unbounded runtime | Foreclosed by §14 — explicit 15-minute timeout frozen |
| 19 | Hidden external-network OCR | Foreclosed by §14 — network access explicitly not authorised, closing the Scope Lock's own open conditional |
| 20 | Source mutation | Foreclosed by §4 — the Original-Evidence Boundary (Scope Lock §7) is restated, not reopened, and remains absolute |
| 21 | Duplicate derivative authority | Foreclosed by §13 — no new derivative store or identity authority is created |
| 22 | Non-atomic publication | Not applicable — Tier B output never enters the `prepare → ADMISSION_AUTHORISED → publish → ADMITTED` pipeline (§13) |
| 23 | Missing audit | `EvidenceCustodian.accept` and `MemoryCore`'s own write interface each already carry whatever audit obligation their own existing, unmodified contracts require; this document adds no exemption |
| 24 | Reprocessing collapse | Foreclosed by §16 — both dispatch legs are independently append-only by their own existing contracts |
| 25 | Silent overwrite | Foreclosed by §16, same reasoning |
| 26 | `ResourceId` registration omission | Not applicable — no new `ResourceId` is introduced (§5); the existing `EVIDENCE_INTELLIGENCE_INVOCATION_RESOURCE_ID` is already registered, confirmed at §2 |
| 27 | Permission verb not exercised by real production policy | Foreclosed — confirmed at §2/§5 that `ANALYSE_ACTION_NAME` is registered in both `ResourceRegistry` and `ActionVocabulary` and exercised by the real `DefaultPermissionPolicy` through the real `analyseEvidence` |
| 28 | Runtime object unreachable | Confirmed reachable — `analyseEvidence` is a real, accepted, production `ParkerRuntime` method today; the disclosed gap (§11) is Evidence Intelligence's own internal OCR composition, not this invocation path |
| 29 | Runtime object reachable without owner gate | Foreclosed — `analyseEvidence` evaluates `EvidenceIntelligenceInvocationGate`'s own `ExecutionRequest` before any further step, confirmed unmodified |
| 30 | QMD/RKS authority expansion | Foreclosed by §17 — no retrieval or indexing capability is added |
| 31 | Deletion cascade invention | Foreclosed by §17 — no new mechanism; existing rules apply unmodified |
| 32 | Retention-policy invention | Foreclosed by §17, same reasoning |
| 33 | Combined import→Tier A→Tier B→Memory orchestration | Foreclosed by §6-§7 — every step (`importEvidenceFileAsOwner`, `invokeTierAIngestionAsOwner`, `analyseEvidence`, and any eventual acceptance dispatch) remains a separate, explicit, independently-gated owner action; this document authorises no combined operation and adds no chaining of any kind |

No item resolves to a blocker for adopting this document. Thirty-two
of thirty-three challenges resolve cleanly within this document's own
sections; challenge 3 resolves to a disclosed, pre-existing limitation
of already-accepted Evidence Intelligence code (§6, above) that this
document neither creates, worsens, nor is authorised to correct.

## 22. Conflicts/ambiguities discovered

**One disclosed staleness, not a conflict, carried forward accurately:**
the Scope Lock's and Contract Design's own file-level Status headers
still read "Draft... Not accepted... Not canonical" despite both having
completed their own independent constitutional review cycles
(`OCR_MECHANISM_SCOPE_LOCK_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`,
Verdict: REQUIRES REVISION; `OCR_MECHANISM_SCOPE_LOCK_DEFECT_CONFIRMATION_REVIEW.md`,
Verdict: no further correction required, status "may move from Draft to
Accepted") and despite Units 1-11 having since been fully implemented
and accepted against them — functionally impossible unless that
acceptance occurred. This document treats the Contract Design and
Scope Lock as currently Accepted, controlling authority, consistent
with `OCR_MECHANISM_PROGRAMME_COMPLETION_REVIEW.md`'s own unqualified
treatment of both as such, and flags the header staleness here for a
future editor rather than silently relying on it or attempting to
correct a file this document's own "Files that must not change"
inheritance (Implementation Plan §13) forbids touching.

**One disclosed citation-vintage observation, not a conflict:** §12,
above, already discloses the Contract Design's own "Candidate artefact
produced" prose predating Evidence Intelligence Unit 1's later,
corrected `CandidateArtifactProduced` naming. No rule conflicts; only a
name postdates the document that first described its shape.

**No other conflict, duplicated authority, or accidental authority
expansion was found** across the Contract Design, the Scope Lock, the
Implementation Plan, Alignment Amendment 5, CDR-007, CDR-008,
`DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md`,
`DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md`, or
`DOCUMENT_INGESTION_DERIVATIVE_TO_MEMORY_CORE_REGISTRATION_SCOPE_LOCK.md`,
each freshly re-read for this document (§2, above).

## 23. Governance change classification

**A/B — clarification and narrow authority extension consistent with
existing authority. Not C.** Every decision this document makes reuses
an already-accepted mechanism verbatim: the existing `(EXECUTE,
DOCUMENT)` permission pair, the existing `EvidenceIntelligenceInvocationGate`
convention, the existing `ParkerRuntime.analyseEvidence` entry point,
the existing four-way `EvidenceAnalysisResult` taxonomy, and the
existing `EvidenceIntelligenceAcceptanceCoordinator` dispatch. No new
`PermissionAction`, `ResourceType`, public type, or interface is
introduced. The only genuinely new content is §14's numeric resource
bounds — a narrow, conservative, load-bearing security freeze
explicitly anticipated but left unresolved by the Scope Lock's own §15
and the Planning Review's own §3.8 finding, not a constitutional
question in its own right.

## Final Recommendation

**READY FOR OWNER REVIEW.**

This document resolves Implementation Plan §16 items 1-3 in full
(owner-control model: explicit, owner-triggered, via the existing
`analyseEvidence` entry point; composition-level coordinator: the owner
role itself, reusing that entry point, no new class authorised or
required; dedicated permission surface: not required, the existing
`(EXECUTE, DOCUMENT)` invocation-level gate already suffices) and items
4-5 to the extent required (rejected output can never be exposed
ungated, because every acceptance leg is already, independently,
permission-gated or self-gating). It freezes conservative,
load-bearing resource/security bounds the Scope Lock itself left open.
It discloses, but does not resolve, one remaining implementation gap
(Evidence Intelligence's own composition does not yet hold an
`OcrMechanism` dependency) — squarely future implementation work this
document authorises to begin, consistent with its own governance-only
character. Upon adoption, Document Ingestion's own Tier B
implementation, and a future OCR Mechanism Unit 12 Implementation Plan,
may each be separately proposed and reviewed on their own merits,
against the fixed boundaries this document now supplies.
