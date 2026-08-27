# External Transcription Programme — Unit O.4R Literal-v2 Remediation Scope Lock

## Status

**Adopted governance.** This amendment freezes the Unit O.4R remediation design and authorises
only a later, separately bounded implementation unit. It does not authorise production-code or
runtime-configuration changes, deployment, a provider request, OCR, analysis, CLEAN reacceptance,
or O.5 execution.

## 1. Authority and relationship to existing governance

This lock is subordinate to Parker's Evidence Custodian, OCR Mechanism, OCR transcription
fidelity, Tier B durable derivative-content, OpenAI external-transcription provider-authorization,
Unit I processing-representation, Unit J durable-admission, exact-generation retrieval, analysis,
and Unit O real-document acceptance governance. It narrowly amends those instruments only where
stated below. Their evidence-custody, authority, immutability, privacy, failure, and human-review
boundaries remain unchanged.

Unit O.4's source-grounded human review found that the external result was more readable than the
local result but contained critical, fluent, source-inconsistent invention. CLEAN non-inferiority
therefore failed. The demonstrated cause is classified as `MODEL_CAPABILITY_LIMITATION` under the
tested profile; no custody, source-identity, request-contamination, transport, schema-pressure,
response-extraction, or admission defect was established.

Readability, fluency, provider success, structural validity, and apparent completeness establish
neither fidelity nor truth. A generative external provider may produce an
`UNVERIFIED_LITERAL_TRANSCRIPTION`, but provider/model/configuration suitability for literal
evidence transcription requires governed acceptance, and critical source-inconsistent invention
is an acceptance failure.

## 2. First remediation and immutable identities

The first remediation is the candidate transcription profile:

`openai-literal-page-transcription-v2`

The historical profile remains:

`openai-faithful-page-transcription-v1`

The candidate adapter version for a later implementation is `1.1.0`. The OpenAI provider,
`gpt-4.1-mini` model-selection rule, `POST /v1/responses` endpoint, `store=false`, and processing
profile `external-transcription.direct-byte-exact-v1` remain unchanged. The remediation permits no
model or endpoint change, retry, fallback, analysis, O.5 execution, or provider request during
implementation.

Existing generations retain their original profile, adapter, provider, model, processing, and
correlation facts. No historical generation is migrated, backfilled, rewritten, or reinterpreted.

## 3. Frozen literal-v2 instruction

The instruction is exactly the following single line. Its canonical byte sequence contains no
indentation, BOM, line break, or terminal newline:

```text
Perform literal transcription only. Reproduce only text visibly present in the submitted source. Preserve source spelling, punctuation, capitalization, page association, and visible reading order exactly as seen. Do not paraphrase, summarize, rewrite for clarity, correct grammar, normalize wording, infer missing text, or complete fragments. Do not insert likely names, dates, amounts, facts, legal propositions, or contextual completions. Do not use general knowledge, perform legal interpretation or analysis, or resolve factual conflicts. Where handwritten text is visibly distinct, preserve it as a separate line or block in visible reading order; do not merge it into or rewrite printed text. If any text is unreadable or uncertain, disclose that using the structured uncertainty, illegibility, qualification, or page-outcome fields and qualify or omit the text instead of completing it. Omission or qualification is preferable to invention. If page orientation is awkward, read the visible page as supplied and do not invent text to compensate. Output must correspond only to the submitted source. Return only the strict structured schema.
```

Its canonical form is its exact UTF-8 encoding: no BOM; no terminal newline; no trimming, newline
normalisation, indentation mutation, or JSON escaping before hashing. The SHA-256 digest is the
lowercase hexadecimal encoding of the digest over those pre-JSON bytes. The same bytes later
become the Responses API `instructions` value. The frozen instruction is 1,146 bytes and its
digest is:

`c721e63b29e56f9242ee24dd8f13ddcab5d4468d3d17e9e3b9b1d66a68cb2000`

Any byte change creates a different configuration tuple and requires a different immutable
profile/version and renewed acceptance.

## 4. Frozen structured schema and digest rule

Literal-v2 reuses the existing strict `parker_page_transcription` schema without structural,
field-description, required-field, or enum changes. It already permits nullable text, qualified
or partial outcomes, uncertainty and illegibility spans, warnings, and truthful page accounting;
it does not require complete prose.

The schema is parsed as JSON and serialised under RFC 8785 JSON Canonicalization Scheme semantics:
object keys are lexicographically ordered, array order is preserved, primitives and escaping are
canonical, insignificant whitespace is absent, and the result is UTF-8 without BOM or terminal
newline. Its lowercase 64-character SHA-256 digest is calculated over those canonical bytes. The
schema submitted to the provider must derive from the same canonical bytes. An implementation
test must freeze the resulting digest; a mutable label alone is insufficient configuration
identity.

