# OI11R6Q — Exact Accepted Readback-Compatible Artifact Deployment and Production Canonical Readback Verification

## 1. Starting repository state

Host `parker`, branch `main`, HEAD and upstream were exactly `6633ee315c4d3c09d3f86ed593813ba73fc79cec`; the worktree was clean.

## 2. Owner acceptance verification

The deployment-only acceptance record `/mnt/parker-data/parker/replacement-candidates/oi11r6p-artifact-acceptance-55b4f29b-v1.json` was verified directly: SHA-256 `720f57f465c8d386c03b9dc7fa4cde683bbf3e77d6e5e3308969cc03cf97f00c`, size 3,679 bytes, mode `0600`, owner/group `steve:steve`. Event `oi11r6p-owner-artifact-acceptance-55b4f29b-v1`, type `exact-production-artifact-deployment-only`, binds the exact artifact and R6N derivative context. Its exclusions prohibit continuation, derivative admission/regeneration/replacement/mutation, duplicate derivative, new authorization/execution/attempt, capability acceptance mutation, provider activity, retry, egress, external reasoning, and historical-state mutation.

## 3. Exact artifact identity

The preserved archive `/mnt/parker-data/parker/replacement-candidates/oi11r6p-readback-compatible-d45efbee-20260903.tar` matched SHA-256 `ca01eec07e6d6b4774ee5e0efb1a18545818248721fa33ce13b110e0faf96b17`, size 1,031,923,200 bytes, mode `0600`, owner/group `steve:steve`. Its recoverability was already PASS and its loaded identities were re-used without rebuild or pull:

* source `d45efbee348b842340616a6a73831ef130086d90`;
* runtime JAR `71f154b230a5ce318915f7fdc66b24ad11393c0112e5f76a1a9c289255c3815a`;
* OCI image/index `sha256:55b4f29b4e8f30b80528fda075c7936a968ae98a0b6b54e55b536e9fb9d9ac9c`;
* platform manifest `sha256:af821d46aabaa2a7baf68f597c10ac7a32bc2f2f46ed0c360eb6510ab6a2d03d`;
* OCI config `sha256:54545105d97b6f15483ea1df8b01f2d7228354a5ca5371bef093c41edb546548`;
* platform `linux/amd64`.

## 4. Pre-deployment production identity

Production exactly matched the required baseline: container `aae0bf09790510cdb6d2e47a7dfeb25e79bc7f4b86236e7e3121fb5fa66f3149`, image/index `sha256:adbb96afdb732a4549661fef08773d1b70a471e5311c804392f5fba26ce1ea4e`, source `ca15222c9f5edea28e68bbb0099734578fc30c4a`, runtime JAR `ce5191a5a04de91c9697acb38043cba6ddfa0c11bfb20f16babeb216804d7137`, restart count zero, readiness PASS.

## 5. Pre-store snapshot

| Store | Count | Aggregate SHA-256 |
|---|---:|---|
| evidence | 29 | `e5d29f86bc047774082d0beb70f62b81d2b344b8666edfeb1b8d481f4fe27d85` |
| manifests | 29 | `ec2a7dc1aad1efc9bac3763930564da11a0d5674140378e77d4f108228d76559` |
| corrected-preparation files | 6 | `0701859977ca979f1dfc64f605e550ee1e963104e445d17c0e361fb5b06b5b3d` |
| capability acceptances | 14 | `14eeeb9ceac5361c394c58aeae66ee9b3d6b908717ce06e416c113e0aa97b950` |
| authorizations | 14 | `b9b30ccee935874b8c1ccf397b88dd4b166e87d350a5b1cf42301af43f42c6f6` |
| attempts/executions | 10 | `798ed0fd9e4eff2b19085c33f94cb5a495fae9cb4c849659e35b7f1915c8e12f` |
| provider state | 8 | `be1b33109ed42420d260e94c884af0c23a557bda201285feba7c06edc75845d6` |
| derivative generations | 23 | `801bdfd3c4b4801ac1981cb48d90ccd1721fdd782d0d54326253de62ecb9b19f` |
| derivative content | 21 | `80eee2bb97f2532b0a1b22e7fbfc6e59c8b8a3d15e98ad24b7969917e5f27435` |
| evidence audit | 1 | `1d8b0f78663772b52b5490f6051a92ad453e1ca49365bc5eff8b6c301ae98e59` |
| document-ingestion audit | 1 | `3ff333aa80da829ad3454c1d35136a2f24e0997391c1da4a5ef9cc9604429359` |

## 6. Deployment result

Only the established immutable image/source/production-commit fields in `/home/steve/.config/parker/docker-compose.fa-a1r.yml` were advanced. The override SHA-256 is `3c8288aacfafa57282b3355b58b7bd288e2b147d5280b769e554722bb3cc9e87`; the non-interpolated rendered three-file Compose configuration SHA-256 is `60ae94249d4dbbb920f6a0b53e52632083fcecbb34554f9192e5bbaebcc8b3dd`.

The first `docker compose` invocation stopped before container mutation because the base file requires `PARKER_BUILD_COMMIT` interpolation even with `--no-build`. It created no container or governed state. The exact accepted commit was then supplied as the interpolation value. The successful deployment used the complete established three-file stack with `--no-build --pull never --no-deps --force-recreate`; no artifact was rebuilt or pulled.

## 7. Running production identity

The new container is `7d51a0c2b3c499cee97818c04c8599351cc03c6b515e5ff0358eaa95dfef62fc`. It runs exact image/index `sha256:55b4f29b4e8f30b80528fda075c7936a968ae98a0b6b54e55b536e9fb9d9ac9c`, source/production commit `d45efbee348b842340616a6a73831ef130086d90`, and runtime JAR `71f154b230a5ce318915f7fdc66b24ad11393c0112e5f76a1a9c289255c3815a`. These bind the accepted platform manifest and config in Section 3.

