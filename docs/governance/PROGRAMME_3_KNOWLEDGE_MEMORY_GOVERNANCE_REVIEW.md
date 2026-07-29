**Status:** Governance — Programme 3, opening review. Architectural only. No API is designed, no Kotlin is proposed, and no implementation detail is specified anywhere in this document. Neither `src/` nor `tests/` is touched. Nothing is staged, committed, or pushed.

# Programme 3 — Knowledge Memory Governance Review

Programme: **Programme 3 — Knowledge Memory Governance Review.**

**Normative inputs, frozen, not redefined:** `docs/architecture/parker-constitution.md`, `docs/architecture/user-authorship-and-evidence.md`, `docs/architecture/reasoning-context.md`, `docs/architecture/epistemic-integrity.md`, `docs/architecture/MEMORY_CORE_GOVERNANCE_REVIEW.md`, `docs/architecture/MEMORY_ARCHITECTURE_RECONCILIATION.md`, `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md`, `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`, `docs/implementation/MEMORY_CORE_IMPLEMENTATION_PLAN.md`, `docs/reviews/EPISTEMIC_INTEGRITY_EXECUTABLE_TEST_COMPLIANCE_AUDIT.md`, `docs/reviews/EPISTEMIC_INTEGRITY_CONSTITUTIONAL_REMEDIATION_PLAN.md`. This review does not reopen the Reconciliation's own already-decided architecture — Candidate A (Memory Core beneath Knowledge Memory), the corrected layering diagram, the canonical terminology, the single Trust boundary, or the Provenance-as-separate-record decision are all treated here as settled fact, restated where load-bearing, never re-derived. This review's own contribution is narrower: applying that already-settled architecture specifically to the constitutional questions Epistemic Integrity and the Compliance Audit raise, which the Reconciliation — written before ratification — did not address.

This document begins Programme 3. It does not complete it. It is the governance review that must exist before a Knowledge Memory Contract Design is written, exactly as `MEMORY_CORE_GOVERNANCE_REVIEW.md` preceded `MEMORY_CORE_CONTRACT_DESIGN.md` for Programme 2.

---

## 1. Purpose of Knowledge Memory

**Responsibilities.** Knowledge Memory is the evaluation-and-promotion layer of Parker's Memory family. It decides whether a candidate fact, drawn from Memory Core content, is worth retaining as durable, retrievable knowledge, and it is the layer `reasoning-context.md` already means when it says Memory is "responsible for durable storage and retrieval of long-term knowledge, and for exposing only the portions relevant to a given task." Concretely, Knowledge Memory: evaluates submissions against promotion factors (repetition, importance, relevance, frequency, confidence, explicit request — unchanged from today's `MemoryPromotionPolicy`); holds the record of what has actually been promoted; and answers the query a reasoning task actually needs answered — "what does Parker already know that is relevant here" — as a task-scoped read, not a raw dump of everything Memory Core has ever recorded.

**Boundaries.** Knowledge Memory does not identify, does not mint identity, and does not record provenance. It does not decide what *is* — that is Memory Core's exclusive responsibility (Reconciliation §5, restating `MEMORY_RUNTIME_ARCHITECTURE.md` §1: "Memory stores knowledge. Memory never decides. Memory never acts."). Knowledge Memory decides what *matters enough to keep*. This is a narrower, later, and different judgment than Memory Core's own recording act, and the two must never be collapsed into one (Reconciliation §4, Candidate D, rejected for exactly this reason).

**Constitutional role.** Knowledge Memory is the layer at which Article VI (Evidential Sufficiency) and Article VII (Burden of Justification) first become operative. Memory Core records evidence without judging it; Knowledge Memory is where a judgment — "is this sufficiently supported to be retained as knowledge" — is first made, and is therefore the layer that must carry that judgment honestly: disclosing its basis, never treating promotion itself as proof, and never discarding a proposition merely because it was not promoted. A candidate that fails promotion is not thereby false; Knowledge Memory's silence about it is not itself a representation, constitutionally or otherwise.

---

## 2. Relationship with Memory Core

