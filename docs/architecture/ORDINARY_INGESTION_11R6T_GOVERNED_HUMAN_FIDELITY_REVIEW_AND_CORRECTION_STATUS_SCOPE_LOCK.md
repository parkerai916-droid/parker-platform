# OI11R6T — Governed Human Fidelity Review and Correction Status Scope Lock

## 1. Status

**DRAFT — PENDING OWNER ACCEPTANCE**

This document is an architectural and governance scope lock only. It is not accepted, canonical, frozen, or implementation-authorising. It creates no production capability, type, schema, codec, API, policy, Authorization Purpose, audit event, or store.

## 2. Purpose

Define the minimum safe governance for recording attributable human fidelity review of an exact admitted transcription, representing discrepancies, optionally creating a separately governed human-corrected representation, and deciding what text is eligible for a stated downstream purpose. The design is grounded in Parker's existing contracts and the first real-document R6 fixture. It does not correct that fixture.

## 3. Governing Principles

1. **Human correction must never rewrite history.** Source evidence, provider transcription, human fidelity review, and any human-corrected representation are distinct provenance-bearing facts.
2. Source custody remains authoritative for source bytes. Review does not modify source evidence or preparation.
3. Provider transcription records what the provider returned. It remains immutable and retrievable after any review or correction.
4. Human review records what an attributable reviewer determined about the relationship between an exact source and an exact transcription. It is not transcription content.
5. A failed fidelity review does not retroactively change a successful provider execution into a failed execution.
6. A corrected representation, if accepted, is new content with explicit lineage. It never replaces, edits, or hides the provider transcription.
7. Known material discrepancy, unresolved conflict, unknown state, malformed state, or missing required authority fails closed for source-confirmed use.
8. Review, correction, acceptance, retrieval, and downstream use are separate acts. Authority for one does not imply authority for another.
9. **Materiality does not imply low transcription quality.** Materiality describes the consequence or significance of one difference, not its frequency, cause, document-wide accuracy, producer competence, or overall fidelity. A one-character difference may be material because it changes an identity while the remainder is highly faithful.

## 4. Existing Architecture Investigation

### Reusable concepts

- `EvidenceArtifactId`, authoritative source manifest SHA-256/media type/size, and corrected-preparation page/region identities already bind source custody and deterministic page order.
- `DerivativeGenerationId`, `DerivativeGenerationRecord`, `DerivativeParentReference`, `DerivativeContentIdentity`, generation/content write-once stores, completeness and operational outcome already provide immutable derivative identity, lineage, content integrity, atomic admission and canonical retrieval.
- `OrdinaryRegionTranscriptionDerivative` already carries the exact evidence, preparation, request, authorization, execution, attempt, provider state, provider/profile/model, capability/digest, Authorization Purpose, page/region/block order and content provenance required to identify the provider transcription.
- `PrincipalId` and the configured owner-principal pattern are Parker's existing accountable identity primitives. No second reviewer identity system is needed.
- `HumanVerificationRecord` is already an immutable, exact-evidence/exact-generation, reviewer-attributed record with reviewed page scope, optional bounded character scopes, review timestamp, outcome, and review-artifact SHA-256. `HumanVerificationStorage` is write-once, durable, exact-generation queryable, rejects duplicate/unsafe/corrupt/unsupported records, and gives no timestamp precedence.
- `HumanVerificationOutcome` and `AnalysisHumanReviewState` already distinguish unreviewed, passed, failed and partial review for OCR-derived material. Existing owner presentation displays external transcription as unverified and exposes review state separately.
- `DerivativeReviewRegistry` supplies an older flat `PENDING_REVIEW/APPROVED/REJECTED/NEEDS_CORRECTION` lifecycle and an append-only transition precedent. Its current key is evidence-level rather than exact-generation-level, its implementation is in-memory, and its terminal states cannot model multiple reviews or corrections.
- `DocumentAnalysisCoordinator` already projects exact-generation review assurance and requires explicit acknowledgement before using unreviewed/failed/out-of-scope external transcription. This is a useful unverified-use precedent, not source-confirmation authority.
- `Relationship.SUPERSEDES` establishes the platform-wide semantic precedent that supersession is an explicit relationship and does not mutate the superseded record. It is not itself a suitable review store.
- Authorization Purpose is an existing registered, immutable, retire-without-deletion vocabulary and `ExecutionRequest` dimension. Existing purposes do not identify human fidelity review or correction acceptance.
- `DocumentIngestionAudit` and `EvidenceDeletionAudit` establish narrow, append-only, purpose-specific audit ports whose callers sequence governance; the general `AuditService` still has no implementation and must not be introduced incidentally.

