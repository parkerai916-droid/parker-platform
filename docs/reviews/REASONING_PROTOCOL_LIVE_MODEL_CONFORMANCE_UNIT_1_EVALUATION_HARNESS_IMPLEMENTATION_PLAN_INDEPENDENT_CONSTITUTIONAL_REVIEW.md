**Status:** Genuine Independent Constitutional/Planning Review of the Unit 1 Implementation Plan. The plan was tested against the accepted Scope Lock and current repository rather than presumed correct.

# Reasoning Protocol Live-Model Conformance Unit 1 Implementation Plan — Independent Constitutional Review

## 1. Independent feasibility check

The repository currently has only `main` and `test` Kotlin source sets. The production client already exposes formatter/parser callbacks and top-level default Ollama functions, so raw-envelope and extracted-output capture can be implemented transparently in test code. `ModelReasoningProvider` already accepts the real builder, client, parser, and timeout. Consequently no production seam or dependency is necessary.

## 2. Planning challenges

| Challenge | Finding |
|---|---|
| File surface is minimal | **PASS.** One build file and two isolated evaluation files contain configuration, instrument, offline verification, and live entry. Further splitting is optional and unauthorized. |
| Production modification necessary | **NO.** Existing constructor seams provide all required observation points. Any discovered production need triggers Boundary Review. |
| Uses production components, not copies | **PASS.** The exact production builder/provider/client/parser and default Ollama functions are mandated. Test code only constructs inputs and records outputs. |
| Deterministic tests run offline | **PASS.** Instrument tests use pure data and loopback stubs. Live configuration absence skips before client construction. |
| Opt-in execution explicit | **PASS.** A detached Gradle task plus a separate evaluation variable namespace prevents ordinary invocation and accidental production-config fallback. |
| Evidence sufficient | **PASS.** Identity, prompt hash/full synthetic prompt, raw envelope, extracted output, parsing, expected/actual action, fidelity, latency, tokens, sequence, and taxonomy are retained. Missing digest/tokens are disclosed. |
| Repetition/profiles supported | **PASS.** Immutable identities, profile constructors, repetitions, hashes, matched-profile J, and repeat-derived K are structural without launching Unit 2. |
| Over-engineering | **PASS after challenge.** No database, framework, JSON dependency, service, alternate runtime, or generalized benchmark platform is proposed. Two test files are the smallest cohesive separation. |
| Fail-closed semantics | **PASS.** Parser remains sole representation authority; classification cannot return a typed result; no retry or downstream call exists. |
| Stops before characterisation | **PASS.** Only a separately authorized smoke call may prove targetability. Statistical runs and conclusions belong exclusively to Unit 2. |

## 3. Adversarial implementation checks

- A capture callback that parses or rewrites before calling the default function would violate the plan and fail completion review.
- A JUnit tag within the ordinary `test` source set would be weaker than the detached source-set boundary and is not the selected design.
- A live test that silently uses `localhost`, production `PARKER_MODEL_*` values, or a default model would violate explicit opt-in configuration.
- A JSONL writer that records a credential-bearing URL, arbitrary environment, or real prompt would be a constitutional defect.
- Calling the provider again after C/E/F/G would be a retry and is forbidden, even if described as measurement.
- Treating a valid `NOACTION` as malformed to improve semantic accuracy would corrupt evidence and is forbidden.

## 4. Boundary and correction determination

Adding the source set/task changes build topology and contacts explicitly configured external infrastructure, so the accepted programme Planning Review correctly requires a distinct Unit 1 Boundary Review before code. This is a procedural gate, not a defect in the Implementation Plan.

No plan correction is required. No production/test implementation is authorized by this review.

## 5. Verdict

```text
ACCEPTED
```

Corrective action: **NONE**. Next action: perform the Unit 1 Boundary Review and stop unless it is accepted and implementation is explicitly approved.
