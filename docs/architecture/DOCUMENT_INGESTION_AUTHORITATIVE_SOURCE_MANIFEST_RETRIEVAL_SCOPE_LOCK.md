# Document Ingestion — Authoritative Source Manifest Retrieval Scope Lock

## Status

**Draft for owner review. Not yet accepted. Governance/architecture only —
no code implemented.** Resolves the governance blocker the Owner-Facing
Tier A Runtime Invocation Boundary's authorization hard gate discovered:
`TierADocumentSourceContext` (`src/interfaces/TierADocumentIngestionRouter.kt`)
requires `evidenceArtifactId`, `content`, `expectedSha256`,
`receivedMediaType`, and `originalFileName`, but no governed, retrievable,
independently-persisted manifest containing these facts exists today. This
document defines the minimum governed authority and retrieval model. It
does not implement it, does not begin runtime invocation, does not touch
Tier B/OCR, and does not ingest real evidence.

## 1. Purpose

Determine, without implementing, the minimum governed source-manifest
authority and read-only retrieval model that a later, separately
authorized Owner-Facing Tier A Runtime Invocation Boundary unit needs in
order to truthfully construct a `TierADocumentSourceContext` for an
already-custodied `EvidenceArtifact` — closing the specific gap `git log`
and fresh code inspection confirm exists today, while preserving every
already-adopted authority boundary (Evidence Custodian exclusivity,
Memory Core's reference-only relationship, Document Ingestion's own
narrower authority, and the frozen source-immutability invariant).

## 2. Blocker being resolved

Fresh code inspection (`src/interfaces/TierADocumentIngestionRouter.kt`,
`src/runtime/DerivativeGenerationCoordinator.kt`, `src/interfaces/EvidenceCustodian.kt`,
`src/interfaces/EvidenceArtifactStorage.kt`, `src/interfaces/MemoryCore.kt`)
confirms all eight of the following findings verbatim:

1. **Confirmed.** `EvidenceCustodian.retrieve` returns
   `EvidenceRetrievalResult.Found(evidenceArtifactId, content)` — identity
   and bytes only. No digest, length, or media-type fact accompanies it.
2. **Confirmed.** `AcceptedEvidenceArtifact` (`src/interfaces/EvidenceCustodian.kt`)
   is exactly two fields — `evidenceArtifactId`, `acceptedAt` — by
   deliberate, documented design (frozen conclusion, restated across the
   whole Document Ingestion programme). It carries no digest, length, or
   media-type field.
3. **Confirmed.** `EvidenceArtifactStorage` (`src/interfaces/EvidenceArtifactStorage.kt`)
   exposes exactly `write`/`read`/`delete` — a pure byte primitive, with
   no manifest concept, digest computation, or metadata retrieval of any
   kind.
4. **Confirmed.** `Document.integrityHash` (`src/interfaces/MemoryCore.kt`,
   line 396) is `val integrityHash: String? = null` — optional, not
   mandatory.
5. **Confirmed.** `Document.metadata` is `Map<String, String> = emptyMap()`
   — an open, free-form map. No canonical key for a received media type
   is established anywhere in Memory Core's own frozen contract.
6. **Confirmed.** Nothing in `EvidenceCustodian.accept` requires or implies
   a subsequent `EvidenceRegistrationCoordinator.register`/`MemoryCore.registerDocument`
   call. `accept` and Memory Core registration are independently callable;
   an `EvidenceArtifactId` can exist with zero linked `Document`.
7. **Confirmed, and the strongest evidence for this whole document's
   purpose.** Fresh reading of `GovernedTierADocumentIngestionRouter.detect`
   (`src/runtime/GovernedTierADocumentIngestionRouter.kt`) shows
   mechanical signature detection exists for PDF (`%PDF-` magic),
   PNG (8-byte signature), and DOCX (ZIP + `[Content_Types].xml` +
   `word/document.xml`), and structural detection for EML (MIME headers).
   **No mechanical detection path exists for CSV at all** — `route()`
   can only select `TierADocumentFormat.CSV` if the *declared*
   `receivedMediaType` (after `detect()` returns `null`) equals
   `"text/csv"` exactly. CSV routing is entirely dependent on an
   authoritative declared media type; there is no content-based fallback.
8. **Confirmed.** `DerivativeGenerationCoordinator.ingestCsv`/`ingestEml`/`ingestDocx`/`ingestPdf`
   (`src/runtime/DerivativeGenerationCoordinator.kt`) each compute
   `sha256(source.content)` and compare it against `source.expectedSha256`
   — supplied by the *same* caller, from the *same* retrieval. If a
   runtime invocation boundary computed `expectedSha256` from the bytes
   it had just retrieved and was about to pass through, the check would
   always trivially pass regardless of whether the retrieved bytes were
   genuinely, uncorruptedly the original custodied content — a tautology
   providing zero integrity assurance. `expectedSha256` must originate
   from a source independent of the retrieval it verifies.

No finding requires correction.

## 3. Existing authority baseline

Fresh-read this unit, precedence noted explicitly:

- **CDR-006** and `EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md` (frozen) fix
  Evidence Custodian's exclusive custody/immutability authority.
  `EVIDENCE_CUSTODIAN_SCOPE_LOCK.md` (frozen) implements it without
  expansion — verified directly against its own Section 11 Verification
  Question 1 ("Does this Scope Lock implement rather than expand the
  Contract Design? Yes").
- **`EVIDENCE_CUSTODIAN_SCOPE_LOCK.md` §9 ("Implementation Independence")**
  — load-bearing for this document's own classification (Section 24
  below) — states explicitly: "This document does not specify... hashing
  algorithms... Every one of these belongs to a later governance stage —
  an Implementation Plan, not yet begun and not authorised to begin by
  this document." §10 ("Change Control") states a change "that merely
  selects among already-authorised implementation options (Section 9)
  does not require" full CDR/owner-constitutional-approval process. A
  digest/manifest scheme is precisely such an already-anticipated,
  not-yet-made implementation-level selection.
