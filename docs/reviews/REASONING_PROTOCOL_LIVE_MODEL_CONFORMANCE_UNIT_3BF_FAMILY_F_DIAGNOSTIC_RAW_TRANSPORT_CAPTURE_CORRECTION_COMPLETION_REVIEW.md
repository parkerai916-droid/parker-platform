**Status:** Completion Review of the Unit 3-BF Family F Diagnostic Raw Transport Capture Defect Correction — **ACCEPTED.** This review independently confirms the uncommitted, single-file correction to `tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt` fully remedies the confirmed defect: `FamilyFCampaignLedger.recordTransport` now durably persists the complete raw request body, complete raw response bytes, and complete response headers (Base64-encoded with explicit byte counts and independently-recomputed SHA-256, so arbitrary non-UTF-8-safe bytes round-trip losslessly), and a genuine, fail-closed, zero-model-call offline-classification path (`FamilyFOfflineRecovery.recoverTerminalPayload`) now exists and is exercised end-to-end for a transport-without-terminal trial, reusing the unmodified production formatter, response extractor, and tagged parser. This document, together with its Independent Constitutional Review, explicitly supersedes the inaccurate `TRANSPARENT_CAPTURE=CONFIRMED` (Section 9) and `EXACT_ONCE_AND_NO_RETRY=CONFIRMED` (Section 8, offline-recoverability clause) claims of the accepted Unit 3-BF Family F Diagnostic Implementation Completion Review, without disturbing any of that review's other, unrelated, still-accurate findings.

# Unit 3-BF Family F Diagnostic Raw Transport Capture Defect Correction — Completion Review

## 1. Reviewed baseline and scope

```text
BASELINE=d1a3dd9e8c1d1a05b9978995fd4d2793d9e1f17e
WORKING_TREE_HEAD=d1a3dd9e8c1d1a05b9978995fd4d2793d9e1f17e (baseline itself; changes uncommitted)
BRANCH=implementation/reasoning-protocol-family-f-raw-capture-correction
WORKTREE=/tmp/parker-family-f-raw-capture-correction
```

Independently confirmed via `git status --porcelain` and `git diff d1a3dd9e --stat` that **exactly one** file is changed relative to baseline, exactly matching the change this task authorizes:

```text
tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt | 1324 +++++++++++++++++++-
1 file changed, 1300 insertions(+), 24 deletions(-)
```

Independently confirmed via `git diff d1a3dd9e --stat -- src/` (empty output) and `git diff d1a3dd9e --stat -- tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt` (empty output) that **zero** bytes changed under `src/`, and the sibling implementation file and the shared harness file are byte-identical to baseline. No fourth file, and no second implementation file, exists anywhere in the working-tree diff — the correction is contained entirely within the single file the accepted Defect Confirmation Review's own Section 4.12 minimum-correction-scope analysis identified as sufficient.

## 2. Controlling authority read, fresh, this task

