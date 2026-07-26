# Sprint 11, Unit 5 (Contract Design Stage) — Engineering Checkpoint

## Status

**Implementation status: implemented, not yet verified.** The Contract
Design this Checkpoint reviews has been realised in Kotlin (Sprint 11 Unit
5 -- Conversation Continuity Implementation; see
`docs/implementation/IMPLEMENTATION_HISTORY.md`'s own "Unit 5" entry for
the full change list). Every risk this Checkpoint's own Section 3
("Compatibility Risks") and Section 5 ("Migration Risks") named has been
addressed directly: the four named test files were migrated, the
concurrency mechanism is a single `Mutex`, and the rejection mechanism is
`IllegalArgumentException` (this repository's own established convention
-- see the Implementation History entry's "Exception decision" for the
narrowest-existing-mechanism review this rests on). Build/test
verification itself has not been performed by this Unit (sandbox
limitation, disclosed in the Implementation History entry); Steven will
run the complete suite and report results, per this Unit's own task
instructions. This section below is otherwise preserved unchanged, as the
historical record of the risk review performed before implementation
began.

**Second revision, following conditional approval.** Architectural review
approved the corrected authority/propagation design subject to confirming
four concurrency and failure guarantees are explicit, binding contract
terms rather than implied behaviour: resolution atomicity, idempotence
while active, no re-resolution inside `submitTurn` (with defined
rejection behaviour for unknown/stale identifiers), and resolution failure
stopping the entire pipeline. These are now stated as Contract Design
Section 5.1's four Binding Guarantees. This Checkpoint's Section 5
(Migration Risks) is updated accordingly: concurrency is no longer an
undecided risk about *whether* atomicity is required — that is now frozen
— only the *mechanism* realising it remains deferred.

**Revised following architectural review (Contract Authority and
Propagation Correction).** The prior version of this Checkpoint deferred
"whether `ParkerRuntime` reuses its own pre-computed `ConversationId`, or
`ConversationEngine` independently re-evaluates the formula" as an
implementation choice. Architectural review correctly identified that this
was not a neutral implementation choice — it was an unresolved
authority-and-propagation contradiction load-bearing enough to block
acceptance. This revision reflects the corrected Contract Design: exactly
one authority (`ConversationEngine`), exactly one decision
(`resolveConversationId`, called once per inbound message), propagated
unchanged to every consumer, including `ConversationEngine`'s own later
`submitTurn` call.

---

## 1. Resolved Architectural Decisions (Not Reopened Here)

- **Model: Resolved identity, not Derived identity.** Continuity key
  `(channelId, senderPrincipalId)` resolves, via `ConversationEngine`'s own
  owned state, to a `ConversationId` — never computed as a stateless
  function of the key alone (Contract Design Sections 2-3).
- **Sole authority: `ConversationEngine`.** Not divided with `ParkerRuntime`,
  which only invokes the authority earlier in the pipeline than
  `submitTurn` runs today (Contract Design Section 4).
- **`ConversationEngine`'s interface changes, additively and honestly.**
  New `resolveConversationId` operation; `submitTurn` gains one additive
  parameter and stops deciding identity itself (Contract Design Sections
  5-6). This reverses the prior revision's claim that the interface could
  remain fully unchanged.
- **Propagation, not re-derivation.** The resolved `ConversationId` is
  carried, as data, through `ParkerRuntime`, the Resolved Inbound Envelope
  (to the Assembler), and as an additive pass-through parameter through
  `ConversationReplyCoordinator`, `CommunicationConversationCoordinator`,
  and `ConversationTurnReasoningCoordinator`, to `ConversationEngine.submitTurn`
  (Contract Design Section 5). No component other than `ConversationEngine`
  ever computes or mints this value.
- **State is required, and its purposes are now distinguished.** Identity
  selection and active-Conversation tracking require new state
  `ConversationEngine` does not hold today; Turn retention requires state
  already held; termination, reopening, and replacement-after-expiry would
  require state a future capability may add, and are explicitly not
  authorised here (Contract Design Section 7).

## 2. Why the Prior Revision Was Wrong (Recorded, Not Repeated)

The prior Contract Design asserted a pure derivation *and* an unchanged
`ConversationEngine` interface *and* exactly one authoritative decision,
simultaneously. Architectural review's own reasoning is adopted here
without softening: those three claims are jointly unsatisfiable once a
second, independent evaluation of the same formula is required inside
`submitTurn` — that is two evaluations, not one decision, regardless of
whether they are guaranteed to agree by purity. The deeper defect, also
identified by review, is that pure derivation cannot support termination or
reopening at all, which this Unit's own frozen requirements anticipate as
legitimate future capability. This is recorded so a future reader does not
re-discover, and re-propose, the same rejected design.

