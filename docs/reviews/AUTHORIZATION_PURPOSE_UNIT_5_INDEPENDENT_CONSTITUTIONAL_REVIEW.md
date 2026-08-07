**Status:** Genuine Independent Constitutional Review of Authorization Purpose Unit 5, performed as if by another reviewer, against Scope Lock §2.3/§2.4 and the Implementation Plan's own Unit 5 section directly, and against the actual, current file contents — not against the Completion Review's own account alone. This document does not amend any governance document. Nothing is staged, committed, or pushed.

# Authorization Purpose — Unit 5 (Composition Wiring) — Independent Constitutional Review

## 1. Baseline and Diff-Shape Re-Verification

Independently re-ran `git diff --stat` and `git diff -- src/composition/ParkerRuntime.kt` in full. **Found the raw diff for this one file is larger than this Unit's own change** — it also contains the pre-existing, already-uncommitted Conversational Memory Admission work (a `MemoryAdmissionCoordinator` import, its construction, two new resource/vocabulary registration stages, and a `conversationReplyCoordinator` reordering), confirmed present in the baseline `git status --short` recorded before this Unit's own Planning Review began, and independently re-confirmed unmodified by this Unit's own three edits. **This Unit's own hunks are exactly three, isolated locations**: one new import line (`InMemoryAuthorizationPurposeRegistry`), one new 12-line comment + `val authorizationPurposeRegistry = InMemoryAuthorizationPurposeRegistry()` (inserted immediately after `actionMapper`, before `toolRegistry`), and one new named argument (`authorizationPurposeRegistry = authorizationPurposeRegistry`) on the pre-existing `DefaultPermissionPolicy(...)` call. Independently confirmed zero diff on `src/runtime/AuthorizationPurposeRegistry.kt`, `src/contracts/AuthorizationPurposeVocabulary.kt`, `src/interfaces/PermissionEngine.kt`, `src/runtime/DefaultPermissionEngine.kt`, `src/runtime/DefaultPermissionPolicy.kt`, and `src/composition/ParkerRuntimeConfig.kt`.

---

## 2. Challenge — Accidental Registration of Production Purpose Values

Independently re-grepped the full, current `ParkerRuntime.kt` for `authorizationPurposeRegistry.` — **zero matches beyond the declaration itself.** No `.register(` call exists anywhere in production code. Independently re-read the Completion Review's own "empty registry" claim against this grep, not merely accepted it. **Confirmed: zero production values registered.**

---

## 3. Challenge — More Than One Registry Instance

Independently re-grepped `InMemoryAuthorizationPurposeRegistry(` in `ParkerRuntime.kt` — **exactly one match** (the new declaration). No second construction anywhere in the file or in any other production file (`grep -rln "InMemoryAuthorizationPurposeRegistry(" src/` returns only `src/runtime/AuthorizationPurposeRegistry.kt`, the class's own definition, and `ParkerRuntime.kt`). **Confirmed: exactly one instance.**

---

## 4. Challenge — Second Permission Authority

Independently re-grepped `DefaultPermissionPolicy(` and `DefaultPermissionEngine(` in `ParkerRuntime.kt` — **exactly one match each**, at the same two lines this Unit's own diff shows (the policy construction gained a fourth named argument; the engine construction is byte-identical to before). No second `PermissionEngine` implementation, no second policy class, anywhere in the diff. **Confirmed: single Permission Engine, single Permission Policy, unchanged from before this Unit.**

---

## 5. Challenge — Hidden Consumer Adoption

Independently re-ran the new test suite's own "no existing consumer class declares a field referencing the Authorization Purpose registry" test by hand-tracing its logic: it reflects `DefaultKnowledgeCandidateEvaluator::class.java.declaredFields` and `EvidenceIntelligenceInputResolver::class.java.declaredFields` directly against the live class files, not a cached or assumed list — a genuine structural proof, not a name-matching guess. Independently confirmed via `git diff --stat` that neither `DefaultKnowledgeCandidateEvaluator.kt` nor `EvidenceIntelligenceInputResolver.kt` appears in the diff at all (zero touch). **Confirmed: no hidden consumer adoption.**

---

## 6. Challenge — Hidden Gap #54 Work

Independently re-confirmed (Section 1) zero diff to any Memory Retrieval, Knowledge Submission, or Evidence Intelligence file. Independently grepped this Unit's own two changed/new files (`ParkerRuntime.kt`'s own three hunks; `ParkerRuntimeAuthorizationPurposeCompositionTest.kt`) for `memory.retrieve`, `PermissionFilteredMemoryRetrieval`, `EvidenceIntelligence`, `KnowledgeSubmission` — none found beyond the pre-existing, unrelated Conversational Memory Admission diff already excluded in Section 1. **Confirmed: no Gap #54 implementation.**

---

## 7. Challenge — Change in Pre-Existing Permission Outcomes

Independently re-read the new test "an existing, already-registered production action still resolves APPROVED after Authorization Purpose composition": it exercises `DefaultEvidenceCustodian.EVIDENCE_INTAKE_RESOURCE_ID`/`ACCEPT_ACTION_NAME` — a real, already-registered production `(WRITE, DOCUMENT)` pair — through the real, reflectively-obtained, fully composed `DefaultPermissionPolicy`, and asserts `APPROVED`/`AUTOMATIC`, matching the pre-existing rule's own already-fixed content (unchanged by this Unit, confirmed Section 1). Independently re-ran the full suite (`./gradlew clean test`): **1977 tests, 0 failures, 0 errors, 5 pre-existing skips** — every test written before this Unit, across every domain, passed unmodified. **Confirmed: no change to any pre-existing permission outcome.**

