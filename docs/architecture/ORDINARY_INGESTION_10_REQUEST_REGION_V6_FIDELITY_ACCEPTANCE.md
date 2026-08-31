# ORDINARY-INGESTION-10 request-region v6 governed fidelity acceptance

## Determination

The governed acceptance failed at fixture A's visual-observation span boundary. The single provider
response transcribed the visible raster text exactly, but returned internally invalid observation
offsets: after the first paragraph break the ranges were misaligned, and the final observation was
`[273,274)` against a 272-code-point literal. The strict v6 validator therefore returned
`MALFORMED`. The raw response and failure assessment were durably preserved before offline analysis.
No retry was made and fixtures B, C, and D were not called.

**UNIT ORDINARY-INGESTION-10 COMPLETE — REQUEST-REGION V6 FIDELITY ACCEPTANCE FAILED AT A PRECISE GOVERNED FIXTURE/REGION BOUNDARY; PROVIDER OUTPUT AND REVIEW EVIDENCE PRESERVED; NO RETRY, PRODUCTION PROMOTION, OR ORDINARY PRODUCTION INGESTION PERFORMED**

## Baseline and capability

Starting repository HEAD and upstream were
`c763b7194e3f909ea574178939adf7a3d056de61`, clean. OI9 implementation commit
`87764757a35a6df8d2491a0fe69608bafee5bca0` exists and remained the exact capability
implementation under acceptance. The OI9 report SHA-256 was
`58a925fb78912c08d35c2f99eaa1ba6d123a0e9ee922d07d06b1248ba757e984`.

The capability was `ordinary-external-request-region-transcription-v6`, lifecycle
`ACCEPTANCE_PENDING`, identity domain `parker.complete-region-set-group.identity.v1`, profile
`request-region-anchored-fidelity-acquisition-v2`, schema
`request-region-anchored-transcription-schema-v2`, wire 6, processing identity
`external-transcription.deterministic-complete-set-request-region-v2`, adapter
`openai-responses-request-region-transcription-adapter` version `5.0.0`, provider/model
OpenAI/`gpt-5.6-sol`, endpoint `https://api.openai.com/v1/responses`, reasoning `none`, image detail
`original`, and `store=false`. The capability digest is recorded in each immutable authority.

## Frozen fixture manifest

All four source/crop manifests and authorities were frozen create-once before egress under the
separate acceptance root `/home/steve/parker-acceptance/oi10-authoritative`. Source truth was reviewed
from authoritative crop pixels only; native PDF text and OCR were not used as truth.

| Fixture | Evidence / SHA-256 | Source → request shape | Request digest | Result |
|---|---|---:|---|---|
| A singleton | `evidence-28c076ae-0666-45ef-a04e-297d74a639c9` / `64039b3200b34c67ce1f993c1cb1f98a115390d6aa25541ceb8a60e674441149` | 1 → 1 | `fc2e0323babfbe86845ad988e6713f9e153bfca8c0f030ca6be2acb7214ecefa` | `FAIL_FIDELITY` at visual-observation validation |
| B Sprint 2 | `evidence-4c6f2ee8-2f62-47be-bd7a-946c744b2766` / `ce8bd4b53d8b007026575974014e71f648f045bf3970b0e984605cf842a7b4a5` | 36 → 32; pages `16/14/6`, quotas `14/12/6` | `b2f50f15d0f80eb66590a160de6e4a910609545214d89f04b67f66c671190968` | frozen and visually reviewed; not called after A failure |
| C source order | `evidence-0275472f-535a-4cf1-b30d-f45ac7684743` / `7373ad403b4fae5bf5c777deb8524eaa3ba38594ce9fabfa8fcbce22fbd33182` | 24 → 24 | `ffc9e3db96e4d6f2d2415c63d48bbeb0fb60ef635616213a58dc2ce96b2b1ef7` | frozen and visually reviewed; not called |
| D mixed structure | `evidence-230b3f36-ff88-4adf-a224-081d2fd8fdee` / `12b6f1b7c18d1ee3623c5dcf8da0d1c9f61b9bd89dde9e86a74e6398157df885` | 40 → 32 | `afad089f046985d001f02c8add6b9bdc5bdc8e696f720fa439d664292af010a5` | frozen and visually reviewed; not called |

