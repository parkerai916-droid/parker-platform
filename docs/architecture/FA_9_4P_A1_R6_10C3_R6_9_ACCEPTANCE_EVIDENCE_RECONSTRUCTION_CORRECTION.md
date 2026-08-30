# FA.9.4P-A1E-R6.10C3 — R6.9 acceptance-evidence reconstruction correction

## Outcome

The sole R6.10D-R2 production promotion POST returned HTTP 409 with `R6_9_PROVIDER_EVIDENCE_UNAVAILABLE_OR_CORRUPT`. It was not retried. Forensic decoding proves that the required R6.9 authority, attempt, provider response, structured projection, and assessment are present and cryptographically intact. The failure was an implementation reconstruction defect, not missing or corrupt evidence.

Starting repository HEAD and `origin/main` were both `5daa344503d8702019b3f398b0507988ec585fde`, with a clean worktree. Governing C2/C1/C and B–B5 report hashes matched their locked values.

## Exact durable records

| Role | Exact record | Integrity/result |
|---|---|---|
| Authority | `/data/region-transcription-acceptance-authorities/authority-fa-9.4p-a1e-r6.8c1.region-acceptance-authority-v2` | file SHA `05b8407e14fc7b2bf9c8d3f2d2519981c5e39070559bc90eaf5edb1cc6ba984d`; envelope/payload/record ID `5497f12e65d6e7a4d795cfec22ee3aa99c40eb00a8fc2e9f76835b5cfb2d23c9` |
| Governed attempt | `/data/external-transcription-attempts/parker-encoded-execution-fa-9-4p-a1e-r6-8c1--5c4ce3ccfff5eac79fb3f174e62b189f1abfd0e9854ac398967a5b78d08e87e8.fidelity-attempt-ledger` | SHA `e6e3a6eba4a2033e58c23a06c39aed95fc1a7c880cd3e568ee0ae01d7806741d`; exact execution and correlation; terminal historical stage `PROVIDER_RESPONSE_RECEIVED` |
| Other historical attempt | `/data/external-transcription-attempts/parker-encoded-execution-fa-9-4p-a1-r2-e0d31561-65f7-4--29e83b013096be59a328e3cc3dfcdf4b6b61804a4c092af90e84dc08a42150c8.fidelity-attempt-ledger` | Enumerated but rejected by governed identity; never selected by first/only/latest logic |
| Provider response | `/data/external-region-provider-state/31b997b2a5208ab120fa483778bca9f1ec270c994b7937b3e5dd765db2bfabcd.provider-state` | file SHA `edd6b3fcf357edfcfce0e6825e2b8734462fb2654f6816255d8e6305b080b4c9`; internal record SHA `ad2542015546250bfe0640e5c31636bb6401a20d537d95db04248c81883ad135` |
| Assessment | `/data/external-region-provider-state/31b997b2a5208ab120fa483778bca9f1ec270c994b7937b3e5dd765db2bfabcd.assessment` | file SHA `62a5b9a266db3cf4702eb5583f61f8a2f23d96e4954371b2750ddf07ebb50ae8`; evidential payload identity `39fbc01c7cf831ebf5fc0751cfcca73310bc1a1a1846508ff46d64c61bd09da7` |

The exact provider record validates request digest `1a691388478370add9bae4e920fb1071369efa543057403727b422e9000a3d36`, raw-response SHA `500863d65c7f9ca69a66b2ffef3ef8a42b7033903cf1b5a5bd774d9f0decd87f`, structured-state SHA `7031179aa4267fdc12a50a429eef184e4ecfb2efb3ae993b6a5527ecf9f4c476`, and complete-record SHA above. The checksum-bound raw Responses envelope contains response ID `resp_0007d6aa81587b3e016a92f716feb087d0ae9e005456676627`, model `gpt-5.6-sol`, status `completed`, no provider error, and usage 19,364 input / 4,803 output / 24,167 total tokens.

