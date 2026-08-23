# Document Ingestion Programme — Implementation Closure

## 1. Status

**Formally closed as technically complete within its currently adopted, governed
scope.** This is a closure/index record of *implementation*, not new governance —
it creates no new rule, resolves no deferral, and grants no new authority. Where
this document restates a conclusion from an adopted governance document or an
existing test, that document or test remains the authoritative source; this
document is a navigational and evidentiary aid only.

## 2. Baseline commit

`main` at `d12313e50123ce17fb31f89a94062de3bfcc97da`, working tree clean at the
time of this review. All test evidence in this document was gathered fresh
against this exact commit, in this review, not carried forward from an earlier
report.

## 3. Governance reviewed

Fresh-inspected this review: `DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md`,
`SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`,
`DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md`,
`DOCUMENT_INGESTION_DERIVATIVE_REVIEW_TARGET_SCOPE_LOCK.md`,
`DOCUMENT_INGESTION_AUDIT_AUTHORITY_SCOPE_LOCK.md`,
`DOCUMENT_INGESTION_MEMORY_CORE_CROSS_REFERENCE_SCOPE_LOCK.md`,
`DOCUMENT_INGESTION_AUTHORITATIVE_SOURCE_MANIFEST_RETRIEVAL_SCOPE_LOCK.md`,
`DOCUMENT_INGESTION_OWNER_AUTHORIZED_LOCAL_FILE_INGRESS_SCOPE_LOCK.md`,
`DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md`,
`DOCUMENT_INGESTION_TIER_A_IMPLEMENTATION_CLOSURE.md`,
`DOCUMENT_INGESTION_PROGRAMME_GOVERNANCE_CLOSURE.md`,
`OCR_MECHANISM_UNIT_12_RUNTIME_INVOCATION_SCOPE_LOCK.md`,
`OCR_MECHANISM_UNIT_12_IMPLEMENTATION_PLAN.md`, CDR-006, CDR-007, CDR-008.

**Adoption chronology, independently re-verified via `git log`** (all committed
under an `adopt`/`authorize` commit, this repository's own established
acceptance convention, even where a document's own internal "Status" header
still reads "Draft for owner review" as a historical artefact of its own
drafting process):

| Document | Adopted (UTC) |
| --- | --- |
| Canonical Governance Alignment, Provenance Model, Routing & Completeness Policy | 2026-08-22 10:59 |
| Derivative Generation Record Scope Lock | 2026-08-22 11:25 |
| Derivative Review Target Scope Lock | 2026-08-22 11:49 |
| Audit Authority Scope Lock | 2026-08-22 12:29 |
| Memory Core Cross-Reference Scope Lock | 2026-08-22 12:53 |
| Programme Governance Closure (Units 1–6) | 2026-08-22 13:16 |
| Tier A Implementation Closure | 2026-08-23 04:01 |
| Authoritative Source Manifest Retrieval Scope Lock | 2026-08-23 04:33 |
| Owner Tier A Runtime Invocation (implementation) | 2026-08-23 05:39 |
| Owner-Authorized Local File Ingress Scope Lock | 2026-08-23 06:12 |
| OCR Mechanism Unit 12 Runtime Invocation Scope Lock | 2026-08-23 10:26 |
| Docling Concrete Provider Authorization Scope Lock | 2026-08-23 11:00 |
| OCR Mechanism Unit 12 Implementation Plan (corrected) | 2026-08-23 11:09 |
| Unit 12 runtime composition (implementation) | 2026-08-23 14:48 |
| Narrow Reasoning/OCR Precedence Resolution (implementation) | 2026-08-23 15:12 |
| Tier B Owner Routing acceptance (implementation) | 2026-08-23 15:52 |

