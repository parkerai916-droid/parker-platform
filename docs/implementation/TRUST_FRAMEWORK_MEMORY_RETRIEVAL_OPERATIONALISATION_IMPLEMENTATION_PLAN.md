**Status:** Accepted Implementation Plan, amended by the Unit 2 Fail-Closed Guard Rules governance correction following the pre-implementation Unit 2 Planning Review. The correction authorises exactly two verb-specific `DENIED` guards needed to prevent the newly configured targetless verbs from inheriting existing coarse production approvals. It does not amend Unit 1, grant retrieval authority, or begin Unit 2 implementation. No Kotlin or test implementation is performed by this document. Nothing is staged, committed, or pushed.

# Trust Framework — Gap #54 Memory Retrieval Operationalisation — Implementation Plan

## 1. Authority and objective

Baseline: `main` at `334d709` (`docs: complete Authorization Purpose unit 6 reviews`), plus the accepted, currently uncommitted Operationalisation Scope Lock and review artifacts.

This Plan translates the accepted Scope Lock exactly. It does not revisit the Memory Retrieval Contract Design, either Adopted Clarification, Authorization Purpose Units 1–6, or the Scope Lock's policy-content determination.

The sole implementation outcome is that governed Knowledge Candidate evidence resolution can pass through the existing permission-filtered Memory Core path and complete the already-governed Knowledge promotion flow, while Evidence Intelligence remains denied. This closes Gap #54's Memory Core authorization/promotion blocker only.

## 2. Current production trace and dependency order

Current denial occurs because `PermissionFilteredMemoryRetrieval` constructs an `ExecutionRequest` with an exact Memory retrieval verb and empty `targetResources`; `DefaultPermissionPolicy` derives resource types only from Resource Registry targets; `ActionMapper` therefore has no applicable mapping and the real engine returns `DENIED`.

The minimum safe dependency order is:

1. build policy mechanism with empty-by-default configuration;
2. register/configure the two action mappings, two targetless derivations and two Purpose values, plus the two exact verb-specific fail-closed `DENIED` guards required to prevent fall-through to existing coarse approvals, but no approving rule;
3. bind and propagate the two consumer purposes, still with no approving rule;
4. add exactly the two accepted candidate-evaluation approving rules; and
5. prove the complete composed admission-to-persistence path and Evidence Intelligence non-widening.

Only Unit 4 first creates new production authority. Units 1–3 must independently remain denied.

## 3. Frozen implementation shapes

### 3.1 Targetless derivation

`DefaultPermissionPolicy` gains an immutable constructor configuration equivalent to:

```kotlin
targetlessResourceTypesByProposedAction: Map<String, Set<ResourceType>> = emptyMap()
```

The default is empty so all existing construction and behavior remain unchanged. When and only when `request.targetResources` is empty, each proposed action is mapped independently using only the resource types configured for that exact verb. Requests with real targets continue to use only Resource Registry resolution. There is no union that could let one proposed action borrow another action's derived type, no general empty-target fallback, and no fabricated `ResourceId`.

Production supplies exactly:

- `memory.retrieve` → `{ MEMORY }`;
- `memory.retrieve_document` → `{ DOCUMENT }`.

`ActionMapper` remains unchanged. It continues to resolve registered vocabulary against the types supplied by policy.

### 3.2 Verb-specific policy representation and selection

`PermissionPolicyRule` gains one backward-compatible optional field equivalent to:

```kotlin
val proposedAction: String? = null
```

`null` preserves every existing coarse rule. A non-null value matches only the exact `ActionMappingResult.Resolved.proposedAction` carried from `ActionMapper`; free text, `intent` and metadata are never interpreted.

For each resolved mapping, policy considers rules matching `(PermissionAction, ResourceType)` and whose optional dimensions match:

- `authorizationPurpose == null` or equals the request's active registered Purpose;
- `proposedAction == null` or equals the resolved verb.

Specificity is a partial order by included matching dimensions. A matching rule that includes every dimension another rule includes and at least one more governs over it. Thus a Purpose-and-verb-specific rule governs over Purpose-only, verb-only and fully coarse rules. Multiple applicable maximal rules with conflicting or duplicate authority are ambiguity and produce `DENIED`; list order never selects the result. Existing callers with only one applicable coarse rule retain current behavior.

