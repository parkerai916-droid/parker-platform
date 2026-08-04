# Evidence Intelligence — Unit 7 (Acceptance Coordination) — Planning and Boundary Review

## Status

**Governance analysis only.** No Kotlin is implemented, proposed, or changed. No governance document is modified. Neither `src/` nor `tests/` is touched. Nothing is staged, committed, or pushed. Repository baseline: `HEAD 7a62fac` ("feat: implement evidence intelligence unit 6"), working tree clean, confirmed before this review began and unchanged throughout it.

Purpose: determine exactly what Unit 7 ("The Evidence Intelligence Acceptance Coordinator") owns, and equally, what it does not own, before any Kotlin is written for it.

---

## 1. Exact Files Reviewed

**Governance, read in full:** `docs/architecture/parker-constitution.md`; `docs/architecture/reasoning-context.md`; `docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md`; `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`; `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`; `docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md`. Verified via `git log` that none of these changed since they were last read in full in this Unit's own governance-first workflow — all still at the same commits.

**Implementation, read in full:** `src/interfaces/EvidenceIntelligence.kt`; `src/runtime/DefaultEvidenceIntelligence.kt`; `src/runtime/EvidenceIntelligenceInputResolver.kt`; `src/runtime/EvidenceIntelligenceReasoningCoordinator.kt`; `src/runtime/AnalyticalOutputDiscipline.kt`; `src/runtime/EvidenceIntelligenceInvocationGate.kt`; `src/runtime/EvidenceRegistrationCoordinator.kt` (in full, both `EvidenceCustodian.accept`-gating and `MemoryCore`-gating halves); `src/interfaces/EvidenceCustodian.kt` (all four Units — identity, acceptance, retrieval, deletion); `src/interfaces/MemoryCore.kt` (in full — every record type, `MemoryCore`, `MemoryRetrieval`); `src/interfaces/KnowledgeStore.kt` (in full — legacy `KnowledgeStore`/`CandidateKnowledge` path and the constitutional `KnowledgeCandidate`/`KnowledgeItem`/`KnowledgeCandidateEvaluator` path); `src/composition/ParkerRuntime.kt` (grepped for every reference relevant to Evidence Custodian and Evidence Intelligence wiring); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` (Contract Inventory, §12–§13, for the "Knowledge Submission (interface)" row). Also inspected: `src/runtime/CommunicationConversationCoordinator.kt`, `src/runtime/ConversationTurnReasoningCoordinator.kt` (coordinator-shape precedent), and confirmed by repository-wide grep that no `PermissionGatedMemoryCore`, `PermissionFilteredMemoryRetrieval`, or Knowledge-Candidate-persisting "Knowledge Submission" implementation exists anywhere in `src/`.

---

## 2. Exact Responsibilities

Traced to governing clause, no restatement invented:

| # | Responsibility | Source |
|---|---|---|
| 1 | Consume the `EvidenceAnalysisResult` list Unit 5's own operation returns for one invocation — "never a reconstructed, filtered, re-ordered, or otherwise reinterpreted copy" | Implementation Plan §8 Unit 7 |
| 2 | Dispatch each candidate to the one existing acceptance interface its own kind already names: `EvidenceCustodian.accept` for `CandidateArtifactProduced`; `MemoryCore`'s write interface for `CandidateRecordProduced`; Knowledge Memory's Knowledge Submission interface for `CandidateKnowledgeProduced` | Scope Lock §6 step 4; Implementation Plan §6 item 5, §8 Unit 7 |
| 3 | Accept a proposed Memory Core record (via `MemoryCore`'s write interface) **before** submitting any Knowledge Candidate that references it — the coordinator's own binding obligation | Scope Lock §6 step 4/5; Contract Design §5, §6 |
| 4 | Pass every acceptance call through the Permission Engine gate that interface's own existing contract already enforces — "cannot bypass Permission Engine evaluation" | Scope Lock §6 step 4 |
| 5 | Remain stateless — no candidate, outcome, or identifier retained across candidates or invocations | Implementation Plan §8 Unit 7 ("How it avoids retaining accepted candidates") |
| 6 | Represent each candidate's outcome as exactly one of the accepting subsystem's own, already-existing dispositions (acceptance / structural rejection / implementation fault) — never a new shape | Implementation Plan §8 Unit 7 |
| 7 | For a Knowledge Candidate submission specifically, confirm (defensively) that the Memory Core record it references already carries a governed identifier before submitting — never a silent proceed if that check ever fails | Implementation Plan §8 Unit 7 |

No responsibility beyond this table is authorised anywhere in the governing stack.

---

## 3. Exact Boundaries

Answering each named question directly, against frozen text:

| Question | Answer | Basis |
|---|---|---|
| Owns acceptance? | **No.** The three accepting subsystems (Evidence Custodian, Memory Core, Knowledge Memory) each own acceptance/custody/promotion within their own domain; the coordinator "owns no custody, truth, promotion, deletion, or evidential-state authority" | Scope Lock §6 step 4 |
| Owns orchestration only? | **Yes, exactly.** "A sequencing mechanism only... holds no capability beyond sequencing calls to interfaces that are already, independently, fully governed" | Scope Lock §6 step 4; Contract Design §6 |
| Owns `KnowledgeCandidate` construction? | **No.** Construction is Evidence Intelligence's (Unit 5's) own, exclusive responsibility; the coordinator only "submits... never constructs one" | Contract Design §1; Implementation Plan §6 item 5 |
| Owns acceptance ordering? | **Partially — the ordering constraint only, not the scheduling decision.** It owns the binding rule "accept the Memory Core record before submitting any Knowledge Candidate naming it," enforced as its own defensive check. It does **not** own the decision of *whether and when* a second Evidence Intelligence invocation constructs that Knowledge Candidate at all — that is "an ordinary scheduling decision for whatever composes Evidence Intelligence into the running system," i.e. Unit 8's own concern | Scope Lock §6 step 4/5; Implementation Plan §8 Unit 5, Unit 7 |
| Owns retry? | **No.** "Does not retry silently and does not substitute a fabricated acceptance" | Implementation Plan §8 Unit 7 |
| Owns batching? | **No.** Processes exactly one already-produced result list per invocation; no cross-invocation aggregation is named or authorised anywhere |  Implementation Plan §8 Unit 7 |
| Owns provenance? | **No.** Evidence Intelligence "creates no independent provenance model" (Contract Design §8), and neither does this coordinator — every provenance reference a dispatched candidate carries is Memory Core's own, unmodified field, untouched by the coordinator | Contract Design §8 |
| Owns Memory Core writes? | **No — it invokes, never owns.** `MemoryCore`'s write interface remains Memory Core's own exclusive contract; the coordinator calls it, exactly as any other caller does | Scope Lock §4; `src/interfaces/MemoryCore.kt` |
| Owns Evidence Custodian? | **No.** Custody, immutability, and deletion remain Evidence Custodian's exclusive domain (CDR-006; CDR-007 §5); the coordinator calls `accept`, nothing more |
| Owns runtime composition? | **No.** Wiring the coordinator into `ParkerRuntime` is Unit 8's own, separate responsibility | Implementation Plan §7, §8 Unit 8 |

---

## 4. Dependencies Entering Unit 7

Exactly one: the `List<EvidenceAnalysisResult>` that Unit 5's `EvidenceIntelligence.analyse` operation returns for one invocation, consumed "exactly as the Evidence Intelligence operation... returned it... never a reconstructed, filtered, re-ordered, or otherwise reinterpreted copy" (Implementation Plan §8 Unit 7). No other input reaches the coordinator — it does not itself accept an `EvidenceAnalysisRequest`, a `PrincipalId` beyond what a candidate's own referenced records already carry, or any Unit 6 output (the invocation gate is upstream of Unit 5 entirely, and is never itself an input to Unit 7 — confirmed structurally: `EvidenceIntelligenceInvocationGate` is never imported by, or referenced from, `EvidenceRegistrationCoordinator`, and no governance document names it as a Unit 7 dependency).

## 5. Dependencies Leaving Unit 7

Exactly the three acceptance interfaces the Scope Lock (§4) and Implementation Plan (§5, §8 Unit 7) name, and no more:

1. **`EvidenceCustodian.accept`** — for `EvidenceAnalysisResult.CandidateArtifactProduced`. **Exists, implemented, already self-gating** (`DefaultEvidenceCustodian.accept` calls `PermissionEngine.evaluate` internally, confirmed by direct inspection). No additional gate is required from the coordinator for this leg.
2. **`MemoryCore`'s public write interface** (`createAssertion`/`createRelationship`, dispatched per `CandidateMemoryCoreRecord.OfAssertion`/`OfRelationship`) — for `EvidenceAnalysisResult.CandidateRecordProduced`. **Exists, implemented — but does not, and by constitutional design never will, self-gate.** `MemoryCore`'s own interface KDoc states this in terms identical to Memory Core Scope Lock §6: "No operation here accepts or evaluates a permission decision — Memory Core never authorises itself; permission evaluation is entirely external, applied by a future decorator (`PermissionGatedMemoryCore`, not implemented by this Unit)." Repository-wide grep confirms `PermissionGatedMemoryCore` is mentioned only in KDoc/governance prose — no such class exists anywhere in `src/`. **This is exactly the situation `EvidenceRegistrationCoordinator` already solved for its own `createProvenance`/`registerDocument` calls**, by holding its own `PermissionEngine` reference and gating those two calls itself, with its own disclosed-but-unregistered `ResourceId`/action-name conventions. Unit 7's coordinator will need to do the same for this leg.
3. **Knowledge Memory's Knowledge Submission interface** — for `EvidenceAnalysisResult.CandidateKnowledgeProduced`. **Does not exist anywhere in this repository.** See Finding 1, below — this is the central blocker this review identifies.

No dependency beyond these three (plus, for the second leg, the `PermissionEngine` the coordinator must hold itself, mirroring `EvidenceRegistrationCoordinator`) is authorised. Confirmed by direct inspection that the coordinator holds, and must hold, no dependency on `OwnerEvidenceDeletionAuthority`, `EvidenceArtifactStorage.delete`, `EvidenceDeletionAudit`, or any Knowledge Memory promotion/revision/retirement/restoration mechanism (`KnowledgeCandidateEvaluator`'s sibling seams, `KnowledgeRevisionEvaluator`/`KnowledgeRetirementEvaluator`, are explicitly out of reach — Scope Lock §4).

---

### Finding 1 — Knowledge Memory's "Knowledge Submission (interface)" does not exist

Both the Evidence Intelligence Scope Lock (§4: "Depended upon only by the Evidence Intelligence acceptance coordinator... Knowledge Memory's Knowledge Submission interface") and Implementation Plan (§5, §8 Unit 7) treat this as an already-existing, already-governed dependency the coordinator simply calls — exactly as they correctly treat `EvidenceCustodian.accept` and `MemoryCore`'s write interface. Programme 3's own Contract Design (`PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §12) does name "Knowledge Submission (interface)" as an approved contract — "the single public path through which a Knowledge Candidate is submitted," gated by its own "Evaluation B" (§7) — but that same document's own §13 ("Out of Scope") states plainly that "implementation, persistence... are all Scope Lock or implementation decisions," not decided by the Contract Design itself.

