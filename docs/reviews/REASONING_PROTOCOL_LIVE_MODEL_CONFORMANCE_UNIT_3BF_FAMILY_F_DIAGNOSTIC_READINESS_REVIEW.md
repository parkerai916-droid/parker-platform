**Status:** Unit 3-BF Family F Diagnostic Readiness Review — **READINESS=NOT READY.** **Corrected** after an independent reviewer identified that this document's original provider-identity investigation had not attempted an already-available, passive, read-only Docker metadata path (`docker ps`, `docker inspect`, `docker image inspect`, `docker volume inspect` — available through this account's pre-existing `docker` group membership, requiring no `sudo`, no Ollama CLI/API call, no container `exec`/`cp`/`export`/`save`, and no filesystem mutation). This correction runs exactly those four passive query types and nothing else. They establish the production Ollama container's exact identity, exact image ID/digest, exact entrypoint/command, exact port mapping, and the model-data volume's exact host mountpoint — a genuine partial provider-identity anchor — but they do not and structurally cannot supply the Plan-required resolved-executable SHA-256 or the subject/control model artifacts' on-disk digest and size, both of which remain genuinely inaccessible read-only at the now-precisely-identified locations. The prior wording describing provider identity as having no read-only avenue at all, and as "not a workaround-avoidable gap," was overbroad; it is corrected in Section 7 below. `READINESS=NOT READY` is unchanged and follows from the same underlying gate (Items 7-through-12) for corrected reasons. Implementation acceptance is unaffected by this finding. Execution remains unauthorized regardless of this document's verdict, and this document grants none.

# Reasoning Protocol Live-Model Conformance — Unit 3-BF Family F Diagnostic Readiness Review

## 1. Three concepts this review keeps separate

