# Parker Ingestion Prep

Parker Ingestion Prep is a Parker-side, pre-ingestion preparation boundary. It
does not replace Hermes, submit evidence, assign a case, OCR, parse documents,
acquire evidence, or make an evidence-governance decision.

## Usage

```text
python3 tools/parker_ingestion_prep.py /path/to/source \
  --workspace /mnt/parker-data/ingestion-prep \
  --job-id JOB-20260919-001 \
  --case-id case-opaque-id
```

The default workspace is `/mnt/parker-data/ingestion-prep`. A job is created
under `jobs/<job_id>/` with `source_copy/`, `extracted/`, `ready/`,
`manifests/`, `reports/`, `logs/`, and `prep.db`. The workspace-level
`prep-content-index.db` is the preparation deduplication index; it is not a
replacement for Parker's governed evidence identity index.

The final `reports/handoff.json` identifies the verified unique `ready/` view,
manifest, reconciliation report, and aggregate handoff status. `source_copy/` still
retains original copied ZIP containers for provenance, but ZIP containers and
duplicates are not placed in `ready/`. An operator may hand the `ready/` view
to the existing Parker/Hermes ingestion process. Prep never invokes that
process itself.

## Source immutability and copy integrity

The source root is opened only for reading. It must not be the preparation
workspace. Files retain their relative paths below `source_copy/`; symlink
sources are review-required and are not followed. Every unique source is
hashed with SHA-256, copied through a temporary file, flushed and atomically
renamed, then hashed again. A mismatch is `HASH_MISMATCH` and cannot enter the
ready handoff.

## Canonical content and provenance

The preparation index is keyed only by lowercase SHA-256. Filename, path,
extension, size, and timestamps are not duplicate identity. The first verified
preparation occurrence is the canonical content item. Later occurrences are
retained in the per-job ledger and manifest as `DUPLICATE_IN_JOB` or
`DUPLICATE_EXISTING_PREP`. A trusted Parker-owned hash export may be loaded
explicitly through `register_existing_evidence`, producing
`DUPLICATE_EXISTING_EVIDENCE`; prep does not infer or query production
evidence storage.

Duplicate occurrences retain their source path, case association, source hash,
archive parent/member path where applicable, and canonical target. Identical
bytes never merge case permissions, visibility, legal context, or provenance.

## ZIP handling

ZIP files are copied unchanged before inspection and extraction. Extraction is
performed only from that Parker-side copy under the job's `extracted/` tree.
The implementation rejects absolute, drive-qualified, traversal, and unsafe
link members; corrupt and password-protected archives become review states.
Member records are created before preflight limits are applied, so blocked
members remain accountable rather than disappearing. Nested ZIP depth defaults
to 3. Entry count defaults to 10,000 and declared/observed expansion defaults
to 1 GiB. These are configurable, conservative limits.

Every archive member has a ledger/manifest occurrence linked to its original
archive occurrence, copied archive, member path, extracted path when retained,
and child SHA-256. Child content enters the same canonical hash index as loose
source content.

## States and reconciliation

The ledger uses explicit mechanical states including `HASHED`,
`HASH_VERIFIED`, `READY`, the duplicate states, `REVIEW_REQUIRED`, and
`FAILED`. Stable reason codes include `COPY_FAILED`, `HASH_MISMATCH`,
`ARCHIVE_CORRUPT`, `PASSWORD_PROTECTED_ARCHIVE`, `UNSAFE_ARCHIVE_PATH`,
`UNSAFE_ARCHIVE_LINK`, `NESTING_LIMIT_EXCEEDED`,
`ARCHIVE_ENTRY_LIMIT_EXCEEDED`, and `ARCHIVE_SIZE_LIMIT_EXCEEDED`.

`reports/reconciliation.json` counts source files, archives, archive members,
duplicates, verified copies, ready content, review/failure states, and
`unaccounted`. The job completion state is separate from item disposition:

* `jobStatus=COMPLETE` means the preparation run finished processing its
  discovered occurrences;
* `reconciliationStatus=COMPLETE` means every occurrence has an accountable
  terminal state and `unaccounted == 0`;
* `handoffStatus=READY` means no item requires review or failure handling;
* `handoffStatus=READY_WITH_EXCEPTIONS` means valid READY content may proceed,
  while REVIEW_REQUIRED and FAILED occurrences remain withheld from `ready/`;
* `handoffStatus=NOT_READY` means reconciliation is incomplete or unsafe,
  including `unaccounted > 0`.

Duplicates do not cause `READY_WITH_EXCEPTIONS`; they are accounted-for
occurrences and remain excluded from `ready/`. The CLI returns exit code 0 for
`READY`, 2 for `READY_WITH_EXCEPTIONS`, and 3 for `NOT_READY`, allowing
automation to distinguish reviewable item exceptions from an unreconciled
preparation failure. Interrupted temporary copies are never treated as
completed files. Re-running a job reuses its durable occurrence and content
records, verifies the source again, and does not create a second canonical
record for completed content.
