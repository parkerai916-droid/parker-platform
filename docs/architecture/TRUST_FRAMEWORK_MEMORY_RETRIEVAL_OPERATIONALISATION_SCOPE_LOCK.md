**Status:** Proposed Scope Lock. Governance only; subject to the accompanying Independent Constitutional Review and explicit acceptance. No Kotlin or test implementation is authorised merely by drafting this document. This document operationalises already-adopted Gap #54 architecture; it does not reopen or amend that architecture. Nothing is staged, committed, or pushed.

# Trust Framework — Gap #54 Memory Retrieval Operationalisation — Scope Lock

## 1. Baseline and authority

Repository baseline: `main` at `334d709` (`docs: complete Authorization Purpose unit 6 reviews`).

This Scope Lock is subordinate to, and implements without redesign:

- `TRUST_FRAMEWORK_MEMORY_RETRIEVAL_GATING_BLOCKER.md`;
- `TRUST_FRAMEWORK_MEMORY_RETRIEVAL_CONTRACT_DESIGN.md`, especially §§9, 14, 17, 19, 21 and 22;
- the Adopted `TRUST_FRAMEWORK_MEMORY_RETRIEVAL_RESOLUTION_DERIVATION_MECHANISM_CLARIFICATION.md`;
- the Adopted `TRUST_FRAMEWORK_MEMORY_RETRIEVAL_POLICY_RULE_COLLISION_CLARIFICATION.md`;
- the Adopted `TRUST_FRAMEWORK_AUTHORIZATION_CONTEXT_CONTRACT_DESIGN.md`;
- the accepted `TRUST_FRAMEWORK_AUTHORIZATION_PURPOSE_PROGRAMME.md`;
- `TRUST_FRAMEWORK_IMPLEMENTATION_SEQUENCE.md`;
- the Adopted `AUTHORIZATION_PURPOSE_SCOPE_LOCK.md` and accepted Authorization Purpose Units 1–6;
- `AUTHORIZATION_PURPOSE_IMPLEMENTATION_PLAN.md`, whose §10 deliberately defers existing-consumer retrofit and Gap #54 policy content to later, separately authorised work;
- the Adopted Programme 3 Unit 8 Scope Lock Clarification; and
- the Adopted Programme 3 Unit 9 Knowledge Retrieval contract and clarifications, whose promoted-Knowledge retrieval surface is distinct from raw Memory Core retrieval.

Gap #54's architecture and constitutional design are complete. Its live operational implementation is not. This document supplies the narrow scope authority that the adopted clarifications deliberately reserved for a later unit.

## 2. Purpose and completion boundary

This unit may do only the minimum work required for `DefaultKnowledgeCandidateEvaluator` to resolve candidate evidence through the existing permission-filtered Memory Core retrieval boundary, so that an otherwise valid Knowledge Submission candidate can reach its already-governed promotion evaluation.

Completion closes only the Memory Core retrieval authorization/promotion blocker recorded as Gap #54. It does not establish conversational recall, knowledge discoverability, Reasoning Context consumption, semantic retrieval, or any new public retrieval capability.

## 3. Inherited frozen decisions

The following are inherited decisions, not decisions made or reopened here:

1. Memory Core records are not Resource Registry resources.
2. Structurally targetless Memory Core retrieval is resolved by an explicit, closed, per-verb-phrase composition-time derivation in `DefaultPermissionPolicy`, not by a general empty-target fallback.
3. The closed action set in this unit is exactly `memory.retrieve` → `(READ, MEMORY)` and `memory.retrieve_document` → `(READ, DOCUMENT)`.
4. Permission rules may discriminate by the original verb phrase. A verb-specific rule takes precedence over a coarser `(PermissionAction, ResourceType)` rule for the same mapping.
5. Authorization Purpose is the governed dimension for distinguishing why otherwise identical requests occur. It is additive to Principal and must not encode caller or class identity.
6. One authoritative `PermissionEngine`/`DefaultPermissionPolicy`/`AuthorizationPurposeRegistry` path makes the decision.
7. Memory Core retrieval remains permission-filtered per returned or candidate record and preserves denied/not-found non-disclosure.
8. Evidence Intelligence remains fail-closed unless separate, explicit governance grants it authority.

