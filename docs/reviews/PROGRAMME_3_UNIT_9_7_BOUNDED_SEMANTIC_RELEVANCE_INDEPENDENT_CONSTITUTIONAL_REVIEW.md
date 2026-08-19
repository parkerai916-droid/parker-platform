**Status: ACCEPT — PROGRAMME MAY CLOSE.** This is the Independent Constitutional Closure Review for Programme 3, Unit 9.7 ("Bounded Semantic Relevance"), conducted during Unit 9.7.6 ("Full Verification, Regression, and Programme Closure") after genuine Windows executable verification. It reviews Unit 9.7 as a completed whole against its own authoritative governance, adversarially, and independently of the Completion Review's own narrative summary.

# Programme 3, Unit 9.7 — Bounded Semantic Relevance — Independent Constitutional Review

Repository: `parkerai916-droid/parker-platform`. Branch `experiment/qmd-canonical-memory-retrieval`. HEAD at the time of this review: `77a414d4f42012b3b5652f91ceb3614888c265e7`.

## 1. Scope Compliance

Unit 9.7's own governance authorises exactly one capability — Bounded Relevance Computation over `DefaultKnowledgeRetrieval`'s already-eligible, closed candidate set, as a structural-match-only fallback. Fresh inspection of `DefaultKnowledgeRetrieval.kt` confirms the mechanism is invoked at exactly one call site, gated by `structurallyMatched.isEmpty() && closedCandidateSet.isNotEmpty()`, and nowhere else in the codebase. Repository-wide search confirms `RelevanceMechanism` is referenced only in `RelevanceMechanism.kt` (the contract), `DefaultKnowledgeRetrieval.kt` (the sole consumer), `QmdRelevanceMechanism.kt` (the sole concrete implementation), and `ParkerRuntime.kt`/`ParkerRuntimeConfig.kt` (composition). **Clean.**

## 2. Canonical-Authority Preservation

`DefaultKnowledgeRetrieval` still holds no `MemoryRetrieval` or `MemoryCore` dependency of any kind; `QmdRelevanceMechanism` holds no `KnowledgeItemPersistence`, `PermissionEngine`, `MemoryRetrieval`, or `MemoryCore` reference (confirmed both by declared-field inspection and by the dedicated `ParkerRuntimeKnowledgeRetrievalCompositionTest.kt` test "no Memory Core, persistence, or PermissionEngine authority reaches QmdRelevanceMechanism through this wiring"). Every disclosed `KnowledgeResultEntry` on the semantic path is built exclusively from a freshly re-resolved canonical `KnowledgeItem`, never from mechanism output. Memory Core remains the sole system of record. **Clean.**

## 3. Permission-Authority Preservation

`PermissionEngine`'s two-tier act-level/item-level gate and frozen nine-step evaluation order are untouched. The semantic-path fallback branch reuses the identical `permissionApprove` gate and adds a third, fresh re-evaluation at Pre-disclosure — never a parallel or weakened permission pathway. Confirmed by `ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`'s "Knowledge Retrieval's own act-level and item-level gates both resolve against the real, composed policy" test, and by code inspection of `resolveSemanticResult`. **Clean.**

## 4. Bounded Candidate-Set Preservation

The closed candidate set is built once, from the full lifecycle-eligible set, before the mechanism is ever invoked, and is never widened or re-derived afterward — `resolveSemanticResult` only resolves tokens already present in the request-scoped `tokenToItem` map built by `mintFallbackCandidates`, never re-queries `persistence.findAll()`. An unknown, duplicate, or excess returned token is a thrown integrity fault, rejected fail-closed, never substituted. **Clean.**

## 5. Opaque Identity Boundary

`RelevanceCandidateToken` is a single blank-rejecting `String`, structurally incapable of carrying `KnowledgeId` — proven by reflection in `RelevanceMechanismContractShapeTest.kt`. Tokens are freshly minted (`UUID.randomUUID()`) per call, never derived from canonical identity, and the token-to-item map is local to a single `retrieve()` invocation, confirmed never to be a class-level field. The mechanism itself (`QmdRelevanceMechanism`) never receives or transmits anything beyond `{token, content}` per candidate and `queryText` — confirmed both by the JSON request builder and by `QmdRelevanceMechanismTest.kt`'s "request JSON carries only queryText and token, content per candidate -- no KnowledgeId" test. **Clean.**

## 6. Fresh Pre-Disclosure Re-Verification

Every mechanism-surfaced token undergoes, immediately before disclosure: a fresh `persistence.find()` (never the Pre-computation snapshot), a fresh `isRetrievable()` check, and a fresh, independent `PermissionEngine.evaluate()` call. The final `KnowledgeResultEntry` is built exclusively from that freshly-resolved `KnowledgeItem`. A candidate that fails any of these three checks is silently excluded — an ordinary exclusion, never escalated to a thrown fault and never causing the whole retrieval to fail. **Clean.**

