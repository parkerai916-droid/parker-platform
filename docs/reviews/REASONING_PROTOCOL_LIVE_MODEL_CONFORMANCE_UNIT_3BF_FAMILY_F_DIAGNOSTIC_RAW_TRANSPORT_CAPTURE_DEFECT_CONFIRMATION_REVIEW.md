**Status:** Unit 3-BF Family F Diagnostic Raw Transport Capture Defect Confirmation Review — **DEFECT CONFIRMED.** Independently tracing the literal runtime path from `FamilyFCaptureProxy` through `FamilyFCampaignLedger.recordTransport`, `transport.jsonl`, recovery, terminal classification, and sealing, this review confirms that the accepted implementation's durable transport ledger persists only SHA-256 hashes, HTTP status, and forwarding outcome for every one of the 392 calls — never the complete raw request body, complete raw response bytes, or response headers Plan Sections 12 and 18 explicitly require the proxy to durably record. The Plan-promised recovery guarantee ("a complete durably captured response without a terminal record may be classified offline without another model call") is consequently unsatisfiable as implemented: no code path exists that could perform such classification from a hash alone, and none is exercised by any test. The accepted Implementation Completion Review's `TRANSPARENT_CAPTURE=CONFIRMED` claim materially overstates what is durably recorded, conflating tested in-memory proxy transparency with untested on-disk durability. This is a genuine implementation defect, correctable within the existing three-file, offline, test-tier scope, requiring no new file and no `src/` change. No live model call occurred. `READINESS=NOT READY` and all execution authority remain exactly as the accepted Readiness Review left them, now with an additional, independent, non-environmental reason execution must not proceed until this defect is corrected and re-reviewed.

# Reasoning Protocol Live-Model Conformance — Unit 3-BF Family F Diagnostic Raw Transport Capture Defect Confirmation Review

## 1. Baseline, worktree, and scope

```text
BASELINE=938667919f445d05d654624c1a3d3c01a17613eb
SEPARATE_WORKTREE=/tmp/parker-family-f-raw-capture-defect-confirmation
BRANCH=governance/reasoning-protocol-family-f-raw-capture-defect-confirmation
```

This review was authored entirely inside a separate git worktree, created from the exact merged baseline above via `git worktree add -b governance/reasoning-protocol-family-f-raw-capture-defect-confirmation /tmp/parker-family-f-raw-capture-defect-confirmation 938667919f445d05d654624c1a3d3c01a17613eb`, independently confirmed clean and at that exact commit (`git rev-parse HEAD` == the baseline) before any investigation began. The primary worktree at `/home/steve/parker-platform`, including its uncommitted, untracked Family F Alternative Diagnostic Host Requirements Scope Lock, was not modified, staged, committed, or deleted by any action in this review — it was preserved by construction, since a separate worktree shares repository history but not working-tree state.

This review does not modify the accepted implementation, the accepted Completion Review, the accepted Independent Constitutional Review, the Scope Lock, the Plan, the Readiness Review, the Planning Review, or their Independent Constitutional Reviews. It cites and quotes them; it edits none of them.

## 2. Controlling authority read

Read completely for this task:

- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_EXPERIMENTAL_RECLASSIFICATION_AND_QUALIFICATION_BOUNDARY_SCOPE_LOCK.md` (accepted Scope Lock);
- `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_EXECUTION_PLAN.md`, with particular attention to Sections 12 (production-path fidelity and transparent capture), 17 (durable campaign layout), 18 (exact-once dispatch and recovery), and 23 (implementation verification requirements);
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_COMPLETION_REVIEW.md` (accepted Completion Review);
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` (accepted Implementation Independent Constitutional Review);
- `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` and `tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt` at `73f8bdc`/`1c699f7`, in full, this session;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_READINESS_REVIEW.md` and its accepted Independent Constitutional Review, and `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_READINESS_BLOCKER_RESOLUTION_PLANNING_REVIEW.md` and its accepted Independent Constitutional Review — checked for reliance on raw-capture claims (Section 9 below);
- the currently uncommitted, preserved Family F Alternative Diagnostic Host Requirements Scope Lock, by preservation reference only, from its content as authored — not re-read from disk in this worktree (it does not exist on this branch; it exists only, untouched, in the primary worktree).

