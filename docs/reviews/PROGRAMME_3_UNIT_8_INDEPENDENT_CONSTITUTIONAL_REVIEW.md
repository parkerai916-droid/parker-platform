# Programme 3 — Unit 8 Scope Lock Clarification — Independent Constitutional Review

## Status

**Final.** This document performs the independent constitutional review
`docs/governance/PROGRAMME_3_UNIT_8_SCOPE_LOCK_CLARIFICATION.md` ("the
Clarification") itself states it requires before it may be treated as
adopted. It does not amend the Clarification, Contract Design V2, the
Scope Lock, any Unit 4–7 Clarification, the Parker Constitution,
Epistemic Integrity, Memory Core's governance, Chapter 10, or CDR-005. It
identifies conflict, or its absence, only, and states a determination.
No Kotlin is implemented, proposed, or changed. No `src/` or `tests/`
file is modified. Nothing is staged, committed, or pushed by this
document.

Programme: **Programme 3 — Unit 8 Governance — Independent Constitutional
Review.**

---

## 1. Scope and Method

This review reads the Clarification in full against: the Parker
Constitution; Epistemic Integrity Amendment No. 1 (with particular
attention to Article XV); Contract Design V2, in full, including §2, §7,
§11, §12, and §16; the Scope Lock, in full, including §5, §6, §7, §10;
Memory Core Scope Lock §5, §6, §14; `docs/architecture/10-permission-engine.md`
("Chapter 10"), in full; `docs/decisions/CDR-005_CONSTITUTIONAL_ADMISSION_OF_PERMISSIONENGINE_PROPOSAL_CLASSES.md`
("CDR-005"), in full; the Unit 4, 5, 6, and 7 Scope Lock Clarifications,
in full; `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`, in full;
and the current production shapes of `src/interfaces/KnowledgeStore.kt`,
`src/interfaces/MemoryCore.kt`, `src/interfaces/PermissionEngine.kt`,
`src/runtime/DefaultEvidenceCustodian.kt`, and
`src/runtime/EvidenceRegistrationCoordinator.kt`. Every claim in the
Clarification checkable directly against code (for example, that
`KnowledgeCandidate` carries no identity field, or that no evaluator
implementation references `PermissionEngine`) was verified against the
current repository state, not accepted on the Clarification's own word.

The nine specific failure modes named by the governing task are each
tested as their own subsection below (Sections 3–11), followed by a
general consistency check against the broader corpus (Section 12).

---

## 2. Baseline Confirmation

At the time of this review, `HEAD` is `3c77bbdcd820b5983b1498e3ae1a1ef10b4b7860`
plus the one new file this task has added
(`docs/governance/PROGRAMME_3_UNIT_8_SCOPE_LOCK_CLARIFICATION.md`); the
working tree carries no other change. No production or test file is
touched by either the Clarification or this review.

---

## 3. Hidden Authority Expansion

**Test:** does the Clarification grant Knowledge Submission, Knowledge
Memory, or any subsystem any authority beyond what Contract Design V2 §7
and the Scope Lock §5 Deliverable 8 already named?

**Finding: none found.** Section 7 of the Clarification states explicitly,
"This document authorises no broader Knowledge Memory permission
framework than this one resource/action pair, for this one act," and
Section 4 exhaustively lists what Evaluation B does *not* govern
(evidence creation, candidate construction, truth determination,
retrieval, the four Unit 7 lifecycle acts, Evidence Intelligence's own
invocation gate, runtime composition). The Clarification introduces no
new `PermissionAction`, `ResourceType`, or `PermissionEngine` capability
(Section 5: "`PermissionEngine`'s own existing interface... is reused
entirely unchanged"). No wording anywhere in the Clarification grants
Knowledge Submission, or anything it calls, authority over Memory Core,
Evidence Intelligence, or any future Knowledge Memory act. Consistent
with the Constitution's own "Parker owns authority. Modules provide
capability" and "No module may grant itself authority."

---

## 4. Ownership Migration

**Test:** does the Clarification move any responsibility currently owned
by Memory Core, Evidence Custodian, Evidence Intelligence, or a closed
Programme 3 unit, to Knowledge Submission or Unit 8?

**Finding: none found.** Section 4 restates, without moving, Memory
Core's exclusive ownership of evidence creation and Evaluation A. Section
11 restates, without moving, Programme 4's exclusive ownership of the
Evidence Intelligence Acceptance Coordinator and its own invocation gate.
Section 4 restates, without moving, Unit 6's exclusive ownership of the
promotion/classification decision and Unit 7's exclusive ownership of
revision/retirement/restoration. Section 5 confirms `MemoryCore`/
`MemoryRetrieval`'s own frozen prohibition on holding `PermissionEngine`
is left exactly as Memory Core Scope Lock §6/§14 already froze it — the
Clarification does not touch, weaken, or generalise that prohibition; it
only observes that no equivalent prohibition exists for Knowledge
Submission, which is a different question (whether a prohibition exists
for a different contract), not a decision to remove one that does.

---

## 5. Candidate Mutation

**Test:** does the Clarification permit `KnowledgeCandidate` to be read,
mutated, enriched, or reinterpreted by Evaluation B before or during
permission evaluation?

**Finding: none found.** Section 8, step 2, confines pre-permission
structural checks to "the `KnowledgeCandidate`'s own shape and the
invariants its own type construction already guarantees," explicitly
forbidding any read of the Memory Core content the candidate's
`evidenceReference` names at that stage. Section 6 confirms the
requesting principal is supplied as a separate parameter, never written
onto or read from `KnowledgeCandidate`. No section proposes adding,
removing, or reinterpreting any field on `KnowledgeCandidate` (Unit 5's
own frozen field list — `evidenceReference`, `explicitlyRequested` — is
cited, never reopened, in Sections 6 and 10).

---

## 6. Evaluator Responsibility Expansion

**Test:** does the Clarification assign `KnowledgeCandidateEvaluator` (or
`KnowledgeRevisionEvaluator`/`KnowledgeRetirementEvaluator`) any
permission-related responsibility, or otherwise expand what Unit 6/7
already froze?

**Finding: none found; the opposite is affirmatively stated.** Section 5:
"`KnowledgeCandidateEvaluator` is explicitly and permanently barred from
holding, invoking, or referencing `PermissionEngine`, or from performing
any permission evaluation of any kind, under any circumstance." Section
14 restates this as a non-expansion for all three evaluators by name.
This was checked directly against the compiled evaluator sources: `grep`
against `src/runtime/DefaultKnowledgeCandidateEvaluator.kt`,
`DefaultKnowledgeRevisionEvaluator.kt`, and
`DefaultKnowledgeRetirementEvaluator.kt` for `PermissionEngine`,
`KnowledgeStore`, `.remember(`, and `InMemoryKnowledgeStore` returned zero
matches for `PermissionEngine` in all three files — the evaluators hold
no such dependency today, and the Clarification proposes none. Section 8,
step 5, confirms the evaluator is invoked "exactly once," "only after
Evaluation B has approved" — the evaluator is a downstream callee of the
permission decision, never a participant in making it.

---

## 7. Permission Bypass

**Test:** does the Clarification leave open any path by which a
`KnowledgeCandidate` could be evaluated by `KnowledgeCandidateEvaluator`
or persisted without Evaluation B having first approved?

**Finding: none found.** Section 8 fixes a strict order — request
received, structural shape check (no protected-content read), Evaluation
B, stop on any non-approval, evaluator invoked, persist only on
`Promote`. Step 4 is explicit: "no evaluator invocation, no Memory Core
read, and no persistence attempt of any kind occurs on this path" upon
denial. This mirrors, and is checked directly against,
`DefaultEvidenceCustodian.accept`/`.retrieve`'s own compiled behaviour
(both call `permissionEngine.evaluate` before any storage access and
return a `Rejected` result without touching storage on denial) and
`EvidenceIntelligenceInvocationGate`'s own "denial stops here" precedent
already frozen at `EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` §6 step 0. No
wording in the Clarification permits a caller to reach the evaluator or
persistence through any second path.

---

## 8. Ambient Identity

**Test:** does the Clarification permit the requesting principal to be
inferred, defaulted, or read from any implicit context rather than
supplied explicitly?

**Finding: none found.** Section 6 states plainly: "It must **not** be
carried inside `KnowledgeCandidate`... and it must **not** be assumed
from any ambient or implicit context... No hidden ambient identity
mechanism of any kind is introduced or authorised by this document." This
was checked against every existing precedent the Clarification cites —
`MemoryCore`'s five operations, `MemoryRetrieval`'s four direct-lookup
methods, and `EvidenceCustodian.accept`/`.retrieve` — each of which was
independently confirmed, by direct reading of
`src/interfaces/MemoryCore.kt` and `src/runtime/DefaultEvidenceCustodian.kt`,
to take a leading, caller-supplied `requestingPrincipalId` parameter with
no default value and no internal derivation. One distinct precedent
exists and was checked for contrast, not adopted: Unit 6 Scope Lock
Clarification §5 records that `DefaultKnowledgeCandidateEvaluator` uses
"its own fixed, named system-identity `PrincipalId` constant" for its own
*internal, audit-only* `MemoryRetrieval` reads — the Clarification does
not extend, generalise, or borrow that pattern for Evaluation B's own
principal, which must be the actual requester, not a system identity;
this distinction is drawn correctly and is not blurred anywhere in the
Clarification's text.

---

## 9. Legacy Knowledge Store Reuse

**Test:** does the Clarification permit `KnowledgeCandidate`,
Evaluation B, or Knowledge Submission's own persistence to route through,
adapt, or be co-classified with legacy `CandidateKnowledge`,
`KnowledgeRecord`, `KnowledgePromotionPolicy`, `DefaultKnowledgePromotionPolicy`,
or `InMemoryKnowledgeStore`?

**Finding: none found.** Section 14 names all five legacy types by name
as untouched and unauthorised for reuse. This was independently checked
against `src/runtime/InMemoryKnowledgeStore.kt`, read in full for the
preceding planning review and re-confirmed here: it implements only
`KnowledgeStore`/`KnowledgeSource` over `CandidateKnowledge`/
`KnowledgeRecord`, with zero reference anywhere in its text to
`KnowledgeCandidate`, `KnowledgeItem`, `KnowledgeCandidateEvaluator`, or
`PermissionEngine`. Nothing in the Clarification proposes closing that
gap, adapting one path into the other, or treating them as co-equal —
consistent with, and citing without reopening, Unit 5 Scope Lock
Clarification §1/§3 and `docs/reviews/PROGRAMME_4_UNIT_7_DEPENDENCY_RECORD.md`
§4's own identical prohibition.

---

## 10. Premature Runtime Composition

**Test:** does the Clarification wire, propose wiring, or presuppose a
specific wiring of Knowledge Submission into `src/composition/ParkerRuntime.kt`,
or otherwise perform Unit 10 / Programme 4 Unit 8 work?

**Finding: none found.** Section 4 and Section 11 both state runtime
composition is "not begun, addressed, or authorised by this document."
No section names a class, a constructor argument order, a composition-root
line, or a registration call. The Clarification's own repeated citation
of `ParkerRuntime.kt`'s *current* state (Section 5, citing
`EvidenceRegistrationCoordinator`'s three-dependency shape; the preceding
planning review's own confirmation that `InMemoryKnowledgeStore()` is
constructed today with no `PermissionEngine` wrapper) is used exclusively
as evidentiary precedent for *why* a given enforcement-location or
disclosed-identifier choice is well-founded — never as a proposal to
change that file. No `src/composition/` file is referenced as a target of
change anywhere in the Clarification.

---

## 11. Persistence of Denied or Declined Candidates

**Test:** does the Clarification permit any Knowledge Memory record —
of any kind, including an audit or attempt log within Knowledge Memory's
own constitutional boundary — to be created for a permission denial, a
malformed submission, or a substantive decline?

**Finding: none found.** Section 8, steps 4 and 7, and Section 10 (first
three bullets) each independently state that no Knowledge Memory record
is created on any of these three paths. Section 10 additionally reaffirms
Contract Design V2 §3's own text verbatim ("A candidate that is not
promoted produces no Knowledge Item and no Knowledge Memory record at
all") as the basis for treating this as an affirmative prohibition, not
mere silence — consistent with the preceding planning review's own
identical reading. The Clarification does not introduce, or leave room
for, a separate "submission-attempt log" or equivalent record within
Knowledge Memory's own boundary; Section 9's new result type is expressly
scoped to representing an outcome to the *caller* of a single, already-
completed call, never to a stored record.

---

## 12. General Consistency Check

- **Parker Constitution.** "Cognition proposes, trust authorises, runtime
  executes" is preserved: Knowledge Submission proposes nothing of its
  own; Evaluation B is the trust-authorises stage, performed before any
  evaluation or persistence; nothing in the Clarification allows
  cognition or a reasoning provider to bypass it. "No capability may
  bypass trust" is preserved by Section 8's own strict ordering.
- **Epistemic Integrity, Article XV.** No subsystem determines its own
  evidential status; the Clarification does not touch evidential-state
  assignment at all — that remains Unit 6's own, unmodified
  responsibility.
- **Contract Design V2.** §7 (Amendment 8), §11, and §12's own Knowledge
  Submission row are each cited accurately and are not contradicted.
  §16.5's "the same consumer already established for the other five
  named promotion factors" pattern (naming an act's sole authorised
  consumer) is mirrored, not violated, by Section 5's own "Evaluation B
  is Knowledge Submission's own, exclusive responsibility" statement.
