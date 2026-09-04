# OI11R6V-A10 — Sequence A Programme Closure and Final Acceptance

## 1. Status

**Status: ACCEPTED — CANONICAL — CLOSED**

**Programme:** Ordinary Ingestion 11R6 Sequence A

**Closure basis:** Frozen R6T Scope Lock + Frozen R6V Implementation Plan + completed A1–A9 implementation, deployment, and verification record.

## 2. Purpose

This record determines whether the accepted and frozen Sequence A architecture was implemented, deployed, and verified in production. It records programme acceptance and closure only. It authorizes no implementation, production mutation, deployment, provider activity, correction, or Sequence B work.

## 3. Authoritative Governance

- R6T Scope Lock: `docs/architecture/ORDINARY_INGESTION_11R6T_GOVERNED_HUMAN_FIDELITY_REVIEW_AND_CORRECTION_STATUS_SCOPE_LOCK.md`, SHA-256 `f90c9fc654136ea5e92723a1704ad58108ab0f8a9e73a401e8039d3210e4cd2a`, accepted/canonical/frozen.
- R6V Implementation Plan: `docs/architecture/ORDINARY_INGESTION_11R6V_GOVERNED_HUMAN_FIDELITY_REVIEW_AND_CORRECTION_STATUS_IMPLEMENTATION_PLAN.md`, SHA-256 `86f7b27095a6b80b2618556797a877a21b097f76c3c9ba2d91e235b90c395d1d`, accepted/canonical/frozen.
- Final Sequence A implementation source: `e2e824c062c94ffe5b8b75a387a753de7d2f72ce`.
- Final production verification: OI11R6V-A9 commit `4c89fa69a9dd1114c0db502209839f43510a9f41`.

The closure precheck found branch `main`, exact HEAD/upstream `4c89fa69a9dd1114c0db502209839f43510a9f41`, a clean worktree, exact governance hashes, every required report, the exact healthy production artifact, one canonical R6S review, one R6 provider derivative, preserved provider budget, no corrected representation, and no Sequence B correction capability.

## 4. Sequence A Implementation Record

| Unit | Delivered boundary | Completion report commit |
|---|---|---|
| R6V-A1 | Provider-independent human-fidelity domain contracts and exhaustive validation | `58c03abcd96b62f17f10069825982454fd06e680` |
| R6V-A2 | Canonical v1 codec, durable create-once review storage, and narrow append-only audit | `c355ba8ee9254bd1925ada7245e018e568f4616b` |
| R6V-A3 | Dedicated owner-bound `document-ingestion.human-fidelity-review-recording` authority | `8bfb6847204735cb416f654357bd153d183b0640` |
| R6V-A4-MIN | Minimum authorization-before-mutation governed review recording service | `d60530a7ec92af82222ead7857a5270e4285814c` |
| R6V-A5 | Production composition and exact production-equivalent R6 offline convergence | `01fd54237227daff7d0b83064825dd004c9fa1f6` |
| R6V-A6 | Initial exact candidate artifact build and owner deployment gate | `641f69540548e7cff0cf0b87513ad0a12a0dca43` |
| R6V-A7 | Owner-accepted initial artifact deployment | `04c90c3149de41aa799abdd027267f6629e1b32e` |
| R6V-A8 | Owner-authorized canonical historical R6S review recording | `bc83fa3566a248d0b8cf2ba8545173364c1fe668` |
| R6V-A8A | Deterministic effective review and purpose-sensitive source-confirmed eligibility projection | `5f9f4e8c12ca8fb03383c492bd87e862db098e2d` |
| R6V-A8B | Existing governed retrieval/presentation integration | `e2e824c062c94ffe5b8b75a387a753de7d2f72ce` |
| R6V-A8C | Exact final Sequence A candidate artifact and owner deployment gate | `d05230f967094f1efe8624f1b3a1466cebfd654a` |
| R6V-A8D | Owner-accepted final Sequence A artifact deployment | `ba1d551f90cf9e2924be0e10db7f5a6fc508f06e` |
| R6V-A9 | Final production readback, eligibility, compatibility, and immutability verification | `4c89fa69a9dd1114c0db502209839f43510a9f41` |

