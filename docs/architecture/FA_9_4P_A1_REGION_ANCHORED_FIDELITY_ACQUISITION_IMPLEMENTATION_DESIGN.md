# FA.9.4P-A1 Region-Anchored Fidelity Acquisition Implementation Design

**Unit:** FA.9.4P-A1E-R5
**Status:** Accepted by FA.9.4P-A1E-R5A as the governed implementation-design direction. No implementation, provider execution, deployment, acceptance execution, lifecycle change, or production-store mutation is authorised.
**Authoritative design basis:** Ubuntu repository commit `2280991c536a492139bd48daf70780afbdb0198e` and accepted R4 document SHA-256 `b0e929ddc0351af27f10eff00a3c4f184f585a1533c8ec56b0a4479befe8186c`.

## 1. Decision and invariants

Parker shall acquire fidelity from immutable authoritative custody evidence, never from the fluency, completeness, or self-reported ordering of a transcription mechanism. Searchability is not fidelity. Agreement between mechanisms is corroboration, not source authority.

Every mechanism shall consume either the authoritative custody bytes or a deterministic processing representation derived directly from those bytes. Native extraction, OCR output, or one provider transcription shall never be input to another evidence transcription. A human correction is a new provenance-bearing record or successor derivative; it never overwrites historical machine output.

The governed chain is:

`authoritative artifact -> deterministic pages -> deterministic regions -> independently region-bound transcription(s) -> geometry-derived reconstruction -> targeted source-grounded review -> provenance-bearing generation`.

## 2. Proposed governed identities

These names are proposals for the next governance unit, not accepted runtime configuration:

| Identity class | Proposal |
|---|---|
| capability | `region-anchored-fidelity-acquisition-v1` |
| external transcription profile | `openai-region-anchored-transcription-v1` |
| provider-neutral structured schema | `region-anchored-transcription-schema-v1` (wire `schema_version = 4`) |
| processing representation profile | `authoritative-page-region-raster-v1` |
| adapter | existing adapter family with a new region-aware adapter version; do not relabel old bytes |
| region derivation profile | `deterministic-page-regions-v1` |
| reconstruction profile | `geometry-reading-order-v1` |
| comparison profile | `literal-region-comparison-v1` |

Frozen `openai-fidelity-first-transcription-v1`, schema v3, instruction digest `38e4b87e3a429dac8ed5de91e5e2c94ad3d10cd739db186d41d09ed940b11b88`, and schema digest `7b46bdd6ce615592bb4e7cfee84f5ec5f6fde546d678e13bdad0555f829a3313` remain unchanged.

## 3. Deterministic page representation

### 3.1 Contract

`SourcePageRepresentation` shall contain:

- `EvidenceArtifactId`, authoritative source SHA-256, media type, and manifest digest;
- one-based page number and declared source page count;
- renderer identity, exact renderer version/build digest, and processing-profile identity;
- complete render parameters: requested DPI, applied rotation, crop box/page box, colour mode, alpha policy, antialiasing policy, output encoding, and compression parameters;
- canonical pixel width and height after rotation;
- SHA-256 and byte length of canonical rendered bytes;
- creation timestamp and deterministic transformation provenance.

Supported v1 input is PDF and custody-held raster image media already accepted by Parker. PDF pages are rendered independently. Raster evidence is decoded and canonicalized as a single page only when its media profile is governed. Native extracted text is never a page-rendering input.

### 3.2 Determinism and failure

R6.1 must select a renderer only after Linux byte-reproducibility, maintenance, security, PDF feature coverage, licensing, and resource-bound tests. Candidate families may include Poppler or PDFium; R5/R5A does not select one. The profile will freeze executable/library identity and every material parameter only after that selection. A 300 DPI sRGB PNG is merely a proposed R6.1 empirical/technical-selection baseline, not a frozen architectural requirement; resolution, colour space, and raster format remain unresolved until supported by R6.1 evidence.

Intrinsic PDF rotation is normalized once and recorded. No content-aware auto-rotation is allowed unless separately deterministic, versioned, and recorded. Password protection, unsupported features, page-count conflict, resource limit, decode error, nondeterministic output, or digest mismatch fails closed. No silent renderer fallback is allowed.

## 4. Coordinate and region model

### 4.1 Coordinate system

