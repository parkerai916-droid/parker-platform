# UI-INGESTION-1 — Existing Owner Evidence Upload to Governed Ordinary Ingestion

## Status

COMPLETE — IMPLEMENTED AND FOCUSED-VERIFIED

## Starting state and boundary

- Starting `main` HEAD/upstream: `11f7ec3396fdd633edcb6ed4f1f94f503caca330`.
- Starting worktree: clean.
- Production deployment and governed production-state mutation: none.
- Parker provider calls/retries/external egress: `0 / 0 / 0`.
- This unit connects the existing owner browser UI; it adds no UI application, provider architecture, ingestion governance, review-writing route, or correction-writing route.

## Existing UI trace

| Owner action | Frontend/HTTP path | Existing governed runtime path | Terminal availability before this unit |
|---|---|---|---|
| Owner token | `authHeaders()` -> bearer token on every `/owner/*` call | constant-time token comparison in `OwnerEvidenceHttpServer`; runtime adapter has the configured ordinary owner principal | Working; ordinary UI authentication only |
| Choose Files / Upload | multipart `POST /owner/evidence` | `OwnerEvidenceOperations.importFile` -> `ParkerRuntime.importEvidenceFileAsOwner` -> local-file ingress/custody/registration | Working; returns durable Evidence ID with `IMPORTED` |
| Refresh existing evidence | `GET /owner/evidence` | custody-backed `listRegisteredEvidenceAsOwner` | Working; renders `READY_TO_PROCESS` |
| Acquire machine-readable representation | `GET /owner/evidence/{id}/acquisition` | governed acquisition evaluation and capability selection | Working as a decision display |
| Authorize/execute selected enhanced path | `POST .../authorize-region-transcription`, `POST .../execute-region-transcription`, or `POST .../acquire` | evidence-specific authorization followed by the existing governed ordinary-region execution path | Working, but a completed generation was not transferred into the browser row, so durable readback was unreachable |
| Process | `POST /owner/evidence/{id}/process` | existing Tier A router for PDF/CSV/EML/DOCX | Intentionally disabled legacy/manual compatibility control; governed acquisition remains the ordinary path |
| Run enhanced transcription / readiness | `GET .../transcription-readiness`, legacy `POST .../transcribe-external` | configured readiness evaluator and existing explicit external invocation | Readiness working; legacy button intentionally disabled in favour of the evidence-specific governed acquisition controls, with the reason/title shown |
| Durable readback | `GET /owner/evidence/{id}/content/{generation}` | owner-only runtime boundary -> `TierAContentRetrievalCoordinator` -> immutable generation/content stores | Working, but unreachable after governed acquisition because of the missing row state handoff |
| Human fidelity | part of the same canonical content response | existing A8A projector over exact target | Already serialized, but not rendered by browser content presentation |
| Corrected representation | existing B1/B3 create-once store and eligibility evaluator | existing composed corrected-representation retrieval service | No correction write route (correctly); no exact-target discovery or browser presentation |
| Analyse | `POST /owner/analyse` | existing governed local-analysis coordinator | Available only for supported durable Tier A/Tier B selections. Ordinary region transcription remains unsupported by that coordinator and is now not offered as an analysis selection |

## Minimum wiring implemented

- A completed governed acquisition now transfers its returned `derivativeGenerationId` into the existing evidence row, marks the row's provider representation available, and enables the existing canonical `View Extracted Content` action.
- The existing `REGION_TRANSCRIPTION` response is now rendered. Raw provider blocks, human-fidelity status, and any human-corrected representation are visibly separate provenance layers.
- Human-fidelity rendering includes effective state, coverage, material discrepancy count, systematic-pattern count, unresolved-conflict state, provider source-confirmed eligibility, and denial reason.
- `HumanCorrectedRepresentationStorage` gained an additive exact-target read-only query. The filesystem implementation decodes validated create-once records and returns deterministic identity order. The retrieval service fails closed on ambiguity.
- The existing Tier A retrieval coordinator derives the exact six-part target from canonical generation/content identities and attaches at most one corrected representation. Corrected-store failure never creates an eligibility allowance and never suppresses independently authorized intact raw provider output.
- Corrected presentation includes its distinct generation/kind, canonical review ID, correction count, corrected content digest/blocks, and its own source-confirmed eligibility.
- Ordinary region transcription is not incorrectly offered to the current analysis coordinator, which explicitly rejects that derivative kind. No reasoning scope was expanded.

## Owner token and high-authority credential

The browser Owner token is ordinary UI bearer authentication. It is not the high-authority correction credential. The opaque high-authority principal and external secret remain server-side requirements of correction creation. This unit adds only read-only corrected-representation presentation: no high-authority secret is placed in HTML, JavaScript, JSON, logs, or a browser request, and no legal owner name is required by the new path.

## Supported file types

The existing Tier A router recognizes PDF, CSV, EML, and DOCX using content-derived detection. Searchable PDF uses the established local Tier A path; scanned/image-only PDF can enter the existing governed ordinary acquisition and explicitly authorized enhanced transcription path. PNG remains an acquisition input where the established router supports it; this unit does not broaden formats. The representative acceptance case is PDF.

## Files changed

- `src/composition/OwnerEvidenceHttpServer.kt`
- `src/composition/OwnerUiEvidenceRuntimeAdapter.kt`
- `src/composition/ParkerRuntime.kt`
- `src/interfaces/HumanCorrectedRepresentation.kt`
- `src/interfaces/TierAContentRetrieval.kt`
- `src/runtime/DefaultGovernedHumanCorrectionService.kt`
- `src/runtime/FileSystemHumanCorrectedRepresentationStorage.kt`
- `src/runtime/TierAContentRetrievalCoordinator.kt`
- `src/ui/parker/ui/OwnerEvidenceUpload.kt`
- `tests/composition/OwnerEvidenceHttpServerTest.kt`
- `tests/runtime/HumanCorrectedRepresentationDurabilityTest.kt`

## Verification

Focused command:

`./gradlew test --tests 'parker.composition.OwnerEvidenceHttpServerTest' --tests 'parker.composition.OwnerUiEvidenceRuntimeAdapterTest' --tests 'parker.core.runtime.HumanCorrectedRepresentationDurabilityTest' --tests 'parker.core.runtime.TierAContentRetrievalCoordinatorTest' --tests 'parker.integration.OwnerEvidenceHttpEndToEndLiveAcceptanceTest' --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g`

Result: **4 suites, 131 tests, 0 skipped, 0 failures, 0 errors**. (The requested integration class is exercised through its repository-configured test placement; the generated result set contains four JUnit suite files.)

The live HTTP acceptance fixture proves representative PDF upload -> durable evidence registration -> governed processing -> canonical durable readback without provider activity. Added focused assertions prove the governed acquisition generation handoff, separate raw/review/corrected headings, source-confirmed status display, unsupported analysis exclusion, browser-secret exclusion, and restart-safe exact-target corrected lookup.

## Remaining UI gaps

- The established analysis coordinator does not consume ordinary region transcription or human-corrected region transcription. This unit truthfully withholds that unsupported selection rather than expanding reasoning scope.
- The legacy Process/local OCR/enhanced-transcription compatibility buttons remain deliberately disabled; the active ordinary flow is the governed acquisition decision plus its explicit authorization/execution controls.
- There is no review-authoring or correction-authoring UI. Existing canonical review and corrected representations are read-only here, as required.

Production was not deployed, restarted, or mutated. No production evidence was used for testing. Provider calls, retries, and external egress were zero.