**What remains in Memory Core, unconditionally:** identity (Entity, Document, Evidence Item identifiers), Provenance (as its own record type, mandatorily referenced — Reconciliation §11), Assertion and Relationship records, the frozen lifecycle (ACTIVE/DISPUTED/SUPERSEDED/ARCHIVED/DELETED), and the single Trust boundary that gates every write and every sensitive read for the whole Memory family (Reconciliation §12). None of this is duplicated, re-implemented, or shadowed inside Knowledge Memory.

**What belongs in Knowledge Memory:** the promotion decision itself and its own record of that decision (today's `CandidateMemory` → `MemoryPromotionPolicy` → `MemoryRecord` shape, renamed but not redesigned per Reconciliation §13); the evidential-state classification a promoted item is given for reasoning purposes (Section 3, below); and the task-scoped retrieval surface reasoning actually reads from.

**Avoiding duplication.** Knowledge Memory never re-records what Memory Core already owns. A promoted item does not carry its own copy of provenance — it references the Memory Core record(s) it was promoted from, exactly as `CandidateMemory.correlationId` already references external state today by identifier rather than by copy (Reconciliation §8). Where Memory Core later amends, disputes, or supersedes a record Knowledge Memory has promoted from, that change is visible to Knowledge Memory through the reference, not through a second, independently maintained copy that could drift out of sync — a second copy would itself be exactly the duplicate source of truth this Programme's own constraints prohibit.

---

## 3. Relationship with Reasoning Context

Reasoning Context does not read Memory Core directly. It reads Knowledge Memory, Conversation History, and the World Model as three sibling, read-only projections (Reconciliation §6, §9) — unchanged from the layering the Reconciliation already fixed and unchanged by anything Epistemic Integrity adds.

**How reasoning retrieves knowledge:** a reasoning task's assembly step asks Knowledge Memory for the subset of promoted knowledge relevant to that task, exactly as `DefaultReasoningContextAssembler` asks `MemorySource.recall` today. Knowledge Memory answers from what it has promoted, and, where a promoted item's provenance or evidential state is material to how it may be represented, that state travels with it into the assembled Reasoning Context — not as a separate lookup a reasoning provider must remember to perform, but as part of what Knowledge Memory hands over.

**Where epistemic state lives.** The Article IV fourteen-state evidential taxonomy belongs to Knowledge Memory, not to Memory Core. Memory Core records raw evidential facts — what was submitted, by whom, when, with what content nature — without classifying a proposition's evidential state; that classification is an evaluative judgment about what a body of Memory Core evidence supports, which is precisely Knowledge Memory's own kind of work, not Memory Core's (Section 1, above). This mirrors the Remediation Plan's own assignment of the taxonomy to Programme 3 (Remediation Plan §6).

**Where provenance lives:** Memory Core, exclusively, per Reconciliation §11 — unchanged, and not revisited by this review.

**Where relationships live:** Memory Core, exclusively. `Relationship` (including `SUPPORTS`/`CONTRADICTS`/`DISPUTES`/`AMENDS`/`SUPERSEDES`) is a Memory-Core-owned record type (`MEMORY_CORE_CONTRACT_DESIGN.md`, `MEMORY_CORE_SCOPE_LOCK.md` §2 Objective 7). Knowledge Memory reads relationships that already exist in Memory Core when deciding whether, and how, to promote a disputed or superseded item; it never mints a Relationship of its own, mirroring the same non-duplication rule as provenance.

**Where retrieval belongs.** Two retrieval surfaces exist, at two layers, for two different purposes. Memory Core's own structural retrieval (the seven frozen modes, `MEMORY_CORE_SCOPE_LOCK.md` §10) remains the correct surface for provenance-level drill-down — audits, dispute investigation, "why does Parker believe this." Knowledge Memory's own retrieval surface is the correct, and only, surface for task-scoped reasoning assembly — it is what Reasoning Context actually reads, and it returns evaluated, promoted knowledge, never raw, unevaluated Memory Core content. A reasoning provider should never need to query Memory Core directly to do its job; if one ever appears to need this, that is a signal the Knowledge Memory retrieval surface is incomplete, not a justification for bypassing the layering (Reconciliation §6's own diagram forbids Reasoning Context from reading Memory Core directly).

---

## 4. Migration from MemoryStore

Restated from Reconciliation §8, §14, applied specifically against this review's four migration questions:

**Disappears entirely:** nothing. The Reconciliation found no capability in today's `MemoryStore` that should be discarded — its write path (`remember`/`forget`) has zero production callers today, but that reflects the store being structurally unused in the live system, not that its capability is unwanted; the capability is retained under Knowledge Memory.

**Migrates unchanged:** the promotion/evaluation judgment itself (today's `MemoryPromotionPolicy` factors: repetition, user importance, goal relevance, frequency, confidence, explicit request) and the existing test suite describing that behaviour, which the Reconciliation already confirms remains valid, unmodified, as Knowledge Memory's own test suite once the rename is carried out (Reconciliation §14).

**Becomes Knowledge Memory:** the entire flat store as it exists today (`MemoryStore`/`InMemoryMemoryStore`/`CandidateMemory`/`MemoryRecord`/`MemoryPromotionPolicy`/`MemoryCategory`/`MemorySource`), renamed per the Reconciliation's already-recommended terminology, with its submission path adapted, additively, to reference real Memory Core Provenance and Entity/Document/Evidence records instead of carrying its own flat, duplicate fields (Reconciliation §8, §14).

**Becomes Memory Core's responsibility, where it was never truly Knowledge Memory's to begin with:** identity assignment, provenance capture and enforcement, and the permission boundary. Today's `MemoryStore` has no identity model, no provenance model, and no permission check at all (Reconciliation §2) — these are not migrating *from* Knowledge Memory, they are being supplied *underneath* it for the first time, by Memory Core, which already exists and already enforces them.

---

## 5. Retrieval Architecture

Described at the level of flow, not mechanism — no interface, method, or data shape is proposed here.

**Retrieval flow.** A reasoning task's context-assembly step requests relevant knowledge from Knowledge Memory, scoped to the task. Knowledge Memory answers from its own promoted records, each of which references (never copies) the Memory Core evidence it was promoted from. Where a task's reasoning also needs provenance-level detail beyond what Knowledge Memory surfaces by default — for example, to answer a user's own question about why Parker believes something — that detail is fetched from Memory Core's own retrieval surface, addressed through the reference Knowledge Memory already holds, not re-derived.

**Permission flow.** Unchanged from the single Trust boundary the Reconciliation already fixed (§12): every Memory Core write, amendment, dispute, and sensitive read passes through `PermissionEngine.evaluate` once, at Memory Core's own boundary. Because Knowledge Memory is structurally downstream of Memory Core, this single boundary already covers everything Knowledge Memory could ever promote or later read — Knowledge Memory introduces no second, independent permission check, and must not, per the same reasoning that already rejected scattering permission logic across multiple Memory-family layers (Reconciliation §12).

**Provenance flow.** Provenance is attached once, at the point Memory Core records evidence, and never re-attached, duplicated, or re-derived at the Knowledge Memory layer. When Knowledge Memory promotes an item, the promoted record carries a reference to that same, single Provenance record — not a snapshot, not a copy, and not a summary of it. Any later amendment Memory Core makes to that Provenance (for example, disclosing a previously unknown creation time) is visible through the reference to every promoted Knowledge Memory record that cites it, without Knowledge Memory itself needing to change anything.

**Epistemic flow.** A proposition's evidential-state classification (Section 3) is computed at promotion time, from the Memory Core evidence available at that moment, and is re-evaluated only when new Memory Core evidence bearing on the same proposition subsequently arrives — never spontaneously, never merely with the passage of time, and never silently. This mirrors Article XVI's own revision-without-erasure requirement: a re-evaluation produces a new classification alongside the record of what the classification previously was, not a silent overwrite.

---

## 6. Knowledge Lifecycle

Described architecturally; no state machine, field, or transition name is specified.

**Creation.** A candidate becomes knowledge only through Knowledge Memory's own promotion evaluation, applied to Memory Core evidence that already exists with its own provenance and identity. Promotion is never a caller-facing operation — a caller submits a candidate; whether it is promoted is Knowledge Memory's own internal decision, unchanged from today's architecture (Reconciliation §12). Creation at this layer never fabricates evidence; it only decides whether already-recorded evidence is worth retaining as durable knowledge, consistent with `user-authorship-and-evidence.md`'s own principle that Parker never invents evidence.

**Revision.** New Memory Core evidence bearing on an already-promoted item triggers re-evaluation, not silent replacement. Consistent with Article XVI and Article XVII, a revision produces an updated understanding while preserving what was previously understood and when — Knowledge Memory must be able to show both what it now holds and what it held before, not merely the current state.

**Supersession.** When Memory Core marks an underlying Assertion `SUPERSEDED`, the Knowledge Memory record promoted from it is re-evaluated against the superseding evidence, not silently carried forward unchanged. The superseded promotion is retained, not deleted — mirroring Memory Core's own non-erasing lifecycle exactly, rather than inventing a separate retention policy at the Knowledge Memory layer.

**Contradiction.** Where Memory Core holds a genuine, unresolved `CONTRADICTS`/`DISPUTES` relationship between two Assertions, Knowledge Memory must not force a single promoted "winner" merely because one candidate arrived first, was submitted more often, or scored marginally higher on a promotion factor — this is the same defect the Compliance Audit found in the World Model's single-belief collapse (CT-EI-05/24), and Knowledge Memory's promotion policy must not reintroduce it at a different layer. Where evidence genuinely does not justify exclusivity, Knowledge Memory's honest answer is that the matter is unresolved, not a confident promotion of one side.

**Retirement.** A promoted item that no longer merits durable retention (for example, because its underlying Memory Core evidence was withdrawn via the owner-requested deletion path) is retired, not deleted from Knowledge Memory's own record — Knowledge Memory's retirement marks that a promotion has been withdrawn; it does not erase the historical fact that the promotion once existed, except in the one case Memory Core's own lifecycle already reserves exclusively for owner-requested erasure.

**Historical preservation.** No operation at the Knowledge Memory layer may overwrite a promoted record's history in place. Every correction, revision, or re-evaluation is a new, linked act, exactly mirroring Memory Core's own inherited correction-not-destruction principle (Reconciliation §8, §11) — Knowledge Memory does not get to relax a discipline Memory Core already enforces one layer below it.

---

## 7. Relationship with World Model

**Determination, restated and preserved exactly as the Reconciliation and the World Model's own architecture already require (Reconciliation §10): Knowledge Memory and the World Model remain fully independent, in both directions, permanently.** Knowledge Memory does not feed the World Model, and the World Model does not feed Knowledge Memory. `WorldObservation`'s own approved source list explicitly excludes Memory as a legitimate World Model input; this exclusion is not revisited, weakened, or narrowed by this review.

**What Knowledge Memory knows versus what World Model believes.** Knowledge Memory knows what Parker has deliberately learned and chosen to retain — durable, evaluated, promoted, and slow to change, because promotion is itself a considered act. The World Model believes what is currently true, right now, from live, sourced, transient signals — fast to change, never promoted, never a permanent record. A user's home address, once told to Parker and promoted, is Knowledge Memory; whether the front door is currently locked is World Model. The two answer different questions and must never be merged to answer either one — exactly the distinction `reasoning-context.md` already draws between "what Parker has learned" and "what Parker believes is true right now."

**Where they meet.** Only downstream, at Reasoning Context, as two sibling read projections into one assembled working set — never as one feeding the other, and never through Memory Core.

---

## 8. Relationship with Document Intelligence

No Document Intelligence capability exists in the codebase today; OCR, PDF parsing, file import, and image analysis are explicitly excluded from Memory Core's current scope and deferred to a future Document Handling Programme (`MEMORY_CORE_SCOPE_LOCK.md` §4). This review does not bring that capability forward, and Knowledge Memory must not attempt to build a special-case document-handling path in its absence.

**How extracted documents become constitutional knowledge, once Document Intelligence exists:** identically to any other source of evidence. A future Document Intelligence capability registers extracted content as Memory Core evidence — with Provenance correctly disclosing that the content is `EXTRACTED` (or `SUMMARISED`, `INFERRED`, as appropriate) rather than `ORIGINAL`, per Article IX and per `ContentNature`'s already-existing distinction. Once that evidence exists in Memory Core with honest provenance, it becomes eligible for Knowledge Memory's ordinary promotion evaluation, exactly like evidence from any other origin. Knowledge Memory has, and should have, no document-specific promotion logic, no separate document-promotion path, and no awareness of *how* a piece of evidence was acquired beyond what its Provenance record already discloses — this is precisely what keeps Knowledge Memory from becoming a Document Store, which this Programme's own constraints expressly prohibit.

---

## 9. Relationship with Representation

Responses do not obtain evidence from Knowledge Memory directly. They obtain it exactly as `reasoning-context.md` already specifies: a reasoning provider reasons only over the Reasoning Context assembled for its task, which already contains whatever Knowledge Memory (and Conversation History, and the World Model) contributed. A reasoning provider has no standing access to Knowledge Memory outside that assembled context, and no ability to write back into it.

Knowledge Memory's own constitutional obligation stops at making promoted knowledge, its evidential-state classification, and (by reference) its provenance honestly available to Reasoning Context assembly. Whether and how that state is ultimately disclosed to a user in a drafted response — provenance disclosure, uncertainty disclosure, assumption disclosure — is the Representation Engine's own future responsibility (Remediation Plan Programme 7, its own Section 10), not Knowledge Memory's. Knowledge Memory must not itself compose user-facing text, and must not silently drop evidential-state information on the assumption that a downstream layer will supply it — if the information is not present at the Reasoning Context boundary, no later layer can honestly disclose it.

---

## 10. Programme Boundaries

**Programme 3 (Knowledge Memory) owns:** the `MemoryStore` → Knowledge Memory rename and its adaptation to reference real Memory Core records (Reconciliation §14, §16 step 4); the Article IV evidential-state taxonomy's definition and its housing at the Knowledge Memory layer (Section 3, above); the promotion policy's own handling of contradiction and supersession consistent with Article IV/VII/XVI (Section 6); and the design of Knowledge Memory's own task-scoped retrieval surface. Programme 3 does not implement Reasoning Context's own consumption of that surface — it delivers a surface Programme 4 can consume.

**Programme 4 (Reasoning Context) owns:** wiring `DefaultReasoningContextAssembler` onto Knowledge Memory's new retrieval surface in place of the legacy `MemoryStore`/`MemorySource` path (Remediation Plan §9, Stage 3); Article V propositional integrity; and Article VII's burden-of-justification *computation* over what Knowledge Memory presents (Knowledge Memory classifies evidential state at promotion time; Reasoning Context's own burden-of-justification work operates on that classification during a specific task, a separate and later act).

**Programme 5 (World Model) owns:** the World Model's own conflict remediation (Compliance Audit's four findings) entirely independently of Programme 3. No Programme 3 deliverable depends on, blocks, or is blocked by Programme 5 — the two families remain architecturally parallel exactly as Section 7 describes, and this independence should hold at the Programme-sequencing level, not only at the runtime-architecture level.

**Programme 6 (Document Intelligence) owns:** all document extraction, transformation, and disclosure capability. Programme 3 must not pre-build document-specific promotion logic in anticipation of Programme 6 — per Section 8, Knowledge Memory's promotion path already accommodates future document-sourced evidence without modification, once it exists, because Knowledge Memory promotes from Memory Core evidence generically, never from a document-typed special case.

**Scope creep this review explicitly excludes from Programme 3:** any redesign of Memory Core's own contracts, permission boundary, lifecycle, or event scope (all frozen by `MEMORY_CORE_SCOPE_LOCK.md` and not reopened here); any World Model integration in either direction (Section 7); any Representation-layer work (Section 9); any negative-evidence reasoning capability (a Programme 4 concern per the Remediation Plan, not a Knowledge Memory promotion-policy concern).

---

## 11. Risks

- **Architectural risk:** a rename performed without the accompanying adapter work would leave Knowledge Memory looking complete while still carrying its own flat, duplicate provenance-shaped fields — precisely the duplicate-source-of-truth outcome this Programme exists to prevent. The rename and the adapter must land together, or the rename must not be presented as meaningful progress on its own.
- **Constitutional risk:** if Knowledge Memory's promotion policy applies Article VI/VII sufficiency judgments inconsistently — promoting some propositions on weaker grounds than others without disclosing why — Parker would present a false uniformity of confidence across its "known" facts. The promotion policy's evidential-state classification (Section 3) must be applied by the same standard to every candidate, and any exception must be disclosed, not silent.
- **Migration risk:** today's `MemoryStore` write path has zero production callers, but its read path (`MemorySource.recall`, feeding `DefaultReasoningContextAssembler`) is genuinely production-wired. A cutover error at the read boundary directly breaks live reasoning, unlike a write-path change, which currently affects nothing running. The read-path cutover is the highest-risk single act in this Programme and should be the most carefully sequenced and tested.
- **Performance risk:** introducing Knowledge Memory as a genuine intermediate layer between Memory Core and Reasoning Context adds one more indirection to a path that currently runs in-process and in-memory. Nothing in this review requires that to change, and it should not — any design that introduces network calls, external services, or asynchronous boundaries into this path would be a new architectural decision this review does not authorize.
- **Testing risk:** the Reconciliation asserts that today's `MemoryStore` test suite remains valid, unmodified, as Knowledge Memory's own suite after renaming (Reconciliation §14). That assertion must be verified, not assumed, once the rename and adapter work actually occur — a rename that changes behaviour incidentally (not merely names) would silently invalidate tests that currently pass for the right reasons.

---

## 12. Recommendations

**Recommended implementation order**, continuing directly from where the Reconciliation's own sequencing (§16) left off, now that Memory Core (steps 1–3 of that sequencing) exists:

1. This governance review, accepted as Programme 3's own opening architecture.
2. A Knowledge Memory Contract Design, scoped narrowly to: the rename itself (vocabulary and, later, Kotlin identifiers); the reference-not-copy adaptation of the promotion path onto real Memory Core Provenance/Entity/Evidence records; and the definition of the Article IV evidential-state taxonomy this review assigns to Knowledge Memory's own layer (Section 3). This Contract Design should explicitly inherit, not re-derive, Memory Core's provenance shape, relationship types, and permission boundary.
3. A Knowledge Memory Scope Lock, mirroring `MEMORY_CORE_SCOPE_LOCK.md`'s own discipline — fixing exactly what Version 1 of Knowledge Memory builds, with the same burden-of-proof-favours-exclusion principle, so that Document Intelligence-, Representation-, and World-Model-adjacent capability is not quietly pulled forward into Programme 3 (Section 10, above).
4. A Knowledge Memory Implementation Plan, sequencing the rename and adapter work as independently compilable, independently testable units, exactly as `MEMORY_CORE_IMPLEMENTATION_PLAN.md` already models for Memory Core.
5. Implementation: rename and adapter first, verified against the existing (unmodified) test suite per Reconciliation §14; only once that adapter is proven behaviourally equivalent should Reasoning Context's own cutover (Programme 4, Section 10 above) begin — the two must not be conflated into one migration step, given the read-path risk Section 11 identifies.
6. A re-run of the Executable Test Compliance Audit's own methodology against the relevant CTs (principally the Memory Core PARTIALLY ENFORCED group and the reasoning-pipeline disconnection finding) once Programme 3's own work lands, to confirm the remediation the Constitutional Remediation Plan scheduled here has actually closed what it was scheduled to close.

This review finds no defect in the already-ratified architecture it builds upon, and identifies no reason Programme 3 cannot proceed on the basis above.

```
READY FOR KNOWLEDGE MEMORY CONTRACT DESIGN
```
