**Status:** Implementation Unit 2 Completion Review. This review accepts only the governed `DefaultReasoningKnowledgeSource` implementation and its tests. It does not implement, accept, or begin Unit 3.

# Knowledge Discoverability and Reasoning Context Implementation Unit 2 - Completion Review

## 1. Immutable baseline

```text
base=89f3049c522f0c68da78a8ac53dbb6a73b6b0867
unit=2f3ac3bbd2eab973f59bf6ceef431078c12f4b1d
branch=implementation/knowledge-discoverability-unit-2-reasoning-source
```

The unit is based on merged and accepted Implementation Unit 1. Its complete diff against that baseline contains exactly two new files:

```text
src/runtime/DefaultReasoningKnowledgeSource.kt
tests/runtime/DefaultReasoningKnowledgeSourceTest.kt
```

No pre-existing file changed. `git diff --check` passes.

## 2. Frozen implementation contract

`DefaultReasoningKnowledgeSource` has exactly the five frozen constructor dependencies and implements `ReasoningKnowledgeSource` without adding another capability or dependency.

The implementation follows the governed ten-step order:

1. accept the validated query;
2. construct the act-level request;
3. authorize the act before any persistence read;
4. read promoted Knowledge Items only after approval;
5. apply the Knowledge Item lifecycle filter;
6. authorize each eligible item before evidence dereference;
7. dereference only `ToAssertion` and `ToEntity` through the supplied purpose-bound `MemoryRetrieval`;
8. require an `ACTIVE` referenced record, normalize content, and match only that resolved content;
9. construct `SafeKnowledgeResultEntry` in frozen field order; and
10. apply `maximumResults` last.

`ToDocument` and `ToRelationship` return no result without making a Memory Core call. The implementation contains no Document action or authority, no registry mutation, no exception wrapper, and no call to an excluded retrieval method.

The resource identifier is reused unchanged, the proposed action is exactly `knowledge.retrieve_for_reasoning_context`, the supplied Authorization Purpose is carried on every act- and item-level request, the caller's correlation ID is preserved, and each evaluation receives a fresh request ID.

## 3. Matching, lifecycle, and failure semantics

CRLF and lone CR normalize to LF. No trim, whitespace collapse, stemming, synonym expansion, classification, or basis-text matching is introduced. Entity content is assembled deterministically from primary label followed by aliases with `" | "` separators. Matching is case-insensitive against resolved content only and remains locale-independent.

Only referenced records with `MemoryCoreRecordStatus.ACTIVE` can contribute content. Knowledge Item lifecycle filtering preserves the frozen `ACTIVE`/optional-`RETIRED` behavior. Candidate-level denial, missing evidence, denied evidence, unsupported reference kinds, and non-active referenced records produce silent candidate exclusion. Genuine dependency failures propagate unchanged.

Insertion order is preserved through filtering, and the result bound is applied only after authorization, dereference, record-status, and relevance filtering.

## 4. Test evidence

The new test file contains 39 distinct tests. Together they cover:

- positive Assertion and Entity resolution;
- act denial before persistence access;
- item denial before evidence dereference;
- genuine existing-but-denied Assertion and Entity records, with protected content proven absent;
- genuinely missing Assertion and Entity references, separately proven;
- unsupported `ToDocument` and `ToRelationship` references and the absence of forbidden calls;
- `ACTIVE` and every governed non-active referenced-record status;
- authorized-partial results with surviving insertion order;
- lifecycle behavior, ordering, bounds, staleness, normalization, Unicode preservation, locale independence, and fault propagation;
- the regression proving generic promotion-basis text cannot create a false content match.

The corrected denied-evidence fixtures contain real, non-null protected records in dedicated denied collections. The missing-evidence fixtures use identifiers absent from both allowed and denied collections. Both shapes assert that the exact identifier was requested, so denial and absence cannot substitute for each other.

The item-level mixed test proves only the approved candidate's evidence is dereferenced. Its denied-only companion proves zero evidence dereference occurs. These tests directly establish that item authorization precedes dereference.

The complete Gradle test suite passed on the Parker server at the corrected unit commit. A separate Windows run using Microsoft OpenJDK 17 also passed at the exact commit with `BUILD SUCCESSFUL` and exit code 0.

## 5. Scope and findings

No Unit 3 production, assembler, composition, or composition-test artifact was created or modified. The branch contains no change to `DefaultKnowledgeRetrieval`, `DefaultReasoningContextAssembler`, `ParkerRuntime`, Permission policy, Memory Core, governance inputs, build configuration, or any other file.

```text
P0=0
P1=0
P2=0
P3=0
```

No Unit 2 stop condition was triggered.

## 6. Verdict

```text
VERDICT=ACCEPTED
```

Implementation Unit 2 is complete at commit `2f3ac3bbd2eab973f59bf6ceef431078c12f4b1d`. Its implementation and distinct proof set satisfy Contract Design Invariant 7, Scope Lock Section 11, and Implementation Plan Section 7.

This verdict does not claim production composition, Reasoning Context injection, conversational recall, live verification, restart durability, durable auditing, programme completion, or closure. Unit 3 must not begin until this review and the Independent Constitutional Review are accepted and merged through the governed repository workflow.
