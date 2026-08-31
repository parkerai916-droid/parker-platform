# ORDINARY-INGESTION-10R2 request-region v7 fidelity acceptance

## Determination

V7 acceptance **FAILED at Fixture B observation-semantic alignment**. Fixture A passed literal,
structural, coordinate-arithmetic, semantic-anchor, provenance, reconstruction, persistence, and
provider-free replay review. Fixture B preserved a structurally valid 32-block response with complete
36/36 constituent coverage and zero arithmetic-bound failures, but at least 48 of 106 observations
were deterministically semantically misaligned with their own exact `literal_text`. C and D were not
called. There was no retry and no production promotion.

**UNIT ORDINARY-INGESTION-10R2 COMPLETE — REQUEST-REGION V7 FIDELITY ACCEPTANCE FAILED AT A PRECISE GOVERNED FIXTURE/REGION BOUNDARY; STRICT OFFSET, FIDELITY, PROVENANCE, AND SOURCE-ORDER VALIDATION REMAIN IN FORCE; PROVIDER OUTPUT AND REVIEW EVIDENCE PRESERVED; NO RETRY OR PRODUCTION PROMOTION PERFORMED**

## Baseline and exact capability

Starting HEAD/upstream were both `1763e19bf037507c35134d7f31be2f3d3b689d5d`; the worktree was
clean. OI10R1 implementation `6425a2f5ec85f5b57c452d693cef032c0bbeb2ac` and report SHA-256
`3e4c3dd29d7d81345aa499dc237fb40401b901fcc1aeb4843e9b7774bc50f7aa` were verified.

- Capability: `ordinary-external-request-region-transcription-v7`
- Capability digest: `570ab8975a9a10a809f27c0bbe5dfb67beb9770ca26dfe32af17d52990da5acc`
- Lifecycle: `ACCEPTANCE_PENDING`; implementation commit: `6425a2f5ec85f5b57c452d693cef032c0bbeb2ac`
- Harness commits: `8742a045d0922e8bdc4e7bfa2334ed7199ccd31a`,
  `aba779447304d10a66a41ca01411748cb8b09e38`, and provider-free resume correction
  `586fff4ee25aaa9bbec46aeed1049551541e215e`
- Provider/model/endpoint: OpenAI / `gpt-5.6-sol` / `POST https://api.openai.com/v1/responses`
- Reasoning/store: `none` / `false`
- Profile: `request-region-anchored-fidelity-acquisition-v3`
- Schema/wire: `request-region-anchored-transcription-schema-v3` / 7
- Processing: `external-transcription.deterministic-complete-set-request-region-v3`
- Adapter: `openai-responses-request-region-transcription-adapter` / `6.0.0`
- Parser: `openai-request-region-structured-response-parser` / `2.0.0`
- Instruction digest: `d720870fb27408211a89880c09449148c2370bb5cffc9c152bfcdbf2341b8efe`
- Schema digest: `f045ee304e02323643400837e7a19f81eadeff2ecac737983f8bc8466fcf65ab`
- Coordinate identity: `unicode-scalar-code-point-half-open-v1`

Offsets bind to the exact returned `literal_text`, start at zero, count Unicode scalar values/code
points, and use half-open `[start,end)` ranges. UTF-8 bytes, UTF-16 code units, grapheme clusters,
hidden text, and normalized text are excluded. Bounds are `0 <= start <= end <= codePointLength`;
`LINE_BREAK` alone may use `[n,n)`, other anchored observations require non-empty ranges, and
unanchored observations require `null/null`.

## Fresh authority and frozen fixtures

All four fixtures were frozen before call A under
`/home/steve/parker-acceptance/oi10r2-v7-authoritative`. The authorities bind the capability,
implementation, harness head, provider request identities, bounds, maximum four calls, one per
fixture, and zero retries.

| Fixture | Evidence SHA-256 | Pages | Source -> request | Request digest | Manifest record digest | Authority file SHA-256 |
|---|---|---:|---:|---|---|---|
| A | `64039b3200b34c67ce1f993c1cb1f98a115390d6aa25541ceb8a60e674441149` | 1 | 1 -> 1 | `027824bfae7465dbbb97f263750e243ff4ed67ce8b353dbf56a36f931a23ac50` | `95efcbd058d7eb3ae5f3d9928cd7a8663e9b45aa443fb71be52888f26b9b3f14` | `20831100f745ce79509b1a8709d8418b06527086fee03175ee3b365540c1e86e` |
| B | `ce8bd4b53d8b007026575974014e71f648f045bf3970b0e984605cf842a7b4a5` | 3 | 36 -> 32 | `4545601b6d5cc79adfb6f541dd3b9fd17afecc8fc17aa0d4201b6146351e8bdc` | `da2578de9f06a479adb735d744ae4b9e41e62970b792b21719798b117d546538` | `943eeff50131c8bc0000b63e35b723c5b774248231e4e1dd64650cd3188fb743` |
| C | `7373ad403b4fae5bf5c777deb8524eaa3ba38594ce9fabfa8fcbce22fbd33182` | 3 | 24 -> 24 | `e06b674378bf32ee25a54dfed8fd8bf9cb7d996b9b99ee83153c71f16b7b5580` | `acad320efc2064afdae547e0f30c3d8e91421e8c946a2daabd0e9adb6208249b` | `eb20d6bbcd4778653cba8995137338c62bfc678abf4b445ae69a54face3e8837` |
| D | `12b6f1b7c18d1ee3623c5dcf8da0d1c9f61b9bd89dde9e86a74e6398157df885` | 2 | 40 -> 32 | `857e219b0dba88563278d4fbeb3a25bd200a7cc7b3750ec9c8b05498fa79d10d` | `4ff93e2ac30aba06d5dca490e524068a2dec7bc2cb89e2c949ebd07beb6c75e7` | `8dd1076993f72b32fd793ace24b30c94472f1da1f8c53e6dbc982313a4d389ca` |

