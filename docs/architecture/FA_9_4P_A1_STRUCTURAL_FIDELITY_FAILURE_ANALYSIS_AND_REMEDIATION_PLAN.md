# FA.9.4P-A1 Structural-Fidelity Failure Analysis and Remediation Plan

**Status:** Accepted by FA.9.4P-A1E-R4 as the governance analysis and bounded remediation direction. This document authorises no implementation, provider request, authority, attempt, deployment, lifecycle change, or production-store mutation.

**Baseline:** Production commit `5501cece391d0e1d69bdbbfe4a62ae21cca235c9`; failed generation `8e993aff-3518-445a-991d-e0270b98d510`; human review `review-fa-9-4p-a1e-3ec4e279-8025-49a1-9d43-0e9131c8b148` (`REVIEW_FAILED`).

## 1. Determination

FA.9.4P-A1 completed its governed provider and admission transaction, but failed human source-fidelity acceptance. The two material defects are:

1. On page 2, the emphasized proposition visually following “The initial proposition is simply:” was emitted later, after “Its operative determination should ultimately be:”.
2. On page 3, closing prose was emitted before the authorization block although the visual source places the authorization block first.

The defects are first **demonstrably present in the persisted/admitted page text**. Parker's current code does not reorder the provider's blocks: it requires returned `block_order` values to be exactly `1..n`, joins the returned block array in that order, validates the resulting page text without source geometry, and persists it without reordering. The raw HTTP response, the extracted structured-output JSON, and the individual block objects were not persisted. Consequently, the exact raw provider ordering cannot now be proved from durable evidence.

The required localization for both defects is therefore:

`RAW_PROVIDER_STATE_NOT_PERSISTED_SO_NOT_PROVABLE`

The strongest implementation-supported hypothesis is that the provider's structured block array already carried the wrong order. That remains an inference, not a durable raw-response fact. No evidence supports adapter, admission, persistence, or review-rendering reordering.

The architectural structural-validation gap is broader than prompting: the provider-relative block_order field has no independent source-position anchor. Parker correctly labels the transcription unverified, but its deterministic admission checks only internal sequence consistency, not correspondence to authoritative page geometry.

## 2. Evidence examined and chain of custody

The authoritative implementation and runtime analysis used the isolated Ubuntu clone `/tmp/parker-fa-9.4p-a1e-r3-20260829` at the frozen commit and read-only copies of production artifacts. Earlier Windows visual inspection was retained only as a provisional forensic observation and was bound to the same custody-PDF digest; every implementation conclusion affecting localization was revalidated on Ubuntu. Source truth was the visually rendered custody PDF, not native extraction, PDF logical text order, or the admitted transcription.

