# Programme 3 Unit 9 Scope Lock Clarification — Independent Constitutional Review

## 1. Status

**Independent constitutional review only.** The subject document, `docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md`, was not modified during this review. Unit 9 was not implemented. No interface, query type, result type, runtime wiring, test, or Permission Engine code was created. Nothing is staged, committed, or pushed. This review does not rely on the drafting session's own completion report — every citation below was re-verified against primary text or the actual code.

---

## 2. Repository Baseline

- **HEAD:** `fabb2124a94eed449095b207efada804dc072ea8` (`fabb212`)
- **Branch:** `main`
- **Remote:** `origin` → `git@github.com:parkerai916-droid/parker-platform.git`; `origin/main` confirmed identical to local `HEAD`.
- **Working tree, confirmed before this review began:**
  ```
  ?? docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md
  ?? docs/reviews/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_PLANNING_REVIEW.md
  ?? docs/reviews/PROGRAMME_3_UNIT_9_RETRIEVAL_PERMISSION_EVALUATION_PLANNING_REVIEW.md
  ```
  Exactly the three expected files. No discrepancy.
- **Staged changes:** none.

---

## 3. Authorities Reviewed

Read fresh for this review: `docs/architecture/parker-constitution.md` (Architectural Responsibilities; "Cognition proposes, Trust authorises, Runtime executes"; fail-closed clause); `docs/architecture/10-permission-engine.md` (§3, §10 re-read exactly); `docs/decisions/CDR-005_CONSTITUTIONAL_ADMISSION_OF_PERMISSIONENGINE_PROPOSAL_CLASSES.md` (full, Decision Rules and Consequences sections re-read exactly); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` (§6, §7, §12); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` (§5 Deliverables 8 and 9, compared directly); `docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md` (§13 re-read exactly; confirmed no separate independent-review document exists for Unit 7); `docs/governance/PROGRAMME_3_UNIT_8_SCOPE_LOCK_CLARIFICATION.md` (§1, §5, §12); `docs/reviews/PROGRAMME_3_UNIT_8_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` (full, its own Determination and Recommendation section read exactly, used here as a calibration point for verdict rigor); `docs/reviews/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_PLANNING_REVIEW.md` and `docs/reviews/PROGRAMME_3_UNIT_9_RETRIEVAL_PERMISSION_EVALUATION_PLANNING_REVIEW.md`; the subject Clarification itself, in full; `src/interfaces/KnowledgeStore.kt` (`KnowledgeItem`, lifecycle types); `src/composition/PermissionFilteredMemoryRetrieval.kt` (full, as comparative evidence only); `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` §5, §6 (re-read exactly).

---

## 4. Governance Vehicle Review

**A Unit 9 Scope Lock Clarification is a lawful vehicle for this classification, but the subject document under-defends its own tier choice against a real asymmetry with the Unit 7 and Unit 8 precedents it invokes.**

It does not improperly amend Contract Design V2 — confirmed directly; V2's text is neither quoted as changed nor treated as changed anywhere in the subject document, only cited. It does not require a CDR under CDR-005 (Section 10, below, tests this independently and confirms it).

**Does it follow the accepted Unit 7 precedent accurately? Partially — the method is followed accurately; a real difference in starting conditions is not disclosed.** Unit 7 Clarification §13 performed a first-time Chapter 10/CDR-005 classification, at Scope-Lock-Clarification tier, for an act (revision, supersession, retirement, restoration) that Contract Design V2 already discussed in substantial detail but had never itself classified for permission purposes — this is genuinely the same shape of act the subject document performs for Retrieval, and the subject document's method (apply Chapter 10 §3, cite CDR-005 Model C, disclose reasoning) matches Unit 7's method precisely. But a material asymmetry exists that the subject document does not confront: Scope Lock §5's own Deliverable 8 already named a permission dimension for Submission before any Clarification existed — "Permission-boundary wiring — Evaluation B implemented at Knowledge Memory's own submission boundary" — and Contract Design V2 §7 (Amendment 8) had already named Evaluation B's own existence, leaving only mechanism for the Unit 8 Clarification to resolve. Scope Lock §5's Deliverable 9, by direct comparison, names no such dimension for Retrieval at all — "Retrieval pipeline — the Knowledge Query/Knowledge Result surface, delivered complete... but not itself wired into Reasoning Context" — silent on permission entirely. Unit 7's own act (the four lifecycle acts) is closer to Unit 9's own starting condition than Unit 8's is, since neither Contract Design V2 nor the Scope Lock pre-named a permission conclusion for lifecycle acts either — but Unit 7's classification was *negative*, adding nothing to PermissionEngine's own scope, while the subject document's classification is *positive*, recognising a wholly new proposal class from a starting condition in which no higher-tier document — Contract Design or Scope Lock — has ever named, anticipated, or flagged that a permission dimension might attach to Retrieval at all. The subject document's own Section 1 borrows Unit 8's "narrowest link of that chain" framing without acknowledging that, for a positive classification with no prior naming at any tier, it is doing more first-instance constitutional work than that framing implies. This does not make the vehicle wrong — CDR-005's own Decision Rules draw no distinction between positive and negative classifications, or by governance tier, and require only that classification happen in "the domain's own governance document" — but the document should say so explicitly rather than presenting itself as straightforwardly parallel to two precedents that are not, on this specific point, equally parallel to each other.

