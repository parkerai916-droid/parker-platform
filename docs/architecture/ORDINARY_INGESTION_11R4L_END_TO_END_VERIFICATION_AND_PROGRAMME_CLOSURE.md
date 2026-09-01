# OI11R4L — End-to-End Verification and OI11R4 Programme Closure

## Executive verdict

**B — COMPLETE WITH NON-BLOCKING OBSERVATIONS.** All accepted governance and execution objectives are proven. The synthetic output intentionally reports blank/obscured regions as `_` and `NO_VISIBLE_TEXT`; this is a fidelity observation, not a structural or provenance failure. No real evidence was processed.

## Chronology

| Unit | Result | Provider calls | Mutation / blocker |
|---|---|---:|---|
| OI11R4 Scope Lock | Accepted/frozen | 0 | Governance only |
| OI11R4A | Failed post-egress | 1 | Null structured provenance; preserved |
| OI11R4B | Investigation | 0 | Missing V8 execution authority |
| OI11R4C | Complete | 0 | Promotion codec corrected; production untouched |
| OI11R4D | Complete | 0 | Corrected C304 artifact deployed |
| OI11R4E | Complete | 0 | V8 capability authority created |
| OI11R4F | Failed post-egress | 1 | Historical provenance validation failure; immutable |
| OI11R4G | Complete | 0 | Adapter mapping defect proven |
| OI11R4H | Complete | 0 | D335 enrichment correction deployed |
| OI11R4I | Stopped pre-egress | 0 | Running artifact/authority mismatch |
| OI11R4J | Complete | 0 | D335-bound capability acceptance created |
| OI11R4K | Complete | 1 | Third live synthetic transaction admitted |

## Successful execution proof

OI11R4K used evidence `evidence-84d85f99-3a94-4101-86b2-8b8aa9aef0ae`, SHA-256 `f9ce327166b00c28d0ce50334bad256c03f2ff9c74e6b045592f53f8dce03a89`, size 1,406 bytes, two pages. Fresh authorization was `ordinary-v8-auth-25231fbd4e26fb458534fe82e864d1baacd6a9eb85d7bbe45c0755d73e7b09a7`; execution was `ordinary-exec-f866b9c8-8b27-426b-908b-3b5c11ad17db`; request digest was `4846340d021be8db9ab89f45494a66359dfba617dbc1294b1434f06f9b1c3943`.

The attempt ledger sequence was `AUTHORISED → PREFLIGHT_PASSED → SOURCE_RETRIEVED → REQUEST_PREPARED → PROVIDER_ATTEMPT_STARTED → PROVIDER_RESPONSE_RECEIVED → GENERATION_ADMITTED → TERMINAL_SUCCESS`. This proves provider receipt preceded parsing/enrichment/validation and derivative admission. Raw response SHA-256 was `662fa88d23c0138dcd2d252f8b9a7d8fd71160ebc3698106ecb04e6dece08f18`; provider response ID was `resp_03874b0a56357f61016a96a4cde48087d08e4f23bfb38efffa`; reported model was `gpt-5.6-sol`.

The assessment contained 14 structured blocks, all authorized regions exactly once. V8 validation and provenance validation passed; there were no missing, duplicate, or extra regions. Parker ordering remained authoritative and provider-returned ordinals remained forensic-only. The D335 adapter enriched provider-neutral provenance from the outer envelope.

Derivative generation was `region-dbca2cddb982083a5a54acea321e364c61f724a9f5609007520547ad4e312983`. Its persisted content binds source/digest, execution/request, capability/digest, provider/profile/model, raw provider state, parsed response, request regions, reconstruction and processing provenance. Readable synthetic text and page markers matched; the intentionally obscured region was represented as `NO_VISIBLE_TEXT` with its governed warning.

## Fail-closed and historical verification

OI11R4B, I, and the earlier artifact/profile/authority gates demonstrate closed rejection before transport where prerequisites were absent or mismatched. OI11R4A/F demonstrate post-egress validation failure without derivative admission or retry. Historical raw responses, failure records, C304 acceptance, six legacy records, and OI11R4I state remain immutable. No failure was rewritten as success.

## Capability, artifact, and production integrity

The V8 capability identity/digest and D335-bound acceptance remain exact; unrelated implementation bindings remain rejected. Production runs source/build `d33518e85604083d620be08be4a4f001d7be3187`, image `sha256:5ca82f03ce61eade58c60eb4d3783547b4b266f974ed2ac218c09cf43f86075a`, with restart count `0` and readiness passing. The provider profile and Authorization Purpose were unchanged.

Before OI11R4K: evidence 28, manifests 28, capability acceptances 8, owner authorizations 8, attempts 6, provider state 4, derivative generations 21, derivative content 19. After: evidence 28, manifests 28, capability acceptances 8, owner authorizations 11, attempts 8, provider state 6, derivative generations 22, derivative content 20. These are exactly the governed authorization event, attempt, raw receipt/assessment, and derivative mutations.

Provider totals across live units: OI11R4F OpenAI 1 (post-egress failure), OI11R4I OpenAI 0 (pre-egress), OI11R4K OpenAI 1 (success); retries 0 throughout, Claude 0, other external 0. OI11R4L itself made 0 calls.

## Closure boundary

OI11R4 is closed for the synthetic governed V8 objective. Parker is ready for governed planning of the first bounded real-document ingestion, but this closure authorizes no real-document execution. Before that execution, a new scope/authorization unit must specify the selected real source, purpose, human scrutiny, call budget, provenance and review requirements.
