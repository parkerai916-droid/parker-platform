# Document Ingestion — Derivative-to-Memory-Core Registration Scope Lock

## Status

**Draft for owner review. Not yet accepted. Governance/architecture only —
no code implemented.** Resolves exactly the two questions
`DOCUMENT_INGESTION_MEMORY_CORE_CROSS_REFERENCE_SCOPE_LOCK.md` ("Unit 6")
§20–§21/§25 left explicitly deferred: which Parker-owned coordinator may
register an admitted Tier A `DerivativeGenerationRecord` into Memory
Core, and under what trigger. It does not implement anything, does not
touch Tier B/OCR, does not touch Knowledge promotion, does not touch
Evidence Intelligence, and does not authorize any UI or further ingress
work.

## 1. Purpose and scope

This document decides **two questions only**:

1. Which Parker-owned coordinator is authorized to register an already-
   admitted Tier A `DerivativeGenerationRecord` into Memory Core as a
   `Document`/`Provenance` pair.
2. Under what trigger that registration occurs.

It also fixes the exact field mapping the registration act uses (Unit 6
§9 already named the fields; this document confirms which are
populated, and how, for the derivative case specifically) and the
content boundary (what information actually crosses into Memory Core,
versus what remains exclusively addressable through Document Ingestion's
own storage).