---

## 5. Chapter 10 Admission Review

The proposed act is stated precisely (Section 5 of the subject document: a caller issuing a Knowledge Query and receiving a Knowledge Result disclosing an already-promoted `KnowledgeItem`'s evidential-state classification and provenance reference). The proposer is identified consistently with existing precedent (a requesting principal, explicit, never ambient — Section 11). The authority retrieval would exercise is described accurately: disclosure of governed material to a specific principal, not a write.

**The conclusion is supported without relying on Chapter 10's inaccurate worked example — verified directly, not merely trusted.** Re-checking Memory Core Scope Lock §5 and §6 word for word against the subject document's own Section 6 quotations: the subject document's claim that Scope Lock §5's "structural, non-semantic retrieval" clause concerns Memory Core's own matching algorithm, not a gating exemption, is accurate — that clause sits under "Memory Core owns," a different sentence from the "Runtime owns... every `PermissionEngine.evaluate` call" clause Chapter 10 draws its "sensitive vs. non-sensitive" distinction from. Scope Lock §6's "without exception" is quoted accurately and is decisive. `PermissionFilteredMemoryRetrieval.kt`, re-read in full for this review, confirms all eleven methods gate unconditionally, with no sensitivity branch anywhere. The subject document's Section 6 finding is correct and independently reproducible from primary sources.

The act genuinely satisfies Chapter 10 §3: it is not "ordinary internal computation... structural retrieval of non-sensitive records," since the records in question are, by construction, downstream of Memory Core evidence this repository's own built precedent treats as requiring gating without exception.

---

## 6. Permission Classification Review

The document correctly decides that a PermissionEngine proposal class is required, reasoned from the corrected Chapter 10 §3 test and corroborated independently by Unit 7 §13's own categorical distinction (retrieval discloses to an external principal; none of Unit 7's four acts do) — this corroboration was checked directly against Unit 7 §13's exact text ("none exposes anything beyond what Promotion... already exposed without Permission Engine gating") and holds.

It avoids inventing a new "Evaluation C" tier: the label appears only in the Status block and Git Confirmations, both explicitly disclaiming it as non-canonical, and nowhere in the substantive Sections 4–15 is a third evaluation tier named, argued for, or implied as architecturally distinct from the two-evaluation (A/B) model already in place — the document treats the classification as *a* proposal class, not as a formally numbered third tier.

