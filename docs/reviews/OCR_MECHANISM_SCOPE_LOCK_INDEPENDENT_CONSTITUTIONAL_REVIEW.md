# OCR Mechanism Scope Lock — Independent Constitutional Review

## Status

**Independent review only. No file modified.** `docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md` and `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md` are both reviewed, neither is edited. No Kotlin implemented. No Implementation Plan drafted. Nothing staged, committed, or pushed.

---

## 1. Repository Baseline

`main` at `89bd6ad754017ae02d8de90d95d71ee52d5fdd1a`. Working tree matched the expected state exactly before this review began:

```
?? docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md
?? docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md
```

No discrepancy.

---

## 2. Documents Reviewed

Parker Constitution; CDR-006 (full); CDR-007 (full); Evidence Intelligence Contract Design (as amended by Amendment 2, full); Evidence Intelligence Scope Lock (as amended, full); Evidence Processing Searchable PDF Boundary Clarification (full); Evidence Processing Searchable PDF Scope Lock (full); `OCR_MECHANISM_CONTRACT_DESIGN.md` (full, re-read fresh for this review); `OCR_MECHANISM_SCOPE_LOCK.md` (full, re-read fresh for this review); the Amendment 2 Final Acceptance Confirmation; the OCR Ownership and Sequencing Review; the OCR Planning Review; the OCR Mechanism Amendment Proposal (final revised version, for its own §7 finding on the `RequiresOcr` consumer, cited by the Scope Lock).

---

## 3. Scope Lock Fidelity

Traced each of the Scope Lock's eighteen sections against the Contract Design's own corresponding section. The overwhelming majority — capability boundary (§3), invocation boundary (§4), original-evidence boundary (§7), derivative boundary (§8), output-quality validation boundary (§11), owner-control boundary (§12), dependency boundary (§13), provider neutrality (§14), and the explicit exclusions (§16) — are faithful, narrowly-scoped conversions of the Contract Design's own already-stated responsibilities, non-responsibilities, and deferrals into fixed, numbered constraints. No new authority is invented, no ownership boundary is narrowed or broadened, and no deferred question named in Contract Design §10 or §11 is resolved by any of these sections.

**One section requires correction before acceptance: §10, Failure Taxonomy.** The Contract Design's own §11 ("Deferred to Future Implementation") states explicitly: "Any concrete failure-taxonomy type — §6's three-way classification is a constitutional/organisational one, not a Kotlin sealed type." This deliberately confines what may be fixed at Contract-Design/Scope-Lock tier to the *three-way responsibility split* (detection failure → Evidence Processing; recognition-quality failure → Evidence Intelligence's judgement; operational/mechanical failure → future implementation) — it does not authorise naming the *specific operational failure modes* within that third category. The Scope Lock's §10 nonetheless freezes eleven named categories (unsupported input, encrypted/inaccessible input, malformed input, timeout, resource limit exceeded, engine unavailable, partial recognition, no recognisable text, output invalid, permission not authorised, implementation fault) in a table — several of which (timeout, resource limit exceeded, engine unavailable, output invalid) read as previews of what will eventually become concrete Kotlin sealed-type variant names, mirroring `ExtractionOutcome`'s own four-variant shape in the Evidence Processing programme, rather than the organisational classification the Contract Design's own §11 confined this governance tier to. This is the one place in the document where Scope-Lock-tier elaboration crosses from "converting an accepted decision into a fixed constraint" into "resolving something the Contract Design deliberately left open" (Review Question 1's own test) — even though no Kotlin syntax appears anywhere in it.

**A secondary, minor fidelity observation, not requiring correction:** §9 (Provenance Boundary) elaborates "processing version," "processing time," and "output hash" as minimum required facts by direct analogy to Evidence Processing's own already-established `ExtractionIdentity`/`extractedAt`/`integrityHash` precedent — a different governance document from the one this Scope Lock is meant to convert. The Scope Lock discloses this analogy honestly rather than presenting it as a literal quotation of the Contract Design, and introduces no new type or field, only a more specific reading of what the Contract Design's own "structured identity disclosure" (§5 there) already implies. This is a defensible elaboration, not a violation, but it is the second instance in the document of Scope-Lock-tier content drawing more from analogous precedent than from the Contract Design's own literal text.

---

## 4. Capability Boundary Review