## 5. Provider-profile acceptance-state amendment

Acceptance state binds the complete tuple of provider identity, model-selection rule,
transcription profile identity, instruction digest, schema digest, and processing-profile
identity. It is not inferred from configuration loading, credentials, connectivity, provider
success, or another tuple's acceptance.

- `DISABLED`: neither ordinary nor acceptance execution is available.
- `CONFIGURATION_READY`: the tuple validates structurally, but no provider-execution authority
  exists.
- `ACCEPTANCE_PENDING`: only a separately governed acceptance execution may use the tuple;
  ordinary real-document transcription remains unavailable.
- `ACCEPTED`: ordinary, governed, explicit-owner transcription may use the exact tuple, subject to
  all other readiness and authorization gates.
- `SUSPENDED`: a previously available tuple is disabled because of a capability, acceptance,
  safety, policy, or mutable provider-fact concern. Neither ordinary nor acceptance execution is
  available unless later governance changes its state.

The historical OpenAI / `gpt-4.1-mini` / v1 tuple is `ACCEPTANCE_PENDING`: its technical
composition remains usable only for a separately authorised acceptance comparison, while its O.4
failure prevents ordinary real-evidence use. Literal-v2 begins `CONFIGURATION_READY`. Passing
implementation or offline tests cannot move it to `ACCEPTED`; a separately authorised and
source-grounded acceptance decision is mandatory.

Production readiness and acceptance-instrument readiness are distinct. The ordinary owner route
must fail closed for every state except `ACCEPTED`. A detached acceptance instrument may execute
only an `ACCEPTANCE_PENDING` tuple and only under separate, document- and request-specific
governance.

## 6. Provider-neutral configuration provenance amendment

Every new literal-v2 generation retains:

- transcription profile ID/version;
- exact instruction SHA-256;
- exact structured-schema SHA-256;
- adapter identity and version;
- provider identity;
- exact provider-returned model identifier;
- truthful model-snapshot state;
- processing-profile identity; and
- provider correlation identifier.

The provider-neutral contract distinguishes two closed forms:

- `HistoricalProfileOnly(profileId)`, for a historical representation that recorded a profile
  identity but no configuration digests; and
- `DigestedConfiguration(profileId, instructionSha256, structuredSchemaSha256)`, mandatory for a
  new literal-v2 external generation.

Absence is represented truthfully by the historical form. Parker never derives, guesses, inserts,
or backfills a digest for historical bytes.

## 7. Durable OCR representation version 3

A later implementation may add OCR durable representation version 3. The v1 and v2 readers and
historical encoded bytes remain unchanged. Version 3 extends the external-transcription facts
carried by v2 with mandatory instruction and structured-schema digests through
`DigestedConfiguration`.

Missing, malformed, oversized, or non-SHA-256 digests reject the entire candidate before durable
publication; they are never truncated or replaced. There is no migration, backfill, or rewrite.
Local OCR remains honestly representable without fabricated external-provider configuration facts
and need not acquire external configuration provenance merely because v3 exists.

## 8. Immutable human verification

Human verification is a separate immutable, append-only, independently retrievable and auditable
record bound to an exact `EvidenceArtifactId`, `DerivativeGenerationId`, and reviewed scope. A
record contains:

- `HumanVerificationRecordId`;
- `EvidenceArtifactId` and `DerivativeGenerationId`;
- one-based reviewed page scope and optional bounded character scope;
- reviewer `PrincipalId`;
- review timestamp;
- `REVIEW_PASSED`, `REVIEW_FAILED`, or `PARTIALLY_VERIFIED`;
- review-artifact SHA-256; and
- optional bounded, sensitive notes/status.

`UNREVIEWED` is derived only when no review record exists; Parker does not manufacture an
unreviewed record. A review never mutates the derivative, establishes source or canonical-truth
authority, writes Memory or Knowledge, or automatically changes fidelity. `REVIEW_PASSED` does
not by itself assign `VERBATIM`; that classification remains separately governed and applies only
to an explicitly verified scope.

Review notes and artifacts are sensitive. Ordinary logs and diagnostics may contain bounded IDs,
scope, outcome, timestamps, and digests, but not review text or source/transcription content.

## 9. Owner presentation safety

Every external generation displays prominently and without qualification:

`Machine transcription — unverified`

and:

`Fluent machine transcription may contain plausible text that is inconsistent with the source.`

The same presentation displays the exact `DerivativeGenerationId`, provider identity, exact
returned model identifier, transcription profile identity, and separate human-review state. A
failed review remains visible.