Sprint 2 source/request counts by page were 16/14/6 and 14/12/6. Membership was complete,
unique, ordered, and covered 36/36 source regions. It contained 28 singleton and four coalesced
request regions. The v7 encoded-body digest was
`93533dfdc6ca8e19bf76959250ad87acca740d4ae29c4c218a480cdb6db96ece`.

## Call accounting, persistence, and replay

Provider maximum was four; actual calls were two; retries were zero. Claude calls were zero.

| Fixture | Response ID | Raw SHA-256 | Attempt file SHA-256 | Response file SHA-256 | Assessment file SHA-256 |
|---|---|---|---|---|---|
| A | `resp_0fbdaff37f9e1b41016a95121dd93087d0bc55dede939c0897` | `067310ff9e0d7fb552d6c9718b894e6f1e31f01416271d176c07e444361cbaf1` | `79b37d68e40fcd1648e6899fdf4805137656e19c13e7b91253be84554f592ef0` | `58189ceb521b244272c8b7a15317d7a15f3b5f2ade6f578d08926b8ac5a73935` | `a712e11839a6318b5a13c980553e42568a63629d984011808c6c734165845545` |
| B | `resp_05955e7f2f0f5abf016a951a9edb6c87d086db3880ad58f808` | `adbb651e6e28312796318aac1e6da34aaa456eb7fc746c4fb05c8d8fc3097cc9` | `66bd06ae42c06a6836851dee149dad5aea7c0edc0416b025ccf5b26d55dd081e` | `276c3372a4a62ca602c62e0bdc008251c5a19f4460d9b3c64b03253a3b1cba33` | `cc755bbbb8f18298a5d7cd772b872870e06fd9c549edc4905c361218848d25b6` |

The attempt existed durably before transport and raw response before parsing: **YES**. A and B
replayed provider-free with identical raw identity, structured state, validation, reconstruction,
request order, and constituent provenance. A initially stopped after raw persistence because the
detached harness required the model-authored nullable `provider_response_id` to equal the envelope ID.
The provider-free correction permits null or the exact envelope ID and adds resume-from-persisted;
it did not alter request/capability semantics and made no retry.

## Fixture results and discrepancy inventory

Fixture A: one singleton request reviewed; literal fidelity PASS; omissions/insertions/substitutions/
order/provenance/reconstruction failures all zero. Its 26 observations had zero arithmetic failures
and zero semantic-alignment failures. Heading ranges resolve to the heading; line-break point anchors
land on exact newline code-point positions. Overall A: **PASS**.

Fixture B: 32/32 blocks structurally reviewed; complete membership and Parker reconstruction passed;
zero arithmetic offset failures. Literal strings were preserved for review, but the fixture cannot
receive a material-fidelity pass because observation metadata is fidelity-critical and systemically
false. At least 48/106 anchors fail deterministic semantic checks. Examples:

- request region 4 has `PARAGRAPH_BREAK [124,125)` resolving to `g`, `[222,223)` to `u`,
  `[312,313)` to `i`, and `[415,416)` to `0`;
- request region 5 has `LIST_MARKER` ranges resolving to newlines, `.`, and `i`;
- request region 20 has `LIST_MARKER` ranges resolving to newline, `y`, and `r`;
- request regions 24, 25, and 29 have bold ranges cutting through words rather than their visually
  emphasized targets;
- numerous `LINE_BREAK [n,n)` points land at ordinary letters rather than newline positions.

Classification: **B semantic target misalignment / G provider ignored explicit contract**. The first
precise failed governed boundary is Sprint 2 request-region ordinal 4,
`ee01c2ddf7a8ea0a44b1963a242578e7cad16749d6f1b8ae6793da21363d3c0a`, in its observation stream.
Sprint 2 overall: **FAIL**. Singleton/coalesced material pass counts are not asserted after this
systemic metadata failure; omissions, insertions, substitutions, and order errors are therefore not
promoted as final zero claims. Fixture C: **NOT CALLED**. Fixture D: **NOT CALLED**.

No typed v7 PASS evidence was created. Overall v7 fidelity: **FAIL**. Eligible for production
promotion: **NO**.

## Verification and production preservation

Targeted harness/offset tests passed after correction. Full suite: 243 suites, 3,279 tests, zero
failures, zero errors, 13 skipped. `git diff --check`: PASS.

Production remained healthy (`running`, restart count 0) on image
`sha256:fdb583d16d99a58d13983046b2ad8b936014ead6b6c22cdf0d670b895b071521`.
Before/after ordinary production counts were identical:

| Store | Before | After |
|---|---:|---:|
| attempts | 4 | 4 |
| provider state | 2 | 2 |
| region acceptance authorities | 1 | 1 |
| derivative generations | 21 | 21 |
| derivative content | 19 | 19 |
| capability acceptances | 6 | 6 |
| owner authorizations | 5 | 5 |

Ordinary production routing changed: **NO**. Sprint 2 ordinary owner state changed: **NO**. There was
no deployment, production acceptance, authorization, reservation, execution, or production-store
mutation.

## Recommendation and exact next step

Do not promote v7. Preserve this failed authority and both raw responses. The next unit, if authorized,
must perform offline forensic analysis of v7's systemic semantic-anchor generation under multi-region
requests and define a newly versioned correction. It must not reinterpret this response, clamp or drop
observations, retry these fixtures, or reuse this failed evidence as positive acceptance.
