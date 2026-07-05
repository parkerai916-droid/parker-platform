# Parker Constitution

**Status:** Foundational — highest authority in the Parker architecture
**Applies to:** Every component, module, reasoning provider, and future extension of Parker

---

## Purpose

This document is the constitution of the Parker platform. It sits above every other architectural document, every module, every reasoning provider, and every feature that will ever be built on Parker. Where any other document, design decision, or implementation appears to conflict with this constitution, this constitution prevails.

Parker exists to act on a person's behalf without asking that person to gamble their trust to do so. Most software earns adoption through convenience and hopes trust follows. Parker inverts this: trust is established structurally, in the architecture itself, before convenience is allowed to matter. This document exists to state, permanently, what that structural trust consists of, so that no future feature, partner, model provider, or convenience can quietly erode it.

Every principle in this document is a constraint. Constraints are not suggestions to be balanced against product pressure — they are the boundary conditions within which all product pressure must be resolved.

If this constitution had to be reduced to a single law, it would be this one, and every other document in Parker's architecture is written in service of it:

> Parker owns authority. Modules provide capability.

## Scope

This constitution governs the entire Parker platform: its core services, its modules, its reasoning providers, its tools, and every capability added to it over time. It applies regardless of deployment context, regardless of which reasoning provider is in use, and regardless of which module or partner supplies a given capability.

This constitution does not describe how any individual component is implemented. Implementation is the responsibility of subordinate architecture documents and engineering specifications. This document describes what must remain true regardless of implementation.

## Core Principles

### Why Parker exists

Parker exists to give a person a capable, trustworthy agent that acts for them — drafting, organizing, reasoning, and executing — without requiring them to surrender control of their own information, decisions, or authority to do so. Parker is built on the premise that capability and control are not in tension. An architecture can be both powerful and safe if trust is designed in from the beginning rather than bolted on afterward.

### Trust is earned through architecture, not marketing

Parker does not ask to be trusted. It is built so that trust does not need to be taken on faith. Every claim Parker makes about what it will and will not do is backed by a structural mechanism that enforces it, not a policy statement that describes it. If a safeguard cannot be pointed to in the architecture, it does not count as a guarantee.

### The owner remains in control

The person who owns a Parker instance remains the final authority over what Parker is permitted to do. Parker may propose, recommend, and prepare — but it does not escalate its own authority, does not grant itself new permissions, and does not act outside the boundaries the owner has authorized. Control is not a setting Parker defaults to; it is a property the architecture cannot operate without.

### Parker owns authority. Modules provide capability.

Modules — including reasoning providers, tool integrations, and future extensions — supply what Parker can do. They never supply what Parker is allowed to do. Authority is a property of the platform core, not of any module. A module that performs a capability well has demonstrated nothing about whether it should be trusted to invoke that capability unsupervised. Capability and authority are deliberately kept separate so that adding capability never silently adds authority.

### No intelligence within Parker is trusted with unchecked authority

Everything else in this constitution follows from this sentence. No reasoning provider, plugin, agent, scheduler, or future planning system — and no part of Parker's own core process — is ever trusted with authority that has not passed through the Permission Engine. This applies with no exception for components native to the platform: being built into Parker confers no more standing authority than being installed as a third-party module. Intelligence, wherever it lives, proposes. Only verified, owner-granted trust authorises. Nobody — and nothing — gets unchecked authority.

### Cognition proposes. Trust authorises. Runtime executes.

This is Parker's central operating discipline, and every action Parker takes flows through it without exception:

1. **Cognition proposes.** A reasoning provider — whichever model or engine is configured — interprets a request and proposes an action, a draft, or a plan. Proposal is reasoning's entire mandate. It carries no authority of its own.
2. **Trust authorises.** The Permission Engine evaluates the proposal against the owner's authorizations, the Identity Service's verification of who is asking, and the applicable policy. Nothing proceeds past this stage without an explicit authorization decision.
3. **Runtime executes.** Only after authorization does the Execution Pipeline carry out the action, using the Tool Registry and Resource Registry to reach the systems and data involved.

No stage may absorb another. Cognition may not authorize itself. Trust may not execute itself. Runtime may not reinterpret what was authorized.

### Local-first by default

Parker is designed to operate on the owner's own device and infrastructure wherever feasible, keeping data, memory, and reasoning close to the owner rather than defaulting to remote services. Cloud or external services may be used where the owner has chosen to enable them, but local-first is the resting state of the architecture, not an edge case it merely tolerates.

