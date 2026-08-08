**Status:** Genuine Independent Constitutional Review of the proposed Knowledge Candidate Accountable Principal Architecture Decision. Governance review only. The proposed Decision was treated as a proposition to challenge against primary authority, not as authority for its own validity. No Kotlin or tests are modified or authorized merely by this review. Nothing is staged, committed, or pushed.

# Knowledge Candidate Accountable Principal Architecture Decision — Independent Constitutional Review

## 1. Independent reconstruction

The review independently read the Principal and Identity Service contracts/implementations; Chapters 41–42; `IdentityService.md`; `DefaultPermissionEngine`; Memory Core requesting-Principal rules; Authorization Context and Authorization Purpose governance and accepted reviews; Programme 3 candidate, evaluator and submission contracts/governance; `MemoryAdmissionCoordinator`; Evidence Intelligence acceptance/input resolution; `DefaultKnowledgeSubmission`; both Knowledge evaluators; `PermissionFilteredMemoryRetrieval`; production composition; and every repository occurrence of `system.knowledge-memory` and `system.evidence-intelligence`.

The Completion/Boundary conclusions from paused Unit 4 were evidence only. The constitutional question was re-derived from the current contracts and call graph.

## 2. Challenge: who is genuinely accountable?

The real producer/invoker Principal is explicit throughout each live path. Conversational admission receives the owner Principal and passes it unchanged to Memory writes and `KnowledgeSubmission.submit`. Evidence Intelligence acceptance receives its invocation Principal and passes it unchanged to submission. The public Knowledge Submission contract requires every other producer to do the same.

`DefaultKnowledgeSubmission` uses this Principal for its own mandatory permission gate, so it is not incidental metadata: it is the already-validated actor accountable for the submission. Candidate evaluation occurs only after that gate and exists solely to process the submitted candidate. It initiates no autonomous work and has no independent lifecycle or entry point.

**Finding:** the submission Principal, not the evaluator component, is the accountable actor for evidence resolution.

## 3. Challenge: is the Principal lost earlier than claimed?

No. `KnowledgeCandidate` deliberately carries no Principal, but the same call carries Principal as the separate leading argument to `KnowledgeSubmission.submit`. The value remains live in `DefaultKnowledgeSubmission` until it invokes the evaluator. The precise loss is the candidate-only `KnowledgeCandidateEvaluator.evaluate` signature.

The Decision does not mistake evidence provenance for request accountability. The submitting Principal is accountable for requesting evaluation even where the referenced evidence originated elsewhere; existing provenance remains evidence attribution, not permission identity.

**Finding:** one loss point is accurately identified.

## 4. Challenge: is changing the evaluator contract minimal?

Yes. Adding Principal to `KnowledgeCandidate` would reverse Unit 8's explicit separate-parameter design. Constructor binding would make a shared evaluator Principal-specific and fail across callers. Changing `MemoryRetrieval` is unnecessary because all methods already carry Principal. Ambient context is forbidden. Passing the parameter from the object that already holds it into the immediate collaborator is the smallest explicit carrier.

The public interface change is material, but the Decision does not hide that fact. It places it in a separate prerequisite unit with exact files and review gates rather than smuggling it into Unit 4.

**Finding:** smallest lawful carrier; contract impact honestly governed.

## 5. Challenge: can the carrier be spoofed or mutated?

The evaluator receives a `PrincipalId` value, not a mutable Principal record or registry capability. It cannot activate, translate or alter identity. A caller could already choose the explicit Principal supplied to the public Knowledge Submission boundary; the existing `DefaultPermissionEngine` validates that identity before evaluation. The amendment creates no new externally reachable choice and forwards only the value that passed that gate.

Implementation tests must prove exact reference/value propagation and no fallback. A mismatch cannot be silently repaired by the evaluator; unknown or inactive identity continues to deny at the unchanged engine.

**Finding:** no new spoofing or mutation surface beyond the already-governed explicit submission parameter.

## 6. Challenge: does Authorization Purpose remain separate?

Yes. Principal enters each `MemoryRetrieval` method/query. Purpose remains fixed in the immutable composition-bound candidate view. Neither is derived from the other, and the evaluator cannot select the Purpose. Approval still requires active identity plus active candidate Purpose plus exact verb/action/type plus the unique applicable rule.