Each corresponding report is present under `docs/architecture/ORDINARY_INGESTION_11R6V_A*.md`. The sequence preserved separate contract, durability, authority, service, composition, artifact, deployment, governed recording, projection, presentation, and production-verification gates.

## 5. Historical R6 Execution Record

R6 did not succeed as one uninterrupted operation. The original governed execution passed every pre-egress gate, used exactly one authorized OpenAI call, received HTTP 200, durably preserved raw provider state, and successfully validated five V8 regions in Parker order. It consumed the one-call budget with zero retries.

The first post-egress path then failed closed before derivative admission because the derivative payload supplied the capability processing profile where the version-3 contract required authorization-bound provider-profile provenance. A later zero-egress continuation initially failed its historical-attempt identity gate; the relevant implementation was corrected and accepted separately. A subsequent owner-authorized zero-egress continuation replayed preserved state and admitted exactly one derivative without another provider call. Its immediate owner readback then exposed a distinct canonical presentation compatibility defect. That read-only presentation defect was corrected and deployed without retranscription or provider mutation. The provider derivative remained immutable throughout.

Historical provider facts are:

- Authorization: `ff286fdcc38a35aefed16201724c00d8a9930e2f73c08206571295a664127f97`
- Execution: `ordinary-exec-3c2bf685-d6c2-44e0-acf8-0224d92fd976`
- Provider-state identity: `2b1fbe06ebee0b7a3fdb618159c6987fa713976d7bfd2732b9048b50f11df3a7`
- Raw response SHA-256: `4706c24b8b0b83675a8ded1165f316229fa61a92bff4d8fe0a16c1d7d50cfb4a`
- Provider response ID: `resp_04aa0adc3e021174016a980c0c891487d09764395f58adef7b`
- Provider/model: `OpenAI` / `gpt-5.6-sol`
- Provider call budget: maximum `1`, consumed `1`, retries `0`

The canonical provider-state record was inspected at closure and directly reports the full raw digest above. The malformed shorter value `4706c24b8b03675a8ded1165f316229fa61a92bff4d8fe0a16c1d7d50cfb4a` appearing in the A10 instruction is rejected and corrected here; it is not an authoritative historical identity.

## 6. Final Production Artifact

Closure readback verified:

- Source: `e2e824c062c94ffe5b8b75a387a753de7d2f72ce`
- Runtime JAR SHA-256: `d78c99a389ba3a75e868d007ac13f0678f84ac25e46e7aeca41a513f510865e2`
- Image: `sha256:26a503564698b4ac248cb3e9d94ceba7813b713523dd9b995e73cbf922267895`
- Container: `e4d2c427776a4a8c103bfb8847ed0923acbb215c55b68803f94df89a0e8ae751`
- Runtime status/readiness: running / PASS
- Restart count: `0`

No build, deployment, restart, or configuration change occurred during closure.

## 7. R6 Provider Derivative

- Generation: `region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6`
- Generation SHA-256: `9fb18b02db5ac55e5d446cd48ebc619de929c4596f94d2a11fba1a07da71af14`
- Content SHA-256: `18a6ed08a4729350027d3140dc0f07dd49d32c04aa45f9e3e9558df5d007c4eb`
- Canonical production presentation: `RETRIEVED / REGION_TRANSCRIPTION`
- Regions/pages/blocks: `5 / 5 / 5`
- Parker order: `[1,2,3,4,5]`

Exactly one R6 provider derivative exists. It remains retrievable, historically attributable, provenance-bearing, and unchanged. Its provider transcription still says `Kellee` at pages 1 and 5.

## 8. Canonical Human Fidelity Review

- Review ID: `review-3cf3186ca166acb0f4b6331ca574926dc874225247b296fb972666504992ea6e`
- Stored-record SHA-256: `13e6f5e285d95e19c0926821b63422486e005d22ee484feb70a6b54635046106`
- State: `HUMAN_REVIEWED_WITH_DISCREPANCY`
- Coverage: `FULL_GENERATION`, pages `[1,2,3,4,5]`
- Overall descriptive fidelity: high
- Material discrepancy occurrences: `2`
- Systematic patterns: `1`
- Technical cause: `UNKNOWN`
- Human source resolution: `Kellec` at pages 1 and 5
- Unresolved conflict: `false`
- Review records: `1`
- Immutable review audit facts: `3`

