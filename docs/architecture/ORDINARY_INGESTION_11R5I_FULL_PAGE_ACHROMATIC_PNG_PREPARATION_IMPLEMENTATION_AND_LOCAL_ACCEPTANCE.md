# OI11R5I — Full-Page Achromatic PNG Preparation Implementation and Local Acceptance

## Result and boundary

OI11R5I implemented the owner-accepted `full-page-achromatic-png-preparation-v1`, version 1, from accepted R5H commit `89ee36afbfc28a32e3e23a9436f46deaa98b6fc5` and document SHA-256 `b02da5e1a4207e9c6384a655cfb0a516f1e3822c25b3f7c037d02e186daf4c29`.

The result is **READY FOR SEPARATE ARTIFACT BUILD AND OWNER ACCEPTANCE**. This unit did not build or preserve an accepted production candidate, deploy, restart production, promote capability state, authorize execution, construct attempt state, contact a provider, or transmit the Deed. The local canonical request was built in memory solely to verify shaping, identity and exact size.

## Implementation

Changed files:

* `src/runtime/FullPageAchromaticPreparation.kt` — profile constants, complete-page preparation and provenance types, frozen luma conversion, chromatic-risk gate, deterministic encoder, identities, create-once store/codec, local acceptance entry point;
* `src/runtime/OrdinaryRequestRegionV8TruthfulContract.kt` — dedicated achromatic canonical builder, preparation evidence on canonical construction, exact aggregate image/base64/body gates;
* `src/runtime/OrdinaryRequestRegionV8Execution.kt` — future V8 request preparation selects the accepted R5I builder;
* `tests/runtime/FullPageAchromaticPreparationTest.kt` — synthetic fidelity/chromatic/persistence/determinism tests, PDF construction path, and registered Deed local acceptance;
* this report.

The historical `CanonicalRequestRegionV8Builder` remains unchanged and available for exact old-fixture replay. The execution preparer now selects `FullPageAchromaticCanonicalRequestRegionV8Builder`. Corrected geometry/order is stored under a new preparation identity by `FileSystemFullPageAchromaticPreparationStore`; it is not written through the page-representation-keyed R5F geometry/order paths.

## Geometry, order and coverage

Each authoritative page produces exactly one `SourceRegion` with bounds `[0,0,width,height)`, profile/version `full-page-achromatic-png-preparation-v1/1`, structural class `MIXED`, no graph edges and `UNAMBIGUOUS`. Its persisted state is `DETERMINISTIC_SOURCE_ORDER`. Page order must be the complete ascending sequence `1..pageCount`; gaps, duplicates, cross-source pages, more than 32 pages and invalid page authority fail closed.

For the Deed, every page is 2479×3508 and each full-page region has area 8,696,332 pixels. With one rectangle per page, the union equals the page, pixel multiplicity is exactly one, distinct same-page intersection/containment pairs are zero, and there are no internal boundaries. No content detection, exclusion, overlap, OCR or semantic ordering exists.

## Grayscale transform and deterministic encoder

The implementation reads unsigned RGB samples and applies exactly:

`Y = (77R + 150G + 29B + 128) >> 8`

It produces one sample per source pixel and performs no resize, crop, threshold, denoise, sharpening, contrast change, binarization or palette quantization. Transform identity is `srgb-integer-luma-77-150-29-v1`.

Encoder identity is `jdk17-imageio-png-byte-gray-max-compression-v1`. It selects only provider class `com.sun.imageio.plugins.png.PNGImageWriter`, independent of discovery order, creates `TYPE_BYTE_GRAY`, uses `MODE_EXPLICIT`, compression quality `0.0` (maximum lossless compression), no supplied metadata and no interlace. The governed local runtime was Ubuntu OpenJDK 17.0.20 (`17.0.20+8-1-26.04-Ubuntu`). Provider class, runtime identity, dimensions, color model, parameters, byte length and transport SHA-256 are bound into provenance and deterministic identity.

## Chromatic-risk and fidelity evidence

`ChromaticRiskGate` scans horizontal and vertical source-pixel adjacencies. It rejects after 32 sustained edges where RGB channel-distance sum is at least 160 but frozen-luma distance is at most 4. This deterministic, semantic-free rule detects material color boundaries that grayscale would collapse. It does not select an alternative profile.

Synthetic equal-luminance red/green, near-equal-luminance red/green, and chromatic mark/background fixtures all returned `CHROMATIC_RISK` and stopped. The remainder of the fixture suite covered clean and faint print, dense paragraphs, headings, all margins, handwriting, signatures, scan noise, mixed material, blank and nearly blank pages, and multipage ordering. Tests verified original dimensions, retained strokes, complete bounds, deterministic samples/bytes/identities and no omitted blank/noisy area.

