# Fidelity-Preserving Evidence Acquisition Scope Lock

## Status and effect

**Adopted owner governance — Unit FA.1.** This instrument governs future Parker document-acquisition design and implementation. It authorises no production implementation, deployment, provider call, OCR invocation, evidence processing, analysis, provider activation, fallback, or request-budget use.

It prospectively supersedes any future-routing assumption in earlier governance that treats local OCR as constitutionally preferred, requires an external mechanism to prove universal non-inferiority to local OCR before it can be eligible, equates a UI processing state with permanent acquisition policy, or requires universal human verification of every machine-readable derivative. Historical scope locks, implementation records, generations, acceptance results, and request accounting remain truthful and unchanged.

## 1. Constitutional objective

Parker's acquisition layer takes already human-authorised evidence and produces the strongest faithful machine-readable representation reasonably achievable among capabilities that are actually governed, eligible, and permitted for that source. It is an evidence-acquisition mechanism, not an evidence-assessment or reasoning mechanism.

Fidelity, source lineage, immutable identity, provenance, uncertainty, privacy, and bounded authority govern acquisition. Neither local nor external execution has inherent fidelity authority.

## 2. Evidence authorisation precedes acquisition

The authorised human decides whether a source belongs in Parker and admits it through the governed Evidence Custodian boundary. Acquisition does not decide relevance, admissibility, truth, reliability, legal significance, credibility, case issue, or whether the source should have been uploaded.

After admission, acquisition may determine only source characteristics, eligible capabilities, deterministic capability selection, required processing representation, uncertainty and illegibility, and derivative provenance.

Human authorisation of the input never verifies a derivative output.

## 3. Original EvidenceArtifact authority

The original `EvidenceArtifact` is the authoritative evidential source. Its identity is anchored by its exact `EvidenceArtifactId`, authoritative SHA-256, byte length, media type, and Evidence Custodian provenance.

Every extraction, OCR result, transcription, processing representation, provider result, human-corrected text, and later analysis is subordinate derivative material. Readability, provider identity, operational success, structural completeness, human input authorisation, or later reasoning confers no source authority. No derivative may silently replace, overwrite, supersede, mutate, or become the original evidence.

The original remains retrievable for inspection and exact source comparison.

## 4. Direct-source acquisition invariant

Every extraction, OCR, transcription, OCR rendering, or vision-acquisition operation must consume either:

1. the authoritative Evidence Custodian bytes for the exact selected `EvidenceArtifactId`; or
2. a governed processing representation derived directly from those authoritative bytes.

A previous OCR generation, transcription, Tier A text result, derivative generation, browser preview, UI rendering, thumbnail, screenshot, exported review image, worksheet, temporary display copy, derivative PDF, analysis text, or raster created from another derivative is not valid acquisition input merely because it exists.

A previous derivative can become input only after a separate human-authorised operation explicitly admits it as a new source `EvidenceArtifact`. Its new source identity and provenance must then be used; no implicit derivative-of-derivative chain is permitted.

The normal lineage is:

`authoritative source → direct extraction → immutable derivative`

or:

`authoritative source → directly derived processing representation → OCR/vision/transcription → immutable derivative`.

## 5. Current direct-source audit

The Unit FA.1 repository inspection establishes:

