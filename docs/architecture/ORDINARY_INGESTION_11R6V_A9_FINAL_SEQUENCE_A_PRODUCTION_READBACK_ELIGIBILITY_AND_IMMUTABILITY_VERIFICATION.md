# OI11R6V-A9 — Final Sequence A Production Readback, Eligibility and Immutability Verification

## Verdict

**A — FINAL SEQUENCE A PRODUCTION SEMANTICS VERIFIED**

## Starting state and governance

The unit started on host `parker`, branch `main`, at clean HEAD/upstream `ba1d551f90cf9e2924be0e10db7f5a6fc508f06e`. The frozen R6T Scope Lock SHA-256 was `f90c9fc654136ea5e92723a1704ad58108ab0f8a9e73a401e8039d3210e4cd2a`; the frozen R6V Implementation Plan SHA-256 was `86f7b27095a6b80b2618556797a877a21b097f76c3c9ba2d91e235b90c395d1d`.

This was production verification only. No source, test, runtime, configuration, provider, derivative, review, audit, or other governed semantic state was modified.

## Exact deployed production artifact

Before and after verification, production was exact:

- Container: `e4d2c427776a4a8c103bfb8847ed0923acbb215c55b68803f94df89a0e8ae751`
- Image: `sha256:26a503564698b4ac248cb3e9d94ceba7813b713523dd9b995e73cbf922267895`
- Source: `e2e824c062c94ffe5b8b75a387a753de7d2f72ce`
- Runtime JAR SHA-256: `d78c99a389ba3a75e868d007ac13f0678f84ac25e46e7aeca41a513f510865e2`
- Runtime status/readiness: running / PASS (`Runtime starting`, `Runtime started`, and owner HTTP listener startup remained recorded)
- Restart count: `0`

No rebuild, deployment, or restart occurred.

## Canonical production owner retrieval procedure

The existing owner bearer credential was resolved locally without printing it. An authenticated `GET` was issued to the established production Tier A route:

```text
/owner/evidence/evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9/content/region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6
```

This exercised the deployed owner-authorized retrieval coordinator and A8B presentation integration. It did not infer semantics from store files alone. Repeated bounded GETs were used only to project separate identity, order, and text checks from the same pure read path.

## Canonical R6 retrieval and ordering result

The response returned:

- Status: `RETRIEVED`
- Kind: `REGION_TRANSCRIPTION`
- Exact generation: `region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6`
- Exact evidence: `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`
- Source SHA-256: `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`
- Preparation identity: `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f`
- Page bindings / region bindings / transcription blocks: `5 / 5 / 5`
- Page ordinals: `[1,2,3,4,5]`

Each `parkerSourceOrder` identity equalled the leading source-region identity in its corresponding canonical `regionBindings` entry, establishing Parker order `[1,2,3,4,5]`. Provider-returned order remains separately exposed as forensic provenance.

## Human-fidelity presentation and source-confirmed eligibility

The same retrieval response exposed the accepted A8B `humanFidelityStatus` fields with these exact values:

- `effectiveReviewState`: `HUMAN_REVIEWED_WITH_DISCREPANCY`
- `coverage`: `FULL_GENERATION`
- `materialDiscrepancyCount`: `2`
- `systematicPatternCount`: `1`
- `unresolvedConflict`: `false`
- `sourceConfirmedEligibility`: `DENIED`
- `sourceConfirmedDenialReason`: `MATERIAL_DISCREPANCY`

This is the required production evaluation for `SOURCE_CONFIRMED_WHOLE_GENERATION`. The denial is the correct successful governance outcome, not an operational failure. Raw provider retrieval remained independently available under existing owner retrieval authority.

## Provider transcription and human facts remain separate

The canonical provider transcription returned by the governed path still contains `Michael Gary Kellee` in transcription block/page 1 and transcription block/page 5. It contains no substituted `Michael Gary Kellec` at those provider locations.

The separately stored canonical review remains `review-3cf3186ca166acb0f4b6331ca574926dc874225247b296fb972666504992ea6e`, SHA-256 `13e6f5e285d95e19c0926821b63422486e005d22ee484feb70a6b54635046106`. Read-only inspection confirmed two `Kellee` provider values and two `Kellec` human source resolutions, two `MATERIAL` classifications, two `UNKNOWN` cause values, full coverage, and the formal discrepancy state. The established exact review facts bind source resolution `Kellec` to pages 1 and 5. No human resolution masqueraded as provider output and no merged or corrected transcript exists.

## Fail-closed and historical compatibility checks

An authenticated safe GET using established unknown generation `oi11r6q-unknown-generation` returned exactly `UNKNOWN_GENERATION`. It created no record and exposed no successful presentation.