**Historical blocker, resolved, not current.** `DOCUMENT_INGESTION_TIER_A_IMPLEMENTATION_CLOSURE.md`
§14 and `DOCUMENT_INGESTION_PROGRAMME_GOVERNANCE_CLOSURE.md`'s own table (row 12)
each name one blocker to production OCR/Tier B invocation: an "outstanding
permission/resource authority issue" (whether a dedicated `PermissionAction`/
`ResourceType` pairing is required for OCR invocation). Both documents predate
the Unit 12 Runtime Invocation Scope Lock (2026-08-23 10:26) that resolves it
(§5: the existing `(EXECUTE, DOCUMENT)` gate suffices; no new pairing required
— §19 item 8, **F**orbidden) — the Programme Governance Closure by about 21
hours (2026-08-22 13:16, the day before), the Tier A Implementation Closure by
about 6 hours the same day (2026-08-23 04:01). That resolution was then
implemented and owner-accepted (14:48–15:52). This blocker is resolved, not
outstanding, as of this baseline — confirmed by `git log` chronology, not
merely by the later documents' own claims.

## 4. Implemented capability surface

Fresh-inspected this review, against actual current `src/` and `tests/`
content, not carried forward from a prior report:

| Capability | Implementation | Proving evidence |
| --- | --- | --- |
| Owner-authorized local file ingress | `OwnerLocalFileIngressCoordinator`, `ParkerRuntime.importEvidenceFileAsOwner` | `OwnerLocalFileIngressCoordinatorTest`, `ParkerRuntimeOwnerLocalFileIngressIntegrationTest` — green |
| Evidence Custodian admission | `DefaultEvidenceCustodian.accept` (unmodified by this programme) | pre-existing suite, unaffected |
| Authoritative source manifest | `EvidenceSourceManifest`, `FileSystemEvidenceSourceManifestStorage` | `FileSystemEvidenceSourceManifestStorageTest` — green |
| Tier A owner invocation | `TierAOwnerInvocationCoordinator`, `ParkerRuntime.invokeTierAIngestionAsOwner` (structurally owner-only, no principal parameter) | `TierAOwnerInvocationCoordinatorTest`, `ParkerRuntimeTierAOwnerInvocationIntegrationTest` — green |
| Tier A routing | `GovernedTierADocumentIngestionRouter`, `TierADocumentIngestionComposition` | `GovernedTierADocumentIngestionRouterTest` (seven-fixture matrix, content-signature routing) — green |
| CSV | `ApacheCommonsCsvExtractor` (Commons CSV 1.14.1) | `ApacheCommonsCsvExtractorTest` — green |
| EML | `ApacheJamesMime4jExtractor` (Mime4j 0.8.14) | `ApacheJamesMime4jExtractorTest` — green |
| DOCX | `ApachePoiXwpfExtractor` (POI 5.5.1) | `ApachePoiXwpfExtractorTest` — green |
| Searchable PDF | `TikaPdfStructuralExtractor` (Tika 3.3.1, PDF-only module) | `TikaPdfStructuralExtractorTest` — green |
| `RequiresTierB` classification | `TierADocumentRoutingResult.RequiresTierB` | proven classification-only, never self-invoking (§9, below) |
| Explicit owner Tier B invocation | `ParkerRuntime.analyseEvidence` (reused, unmodified as an invocation surface) | `TierBOwnerRoutingTest`, `TierBOwnerRoutingLiveAcceptanceTest` — green |
| Evidence Intelligence permission gate | `EvidenceIntelligenceInvocationGate`, existing `(EXECUTE, DOCUMENT)` rule | proven pre-OCR, real `ResourceRegistry` entry confirmed |
| Source-integrity verification (Tier B) | `EvidenceIntelligenceOcrCoordinator` (manifest → byte-length → SHA-256 → eligibility) | `EvidenceIntelligenceOcrCoordinatorTest` — green, non-tautological digest proven |
| `OcrMechanism` | `src/interfaces/OcrMechanism.kt` | implemented, sole path |
| `OcrExecutionSequencer` | `src/runtime/OcrExecutionSequencer.kt` | proven unbypassed, reflection-verified |
| Docling adapter | `DoclingOcrProviderAdapter`, `ProcessBuilderDoclingSubprocessInvoker` | `DoclingOcrProviderAdapterTest`, live acceptance — green |
| Python bridge | `tools/docling-ocr-bridge.py` | exercised by every real-subprocess test in this programme |
| Real Docling runtime boundary | External, host-provisioned Python venv (not repository-contained — §15, below) | live acceptance, this review, fresh |
| OCR-derived `EvidenceAnalysisResult` return | `DefaultEvidenceIntelligence.analyse` (reasoning/OCR precedence fix) | live: `Completed` genuinely returned for real fixtures 03/07 |
| Audit/persistence (Tier A) | `FileSystemDocumentIngestionAudit`, `FileSystemDerivativeGenerationStorage` (`prepare → ADMISSION_AUTHORISED → publish → ADMITTED`) | `GovernedTierADocumentIngestionRouterTest` — 10 audit lines for 5 admissions, verified |

