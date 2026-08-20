# Controlled Live Owner REMEMBER Verification Execution Authorization — Independent Constitutional Review

**Status:** Independent Constitutional Review of
`docs/decisions/CONTROLLED_LIVE_OWNER_REMEMBER_VERIFICATION_EXECUTION_AUTHORIZATION.md`
— **ACCEPTED.**

## 1. Repository state and review boundary

```text
BRANCH=main
HEAD=482ccdae6a5ef3a798ab0ceb803fddfa3ea8d22d
ORIGIN_MAIN=482ccdae6a5ef3a798ab0ceb803fddfa3ea8d22d
```

Pre-review status was exactly the two new untracked files (the authorization
under review and this document). This review created only this document. It
did not edit the authorization, any governance document, or any source file.

## 2. Governance re-derived fresh, independently

Re-read directly (not trusted from the authorization's own citations):
`docs/architecture/BASIC_OWNER_UI_LIVE_VERIFICATION_BLOCKER_RESOLUTION_SCOPE_LOCK.md`
§8 (live-retest prerequisites) and §9 (exclusions/stop boundary);
`docs/reviews/BASIC_OWNER_UI_LIVE_VERIFICATION_BLOCKER_RESOLUTION_COMPLETION_REVIEW.md`
status line; `docs/reviews/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_LIVE_VERIFICATION_ATTEMPTS_1_2_REVIEW.md`
§1–3; `docs/architecture/AUTHORIZATION_PURPOSE_SCOPE_LOCK.md` §2.5–2.6.

**Finding:** the UI scope lock's own §8/§9 text is confined, by its own
Section 1 baseline and Section 3/6 file lists, to the Windows-local owner UI
presentation/composition surface (`OwnerWindowPresentation.kt`,
`ParkerOwnerWindow.kt`, `OfflineOwnerUiMain.kt`, `OwnerUiMain.kt`,
`OwnerUiRuntimeAdapter.kt`) and a Windows-local endpoint dependency. It does
not name, reference, or constrain the `--interactive` CLI adapter
(`Main.kt`, `InteractiveConsole.kt`) at all. The authorization under review
does not claim that lock's prerequisites are satisfied; it correctly treats
only the *general pattern* that lock establishes (live model-backed
verification requires its own separate execution authorization) as binding,
and supplies exactly that separate authorization for a different surface.
This is a correct, non-overreaching reading — it neither claims the UI
lock's prerequisites are met (they are not, and are irrelevant here) nor
claims the UI lock forbids this different surface (it does not address it).

**Finding:** the Knowledge Discoverability live-verification precedent
independently confirms real, same-runtime live verification against a real
Ollama server is a lawful, previously-performed activity in this
repository — this is direct evidence against any reading that live model
calls are categorically forbidden platform-wide, and supports the
authorization's Section 2 citation of it accurately (it is cited for
precedent-of-activity-type only, not as a source of authority for *this*
specific test, which the authorization correctly does not claim).

## 3. Source-inspection claims independently re-verified

Read fresh, in full: `src/composition/Main.kt`,
`src/composition/InteractiveConsole.kt`,
`src/runtime/ReasoningResponseParser.kt` (relevant section),
`src/composition/ParkerRuntime.kt` (relevant sections, lines ~740-930),
`src/runtime/FileSystemMemoryCoreDurabilityLog.kt`,
`src/runtime/DurableMemoryCoreEntryCodec.kt` (header/format section),
`docker-compose.yml`, `Dockerfile`.

Every specific claim in the authorization's Section 3 was independently
checked against this source, not accepted on the authorization's own word:

| Claim | Independent check | Result |
|---|---|---|
| `--interactive` is a real, existing, documented flag | `Main.kt` line 55 (`"--interactive" in args`), KDoc lines 18-29 | **CONFIRMED** |
| Stdin adapter shape (`readLine -> submitOwnerMessage -> print`) | `InteractiveConsole.kt` `runInteractiveConsole`, lines 110-177 | **CONFIRMED** |
| `channel.local-text` / owner principal wiring | `Main.kt` lines 131-132, matches `docker-compose.yml`'s `PARKER_LOCAL_TEXT_CHANNEL_MODULE_ID` default | **CONFIRMED** |
| `REMEMBER:<text>` parses to `ReasoningProviderResponse.Remember` | `ReasoningResponseParser.kt` line 71 | **CONFIRMED** |
| `DurableMemoryCore`/`FileSystemMemoryCoreDurabilityLog` wired into production `ParkerRuntime` composition, not merely built in isolation | `ParkerRuntime.kt` lines 71, 81-82, 777, 780, 923; config keys read at `ParkerRuntimeConfig.kt` lines 199-200, 279-280 | **CONFIRMED** |
| Durability log format (tab-separated `key=value`, Base64 string fields, `kind`/`schemaVersion` first) | `DurableMemoryCoreEntryCodec.kt` header KDoc lines 31-77 | **CONFIRMED, and independently decodable read-only without Parker-internal tooling** |
| Deployed compose config matches (`host.docker.internal:11434`, `qwen2.5-coder:7b` default, `/data/memory-core/durability.log`, `/data/knowledge-items/durability.log`) | `docker-compose.yml` environment/volumes blocks | **CONFIRMED** |
| No production/runtime file is proposed for change by this authorization | Authorization Section 4.2 | **CONFIRMED — the authorization text itself contains no code, and this review adds none** |

No claim in the authorization's Section 3 was found to be inflated,
assumed, or unverifiable from current source.

## 4. Scope discipline

**CONFORMING.** Section 4.1 (A-I) matches, item for item, the task's own
authorized-actions list. Section 4.2's exclusion list matches, item for
item, the task's own "MUST NOT authorize" list, with no omission and no
addition of unauthorized scope. The synthetic-data distinguishability
requirement (Section 1 status line, Section 4 opening) is present and
explicit, satisfying the task's own requirement that the fact "must remain
distinguishable from real owner/case knowledge."

## 5. Self-authorization risk

**CONFORMING.** Section 6 of the authorization explicitly defers its own
effectiveness to this Independent Constitutional Review's acceptance and a
governance-acceptance commit reaching `origin/main` — it does not treat its
own drafting as sufficient. This mirrors the FamilyFRole Source Correction
Amendment's own accepted precedent for exactly this two-document,
commit-gated effectiveness pattern.

## 6. Constitutional risk sweep

- **Second authorization system?** No — this document creates no new
  standing authorization mechanism; it is a single-use, single-fact grant
  consumed by one Gate 2 execution.
- **Precedent creep?** No — Section 4.2 is exhaustive and explicit; nothing
  in this authorization can be read as licensing any future live test
  beyond the one named fact and the two named messages.
- **Conflict with Memory Core governance?** No — Section 2/3 confirm this
  authorization changes nothing about Memory Core's contract, schema, or
  durability mechanism; it only observes the existing, already-accepted
  mechanism from outside.
- **Conflict with Family F governance?** No — Section 4.2 names the Family F
  approval variable and evidence-production activity explicitly as excluded.
- **Destructive-action risk?** No — Section 4.2 forbids deletion, truncation,
  or rewriting of any existing canonical record, and Section 5 of the
  authorization forbids manual log editing or API bypass.

## 7. Verdict

**ACCEPTED.** The authorization is narrowly scoped, its every factual claim
about the current implementation is independently confirmed against fresh
source reading rather than assumed, its exclusions are exhaustive and
correctly transcribed from the governing task, and it correctly declines to
either overreach into the separate, still-open Basic Owner UI prerequisite
chain or claim any Family F authority. It becomes constitutionally effective
once committed to `main` per its own Section 6, which this review's
acceptance now permits.

```text
CONTROLLED LIVE OWNER REMEMBER VERIFICATION EXECUTION AUTHORIZATION —
INDEPENDENT CONSTITUTIONAL REVIEW COMPLETE — ACCEPTED
```