- **Scope Lock.** §5 Deliverable 8, §6 (Article XV), and §10 (Out-of-
  Scope Change Policy) are each satisfied: the Clarification introduces
  no new subsystem responsibility beyond what §3/§5 already named, and
  changes no public contract Contract Design V2 already froze.
- **Memory Core boundaries.** §5, §6, §14 (Memory Core Scope Lock) are
  cited accurately in Section 5 of the Clarification and are not
  reopened, weakened, or generalised beyond their own text.
- **Permission Engine architecture (Chapter 10).** §3 (Proposal
  Abstraction), §8 (Domain Consumer Relationship), and §10
  (Extensibility) are each applied, not reopened; the Clarification's own
  self-certification (its Section 12) follows Chapter 10 §10's own
  required form (domain defines the act; domain self-certifies against
  published criteria; escalate only if contested) exactly.
- **Units 4–7 Clarifications.** No conflict found against any of the
  four; each is cited only for its own, already-settled content (Unit 5's
  field list; Unit 6's principal-identity precedent and its own "no
  independent permission wiring" principle; Unit 7's classification of
  its four lifecycle acts as non-gated, cited by contrast).
- **Evidence Intelligence boundaries.** `EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`
  §6/§11 is cited as methodological precedent for the CDR-005
  self-certification form and for the disclosed-but-unregistered
  identifier convention; the Clarification does not reference, depend
  upon, or alter Evidence Intelligence's own invocation gate, and does
  not authorise Evidence Intelligence, or its Acceptance Coordinator, to
  do anything beyond what `PROGRAMME_4_UNIT_7_DEPENDENCY_RECORD.md`
  already establishes.

