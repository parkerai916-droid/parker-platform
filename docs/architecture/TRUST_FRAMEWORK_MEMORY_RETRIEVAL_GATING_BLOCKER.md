# Trust Framework — Memory Retrieval Gating Blocker

## Status

**Governance review and problem statement only. No Kotlin is implemented, proposed as a diff, or changed by this document.** Neither `src/` nor `tests/` is touched. This document does not amend `docs/architecture/10-permission-engine.md` ("Chapter 10"), `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`, `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md` ("Errata 004"), `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`, or any other governance document — it records a finding and its evidence, and hands off a scoping question to whichever future Contract Design/Scope Lock pass takes it up. It does not design a resolution. Nothing is staged, committed, or pushed — this includes the Parker Conversational Memory Bridge, Admission Unit's own already-complete, already-reviewed implementation, which remains deliberately uncommitted pending this blocker's own resolution.

**Priority: highest-priority open governance work**, ranked above all other currently-planned units, for the reason given in Section 5.

---

## 1. Executive Summary

`KnowledgeSubmission.submit` — the single, existing, governed path by which any Memory Core evidence becomes a promoted Knowledge Item — **cannot succeed for any caller, for any candidate, anywhere in the live, composed Parker runtime, today.** This is not a new defect. It has been true since Evidence Intelligence's own runtime composition first wired `DefaultKnowledgeCandidateEvaluator` to `PermissionFilteredMemoryRetrieval`, and it was never previously discovered because no prior unit performed genuine, live, end-to-end verification of a successful promotion against the real, fully-composed runtime — every existing test either uses a raw, unfiltered `MemoryCore`/`MemoryRetrieval` in isolation (correctly proving the *evaluator's* own logic, but not the *composition*), or deliberately, correctly proves the retrieval decorator's own fail-closed denial as the expected outcome, never attempting a genuine success case against it.

It was found by the Parker Conversational Memory Bridge, Admission Unit, whose own governing task required exactly the kind of live, behavioural, end-to-end verification that had never previously been applied to this path. That Unit's own implementation is confirmed correct and is not the cause — see Section 4.

---

## 2. Discovery Context

Full primary evidence, including the original live reproduction, is recorded in `docs/reviews/CONVERSATIONAL_MEMORY_ADMISSION_COMPLETION_REVIEW.md` (Section 10) and independently re-derived from first principles in `docs/reviews/CONVERSATIONAL_MEMORY_ADMISSION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` (Section 6, `ACCEPTED`). This document restates the root-cause trace directly, rather than only by reference, so that it stands on its own as the primary governance record for this blocker specifically.

A real `ParkerRuntime`, running against a real (stubbed) model server, given an explicit "Remember that my favourite coffee mug is black" instruction, correctly recognised the instruction, correctly and durably wrote a Memory Core `Provenance` and `Assertion` through a properly permission-gated path, and then correctly, honestly reported failure — because `KnowledgeSubmission.submit`'s own internal evaluator could not resolve the record it had itself just been given a reference to, moments after that same record was durably created.

---

## 3. Root Cause, Traced

1. `ParkerRuntime.kt` constructs exactly one `DefaultKnowledgeCandidateEvaluator`, given `permissionFilteredMemoryRetrieval` as its retrieval dependency. This is the same instance `EvidenceIntelligenceInputResolver` also holds — there is no second evaluator and no second retrieval decorator anywhere in the composed graph.
2. `PermissionFilteredMemoryRetrieval`'s own KDoc states its own governing design directly: "Memory Core records are never Resource Registry entries, so `ExecutionRequest.targetResources` is always `emptyList()`" (Errata 004 Section 7).
3. `DefaultPermissionPolicy.evaluate` computes `resourceTypes` by resolving `request.targetResources` against the `ResourceRegistry`. An empty `targetResources` list resolves to an empty `resourceTypes` set, unconditionally, for every retrieval request this decorator ever issues.
4. `ActionMapper.mapOne` computes `applicable = entry.mappings.filter { it.resourceType in targetResourceTypes }`. An empty `targetResourceTypes` set can never contain any `ResourceType`, so `applicable` is always empty — **regardless of whether the proposed action's own verb phrase is registered in the Action Vocabulary at all.** Registering `PermissionFilteredMemoryRetrieval`'s own `RETRIEVE_ACTION_NAME`/`RETRIEVE_DOCUMENT_ACTION_NAME` verb phrases (they are not registered today) would not change this outcome — the failure occurs one step earlier, at resource-type resolution, not at vocabulary lookup.
5. `DefaultPermissionPolicy.evaluate` returns `deniedDecision(request)` whenever no mapping resolves. Every retrieval request `PermissionFilteredMemoryRetrieval` issues is therefore denied, unconditionally, for every principal, for every record, by construction — not by policy configuration, and not correctable by registering anything in the Action Vocabulary or Resource Registry as those two registries are actually used today.
6. `DefaultKnowledgeCandidateEvaluator.resolve()` calls `memoryRetrieval.getEntity`/`getAssertion`/`getDocument`/`getRelationship` — all four routed through the same, always-denying decorator — and receives `null` in every case. `evaluate()` therefore always returns `KnowledgeCandidateEvaluation.Reject("the referenced Memory Core record could not be resolved...")`, regardless of whether the record genuinely exists, regardless of who is asking, and regardless of how it was created.

