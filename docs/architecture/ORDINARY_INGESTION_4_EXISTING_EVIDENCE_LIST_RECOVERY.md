# ORDINARY-INGESTION-4 Existing Evidence List Recovery

## Baseline and safety

- Starting repository HEAD/upstream: `868ac1248a374046ba7470984e3b4e89253b7aa1`; worktree clean.
- Starting production: container `1a7e2120f6cb47335b0c34253386adab28bd97134d4429e19acb1179a6c0c653`, image `sha256:6e811f2af485f8e65ade9a44e10cc1d49041bd41b7679eff2cc5b7b1bcbc4588`, implementation `3ae55f492e10f18b9dca0846114bd80458680fe6`, running, restart count zero.
- Root HTTP 200; unauthenticated owner boundary HTTP 401; region-v5 `ACCEPTED`; capability acceptances 3.
- Symptom: the browser table began with zero rows after reload even though evidence had previously been visible.
- No production evidence was uploaded, re-registered, rewritten, moved, renamed, or deleted. No authorization or execution action was invoked. OpenAI calls: 0. Claude calls: 0.

## Durable custody proof

The running container maps `/mnt/parker-data/parker/evidence` to `/data/evidence` and `/mnt/parker-data/parker/evidence-source-manifests` to `/data/evidence-source-manifests`; both mounts were and remain present and writable. The active Compose configuration points to these exact container roots.

There were 27 `.evidence` byte objects and 27 `.manifest` registrations. Both required records are complete and retrievable:

| Evidence | ID | Bytes | Media type | SHA-256 | Manifest filename fact |
|---|---|---:|---|---|---|
| Parker Platform Sprint 2 comms.pdf | `evidence-4c6f2ee8-2f62-47be-bd7a-946c744b2766` | 124,027 | `application/pdf` | `ce8bd4b53d8b007026575974014e71f648f045bf3970b0e984605cf842a7b4a5` | `owner-upload-14544486980779959571.part` |
| Steven_McTague_CV_Traditional_Format.pdf | `evidence-99b1c6fa-91e7-4a30-b212-b7a677718417` | 6,029 | `application/pdf` | `3e7e3777671a22465e8200c8daecdeb910fbe81cf63a79d2effedfbd038db85b` | `owner-upload-430420510581783368.part` |

The legacy manifests truthfully contain server temporary upload names rather than the browser-local display names. Recovery does not fabricate or rewrite immutable metadata; rows remain unambiguously identifiable by their preserved evidence IDs and hashes.

Starting and final evidence byte fingerprint: `a39fa0d33d73c5b1df28e6dec43656dd618756bcec822e1f81fec59f9e9ccd97`. Starting and final manifest fingerprint: `d864ecda79b84782109903c19ed9417e6028bdc5536fcac782edffc38483b266`.

## Complete retrieval trace before correction

| Stage | File/class | Method/route | Input | Output | State | Expected | Actual | Filtering | Mismatch/classification |
|---|---|---|---|---|---|---:|---:|---|---|
| Authoritative bytes | `FileSystemEvidenceArtifactStorage` | keyed files | evidence ID | exact bytes | persistent | 27 | 27 | none | none |
| Registration | `FileSystemEvidenceSourceManifestStorage` | keyed manifests | evidence ID | decoded manifest | persistent | 27 | 27 | none | none |
| Repository enumeration | none | none | durable registrations | identities | absent | 27 | 0 | none | `C` |
| Listing service | none | none | identities | owner summaries | absent | 27 | 0 | none | `C` |
| Runtime composition | `ParkerRuntime` | none | owner request | list | absent | 27 | 0 | none | `E` |
| Owner adapter | `OwnerEvidenceOperations` / `OwnerUiEvidenceRuntimeAdapter` | none | owner request | presentation rows | absent | 27 | 0 | none | `G` |
| HTTP | `OwnerEvidenceHttpServer.EvidenceHandler` | authenticated `GET /owner/evidence` | bearer-authenticated GET | JSON list | request | 27 | HTTP 404 | none | `F` |
| Browser parsing | owner page JavaScript | none | list JSON | `rows` | in-memory | 27 | 0 | none | `D`, `E`, `L` |
| Table rendering | `render()` | existing `rows` | rows | table DOM | in-memory | supplied rows | supplied rows render | none | no renderer defect |

Total defects before editing: **6** — `C. LISTING_SERVICE_NOT_COMPOSED`, `D. IN_MEMORY_ONLY_LISTING`, `E. STARTUP_RECONSTRUCTION_MISSING`, `F. HTTP_LIST_ROUTE_DEFECT`, `G. OWNER_ADAPTER_LIST_DEFECT`, and `L. SESSION_STATE_DEFECT`.

There was no wrong root, missing mount, configuration defect, pagination, status filter, or hidden evidence filter. OI3 did not cause the defect: its exact diff added authorization state/action wiring without removing or replacing a list path. The session-only table design predated OI3; the deployment/reload merely made it visible.

## Correction

