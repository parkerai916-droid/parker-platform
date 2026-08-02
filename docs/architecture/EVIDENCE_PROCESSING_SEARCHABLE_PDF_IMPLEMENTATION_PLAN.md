# Evidence Processing (Searchable PDF) — Implementation Plan

## Status

**Accepted. Canonical. Frozen.** Independent Review and Final Freeze
Verification have both been completed. This Implementation Plan is
adopted as normative Parker governance for the Evidence Processing
(Searchable PDF) programme. Implements
`docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`
("the Boundary Clarification" — Accepted, Canonical, Frozen, committed and
pushed at commit `11b6f33`) and its own companion
`docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_SCOPE_LOCK.md` ("the
Scope Lock"), without reopening either. Fixes exact production and test
file lists, the responsibilities each new type carries, and verified
Apache Tika dependency coordinates for the units the Scope Lock (Section
3) already named. Concrete field names, parameter types, and constructor
shapes are the implementing unit's own decision, verified by that unit's
own tests — this plan fixes responsibility and behaviour, not source
code; it is not a parallel source file.

Programme: **Evidence Processing — Searchable PDF, Implementation Plan.**

---

## 1. Purpose

Where the Scope Lock fixes *what* each unit may and may not do, this
document fixes *exactly which files* each unit creates, *what
responsibility* each new type carries, and *exactly which dependency
coordinates* Unit 2 introduces — verified against Maven Central during
this planning pass, not left as a placeholder for implementation time.

---

## 2. Implementation Governance Rules

1. No unit begins until this document and its companion Scope Lock are
   accepted.
2. No unit may alter the Boundary Clarification, the Scope Lock, CDR-006,
   CDR-007, the Contract Design, `EvidenceRegistrationCoordinator`, or the
   paused Evidence Intelligence Contract Design.
3. Units execute in the order given (Section 4) — each depends on the
   previous unit's own completed, tested output. No unit's tests may be
   written against a type another, later unit has not yet introduced.
4. Every new governed act follows the existing "disclosed, unregistered
   convention" precedent (`DefaultEvidenceCustodian`,
   `EvidenceRegistrationCoordinator`) until Unit 5 registers it — no unit
   before Unit 5 registers a `Resource` or `ActionVocabulary` entry.
5. No unit introduces a Kotlin default value, fallback branch, or silent
   substitution for a case its own tests do not directly exercise.
6. **Extraction must be deterministic and completely local.** No network
   communication, cloud service, or remote parser may be invoked during
   extraction, now or in any future configuration of it — binding
   specifically on Unit 2, and on any unit that ever touches
   `TikaEvidenceExtractor`. Parker's trust model depends on this being
   stated, not merely assumed.
7. Steven performs formal verification, staging, commit, and push for
   every unit — no unit stages, commits, or pushes its own work.

---

## 3. Implementation Objectives

Build, test, and prove — against real evidence, processed locally and
never committed — a complete `custodied original → authorised retrieval →
identity verification → fail-closed integrity verification → media
detection → deterministic Tika extraction → derivative creation →
Evidence Custodian acceptance → provenance registration → structured
extraction-identity preservation → governed human review → approved
retrieval` pipeline, exactly as the Boundary Clarification's own target
sequence (Section 7) and this task's own stated end-to-end behaviour
describe it.

---

## 4. Major Implementation Units

### Unit 1 — Extraction Contracts

**Production:**

- New file: `src/interfaces/EvidenceExtractor.kt`, package
  `parker.core.interfaces`, mirroring `ReasoningProvider.kt`'s own
  "narrow, dependency-free interface plus its own sealed response type,
  one file" shape.

  Introduces an `EvidenceExtractor` interface with a single suspend
  extraction operation that takes the candidate content bytes and
  returns a sealed `ExtractionOutcome`. `ExtractionOutcome` carries
  exactly four variants: a successful case wrapping an `ExtractionResult`;
  a "requires OCR" case carrying the detected media type and, where
  known, the page count; an "unsupported" case carrying the detected
  media type, where known, and a reason; and a "malformed" case carrying
  a reason. `ExtractionResult` carries every fact Boundary Clarification
  Section 4's table assigns to the extractor's own responsibility: the
  extracted text itself, the detected media type, the extractor's own
  name and version, a structured extraction-identity value (parser
  identity, configuration profile, normalisation profile), the
  extraction timestamp, any warnings, the always-`false` OCR-used flag,
  embedded-resource observations (declared file name and media type
  only, never content), and the original document's own metadata as
  Tika itself reports it. `ExtractionResult` carries no digest field of
  any kind — digests are computed by the coordinator (Unit 4A), never by
  the extractor.

  The extraction operation is declared `suspend`, for consistency with
  every other Parker interface a coordinator calls (`EvidenceCustodian.accept`/`retrieve`,
  `MemoryCore.createProvenance`/`registerDocument`), even though this
  first implementation performs no genuinely asynchronous work itself
  (Unit 2) — a shape decision, not a behavioural one. `EvidenceExtractor`
  itself declares no constructor, no property, and no dependency of any
  kind (Boundary Clarification Section 3, "pure callee, calls nothing").
  Concrete field names, types, and constructor shapes for
  `ExtractionOutcome`, `ExtractionResult`, and the extraction-identity
  value are this Unit's own implementation decision, verified by its own
  tests, not fixed in advance by this plan.

