# OI11R4G — V8 Provider-Response Provenance and Validation Failure Investigation

## 1. Starting state

Host `parker`; repository `/home/steve/parker-platform`; HEAD/upstream `2afd7ce2c32686c8468f3ebdb47ccbb91ff32f28`; clean worktree. Production remains running on image `sha256:e161c65c98c0572cb0981be652adfa029406e0104f843b1359a70b0031398673`, restart count `0`. Provider profile, capability acceptance, six legacy records, and the single V8 acceptance record are unchanged. Provider calls/retries/egress remain `0 / 0 / 0`.

## 2. OI11R4F evidence

Execution `ordinary-exec-adaafa06-15b3-42c1-8edf-4ef753996ce9`, request digest `51cfa64e8764f035ee94904f1d9f2217012b8431e7886295909262f5353c7638`, and raw response digest `4ef964fa9b3f1259517853c1e33e2efda964bcf1268f4e17a34450f580f62033` are preserved. The provider-state record is `b843244d7b907955bd26ff54abd26099fe1fd68e6722435021379e96531099b3`; no derivative was admitted.

## 3. Raw envelope and structured payload

Offline inspection of the persisted raw JSON proved:

- outer `id`: `resp_08a1e0a340cb4182016a9696f3a87087d0950a52f0a85b8be5`
- outer `model`: `gpt-5.6-sol`
- status: `completed`; HTTP status: `200`; output contained one `output_text` item
- structured payload contained `provider_provenance` with keys present, but `provider_response_id: null` and `provider_reported_model: null`; `requested_model` was `gpt-5.6-sol`
- structured payload contained 14 blocks, matching the prepared request cardinality

Thus the fields were present-and-null in the model-generated payload, not absent. The non-null identities existed only in the outer provider envelope.

## 4. Request and adapter boundary

`OpenAiRequestRegionV8Codec` builds the Responses request with model `gpt-5.6-sol`, strict schema `request-region-transcription-schema-v4`, and instruction directing transcription only. The V8 schema permits nullable `provider_reported_model` and `provider_response_id`; the instruction does not ask the model to manufacture an API response ID or envelope model.

`OpenAiRequestRegionV8ProviderExchange` calls the transport, persists the raw response first, parses the envelope, extracts `responseId` and `model`, parses the model-generated structured text, validates it, and then requires `valid.result.providerProvenance.providerResponseId == responseId` and `providerReportedModel == prepared.capability.model`. It never injects the authoritative envelope values into the parsed provider-neutral provenance object before validation.

## 5. Provenance semantics and deterministic replay

`provider_response_id` is authoritative provider-envelope metadata. `provider_reported_model` is likewise authoritative only when supplied by the provider envelope; the requested model is Parker configuration identity. The model-generated structured payload is not an independent authority for either field. Replaying the preserved response through the local parser/validator yields the same structured nulls and the same failed equality requirement, therefore `V8_PARSE_OR_VALIDATION_FAILED`, without network access.

The equality check is a useful fail-closed consistency check, but in the current ordering it compares envelope facts with nullable model echo fields that the request never requires the model to know. It is not a meaningful independent provenance proof and is impossible to satisfy for this valid response shape without adapter enrichment.

## 6. Root cause

Primary classification: **C — Adapter mapping defect**.

The adapter discards the already extracted outer response ID/model when constructing the validated provider-neutral result. The validator correctly rejects the resulting null/mismatch rather than admitting an unverifiable derivative. No provider defect is proven; the outer response was successful and internally identified.

## 7. Smallest remediation

At the adapter boundary, after parsing the structured payload and before V8 validation, inject the outer envelope `responseId` and `model` into the provider-neutral provenance fields (or construct the provenance object from those authoritative values). Keep raw persistence before parsing. Retain fail-closed checks that the envelope model equals the configured requested model and that the response ID is valid. Persist the enriched parsed state only as assessment/structured state; never alter the preserved raw response.

This is an implementation correction: the existing V8 request schema, capability identity/digest, provider-neutral acceptance record, and historical V5 semantics need not change. Existing failed OI11R4F raw/state records remain immutable and are not retroactively admitted.

## 8. Retry preconditions and boundary

Before any retry: implement the bounded adapter correction in a separate unit; add focused tests for envelope-to-provenance enrichment, null model payloads, model mismatch, and response-ID validation; run the full suite; build/accept/deploy a new artifact if required by source change; preserve the failed attempt; and authorize a new execution unit with a fresh one-call budget. No retry is authorized here.

