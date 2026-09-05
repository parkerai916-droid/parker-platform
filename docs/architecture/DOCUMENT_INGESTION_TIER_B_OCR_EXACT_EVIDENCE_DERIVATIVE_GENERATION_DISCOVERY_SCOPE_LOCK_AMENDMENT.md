**Status: Adopted.** The owner accepted the independent architecture review's
VERDICT A — EXACT-EVIDENCE DERIVATIVE DISCOVERY SCOPE READY FOR OWNER
ACCEPTANCE (UI-INGESTION-8A) and explicitly accepted this document (verbatim,
unmodified) as the governing scope for the UI-INGESTION-8B implementation. No
substantive scope was altered between proposal and acceptance. This is an
adopted amendment to
`docs/architecture/DOCUMENT_INGESTION_TIER_B_DURABLE_OCR_DERIVATIVE_CONTENT_SCOPE_LOCK.md`
("the Tier B Scope Lock"). The Tier B Scope Lock is not edited by this
document — its §24 general enumeration/browsing/search prohibition remains
frozen, unchanged, and in full force exactly as written today; this document
is now the governing instrument for the one narrow, bounded exception §13
(below) states. This document supersedes and rejects the UI-INGESTION-8
candidate's `ExternalTranscriptionOwnerAuthorization.derivativeGenerationId`/
`recordGeneration()` mechanism (see §4 and the UI-INGESTION-8 architecture
review, VERDICT C) as the means of solving the same problem; UI-INGESTION-8B
removed that mechanism from the repository and implemented the capability
this document authorises instead, per §11(b) below (a new query directly on
`FileSystemDerivativeGenerationStorage`, encapsulating its own filesystem scan
rather than reusing the audit log).

# Document Ingestion — Tier B OCR — Exact-Evidence Derivative Generation Discovery Scope Lock Amendment

Programme: **Tier B Durable OCR Derivative Content Scope Lock, Exact-Evidence Discovery Amendment (proposed).**

## 1. Capability Authorised

Exactly one capability: given one already-known `EvidenceArtifactId`, return
the identities and minimum governed metadata of every admitted derivative
generation whose `DerivativeGenerationRecord.rootSourceEvidenceArtifactId`
equals that exact artifact and whose `transformationHistory` contains
`DerivativeTransformation.OCR` — the same Tier-B-eligible generation family
`TierBOcrContentRetrievalCoordinator` already serves, never a broader "all
derivatives of any kind" surface. Nothing else. This authorises no
enumeration without an already-known evidence artifact, no cross-evidence
query, no filesystem browsing exposed as a capability, and no content return.

## 2. Problem Solved

The UI-INGESTION-8 unit needed a way for the Owner UI to discover, on a fresh
page load, which admitted derivative generation(s) exist for a given evidence
artifact, so it can offer "View enhanced transcription" without re-triggering
execution merely to rediscover an identity the owner's browser session had
already forgotten. No governed path for this exists today (confirmed by
direct trace in the UI-INGESTION-8 architecture review — `DerivativeGenerationStorage`,
`DerivativeGenerationCoordinator`, `TierBOcrContentRetrievalCoordinator`, and
`DocumentIngestionAudit` all lack any query keyed by evidence artifact). That
review's candidate implementation worked around the gap by storing
`derivativeGenerationId` on `ExternalTranscriptionOwnerAuthorization` — which
that same review found architecturally wrong (Q2/Q9) and operationally broken
under multiple admitted generations (Q6/Q7/Q8, restated in §4/§14 below). This
amendment authorises the correct capability instead.

## 3. Relationship to §24's Prohibition — Narrow Exception, Not a Repeal

Tier B Scope Lock §24 states: *"Prohibited, unchanged: enumeration, browsing,
search, or general evidence discovery of any kind; arbitrary generation-only
lookup without the paired source-identity check; any client-supplied
filesystem path (§18, above); retrieval ever triggering a fresh OCR
execution."* That prohibition was written to forbid **unbounded** discovery —
finding generations without already possessing the evidence identity they
belong to, or finding them by any fact other than that identity. This
amendment authorises no such thing. It authorises only a paired-identity-bounded,
deterministic, non-enumerable query rooted at a single already-known evidence
artifact the caller must already possess — structurally identical in shape to
the paired-identity check `TierBOcrContentRetrievalCoordinator.retrieve()`
already performs today, merely relaxing "exactly one already-known
generation ID required" to "zero or more generation IDs returned, for that
same already-known artifact." §24's text is not weakened by this document for
any case outside that one narrow shape.