## 3. Literal runtime path traced

```text
FamilyFCaptureProxy.ProxyHandler.handle(exchange)
  -> reads exchange.requestBody fully into a ByteArray, computes requestSha256
  -> forwards to upstreamBaseUrl via HttpClient.send(...)
  -> reads upstream response fully into a ByteArray, computes responseSha256
  -> constructs FamilyFProxyExchangeRecord(requestBody=<full bytes>, responseBody=<full bytes>, responseHeaders=<full map>, requestSha256, responseSha256, responseStatus, forwardingOutcome, ...)
  -> listener.onExchange(record)   [ReasoningProtocolFamilyFDiagnosticTest.kt:526-541]
       -> this call happens BEFORE exchange.sendResponseHeaders(...)/exchange.responseBody.write(...) (lines 543-552) -- ordering is correct
FamilyFTrialTransportRecorder.onExchange(record)  [ReasoningProtocolFamilyFDiagnosticTest.kt:673-677]
  -> ledger.recordTransport(trialId, record)
FamilyFCampaignLedger.recordTransport(trialId, record)  [ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:636-651]
  -> FamilyFChainedLedger.append(transportFile, cursor, campaignId, trialId,
        listOf(
          "exchangeSequence" to record.sequence,
          "requestSha256" to record.requestSha256,
          "responseStatus" to (record.responseStatus ?: -1),
          "responseSha256" to (record.responseSha256 ?: ""),
          "forwardingOutcome" to record.forwardingOutcome,
        ))
  -- record.requestBody, record.responseBody, and record.responseHeaders are never read here; they are silently dropped
transport.jsonl   -- one line per exchange, containing only the five fields above plus the universal chain envelope; no raw bytes, no headers, anywhere
FamilyFCampaignLedger.recover()  [ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:710-734]
  -> pendingOfflineClassification = transportIds - terminalIds
  -- computed purely from trial-ID set membership across ledger files; never reads or attempts to reconstruct any response content
  -- no function anywhere in either file performs "offline classification" of a pendingOfflineClassification trial
FamilyFOrchestrationDriver.runBlock(...)  [ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:1108-1156]
  -> modelCaller.call(trial, roleModelName) -> FamilyFRealModelCaller.call(...) [ReasoningProtocolFamilyFDiagnosticTest.kt:684-699]
       -> delegates to the unmodified, pre-existing ReasoningProtocolLiveModelEvaluationHarness.execute(...)
       -> that harness's own TransportCapture (ReasoningProtocolLiveModelEvaluationHarness.kt:315-334) DOES populate
          requestBody/rawOllamaEnvelope/extractedResponse as real text on the TrialObservation it returns
  -> ledger.recordTerminal(trial.id, EvaluationJsonLines.trial(observation))
       -> EvaluationJsonLines.trial(...) DOES serialize requestBody/rawOllamaEnvelope/extractedResponse into terminal.jsonl's payload
          (ReasoningProtocolLiveModelEvaluationHarness.kt:541-579) -- for a trial that reaches this line before any crash
sealAfterAdvancementRecorded(...)  [ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:736-...]
  -> calls recover() again; throws if pendingOfflineClassification is non-empty -- the ONLY handling of that state is a hard, permanent, unsealable halt
manifest/SHA256SUMS.txt
  -> hashes the named mandatory artifact files as a whole; does not itself expose or depend on per-record raw-body content
```

## 4. Item-by-item findings

### 4.1 — Are complete raw request bodies durably written anywhere?

**No, not in the location Plan Section 12 names.** The only durable, append-only record of a request is `transport.jsonl`'s `requestSha256` field — a one-way SHA-256 digest, not the request body itself. The complete raw request body exists durably only as a side effect, inside `terminal.jsonl`'s `requestBody` field, and only for a trial that survives to `recordTerminal` without crashing first — a fact of the pre-existing, shared `ReasoningProtocolLiveModelEvaluationHarness`'s own capture mechanism (`TransportCapture.formatRequest`), not of the Family F proxy or ledger this Plan section is actually describing.

