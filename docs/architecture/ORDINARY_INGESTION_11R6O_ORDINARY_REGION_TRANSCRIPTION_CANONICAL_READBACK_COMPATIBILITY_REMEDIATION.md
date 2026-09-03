# OI11R6O — Ordinary-Region Transcription Canonical Readback Compatibility Remediation

## 1. Starting state

The repository gate passed on host `parker`: branch `main`, HEAD and upstream both `a5cac08c7d2f6b58470ec3ad5a524bfcdf832059`, with a clean worktree. Production was inspected read-only before implementation. It was running container `aae0bf09790510cdb6d2e47a7dfeb25e79bc7f4b86236e7e3121fb5fa66f3149`, image/index `sha256:adbb96afdb732a4549661fef08773d1b70a471e5311c804392f5fba26ce1ea4e`, embedded source `ca15222c9f5edea28e68bbb0099734578fc30c4a`, and runtime JAR SHA-256 `ce5191a5a04de91c9697acb38043cba6ddfa0c11bfb20f16babeb216804d7137`. Restart count was zero and readiness was PASS.

## 2. Preserved R6N outcome

OI11R6N remains verdict C, `POST-ADMISSION VERIFICATION FAILURE`. Its canonical zero-egress continuation admitted one derivative with `providerInvoked=false`; only the subsequent owner-facing canonical readback failed with HTTP 500. This unit did not invoke continuation, alter that verdict, or reinterpret the historical event as contemporaneously successful.

## 3. Exact admitted derivative identity

The existing generation is `region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6`. The generation record SHA-256 is `9fb18b02db5ac55e5d446cd48ebc619de929c4596f94d2a11fba1a07da71af14`; the content record SHA-256 is `18a6ed08a4729350027d3140dc0f07dd49d32c04aa45f9e3e9558df5d007c4eb`. Exactly one generation and content pair remains admitted for the preserved execution. Neither record was rewritten, replaced, deleted, or duplicated.

## 4. Failing canonical path

The R6N request used `GET /owner/evidence/{evidenceArtifactId}/content/{derivativeGenerationId}`. `OwnerEvidenceHttpServer.handleRetrieveContent` called `OwnerEvidenceOperations.retrieveTierAExtractedContent`, implemented by `OwnerUiEvidenceRuntimeAdapter.retrieveTierAExtractedContent`. That delegated to `ParkerRuntime.retrieveTierAExtractedContentAsOwner` and `TierAContentRetrievalCoordinator`, which successfully read the independently governed generation and content stores.

`DerivativeContentCodec` already canonically supports format byte 6 as `TierADerivativePayload.RegionTranscription`, including representation versions 1, 2 and 3, and already fails closed for corrupt content and unsupported representation versions. The failure occurred only after successful canonical readback: `OwnerUiEvidenceRuntimeAdapter.toOwnerContent` had an exhaustive `RegionTranscription` branch that deliberately threw `Ordinary region transcription uses its governed owner-result projection, not historical Tier A presentation`. The exception propagated through the HTTP handler and became HTTP 500.

## 5. Root cause

The exact cause was a missing explicit owner-presentation adapter branch: the legacy exhaustive Tier A presentation switch prohibited a derivative kind that the canonical stores and codec validly supported. This was not a provider, authorization, transcription, provenance, admission, storage, or codec defect, and it was not an invalid Tier classification. The HTTP 500 was an unhandled compatibility restriction at the final presentation boundary.

## 6. Scope boundary

The correction is read-only presentation mapping. It does not regenerate or edit transcription, create derivative content or provenance, infer missing fields, call a provider, or change admission, execution, capability, evidence, or derivative semantics. Ordinary-region transcription retains its truthful type identity and is not relabelled as PDF, CSV, email, DOCX, or OCR.

## 7. Code correction

`OwnerTierAContent` now has an explicit `RegionTranscription` presentation type. `OwnerUiEvidenceRuntimeAdapter` deterministically projects the canonical generation record and `OrdinaryRegionTranscriptionDerivative` into that type. The projection retains generation/content identities, evidence binding, page and region bindings, literal transcription blocks, provider-returned and Parker source order, execution/provider/preparation/capability/authorization provenance, transformation history, completeness, and warnings. `OwnerEvidenceHttpServer` serializes this type under the truthful discriminator `REGION_TRANSCRIPTION`.

