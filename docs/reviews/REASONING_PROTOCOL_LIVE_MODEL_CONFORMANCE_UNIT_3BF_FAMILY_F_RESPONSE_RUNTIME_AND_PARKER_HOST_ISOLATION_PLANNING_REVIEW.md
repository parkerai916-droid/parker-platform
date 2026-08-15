**Status:** Unit 3-BF Family F Response/Request-Size, Dedicated-Runtime-Growth, and Parker-Host Isolation Planning Review — **PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW.** Governance-only, drafted against merged baseline `3038bf7c7b0d9dfdf53d51b41fe94c4cd5585423`, performing exactly the `RECOMMENDED_NEXT_ACTION` the accepted Parker Candidate Host Assessment fixed: "A FAMILY F CAPTURE-PROXY RESPONSE/REQUEST-SIZE AND DEDICATED-RUNTIME-GROWTH BOUNDING GOVERNANCE DOCUMENT." This review evaluates, on paper only, how a bounded request/response capture regime and a bounded dedicated-runtime-growth regime could be established, and separately evaluates three physical-host paths for a future candidate diagnostic — existing Parker VM 102 under an express same-VM coexistence amendment, a new dedicated diagnostic VM on the same Proxmox node, and remaining blocked. It performs no command against any host, container, or model endpoint beyond reading this repository's own working tree and git history. It selects no final numeric byte bound, authorizes no implementation, provisioning, acquisition, model contact, execution, Explicit Execution Approval, or Knowledge Discoverability Attempt 3. `READINESS=NOT READY` is unchanged.

# Reasoning Protocol Live-Model Conformance — Unit 3-BF Family F Response/Request-Size, Dedicated-Runtime-Growth, and Parker-Host Isolation Planning Review

## 1. Baseline and controlling authority

```text
BASELINE=3038bf7c7b0d9dfdf53d51b41fe94c4cd5585423
$ git status --porcelain --branch  -> ## main...origin/main (clean)
$ git rev-parse HEAD                -> 3038bf7c7b0d9dfdf53d51b41fe94c4cd5585423
$ git rev-parse origin/main          -> 3038bf7c7b0d9dfdf53d51b41fe94c4cd5585423
```

Branch `governance/reasoning-protocol-family-f-bounding-planning` was created from this exact commit.

Controlling authority, read completely and fresh for this task, directly from the working tree at this commit:

