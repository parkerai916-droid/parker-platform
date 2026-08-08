**Status:** Genuine Independent Constitutional Review of Authorization Purpose Unit 6, performed against the frozen governance, Scope Lock, Implementation Plan, Unit 6 Planning Review, relevant constitutional principles, accepted Units 1–5, current production composition, and actual Unit 6 tests. The Unit 6 Completion Review was treated as evidence to challenge, not as authority. This document does not amend governance, production code, or tests. It does not perform or authorise any later implementation unit. Nothing is staged, committed, or pushed.

# Authorization Purpose — Unit 6 (End-to-End Verification) — Independent Constitutional Review

## 1. Independent Baseline and Diff Attribution

Independently re-verified the reviewed checkpoint as branch `codex-handover-2026-08-08`, `HEAD` `80e561a79be7d03ff02ea4662eb3384d837921ae`, one checkpoint commit beyond the Unit 5 baseline `aa4ee9c` (`origin/main`). The checkpoint as a whole contains Conversational Memory Admission production work as well as Unit 6 verification work; checkpoint membership alone therefore cannot establish Unit 6 attribution.

The Unit 6-attributable implementation is isolated by the Planning Review, file content, and diff shape:

- new `tests/runtime/AuthorizationPurposeEndToEndVerificationTest.kt`;
- additions to `tests/composition/ParkerRuntimeAuthorizationPurposeCompositionTest.kt` under its expressly marked Unit 6 section; and
- Unit 6 review documents.

No Authorization Purpose production contract, registry, policy, engine, or composition file changes between `aa4ee9c` and `80e561a`. The checkpoint's other production changes belong to the separately documented Conversational Memory Admission unit. Unit 6 did not modify them merely by testing the already-present `MemoryAdmissionCoordinator` as an existing caller.

**Classification: OBSERVATION ONLY.** The mixed checkpoint requires careful attribution, but the Unit 6 boundary is recoverable and no production change is attributable to Unit 6.

---

## 2. Challenge — Did Unit 6 Remain Verification-Only?

Implementation Plan §8 authorises end-to-end verification with synthetic test-tier-only purposes and expects test files only. Independently inspected both Unit 6 test deltas. Every registry mutation, purpose-aware rule, resource, action mapping, principal, and request created for the behavioral matrix exists inside test code. No production accessor or test hook is added. `ParkerRuntime.kt` receives no Unit 6 test content and no Unit 6 modification.

The composition test uses reflection solely to inspect and invoke the already-composed graph. Reflection changes no production object definition and creates no runtime entry point. Its synthetic registration is confined to the registry owned by one test-local `ParkerRuntime` instance and disappears with that instance.

**Finding: within authorised scope. Classification: OBSERVATION ONLY.**

---

## 3. Challenge — Did Unit 6 Change Production Authorization Semantics?

Independently compared the Authorization Purpose production path across the Unit 5 baseline and checkpoint:

- `ExecutionRequest.authorizationPurpose` is unchanged;
- `InMemoryAuthorizationPurposeRegistry` is unchanged;
- `DefaultPermissionPolicy` and its purpose-resolution/precedence logic are unchanged;
- `PermissionEngine` and `DefaultPermissionEngine` are unchanged;
- the `ParkerRuntime` registry/policy/engine composition is unchanged.

Tests instantiate existing classes with controlled data but do not alter their implementations or live configuration. No production rule, action mapping, resource mapping, default, branch, or decision result is changed.

**Finding: no production authorization semantic change. Classification: OBSERVATION ONLY.**

---

## 4. Challenge — Did a Synthetic Test Purpose Become Real Authority?

Independently grepped production code for Authorization Purpose construction, registration, and request adoption. `ParkerRuntime` constructs one empty `InMemoryAuthorizationPurposeRegistry` and supplies it to `DefaultPermissionPolicy`; it contains no `authorizationPurposeRegistry.register(...)` call. No production `PermissionPolicyRule` names an Authorization Purpose. No production `ExecutionRequest` constructor assigns `authorizationPurpose`.

Unit 6 identifiers are visibly `test.`-prefixed and constructed only in test methods. Purpose-aware rules exist only in test-local lists supplied to test-local real policy instances. The composition tests that mutate the composed registry do so only after constructing a test-owned `ParkerRuntime`, never by changing its startup sequence or static production content.

The test mechanism therefore creates executable test authority for the duration of a test—as it must to prove policy behavior—but no production authority, vocabulary entry, policy rule, persistence, or architectural registration precedent.

