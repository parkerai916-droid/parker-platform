# ResponseComposer — Scope Lock (Sprint 10, Unit 1)

## Status

**Stage 5 Scope Lock, PES-001. Locked for Unit 1 only.** This document
freezes `docs/implementation/REPLY_TO_OUTBOUND_RESPONSE_IMPLEMENTATION_PLAN.md`
("the Plan," as corrected — see its own Fourth revision note, retitled
"Sprint 10, Unit 1") exactly as reviewed and accepted. It does not
redesign, add to, or narrow anything the Plan already decided — it
restates the Plan's own decisions as frozen scope, per PES-001's own
Stage 3/Stage 5 relationship ("Implementation Decisions clarify approved
architecture. They do not replace or redefine it," applied here one level
up: a Scope Lock clarifies which of a Plan's own already-approved
decisions are now frozen; it does not relitigate them).

**Unit 2 (`ReplyDeliveryCoordinator`,
`docs/implementation/REPLY_DELIVERY_COORDINATOR_IMPLEMENTATION_PLAN.md`)
is explicitly, entirely out of scope of this Scope Lock.** See
Section 13.

No Kotlin is written by this document. No test is created by this
document. `IMPLEMENTATION_GAPS.md` and `IMPLEMENTATION_HISTORY.md` are
not modified by this document. No commit or push is performed by this
document.

---

## 1. Governing Documents

In order of authority, per this program's own established hierarchy
(Constitution > PES-001 > Accepted ADRs > Contract Design > Implementation
Plans > Sprint Handovers):

1. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001) — Stage 3
   (Implementation Plan) and Stage 5 (Scope Lock) definitions, and the
   Human-primary-authority model governing both.
2. `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md` — Section 3
   (`OutboundParkerResponse.senderPrincipalId` must be "a real, resolved
   `PrincipalId`, threaded through explicitly, never a hardcoded
   constant").
3. `docs/architecture/RESPONSE_DELIVERY_CONTRACT_DESIGN.md` — Section 8 /
   Section 11 Deferred Item 3 (construction of `OutboundParkerResponse`
   from a `Reply` is "a future, separately-scoped coordinator's job").
4. `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md` — Section 3
   (`Reply.text` "deliberately shaped to be directly usable as a future
   `OutboundParkerResponse.text`").
5. `docs/architecture/IMPLEMENTATION_GAPS.md` #53 (item 1, the gap this
   Unit narrows) and #49 (the identity-constant defect class this Unit's
   design must not, and does not, reintroduce).
6. `docs/implementation/SPRINT_9_HANDOVER.md` Section 3 — the Sprint 10
   candidate identification this Unit implements, and the source of the
   Plan's own corrected title (its Fourth revision note).
7. `docs/implementation/COMMUNICATION_TO_CONVERSATION_WIRING_IMPLEMENTATION_PLAN.md`
   — the direct structural precedent for proceeding straight to Stage 3
   without a Stage 2A pass, and for reusing `GatedOutcome<T>`.
8. `RECONCILIATION_ADDENDUM_RESPONSE_COMPOSER_VS_REPLY_DELIVERY_COORDINATOR.md`
   — Option C, the governing decision confirming `ResponseComposer` (not
   the withdrawn `ReplyDeliveryCoordinator` design) as canonical for gap
   #53 item 1's construction half.
9. **`docs/implementation/REPLY_TO_OUTBOUND_RESPONSE_IMPLEMENTATION_PLAN.md`
   itself, as corrected (Third and Fourth revision notes) and accepted —
   the direct source this Scope Lock freezes**, Sections 1 through 18.

---

## 2. Exact Included Work

Restated from the Plan, Section 2, unchanged:

- One new, small, standalone, non-interface-backed component,
  `ResponseComposer`, with exactly one public method, `compose`,
  performing exactly the branch behaviour in Section 6 below.
- Unit tests for the composer, per Section 10 below, including a
  compatibility test proving its output is accepted by the real
  `ResponseDelivery` unchanged, performed by the test, not by the
  composer.

## 3. Exact Excluded Work

Restated from the Plan, Section 9, unchanged:

