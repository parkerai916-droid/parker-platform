# ORDINARY-INGESTION-8 above-32 complete-region-set remedy decision

## Outcome and authority

The recommended general remedy is **hybrid deterministic complete-set region coalescing**: retain the
accepted fine-region path unchanged for documents with at most 32 regions; above 32, group only
same-page, consecutive Parker source regions into at most 32 request crops while retaining the exact
ordered constituent IDs and geometry. This is an offline feasibility decision, not production
approval. Production remains unchanged and the new mode requires a new governed fidelity acceptance.

Starting repository HEAD/upstream was
`d6252d6d97301d01d1fbdd8a034a6118038961b7`, clean. Production remained implementation
`def611a8bf8cb6c2297f1d9bf6cd8146a58d4cbc`, image
`sha256:fdb583d16d99a58d13983046b2ad8b936014ead6b6c22cdf0d670b895b071521`, container
`281bba01fa82ddd4a172a424688845ea180a6dfe28eb4ae2aebd9c064ecd68ca`.

No provider transport was constructed or called. No production evidence, authorization, reservation,
attempt ledger, provider state, derivative, acceptance, image, container, or deployment was mutated.
The only evidence access was a byte-exact read-only copy into `/tmp/oi8-corpus` for offline rendering.

## Exact Sprint 2 inventory

Evidence `evidence-4c6f2ee8-2f62-47be-bd7a-946c744b2766` was SHA-verified as
`ce8bd4b53d8b007026575974014e71f648f045bf3970b0e984605cf842a7b4a5` (124,027 bytes).
The 300-DPI profile produced three 2482x3507 pages:

| Page | Canonical pixel digest | Regions |
|---:|---|---:|
| 1 | `3dc8649d608dc2051cc3950f393b8c68aac7daa98b62341a24c621215222a7bd` | 16 |
| 2 | `d00b1b3c43b48de1888108b56ba2cf853fafa5cbc0d30b1226b8d376c820a539` | 14 |
| 3 | `319ec621f29a58a1b51bdc3f5d5a9ba816668572729610efd5fa9127c2f4ab8d` | 6 |

The following table is the deterministic Parker source sequence. Bounds are
`left,top,rightExclusive,bottomExclusive`; crop bytes are canonical PNG bytes.

