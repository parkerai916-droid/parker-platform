# ORDINARY-INGESTION-10R3 multi-region semantic-anchor forensic analysis

## Determination

The Sprint 2 failure is provider-generated semantic-anchor failure, not transcription, constituent
provenance, Parker ordering, coordinate conversion, or coalescing failure. All 32 literals are
visually faithful; all 36 source constituents are present once in Parker order. Of 106 observations,
55 are semantically aligned, 51 misaligned, and none arithmetically out of bounds.

The selected successor is Model G with the deterministic portion of Model F: v8 removes provider
visual observations from canonical evidential transcription. Exact line/paragraph structure already
exists in `literal_text`; Parker may inspect it deterministically for presentation, but it must not
derive semantic styling from text heuristics. Provider output is limited to literal transcription,
explicit uncertainty, and request-region identity. Parker retains page, geometry, membership, order,
and provenance.

**UNIT ORDINARY-INGESTION-10R3 COMPLETE — V7 MULTI-REGION FAILURE HAS BEEN FORENSICALLY ISOLATED TO PROVIDER-GENERATED SEMANTIC ANCHORING RATHER THAN LITERAL TRANSCRIPTION, CONSTITUENT PROVENANCE, OR PARKER SOURCE ORDER; A TRUTHFUL VERSIONED V8 OBSERVATION MODEL HAS BEEN DEFINED AND VERIFIED PROVIDER-FREE WITHOUT HEURISTIC REPAIR OR WEAKENED VALIDATION; PRODUCTION UNCHANGED**

## Exact evidence

Starting HEAD/upstream were `8bfc6531807862d581ec392f8320d6002e087b6a`; worktree clean.
OI10R2 report SHA-256 was
`0b265d8ddd2a7789b571ff365f31008b6cb14c9da5e0867f1a6fa8ff0c562e14`.

| Fixture | Response | Raw SHA-256 | Authority | Attempt | Response record | Assessment | Manifest |
|---|---|---|---|---|---|---|---|
| A | `resp_0fbdaff37f9e1b41016a95121dd93087d0bc55dede939c0897` | `067310ff9e0d7fb552d6c9718b894e6f1e31f01416271d176c07e444361cbaf1` | `20831100f745ce79509b1a8709d8418b06527086fee03175ee3b365540c1e86e` | `79b37d68e40fcd1648e6899fdf4805137656e19c13e7b91253be84554f592ef0` | `58189ceb521b244272c8b7a15317d7a15f3b5f2ade6f578d08926b8ac5a73935` | `a712e11839a6318b5a13c980553e42568a63629d984011808c6c734165845545` | `a3b2a15e49a13972be750f23e16ddaf97f80deb8f8819cdae333016b1f2c8b4e` |
| B | `resp_05955e7f2f0f5abf016a951a9edb6c87d086db3880ad58f808` | `adbb651e6e28312796318aac1e6da34aaa456eb7fc746c4fb05c8d8fc3097cc9` | `943eeff50131c8bc0000b63e35b723c5b774248231e4e1dd64650cd3188fb743` | `66bd06ae42c06a6836851dee149dad5aea7c0edc0416b025ccf5b26d55dd081e` | `276c3372a4a62ca602c62e0bdc008251c5a19f4460d9b3c64b03253a3b1cba33` | `cc755bbbb8f18298a5d7cd772b872870e06fd9c549edc4905c361218848d25b6` | `31a6510c10527030df9b8c6e0f930953de3901fd42ac1b2314cc4153051434b8` |

Request digests were A `027824bfae7465dbbb97f263750e243ff4ed67ce8b353dbf56a36f931a23ac50`
and B `4545601b6d5cc79adfb6f541dd3b9fd17afecc8fc17aa0d4201b6146351e8bdc`.
Manifest record digests were A `95efcbd058d7eb3ae5f3d9928cd7a8663e9b45aa443fb71be52888f26b9b3f14`
and B `da2578de9f06a479adb735d744ae4b9e41e62970b792b21719798b117d546538`.

## Literal fidelity

All 32 B crops were independently compared with their returned literals: 32 exact/material PASS,
zero material failures, omissions, insertions, substitutions, source-order displacements, or
uncertainties. The four coalesced regions also pass:

| Ordinal / request region | Constituents | Literal result |
|---|---|---|
| 7 / `2aa303dd8f6e1069bc562f9456eff1e356339c9e5be06f1a43584791a9304167` | `76bafb16…`, `4ad7be42…` | exact PASS |
| 14 / `80e9ce224bd3e023be309eda26e5cc781e3b0ed82de9dbcbd876a34d2086638b` | `63d80706…`, `dc482166…` | exact PASS |
| 20 / `9e64d2b56fd1739b97bc464b0d9871ee731ad28a32f828f6a4b7a3a3684bcd9d` | `44888288…`, `544b4d4b…` | exact PASS |
| 26 / `f9ab1981fbe5520c028634fdb858ac243425982bc2cd4e14441e2d0bb9d985f3` | `43571ee3…`, `5954f7a8…` | exact PASS |

This separates text fidelity (PASS) from observation-anchor fidelity (FAIL).

## Complete observation inventory and distributions

The authoritative 106-row inventory is committed alongside this report as
`ORDINARY_INGESTION_10R3_OBSERVATION_INVENTORY.csv`. It records region/request identity, page,
constituent scope, observation ordinal/type/range, exact anchored substring, arithmetic and semantic
validity, nearest genuine structural target, and displacement. No observation is sampled or omitted.

| Distribution | Total | Misaligned | Rate |
|---|---:|---:|---:|
| singleton | 68 | 34 | 50.0% |
| coalesced | 38 | 17 | 44.7% |
| constituent count 1 | 68 | 34 | 50.0% |
| constituent count 2 | 38 | 17 | 44.7% |
| page 1 | 45 | 22 | 48.9% |
| page 2 | 45 | 20 | 44.4% |
| page 3 | 16 | 9 | 56.2% |
| text <25 code points | 14 | 0 | 0.0% |
| text 25–99 | 24 | 2 | 8.3% |
| text 100–249 | 36 | 21 | 58.3% |
| text >=250 | 32 | 28 | 87.5% |

| Type | Total | Misaligned | Rate |
|---|---:|---:|---:|
| `BOLD` | 9 | 3 | 33.3% |
| `ENLARGED_TEXT` | 5 | 0 | 0.0% |
| `LINE_BREAK` | 61 | 36 | 59.0% |
| `LIST_MARKER` | 25 | 8 | 32.0% |
| `PARAGRAPH_BREAK` | 6 | 4 | 66.7% |

Failures occur in request ordinals 4, 5, 20, 24, 25, 26, 29, 30, 31, and 32. The decisive
correlate is long/structurally complex literal output, not coalescing, page, or constituent count.

## Drift and root cause

Returned-to-nearest-target displacement ranges from -8 to +19 code points. Within long blocks it
often grows (for example region 30: +5, +12, +19; region 31: +4, +8, +14), but other blocks change
magnitude and reverse direction. Drift changes around newline/punctuation and is not constant. There
is no UTF-8/UTF-16/normalization conversion, fixed shift, one-line shift, or constituent-boundary
transformation. Cumulative estimation drift is present as a symptom, not a deterministic codec defect.
No repair is valid.

Ranked hypotheses:

1. H1/H3: provider writes or visually infers structure independently, then estimates ranges against
   the returned literal — strongly supported.
2. H4: reliability degrades sharply with text length/structural complexity — strongly supported.
3. H7: requested precision exceeds what acquisition needs and what the model reliably supplies —
   strongly supported by the consumer audit.
4. H2: some anchors may reference an earlier internal draft — plausible from cumulative drift, not
   independently provable.
5. H6: paragraph/list semantics add ambiguity — contributory, but line breaks also fail.
6. H9: type-level observations may be visually true while exact offsets are false — supported.
7. H5/H8: coalescing/request-region coordinate frames cause failure — contradicted by singleton and
   constituent-count rates.

Fixture A had 272 code points, 21 lines, 26 observations, one constituent, and 9.56 observations per
100 code points; it passed 26/26. B spans many independent crops: short simple blocks pass, while
long blocks fail despite one constituent. Thus no universal scalar threshold exists, but risk rises
sharply past 100 code points and is extreme at 250+.

## Consumer and trust audit

