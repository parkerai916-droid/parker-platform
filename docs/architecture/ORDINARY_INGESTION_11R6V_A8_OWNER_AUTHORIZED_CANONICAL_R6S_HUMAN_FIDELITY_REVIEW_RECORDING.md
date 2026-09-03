# OI11R6V-A8 — Owner-Authorized Canonical R6S Human Fidelity Review Recording

## Verdict

**A — CANONICAL R6S HUMAN FIDELITY REVIEW RECORDED AND VERIFIED**

## Starting state and owner authorization

The unit started on branch `main` with HEAD/upstream `04c90c3149de41aa799abdd027267f6629e1b32e` and a clean worktree. Steven Francis McTague explicitly authorized exactly one canonical recording of the already-completed R6S human fidelity review. The authority excluded provider access, retranscription, retry, correction, corrected representation, external reasoning, external evidence egress, unrelated reviews, and any second semantic review act.

The frozen R6T Scope Lock SHA-256 was `f90c9fc654136ea5e92723a1704ad58108ab0f8a9e73a401e8039d3210e4cd2a`; the frozen R6V Implementation Plan SHA-256 was `86f7b27095a6b80b2618556797a877a21b097f76c3c9ba2d91e235b90c395d1d`.

## Pre-mutation production and artifact gates

Production matched the required deployed artifact:

- Container: `ccf93adcaf7b37e12eb5d8f93c7419d588d713c03881420b49021e5dd8e1b707`
- Image: `sha256:51eff5a7060b478ec66b9ad6e42b56b4ec920d142a984b6e5e7e13dce56f89f5`
- Source: `01fd54237227daff7d0b83064825dd004c9fa1f6`
- Runtime JAR SHA-256: `dc04f7c3498607f35b721348087389f7b1c15e9064ed8a98c5e11c765b2b981c`
- Runtime status/readiness: running / PASS (`Runtime started`)
- Restart count: `0`
- Configured owner PrincipalId: `user.steve`

Before mutation, the human-fidelity review and audit roots contained zero files. The local R6S provenance artifacts were verified exactly:

- Completed worksheet SHA-256: `8e7928c671cd36c7a4517dc5d9429706c46efb65c565e948684d6c3e7c8773a4`
- Owner-review record SHA-256: `2d47f50e0f2915bd0e18e914eac4bd5abc879cf5419969d482b2b7f6ff6b1293`
- Package checksum-file SHA-256: `7b4bd346b22976b75976970ff189eb59403ecf633577820941bf7c72eeea99e5`

All historical store counts and hashes matched the A7 baseline. The provider path was not a dependency of human-review recording.

## Exact target, authority, and resource

The canonical six-part target was:

1. Evidence: `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`
2. Source SHA-256: `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`
3. Preparation identity: `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f`
4. Derivative generation: `region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6`
5. Generation SHA-256: `9fb18b02db5ac55e5d446cd48ebc619de929c4596f94d2a11fba1a07da71af14`
6. Content SHA-256: `18a6ed08a4729350027d3140dc0f07dd49d32c04aa45f9e3e9558df5d007c4eb`

The reviewer was the configured production owner `user.steve`. The exact active purpose was `document-ingestion.human-fidelity-review-recording`. A5's exact-target registrar derived and registered resource `human-fidelity-review-target-a255e18a3742372a3efb0ed07d8bafdb9f8340aa7ce6abd9b8565a28f86a902e`. A3 permission evaluation returned authorized before any A2 review or audit mutation.

## Governed recording path

Because A5 deliberately introduced no public write endpoint, the governed act used a one-shot invocation of the exact accepted image, with network mode `none`. It instantiated the production `ParkerRuntime` composition, used temporary roots for unrelated subsystems, mounted only the production review/audit roots writable, registered the exact target through `HumanFidelityReviewExactTargetRegistrar`, and invoked the composed `DefaultGovernedHumanFidelityReviewRecordingService`. It did not write store files directly.

The first attempt failed before review construction because runtime UID 999 could not traverse the owner package's intentional `0700` directory. Inspection proved zero review/audit files. Two read-only temporary copies of the required page transcription exports were then made and proven byte-identical (page 1 SHA-256 `d064b5bd2d9d408f0a24c9fdbc6111f98181415da83b31a105fe450ed13d7cbd`; page 5 SHA-256 `bd565520b36193809495f330c4827f3aaa05e54ee57c229ef22c6b8a509249fd`). The package and its artifacts were not modified.

The successful network-disabled invocation performed exact request validation, exact target registration and A3 authorization, A2 prepare and PREPARED audit, create-once publication and PUBLISHED audit, canonical A2 retrieval, byte-exact codec comparison, and then the single required duplicate/idempotency call. It returned `Recorded`, followed by `AlreadyRecorded`. No second review was created.

## Canonical review identity and semantics

- HumanFidelityReviewId: `review-3cf3186ca166acb0f4b6331ca574926dc874225247b296fb972666504992ea6e`
- Canonical payload SHA-256: `329209ed3e9c6e474bcd88e8d707701bfdbec97650e157323b0b2e5b75a6ede0`
- Canonical encoded/stored-record SHA-256: `13e6f5e285d95e19c0926821b63422486e005d22ee484feb70a6b54635046106`
- Stored record size: `3,521` bytes
- Review state: `HUMAN_REVIEWED_WITH_DISCREPANCY`
- Coverage: `FULL_GENERATION`, pages `[1,2,3,4,5]`
- Descriptive fidelity: `high overall fidelity`
- Review records for exact target: `1`