| Page.order | Source-region ID | Bounds | Class | Crop pixels | Crop bytes | Crop digest |
|---|---|---|---|---:|---:|---|
| 1.1 | `9803243627f40447f7215cf7ac6384faac3ccfd5b46c2508ebfe00af327cbc74` | 238,130,1276,194 | MIXED | 1038x64 | 12,764 | `ca9cb978ecf89e5460675c0daabd5ef8a51a0ed4e48d333315a0c82cea161bd6` |
| 1.2 | `448d3a30b6b80d8d36a09581a39c5c424d245e28810a401272a4adb995e8d17f` | 234,261,1435,325 | MIXED | 1201x64 | 16,004 | `88328e222a737b834e098b408711c33ca550d6057a8f9491440a9f33a6725be2` |
| 1.3 | `7c6037f37d7b7d3b2dec06544b511d11cd0aa79a6b6bbd90d3264c960fcdc124` | 236,391,889,461 | MIXED | 653x70 | 9,510 | `2fddea5a9405a69437e90cbfb2b432b03b52326deb38e8ba1abfc9beb5f143fa` |
| 1.4 | `eff3a8515a3339a07ee5bbb78429b12edf7bdec0e30c42dd3ff893589397f28b` | 236,509,2252,1480 | TABLE_LIKE | 2016x971 | 135,356 | `c31958632c45ac494862dcd3578d30ce1e70dde624611d383ec148f4a425e06d` |
| 1.5 | `44cd5cca2195b7f55360456fae6216c31082fd45f0c9a54d8519e5fdc477be99` | 238,1512,2200,2096 | TABLE_LIKE | 1962x584 | 108,676 | `6fb6c53100111825544489ab7edf90a31ec2419ac5adafff7b61b9679cdc96d1` |
| 1.6 | `89b353b3280e872a49e84f029d72b1442457696cb17845af59eeb7d4a5db6743` | 235,2176,2252,2196 | MIXED | 2017x20 | 292 | `c67383d144fcfb53cf6861a79e0bde3078fea17eab6a117a14314de92d91bf28` |
| 1.7 | `76bafb16eff6331dc010e2f892386821c3d3bf1f8a72b12e0d8cd8702843c037` | 234,2287,1014,2350 | TABLE_LIKE | 780x63 | 11,707 | `01caed87400f7b36df0f7ca696b7c29a604461721958a13a18892f79c53bfcc5` |
| 1.8 | `4ad7be42f3ddaf3e82363f2998e9d7970ac01d7d1d0090333b5c76c43cf7a259` | 234,2403,1110,2465 | MIXED | 876x62 | 11,794 | `e6fd97d1b0818df91db7c257c069f8a136d9a9874cbe261fad1b4711ab085d9e` |
| 1.9 | `c28a5e3b215b780cae7eaa8ed46ac397d374dbb5e1cc3e17fb2d2e2c6174a671` | 238,2532,854,2602 | TABLE_LIKE | 616x70 | 8,777 | `5793829d98fbb7b55d9bed04da9b6cdc17717843a1fc2fbc624392c3f69b2fc8` |
| 1.10 | `fce87538605892b4e4e2413032a6b2dbb0d5ca789ff928b5d442d2e821aead2f` | 263,2633,613,2687 | MIXED | 350x54 | 4,581 | `090640e6be83fe92c9a23efcb8f885a2adc91b010b5ddf5f5045b2db577c3af9` |
| 1.11 | `3c20ac7d912ada2a04b40cc7f4becbe3959fb02c4c34c67063a7f23dcb5bfa11` | 263,2715,614,2768 | MIXED | 351x53 | 4,764 | `165a3a44248f486bab55e8585cc68a855261af3d2f40870360d2a0a6841bd36d` |
| 1.12 | `e629f634c300b4910daa7055d5b3cd0feb04ea7b0980334b81e5722434cbddf8` | 263,2796,834,2931 | TEXT_LIKE | 571x135 | 14,632 | `ecf8f7384742b6bf71c809deccfc7fb42abae2e18efd54ce25accee00b076542` |
| 1.13 | `b4b194a0fe3c91a2db3c31631205ac9917954b9feabb4de9c95ffa1d3019fca6` | 263,2959,749,3012 | MIXED | 486x53 | 6,440 | `ae65d2c8accdd8a73b2618a8adb5c92f682f74feb19c097247119fd4d32fb39b` |
| 1.14 | `86abf76f38ea0a49eefbc35daa0b334df00d541c57e023e4536e9c08877f7c15` | 263,3040,888,3175 | TEXT_LIKE | 625x135 | 15,004 | `9509256ab1008939d085d3401569c8d1ad82a70b46cd816cfd064a84d31b3cc7` |
| 1.15 | `63d807069f594dfc1b0c86db91764a35ba40a8352b38057162a7da5ebb4ed4ef` | 263,3203,727,3256 | MIXED | 464x53 | 6,637 | `a26279a0dc82188a1b44be3141cedeb83c422b5dc15bb95ba222324513afbcee` |
| 1.16 | `dc482166a76cf2f089c923202de6ce219a061421488f92ad6f3a5e172472e863` | 263,3284,867,3347 | MIXED | 604x63 | 8,251 | `9aba7701d268f0957cfd17f384cd816aba9513af1543ef371f3992f7f41af928` |
| 2.1 | `cc50a3c3dbfae82f895aaa999c1d443539430f0fac79c78eb555fbc32e4a5929` | 263,130,970,184 | MIXED | 707x54 | 9,263 | `fbce8f41596d507dfc13bf09516f60c8f8044da777152e6187044729c6d70578` |
| 2.2 | `f660307fd896ddf72c5dd2a376e85b4bcc31e587fae59a6fbbbf5b2bf0020ddb` | 263,211,911,265 | MIXED | 648x54 | 8,391 | `df05cb8fb8db1a27af9fd43034ab6e1eb17424aeb92083b4291ca8e780b04184` |
| 2.3 | `d2e175dbbfa0da46f51d78ac83450935b846d3e5117ed4fcd3c76f5aa101c3f7` | 234,318,1191,381 | MIXED | 957x63 | 13,220 | `652587b06a908b40378cad98b4b8b8700668c9bbc1e63dda466e3ad45c84133d` |
| 2.4 | `796ae200d96e9ca84f12437b8d9c702d5f0d71a7d7e064c784030f983edd419f` | 238,447,873,517 | TABLE_LIKE | 635x70 | 8,816 | `39dd442f494dd77070abc6bbaea3bd8df950e92bc73bc52f4b502169c4252840` |
| 2.5 | `05fae25adcbc5985d62ee61ca231fa12792df0e6083d217954a1c3f57bd8a1e8` | 263,550,509,684 | TEXT_LIKE | 246x134 | 5,184 | `0d7d933e485b5d2aa96057215338a14bd63de26de4539c8490adef9b18bf57f4` |
| 2.6 | `44888288b1b5e3aa9b95034be88ee13f0fca3d14e96e8b75d28ea727705400b9` | 263,712,665,847 | TEXT_LIKE | 402x135 | 9,152 | `a529310ee63d9dc581739eeda3483c540aecbe72b4fbc050a29568843672c6c2` |
| 2.7 | `544b4d4baceea2a2c9729b9272a5aa40e06e201fe721156ebed6904811c0b72f` | 263,875,763,1181 | TEXT_LIKE | 500x306 | 24,943 | `c63dae9896a6a9e82c88f9882a949c9da6ba40db281dbc131dd229fed3b67b06` |
| 2.8 | `545a1b8ecb113c027e1067955622bdfbdefa4796efdc27107c72156fba40cfe7` | 234,1224,876,1288 | MIXED | 642x64 | 8,586 | `fdcb8fa851fcd21a46fb0e1d0cfc701c385d6dfb97a028c302ac814ce8305060` |
| 2.9 | `ffdbd2d429384dbb8555c4d0dbae4a743ea654444842e7f88a82dc964dcf7955` | 235,1373,2252,1393 | MIXED | 2017x20 | 291 | `448d7fcc843b158d0fc10d206d9e32c95a7c61b6716540d0fb2457c05f41316d` |
| 2.10 | `77af7cc5968cc1fef55915a40de062cc4b44964e931bfdaac78f422c831e8af0` | 234,1484,764,1547 | TABLE_LIKE | 530x63 | 7,799 | `79c478dfbacf28d77783c6f0a8668aaa23a2f011a314042fad597a5107a6294e` |
| 2.11 | `094211dab460064f3349cc009daa0d5fde2a17a8cf94123bd2aae18e1cb89403` | 235,1599,2186,1737 | TEXT_LIKE | 1951x138 | 31,369 | `7c08c49389b25a21716f7cf5abcc6d50857445dfd00703a6079690a39b5b8d40` |
| 2.12 | `69be9a391d45d074091df2542c1199a8a06c6b2afd967805ce88946a19a7b2ff` | 235,1815,1816,1878 | MIXED | 1581x63 | 18,584 | `6cf5836764f9ccfabf03105ae3680a03ee187525920a4459f3054b788f9e4b24` |
| 2.13 | `43571ee3ba4809fbfec9e8b3747aa550c325d75ed4bb350043b22915ce767820` | 238,1950,601,2013 | MIXED | 363x63 | 5,429 | `c7344b185f9b90a2d4b8399a8665daa3959cf37d551bcc75174b2a404d600ace` |
| 2.14 | `5954f7a8f8dcd779ac891a82ed5e50d7d00cf0905455cbaf253337795d8aeeed` | 236,2059,2252,3398 | TABLE_LIKE | 2016x1339 | 50,115 | `f0b363d77d9937e97a673c3dc8257ea43f1467a8e0e34202910980776af961c5` |
| 3.1 | `effd324fa3a934fe97eab08898981e237b87e9a88f337548a4c2c8906e55245f` | 236,107,2252,664 | TABLE_LIKE | 2016x557 | 21,229 | `78674e64ced223ea51ca664cda96731b7955641d63e407841fb9f5eec6f66af3` |
| 3.2 | `5c84884333755bf6cb8f6b29a5b658cb89ee4aa9e50fe44e617a2df1c378e126` | 238,696,866,760 | MIXED | 628x64 | 8,809 | `446d2308712579c5e443b18fe7ea0186838b1f4613187dda3f926856330dacc8` |
| 3.3 | `a0c0c79b5f429e178b9ea9521baa81abe60ea9518e1323e9c04b5459c1761364` | 238,828,1670,890 | MIXED | 1432x62 | 17,239 | `ac85d3e729899d5f20610ba3e83176f0eae658c5474ede18f83b8382a99b0fd7` |
| 3.4 | `c155f4c79fdaa207ae0e64217120cef85190b72ba037f24fbe8c2573ebea71ae` | 235,961,2219,1269 | TEXT_LIKE | 1984x308 | 90,870 | `8fa140a90a88a32823ea45409ce7ed2b08455b46a48174702645047cb181cc49` |
| 3.5 | `72dcc4e690c07f2aebdbc357d5bb400f655e8d2c2b20ec2433a04efb731e9ba7` | 235,1336,2238,1644 | TEXT_LIKE | 2003x308 | 84,177 | `9d39b43b43a8914f3065108ec09ef2e57e959281e878d68db3cfc4cd7a3220ee` |
| 3.6 | `e14bb3c3d3fe8db082970a4251037a386ae6f338a86b799c305903e1fffe1dec` | 234,1711,2227,1938 | TEXT_LIKE | 1993x227 | 61,419 | `fad163d659c18529e6abd1eb5320b1bc446c3944e61dfd6ff2a35ed876852334` |

