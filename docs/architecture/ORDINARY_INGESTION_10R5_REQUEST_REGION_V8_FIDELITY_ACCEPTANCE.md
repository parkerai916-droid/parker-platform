# ORDINARY-INGESTION-10R5 — Request-region V8 fidelity acceptance

## Verdict

UNIT ORDINARY-INGESTION-10R5 COMPLETE — REQUEST-REGION V8 FIDELITY ACCEPTANCE FAILED AT A PRECISE GOVERNED FIXTURE/REQUEST-REGION BOUNDARY; STRICT LITERAL, UNCERTAINTY, PROVENANCE, SOURCE-ORDER, AND PERSISTENCE VALIDATION REMAIN IN FORCE; PROVIDER EVIDENCE PRESERVED; NO RETRY OR PRODUCTION PROMOTION PERFORMED

The unit stopped before fixture B transport. Fixture B had been frozen before call A with request digest `d87de41366052eff66b45ede428536e56dd54df8a9efcfdbab4dce41a6858768` and body digest `826119f3e767c5da008f9237c2111f21013254e8d8553f22b48ba26e66ce6f7e`. The governed Sprint 2 boundary requires the R4 digests `a5ef4672e07493ae870cc798fe24a87651ffc007446e40716f48ea5e62356d03` and `8bf1c47066c6c13bd2a72757712ceebe5497b3929153b0c5befc0a56c362690a`. The mismatch was caused by the acceptance harness assigning an R5-specific correlation ID; correlation ID is included in both canonical request binding and provider body. Changing the already-frozen B request after observing A would violate the authority, so no correction or continuation was performed.

## Frozen authority and capability

- Baseline/report commit: `42b7dc0038d3757ee92ad66f81f938d54cc400e3`
- V8 implementation commit: `18b81a6d7834cb19e1ad884dbcc40a22289af288`
- Acceptance harness commit: `78a08f8e65d92b59fc0e9700a017973ef2eb476e`
- Capability: `ordinary-external-request-region-transcription-v8`
- Capability digest: `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`
- Lifecycle: `ACCEPTANCE_PENDING` (unchanged)
- Provider/model/endpoint: OpenAI / `gpt-5.6-sol` / `/v1/responses`
- Reasoning/store/retries: `none` / `false` / `0`
- Profile: `request-region-fidelity-acquisition-v4`
- Schema/wire: `request-region-transcription-schema-v4` / `8`
- Adapter/parser: `7.0.0` / `3.0.0`
- Processing: `external-transcription.deterministic-complete-set-request-region-v4`
- Instruction digest: `effa7ff9f29c41a4eff31e79664c33f74ba399b1a883ef138fbd866a0034fe55`
- Schema digest: `0e625f26b6f977482a73bcf2de929afd9dbd4f4acc8c246f14c1a53df3cf9cd9`
- Isolated acceptance root: `/home/steve/parker-acceptance/oi10r5-v8-authoritative`

All three fixture sources, derived crops, manifests, request ordering, geometry, memberships, request digests, body digests, and per-fixture authorities were frozen before call A. No fixture was replaced or regenerated afterward.

## Fixture results

### A — singleton control

- Evidence: `evidence-28c076ae-0666-45ef-a04e-297d74a639c9`
- Source SHA-256: `64039b3200b34c67ce1f993c1cb1f98a115390d6aa25541ceb8a60e674441149`
- Request digest: `2ec26ecb4c3dc17e9ff38808360d07c42f5e92b4cbe74168a7e377beee17510c`
- Authority/manifest hashes: `b5c98a4dba5631f23cc6b564170beacb5dba4ca0bdb0905beb1cc32163b114da` / `8cc7ca3c143236ac90f771c93d8ae4cd019c5e8a7f7e0fac5439b826ecd084a4`
- Attempt/response/assessment hashes: `21bfcc77a8d9b4a1b0b21aab3cec8911eaa3827bbee3fe8f3535f2842dbbf401` / `6c273baea8c116046c78fe38f8b754522c172e5c473c318b1659bcc627843ca3` / `5c947b4aeb6c7ab5e039ebaa7e74987cdc20f85a190c77c56dc5f9f59874b284`
- Provider response: `resp_0b9b3eb5ed486050016a955c66c73487d0a82abfa450461ad0`; raw SHA-256 `8dbbacbd3a01cc9974c438134abb71dd429c86112d5adb46300b08274f53b35f`
- Result: PASS. Exact literal text matched the authoritative crop, including punctuation, diacritics, leading zeroes, and footer. Literal failures `0`; uncertainty failures `0`; omissions `0`; insertions `0`; substitutions `0`.
- Persistence: attempt-start existed before transport; raw response existed before parse/validation; provider-free replay reproduced canonical digest `1556953425bef579e45b2fb234fa3288a50e32b1a75b3b6c932b36523da1e162`.

### B — Sprint 2 complete/coalesced set

- Evidence: `evidence-4c6f2ee8-2f62-47be-bd7a-946c744b2766`
- Source SHA-256: `ce8bd4b53d8b007026575974014e71f648f045bf3970b0e984605cf842a7b4a5`
- Derivation: 36 source regions (`16/14/6`) to 32 request regions (`14/12/6`), with all 36 constituents covered exactly once and four coalesced request regions.
- Frozen authority/manifest hashes: `e3ab45d562d707b396e1b358eace4e5cbb27242cf4a001fc4e07d3d54fd8` / `f58ec2d446e3a1435c7e70db7569e3b9e322927e4940bc60a3784de08e407302`
- Result: STOPPED PRE-TRANSPORT at exact digest verification. Attempt files `0`; response files `0`; provider calls `0` for B.

### C — source-order-sensitive control

- Evidence: `evidence-0275472f-535a-4cf1-b30d-f45ac7684743`
- Source SHA-256: `7373ad403b4fae5bf5c777deb8524eaa3ba38594ce9fabfa8fcbce22fbd33182`
- Frozen authority/manifest hashes: `60ecd74d81729b9a2ae71df83f991515450e8b653652c2937514b2c3301fc405` / `9ebbc21ef3ed1ebdbc4b298523800a86873d99979f601cdc5b995d2a7991b42f`
- Result: NOT ATTEMPTED after the B stop. Attempt files `0`; response files `0`; provider calls `0` for C.

## Call, mutation, and verification accounting

- Provider calls: `1` total (A only); maximum `3`; retries `0`.
- A replay: PASS, provider-free.
- B: fail-closed before attempt persistence or transport.
- C: not attempted.
- Typed capability-acceptance evidence: not created because the three-fixture acceptance did not pass.
- Deployment/promotion: none.
- Production stores: not written or mutated.
- Production runtime: image `sha256:fdb583d16d99a58d13983046b2ad8b936014ead6b6c22cdf0d670b895b071521`, running, restart count `0`, unchanged by this unit.
- Targeted V8/harness verification: PASS.
- Full offline suite after stop: 245 suites, 3,284 tests, 0 failures, 0 errors, 16 skipped.
- Final repository state before this report: HEAD/upstream `78a08f8e65d92b59fc0e9700a017973ef2eb476e`, clean.

V8 remains `ACCEPTANCE_PENDING` and is not eligible for production promotion from this unit.
