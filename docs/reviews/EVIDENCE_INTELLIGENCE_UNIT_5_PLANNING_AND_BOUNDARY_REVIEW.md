# Unit 5 — The Evidence Intelligence Operation — Planning Review and Boundary Review

**Baseline confirmed:** `HEAD 066dc8e` ("feat: implement evidence intelligence unit 4"), working tree clean. Units 1–4 committed and frozen. No `EvidenceIntelligence` interface exists anywhere in the repository yet — Unit 5 genuinely not begun.

---

## 1. Planning Review

**Unit 5's exact, cited responsibilities** (Implementation Plan §8 Unit 5, restating Contract Design §10 and Scope Lock §6–§7):

| Responsibility | Governing citation |
|---|---|
| Accept one `EvidenceAnalysisRequest`; return a `List<EvidenceAnalysisResult>` | Contract Design §10 ("one operation... given an `EvidenceAnalysisRequest`, return a list of `EvidenceAnalysisResult` values") |
| Resolve inputs via Unit 2 | Implementation Plan §8 Unit 5 ("resolve inputs (Unit 2)") |
| Perform analysis, optionally via Reasoning Providers | Implementation Plan §8 Unit 5; Contract Design §1 ("Orchestrating existing Reasoning Providers") |
| Apply Unit 4's output discipline | Implementation Plan §8 Unit 5 ("apply output discipline (Unit 4)") |
| Represent partial completion via a genuine non-empty list + Unit 2's `EvidenceRetrievalResult`/observability, never a wrapper or empty list | Implementation Plan §8 Unit 5; Contract Design §11 |
| Construct (never submit) a `KnowledgeCandidate`, including via a **second**, later invocation once a proposed record is accepted | Implementation Plan §8 Unit 5 ("resolving the correction task's item 4"); Contract Design §5, §6 |
| Stop at returning the result list — no acceptance, no permission evaluation of its own | Scope Lock §7 ("Evidence Intelligence's own implementation stops at the operation defined in Contract Design §10") |

**Explicit exclusions** (Scope Lock §3, §11; Implementation Plan §11): acceptance orchestration, Permission Engine dependency of its own, any of the eight named zero-dependency items (`OwnerEvidenceDeletionAuthority`, `EvidenceArtifactStorage.delete`, `EvidenceDeletionAudit`, `EvidenceCustodian.accept`, `MemoryCore`'s write interface, Knowledge Memory's Knowledge Submission interface, Knowledge Memory promotion/revision/retirement/restoration, `EvidentialState`), a fifth `EvidenceAnalysisResult` category, any specific analysis kind's own internal algorithm.

**Deferred to Units 6–8:** invocation permission gating (Unit 6 — `PermissionAction.EXECUTE`/`ResourceType.DOCUMENT`, held by the composing caller); acceptance orchestration (Unit 7 — the acceptance coordinator, structurally outside Evidence Intelligence); runtime composition and production reachability, which is explicitly conditioned on Unit 6 also being complete (Unit 8).

**One editorial finding, not a blocker:** `REASONING_PROVIDER_CONTRACT_DESIGN.md` line 15 still literally reads "**Pending acceptance**" for Amendment 1, with no corresponding "Final Acceptance Confirmation" document in `docs/reviews/`. This contradicts the fact that Units 3 and 4 are already committed against it, and contradicts `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`'s own Amendment 1 header, which treats Amendment 1 as already integrated with no "pending" qualifier. The *substance* is unambiguous and already relied upon by frozen, accepted governance and code — this is a stale status header, not a live ambiguity — but it should be corrected the next time governance text is touched.

---

## 2. Boundary Review

- **Reasoning Provider Contract Design/CDR-007:** unaffected; Unit 5 only orchestrates the existing `ReasoningProvider` via Unit 3's already-frozen coordinator pattern.
- **Ownership:** unchanged from Contract Design §5/Scope Lock §5 — Evidence Intelligence owns production up to acceptance, never after; nothing in Unit 5 grants a residual claim.
- **No other Programme is affected.** Evidence Custodian, Memory Core, Knowledge Memory, Permission Engine, Conversation Engine: no contract of theirs is touched, extended, or reinterpreted.
- **No governance amendment is required to *begin* Unit 5.** Every responsibility, boundary, and exclusion Unit 5 needs is already fixed by the Contract Design, Scope Lock, and Implementation Plan. (See §5 and §9 below for the one caveat that materially limits, but does not block, Unit 5.)