Direct inspection of `src/` confirms Programme 3 has not yet built it. The only production code touching the constitutional `KnowledgeCandidate`/`KnowledgeItem` model is `KnowledgeCandidateEvaluator`/`DefaultKnowledgeCandidateEvaluator` — a pure **evaluation** seam whose own KDoc states, without qualification: "This interface performs no persistence... nothing implementing this type writes `item` or `promotion` anywhere; persistence remains a later, separately authorised responsibility." There is no `submit`-shaped operation, no permission gate for it (Evaluation B is named in governance but implemented nowhere), and no durable store for a promoted `KnowledgeItem` anywhere in this repository. (The unrelated, legacy `KnowledgeStore.remember` operates on `CandidateKnowledge`/`KnowledgeRecord` — a different model entirely, explicitly not adapted into the constitutional path, per `PROGRAMME_3_UNIT_5_SCOPE_LOCK_CLARIFICATION.md` §1.)

**Consequence for Unit 7:** the coordinator cannot be built to completion today. Its own frozen completion criterion — "every candidate Unit 5 can produce reaches its accepting subsystem only through the coordinator, only through that subsystem's own unmodified acceptance interface" (Implementation Plan §8 Unit 7) — cannot be satisfied for `CandidateKnowledgeProduced`, because no acceptance interface exists for it to route through. This is not a textual ambiguity resolvable by re-reading the frozen documents more carefully (as several prior Unit 6 governance questions were); it is a missing, cross-programme implementation dependency. Whether that future interface will itself self-gate (like `EvidenceCustodian.accept`) or require external gating by the coordinator (like `MemoryCore`'s write interface) is presently unknowable, since it has not been designed.

### Finding 2 — The `MemoryCore`-write leg requires the coordinator to hold its own `PermissionEngine`, mirroring precedent exactly

Distinguished from Finding 1: this is **not** a blocker, because a clean, already-established precedent resolves it — but the Scope Lock's own phrasing ("the Permission Engine gate each of those three already enforces," §4) risks misleading a future implementer into assuming all three legs are already self-gating, when `MemoryCore`'s write interface constitutionally never will be (Memory Core Scope Lock §6: "Memory Core never evaluates permissions... No contract implementing `MemoryCore`... may hold a `PermissionEngine` reference"). `EvidenceRegistrationCoordinator` already had, and solved, this identical problem for its own two `MemoryCore` calls (`createProvenance`, `registerDocument`) by holding a `PermissionEngine` reference itself — "the one documented, load-bearing reason this coordinator's dependency list differs from every other existing coordinator's." Unit 7's coordinator must do the same for its `CandidateRecordProduced` leg, with its own disclosed-but-unregistered `ResourceId`/action-name pair, exactly mirroring that precedent. This does not require a Scope Lock amendment — it requires correctly applying Memory Core's own, already-frozen Scope Lock §6 — but it should be stated explicitly so it is not silently missed.

---

## 6. Comparison With Every Existing Coordinator

| Coordinator | Holds `PermissionEngine`? | Why | Shape |
|---|---|---|---|
| `EvidenceRegistrationCoordinator` | **Yes** | One of its two callees, `MemoryCore`, cannot self-gate | Concrete class, sequences `EvidenceCustodian.accept` (self-gated) → `MemoryCore.createProvenance` (coordinator-gated) → `MemoryCore.registerDocument` (coordinator-gated); returns its own `EvidenceRegistrationOutcome` |
| `ConversationTurnReasoningCoordinator` | No | Both callees (`ConversationEngine`, `ReasoningProvider`) need no gating for this sequencing | Concrete class, no interface |
| `CommunicationConversationCoordinator` | No | Same reason | Concrete class, no interface |
| **Unit 7 (this review)** | **Yes, for the same reason as `EvidenceRegistrationCoordinator`** | `MemoryCore`'s write interface cannot self-gate; `EvidenceCustodian.accept` already does; Knowledge Memory's own gate is undesigned | Concrete class (frozen by Implementation Plan §8 Unit 7: "concrete, non-interface-backed... An interface here would itself be the new public type the Scope Lock's own... freeze already excludes") |

**Conclusion: Unit 7 follows an existing architectural precedent — `EvidenceRegistrationCoordinator`'s own shape — almost exactly.** It is not a genuinely new coordination pattern. The one respect in which it cannot yet fully mirror that precedent is Finding 1: `EvidenceRegistrationCoordinator` sequences two subsystems that both already exist and are both reachable (one self-gated, one coordinator-gated); Unit 7 must sequence three, one of which (Knowledge Memory's submission boundary) does not yet exist to be sequenced at all.