All persisted region geometry uses integer normalized coordinates in a top-left origin Cartesian raster frame with half-open bounds `[left, top, right, bottom)` and fixed scale `1_000_000` per page dimension. The source pixel dimensions and exact pixel bounds are also retained. Normalized coordinates make identity independent of storage rescaling while the processing-representation digest binds the region to exact rendered bytes.

Normalized coordinates never replace exact page-render pixel geometry. R6.1/R6.2 must select and prove a canonical integer conversion and rounding specification, including forward and reverse conversion, boundary clamping, degenerate-box rejection, overflow behavior, and reproducibility tests at representative page dimensions. Until those rules are governed, the scale and coordinate model are accepted architecture but not an implementable wire encoding.

Bounds must satisfy `0 <= left < right <= 1_000_000` and `0 <= top < bottom <= 1_000_000`. Polygonal geometry is deferred; v1 uses axis-aligned rectangles. A layout requiring unsafe nonrectangular interpretation is `NOT_YET_SUPPORTED` or `HUMAN_ORDER_REQUIRED`.

### 4.2 Stable source-derived identity

`SourceRegion` contains:

- artifact ID and authoritative source SHA-256;
- page number, page-representation identity and page digest;
- coordinate system/version, normalized bounds, pixel bounds, and page dimensions;
- region derivation profile/mechanism/version;
- structural class from a bounded nonsemantic vocabulary;
- geometry-derived order key or explicit ambiguity group;
- region processing-representation digest;
- transformation provenance.

The deterministic region ID is a lowercase SHA-256 over a domain-separated, versioned, canonical length-prefixed serialization of: identity scheme/domain, identity version, artifact ID, source digest, page number, page digest, coordinate-system identity, normalized bounds, region-derivation profile/version, and structural class. Provider output, timestamps, random values, inferred legal meaning, and transcription text are excluded. Re-running identical governed inputs must reproduce the same ID. R6.2 must govern the exact domain tag and serialization; R5A does not freeze their byte format.

The region digest binds canonical cropped raster bytes plus page digest and bounds. The cropped bytes are derived directly from the page representation, which derives directly from custody bytes; they are not derivative transcriptions.

## 5. Region derivation

V1 should use a bounded hybrid mechanism: deterministic connected-component/layout analysis creates primitive source-grounded boxes; governed rules merge them into whole-width line or paragraph candidates only where objective spacing and alignment thresholds permit. Thresholds and algorithms are profile-bound. Region classes are limited to observable structure such as `PROSE`, `HEADING_LIKE`, `LIST_ITEM`, `TABLE`, `FORM_FIELD`, `HEADER_FOOTER`, `IMAGE`, `ANNOTATION`, `SIGNATURE_AUTHORIZATION`, and `UNKNOWN`. Names describe geometry/presentation, not legal meaning.

No provider may create, resize, merge, split, or name an authoritative region. Provider-suggested structure may be retained as an assertion but cannot alter the source-region set.

If segmentation is unstable, clips visible glyphs, overlaps incompatibly, produces unaccounted content, exceeds limits, or encounters an unsupported layout, derivation fails closed or marks the page `HUMAN_ORDER_REQUIRED`. A bounded whole-page region may be used only for objectively single-flow pages and never to claim detailed order that its geometry cannot prove.

## 6. Geometry-derived reading order

Ordering inputs are page number, normalized bounds, containment/overlap relations, column bands, baseline bands, structural class, and explicit ambiguity flags. Provider array position is excluded.

Deterministic rules are:

1. pages ascend numerically;
2. remove governed repeating header/footer bands from body flow while preserving them as regions;
3. identify nonoverlapping column bands using frozen geometric thresholds;
4. within a proven single band, order by top edge, then left edge, then bottom/right edge, then region ID as a deterministic tie-breaker that does not resolve semantic ambiguity;
5. for proven columns, traverse columns in the profile-declared writing direction and top-to-bottom within each column;
6. contained regions follow their container's class-specific rule only when that rule is supported;
7. overlapping or crossing relations that could change meaning create an ambiguity group and cannot be silently linearized.

Tables remain a structural region with cell geometry only when row/column boundaries are objectively derivable. Otherwise they are `HUMAN_ORDER_REQUIRED` or `NOT_YET_SUPPORTED`. Forms pair labels and fields only under a separately tested geometry rule. Sidebars/annotations are preserved outside main flow with an explicit anchor or ambiguity. Signature/authorization blocks are ordered by their source bounds, never semantic expectations. Mixed image/text pages retain image regions and text regions; no image meaning is inferred.