| Artifact | Production original | Isolated copy | Bytes | SHA-256 |
|---|---|---|---:|---|
| Authoritative PDF custody bytes | `/data/evidence/evidence-0275472f-535a-4cf1-b30d-f45ac7684743.evidence` | `forensics/authoritative.evidence` | 111122 | `7373ad403b4fae5bf5c777deb8524eaa3ba38594ce9fabfa8fcbce22fbd33182` |
| Source manifest | `/data/evidence-source-manifests/evidence-0275472f-535a-4cf1-b30d-f45ac7684743.manifest` | `forensics/source.manifest` | 196 | `4022d3673845e99d8d41358e9dbe7da9d35ea5aecb2d47347c0ed6b4b963d6a3` |
| Acceptance authority | `/data/external-transcription-acceptance-authorities/authority-fa-9.4p-a1-r2-2f5a4813-cccb-4277-9f18-36814d737534.acceptance-authority` | `forensics/authority.acceptance-authority` | 1178 | `889b7c138fa9ef274b41e3f20bbb79411bf655e0bca11947d2a87e3dc5e8aee4` |
| Attempt ledger | `/data/external-transcription-attempts/parker-encoded-execution-fa-9-4p-a1-r2-e0d31561-65f7-4--29e83b013096be59a328e3cc3dfcdf4b6b61804a4c092af90e84dc08a42150c8.fidelity-attempt-ledger` | `forensics/attempt.fidelity-attempt-ledger` | 2148 | `40f64fff0310c3bf53397cf20c1c5615c4f17096f181b0fc6adf4b5a0638b733` |
| Generation record | `/data/derivative-generations/8e993aff-3518-445a-991d-e0270b98d510.derivative` | `forensics/generation.derivative` | 417 | `2340197b79383a284018cbd303dbaa560791c623a17661fe34d0e8ea247e3f85` |
| Derivative content | `/data/derivative-content/8e993aff-3518-445a-991d-e0270b98d510.content` | `forensics/content.content` | 8846 | `a106e3c575270d3eaab3c1e14915a789d26ae83d82f7845a6e95b4cd63289895` |
| Human review | `/data/saved-analyses/human-verification/review-fa-9-4p-a1e-3ec4e279-8025-49a1-9d43-0e9131c8b148.human-verification` | `forensics/review.human-verification` | 865 | `e8d85c1e2d55d52a145ab30b4e19dbc182711caf0b5a65842720d8ea925c1960` |
| Ingestion audit | `/data/document-ingestion-audit/audit.log` | `forensics/document-ingestion-audit.log` | 15246 | `4f585b3d81e51c720a7dd72ff6a79219b701dd2deba231736243297ec406406e` |

Original and copied byte lengths and hashes matched. No credential material was copied or inspected.

The attempt ledger's ordered stages are `AUTHORISED`, `PREFLIGHT_PASSED`, `SOURCE_RETRIEVED`, `REQUEST_PREPARED`, `PROVIDER_ATTEMPT_STARTED`, `PROVIDER_RESPONSE_RECEIVED`, `GENERATION_ADMITTED`, and `TERMINAL_SUCCESS`. This proves one completed governed transport/admission transaction; it does not prove human fidelity acceptance.

### Ubuntu revalidation

The isolated Ubuntu checkout resolved to `5501cece391d0e1d69bdbbfe4a62ae21cca235c9` with a clean baseline before the document and forensic-copy directory were added. The following exact production-commit files were inspected on Linux:

- `OpenAiResponsesExternalTranscriptionAdapter.kt` (`0b0faec9f5cbef15279676fe97a2e8d8a23ed4597bcf8ecc1d4c9ec89a682ac5`);
- `OcrStructuredResultValidator.kt` (`f75d9073886a45080514d00812366641a44809a4646209ddbcf5d3963fc07991`);
- `DerivativeGenerationCoordinator.kt` (`e96d0b7d3819ebec860b2bd7a4c921e339f40993cf35b32dc88f9dcc534e8493`);
- `DerivativeContentCodec.kt` (`576bdee4ecb3d6f4f6ecebc5f20477a35bc43564cfa06aae1b65dd20e5406b21`);
- `OcrProcessingRepresentationFactory.kt` (`1dcea364fdc3cbb9ca8a21e8ff2a146419a3c7b7e421f0c642b81bc7c3a8bc14`);
- `ExternalTranscriptionOwnerInvocationCoordinator.kt` (`34e8820edf80fb1bbb7a84508561bd022753e06e4aa2e3158a9d6eb764059cf6`).

Existing tests `FidelityFirstExternalTranscriptionTest`, `OcrStructuredResultValidatorTest`, `DerivativeContentCodecTest`, and `FidelityFirstAcceptanceCoordinatorTest` passed under `./gradlew test --offline` in the isolated Ubuntu clone. They use fake mechanisms/transports; no provider request was possible or made. The tests corroborate deterministic preservation and internal validation of returned ordering. They do not supply the missing source-geometry oracle and therefore cannot establish fidelity to the page.

## 3. Provider-response persistence boundary

### Persisted