- **`EVIDENCE_CUSTODIAN_PHASE_7_BOUNDARY_CLARIFICATION.md`** (adopted)
  establishes the precedent this document reuses directly: deletion
  authority is a separate, narrow, structurally isolated interface
  (`OwnerEvidenceDeletionAuthority`), never folded into `EvidenceCustodian`
  itself, gated the same way, audited via a purpose-built port, "storage
  never gates; gating happens one level up."
- **`SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` §3** (adopted, part of Unit 1)
  already names "source hash/length/media type" as facts a "Parker-owned
  ingestion-provenance subsystem" tracks as part of Document Ingestion's
  own **per-attempt Source Manifest**. This document explicitly
  distinguishes that concept from the one it defines here (Section 4)
  to avoid a genuine, otherwise-real vocabulary collision.
- **`DOCUMENT_INGESTION_MEMORY_CORE_CROSS_REFERENCE_SCOPE_LOCK.md`**
  (adopted, Unit 6) fixes: no direct Document Ingestion write authority
  into Memory Core; a reference transfers no authority in either
  direction; Memory Core registration is optional, not mandatory, for
  any ingested derivative.
- **`DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md`**
  (Unit 2), **`DOCUMENT_INGESTION_AUDIT_AUTHORITY_SCOPE_LOCK.md`** (Unit
  5), **`DOCUMENT_INGESTION_ADMISSION_AUDIT_ATOMIC_VISIBILITY_CLARIFICATION.md`**,
  **`DOCUMENT_INGESTION_TIER_A_IMPLEMENTATION_PLAN.md`**, **`DOCUMENT_INGESTION_TIER_A_IMPLEMENTATION_CLOSURE.md`**,
  and **`DOCUMENT_INGESTION_PROGRAMME_GOVERNANCE_CLOSURE.md`** (all
  adopted) establish the now-implemented Tier A capability this document
  sits immediately upstream of; none is reopened.
- **Permission vocabulary** (`src/contracts/Permission.kt`,
  `src/contracts/Resource.kt`, fresh-read): `PermissionAction` has no
  action narrower than `READ`/`WRITE`/`DELETE`/etc.; `ResourceType` has
  `DOCUMENT` but no dedicated `EVIDENCE` type. `evidence.accept` →
  `(WRITE, DOCUMENT)`, `evidence.retrieve` → `(READ, DOCUMENT)`,
  `evidence.delete` → `(DELETE, DOCUMENT)` — all three already registered
  in `ParkerRuntime.kt` (fresh-read, lines ~505–585).
- **Owner-only enforcement precedent** (`ParkerRuntime.kt`, fresh-read):
  explicitly documented in-repo as *not* a Permission Engine matching
  capability — "DELETE/DOCUMENT's own owner-only guarantee is
  deliberately NOT enforced here... Owner-scoping is enforced instead by
  call-site structure... the only production caller that can ever
  construct a delete-shaped `ExecutionRequest` at all... and it always
  supplies `PrincipalId(config.ownerPrincipalId)` itself — never a
  caller-supplied principal." This is the exact, reusable pattern for
  owner-only Tier A invocation (Section 17).

No later document narrows or contradicts any of the above; precedence is
straightforward and does not require resolving a conflict.

## 4. Authoritative manifest owner

Evaluated against six required properties: preserving Evidence Custodian
authority, avoiding duplicate canonical truth, avoiding Document
Ingestion source authority, avoiding a second Memory Core evidence
manifest, supporting independent integrity verification, and supporting
the routing facts Tier A requires.

| Option | Assessment |
|---|---|
| A. Evidence Custodian owns it as part of custody | Preserves Evidence Custodian authority directly; the only option under which the manifest's founding fact (digest/length) is established at the one moment — `accept` — truthfulness about "these are the original bytes" is actually available |
| B. `EvidenceArtifactStorage` owns/persists it beneath Evidence Custodian authority | Structurally equivalent to A one layer down — the storage primitive computes/persists the manifest at `write` time, Evidence Custodian's governed `accept` boundary remains the only caller that can produce one |
| C. Document Ingestion owns a separate read-only source-manifest record | **Rejected for this purpose.** Would make Document Ingestion authoritative over a custody-integrity fact it does not control — it can only ever re-derive a digest from bytes it retrieves *after* the fact, reproducing exactly the Section 2, Finding 8 tautology one layer higher. Confusable with, but is not, the already-adopted, different, per-attempt "Source Manifest" `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` §3 already assigns to Document Ingestion — see disambiguation below |
| D. Memory Core `Document` becomes mandatory source-manifest authority | **Rejected.** Contradicts Section 3's Memory Core baseline directly: not every `EvidenceArtifact` has a `Document` (Section 2, Finding 6); making one mandatory to obtain integrity/media-type facts would newly require Document Ingestion (or any invocation boundary) to depend on Memory Core registration as a prerequisite for an Evidence Custodian operation — inverting CDR-006's own independence and creating exactly the "Memory Core becomes a second evidence manifest" risk this section's own required properties (above) exist to prevent |

**Decision: A/B combined** — the authoritative manifest is owned within
Evidence Custodian's own domain, established at (or immediately beneath)
the governed `accept` boundary, exposed only through a narrow, read-only
retrieval surface subordinate to Evidence Custodian (Section 14). This is
not a new authority; it completes what `EVIDENCE_CUSTODIAN_SCOPE_LOCK.md`
§9 already flagged as future implementation-level work.

