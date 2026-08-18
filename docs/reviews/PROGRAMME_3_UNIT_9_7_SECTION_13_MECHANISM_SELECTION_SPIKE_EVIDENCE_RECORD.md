Programme: **Programme 3, Unit 9.7, Section 13/13.1 — Bounded Semantic Relevance Mechanism-Selection Spike Evidence Record.**

## 1. Purpose

This document records the bounded, time-boxed engineering spike `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md` §13/§13.1 require before Unit 9.7.3 (Local Relevance Mechanism Adapter) may be implemented: a comparison of exactly two governed candidates — QMD, used strictly as a subordinate local semantic retrieval mechanism behind the Unit 9.7.1 `RelevanceMechanism` contract, and a simpler, dependency-light, in-process JVM mechanism — against §13.1's ten mandatory pass/fail criteria. This spike selects (or blocks) Unit 9.7.3's own concrete implementation; it does not implement Unit 9.7.3, does not begin Unit 9.7.2 or any RKS unit, and does not reopen whether Parker needs semantic relevance, QMD's subordinate role, or the adopted remedy itself — all of which remain settled.

## 2. Authoritative Governance Basis

- `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md` §13 ("QMD/Dependency Decision") and §13.1 ("Semantic fitness is a mandatory acceptance gate, not a desirable characteristic") — the spike's own mandate, scope, and the ten mandatory criteria.
- `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md` §3, §4 — the authorised production boundary and required contract surface both candidates are evaluated against.
- `PROGRAMME_3_UNIT_9_SEMANTIC_RELEVANCE_SCOPE_LOCK_REVISION_PROPOSAL.md` §2–§4 — the original demonstrated defect and the accepted experimental evidence this spike reuses rather than re-runs.
- `src/interfaces/RelevanceMechanism.kt` and `tests/contracts/RelevanceMechanismContractShapeTest.kt` (Unit 9.7.1, commit `a9d4a3e`) — the shared, implementation-neutral contract both candidates are evaluated behind.
- `tests/composition/QmdRealEmbeddingFixtures.kt`, `tests/composition/fixtures/qmd-real-embedding-vectors.json`, `tests/composition/QmdCanonicalMemoryRetrievalExperimentTest.kt`, `tests/composition/qmd-authorized-vector-bridge.mts` — the already-accepted, checked-in evidence surface this spike reuses, extends to a full six-candidate ranking, and does not silently replace.

## 3. Baseline Commit

Starting HEAD: `a9d4a3e8b4911a7ff62e24d146b754c404889ec3` ("feat(memory): add bounded relevance contract types"), matched origin exactly at spike start. Branch `experiment/qmd-canonical-memory-retrieval`. No untracked files present before this spike began.

## 4. Fixture Surface

