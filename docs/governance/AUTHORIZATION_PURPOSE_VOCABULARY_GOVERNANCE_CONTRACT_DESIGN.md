**Status:** Governance and architecture only. No Kotlin is implemented, proposed as a diff, or changed by this document. Neither `src/` nor `tests/` is touched. This document does not amend `docs/architecture/action-mapping.md`, `docs/architecture/10-permission-engine.md` ("Chapter 10"), `docs/architecture/15-plugin-sdk.md` ("Chapter 15"), `docs/adr/ADR-019-core-schema-versioning.md`, `docs/governance/TRUST_FRAMEWORK_AUTHORIZATION_CONTEXT_CONTRACT_DESIGN.md`, `docs/architecture/TRUST_FRAMEWORK_AUTHORIZATION_PURPOSE_PROGRAMME.md`, `docs/governance/AUTHORIZATION_PURPOSE_CARRIER_CONTRACT_DESIGN.md` ("Unit 1," Adopted), or any other frozen or draft governance document — it reads them, cites them, and reasons from them. It does not authorise a Scope Lock, an Implementation Plan, or any implementation work. Nothing is staged, committed, or pushed.

# Trust Framework — Authorization Purpose — Unit 2 — Vocabulary Governance Contract Design

Programme: **Trust Framework Authorization Purpose Programme, Unit 2 (Vocabulary Governance).**

---

## 1. Governing Context

Read fresh: `docs/governance/AUTHORIZATION_PURPOSE_CARRIER_CONTRACT_DESIGN.md` (Unit 1, Adopted — establishes that Authorization Purpose lives on `ExecutionRequest` itself as a small, closed-value-typed field; not reopened here); `docs/governance/TRUST_FRAMEWORK_AUTHORIZATION_CONTEXT_CONTRACT_DESIGN.md` (Adopted); `docs/architecture/TRUST_FRAMEWORK_AUTHORIZATION_PURPOSE_PROGRAMME.md` (names this Unit as its own second step); `docs/architecture/action-mapping.md` (in full — "Transformation Rules," "Plugin Supplied Actions," "Future Extensibility"); `docs/architecture/10-permission-engine.md` §10 ("Extensibility"); `docs/architecture/15-plugin-sdk.md` (Chapter 15, in full); `docs/decisions/CDR-005_CONSTITUTIONAL_ADMISSION_OF_PERMISSIONENGINE_PROPOSAL_CLASSES.md`; `docs/adr/ADR-019-core-schema-versioning.md`; every `docs/specifications/volume-02-core-schemas/*.md` file's own "Breaking changes require a schema version update and ADR (ADR-019)" clause (grepped, confirmed repeated verbatim across all of them); `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md` §'s own breaking-change definition. Production code read fresh, in full or in relevant part: `src/contracts/ActionMapping.kt`, `src/runtime/ActionMapper.kt` (`InMemoryActionVocabulary.register`), `src/contracts/Module.kt`, `src/contracts/Permission.kt`, `src/contracts/Resource.kt`; every already-registered Action Vocabulary verb phrase in `src/`, enumerated by direct grep (Section 3, below).

---

## 2. Purpose

Determine how the closed Authorization Purpose vocabulary is governed: who may define a value, where it is registered, its own mutability/versioning/deprecation/retirement rules, how plugin-declared values are handled, how collisions and naming are governed, its relationship to Action Vocabulary, whether individual values are constitutional or implementation identifiers, what counts as a breaking change, and how future subsystems participate. This is Unit 2 of the Authorization Purpose Programme; it does not reopen Unit 1's own carrier selection and does not authorise building anything.

---

## 3. Existing Architecture — The Closest Governed Precedent, Audited Fresh