- `OwnerLocalFileIngressCoordinator` reads the one owner-designated file byte-exactly and submits a `CandidateEvidenceArtifact` to `DefaultEvidenceCustodian` only after permission approval.
- `DefaultEvidenceCustodian` stores the accepted bytes and writes an authoritative `EvidenceSourceManifest` from those same bytes.
- `TierAOwnerInvocationCoordinator` retrieves the manifest and exact Evidence Custodian bytes, verifies length and SHA-256, and passes those bytes to `GovernedTierADocumentIngestionRouter`.
- Tier A CSV, EML, DOCX, and PDF coordinators receive copies of those verified source bytes. Their generation records use a `RootEvidenceArtifact` parent and preserve the root source identity.
- `TikaPdfStructuralExtractor` operates with OCR disabled on the source PDF bytes. A textless PDF returns `RequiresTierB`; Tier A does not OCR an extracted-text derivative.
- `TierBOcrOwnerInvocationCoordinator` retrieves authoritative source bytes. `EvidenceIntelligenceOcrCoordinator` independently verifies their manifest length and digest before constructing the local `OcrRecognitionRequest`.
- `ExternalTranscriptionOwnerInvocationCoordinator` retrieves authoritative source bytes and manifest, verifies both, then uses `OcrProcessingRepresentationFactory`.
- The current external processing representation is a defensive byte-exact copy. Its representation digest, length, and media type are required to match the authoritative source facts.
- `DerivativeGenerationCoordinator` admits Tier A, local OCR, and external transcription as fresh immutable generations rooted directly in the selected EvidenceArtifact.
- `TierAContentRetrievalCoordinator` and `TierBOcrContentRetrievalCoordinator` retrieve only an exact EvidenceArtifactId and DerivativeGenerationId pair and have no extraction/OCR/provider dependency.
- `DocumentAnalysisCoordinator` consumes only explicit exact-generation selections. It cannot cause acquisition or regenerate a derivative.

No composed production acquisition path was found that consumes a previous derivative, Tier A extracted text, browser/UI rendering, thumbnail, review image, worksheet, screenshot, or analysis output. `EvidenceExtractionCoordinator` is a legacy, currently uncomposed path and is not part of the owner document-ingestion runtime; its older registration/review model must not be repurposed as the future acquisition router.

The low-level `OcrMechanism` and provider-adapter contracts accept caller-supplied request bytes and do not themselves prove custody lineage. This is acceptable only because current production composition reaches them through manifest-verifying coordinators. FA.3 must make that direct-source invariant explicit and structurally testable for every future adapter and transformed representation.

Current provenance coverage is not identical across mechanisms. Tier A and local OCR generations retain the root EvidenceArtifact identity and depend on its separately durable authoritative manifest; the generation record itself does not repeat the source SHA-256. Local OCR's `OcrRecognitionResult` can carry page accounting and processing provenance, but those fields are optional and `DerivativeGenerationCoordinator.ingestOcr` does not currently require or persist them in the same mandatory form used for validated external transcription. Page mapping for local OCR is limited to whatever page-associated segments the concrete adapter truthfully returns. The external path already requires and persists source/representation SHA-256, source/representation lengths and media types, page accounting, and provider provenance. FA.2 and FA.3 must define a provider-neutral minimum without fabricating unavailable historical facts.

The repository models material transformations in `OcrMaterialTransformation`, but the currently composed external `OcrProcessingRepresentationFactory` creates only a byte-exact copy. No production page-rasterisation/orientation/crop path is presently authorised through that factory. Any future transformed path must implement direct derivation, exact page mapping, transformation provenance, bounded temporary-byte handling, and offline lineage tests before it becomes eligible.

## 6. Processing representations

A provider need not always receive original file bytes. Page rasterisation, page extraction, rotation, orientation correction, bounded crop, DPI or scale adjustment, colour-space conversion, lossless conversion, or another mechanism-required transformation may be used only when generated directly from the authoritative source.

Where applicable, provenance retains:

- source EvidenceArtifactId, SHA-256, byte length, media type, and page scope;
- representation identity, SHA-256, byte length, media type, and page mapping;
- processing profile, mechanism, version, producer, and creation time;
- pixel dimensions, DPI, rotation, scaling, crop, colour-space, compression, and encoding facts;
- `byteExactCopy=true/false` and a truthful material-transformation record.

Existing `OcrProcessingRepresentation`, `OcrProcessingProvenance`, `OcrMaterialTransformation`, `OcrPageScope`, and digest contracts are reused. New fields are introduced later only where a selected transformation cannot be represented truthfully.

A processing representation is request-scoped subordinate material, not new authoritative evidence.

## 7. Source-appropriate fidelity selection

Selection occurs before execution and is deterministic. Parker chooses the strongest suitable capability among those authorised and eligible for the established source characteristics and requested acquisition purpose.