Confirmed. §3 states, and correctly traces to Contract Design §1–§3, §12, that the OCR mechanism is: a capability, not a concrete engine (Contract Design Status, §11, §13); a pure callee (Contract Design §4, §12, mirroring `ReasoningProvider`); not a subsystem (Amendment 2's own constitutional-tier reasoning, correctly cited); not a coordinator (Contract Design §2, "orchestration... out of scope"); not a truth authority (Contract Design §2, §8); not a persistence owner (Contract Design §2, "Write Memory"); not a Memory component (Contract Design §2, §12); not a Knowledge component (Contract Design §2, §12). All eight required properties present and correctly sourced.

---

## 5. Ownership Boundary Review

- **Evidence Processing ownership of OCR detection** — preserved. §4 explicitly prohibits the OCR mechanism from consuming `RequiresOcr` directly, and §17's self-certification restates this. No paragraph anywhere in the Scope Lock touches, narrows, or duplicates Evidence Processing's own detection responsibility.
- **Evidence Intelligence ownership of OCR execution** — preserved and made executable. §4 confines invocation to "an authorised Evidence Intelligence orchestration path," consistent with CDR-007's own classification and Amendment 2's own dependency-table row.
- **Evidence Custodian ownership** — preserved. §8 (Derivative Boundary) correctly restates the Evidence Intelligence Contract Design's own ownership-transfer-on-acceptance rule, unmodified; §13 (Dependency Boundary) confirms no dependency on `EvidenceCustodian.accept`.
- **Provenance ownership** — preserved. §9 confirms every fact is expressed through Memory Core's existing, unmodified `Provenance`/`CandidateProvenance` contract; no new provenance-carrying type is introduced (see §3, above, for the one qualification on how far §9's elaboration reaches).
- **Original-evidence immutability** — preserved without exception. §7 traces directly to CDR-006's Constitutional Optimisation Safeguard, which names OCR by its own illustrative example.

All five ownership boundaries the review was asked to check are intact.

---

## 6. Authority Boundary Review

