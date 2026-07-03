# Architecture Decisions Review

## Status

Review-only. This document does not modify
`docs/architecture/ARCHITECTURE_DECISIONS.md`, `src/`, `tests/`, or any
existing specification. It validates whether that document accurately
records Parker's existing platform-wide architectural decisions.

Reviewed against: `docs/architecture/IMPLEMENTATION_ORDER.md`,
`docs/architecture/INTER_SPECIFICATION_CONTRACTS.md`,
`docs/reviews/Phase3ArchitecturePositionReview.md`,
`docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`,
`docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md`,
`docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`,
`docs/architecture/IdentityService.md`,
`docs/specifications/volume-02-core-schemas/Task-Schema.md`, ADR-012,
`docs/specifications/volume-03-core-interfaces/ExecutionPipeline.md`,
`docs/specifications/volume-03-core-interfaces/PermissionEngine.md`,
`docs/specifications/volume-03-core-interfaces/ToolRegistry.md`,
`docs/specifications/volume-03-core-interfaces/ResourceRegistry.md`,
`docs/specifications/volume-03-core-interfaces/EventBus.md`, and
`docs/architecture/action-mapping.md`.

## 1. Executive Summary

`docs/architecture/ARCHITECTURE_DECISIONS.md` is substantially accurate.
Every one of its fourteen decisions (AD-001 through AD-014) is supported
by directly citable repository text, and this review independently
verified a sample of the document's evidence quotations against the
actual source documents and found them accurate, including several
verbatim matches. No decision invents a principle that is not already
present, by name or by clearly repeated pattern, in an existing
specification. No decision was found to contradict a specification it
cites. The claimed governance hierarchy (Architecture Decisions →
Specifications → Inter-specification Contracts → Implementation) is
broadly consistent with how the repository is actually organised, with
one precision caveat detailed in Section 7.

This review found one factual error (a mis-stated event count in AD-009),
one wording-precision issue (AD-002's two-category framing does not
cleanly place the Task Manager Runtime's own "coordinate and decide"
role), one status-transparency gap worth making explicit rather than
leaving implicit (several decisions rely on the not-yet-independently-
reviewed Planner Runtime Specification), and two genuinely missing,
well-evidenced platform-wide decisions. None of these findings amounts to
an architectural contradiction or requires redesigning any decision;
all are narrow, specific corrections. **Final assessment: Needs
correction pass.**

## 2. Validated Decisions

The following decisions were checked against their cited evidence and
against every other specification they touch, and are confirmed accurate
as written, requiring no correction:

- **AD-001 (Identity First).** Confirmed. `Principal.md`'s "Parker MUST
  NOT execute any request without an identified Principal," `IdentityService.md`'s
  identity-before-permission framing, and the near-identical resolution
  rule independently restated in all three Phase 3 specifications all
  support this decision exactly as stated.
- **AD-003 (Execution Pipeline Is the Sole Execution Authority).**
  Confirmed. ADR-003, `ExecutionPipeline.md`'s normative requirements, and
  `ToolRegistry.md`'s "only the Execution Pipeline" resolution rule are
  accurately cited and consistently restated across all three Phase 3
  specifications.
- **AD-004 (Task Manager Owns Canonical Tasks).** Confirmed, and notably
  well-triangulated: this review independently verified that
  `TaskManagerRuntimeSpecification.md`, `AgentRuntimeSpecification.md`,
  and `PlannerRuntimeSpecification.md` each state this rule from their
  own side without deferring to one another, which is stronger evidence
  than a single document's claim would be.
- **AD-006 (Agent Runtime Never Owns Tasks).** Confirmed. The quoted
  "MUST NOT be inferred from one another" language and the reciprocal
  Task Manager Runtime Specification statement are both accurate
  verbatim or near-verbatim citations.
- **AD-007 (Permission Decisions Belong to the Permission Engine).**
  Confirmed, including its own honest carve-out: the "Consequences" field
  correctly notes that pure lifecycle transitions with no external effect
  are not gated by default, which is `TaskManagerRuntimeSpecification.md`
  §8's own stated reasoning, not an invented exception.
