**Status:** Narrow Defect Confirmation Review, following the Independent Constitutional Review's `REQUIRES REVISION` verdict on `docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_RESOLUTION_DERIVATION_MECHANISM_CLARIFICATION.md`. No Kotlin, test, or frozen/draft governance document is touched. Nothing is staged, committed, or pushed.

# Trust Framework — Memory Retrieval Resolution Derivation Mechanism Clarification — Defect Confirmation Review

## The Two Required Corrections

The Independent Constitutional Review (`docs/reviews/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_RESOLUTION_DERIVATION_MECHANISM_CLARIFICATION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`, Sections 4 and 5) found:

1. Section 6 presented the resolution-derivation mechanism as working uniformly across all nine Errata-004-named action names, but `memory.transition_status`/`memory.delete_record` cannot be reduced to a single `(action, resourceType)` pair from the verb phrase alone.
2. Section 3's own table claimed the two Memory Core decorators were "the only two production classes... constructing `targetResources = emptyList()`," stated more absolutely than the evidence (a broader grep found `InMemoryAgentRuntime.kt`'s own caller-supplied, potentially-empty `targetResources`) supported.

## Corrections Applied

- **Section 3's own table** — the Resource Registry resolution row now states the closed-set claim precisely (literal `emptyList()` construction, not every path that could theoretically produce an empty list), discloses the `InMemoryAgentRuntime.kt` case, and explains why it does not weaken the mechanism (Section 6's own verb-phrase-closed-set condition already guards against it, consistent with the system's own pre-existing, verb-phrase-is-the-trust-boundary design).
- **Section 4** — the closed-set sentence now says "genuinely closed today for literal, always-empty construction," matching the corrected Section 3 claim.
- **Section 6** — restructured to state plainly that the mechanism is derived per-verb-phrase "for the seven of the nine action names that are genuinely single-typed," explicitly naming `memory.retrieve`/`memory.retrieve_document` as both unambiguous and the two this task's own scope actually concerns. A new paragraph discloses the `memory.transition_status`/`memory.delete_record` exception in full: why they are not single-typed, what a composite/tie-break treatment would require and its own honest under-approval risk, and why this document defers resolving them (dormant `PermissionGatedMemoryCore`, outside this task's own read-side audit trace) rather than silently generalising the mechanism over them.

No other section was changed. Both corrections are disclosures and precision fixes to Sections 3, 4, and 6 — they do not alter Section 5's own resource-existence/classification reasoning, Section 7's own shared-decorator finding, or Section 9's own readiness conclusion, each of which the ICR independently confirmed already accounted for, or was unaffected by, these findings.

## Re-Verification

- **Scope check:** the only edited regions are the Section 3 table row, one sentence in Section 4, and the "What is derived" paragraph plus new disclosed-exception paragraph in Section 6 — all traced directly above.
- **Consistency check:** the corrected Section 6 text is consistent with the ICR's own Section 5 finding (confirmed the same seven/two split, the same composite-mapping/tie-break description, the same dormancy/out-of-scope rationale for deferral).
- **No regression:** every other ICR finding (Sections 2, 3, 6, 7, 8, 9 of that review) required no correction and remains valid against the now-corrected document, since none of those sections' own subject matter was touched by these two corrections.

## Outcome

```
DEFECT CONFIRMATION REVIEW COMPLETE — CORRECTIONS APPLIED, NO FURTHER DEFECT FOUND
```

The Clarification, as corrected, stands as this governance cycle's own final artifact. Its own Section 9 conclusion is unaffected and remains in force: the mechanism-level work (verb-phrase-specific rule matching plus closed-set, no-Resource-required derivation for the seven single-typed action names, centrally including `memory.retrieve`/`memory.retrieve_document`) could lawfully begin at Scope Lock; a Scope Lock step that also approves `memory.retrieve`/`memory.retrieve_document` may not yet begin, pending resolution of the newly-disclosed shared-decorator prerequisite (Section 7 of the Clarification).
