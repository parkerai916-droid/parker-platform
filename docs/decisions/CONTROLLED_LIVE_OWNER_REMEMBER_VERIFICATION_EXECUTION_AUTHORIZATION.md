# Controlled Live Owner REMEMBER Verification — Execution Authorization

**Status:** Narrow, single-use execution authorization — the "separate
live-verification execution authorization" prerequisite identified by
`docs/architecture/BASIC_OWNER_UI_LIVE_VERIFICATION_BLOCKER_RESOLUTION_SCOPE_LOCK.md`
§8 item 9, applied here to a different invocation surface (the existing
`--interactive` CLI adapter, not the owner UI). This document authorizes
exactly one controlled live test sequence. It is not a Scope Lock, not an
Implementation Plan, and does not authorize any code, configuration, or
architecture change. Nothing outside this document and its own Independent
Constitutional Review is staged, committed, or pushed by this document
itself.

## 1. Baseline

```text
BRANCH=main
HEAD=482ccdae6a5ef3a798ab0ceb803fddfa3ea8d22d
ORIGIN_MAIN=482ccdae6a5ef3a798ab0ceb803fddfa3ea8d22d
WORKING_TREE=clean
```

## 2. Governance read fresh

- `docs/architecture/BASIC_OWNER_UI_LIVE_VERIFICATION_BLOCKER_RESOLUTION_SCOPE_LOCK.md`
  — establishes the repository's binding pattern that any live model-backed
  verification requires prerequisites including a separate execution
  authorization (§8 item 9), and that no scope lock or completion review by
  itself authorizes a live call (§5, §9).
- `docs/reviews/BASIC_OWNER_UI_LIVE_VERIFICATION_BLOCKER_RESOLUTION_COMPLETION_REVIEW.md`
  and its Independent Review — confirm that unit's own status remains
  "READY FOR LIVE RETEST ONCE THE SEPARATELY AUTHORIZED... PREREQUISITE
  EXISTS," a different, still-open prerequisite chain this document does not
  touch or satisfy.
- `docs/reviews/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_LIVE_VERIFICATION_ATTEMPTS_1_2_REVIEW.md`
  — direct repository precedent that a real, same-runtime live verification
  against a real local Ollama server and a real model is a lawful,
  previously-performed activity type in this repository, factually recorded
  rather than fabricated, and not itself blocked by the UI-specific scope
  lock above (a different subsystem, a different invocation surface).
- `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md` and
  `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` (plus errata 001-004) —
  confirm Memory Core durability is already-accepted, already-implemented
  governance; this document does not amend, extend, or reinterpret either.
- `docs/architecture/AUTHORIZATION_PURPOSE_SCOPE_LOCK.md` — confirms
  Authorization Purpose is a separate, unrelated constitutional track; this
  document does not touch it.
- Grep of `docs/architecture/` and `docs/decisions/` for "Explicit Execution
  Approval" found no repository-wide concept by that name outside the
  Family F track's own vocabulary; this document does not borrow or imply
  that specific Family F term.

## 3. Owner-message invocation surface, confirmed by direct source inspection

Read fresh, in full: `src/composition/Main.kt`,
`src/composition/InteractiveConsole.kt`. Confirmed, not assumed:

- `parker`'s single OS entry point (`fun main(args: Array<String>)`,
  `mainClass = parker.composition.MainKt`) checks for a literal
  `--interactive` program argument. Documented in `Main.kt`'s own KDoc as
  "development/debug only," strictly opt-in, and unchanged in headless mode.
- When present, `main` switches from the headless wait to
  `runInteractiveConsole`, a stdin adapter: `readLine -> InboundOwnerMessage
  -> ParkerRuntime.submitOwnerMessage -> print`. `channelId` is
  `ModuleId(config.localTextChannelModuleId)` (`channel.local-text` by the
  existing deployed `docker-compose.yml` default), sender is
  `PrincipalId(config.ownerPrincipalId)`.
- The lawful invocation is therefore `docker compose run --rm parker
  --interactive` (or an environment-variable-overridden equivalent of the
  same container/image/compose project the already-deployed
  `parker-runtime` service uses) — exactly the mechanism the immediately
  preceding, correctly-stopped attempt proposed, now independently confirmed
  from source rather than assumed.
- Classification path, confirmed from `src/runtime/ReasoningResponseParser.kt`:
  a model reply matching the `REMEMBER:<text>` tag is parsed into
  `ReasoningProviderResponse.Remember(text)`. This is the authoritative
  classification signal this authorization's Gate 2 must observe.
- Durability path, confirmed from `src/composition/ParkerRuntime.kt`,
  `src/runtime/FileSystemMemoryCoreDurabilityLog.kt`,
  `src/runtime/DurableMemoryCoreEntryCodec.kt`: `DurableMemoryCore.create()`
  wraps `InMemoryMemoryCore` with `FileSystemMemoryCoreDurabilityLog` at
  `config.memoryCoreDurabilityLogPath` (`PARKER_MEMORY_CORE_DURABILITY_LOG_PATH`,
  `/data/memory-core/durability.log` in the existing deployed compose
  configuration) and replays it at startup; `FileSystemKnowledgeItemDurabilityLog`
  does the analogous job for Knowledge Items. Entries are one per line,
  tab-separated `key=value` fields, `kind`/`schemaVersion` first, every
  string-valued field Base64-encoded — independently decodable read-only
  without any Parker-internal tooling.

This confirms both the entry surface and the durability artifact this
authorization's Gate 2 will observe are real, already-implemented, and
already wired into the production composition graph — not a hypothetical or
partially-built capability.