- admitted flattened recognized text;
- one text segment per page and page number;
- requested, submitted, and returned page scopes;
- page outcomes, warnings, and converted uncertainty character spans;
- processing provenance, source and representation digests and lengths;
- provider, adapter, model, profile, response correlation ID, and instruction/schema digests;
- generation metadata, attempt stages, audit metadata, and human review metadata.

### Not persisted

- raw HTTP response body;
- complete Responses API envelope;
- extracted `output_text` structured JSON;
- the `pages[].blocks[]` objects;
- block `kind` and `block_order` after flattening;
- provider usage metadata;
- objective source coordinates or regions.

Thus Parker persisted category **D: only admitted/transformed generation content**, augmented by provenance and accounting metadata. It did not persist A, B, or C as independently reviewable artifacts.

## 4. Exact effective request semantics

The adapter constructed one `POST https://api.openai.com/v1/responses` request with:

- model `gpt-5.6-sol`;
- `store=false`, `stream=false`, and reasoning effort `none`;
- authoritative PDF bytes embedded directly as an `input_file` data URL named `source.pdf` with detail `high`;
- a short user instruction carrying the frozen profile, safe request identity, safe attempt identity, maximum page count, and no independently established expected PDF page count;
- the frozen fidelity-first developer instruction;
- strict `json_schema` output named `parker_page_transcription`;
- no web search, file search, code interpreter, previous response, native extraction, page render, region coordinates, or precomputed visual layout.

The source was a defensive byte-exact copy of verified custody bytes. Parker generated no page image and no visual region before transmission. OpenAI alone interpreted the PDF and established the block order.

Fresh digest reconstruction from current source produced:

- instruction: `38e4b87e3a429dac8ed5de91e5e2c94ad3d10cd739db186d41d09ed940b11b88`;
- canonical schema: `7b46bdd6ce615592bb4e7cfee84f5ec5f6fde546d678e13bdad0555f829a3313`.

Both match the authority and attempt ledger.

One separate provenance discrepancy was found: the authority/configuration identity is `external-transcription.direct-authoritative-byte-v1`, while `OcrProcessingRepresentationFactory` stamps the persisted representation `external-transcription.direct-byte-exact-v1`. The bytes are demonstrably direct and byte exact, so this did not cause either ordering defect. It should nevertheless be reconciled in the future implementation because the configured and persisted processing-profile names currently diverge.

## 5. Instruction analysis

| Requirement | Current classification | Basis |
|---|---|---|
| Source visual reading order | **EXPLICIT**, but not operationally anchored | “layout-significant reading order” and “deterministic reading order” are required. |
| Preserve block relationships | **AMBIGUOUS** | Block ordering is explicit; relationships among heading, emphasis, and following block are not defined. |
| Preserve emphasized block position | **ABSENT** | Emphasis and its positional relationship are not named. |
| No semantic regrouping | **IMPLICIT** | Summarizing, paraphrasing, normalizing, inferring, and reconstructing are prohibited, but regrouping is not named. |
| Do not move text to a seemingly logical heading | **ABSENT** | No explicit anti-relocation rule exists. |
| Top-to-bottom/source-order sequencing | **AMBIGUOUS** | Deterministic visible order is requested, but no geometric rule or supported-layout algorithm is specified. |
| Preserve authorization/signature/form blocks in visual position | **ABSENT** | These block relationships are not named. |
| Separate transcription from interpretation | **EXPLICIT** | “Transcribe only,” with summarization, inference, reconstruction, and correction prohibited. |

Prompt wording recognizes ordering as important, but it cannot make the provider's self-reported order independently verifiable.

## 6. Schema analysis