An authenticated GET for historical searchable-PDF generation `090ce75f-22ab-4225-b731-c6367ebea5c6`, canonically bound to `evidence-0a86424a-b39d-4a75-9a5a-603e1e9b30ec`, returned `RETRIEVED`, kind `PDF`, completeness `ACCOUNTED_FOR_WITH_QUALIFICATIONS`. Historical Tier A retrieval compatibility therefore remains PASS.

## Governed state before and after

The bounded pre-verification snapshot and final snapshot were identical for every semantically relevant store. The final complete counts and aggregate SHA-256 values were:

| Store | Files | Aggregate SHA-256 |
|---|---:|---|
| Evidence | 29 | `e5d29f86bc047774082d0beb70f62b81d2b344b8666edfeb1b8d481f4fe27d85` |
| Evidence source manifests | 29 | `ec2a7dc1aad1efc9bac3763930564da11a0d5674140378e77d4f108228d76559` |
| Corrected preparation files | 6 | `0701859977ca979f1dfc64f605e550ee1e963104e445d17c0e361fb5b06b5b3d` |
| External-region owner authorizations | 14 | `b9b30ccee935874b8c1ccf397b88dd4b166e87d350a5b1cf42301af43f42c6f6` |
| External-transcription attempts | 10 | `798ed0fd9e4eff2b19085c33f94cb5a495fae9cb4c849659e35b7f1915c8e12f` |
| External-region provider state | 8 | `be1b33109ed42420d260e94c884af0c23a557bda201285feba7c06edc75845d6` |
| Derivative generations | 23 | `801bdfd3c4b4801ac1981cb48d90ccd1721fdd782d0d54326253de62ecb9b19f` |
| Derivative content | 21 | `80eee2bb97f2532b0a1b22e7fbfc6e59c8b8a3d15e98ad24b7969917e5f27435` |
| Document-ingestion audit | 1 | `3ff333aa80da829ad3454c1d35136a2f24e0997391c1da4a5ef9cc9604429359` |
| Human-fidelity reviews | 1 | `47e4ceb3f36117b436d0fbb389cc8a25f83b7bf8354dbab7219656963217444d` |
| Human-fidelity review audit | 3 | `14f131f4d1215830fe43eb51a6ea0b81ce76f77117d71c4e43e3ad90b3d5893d` |

The exact relevant records remained:

- Evidence bytes: `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`
- Corrected preparation record: `fe1111c75ee0307f755e03ac479dbb51ac7b9af7a4eeed715b9e6d1fffc18ae9`
- Authorization record/events: `48f4e5405fd298df9c492cd2cab95f65ea9abdfe2546cb951de5f0c9f0cd5544` / `64070a0d042373164902615edd03a19a4bdf7f602e911d7eecbcec8c31bcb675`
- Attempt ledger: `bff24b6c97ecd5382e514a0dc57f1f28c40509c0d483ba553d2db4d03b5d7591`
- Provider-state record/assessment: `c3f1ea29c9f1b4e76b886a33fdfd7a84400a4b96250d9899905493c844c9620c` / `bace05830d9a2872dbcbb78d3bc73b192dcfb179925def756fc6684604b87d01`
- Derivative generation: `9fb18b02db5ac55e5d446cd48ebc619de929c4596f94d2a11fba1a07da71af14`
- Derivative content: `18a6ed08a4729350027d3140dc0f07dd49d32c04aa45f9e3e9558df5d007c4eb`
- Canonical human review: `13e6f5e285d95e19c0926821b63422486e005d22ee484feb70a6b54635046106`

There remains exactly one R6 provider derivative and exactly one R6 human review. Human-fidelity audit facts remain exactly three. Governed semantic delta was zero.

## Historical provider state and boundary

Historical facts remained unchanged:

- Authorization: `ff286fdcc38a35aefed16201724c00d8a9930e2f73c08206571295a664127f97`
- Execution: `ordinary-exec-3c2bf685-d6c2-44e0-acf8-0224d92fd976`
- Provider-state identity: `2b1fbe06ebee0b7a3fdb618159c6987fa713976d7bfd2732b9048b50f11df3a7`
- Raw response SHA-256: `4706c24b8b0b83675a8ded1165f316229fa61a92bff4d8fe0a16c1d7d50cfb4a`
- Provider response ID: `resp_04aa0adc3e021174016a980c0c891487d09764395f58adef7b`
- Provider/model: `OpenAI` / `gpt-5.6-sol`
- Historical budget: maximum `1`, consumed `1`, retries `0`

OpenAI calls, Claude calls, other provider calls, retries, and external evidence egress during A9 were `0 / 0 / 0 / 0 / 0`. No external reasoning occurred.

## Completion boundary

Sequence A implementation and production semantic verification are complete. No correction purpose, correction proposal, correction acceptance, corrected representation, retranscription, or second provider derivative exists. The only remaining Sequence A programme work is OI11R6V-A10 closure.
