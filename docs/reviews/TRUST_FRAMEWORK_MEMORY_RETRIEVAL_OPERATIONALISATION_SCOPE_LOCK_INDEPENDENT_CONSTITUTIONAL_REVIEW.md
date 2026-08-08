**Status:** Genuine Independent Constitutional Review of the proposed Gap #54 Memory Retrieval Operationalisation Scope Lock. Review only. The Scope Lock was treated as a proposition to challenge against primary authority, not as authority for its own validity. No Kotlin or tests are modified or authorised by this review. Nothing is staged, committed, or pushed.

# Trust Framework — Gap #54 Memory Retrieval Operationalisation Scope Lock — Independent Constitutional Review

## 1. Review baseline and authorities

Reviewed on `main` at `334d709` against the Parker Constitution, Trust Framework and Permission Engine principles; the Memory Retrieval blocker, Contract Design and both Adopted Clarifications; the Authorization Context Contract Design; Authorization Purpose Programme, Scope Lock, Implementation Plan and accepted Units 1–6; the Trust Framework Implementation Sequence; relevant Programme 3 Unit 8 and Unit 9 governance; and the current production implementations of `DefaultPermissionPolicy`, `PermissionFilteredMemoryRetrieval`, `DefaultKnowledgeCandidateEvaluator`, `EvidenceIntelligenceInputResolver` and `ParkerRuntime`.

The question is not whether operationalisation is desirable. It is whether this Scope Lock lawfully translates settled architecture into a bounded implementable unit while making only the policy and adoption decisions that prior governance explicitly deferred.

## 2. Challenge: does the Scope Lock reopen settled Gap #54 architecture?

It does not. Sections 3 and 4 preserve the adopted closed per-verb derivation and verb-discriminator selections verbatim in substance. The Scope Lock rejects fake resources, a general empty-target fallback, alternative engines and direct-memory exceptions. It selects no competing mechanism.

Its new decisions are correctly identified as adoption decisions: the two production Purpose values, their consumer binding, action registration and exact purpose-specific policy rules. These are the decisions that Authorization Purpose Implementation Plan §10 and Memory Retrieval Contract Design §22 left for later governance.

**Finding:** no reopening or redesign; scope is constitutionally derivative.

## 3. Challenge: is policy-content authority sufficient?

The Scope Lock does not merely say candidate retrieval “may be approved.” It fixes both actionable rules: exact verb, permission action, derived resource type, active Authorization Purpose, outcome and level. It prohibits coarse and Evidence Intelligence approvals and states the preconditions under which the rules apply.

Prior governance did not require a separate Contract Design for this later policy-content choice. It required the Authorization Purpose dimension to exist evaluably and a later explicitly reasoned decision. Units 1–6 satisfy the prerequisite; this dedicated Scope Lock supplies the explicit decision. A second governance artifact would add sequence without resolving an unasked architectural question.

`APPROVED`/`AUTOMATIC` is constitutionally supportable because this is internal, per-record evidence resolution within an already permission-admitted Knowledge Submission operation. It grants neither submission nor promotion. The synchronous evaluator contract has no confirmation continuation, so `APPROVED_WITH_CONFIRMATION` would not operationalize the blocker and would disguise an unusable decision as authority.

**Finding:** policy-content authority is precise and sufficient within this Scope Lock; no separate policy-content artifact is required.

## 4. Challenge: do the Purpose values represent purpose rather than caller identity?

`knowledge-memory.candidate-evaluation` names the governed reason for resolving evidence: evaluating a Knowledge Candidate. `evidence-intelligence.input-resolution` names the governed reason: resolving input to an Evidence Intelligence analysis. Neither value names a Kotlin class, method, instance or principal. The Scope Lock explicitly makes their meanings invariant under implementation renaming and forbids runtime inference from caller identity.

The production boundary necessarily identifies which governed consumer supplies each value, but association with a consumer does not turn a purpose into raw caller identity. The policy matches the declared registered reason carried in `ExecutionRequest`, not a class check.

**Finding:** both values satisfy the accepted Authorization Purpose model.

## 5. Challenge: is Evidence Intelligence protected from widening?

Yes. Its purpose is registered and propagated for auditability, but receives no approving Memory Core retrieval rule. Candidate approval requires the distinct candidate-evaluation purpose. The Scope Lock additionally requires an adversarial coarse-rule test and freezes denial before, during and after implementation.

Registering the Evidence Intelligence purpose is not authority: accepted registry governance makes eligibility necessary but not sufficient. This distinction is stated repeatedly and is testable.

