# PDF Ingestion Programme Closure / Acceptance Record

## Status

Accepted. Production verified. Programme complete.

## Accepted Production Baseline

- Commit: `0b7f4296107b70847580281e028936a6395dd998`
- Image: `sha256:546333cf52bd0adcb7440388d7db839c082c64650156c823dcf78abbbeadbb43`
- Status: running
- Restart count: 0
- Readiness: PASS

## Real-Document Acceptance Target

- Evidence: `evidence-3b2fb721-11e8-48cf-854a-30470449c857`
- Filename: `amber email to Kylie Tue 28 Jan 2025 at 1034 AM.pdf`
- Case: Michael - ERA
- External derivative generation: `52254126-da94-45ca-a7c1-fb513ef7b319`
- Saved analysis: `ebb6b3b5-3bf2-4b30-9743-a998f7d9cb8a`
- Provider/model: OpenAI / `gpt-5.6-sol`
- Completeness: `ACCOUNTED_FOR`
- Intrinsic fidelity: `UNVERIFIED_LITERAL_TRANSCRIPTION`
- Effective human review: `HUMAN_REVIEWED_PASS`

## Verified End-to-End Path

Owner PDF upload → immutable EvidenceArtifact → Case assignment → source inspection → exact-evidence external authorization → deterministic governed external routing → fresh per-attempt execution binding → durable attempt ledger → governed OpenAI transcription → provider result validation → immutable derivative admission → exact-generation retrieval → exact-generation Human Fidelity Review → `HUMAN_REVIEWED_PASS` effective review → exact reviewed generation supplied to analysis → provider-generated non-canonical analysis → explicit Owner Save → durable saved-analysis retrieval without model reinvocation.

## Verified Invariants

- Original evidence, identity, source SHA-256, and provenance remain immutable and preserved.
- Case classification is durably connected without changing evidence identity.
- Production Local OCR is `DISABLED` and was not used for the accepted generation.
- Historical Local OCR generation `efa89a97-f94d-4aa2-9309-8d9cf227dccc` remains preserved and unused.
- External transcription is governed by global acceptance, explicit exact-evidence authorization, permission, readiness, and egress controls.
- Routing is deterministic and fail-closed; governed execution uses a fresh per-attempt binding and durable attempt ledger.
- Provider validation precedes immutable derivative admission; derivative identity is separate from Evidence identity.
- Retrieval and HFR bind to the exact evidence and exact generation.
- `HUMAN_REVIEWED_PASS` does not alter intrinsic machine fidelity.
- Analysis consumed the exact reviewed external generation and remains provider-generated, non-canonical material.
- Save is explicit and the saved analysis is durably retrievable without model reinvocation.
- Evidence, derivatives, HFR, analysis, saved analysis, Memory, Knowledge, and canonical Parker truth remain distinct governed states.

## Known Non-Blocking Observation

Analysis instruction adherence was PARTIAL in the final real-document test. For the instruction “was brooklyn trainer in this email? make no changes answer yes or no”, the substantive result was `NO`, but the model supplied explanatory text despite the requested yes/no-only form. This is an analysis/reasoning instruction-adherence or UX concern, not a PDF-ingestion correctness, fidelity, provenance, authorization, custody, routing, HFR, durability, or retrieval defect, and does not reopen this programme.

## Final Acceptance Verdict

**A — PRODUCTION PDF INGESTION END-TO-END VERIFIED AND COMPLETE**

Parker’s production PDF-ingestion path has been demonstrated end-to-end against real evidence through durable saved-analysis retrieval. No unresolved blocker remains within the accepted PDF-ingestion scope. The accepted production baseline is commit `0b7f4296107b70847580281e028936a6395dd998` and image `sha256:546333cf52bd0adcb7440388d7db839c082c64650156c823dcf78abbbeadbb43`. The PDF-ingestion programme is CLOSED.