1. **Implementation acceptance** — already established by `73f8bdc` (implementation) and `710400a` (Completion Review + Independent Constitutional Review, both `VERDICT=ACCEPTED`, merged as `1c699f7`, PR #22). This review's findings, whatever they are, do not reopen or diminish that acceptance.
2. **Environmental readiness** — this document's own subject matter: whether the actual host, actual artifacts, actual processes, and actual resources this diagnostic would run against are, right now, in a state consistent with the Plan's Section 24 requirements. The verdict below is `NOT READY`.
3. **Execution authorization** — never granted by a Readiness Review under the Plan's own Section 22 sequence, regardless of the readiness verdict. This document authorizes no model acquisition, no model load, no endpoint contact, no campaign creation, and no Explicit Execution Approval. A future `READY` verdict on a re-run of this review would still not authorize execution; only a separate, later Explicit Execution Approval can do that, after this review and its own Independent Constitutional Review are both accepted.

## 2. Baseline, branch, and governance chain

```text
BASELINE=1c699f7c052971f71166f7fc0226164481dac62e
```

Confirmed fresh, this task: `git fetch origin`, `git checkout main`, `git pull --ff-only origin main`, then `git rev-parse HEAD` and `git rev-parse origin/main` both resolved to `1c699f7c052971f71166f7fc0226164481dac62e`, with a clean worktree. Branch `governance/reasoning-protocol-family-f-diagnostic-readiness-review` was created from that exact commit.

Governance chain, independently confirmed via `git log --oneline` on this exact history (every commit below is reachable from `HEAD`):

```text
509f3f2  docs(governance): scope Family F model diagnostic                       (Scope Lock)
a4ccfe6  docs(review): accept Family F diagnostic scope                          (Scope Lock ICR, VERDICT=ACCEPTED)
0f9eb8b  Merge PR #20 — Scope Lock + ICR merged to main
c102a9e  docs(governance): plan Family F diagnostic execution                    (Implementation/Execution Plan)
2853c49  docs(review): accept Family F diagnostic execution plan                 (Plan ICR, VERDICT=ACCEPTED)
9ce2f4a  Merge PR #21 — Plan + ICR merged to main
73f8bdc  test(reasoning): implement Family F diagnostic harness                  (the merged implementation)
710400a  docs(review): accept Family F diagnostic implementation                 (Completion Review + ICR, both VERDICT=ACCEPTED)
1c699f7  Merge PR #22 — Implementation + reviews merged to main (= BASELINE)
```

All six governing documents were read fresh, in full, in this task: the Scope Lock, its ICR, the Implementation/Execution Plan, its ICR, the Completion Review, and the Implementation Independent Constitutional Review. All are internally consistent with the governance chain above; no citation drift found.

```text
GOVERNANCE_CHAIN=CONFIRMED — 509f3f2, a4ccfe6, 0f9eb8b, c102a9e, 2853c49, 9ce2f4a, 73f8bdc, 710400a, 1c699f7 all present and reachable from HEAD, all reviews VERDICT=ACCEPTED
IMPLEMENTATION_STATUS=ACCEPTED (unaffected by this review's environmental findings)
```

## 3. Item 3 — ordinary Gradle suite

Ran fresh, forced (not cached):

```text
./gradlew test --rerun-tasks -> BUILD SUCCESSFUL in 58s, 8 actionable tasks: 8 executed
```

```text
ORDINARY_GRADLE_TEST=BUILD SUCCESSFUL
```

## 4. Item 4 — focused offline Family F test success

Ran fresh, forced:

```text
./gradlew reasoningProtocolFamilyFDiagnostic --rerun-tasks -> BUILD SUCCESSFUL in 32s, 4 actionable tasks: 4 executed
```

JUnit XML inspected directly:

```text
TEST-...FamilyFDiagnosticOrchestrationTest.xml: tests="91" skipped="0" failures="0" errors="0"
TEST-...FamilyFDiagnosticTest.xml:               tests="21" skipped="1" failures="0" errors="0"
```

The single skipped test is `live Family F campaign is skipped before any configuration is loaded unless the Gradle-set property and the execution-approval environment value are both present()` — confirmed by name, not assumed. The one pre-existing compiler warning (`ReasoningProtocolBaselineCharacterisationTest.kt:679`, a label-shadowing note) is outside the reviewed diff, matching the Completion Review's own prior observation.

```text
FOCUSED_OFFLINE_TESTS=112 total (91 + 21), 111 executed, 1 correctly self-skipped, 0 failures, 0 errors
```

## 5. Item 5 — task detachment and double-gate behavior

Independently confirmed `PARKER_REASONING_FAMILY_F_EXECUTION_APPROVED` is genuinely absent from this shell's environment (`echo "[${PARKER_REASONING_FAMILY_F_EXECUTION_APPROVED:-UNSET}]"` → `[UNSET]`), and that the live trigger test was skipped under exactly that real condition — this is task detachment exercised empirically, not merely read from source.

Ran fresh, against the actual Gradle task-graph resolver:

```text
./gradlew check --dry-run    | grep -i familyf  -> no match
./gradlew build --dry-run    | grep -i familyf  -> no match
./gradlew assemble --dry-run | grep -i familyf  -> no match
./gradlew tasks --group=verification | grep -i familyf -> "reasoningProtocolFamilyFDiagnostic - Runs the explicit opt-in Reasoning Protocol Unit 3-BF Family F alternative-model diagnostic instrument"
```

The task is listed as its own opt-in verification-group entry and is absent from every ordinary lifecycle task's resolved graph.

```text
TASK_DETACHMENT=CONFIRMED by direct Gradle task-graph resolution
DOUBLE_GATE=CONFIRMED — parker.reasoning.familyf.enabled (Gradle-set) and PARKER_REASONING_FAMILY_F_EXECUTION_APPROVED (env, genuinely absent) both required; live trigger test empirically skipped under real conditions
```

## 6. Item 6 — exact 392-call schedule and schedule-hash mechanism

Re-read `FamilyFCampaignDefinition` (`ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:66-121`) directly. The schedule is constructed from four repetitions × AB/BA role order × (3 warm-ups + 46 scored fixture/profile cells per block), and self-enforced in the object's own `init` block:

```text
check(ids.size == ids.distinct().size)                                                  // unique trial IDs
check(allTrials.count { kind == SCORED } == 368)
check(allTrials.count { kind == WARMUP } == 24)
check(allTrials.size == 392)
check(allTrials.count { role == SUBJECT && kind == SCORED } == 184)
check(allTrials.count { role == CONTROL && kind == SCORED } == 184)
check(blocks.size == 8)
check(blocks.all { it.trials.size == 49 })                                              // 3 warm-ups + 46 scored per block
```

The schedule hash is:

```kotlin
val scheduleHash: String = sha256(allTrials.joinToString("\n") { it.id })
```

— a pure function of trial IDs only, structurally incapable of being affected by any timestamp, envelope field, or ledger record, independently confirmed by direct source read (not restated from the Completion Review).

```text
SCHEDULE_AND_HASH=CONFIRMED — 368 scored + 24 warm-up = 392 total, self-enforced by FamilyFCampaignDefinition's own init block; scheduleHash = sha256 over trial IDs only, timestamp-independent
```

## 7. Item 7 — provider executable identity (CORRECTED — partially established via passive Docker metadata; resolved-binary SHA-256 remains BLOCKED)

### 7.1 Original filesystem-only attempt (unchanged; still blocked)

Attempted, filesystem-only, no provider command or API:

```text
$ which ollama                       -> not found on PATH
$ ps aux | grep ollama                -> root  1926  ...  /bin/ollama serve   (resident production daemon)
$ ls -l /bin/ollama                   -> No such file or directory (not present in this account's mount namespace)
$ ls -la /proc/1926/exe               -> Permission denied
$ readlink /proc/1926/exe             -> (empty; permission denied)
$ stat /proc/1926/exe                 -> Permission denied
$ find /usr/local/bin /usr/bin /opt/ollama /snap/bin ~/bin ~/.local/bin -name ollama -> none found
$ sudo -n true                        -> "sudo: interactive authentication is required" (no passwordless sudo)
```

These bare-filesystem checks remain accurate and remain blocked exactly as before: `/proc/1926/exe` is permission-denied to this account, and `/bin/ollama` does not exist in this account's own mount namespace. Standing alone, they do not explain *why* — they do not by themselves reveal that PID 1926 is a containerized process.

### 7.2 Correction: passive Docker metadata path (independently identified, not originally attempted)

An independent reviewer of this document correctly identified that this account (`steve`) is a member of the `docker` group (`groups` -> `... docker`), and that `docker ps`, `docker inspect`, `docker image inspect`, and `docker volume inspect` are passive, read-only metadata queries against the Docker daemon — not an Ollama CLI/API call, not a container `exec`, `cp`, `export`, or `save`, not a start/stop/restart, not a volume mount, and not `sudo` — that this account can already run today with no additional authority. This correction task performs exactly those four query types, and only those:

```text
$ docker ps --no-trunc
CONTAINER ID: f795e46c5eac20f5fb423014b94d799df9ce2ce11ef1cb58a15b93d5885a258d
IMAGE:        ollama/ollama:latest
COMMAND:      "/bin/ollama serve"
PORTS:        0.0.0.0:11434->11434/tcp, [::]:11434->11434/tcp
NAMES:        ollama
```

```text
$ docker inspect ollama   (selected fields)
Id (full container ID)     = f795e46c5eac20f5fb423014b94d799df9ce2ce11ef1cb58a15b93d5885a258d
Name                        = /ollama
State.Pid                   = 1926   (matches the host-visible PID already identified via ps aux in Section 16)
State.StartedAt              = 2026-08-08T08:40:51.34600392Z (matches the ps-derived start time already recorded in Section 16)
Image (image ID)            = sha256:4dea9fb511947e24a84237bb636b0203abcb2ff0d3fbc7b4ff865deb91362131
Config.Entrypoint            = ["/bin/ollama"]
Config.Cmd                   = ["serve"]
Config.Image                  = ollama/ollama:latest
NetworkSettings.Ports        = "11434/tcp": [{"HostIp":"0.0.0.0","HostPort":"11434"},{"HostIp":"::","HostPort":"11434"}]
HostConfig.Binds             = ["ollama_ollama_data:/root/.ollama:rw"]
Mounts[0]                     = {Type: volume, Name: ollama_ollama_data, Source: /var/lib/docker/volumes/ollama_ollama_data/_data, Destination: /root/.ollama, RW: true}
ImageManifestDescriptor.digest = sha256:98c19ced6600f2924e80b92d701cd867d8f7ef0c4dde516c619484e31e47f103
```

```text
$ docker image inspect ollama/ollama:latest   (selected fields)
Id            = sha256:4dea9fb511947e24a84237bb636b0203abcb2ff0d3fbc7b4ff865deb91362131
RepoTags      = ["ollama/ollama:latest"]
RepoDigests   = ["ollama/ollama@sha256:4dea9fb511947e24a84237bb636b0203abcb2ff0d3fbc7b4ff865deb91362131"]
Created       = 2026-07-27T18:47:31.432040578Z
Size          = 3260568805 bytes
Config.Entrypoint = ["/bin/ollama"]
Config.Cmd        = ["serve"]
```

```text
$ docker volume inspect ollama_ollama_data
Name        = ollama_ollama_data
Driver      = local
Mountpoint  = /var/lib/docker/volumes/ollama_ollama_data/_data
CreatedAt   = 2026-08-02T12:03:27Z
```

```text
$ ls -la /var/lib/docker/volumes/ollama_ollama_data/_data
ls: cannot open file '/var/lib/docker/volumes/ollama_ollama_data/_data': Permission denied
$ stat /var/lib/docker/volumes/ollama_ollama_data/_data
stat: cannot stat '/var/lib/docker/volumes/ollama_ollama_data/_data': Permission denied (os error 13)
```

Corrected findings:

```text
CONTAINER_NAME=/ollama
CONTAINER_ID_FULL=f795e46c5eac20f5fb423014b94d799df9ce2ce11ef1cb58a15b93d5885a258d
IMAGE_REFERENCE=ollama/ollama:latest
IMAGE_ID=sha256:4dea9fb511947e24a84237bb636b0203abcb2ff0d3fbc7b4ff865deb91362131
IMAGE_REPO_DIGEST=ollama/ollama@sha256:4dea9fb511947e24a84237bb636b0203abcb2ff0d3fbc7b4ff865deb91362131
IMAGE_MANIFEST_DESCRIPTOR_DIGEST=sha256:98c19ced6600f2924e80b92d701cd867d8f7ef0c4dde516c619484e31e47f103
ENTRYPOINT_AND_COMMAND=/bin/ollama serve — independently consistent across docker inspect (container Config), docker image inspect (image Config), and the original ps aux capture in Section 16; none contradicts another
PORT_MAPPING=container 11434/tcp -> host 0.0.0.0:11434 and [::]:11434 — matches Section 16's independent ss -tln finding exactly
MODEL_VOLUME_NAME=ollama_ollama_data
MODEL_VOLUME_CONTAINER_PATH=/root/.ollama
MODEL_VOLUME_HOST_MOUNTPOINT=/var/lib/docker/volumes/ollama_ollama_data/_data
MODEL_VOLUME_HOST_PERMISSION_RESULT=Permission denied to this account (ls and stat both fail with EACCES/os error 13) — this is the exact, Docker-confirmed location of the model-data store, not a guess, and it is genuinely inaccessible read-only under this account
```

### 7.3 What this does and does not establish

The container image digest (`sha256:4dea9fb5...`, in both its `Image`/`RepoDigests` form and its `ImageManifestDescriptor.digest` form) is a **genuine, immutable, partial provider-identity anchor**: it identifies exactly which published `ollama/ollama` image this account's Docker daemon has pulled and is currently running as PID 1926, obtained without `sudo`, without any Ollama CLI/API call, and without touching the container's own filesystem.

It is **not equivalent** to the Plan Section 14/19 requirement of "the provider binary's resolved absolute path and SHA-256." An image digest is computed over the image manifest and its layers as a whole, not over the single `/bin/ollama` file inside the running container's own filesystem; the two are different identity artifacts, and one cannot be derived from the other without unpacking the image or reading the live filesystem. Establishing the literal SHA-256 of the actual `/bin/ollama` executable bytes at its resolved path would require reading that file from inside the container's filesystem or its root-owned overlay — via `docker exec`, `docker cp`, `docker export`/`docker save`, or root-level host access to the container's merged layers — every one of which this task was explicitly instructed not to use, and none of which was used. The executable's own bytes and their SHA-256 therefore remain unverified by this review, not because no avenue exists to investigate, but because every remaining avenue is either a prohibited extraction/execution action or requires additional (root) authority this account does not have and this task does not request.

```text
PROVIDER_BINARY_IDENTITY=PARTIALLY ESTABLISHED READ-ONLY VIA PASSIVE DOCKER METADATA — exact container identity, exact image ID/digest, exact entrypoint/command, and exact port mapping are now confirmed without escalation; the Plan-required resolved-executable-path SHA-256 remains genuinely unestablished read-only, since only a prohibited extraction/exec action or additional (root) authority could supply it
```

### 7.4 Correction to prior wording

The version of this document seen by the independent reviewer stated that provider identity "cannot be established read-only" and characterized the gap as "a genuine, unresolved blocker, not a workaround-avoidable gap," which read as an exhaustive search having been performed. That characterization was overbroad: a real, zero-escalation, non-Ollama-command avenue (passive Docker metadata, available through this account's pre-existing `docker` group membership) existed and had not been attempted or disclosed. That avenue is now used and disclosed in Section 7.2 above. The correction narrows, but does not close, Item 7: exact container and image identity are now established; the resolved binary's own SHA-256 is not, and remains a genuine blocker for the reasons given in Section 7.3.

## 8. Item 8 — subject/control artifact identity, digest, and size (BLOCKED)

Attempted, filesystem-only, no `ollama list`/`/api/tags`/`/api/show`/`/api/ps`/any provider command:

```text
$ find / -maxdepth 6 -iname ".ollama" -type d   -> no match reachable by this account
$ ls -la ~/.ollama                               -> No such file or directory
$ ls -la /usr/share/ollama /var/lib/ollama       -> No such file or directory (neither exists)
$ ls -la /root/.ollama                           -> exists, but "cannot open file: Permission denied"
$ find /etc -iname "*ollama*"                    -> no match (no readable service/config file exposing OLLAMA_MODELS)
$ cat /proc/1926/environ                         -> Permission denied
```

No manifest directory (`manifests/...`) or blob store (`blobs/sha256-...`) for either `qwen2.5-coder:7b` or `llama3.2:3b` is reachable by this account on this host. `/root/.ollama` visibly exists on the bare host (its directory entry is listable by name from the parent) but its contents cannot be read without root privilege, which was not invoked. No `OLLAMA_MODELS` override path was discoverable through any readable configuration file. Pre-existing files in this session's scratch history (e.g. `/tmp/unit3c_show_response.json`) are themselves the cached output of a prior `ollama show` **provider command** call from an earlier task — reusing them here would violate this task's explicit "no API, provider command, or model endpoint" instruction and was not done.

### 8.1 Correction: exact volume location confirmed via passive Docker metadata

Section 7.2's passive `docker inspect ollama` query independently confirms where the model data actually lives: the container's `HostConfig.Binds` entry `ollama_ollama_data:/root/.ollama:rw`, and `docker volume inspect ollama_ollama_data`, together show that the model store is a Docker-managed named volume — not the bare host's own `/root/.ollama` directory that the original filesystem sweep above happened to list. The host-side path is:

```text
$ docker volume inspect ollama_ollama_data --format '{{.Mountpoint}}'
/var/lib/docker/volumes/ollama_ollama_data/_data
```

This is the precise, Docker-confirmed model-data location, superseding the original sweep's unverified guesses (`~/.ollama`, `/usr/share/ollama`, `/var/lib/ollama`, the bare host `/root/.ollama`) — none of which was actually known to be correct at the time; they were plausible defaults, not confirmed locations. Checking read access at the now-confirmed location, passively (`ls`/`stat`, no `docker exec`, no `docker cp`):

```text
$ ls -la /var/lib/docker/volumes/ollama_ollama_data/_data
ls: cannot open file '/var/lib/docker/volumes/ollama_ollama_data/_data': Permission denied
$ stat /var/lib/docker/volumes/ollama_ollama_data/_data
stat: cannot stat '/var/lib/docker/volumes/ollama_ollama_data/_data': Permission denied (os error 13)
```

The conclusion is unchanged — the model manifest/blob store remains genuinely inaccessible read-only to this account — but it is now established against the actual, confirmed location rather than an assumed one. `/var/lib/docker` itself is `drwx--x--- root:root` (mode `0710`), so no listing or read is possible below it without root.

```text
SUBJECT_ARTIFACT_IDENTITY_AND_SIZE=CANNOT BE ESTABLISHED READ-ONLY — the exact on-disk manifest/blob store location is now confirmed (`/var/lib/docker/volumes/ollama_ollama_data/_data`, Docker-managed volume backing `/root/.ollama` inside the container) via passive Docker metadata, and remains permission-denied to this account at that exact, confirmed location
CONTROL_ARTIFACT_IDENTITY_AND_SIZE=CANNOT BE ESTABLISHED READ-ONLY — same confirmed location and same permission-denied result; no per-model manifest file was reachable to distinguish qwen2.5-coder:7b from llama3.2:3b within the store even had the parent directory been readable
```

## 9. Item 9 — can required artifact metadata be established without contacting a model?

Partially, and now corrected. Passive Docker metadata (Section 7.2) establishes exact container and image identity — including an immutable image digest — without contacting a model, without elevated privilege, and without any provider command/API call. It does not establish the two values Item 9 actually asks about: the resolved provider binary's own SHA-256, and the subject/control model artifacts' on-disk digest and size (Sections 7.3 and 8.1). For those two specific values, this account's read/exec access is insufficient at every location this task is permitted to check, and no read-only, non-`sudo`, non-Ollama-command, non-extraction path was found to establish them. This remains a hard readiness blocker for those two specific values, not routed around — but it is no longer accurate to say no read-only avenue exists for provider identity generally; Section 7 corrects that.

## 10. Item 10 — current raw `MemAvailable`

```text
$ grep '^MemAvailable:' /proc/meminfo
MemAvailable:    2525956 kB          (read at 2026-08-14T04:39:04+00:00)
```

```text
2525956 kB = 2,586,578,944 bytes ≈ 2.41 GiB
```

This is a point-in-time reading from the same source (`/proc/meminfo`) `FamilyFMemoryGate.defaultReader` uses in the implementation; it will drift with ordinary host activity and must be re-measured, not reused, at actual execution time.

```text
MEMAVAILABLE=2525956 kB (~2.41 GiB), read 2026-08-14T04:39:04Z
```

### 10.1 Follow-up reading (this correction task)

This correction task independently re-read `/proc/meminfo` at the time of this correction, purely to demonstrate drift; it does not replace or supersede the original reading above, which remains the historical evidence for the original review:

```text
$ grep '^MemAvailable:' /proc/meminfo
MemAvailable:    1714828 kB          (read at 2026-08-14T04:59:48+00:00)
```

```text
1714828 kB = 1,755,983,872 bytes ≈ 1.64 GiB
```

```text
MEMAVAILABLE_FOLLOWUP=1714828 kB (~1.64 GiB), read 2026-08-14T04:59:48Z — lower than the original 2026-08-14T04:39:04Z reading, consistent with ordinary host drift; both readings are point-in-time and must be re-measured, not reused, at actual execution time
```

## 11–12. Items 11–12 — required pre-load thresholds and whether this host satisfies them

The implementation's memory gate (`FamilyFMemoryGate`, `ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:~165-230`) requires, before loading a model in a residency block:

```kotlin
FAMILY_F_MINIMUM_FREE_MEMORY_BYTES: Long = 2L * 1024L * 1024L * 1024L   // 2 GiB
threshold = artifactSizeBytes + FAMILY_F_MINIMUM_FREE_MEMORY_BYTES
```

i.e. `MemAvailable >= subject/control artifact size + 2 GiB`.

Because the artifact sizes themselves cannot be established (Item 8, blocked), the exact required thresholds cannot be computed, and therefore whether this host satisfies them cannot be determined exactly. This is recorded as `UNDETERMINABLE`, not assumed passing or failing.

As a directional, non-substitute observation only: current `MemAvailable` (~2.41 GiB at the original reading; ~1.64 GiB at the follow-up reading in Section 10.1) already barely exceeds, or in the follow-up reading falls below, the flat 2 GiB safety-reserve component alone, before any artifact-size addend is applied. Real models of the general class named by the Scope Lock (multi-billion-parameter, `7b`/`3b`-scale) are, as a matter of common knowledge about such artifacts generally, ordinarily several gigabytes on disk — which would push the true required threshold well above either reading. This observation is not a substitute for the required exact, filesystem-verified artifact size — which the Section 7/8 correction confirms is still genuinely inaccessible, not merely unresolved through lack of effort — is not used to declare a numeric PASS/FAIL, and does not change the `UNDETERMINABLE` finding; it is recorded only so a reader is not left with the impression that resolving Item 8 would likely resolve favorably.

```text
SUBJECT_REQUIRED_MEMORY=UNDETERMINABLE (artifact size unknown; formula = size + 2 GiB)
CONTROL_REQUIRED_MEMORY=UNDETERMINABLE (artifact size unknown; formula = size + 2 GiB)
MEMORY_GATE=NOT ESTABLISHED — blocked upstream by Item 8 (unchanged by the Section 7/8 correction, which confirms rather than resolves the artifact-store access gap); current MemAvailable is independently low relative to the 2 GiB flat reserve component alone, before any artifact-size addend (~2.41 GiB original reading, ~1.64 GiB follow-up reading)
```

## 13. Item 13 — free space at proposed evidence and dedicated-runtime parent paths

No prior governance document fixes concrete evidence-root or dedicated-runtime-root paths; Plan Sections 14, 19, and 25 explicitly defer these exact values to the future Explicit Execution Approval. To perform this check, this review proposes (without creating) paths consistent with this repository's own established convention, discovered by read-only inspection: every prior campaign (`qwen25coder7b-baseline-20260809`, `qwen25coder7b-llama32-3b-diagnostic-20260809`, `unit3c-remedy-experiments-20260810[-02/-03]`) is a sibling directory under `/var/lib/parker/reasoning-protocol-live-model/`, owned by `steve:steve`, mode `0700`.

```text
$ df -h /var/lib/parker/reasoning-protocol-live-model
/dev/mapper/ubuntu--vg-ubuntu--lv   30G   25G  3.5G  88%  /
```

No established host convention exists for a separate "dedicated runtime root" (a fresh, non-production Ollama daemon's own writable state, distinct from the evidence root) — Unit 3-C's own tests were grepped for this concept and none exists, confirming Family F introduces this requirement newly. Any proposed dedicated runtime root would necessarily resolve to the same single root filesystem (`/dev/mapper/ubuntu--vg-ubuntu--lv`, 3.5G available) as above, since this host exposes only that one mounted volume under `/var`, `/home`, and `/`.

```text
DISK_GATE=3.5 GiB usable at the proposed evidence-root and dedicated-runtime-root parent paths, both >= the flat 2 GiB minimum, but on a single shared, 88%-full root filesystem with no dedicated volume — thin margin, not a dedicated-capacity guarantee
```

## 14. Item 14 — proposed dedicated endpoint, launch procedure, roots, and campaign ID (proposed only, nothing started or created)

```text
PROPOSED_DEDICATED_ENDPOINT=http://127.0.0.1:21434   (loopback; production Ollama is confirmed bound to 0.0.0.0:11434 and [::]:11434 via read-only `ss -tln` inspection — port 21434 is currently unused and clearly distinct)
PROPOSED_LAUNCH_PROCEDURE=not fixed by any accepted governance document; Plan Section 14 requires the Explicit Execution Approval to fix "the dedicated daemon launch procedure and executable identity" — this remains genuinely undetermined pending that approval, and pending resolution of Item 7's blocker (the provider binary identity that any launch procedure would need to reference)
PROPOSED_RUNTIME_ROOT=/var/lib/parker/reasoning-protocol-live-model-runtime/familyf-diagnostic-2026-08-14  (proposed; sibling to, not nested within, the evidence parent; does not exist)
PROPOSED_EVIDENCE_ROOT=/var/lib/parker/reasoning-protocol-live-model/familyf-diagnostic-2026-08-14  (proposed; follows the confirmed existing sibling-campaign convention; does not exist)
PROPOSED_CAMPAIGN_ID=familyf-diagnostic-2026-08-14  (starts with the required FAMILY_F_CAMPAIGN_ID_MARKER = "familyf-diagnostic-", confirmed against FamilyFConfigLoader's own validation at ReasoningProtocolFamilyFDiagnosticTest.kt:352-353)
```

No directory was created, no port was bound, and no daemon was launched to produce the above; they are proposals for a future Explicit Execution Approval to adopt, amend, or replace.

## 15. Item 15 — proposed campaign directory does not exist

```text
$ ls -la /var/lib/parker/reasoning-protocol-live-model/
qwen25coder7b-baseline-20260809
qwen25coder7b-llama32-3b-diagnostic-20260809
unit3c-remedy-experiments-20260810
unit3c-remedy-experiments-20260810-02
unit3c-remedy-experiments-20260810-03
```

No entry named `familyf-diagnostic-2026-08-14` (or any `familyf-diagnostic-*`) exists. Confirmed via a non-creating `ls` only.

```text
CAMPAIGN_DIRECTORY_ABSENT=CONFIRMED
```

## 16–17. Items 16–17 — production process baseline and isolation

Read-only `ps` inspection (no signal sent to either process):

```text
production Parker:  PID 5261, PPID 5236, user uid 999, started Sat Aug 8 12:41:11 2026, elapsed ~489437s
  cmd: /opt/java/openjdk/bin/java -classpath /opt/parker/lib/parker-platform-0.8.0-runtime-complete.jar:... parker.composition.MainKt

production Ollama:   PID 1926, PPID 1850, user root, started Sat Aug 8 08:40:51 2026, elapsed ~503858s
  cmd: /bin/ollama serve
  listening: 0.0.0.0:11434 and [::]:11434 (confirmed via read-only `ss -tln`, no request sent)
```

Both processes are present and alive under this read-only check. Section 7.2's passive `docker inspect`/`docker ps` queries additionally, and independently, confirm PID 1926 is the production Ollama container `ollama` (full ID `f795e46c5eac20f5fb423014b94d799df9ce2ce11ef1cb58a15b93d5885a258d`), running image `ollama/ollama:latest` at image ID `sha256:4dea9fb5...`, and published on host ports `11434/tcp` — this is read-only metadata confirmation, not a new interaction with the process. The proposed dedicated endpoint (`127.0.0.1:21434`), proposed evidence root, and proposed runtime root (Item 14) are each distinct from production's port (`11434`), production's container/image identity, and production's process identity (PIDs 1926/5261) by construction of the proposal. No signal, stop, restart, reconfiguration, or traffic-routing action was taken against either process or container.

```text
PRODUCTION_PROCESS_BASELINE=Parker PID 5261 (uid 999, started 2026-08-08T12:41:11Z); Ollama PID 1926 (root, started 2026-08-08T08:40:51Z, listening 0.0.0.0:11434), confirmed via passive Docker metadata to be container `ollama` (full ID f795e46c5eac20f5fb423014b94d799df9ce2ce11ef1cb58a15b93d5885a258d, image ollama/ollama:latest, image ID sha256:4dea9fb5...)
ISOLATION=proposed dedicated endpoint/roots/campaign ID are distinct from the confirmed production baseline by construction; actual isolation at execution time still depends in part on the unresolved portion of Item 7 (the dedicated daemon's own resolved-executable SHA-256 cannot yet be verified, though its container/image identity now can be)
```

## 18. Item 18 — exact Explicit Execution Approval value template (Plan Sections 19 and 25)

```text
EXPLICIT_APPROVAL_TEMPLATE:
  - approval document's own immutable identifier and document hash
  - exact accepted repository commit
  - campaign ID and evidence/artifact parent
  - dedicated upstream endpoint URL, launch procedure, and expected daemon executable identity (path + digest)
  - protected production endpoint identities
  - subject model name, digest, size, and metadata (qwen2.5-coder:7b)
  - control model name, digest, size, and metadata (llama3.2:3b)
  - provider binary path and digest
  - dedicated provider launch procedure and the observed PID recorded after approved launch
  - protected production PIDs (Parker, model daemon)
  - request timeout and unload timeout
  - the operating-system source for MemAvailable and disk-usable-space readings
  - schedule hash and expected counts (368 scored, 24 warm-up, 392 total)
  - authorized start window and human operator
  - the one permitted Gradle command
  - confirmation that only one fresh campaign is authorized
  - restatement that a halt, ambiguity, or failed campaign receives no automatic retry
  - restatement that Knowledge Discoverability Attempt 3 remains unauthorized
```

Reproduced from Plan Sections 19 and 25, cross-checked against `FamilyFConfigLoader`'s exhaustive environment-variable constant list (`ReasoningProtocolFamilyFDiagnosticTest.kt:318-334`): `CAMPAIGN_ID`, `ARTIFACT_ROOT`, `DEDICATED_RUNTIME_ROOT`, `DEDICATED_ENDPOINT_URL`, `REPOSITORY_COMMIT`, `SUBJECT_MODEL_DIGEST`, `SUBJECT_MODEL_SIZE_BYTES`, `CONTROL_MODEL_DIGEST`, `CONTROL_MODEL_SIZE_BYTES`, `PROVIDER_BINARY_PATH`, `PROVIDER_BINARY_DIGEST`, `PROTECTED_PARKER_PID`, `PROTECTED_MODEL_DAEMON_PID`, `REQUEST_TIMEOUT_MS`, `UNLOAD_TIMEOUT_MS`, `EXECUTION_APPROVAL_ID`, `EXECUTION_APPROVAL_HASH` — every template item above maps onto a concrete, already-implemented, exhaustively-validated configuration constant; nothing in the template is aspirational or unimplemented.

## 19. Item 19 — absence of prohibited activity during this readiness review

```text
MODEL_ACQUISITION=NONE PERFORMED
MODEL_LOAD=NONE PERFORMED
MODEL_ENDPOINT_CONTACT=NONE — no HTTP request was made to port 11434, port 21434, or any model endpoint
CAMPAIGN_CREATION=NONE — no directory was created beneath any artifact or runtime parent
PROVIDER_COMMAND_OR_API=NONE — no `ollama list`/`/api/tags`/`/api/show`/`/api/ps`/any provider CLI or HTTP endpoint was invoked at any point in this task
DOCKER_METADATA_QUERIES=`docker ps --no-trunc`, `docker inspect ollama`, `docker image inspect ollama/ollama:latest`, `docker volume inspect ollama_ollama_data` — four passive, read-only metadata queries only, used solely to correct Item 7/8's investigation (Sections 7.2 and 8.1); no `docker exec`, `docker cp`, `docker export`, `docker save`, container start/stop/restart, or volume mount was used at any point
MODEL_UNLOAD_OR_PROCESS_SIGNAL=NONE — no process or container was stopped, signaled, reconfigured, started, or restarted; `ps`/`ss`/`/proc`/`docker ps`/`docker inspect`/`docker image inspect`/`docker volume inspect` reads were the only interaction with process, container, or volume state
PRIVILEGE_ESCALATION=NONE — `sudo -n true` was tried only to confirm passwordless sudo was unavailable (it is), and no further privilege-escalation attempt followed; Docker-group membership was used only for passive metadata queries already available to this account, never treated as authorization to manipulate the container or model store
KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3=NOT STARTED, NOT REACHABLE — no code path exercised in this task touches ConversationReplyCoordinator, MemoryAdmissionCoordinator, ReasoningKnowledgeSource, KnowledgeSubmission, MemoryCore, or ParkerRuntime; this review is documentation- and read-only-command-only
```

## 20. Overall readiness determination

**Corrected.** Item 7 (provider executable identity) is now *partially* established: passive, already-available Docker metadata (Section 7.2), obtained without `sudo`, without any Ollama CLI/API call, and without any prohibited extraction or exec action, confirms the production container's exact identity, exact image ID/digest, exact entrypoint/command, and exact port mapping. What remains genuinely unestablished read-only is narrower than the original wording claimed: the resolved provider executable's own SHA-256 (Section 7.3), and — unchanged — Item 8's subject/control artifact identity, digest, and size, now confirmed blocked at the precise, Docker-identified volume mountpoint rather than at a set of unverified guesses (Section 8.1). Item 9 (a direct consequence) and Items 11–12 (a direct arithmetic dependency on Item 8) remain `UNDETERMINABLE` rather than passing, for the same underlying reason as before, now more precisely stated. Per the governing instruction for this task, an unresolved mandatory value is sufficient and mandatory grounds for `READINESS=NOT READY`; it is recorded as a blocker, not routed around by substituting a smaller model, a different host, reduced scope, or elevated privilege — and the correction itself introduces no new authority, since passive metadata queries are not equivalent to establishing the two still-missing values.

Every other item (3, 4, 5, 6, 10, 13, 14, 15, 16, 17, 18, 19) was independently, freshly verified and did not itself surface a blocker, though Item 13's disk margin is thin and shared with production on a single 88%-full volume, and Item 17's isolation confirmation is now only partially contingent on Item 7 — the dedicated daemon's own container/image identity can now be verified against an approval, but its resolved-executable SHA-256 still cannot.

This finding does not suggest a defect in the implementation, which independently, freshly re-passed every one of its own tests and detachment/double-gate checks in this task (Sections 3–6 above). It is a statement about this host's current, actual, permission-constrained environment, re-derivable and potentially resolved by a future readiness check performed with different account privileges (root) or a different extraction method — neither of which this task attempted or requests.

### 20.1 Correction record

This document was independently reviewed after its first draft; the review found that the original Item 7 investigation had not attempted an already-available, passive, read-only Docker metadata path, and that the original wording ("cannot be established read-only... not a workaround-avoidable gap") was therefore overbroad. This correction task performed exactly four passive Docker metadata queries (`docker ps --no-trunc`, `docker inspect`, `docker image inspect`, `docker volume inspect`) against the confirmed production Ollama container, its image, and its model-data volume — no `docker exec`, `docker cp`, `docker export`/`docker save`, container lifecycle action, volume mount, `sudo`, Ollama CLI/API call, model load, or inference occurred. The correction narrows Item 7 to its true remaining scope (the resolved executable's own SHA-256) and confirms, rather than resolves, Item 8's blocker at its now-precisely-identified location. `READINESS=NOT READY` is unchanged by this correction.

## Structured return block

```text
BASELINE=1c699f7c052971f71166f7fc0226164481dac62e
BRANCH=governance/reasoning-protocol-family-f-diagnostic-readiness-review
FILE_CREATED=docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_READINESS_REVIEW.md
GOVERNANCE_CHAIN=CONFIRMED (509f3f2, a4ccfe6, 0f9eb8b, c102a9e, 2853c49, 9ce2f4a, 73f8bdc, 710400a, 1c699f7 — all ACCEPTED/merged, all reachable from HEAD)
IMPLEMENTATION_STATUS=ACCEPTED (unaffected by this review)
ORDINARY_GRADLE_TEST=BUILD SUCCESSFUL (./gradlew test --rerun-tasks, 58s, 8/8 executed)
FOCUSED_OFFLINE_TESTS=112 total (91+21), 111 executed, 1 correctly self-skipped, 0 failures, 0 errors
TASK_DETACHMENT=CONFIRMED (absent from check/build/assemble dry-run graphs; listed only under verification group)
DOUBLE_GATE=CONFIRMED (empirically exercised: env value genuinely absent, live trigger test genuinely skipped)
PROVIDER_BINARY_IDENTITY=PARTIALLY ESTABLISHED READ-ONLY VIA PASSIVE DOCKER METADATA (corrected) — container ID f795e46c5eac20f5fb423014b94d799df9ce2ce11ef1cb58a15b93d5885a258d, image ollama/ollama:latest, image ID/digest sha256:4dea9fb511947e24a84237bb636b0203abcb2ff0d3fbc7b4ff865deb91362131, entrypoint+cmd "/bin/ollama serve" all confirmed without escalation; the resolved-executable's own SHA-256 remains CANNOT BE ESTABLISHED READ-ONLY — /proc/1926/exe permission-denied; /bin/ollama absent from this account's own mount namespace; reading the file requires a prohibited exec/extraction action or root
SUBJECT_ARTIFACT_IDENTITY_AND_SIZE=CANNOT BE ESTABLISHED READ-ONLY — exact location now confirmed via passive Docker metadata (/var/lib/docker/volumes/ollama_ollama_data/_data, bind-mounted to /root/.ollama in the container), permission-denied to this account at that exact location
CONTROL_ARTIFACT_IDENTITY_AND_SIZE=CANNOT BE ESTABLISHED READ-ONLY — same confirmed location and same permission-denied result
MEMAVAILABLE=2525956 kB (~2.41 GiB), read 2026-08-14T04:39:04Z from /proc/meminfo (original, preserved as historical evidence); follow-up reading 1714828 kB (~1.64 GiB), read 2026-08-14T04:59:48Z from /proc/meminfo (this correction task; does not replace the original)
SUBJECT_REQUIRED_MEMORY=UNDETERMINABLE (artifact size unknown; formula = size + 2 GiB)
CONTROL_REQUIRED_MEMORY=UNDETERMINABLE (artifact size unknown; formula = size + 2 GiB)
MEMORY_GATE=NOT ESTABLISHED — blocked upstream by artifact-size unavailability, now confirmed rather than merely unresolved at the correct location; recorded as a blocker per instruction, not worked around
DISK_GATE=3.5 GiB usable at proposed evidence-root/runtime-root parent (single shared 88%-full root filesystem; exceeds the flat 2 GiB minimum but with thin margin and no dedicated volume)
PROPOSED_DEDICATED_ENDPOINT=http://127.0.0.1:21434 (proposed only; distinct from confirmed production 0.0.0.0:11434)
PROPOSED_RUNTIME_ROOT=/var/lib/parker/reasoning-protocol-live-model-runtime/familyf-diagnostic-2026-08-14 (proposed only; does not exist)
PROPOSED_EVIDENCE_ROOT=/var/lib/parker/reasoning-protocol-live-model/familyf-diagnostic-2026-08-14 (proposed only; does not exist)
PROPOSED_CAMPAIGN_ID=familyf-diagnostic-2026-08-14 (starts with required marker, validated against FamilyFConfigLoader's own rule)
PRODUCTION_PROCESS_BASELINE=Parker PID 5261 (uid 999, started 2026-08-08T12:41:11Z); Ollama PID 1926 (root, started 2026-08-08T08:40:51Z, listening 0.0.0.0:11434/[::]:11434), confirmed via passive Docker metadata to be container `ollama` — both confirmed alive via read-only ps/docker inspect, neither signaled nor started/stopped
ISOLATION=proposed dedicated endpoint/roots/campaign ID distinct from confirmed production baseline by construction; container/image identity now verifiable via passive Docker metadata; the dedicated daemon's resolved-executable SHA-256 remains unverifiable pending root access or a future, separately-governed extraction method
EXPLICIT_APPROVAL_TEMPLATE=reproduced in full in Section 18 above, cross-checked against FamilyFConfigLoader's exhaustive environment-variable constants; nothing aspirational or unimplemented
MODEL_CONTACT=NONE
KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3=NOT STARTED, NOT REACHABLE
READINESS=NOT READY (unchanged by this correction)
OVERCLAIM_REMOVED=YES — "cannot be established read-only" and "not a workaround-avoidable gap" (original Item 7) corrected to reflect the passive Docker metadata path in Sections 7.2-7.4; Item 8's guessed paths (Section 8) superseded by the Docker-confirmed exact mountpoint in Section 8.1; underlying NOT READY conclusion unchanged
BLOCKERS=[1] provider executable's resolved-binary SHA-256 not establishable read-only on this host/account (container/image identity now IS establishable, per the Section 7 correction); [2] subject and control model artifact identity/digest/size not establishable read-only on this host/account, now confirmed at the exact Docker-identified volume mountpoint; [3] consequential: pre-load memory-gate thresholds not computable without [2]; disk margin is thin (3.5 GiB, single shared 88%-full volume) though it does not itself fail the flat 2 GiB minimum
DIFF_CHECK=CLEAN (git diff --check on the corrected file: no output)
FILES_CHANGED=1 (docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_READINESS_REVIEW.md, corrected in place; still untracked, uncommitted)
GIT_STATUS=1 untracked file (corrected in place); nothing staged, committed, or pushed; no PR opened; no Explicit Execution Approval issued; no Independent Constitutional Review created
```
