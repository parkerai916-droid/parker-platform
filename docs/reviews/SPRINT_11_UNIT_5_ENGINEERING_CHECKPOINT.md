# Sprint 11, Unit 5 — Conversation Identity Architecture — Engineering Checkpoint

## Status

**Engineering Checkpoint, PES-001 Stage 9, governance-only Unit.**
Reviews risk across
`CONVERSATION_IDENTITY_ARCHITECTURE_IMPLEMENTATION_PLAN.md` and
`CONVERSATION_IDENTITY_ARCHITECTURE_SCOPE_LOCK.md` before any Contract
Design begins. Per this Unit's own instruction, risks, assumptions, and
unresolved questions are recorded here, not answered — answering any of
them by implementation detail would exceed this Checkpoint's own
authority.

---

## 1. Architectural Risks

- **This document could be misread as authorising a new runtime
  component that competes with the Conversation Engine.** Implementation
  Plan Section 2 (Fact 1) and Scope Lock Section 4 both state plainly
  that recognition ownership remains the Conversation Engine's, unchanged
  — but the very act of naming "Conversation Identity Architecture" as
  its own governance document, parallel in form to Conversation History
  Source's, creates a real risk that a future reader treats it as a
  second peer component. Named explicitly here so a future Contract
  Design's own review has this exact misreading to check against.
- **The recognition rule (Fact 2) remains open across two prior
  governance stages, and this Unit does not close that gap either.**
  Whatever future Contract Design finally resolves
  `19-conversation-engine.md` Section 13 Item 3 must do so consistently
  with this document's own architectural principles (Scope Lock Section
  3) — stability, content-independence, no persistence implication. This
  is a real constraint on that future work, not a risk this Unit
  resolves.
- **Fact 4 (pre-Turn identity availability) may not be resolvable without
  touching `ConversationEngine`'s own interface, and this Unit is not
  authorised to decide that it must be.** Two paths are visible, neither
  is designed here, and choosing between them is a genuine architectural
  decision this Checkpoint declines to make: (a) some component
  independently derives a *provisional* conversation identity before
  `submitTurn` runs, from information already on `InboundOwnerMessage`
  (e.g. `channelId` and `senderPrincipalId`), which `ConversationEngine`
  would need to somehow reconcile with its own eventual, authoritative
  recognition; or (b) `ConversationEngine`'s own interface eventually
  gains a way to expose or pre-compute identity earlier, which would be a
  contract change to a currently-frozen component, out of any single
  future Unit's authority without a dedicated `ConversationEngine`-focused
  Contract Design revision. Both paths carry real risk; neither is
  authorised here.

## 2. Dependency Risks

- **"Read-only" and "side-effect free" (Scope Lock Section 8) are
  documented discipline, not type-enforced, exactly as the identical
  risk was already recorded for `IdentityService`/`ToolRegistry` usage
  in `SPRINT_11_UNIT_2_ENGINEERING_CHECKPOINT.md` Section 2.** Whatever
  future mechanism exposes conversation identity, nothing in Kotlin's
  type system by itself prevents a future implementation from also
  granting write access to `Conversation`/`Turn` state. Recorded, not
  solved (splitting `ConversationEngine`'s own interface would modify a
  frozen contract, out of this Unit's authority).
- **Conversation History Source's own Contract Design cannot proceed
  further than Conversation Identity Architecture allows.** Restating
  `CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md` Section 9's own "prerequisite"
  framing from this document's side: any future Conversation History
  Source Contract Design that assumes a specific identity-resolution
  mechanism before one is actually decided (Fact 2, Fact 4) risks having
  to be revised once that mechanism lands. Sequencing risk, restated in
  Section 3 below.
- **Discipline risk: a future implementer reaching for `ConversationEngine`
  directly, or inventing a second, independent conversation-identification
  scheme, "just this once."** Mirrors
  `SPRINT_11_UNIT_2_ENGINEERING_CHECKPOINT.md` Section 4's and
  `SPRINT_11_UNIT_4_ENGINEERING_CHECKPOINT.md` Section 2's identical
  warning: under delivery pressure, a future Conversation History Source
  implementation could be tempted to derive its own conversation
  identity independently of whatever `ConversationEngine` eventually
  recognises, producing two, possibly divergent, notions of "which
  conversation this is." Named here explicitly so a future
  implementation's review has this exact risk to check against.

## 3. Sequencing Risks

- **This Unit was chartered after Conversation History Source (Sprint
  11, Unit 4), even though Conversation Identity is logically that
  Unit's own prerequisite.** Not itself a defect — Unit 4's own
  Engineering Checkpoint named this exact gap and recommended it be
  resolved first — but it means any reader encountering Unit 4's
  documents before this one will find an unresolved forward reference.
  No document is revised to fix this; both stand as written, in the
  order they were produced.
- **The ordering between resolving Fact 2 (recognition rule) and Fact 4
  (pre-Turn availability) is not decided.** Either could plausibly be
  resolved first, or both could require a single, combined Contract
  Design pass — not decided here, mirroring
  `SPRINT_11_UNIT_4_ENGINEERING_CHECKPOINT.md` Section 3's identical,
  unresolved sequencing question for `ConversationEngine`'s own
  continuity recognition versus Conversation History Source's own
  design.
- **A second future production entry point risk, restated identically
  from `SPRINT_11_UNIT_2_ENGINEERING_CHECKPOINT.md` Section 4 and
  `SPRINT_11_UNIT_4_ENGINEERING_CHECKPOINT.md` Section 3, applies here
  too.** If a second Communication Channel or trigger ever calls into
  the runtime independently of today's one `ParkerRuntime.submitOwnerMessage`
  call site, how conversation identity would be recognised or exposed
  for that second path is not decided by this Unit either.

## 4. Assumptions Requiring Future Validation

- **That `ConversationDisposition`'s existing fields (`isNewConversation`,
  `Conversation.turnIds`) are sufficient to carry a real recognition
  rule's richer answer, with no further contract change.**
  `CONVERSATION_ENGINE_IMPLEMENTATION_PLAN.md` Section 5 states this as
  the reason its own conservative default was judged safe, but no real
  recognition rule has ever been implemented against these shapes — this
  is a stated expectation, not a validated one. A future Contract Design
  should confirm it holds before relying on it further.
