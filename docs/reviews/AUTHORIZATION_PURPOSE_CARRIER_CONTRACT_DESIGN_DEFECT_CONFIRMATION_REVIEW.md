**Status:** Narrow Defect Confirmation Review, following the Independent Constitutional Review's `REQUIRES REVISION` verdict on `docs/governance/AUTHORIZATION_PURPOSE_CARRIER_CONTRACT_DESIGN.md`. No Kotlin, test, or frozen/draft governance document is touched. Nothing is staged, committed, or pushed.

# Authorization Purpose Carrier Contract Design — Defect Confirmation Review

## The Two Required Corrections

The Independent Constitutional Review (`docs/reviews/AUTHORIZATION_PURPOSE_CARRIER_CONTRACT_DESIGN_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`, Sections 4 and 5) found:

1. Section 17 overstated audit inheritance, implying Authorization Purpose would automatically reach the `EventBus`'s own published events, when `DefaultExecutionPipeline.publishLifecycleEvent`'s own payload carries only `requestId` — no request content field reaches the event trail today.
2. Section 13's own "same trust boundary as `principalId`" analogy omitted that `principalId` has an indirect, lifecycle-based enforcement backstop (`IdentityService` suspension/revocation) that Authorization Purpose, as specified, does not.

## Corrections Applied

- **Section 17** — rewritten to state precisely, with direct citation to `DefaultExecutionPipeline.publishLifecycleEvent`'s own `payload = mapOf("requestId" to request.requestId.value)`, that no request content field reaches the `EventBus` today, and that Authorization Purpose's own audit visibility would be identical to every other content field's: reachable by inspecting the retained request, not automatically event-published.
- **Section 13** — a new paragraph added disclosing the enforcement asymmetry explicitly: `principalId` has `IdentityService`'s own structural suspension/revocation backstop; Authorization Purpose, as this document specifies it, has none — its own integrity rests on code review alone.

No other section was changed. Both corrections are precision/honesty fixes — neither alters Section 7's own selected model, Section 6's own comparative analysis, or any other section's own conclusion, each of which the ICR independently confirmed already sound.

## Re-Verification

- **Scope check:** the only edited regions are Section 17 and Section 13, both traced directly above.
- **Consistency check:** Section 17's own corrected text cites the exact code path the ICR itself verified; Section 13's own new paragraph is consistent with Section 3's own already-stated `IdentityService` short-circuit mechanism, not a new claim introduced without grounding.
- **No regression:** every other Independent Constitutional Review finding (Sections 2, 3, 6, 7 of that review) required no correction and remains valid against the now-corrected document.

## Outcome

```
DEFECT CONFIRMATION REVIEW COMPLETE — CORRECTIONS APPLIED, NO FURTHER DEFECT FOUND
```

The Contract Design, as corrected, stands as this governance cycle's own final artifact. Its own Section 7 conclusion is unaffected and remains in force: Candidate A (extend `ExecutionRequest` with a new, optional, closed-value-typed field) is the selected carrier model, with the exact field shape, vocabulary governance, and `PermissionDecision` extension question all deliberately left open for later governance tiers.
