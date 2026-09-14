# Parker OCR Authority Policy

## Governing rule

Parker does not treat local OCR output as authoritative evidence extraction. OCR used for
governed evidence analysis must come from an authorised external OCR provider. Local OCR is
preliminary/diagnostic only.

Local Docling/RapidOCR may be used for diagnostics, triage, pre-screening, determining whether
OCR is required, comparison against external OCR, and developer/test tooling. Its output must
not become the preferred governed representation or suppress escalation to authorised external
OCR.

This policy is explicit because local OCR has demonstrated insufficient extraction fidelity for
Parker’s evidentiary requirements.

## Required flows

Usable native text follows the existing native extraction and governed representation path.

OCR-required evidence follows:

```text
detect OCR requirement
→ authorised external OCR
→ validate provider response and provenance
→ persist OCR derivative
→ governed selection
→ retrieval/analysis
```

If authorised external OCR is unavailable because of provider, credential, egress, network, or
service policy, the evidence remains `REVIEW_REQUIRED` or `CAPABILITY_UNAVAILABLE`. Local OCR is
not promoted as a substitute.

## Persisted authority classification

Every persisted OCR payload carries an `OcrAuthorityClassification`:

- `EXTERNAL_AUTHORITATIVE`: produced by an authorised external provider and eligible for governed
  evidence analysis after normal validation;
- `LOCAL_PRELIMINARY`: produced by local OCR, including Parker Docling and Hermes Docling
  pre-ingestion, and never eligible as authoritative evidence.

The classification survives derivative-content persistence and reload. Source identity,
provenance, hash, completeness, and review controls remain independently enforced.

## Format scope

The authority rule applies uniformly to JPEG, PNG, WebP, image-only/scanned PDF, and any future
OCR-required media type. There are no format-specific local-OCR exceptions.
