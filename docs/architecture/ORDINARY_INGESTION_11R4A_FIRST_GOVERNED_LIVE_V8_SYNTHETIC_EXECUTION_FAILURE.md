# OI11R4A First Governed Live V8 Synthetic Execution — Failure Evidence

**Verdict: UNIT ORDINARY-INGESTION-11R4A NOT COMPLETE**

The frozen synthetic fixture was registered successfully, but Parker's governed authorization gate stopped the unit before provider execution with `CAPABILITY_NOT_ACCEPTED`. The single authorized provider transaction was not consumed; no retry is authorized.

## Preflight

- Source fixture: two-page PDF generated with the frozen PDFBox procedure.
- Evidence artifact: `evidence-84d85f99-3a94-4101-86b2-8b8aa9aef0ae`.
- Source SHA-256: `f9ce327166b00c28d0ce50334bad256c03f2ff9c74e6b045592f53f8dce03a89`.
- Size: 1,406 bytes; media type `application/pdf`.
- Required purpose: `evidence-intelligence.external-transcription`.
- Required capability: `ordinary-external-request-region-transcription-v8`.

## Gate result

`POST /owner/evidence/{id}/authorize-region-transcription` returned HTTP 409 with status `UNAVAILABLE` and detail `CAPABILITY_NOT_ACCEPTED`. No authorization reservation, request-region construction for transport, provider attempt, raw provider state, parse, validation, reconstruction, or derivative admission occurred.

Static inspection of the mounted capability-acceptance root showed only legacy `*.region-capability-acceptance-v2` records. The V8 evaluator requires an exact V8 capability acceptance record matching the current runtime implementation commit and V8 capability digest. This is a governed acceptance-storage prerequisite, not a reason to bypass authorization.

## Integrity

Production remained running on image `sha256:9268d5d1685f6760cc6daea7fb40000c437584ec2721156a40143266530a3ec7`, restart count 0. The seven programme store counts after source registration were `4 / 2 / 1 / 21 / 19 / 6 / 5`; source registration accounts for the intended new synthetic source/manifest records. No provider calls, retries, or external egress occurred.

## Next unit

`OI11R4B — V8 Capability Acceptance Reconciliation` must establish the exact governed V8 capability-acceptance record for the accepted runtime artifact/implementation, without provider execution. Only after that prerequisite is satisfied may OI11R4A be re-authorized; no second live call is permitted in this unit.