## 5. Owner workflow

The only currently supported real, end-to-end path:

```
owner local file
  -> ParkerRuntime.importEvidenceFileAsOwner (explicit)
  -> EvidenceArtifactId
  -> ParkerRuntime.invokeTierAIngestionAsOwner (explicit, separate)
  -> Admitted(format)                       OR   RequiresTierB
       (workflow ends here)                        |
                                                     v
                                        ParkerRuntime.analyseEvidence (explicit, separate, third action)
                                                     |
                                                     v
                                        OCR-derived EvidenceAnalysisResult, returned Completed
```

Each arrow is a **separate, explicit, owner-triggered** call — `DOCUMENT_INGESTION_OWNER_AUTHORIZED_LOCAL_FILE_INGRESS_SCOPE_LOCK.md`
§14's "no-automatic-ingestion rule" names `invokeTierAIngestionAsOwner`, every
Tier A specialist, every OCR mechanism, Tier B, Memory Core, Knowledge, and
Evidence Intelligence explicitly as *forbidden* automatic consequences of a
successful import. Confirmed structurally, not merely by convention:
`TierAOwnerInvocationCoordinator` holds exactly two dependencies
(`EvidenceCustodian`, `TierADocumentIngestionRouter`) — no `EvidenceIntelligence`
or `OcrMechanism` reference exists anywhere in it.

**What this workflow does not yet expose**, by governance's own explicit,
current non-goals (not oversights): a UI; Gmail/IMAP ingestion; directory or
bulk import; filesystem watchers; automatic/background processing of any kind;
a thin `RequiresTierB -> EvidenceAnalysisRequest` convenience helper (optional,
never required, per Unit 12 Scope Lock §18). None of these is required for
closure.

## 6. Tier A capability

- **CSV** — governed Commons CSV extraction; exact structural/literal controls
  (fixture 06) verified; bounded operation; provenance recorded via the
  existing `Provenance`/`Document` Memory Core contracts.
- **EML** — Mime4j; full MIME tree; body fidelity; attachment candidate
  metadata (parent ID/hash, MIME part path, transfer encoding, declared media
  type, decoder identity/version, decoded hash/length); **no automatic child
  admission** — `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` §4 requires "separate
  authorised acceptance" before a decoded attachment becomes its own custodied
  child source, and none exists in this programme's current implementation.
- **DOCX** — POI XWPF; paragraphs/runs/tables/header/footer; explicit hard
  page breaks; metadata/inventory; no fabricated rendered layout (structural
  inventory only, not a rendered visual representation).
- **Searchable PDF** — Tika (PDF-only module, `tika-parser-ocr-module` never
  added anywhere in `build.gradle.kts`); explicit `NO_OCR` disclosure when no
  text layer exists ("Tier B OCR is required and was not invoked" — literal
  string in `TikaPdfStructuralExtractor.kt`); whole-document text; truthful,
  disclosed layout/page-level limitations (not a synthesised or inferred
  layout).