The human review is a separate, attributable, create-once provenance fact. It neither mutates nor relabels provider output.

## 9. Materiality and Descriptive Fidelity

The two independently location-bound `Kellee`/`Kellec` discrepancies are material because they concern an identity-bearing proper name. They are two occurrences associated with one descriptive systematic pattern. Pattern association supplies no replacement authority and establishes no technical cause.

Materiality does not imply low document-wide transcription quality. Pages 2, 3, and 4 passed; no other substantive printed-text error, missing material text, or added/hallucinated substantive text was identified; and page 5 handwriting/signature uncertainty was handled appropriately. Overall descriptive fidelity remains high while the formal historical owner verdict remains FAIL and the canonical semantic state remains `HUMAN_REVIEWED_WITH_DISCREPANCY`.

Technical cause remains `UNKNOWN`. Visual-character, glyph, font/rendering, rasterisation, or provider-interpretation ambiguity are possible mechanisms only, not established facts.

## 10. Source-Confirmed Eligibility

Final production evaluation for `SOURCE_CONFIRMED_WHOLE_GENERATION` is:

**DENIED — MATERIAL_DISCREPANCY**

This denial is the correct fail-closed outcome and is not a programme failure. It proves that Parker does not silently promote known-discrepant provider text into source-confirmed evidence. Human source resolution `Kellec` remains a review fact; provider value `Kellee` remains provider output. Sequence A creates no merged or corrected transcript.

## 11. Acceptance Criteria Matrix

| # | Criterion | Result | Closure evidence |
|---:|---|---|---|
| 1 | Provider transcription immutable | PASS | Exact generation/content hashes and single derivative preserved through A9/A10 |
| 2 | Source evidence immutable | PASS | Evidence SHA-256 `5d73e6e5…237e` preserved |
| 3 | Corrected preparation immutable | PASS | Identity `85054cc7…bed8f` and record hash preserved |
| 4 | Human review separately provenance-bearing | PASS | Separate exact target, reviewer, artifacts, review ID, and digest |
| 5 | Human review create-once durable | PASS | A2 durability plus one canonical record across restart/deployment |
| 6 | Review authority fail-closed | PASS | A3 exact principal/purpose/target enforcement |
| 7 | Review recording owner-bound | PASS | Canonical A8 act attributed to configured owner `user.steve` |
| 8 | Effective review deterministic | PASS | A8A projector and exact A9 production projection |
| 9 | Conflict state fail-closed | PASS | Unsupported/ambiguous multiple-review state projects conflict/denial without winner heuristics |
| 10 | Eligibility purpose-sensitive | PASS | Typed `SOURCE_CONFIRMED_WHOLE_GENERATION` evaluation |
| 11 | Material discrepancy denies source-confirmed whole-generation use | PASS | Production `DENIED / MATERIAL_DISCREPANCY` |
| 12 | Raw provider derivative remains retrievable | PASS | Production `RETRIEVED / REGION_TRANSCRIPTION` |
| 13 | Human status presented alongside provider representation | PASS | A8B additive `humanFidelityStatus` production result |
| 14 | Provider and human facts not flattened | PASS | Provider `Kellee`; review source resolution `Kellec` |
| 15 | Historical Tier A retrieval compatible | PASS | Historical PDF retrieval remained `RETRIEVED` |
| 16 | Unknown/unsupported retrieval fail-closed | PASS | Safe production unknown-generation check returned `UNKNOWN_GENERATION` |
| 17 | Canonical review survives restart/deployment | PASS | One record with exact SHA after A8D and A9 |
| 18 | Final production artifact exact | PASS | Source/JAR/image/container all exact |
| 19 | Production readiness | PASS | Runtime started and remains ready |
| 20 | Restart count | PASS | `0` |
| 21 | Provider budget preserved | PASS | Maximum 1, consumed 1, retries 0 |
| 22 | No retry occurred | PASS | Historical and closure retry count `0` |
| 23 | No second provider call occurred | PASS | One historical consumed call; no later provider attempt |
| 24 | No correction exists | PASS | No correction record or authority |
| 25 | No corrected representation exists | PASS | Production derivative stores contain no corrected representation |
| 26 | No Sequence B capability implemented | PASS | No correction contracts/service/composition/purpose in production source |
| 27 | No Gap #54 implementation | PASS | Explicitly excluded throughout the sequence |
| 28 | Audit/provenance preserved | PASS | Three immutable review audit facts and historical provider provenance unchanged |
| 29 | Governed-state immutability verified at A9 | PASS | Before/after counts and aggregate hashes identical; semantic delta 0 |
| 30 | Final canonical production retrieval semantics verified | PASS | Exact A9 governed owner retrieval and eligibility result |