---

## 7. Public Contract

- **New public runtime type?** The coordinator itself will be a new class, but — mirroring exactly the reasoning already accepted for Unit 6's own `EvidenceIntelligenceInvocationGate` and stated directly by governance for this Unit — it does **not** count against Evidence Intelligence's own four-type ceiling (`EvidenceAnalysisRequest`, `EvidenceAnalysisResult`, the payload selector, `EvidenceIntelligence`), because it is a composition-level mechanism outside Evidence Intelligence's own model, never referenced from any of those four types. Scope Lock §4/§10 states this explicitly: "not a new public type... its own concrete Kotlin shape... its literal Kotlin name and internal algorithm remain out of scope regardless" (name and internals only — the six frozen properties are already decided).
- **New interface?** **No — explicitly forbidden.** "Concrete, non-interface-backed... An interface here would itself be the new public type the Scope Lock's own 'no fifth new public type or interface' freeze already excludes" (Implementation Plan §8 Unit 7).
- **Reuses existing acceptance outcomes?** **Yes, exclusively.** "The observable outcome for that candidate is exactly whichever of three already-existing possibilities occurred, each expressed through that accepting subsystem's own, already-existing, unmodified contract — never a new shape this plan introduces" (Implementation Plan §8 Unit 7): `EvidenceAcceptanceResult` (Evidence Custodian), whatever `MemoryCore`'s write interface returns or throws, and whatever Knowledge Memory's (not-yet-built) submission interface will eventually return or throw.
- **Requires a Contract Design amendment?** **No.** The Contract Design already anticipated and authorised this coordinator's existence and shape (§6, §12).
- **Requires a Scope Lock amendment?** **No, for the coordinator's own shape** (already frozen, §6 step 4, §10). The `PermissionEngine`-holding requirement for the `MemoryCore` leg (Finding 2) is a correct application of Memory Core's own, separately frozen Scope Lock, not a change to Evidence Intelligence's Scope Lock. Finding 1 is not resolved by amending Evidence Intelligence's own Scope Lock either — it requires Programme 3 to build the missing interface, a different governance track entirely.

