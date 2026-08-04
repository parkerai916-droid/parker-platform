# Unit 6 — Invocation Permission Denial Representation Study

## Status

**Governance study only.** No Kotlin is implemented, proposed, or changed. No
governance document is modified. Neither `src/` nor `tests/` is touched.
Nothing is staged, committed, or pushed. This document investigates
exactly one question left open by
`docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_6_PLANNING_AND_BOUNDARY_REVIEW.md`'s
own Section 6/Section 9/Section 13 findings, and no other question.

---

## 1. The Precise Unresolved Question

Every existing Parker component that is gated by `PermissionEngine.evaluate`
gates itself **inside the same operation being gated** — the permission
check and the domain action share one function, so "what happens on
denial" is answered by a branch of that function's own, already-existing
return type. Unit 6's invocation gate cannot take that shape: the Evidence
Intelligence Scope Lock (§4, §9) and Contract Design (§12) forbid
`EvidenceIntelligence`/`DefaultEvidenceIntelligence` from holding a
`PermissionEngine` reference of any kind, so the gate must sit **outside**
`EvidenceIntelligence.analyse`, at whatever future call site composes
Evidence Intelligence into the running system (Scope Lock §6 step 0: "This
evaluation is performed by whatever composes Evidence Intelligence into
the running system"). That call site is Unit 8's own, not-yet-designed
composition work.

`EvidenceIntelligence.analyse`'s own return type,
`List<EvidenceAnalysisResult>`, cannot represent denial: `EvidenceAnalysisResult`
is sealed to exactly four variants, all of them genuine analytical output
(Contract Design §5; Scope Lock §3, §4), and Implementation Plan §10 items
3 and 12 already forbid an empty list, a fifth variant, or any wrapper
from ever standing in for denial or failure.

**The precise question:** given that (a) `analyse`'s own return type
structurally cannot carry a denial outcome, and (b) no other type exists
yet at the layer where the gate must actually run (Unit 8's composing
caller, not yet built), **what does the composing caller do, and what does
it return or signal, when the invocation-gating Permission Engine
evaluation does not produce `APPROVED`/`APPROVED_WITH_CONFIRMATION`?**

---

## 2. Existing Precedent Catalogue

Every `PermissionEngine.evaluate` call site in the repository, inspected
directly:

### 2.1 `DefaultEvidenceCustodian.accept` (`src/runtime/DefaultEvidenceCustodian.kt:87-116`)

- **Location/caller:** inside `accept` itself, first statement.
- **Return type:** `EvidenceAcceptanceResult` (existing, pre-dates this
  gate).
- **Behaviour on `DENIED`:** returns `EvidenceAcceptanceResult.Rejected(reason)`.
  `storage.write` never called.
- **Behaviour on `DEFERRED`:** identical — the code tests
  `decision.decision != APPROVED && decision.decision != APPROVED_WITH_CONFIRMATION`,
  a single combined branch. `DEFERRED` produces the same
  `Rejected(reason)` value, with `decision=DEFERRED` interpolated into the
  reason string only.
- **Downstream unreachable?** Yes — `EvidenceArtifactId` is never minted,
  `storage.write` is never called.

### 2.2 `DefaultEvidenceCustodian.retrieve` (same file, `:150-178`)

- **Location/caller:** inside `retrieve` itself, first statement.
- **Return type:** `EvidenceRetrievalResult`.
- **`DENIED`:** `EvidenceRetrievalResult.Rejected(evidenceArtifactId, reason)`.
- **`DEFERRED`:** identical combined branch; same `Rejected` value.
- **Downstream unreachable?** Yes — `storage.read` never called.

### 2.3 `EvidenceRegistrationCoordinator.register` — two evaluations (`src/runtime/EvidenceRegistrationCoordinator.kt:250-293`)

- **Location/caller:** inside `register`, a composition-level coordinator
  that holds `PermissionEngine` specifically because `MemoryCore` cannot
  self-gate (Memory Core Scope Lock §6).
- **Return type:** `EvidenceRegistrationOutcome` (a sealed type this
  coordinator itself defines, mirroring exactly the shape this study
  evaluates as Candidate 5 below).
- **`DENIED`:** first evaluation (`createProvenance`) →
  `EvidenceRegistrationOutcome.ProvenanceNotAuthorised(acceptedEvidenceArtifact, reason)`;
  second (`registerDocument`) →
  `EvidenceRegistrationOutcome.DocumentRegistrationNotAuthorised(acceptedEvidenceArtifact, provenance, reason)`.
- **`DEFERRED`:** identical combined-branch treatment as 2.1/2.2 — same
  variant, `decision=DEFERRED` only in the interpolated reason string.
- **Downstream unreachable?** Yes — `memoryCore.createProvenance`/
  `registerDocument` never called past the point of denial; the coordinator
  never re-decides Evidence Custodian's own denial either (§ "Two separate
  permission decisions" KDoc).

### 2.4 `DefaultOwnerEvidenceDeletionAuthority.deleteAsOwner` (`src/runtime/DefaultOwnerEvidenceDeletionAuthority.kt:100-113`)

- **Location/caller:** inside `deleteAsOwner`, first statement.
- **Return type:** `EvidenceDeletionResult`.
- **`DENIED`/`DEFERRED`:** identical combined branch →
  `EvidenceDeletionResult.Rejected(reason)`. `evidenceDeletionAudit.record`
  and `storage.delete` never called.
- **Downstream unreachable?** Yes.

### 2.5 `DefaultExecutionPipeline.submit` (`src/runtime/DefaultExecutionPipeline.kt:150-190`)

- **Location/caller:** inside `submit`, after resource/action resolution.
- **Return type:** `ExecutionResult`.
- **Behaviour on `DENIED`:** explicit `when` branch — transitions lifecycle
  state to `DENIED`, publishes `permission.denied`, returns
  `ExecutionResult(status = ExecutionResultStatus.DENIED)`.
- **Behaviour on `DEFERRED`:** separate `when` branch — transitions
  lifecycle state to `DEFERRED` (no event published), returns
  `ExecutionResult(status = ExecutionResultStatus.DEFERRED)`.
- **Downstream unreachable?** Yes — `toolRegistry.resolve`/
  `toolInvocationBinding`/`Tool.execute` are only reached inside the
  `APPROVED`/`APPROVED_WITH_CONFIRMATION` branch.
- **Note:** this is the one precedent with a genuinely distinct `DENIED`
  vs. `DEFERRED` code path (different lifecycle state, different event
  publication) — but both branches still return the **same result type**,
  differing only in which enum value of `ExecutionResultStatus` is set.

### 2.6 `InMemoryAgentRuntime.start` — run-initiation gate (`src/runtime/InMemoryAgentRuntime.kt:233-254`)

- **Location/caller:** inside `start`, before any `AgentRun` record is
  created.
- **Return type:** `AgentRunCommandResult`.
- **Behaviour on `DENIED`:** `AgentRunCommandResult.Rejected(commandType, "run-initiation permission DENIED for requestingPrincipalId '...'")`.
- **Behaviour on `DEFERRED`:** `AgentRunCommandResult.Rejected(commandType, "run-initiation permission DEFERRED for requestingPrincipalId '...'")` —
  same variant (`Rejected`), message text only differs.
- **Downstream unreachable?** Yes — explicitly documented in this class's
  own comment: "writes nothing to `agentRuns`, resolves no Agent Identity,
  invokes no `AgentStepSource`, and publishes no `agent.*` event."

### 2.7 Cross-precedent invariants

Six call sites, zero exceptions to the following rules:

1. **No precedent ever throws for an ordinary, non-approved decision.**
   Exceptions are reserved exclusively for genuine implementation faults
   (storage exceptions, audit-write exceptions) that propagate *unchanged*
   — denial itself is explicitly documented, at 2.3, as "an ordinary,
   non-exceptional result."
2. **No precedent ever returns `PermissionDecision` or `ExecutionRequest`
   directly to its own caller.** Every one translates the outcome into a
   pre-existing, domain-specific result type.
3. **No precedent ever introduces a new public type solely to represent
   permission denial.** Where a new type exists at all (`EvidenceRegistrationOutcome`,
   2.3), it exists because the operation itself is new and needed an
   outcome type regardless of permission gating (it also represents
   `Registered`, a non-denial case) — never a type whose sole purpose is
   carrying a denial.
4. **`DENIED` and `DEFERRED` are never given materially different
   handling.** Four of six precedents (2.1, 2.2, 2.3, 2.4) collapse them
   into one combined branch outright. The two that branch explicitly (2.5,
   2.6) still return the same result type in both branches, differing only
   in a status-enum value or a message string — never a different
   downstream effect.
5. **Downstream work is unreachable in every case**, proven structurally
   (the gated call simply never occurs on any branch other than
   `APPROVED`/`APPROVED_WITH_CONFIRMATION`), never merely by convention.

---

## 3. Option Analysis — Every Constitutionally Possible Behaviour

Evaluated using only already-existing public contracts, per instruction. No
option below introduces a new public type.

### Option A — Caller never invokes `EvidenceIntelligence.analyse`

The composing caller (Unit 8) evaluates the gate; on non-approval, it
simply does not call `analyse`. Whatever the composing caller's *own*
operation returns to *its* caller on this branch is a question belonging
to that operation's own design — exactly as `DefaultEvidenceCustodian.accept`'s
own `EvidenceAcceptanceResult.Rejected` belongs to `accept`'s own
design, not to the Permission Engine's.

This is not really a fourth candidate distinct from the others — it is the
**structural precondition all six precedents already share** ("the gated
action is never performed"). The genuine open question is only what value,
if any, is produced for whoever called the composing caller — which is
Option E, below, not a separate option.

### Option B — Existing `PermissionDecision` returned

The composing caller could return the `PermissionDecision` itself (or a
value carrying it) to whatever called it.

- **Constitutional consistency:** permitted in principle — `PermissionDecision`
  already exists and carries no confidence/evidential-state authority
  Evidence Intelligence is barred from. Chapter 10 §5's traceability
  guarantee ("every authorised action leaves a record sufficient to
  reconstruct what was proposed, what was authorised... and what was
  executed") is satisfied by this option at least as well as any other.
- **Scope Lock consistency:** does not touch Evidence Intelligence's own
  four-type ceiling (Scope Lock §4) — `PermissionDecision` is not one of
  Evidence Intelligence's own types and this option adds nothing to
  Evidence Intelligence's own model.
- **Public contract impact:** none — no new type.
- **Implementation impact:** possible, but **used by no existing precedent
  anywhere in this repository.** All six call sites in §2 translate the
  decision into a domain-specific type rather than surfacing it directly.
  Adopting this here would be a first-of-its-kind pattern for Parker.
- **Interaction with Unit 8:** would require Unit 8's own operation to
  either return `PermissionDecision` as one branch of a broader type, or
  return it standing alone — the former is really Option E with
  `PermissionDecision` embedded as a field; the latter has no precedent.
- **Compatibility with Unit 5's interface:** fully compatible — does not
  touch `EvidenceIntelligence.analyse`'s signature at all, since this value
  is produced by the composing caller, never by `analyse` itself.

### Option C — Exception

The composing caller throws when the gate does not approve.

- **Constitutional consistency:** not prohibited by name anywhere, but in
  tension with the Constitution's own "fail-closed... defaults to
  inaction" framing, which describes denial as an ordinary, expected
  outcome of evaluating trust — not an exceptional, abnormal condition.
- **Scope Lock consistency:** no conflict textually, but conflicts with
  this Programme's own, uniformly and repeatedly stated distinction
  between a genuine implementation fault (which does propagate as an
  exception, per every precedent in §2) and an ordinary denial (which
  never does). Adopting an exception here would treat "the owner's policy
  said no" identically to "the storage layer crashed" — the same
  conflation `EvidenceRegistrationCoordinator`'s own KDoc (§2.3) explicitly
  distinguishes and rejects for its own two gates.
- **Public contract impact:** none — no new type (an exception class could
  be existing or new, but no existing exception type fits, and inventing
  one would itself be a new public type this study is instructed not to
  propose).
- **Implementation impact:** would require every caller of the composing
  operation to add exception handling for a routine, expected outcome —
  a distinctly different shape from every one of the six precedents.
- **Interaction with Unit 8:** would make Unit 8's own operation the only
  Permission-Engine-adjacent operation in the entire repository that
  signals ordinary denial by throwing.
- **Compatibility with Unit 5's interface:** compatible (again, outside
  `analyse` itself), but inconsistent with `analyse`'s own documented
  failure model (Contract Design §11; Implementation Plan §8 Unit 5 —
  which does use exceptions, but only for genuine implementation-level
  anomalies such as `ReasoningProviderResponse.Goal`, never for an
  ordinary, expected outcome).
- **Verdict:** constitutionally permitted, but the only option that
  actively **contradicts** an otherwise-universal repository convention,
  rather than merely lacking precedent for it.

### Option D — An existing, unrelated domain-specific denial result reused (e.g. `EvidenceRetrievalResult.Rejected`, `EvidenceAcceptanceResult.Rejected`)

- **Constitutional consistency:** fails the Constitution's own
  transparency principle ("Parker does not obscure what it is doing or
  why") if reused for a semantically different event — a caller reading
  `EvidenceRetrievalResult.Rejected` would reasonably conclude evidence
  *retrieval* was denied, when the actual event was the invocation gate
  itself, possibly before any retrieval was ever attempted.
- **Scope Lock consistency:** no direct textual conflict, but no textual
  authorisation either — nothing in the Contract Design, Scope Lock, or
  Implementation Plan connects Evidence Intelligence's own invocation
  gate to Evidence Custodian's own retrieval-denial type.
- **Public contract impact:** none — no new type, but a misleading reuse
  of an existing one.
- **Implementation impact:** would require `EvidenceCustodian`'s own
  result types to be imported and returned by a wholly unrelated
  operation, coupling Unit 8's composition-root code to a type owned by a
  different subsystem for a purpose that type was never designed to
  serve.
- **Interaction with Unit 8:** discouraged — Unit 8 would need to justify,
  case by case, why a caller should read an Evidence-Custodian-specific
  outcome type for an Evidence-Intelligence-specific event.
- **Compatibility with Unit 5's interface:** compatible (outside `analyse`),
  but semantically dishonest about what was actually denied.
- **Verdict:** technically constructible, but the weakest candidate on
  transparency grounds; no precedent reuses one subsystem's denial type
  for a different subsystem's gate.

### Option E — A new outcome type defined by whatever Unit 8 introduces as the composing caller (mirroring `EvidenceRegistrationOutcome`)

- **Constitutional consistency:** fully consistent — this is exactly what
  every existing coordinator in this repository already does when it
  introduces a genuinely new sequencing operation (`EvidenceRegistrationOutcome`,
  `AgentRunCommandResult`, `ExecutionResult`, `GoalPlanningHandoffOutcome`,
  per the Planning Review's own §8 "Existing Precedent Comparison").
  "Cognition proposes, trust authorises, runtime executes" is respected
  identically to every other precedent: the composing caller (runtime,
  not Evidence Intelligence) owns the decision of what to report.
- **Scope Lock consistency:** does **not** violate Evidence Intelligence's
  own four-public-type ceiling (Scope Lock §4) — that ceiling governs
  `EvidenceAnalysisRequest`, `EvidenceAnalysisResult`, the payload
  selector, and `EvidenceIntelligence` itself; it says nothing about, and
  was never intended to reach, a wholly separate composition-level type
  the way Unit 7's own acceptance coordinator is already, separately,
  authorised to have its own outcome representation (Scope Lock §6 step 4;
  Implementation Plan §8 Unit 7's own "not a new public type... the
  observable outcome... expressed through that accepting subsystem's own,
  already-existing... contract" language governs the *coordinator's*
  outcome, drawn from *existing* acceptance-interface dispositions — a
  narrower case than this one, discussed next).
- **Public contract impact:** technically a new type, but not one
  belonging to Evidence Intelligence's own model — precisely the same
  distinction the Implementation Plan itself draws between Evidence
  Intelligence's four capped types and the acceptance coordinator's own,
  separately-permitted representation.
- **Implementation impact:** ordinary composition-root work, no different
  in kind from any other coordinator this repository already has.
- **Interaction with Unit 8:** this **is** Unit 8's own work — not
  something Unit 6 needs to pre-empt or design now.
- **Compatibility with Unit 5's interface:** fully compatible — `analyse`'s
  signature is untouched; this type wraps a *call to* `analyse`, never
  replaces or modifies it.
- **Verdict:** the only option matching every one of the cross-precedent
  invariants in §2.7 without qualification.

---

## 4. Constitutional Comparison

| Option | Never throws for ordinary denial | Never exposes raw Permission Engine value as analytical output | Transparent about what was denied | Matches ≥1 existing precedent exactly | New type outside Evidence Intelligence's own cap |
|---|---|---|---|---|---|
| A (never invoke) | n/a (precondition, not an outcome) | n/a | n/a | Yes (universal) | n/a |
| B (`PermissionDecision`) | Yes | Yes (distinct type) | Yes | **No** (no precedent surfaces it directly) | No new type |
| C (exception) | **No** | Yes | Yes | **No** (contradicts every precedent) | No new type |
| D (reuse unrelated Rejected type) | Yes | Yes | **No** (misleading) | No (never done across subsystems) | No new type |
| E (Unit 8's own outcome type) | Yes | Yes | Yes | **Yes** (`EvidenceRegistrationOutcome` et al.) | Yes, but outside Evidence Intelligence's own model — same footing as Unit 7's coordinator |

No option is constitutionally forbidden outright except that Option C sits
in direct tension with this Programme's own consistently-applied
denial-is-not-an-exception discipline, and Option D sits in tension with
the transparency principle. Options A, B, and E are each individually
constitutionally admissible; only Option E satisfies every precedent
invariant simultaneously.

---

## 5. Is One Option Already Mandated, or Is Governance Genuinely Silent?

**Genuinely silent on the specific mechanism, but not silent on the rule
the mechanism must obey.**

No document — Constitution, CDR-005, CDR-007, Contract Design, Scope Lock,
or Implementation Plan — states which of Options B, C, D, or E the future
Unit 8 composing caller must use. The Implementation Plan's own §11 ("Out
of Scope") explicitly places "the literal `PermissionLevel` value and any
literal string identifiers used in registering the invocation-gating
proposal" out of scope as "composition-root policy content decided at
implementation time" — the same reasoning extends, by direct analogy and
without contradiction, to the *return-type* question, which is equally a
composition-root implementation decision the Contract Design and Scope
Lock never purported to make (their own "Evidence Intelligence's own
contract ends at returning the result list" language, Contract Design §10,
§14, describes `analyse` itself, not whatever wraps a call to it).

What **is** already settled, and does constrain Unit 8's eventual choice
without further governance action, is the pattern every existing precedent
in §2 uniformly follows:

- denial must never be signalled by throwing (contradicts Option C);
- denial must never be signalled by a fabricated or empty
  `EvidenceAnalysisResult` (already stated, Implementation Plan §10 items
  3, 12 — this rules out disguising denial as `analyse` returning
  `emptyList()`, which the Planning Review already confirmed means
  "nothing worth proposing," never "not permitted to try");
- denial must never be represented by silently reusing a semantically
  unrelated subsystem's own outcome type (Option D, disfavoured on
  transparency grounds, though not textually forbidden);
- whatever type is used should be genuinely owned by the operation that
  needs it (mirroring Option E), not manufactured solely to carry a
  permission outcome.

---

## 6. Minimum Governance Action Required

Given the above, the minimum action is narrow and does **not** require
redesigning anything already frozen:

**A short Implementation Plan clarification** (not a new document, not a
Scope Lock amendment, not a CDR, not a constitutional amendment) stating,
in one or two sentences at Unit 6's own entry (§8 Unit 6) or Unit 8's own
entry (§8 Unit 8):

> The invocation-gating decision's own denial representation is deferred
> to whatever operation Unit 8 introduces as Evidence Intelligence's
> composing caller. That operation defines its own result type for this
> purpose, exactly as `EvidenceRegistrationCoordinator` already defines
> `EvidenceRegistrationOutcome` for its own sequencing responsibility —
> never by throwing for an ordinary denial, never by fabricating or
> emptying an `EvidenceAnalysisResult`, and never by reusing a
> semantically unrelated subsystem's own result type.

This closes the gap the Planning Review identified without inventing any
Kotlin, without touching CDR-005, Chapter 10, the Contract Design, or the
Scope Lock, and without expanding Evidence Intelligence's own four-type
ceiling. It is genuinely minimal: it says *what rule* Unit 8 must follow
(already, in effect, established by six-for-six precedent), while leaving
the *actual type* to Unit 8's own composition-root work, exactly where
every analogous decision in this Programme (Evidence Custodian's own
Phase 10 registration, the acceptance coordinator's own concrete shape) has
always been left.

---

## 7. Final Verdict

**B — Governance clarification required before Unit 6 implementation.**

The clarification required is the one stated in §6 above — an
Implementation Plan clarification only. It is not evidence that Unit 6's
architecture is unsettled; every other question the Planning Review raised
about Unit 6 (gate ownership, resource identity, sequencing) was already
answered cleanly by existing frozen text. This is the single remaining
point where the Implementation Plan is silent on a question its own text
implicitly assumes an answer to (Implementation Plan §10 item 3's
"reachable in production" language presumes *something* happens on the
non-reachable branch, without saying what) — and six-for-six precedent
already tells us the shape that answer must take, even though no document
yet says so for Unit 6/Unit 8 specifically.

---

## 8. Confirmation No Files Changed

No governance document was modified. No Kotlin was written, modified, or
proposed as a diff. No test was modified. This document is the only file
created by this study.

## 9. No Git Actions

Nothing was staged, committed, or pushed. No git command other than
read-only inspection (`git log`, `git show`, directory listing) was run
during this study.
