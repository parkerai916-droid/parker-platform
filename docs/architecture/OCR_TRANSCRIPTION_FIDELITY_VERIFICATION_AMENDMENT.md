# OCR Transcription Fidelity and Verification Amendment

## Status

**Adopted governance.** This amendment corrects the provider-neutral
transcription-fidelity taxonomy. It authorises no provider, network access,
runtime implementation, deployment, external submission, persistence action,
or mutation of any historical derivative.

## 1. Purpose

The taxonomy shared by `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` §5,
`OCR_MECHANISM_CONTRACT_DESIGN.md`, and `OCR_MECHANISM_SCOPE_LOCK.md`
previously distinguished verbatim, normalised, and inferred-reconstruction
output but lacked a truthful category for a machine-produced attempt at
literal transcription whose exact correspondence with the source has not
been independently established. This amendment adds that missing category.

## 2. `VERBATIM` remains a strict source-fidelity claim

`VERBATIM` means that the classified transcription reproduces the source's
exact characters, spelling, and layout as read. It does not merely mean that:

- Parker preserved the provider response exactly;
- the provider labelled an operation successful;
- no intentional normalisation was requested;
- output appears plausible or fluent;
- a provider supplied high confidence; or
- no warning was returned.

Preservation of provider-returned content is derivative-integrity provenance,
not proof of exact correspondence with the source.

## 3. `UNVERIFIED_LITERAL_TRANSCRIPTION`

A fourth value, `UNVERIFIED_LITERAL_TRANSCRIPTION`, is authorised. It means:

> A machine-produced attempt to reproduce readable source content without
> intentional normalisation, substantive correction, or inferred
> reconstruction, whose exact correspondence with the source has not been
> independently established.

It does not assert character-for-character accuracy or completeness, conceal
known uncertainty or omission, or authorise guessed completion of illegible
content. It remains derivative material, never source text.

## 4. Existing classifications

`NORMALISED` continues to mean that recognised content was deliberately
corrected or standardised for readability and is no longer an exact literal
rendering.

`INFERRED_RECONSTRUCTION` continues to mean that content not clearly supported
by the source was supplied through inference about what the source likely said.

An illegibility marker or structured uncertainty annotation is a disclosure
about the transcription and must not itself be represented as literal source
text.

## 5. Assignment rule

Ordinary OCR or transcription success is insufficient to assign `VERBATIM`.
That classification requires a separately governed verification method that
establishes exactness for the classified scope, such as human verification
against the source or deterministic comparison with an independently
authoritative textual representation. Provider confidence, fluency, absence of
warnings, or clean transport is not independent verification.

Where verification covers only part of a document, `VERBATIM` applies only to
that identified part. Other portions retain their own truthful classifications.

## 6. Uncertainty and mixed fidelity

Transcription output must preserve, where applicable, uncertain and illegible
spans, omitted or unavailable material, inferred reconstruction, page
association, provider warnings, and partial or degraded status. A provider must
not manufacture certainty to produce complete-looking text. Segment-level
classification must preserve mixed fidelity and may not be erased by a less
qualified document-level label.

## 7. Fidelity and completeness are distinct

Fidelity describes the relationship between returned text and readable source
content. Completeness describes whether the governed source scope was
accounted for. `VERBATIM` does not prove complete page coverage, and
`UNVERIFIED_LITERAL_TRANSCRIPTION` does not by itself establish incompleteness.
A complete-looking transcript proves neither fidelity nor coverage.

## 8. Durable representation and history

A durable OCR/transcription representation must preserve its truthful fidelity
classification and material qualifications. Historical generations are
immutable. This amendment does not authorise rewriting an earlier generation
whose fidelity was overstated. Correction or human review produces a new linked
derivative or separately governed review record.

## 9. Consequential normative consistency

Upon adoption:

1. `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` §5 contains four fidelity
   categories.
2. `OCR_MECHANISM_CONTRACT_DESIGN.md` references those same four categories.
3. `OCR_MECHANISM_SCOPE_LOCK.md` requires one of those four and prohibits an
   invented fifth category.
4. Historical implementation plans, completion reviews, and verification
   records remain accurate records of the earlier three-value contract and are
   not rewritten.
5. The implemented enum, durable representation, mappings, tests, and UI
   terminology require a later separately authorised implementation unit.

## 10. Explicit non-authorities

This amendment does not authorise a provider, OpenAI, network access, external
disclosure, automatic OCR, durable admission, substantive analysis, Memory or
Knowledge writes, source modification, source replacement, retroactive
derivative mutation, or treatment of unverified machine text as source truth.

## 11. Constitutional consistency

This amendment strengthens rather than reopens original-evidence immutability,
Evidence Custodian's exclusive custody, Evidence Intelligence's lack of truth
authority, provenance, uncertainty disclosure, human review, and fail-closed
behaviour under uncertainty.

## Final determination

**ADOPTED.** `VERBATIM` remains strict. Unverified machine attempts at literal
transcription are represented distinctly and never promoted into proven source
accuracy by ordinary provider success.