**No conflict found anywhere in this review.**

---

## 13. Determination and Recommendation

**Determination.** No constitutional, governance, or scope change is
required to the Clarification before it may be treated as adopted. It is
consistent with the Parker Constitution, Epistemic Integrity Amendment
No. 1, Contract Design V2, the Scope Lock, Memory Core's frozen
boundaries, Chapter 10, CDR-005, and all four prior Unit Clarifications.
Each of the nine specific failure modes named by the governing task was
tested individually and none was found. The one disclosed limitation —
that Chapter 10 and CDR-005 themselves remain Draft, and that this
Clarification is the second, disclosed instance of this project relying
on CDR-005's mechanism before its own freeze completed — is candidly
stated in the Clarification's own Section 13 and does not, on the
evidence reviewed, constitute a defect in the Clarification itself; it is
a property of the governance it depends upon, already present and already
disclosed before this review began.

**Recommendation.**

1. The Clarification is fit for adoption as drafted, without amendment.
2. Its own status header and Disposition section should be updated to
   reflect adoption, citing this review as the disclosed basis — mirroring
   the precedent `docs/decisions/CDR-006_CONSTITUTIONAL_CLASSIFICATION_OF_ORIGINAL_EVIDENCE_CUSTODY_AND_IMMUTABILITY.md`
   already followed after its own Final Freeze Review.
