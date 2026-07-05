# Permission Policy

## Status

Version: 1.0 (Sprint 2 Unit A2 prerequisite)

Purpose: Defines Parker's authorisation policy model.

Scope: Specification only. No Kotlin implementation. This document
describes behaviour, not implementation -- it assumes no Kotlin classes
beyond the existing `PermissionEngine` contracts already in
`src/interfaces/PermissionEngine.kt` and `src/contracts/Permission.kt`,
and it introduces no new Architecture Decision. It fits beneath
`IdentityService` and `PermissionEngine`, and above the Execution
Pipeline, in the existing architectural sequence:

Identity → Permission Policy → Execution

Identity determines WHO is acting. Permission Policy determines WHETHER
that Principal may perform the requested Action. Execution performs the
Action only after permission has been granted.

This specification complies with `docs/architecture/parker-constitution.md`
("Architecture v1.0 -- Constitutional Foundation") and preserves its
governing principles without exception:

- Parker owns authority. Modules provide capability.
- Cognition proposes. Trust authorises. Runtime executes.
- If trust cannot be verified, Parker cannot act.
- The owner remains in control.

Nothing in this document relaxes, reinterprets, or creates an exception
to any of the four principles above. Where a section below appears to
create tension with one of them, the principle governs and the section
is wrong.

## 1. Purpose

Three questions are asked, in order, of every proposed action, and each
is answered by a different, non-overlapping authority:

- **Identity** answers: "Who is requesting the action?" This is
  `IdentityService`'s question alone, and it is answered before either
  of the two questions below is asked (`docs/architecture/IdentityService.md`).
- **Permission Policy** answers: "May this Principal perform this
  Action under these circumstances?" This is the question this document
  defines the model for.
- **Execution** answers: "Perform the authorised action." This is the
  Execution Pipeline's question alone, and it is never asked until the
  question above has already been answered affirmatively.

This ordering is not a convenience -- it is the constitution's central
operating discipline restated for this specific policy layer: **Cognition
proposes. Trust authorises. Runtime executes.** A reasoning provider or
any other cognition proposes an action; it carries no authority of its
own. Permission Policy is where trust authorises -- the Permission
Engine evaluates the proposal against the owner's authorisations, the
Identity Service's determination of who is asking, and the policy this
document describes. Only after that authorisation does the Execution
Pipeline execute. No stage may absorb another: Permission Policy does
not resolve identity, and it does not execute anything.

## 2. Evaluation Order

Every `ExecutionRequest` is evaluated in exactly this sequence, and no
step may be skipped or reordered:

1. **Identity resolved.** `IdentityService.resolve` is consulted for the
   requesting Principal.