**Tests:**

- New file: `tests/contracts/EvidenceExtractorScopeTest.kt` —
  structural shape checks only, mirroring `EvidenceCustodianScopeTest.kt`'s
  own Kotlin-reflection discipline (`KClass::declaredFunctions`, never
  raw `java.lang.reflect`):
  - `EvidenceExtractor` declares exactly one operation, public, suspend,
    abstract.
  - `ExtractionOutcome` has exactly four subclasses.
  - `ExtractionResult` and the extraction-identity value carry exactly
    the responsibilities described above — no digest-named field, no
    confidence field (deferred to `Provenance.confidence`, left `null`,
    not duplicated here).
  - No `org.apache.tika` type is reachable from any public
    `EvidenceExtractor`/`ExtractionResult`/`ExtractionOutcome` signature.
    **This assertion is written now but is only a meaningful test once
    Unit 2 adds the Tika dependency** — before Unit 2, it passes
    vacuously (no such type could appear, since Tika is not yet on the
    classpath at all). Unit 2 re-runs this exact test as its own first
    verification step, and it must still pass.

---

### Unit 2 — Apache Tika Adapter

**Verified dependency coordinates** (Maven Central, checked during this
planning pass — the Boundary Clarification's own Section 12 named
`3.3.2` as a placeholder pending this exact confirmation; the actual
latest stable Tika 3.x release is **3.3.1**, not `3.3.2`, which does not
exist on Maven Central):

- `org.apache.tika:tika-core:3.3.1`
- `org.apache.tika:tika-parser-pdf-module:3.3.1` — the scoped PDF-only
  parser module, not the full `tika-parsers-standard-package` bundle.
  Deliberately excludes any OCR module (`tika-parser-ocr-module` is a
  separate, unrelated artefact never added to this build) — OCR is
  therefore structurally unreachable, not merely unconfigured, closing
  Determination 2 and the "no Tesseract" constraint at the dependency
  level, not only the configuration level.

**Production:**

- **`build.gradle.kts`** — add both coordinates above to
  `dependencies { implementation(...) }`. This is the repository's first
  third-party (non-`kotlinx`) production dependency; no other change to
  `build.gradle.kts` is required (no new source set, no new plugin).
- New file: `src/runtime/TikaEvidenceExtractor.kt`, package
  `parker.core.runtime`, the sole implementation of `EvidenceExtractor`
  and the only file in this repository permitted to import
  `org.apache.tika.*`:
  - Constructs its own internal Tika parser and metadata objects per
    extraction call — no constructor parameter, matching
    `EvidenceExtractor`'s own "no dependency" contract exactly (an
    internal Tika object is not a Parker-owned dependency).
  - **Media detection.** Runs Tika's own detector first. Any
    non-PDF result becomes the "unsupported" outcome, carrying the
    detected type and a reason. A detection or parse exception genuinely
    indicating corrupt/truncated bytes becomes the "malformed" outcome,
    carrying the exception's own message as the reason — never silently
    retried, never reinterpreted as "unsupported."
  - **Configuration.** A single, fixed parse configuration: embedded-document
    handling configured to record presence/declared name/declared media
    type only — never parsing embedded content — and no OCR strategy
    set (Tika's default: OCR only activates if a Tesseract binary is
    present *and* explicitly configured — neither is true here, and
    `tika-parser-ocr-module` is not even on the classpath, Determination
    2). This fixed configuration is tagged with one constant string,
    recorded verbatim as the extraction-identity value's own
    configuration-profile field — not user-configurable (Scope Lock
    Section 4). Extraction performs no network communication of any
    kind: Tika's own detector and PDF parser operate entirely in-process
    against the bytes already supplied; no remote parser, cloud OCR or
    extraction service, or external network call is ever invoked by
    this Unit (Section 2, Rule 6, above).
  - **Searchable-versus-scanned detection.** After a successful parse,
    if the extracted text, trimmed, falls below a small fixed-character
    threshold (a compiled-in constant, twenty characters — small enough
    that no genuine searchable-text PDF is misclassified, large enough
    that Tika's own incidental whitespace/artefact output from a scanned
    page is not mistaken for real text) while the document reports at
    least one page, the outcome becomes "requires OCR," carrying the
    detected media type and page count where known — never a false,
    emptily "successful" extraction.
  - **Success.** The successful outcome carries `extractorName = "Apache
    Tika"`, `extractorVersion = "3.3.1"`, a parser-identity value equal
    to the actual concrete parser class Tika delegated to, a
    normalisation profile stating plainly that no post-extraction
    normalisation was applied (Boundary Clarification Section 4),
    `ocrUsed = false` always, and document metadata populated only from
    Tika's own reported original-document metadata (page count, declared
    producer, declared title) — never a duplicate of any fact the
    extraction-identity value or the digest fields already carry.