## 4. Scope of this authorization

Limited to one test fact only:

```text
"The test lighthouse is painted orange."
```

This is synthetic test data. It carries no relation to any real owner fact,
case fact, or evidentiary content, and must remain distinguishable as such
in every artifact this test produces (log files, this document, and any
later review referencing it).

### 4.1 Authorized

- **A.** Read-only inspection necessary to identify the real, currently
  supported owner-message entry surface (already performed in Section 3
  above; may be repeated/re-confirmed at execution time).
- **B.** Starting or restarting the already-deployed Parker runtime
  components (`docker compose up -d parker`, `docker compose run --rm
  parker --interactive`, `docker stop`/equivalent on the existing
  `parker-runtime` container) using the existing, unmodified
  `docker-compose.yml` and `Dockerfile` — no rebuild unless source/image
  drift is actually proven, and none is authorized to be introduced by this
  document.
- **C.** Use of the existing deployed Ollama/model configuration exactly as
  `docker-compose.yml` already declares it
  (`PARKER_MODEL_ENDPOINT_URL=http://host.docker.internal:11434/api/generate`,
  `PARKER_MODEL_NAME` default `qwen2.5-coder:7b`) — no model download,
  substitution, or reconfiguration.
- **D.** Submission of exactly: `Remember the test lighthouse is painted
  orange.`
- **E.** Observation and recording of Parker's response and resulting
  governed memory/knowledge persistence.
- **F.** Controlled restart/recovery of Parker sufficient to cross a genuine
  durability boundary, using the least invasive already-authorized mechanism
  (item B).
- **G.** Submission, after that boundary, of exactly: `What colour is the
  test lighthouse?`
- **H.** Observation and recording of Parker's answer and the authoritative
  canonical record(s) used to produce it.
- **I.** Read-only inspection, hashing, and size comparison of the Memory
  Core and Knowledge Item durability logs (and no other persistence
  artifact) necessary to prove persistence and retrieval.

### 4.2 Not authorized

This document does not authorize, and any of the following discovered as
necessary during execution requires STOPPING and returning to governance
rather than proceeding:

- model download, replacement, or any model-role change;
- Family F Bounding Evidence production of any kind, or setting/using
  `PARKER_REASONING_FAMILY_F_BOUNDING_EVIDENCE_APPROVED`;
- numeric-bound selection, qualification, or remedy selection;
- any live-model testing unrelated to the single lighthouse fact above;
- production deployment changes, image rebuilds, or `Dockerfile`/
  `docker-compose.yml` edits;
- firewall or network configuration changes;
- QMD architectural changes (QMD may only be observed passively if the
  deployed runtime invokes it automatically; it must not be manually seeded,
  configured, or altered);
- Memory Core or Knowledge Item architecture, schema, or contract changes;
- source-code corrections of any kind, including to fix a test failure
  discovered during execution;
- UI implementation changes (this authorization is scoped to the CLI
  `--interactive` surface only; it does not extend, satisfy, or shortcut the
  separate, still-open Basic Owner UI live-verification prerequisite chain);
- deletion, truncation, or rewriting of any existing canonical memory or
  knowledge record, including records unrelated to the lighthouse fact;
- arbitrary, open-ended interactive Parker use beyond the two exact messages
  named in 4.1.D and 4.1.G.

## 5. Execution discipline binding on Gate 2

- No manual editing of durability logs, no direct memory/knowledge API call
  bypassing the owner-message path, no prompt/parser logic change, no model
  substitution, and no second-conversation hint containing "orange" —
  Parker's actual, unassisted owner-message path is what is under test.
- The synthetic fact is not deleted after the test unless a future,
  separate governance act requires synthetic-test cleanup; it is left in
  place as ordinary accepted test evidence.
- If the classification, persistence, or retrieval step fails, Gate 2 stops
  at that boundary, preserves the failing state for diagnosis, and does not
  attempt repair within the same task.

## 6. Effectiveness

This authorization becomes constitutionally effective only once its
Independent Constitutional Review accepts it and both documents are
committed to `main` (mirroring the governance-acceptance discipline already
used for the FamilyFRole Source Correction Amendment). Gate 2 of the task
that requested this document may not begin before that commit is confirmed
and `HEAD == origin/main`.

## 7. Independent Constitutional Review Self-Check

- **Does this authorize code, configuration, or architecture change?** No —
  Section 4.2 explicitly excludes every category of change; only inspection,
  container start/restart, and two exact owner messages are authorized.
- **Does this conflict with the Basic Owner UI Live Verification Blocker
  Resolution Scope Lock?** No — that lock governs a different subsystem (the
  owner UI) and a different, still-unsatisfied prerequisite chain (a
  Windows-local endpoint); this document neither claims to satisfy that
  chain nor authorizes UI work.
- **Does this imply Family F authority?** No — Section 4.2 explicitly
  excludes Family F evidence production and the Family F approval variable
  by name.
- **Does this risk destroying existing canonical memory?** No — Section 4.2
  explicitly forbids deletion, truncation, or rewriting of any existing
  record.
- **Is the test fact distinguishable from real data?** Yes — Section 4 and
  Section 1's status line both state explicitly that the lighthouse fact is
  synthetic test data.
- **Does this authorize itself into effect?** No — Section 6 requires a
  separate Independent Constitutional Review and a governance-acceptance
  commit before Gate 2 may begin.

```text
CONTROLLED LIVE OWNER REMEMBER VERIFICATION EXECUTION AUTHORIZATION —
DRAFT COMPLETE, PENDING INDEPENDENT CONSTITUTIONAL REVIEW
```