Total is 36, four above the accepted maximum. The exact current adapter JSON-shape estimate is
1,163,076 UTF-8 bytes, below 16,777,216. Page count, pixel geometry, rendering, derivation,
unambiguous order, and body size otherwise pass. Therefore region count is the only failing request
bound for this evidence: **YES**.

The excess is expected whitespace-separated, multi-page structural fragmentation, not a geometry
defect. Counts by class are MIXED 19, TEXT_LIKE 9, TABLE_LIKE 8; there are two 20-pixel rule-like
crops classified MIXED, but no population of repeated tiny regions large enough to explain the excess.
Classification: primarily F (expected ordinary multi-page behavior), with B/D (fine/page-level
fragmentation); not E (deterministic geometry defect). No semantic document meaning was inferred.

## Registered PDF corpus

Twenty-four registered PDFs were examined provider-free. Counts were:

- min 1, median 24, p75 46, p90 63, p95 63, max 63;
- <=32: 15/24 (62.5%); >32: 9/24 (37.5%);
- >48: 5; >64: 0; >96: 0.

Thus 32 is frequently exceeded in this small corpus and is operationally restrictive, not merely a
rare edge case. After the proposed shaping rule, 17/24 (70.83%) are eligible. All shapeable documents
finish at no more than 32 request regions; worst is 32. Seven remain fail-closed: two because their
source-order graphs are ambiguous, and five because their shaped bodies are 19,773,533 bytes, over the
16-MiB bound. No document remains over the count bound after successful shaping. The corpus includes
duplicate bytes under different evidence IDs; percentages are per registered evidence artifact.

