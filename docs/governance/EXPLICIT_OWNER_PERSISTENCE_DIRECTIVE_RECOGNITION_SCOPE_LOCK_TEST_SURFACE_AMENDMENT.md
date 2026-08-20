# Explicit Owner Persistence Directive Recognition — Test Surface Amendment

**Status:** Adopted. Independent Constitutional Review accepted with no required correction.

## 1. Purpose

The adopted Explicit Owner Persistence Directive Recognition Scope Lock freezes
zero model-delegate calls for deterministic owner directives. Fresh full-suite
verification showed that one existing composition test file counts the former
model calls for explicit `Remember` setup turns. Those expectations must change
to test the newly governed zero-call behavior; no production change is required.

This amendment adds exactly one test file to the authorized test surface:

```text
MODIFY tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt
```

All original Scope Lock guarantees and boundaries remain unchanged.

## 2. Authorized changes

Only tests whose setup sends an explicit governed owner `Remember` directive may
be adjusted. Such setup turns must now be treated as deterministic and must not
consume a stub model response. A later ambiguous/query turn must still reach the
stub exactly once.

The test may therefore:

- replace obsolete `REMEMBER:` stub responses for deterministic setup turns with
  the response needed only by the later delegated query;
- change expected stub request counts to exclude deterministic setup turns;
- select the resulting query request at its new zero-based position; and
- retain all existing assertions about real admission, promoted memory,
  retrieval, prompt rendering, and QMD-control behavior.

## 3. Prohibitions

This amendment authorizes no production file, new behavior, weakened assertion,
direct persistence seeding, fake Knowledge Item, model invocation for a governed
directive, live model call, QMD change, or persistence change. It may not alter
unrelated conversation-continuity/history tests in the same file.

## 4. Acceptance

After the narrow expectation correction:

- each governed setup `Remember` reaches the real existing admission path;
- setup model-call count is zero;
- the later non-directive query reaches the model exactly once;
- the same real promoted propositions remain available to retrieval tests; and
- the ordinary full suite passes.

This amendment is effective only after accepted Independent Constitutional Review
and repository-consistent adoption.

```text
TEST SURFACE AMENDMENT — ADOPTED
```