| Information | Schema state | Finding |
|---|---|---|
| Page number | **PRESENT_AND_REQUIRED** | `page_number` |
| Block sequence | **PRESENT_AND_REQUIRED** | Array order plus `block_order` |
| Stable block ordinal | **PRESENT_AND_REQUIRED**, but provider-relative only | Consecutive integer; no source anchor |
| Block type | **PRESENT_AND_REQUIRED** | `kind` enum |
| Literal text | **PRESENT_AND_REQUIRED** | `text` |
| General character/span mapping | **ABSENT** | Only uncertainty observations are block-associated |
| Visual position | **ABSENT** | No x/y/page region |
| Bounding box/coordinates | **ABSENT** | No geometry |
| Parent/child relationship | **ABSENT** | No structure graph |
| Heading association | **ABSENT** | No relationship field |
| Emphasis relationship | **ABSENT** | No style or association field |
| Reading-order predecessor/successor | **ABSENT** | Only provider-returned ordinal |
| Source-region identity | **ABSENT** | No deterministic region ID |
| Uncertainty | **PRESENT_AND_REQUIRED** | Array and item fields are required; array may be empty |

“Ordered blocks” currently means only that the provider returns an array whose `block_order` values are consecutive and agree with array position. Parker does not verify that this order corresponds to authoritative visual position.

## 7. Page 2 forensic trace

| Layer | Available fact | Position result |
|---|---|---|
| Authoritative visual source | The emphasized proposition immediately follows “The initial proposition is simply:” and precedes the separate-evidence-stream paragraph. | Correct source order. |
| Request requirement | General layout-significant and deterministic reading order required; no emphasis relationship or objective coordinates supplied. | Intended, not objectively anchored. |
| Raw provider response | Not persisted. | Unknown. |
| Parsed structured output | Not persisted as blocks. Code requires consecutive ordinals and joins the returned array without sorting or regrouping. | Exact raw block identity unknown; parser has no reordering operation. |
| Admission input | Flattened page text has the proposition after “Its operative determination should ultimately be:”. | Wrong. |
| Admitted generation | Same flattened order. | Wrong. |
| Reviewed representation | Governed API returns the same page text; human review records `REVIEW_FAILED`. | Wrong, faithfully displayed. |

**First demonstrable defect layer:** admission input/admitted representation.

**Localization:** `RAW_PROVIDER_STATE_NOT_PERSISTED_SO_NOT_PROVABLE`.

**Most likely hypothesis:** provider structured output already wrong, because every available Parker transformation preserves returned order.

## 8. Page 3 forensic trace

| Layer | Available fact | Position result |
|---|---|---|
| Authoritative visual source | Authorization block appears first; closing prose follows it. | Correct source order. |
| Request requirement | General layout-significant and deterministic reading order required; no authorization-block rule or geometry supplied. | Intended, not objectively anchored. |
| Raw provider response | Not persisted. | Unknown. |
| Parsed structured output | Not persisted as blocks; code only joins returned array order. | Exact raw block identity unknown; parser has no reordering operation. |
| Admission input | Closing prose precedes authorization block. | Wrong. |
| Admitted generation | Same flattened order. | Wrong. |
| Reviewed representation | Governed API returns the same page text; review correctly exposes the failure. | Wrong, faithfully displayed. |

**First demonstrable defect layer:** admission input/admitted representation.

**Localization:** `RAW_PROVIDER_STATE_NOT_PERSISTED_SO_NOT_PROVABLE`.

**Most likely hypothesis:** provider structured output already wrong.

## 9. Structural-fidelity gap and failure-pattern comparison

The current profile cannot prove structural/source-order fidelity from provider output alone. Its only ordering evidence is the provider's own ordinal. A consecutive sequence proves internal schema consistency, not that block A visually preceded block B on the authoritative page.

The historical native extraction and GPT-5.6 direct-PDF transcription independently displaced the same page-2 proposition after the later operative heading. The external result additionally reversed page 3's authorization-block/prose order. The supported common pattern is that both mechanisms failed to preserve visually intended relationships in a PDF whose logical/content-stream ordering differs from visual order. The mechanisms and Parker post-processing paths differ, and current adapter/admission code contains no common semantic reorder. Therefore evidence supports **the same specific page-2 relationship failing independently, within a broader common inability to bind output to source geometry**; it does not support Parker post-processing as the common cause.

