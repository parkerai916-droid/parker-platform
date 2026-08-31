# ORDINARY-INGESTION-10R7 — Request-region V8 fidelity acceptance continuation

## Verdict

UNIT ORDINARY-INGESTION-10R7 COMPLETE — REQUEST-REGION V8 GOVERNED FIDELITY ACCEPTANCE PASSED ACROSS IMMUTABLE SINGLETON A, SPRINT 2 COALESCED B, AND SOURCE-ORDER-SENSITIVE C; CANONICAL LITERAL FIDELITY, REGION-SCOPED UNCERTAINTY, COMPLETE CONSTITUENT PROVENANCE, PARKER-AUTHORITATIVE SOURCE ORDER, DURABLE RESPONSE PERSISTENCE, AND PROVIDER-FREE REPLAY VERIFIED; V8 IS ELIGIBLE FOR SEPARATE PRODUCTION PROMOTION

## Baseline, capability, and authority continuation

- Starting HEAD/upstream: `2b6000bab4938e4c3ff4f5b6605e14f60777f816`; clean.
- R6 implementation/test commit: `1e88eb0d8c1b1a3d70aca88a1ed2a9d419847f9a` verified.
- R6 report SHA-256: `a6894c0e84c5c46eac459f1448821cb2aa67fa2cd59ab65e8dffc5619f6023ba` verified.
- Capability: `ordinary-external-request-region-transcription-v8`; digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`; implementation `18b81a6d7834cb19e1ad884dbcc40a22289af288`; lifecycle before `ACCEPTANCE_PENDING`.
- Contract: profile `request-region-fidelity-acquisition-v4`; schema `request-region-transcription-schema-v4`, wire 8; adapter/parser `7.0.0`/`3.0.0`; processing `external-transcription.deterministic-complete-set-request-region-v4`; reasoning none; store false.
- Authority continuation: VALID. Immutable A authority SHA `b5c98a4dba5631f23cc6b564170beacb5dba4ca0bdb0905beb1cc32163b114da`; corrected R6 B/C authority SHAs `c54ba8b613cd44ec731af7b7468b09884c6b87c6d383ab12878b5e8ac59be663` and `d7f1b126e7e736b510e17f95b3b8d220d84c3e66fe6362090c6fb6f82464f278`. They preserve the OI10R5 per-fixture authority IDs, capability/build, maximum 3 calls, maximum one call per fixture, and zero retries. A had consumed one; B/C had consumed zero.

## Immutable fixture A

- A was not called again.
- Attempt-record identity: SHA `21bfcc77a8d9b4a1b0b21aab3cec8911eaa3827bbee3fe8f3535f2842dbbf401` (the historical schema has no separate attempt-ID field).
- Provider response: `resp_0b9b3eb5ed486050016a955c66c73487d0a82abfa450461ad0`.
- Response/assessment/raw hashes: `6c273baea8c116046c78fe38f8b754522c172e5c473c318b1659bcc627843ca3` / `5c947b4aeb6c7ab5e039ebaa7e74987cdc20f85a190c77c56dc5f9f59874b284` / `8dbbacbd3a01cc9974c438134abb71dd429c86112d5adb46300b08274f53b35f`.
- Exact capability/authority applicability and provider-free replay: PASS.

## Fixture B — frozen input and provider evidence

- Evidence SHA: `ce8bd4b53d8b007026575974014e71f648f045bf3970b0e984605cf842a7b4a5`.
- Request digest: `a5ef4672e07493ae870cc798fe24a87651ffc007446e40716f48ea5e62356d03`.
- Provider-body digest/size: `8bf1c47066c6c13bd2a72757712ceebe5497b3929153b0c5befc0a56c362690a` / 1,165,499 bytes.
- Geometry: 36→32; pages `16/14/6 → 14/12/6`; coverage 36/36; missing 0; duplicates 0.
- Provider response: `resp_06ed7a0af4c06ee8016a956540ac7487d08914aedab704271a`.
- Attempt/response/assessment hashes: `5f2d6b072f3b5b2e0febba831679de4fd7b92e028ecea60548b485104c467790` / `8326de5fbc6fbeb45b92dc4f0a56b0ab92515527643e7415ab6a853d92462aeb` / `8241e01bdc93c80932793460de7f612bdcb1c004aa0e9fc7728d38623b50e64f`.
- Raw/structured hashes: `55f6a7ac424a14ac8f389ba199d72b769af58efad0ce6449d7a05fcf90a31c5c` / `c19b8bcc7385877778782cf902784a609f5e6747182d10828ca3cf924587b156`.
- Fidelity review SHA: `e50ed3978c61998a5cbdbddbe6b3389d00affee1ba4c4af73cba18a08b1e8767`.

### B 32-region fidelity table

The ID column is the leading portion of the full persisted request-region ID; the governed files contain every full ID and literal.

| # | Page | ID | Kind | Returned literal / classification |
|---:|---:|---|---|---|
| 1 | 1 | `4fd757b2` | singleton | “Perfect. This is exactly what we wanted to see.” — PASS |
| 2 | 1 | `63f8d87b` | singleton | Git-history sentence — PASS |
| 3 | 1 | `fa7e8fbf` | singleton | “Current repository state” — PASS |
| 4 | 1 | `ee01c2dd` | singleton | Five-commit block — PASS; truthful `CLIPPED` disclosure at visible right edge |
| 5 | 1 | `218f09a0` | singleton | Six checked conclusions — PASS |
| 6 | 1 | `3c05a6b1` | singleton | no visible text / null — PASS |
| 7 | 1 | `2aa303dd` | **coalesced** | heading plus milestone sentence — PASS |
| 8 | 1 | `003232e2` | singleton | “Foundation (complete)” — PASS |
| 9 | 1 | `0ef9a051` | singleton | Constitution — PASS |
| 10 | 1 | `795d5922` | singleton | Architecture — PASS |
| 11 | 1 | `d832884f` | singleton | Engineering Standard; Runtime architecture — PASS |
| 12 | 1 | `00e67d23` | singleton | Runtime contracts — PASS |
| 13 | 1 | `4c084761` | singleton | Runtime implementation; Module architecture — PASS |
| 14 | 1 | `80e9ce22` | **coalesced** | Module contracts; Module registry runtime — PASS |
| 15 | 2 | `a6024ecc` | singleton | Communication architecture — PASS |
| 16 | 2 | `e1cb7c56` | singleton | Communication contracts — PASS |
| 17 | 2 | `879ed187` | singleton | versioned-and-tagged sentence — PASS |
| 18 | 2 | `8539e325` | singleton | “Runtime (implemented)” — PASS |
| 19 | 2 | `ca567f3d` | singleton | Identity; Trust — PASS |
| 20 | 2 | `9e64d2b5` | **coalesced** | six registry/runtime bullets — PASS |
| 21 | 2 | `c03236c0` | singleton | “with 441/441 tests passing.” — PASS |
| 22 | 2 | `4b4736eb` | singleton | no visible text / null — PASS |
| 23 | 2 | `11783b0a` | singleton | “What comes next” — PASS |
| 24 | 2 | `82ce2a12` | singleton | observable-abilities sentence — PASS |
| 25 | 2 | `e293f7e2` | singleton | Communication Runtime next-unit sentence — PASS |
| 26 | 2 | `f9ab1981` | **coalesced** | forward execution-flow diagram — PASS |
| 27 | 3 | `c606f852` | singleton | return-flow continuation — PASS |
| 28 | 3 | `653d457e` | singleton | “Notice something important.” — PASS |
| 29 | 3 | `454d31ae` | singleton | brain/mouth/ears sentence — PASS |
| 30 | 3 | `237a10f9` | singleton | communication-channel paragraph — PASS |
| 31 | 3 | `d8f5481f` | singleton | architecture/backend paragraph — PASS |
| 32 | 3 | `1315fe73` | singleton | platform-transition closing paragraph — PASS |

B totals: literal PASS 32, FAIL 0; coalesced PASS 4/4; omissions 0; insertions 0; substitutions 0; semantic rewrites 0; uncertainty failures 0; provenance failures 0. Provider-free replay reproduced canonical digest `3822efc72b707f643c9039f8291b6e928aed288b6901c1695eca563943889114`: PASS.

## Fixture C — source-order-sensitive acceptance

- Evidence: `evidence-0275472f-535a-4cf1-b30d-f45ac7684743`; SHA `7373ad403b4fae5bf5c777deb8524eaa3ba38594ce9fabfa8fcbce22fbd33182`.
- Three pages; 24 source regions → 24 request regions with complete ordered membership.
- Request digest: `8b49d1ced865302956d57ef2a07f7e522764ac56de4e981dcc72edd40f257f6a`.
- Provider-body digest/size: `15d85cf3ddb82f52364e9245cb9208d93b2ff2b17c45eb34290100536d7d3444` / 1,477,537 bytes.
- Provider response: `resp_0b1b1e3dc0a05931016a9565b8a2fc87d0ad3ebabc7f07b0b7`.
- Attempt/response/assessment hashes: `d75cf4d61746264bed5fa958c177c67acefcbefc11dd6c6a5ad6302c9eff9799` / `02ff8dd51f18f855cc16334080120d662dd34c3972399a64f36096221708143e` / `2cb3fc1991e228a3e751e28f91520270267f6fb352da5f850e4f1c58e37bf2f3`.
- Raw/structured hashes: `b1ad9979352720e4df68d1e451457ffa3837db645065aa1d5e47736dfd4d0eed` / `b7747cfbeaa1474beb0a041db1e3c3624c5fd20ddef234f1f44e1e5f2e05f310`.
- Fidelity review SHA: `6bfb68250c69d179ab098992e6e4d7f88da2f58f841220005b66a18159b774cf`.
- Literal PASS 24, FAIL 0; uncertainty failures 0; provenance failures 0.

Five source-order witness groups passed with zero errors: the page-1 governance chain, page-2 proposition placement, page-2/3 authorization block placement, page-3 closing prose, and all adjacent request-region transitions. Parker reconstructed by frozen request order, not provider ordinal. Replay reproduced canonical digest `f9f63b3894d9c145a7e5824971cc47b629c54c36fc116410ccdd433062787431`: PASS.

## Final replay, discrepancies, and typed evidence

- Final A/B/C provider-free replay: PASS/PASS/PASS.
- Discrepancy inventory: no material omissions, insertions, substitutions, paraphrases, semantic rewrites, binding failures, provenance failures, or source-order errors. One truthful B `CLIPPED` qualification; no other uncertainties or warnings.
- Typed evidence ID: `acceptance-evidence-ordinary-ingestion-10r7-v8-fidelity`.
- Typed evidence SHA-256: `34ec3c703aacb754c45fa58ddf941d7368e2b4cc2e373cb412eb99c4de30902b`.
- Cumulative calls: 3 (A historical + B/C new); new calls in R7: 2; retries 0; Claude calls 0.
- Result: `FIDELITY_ACCEPTANCE_PASSED`; eligible for a separate production-promotion unit. No production acceptance or routing was changed.

## Verification and production preservation

- Continuation harness commits: `eb84653b41b260103c6e50cf3efb69082ce4ff58`, `9fb35f2b923c45af26099f28d6e17783aaeced3a`.
- Targeted continuation/V8 tests: PASS.
- Full suite: 245 suites, 3,285 tests, 0 failures, 0 errors, 15 skipped.
- `git diff --check`: PASS.
- Production counts before/after: attempts 4/4; provider-state 2/2; authorities 1/1; generations 21/21; content 19/19; capability acceptances 6/6; owner authorizations 5/5.
- Production image: `sha256:fdb583d16d99a58d13983046b2ad8b936014ead6b6c22cdf0d670b895b071521`; running; restart count 0.
- Production routing changed: NO. Sprint 2 production state changed: NO.

## Exact next step

If desired, authorize a separate production-promotion unit that consumes the typed evidence above, re-verifies the exact capability/build and production gates, and independently decides whether to promote V8. This acceptance unit does not authorize deployment, routing, production capability acceptance, owner authorization, or Sprint 2 execution.
