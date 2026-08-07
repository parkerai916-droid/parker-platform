**Status:** Planning Review only. No Kotlin is implemented, proposed as a diff, or changed by this document. Repository baseline independently re-verified below, not assumed from the task's own claim. This document does not amend any governance document.

# Authorization Purpose — Unit 5 (Composition Wiring) — Planning Review

## 0. Baseline, Independently Re-Verified

`git log --oneline -10`: `HEAD` is `a0e619d "feat: implement Authorization Purpose unit 4"`, tracking `origin/main`. `git status --short` shows only pre-existing, unrelated uncommitted work (Conversational Memory Admission, a Programme 3 clarification), unchanged by this Unit. The task's own claim "Units 1–4 are accepted, verified, committed, and pushed" is **confirmed true, independently**.

---

## 1. Current Composition Path, Inspected Fresh

### 1.1 Where `DefaultPermissionPolicy` is constructed

`src/composition/ParkerRuntime.kt`, inside `buildAndRegisterRuntimeGraph()` (a private method called once from `start()`), line 507: `val permissionPolicy = DefaultPermissionPolicy(actionMapper = actionMapper, resourceRegistry = resourceRegistry, rules = listOf(...8 rules...))`. **This is the only production construction site in the repository** — confirmed by `grep -rln "DefaultPermissionPolicy("` returning only `ParkerRuntime.kt` (production) and three test files. `permissionPolicy` is a **construction-local `val`**, never promoted to a `ParkerRuntime` field, and never referenced again outside the four lines immediately following its own construction.

### 1.2 Where `PermissionEngine` is constructed

Line 602, immediately after: `permissionEngine = DefaultPermissionEngine(identityService, permissionPolicy)`, assigned to the pre-existing field `private lateinit var permissionEngine: PermissionEngine` (promoted to a field for a documented, unrelated reason — Evidence Intelligence Unit 8's own `analyseEvidence` entry point needs direct access; see that field's own KDoc, lines 224–231). This is the single, already-existing `PermissionEngine` instance every downstream coordinator (`DefaultExecutionPipeline`, `EvidenceRegistrationCoordinator`, `DefaultKnowledgeRetrieval`, `MemoryAdmissionCoordinator`, etc.) receives — confirmed by `grep -n "permissionEngine" ParkerRuntime.kt` showing every one of these constructions passing the same field reference, never a second construction of `DefaultPermissionEngine` anywhere in the file.

### 1.3 Where `AuthorizationPurposeRegistry` should be constructed

Immediately before `permissionPolicy`'s own construction (line 507), alongside `resourceRegistry`/`vocabulary`/`actionMapper` (lines 351–353) — the exact composition stage Scope Lock §2.3 and the Implementation Plan's own Unit 5 section (§7) both name: *"the registry is constructed once, at the same composition stage as `ActionVocabulary`/`ResourceRegistry`."* As a **construction-local `val`**, mirroring `resourceRegistry`/`vocabulary`/`actionMapper`'s own precedent exactly — none of those three is a `ParkerRuntime` field either, since nothing outside `buildAndRegisterRuntimeGraph` currently needs to reach them directly. No new field is warranted: the KDoc precedent for promoting `permissionEngine` to a field (lines 224–231) states the reason explicitly ("the first production entry point that must evaluate a permission decision directly") — no analogous reason exists for the registry in this Unit, since Unit 5 explicitly adopts no consumer.

### 1.4 Whether there is exactly one shared registry instance

Trivially yes by construction: exactly one `InMemoryAuthorizationPurposeRegistry()` call site will exist (the new local `val`), passed to the one `DefaultPermissionPolicy` construction. No second consumer exists yet in this Unit's own scope (Unit 5 explicitly does not adopt `DefaultKnowledgeCandidateEvaluator`/`EvidenceIntelligenceInputResolver` or any other consumer — Implementation Plan §7 Non-responsibilities), so "shared across multiple consumers" is not yet a live question; "shared" here means only "the one instance `DefaultPermissionPolicy` receives is the one this method constructs, not a second, independently-constructed one" — verifiable by reflection (Section 4, below) even with a single consumer.