Sprint 2 reproduced the exact 1,169,528-byte body and encoded request/manifest digest
`11baaf1818581d3e50c1a6a3069e52407eacb9e06a52d4131d8e484a14af9851`, with 36/36 unique
constituents, zero missing/duplicate members, deterministic page quotas, request identities, geometry,
membership, and order. No Sprint 2 provider output exists, so its coalesced regions have a provider
pass count of 0/32.

## Fixture A review and persisted state

Fixture A visibly contains the raster-only controls `SCANNED SYNTHETIC EVIDENCE`,
`PARKER-FIXTURE-2026-003`, `SCAN-88421`, `Harcourt`, `$8,750.04`, `KJH482`, `09/10/2025`,
`Comfort eligibility removed.`, `Māori control: Kōwhai`, `004209`, and the footer. The provider's
272-code-point literal matched all visible characters and order: no material omission, insertion,
substitution, or literal-order error was found.

The response ID was `resp_0e12772844c8f1b7016a94eb570ec887d0a6f4140a8360802f`. HTTP status was
200. Raw SHA-256 was `8ea216dde619c35de7c164787e878db754f795d94fcdaa28cce4491cd8cc768b`;
structured SHA-256 was `59f37649c8af98e1897cd4a509bd6cfa0744814cf5314bf590e4a3746bc0ef41`.
Create-once record file hashes were:

- authority `e40fbb2afdefef0f5e1c4b513fc822f812f5c23a77455579972870378b83a313`;
- manifest `55af9fdb03a3ac33d1181cd9e25c12ae267af94f1da8d21b06f92fee14072750`;
- attempt `cfef2561f32ea0bca04e989eb51d7b58bb9b498b14a09bff19e541af398a60c5`;
- response `4a9477fcb478255349519e99c11783dd8116e5fb7711f504c02d41eeb9563d55`;
- assessment `a9372e9ae27498fd7819427d28a258250204c539e54cd4ce5e921daac6d0842d`.

The attempt was durable before transport and the raw response was durable before parse. Exact ID,
page, model, provider, profile, schema/wire, adapter/parser, request membership, and literal binding
were present. Provider response provenance left `provider_response_id` null in the structured object;
the decisive earlier boundary was nevertheless malformed visual-observation state. Parker-order
reconstruction was not admitted after validation failure, as required by fail-closed ordering.

Provider-free replay reproduced the same source/request digest, raw digest, structured state,
`MALFORMED` validation result, and `FAIL_FIDELITY` assessment. An in-memory diagnostic removing only
the observation list validated successfully, isolating the defect without changing persisted output.

## Verification and preservation

Before the call, targeted v6 tests passed. The full suite passed with 241 suites, 3,271 tests,
0 failures, 0 errors, and 11 skips; the +1 suite/test/skip over OI9 is the opt-in harness skipped in
ordinary execution. `git diff --check` passed. The acceptance failure replay passed provider-free.

Provider accounting: planned maximum 4; actual attempted 1; HTTP responses received 1; retries 0;
OpenAI calls 1; Claude calls 0. The exact stop was fixture A's invalid visual-observation spans. B, C,
and D consumed zero calls.

Production before/after counts were identical: attempts 4/4, provider-state 2/2, generations 21/21,
content 19/19, capability acceptances 6/6, ordinary owner authorizations 5/5. Production remained
image `sha256:fdb583d16d99a58d13983046b2ad8b936014ead6b6c22cdf0d670b895b071521`, container
`281bba01fa82ddd4a172a424688845ea180a6dfe28eb4ae2aebd9c064ecd68ca`, running with zero restarts.
No production route, Compose file, container, acceptance, authorization, evidence, derivative, or
provider-state store was changed. Sprint 2 ordinary owner authorization/execution remained unchanged.