**Disambiguation, load-bearing.** This document's manifest —
**"Authoritative Evidence Source Manifest"** — is a *different* concept
from `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` §3's Document-Ingestion-owned
**"Source Manifest,"** despite overlapping field names. The Authoritative
Evidence Source Manifest is established once, by Evidence Custodian, at
custody admission, and is the single source of truth an invocation
boundary verifies retrieved bytes against. Document Ingestion's own
per-attempt Source Manifest (parser/adapter identity, configuration
digest, ordered transformation disclosures — Unit 1's own vocabulary) is
produced *later*, once per ingestion attempt, by the ingestion
coordinator, and should *reference* (read, never re-derive or duplicate)
this document's manifest for the source-identity facts it needs, exactly
mirroring how `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` §3 already
describes OCR's own recognition-identity records as "referenced... never
duplicated." Section 23 (amendment map) proposes a cross-reference note
in `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` to state this explicitly.

## 5. Manifest semantics

The Authoritative Evidence Source Manifest is a **custody-integrity
record**, not a derivative, not a Memory record, not a routing decision,
and not itself evidence. It states only what can be truthfully asserted
about the original submitted bytes at, or before, the moment they became
custodied. It never changes after creation (Section 6 classifies each
field's immutability individually). Retrieving it grants no write,
mutation, deletion, or promotion authority of any kind (Section 14).

## 6. Minimum fields

| Field | Classification | Reasoning |
|---|---|---|
| `EvidenceArtifactId` | **REQUIRED** | The manifest's own key; without it there is nothing to retrieve by |
| SHA-256 | **REQUIRED** | The specific fact `TierADocumentSourceContext.expectedSha256` needs and that Section 2 Finding 8 proves cannot be safely supplied any other way |
| Byte length | **REQUIRED** | Cheap, deterministic, strengthens integrity verification (Section 9) at negligible cost; already effectively known the moment content is accepted |
| Received media type | **OPTIONAL** | Not deterministically derivable from bytes (Section 9) — may be legitimately absent; *functionally* necessary for CSV Tier A routing specifically (Section 2, Finding 7), but that necessity does not make the field mandatory at the manifest-schema level, only mandatory as a precondition for *that one format's* successful routing |
| Original filename | **OPTIONAL** | Metadata only (Section 11); many legitimate custody paths never have one |
| Custody/admission time | **NOT A NEW FIELD** | Already exists as `AcceptedEvidenceArtifact.acceptedAt` — the manifest references or is co-located with this value, never duplicates or re-derives a second timestamp for the same fact |
| Storage reference | **NOT PART OF THE AUTHORITATIVE MANIFEST** | An internal implementation detail of whichever component persists the manifest; exposing it in the manifest's own semantic contract would leak storage-layer structure into a governance-level shape and risks being mistaken for, or misused as, a direct storage bypass — exactly the "arbitrary filesystem path" exposure Section 21 (Owner Invocation Consequence) must foreclose |

No field is added merely because it might someday be useful — every
`REQUIRED`/`OPTIONAL` field maps directly to a concrete, already-identified
consumer (`TierADocumentSourceContext`'s own existing shape, or CSV
routing's own already-confirmed dependency).

## 7. Digest authority

- **Component responsible:** whichever component implements the governed
  `accept` boundary (today, `DefaultEvidenceCustodian`, or an immediate,
  narrow extension of it/its own storage dependency) — never a later
  caller, never Document Ingestion, never the invocation boundary.
- **Moment of calculation:** once, from the exact candidate bytes being
  accepted, at or immediately after the same moment `acceptedAt` is
  minted — before any later retrieval cycle exists to create a tautology.
- **Persisted representation:** durably, independently of the raw bytes
  themselves (not merely re-derivable on demand, though it happens to be
  re-derivable — Section 15) — as part of the Authoritative Evidence
  Source Manifest (Section 4).
- **Immutability rule:** fixed forever once established, mirroring every
  other Evidence-Custodian-owned fact's immutability (`EvidenceArtifactId`,
  `acceptedAt`) — never recomputed-and-overwritten by a later retrieval,
  only ever *compared against*.
- **Verification on later retrieval:** any component retrieving bytes for
  a governed purpose (including the invocation boundary) computes its own
  digest from the freshly retrieved bytes and compares it against the
  persisted manifest value — never the reverse.
- **Behaviour on mismatch:** fail closed — mirrors `EvidenceExtractionCoordinator`'s
  own already-adopted `IntegrityVerificationOutcome.Mismatch`/`Unverifiable`
  handling exactly (Section 16). No candidate of any kind is constructed
  from mismatched bytes; the attempt fails, is auditable, and is never
  silently downgraded to a warning.

The invocation boundary is explicitly forbidden from computing its own
"expected" digest from the bytes it is about to verify (Section 2,
Finding 8) — it may only *compare against* the already-persisted,
independent manifest value.

## 8. Byte-length authority

**Yes**, persisted alongside the digest, for the same reasoning and by
the same component, at the same moment (Section 7). Byte length is
calculated once, at admission; a later retrieval verifies the retrieved
content's own length against it; a mismatch is treated identically to a
digest mismatch — fail closed, no partial or "close enough" outcome.
This is not redundant authority: a length check is a cheap, independent,
early-exit signal (catching gross truncation before a full digest
computation is even needed) that materially strengthens defense-in-depth
at negligible cost — a redundant check is avoided only where it would add
no genuine defense-in-depth beyond what an existing check already gives;
here, the check is retained precisely because it strengthens, not merely
duplicates, the digest check (Section 16).

## 9. Received media type

**Canonical meaning:** the media type *declared* for the content at (or
as close as possible to) the moment it was originally submitted for
custody — an externally asserted fact Parker records, never a fact Parker
computes from the bytes themselves.

- **Origin:** the original ingress channel's own declaration (for
  example, an email attachment's declared `Content-Type`, or an explicit
  caller-supplied value at `accept` time) — never Parker's own later
  inspection of the bytes.
- **May it be absent:** yes — legitimately, for any artifact whose
  original ingress channel supplied none. Absence is truthful, not an
  error, and not something to be silently filled in later (Section 13).
- **Immutability:** once recorded, fixed forever — the same immutability
  discipline as digest/length (Section 7).
- **Syntactic normalisation:** trimmed and compared case-insensitively
  for *routing* purposes only (mirroring `GovernedTierADocumentIngestionRouter`'s
  own already-implemented `.substringBefore(';').trim().lowercase()`
  behaviour) — but the manifest's own *persisted* value is stored exactly
  as declared, unnormalised, so that what Parker recorded and what was
  actually declared always remain traceable to each other.
- **Parameters:** retained in the persisted value (for example,
  `text/csv; charset=utf-8` is stored whole) — routing-time comparisons
  strip parameters (as above), but the manifest does not silently discard
  them.
- **Detected media type:** an entirely separate, independently computed
  fact — `GovernedTierADocumentIngestionRouter.detect()` already performs
  this, mechanically, from the bytes themselves, and already surfaces both
  values plus their disagreement via `TierAMediaFacts.disagreement`. The
  manifest supplies only the *received* half of that pair; the *detected*
  half remains the router's own, already-correct, unchanged
  responsibility.
- **Filename extension:** never establishes received media type, under
  any circumstance. `TierAMediaFacts.originalFileName` already exists
  purely for disclosure; `route()` never reads it.

**Do not equate the three.** This document does not, and must not,
collapse received, detected, and filename-derived media type into one
value at any point.

## 10. Canonical media-type vocabulary

**A typed manifest field, not a `Document.metadata` string key.**
Governance supports this directly: nothing in the Memory Core Contract
Design or Scope Lock requires ingestion-specific facts to live in
`Document.metadata`'s open map (`SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`
§3 already assigns "source hash/length/media type" to the
ingestion-provenance subsystem's *own* structured facts, explicitly not
Memory Core's schema). A typed field (`receivedMediaType: String?`) on
the Authoritative Evidence Source Manifest is the minimum, single,
canonical location — no synonymous key (`"content-type"`,
`"declared-media-type"`, `"mime-type"`, etc.) is introduced anywhere, and
none should ever independently exist in `Document.metadata` for the same
fact.

## 11. Original filename

**Belongs in the manifest, optional, as disclosure only:**

- classified as metadata, never routing authority — `route()` already,
  correctly, never reads it;
- preserved literally, unmodified, wherever available;
- never used as, or converted into, a filesystem path (mirrors
  `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`'s and `EvidenceArtifactIdentifierSafety`'s
  own existing "filename-as-metadata only, never a path" discipline,
  already established for embedded-resource observations);
- never overrides received or detected media-type facts under any
  circumstance.

## 12. Memory Core relationship

**NO** — an invocable `EvidenceArtifact` does not require a Memory Core
`Document`. Derived directly from Section 3's baseline (Unit 6's
reference-only boundary) and Section 2, Finding 6 (not every artifact has
one). Making Memory Core mandatory merely to obtain `integrityHash` or a
media-type fact would violate CDR-006's own custody independence — it
would make an Evidence Custodian operation depend on a Memory Core write
having already occurred, inverting the existing, frozen direction of
dependency (Memory Core references evidence identities; evidence
authority never depends on Memory Core).