The five registered Deed pages passed the gate. Local visual inspection of the staged full-page grayscale pages found printed content, faint scan material, handwriting and signatures visible at the original 2479×3508 scale. The blue handwriting on page 5 remains high-contrast after conversion. These transport derivatives do not replace the RGB authority.

## Identity, provenance and persistence

Preparation, region and document/region-set identities use domain-separated, length-prefixed SHA-256 fields. They bind evidence/source, page, authoritative representation and canonical-pixel identities, dimensions/full bounds, profile/version, transform rule/version, encoder/runtime/parameters, byte length, transport digest and order. Timestamps, paths and discovery order are absent.

The create-once store persists a canonical preparation record and transport PNGs under new preparation/transport identities. Identical re-admission is idempotent; conflicting bytes fail closed. Codec readback reconstructs and validates preparation, provenance, singleton deterministic order, image digest and transport bytes. Existing page rendering and R5F geometry/order codecs remain unchanged and full-suite historical tests pass.

The R5F page-1 region-set digest remains `4b8571e618e174adc4e8171bdf0fc1ab512e2a4f164abb11925bef93437cc73f`. Its observed geometry/order file SHA-256 values remained `8c8d9949b7fa9308381c3be3915e8ab5f78c4c0575cf30315481a4296565fcb4` and `99b594592e18d812da7750e84873071a6ed51604a4a8a17708bbf3cf3ed70e79`. No historical source region is a corrected constituent and no owner order was created.

## Registered Deed local acceptance