These are **qualifications**, not closure blockers — no adopted governance
document requires richer rendered layout, coordinate-level fidelity, or
automatic attachment materialization for the currently governed scope; each is
an explicit, named, non-blocking deferral (§17, below).

## 7. Tier B / OCR capability

- Scanned PDF (fixture 03) and supported images (PNG, fixture 07) return
  `RequiresTierB` from Tier A — verified fresh, this review, both at the
  composition level (fake-bridge) and live (real Docling).
- `RequiresTierB` remains classification-only — `TierADocumentRoutingResult.RequiresTierB(reason, mediaFacts)`
  carries no execution capability; `TierAOwnerInvocationCoordinator`'s own
  2-dependency structure makes automatic Tier B invocation structurally
  impossible, not merely undocumented.
- The owner must explicitly invoke `analyseEvidence` — proven as a separate,
  temporally distinct action in every end-to-end test (marker-file/route
  evidence shows zero OCR activity between the Tier A call and the explicit
  Tier B call).
- Source integrity is verified before OCR (manifest → byte-length → SHA-256,
  non-tautological — the expected digest is always the manifest's own
  persisted value) — proven via a fresh tamper-after-`RequiresTierB` test.
- Permission is checked before OCR — the existing, unmodified
  `(EXECUTE, DOCUMENT)` gate, proven via an unregistered-principal test
  showing zero OCR invocation even for an already-`RequiresTierB` source.
- `OcrExecutionSequencer` cannot be bypassed — reflection-verified: it is the
  sole type behind `EvidenceIntelligenceOcrCoordinator`, and
  `DoclingOcrProviderAdapter` is the sole type behind it.
- Docling is the sole authorized, implemented provider (`OCR_MECHANISM_DOCLING_PROVIDER_AUTHORIZATION_SCOPE_LOCK.md`);
  no second `OcrMechanism`/adapter exists anywhere in `src/`.
- Resource bounds match the adopted, frozen values (Unit 12 Scope Lock §14),
  independently re-verified against current source this review: max source
  bytes 64 MiB (`67_108_864`), timeout 15 minutes (`900_000`ms), max PDF pages
  200, max image dimension 10,000px, max total pixels 100,000,000, max output
  20 MiB, concurrency exactly 1 (single-permit `Mutex`).
- OCR works fully offline with the provisioned runtime — `HF_HUB_OFFLINE=1`/`TRANSFORMERS_OFFLINE=1`,
  RapidOCR's `onnxruntime` backend (no `modelscope.cn`/network-dependent
  `torch` backend), confirmed via this and prior turns' live runs; no network
  call is made by any code this repository owns.
- The OCR-derived result genuinely returns to the caller — live-proven, this
  review: `analyseEvidence` returns `EvidenceIntelligenceInvocationOutcome.Completed`
  for both real fixtures against real Docling (§11, below).
- No automatic Memory/Knowledge/`DerivativeGenerationRecord` activity follows
  a successful Tier B call — verified both structurally (durability logs
  remain 0 bytes; derivative-generation storage holds no `*.derivative`
  record) and live, this review.

**Repository vs. host provisioning, explicitly distinguished.** The Docling
Python runtime, its model weights, and its own dependency chain (`docling`,
`rapidocr`, `onnxruntime`, PyTorch CPU wheels) are **not** repository-contained
— they live in a separately, manually provisioned host virtual environment
(`~/docling-venv` on the machine this review ran on), resolved at test/runtime
via the `DOCLING_TEST_PYTHON` environment variable or `ParkerRuntimeConfig.doclingPythonExecutablePath`,
never bundled, installed, or downloaded by any code in this repository. This
document does not claim otherwise.

## 8. Source authority / integrity

