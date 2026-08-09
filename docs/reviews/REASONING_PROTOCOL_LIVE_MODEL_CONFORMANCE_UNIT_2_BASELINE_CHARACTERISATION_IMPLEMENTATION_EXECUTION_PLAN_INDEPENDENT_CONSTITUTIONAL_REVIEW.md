**Status:** Genuine Independent Constitutional/Planning Review of the Unit 2 Implementation/Execution Plan. Feasibility and constitutional isolation were challenged against the repository and accepted Scope Lock rather than presumed.

# Reasoning Protocol Live-Model Conformance Unit 2 Implementation/Execution Plan — Independent Constitutional Review

## 1. Feasibility reconstruction

The existing `liveModelEvaluation` source set contains public Unit 1 fixture, profile, execution, observation, derivation, and JSONL functions. A new class in the same test-only source set can compose them. A dedicated filtered Gradle task can set the additional activation property without attaching to lifecycle tasks. No production seam, Unit 1 edit, dependency, or third helper is inherently required.

## 2. Challenges

| Challenge | Finding |
|---|---|
| Minimal surface | **PASS.** One build task and one cohesive campaign class are sufficient; data artifacts are outputs, not repository implementation files. |
| Unit 1 unchanged | **PASS.** Public APIs are composed. Hash regression guards both committed Unit 1 files. Any insufficiency stops for Unit 1 defect governance. |
| Production Kotlin needed | **NO.** Model calls and parsing remain inside the accepted Unit 1 production-component path. |
| Deterministic batching | **PASS.** Full schedule is generated before endpoint construction; fourteen bounded batches preserve cell order and trial identity. |
| Resume protection | **PASS.** Raw record is durable before checkpoint; manifests and full configuration fingerprint are verified; first missing scheduled ID is the only next call. |
| Duplicate prevention | **PASS.** Existing verified ID blocks a call; duplicate/unknown raw ID blocks resume; crash between raw/checkpoint does not repeat inference. |
| Reproducible summaries | **PASS.** Pure aggregation, stable ordering/encoding, preserved reviewer worksheet, and artifact hashes permit regeneration. |
| New dependency | **NONE.** JDK/Kotlin standard library and existing Unit 1 serialization are sufficient. |
| Lifecycle isolation | **PASS.** Dedicated task is filtered and property-gated; existing Unit 1 task lacks the property; ordinary lifecycle remains detached. |
| Preflight before scoring | **PASS.** Stage 1 is unreachable until eleven Stage 0 records and identity/artifact manifests validate and explicit scored approval exists. |
| Explicit execution | **PASS.** Dedicated task, campaign property, full environment identity, Stage 0 approval, and operator-controlled batches prevent accidental campaign launch. |
| Poor results recorded without fixing | **PASS.** Completed adverse outcomes are never retried; false positives pause/preserve; tuning and remedy work are forbidden. |

## 3. Adversarial checks

The current Unit 1 `writeArtifact` writes a supplied observation list as a complete file. The plan does not depend on using it as an append API: Unit 2 can use the public deterministic `EvaluationJsonLines` serializer and JDK file/channel operations to append and force each record without changing Unit 1.

Running the general Unit 1 evaluation task with full evaluation variables could otherwise select all integration tests. The separate `parker.reasoning.baseline.enabled` property ensures the Unit 2 live method skips there; the dedicated Unit 2 task filters to its class. This is a necessary safety boundary, not a new production configuration.

Human fidelity review cannot be fully deterministic, but its inputs, independent decisions, resolution, and summary regeneration can be. The plan correctly preserves both reviewer judgments rather than overwriting disagreement.

The proposed atomic checkpoint cannot make all files a single filesystem transaction. Raw-first ordering is therefore constitutionally important: after a crash, a valid raw record is adopted without another endpoint call; checkpoint-without-raw blocks. This prevents duplicate sampling.

## 4. Boundary determination

The two-file surface is feasible but not yet implementation authority. The later Boundary Review must freshly confirm Gradle task filtering/property behavior, public API visibility, file-channel durability, path/access controls, and that one Kotlin file remains maintainable without hidden additional surface.

No plan defect or corrective action is identified.

## 5. Verdict

```text
ACCEPTED
```

Corrective action: **NONE**. Next gate: Unit 2 Boundary Review. No implementation or live call is authorized.