- **AD-008 (Identity Decisions Belong to Identity Service).** Confirmed;
  the `IdentityService.md` quotation ("`updateStatus` is where the
  Identity Service becomes the single enforcement point...") is verbatim
  accurate.
- **AD-009 (Everything Important Is Auditable).** The underlying decision
  is confirmed and well-evidenced; one citation within it contains a
  factual error — see Section 4.
- **AD-010 (Model Independence).** Confirmed. Both cited specifications
  literally title a design goal "Model independence," so this is a
  directly-named principle, not an inferred one.
- **AD-011 (Context Is Reference-Based) and AD-012 (Memory and World
  Model Are Context Providers).** Both confirmed; the
  `TaskManagerRuntimeSpecification.md` §9 quotation used as evidence for
  AD-011 is verbatim accurate, including the em-dash phrasing.
- **AD-013 (Specifications Define Contracts).** Confirmed. Every
  specification's own Status header states this pattern in nearly
  identical language, and no specification reviewed contradicts it.
- **AD-014 (Architecture Before Implementation).** Confirmed, and its own
  "Future considerations" field is itself an example of the honesty this
  review is checking for: it correctly discloses that the Planner Runtime
  Specification has not yet had its own independent review, rather than
  implying full parity with the other two Phase 3 specifications.

## 3. Decisions Requiring Correction

- **AD-002 (Proposal Before Authority).** The decision's own wording —
  "Intelligent subsystems propose. Deterministic runtime components
  authorise and execute." — presents a clean two-category split that does
  not explicitly place the Task Manager Runtime, which is neither purely
  a proposer nor purely an authoriser/executor. `TaskManagerRuntimeSpecification.md`
  §1's own core principle is "The Task Manager coordinates work. It does
  not execute tools directly, does not bypass permissions, and does not
  replace the Execution Pipeline" — a third, coordinating/deciding role
  distinct from both "propose" and "authorise/execute." This is not a
  contradiction (Task Manager clearly belongs on the deterministic side
  of the boundary, not the proposing side), but the current two-bucket
  phrasing invites a reader to misclassify Task Manager as an "authoriser"
  or "executor," which it explicitly is not (only Permission Engine
  authorises; only Execution Pipeline executes). **Recommended fix:** add
  a clarifying phrase noting that "deterministic runtime components"
  includes coordinating components (the Task Manager) that neither
  authorise nor execute, distinct from the Permission Engine (authorises)
  and Execution Pipeline (executes) it depends on. A wording clarification,
  not a restatement of the underlying rule.

No other decision requires correction to its substantive Decision or
Reasoning text.

## 4. Evidence Problems

- **AD-009 cites an incorrect event count for the Agent Runtime
  Specification.** The entry reads "`AgentRuntimeSpecification.md` §9
  (16-event table)." This review counted the actual event table in
  `AgentRuntimeSpecification.md` §9 directly (`AgentCreated` through
  `AgentCancelled`) and found **17 rows**, not 16. By contrast, the same
  entry's other two counts are exactly correct: `TaskManagerRuntimeSpecification.md`
  §10 does contain exactly 19 event rows, and `PlannerRuntimeSpecification.md`
  §11 does contain exactly 13. **Recommended fix:** change "16-event
  table" to "17-event table" in AD-009's Evidence field. This is a narrow
  factual correction; it does not affect the decision's substance.

No other cited quotation or paraphrase checked by this review was found
to misstate its source. This review specifically re-verified, against
the primary documents, the `Principal.md` quotation (AD-001), the
`ExecutionPipeline.md` normative-requirements quotation (AD-003), the
Agent Runtime Specification's "Agent Task" quotation (AD-004), and the
`IdentityService.md` `updateStatus` quotation (AD-008); all four matched
their sources exactly.

## 5. Status Problems

No decision's status should change from Accepted to Proposed, Draft, or
Open. However, one transparency point is worth making explicit rather
than leaving implicit:

- **Decisions AD-002, AD-005, AD-010, AD-011, and AD-012 all cite
  `PlannerRuntimeSpecification.md` as primary or supporting evidence,**
  and that specification has not yet undergone its own independent
  review-and-correction pass, unlike the Agent Runtime Specification
  (`docs/reviews/AgentRuntimeSpecificationReview.md` → correction pass)
  and the Task Manager Runtime Specification
  (`docs/reviews/TaskManagerRuntimeSpecificationReview.md` → correction
  pass). `docs/reviews/Phase3ArchitecturePositionReview.md` examined the
  Planner Runtime Specification only as part of a cross-specification
  architecture check, not as a standalone review of its own internal
  consistency — a distinction that document itself draws, and
  `ARCHITECTURE_DECISIONS.md`'s own AD-014 "Future considerations"
  already discloses this fact once, in one place. This review does not
  find that this downgrades any decision's Accepted status: every rule
  the five decisions above cite from the Planner Runtime Specification is
  a foundational boundary statement (e.g. "the Planner Runtime does not
  create Tasks directly") that is independently cross-corroborated by the
  other two, already-reviewed specifications, and none of these
  boundaries is the kind of narrow implementation detail a future
  Planner-specific correction pass would be expected to reverse. Still,
  since five separate decisions lean on this one not-yet-reviewed
  document, it is worth surfacing once, in `ARCHITECTURE_DECISIONS.md`
  itself, rather than only in AD-014's own entry.

## 6. Missing Decisions

Two platform-wide decisions are well-evidenced across multiple
independent documents but not currently recorded in `ARCHITECTURE_DECISIONS.md`:

- **"Invalid, Not Denied."** The distinction between an invalid request
  (malformed, or referring to a capability Parker has no concept of —
  never reaching a decision at all) and a denied request (well-formed,
  but declined by an authority) is named explicitly, using that exact
  "invalid, not denied" phrasing, in at least four independent documents:
  `docs/architecture/action-mapping.md` ("Unknown Actions": "An
  unresolvable proposed action is treated as invalid, not denied");
  `docs/architecture/IdentityService.md` ("Principal Resolution": "An
  unresolvable `PrincipalId`... is treated the same way
  `action-mapping.md` treats an unresolvable proposed action: invalid,
  not denied"); `AgentRuntimeSpecification.md` §10 ("Malformed action...
  is, per `action-mapping.md`'s existing... section, invalid, not
  denied"); and `TaskManagerRuntimeSpecification.md` §8 ("An unresolvable
  Task Owner is invalid, not denied"). This meets
  `ARCHITECTURE_DECISIONS.md`'s own Section 7 criteria for a new
  decision — multiple specifications depend on it, it is a platform-wide
  rule about how every subsystem categorises failure, and changing it
  would affect audit semantics (Chapter 43) across every subsystem.
- **"Terminal Lifecycle States Are Truly Terminal."** No lifecycle state
  machine in this repository permits a transition out of a terminal
  state, and this is stated as a deliberate, cross-referenced pattern —
  not a coincidence — in multiple places: `AgentRuntimeSpecification.md`
  §5 ("matching every other lifecycle state machine in this repository
  (`ExecutionLifecycleState`, `ToolLifecycleTransitions`,
  `PrincipalLifecycleTransitions`, and the Task Manager Task lifecycle
  itself) — none of which permit resurrection out of a terminal state");
  `TaskManagerRuntimeSpecification.md` §5 ("No transition out of any
  terminal state"); and `PlannerRuntimeSpecification.md` §5 ("matching
  every other lifecycle state machine in this repository"). This spans
  at least six distinct state machines (three already implemented, three
  Phase 3 lifecycles) and is exactly the kind of platform-wide,
  multiple-specification-dependent rule Section 7 describes.

One weaker, optional candidate is also worth recording for
completeness, though this review rates it lower-priority than the two
above:

- **"Reuse Existing Vocabulary Rather Than Inventing Parallel Enums."**
  `TaskManagerRuntimeSpecification.md` §4 reuses `RequestOrigin` and
  `RequestPriority` (`src/contracts/ExecutionRequest.kt`) for Task Source
  and Task Priority rather than inventing new enums, and
  `PlannerRuntimeSpecification.md` §4 independently does the same for
  Task Proposal's Source and Priority fields, plus reuses `RiskEstimate`
  for its own Risk concept. This is evidenced twice, independently, which
  meets the letter of Section 7's "multiple specifications depend on it"
  criterion, but it is narrower in scope (a data-modelling convention)
  than the other two candidates above, which are load-bearing semantic
  and lifecycle rules. This review raises it for consideration rather
  than recommending it be added outright.

## 7. Governance Assessment

The claimed hierarchy — Architecture Decisions → Specifications →
Inter-specification Contracts → Implementation
(`ARCHITECTURE_DECISIONS.md` §5) — is broadly consistent with the
repository's actual structure and process: implementation genuinely does
not exist yet for any Phase 3 concept, contracts genuinely are cited as
derived from specifications rather than the reverse, and specifications
genuinely do implement the decisions this document records.

One precision point is worth noting, though it does not amount to an
inconsistency this review would call a governance defect. The diagram
reads as, and is reasonably read as, a top-down *authority* ordering
(when two levels disagree, the higher one wins — `ARCHITECTURE_DECISIONS.md`
§2 states this explicitly: "the specification is more likely to contain
the error"). It is not, however, the *historical* order in which these
particular fourteen decisions came to be written down: all fourteen were
extracted from already-existing specifications (the Agent Runtime,
Task Manager Runtime, and Planner Runtime Specifications were all written
before `ARCHITECTURE_DECISIONS.md`), not authored independently in
advance of them. `ARCHITECTURE_DECISIONS.md` §1 already discloses this
plainly ("Every decision recorded here is already evidenced by the
existing repository — this document invents no new principle"), so this
is not a hidden or misleading claim — but the §5 diagram, read on its
own, could be misread as describing a temporal creation sequence rather
than an authority-when-conflict sequence. A one-sentence clarification in
§5 itself (rather than relying on §1's disclosure alone) would remove any
ambiguity.

Separately, `docs/architecture/INTER_SPECIFICATION_CONTRACTS.md` — the
third box in the diagram — does not itself define or add any contract;
by its own §8 ("Future Use"), it is explicitly a "follow-up act
reflecting a decision already made in the relevant specification(s),
never as the place that decision is made." This is consistent with, not
contradictory to, `ARCHITECTURE_DECISIONS.md`'s own §5 prose (which
already gets this right: "A gap discovered at the Inter-specification
Contracts level ... is resolved by revising the affected
specification(s)"), so this review records it as a confirmation of
existing accuracy, not a problem requiring correction.

## 8. Recommended Corrections

1. **AD-002:** Add a clarifying phrase distinguishing the Task Manager
   Runtime's coordinating/deciding role from both "propose" and
   "authorise/execute," so the two-category framing cannot be misread as
   placing Task Manager among the authorities. Wording only; the
   underlying rule is correct as stated. (Section 3)
2. **AD-009:** Correct "16-event table" to "17-event table" for
   `AgentRuntimeSpecification.md` §9. A factual correction only. (Section
   4)
3. **Status transparency:** Add one sentence to `ARCHITECTURE_DECISIONS.md`
   (Section 1 or 2) noting that some decisions currently rely on the
   not-yet-independently-reviewed Planner Runtime Specification, so this
   is visible without a reader needing to notice it only inside AD-014's
   own entry. (Section 5)
4. **Missing decisions:** Consider adding "Invalid, Not Denied" and
   "Terminal Lifecycle States Are Truly Terminal" as AD-015 and AD-016
   respectively, both well-evidenced across multiple independent
   documents by name or by explicit cross-reference. Optionally consider
   "Reuse Existing Vocabulary Rather Than Inventing Parallel Enums" as a
   narrower AD-017. (Section 6)
5. **Governance clarity:** Add one sentence to §5 distinguishing the
   diagram's authority-when-conflict meaning from a historical-creation-
   order reading, given all fourteen current decisions were extracted
   from already-existing specifications rather than authored in advance
   of them. (Section 7)

None of the above requires revisiting any decision's substantive Decision
or Reasoning text, reclassifying any decision's Status, or altering the
document's overall structure.

## 9. Final Assessment

**Needs correction pass.**

`ARCHITECTURE_DECISIONS.md` is fundamentally sound: every decision is
evidenced, none contradicts a specification, none invents a principle
not already present in the repository, and the governance hierarchy it
claims is genuinely consistent with how the repository operates. The
issues this review found — one factual event-count error, one wording
clarification, one transparency addition, and two well-evidenced
candidate decisions worth adding — are all narrow and specific, each
tied to an exact location in the document. None represents a
contradiction or a structural flaw that would require rethinking the
document's approach. A short, targeted correction pass addressing
Section 8's five items would bring this document to the same
publication-ready state already reached by the corrected Agent Runtime
and Task Manager Runtime Specifications.

## Related

- `docs/architecture/ARCHITECTURE_DECISIONS.md`
- `docs/architecture/IMPLEMENTATION_ORDER.md`
- `docs/architecture/INTER_SPECIFICATION_CONTRACTS.md`
- `docs/reviews/Phase3ArchitecturePositionReview.md`
- `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`
- `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md`
- `docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`
- `docs/reviews/AgentRuntimeSpecificationReview.md`
- `docs/reviews/TaskManagerRuntimeSpecificationReview.md`
- `docs/architecture/IdentityService.md`
- `docs/specifications/volume-02-core-schemas/Task-Schema.md`
- `docs/adr/ADR-012-task-and-workflow-separation.md`
- `docs/specifications/volume-03-core-interfaces/ExecutionPipeline.md`
- `docs/specifications/volume-03-core-interfaces/PermissionEngine.md`
- `docs/specifications/volume-03-core-interfaces/ToolRegistry.md`
- `docs/specifications/volume-03-core-interfaces/ResourceRegistry.md`
- `docs/specifications/volume-03-core-interfaces/EventBus.md`
- `docs/architecture/action-mapping.md`
- `src/contracts/ExecutionRequest.kt`