This is a **structural** property of the current composition, not a missing configuration value, not a missing test, and not specific to any one caller's own candidate shape.

---

## 4. Blast Radius

| Consumer | Effect | Status |
| --- | --- | --- |
| `PermissionFilteredMemoryRetrieval` itself | Denies every retrieval, for every principal, unconditionally | Working exactly as designed (Errata 004's own deliberate fail-closed guarantee) — not itself defective |
| `DefaultKnowledgeCandidateEvaluator` / `KnowledgeSubmission.submit` | Can never resolve, and therefore can never promote, any candidate | **Blocked**, confirmed for every caller |
| Evidence Intelligence (`EvidenceIntelligenceAcceptanceCoordinator.dispatch` → `knowledgeSubmission.submit`) | Would hit the identical resolution failure for any candidate it dispatched | **Blocked** in principle; not independently confirmed reachable in production today for a separate, distinct reason — `EvidenceAnalysisResult.CandidateRecordProduced` (the variant that would carry such a candidate) is declared but never constructed anywhere in `src/`, so Evidence Intelligence's own live reasoning pipeline does not currently reach this call at all. This second gap is disclosed here for completeness and is not investigated further by this document. |
| Parker Conversational Memory Bridge, Admission Unit | Genuinely reaches this call, for real, via a real owner instruction | **Blocked**, confirmed directly, live, end-to-end |
| A future Parker Conversational Memory Bridge, Retrieval Unit | Would have nothing to retrieve, even once built, since nothing can be promoted | **Blocked**, transitively — building retrieval before this is resolved would produce a unit with no reachable success case |

**This blocks two already-built subsystems' own basic, intended usability (Knowledge Memory's own promotion pipeline; Evidence Intelligence's own knowledge dispatch), not merely one new unit's own forward progress.** That is the basis for this blocker's own priority ranking (Section 5).

---

## 5. Why This Is Now the Highest-Priority Governance Work

- It is not a missing feature; it is a **already-built subsystem that cannot do the one thing it exists to do**, in the live runtime, for any caller — discovered only because live verification was finally, genuinely applied to it.
- It blocks **two independently-governed subsystems already treated as complete** (Knowledge Memory's own Unit 6–9 sequence; Evidence Intelligence's own acceptance pipeline), not only new, not-yet-built work.
- Every future unit that depends on a genuinely promoted Knowledge Item — conversational retrieval chief among them, but not exclusively it — is blocked transitively until this is resolved, meaning further work in that direction would be built on an untestable foundation.
- The fix is squarely Trust-Framework-tier (Chapter 10 / `DefaultPermissionPolicy` / `ActionMapper` / Memory Core's own Resource-representation choice), not Programme-3-tier or Programme-4-tier — resolving it correctly, once, at the right layer, avoids each dependent Programme separately inventing its own narrower, inconsistent workaround.

---

## 6. Why No Narrow, In-Scope Fix Exists

Investigated directly (at the Conversational Memory Bridge Admission Unit's own governing task's explicit direction) for a narrow fix that would not touch frozen Trust Framework components. None exists:

- Registering `RETRIEVE_ACTION_NAME`/`RETRIEVE_DOCUMENT_ACTION_NAME` in the Action Vocabulary does not help — the failure occurs at resource-type resolution (Section 3, step 3–4), before vocabulary lookup is ever consulted.
- Registering every individual Memory Core record as its own `ResourceRegistry` entry would work mechanically, but is not a narrow fix — it is a fundamental change to Memory Core's own Resource-representation choice (Errata 004's own deliberate design: Memory Core records are never Resource Registry entries), with unknown consequences for every other `MemoryRetrieval` consumer and for Resource Registry's own scale/lifecycle assumptions.
- Changing `DefaultPermissionPolicy`/`ActionMapper`'s own matching logic to special-case an empty `targetResources` would weaken the general mechanism these two frozen, already-verified Trust Framework components provide for every other proposal class in the system, not only Memory Core retrieval — a change of exactly the kind Chapter 10's own frozen status exists to prevent being made casually.

**Any of these is Contract-Design-tier work, requiring its own Planning Review, Boundary Review, and adversarial Independent Constitutional Review — not an implementation-tier patch.**

---

## 7. Requirements a Future Resolution Must Satisfy

Recorded here as constraints on the solution space, not as a designed solution. A future Contract Design pass addressing this blocker must, at minimum:

1. **Preserve genuine, per-record, fail-closed authorisation for retrieval as a general property.** Nothing here licenses making Memory Core retrieval broadly permissive again — Errata 004's own guarantee (no caller sees a record it is not authorised to see) must survive this resolution intact for every retrieval path unrelated to this blocker.
2. **Give a caller that already holds a lawfully, durably written Memory Core reference (obtained through an already-gated write it itself just performed, or was itself already authorised to read) a way for that specific resolution to succeed**, without that becoming a general bypass usable by an unrelated caller for an unrelated record.
3. **Introduce no second, parallel Permission Engine, and no shortcut around the one, shared `PermissionEngine`** — Chapter 10's own standing invariant.
4. **Not silently change behaviour for `EvidenceIntelligenceInputResolver`'s own existing, deliberately fail-closed retrieval path** (the `even a record that genuinely exists` denial `ParkerRuntimeEvidenceIntelligenceCompositionTest` already proves and relies upon) unless that change is itself explicitly, separately reasoned about and disclosed — this blocker's own resolution must not accidentally widen that specific, already-verified guarantee as a side effect.
5. **Be evaluated against, at minimum, three candidate directions** (none pre-selected, none designed by this document): (a) a narrow, caller-scoped exception allowing a principal to resolve a record it was itself just authorised to write, distinct from general retrieval authorisation; (b) extending Resource Registry to cover Memory Core records after all, with its own full cost/benefit analysis; (c) a distinct, new evaluation path for Knowledge Submission's own resolution step specifically, separate from `PermissionFilteredMemoryRetrieval`'s own general-purpose retrieval gate. A future Contract Design pass should treat this list as a starting point for investigation, not a shortlist to choose from without further reasoning.

---

## 8. Explicit Non-Responsibilities of This Document

This document does not: design a resolution mechanism; select among the candidate directions in Section 7; modify `DefaultPermissionPolicy`, `ActionMapper`, `PermissionFilteredMemoryRetrieval`, `DefaultKnowledgeCandidateEvaluator`, or any other Kotlin file; modify any frozen governance document; authorise any implementation to begin; or resolve the separate, disclosed `CandidateRecordProduced` gap named in Section 4's own table (recorded there for completeness, not investigated further here).

---

## 9. Recommended Next Step

A dedicated Trust Framework/Memory Core Contract Design pass, scoped specifically to this blocker, following the same Planning Review → Boundary Review → Contract Design → Scope Lock → Implementation Plan discipline already established throughout this repository's own governance history — beginning with the three candidate directions in Section 7 as its own starting material, not as pre-made decisions.

The Parker Conversational Memory Bridge's own second unit (conversational retrieval) should not begin until this blocker is resolved, since it would have no genuinely reachable success case to build or verify against.

---

## Final Report

**File created:** this document (`docs/architecture/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_GATING_BLOCKER.md`).

**Root cause:** traced to three frozen components acting together — `PermissionFilteredMemoryRetrieval`'s own "Memory Core records are never Resource Registry entries" design, `DefaultPermissionPolicy.evaluate`'s own resource-type resolution, and `ActionMapper.mapOne`'s own matching requirement — none individually defective, but their combination makes every retrieval through this decorator structurally, unconditionally denied.

**Blast radius:** Knowledge Memory's own promotion pipeline (confirmed blocked for every caller); Evidence Intelligence's own knowledge dispatch (blocked in principle, additionally gated by a separate, disclosed, unconfirmed-reachable issue); the Parker Conversational Memory Bridge, Admission Unit (confirmed blocked, live, end-to-end); any future Retrieval Unit (transitively blocked).

**Priority:** highest-priority open governance work, per Section 5.

**Narrow fix investigated and confirmed unavailable:** per Section 6.

Confirmed: no production code modified; no tests modified; no other governance document modified; nothing staged; nothing committed; nothing pushed; the Parker Conversational Memory Bridge, Admission Unit's own implementation remains uncommitted, unchanged from its own already-reviewed state.