## 4. Domain Ownership

The derivative-generation domain — never the External Transcription
Authorization domain. `ExternalTranscriptionOwnerAuthorization`'s own file
header (`src/runtime/ExternalTranscriptionOwnerAuthorization.kt`) scopes it to
*"durable, exact-target owner authorization... and nothing else"*; its
lifecycle (a one-time permission decision preceding execution) is unrelated
to, and must not gate, the separate, later, multiplicity-bearing fact of
which derivative generations have since been admitted. This is the accepted
finding of the UI-INGESTION-8 architecture review (Q2, Q9) and is restated,
not reopened, here.

## 5. Query Shape and Cardinality

**Input:** one `EvidenceArtifactId`, already known to the caller — never
inferred, searched, guessed, or supplied as a partial/fuzzy value.
**Output:** `0..N` tuples of `(DerivativeGenerationId, minimum metadata per §10)`.
There is no single authoritative slot and no "most recent" default baked into
the query itself — see §14 for why this matters against the real, current
evidence state.

## 6. Type Filtering — Determination

Traced the actual `DerivativeGenerationRecord` vocabulary at every admission
call site in `DerivativeGenerationCoordinator`:

| Path | `derivativeKind` (free string) | `transformationHistory` |
|---|---|---|
| `ingestCsv` | `"CSV structure"` | (CSV-specific transformations, no `OCR`) |
| `ingestEml` | `"EML MIME structure"` | (EML-specific transformations, no `OCR`) |
| `ingestDocx` | `"DOCX OOXML structure"` | (DOCX-specific transformations, no `OCR`) |
| `ingestPdf` | `"Searchable PDF literal text"` | (PDF-specific transformations, no `OCR`) |
| `ingestOcr` (local Docling) | `"OCR recognised text"` | `[OCR, MODEL_INFERENCE]` |
| `admit` (external transcription) | `"External transcription recognised text"` | `[OCR, MODEL_INFERENCE]` |

`derivativeKind` is free-form descriptive text — it does differ between local
and external OCR, but it is not a governed enum and is documented in-code as
descriptive, not load-bearing; too fragile to gate an authorization decision
on (a future rewording would silently change filtering behaviour with no
compile-time signal). `DerivativeTransformation.OCR` is the one governed,
structural, already-relied-upon signal: it is the exact fact
`TierBOcrContentRetrievalCoordinator.retrieve()` itself checks
(`DerivativeTransformation.OCR !in record.transformationHistory` →
`WrongDerivativeKind`) before ever decoding content. **Decision: discovery
filters to `DerivativeTransformation.OCR ∈ transformationHistory`** — i.e.
Option A restricted to the Tier-B-eligible family, not filtered further down
to "external only" at the discovery layer. The finer local-vs-external
distinction is a content-layer fact (§7) and is deliberately left to the
retrieval/projection layer that already computes it correctly, rather than
duplicated or raced against inside a lightweight discovery index.

## 7. External Transcription Identification (for the record; not exposed at discovery)

`OcrDerivativeExtractedResult.providerProvenance: OcrProviderProvenance?` is
the governed, existing signal: non-null if and only if the generation passed
through `DerivativeGenerationCoordinator.admit()`'s external-transcription
path, which requires it as **mandatory** provenance and fails closed with
`MandatoryProvenanceUnavailable` if absent; always `null` for `ingestOcr()`'s
local-Docling path, which never constructs it. This is exactly the signal
`OwnerUiEvidenceRuntimeAdapter.toOwnerOcrContent()` already uses
(`externalTranscription = extracted.providerProvenance != null`). Because
this field lives on the derivative **content** payload
(`OcrDerivativeExtractedResult`, reached only via `DerivativeContentStorage`),
not the lightweight `DerivativeGenerationRecord` index, discovery (§6) does
not attempt to expose it — it remains correctly derived, once, at the
existing retrieval step.

## 8. Ordering