`Document.integrityHash`, where present, is **optional corroborating
information** — never canonical source authority, and never a substitute
for the Authoritative Evidence Source Manifest's own digest. It is set
today only by callers that happen to compute and supply it (for example,
`EvidenceRegistrationCoordinator`'s own callers passing
`documentIntegrityHash`) and is not itself guaranteed to originate from
an independent, pre-retrieval source the way this document's manifest
digest is required to (Section 7) — it must never be substituted for the
manifest's own digest by an invocation boundary.

## 13. Legacy-artifact handling

For an already-custodied `EvidenceArtifact` lacking one or more manifest
facts:

- **Digest and byte length — governed, deterministic backfill is
  authorized.** Both are pure, deterministic functions of the already-immutable
  custodied bytes (`SHA-256(bytes)`, `bytes.size`). A separately
  authorized migration/backfill operation may compute and durably persist
  them for a legacy artifact without fabricating anything — this is
  recomputation of a true fact from authoritative bytes, not invention.
  This operation is itself a governed act (it writes a new, immutable
  manifest fact) and should be audited the same way any other
  Evidence-Custodian-adjacent write is, but it introduces no new kind of
  authority beyond what Section 7 already establishes.
- **Received media type — cannot be truthfully backfilled by
  recomputation, because it is not a function of the bytes.** A legacy
  artifact missing this fact must remain, honestly, `receivedMediaType =
  null` unless a separately, explicitly governed **media-type enrichment
  under controlled review** (a human- or owner-authorized, disclosed,
  audited act of *assertion*, not inference) supplies one. **This
  document does not authorize automatic CSV inference from content or
  extension during migration, or at any other time** — consistent with
  Section 2, Finding 7's own routing-fidelity concern: silently
  asserting `text/csv` for a legacy
  artifact based on its bytes "looking like" CSV would recreate exactly
  the unauthoritative-inference risk this whole document exists to close.
