# Tier B OCR Durable Representation — Bounds Decision

## Status

**Implementation-note, not a scope-lock.** Resolves exactly one mandatory
precondition the accepted
`docs/architecture/DOCUMENT_INGESTION_TIER_B_DURABLE_OCR_DERIVATIVE_CONTENT_SCOPE_LOCK.md`
(the Tier B scope lock) §17 imposes before durable Tier B admission may be
implemented: "freezing further limits is a mandatory precondition for
implementation authorisation, not an encouragement." No other section of
that document is reopened, reinterpreted, or restated here.

## 1. Prerequisite

Tier B scope lock §17 requires finite, justified numeric maximums, frozen
before implementation, for: segment count; individual segment text
length; warning count; individual warning length; provenance/metadata
string length (mechanism identity, configuration profile, mechanism
version, and — per the now-completed truthful-provenance unit,
`TIER_B_OCR_TRUTHFUL_MANDATORY_PROVENANCE_IMPLEMENTATION_PLAN.md` — model
identity, model version); and any other repeated collection or free-form
string in the persisted representation. The outer 20 MiB `recognisedText`
ceiling and 64 MiB total-entry ceiling are already frozen (§17, first
paragraph) and are not revisited here.

## 2. Decision

Every limit below reuses an already-governed number from either the
existing OCR mechanism bounds (Unit 12 §14) or the existing Tier A
derivative-content codec (`DerivativeContentCodec.kt`) — no new numeric
value is invented.

| Field | Limit | Reused from |
| --- | --- | --- |
| Segment count | 200 | `MAX_PDF_PAGES` (`tools/docling-ocr-bridge.py`) — segments are page-aligned (`OcrRecognitionSegment.pageNumber`); Tier B OCR is already bounded to 200 pages |
| Individual segment text length | 20 MiB | `MAX_OUTPUT_BYTES` — a single segment can never exceed the whole-document `recognisedText` ceiling already governing the same OCR result |
| Warning count | 200 | `MAX_PDF_PAGES`, same page-aligned rationale as segment count |
| Individual warning length | 1 MiB | `MAX_SHORT_STRING_BYTES` (`DerivativeContentCodec.kt`) |
| Degradation reason length | 1 MiB | `MAX_SHORT_STRING_BYTES` — same class of field as a warning, not document text |
| `mechanismIdentity` length | 1 MiB | `MAX_SHORT_STRING_BYTES` — the identical bound the same codec already applies to `DerivativeProducerIdentity.pluginIdentity` |
| `mechanismVersion` length | 1 MiB | `MAX_SHORT_STRING_BYTES` — mirrors `pluginVersion` |
| `configurationProfile` length | 1 MiB | `MAX_SHORT_STRING_BYTES` — mirrors `configurationIdentity` |
| `modelIdentity` length | 1 MiB | `MAX_SHORT_STRING_BYTES` — mirrors `DerivativeProducerIdentity.modelIdentity` |
| `modelVersion` length | 1 MiB | `MAX_SHORT_STRING_BYTES` — generous relative to the field's own actual fixed shape (`sha256:` + 64 hex characters, ~71 bytes) |
| `EvidenceArtifactId`/`DerivativeGenerationId` length | 1 MiB | `MAX_SHORT_STRING_BYTES` — already applied automatically; these reuse Tier A's own identifier encoding unchanged |

## 3. Rationale

Segments and warnings are the only two repeated collections a Tier B
result can carry; both are naturally scoped to page count, so reusing the
already-accepted 200-page ceiling is a direct, non-arbitrary
justification rather than a freshly invented number. Every free-form or
identifier-shaped string reuses the Tier A codec's own existing
`MAX_SHORT_STRING_BYTES` (1 MiB) — the identical bound already applied to
the structurally equivalent `DerivativeProducerIdentity` fields Tier B
will populate through the same producer-identity mapping. No limit here
is smaller than what real OCR output has ever required: today's bridge
never populates `segments` at all, and warnings are short, occasional
diagnostic strings — 200 of either, at up to 1 MiB (segments: 20 MiB)
each, is generous headroom, not a practical constraint on genuine
recognition output.

All bounded fields, summed at their own individual maximums, remain well
inside the already-frozen 64 MiB total-entry ceiling (§17, first
paragraph): the dominant term is `recognisedText` at 20 MiB, plus at most
200 × 20 MiB of segment text — the codec's own 64 MiB total-entry check
(already enforced, unchanged) is the final, authoritative backstop should
a pathological combination of otherwise-individually-legal fields exceed
it; no field bound above is claimed sufficient on its own to guarantee
the total stays under 64 MiB, only that the existing check already
catches any case where it would not.

## 4. Acceptance implication

A future implementation must enforce every limit above explicitly, before
durable publication, rejecting an oversized representation in full — never
truncating silently and reporting success (Tier B scope lock §17's own
requirement, restated exactly, not weakened). Enforcement may reuse the
existing codec's own `writeString(value, MAX_SHORT_STRING_BYTES)` /
`writeStrings()` bounded-write machinery directly, since every field
above maps onto that machinery's own existing bound categories
(`MAX_SHORT_STRING_BYTES`/`MAX_LARGE_TEXT_BYTES` are not both needed —
only the short-string bound applies to every Tier B–specific field this
decision covers, since Tier B introduces no new large-text field beyond
the already-bounded `recognisedText`/segment text).

## 5. Recommendation

READY FOR IMPLEMENTATION. This resolves the sole outstanding §17
precondition; no other Tier B scope-lock section requires a further
frozen decision before implementation.
