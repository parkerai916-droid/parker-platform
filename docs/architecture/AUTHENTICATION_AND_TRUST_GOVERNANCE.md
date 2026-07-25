# Authentication & Trust — Governance

## Status

**Governance only. No Kotlin, no interfaces, no contracts, no tests, no
dependency or build changes, no API changes, and no TODO implementations
exist anywhere in this document or arise from it.** This document is
mergeable immediately, without affecting the current runtime in any way
-- it changes nothing under `src/` or `tests/`, and nothing about
`ConversationEngine`, Conversation History, or any other existing
component. **Status update, documentation reconciliation pass following
Sprint 11 Unit 6's acceptance:** at the time this document was originally
written, Conversation History was the next implementation milestone; it
has since been implemented and accepted as Conversation History Source
(Sprint 11 Unit 6, commit `ad21659`, verified BUILD SUCCESSFUL). This
document did not move it, precede it in implementation order, or compete
with it for engineering attention then, and makes no claim about
implementation sequencing now -- what (if anything) follows Conversation
History Source is a separate project decision, not settled by this
document.

**Success criterion.** This document's success is measured by clarity of
responsibility, not completeness of functionality. It reserves
architectural space for Authentication & Trust so that a future
implementation Unit has a constitutional boundary to build against -- it
does not attempt to describe, anticipate, or partially build that
implementation. A governance document that said less than this would
leave the boundary undefined when sensitive capabilities arrive; a
governance document that said more would be implementation wearing
governance's name. This one is scoped to say exactly as much as
responsibility-clarity requires, and no more.

---

## 1. Purpose

Parker began as a conversational runtime: an owner speaks, Parker reasons,
Parker replies. Every capability built so far -- Communication Intake,
the Conversation Engine, the Reasoning Provider, Response Composition and
Delivery -- exists to serve that one loop, and every action available to
it today is limited to notifying the owner through a channel the owner
already controls.

Parker is evolving beyond that loop. The roadmap this repository's own
`IMPLEMENTATION_GAPS.md` and Sprint history describe -- voice
identification, Home Assistant control, email, calendars, and other
external actions -- names a different kind of system: one that can act on
the owner's behalf against real external systems, with real consequences.
A conversational runtime that only replies in text can be conservative
about identity almost by accident, because the cost of a mistaken
inference is a wrong sentence. A platform that can unlock a door, send an
email, or move money cannot afford that same accident.

**Authentication & Trust exists to answer one question, before that
maturation happens, not after: how should Parker determine trust before
allowing sensitive actions?** This document answers it architecturally.
It does not implement voice recognition, two-factor authentication,
biometrics, passkeys, or any other authentication mechanism -- it defines
where the boundary for all of them will live, what it will and will not
be responsible for, and how it relates to the constitutional invariants
Parker already operates under (`docs/architecture/parker-constitution.md`).
The purpose is to establish this boundary while it is still cheap to get
right -- before any sensitive capability exists to make getting it wrong
expensive.

---

## 2. Design Goals

- **Model independent.** Authentication & Trust must never depend on, or
  be implemented inside, a reasoning model. Whatever model Parker uses
  today or in the future, this boundary's own behaviour does not change.