- **Original filename — the same "cannot be reconstructed truthfully"
  category as received media type** — if never recorded, it remains
  permanently absent; no later component may invent one.
- **Consequence for invocation:** an artifact missing `receivedMediaType`
  simply cannot be routed to CSV by the existing router (Section 2,
  Finding 7) until enrichment supplies one — this is the correct,
  fail-closed behaviour, not a defect this document must additionally
  solve.
- **Reject-as-not-presently-invocable** remains the correct default for
  any artifact whose digest/length cannot yet be established (for
  example, if the underlying bytes are themselves found missing or
  corrupted) — this document does not authorize treating an
  unverifiable artifact as invocable under any relaxed standard.

## 14. Retrieval authority

**Narrowest authorized model: B — a narrow Evidence Custodian manifest
retrieval operation**, evaluated against four candidate models:

- (A) Extending `EvidenceCustodian.retrieve` itself was considered and
  rejected: `retrieve`'s own governed contract already returns exactly
  bytes-or-not-authorized-or-not-found (`EvidenceRetrievalResult`); folding
  manifest facts into the same call would change an already-adopted,
  tested, documented return shape for every existing caller, for no
  necessary reason.
- **(B) Adopted.** A second, narrow, read-only operation within Evidence
  Custodian's own domain — either a new method on the existing
  `EvidenceCustodian` interface (mirroring how `accept`/`retrieve`
  themselves were incrementally added to the same interface across prior
  Units) or, if a future implementation unit finds a structural
  dependency reason to separate it (mirroring exactly why
  `OwnerEvidenceDeletionAuthority` was split out — Section 3), a
  sibling, narrow interface subordinate to Evidence Custodian. Either
  shape satisfies this document's semantics; the choice between them is
  implementation-plan work (Section 25), not fixed here.
- (C) A source-manifest port *subordinate to* Evidence Custodian is
  effectively the same authority shape as (B) with a different name —
  acceptable only if it remains structurally subordinate (Section 3's
  narrow-port precedent), never independently authoritative the way
  option C in Section 4 (Document Ingestion source authority) was
  rejected.
- (D) No other existing governed interface fits — `MemoryRetrieval`
  belongs to Memory Core (rejected as owner, Section 4); `DerivativeReviewRegistry`
  and the ingestion audit port are unrelated domains.

**The chosen model grants no admission, mutation, deletion, or manifest-rewrite
authority of any kind** — read-only, exactly mirroring `retrieve`'s own
"authorised, observational read access... never writes, replaces,
deletes, promotes, classifies, hashes, or annotates anything" discipline
(`EvidenceCustodian.kt`'s own Unit 3 KDoc, restated here for the
manifest).

## 15. Bytes/manifest admission invariant

**Load-bearing — assessed honestly, not pretended.**

Current architecture, as it stands, cannot literally guarantee a single
atomic write spanning both raw-byte storage (`EvidenceArtifactStorage.write`)
and a separately persisted manifest — two independent writes are not
atomic merely by being sequenced, exactly as the prior Admission/Audit
Atomic Visibility Clarification already established for a structurally
identical problem one layer up (Derivative Generation admission vs. its
audit). This document does not pretend otherwise.

The required admission invariant, and the smallest additional governance
requirement that closes the gap without pretending atomicity that does
not exist:

1. **Bytes must be durably written and immutable before the manifest may
   ever be populated or referenced for that identity** — never the
   reverse; no manifest may exist, even transiently, for bytes not yet
   durably custodied (mirrors Unit 2 §19's own admission-order
   precedent).
2. **Digest and byte length are safe under a bytes-first, manifest-second
   sequence, even across a crash, because they are deterministically
   recoverable from the immutable bytes at any later time.** A crash
   between "bytes written" and "digest/length manifest facts persisted"
   is not a permanent integrity gap — it is exactly the legacy-artifact
   backfill case (Section 13), authorized as an ordinary, deterministic
   recomputation, never a fabrication. This is the smallest additional
   governance requirement this document identifies: **a manifest
   reconciliation/backfill operation for digest/length must be treated as
   authorized, ordinary, non-widening custody maintenance**, not a new or
   exceptional authority.
3. **Received media type carries no equivalent recoverability guarantee**
   (Section 9) — its absence following any crash, partial write, or
   legacy gap is simply, permanently, truthfully `null` unless a separate,
   explicitly governed enrichment act (Section 13) supplies it. No
   reconciliation process may ever *infer* it from bytes or invent one to
   "complete" the record.

This is not two independent writes pretended atomic — it is an explicit
acknowledgment that only one of the manifest's two write-time facts needs
a genuine atomicity guarantee it does not structurally get, and that gap
is closed by deterministic recoverability rather than by inventing a
transaction mechanism this repository does not have.

## 16. Retrieval integrity invariant

A later manifest+bytes retrieval, at minimum, must:

- obtain the manifest and the bytes for the *same* `EvidenceArtifactId`
  — a single logical read of two facts about one identity, never mixed
  across identities;
- verify byte length (cheap, early-exit signal, Section 8);
- verify SHA-256 (the load-bearing check, Section 7);
- **fail closed on any mismatch** — no `TierADocumentSourceContext` is
  ever constructed from bytes that fail either check; this exactly
  mirrors `EvidenceExtractionCoordinator`'s own already-adopted
  `IntegrityVerificationOutcome.Mismatch`/`Unverifiable` → "extraction
  never runs; no candidate of any kind is constructed" discipline, reused
  here rather than reinvented.