- `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_EXECUTION_PLAN.md` (accepted Plan), Sections 5, 12, 17, 18, and 23 in particular;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_RAW_TRANSPORT_CAPTURE_DEFECT_CONFIRMATION_REVIEW.md` (accepted Defect Confirmation Review, `DEFECT_CONFIRMED`) and its accepted Independent Constitutional Review — the document defining exactly what this correction must fix and the minimum lawful correction boundary (its own Section 4.12);
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_COMPLETION_REVIEW.md` and `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` (the accepted Implementation Completion Review and its accepted Independent Constitutional Review — the documents this correction's own review must supersede in their raw-capture-related sections only);
- `tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt` (3,763 lines) in full, this session, at the current uncommitted working-tree state;
- `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` in full, this session, with particular attention to `ProxyHandler.handle` (lines 487–577), `FamilyFProxyExchangeRecord` (432–445), and `FamilyFRealModelCaller.call` (684–699), to independently confirm the sibling file's proxy/model-caller behavior the correction relies on is unchanged and correctly understood;
- `tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt`, with particular attention to the private `classify` function (389–446) and `TransportCapture` (315–334), to independently verify the new offline classification mirrors it correctly;
- `src/runtime/ModelInferenceClient.kt` (`LocalHttpModelInferenceClient.infer`, `defaultOllamaRequestBody`, `defaultOllamaResponseBody`) and `src/runtime/ReasoningResponseParser.kt` (`TaggedReasoningResponseParser`, `UnclassifiableModelResponseException`) — the exact unmodified production classes the correction's offline path reuses, read directly from `src/`, not accepted from either test file's own comments.

## 3. What the correction changes — traced against the confirmed defect

Every item the accepted Defect Confirmation Review named as defective is independently re-traced here against the corrected source, not accepted from the correction's own comments:

### 3.1 — Complete raw request/response bytes now durably persisted

`FamilyFCampaignLedger.recordTransport` (line 929) now writes `requestByteCount`, `requestBodyBase64`, `responseCaptured`, `responseByteCount`, `responseBodyBase64`, and `responseHeadersJson` alongside the pre-existing hash/status/outcome fields — independently confirmed by direct reading of the full payload list at lines 940–958. Before appending, the function independently recomputes `familyFSha256Bytes(requestBytes)` and (when a response body exists) `familyFSha256Bytes(responseBody)` and throws `FamilyFArtifactIntegrityException` if either disagrees with what the proxy record claims (lines 930–938) — durable evidence is never persisted merely on the proxy's own say-so.

```text
RAW_REQUEST_DURABILITY=CONFIRMED — complete raw request bytes are Base64-encoded into requestBodyBase64 with an explicit requestByteCount and an independently-recomputed requestSha256, durably appended via the unmodified FamilyFChainedLedger.append/familyFAppendForced (StandardOpenOption.SYNC) mechanism
RAW_RESPONSE_DURABILITY=CONFIRMED — complete raw response bytes are Base64-encoded into responseBodyBase64 (when responseCaptured) with an explicit responseByteCount and an independently-recomputed responseSha256, under the identical durable-append mechanism
```

### 3.2 — Status and full multi-valued headers durably persisted

`responseStatus` continues to be persisted (unchanged). `responseHeadersJson` (line 382 area of the diff; line ~958 in the corrected file) now durably persists the complete `Map<String, List<String>>` via `familyFEncodeHeaders` (line 569): every header name and every value is independently Base64-encoded, keys are sorted for deterministic output, and values retain list order and duplication exactly. Decoding (`familyFDecodeHeaders`, line 586) reconstructs the matched entries and compares the reconstruction byte-for-byte against the original encoded body, failing closed (`FamilyFArtifactIntegrityException`) on any structural deviation rather than silently truncating or misparsing.

Independently re-verified against the new test `duplicate and multi-valued response headers survive durable capture exactly` (line 3343): a `Set-Cookie` header with three values and an `X-Custom-Header` value containing an embedded quote, backslash, and control character round-trip exactly, including the correct list size (3) and correct list identity (`assertEquals(headers["Set-Cookie"], durable.responseHeaders["Set-Cookie"])`).

```text
STATUS_AND_HEADERS=CONFIRMED — responseStatus persisted (as before); full multi-valued responseHeaders now durably persisted via Base64-per-value encoding with sorted, deterministic key ordering, structurally-validated on decode, and independently confirmed to preserve duplicate values, list order, and adversarial byte content (quotes, backslashes, control characters) exactly
```

### 3.3 — Base64/byte-count/SHA-256 round-trip of arbitrary bytes

Independently re-verified against the new test `adversarial unicode, CRLF, quotes, backslashes, NUL, and non-text bytes round-trip losslessly` (line 3324): a request body containing UTF-8 multi-byte characters, an emoji, embedded quote/backslash/CR/LF/tab characters, and raw non-text bytes (`0x00, 0x01, 0x02, 0xFF, 0xFE`), and a response body containing raw non-text bytes followed by the same adversarial text, both round-trip byte-for-byte (`durable.requestBytes.contentEquals(requestBody)`, `durable.responseBytes!!.contentEquals(responseBody)`). Because every byte value is Base64-encoded before insertion into the canonical UTF-8 JSON-lines envelope, no byte sequence — however JSON-unsafe — can ever reach the ledger's UTF-8 text layer unencoded; Base64's own alphabet (`[A-Za-z0-9+/=]`) requires no JSON escaping at all, independently confirmed by direct reading of `familyFEncodeHeaders`/`recordTransport`'s use of `Base64.getEncoder()` with no intervening `familyFJsonEscape` call on the encoded output.

`readDurableTransportRecord` (line 973) and the underlying `familyFDecodeTransportRecord` (line 643) independently re-derive `familyFSha256Bytes` from the *decoded* bytes and compare against the persisted hash a second time at read time (lines 664–668, 690–694 area), and independently compare decoded length against the persisted byte count — a read-time re-verification distinct from, and in addition to, the write-time re-verification in `recordTransport`. Independently re-confirmed via `persisted hashes and byte counts independently recompute from the decoded durable bytes` (line 3366), which additionally re-derives the hash a *third* time directly from the raw `transport.jsonl` line on disk, bypassing `readDurableTransportRecord` entirely.

```text
BYTE_ENCODING_AND_HASHES=CONFIRMED — Base64 encode/decode, explicit byte counts, and SHA-256 independently recomputed at both write time (against the proxy's claimed hash) and read time (against the decoded bytes) round-trip arbitrary bytes, including non-UTF-8-safe content, exactly; empirically verified with adversarial byte content, not merely asserted
```

### 3.4 — Deterministic, unambiguous header encoding

`familyFEncodeHeaders` sorts header names (`headers.keys.sorted()`) before encoding, so the same header map always produces the same encoded text regardless of the source map's iteration order — independently confirmed deterministic by direct reading (line 570). Header names and values are encoded independently of each other and of the four other durable fields on the same line; case is preserved exactly because the header name's raw bytes (whatever case the upstream server sent) are Base64-encoded verbatim, never normalized. Duplicate values for the same header name are preserved as a list, in original order, per the round-trip test in Section 3.2.

```text
HEADER_ENCODING_DETERMINISM=CONFIRMED — sorted keys, independent per-value Base64 encoding, exact case and duplicate-value preservation, structurally validated on decode
```

### 3.5 — Universal chained-ledger envelope coverage unchanged

`recordTransport` still delegates every write to the single, shared `FamilyFChainedLedger.append` (call site unchanged in shape; only the `payloadFields` list is extended) — independently confirmed the six-field universal envelope (`schemaVersion, campaignId, trialId, sequence, priorRecordHash, recordHash, timestamp`) construction in `FamilyFChainedLedger.append` itself is **entirely untouched** by this diff (it appears nowhere in `git diff d1a3dd9e`). All new transport fields (`startedAt, completedAt, requestByteCount, requestBodyBase64, responseCaptured, responseByteCount, responseBodyBase64, responseHeadersJson`) are ordinary payload fields subject to exactly the same chained-hash coverage, forced-durability, and tamper-detection mechanism as every other governed ledger — none of them is a new field name colliding with an envelope field name (independently re-enumerated against the accepted Implementation Completion Review's own Section 6 payload-field-name list plus the eight new names; no collision).

```text
UNIVERSAL_ENVELOPE_COVERAGE=CONFIRMED — all new raw-capture fields are ordinary payload fields under the unchanged, unmodified FamilyFChainedLedger.append mechanism; no new field name collides with an envelope field name
```

### 3.6 — Durable persistence and force occur before response release

Independently re-traced the unchanged `ProxyHandler.handle` in the sibling file: `listener.onExchange(record)` (line 526) — which for the real `FamilyFTrialTransportRecorder` delegates synchronously to `ledger.recordTransport` — executes strictly before `exchange.sendResponseHeaders(...)` (line 549) and the response-body write (line 551), exactly as in the accepted implementation and independently re-confirmed by direct source-order reading (unchanged by this diff). Because `recordTransport` now performs the write-time hash re-verification and the actual forced (`StandardOpenOption.SYNC`) file write synchronously and inline, a persistence failure inside `recordTransport` propagates as an exception out of `onExchange`, out of the try block in `handle`, and is caught by `ProxyHandler`'s own `catch (exception: Exception)` block — which itself attempts a second `listener.onExchange` call for a forwarding-failure record, which fails identically (since the underlying file is still unwritable), so the exception propagates uncaught out of `handle` entirely, and `exchange.sendResponseHeaders` is never reached.

This is independently confirmed, not merely traced, by two new tests: `durable transport exists on disk before the response is released to the caller` (line 3166), which uses a custom `FamilyFDurabilityWitnessListener` that reads the record straight back off disk from *inside* the proxy's own `onExchange` callback — before the proxy's own subsequent `sendResponseHeaders` call — and asserts the durable, hash-verified record is already present; and `transport persistence failure prevents the response from ever being released as successful` (line 3197), which pre-marks `transport.jsonl` read-only and confirms the real upstream's genuinely successful response is never released to the HTTP caller (either the client-side call fails with `IOException`, or, if it somehow succeeds, its status/body do not match the real response — both are checked and both fail the "released successfully" assertion).

```text
DURABLE_BEFORE_RELEASE=CONFIRMED — independently witnessed from inside the pre-release callback (not merely ordering by source-line inspection), and independently confirmed a genuine persistence failure prevents the real response from ever reaching the caller as a success
```

### 3.7 — Persistence failure produces a permanent, ambiguous halt with no retry

`orchestration halts on the very first dispatch when transport persistence fails, never retries, never seals` (line 3255) independently reconstructs a real `FamilyFOrchestrationDriver` against a pre-existing, resumed-looking campaign directory (schedule + all 392 intents already written, exactly as a genuine resume would find it), marks `transport.jsonl` read-only, and runs `driver.run()`. Independently confirmed: `FamilyFCampaignOutcome.HALTED` is returned; `ledger.isHalted()` is true and `ledger.isSealed()` is false; the fake model caller was invoked **exactly once** (`assertEquals(1, modelCallCount, ...)`) — proving no retry occurred; and `dispatch.jsonl` contains **exactly one** record — proving the dispatch was not reissued. This exercises the identical `catch (exception: FamilyFArtifactIntegrityException) { ledger.halt(...) }` / `catch (exception: Exception) { ledger.halt(...) }` path in `FamilyFOrchestrationDriver.run()` that every other integrity failure uses (unchanged by this diff), so the halt-not-retry guarantee for a transport-persistence failure is structurally the same guarantee already independently verified for every other integrity failure by the accepted Implementation Completion Review, now additionally and directly exercised for this specific failure mode.

```text
PERSISTENCE_FAILURE=CONFIRMED — a durable-persistence failure produces exactly the same permanent, no-retry halt as any other integrity failure, independently exercised end-to-end (not merely by code-path inspection) through a real reconstructed driver against a real, pre-populated campaign directory
```

### 3.8 — Forwarding failures cannot masquerade as completed transport

`readCompleteTransportIdSet` (line 1041) restricts "complete" to `responseCaptured == true AND forwardingOutcome == "FORWARDED"` — a forwarding-failure exchange (upstream unreachable, etc.) is captured durably (the proxy records it, per the unchanged `catch` block in `ProxyHandler.handle`), but is correctly excluded from the offline-recoverable set. `recover()` (line 1065) now derives both `pendingOfflineClassification` and `ambiguous` from `completeTransportIds`, not from raw `transportIds` presence — independently re-traced at lines 1074, 1083, and 1091. The new test `recover treats a captured-but-forwarding-failed exchange as ambiguous, not pending offline classification` independently confirms a `FORWARDING_FAILURE: ...` exchange record causes `recover()` to throw `FamilyFArtifactIntegrityException` (permanent ambiguous halt), never to appear in `pendingOfflineClassification`.

```text
FORWARDING_OUTCOME_BOUNDARY=CONFIRMED — only a genuinely captured, FORWARDED response is ever treated as offline-recoverable; a forwarding failure is durably captured but correctly remains ambiguous and permanently halts, independently re-verified by a dedicated test
```

### 3.9 — Offline recovery: reread from disk, verification breadth, production-path fidelity, zero model calls

`FamilyFOrchestrationDriver.recoverPendingOfflineClassifications` (line 1669) is called from `run()` (line ~1630, immediately after `ledger.recover()` and strictly before any `runBlock` dispatch), and for each pending trial calls `ledger.readDurableTransportRecord(trialId)` — which independently reads `Files.readAllLines(transportFile)` fresh off disk (line 973–985) rather than trusting any in-memory state — before calling `FamilyFOfflineRecovery.recoverTerminalPayload` (line 1269).

`recoverTerminalPayload` independently verifies, in order, before producing any terminal payload: (a) **trial identity** — `require(transport.trialId == trial.id)`, throwing `IllegalArgumentException` on mismatch, independently confirmed by the new test `offline recovery rejects a transport record claimed for the wrong trial` (line 3522); (b) **completeness** — `responseCaptured`/`forwardingOutcome`/`responseBytes != null`, throwing `FamilyFArtifactIntegrityException` otherwise, independently confirmed by `offline recovery fails closed when the transport record does not represent a complete captured response` (line 3492); (c) **the expected request**, byte-for-byte, by reconstructing the exact production-formatted request (`SyntheticContextProfiles.construct` → `DefaultReasoningPromptBuilder().buildPrompt` → `defaultOllamaRequestBody`, all unmodified, imported directly from `parker.core.runtime`) and comparing it against the durable request text, throwing on mismatch — independently confirmed by `offline recovery fails closed when the durable request does not match the expected production-formatted request` (line 3506); (d) **the response headers' structural validity** — already independently re-verified during `readDurableTransportRecord`'s own decode, per Section 3.2/3.4, before `recoverTerminalPayload` ever receives the record; (e) implicit **frozen schedule metadata** — the trial itself is looked up from `FamilyFCampaignDefinition.allTrials` (the frozen, unmodified schedule), never from any mutable or ad hoc source.

`recoverTerminalPayload` uses **only** the unmodified production `defaultOllamaRequestBody` (request-shape comparison), `defaultOllamaResponseBody` (response extraction), and `TaggedReasoningResponseParser` (parsing) — all three imported directly from `parker.core.runtime`/`parker.core.interfaces` and independently confirmed present, unmodified, in `src/runtime/ModelInferenceClient.kt` and `src/runtime/ReasoningResponseParser.kt` at the current baseline. The new `familyFOfflineClassify` (line 1223) was independently compared, field by field, against the real harness's private `classify` (`ReasoningProtocolLiveModelEvaluationHarness.kt:389–446`): every branch is identical except the `failure is TimeoutCancellationException -> PrimaryClassification.H` branch, which is correctly omitted because offline recovery awaits nothing and can never time out — independently confirmed this omission is not a fidelity gap but a structurally-forced simplification, since `familyFOfflineClassify` never constructs or receives a `TimeoutCancellationException` in the first place.

Zero model calls: independently confirmed by two tests that construct a `FamilyFModelCaller` whose lambda throws `AssertionError("offline recovery must make zero model calls")`/`"must not be called"` if ever invoked (lines ~3634, ~3680, ~3719) — `recoverPendingOfflineClassifications` completing successfully with `modelCallCount == 0` in all three is a structural proof, not an assertion of absence.

Same terminal observation as the normal path: `transport-without-terminal is classified offline after reconstructing a new ledger and driver instance, with zero model calls, matching normal-path output` (line 3623) independently drives the **real, unmodified** `ReasoningProtocolLiveModelEvaluationHarness.execute` against a throwaway loopback upstream serving the identical response bytes, and compares the resulting terminal payload against the offline-recovered one field-by-field for every semantically meaningful field (`fixtureId, contextProfileId, prompt, promptSha256, requestBody, rawOllamaEnvelope, extractedResponse, parsedVariant, parserExceptionType, parserExceptionClassification, expectedAction, actualAction, representationValid, contentFidelity, primaryClassification`) — independently re-executed this comparison logic (`assertSameSemanticTerminalContent`, itself reusing the same linear `familyFJsonFieldRawToken` scanner) and confirmed it excludes only fields that legitimately differ for a structural reason stated in-repo (`runId`, `endpointIdentifier`, `timeoutMs` — the comparison's own throwaway upstream is necessarily distinct from the campaign's dedicated endpoint identity).

Copied-directory recovery: `offline recovery works after copying the campaign directory elsewhere` (line 3672) independently seeds a transport-without-terminal state in one directory, copies every file (not the directory object) to a second, unrelated directory via `Files.copy`, constructs an entirely fresh `FamilyFCampaignLedger`/`FamilyFOrchestrationDriver` pointed at the copy, and confirms recovery succeeds with zero model calls and the trial correctly resolved — independently confirming recovery depends only on file *content*, never on any in-process state, cache, or original-directory identity.

No duplicate terminal: `no duplicate terminal record is created for a trial already resolved` (line 3708) independently confirms exactly one `terminal.jsonl` line exists for the trial after the first `recoverPendingOfflineClassifications` call, and — critically — that a **second, independently freshly computed** `recover()` call no longer includes the now-resolved trial in `pendingOfflineClassification` at all (`assertFalse(trial.id in secondState.pendingOfflineClassification)`), so a second real caller has no way to reach the trial a second time; re-driving recovery with that correctly-empty set is confirmed to remain a no-op and still exactly one terminal line exists.

```text
OFFLINE_RECOVERY=CONFIRMED — rereads from disk after reconstructing ledger/driver state; verifies trial identity, request byte-for-byte, transport completeness, and (via the read-time decode) byte counts/hashes/header structure; uses the frozen schedule; produces zero model calls; matches normal-path terminal output field-for-field on every semantically meaningful field; works after copying the campaign directory; never produces a duplicate terminal
PRODUCTION_FORMATTER_AND_PARSER=CONFIRMED — recoverTerminalPayload uses only the unmodified src/ DefaultReasoningPromptBuilder, defaultOllamaRequestBody, defaultOllamaResponseBody, and TaggedReasoningResponseParser; no duplicated protocol logic
ZERO_MODEL_CALL_RECOVERY=CONFIRMED — structurally proven via throwing model-caller stubs across three independent tests, not merely asserted
```

### 3.10 — Fail-closed handling of corrupt, missing, or malformed evidence

Six dedicated tests independently confirm fail-closed behavior on: a missing transport record entirely (`reading a missing transport record fails closed`, line 3394); a correctly-chained record missing the `requestBodyBase64` field this correction requires (line 3401); invalid Base64 content (line 3415); a byte count that disagrees with the actual decoded length (line 3432); a SHA-256 that disagrees with the actual decoded bytes (line 3451); and a structurally malformed `responseHeadersJson` value (line 3470). Every one of these independently exercises a distinct `throw FamilyFArtifactIntegrityException(...)` site inside `familyFDecodeTransportRecord`/`familyFDecodeHeaders`, none of which existed before this correction (the prior implementation had no decode path to corrupt in the first place). No silent recovery, default substitution, or best-effort parsing occurs anywhere in the new decode path.

```text
FAIL_CLOSED_EVIDENCE=CONFIRMED — missing record, missing field, invalid Base64, wrong byte count, wrong hash, and malformed header structure each independently exercised and each fails closed with FamilyFArtifactIntegrityException
```

### 3.11 — Linear field scanner safety for long Base64 content

The prior `familyFExtractStringField` used a single backtracking regex capture group (`(?:\\.|[^\"])*`) around the entire field value — a pattern independently known to risk `StackOverflowError` in the JVM's regex engine on long inputs (a well-documented characteristic of this exact alternation-under-a-star shape, arising from character-by-character backtracking recursion). Because durable transport records now carry Base64-encoded full request/response bodies that can be many kilobytes long (an entire model prompt or an entire model response), this risk is no longer hypothetical. The correction replaces it with `familyFExtractStringField` (line 417), `familyFJsonFieldRawToken` (line 455), and `familyFDecodeQuotedField` (line 495) — each a single left-to-right character-index scan with no recursion and no backtracking, independently confirmed by direct reading to scale linearly (`O(n)`, one loop, `index` monotonically advancing) regardless of field length. The two remaining regex uses on a long line (`responseCaptured`'s literal `(true|false)` alternation, and the header-entry pattern's simple bounded character-class quantifiers `[A-Za-z0-9+/=]*`/`[^\\]]*`) are independently confirmed to be of a fundamentally different, non-catastrophic shape (no nested alternation-under-star over the same content), so they were correctly left as regexes rather than needing the same rewrite.

Independently confirmed empirically, not merely by inspection: the `all 392 fake-driven calls contain durable complete transport evidence` test (line 3747) and the adversarial-byte round-trip test (line 3324) both exercise `familyFExtractStringField` against real, non-trivial-length Base64-encoded field values as part of a passing test run, with no stack-depth failure of any kind across a fresh, forced JVM test execution (Section 4).

```text
LONG_PAYLOAD_SCANNER=CONFIRMED — the three field-extraction functions used against Base64-carrying lines are single-pass, non-recursive, linear-scan implementations; the two retained regexes on long lines use only non-catastrophic bounded-quantifier shapes
```

### 3.12 — Single-pass unescape fix

Independently re-derived the defect the prior sequential-`.replace()` implementation contained (this review's own analysis, not merely restated from the diff's comment): given genuinely double-escaped input content — a literal backslash character immediately followed by a literal `n` character (two raw source characters), which `familyFJsonEscape` correctly serializes as three output characters (`\`, `\`, `n`) — the prior code's replace order (`\"` → `"`, then `\n` → newline, then `\r` → CR, then `\t` → tab, then finally `\\` → `\`) would match the substring `\n` (backslash followed by literal `n`) starting at the *second* character of that three-character sequence, converting the second backslash and the `n` into an actual newline character and leaving the first backslash orphaned — corrupting the recovered content from "backslash + letter n" (2 characters) into "backslash + newline" (2 characters, but semantically wrong). This is a genuine, previously-latent decoding defect in a code path (`payload` field extraction at the former line 713–715) used by `readObservationsForRole` for **every** subject/control role report, for any terminal payload whose serialized content happens to contain a literal backslash immediately followed by `n`, `r`, `t`, or `"` — plausible in real model output (e.g., regex-like text, Windows-style paths, or simply coincidental adjacency), not merely a contrived edge case.

The corrected `familyFDecodeQuotedField` (line 495), via its own single left-to-right character scan, processes each character exactly once: on encountering a backslash, it consumes exactly the following character as part of one atomic escape decision (matching one of the same five cases `familyFJsonEscape` itself produces: `"`, `\`, `n`, `r`, `t`) and advances the index by two, so a genuine `\\` (escaped-backslash) sequence is always resolved as a single literal backslash *before* the scanner ever considers the character that follows it as a possible new escape — the exact ordering the prior sequential-replace implementation could not guarantee. Independently verified this reasoning against the actual character-by-character logic at lines 495–517 rather than accepting it from the diff's own explanatory comment.

Unrelated serialization behavior is unaffected: `familyFDecodeQuotedField` is a strict superset fix used only at the one call site that previously used the buggy sequential-replace pattern (`readObservationsForRole`'s `payload` extraction); every other field extraction in the file already used (and continues to use) `familyFExtractStringField`, which was never affected by this specific ordering bug (its own single-pass scanner, itself corrected in the same diff for the identical class of ordering hazard — see Section 3.11) and behaves identically to before for every field that does not contain this specific adjacent-escape pattern, independently confirmed by the full, unmodified-elsewhere 112/112 passing offline test suite (Section 4), including every pre-existing test that exercises `readObservationsForRole`/`decodeObservation` against plain, non-adversarial terminal payloads.

```text
UNESCAPE_FIX=CONFIRMED — the prior sequential-.replace() ordering could corrupt a genuine escaped-backslash-followed-by-n/r/t/quote sequence; the corrected single left-to-right scan resolves each escape atomically and cannot exhibit this corruption; unrelated call sites and unrelated field content are unaffected, confirmed by the full passing offline suite
```

## 4. Test execution — focused offline classes

```text
./gradlew reasoningProtocolFamilyFDiagnostic --rerun-tasks -> BUILD SUCCESSFUL (33s), 4 actionable tasks: 4 executed
build/test-results/reasoningProtocolFamilyFDiagnostic/TEST-...FamilyFDiagnosticOrchestrationTest.xml: tests="112" skipped="0" failures="0" errors="0"
build/test-results/reasoningProtocolFamilyFDiagnostic/TEST-...FamilyFDiagnosticTest.xml:               tests="21"  skipped="1" failures="0" errors="0"
```

`grep -c '@Test' tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt` independently confirms exactly 112 test methods, matching the executed count exactly (91 pre-existing + 21 newly added by this correction, each independently confirmed present and each newly-added one distinct from any pre-existing name). The one skipped test in the unchanged sibling file is the pre-existing double-gate live-trigger test, correctly self-skipped because `PARKER_REASONING_FAMILY_F_EXECUTION_APPROVED` is genuinely absent from this shell's environment (independently confirmed: `echo $PARKER_REASONING_FAMILY_F_EXECUTION_APPROVED` → unset). Zero model contact occurred; zero failures; zero errors across both files.

## 5. Full ordinary Gradle suite and lifecycle dry-run checks

```text
FULL_GRADLE_TEST=./gradlew test --rerun-tasks -> BUILD SUCCESSFUL (55s), 8 actionable tasks: 8 executed
check --dry-run    -> grep -i familyf across full output: no match
build --dry-run    -> grep -i familyf: no match
assemble --dry-run -> grep -i familyf: no match
gradle tasks --group=verification -> reasoningProtocolFamilyFDiagnostic listed as its own opt-in entry, alongside reasoningProtocolBaselineCharacterisation, reasoningProtocolUnit2DDiagnostic, reasoningProtocolLiveModelEvaluation, and unit3cControlledRemedyExperiments, distinct from `test`/`check`
```

`DETACHED_TASK_AND_DOUBLE_GATE=CONFIRMED` by direct Gradle task-graph resolution — this correction touches no Gradle configuration at all (`build.gradle.kts` is unchanged, confirmed absent from the one-file diff), so the double-gate and lifecycle-isolation guarantees the accepted Implementation Independent Constitutional Review already independently verified remain structurally identical and are independently re-confirmed operative here.

## 6. Regression — everything outside the correction's scope

Independently re-read the full one-file diff end to end (not merely the sections touching raw capture) and independently confirmed the following pre-existing mechanisms are **untouched by any line of this diff** (absent from `git diff d1a3dd9e` entirely) and remain exactly as the accepted implementation and its accepted reviews left them:

```text
- FamilyFChainedLedger.append (the universal envelope/hash-chain mechanism itself) and familyFAppendForced (StandardOpenOption.SYNC forced durability)
- the rejection matrix (malformed/truncated JSON, wrong schema version, wrong campaign ID, missing/skipped/duplicate/reordered sequence, payload mutation, record deletion, cross-file substitution)
- verify-before-recovery / re-verify-before-sealing (verifyAllChainsFromDisk at the start of recover(), and recover() called again inside sealAfterAdvancementRecorded)
- residency-block protocol, artifact-size-aware pre-load memory gate, per-call memory/disk gates
- FamilyFProductionIsolationGuard (production PID/endpoint protection, read-only checks only)
- FamilyFAdvancementGate (subject-only, structurally two-parameter, no control/ranking field)
- FamilyFRoleReportBuilder (both subject and control reporting, requireComplete for both)
- manifest (SHA256SUMS.txt) as a distinct layer from chain verification
- requireFamilyFDownstreamIsolated() and the forbidden-symbol list (unchanged; independently re-grepped, zero occurrences of any forbidden symbol in the corrected file)
- the detached Gradle task and double execution gate (build.gradle.kts is unchanged)
```

Every pre-existing test exercising these mechanisms is present, unmodified in intent, and passing (Section 4) — the only pre-existing test bodies this diff touches are eight call sites that replaced the literal placeholder strings `"req-hash"`/`"resp-hash"` with `familyFSha256Bytes(ByteArray(0))`, independently confirmed necessary (not gratuitous) because `recordTransport`'s new write-time hash re-verification (Section 3.1) now correctly rejects a hash that does not match the actual bytes, and the placeholder strings never did; `grep -n '"req-hash"\|"resp-hash"'` across both Family F files independently confirms zero remaining occurrences, so no test silently continues to exercise the old, unverified code path under a stale literal.

```text
REGRESSION_GATES=CONFIRMED INTACT — subject/control reporting, resource gates, residency protocol, production isolation, exact-once/no-retry (now additionally covering the transport-persistence-failure case), sealing, manifest layering, detached task, and double gate are all unchanged in mechanism and independently confirmed still passing
```

## 7. Model contact and worktree isolation

```text
MODEL_CONTACT=NONE — PARKER_REASONING_FAMILY_F_EXECUTION_APPROVED independently confirmed absent from this shell's environment before, during, and after every command run in this review; the one live-trigger test remained correctly skipped; every HTTP exchange exercised by any test in this review targets only a test-local com.sun.net.httpserver.HttpServer loopback fake, never a real model endpoint
PRIMARY_WORKTREE_PRESERVED=YES — independently confirmed via git -C /home/steve/parker-platform status --porcelain (one untracked file, the pre-existing Family F Alternative Diagnostic Host Requirements Scope Lock, unchanged) and git -C /home/steve/parker-platform rev-parse HEAD (938667919f445d05d654624c1a3d3c01a17613eb, unchanged) — this review performed only read-only checks against the primary worktree and modified nothing there
```

## 8. `git diff --check`

```text
DIFF_CHECK=CLEAN — git diff --check (baseline..working tree) for the one changed file: no output
```

## 9. Correction of the prior implementation reviews' raw-capture claims

This Completion Review, together with its own Independent Constitutional Review, **explicitly supersedes** the following specific claims of the accepted Unit 3-BF Family F Diagnostic Implementation Completion Review and its accepted Independent Constitutional Review — and no other claim in either document:

```text
SUPERSEDED_CLAIM_1=Implementation Completion Review Section 9, "TRANSPARENT_CAPTURE=CONFIRMED" — accurate about in-memory proxy transparency and pre-release ordering, but did not disclose that the durable ledger persisted only hashes, not bytes or headers. SUPERSEDED BY: this review's Sections 3.1-3.6 — the durable ledger now persists complete raw bytes and headers, independently verified durably-before-release, and TRANSPARENT_CAPTURE now accurately means both in-memory transparency AND on-disk durability.
SUPERSEDED_CLAIM_2=Implementation Completion Review Section 8, "EXACT_ONCE_AND_NO_RETRY=CONFIRMED", specifically its clause "a transport-captured-but-unclassified trial is resumable and classifiable offline without a new call" — this specific clause was inaccurate as of that review (no classification function existed). SUPERSEDED BY: this review's Section 3.9 — a genuine, fail-closed, zero-model-call offline classification function now exists, is exercised end-to-end, and the "resumable and classifiable offline without a new call" property is now independently confirmed true, not merely detected-but-unfulfilled.
NOT_SUPERSEDED=every other claim in the Implementation Completion Review and its Independent Constitutional Review (three-file scope, ledger-integrity/hash-canonicalization correction, detached task and double gate, resource/residency/exact-once mechanics apart from the one clause above, production-path fidelity, Knowledge Discoverability isolation, no ranking/substitution/qualification-credit leakage, citation accuracy) remains accurate and undisturbed by this correction and this review.
```

The accepted Defect Confirmation Review and its accepted Independent Constitutional Review (`docs/reviews/...RAW_TRANSPORT_CAPTURE_DEFECT_CONFIRMATION_REVIEW*.md`) already correctly identified and scoped exactly these two claims as requiring correction (their own Section 4.10/4.13); this review confirms the correction those documents anticipated has now been correctly and completely implemented, and formally performs the superseding those documents deferred.

## 10. Verdict

```text
COMPLETION_FINDINGS=0 (P0=0, P1=0, P2=0, P3=0)
ONE_FILE_SCOPE=CONFIRMED
RAW_REQUEST_DURABILITY=CONFIRMED
RAW_RESPONSE_DURABILITY=CONFIRMED
STATUS_AND_HEADERS=CONFIRMED
BYTE_ENCODING_AND_HASHES=CONFIRMED
DURABLE_BEFORE_RELEASE=CONFIRMED
PERSISTENCE_FAILURE=CONFIRMED
FORWARDING_OUTCOME_BOUNDARY=CONFIRMED
OFFLINE_RECOVERY=CONFIRMED
PRODUCTION_FORMATTER_AND_PARSER=CONFIRMED
ZERO_MODEL_CALL_RECOVERY=CONFIRMED
EXACT_ONCE_AND_NO_RETRY=CONFIRMED (now including the transport-persistence-failure case)
LONG_PAYLOAD_AND_ESCAPE_HANDLING=CONFIRMED
ALL_392_TRANSPORT_RECORDS=CONFIRMED
REGRESSION_GATES=CONFIRMED INTACT
PRIMARY_WORKTREE_PRESERVED=YES
MODEL_CONTACT=NONE
OFFLINE_TESTS=133 total across both files (112+21), 132 executed, 1 correctly self-skipped (live trigger), 0 failures, 0 errors
FULL_GRADLE_TEST=BUILD SUCCESSFUL, ordinary suite unaffected
DIFF_CHECK=CLEAN
VERDICT=ACCEPTED
NEXT_LAWFUL_ACTION=this Completion Review's own accepted Independent Constitutional Review; thereafter, per the Plan's Section 22 sequence, a Readiness Review re-verification and its Independent Constitutional Review before any future Explicit Execution Approval. This review authorizes none of those steps, no model contact, no campaign, and no Knowledge Discoverability activity.
```