It does not decide, and must not be read as deciding, anything about
Tier B/OCR registration mechanics (Unit 6 §11, second bullet — left to
Evidence Intelligence's own governance, unaffected here), Knowledge
promotion, Evidence Intelligence invocation, audit-record retention,
Memory Core/Knowledge deletion propagation, or any UI/ingress work.

## 2. Authoritative sources inspected (fresh, this unit)

- `DOCUMENT_INGESTION_MEMORY_CORE_CROSS_REFERENCE_SCOPE_LOCK.md` ("Unit
  6") — §4 (existing authority), §6-7 (ownership boundary, direct-write
  decision), §8 (candidate-information boundary), §9-10 (provenance
  cross-reference/identity-reference decisions — the field mapping this
  document applies), §11 (Tier A → Memory Core directly, no EI, already
  available), §16 (RKS/QMD boundary), §17 (deletion/retention
  deferral), §20-21 (the two questions this document resolves), §24-25
  (future implementation surfaces, explicit deferrals).
- `DOCUMENT_INGESTION_PROGRAMME_GOVERNANCE_CLOSURE.md` — §6 (authority-
  ownership matrix, Memory Core registration row), §7 row 27
  (coordinator identity — deferred, "must not be assumed"), §8/§11
  (deferrals register, unresolved matters).
- `DOCUMENT_INGESTION_TIER_A_IMPLEMENTATION_CLOSURE.md` — §11 (no
  Memory Core integration claimed complete), §13 (explicit non-blocking
  deferrals list, including optional Memory Core registration).
- `DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md`
  ("Unit 2") — §8-9 (byte-backed versus non-byte derivatives; content
  identity/digest), §17 (retention/deletion — no new authority created),
  §18 (authorization — only the ingestion coordinator may admit),
  §20 (field-shape classification), §23 (downstream-reference boundary
  — a future system may reference `DerivativeGenerationId` as an opaque
  identity; Memory Core wiring explicitly deferred to this unit).
- `DOCUMENT_INGESTION_DERIVATIVE_REVIEW_TARGET_SCOPE_LOCK.md` ("Unit
  3") — §8 (critical): `APPROVED` "does **not**... grant Memory Core
  authority — reviewing has no write path into Memory Core... at most, a
  reference, never a write." This directly forecloses using `APPROVED`
  as a registration trigger (§5, below).
- `DOCUMENT_INGESTION_AUDIT_AUTHORITY_SCOPE_LOCK.md` ("Unit 5") — §22:
  the Memory Core/Knowledge boundary is explicitly deferred to this
  unit; ingestion audit invents no Memory Core field or relationship.
  Confirms this document owes ingestion audit nothing and receives
  nothing from it.
- `DOCUMENT_INGESTION_AUTHORITATIVE_SOURCE_MANIFEST_RETRIEVAL_SCOPE_LOCK.md`
  — §12: "an invocable `EvidenceArtifact` does not require a Memory Core
  `Document`... not every artifact has one." Confirms registration is
  additive discoverability, never a precondition for anything else in
  Document Ingestion.
- `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` — I-15 ("Independent
  retrieval... derivative deletion/replacement cannot affect [source]");
  §7 amendment 3, superseded by Unit 6's own adoption.
- `MEMORY_CORE_CONTRACT_DESIGN.md` — §5 (Document contract: registration
  only, never parsed contents); §6 (Assertion contract: a claim, never
  automatically truth); §7 (Provenance contract: the exact field shape
  this document applies); §10 (Permission Boundary: "No new
  `PermissionAction` or `ResourceType` value is required").
- `MEMORY_CORE_SCOPE_LOCK.md` — confirms Memory Core never self-
  authorizes and the seven-mode retrieval freeze (unaffected here, no
  retrieval capability is added).
- CDR-004 (Provenance Identifier Resolution — Model B, a specialised
  capability within existing Identifier lookup; not directly load-
  bearing here since this document adds no retrieval capability) and
  CDR-008 (Memory Core / downstream relevance boundary — confirms RKS/
  QMD gains no canonical authority from content it reads; directly
  informs §16, below).
- `src/interfaces/MemoryCore.kt` (fresh-read: `MemoryCore` interface,
  `CandidateProvenance`, `CandidateDocument`, `ContentNature`,
  `DocumentProcessingStatus` — exact current field shapes, confirmed
  below).
- `src/runtime/EvidenceRegistrationCoordinator.kt` (fresh-read in full)
  — the existing, adopted, working precedent this document mirrors by
  direct analogy throughout.
- `src/interfaces/DerivativeGeneration.kt` (fresh-read:
  `DerivativeGenerationRecord`, `DerivativeContentIdentity` —
  `rootSourceEvidenceArtifactId`, `derivativeKind`, `contentIdentity`
  as `Digest(algorithm, digest)` or `NoCanonicalSerialization`).
- `src/interfaces/TierADocumentIngestionRouter.kt`,
  `src/runtime/GovernedTierADocumentIngestionRouter.kt` (fresh-read:
  confirms `ReconciliationRequired` corresponds to
  `AdmittedAuditFailed` — the record is durably published, but its own
  `ADMITTED` audit record failed; addressed in §6, below).
- `src/runtime/TierAOwnerInvocationCoordinator.kt`,
  `src/runtime/OwnerLocalFileIngressCoordinator.kt` (fresh-read this
  session, already adopted) — both hold exactly the dependencies their
  own governance authorizes and neither references `MemoryCore`;
  confirms neither is a viable host for this document's registration
  act without violating its own adopted scope lock (§4.A, below).

## 3. Existing frozen boundaries (restated, not reopened)

- Evidence Custodian remains the sole admission authority for source
  bytes (Evidence Custodian Scope Lock, restated throughout Document
  Ingestion Units 1-6 and both accepted implementation units this
  session).
- The Document Ingestion coordinator role mints `DerivativeGenerationId`s
  and admits `DerivativeGenerationRecord`s; it acquires no Memory Core
  authority by doing so (Unit 2 §18; Programme Closure §6).
- Memory Core never self-authorizes; every write is gated by whatever
  composes it, using the existing `(PermissionAction.WRITE,
  ResourceType.MEMORY)` pair (Contract Design §10).
- `APPROVED` (`DerivativeReviewRegistry`) is a human-verification signal
  only; it grants no write authority of any kind, into Memory Core or
  anywhere else (Unit 3 §8).
- A `Document`/`Provenance` reference to an ingestion identity transfers
  no authority in either direction (Unit 6 §10) and is never mandatory
  for that identity's own Document Ingestion validity (Manifest
  Retrieval Scope Lock §12).

## 4. Coordinator ownership decision

**Decision: a new, narrowly dedicated registration coordinator** —
structurally analogous to the existing, adopted
`EvidenceRegistrationCoordinator`, but for an already-admitted
`DerivativeGenerationRecord` rather than a `CandidateEvidenceArtifact`.
No specific Kotlin class name is frozen here (an illustrative name, not
a requirement, might be "Derivative Memory Registration Coordinator" —
implementation planning may choose differently, consistent with
Programme Closure §10's own "authority role, not a mandated single
class" discipline).

**A. `EvidenceRegistrationCoordinator` itself — rejected, wrong input
shape.** Its `register` signature is built around
`CandidateEvidenceArtifact` and calls `EvidenceCustodian.accept`
internally as its first step (fresh-confirmed, `src/runtime/EvidenceRegistrationCoordinator.kt`
lines 244-248). A `DerivativeGenerationRecord` has already been
admitted by the time this document's registration act would run — there
is no candidate to accept, and no `EvidenceCustodian` call belongs in
this act at all. Reusing this class would require changing its existing,
adopted signature to accommodate an unrelated input shape, coupling two
independently-governed acts (raw evidence registration; derivative
registration) into one class for no architectural reason.

**B. Extending `TierAOwnerInvocationCoordinator` or
`OwnerLocalFileIngressCoordinator` — rejected, violates their own
adopted governance.** Both are fresh-confirmed (this session) to hold
exactly two constructor dependencies each, proven structurally by their
own test suites, and both scope locks state, as a hard rule, that
successful completion "ends this operation" with no Memory Core call of
any kind. Adding Memory Core registration to either would be a
regression against governance already accepted and pushed to
`origin/main`, not an extension within remaining discretion.

**C. A narrowly dedicated new coordinator — adopted.** Mirrors
`EvidenceRegistrationCoordinator`'s own shape exactly: takes the
already-admitted `DerivativeGenerationRecord` (and whatever caller-
supplied facts it cannot itself derive — analogous to `documentType` in
the existing precedent) as a parameter to one operation, never fetches
it itself. Dependencies: `MemoryCore` and `PermissionEngine` only —
**not** `EvidenceCustodian`, **not** `DerivativeGenerationStorage`,
**not** a reference to either existing Tier A/local-ingress coordinator.
This is the minimum dependency set consistent with `EvidenceRegistrationCoordinator`'s
own "neither holds a reference to the other, or to this class" discipline
(its own KDoc, restated here for the new coordinator), and satisfies
every preservation criterion this unit's own task named:

- **Memory Core's exclusive write authority** — preserved; the new
  coordinator calls `MemoryCore.createProvenance`/`registerDocument`
  exactly as the existing precedent does, granting itself no authority
  Memory Core's own gate does not confer.
- **Document Ingestion's lack of Memory authority** — preserved; the new
  coordinator is not part of, and holds no reference to, the Tier A
  router, either owner-invocation coordinator, or `EvidenceCustodian`.
- **No duplicated registration logic** — preserved; the new coordinator
  reuses the identical two-call, two-gate sequence and
  `ExecutionRequest` construction shape already adopted, not a
  parallel implementation.
- **Provenance fidelity** — preserved; see §7, exact field mapping.
- **Existing permission/write-gate semantics** — preserved; see §18, no
  new `PermissionAction`/`ResourceType`.
- **Single-purpose coordinator discipline** — preserved; this
  coordinator does exactly one thing (derivative-to-Memory-Core
  registration) and nothing else, exactly as
  `EvidenceRegistrationCoordinator` does exactly one thing.

## 5. Registration universality/trigger decision

**Decision: option 2 — registration is optional and explicitly owner-
triggered, as a separate, third owner action.** Not universal-automatic
(option 1), not derivative-kind/completeness-filtered (option 3), and
not `DerivativeReview` `APPROVED`-triggered (option 4).

**Why not automatic on every admission (option 1).** Every governed
owner-facing act in this programme — evidence submission, Tier A
invocation, local file ingress — is, by explicit, repeated, adopted
design, an individually authorized owner action, never a silent
consequence of a prior act succeeding (`..._LOCAL_FILE_INGRESS_SCOPE_LOCK.md`
§14; the Owner-Facing Tier A Runtime Invocation Boundary's own identical
discipline). Automatic registration on every admission would be the
first departure from that pattern anywhere in the programme, and no
document reviewed authorizes it. Unit 6 §21 itself leaves "whether every
ingested derivative is ever registered" as an open **policy** question,
not a default.

**Why not derivative-kind/completeness-filtered (option 3).** No
document reviewed defines, or authorizes defining, a completeness-class
or derivative-kind eligibility filter for Memory Core registration
specifically; inventing one here would be exactly the kind of
unsupported trigger this unit's own task instructs against.

**Why not `DerivativeReview` `APPROVED`-triggered (option 4) —
explicitly foreclosed, not merely disfavored.** Unit 3 §8 states, in
terms directly on point: `APPROVED` "does **not**, under any
circumstance... grant Memory Core authority — reviewing has no write
path into Memory Core... where `documentId` is present at all..., it
is, at most, a reference, never a write." Making `APPROVED` a
registration trigger would convert a human-verification signal into a
write-authorizing event — precisely the authority Unit 3 already,
explicitly withholds from it. This is foreclosed by already-adopted
governance, not merely a design preference this document is choosing
against.

**Why explicit, owner-triggered (option 2) — adopted.** This is the
only option consistent with every existing owner-facing precedent in
the programme, requires no new trigger mechanism to be invented, and
keeps the owner in structural control of what enters Memory Core exactly
as they already are for evidence submission and Tier A invocation.

## 6. Eligible derivative state

**Decision: exactly a `DerivativeGenerationRecord` backing a
`TierADocumentRoutingResult.Admitted` result** — the record and payload
Document Ingestion's own atomicity invariant (Unit 2 §19; Tier A
Implementation Closure §9) already guarantees are durably published,
with a completed `ADMITTED` audit record.

**`ReconciliationRequired` is explicitly excluded from initial
eligibility.** Fresh code inspection
(`GovernedTierADocumentIngestionRouter.kt`) confirms
`ReconciliationRequired` corresponds to `AdmittedAuditFailed`: the
underlying record and payload are genuinely, durably published, but the
record's own governed admission story is not yet closed — its `ADMITTED`
audit record failed to write and remains outstanding for explicit
reconciliation (Tier A Implementation Closure §9's own "leaves the
published record and payload available for explicit reconciliation
rather than misrepresenting them as absent"). Registering a derivative
whose own audit trail is incomplete would create a Memory Core reference
to an ingestion record this programme's own governance does not yet
consider cleanly closed. This is deferred, not decided, pending a future
unit that governs reconciliation mechanics themselves (already an open
item across Units 2 and 5's own deferrals).

No other `TierADocumentRoutingResult` variant (`RequiresTierB`,
`Unsupported`, `ExtractionFailed`, `SourceIntegrityFailed`,
`AdmissionFailed`) produces an admitted record at all, so none is
eligible — this is a structural consequence of the existing sealed
class, not a new rule this document invents.

Eligibility is uniform across all four current Tier A formats (CSV,
EML, DOCX, PDF) — Unit 6 §11 authorizes "Tier A (mechanical) → Memory
Core, directly" without format qualification, and this document
introduces none.

## 7. Exact Memory Core provenance/reference mapping

Following Unit 6 §9's already-fixed field-selection decision, applied
concretely to the derivative case, by direct analogy to
`EvidenceRegistrationCoordinator`'s own existing field population:

**`CandidateProvenance`** (`src/interfaces/MemoryCore.kt`):

| Field | Value | Required/Conditional |
| --- | --- | --- |
| `sourceIdentifier` | The admitted `DerivativeGenerationId`'s own string value | **Required** |
| `sourceType` | An open, non-blank classification (for example, `"document-ingestion-tier-a-derivative"`) | **Required** |
| `acquisitionTime` | The registration act's own current time (mirrors `EvidenceRegistrationCoordinator`'s own `Instant.now()` — not `DerivativeGenerationRecord.generatedAt`, which is a distinct, earlier fact already preserved by Document Ingestion's own record) | **Required** |
| `contentNature` | `ContentNature.EXTRACTED` | **Required** — the correct, already-existing value; no new value introduced |
| `creator` / `creatorPrincipalId` | Absent unless a genuine, resolvable creator fact exists (the Tier A specialist is a mechanism, not a `Principal`) | **Optional**, left absent |
| `claimedCreationTime` | Absent, unless the source document's own claimed creation time is independently known and worth preserving here (ordinarily this belongs on the *source's* own Provenance, if one exists, not the derivative's) | **Optional** |
| `derivedFrom` | Absent for a single-parent generation; for a reconciliation-eligible multi-parent case (out of scope, §6), left to a future unit | **Conditional**, empty by default |
| `extractedFrom` | The source's own `DocumentId`, **if and only if** the source `EvidenceArtifact` was itself separately registered into Memory Core (for example, via `submitEvidence`/`EvidenceRegistrationCoordinator`) | **Conditional — frequently absent.** Local file ingress (`importEvidenceFileAsOwner`) calls `EvidenceCustodian.accept` directly and never registers the source into Memory Core (fresh-confirmed, `OwnerLocalFileIngressCoordinator.kt`) — for a derivative whose source entered custody that way, no source `DocumentId` exists to reference, and `extractedFrom` **must remain null**, truthfully, never fabricated or backfilled. |
| `processingHistory` | One structured entry naming and pointing to the `DerivativeGenerationId` (mirroring Unit 6 §9's own "one structured entry... naming and pointing to the external, ingestion-owned... Derivative Generation Record") | **Required** |
| `integrityInformation` | The derivative's own `contentIdentity` digest, where `DerivativeContentIdentity.Digest(algorithm, digest)` (byte-backed case); absent where `NoCanonicalSerialization` (non-byte case, Unit 2 §8) | **Conditional**, per byte-backed/non-byte split |
| `confidence` | Absent, unless the derivative's own `DerivativeGenerationRecord.confidence` is present and worth carrying forward | **Optional** |
| `sensitivity` | Left to implementation-plan judgment; must never default to a less-protective value than `null`'s own conservative treatment already requires (Contract Design §7) | **Optional** |

**`CandidateDocument`**:

| Field | Value | Required/Conditional |
| --- | --- | --- |
| `documentType` | The derivative's own kind/format (for example, `"csv-derivative"`, or `DerivativeGenerationRecord.derivativeKind` directly) | **Required** |
| `locationReference` | The admitted `DerivativeGenerationId`'s own string value — mirrors `EvidenceRegistrationCoordinator`'s own `acceptedEvidenceArtifact.evidenceArtifactId.value` exactly, substituting the derivative identity for the evidence identity | **Required** |
| `provenanceId` | The `Provenance` record just created, above | **Required** |
| `integrityHash` | Same digest as `integrityInformation`, where present; null for non-byte derivatives | **Conditional** |
| `processingStatus` | `DocumentProcessingStatus.PROCESSED_EXTERNALLY` — Tier A, external to Memory Core, has already, completely processed this content by the time registration is possible | **Required — recommended value**, not frozen; implementation planning may confirm |
| `metadata` | Empty, or the narrowest open key/value facts implementation planning finds genuinely necessary — never a home for the extracted content itself (§8) | **Optional** |

**No new field is invented anywhere in this mapping.** Every value above
uses an existing `Provenance`/`Document` field exactly as Unit 6 §9
already authorized, or an existing enum value already defined.

## 8. Content boundary

**Decision: only provenance/reference metadata is registered. The
actual extracted derivative content — CSV rows, DOCX paragraphs, EML
headers/bodies, PDF text — is never copied into Memory Core, under any
circumstance.**

Distinguished explicitly, per this unit's own instruction:

- **Provenance/reference metadata** — what this document authorizes:
  the fields in §7, establishing that a derivative exists, where it can
  be found (by `DerivativeGenerationId`), and its own custody-adjacent
  facts (digest, processing status). This is registration, exactly as
  Contract Design §5 defines it for `Document`: "registers that a
  source document exists... **registration only**."
- **Extracted content** (the parser payload itself — `CsvStructuralResult.rows`,
  `DocxStructuralResult`'s paragraphs, and so on) — explicitly **not**
  registered. Contract Design §5 states this directly for `Document`:
  it "never represent[s] a document's parsed contents, extracted text,
  page structure, or any interpretation of what the document says."
  This content remains exclusively addressable through Document
  Ingestion's own `DerivativeGenerationStorage`, retrievable today only
  via a fresh, explicit `invokeTierAIngestionAsOwner` call (which
  returns it synchronously) — this document adds no new retrieval path
  to it (§16).
- **Candidate propositions** — not created by this act. Nothing in Tier
  A's own mechanical extraction, or in this registration act, proposes
  a claim about what the source document *means* — that is squarely
  Evidence Intelligence's own, separately governed domain (§11, below),
  untouched here.
- **Memory assertions** (`Assertion` records) — not created by this act.
  An `Assertion` "records a claim" and "is never automatically truth"
  (Contract Design §6); mechanical Tier A extraction has not, and this
  registration act does not, make any evidential claim requiring an
  `Assertion`. Only `Provenance` and `Document` are created here.
- **Knowledge Items** — not created by this act, and not implied by it
  (§10, below).

These five layers are never collapsed into one another by this document.

## 9. Memory Core authority result

- `DerivativeGenerationRecord` remains ingestion-owned and immutable
  (Unit 2 §6); this document creates no path by which Memory Core
  registration mutates, supersedes, or reinterprets it.
- Memory Core registration does not convert a `DerivativeGenerationRecord`
  into a Memory Core record of any kind — it remains exactly what it
  was, addressable by its own `DerivativeGenerationId`, through Document
  Ingestion's own storage, unchanged.
- Memory Core may reference the derivative by identifier/provenance
  only (§7) — never by copying its content (§8).
- Document Ingestion receives no direct Memory Core write authority by
  virtue of this document — the write is performed by the new
  coordinator (§4), gated by Memory Core's own existing permission
  boundary (§18), exactly as `EvidenceRegistrationCoordinator` already
  demonstrates for raw evidence.
- Memory Core remains Parker's sole canonical system of record; nothing
  in this document creates, or implies, a second or parallel canonical
  store.

## 10. Knowledge result

Registration does not imply, trigger, or authorize Knowledge promotion
of any kind. Knowledge Memory's own promotion mechanism remains
exclusively its own (Unit 6 §4, §7, Memory Core Contract Design lines
903-913, cited in Programme Closure §6) — a `Document`/`Provenance`
pair existing in Memory Core is, at most, material Knowledge Memory
*could* later, separately, and through its own governed process,
choose to evaluate; this document neither triggers nor requires that it
ever does.

## 11. Evidence Intelligence result

Registration does not invoke, gate, or require Evidence Intelligence.
Unit 6 §11's already-adopted "Tier A (mechanical) → Memory Core,
directly, without Evidence Intelligence" path is the one this document
implements; the parallel "Tier B (recognition/model-backed) → Evidence
Intelligence → Memory Core" path is explicitly out of scope here,
untouched, and left to Evidence Intelligence's own governance exactly
as Unit 6 §11/§25 already state.

## 12. `DerivativeReview` result

Registration is completely independent of `DerivativeReviewRegistry`
state, in both directions:

- A derivative may be registered whether or not it has ever been
  reviewed, and regardless of its current review status.
- Registering a derivative never marks it `APPROVED`, and never writes
  to `DerivativeReviewRegistry` in any way — this document creates no
  new caller of that registry.
- Approving a derivative (`APPROVED`) never triggers, authorizes, or
  implies registration (§5, above) — this is the specific, foreclosed
  trigger option this document rejects, not merely declines to adopt.

## 13. OCR/Tier B result

This document authorizes registration only for a Tier A `Admitted`
result (§6). It does not invoke OCR, does not invoke Tier B, and does
not alter the existing `RequiresTierB` stop boundary in any way — a
`RequiresTierB` result produces no admitted record and is therefore
categorically ineligible for this document's registration act, not by
a new exclusion rule but because no record exists to register.

## 14. Reprocessing result

Multiple admitted generations from the same source (Unit 2's own
append-only, non-deduplicating reprocessing model, restated by
`OwnerLocalFileIngressCoordinatorTest`'s and
`TierAOwnerInvocationCoordinatorTest`'s own already-adopted tests this
session) may each receive **independent** Memory Core references if the
owner separately, explicitly registers each one. This document invents
no deduplication and no replacement: a second registration act for a
second, later generation of the same source creates a second, new
`Provenance`/`Document` pair, pointing at the second `DerivativeGenerationId`
— it never overwrites, amends, or supersedes a `Document`/`Provenance`
pair already created for an earlier generation. Prior Memory Core
records remain exactly as durable and independently retrievable as
Memory Core's own append-only history already guarantees for every
other record kind.

## 15. Failure/sequencing result

Mirroring `EvidenceRegistrationCoordinator`'s own already-adopted
failure discipline exactly, with no invented distributed-transaction or
rollback mechanism:

- **Derivative admission succeeded but Memory registration is never
  attempted, or fails at the permission stage:** the `DerivativeGenerationRecord`
  remains exactly as durably admitted as it already was — this document
  creates no path by which a downstream registration failure
  retroactively invalidates, unpublishes, or reinterprets an already-
  admitted derivative. No governance reviewed requires such
  invalidation, and none is invented here.
- **`createProvenance`'s own permission gate rejects:** registration
  stops there; `registerDocument` is never called; the caller receives
  a truthful, non-exceptional outcome (mirroring `EvidenceRegistrationOutcome.ProvenanceNotAuthorised`'s
  own existing shape) — no `Provenance` and no `Document` exist.
- **`registerDocument`'s own permission gate rejects, after `createProvenance`
  already succeeded:** the `Provenance` record remains, durably,
  unreferenced by any `Document` (mirroring `EvidenceRegistrationOutcome.DocumentRegistrationNotAuthorised`'s
  own existing shape and its own documented rationale: "Memory Core's
  own history is append-only; nothing here attempts to roll it back").
- **The referenced source or derivative identity is missing or
  inconsistent:** this document does not invent a new consistency check
  beyond what already exists — `Provenance.extractedFrom`/`sourceIdentifier`
  simply record whatever identity genuinely exists (§7); a missing
  source `DocumentId` is recorded as absence (null), never fabricated.
- **Registration is repeated for the same generation:** not addressed
  by a new rule here — whatever duplicate-creation behavior Memory
  Core's own `createProvenance`/`registerDocument` already exhibit for
  any other repeated candidate governs identically; this document
  invents no derivative-specific deduplication or idempotency
  mechanism.
- **A later, corrected/reprocessed generation exists:** see §14 — an
  independent registration act, never an amendment of the earlier one.

No distributed transaction, saga, or rollback authority is created or
implied anywhere in this section.

## 16. Retrieval/RKS/QMD result

**Decision: no.** This document grants Document Ingestion no new Memory
Core retrieval capability of any kind. `MemoryRetrieval`'s existing
seven frozen modes (Memory Core Scope Lock §10) are entirely unaffected
— this document is about a *write* (registration), not a read. RKS/QMD
remains exactly the subordinate, separately-governed retrieval
infrastructure CDR-008 already confirms it is: a downstream mechanism
that may compute relevance over content already retrieved through
Memory Core's own unmodified interface, gaining no canonical authority
of its own, and this document's registration act creates no new
canonical status for anything RKS/QMD might later surface — a
registered derivative's `Document`/`Provenance` pair is exactly as
subject to Memory Core's existing seven-mode structural retrieval, and
exactly as un-privileged relative to ranked/semantic access, as any
other Memory Core record.

## 17. Deletion/retention result

**Deferred, exactly as Units 2, 5, and 6 already left it — not decided
here.** This document creates no new deletion mechanism and no new
retention policy. It restates, without narrowing or widening, the
already-adopted rules this act must respect if and when a deletion
mechanism is ever built:

- Deleting a `DerivativeGenerationRecord` (if such a capability is ever
  built) never authorizes silent mutation of any Memory Core record
  referencing it (Unit 2 §17's own "source deletion never authorizes
  silent derivative mutation," extended here by the same reasoning).
- Deleting a Memory Core record (if such a capability is ever built for
  `Document`/`Provenance`) never authorizes mutation of the
  `DerivativeGenerationRecord` it references — Document Ingestion's own
  records remain immutable and independently retrievable regardless of
  what happens on the Memory Core side (Unit 6 §17, restated).
- What retention policy, if any, should apply to a `Document`/`Provenance`
  pair referencing an ingestion identity is explicitly left to future,
  separate governance — not a question this document resolves or must
  resolve to be adopted.

## 18. Permission/write-gate result

**No new `PermissionAction` and no new `ResourceType` is required.**
Contract Design §10 states directly: "No new `PermissionAction` or
`ResourceType` value is required. Every check above is expressible with
the enum values that already exist in `src/contracts/Permission.kt` and
`src/contracts/Resource.kt` today." This document reuses the identical
existing `(PermissionAction.WRITE, ResourceType.MEMORY)` pair
`EvidenceRegistrationCoordinator`'s own `createProvenance`/`registerDocument`
gates already use, via two new, distinct, disclosed action-vocabulary
verb phrases (mirroring `EvidenceRegistrationCoordinator`'s own
`"memory.create-provenance"`/`"memory.register-document"` naming
convention, with a distinct namespace for this new caller — for
example, `"derivative.create-provenance"`/`"derivative.register-document"`,
not frozen here) — keeping this act separately auditable and
permission-decision-traceable from raw-evidence registration, exactly
as the Manifest Retrieval Scope Lock §17 and the Local File Ingress
Scope Lock §3 already established this identical pattern for two
unrelated pairs of adjacent acts. Whether the new verb phrases target
the same `ResourceId`s `EvidenceRegistrationCoordinator` already uses,
or new, distinct ones, is implementation-plan work, consistent with
"not registered anywhere by this Unit" — the precedent set by every
prior verb-phrase introduction in this programme.

**No parallel authorization system.** The new coordinator holds a
`PermissionEngine` reference for the identical, disclosed reason
`EvidenceRegistrationCoordinator` already does: `MemoryCore` cannot
self-gate (Memory Core Scope Lock §6), so nothing gates this act unless
the calling coordinator does.

## 19. Implementation impact map

| Surface | Classification |
| --- | --- |
| One new registration coordinator (§4), taking `MemoryCore` and `PermissionEngine` only | Required — exact class name/shape not frozen here |
| Two new action-vocabulary verb phrases (§18), mapped to the existing `(WRITE, MEMORY)` pair | Required — no new `PermissionAction`/`ResourceType` |
| One owner-facing `ParkerRuntime` entry point, mirroring `deleteEvidenceAsOwner`/`invokeTierAIngestionAsOwner`/`importEvidenceFileAsOwner`'s existing no-caller-principal shape, accepting a `DerivativeGenerationId` (or the full `Admitted` result) and returning a truthful, non-collapsing outcome | Likely required, given §5's explicit-owner-trigger decision; exact signature is implementation-plan work |
| Focused tests proving every rule in this document (coordinator dependency shape; no auto-trigger; field mapping; content-boundary; failure sequencing; reprocessing independence) | Required, later, alongside implementation |
| A `DerivativeGenerationId`-or-equivalent parameter shape for the new coordinator/entry point | Required; exact type not frozen |
| Not required by any reading of this document | Any change to `EvidenceRegistrationCoordinator`, `MemoryCore.kt`, `TierAOwnerInvocationCoordinator`, `OwnerLocalFileIngressCoordinator`, `DerivativeReviewRegistry`, any Tier A specialist, any OCR/Tier B change, any Knowledge Memory change, any new `PermissionAction`/`ResourceType`, any new deletion/retention mechanism |

## 20. Required / conditional / optional / forbidden classification

| # | Rule | Class |
| --- | --- | --- |
| 1 | A new, narrowly dedicated coordinator (not an extension of an existing one) performs registration | **R** |
| 2 | Registration is explicit, separate, owner-triggered — never automatic on admission | **R** |
| 3 | Only an `Admitted` (not `ReconciliationRequired` or any other variant) derivative is eligible | **R** |
| 4 | `sourceIdentifier`, `sourceType`, `acquisitionTime`, `contentNature=EXTRACTED`, `processingHistory` entry | **R** on `CandidateProvenance` |
| 5 | `extractedFrom` | **C** — only when the source was itself separately registered |
| 6 | `integrityInformation`/`integrityHash` | **C** — only for byte-backed derivatives |
| 7 | `creator`/`creatorPrincipalId`/`claimedCreationTime`/`confidence`/`sensitivity`/`derivedFrom`/`metadata` | **O** |
| 8 | Extracted parser payload copied into any Memory Core field | **F** |
| 9 | `Assertion` creation as part of this act | **F** |
| 10 | Knowledge Item creation or promotion as part of this act | **F** |
| 11 | Evidence Intelligence invocation as part of this act | **F** |
| 12 | OCR/Tier B invocation as part of this act | **F** |
| 13 | `DerivativeReviewRegistry` write, or `APPROVED` as a trigger | **F** |
| 14 | New `PermissionAction`/`ResourceType` | **F** |
| 15 | Deduplication of repeated registration or reprocessed generations | **F** |
| 16 | Retroactive invalidation of an admitted derivative on downstream registration failure | **F** |
| 17 | New Memory Core retrieval capability | **F** |
| 18 | Deletion/retention mechanism | **D** — deferred |

## 21. Adversarial challenge table

| # | Challenge | Resolution |
| --- | --- | --- |
| 1 | Automatic Memory promotion creep | Foreclosed by §5 — explicit, owner-triggered only, no automatic path exists |
| 2 | Automatic Knowledge promotion | Foreclosed by §10 — registration never triggers, requires, or implies Knowledge promotion |
| 3 | Registration mistaken for evidential validation | Foreclosed by §8/§9 — registration is provenance/reference metadata only; no evidential claim is made or implied; `Document`/`Provenance` creation asserts nothing about truth (Contract Design §6's own "never automatically truth") |
| 4 | `DerivativeReview` `APPROVED` accidentally becoming a write authority | Foreclosed explicitly by §5/§12, citing Unit 3 §8's own "no write path into Memory Core" directly |
| 5 | Duplicate source/derivative facts | Foreclosed by §8 — reference-only, never duplicated (Unit 6 §10's "reference over duplication, in both directions" restated) |
| 6 | Direct Document Ingestion → Memory write authority | Foreclosed by §9 — the new coordinator, not Document Ingestion generically, performs the write, gated by Memory Core's own existing boundary |
| 7 | Evidence Intelligence authority laundering | Foreclosed by §11 — this document authorizes only the already-adopted Tier A → Memory Core direct path; EI is neither invoked nor required |
| 8 | OCR/Tier B authority laundering | Foreclosed by §13 — no eligible record exists on the `RequiresTierB` path; nothing to register |
| 9 | Every-generation registration creating unwanted canonical noise | Foreclosed by §5 — explicit-trigger-only means registration volume is exactly what the owner chooses, never automatic bulk noise |
| 10 | Optional registration making required information unreachable | Not a defect: the derivative's own content remains reachable via the existing `invokeTierAIngestionAsOwner` synchronous return and `DerivativeGenerationStorage`, regardless of whether it is ever registered (§8); registration adds Memory Core discoverability, it is not the sole access path |
| 11 | Silent deduplication of reprocessed generations | Foreclosed by §14 — each generation, if registered, receives its own independent `Provenance`/`Document` pair |
| 12 | Replacement of prior Memory records | Foreclosed by §14/§15 — Memory Core's append-only history is never overwritten by this act |
| 13 | Loss of derivative lineage | Foreclosed by §7 — `processingHistory` points back at the exact `DerivativeGenerationId`; Document Ingestion's own lineage records (Unit 2) are entirely unaffected and remain the authoritative lineage source |
| 14 | Parser payload mistaken for Memory assertions | Foreclosed by §8 — explicit, named distinction between provenance metadata and extracted content; no `Assertion` is ever created by this act |
| 15 | Candidate propositions becoming canonical facts | Foreclosed by §8/§9 — no proposition of any kind is created by mechanical Tier A extraction or by this registration act |
| 16 | Missing-source/derivative handling | Addressed honestly in §7/§15 — a missing source `DocumentId` is recorded as absence, never fabricated; not a failure condition requiring invented handling |
| 17 | Partial downstream failure | Addressed in §15 — the already-admitted derivative is never retroactively invalidated; the two-gate sequence fails closed at whichever stage is reached |
| 18 | Rollback fiction | Foreclosed by §15 — no distributed transaction, saga, or rollback mechanism is invented or implied |
| 19 | Permission bypass | Foreclosed by §18 — both writes remain independently gated through the existing, unweakened `(WRITE, MEMORY)` pair |
| 20 | New identifier invention | Foreclosed by §7 — every field uses an existing `Provenance`/`Document` field and an existing enum value; no new identifier type is introduced |
| 21 | Memory Core becoming a second evidence custodian | Foreclosed by §9 — Memory Core holds only a reference, never custody, never the bytes, never the payload |
| 22 | QMD/RKS gaining canonical status | Foreclosed by §16, citing CDR-008 directly |
| 23 | Deletion cascade invention | Foreclosed by §17 — no cascade mechanism of any kind is created |
| 24 | Retention-policy invention | Foreclosed by §17 — explicitly deferred, not decided |
| 25 | Owner-trigger invention (an unsupported trigger mechanism) | Foreclosed by §5 — the owner-trigger pattern mirrors two already-adopted, identically-shaped precedents (`invokeTierAIngestionAsOwner`, `importEvidenceFileAsOwner`), not a novel invention |
| 26 | Background/automatic registration invention | Foreclosed by §5 — explicit only, no watcher, scheduler, or startup path is authorized or implied |
| 27 | Circular dependency between Memory Core and Document Ingestion | Foreclosed by §4/§9 — causality runs one way only (Memory Core references an already-existing, already-immutable ingestion identity); Document Ingestion's own records never gain a forward-pointing field into Memory Core (Unit 6 §10, restated); the new coordinator depends on `MemoryCore` and `PermissionEngine`, never the reverse |

No item resolves to a blocker. All twenty-seven challenges resolve
within this document's own sections.

## 22. Explicit deferrals

- `ReconciliationRequired`-eligible registration (§6) — pending future
  reconciliation-mechanics governance.
- Multi-parent/reconciliation-generation `derivedFrom` population (§7)
  — pending the same.
- Deletion/retention mechanism and policy for the registered
  `Document`/`Provenance` pair (§17) — pending future, separate
  governance, exactly as Units 2/5/6 already left it.
- Whether registration is ever offered for legacy artifacts admitted
  before this document existed — not addressed; no retroactive
  registration obligation is created.
- Exact Kotlin type/class names, `ResourceId` literals, and verb-phrase
  strings (§18-19) — implementation-plan work, consistent with every
  prior verb-phrase introduction in this programme.
- Whether the new owner-facing entry point accepts a bare
  `DerivativeGenerationId` or the fuller `Admitted` result — deferred to
  implementation planning; either satisfies this document provided no
  caller-supplied field (§7's table) substitutes for a value this
  document requires to be derived.

## 23. Conflicts discovered

**None.** Direct re-reading of Units 1-6, both accepted implementation
units this session, `MEMORY_CORE_CONTRACT_DESIGN.md`,
`MEMORY_CORE_SCOPE_LOCK.md`, CDR-004, and CDR-008 found no contradiction
with anything this document decides. No prior adopted document required
modification; none is modified by this document.

## 24. Governance impact classification

**A/B — clarification and narrow authority extension consistent with
existing authority. Not C.** No new `PermissionAction`/`ResourceType` is
introduced (§18); the new coordinator's authority is exactly what
Memory Core's own existing write gate already permits any caller
satisfying it (§9); Document Ingestion gains no authority beyond
constructing a candidate and calling the existing, unchanged
`MemoryCore` interface, mirroring `EvidenceRegistrationCoordinator`'s
own already-adopted precedent exactly; the capability authorized is
deliberately, narrowly bounded (one eligible derivative state, one
explicit trigger, reference-only content boundary) rather than a
general Document-Ingestion-to-Memory-Core write grant.

## Recommendation

**READY FOR OWNER REVIEW**
