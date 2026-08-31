# ORDINARY-INGESTION-10R4 request-region v8 truthful contract hardening

## Result

V8 is now a complete offline production-capable acquisition contract and remains
`ACCEPTANCE_PENDING`. No provider, deployment, promotion, routing, authorization, execution, or
production-store mutation occurred.

**UNIT ORDINARY-INGESTION-10R4 COMPLETE — REQUEST-REGION V8 TRUTHFUL TRANSCRIPTION CONTRACT IS FULLY HARDENED OFFLINE WITH CANONICAL LITERAL TEXT, EXPLICIT UNCERTAINTY, REQUEST/CONSTITUENT PROVENANCE, PARKER-AUTHORITATIVE SOURCE ORDER, STRICT VALIDATION, DETERMINISTIC DERIVATIVES, AND NO PROVIDER-GENERATED SEMANTIC ANCHORS; V8 REMAINS ACCEPTANCE-PENDING AND PRODUCTION UNCHANGED**

## Baseline and final identities

Starting HEAD/upstream were `e3cba4163718617eeadc1ce5491a0e48dc04e325`, worktree clean.
R3 prototype `952beb96abc1beb840e13b842f495b843cf04452`, report SHA
`fbf09f41ca8d87abe2de081d9fcefedeb977294b4c2e500076a85ebda8383ea9`, and inventory SHA
`a6e7e7ad1a177bd79394c9772aed018db699e1d92d45d26608d7701561ca4a11` verified.

- Capability: `ordinary-external-request-region-transcription-v8`
- Digest: `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`
- Lifecycle: `ACCEPTANCE_PENDING`
- Profile/schema/wire: `request-region-fidelity-acquisition-v4` /
  `request-region-transcription-schema-v4` / 8
- Adapter/parser: `7.0.0` / `3.0.0`
- Processing: `external-transcription.deterministic-complete-set-request-region-v4`
- Instruction digest: `effa7ff9f29c41a4eff31e79664c33f74ba399b1a883ef138fbd866a0034fe55`
- Schema digest: `0e625f26b6f977482a73bcf2de929afd9dbd4f4acc8c246f14c1a53df3cf9cd9`

Hardening changed the capability and schema digests because span-level uncertainty was replaced by a
truthful region-scoped contract. Provider/model/endpoint/reasoning/store remain OpenAI,
`gpt-5.6-sol`, `POST /v1/responses`, `none`, and `false`.

## Provider request and response contracts

The deterministic codec emits one user request containing the exact v8 instruction, Parker-issued
request-region manifests, and original PNG crops. It binds correlation ID, v8 profile/schema/wire,
schema/instruction digests, provider/model, adapter/parser versions, processing identity, evidence,
page/bounds/crop/image identity, and ordered constituent IDs. The 32-request and 16 MiB limits are
fail-closed.

Canonical provider block fields are `request_region_id`, `page_number`, `literal_text`, `status`,
`uncertainties`, `warnings`, and forensic `provider_returned_ordinal`. Provider visual observations or
character anchors are absent; additional legacy fields are rejected.

Literal text preserves exact Unicode, combining/supplementary scalars, CR/LF, spaces, punctuation,
and ordering without normalization. Provider order never overrides Parker order.

## Uncertainty

V8 uncertainty is request-region scoped: category, nullable bounded description, bounded alternatives,
and nullable provider confidence. It makes no exact character-span claim. Historical non-empty v7
span uncertainty cannot be projected into v8; projection fails closed rather than repairing or
coarsening it silently. A/B contain no such uncertainty and replay exactly.

## Parser, validation, reconstruction, and derivatives

The strict parser validates exact top/block/provenance keys, capability identities, complete and
unique request IDs, exact page binding, literal bounds, region-scoped uncertainty shape, and known
statuses. Unknown, duplicate, missing, and legacy-observation-bearing blocks are rejected.

Reconstruction indexes provider blocks by Parker-issued ID and emits them strictly in the existing
request order. Derivative blocks contain request/page identity, ordered constituent IDs, exact literal,
status, region uncertainty, and warnings. They never fabricate substring-to-constituent mapping.

Canonical digest inputs are capability ID, exact v8 request digest, evidence ID/SHA, Parker-ordered
request IDs/pages/constituent memberships, exact literals, status, region uncertainties, and warnings.
Provider observations are excluded. Repeated binding produces an identical digest.

Provider-state durability remains unchanged: durable attempt start → transport → durable raw response
→ parse → validate → reconstruct/admit. The implementation adds no production route; the next
acceptance can use the existing durability architecture. Owner authorization remains separate.

## Historical compatibility and replay

V5/V6/V7 codecs, digests, observations, derivatives, and failed evidence are unchanged. The explicit
feasibility projector preserves A's exact literal/request/provenance and B's 32/32 exact literals,
36/36 constituents, four coalesced literals, and deterministic order. It drops anchors without repair
and is not acceptance evidence.

## Sprint 2 proof

- Evidence SHA: `ce8bd4b53d8b007026575974014e71f648f045bf3970b0e984605cf842a7b4a5`
- Source regions: 36; requests: 32; pages: 16/14/6 → 14/12/6
- Coverage: 36/36, missing 0, duplicates 0
- Body: 1,165,499 bytes
- V8 request digest: `a5ef4672e07493ae870cc798fe24a87651ffc007446e40716f48ea5e62356d03`
- V8 body/manifest digest: `8bf1c47066c6c13bd2a72757712ceebe5497b3929153b0c5befc0a56c362690a`
- Repeated construction: identical, PASS

## Semantic delta and verification

Changed from v7: provider response/schema, instruction trust boundary, region-level uncertainty,
parser/validator, canonical derivative, digest inputs, and observation evidentiary status. Unchanged:
evidence/rendering, source regions, coalescing, constituent provenance, Parker order, provider/model/
endpoint, reasoning/store, request/body limits, durability order, and owner separation.

Targeted v8 matrix passed, covering lifecycle/schema, exact identities, A/B replay, unknown/duplicate/
missing IDs, provider-order reversal, Unicode/newlines, uncertainty, constituent provenance,
coalescing, legacy observation rejection, canonical digest determinism, limits, and Sprint 2 shaping.
Full suite: 244 suites, 3,283 tests, 0 failures, 0 errors, 15 skipped. `git diff --check`: PASS.
Implementation commit: `18b81a6d7834cb19e1ad884dbcc40a22289af288`.

OpenAI calls: 0. Claude calls: 0. Production mutations: 0.

## Acceptance readiness

D is **NOT REQUIRED**: v8 no longer claims mixed visual structure, while A/B/C cover singleton,
long/coalesced literal fidelity, uncertainty shape, binding, and source order. The next unit should run
exactly three fresh one-shot/no-retry calls: A singleton, B Sprint 2, C source-order-sensitive, with
raw-before-parse durability and provider-free replay. Do not deploy or promote before that acceptance.
