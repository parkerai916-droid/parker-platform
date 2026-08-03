# Evidence Intelligence — Unit 1 Governance Remediation Plan

## Status

**Programme:** Evidence Intelligence, Implementation Unit 1 ("Input/Output
Shape Foundation").

**Phase:** Governance remediation proposal, prepared in response to
Governance Contradiction Report GCR-EI-UNIT1-001. **This document is
itself a proposal, not an amendment.** It recommends what the Contract
Design, Scope Lock, and Implementation Plan should say once amended; it
does not itself change what any of them currently says. No Kotlin is
implemented, proposed as a diff, or changed by this document. Neither
`src/` nor `tests/` is touched. `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`,
`docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`,
`docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md`, and
`docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md`
are all unmodified by this document. Nothing is staged, committed, or
pushed.

**Ratification status: Drafted. Presented for Independent Constitutional
Review.** Not yet reviewed or accepted. This document authorises nothing
by its own drafting.

**Normative inputs, frozen, not redefined:** the same governing chain
Unit 1 itself operates under —
`docs/architecture/parker-constitution.md`;
`docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md`
("CDR-007"); `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`
("the Contract Design"); `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`
("the Scope Lock"); `docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md`
("the Implementation Plan"); `docs/decisions/CDR-005_CONSTITUTIONAL_ADMISSION_OF_PERMISSIONENGINE_PROPOSAL_CLASSES.md`
("CDR-005"), consulted here only for its general precedent on when a new
Constitutional Decision Record is required (§7, below); and
`docs/architecture/PARKER_ENGINEERING_STANDARD.md` ("PES-001"),
consulted only for its governance-workflow stage and change-level
definitions (§8, below).

---

## 1. Contradiction Record

**Identifier: GCR-EI-UNIT1-001 — CandidateRecordProduced Representation
Contradiction.**

**Summary.** `EvidenceAnalysisResult`'s `CandidateRecordProduced` variant
must carry, per Contract Design §5, "an existing, unmodified
`CandidateAssertion` or `CandidateRelationship`." Realising that content
requirement in Kotlin, without a new type, requires one of: a dedicated
sum type, an untyped (`Any`) carrier, a nullable-per-case field pair, or
a shared interface retrofitted onto the two existing candidate types.
Every one of those four general mechanisms is independently foreclosed
by an already-accepted governance clause (the Scope Lock's exactly-three
public-type cap; this repository's own structural-prevention and
type-safety discipline; and the Scope Lock's "no new sealed shape"
exclusion, respectively). The requirement set is therefore jointly
unsatisfiable by any Kotlin representation — not a drafting gap in Unit
1's own implementation, but a genuine contradiction between what the
Contract Design's content requirement demands and what the Scope Lock's
own type-count and shape exclusions permit.

Unit 1's prior implementation (the now-superseded `AnalyticalClaim` and
`CandidateMemoryCoreRecord` types) resolved the contradiction by silently
exceeding the type-count cap. That was an error, not a legitimate
resolution — identified, and corrected by removal from consideration,
across the governance review that produced GCR-EI-UNIT1-001.

---

## 2. Requirement Classification

### 2.1 Literal governing requirements — explicit, unqualified, no exception clause

| Requirement | Citation |
| --- | --- |
| Exactly three new public contracts (`EvidenceAnalysisRequest`, `EvidenceAnalysisResult`, `EvidenceIntelligence`) | Scope Lock §4: "No fourth new public type or interface is authorised. These three are the entire new public surface this implementation may create." Contract Design §3: "It introduces exactly two new public types"; §10: "this document defines exactly one new public interface." |
| `CandidateRecordProduced` must carry an existing, unmodified `CandidateAssertion` or `CandidateRelationship` | Contract Design §5 (output table); §3: "Each variant itself carries only an existing, unmodified candidate type." |
| No new sealed shape (of contested scope — see 2.3) | Scope Lock §3 (Explicit Exclusions): "A fourth `EvidenceAnalysisResult` variant, a partial-result wrapper type, or any new sealed shape," citing Contract Design §11's narrower "no fourth failure variant" precedent. |
| Reused Memory Core types (`CandidateAssertion`, `CandidateRelationship`) may not be modified | Scope Lock §4 / Contract Design §12: reuse table marks both "Reuses, unmodified." |

### 2.2 Requirements derived from Parker's structural-prevention and type-safety discipline — not literally named, but consistently and unbrokenly applied

