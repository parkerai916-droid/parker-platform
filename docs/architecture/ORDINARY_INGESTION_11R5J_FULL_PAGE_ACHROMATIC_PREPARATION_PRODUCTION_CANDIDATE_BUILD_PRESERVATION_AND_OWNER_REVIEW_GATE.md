# OI11R5J — Full-Page Achromatic Preparation Production Candidate Build, Preservation and Owner Review Gate

## Stop state

The exact OI11R5I source has been built into a production candidate, verified from inside the artifact, and preserved. **No artifact acceptance has been created and no deployment is authorized.** This unit stops for Steven's visual inspection of all five RGB/grayscale page pairs and explicit acceptance or rejection of the exact artifact identities below.

Acceptance of this artifact would authorize deployment of this exact binary only. It would not authorize production preparation of the Deed, provider execution or transmission, retry, transcription, external reasoning, or implementation-bound V8 capability acceptance. Those remain separate later gates.

## Starting repository and production state

Repository HEAD and upstream were both exactly `fe13047df0dd5f155d6a6921acf7bc85541af26f`; branch was `main`, worktree was clean and `git diff --check` passed before build.

Before R5J, production container `34e0637eaa2b32c3e4c43b3e29c274b3da2d7a59c641cc5a2e25143465ba36b4` was running implementation `9e2a900fee388ebf4787817c24f34a63190b3f0d`, image `sha256:c121ea0d5c55c32a8cec9b38eade8ecb5f2d72f5331a8ed761b10fb8cfef0ae4`, restart count 0. The non-egress diagnostic returned `ordinaryExecutionReady=true`, `overallReady=true` and no reasons. The exact implementation-bound V8 acceptance record was present, so evaluator state remained `ACCEPTED`.

## Exact source and build environment

Source is exactly `fe13047df0dd5f155d6a6921acf7bc85541af26f`. The source-to-production-base comparison showed no changes to `Dockerfile`, `build.gradle.kts`, settings, Gradle wrapper or `tools`; only the accepted R5I Kotlin implementation differs from the running source. Host architecture was `x86_64`/`amd64`; candidate platform is `linux/amd64`. Docker Engine was 29.7.2 and BuildKit 0.32.2.

An initial full-Dockerfile build used `--network=none --pull=false`. It stopped before application compilation because its uncached build stage attempted Gradle/apt dependency resolution, which the disabled build network correctly prevented. It produced and loaded no candidate artifact. No dependency or application bytes were downloaded.

The bounded offline procedure then used the already verified local caches:

```text
PARKER_BUILD_COMMIT=fe13047df0dd5f155d6a6921acf7bc85541af26f ./gradlew installDist --offline --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g
```

It passed. The exact distribution was layered with Docker network disabled and pull disabled over a local tag of the current, already accepted production runtime image. This is equivalent for this source delta because all container/runtime/dependency inputs are unchanged; it replaces `/opt/parker` and unchanged `tools` only. OCI label build timestamp is `2026-09-01T13:02:12Z`; image creation timestamp is `2026-09-01T13:02:58.597889227Z`. No production Compose file or service was used or changed.

## Exact candidate identities

| Identity | Exact value |
|---|---|
| Source | `fe13047df0dd5f155d6a6921acf7bc85541af26f` |
| JAR SHA-256 | `fd259f70b58843a2bee8955edb98a767b89b0307643d8fe57eb155f22448fe89` |
| Embedded `Parker-Source-Commit` | `fe13047df0dd5f155d6a6921acf7bc85541af26f` |
| OCI index/image | `sha256:dea81e5d8d2a339cd0da407716ac532ca58320b81f8f932d68775cf8b8d0535f` |
| Linux/amd64 platform manifest | `sha256:1a0c0fee5dba98c2ff66ccc4718b327b3a561541bd9b1f04a92088d4f260eb53` |
| OCI config | `sha256:1ae20b2d046996ac2a07abc8f624c48ef2f544171080d84a4db5e8d28abc5d6f` |
| Provenance attestation manifest | `sha256:2c9fa54424d9ce2fe90f368834a1637680991439a60a6363b9f96bc44838f30b` |
| Tag | `parker-oi11r5j-fe13047:candidate` |

All four reported OCI digests were recomputed successfully from their exact blobs in the preserved archive. Image labels bind revision, creation time and `parker-oi11r5j-full-page-achromatic-candidate` title. The JAR inside the image has the same SHA-256 as the host-built JAR, and its manifest contains the complete required 40-character source identity.