Confirmed the OCR mechanism never gains authority to: accept evidence (§3, §13; Contract Design §2, §12); reject evidence (§11; Contract Design §2, "Reject evidence" — the mechanism discloses, Evidence Intelligence decides); determine truth (§3, §17; Contract Design §2, §8); modify originals (§7; CDR-006); write Memory (§3, §13; Contract Design §2, §12); submit Knowledge (§3, §13; Contract Design §2, §12); report conclusions (§6, "final reports of any kind" is explicitly listed as a prohibited output; §16's Reporting row). All seven prohibitions are present, correctly sourced, and none is narrowed anywhere else in the document.

---

## 7. Invocation Boundary Review

Confirmed: self-triggering is prohibited (§4, mirroring the Evidence Intelligence Scope Lock's own "No self-initiated analysis" invariant); background execution is prohibited (§4); repository/folder/queue/conversation scanning is prohibited (§4); conversation attachment is prohibited (§4, naming `submitOwnerMessage` specifically — see §13, below, for one style observation on this); machine-triggered invocation is left unresolved (§4's own closing line, §12, §18); owner-authorisation for it is left unresolved (§12, §18). No implicit invocation path was found anywhere in the document — every invocation route described is explicitly gated through "an authorised Evidence Intelligence orchestration path," itself gated by the already-frozen Permission Engine step-0 gate the Evidence Intelligence Scope Lock established.

---

## 8. Input/Output Boundary Review

**Input (§5):** permitted categories (source evidence identity, immutable/read-only source bytes, media type, page/document scope, processing context) all trace to Contract Design §4. Prohibited categories (authority to alter the original, Memory Core write access, Knowledge Submission access, Evidence Custodian acceptance authority, broad repository access, arbitrary filesystem authority) all trace to Contract Design §2, §7, or the Scope Lock's own §15. No public interface or Kotlin shape is implied — every category is described only in prose.

**Output (§6):** confirmed limited to elaborations of the Contract Design's own four categories (recognised text, fidelity disclosure, identity disclosure, confidence signal), with the Failure Taxonomy caveat noted at §3, above, applying equally here to the extent §6 leans on §10's own over-specific categories. Confirmed the output boundary does **not** accidentally authorise evidence, Memory, Knowledge, reports, or truth claims — all five are explicitly named as prohibited representations.

**One citation defect found in §6:** "final reports of any kind (§14, below)" — §14 is *Provider Neutrality*, which has no relationship to reporting. The correct target is either §16 (Explicit Exclusions, where the "Reporting" row actually lives) or no cross-reference at all. This is a mechanical drafting defect, not a substantive one — see Findings, below.

---

## 9. Provenance Review

Confirmed complete against the Contract Design's own §7 requirements: source identity, mechanism identity (provider-neutral), source-to-output relationship, and honest disclosure of uncertainty are all present. Confirmed governed — every fact is expressed through Memory Core's existing `Provenance`/`CandidateProvenance` contract, explicitly stated to remain "owned... exactly where CDR-006 and the Evidence Intelligence Contract Design already placed" it. Confirmed sufficient for downstream traceability — the eight facts listed (§9) collectively supply everything `extractedFrom`/`derivedFrom` and the fidelity-disclosure requirement need. **No new provenance model is invented** — confirmed directly; every fact traces to an already-existing field. The one qualification already noted at §3, above (three of the eight facts are elaborated by analogy to Evidence Processing's own precedent rather than the Contract Design's own literal text) does not itself constitute a new model, only a more specific reading of an existing one.

---

## 10. Failure Taxonomy Review

The three-way responsibility split (mechanism/orchestration/Evidence-Intelligence-judgement) is correctly assigned in every row of the §10 table: the six operational categories correctly belong to a future mechanism implementation; "partial recognition" and "no recognisable text" correctly belong to Evidence Intelligence's own analytical judgement, with the table's own language correctly preserving that the mechanism only discloses, never decides; "permission not authorised" is correctly classified as an orchestration outcome that never reaches the mechanism at all, mirroring the Evidence Intelligence Scope Lock's own step-0 gate precisely. **No misplaced responsibility was found** — every category sits at the correct layer.

**The defect is not misplacement; it is over-specification relative to the Contract Design's own explicit deferral, as detailed at §3, above.** The Contract Design's §11 confines this governance stage to the three-way organisational split; the Scope Lock's eleven named categories exceed that confinement, even though each is correctly assigned once named.

---

## 11. Provider Neutrality Review

Confirmed. §14, cross-checked by direct grep of the entire Scope Lock: no OCR provider is selected; OCRmyPDF, Tesseract, PaddleOCR, and EasyOCR each appear only as explicit non-examples ("is not authorised by name... none is chosen, evaluated, ruled in, or ruled out"); no provider preference is implied anywhere; the provider-specific-type-leakage prohibition correctly cites the already-established `org.apache.tika.*`-confinement precedent from Evidence Processing as the model this document expects a future implementation to follow.

---

## 12. Deferred Governance Review

Confirmed the Scope Lock leaves unresolved, correctly and explicitly: machine-triggered invocation (§4, §12, §18); the owner-authorisation model for it (§12, §18); rejected-output permission gating (§11, §18); validation policy (§11, §18); provider implementation (§14, §18); runtime composition (§13, §16, §18). Cross-checked each of the six against the Contract Design's own §10/§11 deferral lists — all six trace directly, and none is silently decided anywhere else in the document. §18's own consolidated list matches the six items exactly, with two additions (the `RequiresOcr`-consuming coordinator, and exact Kotlin/file-layout questions) both independently and correctly traceable to Contract Design §10 item 3 and §11 respectively.

---

## 13. Security Boundary Review

Confirmed §15 establishes constitutional minimums only: no network access absent future authorisation, bounded resource consumption, a controlled temporary workspace, prohibition of path traversal and command injection, cleanup deferred to future rules, and a blast-radius guarantee against hostile input. Confirmed **no** process architecture, container architecture, operating-system assumption, or deployment assumption is chosen anywhere — §15's own opening sentence states this explicitly, and no later paragraph in the document contradicts it.

**One style observation, not a security-substance defect:** §4 names `submitOwnerMessage` — an existing, real entry-point name in this repository — as an example of what the OCR mechanism must not attach itself to. The task's own style constraint for this document was unqualified: "Do not use... Kotlin names." Naming an existing function as a *prohibition target* is a materially different use from defining a new one, and the intent (foreclosing an obvious anti-pattern) is sound — but the letter of the style constraint was not observed. This is noted as a minor drafting-discipline finding, not a constitutional one.

---

## 14. Explicit Exclusions Review

The §16 table contains exactly the fifteen items the drafting task itself required (provider selection; OCRmyPDF implementation; Tesseract implementation; Docling; structured document model; reporting; runtime composition; background queues; automatic conversation invocation; Memory writes; Knowledge promotion; evidence acceptance; truth determination; output-quality policy; rejected-output permission semantics), each with a distinct, non-overlapping reason. Confirmed exhaustive against that list and internally consistent — no row contradicts another, and no excluded item reappears elsewhere in the document as an authorised capability.

---

## 15. Self-Certification Verification

Each of the ten claims in §17 was checked against the document's own content, not accepted on assertion:

- Owner control, original-evidence immutability, Evidence Custodian ownership, Evidence Processing ownership, Evidence Intelligence ownership, provenance, no peer subsystem creation, and no public-contract expansion — **each independently verified true**, against the sections the claim itself cites.
- No implementation pre-authorisation — **verified true for provider/Kotlin/deployment choices specifically** (§14, §15, §16 correctly contain none), but the claim should be read alongside the Failure Taxonomy finding (§3, §10, above): §10's own level of specificity is the one place in the document closest to this boundary, even though it names no Kotlin type.
- Parser non-authority — **verified true in substance**, but its own citation, "(§3, §7, above...)", imprecisely includes §7 (Original-Evidence Boundary, about overwriting and identity), which is not actually about truth authority; the correct sole citation is §3 (Capability Boundary, "Not a truth authority"). A minor citation-precision defect, not a substantive one.

---

## 16. Findings

**Finding 1 (primary — fidelity, moderate severity).** §10 (Failure Taxonomy) names eleven specific operational failure categories in a table, exceeding the organisational three-way split the Contract Design's own §11 explicitly confined this governance tier to ("Any concrete failure-taxonomy type... is deferred"). Each category is correctly assigned once named (§10, above), so there is no misplaced-responsibility defect — only an over-specification defect relative to what the Contract Design authorised at this tier.

**Finding 2 (mechanical — internal cross-references, minor severity, three instances).**
- §1 (Executive Summary): "each remains explicitly deferred (§4, §11, §12, §17)" — §17 is *Constitutional Self-Certification*; the correct target is §18 (*Deferred to Future Governance and Implementation*).
- §12 (Owner-Control Boundary), closing line: "This remains future governance (§4, above; §17, below)" — same error; should read §18.
- §6 (Output Boundary): "final reports of any kind (§14, below)" — §14 is *Provider Neutrality*, unrelated; the correct target is §16 (*Explicit Exclusions*, where the Reporting row lives) or no cross-reference.

**Finding 3 (minor — self-certification citation precision).** §17's "Parser non-authority" bullet cites "(§3, §7, above...)" — §7 is about original-evidence immutability, not truth authority. The correct, sole citation is §3.

**Finding 4 (minor — style discipline).** §4 names `submitOwnerMessage`, an existing repository-level function name, as a prohibition target. This is a defensible use in substance but is not strictly consistent with the drafting task's own unqualified "Do not use... Kotlin names" style constraint.

**Finding 5 (disclosed, not a violation — noted for completeness).** §9 (Provenance Boundary) elaborates three of its eight minimum facts (processing version, processing time, output hash) by analogy to Evidence Processing's own established precedent rather than the Contract Design's own literal text. The Scope Lock discloses this honestly and introduces no new type or field; recorded here only because Review Question 8 asked this to be checked explicitly.

No finding was made against ownership boundaries, authority boundaries, provider neutrality, security boundaries, or deferred-governance preservation — all five passed without qualification.

---

## 17. Required Corrections

1. Trim §10 (Failure Taxonomy) to the Contract Design's own three-way organisational split (detection / recognition-quality / operational), removing the eleven named sub-categories as a Scope-Lock-tier freeze — or, if the eleven categories are judged genuinely necessary at this tier, first amend the Contract Design's own §11 to explicitly authorise Scope-Lock-tier failure-category naming (a narrow, disclosed amendment, not a redesign) before this Scope Lock relies on it. Do not resolve this silently; the choice belongs to whoever accepts this Scope Lock, not to this review.
2. Correct the three cross-reference errors identified in Finding 2 (§1, §12, §6).
3. Correct the self-certification citation identified in Finding 3 (§17).
4. Optional, not blocking: replace the `submitOwnerMessage` example (Finding 4) with a purely descriptive phrase (for example, "any existing communication-layer message-submission entry point") that conveys the same prohibition without naming an existing Kotlin-level identifier.

None of these corrections requires new constitutional reasoning; each is a textual-precision or scope-narrowing fix, consistent with the Contract Design's own already-accepted text.

---

## 18. Constitutional Verdict

**REQUIRES REVISION**

The Scope Lock's ownership boundaries, authority boundaries, provider neutrality, security minimums, and deferred-governance preservation are all sound and ready. The document requires one substantive correction (Finding 1, the Failure Taxonomy's over-specification relative to the Contract Design's own explicit deferral) and several mechanical corrections (Findings 2–4) before it may be accepted.

---

## 19. Recommended Next Step

Apply the four corrections in §17 in a narrow, targeted pass — not a redesign — then perform one final, narrow confirmation review, mirroring exactly the discipline already used for Amendment 2's own two-round defect-correction cycle. Only after that confirmation should this Scope Lock's own status change from Draft to Accepted, and only then is an OCR Mechanism Implementation Plan authorised to begin.
