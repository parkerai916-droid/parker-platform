**Status: ACCEPTED — CANONICAL — FROZEN**

Owner: Steven Francis McTague

Accepted: 5 September 2026

The owner reviewed this amendment and accepted its substance as reported
(HFR-1A, Owner Decision A — ACCEPT): that Parker already has a complete,
frozen, production-proven Human Fidelity Review domain, and that this
document narrowly extends that existing governance to permit governed Owner
UI exposure for the Tier B OCR / External Transcription pipeline using the
existing domain, storage, audit, recording service, projector, Authorization
Purpose, and Permission Engine policy unchanged. No substantive content was
altered between draft and acceptance.

This remains a governance/design document only. No Kotlin is implemented,
proposed as a diff, or changed by this document. Neither `src/` nor `tests/`
is touched. No provider call, no derivative, no evidence mutation, no review
persistence. Acceptance freezes the decisions recorded below; it authorises
no implementation. Each proposed unit (§20) requires its own separate
authority before any code is written, exactly as R6T §1 and R6V §1 already
require for this same governance family.

**This is not a freestanding redefinition of human fidelity review.** Parker
already has a frozen governance document for that exact domain:
`docs/architecture/ORDINARY_INGESTION_11R6T_GOVERNED_HUMAN_FIDELITY_REVIEW_AND_CORRECTION_STATUS_SCOPE_LOCK.md`
("the R6T Scope Lock", **ACCEPTED — CANONICAL — FROZEN**, 3 September 2026),
together with its accepted implementation plan
(`ORDINARY_INGESTION_11R6V_GOVERNED_HUMAN_FIDELITY_REVIEW_AND_CORRECTION_STATUS_IMPLEMENTATION_PLAN.md`)
and closed Sequence A programme
(`ORDINARY_INGESTION_11R6V_A10_SEQUENCE_A_PROGRAMME_CLOSURE_AND_FINAL_ACCEPTANCE.md`,
**ACCEPTED — CANONICAL — CLOSED**). That domain — the four-layer model, the
`HumanFidelityReviewState` vocabulary, the discrepancy/pattern/cause/source-
resolution model, the two Authorization Purposes, immutability, supersession,
audit, fail-closed rules, correction boundary, and downstream trust
semantics — is real, implemented, and already production-proven for one real
evidence artifact under the "Ordinary Region Transcription" pipeline. The R6T
Scope Lock is **not edited by this document** — every one of its Frozen
Decisions (§31) remains in force exactly as written.

This document is a narrow, additive amendment addressing the one thing R6T
never claimed to do and never implemented: **exposing that already-governed
recording capability through the Owner LAN HTTP UI, and confirming it applies
without modification to the Tier B OCR / External Transcription pipeline**
(the pipeline behind `TierBOcrContentRetrievalCoordinator`, `GET
/owner/evidence/{id}/ocr-content/{generationId}`, and the UI-INGESTION-8
series' exact-evidence discovery/inspection workflow). This was written as
HFR-1 under that exact objective; the architecture inventory below found the
domain already exists, so HFR-1's real job narrowed to this amendment rather
than a new domain design.

# Human Fidelity Review — Owner UI Exposure Scope Lock Amendment

Programme: **Human Fidelity Review Programme, Unit HFR-1 (governance/design only).**

## 1. Status

**ACCEPTED — CANONICAL — FROZEN**, 5 September 2026 (HFR-1A). This document
creates no production capability, type, schema, codec, API, policy,
Authorization Purpose, audit event, or store — identical in force to R6T
§1's own self-description, which this amendment inherits rather than
restates per section. Acceptance authorises no implementation unit (HFR-2
onward, §20 below); each remains separately gated exactly as R6T's own
Frozen Decisions required for its own proposed units (R6T §28).

## 2. Purpose

Define the minimum safe governance for exposing Parker's existing, frozen,
production-proven Human Fidelity Review recording capability (R6T) through the
Owner LAN HTTP UI, and for constructing the exact-target binding
(`HumanFidelityReviewTarget`) for the Tier B OCR / External Transcription
pipeline the same way it is already constructed for the Ordinary Region
Transcription pipeline. It answers, for this narrow scope, the same question
R6T's own Purpose section already answers narrowly for the domain itself:
*"Has the human reviewer compared this exact derivative representation against
the exact source evidence and determined the fidelity relationship between the
representation and that source?"* This document does not re-derive that
question's answer; R6T already froze it. It asks only: *how does the owner,
sitting at the LAN HTTP UI this whole UI-INGESTION-8 series built, actually
invoke it?*

## 3. Existing Architecture / Reused Capabilities

Full architecture inventory performed against `src/interfaces/`,
`src/runtime/`, `src/composition/`, and `docs/architecture/` before drafting
this document, per the workflow HFR-1 required.

**Reused unchanged, in full:**

- `HumanFidelityReviewTarget`, `HumanFidelityReviewRecord`,
  `HumanFidelityReviewState`, `HumanFidelityReviewCoverage`,
  `FidelityDiscrepancyOccurrence`/`FidelityDiscrepancyLocation`,
  `FidelityDiscrepancyClassification`/`Severity`, `FidelityCauseAssessment`,
  `HumanSourceResolution`, `SystematicDiscrepancyPattern`,
  `HumanFidelityReviewSupersession`, `HumanFidelityReviewAdjudicationReference`
  (`src/interfaces/HumanFidelityReview.kt`) — the complete domain model, R6T
  §6–§18. Every constructor already enforces R6T's own invariants (closed
  vocabularies, exact bindings, canonical code-point spans, unique
  identities); nothing here needs a new field, a new state, or a relaxed
  invariant to serve this amendment's scope.
