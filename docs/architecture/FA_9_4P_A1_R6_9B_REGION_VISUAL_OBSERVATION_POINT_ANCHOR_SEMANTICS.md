# FA.9.4P-A1E-R6.9B  Region visual-observation point anchors

## Decision

Region transcription wire v5 adopts three explicit anchor forms, measured in Unicode code points:

- `null/null`: an unanchored region-level observation.
- `[n,n)`: a zero-width point at a code-point boundary, where `0 <= n <= literal_text.codePointCount`.
- `[n,m)`: a non-empty half-open text range, where `0 <= n < m <= literal_text.codePointCount`.

The v5 point form is accepted for `LINE_BREAK`. A `LINE_BREAK` range is rejected. Other
observation kinds retain non-empty range or unanchored behavior; a point does not broaden their
contract. Mixed-null endpoints, negative endpoints, reversed ranges, and endpoints beyond the
literal text are rejected fail closed.

Uncertainty spans are deliberately unchanged: they remain non-empty ranges with
`0 <= start < end <= literal_text.codePointCount`.

## Version and identity boundary

Historical wire v4 remains available with its original schema, schema digest, provider instruction,
provider instruction digest, provider profile `openai-region-anchored-transcription-v1`, and adapter
version `3.0.0`. Its point response remains rejected as `MALFORMED_SCHEMA`.

Wire v5 has a distinct schema digest, point-anchor provider instruction, provider profile
`openai-region-anchored-transcription-v2`, and adapter version `4.0.0`. The adapter binds its
request schema and instruction to the selected coherent profile tuple; mixed v4/v5 identities cannot
be constructed.

An explicit superseding-validation mode exists only to revalidate immutable historical response
maps under v5 observation semantics. It does not rewrite the response or reinterpret the original
v4 acceptance result.

## Offline evidence

The isolated copy of the exact consumed provider state was decoded and validated without network
access. Its raw response SHA-256 remained
`500863d65c7f9ca69a66b2ffef3ef8a42b7033903cf1b5a5bd774d9f0decd87f`.
The exact structured map remained byte-for-byte canonically unchanged and retained structured
SHA-256 `7031179aa4267fdc12a50a429eef184e4ecfb2efb3ae993b6a5527ecf9f4c476`.

Superseding validation accepted all 24 distinct requested regions. All 24 were non-empty
`TRANSCRIBED` blocks. It identified exactly 70 `LINE_BREAK` observations, all represented as
valid `[n,n)` points. Existing non-line-break observations retained their captured non-empty
ranges.

## Operational boundary

R6.9B is an offline contract and replay change only. It does not call a provider, create or retry an
attempt, deploy or restart a runtime, change lifecycle state, or mutate a production provider,
authority, assessment, or attempt store. The governed v2 acceptance authority
`authority-fa-9.4p-a1e-r6.8c1` is not recreated.
