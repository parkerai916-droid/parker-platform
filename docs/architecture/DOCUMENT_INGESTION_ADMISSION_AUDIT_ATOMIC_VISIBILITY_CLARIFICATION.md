# Document Ingestion — Derivative Admission / Audit Atomic Visibility Boundary Clarification

## Status

**Draft for owner review. Not yet accepted. Targeted clarification, not a
reopening of the Document Ingestion governance programme.** Resolves one
specific blocker discovered during Implementation Unit 2, before CSV
extraction work resumes. Does not redesign Tier A, plan CSV parsing, or
touch any code. Governed in full by, and does not reopen:
`DOCUMENT_INGESTION_AUDIT_AUTHORITY_SCOPE_LOCK.md` ("Unit 5"),
`DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md` ("Unit 2"),
`SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`,
`DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md`,
`DOCUMENT_INGESTION_TIER_A_IMPLEMENTATION_PLAN.md`, and
`DOCUMENT_INGESTION_PROGRAMME_GOVERNANCE_CLOSURE.md` — all fresh-read for
this document, not recalled from summary.

## 1. Blocker statement

`DerivativeGenerationStorage.admit(record)` (`src/interfaces/DerivativeGeneration.kt`,
implemented by `FileSystemDerivativeGenerationStorage`) durably persists
a complete `DerivativeGenerationRecord` via a write-once, temp-file-then-atomic-move
sequence and makes it immediately, unconditionally retrievable —
confirmed by direct read of `FileSystemDerivativeGenerationStorage.admit`/`retrieve`.
`DocumentIngestionAudit.record(record)` (`src/interfaces/DocumentIngestionAudit.kt`,
implemented by `FileSystemDocumentIngestionAudit`) independently appends
one audit fact to a separate durable log. The two operations share no
transaction. Given: an audit fact cannot truthfully claim admission
occurred before it did; admission cannot safely be attempted first if
the audit write that must back it may subsequently fail; and deletion or
rollback of an admitted, immutable `DerivativeGenerationRecord` is not
authorised by any adopted governance — there is no way to sequence these
two independent operations using `admit`'s and `record`'s *current*
shapes that satisfies the adopted no-unaudited-admission invariant
(Section 2) without either lying in the audit log or leaving a
crash-window state in which an unaudited record is genuinely,
unconditionally observable via `retrieve`.

## 2. Authoritative existing invariant

Fresh-read, verbatim, from `DOCUMENT_INGESTION_AUDIT_AUTHORITY_SCOPE_LOCK.md`
§14 ("Immutability/append-only decision"), the section explicitly headed
"The load-bearing invariant, stated explicitly per the task's own
instruction":

> "Parker may never leave behind an admitted Derivative Generation
> Record — one satisfying Unit 2 §19's required-semantics test and
> therefore observable, referenceable, or reviewable by any consumer —
> for which no durable audit record of its own admission exists. This is
> not a new rule; it is Unit 2 §19's own atomicity rule ('admission is
> all-or-nothing at the governance boundary') read together with the
> already-adopted Routing Policy §5 fail-closed rule... made explicit for
> this unit exactly as the deletion precedent already makes it explicit
> for deletion... if durable audit persistence for the completion fact
> fails, the coordinator must not report, and no consumer may observe,
> that the Derivative Generation Record was successfully admitted —
> regardless of whether its content already exists in some underlying
> store."

Two clauses matter and are analysed separately, per the task's own
Step 3 instruction not to soften governance to fit current code:

- **"the coordinator must not report... successfully admitted"** — a
  constraint on the coordinator's own governed operational outcome to
  its caller.
- **"no consumer may observe... successfully admitted"** — a broader,
  additional constraint (joined by "and," not a restatement), reaching
  any consumer, not only the coordinator's immediate caller.