- **Runtime owned.** Authentication & Trust is a runtime responsibility,
  exercised by Parker's own owned components, exactly as Identity, Trust
  authorisation, and Execution already are (constitution: "Parker owns
  authority. Modules provide capability.").
- **Deterministic.** Given the same evidence, the same trust determination
  results, every time -- no probabilistic or model-driven judgment calls
  decide whether an entity is trusted.
- **Auditable.** Every trust determination must be traceable to the
  evidence that produced it.
- **Extensible.** New sources of authentication evidence must be
  addable without redesigning this boundary's own architecture.
- **Fail safe.** Uncertainty or absence of evidence must never resolve in
  favour of granting trust.
- **Risk aware.** The trust required for an action must scale with that
  action's own consequence, not be fixed at one blanket level for
  everything Parker can do.
- **Future compatible.** This boundary must accommodate capabilities that
  do not exist yet (voice, trusted devices, passkeys, and others named in
  Section 9) without requiring its own constitutional principles to be
  revised when they arrive.

---

## 3. Non-Goals

This document explicitly excludes, and authorises no future Unit to treat
its own existence as license for:

- Voice recognition implementation.
- Two-factor authentication (2FA) implementation.
- Biometric implementation.
- Passkeys.
- Trusted devices.
- Network verification.
- Location verification.
- Cryptography.
- Authentication providers.
- OAuth.
- Any implementation work of any kind.

Every item above is a legitimate future capability (Section 9 names
several of them again, explicitly as examples, not commitments). None of
them is designed, chosen, or scheduled by this document. This document's
own scope ends at the constitutional boundary; realising any one of these
is a separate, future, Contract-Design-and-Implementation-Plan governed
Unit in its own right.

---

## 4. Core Principles

- **Authentication is separate from reasoning.** A reasoning provider
  interprets what an owner is asking for; it never interprets, verifies,
  or influences who is asking.
- **Reasoning cannot grant permissions.** Consistent with, and a direct
  extension of, the constitution's own "Cognition proposes. Trust
  authorises. Runtime executes." -- reasoning's mandate remains proposal
  only. Authentication adds a stage even earlier than that: reasoning
  cannot grant trust any more than it can grant authorisation.
- **Sensitive actions require trust evaluation.** The more consequential
  an action, the more this boundary requires of the trust behind it
  (Section 7).
- **Authentication evidence may come from multiple sources.** No single
  signal is assumed sufficient for every risk level; this boundary is
  designed to combine evidence, not to anoint one mechanism as
  authoritative for all purposes.
- **Authentication confidence is independent of conversation.** How
  confident Parker is in an entity's identity is not a property of what
  that entity is saying, how it is phrased, or how the conversation is
  going -- it is a property of the evidence presented, evaluated on its
  own terms.
- **Every security decision must be explainable.** A trust or
  authorisation outcome that cannot be explained is not a decision this
  boundary is permitted to produce.
- **Failure defaults to deny.** Absent evidence, ambiguous evidence, or a
  fault anywhere in evaluating it, the safe outcome is always the one
  that withholds trust, never the one that grants it.

---

## 5. Authentication Concepts

Defined here at a governance level only -- as concepts this boundary is
responsible for, not as chosen implementations, data shapes, or
algorithms:

- **Authentication.** The process of gathering and evaluating evidence
  that a given identity claim is genuine.
- **Identity.** Who is being claimed -- distinct from whether that claim
  is currently believed (that is Confidence, below). Parker's existing
  `IdentityService` already models identity structurally, as a
  `Principal`; this document does not redefine that model, and does not
  require it to change.
- **Trust.** The state of having established sufficient confidence in an
  identity claim to permit it to proceed toward authorisation.
- **Confidence.** How strongly the available evidence supports a given
  identity claim, at a given moment -- not a fixed property of the
  identity itself, and not permanent.
- **Authorisation.** The decision of whether a specific, already-trusted
  identity may perform a specific action. This is not a new concept this
  document introduces -- it is the existing constitutional responsibility
  already held by the Permission Engine ("Trust authorises," per the
  constitution). Authentication & Trust is upstream of it, supplying a
  richer basis for that existing decision to be made against; it does not
  rename, relocate, or duplicate the Permission Engine's own authority.
- **Policy.** The governed rule set determining how much confidence, and
  from which evidence, is required before trust is granted for a given
  risk level.
- **Approval.** An explicit act, by an owner or an authorised party,
  affirmatively permitting an action -- one possible source of evidence
  among others, not the only one.
- **Risk.** The consequence of a given action if performed by an entity
  that should not have been trusted to perform it (Section 7).

---

## 6. Trust Levels

Conceptual levels only -- no numerical scale, no scoring formula, and no
mapping to any specific evidence source is defined here:

- **Unauthenticated.** No claim of identity has been evaluated, or none
  has been made.
- **Low Confidence.** An identity claim exists but is weakly supported.
- **Authenticated.** An identity claim is adequately supported for
  ordinary purposes.
- **Strongly Authenticated.** An identity claim is supported by evidence
  materially stronger than ordinary authentication.
- **Recently Verified.** A previously strong authentication whose
  strength is understood to diminish with elapsed time, distinct from a
  static "Strongly Authenticated" state.

These names describe conceptual distinctions a future policy may use to
decide what is required for a given risk level (Section 7) -- they are
not a specification of how confidence is measured or combined.

---

## 7. Risk Levels

Conceptual only, describing the consequence of an action, not how
authentication for it is implemented:

- **Informational.** Providing information with no external effect.
- **Routine.** Ordinary, low-consequence actions.
- **Sensitive.** Actions with real, but bounded or reversible,
  consequence.
- **High Risk.** Actions with significant, potentially irreversible
  consequence.
- **Critical.** Actions whose consequence is severe and immediate.

**Required authentication increases with risk.** An Informational request
may proceed with little or no authentication; a Critical action must
demand the strongest trust level this boundary is capable of requiring.
This document does not assign specific trust levels to specific risk
levels, or specific actions to specific risk levels -- that mapping is
policy, deferred to future work (Section 11).

---

## 8. Architectural Position

Conceptual flow only -- this is not a call sequence, not a set of
interfaces, and not a commitment to any particular component boundary
beyond the ordering itself:

```
Communication
    v
Authentication
    v
Identity
    v
Authorisation
    v
Conversation
    v
Reasoning
    v
Tool Execution
    v
Response
```

Authentication sits immediately after Communication and before Identity
is treated as established for the purposes of anything downstream --
consistent with this document's own Core Principles (Section 4):
authentication must occur before authorisation, and authorisation must
occur before execution. **Authorisation**, in this flow, is the existing
Permission Engine stage the constitution already defines ("Trust
authorises") -- this document does not introduce a second, competing
authorisation stage; it places a new, upstream Authentication stage ahead
of the existing one, giving it richer evidence to authorise against.
**Conversation** and **Reasoning** are `ConversationEngine` and the
Reasoning Provider, exactly as they exist today, entirely unaltered by
this document.

This flow is conceptual only. It does not require, imply, or authorise
any change to `ParkerRuntime.submitOwnerMessage`'s own existing sequence,
to `ConversationEngine`, to Conversation History, or to any other
existing component's actual call ordering. Realising this flow in real
code is future implementation work, not decided or begun here.

---

## 9. Future Sources of Authentication

Examples only. None is implemented, chosen, prioritised, or scheduled by
this document:

- Voice.
- Trusted Device.
- Passkey.
- PIN.
- Authenticator.
- Location.
- Network.
- Explicit Confirmation.

Consistent with Design Goal "Extensible" (Section 2) and Core Principle
"Authentication evidence may come from multiple sources" (Section 4),
this boundary's architecture must be capable of accommodating any of
these, or others not yet named, without requiring this document's own
principles to be revised.

---

## 10. Failure Behaviour

- **Authentication failure does not invoke reasoning.** A failure at this
  stage stops before Conversation and Reasoning are ever reached,
  mirroring the discipline this codebase's own coordinators already
  apply elsewhere: a structural failure is not silently smoothed over by
  proceeding anyway.
- **Sensitive actions fail closed.** Absent sufficient trust for a given
  risk level, the action does not proceed -- there is no degraded,
  partially-trusted execution path.
- **Authentication uncertainty is never hidden.** A low- or
  unresolved-confidence state is surfaced, not silently treated as if it
  were resolved in either direction.
- **All denials are explainable.** A denial this boundary produces must
  be traceable to the specific insufficiency of evidence or policy that
  caused it (Core Principle, Section 4).

---

## 11. Future Work

**Implementation is deferred in full.** This document authorises no
Kotlin, no interface, no contract, and no test. It exists so that, when a
future Unit is chartered to begin realising Authentication & Trust, that
Unit inherits a constitutional boundary already settled, rather than
having to settle it under the pressure of a specific capability already
half-built. Possible future implementation Units, named here as
examples of scope this document reserves space for, not as a committed
sequence:

- Authentication Contracts
- Authentication Context
- Voice Identity
- Trusted Devices
- 2FA
- Security Policies
- Risk Engine
- Audit Log

**Conversation History was the next implementation milestone at the time
this document was written; it has since been implemented and accepted as
Conversation History Source (Sprint 11 Unit 6).** This document did not
change that sequencing then, and does not assert any sequencing now --
nothing above schedules Authentication & Trust ahead of, or in
competition with, whatever project decision determines what follows
Conversation History Source.

---

## Constraints

This governance document, and everything that follows from it until a
future Contract Design says otherwise:

- Does **not** modify any existing architecture.
- Does **not** require changes to current components.
- Does **not** alter Conversation History (implemented and accepted as
  Conversation History Source, Sprint 11 Unit 6, per
  `docs/implementation/CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md`; this
  document still does not touch it).
- Does **not** alter `ConversationEngine`.
- Does **not** introduce implementation dependencies.
- Does **not** introduce breaking changes.

---

## Relationship to Existing Constitution

This document is an extension of Parker's existing architectural
constitution (`docs/architecture/parker-constitution.md`), not a
replacement of it. The constitution's two governing invariants --
"Parker owns authority. Modules provide capability." and "Cognition
proposes. Trust authorises. Runtime executes." -- remain exactly as
written and exactly as binding. This document adds one further
constitutional layer beneath them: before Trust can authorise, and before
Cognition can even be reached, Parker must be able to say how confident
it is in who is asking. Its purpose is to establish constitutional
principles for Authentication & Trust before implementation begins --
not to reinterpret, weaken, or supersede any principle the constitution
already states.

---

## Deliverable

A governance document only, suitable for inclusion in the Parker
repository alongside its other permanent architectural governance
documents -- the constitution
(`docs/architecture/parker-constitution.md`), `PARKER_ENGINEERING_STANDARD.md`,
and `PROJECT_GOVERNANCE.md` -- at
`docs/architecture/AUTHENTICATION_AND_TRUST_GOVERNANCE.md`. No code, no
interfaces, no implementation, no tests, no roadmap changes. Conversation
History Source (Sprint 11 Unit 6) has since been implemented and
accepted; this document does not assert what, if anything, follows it.