- Searchable born-digital PDF: prefer faithful native text/structure extraction; do not rasterise merely to force OCR.
- Scanned/image-only PDF or image: select an eligible OCR or vision mechanism.
- Handwriting: select a governed handwriting-capable mechanism where available.
- Difficult layout or tables: select a governed layout- or table-capable mechanism where materially suitable.
- DOCX, spreadsheets, EML, and future structured formats: prefer source-aware native structural acquisition where appropriate.

Source classification and acquisition selection remain different decisions. `TIER_A_COMPLETE`, `REQUIRES_OCR`, and UI row status describe current processing state; they are not the permanent policy model.

## 8. Local and external capabilities

Local OCR is one governed acquisition capability. It is neither a constitutional comparator nor inherently preferred because it is local.

External OCR or vision transcription is another possible governed capability. It may be selected only when external egress, provider/profile, privacy, retention, credential, endpoint, model, cost/request, media, size, page, and representation policies make it eligible.

This instrument creates no blanket external-egress permission and does not activate OpenAI or any other provider. Provider-specific adapters remain subordinate to provider-neutral acquisition contracts. Future capability records may describe OpenAI, Google, Azure, Mistral, specialist OCR, local models, or other governed mechanisms without architectural preference.

## 9. No reasoning during acquisition

Acquisition may read, extract, transcribe, preserve visible wording and structure, identify layout/tables, associate page or region content, and expose uncertainty, illegibility, omission, or failure.

It must not interpret, summarise, paraphrase, reconcile, assess credibility, perform legal analysis, infer intent, resolve factual conflicts, improve grammar, silently correct substantive wording, translate without a separate governed purpose, or reconstruct missing propositions from plausibility or case context.

Uncertainty wins over fluency. `UNKNOWN`, `UNCERTAIN`, `ILLEGIBLE`, qualified/partial output, explicit omission, or failure is preferable to a plausible invented completion.

## 10. Fidelity, completeness, and acquisition results

`UNVERIFIED_LITERAL_TRANSCRIPTION` remains the ordinary classification for unverified machine attempts at literal transcription. `VERBATIM` is not inferred from direct extraction, provider/model identity, success, readability, confidence, clean structure, complete page accounting, or human authorisation of the input.

Fidelity and completeness remain distinct. The provider-neutral acquisition result should reuse existing generation, derivative content, OCR result, page-accounting, uncertainty, completeness, provider, processing, and producer contracts. It must retain, where applicable:

- exact EvidenceArtifactId and DerivativeGenerationId;
- source digest, length, media type, page scope, and direct-source lineage;
- acquisition mechanism and capability identity;
- provider, adapter, model, model snapshot, configuration/profile, instruction digest, and schema digest;
- processing representation and transformation facts;
- page/region/segment mapping, text, uncertainty spans, illegibility, warnings, and page outcomes;
- fidelity, completeness, timestamps, and operational outcome;
- separately linked human-verification state.

No redundant second representation of an already governed fact is required.

## 11. Human review

Human verification remains available and immutable, bound to exact `EvidenceArtifactId + DerivativeGenerationId`. It never mutates the source or derivative.

Review is targeted rather than universally mandatory. Policy may require it for uncertainty, illegibility, conflicting acquisition results, critical passages, especially important evidence, low-confidence output, provider/model acceptance, or explicit owner request. Normal ingestion must not require manual retranscription of every successfully acquired document.

Existing `HumanVerificationRecord`, page/character scopes, outcomes, review-artifact digest, and `FileSystemHumanVerificationStorage` are reusable. Review status informs later selection but never promotes a derivative into original source truth.

## 12. Analysis boundary

Reasoning begins after acquisition. Analysis requires explicit selection of an exact derivative generation and must retain the selected EvidenceArtifactId, DerivativeGenerationId, fidelity, completeness, human-review state, source provenance, uncertainty, and provider/model facts where relevant.

An unreviewed derivative does not silently become source truth; a reviewed derivative still does not replace the original. Parker must preserve a path back to the original source and, where available, page or region for exact-wording review.