Canonical A2 readback exactly equalled the requested immutable record.

## Exact discrepancies and pattern

Page 1:

- Discrepancy ID: `discrepancy-1eddfee4d1c3fe32ea8cf712163741f5cf29e58b842c1f5e92a989c9ae571710`
- Preparation region: `d9e87671239b8d7fab1d3c0b6790c250d5f7579b3aec07309fba0caf142ee1c2`
- Derivative region: `e70de3115d04d3cff1112fe3861dec80618a222605c3be2486d7af8aabf67cff`
- Block index: `0`
- Half-open Unicode code-point span: `[151,157)`
- Provider/source values: `Kellee` / `Kellec`
- Classification/severity/cause: `TRANSCRIPTION_DIFFERENCE` / `MATERIAL` / `UNKNOWN`
- Source resolution: human-resolved against authoritative page representation `33d341f5f169ea09a6cdeffc50c731a6b9d58e2a646ffb1ac32532bee2afff1e`

Page 5:

- Discrepancy ID: `discrepancy-67ac8799a94bb5cbb11a243895dd5468d64cc7a1ac1c164c3fa826d26ff53437`
- Preparation region: `ef79c2e21276979b69d2221301812b65fcfccb8186880906eb5163a991b7f9ba`
- Derivative region: `b4e89df3a5c84f3b18e6f95e4d14861969b9cdc41f23c186c34cf0f2ef998df9`
- Block index: `0`
- Half-open Unicode code-point span: `[89,95)`
- Provider/source values: `Kellee` / `Kellec`
- Classification/severity/cause: `TRANSCRIPTION_DIFFERENCE` / `MATERIAL` / `UNKNOWN`
- Source resolution: human-resolved against authoritative page representation `eb7ea4a7c78af09554f52ad63fcfe7b122b9bb5b23563a488b269fdd9bf23c44`

The single descriptive association is pattern `pattern-db1478d93532196892bb126bd35d2f84b13596de04e6e7fe0634bc99794cd9cb`, containing exactly those two discrepancy IDs. It confers no correction or global replacement authority and establishes no technical cause.

## Audit and duplicate result

Exactly three immutable audit facts exist:

- `REVIEW_PREPARED` / `SUCCEEDED`: `fidelity-audit-5a165cca0df1c4b795077fde8d09fb6281d1e20923bcf5b4070a96907e2f8224`
- `REVIEW_PUBLISHED` / `SUCCEEDED`: `fidelity-audit-43b9477d5b833302fdce4d90e406fb36044406d78772d30dfa2ba6f65e228513`
- `REVIEW_DUPLICATE_CONFIRMED` / `EXACT_DUPLICATE`: `fidelity-audit-5cc5a1309e763e0934ac262c23944510e1da248f1f99dcf9129e8853aabb8bc1`

The exact duplicate call returned `AlreadyRecorded`; review count remained one. The three audit-file SHA-256 values are respectively `1fe7c4edddd6986e352578dd4334c56a020a6981aa61b033cd77f87d3c3b2967`, `9bd03f951e11d2c77eb76328ace4e72199ba6f7e62faf8c34be036751046beef`, and `43282b75b924544d6f67a82f6828a6996e2de6ddd4b5f83769a376f43e3185ea` when ordered by event above.

## Historical immutability and production stability

Pre/post counts and aggregate hashes were identical for evidence (29), source manifests (29), corrected preparations (6), capability acceptances (14), owner authorizations (14), provider attempts (10), provider state (8), derivative generations (23), derivative content (21), and document-ingestion audit (1). The exact aggregate hashes remained those recorded in A7.

The evidence SHA-256 remains `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`; generation SHA-256 remains `9fb18b02db5ac55e5d446cd48ebc619de929c4596f94d2a11fba1a07da71af14`; content SHA-256 remains `18a6ed08a4729350027d3140dc0f07dd49d32c04aa45f9e3e9558df5d007c4eb`. Preparation `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f`, execution `ordinary-exec-3c2bf685-d6c2-44e0-acf8-0224d92fd976`, provider state `2b1fbe06ebee0b7a3fdb618159c6987fa713976d7bfd2732b9048b50f11df3a7`, raw-response SHA `4706c24b8b0b83675a8ded1165f316229fa61a92bff4d8fe0a16c1d7d50cfb4a`, response ID `resp_04aa0adc3e021174016a980c0c891487d09764395f58adef7b`, model `gpt-5.6-sol`, and provider budget (maximum 1, consumed 1, retries 0) remain unchanged. There is still exactly one R6 provider derivative.

Production remains container `ccf93adcaf7b37e12eb5d8f93c7419d588d713c03881420b49021e5dd8e1b707`, running the accepted image/source/JAR, readiness PASS, restart count zero. The two host storage roots remain owned UID 999/GID 1001 and mode `2775`.

## Provider, correction, and outstanding scope

OpenAI calls: `0`; Claude calls: `0`; other provider calls: `0`; provider retries: `0`; external evidence egress: `0`. No external reasoning occurred.

The immutable provider transcription still contains `Kellee` at both locations. No provider derivative bytes changed; no correction purpose, correction proposal, correction acceptance, corrected representation, or `HUMAN_CORRECTED_REGION_TRANSCRIPTION` exists.

Still outstanding under frozen Sequence A are effective-review projection beyond this minimum canonical record, conflict/supersession/adjudication projection, purpose-specific source-confirmed eligibility, retrieval/presentation integration, production verification of those semantics, and final R6 closure. Canonical review presence alone does not implement source-confirmed eligibility.