| Requirement | Basis |
| --- | --- |
| No `Any`-typed field | Contract Design §11's own standard for this exact document: "a structural prevention, not a runtime check." No domain type anywhere in this repository uses `Any`. |
| No nullable-dual-field pattern permitting both/neither | Same §11 standard, plus this repository's unbroken practice of representing "one of several cases" as a sealed type (`EvidenceRetrievalResult`, `MemoryCoreRecord`, `KnowledgeCandidateEvaluation`) rather than parallel nullable fields, anywhere it has faced this exact problem before. |
| No wrapper interface retrofitted onto existing Memory Core types | Directly entailed by two literal clauses at once: the type-count cap (2.1, above — a new interface is a fourth public type) and the reused-unmodified freeze (2.1, above — implementing a new interface on `CandidateAssertion`/`CandidateRelationship` modifies them). |

These are not weaker obligations than 2.1's literal clauses; they are as firmly established by unbroken repository practice as any written rule. But they are properly distinguished from 2.1 because no single sentence in the Contract Design or Scope Lock names them by that description — a future reviewer should know the difference between "the text says this" and "every precedent in this repository says this."

### 2.3 The unresolved ambiguity in Scope Lock §3's "any new sealed shape"

The clause sits in a sentence whose other two items ("a fourth `EvidenceAnalysisResult` variant," "a partial-result wrapper type") are both scoped specifically to `EvidenceAnalysisResult`'s own output taxonomy. Two readings are both textually available:

- **Narrow reading:** the clause forbids only a new sealed shape that functions as an alternate or additional *output-category* mechanism (a competitor to, or expansion of, the frozen four-variant taxonomy). A closed, non-taxonomy-expanding selector used solely as one variant's own internal payload would not be caught.
- **Broad reading:** the clause forbids any new sealed type anywhere in Evidence Intelligence's model, without qualification — which would catch such a selector too.

The governing text does not itself resolve which reading controls, and this document does not resolve it either. §6, below, proposes the narrow reading as the one worth adopting going forward, but proposes it as an amendment — a clarification the amendment supplies — not as an interpretation already settled by the current text.

---

## 3. Implementation Stop Condition

- **Unit 1 is not complete.** Its own governing Implementation Plan requires it to realise `EvidenceAnalysisResult` exactly as Contract Design §5 defines it; GCR-EI-UNIT1-001 establishes that no compliant realisation currently exists.
- **Unit 2 is not authorised to begin.** The Implementation Plan's own sequencing freeze (§7) makes every later unit dependent on Unit 1's completion; Unit 1 having no compliant terminal state blocks the entire sequence at its root.
- **The current uncommitted Unit 1 code must not be committed in its present form.** `src/interfaces/EvidenceIntelligence.kt` and `tests/contracts/EvidenceIntelligenceTest.kt` remain on disk, untracked, exactly as produced before GCR-EI-UNIT1-001 was identified. They are preserved for reference and rework, not as a candidate for commit — they contain the two now-superseded, unauthorised public types (`AnalyticalClaim`, `CandidateMemoryCoreRecord`) this remediation plan exists to replace.

No file has been modified in the course of preparing this remediation plan. This section records the stop condition; it does not lift it.

---

## 4. Recommended Governance Repair

**Authorise exactly one additional behaviour-free public runtime value type owned exclusively by Evidence Intelligence** whose sole purpose is to represent exactly one existing `CandidateAssertion` or one existing `CandidateRelationship`, as the payload of the already-authorised `CandidateRecordProduced` result category.

Frozen properties of that selector, proposed for adoption verbatim:

1. **Behaviour-free.** No method beyond what Kotlin's own `sealed`/`data class` machinery generates (`equals`, `hashCode`, `toString`, `copy`, destructuring).
2. **Closed to exactly two cases.** One case selecting `CandidateAssertion`, one selecting `CandidateRelationship` — no third case, ever, without a further governance amendment.
3. **Owns no data beyond the selected existing candidate.** No field beyond the one wrapped `CandidateAssertion` or `CandidateRelationship` value.
4. **Introduces no fifth `EvidenceAnalysisResult` category.** `EvidenceAnalysisResult` itself remains sealed with exactly four direct variants; this type is not one of them — it is the payload type of one already-authorised variant (`CandidateRecordProduced`).
5. **Creates no acceptance, persistence, retrieval, reasoning, confidence, evidential-state, provenance, or ownership authority.** It is a pure selection mechanism; every one of those responsibilities remains exactly where the Contract Design and Scope Lock already assign it (never to Evidence Intelligence, per CDR-007).
6. **Does not modify `CandidateAssertion` or `CandidateRelationship`.** Both remain reused, unmodified, exactly as the existing dependency freeze already requires; this type references them, never extends, subclasses, or amends them.
7. **Cannot be reused as a generic union abstraction.** Not a `sealed class Either<L, R>` or any other reusable, type-parameterised mechanism — closed to these two named, concrete existing types only, by name, not by generic parameter.
8. **Owned exclusively by Evidence Intelligence.** Not offered to, depended upon by, or discoverable as a general-purpose utility for any other subsystem; its entire reason to exist is realising one already-authorised Contract Design content requirement for this one subsystem.