**Tests:**

- New file: `tests/runtime/TikaEvidenceExtractorTest.kt` — the real
  extractor against a test corpus:
  - One small synthetic searchable PDF, **committed to the repository**
    (e.g. `tests/fixtures/synthetic-searchable.pdf`, a few KB, generated
    once and checked in for deterministic CI — contains no real personal
    evidence).
  - One real searchable Uber PDF, one real searchable WelTec/ERA PDF, one
    malformed/unsupported file, one scanned (image-only) PDF — all read
    from a **gitignored** local fixtures directory (e.g.
    `tests/fixtures/local/`, added to `.gitignore` by this Unit); each
    test **skips gracefully** (not fails) if its file is absent, so CI
    (which never has these files) stays green while a local run with the
    real files present exercises them fully.
  - Asserts: searchable PDF → successful extraction with non-blank text
    and a correctly populated extraction-identity value; scanned PDF →
    "requires OCR"; malformed file → "malformed"; non-PDF/unsupported
    file → "unsupported"; **running extraction twice against the same
    synthetic fixture produces byte-identical extracted text and an
    identical extraction-identity value** (Scope Lock Verification
    Question 9's own determinism property, the one directly testable
    within this Unit).
- **`.gitignore`** — add the local fixtures directory, the one
  non-Kotlin file this Unit touches outside `src`/`tests`.
- Re-run `EvidenceExtractorScopeTest.kt`'s own Tika-leakage assertion
  (Unit 1) — must still find no `org.apache.tika` type reachable from any
  Parker-owned interface signature, now that Tika is genuinely on the
  classpath. Add one further structural test in the same file: **no file
  in `src/` other than `TikaEvidenceExtractor.kt` contains the string
  `org.apache.tika`** (a source-scan test, not a reflection test — the
  only way to catch an import that never appears in any public signature
  but still leaks a third-party type into, say, a private field or a log
  line).

---

### Unit 3 — Human Review Registry

**Production:**

