**Status:** Genuine Independent Constitutional Review of `docs/governance/AUTHORIZATION_PURPOSE_CARRIER_CONTRACT_DESIGN.md`, performed as if by another reviewer, against the governing documents and the actual, current repository content — not against the Contract Design's own Section 21 self-check alone. This document does not amend the Contract Design or any other frozen or draft governance document, Kotlin file, or test. Nothing is staged, committed, or pushed.

# Authorization Purpose Carrier Contract Design — Independent Constitutional Review

## 1. Baseline Confirmation

`git status --short` confirms the Contract Design and this review are the only new files at review time, alongside the already-known, uncommitted Conversational Memory Admission work. Confirmed no governance document, Kotlin file, or test was modified. The Contract Design's own Section 1 citation list was independently re-checked — every cited file exists at the path given, including `docs/adr/ADR-017-execution-request-is-canonical.md` and `docs/adr/ADR-018-immutable-execution-requests.md`, both independently re-read in full.

---

## 2. Challenge — Are ADR-017/018 Quoted and Distinguished Accurately?

Independently re-read both ADRs in full. Both are four-line documents; the Contract Design's own quotations ("Any proposed work... MUST become an `ExecutionRequest`"; "No subsystem may invent a parallel execution request type"; "ExecutionRequests become immutable after validation"; "Changes require creation of a new `ExecutionRequest` linked by correlation ID") are exact, not paraphrased. The Contract Design's own distinction between ADR-017 (governs competing request *types*) and ADR-018 (governs *instance* mutation, already satisfied by the `val`-only data class) is independently confirmed sound — the two ADRs genuinely address different questions, and conflating them (treating ADR-018 as if it forbade schema evolution) would have been a real error this document avoids. **Confirmed accurate.**

---

## 3. Challenge — Is the "No Genuine Nesting or Concurrency" Finding (Section 3) Actually Exhaustive?

Independently re-ran the grep the Contract Design reports (`coroutineScope`/`async {`/`launch {`) across all of `src/` — confirmed the only match is `InteractiveConsole.kt`'s own UI spinner, unrelated to permission evaluation. Independently re-read `InMemoryAgentRuntime.kt` directly at both cited call sites (line ~244, run-initiation `evaluate`; line ~520, step-level `executionPipeline.submit`) — confirmed these are sequential, not concurrent, and confirmed neither call occurs inside the other's own call stack. **Confirmed exhaustive for the current codebase**, correctly caveated as a finding about the *current* system, not a permanent guarantee.

---

## 4. Challenge — Does Section 17 ("Audit and Provenance") Accurately Describe the Existing Audit Mechanism? (Substantive Finding)

Section 17 states: "Inherited automatically from `ExecutionRequest`'s own existing `requestId`/`correlationId`/`createdAt`/`principalId` shape — no new mechanism is required for the value to reach wherever those fields already reach." This implies Authorization Purpose would automatically become audit-visible the same way those fields already are.

**Independently re-read `DefaultExecutionPipeline.kt`'s own `publishLifecycleEvent` directly** — the only event-publishing mechanism anywhere near the permission path (confirmed by grep: `DefaultPermissionEngine.kt` and `DefaultPermissionPolicy.kt` publish no events at all). Its own `ParkerEvent` construction: `publisherPrincipalId = request.principalId`, `correlationId = request.correlationId`, and **`payload = mapOf("requestId" to request.requestId.value)`** — the payload carries only the request's own identifier, nothing else. `proposedActions`, `targetResources`, `intent`, and every other content field on `ExecutionRequest` — not only a hypothetical `authorizationPurpose` — are **not** published to the event/audit trail by this mechanism today.

**Section 17's claim is inaccurate as written**: it implies Authorization Purpose would reach "wherever those fields already reach," but `principalId` and `correlationId` reach the event trail as distinguished, top-level `ParkerEvent` fields, while `createdAt` and every other *content* field (including where Authorization Purpose would live) do not reach it at all via this mechanism — audit visibility for the request's own content depends on the `ExecutionRequest` object itself being retained and inspected elsewhere, not on the `EventBus`. This is not a defect specific to Authorization Purpose — every other content field is in the identical position — but the Contract Design's own text overstates what "automatic" inheritance actually means here.

**Required correction:** Section 17 must state precisely that Authorization Purpose's own audit visibility would be identical to every other `ExecutionRequest` content field's — reachable by direct inspection of the retained request, not by the `EventBus`'s own routinely-published lifecycle events, which today carry only `requestId`/`principalId`/`correlationId`, never proposed-action or target-resource content. This does not change the document's own conclusion (Candidate A remains best-supported), but the claim as drafted is not accurate to the mechanism it cites.

