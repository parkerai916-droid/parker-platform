**Status:** Unit 2 Defect Confirmation Review — **CONFIRMED / CORRECTED**. Classification **B — UNIT 2 IMPLEMENTATION DEFECT; parent-root validation was one level too strict**. Reviewed against committed baseline `3a7c606`. Zero live-model calls have occurred and Stage 0 remains not started.

# Reasoning Protocol Live-Model Conformance Unit 2 — Artifact Root Defect Confirmation Review

## 1. Evidence and governing interpretation

The accepted Boundary Review freezes the authoritative artifact hierarchy as:

```text
/var/lib/parker/reasoning-protocol-live-model/<campaign-id>/
```

The Scope Lock requires artifacts outside the worktree at the accepted durable Ubuntu location. The Implementation/Execution Plan describes an explicitly configured artifact root and then an artifact tree beginning at `<campaign-id>/`. The accepted readiness reviews repeat the same final hierarchy. Read together, these authorities have one coherent meaning:

```text
PARKER_REASONING_BASELINE_ARTIFACT_ROOT=/var/lib/parker/reasoning-protocol-live-model
final campaign directory=artifactRoot.resolve(campaignId)
```

No governance ambiguity exists. The environment variable names the accepted parent, not an already campaign-qualified directory.

## 2. Defect confirmation

The committed loader normalized the configured value and required it to start with:

```text
/var/lib/parker/reasoning-protocol-live-model/
```

That rejected the lawful parent because equality lacks the trailing slash. Conversely, an already campaign-qualified root passed that check, after which the live entry resolved `campaignId` again, producing `<campaign-id>/<campaign-id>`.

Classification:

```text
B — UNIT 2 IMPLEMENTATION DEFECT; parent-root validation is one level too strict
```

The defect was found before Stage 0. It caused no artifact misplacement, inference call, sampling effect, production consequence, or evidence corruption.

## 3. Minimum lawful correction

Only `tests/integration/ReasoningProtocolBaselineCharacterisationTest.kt` changed.

The correction:

1. requires the configured root, after separator normalization and trailing-slash normalization, to equal the accepted parent exactly;
2. rejects already campaign-qualified, outside, relative fallback, and traversal-shaped values;
3. resolves the machine-safe campaign ID exactly once;
4. validates that the resolved result equals `<accepted-parent>/<campaign-id>` exactly;
5. stores that final path as `campaignArtifactRoot`; and
6. passes the precomputed final path to `DurableCampaignRunner`, removing the later second resolution.

No storage design was broadened. No Unit 1, production, Gradle, dependency, prompt, parser, transport, model, or governance file changed.

## 4. Path-safety proof

- Campaign IDs retain `[a-z0-9][a-z0-9.-]*`; slash, backslash, `..`, and relative traversal are impossible.
- The parent must equal `/var/lib/parker/reasoning-protocol-live-model`; prefix membership is insufficient.
- The normalized final path must equal `/var/lib/parker/reasoning-protocol-live-model/<campaign-id>`.
- An already qualified root is rejected instead of silently creating a doubled path.
- `/tmp`, `build/reports`, `src`, and parent-traversal inputs are rejected.
- There is no arbitrary path, worktree fallback, or environment-dependent alternate root.

## 5. Deterministic correction verification

The Unit 2 offline class now proves:

- accepted parent root is valid;
- exact one-level final campaign path;
- campaign-qualified root rejection;
- outside-parent rejection;
- traversal and worktree/build fallback rejection;
- live entry remains skipped without complete explicit configuration; and
- Unit 1 byte hashes and task isolation remain intact.

## 6. Verdict

```text
DEFECT CONFIRMED: YES
CORRECTION: PASS
```

Corrective action remaining: **NONE**, subject to the independent constitutional defect review and final integrity review. This review grants no Stage 0 or scored-execution authority.
