# OI11R4K — Third Governed Live V8 Synthetic Execution

## Disposition

PASS. The frozen synthetic source was processed through the corrected D335 production path with one governed OpenAI call and no retry.

## Starting state and authorization

- Host/repository: `parker`, `/home/steve/parker-platform`
- Starting HEAD/upstream: `59e06a306f9805f799bc3af0c28e82ad8cdbd047`
- Production container: `c3b739bdec37e594b63fe35bad43c4be545f75568db7b431b81b747e1b3c85b6`, restart count `0`
- Image: `sha256:5ca82f03ce61eade58c60eb4d3783547b4b266f974ed2ac218c09cf43f86075a`
- Source/build: `d33518e85604083d620be08be4a4f001d7be3187`
- Capability: `ordinary-external-request-region-transcription-v8`, digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`
- Provider/profile/model: OpenAI / `openai-fidelity-first-transcription-v1` / `gpt-5.6-sol`
- Purpose: `evidence-intelligence.external-transcription`
- Frozen evidence: `evidence-84d85f99-3a94-4101-86b2-8b8aa9aef0ae`, SHA-256 `f9ce327166b00c28d0ce50334bad256c03f2ff9c74e6b045592f53f8dce03a89`, 1,406 bytes, 2 pages

Fresh authorization ID: `ordinary-v8-auth-25231fbd4e26fb458534fe82e864d1baacd6a9eb85d7bbe45c0755d73e7b09a7`.

Fresh execution ID: `ordinary-exec-f866b9c8-8b27-426b-908b-3b5c11ad17db`.

Request digest: `4846340d021be8db9ab89f45494a66359dfba617dbc1294b1434f06f9b1c3943`.

## Governed transaction

The attempt ledger records `AUTHORISED`, `PREFLIGHT_PASSED`, `SOURCE_RETRIEVED`, `REQUEST_PREPARED`, `PROVIDER_ATTEMPT_STARTED`, `PROVIDER_RESPONSE_RECEIVED`, `GENERATION_ADMITTED`, and `TERMINAL_SUCCESS`. Exactly one transport occurred. No retry or fallback path occurred.

The provider response was durably persisted before parse/validation. Provider-state record digest: `5f57957980f3b522fbf8f0bd5f1f6e7b8a305204a93103386137939ff781aeb0`; assessment digest: `46c96b07494356bab68fc77460094cfa0566d64bc6fef9f671b32776ef4e05d6`. Assessment outcome: `SUCCESS`; raw response SHA-256: `662fa88d23c0138dcd2d252f8b9a7d8fd71160ebc3698106ecb04e6dece08f18`.

Authoritative outer provider response ID was `resp_03874b0a56357f61016a96a4cde48087d08e4f23bfb38efffa`; outer model was `gpt-5.6-sol`. The structured payload contained 14 blocks. The corrected adapter enriched/verified provider provenance from the outer envelope; no conflicting non-null metadata was present. V8 parsing and validation passed with all 14 authorized regions, no duplicates, missing regions, or extras. Parker request-region ordering remained authoritative and provider ordinals remained forensic metadata.

## Derivative and fidelity

Derivative generation ID: `region-dbca2cddb982083a5a54acea321e364c61f724a9f5609007520547ad4e312983`.

The derivative binds the frozen source, capability/digest, execution, request, raw provider state, OpenAI/provider model, adapter/parser metadata, and all 14 request regions. Text output matched the known synthetic page markers and readable sentences. The intentionally blank/obscured regions were represented as `_`/`NO_VISIBLE_TEXT` with the governed warning; no unsupported text was admitted. Structural and provenance correctness passed; fidelity was recorded without normalization.

## Store accounting and integrity

Before execution: evidence `28`, manifests `28`, capability acceptances `8`, owner authorizations `8`, attempts `6`, provider state `4`, derivative generations `21`, derivative content `19`.

After execution: evidence `28`, manifests `28`, capability acceptances `8`, owner authorizations `11`, attempts `8`, provider state `6`, derivative generations `22`, derivative content `20`. The deltas are the fresh authorization event/record, attempt, provider receipt/assessment, and one derivative generation/content. Existing evidence/manifests, capability records, six legacy records, OI11R4F, and OI11R4I history were not rewritten.

Production remained running on the exact D335 image with restart count `0`; readiness and capability disposition remained passing. Provider accounting: OpenAI `1`, retries `0`, Claude `0`, other external `0`.

The next boundary is separately governed work only; no real evidence is authorized by this synthetic execution.