- New file: `src/interfaces/DerivativeReview.kt`, package
  `parker.core.interfaces`, mirroring `EvidenceDeletionAudit.kt`'s own
  "narrow, purpose-built port; no dependency of its own" shape.

  Introduces a review-state enumeration with exactly four values —
  pending review, approved, rejected, needs correction — a review-record
  type carrying the derivative's own evidence-artefact identifier, its
  own document identifier, the state being recorded, a timestamp, an
  optional reviewing principal, and an optional free-text note; and a
  `DerivativeReviewRegistry` interface with exactly two suspend
  operations: recording a new review state, and reading the current
  review state for a given evidence-artefact identifier. No third
  operation, and no invented result type — recording returns nothing on
  success and rejects an invalid transition attempt by throwing,
  mirroring `MemoryCoreLifecycleTransitions.requireValidTransition`'s own
  already-established, directly-on-point precedent
  (`src/runtime/InMemoryMemoryCore.kt`) for exactly this kind of check,
  rather than a new outcome type or exception hierarchy. Reading the
  current state for an identifier with no review record at all returns
  an explicit absence, distinct from returning "pending review"
  (Boundary Clarification Section 6, "an identifier with no review
  record ... is a distinct, and itself reportable, condition"). Concrete
  field names and types are this Unit's own implementation decision,
  verified by its own tests.
- New file: `src/runtime/InMemoryDerivativeReviewRegistry.kt`, package
  `parker.core.runtime`. An append-only, mutex-guarded in-memory store
  (mirroring `InMemoryMemoryCore`'s own locking convention). Recording a
  new state looks up the most recently recorded state for the target
  identifier (absent if none), validates the transition against a fixed
  graph — absent state may only move to "pending review"; "pending
  review" may move to approved, rejected, or needs-correction; every
  other pair, including any transition away from approved, rejected, or
  needs-correction (all three terminal for their own identifier,
  Boundary Clarification Section 6, as corrected), is rejected — and
  only then appends the new record. Nothing already appended is ever
  mutated or removed. Reading the current state returns the most
  recently appended record's own state for that identifier, or the
  explicit absence.

**Tests:**

- New file: `tests/contracts/DerivativeReviewScopeTest.kt` — structural:
  the state enumeration has exactly four values; `DerivativeReviewRegistry`
  declares exactly two operations, both suspend, both public, both
  abstract.
- New file: `tests/runtime/InMemoryDerivativeReviewRegistryTest.kt` —
  behavioural: the initial transition into pending review succeeds;
  every valid transition succeeds; every invalid transition throws,
  **including explicitly a needs-correction record attempting to
  transition back to pending review on the same identifier** (the exact
  transition the Boundary Clarification's own second verification pass
  removed — this is the test that keeps it removed); reading the state
  of an unknown identifier returns the explicit absence, not "pending
  review"; append-only history is preserved (recording "approved" after
  "pending review" leaves both records readable in order, never
  overwrites the first); current state always resolves to the most
  recently appended record.

---

### Unit 4A — Coordinator: Identity, Integrity, and Extraction

**Production:**

- New file: `src/runtime/EvidenceExtractionCoordinator.kt`, package
  `parker.core.runtime`. Introduces the `EvidenceExtractionCoordinator`
  class itself, with exactly five dependencies — `EvidenceCustodian`,
  `MemoryRetrieval`, `EvidenceExtractor`, `EvidenceRegistrationCoordinator`,
  `DerivativeReviewRegistry` — and no `PermissionEngine` reference,
  matching the Scope Lock (Section 5) and Boundary Clarification (Section
  7) exactly. This split is a commit boundary only — the coordinator
  remains one class with one five-dependency constructor and one public
  operation; it is not restructured into two classes or two entry
  points. This Unit implements that operation's own first five steps:

  1. Look up the original Document by its supplied document identifier
     via `MemoryRetrieval.getDocument`; absent → a terminal "source
     document not found" outcome.
  2. Compare the found Document's own location reference against the
     supplied evidence-artefact identifier; mismatch → a terminal
     "source identity mismatch" outcome, before any retrieval occurs.
  3. Retrieve the original bytes through `EvidenceCustodian.retrieve`; a
     rejected or not-found result → the corresponding terminal outcome,
     wrapped unchanged.
  4. Compute the SHA-256 of the retrieved bytes, then separately verify
     it against the Document's own recorded integrity hash, producing
     one of exactly three outcomes this Unit defines — verified,
     mismatch, or unverifiable (no recorded hash to compare against, or
     the comparison otherwise cannot be performed). Only a verified
     outcome continues; mismatch and unverifiable both fail closed
     identically — no extraction is attempted, no candidate of any kind
     is constructed, and the operation returns the corresponding
     terminal outcome.
  5. Invoke `EvidenceExtractor.extract` on the verified bytes; a
     "requires OCR," "unsupported," or "malformed" result becomes the
     corresponding terminal outcome; only a successful extraction result
     continues into Unit 4B.

  This Unit also introduces the coordinator's own outcome type,
  including the three-variant integrity-verification outcome and every
  terminal variant steps 1–5 above can produce. This Unit also discloses,
  as a companion-object constant on `EvidenceExtractionCoordinator` —
  never a bare string literal at any call site — the fixed
  `evidence.extract` action-name convention Unit 5 later registers,
  mirroring `DefaultEvidenceCustodian.ACCEPT_ACTION_NAME`'s own existing
  precedent exactly (Scope Lock Section 8).

**Tests:**

- New file: `tests/runtime/EvidenceExtractionCoordinatorTest.kt` (begun
  here, extended in Unit 4B) — hand-written fakes for all five
  dependencies, mirroring `EvidenceRegistrationCoordinatorTest.kt`'s own
  established style. This Unit's own tests cover: source document not
  found; source identity mismatch, and that identity verification
  happens strictly before any retrieval occurs; retrieval rejected;
  retrieval not found; a verified match; a recorded mismatch (fails
  closed); an absent recorded hash (fails closed, distinctly from a
  mismatch); and each of "requires OCR"/"unsupported"/"malformed" from
  the extractor. No registration or review-state assertion is written in
  this Unit — nothing past step 5 exists yet to assert against.

---

### Unit 4B — Coordinator: Derivative Creation, Registration, and Review

**Production:**

- Same file, `src/runtime/EvidenceExtractionCoordinator.kt`. Completes
  the coordinator's own single operation, continuing from Unit 4A's
  successful extraction result:

  6. Build a new candidate evidence artefact from the extracted text,
     and a new candidate provenance record — content nature "extracted,"
     the "extracted from" reference set to the original's own document
     identifier only now that both identity verification (Unit 4A, step
     2) and integrity verification (Unit 4A, step 4) have already
     succeeded, and every reproducibility fact Boundary Clarification
     Section 4's table requires (the original evidence digest and the
     integrity-verification outcome it produced, the detected media
     type, the structured extraction-identity record, the extraction
     timestamp, warnings, the OCR-used flag, embedded-resource
     observations) recorded exactly as that table fixes, with no
     confidence value fabricated. The document type supplied for
     registration is fixed to "extracted-text."
  7. Call `EvidenceRegistrationCoordinator.register` with the above,
     unchanged, exactly as any other caller would call it — this Unit
     adds no new parameter, method, or outcome variant to that existing,
     frozen coordinator.
  8. On a successful registration, record an initial "pending review"
     state for the newly registered derivative through
     `DerivativeReviewRegistry`. On any other registration outcome, no
     review state is recorded at all — the coordinator's own outcome
     carries that non-successful registration result unchanged, so a
     caller is never told a review state exists that was never actually
     recorded (Boundary Clarification Section 9).
  9. Return the coordinator's own terminal "completed" outcome, carrying
     both the registration outcome and the extraction result.

  No exception handling of any kind appears anywhere in the
  coordinator's own operation, in either half — a genuine fault from any
  of the five dependencies propagates unchanged, mirroring
  `EvidenceRegistrationCoordinator.register`'s own identical discipline.