## 3. Architectural Risks

- **A future reader could assume `resolveConversationId` and `submitTurn`
  may be called out of order, or independently, by different callers.**
  Contract Design Section 5's propagation path names `ParkerRuntime` as the
  only legitimate caller of `resolveConversationId`, and the existing
  coordinator chain as the only legitimate path to `submitTurn`. Named here
  so a future implementation review has this exact misuse to check
  against.
- **The pair-keyed continuity model still makes the idle/termination
  question (`19-conversation-engine.md` Section 13 Item 4) more pressing
  than before this Unit** — unchanged from the prior revision's own
  identical risk, now sharpened: without a termination rule,
  `resolveConversationId` will always find an "open" Conversation for any
  previously-seen pair, since nothing ever marks one closed.
- **Cross-channel conversation span (`19-conversation-engine.md` Section 13
  Item 5) remains implicitly foreclosed** by the continuity key's own
  `channelId` component — unchanged from the prior revision.

## 4. Compatibility Risks

- **`ConversationEngine.submitTurn`'s signature change is a breaking
  change to every existing test that calls it directly**
  (`tests/runtime/InMemoryConversationEngineTest.kt` and any coordinator
  test constructing a `ConversationEngine` fake) — larger in scope than the
  prior revision disclosed, since that revision claimed no interface change
  at all. Recorded honestly, not minimised: this is a real, disclosed
  increase in this design's own migration footprint relative to the
  rejected alternative.
- **`ConversationReplyCoordinator.submitAndDeliver`,
  `CommunicationConversationCoordinator.submitAndReason`, and
  `ConversationTurnReasoningCoordinator.submitTurnAndReason` each gain one
  additive parameter** — every existing test constructing or calling these
  three directly will need updating for the new parameter, in addition to
  the Assembler-facing tests the prior revision already flagged.

## 5. Migration Risks

- **`InMemoryConversationEngine` must stop being stateless** (its own KDoc
  today states plainly that it is "deliberately stateless") to hold the
  continuity-key-to-open-`ConversationId` mapping `resolveConversationId`
  requires. This was named in the prior revision for a different reason
  (supporting a derivation formula's own future adoption inside
  `submitTurn`); it is now named for the correct reason: `resolveConversationId`
  itself cannot function without it.
- **Concurrency discipline's *mechanism* is not designed here; the
  *guarantee* it must satisfy now is (Contract Design Section 5.1,
  Guarantee 1).** `InMemoryConversationEngine`'s own future implementation
  must serialise concurrent calls to `resolveConversationId` for the same
  continuity key so two near-simultaneous first messages from the same
  pair never each mint a distinct `ConversationId` — this is now a
  correctness requirement, not merely a desirable property. Only the exact
  mechanism (e.g. a single `Mutex` guarding all resolution and submission,
  versus a per-key locking scheme) remains an implementation choice.
- **`submitTurn`'s rejection behaviour for unknown/stale identifiers is not
  designed here (mechanism), but is required (Guarantee 3).** A future
  implementation must decide the exact exception type and message; it must
  not decide *whether* to reject — that is now fixed.

## 6. Dependency Risks

- **Restated:** "read-only" and "side-effect free" remain documented
  discipline for the Assembler, not type-enforced — unchanged.
- **The Resolved Inbound Envelope must never leak beyond `ParkerRuntime`
  and the Assembler** — unchanged from the prior revision; the plain
  `ConversationId` value, not the envelope, is what threads through the
  coordinator chain (Contract Design Section 8).

## 7. Implementation Choices Deferred to the Next Stage

- The exact internal representation of `ConversationEngine`'s new
  continuity-key state (map shape, concurrency mechanism).
- Termination, reopening, and replacement-after-expiry rules — explicitly
  out of scope, per Contract Design Section 7.
- The Kotlin name, package, and exact signature of `resolveConversationId`
  and the Resolved Inbound Envelope.
- Revision of every test file identified in Section 4 above for the new
  signatures.

## 8. Genuine Blockers

**None identified.** The blocking issue architectural review raised —
absence of a single authoritative decision propagated to
`ConversationEngine` — is resolved by this revision, not merely
downgraded to a risk. Every remaining item above is either a disclosed
consequence of the corrected design, a discipline risk for future review,
or an implementation choice correctly deferred to the next PES-001 stage.

## 9. Recommendation

Recommend architectural review of the revised
`CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` and
`CONVERSATION_CONTINUITY_SEQUENCE.md`, confirming specifically that the
one-authority, one-decision, propagated-not-re-derived structure in
Contract Design Sections 4-6 satisfies the blocking issue raised. If
accepted, the next Implementation Plan stage should budget for the wider
migration footprint disclosed in Section 4 above (four call-site
signature changes, not one) before implementation begins.