Ambiguous columns, overlaps, floating boxes, unclear table cells, uncertain header/footer classification, or competing valid traversal paths fail closed for automatic document reconstruction and require targeted human order verification.

## 7. Region-bound transcription contract

`RegionTranscriptionRequest` binds one or a bounded batch of existing regions to artifact ID, source digest, page ID/digest, region ID/digest, exact cropped representation bytes/digest, processing profile, mechanism identity, provider/model where external, profile/schema/instruction digests, request/attempt correlation, and authority ID.

`RegionTranscriptionCandidate` contains the same bindings plus literal text, explicit empty/illegible/failure outcome, uncertainty spans relative to the returned literal text, warnings, provider response correlation, provider-reported model, adapter/parser versions, and retained structured-state reference. Returned unknown region IDs, missing bindings, or mismatched digests are rejected. A provider cannot return a new authoritative region.

Text must remain literal. Normalized comparison views are separate and never replace stored literal output.

## 8. Provider structured-state retention

Persist an immutable `ProviderStructuredResponseRecord` before or atomically with admission. It contains request/attempt/authority IDs, provider response ID, provider-reported model, adapter and parser identities/versions, schema/profile/instruction digests, requested region IDs in request order, the exact provider-returned structured output object in returned array order, block/region identifiers, literal strings, uncertainty and warning fields, a canonical raw-structured-payload SHA-256 and byte length, parse outcome, and admission/reconstruction references.

Requirement: retain the bounded raw structured payload itself with protection appropriate to its evidence sensitivity and access class when it consists of the provider response body needed to reproduce parsing. Apply a governed maximum size and canonical or byte-exact encoding marker. Access control, least privilege, integrity protection, retention/deletion policy, and auditable access are required security properties. Whether existing Parker storage protection is sufficient or application/storage encryption at rest is required is deferred to an explicit security design decision before implementation; R5A does not invent or select a cryptographic subsystem. If policy prohibits payload retention, fail closed for high-assurance acceptance rather than claiming forensic comparability from a digest alone. Never retain credentials, authorization headers, cookies, hidden provider internals, or unrelated transport metadata. Record only bounded status/timing metadata needed for audit.

Returned array order is retained as a provider assertion. Parker's admitted order is separately recorded from source geometry, allowing direct comparison.

## 9. Independent mechanisms and comparison

Introduce provider-neutral `IndependentRegionTranscriptionMechanism` with `transcribe(RegionTranscriptionRequest)`. Every mechanism consumes the same independently retrieved source-region representation. Mechanism A cannot receive B's text and B cannot receive A's text. Local OCR remains eligible as one mechanism. Claude Vision/OCR is only a future candidate requiring separate current-capability, privacy, configuration, profile/schema, acceptance, and provider-authority governance; it is neither selected nor presumed superior.

`RegionTranscriptionComparator` is pure and deterministic. It detects and locates differences but does not itself decide workflow materiality. It first verifies identical source/page/region bindings. It then computes:

- exact UTF-8/code-point equality;
- Unicode NFC comparison without changing originals;
- line-ending and whitespace-only classifications;
- case-only and punctuation-only classifications;
- numeric-token differences (always material);
- grapheme/code-point edit operations with exact offsets and bounded context;
- token omissions, insertions, substitutions, and uncertainty-span disagreement.

The detector emits exact difference facts and categories. A separately versioned `TranscriptionDifferenceMaterialityPolicy` maps those facts to workflow outcomes such as `AGREED_EXACT`, `MINOR_NORMALIZATION_DIFFERENCE`, `MATERIAL_DISAGREEMENT`, or `UNRESOLVED`; `HUMAN_VERIFIED` is recorded only by human review, not emitted by comparison. Unicode normalization, line-ending, whitespace, punctuation, capitalization, numeric, identifier, word/token, omission/insertion, region, and uncertainty categories are detected deterministically. R5A does not freeze every category as automatically requiring review. Numeric, date, currency, case-number, and exact-name/identifier differences are mandatory high-risk policy inputs and may never be erased by semantic similarity. No semantic AI adjudication is permitted.

