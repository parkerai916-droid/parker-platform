# Document Ingestion — Initial Tier A Implementation Closure

## 1. Purpose

This record formally closes the **initial Tier A implementation** of the
Document Ingestion Programme. It records the owner-accepted conclusion that
the implemented Tier A capability is technically complete within its governed
scope, with the explicit non-blocking deferrals listed below.

This is a closure record. It does not supersede the underlying governance,
widen Document Ingestion authority, resolve a deferred question, or itself
authorize owner-facing runtime invocation or Tier B.

## 2. Status

**CLOSED — READY WITH EXPLICIT NON-BLOCKING DEFERRALS.**

The initial Tier A implementation is accepted as complete within its governed
scope. This status does not mean that the entire Document Ingestion Programme
is finished, that every possible format or fidelity level is implemented, or
that any later integration or Tier B capability is authorized.

## 3. Authoritative governance basis

This closure is governed by the adopted Document Ingestion authority model and
its underlying sources, including:

- `DOCUMENT_INGESTION_PLUGIN_CONTRACT.md`;
- `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`;
- `DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md`;
- `DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md`;
- `DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md`;
- `DOCUMENT_INGESTION_AUDIT_AUTHORITY_SCOPE_LOCK.md`;
- `DOCUMENT_INGESTION_ADMISSION_AUDIT_ATOMIC_VISIBILITY_CLARIFICATION.md`;
- `DOCUMENT_INGESTION_PROGRAMME_GOVERNANCE_CLOSURE.md`; and
- `DOCUMENT_INGESTION_TIER_A_IMPLEMENTATION_PLAN.md`.

The programme governance closure remains an index and closure record rather
than a substitute authority. Where a summary or this document conflicts with
an adopted source, the adopted source governs. Draft cross-reference material
does not independently widen authority. The stale historical statement that
Evidence Intelligence is "paused" is not repeated as current governance.

## 4. Accepted implementation baseline

The accepted implementation chain is:

| Commit | Accepted scope |
|---|---|
| `6139902` | derivative-generation foundation |
| `fe8b389` | atomic admission/audit boundary clarification |
| `8f8b7ba` | governed CSV vertical slice |
| `08d8e02` | governed EML vertical slice |
| `294013b` | governed DOCX vertical slice |
| `a8be3fa` | governed searchable-PDF vertical slice |
| `a20b910` | governed Tier A routing and composition |

This record does not rewrite or squash that history. At closure review,
`HEAD` and `origin/main` both resolved to
`a20b91051560451d3cee62ebbb2952f52ebfac55`, and the working tree was clean.

## 5. Closed Tier A capability surface

Initial Tier A now includes:

1. derivative-generation identity and model;
2. durable, write-once derivative record and payload persistence;
3. atomic admission visibility;
4. ingestion audit sequencing from `ADMISSION_AUTHORISED`, through
   publication, to `ADMITTED`;
5. CSV mechanical structural extraction;
6. EML mechanical structural extraction;
7. DOCX mechanical structural extraction;
8. searchable-PDF mechanical text extraction with OCR disabled;
9. Tier A routing and internal composition;
10. a `RequiresTierB` boundary for scanned PDFs and PNG/images;
11. seven-fixture acceptance; and
12. source/derivative identity, lineage, and provenance preservation.

## 6. Specialist versions

The accepted specialists and pinned implementation versions are:

| Format | Mechanism | Version | Tier A claim |
|---|---|---:|---|
| CSV | Apache Commons CSV | 1.14.1 | literal record/field structure without type inference |
| EML | Apache James Mime4j | 0.8.14 | MIME/header/body structure and attachment candidates |
| DOCX | Apache POI XWPF | 5.5.1 | OOXML text and document structure |
| searchable PDF | Apache Tika PDF parser | 3.3.1 | encoded text-layer extraction with explicit `NO_OCR` |

These are derivative-producing mechanisms. Their output does not replace or
redefine authoritative source bytes.

## 7. Routing and composition status

The governed Tier A router mechanically identifies supported PDF, PNG, DOCX,
EML, and declared CSV inputs, preserves declared/detected disagreement, treats
filenames as non-authoritative, and selects exactly one specialist. Searchable
PDF, CSV, EML, and DOCX can proceed through Tier A admission. Scanned PDF and
PNG/image inputs stop at `RequiresTierB` with no derivative admission side
effects.

`TierADocumentIngestionComposition` supplies the internal composition factory
for the router, specialists, coordinator, storage, and audit dependencies. Its
existence is not owner-facing runtime activation.

## 8. Seven-fixture acceptance summary

The immutable bake-off manifest and all seven source fixtures were retained
unchanged. Acceptance established:

| Fixture | Accepted result |
|---|---|
| 01 — searchable PDF | admitted Tier A derivative |
| 02 — qualified searchable PDF | admitted with explicit fidelity qualifications |
| 03 — scanned PDF | `RequiresTierB`; no admission side effects |
| 04 — structured DOCX | admitted Tier A derivative |
| 05 — structured EML | admitted Tier A derivative with attachment candidate |
| 06 — literal CSV | admitted Tier A derivative |
| 07 — PNG/image | `RequiresTierB`; no admission side effects |

The manifest-driven acceptance checks, including governed source hashes and
format-specific controls, passed. No fixture was edited or regenerated.

## 9. Admission and audit invariant

Successful admission preserves this ordering:

`prepare → ADMISSION_AUTHORISED audit → atomic publication → ADMITTED audit`

