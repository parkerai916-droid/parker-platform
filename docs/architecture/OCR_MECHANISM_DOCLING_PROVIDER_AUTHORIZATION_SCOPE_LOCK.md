# OCR Mechanism — Docling Concrete Provider Authorization Scope Lock

## Status

**Draft for owner review. Not yet accepted, canonical, or
implementation-authorising.** This document performs exactly, and
only, the narrow act `docs/architecture/DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md`
§6 (adopted, commit `84cc061`) predicted would eventually be required:
*"a separate future extension of OCR Mechanism governance to cover
Docling at all."* It does not select a provider — Docling is already
the adopted, demonstrated OCR/re-recognition mechanism (§3, below),
and this document treats that selection as given, not open for
re-evaluation. It does not implement anything: no Kotlin is written,
proposed as a diff, or changed; no dependency is added; no model or
runtime is installed or provisioned; no `ParkerRuntime` wiring occurs.
Neither `src/` nor `tests/` is touched. Nothing is staged, committed,
or pushed.

**This document reopens, redesigns, or reinterprets none of:**
`docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md` ("the Contract
Design"), `docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md` ("the Scope
Lock"), `docs/architecture/OCR_MECHANISM_UNIT_12_RUNTIME_INVOCATION_SCOPE_LOCK.md`
("the Unit 12 Scope Lock"), `docs/architecture/SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`,
`docs/architecture/DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md`,
`docs/architecture/DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md`,
CDR-006, CDR-007, CDR-008, or `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`.
It amends none of them. The Scope Lock's own §13 already, explicitly,
anticipates exactly the act this document performs — *"if a future
provider adapter requires technical process dependencies... those
remain strictly implementation-local to that adapter and must never
migrate authority into the OCR mechanism's own contract"* — and this
document exercises that already-provisioned option for the first time,
rather than reopening either document's own already-frozen "Docling —
Out of Scope" line, which remains true and unchanged (§4, below).

## 1. Purpose

Determine whether, and on what exact bounded terms, Docling may become
the authorized concrete provider behind the already-frozen
`OcrProviderAdapter`/`OcrMechanism` architecture. This document:

1. Confirms, by fresh, independent re-inspection, that Docling is
   already the adopted, demonstrated OCR/re-recognition mechanism
   (§3).
2. Confirms the exact prior authorization gap: adopted governance
   identifies Docling but has never extended OCR Mechanism authority
   to cover it (§4).
3. Freezes the narrow, bounded authority a future concrete
   `DoclingOcrProviderAdapter` implementation must operate within
   (§5-§6).
4. Distinguishes precisely which of provider authorization, runtime/
   model/cache provisioning, concrete adapter implementation, Unit 12
   runtime composition, and Document Ingestion Tier B routing this
   document authorizes, and which remain separately governed (§7-§8).
5. Determines whether `OCR_MECHANISM_UNIT_12_IMPLEMENTATION_PLAN.md`'s
   own accuracy defect may be corrected in this turn (§13).

This document does not decide, and must not be read as deciding:
Docling's own runtime/model/cache provisioning mechanics; the concrete
`DoclingOcrProviderAdapter` implementation itself; Unit 12's own
composition wiring (already separately, previously authorized); or any
Document Ingestion Tier B routing/invocation work.

## 2. Authoritative sources inspected fresh (this document)

**Newly re-read in full for this document, not assumed from any prior
turn's own report:**

- `docs/architecture/SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` (full, 277
  lines) — §5 "OCR provenance": *"Tesseract remains unevaluated
  because it was not installed; no quality conclusion is authorised.
  Docling is the demonstrated OCR mechanism, subject to its recorded
  runtime/model footprint and routing policy."* §7 item 5: *"change[s]
  nothing about OCR's or Docling's assignment to the Evidence
  Intelligence/OCR Mechanism boundary. No reinterpretation of CDR-007
  is made or needed."*
- `docs/architecture/DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md`
  (full, 247 lines) — §2's own provisional specialist table: "Scanned
  PDF or image | Docling OCR/layout | OCR derivative, never source
  text; currently demonstrated, resource/model controls required";
  "Tesseract | none yet | Blocked/deferred; not installed, so no
  OCR-quality conclusion." §8 Owner Decision 6 ("model-backed Docling
  gate"): resolves only that Tier B "requires the existing governed
  `OcrMechanism`/Evidence Intelligence authorisation boundary in
  addition to ordinary ingestion routing" — a gating requirement, not
  an implementation authorization. §9: *"Implementation remains
  blocked until... a scope lock fixes concrete types and authority."*
- `docs/architecture/DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md`
  (full re-read of §5.5-§5.7 and §6) — §5.6: *"Tier B routing through
  'the existing OCR Mechanism/Evidence Intelligence boundary' is
  accurate to what exists... but authorises no invocation by
  itself."* §6, the exact, load-bearing finding this document
  discharges: *"Docling additionally remains, independently, 'Out of
  Scope' (Contract Design §13, Scope Lock line 245) for the OCR
  Mechanism programme itself — Tier B routing for Docling specifically
  depends on both this question's resolution **and a separate future
  extension of OCR Mechanism governance to cover Docling at all**, an
  earlier-stage dependency than the `PermissionAction` question
  alone."*
- **Adoption commit, confirmed by fresh `git log`:** all three
  documents above were committed together at `84cc061`
  ("docs(governance): adopt document ingestion authority model"),
  dated 2026-08-22 — seventeen days *after* the OCR Mechanism Contract
  Design/Scope Lock (`172eb70`, 2026-08-05) and the Units 1-11
  completion/archive (`8d9f4a8`, 2026-08-06). The later-adopted
  documents deliberately do not purport to override or extend the
  earlier OCR Mechanism programme's own separate authority — confirmed
  by their own text (above), not merely by chronology.
- `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md` (full re-read)
  — §2: "Select or implement a concrete engine" is a named
  non-responsibility. Out of Scope: "Docling, or any structured-document-
  conversion capability." §12 Dependency Model: the OCR mechanism
  "holds no dependency of its own... mirroring exactly `ReasoningProvider`'s
  'pure callee, calls nothing' shape" — confirmed this section does
  **not** itself contain the "technical process dependencies" language;
  that text belongs exclusively to the Scope Lock's own §13, below, not
  duplicated in the Contract Design.
- `docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md` (full re-read) —
  §13 Dependency Boundary: "It must not depend on:... Docling, or any
  structured-document-conversion capability" — binding on the
  *contract* (`OcrMechanism`/`OcrProviderAdapter`/`OcrExecutionSequencer`
  themselves), confirmed by its own next paragraph: *"If a future
  provider adapter requires technical process dependencies (a
  subprocess handle, a temporary-file library, a native-binding
  layer), those remain strictly implementation-local to that adapter
  and must never migrate authority into the OCR mechanism's own
  contract — the contract this document freezes remains exactly as
  described above regardless of what any single concrete adapter
  eventually requires internally."* §14 Provider Neutrality; §15
  Security and Resource Limits (network access conditional, closed by
  the Unit 12 Scope Lock, restated here for the concrete-adapter tier
  specifically, §6 below); §16 Explicit Exclusions table row: "Docling
  | Contract Design 'Out of Scope'; §13 — a structured-document-
  conversion capability, unrelated to and not authorised by this
  contract."
- `docs/architecture/OCR_MECHANISM_UNIT_12_RUNTIME_INVOCATION_SCOPE_LOCK.md`
  (full, re-confirmed unchanged since its own adoption commit `0be393c`)
  — §14's own frozen resource/security bounds table, reused verbatim
  at §6, below, never re-derived or altered.
- `docs/architecture/OCR_MECHANISM_UNIT_12_IMPLEMENTATION_PLAN.md`
  (full, re-read fresh, currently untracked) — §4's own conclusion
  ("provider/mechanism selection cannot legitimately be resolved
  inside this implementation plan... a separate, bounded, future
  mechanism-selection governance decision is required") is confirmed
  **directionally correct but incomplete** — it did not name Docling's
  already-adopted, demonstrated status (§13, below).
- `docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md` §16 item
  6: "Concrete provider identity and adapter design — future work";
  `docs/reviews/OCR_MECHANISM_PROGRAMME_COMPLETION_REVIEW.md`
  (re-confirmed Units 1-11 accepted, unmodified).
- CDR-007, CDR-008 — re-confirmed unchanged; no new reading required,
  since this document decides nothing at that tier.
- `src/interfaces/OcrProviderAdapter.kt` (full, re-read fresh) — *"the
  one boundary a future concrete provider's own types are permitted to
  exist behind, mirroring exactly how `TikaEvidenceExtractor` is the
  sole file permitted to import Apache Tika's own types."* Zero
  concrete implementations confirmed present anywhere in `src/` (fresh
  `grep -rn ": OcrProviderAdapter" src/` returns exactly one match —
  `OcrExecutionSequencer`'s own constructor parameter *type*, not an
  implementation).
- `src/interfaces/OcrMechanism.kt`, `src/runtime/OcrExecutionSequencer.kt`
  (full, re-confirmed unchanged since the Unit 12 Implementation Plan's
  own review) — the governing execution machinery this document's own
  §5 relies on, unmodified.
- `src/composition/ParkerRuntime.kt`, `src/runtime/DefaultEvidenceIntelligence.kt`
  — re-confirmed unchanged; this document adds no composition claim of
  its own beyond what the Unit 12 Scope Lock/Implementation Plan
  already established.
- `build.gradle.kts` — fresh, full-file search: zero Docling reference
  of any kind, confirmed again in this same pass (`grep -ni "docling"`
  returns no match).
- Repository-wide fresh search (this pass): `grep -rli "docling"
  --include="*.py" --include="*.txt" --include="*.toml" --include="*.cfg"
  --include="*.sh" .` and `find . -iname "*docling*"` (excluding
  `.git/`) both return zero results outside `docs/`. **No Docling
  installation, script, dependency declaration, or wrapper file exists
  anywhere in this repository.**

## 3. Existing Docling selection evidence — confirmed, independently, not assumed

**Docling is already the adopted, demonstrated OCR/re-recognition
mechanism.** Two independent, adopted documents state this directly,
in evidence-based rather than aspirational terms:

- Provenance Model §5: Docling *"is the demonstrated OCR mechanism"* —
  contrasted directly, in the same sentence, with Tesseract, which
  *"remains unevaluated because it was not installed; no quality
  conclusion is authorised."* This is a factual record that an actual
  technical evaluation occurred and reached a conclusion, not a
  provisional preference stated in the abstract.
- Routing Policy §2's own table, "Scanned PDF or image" row: *"Docling
  OCR/layout... currently demonstrated, resource/model controls
  required."* The same table's "Tesseract" row: *"none yet |
  Blocked/deferred; not installed."*

**No implementation or dependency artefact of this demonstration
exists in the repository** (§2, above) — the evaluation was performed
outside this codebase; only its conclusion was recorded in adopted
governance text. This document treats that recorded conclusion as
authoritative fact, exactly as instructed, and does not attempt to
independently re-verify, repeat, or second-guess the demonstration
itself — doing so would be exactly the re-opened provider-selection
exercise this document is expressly forbidden from performing.

**This document does not reopen provider selection, and does not
newly evaluate or substitute Tesseract, PaddleOCR, EasyOCR, or any
other provider.** Tesseract is not merely undecided — it is
affirmatively excluded by the same adopted evidence ("not installed,"
"no quality conclusion is authorised"). No other provider is named by
any adopted document at all. Nothing below reopens, narrows, or widens
that record.

## 4. Exact prior authorization gap

**Existing OCR Mechanism governance does not yet authorize a concrete
Docling adapter.** Two, independent, textually confirmed reasons:

1. The Contract Design's and Scope Lock's own, unmodified "Docling —
   Out of Scope" exclusions (§2, above) bind the *abstract contract*
   (`OcrMechanism.kt`, `OcrProviderAdapter.kt`, `OcrExecutionSequencer.kt`)
   — they do not, and by their own text cannot, authorize any concrete
   implementation of anything, Docling included; concrete provider
   selection is separately, explicitly excluded from that governance
   tier's own scope ("Select or implement a concrete engine" — a named
   non-responsibility).
2. The Canonical Governance Alignment's own §6 states the gap
   explicitly and by name: Docling *"remains, independently, 'Out of
   Scope'... for the OCR Mechanism programme itself"* and requires *"a
   separate future extension of OCR Mechanism governance to cover
   Docling at all."* This document is that extension.

**No conflict exists between the Document Ingestion-tier evidence and
the OCR Mechanism-tier exclusion.** Both are adopted, both are
internally consistent with each other, and neither purports to resolve
what the other reserves — the Document Ingestion documents record
*that* Docling was demonstrated and is routing-preferred; the OCR
Mechanism documents reserve *whether and how* any concrete provider,
Docling included, may actually be wrapped as an adapter. This document
performs the second act, using the first as its own evidentiary basis
— it does not manufacture new evidence, and it does not reopen or
amend either source.

## 5. Proposed Docling authority boundary

**Docling's exact role and authority: a wrapped, confined, external
recognition engine — nothing more.** Behind a future, single, concrete
`DoclingOcrProviderAdapter` (illustrative name, not frozen — Contract
Design §11's own "exact Kotlin names... remain a future Implementation
Plan's own responsibility" discipline applies identically here),
Docling performs exactly the one act the Contract Design's own §1
already authorizes for the OCR mechanism as a whole: interpreting
already-retrieved image content into recognised text, and nothing
more. Docling acquires no authority beyond what any other hypothetical
`OcrProviderAdapter` implementation would already be confined to by
the already-frozen contract (§2, above) — this document grants Docling
no capability the Contract Design/Scope Lock do not already, generically,
bound every provider to.

**The boundary between Parker and Docling is exactly the
`OcrProviderAdapter` interface — nothing crosses it in either
direction except `OcrRecognitionRequest` in and `OcrRecognitionOutcome`
out.** `DoclingOcrProviderAdapter` is, and must remain, the sole file
in this repository permitted to reference any Docling-specific type,
process invocation, or configuration — mirroring exactly
`TikaEvidenceExtractor`'s own already-established confinement
discipline (Evidence Processing Searchable PDF Boundary Clarification
§3), a discipline `OcrProviderAdapter.kt`'s own KDoc already names by
analogy. No Docling-specific type, exception, or vocabulary may leak
into `OcrMechanism`, `OcrRecognitionRequest`, `OcrRecognitionOutcome`,
`OcrExecutionSequencer`, `DefaultEvidenceIntelligence`, or any Evidence
Intelligence public type — every one of these remains exactly as
provider-neutral after this document as before it.

**`OcrProviderAdapter` remains the sole provider abstraction
boundary.** No second, Docling-specific interface, adapter registry,
or configuration surface is authorized or implied. `DoclingOcrProviderAdapter`
implements `OcrProviderAdapter` directly, with no additional public
surface of its own.

**`OcrExecutionSequencer`/`OcrMechanism` remain the governing execution
machinery, unmodified.** `OcrExecutionSequencer` continues to hold
exactly one `OcrProviderAdapter` (now, for the first time, potentially
a real `DoclingOcrProviderAdapter` instance rather than a test fake),
continues to perform exactly one delegating call with no retry, no
catch, and no timeout wrapping of its own (§6, below, on where timeout
enforcement actually belongs). This document changes nothing about
either class.

## 6. Security/resource/runtime boundaries

Every bound below is the Unit 12 Scope Lock's own §14 table, reused
verbatim — **not re-derived, not altered, not loosened** — now bound
specifically to any future `DoclingOcrProviderAdapter`, the first
concrete adapter this authority could ever apply to:

| Boundary | Frozen requirement | Enforcement point |
| --- | --- | --- |
| Maximum source bytes | 64 MiB (`OwnerLocalFileIngressCoordinator.MAX_SOURCE_BYTES`, reused) | Composition/coordinator tier, before invocation, per the Unit 12 Implementation Plan's own design |
| Maximum PDF page count | 200 pages | Inside `DoclingOcrProviderAdapter`, before invoking Docling itself — **not itself a wall-clock guarantee** (below) |
| Maximum image width/height | 10,000 × 10,000 pixels | Inside `DoclingOcrProviderAdapter`, before invoking Docling |
| Maximum total pixels | 100,000,000 (100 megapixels) | Same |
| Maximum OCR output size | 20 MiB of recognised text | Inside `DoclingOcrProviderAdapter`, on Docling's own returned text, before constructing `OcrRecognitionResult` |
| Timeout/deadline | 15 minutes per invocation, **independent of, and never reconciled against, the page-count bound** | Inside `DoclingOcrProviderAdapter`, wrapping Docling's own invocation (for example, `withTimeout`, or a bounded subprocess wait) — never at the `OcrExecutionSequencer` tier, which the Unit 12 Implementation Plan's own §5.L already confirmed structurally forbids timeout wrapping |
| Concurrency | Exactly 1 concurrent Docling invocation per Parker instance | Composition tier or `DoclingOcrProviderAdapter` itself (for example, a single-permit `Mutex`); not designed in code by this document |
| Network access | **None authorised, for Docling execution specifically.** Closes the Unit 12 Scope Lock's own network conditional a second time, now naming Docling explicitly | Structural — `DoclingOcrProviderAdapter` must hold no networking-capable dependency of any kind; if Docling's own runtime is capable of network access by default, the adapter/deployment must disable it, never merely leave it unconfigured |
| Path traversal / command injection | Prohibited outright | Binding on any subprocess invocation, temporary-file naming, or path construction `DoclingOcrProviderAdapter` performs |
| Temporary workspace | Controlled, cleaned up on every path (success, failure, and timeout alike) | `DoclingOcrProviderAdapter`'s own responsibility |

**Explicit local execution only.** Docling must run in-process or as a
locally-supervised subprocess on the same host as the rest of Parker's
runtime — never a remote service call, never a hosted API, never a
call to any Docling-as-a-service offering, present or future, without
its own separate governance decision.

**No automatic invocation.** Unchanged from the Unit 12 Scope Lock's
own §7: `DoclingOcrProviderAdapter` is reached only via the existing
`OcrMechanism`/`EvidenceIntelligence`/`ParkerRuntime.analyseEvidence`
chain, explicit and owner-triggered, never self-triggered, never from
`RequiresTierB`/`RequiresOcr` alone, never from a background process.
This document adds no new invocation path and narrows nothing already
frozen there.

**Provider/model identity and version provenance.** `OcrRecognitionIdentity`
(already frozen, Unit 1) must be populated by `DoclingOcrProviderAdapter`
with Docling's own concrete identity — `mechanismIdentity` naming
Docling itself (the first time this field is ever populated with a
real, non-illustrative value), `mechanismVersion` naming the exact
Docling release/build in use, and `configurationProfile` naming
whatever model/pipeline configuration Docling was invoked with. Where
Docling itself is model-backed (it is), the underlying model's own
identity/version must be disclosed through the same structured record
— never fabricated, never left silently absent when genuinely known.

**Malformed/oversized/unsupported provider output.** `DoclingOcrProviderAdapter`
must map every Docling-side failure or limit breach to one of the
already-frozen, non-collapsible `OcrRecognitionOutcome` variants (Unit
1/7, unmodified) — a resource-limit breach or timeout to
`ProcessingOrDependencyFailure`; input Docling itself cannot process to
`UnsupportedOrInaccessibleInput`; a genuinely unexpected fault
propagated as an ordinary thrown exception, never silently caught and
converted to a fabricated success. No output larger than, or otherwise
violating, §6's own bounds may ever be wrapped in `Recognised`.

**Controlled model/cache provisioning is a separate deployment
concern, not authorized here.** The Plugin Contract's own §8 already
names *"controlled Docling model/cache provisioning"* as *"mandatory
design concerns, not controls implemented by this draft"* — restated,
unchanged, by this document: how Docling's own model weights, cache
directories, or runtime dependencies are obtained, verified, and
deployed is explicitly deferred (§9, item B, below).

**Source custody and manifest integrity before OCR execution.** Every
requirement the Unit 12 Scope Lock's own §10 already froze —
manifest-verified, non-tautological retrieval before any `OcrMechanism`
call — applies to a Docling-backed invocation identically; this
document adds nothing new here and narrows nothing.

**Append-only/reprocessing behaviour, no automatic Memory Core
registration or Knowledge promotion, no `DerivativeGenerationRecord`
path for Tier B, no deletion/retention authority, no new retrieval/
QMD/RKS authority.** All six restated, unchanged, from the Unit 12
Scope Lock's own §13/§16/§17 — Docling's own selection changes none of
them. A `DoclingOcrProviderAdapter`'s own output is subject to exactly
the same `EvidenceAnalysisResult` mapping (`TransientOutput` by
default; `CandidateArtifactProduced`/`CandidateRecordProduced` only by
a future, separate policy decision) the Unit 12 Implementation Plan
already designed for any provider — this document does not special-case
Docling in the mapping, gate, or downstream-effect layer in any way.

**No provider-specific authority leaks through `OcrProviderAdapter`.**
Restated as its own, final, explicit requirement: no `PermissionAction`,
`ResourceType`, Memory Core field, Knowledge authority, or QMD/RKS
capability may ever be added, or read as implied, on account of
Docling specifically. Every governed authority Docling's output can
ever reach is exactly, and only, the authority any other hypothetical
provider's output could already reach under the Unit 12 Scope Lock —
Docling gains no privilege by being named.

**Explicit, standalone confirmation: Docling receives no Evidence
Custodian authority of any kind.** `OcrRecognitionRequest` carries
only already-retrieved bytes a caller obtained through its own,
separate `EvidenceCustodian.retrieve` dependency before this document's
own boundary is ever reached (§5, above) — `DoclingOcrProviderAdapter`
holds no `EvidenceCustodian` reference, calls no `accept`/`retrieve`/
`retrieveManifest` method, and gains no read, write, or custody
authority of any kind, exactly as no other hypothetical
`OcrProviderAdapter` implementation could under the already-frozen
contract (Scope Lock §13, restated at §2, above).

## 7. Relationship to Unit 12

**Unaffected, unchanged, and not re-authorized by this document — it
was already, separately, authorized.** The Unit 12 Scope Lock and
Implementation Plan resolved the *invocation-authority* question
(permission surface, runtime entry point, composition shape)
independent of which provider, if any, would eventually satisfy
`OcrProviderAdapter`. This document resolves the *provider-authority*
question the Canonical Governance Alignment's own §6 separately named.
The two are, and remain, distinct governance layers — this document
does not modify, extend, or narrow anything in either Unit 12
document, and does not require either to be re-adopted or re-reviewed.

## 8. Relationship to Tier B

**Unaffected, unchanged.** Document Ingestion's own Tier B routing/
owner-invocation work — the code that would actually construct an
OCR-eligible `analyseEvidence` request from a `RequiresTierB`
disclosure — remains entirely outside this document's own scope,
exactly as it remained outside the Unit 12 Scope Lock's own scope.
Authorizing Docling as a concrete provider does not authorize, imply,
or bring closer any Document Ingestion-side implementation decision;
that unit's own governance and implementation-planning remain to be
separately proposed.

## 9. Distinguishing A-E — precisely, per instruction

| # | Item | Authorized by this document? |
| --- | --- | --- |
| A | Docling provider authorization (this document's own subject) | **Yes** — this is what this document does |
| B | Docling runtime/model/cache provisioning | **No** — explicitly named as a separate deployment concern (§6, above), requiring its own future implementation/operational decision, not governance-blocked by this document but not performed or designed by it either |
| C | Implementation of `DoclingOcrProviderAdapter` | **Now authorized to be proposed and built**, subject to every boundary in §5-§6, by a future, separate implementation unit — not performed by this document itself |
| D | Unit 12 runtime composition wiring | **Already separately authorized** (prior Unit 12 Scope Lock/Implementation Plan) — unaffected by this document in either direction |
| E | Document Ingestion Tier B routing/owner invocation | **Not authorized** — remains entirely separate, unaddressed future governance (§8, above) |

These five are not, and must never be, silently combined. A future
implementation turn that builds C must not simultaneously attempt E
without E's own separate proposal and review, and must not treat B as
solved merely because C exists.

## 10. Implementation impact map

| Surface | Classification |
| --- | --- |
| A future `DoclingOcrProviderAdapter` implementation, satisfying §5-§6 in full | **CONDITIONAL** — authorized to be proposed once this document is adopted; not performed here |
| Docling's own runtime/model/cache installation and provisioning | **CONDITIONAL**, separately governed (§9 item B) — required before C can run in production, not designed or performed here |
| Any dependency addition (a Docling wrapper library, a subprocess-invocation utility) | **CONDITIONAL** — required only once C is actually implemented; not identified, evaluated, or added by this document |
| Any change to `OcrMechanism.kt`, `OcrProviderAdapter.kt`, `OcrExecutionSequencer.kt` | **FORBIDDEN** |
| Any change to the Contract Design, the Scope Lock, the Unit 12 Scope Lock, or the Unit 12 Implementation Plan's own design content | **FORBIDDEN** |
| A second, Docling-specific `OcrProviderAdapter`-like interface | **FORBIDDEN** |
| A new `PermissionAction`/`ResourceType`/Memory Core field/Knowledge authority for Docling specifically | **FORBIDDEN** |
| Network access for Docling execution | **FORBIDDEN** |
| Document Ingestion Tier B routing implementation | **FORBIDDEN under this document's own authority** (§8) |
| A narrow accuracy correction to `OCR_MECHANISM_UNIT_12_IMPLEMENTATION_PLAN.md`'s own §4 text | **OPTIONAL, and deferred** (§13, below) — not performed in this turn |

## 11. Required / conditional / optional / forbidden classification

| # | Rule | Class |
| --- | --- | --- |
| 1 | `DoclingOcrProviderAdapter` implements `OcrProviderAdapter` directly, no second interface | **R**, once C is undertaken |
| 2 | Every §6 numeric bound, enforced inside the adapter (or the composition tier, for source-byte size) | **R** |
| 3 | Local execution only; no network access | **R** |
| 4 | `OcrRecognitionIdentity` populated with Docling's own real identity/version | **R** |
| 5 | Every Docling-side failure mapped to an existing, non-collapsible `OcrRecognitionOutcome` variant | **R** |
| 6 | Docling's own runtime/model/cache provisioning mechanics | **C** — required before production use, not designed here |
| 7 | A dependency addition for Docling invocation | **C** — required once C is implemented, not added here |
| 8 | Exact Docling invocation mechanism (in-process binding vs. supervised subprocess) | **O** — left to the future implementer, within §6's own bounds |
| 9 | A new `PermissionAction`/`ResourceType`/interface for Docling specifically | **F** |
| 10 | Network access, hosted-API use, or remote Docling invocation | **F** |
| 11 | Docling output entering `DerivativeGenerationRecord`, automatic Memory Core registration, or automatic Knowledge promotion | **F** |
| 12 | Document Ingestion Tier B routing implementation under this document's own authority | **F** |

## 12. Adversarial review

| # | Attack | Result |
| --- | --- | --- |
| 1 | Reopened provider selection | Foreclosed — §3 treats Docling's adoption as given fact, re-verified against adopted text, never re-evaluated against Tesseract/PaddleOCR/EasyOCR or any alternative |
| 2 | Silent Tesseract/other substitution | Foreclosed — §3 explicitly re-confirms Tesseract's own excluded status; no other provider is named anywhere in this document |
| 3 | Docling authority leaking into the abstract contract | Foreclosed — §5's own confinement rule, mirroring `TikaEvidenceExtractor`'s established discipline exactly; §2's own citation of Scope Lock §13's "must never migrate authority" text |
| 4 | Memory Core/Knowledge/QMD/RKS authority granted to Docling | Foreclosed — §6's own explicit final requirement; no new field, action, or resource type of any kind |
| 5 | Automatic invocation from `RequiresTierB`/`RequiresOcr` | Foreclosed — §6, restating Unit 12 Scope Lock §7 unchanged |
| 6 | Hidden network access | Foreclosed — §6's own explicit Docling-specific closure of the network conditional, plus a structural "no networking dependency" requirement |
| 7 | Unbounded execution/timeout evasion | Partially foreclosed — the same honest limitation the Unit 12 Scope Lock already discloses: bounds are frozen and required, but enforcement is only guaranteed once a real `DoclingOcrProviderAdapter` actually implements them; this document does not, and cannot, implement code |
| 8 | Page-count/timeout reconciliation error (the exact defect corrected in the Unit 12 Scope Lock's own acceptance review) | Foreclosed — §6's table is copied verbatim from the corrected Unit 12 Scope Lock table, explicitly re-stating "never reconciled against the page-count bound" |
| 9 | Decompression/resource bombs | Partially foreclosed, same reasoning as #7 — bounds named, enforcement is adapter-tier future work |
| 10 | Output explosion | Foreclosed at the mapping/bound level (§6); enforcement is adapter-tier |
| 11 | Malformed provider output silently accepted | Foreclosed — §6's own explicit mapping requirement to existing, non-collapsible outcome variants |
| 12 | False-success outcomes | Foreclosed — same mapping requirement; a resource-limit or timeout breach can never become `Recognised` |
| 13 | Provenance/version loss | Foreclosed — §6's own identity/version requirement, first real-value population of `OcrRecognitionIdentity` |
| 14 | Accidental Document Ingestion Tier B authorization | Foreclosed — §8's own explicit exclusion, plus §9's table row E marked "Not authorized" |
| 15 | Silent combination of B-E | Foreclosed — §9's own explicit, itemized distinction, with an explicit warning against combining them |
| 16 | Reopening/editing the Contract Design's or Scope Lock's own "Docling — Out of Scope" text | Foreclosed — this document explicitly, textually confirms that exclusion "remains true and unchanged" (Status, above) and never touches either file |
| 17 | Architectural duplication (a second provider-abstraction interface) | Foreclosed — §5's own explicit "no second interface" requirement, restated at §11 item 1 |
| 18 | Dependency creep (silently adding a Docling dependency in this turn) | Foreclosed — Status and §10 both confirm no dependency is added by this document |
| 19 | Runtime/model provisioning silently treated as solved | Foreclosed — §6/§7/§9 item B each independently, explicitly name this as unsolved, deferred future work |
| 20 | Deletion/retention or new indexing authority for Docling specifically | Foreclosed — §6's own restated, unchanged prohibitions |

No item resolves to a blocker for adopting this document. Items 7 and
9 remain honestly, partially unresolved for the same reason the Unit
12 Scope Lock's own equivalent items do — this document freezes
requirements a future concrete adapter must satisfy; it cannot itself
enforce them without writing code, which is outside its own,
governance-only authority.

## 13. `OCR_MECHANISM_UNIT_12_IMPLEMENTATION_PLAN.md`'s own accuracy defect

**Confirmed accurate defect, correctable, not a conflict** (matching
the prior reconciliation review's own classification, re-verified
here): that plan's §4 correctly concludes no concrete adapter is
authorized, but frames the field as fully open with no existing
preference, omitting Docling's own adopted, demonstrated status and
the precise, named blocking gap this document now closes.

**This document does not correct it.** Two reasons:

1. **Sequencing.** Before this document's own adoption, the accurate
   position was still "no provider is authorized, and the exact
   blocking gap is unnamed in that plan" — correcting the plan before
   this document exists would risk the plan asserting an authorization
   this document had not yet granted. Now that this document exists
   (in Draft), the plan could be updated to *cite* it, but only once
   this document's own status is itself settled by owner review —
   editing the Implementation Plan to reference a still-Draft,
   not-yet-adopted authority would misstate the Implementation Plan's
   own reliability.
2. **Scope discipline.** This governance turn's own task was framed,
   and is being executed, as a narrow provider-authorization decision
   — not as an editing pass over a sibling, separately-authored
   planning document. The governing instruction for this turn required
   determining *whether* correction is permitted now, not performing
   it opportunistically because the tooling is already open.

**Conclusion: the Implementation Plan's own accuracy correction must
occur in a separate, subsequent, owner-reviewed step** — after this
document's own adoption decision is known, so the correction can
accurately state either "Docling is now authorized, subject to
`OCR_MECHANISM_DOCLING_PROVIDER_AUTHORIZATION_SCOPE_LOCK.md`" (if
accepted) or continue stating the gap as still-open (if not). This is
a narrow, mechanical, low-risk correction once the sequencing is
right — not a reconciliation requiring new governance of its own.

## 14. Citation and cross-reference audit

Every external citation above was verified against the actual cited
document in this same drafting pass — full, fresh re-reads of the
Provenance Model, the Routing Policy, and the relevant sections of the
Canonical Governance Alignment (§2, above), not carried forward from
any prior turn's own summary. The Contract Design's and Scope Lock's
own "Docling — Out of Scope" and "technical process dependencies... must
never migrate authority" passages were independently re-located and
quoted verbatim, not paraphrased from memory. Every code fact
(`OcrProviderAdapter` implementer count, `build.gradle.kts` content,
repository-wide Docling search) was re-run fresh in this same pass,
not assumed from the prior reconciliation review. Every internal
`§N` self-reference in this document was checked against this
document's own sixteen numbered sections (1-16, this one being §14)
for correct heading position and direction, using the same automated
audit method the two prior OCR Mechanism governance documents this
session established.

## 15. Conflicts or ambiguities

**None found.** §4, above, already establishes in full why the
Document Ingestion-tier evidence and the OCR Mechanism-tier exclusion
do not conflict — each stays within its own lane, and this document's
own act (extending OCR Mechanism authority to cover Docling,
specifically, narrowly, and only behind the confined adapter boundary)
is exactly, and only, the act the Canonical Governance Alignment's own
§6 predicted would be required, using no authority beyond what that
prediction, and the Scope Lock's own already-provisioned "technical
process dependencies" option, already supply.

## 16. Files created/modified

Exactly one — `docs/architecture/OCR_MECHANISM_DOCLING_PROVIDER_AUTHORIZATION_SCOPE_LOCK.md`
(new). No other file is created, modified, staged, committed, or
pushed.

## Final Recommendation

**READY FOR OWNER REVIEW.**

This document confirms, independently and fresh, that Docling is
already the adopted, demonstrated OCR mechanism (Provenance Model §5;
Routing Policy §2, both adopted at `84cc061`), and that the OCR
Mechanism programme's own governance has never extended its own
authority to cover it — exactly the gap the Canonical Governance
Alignment's own §6 named. It authorizes a future, separate,
implementation-planning proposal for a concrete `DoclingOcrProviderAdapter`
to be built, confined exactly as `TikaEvidenceExtractor` already is,
bound by every numeric/security limit the Unit 12 Scope Lock already
froze, applied here for the first time to a real provider. It does not
authorize Docling's own runtime/model/cache provisioning, does not
perform or authorize the concrete adapter's own implementation, does
not affect Unit 12's own separately-authorized composition wiring, and
does not authorize Document Ingestion's own Tier B routing work — each
remains distinct, and none is silently combined with another. It
identifies, but defers to a separate, subsequent, owner-reviewed step,
the narrow accuracy correction `OCR_MECHANISM_UNIT_12_IMPLEMENTATION_PLAN.md`
itself still needs.