Agreement is corroborative evidence only. It does not make text authoritative or `VERBATIM`. Disagreement does not identify a winner; it preserves both outputs and triggers source-grounded resolution.

## 10. Optional second-mechanism policy

`IndependentComparisonPolicy` may require a second mechanism for low confidence, handwriting, degraded scans, structural ambiguity, numeric/identifier-heavy regions, first acceptance of a mechanism/source class, material disagreement with local OCR, or owner-selected high-assurance mode. The policy returns a requirement and allowed mechanism classes; it never executes or grants disclosure authority.

No second provider receives evidence unless a distinct authority explicitly permits that recipient, source representation, regions, and attempt budget. Uncertainty from provider A cannot expand disclosure to provider B.

## 11. Human verification and correction

The targeted owner view presents the authoritative region image, page/region identity and geometry, transcription A, transcription B when present, exact deterministic diff, uncertainties, and provenance. The reviewer verifies against the image rather than selecting better-sounding prose.

An immutable `RegionHumanVerificationRecord` binds artifact, page and region digests, reviewed generation(s), comparison record, reviewer principal, timestamp, determination, verified scope, and review-artifact digest. Determinations distinguish `CONFIRMED_AS_TRANSCRIBED`, `REJECTED`, `ORDER_CONFIRMED`, `ORDER_REJECTED`, `UNRESOLVED`, and `CORRECTION_RECORDED`.

A correction is not stored by mutating a machine generation or by changing a verification record. `HumanCorrectedRegionTranscription` is a separately identified provenance-bearing successor derivative that cites the machine generation(s), source region, verification record, exact literal correction, reviewer, timestamp, and reason classification. Historical machine outputs remain readable and immutable.

## 12. Admission and reconstruction

`RegionAnchoredTranscriptionValidator` rejects unless:

- artifact/source/page/representation and region digests match custody-derived records;
- every returned region exists and belongs to the bound page;
- no unknown or duplicate region exists unless the schema explicitly permits typed alternatives;
- requested, submitted, returned, failed, and omitted region/page accounting reconciles;
- provider identity, model snapshot, profile/schema/instruction, parser, structured-state digest, request, attempt, and authority bind consistently;
- uncertainty spans are bounded by literal text;
- geometry-derived order is reproducible and no unresolved ambiguity is concealed.

Provider-declared order never determines reconstructed order. `GeometryAnchoredTranscriptionReconstructor` joins region-bound literal text using the persisted Parker order graph. It preserves paragraphs, objectively represented headings/lists, supported table cells, signature/authorization blocks, header/footer roles, and ambiguity markers. It records reconstruction profile/version and ordered region IDs. Unsupported structure produces partial/degraded output or failure, never guessed prose.

## 13. Assurance and owner-visible states

Orthogonal assurance facts should be persisted instead of one overstated label:

- machine transcription exists;
- source region binding validated;
- structural order anchored;
- independent corroboration present;
- material disagreement present;
- human verification present;
- human correction successor exists.

Owner-visible aggregate states are `ACQUIRED`, `STRUCTURALLY_ANCHORED`, `UNVERIFIED`, `CORROBORATED`, `DISAGREEMENT`, `HUMAN_VERIFIED`, and `FAILED`. A corrected successor is additionally labelled `HUMAN_CORRECTION_EXISTS`. Neither agreement nor structural anchoring alone means `VERBATIM`.

## 14. Authority, privacy, and security

Future execution authority must bind authority/programme/execution/request/attempt IDs; artifact ID, source digest/length/media; production commit; page representation profile; region profile/version and region scope; mechanism, provider and model; capability/profile/schema/instruction and adapter/parser versions; comparison policy/mechanism when required; maximum attempts; storage policy; and exact permitted external recipients.

Dual-provider execution requires separate recipient-scoped grants or one authority with two explicit, independently budgeted recipient bindings. Authority for A never implies authority for B. Local OCR requires no external-egress grant but remains governed.

Preserve minimum disclosure, custody verification, `store=false` where supported, bounded region egress, credential non-persistence, appropriately protected structured-state storage, access control, retention/deletion policy, and audit. Encryption-at-rest requirements and mechanism remain an explicit pre-implementation security decision. No provider cross-contamination is permitted.

## 15. Failure model