## Candidate decision matrix

| CANDIDATE | COMPLETE COVERAGE | ONE REQUEST | NO OMISSION | DETERMINISTIC | SOURCE ORDER PROVABLE | PROVENANCE PRESERVED | EXPECTED SPRINT 2 FIT | EXPECTED CORPUS COVERAGE | REQUEST SIZE RISK | IMPLEMENTATION COMPLEXITY | CAPABILITY CHANGE YES/NO | NEW FIDELITY ACCEPTANCE REQUIRED YES/NO | RECOMMEND / REJECT |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| A Keep 32/reject | Yes for accepted inputs | Yes | Yes | Yes | Yes | Yes | No | 62.5% | Low | Low | NO | NO | Reject as general remedy; retain as fail-closed fallback |
| B Raise to 48/64/96 | Yes | Yes | Yes | Yes | Yes | Yes | Yes | 48: 79.2%; 64: 100% by count | High; five 63-region PDFs already exceed body bound | Medium/high | YES | YES | Reject; count alone does not solve body/provider risk |
| C Deterministic coalescing | Yes, with exact membership | Yes | Yes | Yes | Yes only for unambiguous consecutive groups | Yes with new model | Yes, 32 | 70.83% including body/order gates | Medium; must calculate after shaping | High | YES | YES | Viable core |
| D Pages/superregions | Visually yes | Yes when pages <=32 | Yes | Yes | Weak within-page binding | Page-level only unless new membership | Yes, 3 | Likely broad by count | High pixel/body area | High | YES | YES | Reject alone; loses fine geometry/order binding |
| E Hybrid fine/coalesced | Yes | Yes | Yes | Yes | Yes with fail-closed gates | Yes with new model | Yes, 32 | 70.83% proven | Medium; independent 16-MiB gate retained | High | YES | YES | **Recommend** |
| F Multiple requests | Yes | **No** | Yes | Can be | Cross-request semantics new | Could be | Possible | Broad | Lower per request, multiplied attempts | Very high | YES | YES | Reject; conflicts with one-request/single-attempt governance |
| G Truncation/omission | **No** | Yes | **No** | Could be | Incomplete | **No** | Superficially | Misleading | Low | Low | YES | YES | Reject as fidelity violation |

Memory and pixel-area risk remain bounded by streaming one canonical page/crop at a time; the
prototype initially demonstrated why implementations must not retain all 300-DPI pages and base64
payloads concurrently. Provider image-count/model behavior for a changed geometry has not been
accepted and cannot be inferred from offline feasibility.

## Exact deterministic rule

1. Derive all original page graphs unchanged. Reject non-unambiguous/cyclic graphs, empty sets,
   rendering/geometry failures, and more than 32 nonempty pages.