Existing tests prove request construction, strict schema compliance, identity binding, page uniqueness/order, consecutive returned block ordinals, deterministic joining, uncertainty conversion, provenance consistency, durable codec round trips, and no retry. They do not compare provider blocks against an external source-geometry oracle. The missing oracle is a governed expected mapping from authoritative page regions and geometric reading-order relationships to transcription blocks.

## 10. Minimum remediation requirements

1. **Objective source-order anchoring:** order must derive from authoritative page geometry, not provider ordinal alone. This addresses both observed reorderings.
2. **Page/region provenance:** every transcribed block must bind to source digest, page, stable region ID, and coordinates.
3. **Block-position preservation:** admission must reject missing, duplicate, overlapping beyond policy, out-of-range, or reordered region bindings.
4. **Emphasized-proposition relationship:** the page-2 fixture must encode that the emphasized region follows its introducing sentence and precedes the next paragraph.
5. **Authorization-block order:** the page-3 fixture must encode that the authorization region precedes closing prose.
6. **No semantic regrouping:** text cannot move between regions or headings even when another placement appears semantically fluent.
7. **Deterministic admission validation:** source-region coverage and order must be validated independently of provider claims; ambiguous layouts fail closed or require review.
8. **Human-readable review mapping:** owner review must show source page/region beside the associated transcription block and ordinal.
9. **No silent reorder and forensic comparability:** any deterministic reconstruction must be disclosed in processing provenance and must retain enough original provider-returned structured state to compare provider-returned order with Parker-admitted or reconstructed order. Credentials, hidden provider internals, and unnecessary sensitive transport metadata must not be retained.
10. **Fidelity over fluency:** complete fluent text with invalid source relationships remains failed.

## 11. Candidate remedies

### Option 1: Strengthen instruction and schema only

- **Mechanism:** explicitly prohibit relocation and semantic regrouping; add heading/emphasis/relationship fields.
- **Surfaces:** adapter instruction/schema, profile configuration, parser, codecs, tests, governance.
- **Benefit:** clearer provider contract and better review metadata.
- **Residual failure:** all structural facts remain provider assertions; cannot independently prove visual order.
- **Direct-source compliance:** yes.
- **Deterministic verifiability:** low.
- **Provider dependence:** high.
- **Complexity:** low to medium.
- **Future provider attempt:** required for empirical validation.
- **Historical A1 offline use:** useful only as a negative flattened-output fixture; raw blocks unavailable.

### Option 2: Direct authoritative page render, deterministic regions, region-bound transcription, geometry reconstruction

- **Mechanism:** render pages deterministically from custody bytes; segment supported layouts into stable regions; bind each request/output block to source digest, page, region ID, and coordinates; reconstruct order from source geometry; fail closed on ambiguous segmentation; retain human review.
- **Surfaces:** processing representation/provenance, new region contracts, adapter/schema/profile, validator/admission, content codec, review API/UI, tests, governance.
- **Benefit:** objective source-order evidence and targeted review; directly addresses both defects.
- **Residual failure:** bad segmentation, unusual layouts, text within a region, or transcription inaccuracies; bounded by fail-closed layout classification and review.
- **Direct-source compliance:** yes, when render and regions are deterministic direct processing representations of custody bytes.
- **Deterministic verifiability:** high for region order and mapping.
- **Provider dependence:** transcription remains provider-dependent; ordering does not.
- **Complexity:** medium to high, but bounded if initially limited to supported single-column/block layouts.
- **Future provider attempt:** required only after implementation, deployment, and new authority.
- **Historical A1 offline use:** yes as negative evidence; authoritative PDF geometry supplies source truth.

### Option 3: Direct PDF transcription plus separate source-image visual-order verification