Fail-closed classifications include `RENDER_FAILED`, `RENDER_NONDETERMINISTIC`, `SOURCE_BINDING_MISMATCH`, `REGION_DERIVATION_FAILED`, `REGION_ACCOUNTING_FAILED`, `AMBIGUOUS_GEOMETRY`, `HUMAN_ORDER_REQUIRED`, `NOT_YET_SUPPORTED`, `PROVIDER_FAILED`, `MISSING_REGION`, `DUPLICATE_REGION`, `UNKNOWN_REGION`, `REGION_BINDING_MISMATCH`, `MALFORMED_STRUCTURED_OUTPUT`, `STRUCTURED_STATE_PERSISTENCE_FAILED`, `SOURCE_ORDER_INCONSISTENT`, `MATERIAL_DISAGREEMENT`, `HUMAN_REVIEW_FAILED`, and `AUTHORITY_SCOPE_MISMATCH`. No silent fallback changes renderer, segmentation, region scope, mechanism, recipient, or ordering rule.

## 16. Compatibility

Existing page-level codecs and generations remain readable without migration. In particular generations `8e993aff-3518-445a-991d-e0270b98d510` and `43daf1bf-f43c-4348-8f9f-ffd86bcfaab5` are never rewritten. New region-aware payloads use a new tagged codec variant/schema and new stores where atomicity requires it. Readers dispatch by representation version; absence of region data on historical records is reported honestly, not synthesized.

Handwriting can later use the same page, region, authority, comparison, and review contracts with a separately accepted mechanism/profile and stronger policy. O.5 is not consumed by this design.

## 17. Regression oracle and synthetic matrix

The offline oracle consists of custody-derived canonical page fixtures, exact page digests, governed region bounds/IDs, and expected order graphs reviewed against source images. Expected transcription text may test literal comparison, but the failed provider generation is never the geometry oracle.

Mandatory A1 relations are graph assertions, not token-presence checks: on page 2 the initial emphasized proposition region must follow its authoritative preceding region and precede its authoritative following region; on page 3 the authorization block region must precede closing prose. Tests fail when all words exist but a graph edge or reconstructed order is wrong.

Synthetic fixtures cover: simple single-column prose; emphasized proposition between prose; two-column traversal; authorization/signature before closing prose; supported and unsupported tables; repeating header/footer separation; degraded scan; mixed text/image; overlapping ambiguous layout requiring human order; missing/duplicate/unknown region; provider-returned reverse order; and independent outputs exhibiting exact, normalization-only, punctuation, numeric, omission, insertion, region, and uncertainty disagreements. All use fake mechanisms and zero network calls.

## 18. Repository implementation surfaces

### Add

- `src/interfaces/SourcePageRepresentation.kt`: page identity, canonical render facts and provenance.
- `src/interfaces/SourceRegion.kt`: geometry, stable identity, derivation and order graph contracts.
- `src/interfaces/RegionTranscription.kt`: request/candidate/result and independent mechanism abstraction.
- `src/interfaces/RegionTranscriptionComparison.kt`: deterministic comparison records and outcomes.
- `src/interfaces/RegionHumanVerification.kt`: region review and correction-successor contracts.
- `src/interfaces/ProviderStructuredResponse.kt`: retained response record/storage contract.
- `src/runtime/DeterministicPageRepresentationFactory.kt` and selected renderer adapter.
- `src/runtime/DeterministicSourceRegionDeriver.kt`.
- `src/runtime/GeometryReadingOrderResolver.kt`.
- `src/runtime/RegionAnchoredTranscriptionValidator.kt`.
- `src/runtime/GeometryAnchoredTranscriptionReconstructor.kt`.
- `src/runtime/DeterministicRegionTranscriptionComparator.kt`.
- filesystem codecs/stores for page/region representations, provider structured responses, comparisons, and region reviews/corrections.
- focused contract/runtime/composition/UI tests and custody-derived fixture manifests under `tests/fixtures`.

### Modify

