**Status:** Independent Constitutional Defect Review — **ACCEPTED**. The correction was independently checked against immutable-identity governance and actual code rather than accepted from the Defect Confirmation Review.

# Unit 2 Ollama Identity Extraction Defect — Independent Constitutional Review

## 1. Independent classification

The actual matching `/api/tags` object contains the required full digest. The former extractor failed only because its regex prohibited nested braces before the containing model-object close. Therefore the identity evidence was available and the implementation could not read it.

Independent classification:

```text
B — UNIT 2 IMPLEMENTATION DEFECT; extractor incorrectly assumed a flat model object
```

No governance ambiguity exists. Governance requires structural exactness, not this particular regex.

## 2. Exact-model and nested-JSON challenge

The replacement scanner does not search for a digest adjacent to a substring. It first identifies the single root `models` array, then isolates its direct object elements with JSON-aware depth. Strings and escapes are consumed before structural characters are interpreted. Only direct fields of each isolated model object participate in selection.

An object matches only when direct `name` or direct `model` equals the configured value exactly. Exactly one object must match. Its direct digest must be exactly 64 hexadecimal characters. Nested `details.name` or `details.digest`, a partial model, the first model, duplicate model entries, or an abbreviated digest cannot create authority.

## 3. Identity authority preserved

The correction does not weaken identity:

- environment digest alone remains insufficient;
- live `/api/tags` capture remains mandatory;
- `/api/show` is still called and its complete response hashed;
- captured digest must equal configured digest;
- captured show hash must equal frozen show hash;
- any absence, duplication, malformed evidence or mismatch blocks before the runner.

No retry, alternate endpoint, model substitution, abbreviated comparison or inference fallback was introduced.

## 4. Scope and consequence review

The scanner is private evaluation-only standard-library code in the already-authorized Unit 2 file. Production, Unit 1, prompt/parser/transport, model settings, artifacts, scheduling, exact-once behavior and downstream isolation are unchanged. The correction cannot reach Memory, Goal execution, Knowledge Submission or production state.

## 5. Evidence and status

- Unit 2: **PASS**, 19 tests, 0 failures, one live-entry skip.
- Unit 1: **PASS**, 16 tests, 0 failures, one live-smoke skip.
- Lifecycle isolation: **PASS**.
- Ordinary suite: only `OcrStructuralIsolationTest.kt:338`, unrelated.
- Live identity HTTP calls during the failed Ubuntu preflight: **OCCURRED**.
- Live `/api/generate` inference calls: **ZERO** based on failure-before-runner and absent campaign artifacts.
- Stage 0: **NOT STARTED / NO TRIAL RECORDED**.

## 6. Verdict

```text
ACCEPTED
```

Corrective action remaining: **NONE**. Do not rerun Stage 0 until the correction is separately committed/pushed and execution is explicitly authorized.