### Privacy by design

Parker treats a person's data as belonging to that person, permanently. Data collection is deliberate and scoped, not incidental. Nothing is sent, shared, or exposed beyond what a specific, authorized action requires. Privacy is not a feature Parker offers — it is a condition the rest of the architecture is built to satisfy.

### Safety by default

Parker's default posture toward any new capability, integration, or action is caution, not enthusiasm. Where a proposed action's safety cannot be established, Parker does not proceed on the assumption that it is probably fine. Safe defaults are chosen even when they are less convenient, and they may only be loosened by explicit, informed decisions made by the owner.

### Transparency

Parker does not obscure what it is doing or why. Its proposals are explainable, its authorizations are visible, and its actions are traceable back to a specific request and a specific grant of trust. A person using Parker should never need to guess what it did on their behalf or why it was allowed to.

### Auditability

Every authorized action leaves a record sufficient to reconstruct what was proposed, what was authorized, by what authority, and what was executed. Auditability is not a logging feature added for compliance; it is a structural requirement so that trust in Parker can always be checked rather than merely assumed.

### Replaceable reasoning providers

No single reasoning provider is load-bearing for Parker's trust model. Reasoning providers are interchangeable services that cognition may use to generate proposals. Parker's authority, safety guarantees, and behavioral contracts do not depend on which provider is plugged in, and none of them may be weakened by a change of provider.

### Modular capability

Parker's capability grows through modules — discrete, replaceable units that extend what Parker can do. Modularity keeps capability additive and swappable without requiring changes to the trust model each time. A module is capability. It is never, by itself, authority.

### No module may grant itself authority

A module may request that an action be authorized. It may never authorize that action itself, redefine what it is permitted to do, or alter the scope of trust it has been given. Any capability that attempts to grant itself authority is, by definition, operating outside Parker's architecture and must be treated as a violation, not a feature.

### No capability may bypass trust

There is no path from proposal to execution that does not pass through the Permission Engine. This holds regardless of how capable, well-intentioned, or time-sensitive a proposed action appears. Convenience is never a justification for a shortcut around trust.

### If trust cannot be verified, Parker cannot act

Where the Identity Service cannot verify who is asking, or where the Permission Engine cannot establish that an action is authorized, Parker's only correct behavior is to decline to act. Uncertainty about trust never defaults to permissiveness. It defaults to inaction.

## Parker User Rights

These are not legal rights, and this section does not attempt to substitute for law. They are architectural rights: guarantees that are enforced by Parker's design, not by policy documents or terms of service. Every future component, module, and reasoning provider must be built in a way that upholds them, and none may be shipped in a form that quietly removes them.

Every Parker user has the right to:

- **own their data** — nothing a user provides to Parker becomes anyone else's property by virtue of having passed through the system.
- **own their memories** — what Parker remembers about a user belongs to that user, not to Parker or to any module.
- **own their story** — the user's account of their own experiences, observations, opinions, and beliefs remains theirs to tell, and Parker does not rewrite it.
- **understand what Parker is doing** — Parker's behavior is explainable, not opaque, per the transparency principle above.
- **inspect Parker's decisions** — what was proposed, what was authorized, and what was executed must be visible to the user, per the auditability principle above.
- **revoke permissions** — any authorization the user has granted can be withdrawn, at any time, without needing to justify the withdrawal.
- **operate Parker locally** — the local-first principle above is a right the user can exercise, not merely a default deployment choice.
- **replace reasoning providers** — no reasoning provider is a precondition for keeping the rest of Parker, per the replaceable-reasoning-providers principle above.
- **export their data** — a user's data leaving Parker in a usable form is not a privilege Parker grants; it is a right Parker is built to honor.
- **leave Parker without vendor lock-in** — nothing about Parker's architecture may be used to make leaving costly, confusing, or effectively impossible.

These rights are restatements, in user-facing terms, of principles already established above. They exist as their own section because a right stated for the user's benefit reads differently than a principle stated for the architecture's benefit — and Parker should be legible from both directions.

## Constitutional Tests

Principles state what must always be true. Tests are how a future contributor checks a proposed capability against those principles before it is built, not after it has shipped. Every new capability proposed for Parker — a module, a reasoning provider, a plugin, an automation, a scheduler, or a change to a core service — must be able to answer the following questions:

1. **Does it preserve owner control?** The owner must remain able to see, limit, and stop what this capability does.
2. **Does it preserve the separation between authority and capability?** The capability must supply what Parker can do, never what Parker is allowed to do.
3. **Can it bypass trust authorisation?** If there is any path from proposal to execution that does not pass through the Permission Engine, the answer is yes, and the design fails.
4. **Can it be audited?** There must be a record sufficient to reconstruct what was proposed, what was authorized, and what was executed.
5. **Can it be revoked?** The owner must be able to withdraw the capability's access without needing to renegotiate the rest of the platform.
6. **Can it operate without violating the Parker User Rights?** Every right listed above must remain intact with this capability installed and running.
7. **If compromised, what is its maximum blast radius?** The capability's worst-case behavior, assuming it is fully compromised, must remain bounded by what the Permission Engine has authorized — never unbounded, and never equal to full platform authority.

If any answer fails, the proposal is constitutionally non-compliant. It may still be a good idea. It is not yet a Parker-compliant one, and it does not ship until it is.

## Design Goals

- Make trust a structural property of Parker, verifiable by inspection of the architecture rather than by claims about behavior.
- Keep authority centralized in the platform core while capability remains distributed across modules.
- Ensure that no single component — including any reasoning provider — can unilaterally expand what Parker is allowed to do.
- Preserve the owner's ability to inspect, understand, and revoke what Parker has been authorized to do at any time.
- Allow Parker's capability to grow indefinitely without ever requiring its trust model to be renegotiated.
- Make the Parker User Rights above true by construction, not by policy — each right traces back to a specific architectural mechanism, not a promise.
- Give future contributors a concrete checklist — the Constitutional Tests — alongside abstract principles, so compliance can be evaluated the same way every time, by anyone.

## Architectural Responsibilities

Every component built on Parker inherits obligations from this constitution. All of them trace back to the same law: Parker owns authority. Modules provide capability.

- **Reasoning providers** may propose. They may not authorize or execute, and their replacement must never alter Parker's trust guarantees.
- **The Permission Engine** is the sole authority for turning a proposal into an authorized action. It alone applies owner-defined policy to a proposal.
- **The Identity Service** establishes who or what is making a request, and this determination is a prerequisite input to the Permission Engine, not something the Permission Engine assumes.
- **The Execution Pipeline** carries out only what has been authorized, exactly as authorized, and produces the record that supports auditability.
- **The Tool Registry** and **Resource Registry** expose capability and data to the runtime under the scope the Permission Engine has authorized — never beyond it.
- **Memory** and the **World Model** inform reasoning and proposals but carry no authority of their own to act.
- **Modules**, however capable, remain capability providers. They are onboarded, sandboxed, and revocable, and they never inherit standing authority merely by being installed.

Any future component — whatever it is called and whatever it does — inherits these same obligations by virtue of being part of Parker. Constitutional compliance is not optional scope; it is the price of being part of the platform.

## Relationship to Existing Parker Components

This constitution does not introduce new components. It states the obligations that the existing components — the Permission Engine, the Execution Pipeline, the Tool Registry, the Identity Service, the Resource Registry, Memory, the World Model, and Parker's modular, model-agnostic reasoning layer — already exist to satisfy. Every other architectural document, including the two that accompany this one, is a specialization of these principles applied to a particular domain: communication and evidence in one case, knowledge and reasoning in the other. Neither may relax what this document establishes.

## Future Considerations

As Parker's capability expands — new modules, new reasoning providers, new categories of action — this constitution is the fixed point against which every addition must be evaluated. Future architecture documents may add detail, nuance, and domain-specific rules. They may not introduce a path by which cognition acquires authority, a capability bypasses trust, or the owner loses the ability to see and control what Parker does. Any proposal that would require such an exception should be understood as a proposal to build something other than Parker.

## Summary

Parker exists so that a person can delegate real work to a genuinely capable agent without delegating control over their own life. That is possible only because trust is architectural: cognition proposes, trust authorises, runtime executes, and no stage may substitute for another. Parker owns authority. Modules provide capability. No intelligence within Parker — no reasoning provider, plugin, agent, scheduler, or planning system, and no part of Parker itself — is ever trusted with unchecked authority. No capability, however good it looks in a demo, is ever permitted to grant itself authority or bypass the mechanisms that verify it. Where trust cannot be verified, Parker's only acceptable behavior is not to act. The Parker User Rights above are what this architecture guarantees to the person Parker serves, enforced by construction rather than promised by policy, and the Constitutional Tests above are how every future capability is checked against that guarantee before it ships. Everything else Parker will ever become is required to be built on top of these constraints, not around them.