## Artifact semantic verification

Static inspection of the packaged JAR—not merely Git source—confirmed `FullPageAchromaticPreparer`, `ChromaticRiskGate`, `DeterministicAchromaticPngEncoder`, `FullPageAchromaticCanonicalRequestRegionV8Builder`, `FileSystemFullPageAchromaticPreparationStore`, codec and execution-preparer classes. Constant/class inspection confirmed profile `full-page-achromatic-png-preparation-v1`, base64/body gates 14,500,000/16,000,000, explicit provider `com.sun.imageio.plugins.png.PNGImageWriter`, chromatic `requireSafe`, deterministic `convert`/`encode`, full-page builder and create-once `persist`/`read` paths. The R5I full suite remains the authoritative proof of the exact 10,875,000 binary gate, frozen integer luma formula, coverage/order, chromatic fixtures, conflict behavior and historical R5F readback.

The artifact's own `FullPageAchromaticLocalAcceptanceCli` was executed in a disposable `--network none` container against read-only copies of the five governed authoritative pages. It returned 5 pages, 5 preparation regions, 5 request regions, order `[1,2,3,4,5]`, PNG total 9,147,493 bytes, base64 total 12,196,664 characters, and body 12,202,028 bytes. All five transport PNG SHA-256 values exactly match R5I.

The candidate runtime is Eclipse Temurin 17 while R5I local acceptance used Ubuntu OpenJDK 17. Because R5I deliberately binds governed JDK runtime identity into preparation provenance, candidate preparation/request identities are correctly new: preparation identity `3e14f4402b0c0055d33813e50e63558f3d9179a5ee30cfced8297f6e3ee55073`, region-set digest `b73c6c9790edd7a073d384e7eb9c2b4ac2c58104aeca1ce4fe8d1979aabf64bb`, request digest `677c7d2f76fefd2f582f93c6f654ff85327f3159fa1d4a433269f4854840a2da`, and body digest `0e66a6cdcd8a4fb3e80cd421570554a07285832dbb260e39634d29cbafcded63`. This does not redefine the authoritative R5I host results: their values remain `4f9be66a…`, `6302414e…`, `3a1254e5…` and `eadbaa86…`. The transport pixels/bytes and V8 semantics are identical; the provenance input truthfully differs.

## V8 identity