### 1.5 Whether any existing constructor default would mask a missing composition dependency

**Yes — confirmed, and this is precisely the condition Unit 5 exists to close.** `DefaultPermissionPolicy`'s own `authorizationPurposeRegistry: AuthorizationPurposeRegistry? = null` (added by Unit 4) currently defaults to `null` at the one production call site, meaning the composed runtime today has **no** Authorization Purpose registry participating in evaluation at all — the infrastructure Units 1–4 built is inert in the real, running system. Per this task's own instruction ("decide from the accepted Implementation Plan whether runtime composition should now supply it explicitly"): the Implementation Plan's own Unit 5 Outputs (§7) state the registry must be "supplied to wherever Unit 4's own extended policy needs it" — an instruction to supply it, not to leave the default in force. **Decision: `ParkerRuntime.kt`'s own construction call now passes `authorizationPurposeRegistry` explicitly.** The defaulted parameter itself is **not** removed from `DefaultPermissionPolicy`'s own constructor (no governance requires this, and the three existing test files construct it via the 3-arg form; removing the default would be an unauthorised, unnecessary breaking change) — only the one production call site changes.

### 1.6 Every test fixture that constructs the affected runtime/configuration object

`grep -rln "ParkerRuntime("` across `tests/` returns nine files: `ParkerRuntimeEvidenceCustodianIntegrationTest.kt`, `ParkerRuntimeStartupAndShutdownTest.kt`, `ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`, `ParkerRuntimeMemoryCoreDurabilityCompositionTest.kt`, `ParkerRuntimeConversationPipelineTest.kt`, `ParkerRuntimeReasoningContextIntegrationTest.kt`, `ParkerRuntimeFailureHandlingTest.kt`, `ParkerRuntimeConversationalMemoryAdmissionCompositionTest.kt`, `ParkerRuntimeEvidenceIntelligenceCompositionTest.kt`. **None require modification**: `ParkerRuntime`'s own public constructor (`config`, `logger`, `ownerNotificationSink`, `clock`) is unchanged by this Unit — the new registry is wired entirely inside the private `buildAndRegisterRuntimeGraph` method, invisible to every caller of the public constructor. Independently confirmed by inspection: none of the nine files references `DefaultPermissionPolicy`, `PermissionPolicyRule`, or `AuthorizationPurposeRegistry` directly.

### 1.7 Whether runtime startup/shutdown semantics require any changes

**No.** `shutdown()` (lines 1330–1353) stops only `runtimeEventLogger` — no other in-memory registry (`resourceRegistry`, `vocabulary`, `InMemoryToolRegistry`, etc.) has, or needs, any shutdown/cleanup hook; each is pure in-memory state, reclaimed when `ParkerRuntime` itself is garbage-collected. `InMemoryAuthorizationPurposeRegistry` requires no different treatment — confirmed by direct comparison against `InMemoryResourceRegistry`/`InMemoryActionVocabulary`, neither of which appears anywhere in `shutdown()` either.

### 1.8 Whether any persistence or lifecycle mechanism is needed for the registry

**No**, and this task's own non-responsibilities explicitly forbid introducing one ("introduce persistence for the Authorization Purpose registry"). Confirmed consistent with Unit 3's own Scope Lock authority (Deferred Decision 6: "whether the Authorization Purpose vocabulary ever becomes a formal, ADR-019-governed schema artifact... left open," not decided or required here) and with every sibling in-memory registry already composed with no persistence of its own.

---

## 2. Exact Composition Architecture

