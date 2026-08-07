**Status:** Narrow governance clarification only, resolving the single deferred prerequisite question `docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_CONTRACT_DESIGN.md` Section 19, item 2 identified. No Kotlin is implemented, proposed as a diff, or changed by this document. Neither `src/` nor `tests/` is touched. This document does not amend `docs/architecture/10-permission-engine.md` ("Chapter 10"), `docs/architecture/08-resource-registry.md` ("Chapter 8"), `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md` ("Errata 004"), `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`, `docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md` ("the Unit 9 Permission Clarification"), `docs/specifications/volume-03-core-interfaces/PermissionPolicy.md`, or any other frozen or draft governance document — it reads them, cites them, and reasons from them. It does not begin Scope Lock, an Implementation Plan, or any implementation work. Nothing is staged, committed, or pushed.

# Trust Framework — Memory Retrieval Architecture — Gap #54 — Policy Rule Collision Clarification

---

## 1. Governing Context

Read fresh for this document, in full or in every relevant section: `docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_CONTRACT_DESIGN.md` (the Contract Design, in full, treated as prior evidence and independently re-verified below, not assumed correct); `docs/reviews/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_CONTRACT_DESIGN_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` and its own Defect Confirmation Review (evidentiary record only); `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md` §7, §9; `docs/architecture/08-resource-registry.md`; `docs/architecture/10-permission-engine.md` §5, §9 (draft, not yet frozen — cited per the same tiering discipline the Contract Design already established); `docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md` (Adopted — §7, §11 read exactly); `docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md` (searched, not merely re-cited); `docs/specifications/volume-03-core-interfaces/PermissionPolicy.md` (in full — §3, §9 read exactly); `docs/decisions/CDR-005_CONSTITUTIONAL_ADMISSION_OF_PERMISSIONENGINE_PROPOSAL_CLASSES.md` (Model C, Decision Rules). Production code read fresh, in full: `src/runtime/DefaultPermissionPolicy.kt`, `src/runtime/ActionMapper.kt`, `src/contracts/Resource.kt`, `src/contracts/Permission.kt`, `src/runtime/DefaultKnowledgeRetrieval.kt`, `src/composition/PermissionFilteredMemoryRetrieval.kt`, `src/composition/ParkerRuntime.kt`.

---

## 2. The Collision, Stated Precisely

`PermissionPolicyRule` (`src/runtime/DefaultPermissionPolicy.kt`) is keyed only by `(PermissionAction, ResourceType)`. `DefaultPermissionPolicy.evaluate` derives this pair from `ActionMapper.map`'s resolved `ActionResourceMapping` values and, in `ruleOutcomeFor`, discards the originating proposed-action string entirely — `rules.find { it.action == mapping.action && it.resourceType == mapping.resourceType }` never inspects which verb phrase produced `mapping`.

Two independently-registered, semantically distinct verb phrases already map to the identical `(READ, MEMORY)` pair in the live composed runtime:

