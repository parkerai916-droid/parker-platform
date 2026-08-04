# Reasoning Provider — `ReasoningSubject` Contract Design Study

## Status

**Constitutional design study only. Not an amendment.** No governance document is amended by this document. No Kotlin is implemented, proposed as a diff, or changed. Neither `src/` nor `tests/` is touched. Nothing is staged, committed, or pushed.

This study determines the minimum constitutionally valid public design for `ReasoningSubject`, the selector type identified as necessary by `docs/reviews/REASONING_PROVIDER_CONTRACT_DESIGN_AMENDMENT_PROPOSAL.md`. It precedes, and does not itself constitute, any amendment to `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md`.

---

## 1. Why a Selector Is Required

Modifying `ReasoningProviderRequest` alone — without introducing a selector — cannot solve the constitutional problem, and this is demonstrable rather than a matter of preference.

**The two alternatives to a selector are the only other ways to widen a single field, and each is independently foreclosed by existing governance:**

1. **Widening `turn: Turn` into an open, unconstrained type** (an interface both `Turn` and an Evidence-Intelligence-owned subject could implement, or an unconstrained supertype) breaks the "closed, exhaustive, no unauthorised third case" discipline this repository enforces everywhere a value must be one of a known, finite set of things. `CandidateMemoryCoreRecord` is "closed to exactly two cases... never a third without a further Contract Design amendment"; `MemoryCoreRecord` is closed to exactly four; `ReasoningProviderResponse` itself is closed to exactly three (`Goal`, `Reply`, `NoAction`), with an explicit rule that no fourth variant may ever be added for failure. An open supertype cannot make this guarantee — nothing would prevent a future, unauthorised caller from supplying a subject no governing document ever considered, since Kotlin's exhaustiveness checking (the mechanism every one of these existing selectors relies on) applies only to `sealed` hierarchies, not to open interfaces.
2. **Adding a second, nullable field alongside `turn: Turn`** reintroduces the exact anti-pattern the Reasoning Provider Contract Design's own Minimalism Review already rejected on the response side: "Why a sealed type, not three separate boolean/nullable fields... a sealed type makes that structurally impossible to violate, rather than merely conventionally expected of three independent nullable fields." A nullable-field pair on the request side would carry the identical defect — nothing prevents both fields being populated, or neither, and every `ReasoningProvider` implementation would need to defensively re-derive an invariant a sealed type would instead guarantee by construction.

**A third option — making `turn` nullable with no replacement field at all — is not a generalisation but a gap:** it would leave Evidence Intelligence with no field on `ReasoningProviderRequest` capable of carrying any information about what is actually being reasoned about, worse than the status quo rather than better.

A closed, sealed selector is therefore the only shape that achieves genuine generalisation (more than one legitimate subject) while preserving genuine closure (exhaustive, structurally guaranteed to be exactly one of a known, finite set) — precisely the property `CandidateMemoryCoreRecord`, `MemoryCoreRecord`, and `ReasoningProviderResponse` already demonstrate this repository requires wherever such a choice exists.

---

## 2. Ownership

**`ReasoningSubject` is owned by the Reasoning Provider Contract Design** — the same document that already owns `ReasoningProviderRequest`, `ReasoningProviderResponse`, and `ReasoningContext`.

**Justified by direct repository precedent, not by analogy alone:**

- `CandidateMemoryCoreRecord` is "owned exclusively by Evidence Intelligence" even though it wraps `CandidateAssertion` and `CandidateRelationship` — both Memory-Core-owned types. Ownership of the *selector* is assigned to the document that needs the selector to compile one of its own return shapes (`EvidenceAnalysisResult.CandidateRecordProduced`), not to the owner of what is selected.
- `MemoryCoreRecord` (`src/interfaces/MemoryCore.kt`) follows the identical rule from the other direction: it is owned by Memory Core because Memory Core's own `MemoryRetrieval` interface needs it to return a genuinely heterogeneous result across `Entity`/`Document`/`Assertion`/`Relationship` "without forcing [them] into an artificial shared supertype of their own."