---

## 5. Challenge — Is the "Same Trust Boundary as `principalId`" Analogy in Section 13 Complete? (Substantive Finding)

Section 13 states Authorization Purpose's own forgery resistance rests on "the same trust boundary every `ExecutionRequest` field already relies on," analogised directly to `principalId`. Pressed on whether this analogy is complete: `principalId` is backed by `IdentityService`'s own lifecycle (`Suspended`/`Revoked` statuses provide a *structural*, if indirect, backstop — a Principal that misbehaves can be suspended, and every subsequent request naming it is then denied regardless of what a caller declares). **Authorization Purpose, as this document currently specifies it, has no analogous structural backstop** — there is no proposed "suspend a Purpose value" mechanism, and none is implied by anything in Section 5–9's own candidate analysis. A caller that mis-declares its own Authorization Purpose faces no consequence beyond code review catching it before or after the fact — a strictly weaker enforcement position than `principalId`'s own.

This does not change the document's own conclusion (no candidate considered offers a stronger mechanism, and inventing one is a policy-content/vocabulary-governance question, not a carrier question), but the analogy as drafted reads as more complete than it is.

**Required correction:** Section 13 should disclose this asymmetry explicitly — Authorization Purpose's own integrity rests on code review alone, without even the indirect, lifecycle-based backstop `principalId` has via `IdentityService` — rather than presenting the two as straightforwardly equivalent.

---

## 6. Challenge — Nested Calls, Asynchronous Propagation: Does the Selected Model Actually Preserve the Stated Discipline?

Independently re-checked Section 16 ("Lifecycle") and Section 3's own sequential-chaining finding against Candidate A specifically: since Authorization Purpose lives on `ExecutionRequest` itself, and `ExecutionRequest` is never reused or mutated across separate, causally-related requests (confirmed by ADR-018 and by direct code inspection of `InMemoryAgentRuntime`'s own two separate `ExecutionRequest` constructions), there is no code path by which a later, separately-constructed request could silently inherit an earlier one's own Authorization Purpose value — each must be freshly, explicitly populated by its own constructing caller. **Confirmed sound**, and correctly not dependent on any enforcement mechanism beyond the one `ExecutionRequest`'s own existing immutability already provides.

---

## 7. Challenge — Fail-Closed Behaviour, Caller-Specific Exceptions

Independently re-checked Section 4 and Section 6's own "future Tools" row against `PermissionPolicy.md` §4/§7 (re-read directly): "DENIED is the default when no rule matches." The Contract Design does not specify rule content (correctly deferred), but its own stated requirement — that an absent or unregistered value must deny by the existing default, never a new failure mode — is consistent with this text. No caller-specific carrier mechanism is proposed anywhere in Candidate A; the field is uniformly available. **Confirmed sound.**

---

## 8. Findings

**Two required corrections**, both precision/honesty fixes to already-written sections, neither changing the document's own conclusion:

1. **Section 17** overstates audit inheritance — `DefaultExecutionPipeline`'s own `publishLifecycleEvent`, the only event-publishing mechanism near the permission path, carries only `requestId`/`principalId`/`correlationId`, never request content (confirmed by direct code inspection). Authorization Purpose's own audit visibility would be identical to every other content field's — reachable by inspecting the retained request, not automatically published.
2. **Section 13**'s own "same trust boundary as `principalId`" analogy omits that `principalId` has an indirect, lifecycle-based enforcement backstop (`IdentityService` suspension/revocation) that Authorization Purpose, as specified, does not.

No other required correction was found. The ADR-017/018 quotation and distinction, the exhaustiveness of the nested/concurrent-call finding, the sequential-propagation discipline under Candidate A, and fail-closed/no-caller-specific-exception preservation were each independently re-derived from primary sources.

---

## Constitutional Verdict

```
REQUIRES REVISION
```

Two narrow, required corrections (Sections 4 and 5, above): state Section 17's own audit claim precisely against the actual `publishLifecycleEvent` mechanism, and disclose the enforcement asymmetry between `principalId` and Authorization Purpose in Section 13. Proceeding to a Defect Confirmation Review after both corrections are applied.

**Post-correction status:** both required corrections were applied to `docs/governance/AUTHORIZATION_PURPOSE_CARRIER_CONTRACT_DESIGN.md` Sections 13 and 17. See `docs/reviews/AUTHORIZATION_PURPOSE_CARRIER_CONTRACT_DESIGN_DEFECT_CONFIRMATION_REVIEW.md` for the narrow Defect Confirmation Review, which found both corrections complete and no further defect. The Contract Design is accepted as of that review.