## 4. New adoption decisions frozen by this Scope Lock

### 4.1 Closed-set no-target derivation

Implementation is authorised to extend `DefaultPermissionPolicy` with the already-adopted closed configuration:

| Proposed action | Derived resource type | Permission action |
|---|---|---|
| `memory.retrieve` | `MEMORY` | `READ` |
| `memory.retrieve_document` | `DOCUMENT` | `READ` |

The derivation applies only when `targetResources` is structurally empty and only for these two exact registered verb phrases. It must not resolve any other empty-target request. It must not create, register, infer or fabricate a `ResourceId`.

### 4.2 Verb-phrase rule discrimination

Implementation is authorised to add the adopted optional verb-phrase discriminator to the production policy-rule representation and matching algorithm. A matching rule specific to both Authorization Purpose and verb phrase is more specific than a rule omitting either dimension. A coarse rule must never override, bypass or dilute a more-specific rule intended to govern the request. Duplicate or equally specific conflicting applicable rules are ambiguity and must deny.

### 4.3 Action registration

`ParkerRuntime` may register/configure exactly:

- `memory.retrieve` with its frozen `READ`/`MEMORY` mapping; and
- `memory.retrieve_document` with its frozen `READ`/`DOCUMENT` mapping.

Registration and derivation are resolution mechanisms only. Neither confers authority. In the absence of an applicable approving rule, the result remains `DENIED`.

### 4.4 Real Authorization Purpose vocabulary

The minimum production distinction is between the governed reason for Knowledge Candidate evidence evaluation and the governed reason for Evidence Intelligence input resolution. The following real values are frozen:

| Authorization Purpose ID | Immutable meaning | Production consumer boundary |
|---|---|---|
| `knowledge-memory.candidate-evaluation` | Resolve referenced Memory Core evidence solely to evaluate a Knowledge Candidate under the governed Knowledge Submission promotion process | `DefaultKnowledgeCandidateEvaluator` |
| `evidence-intelligence.input-resolution` | Resolve Memory Core evidence as an input to the separately governed Evidence Intelligence analysis process | `EvidenceIntelligenceInputResolver` |

These names describe purposes, not Kotlin classes as authorization subjects. Renaming, moving or replacing a class must not change the meaning of either value. Both values must be registered once through the existing production `AuthorizationPurposeRegistry` with immutable descriptions. Registration of the Evidence Intelligence purpose documents and propagates its distinct reason; it does not approve retrieval for that purpose.

No other production Authorization Purpose value is authorised by this unit. `DefaultKnowledgeRevisionEvaluator`, conversational retrieval and all dormant or future consumers remain outside scope.

### 4.5 Consumer purpose propagation

The two identified consumers may be composition-bound to their respective purposes so that every `ExecutionRequest` constructed by the existing shared `PermissionFilteredMemoryRetrieval` boundary carries the appropriate `authorizationPurpose`.

The later Implementation Plan must select the narrowest Kotlin carrier shape consistent with these constraints:

- `PermissionFilteredMemoryRetrieval` remains the sole production decorator that constructs and submits the Memory Core retrieval `ExecutionRequest`;
- both consumers continue through the same underlying decorator, Permission Engine, policy and registry authority;
- purpose is supplied by an explicit, immutable, composition-time binding, never inferred from stack, class, principal, method caller or mutable metadata;
- the public Memory Core record model and the underlying `MemoryRetrieval` implementation do not become authorization authorities;
- `DefaultKnowledgeCandidateEvaluator` receives no direct `MemoryCore`, durable store or unfiltered retrieval reference; and
- no second permission-filtered implementation or alternate retrieval route may be created.

This Scope Lock freezes the semantic and composition boundary, not a method signature. The Implementation Plan must disclose any contract surface proposed to carry the binding and prove it is the smallest lawful change.

### 4.6 Production policy-content determination

This Scope Lock makes the policy-content decision that prior governance deliberately deferred. The candidate-evaluation purpose may receive exactly these approving rules:

| Verb phrase | Permission action | Resource type | Authorization Purpose | Outcome | Level |
|---|---|---|---|---|---|
| `memory.retrieve` | `READ` | `MEMORY` | `knowledge-memory.candidate-evaluation` | `APPROVED` | `AUTOMATIC` |
| `memory.retrieve_document` | `READ` | `DOCUMENT` | `knowledge-memory.candidate-evaluation` | `APPROVED` | `AUTOMATIC` |

`AUTOMATIC` is bounded to internal evidence resolution after the separately governed `knowledge.submit` admission decision; it does not approve submission, promotion, persistence or any owner-facing retrieval act. Candidate evaluation is deterministic internal evaluation and cannot pause for an interactive confirmation through its current contract.

No approving `memory.retrieve` or `memory.retrieve_document` rule is authorised for:

- an absent purpose;
- `evidence-intelligence.input-resolution`;
- an unregistered, retired or mismatched purpose;
- any other present or future purpose; or
- a coarse purpose-agnostic rule.

Evidence Intelligence therefore continues to receive `DENIED` for Memory Core retrieval under this unit. Its real purpose value makes the distinction auditable; it is not a grant.

## 5. Permitted production components

Only the following production areas may change, and only for the responsibilities above:

- `DefaultPermissionPolicy` and its rule data shape: closed derivation, verb/purpose specificity, precedence and ambiguity denial;
- `PermissionFilteredMemoryRetrieval`: carry an explicit composition-bound purpose into its existing `ExecutionRequest` construction;
- the narrow contract or composition adapter strictly necessary to bind purpose at the shared decorator boundary, if the Implementation Plan proves it necessary;
- `DefaultKnowledgeCandidateEvaluator`: accept/use only its purpose-bound permission-filtered retrieval dependency, without direct Memory Core access;
- `EvidenceIntelligenceInputResolver`: accept/use only its separately purpose-bound permission-filtered retrieval dependency, with no approving retrieval rule;
- `ParkerRuntime`: register the two actions, configure the two closed derivations, register the two purposes, add the two candidate-evaluation rules, and wire the two purpose bindings through the existing single authority graph; and
- KDoc directly made inaccurate by those exact changes.

Associated tests may be added or changed only under a later accepted Implementation Plan.

## 6. Production components that must not change

This unit must not change:

- Memory Core record types, identifiers, persistence, durability or lifecycle;
- `ResourceRegistry` contracts or production Memory Core Resource registration;
- `PermissionEngine` or `PermissionDecision` public contracts;
- Principal, Identity Service or delegation semantics;
- the governed meaning or lifecycle rules of `AuthorizationPurposeId` or `AuthorizationPurposeRegistry`;
- Knowledge Candidate promotion criteria, explicit-owner-instruction exception, provenance, confidence or persistence behavior;
- `DefaultKnowledgeSubmission`'s own `knowledge.submit` admission gate;
- Evidence Intelligence analysis, invocation gate, acceptance coordinator or result contracts;
- Programme 3 Unit 9 Knowledge Retrieval contracts or implementation;
- Knowledge Store discoverability, Reasoning Context or conversational composition; or
- OCR code or tests.

## 7. Fail-closed incremental requirements

Every independently testable and reviewable implementation state must remain fail-closed:

1. Closed derivation without an applicable approving rule denies.
2. Action registration without an applicable approving rule denies.
3. Purpose propagation before its applicable rule exists denies.
4. An absent, unregistered, retired or mismatched purpose cannot ground purpose-specific authority.
5. Conflicting equally specific rules deny.
6. A coarse rule cannot override a more-specific purpose/verb denial or grant authority withheld at the more-specific tier.
7. Evidence Intelligence remains denied before, during and after the unit.
8. Failure to resolve an action, derivation, registry entry or policy match denies without falling back to unfiltered retrieval.
9. No intermediate commit may temporarily add a global approval, bypass or direct-memory dependency.

These requirements must be reflected in implementation-unit ordering and tests; implementation convenience is not authority to create a transient permissive state.

## 8. Per-record authorization and non-disclosure

Each direct lookup and each record returned by a query-shaped Memory Retrieval operation remains subject to its own Permission Engine evaluation at the existing decorator boundary. This unit may not replace that filtering with one approval for a collection, session, submission or whole retrieval act. A denied record remains observationally indistinguishable from an absent record through the existing nullable/filtering contract.