- `HumanFidelityReviewStorage`/`FileSystemHumanFidelityReviewStorage`,
  `HumanFidelityGovernanceAudit`/`FileSystemHumanFidelityGovernanceAudit`
  (`src/interfaces/HumanFidelityReviewStorage.kt`,
  `src/runtime/FileSystemHumanFidelityReviewStorage.kt`,
  `src/runtime/FileSystemHumanFidelityGovernanceAudit.kt`) — create-once,
  durable, exact-target-queryable storage and narrow append-only audit,
  R6T §12/§25. Both storage roots
  (`PARKER_HUMAN_FIDELITY_REVIEW_STORAGE_ROOT`,
  `PARKER_HUMAN_FIDELITY_GOVERNANCE_AUDIT_STORAGE_ROOT`) are already
  configured and mounted in the currently running production container
  (confirmed live during this inventory) — reused, not newly provisioned.
- `HumanFidelityReviewRecordingAuthorityScope`/
  `HumanFidelityReviewRecordingPermissionRequest`/
  `HumanFidelityReviewRecordingPermissionEvaluator`/
  `HumanFidelityReviewRecordingPermissionPolicy`
  (`src/interfaces/HumanFidelityReviewAuthorization.kt`,
  `src/runtime/HumanFidelityReviewRecordingPermissionPolicy.kt`) — the
  registered `document-ingestion.human-fidelity-review-recording`
  Authorization Purpose (R6T §24) and its exact-owner/exact-purpose/exact-
  target/Permission-Engine guard (R6T §11/§22). **Traced precisely: this
  policy requires the configured owner principal, the active Authorization
  Purpose, exact target equality, and Permission Engine approval — it does
  not reference `OwnerHighAuthorityVerification` or any presented secret.**
  This is the frozen answer to this amendment's own "Review Authority"
  question (§6, below); it is cited, not re-derived or second-guessed.
- `GovernedHumanFidelityReviewRecordingService`/
  `DefaultGovernedHumanFidelityReviewRecordingService`
  (`src/interfaces/HumanFidelityReviewRecordingService.kt`) — the minimum
  authorization-before-mutation recording service (R6T.4-MIN, accepted). It
  is already constructed at `ParkerRuntime` startup
  (`src/composition/ParkerRuntime.kt:1120`, field
  `humanFidelityReviewRecordingService`) with real storage/authorization —
  but, traced precisely, **has no live caller anywhere in the current
  composed production graph, for any pipeline.** No `ParkerRuntime` method
  exposes it; `Main.kt` has no CLI or HTTP entry point for it. The historical
  R6V-A8 canonical recording of the R6 fixture is a completed, closed,
  historical fact, not evidence of a currently reachable capability — this
  amendment's own gap (§7, below) is real and applies uniformly, not only to
  the Tier B pipeline.
- `EffectiveHumanFidelityReviewProjector`/
  `DefaultEffectiveHumanFidelityReviewProjector`,
  `EffectiveHumanFidelityReviewProjection`,
  `EffectiveHumanFidelityReviewSummary`, `HumanFidelityEligibilityUse`,
  `SourceConfirmedEligibility` (`src/interfaces/HumanFidelityReviewProjection.kt`)
  — deterministic effective-state/eligibility projection over the immutable
  review store (R6T.A8A, accepted). Already wired as a constructor dependency
  of `TierAContentRetrievalCoordinator`
  (`src/composition/ParkerRuntime.kt:1300-1303`) for the Ordinary Region
  pipeline; **not wired into `TierBOcrContentRetrievalCoordinator`**
  (confirmed — that class's constructor accepts only
  `DerivativeGenerationStorage`/`DerivativeContentStorage`, no projector).