## Complete reconstruction trace

There are 12 governed reconstruction stages.

| # | Stage and implementation | Expected identity/input and store | Pre-fix production result | Corrected result |
|---:|---|---|---|---|
| 1 | Promotion guards, `OrdinaryRegionCapabilityAcceptanceCoordinator.create` | canonical capability and exact embedded build | PASS | PASS |
| 2 | Acceptance lookup, `FileSystemOrdinaryRegionCapabilityAcceptanceStore.findExact` | empty capability store and exact build | PASS, no record | PASS, no record |
| 3 | Authority lookup/codec, `FileSystemRegionAcceptanceAuthorityStorageV2.load` | exact `authority-fa-9.4p-a1e-r6.8c1` and authority root | Not inspected | PASS; v2 checksum and record identity verified |
| 4 | Authority semantics | exact execution, correlation, single-attempt purpose, historical provider surface | Not inspected | PASS |
| 5 | Attempt lookup/codec, `FileSystemFidelityFirstAttemptLedger.readExisting` | exact governed execution in attempt root | Not inspected | PASS; checksum-bound exact identity selected |
| 6 | Authority → attempt linkage | authority facts bound to execution, request, correlation, source, provider and historical adapter | Not inspected | PASS |
| 7 | Historical attempt-stage semantics | one start and one response; historical last stage may be `PROVIDER_RESPONSE_RECEIVED` | Not inspected | PASS; no terminal success/admission requirement imposed |
| 8 | Provider-state lookup/codec, `FileSystemRegionProviderStateStore.read` | fixed record ID in provider-state root | PASS internally | PASS |
| 9 | Attempt → provider linkage | request digest and fixed provider identity | Partially implicit only | PASS explicitly |
| 10 | Assessment linkage/codec | matching record/request/raw identities and payload assessment digest | PASS internally | PASS; file fingerprint is not substituted for payload identity |
| 11 | Historical response identity reconstruction, `DurableOrdinaryRegionR69EvidenceLoader.load` | response identity from verified historical representation | **FAIL**: v4 structured projection has `provider_response_id=null` | PASS: verified raw envelope supplies exact response ID/model/status/usage |
| 12 | Typed R6.9/A/B/C chain and temporary acceptance | exact C1 roles, R6.9B wire v5, R6.9C `PASS_FIDELITY`, exact build | Unreached | PASS; exactly one temporary acceptance |

## Defects collected before correction

Total reconstruction defects: **3**.

1. `HISTORICAL_CODEC_COMPATIBILITY_DEFECT`: C1 required the response ID from the historical v4 structured projection even though that field was null there and intact in the checksum-bound raw Responses envelope.
2. `AUTHORITY_TO_ATTEMPT_LINKAGE_DEFECT`: C1 did not load and cryptographically bind the exact v2 authority to the exact historical attempt ledger.
3. `ATTEMPT_TO_PROVIDER_STATE_LINKAGE_DEFECT`: C1 did not explicitly bind the attempt request identity and required response-received stage to the fixed provider-state record.

No `ACTUAL_DURABLE_RECORD_CORRUPTION` or `ACTUAL_REQUIRED_EVIDENCE_MISSING` condition exists.

## Historical semantics and correction

The authority and attempt correctly describe the capture-time v4/adapter-3.0/provider-profile-v1 surface. The correction does not rewrite those facts or require the historical provider record to claim wire v5. R6.9B remains the typed governed evidence that the exact preserved output validates under wire-v5 point-anchor semantics. R6.9C remains the typed `PASS_FIDELITY` review (24 reviewed, 24 EXACT, zero other discrepancies).

`VALIDATION_MALFORMED_SCHEMA` is retained as the exact assessment outcome. It records the original 70 zero-length `LINE_BREAK` rejection and is not treated as corruption, terminal failure, or a requirement for historical `TERMINAL_SUCCESS`, `ADMITTED`, or `PASS_FIDELITY` ledger stages.

