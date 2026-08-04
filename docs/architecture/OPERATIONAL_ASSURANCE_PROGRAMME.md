# Operational Assurance Programme

**Status:** Planned (Future Programme)
**Classification:** Architectural Evolution
**Priority:** Commences only after Memory Core reaches minimum capability and Evidence Intelligence reaches minimum operational capability.

This document is planning only. It does not alter any current implementation decision, does not change roadmap sequencing, and does not supersede the current roadmap. It records future architectural direction so that the work is not lost, re-derived, or improvised later — nothing in this document authorises any of it to begin now.

---

## 1. Purpose

Parker's architecture, as built so far, is aimed at **constitutional correctness**: every capability traces its authority to the Permission Engine, every action is gated, cognition never authorises itself, and every subsystem's boundary is fixed before it is implemented. This has been the right first target — a system cannot be trustworthy in practice if it is not trustworthy in principle. But constitutional correctness answers a narrower question than the owner actually needs answered day to day: *is this system built correctly?* It does not, by itself, answer: *can I rely on it right now, in front of me, today?*

That second question is **operational trustworthiness** — not a property of the architecture's design, but a property of the architecture's behaviour in the moment: whether the owner can see what Parker is doing, understand why, trust that failure is handled honestly rather than hidden, and recover without guesswork when something goes wrong. A system can be constitutionally flawless and still be operationally opaque — authorising every action correctly while giving the owner no way to observe, explain, or recover from what happens next.

The Operational Assurance Programme exists to close that gap. It does not add new authority, new capability, or new intelligence to Parker. It converts the constitutional principles Parker already has — owner control, transparency, auditability, fail-closed behaviour — into the practical, observable, everyday confidence an owner actually experiences while using the system. Where the Constitution states *what must always be true*, Operational Assurance is the programme that makes that truth **visible, explicable, and recoverable** in real operation.

---

## 2. Programme Goals

- **Operational visibility.** The owner can see, at any time, what Parker is currently doing, what it has recently done, and what state the system is in — without needing to inspect logs or code to find out.
- **Explainability.** Every action, decision, and refusal can be explained in terms an owner can understand, traced back to the specific proposal, authorisation, and governed fact that produced it.
- **Governed failure handling.** Failure is treated as an ordinary, anticipated condition with a defined, disclosed shape — never silently swallowed, never disguised as success, and never left for the owner to infer from absence.
- **Recovery.** When something fails, there is always a defined path back to a known-good state, and the owner is never left holding a system in an ambiguous or unrecoverable condition.
- **Explicit threat modelling.** The risks Parker's architecture is actually exposed to — not hypothetical ones — are named, assessed, and matched to a specific mitigating mechanism, rather than assumed to be covered implicitly by the Constitution's general guarantees.
- **Safe introduction of real tool execution.** Parker's eventual move from governed proposals to real, external, consequential tool execution is bounded by an explicit operational boundary, so that capability growth never outpaces the operational assurance needed to trust it.

---

## 3. Relationship to Existing Roadmap

Operational Assurance is sequenced deliberately, not incidentally:

- **It follows Memory Core and Evidence Intelligence.** Both must reach their own minimum operational capability first, because Operational Assurance is about making *real* operation trustworthy — there is nothing yet to observe, explain, or recover if the systems doing the work do not yet exist in working form.
- **It precedes expansion into plugins, automation, Home Assistant, Android, and large external integrations.** Every one of those expansions increases the surface area of things that can go wrong, be misunderstood, or need recovery. Building them before Operational Assurance exists would mean growing Parker's blast radius before the mechanisms that let an owner see, explain, and recover from that blast radius are in place.

This ordering is a planning statement, not a scheduling commitment. This document does not change when any currently-planned unit begins; it only records where Operational Assurance belongs once its own prerequisites are met.

---

## 4. Programme Units

### Unit 1 — Current Operational State

**Purpose.** Establish a single, accurate picture of what Parker's runtime is actually doing right now — active requests, in-flight executions, current Agent Runs, recent decisions — as a foundation every later unit in this Programme depends on.

**Deliverables.** A defined model of "current operational state," distinct from Memory, the World Model, and Reasoning Context; a way to query it that reflects the running system truthfully, not a cached or stale snapshot.

**Success Criteria.** An owner (or a future observability surface) can ask "what is Parker doing right now" and receive an answer that is true at the moment it is given, sourced from the runtime itself, never inferred or reconstructed after the fact.

### Unit 2 — Runtime Failure Taxonomy

**Purpose.** Give every kind of failure Parker's runtime can experience a named, closed, disclosed category, so that "something went wrong" is never the most specific thing the system can say.

**Deliverables.** A classification covering, at minimum, permission denial, resource unavailability, execution fault, timeout/expiry, and dependency failure — each distinguished from the others, none used as a catch-all for the rest.

**Success Criteria.** Every failure a real invocation can produce maps to exactly one category in the taxonomy, and no failure path in the runtime is left unclassified or defaulted into a generic "error."

### Unit 3 — Owner Observability

**Purpose.** Surface Unit 1's operational state and Unit 2's failure taxonomy to the owner directly, in a form that requires no technical interpretation.

**Deliverables.** An owner-facing view of current activity, recent history, and outstanding failures; a definition of what must always be observable versus what may remain internal.

**Success Criteria.** An owner can determine, unaided, what Parker has done recently and what state it is presently in, without reading logs, code, or developer-facing tooling.

### Unit 4 — Decision Explainability

**Purpose.** Make every authorisation decision, and every proposal that preceded it, reconstructible in plain terms after the fact — realising the Constitution's own auditability guarantee as an actual, usable capability rather than a structural promise.

