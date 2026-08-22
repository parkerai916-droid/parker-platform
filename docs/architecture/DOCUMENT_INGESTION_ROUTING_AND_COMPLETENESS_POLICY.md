# Document Ingestion Routing and Completeness Policy

## Status

**Draft for human review. Governance Unit 1.** This document proposes Parker-
owned routing, outcome, completeness and audit policy. It does not select or wire
production dependencies.

## 1. Parker-owned routing

Routing evaluates, in order: custodied source identity; received media type;
Parker-controlled signature/magic detection; detected media type; structural
observations; plugin capability description; authorisation; security/resource
policy; and the requested derivative purpose. Filename extension is only a hint.
Contradiction among declared type, detected type and signature is disclosed and
may block or route to safe inspection; it is never silently normalised.

Parker—not a plugin—selects zero, one or multiple capabilities. Multiple runs on
the same source are separate attempts and generations with independent audit and
completeness assessments.

## 2. Provisional specialist policy

| Source/purpose | Provisional capability | Output authority and caveat |
|---|---|---|
| Searchable PDF literal text | Tika | Literal-text derivative; current Parker path is fast/lightweight and preserved the tested em dash; not full format/OCR coverage |
| Searchable PDF layout/tables | Docling, only when justified | Independent structural derivative; observed em-dash change and convenience-export omissions must be disclosed |
| Scanned PDF or image | Docling OCR/layout | OCR derivative, never source text; currently demonstrated, resource/model controls required |
| EML | Mime4j | MIME/header/body derivatives and decoded attachment candidates; byte authority retained when charset absent |
| CSV | Commons CSV | Record/field structural derivative; preserves lexical strings/whitespace/empty fields; raw quoting and byte spans not directly exposed |
| DOCX native text/structure | Apache POI XWPF | OOXML structural derivative; source package never replaced because round-trip changed package bytes and 13/19 part hashes |
| DOCX secondary visual/layout view | Docling, only when justified | Independent interpretation; observed page-break loss, weak metadata and furniture limitations disclosed |
| Tesseract | none yet | Blocked/deferred; not installed, so no OCR-quality conclusion |
| Unstructured | none presently | Discontinued on operational feasibility, not technically disproven |

This table is evidence-backed provisional routing, not a hard-coded registry.
Changing a preferred capability/configuration requires policy review and produces
new generations; historical derivatives retain their original identities.

Every row belongs to exactly one of the three governed tiers defined in the
Plugin Contract Section 9.1. Tika, Mime4j, Commons CSV, and Apache POI XWPF rows
are Tier A (deterministic/mechanical); routing them requires no gate beyond this
policy's own authorisation/security checks. Docling rows (OCR/layout, DOCX
secondary view) and any future OCR engine are Tier B (recognition/model-backed)
and require the existing `OcrMechanism`/Evidence Intelligence authorisation
boundary in addition to this policy's routing checks — this is Owner Decision 6,
resolved in Section 8. No row is, or may become, Tier C; evidential/legal
reasoning about routed output is always downstream and out of this policy's
scope.

## 3. Outcomes

Do not collapse Parker's existing typed outcome families. A future general
attempt envelope should map them without erasing distinctions:

- `Completed`: operation ran and produced candidates; never implies completeness;
- `CompletedWithWarnings`: usable candidates plus explicit non-fatal conditions;
- `Partial`: usable output with known missing/degraded scope;
- `Unsupported`: capability does not support the observed input/purpose;
- `Failed`: operational/dependency or genuine implementation failure, kept
  distinguishable where the underlying contract distinguishes them;
- `BlockedByPolicy`: authorisation/security policy stopped before invocation;
- `BlockedByResource`: declared/enforced resource condition prevented completion;
- `CorruptOrMalformedSource`: source cannot be parsed or integrity fails.

Concrete names require a scope lock. Existing `ExtractionOutcome` and
`OcrRecognitionOutcome` values are preserved verbatim in audit/mapping. A generic
wrapper may add context but must never turn `RequiresOcr`, `Malformed`,
`PartialOrDegradedOutput`, `NotAuthorised`, or a thrown fault into plain success.

## 4. Completeness

Completeness is an evidence-supported assessment, separate from operational
outcome:

- `AccountedFor`: declared checks found all determinable expected units;
- `AccountedForWithQualifications`: accounting succeeded within explicit limits
  or unknowns;