---

## 3. Execution Sequence

Established precisely by Scope Lock §6 (Sequencing Freeze), with Unit 5's own place in it:

```
0. Permission Engine evaluates EXECUTE/DOCUMENT ("invoke Evidence Intelligence")   [Unit 6 — NOT Unit 5]
   Denial stops here: no retrieval, no Memory Core read, no ReasoningProvider call.
1. Resolve inputs: EvidenceCustodian.retrieve + MemoryRetrieval                    [Unit 2, called BY Unit 5]
2. Perform analysis, optionally invoking ReasoningProvider                        [Units 3–4, called BY Unit 5]
3. Return List<EvidenceAnalysisResult> — zero, one, or many                       [Unit 5's own contract ends here]
4. Acceptance coordinator dispatches each candidate to its accepting subsystem     [Unit 7 — NOT Unit 5]
5. A KnowledgeCandidate referencing a newly-accepted record is constructed only
   by a SECOND, ordinary Unit 5 invocation, after acceptance                      [Unit 5, invoked again, later]
```

- **Empty-input precondition:** enforced by Unit 2's resolver as its own first action (`require(...)`, already implemented — `EvidenceIntelligenceInputResolver.kt:182`), before any retrieval call. Unit 5 must call this before analysis, never after.
- **Resolved values available afterward:** exactly `Pair<List<EvidenceRetrievalResult>, List<Pair<RelationshipEndpoint, MemoryCoreRecord?>>>` — Unit 2's own already-implemented return shape. Nothing more, nothing less.
- **Reasoning invocation:** via `EvidenceIntelligenceReasoningCoordinator.reason(request, reasoningContext)` — already implemented, Unit 3.
- **Where Unit 4 applies:** while constructing `TransientOutput.text` (and any transcribed `CandidateEvidenceArtifact` content) from whatever Unit 5's own analysis produces — `AnalyticalOutputDiscipline.labelContent`/`labelTranscription` are called by Unit 5's own construction code, not by Units 2 or 3.
- **Zero/one/many results, partial completion:** a non-empty list for every input that resolved and analysed successfully; any input that failed resolution is disclosed exclusively through Unit 2's own `EvidenceRetrievalResult`/observability — never a wrapper, never an empty list standing in for failure (Implementation Plan §8 Unit 5, explicit).
- **KnowledgeCandidate sequencing:** never in the same result list as the record it references; requires the record's acceptance (Unit 7) and a fresh, later Unit 5 invocation, exactly as Implementation Plan §8 Unit 5 spells out — this is the one place governance explicitly requires Unit 5 to be invoked twice for a single logical workflow.
- **Where Unit 5 stops:** at the returned list. No permission evaluation, no acceptance call, no runtime wiring — all explicitly Units 6–8's own responsibility (Scope Lock §7).

No sequencing is invented above; every line is a direct restatement of Scope Lock §6 and Implementation Plan §7–§8.

---

## 4. Output Mapping (Output Conversion Review)

`ReasoningProviderResponse` has exactly three variants (`ReasoningProvider.kt`): `Goal(text: String)`, `Reply(text: String)`, `NoAction`. `EvidenceAnalysisResult` has exactly four variants: `TransientOutput(text, evidenceArtifactReferences, memoryCoreReferences)`, `CandidateArtifactProduced(CandidateEvidenceArtifact)`, `CandidateRecordProduced(CandidateMemoryCoreRecord)`, `CandidateKnowledgeProduced(KnowledgeCandidate)`.

**No governance document anywhere defines a `ReasoningProviderResponse` → `EvidenceAnalysisResult` mapping.** This is confirmed, not merely unfound — the Reasoning Provider Contract Design's own description of `Goal`/`Reply` states both are explicitly "deliberately shaped to be directly usable" as a future `PlanningRequest.goal` or `OutboundParkerResponse.text` respectively — **never** as a `TransientOutput.text`. Evidence Intelligence is not named as an intended consumer of either field's shape.

For each candidate target:

| Target | Exact source needed | What a bare `ReasoningProviderResponse` actually carries | Unambiguous? |
|---|---|---|---|
| `TransientOutput` | non-blank text **and** ≥1 governed reference (`EvidenceArtifactId`/`RelationshipEndpoint`) — constructor-enforced | `Reply.text`/`Goal.text`: prose only, **no reference of any kind** | **No** — the reference(s) must come from Unit 2's resolved inputs, not from the response; which specific reference(s) attach to which specific claim is analysis-kind-specific judgment, never carried by the response itself |
| `CandidateArtifactProduced` | a `CandidateEvidenceArtifact(content: ByteArray)` | prose `String` only | **No** — no byte content, no transcription-fidelity information exists in a bare response |
| `CandidateRecordProduced` | a `CandidateAssertion`/`CandidateRelationship`, each requiring a mandatory `Provenance` reference (Memory Core Contract Design §7) | prose `String` only | **No** — no provenance, no relationship-endpoint pair exists in a bare response |
| `CandidateKnowledgeProduced` | a `KnowledgeCandidate` referencing an **already-accepted**, identifier-bearing Memory Core record | prose `String` only, and structurally cannot reference anything not-yet-accepted within the same invocation | **No** — additionally gated by the two-invocation sequencing rule (§3, above) |

**This absence of a mapping is by design, not an omission.** Both the Contract Design ("any specific analysis kind's own internal algorithm" — Out of Scope) and the Scope Lock (§3, same exclusion, restated in Implementation Plan §11) permanently exclude "how comparison, OCR, translation, or summarisation is actually performed" from every tier of this governance chain. Constructing a genuine `EvidenceAnalysisResult` from a resolved input plus an optional reasoning response is exactly that excluded algorithm — it is ordinary, un-governed engineering judgment exercised per `analysisKind` (itself open and non-enumerated), not a contract shape any of the seven documents were ever meant to fix. Unit 5's own verification goals and completion criteria (Implementation Plan §8 Unit 5) are accordingly all **structural** (no acceptance-interface call, no fabricated empty list, no premature `KnowledgeCandidate` reference) — none require Unit 5 to prove any particular *analytical correctness*, consistent with this reading.

**One genuine edge case worth flagging, not fabricating a rule for:** nothing forbids a `ReasoningProvider` invoked via `OfEvidenceAnalysisRequest` from returning `Goal` — the interface has no per-subject restriction on which response variant is valid. No `EvidenceAnalysisResult` category corresponds to "a goal worth planning," and Evidence Intelligence holds no Planner Runtime dependency to route one to (an explicit Scope Lock exclusion). This is not a governed scenario at all; a concrete Unit 5 implementation should treat an unexpected `Goal` from an Evidence-Intelligence-invoked provider as an implementation-level anomaly (log/fault), never silently coerce it into any of the four categories.

**Do not assume a provider response contains information it does not carry:** confirmed — nowhere above did this review assume traceability, provenance, or byte content is recoverable from `Goal`/`Reply`/`NoAction`.

---

## 5. ReasoningContext Analysis

This is the most material finding of this review.

`EvidenceIntelligenceReasoningCoordinator.reason(request, reasoningContext)` (Unit 3, already committed) takes `reasoningContext` as a value supplied by **its own caller** — it does not read `EvidenceAnalysisRequest.reasoningContext` itself (proven by its own test: *"reason supplies the top-level reasoningContext parameter... never `EvidenceAnalysisRequest`'s own field"*). Unit 5, as that caller, must supply one.

This review checked whether any existing mechanism could lawfully supply it:

- **`EvidenceAnalysisRequest.reasoningContext` (the nested, optional field)** — **forbidden as a source**, not merely unhelpful. The Reasoning Provider Contract Design's own Amendment 1 invariant states the nested field, "for Evidence Intelligence's own purposes, is not read, not merged, not compared, and **not otherwise given any effect on this invocation**." Using it to populate the top-level value Unit 3 forwards to `ReasoningProvider.reason` would be exactly the "effect on this invocation" this frozen invariant exists to rule out.
- **`DefaultReasoningContextAssembler`** — the one production assembler that exists in this repository — is governed entirely by separate documents outside Programme 4 (`PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md`, `CONVERSATION_HISTORY_SOURCE_CONTRACT_DESIGN.md`, etc.), and its `assemble` method's input is `ResolvedInboundMessage` — a Conversation-Engine/Communication-shaped type structurally incompatible with `EvidenceAnalysisRequest`. Reusing or extending it for Evidence Intelligence is not authorised anywhere.
- **A new, Evidence-Intelligence-specific assembler** — would be an unauthorised **seventh** new component; Implementation Plan §6 closes with "No seventh new component is introduced anywhere in this plan," and its six-item list (operation, request shape, result shape, payload selector, acceptance coordinator, invocation gate) has no seventh slot for one.