An absent, unregistered or retired Purpose cannot select a Purpose-specific rule. Accepted Unit 4 coarse-rule compatibility remains unchanged globally; for the two Memory verbs this Plan adds no coarse approving rule, so such requests remain denied.

### 3.3 Minimum lawful purpose carrier

The public `MemoryRetrieval` contract and both consumer constructors already accept the correct abstraction and must remain unchanged. Adding a Purpose parameter to all Memory Core methods would broaden a frozen public contract and every consumer unnecessarily. Constructing two `PermissionFilteredMemoryRetrieval` instances would violate the accepted single-decorator boundary.

The minimum lawful carrier is therefore an immutable purpose-bound `MemoryRetrieval` view created by the one production `PermissionFilteredMemoryRetrieval` instance:

```kotlin
fun forAuthorizationPurpose(purpose: AuthorizationPurposeId): MemoryRetrieval
```

The returned private adapter holds only the parent decorator and one immutable Purpose. It delegates every `MemoryRetrieval` method back to that parent with the bound Purpose. It holds no raw delegate, engine, policy or registry, performs no authorization, and cannot produce a `PermissionDecision`. The parent remains the sole component that builds `ExecutionRequest`, calls the single engine and filters returned records.

The existing unbound `MemoryRetrieval` methods remain available and construct requests with no Purpose, preserving compatibility and fail-closed behavior. `ParkerRuntime` creates exactly two views from the same decorator and injects them through the consumers' existing `MemoryRetrieval` constructor parameters. No change is expected in `DefaultKnowledgeCandidateEvaluator.kt`, `EvidenceIntelligenceInputResolver.kt` or `MemoryCore.kt`.

### 3.4 Frozen production values and policy

`ParkerRuntime` defines, documents and registers exactly:

- `AuthorizationPurposeId("knowledge-memory.candidate-evaluation")` — candidate evidence evaluation; and
- `AuthorizationPurposeId("evidence-intelligence.input-resolution")` — Evidence Intelligence input resolution.

The existing registry contract stores the immutable governed identifiers and lifecycle. Their accepted meanings are documented beside the production constants and remain frozen by the Scope Lock; the registry contract is not changed to add descriptive metadata.

The production rules added in Unit 4 are exactly:

| Verb | Action/type | Purpose | Decision/level |
|---|---|---|---|
| `memory.retrieve` | `READ` / `MEMORY` | `knowledge-memory.candidate-evaluation` | `APPROVED` / `AUTOMATIC` |
| `memory.retrieve_document` | `READ` / `DOCUMENT` | `knowledge-memory.candidate-evaluation` | `APPROVED` / `AUTOMATIC` |

There is no Evidence Intelligence rule and no coarse Memory retrieval approval.

### 3.5 Unit 2 fail-closed guard correction

Fresh pre-implementation inspection found that production already contains coarse `READ`/`MEMORY` and `READ`/`DOCUMENT` `APPROVED` rules for separately governed acts. Once Unit 2 registers and derives the new Memory verbs, absence of any verb-specific rule would allow those coarse rules to authorize the new requests. The originally planned “no rule applies” denial was therefore incorrect.

Unit 2 is amended to add exactly these non-authorizing guards:

| Verb | Action/type | Authorization Purpose | Decision/level |
|---|---|---|---|
| `memory.retrieve` | `READ` / `MEMORY` | `null` | `DENIED` / `AUTOMATIC` |
| `memory.retrieve_document` | `READ` / `DOCUMENT` | `null` | `DENIED` / `AUTOMATIC` |

These rules name acts, not callers. Under Unit 1's accepted specificity mechanism each exact-verb guard outranks the applicable coarse action/type approval, independent of rule order. They grant no authority and apply regardless of absent or ineffective Purpose.

Later Unit 4's already-accepted candidate rules name both the same exact verb and the active `knowledge-memory.candidate-evaluation` Purpose. Their Purpose-plus-verb specificity is greater than these verb-only guards, so they may lawfully override the guards only for that exact active Purpose. Evidence Intelligence has no equivalent approving rule and therefore remains governed by the verb-specific denial. No Unit 3 propagation or Unit 4 approval is authorised by this correction.

