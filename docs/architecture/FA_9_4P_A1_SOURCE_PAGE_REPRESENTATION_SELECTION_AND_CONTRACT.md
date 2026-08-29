# FA.9.4P-A1 Minimum Source Visual Representation for Region-Anchored External Transcription

**Unit:** FA.9.4P-A1E-R6.1
**Status:** Implemented and Linux-verified source-page boundary; no deployment, provider call, OCR, segmentation, reading-order inference, provider integration, transcription schema, comparison, UI, acceptance execution, or O.5 execution is authorised.

## 1. Governing purpose

External high-fidelity visual transcription remains Parker's intended primary fidelity mechanism. Parker owns only the minimum deterministic direct-source visual layer needed to give a future accepted external provider an exact page or crop while Parker independently preserves page identity, pixel geometry, crop reproducibility, and source binding.

The bounded chain is:

`authoritative custody bytes -> deterministic visual page -> future source-grounded regions/crops -> future external visual transcription`.

This unit does not create a local document-understanding or OCR subsystem. It performs no native text extraction. PDF text/font drawing operators are consumed only by a visual renderer as part of producing pixels. Neither native extraction nor rendered pixels become authoritative evidence; the immutable custody source remains authoritative.

## 2. Authoritative evaluation environment and witness

Evaluation ran on Ubuntu host `parker`, OpenJDK `17.0.20+8-1-26.04-Ubuntu`, at repository commit `31b5e7b0705375d8b1ed05108b887e573e9b27a8` in isolated clone `/tmp/parker-fa-9.4p-a1e-r6.1-aS6uLz2F`.

The mandatory read-only witness was copied from the production evidence mount without store mutation:

- artifact: `evidence-0275472f-535a-4cf1-b30d-f45ac7684743`;
- SHA-256: `7373ad403b4fae5bf5c777deb8524eaa3ba38594ce9fabfa8fcbce22fbd33182`;
- length: 111,122 bytes;
- media type: `application/pdf`;
- pages: 3.

## 3. Candidate assessment

No global dependency was installed. Only PDFBox was empirically evaluated because it was already resolved in Parker's Gradle dependency graph. Absence alone did not disqualify other candidates; the assessment considered fitness for the minimum external-transcription representation.

| Candidate | Exact status/version | Licence | Integration/deployment | Determinism/fidelity/security assessment | Decision |
|---|---|---|---|---|---|
| Apache PDFBox | 3.0.7, empirically evaluated | Apache License 2.0 | Pure JVM API; already transitively present through Tika, now made explicit; FontBox dependency; no renderer subprocess/native package | Pixel and PNG bytes repeated deterministically in this Linux environment; handles PDF graphics, embedded fonts/images, transparency and rotation; malformed input remains an in-process parser risk and system-font substitution must be governed | selected as thin PDF-to-visual adapter |
| Poppler | Ubuntu candidate `26.01.0-2ubuntu0.1`, not installed/evaluated | GPL family; distribution obligations require legal confirmation | Native utility/library, OS package and subprocess/JNI lifecycle | Mature high-fidelity renderer, but adds native deployment, sandbox/process, version and licensing surface not justified by current evidence | deferred alternative/fallback evaluation only |
| MuPDF | Ubuntu candidate `1.27.0+ds1-3ubuntu2`, not installed/evaluated | AGPL or commercial terms | Native tool/library and separate deployment/security lifecycle | Strong rendering reputation and compact runtime, but licensing and native integration are disproportionate for the minimum layer | rejected for current selection; revisit only with commercial/licensing governance |
| PDFium | no installed or configured Ubuntu package/version; not empirically evaluated | BSD-style upstream, subject to bundled third-party notices | Native build/JNI wrapper, platform artefacts, patch/security maintenance | Strong browser-grade rendering but materially greater build/deployment ownership; no local evidence justifies it | deferred |