- Human-corrected-representation mechanism
  (`DefaultGovernedHumanCorrectionService`, `HumanCorrectedRepresentationRetrievalService`,
  `HumanCorrectionPermissionPolicy`, the
  `document-ingestion.human-transcription-correction` Authorization Purpose)
  — R6T §13–§16, Sequence B groundwork. Confirmed generic: keyed by the same
  `HumanFidelityReviewTarget`, not coupled to the region-transcription
  pipeline. Untouched by this amendment; Sequence B (correction proposal/
  acceptance) remains separately governed and out of scope here exactly as
  R6T §17/§27 and OI11R6V-A10 §17 already state.
- `EvidenceArtifactId`, `DerivativeGenerationId`, `DerivativeGenerationRecord`,
  `DerivativeGenerationRecordCodec`, `DerivativeContentEntry`,
  `DerivativeContentCodec`, `FileSystemDerivativeGenerationStorage`,
  `FileSystemDerivativeContentStorage`, `OcrSha256Digest` — the same generic,
  producer-neutral identities and codecs the UI-INGESTION-8 series already
  uses for Tier B OCR / External Transcription discovery and retrieval,
  confirmed by direct trace to be the *same* codecs
  `TierAContentRetrievalCoordinator` already uses to compute
  `derivativeGenerationSha256`/`derivativeContentSha256` for its own
  `HumanFidelityReviewTarget` construction (§8, below).
- `OwnerUiAuthentication` (paired-device session), the existing authenticated
  Owner LAN HTTP boundary `OwnerEvidenceHttpServer`/`isAuthorised`, and the
  existing exact-evidence discovery/paired-retrieval routes
  (`GET /owner/evidence/{id}/ocr-derivative-generations`,
  `GET /owner/evidence/{id}/ocr-content/{generationId}`) this programme's
  prior units (UI-INGESTION-8A–8F) already deployed and the owner has already
  accepted. A recording action, when eventually implemented, originates from
  this same authenticated surface — never a new one.

**Traced and explicitly not reused:**

- `HumanVerificationRecord`/`HumanVerificationStorage`
  (`src/interfaces/HumanVerification.kt`) — an older, coarser,
  exact-generation-keyed record (outcome only: `REVIEW_PASSED`/
  `REVIEW_FAILED`/`PARTIALLY_VERIFIED`) already correctly identified by R6T
  §4/§21 as retaining its own historical meaning, mappable conservatively but
  never a substitute for the R6T model. This is the same field the
  UI-INGESTION-8 series' discovery panel already displays as
  `humanReviewState`/`UNREVIEWED` — it remains a read-only display fact and
  is not the write target of this programme.
- `DerivativeReviewRegistry` — R6T §4 already found this evidence-keyed,
  in-memory, single-current-state registry structurally incompatible with
  exact-generation, multi-review, immutable-history semantics. Not reused;
  not extended.
- `OwnerHighAuthorityVerification`/presented-secret verification (the
  mechanism gating external-transcription authorization and human-correction
  acceptance in other units this session) — traced and confirmed **not**
  part of R6T's own frozen recording-authority answer (§6, below). Not
  introduced here as a new requirement.

## 4. Trust Boundary

Unchanged from R6T §2 Governing Principle 4 and §19: a recorded human
fidelity review answers only *"a human reviewer determined the fidelity
relationship between this exact derivative representation and this exact
source evidence, as of this review act."* It says nothing about whether the
source's own content is true, factually correct, legally admissible,
relevant, or interpreted correctly; whether an allegation is proved; whether
a legal conclusion follows; whether a provider is generally reliable; or
whether any other derivative generation (reviewed or not) is faithful. This
boundary is R6T's own, restated here because it directly answers this
amendment's own required "Trust Boundary" section — it is not weakened,
broadened, or reworded by this amendment.

## 5. Exact Review Target

Unchanged: `HumanFidelityReviewTarget(evidenceArtifactId, sourceSha256,
preparationIdentity, derivativeGenerationId, derivativeGenerationSha256,
derivativeContentSha256)` (R6T §10/§11). Binding is structural, not
convention: `HumanFidelityReviewRecord`'s own constructor requires every
discrepancy's location to match the target exactly, and
`HumanFidelityReviewRecordingPermissionPolicy.resourceIdFor` derives its
target-scoped resource ID from all six fields. A review of
`4c8ed1e2-7524-467c-b4b3-32e8293c7854` cannot silently apply to
`6d8d9307-8281-4574-a050-f9fec1c916f1`: they are different
`derivativeGenerationId`s, and (being independently admitted generations)
almost certainly different `derivativeGenerationSha256`/
`derivativeContentSha256` as well — the target equality check in
`HumanFidelityReviewRecordingPermissionPolicy.evaluate` and the exact-target
storage query `listForExactTarget` both fail closed on any field mismatch.

