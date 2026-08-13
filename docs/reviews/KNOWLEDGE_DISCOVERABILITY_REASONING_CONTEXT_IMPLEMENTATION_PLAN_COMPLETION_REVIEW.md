**Status:** Independent Completion Review of the Knowledge Discoverability and Governed Retrieval into Reasoning Context Implementation Plan. Plan review only; no Kotlin or test implementation is performed or accepted by this document.

# Knowledge Discoverability and Reasoning Context Implementation Plan — Independent Completion Review

## 1. Immutable review baseline

This review was performed from scratch against:

```text
base=ac6f861111d74be13fe5b220e598dfd7159b6e6a
plan=59521aed8df5c212bc03a43fb3142dc3564e456c
```

Fresh baseline checks established that `origin/main` resolved to the exact base, `origin/governance/knowledge-discoverability-implementation-plan` resolved to the exact plan commit, the working tree was clean on `main`, `git diff --check origin/main..origin/governance/knowledge-discoverability-implementation-plan` passed, and the branch changed only the new Implementation Plan document.

Earlier reviews and rejected plan revisions were treated only as historical defect prompts. None was used as acceptance evidence for this commit.

## 2. Evidence examined

The corrected Implementation Plan and all four governing documents were read completely:

- `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_PLANNING_REVIEW.md`
- `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_BOUNDARY_REVIEW.md`
- `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_CONTRACT_DESIGN.md`
- `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_SCOPE_LOCK.md`

The fresh repository inspection covered every existing file in the frozen eight-file implementation boundary, confirmed the three planned new files are absent, and inspected the relevant production and test caller graph. The principal evidence included:

- `src/interfaces/KnowledgeStore.kt`
- `src/runtime/DefaultReasoningContextAssembler.kt`
- `src/composition/ParkerRuntime.kt`
- `tests/runtime/DefaultReasoningContextAssemblerTest.kt`
- `tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt`
- `src/runtime/ActionMapper.kt`
- `src/runtime/DefaultPermissionPolicy.kt`
- `src/runtime/DefaultPermissionEngine.kt`
- `src/runtime/AuthorizationPurposeRegistry.kt`
- `src/runtime/InMemoryIdentityService.kt`
- `src/composition/PermissionFilteredMemoryRetrieval.kt`
- `src/contracts/ExecutionRequest.kt`
- `src/contracts/ActionMapping.kt`
- `src/runtime/DefaultKnowledgeRetrieval.kt`
- `src/runtime/KnowledgeItemPersistence.kt`
- `src/interfaces/MemoryCore.kt`
- `src/runtime/DefaultKnowledgeCandidateEvaluator.kt`
- `src/runtime/DefaultKnowledgeSubmission.kt`
- `src/runtime/MemoryAdmissionCoordinator.kt`
- `tests/runtime/FakePermissionEngine.kt`
- `tests/composition/PermissionFilteredMemoryRetrievalTest.kt`
- `tests/composition/ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`
- `tests/composition/ParkerRuntimeConversationalMemoryAdmissionCompositionTest.kt`

No implementation was created, compiled, or claimed complete during this plan review.

## 3. Completion and sequencing audit

The Plan defines exactly five strictly gated units. Each unit depends only on completed predecessors and requires an accepted Unit Completion Review and Independent Constitutional Review before the next begins.

The implementation boundary is exactly eight files: four production and four test files. The three genuinely new implementation files are correctly classified as:

1. `src/runtime/DefaultReasoningKnowledgeSource.kt`
2. `tests/runtime/DefaultReasoningKnowledgeSourceTest.kt`
3. `tests/composition/ParkerRuntimeReasoningKnowledgeSourceCompositionTest.kt`

Unit 3 atomically owns the constructor/cutover boundary across exactly:

```text
src/runtime/DefaultReasoningContextAssembler.kt
tests/runtime/DefaultReasoningContextAssemblerTest.kt
src/composition/ParkerRuntime.kt
```

