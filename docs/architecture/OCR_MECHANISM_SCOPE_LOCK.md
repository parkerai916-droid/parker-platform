# OCR Mechanism — Scope Lock

## Status

**Draft. Pending independent constitutional review. Not canonical. Not accepted. Not frozen. Not implementation authority.** No Kotlin is implemented, proposed as a diff, or changed by this document. No API, database schema, hashing algorithm, or storage technology is specified, invented, or implied. No new interface, repository, service, or public type is introduced anywhere below. Neither `src/` nor `tests/` is touched. Nothing is staged, committed, or pushed. This document does not become binding until it has separately passed the same independent constitutional review cycle every other Scope Lock in this repository has passed.

**Implements `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md` ("the Contract Design") without reopening it.** Every responsibility, non-responsibility, input/output shape, failure category, provenance obligation, and constitutional constraint the Contract Design already fixed is treated here as given, not re-derived. This document adds nothing the Contract Design did not already authorise; it only converts that authorised shape into fixed, numbered implementation constraints — exact Kotlin names, method signatures, and file layout remain a future Implementation Plan's own responsibility, not begun here.

Also binding, unmodified, and not reopened by this document: `docs/decisions/CDR-006_CONSTITUTIONAL_CLASSIFICATION_OF_ORIGINAL_EVIDENCE_CUSTODY_AND_IMMUTABILITY.md` ("CDR-006"), `docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md` ("CDR-007"), `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` (as amended by Amendment 2) and `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` (as amended, mirrored), and `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`/`EVIDENCE_PROCESSING_SEARCHABLE_PDF_SCOPE_LOCK.md`. None of these five documents is amended, narrowed, or reinterpreted by anything below.

**Scope Lock Principle.** The OCR mechanism's first implementation shall perform exactly one act — interpreting image content Evidence Intelligence already holds a governed reference to into recognised text, disclosed honestly — and nothing more. It is not intended to become a coordinator, an evidence custodian, a truth authority, a persistence owner, or a Memory or Knowledge component. Where a candidate capability is plausible, useful, or eventually necessary but not required to satisfy that one sentence, it is **OUT OF SCOPE** — the burden of proof favours exclusion, not inclusion, throughout this document.

Programme: **OCR Mechanism — Scope Lock.**

---

## 1. Executive Summary

This Scope Lock freezes the implementation boundary the Contract Design already authorised: a capability-level Evidence Intelligence dependency, structurally parallel to `ReasoningProvider`, holding no dependency of its own, invoked only by an authorised Evidence Intelligence orchestration path, producing recognised text and its required disclosures as candidate material only — never a governed record, never accepted evidence, never a truth claim. Fourteen boundaries are frozen below (§3–§16), each converting one of the Contract Design's own sections into a numbered, binding constraint. No concrete OCR provider, Kotlin shape, runtime composition, permission gate design, or output-quality policy is chosen anywhere in this document — each remains explicitly deferred (§4, §11, §12, §18).

---

## 2. Frozen Objectives

Implementation shall achieve, and shall be judged against, exactly the following:

1. The OCR mechanism holds no dependency of its own on `EvidenceCustodian`, `MemoryCore`'s write interface, Knowledge Memory's Knowledge Submission interface, or the Permission Engine, at any depth (Contract Design §2, §8, §12).
2. No code path by which the OCR mechanism operates ever writes, overwrites, replaces, or obscures original evidence content (CDR-006's Constitutional Optimisation Safeguard, naming OCR explicitly).
3. The OCR mechanism never itself constructs a governed record — no `CandidateEvidenceArtifact`, no `Assertion`, no `Relationship`, no `KnowledgeCandidate` (Contract Design §5).
4. Every recognition the OCR mechanism discloses carries a fidelity classification drawn from the Evidence Intelligence Contract Design's own three already-frozen categories (§5) — never a fourth, invented category.
5. No implementation of the OCR mechanism holds a Permission Engine reference of its own, and no implementation may be invoked other than through an already-authorised Evidence Intelligence orchestration path (§4, §12, below).
6. No implementation names, selects, or depends upon a concrete OCR engine, library, or service (§14, below).
7. Every provenance fact the OCR mechanism's own output requires uses Memory Core's existing, unmodified `Provenance`/`CandidateProvenance` contract — no new provenance type, no new field (§9, below).

---

## 3. Capability Boundary

Frozen exactly as Contract Design §1–§3 and §12 already establish, restated here as binding, not merely referenced. The OCR mechanism is:

- **An abstract capability**, not a concrete engine — no provider, library, or service is named or implied by anything this document freezes (§14, below).
- **A pure callee**, mirroring `ReasoningProvider`'s own "pure callee, calls nothing" shape exactly (Contract Design §4, §12): it receives already-retrieved content and already-established context, and returns a disclosure. It initiates nothing on its own.
- **Not a subsystem.** Amendment 2 (Evidence Intelligence Contract Design §12, Status) authorised the OCR mechanism as a capability-level dependency *within* the existing Evidence Intelligence subsystem, constitutionally analogous in tier to `ReasoningProvider` — never a new peer subsystem alongside Evidence Custodian, Evidence Intelligence, or Knowledge Memory.
- **Not a coordinator.** It performs no sequencing, orchestration, or acceptance-triggering act of any kind (Contract Design §2, "Perform orchestration, runtime composition, or dependency injection").
- **Not an evidence custodian.** It holds no custody, storage, or persistence responsibility of any kind (Contract Design §2, "Accept evidence"; "Hold custody, at any depth").
- **Not a truth authority.** It cannot determine, assert, or classify truth (Contract Design §2, "Decide truth"; §8).
- **Not a persistence owner.** It holds no dependency capable of writing anything durable, and constructs no durable record itself (Contract Design §2, "Write Memory").
- **Not a Memory or Knowledge component.** It holds no dependency on `MemoryCore`'s write interface or Knowledge Memory's Knowledge Submission interface, at any depth (Contract Design §2, §12).

---

## 4. Invocation Boundary

- The OCR mechanism **may be invoked only by an authorised Evidence Intelligence orchestration path** — the same relationship the Contract Design's own §12 already fixes (Evidence Intelligence depends on the OCR mechanism; the reverse dependency does not exist).
- It **may not self-trigger.** No implementation of the OCR mechanism decides, on its own initiative, what or when to recognise — mirroring exactly the Evidence Intelligence Scope Lock's own already-frozen "No self-initiated analysis" invariant (§8), applied here one tier further out.
- It **may not consume Evidence Processing's own `RequiresOcr` disclosure directly.** The OCR Mechanism Amendment Proposal's own §7 finding remains controlling and is not reopened here: the lawful consumer of `RequiresOcr` is a separate, not-yet-designed, composition-level coordinator that constructs an ordinary `EvidenceAnalysisRequest` and invokes `EvidenceIntelligence.analyse`; the OCR mechanism itself is reached only from *inside* that already-governed Evidence Intelligence invocation, never directly from Evidence Processing's own output.
- It **may not scan repositories, folders, queues, or conversations.** A direct corollary of holding no dependency of its own (§3, §12, above) and initiating nothing on its own (§4, above) — no implementation may grant the OCR mechanism an independent read path of any kind into any Parker store.
- It **may not attach itself to the ordinary owner-conversation submission path, or to any other communication-layer entry point.** The same "no independent trigger" principle applied specifically to the platform's normal conversational submission path — the entry point most likely to be mistaken for a convenient invocation hook.
- It **may not start background work.** Whether OCR requires asynchronous or background invocation at all was identified, and left unresolved, by the OCR Planning Review (§3.9); until a future governance stage resolves it, no implementation may introduce background or scheduled invocation of any kind.

**The future owner-control model for machine-triggered invocation is not resolved by this Scope Lock.** This remains future governance (§12, below), exactly as the Contract Design's own §10 already identifies it.

---

## 5. Input Boundary

The Contract Design's own §4 fixes the permitted input categories; this Scope Lock freezes them without assigning any Kotlin name or method signature:

**Permitted:**

- **Source evidence identity**, sufficient to resolve the recognition back to the original it was produced from — required by §9 (Provenance Boundary), below, and implied by the Contract Design's own "image content Evidence Intelligence already holds a governed reference to" (§4).
- **Immutable source bytes, or controlled, read-only access to them** — never a writable or mutable handle of any kind (Contract Design §4, §7).
- **Media type**, passed through unchanged from whatever upstream detection produced it (Contract Design §4).
- **Page or document scope**, passed through unchanged (Contract Design §4).
- **Processing context** already established by the caller — the fixed set of facts Evidence Intelligence supplies alongside the content itself (Contract Design §4).

**Not determined to be a distinct OCR-mechanism-level input by the Contract Design, and not introduced as one here:**

- **Requesting principal.** The Contract Design's own §4 names no principal-level input to the OCR mechanism itself; principal-level audit context remains at Evidence Intelligence's own tier (its existing `PrincipalId`, "for audit purposes only"), not duplicated here.
- **Provenance context, as a distinct input.** The Contract Design treats provenance as an *output* obligation (§7 there; §9, below), not an input the OCR mechanism itself consumes — source evidence identity (above) is the only provenance-adjacent fact it receives.

**The mechanism must not receive:**

- authority to alter the original (§7, below; CDR-006);
- Memory Core write access (§3, above; Contract Design §2, §12);
- Knowledge Submission access (§3, above; Contract Design §2, §12);
- Evidence Custodian acceptance authority (§3, above; Contract Design §2, §12);
- broad repository access of any kind (§4, above);
- arbitrary filesystem authority (§15, below).

No caller-declared confidence or evidential-state value may be accepted as input, mirroring the Evidence Intelligence Contract Design's own §4 discipline exactly (Contract Design §4).

---

## 6. Output Boundary

The Contract Design's own §5 fixes four output categories (recognised text; a fidelity disclosure; a structured identity disclosure; a working confidence signal). This Scope Lock freezes the permitted output surface at the same granularity a future implementation will need, without inventing a fifth category or any Kotlin shape:

**Permitted, each an elaboration of one of the Contract Design's own four categories, never a fifth:**

- **Recognised text**, optionally organised at page-aligned granularity where the input's own page scope (§5, above) supports it — an elaboration of "recognised text" (Contract Design §5), not a new category.
- **Technical metadata** — the structured identity disclosure the Contract Design's own §5 already requires (what configuration produced the recognition, without naming a concrete engine).
- **Confidence or warning indicators, where genuinely available** — the working confidence signal and fidelity disclosure the Contract Design's own §1 and §5 already authorise; a low-confidence or partial recognition is itself the honest disclosure the Contract Design requires, not a new output kind.
- **Processing status and failure information** — the honest disclosure Contract Design §6 already requires of every failure category (§10, below), never a silent or fabricated result.
- **Provenance-relevant processing facts** — the source-identity, identity-disclosure, and fidelity facts §9, below, requires Evidence Intelligence to be able to construct provenance from.

**The output must not represent:**

- truth claims (§3, above; Contract Design §2, §8);
- accepted evidence — the output is never itself a `CandidateEvidenceArtifact` or any other governed record (Contract Design §5);
- Knowledge Items (Contract Design §2, §12);
- Memory Core records (Contract Design §2, §12);
- final reports of any kind (§16, below);
- Evidence Intelligence acceptance decisions — the OCR mechanism never itself decides that its own output is, or is not, worth producing as a candidate (Contract Design §6).

No public result type or Kotlin shape is invented by this document; the categories above bound a future Implementation Plan's own type design, they do not supply it.

---

## 7. Original-Evidence Boundary

Frozen without exception, tracing directly to CDR-006's Constitutional Optimisation Safeguard (naming OCR explicitly) and the Contract Design's own §2 and §7:

- The original is never overwritten.
- The original's identity never changes.
- OCR operates only on controlled copies or read-only access to already-retrieved content (§5, above) — never a live, writable reference to the original.
- Every OCR output is a separate artefact from the original, never a revision of it (CDR-006's separate-identity requirement).
- No output may be presented, labelled, or treated as though it were the original.
- A processing failure, of any kind (§10, below), cannot modify, corrupt, or otherwise affect original custody — the OCR mechanism holds no write path to the original under any failure condition, exactly as it holds none under success (§3, §5, above).

---

## 8. Derivative Boundary and Ownership Transition

- **OCR output is a candidate derivative, and only a candidate derivative, until Evidence Intelligence's own analytical judgement decides to produce it and the existing, unmodified acceptance path registers it.** The Contract Design's own §5 and §6 already establish this: the OCR mechanism's output "is not itself a `CandidateEvidenceArtifact`... it is the raw material Evidence Intelligence's own analysis uses to decide whether... to produce a governed `CandidateEvidenceArtifact`."
- **It becomes a governed derivative only after Evidence Custodian registration** — the same ownership-transfer-on-acceptance rule the Evidence Intelligence Contract Design already froze (§5 there), reused here unmodified, not reinterpreted.
- **OCR output is never automatically canonical evidence merely because recognition succeeded.** Technical success (a confident, complete recognition) and constitutional acceptance (registration as a governed derivative) are two distinct events; this document introduces no path that collapses them.
- **No new derivative relationship is invented.** The existing `extractedFrom`/`derivedFrom` mechanism (§9, below) already fully expresses the relationship between an OCR-produced candidate and its original; this Scope Lock adds no parallel or additional relationship type.

---

## 9. Provenance Boundary

The Contract Design's own §7 fixes that the OCR mechanism creates no independent provenance model. This Scope Lock freezes the minimum set of facts a future implementation must make available so that whatever eventually constructs provenance can do so honestly:

- **Source evidence identity** — sufficient to populate `extractedFrom`/`derivedFrom` (Contract Design §7; CDR-006's mandatory traceability rule).
- **Processing mechanism identity, without choosing a provider** — the structured identity disclosure Contract Design §5 already requires, elaborated here to include whatever configuration facts a future implementation finds necessary, still without naming a concrete engine (§14, below).
- **Processing version, where available** — an elaboration of the identity disclosure above; the Contract Design does not name "version" as its own field, but the identity disclosure it already requires is understood to include it, mirroring Evidence Processing's own already-established `ExtractionIdentity` precedent for exactly this kind of reproducibility fact.
- **Processing time** — an elaboration of the identity disclosure, mirroring Evidence Processing's own already-established `extractedAt` precedent.
- **Page ordering**, where the input's own page scope (§5, above) makes it meaningful.
- **Source-to-output relationship** — `extractedFrom`/`derivedFrom`, the same, existing, unmodified Memory Core mechanism every other Evidence Intelligence-produced derivative already uses (Contract Design §7).
- **Warnings and partial-result status** — the honest disclosure Contract Design §6 already requires for a partial or insufficient recognition (§10, below).
- **Output hash, where applicable** — not named as its own field by the Contract Design, but consistent with, and not additional to, the already-existing, generic `CandidateDocument.integrityHash` mechanism every other Evidence-Intelligence-produced or Evidence-Processing-produced derivative already populates.

**No new provenance-carrying type is introduced.** Every fact above is expressed through Memory Core's existing, unmodified `Provenance`/`CandidateProvenance` fields, exactly as the Contract Design's own §7 already requires. Provenance ownership itself remains exactly where CDR-006 and the Evidence Intelligence Contract Design already placed it — Memory Core's own contract, never a parallel mechanism of the OCR mechanism's own.

---

## 10. Failure Taxonomy

The Contract Design's own §6 establishes three broad kinds of failure (detection failure, belonging to Evidence Processing; recognition-quality failure, belonging to Evidence Intelligence's own judgement; operational/mechanical failure, belonging to a future OCR mechanism implementation) plus one constitutional-failure category. The Contract Design's own §11 confines this governance tier to exactly that organisational split — "any concrete failure-taxonomy type... is a constitutional/organisational one, not a Kotlin sealed type," deferred to future implementation. This Scope Lock does not exceed that confinement: it freezes no exhaustive, named, coded, or enum-like list of failure categories. It freezes only that a future implementation must not collapse the following constitutional distinctions into one another, whatever concrete shape it eventually gives them:

- **Not authorised** — an orchestration outcome, never reached by the mechanism itself. Mirrors the Evidence Intelligence Scope Lock's own step-0 invocation gate: denial stops before any dependency is invoked; the OCR mechanism holds no Permission Engine reference of its own (§3, §12, above) and never itself evaluates or reports this outcome.
- **Unsupported or inaccessible input** — the OCR mechanism's own operational concern (Contract Design §6, "operational/mechanical failure"), disclosed honestly, never silently substituted with a fabricated result.
- **No recognisable content** — disclosed by the OCR mechanism honestly (Contract Design §6); whether that disclosure means the recognition is worth producing as a candidate at all is Evidence Intelligence's own analytical judgement, never the mechanism's own decision.
- **Partial or technically degraded output** — the same disclosure/judgement split as above: the mechanism discloses what it could and could not recognise; Evidence Intelligence's own analysis decides what that means for the result it produces.
- **Validation rejection** — Parker-owned output-quality judgement (§11, above), never the mechanism's own determination and never an orchestration-layer authorisation failure; a distinct outcome from "not authorised," above.
- **Processing or dependency failure** — the OCR mechanism's own operational concern (Contract Design §6), covering conditions such as a resource limit being exceeded (§15, above) or a required processing step being unavailable, disclosed rather than silently retried or masked.
- **Genuine implementation fault** — the OCR mechanism's own operational concern (Contract Design §6), distinct from every expected operational condition above.

**None of the seven distinctions above may be collapsed into one another, in any implementation.** Each is a distinct, separately disclosed outcome, belonging to exactly one of three responsibility tiers this document already fixes elsewhere: the OCR mechanism's own operational implementation (unsupported/inaccessible input; processing or dependency failure; genuine implementation fault); orchestration (not authorised); and Evidence Intelligence's own analytical judgement, exercised as Parker-owned output-quality judgement where §11 governs it (no recognisable content; partial or technically degraded output; validation rejection). **The concrete taxonomy, naming, and representation of these distinctions remain future governance or implementation work** — a future Implementation Plan's own responsibility, not fixed here, consistent with Contract Design §11's own deferral.

**Which failures are constitutional.** Restated from Contract Design §6: only an attempted act §3 and §7, above, forbid (modifying an original, writing Memory, submitting Knowledge, assigning `EvidentialState`, self-authorising invocation) is constitutional in kind — prevented structurally by the mechanism holding no dependency capable of it, never merely policed as a runtime outcome. None of the seven distinctions above is, itself, a constitutional failure.

---

## 11. Output-Quality Validation Boundary

- The OCR mechanism **may report technical confidence, warnings, or completeness indicators** (§6, above) — this is its full and complete role in quality disclosure.
- It **does not decide whether its own output is sufficiently trustworthy for downstream use.** That determination belongs to Evidence Intelligence's own analytical judgement (Contract Design §6), exactly as Evidence Intelligence already decides, for any other analytical step, whether a result is worth producing as a candidate or only as transient output.
- **Parker-owned validation** — Evidence Intelligence's own analysis, followed by the existing, unmodified acceptance path — decides whether output may become a governed derivative, never the OCR mechanism itself.
- **Permission gating and disposition for rejected output remain unresolved future governance.** The Contract Design's own §10 (item 2) already identifies this precisely; this Scope Lock does not resolve it, and creates no validation policy, threshold, or mechanism of any kind.

---

## 12. Owner-Control Boundary

- **Owner authority must be preserved in full.** Whether OCR capability is available at all, and whether any specific invocation is authorised, remain the owner's own decisions, exercised through the Permission Engine — never assumed, defaulted, or exercised on the owner's behalf by the OCR mechanism, Evidence Intelligence, or any coordinator (Contract Design §9).
- **No machine-triggered OCR flow is authorised merely by this Scope Lock.** Freezing the OCR mechanism's own contract boundary does not, by itself, authorise any concrete invocation path, human-triggered or machine-triggered.
- **The principal and authorisation basis must be explicit in any future orchestration.** No future implementation may rely on an unstated, assumed, or implicit principal.
- **Ambient identity, thread-local identity, or inferred authority is prohibited.** Every invocation of the OCR mechanism, through Evidence Intelligence, must trace to an explicit, caller-supplied authorisation fact — never one recovered from execution context, a coroutine-local value, or any other implicit carrier.

**The future trigger model — in particular, owner control for any machine-triggered invocation — is not resolved by this Scope Lock.** This remains future governance (§4, above; §18, below), exactly as the Contract Design's own §10 (item 1) already identifies it.

---

## 13. Dependency Boundary

The OCR mechanism itself holds **no platform subsystem dependency of any kind**, mirroring exactly the Contract Design's own §12 ("holds no dependency of its own — mirroring `ReasoningProvider`'s 'pure callee, calls nothing' shape"). Specifically, it must not depend on:

- Evidence Custodian, at any depth;
- Memory Core, at any depth;
- Knowledge Submission, at any depth;
- the Permission Engine, at any depth;
- Evidence Intelligence's own public result handling — no reference back to `EvidenceIntelligence`, `EvidenceAnalysisRequest`, or `EvidenceAnalysisResult` (Contract Design §12, "holds no reference back to `EvidenceIntelligence`");
- any runtime conversation component;
- any reporting mechanism;
- Docling, or any structured-document-conversion capability.

**If a future provider adapter requires technical process dependencies** (a subprocess handle, a temporary-file library, a native-binding layer), those remain strictly implementation-local to that adapter and must never migrate authority into the OCR mechanism's own contract — the contract this document freezes remains exactly as described above regardless of what any single concrete adapter eventually requires internally.

---

## 14. Provider Neutrality

- **No concrete OCR provider is selected by this document, or by the Contract Design it implements.**
- **OCRmyPDF, Tesseract, PaddleOCR, EasyOCR, or any other provider is not authorised by name** — none is chosen, evaluated, ruled in, or ruled out.
- **Provider replacement must not require constitutional redesign** if the replacement satisfies the same contract this document freezes — exactly the same substitutability property `ReasoningProvider` already has (Constitution: "Replaceable reasoning providers... Parker's authority, safety guarantees, and behavioral contracts do not depend on which provider is plugged in").
- **Provider-specific types must not leak into Parker's public contracts**, mirroring exactly the already-established, structurally-enforced discipline that keeps `org.apache.tika.*` types confined to a single adapter file in the Evidence Processing programme (Evidence Processing Searchable PDF Boundary Clarification §3) — the same discipline applies here, in principle, to whichever provider a future implementation eventually selects.

---

## 15. Security and Resource Limits

Frozen at the constitutional minimum; no deployment topology, container boundary, or process model is chosen by this document — that remains future implementation work (§18, below), consistent with the OCR Planning Review's own §3.8 finding that this risk category exists but is unresolved:

- **No network access, unless a future governance stage expressly authorises it** — consistent with the Constitution's own "local-first by default" principle and the OCR Planning Review's own finding that native OCR tooling introduces a materially different risk surface than the pure-JVM Apache Tika integration Evidence Processing already uses.
- **Bounded CPU, memory, disk, and time** — a direct application of the Constitution's own Constitutional Test 7 ("If compromised, what is its maximum blast radius? ... never unbounded").
- **A controlled temporary workspace**, where any implementation requires one — the OCR Planning Review's own §3.8 finding names "temporary-file handling" as an identified, unresolved risk.
- **Path traversal and command injection are prohibited outright** — named explicitly by the OCR Planning Review §3.8 as a risk category native OCR tooling introduces that Evidence Processing's own governance has never had to address.
- **Temporary outputs must be cleaned up**, according to rules a future implementation defines — not designed here.
- **Hostile or malformed documents must not compromise Parker's runtime, or original evidence, under any condition** — the same "bounded blast radius" and "original evidence immutability" (§7, above) guarantees apply without exception to adversarial input, not only to well-formed input.

---

## 16. Explicit Exclusions

Every item below is `OUT OF SCOPE` for the OCR mechanism's own contract, with the reason stated directly:

| Excluded capability | Reason |
| --- | --- |
| Provider selection | Contract Design §11, §13, "Out of Scope"; §14, above — no engine is chosen anywhere in this governance stage |
| OCRmyPDF implementation | Same — named only as an explicit non-example |
| Tesseract implementation | Same — named only as an explicit non-example |
| Docling | Contract Design "Out of Scope"; §13 — a structured-document-conversion capability, unrelated to and not authorised by this contract |
| Structured document model | Not named, authorised, or implied anywhere in the Contract Design |
| Reporting | Contract Design "Out of Scope"; §6, above — the OCR mechanism's output is never a final report |
| Runtime composition | Contract Design §11, "Out of Scope" — `ParkerRuntime` wiring is explicitly deferred |
| Background queues | §4, above — the OCR mechanism may not start background work |
| Automatic conversation invocation | §4, above — the OCR mechanism may not attach to the ordinary owner-conversation submission path or any other communication-layer entry point |
| Memory writes | §3, §13, above — no dependency on `MemoryCore`'s write interface |
| Knowledge promotion | §3, §13, above — no dependency on Knowledge Memory's submission interface |
| Evidence acceptance | §6, §8, above — the OCR mechanism never itself accepts anything into custody |
| Truth determination | §3, §7, above — no truth authority of any kind |
| Output-quality policy | §11, above — identified, not created |
| Rejected-output permission semantics | §11, above — identified, not created |

---

## 17. Constitutional Self-Certification

A direct self-check against every named category, performed once here rather than asserted without demonstration:

- **Owner control.** Preserved in full (§12, above) — no invocation, human- or machine-triggered, occurs outside the Permission Engine's existing authority, and the future trigger model is explicitly, not silently, deferred.
- **Parser non-authority.** Preserved — the OCR mechanism never possesses authority over truth, is never a constitutional classifier, and never assigns `EvidentialState` (§3, above; Amendment 2's own table row, unmodified).
- **Original-evidence immutability.** Preserved without exception (§7, above; CDR-006's Constitutional Optimisation Safeguard, naming OCR explicitly).
- **Evidence Custodian ownership.** Preserved — no dependency on `EvidenceCustodian.accept`, at any depth (§3, §13, above); any accepted OCR-produced artefact remains Evidence Custodian's own, exactly as Amendment 2 already fixed.
- **Evidence Processing ownership.** Preserved — the OCR mechanism never directly consumes `RequiresOcr` (§4, above); detection of whether OCR is required remains entirely, and exclusively, Evidence Processing's own, already-governed responsibility, untouched by this document.
- **Evidence Intelligence ownership.** Preserved and made executable — the OCR mechanism is invoked only through an authorised Evidence Intelligence orchestration path (§4, above), consistent with CDR-007's own classification of OCR execution as Evidence Intelligence's analytical function.
- **Provenance.** Preserved — no new provenance type, no new field; every fact traced to Memory Core's existing, unmodified contract (§9, above).
- **No peer subsystem creation.** Confirmed — the OCR mechanism remains capability-tier, within the existing Evidence Intelligence subsystem, never a subsystem of its own (§3, above; Amendment 2's own constitutional-tier reasoning, unmodified).
- **No public-contract expansion.** Confirmed — no new interface, public type, or acceptance disposition is introduced anywhere in this document; every shape is described in prose only (§6, above).
- **No implementation pre-authorisation.** Confirmed — no provider, Kotlin name, runtime path, or deployment topology is chosen anywhere in this document (§14, §15, §16, above).

---

## 18. Deferred to Future Governance and Implementation

Restated once, in full, as the single list a future Implementation Plan and future OCR Mechanism Scope Lock revision (should one prove necessary) must treat as still open:

- machine-triggered invocation and its owner-control model (§4, §12, above);
- the composition-level coordinator that consumes `RequiresOcr` (§4, above);
- whether a dedicated Permission Engine proposal class is required for OCR invocation specifically (§12, above);
- output-quality validation policy and threshold (§11, above);
- permission gating and disposition for rejected output (§11, above);
- concrete provider identity and adapter design (§14, above);
- process/execution and deployment topology (§15, above);
- exact Kotlin names, method signatures, and file layout (Contract Design; this document).

---

## Final Recommendation

This Scope Lock is **Draft**, pending independent constitutional review — it is not yet Accepted, Canonical, or Frozen, and authorises no implementation on its own. It converts the OCR Mechanism Contract Design's already-authorised responsibilities, shape, failure model, and constitutional constraints into fourteen numbered implementation boundaries, without redesigning, reopening, or reinterpreting the Contract Design, Amendment 2, CDR-006, or CDR-007. Independent constitutional review of this document is the required next step before it may become binding; an Implementation Plan is not authorised to begin until that review is complete and this Scope Lock's own status changes from Draft to Accepted.

OCR MECHANISM SCOPE LOCK — DRAFT — AWAITING INDEPENDENT CONSTITUTIONAL REVIEW

Confirmed: no Kotlin implemented; no API, schema, or storage technology defined; no concrete OCR engine named; CDR-006, CDR-007, the Evidence Intelligence Contract Design, the Evidence Intelligence Scope Lock, Amendment 2, the OCR Mechanism Contract Design, and the Evidence Processing Boundary Clarification/Scope Lock not modified; nothing staged; nothing committed; nothing pushed; OCR Mechanism Implementation Plan not started.