Also load-bearing: Unit 2 §19 ("no partial, half-populated Derivative
Generation Record is ever admitted... there is no partially-governed
intermediate state visible to any consumer") and Unit 2 §18
("Admission requires the coordinator's own act" — the storage/audit
primitives never decide admission themselves).

**Determination on the literal-versus-purpose question (Step 3):** the
phrase "the completion fact" in Unit 5 §14 was drafted under an
unexamined assumption — a single audit write occurring strictly *after*
publication (mirroring `FileSystemEvidenceDeletionAudit`'s own two-call
sequence at the surface level, without separately naming its own
two-*stage* structure). Read against its own stated purpose (the
sentence immediately preceding it: "this is not a new rule; it is
[Unit 2 §19] read together with [Routing Policy §5's] fail-closed rule")
and against the deletion precedent it explicitly invokes as its own
model, the invariant's actual purpose is narrower and more precise than
its literal wording: **no consumer may ever observe a Derivative
Generation Record as admitted unless a durable audit fact already exists
that specifically, unambiguously covers that exact identity's
admission.** The invariant does not, on its own terms, require that this
durable fact be recorded strictly *after* publication — only that it
exist, durably, by the moment of observability. Section 3 develops why
this distinction is not softening but is required to make the invariant
implementable without lying.

## 3. Sequencing analysis

| # | Sequence | Truthfulness | Crash-point risk | Observable admitted state | Verdict |
|---|---|---|---|---|---|
| 1 | Completion audit → publication | **False.** The audit record would assert admission occurred before the record exists anywhere | N/A — rejected outright | An audit trail could exist for a generation that was never actually published if the coordinator then fails before publishing | **Rejected.** Violates blocker constraint 3 directly |
| 2 | Publication → completion audit | Each individual record is truthful in isolation | Crash between publication and audit write leaves the record durably, unconditionally retrievable with **no** audit fact of any kind covering it, forever (no rollback authorised) | `retrieve()` unconditionally returns the record; any consumer holding a `DerivativeGenerationStorage` reference sees it as admitted | **Insufficient.** This is the sequence `admit`'s current unconditional-visibility shape actually produces today — it is the blocker itself, not a resolution |
| 3 | Pre-admission audit → publication → completion audit | Truthful, provided the pre-admission record does not claim admission occurred | Same crash-window gap as #2 between publication and the *completion* audit — the pre-admission audit alone does not, by itself, change what `retrieve()` unconditionally returns | Same as #2 unless combined with an observability change at the storage layer | **Necessary but not sufficient on its own** — closes the truthfulness question, not the observability question |
| 4 | Durable preparation → admission-authorising audit → atomic publication → outcome audit | Fully truthful at every step: preparation claims nothing; the authorising audit claims only that publication was authorised; publication is the actual, defining admission event; the outcome audit is confirmation, not the defining event | No crash point produces a state in which `retrieve()` returns a record unbacked by a durable, identity-specific audit fact (proved in Section 7, the crash matrix) | `retrieve()` returns a record only from the moment publication succeeds — and a durable authorising audit fact for that exact identity already exists by then | **Sufficient.** Analysed in full below |

Sequence 4 is the only one of the four that closes both the
truthfulness gap (constraint 3) and the observability gap (Unit 5 §14's
"no consumer may observe" clause) without requiring either a lie or an
unauthorised deletion/rollback.

## 4. Prepared-state analysis

**PREPARED DERIVATIVE GENERATION**, as specified in the task, is
consistent with existing source/derivative and authority governance,
for four independent reasons, each checked directly against adopted
text:

1. **It creates no new evidential authority.** A prepared artifact is
   neither an `EvidenceArtifact` (Evidence Custodian's own exclusive
   domain, CDR-006, untouched) nor a governed `DerivativeGenerationRecord`
   (Unit 2 §2's own "never becomes an `EvidenceArtifact`" boundary,
   likewise untouched) — it carries no identity any governed consumer
   can reference, cite, review, or rely on, because no interface exposes
   it.
2. **It does not create a "partially-governed intermediate state visible
   to any consumer,"** the exact state Unit 2 §19 forbids — the
   prepared state is defined precisely by *not* being visible through
   `DerivativeGenerationStorage.retrieve()` or any other governed
   surface. Unit 2 §19 forbids a *visible* half-governed state; an
   *invisible* fully-written-but-unpublished one is a different thing
   its own text does not reach.
3. **It is not a new architectural concept — it makes an already-existing
   step durable and addressable.** `FileSystemDerivativeGenerationStorage.admit`,
   read fresh this unit, already performs write-to-temp-file →
   `FileChannel.force` → atomic move; the interval between the forced
   write and the atomic move is already, today, a "durably written but
   not yet at its final, retrievable identity" state — it is simply
   anonymous, internal, and discarded (via `finally { Files.deleteIfExists(temporary) }`)
   rather than named and addressable. This clarification proposes
   exposing that already-real interval as a first-class, resumable state
   with its own stable identity, not inventing a new kind of state the
   storage layer does not already pass through on every admission.
4. **Admission remains exclusively the coordinator's own act.** Unit 2
   §18: "Admission requires the coordinator's own act, gated by whatever
   authorization/permission check governs ingestion generally." A
   `prepare`/`publishPrepared` split does not change who is authorised to
   admit — the storage layer still never decides, on its own, that a
   prepared record becomes admitted; only an explicit coordinator call
   to `publishPrepared` does that (Section 8).

## 5. Audit semantics

**Yes** — the ingestion audit may truthfully record, before publication,
a durable fact equivalent to "the coordinator authorised publication of
prepared generation X," provided it does not additionally claim "X has
been admitted." This is not a new pattern requiring new justification:
it is the exact, already-adopted, already-implemented shape of
`EvidenceDeletionAuditStage.AUTHORISED` (`src/interfaces/EvidenceDeletionAudit.kt`,
fresh-read this unit) — "a record with `stage = AUTHORISED` must be
durably confirmed written *before* [the irreversible act] is ever
called... closes the crash-window failure mode... a successful [act]
must never exist without durable evidence, unconditionally, that it was
authorised." Unit 5 §14 itself invokes this exact deletion precedent as
its own interpretive model. Applying the identical two-stage shape here
is therefore the most direct, least invasive resolution available, not
an invention.

**Determination:** such a durable authorisation/commit-intent record
does serve as the required precondition for atomic publication —
provided (a) it names the exact `DerivativeGenerationId` being
authorised (not a vague "an admission is pending" fact), and (b) the
coordinator never calls `publishPrepared` unless this record's write has
already, durably succeeded (Section 6, Section 8).

**Currently disclosed shape gap:** `DocumentIngestionAuditRecord`
(`src/interfaces/DocumentIngestionAudit.kt`, fresh-read this unit)
presently carries `operationalOutcome: String` — a free string — and no
`stage`-typed field analogous to `EvidenceDeletionAuditStage`. It does
already carry an optional `derivativeGenerationId: DerivativeGenerationId?`,
which is sufficient to name the exact identity a stage-typed field would
need. Section 11 states the minimum change this implies for the
Tier A Implementation Plan.

**Exact, closed audit-stage vocabulary.** Mirroring
`EvidenceDeletionAuditStage`'s own precedent exactly, not merely in
shape but in cardinality: a new closed, two-value stage enum —
**`ADMISSION_AUTHORISED`** (written durably before publication; records
only that the coordinator authorised publication of the named,
already-prepared `DerivativeGenerationId` — never that admission
occurred) and **`ADMITTED`** (written durably after publication
succeeds; records that the named `DerivativeGenerationId` was, in fact,
published/admitted). **No third value is introduced.** This is not an
arbitrary minimalism choice: `EvidenceDeletionAuditStage`'s own KDoc
states its two-value shape explicitly and by design — "no `FAILED`,
`REQUESTED`, `NOT_FOUND`, or any other value is introduced (Boundary
Clarification Section 4's own explicit instruction)" — and a failed
publication attempt is handled identically to a failed deletion attempt
under that same precedent: it simply never receives an `ADMITTED`
record, and the standing `ADMISSION_AUTHORISED` record remains true on
its own terms (it never claimed admission occurred). A future
implementation unit must not invent a third stage value (e.g., a
"PUBLICATION_FAILED" stage) without a fresh, separate governance
decision — this document forecloses that specific invention as
unnecessary, mirroring the precedent's own explicit foreclosure.

## 6. Atomic-publication rule

Publication (`publishPrepared`) is the sole, defining moment at which a
Derivative Generation Record becomes admitted:

- It is atomic — either the prepared bytes become retrievable at their
  final identity, in full, or nothing changes (mirrors the existing
  `admit`'s own atomic-move discipline exactly; no new atomicity
  mechanism is invented).
- It never overwrites — attempting to publish an identity that is
  already admitted is a `DuplicateIdentifier`-shaped outcome, not a
  silent no-op and not a second write (mirrors existing `admit`
  behaviour).
- It is called only after the admission-authorising audit fact for that
  exact identity has already, durably succeeded (Section 8) — this is a
  coordinator-owned sequencing rule, not something the storage
  interface can structurally enforce on its own (Unit 2 §18).
- It does not depend on, wait for, or require the *outcome* audit to
  exist first — the outcome audit is written after, and its
  determination (Section 7) is that its absence during a crash window
  does not create an unaudited-observable state, because the
  authorising audit already durably covers this exact identity by the
  time publication makes it observable.

## 7. Crash/failure matrix

| Point | Prepared present? | Admitted present? | Authorisation audit present? | Outcome audit present? | May Parker report success? | Recovery/reconciliation |
|---|---|---|---|---|---|---|
| Before preparation | No | No | No | No | No | None — retry attempt from scratch |
| During preparation (temp write, pre-force/move) | No (anonymous internal temp only, discarded on cleanup) | No | No | No | No | Orphaned temp file cleanup only — no governance claim was ever made about it |
| After durable preparation | Yes | No | No | No | No | Abandon (safe — no interface exposes it) or, optionally, resume by attempting the authorisation-audit step |
| During authorisation-audit write | Yes | No | Absent or present (single-line append; treated as absent unless durably confirmed) | No | No | Same as "after durable preparation" if absent; proceed to publication if confirmed present |
| After authorisation audit | Yes | No | Yes | No | No (not yet published) | Retry `publishPrepared(X)` — safe, since prepare + authorisation already durably exist |
| During atomic publication | Collapses to the state either side of it — the underlying move is itself atomic (Section 6); no new intermediate observable state exists | Collapses to the state either side of it | Yes | No | No, until resolved to one side | None beyond the atomic-move guarantee already relied on by `admit` today |
| Immediately after publication | No (consumed/renamed into the admitted identity) | **Yes** | Yes | No | **Yes** — the durable authorisation audit already, specifically covers this identity (Section 2's determination) | Write the missing outcome audit on next reconciliation, referencing the same identity — no physical act needs to be redone |
| During outcome-audit write | No | Yes | Yes | Absent or present (same append-atomicity caveat as authorisation audit) | Yes (unaffected by this write's own crash risk, per the determination above) | Same as "immediately after publication" if absent |
| After outcome audit | No | Yes | Yes | Yes | Yes | None — attempt fully, durably confirmed |

**Proof (Step 9's own requirement):** at every row, `retrieve()` returns
a record ("Admitted present? = Yes") only from the "Immediately after
publication" row onward — and at that exact row and every row after it,
a durable, identity-specific authorisation-audit fact already exists.
No row exists in which "Admitted present? = Yes" and "Authorisation
audit present? = No" simultaneously. No forbidden state — an
unconditionally observable, admitted, unaudited Derivative Generation
Record — is reachable from any crash point.

**The outcome audit's non-load-bearing status for the core invariant
does not make it optional or discardable.** It remains a required
operational obligation for every admitted generation — the coordinator
must still write it, and a reconciliation process must still complete it
after any crash that leaves it absent (Section 7's "Recovery/reconciliation"
column, every affected row). Its determined non-load-bearing status
(Section 2, this section) answers only the narrower question of whether
the core no-unaudited-admission invariant is violated by its *temporary*
absence — it is not licence to treat the outcome audit as permanently
skippable, best-effort, or unnecessary.

## 8. Authority boundaries

- **The coordinator alone owns sequencing.** Neither `DerivativeGenerationStorage`
  nor `DocumentIngestionAudit` may decide, on its own, to advance from
  one state to the next — `prepare`, the authorisation-audit write,
  `publishPrepared`, and the outcome-audit write are four independent
  calls the coordinator alone sequences, mirroring `EvidenceExtractionCoordinator`'s
  and `EvidenceRegistrationCoordinator`'s own existing "no dependency
  holds a reference to any other; coordination lives in the runtime
  layer" discipline (both fresh-read this unit).
- **Storage never gains evidential or promotion authority.** `prepare`/`publishPrepared`
  are storage-shape changes only — they grant the storage
  implementation no new decision-making authority; `publishPrepared`
  performs exactly the same atomic-move operation `admit` already
  performs today, only now separated from the write step it currently
  performs in the same call.
- **No Memory Core or Knowledge authority is touched.** Nothing in this
  clarification creates, widens, or references any Memory Core write
  path (Unit 6 §7, §19, unaffected).
- **No new deletion authority.** Per the task's own explicit instruction,
  and consistent with `OwnerEvidenceDeletionAuthority`'s own structurally
  isolated, narrow shape, this clarification introduces no delete
  operation of any kind for prepared or admitted state (Section 9).

## 9. Abandoned-prepared-state treatment

Cleanup or retention of abandoned prepared artifacts (those never
published — Section 7's "after durable preparation," "after
authorisation audit," or an authorisation-audit failure) **remains
explicitly deferred**, exactly as Unit 2 §17 and Unit 5 §17 already
defer retention/deletion questions generally. This is not a new
deferral this document invents — it is the same already-open deferral
extended to one more artifact kind. No deletion authority is introduced
to address it (Section 8). An abandoned prepared artifact carries no
observable authority of any kind (Section 4, point 1) — it is inert
until and unless a future, separately governed unit decides what, if
anything, should reclaim it.

## 10. Implementation-plan impact

Minimum change to the accepted `DOCUMENT_INGESTION_TIER_A_IMPLEMENTATION_PLAN.md`
required — not a rewrite:

- **Section 6 (Minimum implementation surface):** the `DerivativeGenerationStorage`
  row's proposed shape is refined from a single `admit(record)` operation
  to two operations — a durable, non-observable `prepare` and an atomic,
  observable `publishPrepared` — consistent with, not additive to, the
  row's existing classification (**NEW**, already justified there).
- **Section 6:** the ingestion audit port row's proposed shape gains a
  closed, two-value stage distinction — `ADMISSION_AUTHORISED` and
  `ADMITTED` (Section 5) — mirroring `EvidenceDeletionAuditStage`'s
  exact cardinality and no-`FAILED`-value discipline, consistent with,
  not additive to, that row's existing classification.
- **Section 9 (Failure-atomicity plan):** the row "Admission succeeds
  but post-admission audit confirmation fails" — previously left as
  "coding-unit work, not fixed here" for its specific one-write-versus-two-write
  shape — is now resolved by this clarification: two writes
  (authorisation before publication, outcome after), with the
  determination that outcome-audit absence during a crash window does
  not violate the no-unaudited-admission invariant (Section 2, Section 7).
- **Section 12 (Implementation sequence) / Section 15 (First
  implementation unit):** the already-committed code at `6139902` (`DerivativeGenerationStorage.admit`,
  `DocumentIngestionAudit.record`) implements sequence #2 (Section 3) —
  the insufficient one. Before CSV extraction (Implementation Unit 2)
  proceeds, a corrective sub-unit must revise these two already-coded
  interfaces and their filesystem implementations to the `prepare`/`publishPrepared`
  and staged-audit shapes this document describes. This is coding work
  for a future unit; **no code is touched by this document.**

No other section of the Tier A Implementation Plan requires any change
— the four selected mechanisms, the four format-specific plans, the
persistence/derivative-content split, the reprocessing rules, the
evidence/acceptance plan, and the implementation-sequence ordering are
all unaffected.

## 11. Exact decision

**Governance-change classification: B — a narrow clarification/addendum
to existing governance.**

Not A (no amendment at all), because Unit 5 §14's literal text names
"the completion fact" without disclosing the authorisation/completion
split this document relies on — an implementer reading Unit 5 §14 alone,
without this clarification, would reasonably conclude sequence #2
(Section 3) is what governance requires, when it is in fact the
insufficient sequence. A clarifying document is needed to make the
already-intended purpose (no observable admission without durable,
identity-specific audit backing) explicit and implementable.

Not C (substantive amendment), because no authority is created,
enlarged, transferred, or resolved that was not already frozen: the
invariant itself — no consumer may observe an unaudited admission — is
restated unchanged, not weakened, and not widened. What changes is only
*which* durable audit fact (authorisation, preceding publication) is
established as satisfying that invariant, in place of an ambiguity in
Unit 5 §14's own prior wording about *when* "the completion fact" must
exist relative to publication. This is the same kind of clarification
Unit 5 §15 already anticipated by its own design ("no rollback
technology chosen... several are possible" — leaving the specific
mechanism to implementation-plan work while the invariant itself stayed
fixed).

## 12. Implementation readiness

This clarification, once adopted, removes the blocker. Implementation
Unit 2 **may then resume**, in this order:

1. A corrective sub-unit revises `DerivativeGenerationStorage` (`prepare`/`publishPrepared`
   replacing `admit`) and `DocumentIngestionAudit`/`DocumentIngestionAuditRecord`
   (an authorisation/outcome stage distinction, mirroring `EvidenceDeletionAuditStage`)
   and their filesystem implementations, per Section 10.
2. Only after that sub-unit is complete and tested does CSV extraction
   (the originally planned Implementation Unit 2 content, per the Tier A
   Implementation Plan's own Section 12 sequencing) resume, using the
   coordinator sequencing this document fixes (Section 6, Section 8).

No further Document Ingestion governance question was found blocking
this resumption. No OCR, Memory Core, or Tier B question is touched.

**READY FOR OWNER ACCEPTANCE.**
