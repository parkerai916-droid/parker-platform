# Document Ingestion — Owner-Authorized Local File Ingress Scope Lock

## Status

**Draft for owner review. Not yet accepted. Governance/architecture only —
no code implemented.** Resolves the governance blocker the attempted
Owner-Authorized Evidence File Ingress implementation unit's own Phase 4
hard gate discovered: no adopted governance authorizes Parker to read an
owner-designated local filesystem path
(`DOCUMENT_INGESTION_AUTHORITATIVE_SOURCE_MANIFEST_RETRIEVAL_SCOPE_LOCK.md`
§22, "External-ingress exclusion," confirmed this gap explicitly at its
own adoption, after its own fresh governance inspection: "No existing
adopted governance already solves it either... no ingress-authorization
document exists in the reviewed set"). This document defines the minimum
governed authority for exactly one narrow ingress mechanism — one
owner-designated local file, read once, on explicit owner request. It
does not implement it, does not touch Tier A/B/OCR, does not touch Memory
Core or Knowledge, and does not authorize any other ingress mechanism.

## Governing authorities

- `parker-constitution.md` — "Parker owns authority. Modules provide
  capability." / "No capability may bypass trust." / "Local-first by
  default." / "Privacy by design." / "Safety by default." / the seven
  Constitutional Tests.
- `epistemic-integrity.md` — Article IX (Integrity of Evidence): original
  evidence, once accepted into custody, is never modified; transformation
  during acquisition, if any, must be disclosed. This document freezes a
  *zero-transformation* acquisition path, which trivially satisfies
  Article IX's transformation-disclosure requirement by having nothing to
  disclose.
- `EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md` — §10 confirms it defines no
  ingress mechanism; this document does not conflict with, narrow, or
  extend it.
- `EVIDENCE_CUSTODIAN_SCOPE_LOCK.md` — §3/§4/§7/§9 govern what happens
  once bytes are in Evidence Custodian's custody; this document supplies
  no new custody-facing authority, only a new, narrow *pre-custody*
  acquisition act feeding the Custodian's own unchanged `accept` path.
- `EVIDENCE_CUSTODIAN_PHASE_7_BOUNDARY_CLARIFICATION.md` — §5/§6 establish
  the "narrow, purpose-built invocation-boundary component; Permission
  Engine evaluation first, before any I/O the evaluation gates" precedent
  this document reuses exactly.
- `DOCUMENT_INGESTION_AUTHORITATIVE_SOURCE_MANIFEST_RETRIEVAL_SCOPE_LOCK.md`
  — §9 ("an explicit caller-supplied value at `accept` time" is an
  already-authorized origin for `receivedMediaType`), §17 (the
  "same permission pair, distinct verb phrase, for a second, adjacent
  read" pattern this document reuses), §22 (the blocker this document
  resolves).
- `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` — "filename-as-metadata only,
  never a path" discipline, extended here to the local ingress path
  itself, not only the manifest's `originalFileName` field.
- `DOCUMENT_INGESTION_PROGRAMME_GOVERNANCE_CLOSURE.md` — §8/§11 confirm no
  existing deferral or unresolved matter addresses local-file ingress;
  nothing in the closure conflicts with this document.
- `DOCUMENT_INGESTION_TIER_A_IMPLEMENTATION_CLOSURE.md` — §15 confirms
  Tier A invocation remains a separately authorized act; this document
  does not touch it, and does not authorize invoking it automatically.
- `src/contracts/Permission.kt`, `src/contracts/Resource.kt` — current
  `PermissionAction`/`ResourceType` enums, freshly inspected (below).
- `src/runtime/DefaultEvidenceCustodian.kt`, `src/interfaces/EvidenceCustodian.kt`
  — current `accept`/`CandidateEvidenceArtifact` contract, freshly
  inspected (below).

## Scope

This document decides **one question only**: whether, and under what
exact authority, Parker may read the bytes of one local filesystem file,
explicitly designated by the owner during one explicit, interactive
invocation, for the sole purpose of constructing a
`CandidateEvidenceArtifact` and submitting it to the existing, unchanged
`EvidenceCustodian.accept` path.

It does not decide, and must not be read as deciding, anything about
upload services, URL fetching, Gmail/IMAP ingestion, cloud storage
integrations, directory import, network shares, watchers, schedulers, or
any other ingress mechanism (§19, below).

## 1. Problem statement

Before Evidence Custodian can accept bytes, some already-authorized actor
must possess those bytes. Today, every test and every governed path
constructs a `CandidateEvidenceArtifact` from bytes the test or caller
already holds — nothing in adopted governance authorizes *Parker itself*
to become the actor that reaches out to the local filesystem and acquires
bytes on the owner's behalf. This document closes exactly that gap, for
exactly the narrowest case: one owner-designated file, one explicit
invocation, read-only.

## 2. Authority decision — Owner-Explicit Single-File Local Read

**Authorized**, subject to every constraint in this document. The
authorized capability is fixed, in full, as:

- one owner-designated local path, supplied at the moment of one
  explicit, interactive invocation;
- read-only — no write, rename, move, delete, chmod, or timestamp touch
  of the source, ever;
- exactly one regular file — no directory, no device file, no socket, no
  FIFO;
- no directory traversal by Parker, no recursive discovery, no glob
  expansion, no search, no sibling-file enumeration;
- no background watching, no startup scan, no scheduler, no automatic
  retry;
- no network retrieval, no URL interpretation, no Gmail/IMAP, no upload
  service, no attachment promotion.

This is consistent with adopted governance: it introduces no new
`PermissionAction`/`ResourceType` (§3); it grants Parker no standing
filesystem authority (the authorization is scoped to one path, for one
invocation, never a directory or a search space); it creates no parallel
security system (§3 reuses the existing owner-only structural pattern and
the existing Permission Engine); and it leaves the Evidence Custodian's
own admission authority completely unchanged (§13). The Constitution's
"local-first by default" and "the owner remains in control" principles
affirmatively favor this capability over pushing the owner toward an
external upload path; the Constitution's "no capability may bypass trust"
and "safety by default" principles are what constrain its exact shape,
below.

## 3. Owner authorization

**Decision: A — existing `PermissionAction`/`ResourceType` vocabulary is
sufficient.** No new `PermissionAction` and no new `ResourceType` is
introduced.

Two authorization layers apply, mirroring the already-adopted
`deleteEvidenceAsOwner`/`invokeTierAIngestionAsOwner` precedent exactly:

1. **Structural owner-only invocation.** The future implementation-boundary
   component takes no caller-supplied `requestingPrincipalId` parameter.
   It always internally constructs `PrincipalId(config.ownerPrincipalId)`
   itself. There is no code path by which any caller — internal or
   external — can substitute a different principal. This is the identical
   pattern `DefaultOwnerEvidenceDeletionAuthority` and
   `TierAOwnerInvocationCoordinator` already establish, not a new one.
2. **A genuine Permission Engine evaluation, before any filesystem
   access.** Unlike the structural layer above (which fixes *who* the
   principal always is), this layer evaluates whether *that* principal's
   proposed act is actually authorized — mirroring
   `EVIDENCE_CUSTODIAN_PHASE_7_BOUNDARY_CLARIFICATION.md` §6's own fixed
   rule ("Permission Engine evaluation, first... Not approved → return
   Rejected. No storage access... of any kind") and
   `DefaultEvidenceCustodian.retrieveManifest`/`retrieve`/`accept`'s own
   existing, identical "evaluate, then act only if approved" shape.

**Resolution of the evaluation-ordering question (this unit's Phase 5):**
reading the local file is itself a new, distinct I/O act with real
authority implications, separate from the already-gated act of accepting
bytes into custody. Following
`DOCUMENT_INGESTION_AUTHORITATIVE_SOURCE_MANIFEST_RETRIEVAL_SCOPE_LOCK.md`
§17's own resolution to the structurally identical problem (a second,
adjacent read needing its own auditable identity without needing its own
new permission pair), this document authorizes exactly one new,
narrow action-vocabulary verb phrase — mirroring
`DefaultEvidenceCustodian.ACCEPT_ACTION_NAME`'s ("evidence.accept") own
existing shape and naming convention — mapped to the **identical existing
pair** `(PermissionAction.WRITE, ResourceType.DOCUMENT)`, evaluated
against the **identical existing** `EVIDENCE_INTAKE_RESOURCE_ID`
resource, since both acts concern the same conceptual boundary: the point
at which new content first proposes to enter Evidence Custodian's
custody. A distinct verb phrase (not a shared one) keeps "Parker read a
local file" auditable and traceable as its own act, separate from "the
Custodian admitted a candidate," exactly as manifest retrieval and source
retrieval remain two separately auditable reads sharing one pair.

The exact literal string value of the new verb phrase is implementation-plan
work (mirroring `ACCEPT_ACTION_NAME`'s own "not registered anywhere by
this Unit" precedent) — this document fixes only that it exists, is
distinct from `"evidence.accept"`, and maps to `(WRITE, DOCUMENT)` against
`EVIDENCE_INTAKE_RESOURCE_ID`.

**Ordering, frozen:** Permission Engine evaluation for this new verb
phrase occurs strictly *before* any filesystem access of any kind — before
`open`, before a `stat`/attribute check, before anything. Not approved →
immediate rejection; the path is never touched. Only once approved does
path validation (§4), the regular-file/symlink check (§5, §6), and the
bounded read (§7, §8) begin. `EvidenceCustodian.accept`'s own existing
Permission Engine evaluation then runs a second time, unchanged, exactly
as it does for every other caller today — this document does not weaken,
skip, or special-case that existing gate in any way.

**No parallel security scheme.** No hard-coded identity, no bespoke
authorization check, no new trust boundary — the two layers above are the
same two mechanisms every other owner-only Parker entry point already
uses.

**Independently verified against the real, wired `DefaultPermissionPolicy`
(`src/runtime/DefaultPermissionPolicy.kt`), not merely by precedent
analogy:** `PermissionPolicyRule.proposedAction` is an existing, real,
already-used field — an optional exact verb-phrase discriminator that
narrows a rule to one specific proposed action without touching any other
rule addressing the same `(PermissionAction, ResourceType)` pair (already
exercised in production for, among others,
`PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME` and
`DefaultReasoningKnowledgeSource.RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME`,
`src/composition/ParkerRuntime.kt`). "A new, distinct verb phrase mapped
to an existing `(PermissionAction, ResourceType)` pair" is therefore not
merely an already-adopted governance pattern (§17 of the Manifest
Retrieval Scope Lock) but an already-*implemented*, already-*exercised*
policy mechanism this document introduces no new capability to support.
`DefaultEvidenceCustodian.EVIDENCE_INTAKE_RESOURCE_ID` is itself already
registered in production (`ParkerRuntime.kt`, "Evidence Custodian
resource registration" stage) as a `ResourceType.DOCUMENT` resource, so
reusing it does not depend on any future registration this document
would need to authorize.

**Disclosed honestly, not overclaimed:** `DefaultPermissionPolicy` rules
are matched by `(PermissionAction, ResourceType)`, not by `ResourceId` —
the registered `ResourceId` is not itself part of rule-matching, only of
audit/tracing identity. The production policy table today contains one
coarse, unconditional `(WRITE, DOCUMENT)` rule (`proposedAction = null`,
`APPROVED`/`AUTOMATIC`) that already, today, is the rule
`"evidence.accept"` itself resolves through — no verb-phrase-specific
rule exists for `evidence.accept` today. Unless and until a future policy
author adds a rule scoped specifically to the new local-file-read verb
phrase (via `proposedAction`, exactly as Gap #54's own precedent already
does for other verbs), a request for the new verb phrase will resolve
through that identical coarse rule — the same rule, not a broader or a
narrower one, that already, today, governs `evidence.accept`. This is not
a new or weaker guarantee: `ParkerRuntime.kt`'s own composition comment
for that coarse rule already discloses that this flat policy mechanism
"has no per-principal matching capability at all," and that genuine
owner-scoping for `evidence.accept`/`evidence.delete` is, today, enforced
by structural call-site restriction alone (layer 1, above) — precisely
the model this document's layer 1 already relies on, not a lesser one.
Reusing `(WRITE, DOCUMENT)` therefore neither broadens any other caller's
authority (no existing rule is modified, and the new verb phrase's own
resolution is independent of every other verb phrase's) nor grants the
new verb phrase any authority `evidence.accept` does not already,
identically, possess today.

## 4. Path semantics

- **Form accepted: absolute paths only.** Relative paths are rejected,
  fail-closed, distinctly from a nonexistent path. Parker performs no
  interpretation of *which* path the owner meant beyond standard,
  deterministic absolute-path resolution — this is an ordinary local file
  selection, not a URL, not a search query, and not a pattern.
- **Why absolute-only (Phase 5 adversarial reassessment):** Parker runs
  as a persistent runtime process, not freshly invoked from the owner's
  own shell for each act (`scripts/run-owner-ui.sh` already confirms
  Parker is launched via a script/launcher, not run fresh from an
  interactive shell the owner's own `cd` history controls). A relative
  path resolved against *the process's own* current working directory —
  not the owner's — could resolve the identical literal input string to
  a different file, or to no file at all, depending on how or where
  Parker happens to have been launched that session: a fact the owner
  does not reliably observe or control. This directly undermines the
  premise this whole document rests on — that the owner is "explicitly
  designating exactly one file" — since an ambiguous, launch-context-dependent
  resolution is not the same as an explicit designation. No compelling
  user or architectural reason favors relative-path support for this
  first, narrowest local-ingress capability: the natural interactive
  surface for "the owner explicitly designates one file" is a native
  file-selection dialog, which already returns an absolute path; any
  other caller can trivially resolve a path to absolute on its own,
  locally-understood side before ever reaching this boundary. Absolute-only
  is therefore both the narrower and the more deterministic choice, per
  this unit's own governing preference, and is adopted without
  reservation.
- **Normalization:** the path may be normalized (`Path.normalize()` or
  equivalent) purely to resolve `.`/`..` components for the purpose of
  identifying *which single file* the owner meant — this is presentation
  normalization, not an authority grant. `..` components are permitted in
  the *input*, exactly as they are permitted in any ordinary file-open
  dialog or command-line argument; nothing about this document authorizes
  *directory traversal* as a capability (§6) — the owner is still
  designating exactly one file, once, by hand, not asking Parker to walk
  a filesystem tree.
- **Nonexistent path:** fails closed. No file is created, implied, or
  silently substituted.
- **Directory path:** fails closed, explicitly and distinctly from a
  nonexistent path.
- **Non-regular file (device file, socket, FIFO, pipe):** fails closed,
  explicitly and distinctly from both of the above (§6).
- **Path retention:** the designated path is **not** retained anywhere
  beyond the single invocation that used it (§18). It is an ephemeral
  ingress locator, never canonical evidence metadata, never written to
  the authoritative source manifest, never logged in a durable audit
  record beyond whatever transient, invocation-scoped diagnostic logging
  already exists for any other owner-facing entry point today.

## 5. Symlink decision

**Decision: reject symbolic links entirely.** The rejection scope is
frozen, explicitly, as the broader of the two possible readings: **no
component of the resolved path may traverse a symlink** — not only "the
final designated filesystem object must not itself be a symlink," but
every ancestor directory component the path passes through as well. An
implementation that checks only `Files.isSymbolicLink()` on the final
path component, while silently traversing one or more symlinked ancestor
directories to reach it, does **not** satisfy this rule — the correct
check inspects the fully resolved real path
(`Path.toRealPath(LinkOption.NOFOLLOW_LINKS)` or equivalent) against the
originally supplied path, or equivalently validates every path segment
individually, so that a symlinked *ancestor* is rejected exactly as a
symlinked *leaf* is. The read fails closed with a distinct rejection in
either case, never silently followed.

**Justification:** this is the first local-ingress capability Parker will
possess. No existing Parker governance provides a affirmative reason to
accept the additional attack surface a followed symlink introduces (a
symlink can be swapped between validation and read, can point outside any
directory the owner actually intended, and can point to a device file or
another sensitive local resource that a naive "is this a regular file"
check performed *after* following the link would not have caught before
the link was followed — a risk that applies identically whether the
symlink is the final component or an ancestor). Per this document's own
§2 deliberately narrow authority-surface framing and the Constitution's
"safety by default" principle, the narrower option is chosen without
needing a stronger justification for the broader one — permitting
resolution through a symlink, under any frozen semantics, at any path
position, is not authorized by this document and would require its own
future governance decision if ever proposed.

**Hard links:** not separately addressed by a rejection rule, and none is
required. A hard link is, at the filesystem level, indistinguishable from
"an ordinary file with more than one directory entry pointing at the same
data" — there is no portable, reliable way for a JVM process to detect
that a given regular file happens to have additional links, and no
security property of this document depends on detecting one. Because this
capability is strictly read-only and never mutates the source (§17), a
hard-linked file poses no different risk than any other regular file: it
is read exactly once, verbatim, and never written to. This document
explicitly declines to invent hard-link-specific behavior.

## 6. Object-type rule (single-file, non-discovery)

Parker may access **only** the exact file the owner designated. This
authority does **not** include:

- listing the parent directory;
- resolving candidate filenames (globbing, pattern matching, "did they
  mean this file or a similarly-named one");
- scanning sibling files;
- recursively descending into any subdirectory;
- probing alternate paths if the designated one fails.

Any filesystem metadata access strictly necessary to safely validate the
designated object (confirming it is a regular file, not a symlink, and
obtaining its size for the bound in §7) must be narrowly incidental to
opening and reading that one object — never a separate, independent
directory-listing or path-discovery act.

## 7. Source-size bound

**Decision: 64 MiB (67,108,864 bytes), fail-closed, checked before the
read completes, never silently truncated.**

Freshly inspected existing bounds, this unit:

| Component | Bound | Scope |
| --- | --- | --- |
| `TikaPdfStructuralExtractor.MAX_SOURCE_BYTES` | 32 MiB | PDF specialist source bytes |
| `ApachePoiXwpfExtractor.MAX_SOURCE_BYTES` | 32 MiB | DOCX specialist source bytes |
| `ApacheJamesMime4jExtractor.MAX_MESSAGE_BYTES` | 16 MiB | EML specialist source bytes |
| `FileSystemDerivativeGenerationStorage.MAX_RECORD_BYTES` | 32 MiB | prepared derivative record (output, not source) |
| `FileSystemEvidenceSourceManifestStorage.MAX_RECORD_BYTES` | 1 MiB | manifest record only |
| `EvidenceArtifactStorage`/`EvidenceCustodian.accept` | **none found** | no existing admission-time bound exists today |

No existing bound governs raw admission generically — the closest
candidates are all per-format Tier A *specialist* limits, and this
document explicitly declines to silently inherit any one of them, since
doing so would incorrectly redefine Evidence Custodian's own admission
authority in terms of one particular downstream parser's constraint
(Phase 9's own instruction). 32 MiB is the largest existing per-format
source-bytes bound (PDF, DOCX); this document selects **64 MiB** — double
that figure — as a conservative, explicit, universal *ingress-boundary*
bound: generously above every existing Tier A specialist's own bound (so
a legitimately Tier-A-admissible file is never rejected at the ingress
boundary before it can even reach a specialist and be evaluated on its
own governed terms), while still a hard, explicit ceiling that prevents a
single explicit owner action from loading an unbounded file into process
memory. A file may still be rejected downstream by a stricter per-format
specialist bound after acceptance — that is expected, unchanged, existing
behavior this document does not alter.

**Mechanics, frozen:** the file's reported size is checked via filesystem
attributes before the full read is attempted, avoiding loading an
oversized file into memory merely to reject it; the read itself must
additionally be bounded (abort if the stream exceeds the limit while
reading) as a defense against the size changing between the attribute
check and the read completing (§9). Exceeding the bound at either point
is a distinct, fail-closed rejection — never a silent truncation to the
first 64 MiB.

## 8. Byte fidelity

Ingress reads exact file bytes and nothing else:

- no decoding;
- no newline normalization;
- no charset conversion;
- no decompression;
- no parsing;
- no reserialization.

The bytes handed to `CandidateEvidenceArtifact.content` must be
byte-identical to the designated file's contents at the moment the read
completed successfully. This is a direct, minimum-scope application of
Epistemic Integrity Article IX's transformation-disclosure discipline: a
zero-transformation acquisition path has nothing to disclose, and this
document freezes it as zero-transformation precisely so that remains
true.

## 9. TOCTOU / file-identity limitation

Addressed honestly, not fictionally.

**Frozen discipline:** a single-open, best-effort discipline — the
designated path is opened once; the regular-file/non-symlink validation
(§5, §6) and the byte read (§8) are performed against that same open
file handle/channel where the underlying platform and Kotlin/JVM NIO APIs
make that practical, rather than via a separate `stat`-then-reopen
sequence, to minimize (not eliminate) the gap between validating what the
path names and reading what it names.

**Explicitly disclosed limitation:** the JVM's filesystem APIs do not
provide, and this document does not claim, a cross-process-atomic
guarantee that the object validated is provably the identical object
read, in the face of a concurrently adversarial local process capable of
substituting filesystem objects at the same path between validation and
read. This is a platform limitation, not a design defect this document
could close by trying harder — claiming a stronger guarantee than the
platform provides would itself violate the Constitution's "trust is
earned through architecture, not marketing" principle. The residual risk
this leaves is bounded and consistent with the rest of this document's
own authority model: even a successful substitution attack can, at most,
cause Parker to read *some* local file's bytes into an ephemeral,
never-persisted `CandidateEvidenceArtifact` that is then subject to
Evidence Custodian's own unchanged, independent admission authority — it
grants no elevated authority, no write access, and no ability to affect
any file Parker does not already read-access under the invoking process's
own ordinary OS permissions.

**Frozen, additionally:** if the source is detected to have been modified
during the read (for example, the stream yields fewer or more bytes than
the size observed at validation), the read fails closed rather than
silently proceeding with a partial or inconsistent result. This detects
only observable, length-changing inconsistency — it is not, and this
document does not claim it to be, a general concurrent-modification
detector: an adversarial same-length in-place content mutation during the
read is not reliably detectable by this or any other truthful mechanism
available to a JVM process reading an ordinary local file, and this
document requires no such impossible guarantee. The residual risk this
leaves is the same bounded risk already disclosed above.

## 10. Original filename

- optional metadata only;
- derived exclusively from the final filename component of the
  owner-designated path (the path's own basename) — never independently
  caller-overridable, and never sourced from anywhere else;
- never used as, treated as, or converted into a filesystem path by any
  downstream component, mirroring
  `DOCUMENT_INGESTION_AUTHORITATIVE_SOURCE_MANIFEST_RETRIEVAL_SCOPE_LOCK.md`
  §11's and `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`'s existing "filename
  as metadata only, never a path" discipline;
- carries no routing authority and no source-identity authority;
- Unicode preserved exactly as the filesystem returns it;
- a hostile-looking filename (path-traversal shapes, control characters,
  reserved device names, etc.) remains inert metadata — it is recorded
  and disclosed, never interpreted, exactly as
  `TierAOwnerInvocationCoordinatorTest`'s own existing hostile-filename
  test already proves for the adjacent Tier A boundary.
- the full local *path* is never persisted anywhere (§18) — only this
  optional basename may survive, exactly as
  `DOCUMENT_INGESTION_AUTHORITATIVE_SOURCE_MANIFEST_RETRIEVAL_SCOPE_LOCK.md`
  §6's "storage reference is not part of the manifest" exclusion already
  establishes for the analogous case.

## 11. Received media type

**Decision: A — owner declaration is authorized**, on the exact terms
already established by
`DOCUMENT_INGESTION_AUTHORITATIVE_SOURCE_MANIFEST_RETRIEVAL_SCOPE_LOCK.md`
§9, which this document does not reopen, narrow, or extend, only applies:

> "Origin: the original ingress channel's own declaration (for example, an
> email attachment's declared `Content-Type`, or **an explicit
> caller-supplied value at `accept` time**) — never Parker's own later
> inspection of the bytes."

Local file ingress is exactly one more instance of "an explicit
caller-supplied value at `accept` time" — the owner, at the moment of the
one explicit invocation this document authorizes, may optionally declare
a received media type for the file being imported. This is **not** a new
authority; §9 already authorized exactly this origin before this document
existed. It remains:

- a literal declaration by the owner, never a Parker inference;
- optional — absence remains truthful, not an error (§9's own rule,
  unchanged);
- immutable once accepted, per the manifest's own existing immutability
  discipline;
- syntactically validated only (non-blank if present, mirroring
  `CandidateEvidenceArtifact`'s own existing `init` check) — never
  verified for objective correctness;
- never inferred from the file extension, the filename, any parser, or
  any content-sniffing of the bytes, under any circumstance;
- subject to the router's own existing, unchanged received-vs-detected
  disagreement surfacing (`TierAMediaFacts.disagreement`) at whatever
  later, separate moment Tier A invocation actually occurs.

If the owner declines to declare a media type, `receivedMediaType`
remains absent, exactly as §9 already tolerates, and the imported
artifact may remain non-invocable by CSV-requiring Tier A routing until a
truthful media type exists by some other already-governed means. This
document does not weaken that outcome to make CSV more convenient, per
this unit's own Phase 13 instruction.

## 12. `CandidateEvidenceArtifact` construction boundary

Local ingress may supply, and only supply:

- exact bytes (§8);
- the owner/requesting-principal context already governed by the
  structural pattern (§3) — not a new field on the candidate itself,
  since `CandidateEvidenceArtifact` carries no principal field today and
  none is introduced;
- optional `originalFileName`, exactly as derived in §10;
- optional `receivedMediaType`, only as authorized in §11.

It must **not** supply, and no future implementation may add a path for
it to supply:

- `EvidenceArtifactId`;
- an expected SHA-256;
- a canonical byte-length authority distinct from what
  `CandidateEvidenceArtifact.content.size` itself already, structurally,
  is;
- an evidential-state classification;
- a parser result of any kind;
- a derivative identity;
- a Memory Core identifier;
- a Knowledge identifier;
- an OCR or Evidence Intelligence classification of any kind.

Every one of these facts remains exclusively an Evidence Custodian
admission fact (§13), or, where applicable, a fact belonging to an
entirely separate, not-yet-invoked subsystem (§14).

## 13. Evidence Custodian authority boundary

Local ingress has **no** admission authority of its own. It may only
construct a candidate (§12) and call the existing, unchanged
`EvidenceCustodian.accept` exactly once. Only Evidence Custodian may:

- accept or reject the candidate;
- mint the `EvidenceArtifactId`;
- persist the authoritative bytes;
- establish the authoritative digest;
- establish the authoritative byte length;
- establish the authoritative source manifest.

Local ingress may not write `EvidenceArtifactStorage`,
`EvidenceSourceManifestStorage`, or any other storage directly, under any
circumstance, at any point, for any reason — including failure recovery,
retry, or convenience. This mirrors
`DOCUMENT_INGESTION_TIER_A_IMPLEMENTATION_CLOSURE.md`'s own existing
"Evidence Custodian remains the sole admission authority" determination,
applied here to a new caller rather than a new authority.

## 14. No-automatic-ingestion rule

Successful `EvidenceCustodian.accept` acceptance **ends** this operation.
Local ingress must not call, directly or indirectly, on the same code
path, as a consequence of a successful acceptance:

- `invokeTierAIngestionAsOwner`;
- `TierAOwnerInvocationCoordinator`;
- `TierADocumentIngestionRouter`;
- any Tier A specialist parser;
- any OCR mechanism;
- any Tier B invocation;
- any Memory Core write;
- any Knowledge promotion;
- any Evidence Intelligence analysis.

A subsequent Tier A invocation, if the owner wants one, is necessarily a
**separate, later, independently authorized owner action** — exactly the
two-step shape
`DOCUMENT_INGESTION_TIER_A_IMPLEMENTATION_CLOSURE.md` §15 and the
Owner-Facing Tier A Runtime Invocation Boundary's own governance already
establish and this document does not reopen.

## 15. Failure semantics (conceptual, not a frozen Kotlin shape)

At minimum, a truthful future result model distinguishes:

- accepted evidence, carrying the resulting `EvidenceArtifactId`;
- owner-authorization rejected (the new verb phrase in §3 denied, before
  any filesystem access);
- invalid or nonexistent designated path;
- disallowed filesystem object (directory, symlink, device file, socket,
  FIFO);
- oversized source (§7);
- source read failure (an I/O fault distinct from the above — the object
  passed validation but the read itself failed, including the
  modified-during-read case in §9);
- Evidence Custodian rejection (the existing `accept`'s own Permission
  Engine evaluation denied, per §3's second layer);
- Evidence Custodian persistence failure, if and only if the existing
  `EvidenceCustodian`/storage contracts already expose such a fault
  today (this document neither invents nor forecloses that; it is an
  existing-contract question, not a new one).

No result may collapse two of the above into one undistinguished
`Boolean` or generic failure. This document does not fix the exact
Kotlin sealed-class shape — that remains implementation-plan work,
consistent with §20.

## 16. Repeated-import semantics

No deduplication policy is invented here. Repeated import of the same
local file, whether byte-identical or not, follows
`EvidenceCustodian.accept`'s own existing, unchanged behavior exactly —
today, that behavior mints a fresh `EvidenceArtifactId` for each
successful `accept` call regardless of content, per
`DefaultEvidenceCustodian`'s own current implementation
(`EvidenceArtifactId("evidence-${UUID.randomUUID()}")`, unconditionally,
on every approved acceptance). If that behavior ever changes, it changes
by amendment to Evidence Custodian's own governance, not by anything this
document fixes or could fix.

## 17. Source mutation prohibition

Local ingress is read-only, without exception:

- no write;
- no rename;
- no move;
- no delete;
- no chmod;
- no timestamp touch, where technically avoidable;
- no rewrite, temporary or otherwise.

This mirrors `EVIDENCE_CUSTODIAN_SCOPE_LOCK.md` §8's "modification is
prohibited" discipline, applied here one step earlier — to the *source*
file itself, before it has even become a custodied `EvidenceArtifact` —
rather than weakening it.

## 18. Source path retention / privacy

**Decision: No.** The owner-designated local path is invocation-local
only. It is used to open and read exactly one file, once, and then
disappears — it is never persisted in the authoritative source manifest,
never persisted in any other durable Parker record, and never returned
to any caller beyond whatever transient, invocation-scoped return value
or diagnostic log entry already exists for any other owner-facing entry
point today (mirroring, for example, `invokeTierAIngestionAsOwner`'s own
existing log line, which records only an `EvidenceArtifactId`, never a
filesystem path). Only the optional basename metadata described in §10
may survive, as manifest-level `originalFileName` disclosure — never the
directory structure, never the absolute path, never any component beyond
the final filename segment.

This is the minimum-data-necessary rule the Constitution's "privacy by
design" principle requires: the full local path reveals information about
the owner's filesystem layout (directory names, usernames, project
structure) that serves no purpose once the bytes are safely in custody
and identifiable by their own `EvidenceArtifactId`. No proposal to retain
more than the basename is made or justified here.

**Transient diagnostics, explicitly distinguished from durable storage:**
the rule above governs *durable, persisted* records only. A truthful,
immediate, invocation-scoped failure response (for example, "path
'/home/owner/report.pdf' does not exist," returned synchronously to the
same owner who supplied that exact path in that exact invocation) is not
a privacy violation and is not prohibited by this section — the owner
already knows the path; showing it back to them in the same interaction
discloses nothing they did not already possess. What this section
forbids is the path surviving *beyond* that single, immediate,
invocation-scoped interaction: it must never be written into a durable
log file, a durable audit record, an exception trace persisted to disk,
or any other record retained after the invocation completes and readable
in a later context. An in-process, non-durable diagnostic (a transient
log line at whatever level, never flushed to a durable, retained
destination beyond the process's own ordinary transient console/stdout
output already governed like any other Parker diagnostic output) may
include the path; a durable record — including any audit log this
programme's own governance elsewhere establishes — must not.

## 19. Explicit non-goals — external-ingress boundary after this unit

This document authorizes **exactly** the capability in §2, and nothing
else. Explicitly, still **unauthorized** after adoption of this document:

- directory import (importing every file in a folder);
- bulk import (importing a supplied list of many paths in one act);
- a drag-and-drop or browser-mediated upload service;
- Gmail ingestion;
- IMAP ingestion;
- URL fetching of any kind;
- cloud storage integration (Google Drive, Dropbox, S3, or equivalent);
- **network shares — resolved explicitly, not deferred:** a path the
  operating system already presents as an ordinary local filesystem path
  (for example, a directory under an NFS or SMB mount point) **is**
  covered by this document's existing §2 authorization, exactly as any
  other local path is — Parker has no portable, reliable way to
  distinguish "genuinely local storage" from "network-backed storage
  already mounted by the host" at the ordinary file-read API level (the
  identical reasoning already applied to hard links, §5), and this
  document does not ask a future implementation to attempt that
  distinction. What remains explicitly **unauthorized** is a materially
  different capability this document does not touch at all: Parker
  itself *initiating* network discovery, connection, mounting, browsing,
  or credential use to *reach* a share that is not already mounted and
  presented to the OS as an ordinary local path before the owner's
  invocation begins. The boundary this document actually draws is
  "whatever the OS already, independently presents as a local path" —
  never "whatever Parker could itself reach over a network if it tried";
- filesystem watchers of any kind;
- scheduled or automatic import of any kind;
- attachment auto-promotion (an EML attachment becoming its own
  `EvidenceArtifact` without a separate, explicit owner act).

"Local file ingress," as authorized here, is not a generic external-ingress
umbrella. Each item above, if ever proposed, requires its own future,
separately authorized governance decision — this document's adoption
does not lower the bar for any of them.

## 20. Implementation impact (not performed here)

Likely future implementation surfaces, none frozen by this document
beyond what is stated above:

| Surface | Classification |
| --- | --- |
| One owner-facing ingress result type (mirroring `TierAOwnerInvocationOutcome`'s own sealed-class shape) | Likely required — exact shape is implementation-plan work (§15) |
| One narrow runtime coordinator (mirroring `TierAOwnerInvocationCoordinator`'s "sequences two constitutional domains" pattern — here, the new local-read act and `EvidenceCustodian.accept`) | Likely required; exact class name/shape not frozen here |
| One `ParkerRuntime` entry point (mirroring `deleteEvidenceAsOwner`/`invokeTierAIngestionAsOwner`'s existing no-principal-parameter shape) | Likely required |
| A narrow filesystem-reader abstraction, if useful for testability (mirroring `FileSystemEvidenceArtifactStorage`'s own encapsulation discipline) | Possibly required; implementation-plan judgment |
| One new action-vocabulary verb phrase (§3), mapped to the existing `(WRITE, DOCUMENT)` pair against the existing `EVIDENCE_INTAKE_RESOURCE_ID` | Required — no new `PermissionAction`/`ResourceType`, no new `ResourceId` literal |
| A bound constant (§7) | Required — exact placement (a new file vs. an existing one) is implementation-plan work |
| Focused tests proving every rule in this document | Required, later, alongside implementation |
| Not required by any reading of this document | Any change to `EvidenceCustodian.accept`'s existing signature; any new `PermissionAction`/`ResourceType`; any Memory Core, Knowledge, Tier B, or OCR change; any Tier A router/specialist change; any change to the authoritative source manifest's own already-frozen shape |

## 21. Governance impact classification

**A/B — clarification and narrow authority extension consistent with
existing authority. Not C.**

Not a constitutional change because: (1) no new `PermissionAction` or
`ResourceType` is introduced (§3); (2) the new action-vocabulary verb
phrase reuses the identical, already-adopted `(WRITE, DOCUMENT)` pair and
`EVIDENCE_INTAKE_RESOURCE_ID` resource, mirroring an already-adopted
precedent (§17 of the Manifest Retrieval Scope Lock) rather than
inventing a new authority shape; (3) Evidence Custodian remains the sole
admission authority — no authority is transferred to a new subsystem
(§13); (4) the owner-only structural pattern reused (§3) is identical to
two already-adopted precedents, not a new mechanism; (5) the capability
authorized is deliberately, narrowly bounded (one file, one invocation,
read-only, no discovery) rather than a general filesystem-access grant,
satisfying every one of the Constitution's seven Constitutional Tests
without requiring any of them to be re-balanced against a broader
capability — the 28-item adversarial pass in §22, below, provides the
supporting detail for that claim.

## 22. Adversarial review

| # | Challenge | Resolution |
| --- | --- | --- |
| 1 | Arbitrary filesystem browsing | Foreclosed by §6 — no directory listing, no candidate resolution, no discovery of any kind; exactly one owner-designated path per invocation |
| 2 | Directory recursion | Foreclosed by §4 (directories fail closed) and §6 (no recursive descent) |
| 3 | Glob expansion | Foreclosed by §6 explicitly; the owner designates one literal path, never a pattern |
| 4 | Symlink escape | Foreclosed by §5 — symlinks rejected entirely, not resolved |
| 5 | Device-file reads | Foreclosed by §4/§6 — non-regular files fail closed, explicitly including device files, sockets, FIFOs |
| 6 | URL interpretation | Foreclosed by §4 and §19 — the designated value is a filesystem path only, never parsed as, or accepted as, a URL |
| 7 | Owner-authorization bypass | Foreclosed by §3's two-layer model — structural no-caller-principal shape plus a genuine Permission Engine evaluation strictly before any filesystem access |
| 8 | Persistent path leakage | Foreclosed by §18 — the full path is never persisted anywhere; only an optional basename may survive |
| 9 | Media-type fabrication | Foreclosed by §11 — owner declaration only, never inferred from extension/filename/parser/content, exactly mirroring the already-adopted §9 rule this document merely applies |
| 10 | Filename authority leakage | Foreclosed by §10 — metadata only, never a path, never routing authority, mirroring the already-adopted precedent for the analogous manifest field |
| 11 | Source mutation | Foreclosed by §17 — read-only, no exception |
| 12 | Direct `EvidenceArtifactStorage` writes | Foreclosed by §13 — Evidence Custodian remains the sole admission authority; local ingress may only call the existing `accept` |
| 13 | Direct manifest writes | Foreclosed by §13, identically |
| 14 | Automatic Tier A invocation | Foreclosed by §14 — successful acceptance ends the operation; a subsequent Tier A invocation is a separate, later, independently authorized act |
| 15 | OCR/Tier B expansion | Foreclosed by §14 — neither is called, directly or indirectly, as a consequence of successful acceptance |
| 16 | Memory/Knowledge/EI expansion | Foreclosed by §14, identically, and by §12's explicit exclusion of any such identifier from candidate construction |
| 17 | Unlimited file size | Foreclosed by §7 — an explicit, conservative, universal 64 MiB bound, fail-closed |
| 18 | Silent truncation | Foreclosed by §7 — exceeding the bound is a distinct rejection, never a silent truncation to the first N bytes |
| 19 | TOCTOU overclaim | Addressed honestly, not overclaimed, in §9 — a best-effort single-open discipline is frozen, with the platform's actual limitation explicitly disclosed rather than papered over |
| 20 | Duplicate-import policy invention | Foreclosed by §16 — no new deduplication policy; existing `EvidenceCustodian.accept` behavior governs unchanged |
| 21 | Relative-path launch-context ambiguity | Foreclosed by §4 — absolute paths only, adopted specifically because Parker runs as a persistent service process whose own cwd the owner does not reliably control; relative-path resolution is not authorized at all |
| 22 | Symlinked ancestor bypassing the symlink prohibition | Foreclosed by §5's explicit, broadened rule — "no component of the resolved path may traverse a symlink," with the final-component-only check explicitly identified as non-compliant |
| 23 | Owner-declared media type mistaken for Parker-detected truth | Foreclosed by §11 — a literal owner declaration only, never conflated with the router's own separately, mechanically computed detected media type; `TierAMediaFacts.disagreement` keeps the two visible and distinct downstream, unchanged |
| 24 | Source path leaking through durable error/audit data | Foreclosed by §18's explicit transient/durable distinction — an immediate, invocation-scoped diagnostic returned to the same owner who supplied the path is not a leak; a durable log, audit record, or persisted exception trace must never contain it |
| 25 | Network-mounted path unintentionally widening network authority | Resolved explicitly, not deferred, in §19 — an already-OS-mounted path is covered exactly like any other local path (Parker cannot and is not asked to distinguish network-backed from local storage at the file-read level), but Parker gains no authority to itself initiate network discovery, connection, or mounting; that boundary is untouched |
| 26 | Preauthorization filesystem metadata access leaking file existence | Foreclosed by §3's frozen ordering — Permission Engine evaluation occurs strictly before any filesystem access "of any kind... before a stat/attribute check, before anything"; a denied request never touches the filesystem, so no existence side-channel is created |
| 27 | The 64 MiB rule creating parser/storage authority confusion | Foreclosed by §7's own text — the bound is explicitly independent of, and does not redefine, any Tier A specialist's own bound; a file may still be independently rejected downstream by a stricter specialist limit, unchanged |
| 28 | TOCTOU language promising undeliverable detection | Foreclosed by §9's explicit disclaimer, strengthened to state directly that "detected modification" covers only observable, length-changing inconsistency, not general same-length concurrent-mutation detection, which is not claimed |

No item resolves to a blocker. All twenty-eight challenges resolve within
this document's own sections.

## Implementation authorization consequence

Once this scope lock is adopted, a later, separately authorized
Owner-Authorized Evidence File Ingress implementation unit may implement
exactly the chain this document fixes, and no more:

```
owner explicit invocation, designating one local path
  → owner-only structural authorization (§3, layer 1)
  → Permission Engine evaluation, new verb phrase, (WRITE, DOCUMENT) (§3, layer 2)
      → not approved: reject, no filesystem access of any kind
  → path validation: exists, regular file, not a symlink (§4, §5, §6)
      → invalid: reject, distinctly by failure kind
  → size check against the 64 MiB bound (§7)
      → oversized: reject, no truncation
  → single-open, bounded, byte-exact read (§8, §9)
      → read failure or mid-read modification: reject
  → CandidateEvidenceArtifact construction: bytes, optional basename (§10),
    optional owner-declared receivedMediaType (§11) — nothing else (§12)
  → EvidenceCustodian.accept(ownerPrincipal, candidate), exactly once,
    unchanged (§13)
  → truthful result returned (§15); operation ends (§14)
```

It still must **not** authorize any capability listed in §19, must not
persist the local path (§18), must not invent a deduplication policy
(§16), and must not call Tier A, OCR, Tier B, Memory Core, Knowledge
promotion, or Evidence Intelligence as a consequence of this operation
(§14).