PDFBox was not selected merely because present. It was selected because the A1 witness and synthetic fixture produced stable geometry and pixels, the output is directly usable by external vision mechanisms, the licence is compatible, and the implementation adds no native deployment surface.

## 4. DPI and resource evidence

Each A1 page was rendered independently ten times at each DPI. Timings are per page on the Parker Ubuntu host and are descriptive, not a benchmark claim.

| DPI | Dimensions | Encoded size range | Average latency range | Finding |
|---:|---|---:|---:|---|
| 200 | 1655 x 2338 | 164,396–379,905 bytes | 201–259 ms | visually preserves layout; lowest cost, but less crop detail margin |
| 300 | 2482 x 3507 | 279,713–614,207 bytes | 431–462 ms | preserves small type, emphasis, thin rules and exact layout with moderate cost |
| 400 | 3310 x 4677 | 401,967–858,551 bytes | 721–835 ms | deterministic and sharper, but about 1.78x the 300-DPI pixels and materially higher CPU/memory without a demonstrated current need |

### Selection

The v1 PDF profile uses **300 DPI**. This is a bounded external-vision source quality choice, not a general OCR optimization claim. It may change only through a new render-profile identity and evidence. Future controlled provider acceptance, not R6.1, must establish provider transcription fidelity.

At 300 DPI an A1 page has 8,704,374 pixels and 26,113,122 canonical RGB bytes. At 400 DPI it has 15,480,870 pixels and 46,442,610 canonical RGB bytes. The test worker completed within a 512 MiB heap after the harness stopped retaining multiple full page buffers concurrently.

## 5. Raster, colour, transparency and orientation

### Canonical encoded representation

Lossless non-interlaced PNG is selected. The Java ImageIO PNG encoder produced identical encoded SHA-256 values over ten repetitions in the evaluated build. JPEG is prohibited as canonical representation. TIFF adds no current provider/interoperability advantage and multi-page TIFF is deferred.

The canonical source-independent verification anchor is the pixel digest, not PNG container bytes. Encoded PNG SHA-256 is also retained whenever bytes are materialized or persisted.

### Colour and alpha

The v1 page pixels are opaque 8-bit sRGB RGB. Colour is preserved because it may carry evidence; grayscale is not the canonical v1 policy. Transparency is deterministically composited in sRGB onto solid white using the governed profile. There is no platform-dependent implicit background. Both policies are persisted in provenance.

### Orientation

PDF page rotation metadata is applied into the visually presented orientation. The original normalized rotation value (0/90/180/270) and source CropBox dimensions are retained, while rendered pixel dimensions describe the presented page. This is the coordinate frame a future external vision provider sees and R6.2 regions use.

For PNG evidence, custody pixels are decoded directly, dimensions remain source-pixel dimensions, no DPI resampling occurs, and alpha/colour are normalized only according to the profile. JPEG/EXIF orientation is not silently guessed and remains unsupported until separately governed. Multi-page TIFF and similar scan containers are deferred.

## 6. Determinism results

For PDFBox 3.0.7 plus Java ImageIO on the evaluated Ubuntu/JDK build:

- repetition count: 10 per A1 page per DPI, plus 10 per evaluated synthetic DPI;
- pixel determinism: passed; one canonical pixel digest per page/profile;
- byte determinism: passed; one encoded PNG SHA-256 per page/profile;
- dimension determinism: passed;
- representation identity determinism: passed;
- crop determinism: passed for identical representation ID and integer half-open pixel bounds.

Determinism is explicitly build/profile-bound. Cross-JDK, cross-PDFBox, cross-font-environment, and cross-architecture equality is not asserted without separate evidence. A different build yields a different governed renderer/build identity even when pixels happen to match.

## 7. A1 canonical results at selected 300 DPI