```text
RAW_REQUEST_DURABILITY=NOT DURABLY WRITTEN BY THE PROXY/TRANSPORT LEDGER PLAN SECTION 12 NAMES — only its SHA-256 is; the full text incidentally reaches terminal.jsonl via a separate, pre-existing harness capture path, but only for trials that complete without crashing before recordTerminal
```

### 4.2 — Are complete raw response bytes durably written anywhere?

**No, not in the location Plan Section 12 names**, for the same reason as 4.1: `transport.jsonl`'s `responseSha256` is a hash only. `response_headers` is not persisted anywhere at all (Section 4.5 below). The response text incidentally reaches `terminal.jsonl`'s `rawOllamaEnvelope`/`extractedResponse` fields via the same pre-existing harness mechanism, again only for trials that complete without crashing first.

```text
RAW_RESPONSE_DURABILITY=NOT DURABLY WRITTEN BY THE PROXY/TRANSPORT LEDGER PLAN SECTION 12 NAMES — only its SHA-256 is; the full text incidentally reaches terminal.jsonl for trials that complete without crashing before recordTerminal
```

### 4.3 — Exact fields persisted in `transport.jsonl`

Per record, from `FamilyFChainedLedger.append`'s universal envelope plus `recordTransport`'s own payload list:

```text
schemaVersion, campaignId, trialId, sequence, priorRecordHash, recordHash, timestamp   (universal envelope, every ledger file)
exchangeSequence, requestSha256, responseStatus, responseSha256, forwardingOutcome     (recordTransport's own payload -- the complete list, nothing more)
```

```text
TRANSPORT_FIELDS=schemaVersion, campaignId, trialId, sequence, priorRecordHash, recordHash, timestamp, exchangeSequence, requestSha256, responseStatus, responseSha256, forwardingOutcome -- NO requestBody, NO responseBody, NO responseHeaders field exists anywhere in this record
```

### 4.4 — Were hashes alone incorrectly described as raw capture?

**Yes.** The accepted Completion Review's Section 9 states: `` `TRANSPARENT_CAPTURE=CONFIRMED`: four dedicated tests against a real loopback `HttpServer` fake upstream independently prove byte-for-byte request/response body preservation, response-status preservation, an interpretation-relevant header's preservation, exactly-one forwarding per inbound request, and correct `502`/failure-record behavior when the upstream is unreachable — durably recording the exchange (via `listener.onExchange`) strictly before the response bytes are released to the caller. ``

This sentence is factually accurate about what the four cited tests actually assert (Section 5 below) and about the *ordering* of `listener.onExchange` relative to response release — but it juxtaposes "byte-for-byte request/response body preservation" (proven only in-memory, via a test-only `RecordingListener`) with "durably recording the exchange... before the response bytes are released," without ever stating that what is *actually* durably recorded through the real `FamilyFTrialTransportRecorder` → `FamilyFCampaignLedger.recordTransport` path is only the two SHA-256 hashes, the status, and the forwarding outcome — never the bytes themselves. A reader relying on this sentence alone would reasonably conclude the durable ledger contains byte-for-byte content; it does not.

```text
HASHES_DESCRIBED_AS_RAW_CAPTURE=YES — the Completion Review's TRANSPARENT_CAPTURE=CONFIRMED line conflates tested in-memory transparency with untested on-disk durability, and never discloses that recordTransport's actual field list is hash-only
```

### 4.5 — Are response status and required headers persisted?

`responseStatus` is persisted (as `-1` when absent). **Response headers are not persisted anywhere in `transport.jsonl` or any other durable ledger file.** `record.responseHeaders` (a full `Map<String, List<String>>`, correctly captured in-memory by the proxy, and correctly forwarded to the real caller) is read by nothing in `recordTransport`.

```text
STATUS_AND_HEADERS=STATUS PERSISTED; HEADERS NOT PERSISTED ANYWHERE DURABLE — Plan Section 12's "interpretation-relevant response headers" requirement is unmet by the transport ledger
```

### 4.6 — Does raw response durability occur before release to `LocalHttpModelInferenceClient`?