**Finding: no real Authorization Purpose introduced or enabled. Classification: OBSERVATION ONLY.**

---

## 5. Challenge — Was Any Existing Caller Silently Retrofitted?

Independently inspected the production grep and the structural tests. No production request construction assigns the new field. The existing Unit 5 checks cover `DefaultKnowledgeCandidateEvaluator` and `EvidenceIntelligenceInputResolver`; Unit 6 adds the already-present `MemoryAdmissionCoordinator` to the structural non-adoption check. None declares an Authorization Purpose field or registry reference.

The test's field-name check is not, alone, a universal proof that no method-local purpose could ever exist. The independent production grep closes that possible weakness: there is no production `authorizationPurpose =` assignment and no production registration call. Existing callers therefore remain on their pre-existing request shapes.

**Finding: no silent retrofit. Classification: OBSERVATION ONLY.**

---

## 6. Challenge — Was an Alternate Authorization Path Introduced?

`AuthorizationPurposeEndToEndVerificationTest` directly constructs the real `DefaultPermissionPolicy` and real `DefaultPermissionEngine`, following the already-accepted `DefaultPermissionEngineTest` pattern. Test-supplied `InMemoryIdentityService`, `InMemoryResourceRegistry`, `InMemoryActionVocabulary`, registry, and rules are dependencies/data for the real implementation, not substitute authorization implementations. The test calls the public `DefaultPermissionEngine.evaluate(ExecutionRequest): PermissionDecision` path; it does not implement an evaluator, policy, engine, wrapper request, or alternate decision type.

The distinction between a second object instance in isolated test scaffolding and a second authorization *architecture* is constitutionally material. Chapter 10 and Scope Lock §2.4 prohibit competing production authority, not construction of real classes in unit tests. Treating ordinary isolated test instances as an alternate constitutional authority would invalidate the repository's already-accepted testing pattern and make the frozen Unit 6 verification impossible.

**Finding: no alternate authorization path. Classification: OBSERVATION ONLY.**

---

## 7. Challenge — Is Fail-Closed Behavior Actually Preserved?

Independently traced `DefaultPermissionPolicy.evaluate` and `ruleOutcomeFor`:

1. `request.authorizationPurpose` becomes an effective purpose only when the wired registry reports it active.
2. A purpose-aware rule can be selected only for that effective purpose.
3. Otherwise evaluation considers an independently applicable coarse rule.
4. Where no rule matches, the pre-existing default returns `DENIED`.

The phrase “absent or unregistered ... denies” in Scope Lock §2.4 and Implementation Plan §8 must be read together with the accepted additive/optional carrier and no-regression requirements. Accepted Unit 4 already resolved this apparent tension: absence, non-registration, or retirement prevents the purpose from grounding *purpose-specific authority*; it does not invalidate authority independently granted by a pre-existing coarse rule. If no coarse rule applies, the unchanged default denies. Unit 6 proves both sides: absent/unregistered/retired purpose cannot select the purpose-aware rule in the real engine matrix, while the composed `memory.retrieve` shape—having no governing production rule—remains denied.

This is not a silent reinterpretation invented by Unit 6. It is the behavior frozen in Unit 4's accepted implementation and constitutional review, which Unit 6 was required to verify without regression.

**Finding: fail-closed behavior remains constitutionally intact. Classification: OBSERVATION ONLY.**

---

## 8. Challenge — Unregistered and Retired Purpose Semantics

The unregistered test supplies a purpose-aware DENY rule alongside a coarse APPROVE rule. Because the purpose is not active, the engine returns the coarse result; it never treats the unregistered identifier as authority. The retired test repeats that challenge after a real register/retire sequence and additionally confirms `lookup` retains a `RETIRED` entry.

The conflict-history test independently proves a retired identifier cannot be silently reactivated by re-registration and still cannot govern the purpose-aware rule on the same registry instance wired into a real engine. These outcomes preserve immutable meaning, reject-on-conflict, retirement without deletion, historical visibility, and ineligibility for new authority.

**Finding: constitutionally correct. Classification: OBSERVATION ONLY.**

---

## 9. Challenge — Does Precedence Preserve the Frozen Authority Model?

The frozen outcome constraint is that a coarse `(action, resourceType)` rule may not govern when a more specific active-purpose rule is meant to govern. Unit 6 tests both restrictiveness directions:

- coarse APPROVE + purpose-specific DENY → DENY;
- coarse DENY + purpose-specific APPROVE → APPROVE.