**Tests:**

- Same file, `tests/runtime/EvidenceExtractionCoordinatorTest.kt`,
  extended. Covers: each of `EvidenceRegistrationCoordinator`'s own three
  non-successful outcomes (no review state recorded for any of them);
  the full successful path (review state recorded exactly once, and
  exactly "pending review"); and that the derivative's own recorded
  provenance carries every reproducibility fact Unit 4A's extraction
  result and integrity-verification outcome produced, unaltered.

---

### Unit 5 — Production Composition and Runtime Entry Points

**Production — all changes confined to `src/composition/ParkerRuntime.kt`:**

- **Resource registration** (mirroring the existing "Evidence Custodian
  resource registration" `stage` block exactly): register one new
  `DOCUMENT`-typed Resource for the extraction capability itself, and one
  new `DOCUMENT`-typed Resource for the review-approval gate — the one
  Permission Engine expectation the Boundary Clarification (Section 6)
  disclosed but did not wire.
- **Action vocabulary registration**: two new `ActionVocabularyEntry`
  registrations, one for the extraction action, one for the
  review-approval action, both mapped to `(PermissionAction.WRITE,
  ResourceType.DOCUMENT)`. The extraction action's own constant is the
  one Unit 4A already disclosed on `EvidenceExtractionCoordinator`'s own
  companion object; the review-approval action has no earlier
  disclosure, since no prior unit's own operation corresponds to
  "approval" specifically — its constant is declared here, for the first
  time. Both, in either case, are companion-object constants exactly
  matching the existing `DefaultEvidenceCustodian.ACCEPT_ACTION_NAME`
  convention — never a bare string literal at the registration call
  site.
- **Permission policy rules**: the extraction action reuses the
  *existing* `WRITE`/`DOCUMENT` rule already present for evidence
  acceptance (no duplicate rule needed, since `DefaultPermissionPolicy`
  matches on `(action, resourceType)`, not on action name) — the same
  "minimum required, narrow in what it grants" discipline already
  governing every existing rule in this list. If the review-approval
  action also resolves to `WRITE`/`DOCUMENT`, no new
  `PermissionPolicyRule` is added at all, only the new Resource/action-vocabulary
  registrations above — this coarse-grained policy mechanism has no
  per-action matching capability (`DefaultPermissionPolicy`'s own
  existing, documented limitation, already noted for `DELETE`/`DOCUMENT`'s
  own owner-only guarantee), so no rule addition would change
  reachability either way.
- **Construction**, added to `buildAndRegisterRuntimeGraph`, after
  `evidenceRegistrationCoordinator` is constructed: one Tika extractor,
  one in-memory review registry, and one extraction coordinator wired to
  the already-constructed Evidence Custodian, Memory Core (passed as
  `MemoryRetrieval`, since `InMemoryMemoryCore` already implements both
  interfaces — confirmed directly against its own declaration, so no
  second Memory Core instance and no adapter are needed), the new Tika
  extractor, the existing Evidence Registration Coordinator, and the new
  review registry — mirroring the ordering and construction-only
  discipline every existing construction site in this file already
  follows.
- **Two new fields** held privately on `ParkerRuntime` — one for the
  extraction coordinator, one for the review registry — mirroring the
  existing `evidenceCustodian`/`evidenceRegistrationCoordinator` field
  pattern exactly.
- **Three new thin entry points**, mirroring `submitEvidence`/
  `retrieveEvidence`'s own exact shape (a `RUNNING` guard, one log line,
  one unchanged delegating call, nothing else):
  - An extraction entry point, taking a requesting principal, the
    original's document identifier, and its evidence-artefact
    identifier, delegating unchanged to the coordinator's own operation.
  - A review-decision entry point, taking a review record, delegating
    unchanged to the review registry's own recording operation. (No
    `PermissionEngine` check is added inside this method itself — Scope
    Lock Section 8 places the gate at the review-approval action/resource
    registration above, consistent with every other entry point on this
    class, none of which duplicates its own dependency's internal
    gating.)
  - An approved-retrieval entry point, taking a requesting principal and
    an evidence-artefact identifier — a thin wrapper that first reads
    the current review state for that identifier and returns an
    explicit rejection unless it is exactly "approved" (Boundary
    Clarification Section 6's own gating rule, enforced here for the
    first time since no consumer existed to enforce it against until
    now), then delegates to Evidence Custodian's own retrieval operation
    unchanged. This is the one entry point with genuine logic beyond
    "guard, log, delegate" — justified because the Boundary
    Clarification itself names this exact check as the reason the
    review registry exists at all.