1. `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_PARKER_CANDIDATE_HOST_ASSESSMENT_REVIEW.md` (`READINESS=NOT READY`; `RECOMMENDED_NEXT_ACTION` quoted above) and its accepted Independent Constitutional Review (`VERDICT=ACCEPTED`, `P0=P1=P2=P3=0`) — the assessment this document exists to answer;
2. `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_ALTERNATIVE_DIAGNOSTIC_HOST_REQUIREMENTS_SCOPE_LOCK.md` and its accepted Independent Constitutional Review — the frozen nine-category requirement set (Sections 5–13), in particular Section 9's `E`/`R` disk-consumption formula, Section 10's `NO_COEXISTING_PRODUCTION_WORKLOAD` default rule, and Section 12's governance sequence;
3. `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_EXECUTION_PLAN.md` and its accepted Independent Constitutional Review — Section 14's production-isolation rule (never stop, signal, reconfigure, or route traffic through a protected process) and Section 16's resource-gate formulas;
4. `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_COMPLETION_REVIEW.md` and `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`;
5. `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_READINESS_REVIEW.md` and its accepted Independent Constitutional Review;
6. `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_READINESS_BLOCKER_RESOLUTION_PLANNING_REVIEW.md` and its accepted Independent Constitutional Review — the Planning Review whose Path 2 recommendation produced the Host Requirements Scope Lock (item 2 above);
7. `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_RAW_TRANSPORT_CAPTURE_DEFECT_CONFIRMATION_REVIEW.md` and its accepted Independent Constitutional Review, and `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_RAW_TRANSPORT_CAPTURE_CORRECTION_COMPLETION_REVIEW.md` and its accepted Independent Constitutional Review — the raw-transport defect-confirmation and correction chain confirming `FamilyFCampaignLedger.recordTransport` (`tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:929-964`) now durably persists complete raw request/response bytes and full multi-valued headers, commit `c5b90fa7c187e68d0d7fb2a9c165202932d482c7`, merged as `c419db3e570bef101c200637fb6668837d77b148` via PR #26;
8. `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_EXPERIMENTAL_RECLASSIFICATION_AND_QUALIFICATION_BOUNDARY_SCOPE_LOCK.md` and its accepted Independent Constitutional Review — the Unit 3-BF Scope Lock fixing subject/control identity, the frozen 23-fixture corpus, the two frozen context profiles, the four-repetition/392-call schedule, and the absolute, subject-only advancement gate (Section 4's frozen invariants, unaffected by host choice);
9. `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` (1,073 lines), `tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt` (3,763 lines), and `tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt` (613 lines), read directly from the working tree at this commit, not from any document's quotation of them;
10. `docs/architecture/parker-constitution.md` — the platform's controlling constitutional document; nothing in this review is read to conflict with it (Section 15 below).

This review does not reopen, diminish, or restate a verdict on any of the above. `IMPLEMENTATION_STATUS=ACCEPTED` and `READINESS=NOT READY` both stand exactly as the cited documents left them.

## 2. Purpose and boundary

This document performs exactly the Parker Candidate Host Assessment's `RECOMMENDED_NEXT_ACTION`. It evaluates, on paper only:

- **Part A** — how to close the confirmed `E_STATUS=UNCOMPUTABLE` finding (unbounded capture-proxy request/response reads);
- **Part B** — how to close the confirmed `R_STATUS=UNCOMPUTABLE` finding (no bounded dedicated-runtime-growth evidence);
- **Part C** — the Category 6 `NO_COEXISTING_PRODUCTION_WORKLOAD` gap, evaluated across three physical-host paths.

It does not:

- select, compute, or adopt a final numeric byte bound for `MAX_REQUEST_BOUND`, `MAX_RESPONSE_BOUND`, a header-count/aggregate-header-byte limit, or a dedicated-runtime writable-growth ceiling — Sections 6, 7, and 10 below explain exactly why primary evidence is currently insufficient for each;
- implement, modify, or propose modifying any test, source, or Gradle file;
- provision, create, resize, or configure any VM, container, or storage volume;
- acquire, pull, download, copy, or convert any model artifact;
- launch, start, stop, signal, or reconfigure any process, container, or daemon;
- contact any model endpoint or provider API;
- authorize an Explicit Execution Approval or any part of the governance sequence beyond this document's own Independent Constitutional Review;
- authorize Knowledge Discoverability Attempt 3.

## 3. Method

Every finding below is derived from direct reading of the working tree at the stated baseline (Section 1, item 9) and from the accepted governance chain (Section 1, items 1–8), cross-cited by exact file and line number where the underlying claim is a source-code fact. Where this review evaluates a mechanism (a bounded reader, a cgroup limit, a tmpfs mount) it describes the mechanism's shape and what evidence would be needed to adopt it; it does not execute, simulate, or benchmark any mechanism, consistent with this task's "evaluate on paper only; perform nothing" instruction.

```text
COMMANDS_RUN_THIS_SESSION=git status/rev-parse/branch/checkout (repository-local only); grep and wc -l against tracked files; Read of the governance documents and Kotlin files named in Section 1
HOST_CONTACT=NONE
MODEL_CONTACT=NONE
PROXMOX_CONTACT=NONE
```

---

# Part A — Bounded request and response capture

## 4. Every currently unbounded body-read path

Independently re-grepped both Family F implementation files and the production inference client for every `readBytes()`/`BodyHandlers` call site:

```text
$ grep -n "readBytes\|BodyHandlers" tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt src/runtime/ModelInferenceClient.kt
```

Two occurrences sit on the **live campaign path** — the only path any of the 392 governed calls actually traverses:

```text
LIVE_PATH_UNBOUNDED_READ_1: ReasoningProtocolFamilyFDiagnosticTest.kt:493
  val requestBody = exchange.requestBody.readBytes()
  -- FamilyFCaptureProxy.ProxyHandler.handle: reads the inbound request from the production
     LocalHttpModelInferenceClient to end-of-stream, no length argument, no configured maximum.
LIVE_PATH_UNBOUNDED_READ_2: ReasoningProtocolFamilyFDiagnosticTest.kt:516
  val upstreamResponse = httpClient.send(upstreamRequestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray())
  -- same handler: reads the upstream response from the dedicated model daemon into a single
     in-memory byte array, no configurable or default maximum size.
```

Both calls occur before `recordTransport`'s write-time hash re-verification (`ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:929-938`) and before any byte-count check anywhere in either file — independently reconfirmed by a direct grep for any bound-naming constant:

```text
$ grep -nE "MAX_RESPONSE|MAX_REQUEST|maxResponseBytes|maxRequestBytes|responseByteLimit|MAX_BODY|MAX_HEADER" tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt
  (no output, exit 1)
```

This reproduces exactly the finding the accepted Parker Candidate Host Assessment already made (its Section 7.2); nothing has changed since that assessment, because no implementation change has occurred since the raw-transport correction and this review authorizes none.

Every other `readBytes()`/`BodyHandlers.ofByteArray()` occurrence in `ReasoningProtocolFamilyFDiagnosticTest.kt` (lines 900, 924, 956, 976, 990, 1013) and `ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt` (lines 3096, 3125, 3183, 3233, 3571) belongs to a **fake upstream server or a test-local HTTP client** constructed inside an offline `@Test` method — none of these executes during a live campaign, and none needs its own bound, since a future bounded implementation would replace the *proxy's own* two call sites above; a fake upstream server used only to prove the proxy's own bounding behavior would need a large-payload variant, but that is an implementation-tier test obligation, not a currently-unbounded live path.

```text
RELATED_OUT-OF-SCOPE_PATH: src/runtime/ModelInferenceClient.kt:61
  val future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
  -- LocalHttpModelInferenceClient.infer(), the unmodified production inference client, also
     reads its own inbound response (from whatever endpoint it is pointed at -- for Family F,
     the capture proxy's own forwarded response) unbounded. This is production `src/` code; the
     accepted Plan's Section 4 ("No file under src/ may change") and Section 5's exact three-file
     boundary forbid touching it under this governance family. It is noted, not corrected, here:
     provided the proxy itself never forwards a response larger than an enforced MAX_RESPONSE_BOUND
     (Section 6 below requires reject-before-forward, never truncate-and-forward), the production
     client inherits that same bound transitively, because it can never receive more bytes than the
     proxy chose to release. If a future bounding correction instead let the proxy forward an
     oversize body while merely flagging it, this transitive bound would not hold, and the
     production client's own unbounded read would remain a live, unaddressed gap. This is one of
     the reasons Section 6 below requires fail-closed rejection at the proxy, not truncation.
```

```text
DERIVED_LEDGER-SIDE_READS: FamilyFCampaignLedger and FamilyFChainedLedger's Files.readAllLines/readAllBytes/readString
  calls (e.g. ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:791, 977, 1019, 1043, 1147, 1745)
  read whole ledger files into memory. These are not independent unbounded paths -- their upper
  bound is a direct function of E once E is computable (Section 9 below), since every byte any of
  these calls can ever read was itself written by a bounded recordTransport/recordTerminal call.
  They are recorded here as a dependency, not a separate finding requiring their own bound.
```

## 5. Bounded-streaming approaches evaluated

Two mechanisms would close both `LIVE_PATH_UNBOUNDED_READ_1` and `LIVE_PATH_UNBOUNDED_READ_2`, each rejecting excess bytes **before** unbounded allocation or durable release, never after:

```text
MECHANISM_1 -- bounded manual read loop (inbound request, exchange.requestBody):
  HttpExchange.getRequestBody() returns a plain InputStream. A fixed-size-buffer read loop
  (e.g. 64 KiB chunks) that accumulates a running total and throws the moment the total would
  exceed MAX_REQUEST_BOUND -- before appending the chunk that would cross the limit -- replaces
  readBytes() directly. No JDK API beyond InputStream.read(buffer, off, len) is required. The
  maximum transient allocation is bounded by (MAX_REQUEST_BOUND + one chunk), never by the
  attacker- or defect-controlled body size.

MECHANISM_2 -- bounded BodySubscriber (upstream response, HttpResponse.BodyHandlers.ofByteArray()):
  The JDK's java.net.http package exposes no built-in bounded byte-array body handler. A custom
  HttpResponse.BodyHandler<byte[]> backed by a custom Flow.Subscriber that counts bytes across
  onNext(List<ByteBuffer>) calls and calls subscription.cancel() plus completes the downstream
  CompletableFuture exceptionally the moment the running total would exceed MAX_RESPONSE_BOUND
  closes this gap without discarding HttpClient's own connection-pooling/HTTP-version handling.
  An equivalent alternative is HttpResponse.BodyHandlers.ofInputStream() paired with the same
  bounded read loop as Mechanism 1, applied to the response stream instead of the request stream.
  Either is a genuine "reject excess bytes before unbounded allocation" mechanism; a future
  bounding implementation must pick one and disclose which.

COMMON_REQUIREMENT: both mechanisms must reject, not truncate. A truncate-and-forward design
  would (a) durably record a response that is not what the model actually produced, corrupting
  the evidence Plan Section 12 requires to be a byte-for-byte capture, and (b) as Section 4 above
  notes, would defeat the transitive bound the unmodified production client currently relies on.
```

## 6. Required oversize semantics

```text
FAIL_CLOSED: the bounded reader/subscriber aborts the read the instant the running byte count
  would exceed the bound; it never completes a buffer larger than the bound, and it never
  silently truncates and continues.
DURABLE_EVIDENCE_WITHOUT_REINTRODUCING_THE_GAP: an oversize rejection must still leave a durable,
  reviewable trace -- but recording the actual oversize bytes durably would reintroduce exactly
  the unbounded-allocation/unbounded-evidence problem this correction exists to close. A future
  bounding document must specify that only the observed byte count at the moment of rejection
  (itself bounded by MAX_*_BOUND + one chunk) and the fact of rejection are durably recorded --
  never the oversize content itself, not even truncated.
NO_RETRY: an oversize rejection is a capture failure under Plan Section 18/Scope Lock Section 22's
  existing "raw request/response capture failure" measurement-invalidating category. It must
  route through the identical FamilyFArtifactIntegrityException -> ledger.halt(...) path every
  other integrity failure already uses in FamilyFOrchestrationDriver.run()
  (ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:1634-1640) -- never a distinct,
  newly-invented recovery branch.
NO_PARTIAL_CLASSIFICATION: because the bytes were never fully captured, no PrimaryClassification
  value (A-I) currently represents "rejected before capture." A future bounding document must
  either add one, or must state explicitly that an oversize rejection is never routed through
  familyFOfflineClassify/classify at all -- it is a hard halt, not a scored or reportable outcome.
DETERMINISTIC_CAMPAIGN_HALT: identical to every other Section 18 recovery-rule failure -- no
  discretion, no partial seal, no reissue of the trial.
```

## 7. Governance evidence required to select exact byte ceilings

The Host Requirements Scope Lock (Section 9.2) already fixes the admissible evidence sources; this review restates them against the corrected implementation's actual field composition (Section 9 below) and states, for each of the two bounds, exactly what is missing.

### 7.1 Request ceiling — computable now, not computed here

Per Scope Lock Section 9.2's own `MAX_REQUEST_BOUND` rule, the request ceiling is "fully determined by the frozen corpus and frozen context profiles ... via the unmodified production `DefaultReasoningPromptBuilder`/`defaultOllamaRequestBody` path" and "must be computed directly ... never assumed, sampled, or bounded by an external provider limit." This review independently confirms the inputs that determine it are all already frozen and already present in the working tree:

```text
FIXED_INPUT_1: FamilyFCorpus.fixtures -- exactly 23 frozen fixture texts (ReasoningProtocolFamilyFDiagnosticTest.kt:90-273)
FIXED_INPUT_2: FamilyFCorpus.profiles -- exactly 2 frozen profiles, minimal-production-context and mixed-full-production-like (:269-272)
FIXED_INPUT_3: FamilyFCorpus.warmupFixture -- one frozen warm-up input (:260-266)
FIXED_INPUT_4: two frozen model-name strings, FAMILY_F_SUBJECT_MODEL_NAME / FAMILY_F_CONTROL_MODEL_NAME (:62-63)
FIXED_FORMATTER: DefaultReasoningPromptBuilder().buildPrompt(turn, reasoningContext) -> defaultOllamaRequestBody(prompt, modelName)
  -- the unmodified production path, already independently confirmed (Implementation Completion
     Review Section 9; this review's own reading of FamilyFOfflineRecovery.recoverTerminalPayload,
     ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:1282-1292) to be the exact path every
     live request would traverse
```

The maximum, across all 46 fixture/profile combinations plus the frozen warm-up input, of the exact production-formatted UTF-8 request-body byte length, for each of the two frozen model names, is therefore a **deterministic, reproducible quantity** — not an estimate, not a sample, and not dependent on anything not already frozen and merged. This review does not compute that number. Computing it means writing and running an offline estimator against the real `DefaultReasoningPromptBuilder`/`defaultOllamaRequestBody` — an implementation-tier act this "evaluate on paper only; perform nothing" review does not perform, and one the task's own "do not select final numeric bounds unless primary evidence is sufficient" instruction does not ask this review to perform by hand (hand-computation of 46-plus-1 formatted-prompt byte lengths would itself be exactly the kind of unverifiable, error-prone "intuition" the task instructs this review to avoid). A future, separately governed and separately reviewed estimator must execute it, with its own version identifier, the exact commit it ran against, its raw output, and the SHA-256 of that output preserved as evidence — mirroring the `ESTIMATOR_PROVENANCE` discipline the Scope Lock's Section 9.5 already requires for `E` generally.

```text
REQUEST_CEILING_STATUS=COMPUTABLE BY A FUTURE, SEPARATELY GOVERNED ESTIMATOR -- METHODOLOGY FIXED HERE, NUMBER NOT COMPUTED HERE
```

### 7.2 Response ceiling — not computable on current evidence

Per Scope Lock Section 9.2, `MAX_RESPONSE_BOUND` must come from "immutable provider/model evidence (e.g. a documented, provider-published maximum output-token/byte limit for the exact frozen model artifact)" or "an already-governed, enforceable transport bound this programme's own implementation actively enforces." Response text is model-generated, not fixed governance text, so no offline estimator can compute it the way Section 7.1's request bound can. This review independently re-checked both admissible sources:

```text
SOURCE_1 -- PROVIDER-PUBLISHED MAXIMUM OUTPUT: the accepted Parker Candidate Host Assessment (Section 7.2)
  already searched this repository's governed evidence base and found
  PROVIDER_PUBLISHED_MAXIMUM_OUTPUT_BYTES=NOT FOUND. This review re-grepped the governance chain
  read for this task (Section 1) and found no later document supplying it. Nothing has changed.

SOURCE_2 -- ALREADY-GOVERNED ENFORCEABLE TRANSPORT BOUND: the only way this source could be satisfied
  without provider documentation is for a future implementation to itself constrain the dedicated
  provider's own generation length at request time (e.g. an Ollama num_predict-equivalent
  parameter fixed by the future Explicit Execution Approval), converted to a worst-case byte count
  via a provider/tokenizer-published bytes-per-token ratio. This is a legitimate, evidence-based
  path in principle -- it does not depend on discovering a natural maximum, it creates an enforced
  one -- but it requires two facts neither this review nor any document in the governance chain
  has established: (a) confirmation, for the exact provider version in use, that the chosen
  generation-length parameter is a hard, enforced cap rather than a soft target (PROVIDER_VERSION
  itself is recorded as NOT ESTABLISHED by the accepted Candidate Host Assessment, Section 4 --
  "would require either the Ollama HTTP API or the ollama CLI, both prohibited by this task's
  boundaries"); and (b) a provider/tokenizer-published worst-case bytes-per-token ratio for the
  exact frozen model artifacts, which this review did not find cited anywhere in the read
  governance base.
```

Neither source currently supplies a defensible number. Per this task's explicit instruction ("Do not choose an arbitrary numeric cap without traceable evidence" and "Do not select final numeric bounds unless primary evidence is sufficient"), this review does not propose one — not a round number, not a "conservative" guess, and not an average of typical Ollama responses observed elsewhere in this repository's own evidence base (which would be exactly the "observed average" or "unenforced assumption" Scope Lock Section 9.2 already forbids).

```text
RESPONSE_CEILING_STATUS=UNCOMPUTABLE ON CURRENT EVIDENCE -- E_STATUS REMAINS UNCOMPUTABLE, EXACTLY AS THE ACCEPTED CANDIDATE HOST ASSESSMENT FOUND. THE TWO LAWFUL PATHS TO CLOSE IT (SOURCE_1, SOURCE_2 ABOVE) ARE STATED; NEITHER IS SATISFIED BY THIS DOCUMENT.
```

## 8. Header-count and aggregate-header-byte bounds

Independently re-read `familyFEncodeHeaders` (`ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:569-579`) and the proxy's own header-forwarding loops (`ReasoningProtocolFamilyFDiagnosticTest.kt:505-514, 543-547`): both the inbound request's headers and the upstream response's headers are iterated and forwarded/encoded with no count limit and no aggregate-byte limit. A response carrying an extreme header count or extreme individual header values (a `Set-Cookie`-style multi-valued header pattern, already exercised adversarially by the accepted raw-transport correction's own test suite for *correctness*, not for *size*) would still consume unbounded memory in `responseHeadersJson` even once the two body bounds above are enforced — exactly the risk this task's own instruction names.