Fresh caller inspection confirms `ParkerRuntime.kt` is the sole production construction site for `DefaultReasoningContextAssembler`; every direct test construction is confined to the Unit 3-owned assembler test. Unit 3 changes the assembler contract, adapts its complete test surface, constructs the governed source from the already-shared dependencies, retires the legacy production binding, and updates the sole production caller in one unit and review boundary. The new source can be constructed before assigning the assembler within the same existing composition method, with all required collaborators in scope. No additional file or intermediate non-compiling unit boundary is required. Unit 3's completion gate requires direct diff inspection and a successful full Gradle suite.

Unit 4 owns only the new composition test file. It is explicitly test-only, has no authority to edit or repair production, and must return a production defect to Unit 3 if its verification fails. Units 1–4 leave `ParkerRuntimeReasoningContextIntegrationTest.kt` untouched; Unit 5 alone extends it and renames the prescribed authorized-empty test. Unit 5 is likewise test-only and cannot repair production.

## 4. Unit 2 proof completeness

Unit 2 assigns differential item-level denial and silent exclusion to a controllable `FakePermissionEngine`, without claiming that the real `DefaultPermissionPolicy` can create mixed outcomes for candidates sharing the same principal/action/resource-type/Purpose/verb tuple.

Its required tests separately and mandatorily prove:

- item-level Knowledge Item visibility denial;
- denied Assertion evidence;
- denied Entity evidence;
- missing or deleted Assertion evidence;
- missing or deleted Entity evidence;
- unsupported `ToDocument`;
- unsupported `ToRelationship`;
- an authorized-partial result whose excluded candidate is specifically denied at evidence resolution;
- positive `ACTIVE` evidence and each non-`ACTIVE` status (`DISPUTED`, `SUPERSEDED`, `ARCHIVED`, `DELETED`);
- the generic promotion-basis false-match regression;
- authorization-before-persistence, lifecycle behavior, deterministic normalization and matching, insertion ordering, last-step bounding, and dependency-fault propagation.

The Plan prohibits any of those proofs from substituting for another. Together they fully discharge Contract Design Invariant 7 and the referenced-evidence obligations in Scope Lock Section 11. The removal of the infeasible composed mixed-evidence companion from Unit 5 does not weaken denied-evidence coverage because Unit 2 retains the complete, differential denial proof at the only tier capable of controlling that outcome honestly.

## 5. Purpose-bound Document-denial proof

The Unit 4 proof is executable as planned using real production mechanisms and a test-local independent graph.

The corrected source attribution is exact. The production mapping
`memory.retrieve_document -> READ / DOCUMENT` is the pre-existing Gap #54 Unit 2 vocabulary registration at `ParkerRuntime.kt` lines 529–533. Unit 3 must preserve it unchanged and does not introduce, own, modify, replace, or re-register it. `ParkerRuntime.kt` lines 991–1014 identify the separate Knowledge Retrieval resource/action registration site where Unit 3 adds only the new `knowledge.retrieve_for_reasoning_context -> READ / MEMORY` entry. Unit 4 reproduces the pre-existing Document mapping only inside its independent test graph; this authorizes no production change.

The proof requires, in order:

1. a real `InMemoryAuthorizationPurposeRegistry` with the exact Purpose registered and directly confirmed active;
2. a real `InMemoryActionVocabulary` with exactly the pre-existing Document entry;
3. an exact `vocabulary.lookup(...)` assertion;
4. a real `ActionMapper` and a direct `ActionMapper.map(...)` assertion resolving exactly `READ/DOCUMENT`;
5. a real `DefaultPermissionPolicy`, with the frozen targetless mapping, the existing Document guard, and exactly the three new reasoning rules but no Document approval;
6. a real `InMemoryIdentityService` principal registered as `CREATED`, transitioned to `ACTIVE`, and directly confirmed `ACTIVE` before either authorization assertion;
7. a real `DefaultPermissionEngine`, `PermissionFilteredMemoryRetrieval`, and `forAuthorizationPurpose(...)` view;
8. a genuine non-null delegate `Document`, a direct purpose-bound `getDocument(...)` call returning `null`, and a separate direct policy evaluation returning `DENIED`.

