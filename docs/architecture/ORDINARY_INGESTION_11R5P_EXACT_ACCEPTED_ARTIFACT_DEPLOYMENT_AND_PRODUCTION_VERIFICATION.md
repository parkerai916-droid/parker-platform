# OI11R5P — Exact Accepted Artifact Deployment and Production Verification

## Verdict

**A — EXACT ACCEPTED ARTIFACT DEPLOYED AND VERIFIED**

The exact owner-accepted OI11R5O candidate was deployed without rebuild or pull. Running production matches every accepted image/source/JAR identity, readiness passes, the preparation-only operation and corrected-preparation store are composed, historical state remains byte-stable, and the new implementation remains fail-closed for V8 pending separate implementation-bound acceptance.

## Repository starting state

The host was `parker`, repository `/home/steve/parker-platform`, branch `main`. Starting HEAD and upstream were both `4ea521129d63ee23be21b456f9ea5d5436993725`; the worktree was clean and `git diff --check` passed.

## Owner-accepted artifact gate

Steven explicitly accepted this exact artifact for deployment only:

| Identity | Exact value |
|---|---|
| Source | `a031c92549fd7a3b8c92f6917be0e59b61ca5fde` |
| JAR SHA-256 | `b398a59855303d5d47b0c2154d2b6ddb31c8784f97b86b6991f08723b929831f` |
| OCI image/index | `sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb` |
| Linux/amd64 manifest | `sha256:1e7c1b781cc69fb1b42499f31dc05afeaed0975a54a74326b63b7ad9f8a5518f` |
| OCI config | `sha256:157d92a9e1636b4c245de66b3c5f64394dd2db38da1a9e73cc4974aca242e440` |
| Archive | `/mnt/parker-data/parker/replacement-candidates/oi11r5o-governed-preparation-only-a031c925-20260902.tar` |
| Archive SHA-256 | `fdc0ff366b182a2f9ae0fa684c955cf6e2c5264c3cf288831731b85d7e1b6c97` |
| Archive size | 990,716,416 bytes |
| Archive permissions | `0600`, `steve:steve` |

Before production mutation, the archive, local image and internal OCI manifest/config blobs were rehashed. All values matched, the image reported `linux/amd64`, and its OCI revision was exact source `a031c92549fd7a3b8c92f6917be0e59b61ca5fde`. No substitute artifact was built or pulled.

The established deployment-only artifact-acceptance mechanism created `/mnt/parker-data/parker/replacement-candidates/oi11r5p-artifact-acceptance-d33a5a47-v1.json`, SHA-256 `e5a99543a56d553f0c4dfda64edfda0b63c6cbc69fd1b2684a739fee11c4dbf4`, 1,453 bytes, mode `0600`, owner/group `steve:steve`. Event `oi11r5p-owner-artifact-acceptance-d33a5a47-v1`, timestamp `2026-09-02T08:54:23Z`, binds all exact artifact identities and expressly excludes implementation-bound V8 authority, capability promotion, Deed preparation, evidence-specific/provider/execution/egress authority.

## Previous production baseline

Production was exactly the expected earlier R5J/K artifact:

| Field | Exact value |
|---|---|
| Container | `eb5a5bcf74ec26fe09bbb59b9a10a9eb0fd92d02a92cdb5812a18c35c3a4dd0f` |
| Image/index | `sha256:dea81e5d8d2a339cd0da407716ac532ca58320b81f8f932d68775cf8b8d0535f` |
| Embedded source | `fe13047df0dd5f155d6a6921acf7bc85541af26f` |
| Started | `2026-09-01T13:23:54.513307926Z` |
| Restart count | 0 |
| Readiness | PASS |

No unexpected baseline difference existed.

## Pre-deployment stores

| Governed store | Count | Aggregate content digest |
|---|---:|---|
| evidence | 29 | `e5d29f86bc047774082d0beb70f62b81d2b344b8666edfeb1b8d481f4fe27d85` |
| evidence source manifests | 29 | `ec2a7dc1aad1efc9bac3763930564da11a0d5674140378e77d4f108228d76559` |
| capability acceptances | 10 | `3d0ce2eeb4d2642c74dc049e98b2330db84aadce51cfd7ae5b3b17e95a921843` |
| owner authorizations | 11 | `f69eb1cfa4d4ff6438a55e4aa12beeb68a55cd4d5ece17c9fb4b09bb86f24939` |
| attempts | 8 | `842a2457b71df56d8265b1419a37ca2f4e986c483267afee735f0bf344d62e74` |
| provider state | 6 | `980472f44ba44324881755167e7546bfa1d5bb0589db0736c10ac5fcb65cbc5d` |
| derivative generations | 22 | `28626778eec53df921b16063635393cd18c6e390ed0df5c321a30bacdf41f322` |
| derivative content | 20 | `4952e366cae0633922be9b6dcd1204e57d2e3956336c4e5a4d03d3be06ab158e` |
| corrected preparations | 0 / absent root | not applicable |