## 8. Readiness/restart state

Startup logged one normal `Runtime starting`, `Runtime started`, and owner HTTP listener start. The authenticated localhost-only readiness endpoint returned `READY`. Restart count is zero and state is running: readiness **PASS**.

## 9. Existing derivative identity

Before and after deployment/readback, the sole R6N generation remained `region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6`; generation SHA-256 remained `9fb18b02db5ac55e5d446cd48ebc619de929c4596f94d2a11fba1a07da71af14`; content SHA-256 remained `18a6ed08a4729350027d3140dc0f07dd49d32c04aa45f9e3e9558df5d007c4eb`. Counts remained 23 generations and 21 content entries: no duplicate was created.

## 10. Canonical production readback result

The exact authenticated localhost-only canonical GET was performed for the evidence/generation pair. It returned HTTP success with status `RETRIEVED` and kind `REGION_TRANSCRIPTION`. The R6N HTTP 500 did not recur. A local bounded projection emitted identities/counts only; transcription text was not copied into this report.

The current V8 evaluator was queried read-only for transparency and returned `CAPABILITY_NOT_ACCEPTED` for implementation `d45efbee...`. Readback was not blocked, because presenting an already-admitted canonical derivative is correctly independent of new execution authority. No acceptance record was created or changed.

## 11. Five-region/order verification

Production readback returned five page bindings, five region bindings and five transcription blocks. Each Parker source-order identity matched the ordered source identity in its corresponding canonical region binding, preserving page order `[1,2,3,4,5]`. Provider-returned order remained distinct and forensic-only.

## 12. Content/provenance integrity

Readback retained exact evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`, execution `ordinary-exec-3c2bf685-d6c2-44e0-acf8-0224d92fd976`, authorization `ff286fdcc38a35aefed16201724c00d8a9930e2f73c08206571295a664127f97`, provider state `2b1fbe06ebee0b7a3fdb618159c6987fa713976d7bfd2732b9048b50f11df3a7`, raw-response identity `4706c24b8b0b83675a8ded1165f316229fa61a92bff4d8fe0a16c1d7d50cfb4a`, preparation `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f`, provider `OpenAI`, provider profile `openai-fidelity-first-transcription-v1`, model `gpt-5.6-sol`, capability/digest, Authorization Purpose, representation version 3, completeness `ACCOUNTED_FOR`, and canonical content-identity digest. The unchanged stored record hashes prove content and provenance were not rewritten.

## 13. Historical Tier A sanity check

A bounded read-only canonical GET for known historical searchable-PDF generation `090ce75f-22ab-4225-b731-c6367ebea5c6` returned `RETRIEVED`, kind `PDF`, completeness `ACCOUNTED_FOR_WITH_QUALIFICATIONS`. Historical supported presentation: **PASS**. Ordinary-region presentation: **PASS**.

## 14. Fail-closed sanity check

A safe non-mutating GET for `oi11r6q-unknown-generation` returned `UNKNOWN_GENERATION`, never a successful presentation. No malformed record was created and no deliberate internal error was induced. Fail-closed behavior: **PASS**.

## 15. Readback purity

The canonical GET path performed derivative generation/content retrieval and deterministic presentation only. It did not invoke continuation, execution, admission, authorization, attempt, provider-state creation, evidence/preparation mutation, provider logic, or external reasoning. Two identical exact-derivative GETs occurred solely because the first succeeded but its local bounded `jq` order-summary expression was invalid; the corrected projection then recorded the result. Both were pure reads.

## 16. Historical immutability

Post-readback hashes remained: authorization `48f4e5405fd298df9c492cd2cab95f65ea9abdfe2546cb951de5f0c9f0cd5544`, authorization events `64070a0d042373164902615edd03a19a4bdf7f602e911d7eecbcec8c31bcb675`, attempt ledger `bff24b6c97ecd5382e514a0dc57f1f28c40509c0d483ba553d2db4d03b5d7591`, provider state `c3f1ea29c9f1b4e76b886a33fdfd7a84400a4b96250d9899905493c844c9620c`, V8 assessment `bace05830d9a2872dbcbb78d3bc73b192dcfb179925def756fc6684604b87d01`, generation `9fb18b02...`, and content `18a6ed08...`. Historical R6-R1 state and the R6N derivative are immutable.

## 17. Provider/egress accounting

OI11R6Q activity: OpenAI calls 0; Claude calls 0; other provider calls 0; retries 0; external evidence egress 0. Continuation invocations 0; new authorizations 0; new executions 0; new attempts 0; new provider-state records 0; new derivatives 0. The historical budget remains one authorized, one consumed, zero retries.

## 18. Post-store accounting

Every post-readback count and aggregate digest exactly matched Section 5. Evidence, manifests, corrected preparations, capability acceptances, authorizations, executions/attempts, provider state, derivative generations/content, and audit stores each had delta zero. No runtime read-access audit mutation occurred.

## 19. Production stability

Final production remains container `7d51a0c2b3c499cee97818c04c8599351cc03c6b515e5ff0358eaa95dfef62fc`, image/index `sha256:55b4f29b4e8f30b80528fda075c7936a968ae98a0b6b54e55b536e9fb9d9ac9c`, source `d45efbee348b842340616a6a73831ef130086d90`, runtime JAR `71f154b230a5ce318915f7fdc66b24ad11393c0112e5f76a1a9c289255c3815a`, restart count zero, readiness PASS.

## 20. Verdict

**A — EXACT READBACK-COMPATIBLE ARTIFACT DEPLOYED AND EXISTING REAL-DOCUMENT DERIVATIVE READ BACK SUCCESSFULLY IN PRODUCTION**
