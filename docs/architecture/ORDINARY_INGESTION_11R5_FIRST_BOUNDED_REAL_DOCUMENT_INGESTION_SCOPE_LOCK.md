# Ordinary Ingestion OI11R5 — First Bounded Real-Document Ingestion Scope Lock

## Status

**Draft — Pending Owner Acceptance.** OI11R5 is planning only; zero provider calls and zero real-document processing occurred.

## Baseline and purpose

OI11R4 closed with the governed synthetic V8 path proven. Production remains the accepted D335 implementation `d33518e85604083d620be08be4a4f001d7be3187`, image `sha256:5ca82f03ce61eade58c60eb4d3783547b4b266f974ed2ac218c09cf43f86075a`, restart count 0, and V8 capability digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0` accepted. The owner-selected source is presumed human-scrutinized before ingestion; provider output remains derivative.

## Scope and owner boundary

The first real transaction will process one owner-selected, genuine document only (preferred 1–5 pages), with no autonomous selection by Codex. The owner must identify the exact candidate and review its information, third-party content, required pages, and proposed egress before R6 authorization. No case analysis, legal reasoning, summarization, retrieval, or cross-document context is included.

## Runtime readiness verdict

**B — READY WITH OWNER-SELECTION CONSTRAINTS.** The current runtime is ready for a bounded real transaction, but the selected source must be an `application/pdf` resolved through the governed Evidence Custodian, remain within the configured provider/profile source-byte limit, produce 1–32 deterministic request regions, and fit the 16,777,216-byte V8 request-body bound. Page/region facts must be deterministically known; unsupported media or unbounded/ambiguous characteristics are excluded pending separate governance. Existing handling for scanned, mixed, blank, obscured, handwriting, tables, and layout characteristics is observed rather than redefined.

## Identity and evidence path

For an already registered document, R6 must use its immutable evidence ID and manifest without duplication. A new owner-selected document must first use Parker’s canonical registration path; R5 performs no registration. Before authorization R6 freezes filename, media type, byte length, SHA-256, page count, immutable location, manifest identity, and provenance status. Any source change invalidates authorization.

## Egress and request-region lock

Only the selected document’s required PDF bytes/crops may leave Parker, to OpenAI Responses API under profile `openai-fidelity-first-transcription-v1`, model `gpt-5.6-sol`, purpose `evidence-intelligence.external-transcription`, capability `ordinary-external-request-region-transcription-v8`. No unrelated evidence, memory, knowledge, derivatives, correspondence, legal analysis, or hidden retrieval may be included. Parker’s canonical V8 builder determines page/region IDs and ordering; blank/obscured regions remain explicit and are not guessed.

## Execution, persistence, and review requirements

R6 receives a fresh authorization and execution identity, maximum one OpenAI call, automatic retries 0, Claude/other providers 0. The proven ordering remains response → durable raw provider-state → parsing → outer-envelope provenance enrichment → validation → derivative admission. The original remains authoritative; derivative provenance must bind source, authorization, execution, request, capability/digest, provider/profile/model, raw state, regions, uncertainty, and derivative identities. Human review must compare original and derivative page/region by page/region and separately classify structural validity, provenance validity, transcription fidelity, and uncertainty honesty. The OI11R4L `NO_VISIBLE_TEXT`/`_` observation is carried forward for truthful review, not silently normalized.

## Owner privacy/data-minimisation review

Before R6 acceptance, the owner must record what the document contains, whether third-party information is present, why each page/region is needed, exactly what will be transmitted, the OpenAI destination/profile/model, and confirmation that unrelated Parker context is excluded. This is transparent control, not legal adjudication.

## Candidate-selection form (pending owner input)

- Proposed document: `OWNER TO SELECT`
- Evidence ID / filename / SHA-256 / size / page count: `PENDING`
- Document type and representative features: `PENDING`
- Third-party information: `PENDING OWNER REVIEW`
- Pages/regions proposed for egress: `PENDING`
- Provider/profile/model: OpenAI / `openai-fidelity-first-transcription-v1` / `gpt-5.6-sol`
- Maximum calls / retries: `1 / 0`
- External reasoning: `NOT AUTHORIZED`

## Exact next unit

`OI11R6 — First Governed Real-Document V8 Ingestion` may begin only after the owner selects and explicitly accepts one exact document transaction, its frozen identity and egress boundary, and a fresh authorization. No real evidence is authorized by this Scope Lock.
