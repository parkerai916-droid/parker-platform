# Evidence Artifact Storage — Identifier to Storage Mapping

## Status

Design note. Evidence Custodian, Implementation Plan Phase 2 ("Immutable
storage behaviour"), Unit 1. Describes the identifier-to-storage-location
mapping implemented by `EvidenceArtifactIdentifierSafety` and
`FileSystemEvidenceArtifactStorage` (`src/interfaces/EvidenceArtifactStorage.kt`,
`src/runtime/FileSystemEvidenceArtifactStorage.kt`). This document records
design rationale only — it does not introduce, authorise, or imply any
capability beyond what those two files already implement. It is not a
Scope Lock, Contract Design, or Implementation Plan, and does not amend
any of the four documents that already govern the Evidence Custodian
(`docs/decisions/CDR-006_CONSTITUTIONAL_CLASSIFICATION_OF_ORIGINAL_EVIDENCE_CUSTODY_AND_IMMUTABILITY.md`,
`docs/architecture/EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`,
`docs/architecture/EVIDENCE_CUSTODIAN_SCOPE_LOCK.md`,
`docs/architecture/EVIDENCE_CUSTODIAN_IMPLEMENTATION_PLAN.md`).

---

## 1. Filename format

Each stored artefact occupies exactly one file, named:

```
<identifier-value>.evidence
```

where `<identifier-value>` is the canonical (lowercase-only) string value
of the `EvidenceArtifactId` supplied to `write`. For example, an
identifier with value `evidence-4821` is stored as `evidence-4821.evidence`
directly under the configured storage root.

## 2. Identifier-to-file mapping

The mapping is direct and one-to-one: `storageRoot.resolve(identifier.value + ".evidence")`.
No hashing, no encoding, no sharding, and no indirection of any kind sits
between an identifier and its file. This was a deliberate choice, not an
omission — see Section 7 (hashing) and Section 6 (sharding) below for why
each of those was considered and left out of this Unit. A direct mapping
means the relationship between "an identifier the rest of the system
holds" and "a file on disk" can be inspected by a human reading a
directory listing, without running any code — a concrete, checkable
instance of the Constitution's own evidentiary standard that a safeguard
should be something the architecture can point to, not something that
must be trusted to exist inside opaque machinery.

## 3. Canonical lowercase policy

A storage identifier must match `^[a-z0-9_-]+$` — lowercase ASCII
letters, digits, hyphen, and underscore only. An identifier containing
any uppercase character is rejected outright, via
`EvidenceArtifactStorageException.UnsafeIdentifier`, and is **never**
silently lowercased or otherwise normalised.

This is stricter than it first appears necessary to be, and that
strictness is deliberate. `EvidenceArtifactId` equality is case-sensitive
(ordinary Kotlin `String` equality), but the two most common production
filesystems this project targets are not: default Windows NTFS and
default macOS APFS both treat `Evidence-1` and `evidence-1` as the same
file. Had this Unit chosen to silently fold identifiers to lowercase
before deriving a path, two distinct `EvidenceArtifactId` values could
collide on disk without either caller ever being told — one write would
silently appear to succeed while actually landing on top of, or being
rejected in confusing conflict with, a completely different identifier's
content. Rejecting outright, rather than normalising, closes that gap by
construction: a caller who supplies a non-canonical identifier learns
about the mismatch immediately, at the moment of the mistake, rather than
discovering it later as an unexplained collision or an unexplained
"missing" artefact.

The canonical-form requirement lives entirely in the storage boundary
(`EvidenceArtifactIdentifierSafety`), not in `EvidenceArtifactId` itself.
`EvidenceArtifactId`'s own constructor continues to accept any non-blank
string, exactly as already frozen by the Evidence Custodian's earlier
Implementation Plan Unit. See Section 8 for why that separation was kept
rather than tightening `EvidenceArtifactId` directly.

## 4. Windows reserved-name protection

A canonical (already-lowercase) identifier is additionally rejected if it
is one of Windows's reserved device names: `con`, `prn`, `aux`, `nul`, or
`com1`–`com9` / `lpt1`–`lpt9` (exactly one trailing digit `1`–`9`; `com10`
and `lpt10` are ordinary, unreserved identifiers). These names are
special to the Windows filesystem regardless of any extension appended to
them — `con.evidence` remains a reference to the reserved device, not an
ordinary file — so appending `.evidence` does not, by itself, make an
otherwise-reserved identifier safe.

Because the canonical-lowercase policy (Section 3) already guarantees
every identifier reaching this check is lowercase, the reserved-name
comparison itself only needs to test against the lowercase spelling of
each reserved name. The rule is documented, and should be understood, as
case-insensitive protection achieved by composition of the two rules
together — not as an accidental side effect of comparing lowercase
strings. No case variant of a reserved name (`CON`, `Con`, `con`) can
reach the filesystem: uppercase or mixed-case variants are already
rejected by Section 3 before this check ever runs, and the lowercase
variant is rejected here.

Ordinary identifiers that merely resemble a reserved name are
deliberately left valid: `console`, `auxiliary`, `com10`, `lpt10`, and
`my_con_file` are all accepted. The check is an exact match against the
reserved set (or the reserved-prefix-plus-single-digit pattern for
`com`/`lpt`), never a substring or prefix match — a substring match would
have rejected far more identifiers than Windows itself actually reserves.

## 5. `.evidence` extension rationale

Every stored file carries a fixed, content-agnostic `.evidence`
extension, regardless of what kind of artefact it holds. This was chosen
over a content-derived extension (`.pdf`, `.eml`, `.png`, and so on) for
one direct reason: this Unit has no concept of content type at all — no
sniffing, no classification, no `artifactType` field of any kind survives
in the current, corrected shape of this Unit's own types (an earlier,
since-removed draft did carry such a field; see
`src/interfaces/EvidenceCustodian.kt`'s own "Correction history" note).
Choosing a content-derived extension would have required inferring or
accepting a content-type claim this Unit deliberately does not make.
`.evidence` says only "this file is under this storage primitive's
management," nothing about what it contains.

## 6. Flat directory layout

Every artefact's file sits directly in the storage root; there is no
subdirectory structure based on the identifier itself. The one exception
is a single reserved `.tmp` subdirectory, used exclusively for in-flight
writes before their atomic move to the final path (a file is written and
`fsync`'d there first, then moved into place — never written directly to
its final location). `.tmp` is excluded from being treated as a stored
artefact by every operation this Unit implements.

## 7. Future sharding considerations

A flat directory holding many thousands of entries will eventually become
slow to list or traverse on some filesystems — a known, general property
of flat directories, not specific to this Unit. This document records the
consideration without acting on it: nothing in this Unit's current scope
requires solving a scale problem it does not yet have. If it becomes
necessary later, the natural fix is a sharded layout (for example, the
first two characters of an identifier's value used as an intermediate
subdirectory) implemented behind the same `EvidenceArtifactStorage`
interface this Unit already defines — no caller of that interface would
need to change, since nothing outside `FileSystemEvidenceArtifactStorage`
depends on today's flat layout. Directory sharding was explicitly named
as excluded from this Unit's own corrections and is not implemented here.

## 8. Why hashing is not introduced in Unit 1

No hash, checksum, or other integrity verifier is computed or stored
anywhere in this Unit. This is a considered omission, not an oversight:
the Evidence Artifact Contract Design (Section 4, "Preserving provenance
relationships") only ever describes an integrity verifier as something
the Custodian may supply "where it can" — permissive language, not a
requirement — and Memory Core's own `Document.integrityHash` field is
already optional and nullable, confirming no existing governance document
strictly requires a hash at this layer. Introducing one here would have
been scope this Unit was never authorised to claim, exactly the kind of
addition the Evidence Custodian programme's own Implementation Governance
Rules exist to prevent implementation convenience from justifying on its
own. If a future Unit's own governance review concludes a stored
integrity verifier is needed, it remains available as an optional
addition at the Memory Core registration layer without requiring any
change to this storage primitive's own shape.

## 9. Why storage validation is stricter than `EvidenceArtifactId`

`EvidenceArtifactId` accepts any non-blank string — a deliberately
minimal, already-frozen contract with no opinion about filesystem safety,
because at the point that type was defined, no storage mechanism had been
selected yet and none was authorised to be assumed. `EvidenceArtifactStorage`'s
own validation (Sections 3–4 above) is necessarily stricter, because it is
the one boundary in this Unit that actually turns an identifier into a
filesystem path — a boundary `EvidenceArtifactId` itself was never meant
to know about or defend on its own.

Keeping the stricter rule in the storage layer, rather than tightening
`EvidenceArtifactId`'s own constructor to match, preserves a boundary this
Unit's own review treated as important: `EvidenceArtifactId` is a general
identity concept that other, future contexts (a governed acceptance API,
a future retrieval surface, a reference used outside any filesystem
context at all) may need to hold without being constrained by one
particular storage implementation's own safety requirements. Narrowing
`EvidenceArtifactId` itself to satisfy `FileSystemEvidenceArtifactStorage`
would have coupled a general-purpose identity type to one specific,
swappable implementation detail — precisely the kind of coupling the
storage abstraction (`EvidenceArtifactStorage`) exists to prevent, and
precisely why an in-memory implementation of the same interface can, and
does, apply the identical stricter rule without ever touching a
filesystem itself: the rule belongs to the storage *boundary*, not to
identity, and both implementations enforce it identically so that which
identifiers are accepted never silently depends on which implementation
happens to be in use.
