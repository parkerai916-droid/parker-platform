# Explicit Owner Persistence Directive Recognition — Scope Lock

**Status:** Adopted. Independent Constitutional Review accepted with no required correction.

## 1. Purpose and authority

This Scope Lock authorizes one bounded correction to Parker's conversational
intent-selection pipeline. When the configured owner issues an explicit,
unambiguous, deterministically recognizable persistence directive, Parker—not a
probabilistic model—establishes the existing governed intent
`ReasoningProviderResponse.Remember` before model prompt construction, inference,
or response classification.

The demonstrated defect is that a model may understand “Remember X” yet emit a
`Reply`, preventing the already-governed conversational memory admission path
from running. This instrument changes only who decides this narrow explicit
intent. It creates no persistence authority and no second persistence path.

This instrument is read with the adopted Programme 3 Explicit Owner Instruction
Promotion Exception Scope Lock Clarification, the Conversational Memory
Admission Implementation Plan and its accepted reviews, the Reasoning Provider
contract and model-provider plan, the Communication and Conversation contracts,
and Parker's constitutional permission, provenance, epistemic-integrity, and
durability rules. Those authorities remain unchanged.

## 2. Owner provenance boundary

The deterministic path may run only when:

```text
turn.message.senderPrincipalId == configured owner PrincipalId
```

For Version 1, the existing trusted local owner adapters—the interactive console
and owner-facing local composition—are the bounded provenance surface. They
construct `InboundOwnerMessage` with the configured owner principal. This is not
a claim of general cryptographic, remote-session, or channel authentication.

A non-matching principal must delegate unchanged and exactly once to the existing
reasoning provider. Identity equality establishes eligibility for recognition,
not permission to write. All downstream admission and permission decisions remain
authoritative.

## 3. Frozen Version 1 positive grammar

Matching is deterministic, case-insensitive for governed wrapper words, and
requires a nonblank proposition `X`. Only these forms qualify:

```text
Remember X
Remember that X
Please remember X
Please remember that X

Store in memory: X
Please store in memory: X

Save to memory: X
Please save to memory: X

Commit to memory: X
Please commit to memory: X

Commit X to memory
Please commit X to memory

Keep in memory: X
Please keep in memory: X

Keep this in memory: X
Please keep this in memory: X

Keep X in memory
Please keep X in memory
```

Every match produces only:

```text
ReasoningProviderResponse.Remember(proposition)
```

There are no `STORE`, `SAVE`, `COMMIT`, or `KEEP` intents or persistence
mechanisms. Unanchored `Save X`, `Store X`, `Commit X`, and `Keep X` are not in
the grammar.

## 4. Mandatory exclusions and precedence

Negation is evaluated before positive recognition. The following families must
not qualify, including equivalent governed capitalization and surrounding outer
whitespace:

```text
Don't remember X
Do not remember X
Never remember X
Don't store X
Do not save X
Don't commit X to memory
Don't keep X in memory
```

Questions and retrieval language do not qualify:

```text
Do you remember X?
Can you remember X?
Could you remember X?
What do you remember about X?
What did you save about X?
```

Statements do not qualify:

```text
I remember X.
I saved X yesterday.
The application stores X.
We should keep X in memory.
```

Ambiguous or non-memory commands do not qualify:

```text
Remember when X happened?
Save the file.
Store the file.
Commit the code.
Keep going.
Save me from X.
```

Any uncertainty, unmatched syntax, blank proposition, question-shaped input, or
ambiguous construction delegates unchanged and exactly once to normal reasoning.
The classifier must not infer importance, truth, confidence, or evidential weight.

## 5. Proposition extraction

Version 1 extraction is mechanical. It may remove only:

- governed directive wrapper words;
- optional governed `please`;
- optional governed `that` in the `remember` forms;
- the governed colon delimiter;
- governed `to memory` or `in memory` wrapper text;
- outer whitespace; and
- at most one terminal period.

Internal wording, case, and punctuation are otherwise preserved. A blank result
is a no-match. No model, provider, prompt, summarizer, or semantic rewriter may
extract or rewrite the proposition.

## 6. Frozen insertion shape

Production composition is:

```text
LoggingReasoningProvider(
    ExplicitOwnerPersistenceDirectiveReasoningProvider(
        configuredOwnerPrincipalId,
        ExplicitOwnerPersistenceDirectiveClassifier,
        ModelReasoningProvider(...)
    )
)
```

The decorator accepts a `ReasoningProviderRequest`. Only
`ReasoningSubject.OfTurn` can qualify. For an eligible owner and classifier match,
it returns the existing `ReasoningProviderResponse.Remember` without invoking the
delegate. Every other case invokes the existing delegate exactly once and returns
its result unchanged.

## 7. Reused admission path

The resulting `Remember` follows the existing branch unchanged:

```text
ConversationReplyCoordinator
→ MemoryAdmissionCoordinator
→ PermissionEngine
→ Memory Core provenance/assertion
→ KnowledgeSubmission and its own permission gate
→ Knowledge Candidate evaluation
→ Knowledge Item persistence and durability
```

Recognition establishes intent only. It neither writes nor authorizes writing.
Denial or decline remains authoritative and must not be converted into success.

## 8. Authorized implementation surface

Authorized production files:

```text
NEW src/runtime/ExplicitOwnerPersistenceDirectiveClassifier.kt
NEW src/runtime/ExplicitOwnerPersistenceDirectiveReasoningProvider.kt
MODIFY src/composition/ParkerRuntime.kt
```

Authorized tests:

```text
NEW tests/runtime/ExplicitOwnerPersistenceDirectiveClassifierTest.kt
NEW tests/runtime/ExplicitOwnerPersistenceDirectiveReasoningProviderTest.kt
MODIFY tests/composition/ParkerRuntimeConversationalMemoryAdmissionCompositionTest.kt
```

If this surface is insufficient, implementation must stop for further authority.

## 9. Hard boundaries

This unit must not modify or bypass `ConversationReplyCoordinator`,
`MemoryAdmissionCoordinator`, `ReasoningResponseParser`, prompt construction,
model/provider clients or configuration, Memory Core, Knowledge Submission,
candidate evaluation, durability, QMD, UI, Docker, or runtime persistence
architecture. The classifier and decorator may not reference persistence,
Memory Core, knowledge, durability, QMD, HTTP, Ollama, Docker, or provider-client
types. The decorator may depend only on the generic `ReasoningProvider` seam and
the contracts necessary to inspect a turn and return `Remember`.

No prompt workaround, Qwen-specific condition, direct durability write, direct
Memory Core write, or parallel admission mechanism is authorized.

## 10. Acceptance and failure semantics

The primary fixture:

```text
Remember the test lighthouse is painted orange.
```

must deterministically yield:

```text
ReasoningProviderResponse.Remember("the test lighthouse is painted orange")
```

and invoke the model delegate zero times. Non-owner, negated, question,
statement, ambiguous, non-memory, unsupported, and blank-proposition inputs must
delegate exactly once. Exceptions from the classifier or delegate must not be
translated into persistence or false success.

## 11. Review and effectiveness

This Scope Lock becomes effective only after an accepted Independent
Constitutional Review and repository-consistent adoption. Implementation may
then proceed only within Section 8 and must receive fresh Completion Review and
any required Independent Constitutional Review before merge eligibility.

No live model, Parker runtime, Docker, QMD, or evidence execution is authorized
by this instrument.

```text
EXPLICIT OWNER PERSISTENCE DIRECTIVE RECOGNITION — ADOPTED
```