Prepared material is not admitted or retrieval-visible. Publication is
write-once and cannot overwrite an existing generation. A failed authorization
audit prevents publication. A publication failure cannot produce an
`ADMITTED` audit. A post-publication admitted-audit failure leaves the
published record and payload available for explicit reconciliation rather than
misrepresenting them as absent.

## 10. Source immutability and provenance status

Tier A reads source bytes but never rewrites, normalizes, regenerates, deletes,
or replaces them. Source identity and digest remain independent of every
derivative. Each admitted generation retains its own immutable identity,
producer/configuration information, content identity, completeness, warnings,
and lineage to its authoritative root source.

Reprocessing creates a new generation and preserves prior generation history.
A derivative has no upward authority over its source. An EML attachment remains
a child-source candidate unless a separate governed acceptance later admits its
exact decoded bytes.

## 11. Authority-boundary confirmation

Initial Tier A acquired no authority to:

- mutate or replace source evidence;
- assign canonical Memory Core status;
- create or promote Knowledge as an ingestion side effect;
- promote Evidence Intelligence candidates to canonical facts;
- make RKS/QMD or another index a source of canonical truth;
- admit attachment candidates as child evidence automatically; or
- invoke OCR or another Tier B mechanism.

No Memory Core, Knowledge, or Evidence Intelligence integration is claimed
complete by this closure. Evidence Intelligence is not paused, but it remains a
separately governed authority boundary.

## 12. Explicit fidelity qualifications

The accepted Tier A fidelity claims remain deliberately bounded:

- CSV preserves parsed literal field values and structure, not original quote
  characters or byte offsets; a BOM is retained literally and warned about.
- EML preserves the MIME tree, supported raw/semantic headers, bodies, and
  decoded attachment bytes, but does not automatically admit attachments or
  expand richer nested-message semantics.
- DOCX preserves supported OOXML paragraphs, runs, styles, lists, tables,
  explicit page breaks, headers, footers, metadata, relationships, and package
  inventory; it does not claim rendered layout, coordinates, or complete
  complex numbering/table semantics.
- searchable PDF preserves extracted text-layer characters and page-count
  metadata where available; it does not claim page-associated text, correct
  multi-column reading order, rendered layout, coordinates, table
  reconstruction, or OCR.

These qualifications are part of the closed capability surface, not hidden
failures and not authority to infer missing structure.

## 13. Explicit non-blocking deferrals

The following remain deferred and are not resolved by closure:

- historical-generation retention and purge policy;
- audit retrieval/query capability;
- audit retention/deletion policy and authority;
- optional Memory Core registration and which coordinator would perform it;
- whether every derivative should ever receive a Memory record;
- Memory/Knowledge deletion propagation for future references;
- attachment child-source admission and materialization;
- richer nested EML expansion;
- rendered DOCX/PDF layout fidelity;
- Tier B Memory-registration mechanics; and
- OCR Mechanism Unit 12 runtime composition, including its outstanding
  permission/resource authority issue.

These items do not block initial Tier A closure. They remain subject to their
own governance and may not be treated as implied implementation authority.

## 14. OCR and Tier B boundary

Initial Tier A closure **does not authorize production OCR or Tier B
invocation**. OCR is not implemented by this programme slice and is not invoked
by its routing path. Tier A may return `RequiresTierB`; it may not call Tier B
merely because initial Tier A is closed.

OCR Mechanism Unit 12 runtime-composition governance remains unresolved,
including the outstanding permission/resource authority issue identified by
canonical OCR governance. That issue blocks production OCR/Tier B invocation,
not initial Tier A closure.

## 15. Runtime invocation boundary

The internal Tier A composition factory exists. It is not wired into
owner-facing Parker runtime handling.

This closure does not authorize or claim the existence of:

- owner-message ingestion;
- UI ingestion;
- filesystem watchers;
- Gmail ingestion;
- production evidence-directory ingestion;
- automatic evidence processing; or
- ingestion of real evidence.

Any invocation boundary requires a separately authorized implementation unit.

## 16. Test evidence

At the accepted implementation baseline, a forced fresh full-suite execution
completed successfully:

```text
./gradlew test --no-daemon --rerun-tasks
BUILD SUCCESSFUL
2374 tests
0 failures
0 errors
7 skipped/ignored
```

The suite includes contract, specialist, coordinator, persistence, audit,
atomicity, routing, hostile-input, resource-bound, and seven-fixture acceptance
coverage. No production OCR, Tier B invocation, or real-evidence ingestion was
used to obtain this result.

## 17. Repository convergence

Before creation of this closure record, the authoritative repository was on
`main`, its working tree was clean, and:

```text
HEAD        a20b91051560451d3cee62ebbb2952f52ebfac55
origin/main a20b91051560451d3cee62ebbb2952f52ebfac55
```

The accepted implementation was therefore converged before this documentation-
only closure commit. The closure commit changes this document only.

## 18. Closure decision

**READY WITH EXPLICIT NON-BLOCKING DEFERRALS.**

The initial Tier A Document Ingestion implementation is formally closed as
technically complete within its governed scope. This decision records closure;
it does not supersede underlying governance, widen authority, authorize runtime
invocation, authorize Tier B, claim OCR implementation, claim ingestion of real
evidence, or close the entire Document Ingestion Programme forever.

## 19. Next separately authorized stage

The recommended next unit is:

**Owner-Facing Tier A Runtime Invocation Boundary**

That unit must separately define and authorize how an owner-controlled runtime
request reaches the existing Tier A composition while preserving authorization,
source custody, audit, resource limits, and the `RequiresTierB` stop boundary.
It is not begun or authorized by this closure record.
