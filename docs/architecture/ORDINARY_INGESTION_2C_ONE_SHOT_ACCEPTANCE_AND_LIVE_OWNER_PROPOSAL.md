# ORDINARY-INGESTION-2C one-shot acceptance and live owner proposal

## Outcome

The governed acceptance endpoint created one typed acceptance for the exact running implementation in one invocation. The runtime observed it dynamically without restart, and the owner-selected PDF now presents a read-only `PROPOSED / NOT_AUTHORISED` region-v5 workflow. No evidence authorization, execution, or provider egress occurred.

## Starting state

- Repository HEAD/upstream: `e8369963e255169888eaef91fd8007edfb972082`; clean.
- Running implementation: `363201a2233d29240571781ced0e78dfbc6680e1`.
- Image: `sha256:f6e674e0f56f405de55ea73ba97850aaf8ec9c38727fbc924d5cdf08166f2da1`.
- Container: `399dcd70c0a31aae0c6b9d01aaa9347760fc779d0b0bcf63705ee6f05057639a`.
- Restart count: 0; root HTTP 200; unauthenticated admin GET 401.
- Pre-promotion state: `CAPABILITY_NOT_ACCEPTED`; accepted promoting commit `null`.
- Original C3 acceptance: `1ef37f99850d3367fe39cd94c18262318edf043836bd546dff239131bbd14ce6`; file SHA-256 `58f803531959888dc24397ad62ed6b64d9ed0997e0ca1ef7785c20e3bfe410f3`.

The pre-promotion governed-store totals were: one acceptance, zero owner authorizations, four attempt files, two provider-state files, one region authority, 21 derivative-generation files, and 19 derivative-content files.

## Capability identity

- Capability ID: `ordinary-external-region-transcription-v5`.
- Capability digest: `9b404e8dfc4f0ffa3067fcffb00c39e6bd739050f173418de740239a1dc94103`.
- Provider/operation/model: OpenAI / `POST /v1/responses` / `gpt-5.6-sol`.
- Reasoning/store/detail: `none` / `false` / `original`.
- Adapter: `openai-responses-region-transcription-adapter` version `4.0.0`.
- Profile/wire/media: `openai-region-anchored-transcription-v2` / v5 / `application/pdf`.
- Bounds: 32 regions; 16,777,216 aggregate UTF-8 request bytes; batching false.

These semantics and the capability digest equal the preserved C3 acceptance.

## One-shot governed promotion

The sole POST contained only:

```json
{"capabilityId":"ordinary-external-region-transcription-v5","promotingBuildCommit":"363201a2233d29240571781ced0e78dfbc6680e1"}
```

- POST invocation count: 1.
- HTTP result: 201 `CREATED`.
- New record ID/digest: `5303c27eb6cb098a740a1ac5c182d994dc1293e29ef8e1e5ca1516d751580a21`.
- New acceptance file SHA-256: `2830d939048196f5918bb6cc4bad3140cb5a5390958bb1aebb3c2d45604345af`.
- New canonical checksum: `753da8aab9b18328d80d3318d36e3e43fd1e33a3c53edebbff2dd4c3286c9999`.
- Promoting commit equals runtime embedded commit exactly.

Parker reconstructed the preserved R6.9 chain itself: authority `authority-fa-9.4p-a1e-r6.8c1`, execution `execution-fa-9.4p-a1e-r6.8c1`, request digest `1a691388...`, response `resp_0007d6...`, provider state `31b997b2...`, raw/structured/provider-record digests `500863d6...` / `7031179...` / `ad254201...`, assessment `39fbc01c...`, exact R6.9A/B/C commits and reports, and `PASS_FIDELITY` 24 reviewed / 24 exact / 0 discrepancies. No evidence was supplied by the caller and no provider rerun occurred.

The original C3 record remains byte-identical. Acceptance history now contains exactly two records.

## Dynamic transition and live proposal

Without restart, canonical status changed to `ACCEPTED`, with both accepted and embedded commits equal to `363201a2233d29240571781ced0e78dfbc6680e1`. Restart count remained 0.

Fixture `evidence-4c6f2ee8-2f62-47be-bd7a-946c744b2766` remains SHA-256 `ce8bd4b53d8b007026575974014e71f648f045bf3970b0e984605cf842a7b4a5`. Its owner workflow reports:

- status `PROPOSED`;
- accepted/available canonical region-v5 capability;
- media `application/pdf`, three pages, native searchable text `PRESENT`;
- authoritative representation `Selected authoritative PDF evidence crops`;
- provider/model/profile OpenAI / `gpt-5.6-sol` / `openai-region-anchored-transcription-v2`;
- adapter identity `openai-responses-region-transcription-adapter:4.0.0:wire-5`;
- `store=false`, image detail `original`;
- external egress required, authorization `NOT_AUTHORISED`;
- next step `OWNER_REVIEW_REQUIRED`;
- execution unavailable: `executeAvailable=false`.

Exact disclosure: **“Selected authoritative PDF evidence crops will be transmitted to OpenAI for literal transcription.”** Exact explanation: **“The governed ordinary region-v5 capability is accepted for this PDF. Owner review and separate evidence-specific authorization are required before any external processing.”**

Native searchable text did not suppress the proposal. The separate historical enhanced-transcription control remains present in the page, but it did not control or block the ordinary region-v5 proposal. The UI also contains the distinct fields “External transmission disclosure”, “Evidence-specific egress authorization”, and “Owner review of this proposal; authorization remains a separate action.”

## Final preservation state

- Acceptances: 2 (the only intended mutation was one new acceptance).
- Owner authorizations: 0.
- Attempt files: 4; provider-state files: 2; authorities: 1.
- Derivative generations/content: 21/19.
- Fixture and all historical fingerprints: unchanged except for the new acceptance file.
- OpenAI calls: 0; Claude calls: 0.
- Disk: 738 GB total, 652 GB available, 7% used.

The next action is not development: Steven must perform the first ordinary owner review of the displayed disclosure. Any evidence-specific authorization and first real ingestion remain a separate explicit owner decision.