```text
JVM/LIBRARY-LEVEL_IMPLICIT_BOUND: com.sun.net.httpserver and java.net.http both impose their own
  internal limits on header parsing for wire-protocol reasons (e.g. java.net.http's
  jdk.httpclient.maxheadersize system property; the built-in HttpServer's own request-line/header
  buffer limits). These exist today, unexamined, undocumented in this governance chain, and
  unverified against the exact JDK version this repository targets.
REQUIRED_EVIDENCE: a future bounding document must (a) read-only confirm the exact JDK version's
  actual default header-size/header-count limits (a disclosed, reproducible check, not an
  assumption); and (b) decide whether those defaults are sufficient by themselves or whether an
  explicit MAX_HEADER_COUNT / MAX_AGGREGATE_HEADER_BYTES check should be added inside
  familyFEncodeHeaders's own construction path, given that header evidence sizing already has its
  own dedicated contribution to E's TRANSPORT_RECORD_COMPOSITION formula (Section 9 below).
HEADER_BOUND_STATUS=REQUIREMENT IDENTIFIED, NOT SATISFIED -- NO NUMERIC HEADER BOUND SELECTED HERE
```

## 9. How an accepted bound would make `E` computable

Once `MAX_REQUEST_BOUND`, `MAX_RESPONSE_BOUND`, and a header-count/aggregate-byte bound are each established by a future, separately governed act, `E`'s `transport.jsonl` component becomes mechanically computable from the Scope Lock's own already-frozen `TRANSPORT_RECORD_COMPOSITION` formula (Section 9.2), independently re-verified against the corrected `recordTransport`'s actual thirteen persisted payload fields (`ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:929-964`) plus the seven-field universal envelope (`FamilyFChainedLedger.append`, `:721-744`):

```text
- seven universal envelope fields (schemaVersion, campaignId, trialId, sequence, priorRecordHash, timestamp, recordHash) -- bare/short literals, fixed contribution
- exchangeSequence, responseStatus, responseByteCount, requestByteCount -- bare integer/long literals, no expansion
- responseCaptured -- bare boolean literal, no expansion
- startedAt, completedAt -- fixed-length ISO-8601 text, escaped but contributing zero expansion (Raw Transport Capture Correction ICR Section 6, independently re-derived and empirically confirmed in that review)
- requestSha256, responseSha256 -- fixed 64-character hex text, escaped but contributing zero expansion
- requestBodyBase64 = ceil(MAX_REQUEST_BOUND / 3) * 4  -- standard Base64 expansion, Base64 alphabet contributes zero JSON-escaping expansion on top
- responseBodyBase64 = ceil(MAX_RESPONSE_BOUND / 3) * 4  -- identical treatment, pending Section 7.2's still-missing bound
- responseHeadersJson -- Base64 expansion on every encoded header name/value token PLUS JSON-escaping expansion on the outer structure's own literal quote characters (two per encoded name, two per encoded value) -- the one field receiving both expansion types, sized at the header-count/aggregate-byte maximum Section 8 above still requires
- forwardingOutcome -- fixed "FORWARDED" literal for a sealed, fully-successful campaign
```

