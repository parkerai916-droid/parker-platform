**Status:** Planning Review only. No Kotlin is implemented, proposed as a diff, or changed by this document. Repository baseline independently re-verified below, not assumed from the task's own claim. This document does not amend any governance document.

# Authorization Purpose — Unit 6 (End-to-End Verification) — Planning Review

## 0. Baseline, Independently Re-Verified

`git log --oneline -5`: `HEAD` is `aa4ee9c "feat: implement Authorization Purpose unit 5"`, tracking `origin/main`. Units 1–5 confirmed committed and pushed.

**A genuine, independently-discovered baseline fact, disclosed rather than silently absorbed**: the committed `aa4ee9c` snapshot of `src/composition/ParkerRuntime.kt` contains **more than Unit 5's own diff** — it also contains the previously-uncommitted Conversational Memory Admission wiring (`MemoryAdmissionCoordinator` import, construction, and two registration stages), confirmed by `git show HEAD:src/composition/ParkerRuntime.kt | grep MemoryAdmissionCoordinator` returning four matches. `src/runtime/MemoryAdmissionCoordinator.kt` itself, however, **remains untracked** (`git status --short` shows `?? src/runtime/MemoryAdmissionCoordinator.kt`). This means the committed history, checked out in isolation, would not compile — a pre-existing git-hygiene gap in how a prior task's commit was made, not a defect in Authorization Purpose Units 1–5 themselves, and not something this Unit's own "verification-first, no production Kotlin changes" mandate authorises fixing (doing so would mean staging/committing Conversational Memory Admission's own file, explicitly forbidden by this task's own stop condition). **Disclosed here, not corrected, and accounted for below**: because `MemoryAdmissionCoordinator` is now genuinely part of the real, composed runtime graph (its class file present on disk, referenced by committed code, and exercised by `./gradlew clean test` locally), it is treated as a real "existing consumer" for this Unit's own non-adoption checks, alongside `DefaultKnowledgeCandidateEvaluator`/`EvidenceIntelligenceInputResolver`.

`git status --short` otherwise shows only pre-existing, unrelated uncommitted work (the Conversational Memory Admission source/test/review files themselves, a Programme 3 clarification), unchanged by this Unit.

---

## 1. Fresh Re-Reading — Findings