The UI never ranks external above local output, preselects it, calls it preferred, infers accuracy
from fluency, or infers verification from readability, completeness, provider identity, or
structural validity.

## 10. Exact-generation analysis acknowledgement

Analysis remains a separate, owner-selected act over exact known generations. When an exact
selected external generation is `UNVERIFIED_LITERAL_TRANSCRIPTION` and its selected scope is
`UNREVIEWED`, `REVIEW_FAILED`, or outside the verified scope of `PARTIALLY_VERIFIED`, analysis
requires an explicit authenticated acknowledgement bound to that exact `EvidenceArtifactId` plus
`DerivativeGenerationId`.

Browser-only acknowledgement is insufficient. Server-side validation occurs after exact-generation
retrieval establishes the generation's actual type, fidelity, and review coverage. An absent,
mismatched, stale, or wrong-generation acknowledgement fails closed before analysis. No latest-
generation substitution, provider preference, automatic inclusion, or review inference is
permitted.

## 11. CLEAN literal-v2 reacceptance gate

Reacceptance uses the already locked CLEAN evidence and requires separate later authorization. It
allows exactly one provider request, with no retry, fallback, or model switch. Source
`EvidenceArtifactId`, SHA-256, and byte length remain unchanged. The representation is the
byte-exact source PDF under `external-transcription.direct-byte-exact-v1`.

Success requires exact-generation durable admission and retrieval, mandatory source-grounded human
review, materially complete page coverage, materially preserved reading order, honest uncertainty
and illegibility, no substantive paraphrase, zero critical hallucinations, and zero invented names,
dates, amounts, or legal propositions. External remains non-inferior to local on critical errors,
omissions, page coverage, reading order, and owner usability. All prior CLEAN gates remain in
force; governance failure overrides apparent quality.

## 12. Unit O request-budget amendment

The Unit O provider-request ceiling increases from two to exactly three, allocated immutably:

1. CLEAN_PRINTED initial acceptance — consumed.
2. CLEAN_PRINTED literal-v2 reacceptance — allocated but not authorised for execution.
3. HANDWRITTEN_MIXED O.5 — reserved and not authorised for execution.

No request may transfer between allocations. No retry may consume another allocation. A failed
transport, provider, response, or validation operation consumes only its specifically authorised
operation under existing programme semantics and never silently activates or repurposes another
allocation.

## 13. Orientation deferral

Orientation-normalised rendering is not selected for literal-v2. The processing profile remains
`external-transcription.direct-byte-exact-v1`, and the source PDF bytes remain the provider input.
Rendering or rotating pages remains a separately governable fallback hypothesis. Any later
transformation requires a distinct processing-representation profile, complete transformation
provenance, privacy/egress reassessment, implementation authority, and renewed acceptance; it may
not silently reuse literal-v2 acceptance.

## 14. Constitutional and conflict review

This amendment creates no Evidence mutation, custody, truth, canonical-text, Memory, Knowledge,
provider-selection, automatic-egress, automatic-supersession, latest-generation, or analysis
authority. Evidence Custodian remains the sole source authority. Durable results remain immutable
subordinate derivatives. `UNVERIFIED_LITERAL_TRANSCRIPTION` remains unverified; fidelity and
completeness remain distinct. Exact-generation identity is preserved through retrieval, review,
acknowledgement, and analysis selection.

The byte-exact first remediation conforms to Unit I. The additive v3 contract preserves Unit J v2
history rather than reopening it. Provider acceptance state narrows rather than expands the
existing provider authorization. Human review creates no source-truth authority. The request-budget
amendment is the sole narrow qualification to Unit O's former two-request ceiling and one-request-
per-document rule.

No constitutional conflict remains after these qualifications.

## 15. Implementation boundary and next unit

Adoption authorises only a later bounded implementation unit to implement the frozen instruction,
configuration digests, v3 compatibility, acceptance-state enforcement, immutable human-review
record, owner warnings, and exact-generation acknowledgement gate with offline tests.

This document does not itself authorise production-code or test-implementation changes, deployed
profile or runtime-configuration changes, a model or endpoint change, a provider request, OCR,
analysis, CLEAN reacceptance, O.5, deployment, or transformed processing representation.

The next unit must remain offline and must report its exact diff and verification before any
deployment or acceptance authority is considered.

## Final determination

**ADOPTED — READY FOR A SEPARATELY BOUNDED OFFLINE IMPLEMENTATION UNIT.** Literal-v2, configuration
identity, historical compatibility, human-review separation, owner safety, exact-generation
acknowledgement, CLEAN reacceptance, request allocation, and orientation deferral are frozen as
stated above.