- **Mechanism:** retain direct-PDF output, independently render pages, and verify/map returned blocks against source regions before admission.
- **Surfaces:** verifier, mapping contracts, schema, validator/admission, provenance, UI, tests.
- **Benefit:** retains PDF-direct strengths while adding an independent order gate.
- **Residual failure:** matching long flattened text back to regions may be ambiguous; a model-based verifier would add another provider dependency and request budget.
- **Direct-source compliance:** yes if verification images derive directly from custody bytes.
- **Deterministic verifiability:** medium if matching is deterministic; low if model asserted.
- **Provider dependence:** medium to high.
- **Complexity:** medium to high.
- **Future provider attempt:** required to validate the full mechanism; potentially two calls if verification is model-based, which is not preferred.
- **Historical A1 offline use:** useful for deterministic mapping experiments against source geometry.

### Option 4: Change provider/model while retaining the current geometry-free contract

- **Mechanism:** evaluate another strong transcription model with the same direct-PDF schema.
- **Surfaces:** provider profile, adapter/configuration, governance, acceptance tests.
- **Benefit:** may improve empirical performance.
- **Residual failure:** no objective source-order proof; repeats the same architectural weakness.
- **Direct-source compliance:** yes.
- **Deterministic verifiability:** low.
- **Provider dependence:** very high.
- **Complexity:** medium governance, low code.
- **Future provider attempt:** required.
- **Historical A1 offline use:** only as comparative negative evidence.

## 12. Preferred remediation

Select **Option 2: deterministic direct-source page/region anchoring with region-bound transcription and geometry-derived order reconstruction**.

It offers the highest expected source fidelity with bounded complexity because it moves the critical order fact out of provider self-report and into a deterministic representation derived directly from authoritative bytes. Prompt/schema strengthening remains useful inside this design, but is insufficient alone. A provider/model change would not close the proof gap. A post-hoc visual verifier retains difficult text-to-region matching and may require a second model call.

The first implementation should support a deliberately narrow layout class and fail closed to human review for ambiguity. It must not claim universal layout reconstruction.

## 13. Smallest coherent future implementation unit

Likely production/runtime surfaces:

- `src/runtime/OcrProcessingRepresentationFactory.kt`: deterministic PDF page rendering and transformation provenance, or a new narrowly named direct-source page-region factory;
- `src/interfaces/OcrTranscriptionProvenance.kt`: page/region identity, coordinates, render parameters, and source binding;
- `src/interfaces/OcrStructuredTranscription.kt`: provider block-to-region candidate contract;
- `src/composition/OpenAiResponsesExternalTranscriptionAdapter.kt`: new request, instruction, strict schema, parsing, and preservation of provider block metadata;
- `src/composition/OpenAiExternalTranscriptionProviderProfile.kt`: new schema/profile identity and lifecycle configuration;
- `src/runtime/OcrStructuredResultValidator.kt`: independent region coverage/order and ambiguity validation;
- `src/runtime/ExternalTranscriptionOwnerInvocationCoordinator.kt`: direct-source representation selection and fail-closed propagation;
- `src/runtime/DerivativeGenerationCoordinator.kt` and `src/runtime/DerivativeContentCodec.kt`: persist region-bound text and provenance without flattening away the review structure;
- `src/composition/OwnerUiEvidenceRuntimeAdapter.kt`, `src/composition/OwnerEvidenceHttpServer.kt`, and owner DTOs: bounded side-by-side region review projection.

Required tests:

- deterministic render provenance and byte/source binding;
- stable supported-layout region IDs and coordinates;
- ambiguous-layout fail-closed behavior;
- schema rejects missing, duplicate, foreign, or inconsistent region IDs;
- validator rejects provider ordinal inconsistent with source geometry;
- page-2 fixture proves introducing sentence → emphasized proposition → following paragraph;
- page-3 fixture proves authorization block → closing prose;
- no semantic relocation despite token completeness;
- codec round-trip preserves regions, coordinates, ordinals, types, uncertainty, and provider-relative facts;
- UI/API exposes exact source-page/region mapping;
- historical A1 generation remains readable and `REVIEW_FAILED`;
- no native-extraction or OCR-derivative cleanup path;
- no retry/fallback and exact authority/commit binding.