Applying this rule without modification: the Reasoning Provider Contract Design needs `ReasoningSubject` to compile `ReasoningProviderRequest.subject`, so `ReasoningSubject` belongs there — never to Evidence Intelligence, never to Conversation Engine, and never left ownerless as a "shared constitutional abstraction" (a self-inflicted violation of this repository's own repeated "every public object has exactly one constitutional owner" rule, already flagged as a risk in the Remediation Analysis's treatment of a fully opaque envelope). `Turn` remains exclusively Conversation-Engine-owned throughout — `ReasoningSubject` only ever references an already-constructed `Turn`; it never constructs, modifies, or claims any stake in it, exactly as `CandidateMemoryCoreRecord` never constructs, modifies, or claims any stake in `CandidateAssertion`/`CandidateRelationship`.

---

## 3. Minimum Public Shape

Described architecturally; no Kotlin design is proposed.

- **Number of cases.** Two, at authorisation — the minimum the two currently-authorised callers require: one case selecting `Turn` (Conversation Engine's existing, unmodified subject), one case selecting whatever existing, unmodified analytical-subject shape a future Evidence Intelligence Contract Design amendment authorises. Not more, in anticipation of hypothetical future callers — mirroring `EvidenceAnalysisResult`'s own discipline of exactly four categories, "no fifth variant... no future revision may weaken," fixed to what is presently authorised, not to what might eventually be needed.
- **Responsibilities.** Select exactly one already-existing, already-owned subject value per invocation, and carry it, opaquely and unmodified, to `ReasoningProvider.reason`.
- **Behaviour.** None beyond the ordinary structural behaviour any plain, immutable value already has (equality, textual representation, copying). No method that itself reasons, interprets, translates, or performs any domain act.
- **Authority.** None. Grants no reasoning, authorisation, provenance, evidential-state, ownership, or permission authority of its own — a pure selection mechanism, identical in this respect to `CandidateMemoryCoreRecord`.
- **Ownership.** As fixed in Section 2 — the Reasoning Provider Contract Design owns the selector; each wrapped value's own owner (Conversation Engine for `Turn`; Evidence Intelligence for its own case) is unchanged.
- **Extensibility.** Closed, not open. A third case requires a further Contract Design amendment — mirroring `CandidateMemoryCoreRecord`'s "never a third without a further Contract Design amendment" verbatim. Not a generic, type-parameterised mechanism reusable for any other pair of types.
- **Relationship to `Turn`.** Wraps `Turn` entirely unmodified. Does not extend, subclass, or alter it in any way. Never constructs a `Turn` itself — every value it carries in that case arrives already constructed by `ConversationEngine`, preserving `CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` §12's "the only component that ever constructs a... `Turn`" exactly as written.
- **Relationship to Evidence Intelligence.** Evidence Intelligence is a legitimate *constructor* of its own case's payload — this does not make Evidence Intelligence the *owner* of `ReasoningSubject` itself, exactly as Evidence Intelligence's legitimate construction of `CandidateMemoryCoreRecord.OfAssertion` does not make it the owner of `CandidateAssertion` (Memory Core's own type). Construction of one case's content and ownership of the selector are distinct, and only the latter is fixed by Section 2.

---

## 4. Frozen Properties

Mirroring `CandidateMemoryCoreRecord`'s own frozen-property style (`src/interfaces/EvidenceIntelligence.kt`) directly, none of which any future revision may weaken:

- **Behaviour-free** — no operation beyond the ordinary structural operations (equality, textual representation, copying) any plain value already has; no operation that performs reasoning, invocation, translation, or any other domain act.
- **Closed to exactly two cases at authorisation** — one selecting an existing, unmodified `Turn`, one selecting whatever existing, unmodified analytical-subject shape a future Evidence Intelligence Contract Design amendment authorises — never a third case without a further Contract Design amendment.
- **Owns no data beyond the one selected subject value** — no field beyond the single wrapped value.
- **Introduces no new responsibility to `ReasoningProvider` itself** — `ReasoningProvider.reason`'s own signature and "pure callee" status are unchanged; this type only supplies the shape `ReasoningProviderRequest.subject` needs to compile a caller-agnostic request, since Kotlin has no union type.
- **Grants no acceptance, persistence, retrieval, reasoning, confidence, evidential-state, provenance, ownership, or authorisation authority of its own** — a pure selection mechanism; every one of those responsibilities remains exactly where existing governance already assigns it.
- **Does not modify `Turn`, or whatever Evidence-Intelligence-owned type it wraps** — both remain reused, unmodified; this type only references them, never extends, subclasses, or amends either.
- **Not a generic, reusable union mechanism** — closed to these two named, existing types specifically, never a type-parameterised abstraction usable for any other pair of types.
- **Creates no independent dependency entitlement for any other subsystem.** A `ReasoningProvider` implementation, or any composition-level caller, may inspect a value of this type solely to determine which existing subject it carries, in order to interpret that subject appropriately — that consumption grants no ownership, authority, extension right, or independent dependency entitlement over this type itself.
- **No Kotlin name, package, method, or interface is assigned to this type by this study** — exactly as none is assigned to it by the Amendment Proposal that first identified its need.

---

## 5. Explicit Exclusions

What `ReasoningSubject` must never become, each grounded in existing governance:

- **Not a reasoning engine.** It carries a subject; it never itself reasons, interprets, or produces output. That remains exclusively `ReasoningProvider.reason`'s own responsibility (Reasoning Provider Architecture §3: "Never executes actions... Never authorises actions"; Contract Design Constitutional Boundaries: "A Reasoning Provider proposes. It authorises nothing, executes nothing").
- **Not a registry.** No discovery, lookup, or provider-selection mechanism of any kind. A `ReasoningProviderRegistry` is already explicitly excluded at the Architecture tier ("No concrete need for multiple simultaneously-selectable providers has been demonstrated") and restated at Contract Design tier ("Already excluded at the architecture level; this document does not reopen it"). `ReasoningSubject` selects *input shape*, never *provider identity*.
- **Not a router.** It does not decide which `ReasoningProvider` implementation handles a given case, does not dispatch based on its own case, and holds no logic of any kind — restating the "behaviour-free" property above.
- **Not an abstraction layer.** It does not sit between a caller and `ReasoningProvider`, does not wrap the interface itself, and introduces no new method or operation. `ReasoningProvider.reason(request): response` remains completely unchanged; only one field's type changes.
- **Not an authority.** Grants no reasoning, evidential, provenance, ownership, or permission authority of its own — restating the Constitution's "Parker owns authority. Modules provide capability" and the identical "creates no acceptance, persistence, retrieval, reasoning... authority of its own" property already fixed for `CandidateMemoryCoreRecord`.
- **Not a provider selector.** Distinct from "router," above: it never chooses, ranks, or filters among multiple configured `ReasoningProvider` implementations. That question — Architecture §14 Item 3, "How a specific Reasoning Provider is selected or configured" — remains explicitly open and is entirely unaffected by this type.
- **Not a workflow model.** Carries no state, no sequence, and no multi-step lifecycle — restating `ReasoningProviderRequest`'s own existing ownership boundary ("transient, per-invocation value... not retained") and the Architecture's own "A Reasoning Provider owns no persistent Parker-modelled state."
- **Not a general-purpose union type.** Closed to exactly the cases a Contract Design amendment authorises — never a generic `Either`/union-style mechanism reusable by any future, unrelated pair of types, restating `CandidateMemoryCoreRecord`'s identical exclusion verbatim.

---

## 6. Dependency Analysis

Confirmed unchanged:

- **`ReasoningProvider`'s own dependency-free status** — no reference to `PlannerRuntime`, `AgentRuntime`, `TaskManagerRuntime`, `MemoryStore`, `WorldModel`, `ExecutionPipeline`, `PermissionEngine`, `ToolRegistry`, or `ModuleRegistry` is introduced by `ReasoningSubject`, since it only ever wraps `Turn` or an Evidence-Intelligence-owned value, neither of which references any of them.
- **Evidence Intelligence's own dependency table** (`EvidenceCustodian.retrieve`, `MemoryRetrieval`, `ReasoningProvider`) — unchanged. No dependency on `ConversationEngine` is introduced; Evidence Intelligence constructs only its own case's payload, never a `Turn`.
- **Conversation Engine's own dependencies** — unchanged. It continues to construct `Turn` exclusively; wrapping that `Turn` in a selector case at the call site introduces no new dependency.
- **CDR-007's dependency model** (§3, §12) — unchanged. `ReasoningProvider` remains reused in identity (same interface signature); only the internal shape of one already-authorised type changes.
- **Memory Core, Knowledge Memory, Evidence Custodian** — none gains or loses a dependency; none is referenced by `ReasoningSubject` in any case.

---

## 7. Confirmation

No governance document was amended in the preparation of this study. No Kotlin was implemented. No production code or test was touched. Nothing was staged, committed, or pushed.