- `KnownIncomplete`: one or more expected units are missing/degraded;
- `NotAssessable`: the format/plugin exposes insufficient basis for accounting;
- `NotApplicable`: the requested derivative has no meaningful unit inventory.

These names are governance vocabulary pending scope lock. `AccountedFor` means
only the named checks passed, never absolute evidential completeness — never a
metaphysical or absolute claim that no unknown component exists. It states only
what Parker successfully accounted for, what it could not account for, and what
was not assessable.

**Owner Decision — resolved.** Parker requires the minimum format-specific
accounting checks below for supported formats. These are mandatory checks, not
best-effort suggestions: a supported-format attempt that does not run them is
not a complete attempt. Which further checks beyond this minimum become
mandatory for a given format remains an ordinary scope-lock/implementation-
planning decision, not fixed here.

Minimum mandatory checks per format:

- **EML**: MIME structure accounted for (tree traversed, every leaf
  classified); body alternatives accounted for where discoverable; attachment
  count accounted for, with each decoded/refused/failed disposition; malformed
  or unreadable entities disclosed.
- **CSV**: record structure accounted for; field structure accounted for
  (field count per record, dialect/encoding decisions, empty fields); parse
  failures disclosed; raw quoting/span absence disclosed.
- **DOCX**: principal OOXML parts inventoried; body accounted for; headers/
  footers accounted for where present; tables accounted for where present;
  relationships accounted for where discoverable; declared-support limits
  disclosed.
- **PDF (searchable)**: page count accounted for where determinable;
  extraction status recorded per page; embedded/attached content reported
  where detectable.
- **Scanned PDF**: page count accounted for; rasterisation status explicit per
  page; OCR execution/result status explicit per page.
- **Image**: image decode status explicit; OCR execution/result status
  explicit when OCR is requested; every requested image/page scope accounted
  for, including no-content and degraded regions where the mechanism can
  report them.

These are minimum accounting requirements, not guarantees that no unknown
component exists.

**Mandatory-check failure semantics.** A failed mandatory accounting check:

- must not destroy an otherwise useful derivative — the derivative is retained
  with its completeness assessment attached, never discarded because a check
  failed;
- must not erase partial results — whatever was successfully extracted or
  accounted for remains present and retrievable;
- must not permit the extraction to be represented as fully accounted for —
  the outcome cannot be reported as `AccountedFor`;
- must produce an explicit qualified/incomplete outcome —
  `AccountedForWithQualifications`, `KnownIncomplete`, or `NotAssessable`, as
  the specific failure warrants, never a silent omission;
- must be auditable — recorded in the attempt audit (Section 5) with which
  check failed and why.

Docling returning success for EML while omitting an attachment is therefore
operational completion plus `KnownIncomplete`, not complete ingestion.

## 5. Attempt audit

Every attempted route—including a pre-invocation block—produces a durable audit
record through a future purpose-built governed port. The current generic
`AuditService` has no implementation, while Evidence Custodian deletion uses a
purpose-built durable log; this draft does not pretend either already supplies an
ingestion audit.

Required facts are: attempt/correlation ID; source ID, hash algorithm/digest and
length; requesting/submitting principal and authorisation decision references;
received/detected media type and detector; selected plugin/parser/mechanism,
adapter and versions; configuration identity/hash; model identity where used;
start/end times; requested output kinds; operational outcome and original typed
sub-outcome; warnings/errors; ordered transformations; derivative generation IDs,
hashes/lengths and custody/provenance results; completeness assessment, checks,
unknowns and omissions; resource-limit events; and pre/post source-integrity
verification.

Audit failure is fail-closed before declaring successful governed acceptance. An
attempt record must not claim a derivative was custodied or registered until the
corresponding durable operation actually succeeded.

## 6. Security/resource policy

Routing must refuse or isolate inputs that exceed configured byte, expansion,
part, recursion, page, pixel, record, time, CPU, memory or process limits. Archive
and MIME nesting are bounded. Temporary files use Parker-controlled names and
locations, do not trust attachment filenames, cannot escape their root, and are
cleaned after outcome recording. Network is denied by default. External processes
and models require declared capability plus explicit policy. Embedded executable
content, macros, links and package relationships are data, never executed.

## 7. Downstream boundaries