It also reverses list order and obtains the same purpose-specific result. This rules out three constitutionally incorrect shortcuts: always choosing the most restrictive rule, always choosing the coarse rule, and choosing the first list entry. Both approval and denial are necessary evidence: denial alone would not prove that purpose specificity, rather than generic restrictiveness, controls.

**Finding: precedence preserves the frozen authority model. Classification: OBSERVATION ONLY.**

---

## 10. Challenge — Is Production Composition Still a Single Authority?

Independently grepped `ParkerRuntime.kt`: one `InMemoryAuthorizationPurposeRegistry()` construction, one `DefaultPermissionPolicy(...)` construction, and one `DefaultPermissionEngine(...)` construction. The engine receives the single policy, and the policy receives the single registry. The Unit 6 composition test additionally asserts the private `permissionEngine` field is a real `DefaultPermissionEngine`; Unit 5's accepted tests confirm the policy's registry field is one stable reference and its production entries remain empty.

No Unit 6 code introduces a second production engine, policy, registry, composition branch, service locator, or caller-specific evaluator.

**Finding: single authoritative engine/policy/registry path preserved. Classification: OBSERVATION ONLY.**

---

## 11. Challenge — Does the `memory.retrieve` Test Conceal or Resolve Gap #54?

Scope Lock §2.5 keeps Gap #54 separate and blocks `memory.retrieve` policy approval until later governance. Implementation Plan §8 correspondingly forbids Unit 6 from deciding real `memory.retrieve` policy content. The Planning Review nevertheless requires a narrow non-widening proof: Authorization Purpose infrastructure alone must not accidentally authorize the already-denied request shape.

The test constructs the real live shape—`PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME` and empty target resources—and evaluates it through the real composed engine. It performs this once without a purpose and once with a test-registered active purpose. Both results are `DENIED`. It adds no action-vocabulary entry, resource representation, policy rule, consumer purpose, or production call-site change.

The test does not invoke `PermissionFilteredMemoryRetrieval` itself. That limitation is explicitly disclosed in the Planning Review and in the test comment; the values copied are fixed constants/shape, and the constitutional claim is deliberately narrower than end-to-end retrieval: only that the composed authorization mechanism has not widened the known-denied shape. It neither disguises successful retrieval nor claims Gap #54 is solved.

**Finding: Gap #54 remains honestly unresolved and fail-closed. Classification: PRE-EXISTING ARCHITECTURAL BLOCKER.**

---

## 12. Challenge — Do the Tests Prove Real Behavior or Merely Mocks?

The nine matrix tests use real `AuthorizationPurposeId`, `InMemoryAuthorizationPurposeRegistry`, `ExecutionRequest`, `ActionMapper`, `DefaultPermissionPolicy`, `DefaultPermissionEngine`, `InMemoryIdentityService`, resource registry, action vocabulary, and public `PermissionDecision`. Test data is synthetic, as explicitly required; the authorization classes under examination are not mocks or fakes.

The direct harness is necessary because the production policy deliberately has no purpose-aware rule. Adding one to production merely to test precedence would itself violate Unit 6. Composition-specific claims are not inferred from the harness: separate `ParkerRuntime` tests inspect the real graph, evaluate an existing real action through its real engine, verify consumer non-adoption, and verify `memory.retrieve` denial.

The two vehicles therefore divide claims lawfully: real implementation behavior with controlled rules in the harness, and real object-graph/non-widening behavior in composition. Neither substitutes for the other.

**Finding: required behavior is genuinely proven through real implementations. Classification: OBSERVATION ONLY.**

---

## 13. Challenge — Did Unit 6 Commit Future Architecture?

No production API, identifier convention beyond existing `test.` practice, registration lifecycle, persistence mechanism, consumer purpose, permission rule, retrieval mapping, or composition entry point is added. The tests exercise the already-frozen registry and precedence contracts. Reflection remains test-local and does not make internal fields public.

The `memory.retrieve` test commits only to the present, already-governed denial and to the constitutional proposition that infrastructure must not silently widen authority. It does not prescribe how Gap #54 is eventually resolved. Existing-caller checks likewise preserve future retrofit freedom rather than constraining its eventual governed design.

**Finding: no architectural commitment belonging to a future unit. Classification: OBSERVATION ONLY.**

---

## 14. Challenge — Hidden Omissions, Ambiguities, or Dependencies

The principal ambiguity is the fail-closed wording discussed in §7. Read in isolation, “absent value denies” could be mistaken for a requirement to reject every request lacking a purpose even where a valid coarse rule already authorizes it. That reading contradicts the optional/additive carrier, existing-caller non-regression, and accepted Unit 4 determination. Unit 6 follows the harmonized, already-accepted reading and also proves denial where no rule exists. No constitutional omission results.

