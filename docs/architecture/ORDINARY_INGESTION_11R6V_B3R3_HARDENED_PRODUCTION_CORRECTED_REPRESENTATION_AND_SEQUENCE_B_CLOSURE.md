# OI11R6V-B3R3 — Hardened Production Corrected Representation and Sequence B Closure

## Status

ACCEPTED — CANONICAL — CLOSED

## Exact deployment

The owner accepted and production deployed exactly:

- source: `1f12d6f102f886a0d4b9f7ecf8f4b39521c4aa70`;
- runtime JAR SHA-256: `9caa2e0e676c7202d4b76f893175f66ca9385af3d53e93fe30c5d8cd53d54f4a`;
- image: `sha256:2abe33e682a3138382e86e1008cda8e12a9a64e3dc6f8e20f354c250203c1791`;
- OCI config: `sha256:facd9f95ccd4721d41a793c5e81c4d9bde9a0009debcb2169fceea586a167bef`.

There was no rebuild, pull, substitution, or unrelated service recreation. The resulting production container is `a7dd28f20701cdafa59f2fc0515dda8e48d04fbd1b669d4deaa2b35ca93fd664`. Runtime status is running, startup logs record `Runtime started`, and restart count is 0.

## Opaque owner verification

- role: `OWNER`;
- opaque principal: `owner-8990f22b55d93f20516266fc8019159b5dd36b30b7beaf5c0fbdbc8e3e8ce533`;
- purpose: `document-ingestion.human-transcription-correction`;
- authority target: exact evidence/source/preparation/provider-generation/generation-digest/content-digest/canonical-review binding.

The external credential is stored outside Git at the separately protected host boundary and mounted read-only at `/run/secrets/parker_owner_high_authority_verification`. Final verified mount state is a regular file, UID/GID 999, mode `0400`, 48 bytes, and Docker mount `RW=false`. No credential value is present in this report, Git, provenance, audit, or command output.

The first startup attempt failed closed because local bind-backed Compose secrets ignore requested mount ownership and the original root-owned mode `0400` file was unreadable by runtime UID 999. No governed correction state existed. File ownership was corrected, within the authorized prerequisite, to UID/GID 999 while retaining mode `0400`; the exact accepted image was recreated and started successfully. No governance rule or runtime implementation was weakened.

## Canonical corrected representation

- representation ID: `human-corrected-33b284f033c8e250aee0a55f678745d445b9f7fd0be71d1ccf05aff4eae87208`;
- corrected content SHA-256: `8f96a2f9cac3dff5a124761f47006f6e032735fa38e90894f52b5839904b62cd`;
- stored-record SHA-256: `cd48d23c754391cbea92da5b8ac9bdfbe807ebbf42da7e449e0c4c2608535eb2`;
- correction acceptance ID: `correction-acceptance-e00c02f593436786557599ab89bec031fc4418b80ef176a7e886bb277e26ec5a`;
- canonical review ID: `review-3cf3186ca166acb0f4b6331ca574926dc874225247b296fb972666504992ea6e`;
- corrected representations: exactly 1.

Exactly two independently location-bound corrections were applied:

1. discrepancy `discrepancy-1eddfee4d1c3fe32ea8cf712163741f5cf29e58b842c1f5e92a989c9ae571710`, page 1: `Kellee` → `Kellec`;
2. discrepancy `discrepancy-67ac8799a94bb5cbb11a243895dd5468d64cc7a1ac1c164c3fa826d26ff53437`, page 5: `Kellee` → `Kellec`.

The B3R1 literal-region code-point mapping was used. Pages 2–4 and all non-target content are byte-for-byte unchanged relative to the provider representation. No fuzzy or global replacement occurred.

## Provider versus corrected retrieval and eligibility

Canonical governed readback proves:

- provider page 1 retains `Kellee`;
- provider page 5 retains `Kellee`;
- corrected page 1 contains `Kellec`;
- corrected page 5 contains `Kellec`;
- provider source-confirmed whole-generation eligibility remains `DENIED — MATERIAL_DISCREPANCY`;
- corrected-representation source-confirmed whole-generation eligibility is `ELIGIBLE`.

The provider representation and corrected representation remain separately immutable, retrievable, and provenance-bearing.

## Provenance and audit

The corrected representation is deterministically bound to the original evidence, exact preparation, provider generation and content integrity, canonical human review, the two discrepancy/source-resolution facts above, correction purpose, opaque owner principal, acceptance, and corrected content identity.

- evidence: `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`;
- source SHA-256: `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`;
- preparation: `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f`;
- provider generation: `region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6`;
- provider generation SHA-256: `9fb18b02db5ac55e5d446cd48ebc619de929c4596f94d2a11fba1a07da71af14`;
- provider content SHA-256: `18a6ed08a4729350027d3140dc0f07dd49d32c04aa45f9e3e9558df5d007c4eb`.

Exactly two narrow append-only audit facts exist:

- `correction-audit-76bd057812aecbf42431443ec1e535020d8a92bf06386bc564bfef2277a81667`;
- `correction-audit-f73cecbc93a519f00a5dda0de19f4e78d3adfb2be140eca8cfea33d03b083e43`.

They record prepared/published facts only. The verification credential is structurally absent.

## Historical immutability and provider accounting

- canonical human review records/audit facts: 1/3;
- canonical human review stored SHA-256: `13e6f5e285d95e19c0926821b63422486e005d22ee484feb70a6b54635046106`, unchanged;
- R6 provider derivative: exactly one, unchanged;
- derivative generation/content store counts: 23/21, unchanged;
- provider attempt/state counts: 10/8, unchanged;
- provider budget: 1 authorized / 1 consumed / 0 retries.

During B3R3, OpenAI calls, Claude calls, other provider calls, retries, and external evidence egress were all zero.

## Closure decision

ORDINARY INGESTION 11R6 SEQUENCE B IS ACCEPTED, CANONICAL, AND CLOSED.

The immutable historical provider representation is preserved and remains ineligible for source-confirmed use because of its two material discrepancies. The separately governed, create-once human-corrected representation applies only the two exact accepted source resolutions, is durably retrievable with complete provenance and audit, and is source-confirmed eligible. No retranscription, inferred correction, provider retry, external reasoning, or external egress occurred.
