# ORDINARY-INGESTION-10R1 visual-observation offset coordinate correction

## Determination

OI10's preserved Fixture A response is genuinely outside Parker's strict coordinate contract. Parker
did not alter its literal text or offsets. The v6 validator already counts Unicode scalar values/code
points, consistent with Parker's uncertainty and anchor model, but the v6 provider instruction says
nothing about offset coordinates and the schema supplies only suggestive field names and numeric
minimums. It does not bind positions to the exact `literal_text`, distinguish code points from bytes,
UTF-16 units, or graphemes, or express the cross-field upper bound.

The defect classifications are **A PROVIDER_OUTPUT_DEFECT**, **B INSTRUCTION_AMBIGUITY**,
**C SCHEMA_AMBIGUITY**, and **J VISUAL_OBSERVATION_MODEL_DEFECT** (the provider-facing precision
contract was underspecified). The evidence excludes codec, newline-normalization, Unicode-
normalization, surrogate, persisted-projection, reconstruction, and Parker coordinate-mismatch
defects. The terminal end 274 happens to equal both UTF-8 byte length and NFD code-point length, but
the earlier ranges already point at incorrect characters under those interpretations. No alternative
coordinate system makes the full observation set semantically coherent.

The minimum safe correction is a distinct successor identity, not a rewrite of historical v6:

- capability `ordinary-external-request-region-transcription-v7`;
- lifecycle `ACCEPTANCE_PENDING`;
- profile `request-region-anchored-fidelity-acquisition-v3`;
- schema `request-region-anchored-transcription-schema-v3`, wire 7;
- adapter `openai-responses-request-region-transcription-adapter` version `6.0.0`;
- parser `openai-request-region-structured-response-parser` version `2.0.0`;
- processing `external-transcription.deterministic-complete-set-request-region-v3`;
- coordinate identity `unicode-scalar-code-point-half-open-v1`.

V6 remains historical and `ACCEPTANCE_PENDING`. Production remains on the accepted pre-v6 path.

## Preserved evidence

OI10 implementation under acceptance was
`87764757a35a6df8d2491a0fe69608bafee5bca0`; harness commit was
`c14c8829a8a7c37ea5a08ce030db9361855e4eec`. The immutable Fixture A files verified before analysis:

| Record | SHA-256 |
|---|---|
| authority | `e40fbb2afdefef0f5e1c4b513fc822f812f5c23a77455579972870378b83a313` |
| manifest | `55af9fdb03a3ac33d1181cd9e25c12ae267af94f1da8d21b06f92fee14072750` |
| attempt | `cfef2561f32ea0bca04e989eb51d7b58bb9b498b14a09bff19e541af398a60c5` |
| raw-response record | `4a9477fcb478255349519e99c11783dd8116e5fb7711f504c02d41eeb9563d55` |
| failure assessment | `a9372e9ae27498fd7819427d28a258250204c539e54cd4ce5e921daac6d0842d` |

Response ID was `resp_0e12772844c8f1b7016a94eb570ec887d0a6f4140a8360802f`; raw response SHA was
`8ea216dde619c35de7c164787e878db754f795d94fcdaa28cce4491cd8cc768b`; raw structured-output-text
SHA was `6585769db976874e30fb166812cc120bcc4338699e92b0599f776d1b3a292ce9`; parsed canonical structured
SHA was `59f37649c8af98e1897cd4a509bd6cfa0744814cf5314bf590e4a3746bc0ef41`;
exact literal UTF-8 SHA was `266acc1c82c09d8839f7837b3bbfb761424dd3587948bd51b091be62e6fb9741`.

## Exact transcription representations

The persisted string was analyzed without modification:

| Representation | Length |
|---|---:|
| UTF-8 bytes | 274 |
| Unicode scalar values/code points | 272 |
| UTF-16 code units | 272 |
| extended grapheme clusters for this composed text | 272 |
| Java/Kotlin `String.length` | 272 |
| LF characters | 20 |
| CR characters / CRLF sequences | 0 / 0 |
| CR/CRLF normalized to LF | 272 code points |
| LF expanded to CRLF | 292 code points |
| NFC | 272 code points |
| NFD | 274 code points |
| without terminal newline | 272 (there is none) |
| without leading/trailing whitespace | 272 (there is none) |

