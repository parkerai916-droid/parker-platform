# ORDINARY-INGESTION-7B pre-attempt failure diagnosis and recovery

## Outcome and baseline

Starting repository `e4b7fb3b8761bd53cf88bc8fa6e84052b659fd74` was clean/upstream-equal. Production was implementation `72482456531e91ff8eced4cbe073f182ae805126`, image `sha256:c8e0dc7da6b5c167f2259314b06bd838230f048c8786723246aa1b09b5c51dda`, container `728a00e4ce3e4448873b3016c9c3331e67966602842656fba049dc1e3fea7be6`, running/restart 0/HTTP 200 and 401.

Target `evidence-4c6f2ee8-2f62-47be-bd7a-946c744b2766` had SHA-256 `ce8bd4b53d8b007026575974014e71f648f045bf3970b0e984605cf842a7b4a5`. Authorization `ordinary-auth-053379368e563411f3670c43778660de5c463241c9673ea0e1ed181ebaffd721` was reserved to `ordinary-exec-bb43c68d-3e3e-469a-80b3-aa23727f7fe8`. Grant/event SHAs were `5689d30f05089b1dee47f808cdd40af7776f6076f3b23fc22b0b6f7cb460ff7a` and `96b5f0a7eccefa3011dbf2968e015c48b57720cc1dddfbc8ee1016d611ebf61b`. There was no attempt ledger, provider-attempt marker, new provider-state, or derivative.

## Provider-free trace

| Order | Class/method | Input/output | Fail-closed outcome | Durable/logged/surfaced before correction | Can follow reservation |
|---:|---|---|---|---|---:|
| 1 | `OrdinaryRegionIngestionWorkflow.execute` acceptance/source resolution | evidence ID to accepted record and custody-verified bytes | `CAPABILITY_NOT_ACCEPTED`, `SOURCE_UNAVAILABLE` | no/no/HTTP result | no |
| 2 | authorization guard/store `load`/`reserve` | authorization + execution to reserved snapshot | required/expired/revoked/conflict | reservation durable; error not logged; briefly surfaced | yes |
| 3 | `OrdinaryRegionRequestPreparer.prepare` media check | verified media | `UNSUPPORTED_MEDIA` | no/no/briefly | yes before correction |
| 4 | `DeterministicSourcePageRenderer.render` | exact PDF bytes, SHA, 300-DPI profile | `REQUEST_BOUNDS_EXCEEDED` | no/no/briefly | yes before correction |
| 5 | page loop | declared pages to canonical page rasters | `REQUEST_BOUNDS_EXCEEDED` | no/no/briefly | yes before correction |
| 6 | `DeterministicSourceRegionDeriver.derive` | canonical pixels to geometry/order graph | bounds/review/order dispositions | no/no/briefly | yes before correction |
| 7 | graph ambiguity checks | graphs to unambiguous eligibility | `SOURCE_ORDER_REVIEW_REQUIRED`, `SOURCE_ORDER_NOT_SUPPORTED` | no/no/briefly | yes before correction |
| 8 | complete-set construction | all page regions to targets | `NO_TRANSCRIBABLE_REGIONS`, `REQUEST_BOUNDS_EXCEEDED` | no/no/briefly | **exact failure** |
| 9 | request encoding/bound | 1–32 targets to canonical request | `REQUEST_BOUNDS_EXCEEDED` | no/no/briefly | yes before correction |
| 10 | execution identity and Parker order | request/source/build to binding | `REVIEW_REQUIRED` | no/no/briefly | yes before correction |
| 11 | coordinator `prepareForGuardedAttempt` | binding to pre-attempt ledger stages | binding/identity/persistence failures | ledger durable if reached | yes |
| 12 | guarded attempt start | prepared binding to `PROVIDER_ATTEMPT_STARTED` | conflict/persistence failure | durable marker | yes |

The correction moves validation/loading of the authorization and the complete provider-free `prepare` phase before creating a new reservation. Existing same-execution reservations remain admissible to the check; different executions remain rejected. A new reservation occurs only after preparation succeeds, and attempt-start remains immediately before transport under the authorization guard.

## Exact reproduction and root cause

A SHA-verified temporary copy of the exact production evidence was passed through the production resolver, renderer, deriver, request encoder, digest, profile and commit with a transport that throws if called and temporary state only. Result:

`Blocked(disposition=REQUEST_BOUNDS_EXCEEDED, detail=complete region set exceeds 32)`

Reproduction: PASS; deterministic; provider calls 0. Producer: `OrdinaryRegionRequestPreparer.prepare`. The condition is evidence-specific: the complete deterministic region set exceeds `RegionTranscriptionRequest.MAX_REGIONS_PER_REQUEST = 32`.

The limit is intentional accepted governance: exactly one request, complete region set, 1–32 regions, no splitting, truncation, omission, or batching. Region geometry and the limit were not changed. The implementation defect was that this deterministic rejection occurred after a new authorization reservation. Classification: C `REQUEST_BOUND_DEFECT` (sequencing/exposure around an intentional bound) and I `ERROR_PROPAGATION_DEFECT`.

## Error surface

The execution JavaScript stored the exact safe HTTP detail, then called `loadAcquisitionDecision`, which unconditionally cleared `row.acquisitionError`. The loader now accepts `preserveExecutionError`; execution refresh passes `true`, while ordinary user refresh/authorization retains normal clearing. Canonical state refresh and the exact safe disposition/detail therefore display together. No payload, credential, token or stack trace is exposed.

## Tests and semantic check

Targeted ordinary workflow, HTTP/UI, and adapter suites passed. New deterministic coverage proves a 33-region provider-free rejection occurs before a new reservation, produces the exact disposition/detail, creates no ledger and calls no provider; HTTP/UI assertions prove execution refresh preserves the error while canonical state refreshes. Existing same-execution idempotence, different-execution conflict, concurrency, attempt-start ordering, no-retry, authorization, listing, and acceptance tests remain green.

Full suite: 3,258 tests, 0 failures, 0 errors, 9 skipped. `git diff --check`: PASS.

Accepted capability semantics changed: **NO**. Provider, endpoint, model, adapter, profile, wire, schema, rendering, region geometry, 32-region/16-MiB limits, `store=false`, retry, provider-state persistence and derivative admission are unchanged.

## Recovery and build transition

Within the same exact build, `FileSystemOrdinaryRegionAuthorizationStore.reserve` is idempotent for the same execution ID and rejects a different ID. Thus a reserved authorization with no attempt-start is generally same-execution recoverable; no new authorization or duplicate reservation is needed, and attempt-start still occurs only after successful preparation.

The particular production execution cannot succeed under the unchanged capability because its complete region set exceeds 32. It also cannot resume after this source deployment: authorization identity includes `runtimeCommit()`, so the old reserved grant remains immutable historical state bound to `7248245...` and is not valid for `def611a...`. No migration, abandonment, revocation, or replacement was performed. Any future new-build execution requires explicit owner authorization, but Steven should not re-authorize this evidence until a separately governed capability/eligibility decision addresses its >32-region shape.

## Deployment and final state

- Implementation commit: `def611a8bf8cb6c2297f1d9bf6cd8146a58d4cbc`.
- Image: `sha256:fdb583d16d99a58d13983046b2ad8b936014ead6b6c22cdf0d670b895b071521`.
- Container: `281bba01fa82ddd4a172a424688845ea180a6dfe28eb4ae2aebd9c064ecd68ca`, running, restart 0, HTTP 200/401.
- Repository/build/embedded/configured/deployment identity equality: PASS.
- Pre-promotion: `CAPABILITY_NOT_ACCEPTED`.
- Promotion: exactly one POST, HTTP 201, record `226108722a77de027e3ad15226704009aa5f1efb7effd6f35a3c31075a8c58c2`.
- Post-promotion without restart: `ACCEPTED`; acceptance count 6.
- Live target: `PROPOSED`, `NOT_AUTHORISED`, `authorizationAvailable=true`, `executeAvailable=false`, `NOT_STARTED`; no persisted owner error exists across the build transition.
- Historical reserved authorization/event SHAs unchanged.
- Attempts/provider-state/derivatives unchanged at 4/2/21 generations/19 content.
- OpenAI calls 0; Claude calls 0.

## Next action

Do not execute, retry, or re-authorize the target. The next unit must separately govern how ordinary ingestion handles complete deterministic region sets above 32 (for example capability eligibility or a newly accepted acquisition strategy) without silently splitting, truncating, omitting regions, or weakening the one-attempt rule.