Deployment is required before any empirical acceptance. The existing `openai-fidelity-first-transcription-v1` and schema v3 meanings are frozen by their digests and lack objective region semantics. The remedy therefore requires a **new capability/profile and schema version**, not an in-place mutation. Exact identifiers should be frozen by the implementation authority; a suitable planning placeholder is fidelity-first region transcription v2 with schema v4.

The processing-profile identity mismatch identified in Section 4 must also be resolved so configured authority and persisted processing provenance name the same frozen direct-source mechanism.

## 14. Regression and human-review strategy

The authoritative PDF/page geometry remains the oracle. Historical provider output is only negative evidence.

Create governed expected region annotations for the acceptance fixture:

- source digest and render profile;
- page number, stable region ID, bounding box, block class, and geometry-derived ordinal;
- page 2 predecessor/successor assertions around the emphasized proposition;
- page 3 assertion that the authorization region precedes closing prose.

Offline tests should feed deliberately reordered synthetic provider blocks through the parser/validator and require rejection or deterministic reconstruction from the governed geometry. Token equality must not satisfy these tests.

The owner review surface needs only targeted additions: source page image/region, transcription block, source-derived ordinal, block type, and uncertainty, shown side by side. Confidence is optional unless it is calibrated and source-grounded. The UI need not become a general document editor.

## 15. Authority, lifecycle, and historical evidence

The consumed authority `authority-fa-9.4p-a1-r2-2f5a4813-cccb-4277-9f18-36814d737534` must never be retried. Any future empirical provider test requires completed implementation, deployment, exact deployed commit binding, a new immutable authority, a new governed attempt ID, and separate authorization for one provider transmission.

Generation `8e993aff-3518-445a-991d-e0270b98d510` and its `REVIEW_FAILED` record remain immutable negative acceptance evidence. They must not be deleted, replaced, corrected, or reclassified.

`TERMINAL_SUCCESS` is technically correct for the provider/admission transaction because its ledger ends before human review. It is nevertheless easy to misread in reports. No runtime change is necessary for the structural remedy; a separate bounded governance/observability clarification should require presentation as `TRANSACTION_TERMINAL_SUCCESS` or always pair the ledger terminal state with the independent human acceptance state.

Unit O remains unchanged. O.5 remains reserved and unconsumed.

## 16. Demonstrated facts, inferences, and unresolved questions

### Demonstrated

- authoritative page-2 and page-3 visual order;
- wrong admitted order and `REVIEW_FAILED`;
- exact frozen request instruction/schema digests;
- direct byte-exact inline PDF input and no Parker page rendering/regions;
- raw response and structured block objects are not persisted;
- adapter joins provider-returned blocks without reordering;
- validator/admission check internal consistency, page accounting, uncertainty, and provenance, not source geometry;
- persisted processing-profile name differs from authority/configuration processing-profile name.

### Inferred

- provider structured blocks most likely already carried the wrong order;
- the PDF's difficult logical-versus-visual structure likely contributed to both native and external failures.

### Unresolved

- exact raw provider block array and block kinds for A1;
- whether a different direct-PDF model would preserve this layout more reliably;
- the narrowest deterministic segmentation policy that covers this witness without claiming unsupported layouts;
- whether provider calls should be per page or per bounded region after geometry is established;
- the final versioned capability/profile/schema identifiers and acceptance budget.

These questions require future governance and, where empirical, a separately authorized provider attempt. They do not prevent selecting the objective-region remediation direction.

## 17. Recommended next unit

`FA.9.4P-A1E-R4 — REVIEW, ACCEPT, AND COMMIT THE STRUCTURAL-FIDELITY FAILURE ANALYSIS AND BOUNDED REMEDIATION PLAN`

R4 should review this document and its evidence, resolve any governance wording, and—only if accepted—authorize committing the plan. It must not implement the remedy or authorize a provider call.