No Kotlin name, method signature, or package is proposed here — per this task's own instruction, this section states the type's constitutional shape and constraints only.

---

## 5. Governance Documents Requiring Amendment

- **Evidence Intelligence Contract Design §3** ("New types, owned by Evidence Intelligence") **and its ownership/type table** — to record a third new public runtime value type (the fourth authorised public contract type overall, alongside `EvidenceAnalysisRequest`, `EvidenceAnalysisResult`, and the `EvidenceIntelligence` interface), owned by Evidence Intelligence, with the frozen properties in §4 above stated as its own binding content.
- **Evidence Intelligence Scope Lock §3** (Explicit Exclusions) — to narrow the "any new sealed shape" clause per §6, below, and **§4** (Dependency Freeze) — to raise the public-surface count from three to four and add the new type's own row to the "new contracts already authorised" list.
- **Evidence Intelligence Implementation Plan, Unit 1, and every type-count reference elsewhere in that document** — to name the new type in Unit 1's own scope, and to correct every place the Plan currently states or assumes a count of three (its own Status section, §5/§6 "New Components," §10 Completion Criteria, §11 Out of Scope, and §12 Design Rules Compliance all currently assert or depend on the three-type count and will each need the corresponding word or number corrected).

No CDR file is listed here — see §7.

---

## 6. Clarification of "Any New Sealed Shape"

Proposed clarifying text for Scope Lock §3, narrowing the clause to what its own surrounding sentence already suggests it was about:

**Continues to prohibit, without exception:**
- A fifth `EvidenceAnalysisResult` category, however introduced.
- A new failure-result taxonomy (any variant, wrapper, or shape whose purpose is to signal an implementation-level failure, recoverable or not, in place of the existing failure-signalling mechanism Contract Design §11 already establishes).
- A partial-result wrapper (any type whose purpose is to represent partial completion across many referenced inputs, in place of the existing non-empty-list mechanism Contract Design §11 already establishes).
- Any alternate output taxonomy — any sealed or otherwise closed shape that competes with, extends, or stands beside `EvidenceAnalysisResult`'s own frozen four-variant classification of what Evidence Intelligence may produce.

**Does not prohibit:**
- The single, expressly authorised selector type described in §4, above, used solely as the payload of the already-authorised `CandidateRecordProduced` category — because it is not an output taxonomy of any kind; it selects between two possible contents of one already-fixed category, exactly as (for example) `EvidenceRetrievalResult`'s existing `Found`/`NotFound`/`Rejected` cases select between three possible outcomes of one already-fixed operation, without thereby constituting "a new taxonomy" of anything beyond that one operation's own result.

---

## 7. Constitutional Decision Record Requirement Assessment

**Applying the repository's own established precedent**, not assuming an answer in either direction. The only place this repository states a general rule for when a governance question must escalate to a new Constitutional Decision Record versus being resolved by a lower-tier document amendment is CDR-005 (Model C, "Governed Admission"). Restated, its three-way test:

> "When a CDR is required: whenever a domain's self-certification against [the higher-tier document's] criteria is genuinely contested, ambiguous, or would require choosing between two or more constitutionally plausible readings... When a domain amendment alone is sufficient: whenever the classification... is not genuinely contested... When [the higher-tier document] must be reopened: whenever any constitutional section [it] itself states... is materially altered. This is limited to material alteration of [its] own constitutional content, not implementation-level change; it is never triggered merely by the addition of a domain act that already fits existing, unaltered criteria."