## 4. Common unit gates

Every unit below has these mandatory gates in addition to its unit-specific checks:

- targeted tests must pass before the unit Completion Review;
- the full existing automated suite must be run without modifying unrelated failures to obtain a pass;
- any unrelated environmental failure, including the known Windows OCR separator issue if still present, must be recorded and classified rather than fixed here;
- a formal unit Completion Review must compare the actual diff and tests to this Plan and the frozen Scope Lock;
- a genuine unit Independent Constitutional Review must inspect production behavior directly, treating the Completion Review as evidence rather than authority; and
- no next unit begins until the preceding unit is accepted.

## 5. Unit 1 — Policy resolution mechanism

### Authority and preconditions

Inherited from Scope Lock §§3, 4.1, 4.2, 7 and 12. Accepted Authorization Purpose Units 1–6 are the unchanged baseline. No production action, Purpose or approving rule exists yet.

### Exact production files expected to change

- `src/runtime/DefaultPermissionPolicy.kt`

`ActionMapper.kt`, `ExecutionRequest.kt`, `PermissionEngine`, `ResourceRegistry` and Memory Core contracts must not change.

### Exact tests expected to change or be added

- modify `tests/runtime/DefaultPermissionPolicyTest.kt` for backward compatibility and existing coarse/Purpose behavior;
- add `tests/runtime/MemoryRetrievalPermissionPolicyOperationalisationTest.kt` for closed derivation, verb discrimination, specificity and ambiguity.

### Implementation boundary

Add the empty-by-default targetless derivation configuration, optional `PermissionPolicyRule.proposedAction`, independent per-verb mapping, partial-order specificity and ambiguity denial. Add no production configuration or rule content.

### Prohibited changes

No vocabulary registrations, Purpose registrations, real Memory rules, caller logic, fake resources, engine changes or runtime wiring.

### Targeted verification

Prove:

- empty configuration preserves current target/resource behavior;
- unknown and unconfigured empty-target verbs deny;
- each configured verb receives only its own configured type;
- derivation without a rule denies;
- verb-only, Purpose-only and Purpose-plus-verb matching are deterministic;
- Purpose-plus-verb governs over every coarser applicable rule regardless of list order;
- incomparable or equally maximal conflicting/duplicate rules deny;
- absent, unregistered and retired Purposes cannot select Purpose-specific authority; and
- existing single coarse rules behave unchanged.

### Review requirements

Full-suite verification, Unit 1 Completion Review and Unit 1 Independent Constitutional Review are mandatory under §4.

## 6. Unit 2 — Inert production registration and configuration

### Authority and preconditions

Inherited from Scope Lock §§4.1, 4.3, 4.4 and 7. Unit 1 must be accepted. The new mechanism exists but production configuration is still empty.

### Exact production files expected to change

- `src/composition/ParkerRuntime.kt`

No registry, vocabulary or policy contract changes are permitted.

### Exact tests expected to change or be added

- modify `tests/composition/ParkerRuntimeAuthorizationPurposeCompositionTest.kt` to replace the now-obsolete empty-production-registry expectation with exact two-value registration and no-other-value assertions;
- add `tests/composition/ParkerRuntimeMemoryRetrievalOperationalisationCompositionTest.kt` for exact action mappings, derivations, single authority and continued denial.

### Implementation boundary

Register the two exact action entries, configure the two exact derivations, register the two exact accepted Purpose IDs through the existing production registry, and add exactly the two verb-specific `DENIED` guards in §3.5. Add no purpose-bound consumer view and no approving rule.

### Prohibited changes

No consumer propagation, Memory retrieval approval, Evidence Intelligence approval, Purpose-specific rule, adapter, direct access, unrelated registration or policy rule beyond the two exact verb-specific `DENIED` guards.

### Targeted verification

Through the real composed engine, prove:

