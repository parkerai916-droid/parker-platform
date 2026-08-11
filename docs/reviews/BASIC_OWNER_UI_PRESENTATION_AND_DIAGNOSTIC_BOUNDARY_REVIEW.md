# Basic Owner UI Presentation and Diagnostic Boundary Review

Date: 2026-08-11 (Pacific/Auckland)

## Review subject

Proposed `BASIC_OWNER_UI_LIVE_VERIFICATION_BLOCKER_RESOLUTION_SCOPE_LOCK.md` at baseline `3f485fd070024b18203d408f6ac53d6daf3bd89b`.

## Verdict

**BOUNDARY CLEAR WITH QUALIFICATIONS**

The two proposed code corrections remain inside the UI presentation and composition-adapter boundary. They do not change Parker runtime or constitutional semantics. The qualifications below are binding safeguards, not requests for broader architecture.

## Authority review

### UI authority

Unchanged. The closed mode value selects copy only. It carries no runtime object, provider configuration, endpoint, health result, command, or capability. `OwnerUiController` continues to know only `OwnerInteraction`.

### Submission path

Unchanged:

```text
Compose -> OwnerUiController -> OwnerInteraction -> OwnerUiRuntimeAdapter
        -> injected ParkerRuntime::submitOwnerMessage
```

No alternate input path, direct runtime reference, or generic invocation capability is introduced.

### Reply authorship path

Unchanged:

```text
governed delivery -> OwnerNotificationSink -> OwnerUiNotificationBridge
                  -> OwnerReply -> typed Parker transcript -> Compose
```

Allowlisted System status mapping does not create Parker speech and must not intercept, sanitize, fabricate, or reinterpret a governed reply.

## Runtime and constitutional review

The proposal does not:

- alter `ParkerRuntime` startup, submission, shutdown, or outcome production;
- alter reasoning, prompting, parsing, permissions, execution, conversation semantics, Memory, Goals, Planner, Knowledge Submission, or tools;
- alter model/provider access or add any provider dependency to UI;
- alter `OwnerNotificationSink` or bypass it;
- alter `submitOwnerMessage` or its message/correlation construction;
- modify runtime logging or exception types;
- depend on Unit 3-C results or any remedy selection; or
- modify production reasoning.

`OwnerUiRuntimeAdapter` already owns translation from composition outcomes into presentation-safe dispositions. Replacing raw diagnostic propagation with fixed messages is therefore a correction of its existing responsibility, not a new runtime decision.

## Diagnostic boundary review

The current source violates its own `safeMessage` claim because arbitrary `cause.message` is forwarded. Raw provider output can appear in parser exceptions, and untyped rejection reasons can contain identities, channels, resource counts, or internal variant names. The frozen allowlist closes that leak without discarding the real cause from runtime observability: `ParkerRuntime.submitOwnerMessage` already logs the cause/rejection with correlation ID.

The UI does not currently receive a governed reference ID. The decision not to expose correlation IDs is correct and minimal. Fabricating or threading a new owner reference through `OwnerInteraction` would expand the contract and requires separate governance.

## Presentation truthfulness review

The two-mode model reports only facts established by the launch path:

- offline launcher: deterministic offline interaction;
- real Starting: startup attempt in progress;
- real Ready: Parker Runtime started and accepts the UI interaction;
- failure/unavailable: fixed negative state.

It deliberately makes no model-health, successful-inference, server, production, provider, or parity claim. This is truthful and sufficient.

## Windows-local endpoint boundary

The endpoint/model remains external to the UI and separately configured through existing runtime environment keys. Calling it a verification fixture prevents accidental production-model selection. Neither Ollama nor qwen is constitutionally required. The fixture must not be embedded in UI code, Gradle lifecycle, repository defaults, or merge criteria beyond the separately authorized live proof.

## Binding qualifications

1. Mode must remain a closed presentation type with no endpoint/provider/health fields.
2. Real-runtime copy may be shown only in the existing Ready branch.
3. Raw Failed causes and raw NotAccepted reasons must never enter `OwnerSubmissionDisposition` owner-visible fields.
4. Diagnostic causes/reasons must remain available through existing logs; implementation must not suppress or rewrite runtime logging.
5. No correlation/reference ID may be invented or shown in this correction.
6. Governed reply text must remain unchanged and exclusive to the Parker transcript path.
7. No retry claim may be shown because the current typed outcomes do not establish retry safety or likely success.
8. The local endpoint/model remains a separately authorized verification dependency, never a production selection or Reasoning Protocol result.

## Boundary conclusion

The proposed implementation surface is proportionate: five production files, all in `src/ui`, `ui-desktop`, or the existing UI adapter in `src/composition`; no runtime file outside that boundary. Subject to the qualifications above, implementation does not require new constitutional/runtime governance and may proceed only after a separate implementation authorization.