It distinguishes proposal-class admission from implementation naming throughout (Section 12's own explicit exclusion list), and avoids pre-authorising any resource identifier, action string, Kotlin type, or method signature — confirmed by direct search of the document's own text; none appears anywhere outside the disclaimers noting their deliberate absence.

---

## 7. Memory Core Boundary Review

Correctly established, and independently reproducible from the code, not merely asserted. `PermissionFilteredMemoryRetrieval.kt`, read in full, implements `MemoryRetrieval` exclusively — every method signature accepts or filters `Entity`, `Document`, `Assertion`, `Relationship`, or the `MemoryCoreRecord` sealed supertype; `KnowledgeItem` does not appear anywhere in the file, is not assignable to any parameter type used, and could not pass through `isApproved`'s own action-name switch (keyed to `RETRIEVE_ACTION_NAME`/`RETRIEVE_DOCUMENT_ACTION_NAME`, both Memory-Core-specific). The type-level separation is correctly reasoned, not overstated.

The document does not accidentally make Knowledge Retrieval depend on Memory Core retrieval — Section 9 explicitly reserves `PermissionFilteredMemoryRetrieval` as "comparison material" only, and this framing is honoured throughout; no section of the document proposes Knowledge Retrieval call, wrap, or extend it.

---

## 8. Lifecycle Boundary Review

Correctly states that lifecycle visibility and permission are distinct concerns (Section 10), grounded in Chapter 10 §5's "Separation from domain evaluation" guarantee and in a direct, accurate reading of Unit 7 Clarification §9's own "never implies deletion"/"visible restoration event" language as concerning history-visibility, not principal-visibility.

It avoids deciding revision, retirement, restoration, ranking, ordering, or query semantics — confirmed by direct search; Section 12's exclusion list names each explicitly, and no other section makes a determination about any of them. It preserves the existing lifecycle evaluators' own ownership — `DefaultKnowledgeRevisionEvaluator` and `DefaultKnowledgeRetirementEvaluator` are not named anywhere in the subject document, and nothing in it touches their responsibilities.

---

## 9. Owner-Control Review

The proposed classification preserves explicit principal handling (Section 11, first bullet, mirroring Errata 004 and Unit 8 Clarification §6 precisely). It places authority with the Permission Engine, not Knowledge Memory (Section 8: "The Permission Engine remains the sole authority for the decision itself, regardless of this document's own conclusion"). It prevents ambient or implicit identity by the same citation. It avoids self-authorisation: the document performs only the classification authority CDR-005's Model C assigns to a domain (the authority to test its own act against Chapter 10's already-published criteria and disclose the result), never the authority to decide the outcome of any actual future permission evaluation — that distinction is honoured throughout and stated explicitly in Section 8.

---

## 10. CDR-005 Assessment

**Model C self-certification is lawfully sufficient; the issue is genuinely uncontested; the flawed worked example does not itself create a contested constitutional interpretation.**

Re-tested independently against CDR-005's own Decision Rules ("a CDR is required whenever a domain's self-certification... is genuinely contested, ambiguous, or would require choosing between two or more constitutionally plausible readings"): the subject document's Section 7 considers one genuine counter-reading (retrieval as "reasoning over already-available information") and defeats it by direct comparison with the one precedent this repository has actually built (Memory Core gates every read, including of "already-available" records, without exception) — this is not a case of two equally-supported readings with no way to prefer one, which is the bar CDR-004 was created to resolve; it is a case of one reading with strong, built, on-point support and one reading that, on examination, proves not to generalise even to the domain it would need to in order to hold.

**The conflict in Chapter 10's worked example does not itself create a contested constitutional interpretation requiring escalation, and the subject document's own reasoning for why is sound.** The example's inaccuracy is a defect in Chapter 10's own illustrative text, not a defect in Chapter 10 §3's own general criterion — the general criterion (real/external/state-changing consequence; "granting access to a sensitive record") is stated independently of the flawed illustration and remains applicable once the illustration is set aside. Chapter 10 §10's own extensibility rule confirms this reading: "Chapter 10 itself is reopened only when its own constitutional content changes... never merely because a new domain act, cleanly fitting existing criteria, is being recognised" — an inaccurate example is not "constitutional content" in the sense that clause protects; correcting it, when it eventually happens, belongs to Chapter 10's own Final Freeze Verification, exactly as the subject document states.

---

## 11. Public-Contract Consequences

The document remains at constitutional-purpose level throughout Section 11 — checked directly against Unit 8 Clarification §6 and §9's own identical level of abstraction ("must be an explicit parameter"; "capable of representing at least three distinguishable outcomes," neither naming a type). The subject document's two stated consequences (explicit principal; a denial result distinct from "no matches") are directly entailed by the positive classification itself and mirror this Programme's own established pattern exactly — they do not prematurely require a particular denial representation, filtering mechanism, or query/result field, and do not constrain Unit 9's own future design beyond what any positive PermissionEngine classification already, unavoidably, requires.

---

## 12. Scope Discipline

Confirmed, by direct search of the subject document's full text: it does not reopen Units 6, 7, or 8 (each named only as closed precedent, never as reopened); it does not resolve the legacy `KnowledgeSource`/V2 `KnowledgeItem` store coexistence question the preceding planning reviews identified (not mentioned anywhere in the subject document — appropriately silent, since that question is orthogonal to permission classification); it does not compose Reasoning Context (Section 12's own exclusion list names this explicitly); it does not begin Unit 9 implementation (no Kotlin, interface, or type named anywhere).