**The ordering guarantee is genuine, but it only orders what little is actually persisted.** `listener.onExchange(record)` (`ReasoningProtocolFamilyFDiagnosticTest.kt:526`) executes strictly before `exchange.sendResponseHeaders(...)` and the response body write (lines 549-552) — independently confirmed by direct source-order reading, matching Plan Section 18 step 5's ordering requirement. But because `recordTransport` discards the raw bytes, this correct ordering durably persists only a hash before release, not the "raw response" Plan Section 18 step 5 says must be durably persisted at that point.

```text
DURABLE_BEFORE_RELEASE=ORDERING CORRECT, BUT WHAT IS DURABLY PERSISTED UNDER THAT ORDERING IS ONLY A HASH, NOT THE RAW RESPONSE PLAN SECTION 18 STEP 5 REQUIRES
```

### 4.7 — Can `transport-without-terminal` genuinely be classified offline without another model call?

**No.** Plan Section 18's recovery rules state: "a complete durably captured response without a terminal record may be classified offline without another model call." `FamilyFCampaignLedger.recover()` correctly *identifies* this state (`pendingOfflineClassification = transportIds - terminalIds`, `ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:731`) and its own doc comment (lines 704-705) explicitly restates the Plan's promise ("classifiable offline without a new call"). But **no function anywhere in either implementation file reads a `transport.jsonl` record and produces a classification from it** — there is nothing to classify, since only a hash was ever persisted, and a SHA-256 digest cannot be parsed, matched against a fixture's expected action, or scored for content fidelity. The only code that *consumes* `pendingOfflineClassification` is `sealAfterAdvancementRecorded`, which throws `FamilyFArtifactIntegrityException("cannot seal: N trial(s) pending offline classification")` if the set is non-empty (line 745) — a permanent, unsealable halt, not the resumable offline classification the Plan and the code's own comment both promise.

```text
OFFLINE_CLASSIFICATION_RECOVERY=STRUCTURALLY UNSATISFIABLE AS IMPLEMENTED — the state is correctly detected but there is no content to classify and no classification function exists; the only real behavior is a permanent halt at sealing, contradicting the Plan Section 18 / code-comment claim that this state is "resumable, classifiable offline without a new call"
```

### 4.8 — Can crash recovery reconstruct the exact extracted response and parser result?

**No, for the specific window Plan Section 18 is written to cover** (a crash between a durably captured transport response and the terminal record) — for the reason given in 4.7: the content needed does not exist in durable storage. Separately and incidentally, `decodeObservation` (`ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:1181-1224`, used to reconstruct `TrialObservation` values from `terminal.jsonl` for role reporting) hardcodes `requestBody = null`, `rawOllamaEnvelope = null`, `extractedResponse = null`, `parsedVariant = null`, `prompt = ""`, `promptSha256 = ""` even for *resolved* trials whose `terminal.jsonl` payload does contain this text (Section 4.9 below) — this is a narrower, secondary gap: role reporting does not need these fields for its own purpose (fixture ID, expected/actual action, representation validity, content fidelity, primary classification are all it reconstructs, and all it needs), so this omission is not itself a Plan violation, but it means no code path in either file ever reads raw content back out of `terminal.jsonl` either, even where that content is present.

```text
EXACT_ONCE_IMPACT=THE DISPATCH-TO-TERMINAL CRASH WINDOW IS UNRECOVERABLE AS DESIGNED -- a trial dispatched, transport-captured, and then interrupted before recordTerminal has no path to completion other than a permanent artifact-integrity halt at sealing; this is a genuine exact-once/durability defect relative to Plan Section 18, not merely a documentation gap
```

### 4.9 — Does any test prove durable on-disk raw-body recovery rather than in-memory proxy transparency?

