# UI-INGESTION-3 — Human-Readable Evidence Library

## Status

IMPLEMENTED — FOCUSED VERIFIED — CANDIDATE BUILT — NOT DEPLOYED

## Starting state and trace

Work began on clean `main` at HEAD/upstream `b25152d3b70d916a13d7adbddcb22334e6202cb8`.

The existing multipart parser already extracted and sanitized the client filename to a basename. The durable `EvidenceSourceManifest` and versioned manifest codec already supported `originalFileName`, media type, content SHA-256, and byte length. The presentation problem arose because `OwnerEvidenceHttpServer` staged bytes as `owner-upload-*.part`, then called the ordinary local-path import seam; `OwnerLocalFileIngressCoordinator` consequently recorded the staging basename as source-filename provenance.

No persisted evidence format needed to change. The existing owner-only evidence listing reads canonical manifests and revalidates stored bytes and hashes through Evidence Custody. Manifest publication file time supplies a read-only registration-order fact without rewriting historical records.

## Minimum implementation

The HTTP upload path now calls an additive upload-specific method carrying the parser's sanitized client basename through `OwnerUiEvidenceRuntimeAdapter`, `ParkerRuntime`, and `OwnerLocalFileIngressCoordinator` into the existing `CandidateEvidenceArtifact.originalFileName` field. Evidence bytes, SHA-256, EvidenceId minting, custody authorization, and durable manifest encoding are unchanged. Existing local-file ingress retains its original two-argument behavior.

Changed implementation files:

- `src/composition/Main.kt`
- `src/composition/OwnerEvidenceHttpServer.kt`
- `src/composition/OwnerUiEvidenceRuntimeAdapter.kt`
- `src/composition/OwnerUiRuntimeComposition.kt`
- `src/composition/ParkerRuntime.kt`
- `src/runtime/OwnerEvidenceListing.kt`
- `src/runtime/OwnerLocalFileIngressCoordinator.kt`
- `src/ui/parker/ui/OwnerEvidenceUpload.kt`

Focused regression coverage was added in:

- `tests/composition/OwnerEvidenceHttpServerTest.kt`
- `tests/runtime/OwnerLocalFileIngressCoordinatorTest.kt`

## Evidence-library behavior

The existing authenticated page now presents `Document`, `Uploaded / Imported`, `Status`, `Pages`, `Size`, `Analyse`, and `Actions`. A provenance-supported filename is the primary identity; a shortened Evidence ID is subordinate while the full ID remains in Details. Missing filenames and `owner-upload-*.part` staging names display `Unnamed evidence`; the raw staging name is shown only as a technical detail. No historical record is modified or assigned an invented name.

Statuses are translated into human-readable labels while Details retains the exact internal state. Details includes only available facts: full Evidence ID, source filename or neutral fallback, raw staging name when relevant, content digest, exact byte size, media type, registration time, processing state, known representation identity, page count, human-review summary, source-confirmed status, and corrected-representation availability.

Search covers filename/source label and Evidence ID. Sorting supports newest first (default), oldest first, filename A–Z, and status. Deterministically derived filters cover needs processing, processed, human reviewed, and corrected. After successful upload the page reloads durable registrations; the manifest publication time places the new item at the top.

The primary action label is now `Process document`. It invokes the unchanged governed acquisition-decision handler and endpoint. Enhanced transcription remains explicit and governed. Analyse selection remains bound to the exact Evidence ID and now has an accessible label containing the human document name. Provider and corrected representations remain separate.

Current established document paths remain unchanged: CSV, EML, DOCX, and searchable PDF use Tier A; scanned PDF and supported image inputs may require the existing explicit OCR/enhanced-transcription paths. No file-type support was added.

## Focused verification and offline acceptance

The final bounded-heap focused run selected the new upload/library regressions plus the existing runtime-adapter suite:

`./gradlew test --tests '*uploaded human filename survives*' --tests '*human readable evidence library offers*' --tests '*declared upload basename replaces*' --tests 'parker.composition.OwnerUiEvidenceRuntimeAdapterTest' --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g`

Result: **26 tests, 0 failures, 0 errors, 0 skipped**.

The isolated HTTP/runtime acceptance uploaded `Example Human Readable Document.pdf` through a paired server-side owner session. Durable listing returned that exact primary filename and its independently minted Evidence ID, never the random staging name. Tests verify the exact bytes survive, filename metadata does not participate in content identity, multipart path components are reduced to safe basenames by existing parser coverage, filename/Evidence-ID search and newest-first UI logic are present, `Process document` targets the same Evidence ID through the existing governed handler, Details uses existing provenance facts, and analysis selection remains identity-bound. No provider was called.

## Candidate artifact

- Candidate source commit: `296ff5802670387f44a7950ee6c57899ebf17e7d`
- Runtime JAR SHA-256: `ac5d9217a60d52ccaa2ef1dc4ecba6e75264960c19ae86280fd691bd30399d99`
- Image ID/local manifest digest: `sha256:784aa3c69b8fcf1a52d3648f3fe7590fe458acf40ed0c11b3efda56e77665f24`
- OCI config digest: `sha256:0322c3947b4bd969af6c9a26400986bd984d6a57256450a686a3cc2b4aa064a7`
- Image size: `1,032,312,836` bytes
- Platform: `linux/amd64`
- Revision label: `296ff5802670387f44a7950ee6c57899ebf17e7d`

The runtime distribution was built offline with the established bounded heap and layered with `docker buildx build --network=none --pull=false --platform linux/amd64 --provenance=false` over the established local Parker runtime base. A no-network disposable container proved the final image's runtime JAR is byte-identical to the accepted JAR. The candidate was not deployed.

## Security, production, and provider accounting

Paired-device authentication, server-side sessions, HttpOnly credentials, the absence of browser owner-token handling, and the separate high-authority verification boundary are unchanged. No secret was added to HTML, JavaScript, logs, source, or image metadata.

- Production deployment: not performed
- Production governed-state delta: `0`
- Evidence/derivative/review/audit mutation in production: none
- Provider calls: `0`
- Retries: `0`
- External evidence egress: `0`