---

## 8. Challenge — Constructor-Default Masking

Independently re-derived the Planning Review's own finding (§1.5): before this Unit, `DefaultPermissionPolicy`'s own `authorizationPurposeRegistry: AuthorizationPurposeRegistry? = null` default caused the one production call site to silently omit the dependency — the exact "constructor default masking a missing composition dependency" this challenge names. Independently confirmed the fix: the production call site (line 519 region) now passes `authorizationPurposeRegistry = authorizationPurposeRegistry` explicitly, so the default is no longer in effect for the composed runtime; the default itself remains on `DefaultPermissionPolicy`'s own signature (unmodified, Section 1), preserving the three existing test files' own 3-arg construction calls, consistent with this task's own "do not remove backward-compatible defaults unless governance requires it." **Confirmed: the masking condition Unit 4 left behind is now closed for production, without an unauthorised breaking change to the class's own public signature.**

---

## 9. Challenge — Ambient Authority

Independently re-read all three of this Unit's own diff hunks for any `if (caller == ...)`, `if (principalId == ...)`, or implicit/inferred-signal shape — none exists; the registry is constructed unconditionally, once, identically regardless of any request or caller. **Confirmed: no ambient authority introduced.**

---

## 10. Challenge — Startup Failure Behaviour

Independently re-read `InMemoryAuthorizationPurposeRegistry`'s own constructor (`src/runtime/AuthorizationPurposeRegistry.kt`, unmodified) — a no-argument constructor initialising a `Mutex` and an empty `mutableMapOf`, identical in shape to `InMemoryResourceRegistry()`/`InMemoryActionVocabulary()`, neither of which is wrapped in any `stage(...)`/try-catch construct in `buildAndRegisterRuntimeGraph` either (only fallible operations — resource registration, action vocabulary registration — are wrapped in `stage(...)` for named-failure attribution). The registry's own construction, like its two siblings, cannot throw. Independently re-ran the new "runtime constructs and starts successfully... " test plus the full suite's own pre-existing `ParkerRuntimeStartupAndShutdownTest.kt`/`ParkerRuntimeFailureHandlingTest.kt` (both included in the 1977-test, 0-failure full run) — neither shows any new failure mode. **Confirmed: no new startup failure path introduced, and no existing one altered.**

---

## 11. Challenge — Accidental Persistence/Lifecycle Invention

Independently re-read `shutdown()` (lines 1330–1353 in the pre-Unit-5 file; independently re-confirmed zero diff there in Section 1) — no reference to the new registry, no new cleanup step added. Independently re-confirmed `InMemoryAuthorizationPurposeRegistry` itself carries no file handle, no database connection, no durability log — the identical, unmodified Unit 3 class. **Confirmed: no persistence or lifecycle mechanism was introduced, consistent with this task's own explicit prohibition.**

---

## 12. Challenge — Whether Unit 6 Verification Work Leaked Into Unit 5

The most substantive challenge, examined at length rather than dismissed. Unit 6's own defining scope (Implementation Plan §8) is the **full fail-closed/precedence-safety matrix** using a synthetic value: absent denies, unregistered denies, retired denies, and a **registered-and-eligible value participating in precedence-safe resolution against a purpose-aware rule**. Independently re-read this Unit's own new test file end to end against that four-part matrix:

- No test registers a purpose and then asserts a **denial** for an absent/unregistered/retired case through the composed runtime (that matrix already exists, and is already proven, in Unit 4's own `DefaultPermissionPolicyTest.kt`, against a directly-constructed `DefaultPermissionPolicy` — not duplicated here).
- No test adds a purpose-aware `PermissionPolicyRule` to the composed policy's own rule list, and none exercises precedence between a coarse and a purpose-aware rule through the real composed graph — the one Unit-6-defining proof ("a registered-and-eligible value participates correctly in the precedence-safe resolution Unit 4 built") is **not present here**.
- The one test in this Unit's own suite that registers a synthetic purpose (`"test.unit-5-composition-verification-only"`) and evaluates a request declaring it proves a **narrower, different claim**: that composing a live, consulted registry does not, by itself, change any existing outcome when no purpose-aware rule exists to match it — directly serving this Unit's own constitutional requirement ("no behavioural widening simply because the infrastructure is now composed") and its own required test ("existing permission behaviour remains unchanged"), not Unit 6's mandate to prove the mechanism's own correctness end-to-end. The registration itself occurs entirely within test-tier code, on a registry instance scoped to that one test's own `ParkerRuntime` instance, never added to `ParkerRuntime.kt`'s own production registration set — the identical boundary Unit 6's own Outputs section (§8) itself describes as the lawful place for a synthetic value to live, applied here to a materially narrower claim than Unit 6 itself is chartered to prove.

**Confirmed, on close examination: Unit 6's own distinguishing verification matrix (the four-part fail-closed/precedence-safety proof) was not attempted or duplicated by this Unit. The one synthetic-value test present serves a different, narrower, Unit-5-scoped regression claim.**

---

## 13. Findings

No required correction was found. Zero production registration, exactly one registry instance, exactly one Permission Engine/Policy, no hidden consumer adoption, no Gap #54 work, no change to any pre-existing permission outcome, the constructor-default-masking condition closed without an unauthorised breaking change, no ambient authority, no new startup failure path, no persistence/lifecycle invention, and no leakage of Unit 6's own distinguishing verification matrix into this Unit were each independently re-derived from the current diff, the current code, and direct re-execution of the full test suite — not re-accepted from the Completion Review's own account.

---

## Constitutional Verdict

```
ACCEPTED
```

No required correction. No Defect Confirmation Review is necessary.