- both verbs resolve to only their frozen action/type mapping;
- action registration plus derivation returns `DENIED` with absent, either registered, unregistered or retired Purpose because the exact-verb guard outranks the pre-existing coarse approval;
- Purpose registration alone grants no authority;
- both exact guards are present as `DENIED`/`AUTOMATIC`, and no additional Memory rule exists;
- existing coarse `READ`/`MEMORY` and `READ`/`DOCUMENT` approvals cannot authorize either exact Memory verb, independent of rule ordering;
- exactly two real Purpose values are active;
- no fake Resource Registry entry exists; and
- the runtime still has one engine, policy, registry and shared decorator.

### Review requirements

Full-suite verification, Unit 2 Completion Review and Unit 2 Independent Constitutional Review are mandatory under §4.

## 7. Unit 3 — Immutable consumer-purpose propagation

### Authority and preconditions

Inherited from Scope Lock §§4.5, 7, 8 and 9. Unit 2 must be accepted. Both purposes exist and both verbs resolve, but all retrieval remains denied.

### Exact production files expected to change

- `src/composition/PermissionFilteredMemoryRetrieval.kt`;
- `src/composition/ParkerRuntime.kt`.

No production change is expected in:

- `src/interfaces/MemoryCore.kt`;
- `src/runtime/DefaultKnowledgeCandidateEvaluator.kt`; or
- `src/runtime/EvidenceIntelligenceInputResolver.kt`.

Their existing `MemoryRetrieval` dependency is retained.

### Exact tests expected to change or be added

- modify `tests/composition/PermissionFilteredMemoryRetrievalTest.kt` for immutable bound views and captured request Purpose;
- modify `tests/composition/ParkerRuntimeAuthorizationPurposeCompositionTest.kt` for real consumer adoption;
- modify `tests/composition/ParkerRuntimeEvidenceIntelligenceCompositionTest.kt` to prove distinct views share one underlying decorator and Evidence Intelligence remains denied;
- modify `tests/composition/ParkerRuntimeMemoryCoreDurabilityCompositionTest.kt` only where its structural assertions currently assume each consumer field directly holds the decorator;
- extend `tests/composition/ParkerRuntimeMemoryRetrievalOperationalisationCompositionTest.kt` with binding and continued-denial checks.

### Implementation boundary

Add the private, non-authoritative purpose-bound view factory described in §3.3. Bind candidate evaluation to `knowledge-memory.candidate-evaluation` and Evidence Intelligence to `evidence-intelligence.input-resolution` at composition. The single parent decorator constructs all requests and performs all filtering.

### Prohibited changes

No public Memory Retrieval signature change, second decorator instance, direct delegate exposure, caller inference, reflection, stack inspection, principal substitution, policy rule or approval.

### Targeted verification

Prove:

- bound purpose is immutable and appears on every direct and query-shaped request made through the view;
- the unbound decorator remains Purpose-absent and backward compatible;
- the view holds no engine, raw delegate, policy or registry and cannot decide;
- both consumer views delegate to the same production decorator and engine;
- candidate propagation without a rule remains denied;
- Evidence Intelligence remains denied for an existing record; and
- per-record evaluation and denied/not-found behavior remain unchanged.

### Review requirements

Full-suite verification, Unit 3 Completion Review and Unit 3 Independent Constitutional Review are mandatory under §4.

## 8. Unit 4 — Candidate-only production policy authority

### Authority and preconditions

Inherited exclusively from Scope Lock §4.6. Unit 3 must be accepted. All required dimensions are present and observable but no Memory retrieval is approved.

### Exact production files expected to change

- `src/composition/ParkerRuntime.kt`

No other production file may change in this unit.

### Exact tests expected to change or be added

- extend `tests/composition/ParkerRuntimeMemoryRetrievalOperationalisationCompositionTest.kt` with exact approval/denial matrix;
- modify the obsolete Gap #54 continued-denial section of `tests/composition/ParkerRuntimeAuthorizationPurposeCompositionTest.kt` so it proves synthetic, absent and non-candidate purposes remain denied while the exact candidate purpose can govern;
- extend `tests/composition/ParkerRuntimeEvidenceIntelligenceCompositionTest.kt` with post-rule non-widening and coarse-rule challenge evidence.

### Implementation boundary

Add exactly the two rules in §3.4. No rule for Evidence Intelligence and no Purpose-null Memory retrieval rule may be added.