Reused, not replaced, from the already-accepted `aadd596` fixture (as extended by `QmdRealEmbeddingFixtures.kt`'s own six-memory set):

| Token | Knowledge ID | Proposition (content) | Role |
|---|---|---|---|
| `candidate-1` | `memory-1` | the owner's synthetic emergency vet is Harbour Animal Clinic | **Positive target** |
| `candidate-2` | `memory-2` | the owner's synthetic regular vet is Riverside Veterinary Centre | Topically-similar distractor (same domain, different specific vet) |
| `candidate-3` | `memory-3` | the owner's synthetic dog groomer is Central City Grooming | Generic distractor |
| `candidate-4` | `memory-4` | the owner's synthetic emergency plumber is Wellington Rapid Plumbing | Lexically-similar distractor (shares "emergency") |
| `candidate-5` | `memory-5` | the owner's synthetic preferred pharmacy is Harbour Pharmacy | Lexically-similar distractor (shares "Harbour" brand token) |
| `candidate-6` | `memory-6` | the owner's synthetic favourite hiking trail is Widow's Peak Ridge | Generic distractor |

Query (byte-for-byte unchanged from the accepted fixture): `"Which animal clinic did I tell you to use in an emergency?"`

**Disclosed adaptation.** Per §13.1's own instruction, reuse here is at the level of query text, proposition text, and expected relative ranking — not a literal re-execution of `ParkerRuntimeReasoningContextIntegrationTest.kt`'s own object graph (a separately-governed class, §4.2/§4.3 of the Implementation Plan). Content is represented as the plain Proposition string, matching Unit 9.7.1's own content boundary (`RelevanceCandidate.content`, minimum normalised text) rather than `DefaultReasoningKnowledgeSource`'s dereferenced-content object graph. No new fixture content was added: memory-2 and memory-5 already serve §13.1's own "lexically or topically similar but propositionally distinct" requirement, so this spike did not need to invent additional distractors.

## 5. QMD Setup

**Attempted live invocation, found blocked — full account.** This spike's Linux execution environment (the device-bridge VM this session uses) has a real, pre-existing QMD checkout (`qmd`, v2.8.3) and a real, pre-existing experiment directory (`qmd-parker-experiment`) with a pre-built vector index and cached model weights (`embeddinggemma-300M-Q8_0.gguf`, 333,590,944 bytes; `qwen3-reranker-0.6b-q8_0.gguf`, 639,153,184 bytes; `qmd-query-expansion-1.7B-q4_k_m.gguf`, 1,282,438,912 bytes — all present, all `.etag`-confirmed, no network fetch needed for the weights themselves). Adapting only the original `raw-vector-probe.mjs`'s hardcoded Windows `dbPath` to this environment's mounted path and running it live returned `[]` (no results). Root-cause diagnosis, performed by direct SQLite inspection (`store_collections`, `documents`, `content_vectors` tables — all six real documents present, `active = 1`, correctly hashed and vectorised) and by running the adapted probe with full stderr captured, found the true cause: `Cannot find package 'sqlite-vec-linux-x64'` and `node-llama-cpp`'s own `NoBinaryFoundError` — this specific `node_modules` tree carries **no resolved native binary for any platform** (`node-llama-cpp/bins/` contains only unresolved `_linux-x64.moved.txt`, `_win-x64.moved.txt`, etc. placeholder files for every platform, Linux included), and this environment has no network access to fetch one. This is a genuine, disclosed environmental limitation — not a QMD defect, not a governance gap — of the same class already disclosed for `git push` (403 from proxy) and Gradle-plugin-cache resolution (Unit 9.7.1's own final report). It confirms, empirically, the task's own framing that "QMD may be executed locally only within the Windows development environment": this bridged Linux sandbox is not that environment for QMD's own live-inference purposes, even though it is for git and (partially) for this repository's own build tooling.

**Evidence path actually used, and why it is not a workaround.** `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md` §13.1 itself pre-authorises exactly this situation: "The independently recomputed cosine-similarity evidence already accepted into governance... is the reusable, deterministic expected-outcome fixture... captured data, not live model inference, exactly as the adopted Proposal's own evidence was produced." `tests/composition/QmdRealEmbeddingFixtures.kt` / `qmd-real-embedding-vectors.json` are exactly this: QMD's own real embedding-model output for the query and all six memories, captured once, by hand, on the Windows development machine (`2026-08-17T10:21:32.977Z`), via QMD's own `llm.js` `embed()` path, and already checked into this repository as fixed constants. This spike computes cosine similarity directly over those real, already-captured 768-dimension vectors — mathematically identical to what `exactVecScanByHashSeq` computes, only without the native `sqlite-vec` extension this environment cannot supply.

**Corrected mechanism-facing boundary (post Defect Confirmation Review request — see Section 18).** The script below (`~/spike/qmd-candidate-rank.mjs`, not committed to this repository — disposable spike tooling, consistent with §13's own finding that QMD tooling lives entirely outside this Kotlin/JVM repository) separates the Parker-side step that resolves each opaque token to its synthetic memory identifier (needed only to look up the right captured vector, and only for human-readable reporting) from the mechanism-facing scoring step itself, which receives and returns only `{ token, vector }` / opaque tokens — never `knowledgeId`, never `content`. The `memoryId` used for lookup and for labelling the results table (Section 9) is threaded through a separate, clearly-labelled side channel (`tokenToMemoryId`) that is never passed to `score()`:

```javascript
import { readFileSync } from "node:fs";
const fixturePath = process.argv[2];
const data = JSON.parse(readFileSync(fixturePath, "utf8"));
function cosineSimilarity(a, b) {
    let dot = 0, na = 0, nb = 0;
    for (let i = 0; i < a.length; i++) { dot += a[i]*b[i]; na += a[i]*a[i]; nb += b[i]*b[i]; }
    return dot / (Math.sqrt(na) * Math.sqrt(nb));
}

// Parker-side only: opaque token -> synthetic memory identifier, used solely
// to resolve each token's vector and to label the results table below. Never
// passed into score().
const tokenToMemoryId = Object.fromEntries(data.canonicalMemories.map((m, i) => [`candidate-${i + 1}`, m.knowledgeId]));
const tokenToContent = Object.fromEntries(data.canonicalMemories.map((m, i) => [`candidate-${i + 1}`, m.content]));
const tokenToVector = Object.fromEntries(data.canonicalMemories.map((m, i) => [`candidate-${i + 1}`, m.vector]));

// Mechanism-facing candidate: opaque token + vector only. No knowledgeId, no content.
const mechanismFacingCandidates = Object.keys(tokenToMemoryId).map(token => ({ token, vector: tokenToVector[token] }));

// "The mechanism": receives only { token, vector } candidates and a query vector; returns only ranked opaque tokens.
function score(queryVector, candidates) {
    return candidates
        .map(c => ({ token: c.token, cosineSimilarity: cosineSimilarity(queryVector, c.vector) }))
        .sort((a, b) => b.cosineSimilarity - a.cosineSimilarity);
}

const scored = score(data.paraphraseQueryVector, mechanismFacingCandidates);

// Evidence-side only, outside the mechanism boundary: re-attach knowledgeId/content for the human-readable table.
const fullRanking = scored.map(s => ({ token: s.token, knowledgeId: tokenToMemoryId[s.token], content: tokenToContent[s.token], cosineSimilarity: s.cosineSimilarity }));

console.log(JSON.stringify({ query: data.paraphraseQuery, model: data.provenance.model, rankedTokens: scored.map(s => s.token), fullRanking }, null, 2));
```

Re-running this corrected script against the identical captured vectors produced byte-identical `rankedTokens` and `cosineSimilarity` values to the original (uncorrected) version — the correction changes only which object shape flows through `score()`, not any number `score()` computes (Section 9, Section 12).

The equivalent Kotlin, checked into this repository as this spike's disposable evidence artefact, is `tests/contracts/RelevanceMechanismSpikeQmdCandidateTest.kt` — corrected the same way: a private `MechanismFacingCandidate(token, vector)` data class (structurally incapable of carrying a third field, proven by a reflection-based structural test in that file) is what `score()`'s Kotlin equivalent receives and operates over; `tokenToMemoryId` is a separate, private, evidence/reporting-only map never passed to the scoring function. It reuses `QmdRealEmbeddingFixtures` directly (already-compiled Kotlin constants, no subprocess, no native extension) and performs the identical arithmetic. Its numeric output was cross-checked against the Node.js computation above and matches exactly (Section 9).

## 6. JVM Comparator Setup

Classic TF-IDF cosine similarity over lowercased, `[a-z0-9]+`-tokenised terms; inverse document frequency computed only over the six-candidate closed set one `RelevanceRequest` supplies (smoothed IDF, `ln((N+1)/(df+1)) + 1`). Zero external dependency. Chosen because it is the standard, well-known "smallest credible" lexical baseline — not weakened to make QMD win, and not engineered beyond a fair bounded comparison. Implemented identically in Node.js (`~/spike/jvm-comparator-rank.mjs`, used to obtain verified numbers in an environment that cannot compile Kotlin — Section 11) and in Kotlin (`tests/contracts/RelevanceMechanismSpikeJvmComparatorTest.kt`, this spike's checked-in evidence artefact, cross-checked to match the Node.js numbers exactly).

## 7. Exact Frozen Configuration

- Query text: `"Which animal clinic did I tell you to use in an emergency?"` (unchanged).
- Candidate set: the six propositions in Section 4, in fixed order, each given a fixed opaque token `candidate-1`..`candidate-6`.
- QMD candidate: model `hf:ggml-org/embeddinggemma-300M-GGUF/embeddinggemma-300M-Q8_0.gguf`, captured `2026-08-17T10:21:32.977Z`, 768-dimension vectors, cosine similarity.
- JVM comparator: TF-IDF cosine similarity as specified in Section 6, no external configuration.

## 8. Repetition Count

Both candidates were computed **3 times independently** (fresh process invocation each time) against the identical frozen configuration above. All 3 runs were byte-identical for each candidate (`diff` confirmed no difference; `tests/contracts/RelevanceMechanismSpike*Test.kt` each also assert this directly as a compiled, permanent regression check).

## 9. Results Table

**"Knowledge Id" below is an evidence/reporting-table column only** — it is re-attached, outside the mechanism boundary, after `score()` (Section 5) has already returned its ranked opaque tokens, purely so a human reader can see which synthetic memory each token corresponds to. It is never part of the object the mechanism-facing scoring step receives or returns (Section 5, Section 18).

**QMD candidate** (real captured embeddings, cosine similarity):

| Rank | Token | Knowledge ID | Content | Cosine similarity |
|---|---|---|---|---|
| 1 | candidate-1 | memory-1 | emergency vet — Harbour Animal Clinic (**target**) | 0.585831 |
| 2 | candidate-2 | memory-2 | regular vet — Riverside Veterinary Centre | 0.413660 |
| 3 | candidate-3 | memory-3 | dog groomer — Central City Grooming | 0.292532 |
| 4 | candidate-4 | memory-4 | emergency plumber — Wellington Rapid Plumbing | 0.266431 |
| 5 | candidate-5 | memory-5 | pharmacy — Harbour Pharmacy | 0.212974 |
| 6 | candidate-6 | memory-6 | hiking trail — Widow's Peak Ridge | 0.074136 |

This reproduces the already-accepted governance figures exactly (0.586 target vs. 0.414 strongest distractor, `PROGRAMME_3_UNIT_9_SEMANTIC_RELEVANCE_SCOPE_LOCK_REVISION_PROPOSAL.md` §4) and extends them, for the first time in this repository, to the full six-candidate ranking.

**JVM comparator** (TF-IDF cosine similarity):

| Rank | Token | Knowledge ID | Content | Cosine similarity |
|---|---|---|---|---|
| 1 | candidate-1 | memory-1 | emergency vet — Harbour Animal Clinic (**target**) | 0.281137 |
| 2 | candidate-4 | memory-4 | emergency plumber — Wellington Rapid Plumbing | 0.066517 |
| 3 | candidate-2 | memory-2 | regular vet — Riverside Veterinary Centre | 0.000000 |
| 3 | candidate-3 | memory-3 | dog groomer — Central City Grooming | 0.000000 |
| 3 | candidate-5 | memory-5 | pharmacy — Harbour Pharmacy | 0.000000 |
| 3 | candidate-6 | memory-6 | hiking trail — Widow's Peak Ridge | 0.000000 |

The target is still ranked first, but the **emergency plumber** (candidate-4) — propositionally unrelated to an animal clinic — outranks the **regular vet** (candidate-2), which is genuinely in the same domain as the target, purely because "emergency" is a token both the query and candidate-4 share. Candidate-2 scores exactly zero: zero token overlap with the query at all.

## 10. Mandatory Pass/Fail Table (§13.1's ten criteria)

| # | Criterion | QMD | JVM comparator |
|---|---|---|---|
| 1 | Positive semantic recall | **PASS** — target ranked 1st | **PASS** — target ranked 1st |
| 2 | Negative discrimination | **PASS** — clear score gap (0.586→0.414→...), no distractor elevated to/above target | **PASS at rank 1, but see 3/4** — no distractor outranks the target itself |
| 3 | Ranking fidelity | **PASS** — genuinely related distractor (regular vet) ranks 2nd, ahead of unrelated ones | **FAIL** — genuinely related distractor (regular vet) ranks *below* an unrelated one (plumber) |
| 4 | Proposition fidelity | **PASS** — lexically-similar "Harbour Pharmacy" (shared brand) not elevated (ranks 5th); lexically-similar "emergency plumber" not elevated (ranks 4th) | **FAIL** — "emergency plumber" (shares only the word "emergency") is treated as more relevant than "regular vet" (genuinely same domain, zero shared tokens) |
| 5 | Repetition stability | **PASS** — 3/3 runs byte-identical | **PASS** — 3/3 runs byte-identical |
| 6 | Determinism | **PASS** — fixed vectors, fixed arithmetic, no randomness | **PASS** — fixed tokenisation/IDF, fixed arithmetic, no randomness |
| 7 | Contract compliance | **PASS** — evaluated as `RelevanceRequest`-shaped in / `RelevanceResult`-shaped out; no widening | **PASS** — same |
| 8 | Architectural inability | **PASS** — the arithmetic itself needs no persistence/permission/Memory Core reference; see Section 11 for the disclosed native-store-API operational caveat | **PASS** — needs no persistence/permission/Memory Core reference |
| 9 | Local/operator-controlled | **PASS** — on-device model, no network call in the mechanism itself; see Section 11 for the disclosed platform-binary caveat | **PASS** — pure in-process arithmetic, no external call of any kind |
| 10 | Replaceability | **PASS** — fully swappable behind `RelevanceMechanism`, nothing in Unit 9.7.1 couples to QMD | **PASS** — same |

**QMD passes all ten. The JVM comparator fails criteria 3 and 4 — both mandatory. Per §13.1, "ALL TEN ARE MANDATORY... no weighted-average pass," the JVM comparator does not qualify, regardless of its materially lower complexity and dependency burden.**

## 11. Operational Comparison

| | QMD | JVM comparator |
|---|---|---|
| Ranking-arithmetic latency (5 runs, this spike, over already-embedded/tokenised input) | 44–65 ms (Node.js process; includes ~35 ms Node startup baseline) | 46–57 ms (Node.js process; includes ~35 ms Node startup baseline) |
| Live embedding-inference latency | **Not measured in this environment** — blocked by the missing native binary (Section 5); must be measured on the Windows development machine, where the fixture's own provenance confirms it is achievable | N/A — no embedding model involved |
| Model/weight footprint | 333 MB (embed) + 639 MB (rerank, not required for ranking alone) + 1.28 GB (query-expansion, not required) on disk | None |
| Runtime dependency | Node.js process; native `llama.cpp` backend (platform-specific binary); native `sqlite-vec` `vec0` extension if using QMD's own store/search API | None beyond the JVM already running Parker |
| Process lifecycle | Out-of-process (Node.js) unless a genuinely embedded JVM↔native binding is built; the existing accepted precedent (`RealQmdAuthorizedVectorProcessBridge`, `qmd-authorized-vector-bridge.mts`) already demonstrates a disposable, request-scoped subprocess bridge pattern | In-process, no subprocess |
| Testability without live inference | **Demonstrated in this spike**: real captured vectors + pure arithmetic, fully deterministic, no subprocess, no native extension — `RelevanceMechanismSpikeQmdCandidateTest.kt` | Trivially in-process |

## 12. Dependency Comparison

QMD requires: a Node.js runtime, `node-llama-cpp` with a resolved platform-native binary, `sqlite-vec` with a resolved platform-native binary (if using QMD's own store API rather than a bridged precomputed-vector path), and the `embeddinggemma-300M-Q8_0.gguf` model weights (333 MB). The JVM comparator requires nothing beyond the Kotlin standard library already present in this build. This is a real, substantial complexity/dependency asymmetry — acknowledged in full, and not the deciding factor per §13.1's own explicit instruction ("Selection is never made solely because a candidate is simpler... the selected mechanism is the smallest candidate that actually satisfies all ten mandatory requirements").

## 13. Constitutional-Boundary Comparison

Neither candidate, as evaluated in this spike, receives or could receive `KnowledgeId`, canonical Memory IDs, `AssertionId`, `EntityId`, provenance, permission state, lifecycle state, evidential state, or `StalenessDisclosure` — both are evaluated strictly through opaque tokens and minimum content, mirroring `RelevanceCandidateToken`/`RelevanceCandidate`/`RelevanceRequest`/`RelevanceResult`. Neither candidate, as evaluated, has any handle capable of reaching canonical persistence, `PermissionEngine`, or Memory Core. Both remain, as evaluated, fully behind Unit 9.7.1's implementation-neutral interface — no Frozen Boundary was crossed, tested against, or found violated by either candidate in this spike.

**This claim is now literally demonstrated by the spike artefacts themselves, not only asserted in prose (Section 18).** The JVM comparator's mechanism-facing representation (`tests/contracts/RelevanceMechanismSpikeJvmComparatorTest.kt`) was already exactly `(token, content)` — no `knowledgeId` field ever existed there. The QMD candidate's mechanism-facing representation (`tests/contracts/RelevanceMechanismSpikeQmdCandidateTest.kt`, and its Node.js cross-check, Section 5) now uses a dedicated `MechanismFacingCandidate(token, vector)` type — structurally incapable of carrying a `KnowledgeId` or any other field, proven by a reflection-based structural test in that file — with the token-to-synthetic-memory-identifier mapping kept in a separate, private, evidence/reporting-only map that the scoring function itself never receives.

## 14. Selected Mechanism

**QMD**, used strictly as a subordinate local semantic retrieval mechanism behind the Unit 9.7.1 `RelevanceMechanism` contract.

## 15. Reasons for Selection

Per §13.1's selection rule: "Does QMD pass all ten mandatory criteria? ... If only QMD passes: SELECT QMD." That is the exact situation this spike found. QMD passes all ten mandatory criteria; the JVM comparator, evaluated fairly and without deliberate crippling, fails two of them (ranking fidelity, proposition fidelity) — specifically because a bag-of-words/TF-IDF representation has no way to represent that a veterinary clinic is a closer match to an animal-clinic query than a plumbing service is, once both happen to share the incidental token "emergency." This is not a marginal or close-call failure: candidate-2 (the genuinely related distractor) scores exactly `0.0` under TF-IDF, while candidate-4 (the unrelated distractor) scores above zero purely from one shared word. QMD's real semantic embedding, by contrast, correctly separates both lexically-similar distractors (memory-4's shared "emergency," memory-5's shared "Harbour") from genuine propositional relevance, ranking each appropriately below the target and below the genuinely-related distractor. Selection is made on this correctness basis, not because QMD produced the original successful experiment (§13.1's own explicit prohibition) — the JVM comparator was given a fair, standard, non-strawman implementation and still did not meet the bar.

## 16. Any Defects Discovered

No defect in Unit 9.7.1's contract, in any adopted governance document, or in QMD itself. One genuine, disclosed **environmental** limitation of this specific spike execution environment: live QMD embedding-model inference cannot run here (Section 5) — a missing-native-binary-plus-no-network condition, not a design or governance defect, and not something this spike attempted to route around; it is disclosed here and the already-accepted captured-vector evidence path §13.1 itself anticipates was used instead.

## 17. Unit 9.7.3 Implications

1. **Request-scoped, disposable operation is achievable but is not QMD's default API shape.** QMD's own idiomatic `createStore`/`searchVector` API is index/store-centric (a persistent SQLite file, built ahead of time over a filesystem-backed collection) — using it naively risks drifting toward a persistent secondary index, which Frozen Boundary #9/#10 forbids. The already-accepted `qmd-authorized-vector-bridge.mts` precedent (a fresh, disposable, request-scoped SQLite store built and torn down per call, receiving precomputed vectors directly rather than re-embedding) is the correct architectural template for Unit 9.7.3's own adapter design, not the default persistent-store flow this spike's own adapted `raw-vector-probe.mjs` reuse initially reached for.
2. **Platform-native binary provisioning must be pinned and confirmed per deployment target.** This spike's own Linux sandbox lacks a resolved `node-llama-cpp` binary for any platform and lacks `sqlite-vec-linux-x64`; the Windows development machine is confirmed working (the fixture's own provenance). Unit 9.7.3 must pin mechanism identity/version/configuration (per the adopted Successor §3 condition 14) to include the specific native binary/platform combination it depends on, and must not assume it is available in every execution environment this repository's own tooling might otherwise run in.
3. **Prefer captured-vector-plus-pure-arithmetic tests over subprocess/native-extension-dependent tests where possible.** This spike's own `RelevanceMechanismSpikeQmdCandidateTest.kt` demonstrates that QMD's real model output, once captured, can be tested fully deterministically with zero subprocess and zero native dependency — a more portable, more reliable regression-test shape than a live subprocess bridge for any test that does not specifically need to exercise the subprocess/native-extension boundary itself.
4. **Live embedding-inference latency remains unmeasured and should be measured directly on a real deployment target before Unit 9.7.3 commits to a request-scoped, on-demand embedding strategy** — the near-zero latencies this spike measured (Section 11) reflect only ranking arithmetic over already-embedded vectors, not model load or forward-pass cost.
5. Neither candidate's evaluation in this spike required loosening, widening, or reinterpreting Unit 9.7.1's contract in any way — RKS.1's own strict compatibility-gate requirement (no authority to extend Unit 9.7.1) remains straightforwardly satisfiable by a QMD-backed Unit 9.7.3 adapter.

## 18. Independent Mechanism Selection Review and Bounded Defect Correction

**Independent Mechanism Selection Review verdict: REVISE BEFORE ACCEPTANCE.** The QMD semantic result and the QMD-vs-JVM-comparator selection (Sections 9–10, 14–15) were **not** reopened by this review and are unchanged by the correction below.

**Defect found.** The reproduced QMD scoring harness (Section 5's Node.js script, and the parallel construction in `tests/contracts/RelevanceMechanismSpikeQmdCandidateTest.kt`'s original `rank()` function) constructed mechanism/scoring-facing objects carrying `token`, `knowledgeId`, `content`, and `vector` together, and carried `knowledgeId` through into the scored result. This contradicted this same document's own Section 13 claim that neither candidate receives or could receive `KnowledgeId` through the mechanism boundary: the fact that `knowledgeId` was never read by the cosine-similarity arithmetic did not cure the defect, because the mechanism-facing evidence harness must demonstrate architectural inability *by omission* — the same technique `RelevanceMechanism.kt`'s own production contract relies on — not merely by disuse.

**Correction performed.**
- The Node.js harness (Section 5) was restructured so that a Parker-side step (`tokenToMemoryId`, `tokenToContent`, `tokenToVector`) resolves each opaque token's vector and, separately, its human-readable labels, while a distinct `score(queryVector, candidates)` function — standing in for "the mechanism" — receives and returns only `{ token, vector }` / opaque tokens. `knowledgeId` and `content` are re-attached only afterward, for the results table, outside `score()`.
- `tests/contracts/RelevanceMechanismSpikeQmdCandidateTest.kt` was corrected the same way: a new private `MechanismFacingCandidate(token, vector)` data class replaces the previous inline `Pair`-based construction that had `id` (a synthetic memory identifier) in scope within the same lambda that built the scored candidate; `rank()` was split into `buildMechanismFacingCandidates()` (Parker-side, reads the token-to-memory-id map) and `score(queryVector, candidates)` (mechanism-facing, receives only `MechanismFacingCandidate` values). A new structural test, `MechanismFacingCandidate declares only token and vector, nothing else`, uses reflection to assert the type's declared properties are exactly `{token, vector}` and that neither is `KnowledgeId`-typed — the same reflection idiom `tests/contracts/InterfaceContractShapeTest.kt` and `RelevanceMechanismContractShapeTest.kt` (Unit 9.7.1) already establish for this codebase.
- The JVM comparator (`tests/contracts/RelevanceMechanismSpikeJvmComparatorTest.kt`) was inspected and found **already compliant**: its `candidateContents` list was always `(token, content)` pairs only: no `knowledgeId` field existed there at any point. It was not modified.

**Non-regression confirmed.** Query text, the six candidate propositions, the captured QMD vectors, the QMD model identity, the cosine-similarity calculation, the TF-IDF algorithm, the repetition count, the ten mandatory criteria, the selection rule, the `RelevanceMechanism` production contract, adopted Unit 9.7 governance, QMD's subordinate role, Parker's canonical authority, fail-closed requirements, and the request-scoped opaque-token rule are all unchanged. Re-running the corrected Node.js harness against the identical captured vectors produced byte-identical `rankedTokens` and `cosineSimilarity` values to the pre-correction version (verified directly, not assumed) — the correction changes only which object shape flows through the scoring step, not any number it computes. QMD remains the selected mechanism (Sections 14–15), unchanged.

## 19. Adoption Record

**Adopted.** Spike Completion Review was completed (self-conducted). Independent Mechanism Selection Review (Section 18) returned REVISE BEFORE ACCEPTANCE, identifying one bounded constitutional defect — the mechanism-facing QMD candidate representation illegitimately carried `KnowledgeId` alongside `token`/`vector`. That defect was corrected (Section 18): the mechanism-facing representation was reduced to `MechanismFacingCandidate(token, vector)` only, with the token→synthetic-memory mapping retained strictly outside the mechanism boundary for reporting purposes. Windows executable verification of the corrected Kotlin test files was then completed successfully: `RelevanceMechanismSpikeQmdCandidateTest` — BUILD SUCCESSFUL; `RelevanceMechanismSpikeJvmComparatorTest` — BUILD SUCCESSFUL; `RelevanceMechanismContractShapeTest` (Unit 9.7.1 regression) — BUILD SUCCESSFUL; the full repository test suite — BUILD SUCCESSFUL. Defect Confirmation Review then returned ACCEPT. On the strength of this complete review chain, **QMD is now formally selected and adopted as the mechanism Unit 9.7.3 will implement**, strictly as a subordinate local retrieval mechanism behind the Unit 9.7.1 `RelevanceMechanism` contract, per Sections 9–10 and 14–15 of this record. This adoption authorises proceeding to Unit 9.7.3 — the concrete Local Relevance Mechanism Adapter implementing QMD behind `RelevanceMechanism` — but does not itself implement Unit 9.7.3; no production code is introduced by this adoption.