**No.** The four tests the Completion Review cites (`proxy forwards request and response bytes byte-for-byte and preserves status`, `proxy preserves an interpretation-relevant response header`, `proxy forwards exactly once per inbound request`, `proxy records a forwarding failure and responds 502 when the upstream is unreachable` — `ReasoningProtocolFamilyFDiagnosticTest.kt:910, 944, 970, 1003`) all construct a private `RecordingListener` (`line 890-895`) — a pure in-memory `mutableListOf<FamilyFProxyExchangeRecord>()` — and assert against its in-memory `record.requestBody`/`record.responseBody`/`record.responseHeaders` fields directly. None of these tests constructs a real `FamilyFCampaignLedger`, calls `recordTransport`, writes to an actual `transport.jsonl` on disk, or reads any file back to verify raw-body content survives a durability round-trip. The one existing test that *does* exercise `recordTransport` against a real ledger and real `transport.jsonl` file (`recover treats transport-captured-but-unclassified trials as pending offline classification, not ambiguous`, line 1522) constructs its `FamilyFProxyExchangeRecord` with `ByteArray(0)` (empty) for both `requestBody` and `responseBody`, and asserts only trial-ID set membership — it neither exercises nor could reveal the defect, since it never populates or checks for real byte content in the first place.

```text
TEST_COVERAGE_GAP=CONFIRMED — every existing test proving "byte-for-byte preservation" uses an in-memory-only RecordingListener never connected to the real ledger; the one test that does exercise the real recordTransport path uses empty byte arrays and asserts only ID-set membership; no test anywhere writes real bytes through recordTransport and reads them back from transport.jsonl
```

### 4.10 — Do the Completion Review and Independent Constitutional Review contain inaccurate acceptance claims?

**The Completion Review does, in Section 9 (quoted in full in 4.4 above).** `TRANSPARENT_CAPTURE=CONFIRMED` is stated without qualification, immediately adjacent to a durability claim, in a way that does not distinguish tested in-memory transparency from untested on-disk durability, and without ever disclosing `recordTransport`'s actual, hash-only field list.

**The Independent Constitutional Review does not repeat this specific sentence, but its own adversarial-hunting scope (Section 2) does not name raw-capture durability as one of the risk categories it specifically hunted for** (it lists: authority beyond the three-file boundary; live contact without both gates; forbidden production/KD symbols; `src/` edits; double-gate weakening; Gradle lifecycle wiring; ranking/substitution/qualification-credit leakage; treating an offline test as live evidence; TBD/placeholder gaps — durable raw-capture field accuracy is not among them). Its verdict (`PRODUCTION_PATH_FIDELITY=CONFIRMED`, Section 12) was reached without independently re-deriving `recordTransport`'s actual persisted-field list against Plan Section 12's explicit "complete raw request body... complete raw response bytes" text, the way it did independently re-derive, for example, the double-gate mechanism (Section 4) and the forbidden-symbol scan (Section 6) at the same level of field-by-field rigor. This is a gap in adversarial coverage, not a false sentence — but it means the accepted `VERDICT=ACCEPTED` was reached without this specific area receiving the review's own stated standard of independent, primary-source re-derivation.

```text
ACCEPTED_REVIEW_OVERCLAIMS=COMPLETION REVIEW SECTION 9 CONTAINS AN INACCURATE/UNQUALIFIED TRANSPARENT_CAPTURE=CONFIRMED CLAIM; THE IMPLEMENTATION INDEPENDENT CONSTITUTIONAL REVIEW DID NOT INDEPENDENTLY RE-DERIVE THIS SPECIFIC AREA TO ITS OWN STATED ADVERSARIAL STANDARD AND SHOULD BE TREATED AS REQUIRING A SUPERSEDING NOTE ALONGSIDE THE COMPLETION REVIEW
```

### 4.11 — Is the Host Requirements Scope Lock's evidence estimator currently impossible?

**Yes, in the specific sense the question asks.** The (uncommitted, preserved, not edited by this review) Family F Alternative Diagnostic Host Requirements Scope Lock's disk-capacity itemization requires future evidence-package sizing to account for, among other things, "the complete raw request/response capture for all 392 calls (full HTTP bodies, not summaries)" as a disk-space consumer. As of this baseline, **no such artifact exists in the accepted implementation** — the durable transport ledger contains only hashes, so there is currently nothing on disk matching that description to size for at the transport-ledger layer. (Raw text does incidentally exist inside `terminal.jsonl` for non-crashed trials, per Section 4.1/4.2 above, so the itemization is not entirely fictional — but it is not sized correctly against the transport ledger specifically, and is silent about the fact that this content's durability is contingent on no crash occurring before `recordTerminal`.) This finding does not modify the Scope Lock document; it is recorded here as a fact that document's own future correction, or the Explicit Execution Approval process, will need to account for.