The behavioral harness does not use the exact `ParkerRuntime` registry/policy/engine object instances for its purpose-aware precedence matrix. This is disclosed, authorised by Implementation Plan §8's “real ... pairing from a test harness” language, and necessary because the production rule list lawfully contains no purpose-aware rule. Composition-specific properties are independently covered against the real graph. No hidden dependency or acceptance gap results.

The targeted test selection passed. The full Windows run's sole failure is assessed separately below. No missing Unit 6 test, concealed alternate path, or unexplained production dependency prevents acceptance.

**Classification: OBSERVATION ONLY.**

---

## 15. Independent Assessment of the Windows OCR Failure

The observed failure is in `OcrStructuralIsolationTest`, outside Authorization Purpose code and tests. The test discovers exactly one `OcrMechanism` implementation and returns the correct file, but compares Windows `File.path` output (`src\runtime\OcrExecutionSequencer.kt`) with a hard-coded Unix-form expected string (`src/runtime/OcrExecutionSequencer.kt`). The failed equality therefore says nothing about OCR structural multiplicity, Authorization Purpose behavior, Permission Engine composition, or Unit 6 scope compliance.

Unit 6 targeted tests pass. No causal, code-path, architectural, or governance relationship connects Unit 6 to this separator comparison. It has no constitutional or Unit 6 acceptance significance and is not corrected here.

**Classification: UNRELATED ENVIRONMENTAL/PORTABILITY ISSUE.**

---

## 16. Independent Assessment of the Retrieval/Submission Blocker

Direct production inspection confirms `memory.retrieve`/`memory.retrieve_document` remain deliberately unregistered in `ParkerRuntime`, and the real Memory Core retrieval request shape lacks Resource Registry targets. The broader `PermissionFilteredMemoryRetrieval` / `KnowledgeSubmission` inability to promote in the composed runtime therefore predates Unit 6.

Specifically:

- **Unit 6 caused it:** no;
- **Unit 6 worsened it:** no production semantics changed;
- **Unit 6 concealed it:** no—the Planning Review and tests name Gap #54 and assert continued denial;
- **Unit 6 improperly bypassed it:** no—the test does not claim successful retrieval or promotion and introduces no alternate retrieval path;
- **Unit 6 correctly left it unresolved and fail-closed:** yes.

The blocker prevents later real retrieval/promotion outcomes, but frozen Unit 6 scope expressly excludes solving or adopting those outcomes. Requiring its solution here would itself violate programme sequencing.

**Classification: PRE-EXISTING ARCHITECTURAL BLOCKER.**

---

## 17. Findings and Required Classifications

| Matter | Classification | Acceptance effect |
|---|---|---|
| Unit 6 test-only diff and mixed-checkpoint attribution | OBSERVATION ONLY | None; attribution is independently recoverable. |
| Real engine/policy/registry harness uses synthetic test data | OBSERVATION ONLY | Lawful and required; no production authority. |
| Fail-closed wording requires harmonization with additive/no-regression governance | OBSERVATION ONLY | Already settled by accepted Unit 4; Unit 6 proves both non-participation and no-rule denial. |
| Harness and composed-runtime tests use two complementary vehicles | OBSERVATION ONLY | Claims are correctly divided; no alternate authority. |
| `PermissionFilteredMemoryRetrieval` / `KnowledgeSubmission` gap | PRE-EXISTING ARCHITECTURAL BLOCKER | Outside Unit 6; correctly remains denied and unresolved. |
| Windows OCR path-separator failure | UNRELATED ENVIRONMENTAL/PORTABILITY ISSUE | No constitutional or Unit 6 acceptance significance. |

No **UNIT 6 CONSTITUTIONAL DEFECT** was found.

No **UNIT 6 NON-CONSTITUTIONAL DEFECT** was found.

---

## Constitutional Verdict

```
ACCEPTED
```

No corrective action is required for Authorization Purpose Unit 6. No Defect Confirmation Review is necessary.

The pre-existing retrieval/submission blocker remains outside Unit 6 and must not be treated as a qualification of this verdict. The unrelated Windows OCR portability issue likewise does not qualify constitutional acceptance.

---

## Recommended Next Step

Stop after this Independent Constitutional Review and await explicit approval. Do not commit or push, begin another programme unit, address Gap #54 or the retrieval/submission blocker, implement conversational retrieval, or modify the unrelated OCR test as part of Unit 6 acceptance.