**Verification layer: the runtime invocation boundary itself** (not
Evidence Custodian, not the source-manifest retrieval port, not the Tier
A coordinator) — because it is the one component that actually holds
both freshly retrieved bytes and the persisted manifest simultaneously,
immediately before constructing `TierADocumentSourceContext`. This is not
redundant with `DerivativeGenerationCoordinator`'s own existing
`sha256(source.content) != source.expectedSha256` check (Section 2,
Finding 8) — that check remains, unchanged, as **defense in depth**: it
protects against a corrupted or tampered `TierADocumentSourceContext`
constructed anywhere else, including a future, different caller of the
Tier A router that does not go through this invocation boundary at all.
Removing either check would weaken defense in depth this document's own
adversarial review (Section 26) relies on.

## 17. Permission/resource mapping

No new `PermissionAction` and no new `ResourceType` are required.

- **Manifest retrieval:** reuses `(PermissionAction.READ, ResourceType.DOCUMENT)`
  — the same pair `evidence.retrieve` already uses (fresh-confirmed,
  `ParkerRuntime.kt`) — via a new, distinct action-vocabulary verb phrase
  (for example, mirroring `evidence.retrieve`'s own naming convention),
  mapped to the identical pair. A distinct verb phrase, not a shared one,
  keeps the manifest read auditable and permission-decision-traceable as
  its own act, exactly as `evidence.accept`/`evidence.retrieve` already
  are two distinct verb phrases sharing patterns but not identity.
- **Source-bytes retrieval:** unchanged — the existing `evidence.retrieve`
  → `(READ, DOCUMENT)` mapping, exactly as today.
- **Tier A invocation itself:** requires no new permission gate distinct
  from the two reads above — Unit 2 §18 already establishes that
  admission is "gated by whatever authorization/permission check governs
  ingestion generally," and this document identifies no ingestion-specific
  act here that is not already either a governed evidence read (gated as
  above) or a governed Derivative Generation admission (already gated per
  the Admission/Audit Atomic Visibility Clarification, unchanged).

**Owner-only enforcement** requires the same combination the deletion
precedent already establishes (Section 3): (a) the existing
`PermissionEngine`/`ActionVocabulary` gate above, plus (b) **structural
call-site restriction** — only a narrow, purpose-built invocation-boundary
component may ever construct the manifest-retrieval and Tier A
invocation calls, and that component always supplies
`PrincipalId(config.ownerPrincipalId)` itself, never a caller-supplied
principal, mirroring `deleteEvidenceAsOwner`'s own exact precedent.
**No hard-coded owner bypass** — owner-ness is enforced by which
component is ever given a reference to the invocation boundary and by
what principal that component always supplies, never by a special-cased
permission check.

## 18. Production storage locations

No new literal host path is proposed or required by this document. The
Authoritative Evidence Source Manifest, wherever it is ultimately
persisted, is governed by the same configuration-responsibility pattern
already established for `FileSystemEvidenceArtifactStorage`,
`FileSystemDerivativeGenerationStorage`, and
`FileSystemDocumentIngestionAudit` — each receives an already-existing,
already-writable storage root supplied by runtime configuration, never
guessed or defaulted by the component itself. This document does not
invent a new configuration mechanism; it identifies that a manifest
persistence root (if implemented as a sibling to, rather than inside, the
existing evidence storage root) would need the same
`ParkerRuntimeConfig`-owned configuration responsibility the existing
three storage roots already have — a detail for the later implementation
unit (Section 25), not for this document to fix.

## 19. Deletion/retention boundary

When an `EvidenceArtifact` is deleted under governed evidence deletion
(`OwnerEvidenceDeletionAuthority.deleteAsOwner`), **the Authoritative
Evidence Source Manifest for that identity must not survive as a
phantom authority.** It is custody/source authority metadata associated
with, and inseparable from, the source bytes themselves (Section 20) —
its removal is part of the *same* deletion event, not a second,
independently-tracked artifact requiring its own tombstone or a new audit
concept. This does not require a new mechanism: `EvidenceDeletionAuditStage`'s
existing `AUTHORISED`/`COMPLETED` two-stage record already durably
documents *that* a deletion occurred for the identity; the manifest's own
disappearance alongside the bytes is exactly Determination 2's existing
"no tombstone... covers both never existed and already deleted
identically" rule, extended to one more artifact kind, not reopened.

Explicitly distinguished:

- **Active source manifest** — this document's subject; removed with the
  source it describes.
- **Deletion audit/history** (`EvidenceDeletionAuditRecord`) — already
  governed, unchanged, unaffected by this document.
- **Derivative-generation records** (`DerivativeGenerationRecord`) —
  already governed by Unit 2/5/the Admission/Audit Atomic Visibility
  Clarification; **this document does not redesign, and does not touch,**
  their own deferred retention policy (Unit 2 §17, restated at closure).
  A derivative's own immutability and independent retention are entirely
  unaffected by the source manifest's removal, mirroring Unit 2 §17's
  own existing "source deletion never authorizes silent derivative
  mutation" rule exactly.

## 20. Source/derivative classification

The Authoritative Evidence Source Manifest is confirmed **not**:

- a derivative — it describes the *original* bytes, never a transformed
  or extracted output;
- a Memory record — it is not a `Provenance`, `Document`, `Entity`, or
  any other Memory Core type, and creates no Memory Core write authority;
- a Knowledge Item — nowhere near Knowledge Memory's own promotion
  boundary;
- parser output — no parser, extractor, or adapter of any kind produces
  it;
- OCR output — no OCR or recognition mechanism is involved;
- Evidence Intelligence output — no analytical or interpretive act
  produces it; it states only observable, deterministic (digest/length)
  or externally-declared (received media type, filename) facts about
  bytes already in custody.

It is **custody/source authority metadata associated with authoritative
source bytes** — squarely within Evidence Custodian's own existing
domain (Section 4), governed by the same immutability discipline as
`EvidenceArtifactId`/`AcceptedEvidenceArtifact` themselves.

## 21. Runtime invocation consequence

Once this scope lock is adopted, a later, separately authorized
Owner-Facing Tier A Runtime Invocation Boundary unit may implement
exactly this chain, and no more:

```
owner authorization
  → authoritative manifest retrieval (Section 14, read-only)
  → authoritative bytes retrieval (existing EvidenceCustodian.retrieve, unchanged)
  → integrity verification (Section 16: length, then SHA-256, fail-closed)
  → TierADocumentSourceContext construction
      (evidenceArtifactId, content, expectedSha256 = manifest digest,
       receivedMediaType = manifest value, originalFileName = manifest value,
       requestingPrincipalId = owner principal, correlationValue = freshly minted)
  → Tier A router (GovernedTierADocumentIngestionRouter, unchanged)
```

It still must **not** authorize:

- arbitrary filesystem paths (Section 6's own "storage reference is not
  part of the manifest" exclusion directly forecloses this);
- raw owner-supplied bytes (the boundary only ever passes through
  Evidence-Custodian-retrieved, integrity-verified bytes — never bytes a
  caller hands it directly);
- uploads, URLs, or Gmail ingestion of any kind;
- automatic ingestion of any kind — every invocation remains an explicit,
  individually authorized owner act.

## 22. External-ingress exclusion

**Explicitly, deliberately not solved here.** How an external file
becomes an authoritative `EvidenceArtifact` in the first place — upload,
URL fetch, Gmail attachment intake, filesystem watcher, or any other
ingress mechanism — remains a wholly separate, future problem this
document does not begin to authorize. No existing adopted governance
already solves it either (confirmed by this unit's own fresh governance
inspection — no ingress-authorization document exists in the reviewed
set). This scope lock concerns only the retrieval-side gap for content
*already* custodied; it adds no upload, fetch, or filesystem-ingress
authority of any kind.

## 23. Amendment map

| Document | Change |
|---|---|
| `EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md` | **No change.** Section 9-equivalent exclusions already anticipate this |
| `EVIDENCE_CUSTODIAN_SCOPE_LOCK.md` | **No change.** §9/§10 already, explicitly, pre-authorize exactly this class of implementation-level decision (Section 3) |
| `EVIDENCE_CUSTODIAN_PHASE_7_BOUNDARY_CLARIFICATION.md` | **Cross-reference only.** This document's retrieval-port precedent (Section 14) is reused, not altered |
| `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` | **Cross-reference/clarification recommended, not required for adoption.** §3's own "Source Manifest" vocabulary should gain a forward pointer distinguishing it from this document's Authoritative Evidence Source Manifest (Section 4's disambiguation) — a labelling-clarity matter, not a substantive one, mirroring exactly how Unit 6's own closure recorded a similar non-substantive dual-numbering caution rather than editing an adopted document |
| `DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md` | **No change.** Its own routing/media-type framing (received vs. detected) is already fully consistent with Section 9 |
| `DOCUMENT_INGESTION_TIER_A_IMPLEMENTATION_PLAN.md` / `..._CLOSURE.md` | **No change.** Neither claims or requires manifest retrieval; both explicitly defer the runtime invocation boundary to a separately authorized unit, exactly as this document remains |
| `DOCUMENT_INGESTION_MEMORY_CORE_CROSS_REFERENCE_SCOPE_LOCK.md` | **No change.** Section 12's determination is fully consistent with, not in tension with, its existing reference-only boundary |
| Memory Core Contract Design / Scope Lock | **No change.** `Document.integrityHash`/`metadata` remain exactly as frozen; nothing here relies on or alters either |

## 24. Governance-change classification

**A/B — clarification and narrow extension consistent with existing
authority. Not C.**

Not a substantive authority change because: (1) `EVIDENCE_CUSTODIAN_SCOPE_LOCK.md`
§9 explicitly, already anticipated exactly this class of decision as
future implementation-level work, not a constitutional matter; (2) no
new `PermissionAction` or `ResourceType` is introduced (Section 17); (3)
no authority is transferred to Document Ingestion or Memory Core — both
were evaluated and explicitly rejected as manifest owner (Section 4); (4)
the retrieval model is read-only and mirrors an already-adopted precedent
structurally (Section 14) rather than inventing a new authority shape;
(5) the deletion/retention consequence (Section 19) extends an
already-adopted rule (Determination 2's no-tombstone principle) to one
more artifact kind rather than creating a new one.

## 25. Implementation impact (not performed here)

| Surface | Classification |
|---|---|
| `AcceptedEvidenceArtifact` / a new manifest-establishing type | **Possibly required** — either a new optional field on `CandidateEvidenceArtifact` (to carry a caller-declared received media type into `accept`) plus a new manifest type returned alongside/derived from `AcceptedEvidenceArtifact`, or a distinct, parallel manifest-establishing operation — the exact shape is coding-unit work |
| `EvidenceCustodian` interface | **Required** — a new, narrow, read-only manifest-retrieval operation (Section 14) |
| `EvidenceArtifactStorage` / filesystem storage | **Possibly required** — depends on whether digest/length are computed inside `DefaultEvidenceCustodian.accept` from bytes already in hand (no storage-layer change needed) or delegated to the storage layer itself |
| New manifest persistence (interface + filesystem implementation) | **Required** — mirroring `FileSystemDerivativeGenerationStorage`'s/`FileSystemDocumentIngestionAudit`'s own established write-once/append-only patterns, per whichever is more apt to the manifest's own immutable, single-record-per-identity shape |
| Permission/resource registration | **Required** — one new action-vocabulary verb phrase mapped to the existing `(READ, DOCUMENT)` pair (Section 17); no new `Resource`/`ResourceType`/`PermissionAction` |
| Runtime composition (`ParkerRuntime.kt`) | **Required**, later — registering the new verb phrase, wiring the manifest port; **not required by this document itself** |
| Tier A invocation boundary (the separately authorized next unit) | **Required**, later — consumes this document's retrieval port; **not designed or begun here** |
| Legacy-artifact backfill/enrichment mechanism | **Possibly required** — only if and when a legacy artifact actually needs to become invocable; this document authorizes the operation's *shape* (Section 13) without requiring it be built now |
| Tests | **Required**, later, alongside whichever of the above is actually implemented |
| Not required by any reading of this document | Any change to `EvidenceCustodian.accept`'s existing signature beyond what a manifest-establishing addition needs; any change to `MemoryCore`, `Document`, or `Provenance`; any Tier B/OCR change; any CSV/DOCX/EML/PDF extractor change |

## 26. Adversarial review

| Risk | Resolution |
|---|---|
| Digest tautology | Closed by Section 7's "established once, at admission, from candidate bytes, never re-derived from a later retrieval" rule, directly answering Section 2 Finding 8 |
| Stale manifest | Cannot occur under the immutability rule (Section 7/9) — the manifest never changes after creation except through the explicitly governed backfill/enrichment acts of Section 13, which do not overwrite a value that already, truthfully exists |
| Bytes/manifest split-brain | Addressed honestly, not pretended away, in Section 15 — bytes-first sequencing plus deterministic digest/length recoverability closes the only genuinely dangerous gap; received-media-type absence is truthfully tolerated, never fabricated to appear consistent |
| Media-type fabrication | Foreclosed explicitly at every layer: Section 9 (declared only, never computed), Section 13 (no automatic inference during migration), Section 21 (the invocation boundary passes through only the manifest's own recorded value) |
| CSV misrouting | Addressed directly — Section 2 Finding 7 is the concrete evidence motivating this whole document; Section 13's fail-closed "cannot be routed to CSV without an authoritative declared type" outcome is the correct behaviour, not a residual risk |
| Filename authority leakage | Foreclosed by Section 11's explicit "never routing authority, never a path, never overrides media-type facts" rule, matching the router's own already-correct behaviour |
| Memory Core duplicate authority | Foreclosed by Section 4's rejection of Option D and Section 12's explicit "optional corroborating information only" determination |
| Document Ingestion authority creep | Foreclosed by Section 4's rejection of Option C and the explicit disambiguation from `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` §3's own, different, Document-Ingestion-owned concept |
| Legacy-artifact ambiguity | Addressed in Section 13 with an explicit, three-way split (recomputable / assertable-only-under-governed-enrichment / permanently-fail-closed) rather than one blanket rule |
| Unauthorized manifest reads | Foreclosed by Section 14 (read-only, gated) and Section 17 (existing `READ`/`DOCUMENT` pair, no new bypass) |
| Deletion leaving phantom manifests | Foreclosed by Section 19's explicit "removed as part of the same deletion event, no new tombstone" rule |
| Partial writes | Addressed by Section 15's bytes-first invariant and Section 8's independent length check as an early-exit defense |
| Corrupt manifest records | Not separately re-solved — mirrors the already-adopted `DerivativeGenerationStorageException.CorruptRecord` precedent's own shape; a future implementation unit's manifest storage should apply the identical discipline, a coding-unit detail (Section 25), not a governance gap |

No item resolves to a blocker. All fourteen challenges resolve within
this document's own existing sections.

## 27. Unresolved blockers

**None** found that prevent adoption of this scope lock itself. The one
genuinely open design choice — whether the manifest-retrieval operation
becomes a new method on `EvidenceCustodian` or a sibling, narrow,
subordinate interface (Section 14) — is explicitly left to
implementation-plan judgment because either shape satisfies every
semantic requirement this document fixes; it is not a governance question.

## 28. Acceptance criteria

This scope lock is ready for owner acceptance if and only if the owner
confirms:

1. Evidence Custodian remains the authoritative manifest owner (Section
   4), with no authority granted to Document Ingestion or Memory Core.
2. The manifest's minimum fields (Section 6) and their
   required/optional/excluded classification are correct and complete.
3. Digest/length authority (Sections 7–8) and the fail-closed
   verification rule (Section 16) are acceptable as stated.
4. Received-media-type semantics (Section 9) — declared, optional,
   immutable, never inferred — are acceptable, including that CSV Tier A
   routing remains blocked for any artifact lacking one.
5. The legacy-artifact policy (Section 13) — deterministic backfill for
   digest/length, governed enrichment only for received media type, no
   automatic inference — is acceptable.
6. The bytes/manifest admission invariant (Section 15) — bytes-first,
   deterministic-recoverability-for-digest/length, honest-absence-for-media-type
   — is accepted as the correct, smallest closing requirement, not a
   pretended atomicity guarantee.
7. No new `PermissionAction`/`ResourceType` and no hard-coded owner bypass
   (Section 17) are introduced.
8. The runtime invocation consequence (Section 21) and external-ingress
   exclusion (Section 22) are both explicitly acknowledged as the correct
   scope boundary for this document.
9. The governance-change classification (Section 24) — clarification/narrow
   extension, not substantive amendment — is accepted.

If all nine are confirmed, this scope lock authorizes a later,
separately governed implementation unit to build the Authoritative
Evidence Source Manifest and its retrieval port (Section 25), followed
by a separately authorized Owner-Facing Tier A Runtime Invocation
Boundary unit. Neither is begun or authorized by this document itself.
