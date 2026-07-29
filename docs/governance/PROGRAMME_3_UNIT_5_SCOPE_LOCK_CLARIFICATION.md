**Status:** Narrow governance clarification only. Does not reopen Programme 3's architecture, layering, public model, or any of the eight amendments `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` resolved. Does not alter constitutional doctrine. Does not amend `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`, `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md`, or `docs/governance/PROGRAMME_3_UNIT_4_SCOPE_LOCK_CLARIFICATION.md`, all of which remain frozen and unchanged. No Kotlin is implemented, proposed, or changed by this document.

# Programme 3 — Unit 5 Scope Lock Clarification

Programme: **Programme 3 — Knowledge Memory, Unit 5 Scope Lock Clarification.**

This document resolves the conflict the Unit 5 engineering task identified between Contract Design Version 2's repeated, literal description of `Knowledge Candidate`'s field shape and the Implementation Plan's own migration/test wording concerning legacy `CandidateKnowledge`. It resolves nothing else, and settles no question not raised by that conflict.

---

## 1. KnowledgeCandidate Is a New Public Contract

`KnowledgeCandidate` is distinct from legacy `CandidateKnowledge`. It is the constitutionally governed submission contract Programme 3 introduces — one of the eight contracts `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §5 Deliverable 1 authorises "in full," inventoried by Contract Design Version 2 §12.

It is **not** a rename of `CandidateKnowledge`. It is **not** an adapter around `CandidateKnowledge`. It is **not** a second authoritative durable-knowledge representation — it represents nothing durable at all; a candidate that is not promoted produces no Knowledge Item and no Knowledge Memory record of any kind (Contract Design Version 2 §3). It is a submission boundary type only: the caller-facing proposal that existing Memory Core evidence be evaluated for promotion, and nothing more.

---

## 2. Authorised Field Shape

For Programme 3, `KnowledgeCandidate` contains exactly:

```
val evidenceReference: MemoryCoreRecordReference
```

No other field is authorised in Unit 5. This is the literal, consistent reading of three independent passages in Contract Design Version 2: §2 ("Carries a reference to existing Memory Core evidence and nothing else evidential"), §7 ("Every Knowledge Item, Knowledge Candidate, and Knowledge Promotion record carries a reference to Memory Core content — never a copy"), and §12 ("Carries a Memory Core evidence reference only").

Specifically, `KnowledgeCandidate` must not contain: confidence; `EvidentialState`; payload; category; source; correlation identifier; originator identity; sensitivity flag; timestamp; or provenance content. None of these is named anywhere in Contract Design Version 2, the Scope Lock, or the Unit 4 Clarification as belonging to Knowledge Candidate. Their presence on legacy `CandidateKnowledge` is migration context, not authority for carrying them forward onto the new contract.

---

## 3. Legacy CandidateKnowledge Status

Legacy `CandidateKnowledge` remains unchanged during Unit 5 solely to preserve backward compatibility and the existing test suite (`docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §7, Backward Compatibility; `docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md` §5, "none is asked to change what it already proves"). It is migration state, not the new constitutional submission contract, and it has no production call site today (`KnowledgeStore.remember` is confirmed, by `DefaultReasoningContextAssembler`'s own KDoc, to never be called in production).

No new production path may be added during Unit 5, or any unit before Unit 10, that treats `CandidateKnowledge` and `KnowledgeCandidate` as co-equal authoritative submission models. Each remains written to, or constructed, independently, exactly as `docs/governance/PROGRAMME_3_UNIT_4_SCOPE_LOCK_CLARIFICATION.md` §1 already established for `KnowledgeItem.history` and `KnowledgeRecord.history` — the same discipline, applied to the submission side of the same rename family.

**Unit 10 (Composition wiring and regression verification)** is the unit the Implementation Plan already assigns this migration responsibility to, per its own scope ("confirm the full pre-existing test suite... still passes; run a full acceptance pass against Scope Lock §6, §7, and §9") and Scope Lock §9's Success Criterion that, by Programme 3's own completion, "no code path continues to construct or depend on the legacy flat, duplicate-field submission shape." Until Unit 10, `CandidateKnowledge` and `KnowledgeCandidate` coexist, unsynchronised and independently maintained, exactly as this clarification permits — the same unit, and the same reasoning, the Unit 4 Clarification already fixed for the corresponding history-field question.

---

## 4. Verification Interpretation

The Implementation Plan's malformed-submission verification requirement ("a test proving a submission carrying either excluded field is rejected as malformed") is satisfied structurally, not by a runtime rejection test:

- `KnowledgeCandidate` has no caller-settable confidence field.
- `KnowledgeCandidate` has no caller-settable evidential-state field.
- Therefore, a submission carrying either is impossible to construct through the public contract at all — a compile-time guarantee, stronger than, and satisfying the intent of, a runtime-rejected malformed instance.

No field is added merely to give a runtime rejection test something to reject. Existing `CandidateKnowledge` construction tests remain unchanged and continue to pass because the legacy type itself is untouched by this Unit.

---

## 5. Scope of This Clarification

This document resolves only the four points above. It does not authorise, and must not be read as authorising, any change to `CandidateKnowledge`'s own fields or behaviour, any change to `KnowledgeStore.remember`, any promotion or evaluation logic, or any work belonging to Unit 6 or later. It creates no new public type beyond confirming `KnowledgeCandidate`'s already-described shape, and amends no constitutional doctrine.

---

## Disposition

Unit 5 implementation is authorised to proceed exactly as scoped by this clarification.