Canonical, already-governed, non-invented field:
`DerivativeGenerationRecord.generatedAt: Instant` — mandatory, non-nullable on
every record; for `ingestOcr`/`admit` specifically it is set to
`result.recognisedAt`, a real recognition-time fact reported by the
mechanism/provider, never a synthetic "now" invented at discovery or
admission time. Discovery orders results by `generatedAt` descending (most
recent first), with `derivativeGenerationId` value as a deterministic
tiebreaker for the vanishingly unlikely case of an identical timestamp. No new
timestamp field is introduced by this amendment.

(`DocumentIngestionAuditRecord.recordedAt` is a second, real, already-governed
timestamp — audit-write time rather than recognition time — available if the
chosen implementation sources candidate IDs from the audit log; either is a
legitimate canonical fact, but `generatedAt` is preferred because it is
already returned by the same authoritative-record read discovery must
perform anyway for identity/kind re-validation, §9, at zero additional cost.)

## 9. Paired-Identity Protection

Mirrors, exactly, the two-part check `TierBOcrContentRetrievalCoordinator.retrieve()`
already performs: `record.rootSourceEvidenceArtifactId == requested EvidenceArtifactId`
**and** `DerivativeTransformation.OCR ∈ record.transformationHistory`.
Whatever mechanism supplies discovery's candidate `DerivativeGenerationId`
list (§11), **every candidate must be independently re-validated against its
own authoritative `DerivativeGenerationRecord`** before being included in a
discovery result — a candidate source is a hint, never a trust boundary. A
generation whose authoritative record's `rootSourceEvidenceArtifactId` does
not match the requested artifact must never appear in that artifact's
discovery result, regardless of what any index or candidate source claims.
This forecloses cross-evidence leakage at two independent layers: once at
discovery, and again, unconditionally and unchanged, at
`TierBOcrContentRetrievalCoordinator.retrieve()` itself.

## 10. Provenance / Minimum Metadata

Per discovered generation, all already-governed fields, none invented:
`derivativeGenerationId`, `rootSourceEvidenceArtifactId` (redundant
confirmation of the paired identity), confirmation that
`DerivativeTransformation.OCR` is present, `operationalOutcome`
(`USABLE`/`NOT_USABLE`), `completenessState`, `generatedAt`. Provider, model,
profile, qualifications, uncertainty, and recognised text are **not** exposed
at discovery — those remain exclusively reachable via the existing,
unmodified `TierBOcrContentRetrievalCoordinator.retrieve()` call the Owner UI
already makes once a specific `derivativeGenerationId` is chosen from a
discovery result. Discovery returns identities and thin index metadata only;
it never returns or duplicates content, satisfying the read-path
non-duplication requirement directly.

## 11. What This Amendment Does Not Decide (Implementation-Deferred)

The exact storage/query mechanism supplying discovery's candidate ID list is
an implementation decision for a separate, dedicated implementation unit —
not fixed here. Two candidates were identified, both compliant with this
document provided §9's re-validation discipline is honoured regardless of
choice:

- **(a)** a new read method on `DocumentIngestionAudit` (or a wrapping query
  type) filtering already-durably-written `DocumentIngestionAuditRecord` rows
  by `sourceEvidenceArtifactId` and `stage == ADMITTED` — reuses data already
  recorded on every admission (`DerivativeGenerationCoordinator.admit()`/
  `ingestOcr()` already write it), adds no new write, adds exactly one new
  read method to an interface that is today write-only.
- **(b)** a new, narrow, exact-evidence-keyed secondary index maintained by
  `DerivativeGenerationCoordinator` alongside `DerivativeGenerationStorage.publishPrepared`
  — adds a new write, avoids a linear audit-log scan at read time.

Either is authorised under this document; neither is mandated. What **is**
mandated regardless of choice: no filesystem path or directory listing is
ever exposed as, or driven directly by, owner input (Tier B Scope Lock §18,
unchanged); the candidate source is never trusted as authoritative (§9); and
no candidate-source read capability introduced for this purpose may be
repurposed to answer any query broader than "generations rooted at this one
already-known evidence artifact."

## 12. What This Does Not Authorise

- General enumeration, browsing, or search of derivative generations, of any
  kind, for any purpose — §24's prohibition remains fully in force for every
  case not identical in shape to §1's single capability.
- Cross-evidence discovery, lookup by unrelated metadata (filename, media
  type, sha256, principal, date range, free text), or any query not keyed by
  one already-known `EvidenceArtifactId`.
