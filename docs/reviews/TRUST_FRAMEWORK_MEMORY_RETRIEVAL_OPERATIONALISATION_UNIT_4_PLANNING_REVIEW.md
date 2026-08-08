**Status:** Unit 4 Planning Review — PASS. Conducted against baseline `03f0223`, the accepted Operationalisation Scope Lock and Implementation Plan, their accepted constitutional reviews, accepted Units 1–3, and the live production graph before Unit 4 Kotlin or test changes. No Boundary Review is required. Nothing is staged, committed, or pushed.

# Gap #54 Memory Retrieval Operationalisation — Unit 4 Candidate-Only Policy Authority — Planning Review

## 1. Authority and baseline

Unit 3 is committed and accepted at `03f0223`. Scope Lock §4.6 and Implementation Plan §8 authorize the first production retrieval authority: exactly two `APPROVED`/`AUTOMATIC` rules for `knowledge-memory.candidate-evaluation`, one for `memory.retrieve` → `READ`/`MEMORY` and one for `memory.retrieve_document` → `READ`/`DOCUMENT`.

Unit 2's two exact verb-only `DENIED` guards remain mandatory. No authority is authorized for `evidence-intelligence.input-resolution`, absent, unregistered, retired, mismatched, conversational, Reasoning Context, case-work, or other purposes.

## 2. Fresh production trace

`ParkerRuntime` constructs one `InMemoryAuthorizationPurposeRegistry`, one `DefaultPermissionPolicy`, one `DefaultPermissionEngine`, and one parent `PermissionFilteredMemoryRetrieval`. It registers both real purposes, creates exactly two immutable purpose-bound views, and supplies them to the candidate evaluator and Evidence Intelligence resolver. Both views re-enter the same parent request builder and authority path.

Production currently contains coarse `READ`/`MEMORY` and `READ`/`DOCUMENT` approvals plus the two Unit 2 exact-verb denials. With no purpose-specific rule, both real consumers deny. Unit 1's accepted selection algorithm makes a purpose-plus-verb rule specificity two, the guard specificity one, and the coarse rule specificity zero; a unique maximal rule governs independent of list order, while duplicate or incomparable maxima deny.

## 3. Authorized implementation

Only `src/composition/ParkerRuntime.kt` may change. It will add exactly:

1. `memory.retrieve` / `READ` / `MEMORY` / `knowledge-memory.candidate-evaluation` → `APPROVED` / `AUTOMATIC`;
2. `memory.retrieve_document` / `READ` / `DOCUMENT` / `knowledge-memory.candidate-evaluation` → `APPROVED` / `AUTOMATIC`.

The two Unit 2 guards remain byte-for-byte unchanged. No Evidence Intelligence, purpose-null, coarse, or other approval is permitted.

## 4. Test boundary

The accepted Unit 4 surface is:

- `tests/composition/ParkerRuntimeMemoryRetrievalOperationalisationCompositionTest.kt` for the exact production rules, complete outcome matrix, specificity, order independence, ambiguity, wrong verb/type, coarse regressions, and real candidate retrieval;
- `tests/composition/ParkerRuntimeAuthorizationPurposeCompositionTest.kt` to replace obsolete assertions that no production rule names a purpose and that candidate retrieval always denies;
- `tests/composition/ParkerRuntimeEvidenceIntelligenceCompositionTest.kt` for real-record non-widening after candidate authority exists.

Unchanged policy, decorator, evaluator, and conversational-admission suites provide supporting regression evidence. Tests may inspect the real composed graph where no public observation seam exists, but authorization outcomes and real-consumer retrieval must use the production engine, policy, registry, decorator, durable Memory Core, and consumers rather than substitute authorization mocks.

## 5. Fail-closed and adversarial proof plan

Verification must prove candidate approval only at both exact verb/action/type intersections. Evidence Intelligence, absent, unregistered, retired, mismatched and synthetic purposes must deny. Wrong verb or resource type must deny. The guards must continue to defeat coarse approvals for every non-candidate case, candidate rules must defeat guards only for the exact active candidate purpose, reversing order must change nothing, and duplicate/conflicting candidate maxima must deny.

Unrelated coarse `READ`/`MEMORY` and `READ`/`DOCUMENT` acts must retain their accepted outcomes. One engine/policy/registry/decorator graph must remain authoritative. No Unit 5 promotion-closure implementation may be added.

## 6. Boundary Review determination

No separate Boundary Review is required. The two rules use the already-accepted `PermissionPolicyRule` fields and constants inside the authorized composition root. No protected contract or production component requires modification. Discovery of any such requirement is a stop condition.

## 7. Planning verdict

```text
PASS
```

Unit 4 may proceed within the exact production and test boundaries above. Unit 5 remains prohibited.