Exact ranges are parsed and included in reconstructed/derivative canonical digests and therefore
indirectly in admission/authorization identity. They are not read by any downstream decision engine.
They do not determine literal text, request identity, page, geometry, constituent membership,
reconstruction, Parker order, or uncertainty. Tests exercise validation only. Exact-anchor need is
therefore not genuine: **NO**.

Field status: literal is fidelity-critical; request/page identity, Parker geometry, membership, and
order are provenance/reconstruction-critical; uncertainty spans are fidelity/review-critical;
provider observation type/range is review/diagnostic metadata and currently has no independent
consumer. Model-generated observations must not be canonical evidentiary claims. Parker source order
remains PDF → rendered page → source regions → ordered constituents → ordered request regions.

## Candidate decision

| Model | Decision | Reason |
|---|---|---|
| A exact ranges | reject | 48.1% semantic failure and no genuine consumer |
| B request-scoped | reject as canonical; permissible future diagnostic | type alone adds unverified model inference |
| C constituent-scoped | reject | asks provider to assert Parker identity/provenance it cannot prove |
| D optional anchors | reject for canonical v8 | invites selectively fabricated precision with no consumer |
| E embedded tokens | reject | contaminates literal evidence and reconstruction |
| F Parker-derived | select only deterministic literal structure | line breaks are already exact literal characters; no semantic heuristic |
| G remove provider observations | **select** | minimum truthful provider contract; all real consumers remain satisfied |

Nearest-substring, fuzzy/edit-distance relocation, sliding, clamping, regex relocation, and rewriting
are explicitly rejected. They are diagnostics only.

## V8 prototype contract

- Capability: `ordinary-external-request-region-transcription-v8`, lifecycle `ACCEPTANCE_PENDING`.
- Capability digest: `033ce0e606e4c86f9be5c4ee45b5f806dea5451c67e9faf73d697634f43094e5`.
- Profile/schema/wire: `request-region-fidelity-acquisition-v4` /
  `request-region-transcription-schema-v4` / 8.
- Processing: `external-transcription.deterministic-complete-set-request-region-v4`.
- Adapter/parser versions: `7.0.0` / `3.0.0`.
- Instruction/schema digests: `effa7ff9f29c41a4eff31e79664c33f74ba399b1a883ef138fbd866a0034fe55` /
  `b974170079485400ae228b64c3e40442e354983b8b652057b4d47649f0990a7f`.
- Literal, uncertainty, request binding, status, warnings, and provider provenance remain.
- `visual_observations` is absent and is rejected as an additional provider field.
- Constituent IDs, page geometry, and source order remain Parker-owned and are not provider claims.
- Reconstruction and derivatives preserve exact literals/order/membership without observation fields.
- V6/V7 remain immutable historical identities; v7 remains failed evidence.

Provider-free A/B projection preserved exact raw hashes, block order, request IDs, literal strings,
uncertainty, and Parker manifest membership while dropping misleading anchors without relocating or
repairing them. This is feasibility evidence only, not retroactive v8 acceptance.

Capability semantics changed: **YES**. Fresh provider acceptance required: **YES**.

## Minimum next acceptance

Three calls are sufficient: A singleton control, B Sprint 2 coalesced/long-complex control, and C
source-order-sensitive. D mixed structure is redundant because v8 no longer asks for visual
structure metadata; add it only if offline fixture analysis identifies a literal/uncertainty delta.
Each call remains one-shot/no-retry with raw-before-parse persistence and provider-free replay.

## Verification and preservation

Targeted v8 tests: 3 passed, including immutable A/B replay. Full suite: 244 suites, 3,282 tests,
0 failures, 0 errors, 14 skipped. `git diff --check`: PASS. Prototype commit:
`952beb96abc1beb840e13b842f495b843cf04452`.

OpenAI calls: 0. Claude calls: 0. Production store mutations: 0. Production counts remained attempts
4, provider-state 2, authorities 1, generations 21, content 19, capability acceptances 6, owner
authorizations 5.

## Exact next step

Do not deploy or promote v8. The next authorized unit should harden the prototype into the complete
v8 request codec/parser/derivative contract, keep lifecycle acceptance-pending, and perform the
three-call delta-focused fresh acceptance above.