---

## 8. Ownership Verification

| Type/interface | Owner | Unit 7's relationship |
|---|---|---|
| `KnowledgeCandidate` | Evidence Intelligence (Unit 5), for construction; Knowledge Memory, once submitted | Unit 7 submits an already-constructed value; never constructs one |
| `EvidenceCustodian.accept`, `MemoryCore`'s write interface, Knowledge Memory's Knowledge Submission interface | Evidence Custodian; Memory Core; Knowledge Memory, respectively | Unit 7 invokes each; owns none |
| Accepted records (`AcceptedEvidenceArtifact`, `Assertion`/`Relationship`, a promoted `KnowledgeItem`) | The accepting subsystem, permanently, from the moment of acceptance (Contract Design §5's "ownership transfers on acceptance, and only on acceptance") | Unit 7 retains no reference, modification right, or residual claim after dispatch |
| Memory Core writes | Memory Core | Unit 7 is a caller, holding its own gate for this specific call only, as Finding 2 requires |
| Evidence Intelligence's own responsibility boundary | Evidence Intelligence (Units 1–5) | Ends at returning the `EvidenceAnalysisResult` list (Contract Design §10, §14); Unit 7 begins only after that return, and is never called by, nor calls, `EvidenceIntelligence` itself (Scope Lock §4: no reference in either direction) |

---

## 9. Implementation Sequence

Beginning immediately after Unit 6's successful completion, ending immediately before Unit 8:

1. **Confirm Unit 6 is complete** (already true at this baseline — `HEAD 7a62fac`).
2. **Resolve Finding 1** — a prerequisite, not a Unit 7 sub-step: Programme 3 designs and implements a real "Knowledge Submission (interface)" with genuine persistence and its own Evaluation B gate (or an explicit, disclosed decision to gate it externally, mirroring `MemoryCore`'s own pattern). This is cross-programme work, outside Evidence Intelligence's own governance stack, and is not itself "Unit 7."
3. **Define the coordinator's concrete shape** — a single, concrete, non-interface-backed class, dependencies: `EvidenceCustodian`, `MemoryCore`, `PermissionEngine` (for the `MemoryCore` leg, per Finding 2), and whatever concrete type implements the now-real Knowledge Submission interface from step 2.
4. **Implement dispatch for `CandidateArtifactProduced`** → `EvidenceCustodian.accept` (already self-gated; no additional permission logic needed here).
5. **Implement dispatch for `CandidateRecordProduced`** → `MemoryCore.createAssertion`/`createRelationship`, gated by the coordinator's own `PermissionEngine.evaluate` call, using a fresh, disclosed-but-unregistered `ResourceId`/action-name pair (mirroring `EvidenceRegistrationCoordinator.MEMORY_CORE_PROVENANCE_RESOURCE_ID`/`CREATE_PROVENANCE_ACTION_NAME`'s own convention, distinct literal values).
6. **Implement dispatch for `CandidateKnowledgeProduced`** → the now-real Knowledge Submission interface from step 2, including the defensive "governed identifier already exists" check (Implementation Plan §8 Unit 7).
7. **Implement the three-outcome representation** (acceptance / structural rejection / implementation fault) per candidate, using each accepting subsystem's own existing disposition shape — no new type.
8. **Verify statelessness and dependency-reachability structurally** (mirroring the reflection-style tests already used for `EvidenceIntelligenceReasoningCoordinator`/`EvidenceIntelligenceInvocationGate`): no reference to `EvidenceIntelligence`/`DefaultEvidenceIntelligence` in either direction; no reference to `OwnerEvidenceDeletionAuthority`, `EvidenceArtifactStorage`, `EvidenceDeletionAudit`, or any Knowledge Memory promotion/revision/retirement/restoration mechanism.
9. **Stop.** Unit 7's own contract ends here — runtime composition (registering the coordinator, resolving the accept-before-submit-reference sequencing at the composition-root level, wiring the second Evidence Intelligence invocation Implementation Plan §8 Unit 5 describes) is Unit 8's own, separate responsibility, not begun by Unit 7.

---

## Boundary Review — Final Verdict

**B — Governance return required.**

Precisely why, and why not the more familiar kind of "return": every prior Evidence Intelligence governance question this Programme has escalated (the invocation-gating pairing, denial representation) was a *textual* ambiguity — the frozen documents were silent or imprecise, and rereading them, or a short clarification, closed the gap. This one is different in kind. The Scope Lock and Implementation Plan are not ambiguous about what Unit 7 must do with Knowledge Memory's Knowledge Submission interface — they are simply **wrong about the present state of the repository**: they assume, and are written as though, all three acceptance interfaces the coordinator must call already exist and already self-gate. Direct inspection shows one does not exist as an interface at all (Finding 1), and a second, while it exists, constitutionally never self-gates and requires the coordinator to hold its own `PermissionEngine` reference — correctly resolvable by precedent (`EvidenceRegistrationCoordinator`), but easy to miss given the Scope Lock's own phrasing (Finding 2).

Unit 7 cannot be completed today. Two of its three acceptance legs are implementable now, following `EvidenceRegistrationCoordinator`'s own precedent exactly; the third has no interface to route through, and building the coordinator without it would leave one of Evidence Intelligence's own four already-frozen output categories (`CandidateKnowledgeProduced`) permanently unreachable — a real gap, not a hypothetical one, since that type has compiled and been constructible since Unit 1.

The return required is narrow and specific: **not** a rewrite of the Evidence Intelligence Scope Lock or Implementation Plan (their own text about Unit 7's shape, dependencies, and ownership is otherwise correct and sufficient, confirmed throughout §§2–8 above), but a cross-programme readiness dependency — Programme 3 must design and implement a real "Knowledge Submission (interface)" (already named and approved in its own Contract Design, just not yet built) before Unit 7 can be completed in full. Until then, Unit 7 may only be partially built (the `EvidenceCustodian`/`MemoryCore` legs), which does not satisfy its own frozen completion criteria.

---

## 10. Confirmation No Other File Changed

No governance document, production source file, or test file was modified. This review document is the only file created by this task.

## 11. Confirmation No Git Actions

Nothing staged, committed, or pushed. Only read-only `git log`/`git status` commands were run, to confirm the baseline and confirm no governance document had changed since it was last read in full.