**Conclusion:** assembly of a non-empty `ReasoningContext` for Evidence Intelligence's own invocations remains genuinely, permanently unassigned by this governance chain — exactly as the Reasoning Provider Contract Design's own Section 9 already discloses ("`ReasoningContext`'s own assembly mechanism... remains entirely unassigned"), and no later Programme 4 document ever assigns it. However, **Unit 5 can still lawfully invoke Unit 3** without inventing anything, by supplying `ReasoningContext(emptyList())` — an entirely empty list is expressly permitted by `ReasoningContext`'s own frozen shape (its own KDoc gives exactly this precedent: "e.g. the first Turn of a new Conversation"). This is the trivial, always-valid value, not an invented mechanism.

**Per the instruction's own test ("if Unit 5 cannot invoke Unit 3 without inventing a source, classify that as a blocker"): this is not a blocker**, because a lawful, non-invented value exists. But it is a real, load-bearing limitation: as governed today, every Evidence-Intelligence-invoked Reasoning Provider call is necessarily context-free beyond the `EvidenceAnalysisRequest` payload itself carried in `subject`. This should be recorded as a known limitation of Unit 5, not silently worked around, and not solved by Unit 5 inventing a mechanism no document authorises.

---

## 6. Failure and Partial-Completion Analysis

| Outcome | Representation | Citation |
|---|---|---|
| All inputs resolve, analysis succeeds | Non-empty `List<EvidenceAnalysisResult>` | Contract Design §5 |
| Some references missing/rejected | `EvidenceRetrievalResult.NotFound`/`Rejected` per input, each self-identifying by its own `evidenceArtifactId`; Memory Core misses self-identify via the `(RelationshipEndpoint, MemoryCoreRecord?)` pairing Unit 2 already implements | Contract Design §11; Implementation Plan §8 Unit 2 |
| ReasoningProvider refusal (`NoAction`) | A confident semantic determination — never a failure signal, never converted into a fabricated result | Reasoning Provider Contract Design §3 |
| ReasoningProvider inability/fault | Propagates as a thrown exception outside the sealed types, never `NoAction`, never an empty result standing in | Contract Design §11; Reasoning Provider Contract Design §3 |
| Empty analytical output | A legitimate, confident "found nothing worth proposing" — not itself a failure | Contract Design §5, §11 |
| Partial success alongside failure | The successful portion is a genuine, non-empty result list; the failed portion is disclosed exclusively via `EvidenceRetrievalResult` + existing observability — never a wrapper, never a fifth variant, never silent omission | Contract Design §11 (explicit third outcome); Scope Lock §8, §9 |

**No result identity depends on list position, logs, or exception-string parsing:** confirmed structurally — `EvidenceRetrievalResult.Found`/`NotFound`/`Rejected` and the Memory Core `(RelationshipEndpoint, MemoryCoreRecord?)` pairing are each self-identifying by their own carried identifier, exactly as Unit 2's own KDoc explains it resolved `GCR-EI-UNIT2-001`'s original defect.

---

## 7. Implementation Inventory

| Artefact | Visibility | Purpose | Dependencies | Why authorised | Likely location |
|---|---|---|---|---|---|
| `EvidenceIntelligence` interface | **public** | The one new public operation (§10 of the four total authorised across the whole Programme) | `EvidenceAnalysisRequest`, `EvidenceAnalysisResult` | Contract Design §10; Scope Lock §4 | `src/interfaces/EvidenceIntelligence.kt` (extends the existing Unit 1 file) |
| A concrete implementation (name not fixed by governance; precedent suggests `DefaultEvidenceIntelligence`, public, mirroring `DefaultEvidenceCustodian`/`DefaultReasoningContextAssembler`) | public class | Assembles Units 1–4 behind the interface | `EvidenceIntelligenceInputResolver` (Unit 2), `EvidenceIntelligenceReasoningCoordinator` or `ReasoningProvider` (Unit 3), `AnalyticalOutputDiscipline` (Unit 4) | Implementation Plan §6 item 1; §8 Unit 5 | `src/runtime/` |