Applied here by direct analogy (CDR-007 standing in the role Chapter 10 plays in CDR-005's own domain):

- **Does resolving GCR-EI-UNIT1-001 materially alter anything CDR-007 itself states?** No. A direct search of CDR-007's text for any mention of public type counts, sealed shapes, or Kotlin realisation mechanics returns nothing — CDR-007 operates entirely at the level of *what kind of thing* an artefact constitutionally is (its four permitted-artefact categories, §6), and says so explicitly: *"The examples are illustrative only — they classify what kind of thing an artefact is constitutionally; they do not design its storage, format, or implementation."* The proposed selector type changes no category, no ownership assignment, no authority boundary, and no non-responsibility CDR-007 fixes — it only supplies a Kotlin-level mechanism for realising a category (category 3, "Memory Core facts") CDR-007 already fully authorises, including its own explicit "including `SUPPORTS`/`CONTRADICTS`" content. By CDR-005's own test, this is "implementation-level change," not material alteration of CDR-007's constitutional content.
- **Is this "genuinely contested, ambiguous, or requiring a choice between constitutionally plausible readings" of CDR-007 itself?** No — the ambiguity identified in §2.3, above, is internal to the Scope Lock's own drafting (a domain-tier document), not a contest over how to classify this act against CDR-007's own published criteria. CDR-007 supplies no criterion here to be contested; the type-count cap and the "any new sealed shape" exclusion are both self-imposed Scope Lock/Contract Design inventions, not CDR-007 requirements being interpreted.

**Conclusion: applying the repository's current escalation criteria, this remediation recommends that no new Constitutional Decision Record is required.** A Contract Design and Scope Lock amendment, at the domain tier, is sufficient — mirroring CDR-005's own "when a domain amendment alone is sufficient" branch. Consistent with CDR-005's own posture, this is a recommendation, not a foreclosure: if an independent constitutional reviewer finds this analogy unpersuasive, or finds CDR-007's category-3 authorisation less complete than this assessment treats it, escalation to a new CDR remains available — exactly the "escalation valve for genuine doubt" CDR-005 itself preserves for its own domain.

---

## 8. Proposed Governance Sequence

Following `docs/architecture/PARKER_ENGINEERING_STANDARD.md`'s own stage discipline. Introducing any new public runtime contract, however narrow, requires at minimum a Contract Design pass (PES-001, "Level 3 – Architectural Change": *"Contract Design (Stage 2A), for any new public runtime contract"*). Given the narrowness of this change — one amendment to each of three already-accepted documents, not a new subsystem — the sequence below re-runs the relevant stages as **amendments** to the existing, already-accepted documents, not as fresh, from-scratch passes:

1. **Contract Design amendment** (re-running Stage 2A against the existing, Accepted Contract Design) — add the fourth authorised public runtime value type and its frozen properties (§4, above) to §3 and the ownership table.
2. **Independent review of the Contract Design amendment** (mirroring Stage 2's Architecture Review discipline, applied to the amendment) — before the Scope Lock amendment proceeds, since the Scope Lock's own authority is derivative of the Contract Design's.
3. **Scope Lock amendment** (re-running Stage 5 against the existing, Accepted Scope Lock) — raise the type-count cap, add the new type's dependency-freeze row, and narrow the "any new sealed shape" clause per §6, above.
4. **Implementation Plan correction** (re-running Stage 3 against the existing, Accepted Implementation Plan, narrowly) — correct Unit 1's own scope and every type-count reference the amendment invalidates.
5. **Independent Constitutional Review of the amended set** — the same review discipline every prior Evidence Intelligence governance document in this Programme has already undergone before reaching Accepted status.
6. **Only then** does Unit 1 implementation resume: the two superseded types are removed, `CandidateRecordProduced` is realised using the newly authorised selector, and Unit 1's own completion criteria are re-evaluated against the amended Implementation Plan.
7. **Unit 2 remains blocked** throughout steps 1–6, exactly as §3, above, states, and is only reconsidered once Unit 1 is independently confirmed complete against the amended governance set.

---

## 9. Scope Discipline

This document does not itself amend the Contract Design, the Scope Lock, the Implementation Plan, or CDR-007. It proposes no Kotlin source code, method signature, interface, or pseudocode of any kind. It does not decide whether GCR-EI-UNIT1-001 requires a new CDR beyond stating the assessment in §7, and it does not pre-empt the independent review §8 itself calls for. It does not reopen Unit 1's own implementation, and it does not authorise Unit 2 to begin.

---

**Status: Drafted. Presented for Independent Constitutional Review.** Not marked Accepted. No Constitutional Decision Record is created by this document. No other document — the Contract Design, the Scope Lock, the Implementation Plan, any CDR, or the Parker Constitution — is modified by this document.

EVIDENCE INTELLIGENCE UNIT 1 GOVERNANCE REMEDIATION PLAN — DRAFTED — PRESENTED FOR INDEPENDENT CONSTITUTIONAL REVIEW

Confirmed: no Kotlin implemented; no interface, method signature, API, schema, or storage technology defined; no pseudocode, diagram, or implementation example included; the Contract Design, the Scope Lock, the Implementation Plan, CDR-007, and the Parker Constitution all unmodified; `src/interfaces/EvidenceIntelligence.kt` and `tests/contracts/EvidenceIntelligenceTest.kt` unmodified and uncommitted; Unit 1 not reopened; Unit 2 not begun; nothing staged; nothing committed; nothing pushed.