The two Unit 4 Purpose-plus-verb rules are intentionally more specific than Unit 2's existing verb-only `DENIED` guards. They may override those guards only for the exact active `knowledge-memory.candidate-evaluation` Purpose. The guards remain governing denials for absent, ineffective, Evidence Intelligence and every other Purpose.

### Prohibited changes

No new Purpose, action, resource type, confirmation policy, consumer, bypass or broader rule. No change to Knowledge Submission or promotion logic.

### Targeted verification

Through real production policy/engine composition, prove:

- candidate purpose plus exact matching verb returns `APPROVED`/`AUTOMATIC` for both record categories;
- absent, unregistered, retired, Evidence Intelligence, mismatched and synthetic purposes remain denied;
- wrong verb/type combinations deny;
- no existing coarse `READ/MEMORY` rule authorizes either Memory verb;
- the exact candidate rule governs over an adversarial applicable coarse rule independent of order;
- ambiguity denies; and
- Evidence Intelligence remains denied for a real existing Memory record.

### Review requirements

Full-suite verification, Unit 4 Completion Review and Unit 4 Independent Constitutional Review are mandatory under §4.

## 9. Unit 5 — Real composed end-to-end acceptance

### Authority and preconditions

Inherited from Scope Lock §§2, 8, 12 and 13. Units 1–4 must each be accepted. This is verification-only unless an inaccurate KDoc/comment must be updated to describe the now-implemented behavior.

### Exact production files expected to change

- none.

If verification reveals a production defect, stop and return it to the responsible earlier unit; do not fix production code inside Unit 5.

### Exact tests expected to change or be added

- modify `tests/composition/ParkerRuntimeConversationalMemoryAdmissionCompositionTest.kt`, replacing its frozen historical Gap #54 failure expectation with real successful promotion and retaining ordinary-reply regression proof;
- extend `tests/composition/ParkerRuntimeMemoryRetrievalOperationalisationCompositionTest.kt` for final graph invariants if not already complete;
- extend `tests/composition/ParkerRuntimeEvidenceIntelligenceCompositionTest.kt` to prove same-runtime Evidence Intelligence denial alongside successful candidate retrieval.

No mock-only substitute test is acceptable.

### Implementation boundary

Exercise the real production composition:

```text
explicit owner Remember
    → MemoryAdmissionCoordinator
    → newly written Memory Core Assertion
    → KnowledgeCandidate
    → DefaultKnowledgeSubmission
    → DefaultKnowledgeCandidateEvaluator
    → purpose-bound view of the shared PermissionFilteredMemoryRetrieval
    → DefaultPermissionEngine / DefaultPermissionPolicy
    → active candidate-evaluation Purpose and exact candidate rule
    → authorized per-record evidence resolution
    → promotion
    → KnowledgeItemPersistence
```

The test must inspect the real Knowledge persistence boundary to prove a promoted `KnowledgeItem` exists with the expected evidence/provenance, not infer promotion merely from a friendly reply. The same production runtime configuration must also hold a genuine Memory Core record addressed through Evidence Intelligence and prove its retrieval remains denied/non-disclosing.

### Prohibited changes

No conversational recall assertion, search, discoverability, Reasoning Context, Unit 9 retrieval, semantic ranking, durability redesign or test-only production authority.

### Targeted verification

Prove:

- the owner-facing reply reports successful storage only after real promotion/persistence succeeds;
- the just-written assertion is the candidate evidence actually authorized and resolved;
- the explicit-owner-instruction promotion exception remains the existing promotion basis rather than a changed criterion;
- the resulting Knowledge Item is persisted through the real production store;
- ordinary conversational reply behavior is unchanged;
- Evidence Intelligence remains denied in the same accepted production composition; and
- no claim of conversational recall or discoverability is made.

### Review requirements

Full-suite verification, Unit 5 Completion Review and Unit 5 Independent Constitutional Review are mandatory under §4. Unit 5 acceptance closes Gap #54 operational implementation only.

## 10. Fail-closed sequencing proof