- `knowledge.retrieve` (`DefaultKnowledgeRetrieval.RETRIEVE_ACTION_NAME`) — registered in `ActionVocabulary` with `mappings = setOf(ActionResourceMapping(READ, MEMORY))` (`ParkerRuntime.kt`), and its own `KNOWLEDGE_RETRIEVAL_RESOURCE_ID` registered in `ResourceRegistry` with `resourceType = MEMORY`.
- `memory.retrieve` (`PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME`) — deliberately **not** registered in `ActionVocabulary` today (`ParkerRuntime.kt`'s own comment: "deliberately left unregistered... Errata 004 Section 7"), and structurally incapable of resolving any target Resource (`targetResources` is always `emptyList()`), so it currently cannot reach `ruleOutcomeFor` at all — it fails earlier, at `ActionMapper.mapOne`'s `RESOURCE_TYPE_MISMATCH` path (Gap #54's own root cause).

Once Gap #54's own resolution-mechanism gap (Contract Design Section 19, item 1 — Candidate D or an equivalent) is separately closed, `memory.retrieve` would, by Errata 004 §7's own frozen table ("Retrieval of any other Memory Core record | READ | MEMORY"), also resolve to `(READ, MEMORY)`. At that point both verb phrases collapse to the same rule key, and the single, already-`APPROVED` `PermissionPolicyRule(READ, MEMORY, APPROVED, AUTOMATIC)` — registered for Knowledge Retrieval's own purpose — would silently also approve `memory.retrieve`, converting `EvidenceIntelligenceInputResolver`'s own currently-correct, deliberately fail-closed retrieval into an approved one, as an unintended side effect of fixing an unrelated caller. **This is the collision this document resolves.**

---

## 3. Candidate Analysis

### 3.1 Distinct `ResourceType` for Knowledge Retrieval — rejected on fresh evidence

The Contract Design's own Independent Constitutional Review (Section 5) proposed giving `DefaultKnowledgeRetrieval`'s registered Resource a `ResourceType` distinct from `MEMORY`, avoiding the collision with no change to `DefaultPermissionPolicy`'s matching algorithm, and confirmed this would not reopen `docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md` (which the ICR checked and correctly found silent on `ResourceType`).

**This audit checked a second, more directly relevant document the ICR did not: `docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md`** — the Adopted Clarification `DefaultKnowledgeRetrieval`'s own KDoc actually cites for its resource/action identity (Section 7 of that document: "a single, fixed resource identity representing the Knowledge Retrieval boundary itself"). That document's own Section 11, "Explicit Non-Expansions," states plainly it does not authorise "any new `PermissionAction` or `ResourceType` value, or any second resource/action pair beyond the single one Section 7 names."

`ResourceType` (`src/contracts/Resource.kt`) has fourteen existing values: `MEMORY, WORLD_MODEL, DOCUMENT, EMAIL, CALENDAR, CONTACT, HOME_ASSISTANT_ENTITY, ANDROID_CAPABILITY, TOOL, PLUGIN, AGENT, SECRET, CONFIGURATION, AUDIT_LOG`. This candidate has two sub-variants, both foreclosed, for different reasons:

- **Minting a new `ResourceType` value.** The Unit 9 Permission Clarification's own Section 11 explicitly does not authorise "any new `PermissionAction` or `ResourceType` value." Foreclosed directly by that already-Adopted, frozen text.
- **Reusing an existing value other than `MEMORY`** (for example, `WORLD_MODEL`) — literally permitted by Section 11's text, since it introduces no *new* value, but foreclosed on a different, independent ground: none of the thirteen remaining values represents Knowledge Memory's own promoted-evidence boundary. `WORLD_MODEL` is the nearest-sounding neighbour, but is independently confirmed, from `DefaultKnowledgeRetrieval.kt`'s own KDoc, to denote a genuinely different, already-governed subsystem — "`DefaultWorldModelUpdatePolicy`... governs a genuinely different, legitimately age-based epistemic model (belief transience, per ADR-024), never this Unit's own evidence-status-change condition." Registering Knowledge Retrieval's own Resource under `WORLD_MODEL` would misclassify it in the Resource Registry's own "authoritative catalogue of every protected object" (Chapter 8) and against `Resource.md`'s own named "Resource Categories" — not choose an inelegant label, but assert something false about what the resource actually is, purely to obtain a mechanically different policy-rule key. This is the same defect in substance as minting a new value under false pretences, and squarely the pattern this task's own governing instructions forbid: "Do not select a solution simply because it requires fewer code changes." No other existing value is even a plausible candidate on semantic grounds. Nor can the same manoeuvre be applied to Memory Core's own side instead: `(READ, MEMORY)` for `memory.retrieve` is itself fixed by Errata 004 §7's own frozen table, not a value this document, or any future Scope Lock building on it, may reassign.

**Rejected, in both sub-variants.** Not because either costs more code — because the first would require amending a frozen, Adopted governance document (the Unit 9 Permission Clarification), and the second would misclassify a Resource Registry entry to obtain a policy-mechanism side effect, substituting resource-identity integrity for implementation convenience. Neither is something this document does.

### 3.2 Policy keyed by verb phrase directly / a second, more specific matching tier — evaluated together, as one mechanism shape

Both bullets the Contract Design's Section 19 item 2 lists reduce to the same underlying mechanism: let `DefaultPermissionPolicy` distinguish rules by the specific proposed-action string, not only by the coarser `(PermissionAction, ResourceType)` pair it maps to, with a verb-phrase-specific rule taking precedence over a coarser rule addressing the same pair when both exist.

**The data this requires already exists and is already computed — it is discarded, not absent.** `ActionMapper.mapOne` already returns `ActionMappingResult.Resolved(proposedAction, applicable)`, carrying the originating verb phrase alongside its resolved mappings. `DefaultPermissionPolicy.evaluate`'s own `resolvedMappings = ... .flatMap { it.mappings }` is what discards the `proposedAction` field a moment after `ActionMapper` produces it. No change to `ActionMapper`, `ActionVocabulary`, `ResourceRegistry`, `Resource`, `Permission`, or any enum is required — only to how `DefaultPermissionPolicy` uses data it already receives.

- **Compatibility with Constitution / Trust Framework chapters:** Chapter 10 §9's own "may never construct, implement, or substitute a second authority in the Permission Engine's place" is not implicated — this stays within the single `DefaultPermissionPolicy`/`DefaultPermissionEngine` instance `ParkerRuntime.kt` already constructs once; no second evaluator, no second `PermissionEngine`.
- **Compatibility with Errata 004:** §7's own frozen `(action, resourceType)` table is unchanged — `memory.retrieve` still names `(READ, MEMORY)`; this mechanism only lets policy additionally condition on *which verb phrase* produced that pair, a dimension Errata 004 never addressed either way (it fixed the action-name-to-pair mapping, not whether two action names sharing a pair must receive identical policy treatment).
- **Compatibility with `PermissionPolicy.md`:** §3's own "Policy Inputs" already names "Requested Action — What the Principal is proposing to do" as a conceptual input distinct from "Target Resources," and states inputs "correspond to data already carried by `ExecutionRequest`" — `ExecutionRequest.proposedActions` (the verb-phrase strings) already is that data; using it at finer grain is not a new input. §9 "Policy Extensibility" explicitly anticipates the concrete policy model evolving "without changing `PermissionEngine`'s public contract," precisely the property this mechanism preserves (only `DefaultPermissionPolicy`'s internal `PermissionPolicyRule` shape changes, never `PermissionEngine.evaluate`'s or `PermissionEngine.explain`'s signature). This is a refinement of an already-authorised input, not the Role-Based/Attribute-Based/Capability-Based extensibility §9 separately names as a distinct, larger future step — this document does not claim or require any of those.
- **Impact on `DefaultPermissionPolicy`:** `PermissionPolicyRule` gains an additional, optional discriminator (a specific verb phrase, rather than every verb phrase mapping to a given pair); `ruleOutcomeFor`'s matching logic prefers a verb-phrase-specific rule over a coarser one addressing the same `(action, resourceType)` pair when both exist. Confined entirely to this one class.
- **Impact on `ActionMapper`:** none — `mapOne`/`map` are unchanged; `DefaultPermissionPolicy` simply stops discarding `proposedAction` from the result it already receives.
- **Impact on `ResourceRegistry`:** none.
- **Impact on existing Evidence Intelligence retrieval:** protective, not disruptive — once Gap #54's own resolution-mechanism gap is separately closed (Section 19 item 1), a verb-phrase-specific rule can keep `memory.retrieve` distinguishable from `knowledge.retrieve` at the identical `(READ, MEMORY)` pair, so `EvidenceIntelligenceInputResolver`'s own fail-closed guarantee need not be "re-implemented at a different layer" (Contract Design Section 19 item 2's fourth, least-preferred bullet becomes unnecessary).
- **Impact on Knowledge Retrieval:** none required — its own existing coarse `(READ, MEMORY)` rule continues to govern it unchanged unless a future policy-content decision chooses to also give it an explicit verb-phrase-specific rule; this document does not require that.
- **Impact on Memory Retrieval:** enables, for the first time, a policy-content decision to give `memory.retrieve` (and `memory.retrieve_document`) an outcome independent of Knowledge Retrieval's own — without deciding here what that outcome should be (see Section 5, Non-Decisions).
- **Migration cost:** low — additive to one class; every existing rule (no verb-phrase discriminator set) continues to match exactly as it does today, since "no discriminator" naturally means "matches any verb phrase reaching this pair," preserving every current caller's behaviour unchanged until a new, more specific rule is deliberately added.
- **Fail-closed behaviour:** unchanged — an unresolvable action or resource still denies exactly as today; this mechanism only changes which already-resolved rule a request matches against, never the "no rule matches → DENIED" default (`PermissionPolicy.md` §4: "DENIED is the default when no rule matches").
- **Risk of widening access:** none from the mechanism itself — adding the capability to distinguish rules widens nothing by itself; only a future, separate policy-content decision to add an `APPROVED` rule for `memory.retrieve` would widen access, and that decision is explicitly not made here.
- **Risk of creating a second permission model:** low — this is an internal refinement of the single existing table-driven model (`DefaultPermissionPolicy`'s own KDoc: "the smallest possible policy shape... a flat, fixed lookup table"), not a second model; the lookup remains flat and deterministic, only its key becomes finer-grained for rules that choose to use it.
- **Whether frozen governance requires amendment:** no. This mechanism operates entirely within data and extensibility already named or anticipated by `PermissionPolicy.md` §3/§9, and does not touch Errata 004's own frozen mapping table, the Unit 9 Permission Clarification's own frozen resource/action identity, or `PermissionEngine`'s interface.

**Selected.** See Section 4.

### 3.3 Any already-authorised Trust Framework mechanism differentiating these acts without altering Memory Core boundaries

Checked directly for an existing, already-lawful discriminator other than the verb phrase itself: `ExecutionRequest.intent` is the only other per-request string both `DefaultKnowledgeRetrieval` and `PermissionFilteredMemoryRetrieval` already set to a distinct, descriptive value. It is not a lawful policy-matching key: `ActionMapper`'s own KDoc states "action-mapping.md deliberately leaves natural-language-to-vocabulary matching to the Planner/Chapter 20 (out of scope here)" — `intent` is free-text, non-normalised, and not the governed vocabulary surface; keying policy on it would reintroduce exactly the natural-language matching problem the Action Vocabulary was built to avoid. No other already-differentiated, already-lawful field was found. This confirms Section 3.2's mechanism (the verb phrase itself, via the Action Vocabulary already in place) is the correct, and only, already-authorised discriminator available.

### 3.4 Other candidates

None emerged from this audit beyond the three analysed above and the ones the Contract Design's own Section 19 item 2 already named (verb-phrase keying and the "second, more specific matching tier" being the same mechanism under two names, both covered by Section 3.2; "an explicit, disclosed acceptance that Evidence Intelligence's own guarantee must move to a different layer," the fourth, least-preferred bullet, is rendered unnecessary by Section 3.2's selection and is not separately pursued). No mechanism is invented here beyond what Section 3.2 already, minimally, describes.

---

## 4. Selected Resolution

**`DefaultPermissionPolicy`'s own rule model should be extended so a `PermissionPolicyRule` may optionally name the specific proposed-action verb phrase it governs, in addition to the `(PermissionAction, ResourceType)` pair it already requires; a rule naming a specific verb phrase takes precedence, for a request carrying that verb phrase, over a coarser rule addressing the same pair without one.** This uses `ActionMappingResult.Resolved`'s own already-produced `proposedAction` field, currently discarded by `DefaultPermissionPolicy.evaluate` a moment after `ActionMapper` returns it — no new dependency, no new enum value, no change to `ActionMapper`, `ResourceRegistry`, `ExecutionRequest`, or `PermissionEngine`'s interface.

This is a **mechanism** selection only. It resolves *how* `memory.retrieve` and `knowledge.retrieve` can lawfully receive independent policy outcomes at the identical `(READ, MEMORY)` pair. It does **not** select, freeze, or imply what those independent outcomes should actually be — see Section 5.

General-purpose confirmation: this mechanism is keyed on the verb phrase itself (an Action Vocabulary concept already governing every caller identically), never on caller identity, principal, or module — any future pair of distinct verb phrases sharing a `(PermissionAction, ResourceType)` pair, for any subsystem, could use the identical mechanism. It contains no `if caller == X` shape anywhere.

---

## 5. Rejected Alternatives

- **Distinct `ResourceType` for Knowledge Retrieval (Section 3.1)** — rejected in both sub-variants: minting a new value is foreclosed by `docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md` §11 (Adopted, frozen); reusing an existing, different value (for example, `WORLD_MODEL`) is literally permitted by §11's text but would misclassify the Resource Registry entry against Chapter 8's own "authoritative catalogue" framing and `Resource.md`'s own named Resource Categories, since no existing value other than `MEMORY` represents Knowledge Memory's own promoted-evidence boundary. Reassigning Memory Core's own side is separately foreclosed by Errata 004 §7's own frozen table. This reverses the Contract Design's own Independent Constitutional Review recommendation (Section 5 of that review) — that review checked `PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md` and correctly found it silent on `ResourceType`, but did not check the Unit 9 Permission Clarification, the document `DefaultKnowledgeRetrieval` actually derives its own resource/action identity from. This document's own correction supersedes that recommendation; see Section 7 (Non-Decisions) for the required update to the Contract Design.
- **`ExecutionRequest.intent` as a policy-matching discriminator (Section 3.3)** — rejected: `intent` is free-text, outside the governed Action Vocabulary surface, and using it for policy matching would reintroduce the natural-language-matching problem `action-mapping.md` deliberately assigns elsewhere (Chapter 20/Planner), not to `ActionMapper` or `DefaultPermissionPolicy`.
- **Coarsening `PermissionFilteredMemoryRetrieval`'s own per-record evaluation granularity to avoid the collision by evaluating less often** — not separately re-analysed here; already, correctly, rejected by the Contract Design (Section 18, fourth bullet) as a violation of Errata 004 §9's own frozen per-record granularity requirement, and nothing in this audit changes that conclusion.
- **Leaving the collision unresolved and instead re-implementing Evidence Intelligence's own fail-closed guarantee at a different layer** (Contract Design Section 19 item 2's fourth, least-preferred bullet) — not selected: Section 3.2's mechanism makes this unnecessary, and this document's own task explicitly asks for the narrowest lawful resolution, not a workaround at a different architectural layer.

---

## 6. Required Follow-On Changes

1. **A Scope Lock and Implementation Plan for the `DefaultPermissionPolicy`/`PermissionPolicyRule` extension itself** (Section 4) — not authorised or begun by this document, per this task's own explicit stop condition.
2. **A separate, still-open policy-content decision: what outcome should `memory.retrieve`/`memory.retrieve_document` actually receive, once independently distinguishable from `knowledge.retrieve`.** This document deliberately does not decide this (Section 7). At minimum, it must be decided consistently with Errata 004 §9's own per-record granularity and with `EvidenceIntelligenceInputResolver`'s own currently-correct fail-closed behaviour, which no governing document has yet authorised changing.
3. **Resolution of Contract Design Section 19, item 1** — the exact mechanism by which Candidate D derives `(action, resourceType)` at all when `targetResources` is empty by a caller's own structural design. This document's own selection (Section 4) presupposes that item 1 is separately resolved; it does not resolve item 1 itself, and the two remain independent prerequisite questions.
4. **A correction to `docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_CONTRACT_DESIGN.md` Section 19 item 2 and Section 21**, replacing the "distinct `ResourceType`" candidate's standing as "the cheapest candidate, worth checking first" with a citation to this document's own Section 3.1 finding and Section 4 selection — a narrow, disclosed update to a document this task does not otherwise reopen or amend in substance. (Applying this correction is itself a follow-on action, not performed by this narrow Clarification, which is scoped to the collision question alone; noted here as required, not undertaken here.)
5. **CDR-005 self-certification** remains not required for this mechanism itself, consistent with the Contract Design's own Section 19 item 4 reasoning: `memory.retrieve` and `knowledge.retrieve` are both already-existing, already-classified proposal classes (both already reach `PermissionEngine.evaluate`); this document adds no new proposal class, only a finer-grained matching capability within the existing, single policy mechanism.

---

## 7. Non-Decisions (Explicit)

This document does **not** decide:

- What outcome (`APPROVED`/`APPROVED_WITH_CONFIRMATION`/`DEFERRED`/`DENIED`) `memory.retrieve` or `memory.retrieve_document` should receive once distinguishable from `knowledge.retrieve` — a policy-content decision, not a mechanism decision, and one this task's own scope ("resolve the single deferred prerequisite question," a mechanism question) does not ask this document to make.
- Whether `EvidenceIntelligenceInputResolver`'s own currently fail-closed retrieval should ever become permissive — untouched, and not addressed here, consistent with the Contract Design's own Section 14.
- The exact Kotlin shape of the `PermissionPolicyRule` extension (a new field's type, a new class, an ordered-list-of-tiers representation, or another equivalent shape) — deferred to the Scope Lock/Implementation Plan named in Section 6, item 1.
- Item 1 of the Contract Design's own Section 19 (Candidate D's own resolution-derivation mechanism) — a distinct, still-open prerequisite question this document does not attempt to resolve.

---

## 8. Readiness for Scope Lock

**The main Gap #54 Contract Design may not yet proceed to Scope Lock.** This document resolves one of the two prerequisite questions the Contract Design's own Section 19 identified (item 2, the policy-rule collision) — but Section 19's own item 1 (the exact representation of Candidate D's own resolution-derivation mechanism, i.e., how `(action, resourceType)` is derived at all when `targetResources` is empty by design) remains open and is not addressed by this document. The Contract Design's own Section 17 ("Selected Architecture") explicitly declined to finalise pending resolution of *both* items; only one is resolved here. A future, similarly narrow pass addressing item 1 — followed by a Contract Design correction consolidating both resolutions into a completed Section 17 — is the next required governance step before a Scope Lock for Gap #54 itself may lawfully begin.

---

## 9. Independent Constitutional Review Self-Check

Performed by the author before requesting external review, per this repository's own established discipline — a genuine external Independent Constitutional Review follows as a separate document.

- **Fail-closed semantics preserved?** Yes — Section 4's mechanism changes only rule-matching granularity; the "no rule matches → DENIED" default (`PermissionPolicy.md` §4) is untouched, and no rule content is added or changed by this document.
- **Ambient authority introduced?** No — the discriminator is the verb phrase already carried on `ExecutionRequest.proposedActions`, not an implicit or inferred signal.
- **Second authorization system invented?** No — confined to the single, existing `DefaultPermissionPolicy`/`DefaultPermissionEngine` instance; `PermissionEngine`'s interface is unchanged.
- **Caller-specific exceptions present?** No — keyed on verb phrase (an Action Vocabulary concept), never on caller, principal, or module identity.
- **Does this reopen frozen governance?** The selected mechanism (Section 3.2/4): no. The rejected "distinct `ResourceType`" candidate (Section 3.1): would have, which is precisely why it is rejected here rather than selected.
- **Is Gap #54 Contract Design now ready for Scope Lock?** No — Section 8, above, states this explicitly; item 1 remains open.

```
TRUST FRAMEWORK MEMORY RETRIEVAL POLICY RULE COLLISION CLARIFICATION — DRAFT COMPLETE, PENDING INDEPENDENT CONSTITUTIONAL REVIEW
```