## 7. QMD Subordination

QMD is referenced only behind the implementation-neutral `RelevanceMechanism` interface everywhere in production code — proven structurally by `ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`'s "no concrete QMD type leaks into DefaultKnowledgeRetrieval's own constructor contract" test (via `kotlin.reflect`'s `primaryConstructor`). QMD's own ranked output is discovery only, never authority: `resolveSemanticResult` treats it exactly as a hint requiring full independent re-verification, never trusting its ordering, content, or presence without that re-verification. Mechanism selection was itself a governed, adversarially-reviewed spike decision (Section 13/13.1), not an unreviewed default. **Clean.**

## 8. Local/Operator Control

`ProcessBuilderQmdSubprocessInvoker` launches a local executable via `ProcessBuilder` only — no socket, no HTTP client, anywhere in this path. `tools/qmd-relevance-bridge.mts` (read in full during this review) imports only local files and `node:fs/promises`; its own pre-flight `resolveModelFile(modelUri, {directory, download: false})` check is local-filesystem-only, and this is now empirically confirmed by the live-gated "a missing local embedding model fails closed, without triggering an on-demand network download" test. No candidate content or query text crosses any network boundary the operator does not control. **Clean.**

## 9. Failure Semantics

Every fault path — subprocess start failure, timeout, non-zero exit, malformed/partial/extra-field response, unknown/duplicate token — is a distinct, thrown, diagnosable exception at both the QMD-adapter layer and (defense-in-depth) the `DefaultKnowledgeRetrieval` layer, confirmed by dedicated tests for each case in `QmdRelevanceMechanismTest.kt` and `DefaultKnowledgeRetrievalTest.kt`. No fault path is ever silently converted into a successful-looking empty result. A genuinely empty relevant-result over a non-empty supplied set, and a zero-candidate request, are both correctly treated as honest successes, never faults. **Clean.**

## 10. Non-Expansion / RKS Boundary

`RelevanceMechanism.kt` and `DefaultKnowledgeRetrieval.kt` are byte-identical to their own originating commits (`a9d4a3e`, `78de4f5` respectively) as of this review's own fresh `git diff --ignore-space-at-eol` check. `src/runtime/DefaultReasoningKnowledgeSource.kt` contains zero references to `RelevanceMechanism`, `Qmd`, or `QMD` (confirmed by fresh repository-wide grep during this review) — the live-reasoning surface remains entirely unwired to this Unit's own capability. No Unit 9.7.1–9.7.5 commit touches any `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_*AMENDMENT*` document or the Contract and Permission Successor document. No persistent semantic index, vector store, or SQLite file is created anywhere in this path — `qmd-relevance-bridge.mts`'s own header comment explicitly disclaims QMD's `createStore`/`searchVector` API for exactly this reason. **Clean.**

## Explicit Adversarial Confirmation

- **Authority creep** — none found. No component gained authority beyond Section 1's single bounded capability.
- **Canonical identifier leakage** — none found (§5).
- **Permission bypass** — none found (§3, §6).
- **Stale-result disclosure** — none found: every disclosed entry comes from a fresh Pre-disclosure re-read, never a stale Pre-computation snapshot (§6); staleness itself is honestly disclosed via `StalenessDisclosure`, computed identically on both the structural and semantic paths.
- **Persistent semantic authority/index** — none found (§10).
- **Remote semantic dependency** — none found (§8).
- **Automatic model download** — none found; the local-availability precondition is now live-verified to fail closed rather than trigger a download (§8).
- **Semantic-identity/configuration drift** — none found: mechanism identity is a hardcoded literal at the composition site, changeable only through a new, disclosed, reviewed diff, never mutable state or environment-derived drift.
- **Fallback broadening** — none found: the fallback still runs only on exact-zero-structural-match, never partial-match, never permission-denial, never concurrently with structural matching.
- **Premature Reasoning Context/RKS integration** — none found (§10); independently confirmed structurally, not merely documentarily.

## Final Constitutional Verdict

**ACCEPT — PROGRAMME MAY CLOSE.**

Every constitutional boundary Unit 9.7's governance establishes remains intact under fresh, independent, adversarial review, corroborated by genuine Windows executable evidence for both outstanding verification gates. This verdict closes Unit 9.7's own governed scope only.

**Unit 9.7 closure does not by itself establish semantic recall in Parker's live ReasoningContext/model-prompt path. That remains governed by the separate Reasoning Knowledge Source Bounded Semantic Relevance programme.**