```text
HOST_SCOPE_LOCK_IMPACT=THE DISK-CAPACITY ITEMIZATION'S "COMPLETE RAW REQUEST/RESPONSE CAPTURE" LINE ITEM DESCRIBES AN ARTIFACT THAT DOES NOT CURRENTLY EXIST AT THE TRANSPORT-LEDGER LAYER; THE SCOPE LOCK ITSELF IS NOT EDITED BY THIS REVIEW AND REMAINS PRESERVED EXACTLY AS AUTHORED
```

### 4.12 — Smallest lawful correction boundary within the already accepted three-file implementation scope

The defect is fully contained within `FamilyFCampaignLedger.recordTransport` in `tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt` (`ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:636-651`). A lawful correction would, at minimum:

```text
- add the complete raw request body and complete raw response bytes (base64- or equivalently safely-encoded, since the ledger's envelope is UTF-8 JSON text and Ollama traffic may not always be pure ASCII-safe) to recordTransport's persisted payload;
- add the response headers (or the specific interpretation-relevant subset Plan Section 12 names) to that same payload;
- add a genuine offline-classification function that reads a pendingOfflineClassification trial's transport record, reconstructs the response, runs it through the unmodified production parser/classifier, and produces a terminal record -- fulfilling, rather than merely detecting, the Plan Section 18 recovery guarantee;
- add at least one new test that writes a real FamilyFProxyExchangeRecord with non-empty, realistic bytes through a real FamilyFCampaignLedger.recordTransport call, closes and reopens that ledger (or reads transport.jsonl directly), and asserts the exact original bytes are recoverable byte-for-byte from disk -- proving durable recovery, not merely in-memory transparency;
- add at least one new test exercising the new offline-classification function end-to-end for a transport-without-terminal state.
```

None of this requires a fourth file or any `src/` change; it is a correction entirely inside the two existing `tests/integration/` files, within the exact boundary the accepted Plan Section 5 already authorized.

```text
MINIMUM_CORRECTION_SCOPE=CONTAINED ENTIRELY WITHIN tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt (recordTransport's payload list, plus new offline-classification logic and new tests); no new file; no src/ change; the existing two-file, three-file-total implementation boundary is sufficient for the fix
```

### 4.13 — Which accepted reviews and readiness conclusions require correction or superseding notes

```text
GOVERNANCE_DOCUMENTS_AFFECTED:
- docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_COMPLETION_REVIEW.md -- Section 9's TRANSPARENT_CAPTURE=CONFIRMED claim requires a correction/superseding note (Section 4.4, 4.10 above)
- docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md -- did not independently re-derive this area to its own stated standard; requires a superseding note alongside the Completion Review's correction (Section 4.10 above)
- the currently uncommitted, preserved Family F Alternative Diagnostic Host Requirements Scope Lock -- its disk-capacity itemization line item describing raw-capture artifacts requires future correction when work on that document resumes; NOT edited by this review (Section 4.11 above)

NOT AFFECTED -- independently grepped for reliance on raw-capture claims and found silent/uninvolved:
- docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_READINESS_REVIEW.md and its accepted Independent Constitutional Review -- entirely about host environment (identity, RAM, disk, isolation); makes no raw-capture or transport-ledger claim
- docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_READINESS_BLOCKER_RESOLUTION_PLANNING_REVIEW.md and its accepted Independent Constitutional Review -- entirely about host-path evaluation; makes no raw-capture or transport-ledger claim
- the Unit 3-BF Scope Lock and the Implementation/Execution Plan themselves -- these correctly PRESCRIBE the raw-capture requirement (Sections 12, 18); the implementation failed to satisfy them, so these upstream governance documents require no correction
```

## 5. What is confirmed correct, not defective