Artifact execution reported capability `ordinary-external-request-region-transcription-v8` and digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`. Request/response schema, Parker-authoritative order, forensic provider ordinal, literal/uncertainty semantics, response reconstruction and raw-before-parse remain unchanged. The candidate has no production implementation-bound capability acceptance.

## Preserved archive

The exact candidate is preserved at:

`/mnt/parker-data/parker/replacement-candidates/oi11r5j-full-page-achromatic-fe13047-20260901.tar`

Archive SHA-256 is `db3c74049c6868f8bb8aa85223e0e2179b19c28526799ede4bf4e261d2c8cd94`; exact size is 908,298,752 bytes; mode is 600; owner/group are `steve:steve`. The archive contains image/index `sha256:dea81e5d8d2a339cd0da407716ac532ca58320b81f8f932d68775cf8b8d0535f`. No artifact-acceptance record exists for it.

## Review-package verification and R5J bundle

All 11 files in `/home/steve/parker-owner-review/oi11r5i/` matched the R5I report. Its manifest remains SHA-256 `cf7f14b384a87d37175557785d9f5d1502a24926862e357d5d2bb6c61e8e6221`.

The review-only convenience bundle is `/home/steve/parker-owner-review/oi11r5j/`. It contains clearly named `page-N-rgb.png` and `page-N-grayscale.png` pairs, the unchanged R5I manifest, and `review-manifest.txt` (SHA-256 `25551159e55e948e1b0c6f36742397ce077f30daba4dc2be0bd70735e023b29e`). The R5I manifest copy is `cf7f14b…`. Page pair digests are:

| Page | RGB SHA-256 | Grayscale SHA-256 |
|---:|---|---|
| 1 | `ce45f467fbadca484750af31966f5a0cceeea337a11e998d132a7bd77918d434` | `17dbc36d7df4db281d8052bac6ef5a14a5dc04d6ead57413efd707ea3a49504c` |
| 2 | `45906134d0357462b10a5bee3347d4f8d347d54ba8a3e2cf67787ab8d5f14543` | `4de7ea456db529899be8d9ad714a7e3318af70c3a2c68b4c55bd8f75656e1dae` |
| 3 | `95e8b475bd095b7190c7e7f56cbd404af9bc30b12dfb80d6e32fdbe82d4d7ace` | `b0c37fa3032e545d80eeac5da371862586115f8304c41e44c11a63603a723705` |
| 4 | `3de037da9dc9b89fe9007db53b9dda8bf7aaf458791757246f874a79ff384818` | `c481b51bd68d93bd0346237c038c7d4768a4174fa2d42922a30fb4e5630a578b` |
| 5 | `975861968a5deea22d0cddf9f9a7ecebee6770983936fd2d8e5672ed406fd36f` | `3b4f992240518ddbb872fddd65f58c130629c8affd1d0f36d783100c4d1a9816` |

Steven must visually assess ordinary/faint/small print, scan background where relevant, handwriting, signatures, page-5 blue handwriting, stamps/marks and all margins/edge content. Automated PASS does not imply visual or artifact acceptance.

## Production and store integrity

After build, isolated verification, preservation and review staging, production is still the same container, source, image, start timestamp and restart count 0. Readiness remains PASS and the exact `9e2a900…` V8 acceptance remains present/evaluator `ACCEPTED`.

Pre/post production file counts were identical:

| Store | Before | After |
|---|---:|---:|
| evidence | 29 | 29 |
| source manifests | 29 | 29 |
| capability acceptance | 9 | 9 |
| owner authorization | 11 | 11 |
| attempts | 8 | 8 |
| provider state | 6 | 6 |
| derivative generations | 22 | 22 |
| derivative content | 20 | 20 |
| region acceptance authority | 1 | 1 |

Governed production-store delta is 0. R5J added only the protected candidate archive, review-only bundle and this repository report.

## Exact owner acceptance proposition

- Source: `fe13047df0dd5f155d6a6921acf7bc85541af26f`
- JAR SHA-256: `fd259f70b58843a2bee8955edb98a767b89b0307643d8fe57eb155f22448fe89`
- OCI index/image: `sha256:dea81e5d8d2a339cd0da407716ac532ca58320b81f8f932d68775cf8b8d0535f`
- Platform manifest: `sha256:1a0c0fee5dba98c2ff66ccc4718b327b3a561541bd9b1f04a92088d4f260eb53`
- OCI config: `sha256:1ae20b2d046996ac2a07abc8f624c48ef2f544171080d84a4db5e8d28abc5d6f`
- Preserved archive: `/mnt/parker-data/parker/replacement-candidates/oi11r5j-full-page-achromatic-fe13047-20260901.tar`
- Archive SHA-256: `db3c74049c6868f8bb8aa85223e0e2179b19c28526799ede4bf4e261d2c8cd94`
- Archive size: `908,298,752` bytes
- Embedded source: `fe13047df0dd5f155d6a6921acf7bc85541af26f`
- Profile: `full-page-achromatic-png-preparation-v1`, version 1
- V8 capability: `ordinary-external-request-region-transcription-v8`
- V8 digest: `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`
- Review bundle: `/home/steve/parker-owner-review/oi11r5j/`
- Review manifest SHA-256: `25551159e55e948e1b0c6f36742397ce077f30daba4dc2be0bd70735e023b29e`

OpenAI calls: 0. Claude calls: 0. Other external calls: 0. Retries: 0. External egress: 0. No provider authorization or attempt state was created. Build and artifact verification were network-disabled; no Deed bytes left the host.

UNIT ORDINARY-INGESTION-11R5J PAUSED AT OWNER VISUAL REVIEW AND ARTIFACT ACCEPTANCE — THE EXACT OI11R5I PRODUCTION CANDIDATE HAS BEEN BUILT, SEMANTICALLY VERIFIED AND PRESERVED, WHILE THE CURRENT PRODUCTION RUNTIME REMAINS UNCHANGED. THE FIVE-PAGE RGB/GRAYSCALE OWNER-REVIEW PACKAGE HAS BEEN VERIFIED AND STAGED FOR STEVEN. NO ARTIFACT ACCEPTANCE, DEPLOYMENT, IMPLEMENTATION-BOUND V8 ACCEPTANCE, REAL-DOCUMENT PRODUCTION PREPARATION, PROVIDER AUTHORIZATION, PROVIDER CALL, RETRY OR EXTERNAL EGRESS OCCURRED. FURTHER PROGRESS REQUIRES STEVEN TO INSPECT THE TRANSPORT TRANSFORMATION AND EXPLICITLY ACCEPT THE EXACT CANDIDATE ARTIFACT.