The existing `DocumentAnalysisCoordinator` exact-generation resolution and external-unverified acknowledgement are protective current controls. FA.7 may generalise presentation of acquisition and review facts but must not weaken explicit selection or acknowledgement requirements.

## 13. Provider-neutral capability model

FA.2 shall define a bounded capability description capable of expressing:

- capability identity and mechanism type;
- supported media/source characteristics;
- native PDF, scanned PDF, image, handwriting, layout, table, and structured-format support;
- structured output, page/region mapping, uncertainty, and illegibility support;
- byte/page/pixel and other governed bounds;
- direct-byte or transformed-representation requirements;
- external-egress requirement;
- provider/model/profile identities where applicable;
- acceptance/lifecycle state and bounded policy constraints.

Existing provider profiles remain provider-specific readiness/configuration facts and should be referenced, not duplicated. Capability eligibility is not execution authority.

## 14. Deterministic acquisition router

The minimum router is provider-neutral and initially selects among:

1. direct/native structured extraction;
2. local OCR;
3. governed external transcription.

It operates only after human-authorised Evidence Custodian admission. Inputs are bounded non-substantive facts: source identity, manifest media/length/digest, safely established page count and source characteristics, native-text availability, image-only/scanned status, established handwriting/layout/table characteristics, eligible capability records, acceptance state, egress/privacy authority, readiness, and capability limits.

It must not inspect case meaning, legal propositions, credibility, or narrative relevance.

The routing decision identifies the exact source, selected capability/mechanism, provider/model/profile when applicable, required representation, egress requirement, eligibility facts, deterministic reason category, configuration identity, and correlation/time facts. It contains no source narrative or legal reasoning.

The router selects once before execution. It does not try providers sequentially, rank outputs by fluency, or choose whichever result looks nicest.

## 15. Failure and fallback

Valid outcomes include unavailable capability, policy block, unsupported media, provider unavailable, source-integrity failure, representation failure, provider failure, structural-validation failure, partial/qualified acquisition, uncertainty, illegibility, and explicit acquisition failure.

Parker must not fabricate text to reach a complete state. Failure of one capability does not authorise another provider. Retry, fallback, alternate-provider execution, comparison runs, and model switching require separately adopted deterministic policy and request authority.

## 16. Existing components reusable without constitutional change

The following are substantially reusable:

- Evidence Custodian, byte storage, manifests, and owner local-file ingress;
- Tier A specialist extractors and derivative generation/content stores;
- manifest-verified Tier A and Tier B owner invocation boundaries;
- `OcrMechanism`, single-adapter sequencing, Docling adapter, and external transcription mechanism;
- processing, provider, model, page-accounting, uncertainty, fidelity, and completeness provenance;
- immutable derivative admission/audit and exact-generation retrieval;
- human-verification records/storage;
- exact-generation document analysis and owner acknowledgement controls;
- provider-profile/readiness and durable acceptance-attempt accounting.

## 17. Components requiring later amendment

Later implementation units must add or amend, without collapsing existing boundaries:

- provider-neutral capability, source-characteristic, eligibility, routing-input, routing-decision, and acquisition-outcome contracts;
- structural direct-source enforcement for transformed representations and every adapter;
- a deterministic router above existing Tier A/local/external mechanisms;
- adapter registration/wiring as capabilities rather than hard-coded UI-state choices;
- owner UI presentation of selected capability, acquisition stage, provenance, uncertainty, review state, and exact generation;
- analysis-package projection of acquisition/review facts;
- provider/profile capability evidence, including negative evidence.

The current owner UI presents independent `Process`, local OCR, durable OCR, and enhanced-transcription actions based largely on row status/readiness. That remains truthful historical functionality but is not the final routing policy.

## 18. Prospective conflict determination

No existing constitutional boundary prevents this decision. The following earlier assumptions are prospectively superseded where they govern future routing:

- local OCR as universal comparator or inherent first choice;
- universal external-versus-local non-inferiority as an architectural eligibility prerequisite;
- `REQUIRES_OCR` permanently meaning local OCR only;
- `TIER_A_COMPLETE` permanently excluding another separately governed acquisition capability;
- external transcription being inherently exceptional rather than policy-eligible;
- universal human verification of every acquired derivative;
- UI row status itself constituting acquisition policy.

The current implementation does not claim OCR output is authoritative source text and does not silently use derivatives as acquisition input; those are not conflicts.

## 19. Unit O and gpt-4.1-mini

Unit O history is immutable:

- the initial CLEAN request was consumed;
- its gpt-4.1-mini output contained critical source-inconsistent invention;
- literal-v2 remediation was governed and implemented;
- the later CLEAN execution failed in acceptance instrumentation;
- provider consumption remained indeterminate;
- its allocation remains quarantined and operationally consumed;
- durable attempt-stage accounting was subsequently verified offline;
- O.5 remains unconsumed and is not authorised by this instrument.

No retrospective ledger, rewritten worksheet, restored allocation, or reinterpretation of historical generations is permitted.

The tested gpt-4.1-mini/profile combination retains negative capability evidence and is not automatically restored to ordinary acquisition. The existing lifecycle model already supplies constitutionally compatible non-executable states: `SUSPENDED` is the closest state for a previously usable configuration whose evidential capability is now withheld; `ACCEPTANCE_PENDING` remains appropriate only for a configuration genuinely awaiting acceptance. Ordinary composition requires `ACCEPTED`, so either state fails closed. Any actual profile-state change is a separate authorised implementation/operations unit.

Unit O's universal non-inferiority premise is superseded only as a future architectural eligibility rule. Its results remain valid evidence about the exact tested provider/model/profile/request combinations.

## 20. Privacy and authority preservation

Governed external acquisition remains subject to owner and Permission Engine authority, provider/profile acceptance, credential and endpoint readiness, privacy classification, retention/storage and training treatment, jurisdiction, `store=false` or equivalent controls, source eligibility, media/size/page limits, representation policy, and request/cost budgets.

This instrument does not weaken Evidence Custodian authority, source immutability, source-integrity verification, write-once generations, exact retrieval, provenance, page accounting, uncertainty, human verification, acquisition/analysis separation, credential controls, privacy controls, or the prohibition on automatic fidelity promotion.

## 21. Bounded implementation programme

- **FA.2 — Provider-neutral capability and routing contracts.** Define source characteristics, capability facts, eligibility, routing decision, result envelope, and lifecycle linkage. Offline contract tests only.
- **FA.3 — Direct-source lineage enforcement.** Make authoritative-byte/direct-representation lineage structural across all acquisition mechanisms; extend transformation provenance only where required; prove no derivative-of-derivative input.
- **FA.4 — Deterministic router.** Implement pure eligibility/filtering/selection and bounded reason categories with no execution, retry, or fallback.
- **FA.5 — Existing capability integration.** Register direct extraction, local OCR, and governed external transcription behind the router while retaining their authorization, readiness, admission, and request boundaries.
- **FA.6 — Owner UI.** Present acquisition choice/decision, state, provenance, uncertainty, review state, and exact generation without treating row status as policy.
- **FA.7 — Analysis selection.** Carry exact acquisition and human-review facts into explicit generation selection and downstream analysis without promoting derivative truth.
- **FA.8 — Offline synthetic acceptance.** Prove routing, direct lineage, corruption/failure, privacy, no retry/fallback, restart retrieval, and ordinary-runtime isolation without network or real evidence.
- **FA.9 — Governed real-document/provider acceptance.** Separately scope-lock request allocation, documents, providers, profiles, human review, durable attempt accounting, and stop conditions.

Each unit is independently reviewable. No unit inherits provider-call, deployment, evidence-processing, OCR, fallback, or request-budget authority from this document.

## Final determination

The owner decision is constitutionally compatible and adopted prospectively. Parker's acquisition architecture shall maximise faithful machine-readable acquisition among governed eligible capabilities while preserving direct-source lineage, immutable source authority, uncertainty, provenance, privacy, exact derivative identity, and the strict separation between acquisition and later reasoning.
