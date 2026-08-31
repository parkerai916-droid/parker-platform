# ORDINARY-INGESTION-10R6 — V8 frozen request digest reconciliation

## Verdict

UNIT ORDINARY-INGESTION-10R6 COMPLETE — V8 SPRINT 2 PRE-TRANSPORT DIGEST DIVERGENCE HAS BEEN TRACED TO A PRECISE REQUEST-CONSTRUCTION OR DIGEST-DOMAIN BOUNDARY AND CORRECTED PROVIDER-FREE; A SINGLE CANONICAL V8 REQUEST BUILDER NOW PRODUCES REPEATABLE FROZEN ACCEPTANCE VALUES FOR B AND C; NO FIDELITY FAILURE, PROVIDER RETRY, PRODUCTION PROMOTION, OR PRODUCTION MUTATION OCCURRED

Fixture B was a pre-transport frozen-fixture reproducibility failure, not a provider-fidelity failure. R5 made no B attempt and received no B response.

## Baseline and identity

- Starting HEAD/upstream: `14c02154f137a3ea3e1e5374440e23aea7dfaa65`; clean.
- R5 harness commit `78a08f8e65d92b59fc0e9700a017973ef2eb476e` is an ancestor and was verified.
- R5 report SHA-256: `d164cff9db79ad03910d986bbdfb15cf19e67d4051cae6df7982bed581d93d52`.
- R4 implementation: `18b81a6d7834cb19e1ad884dbcc40a22289af288`.
- Capability ID/digest: `ordinary-external-request-region-transcription-v8` / `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`.
- R4 and R5 capability identity: equal. Profile `request-region-fidelity-acquisition-v4`; schema `request-region-transcription-schema-v4`; wire 8; adapter/parser `7.0.0`/`3.0.0`; processing `external-transcription.deterministic-complete-set-request-region-v4`; instruction digest `effa7ff9f29c41a4eff31e79664c33f74ba399b1a883ef138fbd866a0034fe55`; schema digest `0e625f26b6f977482a73bcf2de929afd9dbd4f4acc8c246f14c1a53df3cf9cd9`; OpenAI / `gpt-5.6-sol` / `/v1/responses`; reasoning none; store false; render identity `authoritative-page-region-raster-v1` version 1 at 300 DPI; shaping `deterministic-complete-set-request-region-v4`; maximum 32 regions and 20 MiB body.
- Sprint 2 source: `evidence-4c6f2ee8-2f62-47be-bd7a-946c744b2766`, SHA-256 `ce8bd4b53d8b007026575974014e71f648f045bf3970b0e984605cf842a7b4a5`, 124,027 bytes, `application/pdf`, 3 pages. R4 and R5 used identical bytes, MIME, and page count.

## Exact historical values and root cause

| State | Correlation ID | Request digest | Provider-body digest | Body bytes |
|---|---|---|---|---:|
| R4 | `oi10r4-sprint2-offline` | `a5ef4672e07493ae870cc798fe24a87651ffc007446e40716f48ea5e62356d03` | `8bf1c47066c6c13bd2a72757712ceebe5497b3929153b0c5befc0a56c362690a` | 1,165,499 |
| R5 | `oi10r5-v8-b-acceptance` | `d87de41366052eff66b45ede428536e56dd54df8a9efcfdbab4dce41a6858768` | `826119f3e767c5da008f9237c2111f21013254e8d8553f22b48ba26e66ce6f7e` | 1,165,499 |

The first divergent stage is the governed `correlation_id` supplied while constructing `RequestRegionV8Request`. It is present in canonical binding field `correlation_id` and in the provider input text suffix `request_correlation_id=...`. The two identifiers have equal encoded length, explaining identical body lengths. No timestamp, UUID, build commit, environment path, iteration-order difference, JSON nondeterminism, schema difference, instruction difference, image difference, or base64 difference was involved.

Applicable root-cause classes are **B/E/Q**: the R5 harness supplied a different request-construction input; a dynamic acceptance-unit identifier participated in both governed digest domains; and R5 compared a digest for that different input against R4's frozen digest. R4 did not document the wrong hash. Both historical hashes correctly represent their respective inputs, but only R4 represents the mandated frozen Sprint 2 request.

## Digest domains

- **Request digest**: SHA-256 of UTF-8 canonical `RegionJson` binding in `OpenAiRequestRegionV8Codec.canonicalBinding`; domain `parker.request-region-v8.canonical-binding.v1`. It includes correlation ID, capability constants, and ordered target metadata, but not base64 image bytes directly (it binds each encoded image SHA).
- **Body digest**: SHA-256 of the exact UTF-8 serialized provider request body from `OpenAiRequestRegionV8Codec.buildRequestBody`; domain `parker.request-region-v8.provider-body.utf8.v1`. It includes instruction, correlation suffix, ordered manifests, base64 PNGs, provider controls, and schema.
- **Manifest digest**: SHA-256 of the canonical persisted fixture-manifest record/envelope when explicitly named as such. It is not the provider-body digest.
- Historical **body/manifest digest**: R4/R5 terminology for the provider-body digest. It did not hash the persisted fixture manifest. R6 retains the historical label only when quoting old records and otherwise calls it provider-body digest.

## Stage-by-stage digest ladder