The two additional UTF-8 bytes arise from the composed non-ASCII scalars in `Māori` and `Kōwhai`.
NFD similarly decomposes those two scalars. Neither transformation occurred in the response.

## Complete observation inventory

“CP/16/G” reports bounds validity under code-point, UTF-16, and grapheme coordinates; those lengths
are all 272 for this literal. “B8” reports UTF-8 byte-bound validity against 274. Substrings are what
Parker resolves under its governed code-point semantics.

| # | Kind | Range/type | CP/16/G | B8 | Parker substring | Newline/terminal/zero |
|---:|---|---|---|---|---|---|
| 1 | ALL_CAPS | `[0,26)` range | valid | valid | `SCANNED SYNTHETIC EVIDENCE` | no/no/no |
| 2 | BOLD | `[0,26)` range | valid | valid | same heading | no/no/no |
| 3 | ENLARGED_TEXT | `[0,26)` range | valid | valid | same heading | no/no/no |
| 4 | PARAGRAPH_BREAK | `[26,27)` range | valid | valid | first `LF` of a two-LF break | yes/no/no |
| 5 | ALL_CAPS | `[28,54)` range | valid | valid | `PARKER-FIXTURE-2026-003\n\nO` | yes/no/no |
| 6 | BOLD | `[28,54)` range | valid | valid | same misaligned range | yes/no/no |
| 7 | PARAGRAPH_BREAK | `[54,55)` range | valid | valid | `C` | no/no/no |
| 8 | ALL_CAPS | `[56,78)` range | valid | valid | ` CONTROL: SCAN-88421\n\n` | yes/no/no |
| 9 | PARAGRAPH_BREAK | `[78,79)` range | valid | valid | `S` | no/no/no |
| 10 | PARAGRAPH_BREAK | `[97,98)` range | valid | valid | `A` | no/no/no |
| 11 | PARAGRAPH_BREAK | `[118,119)` range | valid | valid | `h` | no/no/no |
| 12 | PARAGRAPH_BREAK | `[135,136)` range | valid | valid | `t` | no/no/no |
| 13 | PARAGRAPH_BREAK | `[153,154)` range | valid | valid | `r` | no/no/no |
| 14 | PARAGRAPH_BREAK | `[191,192)` range | valid | valid | `o` | no/no/no |
| 15 | PARAGRAPH_BREAK | `[212,213)` range | valid | valid | `L` | no/no/no |
| 16 | PARAGRAPH_BREAK | `[240,241)` range | valid | valid | `R` | no/no/no |
| 17 | PARAGRAPH_BREAK | `[273,274)` range | **invalid** | valid bounds only | out of range (UTF-8 bytes select final `.`) | terminal/no |

Observation count is 17. Bounds-valid count is 16 under code-point/UTF-16/grapheme semantics and 17
under UTF-8 byte bounds. The first and only strict bounds-invalid observation is #17; maximum end is
274. There are no zero-width observations and no `LINE_BREAK` observations. Semantic target alignment
is already lost at #5; #7 onward repeatedly labels ordinary letters as paragraph breaks. Thus merely
choosing UTF-8 would make the arithmetic fit while leaving the observations false. This is precisely
why unit inference, clamping, dropping, and tolerance were forbidden.

## Raw/codec comparison

The incompatible offsets are present in the raw Responses API `output_text` JSON. JSON escape decoding
produces the exact 272-code-point literal and the exact integer offsets above. There is no CRLF, no
terminal newline, no leading/trailing whitespace, no supplementary scalar, and no normalization step.
The raw envelope is preserved base64 with verified digest. `RegionJson` parses strings without
normalization; canonical structured projection changes JSON serialization bytes, not string content or
offset values. Validation occurs before reconstruction. Parker transformation introduced mismatch:
**NO**. Raw provider response already incompatible: **YES**.

## Existing and corrected contracts

