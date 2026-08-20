**Status:** Unit 3-BF Family F Alternative Diagnostic Host Requirements Scope Lock — **PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW. Corrected in place against the now-merged raw transport capture defect correction** (`c5b90fa7c187e68d0d7fb2a9c165202932d482c7`, merged as `c419db3e570bef101c200637fb6668837d77b148` via PR #26), which superseded the prior implementation's hash-only transport ledger with a durable transport ledger that persists complete raw request/response bytes and full response headers. This correction updates Section 9.2's evidence-budget formula (`E`) to size that corrected transport representation exactly once per field, and re-audits `TEMP_DUPLICATE_BYTES` fresh against the corrected implementation commit rather than inheriting its prior value. Governance-only against original drafting baseline `938667919f445d05d654624c1a3d3c01a17613eb`. Freezes the binding requirements a candidate diagnostic host must satisfy before any future Readiness Review may evaluate it, without selecting, naming, identifying, inspecting, provisioning, or contacting any specific host. It authorizes no model acquisition, no daemon launch, no model load, no inference, no campaign, no candidate-host assessment, and no Knowledge Discoverability Attempt 3.

**Model-identity premise correction.** Corrected in place against the accepted Model-Identity Premise Defect Confirmation Review and its Independent Constitutional Review (commit `4d8d5012243df955683fe929a6cf7a0dc6766ffc`): this document's own designation of `llama3.2:3b` as control identity is not itself rewritten, but rests on an upstream premise — that `llama3.2:3b` was Parker's current, live, or production model — now corrected; `qwen2.5-coder:7b`, not `llama3.2:3b`, was Parker's committed deployed Docker baseline throughout this programme. CONTROL_MODEL/SUBJECT_MODEL roles and the Family F research question remain unresolved, pending separate governance. The remainder of this document's body is unmodified and remains the historical record of this review.

# Reasoning Protocol Live-Model Conformance — Unit 3-BF Family F Alternative Diagnostic Host Requirements Scope Lock

## 1. Baseline and controlling authority

This Scope Lock is drafted against merged repository baseline `938667919f445d05d654624c1a3d3c01a17613eb`, with a clean worktree and `HEAD == origin/main` confirmed before branch creation.

Its controlling authority, read completely and fresh where required by this task:

- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_READINESS_BLOCKER_RESOLUTION_PLANNING_REVIEW.md` and its accepted Independent Constitutional Review (`VERDICT=ACCEPTED`, `P0=P1=P2=P3=0`) — the Planning Review whose sole recommended next action this document performs;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_READINESS_REVIEW.md` and its accepted Independent Constitutional Review — the corrected, `READINESS=NOT READY` Readiness Review whose Items 7–12 findings this document exists to make resolvable on a properly qualified host;
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_EXPERIMENTAL_RECLASSIFICATION_AND_QUALIFICATION_BOUNDARY_SCOPE_LOCK.md` — the Unit 3-BF Scope Lock fixing subject/control identity, corpus, profiles, repetitions, schedule, and the absolute advancement gate;
- `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_EXECUTION_PLAN.md` and its accepted Independent Constitutional Review — the Plan fixing the memory-gate formula, disk-gate minimums, dedicated-endpoint requirements, and the mandatory governance sequence this document extends, not replaces;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_COMPLETION_REVIEW.md` and `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` — the accepted implementation reviews confirming the three-file harness this document's future candidate-host assessments and Readiness Reviews will run against; superseded in their raw-capture-related sections only, per the two documents below;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_RAW_TRANSPORT_CAPTURE_DEFECT_CONFIRMATION_REVIEW.md` and its accepted Independent Constitutional Review — the review confirming the pre-correction transport ledger persisted only hashes, never complete raw bytes or headers, and identifying the minimum lawful correction boundary;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_RAW_TRANSPORT_CAPTURE_CORRECTION_COMPLETION_REVIEW.md` and its accepted Independent Constitutional Review — the review confirming the corrected `FamilyFCampaignLedger.recordTransport` (`tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:929-964`, commit `c5b90fa7c187e68d0d7fb2a9c165202932d482c7`, merged as `c419db3e570bef101c200637fb6668837d77b148` via PR #26) now durably persists complete raw request/response bytes and full multi-valued response headers, the source this correction's Section 9.2 update is verified against.

This Scope Lock does not reopen, diminish, or restate a verdict on any of the above. Implementation acceptance remains valid. `READINESS=NOT READY` for the currently governed host remains valid and undisturbed. This document's sole subject is defining, in advance and in the abstract, what a *different* host would have to prove.

## 2. Purpose and boundary

The sole purpose of this Scope Lock is to freeze binding, reviewable requirements a candidate diagnostic host must satisfy before it may be proposed to, or evaluated by, a future Readiness Review.

This document does not:

- select, name, identify, reserve, provision, configure, or contact any specific host;
- inspect any host's actual identity, resources, or processes;
- acquire, pull, download, copy, or convert a model artifact;
- launch, start, stop, or configure any daemon, container, or process;
- load a model, perform inference, or contact any model endpoint;
- create, begin, or perform a candidate-host assessment (Section 13 defines its protocol only);
- weaken, relax, or create an exception to any Unit 3-BF Scope Lock or Plan requirement; or
- authorize Knowledge Discoverability Attempt 3.

## 3. Relationship to existing governance

This document is additive, not substitutive. Every requirement the Unit 3-BF Scope Lock and the Implementation/Execution Plan already fix — subject/control identity, the frozen corpus and profiles, the 392-call schedule, the absolute advancement gate, the memory-gate formula, the disk-gate minimums, the dedicated-endpoint and production-isolation rules — remains controlling and unchanged. Where this document states a requirement more precisely than its predecessors (for example, Section 9's disk-consumption formula or Section 8's measurement-integrity rule), the more precise statement is a clarification of the same underlying requirement, not a new or looser one. Where any wording here can reasonably be read more broadly than the Unit 3-BF Scope Lock or the Plan, those documents remain authoritative.

## 4. Frozen invariants — unaffected by host choice

No candidate host, however qualified, changes any of the following. These remain fixed exactly as the Unit 3-BF Scope Lock and Plan already froze them:

```text
- subject identity: qwen2.5-coder:7b — no substitution, regardless of host
- control identity: llama3.2:3b — no substitution, regardless of host
- no quantization change, smaller model, or different provider substituted for either role on any host
- the complete 23-fixture, two-profile corpus — no reduction on any host
- four repetitions per fixture/profile/model cell — no reduction on any host
- both minimal-production-context and mixed-full-production-like profiles — no reduction on any host
- the exact 368 scored + 24 warm-up = 392-call envelope — unchanged on any host
- the absolute, subject-only, 46-cell advancement gate — unchanged on any host
- no ranking, comparative, or selection authority created between subject and control on any host
```

A host that could only be made to work by relaxing any of the above is not a qualifying host; it is evidence that no qualifying host has yet been found, and Path 4 (remain blocked) of the accepted Planning Review continues to govern.

## 5. Requirement category 1 — host identity

A candidate host must supply, before any further category is evaluated:

```text
HOST_IDENTIFIER: an immutable identifier (hostname, cloud instance ID, hardware serial, or equivalent) sufficient to distinguish this host from every other host this programme has used or could use
OPERATING_SYSTEM: exact distribution and version
KERNEL: exact kernel release
ARCHITECTURE: exact CPU architecture (e.g. x86_64, aarch64)
CPU_CLASS: exact model/class identifier
CORE_COUNT: physical core count and logical (thread) count, separately stated
TOTAL_PHYSICAL_RAM: exact byte figure from a fixed, non-fluctuating source (e.g. MemTotal), not an available/free figure
FILESYSTEM_IDENTITIES: exact device and filesystem identity for every filesystem underlying the proposed evidence root and the proposed dedicated-runtime root — device path, filesystem type, and mount point, not merely a directory path
EVIDENCE_SOURCE_AND_METHOD: for every value above, the exact command or file read used to obtain it, and a timestamp for when it was collected
```

Every value above must be collected read-only, without modifying host state, and re-collectable by an independent reviewer using the same disclosed method. A value asserted without a disclosed collection method and timestamp does not satisfy this category.

## 6. Requirement category 2 — provider identity and access

```text
PROVIDER_EXECUTABLE_PATH: the resolved, absolute filesystem path to the actual provider binary that will serve inference requests — not a symlink, not a container entrypoint string alone, not an inferred default
PROVIDER_EXECUTABLE_SHA256: the SHA-256 of the exact bytes at that resolved path, computed read-only
PROVIDER_IMAGE_IDENTITY: where the provider runs inside a container or similar isolation layer, the image reference, image ID, and image/manifest digest — recorded as a distinct, named field, never substituted for the executable's own SHA-256 above
PROVIDER_VERSION: the provider's self-reported version string
ACCESS_SUFFICIENCY: an explicit statement of how PROVIDER_EXECUTABLE_SHA256 was obtained read-only, without `docker exec`, `docker cp`, `docker export`, `docker save`, any other extraction action, `sudo`, or any other privileged workaround — if no such read-only avenue exists on a candidate host, PROVIDER_EXECUTABLE_SHA256 is unestablished for that host and the host does not satisfy this category, exactly as this programme's own currently governed host does not
DEDICATED_LAUNCH_PROCEDURE: the exact, reproducible procedure by which a dedicated (non-production) instance of this provider would be started on the candidate host, and the exact binary identity (path + SHA-256, or image + digest) that procedure launches
```

This category exists precisely because the currently governed host cannot satisfy it (the Readiness Review's Item 7 finding): passive container/image metadata alone is not sufficient. A candidate host must supply the resolved executable's own SHA-256 through a genuinely read-only avenue, or it fails this category regardless of how favorable its other properties are.

## 7. Requirement category 3 — frozen model artifacts

```text
SUBJECT_ARTIFACT: exact identity evidence for qwen2.5-coder:7b — immutable digest, quantization descriptor, artifact size in bytes, the exact manifest content (or manifest-equivalent metadata the provider exposes), and provider-visible model name/tag
CONTROL_ARTIFACT: the same complete evidence set for llama3.2:3b
ACCESS_METHOD: for both artifacts, the exact read-only method used to obtain the above — filesystem inspection of a manifest/blob store the assessing account can actually read, or an equivalent read-only provider-metadata path that does not require model loading, inference, or a live endpoint call
NO_SUBSTITUTION: neither artifact may be a different model, a re-quantized variant, a converted format, an aliased tag pointing to different underlying bytes, or served by a different provider than the one identified in Section 6 — any of these is a hard disqualification of the candidate host, not a defect to work around
```

Exactly as Section 8 (Item 8) of the accepted Readiness Review found for the currently governed host, if the exact on-disk manifest/blob location for either artifact is not readable by the account that will perform the future Readiness Review, this category is unsatisfied for that host, regardless of whether the host administrator asserts the correct model is installed.

## 8. Requirement category 4 — memory

```text
PRE_LOAD_GATE: before loading either model, MemAvailable >= verified artifact size (Section 7) + 2 GiB
PER_CALL_GATE: at least 2 GiB MemAvailable immediately before and immediately after every one of the 392 calls
MEASUREMENT_INTEGRITY: no swap enablement, cache-clearing, process stopping, service shutdown, or removal of an ungoverned workload may be used to inflate a MemAvailable reading and treated as satisfying either gate above, unless a future, separate governance act expressly authorizes and records that specific action as part of the candidate host's normal, sustained operating condition — a reading obtained only by transiently freeing memory for the measurement itself does not represent genuine readiness
REPEATED_MEASUREMENT: MemAvailable must be measured more than once, at different times, each reading individually timestamped and individually recorded — a single best-case sample does not satisfy this category
GOVERNING_THRESHOLD: the larger of the two verified artifact sizes (Section 7) determines minimum host eligibility; a host that could satisfy the gate only for the smaller model does not qualify, since both roles must be diagnosable in the same campaign
```

This category directly answers the accepted Planning Review's Path 1 futility finding: identity resolution alone (Sections 6–7 above) does not resolve this category. A host must independently, and across repeated measurement, demonstrate `MemAvailable` headroom the currently governed host's own repeated readings (~1.45–2.41 GiB against a ~5.28 GiB total) did not show.

## 9. Requirement category 5 — disk

### 9.1 Definitions

```text
GiB = 1,073,741,824 bytes (2^30) — fixed, used for every byte computation in this category
E = worst-case additional evidence-root bytes consumed by one complete 392-call campaign, computed per Section 9.2
R = worst-case additional dedicated-runtime-root bytes consumed by one complete 392-call campaign, computed per Section 9.3
```

### 9.2 Evidence budget (E)

```text
ESTIMATOR: E must be produced by an offline estimator that invokes the actual corrected implementation's own serializers — the same code paths that write schedule.jsonl, intent.jsonl, dispatch.jsonl, transport.jsonl, terminal.jsonl, control-events.jsonl, resource-readings.jsonl, campaign-definition.json, campaign-identity.json, advancement-worksheet.json, sealed-report.json, and SHA256SUMS.txt, at commit `c5b90fa7c187e68d0d7fb2a9c165202932d482c7` (merged as `c419db3e570bef101c200637fb6668837d77b148` via PR #26) or a later accepted commit — against worst-case synthetic inputs. A hand-derived arithmetic guess, or a figure sampled from a prior, possibly-smaller or pre-correction campaign, does not satisfy this requirement.
TRANSPORT_RECORD_COMPOSITION: for every one of the 392 `transport.jsonl` records, E's estimator must independently size — and never omit or double-count — each of the following fields `recordTransport` (`tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:929-964`) actually persists:
  - the seven universal chained-ledger envelope fields shared by every governed ledger record (schemaVersion, campaignId, trialId, sequence, priorRecordHash, timestamp, and the trailing recordHash — `FamilyFChainedLedger.append`, `tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:721-744`);
  - exchangeSequence, responseStatus, responseByteCount, requestByteCount (bare integer/long literals — no Base64 expansion, no JSON-escaping expansion);
  - responseCaptured (a bare boolean literal — no expansion);
  - startedAt, completedAt (ISO-8601 `Instant.toString()` text — passed through `familyFJsonEscape`, but their fixed character set contains nothing it rewrites, so escaping contributes no expansion; sized at their fixed maximum representable length);
  - requestSha256, responseSha256 (fixed 64-character lowercase-hex text — escaped but, like the timestamps, contain no character `familyFJsonEscape` rewrites, so escaping contributes no expansion);
  - requestBodyBase64: `Base64.getEncoder().encodeToString(requestBytes)`, sized as `ceil(maxRequestBytes / 3) * 4` (standard Base64 expansion); Base64's own alphabet (`[A-Za-z0-9+/=]`) contains no character `familyFJsonEscape` rewrites, so this field receives Base64 expansion only, never JSON-escaping expansion on top of it; `maxRequestBytes` is bounded per MAX_REQUEST_BOUND below;
  - responseBodyBase64: identical treatment, `maxResponseBytes` bounded per MAX_RESPONSE_BOUND below, Base64 expansion only;
  - responseHeadersJson: `familyFEncodeHeaders` (`tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:569-579`) first Base64-encodes every header name and every header value independently (Base64 expansion applies to each token), assembling them into a `{"key":["value",...],...}` structure; that complete structure is then itself stored as this payload field's String value and passed through `familyFJsonEscape` a second time, so every literal `"` character the inner structure contains (two per encoded header name, two per encoded header value) is individually escaped to `\"` at the outer layer — this is the one field in the record that receives BOTH Base64 expansion (on its embedded name/value tokens) AND JSON-escaping expansion (on its own structural quote characters); both must be applied, in that order, exactly once each; sized at the maximum expected header count and maximum expected value length/count the dedicated provider's response may carry;
  - forwardingOutcome: for a sealed, fully-successful 392-call campaign this field is always the fixed literal `"FORWARDED"` at every one of the 392 records (a forwarding-failure value only ever appears on an ambiguous, permanently-halted campaign, which by definition never reaches sealing and is therefore out of scope for a completed campaign's evidence budget).
  No field above may be counted under more than one of these bullets, and the "all chained ledgers" sizing in MANDATORY_COMPONENTS below must not re-add `transport.jsonl`'s per-record size separately from this itemization — this itemization *is* `transport.jsonl`'s per-record composition, not an addition to it.
MAX_REQUEST_BOUND: unlike the response, the maximum request-body size for any of the 392 calls is not model-dependent and needs no external immutable-evidence bound — it is fully determined by the frozen corpus and frozen context profiles (Unit 3-BF Scope Lock Sections 9–10) via the unmodified production `DefaultReasoningPromptBuilder`/`defaultOllamaRequestBody` path, and must be computed directly (the maximum, across all 46 fixture/profile combinations plus the frozen warm-up input, of the exact production-formatted request body length), never assumed, sampled, or bounded by an external provider limit.
MANDATORY_COMPONENTS: E must include, at minimum:
  - transport.jsonl, sized per-record exactly per TRANSPORT_RECORD_COMPOSITION above, across all 392 records, at each record's maximum final on-disk allocation (see ALLOCATION_SIZING below)
  - every other chained ledger in its complete, worst-case-length form (schedule, intent, dispatch, terminal, control-events, resource-readings), sized at each ledger's maximum final on-disk allocation (see ALLOCATION_SIZING below), not a raw serialized-byte count
  - resource-reading and control-event records at their maximum per-record size and maximum expected count
  - subject and control sealed reporting (sealed-report.json) at its maximum size (46 cells x 2 roles, full detail, no truncation)
  - campaign-definition.json and campaign-identity.json
  - the advancement worksheet (advancement-worksheet.json)
  - the manifest (SHA256SUMS.txt) and the terminal marker file
  - every other mandatory campaign artifact the accepted implementation's durable campaign layout (Plan Section 17) requires, including any this list does not separately name
WRITE_SEMANTICS: the accepted, corrected implementation performs no atomic-write-via-rename for any campaign artifact. Every singleton JSON/manifest/marker file (campaign-definition.json, campaign-identity.json, advancement-worksheet.json, sealed-report.json, SHA256SUMS.txt, and the sealed/halted terminal marker) is written through forced in-place `CREATE + TRUNCATE_EXISTING + WRITE + SYNC` (`familyFWriteForced`, `tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:533-543`). Every JSONL ledger (schedule, intent, dispatch, transport, terminal, control-events, resource-readings) is written through the single shared `FamilyFChainedLedger.append` primitive (`tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:721-744`), which itself calls forced in-place append (`familyFAppendForced`, `tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:545-554`). Neither writer creates a temporary file, calls `Files.move`, or performs a rename. In-place forced writes must never be described as atomic replacement.
TEMP_DUPLICATE_BYTES: for the exact corrected implementation commit (`c5b90fa7c187e68d0d7fb2a9c165202932d482c7`, merged as `c419db3e570bef101c200637fb6668837d77b148` via PR #26), TEMP_DUPLICATE_BYTES=0 — independently re-audited fresh against this commit, not inherited from any prior audit of a different commit. A direct search for `Files.move`, `StandardCopyOption`, a `.tmp` suffix literal, or `rename` across both Family F implementation files at this commit returns zero matches: no mandatory campaign artifact transiently coexists with a duplicate of itself during a write, because no writer in the corrected implementation uses a temporary-file/rename pattern. The raw-capture correction added new payload fields (Section 9.2's TRANSPORT_RECORD_COMPOSITION above) and new decode/offline-recovery functions, but changed neither `familyFWriteForced` nor `familyFAppendForced` nor `FamilyFChainedLedger.append`, and introduced no new writer function of any kind.
TEMP_DUPLICATE_EVIDENCE: TEMP_DUPLICATE_BYTES=0 must never be assumed; it is satisfied only by an assessment that records all of:
  - the exact accepted implementation commit hash the source audit was performed against;
  - the exact source file and line range of every writer function the durable campaign layout uses (at minimum `familyFWriteForced` and `familyFAppendForced`, and every call site that writes a mandatory artifact);
  - an explicit audit statement that no `Files.move`, no rename, and no temporary-file path (e.g. a `.tmp` suffix or equivalent) exists in any writer the durable campaign layout uses;
  - the estimator's own version identifier and the SHA-256 of the audit's raw output, preserved alongside the ESTIMATOR_PROVENANCE evidence Section 9.5 already requires
  An assertion of TEMP_DUPLICATE_BYTES=0 without this evidence does not satisfy this category.
ALLOCATION_SIZING: E must size every mandatory campaign artifact and every append-only ledger at its maximum final on-disk allocation, not a raw serialized-byte estimate, computed as:
  allocated(size, blockSize) = ceil(size / blockSize) x blockSize
  using the candidate evidence filesystem's own recorded allocation unit (blockSize) as read at the resolved evidence-root device (Section 9.5's ACTUAL_PATH_MEASUREMENT/PATH_DEVICE_IDENTITY). Every allocation computation must use overflow-safe integer arithmetic, exactly as Section 9.5's OVERFLOW_SAFETY already requires generally.
RESERVE_SCOPE: the existing 2 GiB evidence-root reserve (Section 9.4) remains the sole governed allowance for filesystem overhead, metadata, journaling, and other operational headroom beyond E's own artifact-allocation sizing. No additional, unquantified disk-consumption component may be invented beyond ALLOCATION_SIZING and this 2 GiB reserve; any newly identified consumption source must be sized and named explicitly, exactly as this correction did, never assumed away or bundled into an unstated margin.
IMPLEMENTATION_DRIFT: TEMP_DUPLICATE_BYTES=0 is valid only for the exact accepted implementation commit it was audited against — currently `c5b90fa7c187e68d0d7fb2a9c165202932d482c7` (merged as `c419db3e570bef101c200637fb6668837d77b148`), superseding the earlier audit performed against `73f8bdc`/`1c699f7`, which the raw transport capture correction has itself superseded. If the accepted implementation commit changes again, or a future implementation change introduces a temporary-file or rename write path for any mandatory campaign artifact, or changes `recordTransport`'s persisted field list (Section 9.2's TRANSPORT_RECORD_COMPOSITION), TEMP_DUPLICATE_BYTES=0 and TRANSPORT_RECORD_COMPOSITION are both immediately invalid; E must then be re-derived from the new commit's actual serializers and, if applicable, include the maximum transient coexistence allocation for the affected artifact(s), sized at that artifact's own worst-case size, not an average; and no candidate-host assessment may proceed under the stale figures until the estimator and this section's evidence are updated and independently reviewed.
MAX_RESPONSE_BOUND: the per-call maximum response-body byte bound used to size the 392 worst-case response records must be established from immutable provider/model evidence (e.g. a documented, provider-published maximum output-token/byte limit for the exact frozen model artifact) or from an already-governed, enforceable transport bound this programme's own implementation actively enforces (e.g. a hard response-size cap the transparent capture proxy or HTTP client rejects above) — never from an observed average, a typical-case sample, or an unenforced assumption
UNCOMPUTABLE_RESPONSE_BOUND: if no such immutable-evidence or already-governed enforceable bound exists for a candidate host's exact provider/model pairing, the per-call response-body size is unbounded, E is uncomputable, and the candidate host is `NOT READY` for this category — a plausible-looking estimate is not a substitute for a defensible upper bound
```

### 9.3 Runtime budget (R)

```text
SCOPE: R covers only the dedicated provider runtime's maximum additional writable growth during one complete 392-call campaign — scratch space, request/response buffering internal to the provider, temporary decompression or residency-state spill, log growth, or any other writable state the dedicated (non-production) provider instance creates or grows beyond what it already occupies at rest
EXCLUSION: R excludes already-consumed, immutable model-artifact storage (the frozen subject/control artifacts themselves, Section 7) — that storage is a precondition of the candidate host existing at all, not additional consumption the campaign itself causes
EVIDENCE_REQUIREMENT: R must be supported by provider/runtime evidence — the provider's own documented resource behavior, or a candidate-host-specific, read-only-observed writable-growth ceiling for the exact dedicated launch procedure (Section 6's DEDICATED_LAUNCH_PROCEDURE) — never an assumption that the provider is stateless beyond model weights
UNBOUNDED_RUNTIME: if the dedicated provider's additional writable growth cannot be bounded by such evidence, R is uncomputable, and the candidate host is `NOT READY` for this category
```

### 9.4 Pass rules

```text
SEPARATE_FILESYSTEMS: where the evidence root and the dedicated-runtime root resolve to different underlying filesystems or devices, each is evaluated independently:
  - evidence-root filesystem usable bytes >= E + 2 GiB
  - runtime-root filesystem usable bytes >= R + 2 GiB
SHARED_FILESYSTEM: where the evidence root and the dedicated-runtime root resolve to the same underlying filesystem or device (as they currently do on the presently governed host — a single shared volume), the two roots' requirements are combined, not independently checked against the same pool:
  - shared-filesystem usable bytes >= E + R + 4 GiB
RESERVE_COMPOSITION: the 4 GiB shared-filesystem reserve is exactly the sum of the two independently governed 2 GiB per-root reserves (the SEPARATE_FILESYSTEMS case above, and the Plan's own >= 2 GiB per-root minimum); neither root's 2 GiB reserve may be treated as satisfying, covering, or standing in for the other root's reserve, on a shared or a separate filesystem
```

### 9.5 Measurement and integrity requirements

```text
ACTUAL_PATH_MEASUREMENT: usable space must be measured at the actual, resolved evidence-root parent path/device and the actual, resolved dedicated-runtime-root parent path/device — never at a proxy, sibling, or assumed-equivalent path
PATH_DEVICE_IDENTITY: the resolved filesystem/device identity underlying each root (matching Section 5's FILESYSTEM_IDENTITIES) must be recorded alongside the usable-space reading, so a reviewer can independently confirm whether SEPARATE_FILESYSTEMS or SHARED_FILESYSTEM applies
ESTIMATOR_PROVENANCE: the estimator's exact inputs, its own version identifier, the exact repository commit it was run against, its raw output, and the SHA-256 of that output must all be preserved as evidence — an unreproducible or undated E/R figure does not satisfy this category
OVERFLOW_SAFETY: every byte computation (E, R, the GiB constant, and every pass-rule sum and comparison) must use overflow-safe integer arithmetic — a computation that could silently wrap, truncate, or lose precision at the byte counts this category deals with does not satisfy this category
NO_PROXY_MEASUREMENT: a measurement at `/`, at a parent directory that does not actually correspond to the configured roots, or at a different volume than the one the roots will actually resolve to does not satisfy this category
DYNAMIC_GATES_INDEPENDENT: this category's E/R admission formula is a pre-campaign, static sizing check; it does not replace, weaken, or substitute for the Plan's own dynamic disk checks (each root's fresh >= 2 GiB usable-space check before campaign creation and before every block, Plan Section 16) — both remain independently binding, and passing this category's static formula is not evidence that the Plan's dynamic per-block checks will also pass at execution time
NO_MANUFACTURED_PASS: this category may never be satisfied by deletion or cleanup of unrelated data to inflate a momentary usable-space reading, by pointing at an alternate volume the campaign will not actually use, by reducing the evidence captured (raw body truncation, dropped ledgers, or a reduced 392-call schedule), or by substituting a different, smaller model artifact — any of these manufactures the appearance of a pass without the campaign's actual disk requirement having changed
```

### 9.6 Determinism

```text
IDENTICAL_INPUT_IDENTICAL_VERDICT: given the same recorded E, R, usable-space readings, and filesystem/device-identity determination, two independent assessors applying Section 9.4's pass rules must reach the identical PASS/FAIL result — Section 9.4 involves no threshold left to judgment, no unquantified "sufficient headroom" language, and no aggregate-only verdict that could obscure which of the two rules applied
```

This category directly extends the accepted Readiness Review's own Item 13 finding — the currently governed host's evidence and runtime roots both resolve to a single, 88%-full, 3.5 GiB-available shared volume — into a binding, general, and fully determinate requirement, so that a future candidate host cannot be judged adequate by measuring a different path than the one it would actually use, cannot be judged adequate merely because two individually-passing numbers happen to draw from the same underlying pool, and cannot be judged differently by two assessors working from the same raw numbers.

## 10. Requirement category 6 — operational isolation

```text
DEDICATED_ENDPOINT: a loopback-only endpoint distinct from any production model endpoint on the candidate host or elsewhere
DEDICATED_PROCESS_IDENTITY: a distinct daemon/process/container identity from any production model-serving process, verified by PID, start time, and listening endpoint, not merely by name or configuration label
NO_COEXISTING_PRODUCTION_WORKLOAD: the candidate diagnostic runtime must not itself be, host, or share resources with a production Parker process or a production model-serving workload, unless a future, separate governance amendment expressly proves safe isolation under the specific coexistence proposed — the default, absent such an amendment, is that no production Parker or production model workload may run on the candidate diagnostic runtime at all
NO_SHARED_STATE: no shared production ports, evidence roots, memory roots, audit logs, identity stores, or model state between the candidate diagnostic runtime and any production system, on the candidate host or elsewhere
NO_INDETERMINACY_SOURCES: no unrelated, ungoverned workload on the candidate host may run concurrently with the campaign in a way that makes any resource-gate reading indeterminate or unrepresentative of what the campaign itself would actually experience during a real 392-call run
```

## 11. Requirement category 7 — network and security

```text
LOOPBACK_ONLY_INFERENCE: all inference traffic between the production reasoning chain, the transparent capture proxy, and the dedicated provider must be loopback-only (127.0.0.1 or localhost) — no candidate host may be proposed on the basis that a non-loopback endpoint is "close enough" or firewalled
NO_CREDENTIAL_BEARING_EVIDENCE: exactly as the Plan already requires, no secret, token, or credential may appear in any captured evidence; a candidate host whose dedicated endpoint requires credential-bearing configuration does not satisfy this category and requires fresh governance before use, not a workaround
NO_EXTERNAL_MODEL_ENDPOINT: the dedicated provider must serve inference entirely from local, pre-existing artifacts; no candidate host may reach a remote or cloud-hosted model endpoint for any call in the 392-call schedule
CONTROLLED_OUTBOUND_BEHAVIOR: the candidate host's dedicated diagnostic runtime must not have unconstrained outbound network access that could exfiltrate captured evidence or fetch unexpected remote content during the campaign; outbound behavior must be describable and bounded before any future Explicit Execution Approval, not left implicit
EVIDENCE_DIRECTORY_EXPECTATIONS: the proposed evidence root's filesystem permissions must support the implementation's append-only, hash-chained ledger design — write access sufficient for the campaign process to append and force durability, without ambient write access for arbitrary other processes on the host that could alter a sealed or in-progress ledger file outside the governed append path
```

## 12. Requirement category 8 — governance standing

No candidate host, however evidenced, authorizes execution by itself. The governance sequence a candidate host's evidence feeds into is:

```text
1. this Scope Lock is accepted and merged (freezes the requirements above);
2. a candidate-host assessment is performed against a specific, named host, following the read-only protocol in Section 13 below, producing an exact evidence package and pass/fail matrix against every category in Sections 5-11;
3. that candidate-host assessment receives its own Independent Constitutional Review;
4. if and only if the assessment is accepted as satisfying every category, a renewed Readiness Review is performed against that specific host (not the currently governed host), independently re-verifying every item the currently governed host's Readiness Review already covers, plus every category this Scope Lock adds;
5. that renewed Readiness Review receives its own Independent Constitutional Review;
6. only after all of the above are accepted may a separate Explicit Execution Approval be drafted, fixing every Plan Section 19/25 value against the specific approved host; and
7. only after that Explicit Execution Approval is issued may any launch, model load, or endpoint contact occur.
```

No step in this sequence authorizes its successor automatically. A favorable candidate-host assessment is not a Readiness Review. A favorable Readiness Review is not an Explicit Execution Approval. This mirrors, and does not shorten, the Unit 3-BF Scope Lock's own Section 23 governance-gate sequence.

## 13. Requirement category 9 — candidate-host assessment protocol

A future candidate-host assessment, once a specific host is proposed under separate authority, must:

```text
READ_ONLY_AND_OFFLINE_FIRST: consist entirely of read-only, offline evidence collection against Sections 5-11 above — no model acquisition, no daemon start, no model load, no inference, and no campaign creation may occur during the assessment itself, exactly as the currently governed host's Readiness Review was itself performed read-only and offline
EVIDENCE_PACKAGE: produce an exact, itemized evidence package — every value in Sections 5-11, its collection method, and its timestamp — sufficient for an independent reviewer to reproduce every reading without trusting the assessment's own restatement
PASS_FAIL_MATRIX: produce an explicit pass/fail (or NOT ESTABLISHED) determination for each of the nine requirement categories individually, not a single aggregate verdict that could conceal a failing category behind passing ones
DISK_DETERMINISM_CHECK: for requirement category 5 (disk, Section 9) specifically, the assessment must record E, R, both usable-space readings, and both filesystem/device-identity determinations such that an independent reviewer, given only those recorded numbers and Section 9.4's pass rules, can derive the identical PASS/FAIL result by arithmetic alone, without rerunning the estimator or exercising judgment about what counts as "sufficient" — two independent assessors presented with the same raw disk numbers must always reach the same verdict
TEMP_DUPLICATE_ZERO_CHECK: for requirement category 5 specifically, the assessment must additionally record the exact accepted implementation commit hash, the source file/line evidence for every writer function the durable campaign layout uses, and the explicit no-temp-file/no-rename audit statement Section 9.2's TEMP_DUPLICATE_EVIDENCE requires — an assessment that asserts TEMP_DUPLICATE_BYTES=0 without this evidence package, or that reuses a prior assessment's zero against a different implementation commit without re-auditing, fails requirement category 5 regardless of how favorable its other disk numbers are
DEFAULT_ON_GAPS: any missing identity value, any inaccessible artifact-identity evidence, or any other evidence gap in any category yields `NOT READY` for that category and for the host overall — exactly as the currently governed host's own Readiness Review treated Items 7-12. A gap may never be closed by: substituting a smaller model, a different quantization, a reduced corpus, reduced repetitions, or reduced profiles; by escalating privilege beyond what this Scope Lock's categories already permit as read-only; by asserting a value without a disclosed, reproducible collection method; or by treating administrator assertion as equivalent to independently verified evidence
```

## 14. Relationship to Explicit Execution Approval

Nothing in this Scope Lock, and nothing a future candidate-host assessment or renewed Readiness Review could produce on its own, is an Explicit Execution Approval. That remains a separate, later governance act under Section 12 step 6 above, fixing the complete Plan Section 19/25 value set against the one specific host that has, by then, passed every category in Sections 5–11, its own Independent Constitutional Review, and a renewed Readiness Review with its own Independent Constitutional Review. A host satisfying every requirement in this document is necessary, but never sufficient, for execution.

## 15. Stop conditions

Any future action taken under a purported reading of this Scope Lock stops immediately if it:

- treats acceptance of this Scope Lock as authorization to identify, select, provision, or contact any host;
- treats a candidate-host assessment as a substitute for a renewed Readiness Review, or a renewed Readiness Review as a substitute for a separate Explicit Execution Approval;
- closes any Section 5–11 evidence gap by model substitution, quantization change, reduced corpus, reduced repetitions, reduced profiles, privilege escalation beyond what a category expressly permits as read-only, or unverified administrator assertion;
- treats Section 6's provider-identity requirement as satisfied by container/image metadata alone, without the resolved executable's own SHA-256;
- treats Section 9's disk requirement as satisfied by measuring a proxy path, by treating two roots on a shared volume as independently sized, by an E or R figure lacking a defensible upper-bound evidence source, or by deletion, cleanup, an alternate volume, reduced evidence capture, response truncation, a reduced schedule, or model substitution used to manufacture a passing usable-space reading;
- treats Section 8's memory requirement as satisfied by a single reading, or by a reading obtained through swap enablement, cache manipulation, process stopping, or ungoverned-workload removal performed to produce a favorable measurement;
- permits a production Parker or production model workload to coexist on a candidate diagnostic runtime without the express amendment Section 10 requires;
- acquires, pulls, downloads, copies, or converts a model artifact under the authority of this document alone; or
- begins Knowledge Discoverability Attempt 3 under any pretext.

## 16. Decision register

| Question | Decision | Status |
|---|---|---|
| Does this Scope Lock select, name, or provision a host? | No. | RESOLVED |
| Does this Scope Lock weaken any Unit 3-BF Scope Lock or Plan requirement? | No. | RESOLVED |
| Does resolving provider/artifact identity alone satisfy the memory requirement? | No — Section 8 is independent and must be separately, repeatedly demonstrated. | RESOLVED |
| May a candidate host share resources with production Parker or a production model workload? | No, absent a future, separate, express amendment proving safe isolation. | RESOLVED |
| May any category gap be closed by model substitution or scope reduction? | No. | RESOLVED |
| Does a passing candidate-host assessment authorize execution? | No — a renewed Readiness Review and a separate Explicit Execution Approval remain required. | RESOLVED |
| Is model acquisition authorized by this document? | No. | RESOLVED |
| Is "remain blocked" still available if no host ever qualifies? | Yes — nothing in this document forecloses it. | RESOLVED |
| Is Knowledge Discoverability Attempt 3 authorized? | No. | RESOLVED |

## 17. Explicit non-claims

This Scope Lock does not claim that:

- any host currently exists, is known, or is available that satisfies any category in Sections 5–11;
- the currently governed host is permanently disqualified — only that its own Readiness Review found it currently `NOT READY`, for reasons this document generalizes into transferable requirements;
- satisfying every category in this document guarantees a future Readiness Review will find a candidate host `READY` — only that it is a necessary precondition;
- satisfying this document's requirements qualifies, selects, or authorizes deployment of either named model;
- any organizational, budgetary, or infrastructural authority to obtain a qualifying host exists or is granted by this document; or
- acceptance of this document brings the diagnostic any closer to execution than one governance step, exactly as Section 12 states.

## 18. Exit criteria

This Scope Lock is ready to freeze only when:

1. every cited repository path resolves and every reproduced fact matches its cited source exactly;
2. every requirement category (Sections 5–11) is stated as a binding, evidence-based criterion, not an aspiration;
3. no candidate host is selected, named, identified, inspected, or provisioned anywhere in the document;
4. every frozen invariant in Section 4 is restated as unaffected by host choice;
5. the governance sequence in Section 12 does not shorten or bypass any existing Unit 3-BF gate;
6. the candidate-host assessment protocol (Section 13) is read-only/offline-only and defaults to `NOT READY` on any gap;
7. `git diff --check` passes;
8. exactly this one document is changed; and
9. an Independent Constitutional Review returns `ACCEPTED` or `ACCEPTED WITH NON-BLOCKING QUALIFICATIONS`.

## 19. Final authority statement

```text
PROGRAMME_STATUS=ACTIVE
FAMILY_F_STATUS=INCLUDED_FOR_PRE_QUALIFICATION_DIAGNOSTIC_SCOPING_ONLY (unchanged)
IMPLEMENTATION_STATUS=ACCEPTED (unchanged)
READINESS=NOT READY (unchanged; this document does not reopen the currently governed host's Readiness Review)
HOST_SELECTED_OR_PROVISIONED=NO
HOST_ASSESSMENT_AUTHORIZED=NO — Section 13 defines its future protocol only
MODEL_ACQUISITION_AUTHORIZED=NO
MODEL_RUN_AUTHORIZED=NO
CAMPAIGN_AUTHORIZED=NO
EXPLICIT_EXECUTION_APPROVAL_AUTHORIZED=NO
KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3_AUTHORIZED=NO
NEXT_LAWFUL_ACTION=INDEPENDENT_CONSTITUTIONAL_REVIEW_OF_THIS_SCOPE_LOCK; IF ACCEPTED AND MERGED, THE NEXT LAWFUL ACTION BECOMES EITHER (A) A CANDIDATE-HOST ASSESSMENT AGAINST A SPECIFIC, SEPARATELY PROPOSED HOST UNDER SECTION 13'S PROTOCOL, OR (B) CONTINUED DEFERRAL UNDER PATH 4 OF THE ACCEPTED PLANNING REVIEW IF NO CANDIDATE HOST IS PROPOSED
```
