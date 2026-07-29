**Status:** Narrow governance clarification only. Does not reopen Programme 3's architecture, layering, public model, or any of the eight amendments `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` resolved. Does not alter constitutional doctrine. Does not amend `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` or `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md`, both of which remain frozen and unchanged. No Kotlin is implemented, proposed, or changed by this document.

# Programme 3 — Unit 4 Scope Lock Clarification

Programme: **Programme 3 — Knowledge Memory, Unit 4 Scope Lock Clarification.**

This document resolves exactly the two questions the Unit 4 Governance Reconciliation (a read-only review conducted against `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`, `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md`, `docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md`, and `docs/architecture/MEMORY_ARCHITECTURE_RECONCILIATION.md`, delivered in chat rather than as a saved document) left open after concluding `REVISION REQUIRED`. It resolves nothing else, and settles no question that Reconciliation did not itself raise. The Reconciliation's other two findings — that `evidenceReference` must not be typed exclusively as `AssertionId`, and that the two-value `KnowledgeItemStatus` enum requires no revision — are already unambiguous under existing frozen governance and require no clarification here; the code fix for the first is carried out directly, per Part B of this task.

---

## 1. Knowledge History Authority

**Finding.** `KnowledgeItem.history: List<KnowledgePromotion>` (Unit 4) and the legacy `KnowledgeRecord.history: List<String>` (Unit 1, renamed unchanged from `MemoryRecord.history`) are two structurally independent, differently-shaped "history" fields with no declared relationship between them — a duplicate-source-of-truth risk under `docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md` §5's "avoid duplicate sources of truth" principle: "never as a second, parallel type carrying its own independent copy of what the renamed shape already holds... At no point during the Programme do two independently-writable representations of the same promoted knowledge exist simultaneously."

**Clarification, binding on all Programme 3 implementation from this point forward:**

- `KnowledgeItem.history` is the sole constitutionally authoritative structured lifecycle and classification history for the new Knowledge Memory public model (Contract Design Version 2 §12; Articles XVI/XVII). It is the only history a future unit may treat as satisfying Article XVI's "chronologically ordered, non-forking" requirement.
- Legacy `KnowledgeRecord.history: List<String>` remains exactly what Unit 1 renamed it to be: the free-text audit trail belonging to today's still-production-wired `KnowledgeStore`/`KnowledgeRecord` path, retained solely for backward-compatible legacy behaviour during migration (Implementation Plan §5, "Maintain backward compatibility until replacement is complete... for the entire duration of Programme 3").
- `KnowledgeRecord.history` must not be treated as an independently authoritative history for the same promoted knowledge a `KnowledgeItem` also describes. The two are not guaranteed to describe the same event set, and no code may assume they do.
- No Programme 3 unit — Unit 4 or any unit after it, up to and including Unit 9 — may synchronise, copy, merge, or dual-write between `KnowledgeItem.history` and `KnowledgeRecord.history`. Each is written to independently, by its own path, or not written to at all.
- **Unit 10 (Composition wiring and regression verification)** owns the migration responsibility for this field. Its own acceptance pass, run against `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §9's Success Criteria — specifically "Migration from `MemoryStore` is complete... no code path continues to construct or depend on the legacy flat, duplicate-field submission shape" — is where a Programme 3-level decision must be made and disclosed: either the legacy `KnowledgeRecord.history` field is explicitly retired (its last production writer removed) or explicitly adapted (redefined to delegate to or derive from `KnowledgeItem.history`) before Programme 3 is considered complete. Until Unit 10, both fields may continue to coexist, unsynchronised, exactly as this clarification permits.

---

## 2. KnowledgeReference Content

**Finding.** `KnowledgeReference.summary: String` (Unit 4) is a stored, captured-at-construction field, in tension with Contract Design Version 2 §12's own "carries no independent state" requirement for this contract, and is not concretely specified by any frozen document.

**Clarification:**

- `KnowledgeReference` is identifier-only for the duration of Programme 3. Its sole field is `knowledgeId: KnowledgeId`.
- No stored summary, display text, or projection of any kind belongs on `KnowledgeReference` itself. Any such content, if ever supplied, must be assembled fresh at retrieval time from the live `KnowledgeItem` (or the Memory Core evidence it references) — never captured once and held independently. This is what "carries no independent state" requires structurally, not merely by convention.
- **Unit 9 (Knowledge Query, Knowledge Result, Knowledge Retrieval)** is the authorised unit that may supply projected summary or task-scoped display detail, as part of the Knowledge Result contract Contract Design Version 2 §12 already authorises to "bundle Knowledge References/Items." A Knowledge Result is a response-time projection, not a stored reference, so it does not carry the same "no independent state" constraint `KnowledgeReference` does. Whether that projection lives on Knowledge Result directly, or on some other Unit-9-authorised shape, is Unit 9's own design decision, not fixed by this clarification.

---

## 3. Scope of This Clarification

This document resolves only the two questions above. It does not authorise, and must not be read as authorising, any change to `KnowledgeRecord`'s own fields or behaviour, any change to `KnowledgeStore`'s methods, any lifecycle logic, any promotion algorithm, or any work belonging to Unit 5 or later. It creates no new public type and amends no constitutional doctrine.

---

## Disposition

Unit 4 implementation is authorised to proceed with the minimal corrections this clarification directs, under the same Unit 4 boundary the original engineering task already fixed.