- A model-backed `ReasoningProvider` implementation.
- Planner Runtime / `Goal` routing.
- Agents.
- Invoking `ResponseDelivery.deliver` from within `ResponseComposer`.
- A production composition root (registration of
  `system.response-composer` happens only inside this Unit's own tests).
- Modifying `CommunicationConversationCoordinator`,
  `ConversationTurnReasoningCoordinator`, `ConversationEngine`,
  `ReasoningProvider`, `ResponseDelivery`, or `GatedOutcome`.
- A full owner-message-to-delivery orchestrator wrapping
  `CommunicationConversationCoordinator`, `ResponseComposer`, and
  `ResponseDelivery` together — this is Unit 2's own job
  (`ReplyDeliveryCoordinator`), explicitly out of scope of this Scope
  Lock (Section 13).

## 4. Exact Production Files to Add

**Frozen. No other production file may be added or modified under this
Scope Lock.**

| File | Status |
| --- | --- |
| `src/runtime/ResponseComposer.kt` | New |

No existing `src/` file is modified. No new file is added under
`src/interfaces/` — this Unit introduces no public contract type (Plan,
Required Analysis Question 2).

## 5. Exact Test Files to Add

**Frozen. No other test file may be added or modified under this Scope
Lock.**

| File | Status |
| --- | --- |
| `tests/runtime/ResponseComposerTest.kt` | New |
| `tests/runtime/FakeIdentityService.kt` | New — lambda-based, call-counting `IdentityService` fake (Plan, Section 3, added by the Plan's own Third revision note), mirroring `FakeExecutionPipeline`/`FakeResourceRegistry`'s established precedent. Only `resolve` is exercised by `ResponseComposer`; `register`/`updateStatus`/`touch`/`listByOwner` throw if reached. |

No existing `tests/` file is modified.

## 6. Approved Class and Method Signature

Frozen exactly as the Plan's Section 10 proposes:

```kotlin
package parker.core.runtime

import parker.core.interfaces.IdentityService
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.OutboundParkerResponse
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ReasoningProviderResponse
import java.time.Instant

class ResponseComposer(
    private val identityService: IdentityService,
) {

    private companion object {
        val RESPONSE_COMPOSER_PRINCIPAL_ID = PrincipalId("system.response-composer")
    }

    suspend fun compose(
        originalMessage: InboundOwnerMessage,
        reasoningResponse: ReasoningProviderResponse,
    ): GatedOutcome<OutboundParkerResponse> {
        return when (reasoningResponse) {
            is ReasoningProviderResponse.Reply -> {
                val composerIdentity = identityService.resolve(RESPONSE_COMPOSER_PRINCIPAL_ID)
                    ?: throw IllegalStateException(
                        "Response Composer operating Principal " +
                            "'${RESPONSE_COMPOSER_PRINCIPAL_ID.value}' is not registered with IdentityService",
                    )

                GatedOutcome.Produced(
                    OutboundParkerResponse(
                        channelId = originalMessage.channelId,
                        senderPrincipalId = composerIdentity.principalId,
                        text = reasoningResponse.text,
                        timestamp = Instant.now(),
                        correlationId = originalMessage.correlationId,
                    ),
                )
            }
            is ReasoningProviderResponse.Goal -> GatedOutcome.NotAccepted(
                "not a Reply; reasoningResponse was Goal",
            )
            ReasoningProviderResponse.NoAction -> GatedOutcome.NotAccepted(
                "not a Reply; reasoningResponse was NoAction",
            )
        }
    }
}
```

**Frozen elements of this signature specifically:** the class name
(`ResponseComposer`); its single constructor parameter
(`identityService: IdentityService`); the private, companion-scoped
`RESPONSE_COMPOSER_PRINCIPAL_ID` constant and its literal value
(`"system.response-composer"`); the `compose` method's name, parameter
list (`originalMessage: InboundOwnerMessage`, `reasoningResponse:
ReasoningProviderResponse`), `suspend` modifier, and return type
(`GatedOutcome<OutboundParkerResponse>`); and the placement of the
`identityService.resolve` call strictly inside the `Reply` branch.
Any deviation from this signature discovered during implementation is a
Scope Lock violation requiring a new planning pass, not a silent
adjustment (Plan, Section 18).

## 7. Approved Identity-Resolution Behaviour

Frozen exactly as the Plan's Section 4c, Section 5 Decision 3, and Third
revision note establish:

- `IdentityService.resolve(RESPONSE_COMPOSER_PRINCIPAL_ID)` is called
  **only inside the `Reply` branch**, exactly once, and only at the
  moment an `OutboundParkerResponse` is actually about to be constructed.
- `Goal` and `NoAction` **never** call `IdentityService.resolve` —
  `compose`'s own code on those two branches contains no call capable of
  resolving, or failing to resolve, any identity.
- The selected identity (`system.response-composer`) and the
  resolve-before-use rule itself are unchanged from the architecture's
  own `senderPrincipalId` requirement (`COMMUNICATION_CONTRACT_DESIGN.md`
  Section 3) — only the scope of *when* resolution occurs was corrected
  pre-Scope-Lock.
- No `IdentityService` method other than `resolve` is ever called by
  `ResponseComposer`.

## 8. Approved Branch Behaviour

Frozen exactly as the Plan's Section 1 and Section 10 establish:

| `reasoningResponse` | Identity resolved? | Result |
| --- | --- | --- |
| `Reply(text)` | Yes, exactly once | `GatedOutcome.Produced(OutboundParkerResponse(...))` |
| `Goal(text)` | No | `GatedOutcome.NotAccepted("not a Reply; reasoningResponse was Goal")` |
| `NoAction` | No | `GatedOutcome.NotAccepted("not a Reply; reasoningResponse was NoAction")` |

No other branch, variant, or fallback exists. Nothing routes a `Goal`
onward to Planner Runtime. Nothing is delivered on the `Reply` branch —
`compose` returns a value; it performs no side effect.

## 9. Approved Field Mapping

Frozen exactly as the Plan's Section 11 establishes — applies only on the
`Reply` branch; no `OutboundParkerResponse` is constructed on `Goal` or
`NoAction`:

| `OutboundParkerResponse` field | Source | Derivation |
| --- | --- | --- |
| `channelId` | `originalMessage.channelId` | Copied unchanged. |
| `senderPrincipalId` | `identityService.resolve(RESPONSE_COMPOSER_PRINCIPAL_ID)!!.principalId` | The resolved operating Principal's own `principalId`, resolved inside this same branch, immediately before this field is populated — never the constant directly, never `originalMessage.senderPrincipalId`. |
| `text` | `reasoningResponse.text` | Copied unchanged, read exactly once — no trim, case change, truncation, or reformatting. |
| `timestamp` | `Instant.now()` | Set at the moment of construction. |
| `correlationId` | `originalMessage.correlationId` | Copied unchanged. |
| `metadata` | *(not set)* | Left at its default, `emptyMap()`. |

`reply.text`, `originalMessage.channelId`, and
`originalMessage.correlationId` are never re-derived, defaulted, or
mutated (Plan, Section 4b).

## 10. Approved Exception Behaviour

Frozen exactly as the Plan's Section 4c establishes:

- **The only exception `compose` may throw:** `IllegalStateException`,
  and only when `reasoningResponse` is `Reply` and
  `identityService.resolve(RESPONSE_COMPOSER_PRINCIPAL_ID)` returns
  `null` — an unregistered operating Principal, a deployment/setup
  defect, mirroring `InMemoryConversationEngine.submitTurn`'s identical
  pattern.
- **`Goal` and `NoAction` can never throw this exception, or any
  exception, regardless of whether `system.response-composer` is
  registered.** This is a direct, structural consequence of resolution
  happening only inside the `Reply` branch (Section 7, above).
- No `try`/`catch` exists anywhere in `ResponseComposer`.
- `GatedOutcome.NotAccepted` is reserved exclusively for the `Goal`/
  `NoAction` structural outcome — never used to catch and repackage a
  thrown exception.

## 11. Approved Test Strategy

Frozen exactly as the Plan's Section 7 establishes, for
`tests/runtime/ResponseComposerTest.kt`, using `FakeIdentityService` for
all isolated tests and `InMemoryIdentityService` only for the
compatibility test:

1. Reply path — `Produced` with correct `text`/`channelId`/
   `correlationId`/`senderPrincipalId`; `resolveCallCount == 1`.
2. Goal path — `NotAccepted`; `resolveCallCount == 0`.
3. NoAction path — `NotAccepted`; `resolveCallCount == 0`.
4. Goal/NoAction never throw when unregistered — `NotAccepted` returned
   normally, `resolveCallCount == 0`, no exception.
5. Field pass-through / non-mutation.
6. Unregistered operating identity — Reply branch only — throws
   `IllegalStateException`.
7. Statelessness — reflective test, exactly one declared field
   (`identityService`).
8. Structural composition-only test — constructor accepts only
   `IdentityService`.
9. Exactly-once identity resolution, corrected per branch — `1` for
   `Produced`, `0` for `NotAccepted`.
10. Compatibility test — real `InMemoryResourceRegistry`,
    `InMemoryToolRegistry`, `InMemoryModuleRegistry`,
    `InMemoryToolInvocationBinding`, `InMemoryActionVocabulary`/
    `ActionMapper`, `InMemoryEventBus`, `FakePermissionEngine`,
    `DefaultExecutionPipeline`, real `LocalTextChannelDeliverTool`, real
    `ResponseDelivery`, real `InMemoryIdentityService` — the test itself
    calls `compose` then separately calls `responseDelivery.deliver`
    directly, asserting `SUCCESS` and correct callback text.

Full Gradle suite run once implementation is complete; a static,
honestly-disclosed projected count if the sandbox cannot resolve the
Kotlin Gradle plugin, pending Steven's Android Studio verification
(Section 14, below).

## 12. Acceptance Criteria

Frozen exactly as the Plan's Section 8 establishes:

- `ResponseComposer` exists, is independently constructible with only an
  `IdentityService`, and its own tests pass.
- A `Reply` is composed into exactly one correct `OutboundParkerResponse`,
  field-for-field traceable per Section 9, above.
- A `Goal` or `NoAction` never produces an `OutboundParkerResponse`.
- The composer never reasons, plans, authorises, or delivers — verified
  structurally, by the absence of any dependency slot for
  `ReasoningProvider`, `PlannerRuntime`, `PermissionEngine`, or
  `ResponseDelivery`.
- The composer is stateless, and never mutates or reinterprets its
  inputs — both verified structurally.
- The composer's only possible thrown exception is the disclosed
  operating-identity precondition, reachable only from the `Reply`
  branch — verified by test.
- A separate compatibility test, external to the composer itself, proves
  its output is accepted by the real `ResponseDelivery`/
  `LocalTextChannelDeliverTool` stack unchanged.
- No production (`src/`) code added by this Unit references
  `PlannerRuntime`, `PlanningRequest`, `ExecutionPipeline`,
  `ResponseDelivery`, `PermissionEngine`, `ToolRegistry`, `MemoryStore`,
  `WorldModel`, or `ReasoningProvider`, anywhere.
- No existing `src/` or `tests/` file is modified.
- All tests listed in Section 11, above, pass, and the full Gradle suite
  passes (or a projected count is honestly reported).

## 13. Explicit Confirmation: Unit 2 Remains Out of Scope

**This Scope Lock covers `ResponseComposer` (Unit 1) only.**
`ReplyDeliveryCoordinator` (Unit 2,
`docs/implementation/REPLY_DELIVERY_COORDINATOR_IMPLEMENTATION_PLAN.md`)
is not locked by this document, in any respect:

- No file listed in that Plan's own Section 3
  (`src/runtime/ReplyDeliveryCoordinator.kt`,
  `tests/runtime/ReplyDeliveryCoordinatorTest.kt`) is authorised for
  creation by this Scope Lock.
- No signature, invariant, decision, or test from that Plan is frozen by
  this document.
- That Plan's own Section 14 (Implementation Sequence) precondition —
  "the companion Unit (`ResponseComposer`) is Scope Locked, implemented,
  and its own tests pass first" — is only partially satisfied by this
  document: `ResponseComposer` is now Scope Locked, but not yet
  implemented or tested. Unit 2 implementation must not begin until it
  is both, and until a separate, explicit Scope Lock instruction is given
  for Unit 2 specifically (that Plan, Section 16).
- This Scope Lock does not constitute, and must not be read as, any form
  of implicit authorisation, pre-approval, or head start for Unit 2.

## 14. Verification Requirements

Restated from the Plan's Section 12 Verification Procedure, unchanged,
scoped to Unit 1 only:

1. Steven opens the `parker-platform` project in Android Studio.
2. Runs the full test suite (`./gradlew test` or equivalent) — not only
   `ResponseComposerTest.kt` — confirming no existing test regresses.
3. Confirms each of the ten tests in Section 11, above, passes
   individually.
4. Records the real pass/fail count and compares it against the static
   projection reported at implementation time; any discrepancy is itself
   a finding to report.
5. Only after a real, verified pass does Section 15 (Documentation
   Follow-up), below, become eligible to proceed.

## 15. Documentation Follow-up

Restated from the Plan's Section 15, unchanged, and explicitly deferred —
not performed by this document:

- `IMPLEMENTATION_HISTORY.md` **may** be updated after verified
  implementation, recording this Unit exactly as delivered (files added,
  tests added, a real Gradle result) — not before.
- `IMPLEMENTATION_GAPS.md` #53 **may be clarified further, not closed**
  after verified implementation — this Unit implements construction only;
  the wiring that would actually call `ResponseDelivery` remains open
  until Unit 2 is separately Scope Locked, implemented, and verified.
- No architecture or Contract Design document is modified at any point
  during this Unit's implementation.
- Neither `IMPLEMENTATION_GAPS.md` nor `IMPLEMENTATION_HISTORY.md` is
  touched by this Scope Lock document itself.

## 16. Stop Conditions

This Scope Lock does not authorise:

- Writing any Kotlin file not listed in Sections 4 and 5, above.
- Modifying any existing `src/` or `tests/` file.
- Beginning Unit 2 (`ReplyDeliveryCoordinator`) in any respect
  (Section 13).
- Updating `IMPLEMENTATION_GAPS.md` or `IMPLEMENTATION_HISTORY.md`.
- Any commit or push.
- Any deviation from the signature (Section 6), identity-resolution
  behaviour (Section 7), branch behaviour (Section 8), field mapping
  (Section 9), or exception behaviour (Section 10) frozen above, without
  a new planning pass.

If implementation reveals that any frozen element above cannot be
satisfied as written (for example, a Kotlin compiler constraint this
sandbox's own inability to run Gradle did not surface), work stops and
returns to Steven for a new decision — it is not silently resolved during
implementation.

---

## Conclusion

This Scope Lock freezes `ResponseComposer` (Sprint 10, Unit 1) exactly as
reviewed and accepted in
`docs/implementation/REPLY_TO_OUTBOUND_RESPONSE_IMPLEMENTATION_PLAN.md`,
as corrected by that Plan's own Third revision note (identity resolution
scoped to the `Reply` branch) and Fourth revision note (title corrected
from "Sprint 9" to "Sprint 10, Unit 1"). One production file
(`src/runtime/ResponseComposer.kt`) and two test files
(`tests/runtime/ResponseComposerTest.kt`,
`tests/runtime/FakeIdentityService.kt`) are authorised, all additions, no
existing file modified. `ReplyDeliveryCoordinator` (Unit 2) remains
entirely unlocked and must not begin. This document does not implement
anything, does not create any test, does not modify
`IMPLEMENTATION_GAPS.md` or `IMPLEMENTATION_HISTORY.md`, and does not
commit or push. It awaits Steven's review.

## Related

- `docs/implementation/REPLY_TO_OUTBOUND_RESPONSE_IMPLEMENTATION_PLAN.md` (the Plan this document locks)
- `docs/implementation/REPLY_DELIVERY_COORDINATOR_IMPLEMENTATION_PLAN.md` (Unit 2, explicitly not locked — Section 13)
- `docs/implementation/SPRINT_9_HANDOVER.md` (Section 3)
- `RECONCILIATION_ADDENDUM_RESPONSE_COMPOSER_VS_REPLY_DELIVERY_COORDINATOR.md`
- `docs/architecture/PARKER_ENGINEERING_STANDARD.md`
- `docs/architecture/IMPLEMENTATION_GAPS.md` (#49, #53)