**Deliverables.** A defined explanation shape covering what was proposed, by what authority it was evaluated, what the Permission Engine decided, and what was ultimately executed; a mechanism for producing that explanation from already-governed records.

**Success Criteria.** For any completed or denied action, an owner can ask "why did this happen" (or "why didn't this happen") and receive an answer built entirely from governed facts already recorded elsewhere in the system.

### Unit 5 — Threat Model

**Purpose.** Name, explicitly, the risks Parker's actual architecture is exposed to — not a generic security checklist — and match each to the specific mechanism that mitigates it.

**Deliverables.** A disclosed threat model covering, at minimum, compromised or misbehaving modules, reasoning-provider manipulation, permission-policy misconfiguration, and operational denial-of-service against the owner's own visibility into the system; for each, the existing or planned mechanism that bounds it.

**Success Criteria.** Every named threat has a named, verifiable mitigation; no threat is left implicitly "handled by the Constitution" without a specific mechanism identified.

### Unit 6 — Safe Mode

**Purpose.** Define a bounded, minimal, always-available operating mode Parker can fall back to when normal operation cannot be trusted — a degraded-but-honest state, never a silent continuation of business as usual.

**Deliverables.** A definition of what Safe Mode restricts, what it still permits, what triggers entry into it, and what is required to leave it.

**Success Criteria.** When conditions warrant it, Parker enters Safe Mode automatically, the owner is told plainly that it has, and normal operation resumes only through an explicit, defined exit condition — never a silent, unnoticed reversion.

### Unit 7 — Governed Real Tool Execution Boundary

**Purpose.** Define the boundary conditions under which Parker may move from proposing and simulating actions to actually executing real, external, consequential tool calls — the point at which operational assurance stops being about visibility into governed proposals and starts being about trust in real-world effects.

**Deliverables.** An explicit boundary statement for what distinguishes a tool safe to execute for real from one that is not yet; the operational prerequisites (observability, explainability, failure handling, recovery, threat coverage) that must already be in place before any specific real-execution capability is enabled.

**Success Criteria.** No real, external tool execution capability is introduced anywhere in Parker without first satisfying this boundary's own named prerequisites — capability growth is never permitted to outpace the operational assurance this Programme exists to establish.

Each unit above is planning-level only. No implementation code, interface, or Kotlin type is defined, implied, or authorised by this document for any of them.

---

## 5. Process Sustainability

Work in and around this Programme, and every Programme after it, falls into one of three governance classes, each with its own appropriate expectations:

- **Constitutional.** Changes to the Parker Constitution itself, or to a principle it establishes. These are rare, apply platform-wide, and require the highest scrutiny this repository already applies to constitutional change — full review, explicit ratification, and a durable record, exactly as every existing Constitutional Decision Record already demonstrates.
- **Architectural.** Changes that establish or reshape a subsystem's boundary, responsibilities, or contracts — a Contract Design, a Scope Lock, an Implementation Plan, or a document like this one. These require disclosed reasoning and review proportionate to their scope, but do not require constitutional-tier ratification unless they touch a constitutional principle directly.
- **Operational.** Day-to-day engineering decisions within an already-fixed architectural boundary — which concrete class implements an already-frozen interface, which literal identifier a disclosed convention uses, how a unit's own tests are structured. These are ordinary engineering judgement, reviewed at the same level as any other implementation work, and never require constitutional or architectural-tier process.

Operational Assurance itself is architectural work when it defines a unit's boundary (as Section 4 does here), and will produce operational work when each unit is eventually implemented. Keeping these three classes distinct is itself part of what this Programme protects: an operational fix should never quietly acquire constitutional weight, and a constitutional question should never be settled as if it were merely operational.

---

## 6. Guiding Principles

- **Operational Assurance increases trustworthiness, not intelligence.** Nothing in this Programme makes Parker capable of more; it makes Parker's existing capability more legible, more explicable, and more recoverable. A more trustworthy Parker is not a smarter Parker — it is one an owner can actually rely on.
- **The owner must always be able to determine:**
  - **what Parker is doing** — Unit 1, Unit 3;
  - **why** — Unit 4;
  - **what authority was exercised** — Unit 4, drawing on the Permission Engine's own existing decision record;
  - **what failed** — Unit 2, Unit 3;
  - **how recovery proceeds** — Unit 6, and the recovery path implied by Unit 2's own taxonomy.
- **Explanations are assembled from governed facts, never generated narrative.** An explanation this Programme produces is a reconstruction of what the Permission Engine, the Execution Pipeline, and the runtime's own records already, truthfully, show happened — never a plausible-sounding account authored after the fact by a reasoning provider. If a fact cannot be traced to a governed record, it does not belong in an explanation; a reasoning provider's own summarising language may narrate an explanation's presentation, but it never supplies the explanation's substance.

---

## 7. Long-Term Vision

Parker's architecture has been built, so far, to be **constitutionally correct** — every principle enforced structurally, every action traceable to an authorisation, no capability able to grant itself trust it was not given. That is necessary, and it is not, by itself, sufficient for an owner to feel the confidence the Constitution's own principles are meant to earn.

The Operational Assurance Programme is what carries Parker the rest of the way: from a system that is constitutionally correct in its design to one that is **constitutionally dependable** in its operation — where correctness is not just true of the architecture on paper, but visible, explicable, and recoverable in the owner's own, everyday experience of the system. This is the last architectural distance between a Parker that is trustworthy by construction and a Parker the owner actually, daily, trusts.

---

## Constraints

This document is planning only. It introduces no new constitutional rule, no behavioural change, and no production code. It records future architectural direction only, and does not authorise any unit named above to begin. It does not supersede, reorder, or otherwise alter the current roadmap — Memory Core and Evidence Intelligence's own current sequencing, priority, and scope are entirely unaffected by this document's existence.
