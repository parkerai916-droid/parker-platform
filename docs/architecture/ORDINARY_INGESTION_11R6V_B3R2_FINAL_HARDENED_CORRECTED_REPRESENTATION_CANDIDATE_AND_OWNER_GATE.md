# OI11R6V-B3R2 — Final Hardened Corrected-Representation Candidate and Owner Gate

## Status

BUILT AND VERIFIED — PENDING EXACT OWNER ARTIFACT ACCEPTANCE

## Exact artifact identities

- Source commit: `1f12d6f102f886a0d4b9f7ecf8f4b39521c4aa70`.
- Runtime JAR SHA-256: `9caa2e0e676c7202d4b76f893175f66ca9385af3d53e93fe30c5d8cd53d54f4a`.
- Runtime JAR size: 5,076,129 bytes.
- Image ID / local image-manifest digest: `sha256:2abe33e682a3138382e86e1008cda8e12a9a64e3dc6f8e20f354c250203c1791`.
- OCI config digest: `sha256:facd9f95ccd4721d41a793c5e81c4d9bde9a0009debcb2169fceea586a167bef`.
- Image size: 1,032,268,257 bytes.
- Platform: `linux/amd64`.
- Runtime user: `parker`.
- Entry point: `/opt/parker/bin/parker`.
- Revision label: `1f12d6f102f886a0d4b9f7ecf8f4b39521c4aa70`.

The established bounded offline build completed successfully. The installed-distribution JAR is byte-identical to the runtime JAR and embeds `Parker-Source-Commit: 1f12d6f102f886a0d4b9f7ecf8f4b39521c4aa70`. A stopped temporary container confirmed the final image contains that exact JAR and was then removed.

## Focused smoke verification

Focused correction, permission-policy, runtime-configuration, and production-composition tests passed:

- suites: 4;
- tests: 66;
- skipped: 0;
- failures/errors: 0/0.

The smoke gate verifies:

- the B3R1 literal-region Unicode code-point offset correction;
- exactly two location-bound R6 `Kellee` → `Kellec` corrections;
- unchanged raw provider representation and denied provider source-confirmed eligibility;
- source-confirmed eligibility of the corrected representation;
- opaque `OWNER` principal enforcement;
- external verification credential-file enforcement;
- credential plus exact correction purpose plus exact target/review authorization;
- missing/wrong credential, principal, purpose, and target denial before correction mutation;
- redacted credential rendering and structural absence of credentials from audit facts;
- no provider/retranscription dependency.

Per instruction, the settled full suite was not rerun. The B3R1 full-suite baseline remains 262 suites, 3,407 tests, 18 skipped, zero failures/errors.

## Candidate capability and secret exclusion

Final-image inspection confirms the governed correction service, literal-region mapping fix, opaque owner principal contract, redacted verification credential wrapper, external-file verifier, correction authorization policy, and corrected-representation eligibility path are present.

No production verification credential was generated for this build. No password, token, private key, or credential value is present in Git, the image metadata, build command, logs, report, or audit schema. Synthetic test-only values are not production credentials and are not packaged in the runtime distribution.

## Production secret prerequisites

The exact proposed protected host path is:

`/mnt/parker-secrets/parker/owner-high-authority-verification.secret`

It does not currently exist and was not created in B3R2. Before deployment, a separately authorized host-preparation act must:

1. create `/mnt/parker-secrets/parker` outside Git;
2. generate at least 32 bytes of high-entropy credential material without printing it or placing it in shell history;
3. write it atomically to the exact file above;
4. protect the host file as `root:root`, mode `0400`, with non-symlink verification;
5. set `PARKER_OWNER_HIGH_AUTHORITY_VERIFICATION_SECRET_FILE` to that path;
6. allow Compose to mount it only into the Parker container as `/run/secrets/parker_owner_high_authority_verification`, runtime UID/GID 999, mode `0400`.

The selected non-secret opaque production principal is:

`owner-8990f22b55d93f20516266fc8019159b5dd36b30b7beaf5c0fbdbc8e3e8ce533`

Deployment must set `PARKER_OWNER_HIGH_AUTHORITY_PRINCIPAL_ID` to exactly that identifier. It contains no legal name and is not an authentication secret.

## Production and provider accounting

- Deployment: not performed.
- Production mutation: none.
- Production governed-state delta: 0.
- Provider calls: 0.
- Retries: 0.
- External evidence egress: 0.

Exact owner acceptance is required for the source, JAR, image/manifest, and OCI config identities above before deployment or secret provisioning.
