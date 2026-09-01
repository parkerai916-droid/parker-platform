# OI11R4E — V8 Production Capability Acceptance and Promotion

## Result

`PASS — OI11R4E COMPLETE`

Steven explicitly accepted production execution authority for exactly `ordinary-external-request-region-transcription-v8` with digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`. No provider execution was authorized or performed.

## Baseline and route

The running artifact remained source `c304fdeff6bd89f96e8397ef4192e9f83b41cb93`, image `sha256:e161c65c98c0572cb0981be652adfa029406e0104f843b1359a70b0031398673`, restart count `0`, with the accepted provider profile unchanged. The corrected authenticated route was `POST /owner/admin/region-capability-acceptance`; it delegates to `OrdinaryRequestRegionV8CapabilityAcceptanceCoordinator` and preserves legacy routing and fail-closed unknown/cross-family handling.

## Promotion

The exact request used the V8 capability identity and promoting build commit `c304fdeff6bd89f96e8397ef4192e9f83b41cb93`. The endpoint returned HTTP `201 Created`. The coordinator created:

- Record family: `.request-region-v8-capability-acceptance-v1`
- Record ID: `c3efa482db11d5ecea93ddf9b5cce1fb01793ed25f0577a62a054bf783a8a125`
- Record path: `/data/region-transcription-capability-acceptances/c3efa482db11d5ecea93ddf9b5cce1fb01793ed25f0577a62a054bf783a8a125.request-region-v8-capability-acceptance-v1`
- File SHA-256: `2704d826896002c077c1eeb8d323c100d313546ef5a477eacb73deb0d15eb03e`
- Accepted by: `user.steve` (encoded in the canonical record)
- Accepted promoting commit: `c304fdeff6bd89f96e8397ef4192e9f83b41cb93`
- Governed evidence identity: `acceptance-evidence-ordinary-ingestion-10r7-v8-fidelity` with digest `34ec3c703aacb754c45fa58ddf941d7368e2b4cc2e373cb412eb99c4de30902b`

The provider status endpoint then returned HTTP `200` and `disposition: ACCEPTED`, with matching runtime and accepted promoting commits. The record is provider-neutral; provider/profile/model, Authorization Purpose, credentials, and execution permission remain separate gates.

## Legacy preservation and integrity

Exactly six legacy `.region-capability-acceptance-v2` records remain present with their pre-promotion digests unchanged. Exactly one V8 acceptance record now exists. The OI11R4A failed attempt and synthetic source `evidence-84d85f99-3a94-4101-86b2-8b8aa9aef0ae` remain unchanged. Production was not restarted or redeployed; the same container remained running with restart count `0`.

Programme stores remained `4 / 2 / 1 / 21 / 19 / 6 / 5`. Provider calls were OpenAI `0`, Claude `0`, external `0`, retries `0`. No evidence or provider payload left Parker.

## Boundary

This unit created capability execution authority only. It did not authorize or perform a provider transaction. The exact next unit is `OI11R4F — Governed Live V8 Synthetic Execution After Capability Acceptance`.
