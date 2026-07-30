# CDR-005: Constitutional Admission of PermissionEngine Proposal Classes

## Status

**Draft.** This record is not Accepted, not Canonical, and not Frozen. It
has not yet undergone the independent constitutional verification and
Final Freeze Verification cycle this Programme has applied to every
other governance artefact (Amendments 1, 2, and 5; the Chapter 10
repair itself). No Kotlin is implemented, proposed, or changed. Neither
`src/` nor `tests/` is touched. `docs/architecture/10-permission-engine.md`
and `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`
are both unmodified by this record. Nothing is staged, committed, or
pushed.

---

## Context

`docs/architecture/10-permission-engine.md` ("Chapter 10") was recently
expanded from a two-line stub into a draft constitutional governance
document for the Permission Engine, in response to the Amendment 3
PermissionEngine Ownership Resolution Report's finding that no existing
document adequately defined PermissionEngine's own constitutional
contract, and that Amendment 3 (Knowledge Submission Permission Gating)
could not safely be drafted until that gap was closed.

An independent constitutional verification of the resulting draft
(Chapter 10 Permission Engine Constitutional Verification Report)
classified it Constitutionally Incomplete and found, among three
findings, one requiring escalation: Chapter 10's own Section 10
("Extensibility") asserted, without repository support, that a domain's
governance document may map its own act onto Chapter 10's proposal
abstraction "directly... without requiring this chapter itself to be
reopened each time." The verification found this assertion in tension
with the Ownership Resolution Report's own Structure C conclusion
("first complete or replace the PermissionEngine governance document,
then amend **both** contracts") and identified a genuinely plausible
competing model — that recognising a new proposal class should instead
require a corresponding amendment to Chapter 10 itself, preserving
explicit Trust Framework sign-off over each expansion of what
PermissionEngine evaluates.

This decision is constitutionally material because it determines, for
Amendment 3 and for every future domain that will eventually require
permission gating, what governance action is required before a domain's
own act may lawfully be evaluated by PermissionEngine — a question that
bears directly on the no-bypass guarantee (an act gated too loosely, or
never formally recognised, risks silently evading trust authorisation)
and on the sole-authority guarantee (an admission procedure that lets a
domain expand PermissionEngine's own scope without any check risks a
domain effectively legislating for the Trust Framework layer it does not
own).

No existing document resolves this question. The Parker Constitution
states the Permission Engine's sole authority and the no-bypass
principle, but does not address how new proposal classes are recognised.
`docs/architecture/09-trust-framework.md` and
`docs/architecture/AUTHENTICATION_AND_TRUST_GOVERNANCE.md` do not discuss
proposal classes at all. `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`
and `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`
each define their own domain's acts but neither addresses the general
question of how a domain act becomes a recognised PermissionEngine
proposal class in the first place. This record reviewed all of the
above, together with `docs/reviews/PROGRAMME_3_CONSTITUTIONAL_AUDIT.md`,
`docs/reviews/PROGRAMME_3_BLOCKER_OWNERSHIP_MATRIX.md`,
`docs/governance/PROGRAMME_3_CONSTITUTIONAL_REMEDIATION_ROADMAP.md`, the
Amendment 3 PermissionEngine Ownership Resolution Report, the
PermissionEngine Governance Document Repair Report, the Chapter 10
Permission Engine Constitutional Verification Report, and CDR-001
through CDR-004 in full.

---

## Decision Question

> Once Chapter 10 establishes PermissionEngine's general constitutional
> contract, what governance action is required to recognise a new
> domain act as a PermissionEngine proposal class?

---

## Constitutional Constraints

Any answer to the Decision Question must remain consistent with:

- **Sole authority.** "The Permission Engine is the sole authority for
  turning a proposal into an authorized action" (Constitution). No
  admission model may create, or come to function as, a second authority.
- **No bypass.** "There is no path from proposal to execution that does
  not pass through the Permission Engine" (Constitution). An admission
  model must not permit a consequential domain act to remain
  unrecognised, and therefore effectively ungated, by default or by
  omission.
- **Owner control and Trust Framework ownership.** "Parker owns
  authority. Modules provide capability" (Constitution); PermissionEngine
  is constitutionally owned by the Trust Framework (Chapter 9; Chapter
  10 §2), and no domain governance document may redefine that ownership.
- **Fail-closed under uncertainty.** "Uncertainty about trust never
  defaults to permissiveness. It defaults to inaction" (Constitution).
  Ambiguity about whether an act requires gating must never resolve in
  favour of treating it as ungated.
- **Modular capability without renegotiating the trust model.** "Parker's
  capability grows through modules... without requiring changes to the
  trust model each time" (Constitution). An admission model that requires
  Chapter 10 to be reopened for every new domain act, regardless of
  whether anything about PermissionEngine's own contract changes, is in
  tension with this principle.
- **Governance pattern: general criteria governing a category without
  reopening the document per instance.** CDR-004 offers a persuasive
  structural analogy, not binding authority, for this shape of problem:
  it shows that a frozen document's general criteria ("exactly seven
  [retrieval] modes... everything else excluded") can govern an entire
  category of behaviour — operation *shapes* — without being reopened
  each time a new specific instance (a new record kind reachable through
  an existing shape) arises, provided the general criteria are stated
  explicitly and a disclosed escalation path exists for genuinely
  contested cases. This pattern is informative, but does not by itself
  resolve the present question: CDR-004's own implementation (Memory
  Core Scope Lock's Section 19) stayed within one document amending
  itself — Memory Core Scope Lock classified its own gap and then
  extended its own text — and did not test a domain document
  self-certifying against a *different* document owned by a *different*
  constitutional layer, which is what Model C (below) requires. The
  cross-owner self-certification mechanism this record adopts is
  accordingly this record's own constitutional decision, informed by,
  but not compelled by, CDR-004.

---

## Options Considered

### Model A — Domain Mapping Model

A domain-owned governance document maps its own act onto Chapter 10's
proposal abstraction unilaterally, without any further check.

**Strengths:** maximally modular; avoids reopening Chapter 10; keeps
implementation-level request types out of constitutional inventory;
respects each domain's authority to define its own acts.

**Constitutional risks:** the domain is both defendant and judge of its
own classification — nothing requires it to test its act against
Chapter 10's own published criteria, nothing catches an under-classified
act that quietly evades gating, and nothing prevents two domains from
producing inconsistent or overlapping classifications with no shared
reference point. This is a direct instance of the "hidden changes to
constitutional gating boundaries" and "gradual widening... without Trust
Framework review" risks already identified.

**Consequences for Amendment 3:** Contract Design V2 §7 alone would
recognise Knowledge Candidate submission as a proposal class, with no
required cross-check against Chapter 10's own criteria and no escalation
path if the classification were genuinely doubtful.

**Scalability:** scales without friction, but scales the risk
identically — a higher-consequence future domain (for example, financial
or security-sensitive operations) could self-classify under-cautiously
with the same lack of check as a low-consequence one.

### Model B — Dual-Amendment Model

Every new proposal class requires both a domain-governance amendment
defining the act and a corresponding Chapter 10 amendment admitting it.

**Strengths:** explicit, uniform Trust Framework sign-off on every
expansion of PermissionEngine's scope; strongest possible protection
against unilateral domain expansion; produces, incidentally, a complete
enumeration of recognised proposal classes within Chapter 10 itself.

**Constitutional risks:** requires reopening a foundational,
general-purpose document for every new domain act, however
uncontestedly it fits Chapter 10's own already-published criteria — a
degree of churn this Programme's own additive-only, minimal-reopening
discipline has consistently avoided elsewhere (Amendments 1, 2, and 5
were each specifically designed not to reopen other documents' frozen
text). Risks Chapter 10 drifting from a foundational contract into a
growing registry of narrow, domain-specific admissions, diluting its own
status as general governance. Directly contradicts the Constitution's
own "without requiring changes to the trust model each time" modularity
principle.

**Consequences for Amendment 3:** requires two amendments — Contract
Design V2 §7 and a fresh Chapter 10 amendment — even though Knowledge
Candidate submission's own consequence (recording durable material,
downstream of Evaluation A's own already-approved evidence) is not
obviously different in kind from acts Chapter 10's general criteria
should already be capable of classifying.

**Scalability:** scales at a fixed, non-diminishing administrative cost
per domain, regardless of how routine or how genuinely novel each new
act is — a cost this record finds disproportionate once Chapter 10's own
criteria are properly stated (the corrective work Finding 1 already
identifies as necessary regardless of this decision).

### Model C — Governed Admission Model

Chapter 10 states general, explicit admission criteria (what kind of act
requires gating) once. A domain's own governance document defines its
act and self-certifies that classification against Chapter 10's
published criteria, in an explicit, disclosed section of its own
amendment — following this Programme's own established
"Constitutional Consistency Check" convention. Where that self-
certification is genuinely unclear, contested, or would require choosing
between two or more constitutionally plausible readings of Chapter 10's
criteria, the domain does not resolve the question itself — it is
escalated to a CDR, exactly as Amendment 2's own provenance-lookup
classification was escalated to, and resolved by, CDR-004. Chapter 10
itself is reopened only when a constitutional section it itself
states — its sole-authority rule, ownership assignments, Runtime
relationship, guarantees, boundaries, permission outcomes, failure
semantics, or general admission criteria — must themselves change,
never merely because a new domain act, cleanly fitting existing
criteria, is being recognised.

**Strengths:** reuses two mechanisms this repository and Programme
already possess and have already used repeatedly and successfully — the
additive domain-amendment pattern and the CDR escalation pattern — rather
than inventing new governance machinery. Draws a persuasive structural
analogy from CDR-004's own resolution of a similarly shaped problem
(general criteria governing a category; specific instances handled
without reopening the general document; escalation available for
genuine doubt) — CDR-004 supports the general principle, though its own
implementation stayed within one document amending itself, and does not
itself establish or test the cross-document, cross-owner self-
certification this record adopts; that mechanism is this record's own
constitutional decision. Preserves Trust Framework visibility without requiring
uniform per-domain sign-off: an uncontested classification is
self-evident and disclosed in the domain's own document; a contested one
receives exactly the scrutiny Model B would have applied to every case,
but only where that scrutiny is actually needed.

**Constitutional risks:** depends on Chapter 10's admission criteria
being stated with genuine precision (Finding 1's own correction is a
precondition for this model to function safely) — a vague criterion
would let Model C degrade toward Model A's risk profile. Depends on
domains actually using the CDR escalation path honestly, rather than
self-certifying through genuine ambiguity — the same good-faith
assumption this Programme's entire drafting-review-correction cycle
already rests on for every other constitutional artefact.

**Consequences for Amendment 3:** Contract Design V2 §7 amends to define
Knowledge Candidate submission and self-certifies it against Chapter
10's (corrected) admission criteria. No separate Chapter 10 amendment is
required unless that self-certification proves genuinely contested, in
which case a further CDR — not a Chapter 10 amendment — is the correct
path.

**Scalability:** scales proportionately — routine, clearly-qualifying
future domain acts (for example, an ordinary Memory Core write) impose
no Chapter-10-reopening cost. Financial, security-sensitive, or other
high-consequence future operations are expected, as a practical matter,
to more often produce genuinely contested classifications warranting
CDR-level review, consistent with Authentication & Trust Governance's
own risk-scaling concept (§7). This expectation is not itself a
constitutional guarantee, and this record does not create a mandatory
risk-tier-to-escalation rule: escalation for any domain, high-risk or
otherwise, remains governed solely by the established CDR
threshold — genuine contest or ambiguity against Chapter 10's
criteria — never by domain type alone.

This model was not selected as a compromise between Models A and B for
its own sake. It rests on two distinct grounds. First, CDR-004 supplies
a persuasive structural analogy — not direct constitutional authority,
and not a precedent this record simply applies unchanged — for the
general principle that explicit criteria can govern an entire category
without reopening the document that states them, given a disclosed
escalation valve for genuine doubt. Second, and independently of that
analogy, this model is the one most consistent with the Constitution's
own modularity principle ("Parker's capability grows through
modules... without requiring changes to the trust model each time") and
with this Programme's own additive-only, minimal-reopening discipline,
applied consistently across Amendments 1, 2, and 5. The cross-document,
cross-owner self-certification mechanism itself — a domain document
certifying against a different document owned by a different
constitutional layer — is not something CDR-004 tested or settled; it is
this record's own constitutional decision, adopted because it best
satisfies the Constitutional Constraints above, not because CDR-004
already required it.

---

## Decision

```
Model A — Rejected
Model B — Rejected
Model C — Governed Admission — Adopted
```

---

## Decision Rules

- **Who defines a domain act:** the domain's own governance document
  (for example, a future Contract Design V2 amendment defining Knowledge
  Candidate submission).
- **Who recognises it as a proposal class:** the domain's own document,
  by explicit, disclosed self-certification against Chapter 10's
  published general admission criteria — not Chapter 10 itself, and not
  the domain acting outside a disclosed, reviewable certification.
- **Whether Chapter 10 must be amended:** not for the routine case of a
  domain act that fits Chapter 10's existing, already-corrected admission
  criteria without genuine dispute. Chapter 10 must be reopened only when
  a constitutional section it itself states — its sole-authority rule,
  ownership assignments, Runtime relationship, guarantees, boundaries,
  permission outcomes, failure semantics, or general admission
  criteria — is materially altered, not each time a new domain act is
  classified under criteria that already exist unaltered.
- **What documentation is required:** every newly introduced act with a
  real, external, or state-changing consequence — not ordinary internal
  computation — must receive an explicit constitutional classification in
  the domain's own governance document, stating whether the act is a
  PermissionEngine proposal class or is ordinary internal computation
  outside PermissionEngine's scope. Whichever classification applies, the
  domain's document must cite the specific Chapter 10 admission criterion
  relied upon and disclose the constitutional reasoning supporting that
  classification — following this Programme's established Constitutional
  Consistency Check convention, so neither a positive nor a negative
  classification is asserted or omitted silently. This obligation applies
  only to newly governed consequential acts; it does not require
  documentation of trivial or already-settled internal computation.
- **When a CDR is required:** whenever a domain's self-certification
  against Chapter 10's criteria is genuinely contested, ambiguous, or
  would require choosing between two or more constitutionally plausible
  readings — mirroring exactly the standard this Programme already
  applies to every other constitutional ambiguity (see CDR-001 through
  CDR-004).
- **When a domain amendment alone is sufficient:** whenever the
  classification against Chapter 10's published criteria is not genuinely
  contested — the ordinary case this Programme expects to be the
  majority of future domain acts.
- **When Chapter 10 must be reopened:** whenever any constitutional
  section Chapter 10 itself states — its sole-authority rule, ownership
  assignments (constitutional owner, operational caller, enforcement
  owner, and policy source), Runtime relationship, guarantees,
  boundaries, permission outcomes, failure semantics, or general
  admission criteria — is materially altered. This is limited to material
  alteration of Chapter 10's own constitutional content, not
  implementation-level change; it is never triggered merely by the
  addition of a domain act that already fits existing, unaltered
  criteria.

This record remains technology independent throughout: no request
representation, interface, Kotlin type, or data shape is prescribed or
implied by any of these rules.

---

## Constitutional Visibility

Model C does not require a single, central enumeration of every
recognised PermissionEngine proposal class, and this record deliberately
does not create one. Visibility is instead preserved distributively,
consistent with how this Programme has already made every other
capability independently discoverable: each domain's own governance
document states its own recognised proposal classes, together with the
explicit, criteria-cited classification the corrected Decision Rules
above now require for every newly governed consequential act — whether
that act is classified as a proposal class or as ordinary internal
computation outside PermissionEngine's scope. A reviewer can
independently verify any single domain's compliance by reading that
domain's own document against Chapter 10's published criteria, without
needing a separate registry. Any classification a reviewer finds
doubtful remains open to challenge through the same CDR escalation path
this record already establishes, producing, over time, a disclosed,
canonical record of every contested classification — CDR-004 is itself
precedent for exactly this kind of disclosure — without requiring
Chapter 10 to maintain a running list of its own.

A mandatory central registry was considered and rejected. It would
function as a lightweight version of Model B's own rejected churn:
requiring Chapter 10, or some other single document, to be kept current
against every domain's own independent amendment schedule, without
adding any protection beyond what distributed, criteria-cited disclosure
and CDR escalation already provide.

---

## Consequences

- **Chapter 10 correction:** Finding 1 (the proposal-gating boundary)
  must be corrected as this record's own precondition — Model C's safety
  depends on Chapter 10's admission criteria being stated with genuine
  precision, not on this record substituting for that correction. Finding
  3 (risk-scaling cross-reference) is unaffected by this decision and
  remains an independent correction. Section 10's own extensibility text
  should be corrected to state Model C's rule precisely, replacing its
  prior, unsupported blanket assertion.
- **Amendment 3:** proceeds by amending Contract Design V2 §7 to define
  Knowledge Candidate submission and self-certify it against Chapter 10's
  (corrected) admission criteria. No separate Chapter 10 amendment is
  required by default; a further CDR is the correct path only if that
  self-certification proves genuinely contested when actually drafted
  and reviewed.
- **Future domain contracts:** each future domain requiring permission
  gating (Memory Core's own already-precedented boundary; a future
  Home Assistant, communication, or financial/security-sensitive
  capability) follows the same rule — define the act, self-certify
  against Chapter 10's criteria, escalate to a CDR only if genuinely
  contested.
- **Runtime:** unaffected. Runtime remains the sole operational caller
  and enforcer of PermissionEngine decisions, regardless of how a
  proposal class was admitted.
- **PermissionEngine specifications:** `docs/specifications/volume-03-core-interfaces/PermissionEngine.md`
  remains non-authoritative implementation evidence, unaffected by this
  decision; how any newly recognised proposal class is eventually
  represented at the implementation level is expressly not decided here
  (Non-Decisions, below).
- **Auditability:** preserved distributively, per Constitutional
  Visibility above — each domain's own self-certification (positive or
  negative) is itself a disclosed, reviewable governance artefact, and
  any contested classification produces its own canonical CDR, exactly as
  CDR-004 already did for Amendment 2.
- **Owner control:** unaffected and reinforced — no domain gains any
  authority to expand PermissionEngine's own scope unilaterally in a way
  that survives independent review; the fail-closed principle governs any
  unresolved classification.

---

## Rejected Alternatives

**Model A** is rejected because it permits a domain to both define and
classify its own act with no check against Chapter 10's own criteria and
no required escalation path for doubtful cases — risking exactly the
silent scope-widening and inconsistent classification the option's own
risk profile identifies, with no mechanism this record could point to
that would catch an under-classified, and therefore under-gated, act
before it caused harm.

**Model B** is rejected because it imposes uniform, permanent
constitutional churn on a foundational document disproportionate to the
actual risk once Chapter 10's own criteria are properly stated,
contradicts the Constitution's own "without requiring changes to the
trust model each time" modularity principle, and departs from this
Programme's own consistently applied additive-only, minimal-reopening
discipline without a repository-grounded reason to make an exception for
PermissionEngine specifically.

---

## Non-Decisions

CDR-005 does not decide, and no future reader should treat it as having
decided:

- any implementation interface, Kotlin type, or method signature for
  PermissionEngine or any request representation;
- whether or how `ExecutionRequest` is extended, replaced, or
  supplemented at the implementation level;
- Knowledge Candidate submission's own specific fields, shape, or
  wording — that remains Amendment 3's own drafting task, not begun by
  this record;
- Runtime's own wiring or composition-root sequencing;
- the content of any permission policy (what is actually approved,
  denied, deferred, or conditioned for any specific action);
- the risk-level-to-trust-level mapping Authentication & Trust
  Governance §7 already defers to future policy work;
- Chapter 10's own Finding 1 or Finding 3 corrections — this record only
  establishes the precondition and constraint those corrections must
  satisfy, it does not perform them;
- any wording of Amendment 3 itself.

---

## Verification Criteria

A future reviewer may confirm this decision has been followed by
checking that:

- any governance document introducing a newly governed consequential act
  contains an explicit, disclosed classification of that act — as a
  PermissionEngine proposal class, or as ordinary internal computation
  outside PermissionEngine's scope — citing Chapter 10's own published
  admission criteria by section reference, for either outcome;
- Chapter 10 has not been reopened for the sole reason of recognising a
  routine domain act that already fits its published criteria without
  contest;
- any domain-act classification that was genuinely contested or
  ambiguous was resolved by a dedicated CDR before the domain's own
  amendment proceeded, not silently resolved inside the domain's own
  document;
- no domain-owned document purports to grant itself PermissionEngine
  authority, construct an alternate permission mechanism, or declare a
  newly governed consequential act exempt from gating by silence or
  omission rather than by a disclosed, criteria-tested classification.

---

## Final Report

**Document created:** `docs/decisions/CDR-005_CONSTITUTIONAL_ADMISSION_OF_PERMISSIONENGINE_PROPOSAL_CLASSES.md`
(only file created by this task; no other file modified).

**Decision:** Model C — Governed Admission — Adopted; Models A and B
rejected.

**Status:** Draft, pending independent constitutional verification and
Final Freeze Verification, consistent with this Programme's established
lifecycle for every other governance artefact.

CDR-005 DRAFTED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW

Confirmed: no production code modified; no tests modified;
`docs/architecture/10-permission-engine.md` not modified;
`docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` not
modified; CDR-001 through CDR-004 not modified; only this new draft
decision record created; nothing staged; nothing committed; nothing
pushed; Amendment 3 not started; Chapter 10's Findings 1 and 3 not
corrected.
