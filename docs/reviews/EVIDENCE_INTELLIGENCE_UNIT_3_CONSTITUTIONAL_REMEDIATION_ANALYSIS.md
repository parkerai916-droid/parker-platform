# Evidence Intelligence — Unit 3 Constitutional Remediation Analysis

This is a design study only. No Kotlin, no governance amendment, no test or production change, and no Git action is performed or proposed as final. Every option below is surveyed neutrally; no option is implemented.

---

## 1. Proven Contradiction Summary

The prior constitutional investigation established, from explicit textual governance rather than inference:

- `ReasoningProviderRequest.turn: Turn` is mandatory and non-nullable. `Turn` requires `turnId: TurnId`, `conversationId: ConversationId`, and `message: InboundOwnerMessage` (itself requiring `channelId`, `senderPrincipalId`, non-blank `text`, `timestamp`, `correlationId`).
- `EvidenceAnalysisRequest` — Evidence Intelligence's own frozen input shape — carries none of `ConversationId`, `TurnId`, a channel `ModuleId`, or a `CorrelationId`. Evidence Intelligence analysis is not a conversation.
- `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` §3: *"A Turn is never created except as part of this one operation; there is no separate 'create a Turn' operation."*
- `CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` §12: *"`ConversationEngine` ownership is preserved exactly: it remains the only component that ever constructs a `Conversation` or `Turn`..."*
- Empirically, the only `Turn(...)` construction site in the repository is `InMemoryConversationEngine.kt:178`; even `ConversationTurnReasoningCoordinator` — the exact precedent CDR-007 cites — never constructs one itself.

**Conclusion carried forward:** Evidence Intelligence cannot legitimately construct the `Turn` that `ReasoningProviderRequest` requires. This is a genuine structural incompatibility between two independently-frozen contracts, not an implementation detail. This document surveys every constitutionally plausible way to resolve it.

**One additional piece of governing text, newly relevant to this remediation analysis, not previously needed:** `docs/architecture/19-conversation-engine.md` §4 states an invariant that bears directly on several remediation paths below:

> "**Invariant: Conversation state exists solely to preserve continuity between Turns, and carries no authority outside the Conversation it belongs to.** It is a continuity record, never a workflow record... Anything that looks like it needs to survive beyond a single Conversation's own continuity purpose belongs in Memory, the World Model, or an already-owned runtime's own state — never smuggled into Conversation state because it was convenient to reach."

This becomes important below: any remediation that has Evidence Intelligence obtain a `Turn` *through* Conversation Engine, for a purpose that has nothing to do with conversational continuity, runs directly against this named invariant.

**A second cross-cutting constraint, applying to every option that touches `ReasoningProvider`'s own contract:** CDR-007's Decision block states, verbatim:

> "A new Reasoning Provider abstraction specific to Evidence Intelligence — Not adopted; the existing ReasoningProvider interface is adopted unmodified, and Evidence Intelligence is classified as its orchestrator where used, never as an instance of it."

and its Decision Rules: *"the `ReasoningProvider` abstraction itself is not broadened, extended, or reinterpreted to describe Evidence Intelligence."* The Evidence Intelligence Contract Design's own Repository Reuse Summary (§13) separately lists `ReasoningProvider`, `ReasoningProviderRequest`, `ReasoningProviderResponse`, `ReasoningContext` as "the Reasoning Provider's own contract, unmodified and unbroadened." Any option that changes `ReasoningProviderRequest`'s shape therefore touches language CDR-007 itself froze — even an option that generalises the shape for *every* caller, not "specific to Evidence Intelligence," still contradicts the Contract Design's "unmodified" clause and arguably CDR-007's "not broadened" clause. This is flagged per-option below rather than asserted once and dropped, because the degree of contradiction differs materially across options.

---

## 2. Complete Option Survey

### Option 1 — Generalised Reasoning Subject (sealed union)

**Summary:** Replace `ReasoningProviderRequest.turn: Turn` with `subject: ReasoningSubject`, a new sealed type with (at minimum) two cases — e.g. a case wrapping the existing `Turn` unchanged, and a second case Evidence Intelligence owns, carrying an evidentiary/analytical subject (governed references, `analysisKind`, resolved content). `ReasoningProvider.reason` becomes agnostic to which case it received; `ReasoningContext` is unchanged.