Existing v6 instruction wording only requires preserving exact Unicode and line/paragraph breaks; it
does not mention observation positions. The v6 schema names fields `start_code_point` and
`end_code_point_exclusive`, constrains minima, and requires both fields, but gives no descriptions and
cannot express `end <= codePointCount(literal_text)`. The v6 validator calls
`text.codePointCount(0, text.length)` and `RegionVisualObservationAnchor.resolve`, then requires point
anchors only for `LINE_BREAK` and non-empty ranges for other anchored kinds. Parker-wide uncertainty
coordinates and `RegionVisualObservationAnchor.Point(codePoint)` use the same code-point model.

The selected contract is therefore zero-based Unicode scalar-value/code-point positions over the exact
returned `literal_text`, with half-open `[start,end)`, `0 <= start <= end <= codePointLength`, no hidden
or normalized text, `[n,n)` only for `LINE_BREAK`, non-empty ranges for other anchored observations,
and `null/null` only for unanchored observations. JSON Schema descriptions now state that contract.
Cross-field arithmetic remains strict Parker post-response validation; the report does not claim JSON
Schema can prove it.

The successor validator does not weaken the old validator. It applies explicit v7 coordinate checks
and then retains the complete existing structural/status/identity validator through version-only
translation. It never clamps, shifts, drops, tolerates, guesses, normalizes, or repairs provider state.

Visual observations remain fidelity-critical and provenance/review-critical metadata. They are bound
into derivative blocks and ordinary authorization/reconciliation digests. They do not determine Parker
source order, but invalid observations must prevent admission because accepting them would corrupt the
typed fidelity record. Their precision is retained rather than silently downgraded.

## Identities, replay, and verification

New digests:

- capability: `570ab8975a9a10a809f27c0bbe5dfb67beb9770ca26dfe32af17d52990da5acc`;
- instruction: `d720870fb27408211a89880c09449148c2370bb5cffc9c152bfcdbf2341b8efe`;
- schema: `f045ee304e02323643400837e7a19f81eadeff2ecac737983f8bc8466fcf65ab`.

Instruction changed: **YES**. Schema changed: **YES**. Successor validator added: **YES**. Capability
semantics changed: **YES**. Historical v6 identities and evidence remain unchanged. The exact preserved
OI10 response replays as **STILL MALFORMED** under the corrected, unambiguous contract; history was not
rewritten and the old call is not acceptance evidence for v7.

Targeted provider-free tests: 7 passed. They cover valid ranges, exact terminal end, excessive end,
negative/reversed ranges, supplementary scalars versus UTF-16, combining scalars, CR/LF, terminal
newline, point/unanchored forms, `LINE_BREAK`, exact parser retention, unchanged raw persistence, full
legacy structural validation, versioned identities, and exact OI10 replay. Full suite: 242 suites,
3,278 tests, 0 failures, 0 errors, 11 skipped. `git diff --check`: PASS.

OpenAI calls: 0. Claude calls: 0. Production mutations: 0. Production counts remained attempts 4,
provider-state 2, generations 21, content 19, capability acceptances 6, ordinary owner authorizations
5. No deployment, image, Compose, container, route, authorization, execution, acceptance, or production
store changed.

Implementation commit: `6425a2f5ec85f5b57c452d693cef032c0bbeb2ac`.

## Next acceptance plan

Do not promote v6 or v7. The next unit must create a fresh, build-bound v7 authority and repeat the
governed four-fixture fidelity acceptance with a new provider-call budget. OI10 Fixture A remains
historical failed evidence and must not be retried or counted as a v7 pass.

**UNIT ORDINARY-INGESTION-10R1 COMPLETE — OI10 MALFORMED RESPONSE TRACED TO AN AMBIGUOUS OR INCOMPATIBLE VISUAL-OBSERVATION OFFSET COORDINATE CONTRACT; V6 CONTRACT AND STRICT VALIDATION HAVE BEEN CORRECTED OFFLINE WITHOUT WEAKENING BOUNDS; FRESH PROVIDER FIDELITY ACCEPTANCE REQUIRED; PRODUCTION UNCHANGED**