### Gaps

No current concept, alone or in combination, records structured discrepancy classification and exact target, severity independently from quality, established/hypothesised/unknown cause, repeated-pattern association, human source resolution, an asserted replacement value, review/correction supersession, conflict/adjudication, correction acceptance, or an effective source-confirmed representation. Existing human-verification records do not identify individual discrepancies or accepted corrections. The flat derivative review registry must not be stretched: its evidence-level key, in-memory durability, single-current-state projection and terminal transition graph are incompatible with the required exact-generation, multi-review, immutable-history semantics. The current local R6S owner-review record is integrity-preserved review evidence, but it is not yet a canonical governed production review record and must not be treated as one before a separately authorised import/recording operation.

## 5. R6 Fixture

The immutable fixture is evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`, source SHA-256 `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`, preparation `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f`, provider generation `region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6`, generation SHA-256 `9fb18b02db5ac55e5d446cd48ebc619de929c4596f94d2a11fba1a07da71af14`, and content SHA-256 `18a6ed08a4729350027d3140dc0f07dd49d32c04aa45f9e3e9558df5d007c4eb`.

Provider execution succeeded and the provider transcription remains immutable. The formal R6S owner fidelity verdict is `FAIL`: pages 1 and 5 each contain the material identity-bearing `Michael Gary Kellee` where the source reads `Michael Gary Kellec`; pages 2, 3 and 4 passed. These are two independently location-bound material discrepancy occurrences associated with one observed systematic pattern, `Kellec` interpreted/transcribed as `Kellee`. The owner human-resolved the source value as `Michael Gary Kellec` at both locations. The technical cause is `UNKNOWN`. Visual-character, glyph, font/rendering, rasterisation, or provider-interpretation ambiguity are possible mechanisms consistent with the observation, but none is established and none may be asserted as fact.

The document-wide descriptive fidelity is “high fidelity overall with one systematic material proper-name discrepancy pattern.” That description does not change or contradict the immutable formal `FAIL` verdict: discrepancy count (2), pattern count (1), severity (material at each location), and overall fidelity are separate concepts. No other substantive printed-text discrepancy, missing material text, or added/hallucinated substantive text was identified; handwriting/signature uncertainty handling was appropriate. The completed worksheet, owner record and package checksum hashes are respectively `8e7928c671cd36c7a4517dc5d9429706c46efb65c565e948684d6c3e7c8773a4`, `2d47f50e0f2915bd0e18e914eac4bd5abc879cf5419969d482b2b7f6ff6b1293`, and `7b4bd346b22976b75976970ff189eb59403ecf633577820941bf7c72eeea99e5`.

## 6. Canonical Semantic Model

The minimum future domain has four layers:

| Layer | Canonical fact | Mutability | Authority conveyed |
|---|---|---|---|
| A | source evidence and manifest | immutable | source-byte authority only |
| B | exact provider transcription generation/content | immutable | what provider returned; no human verification |
| C | exact-generation human fidelity review plus structured discrepancies | immutable/additive | attributable assessment only |
| D | separately admitted human-corrected representation derived from B under accepted corrections | immutable/additive | source-confirmed text only after its own acceptance gates |

Review state, correction state and downstream eligibility are derived projections over validated records. They are never mutable flags added to Layers A or B.

## 7. Human Review State

The canonical semantic vocabulary is exactly:

- `UNREVIEWED`: no valid, effective full-scope human review exists for the exact source hash and generation/content identity.
- `HUMAN_REVIEWED_PASS`: a valid, effective full-scope review found no discrepancy that changes source content; harmless layout/whitespace differences may be recorded but do not become transcription corrections.
- `HUMAN_REVIEWED_WITH_DISCREPANCY`: a valid, effective review records one or more minor or material discrepancies.
- `HUMAN_REVIEW_CONFLICT`: two or more non-superseded valid reviews materially conflict and no authorised adjudication resolves them.

Partial review is coverage, not a fifth whole-generation status: uncovered scope remains `UNREVIEWED`, and a whole-generation projection cannot be `HUMAN_REVIEWED_PASS`. Existing `REVIEW_PASSED`, `REVIEW_FAILED`, and `PARTIALLY_VERIFIED` records remain readable under their historical meanings; future projection maps them conservatively and never upgrades absent structured facts.

## 8. Review Verdict vs Execution Verdict

Execution status answers whether the governed provider operation completed. Fidelity status answers whether human review confirmed the exact transcription against the exact source. They are independent dimensions. `execution=SUCCESS` plus `humanReview=HUMAN_REVIEWED_WITH_DISCREPANCY` is valid and is the R6 fixture state. Review never rewrites execution/attempt/provider-state history.

## 9. Discrepancy Model

Each discrepancy is an immutable structured, actor-neutral fidelity fact, not narrative alone. It has an opaque identity; exact review identity; classification; severity; exact source/representation target; observed representation value; asserted source value when the reviewer can support one; human-resolution state; optional cause assessment; optional systematic-pattern identity; optional bounded sensitive note; and reviewer provenance inherited from the review. The model applies unchanged whether the representation was produced by OpenAI, another external model, local OCR, deterministic extraction, a human transcription process, or another authorised mechanism. Producer-specific provenance remains separate and exact.

Closed classifications are: `TRANSCRIPTION_DIFFERENCE`, `MISSING_SOURCE_TEXT`, `ADDED_OR_HALLUCINATED_TEXT`, `INAPPROPRIATE_CERTAINTY`, `APPROPRIATE_UNCERTAINTY`, and `OTHER_EXPLICITLY_CLASSIFIED`. Severity is `MINOR`, `MATERIAL`, or `NON_ERROR_OBSERVATION`; `APPROPRIATE_UNCERTAINTY` is a non-error observation. `OTHER_EXPLICITLY_CLASSIFIED` requires a non-blank bounded classification detail and cannot default to minor. Narrative may explain a structured fact but cannot substitute for classification, severity, or location.

Cause/mechanism is optional and has three distinct semantic states: `ESTABLISHED`, `HYPOTHESISED`, and `UNKNOWN`. An established or hypothesised cause requires a bounded explicit mechanism assertion; unknown carries no fabricated mechanism. A hypothesised mechanism is never presented as established. No component may infer cause from character difference, repetition, provider identity, or severity. Cause is not required to detect, human-resolve, correct, accept, or present a discrepancy.

Human source resolution is independent of cause. A discrepancy is either detected but source-unresolved, or resolved against an exact reviewed source location with an asserted source value. Parker may know what the source says without knowing why a representation differed. The R6 occurrences are human-resolved to `Kellec` while their technical cause remains unknown.

An optional immutable systematic-pattern record may associate two or more independently identified discrepancies that exhibit the same observed relationship. It binds its own opaque identity, exact member discrepancy IDs, bounded observed-pattern description, occurrence count, and reviewer/review provenance. It does not establish technical cause, create corrections, weaken each member's exact location binding, or authorise replacement. Pattern membership is descriptive; global replacement remains prohibited. R6 has one observed `Kellec` → `Kellee` pattern with two members, on pages 1 and 5.

Formal page/document verdict, descriptive fidelity assessment, discrepancy count, pattern count, and discrepancy severity are independent recorded/projection concepts. Severity must never be converted into an accuracy rate or document-wide quality conclusion. A presentation may truthfully show both `formal verdict: FAIL` and `descriptive fidelity: high overall fidelity with one systematic material discrepancy pattern`.

## 10. Source Location Binding

Every correctable discrepancy must bind all of:

- evidence artifact ID and source SHA-256;
- provider derivative generation ID and content digest;
- one-based source page ordinal;
- exact preparation page/region identity when the reviewed preparation supplied the provider input;
- exact derivative region identity and zero-based transcription block index;
- a half-open range measured in Unicode code points in the exact unnormalised transcription block;
- exact original provider substring and its SHA-256.

Source geometry is optional supporting provenance when canonically available; it is not fabricated. A source assertion also records the reviewed source-page representation identity/hash. A correction target must resolve uniquely and reproduce the original substring. Global replacement, fuzzy matching, normalization-dependent matching and implicit all-occurrence replacement are prohibited. The page-1 and page-5 `Kellee` occurrences are two discrepancy/correction facts even though their values match or share one pattern identity.

## 11. Reviewer Identity and Provenance

Reuse `PrincipalId` for the authenticated reviewer. A review requires an opaque review record ID, reviewer `PrincipalId`, review timestamp, evidence ID/source hash, preparation identity when applicable, generation ID/generation digest/content digest, reviewed page coverage, optional character coverage, outcome, structured discrepancy IDs and review-artifact SHA-256. Identity resolution and permission evaluation occur before recording. A display name may accompany presentation but never replaces the principal ID. The R6S package/worksheet/owner-record digests are provenance inputs, not authority by themselves.

## 12. Immutability

Provider transcription, completed review, discrepancy, correction proposal, correction acceptance, adjudication and corrected representation are create-once. No update/delete method is authorised. Later determinations create new records linked by explicit supersession or adjudication. All earlier records remain canonically retrievable. Storage uses canonical versioned encoding, digest verification and prepare/publish semantics; unknown versions and corrupt bytes fail closed.

## 13. Correction Representation

Decision: reject mutation (option A) and annotation-only as the final consumable form (option B). Adopt **a separate corrected derivative/representation (option C)**, supported by immutable structured correction annotations.

An accepted correction set may produce one new derivative generation of a distinct truthful kind, conceptually `HUMAN_CORRECTED_REGION_TRANSCRIPTION`, whose parent is the exact provider generation. Parker's correction coordinator is the mechanical producer; the human reviewer/proposer and accepting authority are recorded separately. Its canonical content is a full deterministic region-transcription representation produced by applying only accepted, location-bound corrections to the parent, copying all unaffected blocks exactly. It carries parent generation/content digest, review/discrepancy/correction/acceptance IDs and digests, reviewer/acceptor principals, source/preparation bindings and correction schema version. It is never labelled provider output.

An exact material discrepancy that is human-resolved against source does not permanently taint faithful unaffected content. Once each exact correction is separately governed and accepted, the corrected representation may become source-confirmed under Sections 15, 16 and 19. Neither a complete retranscription, provider retry, nor whole-document rejection is inherently required merely because the original representation contains a local material discrepancy. Technical-cause resolution is not a precondition.

## 14. Correction Granularity

Correction is text-span granular within one exact page/region/block. A correction set may contain multiple non-overlapping span corrections across a document. Page and region identities are mandatory context; whole-document replacement is prohibited. Overlapping corrections, a target spanning blocks, mismatched original text, or an attempt to alter an unreviewed location fail closed. Materialising the corrected representation copies faithful pages/blocks unchanged and applies only the exact accepted spans.

## 15. Correction Acceptance

A reviewer may assert a discrepancy and propose an exact corrected value. That proposal does not become effective merely because it exists. A separately permission- and purpose-authorised owner acceptance must bind the exact proposal/review/source/generation/content identities and the complete correction set. The same principal may review and later accept only through two distinct recorded acts; no implicit self-acceptance occurs. Admission of the corrected representation follows acceptance and deterministic reconstruction, then create-once generation/content publication. A correction against a different source hash or generation is rejected.

## 16. Minor vs Material Consequences

- `MINOR` means the reviewer explicitly determines the difference does not change an identity, fact, or meaning. It remains visible. A full-scope review containing only minor discrepancies may be source-confirmed **only if** the review explicitly passes substantive fidelity and an authorised acceptance records that those exact minor discrepancies do not require correction for the stated source-confirmed purpose.
- `MATERIAL` means the discrepancy may change identity, fact, meaning, completeness, or warranted certainty. Parker does not autonomously decide legal significance. Any unresolved material discrepancy blocks the provider transcription from source-confirmed status and blocks any corrected representation until the exact correction set is accepted.
- Severity never changes provider execution status or deletes presentation access to raw provider output.

## 17. Supersession

A later review may supersede one earlier review only through an explicit authorised relationship that names both immutable records and a reason. A later correction/acceptance may similarly supersede one earlier record of the same kind and exact source/generation lineage. Supersession is a directed acyclic graph with at most one immediate predecessor per new record. All history remains retrievable. Effective state is selected only when there is one unique valid non-superseded head, or one explicit adjudication selects among heads. Timestamp and record-ID ordering confer no precedence. Missing targets, cross-source/cross-generation edges, self-links, cycles and ambiguous heads fail closed.

## 18. Conflicting Reviews

Conflicting reviews are representable and retained. Two live reviews conflict when their outcomes, severity, asserted source value, or correction for overlapping scope cannot both be true. No latest-wins rule exists. Unresolved material conflict yields `HUMAN_REVIEW_CONFLICT`, blocks source-confirmed eligibility and corrected-representation admission, and causes retrieval of effective state to return an explicit conflict plus all safely disclosable record identities. An owner-authorised adjudication must bind all conflicting records and select or supersede them with reasons; it never edits them.

## 19. Downstream Eligibility

Eligibility is purpose-specific and returns the exact representation identity plus assurance state:

- Raw provider transcription is always forensically retrievable. When unreviewed, partially reviewed, failed, materially discrepant or conflicted, it may be presented as machine transcription and may enter an explicitly authorised unverified-text workflow only with exact-generation acknowledgement and review/discrepancy provenance. It is never source-confirmed.
- A full-scope `HUMAN_REVIEWED_PASS` provider transcription may be eligible as source-confirmed text for an authorised local downstream purpose.
- A provider transcription with only explicitly accepted minor discrepancies may be eligible under Section 16's acceptance rule, with discrepancies disclosed.
- A provider transcription with unresolved material discrepancy is not source-confirmed. Presentation remains permitted with prominent status.
- A separately admitted, accepted corrected representation may be source-confirmed for an authorised purpose without a new provider transcription; correction provenance and its own assurance state must travel with it.
- `HUMAN_REVIEW_CONFLICT`, unknown/malformed review state, missing correction acceptance or ambiguous effective representation is ineligible.

The immutable provider transcription remains `HUMAN_REVIEWED_WITH_DISCREPANCY` after corrected-representation admission; correction never upgrades or relabels it. Current explicit acknowledgement for unverified external transcription is preserved as a limited analysis gate; it does not become source confirmation. No representation becomes eligible for Memory/Knowledge promotion or external reasoning merely through this scope lock.

## 20. Retrieval and Presentation

Canonical retrieval must support distinct views without conflation:

1. provider view: exact immutable provider transcription and provider provenance;
2. review view: coverage, state, formal verdict, descriptive fidelity, review identities, reviewer attribution, structured discrepancies, cause confidence, human source-resolution state and systematic-pattern associations;
3. correction view: proposals, acceptances, corrected generation/content and supersession/conflict state;
4. eligibility view: representation selected for a named authorised purpose, or an explicit blocked reason.

Default generation retrieval never silently substitutes corrected content for a provider generation. Effective-view retrieval must name the returned generation/representation, its kind, parent, review/correction provenance and assurance. Presentation must show “what provider said”, “what human review found”, “what correction was accepted”, and “what is eligible”, separately. Sensitive review notes/source excerpts follow existing disclosure boundaries.

## 21. Historical Compatibility

Existing derivative generation/content codecs and bytes remain unchanged. A historical derivative with no valid exact-generation review record projects `UNREVIEWED`, never pass, verified, failed or corrected. Existing `DerivativeReviewRegistry` and `HumanVerificationRecord` data retain their historical meanings. Legacy `APPROVED` or `REVIEW_PASSED` is a human-verification signal only; absent exact scope/source/content binding required by this model, it cannot silently establish whole-generation source-confirmed eligibility. Compatibility readers may map legacy facts conservatively but never rewrite them.

## 22. Fail-Closed Rules

Fail closed for unknown review status/classification/severity, unsupported schema/version, corrupt digest, missing reviewer, unresolved principal, missing or mismatched source/generation/content/preparation binding, invalid page/region/block/span, original-substring mismatch, overlapping corrections, missing acceptance, cross-lineage supersession, missing supersession target, cycle, multiple unadjudicated heads, unresolved material conflict, conflicting correction values, or ambiguous eligibility. Failure returns a typed governed disposition; it never falls back to `VERIFIED`, `PASS`, latest timestamp, fuzzy replacement, raw provider text as source-confirmed, or a best-effort corrected view.

## 23. External Reasoning Boundary

External reasoning remains unauthorised. Any future reasoning handoff must receive the exact representation identity and one of: unreviewed provider transcription, human-reviewed pass, reviewed with discrepancy, accepted human-corrected representation, or unresolved conflict. It must also receive review/correction provenance and must not receive corrected text stripped of that provenance. Permission to retrieve or reason remains separate from fidelity state.

## 24. Authorization Purpose Impact

A gap exists. Current registered purposes cover external transcription, evidence-intelligence input resolution, knowledge candidate evaluation and reasoning-context retrieval; none truthfully identifies human-review recording or correction acceptance.

Future mutating implementation therefore requires two new, separately registered and policy-governed purposes through the existing Authorization Purpose mechanism:

- `document-ingestion.human-fidelity-review-recording` for creating review/discrepancy records and explicit review supersession;
- `document-ingestion.human-transcription-correction` for accepting/adjudicating correction sets and admitting/superseding corrected representations.

Correction proposal may be carried within the first purpose; it has no effective authority. Conflict adjudication uses the second purpose because it changes effective source-confirmed eligibility. Read-only retrieval uses existing owner retrieval authority and does not inherit mutation authority. Exact naming remains subject to the existing vocabulary registration rules, but the two semantic purposes and their non-interchangeability are frozen here. Their registration/policy implementation is a dedicated future unit, not Gap #54 and not this unit.

## 25. Audit Requirements

Future audit must record structured facts for review creation, discrepancy creation, cause assertion/change by superseding record, human source resolution, pattern association, correction proposal, correction acceptance, corrected-generation admission, review/correction supersession, conflict adjudication and effective-representation selection. Each record minimally binds operation/correlation identity, stage/outcome, requesting/reviewer/accepting principal as applicable, timestamp, Authorization Purpose, source/generation/content identities and affected review/discrepancy/pattern/correction/representation IDs. Audit records contain bounded identifiers/classifications/digests, not transcription text, source excerpts, credentials or generated narrative. Follow the narrow append-only ingestion-audit precedent; do not extend deletion audit or incidentally implement general `AuditService`. A success disposition is not returned until required durable audit ordering completes.

## 26. R6 Fixture Future State

After minimum capability implementation and a separately authorised recording operation, the Deed must project:

- source: `Michael Gary Kellec` at the two reviewed source locations;
- provider transcription: `Michael Gary Kellee`, immutable;
- provider execution: `SUCCESS`;
- formal owner fidelity verdict: `FAIL`, immutable;
- descriptive overall fidelity: high fidelity with one systematic material proper-name discrepancy pattern;
- effective human review: `HUMAN_REVIEWED_WITH_DISCREPANCY`, with two `MATERIAL` identity-bearing discrepancies;
- systematic observed pattern: `Kellec` → `Kellee`;
- pattern occurrence count: 2, independently bound to page 1 and page 5;
- materiality: `MATERIAL` because the value is an identity-bearing proper name, not because document-wide fidelity is low;
- technical cause: `UNKNOWN`;
- possible mechanism: visual-character/glyph/font/rendering/rasterisation/provider-interpretation ambiguity, explicitly hypothesised only and not established;
- human source resolution: `Kellec` confirmed at both exact locations independently of technical-cause resolution;
- page 1 correction proposal: exact local `Kellee` → `Kellec`;
- page 5 correction proposal: separate exact local `Kellee` → `Kellec`;
- original provider generation: immutable and historically retrievable;
- R6S review: immutable and historically retrievable;
- corrected/source-confirmed representation: absent until a separate exact correction acceptance and admission;
- source-confirmed downstream eligibility: blocked until that governed resolution.

This fixture is the required acceptance fixture. Implementation, offline verification and later acceptance use the preserved source, provider derivative and human-review artifacts. No new provider call, retranscription, evidence-specific provider authorization, execution or attempt is required merely because of this discrepancy. Historical budget remains one authorised, one consumed, zero retries.

## 27. Scope Exclusions

Excluded: automated LLM correction, spelling correction, identity resolution, fuzzy/global replacement, OCR regeneration merely because review found an error, provider retry, external reasoning, legal interpretation, evidential-weight/admissibility decisions, autonomous materiality decisions, deletion/mutation of provider output, broad document editing, collaborative multi-user review UI, notifications and unrelated UI redesign. This document also authorises no implementation, schema, API, codec, store, policy or deployment change.

## 28. Proposed Implementation Units

### R6T.1 — Contract and canonical-record design

Purpose: implement exact review, discrepancy, correction proposal/acceptance, supersession/adjudication and eligibility contracts plus versioned codecs. Permitted: new narrowly scoped contracts/codecs and tests. Prohibited: production composition, fixture mutation, corrected admission, provider access. Acceptance: closed vocabularies, exact bindings, canonical code-point span convention, unknown-version/corruption failure and legacy-read compatibility proven. Dependency: owner acceptance of this scope lock and a separately reviewed implementation plan.

### R6T.2 — Durable stores and audit

Purpose: create write-once exact-generation stores and narrow audit records. Permitted: prepare/publish/retrieve/list-exact storage, integrity hashes, supersession graph validation, narrow audit extension/port and tests. Prohibited: derivative mutation, general `AuditService`, effective correction admission. Acceptance: create-once, restart durability, historical immutability, cycle/conflict detection and audit ordering pass.

### R6T.3 — Authorization Purpose and owner operations

Purpose: register and policy-bind the two purposes and implement permission-gated review recording, correction acceptance and adjudication coordinators. Prohibited: provider access, implicit self-acceptance, raw store writes from routes. Acceptance: wrong/missing/retired purpose and wrong principal fail closed; proposal and acceptance remain distinct; audit is complete.

### R6T.4 — Corrected representation admission

Purpose: deterministically materialise and atomically admit a separately identified corrected representation from an accepted exact correction set. Prohibited: editing parent content, fuzzy/global replacement, provider calls, unaccepted corrections. Acceptance: exact span replacement, unaffected block equality, full provenance, create-once/conflict behavior and parent retrieval pass.

### R6T.5 — Retrieval and downstream eligibility

Purpose: expose separate provider/review/correction/eligibility views and enforce purpose-specific downstream gates. Prohibited: silent substitution, automatic promotion, external reasoning. Acceptance: unreviewed/material/conflicted states block source-confirmed use; accepted corrected representation is selectable with complete provenance; historical generations default unreviewed; sensitive data remains bounded.

### R6T.6 — R6 fixture recording and production acceptance

Purpose: build/accept/deploy the exact implementation, canonically record the already completed R6S review, verify the two discrepancies, and only under a further explicit owner decision propose/accept/admit correction. Prohibited: new transcription/provider call, historical mutation, conflating recording with correction acceptance. Acceptance: zero provider activity; original hashes unchanged; review status/discrepancies exact; source-confirmed eligibility blocked before correction and correct after separately accepted admission; production/store accounting exact.

Each unit requires its own scoped authority and evidence. No unit may collapse build, owner artifact acceptance, deployment, governance acceptance or data mutation gates where existing Parker discipline separates them.

## 29. Relationship to R6 Closure

Decision: **R6 must remain open** until the minimum governed review/status capability is implemented, deployed and verified for this fixture through R6T.1–R6T.3 and the review-recording portion of R6T.6. The reason is operational, not cosmetic: Parker currently holds a known material discrepancy only in a local owner-review package, while canonical production cannot yet project that fact or prevent the provider text being mistaken for source-confirmed content. Closure before canonical review/discrepancy recording and enforcement would leave a known integrity fact outside the governed decision path.

Creation of the corrected representation itself may proceed as the immediately following separately owner-governed correction phase (R6T.4–R6T.6); R6 closure may occur once the immutable material review is canonical, retrieval exposes it, and source-confirmed use is fail-closed, even if the optional corrected representation has not yet been accepted. The original derivative is preserved in either case.

## 30. Acceptance Criteria

Owner acceptance of this draft freezes the semantic decisions only. A future implementation is acceptable only when it proves: four-layer separation; independent execution/review dimensions; independent observed discrepancy/severity/cause/pattern/human-resolution/overall-fidelity dimensions; actor-neutral discrepancy semantics; exact-generation/source/content/reviewer binding; structured page-local discrepancies; immutable pattern association without replacement authority; immutable/write-once history; separate correction acceptance; exact-span deterministic materialisation; correction without required retranscription; explicit non-temporal supersession/adjudication; conflict failure; purpose-specific downstream eligibility; distinct retrieval views; conservative historical default; unknown/malformed failure; complete narrow audit; two distinct Authorization Purposes; R6 fixture behavior; zero provider calls; and no mutation of source, preparation or provider derivative.

## 31. Frozen Decisions

Pending explicit owner acceptance, the proposed frozen decisions are:

1. Four distinct provenance layers; no historical rewrite.
2. Review state is `UNREVIEWED`, `HUMAN_REVIEWED_PASS`, `HUMAN_REVIEWED_WITH_DISCREPANCY`, or `HUMAN_REVIEW_CONFLICT`; partiality is coverage.
3. Provider execution and fidelity verdict are independent.
4. Discrepancies are structured, classified, severity-bearing, attributable facts; materiality is independent of frequency and overall transcription quality.
5. Cause is an optional independent dimension—established, hypothesised or unknown—and no component infers it from an observed difference.
6. Unknown technical cause does not prevent human resolution of what an exact source location says.
7. Discrepancy semantics are producer/actor-neutral; producer-specific provenance remains separate.
8. Repeated exact discrepancies may share an immutable systematic-pattern identity, but pattern association establishes no cause and authorises no global replacement.
9. Correctable locations bind exact source/generation/page/region/block/code-point span/original value; no global/fuzzy replacement.
10. `PrincipalId`, exact identities/digests and review-artifact digest supply reviewer provenance.
11. Reviews, discrepancies, patterns, proposals, acceptances, adjudications and corrected representations are immutable/additive.
12. Correction uses a separate corrected representation with the provider generation as parent; provider output is never mutated.
13. Correction is exact text-span granular; faithful content is copied unchanged.
14. Proposal and acceptance are separate permissioned acts; no implicit acceptance.
15. Minor discrepancies may permit source-confirmed use only under the explicit Section 16 acceptance; unresolved material discrepancies block it.
16. Exact accepted human correction may restore source-confirmed eligibility without retranscription or provider retry; source resolution and technical-cause resolution remain separate.
17. The provider transcription remains historically unchanged and reviewed-with-discrepancy after any corrected representation is admitted.
18. Formal verdict, descriptive fidelity, discrepancy count, pattern count and severity are distinct; R6 remains formally `FAIL` while truthfully describable as high fidelity overall.
19. Supersession is explicit, single-predecessor and acyclic; timestamps never win.
20. Unresolved material review conflict blocks source-confirmed use and requires authorised adjudication.
21. Downstream eligibility is purpose-specific and returns exact representation plus assurance/provenance.
22. Retrieval keeps provider, review, correction and effective-eligibility views distinct.
23. Historical derivatives require no rewrite and default to unreviewed absent valid exact-generation review.
24. Unknown, malformed, contradictory or ambiguous state fails closed.
25. Audit is narrow, durable, append-only and fact-based.
26. Two distinct future Authorization Purposes are required; Gap #54 is not reopened.
27. External reasoning remains unauthorised and may never receive stripped correction provenance.
28. The R6 fixture remains immutable, material, human-resolved as to source, technically cause-unknown, and source-confirmed-ineligible until separately governed correction acceptance/admission.
29. R6 remains open until canonical review/discrepancy recording and fail-closed downstream enforcement are implemented and verified; correction admission may remain a separately governed immediate follow-on.

**Scope-lock verdict: A — GOVERNED HUMAN FIDELITY REVIEW AND CORRECTION STATUS SCOPE LOCK DRAFTED FOR OWNER ACCEPTANCE.**