3. CDR-005 and Chapter 10 should **not** be updated to a frozen status as
   part of this task. Both remain genuinely Draft; the only work this
   task performed is a second, disclosed instance of relying on their
   drafted mechanism, not a resolution of their own outstanding freeze
   condition, which requires a separately-scoped review of every domain
   that has applied Model C so far (Evidence Intelligence, and now
   Knowledge Memory) against the full corpus, following
   `EVIDENCE_CUSTODIAN_CDR-006_FINAL_FREEZE_REVIEW.md`'s own scope and
   method. Converting either to a false "Final"/"Frozen" status here would
   misrepresent work not actually performed.
4. `docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md`
   §3's own Unit 8 entry may receive a small, acceptance-tracking note
   recording that Unit 8's Evaluation B constitutional treatment is now
   settled by the adopted Clarification, without altering that entry's own
   objective, dependency, or verification text.
5. This review does not alter the sequencing conclusion of the preceding
   planning review: Unit 8 implementation may now proceed on the
   Evaluation B dimension; groundwork not dependent on it (the internal
   storage contract, evaluator-invocation wiring, non-persistence-on-decline
   discipline) may proceed in parallel, exactly as that review already
   recommended.

---

## Final Report

**Document reviewed:** `docs/governance/PROGRAMME_3_UNIT_8_SCOPE_LOCK_CLARIFICATION.md`.

**Outcome:** No constitutional, governance, or scope change required.
Nine specific failure modes tested individually; none found. One
candidly disclosed limitation (CDR-005/Chapter 10's own Draft status)
carried forward from the Clarification's own text, not a defect this
review introduces or discovers anew. Recommendation: adopt.

PROGRAMME 3 UNIT 8 GOVERNANCE — INDEPENDENT CONSTITUTIONAL REVIEW —
COMPLETE

Confirmed: no Kotlin implemented, proposed, or changed; no test
modified; the Parker Constitution, Epistemic Integrity, Contract Design
V2, the Scope Lock, every Unit 4–7 Clarification, Memory Core's
governance, Chapter 10, and CDR-005 all unmodified by this review;
nothing staged; nothing committed; nothing pushed by this review.