| State after unit | Resolution mechanism | Actions/purposes registered | Consumer Purpose propagated | Candidate rules | Candidate retrieval | Evidence Intelligence |
|---|---:|---:|---:|---:|---|---|
| Baseline | No | No | No | No | `DENIED` | `DENIED` |
| Unit 1 | Mechanism only, empty defaults | No | No | No | `DENIED` | `DENIED` |
| Unit 2 | Configured | Yes | No | Two exact verb-only `DENIED` guards; no approval | `DENIED` | `DENIED` |
| Unit 3 | Configured | Yes | Yes | No | `DENIED` | `DENIED` |
| Unit 4 | Configured | Yes | Yes | Exact candidate-only pair | `APPROVED` only for exact active candidate Purpose | `DENIED` |
| Unit 5 | Unchanged | Unchanged | Unchanged | Unchanged | Real promotion verified | Same-runtime denial verified |

Therefore:

- registration alone grants nothing;
- derivation alone grants nothing;
- Purpose registration alone grants nothing;
- the two Unit 2 verb-specific guards prevent existing coarse approvals from widening either Memory verb;
- propagation without a rule grants nothing;
- absent, unregistered and retired Purposes cannot use candidate rules;
- no coarse Memory approval ever exists;
- exact Purpose-and-verb Unit 4 rules outrank the verb-only guards only for the exact active candidate Purpose;
- ambiguity denies; and
- Evidence Intelligence never passes through a permissive intermediate state.

## 11. Full expected production surface

Expected modified production files across Units 1–4:

- `src/runtime/DefaultPermissionPolicy.kt`;
- `src/composition/PermissionFilteredMemoryRetrieval.kt`;
- `src/composition/ParkerRuntime.kt`.

No other production file is expected to change. A discovered need to change `MemoryCore.kt`, `DefaultKnowledgeCandidateEvaluator.kt`, `EvidenceIntelligenceInputResolver.kt`, `ActionMapper.kt`, Authorization Purpose registry contracts, Permission Engine contracts, Resource Registry, Knowledge Submission or persistence logic is a stop condition requiring plan review—not implied authority.

## 12. Full expected test surface

Expected new tests:

- `tests/runtime/MemoryRetrievalPermissionPolicyOperationalisationTest.kt`;
- `tests/composition/ParkerRuntimeMemoryRetrievalOperationalisationCompositionTest.kt`.

Expected modified tests:

- `tests/runtime/DefaultPermissionPolicyTest.kt`;
- `tests/composition/PermissionFilteredMemoryRetrievalTest.kt`;
- `tests/composition/ParkerRuntimeAuthorizationPurposeCompositionTest.kt`;
- `tests/composition/ParkerRuntimeEvidenceIntelligenceCompositionTest.kt`;
- `tests/composition/ParkerRuntimeMemoryCoreDurabilityCompositionTest.kt` only for structural view assertions;
- `tests/composition/ParkerRuntimeConversationalMemoryAdmissionCompositionTest.kt`.

Any additional test file requires a unit Planning Review explanation showing which frozen acceptance criterion it proves. Test edits must not create production authority or weaken unrelated assertions merely to accommodate a changed object graph.

## 13. Explicit exclusions

No unit may implement or redesign:

- Knowledge discoverability;
- Reasoning Context integration;
- conversational or case-work retrieval;
- Programme 3 Unit 9 Knowledge Retrieval;
- semantic/vector retrieval, ranking or search;
- Memory Core or KnowledgeItem durability/persistence;
- Knowledge promotion criteria;
- direct or unfiltered Memory Core access;
- another Permission Engine, policy, registry or permission-filtering decorator;
- fake Memory Core Resource Registry entries;
- principal/caller/class/stack-based discrimination;
- Evidence Intelligence Memory retrieval authority;
- unrelated Authorization Purpose consumer adoption; or
- OCR portability work.

## 14. Completion boundary and next action

This Plan is complete when independently accepted as an accurate, minimal translation of the frozen Scope Lock. Implementation must then begin with Unit 1 only and stop at each unit's formal review boundary.

Successful Unit 5 acceptance means the Memory Core authorization/promotion blocker is closed. Parker's ability to discover and conversationally recall the promoted item remains unresolved later work and must not be claimed.