- **Documents requiring amendment:** `REASONING_PROVIDER_CONTRACT_DESIGN.md` (owner of `ReasoningProviderRequest`'s shape — direct amendment, §2, §10); `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` §12/§13 (must stop describing `ReasoningProvider` as "unmodified"); `EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` §4 (dependency freeze table); likely `CDR-007` itself, since its Decision Rules state the abstraction is "not broadened, extended, or reinterpreted" — a sealed-union broadening is a plain-text broadening, even if the new case is not "specific to Evidence Intelligence" in isolation (Conversation Engine's case is also present).
- **Whether CDR required:** Very likely yes. CDR-007's own "not broadened, extended, or reinterpreted" language is difficult to satisfy by any reading once `ReasoningProviderRequest` gains a second case. A disciplined argument exists that a CDR amendment (not a fresh CDR) suffices, since this doesn't reopen CDR-007's classification of Evidence Intelligence itself — but the "ReasoningProvider... unmodified" clause is squarely CDR-007's own text, so at minimum a formal CDR amendment is required, not merely a Contract Design revision.
- **Public contract changes:** `ReasoningProviderRequest` restructured (one field replaced by a sealed field); a new sealed `ReasoningSubject` type introduced, adding one new public type to the Reasoning Provider tier (not to Evidence Intelligence's own four-type budget, but a new public surface nonetheless, requiring its own minimalism review).
- **Ownership changes:** `ReasoningSubject` would need a constitutional owner. Cleanest candidate: the Reasoning Provider Contract Design itself (since it already owns `ReasoningProviderRequest`), with each case remaining owned by its originating subsystem (Conversation Engine owns the `Turn`-wrapping case; Evidence Intelligence owns its own case) — mirroring this repository's own `CandidateMemoryCoreRecord` precedent (a closed selector owned by one subsystem, wrapping types owned elsewhere).
- **Dependency changes:** None for Evidence Intelligence beyond what CDR-007 already authorised (`ReasoningProvider`, unchanged in identity even though its request shape changes). No new dependency on Conversation Engine.
- **Implementation impact:** Every existing caller of `ReasoningProviderRequest` (`ConversationTurnReasoningCoordinator`, `ModelReasoningProvider`, `LoggingReasoningProvider`, all associated tests) must be updated to match against the sealed subject rather than reading `.turn` directly — a mechanical but non-trivial, repository-wide change touching already-frozen, already-implemented, already-tested Sprint 7/9/10/11 code.
- **Migration impact:** Moderate-to-high. Existing `ModelReasoningProvider`'s `ReasoningPromptBuilder.buildPrompt(request.turn, request.reasoningContext)` signature would need to become subject-shaped or `Turn`-only with a fallback, and every existing contract test (`ReasoningProviderContractTest`) needs new cases for the added variant.
- **Future extensibility:** High. A third, fourth, or later caller (e.g., a future document-ingestion or scheduling subsystem) could add its own case without further contract redesign, provided the sealed type itself is amended additively at that future point — mirroring the repository's own precedent of sealed types over nullable fields.
- **Architectural advantages:** Single, unified `ReasoningProvider` interface preserved (no duplicate contract to keep in sync); structurally exhaustive (compiler-enforced) rather than convention-enforced; preserves the Constitution's "replaceable reasoning providers" principle without fragmenting the abstraction a provider implements.
- **Architectural disadvantages:** Touches a contract three separate frozen documents (Reasoning Provider Contract Design, CDR-007, Evidence Intelligence Contract Design) all currently describe as settled and "unmodified" — the single heaviest cross-document reopening of the options surveyed that keep one unified interface.
- **Constitutional risks:** Reopening CDR-007's Decision block risks re-litigating settled classification questions even when the intent is narrow; must be scoped tightly (a targeted amendment, not a full CDR reconsideration) to avoid this. Also risks quietly weakening the Reasoning Provider Contract Design's own minimalism discipline ("four new contracts... zero modified") if not carefully bounded.
- **Preserves existing subsystem boundaries:** Yes. Conversation Engine's exclusive ownership of `Turn`/`Conversation` construction is completely undisturbed; Evidence Intelligence gains no dependency on Conversation Engine; each subsystem still owns exactly what it owned before.
- **Long-term effect on Parker:** Establishes `ReasoningProvider` as a genuinely subsystem-neutral capability rather than a Conversation-Engine-shaped one that other subsystems awkwardly reuse — arguably the most faithful realisation of the Constitution's "Replaceable reasoning providers... reasoning providers are interchangeable services that cognition may use to generate proposals," since today's contract is interchangeable in *implementation* but not in *caller*.

---

### Option 2 — Fully Opaque Neutral Envelope

**Summary:** Remove `Turn` from `ReasoningProviderRequest` entirely. Replace it with an extension of `ReasoningContext`'s own existing philosophy — a caller-agnostic bundle of prose/reference content that already-existing conversational fields (`message.text`, sender, timestamp) are flattened into by Conversation Engine, exactly as Evidence Intelligence would flatten its own evidentiary content into the same shape. No `Turn`-shaped field survives on the request at all.

- **Documents requiring amendment:** Same three as Option 1 (`REASONING_PROVIDER_CONTRACT_DESIGN.md`, `CDR-007`, `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`), plus — more severely than Option 1 — `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` and `CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md`, since Conversation Engine's own callers (`ConversationTurnReasoningCoordinator`) currently pass `disposition.turn` directly into `ReasoningProviderRequest` — removing `Turn` from that contract forces Conversation Engine's own call sites to be rewritten to project `Turn` into the new neutral shape.
- **Whether CDR required:** Yes, and more clearly than Option 1 — this doesn't just add a case, it deletes `Turn`'s own place in the request contract entirely, a strictly larger reopening of CDR-007's "unmodified" language and of the Reasoning Provider Contract Design's own field-level shape.
- **Public contract changes:** Largest of any option — `ReasoningProviderRequest` loses its `turn` field outright; a new, larger-surfaced neutral content type replaces it.
- **Ownership changes:** The neutral envelope's owner becomes ambiguous by design (that is its whole purpose) — this is itself a constitutional problem: this repository's own Design Rules Compliance sections repeatedly require "every public object has exactly one constitutional owner" (stated explicitly in the Evidence Intelligence Contract Design's own compliance check). An envelope with no natural single owner is a self-inflicted violation of a rule this Programme applies to itself elsewhere.
- **Dependency changes:** None additional for Evidence Intelligence.
- **Implementation impact:** Largest of any option — every conversational field (`senderPrincipalId`, `correlationId`, `timestamp`, `channelId`) that a concrete `ReasoningProvider` implementation (`ModelReasoningProvider`, its `ReasoningPromptBuilder`) currently reads off `Turn`/`InboundOwnerMessage` must be re-derived from flattened prose or dropped, degrading structure that currently exists.
- **Migration impact:** High. Existing prompt-construction logic (`DefaultReasoningPromptBuilder`) is built around a typed `Turn`; converting to opaque prose risks silently losing structured fields (e.g., `correlationId`) current logging (`LoggingReasoningProvider`) depends on for its own audit trail.
- **Future extensibility:** Superficially high (anything can be flattened to prose) but this is a false economy — the same "no further internal structure imposed" property that makes `ReasoningContext` safe for its narrow, existing purpose (background context, never the primary subject) becomes a liability once it is the *entire* request shape, since Unit 4's claim-level traceability requirement (Evidence Intelligence Contract Design §5) needs structured, resolvable references, not opaque prose.
- **Architectural advantages:** Conceptually the simplest single change (one type, one field, no branching); arguably the most literal reading of "Cognition proposes" as caller-agnostic.
- **Architectural disadvantages:** Discards real structure (`Turn`'s typed fields) that existing, already-shipped, already-tested code relies on; creates an ownerless public type, a self-inflicted rule violation; degrades auditability by collapsing structured identifiers into prose a `ReasoningProvider` implementation must now parse back out, an anti-pattern this repository elsewhere explicitly avoids (see the Reasoning Provider Contract Design's own justification for a sealed response type over "three separate boolean/nullable fields").
- **Constitutional risks:** Epistemic Integrity Article VIII/IX (provenance, evidential integrity) obligations are harder to satisfy once governed references are flattened into undifferentiated prose rather than carried as structured, resolvable identifiers — a real tension with Unit 4's own claim-level traceability requirement, which depends on being able to resolve a claim back to a specific `EvidenceArtifactId`/`RelationshipEndpoint`, not a prose mention of one.
- **Preserves existing subsystem boundaries:** Formally yes (no new dependency edges), but at the cost of eroding Conversation Engine's own carefully-typed `Turn` contract for everyone, not just for Evidence Intelligence's benefit.
- **Long-term effect on Parker:** Trades structural safety for uniformity. Likely to be judged, on reflection, as over-correcting — the repository's own established taste (sealed types, typed references, explicit selectors) argues against this option relative to Option 1, which achieves the same caller-neutrality without discarding structure.

---

### Option 3 — Split the Provider Contract (dedicated Analytical Reasoning Provider)

**Summary:** Leave `ReasoningProvider`, `ReasoningProviderRequest`, `ReasoningProviderResponse`, and `Turn` completely untouched. Introduce a wholly separate, Evidence-Intelligence-scoped interface (e.g., an "Analytical Reasoning Provider" contract) with its own request/response shapes fit for evidentiary analysis — mirroring how `MemoryCore`, `EvidenceCustodian`, and Knowledge Memory are already separate, non-overlapping contracts rather than one shared interface with a caller-discriminated request.

- **Documents requiring amendment:** `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` (a new public interface, currently frozen at exactly one — `EvidenceIntelligence` itself — per Scope Lock §4: "No fifth new public type or interface is authorised"); `EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` (same freeze, needs explicit amendment); and, most seriously, `CDR-007` directly, since this is the literal thing its Decision block names and rejects.
- **Whether CDR required:** Yes, unambiguously, and this is the heaviest CDR lift of any option surveyed. CDR-007's Decision states in so many words: *"A new Reasoning Provider abstraction specific to Evidence Intelligence — Not adopted."* This option is not adjacent to that rejected candidate; it *is* that candidate. A full, explicit CDR-007 amendment (or a superseding CDR) is required, not a targeted textual tweak — the Decision block itself would need to flip from "Not adopted" to "Adopted," which is a substantive re-litigation, not a clarification.
- **Public contract changes:** Largest new-surface count of any option — an entirely new interface, new request type, new response type, all Evidence-Intelligence-owned (or jointly owned if the new contract is framed as reasoning-tier rather than EI-tier).
- **Ownership changes:** Clean in one sense (the new contract's owner is unambiguous — either the Reasoning Provider tier, generalised for analytical use, or Evidence Intelligence itself), but doubles the number of "Reasoning Provider"-shaped contracts a future maintainer must reason about and keep architecturally reconciled.
- **Dependency changes:** Evidence Intelligence's dependency table grows by one net-new contract (an analytical reasoning interface) rather than reusing the existing `ReasoningProvider` entry — a bigger dependency-table change than Options 1/2, which keep the same three-row table with one row's referenced type changed.
- **Implementation impact:** Existing `ReasoningProvider` ecosystem (`ModelReasoningProvider`, `LoggingReasoningProvider`, `ConversationTurnReasoningCoordinator`) is completely undisturbed — a real advantage. But a parallel implementation stack (a concrete analytical provider, its own prompt-builder-equivalent, its own tests) must be built from scratch, duplicating effort rather than reusing `ModelReasoningProvider`'s own already-working model-invocation plumbing unless that plumbing is itself refactored to be shared beneath both contracts.
- **Migration impact:** Low for Conversation Engine's own path (zero change); high in net new engineering for Evidence Intelligence's own path (an entire second contract stack to design, freeze, and implement, following the same multi-stage governance process — Architecture → Contract Design → Implementation Plan — Reasoning Provider itself went through).
- **Future extensibility:** Double-edged. A third analytical consumer (a future subsystem) could reuse the analytical contract directly — but a fourth, differently-shaped consumer might need a third parallel contract, and this pattern does not obviously converge the way a single generalised abstraction (Option 1) does.
- **Architectural advantages:** Cleanest separation of concerns by the repository's own frequent precedent (one bounded contract per bounded responsibility); zero risk to any already-frozen, already-implemented Conversation Engine/Reasoning Provider code; explicitly honours "Evidence Intelligence... is not itself a Reasoning Provider" (CDR-007 §1) by never asking it to share a caller-discriminated shape with one.
- **Architectural disadvantages:** Directly reverses a named, explicit CDR-007 rejection — the single most visible governance reversal of any option; risks the Constitution's own "replaceable reasoning providers" principle being read subsystem-by-subsystem rather than platform-wide, since a model swapped for Conversation Engine would not automatically also serve Evidence Intelligence's analytical contract without separate integration work.
- **Constitutional risks:** Reopening a Decision block, rather than merely a Contract Design, invites a full Independent Constitutional Review cycle equivalent to CDR-007's own original review — the heaviest governance-process risk of any option.
- **Preserves existing subsystem boundaries:** Yes, completely, for Conversation Engine and the existing `ReasoningProvider` stack. No, for the "Evidence Intelligence is not itself a Reasoning Provider, and orchestrates the existing one" boundary CDR-007 drew — that boundary is redrawn, not preserved.
- **Long-term effect on Parker:** Risks normalising "one bespoke Reasoning-Provider-shaped contract per subsystem" as this platform's pattern, which cuts against the Constitution's own "no reasoning provider is load-bearing" and "replaceable" principles if taken further (a third, fourth subsystem each inventing its own analytical-reasoning contract rather than converging on one shape).

---

### Option 4 — Evidence Intelligence Becomes a Conversation Engine Caller

**Summary:** Evidence Intelligence legitimately obtains a real `Turn` by constructing a synthetic `InboundOwnerMessage` representing the analysis request and calling `ConversationEngine.resolveConversationId` / `submitTurn` itself, exactly as any genuine conversational caller would. No contract is touched anywhere; Evidence Intelligence adapts itself to the existing `ReasoningProvider` contract by becoming, procedurally, a Conversation Engine caller.

- **Documents requiring amendment:** `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` §12 and `EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` §4 (both would need `ConversationEngine` added to Evidence Intelligence's closed, three-row dependency table — currently "no additional dependency may be introduced by this plan"); likely `CDR-007` as well, since Evidence Intelligence acquiring a dependency on an entirely unrelated conversational subsystem, for a non-conversational purpose, is a material boundary change CDR-007 never contemplated.
- **Whether CDR required:** Very likely yes, and for a reason more serious than a simple dependency-table addition: this option requires Evidence Intelligence's own analysis to be represented, inside Conversation Engine's own owned state, as a genuine Conversation and Turn — directly contradicting Architecture §4's own invariant, quoted above: *"Conversation state exists solely to preserve continuity between Turns... never smuggled into Conversation state because it was convenient to reach."* An Evidence Intelligence analysis has no relationship whatsoever to "continuity between Turns." This is not a dependency-table technicality; it risks manufacturing a Conversation that does not represent anything the Constitution's own transparency principle would recognise as a real exchange ("Parker does not obscure what it is doing or why... A person using Parker should never need to guess what it did on their behalf").
- **Public contract changes:** None. This is the only option (besides Option 6) that touches zero public contracts.
- **Ownership changes:** None to any type's ownership — but a serious *conceptual* ownership violation: Conversation Engine's owned state (Conversation/Turn membership) would come to represent something (an analytical invocation) that is not what Architecture §4 defines that state as existing for.
- **Dependency changes:** Evidence Intelligence gains a dependency on `ConversationEngine` — a subsystem CDR-007's own dependency model never names, and one organisationally unrelated to Evidence Intelligence's peer relationship with Evidence Custodian/Knowledge Memory.
- **Implementation impact:** Superficially minimal — reuse existing `ConversationEngine.submitTurn`. In practice, requires fabricating a synthetic `channelId` (`ModuleId`), a synthetic `correlationId`, and a synthetic `senderPrincipalId`-shaped identity for a message nobody sent — each fabrication is itself a small integrity compromise multiplied across every field `InboundOwnerMessage` requires.
- **Migration impact:** Low for existing code (nothing existing changes) but introduces a permanent, ongoing implementation burden of maintaining fictitious conversational identity data for every future analysis.
- **Future extensibility:** Poor — every future non-conversational caller of `ReasoningProvider` would need to repeat the same fabrication, multiplying the invariant violation rather than resolving it once.
- **Architectural advantages:** Zero contract changes; fastest to describe.
- **Architectural disadvantages:** Manufactures fictitious Conversations/Turns for non-conversational purposes — the single clearest violation, of any option, of an explicit, named architectural invariant belonging to a subsystem *other than* the one under repair. Risks polluting Conversation Engine's own continuity state and any future conversation-history surface with entries that do not represent genuine owner exchanges.
- **Constitutional risks:** Highest of any option. Directly contradicts `19-conversation-engine.md` §4's invariant; risks the Constitution's own Transparency principle if a synthetic "Conversation" for an analytical operation is ever surfaced to an owner as though it were a real exchange; risks `PrincipalId`/`senderPrincipalId` misattribution (Constitution: "Every action a Reasoning Provider's output eventually leads to still passes through the same Permission Engine evaluation and remains as visible and revocable... a Reasoning Provider introduces no separate, less-visible path" — a fabricated conversational identity is exactly this kind of less-visible path).
- **Preserves existing subsystem boundaries:** No. This is the option that most directly breaches Conversation Engine's own architectural purpose to solve a problem that has nothing to do with conversation.
- **Long-term effect on Parker:** Establishes a precedent that any subsystem needing to reuse a Conversation-Engine-shaped contract may simply "pretend" to be a conversation — corrosive to the very invariant (`Conversation` state means something specific) the Architecture document took care to state explicitly.

---

### Option 5 — Loosen `ReasoningProviderRequest` via Additive Nullable Field

**Summary:** Keep `turn: Turn` but make it nullable; add a second, also-nullable field alongside it (e.g., `analyticalSubject: EvidenceAnalysisContext?` or similar) for non-conversational callers, with an implicit "exactly one must be non-null" convention.

- **Documents requiring amendment:** Same three as Option 1 (`REASONING_PROVIDER_CONTRACT_DESIGN.md`, `CDR-007`, `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`).
- **Whether CDR required:** Same as Option 1 — yes, for the same "unmodified/not broadened" reasons.
- **Public contract changes:** Two new/changed fields on `ReasoningProviderRequest`, one new supporting type.
- **Ownership changes:** Same as Option 1's new-case ownership question, but without a sealed type's structural exclusivity guarantee.
- **Dependency changes:** None additional.
- **Implementation impact:** Every `ReasoningProvider` implementation must now defensively check which of the two nullable fields is populated — logic that can silently be gotten wrong (both null, or both non-null) in a way a sealed type makes impossible.
- **Migration impact:** Similar to Option 1's migration cost, without Option 1's compile-time safety.
- **Future extensibility:** Poor — a third caller means a third nullable field, and the "exactly one populated" invariant becomes combinatorially harder to enforce or even to state, let alone verify.
- **Architectural advantages:** Minimal delta from today's shape (one field added, one field's nullability changed) — superficially the smallest diff of the four "modify `ReasoningProviderRequest`" options.
- **Architectural disadvantages:** Reintroduces, on the request side, precisely the anti-pattern the Reasoning Provider Contract Design's own Minimalism Review explicitly rejected on the response side: *"Why a sealed type, not three separate boolean/nullable fields... a sealed type makes that structurally impossible to violate, rather than merely conventionally expected of three independent nullable fields."* This option is, in effect, a knowing regression relative to a design principle this exact document already applied and defended elsewhere in the same file.
- **Constitutional risks:** Same CDR-reopening risk as Option 1, incurred for a strictly weaker structural guarantee.
- **Preserves existing subsystem boundaries:** Yes, in the same sense as Option 1.
- **Long-term effect on Parker:** **Strictly dominated by Option 1** — it achieves the same caller-generalisation goal, touches the same governing documents, requires the same CDR-level reopening, and yet delivers a weaker (nullable-field, not sealed) structural guarantee than Option 1 does at equivalent cost. There is no axis on which Option 5 outperforms Option 1.

---

### Option 6 — Additive Conversation Engine "Turn-Minting" Operation for Non-Conversational Callers

**Summary:** Extend `ConversationEngine`'s own interface with a new, narrowly-scoped operation that legitimately mints a `Turn` on behalf of a declared non-conversational caller (Evidence Intelligence), without a real `InboundOwnerMessage` behind it — preserving `ConversationEngine` as `Turn`'s sole constructor (satisfying the exclusivity rule literally) while broadening *who* may ask it to construct one.

- **Documents requiring amendment:** `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` (new operation, §1, §9 Minimalism Review); `CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` §12 (its own "the only component that ever constructs a Turn" statement would need updating to describe the new, broader minting path); `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`/`SCOPE_LOCK.md` (new dependency on `ConversationEngine`).
- **Whether CDR required:** Likely yes, for the same reason as Option 4, though somewhat less severely: this option does not fabricate a fictitious *sender* identity or channel, but it still asks Conversation Engine's own owned state to represent something Architecture §4's invariant says it must not represent — an analytical invocation with no continuity purpose. The same quoted invariant applies with full force: *"never smuggled into Conversation state because it was convenient to reach."* Even a dedicated, clearly-labelled minting operation is still minting a `Turn`/`Conversation` for a purpose the Architecture document defines Conversation state as existing specifically *not* to serve.
- **Public contract changes:** One new operation on `ConversationEngine` (currently frozen at exactly two: `resolveConversationId`, `submitTurn` — Continuity Contract Design §6's "additive extension... two operations, not one" already used its one-time justification for exactly this kind of change, for a different reason).
- **Ownership changes:** None to `Turn`/`Conversation`'s own ownership (still exclusively Conversation Engine's) — the cleanest ownership story of any option that keeps `Turn` in the picture at all.
- **Dependency changes:** Evidence Intelligence gains a dependency on `ConversationEngine` — same objection as Option 4, that CDR-007's own dependency model never names or contemplates this subsystem pairing.
- **Implementation impact:** Smaller than Option 4's (no fabricated `InboundOwnerMessage` fields needed if the new operation accepts EI's own native shapes directly) but still requires `ConversationEngine`'s own implementation to special-case a caller that isn't really having a conversation.
- **Migration impact:** Low for existing conversational call sites (nothing about `submitTurn`'s existing behaviour changes).
- **Future extensibility:** Better than Option 4 (a declared, intentional extension point rather than ad hoc fabrication) but still inherits the same "is this state actually a conversation" conceptual problem for every future non-conversational caller.
- **Architectural advantages:** Preserves `Turn`'s exclusivity rule to the letter; avoids fabricating fictitious message fields; smallest implementation footprint of any option that keeps `Turn` unchanged.
- **Architectural disadvantages:** Still repurposes Conversation Engine's owned state for a non-conversational purpose, just through a cleaner door than Option 4; adds a cross-domain dependency (Evidence Intelligence → Conversation Engine) that has no organisational rationale under CDR-007's own peer-subsystem model (Evidence Intelligence is a peer of Evidence Custodian/Knowledge Memory; nothing in CDR-007 suggests a reasoning-adjacent dependency on Conversation Engine specifically, as opposed to on Reasoning Provider directly).
- **Constitutional risks:** Same Architecture §4 invariant conflict as Option 4, somewhat mitigated but not eliminated by being an explicit, declared extension rather than an implicit fabrication.
- **Preserves existing subsystem boundaries:** Partially — `Turn` exclusivity is preserved literally, but a new, previously-unauthorised dependency edge (Evidence Intelligence → Conversation Engine) is introduced, and Conversation Engine's own conceptual boundary ("continuity between Turns... nothing else's," Architecture §4) is stretched to cover a use case it was written to exclude.
- **Long-term effect on Parker:** Turns Conversation Engine into a general-purpose "Turn factory" for any subsystem that happens to need a `Turn`-shaped value for an unrelated contract — a quiet expansion of Conversation Engine's own role that the Architecture document's own ownership boundary was written specifically to prevent.

---

## 3. Comparative Constitutional Analysis

| Axis | Opt 1 (Sealed Subject) | Opt 2 (Opaque Envelope) | Opt 3 (Split Contract) | Opt 4 (EI calls Conv. Engine) | Opt 5 (Nullable Field) | Opt 6 (Turn-Minting Op) |
|---|---|---|---|---|---|---|
| Constitutional cleanliness | Moderate — reopens CDR-007's "unmodified" clause, but stays within Reasoning Provider's own family | Low — same reopening, plus creates an ownerless type | Lowest of the "unified interface" options — directly reverses a named CDR-007 rejection | Very low — violates a named Conversation Engine invariant directly | Moderate (same as Opt 1) but weaker structurally | Low-moderate — violates same invariant as Opt 4, less severely |
| Architectural consistency | High — one interface, structurally exhaustive | Low — discards existing structure for uniformity | High internally, but duplicates the reasoning-abstraction pattern platform-wide | Very low — repurposes an unrelated subsystem's state | Moderate — same shape as Opt 1, weaker guarantees | Moderate — preserves Turn exclusivity, stretches Conversation Engine's role |
| Implementation complexity | Moderate-high (repo-wide caller updates) | High (prompt-builder rework, structure loss) | High (new parallel stack) | Low (no contract change, but fabrication logic) | Moderate-high (same as Opt 1) | Low-moderate |
| Long-term maintainability | High (single contract, compiler-enforced) | Low (opaque, hard to evolve safely) | Moderate (two contracts to keep reconciled) | Low (permanent fabrication burden) | Moderate (weaker than Opt 1, same cost) | Moderate (special-cased Conversation Engine logic) |
| Future subsystem reuse | High (sealed type extends additively) | Moderate (anyone can flatten to prose, but loses structure) | Moderate (each future subsystem may want its own contract, doesn't converge) | Poor (every future caller repeats fabrication) | Poor (nullable fields don't scale) | Poor (every future caller repurposes Conversation Engine) |
| Impact on existing repositories/code | Moderate (touches Reasoning Provider ecosystem broadly) | High (touches Reasoning Provider + Conversation Engine call sites) | Low (existing stack untouched; new stack built alongside) | Very low (no contract touched) | Moderate (same as Opt 1) | Low (Conversation Engine gains one operation) |
| Governance churn | High (CDR-007 amendment + 2 Contract Designs) | Highest (CDR-007 + 4 documents) | Highest in kind (full CDR-007 Decision reversal) | High (CDR-level, cross-subsystem) | Same as Opt 1 | Moderate-high (2 Contract Designs + likely CDR) |
| Migration effort | Moderate-high | High | High (new stack) | Low | Moderate-high | Low-moderate |

**Is any option constitutionally dominant?** No. Two partial-dominance relationships are demonstrable and worth stating plainly:

- **Option 5 is strictly dominated by Option 1** — identical documents affected, identical CDR exposure, identical caller-generalisation goal, but weaker (nullable-field) structural safety. There is no axis on which Option 5 outperforms Option 1.
- **Options 4 and 6 are both constitutionally weaker than they initially appear**, because both conflict with `19-conversation-engine.md` §4's explicit invariant against repurposing Conversation state for non-continuity purposes — a finding not visible without reading that specific Architecture-tier document in full. Option 6 is less severe than Option 4 (no fabricated message fields) but does not escape the same invariant.

Beyond that, no single option dominates across every axis. Options 1 and 3 represent a genuine, unresolved architectural fork — generalise the shared abstraction (Option 1) versus split it into parallel, subsystem-specific contracts (Option 3) — and the repository's own governance record does not settle which pattern it prefers for *this* situation specifically (it has used both patterns elsewhere for different reasons). Option 2 is dominated in spirit by Option 1 (same reopening cost, worse structural outcome) but is not strictly dominated in the formal sense since it does have one narrow advantage (smaller type-count) some readers might weight differently.

---

## 4. Recommended Option

**None is recommended as objectively dominant.** Per the governing instruction for this analysis, a recommendation is withheld because the comparison above shows genuine, non-collapsing trade-offs between the two strongest candidates (**Option 1** and **Option 3**) on axes (constitutional cleanliness vs. architectural consistency, governance churn vs. long-term convergence) that this analysis cannot resolve without a value judgment belonging to governance, not to this review. Options 2, 4, 5, and 6 are each dominated or substantially weakened relative to at least one of Options 1/3 for the reasons stated above, and are not recommended, but are preserved in the survey since the task required every constitutionally plausible option to be analysed, not only the strongest ones.

---

## 5. Governance Sequence Required

No single sequence is prescribed, since no option was found objectively dominant. For completeness, the sequence this repository's own established governance tiers would require is the same shape regardless of which option governance eventually selects, differing only in scope:

1. **Constitutional Decision Record** (new, or an explicit amendment to CDR-007) — required for Options 1, 2, 3, 5 (all touch CDR-007's own "ReasoningProvider... unmodified" language or, for Option 3, its explicit rejection of a dedicated abstraction) and very likely for Options 4 and 6 (both conflict with a named Conversation Engine invariant, which is itself the kind of cross-subsystem boundary question this repository routes through a CDR rather than a Contract Design amendment alone).
2. **Contract Design amendment(s)** — to whichever of `REASONING_PROVIDER_CONTRACT_DESIGN.md`, `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`, `CONVERSATION_ENGINE_CONTRACT_DESIGN.md`, `CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` the chosen option touches (identified per-option above).
3. **Scope Lock amendment** — `EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` §4 (Dependency Freeze) and/or §3 (Explicit Exclusions), since every option changes something that section currently freezes.
4. **Implementation Plan amendment** — `EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md` Unit 3's own text, once the shape it must implement is settled.
5. Only then would Unit 3 planning resume against a resolved contract.

---

## 6. Exact Governance Documents Affected (by option)

| Document | Opt 1 | Opt 2 | Opt 3 | Opt 4 | Opt 5 | Opt 6 |
|---|---|---|---|---|---|---|
| `CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md` | ✔ | ✔ | ✔ (directly) | ✔ (likely) | ✔ | ✔ (likely) |
| `REASONING_PROVIDER_CONTRACT_DESIGN.md` | ✔ | ✔ | — | — | ✔ | — |
| `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| `EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` | — | ✔ | — | — | — | ✔ |
| `CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` | — | ✔ | — | — | — | ✔ |
| `EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md` (Unit 3 text) | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |

---

## 7. Whether a CDR Is Required

Yes, under every option surveyed except a version of Option 3 argued so narrowly it might avoid re-litigating CDR-007's classification of Evidence Intelligence itself (it cannot avoid reversing CDR-007's explicit rejection of a dedicated abstraction, which is itself a CDR-level act regardless of framing). No option in this survey resolves the contradiction through Contract-Design-tier or Scope-Lock-tier amendment alone.

---

## 8. Confirmation No Files Changed

No file was created, edited, or deleted. No Kotlin was written. No governance document was modified. No production code or test was touched.

## 9. No Git Actions

No git command was run. Nothing was staged, committed, or pushed.