## 9. Single authorization path

The only authorised decision path remains:

```text
ExecutionRequest
    → DefaultPermissionEngine
    → DefaultPermissionPolicy
    → AuthorizationPurposeRegistry
    → PermissionDecision
```

Purpose binding supplies request data to this path; it does not decide. No consumer, adapter or retrieval decorator may synthesize approval, skip evaluation, inspect a caller name, or substitute another engine, policy or registry.

## 10. Dependencies and sequencing

Implementation depends on:

1. explicit acceptance of this Scope Lock following its Independent Constitutional Review;
2. a separate Gap #54 Operationalisation Implementation Plan;
3. an Independent Planning Review before implementation begins; and
4. accepted Authorization Purpose Units 1–6 remaining unchanged as the mechanism baseline.

The Implementation Plan must order work so resolution mechanisms and vocabulary adoption remain denied until the final, specific policy content and consumer bindings are present and tested.

## 11. Explicit non-goals and prohibited shortcuts

This Scope Lock does not authorise:

- redesigning or reopening Gap #54 architecture;
- another Permission Engine, policy or registry;
- fake Resource Registry entries for Memory Core records;
- direct or unfiltered Memory Core access from candidate evaluation;
- principal identity, delegation or a `system.*` principal as consumer discrimination;
- caller-class, caller-name, stack or reflection conditionals;
- a global or purpose-agnostic `memory.retrieve` approval;
- bypassing `PermissionFilteredMemoryRetrieval`;
- Programme 3 Unit 9 Knowledge Retrieval redesign;
- Knowledge discoverability changes;
- Reasoning Context integration;
- conversational or case-work retrieval;
- semantic/vector search;
- KnowledgeItem persistence or durability changes;
- retirement or redesign of the `system.*` convention beyond not using it as purpose;
- Evidence Intelligence retrieval authority;
- any other existing or future Memory Core consumer retrofit; or
- unrelated OCR portability work.

## 12. Acceptance criteria

The implementation may be accepted only if targeted verification proves all of the following through real production implementations and the composed runtime:

1. Only the two exact targetless actions receive the frozen derivations.
2. Non-targetless and unknown-action behavior is unchanged.
3. The two action registrations confer no authority by themselves.
4. Verb-specific rules are distinguishable from coarse rules.
5. Purpose-and-verb-specific rules govern over every applicable coarser rule independent of list order.
6. Equal-specificity ambiguity denies.
7. Both real purposes are registered, active and auditable with immutable meanings.
8. Candidate evaluation requests carry only `knowledge-memory.candidate-evaluation`.
9. Evidence Intelligence requests carry only `evidence-intelligence.input-resolution`.
10. Candidate evidence of every supported direct-lookup record kind can be resolved only when all action, resource derivation, active-purpose and rule conditions match.
11. Both candidate rules return `APPROVED`/`AUTOMATIC` only for the candidate-evaluation purpose.
12. Evidence Intelligence remains denied even for a record that exists and even in the presence of an otherwise matching coarse approval challenge.
13. Absent, unregistered, retired and mismatched purposes deny where the candidate-specific authority is required.
14. Per-record checks and denied/not-found non-disclosure are preserved.
15. One shared underlying decorator, engine, policy and registry path remains authoritative.
16. No production consumer receives direct or unfiltered Memory Core access.
17. Existing unrelated callers and Permission Policy outcomes do not regress.
18. No conversational retrieval, discoverability or Reasoning Context behavior is introduced.

Targeted tests, full-suite evidence, a Completion Review and a genuine Independent Constitutional Review are required. Environmental failures unrelated to this unit must be classified, not silently repaired within it.

## 13. Completion statement

Successful completion means only that the governed candidate-evidence retrieval path can pass its existing evidence into Knowledge Candidate evaluation without granting the same Memory Core read authority to Evidence Intelligence or any other consumer.

It does not mean Parker can discover or conversationally recall promoted knowledge. The separately identified Knowledge discoverability mismatch and Reasoning Context consumption gap remain later governed work.

## 14. Scope Lock disposition

This Scope Lock is ready for independent constitutional review. It grants no implementation authority until that review is accepted and explicit approval is given to create the later Implementation Plan.