## Required final fields

1. Starting HEAD/upstream/clean: `c763b7194e3f909ea574178939adf7a3d056de61` / same / yes.
2. OI9 implementation verified: yes, `87764757a35a6df8d2491a0fe69608bafee5bca0`.
3. OI9 report SHA verified: yes, `58a925fb78912c08d35c2f99eaa1ba6d123a0e9ee922d07d06b1248ba757e984`.
4. Capability ID/digest: `ordinary-external-request-region-transcription-v6`; digest in immutable authorities.
5. Implementation commit under acceptance: `87764757a35a6df8d2491a0fe69608bafee5bca0`.
6. Model: `gpt-5.6-sol`.
7. Endpoint: `https://api.openai.com/v1/responses`.
8. Reasoning: `none`.
9. Store setting: `false`.
10. Adapter/version: `openai-responses-request-region-transcription-adapter` / `5.0.0`.
11. Profile: `request-region-anchored-fidelity-acquisition-v2`.
12. Schema/wire: `request-region-anchored-transcription-schema-v2` / 6.
13. Processing identity: `external-transcription.deterministic-complete-set-request-region-v2`.
14. Fixture A identity/shape/result: exact ID/SHA above; 1→1; `FAIL_FIDELITY`.
15. Fixture B identity/shape/result: exact Sprint 2 ID/SHA; 36→32; not called after governed stop.
16. Sprint 2 36→32 verified: YES, provider-free exact reconstruction.
17. Sprint 2 coalesced regions reviewed/pass count: visual sources reviewed 32/32; provider pass 0/32 because not called.
18. Fixture C identity/result: exact ID/SHA above; frozen and visual source reviewed; not called.
19. Fixture D identity/result: exact ID/SHA above; frozen and visual source reviewed; not called.
20. Provider calls planned maximum: 4.
21. Provider calls actual: 1.
22. Retries: 0.
23. Provider response IDs: fixture A ID above; none for B/C/D.
24. Provider-state record IDs/hashes: separate acceptance record hashes above; no production provider-state record.
25. Raw responses persisted before parse: YES.
26. Material omissions: none in A literal; unassessed for uncalled fixtures.
27. Material insertions: none in A literal; unassessed for uncalled fixtures.
28. Material substitutions: none in A literal; unassessed for uncalled fixtures.
29. Material order errors: none in A literal; malformed observation offsets were material structured-order/binding errors.
30. Provenance failures: structured `provider_response_id` was null; exact envelope response ID remained durable.
31. Reconstruction failures: reconstruction correctly did not proceed past rejected validation.
32. Offline replay: PASS for deterministic reproduction of the failure.
33. Overall fidelity: FAIL.
34. Typed acceptance evidence record ID/hash if PASS: not created; PASS precondition was not met.
35. V6 eligible for production promotion: NO.
36. Ordinary production routing changed: NO.
37. Sprint 2 ordinary authorization/execution changed: NO.
38. Production store counts before/after: `4/4, 2/2, 21/21, 19/19, 6/6, 5/5` in required order.
39. OpenAI calls: 1.
40. Claude calls: 0.
41. Report SHA: recorded in the final operator handoff after this file is finalized.
42. Commits: OI9 implementation `87764757...`; OI10 harness `c14c8829...`; report commit recorded in final handoff.
43. Final HEAD/upstream/clean: recorded after report commit/push verification.
44. Exact next step: do not promote v6; govern a separate remedy unit for observation-span fidelity, then repeat acceptance under a fresh authority and call budget.

## Recommendation

Do not promote, deploy, ordinary-route, capability-accept, authorize, or execute v6. The next lawful
step is a separate governed remedy for provider visual-observation span fidelity (including response
provenance completeness), followed by fresh build-bound acceptance. The preserved OI10 response is
forensic evidence and must not be retried or reinterpreted as a pass.