- **No new `ParkerRuntimeConfig` field** (Scope Lock Section 4) — the
  Tika extractor and the in-memory review registry both take zero
  constructor parameters.

**Tests:**

- New file: `tests/composition/ParkerRuntimeEvidenceExtractionIntegrationTest.kt`,
  mirroring `ParkerRuntimeEvidenceCustodianIntegrationTest.kt`'s own
  existing shape — starts a real `ParkerRuntime` against the small
  synthetic PDF fixture (Unit 2), submits it, extracts it, confirms
  approved retrieval is rejected before approval and succeeds after.

---

### Unit 6 — Verification

No new production code. Runs and confirms:

- Every targeted test file from Units 1–5, individually.
- The full native Gradle suite (`./gradlew test`) — zero regressions in
  any existing test.
- `EvidenceExtractorScopeTest.kt`'s structural and source-scan Tika-leakage
  assertions (Units 1/2), re-confirmed green.
- `EvidenceExtractionCoordinatorTest.kt`'s fail-closed integrity
  assertions (Unit 4A) and registration/review assertions (Unit 4B),
  re-confirmed green.
- `InMemoryDerivativeReviewRegistryTest.kt`'s immutable-correction
  assertion (needs-correction rejected from transitioning back to
  pending review — Unit 3), re-confirmed green.
- `ParkerRuntimeEvidenceExtractionIntegrationTest.kt` (Unit 5),
  re-confirmed green.

Any failure at this stage halts the programme and returns to the unit
that owns the failing behaviour — Unit 6 fixes nothing itself.

---

### Unit 7 — Real-World Operational Proof

No new production code beyond what Unit 2's own test fixtures already
require. Steven performs this unit directly (or supervises it directly),
against a five-file corpus so every terminal outcome the pipeline can
produce is proven against real-world-shaped files, not only the
happy path, before Evidence Intelligence ever sees data:

1. One digitally-generated searchable PDF (a real Uber-style receipt or
   similar, born-digital, guaranteed genuine embedded text).
2. One different searchable PDF from another real-world source (e.g.,
   WelTec/ERA), proving the pipeline against a second, independently-produced
   document.
3. One scanned/image-only PDF, proving the "requires OCR" terminal
   outcome end-to-end through the real, started runtime, not only at the
   Tika-adapter level (Unit 2).
4. One malformed PDF, proving the "malformed" terminal outcome
   end-to-end.
5. One unsupported file (a non-PDF format), proving the "unsupported"
   terminal outcome end-to-end.

All five remain in the gitignored local fixtures directory (Unit 2) —
none is ever committed.

Steps:

1. Place all five files into the gitignored local fixtures directory.
2. Run `TikaEvidenceExtractorTest.kt` locally — all real files now
   exercised (no longer skipped).
3. Run the full pipeline against each file through a real, started
   `ParkerRuntime`. For the two searchable files (1, 2): submit, extract,
   approve, retrieve — confirmed successful end-to-end. For the
   remaining three (3, 4, 5): submit, extract — confirmed each produces
   its own correct terminal outcome and that no derivative, no
   registration, and no review record of any kind is created for any of
   them.
4. For the two successful files, confirm by direct inspection: custody,
   a verified integrity outcome, a genuinely populated extraction-identity
   record, a derivative digest that matches a recomputed SHA-256 of the
   extracted text, provenance whose "extracted from" reference correctly
   names the original document, review state (pending review, then
   approved), and approval gating (retrieval fails before approval,
   succeeds after).
5. For each of the five files, record — as an observation, not a
   pass/fail gate, establishing a baseline for when Parker later scales
   to thousands of documents — the file's own size, the extraction
   operation's own wall-clock duration, and the extracted character
   count where extraction succeeded.
6. Before any commit: `git status` confirms none of the five files, nor
   any byte derived from any of them, appears anywhere in the working
   tree outside the gitignored local fixtures directory.

---

## Programme Completion Criteria

This implementation programme is complete only when all of the following
conditions have been satisfied:

1. Every unit defined in Section 4 above — Units 1, 2, 3, 4A, 4B, 5, 6,
   and 7 — has completed successfully, and every verification step
   defined for each unit has passed.
2. The full native Gradle test suite (`./gradlew test`) passes with no
   regressions, exactly as Unit 6 requires.
3. The complete five-file operational proof defined in Unit 7 has
   succeeded exactly as specified there.
4. No real evidence file, nor any byte derived from real evidence,
   appears anywhere in the Git working tree outside the gitignored local
   fixtures directory (Unit 2).
5. Steven has completed formal verification, staging, commit, and push
   for every unit, in accordance with the project's governance workflow
   (Section 2, Rule 7, above).

Only when every condition above has been satisfied is Programme:
Evidence Processing (Searchable PDF) considered complete. At that point,
and not before, the paused Evidence Intelligence Contract Design becomes
eligible to resume as the next governed programme.

---

## 5. Dependencies

| Coordinate | Version | Scope | Verified against |
| --- | --- | --- | --- |
| `org.apache.tika:tika-core` | `3.3.1` | `implementation` | Maven Central, this planning pass |
| `org.apache.tika:tika-parser-pdf-module` | `3.3.1` | `implementation` | Maven Central, this planning pass |

No other new dependency. `kotlinx-coroutines-core:1.8.1`, `kotlin-test-junit5`,
`kotlin-reflect`, `kotlinx-coroutines-test:1.8.1`, and JUnit Jupiter
`5.10.2` remain the only other declared dependencies, unchanged. Kotlin
`1.9.24`, JVM toolchain `17` (confirmed installed: OpenJDK 17.0.19),
Gradle `8.10` — all unchanged, all already satisfied by the current
environment.

---

## 6. Out of Scope

Every item the Scope Lock's own Section 4 excludes, restated here as
binding on every unit above: OCR, Tesseract, image interpretation,
transcription, translation, summarisation, comparison, inference,
Knowledge Memory promotion, Evidence Intelligence reasoning, Tika Server,
network communication of any kind during extraction, a database, a UI
framework, a general workflow engine, a platform-wide audit framework,
mutation of original or derivative evidence, any `org.apache.tika` type
crossing a Parker-owned interface, and any real personal evidence
committed to Git.

---

## 7. Verification Strategy