Plugins cannot call `MemoryCore`, `KnowledgeStore`, Knowledge Items,
`KnowledgeSource`, `DefaultReasoningKnowledgeSource`, `QmdRelevanceMechanism`, or
RKS/QMD indexes. This is not a new prohibition invented for ingestion: ADR-024
already forbids any module from writing "directly to Task, Agent Run, Planning
Session, Memory, or World Model state," and an ingestion plugin is an ordinary
module/`Principal` under that rule. Parker-owned coordination may accept/register
derivatives only after authorisation, validation and audit using existing
governed mechanisms — the same `MemoryAdmissionCoordinator`-style promotion path
RKS/QMD governance already reserves exclusively for the Remember/promotion path.
RKS/QMD is retrieval/relevance over already-promoted knowledge, never a second
durable or indexed content source and never an ingestion sink; it caches or
persists nothing. `DefaultReasoningContextAssembler` only consumes and renders
already-assembled Reasoning Context and is not, and cannot become, an ingestion
coordinator.

## 8. Owner decisions

Six owner decisions were open pending human review. All six are now resolved by
owner selection; none conflicts with a stronger existing constitutional rule.

1. **Resolved — non-byte structural derivatives.** Not every durable derivative
   is forced into Evidence Custodian's `EvidenceArtifact`. A derivative with
   independently preserved bytes is custodied as today; a structural derivative
   with no single byte representation gets a separately custodied Derivative
   Generation Record (Provenance Model Section 7, item 2). This does not
   overload Evidence Artifact semantics.
2. **Resolved — child-source acceptance is explicit, never automatic.** A
   plugin may only propose candidate child bytes. Parker's ingestion
   coordinator must separately accept them through Evidence Custodian's
   governed custody boundary before they carry child-source authority; no
   validated-decoding event by itself confers custody.
3. **Resolved — hash agility.** SHA-256 is the required current algorithm,
   represented as an (algorithm, digest) pair, never a bare digest with an
   assumed algorithm (Provenance Model Section 1). If a manifest/provenance
   envelope itself becomes hash-addressed, its serialisation must be
   deterministic and canonical before its digest carries evidential meaning.
   No existing ID is redesigned.
4. **Resolved — ingestion audit is Parker-owned infrastructure.** It is owned
   by neither parser plugins, Memory Core, Knowledge Items, nor RKS/QMD.
   Plugins may emit audit-relevant facts; Parker's ingestion coordinator
   records the authoritative attempt audit (Section 5). The current absence of
   a general ingestion-audit persistence port is a future implementation
   requirement, tracked in the amendment map document, following the existing
   `FileSystemEvidenceDeletionAudit` durable-log pattern rather than waiting on
   a general-purpose `AuditService` that does not yet exist.
5. **Resolved — completeness is format-specific and incremental, with a
   mandatory minimum.** The owner decision fixes both the *shape* (Section 4's
   five-state vocabulary, format-specific accounting, no universal "complete
   document" test) and a *mandatory minimum check set* per supported format
   (Section 4), plus the failure-semantics rule that a failed mandatory check
   must degrade the completeness assessment honestly, never destroy or hide a
   derivative, and always remain auditable. Whether any *additional* check
   beyond this minimum becomes mandatory for a given format remains an
   ordinary scope-lock/implementation-planning decision, not a constitutional
   one, and is the one item still open — see the Remaining Owner Decisions
   section of the governing review.
6. **Resolved — model-backed Docling gate.** Model-backed document
   interpretation (Tier B: OCR, layout inference, model-backed table
   reconstruction) requires the existing governed `OcrMechanism`/Evidence
   Intelligence authorisation boundary in addition to ordinary ingestion
   routing, because its transformation characteristics, reproducibility,
   resource requirements, and model/version dependencies differ materially
   from Tier A mechanical parsing (Plugin Contract Section 9.1). Not every
   Docling operation is thereby "legal/evidential reasoning" (Tier C remains
   distinct and is never entered by ingestion); it is Tier B, gated, but not
   Tier C, forbidden.

## 9. Acceptance gate

This policy is ready only for human architecture review. Implementation remains
blocked until the amendments in `DOCUMENT_INGESTION_GOVERNANCE_AMENDMENT_MAP.md`
are carried out, a scope lock fixes concrete types and authority, and an
implementation plan maps work to reviewable units.