2. If total regions <=32, retain the existing fine-region mode unchanged.
3. Otherwise give every nonempty page quota one. Repeatedly give one remaining quota to the page
   with maximum `sourceRegionCount/currentQuota`; ties are page number then page representation ID.
   Stop at 32 or when each original region has its own quota.
4. Use Parker's deterministic topological source order. Partition each page's ordered list into its
   quota of consecutive, balanced groups with boundaries `floor(i*n/q)` and `floor((i+1)*n/q)`.
5. A group never crosses a page. Its crop is the bounding union of its constituents. Its class is the
   common class, else MIXED. Retain the exact ordered constituent IDs, each constituent's original
   bounds/digest/provenance, union bounds, and canonical union-crop digest.
6. Hash a length-prefixed domain (`parker.complete-region-set-group.identity.v1`), page identity,
   page number, derivation profile/version, union bounds, union crop digest, member count, and every
   member ID in order for the request-region identity.
7. Prove membership is an exact partition (no missing/duplicate constituent), group order expands to
   the original Parker order, dimensions are within the source page, identities are unique, resulting
   count is 1..32, and exact UTF-8 body is <=16,777,216. Otherwise reject before reservation/attempt.

Consecutive grouping prevents crossing source-order branches only after the original graph has one
provable deterministic topological order. Ambiguous graphs fail closed. Within a merged crop, provider
text cannot be reassigned to individual constituents without new accepted semantics; the derivative
must bind the returned block to the group plus its ordered constituent membership. Provider-relative
order remains non-authoritative.

## Sprint 2 proof

The deterministic quotas are page 1=14, page 2=12, page 3=6, yielding 32 groups. Four pairs merge:
page 1 regions 7+8 and 15+16; page 2 regions 6+7 and 13+14. All other groups are singletons.

- original/remedied counts: 36/32;
- constituent coverage: YES, 36/36 exactly once;
- duplicates: NO; missing IDs: none;
- source order: YES; expanding groups produces the exact 36-ID sequence above;
- provenance: YES in the proposed model, through group -> ordered members -> page -> evidence/bounds;
- shaped request estimate: 1,166,199 bytes, below 16 MiB;
- repeated execution: PASS; identities, group membership, bounds, and ordering identical.

The retained test-only prototype proves deterministic allocation/partition/identity, exact membership,
same-page containment, <=32 behavior, unchanged singleton behavior at 32, and fail-closed ambiguous,
cyclic, and >32-nonempty-page cases. It is not reachable from production composition or routing.

## Required production and governance work (future unit only)

Current models cannot preserve constituent identities: `RegionTranscriptionTarget` and response blocks
bind one `sourceRegionId`, and the v5 manifest/schema/validator/reconstructor know no membership list.
Exact implementation units required are:

1. production complete-set shaper and immutable group/membership/provenance models;
2. request target, digest, manifest, wire schema and processing-profile version with ordered constituent
   IDs, union geometry/digest, and explicit fine/coalesced mode;
3. strict validator for group identity, complete partition, page/bounds, uniqueness, count and body;
4. reconstruction/admission payloads preserving group and original member provenance and Parker order;
5. preparation gates before authorization reservation and provider-attempt start, with explicit
   unsupported dispositions for ambiguity, >32 nonempty pages, geometry and body overflow;
6. catalogue/status/owner presentation of the changed capability and bounds;
7. synthetic, exact-Sprint-2, corpus, crash/restart, malformed-wire, no-egress and full-suite tests;
8. a new capability identity, new build-bound acceptance record and deployment preflight.

This is a **SUBSTANTIVE ACCEPTED CAPABILITY CHANGE**. New governed fidelity evidence must exercise
singletons and merged groups (including mixed structural classes, long vertical unions, tables,
uncertainty and point anchors), prove exact text/visual fidelity against authoritative pixels, strict
membership and source-order reconstruction, schema rejection, 32/body boundaries, no omission, one
request/attempt, durable raw/structured state and deterministic replay. Existing R6.9 acceptance must
not be carried forward. No such acceptance was created in OI8.

## Verification and next step

- Exact target rendering/inventory/body measurement: PASS, repeated deterministic result.
- Registered PDF survey: 24/24 completed in isolated per-document workers.
- `CompleteRegionSetCoalescerTest`: 3 tests PASS, provider-free.
- Production source/build/image/runtime: unchanged; provider calls OpenAI 0, Claude 0.

Next: open a separately governed implementation unit for the eight units above, followed by new
provider fidelity acceptance and only then a separately authorized deployment/promotion unit. Do not
authorize or execute the Sprint 2 evidence under the current accepted capability.