2. **Identity status validated.** A Suspended, Revoked, Archived, Created,
   or otherwise unresolvable Principal is denied here, before Permission
   Policy is ever consulted -- this step is already implemented by
   `DefaultPermissionEngine` (Sprint 2, Unit A1;
   `docs/implementation/IMPLEMENTATION_GAPS.md` #40, closed) and this
   document does not change it.
3. **Permission Policy evaluated.** Only for a Principal that has passed
   step 2 is the policy this document describes consulted, against the
   requested Action, the target Resources, and the other inputs in
   Section 3.
4. **Permission decision produced.** One of the outcomes in Section 4 is
   produced.
5. **Execution Pipeline proceeds only if authorised.** `APPROVED` or
   `APPROVED_WITH_CONFIRMATION` are the only outcomes under which
   execution may proceed at all.

**Permission Policy is never evaluated for an unresolved or non-active
Principal.** Step 2 is an identity-status gate, not a policy decision --
by the time Permission Policy runs, the question "who is this and are
they in good standing" has already been answered. This document assumes
that gate exists and behaves as Unit A1 already implements it; it does
not redefine it.

## Policy Invariants

Three invariants hold at all times, without exception:

- Permission Policy never executes actions.
- Permission Policy never resolves identity.
- Permission Policy never modifies Resources.

These invariants preserve the constitutional separation of
responsibility between Identity, Permission, and Execution: each stage
answers its own question and no other. Blurring any one of them into
Permission Policy would collapse a distinction the constitution treats
as load-bearing, not incidental.

## Policy Evaluation Principles

Permission Policy evaluates only the current request.

Policy decisions are deterministic for a given set of inputs.

Policy evaluation has no side effects.

Policy evaluation must not modify runtime state.

Policy evaluation must not depend upon hidden internal state.

## 3. Policy Inputs

Permission Policy is evaluated against the following inputs, described
conceptually. None of these is a new contract -- each corresponds to
data already carried by `ExecutionRequest`, `Principal`, or the
Permission contracts (`src/contracts/ExecutionRequest.kt`,
`src/contracts/Principal.kt`, `src/contracts/Permission.kt`):

- **Principal.** The identity already resolved and validated in
  Section 2 -- who is asking.
- **Requested Action.** What the Principal is proposing to do.
- **Target Resources.** What the action would act upon.
- **Execution Context.** The circumstances surrounding the request --
  its origin, priority, and any other contextual detail already carried
  by the request.
- **Requested Permission or Capability Scope.** The extent of authority
  the request implies it needs -- not a promise that this extent will
  be granted, only what is being asked for.
- **Environmental Context (future).** Circumstances external to the
  request itself -- time, location, device state, or similar signals --
  that a future policy model may take into account. No such signal is
  defined or required today; this input is named so a future extension
  has a place to attach to without renegotiating this document.

## 4. Policy Outcomes

Permission Policy produces exactly one of the four outcomes already
defined by `PermissionDecisionOutcome` (`src/contracts/Permission.kt`).
This document does not add, remove, or rename any of them:

- **APPROVED.** The action is authorised to proceed to execution
  without further condition.
- **APPROVED_WITH_CONFIRMATION.** The action is authorised, but only
  after the owner (or another appropriately-authorised Principal)
  explicitly confirms it. See Section 8.
- **DEFERRED.** No decision is made yet -- the request is neither
  authorised nor denied, pending something the policy needs before it
  can decide (for example, information not yet available).
- **DENIED.** The action is not authorised.

**DENIED is the default when no rule matches.** Permission Policy does
not have an implicit "otherwise approve" path. If no policy rule
addresses a given Principal, Action, Resource, and context combination,
the outcome is `DENIED` -- silence in the policy is not permission,
consistent with the constitution's "if trust cannot be verified, Parker
cannot act."

## 5. Action Categories

The following are conceptual policy categories, used to reason about
and group actions for policy purposes. They are not Kotlin enums, and
they do not replace or redefine `PermissionAction`
(`src/contracts/Permission.kt`) -- a single `PermissionAction` value may
fall into one of these categories depending on context, and this
document does not assert a fixed mapping between the two:

- **Read.** Observing existing data without altering it.
- **Observe.** Passive monitoring of state or events, distinct from a
  one-time read.
- **State Change.** Creating, modifying, or removing data or state.
- **Communication.** Sending information to a person or system, inside
  or outside Parker.
- **Financial.** Anything with a direct monetary consequence.
- **Security.** Anything that affects Parker's own trust boundaries --
  identity, permissions, or credentials.
- **Administration.** Configuration or management of Parker itself,
  distinct from acting on the owner's behalf in the world.
- **Destructive.** Anything not reasonably reversible.

## 6. Risk Levels

Permission Policy reasons about risk qualitatively, using four levels
consistent with the risk vocabulary `ExecutionRequest.riskEstimate`
already uses (`src/contracts/ExecutionRequest.kt`'s `RiskEstimate`
enum: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`):

- **Low.** Limited or no consequence if the action turns out to be
  unwanted.
- **Medium.** Noticeable but recoverable consequence.
- **High.** Significant, difficult-to-reverse consequence.
- **Critical.** Severe or irreversible consequence.

Risk level influences policy in one direction only: higher risk narrows
what is permitted by default and raises the bar for automatic approval.
A Low-risk action is the most plausible candidate for `APPROVED`. A
High- or Critical-risk action is a plausible candidate for
`APPROVED_WITH_CONFIRMATION` or `DENIED`, depending on the policy rule
that governs it. This document does not define a scoring algorithm,
formula, or weighting for how a specific risk level is computed or how
it combines with other inputs -- that is implementation-phase content
for whatever concretely defines policy rules, not something this
specification settles in the abstract.

## 7. Default Policy

In the absence of a specific rule, Permission Policy's defaults are
conservative, matching the constitution's "safety by default" posture:

- **Unknown action → DENIED.** An Action Permission Policy has no rule
  for is not implicitly allowed.
- **Unknown resource → DENIED.** A target Resource Permission Policy
  has no rule for is not implicitly reachable.
- **Unknown permission → DENIED.** A Requested Permission or Capability
  Scope Permission Policy does not recognise is not implicitly granted.
- **Inactive Principal → DENIED.** Restated from Section 2 for
  completeness: a Principal that is not Active never reaches Permission
  Policy in the first place. **Identity failures occur before policy
  evaluation** -- they are Section 2's concern, not this section's, and
  this document does not duplicate or re-derive that check.

## 8. Confirmation Policy

`APPROVED_WITH_CONFIRMATION` exists for actions Permission Policy is
willing to authorise, but only with the owner's (or another
appropriately-authorised Principal's) explicit, in-the-moment consent.
Confirmation is expected, conceptually, wherever an action's risk level
or category makes proceeding without a human check inappropriate --
for example:

- A Financial action above a threshold the owner has not pre-authorised.
- A Destructive action that cannot be undone.
- A Security action that changes who else can act on the owner's
  behalf.
- Any High- or Critical-risk action outside the Principal's usual
  pattern of activity.

These are illustrative, not an exhaustive or binding list -- concrete
confirmation rules are policy content, not something this specification
enumerates exhaustively. This document does not define how confirmation
is presented, requested, or collected -- no UI, prompt, or interaction
flow is specified here. `DefaultExecutionPipeline`'s existing
simplification (treating `APPROVED_WITH_CONFIRMATION` identically to
`APPROVED`, per its own KDoc, pending "a real confirmation workflow" it
names as Chapter 42 territory) is unaffected by this document. This
specification defines only when policy *should* select
`APPROVED_WITH_CONFIRMATION` as an outcome, not how a confirmation is
subsequently carried out. Confirmation authorises a single execution
request only. Confirmation does not permanently modify Permission
Policy or create an ongoing permission grant.

## 9. Policy Extensibility

The policy model this document describes is deliberately abstract
enough that its concrete form may later become:

- **Role-based** -- decisions keyed on a Principal's assigned role.
- **Attribute-based** -- decisions keyed on Principal or Resource
  attributes rather than fixed roles.
- **Capability-based** -- decisions keyed on scoped capability grants.
- **Context-aware** -- decisions that incorporate the Environmental
  Context named in Section 3.

Any of these may be adopted later **without changing `PermissionEngine`'s
public contract** (`evaluate(request): PermissionDecision`,
`explain(decisionId): PermissionExplanation`, `src/interfaces/PermissionEngine.kt`).
The contract already treats policy as something an implementation
supplies, not something its signature encodes -- extensibility is a
property this document relies on, not one it needs to add.

## 10. Relationship to Existing Architecture

- **`PermissionEngine`** (`src/interfaces/PermissionEngine.kt`,
  `docs/specifications/volume-03-core-interfaces/PermissionEngine.md`)
  is the component that will apply the policy this document describes.
  This document defines the policy `PermissionEngine.evaluate` consults
  once identity has already been validated -- it does not change
  `PermissionEngine`'s interface.
- **`IdentityService`** (`docs/architecture/IdentityService.md`) and
  its Sprint 2 Unit A1 integration (`DefaultPermissionEngine`) remain
  entirely outside this document's scope. Identity resolution is
  Section 2, steps 1-2's concern, already implemented, and not
  redefined here.
- **`ExecutionPipeline`** (`docs/specifications/volume-03-core-interfaces/ExecutionPipeline.md`,
  `src/runtime/DefaultExecutionPipeline.kt`) remains entirely outside
  this document's scope. Execution is Section 2, step 5's concern --
  this document defines what must be true before execution may proceed,
  not how execution itself proceeds.
- **AD-007 (Permission Decisions Belong to the Permission Engine).**
  This document is a specialisation of AD-007's decision that "subsystems
  never self-authorise" and that every `ExecutionRequest` is evaluated
  exactly once by `PermissionEngine.evaluate`. Nothing in this document
  introduces a second evaluation authority.
- **AD-015 (Invalid Is Not Denied).** Section 7's "unknown action/resource/permission
  → DENIED" defaults describe genuine policy denials -- Permission
  Policy is reached only after a request has already passed action-mapping
  and resource-resolution validation (`docs/architecture/action-mapping.md`),
  so an "unknown" case at this layer is a policy gap, not a malformed
  request. This document does not blur that distinction; a structurally
  invalid request never reaches Permission Policy at all.

## 11. Out of Scope

The following are explicitly excluded from this document and remain
later work:

- Role implementation
- Policy storage
- Policy editing
- Android UI
- Cryptographic identity
- Delegated authority
- Organisation policy
- Temporary permissions
- Plugin permissions

## 12. Future Considerations

Without specifying behaviour, likely future extensions include: a
concrete risk-scoring mechanism to replace Section 6's qualitative
levels where warranted; a role- or attribute-based rule model per
Section 9; incorporation of the Environmental Context named in
Section 3 once a concrete signal source exists; and delegated or
temporary permission models, once Section 11's exclusions are
individually taken up as their own specification work.

Future policy models may also consider declared execution intent
alongside the requested action. Multiple Actions may eventually
contribute to a single higher-level Intent, and a future policy model
may evaluate that relationship rather than judging each Action in
isolation. This is not a today's-policy input (Section 3 is
unchanged), it introduces no new contract, and it is noted here only as
a future direction, not as behaviour this document specifies.

## Non-Goals

Permission Policy is not responsible for, and never becomes responsible
for, any of the following -- these are architectural responsibility
boundaries, not features awaiting implementation:

- Permission Policy does not infer user intent.
- Permission Policy does not perform reasoning.
- Permission Policy does not plan workflows.
- Permission Policy does not resolve identity.
- Permission Policy does not execute actions.
- Permission Policy does not modify Resources.

## Related

- `docs/architecture/parker-constitution.md`
- `docs/specifications/volume-03-core-interfaces/PermissionEngine.md`
- `docs/architecture/IdentityService.md`
- `docs/specifications/volume-03-core-interfaces/ExecutionPipeline.md`
- `docs/architecture/ARCHITECTURE_DECISIONS.md` (AD-007, AD-015)
- `docs/architecture/IMPLEMENTATION_GAPS.md` #25 (open -- this document
  is the prerequisite for closing it), #40 (closed by Sprint 2, Unit A1)
- `docs/implementation/SPRINT_2_IMPLEMENTATION_PLAN.md` (Unit A2)
- `docs/reviews/SPRINT_2_A1_CHECKPOINT.md`
