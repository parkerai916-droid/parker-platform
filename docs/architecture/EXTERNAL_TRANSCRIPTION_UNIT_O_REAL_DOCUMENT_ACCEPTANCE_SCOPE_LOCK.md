# External Transcription Programme — Unit O Real-Document Acceptance Scope Lock

## Status

**Adopted governance.** This document freezes Unit O's acceptance method and authorises only the
detached, offline, synthetic O.2 instrument. It does not authorise real-evidence access, local OCR
of real evidence, provider egress, analysis, deployment, or Unit O.3–O.7 execution.

## 1. Purpose

Unit O will determine whether owner-selected external transcription is materially more useful and
reliable than Parker's existing local OCR for exactly two representative real documents, while
keeping errors and uncertainty visible. Neither result becomes authoritative merely because of
its producer. The human-reviewed source remains the comparison reference.

## 2. Constitutional alignment

This lock is subordinate to the Evidence Custodian, OCR Mechanism, OpenAI External Transcription
Provider Authorization, OCR Fidelity, Tier B Durable OCR Content, and Derivative Content
Persistence/Retrieval locks. It does not reopen their authority decisions. In particular:

- Evidence Custodian remains sole authority for source identity and bytes;
- external transcription remains a separate owner-selected and Permission-Engine-authorised act;
- local and external results are immutable subordinate derivatives with fresh generation IDs;
- retrieval requires a known EvidenceArtifactId plus known DerivativeGenerationId;
- no latest-generation selection, supersession, retry, fallback, model switch, or analysis occurs;
- external output remains UNVERIFIED_LITERAL_TRANSCRIPTION unless separately verified; and
- Memory, Knowledge, QMD/RKS, saved analysis, and canonical-truth authority are absent.

## 3. Locked stages

O.1 governance lock; O.2 detached instrument and offline synthetic verification; O.3 owner
selection and metadata-only preflight; O.4 clean printed image-only PDF; O.5 difficult
handwritten/mixed PDF; O.6 human review and locked decision; O.7 closure and regression. Stages
must not be collapsed. Unit N remains closed.

## 4. Document cases

Later execution uses exactly two separately selected EvidenceArtifactIds:

1. CLEAN_PRINTED — one clean printed image-only PDF.
2. HANDWRITTEN_MIXED — one difficult handwritten/mixed PDF.

The owner selects and authorises each document separately. Selection or authorisation of one does
not apply to the other.

## 5. Metadata-only preflight

Unit O uses a detached acceptance bridge, not a new production HTTP route. Before content access
or egress it may expose only EvidenceArtifactId, authoritative SHA-256, byte length, declared
media type, and eligibility against the locked bounds. It must expose no content, transcript,
case narrative, unrelated metadata, Memory, Knowledge, saved analysis, worksheet material, or
secret. O.3 is not authorised by this document's O.2 implementation permission.

## 6. Comparison operations

For each selected document, one intended local durable OCR operation and one intended external
transcription operation produce independent immutable generations. Each generation ID is captured
at admission and retained. Both are retrieved only by their exact EvidenceArtifactId and
DerivativeGenerationId. Neither path is prerequisite, fallback, authority, or substitute for the
other. Failure cannot manufacture a generation.

## 7. External egress and request bound

Only the selected document's bounded processing representation plus the fixed transcription
instruction/schema may leave Parker. Surrounding evidence, case narrative, Memory, Knowledge,
saved analyses, earlier derivatives, worksheets, unrelated metadata, analytical instructions,
tools, browsing, and secondary retrieval are forbidden.

The programme maximum is two provider requests: one per selected document. Retries, fallback
provider calls, model switches, endpoint switches, and analysis invocations are zero. A failure
stops that document's external operation.

## 8. Immutable persistence and restart

Local and external generations are separate write-once record/content pairs. After both documents
have produced their intended admitted generations, one controlled restart is permitted in O.6 or
O.7. All four generations are retrieved afterward by their recorded exact pairs. Restart and
retrieval must invoke no OCR, provider, fallback, latest-selection, regeneration, or analysis.
Missing generations are reported, never manufactured.

## 9. Worksheet

One worksheet per document records reviewer identity/time, evidence identity/hash/length, exact
local/external generation IDs, and page-by-page review units for names, dates, dollar amounts,
legal wording, ordinary text, handwriting, omissions, additions, reading order, page coverage,
uncertainty, and owner usability.

Review classifications are fixed as EXACT_CORRECT, SUBSTANTIVELY_CORRECT, INCORRECT, OMITTED,
INVENTED_HALLUCINATED, and GENUINELY_UNREADABLE_UNCERTAIN. Criticality is fixed as CRITICAL_FACT,
SUBSTANTIVE_WORDING, or ORDINARY_TEXT. Worksheets do not modify derivatives and are not Evidence,
Memory, Knowledge, or canonical truth.

Completed worksheets and bounded acceptance records are stored outside Git under
`/mnt/parker-data/parker/acceptance-records/unit-o/` with owner-restricted permissions and retained
through programme closure. Unit O creates no automatic deletion lifecycle.

## 10. Locked decision rule

Governance gates require exactly the two locked cases; one intended local and external operation
per case; at most two external requests; zero retry/fallback/model-switch/analysis; unchanged
source identity/hash/length/bytes; exact and restart retrieval; truthful supported page accounting;
no fidelity promotion; and no critical external invention.

CLEAN_PRINTED requires external non-inferiority on critical errors, omissions, page coverage,
reading order, and owner usability. HANDWRITTEN_MIXED requires at least two fewer external
high-impact defects and external usability at least one anchored point higher. Combined external
high-impact incorrect/omitted/invented units must fall by at least 25 percent and at least three
units. External hallucinations must not exceed local hallucinations; critical external
hallucinations must be zero; genuinely unreadable/obscured material must be qualified. Any
governance failure overrides quality. No single OCR percentage replaces these rules.

## 11. Privacy and diagnostics

Diagnostics may contain fixed statuses, bounded counts, fixed enums, evidence/generation IDs,
hashes, byte lengths, and safe failure categories. They never contain source/transcript/worksheet
text, provider bodies/messages, prompts, Base64, credentials, headers, filesystem paths, or stack
traces. Human worksheets are sensitive acceptance records and never enter Git or ordinary logs.

## 12. O.2 implementation authority

O.2 may add a detached Gradle task, detached integration source, acceptance-only bridge, synthetic
fixtures, and offline tests. It may reuse production durable admission/retrieval types with
synthetic inputs and temporary storage. It may not add a production manifest endpoint, change
production runtime/provider semantics, require a credential, access real evidence, make a network
call, invoke Unit N, or persist into Parker's real stores.

## Final determination

**UNIT O.1 ADOPTED — O.2 OFFLINE SYNTHETIC IMPLEMENTATION AUTHORISED.** O.3 and all real-document
or provider execution remain separately gated.