| Page | Pixel dimensions | Canonical pixel SHA-256 | Encoded PNG SHA-256 | Bytes |
|---:|---|---|---|---:|
| 1 | 2482 x 3507 | `e0bafdc978ff9e6ca7b6f312c34abebd02d6e408ce6c5a198f2b89e84a460683` | `45978b3bf9ea03b3c565ce54ab8f40e2ed072a836aa09bc6d504d172383a44e5` | 397,998 |
| 2 | 2482 x 3507 | `9c854c27deae0978f58086a77c0f29cfd1c202b719e3d843c58370a4d41dcfe2` | `f4a7c2093d71e1cdd7da97f108ae7c79c017eeb2db390ef6c9dc45b34fd4da90` | 614,207 |
| 3 | 2482 x 3507 | `39d1af8e8856a76f34d9401846be6f5af5aa363ab3d7256c64b1094a846a5e66` | `d8c3c0bd0f273a60fd581bc920360ec54ce99f5659ff6459c30f0b78c77579bc` | 279,713 |

The selected rendering preserves page 2's emphasized initial proposition in its visible location between its neighbouring prose. Page 3 preserves the authorization block before closing prose. This is a visual-layout witness only; no OCR or transcription was performed and no extracted text served as an oracle.

## 8. Synthetic evidence

The local synthetic PDF fixture covers single-column prose, small type, bold proposition between paragraphs, two columns, a table grid, header/footer, authorization/signature block, thin rule, embedded raster, transparency, mixed text/image and a rotated page. A synthetic transparent PNG covers direct image decode and white alpha compositing. The focused suite verifies dimensions, visually presented rotation, colour/alpha policy, source binding, corrupt bytes, wrong digest, invalid page, unsupported media, pixel limits, defensive copies and deterministic crops.

Unusual-glyph fidelity remains renderer/font dependent. Standard embedded and A1 fonts rendered visibly; a dedicated embedded-font Unicode fixture is an exact unresolved test addition before broad source-class acceptance. This does not block the A1 and minimum visual-boundary selection.

## 9. Contract

`SourcePageRenderer` accepts a `SourcePageRenderRequest` containing:

- `EvidenceArtifactId` and expected custody SHA-256;
- immutable defensive copy of custody bytes;
- media type and one-based page number;
- versioned `PageRenderProfile`.

`AuthoritativePageRepresentation` contains immutable defensive copies of encoded PNG and canonical RGB pixels, plus:

- domain-separated `PageRepresentationId`;
- artifact/source digest, source byte length and media type;
- page number and declared page count;
- renderer identity/version/build;
- render-profile identity/version and PDF DPI where applicable;
- encoded format, pixel format, orientation and transparency policies;
- exact source dimensions and units;
- normalized source rotation;
- exact rendered width/height;
- canonical pixel digest and encoded representation digest.

PDF uses CropBox dimensions in decimal PDF points. PNG uses exact source-pixel dimensions. Both expose exact positive rendered pixel dimensions for R6.2. The page-side coordinate bounds are integer half-open `[0,width) x [0,height)`; normalized region conversion and rounding remain R6.2 work.

### Pixel digest

`CanonicalPagePixelDigests` hashes:

1. UTF-8 domain `parker.source-page.canonical-pixels.v1` plus NUL;
2. big-endian width and height;
3. pixel-format identity plus delimiter;
4. row-major RGB bytes, three bytes per pixel.

### Representation identity

The representation ID is a versioned, domain-separated SHA-256 over length-prefixed canonical fields: domain, artifact ID, source digest, page, renderer/version/build, profile/version/DPI, pixel/orientation/transparency policies, rendered dimensions and canonical pixel digest. Timestamps, paths, randomness and container IDs are excluded.

### Crop primitive

`crop` accepts an existing representation and positive integer half-open pixel bounds. It rejects out-of-page bounds, copies row-major pixels exactly, records the parent representation ID and bounds, and computes the same domain-separated canonical pixel digest over the crop. It has no region identity, segmentation, semantic class or reading-order behavior.

## 10. Failure and security boundary

Fail-closed outcomes cover unsupported media, corrupt source, source digest mismatch, invalid page, extreme dimensions, resource limits, provenance mismatch, digest mismatch, nondeterministic pixels and renderer failure. Persistence failures belong to a future store because R6.1 does not persist production page representations.

