**Status:** Finalised Independent Constitutional Review of the Unit 2 Stage 0 Failure — **ACCEPTED WITH QUALIFICATIONS**. The Failure Review was evidence, not authority; the determination also incorporates independent Claude cross-model review and a read-only Ubuntu artifact-integrity capture. No live HTTP or campaign execution occurred during this review.

# Unit 2 Stage 0 Failure — Independent Constitutional Review

## 1. Independent evidence reconstruction

PF01 used the frozen direct-Remember fixture under minimal production context. The production prompt explicitly routes direct “Remember that X” instructions to REMEMBER. The captured response begins with one valid `REPLY:` tag. The production parser therefore correctly returned `Reply`; expected REMEMBER versus actual REPLY deterministically yields primary classification D. Representation-valid `true` is compatible with semantic-wrong `D` and is not contradictory.

Independent determination: **genuine semantic action-selection failure by the pinned live model configuration**.

## 2. Production-chain and measurement challenge

Source reconstruction confirms Unit 1 instantiates the real `DefaultReasoningPromptBuilder`, `LocalHttpModelInferenceClient`, `ModelReasoningProvider` and `TaggedReasoningResponseParser`. Transparent transport capture records rather than replaces production request/response formatting. The classification branch is direct and contains no semantic repair. The Unit 2 driver invokes this harness once per scheduled trial and appends the returned observation.

The supplied prompt/output/parser/metadata/artifact facts cohere with that path. No evidence suggests alternate context, retry, normalization, parser error, timeout or transport failure. The harness has no downstream production destination. **No measurement or implementation defect is found.**

## 3. Artifact and stopping-point challenge

Supplied state—four intents, four raw records, four checkpoints, PF01:D failure marker, `PREFLIGHT_FAILED` manifest, no Stage 0 seal and no scored evidence—is exactly what the committed fail-closed branch produces after three warm-ups and PF01. The runner evaluates the failure only after raw force and checkpoint, then returns before selecting PF02. A subsequent run blocks on `stage-0.failed`.

The stopping point is constitutionally correct under the accepted implementation/readiness boundary. No scored execution accidentally occurred according to the supplied absence of Stage 1/2 artifacts and the false scored-approval flag.

The authoritative Ubuntu runtime's read-only inventory records:

| Artifact | SHA-256 | Bytes | Lines |
|---|---|---:|---:|
| `campaign-definition.txt` | `64ce538c39d135b9bcd7fee14bb5e49bcc9ef0ef1962d5afb318b02d9fe662a3` | 404211 | 3950 |
| `campaign-identity.txt` | `c2f7f56a7dabe552e5311051e2545d73a403e417137b56ccebb841b46ac12c94` | 202 | 3 |
| `stage-0.failed` | `8bb87b7a23b30de1bb8b890272f402bf91cbcbc845f8091e56e07ba6d9f11a1f` | 7 | 1 |
| `stage-0/STAGE-0/intent.jsonl` | `325ca44ec35f8c1594f77105fe0ca2f1a93eaa088fc4d6cd7324daccf5ca0f32` | 728 | 4 |
| `stage-0/STAGE-0/raw.jsonl` | `c635ebcd051a7eeb02e154e3b07a4ba9e101fcd71f019bebc5990961f8179d5f` | 24727 | 4 |
| `stage-0/STAGE-0/checkpoint.txt` | `1cdd4644df9c8d8f9437354228a090300c65285de1c70d2bfb7db2e72af2fd08` | 360 | 4 |
| `stage-0/STAGE-0/manifest.txt` | `4e63cb8a13edc7bfc146e4573cc4717acea00b5a86e4091416e4c985b2a40cce` | 275 | 10 |

The inventory was captured read-only on Ubuntu and was not independently recomputed by this Windows workspace. It fingerprints a state consistent with three warm-ups, PF01, four intent/raw/checkpoint entries, `PREFLIGHT_FAILED`, no Stage 0 seal and no scored evidence.

## 4. Continuation and scoring

```text
PF02–PF08:
NOT AUTHORIZED UNDER EXISTING GOVERNANCE
```

The original plan anticipates eleven calls, but accepted implementation reviews explicitly establish that adverse preflight evidence blocks continuation. Neither deleting the marker nor bypassing the guard is authorized. PF02–PF08 cannot run now. A new explicit governance determination would be required before collecting the remaining unscored calls if continuation were later proposed; that requirement does not itself authorize continuation.

```text
SCORED CAMPAIGN:
NOT AUTHORIZED
```

The required Stage 0 seal is absent and scored approval is false. Interesting adverse evidence cannot weaken that gate.

## 5. Early-closure challenge

PF01 proves one important deterministic possibility, not a statistical baseline. Treating it as full Unit 2 characterisation would overclaim all unobserved fixtures, contexts and repeatability dimensions. The Scope Lock's exceptional early-closure route is not satisfied merely because the first direct-Remember preflight failed.

```text
UNIT 2 EARLY ADVERSE CLOSURE:
NOT ACCEPTED ON CURRENT EVIDENCE
```

## 6. Remedy leakage

No prompt, schema, retry, sampling, model, classifier/renderer or other remedy is selected or recommended. Those remain neutral future Unit 3 candidate categories. Unit 2 remains measurement-only.

## 7. Constitutional verdict

```text
ACCEPTED WITH QUALIFICATIONS
```

Qualifications/corrective actions:

1. preserve the existing campaign directory unchanged;
2. keep PF01 rerun, PF02–PF08 and every scored call prohibited under current governance;
3. require new explicit governance authority before any proposed continuation; and
4. do not treat PF01 as a statistical baseline or select a remedy.

No code correction is authorized or required by this review.

## 8. Cross-model consistency confirmation

The independent Claude cross-model review also returned `ACCEPTED WITH QUALIFICATIONS` and reached the same substantive findings: genuine semantic failure, correct D/representation classifications, trusted production evaluation path, no harness/parser/driver defect, correct fail-closed stop, no Stage 1/2 authority, no remedy selection and no broad reliability inference. Its refinement that PF02–PF08 are presently not authorized is adopted in Section 4. The two independent reviews are consistent.