**Finding:** Evidence Intelligence remains explicitly fail-closed and cannot inherit candidate authority merely by sharing the decorator.

## 6. Challenge: can coarse Permission Policy rules defeat the distinction?

The proposed precedence is stronger than simple purpose matching: purpose-and-verb-specific rules govern over rules omitting either dimension, independent of list order, and equal-specificity conflict denies. The Scope Lock prohibits any new purpose-agnostic approval and requires a test in the presence of an otherwise matching coarse approval challenge.

One subtlety was examined: accepted Authorization Purpose Units 4–6 preserve unrelated pre-existing coarse authority where no effective purpose-specific rule applies. Gap #54 cannot rely on that general fallback because its new authority is expressly purpose-specific. The Scope Lock resolves the local risk by requiring the active candidate purpose and forbidding a coarse Gap #54 approval; it does not unlawfully rewrite all pre-existing policy semantics.

**Finding:** the intended distinction cannot lawfully be defeated by a coarse rule under the locked mechanism.

## 7. Challenge: are incremental states fail-closed?

Section 7 covers each partial state: derivation alone, registration alone, propagation alone, missing/inactive/mismatched purpose, ambiguous rules and resolution failure. It also prohibits a temporarily permissive intermediate commit. Because only the final purpose-and-verb-specific rules grant authority, all preceding states remain denied.

The implementation sequence remains for the later plan, appropriately: a Scope Lock should freeze the invariant, not pretend to be the implementation plan it explicitly excludes.

**Finding:** every incremental implementation state is required to remain fail-closed.

## 8. Challenge: does the Scope Lock authorize conversational retrieval or Reasoning Context work?

No. The purpose, permitted components, non-goals, acceptance criteria and completion statement all confine success to candidate evidence reaching existing promotion evaluation. Knowledge discoverability, conversational and case-work retrieval, Reasoning Context, semantic search and Unit 9 redesign are expressly excluded.

**Finding:** no adjacent retrieval programme is silently admitted.

## 9. Challenge: is an alternate authorization or retrieval path created?

No. The Scope Lock freezes the existing `ExecutionRequest → DefaultPermissionEngine → DefaultPermissionPolicy → AuthorizationPurposeRegistry → PermissionDecision` path. It permits purpose binding at composition but requires the single shared underlying `PermissionFilteredMemoryRetrieval`, engine, policy and registry authority. Direct Memory Core access, a second decorator authority and consumer-local approval are forbidden.

The present `MemoryRetrieval` interface lacks an Authorization Purpose parameter. The Scope Lock responsibly does not invent a method signature. It authorizes a narrow immutable composition-time binding and requires the Implementation Plan to prove the smallest carrier surface. This is an implementation-shape question within the accepted carrier architecture, not an unresolved constitutional design question.

**Finding:** no alternate authority is created; the remaining carrier shape is properly deferred to planning.

## 10. Challenge: is the permitted-file boundary too broad?

The list is bounded to policy resolution, the existing decorator request construction, two consumers, production composition and only an unavoidable narrow purpose-binding surface. It expressly protects Memory Core records, Resource Registry, public Permission Engine/Decision contracts, identity, promotion semantics, Evidence Intelligence behavior, Knowledge Retrieval and adjacent programmes.

The conditional permission for a narrow contract or composition adapter is necessary because the existing consumer-facing interface carries no purpose. It is constrained by a proof obligation in the later plan and cannot become a new authority.

**Finding:** the permitted production boundary is minimal but implementable.

## 11. Defect search and corrections

The review challenged the initial policy concept for two possible defects:

1. An approval expressed only for `memory.retrieve` would leave Document evidence permanently blocked. The reviewed Scope Lock correctly includes the independently governed `memory.retrieve_document` rule.
2. Leaving Evidence Intelligence without a declared purpose would preserve request indistinguishability and weaken auditability. The reviewed Scope Lock correctly registers and propagates a distinct purpose while withholding all approving rules.

No constitutional defect remains in the presented Scope Lock. No Defect Confirmation Review is required.

## 12. Constitutional verdict

```text
ACCEPTED
```

The Scope Lock is constitutionally sufficient for explicit acceptance. No corrective action is required before acceptance.

Acceptance of this review does not itself authorize Kotlin or test changes. The next permissible artifact after explicit approval is the narrow Gap #54 Memory Retrieval Operationalisation Implementation Plan and its Independent Planning Review.