**New, narrow finding this amendment adds:** for the Tier B OCR / External
Transcription pipeline specifically, `preparationIdentity` — the field
`TierAContentRetrievalCoordinator` populates from
`RegionTranscriptionDerivative.preparationIdentity` for the Ordinary Region
case — has a direct, already-governed analogue:
`OcrProcessingProvenance.representationSha256` (on
`OcrDerivativeExtractedResult.processingProvenance`), the SHA-256 of the
exact processing representation actually submitted to the mechanism/provider.
`sourceSha256` maps identically to
`OcrProcessingProvenance.sourceManifestSha256`. No new field, digest scheme,
or governed fact is required to construct a complete, valid
`HumanFidelityReviewTarget` for a Tier B OCR / External Transcription
generation — see §19 for the worked real-case check.

## 6. Review Authority

Unchanged, cited from R6T §11/§22/§24 and traced directly in
`HumanFidelityReviewRecordingPermissionPolicy` (§3, above): recording
requires (a) the configured owner `PrincipalId` exactly, (b) the
`document-ingestion.human-fidelity-review-recording` Authorization Purpose
active, (c) exact equality between the presented authority's target and the
proposed review's target, and (d) `PermissionEngine` approval
(`APPROVED`/`APPROVED_WITH_CONFIRMATION`). This is an authenticated-owner-
session-plus-Authorization-Purpose-plus-exact-target model — **not** an
`OwnerHighAuthorityVerification` presented-secret model. This was traced
precisely, not assumed: the existing frozen R6T.3 implementation does not
call `OwnerHighAuthorityVerification` anywhere in
`HumanFidelityReviewRecordingPermissionPolicy`, unlike the external-
transcription-authorization and human-correction-acceptance flows this
session's other units touched, which do. This amendment adopts that frozen
answer unchanged for the Owner UI surface; it does not add a new
high-authority gate, and does not weaken the existing one, because there is
no existing high-authority gate on this specific action to weaken. The
credential itself (there being none beyond the paired-device session already
gating the whole Owner LAN HTTP boundary) is therefore not a persistence
concern here the way an `OwnerVerificationCredential` secret is elsewhere;
no HTTP route implementation may persist a credential, session token, or
device identifier into a `HumanFidelityReviewRecord` or audit fact regardless.

## 7. Review Granularity

Unchanged, R6T §6/§7: **C — generation-level review state with page-specific
(and, where finer, character-scope-specific) findings.** `HumanFidelityReviewCoverage`
already distinguishes `FULL_GENERATION` (page scope only) from `PARTIAL`
(exact character scopes required); `HumanFidelityReviewState` is a
generation-level derived/effective projection, never a page-by-page status
vocabulary of its own. This already truthfully represents every case this
amendment's own required-granularity list names: all pages checked and
faithful (`FULL_GENERATION` coverage, `HUMAN_REVIEWED_PASS`), pages checked
with qualifications (coverage plus one or more `MINOR`/`NON_ERROR_OBSERVATION`
discrepancies, `HUMAN_REVIEWED_WITH_DISCREPANCY`), material discrepancy found
(one or more `MATERIAL` discrepancies, same state), and review incomplete
(`PARTIAL` coverage, or no valid review at all, projecting `UNREVIEWED` for
the uncovered scope — R6T §7's own explicit rule that "partial review is
coverage, not a fifth whole-generation status"). No new granularity concept
is proposed.

## 8. Review Outcomes

Unchanged, R6T §7: the closed vocabulary is exactly `UNREVIEWED`,
`HUMAN_REVIEWED_PASS`, `HUMAN_REVIEWED_WITH_DISCREPANCY`,
`HUMAN_REVIEW_CONFLICT`. This already satisfies the narrow four-way
distinction this amendment's own instructions require (source-faithful;
source-faithful with recorded qualifications; material discrepancy; review
incomplete) without any of the broad labels (`APPROVED`, `VALID`, `TRUE`,
`AUTHORITATIVE`, `VERIFIED_EVIDENCE`) those instructions correctly warn
against. `HumanVerificationRecord`'s own separate, coarser
`REVIEW_PASSED`/`REVIEW_FAILED`/`PARTIALLY_VERIFIED` vocabulary is untouched
and remains a distinct, older concept (§3, above) — this amendment does not
merge the two vocabularies.

## 9. Qualifications and Discrepancies