The immutable source linkage is evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`, source SHA-256 `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`, manifest SHA-256 `ec98834d794713ba2842506a9cabb6f200a0c0b19876f6724fc6da17e40c5e34`. The local harness consumed bounded copies of the five already governed authoritative page representations; it did not modify their source files or governed records.

Result: `5 pages → 5 preparation regions → 5 V8 request regions`, order `[1,2,3,4,5]`, all bounds `[0,0,2479,3508)`, R5F constituents `0`, order state `DETERMINISTIC_SOURCE_ORDER`.

Preparation identity: `4f9be66a5e910ddc85420221c1824e6ad7496b60720e8b3fe324edf3a432b8b1`

Region-set digest: `6302414e09c1c2350c0a16918f01cb01fb893fe3e518b669bfcf44d01ebb6e01`

| Page | PNG bytes | Transport SHA-256 | Preparation ID | V8 request-region ID |
|---:|---:|---|---|---|
| 1 | 1,380,662 | `17dbc36d7df4db281d8052bac6ef5a14a5dc04d6ead57413efd707ea3a49504c` | `1c95e12f64e424408e97201a2e1c5c3a818a4ce330fcb1c109193f5b15310ba1` | `6227635633168648e92c8986eaa3ddcc27a7bebe5eaf74407a457b0fd2c288d0` |
| 2 | 2,236,986 | `4de7ea456db529899be8d9ad714a7e3318af70c3a2c68b4c55bd8f75656e1dae` | `d52a215cc96a08ae2baadcbb5f603af03d7b1a95c3640b3042cec89a6b89812a` | `50d45783c051d97d91382014ec8e83090524764d0e555fff9c9b476d04b2d7d6` |
| 3 | 2,084,632 | `b0c37fa3032e545d80eeac5da371862586115f8304c41e44c11a63603a723705` | `42aeaf97dd9f50e9ac89987032af8eea50af43d1143d287c24fde62848660450` | `d7adcead2804bf779a0933f138a9d775c140dee1eaf94ba2fb1c67c8b8aebcc3` |
| 4 | 1,459,264 | `c481b51bd68d93bd0346237c038c7d4768a4174fa2d42922a30fb4e5630a578b` | `7c6c50e66f6de7a84fe5c7106307cd6663be7845d063b2d27b5bfba6b02c0c55` | `5585031ddae127c62247693ec108956797624b2ebe6ccef280459fc4038d68ba` |
| 5 | 1,985,949 | `3b4f992240518ddbb872fddd65f58c130629c8affd1d0f36d783100c4d1a9816` | `447e653ed1e0a67a850d25900de05ac12d5873ac56fd118d56d5122885a2bb06` | `c5f4301babbb3a2d0c093bd1eaa0c8e9b93a2f001703c5daa8bb93f6b2f3d265` |

Aggregate PNG bytes are **9,147,493** (`≤10,875,000`). Exact aggregate base64 characters are **12,196,664** (`≤14,500,000`). The exact canonical UTF-8 request body is **12,202,028 bytes** (`≤16,000,000` and `≤16,777,216`). Request binding digest is `3a1254e52df174cdef310435359538dca1aa15fb2e263a5b4cbc0536301e22c2`; provider-body digest is `eadbaa86326a9b002d92abffd259f88d43dd0091de761cf48fd459545e5226d3`. Two clean constructions reproduced every value and byte exactly.

## Owner-review package

Paired RGB/grayscale files are staged outside Git and authoritative storage at `/home/steve/parker-owner-review/oi11r5i/`. `review-manifest.txt` binds page, authoritative filename/SHA/representation ID, transport filename/SHA and preparation ID; its SHA-256 is `cf7f14b384a87d37175557785d9f5d1502a24926862e357d5d2bb6c61e8e6221`.

| Page | RGB SHA-256 | Grayscale SHA-256 |
|---:|---|---|
| 1 | `ce45f467fbadca484750af31966f5a0cceeea337a11e998d132a7bd77918d434` | `17dbc36d7df4db281d8052bac6ef5a14a5dc04d6ead57413efd707ea3a49504c` |
| 2 | `45906134d0357462b10a5bee3347d4f8d347d54ba8a3e2cf67787ab8d5f14543` | `4de7ea456db529899be8d9ad714a7e3318af70c3a2c68b4c55bd8f75656e1dae` |
| 3 | `95e8b475bd095b7190c7e7f56cbd404af9bc30b12dfb80d6e32fdbe82d4d7ace` | `b0c37fa3032e545d80eeac5da371862586115f8304c41e44c11a63603a723705` |
| 4 | `3de037da9dc9b89fe9007db53b9dda8bf7aaf458791757246f874a79ff384818` | `c481b51bd68d93bd0346237c038c7d4768a4174fa2d42922a30fb4e5630a578b` |
| 5 | `975861968a5deea22d0cddf9f9a7ecebee6770983936fd2d8e5672ed406fd36f` | `3b4f992240518ddbb872fddd65f58c130629c8affd1d0f36d783100c4d1a9816` |

## V8 and test verification

Capability remains `ordinary-external-request-region-transcription-v8`; digest remains `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`. The codec still supplies PNG with `detail=original`. Request schema/instruction, raw-before-parse, validation, Parker order, forensic provider ordinal, literal/uncertainty semantics and reconstruction are unchanged. No boundary deduplication or response change is needed because each page has one complete region.

The first ordinary compile attempt used repository defaults and failed transparently in `compileTestKotlin` with `java.lang.OutOfMemoryError: Java heap space`. The bounded remediation was `./gradlew test --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g` on host `parker`, Ubuntu OpenJDK 17.0.20, 30 GiB physical memory/about 20 GiB available. The complete offline suite passed: **250 suites, 3,310 tests, 0 failures, 0 errors, 17 skipped**. The six R5I tests passed. Live-model/external test source sets were not attached. `git diff --check` passed.

## Production and provider boundary

Production remains on implementation `9e2a900fee388ebf4787817c24f34a63190b3f0d`, image `sha256:c121ea0d5c55c32a8cec9b38eade8ecb5f2d72f5331a8ed761b10fb8cfef0ae4`, with no R5I deployment or restart. No production store was used for R5I preparation. No artifact acceptance or new implementation-bound capability acceptance was created.

Final accounting: OpenAI `0`; Claude `0`; other external `0`; retries `0`; external egress `0`.

The next work is the separate governed sequence: build and preserve the exact candidate; present identity; obtain explicit owner artifact acceptance; deploy; verify readiness/history; obtain and promote new implementation-bound V8 acceptance; only then generate corrected governed production preparation and obtain owner visual verification. No real-provider execution is authorized.

UNIT ORDINARY-INGESTION-11R5I COMPLETE — THE OWNER-ACCEPTED FULL-PAGE-ACHROMATIC-PNG-PREPARATION-V1 PROFILE HAS BEEN IMPLEMENTED AND VERIFIED LOCALLY. EACH AUTHORITATIVE PDF PAGE IS REPRESENTED BY EXACTLY ONE COMPLETE FULL-PAGE PREPARATION REGION AND ONE DETERMINISTIC FULL-RESOLUTION GRAYSCALE PNG TRANSPORT REPRESENTATION IN PDF PAGE ORDER, WITH COMPLETE TRANSFORMATION PROVENANCE, DETERMINISTIC IDENTITIES, FAIL-CLOSED CHROMATIC-RISK HANDLING, EXACT REQUEST-SIZE GATES AND UNCHANGED V8 CAPABILITY IDENTITY AND DIGEST. THE REGISTERED DEED HAS PASSED ONLY THE AUTHORIZED LOCAL PREPARATION AND FIDELITY GATES; ITS HISTORICAL OI11R5F REGION SET REMAINS IMMUTABLE AND UNORDERED. NO PRODUCTION DEPLOYMENT, PROVIDER AUTHORIZATION, PROVIDER CALL, RETRY OR EXTERNAL EGRESS OCCURRED. THE NEXT GOVERNED WORK MUST CONVERGE THIS EXACT IMPLEMENTATION THROUGH ARTIFACT ACCEPTANCE, DEPLOYMENT AND NEW IMPLEMENTATION-BOUND V8 AUTHORITY BEFORE ANY REAL-DOCUMENT EXECUTION CAN BE CONSIDERED.
