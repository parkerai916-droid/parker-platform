# Document Ingestion — Derivative Content Persistence and Retrieval Scope Lock

## Status

**Draft for owner review. Not yet accepted, canonical, or
implementation-authorising.** Governance only. No Kotlin is implemented,
proposed as a diff, or changed by this document. No dependency is added.
No interface is implemented. No persistence technology is chosen beyond
naming the same file-per-identity, prepare/publish pattern
`FileSystemDerivativeGenerationStorage`/`FileSystemEvidenceSourceManifestStorage`
already use, as precedent, not as a frozen implementation. No storage
directory is created. No runtime, test, or Docker file is touched. Memory
Core, Knowledge, QMD, and RKS are not modified.

This document responds directly to the governance blocker raised by the
immediately preceding Durable Extracted-Content Retrieval investigation
(same baseline, `HEAD == origin/main == 3c7b7de2433d6d8efde64e87d222f47b184f85be`):
*"DERIVATIVE CONTENT PERSISTENCE AND RETRIEVAL SCOPE LOCK REQUIRED."*

**This document reopens, redesigns, or reinterprets none of:** CDR-006,
CDR-007, CDR-008, `DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md`'s
own frozen `DerivativeGenerationRecord` field-shape taxonomy (§20 there),
`SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`, `DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md`,
`DOCUMENT_INGESTION_MEMORY_CORE_CROSS_REFERENCE_SCOPE_LOCK.md`, or
`OCR_MECHANISM_UNIT_12_RUNTIME_INVOCATION_SCOPE_LOCK.md`'s own frozen
Tier B/Document-Ingestion-pipeline exclusion (§13 there). Where this
document's own conclusions rest on one of those documents, the citation
is exact and re-verified fresh, not assumed.

## 1. Executive Summary

This scope lock authorizes exactly one new capability: a durable,
subordinate storage representation of the structured content a Tier A
Document Ingestion generation already, truthfully produces, keyed
one-to-one to that generation's own already-governed `DerivativeGenerationId`,
retrievable later by an authorized owner action without re-running
extraction. It authorizes nothing else. Tier B/OCR durable content is
explicitly **deferred** (§15) — its inclusion here would require reopening
frozen Unit 12 governance this document has no authority to reopen.

## 2. Governing documents inspected (fresh, this unit)

- `DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md` — §§8-9
  (byte-backed vs. non-byte derivatives; content identity/digest), §20
  (field-shape classification: no content/payload category exists).
- `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` — §1 (hash-agility, "an envelope
  digest computed over a non-canonical or non-deterministic serialisation
  is not evidence of anything"), §2 ("derivative kind and content
  hash/length (**or a structured manifest hash when it has no single byte
  representation**)" — the existing, adopted seed for this document's own
  §6 resolution), Invariant I-11 (reprocessing is always a new generation,
  never in-place mutation), Invariant I-15 (source remains independently
  retrievable; derivative deletion/replacement cannot affect it).