Unchanged, R6T §9/§16: `FidelityDiscrepancyOccurrence` with
`FidelityDiscrepancyClassification` (`TRANSCRIPTION_DIFFERENCE`,
`MISSING_SOURCE_TEXT`, `ADDED_OR_HALLUCINATED_TEXT`, `INAPPROPRIATE_CERTAINTY`,
`APPROPRIATE_UNCERTAINTY`, `OTHER_EXPLICITLY_CLASSIFIED`),
`FidelityDiscrepancySeverity` (`MINOR`/`MATERIAL`/`NON_ERROR_OBSERVATION`),
optional `FidelityCauseAssessment` (`ESTABLISHED`/`HYPOTHESISED`/`UNKNOWN`,
never inferred from the character difference itself), and optional
`HumanSourceResolution` (source-unresolved, or resolved with an asserted,
digest-verified exact value). This structure already distinguishes, exactly
as this amendment's own instructions require: source itself truncated/
illegible/uncertain and conservatively so reported
(`APPROPRIATE_UNCERTAINTY`/`NON_ERROR_OBSERVATION`); machine transcription
introduces an error (`TRANSCRIPTION_DIFFERENCE`/`INAPPROPRIATE_CERTAINTY`,
severity per Section 16's own reviewer judgment); machine transcription omits
source material (`MISSING_SOURCE_TEXT`); machine transcription adds text not
in the source (`ADDED_OR_HALLUCINATED_TEXT`); reading-order or region
mis-sequencing (already the documented cause of a real, separately governed,
unresolved structural finding this session's memory tracks — representable
as `TRANSCRIPTION_DIFFERENCE`/`OTHER_EXPLICITLY_CLASSIFIED` with an explicit
detail, not a new classification); review unable to determine fidelity for a
region (simply: no discrepancy occurrence recorded for that scope, and
`PARTIAL` coverage if the whole generation was not reviewed — R6T never
requires forcing an outcome). `descriptiveFidelity` is a required bounded
free-text field on the review record itself, but — per R6T §9's own text —
narrative "cannot substitute for classification, severity, or location"; a
`HumanFidelityReviewRecord`'s constructor already enforces that the governed
`reviewState` is derived from structured discrepancy presence/severity, never
from that free text alone. This already satisfies the "do not permit free
text alone to determine governed state" requirement structurally, not by
policy.

## 10. Immutable Review Record

PASS, unchanged, R6T §12/§21/Frozen Decision 11: reviews, discrepancies,
patterns, proposals, acceptances, and adjudications are create-once;
`HumanFidelityReviewStorage`/`HumanFidelityGovernanceAudit` expose no
update/delete method (`prepare`/`publishPrepared`/`retrieve`/
`listForExactTarget` only). Nothing in this domain — traced directly in
`HumanFidelityReviewRecord`'s own constructor — can rewrite recognised text,
page segments, provider provenance, model provenance, generation metadata, or
source evidence; the target's own digests (`derivativeGenerationSha256`,
`derivativeContentSha256`) are computed *from* the immutable derivative, never
written back into it. A future Owner UI recording action, per §15 below, must
call only the existing `GovernedHumanFidelityReviewRecordingService`; it must
never call `DerivativeGenerationStorage.prepare`/`DerivativeContentStorage.prepare`
or any correction-admission path.

## 11. Multiple Reviews / Supersession

Unchanged, R6T §17/§18/Frozen Decisions 19–20: append-only; a later review may
supersede exactly one earlier review only through an explicit, named
`HumanFidelityReviewSupersession` naming both immutable records and binding
the same exact target — never a silent overwrite, never timestamp/record-ID
precedence. Two live, non-superseded reviews that materially conflict on the
same target project `HUMAN_REVIEW_CONFLICT` (fail-closed, denies
source-confirmed eligibility) until an explicit
`HumanFidelityReviewAdjudicationReference` selects among them. "Current
review" is therefore never a mutable flag; it is always the deterministic
output of `EffectiveHumanFidelityReviewProjector.project(target, purpose)`
over the full immutable history, traceable back to the specific
`applicableReviewIds` that produced it (`EffectiveHumanFidelityReviewProjection`).
This amendment introduces no UI-convenience shortcut around that projection.

## 12. Audit Requirements

Unchanged, R6T §25, implemented as `HumanFidelityGovernanceAuditRecord`
(`REVIEW_PREPARED`/`REVIEW_PUBLISHED`/`REVIEW_DUPLICATE_CONFIRMED`), each
binding event ID (content-derived), event type, timestamp, actor principal,
review ID, full target, review-payload SHA-256, and outcome
(`SUCCEEDED`/`EXACT_DUPLICATE`). This already satisfies the minimum audit
facts this amendment's own instructions list (review record ID, exact
evidence ID, source hash, exact derivative generation ID, derivative/content
identity, reviewer principal, review outcome via the payload digest,
timestamp; page coverage and qualifications live in the reviewable payload
the digest binds to, not duplicated separately into the audit fact). It never
stores an owner high-authority credential, API key, or unrelated evidence
content — the audit record's own bounded `factualDetail` field is capped at
1024 characters and is not free-form content capture. No new audit port is
proposed; the existing `FileSystemHumanFidelityGovernanceAudit` is reused
unchanged, following the same narrow-append-only-port precedent
`DocumentIngestionAudit`/`EvidenceDeletionAudit` already established and R6T
§25 explicitly required continuing.