Unchanged throughout this programme: Evidence Custodian remains the sole
admission and byte-retrieval authority (CDR-006, unreopened); the
authoritative source manifest (`EvidenceSourceManifest`) is the sole source of
truth for byte length and SHA-256; every verification against it compares to
the manifest's own already-persisted value, never a digest recomputed from
the same retrieval and compared to itself (proven identically for Tier A via
`TierAOwnerInvocationCoordinatorTest` and for Tier B via
`EvidenceIntelligenceOcrCoordinatorTest`). Source mutation is prohibited and
verified absent: every fixture test in this programme (composition and live)
compares stored/retrieved bytes against the original file bytes read directly
from `tests/fixtures/document-ingestion-bakeoff/fixtures/`, byte-for-byte,
after every operation.

## 9. Permission boundary

The existing `(EXECUTE, DOCUMENT)` `EvidenceIntelligenceInvocationGate` rule
governs every `analyseEvidence` call, OCR-eligible or not, unchanged since
before this programme began. No new `PermissionAction`, `ResourceType`, verb
phrase, or `ResourceId` was introduced anywhere in Document Ingestion or OCR
Mechanism Unit 12 (Unit 12 Scope Lock §19 item 8, **F**orbidden — confirmed,
fresh, via `git diff`-equivalent inspection of every commit in this programme:
no `resourceRegistry.register`/`vocabulary.register` call was added for OCR or
Tier B specifically). `EVIDENCE_INTELLIGENCE_INVOCATION_RESOURCE_ID`'s
registration in the real, production `ResourceRegistry` is independently,
freshly proven by a dedicated reflection test reaching the real
`DefaultPermissionPolicy` this composition actually wires.

## 10. Derivative / audit semantics

**Tier A** (unchanged by this review): `prepare -> ADMISSION_AUTHORISED ->
publish -> ADMITTED`, each transition independently audited by
`FileSystemDocumentIngestionAudit`; a `DerivativeGenerationRecord`'s own
identity (`DerivativeGenerationId`) is minted once, durably, append-only,
never overwritten; `FileSystemDerivativeGenerationStorage`'s own
`.tmp`/`.prepared` internal staging directories are construction-time
artefacts of the storage mechanism itself, never mistaken in this review's own
verification for a persisted record (an actual record lives at
`<id>.derivative`, directly under the storage root).

**Tier B/OCR** (confirmed, unchanged): produces no `DerivativeGenerationRecord`
at all — Unit 12 Scope Lock §13 explicitly excludes Tier B output from
Document Ingestion's own generation pipeline. OCR output today only ever
becomes `TransientOutput` or nothing — freshly re-confirmed against
`DefaultEvidenceIntelligence.convertOcrOutcome`'s own current body, which has
no branch constructing `CandidateArtifactProduced` or `CandidateRecordProduced`
at all (§5.R of the Unit 12 plan maps `Recognised`/`Partial` to
`TransientOutput` only, by design, as "the single safest default"). A
`TransientOutput` always dispatches to the bare, content-free `NotDispatched`
marker — never durable. `EvidenceIntelligenceAcceptanceCoordinator`'s own
dispatch logic for `CandidateArtifactProduced`/`CandidateRecordProduced` is
real, already-gated, tested machinery — but, freshly confirmed this review, no
producer anywhere in `src/` (for OCR or any other analysis kind) currently
constructs either variant; it is correctly-typed capacity for a future,
separate policy decision, not a currently-reachable-but-merely-untested Tier B
path.

**Reprocessing.** Two independent, explicit `analyseEvidence` calls against
the same custodied source each produce their own, independent
`Completed`/`NotDispatched` result — proven fresh, this review's predecessor
turn, and unmodified since. No deduplication is invented; none is required by
any adopted governance document.

**Reconciliation-required.** Applies only to Tier A's own
`prepare/publish` two-stage sequence (a genuine post-publication
reconciliation state Document Ingestion Unit 4 already governs); inapplicable
to Tier B, which never enters that pipeline (§13, above; Unit 12 Scope Lock
§15).