- Added `FileSystemOwnerEvidenceListing`, an explicit durable discovery mechanism—not a fallback index. It enumerates only canonical manifest records, re-decodes each through the existing manifest storage codec, retrieves the corresponding bytes through the governed `EvidenceCustodian`, and verifies identity, byte length, and SHA-256 before returning any row.
- Listing fails closed as a whole for corrupt, missing, denied, or integrity-mismatched custody; it silently repairs or omits nothing.
- Added the narrow `ParkerRuntime.listRegisteredEvidenceAsOwner` and owner adapter projection.
- Added authenticated, read-only `GET /owner/evidence`.
- Added page-start and explicit-refresh loading into the existing renderer. Reload no longer depends on the current upload session.
- No evidence index, mutable registration, acquisition semantic, authorization semantic, provider code, or execution path was added or changed.

Changed implementation files: `src/runtime/OwnerEvidenceListing.kt`, `src/composition/ParkerRuntime.kt`, `src/ui/parker/ui/OwnerEvidenceUpload.kt`, `src/composition/OwnerUiEvidenceRuntimeAdapter.kt`, `src/composition/OwnerUiRuntimeComposition.kt`, `src/composition/Main.kt`, and `src/composition/OwnerEvidenceHttpServer.kt`; plus focused tests.

## Verification

- Focused listing, adapter, HTTP/UI, OI3 authorization, ordinary ingestion, and Evidence Artifact storage suites: PASS.
- Full `./gradlew test`: 3,254 tests, 0 failures, 0 errors, 9 skipped.
- `git diff --check`: PASS.
- Tests cover empty durable stores, multiple records, recreation of listing composition, pre-process records, unchanged IDs/hashes, canonical backend projection, authenticated HTTP listing, browser startup loading, accepted-PDF proposal reconstruction, authorization availability, and fail-closed custody inconsistency.
- Listing tests use temporary stores and fake custody only. Listing creates zero authorization, attempt, provider-state, derivative, or provider calls.

Accepted region-v5 provider, endpoint, model, reasoning, adapter/version, profile, wire/schema, renderer, geometry, request limits, batching, `store=false`, retry, provider-state, authorization, and derivative-admission semantics are unchanged.

## Exact build, deployment, and promotion

- Implementation commit: `ea1d96d656e97c7ed350eeabec5ef279b8ac36bb` (`origin/main`).
- Build input and `PARKER_BUILD_COMMIT`: exact implementation commit, captured mechanically from `git rev-parse HEAD`.
- Image: `sha256:6fa813f9f4c454652329cfb0ec08399055be360ff959fe1de71f98bb41b7e256`.
- Deployment-local source/production commit: exact implementation commit.
- New container: `e11b08cc6955faf29c0274d5fcd10f745563086f28d58a7c1cc2237db1c63b88`, running, restart count zero.
- Root HTTP 200; unauthenticated owner boundary 401; correct evidence mounts retained. The only `error|exception` log match is Log4j's known missing-provider diagnostic, not a Parker startup failure.
- Pre-promotion: `CAPABILITY_NOT_ACCEPTED`, exact runtime commit, null accepted commit.
- Promotion POST count: exactly 1. HTTP 201 `CREATED`; record `e29b10970d63890a260e49c42f3d26103b7aece397d43e122d8557b209a63da3`.
- Post-promotion without restart: `ACCEPTED`; runtime and accepted commits both equal the implementation commit.

## Live recovery and final governed state

Authenticated production `GET /owner/evidence` now returns 27 rows. Both required IDs are present without re-upload, with unchanged byte lengths, media type, and hashes. The served page performs this authenticated GET at startup and on `Refresh existing evidence`; deterministic HTTP/UI verification proves returned rows enter the existing table renderer.

Sprint 2 final acquisition state is `PROPOSED`, `NOT_AUTHORISED`, `OWNER_AUTHORIZATION_REQUIRED`, `authorizationAvailable: true`, `executeAvailable: false`. The CV is listed and likewise remains `PROPOSED` / `NOT_AUTHORISED`; neither was authorized or executed.

Final counts: capability acceptances 4; owner authorizations 0; attempts 4; provider-state 2; region acceptance authorities 1; derivative generations 21; derivative content 19; evidence bytes 27; manifests 27. Derivative generation/content fingerprints remained `55176ee0c863edca19834419c96161b42c90d9aa5f828eccc7ffeee093ff9c25` / `521050570f8b6de018603d2e1bcb65ffc3e3d64b0a55e4605dede4999d8050ba`.

OpenAI calls: 0. Claude calls: 0. Production authorizations: 0.

## Verdict

UNIT ORDINARY-INGESTION-4 COMPLETE — DURABLY REGISTERED PARKER EVIDENCE IS AGAIN EXPOSED THROUGH THE OWNER LIST AFTER RELOAD/DEPLOYMENT; EXISTING EVIDENCE IDENTITIES AND AUTHORITATIVE BYTES ARE UNCHANGED; SPRINT 2 PDF RETURNS TO PROPOSED / NOT_AUTHORISED WITH EXPLICIT AUTHORIZATION AVAILABLE; ZERO PRODUCTION AUTHORIZATION OR PROVIDER EGRESS OCCURRED

## Exact next step

Stop. The owner may reload the production page, select/open the Sprint 2 evidence row, review `PROPOSED`, `NOT_AUTHORISED`, and `Authorize external transcription`, and only then explicitly authorize that exact document. No development agent performs that owner action.