No further new type is authorised. No interface change, no new sealed variant, no acceptance-interface dependency, no Permission Engine reference. The acceptance coordinator (Unit 7) and the invocation gate (Unit 6) are explicitly **not** Unit 5's own artefacts.

---

## 8. Verification Plan

- Complete successful analysis: resolved inputs → non-empty, correctly-shaped result list.
- Empty-request rejection: `require` fires before any retrieval, mirroring Unit 2's own test.
- Mixed `Found`/`NotFound`/`Rejected` evidence within one invocation: each disposition individually identifiable in the same invocation's disclosure.
- Missing Memory Core records: `null` results correctly paired with their originating `RelationshipEndpoint`.
- Each `ReasoningProviderResponse` case (`Goal`, `Reply`, `NoAction`) exercised via a fake provider (mirroring Unit 3's own `FakeReasoningProvider` — **not** `ModelReasoningProvider`, which currently throws `UnsupportedOperationException` for `OfEvidenceAnalysisRequest`).
- Multiple output results in one invocation; partial completion (some succeed, some fail) never collapsed into total success/failure.
- Claim-level traceability: multiple claims represented as multiple `TransientOutput` values, never one bundling several, reusing Unit 1's own constructor guarantee.
- Contradiction disclosure: `discloseContradiction`'s own signature reused, never a single-sided result.
- Provider-context invariant: the `ReasoningContext` Unit 5 supplies to Unit 3 is the exact same value present on the resulting `ReasoningProviderRequest.reasoningContext`, and `request.reasoningContext` (nested) is never read to derive it — a direct, structural test.
- No permission gating: Unit 5's own constructor/dependency reflection test proves no `PermissionEngine` reference exists.
- No acceptance: reflection/dependency-reachability test proves no path to any of the eight excluded items.
- No runtime composition: Unit 5 not wired into `ParkerRuntime` by this Unit.
- No new public contract beyond `EvidenceIntelligence`: a structural test enumerating the compiled repository's public runtime types, expecting exactly four.

---

## 9. Governance Sufficiency Test — Summary

| Check | Result |
|---|---|
| Missing resolved-input carrier | Not missing — Unit 2's `Pair<List<EvidenceRetrievalResult>, List<Pair<RelationshipEndpoint, MemoryCoreRecord?>>>` already exists |
| Missing ReasoningContext source | **Materially limited, not blocking** — no non-empty source is governed anywhere; an empty `ReasoningContext` is the one lawful, non-invented value (§5, above) |
| Missing response-to-result mapping | Not missing — deliberately, permanently unassigned as "analysis kind's own internal algorithm," out of scope by design at every tier, not an oversight |
| Missing provenance/reference association | Not missing for `TransientOutput` (Unit 1's own constructor invariant already enforces it); genuinely absent for the other three categories, but that absence is itself the correctly-excluded "analysis algorithm," not a Unit 5 gap |
| Missing failure representation | Not missing — fully specified (§6, above) |
| Ambiguity over Unit 4 labels | Resolved — labels remain embedded in the returned `TransientOutput.text`; nothing authorises stripping them; presentation is explicitly out of scope for the whole Programme |
| Ambiguity over KnowledgeCandidate sequencing | Resolved — the two-invocation rule is explicit (§3, above) |
| Need for a new public/internal shape not authorised | None found |

---

## 10. Final Verdict

**A — GOVERNANCE SUFFICIENT.**

Unit 5 can be implemented completely using the frozen contracts and existing Unit 1–4 outputs. The one genuine gap found — `ReasoningContext` assembly for Evidence Intelligence's own invocations — is resolvable lawfully today (an empty `ReasoningContext`) without inventing any mechanism, and does not require a governance return to *begin* Unit 5. It should, however, be recorded plainly in Unit 5's own engineering report as a disclosed, permanent limitation, not silently worked around as if it were solved.

---

## 11. Confirmation No Files Changed

`git status --porcelain` was checked before and after this review: clean both times. No governance document, no production code, and no test file was modified, created, or deleted.

## 12. No Git Actions

Nothing staged, committed, or pushed.