**One minor, non-blocking observation:** the subject document does not note that the already-live, already-wired legacy `KnowledgeSource.recall()` path (a different, Sprint-11-era capability, per the first Unit 9 planning review, not Knowledge Retrieval itself) is unaffected by this classification. This is not a defect — conflating the two would violate the same "classify Knowledge Retrieval on its own domain act" discipline the classification exercise itself requires — but a one-sentence scope note would pre-empt a reasonable reader's question about why an existing, ungated retrieval path is not immediately addressed by a document about retrieval permission classification.

---

## 13. Findings

| # | Severity | Finding |
| --- | --- | --- |
| 1 | Moderate | The document does not disclose that, unlike the Unit 8 precedent it partially borrows language from, no prior Contract Design V2 or Scope Lock text ever named a permission dimension for Retrieval — Scope Lock Deliverable 9 is silent where Deliverable 8 was explicit. The document's own vehicle-tier framing ("narrowest link of that chain") is accurate for the *method* it follows (closer to Unit 7's true first-instance precedent) but is not disclosed as departing from Unit 8's own starting condition, which the document's borrowed phrasing implicitly invites a reader to assume is equally parallel. |
| 2 | Minor | The document does not note that the already-live legacy `KnowledgeSource.recall()` path is a different act, unaffected by this classification — worth one clarifying sentence, not a defect. |
| 3 | None (confirmed sound) | Chapter 10 admission reasoning, Memory Core type-level boundary, lifecycle/permission separation, owner-control preservation, CDR-005 non-escalation reasoning, and public-contract-consequence calibration are each independently verified against primary sources and found accurate without qualification. |

---

## 14. Required Corrections

One correction is required before acceptance:

1. **Add an explicit paragraph, within the governance-vehicle reasoning (Section 1 or a new subsection), disclosing that Scope Lock Deliverable 9 — unlike Deliverable 8 — never named a permission dimension for Retrieval, and Contract Design V2 never classified Retrieval for permission purposes at any tier before this document.** State plainly that this document's own precedent is therefore closer in kind to Unit 7's (a genuine first-instance classification, not merely a mechanism resolution) than to Unit 8's, and that CDR-005's own Decision Rules support this tier regardless of that difference, since they draw no distinction between positive and negative classifications or by governance tier — but say so, rather than leaving a reader to assume equal parallelism with both precedents.

The Section 12 observation (legacy `KnowledgeSource` path) is a recommended clarification, not a required correction — the document remains sound and unambiguous without it.

---

## 15. Constitutional Verdict

```
REQUIRES REVISION
```

The classification's substance — the admission test, the corrected reading of Chapter 10's worked example, the Memory Core type-level boundary, the lifecycle/permission separation, owner-control preservation, and the CDR-005 non-escalation determination — is sound, independently verified against primary sources and the actual code, and requires no change. What blocks acceptance is narrower: the document borrows Unit 8's own vehicle-tier framing without disclosing that its own starting condition (no prior naming at any higher tier, a positive rather than negative outcome) more closely resembles Unit 7's precedent and departs from Unit 8's in a way a careful reader should be told, not left to assume. This is a disclosure gap in the document's own reasoning about itself, not a defect in the classification it reaches.

---

## 16. Recommended Next Step

Add the required correction (Section 14) directly to the subject document, then request a narrow defect-confirmation review — not a full re-review — verifying only that the correction was made and nothing else was altered, mirroring this repository's own established "narrow correction pass, then defect-confirmation review" pattern already used for the Memory Core Durability Contract Design. Only after that confirmation should the document's own status move from "Narrow governance clarification only" to an adopted disposition, and only then is a future Unit 9 Contract Design passage (recording this classification within Contract Design V2 itself, per the subject document's own Section 15) or a mechanism-resolving follow-up Clarification authorised to begin.

---

## 17. Git Confirmations

- The subject document was not modified during this review.
- Unit 9 was not implemented.
- No interface, query type, result type, runtime wiring, test, or Permission Engine code was created.
- Nothing was staged during this review.
- Nothing was committed during this review.
- Nothing was pushed during this review.

## 18. Final Git Status

```
$ git status --short
?? docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md
?? docs/reviews/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_PLANNING_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_RETRIEVAL_PERMISSION_EVALUATION_PLANNING_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
```
