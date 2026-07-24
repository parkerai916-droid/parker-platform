# Sprint 11, Unit 4 — Conversation History Source — Engineering Checkpoint

## Status

**Engineering Checkpoint, PES-001 Stage 9, governance-only Unit.**
Reviews risk across `CONVERSATION_HISTORY_SOURCE_IMPLEMENTATION_PLAN.md`
and `CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md` before any Contract
Design begins. Per this Unit's own instruction, unresolved questions are
recorded here, not answered — answering any of them by implementation
detail would exceed this Checkpoint's own authority.

---

## 1. Architectural Risks

- **The central risk this whole Unit exists to name: conversation
  identity is not known at the point a future reader would need it.**
  `ReasoningContextAssembler.assemble` runs before
  `ConversationEngine.submitTurn`
  (`PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md` Section 2), and
  `ConversationDisposition`'s own KDoc states plainly that "the caller
  supplies no `ConversationId` on the way in" — only `submitTurn` itself
  decides whether an inbound message continues an existing Conversation.
  Consequently, nothing upstream of `submitTurn` — including any future
  Conversation History Source caller — has a `ConversationId` to query
  with. Not solved here. A future Contract Design must decide how (or
  whether) a conversation can be identified before a Turn exists — for
  example, by some stable key derived from `(channelId,
  senderPrincipalId)` rather than `ConversationId` itself — without
  redesigning `ConversationEngine`'s own ownership of continuity
  recognition.
- **`InMemoryConversationEngine` does not yet implement conversation
  continuity at all.** Its own KDoc: "every inbound Turn begins a new
  Conversation... there is no stored state to consult." `submitTurn`
  mints a fresh, random `ConversationId` every call. There is, today, no
  continuing Conversation for Conversation History Source to retrieve
  history *from* — this boundary's own reason to exist presupposes a
  capability (continuity recognition) that does not yet exist downstream
  of it. A future Contract Design must decide whether Conversation
  History Source's own implementation is blocked on continuity
  recognition landing first, or whether it can be designed and even
  partially implemented against a hypothetical continuing Conversation
  now. Not decided here.
- **The illustrative principle "a read boundary, not a conversation
  owner" may not survive contact with a real interface shape.** Until a
  Contract Design proposes one, it is not known whether "retrieve prior
  Turns" can be expressed as cleanly as `IdentityService.resolve` or
  `ToolRegistry.listAll` were for the Reasoning Context Assembler, or
  whether it will need parameters (a Turn limit, a time window) that
  complicate the "one read, one result" shape this Scope Lock's Section
  6 (Threading) assumes.

## 2. Dependency Risks

- **Whether Conversation History Source depends on `ConversationEngine`
  at all is not decided, and is genuinely hard.** `CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md`
  Section 6 (Construction) names two possibilities — a new, narrower,
  read-only surface added to a future `ConversationEngine` revision, or
  an independent store `ConversationEngine` writes to in addition to its
  own state — without choosing between them. The first risks becoming a
  redesign of a frozen component (out of any single future Unit's
  authority without a separate `ConversationEngine`-focused Contract
  Design pass); the second risks a second, competing source of truth for
  the same `Conversation`/`Turn` state, in tension with this document's
  own governing principle (Section 3) unless very carefully scoped as a
  read-through projection, never an independent write path.
- **Discipline risk: reaching for `ConversationEngine` directly "just
  this once" under delivery pressure.** Named explicitly, mirroring
  `SPRINT_11_UNIT_2_ENGINEERING_CHECKPOINT.md` Section 4's identical
  warning for the Reasoning Context Assembler: a future implementation
  could be tempted to inject a live `ConversationEngine` reference to
  unblock retrieval quickly, quietly handing a reader the ability to
  call `submitTurn` and violating the read-only boundary this whole Unit
  exists to establish. Named here so a future implementation's review
  has this exact risk to check against.
- **Unknown shape-fit with the Reasoning Context Assembler's own
  existing, frozen interface.** `ReasoningContextAssembler.assemble(message: InboundOwnerMessage): ReasoningContext`
  takes exactly one parameter today. If Conversation History Source
  eventually requires a per-call parameter the raw `InboundOwnerMessage`
  cannot supply (Architectural Risk 1, above), threading that parameter
  through may require either revising `ReasoningContextAssembler`'s own
  Contract Design (a frozen document, Sprint 11 Unit 2) or resolving
  conversation identity by some other means entirely. Neither is decided
  here.

## 3. Sequencing Risks

- **This Unit is, by its own task instructions, governance only —
  meaning a real implementation cannot begin until at least two further
  Units land: a Contract Design for Conversation History Source itself,
  and (per Architectural Risk 2, above) very possibly a separate,
  earlier Unit addressing `ConversationEngine`'s own lack of continuity
  recognition.** The ordering between "design Conversation History
  Source's read interface" and "implement continuity recognition in
  `ConversationEngine`" is not decided here — either could plausibly
  come first, and a future Sprint 11 planning pass (not this Checkpoint)
  should decide which.
- **A second future production entry point risk, restated from
  `SPRINT_11_UNIT_2_ENGINEERING_CHECKPOINT.md` Section 4, applies
  identically here.** If a second Communication Channel or trigger ever
  calls into the runtime independently of today's one
  `ParkerRuntime.submitOwnerMessage` call site, whether it shares the
  same Conversation History Source instance or requires its own
  invocation path is not decided by this Unit either.

## 4. Unresolved Questions (Recorded, Not Answered)

- How is "which conversation" identified before `ConversationEngine.submitTurn`
  runs and a `ConversationId` exists? (Architectural Risk 1.)
- Does Conversation History Source require `ConversationEngine` itself
  to gain conversation-continuity recognition first, or can it be
  designed independently of that capability landing? (Architectural Risk
  2.)
- Is Conversation History Source backed by a new read-only surface on a
  future `ConversationEngine` revision, an independent store
  `ConversationEngine` also writes to, or some third mechanism?
  (Dependency Risk 1, `CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md` Section
  6.)
- Who is Conversation History Source's one production caller — a future
  Reasoning Context Assembler revision directly, or the composition root
  on its behalf, passing a result into the Assembler's own existing
  single-parameter shape? (`CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md`
  Section 4.)
- How many prior Turns, if any limit at all, does one retrieval return?
  Unbounded history retrieval is excluded by nothing this document
  states explicitly, and a Contract Design must decide it.
- Does retrieval span only the current Conversation, or could a future
  need (not named by this Unit) require history across more than one
  Conversation for the same owner? Not raised by Steven's own task
  instructions and not assumed here either way.

## 5. Recommendation

No risk above is assessed as blocking Scope Lock acceptance — every one
is either a genuine open design question a future Contract Design must
resolve, or a discipline risk to check against during that future
Contract Design's own review. This Checkpoint recommends architectural
review of `CONVERSATION_HISTORY_SOURCE_IMPLEMENTATION_PLAN.md` and
`CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md` before any Contract Design
Unit begins, per PES-001's own Stage sequencing. In particular, this
Checkpoint recommends that whichever future Unit is chartered to write
that Contract Design be explicitly asked to resolve Architectural Risks 1
and 2 first — everything else in this Checkpoint is comparatively
downstream of those two.
