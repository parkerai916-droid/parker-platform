**Status:** Independent Constitutional Review of Implementation Unit 2. This review is limited to the new governed reasoning-knowledge source and its test evidence; no production composition or later unit is accepted here.

# Knowledge Discoverability and Reasoning Context Implementation Unit 2 - Independent Constitutional Review

## 1. Reviewed evidence

```text
base=89f3049c522f0c68da78a8ac53dbb6a73b6b0867
unit=2f3ac3bbd2eab973f59bf6ceef431078c12f4b1d
```

The review independently inspected:

- the complete two-file unit diff;
- the production implementation and all 39 tests;
- Contract Design Sections 4, 5, 7, 9, and 13;
- Scope Lock Sections 5, 6, and 11, plus inherited stop conditions;
- Implementation Plan Section 7 and its exact proof allocation;
- excluded-call, file-boundary, and later-unit absence checks;
- successful full-suite evidence from the Parker server and an independent Windows run at the corrected commit.

## 2. Authority and capability boundary

The new source is read-only and purpose-bound. It accepts an already-resolved Authorization Purpose and a purpose-bound `MemoryRetrieval`; it registers no Purpose, Resource, vocabulary entry, or permission rule. It exposes no raw Memory Core handle and performs no write, admission, promotion, or lifecycle transition.

Act authorization occurs before persistence inspection. Item authorization occurs before evidence dereference. Only approved candidates can reach `getAssertion` or `getEntity`. `ToDocument` and `ToRelationship` are structurally unreachable from any Memory Core method, and the implementation has no Document action, Document authority, or alternative evidence path.

The implementation does not widen Evidence Intelligence, Programme 3, generic `KnowledgeRetrieval`, or the legacy `KnowledgeSource` surface. It creates the distinct `ReasoningKnowledgeSource` implementation already fixed by the accepted governance chain.

## 3. Information integrity and nondisclosure

Returned content originates only from an authorized, resolved, `ACTIVE` Assertion or Entity. Matching uses that content rather than a Knowledge Item's generic promotion-basis text. No missing, denied, deleted, non-active, or unsupported evidence is fabricated into a result.

Denied and missing evidence are independently proven rather than represented by the same fixture state. Existing-but-denied records contain unique protected text, the exact evidence identifier is observed at the controlled boundary, and no protected content reaches any result. Missing records are absent from both allowed and denied collections while their exact identifiers are still observed. This establishes the uniform silent-exclusion rule without conflating denial with absence.

The authorized-partial proof uses an allowed candidate, a genuine denied-evidence candidate, and a later allowed survivor. It proves both silent exclusion and surviving insertion order. The item-level proofs separately show that denied candidates are never dereferenced.

These proofs, together with the separate unsupported-kind and record-status tests, fully discharge Contract Design Invariant 7 and Scope Lock Section 11 for Unit 2.

## 4. Determinism, failure, and regression challenge

Normalization, Entity assembly, record-status gating, ordering, staleness disclosure, and final bounding follow the frozen design without adding policy discretion. The implementation catches no dependency exception; failures propagate unchanged. Authorization denial and evidence-resolution failure remain candidate-safe and do not expose a denial detail or fabricate content.

The test suite expressly challenges the original false-match defect by placing the query term in generic promotion history while omitting it from resolved evidence content. Exclusion confirms the implementation does not repeat `DefaultKnowledgeRetrieval.matches()` basis-text behavior.

The full Gradle suite passes at the immutable corrected commit. No pre-existing file is modified, so the unit cannot conceal a composition repair, policy change, or later-unit dependency.

## 5. Stop-condition challenge

No inherited or Unit 2 stop condition is triggered:

- the diff is limited to the two authorized new files;
- the constructor has exactly five dependencies;
- the ten-step order is preserved;
- every mandatory proof remains a distinct test;
- denied and missing evidence are not conflated;
- only Assertion and Entity dereference are reachable;
- no Document authority or excluded retrieval call exists;
- no registry, permission-policy, assembler, runtime-composition, or prompt-rendering change is present;
- Unit 3 has not begun;
- the full suite passes.

## 6. Findings and verdict

```text
P0=0
P1=0
P2=0
P3=0
VERDICT=ACCEPTED
```

Implementation Unit 2 at `2f3ac3bbd2eab973f59bf6ceef431078c12f4b1d` is constitutionally aligned with the accepted Contract Design, Scope Lock, and Implementation Plan.

Acceptance is limited to this unit's new implementation and tests. It does not establish production reachability, Reasoning Context delivery, conversational recall, live-model behavior, restart durability, durable auditing, programme completion, or closure. Unit 3 remains prohibited until both Unit 2 reviews are accepted and merged.
