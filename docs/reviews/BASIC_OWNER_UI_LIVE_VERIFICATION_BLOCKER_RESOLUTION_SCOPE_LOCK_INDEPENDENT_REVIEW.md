# Basic Owner UI Live Verification Blocker Resolution Scope Lock Independent Review

Date: 2026-08-11 (Pacific/Auckland)

## Verdict

**ACCEPTED WITH QUALIFICATIONS**

The proposed Scope Lock correctly separates two code defects from one environment prerequisite and maintains the existing owner UI authority boundary. Acceptance is conditional on the binding qualifications listed below.

## Independent challenges

### 1. Is runtime/offline labelling genuinely truthful?

Yes, if implemented exactly as frozen. Offline says deterministic offline preview. Real Ready says only Parker Runtime. Starting makes no readiness claim. No wording claims model health, production/server parity, provider identity, or successful inference.

### 2. Can raw diagnostic text still leak?

Not through the scoped adapter mapping if both Failed stages use fixed text and adversarial tests cover raw provider output, endpoint/model/credential strings, filesystem paths, exception classes, and arbitrary messages. The implementation must avoid string interpolation of the cause anywhere in presentation code.

### 3. Can `NotAccepted.reason` still leak?

Not if the adapter replaces it unconditionally with the frozen fixed text. Conditional pass-through, regex filtering, truncation, or a “known reason” list is not authorized; the underlying value is not an owner-safe typed contract.

### 4. Is useful owner feedback removed excessively?

No. The owner retains typed categories: NotAccepted, Planned, Unavailable, Failed-Reasoning, and Failed-Unknown. Raw diagnostics are not useful owner guidance. No retry guidance is justified by current types. A future richer typed owner-safe rejection contract can be separately governed.

### 5. Is diagnostic detail silently discarded?

No, provided runtime logging remains unchanged. `ParkerRuntime` logs real causes/reasons with correlation IDs. The UI adapter must stop propagating detail to presentation but must not catch earlier, suppress logging, or mutate the runtime outcome.

### 6. Has UI authority expanded?

No. Presentation mode selects copy; safe mapping translates an existing outcome. Neither permits reasoning, authorization, execution, state access, or runtime control.

### 7. Has runtime semantics changed?

No. Runtime outcomes, stages, reasons, exceptions, submission, delivery, and lifecycle remain unchanged. Only downstream presentation is corrected.

### 8. Has direct provider access been introduced?

No. The endpoint remains reachable only through the existing `ParkerRuntime -> ModelReasoningProvider -> LocalHttpModelInferenceClient` path. UI must receive no endpoint/model field.

### 9. Is the Windows-local model becoming an accidental production dependency?

No, if it remains explicitly documented as a separately provisioned test fixture/verification dependency, has no repository default, is not wired into ordinary Gradle lifecycle, and is not merged as a provider selection.

### 10. Has qwen or Unit 3-C been smuggled back in?

No. The lock expressly rejects a qwen requirement and makes no claim based on Unit 3-C or remedy evidence. A smaller/different compatible model is sufficient for transport proof.

### 11. Is live verification still architecture proof rather than semantic qualification?

Yes. The frozen claim is only:

```text
owner UI -> ParkerRuntime.submitOwnerMessage -> existing governed runtime
         -> OwnerNotificationSink -> owner UI
```

It excludes semantic quality, production parity, conformance, remedy efficacy, and behaviour outside the single benign reply path.

### 12. Are implementation and verification authority separate?

Yes. This governance task authorizes neither. A later implementation task may correct A and B only. Endpoint provisioning and live execution require separate authorizations after offline gates pass.

## Binding qualifications

- Use the exact closed two-mode model and copy claims frozen by the Scope Lock.
- Show runtime mode only after the existing Ready result.
- Unconditionally replace raw Failed and NotAccepted text at the adapter boundary.
- Preserve Parker speech only through `OwnerNotificationSink`.
- Preserve runtime logging and correlation internally; show no correlation ID in this scope.
- Add the complete adversarial diagnostic tests before declaring the correction complete.
- Keep endpoint/model provisioning out of source, Gradle defaults, ordinary lifecycle, and this implementation unit.
- Require separate approval for implementation, endpoint provisioning, and live verification.

## Blocking defects

Until future implementation completes:

1. shared real-mode copy remains materially false; and
2. arbitrary failure/rejection diagnostic text can reach owner-visible System status.

Until later live verification:

3. no authorized Windows-local compatible endpoint/model fixture is available.

These must remain classified respectively as implementation blockers A/B and environment prerequisite C.

## Independent conclusion

The scope is constitutional, minimal, testable, and ready for a separately authorized implementation task. Acceptance does not authorize implementation, model installation, model selection, live verification, merge, or push.
