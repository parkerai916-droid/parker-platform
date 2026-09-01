# Ordinary Ingestion OI11R4 — First Live V8 Execution Scope Lock

**Status: Accepted and Frozen — OI11R4**  
**No live execution is authorized by this document.**

**Owner acceptance:** Steven explicitly accepted this document and SHA-256
`770f7d35521f3945cf09132ddaf8ee89b5a57cc1329bc577ce5db1015ab3a9db` on
2026-09-01. Acceptance authorizes governance finalization only; it does not
authorize the live provider transaction.

## 1. Status and purpose

OI11R4 freezes the bounded conditions for the first live V8 external transcription. It is a governance and evidence-plan unit only. During this unit production remains running on the accepted artifact, no provider request is made, and no evidence leaves Parker.

The subsequent execution is a synthetic source-fidelity and governed-derivative test, not case analysis, legal reasoning, benchmarking, or authorization for unrestricted provider use.

## 2. Authoritative baseline

Host `parker`; repository `/home/steve/parker-platform`; branch `main`; source/runtime baseline `d43d8ae3b7316b10d729b1cf962b404477604e90`. Production is the OI11R3-verified artifact `sha256:9268d5d1685f6760cc6daea7fb40000c437584ec2721156a40143266530a3ec7`, with source identity `854910c4cb695f2b74db2b1b4d0779e7b58676c6`. The accepted provider profile is `openai-fidelity-first-transcription-v1` (SHA-256 `3038538d53b98595631c76325062688b40c449d512bb94cae17be2e7f0d6e956`), and the capability is `ordinary-external-request-region-transcription-v8` (digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`).

Production was observed running with restart count 0. Store counts are `4 / 2 / 1 / 21 / 19 / 6 / 5`; these are the pre-execution baseline and must be recaptured immediately before the live unit. OI11R4 itself produced zero provider calls/retries and no store mutation.

## 3. Scope and explicit non-scope

In scope is exactly one owner-authorized synthetic transaction through Parker's existing V8 guarded workflow, followed by evidence inspection and human comparison. Out of scope are Michael's, Steve's, Uber's, ERA's, Privacy Act, or any other real case material; V5 execution; batches; performance tests; provider connectivity tests; direct HTTP/curl/scripts; fallback providers; model substitution; retries; and any production code or configuration change.

## 4. Synthetic test artifact

The next unit shall create one deterministic two-page PDF locally from the existing controlled fixture procedure (PDFBox, LETTER pages, Helvetica 12, fixed coordinates). The exact text is:

**Page 1**

`SYNTHETIC_PAGE_ONE_MARKER`  
`Synthetic Person Alpha and Synthetic Person Beta met on 14 September 2026.`  
`The synthetic amount recorded was $123.45; punctuation: commas, colons, and semicolons.`  
`For avoidance of doubt, this synthetic clause creates no rights, duties, or legal obligations.`

**Page 2**

`SYNTHETIC_PAGE_TWO_MARKER`  
`This is ordinary readable synthetic text on page two.`  
`This lower-contrast sentence remains intentionally readable.`  
`The region below is deliberately obscured and must not be guessed:`  
then a fixed black rectangle, followed by  
`Clear text resumes after the obscured region.`

These are synthetic labels and statements, not real people or case facts. The source PDF, its SHA-256, byte size, two-page count, source/evidence record identity, manifest, and creation time must be recorded before execution. The original remains authoritative; any transcription is derivative.

## 5. Authorization Purpose

The existing accepted purpose is `evidence-intelligence.external-transcription`, with action `evidence-intelligence.transcribe-external`, registered by `ParkerRuntime` and evaluated through the Permission Engine for the Evidence Intelligence invocation resource. The existing authorization scope lock requires an explicit purpose, fail-closed unknown/missing/retired-purpose handling, and owner-selected source material. This purpose is lawful for the synthetic controlled test. `CONTROLLED_LIVE_FIDELITY_ACCEPTANCE` is a separate region-acceptance purpose and is not substituted here. No new purpose or Gap #54 is created.

The next execution must use the existing `ExternalTranscriptionInvocationGate.buildExecutionRequest`; it must not construct an ad hoc request. Owner authorization must identify the synthetic source and this purpose before the call.

## 6. Capability and provider/profile lock

Only capability `ordinary-external-request-region-transcription-v8` with digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0` is permitted. The provider is OpenAI, profile `openai-fidelity-first-transcription-v1`, profile SHA-256 `3038538d53b98595631c76325062688b40c449d512bb94cae17be2e7f0d6e956`. The accepted non-secret snapshot is `POST /v1/responses`, model `gpt-5.6-sol`, `reasoningEffort=none`, `store=false`, PDF detail `high`, image detail `original`, and the accepted instruction/schema digests. The configured destination is `https://api.openai.com/v1/responses`; no Claude or local-model fallback is allowed. If OpenAI does not expose an exact model snapshot in its response, record the provider-neutral `NotExposed` value rather than inventing one. Credentials are checked only by Parker's existing readiness mechanism and never recorded.

## 7. Request-region plan

The source has exactly two pages. Parker must render canonical page representations, derive regions with `pixel-whitespace-source-regions-v1` (version 1), and build the request through `CanonicalRequestRegionV8Builder` and `OpenAiRequestRegionV8Codec`. Region count and identities are not hand-authored: the next unit must record the deterministic derived manifest and digest before transport. It must contain one unique Parker region identity per derived region, page numbers only 1 or 2, exact page/region bindings, authoritative Parker order, no duplicates or unknown IDs, at most 32 regions total, and a body no larger than 16,777,216 bytes. The same frozen manifest must be present in the outgoing request and the returned validation evidence. If derivation is ambiguous or exceeds bounds, abort before provider transport.

## 8. Provider call budget and retry lock

One governed V8 transaction means one prepared request passed through `OrdinaryRequestRegionV8IngestionWorkflow` and one `OpenAiResponsesExternalTranscriptionAdapter` transport execution. The maximum HTTP/provider-call budget is **1**. The coordinator has no retry or fallback loop; automatic retries are locked to **0**. The adapter uses one JDK `HttpClient` request with redirects disabled. Any library/infrastructure retry discovered in preflight would violate this lock and must block execution rather than be learned by trial.

## 9. Raw-before-parse and validation evidence

`OpenAiRequestRegionV8ProviderExchange` must durably persist the non-empty raw response before parsing or validation, including request/body/manifest digests, HTTP status, content type, raw length, and raw SHA-256. Only after that receipt may parsing, `RequestRegionV8StructuredValidator`, and assessment persistence occur. Evidence must include the provider-state record, receipt/assessment IDs, event ordering, and outcome. Raw bytes are Parker-controlled evidence and must not be copied to external systems.

Validation must require the V8 envelope, correlation, profile/schema/version, provider provenance, exact block cardinality, known unique region IDs, and page matches. Missing, duplicate, unknown, malformed, or invalid regions fail closed. A provider-returned ordinal is retained for forensic evidence only; it never controls reconstruction.

## 10. Parker-authoritative reconstruction and fidelity

`RequestRegionV8DerivativeBinder` must iterate the prepared request's Parker-authoritative region order. The derivative records both Parker order and provider order, but `provider_returned_ordinal` is forensic-only and is excluded from the canonical reconstructed-content digest. The known source text above must be compared with returned region/page text without silently normalizing meaningful punctuation, numbers, omissions, additions, substitutions, or uncertainty. Steven must review the original and derivative side by side; an automatic score cannot substitute for first-transaction human review. Quality differences are recorded separately from governance/path correctness.

## 11. Derivative provenance and audit

The resulting derivative must retain source artifact ID/SHA-256, page and region IDs/crop digests, capability ID/digest, provider profile, provider/model and snapshot status, processing identity, request/correlation/request digest, execution and attempt IDs, owner authorization and capability-acceptance record IDs, raw provider-state record and raw digest, parsed-result digest, Parker order, provider order, reconstruction and generation digests, timestamps, and uncertainty metadata. Audit evidence must reconstruct source, authorizer, purpose, capability, profile, regions, destination, call count, retry count, raw receipt, validation, reconstruction, and derivative location. Generated transcription is never promoted to original evidence.

## 12. Expected store mutations and baseline

Immediately before execution, recapture the seven programme counts and identify each store by its configured root/type. The first transaction may create only the records inherent to this path: the synthetic source/manifest (if not already registered), one V8 owner-authorization reservation, one attempt/ledger, one raw provider-state receipt and one assessment, and one admitted derivative generation/content plus its audit/provenance records. Existing capability acceptance and profile state are read, not rewritten. No memory or knowledge mutation, evidence replacement, duplicate attempt, second provider state, or unrelated record is expected. Exact per-root before/after counts and record IDs must be captured; any delta outside this bounded set is failure evidence.

## 13. Network and egress evidence

The execution unit must correlate the authorization/attempt ledger with application logs and host/container network observation. It must prove destination `api.openai.com:443` (the `/v1/responses` operation), exactly one provider call, zero retries, zero Claude calls, zero other external destinations, and no payload sent anywhere else. Headers, tokens, credentials, and unnecessary sensitive payload metadata must be redacted. OI11R4 itself has zero calls and zero egress.

## 14. Failure and abort criteria

Stop after the single authorized attempt for authorization or purpose denial, capability/profile/build mismatch, missing or incomplete acceptance, credential/readiness failure, source/manifest/region mismatch, body/region bounds failure, any retry or call-budget violation, HTTP/provider error, raw persistence failure, parse/validation failure, unknown/duplicate/missing region, reconstruction/provenance/admission failure, unexpected store mutation, or any destination other than the locked OpenAI endpoint. Do not fix and retry in the first-live unit. Preserve evidence and define remediation separately.

## 15. Success criteria and human review

Success requires the complete chain: controlled source preserved; exact purpose/capability/profile authorization; one canonical request; one call and no retry; raw-before-parse receipt; valid V8 response; Parker-order derivative and provenance; exact audit trail; bounded expected mutations; human comparison completed; and zero unrelated egress. A plausible transcription through an unauthorized path is failure. Minor fidelity differences may be recorded as quality findings without masking governance failure.

## 16. Exact next execution unit

`OI11R4A — First Governed Live V8 Synthetic Execution` shall perform exactly the preparation, owner authorization, single transport, evidence capture, validation, reconstruction, and human review frozen here, then stop. It must not use real evidence or expand the provider budget. This scope lock is now **Accepted and Frozen — OI11R4**; no live execution may begin except in OI11R4A under these terms.