The aggregate digest is SHA-256 over the sorted per-file SHA-256 inventory for that store.

## Corrected-preparation host infrastructure

Deployment created only the previously absent infrastructure directory `/mnt/parker-data/parker/corrected-preparations`. It has established persistent-store permissions `2775` and numeric owner/group `999:1001` (runtime owner `parker`, host store-writer group). The accepted image was used as a network-disabled root utility solely to apply this ownership after the host filesystem refused a direct `chown`; no record was created.

On startup, the canonical store composed empty `records/` and `transport/` subdirectories. Total corrected-preparation file count remained zero. Runtime UID 999 confirmed the mounted root readable and writable without a sentinel write. This is infrastructure creation, not governed corrected-preparation content.

## Exact deployment configuration and action

The active immutable override preimage is preserved as `/home/steve/.config/parker/docker-compose.fa-a1r.yml.oi11r5p-preimage`, SHA-256 `5257986a23772098b8f3911b42b292d85a37c573385d31233a95d65a6654c6ad`. Only four exact artifact bindings changed:

- service image to `parker-oi11r5o-a031c925@sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb`;
- `PARKER_DEPLOYED_IMMUTABLE_IMAGE_ID` to that digest;
- `PARKER_SOURCE_COMMIT` to `a031c92549fd7a3b8c92f6917be0e59b61ca5fde`;
- `PARKER_PRODUCTION_COMMIT` to the same exact source.

The new override SHA-256 is `8e0756c3d9389995c0d56cdea0cd7ce8198c12fd4dd22beb1fd2d9fb74ffa1a4`. No provider profile, credential, Authorization Purpose, V8 semantics, evidence configuration or unrelated runtime setting changed.

The final rendered Compose configuration SHA-256 is `9df9c452b8d316838f2855a52a6dafe066b7da6780441dd3bf61a60b42739a22`. It resolves the exact image/source values and maps `/mnt/parker-data/parker/corrected-preparations` to `/data/corrected-preparations` read/write.

Deployment recreated only `parker`, using:

```text
PARKER_BUILD_COMMIT=a031c92549fd7a3b8c92f6917be0e59b61ca5fde docker compose -f /home/steve/parker-platform/docker-compose.yml -f /home/steve/.config/parker/docker-compose.openai-enablement.yml -f /home/steve/.config/parker/docker-compose.fa-a1r.yml up -d --no-build --pull never --no-deps --force-recreate parker
```

No image was built or pulled and no unrelated service was restarted.

## Running production identity and readiness

| Field | Exact value |
|---|---|
| Container | `8b7c4b9b9f1b374de278e37d2f01c8401bc8ab809516d21135ddebf1e8065d7c` |
| Image/index | `sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb` |
| Embedded source | `a031c92549fd7a3b8c92f6917be0e59b61ca5fde` |
| Runtime JAR SHA-256 | `b398a59855303d5d47b0c2154d2b6ddb31c8784f97b86b6991f08723b929831f` |
| Started | `2026-09-02T08:57:38.782928781Z` |
| Restart count | 0 |
| Status | running |

Startup logged `Runtime starting`, `Runtime started`, and the authenticated owner HTTP listener. Repeated canonical non-egress diagnostics returned every predicate true, `ordinaryExecutionReady=true`, `overallReady=true`, and empty reasons. Corrected-store permissions, mount and codec initialization caused no startup failure.

## Preparation-only operation and store composition

The deployed artifact contains `GovernedCorrectedPreparationService`, `FileSystemFullPageAchromaticPreparationStore`, `FullPageAchromaticPreparationCodec`, metadata-only result types, and the owner/admin handler. Production exposes `POST /owner/admin/corrected-preparation/{evidenceArtifactId}`. A request without authentication, using non-evidence identifier `not-governed-evidence`, returned HTTP 401; custody lookup and preparation were therefore not entered. The registered Deed route was never invoked.

The store is mounted read/write at the canonical host/container paths. R5N artifact verification and tests establish canonical codec, create-once conflict detection and readback wiring; runtime composition established their configured availability without manufacturing a record or sentinel.

## Historical compatibility and R5F preservation

