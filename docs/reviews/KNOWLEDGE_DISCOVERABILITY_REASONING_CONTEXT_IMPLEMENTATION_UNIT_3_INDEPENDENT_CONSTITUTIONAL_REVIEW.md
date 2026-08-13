**Status:** Independent Constitutional Review of Implementation Unit 3. This review is limited to the accepted atomic production cutover, assembler behavior, and the four compatibility corrections. It grants no authority to begin or accept Unit 4.

# Knowledge Discoverability and Reasoning Context Implementation Unit 3 - Independent Constitutional Review

## 1. Reviewed evidence

```text
base=77d007f92a1679e12b6e1004f90f026c06907878
unit=b35da869907e4866a6ed30ac4b2109f643ba0990
```

The review independently inspected:

- the complete five-file diff;
- the assembler constructor, query, rendering, and escaping changes;
- the sole production caller and the full permission/resource/vocabulary composition;
- all replacement assembler tests and the genuine assembler-level promotion proof;
- all changes in the two compatibility composition tests;
- Contract Design Sections 7, 8, and 12;
- corrected Scope Lock Sections 4, 6, 7, and 9;
- corrected Implementation Plan Section 8 and inherited stop conditions;
- successful focused and full-suite evidence from the Parker server and an independent Windows full-suite run.

## 2. Atomicity and single-feed boundary

The assembler's dependency type and the sole production call site change in one commit. The composition root no longer constructs the legacy in-memory Knowledge Store for Reasoning Context, and the assembler has no legacy memory contract reference. No dual knowledge feed is active.

The new source is built only after its real dependencies exist and is supplied directly to the assembler. The same promoted-item persistence and permission-engine instances serve generic Knowledge Retrieval and Reasoning Context retrieval; no parallel persistence or authorization state is introduced.

Conversation History, Identity, Tools, and World Model remain separate inputs with their existing behavior and ordering. This unit changes only the knowledge feed.

## 3. Least authority

The new verb is fail-closed without the exact active Purpose. Two Purpose-bound approvals permit only the reachable operations: governed Reasoning Context retrieval and Assertion/Entity Memory retrieval. No Purpose-bound Document approval exists.

The pre-existing `memory.retrieve_document` denial guard remains applicable. The pre-existing `memory.retrieve` guard, candidate-evaluation approvals, Evidence Intelligence isolation, coarse policy rules, Resource, and unrelated vocabulary entries are unchanged.

The independently audited production graph therefore grants no authority beyond the frozen algorithm. The purpose-bound evidence view is not exposed through a public accessor or raw capability.

## 4. Information integrity and prompt safety

The assembler receives only `SafeKnowledgeResultEntry` values. It neither retrieves raw Memory Core records nor mutates knowledge. It preserves entry order and renders all four fixed fields without inference or omission.

The exact escaping boundary prevents authorized content from creating a second prompt entry or structural line. Tests cover all required control ranges and Unicode separators. Raw safe-result content remains unescaped until this boundary, preserving Unit 2 matching semantics.

The genuine assembler-level proof uses real admission/promotion, shared persistence, real governed recall, and real assembly. It is an assembler proof only and does not substitute for Unit 4 production-composition verification or Unit 5 runtime conversational recall.

## 5. Gap #54 compatibility challenge

The two existing composition suites contained four closed-world assertions that became stale when the already-governed third Purpose and rules were composed. Their amendments remain exact rather than permissive:

- exact Purpose sets grow from two to three;
- exact Purpose-bound rule count grows from two to four with explicit partitioning;
- the Memory verb partition grows from four to five by one exact reasoning-context approval;
- all earlier denial, ambiguity, candidate approval, Evidence Intelligence isolation, and least-authority assertions remain.

No production behavior was changed to satisfy stale tests, and no Unit 4 proof was moved into the compatibility suites.

## 6. Stop-condition challenge

No inherited or corrected Unit 3 stop condition is triggered:

- exactly five authorized files changed;
- constructor and caller changed atomically;
- exact legacy symbols are absent from assembler and test;
- non-memory behavior is preserved;
- exactly three new rules exist and only two carry the new Purpose;
- no Document authority exists;
- the original Gap #54 guarantees remain strict;
- exactly four compatibility methods changed;
- no Unit 4 or Unit 5 work began;
- focused and full suites pass.

## 7. Findings and verdict

```text
P0=0
P1=0
P2=0
P3=0
VERDICT=ACCEPTED
```

Implementation Unit 3 at `b35da869907e4866a6ed30ac4b2109f643ba0990` is constitutionally aligned with the corrected Scope Lock, corrected Implementation Plan, and unchanged Contract Design.

Acceptance is limited to Unit 3. It does not establish the separately required Unit 4 composition proof, Unit 5 genuine runtime recall, live-model behavior, restart durability, durable auditing, programme completion, or closure. Unit 4 remains prohibited until both Unit 3 reviews are accepted and merged.
