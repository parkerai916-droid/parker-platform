**Status:** Unit 4 Boundary Reassessment — PASS. Conducted after explicit acceptance of the Knowledge Candidate Accountable Principal Architecture Decision and carrier prerequisite. The original identity blocker is resolved. Unit 4 may resume from its preserved policy implementation. Nothing is staged, committed, or pushed.

# Gap #54 Memory Retrieval Operationalisation — Unit 4 Boundary Reassessment

## 1. Original blocker

The initial real-candidate test denied before policy because `DefaultKnowledgeCandidateEvaluator` substituted unregistered `system.knowledge-memory`. The Unit 4 rules themselves worked for a registered Principal but could not govern the real evaluator path.

## 2. Accepted resolution

The accepted prerequisite now carries:

```text
Principal = requestingPrincipalId supplied to KnowledgeSubmission.submit
Purpose   = knowledge-memory.candidate-evaluation
```

`DefaultKnowledgeSubmission` forwards the exact Principal that passed Evaluation B. `DefaultKnowledgeCandidateEvaluator` forwards it through every direct evidence lookup and relationship query. The immutable Unit 3 view independently supplies the candidate Purpose.

## 3. Live boundary verification

- `system.knowledge-memory` is absent from live candidate evaluation;
- it is absent from `ParkerRuntime` identity registration;
- no substitute system Principal was introduced;
- candidate evaluation remains an internal step acting for the submitting Principal;
- the unchanged `DefaultPermissionEngine` validates that real Principal before policy;
- a registered real owner reaches the two candidate rules;
- an unregistered real Principal denies at the identity gate;
- one Purpose-bound view, parent decorator, engine, policy and registry path remains authoritative; and
- Evidence Intelligence retains its own real Principal and distinct denied Purpose.

## 4. Policy boundary

The preserved production policy contains exactly the two authorized candidate Purpose-plus-verb approvals and the two unchanged Unit 2 verb-only guards. No additional Memory approval exists. The accepted specificity mechanism makes candidate rules govern only for the exact active candidate Purpose, independent of ordering; every other Purpose state remains governed by the guard or default denial.

## 5. Reassessment verdict

```text
PASS — ORIGINAL BLOCKER RESOLVED
```

Unit 4 may resume for missing verification and formal Completion/Constitutional Review only. The prerequisite must not be redone and Unit 5 remains prohibited.