Targeted tests per unit (Section 4, above), the full native Gradle suite
at Unit 6, and Unit 7's own real-evidence operational proof — three
layers, mirroring the Boundary Clarification's own three-layer
expectation (structural scope tests, behavioural coordinator tests,
end-to-end production-graph integration). No layer is skipped, and Unit 7
never substitutes for Units 1–6's own automated coverage — it proves the
pipeline works against real evidence across every terminal outcome, it
does not replace proving it works at all.

---

## 8. Traceability

| Unit | Boundary Clarification section(s) implemented |
| --- | --- |
| 1 — Extraction contracts | Section 3 (port/outcome shape), Section 4 (extraction identity), Section 8 |
| 2 — Tika adapter | Section 3 (searchable-vs-scanned detection, embedded resources, Tika-only-file rule), Determination 2 |
| 3 — Review registry | Section 6 (as corrected: terminal needs-correction) |
| 4A — Coordinator (identity, integrity, extraction) | Section 5 (identity verification), Section 7 steps 1–5 (as corrected: integrity-verification outcome) |
| 4B — Coordinator (derivative, registration, review) | Section 4 (reproducibility facts), Section 7 steps 6–9, Section 9 |
| 5 — Composition | Section 6 ("Permission Engine expectation, disclosed not decided"), Section 11 (Runtime Integration authorised) |
| 6 — Verification | Section 10 (failure-mode table), Section 12 (test inventory) |
| 7 — Real-world proof | Determination 5 (reproducibility), Section 9 (every terminal outcome), task's own stated end-to-end behaviour |

---

## 9. Risks

- **Tika transitive dependency weight.** `tika-parser-pdf-module` pulls in
  `pdfbox` and its own transitives. Mitigation: the scoped module,
  chosen specifically over the full `tika-parsers-standard-package`
  bundle, keeps this to the minimum required for PDF parsing alone —
  confirmed as the intended, narrower artefact during this planning
  pass.
- **Extraction-threshold constant (twenty characters) is a first-unit
  estimate, not a validated figure.** Mitigation: Unit 7's own real-file
  proof against five genuinely different real-world files is the first
  real validation; if any misclassifies, that is a Unit 2 defect to fix
  before Unit 6 is considered complete, not a constant to silently
  adjust after acceptance.
- **`InMemoryDerivativeReviewRegistry` is not durable.** Explicitly
  accepted (Boundary Clarification Section 6, "Durability, explicitly
  scoped down for this first unit") — a process restart loses in-flight
  review state, recoverable by re-review, not by data loss of the
  underlying derivative or its provenance.

---

## 10. Success Criteria

1. `./gradlew test` passes in full, including every new test file listed
   in Section 4.
2. `EvidenceExtractorScopeTest.kt` confirms zero `org.apache.tika`
   surface leakage, structurally and by source scan.
3. All five real-world corpus files (Unit 7) process successfully
   end-to-end, each producing its own correct terminal outcome, and none
   of them, nor any derived byte, ever appears in `git status`.
4. Every Scope Lock Verification Question (Section 10 there) answers as
   that document requires.
5. Steven completes formal verification, staging, commit, and push —
   this Implementation Plan authorises no unit to perform any of those
   four actions itself.

---

## Final Recommendation

This Implementation Plan is Accepted, Canonical, and Frozen. Independent
Review is complete. Final Freeze Verification is complete. It authorises
no implementation on its own — no implementation occurred during this
governance stage. Together with its companion Scope Lock it fixes the
exact files and responsibilities, and verified dependency coordinates,
for Parker's first operational Evidence Processing capability,
implementing the frozen Boundary Clarification exactly, without
reopening it, CDR-006, CDR-007, the Contract Design, or the paused
Evidence Intelligence Contract Design. Unit 1 — Extraction Contracts is
now authorised to begin, but only after Steven completes repository
verification, staging, commit, and push.

EVIDENCE PROCESSING (SEARCHABLE PDF) IMPLEMENTATION PLAN — ACCEPTED —
CANONICAL — FROZEN

Confirmed: no Kotlin implemented; no test written; no dependency added to
`build.gradle.kts`; the Boundary Clarification, the Scope Lock, CDR-006,
CDR-007, the Contract Design, the Evidence Custodian Scope Lock, the
Evidence Custodian Implementation Plan, Memory Core's own contracts,
`EvidenceRegistrationCoordinator`, and the paused Evidence Intelligence
Contract Design all unmodified by this document; nothing staged; nothing
committed; nothing pushed.