**Retention/deletion/purge/query limitations** are separately, explicitly
deferred (§17, below) — not implementation blockers for this closure.

## 11. Seven-fixture evidence

Fresh-run, this review, at both the composition level (fake-bridge, offline)
and live (real Docling):

| Fixture | Route | Result |
| --- | --- | --- |
| 01 searchable PDF | Tier A | `Admitted(PDF)` |
| 02 multicolumn PDF | Tier A | `Admitted(PDF)`, with disclosed layout qualifications |
| 03 scanned PDF | Tier A -> `RequiresTierB` -> explicit `analyseEvidence` | `Completed`, real Docling, 14,837ms (this review's fresh live run) |
| 04 DOCX | Tier A | `Admitted(DOCX)` |
| 05 EML | Tier A | `Admitted(EML)`; attachment remains candidate-only |
| 06 CSV | Tier A | `Admitted(CSV)` |
| 07 PNG | Tier A -> `RequiresTierB` -> explicit `analyseEvidence` | `Completed`, real Docling, 13,332ms (this review's fresh live run) |

Source bytes verified byte-identical, before and after, for all seven, at both
levels. `parker.integration.TierBOwnerRoutingLiveAcceptanceTest`'s own
seven-fixture matrix test independently re-confirms all seven routes and
source-immutability in a single run (29.101s, this review).

## 12. Real Docling evidence

Fresh-run, this review: `doclingOcrProviderAdapterLiveAcceptanceTest` — 9/9
passed, 0 failures, 0 skipped, against `docling==2.121.0`, Python 3.12.14,
proving `mechanismIdentity == "docling"`, a genuine, non-null
`mechanismVersion`, and literal, fixture-specific recognised text (for
example, `"SCANNED SYNTHETIC EVIDENCE"`, `"Māori"` for fixture 03) — the
independent, complementary proof (mirroring this programme's own established
"compose two separately-proven facts" reasoning) that the Tier B owner
workflow's own genuinely-returned `Completed` result (§7, §11, above) carries
real, correct Docling output, not merely a structurally well-typed one.

## 13. Source immutability

Verified, every fixture, every test level, this review: the on-disk fixture
files under `tests/fixtures/document-ingestion-bakeoff/` are read-only inputs
(`git status` shows no modification); the custodied copy inside
`EvidenceCustodian`'s own storage is independently, freshly compared
byte-for-byte against the original fixture bytes after every ingestion,
routing, and OCR operation in this programme's test suite, including after a
real, live Docling invocation.

## 14. Authority preservation

Fresh-confirmed, this review, against current code (not assumed from prior
governance text alone):

- Evidence Custodian remains sole source authority — no other component holds
  a write or accept path to custodied bytes.
- Derivative records (`DerivativeGenerationRecord`) never become evidence
  authority — `EvidenceCustodian.accept` remains the only mint point for an
  `EvidenceArtifactId`.
- Memory Core remains the sole canonical Memory authority — no Tier A or Tier
  B path writes to it automatically; every write, where one exists at all,
  passes through Memory Core's own existing, unmodified, generic
  `PermissionEngine`-gated write path.
- Knowledge remains separate and unaffected — no automatic promotion from
  either Tier A or Tier B exists anywhere in this programme.
- Evidence Intelligence gains no source authority — it only ever reads
  already-custodied bytes via the existing `EvidenceCustodian` interface.
- OCR gains no evidential-truth authority — `OcrRecognitionOutcome`'s own
  disclosure is never treated as durable, canonical fact; at most a
  `TransientOutput`, ephemeral and re-derivable.
- QMD/RKS remain subordinate retrieval mechanisms — CDR-008's own boundary
  (Memory Core's retrieval interface, not any producing component, is what
  CDR-008 governs) is untouched by this programme; no canonical or indexing
  authority was added.
- Owner file ingress does not become arbitrary background filesystem
  authority — `importEvidenceFileAsOwner` reads exactly one owner-designated
  path, once, per explicit invocation; no directory scan, no watcher, no
  background process exists anywhere in this programme.

## 15. Dependency / runtime boundary

**Repository dependencies** (`build.gradle.kts`, fresh-checked this review):
`org.apache.tika:tika-core:3.3.1`, `org.apache.tika:tika-parser-pdf-module:3.3.1`
(PDF-only; the full `tika-parsers-standard-package` bundle and
`tika-parser-ocr-module` are never added), `org.apache.commons:commons-csv:1.14.1`,
`org.apache.james:apache-mime4j-core:0.8.14` / `apache-mime4j-dom:0.8.14`,
`org.apache.poi:poi-ooxml:5.5.1`.

**External, host-provisioned runtime** (never repository-contained, never
installed or downloaded by this repository): a Python 3.12.14 virtual
environment with `docling==2.121.0` and its own transitive dependency chain
(`rapidocr` with the `onnxruntime` backend explicitly selected to avoid a
`modelscope.cn`-dependent alternative; PyTorch CPU wheels), plus a one-time,
manually provisioned Hugging Face Hub model cache
(`docling-project/docling-layout-heron`, `docling-project/docling-models`)
resolved locally under `HF_HUB_OFFLINE=1`/`TRANSFORMERS_OFFLINE=1`.

**Optional, live-test-only environment variables:** `DOCLING_TEST_PYTHON`
(required to run any live acceptance instrument at all — its absence causes a
clean `assumeTrue` skip, never a failure), `DOCLING_TEST_BRIDGE_SCRIPT`,
`DOCLING_TEST_MODEL_CACHE_DIR` (both optional, defaulting to the repository's
own committed `tools/docling-ocr-bridge.py` and Docling's own default cache
resolution respectively).

No installation or dependency change was made in this review.

## 16. Test evidence

Fresh-run, this review, against the exact baseline commit:

- Focused suites (Tier A router/owner-invocation, Tier B owner routing, OCR
  composition/adapter/coordinator, Evidence Intelligence, specialist
  extractors, ingress, manifest/audit/derivative storage) — all green.
- `doclingOcrProviderAdapterLiveAcceptance` — 9/9 passed, 0 failures, real
  Docling.
- `tierBOwnerRoutingLiveAcceptance` — 3/3 passed, real Docling, real owner
  workflow, real seven-fixture matrix.
- `./gradlew test --no-daemon --rerun-tasks` — **2610 tests, 0 failures, 0
  errors, 7 skipped** (pre-existing, unrelated live/opt-in instruments gated
  by system properties not set in the ordinary `test` task — honestly
  recorded, not concealed).

## 17. Explicit non-blocking deferrals

Reconstructed fresh, this review, against current adopted governance:

| Item | Classification |
| --- | --- |
| Historical-generation retention/purge | STILL DEFERRED — NON-BLOCKING |
| Audit query/retention/deletion | STILL DEFERRED — NON-BLOCKING |
| Memory registration (which coordinator, which policy) | STILL DEFERRED — NON-BLOCKING |
| Derivative-to-Memory-Core registration for Tier B specifically | STILL DEFERRED — NON-BLOCKING (Unit 12 Scope Lock §17: explicitly not joined without separate governance) |
| Memory/Knowledge deletion propagation to an ingestion-owned reference | STILL DEFERRED — NON-BLOCKING |
| Attachment child-source admission/materialization (EML) | STILL DEFERRED — NON-BLOCKING (requires "separate authorised acceptance," not yet built) |
| Richer nested EML (attachment-of-attachment) | STILL DEFERRED — NON-BLOCKING |
| Rendered DOCX layout (visual, not structural) | STILL DEFERRED — NON-BLOCKING |
| Rendered PDF layout / page-level provenance beyond current disclosure | STILL DEFERRED — NON-BLOCKING |
| Coordinates / bounding boxes (OCR or otherwise) | STILL DEFERRED — NON-BLOCKING (Unit 12 Scope Lock §19 item 14, explicitly **F**orbidden pending future governance) |
| OCR output-quality validation threshold policy | STILL DEFERRED — NON-BLOCKING (Unit 12 Scope Lock §20) |
| Concurrency-bound raising (currently exactly 1) | STILL DEFERRED — NON-BLOCKING, raise only on demonstrated need |
| Future Tier B `DerivativeGenerationRecord` mechanics (Document-Ingestion-side recording of an externally-obtained Tier B result) | STILL DEFERRED — NON-BLOCKING |
| Tier B Memory mechanics generally | STILL DEFERRED — NON-BLOCKING |
| UI | NOT PART OF CURRENT GOVERNED SCOPE |
| Gmail/IMAP ingestion | NOT PART OF CURRENT GOVERNED SCOPE (explicitly named non-goal) |
| Filesystem watchers | NOT PART OF CURRENT GOVERNED SCOPE (explicitly named non-goal) |
| Directory/bulk import | NOT PART OF CURRENT GOVERNED SCOPE (explicitly named non-goal) |
| Automatic/background processing of any kind | NOT PART OF CURRENT GOVERNED SCOPE (explicitly, repeatedly forbidden) |
| Thin `RequiresTierB -> EvidenceAnalysisRequest` convenience helper | STILL DEFERRED — OPTIONAL, never required |
| OCR Unit 12 permission/resource pairing question | **RESOLVED** (Unit 12 Runtime Invocation Scope Lock §5, implemented, live-proven) |

## 18. Exclusions

This closure does not authorize, and this document does not claim: owner-UI
ingestion; Gmail/IMAP ingestion; directory or bulk import; filesystem
watchers; automatic or background evidence processing of any kind; automatic
Tier A-to-Tier-B invocation; automatic Memory Core registration or Knowledge
promotion from either tier; a `DerivativeGenerationRecord` for Tier B output;
universal document-format support (only CSV, EML, DOCX, searchable/scanned
PDF, and supported raster images are governed); OCR result content as
evidential truth.

## 19. Limitations

OCR text is never source text (it is always a derivative, provenance-linked
back to its source `EvidenceArtifactId`). The Docling runtime is external,
host-provisioned, and not repository-contained — a real deployment must
provision it separately; this repository provides no installer, downloader,
or bundled model. `analyseEvidence`'s own principal handling remains a
caller-discipline guarantee, not a structurally owner-only one (disclosed,
pre-existing, unmodified by this programme). Layout fidelity for DOCX/PDF is
structural/textual, not a rendered visual reproduction. EML attachments
surface as disclosed candidates only, never as automatically admitted child
evidence.

## 20. Reopening conditions

This closure may be reopened, narrowly, by any of: a defect discovered in the
implemented, closed capability surface (§4–§14, above); a decision to resolve
any item in §17's deferral table, each requiring its own separate,
independently authorized governance and implementation unit; a decision to
extend the governed format/capability surface (for example, additional file
formats, coordinate disclosure, richer layout); a decision to build any of the
explicitly out-of-scope surfaces named in §18. None of these is triggered by
this document; each requires its own future proposal.

## 21. Formal closure statement

**The currently governed Document Ingestion Programme implementation is
formally closed as technically complete within its adopted scope. This
closure records the implemented owner-authorized ingress, Tier A
extraction/routing, explicit Tier B OCR invocation, provenance, integrity,
persistence and audit capability demonstrated by the governed acceptance
evidence in this document. It does not supersede underlying governance,
migrate evidential authority, authorize automatic processing, claim universal
document understanding, resolve expressly deferred retention/Memory/layout/attachment
matters, or prevent future separately governed extensions.**