All thirty closure criteria pass.

## 12. Immutability and Provenance

Source evidence, corrected preparation, authorization, attempt ledger, provider state, raw response, admitted provider generation/content, canonical review, and review audit remain distinct immutable records. A9 verified zero before/after semantic delta across the relevant governed stores. A10 performed only read-only identity and count checks and created no governed production record.

At closure there remain 23 total derivative-generation files, 21 total derivative-content files, 10 provider-attempt ledgers, 8 provider-state records, one canonical R6S human review, three associated review-audit facts, and exactly one R6 provider derivative. The exact R6 generation, content, and review hashes match the authoritative record.

## 13. Provider Activity Accounting

Historical R6 budget remains:

- Authorized calls: `1`
- Consumed calls: `1`
- Retries: `0`

A10 activity was OpenAI calls `0`, Claude calls `0`, other provider calls `0`, retries `0`, and external evidence egress `0`. No external reasoning occurred. No provider activity has occurred since A9.

## 14. Historical Compatibility

Sequence A added runtime presentation metadata without changing persisted derivative formats or historical `HumanVerificationRecord` semantics. A9 proved a known historical searchable PDF still retrieves as `PDF / ACCOUNTED_FOR_WITH_QUALIFICATIONS`. Raw historical provider derivatives remain available under their existing retrieval authority even when source-confirmed eligibility is denied.

## 15. Fail-Closed Behaviour

Exact authority precedes review mutation; corrupt, unsupported, mismatched, ambiguous, or conflicting review state cannot produce source-confirmed eligibility. No timestamp, collection order, or lexical identity selects a review winner. Partial coverage cannot assert whole-generation confirmation. A9's safe unknown-generation production request returned `UNKNOWN_GENERATION`. The R6 material-discrepancy denial is the intended fail-closed result.

## 16. Explicit Non-Claims

Sequence A closure does **not** mean:

- the provider transcription is source-confirmed;
- the two discrepancies disappeared;
- technical cause has been established;
- a corrected representation exists;
- human correction has been implemented;
- all future evidence may be treated as source-confirmed;
- provider output may be silently rewritten;
- human review is unnecessary for evidential fidelity;
- Sequence B is complete; or
- Gap #54 has been implemented.

## 17. Sequence B Boundary

Sequence B remains a separate future programme path under the frozen R6V plan. Its subject is a separately governed human-corrected representation, including correction proposal, acceptance, provenance, admission, retrieval, and purpose-specific eligibility. A10 neither designs, implements, authorizes, nor records any part of that path. `Kellec` has not been promoted into a corrected canonical transcript.

## 18. Final Production State

The final read-only closure check confirmed the exact deployed source, JAR, image, and container; running/readiness PASS; restart count zero; exactly one canonical R6S review with exact stored-record SHA; exactly one R6 provider derivative with exact generation/content SHAs; provider budget maximum one/consumed one/zero retries; no corrected representation; no Sequence B capability; and no new provider activity.

Governed-state delta during closure: `0`.

## 19. Programme Closure Decision

**ORDINARY INGESTION 11R6 SEQUENCE A IS ACCEPTED AND CLOSED.**

The first governed real-document external transcription executed under bounded authorization, used one provider call and zero retries, preserved raw provider state, produced one immutable five-region provider derivative through governed zero-egress continuation, became canonically retrievable in production, underwent attributable human fidelity review, preserved two material identity discrepancies as separate review facts, remained immutable as provider output, and was correctly denied source-confirmed whole-generation eligibility. This demonstrates fail-closed governance rather than silent correction.

Sequence B remains separate, outstanding, and separately governed.
