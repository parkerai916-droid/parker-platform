# Sprint 11, Unit 1 — Production Reasoning Context — Engineering Checkpoint

## Status

**Engineering Checkpoint, PES-001 Stage 9, architecture-only Unit.**
Evaluates architectural risk, assumptions, and unresolved questions
before any Contract Design or implementation begins. Does not review
code — none exists for this Unit. Does not answer the questions it
raises; per this Unit's own instruction, unresolved questions are
recorded, not resolved by implementation.

---

## 1. Architectural Risks

- **The Reasoning Context Assembler could become a God object.** It sits
  at the one point in the runtime where message content, identity,
  channel, and (in future Units) Memory and World Model excerpts all
  converge. Nothing about its own architecture prevents a future
  implementation from reaching further than the Scope Lock's Section 1
  permits, unless each future addition is checked against Section 1's
  own justification discipline (every included item argued for
  individually) and Section 2's exclusions (restated, not merely
  implied) before being added. This risk is named, not mitigated, by
  this Checkpoint — mitigation is enforcement discipline for whichever
  future Unit implements it, and for whichever Unit reviews that
  implementation against this Scope Lock.
- **A flat `List<String>` may strain under real content.** Section 1
  commits to rendering seven distinct kinds of information (current
  conversation, participant identities, requesting principal identity,
  current request, active channel, available tool descriptions, current
  time) into `ReasoningContext`'s existing, frozen, structurally opaque
  `entries: List<String>` shape. `REASONING_PROVIDER_CONTRACT_DESIGN.md`
  Section 10 already anticipated and rejected structured alternatives,
  for good reason (avoiding a premature assembly-ownership decision) —
  but that decision was made before assembly ownership was assigned at
  all. Now that this Unit assigns it, a future Unit may find the flat
  shape genuinely strains once real rendering logic exists. This
  Checkpoint does not propose changing `ReasoningContext`'s shape — that
  would be a Contract Design decision, out of this Unit's own authority
  — but names the risk so a future Contract Design does not rediscover
  it from scratch.
- **Coupling to Memory and World Model integrations that do not exist
  yet.** The Reasoning Context Assembler's own future implementation
  depends on two future Units (Memory integration, World Model
  integration) neither of which this Unit designs. Sequencing risk:
  if the Assembler is implemented before either integration exists, its
  first real implementation may only be able to supply the
  already-available fields (current request, identities, channel, tool
  descriptions) and none of the Memory- or World-Model-sourced content
  `reasoning-context.md`'s own architecture ultimately calls for — a
  partial implementation, not a defect, but worth naming so a future
  Unit does not mistake "compiles and passes tests" for "fully realises
  the three-layer architecture."
- **Available tool descriptions could still be mistaken for
  authorisation, if worded carelessly.** Section 1 permits Reasoning
  Context to carry what a Tool is described as doing, deliberately named
  "available," not "authorised" (Scope Lock Section 1), specifically to
  avoid this risk. Naming alone does not eliminate it: if a future
  implementation is careless about the prose it actually renders, a
  reasoning provider's proposal could still read as though a Tool's mere
  presence in Reasoning Context implies it is already approved for use.
  Section 7 of the Scope Lock states the constitutional boundary
  explicitly for this reason; the residual risk is a future
  implementation detail, not an architectural gap, but is named here
  because it is the single clearest way this Unit's own scope could be
  misused to erode "Trust authorises" if not watched.

## 2. Assumptions

- Memory integration and World Model integration are each a distinct,
  future, separately-scoped Unit — this Checkpoint assumes neither
  begins as a side effect of this Unit or the Unit that eventually
  implements the Reasoning Context Assembler.
- `ReasoningContext`'s frozen shape
  (`data class ReasoningContext(val entries: List<String>)`) does not
  change as a consequence of this Unit. If a future Contract Design
  changes it, that Contract Design — not this Scope Lock — is the
  correct place to revisit Section 1's item list against a richer shape.
- The Production Composition Root (`ParkerRuntime`) remains the sole
  production caller of the frozen coordinator chain. If a second
  production entry point is ever added (a second Communication Channel
  module, a scheduled-task trigger — both named as plausible future work
  in `SPRINT_10_UNIT_4_ENGINEERING_CHECKPOINT.md` Section 4), this
  Scope Lock's Section 4 (Ownership) would need revisiting: either every
  entry point invokes the same Reasoning Context Assembler, or ownership
  is reconsidered. Not decided here.
- `InboundOwnerMessage`, `ToolDescriptor`, `IdentityService`, and
  `ParkerRuntimeConfig`'s owner fields remain shaped as they are today.
  Section 1's justifications cite their current, frozen fields directly;
  a future change to any of those contracts would need this Scope Lock's
  Section 1 re-justified against the new shape, not silently assumed
  compatible.

## 3. Unresolved Questions

Recorded, not answered — per this Unit's own instruction not to resolve
unresolved questions with implementation:

- **How much prior conversation history belongs in "current
  conversation"?** Section 1 justifies including it in principle; it
  does not bound how many prior Turns, or by what rule (count, time
  window, relevance) a future Reasoning Context Assembler would select
  them. A future Contract Design must decide this.
- **What prose rendering do "available tool descriptions" and "current
  time" actually take?** Section 1 justifies their
  inclusion; it does not specify wording, ordering, or how many `Tool`s'
  descriptions are included versus filtered to the current channel's own
  `requiredPermissions`. A future Contract Design must decide this.
- **Does every inbound message get a full re-assembly, or is any part
  of Reasoning Context cacheable within a single Conversation?** Section
  2 excludes Reasoning Context itself being a cache; it does not decide
  whether the Reasoning Context Assembler may internally memoise any of
  its own inputs (e.g. a Conversation's participant list, if unchanged
  across consecutive Turns) without that memoisation becoming a cache
  Reasoning Context itself owns. A future Contract Design must decide
  this, consistent with Section 2's own boundary.
- **Where exactly does the Reasoning Context Assembler's own dependency
  boundary sit once Memory and World Model exist?** This Unit names them
  as future input sources (Implementation Plan Section 8); it does not
  decide whether the Assembler depends on `MemoryStore`/`WorldModel`
  directly, or on some narrower read-only excerpt-producing interface
  each of those future Units might expose instead. A future Contract
  Design, informed by whatever Memory and World Model integration Units
  precede it, must decide this.
- **Does a second future production entry point share one Reasoning
  Context Assembler instance, or does each entry point own its own?**
  Named in Assumptions, above, as not decided here — recorded again here
  because it is a genuine open question, not merely an assumption that
  happens to hold today.

## 4. Recommendation

This Checkpoint recommends architectural review of the Scope Lock before
any Contract Design begins, per PES-001's own Stage sequencing. No
implementation risk is assessed as blocking — every risk named in
Section 1 is a discipline risk for future work, not a defect in this
Unit's own architecture. `IMPLEMENTATION_GAPS.md` #53's `ReasoningContext`
assembly item should be updated to reflect "ownership assigned,
implementation not begun" rather than "unassigned," once this Checkpoint
and its Scope Lock are accepted.