- `src/interfaces/EvidenceAcquisition.kt`: advertise region-anchored capability and policy facts.
- `src/interfaces/ExternalTranscriptionOperation.kt`: add a new region execution lane without changing historical page requests.
- `src/interfaces/OcrProcessingRepresentation.kt` and `OcrTranscriptionProvenance.kt`: support region-bound direct representations and versioned transformation chains.
- `src/interfaces/OcrStructuredTranscription.kt`: retain historical v3 types; connect only through explicit compatibility adapters.
- `src/interfaces/DerivativeContent.kt`, `TierBOcrDurableGeneration.kt`, and codecs: add tagged region-aware payloads.
- `src/interfaces/HumanVerification.kt`: retain old records and add links to the new region-level model.
- `src/runtime/AuthoritativeAcquisitionSourceResolver.kt`, `OcrProcessingRepresentationFactory.kt`, `GovernedAcquisitionIntegration.kt`, `ExternalTranscriptionInvocationGate.kt`, and owner coordinator: bind new source/region authority.
- `src/runtime/DerivativeGenerationCoordinator.kt`: admit new reconstruction and structured-state references atomically without changing historical admission.
- `src/runtime/FidelityFirstAcceptanceExecution.kt` and attempt ledger: future new authority fields/identity only; consumed A1 authority remains immutable.
- `src/composition/OpenAiResponsesExternalTranscriptionAdapter.kt`: future new schema parser and structured-state capture; preserve existing adapter behavior/version.
- `src/composition/OpenAiExternalTranscriptionProviderProfile.kt`, `ParkerRuntime.kt`, and `ParkerRuntimeConfig.kt`: separately accepted profile/composition.
- `src/ui/parker/ui/OwnerEvidenceUpload.kt`, `OwnerEvidencePresentation.kt`, and `src/composition/OwnerEvidenceHttpServer.kt`: targeted region comparison/review only.

### No change in design scope

- custody bytes and manifests in `EvidenceArtifactStorage`, `EvidenceCustodian`, and `EvidenceSourceManifest` remain authoritative;
- general Memory, Knowledge, reasoning-provider, conversation, agent, task, and tool contracts;
- native extractor semantics;
- existing generations, reviews, authorities, attempts, and frozen profile/schema records.

## 19. Bounded implementation units

1. **R6.1 — Page representation selection and contract:** Linux renderer bake-off, frozen parameters, page model/store, deterministic fixtures; no provider.
2. **R6.2 — Region geometry and order graph:** region identity, bounded derivation, complex-layout fail-closed rules, A1 geometry oracle; no provider.
3. **R6.3 — Region-aware schema and contracts:** proposed identities, request/candidate types, authority additions, fake adapters; no live provider.
4. **R6.4 — Admission and reconstruction:** binding/accounting validator, geometry reconstruction, new payload codec, historical compatibility.
5. **R6.5 — Structured-state persistence:** appropriately protected bounded record, atomic persistence boundary, forensic replay tests, and the explicit encryption-at-rest decision. This unit must complete before any region-aware live provider test.
6. **R6.6 — Independent comparison and policy hooks:** provider-neutral mechanism, deterministic difference detection, separately versioned materiality policy, local OCR seam, and recipient-scoped authority. This framework remains independently testable and is not a prerequisite for the first single-mechanism region-aware implementation, but any acceptance claiming corroboration requires it.
7. **R6.7 — Targeted human verification:** source-region comparison view, immutable review and correction-successor records.
8. **R6.8 — Offline regression closure:** complete synthetic matrix, A1 source-geometry relations, restart/codec/security tests.
9. **R6.9 — Governed profile acceptance and deployment:** separately reviewed configuration and deployment; still no evidence disclosure unless explicitly authorised.
10. **R6.10 — New controlled acceptance:** fresh one-use authority and governed attempt after all earlier gates; no reuse of consumed A1 authority.

Each unit is independently reviewable and fail-closed. R6.1 is the recommended next unit.

## 20. Unresolved decisions for R6 governance

- renderer and exact canonical raster parameters after Linux reproducibility/security evaluation;
- segmentation implementation and numeric thresholds;
- whether v1 supports any table/form reconstruction or marks all such pages human-required;
- exact persistence transaction boundary across new stores;
- raw payload encryption/retention duration and maximum size;
- exact wire names and final identity strings/digests;
- batching limits and whether external requests are one region or a bounded page-region set;
- exact comparison normalization profile and writing-direction support;
- acceptance budget, provider/model choice, and any second-provider privacy decision.

None may be silently chosen during implementation. They require evidence and explicit governance.

## 21. R5 boundary

R5 made no runtime or test implementation, provider request, Claude request, acceptance invocation, deployment, lifecycle change, authority consumption, or production-store mutation. This document is a draft for governance review and must not be committed or pushed by R5.
