**Status:** Unit 2 Defect Confirmation Review — **CONFIRMED / CORRECTED**. Classification **B — UNIT 2 IMPLEMENTATION DEFECT; extractor incorrectly assumed a flat model object**. Baseline `c08f141`. Stage 0 remains not started and no inference trial was recorded.

# Unit 2 Ollama Model Identity Extraction — Defect Confirmation Review

## 1. Evidence and classification

The Ubuntu preflight called `GET /api/tags` and `POST /api/show`, then failed before `DurableCampaignRunner.runBatch`. The supplied `/api/tags` evidence contains the exact model and full digest:

```text
qwen2.5-coder:7b
dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364
```

The response SHA-256 was reported as `e4e378430199b4236d909ab40aa8a241c39c59a0b9aa8730245b46599c81d2df`. Its matching model object contains nested `details` and a `capabilities` array.

The committed regex used `[^{}]*` both before and after the digest and required the enclosing model-object `}` immediately after content containing no brace. The nested `{` beginning `details` prevented that match even though the digest was present and exact.

Classification:

```text
B — UNIT 2 IMPLEMENTATION DEFECT; extractor incorrectly assumes a flat model object
```

This is not an immutable-identity failure and not governance ambiguity. It is a structurally incorrect extraction mechanism.

## 2. Minimum correction

Only `tests/integration/ReasoningProtocolBaselineCharacterisationTest.kt` changed.

The field-order-dependent regex was replaced by a narrow evaluation-only scanner that:

1. requires a root JSON object;
2. locates one root `models` array;
3. isolates each direct array object using balanced container depth;
4. respects quoted strings, escapes, Unicode escapes, braces and brackets inside strings;
5. reads only direct string fields from each model object;
6. matches exact `name` or exact `model` equality;
7. requires exactly one matching object;
8. requires one direct, nonblank, full 64-hex digest; and
9. preserves the existing `/api/show` response SHA-256 check and configured-versus-captured identity equality.

It is not a general JSON parser. No dependency, production code, Unit 1 code, Ollama configuration, model setting, campaign definition, path, timeout, ledger or batch behavior changed.

## 3. Fail-closed properties

- No substring, prefix, abbreviated or first-object selection exists.
- A different exact model is selected only when that exact model was configured.
- Zero or multiple exact matching objects block.
- Missing, blank, abbreviated or non-hex digest blocks.
- Nested fields cannot impersonate direct `name`, `model` or `digest` fields.
- Duplicate direct fields block.
- Malformed strings, escapes, objects, arrays and values block.
- The captured digest must still equal the configured immutable digest byte-for-byte.
- The captured `/api/show` hash must still equal the frozen show-evidence hash.

## 4. Live-call accounting

```text
Live identity HTTP calls: OCCURRED
  GET /api/tags
  POST /api/show

Live model inference /api/generate calls: ZERO
Stage 0: NOT STARTED / NO TRIAL RECORDED
```

The supplied evidence states no campaign directory, intent, raw record, checkpoint or manifest exists for the attempted campaign, and failure occurred before the runner. Identity preflight requests are not Stage 0 inference trials.

## 5. Verdict

```text
DEFECT CONFIRMED: YES
CORRECTION: PASS
```

Corrective action remaining: **NONE**, subject to independent constitutional review and final integrity review. Do not rerun Stage 0 under this workflow.