For completeness and precision, the following claims the Completion Review and its Independent Constitutional Review make about the proxy and transport path were independently re-verified and found accurate, and are not disturbed by this review:

```text
- the proxy genuinely forwards request/response bytes unchanged in-memory and in-transit (the four cited tests do prove this, correctly, for what they actually test);
- exactly-one forwarding per inbound request is genuine;
- 502/failure-record behavior on unreachable upstream is genuine;
- listener.onExchange(record) genuinely executes before the response is released to the real HTTP caller -- the ordering claim is accurate;
- response status IS durably persisted in transport.jsonl;
- the universal chained-ledger mechanism, genesis hash, rejection matrix, verify-before-recovery/re-verify-before-sealing, and manifest-layer separation (Sections 7, 10 of the Completion Review) are unaffected by this defect and remain independently accurate, as re-confirmed by this review's own reading of the same code;
- production-path fidelity (src/ byte-identical to baseline, unmodified DefaultReasoningPromptBuilder/harness reuse) is unaffected and remains accurate.
```

## 6. Model contact and authority during this review

```text
MODEL_CONTACT=NONE -- no live endpoint was contacted; ./gradlew reasoningProtocolFamilyFDiagnostic --rerun-tasks was run in the separate worktree and reproduced BUILD SUCCESSFUL with the identical 91+21=112 test counts, 0 failures, 1 correctly self-skipped live-trigger test, matching the accepted Completion Review's own prior run exactly -- confirming the defect exists despite a fully passing offline test suite, precisely because no existing test exercises the gap
```

## 7. Readiness and execution authority

Nothing in this review reopens, weakens, or strengthens the accepted Readiness Review's `READINESS=NOT READY` determination, which rests on entirely separate, host-environment grounds (Items 7-12 of that review: provider-executable identity, model-artifact identity/size, and the consequent memory-gate `UNDETERMINABLE` finding). This review adds a second, independent, non-environmental reason execution must not proceed even on a hypothetically qualifying host: the accepted implementation's own crash-recovery guarantee is currently unsatisfiable as built, and must be corrected and re-reviewed (Completion Review + its Independent Constitutional Review, at minimum) before any future Readiness Review or Explicit Execution Approval could soundly rely on it.

```text
READINESS=NOT READY (unchanged; this review does not reopen the accepted Readiness Review's host-environment findings, and adds an independent implementation-correctness reason execution remains inappropriate)
EXECUTION_AUTHORITY=NO -- unchanged; nothing in this review authorizes model contact, campaign creation, implementation correction, or Knowledge Discoverability Attempt 3
```

## 8. Defect classification

```text
DEFECT_CLASSIFICATION=CONFIRMED IMPLEMENTATION DEFECT (not a governance or planning defect) -- the accepted Scope Lock and Plan correctly require durable raw-body capture and offline-classification recovery (Sections 12, 18); the accepted implementation's recordTransport function does not satisfy that requirement, and the accepted Completion Review's TRANSPARENT_CAPTURE=CONFIRMED claim inaccurately represents what is actually durably persisted. Severity: the exact-once/durability guarantee for the dispatch-to-terminal crash window is genuinely broken (a real trial interrupted in that window cannot be recovered, only permanently halted at sealing), not merely a documentation inaccuracy, though the documentation inaccuracy (Section 4.4/4.10) is itself independently confirmed.
```

## 9. Exit criteria for this confirmation review

This review is complete only when it has, without performing any correction:

1. traced the literal runtime path from proxy to sealing and quoted exact, line-cited source for every claim;
2. answered all fifteen verification items with direct evidence, not inference;
3. distinguished the genuine, tested proxy-transparency properties from the untested durability gap;
4. identified the smallest lawful correction boundary without performing it;
5. identified every accepted governance document requiring a correction or superseding note, without editing any of them;
6. confirmed zero model contact and an unchanged `READINESS=NOT READY`/`EXECUTION_AUTHORITY=NO` state;
7. left the primary worktree, including its preserved uncommitted Host Requirements Scope Lock, completely untouched;
8. produced a clean `git diff --check` for this one new file in the separate worktree; and
9. staged, committed, pushed, or opened no pull request.