## 13. Fail-Closed Rules

Unchanged, R6T §22, reused directly: unknown review status/classification/
severity, unsupported schema/version, corrupt digest, missing/unresolved
reviewer, missing or mismatched source/generation/content/preparation
binding, invalid page/region/block/span, original-substring mismatch,
overlapping corrections, missing acceptance, cross-lineage supersession,
missing supersession target, cycle, multiple unadjudicated heads, unresolved
material conflict, or ambiguous eligibility all fail closed with a typed
disposition — never a fallback to a passing/verified/latest-wins/best-effort
result. This directly satisfies every fail-closed condition this amendment's
own instructions enumerate (non-existent source/generation, derivative not
belonging to the specified evidence, source/derivative identity mismatch,
reviewer authority failure, unknown outcome, inconsistent findings structure,
storage/audit admission failure): each already has a named, typed governed
disposition in `GovernedHumanFidelityReviewRecordingResult`
(`AuthorizationDenied`, `Failure` with `AUTHORITY_EVALUATION_FAILED`/
`STORAGE_OPERATION_FAILED`/`CANONICAL_READBACK_MISSING`/
`CANONICAL_READBACK_MISMATCH`) or in the storage/permission-policy exception
hierarchies. No new fail-closed rule is proposed; none is missing.

## 14. Atomicity

Unchanged, R6T §12 create-once/prepare-publish discipline, reflected exactly
in `GovernedHumanFidelityReviewRecordingResult`: a successful `Recorded`
result is returned only after prepare, publish, and canonical readback all
succeed (`CANONICAL_READBACK_MISSING`/`CANONICAL_READBACK_MISMATCH` are
distinct, named failure results specifically guarding against "the UI says
reviewed but durable admission failed"). No derived/index state is proposed
by this amendment; the effective-review projection (§11, above) is computed
fresh from the durable store on each read, never cached as a separate
mutable index that could drift from it. A future Owner UI recording route
must return success to the browser only on `GovernedHumanFidelityReviewRecordingResult.Recorded`
(or `AlreadyRecorded`, for an exact-duplicate resubmission) — never
speculatively before that result is known.

## 15. UI Contract

**This is the one genuinely new content in this amendment** — R6T is
deliberately UI-agnostic and does not specify one. Defined here, not
implemented:

- The review action originates from the exact inspected derivative
  generation the owner is already looking at — the same
  `buildEnhancedTranscriptionPanel()` surface UI-INGESTION-8E/8F deployed and
  the owner has already used to inspect `4c8ed1e2-7524-467c-b4b3-32e8293c7854`.
  It must never originate from a generic, evidence-level, or ambiguous
  control.
- Before submission, the owner must see: the exact `derivativeGenerationId`
  and `evidenceArtifactId` being reviewed (both already rendered by the
  existing inspection panel); the review outcome being selected, from the
  closed R6T vocabulary only; any qualifications/findings being recorded, in
  the same structured form they will be stored (classification, severity,
  location — never free text alone, per §9); and an explicit, un-missable
  statement that the review concerns transcription fidelity only, per the
  Trust Boundary (§4) — not the truth, admissibility, relevance, or legal
  significance of the source.
- The UI must make the exact target generation visually unambiguous at the
  moment of submission — e.g., displaying `derivativeGenerationId` and
  `evidenceArtifactId` directly on the submission control itself, not merely
  earlier on the page, so a reviewer working through multiple discovered
  generations (the real case: `4c8ed1e2-...` and `6d8d9307-...` for the same
  evidence, per UI-INGESTION-8's own discovery panel) cannot submit a review
  against the wrong one merely because it was the panel open a moment ago.
- The existing generic analysis-selection acknowledgement
  (`acknowledgesUnverifiedExternalTranscription`, an ephemeral,
  client-side-only checkbox gating the unrelated "Analyse" LLM-reasoning
  selection flow, traced in UI-INGESTION-8E) is explicitly **out of scope and
  must not be reused, relabelled, or presented as if it were this
  capability.** It writes nothing durable and carries no reviewer identity,
  target binding, or Authorization Purpose of any kind.
- The eventual HTTP route (not created by this document) must sit inside the
  existing authenticated `OwnerEvidenceHttpServer`/`isAuthorised` boundary,
  exactly like every other owner-facing route this session's units added; it
  introduces no new authentication mechanism.

## 16. Correction Boundary

Confirmed unchanged from R6T §13–§16/§27, Frozen Decisions 12–17: if the
owner's comparison finds a substantive transcription error, Human Fidelity
Review records the discrepancy as a structured fact — it does not, and
structurally cannot (§10, above), edit the derivative. Any corrected
representation is created only through the separately governed correction
capability (`HUMAN_CORRECTED_REGION_TRANSCRIPTION`-kind derivative, its own
Authorization Purpose, its own acceptance act distinct from the review act),
preserving the explicit lineage source evidence → machine derivative → human
review finding → corrected representation that R6T §13 already requires.
Review, correction, replacement, and promotion (to Memory/Knowledge/external
reasoning) remain four distinct acts requiring four distinct authorities;
this amendment does not conflate them and does not touch Sequence B (the
correction capability itself), which OI11R6V-A10 §17 confirms remains
unimplemented and separately governed.

## 17. Downstream Trust Semantics

Confirmed unchanged from R6T §19/§20/§23, Frozen Decision 21:

**Permitted inference:** "A human reviewer recorded that derivative X
faithfully represents source Y under review outcome Z" (with Z one of the
four closed states, and any recorded discrepancies disclosed alongside).

**Prohibited inference:** "The contents of source Y are true," or any of the
other eight exclusions this amendment's own Purpose section (§2) and R6T §2
Governing Principle enumerate.

Confirmed, and explicitly adopted as this amendment's own answer to the
"does a successful review change the displayed fidelity label" question:
**the strong preference is correct and is already how R6T's own accepted
retrieval-integration unit (R6V-A8B) behaves.** The machine derivative
remains intrinsically "Machine transcription — unverified" — that string is
a property of the derivative's own producer provenance
(`providerProvenance != null`, UI-INGESTION-8's own existing
`externalTranscription` signal) and is never rewritten by a review. The human
review is displayed as a separate, additive provenance/trust fact
(`humanFidelityStatus`, mirroring R6V-A8B's own additive field on the Tier A
retrieval result) alongside it, never replacing or downgrading the intrinsic
label. A future HTTP/UI unit exposing review state for the Tier B pipeline
must follow this same additive pattern, not fold review state into the
existing `externalTranscription`/`fidelity` fields.

## 18. Explicit Prohibitions

This amendment authorises no implementation. It does not: create an HTTP
endpoint; change the Owner UI; create, mutate, or persist a
`HumanVerificationRecord` or a `HumanFidelityReviewRecord`; invoke a
provider; generate another derivative; alter existing evidence; register a
new Authorization Purpose (both required purposes are already registered per
R6T §24/R6V-A3); introduce a new authority/verification mechanism beyond
§6's cited, frozen answer; reuse or relabel the analysis-selection
acknowledgement as fidelity review; edit `ORDINARY_INGESTION_11R6T_...` or any
other frozen document; or implement any part of Sequence B (correction). It
also does not record a review for the real acceptance generation
(`4c8ed1e2-7524-467c-b4b3-32e8293c7854`) — §19 is design validation only.

## 19. Real Acceptance Case Validation

Evidence `evidence-44d61bfe-e46f-4d39-85e7-9f68f122369d`, derivative
`4c8ed1e2-7524-467c-b4b3-32e8293c7854`, owner's actual comparison result (not
persisted, per explicit instruction):

