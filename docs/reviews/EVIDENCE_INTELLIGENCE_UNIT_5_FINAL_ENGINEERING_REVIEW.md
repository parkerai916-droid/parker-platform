# Unit 5 — Final Engineering Review

**1. Exact files inspected**
- `src/interfaces/EvidenceIntelligence.kt` (the `EvidenceIntelligence` interface addition)
- `src/runtime/DefaultEvidenceIntelligence.kt`
- `tests/runtime/DefaultEvidenceIntelligenceTest.kt`
- `src/runtime/EvidenceIntelligenceInputResolver.kt` (Unit 2, re-read, unmodified)
- `src/runtime/EvidenceIntelligenceReasoningCoordinator.kt` (Unit 3, re-read, unmodified)
- `src/runtime/AnalyticalOutputDiscipline.kt` (Unit 4, re-read, unmodified)
- `EvidenceAnalysisResult`/`ReasoningProviderResponse` (both re-read in full, unmodified)
- `tests/contracts/EvidenceIntelligenceTest.kt`, `tests/contracts/EvidenceCustodianScopeTest.kt` (the two pre-existing scope-guard tests updated when Unit 5 began)
- Repository-wide grep for `DefaultEvidenceIntelligence`/`EvidenceIntelligence` outside the files above (composition roots, `ParkerRuntime`)

**2. Orchestration findings**

Confirmed exactly: `analyse()` calls `inputResolver.resolve(request)` first (Unit 2) → derives the successfully-resolved reference subset → optionally calls `reasoningCoordinator.reason(request, ReasoningContext(emptyList()))` (Unit 3) → converts the response via `AnalyticalOutputDiscipline.labelContent` (Unit 4) → returns `List<EvidenceAnalysisResult>`. A dedicated ordering test (`analyse resolves inputs before ever invoking a Reasoning Provider`) proves resolution happens strictly before reasoning via an observed call-order list. No fifth step, no acceptance call, no permission evaluation exists anywhere in the method body.

**3. Dependency findings**

`DefaultEvidenceIntelligence`'s only two constructor dependencies are `EvidenceIntelligenceInputResolver` and `EvidenceIntelligenceReasoningCoordinator?` (verified structurally by test and by direct reading). Neither wraps `PermissionEngine`, an acceptance coordinator, `EvidenceCustodian.accept`, `MemoryCore`'s write interface, or Conversation Engine — confirmed by re-reading Units 2 and 3's own unmodified source (Unit 2: `EvidenceCustodian`+`MemoryRetrieval`, both read-only; Unit 3: `ReasoningProvider` only). Repository-wide grep confirms `DefaultEvidenceIntelligence` is referenced nowhere else in `src/` — no composition wiring, no `ParkerRuntime` reference — matching "no runtime composition."

**4. Response-conversion findings**

The conversion is isolated to the single private function `convertReasoningResponse`, called from exactly one call site. `Reply` → exactly one `TransientOutput`, labelled `MODEL_GENERATED` (an honest, kind-independent classification, not an analysis-kind algorithm). `NoAction` → empty list, never fabricated. `Goal` → `UnsupportedOperationException`, never silently coerced into any of the four categories — this is the correct handling of the "no lawful mapping" anomaly the Planning Review identified. The `when` is an exhaustive expression over `ReasoningProviderResponse` (sealed), so a future fourth variant fails to compile here rather than silently falling through — no exhaustive-`when` hazard.

**5. Reference-attachment findings**

Verified by direct trace: `resolvedEvidenceArtifactIds`/`resolvedMemoryCoreReferences` are derived exclusively from Unit 2's own per-input, self-identifying return values (`EvidenceRetrievalResult.Found.evidenceArtifactId`, and the `(RelationshipEndpoint, MemoryCoreRecord?)` pairing filtered to non-null). No id or endpoint not present in the original request, and not itself successfully resolved, can ever appear in a produced `TransientOutput`. No fabricated or inferred reference exists. The guard at line 155–157 (`return emptyList()` when both lists are empty) additionally guarantees `TransientOutput`'s own constructor invariant (≥1 reference) can never be violated by this code path — confirmed by tracing that `convertReasoningResponse` is only reachable once at least one reference is known non-empty.

**6. Partial-completion findings**

Confirmed via the `mixed-resolution` test: a found artifact is cited, a missing one is not, and the successful portion still produces a genuine, non-empty result. The `all-inputs-failed` test confirms an empty list is returned in that case *and* that reasoning is never invoked with nothing to reason about — consistent with treating "everything failed to retrieve" as already fully disclosed via Unit 2's own retrieval dispositions (its own already-authorised mechanism), not a fifth failure category or wrapper this operation would need to invent. No wrapper type, no fifth `EvidenceAnalysisResult` variant, and no silent suppression exist anywhere in this file.

**7. Unit 4 integration findings**

`AnalyticalOutputDiscipline` is used only via its already-authorised `labelContent` function, with the already-authorised `MODEL_GENERATED` constant — no new label, no new discipline mechanism, no stripping of labels before return, and no presentation policy invented. Labels remain embedded in the returned `TransientOutput.text`, consistent with the Planning Review's own conclusion that Unit 4's discipline is meant to survive into the returned output, not be scrubbed by Unit 5.

**8. Unit-boundary findings**

Confirmed: no `PermissionAction`/`ResourceType` pairing, no proposal-class registration, no acceptance-coordinator class, and no `ParkerRuntime`/composition-root reference exists anywhere in the diff or in a repository-wide grep. Units 6, 7, and 8 have not begun.

**9. Test-adequacy findings**

All eleven items the review asks about are demonstrated: orchestration order, empty-request rejection, `Reply`, `NoAction`, `Goal`, mixed retrieval outcomes, partial completion (including the all-failed case and the Memory-Core-reference case), reference attachment, the `ReasoningContext` limitation (proving the nested request field never reaches the provider), dependency boundaries (constructor shape + declared-field check + sole-interface check), and fault propagation. One minor observation, not a defect: the "declares no field of any acceptance-interface or PermissionEngine type" test checks only `DefaultEvidenceIntelligence`'s own direct fields, not full depth through `EvidenceIntelligenceInputResolver`/`EvidenceIntelligenceReasoningCoordinator`'s own fields. This is not a gap in the actual dependency graph — direct reading of both Unit's own unmodified, already-accepted source confirms neither reaches a forbidden type at any depth — Unit 5 is correctly relying on Units 2 and 3's own already-demonstrated guarantees rather than re-proving them, consistent with this Programme's own "verification before progression" principle. Not classified as a defect.

**10. Final verdict: A — READY TO COMMIT**

**11. Confirmation no files changed**

`git status --porcelain` was checked before and after this review: identical both times — the same three modified files (`src/interfaces/EvidenceIntelligence.kt`, `tests/contracts/EvidenceCustodianScopeTest.kt`, `tests/contracts/EvidenceIntelligenceTest.kt`) and three new files (the Planning Review doc, `DefaultEvidenceIntelligence.kt`, `DefaultEvidenceIntelligenceTest.kt`) from the prior engineering turn. No file was modified, created, or deleted during this review.

**12. No Git actions.** Nothing staged, committed, or pushed. (Gradle was re-run to confirm the build is still green: `BUILD SUCCESSFUL`, all tasks `UP-TO-DATE` or passing — no corrections were needed, so no new test totals differ from the 1334/0/0/0 already reported.)