Initial limits are:

- 64 MiB source bytes;
- 200 PDF pages;
- 20,000 pixels per rendered dimension;
- 50,000,000 decoded pixels per page;
- PDF profile DPI constrained to 72–600.

Preflight computes PDF dimensions before rendering to prevent ordinary extreme allocations; actual dimensions are checked again. The parser still processes untrusted structured bytes in-process. A hard render timeout and crash isolation cannot be made reliable by interrupting an in-process Java renderer and remain a deployment-security design issue. Production composition should use bounded concurrency and may require a worker process/container before broad hostile-input exposure. Embedded fonts/images, decompression bombs, malformed objects and parser CVEs require dependency monitoring and source-class acceptance tests.

Font substitution can change pixels across hosts. The renderer/build profile must bind the font environment or fail when required fonts are unavailable; broad font-environment capture is unresolved. This is why no cross-host pixel-determinism claim is made.

## 11. Storage recommendation

Use **persist with regeneration capability** for any page representation actually sent externally or used in a governed review. Persisted encoded PNG plus provenance and both digests provides exact forensic replay; deterministic regeneration verifies reproducibility. R6.1 implements the in-memory value and renderer only, not a production store. Storage location, atomic publication, encryption/access class and retention policy are later governed work.

## 12. Repository surface

Reused:

- `EvidenceArtifactId` and authoritative source-resolution/custody concepts;
- existing SHA-256 conventions and Gradle/JVM environment;
- PDFBox already present through the scoped Tika PDF dependency graph.

Modified:

- `build.gradle.kts`: makes PDFBox 3.0.7 an explicit implementation dependency rather than relying on transitivity.

Added:

- `src/interfaces/SourcePageRepresentation.kt`;
- `src/runtime/DeterministicSourcePageRenderer.kt`;
- `tests/runtime/DeterministicSourcePageRendererTest.kt`.

Unchanged:

- OCR/transcription contracts and adapters;
- native PDF extraction;
- acquisition routing, admission, derivatives and codecs;
- authority/attempt/generation/review stores;
- UI, Memory, Knowledge and production composition.

Deferred:

- region segmentation, identity and normalized coordinates;
- provider request/schema integration;
- independent comparison and human review;
- persistence implementation;
- JPEG/EXIF, TIFF and broad image-format support;
- worker isolation/timeouts and font-environment binding.

## 13. Implementation decision and correction compliance

Implementation proceeded because selected PDF rendering was pixel deterministic, directly preserved the A1 visual layout, had acceptable licensing/deployment implications, and could be kept to the minimum source-visual boundary.

The implementation exists solely so a future accepted external vision/OCR provider can receive an exact custody-derived page or crop and Parker can prove where it came from. It contains no OCR, text extraction, semantic layout classification, reading-order inference, provider adapter, transcription schema, comparison, human correction, UI or acceptance execution. External transcription remains primary; local OCR remains only a possible future independent mechanism under separate governance.

## 14. R6.2 prerequisites and unresolved issues

R6.2 may consume exact page dimensions, canonical pixels, representation ID/digest and deterministic crop bounds. It must govern region identity, normalized-coordinate conversion/rounding and source-geometry order without expanding the renderer.

Exact unresolved issues are:

1. cross-host/JDK/architecture pixel reproducibility and font-environment binding;
2. dedicated embedded-font unusual-Unicode fixture;
3. JPEG EXIF orientation and additional raster formats;
4. hard timeout/crash isolation and bounded concurrency;
5. durable representation storage/security/retention;
6. whether future provider acceptance demonstrates a reason for a different DPI/profile;
7. final normalized coordinate conversion, rounding and region derivation, owned by R6.2.

Recommended next unit: `FA.9.4P-A1E-R6.2 — DETERMINISTIC SOURCE-REGION GEOMETRY AND ORDER GRAPH`.
