**Status:** Unit 5 Planning Review — PASS. Conducted against baseline `1cf3659`, the accepted Operationalisation Scope Lock and amended Implementation Plan, accepted Units 1–4, and the accepted accountable-Principal architecture decision and prerequisite before Unit 5 test changes. No Boundary Review is required. Nothing is staged, committed, or pushed.

# Gap #54 Memory Retrieval Operationalisation — Unit 5 Real Composed Acceptance and Persisted Promotion — Planning Review

## 1. Authority and boundary

Unit 5 is verification and acceptance closure only. The accepted implementation already contains the complete candidate-evidence authority path. This unit may replace the obsolete continued-failure assertion in `ParkerRuntimeConversationalMemoryAdmissionCompositionTest.kt` with stronger real-composition proof that an explicit owner Remember instruction creates governed Memory Core evidence and a promoted `KnowledgeItem` in the authoritative production-composed persistence instance.

No production Kotlin change is expected or authorized. Conversational recall, proposition discoverability, Reasoning Context consumption, semantic retrieval, case-work retrieval, and persistence redesign remain excluded.

## 2. Fresh production trace

`ParkerRuntime.submitOwnerMessage` routes a real owner message through the communication and reasoning pipeline. A parsed `ReasoningProviderResponse.Remember` reaches `ConversationReplyCoordinator`, which calls the one composed `MemoryAdmissionCoordinator` with the message Principal, correlation identifier, and parsed proposition.

The admission coordinator gates its own write, creates durable Memory Core provenance and assertion, constructs a `KnowledgeCandidate` with that assertion reference and `soleBasisIsExplicitInstruction = true`, and calls the one composed `DefaultKnowledgeSubmission`. Submission gates, forwards the same accountable Principal to `DefaultKnowledgeCandidateEvaluator`, and stores the returned item only on `Promote`.

The evaluator receives the immutable candidate-purpose-bound view of the one shared `PermissionFilteredMemoryRetrieval`. That parent constructs the requests evaluated by the one `DefaultPermissionEngine`, policy, and Authorization Purpose registry. The two Unit 4 candidate rules authorize only the exact active `knowledge-memory.candidate-evaluation` Purpose and verbs. Evidence Intelligence retains its distinct denied Purpose.

`ParkerRuntime` constructs one long-lived `InMemoryKnowledgeItemPersistence` and supplies that exact instance to both `DefaultKnowledgeSubmission` and the composed `DefaultKnowledgeRetrieval`. Its `findAll` observation seam can prove persistence after the conversational call returns without invoking or redesigning Knowledge Retrieval matching. The Memory Core assertion is separately retrievable from the one composed `DurableMemoryCore`, and its durability log remains the authoritative file-backed Memory Core mechanism.

## 3. Authorized test change

Only `tests/composition/ParkerRuntimeConversationalMemoryAdmissionCompositionTest.kt` needs modification. The obsolete test that requires Remember to fail will instead prove:

1. a deterministic synthetic owner request explicitly asks Parker to remember `My test coffee mug is black.`;
2. the real model response is classified as `Remember` and the real composed path replies `I'll remember that.`;
3. exactly one promoted item exists in the same persistence used by production submission and retrieval;
4. the item references an actual assertion in the composed durable Memory Core;
5. that assertion contains the parsed proposition and its provenance identifies the original conversation correlation;
6. the promotion history references the same assertion and discloses the explicit-owner-instruction exception;
7. the item remains observable after the complete conversational call, rather than only as an evaluator-local value; and
8. a real admission attempt for an unregistered Principal does not add a persisted item.

The ordinary reply regression remains. Unit 4's genuine `CONTRADICTS` relationship test remains unchanged and is included in targeted verification.

## 4. Durability semantics

The Memory Core provenance and assertion are appended through `DurableMemoryCore` to the configured file-backed durability log. The promoted `KnowledgeItem` is stored in the single mutex-protected `InMemoryKnowledgeItemPersistence` owned by the running `ParkerRuntime`. Therefore Unit 5 can prove durable Memory Core evidence across recovery semantics already accepted by the durability programme, and Knowledge persistence for the lifetime of the current runtime repository. It must not claim that the in-memory Knowledge Item survives process/runtime reconstruction.

## 5. Fail-closed verification

The targeted set will retain the accepted matrices proving absent, wrong, unregistered and retired Purpose denial; unregistered Principal denial; exact Unit 2 guards; candidate-only approval; order independence and ambiguity denial; one authority graph; and genuine Evidence Intelligence denial. The new negative admission assertion proves denial cannot persist Knowledge.

No test-only engine, policy, registry, raw retrieval route, production authority, or failure hook will be introduced.

## 6. Boundary Review determination

No Boundary Review is required. Existing composition fields and the established reflection precedent expose the authoritative persistence and Memory Core objects to tests. Existing contracts expose every assertion, provenance, evidence-reference, promotion-history, and persistence fact required for acceptance.

If the narrow test reveals that persistence did not occur or that production modification is required, implementation must stop and a Boundary Review must classify the defect rather than changing production in Unit 5.

## 7. Verification plan

Run the mandated targeted composition, admission, policy, decorator, evaluator, submission, durability, and persistence suites; then run the full repository suite. The historical conversational failure must disappear. The known Windows OCR separator failure may remain only as an unrelated portability qualification.

After verification, create a Unit 5 Completion Review, an independent constitutional review, and a programme closure determination. A Defect Confirmation Review is required only if a correctable Unit 5 defect is found.

## 8. Planning verdict

```text
PASS
```

Unit 5 may proceed within the single-test-file verification boundary. No production change and no conversational recall claim are authorized.