No broad Tier A redesign or codec evolution was needed.

## 8. Fail-closed behavior

Existing fail-closed boundaries remain intact: unknown generation, evidence mismatch, missing content, corrupt content, unsupported representation version, and unsupported presentation payloads retain their governed outcomes. OCR remains outside this Tier A presentation path. The change is an explicit adapter for one already-governed payload type, not an accept-all default. Invalid states are not converted to success and contradictions are not hidden.

## 9. Historical compatibility

Historical PDF, CSV, email and DOCX presentation branches are unchanged. Their existing regression suites pass. Content codec meanings and representation versions are unchanged, and no historical record is reinterpreted. Tests also retain deterministic rejection of unknown/unsupported versions and corrupt content.

## 10. Focused tests

Focused bounded-heap command:

`OI11R6O_GENERATION_ROOT=/tmp/oi11r6o-readback.6IW6rg/generations OI11R6O_CONTENT_ROOT=/tmp/oi11r6o-readback.6IW6rg/content ./gradlew --no-daemon test --tests 'parker.composition.OwnerUiEvidenceRuntimeAdapterTest' --tests 'parker.composition.OwnerEvidenceHttpServerTest' --tests 'parker.core.runtime.TierAContentRetrievalCoordinatorTest' --tests 'parker.core.runtime.FileSystemDerivativeContentStorageTest' -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g`

Result: 139 tests, zero skipped, zero failures, zero errors. Coverage includes the adapter and actual loopback HTTP endpoint, five-region/order retention, exact transcription projection, repeated read-only retrieval, historical Tier A paths, missing/corrupt/unsupported content, and absence of any provider dependency in the retrieval composition.

An initial default-heap compile attempt failed transparently with Kotlin compiler `OutOfMemoryError`. No test failed in that attempt. Re-running with the established bounded 4 GiB Gradle/Kotlin heap succeeded.

## 11. Full-suite result

`./gradlew --no-daemon test -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g` passed: 251 suites, 3,329 tests, 18 skipped, zero failures and zero errors.

## 12. Exact derivative offline readback

The exact production generation and content files were copied read-only into isolated `/tmp` roots. Their copied hashes reproduced `9fb18b02db5ac55e5d446cd48ebc619de929c4596f94d2a11fba1a07da71af14` and `18a6ed08a4729350027d3140dc0f07dd49d32c04aa45f9e3e9558df5d007c4eb`. The test traversed the real filesystem generation/content codecs, `TierAContentRetrievalCoordinator`, and corrected owner adapter.

Canonical offline readback passed for generation `region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6`: five page bindings, five region bindings, five transcription blocks, and the persisted Parker source order corresponding to pages `[1,2,3,4,5]` were retained. Evidence and execution bindings, provider `OpenAI`, provider profile `openai-fidelity-first-transcription-v1`, model `gpt-5.6-sol`, preparation profile `full-page-achromatic-png-preparation-v1`, and the distinct governed processing profile all read back exactly. Provider calls and governed mutations were zero.

## 13. Production/store immutability

Final read-only inspection found the same container, image, source and runtime JAR, restart count zero, running/readiness PASS, and no deployment or restart. Before/after store counts and aggregate hashes were unchanged: evidence 29 (`e5d29f86...`), manifests 29 (`ec2a7dc1...`), corrected preparations 6 (`07018599...`), capability acceptances 14 (`14eeeb9c...`), authorizations 14 (`b9b30cce...`), attempts 10 (`798ed0fd...`), provider state 8 (`be1b3310...`), derivative generations 23 (`801bdfd3...`), and derivative content 21 (`80eee2bb...`). Production governed-store delta was zero.

## 14. Provider/egress accounting

OI11R6O activity: OpenAI calls 0; Claude calls 0; other provider calls 0; retries 0; external evidence egress 0. The historical real-document budget remains one call authorized, one consumed, zero retries.

## 15. Remaining blockers

The complete canonical retrieval-to-owner-presentation-to-HTTP response path has no remaining known structural readback blocker. Production still runs the prior artifact; a separate artifact build, owner acceptance, deployment and production readback verification remain required.

## 16. Verdict

**A — ORDINARY-REGION TRANSCRIPTION CANONICAL READBACK COMPATIBILITY CORRECTED AND STATICALLY CONVERGED**