**Scope Lock** (`AUTHORIZATION_PURPOSE_SCOPE_LOCK.md`), read in full: §2.2 (carrier, `PermissionEngine` unchanged), §2.3 (vocabulary: closed, reject-on-conflict, retirement without deletion), §2.4 (single engine/policy, fail-closed, precedence-safety as an outcome constraint not an algorithm, no caller-specific exceptions), §2.5 (Gap #54 depends on Authorization Purpose but remains separately blocked), §2.6 (boundaries — not authentication/identity/delegation/etc.).

**Implementation Plan** (`AUTHORIZATION_PURPOSE_IMPLEMENTATION_PLAN.md`) §8, Unit 6, read in full: Purpose (prove the composed mechanism — fail-closed, precedence-safe, no regression — without real adoption), Outputs (a single synthetic, test-tier-only value; "end-to-end" means invoking the real `DefaultPermissionPolicy`/registry pairing from a test harness, not that `ParkerRuntime.kt` gains test content, and not a narrower substitute that avoids the real composed objects; proof required: absent denies, unregistered denies, retired denies, registered-and-eligible participates in precedence-safe resolution, existing callers unchanged), Files expected to change (new test files only), Non-responsibilities (no real value, no policy-content decision, not Gap #54/Knowledge Submission/Conversational Retrieval implementation), Stop condition (if verification requires a real domain value to be convincing, stop and report).

**Unit 1–5 Completion Reviews and Independent Constitutional Reviews**, all read in full, fresh (not from memory): Units 1 and 2 each required one narrow, disclosure-only correction (Unit 1: an undisclosed `EventType` precedent for Unit 3's future benefit; Unit 2: a missed third specification document), both since corrected, confirmed via their own Defect Confirmation Reviews. Units 3, 4, and 5 each received a clean `ACCEPTED` verdict with no required correction. No open, unresolved finding from any prior Unit's own ICR remains outstanding.

**Current production implementation**, read fresh in full: `src/contracts/Identifiers.kt` (`AuthorizationPurposeId`, unchanged since Unit 1's own correction), `src/contracts/ExecutionRequest.kt` (`authorizationPurpose: AuthorizationPurposeId? = null`), `src/contracts/AuthorizationPurposeVocabulary.kt` and `src/runtime/AuthorizationPurposeRegistry.kt` (Unit 3, unchanged), `src/runtime/DefaultPermissionPolicy.kt` (Unit 4's precedence mechanism, unchanged), `src/composition/ParkerRuntime.kt` (Unit 5's wiring, unchanged in substance from what was committed), `src/interfaces/PermissionEngine.kt` and `src/runtime/DefaultPermissionEngine.kt` (both untouched by any Authorization Purpose Unit, confirmed by zero diff across all five prior ICRs and independently re-read here).

---

## 2. The One Genuinely New Path This Unit Must Exercise

Every prior Unit's own tests either (a) exercise `DefaultPermissionPolicy.evaluate` **directly**, bypassing `DefaultPermissionEngine` entirely (Unit 3, Unit 4, and Unit 5's own composition tests all do this — confirmed by fresh re-reading each file), or (b) exercise `DefaultPermissionEngine` **without** any Authorization-Purpose-bearing request (the pre-existing `DefaultPermissionEngineTest.kt`, read fresh above, never sets `authorizationPurpose`). **No existing test anywhere in the repository exercises the full `AuthorizationPurposeId → AuthorizationPurposeRegistry → ExecutionRequest.authorizationPurpose → DefaultPermissionPolicy → DefaultPermissionEngine → PermissionDecision` path this task's own prompt names as the required end-to-end chain.** This is the genuine, primary coverage gap Unit 6 exists to close — not a re-test of anything already proven, but the one hop (`DefaultPermissionEngine`'s own identity-resolution gate, sitting in front of an Authorization-Purpose-aware policy) no prior Unit's own tests ever combined.

---

## 3. Verification Matrix

| # | Frozen property | Authority | Existing coverage | Unit 6 gap / action |
|---|---|---|---|---|
| 1 | Registered+active purpose selects a purpose-aware rule, both ALLOW and DENY | Scope Lock §2.4 | `DefaultPermissionPolicyTest.kt` tests 12–13 (direct policy only) | **Gap**: never proven through `DefaultPermissionEngine`. New tests 1–2. |
| 2 | Precedence safety, both directions, order-independent | Scope Lock §2.4 | `DefaultPermissionPolicyTest.kt` tests 12, 13, 17 (direct policy only) | **Gap**: never proven through `DefaultPermissionEngine`. New tests 3–5. |
| 3 | Absent purpose retains pre-existing behaviour | Scope Lock §2.4, Implementation Plan §6/§8 | `DefaultPermissionPolicyTest.kt` tests 10–11; `ParkerRuntimeAuthorizationPurposeCompositionTest.kt` (direct policy, real composition, no purpose-aware rule) | **Partial gap**: no test proves this through the full engine chain *in the presence of* a purpose-aware rule. New test 6; extended by Unit 6's own composition-level regression test (below). |
| 4 | Unregistered purpose cannot gain authority | Scope Lock §2.4 | `DefaultPermissionPolicyTest.kt` test 14 (direct policy only) | **Gap**: through `DefaultPermissionEngine`. New test 7. |
| 5 | Retired purpose cannot participate; historical presence intact | Scope Lock §2.3/§2.4 | `DefaultPermissionPolicyTest.kt` test 15 (direct policy only); `AuthorizationPurposeRegistryTest.kt` (registry alone) | **Gap**: through `DefaultPermissionEngine`, and combined with lookup-visibility in the same test. New test 8. |
| 6 | Registry: reject-on-conflict, retirement without deletion, no silent reactivation | Scope Lock §2.3 | `AuthorizationPurposeRegistryTest.kt`, full (19 tests) | **No gap in substance** — already fully proven in isolation. Task explicitly forbids duplicating Unit 3's suite; add one narrow, combined test proving these three properties hold on the registry instance actually wired into a real policy/engine pairing. New test 9. |
| 7 | Single registry / single policy / single engine in the composed runtime | Scope Lock §2.4 | `ParkerRuntimeAuthorizationPurposeCompositionTest.kt` (construction-count structural proof, Unit 5) | **No gap in substance**; one additional direct type-identity assertion added for completeness. New composition test A. |
| 8 | Zero production purposes registered | Implementation Plan §7/§8 | `ParkerRuntimeAuthorizationPurposeCompositionTest.kt` (Unit 5, `entries` map emptiness) | **No gap** — already proven; not duplicated. |
| 9 | Existing behaviour regression, real composed runtime | Implementation Plan §5/§8 | `ParkerRuntimeAuthorizationPurposeCompositionTest.kt` (Unit 5, direct `policy.evaluate`, bypassing identity) | **Gap**: never proven through the full composed `permissionEngine.evaluate` (identity + policy). New composition test B. |
| 10 | Gap #54 remains unresolved; Authorization Purpose infrastructure alone does not authorise `memory.retrieve` | Scope Lock §2.5 | None — never tested anywhere in this Programme | **Gap, mandatory.** New composition test C. |

No test is added merely to inflate a count; every new test above closes a named, specific gap this matrix identifies, or (rows 6–7) narrowly proves a property already covered in isolation now also holds in the integrated instance, exactly as the task's own "exercise only what is necessary to prove the integrated capability" instruction requires.

---

## 4. Test Architecture — Two Deliberately Different Vehicles

**Vehicle A — a dedicated, new file, `tests/runtime/AuthorizationPurposeEndToEndVerificationTest.kt`**, for matrix rows 1–6. Constructs the **real** `AuthorizationPurposeId`, `InMemoryAuthorizationPurposeRegistry`, `DefaultPermissionPolicy`, `DefaultPermissionEngine`, and `InMemoryIdentityService` directly — mirroring the pre-existing `DefaultPermissionEngineTest.kt`'s own already-established, already-ICR-accepted construction pattern (`registerAt`/`newPrincipal` helpers, reproduced verbatim) — with a test-supplied rule list, so a purpose-aware `PermissionPolicyRule` can exist for precedence testing (rows 1–2), something the real `ParkerRuntime`-composed policy's own fixed, production rule list cannot supply without an unauthorised production change. **This is not "a second authorization path"**: it is the one, real `DefaultPermissionEngine`/`DefaultPermissionPolicy` class pair, the identical classes `ParkerRuntime.kt` composes, exercised with test data — the same posture `DefaultPermissionEngineTest.kt` and `DefaultPermissionPolicyTest.kt` already hold and have already passed ICR under. The Implementation Plan's own Unit 6 Outputs text supports this reading directly: "invoking that real, composed pairing from a test harness — not that `ParkerRuntime.kt` itself gains any test-only content" names the **class pairing**, not one specific object graph, as what must be real.

**Vehicle B — extending the existing `tests/composition/ParkerRuntimeAuthorizationPurposeCompositionTest.kt`** (Unit 5's own file, already scoped to Authorization Purpose within the real composed runtime, already carrying the established `privateField` reflection helper), for matrix rows 7, 9, 10 — properties that specifically require the real `ParkerRuntime`'s own composition (single-instance structure; regression against real, already-registered production actions; the real, deliberately-unregistered `memory.retrieve` verb phrase). Reflection is used only exactly as Unit 5 already established it (no new technique, no new helper).

No production accessor is added to any `src/` file for either vehicle.

---

## 5. Requirement 10 (Gap #54) — Design, and the Disclosed Limitation

Independently re-confirmed, fresh, against the current `ParkerRuntime.kt`: `memory.retrieve` (`PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME`) remains **deliberately unregistered** in the composed `ActionVocabulary` (`ParkerRuntime.kt`'s own comment, line ~792: "memory.retrieve/memory.retrieve_document actions are deliberately left unregistered"), and `PermissionFilteredMemoryRetrieval`'s own request construction always sets `targetResources = emptyList()`. The exact live path (calling `PermissionFilteredMemoryRetrieval` itself) is **not** exercised — doing so would mean invoking, and therefore testing assumptions about, a class this task's own explicit exclusion list names ("do not modify `PermissionFilteredMemoryRetrieval`"); reading/calling it is not modifying it, but the narrower, cleaner, equally-truthful proof is to construct the identical `(verbPhrase, emptyList())` shape directly and evaluate it through the real composed `permissionEngine` — the **nearest truthful existing seam**, per this task's own explicit instruction, without depending on or exercising Memory Retrieval's own code at all. This proves the invariant this task requires (Authorization Purpose infrastructure alone has not accidentally authorised this path) while disclosing, here, that it does not exercise `PermissionFilteredMemoryRetrieval`'s own call site verbatim — a deliberate, narrower substitute, not a broader one, chosen specifically because Unit 6's own non-responsibilities forbid going further.

---

## 6. Boundary Review

**Not genuinely required.** The one substantive design question this Unit faced — how to prove precedence-safety and fail-closed behaviour through the real `DefaultPermissionEngine` without adding a rule to production — resolved to a single, well-precedented answer (Vehicle A, Section 4) directly supported by both the Implementation Plan's own text and an already-ICR-accepted prior test file's own established pattern, not a choice between competing lawful designs needing external adjudication.

---

## 7. Non-Responsibilities Confirmed Unchanged

No real Authorization Purpose value registered anywhere (every value used is `test.`-prefixed, distinguishable, mirroring every prior Unit's own convention). No `memory.retrieve` policy content added. No Memory Retrieval verb-phrase discrimination added. No change to `PermissionFilteredMemoryRetrieval`, Knowledge Submission, Evidence Intelligence, or Conversational Memory Admission. No production Kotlin file expected to change (Section 8 confirms whether this held).

```
UNIT 6 PLANNING REVIEW COMPLETE — PROCEEDING TO VERIFICATION IMPLEMENTATION
```