- Return or duplication of derivative content, recognised text,
  qualifications, uncertainty spans, or provider/model detail — these remain
  exclusively `TierBOcrContentRetrievalCoordinator.retrieve()`'s
  responsibility, unmodified.
- Mutation of any kind — of `DerivativeGenerationRecord`,
  `DerivativeContentEntry`, `EvidenceSourceManifest`, or
  `ExternalTranscriptionOwnerAuthorization`. Discovery is read-only, full
  stop.
- Any relationship to, or reuse of,
  `ExternalTranscriptionOwnerAuthorization`/`FileSystemExternalTranscriptionAuthorizationStore`
  for this purpose. The UI-INGESTION-8 candidate's `derivativeGenerationId`/
  `recordGeneration()` addition to that store is explicitly superseded and
  rejected (§4; UI-INGESTION-8 architecture review, VERDICT C).
- Unauthenticated access of any kind — this capability, once implemented, is
  reachable only inside the existing authenticated Owner UI boundary
  (`OwnerEvidenceHttpServer`'s existing `isAuthorised` gate), exactly like
  `TierBOcrContentRetrievalCoordinator`'s own existing HTTP route.
- A general derivative-generation repository/query abstraction, a persistent
  index visible outside this one bounded shape, or precedent for any other
  domain's discovery need (e.g. Tier A) — a future, unrelated discovery
  requirement requires its own, separate governance review; this document
  authorises nothing beyond §1.

## 13. Proposed Textual Qualification to §24

*(Base document not edited. Proposed text only, for reference; in force as
the governing instrument for the qualification below only upon this
amendment's own owner acceptance.)*

Existing text (Tier B Scope Lock §24, final paragraph):

> "Prohibited, unchanged: enumeration, browsing, search, or general evidence
> discovery of any kind; arbitrary generation-only lookup without the paired
> source-identity check; any client-supplied filesystem path (§18, above);
> retrieval ever triggering a fresh OCR execution."

Proposed qualification, in force only upon this amendment's own owner
acceptance:

> "Prohibited, unchanged: enumeration, browsing, search, or general evidence
> discovery of any kind; arbitrary generation-only lookup without the paired
> source-identity check; any client-supplied filesystem path (§18, above);
> retrieval ever triggering a fresh OCR execution — **except the single,
> narrowly bounded, paired-identity, already-known-evidence-artifact-only
> discovery capability the Exact-Evidence Derivative Generation Discovery
> Scope Lock Amendment authorises, which returns identities and minimum
> index metadata only, never content, and is itself subject to that
> document's own paired-identity re-validation discipline.**"

## 14. Real-Case Representability Check

`evidence-44d61bfe-e46f-4d39-85e7-9f68f122369d` has at least two admitted
external-transcription generations, `6d8d9307-8281-4574-a050-f9fec1c916f1`
and `4c8ed1e2-7524-467c-b4b3-32e8293c7854`, each independently satisfying
`rootSourceEvidenceArtifactId == evidence-44d61bfe-...` and
`DerivativeTransformation.OCR ∈ transformationHistory`. Under §5's query
shape, both are correctly and simultaneously representable in one discovery
result (`0..N`, no single slot), ordered deterministically by `generatedAt`
(§8), with neither overwriting, hiding, or superseding the other — unlike the
rejected UI-INGESTION-8 mechanism's single-slot, fail-closed-on-conflict
behaviour, which could represent only whichever generation happened to admit
first through that code path, permanently.

## 15. Adoption / Implementation Sequencing

This document authorises design only. A separate, dedicated implementation
unit — itself independently reviewed before any Kotlin is written — is
required, following this repository's existing convention (cf. the Reasoning
Context Bounded Semantic Relevance Amendment's own "does not itself authorise
implementation" clause). That implementation unit must also resolve, and is
not authorised to skip: which of §11(a)/§11(b) to build; the exact new
type/interface shapes; the exact new HTTP route (if any) exposing this to the
Owner UI, gated by the existing `isAuthorised` check; and focused tests
proving §9's re-validation discipline holds even against a
deliberately-wrong or stale candidate list. It must also perform the
UI-INGESTION-8 candidate cleanup this amendment's companion review specifies
(REMOVE the rejected authorization-store coupling; REWORK the discovery
trigger wiring to consume this capability instead) before any deployment.