All nine are satisfied as of this document.

## 10. Structured findings summary

```text
BASELINE=938667919f445d05d654624c1a3d3c01a17613eb
SEPARATE_WORKTREE=/tmp/parker-family-f-raw-capture-defect-confirmation (branch governance/reasoning-protocol-family-f-raw-capture-defect-confirmation)
RAW_REQUEST_DURABILITY=NOT DURABLY WRITTEN AT THE PROXY/TRANSPORT-LEDGER LAYER PLAN SECTION 12 NAMES -- only its SHA-256 is; full text incidentally reaches terminal.jsonl for non-crashed trials only
RAW_RESPONSE_DURABILITY=NOT DURABLY WRITTEN AT THE PROXY/TRANSPORT-LEDGER LAYER PLAN SECTION 12 NAMES -- only its SHA-256 is; full text incidentally reaches terminal.jsonl for non-crashed trials only
TRANSPORT_FIELDS=schemaVersion, campaignId, trialId, sequence, priorRecordHash, recordHash, timestamp, exchangeSequence, requestSha256, responseStatus, responseSha256, forwardingOutcome -- no raw body, no headers
STATUS_AND_HEADERS=STATUS PERSISTED; HEADERS NOT PERSISTED ANYWHERE DURABLE
DURABLE_BEFORE_RELEASE=ORDERING CORRECT; CONTENT PERSISTED UNDER THAT ORDERING IS HASH-ONLY, NOT RAW BYTES
OFFLINE_CLASSIFICATION_RECOVERY=STRUCTURALLY UNSATISFIABLE AS IMPLEMENTED -- state detected, never resolved; only outcome is a permanent halt at sealing
EXACT_ONCE_IMPACT=DISPATCH-TO-TERMINAL CRASH WINDOW IS UNRECOVERABLE AS DESIGNED -- genuine durability defect, not merely documentation
TEST_COVERAGE_GAP=CONFIRMED -- all "byte-for-byte" tests use an in-memory-only RecordingListener; the one real-ledger transport test uses empty byte arrays and checks only ID-set membership
ACCEPTED_REVIEW_OVERCLAIMS=COMPLETION REVIEW SECTION 9 (TRANSPARENT_CAPTURE=CONFIRMED) INACCURATE/UNQUALIFIED; IMPLEMENTATION INDEPENDENT CONSTITUTIONAL REVIEW DID NOT INDEPENDENTLY RE-DERIVE THIS AREA TO ITS OWN STATED STANDARD
HOST_SCOPE_LOCK_IMPACT=DISK-CAPACITY ITEMIZATION'S RAW-CAPTURE LINE ITEM DESCRIBES AN ARTIFACT NOT CURRENTLY PRODUCED AT THE TRANSPORT-LEDGER LAYER; DOCUMENT NOT EDITED BY THIS REVIEW
DEFECT_CLASSIFICATION=CONFIRMED IMPLEMENTATION DEFECT, CONTAINED, CORRECTABLE WITHIN EXISTING SCOPE
MINIMUM_CORRECTION_SCOPE=WITHIN tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt ONLY -- no new file, no src/ change
GOVERNANCE_DOCUMENTS_AFFECTED=Implementation Completion Review (correction note required); Implementation Independent Constitutional Review (superseding note required); Host Requirements Scope Lock (future correction noted, not performed); Readiness Review/ICR and Planning Review/ICR unaffected
MODEL_CONTACT=NONE
READINESS=NOT READY (unchanged, independently reconfirmed as still correct on separate grounds)
EXECUTION_AUTHORITY=NO (unchanged)
DIFF_CHECK=CLEAN
FILES_CHANGED=1 (this document only, in the separate worktree)
PRIMARY_WORKTREE_PRESERVED=YES -- /home/steve/parker-platform and its uncommitted Host Requirements Scope Lock were not modified, staged, committed, or deleted
GIT_STATUS=one untracked file in the separate worktree; nothing staged, committed, or pushed in either worktree; no PR opened; no model endpoint contacted; no live diagnostic task run with execution approval; no Knowledge Discoverability Attempt 3
```