- `DOCUMENT_INGESTION_PROGRAMME_GOVERNANCE_CLOSURE.md` — §10 ("persistence
  location (none) — explicitly out of scope in every unit's own Status
  section"); §8 Deferrals register (audit query/retrieval capability
  explicitly excluded by precedent, never extended — the direct governance
  analogue this document's own §11 retrieval-scoping decision follows).
- `DOCUMENT_INGESTION_PROGRAMME_IMPLEMENTATION_CLOSURE.md` — §17/§18
  (Tier B produces no `DerivativeGenerationRecord`, named exclusion), §20
  ("Reopening conditions" — resolving any §17 deferral "requir[es] its own
  separate, independently authorized governance and implementation unit").
- `DOCUMENT_INGESTION_MEMORY_CORE_CROSS_REFERENCE_SCOPE_LOCK.md` — §19
  ("must reference ingestion-owned identities by value only... never a new
  field, never duplicated content"), §22 (forbidden semantics, restated
  §16 below).
- `DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md` — §1.5 ("derivative
  content identity/hash **where applicable**" — capability list, not a
  frozen schema; content identity/hash, never content itself, is what
  Amendment 1 ever contemplated the record carrying).
- `OCR_MECHANISM_UNIT_12_RUNTIME_INVOCATION_SCOPE_LOCK.md` — §13 ("Tier
  B/OCR output does not use Document Ingestion's own
  `DerivativeGenerationId`/`DerivativeGenerationStorage`/`DocumentIngestionAudit`
  pipeline... at all... Explicitly not resolved here, and not needed to
  unblock Unit 12... left, as Alignment Amendment 5 §7 already leaves it,
  to future, separate Document Ingestion governance"); §19 item 12 ("Tier
  B output entering Document Ingestion's own
  `DerivativeGenerationRecord`/`DerivativeGenerationStorage`/`DocumentIngestionAudit`
  pipeline" classified **Forbidden**).
- CDR-006 — Evidence Custodian is "the sole intended custodian of
  preserved original evidence artefacts and the sole intended enforcer of
  their [immutability]"; unreopened.
- CDR-007 — Evidence Intelligence "transform[s] governed evidence into
  governed analytical products while preserving complete provenance";
  unreopened, uninvolved (this document concerns Tier A mechanical
  parsing, never analytical judgement).
- CDR-008 — Memory Core's seven modes, and the bar on any component
  quietly acquiring ranked/semantic/canonical authority over content,
  remain fully binding; irrelevant to a subordinate Document-Ingestion-owned
  content store, which is not Memory Core, does not delegate to it, and is
  not itself a semantic/ranked mechanism of any kind.

## 3. Authority model — frozen

The distinction stated in the governing task is confirmed correct by fresh
inspection and is adopted verbatim:

- **Source Evidence** — Evidence Custodian remains the sole authority for
  original admitted source bytes (CDR-006, unreopened).
- **Derivative Generation Record** — Document Ingestion remains
  authoritative for derivative-generation identity, lineage, producer
  identity, transformation history, completeness, warnings, and the other
  already-governed generation metadata (`DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md`
  §20, untouched, unamended).
- **Derivative Content** (new, this document) — Document Ingestion may own
  a subordinate durable representation of the content produced by one
  specific, already-admitted Tier A derivative generation.
- **Memory Core** — no bulk derivative-content storage; value-only
  cross-reference semantics unchanged (§16).
- **Knowledge** — no automatic promotion; untouched (§16).
- **QMD/RKS** — no canonical authority; no persistence authority over
  derivative content; untouched (§16).

## 4. Storage-authority decision

Three options were evaluated, per the governing task's own instruction not
to select the preferred candidate merely because it was preferred:

**A. Add full content to `DerivativeGenerationRecord`.** Rejected. The
Record's field-shape taxonomy (`DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md`
§20-A/B/C) is itself frozen, adopted governance — every category was
enumerated and closed; no category anywhere admits a content/payload
field, and §20-D forbids any field that would let a Derivative Generation
Record "assert or imply it is, or has become, an `EvidenceArtifact`."
Adding content directly to the Record would not be an additive extension
of that taxonomy — it would be a reopening and amendment of an already-frozen
document, which this narrow unit has no authority to perform. Rejected on
authority grounds, not convenience.

**B. A separate Derivative Content Store, subordinate to
`DerivativeGenerationId`.** **Adopted.** This is the only option that
adds a genuinely new, narrow capability without reopening or amending any
already-frozen document. The Record's own taxonomy is untouched — it may
continue to carry, at most, a content *digest* exactly where §9 of the
Record Scope Lock already conditionally allows one; the actual content
lives in a wholly separate store this document alone authorizes,
subordinate to (never independent of) the Record's own identity.

**C. Another already-governed existing store (Evidence Custodian, Memory
Core, Evidence Source Manifest Storage).** Rejected for each: Evidence
Custodian is sole custodian of *original source bytes* only (CDR-006) —
placing derivative content there would misrepresent it as source evidence,
the exact confusion §5 exists to forbid. Memory Core is explicitly barred
from duplicating ingestion-derived content (§19 of the Cross-Reference
Scope Lock, restated §16 below). Evidence Source Manifest Storage's own
governed purpose is the *source* manifest (received/detected media type,
digest, byte length of the *original* artifact) — unrelated in kind and
authority to derivative content, and not extended here.

**Cardinality freeze.** Exactly one persisted content representation per
`DerivativeGenerationId`: `DerivativeGenerationId → one content
representation`. Each generation already denotes one specific admission
attempt (`DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md`
§6, generation immutability); nothing about a single generation's own
content requires — or would make honest sense as — more than one stored
representation. Reprocessing produces a new `DerivativeGenerationId`
(Provenance Model I-11) and therefore a new, independent content entry;
this is not an exception to the 1:1 rule, it is the rule applied to a new
identity. The content store creates no semantic identity of its own
detached from the generation it belongs to — a content entry with no
corresponding published, valid Record is never a retrievable generation
(§9, §13).

## 5. Content status — precisely defined

Persisted derivative content **IS**: a durable storage representation of
the structured output a specific, already-governed Tier A derivative
generation truthfully produced, subordinate to that generation's own
`DerivativeGenerationId`, and traceable through it to the generation's
recorded root `EvidenceArtifactId`.

Persisted derivative content **IS NOT**: original source evidence; a
replacement or new `EvidenceArtifact`; canonical truth about the source
(`SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` — "[a derivative] carries no
source authority"); Memory; Knowledge; a QMD/RKS record; or a reasoning
conclusion of any kind. It is exactly, and only, what the specialist
extractor already, truthfully claims to have produced — nothing added,
nothing inferred, nothing embellished for storage's sake.

## 6. Serialization / content-identity decision

**No canonical serialization is declared for any structured Tier A
payload kind** (`DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md`
§9 explicitly defers this; this document does not reopen that deferral —
it resolves the narrower, different question of *storage*, not semantic
canonicity).

**Versioned Storage Representation (new concept, this document).** For
each Tier A payload kind (PDF/CSV/EML/DOCX), a future implementation unit
may define a versioned encoding — a **Derivative Content Representation,
version N** — whose sole purpose is to durably persist and later
reconstruct the specialist's own already-produced field values (§7). This
representation is explicitly, permanently **not canonical**: it is one
particular storage encoding of already-known field values, not a claim
about the one true byte form of the semantic content. This mirrors
`SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` §2's own already-adopted language
almost exactly — "a structured manifest hash when it has no single byte
representation" — this document simply names and scopes that already-
anticipated concept for the derivative-content case specifically.

**Digest.** A SHA-256 digest MAY be computed and recorded over the
Versioned Storage Representation's own serialized bytes. This digest
proves **exactly one thing**: the persisted storage bytes have not been
altered or corrupted since they were written — an integrity check on the
storage artifact itself. It proves **none** of: source-byte identity
(that remains Evidence Custodian's own digest, on the source artifact,
unrelated); canonical derivative identity (no canonical form is declared,
§ above); evidential truth (Tier A performs no evidential judgement,
`SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` I-13, unaffected); or semantic
equivalence across representation versions (a version-N-to-version-(N+1)
migration, if ever governed, is a distinct question this document does
not resolve, §8). This is precisely the "content digest, only where a
governed canonical serialization exists" field the Record Scope Lock §20-B
already conditionally allows — except here the digest lives on the
**content entry**, describing the content entry's own storage integrity,
not asserted as a property of the Record or of the semantic derivative
itself.

## 7. Format coverage

The Versioned Storage Representation for each kind persists exactly the
fields the already-governed, unmodified specialist output types already,
truthfully produce — nothing invented, nothing redefined:

**PDF** (from `PdfStructuralResult`, unmodified): `documentText`;
`pageCount`; `pageTextAssociationAvailable`; `metadata` (name/value/
representation triples); `embeddedResources` (observations only);
`producerIdentity`; `transformationHistory`; `completenessState`;
`warnings`.

**CSV** (from `CsvStructuralResult`, unmodified): `headers`; `rows`
(the full, ungoverned-for-preview-only row set — display-layer truncation,
if any, remains a presentation-layer concern this document does not
reach; the *stored* representation is complete); `delimiter`;
`quoteCharacter`; `lineEnding`; `producerIdentity`; `transformationHistory`;
`completenessState`; `warnings`.

**EML** (from `EmlStructuralResult`, unmodified): parsed headers
(`from`/`to`/`cc`/`rawDate`/`parsedDate`/`subject`/`messageId`/
`mimeVersion`/`contentType`, plus the full `headers` list); `mimeEntities`;
`bodyAlternatives` (decoded text, never raw attachment bytes beyond what
the specialist already exposes as candidate metadata); `attachmentCandidates`
(candidate metadata as the specialist already produces it — filename,
declared MIME type, digest, byte length, transformations; storing an
attachment candidate's own decoded bytes durably is **not** authorized by
this document, which concerns Tier A *structural* content, not a new
byte-backed child-source acceptance path — that remains
`SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`'s own separately governed
acceptance boundary, untouched); `producerIdentity`;
`transformationHistory`; `completenessState`; `warnings`.

**DOCX** (from `DocxStructuralResult`, unmodified): `paragraphs`;
`tables`; `headers`/`footers` (header/footer paragraph structures);
`metadata`; `parts` (OOXML part inventory); `relationshipCount`;
`relationshipTypes`; `mediaPartNames` (names/observations only — the media
parts' own bytes are not durably persisted by this document); `producerIdentity`;
`transformationHistory`; `completenessState`; `warnings`.

No layout, coordinate, numbering-rendering, or visual-fidelity semantics
are invented for any format — this document persists exactly what the
already-governed specialist already, truthfully claims, in the same shape
it already claims it.

## 8. Versioning

Every persisted content entry carries an explicit `representationVersion`
(an integer, or equivalent monotonic identifier) naming which Versioned
Storage Representation schema (§6) encoded it, per format kind. Required:

- **Old content remains identifiable.** A stored entry's own
  `representationVersion` is permanent, immutable metadata of that entry,
  never inferred from context.
- **Unsupported versions fail closed.** A future implementation reading a
  `representationVersion` it does not recognize must produce an explicit,
  honest, non-fabricated failure — never a best-effort guess, never a
  silent downgrade, never a fabricated "recovered" content value.
- **No silent rewriting.** A schema-version upgrade never rewrites
  historical entries in place. A generation remains associated with
  whichever representation version was originally used to admit it,
  permanently, unless a separately governed future migration unit
  explicitly re-encodes it — and even then, per §10 (Immutability), the
  original generation's own identity and history are never mutated; a
  migration, if ever authorized, would itself need its own governance
  determining whether it produces a new generation or a genuinely
  in-place re-encoding of unchanged semantic content, a question this
  document does not resolve and does not need to for initial persistence
  to proceed.

## 9. Write / admission semantics

**Sequencing (frozen).** Content is prepared and published to durable
storage **before** its corresponding `DerivativeGenerationRecord` is
prepared or published, mirroring `FileSystemDerivativeGenerationStorage`'s
own existing `prepare → publish` atomic-rename pattern (temp file →
`.prepared` staging → atomic rename to final location), applied once more
for the content entry, then once more for the Record itself. This
ordering is the load-bearing invariant behind the requirement that
**Parker must never report a derivative generation as durably retrievable
if its required persisted content is absent** — because the Record (the
thing an owner-facing retrieval enumerates or resolves through) is never
published until its content already durably exists.

**The five listed failure combinations, resolved under this ordering:**

- **Content prepared, Record fails.** The Record was never even attempted
  (content-first ordering) — this combination does not arise under this
  design; if content preparation alone leaves temporary staging state, it
  is cleaned up exactly as `FileSystemDerivativeGenerationStorage`'s own
  existing `.tmp` discipline already does for the Record today. No
  observable effect; nothing was ever reported admitted.
- **Record prepared, content fails.** Does not arise under content-first
  ordering — the Record is never prepared until content publication has
  already succeeded.
- **Record published, content publication fails.** Cannot occur under
  this ordering — content publication is a strict precondition of Record
  preparation, which is itself a strict precondition of Record
  publication.
- **Content published, Record publication fails.** The one genuinely
  reachable failure window under this design. Result: a published,
  durable content entry with no corresponding published Record. Because
  retrieval always resolves through the Record first (§11), this
  generation was **never reported as admitted** — the owner never sees
  it, exactly as if the whole attempt had failed. The orphaned content
  entry itself is classified as **requiring reconciliation** (§13) — the
  same honest, non-fabricated disclosure discipline Document Ingestion
  Unit 4 already established for the structurally comparable Record-only
  two-stage sequence, extended narrowly to also recognize "content exists,
  Record does not" as a reconciliation case a future implementation's
  existing reconciliation mechanism must sweep. This document does not
  invent automatic sweep behaviour, cross-filesystem distributed
  transactions, or rollback fiction of any kind — it only requires that
  the state be named truthfully and never silently presented as a valid,
  retrievable generation.
- **Restart during publication.** Both the content store and the Record
  store use the identical, already-proven atomic-rename pattern; an
  interrupted publish leaves, at most, orphaned `.tmp`/`.prepared` staging
  state on one side, never a corrupted or partially-visible final entry
  (the OS-level rename is atomic; a process restart cannot observe a
  half-renamed file). This is the same restart-safety property
  `FileSystemDerivativeGenerationStorage` already has today, extended
  once more, not reinvented.

## 10. Immutability

Content for a given `DerivativeGenerationId` is **write-once**. Once
successfully published, it is never silently replaced, appended to, or
mutated in place — mirroring `DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md`
§6's identical rule for the Record itself. Reprocessing the same
`EvidenceArtifact` produces a **new** `DerivativeGenerationId` (already
governed, Provenance Model I-11) and therefore a new, independent content
entry — never an in-place overwrite of a prior generation's own content,
regardless of whether the newly produced content happens to be identical.

## 11. Retrieval

**Scope, deliberately narrow.** This document authorizes retrieval **by
already-known identity only** — an owner (or the owner-facing UI acting
on the owner's behalf) who already possesses both a `EvidenceArtifactId`
and a `DerivativeGenerationId` (for example, from the same session's own
prior Upload/Process response) may retrieve that generation's persisted
content. This document does **not** authorize a new "list every
generation for this evidence item" enumeration/browse/query capability.
That is a materially different capability — direct governance analogue:
`DOCUMENT_INGESTION_PROGRAMME_GOVERNANCE_CLOSURE.md` §8's own audit-query
row, "Precedent (`EvidenceDeletionAudit`) explicitly excludes query,
extending it was out of scope... implementation planning may NOT assume
that any query capability exists or is planned" — and remains its own,
separate, future, non-authorized question.

**Mismatch behaviour (frozen).** A retrieval request naming a
`DerivativeGenerationId` whose own Record's `rootSourceEvidenceArtifactId`
does not equal the `EvidenceArtifactId` supplied in the same request must
fail closed with an explicit, honest "mismatch" outcome — never silently
substitute, coerce, or return content associated with a different root
than the one named. This directly implements the governing task's own
"a generation requested under the wrong source identity must fail closed"
requirement and mirrors the already-adopted discipline
`TierAOwnerInvocationOutcome`'s own family of distinct, non-collapsed
failure outcomes already applies elsewhere in this same subsystem.

**Unknown-identity behaviour.** An unknown `EvidenceArtifactId` and an
unknown `DerivativeGenerationId` must each produce their own distinct,
honest "not found" outcome — never conflated with each other, and never
conflated with the mismatch outcome above (three genuinely different
facts, three genuinely different disclosures).

## 12. Owner-authority decision

Retrieval must be gated by the same structural discipline
`OwnerEvidenceOperations`'s existing three methods already establish:
**no caller-supplied principal parameter of any kind** — the retrieval
operation resolves the owner identity internally, exactly as
`processTierA`/`processTierB` already do, foreclosing caller-substitution
by construction rather than by convention. Whether an existing
`PermissionAction`/`ResourceType` pairing already covers "read Document-Ingestion-owned
derivative content," or a future implementation-plan unit must define a
new, narrow one, is left to that future unit to determine against the
current `PermissionEngine`/`ResourceType` source directly — this document
does not invent or presuppose a specific enum value, consistent with its
own "do not freeze class names unless governance requires them"
discipline (§17). No anonymous retrieval is authorized under any
circumstance. This document does **not** authorize, and takes no position
on, general reasoning-provider access to persisted content — external
reasoning submission remains entirely separately governed (Evidence
Intelligence's own contract, untouched, uninvolved by this document,
which concerns Tier A mechanical output only).

## 13. Retention / deletion / purge — minimum safe semantics

**This document does not resolve historical-generation retention/purge
policy** — that remains exactly as deferred as it already is for the bare
Record today (`DOCUMENT_INGESTION_PROGRAMME_GOVERNANCE_CLOSURE.md` §8; no
new deferral is created, none of the existing ones is resolved). The
minimum required to safely authorize *creating* the new store, without
deciding the full policy:

- **Shared lifecycle with the Record.** Content and Record are published
  as a pair (§9); for as long as no future deletion mechanism exists,
  they persist together, with no independent retention clock for content
  alone.
- **Source deletion does not, by this document, cascade to derivative
  content.** Whether it *should*, in a future governed deletion
  mechanism, remains exactly as unresolved as "Memory/Knowledge deletion
  propagation to an ingestion-owned reference" already is
  (`DOCUMENT_INGESTION_PROGRAMME_GOVERNANCE_CLOSURE.md` §8) — this
  document neither authorizes nor forecloses that future decision.
  `EvidenceCustodian`'s own sole deletion authority over source bytes
  (CDR-006) is restated, not touched: **Document Ingestion may not delete
  source evidence**, under any circumstance, as a side effect of anything
  this document authorizes.
- **Tombstone capability preserved, not decided.** If a future, separately
  governed deletion mechanism ever deletes persisted derivative content,
  the `DerivativeGenerationRecord` may survive as an audit/provenance
  tombstone recording that a generation existed and that its content was
  deleted — mirroring how `EvidenceDeletionAudit`/`OwnerEvidenceDeletionAuthority`
  already preserve an audit trail independent of the underlying bytes
  they authorize deleting. This document does not authorize any deletion
  mechanism itself; it only ensures the storage design does not foreclose
  a future one from being built safely.
- **Orphan content is forbidden as a permanent state.** The one reachable
  orphan case this document's own write ordering produces (§9, "content
  published, Record publication fails") must be reconciled — swept by
  whatever reconciliation mechanism a future implementation unit already
  needs for the structurally identical Record-only case — never left
  indefinitely as unaccounted-for, inaccessible-but-present bytes.
- **Interrupted-deletion recovery** is not resolved here, because no
  deletion mechanism is authorized here. A future deletion-mechanism unit
  owns that question entirely, exactly as it owns whether deletion is
  authorized at all.

## 14. Sensitive-content controls

Derivative content may contain the same volume and sensitivity of
information as the source evidence it was extracted from — full document
text, personally identifying information, case-relevant material —
because it *is* that information, truthfully re-expressed. A future
implementation must therefore observe, as a direct consequence of this
document (not a new authority, a constraint on how the authorized
capability is built):

- filesystem access to the content store restricted exactly as narrowly
  as the existing Derivative Generation/Evidence Source Manifest stores
  already are (owner-process-only, no wider);
- full content is never logged, at any log level, under any
  circumstance — mirroring this repository's own already-established
  "structural facts only, never conversation/document content" logging
  discipline;
- no secret, token, or credential value ever appears in a content entry
  or its surrounding metadata (content is document text; this is a
  restatement of an existing, unrelated discipline for completeness, not
  a new risk this document introduces);
- exception/error messages arising from content storage or retrieval
  never include the content itself, a server filesystem path, or a stack
  trace to any owner-facing surface — mirroring the already-implemented
  discipline `OwnerEvidenceHttpServer.kt`'s own JSON error responses
  already follow;
- reads and writes are bounded — a future implementation must define an
  explicit maximum stored-representation size per content entry,
  informed by (not necessarily identical to) the already-governed 8
  Mi-character PDF extraction bound and the existing 64 MiB source-ingress
  bound, so that persistence itself cannot become an unbounded-growth or
  resource-exhaustion vector;
- corruption is detected via the storage-integrity digest (§6), and a
  digest mismatch on read must fail closed with an honest "corrupt
  content" disclosure, never a partial or best-effort return of
  possibly-damaged data;
- an unsupported `representationVersion` fails closed (§8), never a
  guess.

**No claim of encryption-at-rest is made.** This document does not adopt
encryption-at-rest for the new store, because none is currently
implemented or supported anywhere in this repository's own persistence
layer (the existing Evidence Custodian, Derivative Generation, and
Evidence Source Manifest stores are equally unencrypted at rest); claiming
otherwise here would misstate the actual, adopted security posture.

## 15. Tier B / OCR decision — deferred

**Tier B durable content persistence is explicitly deferred to a future,
independent scope lock. It is not authorized by this document.**

This is not a convenience shortcut; it is the only outcome fresh
inspection of `OCR_MECHANISM_UNIT_12_RUNTIME_INVOCATION_SCOPE_LOCK.md`
permits. §13 of that already-frozen, adopted document states, in terms
that admit no reinterpretation: *"Tier B/OCR output does not use Document
Ingestion's own `DerivativeGenerationId`/`DerivativeGenerationStorage`/`DocumentIngestionAudit`
pipeline, or its `prepare → ADMISSION_AUTHORISED → publish → ADMITTED`
sequence, at all... Explicitly not resolved here, and not needed to
unblock Unit 12: whether Document Ingestion may, in some future,
separately governed unit, construct its own `DerivativeGenerationRecord`
disclosing that an externally-obtained Tier B/OCR result relates to a
source it ingested... left, as Alignment Amendment 5 §7 already leaves
it, to future, separate Document Ingestion governance."* §19 item 12 of
the same document classifies "Tier B output entering Document Ingestion's
own... pipeline" as **Forbidden**. `DOCUMENT_INGESTION_PROGRAMME_IMPLEMENTATION_CLOSURE.md`
§18 independently confirms the same exclusion by name.

Including Tier B in this document's own authorized capability would
require reopening and amending Unit 12's own §13 exclusion — a
substantially larger governance act than "define a storage representation
for Tier A's already-produced payload," and squarely the kind of "casually
make Tier B look like Tier A merely for implementation convenience" the
governing task itself warned against.

**Consequences of deferral, stated plainly:**

- Tier A durable derivative-content persistence and retrieval **may
  proceed independently**, fully authorized by this document once
  adopted; it depends on nothing Tier B-related.
- Tier B/OCR content **remains transient**, exactly as it is today —
  `TransientOutput`, dispatched to the content-free `NotDispatched`
  marker, never durable.
- The browser owner UI **can still display its immediate OCR result**
  (the existing, unmodified, already-accepted `View Extracted Content`
  presentation, populated from the single `analyseEvidence` call's own
  in-memory response) — this document changes nothing about that already-
  working, already-accepted capability.
- **Durable OCR retrieval** — recalling a scanned PDF's or image's
  recognized text after the original response is gone, or after a restart
  — requires its own, subsequent, independently authorized Tier B
  Derivative Generation governance unit, which must itself resolve
  whether and how Tier B ever gains a `DerivativeGenerationRecord` at all
  (Unit 12 §13's own named open question) before this document's own
  content-storage pattern could even apply to it.

## 16. Memory / Knowledge / QMD / RKS — explicit non-effects

This document does not, and no future implementation it authorizes may,
without its own separate governance:

- register derivative content into Memory Core;
- copy derivative content into Memory Core (`DOCUMENT_INGESTION_MEMORY_CORE_CROSS_REFERENCE_SCOPE_LOCK.md`
  §19: "never a new field, never duplicated content" — restated, not
  narrowed, not widened);
- promote derivative content to Knowledge;
- index derivative content into QMD;
- give RKS canonical or persistence authority over derivative content
  (CDR-008 Invariant, restated by the Cross-Reference Scope Lock §22);
- submit derivative content to an external reasoning provider;
- perform, or imply, any case analysis.

Every one of those remains its own, separately governed action, entirely
untouched by this document.

## 17. Future implementation boundary (illustrative only)

If adopted, the minimum future implementation surface this document
contemplates — no name below is frozen; a future implementation-plan unit
may choose differently provided it satisfies the semantics this document
fixes:

- a `DerivativeContentStorage`-shaped interface (`prepare`/`publish`/`retrieve`,
  mirroring `DerivativeGenerationStorage`'s own existing shape) keyed by
  `DerivativeGenerationId`;
- a filesystem-backed implementation following the identical
  temp-file/`.prepared`/atomic-rename pattern `FileSystemDerivativeGenerationStorage`
  already uses;
- one versioned codec per Tier A format kind (§7/§8);
- composition/configuration wiring analogous to the existing
  `derivativeGenerationStorageRootPath`-style config key;
- a write coordinator sequencing content-then-Record (§9), most naturally
  as a narrow extension of whichever coordinator already performs Tier A
  admission today;
- a retrieval coordinator/adapter exposing retrieval-by-known-identity
  (§11) through the same structural, no-caller-principal discipline
  `OwnerUiEvidenceRuntimeAdapter` already uses (§12);
- an HTTP/browser-UI retrieval surface reusing the existing owner
  authentication/authorization path, presenting content through the
  already-accepted `View Extracted Content` panel rather than a new UI
  surface;
- focused tests covering persistence, restart/recovery, reprocessing
  independence, and every named failure/corruption case (§18).

## 18. Future acceptance requirements

A future implementation unit relying on this scope lock must
demonstrate, at minimum, all of:

1. process a deterministic Tier A fixture;
2. persist its Record and its content;
3. restart Parker;
4. retrieve the content without re-running extraction;
5. exact `documentText` (for PDF) preserved byte-for-byte;
6. producer identity, completeness, and warnings preserved unchanged;
7. process the same source a second time;
8. the second attempt mints a distinct `DerivativeGenerationId`;
9. both generations independently retrievable after restart;
10. neither generation's content is overwritten by the other;
11. a corrupted stored content entry fails closed, honestly, on retrieval;
12. an unsupported `representationVersion` fails closed, honestly, on
    retrieval;
13. a retrieval naming a mismatched `EvidenceArtifactId`/`DerivativeGenerationId`
    pair fails closed, honestly (§11);
14. no Memory Core, Knowledge, QMD, or RKS side effect occurs anywhere in
    the above;
15. storage and retrieval are demonstrably bounded (§14);
16. whatever minimum retention/deletion behaviour this document actually
    fixes (§13 — tombstone-capable, no permanent orphans, source deletion
    authority unchanged) is proven, without requiring the still-deferred
    full retention policy to itself be resolved.

## 19. Adversarial review

| Attack | Finding | Where addressed |
| --- | --- | --- |
| Duplicate evidence authority (content mistaken for source) | Rejected — §5 defines content as never source, never a replacement `EvidenceArtifact`; storage authority (§4) is a wholly separate, subordinate store, never Evidence Custodian's own | §4, §5 |
| `DerivativeGenerationRecord` scope creep | Rejected — Option A (content in the Record) explicitly rejected on authority grounds; the Record's own frozen taxonomy is never touched | §4 |
| Memory Core misuse as bulk content store | Rejected outright by existing, unamended governance; restated, not reopened | §16 |
| Canonical-serialization smuggling | Rejected — §6 explicitly, repeatedly declares the Versioned Storage Representation non-canonical; the deferred canonical-serialization question (Record Scope Lock §9) is not reopened, resolved, or quietly answered by implication | §6 |
| Digest overclaim | Rejected — §6 states exactly the one fact the digest proves (storage-representation integrity) and exactly four facts it does not prove | §6 |
| Mutable historical generations | Rejected — write-once per `DerivativeGenerationId`, restated from the Record's own existing immutability rule; reprocessing always mints a new identity | §10 |
| Orphan content | Acknowledged as one reachable, narrow failure window (§9) — never silent, never permanent, explicitly requires reconciliation, never presented as a valid retrievable generation | §9, §13 |
| Ungoverned deletion | Rejected — no deletion mechanism is authorized by this document at all; only a tombstone *capability* is preserved for a future, separately governed mechanism; Evidence Custodian's sole source-deletion authority is restated, not touched | §13 |
| Sensitive-content leakage | Addressed directly — no full-content logging, no content in errors/paths/stack traces, bounded reads/writes, no false encryption-at-rest claim | §14 |
| Path-based retrieval | Rejected — retrieval is exclusively by governed Parker identity (`EvidenceArtifactId`/`DerivativeGenerationId`); no browser-supplied filesystem path of any kind is authorized anywhere in this document | §11 |
| Reprocessing overwrite | Rejected — §10; a new `DerivativeGenerationId` per reprocessing attempt structurally forecloses overwrite | §10 |
| Unsupported-version ambiguity | Rejected — explicit fail-closed requirement, no guessing, no silent downgrade | §8, §18 items 12 |
| Tier B accidentally included without authority | Explicitly, deliberately excluded, with the exact frozen citation blocking its inclusion named in full — not merely asserted | §15 |
| Reasoning-provider authority expansion | Rejected — explicitly, separately disclaimed; external reasoning submission remains entirely outside this document's authority | §12 |
| QMD/RKS authority expansion | Rejected — explicit non-effect, restating CDR-008's own already-adopted boundary | §16 |
| Unauthorized new enumeration/query capability | Rejected — §11 deliberately scopes retrieval to known-identity only, explicitly declining the broader "list all generations" capability, citing the audit-query precedent that already forecloses inventing query capability without its own authorization | §11 |

## 20. Constitutional self-certification

| Authority | Check | Result |
| --- | --- | --- |
| CDR-006 | Evidence Custodian sole source authority | Unreopened; §4/§5/§13 restate, never narrow, its scope |
| CDR-007 | Evidence Intelligence owns analytical judgement | Untouched; this document concerns Tier A mechanical output only |
| CDR-008 | Memory Core boundary; no component quietly gains ranked/semantic/canonical authority | Untouched; §16 restates verbatim |
| Derivative Generation Record Scope Lock | Frozen field-shape taxonomy | Unamended; Option A rejected precisely to preserve this (§4) |
| Source/Derivative Provenance Model | Hash-agility; no fabricated canonical digest; independent source retrievability | §6 extends its own already-adopted "structured manifest hash" language; §5/§13 preserve I-15 |
| Canonical Governance Alignment | Amendment 1's own "content identity/hash where applicable" capability | Restated, not widened — content itself was never contemplated on the Record; this document supplies the missing subordinate store instead |
| Memory Core Cross-Reference Scope Lock | No duplicated content; value-only reference | §16 restates verbatim |
| OCR Mechanism Unit 12 Runtime Invocation Scope Lock | Tier B excluded from the Document Ingestion pipeline | §15 names and honors this exclusion in full, does not reopen it |
| Programme Governance Closure | No persistence location previously authorized; audit query capability precedent | §4 authorizes the new, narrow location this document itself creates; §11 follows the query-capability precedent rather than inventing enumeration |
| Programme Implementation Closure | Tier B produces no Record; reopening any §17 deferral needs its own separate unit | §15; this document is itself that separate unit, for Tier A only |

## Final Recommendation

**READY FOR OWNER REVIEW** (scope-lock stage). No code implementation is
authorized until this document is itself adopted; adoption does not
itself authorize Tier B, retention/deletion policy resolution, a new
Memory Core/Knowledge/QMD/RKS capability, or a general query/enumeration
surface — each remains exactly as separately governed as it was before
this document.