Startup and readiness composed the existing evidence, manifest, capability, authorization, attempt, provider-state and derivative stores. Their complete sorted content inventories remained byte-identical across deployment, proving no historical record rewrite. Existing legacy and V8 capability histories remain present and readable; the evaluator read the store successfully.

Historical R5F page-1 records remain exact:

| Item | Exact value |
|---|---|
| Representation | `33d341f5f169ea09a6cdeffc50c731a6b9d58e2a646ffb1ac32532bee2afff1e` |
| Geometry SHA-256 | `8c8d9949b7fa9308381c3be3915e8ab5f78c4c0575cf30315481a4296565fcb4` |
| Order SHA-256 | `99b594592e18d812da7750e84873071a6ed51604a4a8a17708bbf3cf3ed70e79` |
| Region-set digest | `4b8571e618e174adc4e8171bdf0fc1ab512e2a4f164abb11925bef93437cc73f` |
| Disposition | `SOURCE_ORDER_REVIEW_REQUIRED` |

No owner resolution, corrected substitution, reinterpretation or rewrite occurred.

## V8 identity and implementation-bound authority

The deployed runtime exposes unchanged capability `ordinary-external-request-region-transcription-v8`, digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`, wire version 8, maximum 32 regions, maximum body 16,777,216 bytes, and no batching.

The authenticated canonical evaluator returned `CAPABILITY_NOT_ACCEPTED`, runtime embedded build `a031c92549fd7a3b8c92f6917be0e59b61ca5fde`, and no accepted promoting build. The capability store contains zero records binding this new implementation. R5P created no implementation-bound acceptance or capability promotion; production correctly remains fail-closed.

## Deed and provider boundary

Registered evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`, source SHA-256 `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`, was not passed to the preparation route, prepared, request-shaped, authorized, attempted, transmitted or transcribed. No corrected-preparation, provider-state or derivative record was created.

OpenAI calls: 0. Claude calls: 0. Other external provider calls: 0. Retries: 0. External provider/evidence egress: 0. Provider-state delta: 0.

## Post-deployment store accounting

| Governed store | Before | After | Delta | Aggregate digest unchanged |
|---|---:|---:|---:|---|
| evidence | 29 | 29 | 0 | yes |
| evidence source manifests | 29 | 29 | 0 | yes |
| capability acceptances | 10 | 10 | 0 | yes |
| owner authorizations | 11 | 11 | 0 | yes |
| attempts | 8 | 8 | 0 | yes |
| provider state | 6 | 6 | 0 | yes |
| derivative generations | 22 | 22 | 0 | yes |
| derivative content | 20 | 20 | 0 | yes |
| corrected preparations | 0 | 0 | 0 | yes |

The acceptance JSON and empty persistent-store directory are authorized deployment infrastructure outside the governed record counts. No governed data mutation occurred.

## Stop state

Production is stable on the exact accepted artifact with restart count zero and readiness PASS. The preparation-only operation and durable empty store are composed. The Deed remains untouched and V8 remains implementation-bound fail-closed. R5P stops before capability rebinding or Deed preparation.

UNIT ORDINARY-INGESTION-11R5P COMPLETE — THE EXACT OWNER-ACCEPTED OI11R5O ARTIFACT HAS BEEN DEPLOYED TO PRODUCTION. THE RUNNING RUNTIME MATCHES OCI INDEX SHA256:D33A5A47F8A540BF11375C2FD373D5BF3257F36DA5F0F4AFB444BBF3CE46F9CB AND EMBEDDED SOURCE A031C92549FD7A3B8C92F6917BE0E59B61CA5FDE. RUNTIME READINESS PASSES, THE GOVERNED PREPARATION-ONLY OPERATION AND DURABLE CORRECTED-PREPARATION STORE ARE COMPOSED, HISTORICAL PRODUCTION STATE REMAINS READABLE AND IMMUTABLE, AND NO GOVERNED DATA MUTATION OCCURRED AS A RESULT OF DEPLOYMENT. THE REGISTERED DEED HAS NOT BEEN PREPARED, AUTHORIZED, TRANSMITTED OR TRANSCRIBED. NO PROVIDER CALL, RETRY OR EXTERNAL EGRESS OCCURRED. NO IMPLEMENTATION-BOUND V8 ACCEPTANCE FOR THE NEW IMPLEMENTATION WAS CREATED. PRODUCTION REMAINS FAIL-CLOSED PENDING A SEPARATE EXPLICIT OWNER DECISION ON V8 IMPLEMENTATION-BOUND AUTHORITY.