```
InMemoryAuthorizationPurposeRegistry()        (new local val, constructed alongside
        |                                       resourceRegistry/vocabulary/actionMapper)
        v
DefaultPermissionPolicy(
    actionMapper, resourceRegistry, rules,
    authorizationPurposeRegistry = <above>    (explicit, was previously omitted/defaulted null)
)
        v
DefaultPermissionEngine(identityService, permissionPolicy)   (unchanged construction)
        v
permissionEngine  (existing field; unchanged downstream — every consumer already
                    receives this same instance, and needs no change of its own)
```

No new field, no new `ParkerRuntimeConfig` option, no new public method, no interface change anywhere in this graph.

---

## 3. Exact Scope for This Pass

**Modify:** `src/composition/ParkerRuntime.kt` only — confirmed as complete (not merely the Implementation Plan's own illustrative guess) by the fresh inspection above: no second file constructs `DefaultPermissionPolicy` in production, and no other file needs a compensating change since the public API surface these tests rely on is untouched.

**Do not modify:** `DefaultPermissionPolicy.kt`, `PermissionEngine.kt`, `DefaultPermissionEngine.kt`, `AuthorizationPurposeRegistry.kt`, `AuthorizationPurposeVocabulary.kt`, `Identifiers.kt`, `ExecutionRequest.kt`, `DefaultKnowledgeCandidateEvaluator.kt`, `EvidenceIntelligenceInputResolver.kt`, any Gap #54/Memory Retrieval/Knowledge Submission/Evidence Intelligence/Conversational Memory Admission file, `ParkerRuntimeConfig.kt`.

**New tests only:** a new, dedicated composition test file, mirroring `ParkerRuntimeEvidenceIntelligenceCompositionTest.kt`'s own established structure and its `privateField` reflection helper (Section 4).

---

## 4. Test Strategy — Reflection, Precedented Not Invented

`ParkerRuntimeEvidenceIntelligenceCompositionTest.kt` already documents and uses exactly this technique: *"Reflection is used only where no public seam exists to observe shared instance identity across the composed graph — `ParkerRuntime` exposes none of its internal composition by design."* The identical situation applies here: `permissionPolicy`/`authorizationPurposeRegistry` are construction-local, by design (Section 1.3), so no public accessor exists or will be added (task's own explicit "do not invent production accessors solely for tests"). The existing `privateField<T>(name)` extension (declared per-file today, not shared via a common test-utility file) will be reused verbatim in the new test file. Reachability path: `runtime.privateField<PermissionEngine>("permissionEngine")` (already a field) → `.privateField<DefaultPermissionPolicy>("policy")` (`DefaultPermissionEngine`'s own constructor parameter name, confirmed by fresh read of `DefaultPermissionEngine.kt`) → `.privateField<AuthorizationPurposeRegistry?>("authorizationPurposeRegistry")` / `.privateField<List<PermissionPolicyRule>>("rules")`.

`InMemoryAuthorizationPurposeRegistry`'s own private `entries` map (Unit 3) is also reflectively reachable, used only to prove emptiness (zero production values registered) — Unit 3 deliberately built no enumeration method, so this is the only way to observe the whole-registry state without inventing one.

---

## 5. Boundary Review

**Not genuinely required.** Every question this task's own Planning Review instructions posed (construction sites, shared-instance question, constructor-default masking, test fixture impact, startup/shutdown semantics, persistence) resolved to a single, unambiguous answer directly from fresh inspection of the current code and the already-frozen Implementation Plan §7 — none required a choice between two lawful designs needing external adjudication.

---

## 6. Non-Responsibilities Confirmed Unchanged

No Authorization Purpose value registered for any real domain. No consumer adoption (`DefaultKnowledgeCandidateEvaluator`, `EvidenceIntelligenceInputResolver`, or any other). No new `PermissionPolicyRule`. No Gap #54, Memory Retrieval, Knowledge Submission, Evidence Intelligence, or Conversational Memory Admission change. No `PermissionEngine` interface change. No second `PermissionEngine`/`DefaultPermissionPolicy`. No persistence. Unit 6 not begun.

```
UNIT 5 PLANNING REVIEW COMPLETE — PROCEEDING TO IMPLEMENTATION
```