The corrected loader now:

- loads the exact v2 authority by governed identity;
- derives and checks the exact historical attempt identity from authority facts;
- reads the exact attempt without creating a lock or record;
- requires one provider attempt and one received response, ending at the intentional historical boundary;
- loads the fixed provider-state record and validates all exact hashes and the assessment payload identity;
- parses only the already-digest-verified raw response to recover response ID and validate model, completed status, no error, and usage;
- builds the unchanged C1 typed R6.9/A/B/C evidence chain.

No fuzzy search, first-file selection, migration, reserialization, or durable repair is used.

## Offline replay and negative verification

An exact four-file copy of the production record set is held at `tests/fixtures/r69-production`. `OrdinaryRegionR69ProductionEvidenceReplayTest` runs the actual stores/codecs and corrected coordinator against copies in temporary roots.

Exact replay results:

- evidence reconstruction: PASS;
- typed evidence chain: COMPLETE;
- promotion eligibility: PASS;
- temporary acceptance creations: exactly 1;
- provider calls: 0;
- owner authorizations: 0;
- attempt starts: 0;
- derivative writes: 0;
- fixture input hashes unchanged.

Negative cases fail closed for wrong authority, wrong execution, wrong request digest, wrong provider-state ID, wrong response ID, wrong raw SHA, wrong structured SHA, missing provider file, corrupted provider checksum, provider state from another attempt, missing assessment, wrong assessment identity, missing/incorrect R6.9B semantics, non-PASS R6.9C classification, and the historical malformed outcome without the complete governed B/C chain.

C1 typed acceptance, C2 read-only capability status, authenticated GET/POST handler behavior, dynamic acceptance, offline ordinary-ingestion E2E, historical provider-state decoding, R6.9A replay, R6.9B point-anchor replay, R6.9C fixture, authorization/revocation, attempt recovery, and derivative admission are covered by the passing complete suite.

## Verification and production preservation

The complete Ubuntu suite passed: **3,245 tests, 236 suites, 0 failures, 0 errors, 9 conditional skips**. `git diff --check` passed.

Production remained unchanged throughout C3:

- container `17ecdcfa2138ddba5baae9bcb70bdc39c71fc9e5ddfc37e28e35c9c75b3daf1e`;
- image `sha256:e94f0ccc9d978ce83f3c71f5ed3afe045af48e8331b6ea9d65cdc4402abbf0cc`;
- embedded commit `5daa344503d8702019b3f398b0507988ec585fde`;
- restart count 0 and running;
- capability `CAPABILITY_NOT_ACCEPTED`;
- production acceptance records 0;
- production owner authorizations 0;
- production POST retries 0;
- OpenAI calls 0 and Claude calls 0;
- attempt/provider/assessment/audit fingerprints unchanged (`e6e3a6…`, `edd6b3…`, `62a5b9…`, `4f585b…`);
- derivative-content and derivative-generation preservation snapshots remained unchanged; the locked baseline aggregate identities remain `521050…` and `55176e…`.

Filesystem capacity is 4.4 GB available (93% used). The prior 6.85 GB BuildKit-cache prune is operational context only; no Parker persistent root was cleaned or modified.

## Changed implementation

- `src/runtime/OrdinaryRegionIngestion.kt`
- `src/runtime/FidelityFirstTranscriptionAttemptLedger.kt`
- `src/composition/ParkerRuntime.kt`
- `tests/runtime/OrdinaryRegionR69ProductionEvidenceReplayTest.kt`
- four immutable exact-record fixtures under `tests/fixtures/r69-production/`

## Requirement for resumed D

Because C3 changes source, B3 exact-build equality requires resumed D to build and deploy the exact C3 commit. It must then observe GET `CAPABILITY_NOT_ACCEPTED`, invoke the promotion POST exactly once, observe GET `ACCEPTED`, and prove zero egress. The currently running C2 build must not be promoted.
