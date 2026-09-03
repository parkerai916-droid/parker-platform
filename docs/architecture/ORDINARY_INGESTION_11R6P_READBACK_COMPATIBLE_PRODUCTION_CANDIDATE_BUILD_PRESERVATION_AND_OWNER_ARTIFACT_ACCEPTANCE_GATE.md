# OI11R6P — Readback-Compatible Production Candidate Build, Preservation and Owner Artifact-Acceptance Gate

## 1. Starting repository state

Host `parker`, branch `main`, HEAD and upstream were exactly `d45efbee348b842340616a6a73831ef130086d90`; the worktree was clean. The authoritative R6O report existed with SHA-256 `2f6ba23f186277c65455226a5be95fbd9160de55ff203a5e1d4b01a428ad6b64`.

## 2. R6O source identity

The executable candidate was built from exactly `d45efbee348b842340616a6a73831ef130086d90`. This is the R6O implementation that adds an explicit, truthful ordinary-region transcription branch to the legacy Tier A owner-presentation adapter. No later application source entered the candidate. The V8 capability remains `ordinary-external-request-region-transcription-v8`, digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`; R6O did not change its semantics.

## 3. Current production baseline

Read-only inspection found container `aae0bf09790510cdb6d2e47a7dfeb25e79bc7f4b86236e7e3121fb5fa66f3149`, image/index `sha256:adbb96afdb732a4549661fef08773d1b70a471e5311c804392f5fba26ce1ea4e`, embedded source `ca15222c9f5edea28e68bbb0099734578fc30c4a`, and runtime JAR SHA-256 `ce5191a5a04de91c9697acb38043cba6ddfa0c11bfb20f16babeb216804d7137`. The runtime was running, restart count was zero, and readiness was PASS.

## 4. Existing derivative identity

Exactly one admitted R6N derivative remains present:

* generation `region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6`;
* generation-record SHA-256 `9fb18b02db5ac55e5d446cd48ebc619de929c4596f94d2a11fba1a07da71af14`;
* content-record SHA-256 `18a6ed08a4729350027d3140dc0f07dd49d32c04aa45f9e3e9558df5d007c4eb`.

The record was read only. It was not mutated, deleted, recreated, replaced, or duplicated.

## 5. Pre-store snapshot

| Store | Count | Aggregate SHA-256 |
|---|---:|---|
| evidence | 29 | `e5d29f86bc047774082d0beb70f62b81d2b344b8666edfeb1b8d481f4fe27d85` |
| evidence-source manifests | 29 | `ec2a7dc1aad1efc9bac3763930564da11a0d5674140378e77d4f108228d76559` |
| corrected-preparation files | 6 | `0701859977ca979f1dfc64f605e550ee1e963104e445d17c0e361fb5b06b5b3d` |
| capability acceptances | 14 | `14eeeb9ceac5361c394c58aeae66ee9b3d6b908717ce06e416c113e0aa97b950` |
| execution authorizations | 14 | `b9b30ccee935874b8c1ccf397b88dd4b166e87d350a5b1cf42301af43f42c6f6` |
| attempts/executions | 10 | `798ed0fd9e4eff2b19085c33f94cb5a495fae9cb4c849659e35b7f1915c8e12f` |
| provider state/assessments | 8 | `be1b33109ed42420d260e94c884af0c23a557bda201285feba7c06edc75845d6` |
| derivative generations | 23 | `801bdfd3c4b4801ac1981cb48d90ccd1721fdd782d0d54326253de62ecb9b19f` |
| derivative content | 21 | `80eee2bb97f2532b0a1b22e7fbfc6e59c8b8a3d15e98ad24b7969917e5f27435` |
| evidence audit | 1 | `1d8b0f78663772b52b5490f6051a92ad453e1ca49365bc5eff8b6c301ae98e59` |
| document-ingestion audit | 1 | `3ff333aa80da829ad3454c1d35136a2f24e0997391c1da4a5ef9cc9604429359` |

## 6. JAR build identity

The established bounded, offline production build was:

`PARKER_BUILD_COMMIT=d45efbee348b842340616a6a73831ef130086d90 ./gradlew clean installDist --offline --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g`

It completed successfully. The exact JAR incorporated into the candidate was:

* path `build/libs/parker-platform-0.8.0-runtime-complete.jar`;
* size `4,661,491 bytes`;
* SHA-256 `71f154b230a5ce318915f7fdc66b24ad11393c0112e5f76a1a9c289255c3815a`;
* manifest `Parker-Source-Commit: d45efbee348b842340616a6a73831ef130086d90`.

The installed-distribution copy was byte-identical. Extraction from the recovered candidate reproduced the same size and hash.

## 7. OCI candidate identity

The established isolated BuildKit procedure copied the exact installed distribution and tools over the already-local Parker runtime base `sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb`. It used `--network=none`, `--pull=false`, platform `linux/amd64`, OCI output, and provenance attestation.

* OCI image/index: `sha256:55b4f29b4e8f30b80528fda075c7936a968ae98a0b6b54e55b536e9fb9d9ac9c`
* platform manifest: `sha256:af821d46aabaa2a7baf68f597c10ac7a32bc2f2f46ed0c360eb6510ab6a2d03d`
* OCI config: `sha256:54545105d97b6f15483ea1df8b01f2d7228354a5ca5371bef093c41edb546548`
* attestation manifest: `sha256:02bb7987c2c2804d33dbd7b3f74b37f22a4d229e2831ef9eb1b47a2390cb51a2`
* platform: `linux/amd64`
* runtime user/entry point: `parker` / `/opt/parker/bin/parker`

## 8. Artifact-local source/JAR verification

OCI revision inspection returned exactly `d45efbee348b842340616a6a73831ef130086d90`. A never-started temporary container yielded the runtime JAR with SHA-256 `71f154b230a5ce318915f7fdc66b24ad11393c0112e5f76a1a9c289255c3815a` and the same embedded source. The temporary container was removed without ever running. Candidate bytecode contains `OwnerTierAContent.RegionTranscription` and the record-aware `OwnerUiEvidenceRuntimeAdapter.toOwnerContent` mapping.

## 9. Exact R6N derivative readback result

Read-only copies of the exact production generation/content files reproduced their authoritative SHA-256 values. Those copies traversed the real filesystem generation and content codecs, `TierAContentRetrievalCoordinator`, the R6O owner adapter, and the canonical HTTP presentation tests. Result: **PASS**.

The candidate returned the ordinary-region transcription under its truthful type, retained the canonical content unchanged, and exposed its governed evidence, execution, provider, preparation, capability, authorization, transformation and content-identity fields. No production endpoint or mutating path was invoked.

## 10. Five-region/order verification

The exact copied R6N derivative returned five page bindings, five region bindings and five transcription blocks. Its persisted Parker source order matched the ordered source identities of those five region bindings, preserving pages `[1,2,3,4,5]`. Provider-returned order remains separately represented and does not redefine Parker order.

## 11. Historical Tier A regression

The bounded candidate-local suite covered `OwnerUiEvidenceRuntimeAdapterTest`, `OwnerEvidenceHttpServerTest`, `TierAContentRetrievalCoordinatorTest`, and `FileSystemDerivativeContentStorageTest`: 4 suites, 139 tests, zero skipped, zero failures and zero errors. Existing PDF, CSV, email and DOCX owner presentation remains unchanged; ordinary-region presentation succeeds through both adapter and HTTP layers.

## 12. Fail-closed unsupported-type behavior

The same suite verifies unknown generation, source mismatch, missing/corrupt content, unknown or unsupported representation version, and unsupported presentation payloads remain governed failures. OCR remains outside this Tier A branch. No permissive default or accept-all conversion was added.

## 13. Provider-prohibition verification

Static composition and candidate bytecode show owner readback depends only on canonical derivative generation/content retrieval and deterministic presentation mapping. It has no provider adapter, continuation, execution, admission, OpenAI, Claude, or other external-provider dependency. Readback cannot regenerate or edit transcription, create provenance or derivative state, or perform external reasoning. Provider invocation capability from readback: **NONE**.

## 14. Archive preservation

The exact candidate is preserved at:

`/mnt/parker-data/parker/replacement-candidates/oi11r6p-readback-compatible-d45efbee-20260903.tar`

* archive SHA-256: `ca01eec07e6d6b4774ee5e0efb1a18545818248721fa33ce13b110e0faf96b17`;
* archive size: `1,031,923,200 bytes`;
* permissions: `0600`;
* owner/group: `steve:steve`.

No existing archive was overwritten.

## 15. Recoverability

The preserved archive is byte-identical to the verified OCI export. `docker load` of the preserved archive succeeded and recovered image/index `sha256:55b4f29b4e8f30b80528fda075c7936a968ae98a0b6b54e55b536e9fb9d9ac9c`. Nested OCI inspection reproduced the platform manifest/config and Linux/amd64 identity; never-started extraction reproduced the exact runtime JAR/source. Recoverability: **PASS**.

## 16. Post-build production identity

Production remained container `aae0bf09790510cdb6d2e47a7dfeb25e79bc7f4b86236e7e3121fb5fa66f3149`, image/index `sha256:adbb96afdb732a4549661fef08773d1b70a471e5311c804392f5fba26ce1ea4e`, source `ca15222c9f5edea28e68bbb0099734578fc30c4a`, and runtime JAR `ce5191a5a04de91c9697acb38043cba6ddfa0c11bfb20f16babeb216804d7137`. Restart count remained zero and readiness PASS. No deployment or restart occurred.

## 17. Governed-store accounting

Every post-build count and aggregate hash exactly matched Section 5. The exact R6N generation/content hashes also reproduced. All production governed-store deltas were zero, including evidence, manifests, corrected preparation, capability acceptance, authorization, execution/attempt, provider state, derivative generation/content, and audit stores. Historical R6-R1 state and its exhausted one-call/zero-retry budget remain immutable.

## 18. Provider/egress accounting

OI11R6P activity: OpenAI calls 0; Claude calls 0; other provider calls 0; retries 0; external evidence egress 0. The historical real-document total remains one authorized call, one consumed call, and zero retries.

## 19. Owner-acceptance boundary

**OWNER ARTIFACT ACCEPTANCE REQUIRED.** The exact decision boundary is source `d45efbee348b842340616a6a73831ef130086d90`, JAR `71f154b230a5ce318915f7fdc66b24ad11393c0112e5f76a1a9c289255c3815a`, OCI index `sha256:55b4f29b4e8f30b80528fda075c7936a968ae98a0b6b54e55b536e9fb9d9ac9c`, platform manifest `sha256:af821d46aabaa2a7baf68f597c10ac7a32bc2f2f46ed0c360eb6510ab6a2d03d`, config `sha256:54545105d97b6f15483ea1df8b01f2d7228354a5ca5371bef093c41edb546548`, and the archive identity in Section 14. Artifact-local exact R6N readback and recoverability are PASS.

This unit does not accept, deploy, restart, create V8 authority, invoke continuation, admit or alter a derivative, create execution state, or authorize provider activity.

## 20. Verdict

**A — READBACK-COMPATIBLE PRODUCTION CANDIDATE BUILT AND PRESERVED; OWNER ACCEPTANCE REQUIRED**
