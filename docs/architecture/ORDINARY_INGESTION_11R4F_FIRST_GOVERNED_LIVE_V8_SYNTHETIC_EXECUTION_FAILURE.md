# OI11R4F — First Governed Live V8 Synthetic Execution Failure

## Disposition

`UNIT ORDINARY-INGESTION-11R4F STOPPED POST-EGRESS`

The exact frozen synthetic evidence was used through Parker’s governed path. Pre-egress authorization, capability, provider profile, model, request preparation, and attempt gates passed. Exactly one OpenAI Responses API call occurred and zero retries occurred. No second call is authorized.

## Source and identity

- Evidence: `evidence-84d85f99-3a94-4101-86b2-8b8aa9aef0ae`
- Source SHA-256: `f9ce327166b00c28d0ce50334bad256c03f2ff9c74e6b045592f53f8dce03a89`
- Size/page count: 1,406 bytes / 2 pages
- Capability: `ordinary-external-request-region-transcription-v8`
- Capability digest: `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`
- Provider profile: `openai-fidelity-first-transcription-v1`
- Model: `gpt-5.6-sol`

## Attempt and persistence

- Execution ID: `ordinary-exec-adaafa06-15b3-42c1-8edf-4ef753996ce9`
- Request digest: `51cfa64e8764f035ee94904f1d9f2217012b8431e7886295909262f5353c7638`
- Provider-state record: `b843244d7b907955bd26ff54abd26099fe1fd68e6722435021379e96531099b3`
- Raw response SHA-256: `4ef964fa9b3f1259517853c1e33e2efda964bcf1268f4e17a34450f580f62033`
- Raw response was durably persisted before parsing/validation.

## Failure

The outer Responses API envelope contained an ID and model, but the provider-generated structured transcription’s `provider_provenance` contained `provider_response_id: null` and `provider_reported_model: null`. Parker’s accepted validator requires those fields to match the outer response identity and requested model. Consequently the outcome was `V8_PARSE_OR_VALIDATION_FAILED`; no derivative was admitted.

The attempt ledger records `PROVIDER_RESPONSE_RECEIVED`. The failed authorization/attempt and raw provider state are preserved. This is a post-egress validation/provenance-contract failure, not a capability or provider-readiness failure.

## Integrity

Production remains on image `sha256:e161c65c98c0572cb0981be652adfa029406e0104f843b1359a70b0031398673`, running with restart count `0`. No real evidence was processed. No derivative, memory, or knowledge record was created. Provider accounting is OpenAI `1`, Claude `0`, other external `0`, retries `0`.

## Next boundary

OI11R4F is not complete. A separately governed remediation must address the V8 response provenance/validation contract before any retry decision. No retry is authorized in this unit.