The active identity requirement prevents the proof from passing at `DefaultPermissionEngine`'s identity gate. The active Purpose and exact real action mapping prevent Purpose-registration rejection, `UNKNOWN_ACTION`, and `RESOURCE_TYPE_MISMATCH` from explaining the result. `ExecutionRequest` is correctly described as carrying no `resourceType`; `ResourceType.DOCUMENT` is derived by the real policy from the frozen targetless proposed-action mapping. With those preliminary paths excluded, `DefaultPermissionPolicy.ruleOutcomeFor` selects the existing verb-specific Document guard and determines `DENIED`.

`PermissionFilteredMemoryRetrieval.getDocument` at lines 138–148 first obtains the real document from its delegate and then evaluates permission; on denial it returns `null`. The Plan therefore states the real behavior accurately: delegate fetch followed by nondisclosure, not a claim that the delegate was never reached. The proof needs no access to a `ParkerRuntime` private field or local instance, no reflection, no accessor, no visibility widening, no duplicated authorization algorithm, no raw retrieval exposure, no new production file, and no `ToDocument` path in `DefaultReasoningKnowledgeSource`.

## 6. Scope, citations, and closure audit

The Plan carries forward exactly three new permission rules, grants no Document approval, preserves the existing Document denial, and leaves the pre-existing Gap #54 rules and candidate-evaluation authority unchanged. It requires an atomic single-feed cutover with no production `InMemoryKnowledgeStore`, leaves World Model and Conversation History behavior unchanged, and structurally preserves Evidence Intelligence non-widening.

The deterministic normalization, locale-independent substring matching, Entity rendering source, status gate, staleness disclosure, prompt escaping, field ordering, insertion ordering, and last-step result bounding are fixed with no substantive implementation choice left open. The positive Unit 5 proof is a genuine same-runtime promotion-to-recall chain through the real conversational Remember path and real assembled prompt, with no synthetic `KnowledgeItem` substitution. Unit 5 contains no infeasible mixed denied-evidence negative companion.

The citation audit found the paths, symbols, section references, file status, and relevant line ranges accurate, including:

- Scope Lock Section 12, unambiguously identified as the Scope Lock's section;
- the assembler's sole production caller at `ParkerRuntime.kt` line 409;
- the Unit 3 replacement block in `DefaultReasoningContextAssemblerTest.kt` lines 397–571, ending before the World Model section;
- the pre-existing Document vocabulary entry at `ParkerRuntime.kt` lines 529–533;
- the separate Knowledge Retrieval registration site at lines 991–1014;
- `DefaultPermissionPolicy.ruleOutcomeFor` at lines 228–244;
- `PermissionFilteredMemoryRetrieval.getDocument` at lines 138–148 and targetless request construction at lines 253–271;
- the positive proof references to Plan Section 14 and Unit 5 Section 10;
- Plan Section 17 exclusions and Section 19 sequencing stop conditions.

Every Scope Lock stop condition is carried forward, and the Plan adds unit/file/review/atomic-cutover stop conditions without weakening any governing stop. It makes no implementation-completion, test-success, conversational-recall, restart-durability, constant-time, timing-resistance, durable-audit, programme-closure, or Closure Determination claim.

## 7. Findings and verdict

```text
P0=0
P1=0
P2=0
P3=0
VERDICT=ACCEPTED
```

No substantive correction remains in the reviewed Plan.

`ACCEPTED` applies only to the completeness and executability of the implementation plan at commit `59521aed8df5c212bc03a43fb3142dc3564e456c` against base `ac6f861111d74be13fe5b220e598dfd7159b6e6a`. It does not prove that any Kotlin implementation exists, compiles, or passes tests; it does not prove live conversational recall, restart durability, durable permission-decision auditing, programme completion, closure, or a Closure Determination. Those remain future implementation and independently gated review obligations.