Action Vocabulary is the closest already-governed precedent (Unit 1 §12/§15's own "parallel, not overlapping" relationship), audited here on its own terms, not assumed to be a clean model in every respect:

- **Registration mechanism.** `InMemoryActionVocabulary.register(entry: ActionVocabularyEntry)`: a verb phrase not yet registered succeeds; re-registering an *identical* entry is an idempotent no-op; registering a *different* mapping set under an already-used verb phrase is rejected outright. **There is no `update`, `deprecate`, or `unregister` method of any kind** — Action Vocabulary has never had to answer "how is a retired entry handled," because nothing has ever been retired.
- **Naming, audited for actual consistency, not assumed uniform.** Grepped every registered action name in `src/`: the dominant pattern is `<domain>.<verb>` (`evidence.accept`, `evidence.retrieve`, `evidence.extract`, `evidence.delete`, `memory.retrieve`, `memory.retrieve_document`, `knowledge.submit`, `knowledge.retrieve`, `evidence-intelligence.analyse`, `evidence-intelligence.accept-memory-core-candidate`) — but it is **not mechanically enforced and is not uniform in practice**: `MemoryAdmissionCoordinator.CREATE_CONVERSATIONAL_MEMORY_ACTION_NAME` is `"create conversational memory record"` — a plain-English phrase with spaces, no domain prefix, no verb-first structure, breaking the convention entirely, and independently confirmed live-registered in `ParkerRuntime.kt`. (A second, weaker candidate example — `EvidenceRegistrationCoordinator`'s hyphenated `"memory.create-provenance"` versus `PermissionGatedMemoryCore`'s underscored `"memory.create_provenance"` — was checked and is **not** a live collision: `PermissionGatedMemoryCore` is confirmed dormant, never constructed in `ParkerRuntime.kt`'s own composition root, so its own constant is never actually passed to `vocabulary.register()`. It remains an in-source inconsistency worth noting, but not evidence of two simultaneously-registered, differently-styled entries.) **This is an honest finding, not a rhetorical one**: Action Vocabulary's own naming discipline is a norm every registrant has been expected to follow, not a mechanically validated constraint — and at least one already-written, currently-uncommitted, live-registered entry does not follow it.
- **Plugin authorship.** `action-mapping.md`'s own "Plugin Supplied Actions": a Plugin "MAY register new action vocabulary entries as part of its Tool registration, scoped to its own declared `supportedActions`/`supportedResourceTypes`"; "MUST NOT register a vocabulary entry that maps to a `PermissionAction` outside what its own Principal could ever be granted"; entries "are namespaced by the Plugin's identity (e.g. `plugin:<pluginId>:run irrigation cycle`) to prevent collision... and to make audit trails immediately show which Plugin introduced a given mapping." Chapter 15, read in full (five lines): "Plugins are guests. The platform remains the owner." "Installation does not imply permission."
- **Versioning — an open question even for Action Vocabulary itself, not an established pattern.** `action-mapping.md`'s own "Transformation Rules" asserts "the action vocabulary is versioned and extensible" but its own "Future Extensibility" section immediately discloses this is aspirational: "Whether the action vocabulary itself should become a first-class, versioned schema artifact under `docs/schemas/`... Recommended for a future ADR once an initial vocabulary table is actually drafted." **Action Vocabulary's own versioning story is unresolved**, not a precedent this document can simply copy.
- **A separate, genuinely established repository-wide precedent — breaking changes, distinct from Action Vocabulary.** `docs/adr/ADR-019-core-schema-versioning.md`: "All canonical schemas MUST be versioned... Every schema document must define compatibility expectations and future migration considerations." Every `docs/specifications/volume-02-core-schemas/*.md` file repeats: "Breaking changes require a schema version update and ADR (ADR-019)." Separately, `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md`: "A genuinely breaking change (a field renamed, removed, or given a new mandatory meaning) requires its own dedicated governance review — an amendment to this document — accepted *before* any migration code is written." **This is the more directly applicable precedent for "what constitutes a breaking change,"** since it is a general, already-applied repository discipline, not scoped only to JSON-Schema-tier artifacts.
- **Retirement-without-deletion — an established repository-wide philosophy, from Knowledge Memory.** Contract Design V2 §3 (cited throughout the Knowledge Memory governance chain): retirement "never implies deletion... remain retained and retrievable"; a retired item is excluded from ordinary results by default but never destroyed, and its own history remains fully traceable. No equivalent exists yet for Action Vocabulary, but this is the closest already-governed answer to "how are retired [governed identifiers] handled" this repository has ever adopted.
- **CDR-005's own threshold.** "A CDR is required: whenever a domain's self-certification against Chapter 10's criteria is genuinely contested, ambiguous, or would require choosing between two or more constitutionally plausible readings" — for *newly introduced acts*, not for routine registration of an already-classified vocabulary entry.

---

## 4. Constitutional Requirements

Restated, not invented: single Permission Engine/Policy (Chapter 10 §5); fail-closed default for an unregistered value (`PermissionPolicy.md` §4/§7, Unit 1 §4); no ambient authority (explicit declaration only, Unit 1 §3); no caller-specific exceptions (a registration mechanism available identically to every domain and, subject to Section 8 below, every Plugin); "installation does not imply permission" (Chapter 15); domains define their own acts, escalating only genuinely contested classifications to a CDR (Chapter 10 §10, CDR-005).

---

## 5. Who Defines Authorization Purposes

**Domains define their own values, mirroring Chapter 10 §10's own "domains define their own acts" principle exactly.** Knowledge Memory defines its own Authorization Purpose values for its own internal consumers (candidate evaluation, revision evaluation); Evidence Intelligence defines its own; a future Conversational Retrieval unit defines its own — each within its own governing Contract Design/Scope Lock, the same tier of document that already defines that domain's own resource/action identity today (for example, the Programme 3 Unit 9 Permission Enforcement Mechanism Clarification already does exactly this for Knowledge Retrieval's own resource/action pair). **No central committee pre-approves every future value**; the discipline is distributed, exactly as Action Vocabulary's own registration already is. Chapter 10 itself is not reopened by any individual domain's own registration (Chapter 10 §10: reopened "only when its own constitutional content changes... never merely because a new domain act, cleanly fitting existing criteria, is being recognised").

---

## 6. Where They Are Registered

**A registration mechanism analogous to, but distinct from, `ActionVocabulary`'s own** — mirroring Unit 1 §15's own "parallel, not overlapping" relationship. Values are registered once, at composition time (`ParkerRuntime.kt`, the same stage `ActionVocabulary`/`ResourceRegistry` entries are already registered), by whichever domain's own governance authorises a given value. This document does not fix the exact Kotlin registration API (Scope-Lock-tier, per Unit 1's own identical deferral discipline) — only that registration, like `ActionVocabulary.register`, must be **additive and reject-on-conflict**, never silently overwriting an existing value with a different meaning.

---

## 7. Immutability

**Once registered, a specific Authorization Purpose value's own meaning is immutable** — mirroring `InMemoryActionVocabulary.register`'s own established behaviour (a conflicting re-registration under the same identifier is rejected, not applied). Changing what an already-registered value *means* is a breaking change (Section 15) requiring its own dedicated governance review before any change is made — it is never a silent edit. This is distinct from the value's own *lifecycle status* (Section 8) changing, which does not alter meaning.

---

## 8. Can They Be Deprecated?

**Yes, in the sense of being marked no longer eligible for new registration or new policy-rule authorship — but this is a status this document introduces, not one Action Vocabulary's own precedent already answers (Section 3: no entry has ever been deprecated).** A deprecated value: (a) remains valid for any already-approved historical `PermissionDecision` referencing it (auditability is never retroactively broken); (b) may not be the basis of any *new* `PermissionPolicyRule`; (c) is not deleted from the registry. This mirrors Contract Design V2 §3's own "retirement never implies deletion" philosophy (Section 3, above), the closest already-governed analogue this repository has for the general shape of the question, applied here for the first time to a governed vocabulary rather than to a Knowledge Item.

---

## 9. Can They Be Versioned?

**Not automatically, and not by inheriting ADR-019 wholesale — that ADR governs `docs/schemas/`-tier canonical schemas specifically, and Authorization Purpose's own vocabulary is not (yet, and not necessarily ever) one of those, exactly as Action Vocabulary's own identical, still-open "should this become a first-class, versioned schema artifact" question (Section 3) has never been resolved either.** What this document does settle, independent of that open question: **individual values are not versioned; the vocabulary's own registration discipline is what changes over time.** A genuinely different meaning for what was conceptually "the same" purpose is not a version bump of one identifier — it is a **new, distinctly-named value**, with the old one deprecated (Section 8), mirroring how this repository already treats a breaking schema change elsewhere (a new field/type, not a silent redefinition of an old one — Memory Core Durability Contract Design, Section 3). Whether the vocabulary as a whole should eventually become a formal, ADR-019-governed schema artifact is left open, exactly as it already is for Action Vocabulary — not decided by this Unit.

---

## 10. Can Plugins Define Them?

**Yes, under the identical constraints `action-mapping.md`'s own "Plugin Supplied Actions" already establishes for Action Vocabulary, extended here by direct analogy:**

- A Plugin may register its own Authorization Purpose value(s), scoped to its own declared capabilities, as part of its own Tool/Module registration.
- A Plugin's own registered value must be namespaced by its own Plugin identity (mirroring `plugin:<pluginId>:<name>`), never a bare or domain-prefixed name that could be mistaken for a core-platform value.
- **Installation does not imply permission (Chapter 15) applies at this layer identically to every other**: registering an Authorization Purpose value grants nothing by itself; every actual invocation is still independently evaluated by `PermissionEngine.evaluate`, exactly as `ModulePermissionRequirement`'s own KDoc already establishes for a Module's declared permission requirements generally (Unit 1 §4b, citing `src/contracts/Module.kt`).
- A Plugin may not register a value that would be matched by a `PermissionPolicyRule` granting authority its own Principal could never hold — the identical ceiling `action-mapping.md` already states for Plugin-supplied actions.

---

## 11. How Are Collisions Prevented?

**Two layers, mirroring Action Vocabulary's own precedent and extending it where that precedent is silent:** (1) **namespacing** — every value is prefixed by its own owning domain or Plugin identity (Section 12), making accidental collision between unrelated registrants structurally unlikely; (2) **reject-on-conflict registration** (Section 6/7) — a registration attempt naming an identifier already registered with a *different* meaning fails outright, never silently overwrites, mirroring `InMemoryActionVocabulary.register`'s own established behaviour exactly. Unlike Action Vocabulary's own precedent (which has never needed a cross-domain uniqueness discipline beyond this), this document additionally recommends — as a governance norm for whichever future Scope Lock builds the registry, not a mechanism this document designs — that domain-prefix uniqueness itself be checked at registration time, since Section 3's own naming-consistency finding shows convention alone has already, once, failed to hold.

---

## 12. How Are Names Structured?

**A `<domain>.<purpose>` dotted-namespace convention, for core-platform values, and `plugin:<pluginId>:<purpose>` for Plugin-supplied ones** — adopting Action Vocabulary's own dominant (if not perfectly enforced) pattern deliberately, not by default. Given Section 3's own honest finding that this convention is **not currently mechanically validated** for Action Vocabulary, and has already been violated once, this document recommends — without designing the mechanism — that Authorization Purpose's own eventual registration function should **validate the naming structure at registration time**, rather than repeat Action Vocabulary's own convention-only discipline. This is a recommendation for a future Scope Lock, not a requirement this Contract Design enforces on itself.

---

## 13. Relationship to Action Vocabulary

**Parallel and orthogonal, restated from Unit 1 §12/§15, not reopened:** Action Vocabulary continues to answer "what kind of act"; Authorization Purpose answers "for what governed reason." Their own **governance disciplines are analogous but administratively separate** — each domain registers its own entries in each, at the same composition stage, but registering an Action Vocabulary entry does not itself register, require, or imply any particular Authorization Purpose value, and vice versa. A domain may reuse an already-existing Action Vocabulary verb phrase (e.g., `memory.retrieve`) while declaring a *new* Authorization Purpose value for its own distinct use of it — this is precisely the situation the Resolution Derivation Mechanism Clarification's own Section 7 finding, and the entire reason this Programme exists, already establishes as the normal, expected case, not an edge case.

---

## 14. Constitutional Identifiers or Implementation Identifiers?

**The dimension is constitutional; individual values within it are implementation identifiers — mirroring exactly the same altitude distinction Action Vocabulary already embodies.** `PermissionAction`/`ResourceType` (`src/contracts/Permission.kt`/`Resource.kt`) are genuinely constitutional identifiers: closed Kotlin enums, changeable only by amending frozen contracts, cited directly in Chapter 10's own admission criteria. Action Vocabulary *verb phrases*, by contrast, are implementation identifiers: strings, registered at runtime, governed by a registration *process* (Section 3), never individually enshrined in frozen governance text — Errata 004 §7's own table merely *cites* already-chosen strings, it does not constitute their own authority to exist. **Authorization Purpose follows the verb-phrase pattern, not the enum pattern**: the *existence of the Authorization Purpose dimension itself* is constitutional (fixed by the Adopted Authorization Context Contract Design); a *specific value* such as `"knowledge-memory.candidate-evaluation"` is an implementation identifier, introduced and retired through the ordinary registration discipline this document describes, never requiring a Constitutional Decision Record of its own unless Section 3's own CDR-005 threshold (genuine contest) is actually met.

---

## 15. What Constitutes a Breaking Change?

**Adapting the Memory Core Durability Contract Design's own already-established definition (Section 3, above) to this vocabulary directly:** a breaking change is (a) renaming an already-registered value, (b) removing an already-registered value outright (as opposed to deprecating it, Section 8), or (c) giving an already-registered value a new mandatory meaning (changing which acts it legitimately classifies, or which policy outcomes attach to it). **Adding a new value is never breaking** — mirroring `InMemoryActionVocabulary.register`'s own additive-only semantics. A breaking change requires its own dedicated governance review, accepted before any change is made — the same discipline already established for Memory Core Durability, applied here for the first time to this vocabulary.

---

## 16. How Are Future Subsystems Added?

**No different from how any domain participates today (Section 5).** A future subsystem's own governing Contract Design/Scope Lock defines and registers its own Authorization Purpose value(s), the same way Programme 3 Unit 9 already defined Knowledge Retrieval's own resource/action pair — no separate, centralized "vocabulary maintenance" process exists or is created by this document. This directly satisfies the Authorization Purpose Programme's own "no bespoke mechanism per subsystem" goal (Programme §6).

---

## 17. How Are Retired Purposes Handled?

Restated precisely from Section 8, since the two questions (deprecation, retirement) are the same lifecycle event under two names in this task's own list: **never deleted; marked ineligible for new policy authorship; every already-recorded `PermissionDecision` that referenced the value while it was active remains valid, auditable, and unaffected.** This is the Contract Design V2 §3 "retirement never implies deletion" philosophy, applied here for the first time to a Trust Framework vocabulary rather than a Knowledge Item — an extension by analogy, disclosed as such, not a pre-existing rule this document merely restates.

---

## 18. Risks

- **Risk: naming-convention drift repeats** (Section 3's own already-observed `"create conversational memory record"` violation) **if Authorization Purpose relies on convention alone.** **Mitigation:** Section 12's own recommendation that a future Scope Lock mechanically validate naming structure at registration time, not merely document it.
- **Risk: "deprecated" is implemented as "deleted," breaking historical audit trails.** **Mitigation:** Section 8/17 state explicitly that deletion is never lawful; only new-authorship ineligibility is.
- **Risk: a domain treats its own routine value registration as requiring full CDR-005 escalation, over-bureaucratising ordinary vocabulary growth.** **Mitigation:** Section 3/14 state the CDR-005 threshold precisely — genuine contest only, mirroring the identical, already-settled threshold for Action Vocabulary and for the Memory Retrieval governance chain's own two Adopted Clarifications, both of which independently confirmed CDR-005 was not required for their own mechanism-level work.
- **Risk: this document is read as resolving Action Vocabulary's own still-open versioning/schema-artifact question as a side effect.** **Mitigation:** Section 3 and Section 9 both state explicitly that this remains open for Action Vocabulary, unchanged, and that Authorization Purpose's own analogous question is *also* left open here, not silently resolved by implication.

---

## 19. Recommendation

Proceed to Unit 3 (Permission Policy Extension) and Unit 4 (Existing Consumer Retrofit) informed by this Unit's own governance answers, before any Scope Lock is drafted for the vocabulary's own concrete registration mechanism. A future Scope Lock should treat Section 12's own naming-validation recommendation and Section 6's own reject-on-conflict requirement as binding constraints on its own design, not optional refinements.

---

## 20. Independent Constitutional Review Self-Check

- **Domain-owned, not centrally gatekept?** Yes (Section 5, 16) — mirrors Chapter 10 §10 exactly.
- **No caller-specific exception?** Yes — the registration mechanism is uniformly available to every domain and, under stated constraints, every Plugin (Section 10).
- **Fail-closed preserved?** Yes — an unregistered or deprecated-for-new-authorship value cannot ground a new rule (Section 8).
- **No ambient/ungoverned naming?** Addressed honestly — Section 3 discloses Action Vocabulary's own naming discipline is not currently mechanically enforced, and Section 12 recommends closing that gap for Authorization Purpose rather than repeating it.
- **Constitutional/implementation-identifier boundary drawn, not assumed?** Yes (Section 14), grounded directly in the existing `PermissionAction`/`ResourceType`-vs-verb-phrase distinction already present in the codebase.
- **Breaking-change definition sourced, not invented?** Yes (Section 15), adapted from the Memory Core Durability Contract Design's own already-accepted text.
- **Implementation details frozen prematurely?** No — the exact registration API, the exact deprecation flag shape, and whether the vocabulary ever becomes an ADR-019 schema artifact are all left open.

```
AUTHORIZATION PURPOSE VOCABULARY GOVERNANCE CONTRACT DESIGN — DRAFT COMPLETE, PENDING INDEPENDENT CONSTITUTIONAL REVIEW
```