**Finding:** additive Principal/Purpose model preserved.

## 7. Challenge: is identity substitution reintroduced?

No. The Decision removes `system.knowledge-memory` from live candidate evaluation and forbids fallback. It creates or registers no replacement subsystem identity. The evaluator becomes transparent to accountable Principal rather than being assigned an identity of its own.

This follows accepted Authorization Context governance rather than contradicting it: internal services are not anonymous when they are autonomous actors, but an internal processing step must not displace the real actor merely to identify which component executes the step.

**Finding:** fixed identity substitution is retired on the live path.

## 8. Challenge: is `system.knowledge-memory` retained without necessity?

It is not retained in the live candidate path and receives no registration, type, lifecycle or authority. The dormant revision evaluator remains excluded because its accountable-Principal carrier is a distinct future invocation question and it is not composed. The Decision explicitly blocks future composition without governance.

Removing the dormant constant now would be unrelated cleanup with no live constitutional effect and would broaden the prerequisite unit. Leaving it unreachable but disclosed is an acceptable temporary qualification of repository state, not acceptance or authority.

**Finding:** no live retention; dormant issue bounded for future governance.

## 9. Challenge: can a new Principal inherit coarse authority?

No Principal is created, registered or activated. The carrier uses a Principal already supplied to and validated by Knowledge Submission. The request may reach only whatever authority that real Principal lawfully obtains under the full policy dimensions. Unit 4's exact Memory verbs remain guarded absent the exact candidate Purpose.

The Decision therefore avoids the prior registration proposal's central defect: exposing a newly active root identity to purpose-agnostic coarse approvals.

**Finding:** no new coarse-policy eligibility is created.

## 10. Challenge: is Evidence Intelligence widened?

No. Evidence Intelligence already propagates its real requesting Principal. Its immutable Purpose remains `evidence-intelligence.input-resolution`, with no approving rule. Candidates produced by its acceptance coordinator lawfully use that same real Principal at Knowledge Submission, but candidate evaluation's separate Purpose is composition-bound to the evaluator retrieval dependency. This does not grant Evidence Intelligence input-resolution authority; it authorizes the distinct later act of evaluating a candidate after the submission gate.

**Finding:** identities remain accountable and purposes remain act-specific; Evidence Intelligence retrieval stays denied.

## 11. Challenge: is Unit 4 broadened or redesigned?

No. The Decision classifies the carrier as a prerequisite unit because its three production files lie outside Unit 4's one-file policy-content boundary. The two candidate policy rules remain unchanged and are neither weakened nor expanded. Unit 4 cannot resume until the prerequisite has its own implementation, tests, Completion Review and Independent Constitutional Review.

No Unit 5 promotion-closure behavior is authorized. The conversational composition suite is regression evidence only during the prerequisite.

**Finding:** correct sequencing; no Unit 4 or Unit 5 scope laundering.

## 12. Challenge: hidden context or alternate authority

The Decision expressly prohibits globals, thread-locals, reflection, stack inspection, caller inference, metadata, fixed fallback and identity/Purpose derivation. The evaluator continues through the same purpose-bound view, one parent decorator, one engine, one Identity Service, one policy and one registry. It gains no direct Memory Core or authority dependency.

**Finding:** explicit carrier and single authority path preserved.

## 13. Defect search and corrective action

The review challenged two possible defects:

1. **Should Principal be added to `KnowledgeCandidate` so it cannot diverge from its origin?** No. The candidate can lawfully be submitted by a Principal other than the evidence creator; provenance answers evidence origin, while the explicit submission parameter answers accountability for this evaluation request. Embedding identity in the payload would conflate them and contradict frozen Unit 8 governance.
2. **Should dormant revision evaluation be changed simultaneously?** No. It has no composed caller whose accountable Principal can be traced. Changing it now would invent a carrier without an invocation context and broaden the unit.

No constitutional or planning defect remains. No correction or Defect Confirmation Review is required.

## 14. Constitutional verdict

```text
ACCEPTED
```

No corrective action is required before explicit acceptance of the Architecture Decision.

Acceptance authorizes planning and implementation of the separately bounded accountable-Principal carrier prerequisite only. It does not authorize resuming Unit 4 in the same step, modifying its policy rules, beginning Unit 5, or registering `system.knowledge-memory`.