| Stage | R4 versus R5 | Forensic result |
|---|---|---|
| 1. PDF bytes | Equal | Exact SHA and length above |
| 2–3. rendered page bytes/pixel digests | Equal | Same canonical builder path; repeated pixel-digest ladder equal |
| 4–6. source manifest/crops/identities | Equal | Same 36 ordered source identities and crop-derived identities |
| 7–10. request grouping/IDs/memberships/crops | Equal | `16/14/6 → 14/12/6`; identical 32 IDs; identical ordered 36/36 membership |
| 11. image/base64 payloads | Equal | All ordered encoded-image SHA values equal; same standard unwrapped Base64 path |
| 12. request manifest | Equal | Geometry, request IDs, crop digests, and order equal |
| 13. instruction bytes | Equal | Digest `effa7ff9…e55` |
| 14. schema bytes | Equal | Digest `0e625f26…9cd9` |
| 15. provider object | **First difference** | Correlation suffix only |
| 16. serialized body | Different | Digests `8bf1c470…690a` versus `826119f3…6f7e`; equal length |
| 17. canonical request binding | Different | Correlation field only; request digests above |
| 18. historical “body/manifest” | Different | Same body domain, different governed correlation input |

The executable R4 path rendered via `DeterministicSourcePageRenderer`, derived with `DeterministicSourceRegionDeriver`, shaped with `DeterministicCompleteSetRequestRegionShaper`, constructed `RequestRegionV8Request("oi10r4-sprint2-offline", ...)`, and used `OpenAiRequestRegionV8Codec`. R5 used those same executable components but independently synthesized `oi10r5-v8-b-acceptance`.

## Correction and semantic decision

`CanonicalRequestRegionV8Builder` now owns render → derive → shape → V8 request → provider body → domain-specific digests. The R4 offline proof and R5/R6 acceptance harness use it. The acceptance fixture carries an explicit frozen correlation ID; B uses the exact R4 value. The duplicated hand-built orchestration is bypassed. Eventual production V8 execution can consume this same builder; V8 is not currently production-routed.

- Capability semantics changed: **NO**.
- Capability digest changed: **NO**.
- Schema, instruction, geometry, coalescing, source order, provenance, provider controls, limits, codec, and provider bytes for canonical B changed: **NO**.
- R4 values canonical: **YES**, for the frozen correlation/input mandated by acceptance.
- R5 values canonical: **YES for their distinct correlation input, NO as a reproduction of the R4 frozen request**.
- Fixture A remains applicable: **YES**. Its correlation ID remains `oi10r5-v8-a-acceptance`; the correction neither changes nor reinterprets its exact provider request, capability, implementation, persisted response, or R5 authority. Its provider-free readability/replay path remains covered. Reuse in a live continuation must explicitly bind the immutable R5 A authority/evidence rather than recreate A.

## Corrected provider-free freezes

Freeze root: `/home/steve/parker-acceptance/oi10r6-v8-canonical-freeze`.

### B

- Source/request regions: 36 → 32; page shaping `16/14/6 → 14/12/6`.
- Constituent coverage: 36/36; missing 0; duplicates 0; four coalesced regions.
- Request digest: `a5ef4672e07493ae870cc798fe24a87651ffc007446e40716f48ea5e62356d03`.
- Provider-body digest: `8bf1c47066c6c13bd2a72757712ceebe5497b3929153b0c5befc0a56c362690a`.
- Body length: 1,165,499 bytes.
- Frozen manifest SHA-256: `bc28fe491b3126d95eb43cb2f4fd1f0e7ea16818d86439889e220e9d11ad3093`.

### C

- Evidence/SHA: `evidence-0275472f-535a-4cf1-b30d-f45ac7684743` / `7373ad403b4fae5bf5c777deb8524eaa3ba38594ce9fabfa8fcbce22fbd33182`.
- Source/request regions: 24 → 24; complete ordered coverage.
- Request digest: `8b49d1ced865302956d57ef2a07f7e522764ac56de4e981dcc72edd40f257f6a`.
- Provider-body digest: `15d85cf3ddb82f52364e9245cb9208d93b2ff2b17c45eb34290100536d7d3444`.
- Body length: 1,477,537 bytes.
- Frozen manifest SHA-256: `9ebbc21ef3ed1ebdbc4b298523800a86873d99979f601cdc5b995d2a7991b42f`.

No provider attempt or response exists in the R6 freeze root.

## Verification and preservation

- Same-process determinism: 10 repetitions, byte-identical.
- Separate-process determinism: 3 Gradle/JVM executions, each performing 10 constructions; all passed (30 additional constructions).
- Targeted V8/canonical-builder/historical replay tests: PASS; 6 tests, 0 failures, 2 expected skips when unrelated response paths were absent.
- Full suite with Sprint 2 source supplied: 245 suites, 3,285 tests, 0 failures, 0 errors, 15 skipped.
- `git diff --check`: PASS.
- OpenAI calls: 0. Claude calls: 0.
- Production mutation: none. Counts before/after remained attempts 4, provider-state 2, authorities 1, generations 21, content 19, capability acceptances 6, owner authorizations 5.
- Production image `sha256:fdb583d16d99a58d13983046b2ad8b936014ead6b6c22cdf0d670b895b071521`; running; restart count 0.
- Implementation/test commit: `1e88eb0d8c1b1a3d70aca88a1ed2a9d419847f9a`.

## Exact next live step

Run a separate governed acceptance-continuation unit. It must verify the immutable R5 A response and replay without a provider call, bind B and C to the R6 freeze root and the exact digests above, permit at most one B call followed only on complete fidelity success by at most one C call, use zero retries, persist attempt before transport and raw response before parsing, and perform no deployment or production mutation. R6 itself authorizes no provider call.