Each of the 392 transport records' maximum on-disk allocation is then `allocated(recordSize, blockSize) = ceil(recordSize / blockSize) * blockSize`, using the evidence-root filesystem's own confirmed allocation unit at the specific candidate host eventually chosen (the currently governed host's own allocation unit, 4096 bytes, was independently confirmed by the Parker Candidate Host Assessment Section 7.1 — but that figure is host-specific and must be re-confirmed, not assumed transferable, for whichever host Part C below eventually yields). Summed across all 392 records, plus the Scope Lock's own already-frozen `MANDATORY_COMPONENTS` (every other chained ledger at its own maximum allocation, `sealed-report.json` at its full 46×2-role size, `campaign-definition.json`, `campaign-identity.json`, `advancement-worksheet.json`, the manifest, and the terminal marker), plus the governed 2 GiB reserve, this is exactly `E` — a single, deterministic, overflow-safe-arithmetic sum with no remaining unquantified term, once and only once both `MAX_REQUEST_BOUND` and `MAX_RESPONSE_BOUND` exist.

```text
E_COMPUTABILITY=STRUCTURALLY RESTORED BY AN ACCEPTED REQUEST BOUND AND AN ACCEPTED RESPONSE BOUND -- NEITHER BOUND IS ADOPTED BY THIS DOCUMENT; E REMAINS UNCOMPUTABLE UNTIL BOTH EXIST, PARTICULARLY THE RESPONSE BOUND (SECTION 7.2)
```

## 10. No arbitrary numeric cap

Consistent with Sections 7.1, 7.2, and this task's own explicit instruction, this document selects **no** numeric value for `MAX_REQUEST_BOUND`, `MAX_RESPONSE_BOUND`, any header-count/aggregate-byte bound, or any dedicated-runtime writable-growth ceiling (Part B). Every number this review could have proposed would have been either (a) computable now but not yet computed by a disclosed, reproducible estimator (the request bound), or (b) not computable at all on current evidence (the response bound, the header bound, and `R` in Part B). Proposing a placeholder figure for any of these would itself be the "plausible-looking estimate" the Scope Lock's own `UNCOMPUTABLE_RESPONSE_BOUND` rule already forbids treating as a substitute for a defensible upper bound.

---

# Part B — Bounded dedicated-runtime growth

## 11. Every possible mutable location for the dedicated provider

```text
WRITABLE_CONTAINER_LAYER: if the dedicated provider runs as a container (the production Ollama
  instance already does, per the Parker Candidate Host Assessment Section 4 -- image
  ollama/ollama:latest), its own writable layer (anything not part of the immutable image, e.g.
  /tmp inside the container, any scratch path the ollama binary itself writes to at runtime)
LOGS: stdout/stderr redirection, or an internal log file the provider writes if configured to do so
TEMPORARY_FILES: any temporary decompression, request/response buffering internal to the provider
  process, or KV-cache spill-to-disk behavior under memory pressure -- none of this is documented
  anywhere in the governance chain read for this task (Section 12 below)
CACHES: any local inference cache distinct from the immutable model-weight blob store
GENERATED_METADATA: manifest/index updates the provider daemon may perform at its own runtime state
  path, distinct from the read-only-verified blob store
RUNTIME_STATE: PID files, socket files, any lock files the daemon creates for its own process
  lifecycle management
UNLOAD_RESIDUE: files or state left behind after an unload command that are not cleaned up as part
  of the unload itself -- a genuine, easily overlooked growth source distinct from steady-state
  writable growth
CRASH_OR_RESTART_RESIDUE: core dumps or partial-write artifacts if the dedicated process crashes
  mid-campaign -- a source this review's reading of the governance chain found no prior document
  had named
```

## 12. Enforceable mechanisms evaluated

```text
READ-ONLY_CONTAINER_ROOT (Docker --read-only): forces every write the dedicated container attempts,
  outside an explicitly mounted writable path, to fail. Converts "every unknown mutable location in
  Section 11" into "exactly the one or few locations this governance document chose to make
  writable." Verifiable, read-only, post-launch: `docker inspect --format
  '{{.HostConfig.ReadonlyRootfs}}'` on the dedicated (never the production) container. This is the
  strongest, most directly verifiable mechanism of the set and should be the default posture a
  future bounding document adopts.

EXPLICITLY_SIZED_WRITABLE_VOLUME: a single, named, explicitly sized Docker volume or bind mount at
  the one writable path (if any) the read-only-root container still needs. Docker's own
  `--storage-opt size=` enforcement is storage-driver-dependent (e.g. requires overlay2 with a
  pquota-capable backing filesystem) and was not confirmed available for either the currently
  governed host or any future candidate host by any document this review read -- must be verified,
  not assumed, against whichever host Part C below eventually yields.

BOUNDED_TMPFS: an explicitly sized tmpfs mount (`--tmpfs /path:size=Xm`) for any writable scratch
  space. tmpfs is RAM-backed, not disk-backed -- a future bounding document that adopts tmpfs for
  any part of R must feed that consumption into the existing FamilyFMemoryGate accounting (Plan
  Section 16 / ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:168-229), not treat it as a
  disk-only concern; failing to do so would silently reintroduce an unbounded-memory risk under a
  disk-bounding label.

FILESYSTEM/PROJECT_QUOTA: XFS project quotas or ext4 usrquota/grpquota/prjquota mount options,
  kernel-enforced, verifiable read-only via xfs_quota/repquota. The currently governed host's own
  filesystem was independently confirmed ext4 (Parker Candidate Host Assessment Section 3); whether
  its mount carries the quota options required to enforce this was not established by any document
  read for this task and must not be assumed present.

CONTAINER_STORAGE_LIMIT ("where genuinely supported and verifiable"): the least portable of the
  set -- requires a specific Docker storage driver/backing-filesystem combination this review found
  no confirmation of for any host in the governance chain.

FAIL-CLOSED_PRE-LAUNCH_AND_CONTINUOUS_CHECKS: before any dedicated launch, a future bounding
  implementation must read-only verify the chosen mechanism is actually active (e.g. confirm
  ReadonlyRootfs=true, confirm the tmpfs/quota is mounted at the expected size) -- and continuous,
  per-block dynamic disk checks (the existing FamilyFDiskSpaceGate, already measuring the dedicated
  runtime root's own usable space before every one of the eight blocks) must remain independently
  binding alongside any static R computation, exactly as Scope Lock Section 9.5's
  DYNAMIC_GATES_INDEPENDENT rule already requires for disk generally -- a static R bound is never a
  substitute for the existing dynamic per-block gate, and vice versa.
```

## 13. Immutable provider image and verified model artifacts kept distinct from mutable runtime growth

Per Scope Lock Section 9.3's own `EXCLUSION` rule, `R` covers only the dedicated provider's *additional* writable growth beyond what it already occupies at rest. This review keeps three categories explicitly distinct, consistent with that rule and with the Parker Candidate Host Assessment's own Category 2/3 findings:

```text
CATEGORY_1_IMMUTABLE_MODEL_ARTIFACT: the frozen subject/control model weights themselves (blob store)
  -- already governed by Scope Lock Section 7 (frozen artifacts), never counted toward R.
CATEGORY_2_IMMUTABLE_PROVIDER_IMAGE: the container image's own layers, read-only once pulled --
  already governed by Scope Lock Section 6 (provider identity), never counted toward R.
CATEGORY_3_MUTABLE_RUNTIME_GROWTH: only genuinely new writable growth during the campaign (Section 11
  above) -- this, and only this, is what R measures.
```

No sentence in this review conflates any of the three, and no future bounding document may either.

## 14. Evidence needed to compute a genuine `R`

Per Scope Lock Section 9.3's `EVIDENCE_REQUIREMENT`, `R` must be supported by "the provider's own documented resource behavior, or a candidate-host-specific, read-only-observed writable-growth ceiling for the exact dedicated launch procedure (Section 6's `DEDICATED_LAUNCH_PROCEDURE`)."

```text
SOURCE_1 -- DOCUMENTED_PROVIDER_RESOURCE_BEHAVIOR: no document in the governance chain read for this
  task cites, quotes, or links any Ollama-published specification of what it writes at runtime
  beyond model weights (logs, cache, KV-cache spill behavior). This remains an open evidence gap.
SOURCE_2 -- READ-ONLY-OBSERVED WRITABLE-GROWTH CEILING FOR THE EXACT DEDICATED LAUNCH PROCEDURE: this
  requires an actual dedicated (non-production) instance to have been launched, under the exact
  procedure a future Explicit Execution Approval would use, with its writable-directory growth
  passively measured across a representative load. The accepted Parker Candidate Host Assessment,
  Section 7.2, establishes that no dedicated non-production Ollama instance has been launched on
  Parker VM 102 under governance and therefore no candidate-host-specific, read-only-observed
  writable-growth ceiling exists for that dedicated launch procedure. This supports
  R_STATUS=UNCOMPUTABLE for the assessed Parker host. This Planning Review found no separately
  governed evidence establishing a reusable runtime-growth ceiling for another candidate host. This
  document -- paper-only, authorizing no launch -- cannot create that evidence for any host. A
  future, separately governed step would be required, itself gated behind Part C's own unresolved
  host question below.
```

```text
R_STATUS=UNCOMPUTABLE ON CURRENT EVIDENCE -- UNCHANGED FROM THE ACCEPTED PARKER CANDIDATE HOST ASSESSMENT'S OWN FINDING. NEITHER ADMISSIBLE EVIDENCE SOURCE EXISTS TODAY, AND THIS DOCUMENT DOES NOT AND CANNOT CREATE EITHER.
```

## 15. Rejecting observational guesswork

Per Scope Lock Section 9.5's own `NO_MANUFACTURED_PASS` rule, and per this task's explicit instruction, raw usable disk space — however large — is not treated here as resolving `R` (or `E`). The Parker Candidate Host Assessment's own ~33.97 GiB usable-margin finding on VM 102 (Section 7.1 of that document) is not cited anywhere in this review as evidence that `R`, once computed, would "obviously fit" — that finding is explicitly disclaimed by the Assessment's own Section 16 non-claims list, and this review reaffirms rather than relaxes that disclaimer.

---

# Part C — Parker physical-server isolation

## 16. Three paths evaluated

Per this task's instruction, the physical Proxmox server remains the only candidate location; the existing production VM is not assumed to be the only possible execution boundary on it. Three paths are evaluated separately below: (1) existing Parker VM 102 under an express same-VM coexistence amendment; (2) a new, dedicated diagnostic VM on the same Proxmox node, without creating it; (3) remaining blocked.

## 17. Path 1 — existing VM 102 with an express same-VM coexistence amendment

The Scope Lock's Section 10 default rule, quoted verbatim and independently re-confirmed against the working tree (`docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_ALTERNATIVE_DIAGNOSTIC_HOST_REQUIREMENTS_SCOPE_LOCK.md:209`):

> "`NO_COEXISTING_PRODUCTION_WORKLOAD`: the candidate diagnostic runtime must not itself be, host, or share resources with a production Parker process or a production model-serving workload, unless a future, separate governance amendment expressly proves safe isolation under the specific coexistence proposed — the default, absent such an amendment, is that no production Parker or production model workload may run on the candidate diagnostic runtime at all."

The Plan's own production-protection rule (Section 14, `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_EXECUTION_PLAN.md:237`): "The runner ... never signals, stops, restarts, reconfigures, or routes traffic through them." This review does not evaluate, and does not silently authorize, stopping production Parker or production Ollama under any version of a same-VM proposal — that action is foreclosed by the Plan's own text and by the Constitution's owner-control principle (Section 15 below), not merely omitted from consideration.

### 17.1 Whether strict technical controls can prove isolation while production continues

```text
CPU: VM 102 carries 4 physical cores total (Parker Candidate Host Assessment Section 3, matching the
  supplied "4 cores" configuration exactly). Production Parker (container 736a6f38...) and
  production Ollama (container f795e46c...) are both already resident, principal workloads on this
  same VM (Assessment Section 8). Cgroup CPU shares/quotas (docker run --cpus=) can cap the
  dedicated diagnostic container's own worst-case CPU ceiling, but cannot eliminate contention: with
  only 4 physical cores shared between production's own unpredictable, traffic-driven demand and a
  392-call sequential inference campaign, the diagnostic's own measured latency becomes a function of
  whatever production happens to be doing at the same moment -- not a controlled variable. This is
  not a hypothetical: the task's own instruction requires this review to "account for production
  traffic arriving during the diagnostic; do not assume production remains idle." Under that
  requirement, CPU contention is a genuine, currently unremovable source of indeterminacy.
MEMORY: VM 102's guest RAM is fixed at 12,288 MiB (Assessment Section 2), independently confirmed as
  MemTotal 11,718,700 kB (Section 3). The Assessment's own three MemAvailable readings (10,695,912 -
  11,054,204 kB) were taken while production was already running its own -- not necessarily peak --
  baseline load. A dedicated diagnostic instance loading the subject artifact (~4.68 GB, requiring
  per FamilyFMemoryGate's own formula >= artifact size + 2 GiB MemAvailable, i.e. ~6.83 GB) is a
  second, concurrent claim on the same fixed ~11.7 GB pool production's own Ollama already draws
  from. Cgroup memory limits (docker run --memory=) can cap the diagnostic's own consumption, but
  proving the sum of (production's own worst-case memory need) + (the diagnostic's cgroup-limited
  share) + (OS/other overhead) never exceeds guest MemTotal minus a safety margin requires knowing
  production's own worst-case memory footprint -- a fact no document in the governance chain read
  for this task establishes. Without it, a memory-based safe-isolation showing cannot be completed,
  only asserted.
LOOPBACK-ONLY_NETWORKING: genuinely achievable and already specified by the Scope Lock (a distinct,
  loopback-only port from production's confirmed 0.0.0.0:11434/[::]:11434 binding) -- this dimension
  does not depend on production's own behavior and is tractable regardless of host choice.
DISTINCT_VOLUMES/ROOTS: genuinely achievable -- a separate evidence root and dedicated runtime root,
  distinct from production's own Docker volumes, is a filesystem-namespace decision unaffected by
  concurrent production load. Tractable.
PID/ENDPOINT_GUARDS: the already-implemented FamilyFProductionIsolationGuard
  (ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:334-363) performs genuine, read-only PID
  and endpoint-identity distinctness checks, and liveness checks, before and never during a
  campaign. This proves identity distinctness -- that the dedicated process is not production and
  is not signaled -- but it does not, and structurally cannot, guard against resource contention
  between two workloads that remain identity-distinct but resource-adjacent. Identity isolation and
  resource isolation are different properties; this mechanism only supplies the first.
RESOURCE_ADMISSION: cgroup CPU/memory limits are the correct mechanism family in principle, but on
  this host's specific numbers -- 4 total cores, ~11.7 GB fixed total RAM, an already-resident,
  traffic-receiving production workload whose own worst-case resource need is unestablished --
  admission control can bound worst-case ceilings; it cannot prove the absence of contention-induced
  measurement indeterminacy, which is precisely what the Scope Lock's own NO_INDETERMINACY_SOURCES
  rule (Section 10) already forbids: "no unrelated, ungoverned workload on the candidate host may
  run concurrently with the campaign in a way that makes any resource-gate reading indeterminate or
  unrepresentative of what the campaign itself would actually experience during a real 392-call run."
```

### 17.2 Conclusion on Path 1

```text
PATH_1_CONCLUSION=ON VM 102'S CURRENT, ESTABLISHED RESOURCE ENVELOPE, AN EXPRESS SAFE-ISOLATION
  AMENDMENT DOES NOT APPEAR ACHIEVABLE. CPU contention under concurrent, unpredictable production
  load is a genuine, currently unremovable indeterminacy source the Scope Lock's own rule already
  forbids; memory admission cannot be proven safe without an established (and currently absent)
  worst-case production memory footprint. This is a reasoned recommendation against pursuing this
  path on the evidence this review has read, not an assertion that same-VM coexistence is
  constitutionally impossible in the abstract on every conceivable host -- a sufficiently
  over-provisioned host with a bounded, verified production footprint could in principle satisfy
  Section 10's default rule. VM 102, as currently sized and as currently the sole host this
  programme's production workload runs on, is not that host on this evidence. This review does not
  draft, and does not recommend drafting, the express amendment Section 10 would require for VM 102.
PRODUCTION_SHUTDOWN=NOT EVALUATED, NOT AUTHORIZED, NOT IMPLIED BY ANY PART OF THIS SECTION
```

## 18. Path 2 — a new, dedicated diagnostic VM on the same Proxmox node

### 18.1 Whether it can constitutionally count as a genuinely separate execution boundary

Yes, structurally. A distinct VM (a new VMID) would carry its own kernel, its own process/cgroup namespace, its own virtual disk, and its own virtual NIC — a genuinely separate execution boundary from VM 102 in every sense the Scope Lock's Category 1 (host identity) and Category 6 (operational isolation) requirements care about. Critically, it would not run any production Parker or production Ollama container at all, since none would be provisioned on it — `NO_COEXISTING_PRODUCTION_WORKLOAD` would be satisfied **by construction**, not by an exception to Section 10's default rule. No express amendment of the kind Path 1 requires would even be needed, because the condition that rule guards against (a production workload resident on the candidate diagnostic runtime) would simply not obtain.

### 18.2 Required isolation dimensions and what further evidence each needs

```text
CPU: requires its own allocated vCPU count carved from the Proxmox host's own total physical
  capacity -- a figure no document in the governance chain read for this task independently
  confirms. The Parker Candidate Host Assessment explicitly could not establish it: "this session
  has no access to the Proxmox host" (Section 2). A dedicated VM's CPU allocation is a
  Proxmox-host-level decision this review cannot make or verify from inside any existing guest.
RAM: the task supplies observed Proxmox host figures -- MemTotal=16,233,988 kB (~15.48 GiB),
  MemAvailable=5,387,672 kB (~5.14 GiB) at the recorded time -- hypervisor-visible evidence the
  accepted Parker Candidate Host Assessment (Section 6.2) already independently corroborated as
  informational-only and explicitly declined to substitute into any guest-level gate; this review
  does the same. VM 102 is fixed at 12,288 MiB and must not be resized by this review (task
  instruction). Home Assistant VM 100 and Frigate CT 101 are stopped and must remain so (task
  instruction) -- but whether their own configured RAM allocations are statically reserved by
  Proxmox regardless of running state, or are genuinely reclaimable for a new VM, is a Proxmox
  configuration-policy fact this review cannot verify from inside any existing guest and does not
  assume either way. The observed ~5.14 GiB MemAvailable figure was measured at one past moment,
  while VM 102/100/101 already held whatever allocation Proxmox had committed to them at that
  moment; it is not treated here as a current or future-provisioning-time guarantee. Per the task's
  own instruction, the 8 GiB of unused host swap is explicitly not treated as a substitute for
  physical-memory headroom, consistent with this review's own refusal (Parts A/B) to accept
  unverified or swap-backed headroom as a pass anywhere else in this document.
DISK: a new VM needs its own virtual disk or dedicated logical volume, sized to at least E + R + the
  governed reserve -- which does not resolve Part A/B's own uncomputability; a dedicated VM only
  potentially gives a computed E+R a filesystem with no production sharing. It does not, by itself,
  make E or R computable.
NETWORK: loopback-only isolation within the new VM is trivially achievable; as a genuinely separate
  guest, even non-loopback isolation from production is structurally easier here than on VM 102,
  since there is no shared network namespace with production's own guest at all.
PROVIDER: the new VM would need its own dedicated Ollama instance under a to-be-defined
  DEDICATED_LAUNCH_PROCEDURE (Scope Lock Section 6) -- provider-executable identity and
  ACCESS_SUFFICIENCY would both need fresh, independent establishment on the new VM; nothing from
  VM 102's own Category 2 findings (Parker Candidate Host Assessment Section 4) transfers.
MODEL-VOLUME: the frozen qwen2.5-coder:7b and llama3.2:3b artifacts do not currently exist anywhere
  but VM 102's own production Ollama volume. Provisioning them onto a new VM is a model-acquisition
  act, already separately gated by the accepted Readiness Blocker Resolution Planning Review's own
  Path 3 discipline (a dedicated governance act naming exact source/registry, a pre-declared
  verifiable digest checked immediately after acquisition, an explicit no-qualification-credit
  statement, confirmation of no production-store contact, and its own Independent Constitutional
  Review before any pull) -- not performed, proposed in its concrete content, or shortened here.
EVIDENCE-ROOT: trivially achievable -- a new VM has its own disk entirely, genuinely separate from
  any production evidence root by construction.
FROZEN_SUBJECT/CONTROL_IDENTITIES_AND_CAMPAIGN_INVARIANTS: unaffected by host choice, exactly as
  Scope Lock Section 4 already fixes -- no invariant in that section is touched by evaluating this
  path.
```

### 18.3 Conclusion on Path 2

```text
PATH_2_CONCLUSION=CONSTITUTIONALLY VIABLE IN PRINCIPLE, AND THE MOST DEFENSIBLE OF THE THREE PATHS
  EVALUATED -- it satisfies NO_COEXISTING_PRODUCTION_WORKLOAD by construction rather than by
  exception, avoids the CPU/memory-contention indeterminacy Section 17 identifies for VM 102, and
  remains on the required physical Proxmox server. It requires, before it could be proposed to a
  future candidate-host assessment under Scope Lock Section 13: (a) genuine Proxmox-host-level free
  CPU/RAM/disk capacity, verified at the time of any actual provisioning decision, not inferred from
  the one past reading supplied to this task; (b) an explicit, separately obtained answer on whether
  VM 100/CT 101's reserved-but-stopped allocations count as available; (c) resolution of Part A's
  request/response bounds and Part B's runtime-growth bound, since the new VM's own disk sizing
  still depends on a computable E and R; (d) a freshly performed Category 1-9 candidate-host
  assessment against the new VM, exactly as Scope Lock Section 13 already requires for any candidate
  host, performed once the VM exists -- this document does not and cannot perform that assessment
  against a VM that does not yet exist; and (e) separately governed model-artifact acquisition under
  the Readiness Blocker Resolution Planning Review's own already-defined Path 3 discipline.
  This document does not create, provision, size, or configure the VM, and does not authorize its
  creation.
```

## 19. Path 3 — remain blocked

Preserved as a valid outcome, exactly as the accepted Readiness Blocker Resolution Planning Review's own Path 4 and every subsequent document in this chain has preserved it. If the evidence gaps Parts A, B, and Section 18.2 identify are not resolved by proportionate future governance action, remaining blocked is not a failure of this programme; it is the correct default under the Scope Lock's own fail-closed discipline.

```text
PATH_3_CONCLUSION=A VALID OUTCOME, NOT SUPERSEDED BY THIS REVIEW'S PREFERENCE FOR PATH 2 -- IF NO
  CANDIDATE HOST IS EVER PROPOSED OR FOUND SUITABLE, REMAINING BLOCKED REQUIRES NO FURTHER ACTION TO
  STAY VALID.
```

---

# Required outcome

## 20. Is a bounded-transport/runtime Scope Lock proportionate, and what must it freeze?

Yes. It directly, narrowly answers the two specific uncomputability findings (`E`, `R`) the accepted Parker Candidate Host Assessment identified as the controlling blockers on every candidate host this programme could propose — not merely VM 102 — and it is exactly the action that Assessment's own `RECOMMENDED_NEXT_ACTION` calls for. A future such Scope Lock must freeze, at minimum:

```text
1. MAX_REQUEST_BOUND methodology (Section 7.1): compute-from-frozen-corpus via the unmodified
   production formatter across all 46 fixture/profile cells plus the frozen warm-up input, executed
   by a future, separately governed and reviewed offline estimator with full ESTIMATOR_PROVENANCE --
   not a number adopted here.
2. MAX_RESPONSE_BOUND evidence requirement (Section 7.2): either a provider-published maximum output
   limit for the exact frozen artifacts, or a verified, provider-version-confirmed enforced
   generation-length parameter converted via a published tokenizer byte-per-token ratio -- an
   explicit statement that neither exists today, so no numeric response bound may be adopted until
   one does.
3. The bounded-streaming mechanism requirement (Section 5): reject-before-allocation, never
   truncate-and-forward, for both FamilyFCaptureProxy.ProxyHandler.handle call sites
   (ReasoningProtocolFamilyFDiagnosticTest.kt:493, 516).
4. Header-count/aggregate-header-byte bound requirement (Section 8), with the identical
   evidence-not-guess discipline.
5. Oversize-semantics requirements (Section 6): fail closed; durable evidence limited to the
   observed byte count at rejection and the fact of rejection, never the oversize bytes themselves;
   no retry; no partial classification; routed through the existing capture-failure halt path.
6. R evidence requirement (Section 14): documented provider behavior, or a read-only-observed
   writable-growth ceiling from an actual dedicated launch under the exact future launch procedure --
   an explicit statement that neither exists today and that this paper document cannot create either.
7. Explicit reaffirmation, unweakened, of Scope Lock Section 9.5's NO_MANUFACTURED_PASS rule: raw
   usable disk space, however large, never substitutes for computed E+R (Section 15).
8. Explicit non-relaxation of every Section 4 frozen invariant (subject/control identity, the
   23-fixture corpus, both profiles, four repetitions, the 392-call schedule, the absolute
   subject-only advancement gate, no ranking) -- a bounding document touches none of these.
```

## 21. Is same-VM coexistence governable, or should it be rejected?

On VM 102's current, established resource envelope, this review's own Section 17 analysis concludes an express safe-isolation amendment is **not currently achievable** — not because same-VM coexistence is abstractly unconstitutional, but because this specific host's 4-core, ~11.7 GB fixed-RAM envelope, already carrying production Parker and production Ollama as its principal, traffic-receiving workload, cannot support a defensible proof that concurrent production traffic would not introduce the exact resource-reading indeterminacy the Scope Lock's own Section 10 rule already forbids. This review recommends against pursuing a same-VM express amendment on VM 102 as currently sized. It does not purport to bind a future reviewer absolutely — a future document remains free to attempt the amendment if it can supply evidence this review did not have (e.g. a verified production worst-case memory ceiling, or additional host capacity) — but on the evidence read for this task, same-VM coexistence should not be the path a future bounding/host-track document pursues first.

## 22. Is a dedicated VM on the same Proxmox node constitutionally viable, and what further evidence would it require?

Yes, constitutionally viable in principle, per Section 18.3, and the more defensible of the two host-specific paths evaluated. It requires the five categories of further evidence Section 18.2 itemizes (genuine host-level free-capacity evidence; a decision on VM 100/CT 101's reserved allocations; resolution of Part A/B's bounds; a freshly performed candidate-host assessment against the new VM once it exists; and separately governed model-artifact acquisition) — none of which this document supplies, performs, or authorizes.

## 23. Recommended next governance action

Exactly one narrowly scoped next governance action is recommended, matching and instantiating the accepted Parker Candidate Host Assessment's own `RECOMMENDED_NEXT_ACTION`:

```text
RECOMMENDED_NEXT_ACTION=A FAMILY F CAPTURE-PROXY RESPONSE/REQUEST-SIZE AND DEDICATED-RUNTIME-GROWTH
  BOUNDING SCOPE LOCK
```

This future Scope Lock would need to freeze, at minimum, the eight items in Section 20 above (the `E`/`R` bounding requirements), and would separately need to address the Category 6 coexistence gap this review found by **explicitly handing it off to a distinct future governance track** rather than resolving it here — specifically: recommending that a future, separate Alternative-Host-track document (built on this review's Section 18 findings) evaluate a dedicated diagnostic VM as the primary path forward, while leaving the same-VM express-amendment path (Section 17) formally available but explicitly not recommended on current evidence. This review does not draft, authorize, or implement either the bounding Scope Lock or the host-track document; per this task's own boundary, it recommends the action's scope only.

## 24. Explicit non-claims

This review does not claim that:

- any numeric byte bound, of any kind, has been established, computed, or adopted by this document;
- a dedicated diagnostic VM, once evaluated further, will actually be found provisionable — only that it is the more constitutionally tractable of the two host-specific paths on the evidence read;
- VM 102 is permanently disqualified from ever hosting a Family F diagnostic — only that an express same-VM coexistence amendment is not achievable on its *currently established* resource envelope;
- the observed Proxmox host figures (`MemTotal=16,233,988 kB`, `MemAvailable=5,387,672 kB`) remain accurate at any future provisioning time — they are a single past reading, not a standing guarantee;
- Home Assistant VM 100's or Frigate CT 101's own reserved allocations are, or are not, reclaimable for a new VM — this review could not verify Proxmox's own allocation policy from inside any existing guest;
- resolving Parts A and B alone would also resolve Part C, or vice versa — the three are independent findings, exactly as the accepted Parker Candidate Host Assessment's own Section 16 already established for `E`/`R` versus Category 6;
- this review, or any future document it recommends, is or authorizes an Explicit Execution Approval, a Readiness Review, a candidate-host assessment, model acquisition, model contact, or Knowledge Discoverability Attempt 3; or
- any organizational, budgetary, or infrastructural authority to create a VM, resize a VM, acquire a model, or grant a coexistence amendment exists or is granted by this document.

## 25. Stop conditions

Any future action taken under a purported reading of this review stops immediately if it:

- treats this document as authorizing implementation of a bounded reader/subscriber, a header-count check, an oversize-semantics change, a cgroup/tmpfs/quota configuration, VM creation, VM resize, model acquisition, or any other executable change;
- adopts a numeric `MAX_REQUEST_BOUND`, `MAX_RESPONSE_BOUND`, header bound, or `R` ceiling on the basis of this document alone, without the disclosed estimator/evidence Sections 7, 8, and 14 each require;
- treats Section 7.1's "computable now" finding for the request bound as equivalent to that bound having actually been computed;
- treats a truncate-and-forward design as satisfying Section 6's fail-closed requirement;
- treats Section 18's dedicated-VM path as already evaluated to completion, or proceeds to create such a VM without the five evidence categories Section 18.2 names;
- treats Section 17's recommendation against a VM 102 same-VM amendment as a permanent constitutional prohibition rather than a reasoned recommendation on current evidence;
- silently authorizes stopping, restarting, or reconfiguring production Parker or production Ollama under any reading of Part C;
- treats this review as a candidate-host assessment, a renewed Readiness Review, or an Explicit Execution Approval;
- begins Knowledge Discoverability Attempt 3 under any pretext.

## 26. Decision register

| Question | Determination | Status |
|---|---|---|
| Is a bounded-transport/dedicated-runtime-growth Scope Lock proportionate to the confirmed E/R blockers? | Yes. | RESOLVED |
| Does this review select a numeric request, response, header, or R bound? | No. | RESOLVED |
| Is the request ceiling computable in principle from already-frozen inputs? | Yes — by a future, separately governed estimator; not computed here. | RESOLVED |
| Is the response ceiling computable on current evidence? | No — neither admissible evidence source (Scope Lock Section 9.2) currently exists. | RESOLVED |
| Is R computable on current evidence? | No — neither admissible evidence source (Scope Lock Section 9.3) currently exists; no dedicated instance has been launched on Parker VM 102 under governance, and no separately governed evidence establishes a reusable ceiling for any other candidate host. | RESOLVED |
| Does raw usable disk space substitute for uncomputable E or R? | No. | RESOLVED |
| Is same-VM coexistence on VM 102 governable on current evidence? | No — recommended against, not permanently foreclosed. | RESOLVED |
| Is a dedicated diagnostic VM on the same Proxmox node constitutionally viable? | Yes, in principle — further evidence required before any candidate-host assessment. | RESOLVED |
| Does this review authorize VM creation, resize, or storage allocation? | No. | RESOLVED |
| Does this review authorize model acquisition, model contact, or execution? | No. | RESOLVED |
| Is "remain blocked" preserved as a valid outcome? | Yes. | RESOLVED |
| Does this review create its own Independent Constitutional Review? | No — explicitly out of scope for this task. | RESOLVED |
| What is the single recommended next governance action? | A Family F Capture-Proxy Response/Request-Size and Dedicated-Runtime-Growth Bounding Scope Lock, separately handing off the Category 6 gap to a future host-track document favoring Path 2. | RESOLVED |

## 27. Complete citation and cross-reference audit

Every internal cross-reference in this document (e.g. "Section 7.2", "Section 9.5's `NO_MANUFACTURED_PASS` rule", "Section 18.2") was checked against this document's own actual section numbering and resolves correctly. Every external citation was checked against the source document's own text, read directly from the working tree at baseline `3038bf7c7b0d9dfdf53d51b41fe94c4cd5585423`, not from any intermediate restatement:

```text
Scope Lock Section 9.2 (TRANSPORT_RECORD_COMPOSITION, MAX_REQUEST_BOUND, MAX_RESPONSE_BOUND,
  UNCOMPUTABLE_RESPONSE_BOUND) -- quoted/paraphrased in Sections 7.1, 7.2, 9, 10: verified against
  docs/architecture/...ALTERNATIVE_DIAGNOSTIC_HOST_REQUIREMENTS_SCOPE_LOCK.md lines 122-162
Scope Lock Section 9.3 (SCOPE, EXCLUSION, EVIDENCE_REQUIREMENT, UNBOUNDED_RUNTIME) -- Sections 13-14:
  verified against the same file, lines 164-171
Scope Lock Section 9.5 (NO_MANUFACTURED_PASS, and the measurement/integrity requirements generally) --
  Section 15: verified, lines 184-195. Section 9.4 (lines 173-183) is the distinct Pass Rules
  subsection (SEPARATE_FILESYSTEMS/SHARED_FILESYSTEM/RESERVE_COMPOSITION); NO_MANUFACTURED_PASS is
  not part of it and this document no longer attributes it there.
Scope Lock Section 10 (NO_COEXISTING_PRODUCTION_WORKLOAD, NO_INDETERMINACY_SOURCES) -- Section 17:
  verified verbatim, lines 204-212 -- independently reproduced byte-for-byte
Scope Lock Section 4 (frozen invariants) -- Sections 18.2, 20, 24: verified, lines 40-56
Scope Lock Section 6 (provider identity and access), lines 76-88; Section 7 (frozen model artifacts),
  lines 89-99; Section 13 (candidate-host assessment protocol), lines 240-252 -- each independently
  re-read with numbered lines and cited separately; Section 18.2: verified against each of these
  three distinct ranges. No combined range is used; none includes Section 5 (host identity, lines
  58-75) or omits Section 13.
Plan Section 14 (dedicated endpoint and production isolation) -- Section 17: verified against
  docs/implementation/...DIAGNOSTIC_IMPLEMENTATION_EXECUTION_PLAN.md lines 221-239
Plan Section 16 (resource gates) -- Section 12: verified, lines 256-270
Parker Candidate Host Assessment Sections 2-8, 15-16 -- cited throughout Part C: verified against
  docs/reviews/...PARKER_CANDIDATE_HOST_ASSESSMENT_REVIEW.md, matching line ranges cited in text
Readiness Blocker Resolution Planning Review Path 3 (Section 8, model-acquisition governance
  discipline) and Path 4 (Section 9, remain-blocked/defer) -- Sections 18.2, 19: verified against
  docs/reviews/...READINESS_BLOCKER_RESOLUTION_PLANNING_REVIEW.md, matching the acquisition-discipline
  and remain-blocked text cited. This source is not cited in Section 14; Section 14's R-evidence
  finding is attributed solely to the Parker Candidate Host Assessment, Section 7.2, scoped to Parker
  VM 102 (see Section 14 above)
Raw Transport Capture Correction Independent Constitutional Review, Section 6 (Base64/JSON-escaping
  expansion rules, independently re-derived and empirically tested -- matching the inline citation at
  Section 9 above) and Completion Review Section 3.1/3.3 (the same topic, un-numbered as a standalone
  top-level section in that document) -- Section 9: verified against both
  docs/reviews/...RAW_TRANSPORT_CAPTURE_CORRECTION_COMPLETION_REVIEW.md and its ICR
Source line citations (ReasoningProtocolFamilyFDiagnosticTest.kt:493, 516, 505-514, 543-547, 900,
  924, 956, 976, 990, 1013; ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:334-363, 569-579,
  721-744, 929-964, 1282-1292, 168-229; src/runtime/ModelInferenceClient.kt:61) -- every one
  independently reproduced by direct grep/Read against the working tree at this session's baseline,
  Section 4 and 5 above
```

```text
CITATION_AUDIT=NO STALE, MISQUOTED, OR UNRESOLVED CITATION FOUND
```

## 28. Validation

```text
$ git diff --check
  (no output -- clean; single new, untracked file, no existing file modified)
$ git status --porcelain --branch
  ## governance/reasoning-protocol-family-f-bounding-planning
  ?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_RESPONSE_RUNTIME_AND_PARKER_HOST_ISOLATION_PLANNING_REVIEW.md
FILES_CHANGED=1 (this document, new, untracked)
EXISTING_FILES_MODIFIED=NONE
STAGED_COMMITTED_PUSHED=NONE
PULL_REQUEST_OPENED=NONE
INDEPENDENT_CONSTITUTIONAL_REVIEW_OF_THIS_DOCUMENT=NOT CREATED, per this task's own boundary
PROXMOX_VM_CONTAINER_FILESYSTEM_PROCESS_CHANGE=NONE
HOME_ASSISTANT_AND_FRIGATE=UNTOUCHED, REMAIN STOPPED
MODEL_ACQUISITION=NONE
MODEL_LOAD_OR_CONTACT=NONE
EXPLICIT_EXECUTION_APPROVAL=NOT ISSUED
KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3=NOT STARTED, NOT REACHABLE
```

## 29. Constitutional conformance

Independently checked against `docs/architecture/parker-constitution.md`: this review proposes no capability that authorizes itself, no path from proposal to execution that bypasses the Permission Engine, and no action reducing the owner's ability to see, limit, or stop what any future Family F action does. It is read-only, offline-only, and self-limiting to exactly the evaluative task this task assigned — consistent with the constitution's "Cognition proposes. Trust authorises. Runtime executes." discipline and its "No capability may bypass trust" principle. It preserves, and does not weaken, the constitution's "the owner remains in control" principle by refusing to silently authorize stopping production Parker or production Ollama, and by refusing to select a numeric bound the owner has not seen evidence for.

## 30. Final authority statement

```text
PROGRAMME_STATUS=ACTIVE
FAMILY_F_STATUS=INCLUDED_FOR_PRE_QUALIFICATION_DIAGNOSTIC_SCOPING_ONLY (unchanged)
IMPLEMENTATION_STATUS=ACCEPTED (unchanged)
READINESS=NOT READY (unchanged)
E_STATUS=UNCOMPUTABLE (unchanged; methodology to close the request-side component fixed in Section 7.1; the response-side component remains blocked on missing provider evidence, Section 7.2)
R_STATUS=UNCOMPUTABLE (unchanged; neither admissible evidence source exists, Section 14)
CATEGORY_6_COEXISTENCE_GAP=ADDRESSED BY EXPLICIT HAND-OFF -- Path 1 (VM 102 same-VM amendment) recommended against on current evidence (Section 17); Path 2 (dedicated diagnostic VM) recommended as the more tractable future direction, further evidence required (Section 18); Path 3 (remain blocked) preserved (Section 19)
NUMERIC_BOUND_SELECTED=NONE
HOST_SELECTED_OR_PROVISIONED=NO
VM_CREATED_RESIZED_OR_CONFIGURED=NO
MODEL_ACQUISITION_AUTHORIZED=NO
MODEL_RUN_AUTHORIZED=NO
CAMPAIGN_AUTHORIZED=NO
EXPLICIT_EXECUTION_APPROVAL_AUTHORIZED=NO
KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3_AUTHORIZED=NO
NEXT_LAWFUL_ACTION=an Independent Constitutional Review of this Planning Review (not created by this task); if accepted and merged, the next lawful action becomes drafting the recommended Family F Capture-Proxy Response/Request-Size and Dedicated-Runtime-Growth Bounding Scope Lock, itself subject to its own Independent Constitutional Review, and separately, a future Alternative-Host-track document evaluating a dedicated diagnostic VM under Section 18's findings -- neither drafted, authorized, nor implemented by this document
```