- **That a stable conversation identifier can be made available before
  `ConversationEngine.submitTurn` runs without either duplicating its
  recognition authority or modifying its interface.** This is the
  central unvalidated assumption behind Fact 4 (Architectural Risk 3,
  above) — this Unit names it as an assumption, not a fact, precisely
  because neither path sketched in Architectural Risk 3 has been
  designed or tested.
- **That "one production owner, one production caller" (the discipline
  `CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md` Section 4 and this
  document's own Section 4 both rely on) remains achievable once a real
  recognition rule and a real pre-Turn exposure mechanism both exist.**
  Assumed, not yet demonstrated against a concrete design.

## 5. Unresolved Architectural Questions (Recorded, Not Resolved)

- What is the actual recognition rule — correlation-based,
  time-window-based, explicit-session-based, or some combination
  (`19-conversation-engine.md` Section 13 Item 3, still open after two
  governance stages)?
- How, if at all, can a conversation identity be resolved or provisioned
  before `ConversationEngine.submitTurn` runs, without granting a reader
  the ability to call `submitTurn` itself or duplicating its recognition
  authority (Fact 4)?
- Is a *provisional* conversation identity (available before a Turn
  exists, possibly later reconciled with `ConversationEngine`'s own
  authoritative recognition) an idea worth pursuing, or does it risk two
  competing notions of conversation identity? Raised here as a genuinely
  open design question, not proposed as a solution.
- Does Conversation History Source become the only future reader of
  conversation identity, or should this document anticipate others? Not
  raised by this Unit's own task instructions and not assumed either way.
- Idle/termination rules for a Conversation (`19-conversation-engine.md`
  Section 13 Item 4) — inherited, unresolved, and directly relevant to
  when a conversation identifier is considered expired
  (`CONVERSATION_IDENTITY_ARCHITECTURE_SCOPE_LOCK.md` Section 5).
- Whether a Conversation — and therefore its identifier — may span more
  than one Communication Channel (`19-conversation-engine.md` Section 13
  Item 5) — inherited, unresolved.

## 6. Recommendation

No risk above is assessed as blocking Scope Lock acceptance — every one
is either a discipline risk for a future Contract Design to check
against, an inherited open question this Unit correctly declines to
re-litigate, or a genuinely new question this Sprint's own findings
surfaced for the first time. This Checkpoint recommends architectural
review of
`CONVERSATION_IDENTITY_ARCHITECTURE_IMPLEMENTATION_PLAN.md` and
`CONVERSATION_IDENTITY_ARCHITECTURE_SCOPE_LOCK.md` before any Contract
Design Unit begins, per PES-001's own Stage sequencing. In particular,
this Checkpoint recommends that whichever future Unit is chartered next
be explicitly asked to decide the ordering question in Section 3 above
— resolving the recognition rule first, resolving pre-Turn availability
first, or scoping a single Contract Design to both — before further
governance documents are produced for either Conversation Identity or
Conversation History Source.