| Owner observation | Representable under the existing R6T model without overclaiming? |
|---|---|
| Page 1 text fidelity/reading order/completeness: PASS | Yes — full-page coverage, zero discrepancy occurrences for page 1. |
| Page 2 text fidelity/reading order/completeness: PASS | Yes — same, for page 2. Together: `HumanFidelityReviewCoverage(FULL_GENERATION, [1,2])`, zero occurrences → `HUMAN_REVIEWED_PASS`. |
| No substantive hallucinated text found | Yes — the *absence* of an `ADDED_OR_HALLUCINATED_TEXT` occurrence is exactly what "no discrepancy" already means; nothing is asserted beyond that absence. |
| Truncated Gmail footer URLs conservatively preserved as truncated/uncertain, not reconstructed | Yes, and precisely: this is exactly `FidelityDiscrepancyClassification.APPROPRIATE_UNCERTAINTY` / `FidelityDiscrepancySeverity.NON_ERROR_OBSERVATION` (R6T §9's own paired requirement) — a *positive*, recordable observation that the mechanism behaved correctly, not a discrepancy against the owner. Recording it (if a future unit chooses to) would not change the `HUMAN_REVIEWED_PASS` outcome, since `NON_ERROR_OBSERVATION` occurrences do not trigger `HUMAN_REVIEWED_WITH_DISCREPANCY` (R6T §9/§16). |

Target construction (§5, above) is concretely satisfiable from already-durable
facts alone, with no new provider call and no new digest scheme:
`evidenceArtifactId` = `evidence-44d61bfe-...` (known); `sourceSha256` =
`OcrProcessingProvenance.sourceManifestSha256` (already persisted on the
admitted generation's content); `preparationIdentity` =
`OcrProcessingProvenance.representationSha256` (ditto);
`derivativeGenerationId` = `4c8ed1e2-...` (known); `derivativeGenerationSha256`
= `sha256(DerivativeGenerationRecordCodec.encode(record))`, computable today
by retrieving the record via the existing
`TierBOcrContentRetrievalCoordinator`/discovery path already deployed;
`derivativeContentSha256` = `sha256(DerivativeContentCodec.encode(entry))`,
same retrieval, same generic codec `TierAContentRetrievalCoordinator` already
uses for the Ordinary Region case. **No field is missing, invented, or
requires new governance to populate.**

The model can therefore truthfully record exactly what the owner
established — a full-coverage pass with one correctly-conservative, non-error
uncertainty observation — and nothing broader. It cannot be made to assert
that the source document's contents are true, that the complaint is
meritorious, or that any other generation (including the older
`6d8d9307-8281-4574-a050-f9fec1c916f1`) is faithful. **Real case
representable: PASS.**

## 20. Implementation Units

Proposed, adjusted to the architecture actually found — narrower than a
from-scratch build, since the domain/storage/authorization/service layers
already exist and are production-proven. None are implemented by this
document.

- **HFR-2 — Tier B exact-target construction.** Add a `HumanFidelityReviewTarget`
  construction path for Tier B OCR / External Transcription retrieval,
  mirroring `TierAContentRetrievalCoordinator`'s existing pattern exactly
  (§5/§19). Wire an `EffectiveHumanFidelityReviewProjector` into
  `TierBOcrContentRetrievalCoordinator` (or an equivalent sibling
  coordinator), matching the existing Tier A constructor shape. No new
  domain type. No HTTP route. No UI change.
- **HFR-3 — Read-only effective-review presentation.** Extend the existing
  Owner UI inspection panel (`buildEnhancedTranscriptionPanel`) and/or
  discovery panel to display the *effective* human fidelity review
  projection (state, coverage, discrepancy/pattern counts) for a Tier B
  generation, following R6V-A8B's own additive-field precedent (§17). Purely
  additive presentation of already-computed data from HFR-2; still no write
  path.
- **HFR-4 — Owner UI recording route.** Implement one new, narrow,
  authenticated HTTP route that accepts a structured review submission
  (outcome, coverage, discrepancy occurrences per §9) for one exact,
  already-known `(evidenceArtifactId, derivativeGenerationId)` pair, resolves
  the exact target (reusing HFR-2's construction), and calls the existing,
  unmodified `GovernedHumanFidelityReviewRecordingService`. No new
  authorization mechanism (§6); no new storage; no new service.
- **HFR-5 — Owner UI recording workflow.** Implement the §15 UI contract:
  the exact-generation-originated review control, the required
  pre-submission disclosure, and the unambiguous target display — reusing
  the existing inspection panel and authenticated session, never the
  analysis-selection acknowledgement.
- **HFR-6 — Build, deploy, and live owner acceptance.** Following this
  session's own established exact-artifact build/deploy/verify/rollback
  discipline (UI-INGESTION-8C–8F), with its own explicit provider-safety and
  zero-mutation verification, culminating in the owner recording a real
  review for `4c8ed1e2-7524-467c-b4b3-32e8293c7854` for the first time —
  under a separate, explicit owner decision, not this amendment's own
  acceptance.

Each unit requires its own scoped authority, its own focused tests, and its
own completion report, matching this repository's established discipline; no
unit may collapse design, implementation, build, and deployment gates.

## 21. Acceptance Criteria

Owner acceptance of this amendment freezes only: (a) that R6T's existing
domain, storage, authorization, and service layers are reused entirely
unchanged for the Owner UI exposure; (b) that Tier B OCR / External
Transcription target construction (§5/§19) is architecturally sound and
requires no new field or digest scheme; (c) that recording authority remains
the existing owner-principal-plus-Authorization-Purpose-plus-Permission-Engine
model, not a new high-authority gate (§6); (d) the §15 UI contract; and (e)
the §16/§17 correction-boundary and downstream-trust-semantics restatements.
It does not authorise HFR-2 through HFR-6; each remains separately gated. A
future implementation is acceptable only when it additionally proves, for the
Tier B pipeline specifically: exact target construction matches
`TierAContentRetrievalCoordinator`'s own established shape; the recording
route is reachable only from the exact inspected generation; zero provider
calls occur at any point in discovery, inspection, or recording; the real
`4c8ed1e2-...`/`6d8d9307-...` pair remains independently, unambiguously
selectable; and every R6T Frozen Decision (§31 of that document) remains
true, unweakened, and unre-litigated.
