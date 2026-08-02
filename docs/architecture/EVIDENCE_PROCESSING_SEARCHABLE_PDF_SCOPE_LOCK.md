# Evidence Processing (Searchable PDF) — Scope Lock

## Status

**Accepted. Canonical. Frozen.** Independent Review and Final Freeze
Verification have both been completed. This Scope Lock is adopted as
normative Parker governance for the Evidence Processing (Searchable PDF)
programme. Implements
`docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`
("the Boundary Clarification" — Accepted, Canonical, Frozen, committed and
pushed at commit `11b6f33`) without reopening it. Every determination,
classification boundary, contract shape, sequencing rule, and failure
semantic already fixed by the Boundary Clarification is treated here as
given, not re-derived. This document adds nothing the Boundary
Clarification did not already authorise; it only divides that authorised
shape into implementable units and fixes their boundaries and
dependencies — exact files and verified dependency coordinates are the
companion Implementation Plan's own responsibility (Implementation Plan
Section 1).

Also binding, unmodified, and not reopened by this document:
`docs/decisions/CDR-006_CONSTITUTIONAL_CLASSIFICATION_OF_ORIGINAL_EVIDENCE_CUSTODY_AND_IMMUTABILITY.md`
("CDR-006"),
`docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md`
("CDR-007"), `docs/architecture/EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md` ("the
Contract Design"), `docs/architecture/EVIDENCE_CUSTODIAN_SCOPE_LOCK.md`,
`docs/architecture/EVIDENCE_CUSTODIAN_IMPLEMENTATION_PLAN.md`,
`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md`, and the existing
`EvidenceRegistrationCoordinator` contract
(`src/runtime/EvidenceRegistrationCoordinator.kt`). The paused
`docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` remains
untouched, unmodified, and unincorporated — nothing in this document reads
it, references it for scope, or resumes it.

Programme: **Evidence Processing — Searchable PDF, Scope Lock.**

---

## 1. Executive Summary

This Scope Lock and its companion Implementation Plan authorise Parker's
first operational document-processing capability: a coordinator that
takes a custodied original evidence artefact already known to Memory
Core, verifies its identity pairing, retrieves it through Evidence
Custodian, verifies its integrity, detects whether it is a searchable
PDF, extracts its already-encoded text layer using Apache Tika, registers
the extracted text as a new, separately custodied and registered
derivative, and places that derivative into a governed human-review
queue. Nothing is exposed to Evidence Intelligence, and Evidence
Intelligence itself remains paused — this program builds and proves the
pipeline that will one day feed it, not the consumer.

Seven units, in strict dependency order: (1) extraction contracts, (2)
the Apache Tika adapter, (3) the human review registry, (4) the
coordinator — implemented and reviewed as two commit-bounded sub-units,
4A and 4B, per the companion Implementation Plan; the coordinator itself
remains one class with one five-dependency constructor and one
operation, never two — (5) production composition, (6) verification, (7)
real-world operational proof. Units 1–4B introduce no third-party type
into any Parker-owned interface; Unit 2 is the only place
`org.apache.tika.*` may ever be imported, exactly as the Boundary
Clarification (Section 3) requires.

---

## 2. Constitutional Purpose

The Boundary Clarification already settled the constitutional question:
deterministic extraction of an already-encoded PDF text layer is
mechanical file-format parsing, not an exercise of Evidence Intelligence's
analytical functions (Determination 1). This programme exists to give
that determination a real, tested, narrowly-scoped implementation —
nothing more. It does not decide, revisit, or extend the classification
boundary; it builds inside it.

---

## 3. In-Scope Responsibilities

1. **Extraction contracts** (`EvidenceExtractor`, `ExtractionResult`,
   `ExtractionOutcome`, `ExtractionIdentity`, `IntegrityVerificationOutcome`,
   `EvidenceExtractionOutcome`) — Boundary Clarification Sections 3, 4, 8.
2. **The Apache Tika adapter** (`TikaEvidenceExtractor`) — the sole
   production implementation of `EvidenceExtractor`, and the only file in
   this repository permitted to import `org.apache.tika.*` — Boundary
   Clarification Section 3.
3. **The human review registry** (`DerivativeReviewState`,
   `DerivativeReviewRecord`, `DerivativeReviewRegistry`,
   `InMemoryDerivativeReviewRegistry`) — Boundary Clarification Section 6.
4. **The coordinator** (`EvidenceExtractionCoordinator`) — sequencing
   identity verification, retrieval, integrity verification, extraction,
   derivative registration, and initial review-state recording — Boundary
   Clarification Section 7.
5. **Production composition** — wiring the above into `ParkerRuntime`,
   registering only the Resources and action-vocabulary entries the
   Boundary Clarification's own disclosed-but-unregistered conventions
   name, and three thin entry points (extract, record review decision,
   retrieve approved text).
6. **Verification** — targeted and full test suites, structural
   Tika-leakage tests, fail-closed integrity tests, immutable-correction
   tests, and one production-graph integration test.
7. **Real-world operational proof** — processing a five-file real-world
   corpus (two independently-sourced searchable PDFs, one scanned/
   image-only PDF, one malformed PDF, one unsupported file) end-to-end,
   locally, never committed; one small synthetic searchable PDF fixture
   committed for deterministic CI.

---

## 4. Explicit Exclusions

Restating the Boundary Clarification's own exclusions (Section 2) as
binding implementation constraints, plus the operational constraints this
task adds:

- No OCR, no Tesseract, no image interpretation of any kind.
- No transcription, no translation, no summarisation, no comparison, no
  inference.
- No Knowledge Memory promotion.
- No Evidence Intelligence reasoning of any kind — CDR-007 and the paused
  Evidence Intelligence Contract Design are not reopened, narrowed, or
  resumed.
- No Tika Server — in-process JVM integration only (Unit 2).
- No network communication, cloud service, or remote parser may be
  invoked during extraction — extraction is deterministic and completely
  local (Unit 2).
- No database — every new component is either a pure computation
  (coordinator, contracts) or an in-memory store
  (`InMemoryDerivativeReviewRegistry`), consistent with the Boundary
  Clarification's own "durability explicitly scoped down for this first
  unit" determination (Section 6).
- No UI framework, no general workflow engine, no platform-wide audit
  framework — `DerivativeReviewRegistry` remains the narrow, two-operation
  contract the Boundary Clarification fixed, never a general state
  machine or notification system.
- No mutation of original or derivative evidence — every write is a new,
  separately identified record; nothing already accepted, registered, or
  reviewed is ever edited in place.
- No Apache Tika type may cross a Parker-owned interface — enforced
  structurally by the reflection and source-scan tests introduced in
  Units 1–2, re-confirmed by Unit 6, not merely by convention.
- No real Uber, WelTec, ERA, or other personal evidence may be committed
  to Git, at any point, in any form — Unit 7's own case files remain
  local and gitignored.
- No change to CDR-006, CDR-007, the Contract Design, the Evidence
  Custodian Scope Lock, the Evidence Custodian Implementation Plan, or
  the paused Evidence Intelligence Contract Design.
- No new Memory Core field — every fact this programme records reuses an
  existing `Provenance`/`Document`/`CandidateProvenance`/`CandidateDocument`
  field, exactly as Boundary Clarification Section 4 fixes.
- No new `ParkerRuntimeConfig` field — `TikaEvidenceExtractor`'s
  configuration (embedded-resource handling, OCR strategy disabled,
  extraction-threshold constant) is fixed, compiled-in behaviour for this
  first unit, not externally tunable; `InMemoryDerivativeReviewRegistry`
  requires no path or connection string. If this changes, that is a
  future Scope Lock's decision, not this one's.

---

## 5. Subsystem Boundaries

`EvidenceExtractionCoordinator` holds exactly five dependencies —
`EvidenceCustodian`, `MemoryRetrieval`, `EvidenceExtractor`,
`EvidenceRegistrationCoordinator`, `DerivativeReviewRegistry` — and no
`PermissionEngine` reference of its own (Boundary Clarification Section
7). This mirrors `EvidenceRegistrationCoordinator`'s own existing
"neither subsystem holds a reference to the other" discipline exactly:
`EvidenceExtractor` and `DerivativeReviewRegistry` hold no dependency of
their own (no `EvidenceCustodian`, no `MemoryCore`, no `PermissionEngine`)
— `EvidenceExtractor` mirrors `ReasoningProvider`'s "pure callee, calls
nothing" shape; `DerivativeReviewRegistry` mirrors `EvidenceDeletionAudit`'s
"narrow, purpose-built port" shape.

`EvidenceRegistrationCoordinator` itself is reused entirely unchanged —
this programme adds no new parameter, no new method, and no new outcome
variant to it. The extraction coordinator is a new caller of an existing,
frozen contract, exactly as any other caller would be.

`ParkerRuntime` remains a composition root only (Unit 5) — it decides no
new architecture, gates no new behaviour beyond registering the
Resources/actions the Boundary Clarification already disclosed, and
delegates unchanged, mirroring its own existing `submitEvidence`/
`retrieveEvidence`/`deleteEvidenceAsOwner` precedent exactly.

---

## 6. Extraction Identity and Integrity Verification

Two structural corrections the Boundary Clarification's own second
verification pass fixed (Sections 4, 7, 8, 9) are binding implementation
requirements, not optional refinements:

- **`ExtractionIdentity`** is one small, flat, structured type —
  `parserIdentity`, `configurationProfile`, `normalisationProfile` — never
  three independently-worded `processingHistory` sentences. `Unit 1`
  implements it as part of `ExtractionResult`'s own shape
  (`src/interfaces/EvidenceExtractor.kt`).
- **`IntegrityVerificationOutcome`** is a sealed type with exactly three
  variants — `Verified`, `Mismatch`, `Unverifiable` — and `Mismatch`/
  `Unverifiable` fail closed identically: no extraction proceeds, no
  derivative is created, for either. `Unit 4A`'s own coordinator sequence
  (step 4) is the sole place this outcome is produced.

Neither type may gain a variant, a default value, or a fallback path this
programme's own tests do not directly exercise.

---

## 7. Human Review Lifecycle

`DerivativeReviewState` — exactly four values: `PENDING_REVIEW`,
`APPROVED`, `REJECTED`, `NEEDS_CORRECTION`. `APPROVED`, `REJECTED`, and
`NEEDS_CORRECTION` are all terminal for their own identifier — a
correction is always a new coordinator run producing a new, separately
identified derivative starting its own `PENDING_REVIEW`, never a
transition back on the same identifier (Boundary Clarification Section
6). `Unit 3`'s own structural test suite must prove the transition graph
rejects every attempt to move an existing identifier from
`NEEDS_CORRECTION`, `APPROVED`, or `REJECTED` back to `PENDING_REVIEW`.

Only `APPROVED` is human-verified. No consumer — including a future,
still-paused Evidence Intelligence — may treat extracted text as
human-verified without querying `DerivativeReviewRegistry.currentReviewState`
and finding exactly `APPROVED`.

---

## 8. Permission Boundaries

Every new governed act this programme introduces follows the Boundary
Clarification's own disclosed-but-unregistered convention exactly, the
same convention `DefaultEvidenceCustodian` and `EvidenceRegistrationCoordinator`
already established: a fixed, well-known `ResourceId`/action-name pair
for extraction is named in Unit 4A's own code, registered nowhere until
Unit 5. The review-approval action has no earlier Kotlin-level
disclosure of its own — its underlying expectation is the Boundary
Clarification's own Section 6 disclosure; its concrete action-name
literal is first minted and registered together, in Unit 5. Until Unit 5
registers either, a real `DefaultPermissionPolicy` denies every request
through these paths — the same safe, conservative failure mode already
governing every existing Evidence Custodian path.

`EvidenceExtractionCoordinator` itself holds no `PermissionEngine`
reference (Section 5, above) — every governed act it triggers is already
gated by one of its five dependencies' own existing contracts
(`EvidenceCustodian.retrieve`, `EvidenceRegistrationCoordinator.register`).
It introduces no new Permission Engine evaluation of its own.

The `APPROVED` review-state transition is the one exception, and the
Boundary Clarification (Section 6, "Permission Engine expectation,
disclosed not decided") already fixes it: recording `APPROVED` is a
genuinely consequential act and a future Scope Lock must gate it. Unit 5
registers the disclosed `(action, resourceType)` pairing for it but does
not invent new policy content beyond the same "minimum required, narrow
in what it grants" discipline `ParkerRuntime`'s own existing rules
already use.

---

## 9. Change Control

No unit in this programme may reopen the Boundary Clarification, CDR-006,
CDR-007, the Contract Design, or the paused Evidence Intelligence
Contract Design. Any discovery during implementation that the frozen
Boundary Clarification's own contract shape is wrong, ambiguous, or
insufficient halts implementation and returns to governance review — it
is not silently reinterpreted or patched around in code. Any change to
this Scope Lock or its companion Implementation Plan after acceptance
follows the same discipline the Evidence Custodian Scope Lock already
established for itself (Section 10 there): a new, dated revision, never a
silent edit.

---

## 10. Verification Questions

1. Does `EvidenceExtractor`'s own public shape contain any
   `org.apache.tika.*` type, anywhere, including generic type parameters?
   (Must be no — Unit 6.)
2. Does any file other than `TikaEvidenceExtractor.kt` import
   `org.apache.tika.*`? (Must be no — Unit 6.)
3. Does `EvidenceExtractionCoordinator` construct, or ever call, a
   `PermissionEngine` directly? (Must be no — Unit 4A, Unit 4B.)
4. Does any code path allow `IntegrityVerificationOutcome.Mismatch` or
   `.Unverifiable` to proceed to extraction? (Must be no — Unit 4A, Unit
   6.)
5. Does any code path allow `DerivativeReviewState.NEEDS_CORRECTION`,
   `.APPROVED`, or `.REJECTED` to transition back to `PENDING_REVIEW` on
   the same identifier? (Must be no — Unit 3, Unit 6.)
6. Does `ParkerRuntime` gain any new coordinator-level responsibility
   beyond construction, registration, and delegation? (Must be no — Unit
   5.)
7. Is any real personal evidence file (Uber, WelTec, ERA, or otherwise)
   present anywhere in `git status`, staged or committed? (Must be no —
   Unit 7, verified before every commit.)
8. Does `Provenance`, `Document`, `CandidateProvenance`, or
   `CandidateDocument` gain a new field? (Must be no — Unit 1, Unit 4B.)
9. Can the exact same original evidence, processed twice with the same
   parser version, configuration profile, and normalisation profile,
   produce byte-identical extracted text and the same computed digest?
   (Must be yes, for deterministic searchable-PDF extraction — Unit 2,
   Unit 6.)

---

## Final Recommendation

This Scope Lock is Accepted, Canonical, and Frozen. Independent Review is
complete. Final Freeze Verification is complete. It authorises no
implementation on its own — no implementation occurred during this
governance stage. Together with its companion Implementation Plan it
fixes the unit boundaries, dependency list, and verification obligations
for Parker's first operational Evidence Processing capability,
implementing the frozen Boundary Clarification exactly, without
reopening it, CDR-006, CDR-007, the Contract Design, or the paused
Evidence Intelligence Contract Design. Unit 1 — Extraction Contracts is
now authorised to begin, but only after Steven completes repository
verification, staging, commit, and push.

EVIDENCE PROCESSING (SEARCHABLE PDF) SCOPE LOCK — ACCEPTED — CANONICAL —
FROZEN

Confirmed: no Kotlin implemented; no test written; the Boundary
Clarification, CDR-006, CDR-007, the Contract Design, the Evidence
Custodian Scope Lock, the Evidence Custodian Implementation Plan, Memory
Core's own contracts, `EvidenceRegistrationCoordinator`, and the paused
Evidence Intelligence Contract Design all unmodified by this document;
nothing staged; nothing committed; nothing pushed.
